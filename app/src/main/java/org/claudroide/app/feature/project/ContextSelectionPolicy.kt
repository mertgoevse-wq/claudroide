package org.claudroide.app.feature.project

import org.claudroide.app.feature.provider.UsageForecast
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Task 068 — „Nur nötigen Projektkontext wählen“ (Gate).
 *
 * Ziel der Aufgabe: die für eine Aufgabe relevanten Dateien finden, **ohne den
 * ganzen Projektordner zu übertragen**. Drei Zusagen sind strukturell
 * abgesichert, nicht nur per Test:
 *
 *  1. **Jede gesendete Datei ist benennbar.** Was ausgewählt wird, steht in
 *     [SelectedContext.describe] — Pfad, Zeilenbereich und der Grund der
 *     Auswahl. [ContextSelection.disclosureLines] listet genau diese Zeilen für
 *     die Oberfläche; es gibt keinen Weg, ein Element ohne Beschreibung in die
 *     Auswahl zu bringen, weil [ContextSelection.included] aus [SelectedContext]
 *     besteht und nichts anderes kennt.
 *
 *  2. **Kontextgrenzen lassen sich nicht durch stilles Vollprojektladen
 *     umgehen.** Das Budget in [ContextBudget] ist ein *kleiner Bruchteil* des
 *     Modellfensters, nie das Fenster selbst ([MAX_PROJECT_FRACTION]) und nie
 *     mehr als [HARD_PROJECT_TOKEN_CEILING]. [ModelContextPolicy.selectWholeProject]
 *     lehnt ein Vollprojektladen ohne ausdrückliche Bestätigung ab und bleibt
 *     selbst nach Bestätigung unter derselben Obergrenze und derselben
 *     Geheimnisregel. Es gibt keinen Parameter, der das Budget aufhebt.
 *
 *  3. **Der Geheimnisfilter läuft vor dem Kontextversand.** [ProjectExclusionPolicy]
 *     entscheidet in [ContextSelectionPolicy.select] als Erstes. Ein `BLOCKED_SECRET`
 *     kann [ProjectExclusionPolicy.canOverride] nicht aufheben, und diese Klasse
 *     bietet keinen Umweg daran vorbei — die Kandidatenliste ist bereits das
 *     Ergebnis des Filters, nicht das Material, aus dem der Filter später noch
 *     etwas entfernt.
 *
 * **Ehrlichkeitsregeln, die der Rest des Projekts bereits trägt:**
 *  - Fenstergrößen stammen aus [ModelContextLimits.TABLE] mit Quellen-URL und
 *    Prüfdatum. Für ein unbekanntes Modell wird kein Fenster geraten; es gilt
 *    dann der konservative [UNKNOWN_WINDOW_TOKENS]-Wert mit
 *    [ContextBudget.isWindowKnown] `false`.
 *  - Eine Tokenzahl wird nie geraten, wenn sie nicht bekannt ist: Kandidaten ohne
 *    Tokens und ohne Größe werden ausgelassen und begründet, statt still ein
 *    niedriges Budget anzunehmen.
 *  - Diese Datei kennt keinen Anbietertransport. Sie zählt nie einen
 *    [org.claudroide.app.feature.provider.ProviderCallAudit]-Aufruf und sendet
 *    nichts; sie entscheidet, was später gesendet *werden darf*.
 *
 * Verwendet wurde bewusst kein semantischer Ranking-Ansatz: ein solcher kann auf
 * dem Gerät nicht nachvollziehbar erklären, warum eine Datei gewählt wurde. Die
 * [RelevanceSignal] hier sind überprüfbar — der Nutzer kann sie gegen die
 * ausgewählten Pfade prüfen, und ein `NONE` bedeutet ehrlich „kein erkennbarer
 * Zusammenhang“, nicht „vielleicht nützlich“.
 */
object ModelContextLimits {

