package org.claudroide.app

import org.claudroide.app.feature.agent.AgentContextManager
import org.claudroide.app.feature.agent.AgentContextManager.ContextPlanStatus
import org.claudroide.app.feature.agent.AgentContextManager.ConversationTurn
import org.claudroide.app.feature.agent.AgentContextManager.DropReason
import org.claudroide.app.feature.agent.AgentContextManager.TurnRole
import org.claudroide.app.feature.project.ContextCandidate
import org.claudroide.app.feature.project.ContextSelection
import org.claudroide.app.feature.project.ContextSelectionPolicy
import org.claudroide.app.feature.project.RelevanceSignal
import org.claudroide.app.feature.project.SelectedContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 074 — „Kontext verwalten“.
 *
 * Die Rechnung läuft mit einem **nicht belegten** Modell: Dafür greift
 * `ModelContextLimits.UNKNOWN_WINDOW_TOKENS` (32 000) ohne reservierte Antwort.
 * Das ist die einzige Konstellation, in der sich die Kürzung mit kleinen Zahlen
 * zeigen lässt, ohne eine erfundene Fenstergröße zu benutzen.
 */
class AgentContextManagerTest {

    private val unknownModel = "nicht-belegtes-modell"
    private val today = 20_800L

    /** 32 000 Tokens ohne reservierte Antwort. */
    private val window = ContextSelectionPolicy.budgetFor(unknownModel, today).windowTokens

    private fun turn(
        id: String,
        tokens: Long? = 1_000L,
        pinned: Boolean = false,
        text: String = "Text von $id",
        role: TurnRole = TurnRole.USER
    ) = ConversationTurn(
        id = id,
        role = role,
        text = text,
        estimatedTokens = tokens,
        isPinned = pinned
    )

    private fun file(path: String, tokens: Int = 500) = ContextCandidate(
        path = path,
        exists = true,
        estimatedTokens = tokens,
        signal = RelevanceSignal.EXPLICIT_MATCH
    )

    private fun plan(
        turns: List<ConversationTurn>,
        files: List<ContextCandidate> = emptyList(),
        systemTokens: Long = 0L,
        toolTokens: Long = 0L,
        modelId: String = unknownModel
    ) = AgentContextManager.plan(
        modelId = modelId,
        systemPromptTokens = systemTokens,
        toolDefinitionTokens = toolTokens,
        turns = turns,
        projectCandidates = files,
        todayEpochDays = today
    )

    // ── Kürzung wird sichtbar ────────────────────────────────────────────────

    @Test
    fun everyTurnIsEitherKeptOrDroppedWithAReason() {
        val turns = (1..10).map { turn("t$it", tokens = 4_000L) }

        val result = plan(turns)

        val kept = result.keptTurns.map { it.id }
        val dropped = result.droppedTurns.map { it.turn.id }
        assertEquals(turns.map { it.id }.toSet(), (kept + dropped).toSet())
        assertTrue("Etwas musste gekürzt werden", result.isTrimmed)
        result.droppedTurns.forEach {
            assertFalse("Jeder Auslassungsgrund ist eine deutsche Begründung", it.message.isBlank())
        }
    }

    @Test
    fun theNewestTurnsSurviveAndTheOldestAreDropped() {
        val turns = (1..10).map { turn("t$it", tokens = 4_000L) }

        val result = plan(turns)

        val kept = result.keptTurns.map { it.id }
        val dropped = result.droppedTurns.map { it.turn.id }
        assertTrue("Der neueste Teil muss dabei sein", kept.contains("t10"))
        assertTrue("Der älteste Teil wird zuerst gekürzt", dropped.contains("t1"))
        assertEquals("Es bleiben die jüngsten Teile", 8, kept.size)
    }

    @Test
    fun theDisclosureNamesEveryDroppedTurn() {
        val turns = (1..10).map { turn("t$it", tokens = 4_000L) }

        val text = plan(turns).disclosureLines().joinToString("\n")

        assertTrue("Die Kürzung muss Überschrift haben", text.contains("NICHT mehr berücksichtigt"))
        assertTrue(text.contains("t1"))
        assertTrue(text.contains("t2"))
        assertTrue(text.contains("älterer Teil, kein Platz mehr"))
    }

