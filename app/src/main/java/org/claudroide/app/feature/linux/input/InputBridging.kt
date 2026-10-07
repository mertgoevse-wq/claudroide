package org.claudroide.app.feature.linux.input

/**
 * Translates touchscreen gestures, clicks, and physical/virtual keyboard events
 * on the Android host into standard RFC 6143 RFB / X11 pointer and key events
 * for both local (Mode A/B) and remote (Mode C) Linux desktop sessions.
 */
class InputBridging {

    data class PointerEvent(
        val x: Int,
        val y: Int,
        val buttonMask: Int
    )

    data class KeyEvent(
        val keysym: Int,
        val isDown: Boolean
    )

    var pointerListener: ((PointerEvent) -> Unit)? = null
    var keyListener: ((KeyEvent) -> Unit)? = null

    // Track active pointer buttons
    private var currentButtonMask = 0

    /**
     * Translates a touch/motion event coordinate and action to an RFB pointer event.
     * Coordinates are scaled proportionally from view space to framebuffer space.
     *
     * @param touchX X coordinate in view pixels
     * @param touchY Y coordinate in view pixels
     * @param viewWidth Width of the viewing widget
     * @param viewHeight Height of the viewing widget
     * @param fbWidth Width of the remote framebuffer
     * @param fbHeight Height of the remote framebuffer
     * @param action MotionEvent action code (e.g. ACTION_DOWN, ACTION_MOVE, ACTION_UP)
     * @param pointerCount Number of active touch pointers (for multi-touch gestures)
     */
    fun translateTouch(
        touchX: Float,
        touchY: Float,
        viewWidth: Int,
        viewHeight: Int,
        fbWidth: Int,
        fbHeight: Int,
        action: Int,
        pointerCount: Int = 1
    ): PointerEvent? {
        if (viewWidth <= 0 || viewHeight <= 0 || fbWidth <= 0 || fbHeight <= 0) {
            return null
        }

        val clampedX = touchX.coerceIn(0f, viewWidth.toFloat())
        val clampedY = touchY.coerceIn(0f, viewHeight.toFloat())

        val scaledX = ((clampedX / viewWidth) * fbWidth).toInt().coerceIn(0, fbWidth - 1)
        val scaledY = ((clampedY / viewHeight) * fbHeight).toInt().coerceIn(0, fbHeight - 1)

        val mask = when (action) {
            ACTION_DOWN -> {
                // Secondary touch (e.g. 2 fingers) maps to right click
                if (pointerCount >= 2) BUTTON_RIGHT else BUTTON_LEFT
            }
            ACTION_POINTER_DOWN -> {
                BUTTON_RIGHT
            }
            ACTION_MOVE -> {
                if (currentButtonMask != 0) currentButtonMask else BUTTON_LEFT
            }
            ACTION_UP, ACTION_CANCEL -> {
                BUTTON_NONE
            }
            ACTION_SCROLL_UP -> {
                BUTTON_WHEEL_UP
            }
            ACTION_SCROLL_DOWN -> {
                BUTTON_WHEEL_DOWN
            }
            else -> currentButtonMask
        }

        currentButtonMask = if (action == ACTION_UP || action == ACTION_CANCEL) BUTTON_NONE else mask

        val event = PointerEvent(x = scaledX, y = scaledY, buttonMask = mask)
        pointerListener?.invoke(event)
        return event
    }

    /**
     * Translates an Android key code and Unicode character to an X11 keysym.
     *
     * @param keyCode Android KeyEvent keycode
     * @param isDown True if key is pressed down, false if released
     * @param unicodeChar Character code if available (e.g. from event.unicodeChar)
     */
    fun translateKey(
        keyCode: Int,
        isDown: Boolean,
        unicodeChar: Int = 0
    ): KeyEvent? {
        val keysym = when {
            // First check special control/navigation keycodes
            keyCodeMap.containsKey(keyCode) -> keyCodeMap[keyCode]!!
            // If printable ASCII or Latin-1 character available
            unicodeChar in 0x0020..0x00FF -> unicodeChar
            // Unicode characters above Latin-1 map to 0x01000000 + codepoint (X11 convention)
            unicodeChar > 0x00FF -> 0x01000000 or unicodeChar
            // Letter keys fallback (A-Z -> a-z)
            keyCode in KEYCODE_A..KEYCODE_Z -> (keyCode - KEYCODE_A) + 0x0061
            // Number keys fallback (0-9)
            keyCode in KEYCODE_0..KEYCODE_9 -> (keyCode - KEYCODE_0) + 0x0030
            else -> return null
        }

        val event = KeyEvent(keysym = keysym, isDown = isDown)
        keyListener?.invoke(event)
        return event
    }

