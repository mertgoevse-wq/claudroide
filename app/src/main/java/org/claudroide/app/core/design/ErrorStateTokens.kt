package org.claudroide.app.core.design

/**
 * Transparent status indicator informing user whether their request
 * left the device or modified local files.
 */
enum class DataTransmissionStatus(val explanationDe: String, val explanationEn: String) {
    NOT_SENT(
        "Es wurden keine Daten an externe Server übertragen und keine Dateien verändert.",
        "No data was transmitted to external servers, and no files were modified."
    ),
    PARTIALLY_SENT(
        "Die Verbindung wurde während der Übertragung unterbrochen. Möglicherweise wurden Teilinhalte empfangen.",
        "Connection interrupted mid-transmission. Partial content may have been received by provider."
    ),
    SENT_NO_CHANGES_SAVED(
        "Antwort wurde empfangen, aber keine lokalen Dateiänderungen gespeichert.",
        "Response was received, but no local file modifications were written."
    ),
    LOCAL_ONLY(
        "Lokaler Vorgang ohne Netzwerkverbindung. Keine externen Aufrufe.",
        "Local operation without network connection. Zero external calls."
    )
}

enum class UiErrorType {
    OFFLINE,
    PERMISSION_DENIED,
    RATE_LIMITED,
    PROVIDER_ERROR,
    STREAM_STALLED,
    EMPTY_STATE
}

/**
 * User-facing transparent error representation with distinct retry and cancel pathways.
 */
data class AppUiError(
    val type: UiErrorType,
    val title: String,
    val message: String,
    val transmissionStatus: DataTransmissionStatus,
    val canRetry: Boolean = true,
    val retryLabel: String = "Erneut versuchen",
    val cancelLabel: String = "Abbrechen"
)

/**
 * Privacy and security sanitizer ensuring error dialogs never leak
 * API keys, authentication tokens, or private path segments.
 */
object ErrorSanitizer {
    private val API_KEY_REGEX = Regex("""(?i)(sk-ant-[a-zA-Z0-9_\-]{8,}|sk-[a-zA-Z0-9_\-]{16,}|bearer\s+[a-zA-Z0-9\-_\.]{16,})""")
    private val SENSITIVE_PARAM_REGEX = Regex("""(?i)(api[_-]?key|password|secret|token)\s*[=:]\s*['"]?([^'"\s&,]+)['"]?""")

    fun sanitizeErrorMessage(rawMessage: String): String {
        if (rawMessage.isBlank()) return "Ein unbekannter Fehler ist aufgetreten."
        var sanitized = API_KEY_REGEX.replace(rawMessage, "[SCHLÜSSEL AUSGEBLENDET]")
        sanitized = SENSITIVE_PARAM_REGEX.replace(sanitized) { matchResult ->
            "${matchResult.groupValues[1]}=[GESCHÜTZT]"
        }
        return sanitized
    }
}
