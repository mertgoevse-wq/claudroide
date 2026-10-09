package org.claudroide.app.feature.control.bridge

import org.claudroide.app.feature.control.model.CapabilityState
import org.claudroide.app.feature.control.model.ControlCapability
import org.claudroide.app.feature.control.model.ScreenDimensions
import org.claudroide.app.feature.control.model.ScreenSnapshot
import org.claudroide.app.feature.control.model.ScrollDirection

/**
 * Global registry and access point for Android control capabilities.
 */
object AccessibilityBridgeRegistry {

    private var activeBridge: AndroidControlBridge? = null

    /** Fallback bridge when no service is active */
    private val disabledBridge = object : AndroidControlBridge {
        override fun getCapabilityState(capability: ControlCapability): CapabilityState =
            CapabilityState.SUPPORTED_WITH_USER_PERMISSION

        override fun isServiceActive(): Boolean = false
        override fun getScreenDimensions(): ScreenDimensions = ScreenDimensions(1080, 2400)
        override suspend fun captureScreenTree(includeScreenshot: Boolean): ScreenSnapshot? = null
        override suspend fun takeScreenshot(): ScreenshotResult =
            ScreenshotResult.Failure("AccessibilityService is not active", CapabilityState.SUPPORTED_WITH_USER_PERMISSION)
        override suspend fun tap(x: Float, y: Float): Boolean = false
        override suspend fun doubleTap(x: Float, y: Float): Boolean = false
        override suspend fun longPress(x: Float, y: Float, durationMs: Long): Boolean = false
        override suspend fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, durationMs: Long): Boolean = false
        override suspend fun scroll(direction: ScrollDirection): Boolean = false
        override suspend fun typeText(text: String, targetElementId: String?): Boolean = false
        override suspend fun pressBack(): Boolean = false
        override suspend fun pressHome(): Boolean = false
        override suspend fun pressRecents(): Boolean = false
        override suspend fun launchApp(packageName: String): Boolean = false
        override suspend fun openAppByName(appName: String): Boolean = false
    }

    /** Register an active bridge (called by ClaudroideAccessibilityService or test harness). */
    fun register(bridge: AndroidControlBridge) {
        activeBridge = bridge
    }

    /** Unregister the bridge (called on service disconnect or test teardown). */
    fun unregister(bridge: AndroidControlBridge) {
        if (activeBridge === bridge) {
            activeBridge = null
        }
    }

    /** Get the currently active bridge, or the disabled fallback. */
    fun getBridge(): AndroidControlBridge = activeBridge ?: disabledBridge

    /** Check whether the accessibility service is active. */
    val isReady: Boolean
        get() = activeBridge?.isServiceActive() == true
}
