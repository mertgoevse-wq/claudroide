package org.claudroide.app

import org.claudroide.app.feature.provider.LimitSource
import org.claudroide.app.feature.provider.LocalUsageCounter
import org.claudroide.app.feature.provider.UsageLimit
import org.claudroide.app.feature.provider.UsageLimitKind
import org.claudroide.app.feature.provider.UsageLimitPolicy
import org.claudroide.app.feature.provider.UsageWarningLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 065 — "Nutzungsgrenzen".
 *
 * Acceptance criteria under test:
 *  - Limits and warnings are honest about their origin.
 *  - The app never promises a request will succeed.
 *
 * The load-bearing rule of this task: the app has no access to a provider's account
 * dashboard, so it may never present a remaining quota as a fact. Every test below
 * exists to pin that down, or the advisory-not-blocking guarantee.
 */
class UsageLimitTest {

    private val today = 20675L // 2026-10-01 in epoch days

    private fun userLimit(
        value: Int = 100,
        kind: UsageLimitKind = UsageLimitKind.REQUESTS_PER_DAY,
        projectId: String? = null
    ) = UsageLimit(
        providerId = "anthropic",
        kind = kind,
        limitValue = value,
        source = LimitSource.USER_ENTERED,
        projectId = projectId
    )

    private fun documentedLimit(value: Int = 100) = UsageLimit(
        providerId = "anthropic",
        kind = UsageLimitKind.REQUESTS_PER_DAY,
        limitValue = value,
        source = LimitSource.VENDOR_DOCUMENTED,
        sourceUrl = "https://example.org/limits",
        verifiedOn = "2026-10-01"
    )

    // ── The app never claims to know a provider's real quota ─────────────────

    @Test
    fun withoutALimit_nothingIsAsserted() {
        val status = UsageLimitPolicy.evaluate(limit = null, used = 500, currentEpochDays = today)

        assertEquals(UsageWarningLevel.UNKNOWN_LIMIT, status.level)
        assertEquals("must not invent a number", 0, status.usedValue)
        assertTrue("must say unknown", status.headline.contains(UsageLimitPolicy.UNKNOWN_LIMIT_TEXT))
    }

    @Test
    fun aUserEnteredValue_isLabelledAsTheUsersOwnInput() {
        val status = UsageLimitPolicy.evaluate(userLimit(), used = 10, currentEpochDays = today)

        assertTrue(
            "the user must be able to see where the number came from: ${status.footnotes}",
            status.footnotes.any { it == UsageLimitPolicy.USER_ENTERED_FOOTNOTE }
        )
    }

    @Test
    fun aDocumentedLimitWithoutSourceUrl_isNotUsable() {
        val broken = userLimit().copy(
            source = LimitSource.VENDOR_DOCUMENTED,
            sourceUrl = null,
            verifiedOn = "2026-10-01"
        )

        val status = UsageLimitPolicy.evaluate(broken, used = 10, currentEpochDays = today)

        assertEquals(UsageWarningLevel.UNKNOWN_LIMIT, status.level)
    }

    @Test
    fun aDocumentedLimitWithoutDate_isNotUsable() {
        val broken = documentedLimit().copy(verifiedOn = null)

        val status = UsageLimitPolicy.evaluate(broken, used = 10, currentEpochDays = today)

        assertEquals(UsageWarningLevel.UNKNOWN_LIMIT, status.level)
    }

    @Test
    fun aDocumentedLimitWithAMalformedDate_isNotUsable() {
        val broken = documentedLimit().copy(verifiedOn = "01.10.2026")

        val status = UsageLimitPolicy.evaluate(broken, used = 10, currentEpochDays = today)

        assertEquals(UsageWarningLevel.UNKNOWN_LIMIT, status.level)
    }

    @Test
    fun aStaleDocumentedLimit_stopsBeingPresentedAsCurrent() {
        val stale = documentedLimit()
        val farFuture = today + UsageLimitPolicy.MAX_VENDOR_SOURCE_AGE_DAYS + 1

        assertFalse(
            "a source older than the window must not count as current",
            UsageLimitPolicy.isSourceStillFresh(stale, farFuture)
        )
        assertEquals(
            UsageWarningLevel.UNKNOWN_LIMIT,
            UsageLimitPolicy.evaluate(stale, used = 10, currentEpochDays = farFuture).level
        )
    }

    @Test
    fun aNonPositiveLimitIsTreatedAsNoLimit() {
        val zero = userLimit(value = 0)

        assertEquals(
            UsageWarningLevel.UNKNOWN_LIMIT,
            UsageLimitPolicy.evaluate(zero, used = 5, currentEpochDays = today).level
        )
    }

    @Test
    fun isoDateValidation_isStrict() {
        assertTrue(UsageLimitPolicy.isIsoDate("2026-10-01"))
        assertTrue(UsageLimitPolicy.isIsoDate("2024-02-29"))
        assertFalse("a real day does not exist", UsageLimitPolicy.isIsoDate("2026-02-30"))
        assertFalse(UsageLimitPolicy.isIsoDate("2026-13-01"))
        assertFalse(UsageLimitPolicy.isIsoDate("2026-10"))
        assertFalse(UsageLimitPolicy.isIsoDate(null))
        assertFalse(UsageLimitPolicy.isIsoDate(""))
    }

    // ── Criterion: warnings, never silent blocking ───────────────────────────

    @Test
    fun aWarningNeverBlocksTheRequest() {
        listOf(0, 79, 80, 99, 100, 5000).forEach { used ->
            val status = UsageLimitPolicy.evaluate(userLimit(100), used = used, currentEpochDays = today)
            assertTrue(
                "used=$used must still be allowed; the app cannot know the provider's quota",
                status.requestStillAllowed
            )
        }
    }

