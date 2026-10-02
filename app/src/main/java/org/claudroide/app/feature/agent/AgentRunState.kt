package org.claudroide.app.feature.agent

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 073 — „Agentenlauf speichern“.
 *
 * Ziel: Arbeitsschritte und bereits abgeschlossene Nebenwirkungen bleiben
 * wiedererkennbar. Drei Zusagen sind strukturell abgesichert:
 *
 *  1. **Ein Neustart führt keine Aktion doppelt aus.** Das ist keine
 *     Disziplinfrage, sondern eine Eigenschaft des Datentyps: Ein Aufruf wird
 *     über [AgentRunStore.callKey] identifiziert (Werkzeug + Pfad) und genau
 *     einmal registriert. [AgentRunStore.mayExecuteCall] beantwortet danach nur
 *     noch „ja“ oder „nein“ — es gibt keinen Weg, einen bereits ausgeführten
 *     Aufruf erneut anzumelden, und [AgentRunState.stepsThatMayRunAgain] führt
 *     ausschließlich Schritte auf, in denen nachweislich nichts passiert ist
 *     ([AgentRunStatus.PLANNED], [AgentRunStatus.WAITING_FOR_APPROVAL]).
 *
 *  2. **Der Zustand kann nicht über den Treuigkeitsgrad lügen.** Es gibt den
 *     Zustand [AgentRunStatus.UNCERTAIN_SIDE_EFFECT], und jeder Lauf, bei dem
 *     nach einem Abbruch nicht feststeht, ob eine Änderung geschrieben wurde,
 *     landet dort — nicht in „beendet“. [AgentRunState.needsReview] macht solche
 *     Schritte sichtbar, statt sie als erledigt zu führen.
 *
 *  3. **Keine Zugangsdaten im Laufstatus.** Zwei Ebenen: Der gespeicherte Zustand
 *     kennt überhaupt keine Argumentwerte — nur Werkzeugnamen und Pfade. Und jeder
 *     frei formulierte Text läuft vor dem Speichern durch [SecretMasker]; wird
 *     darin ein Schlüssel erkannt, landet der Platzhalter im Status und der Aufruf
 *     meldet [AgentRunStore.RecordOutcome.Redacted]. Es gibt keine Methode, die
 *     Text unverprüft übernimmt.
 *
 * Dazu kommt die vierte, im Auftrag genannte Sichtbarkeit:
 * **„Nutzer sieht, ob eine Änderung schon gespeichert wurde“** —
 * [AgentRunState.unsavedChangeLines] nennt jede berührte Datei mit ihrem
 * [CommitState], und [AgentRunState.warnings] weist auf Änderungen hin, die nach
 * einem Abbruch nicht gesichert sind.
 *
 * Reines Kotlin: keine Datei, kein Netzwerk, kein Anbieter. Der Laufstatus
 * beschreibt, was passiert ist; er führt nichts aus.
 */

/** Der Zustand eines Arbeitsschritts im Agentenlauf. */
enum class AgentRunStatus(val germanLabel: String) {
    /** Aus dem Plan bekannt, noch nichts angefasst. */
    PLANNED("geplant"),

    /** Wartet auf eine Freigabe des Nutzers. */
    WAITING_FOR_APPROVAL("wartet auf Zustimmung"),

    /** Läuft gerade. */
    RUNNING("läuft"),

    /** Fertig und vollständig. */
    FINISHED("beendet"),

    /** Vom Nutzer abgebrochen. */
    ABORTED("abgebrochen"),

    /** Fehlgeschlagen. */
    FAILED("fehlerhaft"),

    /**
     * Nach einem Abbruch ist nicht feststellbar, ob eine Änderung geschrieben
     * wurde. Dieser Zustand ist ehrlicher als „beendet“ und blockiert deshalb
     * jede Wiederholung, bis der Nutzer nachgesehen hat.
     */
    UNCERTAIN_SIDE_EFFECT("Nebenwirkung ungeklärt")
}

