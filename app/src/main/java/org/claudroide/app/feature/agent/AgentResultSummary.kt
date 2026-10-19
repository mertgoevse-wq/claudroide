package org.claudroide.app.feature.agent

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 076 — „Ergebnis zusammenfassen“.
 *
 * Ziel: Am Ende klar berichten, was geändert, geprüft oder nicht geschafft wurde.
 *
 * Der Bericht ist die letzte Stelle, an der ein Lauf dem Nutzer etwas vormacht.
 * Deshalb stehen die drei Zusagen der Aufgabe nicht im Kommentar, sondern im Typ:
 *
 *  1. **Vorschlag, gespeicherte Änderung und Git-Upload bleiben unterscheidbar.**
 *     [ChangeLevel] kennt den Weg einer Datei vom Vorschlag bis zum Hochladen als
 *     eigene Werte, und [AgentResultSummary.changeLevel] nennt für den ganzen Lauf
 *     den **schwächsten** davon. Ein Lauf, in dem vier Dateien hochgeladen und eine
 *     nur lokal gespeichert wurde, ist dadurch „lokal gespeichert“ und nicht
 *     „hochgeladen“. Der Bericht kann eine Stufe nicht behaupten, die nicht jede
 *     einzelne Änderung erreicht hat — [AgentResultSummary.hasReached] ist die
 *     einzige Stelle, die das beantwortet.
 *
 *  2. **Fehler und unbestätigte Punkte bleiben sichtbar.** [RunVerdict] ist kein
 *     frei übergebener Wert, sondern wird aus Tests, Fehlern, Abbruchgrund,
 *     unbestätigten Punkten und dem erreichten [ChangeLevel] **berechnet**. Ein
 *     ungeklärter Nebeneffekt ([AgentRunStatus.UNCERTAIN_SIDE_EFFECT]) führt damit
 *     zwangsläufig zu [RunVerdict.UNCERTAIN] und nie zu „abgeschlossen“. Dasselbe
 *     gilt für einen Testlauf, dessen Ergebnis nicht ausgewertet wurde: er zählt
 *     nicht als bestandener Test.
 *
 *  3. **Keine Schlüssel und keine vertraulichen Ausgaben im Bericht.** Jeder frei
 *     kommende Text — Pfade nicht ausgenommen — läuft über [SecretMasker]. Der
 *     `init`-Block weist zusätzlich jeden Versuch zurück, ungeprüften Text in einen
 *     fertigen Bericht zu bringen; wer einen Schlüssel durchreicht, bekommt
 *     keinen Bericht, sondern eine Ausnahme.
 *
 * Reines Kotlin: keine Datei, kein Netzwerk, kein Anbieter. Der Bericht beschreibt,
 * was passiert ist; er führt nichts aus.
 */

/**
 * Wie weit ist eine Änderung tatsächlich gekommen?
 *
 * Die Reihenfolge der Werte ist zugleich die aufsteigende Stärke der Zusage:
 * weiter unten bedeutet mehr, was der Nutzer nach einem Absturz wiederfindet.
 */
enum class ChangeLevel(val germanLabel: String) {

    /** Nur vorgeschlagen. Auf dem Gerät ist nichts davon angekommen. */
    PROPOSED("nur vorgeschlagen, nichts davon ist gespeichert"),

    /** Es stand einmal etwas da und wurde wieder zurückgenommen. */
    DISCARDED("wieder verworfen, nichts davon ist geblieben"),

    /** Geändert, aber nicht gesichert — nach einem Absturz weg. */
    CHANGED_UNSAVED("geändert, aber nicht gesichert"),

    /** Es steht nicht fest, ob überhaupt geschrieben wurde. */
    UNCERTAIN("ungeklärt, ob geschrieben wurde"),

    /** Liegt dauerhaft auf dem Gerät. */
    SAVED("lokal gespeichert, nicht hochgeladen"),

    /** Liegt dauerhaft auf dem Gerät und im entfernten Repository. */
    PUSHED("lokal gespeichert und hochgeladen");

