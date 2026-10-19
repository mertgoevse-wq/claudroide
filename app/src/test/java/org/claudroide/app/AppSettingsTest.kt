package org.claudroide.app

import org.claudroide.app.feature.settings.ApprovalLevel
import org.claudroide.app.feature.settings.SettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun maskApiKey_masksMiddleCharacters() {
        val fullKey = "sk-ant-api03-abcdef1234567890wxyz"
        val masked = SettingsState.maskApiKey(fullKey)
        assertEquals("sk-ant-api...wxyz", masked)
        assertFalse(masked.contains("abcdef1234567890"))
    }

    @Test
    fun maskApiKey_shortKey_returnsBullets() {
        val shortKey = "secret"
        val masked = SettingsState.maskApiKey(shortKey)
        assertEquals("••••••••", masked)
    }

    @Test
    fun approvalLevels_containsExpectedTiers() {
        val tiers = ApprovalLevel.entries
        assertEquals(3, tiers.size)
        assertEquals(ApprovalLevel.CAREFUL, tiers[0])
        assertEquals(ApprovalLevel.BALANCED, tiers[1])
        assertEquals(ApprovalLevel.REDUCED, tiers[2])
    }

    @Test
    fun defaultSettings_hasSafeDefaults() {
        val defaults = SettingsState()
        assertEquals(ApprovalLevel.CAREFUL, defaults.approvalLevel)
        assertTrue(defaults.batteryWarningEnabled)
        assertTrue(defaults.storageWarningEnabled)
        assertTrue(defaults.maskKeysInLogs)
    }
}
