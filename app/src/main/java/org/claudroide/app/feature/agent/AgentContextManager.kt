package org.claudroide.app.feature.agent

import org.claudroide.app.feature.project.ContextCandidate
import org.claudroide.app.feature.project.ContextSelection
import org.claudroide.app.feature.project.ContextSelectionPolicy
import org.claudroide.app.feature.project.ProjectExclusionPolicy
import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 074 — „Kontext verwalten“.
 *
 * Ziel: aus Chat und Projekt nur relevante Teile verwenden und bei einer
 * Begrenzung offen damit umgehen. Vier Zusagen sind strukturell abgesichert:
 *
 *  1. **Der Nutzer erkennt, wenn ältere Gesprächsteile nicht mehr
 *     berücksichtigt werden.** Jeder ausgelassene Teil steht mit Kennung und
 *     Grund in [ContextPlan.droppedTurns], und [ContextPlan.disclosureLines] führt
 *     sie unter der Überschrift „nicht mehr berücksichtigt“. Es gibt keinen Weg,
 *     einen Teil stillschweigend zu streichen: Der Plan entsteht aus *allen*
 *     übergebenen Teilen, und jeder Teil ist entweder in [ContextPlan.keptTurns]
 *     oder in [ContextPlan.droppedTurns] mit Grund.
 *
 *  2. **Keine vertrauliche Datei gelangt über die Kürzung hinein.** Zwei
 *     Ebenen, weil Kürzung ein zweiter Weg in den Kontext wäre: Die Dateiliste
 *     stammt ausschließlich aus [ContextSelectionPolicy.select] — und wird
 *     danach noch einmal gegen [ProjectExclusionPolicy] geprüft
 *     ([ContextPlan.rejectedFiles]). Findet sich dort eine Geheimnisdatei, ist der
 *     ganze Plan [ContextPlanStatus.BLOCKED_BY_SECRET_FILE] und wird nicht
 *     gesendet. Außerdem kennt [ContextPlan] überhaupt keinen Dateiinhalt: Es
 *     führt Pfade, keine Texte.
 *
 *  3. **Ausschlüsse und Datenvorschau gelten vor jeder Anbieteranfrage.**
 *     [AgentContextManager.plan] rechnet die Dateiauswahl als ersten Schritt vor
 *     jeder Kürzungsentscheidung, und [ContextPlan.canSend] ist `false`, solange
 *     der Plan über dem Fenster liegt oder eine Datei abgelehnt wurde.
 *
 *  4. **Was der Nutzer festgehalten hat, wird nicht heimlich gekürzt.** Ein
 *     [ConversationTurn.isPinned] Teil bleibt in jedem Fall erhalten. Passt die
 *     festgehaltene Menge allein nicht ins Fenster, ist das kein Grund, still zu
 *     kürzen, sondern [ContextPlanStatus.OVER_LIMIT] mit Kürzungshinweis.
 *
 * **Belegte Zahlen, nicht geratene:** Die Anbieterdokumentation (Quelle in
 * [CONTEXT_DOC_URL], gelesen am [CONTEXT_DOC_VERIFIED]) nennt, dass Systemprompt,
 * alle Nachrichten, Werkzeugdefinitionen, Bilder, Dokumente **und** die erzeugte
 * Antwort in dasselbe Fenster zählen und dass die Genauigkeit mit der Tokenzahl
 * abnimmt — „mehr Kontext ist nicht automatisch besser“. Deshalb wird hier
 * zuerst gekürzt und nicht zuerst gefüllt; die Fenstermitte selbst kommt aus
 * [ContextSelectionPolicy.budgetFor] mit ihrer Quelle.
 *
 * Reines Kotlin: keine Datei, kein Netzwerk, kein Anbieteraufruf.
 */
object AgentContextManager {

    /**
     * Quellseite für die Regeln, nach denen gerechnet wird.
     *
     * Gelesen am [CONTEXT_DOC_VERIFIED]. Ohne diese Belegung wäre die Rechnung im
     * Code eine Behauptung.
     */
    const val CONTEXT_DOC_URL: String =
        "https://platform.claude.com/docs/en/build-with-claude/context-windows"

