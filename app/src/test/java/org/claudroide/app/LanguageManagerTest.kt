package org.claudroide.app

import org.claudroide.app.core.i18n.AppLanguage
import org.claudroide.app.core.i18n.LanguageManager
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LanguageManagerTest {

    @Test
    fun resolveEffectiveLanguage_germanSystem_resolvesToGerman() {
        val resolved = LanguageManager.resolveEffectiveLanguage(
            userPreference = AppLanguage.SYSTEM,
            systemLocale = Locale.GERMAN
        )
        assertEquals(AppLanguage.GERMAN, resolved)
    }

    @Test
    fun resolveEffectiveLanguage_austrianGermanSystem_resolvesToGerman() {
        val resolved = LanguageManager.resolveEffectiveLanguage(
            userPreference = AppLanguage.SYSTEM,
            systemLocale = Locale("de", "AT")
        )
        assertEquals(AppLanguage.GERMAN, resolved)
    }

    @Test
    fun resolveEffectiveLanguage_englishSystem_resolvesToEnglish() {
        val resolved = LanguageManager.resolveEffectiveLanguage(
            userPreference = AppLanguage.SYSTEM,
            systemLocale = Locale.ENGLISH
        )
        assertEquals(AppLanguage.ENGLISH, resolved)
    }

    @Test
    fun resolveEffectiveLanguage_unsupportedSystem_fallsBackToEnglish() {
        // Französisch oder Türkisch als Systemsprache muss sicher auf Englisch zurückfallen
        val resolvedFrench = LanguageManager.resolveEffectiveLanguage(
            userPreference = AppLanguage.SYSTEM,
            systemLocale = Locale.FRENCH
        )
        assertEquals(AppLanguage.ENGLISH, resolvedFrench)

        val resolvedTurkish = LanguageManager.resolveEffectiveLanguage(
            userPreference = AppLanguage.SYSTEM,
            systemLocale = Locale("tr", "TR")
        )
        assertEquals(AppLanguage.ENGLISH, resolvedTurkish)
    }

    @Test
    fun resolveEffectiveLanguage_manualPreference_neverOverriddenBySystem() {
        // Nutzer wählt Deutsch, auch wenn Systemsprache Englisch ist
        val manualGerman = LanguageManager.resolveEffectiveLanguage(
            userPreference = AppLanguage.GERMAN,
            systemLocale = Locale.ENGLISH
        )
        assertEquals(AppLanguage.GERMAN, manualGerman)

        // Nutzer wählt Englisch, auch wenn Systemsprache Deutsch ist
        val manualEnglish = LanguageManager.resolveEffectiveLanguage(
            userPreference = AppLanguage.ENGLISH,
            systemLocale = Locale.GERMANY
        )
        assertEquals(AppLanguage.ENGLISH, manualEnglish)
    }
}
