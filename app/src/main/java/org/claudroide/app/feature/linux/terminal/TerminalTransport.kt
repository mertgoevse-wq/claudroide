package org.claudroide.app.feature.linux.terminal

interface TerminalTransport : AutoCloseable {
    val state: TerminalSessionState
    val failureReason: String?

    /**
     * Connects to the remote/local socket endpoint.
     */
    fun connect(host: String, port: Int, timeoutMs: Int = 5000): Boolean

    /**
     * Allocates PTY and opens an interactive shell session with specified dimensions.
     */
    fun openShell(dimensions: TerminalDimensions, termType: String = "xterm-256color"): Boolean

    /**
     * Sends raw input data to the PTY / shell stdin.
     */
    fun write(data: ByteArray)

    /**
     * Convenience method to send string text encoded as UTF-8.
     */
    fun write(text: String) {
        write(text.toByteArray(Charsets.UTF_8))
    }

    /**
     * Informs the remote PTY that terminal dimensions have changed (window-change / SIGWINCH).
     */
    fun resize(dimensions: TerminalDimensions)

    /**
     * Sets listener for stdout/data stream coming from the shell.
     */
    fun setOutputListener(listener: (ByteArray) -> Unit)

    /**
     * Sets listener for stderr/error stream coming from the shell.
     */
    fun setErrorListener(listener: (ByteArray) -> Unit)

    /**
     * Sets listener for state changes (e.g. CONNECTING, RUNNING, CLOSING, FAILED).
     */
    fun setStateListener(listener: (TerminalSessionState, String?) -> Unit)

    /**
     * Disconnects and releases resources.
     */
    fun disconnect()

    /**
     * Checks if transport is currently connected and active.
     */
    fun isAlive(): Boolean

    override fun close() {
        disconnect()
    }
}