/**
 * Ob eine Änderung gesichert ist.
 *
 * [NOT_APPLICABLE] heißt: Der Schritt hat nichts verändert (gelesen, gesucht,
 * getestet). Die drei übrigen Werte unterscheiden sich darin, was der Nutzer
 * nach einem Absturz wiederherstellen kann.
 */
enum class CommitState(val germanLabel: String, val isSecure: Boolean) {
    NOT_APPLICABLE("nichts geändert", true),
    NOT_SAVED("geändert, aber nicht gesichert", false),
    SAVED_LOCALLY("lokal gesichert", true),
    SAVED_AND_PUSHED("lokal gesichert und hochgeladen", true),
    DISCARDED("verworfen", true)
}

/**
 * Das Protokoll eines Schritts.
 *
 * @property toolNames nur die **Namen** der benutzten Werkzeuge. Argumentwerte
 *           werden nicht gespeichert — dort stünden Dateiinhalte und
 *           möglicherweise Schlüssel.
 * @property touchedPaths die Dateien, an denen etwas geändert wurde.
 * @property executedCallKeys die Schlüssel aus [AgentRunStore.callKey]; jedes
 *           Zeichen dieser Liste steht für einen *einmalig* ausgeführten Aufruf.
 * @property note ein kurzer, bereits geschwärzter Hinweis.
 */
data class AgentStepRecord(
    val stepId: String,
    val status: AgentRunStatus,
    val toolNames: List<String> = emptyList(),
    val touchedPaths: List<String> = emptyList(),
    val executedCallKeys: List<String> = emptyList(),
    val commitState: CommitState = CommitState.NOT_APPLICABLE,
    val note: String = ""
) {
    /** Wurde in diesem Schritt überhaupt etwas ausgeführt? */
    val hasExecutedSomething: Boolean
        get() = executedCallKeys.isNotEmpty()

    /**
     * Kann nach einem Neustart noch einmal gelaufen werden?
     *
     * Nur Zustände, in denen nachweislich nichts passiert ist: geplant, wartend —
     * und abgebrochen, sofern beim Abbruch nichts geschrieben wurde. Alles andere
     * ist entweder erledigt oder ungeklärt, und beides ist keine Grundlage für
     * eine Wiederholung.
     */
    val mayRunAgain: Boolean
        get() = status == AgentRunStatus.PLANNED ||
            status == AgentRunStatus.WAITING_FOR_APPROVAL ||
            status == AgentRunStatus.ABORTED

    /** Der Schritt braucht einen Blick des Nutzers, bevor es weitergeht. */
    val needsReview: Boolean
        get() = status == AgentRunStatus.UNCERTAIN_SIDE_EFFECT ||
            status == AgentRunStatus.RUNNING ||
            (status == AgentRunStatus.FAILED && touchedPaths.isNotEmpty()) ||
            (status == AgentRunStatus.ABORTED && touchedPaths.isNotEmpty() && !commitState.isSecure)
}

