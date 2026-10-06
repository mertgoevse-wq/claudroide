package org.claudroide.app.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.claudroide.app.feature.provider.InMemorySecureKeyVault
import org.claudroide.app.feature.provider.KeyVaultStorage
import org.claudroide.app.feature.provider.ProviderAuth
import org.claudroide.app.feature.provider.ProviderAuthType
import org.claudroide.app.feature.provider.ProviderCatalogRegistry
import org.claudroide.app.feature.provider.ProviderDescriptor
import org.claudroide.app.feature.provider.ProviderTransport

/**
 * Task 070+ — "Gemeinsame Agent-Funktionen" / Chat session state.
 *
 * Owns the live chat: messages, streaming state, provider selection, session persistence.
 * This is the only ViewModel in the app and the bridge between ChatScreen and the transport.
 */
class ChatViewModel(
    private val keyVault: KeyVaultStorage,
    private val transport: ProviderTransport = ProviderTransport(keyVault),
    private val sessionStore: SessionPersistence = InMemorySessionStore()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Restore last session on startup (Task 039 — Sitzungen fortsetzen)
        viewModelScope.launch {
            val last = sessionStore.lastSession()
            if (last != null && last.messages.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    messages = last.messages.mapIndexed { i, text ->
                        ChatMessage(
                            id = "restored_$i",
                            text = text,
                            fromUser = i % 2 == 0
                        )
                    },
                    currentProviderId = last.providerId,
                    currentModelId = last.modelId,
                    conversationId = last.conversationId
                )
            }
        }
    }

    fun onInputChange(newText: String) {
        _uiState.value = _uiState.value.copy(
            input = _uiState.value.input.copy(text = newText)
        )
    }

    fun onProviderSelected(providerId: String, modelId: String) {
        _uiState.value = _uiState.value.copy(
            currentProviderId = providerId,
            currentModelId = modelId
        )
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.input.text.trim()
        if (text.isBlank() || state.isStreaming) return

        val providerId = state.currentProviderId
        if (providerId.isBlank()) {
            _uiState.value = state.copy(
                input = state.input.copy(text = ""),
                error = "Kein Anbieter ausgewaehlt."
            )
            return
        }

        val entry = ProviderCatalogRegistry.getProvider(providerId)
            ?: run {
                _uiState.value = state.copy(error = "Anbieter '$providerId' nicht gefunden.")
                return
            }

        val descriptor = ProviderDescriptor(
            providerId = entry.id,
            displayName = entry.displayName,
            baseUrl = entry.defaultEndpoint,
            auth = when {
                entry.allowedAuthTypes.contains(ProviderAuthType.API_KEY_HEADER) -> ProviderAuth.API_KEY_HEADER
                entry.allowedAuthTypes.contains(ProviderAuthType.BEARER_TOKEN) -> ProviderAuth.BEARER
                else -> ProviderAuth.NONE
            },
            documentationUrl = entry.documentationUrl
        )

        val userMessage = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            text = text,
            fromUser = true
        )

        val assistantPlaceholder = ChatMessage(
            id = "asst_${System.currentTimeMillis()}",
            text = "",
            fromUser = false,
            isStreaming = true
        )

        _uiState.value = state.copy(
            messages = state.messages + userMessage + assistantPlaceholder,
            input = state.input.copy(text = ""),
            isStreaming = true,
            error = null,
            streamState = StreamState.CONNECTING
        )

        viewModelScope.launch {
            val engine = StreamingResponseEngine()
            val history = buildHistory(_uiState.value.messages.dropLast(1), text)

            try {
                transport.streamRequest(
                    descriptor = descriptor,
                    modelId = state.currentModelId,
                    messages = history,
                    system = null,
                    maxTokens = 1024
                ).collect { event ->
                    engine.accept(event)
                    val response = engine.current()

                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages.dropLast(1) + ChatMessage(
                            id = _uiState.value.messages.last().id,
                            text = response.text,
                            fromUser = false,
                            isStreaming = !response.isComplete
                        ),
                        streamState = response.state,
                        lastUsage = if (response.usageReported) {
                            TokenUsage(response.inputTokens ?: 0, response.outputTokens ?: 0)
                        } else _uiState.value.lastUsage
                    )

                    if (response.state == StreamState.COMPLETED || response.state == StreamState.FAILED) {
                        persistSession()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages.dropLast(1) + ChatMessage(
                        id = _uiState.value.messages.last().id,
                        text = "Fehler: ${e.message}",
                        fromUser = false,
                        isStreaming = false
                    ),
                    isStreaming = false,
                    streamState = StreamState.FAILED,
                    error = e.message
                )
            }
        }
    }

    fun stopStreaming() {
        _uiState.value = _uiState.value.copy(isStreaming = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun newConversation() {
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            conversationId = generateConversationId(),
            streamState = StreamState.IDLE
        )
        persistSession()
    }

    private fun buildHistory(previous: List<ChatMessage>, newUserText: String): List<org.claudroide.app.feature.provider.ChatMessage> {
        val history = previous.map { m ->
            org.claudroide.app.feature.provider.ChatMessage(
                role = if (m.fromUser) "user" else "assistant",
                content = m.text
            )
        }
        return history + org.claudroide.app.feature.provider.ChatMessage("user", newUserText)
    }

    private fun persistSession() {
        val state = _uiState.value
        viewModelScope.launch {
            sessionStore.save(
                SessionRecord(
                    conversationId = state.conversationId,
                    providerId = state.currentProviderId,
                    modelId = state.currentModelId,
                    messages = state.messages.map { it.text },
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun generateConversationId(): String =
        "conv_${System.currentTimeMillis()}_${(0..999).random()}"
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: ChatInputState = ChatInputState(),
    val currentProviderId: String = "anthropic",
    val currentModelId: String = "claude-sonnet-5-5",
    val isStreaming: Boolean = false,
    val streamState: StreamState = StreamState.IDLE,
    val conversationId: String = generateConversationId(),
    val error: String? = null,
    val lastUsage: TokenUsage? = null
)

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int
) {
    val total: Int get() = inputTokens + outputTokens
}

interface SessionPersistence {
    suspend fun save(record: SessionRecord)
    suspend fun lastSession(): SessionRecord?
}

data class SessionRecord(
    val conversationId: String,
    val providerId: String,
    val modelId: String,
    val messages: List<String>,
    val timestamp: Long
)

/** In-memory fallback until encrypted DataStore is wired. */
class InMemorySessionStore : SessionPersistence {
    private var last: SessionRecord? = null
    override suspend fun save(record: SessionRecord) { last = record }
    override suspend fun lastSession(): SessionRecord? = last
}

private fun generateConversationId(): String =
    "conv_${System.currentTimeMillis()}_${(0..999).random()}"