    /** Ist der Zustand so weit, dass ein Absturz nichts mehr kostet? */
    val isPersisted: Boolean
        get() = this == SAVED || this == PUSHED

    /** Wurde etwas tatsächlich geschrieben — auch wenn es wieder verworfen wurde? */
    val hasBeenWritten: Boolean
        get() = this != PROPOSED

    companion object {

        /** Die Stufen von schwach nach stark. */
        private val STRENGTH: List<ChangeLevel> = listOf(
            PROPOSED, DISCARDED, CHANGED_UNSAVED, UNCERTAIN, SAVED, PUSHED
        )

        /**
         * Die schwächste Stufe der Liste, oder `null` bei leerer Liste.
         *
         * Bewusst „schwächste“ und nicht „stärkste“: Ein Lauf ist so weit wie seine
         * schlechteste Änderung, nicht wie seine beste. [UNCERTAIN] steht bewusst
         * über [SAVED] — solange eine Nebenwirkung ungeklärt ist, darf der ganze
         * Lauf nicht als gesichert gelten.
         */
        fun weakestOf(levels: Iterable<ChangeLevel>): ChangeLevel? =
            levels.minByOrNull { STRENGTH.indexOf(it) }

        /** Erreicht [level] mindestens [minimum]? */
        fun isAtLeast(level: ChangeLevel, minimum: ChangeLevel): Boolean =
            STRENGTH.indexOf(level) >= STRENGTH.indexOf(minimum)
    }
}

/**
 * Wie ein Testlauf ausgegangen ist.
 *
 * [RAN_UNVERIFIED] ist der ehrliche Sonderfall: Der Befehl ist gelaufen, aber
 * niemand hat die Ausgabe gelesen. Er wird nicht als Erfolg gezählt — ein
 * ausgeführter Befehl ist noch kein bestandener Test.
 */
enum class TestOutcome(val germanLabel: String) {

    /** Die Ausgabe wurde geprüft und war erfolgreich. */
    PASSED("bestanden"),

    /** Die Ausgabe wurde geprüft und war fehlerhaft. */
    FAILED("fehlgeschlagen"),

    /** Der Befehl lief, das Ergebnis wurde aber nicht ausgewertet. */
    RAN_UNVERIFIED("ausgeführt, Ergebnis nicht ausgewertet"),

    /** Der Befehl lief nicht. */
    NOT_RUN("nicht ausgeführt");

    /** Belegt dieser Zustand einen bestandenen Test? */
    val provesSuccess: Boolean get() = this == PASSED
}

/**
 * Ein einzelner Testlauf im Bericht.
 *
 * Der `init`-Block macht die Zusage „Teststatus korrekt ausweisen“ strukturell:
 * [TestOutcome.PASSED] und [TestOutcome.FAILED] **müssen** Zahlen tragen, und die
 * beiden Zustände ohne Auswertung **dürfen keine** tragen. Es gibt keinen Weg,
 * einen bestandenen Test zu melden, ohne dass benannt ist, wie viele Tests liefen.
 */
data class ReportedTest(
    val taskName: String,
    val command: String,
    val outcome: TestOutcome,
    val totalCount: Int = 0,
    val failedCount: Int = 0
) {
    init {
        require(taskName.isNotBlank()) { "Ein Testlauf braucht einen Namen." }
        val carriesCounts = totalCount > 0 || failedCount > 0
        require(outcome.provesSuccess || outcome == TestOutcome.FAILED || !carriesCounts) {
            "Ein Testlauf ohne Auswertung darf keine Testergebnisse melden: $taskName"
        }
        require(outcome != TestOutcome.PASSED || (totalCount > 0 && failedCount == 0)) {
            "Ein bestandener Testlauf braucht mindestens einen Test und keinen Fehlschlag: $taskName"
        }
        require(outcome != TestOutcome.FAILED || failedCount > 0) {
            "Ein fehlgeschlagener Testlauf ohne Fehlschlag ist widersprüchlich: $taskName"
        }
        require(failedCount <= totalCount) {
            "Fehlschläge ($failedCount) können nicht mehr sein als Tests ($totalCount): $taskName"
        }
    }

    /** Die eine Zeile, die der Nutzer zu diesem Testlauf sieht. */
    fun reportLine(): String {
        val head = "$taskName — ${outcome.germanLabel}"
        return if (totalCount > 0) {
            "$head: ${totalCount - failedCount} von $totalCount bestanden"
        } else {
            head
        }
    }
}

