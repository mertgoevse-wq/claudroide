package org.claudroide.app.feature.control.tools

import org.claudroide.app.feature.control.model.ScreenSnapshot
import org.claudroide.app.feature.control.screen.ScreenChangeDetector
import org.claudroide.app.feature.control.screen.ScreenDiff

/**
 * Suggested recovery strategy when an action fails verification or encounters an obstacle.
 */
enum class RecoveryAction(val description: String) {
    DISMISS_DIALOG("An unexpected dialog or popup is intercepting the screen. Try pressing Back or tapping Dismiss."),
    DISMISS_KEYBOARD("Software keyboard is obscuring the target area. Dismiss the keyboard."),
    SCROLL_SEARCH("Target element is not currently visible in viewport. Scroll down or up to locate element."),
    REQUEST_USER_PERMISSION("Action blocked by Android system permission prompt. Request user confirmation."),
    RETRY_WITH_BACKOFF("Screen had not settled. Wait and retry action."),
    FATAL_STOP("Application crashed or encountered an unrecoverable failure. Stop autonomous loop.")
}

/**
 * Comprehensive verification result after executing an Android tool action.
 */
data class ActionVerificationResult(
    val success: Boolean,
    val observation: String,
    val verificationDetails: String,
    val diff: ScreenDiff?,
    val suggestedRecovery: RecoveryAction? = null
)

/**
 * Verifier that verifies state changes and screen responses.
 */
object ActionVerification {

