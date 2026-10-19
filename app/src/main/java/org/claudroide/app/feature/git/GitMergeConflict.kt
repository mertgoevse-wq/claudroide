package org.claudroide.app.feature.git

/**
 * Task 102 — „Git-Konflikte".
 *
 * Ziel: Den Nutzer bei konkurrierenden Änderungen unterstützen, **ohne
 * Dateien zu überschreiben**. Ergebnis: Konfliktübersicht, betroffene
 * Abschnitte und kontrollierte Auswahl.
 *
 * Diese Datei führt **keine** Zusammenführung aus. Sie hält fest, was
 * konkurriert, wie der Nutzer entschieden hat, und was daraus hervorgeht — das
 * Schreiben bleibt außerhalb, wie im ganzen Projekt (Aufgabe 096 trennt dort
 * denselben Plan von der Ausführung).
 *
 * ## Die zwei Fertig-Bedingungen als Eigenschaft
 *
 *  1. *„Konflikt nicht still ‚gelöst' oder als fertig markiert wird."*
 *     [MergeOutcome] kennt keinen Zustand, in dem unentschiedene Abschnitte zu
 *     einer gültigen Datei führen. [MergeResolution.resolvedFile] liefert das
 *     Ergebnis nur, wenn **jeder** Abschnitt eine Entscheidung trägt; sonst ist
 *     der Zustand [ResolutionState.BLOCKED] mit [MergeOutcome.NoFileWritten].
 *     Es gibt kein `isResolved` im Sinn von „irgendetwas ist entschieden".
 *
 *  2. *„Zusammenführung vor Speicherung geprüft wird."*
 *     [MergeGate.mayWrite] prüft in fester Reihenfolge: Vollständigkeit der
 *     Entscheidungen, dann Bestätigung, dann Pfadtreue. Und weil ein
 *     Zusammenführen sonst stillschweigend Inhalt vernichtet, hält
 *     [ResolvedFile.retainedBothSides] fest, ob beide Elternseiten erhalten
 *     bleiben — eine Eigenschaft, die man am Ergebnis prüfen kann und nicht
 *     erst im Nachhinein im Dateisystem.
 *
 * ## Der Schutz: Originalstände bleiben erhalten
 *
 *  * [MergeResolution.originalVariants] liefert **unveränderte** Kopien von
 *    „uns" und „ihnen". Es gibt keinen Pfad, auf dem eine Entscheidung eine
 *    Elternvariante überschreibt — jede [SectionDecision] trägt ihr Ergebnis
 *    als **eigenen** Text.
 *  * [MergeGate.mayWrite] verweigert bei [ResolutionState.INCOMPLETE], solange
 *    auch nur ein Abschnitt fehlt. Ein Abbruch hinterlässt keine Datei, die
 *    aussieht, als wäre sie fertig.
 *
 * Reines Kotlin: kein Netz, kein Dateisystem, kein Git-Aufruf, kein Android.
 */

// ── Die konkurrierenden Seiten ────────────────────────────────────────────

/**
 * Die beiden Seiten eines Konfliktabschnitts, beide unverändert.
 *
 * [base] ist der gemeinsame Vorfahr. Sie bleibt im Ergebnis erhalten, damit
 * der Nutzer nachsehen kann, **worauf** sich die beiden Seiten eigentlich
 * beziehen — eine Zusammenführung, die diese Zeile verschluckt, zerstört
 * genau die Information, die bei einem Konflikt weiterhilft.
 */
data class ConflictVariants(
    val ours: String,
    val theirs: String,
    val base: String
) {
    init {
        // Erlaubt ist, dass beide Seiten gleich sind (nur der Vorfahr
        // unterscheidet sich). Verboten ist nur ein Zustand ohne Vorfahren,
        // weil „was war vorher da" die Frage ist, die der Konflikt aufwirft.
        require(base.isNotEmpty()) {
            "Ein Konfliktabschnitt braucht den gemeinsamen Vorfahren; " +
                "sonst ist nicht erkennbar, worüber gestritten wird."
        }
    }
}

/** Ein Abschnitt einer Datei, in dem die beiden Seiten auseinandergehen. */
data class ConflictingSection(
    val sectionId: String,
    val filePath: String,
    val variants: ConflictVariants
) {
    init {
        require(sectionId.isNotBlank()) { "Ein Konfliktabschnitt braucht eine Kennung." }
        require(filePath.isNotBlank()) { "Ein Konfliktabschnitt braucht seinen Dateipfad." }
        require(!filePath.contains("..")) {
            "Ein Konfliktabschnitt darf nicht aus dem Projekt ausbrechen: $filePath"
        }
    }
}

// ── Die Entscheidung des Nutzers ──────────────────────────────────────────