/** Eine einzelne geänderte Datei und wie weit sie gekommen ist. */
data class FileChangeReport(
    val path: String,
    val level: ChangeLevel,
    val stepId: String = ""
) {
    init {
        require(path.isNotBlank()) { "Eine Änderung braucht einen Pfad." }
    }

    /** Die Zeile für den Bericht. */
    fun reportLine(): String = if (stepId.isBlank()) {
        "$path — ${level.germanLabel}"
    } else {
        "$path — ${level.germanLabel} (Schritt $stepId)"
    }
}

/** Wie ein Lauf insgesamt ausgegangen ist. */
enum class RunVerdict(val germanLabel: String) {

    /** Alles verlangte ist erreicht. */
    COMPLETED("abgeschlossen"),

    /** Ein Teil hat funktioniert, ein Teil fehlt. */
    PARTIAL("teilweise erledigt"),

    /** Der Lauf ist fehlgeschlagen. */
    FAILED("fehlgeschlagen"),

    /** Der Nutzer hat abgebrochen. */
    ABORTED("abgebrochen"),

    /** Ob eine Nebenwirkung eintrat, steht nicht fest. */
    UNCERTAIN("Nebenwirkung ungeklärt")
}

/**
 * Der Abschlussbericht eines Agentenlaufs.
 *
 * @property runId Kennung des Laufs, für den der Bericht steht.
 * @property changes jede einzelne geänderte Datei mit ihrer eigenen Stufe.
 * @property tests die gelaufenen Testaufgaben.
 * @property commands die ausgeführten Befehle, in Ausführungsreihenfolge.
 * @property errors Fehler, die passiert sind.
 * @property unconfirmedPoints Punkte, die niemand geprüft hat.
 * @property risks Risiken, die der Nutzer kennen sollte.
 * @property nextOptions das, was als Nächstes möglich ist.
 * @property abortReason der Stoppgrund aus [AgentToolLoop], falls einer vorlag.
 * @property unexecutedToolNames Werkzeugaufrufe, die nach dem Stopp nicht liefen.
 * @property expectedTestCount wie viele Testaufgaben der Auftrag vorsah; `0`
 *           heißt „der Auftrag sah keine Tests vor“.
 * @property expectedChangeLevel die Stufe, die der Auftrag verlangt hat.
 * @property unresolvedSideEffect `true`, wenn ein Schritt mit ungeklärter
 *           Nebenwirkung endete, **ohne** dass eine Datei bekannt ist, die man
 *           nennen könnte. Solange das gilt, ist die erreichte Stufe des Laufs
 *           [ChangeLevel.UNCERTAIN] — auch wenn jede *bekannte* Datei gesichert ist.
 *           Genau dieser Fall entsteht, wenn nach einem Abbruch nichts Genaueres
 *           aufgeschrieben wurde, und ohne dieses Feld würde der Bericht „lokal
 *           gespeichert“ sagen, obwohl möglicherweise geschrieben wurde.
 */