    /** Datum, an dem [CONTEXT_DOC_URL] gelesen wurde. */
    const val CONTEXT_DOC_VERIFIED: String = "2026-10-02"

    /** Wer einen Gesprächsteil geschrieben hat. */
    enum class TurnRole(val germanLabel: String) {
        USER("Sie"),
        ASSISTANT("Antwort"),
        TOOL_RESULT("Werkzeugergebnis")
    }

    /**
     * Ein Teil des Gesprächs.
     *
     * @property estimatedTokens bekannte Tokenzahl. Ist sie `null` oder negativ,
     *           wird der Teil nicht geraten, sondern als
     *           [DropReason.TOKEN_COUNT_UNKNOWN] ausgelassen — eine unbekannte
     *           Größe ist keine kleine Größe.
     * @property isPinned vom Nutzer festgehalten. Wird nie gekürzt.
     */
    data class ConversationTurn(
        val id: String,
        val role: TurnRole,
        val text: String,
        val estimatedTokens: Long? = null,
        val isPinned: Boolean = false
    )

    /** Warum ein Teil nicht in den Kontext geht. */
    enum class DropReason(val germanLabel: String) {
        /** Älterer Teil, für den kein Platz mehr blieb. */
        TOO_OLD_FOR_BUDGET("älterer Teil, kein Platz mehr"),

        /** Der Teil passt nicht in den Rest, der nach allem anderen übrig ist. */
        DOES_NOT_FIT("passt nicht mehr in den Rest des Kontextfensters"),

        /** Die Tokenzahl ist unbekannt, deshalb wird nicht geraten. */
        TOKEN_COUNT_UNKNOWN("Tokenzahl unbekannt, deshalb nicht verwendet"),

        /** Der Teil enthält einen Schlüssel und wurde geschwärzt statt gesendet. */
        SECRET_REDACTED("enthielt einen Zugangsschlüssel und wurde geschwärzt")
    }

    /** Ein ausgelassener Gesprächsteil — mit Grund, immer. */
    data class DroppedTurn(
        val turn: ConversationTurn,
        val reason: DropReason,
        val message: String
    )

    /** Wie viel vom Kontextfenster belegt ist. */
    data class ContextUsage(
        val windowTokens: Long,
        val systemPromptTokens: Long,
        val toolDefinitionTokens: Long,
        val keptTurnTokens: Long,
        val fileTokens: Long,
        val reservedForAnswerTokens: Long
    ) {
        /** Was tatsächlich im Fenster liegt, ohne die reservierte Antwort. */
        val sentTokens: Long
            get() = systemPromptTokens + toolDefinitionTokens + keptTurnTokens + fileTokens

        /** Fenster minus Belegung. */
        val remainingTokens: Long
            get() = windowTokens - sentTokens

        /**
         * Passt der Plan — samt Platz für die zugesagte Antwort — ins Fenster?
         *
         * Nicht nur „passt der Kontext“, sondern „bleibt auch Raum für die
         * Antwort“. Ein Plan, der das Fenster für sich allein ausfüllt, wäre
         * technisch passend und praktisch unbrauchbar: Die Antwort hätte keinen
         * Platz mehr.
         */
        val fits: Boolean
            get() = remainingTokens >= reservedForAnswerTokens

        /** Um wie viele Tokens der Plan zu groß wäre. */
        val overageTokens: Long
            get() = if (fits) 0L else (reservedForAnswerTokens - remainingTokens).coerceAtLeast(0L)
    }

    /** Der Zustand des Plans. */
    enum class ContextPlanStatus(val germanLabel: String) {
        /** Passt und kann gesendet werden. */
        READY("bereit zum Senden"),

        /** Zu groß. Der Nutzer muss kürzen. */
        OVER_LIMIT("zu groß für das Kontextfenster"),

        /** Eine Geheimnisdatei wäre im Kontext gelandet. */
        BLOCKED_BY_SECRET_FILE("eine Datei mit möglichen Geheimnissen ist im Kontext")
    }

