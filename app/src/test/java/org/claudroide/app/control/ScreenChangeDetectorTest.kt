package org.claudroide.app.control

import org.claudroide.app.feature.control.model.RectBounds
import org.claudroide.app.feature.control.model.ScreenDimensions
import org.claudroide.app.feature.control.model.ScreenElement
import org.claudroide.app.feature.control.model.ScreenSnapshot
import org.claudroide.app.feature.control.screen.ScreenChangeDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenChangeDetectorTest {

    private val dimensions = ScreenDimensions(1080, 2400)

    @Test
    fun `initial snapshot with no previous state generates full diff`() {
        val snapshot = ScreenSnapshot(
            packageName = "org.claudroide.app",
            dimensions = dimensions,
            elements = listOf(
                ScreenElement(id = "elem1", text = "Hello", bounds = RectBounds(0, 0, 100, 100))
            )
        )

        val diff = ScreenChangeDetector.computeDiff(null, snapshot)
        assertTrue(diff.hasChanged)
        assertEquals(1, diff.addedElements.size)
        assertEquals("org.claudroide.app", diff.currentPackage)
    }

    @Test
    fun `detects package change when user or agent navigates to another app`() {
        val before = ScreenSnapshot(
            packageName = "org.claudroide.app",
            dimensions = dimensions,
            elements = emptyList()
        )
        val after = ScreenSnapshot(
            packageName = "com.steinberg.cubasis3",
            dimensions = dimensions,
            elements = emptyList()
        )

        val diff = ScreenChangeDetector.computeDiff(before, after)
        assertTrue(diff.hasChanged)
        assertTrue(diff.packageChanged)
        assertEquals("org.claudroide.app", diff.previousPackage)
        assertEquals("com.steinberg.cubasis3", diff.currentPackage)
    }

    @Test
    fun `detects app crash dialogs`() {
        val before = ScreenSnapshot(
            packageName = "com.example.app",
            dimensions = dimensions,
            elements = emptyList()
        )
        val after = ScreenSnapshot(
            packageName = "android",
            dimensions = dimensions,
            elements = listOf(
                ScreenElement(id = "dialog_msg", text = "Example App keeps stopping", bounds = RectBounds(100, 500, 900, 700))
            )
        )

        val diff = ScreenChangeDetector.computeDiff(before, after)
        assertTrue(diff.isAppCrashDetected)
    }

    @Test
    fun `detects system permission request dialog`() {
        val before = ScreenSnapshot(
            packageName = "org.claudroide.app",
            dimensions = dimensions,
            elements = emptyList()
        )
        val after = ScreenSnapshot(
            packageName = "com.google.android.permissioncontroller",
            dimensions = dimensions,
            elements = listOf(
                ScreenElement(id = "perm_text", text = "Allow ClauDroide to record audio?", bounds = RectBounds(100, 500, 900, 700))
            )
        )

        val diff = ScreenChangeDetector.computeDiff(before, after)
        assertTrue(diff.isPermissionRequested)
    }

    @Test
    fun `detects unchanged screen state`() {
        val element = ScreenElement(id = "button1", text = "Click me", bounds = RectBounds(10, 20, 100, 80))
        val snapshot1 = ScreenSnapshot(
            packageName = "org.claudroide.app",
            dimensions = dimensions,
            elements = listOf(element)
        )
        val snapshot2 = ScreenSnapshot(
            packageName = "org.claudroide.app",
            dimensions = dimensions,
            elements = listOf(element)
        )

        val diff = ScreenChangeDetector.computeDiff(snapshot1, snapshot2)
        assertFalse(diff.hasChanged)
        assertEquals(0, diff.addedElements.size)
        assertEquals(0, diff.removedElements.size)
        assertEquals(0, diff.movedOrModifiedElements.size)
    }
}
