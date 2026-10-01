package org.claudroide.app.feature.chat

import org.claudroide.app.core.design.ErrorSanitizer
import java.util.UUID

/**
 * Full conversation entity managed on device.
 */
data class ManagedConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val projectId: String? = null,
    val projectName: String? = null,
    val createdAtTimestampMs: Long = System.currentTimeMillis(),
    val lastModifiedTimestampMs: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val messages: List<String> = emptyList()
) {
    val isProjectBound: Boolean
        get() = !projectId.isNullOrBlank()

    fun toSummary(): ChatSummaryItem {
        return ChatSummaryItem(
            id = id,
            title = title,
            rawPreviewSnippet = messages.lastOrNull() ?: "",
            lastModifiedTimestampMs = lastModifiedTimestampMs,
            projectId = projectId,
            projectName = projectName,
            messageCount = messages.size,
            isArchived = isArchived
        )
    }
}

/**
 * Local conversation manager handling creation, renaming, and reversible archiving.
 * Operates strictly locally without any background network transmissions.
 */
object ConversationManager {
    const val DEFAULT_CHAT_TITLE = "Neue Unterhaltung"
    const val MAX_TITLE_LENGTH = 50

    /**
     * Creates a new conversation with sanitized auto-title or default title.
     */
    fun createConversation(
        initialPrompt: String? = null,
        projectId: String? = null,
        projectName: String? = null
    ): ManagedConversation {
        val title = if (!initialPrompt.isNullOrBlank()) {
            autoGenerateTitle(initialPrompt)
        } else {
            DEFAULT_CHAT_TITLE
        }

        val messages = if (!initialPrompt.isNullOrBlank()) listOf(initialPrompt) else emptyList()

        return ManagedConversation(
            title = title,
            projectId = projectId,
            projectName = projectName,
            messages = messages
        )
    }

    /**
     * Safely derives a concise title from the user's first prompt,
     * scrubbing any API keys, tokens, or credentials.
     */
    fun autoGenerateTitle(prompt: String): String {
        val sanitized = ErrorSanitizer.sanitizeErrorMessage(prompt.trim())
        // Take first line or up to 50 characters
        val firstLine = sanitized.lineSequence().firstOrNull { it.isNotBlank() } ?: DEFAULT_CHAT_TITLE
        return if (firstLine.length > MAX_TITLE_LENGTH) {
            firstLine.take(MAX_TITLE_LENGTH - 1).trim() + "…"
        } else {
            firstLine
        }
    }

    /**
     * Renames a conversation, guarding against blank titles.
     */
    fun renameConversation(conversation: ManagedConversation, newTitle: String): ManagedConversation {
        val cleanTitle = newTitle.trim()
        val finalTitle = if (cleanTitle.isBlank()) DEFAULT_CHAT_TITLE else cleanTitle.take(MAX_TITLE_LENGTH)
        return conversation.copy(
            title = finalTitle,
            lastModifiedTimestampMs = System.currentTimeMillis()
        )
    }

    /**
     * Reversibly archives a conversation.
     * Note: Archiving moves conversation out of active list, but does NOT delete it.
     */
    fun archiveConversation(conversation: ManagedConversation): ManagedConversation {
        return conversation.copy(
            isArchived = true,
            lastModifiedTimestampMs = System.currentTimeMillis()
        )
    }

    /**
     * Restores an archived conversation back to active list.
     */
    fun unarchiveConversation(conversation: ManagedConversation): ManagedConversation {
        return conversation.copy(
            isArchived = false,
            lastModifiedTimestampMs = System.currentTimeMillis()
        )
    }
}
