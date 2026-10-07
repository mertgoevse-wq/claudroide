package org.claudroide.app.feature.linux.terminal

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Socket-based PTY transport connecting to an SSH tunnel forwarder or PTY bridge.
 * Implements bidirectional streaming, ANSI window resize signaling, and clean lifecycle management.
 */
class SocketPtyTransport : TerminalTransport {

    override var state: TerminalSessionState = TerminalSessionState.DISCONNECTED
        private set

    override var failureReason: String? = null
        private set

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val isRunning = AtomicBoolean(false)
    private var readerThread: Thread? = null

    private var outputListener: ((ByteArray) -> Unit)? = null
    private var errorListener: ((ByteArray) -> Unit)? = null
    private var stateListener: ((TerminalSessionState, String?) -> Unit)? = null

    private var currentDimensions: TerminalDimensions = TerminalDimensions()

    override fun connect(host: String, port: Int, timeoutMs: Int): Boolean {
        if (isRunning.get()) return true

        transitionState(TerminalSessionState.CONNECTING)
        try {
            val s = Socket()
            s.connect(InetSocketAddress(host, port), timeoutMs)
            s.tcpNoDelay = true
            socket = s
            outputStream = s.getOutputStream()
            inputStream = s.getInputStream()
            transitionState(TerminalSessionState.CONNECTED)
            return true
        } catch (e: Exception) {
            failureReason = "Connection to $host:$port failed: ${e.message}"
            transitionState(TerminalSessionState.FAILED, failureReason)
            cleanupSocket()
            return false
        }
    }

    override fun openShell(dimensions: TerminalDimensions, termType: String): Boolean {
        val s = socket
        if (s == null || !s.isConnected || s.isClosed) {
            failureReason = "Socket is not connected"
            transitionState(TerminalSessionState.FAILED, failureReason)
            return false
        }

        transitionState(TerminalSessionState.STARTING_SHELL)
        currentDimensions = dimensions
        isRunning.set(true)

        // Start background I/O reader thread
        readerThread = thread(name = "mll-pty-reader", isDaemon = true) {
            val buffer = ByteArray(4096)
            try {
                val input = inputStream ?: return@thread
                while (isRunning.get() && !s.isClosed) {
                    val read = input.read(buffer)
                    if (read <= 0) {
                        // EOF reached: remote shell exited
                        break
                    }
                    val chunk = buffer.copyOf(read)
                    outputListener?.invoke(chunk)
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    failureReason = "PTY read error: ${e.message}"
                    errorListener?.invoke("Connection lost: ${e.message}\n".toByteArray(Charsets.UTF_8))
                }
            } finally {
                if (isRunning.get()) {
                    disconnect()
                }
            }
        }

        transitionState(TerminalSessionState.RUNNING)
        return true
    }

    override fun write(data: ByteArray) {
        if (!isRunning.get() || socket?.isClosed == true) return
        try {
            outputStream?.let { out ->
                synchronized(out) {
                    out.write(data)
                    out.flush()
                }
            }
        } catch (e: Exception) {
            failureReason = "Write error: ${e.message}"
            errorListener?.invoke("Write failed: ${e.message}\n".toByteArray(Charsets.UTF_8))
        }
    }

    override fun resize(dimensions: TerminalDimensions) {
        currentDimensions = dimensions
        if (isRunning.get() && socket?.isClosed == false) {
            sendResizeSequence(dimensions)
        }
    }

    private fun sendResizeSequence(dimensions: TerminalDimensions) {
        try {
            // Standard ANSI terminal window resize sequence: \u001b[8;rows;colst
            val ansiResize = "\u001b[8;${dimensions.rows};${dimensions.cols}t".toByteArray(Charsets.UTF_8)
            outputStream?.let { out ->
                synchronized(out) {
                    out.write(ansiResize)
                    out.flush()
                }
            }
        } catch (_: Exception) {
        }
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
        if (!isRunning.getAndSet(false) && state == TerminalSessionState.DISCONNECTED) {
            return
        }

        transitionState(TerminalSessionState.CLOSING)
        cleanupSocket()
        transitionState(TerminalSessionState.DISCONNECTED)
    }

    private fun cleanupSocket() {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        outputStream = null
        inputStream = null

        readerThread?.interrupt()
        readerThread = null
    }

    override fun isAlive(): Boolean {
        return isRunning.get() && socket?.isConnected == true && socket?.isClosed == false
    }

    private fun transitionState(newState: TerminalSessionState, error: String? = null) {
        state = newState
        stateListener?.invoke(newState, error)
    }
}
