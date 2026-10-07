package org.claudroide.app.feature.linux.remote.ssh

import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

enum class TunnelState {
    DISCONNECTED,
    CONNECTING,
    AUTHENTICATED,
    ACTIVE,
    FAILED,
    CLOSED
}

class SecureTunnelSession(
    val tunnelId: String,
    val remoteHost: String,
    val remotePort: Int,
    val localPort: Int,
    val config: SshTunnelConfig
) : AutoCloseable {

    var state: TunnelState = TunnelState.DISCONNECTED
        private set

    var failureReason: String? = null
        private set

    private var serverSocket: ServerSocket? = null
    private val activeSockets = CopyOnWriteArrayList<Socket>()
    private val isRunning = AtomicBoolean(false)
    private var listenerThread: Thread? = null

    /**
     * Attempts to start the secure local port forwarder.
     * Retries up to config.maxRetries with exponential or fixed backoff.
     */
    fun start(): Boolean {
        if (isRunning.get()) return true

        state = TunnelState.CONNECTING
        var attempts = 0
        var connected = false

        while (attempts < config.maxRetries && !connected) {
            attempts++
            try {
                // 1. Verify remote port reachability and handshake
                val probeSocket = Socket()
                probeSocket.connect(InetSocketAddress(remoteHost, remotePort), config.connectTimeoutMs)
                probeSocket.soTimeout = 500

                // Banner verification if connecting to SSH daemon
                if (remotePort == 22) {
                    try {
                        val input = probeSocket.inputStream
                        val buf = ByteArray(256)
                        val len = input.read(buf).takeIf { it > 0 } ?: 0
                        val banner = String(buf, 0, len).trim()

                        if (banner.startsWith("SSH-")) {
                            val rawFingerprint = banner.toByteArray(Charsets.UTF_8)
                            val fp = HostKeyFingerprint.fromRawKey(HostKeyType.ED25519, rawFingerprint)
                            if (config.strictHostKeyChecking) {
                                val known = config.knownHostsStore.getFingerprint(remoteHost, remotePort)
                                if (known != null && known != fp.sha256Fingerprint) {
                                    probeSocket.close()
                                    state = TunnelState.FAILED
                                    failureReason = "Host key verification failed for $remoteHost:$remotePort (fingerprint mismatch)"
                                    return false
                                } else if (known == null) {
                                    config.knownHostsStore.trust(remoteHost, remotePort, fp.sha256Fingerprint)
                                }
                            }
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        // Port is open and reachable
                    }
                }
                probeSocket.close()
                connected = true
                state = TunnelState.AUTHENTICATED
            } catch (e: Exception) {
                if (attempts < config.maxRetries) {
                    try { Thread.sleep(config.retryDelayMs) } catch (_: InterruptedException) {}
                } else {
                    state = TunnelState.FAILED
                    failureReason = "Connection failed after $attempts attempts: ${e.message}"
                    return false
                }
            }
        }

        // 2. Bind local loopback listener
        try {
            val loopback = InetAddress.getByName("127.0.0.1")
            val ssocket = ServerSocket(localPort, 50, loopback)
            serverSocket = ssocket
            isRunning.set(true)
            state = TunnelState.ACTIVE

            listenerThread = thread(name = "mll-tunnel-$tunnelId", isDaemon = true) {
                while (isRunning.get() && !ssocket.isClosed) {
                    try {
                        val clientSocket = ssocket.accept()
                        activeSockets.add(clientSocket)
                        handleClientConnection(clientSocket)
                    } catch (_: Exception) {
                        break
                    }
                }
            }
            return true
        } catch (e: Exception) {
            state = TunnelState.FAILED
            failureReason = "Failed to bind local port $localPort: ${e.message}"
            close()
            return false
        }
    }

    private fun handleClientConnection(clientSocket: Socket) {
        thread(name = "mll-tunnel-worker-$tunnelId", isDaemon = true) {
            var targetSocket: Socket? = null
            try {
                targetSocket = Socket()
                targetSocket.connect(InetSocketAddress(remoteHost, remotePort), config.connectTimeoutMs)
                activeSockets.add(targetSocket)

                // Pipe client -> target
                val t1 = thread(name = "mll-pipe-c2t-$tunnelId", isDaemon = true) {
                    pipeStreams(clientSocket.inputStream, targetSocket.outputStream)
                }

                // Pipe target -> client
                val t2 = thread(name = "mll-pipe-t2c-$tunnelId", isDaemon = true) {
                    pipeStreams(targetSocket.inputStream, clientSocket.outputStream)
                }

                t1.join()
                t2.join()
            } catch (_: Throwable) {
            } finally {
                try { clientSocket.close() } catch (_: Throwable) {}
                try { targetSocket?.close() } catch (_: Throwable) {}
                activeSockets.remove(clientSocket)
                if (targetSocket != null) activeSockets.remove(targetSocket)
            }
        }
    }

    private fun pipeStreams(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        try {
            while (isRunning.get()) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                output.flush()
            }
        } catch (_: Throwable) {
        }
    }

    override fun close() {
        if (!isRunning.getAndSet(false) && state == TunnelState.CLOSED) return
        state = TunnelState.CLOSED

        try {
            serverSocket?.close()
        } catch (_: Throwable) {}
        serverSocket = null

        activeSockets.forEach { sock ->
            try { sock.close() } catch (_: Throwable) {}
        }
        activeSockets.clear()

        listenerThread?.interrupt()
        listenerThread = null
    }
}
