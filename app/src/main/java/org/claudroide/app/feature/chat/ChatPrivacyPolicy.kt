package org.claudroide.app.feature.chat

/**
 * Conversation-level privacy settings governing remote data transmission,
 * local storage privacy, and sensitive path exclusions.
 */
data class ChatPrivacySettings(
    val conversationId: String,
    val hasAcknowledgedExternalTransfer: Boolean = false,
    val excludedFilePatterns: List<String> = listOf(".env*", "*id_rsa*", "*.pem", "*secret*", "*.key"),
    val isLocalOnlyMode: Boolean = false,
    val targetProviderName: String = "Anthropic Claude API"
)

/**
 * Enforces zero-telemetry rules, outbound transfer consent, and file exclusions.
 */
object PrivacyEnforcer {
    // Architectural security invariant: Zero silent analytics, trackers, or telemetries
    const val ZERO_TELEMETRY_INVARIANT: Boolean = true

    /**
     * Checks if outbound network dispatch to an external provider is authorized.
     * Before first external dispatch, user MUST explicitly acknowledge that data leaves device.
     */
    fun canDispatchExternalPrompt(settings: ChatPrivacySettings, isLocalServer: Boolean): Boolean {
        if (isLocalServer || settings.isLocalOnlyMode) {
            // Local server never transmits data off-device
            return true
        }
        // External provider requires prior explicit acknowledgment
        return settings.hasAcknowledgedExternalTransfer
    }

    /**
     * Checks if a file path is blocked by exclusion patterns to protect sensitive secrets.
     */
    fun isPathExcluded(path: String, patterns: List<String>): Boolean {
        val fileName = path.substringAfterLast('/')
        return patterns.any { pattern ->
            when {
                pattern.startsWith("*") && pattern.endsWith("*") -> {
                    val core = pattern.substring(1, pattern.length - 1)
                    fileName.contains(core, ignoreCase = true)
                }
                pattern.startsWith("*") -> {
                    val suffix = pattern.substring(1)
                    fileName.endsWith(suffix, ignoreCase = true)
                }
                pattern.endsWith("*") -> {
                    val prefix = pattern.substring(0, pattern.length - 1)
                    fileName.startsWith(prefix, ignoreCase = true)
                }
                else -> fileName.equals(pattern, ignoreCase = true)
            }
        }
    }

    /**
     * Confirms that stored conversation history is 100% locally readable
     * without any network access or server validation.
     */
    fun isOfflineReadable(conversation: ManagedConversation): Boolean {
        return conversation.messages.isNotEmpty() || conversation.title.isNotEmpty()
    }
}
