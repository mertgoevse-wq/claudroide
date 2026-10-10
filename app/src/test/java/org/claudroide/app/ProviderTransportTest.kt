package org.claudroide.app

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.claudroide.app.feature.provider.ChatMessage
import org.claudroide.app.feature.provider.InMemorySecureKeyVault
import org.claudroide.app.feature.provider.ProviderAuth
import org.claudroide.app.feature.provider.ProviderConnectionException
import org.claudroide.app.feature.provider.ProviderDescriptor
import org.claudroide.app.feature.provider.ProviderTransport
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderTransportTest {

    @Test
    fun streamRequest_withoutKey_throwsProviderConnectionException() = runTest {
        val vault = InMemorySecureKeyVault()
        val transport = ProviderTransport(vault)
        val descriptor = ProviderDescriptor(
            providerId = "anthropic",
            displayName = "Anthropic",
            baseUrl = "https://api.anthropic.com/v1/messages",
            auth = ProviderAuth.API_KEY_HEADER,
            documentationUrl = "https://docs.anthropic.com"
        )

        var caught: Throwable? = null
        try {
            transport.streamRequest(
                descriptor = descriptor,
                modelId = "claude-sonnet-5-5",
                messages = listOf(ChatMessage("user", "Hello"))
            ).collect { }
        } catch (e: Throwable) {
            caught = e
        }

        assertNotNull(caught)
        assertTrue(caught is ProviderConnectionException)
        assertTrue(caught!!.message!!.contains("Kein Zugangsschlüssel"))
    }
}
