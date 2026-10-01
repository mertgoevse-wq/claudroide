package org.claudroide.app

import org.claudroide.app.feature.provider.*
import org.junit.Assert.*
import org.junit.Test

class ProviderErrorHelpTest {

    // ── HTTP status classification ────────────────────────────────────────────

    @Test
    fun classifyHttpStatus_401_isAuthRejected() {
        assertEquals(ProviderErrorKind.AUTH_REJECTED, ProviderErrorHelpMapper.classifyHttpStatus(401))
    }

    @Test
    fun classifyHttpStatus_403_isAuthRejected() {
        assertEquals(ProviderErrorKind.AUTH_REJECTED, ProviderErrorHelpMapper.classifyHttpStatus(403))
    }

    @Test
    fun classifyHttpStatus_429_isRateLimited() {
        assertEquals(ProviderErrorKind.RATE_LIMITED, ProviderErrorHelpMapper.classifyHttpStatus(429))
    }

    @Test
    fun classifyHttpStatus_404_isModelNotFound() {
        assertEquals(ProviderErrorKind.MODEL_NOT_FOUND, ProviderErrorHelpMapper.classifyHttpStatus(404))
    }

    @Test
    fun classifyHttpStatus_400_isBadRequest() {
        assertEquals(ProviderErrorKind.BAD_REQUEST, ProviderErrorHelpMapper.classifyHttpStatus(400))
    }

    @Test
    fun classifyHttpStatus_500_isServerError() {
        assertEquals(ProviderErrorKind.SERVER_ERROR, ProviderErrorHelpMapper.classifyHttpStatus(500))
    }

    @Test
    fun classifyHttpStatus_503_isServerError() {
        assertEquals(ProviderErrorKind.SERVER_ERROR, ProviderErrorHelpMapper.classifyHttpStatus(503))
    }

    @Test
    fun classifyHttpStatus_200_isUnknown() {
        assertEquals(ProviderErrorKind.UNKNOWN, ProviderErrorHelpMapper.classifyHttpStatus(200))
    }

    // ── Help content invariants ───────────────────────────────────────────────

    @Test
    fun buildHelp_authRejected_notBillable() {
        val help = ProviderErrorHelpMapper.buildHelp(ProviderErrorKind.AUTH_REJECTED)
        assertFalse(help.mayHaveBeenBilled)
        assertTrue(help.suggestedActions.isNotEmpty())
        assertTrue(help.headline.isNotBlank())
    }

    @Test
    fun buildHelp_serverError_isBillable() {
        val help = ProviderErrorHelpMapper.buildHelp(ProviderErrorKind.SERVER_ERROR)
        assertTrue("5xx errors may have been billed", help.mayHaveBeenBilled)
    }

    @Test
    fun buildHelp_networkUnreachable_notBillable() {
        val help = ProviderErrorHelpMapper.buildHelp(ProviderErrorKind.NETWORK_UNREACHABLE)
        assertFalse(help.mayHaveBeenBilled)
    }

    @Test
    fun buildHelp_tlsError_notBillable() {
        val help = ProviderErrorHelpMapper.buildHelp(ProviderErrorKind.TLS_ERROR)
        assertFalse(help.mayHaveBeenBilled)
    }

    @Test
    fun buildHelp_unsafeRedirect_notBillable() {
        val help = ProviderErrorHelpMapper.buildHelp(ProviderErrorKind.UNSAFE_REDIRECT)
        assertFalse(help.mayHaveBeenBilled)
    }

    @Test
    fun buildHelp_everyKind_hasNonEmptySuggestedActions() {
        for (kind in ProviderErrorKind.entries) {
            val help = ProviderErrorHelpMapper.buildHelp(kind)
            assertTrue(
                "Kind $kind should have at least one suggested action",
                help.suggestedActions.isNotEmpty()
            )
        }
    }

    @Test
    fun buildHelp_technicalDetail_isNullByDefault() {
        val help = ProviderErrorHelpMapper.buildHelp(ProviderErrorKind.AUTH_REJECTED)
        assertNull(help.technicalDetail)
    }

    // ── Secret scrubbing in technical detail ──────────────────────────────────

    @Test
    fun buildHelp_scrubsAnthropicKeyFromDetail() {
        val detail = "request failed: x-api-key: sk-ant-api03-secretkeyvalue1234"
        val help = ProviderErrorHelpMapper.buildHelp(
            ProviderErrorKind.AUTH_REJECTED, rawDetail = detail
        )
        assertFalse(
            "Raw Anthropic key must not appear in technical detail",
            help.technicalDetail?.contains("secretkeyvalue1234") ?: false
        )
        assertTrue(help.technicalDetail?.contains("••••••••") ?: false)
    }

    @Test
    fun buildHelp_scrubsBearerTokenFromDetail() {
        val detail = "Authorization: Bearer sk-or-v1-mysecrettoken12345"
        val help = ProviderErrorHelpMapper.buildHelp(
            ProviderErrorKind.AUTH_REJECTED, rawDetail = detail
        )
        assertFalse(help.technicalDetail?.contains("mysecrettoken12345") ?: false)
    }

    @Test
    fun buildHelp_preservesNonSecretDetailText() {
        val detail = "HTTP 429 Too Many Requests"
        val help = ProviderErrorHelpMapper.buildHelp(
            ProviderErrorKind.RATE_LIMITED, rawDetail = detail
        )
        assertTrue(help.technicalDetail?.contains("429") ?: false)
        assertTrue(help.technicalDetail?.contains("Too Many Requests") ?: false)
    }

    // ── Provider name appears in explanation ──────────────────────────────────

    @Test
    fun buildHelp_providerNameAppearsInExplanation() {
        val help = ProviderErrorHelpMapper.buildHelp(
            ProviderErrorKind.AUTH_REJECTED,
            providerName = "Mein Testanbieter"
        )
        assertTrue(help.explanation.contains("Mein Testanbieter"))
    }

    // ── All kinds return a matching kind field ────────────────────────────────

    @Test
    fun buildHelp_returnedKindMatchesInput() {
        for (kind in ProviderErrorKind.entries) {
            val help = ProviderErrorHelpMapper.buildHelp(kind)
            assertEquals("Returned kind must match input for $kind", kind, help.kind)
        }
    }
}
