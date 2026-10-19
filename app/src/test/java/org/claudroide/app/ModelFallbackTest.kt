package org.claudroide.app

import org.claudroide.app.feature.provider.CatalogModelAvailability
import org.claudroide.app.feature.provider.CostEstimator
import org.claudroide.app.feature.provider.FailureKind
import org.claudroide.app.feature.provider.FallbackDecision
import org.claudroide.app.feature.provider.FallbackMode
import org.claudroide.app.feature.provider.FallbackOutcome
import org.claudroide.app.feature.provider.FallbackSetup
import org.claudroide.app.feature.provider.ModelAvailabilitySource
import org.claudroide.app.feature.provider.ModelFallbackPolicy
import org.claudroide.app.feature.provider.PriceFreshness
import org.claudroide.app.feature.provider.ProviderTermsRecord
import org.claudroide.app.feature.provider.ProviderTermsSource
import org.claudroide.app.feature.provider.SelectionScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Task 063 — „Ersatzmodell einstellen“.
 *
 * Geprüfte Zusagen:
 *  - Automatischer Wechsel nur nach ausdrücklicher Einrichtung.
 *  - Anbieterkosten und Datenregeln werden erneut berücksichtigt.
 *  - Keine stille Übertragung an andere Anbieter.
 */
class ModelFallbackTest {

    private val chat = SelectionScope.Chat("chat-1")
    private val project = SelectionScope.Project("projekt-1")

    /** The Haiku id the catalog actually offers (with its date suffix). */
    private val HAIKU = "claude-haiku-4-5-20251001"

    /** 2026-10-01 as days since the epoch, so the 90-day price window is current. */
    private val today = 20727L

    private fun policy(
        termsFor: (String) -> ProviderTermsRecord? = { currentTerms() }
    ) = ModelFallbackPolicy(terms = ProviderTermsSource { providerId ->
        termsFor(providerId)
    })

    private fun currentTerms() = ProviderTermsRecord(
        providerId = "openrouter",
        termsOfServiceUrl = "https://openrouter.ai/terms",
        privacyPolicyUrl = "https://openrouter.ai/privacy",
        lastVerifiedDate = "2026-10-01",
        verifiedEpochDays = today
    )

    /** Availability that accepts every pair, to isolate a single check in a test. */
    private fun acceptingAvailability() = object : ModelAvailabilitySource {
        override fun unavailabilityReason(providerId: String, modelId: String) = null
    }

    /** Establishes a fallback to the same provider, which the catalog accepts. */
    private fun ModelFallbackPolicy.setUpFallback(
        scope: SelectionScope = chat,
        providerId: String = "anthropic",
        modelId: String = HAIKU,
        acknowledged: Boolean = true,
        acceptedHigherCost: Boolean = true,
        onDay: Long = today
    ) {
        configure(scope, providerId, modelId, acknowledged, acceptedHigherCost, onDay)
    }

    // ── Criterion 1: automatic switch only after explicit setup ────────────────

    @Test
    fun withoutSetup_thePolicyAsksTheUser() {
        val decision = policy().decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        )

