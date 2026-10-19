package org.claudroide.app

import org.claudroide.app.feature.provider.SecretMasker
import org.junit.Assert.*
import org.junit.Test

class SecretMaskerTest {

    @Test
    fun secretMasker_scrubsAnthropicAndOpenAiSyntheticKeys() {
        val rawLog = "Connecting to Anthropic using sk-ant-synthetic-key-99887766 and proxy sk-abcdef1234567890abcdef12"
        val scrubbed = SecretMasker.redact(rawLog)

        assertFalse(scrubbed.contains("sk-ant-synthetic"))
        assertFalse(scrubbed.contains("sk-abcdef1234"))
        assertTrue(scrubbed.contains("[REDACTED]"))
    }

    @Test
    fun secretMasker_scrubsBearerHeadersAndCredentials() {
        val rawHeader = "HTTP/1.1 Header Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.synthetic"
        val scrubbed = SecretMasker.redact(rawHeader)

        assertFalse(scrubbed.contains("eyJhbGciOi"))
        assertTrue(scrubbed.contains("[REDACTED]"))
    }

    @Test
    fun secretMasker_scrubsPrivateKeyCertificates() {
        val certificateBlock = """
            Config loaded:
            -----BEGIN RSA PRIVATE KEY-----
            MIIEowIBAAKCAQEA0syntheticKeyDataHereForTestingPurposeOnly123456
            -----END RSA PRIVATE KEY-----
            Done.
        """.trimIndent()

        val scrubbed = SecretMasker.redact(certificateBlock)
        assertFalse(scrubbed.contains("MIIEowIBAAKCAQEA0synthetic"))
        assertTrue(scrubbed.contains("[REDACTED]"))
    }

    @Test
    fun secretMasker_auditsDetailedPatternDetections() {
        val text = "Set apiKey='sk-ant-testkey123' and password='supersecretpass'"
        val report = SecretMasker.auditAndRedact(text)

        assertTrue(report.scrubbedMatchesCount >= 2)
        assertFalse(report.sanitizedOutput.contains("supersecretpass"))
        assertFalse(report.sanitizedOutput.contains("sk-ant-testkey123"))
    }

    @Test
    fun secretMasker_neverClaimsCompleteSecurityGuarantee() {
        // Invariant: Masking is defense-in-depth, not a replacement for access controls
        assertFalse(SecretMasker.isCompleteSecurityGuarantee())
    }
}
