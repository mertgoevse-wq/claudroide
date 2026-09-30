package org.claudroide.app.core.i18n

import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String) {
    SYSTEM("system", "Systemstandard"),
    GERMAN("de", "Deutsch"),
    ENGLISH("en", "English")
}

object LanguageManager {

    /**
     * Löst die effektive App-Sprache auf.
     * Wenn der Nutzer eine spezifische Sprache (DE oder EN) gewählt hat, hat diese Vorrang.
     * Wenn SYSTEM gewählt ist, wird Deutsch gewählt, wenn die Systemsprache mit "de" beginnt,
     * ansonsten greift der sichere Standard-Fallback English.
     */
    fun resolveEffectiveLanguage(
        userPreference: AppLanguage,
        systemLocale: Locale = Locale.getDefault()
    ): AppLanguage {
        return when (userPreference) {
            AppLanguage.GERMAN -> AppLanguage.GERMAN
            AppLanguage.ENGLISH -> AppLanguage.ENGLISH
            AppLanguage.SYSTEM -> {
                val lang = systemLocale.language.lowercase()
                if (lang.startsWith("de")) {
                    AppLanguage.GERMAN
                } else {
                    AppLanguage.ENGLISH
                }
            }
        }
    }
}