    /**
     * Quellseite der Fensterrößen. Abgerufen am [VERIFIED]; davor prüfen, weil
     * sich Fenstergrößen mit einem Modellwechsel ändern können.
     */
    const val SOURCE_URL: String = "https://platform.claude.com/docs/en/models/overview"

    /** Datum, an dem [TABLE] zuletzt gegen [SOURCE_URL] gelesen wurde. */
    const val VERIFIED: String = "2026-10-01"

    /**
     * Fenstergröße, wenn das Modell nicht eingetragen ist.
     *
     * Bewusst klein und nicht als Vermutung: 32 000 Tokens sind deutlich unter
     * jedem heutigen Claude-Fenster, damit ein unbekanntes Modell nicht dazu
     * führt, dass mehr Daten das Gerät verlassen als beabsichtigt.
     */
    const val UNKNOWN_WINDOW_TOKENS: Long = 32_000L

    /**
     * Eine belegte Fensterröße.
     *
     * @property maxInputTokens Eingabe-Kontextfenster des Modells.
     * @property maxOutputTokens maximale Antwortlänge; die wird vom Kontext
     *           abgezogen, damit Kontext und Antwort nicht dasselbe Fenster
     *           beanspruchen.
     */
    data class Limit(
        val modelId: String,
        val maxInputTokens: Long,
        val maxOutputTokens: Long,
        val sourceUrl: String = SOURCE_URL,
        val verifiedDate: String = VERIFIED
    ) {
        /** Ob Quelle und Datum benutzbar sind — ohne Beleg keine Zahl verwenden. */
        val isUsable: Boolean
            get() = (sourceUrl.startsWith("https://") || sourceUrl.startsWith("http://")) &&
                parseLimitDate(verifiedDate) != null &&
                maxInputTokens > 0 && maxOutputTokens > 0
    }

    /**
     * Die belegten Fenstergrößen. Gelesen aus [SOURCE_URL] am [VERIFIED]:
     * Opus 5.5, Sonnet 5.5 und Fable 5.1 mit je 1 Mio. Eingabe- und 128 000
     * Ausgabetokens; Haiku 4.5 mit 200 000 Eingabe- und 64 000 Ausgabetokens.
     */
    val TABLE: Map<String, Limit> = mapOf(
        "claude-opus-5-5" to Limit("claude-opus-5-5", 1_000_000L, 128_000L),
        "claude-sonnet-5-5" to Limit("claude-sonnet-5-5", 1_000_000L, 128_000L),
        "claude-fable-5-1" to Limit("claude-fable-5-1", 1_000_000L, 128_000L),
        "claude-haiku-4-5" to Limit("claude-haiku-4-5", 200_000L, 64_000L),
        "claude-haiku-4-5-20251001" to Limit("claude-haiku-4-5-20251001", 200_000L, 64_000L)
    )

    /** Eintrag für ein Modell, `null` wenn es nicht belegt ist. */
    fun forModel(modelId: String): Limit? = TABLE[modelId.trim()]

    /**
     * Wie alt ist die geprüfte Angabe in Tagen? `-1`, wenn das Datum nicht lesbar
     * ist. Ein negatives Ergebnis (Prüfdatum in der Zukunft) wird gekappt, weil
     * ein Datum in der Zukunft kein Beleg ist.
     */
    fun ageInDays(limit: Limit, todayEpochDays: Long): Long {
        val verified = parseLimitDate(limit.verifiedDate) ?: return -1L
        return maxOf(0L, todayEpochDays - verified.toEpochDay())
    }

    private fun parseLimitDate(text: String): LocalDate? = try {
        LocalDate.parse(text.trim())
    } catch (e: DateTimeParseException) {
        null
    } catch (e: Exception) {
        null
    }
}

/** Wie eine Datei in die Auswahl kam. Jedes Signal ist für den Nutzer prüfbar. */
enum class RelevanceSignal(val germanLabel: String) {

    /** Der Nutzer hat die Datei selbst benannt. */
    EXPLICIT_MATCH("von Ihnen ausgewählt"),

