package org.claudroide.app.core.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

enum class PermissionState {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

object PermissionManager {

    /**
     * Bestimmt, ob für Android 13+ (API 33) eine Runtime-Berechtigung
     * für Benachrichtigungen (POST_NOTIFICATIONS) nötig ist.
     */
    fun needsNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Sichert den Zugriff auf einen vom Nutzer per SAF gewählten Ordner dauerhaft.
     */
    fun takePersistableUriPermission(context: Context, uri: Uri) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            // Sicher abfangen, falls das System die Persistierung nicht gewährt
        }
    }

    /**
     * Gibt eine persistente Uri-Berechtigung wieder frei (Widerruf).
     */
    fun releasePersistableUriPermission(context: Context, uri: Uri) {
        val releaseFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.releasePersistableUriPermission(uri, releaseFlags)
        } catch (e: SecurityException) {
            // Ignorieren falls bereits widerrufen
        }
    }

    /**
     * Liefert die Begründung (Rationale) für eine angeforderte Berechtigung.
     */
    fun getRationale(permission: String): String {
        return when {
            permission.contains("POST_NOTIFICATIONS") ->
                "Benachrichtigungen informieren dich über den Fortschritt von Hintergrundaufgaben und Tests, ohne dass du die App geöffnet halten musst."
            else -> "Diese Berechtigung wird für die ausgewählte Funktion benötigt."
        }
    }
}
