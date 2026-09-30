package org.claudroide.app

import org.claudroide.app.core.security.PermissionManager
import org.claudroide.app.core.security.PermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionManagerTest {

    @Test
    fun permissionStates_hasExpectedValues() {
        val states = PermissionState.entries
        assertEquals(3, states.size)
        assertEquals(PermissionState.GRANTED, states[0])
        assertEquals(PermissionState.DENIED, states[1])
        assertEquals(PermissionState.PERMANENTLY_DENIED, states[2])
    }

    @Test
    fun getRationale_notifications_returnsInformativeExplanation() {
        val rationale = PermissionManager.getRationale("android.permission.POST_NOTIFICATIONS")
        assertTrue(rationale.contains("Hintergrundaufgaben"))
        assertTrue(rationale.isNotEmpty())
    }

    @Test
    fun getRationale_generic_returnsFallback() {
        val fallback = PermissionManager.getRationale("android.permission.CAMERA")
        assertTrue(fallback.contains("benötigt"))
    }
}