    /** Legacy bridge entry points */
    fun sendPointerEvent(x: Float, y: Float, action: Int) {
        translateTouch(x, y, 100, 100, 100, 100, action)
    }

    fun sendKeyEvent(keyCode: Int, isDown: Boolean) {
        translateKey(keyCode, isDown)
    }

    companion object {
        // RFC 6143 Pointer Button Masks
        const val BUTTON_NONE = 0
        const val BUTTON_LEFT = 1
        const val BUTTON_MIDDLE = 2
        const val BUTTON_RIGHT = 4
        const val BUTTON_WHEEL_UP = 8
        const val BUTTON_WHEEL_DOWN = 16

        // MotionEvent Action Constants (pure JVM friendly)
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        const val ACTION_CANCEL = 3
        const val ACTION_POINTER_DOWN = 5
        const val ACTION_POINTER_UP = 6
        const val ACTION_SCROLL_UP = 1008
        const val ACTION_SCROLL_DOWN = 1009

        // Android KeyEvent Constants
        private const val KEYCODE_0 = 7
        private const val KEYCODE_9 = 16
        private const val KEYCODE_A = 29
        private const val KEYCODE_Z = 54

        // X11 Keysym definitions
        const val KEYSYM_BACKSPACE = 0xFF08
        const val KEYSYM_TAB = 0xFF09
        const val KEYSYM_LINEFEED = 0xFF0A
        const val KEYSYM_RETURN = 0xFF0D
        const val KEYSYM_ESCAPE = 0xFF1B
        const val KEYSYM_DELETE = 0xFFFF
        const val KEYSYM_HOME = 0xFF50
        const val KEYSYM_LEFT = 0xFF51
        const val KEYSYM_UP = 0xFF52
        const val KEYSYM_RIGHT = 0xFF53
        const val KEYSYM_DOWN = 0xFF54
        const val KEYSYM_PAGE_UP = 0xFF55
        const val KEYSYM_PAGE_DOWN = 0xFF56
        const val KEYSYM_END = 0xFF57
        const val KEYSYM_INSERT = 0xFF63
        const val KEYSYM_SHIFT_L = 0xFFE1
        const val KEYSYM_SHIFT_R = 0xFFE2
        const val KEYSYM_CONTROL_L = 0xFFE3
        const val KEYSYM_CONTROL_R = 0xFFE4
        const val KEYSYM_ALT_L = 0xFFE9
        const val KEYSYM_ALT_R = 0xFFEA
        const val KEYSYM_SUPER_L = 0xFFEB
        const val KEYSYM_SPACE = 0x0020

        private val keyCodeMap = mapOf(
            66 to KEYSYM_RETURN,     // KEYCODE_ENTER
            160 to KEYSYM_RETURN,    // KEYCODE_NUMPAD_ENTER
            67 to KEYSYM_BACKSPACE,  // KEYCODE_DEL
            112 to KEYSYM_DELETE,    // KEYCODE_FORWARD_DEL
            61 to KEYSYM_TAB,        // KEYCODE_TAB
            111 to KEYSYM_ESCAPE,    // KEYCODE_ESCAPE
            19 to KEYSYM_UP,         // KEYCODE_DPAD_UP
            20 to KEYSYM_DOWN,       // KEYCODE_DPAD_DOWN
            21 to KEYSYM_LEFT,       // KEYCODE_DPAD_LEFT
            22 to KEYSYM_RIGHT,      // KEYCODE_DPAD_RIGHT
            92 to KEYSYM_PAGE_UP,    // KEYCODE_PAGE_UP
            93 to KEYSYM_PAGE_DOWN,  // KEYCODE_PAGE_DOWN
            122 to KEYSYM_HOME,      // KEYCODE_MOVE_HOME
            123 to KEYSYM_END,       // KEYCODE_MOVE_END
            124 to KEYSYM_INSERT,    // KEYCODE_INSERT
            59 to KEYSYM_SHIFT_L,    // KEYCODE_SHIFT_LEFT
            60 to KEYSYM_SHIFT_R,    // KEYCODE_SHIFT_RIGHT
            113 to KEYSYM_CONTROL_L, // KEYCODE_CTRL_LEFT
            114 to KEYSYM_CONTROL_R, // KEYCODE_CTRL_RIGHT
            57 to KEYSYM_ALT_L,      // KEYCODE_ALT_LEFT
            58 to KEYSYM_ALT_R,      // KEYCODE_ALT_RIGHT
            62 to KEYSYM_SPACE       // KEYCODE_SPACE
        )
    }
}