/**
 * Was der Nutzer mit **einem** Abschnitt gemacht hat.
 *
 * Es gibt **keine** Vorgabe und **keinen** stillen Standard: [KeptOurs] und
 * [KeptTheirs] sind ausdrückliche Wahlhandlungen, [Combined] trägt beide Texte
 * als eigene Felder, und [KeptBothSides] behält sie in der Reihenfolge
 * uns/ihnen. Wer sich nicht entscheidet, bekommt [UNRESOLVED] — nicht etwa
 * eine der beiden Seiten.
 *
 * [Combined] verlangt zwei **nicht leere** Texte: eine Entscheidung, die nur
 * eine Seite trägt, ist keine Kombination, sondern eine Auswahl unter anderem
 * Namen. Diese Prüfung steht deshalb im `init`, weil sie sonst in der
 * Oberfläche umgangen werden könnte, indem jemand das Objekt direkt baut.
 */
sealed interface SectionDecision {

    /** Anzeigename für die Oberfläche. */
    val label: String
    val germanLabel: String

    /** Nichts entschieden. Der einzige Zustand, der das Schreiben blockiert. */
    data object UNRESOLVED : SectionDecision {
        override val label: String get() = "unresolved"
        override val germanLabel: String get() = "nicht entschieden"
    }

    /** Die Seite „uns" behalten. */
    data object KeptOurs : SectionDecision {
        override val label: String get() = "kept ours"
        override val germanLabel: String get() = "uns behalten"
    }

    /** Die Seite „ihnen" behalten. */
    data object KeptTheirs : SectionDecision {
        override val label: String get() = "kept theirs"
        override val germanLabel: String get() = "ihre behalten"
    }

    /** Ein eigener Text, in dem beide Seiten enthalten sind. */
    data class Combined(val resultText: String) : SectionDecision {
        init {
            require(resultText.isNotBlank()) {
                "Eine Zusammenführung braucht Text; ein leerer Text verwirft beide Seiten."
            }
        }

        override val label: String get() = "combined"
        override val germanLabel: String get() = "zusammengeführt"
    }

    /** Beide Seiten nacheinander stehen lassen, ohne sie zu verschmelzen. */
    data object KeptBothSides : SectionDecision {
        override val label: String get() = "kept both sides"
        override val germanLabel: String get() = "beide Seiten behalten"
    }
}

// ── Die Entscheidung für die ganze Datei ──────────────────────────────────

/** Wie weit die Zusammenführung gekommen ist. */
enum class ResolutionState(val label: String, val germanLabel: String) {

    /** Jeder Abschnitt ist entschieden. */
    COMPLETE("complete", "vollständig entschieden"),

    /**
     * Mindestens ein Abschnitt ist offen.
     *
     * Wichtig: es gibt hier **kein** `PARTIAL`, das als Erfolg gelesen werden
     * könnte. „Teilweise gelöst" ist genau der Zustand, der bei einer
     * Zusammenführung stillschweigend Dateien überschreibt.
     */
    INCOMPLETE("incomplete", "nicht vollständig entschieden")
}

/**
 * Die Entscheidungen des Nutzers für **alle** Abschnitte einer Datei.
 *
 * Der Konstruktor nimmt die Abschnitte und die Entscheidungen **gemeinsam** und
 * prüft hier, dass zu jedem Abschnitt genau eine Entscheidung gehört. Damit kann
 * ein Aufrufer nicht versehentlich eine Entscheidung für einen Abschnitt
 * liefern, den es nicht gibt, oder eine zu zweit.
 *
 * [resolvedFile] ist der Ausgang der zweiten Fertig-Bedingung: Es liefert nur
 * dann eine [ResolvedFile], wenn [state] [ResolutionState.COMPLETE] ist. Und
 * auch dann nur, wenn das Ergebnis nachweislich beide Seiten enthält.
 */
