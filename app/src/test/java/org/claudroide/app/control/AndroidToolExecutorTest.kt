package org.claudroide.app.control

import kotlinx.coroutines.runBlocking
import org.claudroide.app.feature.control.bridge.FakeAndroidControlBridge
import org.claudroide.app.feature.control.model.RectBounds
import org.claudroide.app.feature.control.model.ScreenDimensions
import org.claudroide.app.feature.control.model.ScreenElement
import org.claudroide.app.feature.control.model.ScreenSnapshot
import org.claudroide.app.feature.control.model.ScrollDirection
import org.claudroide.app.feature.control.tools.AndroidToolAction
import org.claudroide.app.feature.control.tools.AndroidToolExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AndroidToolExecutorTest {

    private lateinit var bridge: FakeAndroidControlBridge
    private lateinit var executor: AndroidToolExecutor

    @Before
    fun setUp() {
        bridge = FakeAndroidControlBridge()
        executor = AndroidToolExecutor(bridge, settleDelayMs = 0L)
    }

    @Test
    fun `tap action succeeds and verifies screen response`() = runBlocking {
        val response = executor.execute(AndroidToolAction.Tap(200f, 450f))

        assertTrue(response.verification.success)
        assertTrue(bridge.actionLog.any { it.startsWith("tap(200.0, 450.0)") })
    }

    @Test
    fun `tap action fails precondition when coordinates are out of bounds`() = runBlocking {
        val response = executor.execute(AndroidToolAction.Tap(2000f, 4000f))

        assertFalse(response.verification.success)
        assertTrue(response.verification.observation.contains("outside screen bounds"))
        assertFalse(bridge.actionLog.any { it.startsWith("tap(2000") })
    }

    @Test
    fun `launch app updates active package in screen snapshot`() = runBlocking {
        val response = executor.execute(AndroidToolAction.LaunchApp("com.steinberg.cubasis3"))

        assertTrue(response.verification.success)
        assertEquals("com.steinberg.cubasis3", response.latestSnapshot?.packageName)
        assertTrue(response.verification.observation.contains("Cubasis 3") || response.verification.observation.contains("cubasis3"))
    }

    @Test
    fun `type text updates editable element in snapshot`() = runBlocking {
        val initialSnapshot = ScreenSnapshot(
            packageName = "org.claudroide.app",
            dimensions = ScreenDimensions(1080, 2340),
            elements = listOf(
                ScreenElement(
                    id = "input_box",
                    text = "",
                    className = "android.widget.EditText",
                    bounds = RectBounds(50, 500, 1000, 600),
                    isEditable = true,
                    isFocused = true
                )
            )
        )
        bridge.currentSnapshot = initialSnapshot

        val response = executor.execute(AndroidToolAction.Type("Create new MIDI project", targetElementId = "input_box"))

        assertTrue(response.verification.success)
        assertTrue(bridge.currentSnapshot?.visibleText?.contains("Create new MIDI project") == true)
    }

    @Test
    fun `observe captures screen tree and elements`() = runBlocking {
        val response = executor.execute(AndroidToolAction.Observe(includeScreenshot = false))

        assertTrue(response.verification.success)
        assertNotNull(response.latestSnapshot)
        assertTrue(response.latestSnapshot!!.elements.isNotEmpty())
    }

    @Test
    fun `find text locates target element on screen`() = runBlocking {
        val response = executor.execute(AndroidToolAction.FindText("Start Task", exact = true))

        assertTrue(response.verification.success)
        assertTrue(response.verification.observation.contains("Found 1 element"))
    }

    @Test
    fun `scroll dispatches swipe gesture`() = runBlocking {
        val response = executor.execute(AndroidToolAction.Scroll(ScrollDirection.DOWN))

        assertTrue(bridge.actionLog.any { it.startsWith("scroll(DOWN)") })
    }

    @Test
    fun `failing action when service is disabled`() = runBlocking {
        bridge.isEnabled = false

        val response = executor.execute(AndroidToolAction.Tap(100f, 100f))

        assertFalse(response.verification.success)
        assertTrue(response.verification.observation.contains("AccessibilityService is not enabled"))
    }

    @Test
    fun `failing action when emergency stop is active`() = runBlocking {
        val stopController = org.claudroide.app.feature.control.safety.EmergencyStopController()
        stopController.trigger(org.claudroide.app.feature.control.safety.EmergencyStopSource.TOP_BAR_BUTTON)
        val safetyExecutor = AndroidToolExecutor(bridge, settleDelayMs = 0L, emergencyStopController = stopController)

        val response = safetyExecutor.execute(AndroidToolAction.Tap(100f, 100f))

        assertFalse(response.verification.success)
        assertTrue(response.verification.observation.contains("Emergency stop is active"))
        assertFalse(bridge.actionLog.any { it.startsWith("tap(100") })
    }

    @Test
    fun `failing action when launching blacklisted app`() = runBlocking {
        val response = executor.execute(AndroidToolAction.LaunchApp("com.paypal.android.p2pmobile"))

        assertFalse(response.verification.success)
        assertTrue(response.verification.observation.contains("Security Blacklist Block"))
    }

    @Test
    fun `failing action when foreground app is blacklisted`() = runBlocking {
        bridge.currentSnapshot = ScreenSnapshot(
            packageName = "com.revolut.revolut",
            dimensions = ScreenDimensions(1080, 2340),
            elements = emptyList()
        )

        val response = executor.execute(AndroidToolAction.Tap(100f, 100f))

        assertFalse(response.verification.success)
        assertTrue(response.verification.observation.contains("Security Blacklist Block"))
        assertFalse(bridge.actionLog.any { it.startsWith("tap(100") })
    }
}
