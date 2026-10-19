package org.claudroide.app.feature.linux.terminal

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Mock implementation of TerminalTransport for unit testing and local simulation.
 * Verifies command sequences, resize events, input buffering, and error reporting.
 */
class MockPtyTransport(
    var simulatedEcho: Boolean = true,
    var prompt: String = "user@mll:~$ "
) : TerminalTransport {

    override var state: TerminalSessionState = TerminalSessionState.DISCONNECTED
        private set

    override var failureReason: String? = null
        private set

    var connectedHost: String? = null
        private set

    var connectedPort: Int? = null
        private set

    var lastDimensions: TerminalDimensions? = null
        private set

    val writtenBytes = CopyOnWriteArrayList<ByteArray>()
    val writtenStrings = CopyOnWriteArrayList<String>()
    val resizeEvents = CopyOnWriteArrayList<TerminalDimensions>()

    private var outputListener: ((ByteArray) -> Unit)? = null
    private var errorListener: ((ByteArray) -> Unit)? = null
    private var stateListener: ((TerminalSessionState, String?) -> Unit)? = null

    var shouldFailConnect: Boolean = false
    var failConnectReason: String = "Simulated connection failure"

    override fun connect(host: String, port: Int, timeoutMs: Int): Boolean {
        transitionState(TerminalSessionState.CONNECTING)
        if (shouldFailConnect) {
            failureReason = failConnectReason
            transitionState(TerminalSessionState.FAILED, failureReason)
            return false
        }
        connectedHost = host
        connectedPort = port
        transitionState(TerminalSessionState.CONNECTED)
        return true
    }

    override fun openShell(dimensions: TerminalDimensions, termType: String): Boolean {
        if (state != TerminalSessionState.CONNECTED) {
            failureReason = "Cannot open shell when state is $state"
            transitionState(TerminalSessionState.FAILED, failureReason)
            return false
        }
        transitionState(TerminalSessionState.STARTING_SHELL)
        lastDimensions = dimensions
        transitionState(TerminalSessionState.RUNNING)

        // Emit initial shell banner and prompt
        if (prompt.isNotEmpty()) {
            emitOutput("Welcome to Mobile Linux Lab PTY (${dimensions.cols}x${dimensions.rows})\r\n$prompt")
        }
        return true
    }

    override fun write(data: ByteArray) {
        if (state != TerminalSessionState.RUNNING) return
        writtenBytes.add(data)
        val text = String(data, Charsets.UTF_8)
        writtenStrings.add(text)

        if (simulatedEcho) {
            when (text) {
                "\r", "\n" -> {
                    emitOutput("\r\n$prompt")
                }
                "\u0003" -> { // Ctrl+C
                    emitOutput("^C\r\n$prompt")
                }
                "\u0004" -> { // Ctrl+D (EOF/exit)
                    emitOutput("exit\r\n")
                    disconnect()
                }
                "\u007f", "\b" -> { // Backspace
                    emitOutput("\b \b")
                }
                else -> {
                    emitOutput(text)
                }
            }
        }
    }

    override fun resize(dimensions: TerminalDimensions) {
        lastDimensions = dimensions
        resizeEvents.add(dimensions)
    }

    override fun setOutputListener(listener: (ByteArray) -> Unit) {
        outputListener = listener
    }

    override fun setErrorListener(listener: (ByteArray) -> Unit) {
        errorListener = listener
    }

    override fun setStateListener(listener: (TerminalSessionState, String?) -> Unit) {
        stateListener = listener
    }

    override fun disconnect() {
        if (state == TerminalSessionState.DISCONNECTED) return
        transitionState(TerminalSessionState.CLOSING)
        transitionState(TerminalSessionState.DISCONNECTED)
    }

    override fun isAlive(): Boolean {
        return state == TerminalSessionState.RUNNING
    }

    fun emitOutput(text: String) {
        outputListener?.invoke(text.toByteArray(Charsets.UTF_8))
    }

    fun emitOutput(data: ByteArray) {
        outputListener?.invoke(data)
    }

    fun emitError(text: String) {
        errorListener?.invoke(text.toByteArray(Charsets.UTF_8))
    }

    fun simulateDrop(reason: String = "Remote host closed connection") {
        failureReason = reason
        transitionState(TerminalSessionState.FAILED, reason)
    }

    private fun transitionState(newState: TerminalSessionState, error: String? = null) {
        state = newState
        stateListener?.invoke(newState, error)
    }
}