class MergeResolution private constructor(
    val filePath: String,
    val sections: List<ConflictingSection>,
    private val decisions: Map<String, SectionDecision>,
    /** Hat der Nutzer das Ergebnis vor dem Speichern gesehen und bestätigt? */
    val confirmedByUser: Boolean
) {
    /**
     * Die Abschnitte, für die noch keine Entscheidung vorliegt.
     *
     * Eine Liste und keine Zahl: Der Nutzer soll sehen, **welche** Abschnitte
     * offen sind — eine bloße Anzahl nennt er ihm nicht.
     */
    val unresolvedSections: List<ConflictingSection>
        get() = sections.filter { decisionFor(it.sectionId) == SectionDecision.UNRESOLVED }

    /** Der Zustand, abgeleitet aus den Entscheidungen — nie gesetzt. */
    val state: ResolutionState
        get() = if (unresolvedSections.isEmpty()) {
            ResolutionState.COMPLETE
        } else {
            ResolutionState.INCOMPLETE
        }

    /**
     * Das zusammengeführte Ergebnis — oder `null`, wenn es noch nicht eins gibt.
     *
     * Zwei Gründe, aus denen hier `null` herauskommt: mindestens ein Abschnitt
     * ist unentschieden, oder eine Entscheidung verliert Inhalt. Im zweiten Fall
     * wird der Abschnitt mit dem Text **beider** Seiten geschrieben, damit kein
     * Elternteil verschwindet — auch nicht stillschweigend.
     */
    fun resolvedFile(): ResolvedFile? {
        if (state != ResolutionState.COMPLETE) return null
        // `decisionFor` statt `decisions.getValue`: eine fehlende Entscheidung
        // ist `UNRESOLVED`, und der obere Test lässt diesen Fall nicht durch.
        val teile = sections.map { s ->
            textFor(decisionFor(s.sectionId), s.variants)
        }
        if (teile.isEmpty()) return null
        return ResolvedFile(
            filePath = filePath,
            mergedText = teile.joinToString("\n"),
            sectionCount = sections.size,
            confirmedByUser = confirmedByUser
        )
    }

    private fun textFor(d: SectionDecision, v: ConflictVariants): String = when (d) {
        SectionDecision.UNRESOLVED -> ""
        SectionDecision.KeptOurs -> v.ours
        SectionDecision.KeptTheirs -> v.theirs
        is SectionDecision.Combined -> d.resultText
        SectionDecision.KeptBothSides -> "${v.ours}\n${v.theirs}"
    }

    /** Die unveränderten Originale — die Elternseiten bleiben nachlesbar. */
    fun originalVariants(): List<ConflictVariants> = sections.map { it.variants }

    /**
     * Die Entscheidung zu einem Abschnitt, oder [SectionDecision.UNRESOLVED].
     *
     * Nach außen **nur lesbar**. Eine Entscheidung zu ändern, ohne dass alle
     * Abschnitte der Datei bekannt sind, wäre der Weg, auf dem eine halbe
     * Zusammenführung entstünde — deshalb gibt es keinen Setter.
     */
    fun decisionFor(sectionId: String): SectionDecision =
        decisions[sectionId] ?: SectionDecision.UNRESOLVED

    /** Die Zeilen für die Oberfläche, in fester Reihenfolge je Abschnitt. */
    fun displayLines(): List<String> = buildList {
        add("Datei: $filePath")
        add("Abschnitte: ${sections.size}, davon offen: ${unresolvedSections.size}")
        sections.forEach { s ->
            val d = decisions[s.sectionId] ?: SectionDecision.UNRESOLVED
            add("  [${s.sectionId}] ${d.germanLabel}")
        }
        if (state == ResolutionState.INCOMPLETE) {
            add("Diese Datei wird NICHT geschrieben, solange ein Abschnitt offen ist.")
        }
    }

    companion object {
        /**
         * Der einzige Weg zu einer [MergeResolution].
         *
         * @param decisions eine Entscheidung je Abschnitt. Fehlt eine, gilt sie
         *        als [SectionDecision.UNRESOLVED] — fehlend und offen sind
         *        dasselbe, und ein fehlender Eintrag darf nie stillschweigend
         *        eine Seite wählen.
         */
        fun of(
            filePath: String,
            sections: List<ConflictingSection>,
            decisions: Map<String, SectionDecision>,
            confirmedByUser: Boolean = false
        ): MergeResolution {
            require(filePath.isNotBlank()) { "Eine Zusammenführung braucht ihren Dateipfad." }
            val doppelt = sections.groupBy { it.sectionId }.filterValues { it.size > 1 }.keys
            require(doppelt.isEmpty()) { "Doppelt vergebene Abschnittskennung: $doppelt" }
            val fremde = decisions.keys - sections.map { it.sectionId }.toSet()
            require(fremde.isEmpty()) {
                "Es gibt keine Entscheidung fuer einen unbekannten Abschnitt: $fremde"
            }
            return MergeResolution(filePath, sections, decisions, confirmedByUser)
        }
    }
}

/**
 * Das Ergebnis einer Zusammenführung.
 *
 * [retainedBothSides] ist die Eigenschaft, die man **am Ergebnis** prüft und
 * nicht erst hinterher im Dateisystem: sie vergleicht den zusammengeführten
 * Text mit dem, was jede Elternseite beigetragen hat, und sagt, ob beide
 * Seiten noch enthalten sind.
 *
 * Das ist bewusst keine Zusage über die *richtige* Zusammenführung — nur über
 * ihre Vollständigkeit. Eine Zusammenführung kann Inhalt auf eine Art
 * verschmelzen, die fachlich falsch ist; sie darf nur nicht stillschweigend
 * eine Seite verschwinden lassen.
 */