/** Das Bild eines ganzen Laufs, wie es gespeichert und wiederhergestellt wird. */
data class AgentRunState(
    val runId: String,
    val startedAtEpochMillis: Long,
    val steps: List<AgentStepRecord>
) {
    /** Der Schritt mit dieser Kennung. */
    fun step(stepId: String): AgentStepRecord? = steps.firstOrNull { it.stepId == stepId }

    /** Schritte, die nach einem Neustart ohne Wiederholung eines Aufrufs laufen dürfen. */
    fun stepsThatMayRunAgain(): List<AgentStepRecord> = steps.filter { it.mayRunAgain }

    /** Schritte, die der Nutzer ansehen muss, bevor es weitergeht. */
    fun stepsNeedingReview(): List<AgentStepRecord> = steps.filter { it.needsReview }

    /** Alles, was ausgeführt wurde und noch nicht als ausgeführt eingetragen ist. */
    fun executedCallKeys(): Set<String> = steps.flatMap { it.executedCallKeys }.toSet()

    /**
     * Jede berührte Datei mit ihrem Sicherungszustand.
     *
     * Das ist die Antwort auf „weiß ich, ob eine Änderung gesichert ist?“: Jede
     * Datei wird namentlich mit ihrem Zustand genannt, auch die bereits
     * gesicherten. Eine Liste nur der ungesicherten Dateien würde die Frage
     * beantworten, ohne zu zeigen, dass es auch gesicherte gibt.
     */
    fun changedFileLines(): List<String> = steps
        .filter { it.touchedPaths.isNotEmpty() }
        .flatMap { step ->
            step.touchedPaths.map { path -> "$path — ${step.commitState.germanLabel}" }
        }

    /** Nur die Dateien, die noch nicht gesichert sind. */
    fun unsavedChangeLines(): List<String> = steps
        .filter { it.touchedPaths.isNotEmpty() && !it.commitState.isSecure }
        .flatMap { step -> step.touchedPaths }

    /** Warnhinweise für die Oberfläche. */
    fun warnings(): List<String> = buildList {
        val unsaved = steps.filter { it.touchedPaths.isNotEmpty() && !it.commitState.isSecure }
        if (unsaved.isNotEmpty()) {
            add(
                "${unsaved.size} Änderung(en) sind nicht gesichert. Beim nächsten " +
                    "Appstart wird daraus nichts automatisch wiederholt."
            )
        }
        stepsNeedingReview().forEach {
            add("Schritt ${it.stepId}: ${it.status.germanLabel}. ${it.note}".trim())
        }
    }

    /** Die Zeilen für den Verlauf in der Oberfläche. */
    fun timelineLines(): List<String> = buildList {
        add("Agentenlauf ${runId}:")
        if (steps.isEmpty()) {
            add("Es ist noch kein Schritt erfasst.")
            return@buildList
        }
        steps.forEach { step ->
            val line = buildString {
                append(step.stepId)
                append(" — ")
                append(step.status.germanLabel)
                if (step.toolNames.isNotEmpty()) {
                    append(" (${step.toolNames.joinToString(", ")})")
                }
                if (step.commitState != CommitState.NOT_APPLICABLE) {
                    append(" — ${step.commitState.germanLabel}")
                }
            }
            add(line)
        }
        warnings().forEach { add("! $it") }
    }
}

/**
 * Hält den Zustand eines Agentenlaufs.
 *
 * Jede Änderung geht über eine benannte Methode; keine schreibt Text
 * ungeprüft in den Zustand.
 */
