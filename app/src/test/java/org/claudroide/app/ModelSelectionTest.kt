package org.claudroide.app

import org.claudroide.app.feature.provider.ActiveSelection
import org.claudroide.app.feature.provider.CostHint
import org.claudroide.app.feature.provider.ModelAvailabilitySource
import org.claudroide.app.feature.provider.ModelCapability
import org.claudroide.app.feature.provider.ModelSelectionPresenter
import org.claudroide.app.feature.provider.ProviderCallAudit
import org.claudroide.app.feature.provider.SelectionOutcome
import org.claudroide.app.feature.provider.SelectionScope
import org.claudroide.app.feature.provider.SelectionState
import org.claudroide.app.feature.provider.costHintText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 062 — "Modell auswählen".
 *
 * Geprüfte Zusage:
 *  - Ein Modellwechsel löst keine unbemerkte Datenweitergabe aus.
 *  - Nicht verfügbare Modelle werden nicht still ersetzt.
 */
class ModelSelectionTest {

    private val chat = SelectionScope.Chat("chat-42")
    private val project = SelectionScope.Project("projekt-a")

    // ── Zusage 1: kein Modellwechsel ohne Datenweitergabe ──────────────────────

    @Test
    fun modelSwitch_neverCallsTheProvider() {
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "claude-opus-5-5")
        presenter.confirm(chat)
        presenter.request(chat, "anthropic", "claude-sonnet-5-5")
        presenter.confirm(chat)

