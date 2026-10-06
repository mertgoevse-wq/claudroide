package org.claudroide.app.feature.provider

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
import org.claudroide.app.feature.chat.SseEvent
import org.claudroide.app.feature.chat.SseEventParser
import java.util.concurrent.TimeUnit

/**
 * Task 072+ — raw HTTP transport for LLM providers.
 *
 * The only production code that opens a network connection.
 * API keys come from [KeyVaultStorage]; this class never holds one.
 * Output is [SseEvent]s, never raw text.
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

        val authHeaders = when (descriptor.auth) {
            ProviderAuth.API_KEY_HEADER -> mapOf("x-api-key" to apiKey)
            ProviderAuth.BEARER -> mapOf("Authorization" to "Bearer $apiKey")
            ProviderAuth.NONE -> emptyMap()
        }

        val formatResult = when (descriptor.providerId) {
            "anthropic" -> AnthropicMessageFormatter.buildRequestBody(
                modelId, messages, system, maxTokens, stream = true
            )
            else -> OpenAiMessageFormatter.buildRequestBody(
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

        val request = Request.Builder()
            .url(descriptor.baseUrl)
            .post(requestBody)
            .apply { authHeaders.forEach { (k, v) -> addHeader(k, v) } }
            .addHeader("anthropic-version", "2023-06-01")
            .build()

        val call = httpClient.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                close(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val parser = SseEventParser()
                response.body?.charStream()?.buffered()?.use { reader ->
                    reader.lineSequence().forEach { line ->
                        parser.acceptLine(line)?.let { event ->
                            trySend(event).getOrThrow()
                        }
                    }
                }
                parser.acceptLine("")?.let { trySend(it).getOrThrow() }
                response.close()
                close()
            }
        })

        awaitClose { call.cancel() }
    }

    companion object {
        val DEFAULT_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

class ProviderConnectionException(message: String) : Exception(message)
