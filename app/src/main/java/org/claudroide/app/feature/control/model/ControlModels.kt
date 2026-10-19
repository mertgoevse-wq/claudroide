package org.claudroide.app.feature.control.model

/**
 * Capability states for Android platform features.
 *
 * Exposes clear capability states according to system security constraints:
 * - [SUPPORTED]: Capability is available and ready for immediate execution.
 * - [SUPPORTED_WITH_USER_PERMISSION]: Feature is technically available on the device,
 *   but requires the user to explicitly grant system or runtime permission (e.g. AccessibilityService, Overlay).
 * - [LIMITED]: Available with restricted fidelity (e.g., fallback screenshot via MediaProjection vs direct accessibility API).
 * - [UNAVAILABLE]: Hardware, OS version or platform policy does not support this capability.
 */
enum class CapabilityState {
    SUPPORTED,
    SUPPORTED_WITH_USER_PERMISSION,
    LIMITED,
    UNAVAILABLE
}

/**
 * Android control and device capabilities.
 */
enum class ControlCapability(val id: String, val label: String, val germanLabel: String) {
    ACCESSIBILITY_CONTROL("accessibility_control", "Accessibility Control", "Bedienungshilfen-Steuerung"),
    SCREEN_OBSERVATION("screen_observation", "Screen Observation & Tree", "Bildschirm-Beobachtung & Baum"),
    GESTURE_DISPATCH("gesture_dispatch", "Touch & Gesture Dispatch", "Touch- & Gesten-Ausführung"),
    SCREENSHOT_CAPTURE("screenshot_capture", "Screenshot Capture", "Bildschirmaufnahme"),
    APP_LAUNCHING("app_launching", "Application Launching", "App-Start"),
    CLIPBOARD_ACCESS("clipboard_access", "Clipboard Access", "Zwischenablage"),
    AUDIO_ANALYSIS("audio_analysis", "Audio Analysis", "Audio-Analyse"),
    MIDI_GENERATION("midi_generation", "MIDI Generation", "MIDI-Generierung"),
    SHELL_EXECUTION("shell_execution", "Terminal & Shell Execution", "Terminal- & Befehlsausführung")
}

/**
 * Physical or virtual screen display metrics.
 */
data class ScreenDimensions(
    val width: Int,
    val height: Int,
    val densityDpi: Int = 420
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1.0f
}

/**
 * 2D bounding rectangle in absolute screen coordinates.
 */
data class RectBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val centerX: Float get() = left + width / 2.0f
    val centerY: Float get() = top + height / 2.0f

    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }

    val isEmpty: Boolean get() = width == 0 || height == 0
}

/**
 * A structured node representing a visible or interactive element on the screen.
 */
data class ScreenElement(
    val id: String,
    val text: String? = null,
    val contentDescription: String? = null,
    val className: String = "android.view.View",
    val packageName: String = "",
    val bounds: RectBounds = RectBounds(0, 0, 0, 0),
    val isClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEditable: Boolean = false,
    val isFocused: Boolean = false,
    val isSelected: Boolean = false,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
    val viewIdResourceName: String? = null,
    val childCount: Int = 0
) {
    /** Best human-readable identifier for AI reasoning. */
    val readableLabel: String
        get() = text?.takeIf { it.isNotBlank() }
            ?: contentDescription?.takeIf { it.isNotBlank() }
            ?: viewIdResourceName?.substringAfter(":id/")
            ?: className.substringAfterLast('.')

    val isInteractive: Boolean
        get() = isClickable || isScrollable || isEditable
}

/**
 * Snapshot of visible Android screen state.
 */
data class ScreenSnapshot(
    val packageName: String,
    val activityName: String? = null,
    val dimensions: ScreenDimensions,
    val elements: List<ScreenElement>,
    val screenshotBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val visibleText: List<String>
        get() = elements.mapNotNull { it.text?.trim() }.filter { it.isNotEmpty() }

    val interactiveElements: List<ScreenElement>
        get() = elements.filter { it.isInteractive }

    fun findElementById(id: String): ScreenElement? =
        elements.firstOrNull { it.id == id }

    fun findElementsByText(query: String, exact: Boolean = false): List<ScreenElement> {
        val q = query.trim().lowercase()
        return elements.filter { element ->
            val t = element.text?.lowercase() ?: ""
            val cd = element.contentDescription?.lowercase() ?: ""
            if (exact) t == q || cd == q
            else t.contains(q) || cd.contains(q)
        }
    }
}

/**
 * Direction for scroll gestures.
 */
enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}
