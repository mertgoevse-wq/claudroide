package org.claudroide.app.feature.provider

/**
 * Categorised provider error kinds.
 * Used by [ProviderErrorHelpMapper] to produce actionable German-language guidance.
 */
enum class ProviderErrorKind {
    /** 401 / 403 — key invalid, revoked, or wrong provider. */
    AUTH_REJECTED,
    /** 429 — rate limit or quota exceeded. */
    RATE_LIMITED,
    /** 404 on the model endpoint or unknown model id. */
    MODEL_NOT_FOUND,
    /** 400 — request payload rejected by the provider. */
    BAD_REQUEST,
    /** 5xx — transient provider-side problem. */
    SERVER_ERROR,
    /** DNS failure, TCP timeout, no reachable route. */
    NETWORK_UNREACHABLE,
    /** TLS handshake failure or certificate error. */
    TLS_ERROR,
    /** Redirect to a different host detected. */
    UNSAFE_REDIRECT,
    /** Anything else that does not map to the above. */
    UNKNOWN
}

/**
 * Human-readable help record shown after a provider error.
 *
 * Invariants:
 *  - [technicalDetail] is null by default; only shown when the user explicitly opts in.
 *  - No API key, private header, or request body content may appear in any field.
 *  - [mayHaveBeenBilled] is true when the request may have reached the provider
 *    and incurred a charge, even if no response was received.
 */
data class ProviderErrorHelp(
    val kind: ProviderErrorKind,
    val headline: String,
    val explanation: String,
    val suggestedActions: List<String>,
    /** True when this error may have resulted in a charge by the provider. */
    val mayHaveBeenBilled: Boolean,
    /** Optional technical detail — must be pre-sanitised; never includes secrets. */
    val technicalDetail: String? = null
)

/**
 * Maps provider HTTP/network errors to actionable user-facing [ProviderErrorHelp] records.
 *
 * Callers must sanitise raw error messages with [SecretMasker] before passing them as
 * [rawDetail]. This class performs an additional best-effort scrub but is not the
 * primary sanitisation boundary.
 */
object ProviderErrorHelpMapper {

    /**
     * Classifies an HTTP status code into a [ProviderErrorKind].
     */
    fun classifyHttpStatus(httpStatus: Int): ProviderErrorKind = when (httpStatus) {
        401, 403    -> ProviderErrorKind.AUTH_REJECTED
        429         -> ProviderErrorKind.RATE_LIMITED
        404         -> ProviderErrorKind.MODEL_NOT_FOUND
        400         -> ProviderErrorKind.BAD_REQUEST
        in 500..599 -> ProviderErrorKind.SERVER_ERROR
        else        -> ProviderErrorKind.UNKNOWN
    }