        assertTrue(
            "Ohne Einrichtung muss nachgefragt werden, war $decision",
            decision is FallbackDecision.AskUser
        )
    }

    @Test
    fun newScope_startsInAskFirstMode() {
        val policy = policy()

        assertEquals(FallbackMode.ASK_FIRST, policy.mode(chat))
        assertEquals(FallbackMode.ASK_FIRST, policy.mode(project))
    }

    @Test
    fun afterSetup_theModeSwitchesToUseApproved() {
        val policy = policy().apply { setUpFallback() }

        assertEquals(FallbackMode.USE_APPROVED_FALLBACK, policy.mode(chat))
    }

    @Test
    fun setupIsScopedPerChatAndProject() {
        val policy = policy().apply { setUpFallback(scope = chat) }

        assertEquals(FallbackMode.USE_APPROVED_FALLBACK, policy.mode(chat))
        assertEquals(
            "Die Einrichtung eines Chats darf kein Projekt freischalten",
            FallbackMode.ASK_FIRST,
            policy.mode(project)
        )
    }

    @Test
    fun clearingASetup_returnsTheScopeToAsking() {
        val policy = policy().apply {
            setUpFallback()
            clear(chat)
        }

        assertEquals(FallbackMode.ASK_FIRST, policy.mode(chat))
        assertTrue(policy.setupFor(chat) == null)
    }

    @Test
    fun decision_neverCallsTheProvider() {
        val policy = policy().apply { setUpFallback() }

        policy.decide(chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today)
        policy.decide(chat, "anthropic", "claude-opus-5-5", FailureKind.OVERLOADED, today)

        assertEquals(
            "Die Fallback-Politik darf keinen Anbieter aufrufen",
            0,
            policy.providerCallCount()
        )
    }

    // ── A refusal is never routed around ──────────────────────────────────────

    @Test
    fun safetyRefusal_isNotRetriedOnAnotherProvider() {
        val policy = policy().apply { setUpFallback() }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.SAFETY_REFUSAL, today
        )

        assertTrue(
            "Eine Ablehnung darf nicht umgangen werden, war $decision",
            decision is FallbackDecision.Fail
        )
    }

    @Test
    fun authenticationFailure_isNotRetriedOnAnotherProvider() {
        val policy = policy().apply { setUpFallback() }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.AUTHENTICATION_FAILED, today
        )
        assertTrue(decision is FallbackDecision.Fail)
    }

    @Test
    fun failExplanation_saysInGermanThatNoOtherProviderIsTried() {
        val policy = policy().apply { setUpFallback() }

        val reason = (
            policy.decide(
                chat, "anthropic", "claude-opus-5-5", FailureKind.SAFETY_REFUSAL, today
            ) as FallbackDecision.Fail
            ).reason

        assertTrue(reason.contains("nicht automatisch ein anderer Anbieter"))
    }

    // ── Criterion 2: provider costs and data rules re-checked ─────────────────

    @Test
    fun setupIsRejected_whenTheReplacementIsNotAvailable() {
        val outcome = policy().configure(
            chat, "anthropic", "gibt-es-nicht", true, true, today
        )

        assertTrue(
            "Ein nicht verfügbares Modell darf nicht eingerichtet werden",
            outcome is FallbackOutcome.Rejected
        )
    }

    @Test
    fun rejection_doesNotLeaveAFallbackBehind() {
        val policy = policy()
        val outcome = policy.configure(chat, "anthropic", "gibt-es-nicht", true, true, today)

        assertTrue(outcome is FallbackOutcome.Rejected)
        assertEquals(FallbackMode.ASK_FIRST, policy.mode(chat))
        assertTrue(policy.setupFor(chat) == null)
    }

    @Test
    fun unacknowledgedTransfer_blocksTheAutomaticSwitch() {
        val policy = policy().apply { setUpFallback(acknowledged = false) }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        )

        assertTrue(
            "Ohne Bestätigung darf nicht automatisch gesendet werden, war $decision",
            decision is FallbackDecision.AskUser
        )
        assertTrue((decision as FallbackDecision.AskUser).reasons.any { it.contains("nicht bestätigt") })
    }

    @Test
    fun expiredSetup_blocksTheAutomaticSwitch() {
        val policy = policy().apply {
            setUpFallback(onDay = today - ModelFallbackPolicy.SETUP_MAX_AGE_DAYS - 1)
        }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        )

        assertTrue(decision is FallbackDecision.AskUser)
        assertTrue((decision as FallbackDecision.AskUser).reasons.any { it.contains("erneuert") })
    }

    @Test
    fun unpricedReplacement_blocksTheAutomaticSwitch() {
        // A model the catalog accepts, but which has no entry in the price table.
        // Availability must not be the reason it is blocked — the missing price is.
        val policy = ModelFallbackPolicy(
            availability = acceptingAvailability(),
            terms = ProviderTermsSource { currentTerms() }
        )
        policy.configure(chat, "anthropic", "unpriced-modell", true, true, today)

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        )

        assertTrue(
            "Ein Modell ohne Preiseintrag darf nicht automatisch genutzt werden",
            decision is FallbackDecision.AskUser
        )
        assertTrue(
            "Der Grund muss der fehlende Preis sein, nicht die Verfügbarkeit",
            (decision as FallbackDecision.AskUser).reasons.any { it.contains("kein belegter Preis") }
        )
    }

    @Test
    fun moreExpensiveReplacement_needsAnAcceptedHigherCost() {
        val policy = policy().apply {
            setUpFallback(modelId = "claude-opus-5-5", acceptedHigherCost = false)
        }

        val decision = policy.decide(
            chat, "anthropic", HAIKU, FailureKind.RATE_LIMIT, today
        )

        assertTrue(decision is FallbackDecision.AskUser)
        assertTrue(
            (decision as FallbackDecision.AskUser).reasons.any { it.contains("teurer") }
        )
    }

    @Test
    fun acceptedHigherCost_allowsTheSwitchEvenWhenPricedHigher() {
        val policy = policy().apply {
            setUpFallback(modelId = "claude-opus-5-5", acceptedHigherCost = true)
        }

        val decision = policy.decide(
            chat, "anthropic", HAIKU, FailureKind.RATE_LIMIT, today
        )

        assertTrue(
            "Ein akzeptierter Mehrpreis darf den Wechsel nicht blockieren, war $decision",
            decision is FallbackDecision.UseFallback
        )
    }

    @Test
    fun staleProviderTerms_blockTheAutomaticSwitch() {
        val policy = policy(termsFor = { currentTerms().copy(verifiedEpochDays = today - 91) })
            .apply { setUpFallback() }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        )

        assertTrue(decision is FallbackDecision.AskUser)
        assertTrue(
            (decision as FallbackDecision.AskUser).reasons.any { it.contains("älter als") }
        )
    }

    @Test
    fun missingProviderTerms_blockTheAutomaticSwitch() {
        val policy = policy(termsFor = { null }).apply { setUpFallback() }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        )

        assertTrue(
            "Fehlende Bedingungen gelten als ungeprüft, nicht als Erlaubnis",
            decision is FallbackDecision.AskUser
        )
    }

    @Test
    fun costReassessment_reportsFreshnessForTheReplacement() {
        val policy = policy()

        val cost = policy.reassessCosts("claude-opus-5-5", HAIKU, today)

        assertEquals(HAIKU, cost.targetModelId)
        assertEquals(PriceFreshness.CURRENT, cost.freshness)
        assertTrue(cost.isPriced)
        assertFalse("Haiku ist günstiger als Opus", cost.isMoreExpensiveThanPrimary)
    }

    @Test
    fun costReassessment_flagsAnUnpricedModel() {
        val policy = policy()

        val cost = policy.reassessCosts("claude-opus-5-5", "unbekanntes-modell", today)

        assertFalse("Ein Modell ohne Preiseintrag ist nicht bepreist", cost.isPriced)
        assertEquals(PriceFreshness.UNVERIFIED, cost.freshness)
    }

    @Test
    fun costReassessment_marksAnOldPriceStale() {
        val policy = policy()

        val cost = policy.reassessCosts("claude-opus-5-5", HAIKU, today + 91)

        assertEquals(PriceFreshness.STALE, cost.freshness)
    }

    @Test
    fun costReassessment_isNotCachedInTheSetup() {
        val policy = policy().apply { setUpFallback() }

        assertTrue(policy.reassessCosts("claude-opus-5-5", HAIKU, today).isPriced)
        val later = policy.reassessCosts("claude-opus-5-5", HAIKU, today + 120)

        assertEquals(
            "Die Preisprüfung muss bei jedem Aufruf neu erfolgen",
            PriceFreshness.STALE,
            later.freshness
        )
    }

    // ── Criterion 3: no silent transfer to another provider ───────────────────

    @Test
    fun disclosures_nameTheReplacementProviderAndModel() {
        val policy = policy().apply { setUpFallback() }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        ) as FallbackDecision.UseFallback

        val all = decision.disclosures.joinToString("\n")
        assertTrue(all.contains(HAIKU))
        assertTrue(all.contains("Anthropic"))
        assertTrue(all.contains("Daten verlassen das Gerät"))
    }

    @Test
    fun disclosures_sayThePrimaryModelIsNotUsedForThisRequest() {
        val policy = policy().apply { setUpFallback() }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        ) as FallbackDecision.UseFallback

        assertTrue(
            decision.disclosures.any { it.contains("claude-opus-5-5") && it.contains("nicht benutzt") }
        )
    }

    @Test
    fun disclosures_areNeverEmpty() {
        val policy = policy().apply { setUpFallback() }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        ) as FallbackDecision.UseFallback

        assertTrue(decision.disclosures.isNotEmpty())
    }

    @Test
    fun useFallback_carriesTheSetupThatWasActuallyUsed() {
        val policy = policy().apply { setUpFallback() }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        ) as FallbackDecision.UseFallback

        assertEquals(HAIKU, decision.target.targetModelId)
        assertEquals(chat, decision.target.scope)
    }

    @Test
    fun aFallbackIsNeverOfferedToADifferentProviderWithoutItsOwnSetup() {
        val policy = ModelFallbackPolicy(
            availability = acceptingAvailability(),
            terms = ProviderTermsSource { currentTerms() }
        )
        // A fallback set up for the chat, but asked about for the project.
        policy.setUpFallback(scope = chat)

        val decision = policy.decide(
            project, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        )

        assertTrue(
            "Ein Bereich ohne eigene Einrichtung muss nachfragen, war $decision",
            decision is FallbackDecision.AskUser
        )
    }

    @Test
    fun askUser_explainsEveryBlockingReason() {
        val policy = policy(termsFor = { null }).apply { setUpFallback(acknowledged = false) }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        ) as FallbackDecision.AskUser

        assertEquals(2, decision.reasons.size)
        assertTrue(decision.reasons.any { it.contains("nicht bestätigt") })
        assertTrue(decision.reasons.any { it.contains("nicht hinterlegt") })
    }

    @Test
    fun askUser_offersRetryingTheReplacementAndChoosingManually() {
        val policy = policy()

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        ) as FallbackDecision.AskUser

        assertTrue(decision.options.any { it.contains("erneut versuchen") })
        assertTrue(decision.options.any { it.contains("selbst auswählen") })
    }

    @Test
    fun setupRejection_explainsInGermanAndNamesNoOtherReplacement() {
        val outcome = policy().configure(chat, "unbekannt", HAIKU, true, true, today)

        assertTrue(outcome is FallbackOutcome.Rejected)
        val reason = (outcome as FallbackOutcome.Rejected).reason
        assertTrue(reason.contains("nicht bekannt"))
        assertTrue(reason.contains("kein anderer Ersatz"))
    }

    @Test
    fun aSavedSetup_isReadableBack() {
        val policy = policy().apply { setUpFallback() }

        val setup: FallbackSetup = policy.setupFor(chat)!!

        assertEquals("anthropic", setup.targetProviderId)
        assertEquals(HAIKU, setup.targetModelId)
        assertTrue(setup.acknowledgedExternalTransfer)
    }

    @Test
    fun anUnavailableProvider_blocksTheSwitchAtDecisionTime() {
        // The catalog accepts the pair at setup time, but not at decide time.
        var available = true
        val policy = ModelFallbackPolicy(
            availability = object : ModelAvailabilitySource {
                override fun unavailabilityReason(providerId: String, modelId: String) =
                    if (available) null else "Anbieter ist inzwischen abgeschaltet."
            },
            terms = ProviderTermsSource { currentTerms() }
        )
        policy.setUpFallback()
        available = false

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        )

        assertTrue(decision is FallbackDecision.AskUser)
        assertTrue(
            (decision as FallbackDecision.AskUser).reasons.any { it.contains("nicht mehr verfügbar") }
        )
    }

    @Test
    fun costReassessment_comparesRatesAndNotInventedWeights() {
        val policy = policy()

        val cheaper = policy.reassessCosts("claude-opus-5-5", HAIKU, today)
        val pricier = policy.reassessCosts(HAIKU, "claude-fable-5-1", today)

        assertFalse(cheaper.isMoreExpensiveThanPrimary)
        assertTrue("Fable ist teurer als Haiku", pricier.isMoreExpensiveThanPrimary)
    }

    @Test
    fun anUnpricedPrimary_cannotMakeTheReplacementLookMoreExpensive() {
        val policy = policy()

        val cost = policy.reassessCosts("unbekanntes-hauptmodell", "claude-opus-5-5", today)

        assertTrue("Der Ersatz selbst ist bepreist", cost.isPriced)
        assertFalse(
            "Ohne bekannten Hauptpreis darf kein Mehrpreis behauptet werden",
            cost.isMoreExpensiveThanPrimary
        )
    }

    @Test
    fun reassessmentLines_carryTheSourceUrl() {
        val policy = policy()

        val lines = policy.reassessCosts("claude-opus-5-5", HAIKU, today).lines

        assertTrue(lines.any { it.contains("https://") })
    }

    @Test
    fun priceRates_matchTheDocumentedTable() {
        val policy = policy()

        val cost = policy.reassessCosts(HAIKU, "claude-fable-5-1", today)

        // Fable 10+50 = 60 per million, Haiku 1+5 = 6. Fable is more expensive.
        assertTrue(cost.isMoreExpensiveThanPrimary)
        // compareTo, nicht equals: BigDecimal.equals vergleicht auch die Nachkommastellen,
        // "60.00".compareTo("60") ist 0, "60.00".equals("60") ist false.
        assertEquals(
            "Fable kostet 60 USD je eine Million Tokens (10 Eingabe + 50 Ausgabe)",
            0,
            BigDecimal("60").compareTo(totalRateOf("claude-fable-5-1"))
        )
        assertEquals(
            "Haiku kostet 6 USD je eine Million Tokens (1 Eingabe + 5 Ausgabe)",
            0,
            BigDecimal("6").compareTo(totalRateOf(HAIKU))
        )
    }

    /** Summe aus Eingabe- und Ausgabepreis, unabhängig von der Skala. */
    private fun totalRateOf(modelId: String): BigDecimal {
        val price = CostEstimator.priceFor(modelId)!!
        return price.inputUsdPerMillionTokens.add(price.outputUsdPerMillionTokens)
    }

    @Test
    fun fallbackToTheFailingModel_isNotTreatedAsAReplacement() {
        // The setup points at the very model that just failed. Switching to it
        // is a retry, not a fallback: it would burn the same quota again.
        val policy = policy().apply {
            setUpFallback(providerId = "anthropic", modelId = "claude-opus-5-5")
        }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        )

        assertTrue(
            "Ein Wechsel auf dasselbe Modell ist kein Ersatz, war $decision",
            decision is FallbackDecision.AskUser
        )
        assertTrue(
            (decision as FallbackDecision.AskUser).reasons.any { it.contains("dasselbe Modell") }
        )
    }

    @Test
    fun fallbackToTheSameModelOnADifferentProvider_isStillAReplacement() {
        val policy = ModelFallbackPolicy(
            availability = acceptingAvailability(),
            terms = ProviderTermsSource { currentTerms() }
        )
        policy.configure(chat, "openrouter", "claude-opus-5-5", true, true, today)

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        )

        assertTrue(
            "Ein anderer Anbieter ist auch bei gleichem Modellname ein Ersatz, war $decision",
            decision is FallbackDecision.UseFallback
        )
    }

    @Test
    fun stalePrice_isReportedWithItsRealAge() {
        val policy = policy().apply { setUpFallback() }

        val decision = policy.decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today + 100
        ) as FallbackDecision.AskUser

        val age = policy.reassessCosts("claude-opus-5-5", HAIKU, today + 100).priceAgeInDays
        assertTrue(
            "Die Begründung nennt das Alter der Preisquelle, war: ${decision.reasons}",
            decision.reasons.any { it.contains("$age Tage alt") }
        )
    }

    @Test
    fun askUser_withoutASetup_doesNotInventAModelName() {
        val decision = policy().decide(
            chat, "anthropic", "claude-opus-5-5", FailureKind.RATE_LIMIT, today
        ) as FallbackDecision.AskUser

        assertTrue(
            "Ohne Einrichtung darf kein Modellname erfunden werden, war ${decision.options}",
            decision.options.none { it.contains("unbekannt") }
        )
    }

    @Test
    fun catalogAvailability_isUsedByDefault() {
        // The default source must reject an unknown provider, not accept it.
        val reason = CatalogModelAvailability.unavailabilityReason("unbekannt", HAIKU)

        assertTrue(reason != null)
    }
}