    @Test
    fun everyUsableStatusCarriesTheAdvisoryFootnote() {
        listOf(0, 80, 100).forEach { used ->
            val status = UsageLimitPolicy.evaluate(userLimit(100), used = used, currentEpochDays = today)
            assertTrue(
                "used=$used must state that nothing is blocked",
                status.footnotes.contains(UsageLimitPolicy.ADVISORY_FOOTNOTE)
            )
        }
    }

    @Test
    fun noStatusEverPromisesThatTheProviderWillAcceptARequest() {
        val statuses = listOf(0, 80, 100).map {
            UsageLimitPolicy.evaluate(userLimit(100), used = it, currentEpochDays = today)
        } + UsageLimitPolicy.evaluate(null, used = 1, currentEpochDays = today)

        statuses.forEach { status ->
            val text = status.headline + status.footnotes.joinToString(" ")
            assertFalse(
                "must not promise success: $text",
                text.contains("garantiert", ignoreCase = true) ||
                    text.contains("wird angenommen", ignoreCase = true) ||
                    text.contains("sicher", ignoreCase = true)
            )
        }
    }

    // ── Warning levels follow the user's own threshold ───────────────────────

    @Test
    fun levelsFollowTheUsersOwnThreshold() {
        assertEquals(
            UsageWarningLevel.BELOW_LIMIT,
            UsageLimitPolicy.evaluate(userLimit(100), used = 79, currentEpochDays = today).level
        )
        assertEquals(
            UsageWarningLevel.NEAR_LIMIT,
            UsageLimitPolicy.evaluate(userLimit(100), used = 80, currentEpochDays = today).level
        )
        assertEquals(
            UsageWarningLevel.LIMIT_REACHED,
            UsageLimitPolicy.evaluate(userLimit(100), used = 100, currentEpochDays = today).level
        )
        assertEquals(
            UsageWarningLevel.LIMIT_REACHED,
            UsageLimitPolicy.evaluate(userLimit(100), used = 250, currentEpochDays = today).level
        )
    }

    @Test
    fun aNegativeCountIsTreatedAsZeroRatherThanCrashing() {
        val status = UsageLimitPolicy.evaluate(userLimit(), used = -5, currentEpochDays = today)

        assertEquals(0, status.usedValue)
        assertEquals(UsageWarningLevel.BELOW_LIMIT, status.level)
    }

    // ── The counter only counts what the user already sent ───────────────────

    @Test
    fun theCounter_onlyRecordsWhatTheUserAlreadyTriggered() {
        val counter = LocalUsageCounter("anthropic")
            .record(UsageLimitKind.REQUESTS_PER_DAY)
            .record(UsageLimitKind.REQUESTS_PER_DAY, 4)

        assertEquals(5, counter.used(UsageLimitKind.REQUESTS_PER_DAY))
        assertEquals("a kind never touched stays zero", 0, counter.used(UsageLimitKind.TOKENS_PER_DAY))
    }

    @Test
    fun theCounter_ignoresNegativeAmounts() {
        val counter = LocalUsageCounter("anthropic").record(UsageLimitKind.COST_PER_MONTH, -100)

        assertEquals(0, counter.used(UsageLimitKind.COST_PER_MONTH))
    }

    @Test
    fun aLimitOfAnotherProvider_isNotEvaluated() {
        val statuses = UsageLimitPolicy.evaluateAll(
            limits = listOf(userLimit().copy(providerId = "openrouter")),
            providerId = "anthropic",
            projectId = null,
            counter = LocalUsageCounter("anthropic").record(UsageLimitKind.REQUESTS_PER_DAY, 5),
            currentEpochDays = today
        )

        assertTrue("another provider's limit must not apply", statuses.isEmpty())
    }

    @Test
    fun withoutACounter_nothingIsAssumed() {
        val status = UsageLimitPolicy.evaluateAll(
            limits = listOf(userLimit(100)),
            providerId = "anthropic",
            projectId = null,
            counter = null,
            currentEpochDays = today
        ).single()

        assertEquals("no counter means no claimed usage", 0, status.usedValue)
    }

    // ── Project scope ───────────────────────────────────────────────────────

    @Test
    fun aProjectLimit_appliesToThatProjectOnly() {
        val limits = listOf(userLimit(100, projectId = "projekt-a"))

        assertEquals(
            1,
            UsageLimitPolicy.selectApplicable(limits, "anthropic", "projekt-a").size
        )
        assertTrue(
            UsageLimitPolicy.selectApplicable(limits, "anthropic", "projekt-b").isEmpty()
        )
    }

    @Test
    fun aGlobalLimit_appliesToEveryProject() {
        val limits = listOf(userLimit(100, projectId = null))

        assertEquals(1, UsageLimitPolicy.selectApplicable(limits, "anthropic", "projekt-b").size)
    }

    // ── German output, no slop ───────────────────────────────────────────────

    @Test
    fun theExplanationWithoutALimit_isGermanAndSaysNothingIsKnown() {
        val text = UsageLimitPolicy.explanationWithoutLimit("anthropic")

        assertTrue("must not be blank", text.isNotBlank())
        assertTrue("must state unknown: $text", text.contains(UsageLimitPolicy.UNKNOWN_LIMIT_TEXT))
    }

    @Test
    fun statusText_isReadablyGerman() {
        val status = UsageLimitPolicy.evaluate(userLimit(100), used = 85, currentEpochDays = today)

        assertTrue(
            "headline must be a sentence: ${status.headline}",
            status.headline.length > 15 && status.headline.contains(' ')
        )
        assertFalse("must not be empty", status.footnotes.isEmpty())
    }
}
