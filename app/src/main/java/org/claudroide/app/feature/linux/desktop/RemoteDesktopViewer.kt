package org.claudroide.app.feature.linux.desktop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import org.claudroide.app.feature.linux.desktop.rfb.Framebuffer
import org.claudroide.app.feature.linux.desktop.rfb.RfbClient
import org.claudroide.app.feature.linux.desktop.rfb.RfbPixelFormat
import org.claudroide.app.feature.linux.input.InputBridging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.IntBuffer

/**
 * Live VNC viewer for DESKTOP-001 (local Mode A/B) and REMOTE-001 (Mode C over
 * network). Connects with the real RFB 3.8 client, requests Raw encoding, and
 * renders decoded framebuffer updates into a Bitmap on every frame.
 *
 * The update loop runs on an IO coroutine; drawing is marshalled to the UI
 * thread. CopyRect/RRE/Hextile/ZRLE remain future work — servers that refuse
 * Raw fail the connection with a clear error message.
 */
class RemoteDesktopViewer(context: Context) : View(context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val errorPaint = Paint().apply {
        color = Color.RED
        textSize = 40f
    }

    val inputBridging = InputBridging().apply {
        pointerListener = { event ->
            val client = rfbClient
            if (client != null) {
                scope.launch {
                    try {
                        client.sendPointerEvent(event.x, event.y, event.buttonMask)
                    } catch (_: Exception) {
                    }
                }
            }
        }
        keyListener = { event ->
            val client = rfbClient
            if (client != null) {
                scope.launch {
                    try {
                        client.sendKeyEvent(event.keysym, event.isDown)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private var connectionJob: Job? = null

    @Volatile
    var rfbClient: RfbClient? = null
        private set

    @Volatile
    private var currentFrame: Bitmap? = null

    @Volatile
    private var framebuffer: Framebuffer? = null

    @Volatile
    var isConnected: Boolean = false
        private set

    @Volatile
    var connectionError: String? = null
        private set

    var onConnectionStateChanged: ((isConnected: Boolean, error: String?) -> Unit)? = null

    /** Connects to a VNC server and starts the framebuffer update loop. */
    fun connectVnc(host: String, port: Int, password: String?) {
        disconnect()
        connectionJob = scope.launch {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                    socket.soTimeout = READ_TIMEOUT_MS
                    val client = RfbClient(
                        DataInputStream(socket.getInputStream().buffered()),
                        DataOutputStream(socket.getOutputStream().buffered())
                    )
                    client.negotiateVersion()
                    client.handshakeSecurity(password)?.let {
                        throw IOException("VNC auth failed: $it")
                    }
                    client.initialize()
                    client.setPixelFormat(RfbPixelFormat.BGRA32)
                    client.setEncodings(RfbClient.ENC_RAW)

                    rfbClient = client
                    framebuffer = client.framebuffer
                    isConnected = true
                    connectionError = null
                    mainHandler.post { onConnectionStateChanged?.invoke(true, null) }
                    rebuildBitmap(client.framebuffer)

                    // First request is non-incremental (full frame), then incremental.
                    var first = true
                    while (isActive) {
                        client.requestFramebufferUpdate(incremental = !first)
                        first = false
                        if (client.readServerMessage() != null) {
                            onFramebufferChanged(client.framebuffer)
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    val err = e.message ?: e.javaClass.simpleName
                    connectionError = err
                    isConnected = false
                    mainHandler.post {
                        onConnectionStateChanged?.invoke(false, err)
                        invalidate()
                    }
                }
            } finally {
                rfbClient = null
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        rfbClient = null
        isConnected = false
        framebuffer = null
        currentFrame = null
        connectionError = null
        mainHandler.post { onConnectionStateChanged?.invoke(false, null) }
        postInvalidate()
    }

    private fun onFramebufferChanged(fb: Framebuffer) {
        val bmp = currentFrame ?: return
        if (bmp.width != fb.width || bmp.height != fb.height) {
            rebuildBitmap(fb)
        } else {
            bmp.copyPixelsFromBuffer(IntBuffer.wrap(fb.pixels))
            mainHandler.post { invalidate() }
        }
    }

    private fun rebuildBitmap(fb: Framebuffer) {
        val bmp = Bitmap.createBitmap(fb.width, fb.height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(IntBuffer.wrap(fb.pixels))
        currentFrame = bmp
        mainHandler.post { invalidate() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when {
            !isConnected && connectionError != null -> {
                canvas.drawColor(Color.BLACK)
                connectionError?.let { canvas.drawText("VNC error: $it", 20f, 60f, errorPaint) }
            }
            isConnected -> {
                val frame = currentFrame
                if (frame != null) {
                    val srcRect = Rect(0, 0, frame.width, frame.height)
                    val dstRect = Rect(0, 0, width, height)
                    canvas.drawBitmap(frame, srcRect, dstRect, null)
                } else {
                    canvas.drawColor(Color.BLACK)
                }
            }
            else -> canvas.drawColor(Color.DKGRAY)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isConnected) return super.onTouchEvent(event)
        val fb = framebuffer ?: return super.onTouchEvent(event)
        val pointer = inputBridging.translateTouch(
            touchX = event.x,
            touchY = event.y,
            viewWidth = width,
            viewHeight = height,
            fbWidth = fb.width,
            fbHeight = fb.height,
            action = event.actionMasked,
            pointerCount = event.pointerCount
        )
        if (pointer != null) {
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isConnected) {
            val mapped = inputBridging.translateKey(keyCode, isDown = true, unicodeChar = event.unicodeChar)
            if (mapped != null) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isConnected) {
            val mapped = inputBridging.translateKey(keyCode, isDown = false, unicodeChar = event.unicodeChar)
            if (mapped != null) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        connectionJob?.cancel()
        scope.cancel()
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 8000
        private const val READ_TIMEOUT_MS = 30000
    }
}