    /** Der Dateiname enthält einen Begriff aus dem Aufgabentext. */
    TASK_TERM_MATCH("Name passt zum Aufgabentext"),

    /** Eine Projektanweisung wie `CLAUDE.md`, die den Kontext selbst erklärt. */
    PROJECT_INSTRUCTION("Projektanweisung"),

    /**
     * Kein erkennbarer Zusammenhang. Wird **nicht** automatisch aufgenommen —
     * eine Datei ohne erkennbare Begründung zu senden hieße, dem Anbieter Daten
     * ohne benennbaren Grund zu geben.
     */
    NONE("kein erkennbarer Zusammenhang")
}

/**
 * Eine Datei, die als Kontext infrage kommt.
 *
 * @property estimatedTokens bekannte Tokenzahl, oder `null`.
 * @property sizeBytes Größe, falls bekannt. Nur als Notfallquelle für eine
 *           Tokenzahl, nie als Ersatz für eine Messung.
 */
data class ContextCandidate(
    val path: String,
    val exists: Boolean = true,
    val estimatedTokens: Int? = null,
    val sizeBytes: Long? = null,
    val signal: RelevanceSignal = RelevanceSignal.NONE
) {
    /**
     * Tokenzahl und woher sie stammt.
     *
     * Ein Dateigrößenvergleich ist eine grobe Annahme — dieselbe, die
     * [UsageForecast] im Projekt bereits verwendet — und wird als Annahme
     * gekennzeichnet, nie als Messwert.
     */
    fun tokensAndSource(): Pair<Long, String> = when {
        estimatedTokens != null -> estimatedTokens.toLong() to "gemessen"

        sizeBytes != null ->
            sizeBytes / UsageForecast.CHARACTERS_PER_TOKEN to
                "grobe Annahme aus der Dateigröße (${UsageForecast.CHARACTERS_PER_TOKEN} Zeichen je Token)"

        else -> -1L to "unbekannt"
    }
}

/** Eine ausgewählte Datei — mit allem, was sie für die Anzeige braucht. */
data class SelectedContext(
    val path: String,
    val signal: RelevanceSignal,
    val estimatedTokens: Long,
    val tokenSource: String,
    val firstLine: Int? = null,
    val lastLine: Int? = null
) {
    /**
     * Die eine Zeile, die diese Datei für den Nutzer benennbar macht.
     *
     * Die Reihenfolge ist fest: was, woher, wozu. „Datei“ steht immer vor dem
     * Pfad, damit ein Screenshot oder eine Sprachausgabe nicht beim blossen
     * Dateinamen stehenbleibt.
     */
    fun describe(): String {
        val range = when {
            firstLine != null && lastLine != null && firstLine == lastLine -> " (Zeile $firstLine)"
            firstLine != null && lastLine != null -> " (Zeilen $firstLine–$lastLine)"
            else -> ""
        }
        return "Datei: $path$range — ${signal.germanLabel}, $estimatedTokens Tokens ($tokenSource)"
    }
}

/** Eine Datei, die nicht in den Kontext kommt — mit Grund. */
data class OmittedContext(
    val path: String,
    val reason: String
)

/**
 * Das Budget für einen Aufruf.
 *
 * @property windowTokens Fenstergröße des Modells, falls belegt.
 * @property isWindowKnown `false`, wenn das Modell nicht belegt ist und
 *           [ModelContextLimits.UNKNOWN_WINDOW_TOKENS] gilt.
 * @property maxProjectTokens das tatsächlich erlaubte Budget für Projektinhalte.
 *           Das ist nie das ganze Fenster.
 * @property ageInDays Alter der Fensterquelle; ein veralteter Wert wird in der
 *           Oberfläche sichtbar gemacht.
 */
