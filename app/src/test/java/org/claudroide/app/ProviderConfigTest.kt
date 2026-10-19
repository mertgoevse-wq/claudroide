package org.claudroide.app

import org.claudroide.app.feature.provider.ApiProtocolFormat
import org.claudroide.app.feature.provider.CustomProviderDraft
import org.claudroide.app.feature.provider.ProviderConfigValidator
import org.junit.Assert.*
import org.junit.Test

class ProviderConfigTest {

    @Test
    fun validRemoteDraft_passesValidation() {
        val draft = CustomProviderDraft(
            displayName = "Custom AI Gateway",
            endpointUrl = "https://gateway.example.com/v1/chat/completions",
            protocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE,
            rawApiKey = "sk-1234567890abcdef12345678",
            defaultModelId = "mistral-large"
        )
        val result = ProviderConfigValidator.validateDraft(draft)
        assertTrue(result.isValid)
        assertTrue(result.fieldErrors.isEmpty())
    }

    @Test
    fun unencryptedHttpForRemoteServer_isRejected() {
        val draft = CustomProviderDraft(
            displayName = "Insecure Gateway",
            endpointUrl = "http://remote-api.com/v1/chat/completions",
            protocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE,
            rawApiKey = "sk-1234567890abcdef",
            defaultModelId = "gpt-4o"
        )
        val result = ProviderConfigValidator.validateDraft(draft)
        assertFalse(result.isValid)
        assertTrue(result.fieldErrors.containsKey("endpointUrl"))
        assertTrue(result.fieldErrors["endpointUrl"]!!.contains("https://"))
    }

    @Test
    fun httpLocalhost_isPermitted() {
        val localDraft = CustomProviderDraft(
            displayName = "Local Ollama",
            endpointUrl = "http://localhost:11434/v1/chat/completions",
            protocolFormat = ApiProtocolFormat.OPENAI_COMPATIBLE,
            rawApiKey = "", // No key needed for local
            defaultModelId = "llama3.2:latest"
        )
        val result = ProviderConfigValidator.validateDraft(localDraft)
        assertTrue(result.isValid)
    }

    @Test
    fun missingModelOrShortName_failsValidation() {
        val invalidDraft = CustomProviderDraft(
            displayName = "A", // too short
            endpointUrl = "https://api.openai.com/v1/chat/completions",
            rawApiKey = "sk-validkey12345678",
            defaultModelId = "" // empty
        )
        val result = ProviderConfigValidator.validateDraft(invalidDraft)
        assertFalse(result.isValid)
        assertTrue(result.fieldErrors.containsKey("displayName"))
        assertTrue(result.fieldErrors.containsKey("defaultModelId"))
    }

    @Test
    fun maskApiKey_safelyObscuresRawKey() {
        val raw = "sk-ant-api03-abcdef1234567890_test"
        val masked = ProviderConfigValidator.maskApiKey(raw)

        assertFalse(masked.contains("abcdef1234567890"))
        assertTrue(masked.startsWith("sk-a"))
        assertTrue(masked.endsWith("test"))
        assertTrue(masked.contains("••••••••"))
    }

    @Test
    fun reviewSummary_previewsConfigBeforeSaving() {
        val draft = CustomProviderDraft(
            displayName = "Enterprise Claude",
            endpointUrl = "https://claude-proxy.corp.net/v1/messages",
            protocolFormat = ApiProtocolFormat.ANTHROPIC_MESSAGES,
            rawApiKey = "sk-ant-corp-key-1234567890",
            defaultModelId = "claude-sonnet-5-5"
        )
        val summary = ProviderConfigValidator.createReviewSummary(draft)

        assertEquals("Enterprise Claude", summary.displayName)
        assertEquals("https://claude-proxy.corp.net/v1/messages", summary.endpointUrl)
        assertEquals("Anthropic Messages API", summary.protocolName)
        assertEquals("claude-sonnet-5-5", summary.defaultModelId)
        assertFalse(summary.maskedApiKey.contains("corp-key"))
        assertFalse(summary.isLocalServer)
        assertTrue(summary.isReadyToSave)
    }
}
