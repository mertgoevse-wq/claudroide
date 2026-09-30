package org.claudroide.app

import org.claudroide.app.core.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguagePreferenceTest {

    @Test
    fun appLanguages_containsExpectedEntries() {
        val languages = AppLanguage.entries
        assertEquals(3, languages.size)
        assertEquals("system", AppLanguage.SYSTEM.code)
        assertEquals("de", AppLanguage.GERMAN.code)
        assertEquals("en", AppLanguage.ENGLISH.code)
    }

    @Test
    fun appLanguages_displayNamesAreNonEmpty() {
        AppLanguage.entries.forEach { lang ->
            assert(lang.displayName.isNotEmpty())
            assert(lang.code.isNotEmpty())
        }
    }
}
