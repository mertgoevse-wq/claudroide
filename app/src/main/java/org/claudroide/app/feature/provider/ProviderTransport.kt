package org.claudroide.app.feature.provider

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.claudroide.app.feature.provider.network.SseEvent
import org.claudroide.app.feature.provider.network.SseEventParser
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Task 072+ — raw HTTP transport for LLM providers.
 *
 * The only production code that opens a network connection.
 * API keys come from [KeyVaultStorage]; this class never holds one.
 * Output is [SseEvent]s, never raw text.
 * 
 * Features:
 * - Proper HTTP status handling (non-2xx)
 * - Timeout handling
 * - Cancellation support
 * - Provider-specific headers and auth
 * - Protocol-specific formatting
 * - Stream error handling
 * - Usage/token data extraction
 */
class ProviderTransport(
    private val keyVault: KeyVaultStorage,
    private val httpClient: OkHttpClient = DEFAULT_CLIENT
) {

    fun streamRequest(
        descriptor: ProviderDescriptor,
        modelId: String,
        messages: List<ChatMessage>,
        system: String? = null,
        maxTokens: Int = 1024
    ): Flow<SseEvent> = callbackFlow {
        val apiKey = keyVault.retrieveKey(descriptor.providerId)
            ?: throw ProviderConnectionException("Kein Zugangsschlüssel fuer ${descriptor.providerId} hinterlegt.")

        val (headers, requestBody) = buildRequest(descriptor, modelId, messages, system, maxTokens, apiKey)

        val request = Request.Builder()
            .url(descriptor.baseUrl)
            .post(requestBody)
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()

        val callRef = AtomicReference<Call?>()

        val call = httpClient.newCall(request)
        callRef.set(call)

        // Allow cancellation from outside
        awaitClose { callRef.get()?.cancel() }

        val lastActivityMillis = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())

        // Idle timeout: cancel the stream only if no data arrives for SSE_IDLE_TIMEOUT_SECONDS
        val timeoutJob = launch {
            while (isActive) {
                delay(2000)
                val idleSeconds = (System.currentTimeMillis() - lastActivityMillis.get()) / 1000
                if (idleSeconds >= SSE_IDLE_TIMEOUT_SECONDS) {
                    if (!call.isCanceled()) {
                        callRef.get()?.cancel()
                        close(ProviderConnectionException("Stream-Timeout: keine Daten fuer ${SSE_IDLE_TIMEOUT_SECONDS}s"))
                    }
                    break
                }
            }
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                if (!call.isCanceled()) {
                    close(ProviderConnectionException("Verbindungsfehler: ${e.message}"))
                } else {
                    close(java.util.concurrent.CancellationException("Anfrage abgebrochen"))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                lastActivityMillis.set(System.currentTimeMillis())
                if (!response.isSuccessful) {
                    if (call.isCanceled()) {
                        response.close()
                        close(java.util.concurrent.CancellationException("Anfrage abgebrochen"))
                        return
                    }
                    val errorBody = response.body?.string() ?: "Keine Fehlerdetails"
                    response.close()
                    close(ProviderConnectionException(
                        "HTTP ${response.code}: ${response.message} - $errorBody"
                    ))
                    return
                }

                if (call.isCanceled()) {
                    response.close()
                    close(java.util.concurrent.CancellationException("Anfrage abgebrochen"))
                    return
                }

                val parser = SseEventParser()
                try {
                    response.body?.charStream()?.buffered()?.use { reader ->
                        reader.lineSequence().forEach { line ->
                            lastActivityMillis.set(System.currentTimeMillis())
                            if (call.isCanceled()) {
                                response.close()
                                close(java.util.concurrent.CancellationException("Anfrage abgebrochen"))
                                return@use
                            }
                            parser.acceptLine(line)?.let { event ->
                                trySend(event).getOrThrow()
                            }
                        }
                    }
                    parser.acceptLine("")?.let { trySend(it).getOrThrow() }
                } catch (e: Exception) {
                    if (e is java.util.concurrent.CancellationException || call.isCanceled()) {
                        close(java.util.concurrent.CancellationException("Anfrage abgebrochen"))
                    } else {
                        close(ProviderConnectionException("Stream-Fehler: ${e.message}"))
                    }
                } finally {
                    timeoutJob.cancel()
                    response.close()
                    close()
                }
            }
        })
    }

    /**
     * Builds the request headers and body for the specific provider.
     * Returns (headers, requestBody).
     */
    private fun buildRequest(
        descriptor: ProviderDescriptor,
        modelId: String,
        messages: List<ChatMessage>,
        system: String?,
        maxTokens: Int,
        apiKey: String
    ): Pair<Map<String, String>, okhttp3.RequestBody> {

        val authHeaders = when (descriptor.auth) {
            ProviderAuth.API_KEY_HEADER -> mapOf("x-api-key" to apiKey)
            ProviderAuth.BEARER -> mapOf("Authorization" to "Bearer $apiKey")
            ProviderAuth.NONE -> emptyMap<String, String>()
        }

        // Provider-specific headers
        val providerHeaders = buildProviderHeaders(descriptor.providerId, modelId)

        val allHeaders = authHeaders + providerHeaders

        // Get protocol format from catalog
        val catalogEntry = ProviderCatalogRegistry.getProvider(descriptor.providerId)
        val protocolFormat = catalogEntry?.protocolFormat ?: ApiProtocolFormat.OPENAI_COMPATIBLE

        val formatResult = when (protocolFormat) {
            ApiProtocolFormat.ANTHROPIC_MESSAGES -> AnthropicMessageFormatter.buildRequestBody(
                modelId, messages, system, maxTokens, stream = true
            )
            ApiProtocolFormat.OPENAI_COMPATIBLE -> OpenAiMessageFormatter.buildRequestBody(
                modelId, messages, system, maxTokens, stream = true
            )
        }

        val payload = when (formatResult) {
            is MessageFormatResult.Success -> formatResult.body
            is MessageFormatResult.FormatError ->
                throw ProviderConnectionException(formatResult.reason)
        }

        val requestBody = org.json.JSONObject(payload).toString()
            .toRequestBody("application/json".toMediaType())

        return allHeaders to requestBody
    }

    /**
     * Provider-specific headers that MUST be sent for each provider.
     * Critical: Anthropic headers only for Anthropic, not for others.
     */
    private fun buildProviderHeaders(providerId: String, modelId: String): Map<String, String> {
        return when (providerId) {
            "anthropic" -> mapOf(
                "anthropic-version" to "2023-06-01",
                "anthropic-beta" to "messages-2023-12-15"
            )
            "openrouter" -> mapOf(
                "HTTP-Referer" to "https://claudroide.app",
                "X-Title" to "ClauDroide"
            )
            else -> emptyMap()
        }
    }

    companion object {
        private const val SSE_IDLE_TIMEOUT_SECONDS = 90L

        val DEFAULT_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

class ProviderConnectionException(message: String) : Exception(message)