    /**
     * Der fertige Kontextplan.
     *
     * @property files die Auswahl aus Aufgabe 068 — mit Pfad, Zeilenbereich und
     *           Auswahlgrund je Datei. **Kein Dateiinhalt** in diesem Typ.
     */
    data class ContextPlan(
        val modelId: String,
        val status: ContextPlanStatus,
        val keptTurns: List<ConversationTurn>,
        val droppedTurns: List<DroppedTurn>,
        val files: ContextSelection,
        val sanitisedTurnIds: List<String>,
        val usage: ContextUsage,
        val rejectedFiles: List<String> = emptyList(),
        val notes: List<String> = emptyList()
    ) {
        /** Darf gesendet werden? */
        val canSend: Boolean
            get() = status == ContextPlanStatus.READY

        /**
         * Wurde überhaupt etwas gekürzt?
         *
         * Für die Oberfläche wichtig: Ein gekürzter Plan ist kein vollständiger
         * Plan, und das wird hier als Eigenschaft abgefragt statt aus dem Text
         * erschlossen.
         */
        val isTrimmed: Boolean
            get() = droppedTurns.isNotEmpty()

        /**
         * Die vollständige Anzeige vor dem Senden.
         *
         * Reihenfolge: Was geht raus (Fenster und Budget), was ist drin
         * (Gesprächsteile), was ist raus (Gesprächsteile mit Grund), was sind
         * Dateien (aus Aufgabe 068). Nichts davon wird zusammengefasst zu
         * „ein Teil Kontext“.
         */
        fun disclosureLines(): List<String> = buildList {
            add("Kontextplan für Modell $modelId — ${status.germanLabel}.")
            add(
                "Kontextfenster: ${usage.windowTokens} Tokens. Belegt: " +
                    "${usage.sentTokens} Tokens, davon ${usage.keptTurnTokens} Tokens " +
                    "Gespräch und ${usage.fileTokens} Tokens Projektdateien."
            )
            add(
                "Davon Systemanweisung: ${usage.systemPromptTokens} Tokens, " +
                    "Werkzeugbeschreibungen: ${usage.toolDefinitionTokens} Tokens, " +
                    "reserviert für die Antwort: ${usage.reservedForAnswerTokens} Tokens."
            )
            if (!usage.fits) {
                add(
                    "Der Kontext ist ${usage.overageTokens} Tokens zu groß. Es wird " +
                        "er nichts gesendet, bis gekürzt wurde."
                )
            }

            add("")
            add("Berücksichtigte Gesprächsteile (${keptTurns.size}):")
            if (keptTurns.isEmpty()) {
                add("  keine")
            }
            keptTurns.forEach {
                add("  ${it.id} (${it.role.germanLabel}${if (it.isPinned) ", festgehalten" else ""})")
            }

            add("")
            if (droppedTurns.isEmpty()) {
                add("Es wurde nichts weggelassen.")
            } else {
                add("NICHT mehr berücksichtigt (${droppedTurns.size}):")
                droppedTurns.forEach {
                    add("  ${it.turn.id} (${it.turn.role.germanLabel}): ${it.message}")
                }
            }

            if (sanitisedTurnIds.isNotEmpty()) {
                add("")
                add(
                    "Geschwärzt vor dem Senden: ${sanitisedTurnIds.size} Teil(e). " +
                        "Ein Zugangsschlüssel wird nie übertragen."
                )
            }

            if (rejectedFiles.isNotEmpty()) {
                add("")
                add(
                    "Abgelehnt (${rejectedFiles.size}): " +
                        rejectedFiles.joinToString(", ") +
                        ". Diese Dateien werden nicht gelesen und nicht gesendet."
                )
            }

            add("")
            add("Projektdateien:")
            addAll(files.disclosureLines().map { "  $it" })

            notes.forEach { add("") ; add("Hinweis: $it") }
        }
    }

