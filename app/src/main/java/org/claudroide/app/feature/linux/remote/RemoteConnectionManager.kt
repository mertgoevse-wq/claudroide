package org.claudroide.app.feature.linux.remote

import org.claudroide.app.feature.linux.remote.discovery.DynamicPeerScanner
import org.claudroide.app.feature.linux.remote.ssh.SecureTunnelSession
import org.claudroide.app.feature.linux.remote.ssh.SshTunnelConfig
import org.claudroide.app.feature.linux.terminal.TerminalConfig
import org.claudroide.app.feature.linux.terminal.TerminalSession
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

enum class ConnectionBearer {
    WLAN_HOTSPOT, // Provided by A56 to OnePlus or vice-versa
    USB_TETHERING,
    BLUETOOTH_PAN
}

data class RemoteHost(
    val ipAddress: String,
    val bearer: ConnectionBearer,
    val sshPort: Int = 22,
    val vncPort: Int = 5900,
    val isReachable: Boolean = false,
    val macAddress: String? = null,
    val hostname: String? = null,
    val hostKeyFingerprint: String? = null
)

/**
 * Handles dynamic device discovery, reachability, and secure tunnel lifecycle for REMOTE-001.
 */
interface RemoteConnectionManager {
    fun discoverHosts(bearer: ConnectionBearer): List<RemoteHost>
    fun testReachability(host: RemoteHost): Boolean
    fun testReachability(host: RemoteHost, timeoutMs: Int): Boolean
    fun establishSshTunnel(host: RemoteHost, localPort: Int, remotePort: Int): Boolean
    fun createSecureTunnel(
        host: RemoteHost,
        localPort: Int,
        remotePort: Int,
        config: SshTunnelConfig = SshTunnelConfig()
    ): SecureTunnelSession
    fun openTerminalSession(
        host: RemoteHost,
        localTunnelPort: Int = 2223,
        config: TerminalConfig = TerminalConfig()
    ): TerminalSession
    fun getActiveTunnels(): List<SecureTunnelSession>
    fun closeAllTunnels()
}

class RemoteConnectionManagerImpl(
    private val peerScanner: DynamicPeerScanner = DynamicPeerScanner()
) : RemoteConnectionManager {

    private val TAG = "RemoteConnectionManagerImpl"
    private val activeTunnels = CopyOnWriteArrayList<SecureTunnelSession>()

    private fun logInfo(msg: String) {
        try {
            android.util.Log.i(TAG, msg)
        } catch (_: Throwable) {
        }
    }

    private fun logWarn(msg: String) {
        try {
            android.util.Log.w(TAG, msg)
        } catch (_: Throwable) {
        }
    }

    override fun discoverHosts(bearer: ConnectionBearer): List<RemoteHost> {
        val discovered = peerScanner.scanPeers(bearer)
        logInfo("Discovered ${discovered.size} peers on bearer $bearer")
        return discovered
    }

    override fun testReachability(host: RemoteHost): Boolean {
        return testReachability(host, 1500)
    }

    override fun testReachability(host: RemoteHost, timeoutMs: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host.ipAddress, host.sshPort), timeoutMs)
            socket.close()
            true
        } catch (_: Throwable) {
            false
        }
    }

    override fun establishSshTunnel(
        host: RemoteHost,
        localPort: Int,
        remotePort: Int
    ): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host.ipAddress, host.sshPort), 3000)
            socket.soTimeout = 2000

            val input = socket.inputStream
            val buf = ByteArray(256)
            val len = input.read(buf).takeIf { it > 0 } ?: 0
            val banner = String(buf, 0, len).trim()

            val sshDetected = banner.startsWith("SSH-")
            socket.close()

            if (sshDetected) {
                logInfo("SSH banner verified on ${host.ipAddress}:${host.sshPort}: $banner")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logWarn("SSH probe failed to ${host.ipAddress}: ${e.message}")
            false
        }
    }

    override fun createSecureTunnel(
        host: RemoteHost,
        localPort: Int,
        remotePort: Int,
        config: SshTunnelConfig
    ): SecureTunnelSession {
        val tunnelId = UUID.randomUUID().toString().take(8)
        val session = SecureTunnelSession(
            tunnelId = tunnelId,
            remoteHost = host.ipAddress,
            remotePort = remotePort,
            localPort = localPort,
            config = config
        )
        if (session.start()) {
            activeTunnels.add(session)
            logInfo("Started secure tunnel $tunnelId to ${host.ipAddress}:$remotePort via local port $localPort")
        } else {
            logWarn("Failed to start secure tunnel to ${host.ipAddress}:$remotePort: ${session.failureReason}")
        }
        return session
    }

    override fun openTerminalSession(
        host: RemoteHost,
        localTunnelPort: Int,
        config: TerminalConfig
    ): TerminalSession {
        val tunnelConfig = SshTunnelConfig(
            connectTimeoutMs = config.connectTimeoutMs,
            strictHostKeyChecking = host.hostKeyFingerprint != null
        )
        val tunnel = SecureTunnelSession(
            tunnelId = UUID.randomUUID().toString().take(8),
            remoteHost = host.ipAddress,
            remotePort = host.sshPort,
            localPort = localTunnelPort,
            config = tunnelConfig
        )
        activeTunnels.add(tunnel)

        val session = TerminalSession(
            sessionTitle = "Terminal: ${host.hostname ?: host.ipAddress}",
            config = config
        )
        session.attachTunnel(tunnel)
        session.start("127.0.0.1", localTunnelPort)
        return session
    }

    override fun getActiveTunnels(): List<SecureTunnelSession> {
        return activeTunnels.filter { it.state == org.claudroide.app.feature.linux.remote.ssh.TunnelState.ACTIVE }
    }

    override fun closeAllTunnels() {
        activeTunnels.forEach { tunnel ->
            try {
                tunnel.close()
            } catch (_: Throwable) {}
        }
        activeTunnels.clear()
    }
}