    fun verify(
        action: AndroidToolAction,
        before: ScreenSnapshot?,
        after: ScreenSnapshot?
    ): ActionVerificationResult {
        if (after == null) {
            return ActionVerificationResult(
                success = false,
                observation = "No screen tree could be captured after action.",
                verificationDetails = "Accessibility tree was null. Service may have been killed or target app restricts accessibility.",
                diff = null,
                suggestedRecovery = RecoveryAction.RETRY_WITH_BACKOFF
            )
        }

        val diff = ScreenChangeDetector.computeDiff(before, after)

        // 1. Check for application crash
        if (diff.isAppCrashDetected) {
            return ActionVerificationResult(
                success = false,
                observation = "App crash or ANR dialog detected on screen.",
                verificationDetails = diff.summary,
                diff = diff,
                suggestedRecovery = RecoveryAction.FATAL_STOP
            )
        }

        // 2. Check for system permission request
        if (diff.isPermissionRequested) {
            return ActionVerificationResult(
                success = false,
                observation = "Android system permission request dialog appeared.",
                verificationDetails = "Package: ${after.packageName}. Dialog: ${after.visibleText.take(3).joinToString(", ")}",
                diff = diff,
                suggestedRecovery = RecoveryAction.REQUEST_USER_PERMISSION
            )
        }

        // 3. Action-specific verification
        return when (action) {
            is AndroidToolAction.LaunchApp -> {
                val launched = after.packageName == action.packageName ||
                        after.packageName.contains(action.packageName.substringAfterLast('.'))
                if (launched) {
                    ActionVerificationResult(
                        success = true,
                        observation = "Application '${action.packageName}' is running in foreground.",
                        verificationDetails = "Active package: ${after.packageName}, elements: ${after.elements.size}",
                        diff = diff
                    )
                } else {
                    ActionVerificationResult(
                        success = false,
                        observation = "App did not come to foreground. Active package is still '${after.packageName}'.",
                        verificationDetails = diff.summary,
                        diff = diff,
                        suggestedRecovery = RecoveryAction.RETRY_WITH_BACKOFF
                    )
                }
            }

            is AndroidToolAction.Tap -> {
                // Verified if screen changed or dialog/keyboard appeared
                if (diff.hasChanged) {
                    ActionVerificationResult(
                        success = true,
                        observation = "Tap at (${action.x}, ${action.y}) caused screen transition.",
                        verificationDetails = diff.summary,
                        diff = diff
                    )
                } else {
                    // Tap occurred but nothing changed on screen
                    ActionVerificationResult(
                        success = true,
                        observation = "Tap delivered at (${action.x}, ${action.y}), but screen state remained unchanged.",
                        verificationDetails = "Change score: 0.0. Element might have responded without layout change.",
                        diff = diff
                    )
                }
            }

            is AndroidToolAction.Type -> {
                val textFound = after.visibleText.any { it.contains(action.text) }
                if (textFound || diff.hasChanged) {
                    ActionVerificationResult(
                        success = true,
                        observation = "Text '${action.text}' entered successfully.",
                        verificationDetails = diff.summary,
                        diff = diff
                    )
                } else {
                    ActionVerificationResult(
                        success = false,
                        observation = "Typed text '${action.text}' was not found in visible elements.",
                        verificationDetails = "No editable field appeared updated.",
                        diff = diff,
                        suggestedRecovery = RecoveryAction.RETRY_WITH_BACKOFF
                    )
                }
            }

            is AndroidToolAction.Back -> {
                ActionVerificationResult(
                    success = true,
                    observation = "Back button pressed.",
                    verificationDetails = diff.summary,
                    diff = diff
                )
            }

            is AndroidToolAction.Home -> {
                val isHome = after.packageName.contains("launcher") || after.packageName.contains("home")
                ActionVerificationResult(
                    success = isHome || diff.hasChanged,
                    observation = if (isHome) "Home screen visible." else "Home button pressed.",
                    verificationDetails = diff.summary,
                    diff = diff
                )
            }

            is AndroidToolAction.Recents -> {
                ActionVerificationResult(
                    success = true,
                    observation = "Recents overview opened.",
                    verificationDetails = diff.summary,
                    diff = diff
                )
            }

            is AndroidToolAction.Scroll, is AndroidToolAction.Swipe -> {
                ActionVerificationResult(
                    success = diff.hasChanged,
                    observation = if (diff.hasChanged) "Scroll/Swipe completed, screen content shifted."
                    else "Scroll/Swipe performed but content did not move (reached edge or non-scrollable).",
                    verificationDetails = diff.summary,
                    diff = diff,
                    suggestedRecovery = if (!diff.hasChanged) RecoveryAction.SCROLL_SEARCH else null
                )
            }

            is AndroidToolAction.Wait, is AndroidToolAction.Observe, is AndroidToolAction.Screenshot -> {
                ActionVerificationResult(
                    success = true,
                    observation = "Observation completed. Active package: ${after.packageName}, ${after.elements.size} elements.",
                    verificationDetails = "Visible text items: ${after.visibleText.size}",
                    diff = diff
                )
            }

            is AndroidToolAction.FindText -> {
                val matches = after.findElementsByText(action.query, action.exact)
                ActionVerificationResult(
                    success = matches.isNotEmpty(),
                    observation = "Found ${matches.size} element(s) matching '${action.query}'.",
                    verificationDetails = matches.joinToString("\n") { "Element [${it.id}]: '${it.readableLabel}' at ${it.bounds}" },
                    diff = diff,
                    suggestedRecovery = if (matches.isEmpty()) RecoveryAction.SCROLL_SEARCH else null
                )
            }

            is AndroidToolAction.FindElement -> {
                val matches = after.elements.filter { it.id.contains(action.query) || it.viewIdResourceName?.contains(action.query) == true }
                ActionVerificationResult(
                    success = matches.isNotEmpty(),
                    observation = "Found ${matches.size} element(s) with id matching '${action.query}'.",
                    verificationDetails = matches.joinToString("\n") { "Element [${it.id}]: bounds ${it.bounds}" },
                    diff = diff,
                    suggestedRecovery = if (matches.isEmpty()) RecoveryAction.SCROLL_SEARCH else null
                )
            }

            is AndroidToolAction.DoubleTap, is AndroidToolAction.LongPress -> {
                ActionVerificationResult(
                    success = true,
                    observation = "Gesture executed.",
                    verificationDetails = diff.summary,
                    diff = diff
                )
            }
        }
    }
}
