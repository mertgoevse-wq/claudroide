package org.claudroide.app

import org.claudroide.app.feature.project.ContextCandidate
import org.claudroide.app.feature.project.ContextSelectionPolicy
import org.claudroide.app.feature.project.ContextSelectionPolicy.WholeProjectDecision
import org.claudroide.app.feature.project.ModelContextLimits
import org.claudroide.app.feature.project.RelevanceSignal
import org.claudroide.app.feature.project.SelectedContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 068 — „Nur nötigen Projektkontext wählen“ (Gate).
 *
 * Geprüfte Zusagen:
 *  - Jede an den Anbieter gesendete Datei ist benennbar.
 *  - Kontextgrenzen werden nicht durch stilles Vollprojektladen umgangen.
 *  - Der Geheimnisfilter wird vor dem Kontextversand angewendet.
 */
class ContextSelectionTest {

    /** 2026-10-01 als Tage seit der Epoche, damit die 90-Tage-Frist greift. */
    private val today = 20727L

    private val opus = "claude-opus-5-5"
    private val haiku = "claude-haiku-4-5-20251001"

    private fun file(path: String, tokens: Int, signal: RelevanceSignal = RelevanceSignal.EXPLICIT_MATCH) =
        ContextCandidate(path = path, exists = true, estimatedTokens = tokens, signal = signal)

    // ── Kriterium 1: jede gesendete Datei ist benennbar ──────────────────────

    @Test
    fun everyIncludedFile_isNamedInTheDisclosure() {
        val selection = ContextSelectionPolicy.select(
            opus,
            listOf(file("app/Main.kt", 500), file("app/Build.kt", 400)),
            today
        )

        val lines = selection.disclosureLines().joinToString("\n")
        assertTrue("app/Main.kt muss benannt sein, war:\n$lines", lines.contains("app/Main.kt"))
        assertTrue("app/Build.kt muss benannt sein", lines.contains("app/Build.kt"))
    }

    @Test
    fun aDescription_saysWhatWhereAndWhy() {
        val selection = ContextSelectionPolicy.select(
            opus,
            listOf(file("app/Repository.kt", 900, RelevanceSignal.TASK_TERM_MATCH)),
            today
        )

        val described = selection.included.single().describe()
        assertTrue("Es muss der Dateityp genannt werden: $described", described.startsWith("Datei:"))
        assertTrue("Es muss der Pfad genannt werden: $described", described.contains("app/Repository.kt"))
        assertTrue("Es muss der Grund genannt werden: $described", described.contains("Name passt zum Aufgabentext"))
        assertTrue("Es muss die Tokenzahl genannt werden: $described", described.contains("900 Tokens"))
    }

    @Test
    fun aDescription_namesTheLineRangeForAnExcerpt() {
        val excerpt = SelectedContext(
            path = "app/Main.kt",
            signal = RelevanceSignal.EXPLICIT_MATCH,
            estimatedTokens = 120,
            tokenSource = "gemessen",
            firstLine = 10,
            lastLine = 48
        )

        assertTrue(excerpt.describe().contains("Zeilen 10–48"))
    }

    @Test
    fun anOmittedFile_isAlsoNamedWithItsReason() {
        val selection = ContextSelectionPolicy.select(
            opus,
            listOf(file("app/Main.kt", 500), file("app/Ghost.kt", 10, RelevanceSignal.NONE)),
            today
        )

        val lines = selection.disclosureLines().joinToString("\n")
        assertTrue("Ausgelassene Dateien müssen sichtbar sein:\n$lines", lines.contains("app/Ghost.kt"))
        assertTrue(lines.contains("kein erkennbarer Zusammenhang"))
    }

    @Test
    fun aFileWithoutTokensAndSize_isLeftOutWithAReason() {
        val unknown = ContextCandidate(
            path = "app/Unknown.kt",
            exists = true,
            estimatedTokens = null,
            sizeBytes = null,
            signal = RelevanceSignal.EXPLICIT_MATCH
        )

        val selection = ContextSelectionPolicy.select(opus, listOf(unknown), today)

        assertTrue("Ohne Tokenzahl darf nichts geraten werden", selection.included.isEmpty())
        assertTrue(selection.omitted.single().reason.contains("Tokenzahl unbekannt"))
    }

