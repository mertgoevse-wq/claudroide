package org.claudroide.app

import org.claudroide.app.feature.provider.CostEstimate
import org.claudroide.app.feature.provider.CostEstimator
import org.claudroide.app.feature.provider.MissingReason
import org.claudroide.app.feature.provider.ModelPrice
import org.claudroide.app.feature.provider.PriceFreshness
import org.claudroide.app.feature.provider.TokenForecast
import org.claudroide.app.feature.provider.UsageForecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Task 064 — "Kostenschätzung".
 *
 * Acceptance criteria under test:
 *  - The price source is current and linkable (source URL + verification date).
 *  - No exact invoice is ever claimed, because no provider invoice is available here.
 *  - A missing or stale price is stated, never replaced by a guess.
 *  - The estimator works offline and never performs a request.
 */
class CostEstimatorTest {

    /** Date on which the seed prices were last verified. */
    private val verifiedDay: Long = LocalDate.parse(CostEstimator.VERIFIED).toEpochDay()

    private fun today(): Long = verifiedDay

    // ── Criterion: price source is current and linkable ───────────────────────

    @Test
    fun everySeedPrice_carriesASourceUrlAndAVerificationDate() {
        val problems = CostEstimator.validateTable()

        assertTrue("seed table must be clean, but: $problems", problems.isEmpty())
        CostEstimator.PRICE_TABLE.forEach { (id, price) ->
            assertTrue("$id needs an https source", price.sourceUrl.startsWith("https://"))
            assertFalse("$id needs a date", price.verifiedDate.isBlank())
            assertTrue("$id date must be ISO", price.hasUsableVerifiedDate)
            assertTrue("$id must be usable", price.isUsable)
        }
    }

    @Test
    fun seedTable_containsTheFourDocumentedAnthropicRates() {
        fun pair(id: String): Pair<String, String> {
            val p = CostEstimator.priceFor(id)!!
            return p.inputUsdPerMillionTokens.toPlainString() to
                p.outputUsdPerMillionTokens.toPlainString()
        }

        assertEquals("4.00" to "20.00", pair("claude-opus-5-5"))
        assertEquals("2.00" to "10.00", pair("claude-sonnet-5-5"))
        assertEquals("1.00" to "5.00", pair("claude-haiku-4-5"))
        assertEquals("10.00" to "50.00", pair("claude-fable-5-1"))
    }

    @Test
    fun estimate_surfacesTheSourceUrlAndDateInTheShownText() {
        val result = CostEstimator.estimate(
            modelId = "claude-opus-5-5",
            forecast = TokenForecast.measured(1000, 500),
            todayEpochDays = today()
        )
        val text = result.toDisplayText()

        assertTrue("source must be linkable in the UI", text.contains("https://platform.claude.com"))
        assertTrue("date must be visible", text.contains(CostEstimator.VERIFIED))
        assertEquals(
            CostEstimator.VERIFIED,
            result.verifiedDate
        )
        assertEquals(PriceFreshness.CURRENT, result.freshness)
    }

    @Test
    fun aPriceOlderThanTheRecheckWindow_isMarkedStale_butStillLabelled() {
        val stale = CostEstimator.estimate(
            modelId = "claude-opus-5-5",
            forecast = TokenForecast.measured(1000, 500),
            todayEpochDays = today() + CostEstimator.MAX_PRICE_AGE_DAYS + 1
        )

        assertEquals(PriceFreshness.STALE, stale.freshness)
        val text = stale.toDisplayText()
        assertTrue("must say the source needs a re-check", text.contains("veraltet"))
        assertTrue("still an estimate", text.contains("Schätzung"))
        assertNotNull("a stale price is still a known price", stale.amountUsd)
    }

    @Test
    fun aPriceAtTheEdgeOfTheWindow_isStillCurrent() {
        val result = CostEstimator.estimate(
            modelId = "claude-opus-5-5",
            forecast = TokenForecast.measured(10, 10),
            todayEpochDays = today() + CostEstimator.MAX_PRICE_AGE_DAYS
        )

        assertEquals(PriceFreshness.CURRENT, result.freshness)
    }

