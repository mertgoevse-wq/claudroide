package org.claudroide.app.feature.control.tools

import org.claudroide.app.feature.control.model.ScrollDirection

/**
 * Concrete Android control actions supported by the Claudroide Agent.
 */
sealed class AndroidToolAction(val toolName: String) {

    data class Tap(val x: Float, val y: Float, val elementId: String? = null) :
        AndroidToolAction("android.tap")

    data class DoubleTap(val x: Float, val y: Float) :
        AndroidToolAction("android.double_tap")

    data class LongPress(val x: Float, val y: Float, val durationMs: Long = 1000L) :
        AndroidToolAction("android.long_press")

    data class Swipe(val fromX: Float, val fromY: Float, val toX: Float, val toY: Float, val durationMs: Long = 300L) :
        AndroidToolAction("android.swipe")

    data class Scroll(val direction: ScrollDirection) :
        AndroidToolAction("android.scroll")

    data class Type(val text: String, val targetElementId: String? = null) :
        AndroidToolAction("android.type")

    data object Back : AndroidToolAction("android.back")
    data object Home : AndroidToolAction("android.home")
    data object Recents : AndroidToolAction("android.recents")

    data class LaunchApp(val packageName: String) :
        AndroidToolAction("android.launch_app")

    data class Wait(val durationMs: Long) :
        AndroidToolAction("android.wait")

    data class Observe(val includeScreenshot: Boolean = false) :
        AndroidToolAction("android.observe")

    data object Screenshot :
        AndroidToolAction("android.screenshot")

    data class FindText(val query: String, val exact: Boolean = false) :
        AndroidToolAction("android.find_text")

    data class FindElement(val query: String) :
        AndroidToolAction("android.find_element")
}
