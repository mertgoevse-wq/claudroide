package org.claudroide.app.feature.chat

import org.claudroide.app.core.design.ErrorSanitizer

/**
 * Summary descriptor of a conversation for list displays,
 * with optional project binding and safe preview snippets.
 */
data class ChatSummaryItem(
    val id: String,
    val title: String,
    val rawPreviewSnippet: String,
    val lastModifiedTimestampMs: Long,
    val projectId: String? = null,
    val projectName: String? = null,
    val messageCount: Int = 0,
    val isArchived: Boolean = false
) {
    val isProjectBound: Boolean
        get() = !projectId.isNullOrBlank()

    /**
     * Sanitized preview snippet guaranteed free of API keys or credentials.
     */
    val sanitizedPreview: String
        get() = ChatPreviewSanitizer.sanitizePreview(rawPreviewSnippet)
}

/**
 * Ensures chat list previews never display leaked credentials or raw tokens.
 */
object ChatPreviewSanitizer {
    fun sanitizePreview(snippet: String): String {
        return ErrorSanitizer.sanitizeErrorMessage(snippet).take(120)
    }
}

/**
 * UI State for conversation history list with project filtering
 * and empty-state detection.
 */
data class ChatListUiState(
    val chats: List<ChatSummaryItem> = emptyList(),
    val selectedProjectIdFilter: String? = null,
    val isLoading: Boolean = false
) {
    val isEmpty: Boolean
        get() = chats.isEmpty() && !isLoading

    val filteredChats: List<ChatSummaryItem>
        get() = if (selectedProjectIdFilter.isNullOrBlank()) {
            chats.filter { !it.isArchived }.sortedByDescending { it.lastModifiedTimestampMs }
        } else {
            chats.filter { !it.isArchived && it.projectId == selectedProjectIdFilter }
                .sortedByDescending { it.lastModifiedTimestampMs }
        }
}
