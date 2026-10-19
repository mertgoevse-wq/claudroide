package org.claudroide.app

import org.claudroide.app.core.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class AppShellTest {

    @Test
    fun appDestinations_hasExpectedItemsAndLabels() {
        val destinations = AppDestination.entries
        assertEquals(6, destinations.size)
        assertEquals(AppDestination.CHAT, destinations[0])
        assertEquals(AppDestination.PROJECTS, destinations[1])
        assertEquals(AppDestination.CODE_MCP, destinations[2])
        assertEquals(AppDestination.MEDIA_STUDIO, destinations[3])
        assertEquals(AppDestination.ARTIFACTS, destinations[4])
        assertEquals(AppDestination.SETTINGS, destinations[5])

        destinations.forEach {
            assertNotNull(it.icon)
            assert(it.labelRes != 0)
        }
    }

    @Test
    fun appDoesNotBundleAwesomePrompts() {
        assertFalse(File("src/main/assets/prompts").exists())
        assertFalse(AppDestination.entries.any { it.name.contains("PROMPT", ignoreCase = true) })
        assertFalse(File("src/main/assets/prompts").walkTopDown().any { it.isFile })
    }

    @Test
    fun defaultDestination_isChat() {
        val defaultDest = AppDestination.CHAT
        assertEquals("CHAT", defaultDest.name)
    }
}
