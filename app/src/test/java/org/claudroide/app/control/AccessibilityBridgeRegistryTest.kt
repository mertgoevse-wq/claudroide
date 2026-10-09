package org.claudroide.app.control

import org.claudroide.app.feature.control.bridge.AccessibilityBridgeRegistry
import org.claudroide.app.feature.control.bridge.FakeAndroidControlBridge
import org.claudroide.app.feature.control.model.CapabilityState
import org.claudroide.app.feature.control.model.ControlCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityBridgeRegistryTest {

    @Test
    fun `default registry returns disabled fallback when no bridge registered`() {
        val bridge = AccessibilityBridgeRegistry.getBridge()

        assertFalse(bridge.isServiceActive())
        assertEquals(
            CapabilityState.SUPPORTED_WITH_USER_PERMISSION,
            bridge.getCapabilityState(ControlCapability.ACCESSIBILITY_CONTROL)
        )
    }

    @Test
    fun `registered bridge becomes active and can be unregistered`() {
        val fakeBridge = FakeAndroidControlBridge(isEnabled = true)

        AccessibilityBridgeRegistry.register(fakeBridge)
        assertTrue(AccessibilityBridgeRegistry.isReady)
        assertEquals(fakeBridge, AccessibilityBridgeRegistry.getBridge())

        AccessibilityBridgeRegistry.unregister(fakeBridge)
        assertFalse(AccessibilityBridgeRegistry.isReady)
    }
}
