package org.claudroide.app.feature.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
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
import org.claudroide.app.feature.provider.network.SseEvent
import org.claudroide.app.feature.provider.network.StreamState
import org.claudroide.app.feature.provider.network.StreamingResponseEngine

/**
 * Task 070+ — "Gemeinsame Agent-Funktionen" / Chat session state.
 *
 * Owns the live chat: messages, streaming state, provider selection, session persistence.
 * This is the only ViewModel in the app and the bridge between ChatScreen and the transport.
 */
interface ChatStreamTransport {
    fun streamRequest(
        descriptor: ProviderDescriptor,
        modelId: String,
        messages: List<org.claudroide.app.feature.provider.ChatMessage>,
        maxTokens: Int = 1024,
    ): Flow<SseEvent>
}

class ChatViewModel @JvmOverloads constructor(
    application: Application,
    private val keyVault: KeyVaultStorage = KeyVaultFactory.create(application),
    private val transport: ChatStreamTransport = ProviderTransport(keyVault),
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
        transport: ChatStreamTransport = ProviderTransport(keyVault),
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
        override suspend fun sessions(limit: Int): List<SessionRecord> = emptyList()
        override suspend fun loadSession(conversationId: String): SessionRecord? = null
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Reference to the active streaming job so stopStreaming can cancel it.
    private var streamingJob: Job? = null

    init {
        // Restore last session on startup (Task 039 — Sitzungen fortsetzen)
        viewModelScope.launch {
            val sessions = runCatching { sessionStore.sessions() }.getOrDefault(emptyList())
            val last = sessions.firstOrNull()
            _uiState.value = _uiState.value.copy(
                recentSessions = sessions.map { it.toRecentSession() },
            )
            if (last != null) restoreSession(last)
        }
    }

    fun onInputChange(newText: String) {
        _uiState.value = _uiState.value.copy(
            input = _uiState.value.input.copy(text = newText)
        )
    }

    fun onProviderSelected(providerId: String, modelId: String) {
        val state = _uiState.value
        if (state.isStreaming) {
            _uiState.value = state.copy(error = "Stoppe zuerst die laufende Antwort, bevor du den Anbieter wechselst.")
            return
        }
        _uiState.value = state.copy(
            currentProviderId = providerId,
            currentModelId = modelId,
        )
        persistSession()
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
        persistSession()

        streamingJob = viewModelScope.launch {
            val engine = StreamingResponseEngine()
            val history = buildHistory(_uiState.value.messages.dropLast(1), text)

            try {
                transport.streamRequest(
                    descriptor = descriptor,
                    modelId = state.currentModelId,
                    messages = history,
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
        if (_uiState.value.isStreaming) {
            _uiState.value = _uiState.value.copy(error = "Stoppe zuerst die laufende Antwort, bevor du einen neuen Chat öffnest.")
            return
        }
        persistSession()
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            conversationId = generateConversationId(),
            streamState = StreamState.IDLE,
            error = null,
        )
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
                    streamState = state.streamState,
                )
            )
            val sessions = runCatching { sessionStore.sessions() }.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                recentSessions = sessions.map { it.toRecentSession() },
            )
        }
    }

    private fun generateConversationId(): String =
        "conv_${System.currentTimeMillis()}_${(0..999).random()}"

    fun onAttachmentClick() {
        _uiState.value = _uiState.value.copy(
            showAttachmentSheet = !_uiState.value.showAttachmentSheet
        )
    }

    fun onDismissAttachmentSheet() {
        _uiState.value = _uiState.value.copy(showAttachmentSheet = false)
    }

    fun onModeClick() {
        _uiState.value = _uiState.value.copy(showModelDialog = true)
    }

    fun onDismissModelDialog() {
        _uiState.value = _uiState.value.copy(showModelDialog = false)
    }

    fun onVoiceClick() {
        _uiState.value = _uiState.value.copy(showVoiceInfoDialog = true)
    }

    fun onDismissVoiceInfoDialog() {
        _uiState.value = _uiState.value.copy(showVoiceInfoDialog = false)
    }

    fun onProfileClick() {
        _uiState.value = _uiState.value.copy(showProfileDialog = true)
    }

    fun onDismissProfileDialog() {
        _uiState.value = _uiState.value.copy(showProfileDialog = false)
    }

    fun selectConversation(conversationId: String, title: String) {
        if (_uiState.value.isStreaming) {
            _uiState.value = _uiState.value.copy(error = "Stoppe zuerst die laufende Antwort, bevor du den Chat wechselst.")
            return
        }
        viewModelScope.launch {
            val session = runCatching { sessionStore.loadSession(conversationId) }.getOrNull()
            if (session == null) {
                _uiState.value = _uiState.value.copy(error = "Die Unterhaltung konnte nicht gefunden werden.")
                return@launch
            }
            restoreSession(session)
        }
    }

    private fun restoreSession(session: SessionRecord) {
        _uiState.value = _uiState.value.copy(
            messages = session.messages.mapIndexed { index, text ->
                ChatMessage(
                    id = "${session.conversationId}_$index",
                    text = text,
                    fromUser = index % 2 == 0,
                )
            },
            currentProviderId = session.providerId,
            currentModelId = session.modelId,
            conversationId = session.conversationId,
            streamState = session.streamState.takeUnless {
                it == StreamState.CONNECTING || it == StreamState.RECEIVING
            } ?: StreamState.ABORTED,
            error = null,
        )
    }

    private fun SessionRecord.toRecentSession(): RecentSession {
        val title = messages.firstOrNull { it.isNotBlank() }
            ?.let(ConversationManager::autoGenerateTitle)
            ?: ConversationManager.DEFAULT_CHAT_TITLE
        val preview = messages.lastOrNull().orEmpty()
        return RecentSession(
            conversationId = conversationId,
            title = org.claudroide.app.feature.provider.SecretMasker.redact(title),
            preview = org.claudroide.app.feature.provider.SecretMasker.redact(preview),
            timestamp = timestamp,
        )
    }

    fun addAttachment(
        name: String,
        sizeBytes: Long,
        mimeType: String = "text/plain",
        isTextOrCode: Boolean = true,
    ) {
        val item = AttachmentItem(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            isTextOrCode = isTextOrCode,
            providerConsentGiven = true,
        )
        _uiState.value = _uiState.value.copy(
            input = _uiState.value.input.copy(
                attachments = _uiState.value.input.attachments + item
            )
        )
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
    val lastUsage: TokenUsage? = null,
    val recentSessions: List<RecentSession> = emptyList(),
    val showAttachmentSheet: Boolean = false,
    val showModelDialog: Boolean = false,
    val showVoiceInfoDialog: Boolean = false,
    val showProfileDialog: Boolean = false,
)

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int
) {
    val total: Int get() = inputTokens + outputTokens
}

/** In-memory fallback for JVM tests. */
class InMemorySessionStore : SessionPersistence {
    private val records = linkedMapOf<String, SessionRecord>()
    override suspend fun save(record: SessionRecord) {
        records[record.conversationId] = record
    }
    override suspend fun lastSession(): SessionRecord? = sessions().firstOrNull()
    override suspend fun sessions(limit: Int): List<SessionRecord> =
        records.values.sortedByDescending { it.timestamp }.take(limit.coerceIn(0, 25))
    override suspend fun loadSession(conversationId: String): SessionRecord? = records[conversationId]
}

private fun generateConversationId(): String =
    "conv_${System.currentTimeMillis()}_${(0..999).random()}"
