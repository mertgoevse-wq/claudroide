package org.claudroide.app.feature.provider

import java.net.URI

data class CustomEndpointConfig(
    val id: String,
    val displayName: String,
    val targetUrl: String,
    val protocolFormat: ApiProtocolFormat,
    val defaultModel: String,
    val customHeaders: Map<String, String> = emptyMap(),
    val supportsStreaming: Boolean = true,
    val supportsTools: Boolean = false
)

data class EndpointVerificationVerdict(
    val isSecure: Boolean,
    val errorDescription: String? = null,
    val isLocalhostExemption: Boolean = false
)

/**
 * Hardened validation and dispatch guard for custom API endpoints.
 * Enforces HTTPS TLS security, rejects private IP SSRF attacks,
 * and guarantees key isolation per target host.
 */
object CustomEndpointGuard {

    private val BLOCKED_PRIVATE_IP_PREFIXES = listOf(
        "10.", "172.16.", "172.17.", "172.18.", "172.19.", "172.20.",
        "172.21.", "172.22.", "172.23.", "172.24.", "172.25.", "172.26.",
        "172.27.", "172.28.", "172.29.", "172.30.", "172.31.",
        "192.168.", "169.254.", "fc00:", "fe80:"
    )

    fun verifyEndpointSecurity(url: String): EndpointVerificationVerdict {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return EndpointVerificationVerdict(false, "Ziel-URL darf nicht leer sein.")
        }

        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase() ?: ""
            val host = uri.host?.lowercase() ?: ""

            if (scheme != "https" && scheme != "http") {
                return EndpointVerificationVerdict(false, "Nur https:// oder lokales http:// zulässig.")
            }

            val isLocalhost = host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2"

            if (scheme == "http" && !isLocalhost) {
                return EndpointVerificationVerdict(
                    false,
                    "Unsicheres HTTP ist für externe Adressen verboten. TLS-Verschlüsselung (https://) erforderlich."
                )
            }

            // SSRF protection: reject internal network IP addresses unless specifically localhost
            val isPrivateIp = BLOCKED_PRIVATE_IP_PREFIXES.any { host.startsWith(it) }
            if (isPrivateIp && !isLocalhost) {
                return EndpointVerificationVerdict(
                    false,
                    "Verbindungen zu privaten LAN-Netzwerkadressen sind zum Schutz vor SSRF-Angriffen gesperrt."
                )
            }

            EndpointVerificationVerdict(isSecure = true, isLocalhostExemption = isLocalhost)
        } catch (e: Exception) {
            EndpointVerificationVerdict(false, "Ungültiges URL-Format.")
        }
    }

    /**
     * Verifies that credentials are only dispatched to the exact configured host.
     */
    fun canSendKeyToHost(configuredUrl: String, destinationUrl: String): Boolean {
        return try {
            val configuredUri = URI(configuredUrl)
            val destUri = URI(destinationUrl)

            val configuredHost = configuredUri.host?.lowercase() ?: ""
            val destHost = destUri.host?.lowercase() ?: ""

            configuredHost.isNotBlank() && configuredHost == destHost
        } catch (e: Exception) {
            false
        }
    }
}
