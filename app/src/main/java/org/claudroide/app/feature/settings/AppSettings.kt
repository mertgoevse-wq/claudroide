package org.claudroide.app.feature.settings

import org.claudroide.app.core.i18n.AppLanguage

enum class ApprovalLevel(val title: String, val description: String) {
    CAREFUL("Vorsichtig (Standard)", "Jede Dateiänderung und jeder Befehl erfordert deine explizite Bestätigung."),
    BALANCED("Ausgewogen", "Projektinterne Dateiänderungen erfolgen nach Freigabe des Blocks; Befehle brauchen Bestätigung."),
    REDUCED("Weniger Rückfragen", "Zeitlich befristeter Ausnahmemodus für erfahrene Entwickler.")
}

data class SettingsState(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val approvalLevel: ApprovalLevel = ApprovalLevel.CAREFUL,
    val batteryWarningEnabled: Boolean = true,
    val storageWarningEnabled: Boolean = true,
    val reducedContextMode: Boolean = true,
    val maskKeysInLogs: Boolean = true
) {
    companion object {
        /**
         * Maskiert sensible API-Keys für die Anzeige in Einstellungen.
         * Erlaubt Identifikation des Schlüssels, ohne das Geheimnis im Klartext preiszugeben.
         * Bsp: "sk-ant-api03-1234567890abcdef" -> "sk-ant-api...cdef"
         */
        fun maskApiKey(key: String): String {
            if (key.length <= 10) return "••••••••"
            val prefix = key.take(10)
            val suffix = key.takeLast(4)
            return "$prefix...$suffix"
        }
    }
}
