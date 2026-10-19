package org.claudroide.app

import org.claudroide.app.feature.chat.ChatInputState
import org.claudroide.app.feature.chat.ChatMessage
import org.claudroide.app.feature.chat.ChatViewModel
import org.claudroide.app.feature.chat.InMemorySessionStore
import org.claudroide.app.feature.chat.SessionRecord
import org.claudroide.app.feature.provider.network.StreamState
import org.claudroide.app.feature.provider.KeyVaultStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.claudroide.app.feature.provider.ChatMessage as ProviderChatMessage
import org.claudroide.app.feature.provider.ProviderDescriptor
import org.claudroide.app.feature.provider.network.SseEvent
import org.junit.After
import org.junit.Before

/**
 * Task 070+ — ChatViewModel tests.
 *
 * Tests the ViewModel that wires ChatScreen to the provider transport.
 * Uses a fake KeyVaultStorage so no real keys or network calls are involved.
 */
class ChatViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ── Input handling ─────────────────────────────────────────────────────────

    @Test
    fun onInputChange_updatesState() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        vm.onInputChange("Hallo")
        val state = vm.uiState.value
        assertEquals("Hallo", state.input.text)
    }

    // ── Send without provider ──────────────────────────────────────────────────

    @Test
    fun sendMessage_withoutKey_showsError() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        vm.onProviderSelected("anthropic", "claude-sonnet-5-5")
        vm.onInputChange("Test")
        vm.sendMessage()
        val state = vm.uiState.value
        assertTrue(state.messages.isNotEmpty())
        assertTrue(state.messages[0].fromUser)
        assertEquals("Test", state.messages[0].text)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Zugangsschlüssel"))
    }

    // ── Send with unknown provider ─────────────────────────────────────────────

    @Test
    fun sendMessage_withUnknownProvider_showsError() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        vm.onProviderSelected("nonexistent", "model")
        vm.onInputChange("Test")
        vm.sendMessage()
        val state = vm.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("nicht gefunden"))
    }

    // ── New conversation clears messages ──────────────────────────────────────

    @Test
    fun sendMessage_persistsCurrentSessionBeforeProviderFailure() = runTest {
        val store = InMemorySessionStore()
        val vm = ChatViewModel(keyVault = FakeKeyVault(), sessionStore = store)
        vm.onInputChange("Question to keep")
        vm.sendMessage()

        val stored = store.loadSession(vm.uiState.value.conversationId)
        assertEquals("Question to keep", stored?.messages?.first())
        assertTrue(stored?.messages?.last()?.contains("Zugangsschlüssel") == true)
    }

    @Test
    fun newConversation_clearsMessages() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        vm.onInputChange(" Erst ")
        vm.sendMessage()
        assertTrue(vm.uiState.value.messages.isNotEmpty())

        vm.newConversation()
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertNotNull(vm.uiState.value.conversationId)
    }

    @Test
    fun startup_loadsRecentSessionsAndRestoresMostRecentTranscript() = runTest {
        val store = InMemorySessionStore()
        store.save(SessionRecord("older", "openrouter", "older-model", listOf("older question"), 1L))
        store.save(SessionRecord("newer", "anthropic", "newer-model", listOf("new question", "new answer"), 2L))

        val vm = ChatViewModel(keyVault = FakeKeyVault(), sessionStore = store)

        assertEquals(listOf("newer", "older"), vm.uiState.value.recentSessions.map { it.conversationId })
        assertEquals("newer", vm.uiState.value.conversationId)
        assertEquals(listOf("new question", "new answer"), vm.uiState.value.messages.map { it.text })
        assertEquals("anthropic", vm.uiState.value.currentProviderId)
        assertEquals("newer-model", vm.uiState.value.currentModelId)
    }

    @Test
    fun selectingSavedConversation_restoresItsTranscriptAndProvider() = runTest {
        val store = InMemorySessionStore()
        store.save(
            SessionRecord(
                "second-chat", "openrouter", "selected-model", listOf("review this", "Looks good"), 1L,
            )
        )
        val vm = ChatViewModel(keyVault = FakeKeyVault(), sessionStore = store)

        vm.selectConversation("second-chat", "Second chat")

        assertEquals("second-chat", vm.uiState.value.conversationId)
        assertEquals(listOf("review this", "Looks good"), vm.uiState.value.messages.map { it.text })
        assertTrue(vm.uiState.value.messages[0].fromUser)
        assertFalse(vm.uiState.value.messages[1].fromUser)
        assertEquals("openrouter", vm.uiState.value.currentProviderId)
        assertEquals("selected-model", vm.uiState.value.currentModelId)
    }

    @Test
    fun selectingUnknownConversation_keepsCurrentConversationAndReportsError() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault(), sessionStore = InMemorySessionStore())
        val currentId = vm.uiState.value.conversationId

        vm.selectConversation("missing", "Missing chat")

        assertEquals(currentId, vm.uiState.value.conversationId)
        assertNotNull(vm.uiState.value.error)
    }

    // ── Stop streaming ─────────────────────────────────────────────────────────

    @Test
    fun stopStreaming_setsFlag() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        vm.onInputChange("Test")
        vm.sendMessage()
        assertFalse(vm.uiState.value.isStreaming)
    }

    @Test
    fun recentSessionTitlesAndPreviews_redactCredentialPatterns() = runTest {
        val store = InMemorySessionStore()
        store.save(
            SessionRecord(
                conversationId = "secret-chat",
                providerId = "anthropic",
                modelId = "model",
                messages = listOf("sk-ant-abcdefghijk12345", "Bearer abcdefghijklmnop123456"),
                timestamp = 1L,
            )
        )

        val vm = ChatViewModel(keyVault = FakeKeyVault(), sessionStore = store)

        val recent = vm.uiState.value.recentSessions.single()
        assertFalse(recent.title.contains("sk-ant-"))
        assertFalse(recent.preview.contains("Bearer"))
        assertTrue(recent.title.contains("AUSGEBLENDET"))
        assertTrue(recent.preview.contains("[REDACTED]"))
    }

    // ── Provider selection ─────────────────────────────────────────────────────

    @Test
    fun onProviderSelected_updatesStateAndPersistsSelection() = runTest {
        val store = InMemorySessionStore()
        val vm = ChatViewModel(keyVault = FakeKeyVault(), sessionStore = store)

        vm.onProviderSelected("openrouter", "selected-model")

        val state = vm.uiState.value
        assertEquals("openrouter", state.currentProviderId)
        assertEquals("selected-model", state.currentModelId)
        val saved = store.loadSession(state.conversationId)
        assertEquals("openrouter", saved?.providerId)
        assertEquals("selected-model", saved?.modelId)
    }

    @Test
    fun providerSelection_isRejectedDuringStreaming() = runTest {
        val heldStream = kotlinx.coroutines.CompletableDeferred<Unit>()
        var requestProvider = ""
        val transport = object : org.claudroide.app.feature.chat.ChatStreamTransport {
            override fun streamRequest(
                descriptor: ProviderDescriptor,
                modelId: String,
                messages: List<ProviderChatMessage>,
                maxTokens: Int,
            ): Flow<SseEvent> = flow {
                requestProvider = descriptor.providerId
                heldStream.await()
            }
        }
        val vm = ChatViewModel(keyVault = FakeKeyVault(), transport = transport)
        vm.onInputChange("Hold the stream")
        vm.sendMessage()
        runCurrent()
        assertTrue(vm.uiState.value.isStreaming)
        assertEquals("anthropic", requestProvider)

        vm.onProviderSelected("openrouter", "other-model")
        assertEquals("anthropic", vm.uiState.value.currentProviderId)
        assertEquals("claude-sonnet-5-5", vm.uiState.value.currentModelId)
        assertNotNull(vm.uiState.value.error)

        assertTrue(vm.uiState.value.isStreaming)
        heldStream.complete(Unit)
        runCurrent()
    }

    // ── Error clearing ─────────────────────────────────────────────────────────

    @Test
    fun clearError_removesError() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        vm.onInputChange("Test")
        vm.sendMessage()
        assertNotNull(vm.uiState.value.error)

        vm.clearError()
        assertNull(vm.uiState.value.error)
    }

    // ── Conversation ID is unique ──────────────────────────────────────────────

    @Test
    fun eachViewModel_hasUniqueConversationId() = runTest {
        val vm1 = ChatViewModel(keyVault = FakeKeyVault())
        val vm2 = ChatViewModel(keyVault = FakeKeyVault())
        assertTrue(vm1.uiState.value.conversationId.isNotBlank())
        assertTrue(vm2.uiState.value.conversationId.isNotBlank())
    }

    // ── Attachment sheet toggle ─────────────────────────────────────────────────

    @Test
    fun onAttachmentClick_showsAttachmentSheet() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        assertFalse(vm.uiState.value.showAttachmentSheet)

        vm.onAttachmentClick()
        assertTrue(vm.uiState.value.showAttachmentSheet)
    }

    @Test
    fun onAttachmentClick_togglesAttachmentSheet() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())

        vm.onAttachmentClick()
        assertTrue(vm.uiState.value.showAttachmentSheet)

        vm.onAttachmentClick()
        assertFalse(vm.uiState.value.showAttachmentSheet)
    }

    @Test
    fun onDismissAttachmentSheet_hidesAttachmentSheet() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())

        vm.onAttachmentClick()
        assertTrue(vm.uiState.value.showAttachmentSheet)

        vm.onDismissAttachmentSheet()
        assertFalse(vm.uiState.value.showAttachmentSheet)
    }

    @Test
    fun addAttachment_and_removeAttachment_manageAttachmentsList() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        assertEquals(0, vm.uiState.value.input.attachments.size)

        vm.addAttachment("test.kt", 1024)
        assertEquals(1, vm.uiState.value.input.attachments.size)
        val attachment = vm.uiState.value.input.attachments[0]
        assertEquals("test.kt", attachment.name)
        assertEquals(1024L, attachment.sizeBytes)

        vm.removeAttachment(attachment.id)
        assertEquals(0, vm.uiState.value.input.attachments.size)
    }

    @Test
    fun onModeClick_and_onDismissModelDialog_toggleState() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        assertFalse(vm.uiState.value.showModelDialog)

        vm.onModeClick()
        assertTrue(vm.uiState.value.showModelDialog)

        vm.onDismissModelDialog()
        assertFalse(vm.uiState.value.showModelDialog)
    }

    @Test
    fun onVoiceClick_and_onDismissVoiceInfoDialog_toggleState() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        assertFalse(vm.uiState.value.showVoiceInfoDialog)

        vm.onVoiceClick()
        assertTrue(vm.uiState.value.showVoiceInfoDialog)

        vm.onDismissVoiceInfoDialog()
        assertFalse(vm.uiState.value.showVoiceInfoDialog)
    }

    @Test
    fun onProfileClick_and_onDismissProfileDialog_toggleState() = runTest {
        val vm = ChatViewModel(keyVault = FakeKeyVault())
        assertFalse(vm.uiState.value.showProfileDialog)

        vm.onProfileClick()
        assertTrue(vm.uiState.value.showProfileDialog)

        vm.onDismissProfileDialog()
        assertFalse(vm.uiState.value.showProfileDialog)
    }

}

/** Minimal key vault that returns null for every key. */
private class FakeKeyVault(private val apiKey: String? = null) : KeyVaultStorage {
    override fun storeKey(providerId: String, apiKey: String) {}
    override fun retrieveKey(providerId: String): String? = apiKey
    override fun removeKey(providerId: String): Boolean = false
    override fun hasKey(providerId: String): Boolean = apiKey != null
    override fun clearAllKeys() {}
    override fun exportSafeMetadata(): Map<String, String> = emptyMap()
}