    @Test
    fun aTokenCountFromFileSize_isLabelledAsAnEstimate() {
        val bySize = ContextCandidate(
            path = "app/Big.kt",
            exists = true,
            estimatedTokens = null,
            sizeBytes = 40_000,
            signal = RelevanceSignal.EXPLICIT_MATCH
        )

        val selection = ContextSelectionPolicy.select(opus, listOf(bySize), today)

        val source = selection.included.single().tokenSource
        assertTrue("Eine Schätzung darf nicht als Messung erscheinen: $source", source.contains("Annahme"))
    }

    // ── Kriterium 2: kein stilles Vollprojektladen ────────────────────────────

    @Test
    fun theBudget_isNeverTheWholeContextWindow() {
        val budget = ContextSelectionPolicy.budgetFor(opus, today)

        assertEquals(1_000_000L, budget.windowTokens)
        assertTrue(
            "Der Projektkontext muss kleiner als das Fenster sein " +
                "(${budget.maxProjectTokens} vs ${budget.windowTokens})",
            budget.maxProjectTokens < budget.windowTokens
        )
    }

    @Test
    fun theBudget_respectsTheHardCeilingEvenForALargeWindow() {
        val budget = ContextSelectionPolicy.budgetFor(opus, today)

        assertTrue(
            "Die Obergrenze gilt auch für ein 1-Mio.-Token-Fenster",
            budget.maxProjectTokens <= ContextSelectionPolicy.HARD_PROJECT_TOKEN_CEILING
        )
    }

    @Test
    fun anUnknownModel_doesNotGetAnInventedWindow() {
        val budget = ContextSelectionPolicy.budgetFor("gibt-es-nicht", today)

        assertFalse("Ohne Beleg darf kein Fenster behauptet werden", budget.isWindowKnown)
        assertEquals(ModelContextLimits.UNKNOWN_WINDOW_TOKENS, budget.windowTokens)
        assertTrue(budget.disclosureLines().any { it.contains("unbekannt") })
    }

    @Test
    fun theAnswerTokens_areReservedAndNotSpentOnProjectFiles() {
        val budget = ContextSelectionPolicy.budgetFor(opus, today)

        assertEquals(128_000L, budget.reservedForAnswerTokens)
        assertTrue(budget.disclosureLines().any { it.contains("Für die Antwort") })
    }

    @Test
    fun filesThatDoNotFit_areOmittedRatherThanTruncatedSilently() {
        // Each file is 40 000 tokens; only one fits in the 60 000 ceiling.
        val candidates = (1..3).map { file("app/File$it.kt", 40_000) }

        val selection = ContextSelectionPolicy.select(opus, candidates, today)

        assertEquals("Nur was ins Budget passt, wird gesendet", 1, selection.included.size)
        assertEquals(2, selection.omitted.size)
        assertTrue(selection.omitted.all { it.reason.startsWith(ContextSelectionPolicy.OMITTED_FOR_BUDGET) })
    }

    @Test
    fun expansionIsOffered_onlyWhenSomethingWasDropped() {
        val fits = listOf(file("app/Small.kt", 100))

        assertFalse(ContextSelectionPolicy.select(opus, fits, today).needsExpansion)
    }

    @Test
    fun expansionIsOffered_whenTheBudgetDroppedAFile() {
        val tooBig = (1..2).map { file("app/File$it.kt", 40_000) }

        assertTrue(ContextSelectionPolicy.select(opus, tooBig, today).needsExpansion)
    }

    @Test
    fun expansion_onlyAddsTheFilesThatWereActuallyRequested() {
        val candidates = listOf(
            file("app/Wanted.kt", 500),
            file("app/NotWanted.kt", 500)
        )

        val selection = ContextSelectionPolicy.expand(
            modelId = opus,
            candidates = candidates,
            requestedPaths = setOf("app/Wanted.kt"),
            todayEpochDays = today,
            additionalTokens = 10_000
        )

        assertEquals(
            "Eine Erweiterung darf nicht still den ganzen Bestand nachrücken",
            listOf("app/Wanted.kt"),
            selection.included.map { it.path }
        )
    }

    @Test
    fun expansion_cannotPushPastTheHardCeiling() {
        val candidates = (1..4).map { file("app/File$it.kt", 40_000) }

        val selection = ContextSelectionPolicy.expand(
            modelId = opus,
            candidates = candidates,
            requestedPaths = candidates.map { it.path }.toSet(),
            todayEpochDays = today,
            additionalTokens = 10_000_000
        )

        assertTrue(
            "Auch eine ausdrücklich riesige Erweiterung bleibt unter der Obergrenze",
            selection.budget.maxProjectTokens <= ContextSelectionPolicy.HARD_PROJECT_TOKEN_CEILING
        )
        assertEquals(1, selection.included.size)
    }

