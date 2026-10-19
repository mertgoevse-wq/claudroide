package org.claudroide.app.feature.control.bridge

import org.claudroide.app.feature.control.model.CapabilityState
import org.claudroide.app.feature.control.model.ControlCapability
import org.claudroide.app.feature.control.model.RectBounds
import org.claudroide.app.feature.control.model.ScreenDimensions
import org.claudroide.app.feature.control.model.ScreenElement
import org.claudroide.app.feature.control.model.ScreenSnapshot
import org.claudroide.app.feature.control.model.ScrollDirection

/**
 * High-fidelity test double for [AndroidControlBridge] for unit tests and local JVM simulation.
 */
class FakeAndroidControlBridge(
    var isEnabled: Boolean = true,
    var dimensions: ScreenDimensions = ScreenDimensions(width = 1080, height = 2340, densityDpi = 420),
    initialSnapshot: ScreenSnapshot? = null
) : AndroidControlBridge {

    var currentSnapshot: ScreenSnapshot? = initialSnapshot ?: createDefaultSnapshot()
    val actionLog = mutableListOf<String>()

    override fun getCapabilityState(capability: ControlCapability): CapabilityState {
        return if (isEnabled) CapabilityState.SUPPORTED
        else CapabilityState.SUPPORTED_WITH_USER_PERMISSION
    }

    override fun isServiceActive(): Boolean = isEnabled

    override fun getScreenDimensions(): ScreenDimensions = dimensions

    override suspend fun captureScreenTree(includeScreenshot: Boolean): ScreenSnapshot? {
        actionLog.add("captureScreenTree(includeScreenshot=$includeScreenshot)")
        return currentSnapshot
    }

    override suspend fun takeScreenshot(): ScreenshotResult {
        actionLog.add("takeScreenshot")
        return if (isEnabled) {
            ScreenshotResult.Success(
                base64Data = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
                width = dimensions.width,
                height = dimensions.height
            )
        } else {
            ScreenshotResult.Failure("Accessibility service disabled", CapabilityState.SUPPORTED_WITH_USER_PERMISSION)
        }
    }

    override suspend fun tap(x: Float, y: Float): Boolean {
        actionLog.add("tap($x, $y)")
        if (!isEnabled) return false

        // Check if there is an element at (x, y) to toggle state or trigger interaction
        val hit = currentSnapshot?.elements?.firstOrNull { it.bounds.contains(x, y) && it.isClickable }
        if (hit != null) {
            actionLog.add("tap_hit:${hit.id}")
        }
        return true
    }

    override suspend fun doubleTap(x: Float, y: Float): Boolean {
        actionLog.add("doubleTap($x, $y)")
        return isEnabled
    }

    override suspend fun longPress(x: Float, y: Float, durationMs: Long): Boolean {
        actionLog.add("longPress($x, $y, $durationMs)")
        return isEnabled
    }

    override suspend fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, durationMs: Long): Boolean {
        actionLog.add("swipe($fromX, $fromY -> $toX, $toY, ${durationMs}ms)")
        return isEnabled
    }

    override suspend fun scroll(direction: ScrollDirection): Boolean {
        actionLog.add("scroll($direction)")
        return isEnabled
    }

    override suspend fun typeText(text: String, targetElementId: String?): Boolean {
        actionLog.add("typeText('$text', target=$targetElementId)")
        if (!isEnabled) return false

        // Simulate updating text on the editable element
        val snap = currentSnapshot
        if (snap != null) {
            val updated = snap.elements.map { elem ->
                if ((targetElementId != null && elem.id == targetElementId) || (targetElementId == null && elem.isFocused)) {
                    elem.copy(text = text)
                } else elem
            }
            currentSnapshot = snap.copy(elements = updated)
        }
        return true
    }

    override suspend fun pressBack(): Boolean {
        actionLog.add("pressBack")
        return isEnabled
    }

    override suspend fun pressHome(): Boolean {
        actionLog.add("pressHome")
        if (isEnabled) {
            currentSnapshot = ScreenSnapshot(
                packageName = "com.sec.android.app.launcher",
                activityName = "com.sec.android.app.launcher.activities.MainActivity",
                dimensions = dimensions,
                elements = listOf(
                    ScreenElement(
                        id = "home_app_drawer",
                        text = "Apps",
                        className = "android.widget.Button",
                        packageName = "com.sec.android.app.launcher",
                        bounds = RectBounds(400, 2000, 680, 2150),
                        isClickable = true
                    )
                )
            )
        }
        return isEnabled
    }

    override suspend fun pressRecents(): Boolean {
        actionLog.add("pressRecents")
        return isEnabled
    }

    override suspend fun launchApp(packageName: String): Boolean {
        actionLog.add("launchApp('$packageName')")
        if (isEnabled) {
            currentSnapshot = ScreenSnapshot(
                packageName = packageName,
                activityName = "$packageName.MainActivity",
                dimensions = dimensions,
                elements = listOf(
                    ScreenElement(
                        id = "$packageName:main_view",
                        text = "Welcome to $packageName",
                        className = "android.widget.TextView",
                        packageName = packageName,
                        bounds = RectBounds(50, 100, 1030, 300)
                    )
                )
            )
        }
        return isEnabled
    }

    override suspend fun openAppByName(appName: String): Boolean {
        actionLog.add("openAppByName('$appName')")
        val pkg = when (appName.lowercase()) {
            "cubasis", "cubasis 3" -> "com.steinberg.cubasis3"
            "fl studio", "fl studio mobile", "flm" -> "com.imageline.FLM"
            "chrome" -> "com.android.chrome"
            "files" -> "com.google.android.documentsui"
            else -> "com.example.${appName.lowercase().replace(" ", "")}"
        }
        return launchApp(pkg)
    }

    private fun createDefaultSnapshot(): ScreenSnapshot {
        return ScreenSnapshot(
            packageName = "org.claudroide.app",
            activityName = "org.claudroide.app.MainActivity",
            dimensions = dimensions,
            elements = listOf(
                ScreenElement(
                    id = "title_bar",
                    text = "ClauDroide Agent",
                    className = "android.widget.TextView",
                    packageName = "org.claudroide.app",
                    bounds = RectBounds(40, 60, 1040, 180)
                ),
                ScreenElement(
                    id = "btn_start_task",
                    text = "Start Task",
                    className = "android.widget.Button",
                    packageName = "org.claudroide.app",
                    bounds = RectBounds(100, 400, 980, 560),
                    isClickable = true
                )
            )
        )
    }
}
