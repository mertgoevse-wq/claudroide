package org.claudroide.app.feature.linux.desktop.rfb

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.Socket

/**
 * RFB 3.8 client (RFC 6143). Implements the handshake, initialization and
 * the display-side messages needed for a working viewer:
 * SetPixelFormat, SetEncodings (Raw), FramebufferUpdateRequest,
 * FramebufferUpdate (Raw + DesktopSize pseudo-encoding), KeyEvent,
 * PointerEvent.
 *
 * CopyRect/RRE/Hextile/ZRLE decoders are future work; Raw is requested as
 * the working baseline and most servers can serve Raw when requested.
 */
class RfbClient(
    private val input: DataInputStream,
    private val output: DataOutputStream,
    private val shared: Boolean = true
) {
    val framebuffer = Framebuffer(1, 1) // resized at ServerInit
    var desktopName: String = ""
        private set

    private var protocolMinor: Int = 8
    private val writeLock = Any()

    constructor(socket: Socket, shared: Boolean = true) :
        this(
            DataInputStream(BufferedInputStream(socket.getInputStream())),
            DataOutputStream(BufferedOutputStream(socket.getOutputStream())),
            shared
        )

    // ------------------------------------------------------------------
    // Handshake (RFC 6143 §7.1)
    // ------------------------------------------------------------------

    /** Reads the server ProtocolVersion, replies with the version we will use. */
    fun negotiateVersion() {
        val server = String(readExactly(input, 12), Charsets.US_ASCII)
        if (!server.startsWith("RFB ") || !server.endsWith("\n")) {
            throw IOException("Malformed ProtocolVersion: '$server'")
        }
        val major = server.substring(4, 7).toIntOrNull()
            ?: throw IOException("Bad major version in '$server'")
        val minor = server.substring(8, 11).toIntOrNull()
            ?: throw IOException("Bad minor version in '$server'")
        require(major == 3) { "Unsupported RFB major version $major (unknown versions must be treated as 3.3)" }

        protocolMinor = when {
            minor >= 8 -> 8
            minor == 7 -> 7
            else -> 3
        }
        val reply = "RFB 003.00$protocolMinor\n"
        output.write(reply.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    /**
     * Security handshake (§7.1.2). Returns null on success or the failure
     * reason. Chooses VNC auth (2) when offered with a [password], else None (1).
     */
    fun handshakeSecurity(password: String?): String? {
        if (protocolMinor >= 7) {
            val count = input.readUnsignedByte()
            if (count == 0) return readFailureReason(input)
            val types = ByteArray(count)
            input.readFully(types)
            val chosen: Int = when {
                types.contains(SEC_VNC_AUTH.toByte()) && password != null -> SEC_VNC_AUTH
                types.contains(SEC_NONE.toByte()) -> SEC_NONE
                else -> return "Server offers no supported security type"
            }
            output.writeByte(chosen)
            output.flush()
            return performSecurity(chosen, password)
        } else {
            // 3.3: server decides unilaterally
            val chosen = input.readInt()
            return when (chosen) {
                SEC_NONE, SEC_VNC_AUTH -> performSecurity(chosen, password)
                else -> "Server chose unsupported security type $chosen"
            }
        }
    }

    private fun performSecurity(chosen: Int, password: String?): String? {
        when (chosen) {
            SEC_NONE -> {
                // 3.8 None: SecurityResult follows; 3.7 None: none is sent
                return if (protocolMinor >= 8) readSecurityResult(input) else null
            }
            SEC_VNC_AUTH -> {
                val challenge = ByteArray(16)
                input.readFully(challenge)
                output.write(VncAuth.challengeResponse(challenge, password ?: ""))
                output.flush()
                return readSecurityResult(input)
            }
            else -> return "Unsupported security type $chosen"
        }
    }

    // ------------------------------------------------------------------
    // Initialization (§7.3)
    // ------------------------------------------------------------------

    fun initialize() {
        output.writeByte(if (shared) 1 else 0)
        val width = input.readUnsignedShort()
        val height = input.readUnsignedShort()
        val format = RfbPixelFormat.readFrom(input)
        val nameLen = input.readInt()
        require(nameLen >= 0 && nameLen < 1 shl 16) { "Unreasonable desktop name length $nameLen" }
        desktopName = String(readExactly(input, nameLen), Charsets.UTF_8)

        framebuffer.resize(width, height)
        framebuffer.format = format
    }

    // ------------------------------------------------------------------
    // Client -> server messages (§7.5)
    // ------------------------------------------------------------------

    /** Announce BGRA32 pixels so decoded values map straight to ARGB ints. */
    fun setPixelFormat(format: RfbPixelFormat = RfbPixelFormat.BGRA32) = synchronized(writeLock) {
        output.writeByte(0)
        output.writeByte(0); output.writeByte(0); output.writeByte(0)
        format.writeTo(output)
        output.flush()
        framebuffer.format = format
    }

    fun setEncodings(vararg encodings: Int) = synchronized(writeLock) {
        output.writeByte(2)
        output.writeByte(0)
        output.writeShort(encodings.size)
        encodings.forEach { output.writeInt(it) }
        output.flush()
    }

    fun requestFramebufferUpdate(
        incremental: Boolean,
        x: Int = 0,
        y: Int = 0,
        w: Int = framebuffer.width,
        h: Int = framebuffer.height
    ) = synchronized(writeLock) {
        output.writeByte(3)
        output.writeByte(if (incremental) 1 else 0)
        output.writeShort(x)
        output.writeShort(y)
        output.writeShort(w)
        output.writeShort(h)
        output.flush()
    }

    fun sendKeyEvent(keysym: Int, down: Boolean) = synchronized(writeLock) {
        output.writeByte(4)
        output.writeByte(if (down) 1 else 0)
        output.writeByte(0); output.writeByte(0)
        output.writeInt(keysym)
        output.flush()
    }

    fun sendPointerEvent(x: Int, y: Int, buttonMask: Int) = synchronized(writeLock) {
        output.writeByte(5)
        output.writeByte(buttonMask)
        output.writeShort(x)
        output.writeShort(y)
        output.flush()
    }

    // ------------------------------------------------------------------
    // Server -> client messages (§7.6)
    // ------------------------------------------------------------------

    /**
     * Reads the next server message. Returns a [FramebufferUpdate] when one
     * arrives; Bell / ServerCutText / SetColorMapEntries are consumed and
     * produce null.
     */
    fun readServerMessage(): FramebufferUpdate? {
        when (val type = input.readUnsignedByte()) {
            MSG_FRAMEBUFFER_UPDATE -> return readFramebufferUpdate()
            MSG_BELL -> return null
            MSG_SERVER_CUT_TEXT -> {
                input.skipBytes(3)
                val len = input.readInt()
                if (len > 0) readExactly(input, len)
                return null
            }
            MSG_SET_COLOR_MAP -> {
                input.skipBytes(1)
                val n = input.readUnsignedShort()
                input.skipBytes(2) // first-color (unused)
                input.skipBytes(n * 6)
                return null
            }
            else -> throw IOException("Unknown server message type $type")
        }
    }

    private fun readFramebufferUpdate(): FramebufferUpdate {
        input.skipBytes(1) // padding
        val rectCount = input.readUnsignedShort()
        val rects = mutableListOf<Rect>()
        for (i in 0 until rectCount) {
            val x = input.readUnsignedShort()
            val y = input.readUnsignedShort()
            val w = input.readUnsignedShort()
            val h = input.readUnsignedShort()
            val enc = input.readInt()
            when (enc) {
                ENC_RAW -> {
                    // Validate before allocating: payload size derives from server-controlled values.
                    require(x >= 0 && y >= 0 && w >= 0 && h >= 0 &&
                        x.toLong() + w <= framebuffer.width && y.toLong() + h <= framebuffer.height) {
                        "Raw update rect ($x,$y ${w}x$h) outside ${framebuffer.width}x${framebuffer.height} framebuffer"
                    }
                    val payload = ByteArray(w * h * framebuffer.format.bytesPerPixel)
                    input.readFully(payload)
                    framebuffer.decodeRawRect(payload, 0, x, y, w, h)
                    rects.add(Rect(x, y, w, h, enc))
                }
                ENC_DESKTOP_SIZE -> {
                    framebuffer.resize(w, h)
                    rects.add(Rect(0, 0, w, h, enc))
                }
                else -> throw IOException("Unsupported encoding $enc (only Raw and DesktopSize implemented)")
            }
        }
        return FramebufferUpdate(rects)
    }

    companion object {
        const val SEC_NONE = 1
        const val SEC_VNC_AUTH = 2

        const val ENC_RAW = 0
        const val ENC_COPYRECT = 1
        const val ENC_RRE = 2
        const val ENC_HEXTILE = 5
        const val ENC_ZRLE = 16
        const val ENC_DESKTOP_SIZE = -223

        const val MSG_FRAMEBUFFER_UPDATE = 0
        const val MSG_SET_COLOR_MAP = 1
        const val MSG_BELL = 2
        const val MSG_SERVER_CUT_TEXT = 3

        fun readExactly(stream: InputStream, count: Int): ByteArray {
            val buf = ByteArray(count)
            var read = 0
            while (read < count) {
                val n = stream.read(buf, read, count - read)
                if (n < 0) throw EOFException("Stream ended after $read of $count bytes")
                read += n
            }
            return buf
        }

        fun readSecurityResult(input: DataInputStream): String? {
            val status = input.readInt()
            if (status == 0) return null
            val len = input.readInt()
            require(len >= 0 && len < 1 shl 20) { "Unreasonable reason length $len" }
            return String(readExactly(input, len), Charsets.UTF_8)
        }

        fun readFailureReason(input: DataInputStream): String {
            val len = input.readInt()
            require(len >= 0 && len < 1 shl 20) { "Unreasonable reason length $len" }
            return String(readExactly(input, len), Charsets.UTF_8)
        }
    }
}

data class Rect(val x: Int, val y: Int, val w: Int, val h: Int, val encoding: Int)

data class FramebufferUpdate(val rects: List<Rect>)
