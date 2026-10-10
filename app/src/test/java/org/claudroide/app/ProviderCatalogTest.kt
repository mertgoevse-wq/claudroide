package org.claudroide.app

import org.claudroide.app.feature.provider.ApiProtocolFormat
import org.claudroide.app.feature.provider.ProviderAuthType
import org.claudroide.app.feature.provider.ProviderCatalogRegistry
import org.junit.Assert.*
import org.junit.Test

class ProviderCatalogTest {

    @Test
    fun defaultProviders_areRegistered() {
        val all = ProviderCatalogRegistry.getAllProviders()
        assertTrue(all.size >= 6)
        assertNotNull(ProviderCatalogRegistry.getProvider("anthropic"))
        assertNotNull(ProviderCatalogRegistry.getProvider("openrouter"))
        assertNotNull(ProviderCatalogRegistry.getProvider("openai"))
        assertNotNull(ProviderCatalogRegistry.getProvider("omniroute"))
        assertNotNull(ProviderCatalogRegistry.getProvider("local_server"))
        assertNotNull(ProviderCatalogRegistry.getProvider("custom_endpoint"))
    }

    @Test
    fun openaiProvider_conformsToSpecification() {
        val openai = requireNotNull(ProviderCatalogRegistry.getProvider("openai"))
        assertTrue(openai.isOfficiallyDocumented)
        assertEquals(ApiProtocolFormat.OPENAI_COMPATIBLE, openai.protocolFormat)
        assertTrue(openai.allowedAuthTypes.contains(ProviderAuthType.BEARER_TOKEN))
        assertEquals("https://api.openai.com/v1/chat/completions", openai.defaultEndpoint)
    }

    @Test
    fun omnirouteProvider_conformsToSpecification() {
        val omni = requireNotNull(ProviderCatalogRegistry.getProvider("omniroute"))
        assertEquals(ApiProtocolFormat.OPENAI_COMPATIBLE, omni.protocolFormat)
        assertTrue(omni.defaultEndpoint.contains("20128"))
    }

    @Test
    fun anthropicProvider_conformsToOfficialSpecification() {
        val anthropic = ProviderCatalogRegistry.getProvider("anthropic")
        assertNotNull(anthropic)
        assertTrue(anthropic!!.isOfficiallyDocumented)
        assertEquals(ApiProtocolFormat.ANTHROPIC_MESSAGES, anthropic.protocolFormat)
        assertTrue(anthropic.allowedAuthTypes.contains(ProviderAuthType.API_KEY_HEADER))
        assertTrue(anthropic.supportsStreaming)
        assertTrue(anthropic.supportsTools)
        assertEquals("https://api.anthropic.com/v1/messages", anthropic.defaultEndpoint)
    }

    @Test
    fun localServer_supportsZeroAuthentication() {
        val local = ProviderCatalogRegistry.getProvider("local_server")
        assertNotNull(local)
        assertTrue(local!!.allowedAuthTypes.contains(ProviderAuthType.NO_AUTH_LOCAL))
        assertEquals(ApiProtocolFormat.OPENAI_COMPATIBLE, local.protocolFormat)
        assertTrue(local.defaultEndpoint.contains("localhost"))
    }

    @Test
    fun providerToggling_enablesAndDisablesCorrectly() {
        assertTrue(ProviderCatalogRegistry.getProvider("openrouter")!!.isEnabled)

        // Disable openrouter
        val toggled = ProviderCatalogRegistry.setProviderEnabled("openrouter", false)
        assertTrue(toggled)
        assertFalse(ProviderCatalogRegistry.getProvider("openrouter")!!.isEnabled)

        // Re-enable
        ProviderCatalogRegistry.setProviderEnabled("openrouter", true)
        assertTrue(ProviderCatalogRegistry.getProvider("openrouter")!!.isEnabled)
    }

    @Test
    fun catalogEntries_containZeroHardcodedSecrets() {
        for (provider in ProviderCatalogRegistry.getAllProviders()) {
            assertFalse(provider.defaultEndpoint.contains("sk-"))
            assertFalse(provider.defaultEndpoint.contains("key="))
            assertFalse(provider.displayName.contains("Bearer"))
        }
    }
}