    @Test
    fun priceWithoutSource_isRefused_notGuessed() {
        val prices = mapOf(
            "mystery-model" to ModelPrice(
                modelId = "mystery-model",
                inputUsdPerMillionTokens = BigDecimal("1.00"),
                outputUsdPerMillionTokens = BigDecimal("2.00"),
                sourceUrl = "Preis steht im Prospekt",
                verifiedDate = CostEstimator.VERIFIED
            )
        )
        val problems = CostEstimator.validateTable(prices)
        assertTrue("validator must catch it", problems.isNotEmpty())

        val result = CostEstimator.estimate(
            modelId = "mystery-model",
            forecast = TokenForecast.measured(1000, 1000),
            todayEpochDays = today(),
            prices = prices
        )

        assertTrue(result is CostEstimate.Incomplete)
        assertNull("no number may be produced", result.amountUsd)
        assertTrue(result.missing.contains(MissingReason.PRICE_WITHOUT_SOURCE))
        assertEquals(PriceFreshness.UNVERIFIED, result.freshness)
        assertTrue(result.toDisplayText().contains("keine belastbare Angabe"))
    }

    @Test
    fun priceWithBrokenDate_isRefused_notGuessed() {
        val prices = mapOf(
            "mystery-model" to ModelPrice(
                modelId = "mystery-model",
                inputUsdPerMillionTokens = BigDecimal("1.00"),
                outputUsdPerMillionTokens = BigDecimal("2.00"),
                sourceUrl = "https://example.org/preise",
                verifiedDate = "letzten Monat"
            )
        )

        val result = CostEstimator.estimate(
            modelId = "mystery-model",
            forecast = TokenForecast.measured(1000, 1000),
            todayEpochDays = today(),
            prices = prices
        )

        assertTrue(result is CostEstimate.Incomplete)
        assertNull(result.amountUsd)
        assertTrue(result.missing.contains(MissingReason.PRICE_WITHOUT_DATE))
    }

    @Test
    fun validateTable_reportsBothMissingSourceAndMissingDate() {
        val prices = mapOf(
            "bad" to ModelPrice(
                modelId = "bad",
                inputUsdPerMillionTokens = BigDecimal("1"),
                outputUsdPerMillionTokens = BigDecimal("1"),
                sourceUrl = "",
                verifiedDate = ""
            )
        )

        val problems = CostEstimator.validateTable(prices)

        assertEquals(2, problems.size)
    }

    // ── Criterion: no exact invoice is ever claimed ───────────────────────────

    @Test
    fun everyResult_isLabelledEstimateAndDisclaimsTheInvoice() {
        val forecasts = listOf(
            TokenForecast.measured(1000, 1000),
            TokenForecast.unknown(),
            UsageForecast.fromCharacterCounts(400, 800)
        )
        forecasts.forEach { forecast ->
            listOf("claude-opus-5-5", "no-such-model").forEach { model ->
                val text = CostEstimator
                    .estimate(model, forecast, today())
                    .toDisplayText()

                assertTrue(
                    "must be labelled Schätzung for $model",
                    text.contains("Schätzung")
                )
                assertTrue(
                    "must state that the provider reports the real amount",
                    text.contains("Den tatsächlichen Betrag meldet erst die Abrechnung")
                )
                assertFalse(
                    "must not promise an exact invoice",
                    text.contains("Rechnung beträgt") || text.contains("exakt")
                )
            }
        }
    }

    @Test
    fun displayText_statesNoNumberIsGuessed_whenIncomplete() {
        val result = CostEstimator.estimate("claude-opus-5-5", TokenForecast.unknown(), today())

        assertTrue(result.toDisplayText().contains("Es wird keine Zahl geraten."))
    }

    // ── Unknown models are never priced by resemblance ────────────────────────

    @Test
    fun unknownModel_isReportedAsUnknown_notAsFree() {
        val result = CostEstimator.estimate(
            "claude-opus-5-5-turbo",
            TokenForecast.measured(1000, 1000),
            today()
        )

        assertTrue(result is CostEstimate.Incomplete)
        assertNull(result.amountUsd)
        assertEquals(listOf(MissingReason.UNKNOWN_MODEL), result.missing)
    }

