package org.claudroide.app.feature.control.bridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import org.claudroide.app.feature.control.model.CapabilityState
import org.claudroide.app.feature.control.model.ControlCapability
import org.claudroide.app.feature.control.model.ScreenDimensions
import org.claudroide.app.feature.control.model.ScreenSnapshot
import org.claudroide.app.feature.control.model.ScrollDirection
import org.claudroide.app.feature.control.screen.ScreenTreeParser
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

/**
 * Native Android [AccessibilityService] powering the Claudroide Android Control Bridge.
 */
class ClaudroideAccessibilityService : AccessibilityService(), AndroidControlBridge {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBridgeRegistry.register(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event stream monitored for screen transitions
    }

    override fun onInterrupt() {
        // Handle accessibility interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        AccessibilityBridgeRegistry.unregister(this)
    }

    override fun getCapabilityState(capability: ControlCapability): CapabilityState {
        return when (capability) {
            ControlCapability.ACCESSIBILITY_CONTROL,
            ControlCapability.SCREEN_OBSERVATION,
            ControlCapability.GESTURE_DISPATCH,
            ControlCapability.APP_LAUNCHING,
            ControlCapability.CLIPBOARD_ACCESS -> CapabilityState.SUPPORTED

            ControlCapability.SCREENSHOT_CAPTURE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    CapabilityState.SUPPORTED
                } else {
                    CapabilityState.LIMITED
                }
            }

            ControlCapability.AUDIO_ANALYSIS,
            ControlCapability.MIDI_GENERATION,
            ControlCapability.SHELL_EXECUTION -> CapabilityState.SUPPORTED
        }
    }

    override fun isServiceActive(): Boolean = true

    override fun getScreenDimensions(): ScreenDimensions {
        val dm = resources.displayMetrics
        return ScreenDimensions(
            width = dm.widthPixels,
            height = dm.heightPixels,
            densityDpi = dm.densityDpi
        )
    }

    override suspend fun captureScreenTree(includeScreenshot: Boolean): ScreenSnapshot? {
        val root = rootInActiveWindow ?: return null
        val dimensions = getScreenDimensions()
        val screenshotBase64 = if (includeScreenshot) {
            (takeScreenshot() as? org.claudroide.app.feature.control.bridge.ScreenshotResult.Success)?.base64Data
        } else null

        val snapshot = ScreenTreeParser.parse(
            rootNode = root,
            dimensions = dimensions,
            screenshotBase64 = screenshotBase64
        )
        root.recycle()
        return snapshot
    }

    override suspend fun takeScreenshot(): org.claudroide.app.feature.control.bridge.ScreenshotResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return org.claudroide.app.feature.control.bridge.ScreenshotResult.Failure(
                reason = "AccessibilityService.takeScreenshot requires Android 11+ (API 30)",
                capabilityState = CapabilityState.LIMITED
            )
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: android.accessibilityservice.AccessibilityService.ScreenshotResult) {
                            try {
                                val hardwareBuffer = screenshot.hardwareBuffer
                                val bitmap = Bitmap.wrapHardwareBuffer(
                                    hardwareBuffer,
                                    screenshot.colorSpace
                                )?.copy(Bitmap.Config.ARGB_8888, false)
                                hardwareBuffer.close()

                                if (bitmap != null) {
                                    val stream = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                                    val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                                    continuation.resume(
                                        org.claudroide.app.feature.control.bridge.ScreenshotResult.Success(
                                            base64Data = b64,
                                            width = bitmap.width,
                                            height = bitmap.height
                                        )
                                    )
                                } else {
                                    continuation.resume(
                                        org.claudroide.app.feature.control.bridge.ScreenshotResult.Failure(
                                            "Bitmap wrapping failed",
                                            CapabilityState.SUPPORTED
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                continuation.resume(
                                    org.claudroide.app.feature.control.bridge.ScreenshotResult.Failure(
                                        "Screenshot conversion error: ${e.message}",
                                        CapabilityState.SUPPORTED
                                    )
                                )
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            continuation.resume(
                                org.claudroide.app.feature.control.bridge.ScreenshotResult.Failure(
                                    "Screenshot failed with error code $errorCode",
                                    CapabilityState.SUPPORTED
                                )
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                continuation.resume(
                    org.claudroide.app.feature.control.bridge.ScreenshotResult.Failure(
                        "takeScreenshot threw exception: ${e.message}",
                        CapabilityState.SUPPORTED
                    )
                )
            }
        }
    }

    override suspend fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureAsync(gesture)
    }

    override suspend fun doubleTap(x: Float, y: Float): Boolean {
        val firstTap = tap(x, y)
        if (!firstTap) return false
        kotlinx.coroutines.delay(100)
        return tap(x, y)
    }

    override suspend fun longPress(x: Float, y: Float, durationMs: Long): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(500L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureAsync(gesture)
    }

    override suspend fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, durationMs: Long): Boolean {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(100L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureAsync(gesture)
    }

    override suspend fun scroll(direction: ScrollDirection): Boolean {
        val dim = getScreenDimensions()
        val cx = dim.width / 2.0f
        val cy = dim.height / 2.0f
        val distY = dim.height * 0.35f
        val distX = dim.width * 0.35f

        return when (direction) {
            ScrollDirection.DOWN -> swipe(cx, cy + distY, cx, cy - distY, 300)
            ScrollDirection.UP -> swipe(cx, cy - distY, cx, cy + distY, 300)
            ScrollDirection.RIGHT -> swipe(cx - distX, cy, cx + distX, cy, 300)
            ScrollDirection.LEFT -> swipe(cx + distX, cy, cx - distX, cy, 300)
        }
    }

    override suspend fun typeText(text: String, targetElementId: String?): Boolean {
        val root = rootInActiveWindow ?: return false
        val targetNode = if (targetElementId != null) {
            findNodeById(root, targetElementId)
        } else {
            root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }

        return if (targetNode != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            targetNode.recycle()
            root.recycle()
            result
        } else {
            root.recycle()
            false
        }
    }

    override suspend fun pressBack(): Boolean =
        performGlobalAction(GLOBAL_ACTION_BACK)

    override suspend fun pressHome(): Boolean =
        performGlobalAction(GLOBAL_ACTION_HOME)

    override suspend fun pressRecents(): Boolean =
        performGlobalAction(GLOBAL_ACTION_RECENTS)

    override suspend fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun openAppByName(appName: String): Boolean {
        val pm = packageManager
        val query = appName.trim().lowercase()
        val installedApps = pm.getInstalledApplications(0)

        for (appInfo in installedApps) {
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (label == query || label.contains(query)) {
                return launchApp(appInfo.packageName)
            }
        }
        return false
    }

    private suspend fun dispatchGestureAsync(gesture: GestureDescription): Boolean =
        suspendCancellableCoroutine { continuation ->
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        continuation.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        continuation.resume(false)
                    }
                },
                null
            )
        }

    private fun findNodeById(root: AccessibilityNodeInfo, targetId: String): AccessibilityNodeInfo? {
        val viewId = targetId.substringAfter(":", "")
        if (viewId.isNotEmpty()) {
            val matches = root.findAccessibilityNodeInfosByViewId(viewId)
            if (!matches.isNullOrEmpty()) {
                return matches.first()
            }
        }
        return null
    }
}
