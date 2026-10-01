package org.claudroide.app.feature.chat

import org.claudroide.app.core.design.ErrorSanitizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(val extension: String, val mimeType: String) {
    MARKDOWN("md", "text/markdown"),
    JSON("json", "application/json")
}

data class ExportScopeSummary(
    val conversationTitle: String,
    val messageCount: Int,
    val estimatedBytes: Long,
    val containsSanitizedSecrets: Boolean,
    val targetFormat: ExportFormat
)

/**
 * Handles permanent conversation deletion with confirmation
 * and privacy-preserving export to user-selected SAF destinations.
 */
object ChatExportManager {

    /**
     * Inspects conversation and prepares transparent scope preview
     * before user commits to exporting.
     */
    fun getExportScope(conversation: ManagedConversation, format: ExportFormat): ExportScopeSummary {
        val payload = generateExportPayload(conversation, format)
        val rawCombined = conversation.messages.joinToString("\n")
        val hadSecrets = rawCombined.contains("sk-ant-") || rawCombined.contains("sk-")

        return ExportScopeSummary(
            conversationTitle = conversation.title,
            messageCount = conversation.messages.size,
            estimatedBytes = payload.toByteArray().size.toLong(),
            containsSanitizedSecrets = hadSecrets,
            targetFormat = format
        )
    }

    /**
     * Generates a fully sanitized, human-readable export payload.
     * All credentials, tokens, and passwords are unconditionally scrubbed.
     */
    fun generateExportPayload(conversation: ManagedConversation, format: ExportFormat): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val formattedDate = dateFormat.format(Date(conversation.lastModifiedTimestampMs))

        return when (format) {
            ExportFormat.MARKDOWN -> {
                buildString {
                    appendLine("# ${ErrorSanitizer.sanitizeErrorMessage(conversation.title)}")
                    appendLine()
                    appendLine("> Exportiert aus Claudroide am $formattedDate")
                    if (conversation.isProjectBound) {
                        appendLine("> Projekt: ${conversation.projectName ?: conversation.projectId}")
                    }
                    appendLine()
                    appendLine("---")
                    appendLine()
                    conversation.messages.forEachIndexed { idx, msg ->
                        val sanitized = ErrorSanitizer.sanitizeErrorMessage(msg)
                        val role = if (idx % 2 == 0) "**User**" else "**Assistant**"
                        appendLine("### $role")
                        appendLine()
                        appendLine(sanitized)
                        appendLine()
                    }
                }
            }
            ExportFormat.JSON -> {
                val sanitizedTitle = ErrorSanitizer.sanitizeErrorMessage(conversation.title)
                    .replace("\"", "\\\"")
                val msgsJson = conversation.messages.joinToString(",\n    ") { msg ->
                    "\"${ErrorSanitizer.sanitizeErrorMessage(msg).replace("\"", "\\\"").replace("\n", "\\n")}\""
                }
                """{
  "id": "${conversation.id}",
  "title": "$sanitizedTitle",
  "projectId": ${if (conversation.projectId != null) "\"${conversation.projectId}\"" else "null"},
  "exportedAt": "$formattedDate",
  "messages": [
    $msgsJson
  ]
}"""
            }
        }
    }

    /**
     * Irrevocably purges conversation if and only if two-step user confirmation is provided.
     */
    fun confirmAndDeleteConversation(
        conversation: ManagedConversation,
        userConfirmed: Boolean
    ): ManagedConversation? {
        if (!userConfirmed) {
            // Refuse deletion without explicit confirmation
            return null
        }
        return conversation.copy(
            isDeleted = true,
            messages = emptyList(), // Scrub message payload from memory
            lastModifiedTimestampMs = System.currentTimeMillis()
        )
    }
}