    /**
     * Builds a [ProviderErrorHelp] for a given [kind] and optional sanitised detail.
     *
     * @param kind         Error category.
     * @param providerName Display name of the provider (e.g. "Anthropic Claude API").
     * @param rawDetail    Optional raw detail string. Best-effort scrub is applied here;
     *                     callers should also pre-sanitise with [SecretMasker].
     */
    fun buildHelp(
        kind: ProviderErrorKind,
        providerName: String = "Anbieter",
        rawDetail: String? = null
    ): ProviderErrorHelp {
        val sanitisedDetail = rawDetail?.let { scrubDetail(it) }
        return when (kind) {
            ProviderErrorKind.AUTH_REJECTED -> ProviderErrorHelp(
                kind = kind,
                headline = "Schlüssel abgelehnt",
                explanation = "$providerName hat den API-Schlüssel nicht akzeptiert. " +
                    "Möglicherweise ist er abgelaufen, widerrufen oder gehört zu einem anderen Konto.",
                suggestedActions = listOf(
                    "Schlüssel in den Einstellungen prüfen und ggf. erneuern.",
                    "Neuen Schlüssel auf der Anbieter-Website erstellen.",
                    "Sicherstellen, dass der Schlüssel für diesen Anbieter bestimmt ist."
                ),
                mayHaveBeenBilled = false,
                technicalDetail = sanitisedDetail
            )

            ProviderErrorKind.RATE_LIMITED -> ProviderErrorHelp(
                kind = kind,
                headline = "Anfragelimit erreicht",
                explanation = "$providerName hat die Anfrage wegen zu hoher Nutzungsfrequenz " +
                    "oder erreichtem Kontingent abgelehnt.",
                suggestedActions = listOf(
                    "Kurz warten und erneut versuchen.",
                    "Kontingent oder Plan auf der Anbieter-Website prüfen.",
                    "Anfragehäufigkeit in den App-Einstellungen reduzieren."
                ),
                mayHaveBeenBilled = false,
                technicalDetail = sanitisedDetail
            )

            ProviderErrorKind.MODEL_NOT_FOUND -> ProviderErrorHelp(
                kind = kind,
                headline = "Modell nicht gefunden",
                explanation = "Das gewählte Modell ist bei $providerName nicht verfügbar. " +
                    "Es könnte umbenannt, eingestellt oder nur für bestimmte Pläne zugänglich sein.",
                suggestedActions = listOf(
                    "Modellname in den Chat-Einstellungen prüfen.",
                    "Verfügbare Modelle auf der Anbieter-Website nachschlagen.",
                    "Ein anderes Modell auswählen."
                ),
                mayHaveBeenBilled = false,
                technicalDetail = sanitisedDetail
            )

            ProviderErrorKind.BAD_REQUEST -> ProviderErrorHelp(
                kind = kind,
                headline = "Ungültige Anfrage",
                explanation = "$providerName hat die Anfrage wegen eines Formatfehlers abgelehnt. " +
                    "Die Nachricht wurde möglicherweise nicht verarbeitet.",
                suggestedActions = listOf(
                    "Erneut versuchen.",
                    "Nachricht kürzen oder Anhänge entfernen und nochmals senden.",
                    "Modell oder Anbietereinstellungen prüfen."
                ),
                mayHaveBeenBilled = false,
                technicalDetail = sanitisedDetail
            )

            ProviderErrorKind.SERVER_ERROR -> ProviderErrorHelp(
                kind = kind,
                headline = "Serverfehler beim Anbieter",
                explanation = "Bei $providerName ist ein vorübergehender Fehler aufgetreten. " +
                    "Die Anfrage könnte teilweise verarbeitet worden sein.",
                suggestedActions = listOf(
                    "Kurz warten und erneut versuchen.",
                    "Statusseite des Anbieters auf bekannte Störungen prüfen.",
                    "Bei anhaltenden Problemen einen anderen Anbieter wählen."
                ),
                mayHaveBeenBilled = true,   // 5xx may occur after processing has started
                technicalDetail = sanitisedDetail
            )

            ProviderErrorKind.NETWORK_UNREACHABLE -> ProviderErrorHelp(
                kind = kind,
                headline = "Keine Verbindung",
                explanation = "Der Anbieter war nicht erreichbar. " +
                    "Möglicherweise besteht kein Internet, oder der Anbieter ist vorübergehend offline.",
                suggestedActions = listOf(
                    "Internetverbindung prüfen.",
                    "Erneut versuchen, wenn die Verbindung wiederhergestellt ist.",
                    "Bei lokalem Server: sicherstellen, dass der Dienst läuft."
                ),
                mayHaveBeenBilled = false,
                technicalDetail = sanitisedDetail
            )

            ProviderErrorKind.TLS_ERROR -> ProviderErrorHelp(
                kind = kind,
                headline = "Sicherheitszertifikat ungültig",
                explanation = "Die Verbindung zu $providerName konnte nicht sicher hergestellt werden. " +
                    "Das Zertifikat ist möglicherweise abgelaufen, selbst signiert oder die URL ist falsch.",
                suggestedActions = listOf(
                    "Endpunkt-URL in den Einstellungen prüfen.",
                    "Zertifikat des Servers überprüfen.",
                    "Keinen Schlüssel über eine unsichere Verbindung senden."
                ),
                mayHaveBeenBilled = false,
                technicalDetail = sanitisedDetail
            )

            ProviderErrorKind.UNSAFE_REDIRECT -> ProviderErrorHelp(
                kind = kind,
                headline = "Unsichere Umleitung blockiert",
                explanation = "Die Verbindung zu $providerName wurde auf einen anderen Server umgeleitet. " +
                    "Die App hat die Umleitung blockiert, um Schlüssel nicht preiszugeben.",
                suggestedActions = listOf(
                    "Endpunkt-URL in den Einstellungen auf Richtigkeit prüfen.",
                    "Anbieter über die offizielle Dokumentation konfigurieren."
                ),
                mayHaveBeenBilled = false,
                technicalDetail = sanitisedDetail
            )

            ProviderErrorKind.UNKNOWN -> ProviderErrorHelp(
                kind = kind,
                headline = "Unbekannter Fehler",
                explanation = "Bei der Verbindung zu $providerName ist ein unerwartetes Problem aufgetreten.",
                suggestedActions = listOf(
                    "Erneut versuchen.",
                    "Einstellungen und Schlüssel prüfen.",
                    "App neu starten, wenn das Problem anhält."
                ),
                mayHaveBeenBilled = false,
                technicalDetail = sanitisedDetail
            )
        }
    }

    // ── Internal sanitisation ─────────────────────────────────────────────────

    /**
     * Best-effort removal of common secret patterns from a detail string.
     * This is defence-in-depth — callers must apply [SecretMasker] first.
     */
    private fun scrubDetail(raw: String): String = raw
        .replace(Regex("sk-ant-[A-Za-z0-9_\\-]{8,}"), "sk-ant-••••••••")
        .replace(Regex("sk-[A-Za-z0-9]{8,}"), "sk-••••••••")
        .replace(Regex("(?i)bearer\\s+[A-Za-z0-9._\\-]{8,}"), "Bearer ••••••••")
        .replace(Regex("(?i)x-api-key:\\s*[A-Za-z0-9._\\-]{8,}"), "x-api-key: ••••••••")
}