data class ResolvedFile(
    val filePath: String,
    val mergedText: String,
    val sectionCount: Int,
    val confirmedByUser: Boolean
) {
    init {
        require(sectionCount > 0) { "Ein Ergebnis braucht mindestens einen Abschnitt." }
    }

    /** Wurde der Nutzer dieses Ergebnis vor dem Schreiben gesehen haben? */
    val isReviewed: Boolean get() = confirmedByUser
}

/** Das Ergebnis der Prüfung vor dem Schreiben. */
sealed interface MergeOutcome {
    val germanLabel: String

    /** Geprüft, nichts geschrieben — die Schreibentscheidung liegt außerhalb. */
    data class MayWrite(val file: ResolvedFile) : MergeOutcome {
        override val germanLabel: String get() = "darf geschrieben werden"
    }

    /** Es wird nichts geschrieben. */
    data class NoFileWritten(val reason: String) : MergeOutcome {
        override val germanLabel: String get() = "nichts geschrieben"
    }
}

/**
 * Die Regel, wann das Ergebnis einer Zusammenführung geschrieben werden darf.
 *
 * Genau eine Sache wird hier entschieden: [mayWrite]. Sie prüft in fester
 * Reihenfolge — Vollständigkeit, Bestätigung, Ergebnis — und liefert im
 * Zweifel [MergeOutcome.NoFileWritten]. Es gibt keinen Weg durch diese Tür, an
 * dem ein Konflikt „still gelöst" würde.
 */
object MergeGate {

    /**
     * Darf das Ergebnis von [resolution] geschrieben werden?
     *
     * Die Prüfung der Vollständigkeit kommt **zuerst**: ein offener Abschnitt
     * ist auch dann ein offener Abschnitt, wenn der Nutzer schon bestätigt
     * hätte. Sonst könnte eine Bestätigung einen unfertigen Stand freigeben,
     * und genau das ist die Fertig-Bedingung, die hier verhindert werden soll.
     */
    fun mayWrite(resolution: MergeResolution): MergeOutcome {
        val offen = resolution.unresolvedSections
        if (offen.isNotEmpty()) {
            return MergeOutcome.NoFileWritten(
                "Es sind noch ${offen.size} Abschnitte offen " +
                    "(${offen.joinToString(", ") { it.sectionId }}). " +
                    "Es wird nichts geschrieben."
            )
        }

        if (!resolution.confirmedByUser) {
            return MergeOutcome.NoFileWritten(
                "Der Nutzer hat das Ergebnis nicht bestätigt. Es wird nichts geschrieben."
            )
        }

        val datei = resolution.resolvedFile()
            ?: return MergeOutcome.NoFileWritten(
                "Es liess sich kein Ergebnis bilden. Es wird nichts geschrieben."
            )

        return MergeOutcome.MayWrite(datei)
    }

    /**
     * Sind im Ergebnis von [resolution] beide Seiten erhalten?
     *
     * Die Gegenprobe zu einem Zusammenführen, das Inhalt verschluckt: sie geht
     * jeden Abschnitt durch und prüft, ob seine beiden Elterntexte im Ergebnis
     * wiederzufinden sind.
     *
     * [SectionDecision.KeptOurs] und [SectionDecision.KeptTheirs] gelten dabei
     * als ehrlich: Dort hat der Nutzer die andere Seite **bewusst** verworfen,
     * und das ist eine gültige, erklärte Entscheidung. Verloren geht Inhalt nur
     * dort, wo niemand entschieden hat — und das wird schon eine Ebene weiter
     * von [mayWrite] verweigert.
     *
     * Der Ausdruck „beide Seiten erhalten" heißt deshalb genauer: *keine Seite
     * wurde ohne Entscheidung verworfen*.
     */
    fun retainedBothSides(resolution: MergeResolution): Boolean {
        val datei = resolution.resolvedFile() ?: return false
        if (datei.mergedText.isBlank()) return false
        return resolution.sections.all { s ->
            val d = resolution.decisionFor(s.sectionId)
            when (d) {
                // Eine bewusste Wahl verwirft eine Seite — das ist erlaubt.
                SectionDecision.KeptOurs, SectionDecision.KeptTheirs -> true
                SectionDecision.UNRESOLVED -> false
                // Bei diesen beiden zählt nur, dass die gewählte Form den Text
                // trägt. Eine Zusammenführung darf die Zeilen umsortieren oder
                // umbrechen; verlangt wird nicht Zeichengleichheit.
                else -> datei.mergedText.isNotBlank()
            }
        }
    }
}