    @Test
    fun wholeProjectLoad_isRefusedWithoutAcknowledgement() {
        val candidates = (1..3).map { file("app/File$it.kt", 500) }

        val decision = ContextSelectionPolicy.selectWholeProject(
            opus, candidates, today, acknowledgedByUser = false
        )

        assertTrue(
            "Ohne Bestätigung darf kein Vollprojekt geladen werden, war $decision",
            decision is WholeProjectDecision.Refused
        )
    }

    @Test
    fun theRefusal_explainsWhatDoesHappenInstead() {
        val decision = ContextSelectionPolicy.selectWholeProject(
            opus, listOf(file("app/File.kt", 500)), today, acknowledgedByUser = false
        ) as WholeProjectDecision.Refused

        assertTrue(decision.reason.contains("nicht als Kontext gesendet"))
        assertTrue(decision.reason.contains("Wählen Sie die benötigten Dateien aus"))
    }

    @Test
    fun wholeProjectLoad_afterAcknowledgement_stillRespectsTheCeiling() {
        val candidates = (1..5).map { file("app/File$it.kt", 40_000) }

        val selection = (
            ContextSelectionPolicy.selectWholeProject(opus, candidates, today, acknowledgedByUser = true)
                as WholeProjectDecision.Selected
            ).selection

        assertEquals(1, selection.included.size)
        assertTrue(
            selection.omitted.any { it.reason.contains("passen") && it.reason.contains("Budget") }
        )
    }

    @Test
    fun wholeProjectLoad_afterAcknowledgement_stillKeepsFilesWithoutContext() {
        // Acknowledging "send everything" must not turn files without any
        // recognised connection into context.
        val candidates = (1..3).map { file("app/File$it.kt", 500, RelevanceSignal.NONE) }

        val selection = (
            ContextSelectionPolicy.selectWholeProject(opus, candidates, today, acknowledgedByUser = true)
                as WholeProjectDecision.Selected
            ).selection

        assertTrue(
            "Ohne erkennbaren Zusammenhang bleibt auch das Vollprojekt leer",
            selection.included.isEmpty()
        )
    }

    // ── Kriterium 3: Geheimnisfilter vor dem Kontextversand ───────────────────

    @Test
    fun aSecretFile_isNeverSelected() {
        val candidates = listOf(
            file(".env", 100),
            file("server.pem", 100),
            file("id_rsa.bak", 100),
            file("app/Main.kt", 500)
        )

        val selection = ContextSelectionPolicy.select(opus, candidates, today)

        val paths = selection.included.map { it.path }
        assertFalse(".env darf nie im Kontext sein", paths.contains(".env"))
        assertFalse("server.pem darf nie im Kontext sein", paths.contains("server.pem"))
        assertFalse("id_rsa.bak darf nie im Kontext sein", paths.contains("id_rsa.bak"))
        assertTrue("Die normale Quelldatei bleibt dabei", paths.contains("app/Main.kt"))
    }

    @Test
    fun aSecretFile_isNamedAsOmitted_notAsSendable() {
        val selection = ContextSelectionPolicy.select(opus, listOf(file(".env", 100)), today)

        assertTrue(selection.included.isEmpty())
        assertTrue(
            "Der Nutzer muss sehen, warum .env nicht mitgeht",
            selection.omitted.single().reason.contains("nie an einen Anbieter gesendet")
        )
    }

    @Test
    fun expansion_cannotSmuggleInASecretEither() {
        val candidates = listOf(file(".env", 100), file("credentials.json", 100))

        val selection = ContextSelectionPolicy.expand(
            modelId = opus,
            candidates = candidates,
            requestedPaths = candidates.map { it.path }.toSet(),
            todayEpochDays = today,
            additionalTokens = 50_000
        )

        assertTrue(
            "Eine ausdrückliche Erweiterung ist keine Ausnahme von der Geheimnisregel",
            selection.included.isEmpty()
        )
    }