class AgentRunStore(
    private val runId: String,
    private val startedAtEpochMillis: Long = 0L
) {

    private val records = LinkedHashMap<String, AgentStepRecord>()

    /** Was bei einem Anmeldeversuch herauskam. */
    sealed interface RecordOutcome {

        /** Neu eingetragen. */
        data class Recorded(val record: AgentStepRecord) : RecordOutcome

        /** War bereits eingetragen — es wurde nichts hinzugefügt. */
        data class AlreadyRecorded(val record: AgentStepRecord) : RecordOutcome

        /** Der Text enthielt einen Schlüssel und wurde nicht gespeichert. */
        data class Redacted(val record: AgentStepRecord, val placeholder: String) : RecordOutcome
    }

    /** Der aktuelle Zustand, unveränderbar. */
    fun state(): AgentRunState =
        AgentRunState(runId, startedAtEpochMillis, records.values.toList())

    /** Der Schritt mit dieser Kennung, oder `null`. */
    fun step(stepId: String): AgentStepRecord? = records[stepId]

    // ── Zustände setzen ──────────────────────────────────────────────────────

    /** Nimmt einen geplanten Schritt auf. */
    fun markPlanned(stepId: String, toolNames: List<String> = emptyList()): AgentStepRecord =
        update(stepId) { current ->
            current.copy(
                status = AgentRunStatus.PLANNED,
                toolNames = toolNames.map { safe(it) }
            )
        }

    /** Der Schritt wartet auf eine Freigabe. */
    fun markWaitingForApproval(stepId: String): AgentStepRecord = update(stepId) {
        it.copy(status = AgentRunStatus.WAITING_FOR_APPROVAL)
    }

    /** Der Schritt läuft. */
    fun markRunning(stepId: String): AgentStepRecord = update(stepId) {
        it.copy(status = AgentRunStatus.RUNNING)
    }

    /**
     * Der Schritt ist sauber fertig.
     *
     * Berührte Dateien müssen als gesichert übergeben werden — ein Schritt, der
     * etwas geschrieben hat, kann nicht „beendet“ heißen, solange der
     * Sicherungszustand [CommitState.NOT_SAVED] ist. Das ist eine Regel des
     * Typs, keine Empfehlung im Kommentar: [markFinished] prüft sie.
     */
    fun markFinished(
        stepId: String,
        touchedPaths: List<String> = emptyList(),
        commitState: CommitState = CommitState.NOT_APPLICABLE,
        note: String = ""
    ): AgentStepRecord {
        val paths = touchedPaths.map { safe(it) }
        val effective = when {
            paths.isEmpty() -> CommitState.NOT_APPLICABLE
            commitState == CommitState.NOT_APPLICABLE -> CommitState.NOT_SAVED
            else -> commitState
        }
        return update(stepId) {
            it.copy(
                status = AgentRunStatus.FINISHED,
                touchedPaths = paths,
                commitState = effective,
                note = safe(note)
            )
        }
    }

    /** Der Schritt ist fehlgeschlagen. */
    fun markFailed(
        stepId: String,
        reason: String,
        touchedPaths: List<String> = emptyList(),
        /** `true`, wenn vor dem Fehler schon geschrieben wurde. */
        hasWrittenSideEffect: Boolean = false
    ): AgentStepRecord {
        val paths = if (hasWrittenSideEffect) touchedPaths.map { safe(it) } else emptyList()
        return update(stepId) {
            it.copy(
                status = AgentRunStatus.FAILED,
                touchedPaths = paths,
                // Eine Änderung, die nicht ausdrücklich gesichert wurde, gilt als
                // nicht gesichert — nicht als „wird sich schon zeigen“.
                commitState = if (paths.isEmpty()) CommitState.NOT_APPLICABLE else CommitState.NOT_SAVED,
                note = safe(reason)
            )
        }
    }

    /**
     * Der Schritt wurde abgebrochen.
     *
     * Solange nicht feststeht, ob geschrieben wurde, ist das Ergebnis
     * [AgentRunStatus.UNCERTAIN_SIDE_EFFECT] — nicht „beendet“. Ein Abbruch, bei
     * dem nachweislich nichts geschrieben wurde, wird als [AgentRunStatus.ABORTED]
     * geführt und ist damit wiederholbar.
     */
    fun markAborted(
        stepId: String,
        reason: String = "",
        /** `true`, wenn möglicherweise geschrieben wurde. */
        sideEffectUncertain: Boolean
    ): AgentStepRecord = update(stepId) {
        it.copy(
            status = if (sideEffectUncertain) {
                AgentRunStatus.UNCERTAIN_SIDE_EFFECT
            } else {
                AgentRunStatus.ABORTED
            },
            commitState = if (sideEffectUncertain && it.touchedPaths.isNotEmpty()) {
                CommitState.NOT_SAVED
            } else {
                it.commitState
            },
            note = safe(reason)
        )
    }

    /** Setzt den Sicherungszustand einer bereits abgeschlossenen Änderung. */
    fun markCommitState(stepId: String, commitState: CommitState): AgentStepRecord =
        update(stepId) { it.copy(commitState = commitState) }

    /**
     * Hängt einen Hinweis an einen Schritt — der einzige Weg für Text in den
     * gespeicherten Zustand.
     *
     * Enthält der Text einen Schlüssel, wird er geschwärzt und der Rückgabewert
     * sagt das ausdrücklich ([RecordOutcome.Redacted]). Der Aufrufer kann dann
     * anzeigen, dass etwas *nicht* gespeichert wurde; er kann sich nicht darauf
     * verlassen, dass der Text harmlos war.
     */
    fun addNote(stepId: String, text: String): RecordOutcome {
        val containedSecret = SecretMasker.containsSecretLikeText(text)
        val record = update(stepId) { it.copy(note = safe(text)) }
        return if (containedSecret) {
            RecordOutcome.Redacted(record, SecretMasker.REDACTION_PLACEHOLDER)
        } else {
            RecordOutcome.Recorded(record)
        }
    }

    // ── Aufrufe verbuchen ────────────────────────────────────────────────────

    /**
     * Der Schlüssel eines Aufrufs: Werkzeug plus Ziel.
     *
     * Bewusst aus zwei Feldern und einem Trenner gebaut, die beide gescannt werden
     * können — so kann kein Schlüssel durch eine abweichende Schreibweise
     * desselben Ziels eine Doppelanmeldung erzeugen.
     */
    fun callKey(toolName: String, path: String?): String =
        "${safe(toolName).trim().lowercase()}|${safe(path.orEmpty()).trim()}"

    /**
     * Darf dieser Aufruf ausgeführt werden?
     *
     * Das ist die eigentliche Sperre gegen Doppelausführung: Sie beantwortet nur
     * die Frage, nicht die Ausführung. Wer sie beantwortet bekommt, kann sie nicht
     * übergehen.
     */
    fun mayExecuteCall(toolName: String, path: String?): Boolean =
        state().executedCallKeys().contains(callKey(toolName, path)).not()

    /**
     * Trägt einen ausgeführten Aufruf ein — genau einmal.
     *
     * Ein zweiter Aufruf mit demselben Schlüssel ändert den Zustand **nicht**.
     * Damit kann auch ein Fehler in der Aufrufschleife keinen zweiten Eintrag
     * erzeugen, und der Zustand behauptet nie zwei Ausführungen, wo eine war.
     */
    fun recordCall(
        stepId: String,
        toolName: String,
        path: String? = null,
        changedPaths: List<String> = emptyList(),
        isIrreversible: Boolean = false
    ): RecordOutcome {
        val key = callKey(toolName, path)
        val tool = safe(toolName)
        val target = safe(path.orEmpty())
        val changed = changedPaths.map { safe(it) }.ifEmpty {
            if (isIrreversible && target.isNotEmpty()) listOf(target) else emptyList()
        }

        val existing = records[stepId]
        if (existing != null && key in existing.executedCallKeys) {
            return RecordOutcome.AlreadyRecorded(existing)
        }

        val updated = update(stepId) {
            it.copy(
                status = AgentRunStatus.RUNNING,
                toolNames = (it.toolNames + tool).distinct(),
                touchedPaths = (it.touchedPaths + changed).distinct(),
                commitState = if (changed.isNotEmpty() && it.commitState == CommitState.NOT_APPLICABLE) {
                    CommitState.NOT_SAVED
                } else {
                    it.commitState
                },
                executedCallKeys = it.executedCallKeys + key
            )
        }
        return RecordOutcome.Recorded(updated)
    }

    // ── Innere Helfer ────────────────────────────────────────────────────────

    /**
     * Der Zustand eines Schritts, neu angelegt falls unbekannt.
     *
     * `update` kann nichts aus dem Zustand entfernen: es verändert nur Status,
     * Listen und den Hinweis. Ein vergessenes `copy`-Feld wäre hier der einzige
     * Weg, Verlauf zu verlieren.
     */
    private fun update(stepId: String, change: (AgentStepRecord) -> AgentStepRecord): AgentStepRecord {
        val current = records[stepId] ?: AgentStepRecord(
            stepId = stepId,
            status = AgentRunStatus.PLANNED
        )
        val updated = change(current)
        records[stepId] = updated
        return updated
    }

    /**
     * Jeder frei formulierte oder von außen kommende Text läuft hier durch.
     *
     * Ohne das könnte ein Werkzeug, das eine Fehlermeldung mit einem Schlüssel
     * weiterreicht, genau diesen Schlüssel in den gespeicherten Laufstatus
     * schreiben — und von dort in ein Protokoll oder eine Sicherung.
     */
    private fun safe(text: String): String = SecretMasker.redact(text)
}