    @Test
    fun blankModelId_isReportedNotPriced() {
        val result = CostEstimator.estimate("   ", TokenForecast.measured(1000, 1000), today())

        assertTrue(result is CostEstimate.Incomplete)
        assertTrue(result.missing.contains(MissingReason.UNKNOWN_MODEL))
    }

    // ── A partial estimate is a lower bound, never a total ─────────────────────

    @Test
    fun unknownOutputTokens_giveALowerBoundNotATotal() {
        val result = CostEstimator.estimate(
            modelId = "claude-opus-5-5",
            forecast = TokenForecast(1_000_000, null, emptyList()),
            todayEpochDays = today()
        )

        assertTrue(result is CostEstimate.PartialEstimate)
        // 4 USD, same scale convention as unknownInputTokens_giveALowerBoundFromOutputOnly
        // (10) and fullEstimate_addsInputAndOutputExactly (4 / 20 / 24): trailing zeros
        // are trimmed, so the value is compared as 4 rather than 4.00.
        assertEquals("1 input token per million at 4 USD", BigDecimal("4"), result.amountUsd)
        assertTrue(result.missing.contains(MissingReason.OUTPUT_TOKENS_UNKNOWN))
        assertTrue(result.headline.startsWith("Schätzung unvollständig: mindestens"))
        assertTrue(result.toDisplayText().contains("mindestens"))
    }

    @Test
    fun unknownInputTokens_giveALowerBoundFromOutputOnly() {
        val result = CostEstimator.estimate(
            modelId = "claude-sonnet-5-5",
            forecast = TokenForecast(null, 1_000_000, emptyList()),
            todayEpochDays = today()
        )

        assertTrue(result is CostEstimate.PartialEstimate)
        assertEquals(BigDecimal("10"), result.amountUsd)
        assertTrue(result.missing.contains(MissingReason.INPUT_TOKENS_UNKNOWN))
    }

    @Test
    fun nothingKnownAtAll_yieldsNoAmount() {
        val result = CostEstimator.estimate(
            "claude-opus-5-5",
            TokenForecast.unknown(),
            today()
        )

        assertTrue(result is CostEstimate.Incomplete)
        assertNull(result.amountUsd)
        assertTrue(result.missing.contains(MissingReason.INPUT_TOKENS_UNKNOWN))
        assertTrue(result.missing.contains(MissingReason.OUTPUT_TOKENS_UNKNOWN))
    }

    // ── Exact arithmetic, no float artefacts ──────────────────────────────────

    @Test
    fun costOf_isExactForTinyAmounts() {
        // The classic float trap: 1 token at 4 USD per million is 0.000004.
        val cost = CostEstimator.costOf(1, BigDecimal("4.00"))

        assertEquals("0.000004", cost.toPlainString())
        assertFalse(cost.toPlainString().contains("E"))
        assertFalse(cost.toPlainString().contains("9999"))
    }

    @Test
    fun costOf_sumsWithoutRoundingArtefacts() {
        // Three single-token requests: 0.000004 * 3 must not become 0.00001200000x.
        val oneToken = BigDecimal("1")
        val sum = (1..3).fold(BigDecimal.ZERO) { acc, _ ->
            acc.add(CostEstimator.costOf(1, oneToken))
        }

        assertEquals("0.000003", sum.toPlainString())
    }

