package org.claudroide.app.core.i18n

import android.content.Context
import android.content.SharedPreferences

class LanguagePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selectedLanguage: AppLanguage
        get() {
            val code = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.code)
            return AppLanguage.entries.find { it.code == code } ?: AppLanguage.SYSTEM
        }
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value.code).apply()
        }

    fun resetToSystem() {
        selectedLanguage = AppLanguage.SYSTEM
    }

    companion object {
        private const val PREFS_NAME = "claudroide_language_prefs"
        private const val KEY_LANGUAGE = "selected_language_code"
    }
}