    @Test
    fun anUntrimmedPlanSaysSoInsteadOfAList() {
        val result = plan(listOf(turn("t1", tokens = 100L)))

        assertFalse(result.isTrimmed)
        assertTrue(result.disclosureLines().any { it.contains("nichts weggelassen") })
    }

    @Test
    fun aTurnWithUnknownSizeIsDroppedInsteadOfGuessed() {
        val result = plan(listOf(turn("gross", tokens = null), turn("klein", tokens = 100L)))

        val dropped = result.droppedTurns.single()
        assertEquals("gross", dropped.turn.id)
        assertEquals(DropReason.TOKEN_COUNT_UNKNOWN, dropped.reason)
        assertEquals(listOf("klein"), result.keptTurns.map { it.id })
    }

    // ── Festgehaltene Teile ──────────────────────────────────────────────────

    @Test
    fun aPinnedTurnSurvivesEvenWhenItIsTheOldest() {
        val turns = listOf(turn("wichtig", tokens = 5_000L, pinned = true)) +
            (1..10).map { turn("t$it", tokens = 4_000L) }

        val result = plan(turns)

        assertTrue(result.keptTurns.map { it.id }.contains("wichtig"))
        assertTrue(result.keptTurns.first { it.id == "wichtig" }.isPinned)
    }

    @Test
    fun aPinnedTurnIsNeverListedAsDropped() {
        val turns = (1..20).map { turn("t$it", tokens = 4_000L) } +
            listOf(turn("fest", tokens = 2_000L, pinned = true))

        val result = plan(turns)

        assertFalse(result.droppedTurns.any { it.turn.id == "fest" })
    }

    @Test
    fun pinnedPartsThatDoNotFitAreReportedInsteadOfSilentlyRemoved() {
        // Zwei festgehaltene Teile allein über dem Fenster.
        val turns = listOf(
            turn("fest1", tokens = 20_000L, pinned = true),
            turn("fest2", tokens = 20_000L, pinned = true)
        )

        val result = plan(turns)

        assertEquals(ContextPlanStatus.OVER_LIMIT, result.status)
        assertFalse(result.canSend)
        assertEquals(2, result.keptTurns.size)
        assertTrue(result.usage.overageTokens > 0)
        assertTrue(
            result.disclosureLines().any { it.contains("festgehalten") }
        )
    }

    @Test
    fun aSingleTurnLargerThanTheWindowIsDroppedWithAReason() {
        val result = plan(listOf(turn("t1", tokens = 40_000L)))

        // Ein einzelner Teil, der allein nicht passt, wird nicht heimlich
        // versteckt: Er steht mit Grund in der Liste der nicht berücksichtigten
        // Teile, und der Plan ist sichtbar gekürzt.
        assertTrue(result.isTrimmed)
        assertEquals("t1", result.droppedTurns.single().turn.id)
        assertTrue(result.droppedTurns.single().message.isNotBlank())
        assertTrue(result.keptTurns.isEmpty())
    }

    @Test
    fun aPlanThatDoesNotFitAtAllIsNotSent() {
        // Die Systemanweisung allein über dem Fenster: Da hilft auch kein Kürzen
        // von Gesprächsteilen mehr — der Plan ist zu groß.
        val result = plan(listOf(turn("t1", tokens = 1_000L)), systemTokens = 33_000L)

        assertEquals(ContextPlanStatus.OVER_LIMIT, result.status)
        assertFalse(result.canSend)
        assertTrue(result.usage.overageTokens > 0L)
        assertTrue(result.disclosureLines().any { it.contains("zu groß") })
    }

    // ── Fenster, Systemprompt und Werkzeuge ──────────────────────────────────