        assertEquals(
            "Ein Modellwechsel darf keinen einzigen Anbieter-Aufruf auslösen",
            0,
            presenter.providerCallCount()
        )
        assertTrue(presenter.callAudit().protokoll().isEmpty())
    }

    @Test
    fun pendingSwitch_alreadySendsNothing() {
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "claude-opus-5-5")

        assertEquals("Der Wartende-Zustand darf nichts senden", 0, presenter.providerCallCount())
        assertTrue(presenter.snapshot(chat).isPending)
    }

    @Test
    fun buildingTheSelectionList_sendsNothing() {
        val presenter = ModelSelectionPresenter()

        val rows = presenter.optionRows("anthropic")

        assertTrue("Der Katalog sollte Modelle anbieten", rows.isNotEmpty())
        assertEquals(0, presenter.providerCallCount())
    }

    @Test
    fun auditCounter_countsOnlyRealProviderCalls() {
        // Gegenprobe: der Zähler funktioniert überhaupt. Ohne diese Probe
        // wäre "0 Aufrufe" auch bei einem defekten Zähler wahr.
        val audit = ProviderCallAudit()
        assertEquals(0, audit.count)
        audit.record("Chat abschicken")
        audit.record("Bild hochladen")
        assertEquals(2, audit.count)
        assertEquals(2, audit.protokoll().size)
    }

    @Test
    fun auditRecordsWhatWasSent() {
        val seen = mutableListOf<String>()
        val audit = ProviderCallAudit(onCall = { seen.add(it) })
        audit.record("Chat abschicken")

        assertEquals(listOf("Chat abschicken"), seen)
    }

    @Test
    fun auditCanBeReset() {
        val audit = ProviderCallAudit()
        audit.record("Test")
        audit.zuruecksetzen()

        assertEquals(0, audit.count)
        assertTrue(audit.protokoll().isEmpty())
    }

    @Test
    fun switchSummary_statesThatNoDataWasSent() {
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "claude-opus-5-5")
        val applied = presenter.confirm(chat) as SelectionOutcome.Applied

        assertTrue(
            "Die Änderungsmeldung muss die Datenfreigabe benennen",
            applied.selection.changeNote.contains("keine Daten übertragen")
        )
    }

    @Test
    fun pendingSummary_explainsThatNothingIsSentYet() {
        val presenter = ModelSelectionPresenter()

        val pending = presenter.request(chat, "anthropic", "claude-opus-5-5")
            as SelectionOutcome.Pending

        val text = pending.proposal.summaryLines.joinToString(" ")
        assertTrue(text.contains("keine Daten übertragen"))
    }

    // ── Zusage 2: kein stilles Ersetzen ───────────────────────────────────────

    @Test
    fun unknownModel_isRejectedWithAGermanReason() {
        val presenter = ModelSelectionPresenter()

        val outcome = presenter.request(chat, "anthropic", "gpt-4o")

        assertTrue(outcome is SelectionOutcome.Rejected)
        val reason = (outcome as SelectionOutcome.Rejected).reason
        assertTrue("Der Grund muss das Modell nennen", reason.contains("gpt-4o"))
    }

    @Test
    fun unknownProvider_isRejected() {
        val presenter = ModelSelectionPresenter()

        val outcome = presenter.request(chat, "nicht-erfunden", "claude-opus-5-5")

        assertTrue(outcome is SelectionOutcome.Rejected)
        assertTrue((outcome as SelectionOutcome.Rejected).reason.contains("nicht bekannt"))
    }

    @Test
    fun rejectedModel_leavesNoSelectionAndNoSubstitute() {
        val presenter = ModelSelectionPresenter()

        val outcome = presenter.request(chat, "anthropic", "gpt-4o")

        assertTrue(outcome is SelectionOutcome.Rejected)
        assertNull(
            "Nach einer Ablehnung darf kein Ersatzmodell gelten",
            presenter.activeSelection(chat)
        )
        assertTrue(presenter.state(chat) is SelectionState.Idle)
        assertTrue(presenter.snapshot(chat).activeModelId == null)
    }

    @Test
    fun rejectedModel_doesNotReplaceTheExistingOne() {
        val presenter = ModelSelectionPresenter()
        presenter.request(chat, "anthropic", "claude-opus-5-5")
        presenter.confirm(chat)

        presenter.request(chat, "anthropic", "gibt-es-nicht")

        assertEquals(
            "Das geltende Modell darf nicht still wechseln",
            "claude-opus-5-5",
            presenter.activeSelection(chat)?.modelId
        )
    }

    @Test
    fun modelOfAnotherProvider_isRejectedOnThisProvider() {
        // gpt-4o exists, but under "openai" — not under "anthropic".
        val presenter = ModelSelectionPresenter()

        val outcome = presenter.request(chat, "anthropic", "qwen2.5-coder:7b")

        assertTrue(outcome is SelectionOutcome.Rejected)
        assertTrue(
            (outcome as SelectionOutcome.Rejected).reason.contains("nicht angeboten")
        )
    }

    @Test
    fun blankModelId_isRejected() {
        val presenter = ModelSelectionPresenter()

        assertTrue(presenter.request(chat, "anthropic", "   ") is SelectionOutcome.Rejected)
        assertTrue(presenter.request(chat, "  ", "claude-opus-5-5") is SelectionOutcome.Rejected)
    }

    @Test
    fun everyRejectionSaysNoOtherModelWasUsed() {
        val presenter = ModelSelectionPresenter()

        val rejected = listOf(
            presenter.request(chat, "anthropic", "gpt-4o"),
            presenter.request(chat, "nicht-erfunden", "claude-opus-5-5"),
            presenter.request(chat, "anthropic", "")
        ).filterIsInstance<SelectionOutcome.Rejected>()

        assertEquals(3, rejected.size)
        rejected.forEach {
            assertTrue(
                "Die Ablehnung muss sagen, dass nichts ersetzt wurde: ${it.reason}",
                it.reason.contains("kein anderes Modell verwendet") ||
                    it.reason.contains("kein Modell gesetzt")
            )
        }
    }

    // ── Bestätigung ist immer nötig ───────────────────────────────────────────

    @Test
    fun nothingIsActive_beforeConfirmation() {
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "claude-opus-5-5")

        assertNull("Vor der Bestätigung darf kein Modell gelten", presenter.activeSelection(chat))
        assertEquals("claude-opus-5-5", presenter.pendingSwitch(chat)?.choice?.modelId)
        assertTrue(presenter.snapshot(chat).isPending)
    }

    @Test
    fun confirm_appliesTheSelection() {
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "claude-opus-5-5")
        val applied = presenter.confirm(chat) as SelectionOutcome.Applied

        assertTrue(applied.selection is ActiveSelection)
        assertEquals("claude-opus-5-5", presenter.activeSelection(chat)?.modelId)
        assertTrue(presenter.snapshot(chat).isActive)
    }

    @Test
    fun confirmWithoutPendingSwitch_changesNothing() {
        val presenter = ModelSelectionPresenter()

        val outcome = presenter.confirm(chat)

        assertTrue(outcome is SelectionOutcome.Rejected)
        assertTrue(presenter.state(chat) is SelectionState.Idle)
    }

    @Test
    fun cancel_keepsThePreviousModel() {
        val presenter = ModelSelectionPresenter()
        presenter.request(chat, "anthropic", "claude-opus-5-5")
        presenter.confirm(chat)

        presenter.request(chat, "anthropic", "claude-sonnet-5-5")
        assertTrue(presenter.cancel(chat))

        assertEquals("claude-opus-5-5", presenter.activeSelection(chat)?.modelId)
        assertNull(presenter.pendingSwitch(chat))
    }

    @Test
    fun cancelWithoutPendingSwitch_returnsFalse() {
        val presenter = ModelSelectionPresenter()

        assertFalse(presenter.cancel(chat))
    }

    @Test
    fun pendingQuestion_asksInGermanAndNamesTheModel() {
        val presenter = ModelSelectionPresenter()

        val pending = presenter.request(chat, "anthropic", "claude-opus-5-5")
            as SelectionOutcome.Pending

        val question = pending.proposal.confirmationQuestion()
        assertTrue(question.contains("claude-opus-5-5"))
        assertTrue(question.contains("Chat"))
        assertTrue(question.contains("Anthropic") || question.contains("Claude"))
    }

    // ── Auswahl pro Chat und pro Projekt ──────────────────────────────────────

    @Test
    fun chatAndProject_haveIndependentSelections() {
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "claude-opus-5-5")
        presenter.confirm(chat)
        presenter.request(project, "local_server", "qwen2.5-coder:7b")
        presenter.confirm(project)

        assertEquals("claude-opus-5-5", presenter.activeSelection(chat)?.modelId)
        assertEquals("qwen2.5-coder:7b", presenter.activeSelection(project)?.modelId)
    }

    @Test
    fun pendingSwitch_inOneScope_doesNotBlockAnother() {
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "claude-opus-5-5")

        assertTrue(presenter.state(chat) is SelectionState.PendingSwitch)
        assertTrue(presenter.state(project) is SelectionState.Idle)
        assertTrue(presenter.confirm(project) is SelectionOutcome.Rejected)
        assertTrue("Der offene Chat-Wechsel bleibt offen", presenter.state(chat) is SelectionState.PendingSwitch)
    }

    @Test
    fun changeHistory_recordsEveryConfirmedSwitch() {
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "claude-opus-5-5")
        presenter.confirm(chat)
        presenter.request(chat, "anthropic", "claude-sonnet-5-5")
        presenter.confirm(chat)

        val history = presenter.changeHistory(chat)
        assertEquals(2, history.size)
        assertTrue(history[0].contains("erstmals"))
        assertTrue(history[1].contains("von „claude-opus-5-5“"))
    }

    @Test
    fun changeHistory_doesNotRecordRejectedOrCancelledSwitches() {
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "gpt-4o")
        presenter.request(chat, "anthropic", "claude-opus-5-5")
        presenter.cancel(chat)

        assertTrue(presenter.changeHistory(chat).isEmpty())
    }

    @Test
    fun twoChats_keepTheirOwnModel() {
        val other = SelectionScope.Chat("chat-43")
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "claude-opus-5-5")
        presenter.confirm(chat)
        presenter.request(other, "anthropic", "claude-sonnet-5-5")
        presenter.confirm(other)

        assertEquals("claude-opus-5-5", presenter.activeSelection(chat)?.modelId)
        assertEquals("claude-sonnet-5-5", presenter.activeSelection(other)?.modelId)
    }

    // ── Anbieter, Modell, Fähigkeiten, Kostenhinweis ──────────────────────────

    @Test
    fun summary_showsProviderModelCapabilitiesAndCost() {
        val presenter = ModelSelectionPresenter()

        val pending = presenter.request(chat, "anthropic", "claude-opus-5-5")
            as SelectionOutcome.Pending
        val lines = pending.proposal.summaryLines.joinToString(" ")

        assertTrue(lines.contains("Anthropic"))
        assertTrue(lines.contains("claude-opus-5-5"))
        assertTrue(lines.contains("Streaming"))
        assertTrue(lines.contains("Kosten"))
    }

    @Test
    fun capabilityLabels_areGerman() {
        val presenter = ModelSelectionPresenter()

        val pending = presenter.request(chat, "anthropic", "claude-opus-5-5")
            as SelectionOutcome.Pending

        assertTrue(pending.proposal.capabilityLabels.contains("Text"))
        assertTrue(pending.proposal.capabilityLabels.contains("Werkzeuge"))
    }

    @Test
    fun confirmedSelection_keepsTheCapabilities() {
        val presenter = ModelSelectionPresenter()

        presenter.request(chat, "anthropic", "claude-opus-5-5")
        val applied = presenter.confirm(chat) as SelectionOutcome.Applied

        assertTrue(applied.selection.choice.capabilities.contains(ModelCapability.STREAMING))
        assertTrue(applied.selection.choice.isVerified)
    }

    @Test
    fun optionRows_showProviderModelCapabilitiesAndCost() {
        val presenter = ModelSelectionPresenter()

        val rows = presenter.optionRows("anthropic")

        assertTrue(rows.isNotEmpty())
        val row = rows.first { it.modelId == "claude-opus-5-5" }
        assertTrue(row.isAvailable)
        assertEquals("anthropic", row.providerId)
        assertTrue(row.capabilityLabels.contains("Text"))
        assertTrue(row.costHintText.startsWith("Kosten:"))
        assertTrue(row.displayLine().contains("claude-opus-5-5") || row.displayLine().contains("Claude"))
    }

    @Test
    fun optionRows_forUnknownProvider_stayEmptyButDoNotCrash() {
        val presenter = ModelSelectionPresenter()

        assertTrue(presenter.optionRows("nicht-erfunden").isEmpty())
    }

    // ── Kosten: keine erfundenen Zahlen ───────────────────────────────────────

    @Test
    fun withoutCostData_theHintSaysUnknown() {
        val presenter = ModelSelectionPresenter()

        val pending = presenter.request(chat, "anthropic", "claude-opus-5-5")
            as SelectionOutcome.Pending

        assertTrue(pending.proposal.choice.costHint is CostHint.Unknown)
        assertTrue(pending.proposal.costHintText.startsWith("Kosten: unbekannt"))
    }

    @Test
    fun unknownCostHint_neverContainsANumber() {
        val presenter = ModelSelectionPresenter()

        val pending = presenter.request(chat, "anthropic", "claude-opus-5-5")
            as SelectionOutcome.Pending

        assertFalse(
            "Ohne Quelle darf kein Betrag stehen",
            pending.proposal.costHintText.any { it.isDigit() }
        )
    }

    @Test
    fun documentedCost_isShownWithItsSource() {
        val presenter = ModelSelectionPresenter(
            costCatalog = mapOf(
                "claude-opus-5-5" to CostHint.documented(
                    text = "laut Anbieterliste",
                    sourceUrl = "https://example.org/preise"
                )
            )
        )

        val pending = presenter.request(chat, "anthropic", "claude-opus-5-5")
            as SelectionOutcome.Pending

        assertTrue(pending.proposal.costHintText.contains("laut Anbieterliste"))
        assertTrue(pending.proposal.costHintText.contains("https://example.org/preise"))
    }

    @Test
    fun documentedCost_withoutSource_fallsBackToUnknown() {
        assertTrue(CostHint.documented("1,00 EUR", "") is CostHint.Unknown)
        assertTrue(CostHint.documented("", "https://example.org") is CostHint.Unknown)
    }

    @Test
    fun costHintText_isGerman() {
        val unknown = costHintText(CostHint.Unknown("Keine Angabe des Anbieters."))
        assertTrue(unknown.contains("unbekannt"))
        assertNotNull(unknown)
    }

    // ── Verhalten bei unbekanntem Modell im Katalog ──────────────────────────

    @Test
    fun unknownModel_inASecondProvider_stillNeedsNoSubstitute() {
        val presenter = ModelSelectionPresenter()

        // "llama3.2:latest" is offered by the local server.
        val outcome = presenter.request(chat, "anthropic", "llama3.2:latest")

        assertTrue(outcome is SelectionOutcome.Rejected)
        assertNull(presenter.activeSelection(chat))
    }

    @Test
    fun localServerModel_canBeSelected() {
        val presenter = ModelSelectionPresenter()

        val pending = presenter.request(chat, "local_server", "llama3.2:latest")

        assertTrue(
            "Ein Modell des lokalen Servers muss wählbar sein",
            pending is SelectionOutcome.Pending
        )
        val applied = presenter.confirm(chat) as SelectionOutcome.Applied
        assertEquals("local_server", applied.selection.providerId)
        assertEquals("llama3.2:latest", applied.selection.modelId)
        assertEquals(0, presenter.providerCallCount())
    }

    @Test
    fun manualModelEntryIsMarkedUnverified() {
        // A model the user typed in is selectable, but never claimed as verified.
        val presenter = ModelSelectionPresenter(
            availability = ModelAvailabilitySource { _, _ -> null }
        )

        val pending = presenter.request(chat, "custom_endpoint", "mein-eigenes-modell")
            as SelectionOutcome.Pending

        assertFalse(pending.proposal.choice.isVerified)
        assertTrue(pending.proposal.summaryLines.any { it.contains("nein, freie Eingabe") })
    }
}