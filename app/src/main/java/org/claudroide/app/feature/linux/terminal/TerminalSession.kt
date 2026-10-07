package org.claudroide.app.feature.linux.terminal

import org.claudroide.app.feature.linux.remote.ssh.SecureTunnelSession
import org.claudroide.app.feature.linux.remote.ssh.TunnelState
import java.util.concurrent.CopyOnWriteArrayList

/**
 * High-level manager for an interactive terminal/PTY session.
 * Coordinates connection lifecycle, I/O streaming, buffer parsing, resize events,
 * and optional binding to a SecureTunnelSession.
 */
class TerminalSession(
    val sessionTitle: String = "Remote Terminal",
    val config: TerminalConfig = TerminalConfig(),
    val transport: TerminalTransport = SocketPtyTransport()
) : AutoCloseable {

    var state: TerminalSessionState = TerminalSessionState.DISCONNECTED
        private set

    var failureReason: String? = null
        private set

    val buffer = TerminalBuffer(maxLines = config.maxScrollback)

    private var targetHost: String? = null
    private var targetPort: Int? = null
    private var attachedTunnel: SecureTunnelSession? = null

    private val stateListeners = CopyOnWriteArrayList<(TerminalSessionState, String?) -> Unit>()
    private val outputListeners = CopyOnWriteArrayList<(String) -> Unit>()

    init {
        transport.setOutputListener { bytes ->
            buffer.write(bytes)
            val text = String(bytes, Charsets.UTF_8)
            outputListeners.forEach { listener ->
                try { listener(text) } catch (_: Throwable) {}
            }
        }

        transport.setErrorListener { bytes ->
            buffer.write(bytes)
            val text = String(bytes, Charsets.UTF_8)
            outputListeners.forEach { listener ->
                try { listener(text) } catch (_: Throwable) {}
            }
        }

        transport.setStateListener { newState, error ->
            updateState(newState, error)
        }
    }

    /**
     * Binds this terminal session to a SecureTunnelSession.
     * When attached, the terminal connects through the local loopback port of the verified tunnel.
     */
    fun attachTunnel(tunnel: SecureTunnelSession) {
        attachedTunnel = tunnel
    }

    /**
     * Starts the terminal session by connecting to the target host and opening a shell.
     */
    fun start(host: String, port: Int): Boolean {
        targetHost = host
        targetPort = port
        updateState(TerminalSessionState.CONNECTING)

        // If an SSH tunnel is attached, ensure it is active
        attachedTunnel?.let { tunnel ->
            if (tunnel.state != TunnelState.ACTIVE) {
                val ok = tunnel.start()
                if (!ok) {
                    val err = "Secure tunnel failed: ${tunnel.failureReason}"
                    updateState(TerminalSessionState.FAILED, err)
                    return false
                }
            }
        }

        val connected = transport.connect(host, port, config.connectTimeoutMs)
        if (!connected) {
            updateState(TerminalSessionState.FAILED, transport.failureReason ?: "Connection failed")
            return false
        }

        val shellOpened = transport.openShell(config.initialDimensions, config.termType)
        if (!shellOpened) {
            updateState(TerminalSessionState.FAILED, transport.failureReason ?: "Shell allocation failed")
            return false
        }

        return true
    }

    /**
     * Sends user text input to the terminal shell.
     */
    fun sendInput(text: String) {
        if (state != TerminalSessionState.RUNNING) return
        transport.write(text)
    }

    /**
     * Sends raw bytes to the terminal shell.
     */
    fun sendBytes(bytes: ByteArray) {
        if (state != TerminalSessionState.RUNNING) return
        transport.write(bytes)
    }

    /**
     * Sends a control key byte sequence (e.g. Ctrl+C, Ctrl+D, Arrow keys).
     */
    fun sendControlKey(key: ByteArray) {
        sendBytes(key)
    }

    /**
     * Translates a character to a Control sequence (e.g. 'c' -> Ctrl+C).
     */
    fun sendCtrl(char: Char) {
        sendBytes(TerminalKeys.ctrl(char))
    }

    /**
     * Notifies the PTY of a terminal dimension change.
     */
    fun resize(cols: Int, rows: Int) {
        val dimensions = TerminalDimensions(cols = cols, rows = rows)
        transport.resize(dimensions)
    }

    /**
     * Disconnects the session and releases transport resources.
     */
    fun disconnect() {
        if (state == TerminalSessionState.DISCONNECTED || state == TerminalSessionState.CLOSING) return
        updateState(TerminalSessionState.CLOSING)
        transport.disconnect()
        updateState(TerminalSessionState.DISCONNECTED)
    }

    /**
     * Attempts to reconnect to the target host up to maxReconnectAttempts.
     */
    fun reconnect(): Boolean {
        val host = targetHost ?: return false
        val port = targetPort ?: return false

        updateState(TerminalSessionState.RECONNECTING)
        disconnect()

        var attempts = 0
        var connected = false

        while (attempts < config.maxReconnectAttempts && !connected) {
            attempts++
            try {
                if (start(host, port)) {
                    connected = true
                    break
                }
            } catch (_: Exception) {}

            if (!connected && attempts < config.maxReconnectAttempts) {
                try {
                    Thread.sleep(config.reconnectDelayMs)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }

        if (!connected) {
            updateState(TerminalSessionState.FAILED, "Reconnect failed after $attempts attempts")
        }
        return connected
    }

    fun addStateListener(listener: (TerminalSessionState, String?) -> Unit) {
        stateListeners.add(listener)
    }

    fun removeStateListener(listener: (TerminalSessionState, String?) -> Unit) {
        stateListeners.remove(listener)
    }

    fun addOutputListener(listener: (String) -> Unit) {
        outputListeners.add(listener)
    }

    fun removeOutputListener(listener: (String) -> Unit) {
        outputListeners.remove(listener)
    }

    private fun updateState(newState: TerminalSessionState, error: String? = null) {
        state = newState
        failureReason = error
        stateListeners.forEach { listener ->
            try { listener(newState, error) } catch (_: Throwable) {}
        }
    }

    override fun close() {
        disconnect()
        try {
            attachedTunnel?.close()
        } catch (_: Throwable) {}
        attachedTunnel = null
    }
}
