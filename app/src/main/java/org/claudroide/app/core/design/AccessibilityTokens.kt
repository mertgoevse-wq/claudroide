package org.claudroide.app.core.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Roles for accessible focus ordering in security-critical flows.
 */
enum class FocusTraversalRole {
    HEADER_HEADING,
    CRITICAL_SECURITY_NOTICE,
    DIFF_CONTENT,
    SECONDARY_ACTION,
    PRIMARY_ACTION
}

/**
 * Android WCAG 2.2 accessibility policies, TalkBack announcement builders,
 * and font scaling rules.
 */
object AccessibilityPolicy {
    val MinimumTouchTarget: Dp = 48.dp
    const val MaxSystemFontScaleTested: Float = 2.0f // 200% scaling support

    /**
     * Builds an unambiguous TalkBack announcement for security approvals.
     */
    fun buildApprovalAnnouncement(
        actionTitle: String,
        targetProvider: String,
        isDestructive: Boolean,
        filesAffected: Int
    ): String {
        val severity = if (isDestructive) "Sicherheitskritische Freigabe erforderlich" else "Freigabe erforderlich"
        return "$severity: $actionTitle für Anbieter $targetProvider. $filesAffected Datei(en) betroffen. Ablehnen oder Bestätigen."
    }

    /**
     * Builds an audible summary for file modification diffs.
     */
    fun buildDiffAnnouncement(
        fileName: String,
        additions: Int,
        deletions: Int
    ): String {
        return "Dateiänderung in $fileName: $additions Zeile(n) hinzugefügt, $deletions Zeile(n) entfernt."
    }

    /**
     * Enforces the non-color-only rule:
     * Status elements must never rely solely on color. They require a distinct icon
     * and an explicit textual contentDescription.
     */
    fun validateStatusSemantics(
        hasColor: Boolean,
        hasIcon: Boolean,
        contentDescription: String?
    ): Boolean {
        if (!hasColor) return false
        val hasMeaningfulDescription = !contentDescription.isNullOrBlank()
        // Must have both icon/symbol and textual description
        return hasIcon && hasMeaningfulDescription
    }

    /**
     * Verifies that the UI architecture supports dynamic system font scaling.
     */
    fun isScaleSupported(scale: Float): Boolean {
        return scale in 0.85f..MaxSystemFontScaleTested
    }
}
