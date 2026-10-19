package org.claudroide.app.control

import org.claudroide.app.feature.control.safety.EmergencyStopController
import org.claudroide.app.feature.control.safety.EmergencyStopSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyStopControllerTest {

    @Test
    fun initialState_isNotTriggered() {
        val controller = EmergencyStopController()
        assertFalse(controller.isTriggered)
    }

    @Test
    fun triggerFromTopButton_setsTriggeredState() {
        val controller = EmergencyStopController()
        controller.trigger(EmergencyStopSource.TOP_BAR_BUTTON, "User tapped header stop")

        assertTrue(controller.isTriggered)
        assertEquals(EmergencyStopSource.TOP_BAR_BUTTON, controller.lastStopEvent?.source)
    }

    @Test
    fun triggerFromFloatingOverlay_setsTriggeredState() {
        val controller = EmergencyStopController()
        controller.trigger(EmergencyStopSource.FLOATING_OVERLAY, "Floating button pressed")

        assertTrue(controller.isTriggered)
        assertEquals(EmergencyStopSource.FLOATING_OVERLAY, controller.lastStopEvent?.source)
    }

    @Test
    fun volumeDownDoublePress_withinWindow_triggersEmergencyStop() {
        val controller = EmergencyStopController()
        val t0 = 1000L
        val t1 = t0 + 400L // 400ms delta <= 800ms window

        val firstPress = controller.onVolumeDownKeyEvent(t0)
        assertFalse(firstPress)
        assertFalse(controller.isTriggered)

        val secondPress = controller.onVolumeDownKeyEvent(t1)
        assertTrue(secondPress)
        assertTrue(controller.isTriggered)
        assertEquals(EmergencyStopSource.HARDWARE_KEY_DOUBLE_VOLUME_DOWN, controller.lastStopEvent?.source)
    }

    @Test
    fun volumeDownDoublePress_exceedingWindow_doesNotTrigger() {
        val controller = EmergencyStopController()
        val t0 = 1000L
        val t1 = t0 + 1500L // 1500ms delta > 800ms window

        controller.onVolumeDownKeyEvent(t0)
        val secondPress = controller.onVolumeDownKeyEvent(t1)

        assertFalse(secondPress)
        assertFalse(controller.isTriggered)
    }

    @Test
    fun reset_requiresExplicitAction_andClearsState() {
        val controller = EmergencyStopController()
        controller.trigger(EmergencyStopSource.TOP_BAR_BUTTON)
        assertTrue(controller.isTriggered)

        val resetResult = controller.reset()
        assertTrue(resetResult)
        assertFalse(controller.isTriggered)
    }
}
