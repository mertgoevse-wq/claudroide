package org.claudroide.app

import org.claudroide.app.feature.chat.ChatExportManager
import org.claudroide.app.feature.chat.ExportFormat
import org.claudroide.app.feature.chat.ManagedConversation
import org.junit.Assert.*
import org.junit.Test

class ChatExportTest {

    private val sampleConversation = ManagedConversation(
        id = "conv-100",
        title = "Debug API Integration",
        projectId = "proj-cloud",
        projectName = "Cloud SDK",
        lastModifiedTimestampMs = 1700000000000L,
        messages = listOf(
            "Hello, let's configure client with sk-ant-api03-abcdef1234567890 key",
            "Here is the sanitized response without secrets"
        )
    )

    @Test
    fun markdownExport_formatsNicelyAndScrubsSecrets() {
        val md = ChatExportManager.generateExportPayload(sampleConversation, ExportFormat.MARKDOWN)

        assertTrue(md.contains("# Debug API Integration"))
        assertTrue(md.contains("> Projekt: Cloud SDK"))
        assertTrue(md.contains("### **User**"))
        assertTrue(md.contains("### **Assistant**"))

        // Invariant: Exported file must never contain the raw API key
        assertFalse(md.contains("sk-ant-api03"))
        assertTrue(md.contains("[SCHLÜSSEL AUSGEBLENDET]"))
    }

    @Test
    fun jsonExport_generatesValidJsonAndScrubsSecrets() {
        val json = ChatExportManager.generateExportPayload(sampleConversation, ExportFormat.JSON)

        assertTrue(json.contains("\"id\": \"conv-100\""))
        assertTrue(json.contains("\"title\": \"Debug API Integration\""))
        assertTrue(json.contains("\"projectId\": \"proj-cloud\""))

        // Invariant: Exported JSON must scrub secrets
        assertFalse(json.contains("sk-ant-api03"))
        assertTrue(json.contains("[SCHLÜSSEL AUSGEBLENDET]"))
    }

    @Test
    fun exportScope_previewsAccurateMetricsAndFlagsContainedSecrets() {
        val scope = ChatExportManager.getExportScope(sampleConversation, ExportFormat.MARKDOWN)

        assertEquals("Debug API Integration", scope.conversationTitle)
        assertEquals(2, scope.messageCount)
        assertTrue(scope.estimatedBytes > 0)
        assertTrue(scope.containsSanitizedSecrets)
        assertEquals(ExportFormat.MARKDOWN, scope.targetFormat)
    }

    @Test
    fun confirmAndDelete_requiresExplicitConfirmation() {
        // Without confirmation: deletion is rejected
        val rejected = ChatExportManager.confirmAndDeleteConversation(sampleConversation, userConfirmed = false)
        assertNull(rejected)

        // With explicit confirmation: deletion succeeds and memory messages are purged
        val deleted = ChatExportManager.confirmAndDeleteConversation(sampleConversation, userConfirmed = true)
        assertNotNull(deleted)
        assertTrue(deleted!!.isDeleted)
        assertTrue(deleted.messages.isEmpty())
    }
}
