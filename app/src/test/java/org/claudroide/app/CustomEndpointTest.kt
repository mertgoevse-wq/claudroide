package org.claudroide.app

import org.claudroide.app.feature.provider.CustomEndpointGuard
import org.junit.Assert.*
import org.junit.Test

class CustomEndpointTest {

    @Test
    fun verifyEndpointSecurity_allowsValidHttpsEndpoint() {
        val verdict = CustomEndpointGuard.verifyEndpointSecurity("https://llm.mycompany.org/v1/chat/completions")
        assertTrue(verdict.isSecure)
        assertFalse(verdict.isLocalhostExemption)
        assertNull(verdict.errorDescription)
    }

    @Test
    fun verifyEndpointSecurity_rejectsUnencryptedRemoteHttp() {
        val verdict = CustomEndpointGuard.verifyEndpointSecurity("http://llm.mycompany.org/v1/chat/completions")
        assertFalse(verdict.isSecure)
        assertTrue(verdict.errorDescription!!.contains("TLS-Verschlüsselung"))
    }

    @Test
    fun verifyEndpointSecurity_allowsHttpForLocalhostOnly() {
        val verdict = CustomEndpointGuard.verifyEndpointSecurity("http://localhost:11434/v1/chat/completions")
        assertTrue(verdict.isSecure)
        assertTrue(verdict.isLocalhostExemption)

        val verdictIp = CustomEndpointGuard.verifyEndpointSecurity("http://127.0.0.1:8080/v1/messages")
        assertTrue(verdictIp.isSecure)
        assertTrue(verdictIp.isLocalhostExemption)
    }

    @Test
    fun verifyEndpointSecurity_blocksPrivateLanSsrfAttempts() {
        val verdict192 = CustomEndpointGuard.verifyEndpointSecurity("http://192.168.1.100/admin")
        assertFalse(verdict192.isSecure)

        val verdict10 = CustomEndpointGuard.verifyEndpointSecurity("https://10.0.0.5:8443/api")
        assertFalse(verdict10.isSecure)
        assertTrue(verdict10.errorDescription!!.contains("SSRF"))
    }

    @Test
    fun canSendKeyToHost_strictlyPinsKeysToConfiguredHost() {
        val configured = "https://gateway.example.com/v1/chat/completions"

        // Same host, different path: allowed
        assertTrue(CustomEndpointGuard.canSendKeyToHost(configured, "https://gateway.example.com/v1/models"))

        // Different host or proxy: strictly blocked
        assertFalse(CustomEndpointGuard.canSendKeyToHost(configured, "https://evil-gateway.org/v1/chat/completions"))
        assertFalse(CustomEndpointGuard.canSendKeyToHost(configured, "https://sub.gateway.example.com/v1/chat/completions"))
    }
}