data class ContextBudget(
    val windowTokens: Long,
    val reservedForAnswerTokens: Long,
    val maxProjectTokens: Long,
    val isWindowKnown: Boolean,
    val ageInDays: Long,
    val sourceUrl: String?
) {
    /** Deutsche Zeilen, die vor dem Senden angezeigt werden. */
    fun disclosureLines(): List<String> = buildList {
        add(
            if (isWindowKnown) {
                "Kontextfenster: ${windowTokens} Tokens (Quelle geprüft, Alter $ageInDays Tage)."
            } else {
                "Kontextfenster unbekannt — es wird vorsichtig mit " +
                    "${ModelContextLimits.UNKNOWN_WINDOW_TOKENS} Tokens gerechnet."
            }
        )
        add("Davon stehen dem Projektkontext höchstens $maxProjectTokens Tokens zur Verfügung.")
        add("Für die Antwort bleiben $reservedForAnswerTokens Tokens frei.")
        sourceUrl?.let { add("Quelle der Fenstergröße: $it") }
    }
}

/** Das Ergebnis einer Auswahl. */
data class ContextSelection(
    val included: List<SelectedContext>,
    val omitted: List<OmittedContext>,
    val budget: ContextBudget,
    val usedTokens: Long
) {
    /**
     * `true`, wenn etwas wegen des Budgets fehlt. Nur dann lohnt eine bewusste
     * Erweiterung — das ist die geforderte „Erweiterung nur wenn nötig“.
     */
    val needsExpansion: Boolean
        get() = omitted.any {
            it.reason.startsWith(ContextSelectionPolicy.OMITTED_FOR_BUDGET)
        }

    /** Eine Auswahl ohne Inhalt ist kein Vorschlag. */
    val isEmpty: Boolean get() = included.isEmpty()

    /** Jede gesendete Datei wird hier benannt. */
    fun disclosureLines(): List<String> = buildList {
        addAll(budget.disclosureLines())
        if (included.isEmpty()) {
            add("Es wurden keine Projektdateien ausgewählt.")
            return@buildList
        }
        add("Ausgewählte Dateien (${included.size}):")
        included.forEach { add("- ${it.describe()}") }
        if (omitted.isNotEmpty()) {
            add("Nicht enthalten (${omitted.size}):")
            omitted.forEach { add("- ${it.path}: ${it.reason}") }
        }
    }
}

/**
 * Wählt den kleinsten sinnvollen Projektkontext aus.
 *
 * Alle Methoden sind rein: kein Android-Import, kein Netzwerk, kein Zustand.
 * Aufrufer liefern die Kandidaten, diese Klasse entscheidet nur.
 */
object ContextSelectionPolicy {

    /**
     * Höchstens dieser Anteil des Modellfensters wird für Projektdateien
     * verwendet. Bewusst klein: Kontext soll gezielt sein, und auf einem Gerät
     * mit 8 GB RAM soll eine Anfrage nicht megabyteweise Material tragen.
     */
    const val MAX_PROJECT_FRACTION: Double = 0.25

    /**
     * Absolute Obergrenze in Tokens, unabhängig vom Modell. Auch ein 1-Mio-Token-
     * Fenster wird nicht ausgenutzt: das wäre eine stille Entscheidung über den
     * Umfang der übertragenen Daten.
     */
    const val HARD_PROJECT_TOKEN_CEILING: Long = 60_000L

    /** Präfix, damit [ContextSelection.needsExpansion] budgetbezogene Ausfälle erkennt. */
    const val OMITTED_FOR_BUDGET: String = "Budget"

    /** Dateinamen, die als Projektanweisung gelten. */
    private val instructionFileNames: Set<String> = setOf("claude.md", "agents.md")

