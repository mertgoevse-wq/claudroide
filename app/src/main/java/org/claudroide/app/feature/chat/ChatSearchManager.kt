package org.claudroide.app.feature.chat

enum class MatchField {
    TITLE,
    MESSAGE_CONTENT,
    PROJECT_NAME
}

/**
 * Filter criteria for local conversation search.
 */
data class ChatSearchFilter(
    val query: String = "",
    val projectId: String? = null,
    val startDateMs: Long? = null,
    val endDateMs: Long? = null,
    val maxResults: Int = 100
)

/**
 * Search hit descriptor containing matching field and contextual snippet.
 */
data class SearchResultMatch(
    val chatId: String,
    val chatTitle: String,
    val matchedField: MatchField,
    val snippet: String,
    val timestampMs: Long,
    val projectId: String? = null,
    val projectName: String? = null
) {
    val isProjectBound: Boolean
        get() = !projectId.isNullOrBlank()
}

/**
 * Data model for searchable conversation content in local memory/cache.
 */
data class SearchableConversation(
    val id: String,
    val title: String,
    val messages: List<String> = emptyList(),
    val timestampMs: Long,
    val projectId: String? = null,
    val projectName: String? = null,
    val isDeleted: Boolean = false
)

/**
 * 100% On-device, local search engine.
 * Guarantees zero network calls or telemetry leakage during indexing or query execution.
 */
object ChatSearchEngine {
    const val IS_STRICTLY_LOCAL: Boolean = true

    fun search(
        filter: ChatSearchFilter,
        conversations: List<SearchableConversation>
    ): List<SearchResultMatch> {
        val q = filter.query.trim().lowercase()

        return conversations.asSequence()
            // Invariant: Deleted conversations must never appear in search results
            .filter { !it.isDeleted }
            // Filter by project if specified
            .filter { filter.projectId.isNullOrBlank() || it.projectId == filter.projectId }
            // Filter by date range
            .filter { filter.startDateMs == null || it.timestampMs >= filter.startDateMs }
            .filter { filter.endDateMs == null || it.timestampMs <= filter.endDateMs }
            // Match against query if present
            .mapNotNull { conv ->
                when {
                    q.isEmpty() -> {
                        SearchResultMatch(
                            chatId = conv.id,
                            chatTitle = conv.title,
                            matchedField = MatchField.TITLE,
                            snippet = conv.messages.firstOrNull()?.take(80) ?: conv.title,
                            timestampMs = conv.timestampMs,
                            projectId = conv.projectId,
                            projectName = conv.projectName
                        )
                    }
                    conv.title.lowercase().contains(q) -> {
                        SearchResultMatch(
                            chatId = conv.id,
                            chatTitle = conv.title,
                            matchedField = MatchField.TITLE,
                            snippet = conv.title,
                            timestampMs = conv.timestampMs,
                            projectId = conv.projectId,
                            projectName = conv.projectName
                        )
                    }
                    conv.projectName?.lowercase()?.contains(q) == true -> {
                        SearchResultMatch(
                            chatId = conv.id,
                            chatTitle = conv.title,
                            matchedField = MatchField.PROJECT_NAME,
                            snippet = "Projekt: ${conv.projectName}",
                            timestampMs = conv.timestampMs,
                            projectId = conv.projectId,
                            projectName = conv.projectName
                        )
                    }
                    else -> {
                        val matchingMsg = conv.messages.firstOrNull { it.lowercase().contains(q) }
                        if (matchingMsg != null) {
                            val snippet = extractSnippet(matchingMsg, q)
                            SearchResultMatch(
                                chatId = conv.id,
                                chatTitle = conv.title,
                                matchedField = MatchField.MESSAGE_CONTENT,
                                snippet = snippet,
                                timestampMs = conv.timestampMs,
                                projectId = conv.projectId,
                                projectName = conv.projectName
                            )
                        } else null
                    }
                }
            }
            .take(filter.maxResults)
            .toList()
    }

    private fun extractSnippet(text: String, query: String): String {
        val idx = text.lowercase().indexOf(query)
        if (idx == -1) return text.take(80)
        val start = maxOf(0, idx - 30)
        val end = minOf(text.length, idx + query.length + 50)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        return prefix + text.substring(start, end).replace("\n", " ").trim() + suffix
    }
}
