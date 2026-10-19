package org.claudroide.app.feature.chat

/**
 * Action button state reflecting current interaction lifecycle.
 * Prevents double-submissions and seamlessly transforms Send into Stop during active streams.
 */
enum class InputActionButtonState {
    DISABLED,
    SEND,
    STOP
}

/**
 * Staged file attachment metadata with security verification status.
 */
data class AttachmentItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val isTextOrCode: Boolean = true,
    val providerConsentGiven: Boolean = false
)

/**
 * Enforces security and transparency rules for attachments.
 * Attachments cannot be sent to an external provider without explicit provider notice.
 */
object AttachmentPolicy {
    const val MAX_ATTACHMENT_SIZE_BYTES: Long = 10 * 1024 * 1024 // 10 MB

    fun canTransmitAttachment(item: AttachmentItem, targetProvider: String): Boolean {
        if (targetProvider.isBlank()) return false
        if (item.sizeBytes > MAX_ATTACHMENT_SIZE_BYTES) return false
        // Mandatory security rule: Attachments require explicit notice of target provider
        return item.providerConsentGiven
    }
}

/**
 * State representing chat input text, multiline expansion limits,
 * IME handling, and active streaming controls.
 */
data class ChatInputState(
    val text: String = "",
    val isStreaming: Boolean = false,
    val attachments: List<AttachmentItem> = emptyList(),
    val targetProviderName: String = "Anthropic Claude API",
    val maxCollapsedLines: Int = 6,
    val isDictationActive: Boolean = false
) {
    val hasContent: Boolean
        get() = text.isNotBlank() || attachments.isNotEmpty()

    val actionButtonState: InputActionButtonState
        get() = when {
            isStreaming -> InputActionButtonState.STOP
            hasContent -> {
                // If there are attachments, all must satisfy provider consent
                val allAttachmentsApproved = attachments.all {
                    AttachmentPolicy.canTransmitAttachment(it, targetProviderName)
                }
                if (allAttachmentsApproved) InputActionButtonState.SEND else InputActionButtonState.DISABLED
            }
            else -> InputActionButtonState.DISABLED
        }

    val pendingAttachmentConsentsCount: Int
        get() = attachments.count { !it.providerConsentGiven }
}