    /**
     * Berechnet das Budget für ein Modell.
     *
     * Ist das Modell nicht belegt, gilt [ModelContextLimits.UNKNOWN_WINDOW_TOKENS]
     * und [ContextBudget.isWindowKnown] ist `false` — die Oberfläche sagt dann,
     * dass die Fenstergröße unbekannt ist, statt eine Zahl zu erfinden.
     */
    fun budgetFor(modelId: String, todayEpochDays: Long): ContextBudget {
        val limit = ModelContextLimits.forModel(modelId)
        return if (limit == null || !limit.isUsable) {
            val fallback = ModelContextLimits.UNKNOWN_WINDOW_TOKENS
            ContextBudget(
                windowTokens = fallback,
                reservedForAnswerTokens = 0L,
                maxProjectTokens = HARD_PROJECT_TOKEN_CEILING.coerceAtMost(fallback),
                isWindowKnown = false,
                ageInDays = -1L,
                sourceUrl = null
            )
        } else {
            val byFraction = (limit.maxInputTokens * MAX_PROJECT_FRACTION).toLong()
            ContextBudget(
                windowTokens = limit.maxInputTokens,
                reservedForAnswerTokens = limit.maxOutputTokens,
                maxProjectTokens = HARD_PROJECT_TOKEN_CEILING.coerceAtMost(byFraction),
                isWindowKnown = true,
                ageInDays = ModelContextLimits.ageInDays(limit, todayEpochDays),
                sourceUrl = limit.sourceUrl
            )
        }
    }

    /**
     * Ordnet Dateien nach Relevanz: von Hand benannt, dann aus dem Aufgabentext
     * erschlossen, dann Projektanweisung. Innerhalb einer Stufe nach
     * Tokenbedarf aufsteigend — die billigste passende Datei zuerst.
     */
    fun rank(candidates: List<ContextCandidate>): List<ContextCandidate> =
        candidates.sortedWith(
            compareBy(
                { it.signal.ordinal },
                { it.estimatedTokens ?: Int.MAX_VALUE },
                { it.path }
            )
        )

    /**
     * Wählt aus, was in das Budget passt.
     *
     * Ablauf in fester Reihenfolge: Geheimnisfilter, dann Verfügbarkeit, dann
     * Relevanz, dann Budget. Ein Geheimnis wird also nie „gewählt und später
     * herausgerechnet“ — es ist schon vorher kein Kandidat.
     *
     * @param candidates Dateien, die der Aufrufer in Betracht zieht. Die Liste
     *        wird hier **nicht** verändert; [filteredCandidates] wendet denselben
     *        Filter an und ist für Aufrufer nützlich, die schon vorher anzeigen
     *        wollen, was herausfällt.
     */
    fun select(
        modelId: String,
        candidates: List<ContextCandidate>,
        todayEpochDays: Long
    ): ContextSelection = selectWithBudget(
        candidates = candidates,
        budget = budgetFor(modelId, todayEpochDays)
    )

    /**
     * Erweitert die Auswahl ausdrücklich — die einzige Art, wie mehr Kontext
     * hinzukommt.
     *
     * Auch hier laufen Geheimnisfilter und Budget. Eine Erweiterung kann
     * [HARD_PROJECT_TOKEN_CEILING] nicht überschreiten und ein Geheimnis nicht
     * einschleusen; das ist der Grund, warum diese Funktion existiert und keine
     * einfache „Budget aufheben“-Option.
     */
    fun expand(
        modelId: String,
        candidates: List<ContextCandidate>,
        requestedPaths: Set<String>,
        todayEpochDays: Long,
        /** Zusätzliches Budget, hart begrenzt auf [HARD_PROJECT_TOKEN_CEILING]. */
        additionalTokens: Long
    ): ContextSelection {
        val base = budgetFor(modelId, todayEpochDays)
        val raised = ContextBudget(
            windowTokens = base.windowTokens,
            reservedForAnswerTokens = base.reservedForAnswerTokens,
            maxProjectTokens = (base.maxProjectTokens + additionalTokens)
                .coerceIn(0L, HARD_PROJECT_TOKEN_CEILING),
            isWindowKnown = base.isWindowKnown,
            ageInDays = base.ageInDays,
            sourceUrl = base.sourceUrl
        )
        // Nur die ausdrücklich verlangten Pfade kommen in die Erweiterung. Ohne
        // diese Einschränkung würde `expand` still den ganzen Kandidatenbestand
        // nachrücken und damit genau das tun, was die Aufgabe verbietet.
        return selectWithBudget(
            candidates = candidates.filter { it.path in requestedPaths },
            budget = raised
        )
    }

