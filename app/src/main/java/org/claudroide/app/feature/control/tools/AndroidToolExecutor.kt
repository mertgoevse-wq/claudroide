package org.claudroide.app.feature.control.tools

import kotlinx.coroutines.delay
import org.claudroide.app.feature.control.bridge.AndroidControlBridge
import org.claudroide.app.feature.control.bridge.ScreenshotResult
import org.claudroide.app.feature.control.model.ControlCapability
import org.claudroide.app.feature.control.model.ScreenSnapshot
import org.claudroide.app.feature.control.safety.BlacklistVerdict
import org.claudroide.app.feature.control.safety.ControlBlacklistPolicy
import org.claudroide.app.feature.control.safety.EmergencyStopController

/**
 * Result of executing an Android tool action through the executor.
 */
data class ToolExecutionResponse(
    val action: AndroidToolAction,
    val verification: ActionVerificationResult,
    val latestSnapshot: ScreenSnapshot?,
    val screenshotBase64: String? = null
)

/**
 * Orchestrator executing the complete Precondition -> Action -> Observation -> Verification loop.
 */
class AndroidToolExecutor(
    private val bridge: AndroidControlBridge,
    private val settleDelayMs: Long = 250L,
    private val blacklistPolicy: ControlBlacklistPolicy = ControlBlacklistPolicy(),
    private val emergencyStopController: EmergencyStopController = EmergencyStopController()
) {

    suspend fun execute(action: AndroidToolAction): ToolExecutionResponse {
        // 1. Pre-action observation (observe screen state before executing)
        val beforeSnapshot = bridge.captureScreenTree(false)

        // 2. Precondition & safety verification
        val preconditionError = checkPreconditions(action, beforeSnapshot)
        if (preconditionError != null) {
            return ToolExecutionResponse(
                action = action,
                verification = ActionVerificationResult(
                    success = false,
                    observation = "Precondition failed: $preconditionError",
                    verificationDetails = "Action rejected before dispatch.",
                    diff = null,
                    suggestedRecovery = RecoveryAction.REQUEST_USER_PERMISSION
                ),
                latestSnapshot = beforeSnapshot
            )
        }

        // 3. Action dispatch
        var screenshotResult: ScreenshotResult? = null
        val actionSuccess = when (action) {
            is AndroidToolAction.Tap -> bridge.tap(action.x, action.y)
            is AndroidToolAction.DoubleTap -> bridge.doubleTap(action.x, action.y)
            is AndroidToolAction.LongPress -> bridge.longPress(action.x, action.y, action.durationMs)
            is AndroidToolAction.Swipe -> bridge.swipe(action.fromX, action.fromY, action.toX, action.toY, action.durationMs)
            is AndroidToolAction.Scroll -> bridge.scroll(action.direction)
            is AndroidToolAction.Type -> bridge.typeText(action.text, action.targetElementId)
            is AndroidToolAction.Back -> bridge.pressBack()
            is AndroidToolAction.Home -> bridge.pressHome()
            is AndroidToolAction.Recents -> bridge.pressRecents()
            is AndroidToolAction.LaunchApp -> bridge.launchApp(action.packageName)
            is AndroidToolAction.Wait -> {
                delay(action.durationMs)
                true
            }
            is AndroidToolAction.Observe -> true
            is AndroidToolAction.Screenshot -> {
                screenshotResult = bridge.takeScreenshot()
                screenshotResult is ScreenshotResult.Success
            }
            is AndroidToolAction.FindText -> true
            is AndroidToolAction.FindElement -> true
        }

        // 4. Settle window for UI animation / rendering
        if (action !is AndroidToolAction.Wait && action !is AndroidToolAction.Observe) {
            delay(settleDelayMs)
        }

        // 5. Post-action observation
        val afterSnapshot = bridge.captureScreenTree(
            includeScreenshot = action is AndroidToolAction.Observe && action.includeScreenshot
        )

        // 6. Verification
        val verification = if (!actionSuccess) {
            ActionVerificationResult(
                success = false,
                observation = "Bridge failed to dispatch action ${action.toolName}.",
                verificationDetails = "Platform dispatch returned false.",
                diff = null,
                suggestedRecovery = RecoveryAction.RETRY_WITH_BACKOFF
            )
        } else {
            ActionVerification.verify(action, beforeSnapshot, afterSnapshot)
        }

        val base64Screenshot = (screenshotResult as? ScreenshotResult.Success)?.base64Data
            ?: afterSnapshot?.screenshotBase64

        return ToolExecutionResponse(
            action = action,
            verification = verification,
            latestSnapshot = afterSnapshot,
            screenshotBase64 = base64Screenshot
        )
    }

    private fun checkPreconditions(
        action: AndroidToolAction,
        currentSnapshot: ScreenSnapshot?
    ): String? {
        if (emergencyStopController.isTriggered) {
            val source = emergencyStopController.lastStopEvent?.source?.displayNameEn ?: "Emergency Stop"
            return "Emergency stop is active ($source). All device actions are halted."
        }

        if (!bridge.isServiceActive()) {
            return "AccessibilityService is not enabled or currently inactive."
        }

        val blacklistVerdict = blacklistPolicy.checkActionAllowed(action, currentSnapshot?.packageName)
        if (blacklistVerdict is BlacklistVerdict.Blocked) {
            return "Security Blacklist Block: ${blacklistVerdict.reason}"
        }

        val dimensions = bridge.getScreenDimensions()
        when (action) {
            is AndroidToolAction.Tap -> {
                if (action.x < 0 || action.x > dimensions.width || action.y < 0 || action.y > dimensions.height) {
                    return "Tap coordinates (${action.x}, ${action.y}) are outside screen bounds ${dimensions.width}x${dimensions.height}."
                }
            }
            is AndroidToolAction.Swipe -> {
                if (action.fromX < 0 || action.fromX > dimensions.width || action.fromY < 0 || action.fromY > dimensions.height ||
                    action.toX < 0 || action.toX > dimensions.width || action.toY < 0 || action.toY > dimensions.height) {
                    return "Swipe coordinates are outside screen bounds."
                }
            }
            is AndroidToolAction.LaunchApp -> {
                if (action.packageName.isBlank()) {
                    return "Package name cannot be empty."
                }
            }
            is AndroidToolAction.Type -> {
                if (action.text.isEmpty()) {
                    return "Text to type cannot be empty."
                }
            }
            else -> {}
        }

        return null
    }
}
