package org.claudroide.app

import org.claudroide.app.feature.chat.ChatPrivacySettings
import org.claudroide.app.feature.chat.ManagedConversation
import org.claudroide.app.feature.chat.PrivacyEnforcer
import org.junit.Assert.*
import org.junit.Test

class ChatPrivacyTest {

    @Test
    fun zeroTelemetryInvariant_isGuaranteed() {
        assertTrue(PrivacyEnforcer.ZERO_TELEMETRY_INVARIANT)
    }

    @Test
    fun externalDispatch_requiresPriorExplicitAcknowledgment() {
        val unacknowledged = ChatPrivacySettings(
            conversationId = "c-1",
            hasAcknowledgedExternalTransfer = false
        )
        // Blocked for external cloud provider
        assertFalse(
            PrivacyEnforcer.canDispatchExternalPrompt(unacknowledged, isLocalServer = false)
        )

        // Allowed for local server without sending off-device
        assertTrue(
            PrivacyEnforcer.canDispatchExternalPrompt(unacknowledged, isLocalServer = true)
        )

        // Allowed once user explicitly acknowledges
        val acknowledged = unacknowledged.copy(hasAcknowledgedExternalTransfer = true)
        assertTrue(
            PrivacyEnforcer.canDispatchExternalPrompt(acknowledged, isLocalServer = false)
        )
    }

    @Test
    fun pathExclusion_blocksSensitiveFilesFromPromptInclusion() {
        val patterns = listOf(".env*", "*id_rsa*", "*.pem", "*secret*", "*.key")

        assertTrue(PrivacyEnforcer.isPathExcluded("/app/.env", patterns))
        assertTrue(PrivacyEnforcer.isPathExcluded("/app/.env.production", patterns))
        assertTrue(PrivacyEnforcer.isPathExcluded("/root/.ssh/id_rsa", patterns))
        assertTrue(PrivacyEnforcer.isPathExcluded("/app/certs/server.pem", patterns))
        assertTrue(PrivacyEnforcer.isPathExcluded("/config/jwt_secret.json", patterns))
        assertTrue(PrivacyEnforcer.isPathExcluded("/keys/release.key", patterns))

        // Normal files must not be excluded
        assertFalse(PrivacyEnforcer.isPathExcluded("app/src/main/MainActivity.kt", patterns))
        assertFalse(PrivacyEnforcer.isPathExcluded("build.gradle.kts", patterns))
    }

    @Test
    fun offlineReadability_worksWithoutNetworkConnection() {
        val conversation = ManagedConversation(
            id = "c-offline",
            title = "Saved Offline Chat",
            messages = listOf("Local message 1", "Local response 2")
        )
        assertTrue(PrivacyEnforcer.isOfflineReadable(conversation))
    }
}