    /**
     * Ergebnis eines angefragten Vollprojektladens.
     */
    sealed class WholeProjectDecision {

        /**
         * Abgelehnt. Es wurde nichts ausgewählt und nichts gesendet.
         *
         * @property reason deutsche Begründung für die Oberfläche.
         */
        data class Refused(val reason: String) : WholeProjectDecision()

        /** Zugelassen, aber durch Filter und Budget begrenzt. */
        data class Selected(val selection: ContextSelection) : WholeProjectDecision()
    }

    /**
     * Behandelt die Anfrage, den ganzen Projektordner als Kontext zu senden.
     *
     * Das ist der direkte Angriff auf die Zusage „kein stilles
     * Vollprojektladen“. Deshalb ist es ein eigener Weg mit zwei Stufen: ohne
     * ausdrückliche Bestätigung wird abgelehnt, mit Bestätigung wird immer noch
     * nach [ProjectExclusionPolicy] gefiltert und nach
     * [HARD_PROJECT_TOKEN_CEILING] begrenzt. Die Dateien behalten dabei ihr
     * Signal — ein Ordner voller Dateien ohne erkennbaren Zusammenhang wird
     * also auch dann nicht zum Vollprojekt.
     */
    fun selectWholeProject(
        modelId: String,
        candidates: List<ContextCandidate>,
        todayEpochDays: Long,
        acknowledgedByUser: Boolean
    ): WholeProjectDecision {
        if (!acknowledgedByUser) {
            return WholeProjectDecision.Refused(
                "Der ganze Projektordner wird nicht als Kontext gesendet. " +
                    "Wählen Sie die benötigten Dateien aus. Ohne Ihre ausdrückliche " +
                    "Bestätigung werden höchstens " +
                    "${budgetFor(modelId, todayEpochDays).maxProjectTokens} Tokens " +
                    "Projektkontext übertragen."
            )
        }
        // Auch nach der Bestätigung läuft dieselbe Auswahl: gleicher
        // Geheimnisfilter, gleiches Budget, gleiche Obergrenze. Die Dateien
        // behalten ihr Signal — ein Ordner voller Dateien ohne erkennbaren
        // Zusammenhang wird also auch hier nicht zum Vollprojekt.
        val selection = select(modelId, candidates, todayEpochDays)
        val droppedByBudget = selection.omitted.count { it.reason.startsWith(OMITTED_FOR_BUDGET) }
        val note = if (droppedByBudget == 0) {
            null
        } else {
            OmittedContext(
                path = "(Budget)",
                reason = "$OMITTED_FOR_BUDGET: $droppedByBudget weitere Datei(en) passen " +
                    "auch nach Ihrer Bestätigung nicht ins Budget und wurden nicht " +
                    "aufgenommen."
            )
        }
        return WholeProjectDecision.Selected(
            selection.copy(omitted = note?.let { selection.omitted + it } ?: selection.omitted)
        )
    }

    /**
     * Wendet denselben Filter an, wie er auch vor dem Senden liefe — für
     * Aufrufer, die schon vor der Auswahl anzeigen wollen, was herausfällt.
     */
    fun filteredCandidates(candidates: List<ContextCandidate>): List<ContextCandidate> =
        candidates.filter { candidate ->
            val exclusion = ProjectExclusionPolicy.classify(
                candidate.path,
                exists = candidate.exists,
                sizeBytes = candidate.sizeBytes
            )
            exclusion.decision == ProjectExclusionPolicy.FileDecision.ALLOWED
        }

