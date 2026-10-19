package org.claudroide.app.feature.provider

enum class VerificationStatus {
    VERIFIED_CURRENT,
    NEEDS_REVERIFICATION,
    COMMUNITY_UNVERIFIED,
    DEPRECATED
}

/**
 * Compliance record capturing official terms, privacy policies,
 * audit dates, and official support claims.
 */
data class ProviderTermsRecord(
    val providerId: String,
    val termsOfServiceUrl: String,
    val privacyPolicyUrl: String,
    val lastVerifiedDate: String, // YYYY-MM-DD
    val verifiedEpochDays: Long,
    val verifiedBy: String = "ClauDroide Core Governance",
    val isOfficial: Boolean = true,
    val prohibitsSubscriptionScraping: Boolean = true
)

/**
 * Engine auditing provider staleness and enforcing honesty in official claims.
 */
object ProviderTermsEngine {
    // Audit staleness window: provider terms must be re-verified at least every 90 days
    const val MAX_VERIFICATION_AGE_DAYS: Long = 90L

    fun evaluateVerificationStatus(
        record: ProviderTermsRecord,
        currentEpochDays: Long
    ): VerificationStatus {
        if (!record.prohibitsSubscriptionScraping) {
            // Immediately deprecate any integration that attempts web scraping or subscription bypass
            return VerificationStatus.DEPRECATED
        }

        if (!record.isOfficial) {
            return VerificationStatus.COMMUNITY_UNVERIFIED
        }

        val ageDays = currentEpochDays - record.verifiedEpochDays
        return if (ageDays <= MAX_VERIFICATION_AGE_DAYS) {
            VerificationStatus.VERIFIED_CURRENT
        } else {
            VerificationStatus.NEEDS_REVERIFICATION
        }
    }

    /**
     * Prevents custom or unverified connection templates from being falsely advertised
     * as "Official" or "Certified".
     */
    fun canAdvertiseAsOfficial(record: ProviderTermsRecord): Boolean {
        return record.isOfficial &&
                record.prohibitsSubscriptionScraping &&
                record.termsOfServiceUrl.startsWith("https://")
    }
}