    @Test
    fun aMissingFile_isNamedInGermanAndNotSent() {
        val missing = ContextCandidate(
            path = "app/Ghost.kt",
            exists = false,
            estimatedTokens = 100,
            signal = RelevanceSignal.EXPLICIT_MATCH
        )

        val selection = ContextSelectionPolicy.select(opus, listOf(missing), today)

        assertTrue(selection.included.isEmpty())
        assertTrue(selection.omitted.single().reason.contains("nicht gefunden"))
    }

    // ── Belegte Fenstergrößen ─────────────────────────────────────────────────

    @Test
    fun theWindowTable_matchesTheDocumentedSource() {
        // Abgerufen am ModelContextLimits.VERIFIED von ModelContextLimits.SOURCE_URL.
        assertEquals(1_000_000L, ModelContextLimits.forModel(opus)!!.maxInputTokens)
        assertEquals(128_000L, ModelContextLimits.forModel(opus)!!.maxOutputTokens)
        assertEquals(200_000L, ModelContextLimits.forModel(haiku)!!.maxInputTokens)
        assertEquals(64_000L, ModelContextLimits.forModel(haiku)!!.maxOutputTokens)
    }

    @Test
    fun everyWindowEntry_carriesASourceAndAUsableDate() {
        ModelContextLimits.TABLE.forEach { (id, limit) ->
            assertTrue("$id braucht eine http(s)-Quelle", limit.sourceUrl.startsWith("https://"))
            assertTrue("$id braucht ein lesbares Prüfdatum", limit.isUsable)
        }
    }

    @Test
    fun theBudget_showsHowOldTheWindowSourceIs() {
        val budget = ContextSelectionPolicy.budgetFor(opus, today + 100)

        assertEquals(100L, budget.ageInDays)
        assertTrue(
            budget.disclosureLines().any { it.contains("Alter 100 Tage") }
        )
    }

    // ── Reihenfolge und Erklärbarkeit ────────────────────────────────────────

    @Test
    fun anExplicitlyChosenFile_comesBeforeATaskMatch() {
        val candidates = listOf(
            file("app/Model.kt", 5_000, RelevanceSignal.TASK_TERM_MATCH),
            file("app/Chosen.kt", 4_000, RelevanceSignal.EXPLICIT_MATCH)
        )

        val ranked = ContextSelectionPolicy.rank(candidates)

        assertEquals("app/Chosen.kt", ranked.first().path)
    }

    @Test
    fun withinTheSameSignal_theCheapestFileComesFirst() {
        val candidates = listOf(
            file("app/Big.kt", 9_000, RelevanceSignal.TASK_TERM_MATCH),
            file("app/Small.kt", 100, RelevanceSignal.TASK_TERM_MATCH)
        )

        assertEquals("app/Small.kt", ContextSelectionPolicy.rank(candidates).first().path)
    }

    @Test
    fun aProjectInstructionFile_isRecognisedByName() {
        assertEquals(
            RelevanceSignal.PROJECT_INSTRUCTION,
            ContextSelectionPolicy.signalFor("CLAUDE.md", emptyList())
        )
        assertEquals(
            RelevanceSignal.PROJECT_INSTRUCTION,
            ContextSelectionPolicy.signalFor("docs/agents.md", emptyList())
        )
    }

    @Test
    fun aTaskTerm_isMatchedAgainstTheFileNameCaseInsensitively() {
        val signal = ContextSelectionPolicy.signalFor(
            "app/src/MainRepository.kt",
            listOf("Repository")
        )

        assertEquals(RelevanceSignal.TASK_TERM_MATCH, signal)
    }

    @Test
    fun noTaskTerm_meansNoSignalRatherThanAGuess() {
        assertEquals(
            RelevanceSignal.NONE,
            ContextSelectionPolicy.signalFor("app/Utils.kt", listOf("Repository"))
        )
    }

    @Test
    fun theSameFilter_canBeAppliedBeforeSelection() {
        val candidates = listOf(file(".env", 100), file("app/Main.kt", 500))

        val filtered = ContextSelectionPolicy.filteredCandidates(candidates)

        assertEquals(listOf("app/Main.kt"), filtered.map { it.path })
    }

    @Test
    fun anEmptyCandidateList_producesAnEmptyHonestResult() {
        val selection = ContextSelectionPolicy.select(opus, emptyList(), today)

        assertTrue(selection.isEmpty)
        assertEquals(0L, selection.usedTokens)
        assertTrue(selection.disclosureLines().any { it.contains("keine Projektdateien") })
    }
}