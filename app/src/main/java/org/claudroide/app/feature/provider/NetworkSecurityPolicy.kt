package org.claudroide.app.feature.provider

import java.net.URI

/**
 * Classifies a TLS/certificate failure so the UI can show actionable guidance.
 */
enum class TlsFailureKind {
    /** Certificate is expired. */
    CERTIFICATE_EXPIRED,
    /** Certificate is self-signed or issued by an unknown CA. */
    UNTRUSTED_CERTIFICATE,
    /** Hostname in the certificate does not match the request host. */
    HOSTNAME_MISMATCH,
    /** TLS handshake timed out. */
    HANDSHAKE_TIMEOUT,
    /** Any other TLS-layer error. */
    OTHER
}

/**
 * Outcome of a redirect safety check.
 */
sealed class RedirectVerdict {
    /** Redirect is safe — same host, HTTPS (or same-host loopback HTTP). */
    data class Safe(val redirectUrl: String) : RedirectVerdict()

    /**
     * Redirect is blocked — credentials must not follow this redirect.
     * [reason] is a sanitised German-language explanation for the UI.
     */
    data class Blocked(val redirectUrl: String, val reason: String) : RedirectVerdict()
}

/**
 * Network-layer security policy for all outbound provider requests.
 *
 * Design invariants (each verified by unit tests):
 *  1. No API key or credential is ever sent over a non-HTTPS connection to a remote host.
 *  2. TLS certificate errors are never silently ignored — they always surface as [TlsFailureKind].
 *  3. Any redirect that changes the host is blocked before credentials are resent.
 *  4. HTTP/cleartext is only permitted to localhost addresses (documented developer exception).
 *  5. The localhost exception does not permit credential transmission outside loopback range.
 */
object NetworkSecurityPolicy {

    /**
     * Determines whether credentials may be sent to [targetUrl].
     * Returns null (allowed) or a German-language denial reason.
     *
     * Rules:
     *  - Remote hosts: HTTPS required.
     *  - Loopback (127.0.0.1 / ::1 / 10.0.2.2 / localhost): HTTP permitted as a documented exception.
     *  - Empty or unparseable URLs are always denied.
     */
    fun canTransmitCredentials(targetUrl: String): String? {
        val trimmed = targetUrl.trim()
        if (trimmed.isEmpty()) return "URL ist leer."

        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase() ?: return "Kein URL-Schema angegeben."
            val host = uri.host?.lowercase() ?: return "Kein Ziel-Host angegeben."

            if (scheme != "https" && scheme != "http") {
                return "Nur https:// und lokales http:// sind für Anmeldedaten zulässig."
            }

            if (scheme == "http" && !isLoopbackHost(host)) {
                return "Unverschlüsselte Verbindungen (http://) zu externen Hosts sind verboten. " +
                    "Schlüssel dürfen nur über TLS (https://) übertragen werden."
            }

            null  // allowed
        } catch (_: Exception) {
            "Ungültiges URL-Format — Schlüsselübertragung blockiert."
        }
    }

    /**
     * Evaluates whether following a redirect is safe for credential re-transmission.
     *
     * A redirect is safe only when:
     *  - The host has not changed compared to [originalUrl].
     *  - The destination is HTTPS, or same-host loopback HTTP.
     *
     * If the host changes the redirect is blocked regardless of scheme —
     * the caller must re-verify the new host separately before sending credentials.
     */
    fun evaluateRedirect(originalUrl: String, redirectUrl: String): RedirectVerdict {
        val trimmedRedirect = redirectUrl.trim()
        if (trimmedRedirect.isEmpty()) {
            return RedirectVerdict.Blocked(redirectUrl, "Umleitung zu leerer URL blockiert.")
        }

        return try {
            val origUri = URI(originalUrl.trim())
            val redirUri = URI(trimmedRedirect)

            val origHost = origUri.host?.lowercase() ?: ""
            val redirHost = redirUri.host?.lowercase() ?: ""
            val redirScheme = redirUri.scheme?.lowercase() ?: ""

            // Host change: always block credential forwarding.
            if (origHost != redirHost) {
                return RedirectVerdict.Blocked(
                    redirectUrl,
                    "Umleitung auf anderen Host ($redirHost) blockiert. " +
                        "Schlüssel werden nicht weitergeleitet."
                )
            }

            // Same host but downgraded to HTTP for a non-loopback host.
            if (redirScheme == "http" && !isLoopbackHost(redirHost)) {
                return RedirectVerdict.Blocked(
                    redirectUrl,
                    "HTTPS→HTTP-Downgrade blockiert. " +
                        "Schlüssel werden nicht über eine unverschlüsselte Verbindung gesendet."
                )
            }

            RedirectVerdict.Safe(redirectUrl)
        } catch (_: Exception) {
            RedirectVerdict.Blocked(redirectUrl, "Ungültige Umleitungs-URL — blockiert.")
        }
    }

    /**
     * Builds a user-facing explanation for a TLS failure.
     * Never includes raw key, certificate bytes, or server response body.
     */
    fun describeTlsFailure(kind: TlsFailureKind, host: String): String = when (kind) {
        TlsFailureKind.CERTIFICATE_EXPIRED ->
            "Das Sicherheitszertifikat von „$host“ ist abgelaufen. " +
                "Verbindung wurde nicht hergestellt."
        TlsFailureKind.UNTRUSTED_CERTIFICATE ->
            "Das Zertifikat von „$host“ ist nicht vertrauenswürdig (unbekannte CA oder selbst signiert). " +
                "Verbindung wurde nicht hergestellt."
        TlsFailureKind.HOSTNAME_MISMATCH ->
            "Der Hostname „$host“ stimmt nicht mit dem Zertifikat überein. " +
                "Verbindung wurde nicht hergestellt."
        TlsFailureKind.HANDSHAKE_TIMEOUT ->
            "Der TLS-Verbindungsaufbau zu „$host“ hat das Zeitlimit überschritten."
        TlsFailureKind.OTHER ->
            "Sicherheitsfehler bei der Verbindung zu „$host“. " +
                "Details wurden nicht protokolliert."
    }

    /**
     * Returns true when [host] is a loopback address (localhost exception).
     * Only loopback addresses may use HTTP for credential transmission.
     */
    fun isLoopbackHost(host: String): Boolean {
        val h = host.lowercase().removeSurrounding("[", "]")  // strip IPv6 brackets
        return h == "localhost" ||
            h == "127.0.0.1" ||
            h == "::1" ||
            h == "10.0.2.2"  // Android emulator host alias
    }
}