data class AgentResultSummary(
    val runId: String,
    val changes: List<FileChangeReport> = emptyList(),
    val tests: List<ReportedTest> = emptyList(),
    val commands: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val unconfirmedPoints: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val nextOptions: List<String> = emptyList(),
    val abortReason: StopReason? = null,
    val unexecutedToolNames: List<String> = emptyList(),
    val expectedTestCount: Int = 0,
    val expectedChangeLevel: ChangeLevel = ChangeLevel.SAVED,
    val unresolvedSideEffect: Boolean = false
) {

    init {
        require(runId.isNotBlank()) { "Ein Bericht braucht die Kennung des Laufs." }
        require(expectedTestCount >= 0) { "Die erwartete Testanzahl kann nicht negativ sein." }
        require(
            noFieldCarriesSecret(
                listOf(runId),
                changes.flatMap { listOf(it.path, it.stepId) },
                tests.flatMap { listOf(it.taskName, it.command) },
                commands,
                errors,
                unconfirmedPoints,
                risks,
                nextOptions,
                unexecutedToolNames
            )
        ) {
            "Ein Bericht darf keinen Schlüssel und keine vertrauliche Ausgabe enthalten. " +
                "Bitte AgentResultSummary.build verwenden."
        }
    }

    // ── Was wirklich erreicht wurde ──────────────────────────────────────────

    /**
     * Die Stufe des ganzen Laufs: die **schwächste** aller Änderungen.
     *
     * Eine ungeklärte Nebenwirkung ohne bekannte Datei schlägt jede bekannte Stufe:
     * Solange nicht feststeht, ob geschrieben wurde, ist „gespeichert“ eine
     * Behauptung, die niemand geprüft hat.
     *
     * `null`, wenn keine Datei geändert wurde und nichts ungeklärt ist — dann gibt
     * es nichts zu behaupten, und der Bericht sagt das auch.
     */
    val changeLevel: ChangeLevel?
        get() = when {
            unresolvedSideEffect -> ChangeLevel.UNCERTAIN
            else -> ChangeLevel.weakestOf(changes.map { it.level })
        }

    /**
     * Hat der Lauf mindestens diese Stufe erreicht?
     *
     * Das ist die einzige Stelle, die eine Stufe bejaht. Sie ist für **jede**
     * Änderung wahr, nicht für die beste — deshalb kann der Bericht „hochgeladen“
     * nur sagen, wenn es wirklich jede Datei ist.
     */
    fun hasReached(level: ChangeLevel): Boolean {
        val reached = changeLevel ?: return level == ChangeLevel.PROPOSED
        return ChangeLevel.isAtLeast(reached, level)
    }

    /** Wurde überhaupt etwas geschrieben? */
    val hasAnyChange: Boolean get() = changes.any { it.level.hasBeenWritten }

    /** Liegt eine Datei im entfernten Repository? */
    val pushedPaths: List<String>
        get() = changes.filter { it.level == ChangeLevel.PUSHED }.map { it.path }

    /** Liegt eine Datei nur lokal? */
    val savedButNotPushedPaths: List<String>
        get() = changes.filter { it.level == ChangeLevel.SAVED }.map { it.path }

    /** Ist eine Datei nach einem Absturz verloren? */
    val lostOnRestartPaths: List<String>
        get() = changes
            .filter { it.level == ChangeLevel.CHANGED_UNSAVED || it.level == ChangeLevel.UNCERTAIN }
            .map { it.path }

    // ── Testlage ─────────────────────────────────────────────────────────────

    /** Die Zahl der Tests, die als bestanden belegt sind. */
    val provenPassedTests: Int
        get() = tests.filter { it.outcome.provesSuccess }.sumOf { it.totalCount - it.failedCount }

    /**
     * Sind alle gelaufenen Tests nachweislich bestanden?
     *
     * Zwei Wege führen hier bewusst nach `false`: kein Test gelaufen, oder ein
     * gelaufener Test ohne ausgewertetes Ergebnis. Beides ist kein Beweis.
     */
    val allTestsPassed: Boolean
        get() = tests.isNotEmpty() && tests.all { it.outcome == TestOutcome.PASSED }

    /** Testaufgaben, die der Auftrag vorsah und die nicht vorkommen. */
    val missingTestCount: Int
        get() = (expectedTestCount - tests.size).coerceAtLeast(0)

    /**
     * Gibt es einen Test, der einen Erfolg verhindert?
     *
     * `NOT_RUN` in einer ohnehin erwarteten Liste blockiert, ebenso jedes Ergebnis,
     * das keinen bestandenen Test belegt.
     */
    val hasTestBlockingSuccess: Boolean
        get() = when {
            tests.any { it.outcome == TestOutcome.FAILED } -> true
            tests.any { it.outcome == TestOutcome.NOT_RUN } -> true
            tests.any { it.outcome == TestOutcome.RAN_UNVERIFIED } -> true
            tests.isEmpty() && expectedTestCount > 0 -> true
            else -> false
        }

    /** Ein Testlauf ohne ausgewertete Ausgabe ist keine Testaussage. */
    val hasUnverifiedTest: Boolean
        get() = tests.any { it.outcome == TestOutcome.RAN_UNVERIFIED }

    // ── Gesamturteil ─────────────────────────────────────────────────────────

    /**
     * Das Urteil über den Lauf.
     *
     * Es wird gerechnet, nicht angegeben. Die Reihenfolge ist Absicht: Ein Abbruch
     * durch den Nutzer wird nicht als Fehler umgedeutet, ein ungeklärter
     * Nebeneffekt schlägt jede Teilerfolgsmeldung, und erst danach entscheidet die
     * Testlage.
     */
    val verdict: RunVerdict
        get() = when {
            abortReason == StopReason.ABORTED_BY_USER -> RunVerdict.ABORTED
            errors.isNotEmpty() -> RunVerdict.FAILED
            abortReason == StopReason.REFUSED_BY_GATE || abortReason == StopReason.TOOL_FAILED ->
                RunVerdict.FAILED
            tests.any { it.outcome == TestOutcome.FAILED } -> RunVerdict.FAILED
            unconfirmedPoints.isNotEmpty() || changeLevel == ChangeLevel.UNCERTAIN ->
                RunVerdict.UNCERTAIN
            abortReason != null -> RunVerdict.PARTIAL
            unexecutedToolNames.isNotEmpty() -> RunVerdict.PARTIAL
            hasTestBlockingSuccess -> RunVerdict.PARTIAL
            missingTestCount > 0 -> RunVerdict.PARTIAL
            !meetsExpectation -> RunVerdict.PARTIAL
            else -> RunVerdict.COMPLETED
        }

    /** Darf dieser Lauf dem Nutzer als Erfolg gelten? */
    val isSuccess: Boolean get() = verdict == RunVerdict.COMPLETED

    /**
     * Ist die vom Auftrag verlangte Stufe erreicht?
     *
     * Bei *keiner* Änderung ist nur [ChangeLevel.PROPOSED] erfüllt: Wer einen Push
     * verlangt hat und keine Datei geändert bekam, hat nichts erreicht.
     */
    val meetsExpectation: Boolean
        get() = hasReached(expectedChangeLevel)

    /** Die eine Überschriftzeile für die Oberfläche. */
    fun headline(): String = when (verdict) {
        RunVerdict.COMPLETED -> "Ergebnis: abgeschlossen. ${changeSentence()}"
        RunVerdict.PARTIAL -> "Ergebnis: teilweise erledigt. ${changeSentence()}"
        RunVerdict.FAILED -> "Ergebnis: fehlgeschlagen. ${changeSentence()}"
        RunVerdict.ABORTED -> "Ergebnis: abgebrochen. ${changeSentence()}"
        RunVerdict.UNCERTAIN ->
            "Ergebnis: Nebenwirkung ungeklärt. ${changeSentence()}"
    }

    /** Der Satz, der Vorschlag, Speicherung und Upload unterscheidet. */
    fun changeSentence(): String = when (val level = changeLevel) {
        null -> "Es wurde keine Datei geändert."
        ChangeLevel.PROPOSED -> "Es liegt nur ein Vorschlag vor, es wurde nichts gespeichert."
        ChangeLevel.DISCARDED -> "Die Änderung wurde verworfen, es ist nichts davon geblieben."
        ChangeLevel.CHANGED_UNSAVED ->
            "Die Änderung liegt nur im Arbeitsspeicher. Nach einem Appstart ist sie weg."
        ChangeLevel.UNCERTAIN ->
            "Von mindestens einer Änderung steht nicht fest, ob sie geschrieben wurde."
        ChangeLevel.SAVED -> "Die Änderung ist lokal gespeichert, aber nicht hochgeladen."
        ChangeLevel.PUSHED -> "Die Änderung ist lokal gespeichert und hochgeladen."
    }

    /**
     * Der vollständige Bericht.
     *
     * Reihenfolge ist die, in der ein Mensch die Fragen liest: Was ist passiert,
     * welche Dateien, welche Tests, welche Fehler, was ist offen, was kann man tun.
     * Fehler und unbestätigte Punkte stehen **vor** den nächsten Möglichkeiten, damit
     * sie beim Scrollen nicht aus dem Blick geraten.
     */
    fun reportLines(): List<String> = buildList {
        add("Abschlussbericht für Lauf $runId")
        add(headline())

        add("")
        add("Geänderte Dateien (${changes.size}):")
        if (changes.isEmpty()) {
            add("  Keine Datei wurde geändert.")
        } else {
            changes.forEach { add("  ${it.reportLine()}") }
            if (savedButNotPushedPaths.isNotEmpty()) {
                add("  Lokal gespeichert, nicht hochgeladen: ${savedButNotPushedPaths.joinToString(", ")}")
            }
        }

        add("")
        add("Tests:")
        if (tests.isEmpty()) {
            add(
                if (expectedTestCount > 0) {
                    "  Es lief kein Test, der Auftrag sah $expectedTestCount vor."
                } else {
                    "  Der Auftrag sah keine Tests vor; es wurde keiner ausgeführt."
                }
            )
        } else {
            tests.forEach { add("  ${it.reportLine()}") }
            if (hasUnverifiedTest) {
                add(
                    "  Mindestens ein Testlauf wurde nicht ausgewertet. Er zählt hier " +
                        "nicht als bestandener Test."
                )
            }
        }

        add("")
        add("Ausgeführte Befehle (${commands.size}):")
        if (commands.isEmpty()) {
            add("  Kein Befehl wurde ausgeführt.")
        } else {
            commands.forEach { add("  $it") }
        }

        if (errors.isNotEmpty()) {
            add("")
            add("Fehler (${errors.size}):")
            errors.forEach { add("  - $it") }
        }

        if (unconfirmedPoints.isNotEmpty()) {
            add("")
            add("Unbestätigte Punkte (${unconfirmedPoints.size}):")
            unconfirmedPoints.forEach { add("  - $it") }
        }

        if (risks.isNotEmpty()) {
            add("")
            add("Risiken (${risks.size}):")
            risks.forEach { add("  - $it") }
        }

        if (lostOnRestartPaths.isNotEmpty()) {
            add("")
            add(
                "Nach einem Appstart gehen verloren: ${lostOnRestartPaths.joinToString(", ")}"
            )
        }

        if (abortReason != null) {
            add("")
            add("Stoppgrund: ${abortReason.germanLabel}.")
        }
        if (unexecutedToolNames.isNotEmpty()) {
            add("Nicht ausgeführt: ${unexecutedToolNames.joinToString(", ")}")
        }

        if (nextOptions.isNotEmpty()) {
            add("")
            add("Als Nächstes möglich:")
            nextOptions.forEach { add("  - $it") }
        }

        if (expectedChangeLevel != ChangeLevel.PROPOSED && !meetsExpectation) {
            add("")
            add(
                "Der Auftrag verlangte mindestens „${expectedChangeLevel.germanLabel}“. " +
                    "Das ist nicht erreicht."
            )
        }
    }

    /** Kurzfassung für eine Benachrichtigung; enthält keine Detailzeilen. */
    fun shortReport(): String = buildString {
        append(headline())
        if (errors.isNotEmpty()) {
            append(" ${errors.size} Fehler.")
        }
        if (unconfirmedPoints.isNotEmpty()) {
            append(" ${unconfirmedPoints.size} unbestätigte Punkte.")
        }
    }

    companion object {

        /** Der Name des Werkzeugs, das Tests ausführt. */
        const val TEST_TOOL_NAME: String = "run_test"

        /**
         * Baut einen Bericht und schwärzt dabei jeden Text.
         *
         * Das ist der einzige vorgesehene Weg, Texte von außen einzuspeisen. Der
         * `init`-Block des Berichts weist ungeprüften Text ab, damit hier nicht
         * versehentlich etwas durchfällt.
         */
        fun build(
            runId: String,
            changes: List<FileChangeReport> = emptyList(),
            tests: List<ReportedTest> = emptyList(),
            commands: List<String> = emptyList(),
            errors: List<String> = emptyList(),
            unconfirmedPoints: List<String> = emptyList(),
            risks: List<String> = emptyList(),
            nextOptions: List<String> = emptyList(),
            abortReason: StopReason? = null,
            unexecutedToolNames: List<String> = emptyList(),
            expectedTestCount: Int = 0,
            expectedChangeLevel: ChangeLevel = ChangeLevel.SAVED,
            unresolvedSideEffect: Boolean = false
        ): AgentResultSummary = AgentResultSummary(
            runId = safe(runId),
            changes = changes.map {
                it.copy(path = safe(it.path), stepId = safe(it.stepId))
            },
            tests = tests.map {
                it.copy(taskName = safe(it.taskName), command = safe(it.command))
            },
            commands = commands.map { safe(it) },
            errors = errors.map { safe(it) },
            unconfirmedPoints = unconfirmedPoints.map { safe(it) },
            risks = risks.map { safe(it) },
            nextOptions = nextOptions.map { safe(it) },
            abortReason = abortReason,
            unexecutedToolNames = unexecutedToolNames.map { safe(it) },
            expectedTestCount = expectedTestCount,
            expectedChangeLevel = expectedChangeLevel,
            unresolvedSideEffect = unresolvedSideEffect
        )

        /**
         * Die Stufe, die ein gespeicherter Schrittstatus über einen Pfad sagt.
         *
         * Ein Schritt mit ungeklärter Nebenwirkung ist [ChangeLevel.UNCERTAIN] —
         * vor allem Sicherungszustand, denn „lokal gesichert“ wäre hier eine
         * Behauptung, die niemand geprüft hat.
         */
        fun levelFor(record: AgentStepRecord): ChangeLevel = when {
            record.status == AgentRunStatus.UNCERTAIN_SIDE_EFFECT -> ChangeLevel.UNCERTAIN
            else -> when (record.commitState) {
                CommitState.NOT_APPLICABLE -> ChangeLevel.PROPOSED
                CommitState.NOT_SAVED -> ChangeLevel.CHANGED_UNSAVED
                CommitState.SAVED_LOCALLY -> ChangeLevel.SAVED
                CommitState.SAVED_AND_PUSHED -> ChangeLevel.PUSHED
                CommitState.DISCARDED -> ChangeLevel.DISCARDED
            }
        }

        /**
         * Baut den Bericht aus dem gespeicherten Lauf und dem Werkzeuglauf.
         *
         * Die Testergebnisse werden **nicht** aus der Werkzeugausgabe geraten: Ein
         * `run_test`, der abgeschlossen ist, hat einen Befehl ausgeführt, und das ist
         * kein Beweis für bestandene Tests. Der Aufrufer reicht sie deshalb als
         * [tests] ein — nach eigener Auswertung. Ohne sie bleibt der Bericht bei
         * „nicht ausgewertet“ und zählt den Test nicht.
         */
        fun fromRun(
            state: AgentRunState,
            toolRun: ToolLoopRun? = null,
            tests: List<ReportedTest> = emptyList(),
            risks: List<String> = emptyList(),
            nextOptions: List<String> = emptyList(),
            expectedTestCount: Int = 0,
            expectedChangeLevel: ChangeLevel = ChangeLevel.SAVED
        ): AgentResultSummary {
            // Ein gelaufener Testlauf, den niemand ausgewertet hat, darf nicht
            // einfach wegfallen: Sonst wuerde ein Lauf mit Test und ohne Auswertung
            // wie ein Lauf ganz ohne Tests erscheinen — und bei
            // `expectedTestCount = 0` sogar als abgeschlossen. Der fehlende
            // Nachweis wird hier ausdruecklich als [TestOutcome.RAN_UNVERIFIED]
            // eingetragen, damit er den Erfolg blockiert.
            val unverified = toolRun?.results.orEmpty()
                .filter { it.toolName == TEST_TOOL_NAME }
                .map { result ->
                    ReportedTest(
                        taskName = TEST_TOOL_NAME,
                        command = AgentToolCatalog.commandFor(TEST_TOOL_NAME) ?: TEST_TOOL_NAME,
                        outcome = TestOutcome.RAN_UNVERIFIED
                    )
                }
            val effectiveTests = tests + unverified
            return fromRunWithTests(
                state = state,
                toolRun = toolRun,
                tests = effectiveTests,
                risks = risks,
                nextOptions = nextOptions,
                expectedTestCount = expectedTestCount,
                expectedChangeLevel = expectedChangeLevel
            )
        }

        private fun fromRunWithTests(
            state: AgentRunState,
            toolRun: ToolLoopRun?,
            tests: List<ReportedTest>,
            risks: List<String>,
            nextOptions: List<String>,
            expectedTestCount: Int,
            expectedChangeLevel: ChangeLevel
        ): AgentResultSummary = build(
            runId = state.runId,
            changes = state.steps
                .flatMap { step ->
                    step.touchedPaths.map { path ->
                        FileChangeReport(path, levelFor(step), step.stepId)
                    }
                },
            tests = tests,
            commands = tests.map { it.command }.filter { it.isNotBlank() }.distinct(),
            errors = state.steps
                .filter { it.status == AgentRunStatus.FAILED }
                .map { step -> "Schritt ${step.stepId}: ${step.note.ifBlank { "ohne Angabe" }}" },
            unconfirmedPoints = state.stepsNeedingReview()
                .map { step -> "Schritt ${step.stepId}: ${step.status.germanLabel}" } +
                toolRunSideEffectNotes(toolRun),
            risks = risks,
            nextOptions = nextOptions,
            abortReason = toolRun?.stopReason?.takeIf { it == StopReason.ABORTED_BY_USER },
            unexecutedToolNames = toolRun?.notExecuted?.map { it.name }.orEmpty(),
            expectedTestCount = expectedTestCount,
            expectedChangeLevel = expectedChangeLevel,
            // Abgeleitet, nicht übergeben: Der Aufrufer kann eine ungeklärte
            // Nebenwirkung nicht stillschweigend als „geklärt“ deklarieren.
            unresolvedSideEffect = state.steps
                .any { it.status == AgentRunStatus.UNCERTAIN_SIDE_EFFECT }
        )

        /**
         * Was ein Werkzeuglauf an Ungeklärtem hinterlässt.
         *
         * Ein Lauf, der nur wegen [StopReason.CALL_LIMIT_EXCEEDED] oder
         * [StopReason.NO_CALLS] endete, hat nicht behauptet, etwas zu ändern; er
         * gehört deshalb nicht in die unbestätigten Punkte. Ein Abbruch durch den
         * Nutzer dagegen schon — was dabei geschah, ist offen.
         */
        private fun toolRunSideEffectNotes(toolRun: ToolLoopRun?): List<String> {
            if (toolRun == null) return emptyList()
            if (toolRun.stopReason != StopReason.ABORTED_BY_USER) return emptyList()
            return listOf(
                "Der Nutzer hat abgebrochen. Ob dabei noch eine Änderung geschrieben " +
                    "wurde, steht nicht fest."
            ) + toolRun.sideEffects.map { it.germanNote }
        }

        /** Jeder Text von außen läuft hier durch. */
        private fun safe(text: String): String = SecretMasker.redact(text)

        /** Prüft alle Textfelder des Berichts auf Schlüssel. */
        private fun noFieldCarriesSecret(vararg groups: List<String>): Boolean =
            groups.flatMap { it }.none { SecretMasker.containsSecretLikeText(it) }
    }
}
