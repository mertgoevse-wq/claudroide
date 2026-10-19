package org.claudroide.app

import org.claudroide.app.feature.provider.*
import org.junit.Assert.*
import org.junit.Test

class NetworkSecurityPolicyTest {

    // ── canTransmitCredentials ────────────────────────────────────────────────

    @Test
    fun canTransmitCredentials_allowsHttps() {
        assertNull(NetworkSecurityPolicy.canTransmitCredentials("https://api.anthropic.com/v1/messages"))
    }

    @Test
    fun canTransmitCredentials_allowsHttpToLocalhost() {
        assertNull(NetworkSecurityPolicy.canTransmitCredentials("http://localhost:11434/v1"))
    }

    @Test
    fun canTransmitCredentials_allowsHttpTo127() {
        assertNull(NetworkSecurityPolicy.canTransmitCredentials("http://127.0.0.1:8080/api"))
    }

    @Test
    fun canTransmitCredentials_allowsHttpToEmulatorAlias() {
        assertNull(NetworkSecurityPolicy.canTransmitCredentials("http://10.0.2.2:11434/api"))
    }

    @Test
    fun canTransmitCredentials_blocksHttpToExternalHost() {
        val reason = NetworkSecurityPolicy.canTransmitCredentials("http://api.example.com/v1")
        assertNotNull(reason)
        assertTrue(reason!!.contains("http"))
    }

    @Test
    fun canTransmitCredentials_blocksEmptyUrl() {
        val reason = NetworkSecurityPolicy.canTransmitCredentials("   ")
        assertNotNull(reason)
    }

    @Test
    fun canTransmitCredentials_blocksNonHttpScheme() {
        val reason = NetworkSecurityPolicy.canTransmitCredentials("ftp://files.example.com")
        assertNotNull(reason)
    }

    @Test
    fun canTransmitCredentials_blocksMalformedUrl() {
        val reason = NetworkSecurityPolicy.canTransmitCredentials("not a url !!!")
        assertNotNull(reason)
    }

    // ── evaluateRedirect ──────────────────────────────────────────────────────

    @Test
    fun evaluateRedirect_safe_sameHostHttps() {
        val verdict = NetworkSecurityPolicy.evaluateRedirect(
            originalUrl = "https://api.anthropic.com/v1",
            redirectUrl = "https://api.anthropic.com/v2"
        )
        assertTrue(verdict is RedirectVerdict.Safe)
    }

    @Test
    fun evaluateRedirect_blocked_hostChange() {
        val verdict = NetworkSecurityPolicy.evaluateRedirect(
            originalUrl = "https://api.anthropic.com/v1",
            redirectUrl = "https://evil.example.com/steal"
        )
        assertTrue(verdict is RedirectVerdict.Blocked)
        val blocked = verdict as RedirectVerdict.Blocked
        assertTrue(blocked.reason.contains("evil.example.com"))
    }

    @Test
    fun evaluateRedirect_blocked_httpsToHttp_externalHost() {
        val verdict = NetworkSecurityPolicy.evaluateRedirect(
            originalUrl = "https://api.openrouter.ai/v1",
            redirectUrl = "http://api.openrouter.ai/v1"
        )
        assertTrue(verdict is RedirectVerdict.Blocked)
        val blocked = verdict as RedirectVerdict.Blocked
        assertTrue(blocked.reason.contains("Downgrade") || blocked.reason.contains("verschlüsselt"))
    }

    @Test
    fun evaluateRedirect_safe_localhostHttpToHttp() {
        val verdict = NetworkSecurityPolicy.evaluateRedirect(
            originalUrl = "http://localhost:11434/old",
            redirectUrl = "http://localhost:11434/new"
        )
        assertTrue(verdict is RedirectVerdict.Safe)
    }

    @Test
    fun evaluateRedirect_blocked_emptyRedirectUrl() {
        val verdict = NetworkSecurityPolicy.evaluateRedirect(
            originalUrl = "https://api.anthropic.com/v1",
            redirectUrl = "   "
        )
        assertTrue(verdict is RedirectVerdict.Blocked)
    }

    @Test
    fun evaluateRedirect_blocked_malformedRedirectUrl() {
        val verdict = NetworkSecurityPolicy.evaluateRedirect(
            originalUrl = "https://api.anthropic.com/v1",
            redirectUrl = "not a url"
        )
        assertTrue(verdict is RedirectVerdict.Blocked)
    }

    // ── describeTlsFailure ────────────────────────────────────────────────────

    @Test
    fun describeTlsFailure_expired_mentionsExpiry() {
        val msg = NetworkSecurityPolicy.describeTlsFailure(TlsFailureKind.CERTIFICATE_EXPIRED, "api.example.com")
        assertTrue(msg.contains("abgelaufen") || msg.contains("Zertifikat"))
        assertTrue(msg.contains("api.example.com"))
    }

    @Test
    fun describeTlsFailure_untrusted_mentionsCA() {
        val msg = NetworkSecurityPolicy.describeTlsFailure(TlsFailureKind.UNTRUSTED_CERTIFICATE, "api.example.com")
        assertTrue(msg.contains("vertrauenswürdig") || msg.contains("CA") || msg.contains("Zertifikat"))
    }

    @Test
    fun describeTlsFailure_mismatch_mentionsHostname() {
        val msg = NetworkSecurityPolicy.describeTlsFailure(TlsFailureKind.HOSTNAME_MISMATCH, "api.example.com")
        assertTrue(msg.contains("Hostname") || msg.contains("übereinstimm"))
    }

    @Test
    fun describeTlsFailure_neverContainsApiKey() {
        for (kind in TlsFailureKind.entries) {
            val msg = NetworkSecurityPolicy.describeTlsFailure(kind, "host.test")
            assertFalse("TLS description must not mention api key patterns", msg.contains("sk-ant"))
            assertFalse(msg.contains("Bearer"))
        }
    }

    @Test
    fun describeTlsFailure_everyKind_isNonBlank() {
        for (kind in TlsFailureKind.entries) {
            val msg = NetworkSecurityPolicy.describeTlsFailure(kind, "host.test")
            assertTrue("Description for $kind must not be blank", msg.isNotBlank())
        }
    }

    // ── isLoopbackHost ────────────────────────────────────────────────────────

    @Test
    fun isLoopbackHost_localhost_isTrue() {
        assertTrue(NetworkSecurityPolicy.isLoopbackHost("localhost"))
    }

    @Test
    fun isLoopbackHost_127_isTrue() {
        assertTrue(NetworkSecurityPolicy.isLoopbackHost("127.0.0.1"))
    }

    @Test
    fun isLoopbackHost_ipv6Loopback_isTrue() {
        assertTrue(NetworkSecurityPolicy.isLoopbackHost("::1"))
    }

    @Test
    fun isLoopbackHost_emulatorAlias_isTrue() {
        assertTrue(NetworkSecurityPolicy.isLoopbackHost("10.0.2.2"))
    }

    @Test
    fun isLoopbackHost_externalHost_isFalse() {
        assertFalse(NetworkSecurityPolicy.isLoopbackHost("api.anthropic.com"))
    }

    @Test
    fun isLoopbackHost_privateIp_isFalse() {
        assertFalse(NetworkSecurityPolicy.isLoopbackHost("192.168.1.1"))
    }
}
