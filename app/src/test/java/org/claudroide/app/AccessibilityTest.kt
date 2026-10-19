package org.claudroide.app

import androidx.compose.ui.unit.dp
import org.claudroide.app.core.design.AccessibilityPolicy
import org.claudroide.app.core.design.FocusTraversalRole
import org.junit.Assert.*
import org.junit.Test

class AccessibilityTest {

    @Test
    fun approvalAnnouncement_containsProviderAndFileCountAudibly() {
        val announcement = AccessibilityPolicy.buildApprovalAnnouncement(
            actionTitle = "Dateien im Projekt anpassen",
            targetProvider = "Anthropic Claude API",
            isDestructive = true,
            filesAffected = 3
        )
        assertTrue(announcement.contains("Sicherheitskritische Freigabe erforderlich"))
        assertTrue(announcement.contains("Dateien im Projekt anpassen"))
        assertTrue(announcement.contains("Anthropic Claude API"))
        assertTrue(announcement.contains("3 Datei(en)"))
        assertTrue(announcement.contains("Ablehnen oder Bestätigen"))
    }

    @Test
    fun diffAnnouncement_containsFilenameAndLineCounts() {
        val announcement = AccessibilityPolicy.buildDiffAnnouncement(
            fileName = "Settings.kt",
            additions = 12,
            deletions = 4
        )
        assertEquals("Dateiänderung in Settings.kt: 12 Zeile(n) hinzugefügt, 4 Zeile(n) entfernt.", announcement)
    }

    @Test
    fun nonColorOnlyRule_isStrictlyEnforced() {
        // Valid: color + icon + content description
        val valid = AccessibilityPolicy.validateStatusSemantics(
            hasColor = true,
            hasIcon = true,
            contentDescription = "Erfolg: Aufgabe abgeschlossen"
        )
        assertTrue(valid)

        // Invalid: color only, missing icon
        val colorOnly = AccessibilityPolicy.validateStatusSemantics(
            hasColor = true,
            hasIcon = false,
            contentDescription = "Erfolg"
        )
        assertFalse(colorOnly)

        // Invalid: missing content description
        val missingText = AccessibilityPolicy.validateStatusSemantics(
            hasColor = true,
            hasIcon = true,
            contentDescription = ""
        )
        assertFalse(missingText)
    }

    @Test
    fun dynamicFontScaling_supportsUpTo200Percent() {
        assertTrue(AccessibilityPolicy.isScaleSupported(1.0f))
        assertTrue(AccessibilityPolicy.isScaleSupported(1.5f))
        assertTrue(AccessibilityPolicy.isScaleSupported(2.0f))
        assertFalse(AccessibilityPolicy.isScaleSupported(2.5f))
    }

    @Test
    fun minimumTouchTarget_meetsAccessibilityGuidelines() {
        assertEquals(48.dp, AccessibilityPolicy.MinimumTouchTarget)
    }

    @Test
    fun focusOrderRoles_areDistinct() {
        val roles = FocusTraversalRole.entries
        assertEquals(5, roles.size)
        assertTrue(roles.contains(FocusTraversalRole.CRITICAL_SECURITY_NOTICE))
    }
}
