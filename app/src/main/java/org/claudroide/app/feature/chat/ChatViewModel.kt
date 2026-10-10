package org.claudroide.app.feature.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.claudroide.app.feature.provider.KeyVaultFactory
import org.claudroide.app.feature.provider.KeyVaultStorage
import org.claudroide.app.feature.provider.ProviderAuth
import org.claudroide.app.feature.provider.ProviderAuthType
import org.claudroide.app.feature.provider.ProviderCatalogRegistry
import org.claudroide.app.feature.provider.ProviderDescriptor
import org.claudroide.app.feature.provider.ProviderTransport
import org.claudroide.app.feature.provider.network.StreamState
import org.claudroide.app.feature.provider.network.StreamingResponseEngine

/**
 * Task 070+ — "Gemeinsame Agent-Funktionen" / Chat session state.
 *
 * Owns the live chat: messages, streaming state, provider selection, session persistence.
 * This is the only ViewModel in the app and the bridge between ChatScreen and the transport.
 */
class ChatViewModel @JvmOverloads constructor(
    application: Application,
    private val keyVault: KeyVaultStorage = KeyVaultFactory.create(application),
    private val transport: ProviderTransport = ProviderTransport(keyVault),
    private val sessionStore: SessionPersistence = SessionDataStore(application)
) : AndroidViewModel(application) {

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                        return ChatViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }

    /** Test-only constructor — no Android Application required. */
    @Suppress("unused")
    constructor(
        keyVault: KeyVaultStorage,
        transport: ProviderTransport = ProviderTransport(keyVault),
        sessionStore: SessionPersistence = NoOpSessionStore
    ) : this(StubApplication(), keyVault, transport, sessionStore) {
        // Skip Android-specific session restoration in tests.
    }

    private class StubApplication : Application() {
        // Stub exists only so the primary constructor has a non-null Application.
        // No Android runtime is required — the test constructor never invokes
        // Application lifecycle methods.
    }

    private object NoOpSessionStore : SessionPersistence {
        override suspend fun lastSession(): SessionRecord? = null
        override suspend fun save(session: SessionRecord) {}
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Reference to the active streaming job so stopStreaming can cancel it.
    private var streamingJob: Job? = null

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
                    conversationId = last.conversationId,
                    streamState = last.streamState
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

        streamingJob = viewModelScope.launch {
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
                        isStreaming = !response.isComplete,
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
            } finally {
                _uiState.value = _uiState.value.copy(isStreaming = false)
                streamingJob = null
                persistSession()
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        val engine = StreamingResponseEngine()
        engine.abort()
        _uiState.value = _uiState.value.copy(
            isStreaming = false,
            streamState = StreamState.ABORTED
        )
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
                    timestamp = System.currentTimeMillis(),
                    streamState = state.streamState
                )
            )
        }
    }

    private fun generateConversationId(): String =
        "conv_${System.currentTimeMillis()}_${(0..999).random()}"

    fun onAttachmentClick() {
        // TODO: Open attachment bottom sheet
    }

    fun onModeClick() {
        // TODO: Open mode selector
    }

    fun removeAttachment(attachmentId: String) {
        _uiState.value = _uiState.value.copy(
            input = _uiState.value.input.copy(
                attachments = _uiState.value.input.attachments.filter { it.id != attachmentId }
            )
        )
    }
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

/** In-memory fallback for JVM tests. */
class InMemorySessionStore : SessionPersistence {
    private var last: SessionRecord? = null
    override suspend fun save(record: SessionRecord) { last = record }
    override suspend fun lastSession(): SessionRecord? = last
}

private fun generateConversationId(): String =
    "conv_${System.currentTimeMillis()}_${(0..999).random()}"