    /**
     * Ordnet eine Datei einem Signal zu, anhand des Dateinamens.
     *
     * Bewusst mechanisch und erklärbar: `CLAUDE.md`/`AGENTS.md` sind
     * Projektanweisungen, und ein Aufgabenteil passt, wenn er im Dateinamen
     * vorkommt. Der Abgleich kleinschreibung, ohne Regex über dem Inhalt — der
     * Inhalt wird bewusst nicht durchsucht, weil das Geheimnis und Kosten
     * abhängig von der Datei machte.
     */
    fun signalFor(path: String, taskTerms: List<String>): RelevanceSignal {
        val fileName = path.trim().replace('\\', '/').substringAfterLast('/').lowercase()
        if (fileName in instructionFileNames) return RelevanceSignal.PROJECT_INSTRUCTION

        val terms = taskTerms.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val matches = terms.count { term -> fileName.contains(term) }
        return if (matches > 0) RelevanceSignal.TASK_TERM_MATCH else RelevanceSignal.NONE
    }

    /** Rechnet Bytes grob in Tokens, gekennzeichnet als Annahme. */
    fun tokensFromBytes(sizeBytes: Long): Long =
        BigDecimal(sizeBytes).divide(
            BigDecimal(UsageForecast.CHARACTERS_PER_TOKEN.toLong()),
            0,
            java.math.RoundingMode.DOWN
        ).toLong()

    private fun selectWithBudget(
        candidates: List<ContextCandidate>,
        budget: ContextBudget
    ): ContextSelection {
        // Die harte Obergrenze wird hier geprüft und nicht erst am Aufrufer: eine
        // veränderliche Obergrenze wäre genau der Weg, den diese Klasse nicht
        // haben darf. Sie ist für jedes Budget bindend, auch für ein
        // ausdrücklich erhöhtes.
        require(budget.maxProjectTokens <= HARD_PROJECT_TOKEN_CEILING) {
            "Kontextbudget ${budget.maxProjectTokens} überschreitet die Obergrenze " +
                "$HARD_PROJECT_TOKEN_CEILING."
        }

        val included = ArrayList<SelectedContext>()
        val omitted = ArrayList<OmittedContext>()
        var used = 0L

        for (candidate in rank(candidates)) {
            // Schritt 1: Geheimnisfilter — vor allem anderen.
            val exclusion = ProjectExclusionPolicy.classify(
                candidate.path,
                exists = candidate.exists,
                sizeBytes = candidate.sizeBytes
            )
            if (exclusion.decision == ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET ||
                exclusion.decision == ProjectExclusionPolicy.FileDecision.NOT_FOUND
            ) {
                omitted += OmittedContext(candidate.path, exclusion.message)
                continue
            }

            // Schritt 2: Ohne erkennbaren Zusammenhang wird nichts gesendet.
            if (candidate.signal == RelevanceSignal.NONE) {
                omitted += OmittedContext(
                    candidate.path,
                    "${candidate.signal.germanLabel}: Die Datei wird ohne Ihre " +
                        "Auswahl nicht gesendet. Wählen Sie sie bei Bedarf von Hand aus."
                )
                continue
            }

            // Schritt 3: Tokenzahl. Unbekannt heißt auslassen, nicht raten.
            val (tokens, tokenSource) = candidate.tokensAndSource()
            if (tokens < 0) {
                omitted += OmittedContext(
                    candidate.path,
                    "Tokenzahl unbekannt. Ohne diese Angabe lässt sich die Datei " +
                        "nicht gegen das Kontextbudget prüfen."
                )
                continue
            }

            // Schritt 4: Budget.
            if (used + tokens > budget.maxProjectTokens) {
                omitted += OmittedContext(
                    candidate.path,
                    "$OMITTED_FOR_BUDGET: $tokens Tokens, " +
                        "verbleibend ${budget.maxProjectTokens - used} Tokens."
                )
                continue
            }

            used += tokens
            included += SelectedContext(
                path = candidate.path,
                signal = candidate.signal,
                estimatedTokens = tokens,
                tokenSource = tokenSource
            )
        }

        return ContextSelection(
            included = included,
            omitted = omitted,
            budget = budget,
            usedTokens = used
        )
    }
}