    @Test
    fun systemAndToolTokensReduceTheRoomForTurns() {
        val turns = (1..10).map { turn("t$it", tokens = 4_000L) }

        val ohnePlatz = plan(turns, systemTokens = 0L)
        val mitPlatz = plan(turns, systemTokens = 16_000L, toolTokens = 8_000L)

        assertTrue(mitPlatz.keptTurns.size < ohnePlatz.keptTurns.size)
        val text = mitPlatz.disclosureLines().joinToString("\n")
        assertTrue(text.contains("Systemanweisung: 16000"))
        assertTrue(text.contains("Werkzeugbeschreibungen: 8000"))
    }

    @Test
    fun theUsageSumsUpWhatIsSent() {
        val result = plan(
            turns = listOf(turn("t1", tokens = 3_000L)),
            systemTokens = 500L,
            toolTokens = 250L
        )

        assertEquals(3_750L, result.usage.sentTokens)
        assertEquals(window - 3_750L, result.usage.remainingTokens)
        assertTrue(result.usage.fits)
    }

    @Test
    fun aBelegModelKeepsRoomForTheAnswer() {
        // Bei einem belegten 1-Mio.-Fenster sind 128 000 Tokens für die Antwort
        // reserviert; das gehört in die Rechnung.
        val result = plan(
            turns = listOf(turn("t1", tokens = 10_000L)),
            modelId = "claude-sonnet-5-5"
        )

        assertEquals(128_000L, result.usage.reservedForAnswerTokens)
        assertEquals(1_000_000L, result.usage.windowTokens)
        assertTrue(result.canSend)
    }

    @Test
    fun anUnknownWindowIsNamedAsUnknown() {
        val result = plan(listOf(turn("t1")))

        assertTrue(
            "Ohne Beleg darf keine erfundene Fenstergröße stehen: " +
                result.disclosureLines().joinToString("\n"),
            result.disclosureLines().any { it.contains("unbekannt") || result.usage.windowTokens == 32_000L }
        )
    }

    // ── Keine vertrauliche Datei über die Kürzung ────────────────────────────

    @Test
    fun aSecretFileNeverReachesTheContextPlan() {
        val result = plan(
            turns = listOf(turn("t1")),
            files = listOf(file("app/Main.kt"), file(".env"))
        )

        assertFalse(result.usage.fileTokens > 0 && result.files.included.any { it.path == ".env" })
        assertEquals(listOf("app/Main.kt"), result.files.included.map { it.path })
    }

    @Test
    fun aSecretFileNeverCostsTokensEvenWhenItWasOffered() {
        val result = plan(
            turns = listOf(turn("t1", tokens = 1_000L)),
            files = listOf(file(".env", tokens = 9_999))
        )

        assertFalse(result.files.included.any { it.path == ".env" })
        assertEquals(0L, result.usage.fileTokens)
    }

    @Test
    fun aSecondCheckAlsoStopsASecretFileThatSlippedIntoTheSelection() {
        // Heute unerreichbar über plan(), weil 068 dieselbe Regel schon anwendet.
        // Der zweite Durchgang wird deshalb mit einer von Hand gebauten Auswahl
        // geprüft: Genau das wäre der Fall, wenn sich die Regel in 068 lockert.
        val selection = ContextSelection(
            included = listOf(
                SelectedContext(
                    path = "app/Main.kt",
                    signal = RelevanceSignal.EXPLICIT_MATCH,
                    estimatedTokens = 500L,
                    tokenSource = "gemessen"
                ),
                SelectedContext(
                    path = ".env",
                    signal = RelevanceSignal.EXPLICIT_MATCH,
                    estimatedTokens = 500L,
                    tokenSource = "gemessen"
                )
            ),
            omitted = emptyList(),
            budget = ContextSelectionPolicy.budgetFor(unknownModel, today),
            usedTokens = 1_000L
        )

        val found = AgentContextManager.findExcludedFiles(selection)

        assertEquals(listOf(".env"), found)
    }

    @Test
    fun anAllowedSelectionPassesTheSecondCheck() {
        val result = plan(listOf(turn("t1")), files = listOf(file("app/Main.kt")))

        assertTrue(AgentContextManager.findExcludedFiles(result.files).isEmpty())
        assertTrue(result.rejectedFiles.isEmpty())
        assertEquals(ContextPlanStatus.READY, result.status)
    }

