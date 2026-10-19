package org.claudroide.app.core.i18n

import java.time.LocalTime
import java.util.Locale

/**
 * Time-dependent greeting that changes with the hour of day.
 * Replaces the static placeholder with a contextual greeting,
 * following the English-first / German-second convention.
 */
object TimeBasedGreeting {

    /** Returns the greeting for the given hour in English. */
    fun greetingForEnglish(hour: Int, userName: String? = null): String {
        val greeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..21 -> "Good evening"
            else -> "Good night"
        }
        return if (userName != null) "$greeting, $userName" else greeting
    }

    /** Returns the greeting for the given hour in German. */
    fun greetingForGerman(hour: Int, userName: String? = null): String {
        val greeting = when (hour) {
            in 5..11 -> "Guten Morgen"
            in 12..17 -> "Guten Tag"
            in 18..21 -> "Guten Abend"
            else -> "Gute Nacht"
        }
        return if (userName != null) "$greeting, $userName" else greeting
    }

    /** Returns the localized greeting for the given hour based on locale. */
    fun greetingFor(
        hour: Int,
        userName: String? = null,
        locale: Locale = Locale.getDefault(),
    ): String {
        return if (locale.language.equals("de", ignoreCase = true)) {
            greetingForGerman(hour, userName)
        } else {
            greetingForEnglish(hour, userName)
        }
    }

    /** Returns the greeting for right now. Defaults to user "Mert". */
    fun greetingNow(
        userName: String? = "Mert",
        locale: Locale = Locale.getDefault(),
    ): String = greetingFor(LocalTime.now().hour, userName, locale)
}
