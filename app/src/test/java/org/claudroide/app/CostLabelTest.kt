package org.claudroide.app

import org.claudroide.app.feature.provider.CostEstimate
import org.claudroide.app.feature.provider.CostEstimator
import org.claudroide.app.feature.provider.SecretMasker
import org.claudroide.app.feature.provider.CostLabelPresenter
import org.claudroide.app.feature.provider.ModelOrigin
import org.claudroide.app.feature.provider.TokenForecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Task 041 — "Anbieter und Kosten anzeigen".
 *
 * Acceptance criteria under test:
 *  - Schätzwerte sind als solche gekennzeichnet (an estimate is always labelled
 *    as an estimate, never as an invoice).
 *  - Unbekannte Kosten werden nicht als kostenlos ausgegeben (unknown cost never
 *    renders as free, zero or empty).
 *  - "Schutz": no secret reaches the status line or diagnostics text.
 *  - Prices are never derived from the spelling of a model name.
 */
class CostLabelTest {

    private val today: Long = LocalDate.parse(CostEstimator.VERIFIED).toEpochDay()

    private fun measured(): TokenForecast = TokenForecast.measured(1_000, 500)

    private fun knownModel(): String = CostEstimator.PRICE_TABLE.keys.first()

    // ── Criterion: estimates are labelled as estimates ──────────────────────

    @Test
    fun costLabel_marksEveryAmountAsAnEstimateAndNotAsAnInvoice() {
        val estimate = CostEstimator.estimate(knownModel(), measured(), today)

        val label = CostLabelPresenter.costLabel(estimate)

        assertTrue("Betrag fehlt", label.hasAmount)
        assertTrue(
            "Überschrift muss 'Schätzung' nennen: ${label.headline}",
            label.headline.contains("Schätzung")
        )
        assertTrue(
            "Der Text muss 'Schätzung' nennen: ${label.lines}",
            label.lines.any { it.contains("Schätzung") }
        )
        assertTrue(
            "Es darf keine Rechnung behauptet werden: ${label.toDisplayText()}",
            label.lines.any { it.contains("kein Rechnungsbetrag") }
        )
        assertTrue(
            "Herkunft der echten Abrechnung muss genannt werden",
            label.toDisplayText().contains("Abrechnung des Anbieters")
        )
    }

    @Test
    fun costLabel_showsSourceUrlAndIsoDateNextToEveryAmount() {
        val estimate = CostEstimator.estimate(knownModel(), measured(), today)

        val label = CostLabelPresenter.costLabel(estimate)

        assertNotNull("Quelle fehlt", label.sourceUrl)
        assertNotNull("Prüfdatum fehlt", label.verifiedDate)
        assertTrue(
            "Quelle muss http(s) sein: ${label.sourceUrl}",
            label.sourceUrl!!.startsWith("https://") || label.sourceUrl!!.startsWith("http://")
        )
        assertTrue(
            "Prüfdatum muss ISO-8601 sein: ${label.verifiedDate}",
            CostLabelPresenter.isIsoDate(label.verifiedDate!!)
        )
        assertTrue(
            "Quelle muss sichtbar im Text stehen",
            label.toDisplayText().contains(label.sourceUrl!!)
        )
        assertTrue(
            "Prüfdatum muss sichtbar im Text stehen",
            label.toDisplayText().contains(label.verifiedDate!!)
        )
    }

    @Test
    fun partialEstimate_isMarkedAsALowerBound() {
        val estimate = CostEstimator.estimate(
            knownModel(),
            TokenForecast(1_000, null, emptyList()),
            today
        )
        assertTrue("Grundannahme falsch", estimate is CostEstimate.PartialEstimate)

        val label = CostLabelPresenter.costLabel(estimate)

        assertTrue(
            "Teilwert muss als Untergrenze benannt werden: ${label.lines}",
            label.lines.any { it.contains("Untergrenze") }
        )
        assertTrue(
            "Grund muss genannt werden",
            label.missing.contains(
                org.claudroide.app.feature.provider.MissingReason.OUTPUT_TOKENS_UNKNOWN
            )
        )
    }

    // ── Criterion: unknown cost is never shown as free ──────────────────────

