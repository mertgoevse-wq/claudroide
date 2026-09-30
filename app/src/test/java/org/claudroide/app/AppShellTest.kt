package org.claudroide.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppShellTest {

    @Test
    fun appDestinations_hasExpectedItemsAndLabels() {
        val destinations = AppDestination.entries
        assertEquals(3, destinations.size)
        assertEquals(AppDestination.CHAT, destinations[0])
        assertEquals(AppDestination.PROJECTS, destinations[1])
        assertEquals(AppDestination.SETTINGS, destinations[2])

        destinations.forEach {
            assertNotNull(it.icon)
            assert(it.labelRes != 0)
        }
    }

    @Test
    fun defaultDestination_isChat() {
        val defaultDest = AppDestination.CHAT
        assertEquals("CHAT", defaultDest.name)
    }
}
