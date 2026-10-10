package org.claudroide.app.feature.control.safety

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

enum class EmergencyStopSource(val displayNameDe: String, val displayNameEn: String) {
    TOP_BAR_BUTTON("Oberer App-Knopf", "Top App Bar Button"),
    FLOATING_OVERLAY("Schwebender Not-Aus", "Floating Overlay Button"),
    HARDWARE_KEY_DOUBLE_VOLUME_DOWN("Leiser-Taste zweimal", "Hardware Volume-Down Double Press"),
    PROGRAMMATIC("Programmatischer Not-Halt", "Programmatic Emergency Halt")
}

data class EmergencyStopEvent(
    val source: EmergencyStopSource,
    val timestampMs: Long,
    val details: String? = null
)

/**
 * Task 6.4 / R-5.4 / Spec §6.4 — Dreifaches Not-Aus für den Bildschirm-Agenten.
 *
 * Invariants:
 *  1. Any of the three triggers (top button, floating overlay, double volume-down)
 *     halts all actions immediately.
 *  2. Once triggered, [isTriggered] stays true and cannot auto-resume.
 *  3. Resumption requires explicit user reset via [reset].
 */
class EmergencyStopController {

    private val triggered = AtomicBoolean(false)
    private val lastEvent = AtomicReference<EmergencyStopEvent?>(null)
    private val lastVolumeDownPressMs = AtomicLong(0L)

    val isTriggered: Boolean
        get() = triggered.get()

    val lastStopEvent: EmergencyStopEvent?
        get() = lastEvent.get()

    /**
     * Immediately triggers emergency stop from any source.
     */
    fun trigger(source: EmergencyStopSource, details: String? = null): EmergencyStopEvent {
        val event = EmergencyStopEvent(
            source = source,
            timestampMs = System.currentTimeMillis(),
            details = details
        )
        lastEvent.set(event)
        triggered.set(true)
        return event
    }

    /**
     * Processes a hardware key event (e.g. KEYCODE_VOLUME_DOWN).
     * If two presses occur within [DOUBLE_PRESS_WINDOW_MS], triggers emergency stop.
     * Returns true if emergency stop was triggered by this press.
     */
    fun onVolumeDownKeyEvent(nowMs: Long = System.currentTimeMillis()): Boolean {
        val previous = lastVolumeDownPressMs.getAndSet(nowMs)
        val delta = nowMs - previous
        if (delta in 1..DOUBLE_PRESS_WINDOW_MS) {
            trigger(EmergencyStopSource.HARDWARE_KEY_DOUBLE_VOLUME_DOWN, "Double press interval: ${delta}ms")
            return true
        }
        return false
    }

    /**
     * Resets emergency stop state. Requires explicit user action.
     */
    fun reset(): Boolean {
        lastEvent.set(null)
        lastVolumeDownPressMs.set(0L)
        return triggered.getAndSet(false)
    }

    companion object {
        const val DOUBLE_PRESS_WINDOW_MS = 800L

        /** Shared default instance across control components. */
        val DEFAULT = EmergencyStopController()
    }
}