    @Test
    fun unknownModel_isNotShownAsFree() {
        val estimate = CostEstimator.estimate("gibtesnicht-9", measured(), today)

        val label = CostLabelPresenter.costLabel(estimate)

        assertFalse("Unbekanntes Modell darf keinen Betrag zeigen", label.hasAmount)
        assertNull(label.amountText)
        val text = label.toDisplayText()
        assertTrue("Grund muss genannt werden: $text", text.contains("unbekannt") || text.contains("nicht im Preisverzeichnis"))
        assertFalse("Kostenlos darf nicht fallen: $text", text.contains("kostenlos"))
        assertFalse("Gratis darf nicht fallen: $text", text.contains("gratis"))
        assertFalse("Null USD darf nicht fallen: $text", text.contains("0,00 USD"))
    }

    @Test
    fun priceWithoutSource_showsNoAmountAtAll() {
        val prices = mapOf(
            "modell-ohne-quelle" to org.claudroide.app.feature.provider.ModelPrice(
                modelId = "modell-ohne-quelle",
                inputUsdPerMillionTokens = BigDecimal("1.00"),
                outputUsdPerMillionTokens = BigDecimal("2.00"),
                sourceUrl = "nicht-valid",
                verifiedDate = CostEstimator.VERIFIED
            )
        )

        val estimate = CostEstimator.estimate(
            "modell-ohne-quelle", measured(), today, prices
        )
        val label = CostLabelPresenter.costLabel(estimate)

        assertFalse("Ohne Quelle darf kein Betrag erscheinen", label.hasAmount)
        assertNull(label.amountText)
        assertNull(label.sourceUrl)
        assertTrue(
            "Fehlende Quelle muss erklärt werden: ${label.lines}",
            label.lines.any { it.contains("keine belegte Quelle") }
        )
    }

    @Test
    fun priceWithoutVerificationDate_showsNoAmountAtAll() {
        val prices = mapOf(
            "modell-ohne-datum" to org.claudroide.app.feature.provider.ModelPrice(
                modelId = "modell-ohne-datum",
                inputUsdPerMillionTokens = BigDecimal("1.00"),
                outputUsdPerMillionTokens = BigDecimal("2.00"),
                sourceUrl = "https://beispiel.invalid/preise",
                verifiedDate = "letzten Dienstag"
            )
        )

        val estimate = CostEstimator.estimate("modell-ohne-datum", measured(), today, prices)
        val label = CostLabelPresenter.costLabel(estimate)

        assertFalse("Ohne Prüfdatum darf kein Betrag erscheinen", label.hasAmount)
        assertNull(label.verifiedDate)
    }

    @Test
    fun noEstimateAtAll_neverReadsAsFree() {
        val text = CostLabelPresenter.statusText(null, null, null, null)

        assertFalse("Kostenlos darf nicht fallen: $text", text.contains("kostenlos"))
        assertFalse("Gratis darf nicht fallen: $text", text.contains("gratis"))
        assertTrue("Anbieter muss unbekannt sein: $text", text.contains(UNKNOWN))
        assertTrue("Modell muss unbekannt sein: $text", text.contains(UNKNOWN))
        assertFalse("Es darf kein USD-Betrag stehen: $text", text.contains("USD"))
    }

    // ── Criterion: no guessing from the model name ──────────────────────────

    @Test
    fun familiarModelNameIsNotEnoughToProduceAPrice() {
        // A name that looks like a real, expensive model but is not in the table.
        val estimate = CostEstimator.estimate("claude-super-turbo-9000", measured(), today)

        val label = CostLabelPresenter.costLabel(estimate)

        assertFalse("Aus dem Namen darf kein Preis werden", label.hasAmount)
        assertTrue(
            "Grund muss 'nicht im Preisverzeichnis' nennen: ${label.lines}",
            label.lines.any { it.contains("nicht im Preisverzeichnis") }
        )
    }

    @Test
    fun manualUserEntry_withoutPriceEntry_hasNoCost() {
        val estimate = CostEstimator.estimate("mein-eigenes-modell", measured(), today)
        val text = CostLabelPresenter.statusText(
            "Eigener Anbieter",
            "mein-eigenes-modell",
            ModelOrigin.MANUAL_USER_INPUT,
            estimate
        )

        assertFalse("Handmodell ohne Preiseintrag darf keinen Betrag zeigen", text.contains("Geschätzte Kosten"))
        assertTrue(
            "Herkunft der Handeingabe muss sichtbar sein: $text",
            text.contains(ModelOrigin.MANUAL_USER_INPUT.labelDe)
        )
    }

    // ── Criterion: no secret in status text or diagnostics ─────────────────