    @Test
    fun costOf_isMonotonicAndZeroForZeroTokens() {
        assertEquals(BigDecimal.ZERO.stripTrailingZeros(), CostEstimator.costOf(0, BigDecimal("20")))
        assertTrue(
            CostEstimator.costOf(1000, BigDecimal("20")) >
                CostEstimator.costOf(999, BigDecimal("20"))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun costOf_rejectsNegativeTokens() {
        CostEstimator.costOf(-1, BigDecimal("1.00"))
    }

    @Test
    fun format_usesGermanCommaAndSaneDecimals() {
        assertEquals("0,000004 USD", CostEstimator.formatUsd(BigDecimal("0.000004")))
        assertEquals("0,0120 USD", CostEstimator.formatUsd(BigDecimal("0.012")))
        assertEquals("4,00 USD", CostEstimator.formatUsd(BigDecimal("4")))
        assertEquals("12,35 USD", CostEstimator.formatUsd(BigDecimal("12.345")))
    }

    @Test
    fun fullEstimate_addsInputAndOutputExactly() {
        val result = CostEstimator.estimate(
            "claude-opus-5-5",
            TokenForecast.measured(1_000_000, 1_000_000),
            today()
        ) as CostEstimate.Estimate

        assertEquals(BigDecimal("24"), result.amountUsd)
        assertEquals(BigDecimal("4"), result.inputPart.usd)
        assertEquals(BigDecimal("20"), result.outputPart.usd)
        assertEquals("Geschätzte Kosten: 24,00 USD", result.headline)
    }

    // ── The forecast states its assumptions ───────────────────────────────────

    @Test
    fun characterHeuristic_isReportedAsAnAssumption() {
        val forecast = UsageForecast.fromCharacterCounts(400, 800)

        assertEquals(100, forecast.inputTokens)
        assertEquals(200, forecast.outputTokens)
        assertTrue(forecast.assumptions.isNotEmpty())
        assertTrue(forecast.assumptions.first().contains("Annahme"))
        assertTrue(
            "the assumption must reach the user",
            CostEstimator.estimate("claude-sonnet-5-5", forecast, today())
                .toDisplayText()
                .contains("Annahme")
        )
    }

    @Test
    fun characterHeuristic_neverInventsTokensForNegativeInput() {
        assertNull(UsageForecast.fromCharacterCounts(-1, 100).inputTokens)
    }

    @Test
    fun measuredForecastIsNotDressedUpAsAMeasurement() {
        // Measured counts are an assumption-free input; the *result* is still only
        // an estimate because the provider bills the real usage.
        val forecast = TokenForecast.measured(1000, 1000)

        assertEquals(1000, forecast.inputTokens)
        assertEquals(1000, forecast.outputTokens)
        assertTrue(CostEstimator.estimate("claude-haiku-4-5", forecast, today())
            .toDisplayText().contains("Schätzung, keine Rechnung"))
    }

    // ── Offline by construction ───────────────────────────────────────────────

    @Test
    fun estimate_isDeterministicAndRepeatable() {
        val forecast = UsageForecast.fromCharacterCounts(1234, 5678)

        val a = CostEstimator.estimate("claude-sonnet-5-5", forecast, today()).amountUsd
        val b = CostEstimator.estimate("claude-sonnet-5-5", forecast, today()).amountUsd

        assertEquals(a, b)
    }

    @Test
    fun anUnparseableTodayDate_isNotSilentlyTreatedAsFresh() {
        val result = CostEstimator.estimate(
            "claude-opus-5-5",
            TokenForecast.measured(1000, 1000),
            todayIsoDate = "irgendwann"
        )

        assertEquals(PriceFreshness.UNVERIFIED, result.freshness)
        assertTrue(result.toDisplayText().contains("keine belastbare Angabe"))
    }

    @Test
    fun convenienceOverloadMatchesTheEpochDayOverload() {
        val forecast = TokenForecast.measured(1000, 1000)

        assertEquals(
            CostEstimator.estimate("claude-haiku-4-5", forecast, today()).amountUsd,
            CostEstimator.estimate("claude-haiku-4-5", forecast, CostEstimator.VERIFIED).amountUsd
        )
    }

    @Test
    fun ageIsReportedInDays() {
        val price = CostEstimator.priceFor("claude-opus-5-5")!!

        assertEquals(0L, CostEstimator.ageInDays(price, today()))
        assertEquals(45L, CostEstimator.ageInDays(price, today() + 45))
        assertTrue(
            CostEstimator.estimate("claude-opus-5-5", TokenForecast.measured(10, 10), today() + 45)
                .toDisplayText().contains("45 Tage")
        )
    }
}
