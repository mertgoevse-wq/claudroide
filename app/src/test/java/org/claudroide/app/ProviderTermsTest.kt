package org.claudroide.app

import org.claudroide.app.feature.provider.ProviderTermsEngine
import org.claudroide.app.feature.provider.ProviderTermsRecord
import org.claudroide.app.feature.provider.VerificationStatus
import org.junit.Assert.*
import org.junit.Test

class ProviderTermsTest {

    private val officialAnthropicRecord = ProviderTermsRecord(
        providerId = "anthropic",
        termsOfServiceUrl = "https://www.anthropic.com/legal/commercial-terms",
        privacyPolicyUrl = "https://www.anthropic.com/legal/privacy",
        lastVerifiedDate = "2026-10-01",
        verifiedEpochDays = 20727L, // e.g. Day 20727
        isOfficial = true,
        prohibitsSubscriptionScraping = true
    )

    @Test
    fun freshlyVerifiedOfficialProvider_isCurrent() {
        val currentDay = 20727L + 15L // 15 days later (< 90 days)
        val status = ProviderTermsEngine.evaluateVerificationStatus(officialAnthropicRecord, currentDay)
        assertEquals(VerificationStatus.VERIFIED_CURRENT, status)
        assertTrue(ProviderTermsEngine.canAdvertiseAsOfficial(officialAnthropicRecord))
    }

    @Test
    fun staleProviderPast90Days_requiresReverification() {
        val currentDay = 20727L + 95L // 95 days later (> 90 days)
        val status = ProviderTermsEngine.evaluateVerificationStatus(officialAnthropicRecord, currentDay)
        assertEquals(VerificationStatus.NEEDS_REVERIFICATION, status)
    }

    @Test
    fun communityProvider_isMarkedCommunityUnverified() {
        val communityRecord = ProviderTermsRecord(
            providerId = "custom_gw",
            termsOfServiceUrl = "https://mygw.org/terms",
            privacyPolicyUrl = "https://mygw.org/privacy",
            lastVerifiedDate = "2026-10-01",
            verifiedEpochDays = 20727L,
            isOfficial = false // Not an official provider
        )
        val status = ProviderTermsEngine.evaluateVerificationStatus(communityRecord, 20727L)
        assertEquals(VerificationStatus.COMMUNITY_UNVERIFIED, status)
        assertFalse(ProviderTermsEngine.canAdvertiseAsOfficial(communityRecord))
    }

    @Test
    fun subscriptionScraperOrBypass_isStrictlyDeprecated() {
        val badRecord = ProviderTermsRecord(
            providerId = "claude_cookie_scraper",
            termsOfServiceUrl = "https://unauthorized.org/terms",
            privacyPolicyUrl = "https://unauthorized.org/privacy",
            lastVerifiedDate = "2026-10-01",
            verifiedEpochDays = 20727L,
            prohibitsSubscriptionScraping = false // Fails security rule!
        )
        val status = ProviderTermsEngine.evaluateVerificationStatus(badRecord, 20727L)
        assertEquals(VerificationStatus.DEPRECATED, status)
        assertFalse(ProviderTermsEngine.canAdvertiseAsOfficial(badRecord))
    }
}