    @Test
    fun apiKeyInProviderNameIsRedacted() {
        val leaky = "Mein Anbieter sk-abcdef0123456789abcdef"

        val line = CostLabelPresenter.providerLine(leaky)

        assertFalse("Schlüssel darf nicht erscheinen: $line", line.contains("sk-abcdef0123456789abcdef"))
        assertTrue("Schwärzung muss sichtbar sein: $line", line.contains(SecretMasker.REDACTION_PLACEHOLDER))
    }

    @Test
    fun apiKeyInModelIdIsRedacted() {
        val line = CostLabelPresenter.modelLine("modell-token=sk-live-9999999999", null)

        assertFalse("Schlüssel darf nicht erscheinen: $line", line.contains("sk-live-9999999999"))
        assertFalse("Schlüssel darf nicht erscheinen: $line", line.contains("sk-live"))
    }

    @Test
    fun statusTextRedactsSecretsBeforeItCanBeLogged() {
        val text = CostLabelPresenter.statusText(
            "provider?api_key=supersecretvalue123",
            "gpt-4 style model",
            ModelOrigin.FETCHED_FROM_API,
            CostEstimator.estimate(knownModel(), measured(), today)
        )

        assertFalse("Query-Schlüssel darf nicht erscheinen", text.contains("supersecretvalue123"))
        assertFalse("Schlüssel darf nicht erscheinen", text.contains("api_key="))
        assertFalse("Ein langer Geheimnis-Blob darf nicht erscheinen", SecretMasker.containsSecretLikeText(text))
    }

    @Test
    fun secretLikePatternsAreDetected() {
        assertTrue("sk-Schlüssel", SecretMasker.containsSecretLikeText("sk-abcdefghijklmnop"))
        assertTrue("Bearer-Token", SecretMasker.containsSecretLikeText("Bearer abcdefghijklmnop"))
        assertTrue("Zuweisung", SecretMasker.containsSecretLikeText("api_key=abcdefghijklm"))
        assertFalse("harmloser Text", SecretMasker.containsSecretLikeText("Anbieter: Google"))
        assertFalse("normaler Text", SecretMasker.containsSecretLikeText("Geschätzte Kosten: 0,01 USD (Schätzung)"))
    }

    // ── Provider and model lines ───────────────────────────────────────────

    @Test
    fun providerAndModelLinesCarryNamesAndOrigin() {
        val text = CostLabelPresenter.statusText(
            "Testanbieter",
            "testmodell-1",
            ModelOrigin.OFFICIAL_PROVIDER_CATALOG,
            CostEstimator.estimate(knownModel(), measured(), today)
        )

        assertTrue("Anbietername fehlt: $text", text.contains("Anbieter: Testanbieter"))
        // The line must show the model the user actually has selected, not the one the
        // price table happens to contain — the estimate is only a price for that table entry.
        assertTrue("Modellname fehlt: $text", text.contains("Modell: testmodell-1"))
        assertTrue("Herkunft fehlt: $text", text.contains(ModelOrigin.OFFICIAL_PROVIDER_CATALOG.labelDe))
    }

    @Test
    fun blankProviderNameBecomesUnknown() {
        assertTrue(
            "Leerer Anbieter muss unbekannt heißen",
            CostLabelPresenter.providerLine("   ").contains(UNKNOWN)
        )
        assertTrue(
            "Null-Anbieter muss unbekannt heißen",
            CostLabelPresenter.providerLine(null).contains(UNKNOWN)
        )
        assertTrue(
            "Null-Modell muss unbekannt heißen",
            CostLabelPresenter.modelLine(null, null).contains(UNKNOWN)
        )
    }

    @Test
    fun isoDateCheckRejectsFreeText() {
        assertTrue("ISO-Datum", CostLabelPresenter.isIsoDate("2026-10-01"))
        assertFalse("Freitext", CostLabelPresenter.isIsoDate("letzten Dienstag"))
        assertFalse("leer", CostLabelPresenter.isIsoDate(""))
    }

    @Test
    fun stalePriceStillShowsAnAmountButLabelsTheSource() {
        val old = today + 400
        val estimate = CostEstimator.estimate(knownModel(), measured(), old)
        val label = CostLabelPresenter.costLabel(estimate)

        assertEquals("Veraltete Quelle nicht erkannt", org.claudroide.app.feature.provider.PriceFreshness.STALE, label.freshness)
        assertTrue(
            "Veraltung muss sichtbar sein: ${label.lines}",
            label.lines.any { it.contains("veraltet") }
        )
    }

    private companion object {
        const val UNKNOWN = "unbekannt"
    }
}