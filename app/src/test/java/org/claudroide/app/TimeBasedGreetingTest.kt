package org.claudroide.app

import org.claudroide.app.core.i18n.TimeBasedGreeting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TimeBasedGreetingTest {

    @Test
    fun englishGreetings_reflectTimeOfDay() {
        assertEquals("Good morning", TimeBasedGreeting.greetingForEnglish(8))
        assertEquals("Good afternoon", TimeBasedGreeting.greetingForEnglish(14))
        assertEquals("Good evening", TimeBasedGreeting.greetingForEnglish(19))
        assertEquals("Good night", TimeBasedGreeting.greetingForEnglish(23))
        assertEquals("Good night", TimeBasedGreeting.greetingForEnglish(2))

        assertEquals("Good afternoon, Mert", TimeBasedGreeting.greetingForEnglish(15, "Mert"))
    }

    @Test
    fun germanGreetings_reflectTimeOfDay() {
        assertEquals("Guten Morgen", TimeBasedGreeting.greetingForGerman(8))
        assertEquals("Guten Tag", TimeBasedGreeting.greetingForGerman(14))
        assertEquals("Guten Abend", TimeBasedGreeting.greetingForGerman(19))
        assertEquals("Gute Nacht", TimeBasedGreeting.greetingForGerman(23))
        assertEquals("Gute Nacht", TimeBasedGreeting.greetingForGerman(3))

        assertEquals("Guten Tag, Mert", TimeBasedGreeting.greetingForGerman(13, "Mert"))
    }

    @Test
    fun localizedGreeting_respectsLocale() {
        assertEquals("Guten Tag, Mert", TimeBasedGreeting.greetingFor(13, "Mert", Locale.GERMAN))
        assertEquals("Good afternoon, Mert", TimeBasedGreeting.greetingFor(13, "Mert", Locale.ENGLISH))
    }

    @Test
    fun greetingNow_returnsNonEmptyString() {
        val greeting = TimeBasedGreeting.greetingNow("Mert")
        assertTrue(greeting.isNotBlank())
        assertTrue(greeting.contains("Mert"))
    }
}