    /**
     * Erstellt den Kontextplan.
     *
     * Ablauf in fester Reihenfolge: Dateien auswählen (dabei Geheimnisfilter),
     * Auswahl gegen den Geheimnisfilter **nach**prüfen, Gesprächsteile
     * schwärzen, festgehaltene Teile sichern, dann von hinten nach vorne auffüllen.
     *
     * @param systemPromptTokens Platz, den die Systemanweisung belegt.
     * @param toolDefinitionTokens Platz, den die Werkzeugbeschreibungen belegen.
     * @param turns die Gesprächsteile in ihrer ursprünglichen Reihenfolge.
     * @param projectCandidates Dateien des Projekts als Kandidaten.
     */
    fun plan(
        modelId: String,
        systemPromptTokens: Long,
        toolDefinitionTokens: Long,
        turns: List<ConversationTurn>,
        projectCandidates: List<ContextCandidate>,
        todayEpochDays: Long
    ): ContextPlan {
        val budget = ContextSelectionPolicy.budgetFor(modelId, todayEpochDays)

        // Schritt 1: Dateien. Der Geheimnisfilter läuft hier, bevor über
        // irgendetwas anderes entschieden wird.
        val files = ContextSelectionPolicy.select(modelId, projectCandidates, todayEpochDays)

        // Schritt 2: dieselbe Regel noch einmal über das *Ergebnis*. Eine
        // Kürzungsentscheidung darf keinen zweiten Weg in den Kontext öffnen.
        val rejected = findExcludedFiles(files)

        if (rejected.isNotEmpty()) {
            return ContextPlan(
                modelId = modelId,
                status = ContextPlanStatus.BLOCKED_BY_SECRET_FILE,
                keptTurns = emptyList(),
                droppedTurns = emptyList(),
                files = files,
                sanitisedTurnIds = emptyList(),
                usage = usage(
                    budget.windowTokens,
                    systemPromptTokens,
                    toolDefinitionTokens,
                    0L,
                    0L,
                    budget.reservedForAnswerTokens
                ),
                rejectedFiles = rejected,
                notes = listOf(
                    "Es wird nichts gesendet, solange eine Datei mit möglichen " +
                        "Geheimnissen im Kontext wäre."
                )
            )
        }

        // Schritt 3: Schwärzen. Ein Gesprächsteil, in dem ein Schlüssel steht,
        // geht nicht als Originaltext raus.
        val sanitised = turns.map { turn ->
            val text = if (SecretMasker.containsSecretLikeText(turn.text)) {
                SecretMasker.redact(turn.text)
            } else {
                turn.text
            }
            turn.copy(text = text)
        }
        val sanitisedIds = turns.indices
            .filter { sanitised[it].text != turns[it].text }
            .map { turns[it].id }

        // Schritt 4: Platz für Gesprächsteile. Was Dateien, Systemanweisung,
        // Werkzeugbeschreibungen und die reservierte Antwort brauchen, ist schon
        // belegt — für Gesprächsteile bleibt der Rest.
        val fileTokens = files.usedTokens
        val allowance = budget.windowTokens -
            systemPromptTokens -
            toolDefinitionTokens -
            fileTokens -
            budget.reservedForAnswerTokens

        val kept = ArrayList<ConversationTurn>()
        val dropped = ArrayList<DroppedTurn>()

        // Festgehaltene Teile zuerst: Sie sind eine ausdrückliche Nutzerentscheidung
        // und stehen unter keinem Budgetdruck.
        val pinned = sanitised.filter { it.isPinned }
        pinned.forEach { kept += it }

        // Dann von hinten nach vorne, bis nichts mehr passt. Jeder ältere Teil
        // wandert mit Grund in die Liste der nicht berücksichtigten Teile.
        var used = pinned.sumOf { tokensOrZero(it) }
        for (turn in sanitised.filterNot { it.isPinned }.asReversed()) {
            val tokens = tokensOrZero(turn)
            if (tokens < 0L) {
                dropped += DroppedTurn(
                    turn,
                    DropReason.TOKEN_COUNT_UNKNOWN,
                    "${turn.id}: ${DropReason.TOKEN_COUNT_UNKNOWN.germanLabel}. " +
                        "Ohne diese Angabe wird nicht geschätzt und nichts geraten."
                )
                continue
            }
            if (allowance >= 0L && used + tokens > allowance) {
                dropped += DroppedTurn(
                    turn,
                    DropReason.TOO_OLD_FOR_BUDGET,
                    "${turn.id}: ${DropReason.TOO_OLD_FOR_BUDGET.germanLabel}. " +
                        "Der Teil braucht $tokens Tokens, frei sind " +
                        "${(allowance - used).coerceAtLeast(0)} Tokens."
                )
                continue
            }
            if (allowance < 0L) {
                dropped += DroppedTurn(
                    turn,
                    DropReason.DOES_NOT_FIT,
                    "${turn.id}: Der Kontext ist bereits durch Systemanweisung, " +
                        "Werkzeuge, Projektdateien und die reservierte Antwort " +
                        "voll. Dieser Teil wird nicht verwendet."
                )
                continue
            }
            used += tokens
            kept += turn
        }

        val contextUsage = usage(
            budget.windowTokens,
            systemPromptTokens,
            toolDefinitionTokens,
            kept.sumOf { tokensOrZero(it) }.coerceAtLeast(0L),
            fileTokens,
            budget.reservedForAnswerTokens
        )

        val overLimit = !contextUsage.fits

        val notes = buildList {
            if (sanitisedIds.isNotEmpty()) {
                add("Ein Zugangsschlüssel stand im Gespräch und wurde vor dem Senden unkenntlich gemacht.")
            }
            if (pinned.isNotEmpty() && !contextUsage.fits) {
                add(
                    "Die von Ihnen festgehaltenen Teile passen allein nicht ins Fenster. " +
                        "Sie wurden nicht stillschweigend entfernt — bitte lösen Sie die " +
                        "Markierung oder kürzen Sie anderswo."
                )
            }
        }

        return ContextPlan(
            modelId = modelId,
            status = if (overLimit) ContextPlanStatus.OVER_LIMIT else ContextPlanStatus.READY,
            // Chronologisch, damit die Oberfläche die Unterhaltung in Reihenfolge zeigt.
            keptTurns = kept.sortedBy { orderOf(it, sanitised) },
            droppedTurns = dropped,
            files = files,
            sanitisedTurnIds = sanitisedIds,
            usage = contextUsage,
            notes = notes
        )
    }

