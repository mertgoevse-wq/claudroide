package org.claudroide.app

import org.claudroide.app.feature.chat.ConversationManager
import org.junit.Assert.*
import org.junit.Test

class ConversationManagerTest {

    @Test
    fun createConversation_defaultTitleWhenNoPrompt() {
        val conv = ConversationManager.createConversation()
        assertEquals(ConversationManager.DEFAULT_CHAT_TITLE, conv.title)
        assertFalse(conv.isArchived)
        assertFalse(conv.isProjectBound)
    }

    @Test
    fun createConversation_withProjectBinding() {
        val conv = ConversationManager.createConversation(
            initialPrompt = "Write unit tests",
            projectId = "p-1",
            projectName = "Claudroide Core"
        )
        assertEquals("Write unit tests", conv.title)
        assertTrue(conv.isProjectBound)
        assertEquals("p-1", conv.projectId)
        assertEquals("Claudroide Core", conv.projectName)
    }

    @Test
    fun autoGenerateTitle_sanitizesLeakedApiKeyInFirstPrompt() {
        val sensitivePrompt = "Debug connection using sk-ant-api03-abcdef1234567890 key"
        val title = ConversationManager.autoGenerateTitle(sensitivePrompt)
        assertFalse(title.contains("sk-ant-api03"))
        assertTrue(title.contains("[SCHLÜSSEL AUSGEBLENDET]"))
    }

    @Test
    fun autoGenerateTitle_clampsLongTitles() {
        val longPrompt = "This is an extremely detailed and exceptionally long instruction that exceeds the standard fifty characters limit"
        val title = ConversationManager.autoGenerateTitle(longPrompt)
        assertTrue(title.length <= ConversationManager.MAX_TITLE_LENGTH)
        assertTrue(title.endsWith("…"))
    }

    @Test
    fun renameConversation_updatesTitleAndGuardsBlank() {
        val original = ConversationManager.createConversation(initialPrompt = "Original")
        val renamed = ConversationManager.renameConversation(original, "Refactored Feature")
        assertEquals("Refactored Feature", renamed.title)

        // Blank rename falls back to default
        val blankRenamed = ConversationManager.renameConversation(renamed, "   ")
        assertEquals(ConversationManager.DEFAULT_CHAT_TITLE, blankRenamed.title)
    }

    @Test
    fun archiveAndUnarchive_managesReversibleState() {
        val active = ConversationManager.createConversation(initialPrompt = "Active discussion")
        assertFalse(active.isArchived)

        val archived = ConversationManager.archiveConversation(active)
        assertTrue(archived.isArchived)

        val restored = ConversationManager.unarchiveConversation(archived)
        assertFalse(restored.isArchived)
    }
}
