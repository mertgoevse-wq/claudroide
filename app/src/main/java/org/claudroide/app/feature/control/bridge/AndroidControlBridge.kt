package org.claudroide.app.feature.control.bridge

import org.claudroide.app.feature.control.model.CapabilityState
import org.claudroide.app.feature.control.model.ControlCapability
import org.claudroide.app.feature.control.model.ScreenDimensions
import org.claudroide.app.feature.control.model.ScreenSnapshot
import org.claudroide.app.feature.control.model.ScrollDirection

/**
 * Result of taking a screenshot.
 */
sealed class ScreenshotResult {
    data class Success(val base64Data: String, val width: Int, val height: Int) : ScreenshotResult()
    data class Failure(val reason: String, val capabilityState: CapabilityState) : ScreenshotResult()
}

/**
 * Core interface for inspecting and controlling the Android device.
 */
interface AndroidControlBridge {

    /** Query capability state according to Android permission model. */
    fun getCapabilityState(capability: ControlCapability): CapabilityState

    /** Whether the underlying AccessibilityService is actively bound and running. */
    fun isServiceActive(): Boolean

    /** Retrieve latest screen dimensions. */
    fun getScreenDimensions(): ScreenDimensions

    /** Extract structured screen tree from active window. */
    suspend fun captureScreenTree(includeScreenshot: Boolean = false): ScreenSnapshot?

    /** Take a full device screenshot. */
    suspend fun takeScreenshot(): ScreenshotResult

    /** Perform a single tap at screen coordinate (x, y). */
    suspend fun tap(x: Float, y: Float): Boolean

    /** Perform a double tap at screen coordinate (x, y). */
    suspend fun doubleTap(x: Float, y: Float): Boolean

    /** Perform a long press at (x, y) with duration in milliseconds. */
    suspend fun longPress(x: Float, y: Float, durationMs: Long = 1000L): Boolean

    /** Perform a swipe from (fromX, fromY) to (toX, toY). */
    suspend fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, durationMs: Long = 300L): Boolean

    /** Perform a scroll gesture in the given direction. */
    suspend fun scroll(direction: ScrollDirection): Boolean

    /** Type text into currently focused element or specified target node. */
    suspend fun typeText(text: String, targetElementId: String? = null): Boolean

    /** Global system Back button. */
    suspend fun pressBack(): Boolean

    /** Global system Home button. */
    suspend fun pressHome(): Boolean

    /** Global system Recents (App Overview) button. */
    suspend fun pressRecents(): Boolean

    /** Launch application by package name via Android Intent. */
    suspend fun launchApp(packageName: String): Boolean

    /** Open app by matching common application labels. */
    suspend fun openAppByName(appName: String): Boolean
}