    /**
     * Prüft eine fertige Dateiauswahl ein zweites Mal gegen den Geheimnisfilter.
     *
     * Diese Funktion ist heute über [plan] nicht erreichbar, weil
     * [ContextSelectionPolicy.select] dieselbe Regel schon anwendet. Sie ist
     * trotzdem da — und öffentlich prüfbar: Würde sich die Regel in Aufgabe 068
     * einmal lockern, würde hier der Unterschied auffallen, statt dass eine
     * Geheimnisdatei durch die Kürzung in den Kontext wandert. Eine Prüfung, die
     * man nicht aufrufen kann, wäre keine.
     *
     * @return die Pfade, die trotzdem nicht in den Kontext dürfen.
     */
    fun findExcludedFiles(files: ContextSelection): List<String> =
        files.included.filter { included ->
            ProjectExclusionPolicy.classify(included.path, exists = true).decision !=
                ProjectExclusionPolicy.FileDecision.ALLOWED
        }.map { it.path }

    /** Die bekannte Tokenzahl, oder `-1` wenn sie unbekannt ist. */
    private fun tokensOrZero(turn: ConversationTurn): Long =
        turn.estimatedTokens ?: -1L

    /** Die ursprüngliche Reihenfolge, damit die Anzeige nicht springt. */
    private fun orderOf(turn: ConversationTurn, all: List<ConversationTurn>): Int =
        all.indexOfFirst { it.id == turn.id }.coerceAtLeast(0)

    private fun usage(
        windowTokens: Long,
        systemPromptTokens: Long,
        toolDefinitionTokens: Long,
        keptTurnTokens: Long,
        fileTokens: Long,
        reservedForAnswerTokens: Long
    ) = ContextUsage(
        windowTokens = windowTokens,
        systemPromptTokens = systemPromptTokens.coerceAtLeast(0L),
        toolDefinitionTokens = toolDefinitionTokens.coerceAtLeast(0L),
        keptTurnTokens = keptTurnTokens.coerceAtLeast(0L),
        fileTokens = fileTokens.coerceAtLeast(0L),
        reservedForAnswerTokens = reservedForAnswerTokens.coerceAtLeast(0L)
    )
}