    @Test
    fun thePlanCarriesNoFileContentAtAll() {
        val result = plan(listOf(turn("t1")), files = listOf(file("app/Main.kt")))

        // Der Typ kennt nur Pfade: Es gibt keine Eigenschaft, die Text einer Datei
        // aufnehmen könnte.
        val properties = result.files.included.first().javaClass.methods
            .map { it.name }
            .filter { it.contains("content") || it.contains("text") || it.contains("body") }
        assertTrue("Kein Dateiinhalt im Plan: $properties", properties.isEmpty())
    }

    @Test
    fun filesAreSelectedWithTheRuleFromTask068() {
        val result = plan(listOf(turn("t1")), files = listOf(file("app/Main.kt", 2_000)))

        // Gleiche Auswahl wie 068: Pfad, Grund und Tokenzahl stehen drin.
        val described = result.files.included.single().describe()
        assertTrue(described.contains("app/Main.kt"))
        assertTrue(described.contains("von Ihnen ausgewählt"))
    }

    // ── Geheimnisse im Gespräch ─────────────────────────────────────────────

    @Test
    fun aKeyInTheConversationIsRedactedBeforeSending() {
        val result = plan(
            listOf(turn("t1", tokens = 1_000L, text = "hier steht sk-live-1234567890 drin"))
        )

        val kept = result.keptTurns.single()
        assertFalse("Schlüssel darf nicht raus", kept.text.contains("sk-live-1234567890"))
        assertTrue(kept.text.contains("[REDACTED]"))
        assertEquals(listOf("t1"), result.sanitisedTurnIds)
        assertTrue(result.disclosureLines().any { it.contains("Geschwärzt") })
    }

    @Test
    fun aNormalTurnIsNotChanged() {
        val result = plan(listOf(turn("t1", tokens = 1_000L, text = "ganz normaler Text")))

        assertEquals("ganz normaler Text", result.keptTurns.single().text)
        assertTrue(result.sanitisedTurnIds.isEmpty())
    }

    // ── Vorschau ─────────────────────────────────────────────────────────────

    @Test
    fun theDisclosureNamesTheWindowTheUsageAndTheFiles() {
        val result = plan(
            turns = listOf(turn("t1", tokens = 1_000L)),
            files = listOf(file("app/Main.kt")),
            systemTokens = 100L
        )

        val text = result.disclosureLines().joinToString("\n")
        assertTrue(text.contains("Kontextfenster: $window Tokens"))
        assertTrue(text.contains("Belegt:"))
        assertTrue(text.contains("Systemanweisung: 100 Tokens"))
        assertTrue(text.contains("app/Main.kt"))
        assertTrue(text.contains("Berücksichtigte Gesprächsteile"))
    }

    @Test
    fun anEmptyConversationSaysSo() {
        val result = plan(emptyList())

        assertTrue(result.disclosureLines().any { it.contains("keine") })
        assertTrue(result.usage.keptTurnTokens == 0L)
    }

    @Test
    fun theKeptTurnsStayInTheirOriginalOrder() {
        val turns = listOf(
            turn("alt", tokens = 100L, pinned = true),
            turn("neu", tokens = 100L)
        )

        val result = plan(turns)

        assertEquals(listOf("alt", "neu"), result.keptTurns.map { it.id })
    }

    @Test
    fun toolResultTurnsAreTreatedLikeAnyOtherTurn() {
        val turns = (1..10).map {
            turn("w$it", tokens = 4_000L, role = TurnRole.TOOL_RESULT)
        }

        val result = plan(turns)

        assertEquals(ContextPlanStatus.READY, result.status)
        assertTrue(result.isTrimmed)
        assertTrue(
            result.droppedTurns.all {
                it.turn.role == TurnRole.TOOL_RESULT
            }
        )
    }

    @Test
    fun thePlanIsReadyWhenEverythingFits() {
        val result = plan(
            turns = (1..4).map { turn("t$it", tokens = 1_000L) },
            files = listOf(file("app/Main.kt", 1_000))
        )

        assertEquals(ContextPlanStatus.READY, result.status)
        assertTrue(result.canSend)
        assertTrue(result.droppedTurns.isEmpty())
    }
}