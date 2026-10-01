package org.claudroide.app.feature.provider

import java.net.URI

enum class PingResultStatus {
    SUCCESS,
    AUTH_FAILED_401,
    NOT_FOUND_404,
    RATE_LIMITED_429,
    UNSAFE_REDIRECT_BLOCKED,
    NETWORK_OFFLINE,
    UNKNOWN_ERROR
}

data class PingRequestPayload(
    val endpointUrl: String,
    val modelId: String,
    val protocolFormat: ApiProtocolFormat,
    val minimalPrompt: String = "ping",
    val maxTokens: Int = 1,
    val costDisclosure: String = "Minimaler Testaufruf: ~2-5 Tokens (< $0.0001)",
    val containsZeroProjectData: Boolean = true
)

data class PingResponseResult(
    val status: PingResultStatus,
    val latencyMs: Long,
    val diagnosticMessage: String,
    val providerReportedModel: String? = null
)

/**
 * Executes a minimal test ping prior to full conversation dispatch.
 * Enforces zero project code exposure and blocks unsafe cross-host redirects.
 */
object ProviderPingTester {

    /**
     * Constructs a minimal, privacy-safe ping request payload.
     */
    fun createPingPayload(
        endpointUrl: String,
        modelId: String,
        protocol: ApiProtocolFormat
    ): PingRequestPayload {
        return PingRequestPayload(
            endpointUrl = endpointUrl.trim(),
            modelId = modelId.trim(),
            protocolFormat = protocol,
            minimalPrompt = "ping",
            maxTokens = 1,
            containsZeroProjectData = true
        )
    }

    /**
     * Serializes request body conforming to official protocol formats.
     */
    fun buildHttpRequestBody(payload: PingRequestPayload): String {
        return when (payload.protocolFormat) {
            ApiProtocolFormat.ANTHROPIC_MESSAGES -> {
                """{"model":"${payload.modelId}","max_tokens":${payload.maxTokens},"messages":[{"role":"user","content":"${payload.minimalPrompt}"}]}"""
            }
            ApiProtocolFormat.OPENAI_COMPATIBLE -> {
                """{"model":"${payload.modelId}","max_tokens":${payload.maxTokens},"messages":[{"role":"user","content":"${payload.minimalPrompt}"}]}"""
            }
        }
    }

    /**
     * Prevents security attacks by rejecting redirects across different domain names
     * or downgrades from https to http.
     */
    fun isRedirectSafe(originalUrl: String, targetRedirectUrl: String): Boolean {
        return try {
            val originalUri = URI(originalUrl)
            val targetUri = URI(targetRedirectUrl)

            // Block HTTPS -> HTTP downgrade
            if (originalUri.scheme == "https" && targetUri.scheme != "https") {
                return false
            }

            // Strictly require same host (subdomain changes allowed only if parent matches)
            val origHost = originalUri.host?.lowercase() ?: ""
            val targetHost = targetUri.host?.lowercase() ?: ""

            origHost == targetHost
        } catch (e: Exception) {
            false
        }
    }
}
