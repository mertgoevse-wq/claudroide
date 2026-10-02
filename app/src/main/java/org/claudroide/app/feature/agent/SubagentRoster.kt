package org.claudroide.app.feature.agent

/**
 * Task 132 — „Spezialhelfer“.
 *
 * Ziel: Optionale Helfer für Recherche, Prüfung oder Teilaufgaben einsetzen.
 * Ergebnis: Rollen, Aufgabenbereiche, Eingaben, erwartete Ergebnisse und
 * Abbruchregeln.
 *
 * Der Typ verankert die tragende Zusage der Aufgabe als **fehlenden Pfad**:
 *
 * > **Ein Helferergebnis ohne mitgeführten Beleg kann nicht zusammengeführt
 * > werden.**
 *
 * Der Satz ist nicht im Klartext behauptet, sondern so gebaut, dass ein
 * gültiger Aufruf ihn nicht umgehen kann:
 *
 *  - [VerifiedHelperFinding] hat einen **privaten** Konstruktor. Der einzige
 *    öffentliche Weg ist [VerifiedHelperFinding.of], und der liefert ein
 *    [Verification] zurück — bei einem Beleg ohne Herkunft ein `Unverifiable`
 *    mit Grund, **nie** ein Finding.
 *  - [HelperEvidence] ist ein `sealed interface` mit genau **einem** Fall, der
 *    überhaupt trägt: [HelperEvidence.SourceCited], und der muss Quelle **und**
 *    Fundstelle beim Namen nennen. Ein Aufzählungswert `VERIFIED` wäre eine
 *    Behauptung — ein Name ist kein Nachweis. Genau dieses Muster hat
 *    Aufgabe 129 für [org.claudroide.app.feature.skills.ContentEvidence]
 *    eingeführt; hier wird es für Helferergebnisse wiederholt.
 *  - [SubagentRoster.merge] nimmt **ausschließlich** Findings. Ein
 *    [HelperClaim] ohne Beleg hat keinen Weg hinein, auch nicht über einen
 *    Default-Parameter.
 *
 * Die übrigen Zusagen stehen an der Stelle, an der sie sonst nur eine
 * Vereinbarung wären:
 *
 *  1. **Schutz: nur das Nötige.** [SubagentRoster.grantAccess] verweigert zwei
 *     Arten von Zugriff — was die Rolle nicht deckt ([AccessRefusal.OUTSIDE_ROLE_CEILING])
 *     und was der Auftrag nicht nennt ([AccessRefusal.NOT_IN_ASSIGNED_SCOPE]).
 *     `Granted` trägt **nur** den Schnittmenge; es gibt keine Rückgabe, aus der
 *     mehr herausfällt als beantragt.
 *  2. **Parallele Arbeit ist erkennbar.** [SubagentRoster.activity] liefert
 *     [ParallelActivity], und `isParallel` folgt aus der Zahl der laufenden
 *     Helfer — nicht aus einem gemeldeten Gefühl.
 *  3. **Kostenwahrheit.** [ExtraCost] ist ein `sealed interface` mit einem Fall,
 *     der **kein** Zahlenfeld hat: [ExtraCost.Open] trägt nur einen Grund. Ohne
 *     Preisbeleg entsteht keine Zahl, sondern eine offene Angabe — so wie
 *     [org.claudroide.app.feature.provider.CostEstimate.Incomplete] ihre
 *     `amountUsd` auf `null` lässt. [SubagentRoster.costOf] rechnet die
 *     Helferzahl **nie** in einen Betrag um. Die Typen dazu stehen in
 *     `SubagentRunReport.kt`.
 *  4. **Abbruch meldet keinen Teilzustand als Erfolg.** [HelperRunState] ist ein
 *     `sealed interface`; [HelperRunState.Aborted] und [HelperRunState.Failed]
 *     haben **kein** Ergebnisfeld. Es gibt keine Konstruktion „halb fertig, aber
 *     Erfolg".
 *  5. **Widersprüche bleiben sichtbar.** [SubagentRoster.contradictions] vergleicht
 *     Befunde über denselben Gegenstand und behält beide. [SubagentRoster.merge]
 *     stellt die Widerspruchszeilen **vor** das Fazit und markiert das Ergebnis
 *     als strittig ([MergeReport.isContested]).
 *
 * Diese Datei enthält Rollen, Zuweisung, Zugriff, Beleg und Zusammenführung.
 * Was der Nutzer über einen laufenden Lauf **sieht** — Parallelität, Kosten und
 * den Bericht — steht in `SubagentRunReport.kt`.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz. Diese Klasse
 * entscheidet, was ein Helfer bekommt, was sein Ergebnis tragen muss und was
 * daraus in den Hauptlauf darf. Sie startet nichts und spricht niemanden an.
 */

/** Die Rollen, die ein Helfer haben kann. */
enum class HelperRole(val label: String, val germanLabel: String) {

    /** Sucht und sammelt. Liest, startet Tests, ändert nichts. */
    RESEARCH("Research helper", "Recherchehelfer") {
        override val allowedTools: Set<HelperTool> = HelperTool.readOnly + HelperTool.RUN_TEST
    },

    /** Prüft ein fertiges Ergebnis. Liest, schreibt nichts. */
    REVIEW("Review helper", "Prüfhelfer") {
        override val allowedTools: Set<HelperTool> = HelperTool.readOnly
    },

    /** Übernimmt eine abgegrenzte Teilaufgabe. Darf im Auftrag schreiben. */
    SUBTASK("Subtask helper", "Teilaufgabenhelfer") {
        override val allowedTools: Set<HelperTool> = HelperTool.readOnly +
            HelperTool.WRITE_FILE + HelperTool.RUN_TEST
    };

    /**
     * Die Werkzeuge, die diese Rolle **jemals** halten darf.
     *
     * Die Obergrenze der Rolle, nicht die des Helfers: Sie gilt auch dann, wenn
     * jemand mehr beantragt. Deshalb steht sie hier am Rollenwert und nicht in
     * einer [HelperSpec] — eine Rolle, die sich selbst erweitert, wäre keine
     * Obergrenze.
     */
    abstract val allowedTools: Set<HelperTool>
}

/** Die Werkzeuge, die ein Helfer halten kann. */
enum class HelperTool(val label: String, val germanLabel: String) {

    READ_FILE("Read file", "Datei lesen"),
    LIST_DIRECTORY("List folder", "Ordner auflisten"),
    SEARCH_TEXT("Search text", "Text suchen"),
    RUN_TEST("Run tests", "Tests starten"),
    WRITE_FILE("Change file", "Datei ändern"),
    DELETE_FILE("Delete file", "Datei löschen"),
    RUN_COMMAND("Run command", "Befehl ausführen"),
    NETWORK_CALL("Send data to a provider", "Daten an einen Anbieter senden"),
    USE_API_KEY("Use access key", "Zugangsschlüssel verwenden"),
    INSTALL_DEPENDENCY("Install dependency", "Abhängigkeit installieren"),
    GIT_PUSH("Upload to a server", "Git-Upload");

    /**
     * Weitet den Zugriff über das reine Lesen hinaus.
     *
     * Genau diese Werkzeuge sind es, an denen der Schutz der Aufgabe hängt, und
     * genau deshalb sind sie von den lesenden getrennt gezählt.
     */
    val widensScope: Boolean
        get() = this !in readOnly

    companion object {

        /**
         * Das Lesen, das jede Rolle darf.
         *
         * Es steht hier und **nicht** im Companion von [HelperRole]: Die Enum-
         * Entries werden vor dem Companion-Objekt ihrer eigenen Aufzählung
         * ausgewertet, ein Verweis von dort nach oben trifft also eine noch leere
         * Menge. Es steht **bewusst** nicht neben [WRITE_FILE] und [NETWORK_CALL]:
         * Recherche liest, sie startet nichts und installiert nichts. Ein Helfer,
         * der nur lesen soll, bekommt genau diese Menge.
         */
        val readOnly: Set<HelperTool> = setOf(READ_FILE, LIST_DIRECTORY, SEARCH_TEXT)
    }
}

/**
 * Eine Eingabe an einen Helfer.
 *
 * Beide Teile sind Pflicht. Ein Helfer ohne benannte Eingabe hat nichts, woran
 * er arbeitet — und ein leerer Wert würde später als „gearbeitet, aber nichts
 * gefunden" erscheinen, was die Aufgabe nicht meint.
 */
data class HelperInput(val name: String, val value: String) {
    init {
        require(name.isNotBlank()) { "Eine Eingabe braucht ihren Namen." }
        require(value.isNotBlank()) { "Die Eingabe $name braucht einen Wert." }
    }
}

/** Warum ein Zugriff abgelehnt wurde. */
enum class AccessRefusal(val label: String, val germanLabel: String) {

    /** Die Rolle deckt dieses Werkzeug nicht ab — auch nicht auf Verlangen. */
    OUTSIDE_ROLE_CEILING("Outside the role's ceiling", "außerhalb der Rollen-Obergrenze"),

    /** Der Auftrag nennt dieses Werkzeug nicht. */
    NOT_IN_ASSIGNED_SCOPE("Not part of the assignment", "nicht Teil des Auftrags")
}

/**
 * Das Ergebnis einer Zugriffsanfrage.
 *
 * `sealed interface`, damit „verweigert" **kein** Sonderfall in einem
 * Attrappen-Objekt ist: Es gibt keinen Weg, einen verweigerten Zugriff als
 * gewährten zu verpacken.
 */
sealed interface AccessOutcome {

    /** Die gehaltenen Werkzeuge — bei einer Verweigerung leer. */
    val tools: List<HelperTool>

    /** Wurde der Zugriff gewährt? */
    val isGranted: Boolean

    /** Gewährt, und nur das, was auch wirklich beantragt war. */
    data class Granted(override val tools: List<HelperTool>) : AccessOutcome {
        override val isGranted: Boolean get() = true
    }

    /**
     * Abgelehnt.
     *
     * [refusals] nennt **jeden** verweigerten Punkt, nicht nur den ersten: Eine
     * Anfrage, die drei Werkzeuge zu viel enthält, soll der Nutzer vollständig
     * sehen, nicht Punkt für Punkt nachreichen.
     */
    data class Refused(val refusals: List<RefusedTool>) : AccessOutcome {
        override val tools: List<HelperTool> get() = emptyList()
        override val isGranted: Boolean get() = false
    }
}

/** Ein einzelnes abgelehntes Werkzeug, mit seinem Grund. */
data class RefusedTool(val tool: HelperTool, val refusal: AccessRefusal) {

    /** Die Zeile für die Oberfläche. */
    fun line(): String = "${tool.germanLabel} — ${refusal.germanLabel}"
}

/**
 * Die Zuweisung **eines** Helfers.
 *
 * Die Konstruktoren sind `internal`: außerhalb dieser Datei kann niemand eine
 * Zuweisung bauen, um sie an einen Aufruf vorbeizubringen. Der einzige
 * öffentliche Weg ist [SubagentRoster.assign], und der prüft, dass Auftrag,
 * Eingaben, erwartetes Ergebnis und Abbruchregeln wirklich benannt sind.
 *
 * @property role welche Art von Helfer das ist.
 * @property helperId die Kennung, unter der das Ergebnis später ankommt.
 * @property objective der Auftrag in einem Satz.
 * @property inputs was der Helfer bekommt — mindestens eine.
 * @property expectedResult was als Antwort herauskommen **muss**.
 * @property abortRules was gilt, wenn er abbricht oder ausfällt.
 * @property grantedTools die tatsächlich gehaltenen Werkzeuge.
 */
data class HelperSpec internal constructor(
    val helperId: String,
    val role: HelperRole,
    val objective: String,
    val inputs: List<HelperInput>,
    val expectedResult: List<String>,
    val abortRules: List<String>,
    val grantedTools: List<HelperTool>
) {

    /** Der Auftrag, wie er in der Anzeige erscheint. */
    fun assignmentLine(): String = "${role.germanLabel} $helperId: $objective"

    /** Die Zeilen für die Oberfläche: Auftrag, Eingaben, Erwartung, Rechte. */
    fun explanationLines(): List<String> = buildList {
        add(assignmentLine())
        add("  Eingaben: ${inputs.joinToString(", ") { it.name }}")
        add("  Erwartetes Ergebnis: ${expectedResult.joinToString("; ")}")
        add("  Werkzeuge: ${grantedTools.joinToString(", ") { it.germanLabel }}")
        add("  Bei Abbruch: ${abortRules.joinToString("; ")}")
    }
}

/**
 * Der Beleg, auf den sich ein Helferbefund stützt.
 *
 * `sealed interface` mit genau **einem** tragenden Fall. Der Grund ist derselbe
 * wie bei [org.claudroide.app.feature.skills.ContentEvidence]: Ein Aufzählungswert
 * `VERIFIED` wäre eine Behauptung. Ein Name ist kein Nachweis. Deshalb gibt es
 * [SourceCited] — das die Quelle **mitführen muss** — und daneben nur
 * [AssertionWithoutSource], das ehrlich sagt, dass keine Herkunft vorliegt.
 */
sealed interface HelperEvidence {

    /** Nennt der Beleg eine überprüfbare Herkunft? */
    val isCheckable: Boolean

    /**
     * Die Quelle — **mit** dem Ort, an dem sie steht.
     *
     * Beide Teile sind Pflicht: „in irgendeiner Datei" ist kein Fundort, und
     * ein Dateiname ohne Stelle darin wäre ebensowenig einer. Deshalb liegt
     * `source` **nicht** als Feld im Interface — ein Interface-Feld müsste
     * [AssertionWithoutSource] eine leere Herkunft vortäuschen, die es gar nicht
     * hat. Der zweite Fall trägt deshalb gar keine Herkunft.
     */
    data class SourceCited(val source: String, val locator: String) : HelperEvidence {
        init {
            require(source.isNotBlank()) { "Ein Fund braucht eine benannte Quelle." }
            require(locator.isNotBlank()) { "Ein Fund braucht einen Ort in der Quelle." }
        }

        override val isCheckable: Boolean get() = true
    }

    /** Die Behauptung steht da, aber niemand kann sie nachprüfen. */
    data class AssertionWithoutSource(val claim: String) : HelperEvidence {
        init {
            require(claim.isNotBlank()) { "Eine Behauptung ohne Beleg braucht wenigstens ihren Text." }
        }

        override val isCheckable: Boolean get() = false
    }
}

/** Was ein Helfer behauptet — mit oder ohne Beleg. */
data class HelperClaim(
    val helperId: String,
    val statement: String,
    val evidence: HelperEvidence
) {
    init {
        require(helperId.isNotBlank()) { "Ein Helferbefund braucht die Kennung seines Helfers." }
        require(statement.isNotBlank()) { "Ein Helferbefund braucht einen Text." }
    }

    /** Trägt dieser Befund einen überprüfbaren Beleg? */
    val isCheckable: Boolean get() = evidence.isCheckable
}

/** Warum ein Helferbefund nicht überprüfbar ist. */
enum class UnverifiableReason(val label: String, val germanLabel: String) {

    /** Der Helfer hat nichts angegeben, woran man nachprüfen könnte. */
    NO_SOURCE_NAMED("No source named", "keine Quelle genannt"),

    /** Der Gegenstand, um den es geht, wurde nicht benannt. */
    NO_SUBJECT_NAMED("No subject named", "kein Gegenstand genannt")
}

/**
 * Das Ergebnis der Prüfung, ob ein Helferbefund trägt.
 *
 * `sealed interface`, damit „trägt nicht" kein Fehler und kein Sonderfall ist,
 * sondern ein regulärer Ausgang mit Grund.
 */
sealed interface Verification {

    /** Trägt der Befund? */
    val isVerifiable: Boolean

    /** Geprüft — und der Beleg ist mitgeführt. */
    data class Verifiable(val finding: VerifiedHelperFinding) : Verification {
        override val isVerifiable: Boolean get() = true
    }

    /** Trägt nicht — und sagt warum. */
    data class Unverifiable(val reason: UnverifiableReason) : Verification {
        override val isVerifiable: Boolean get() = false
    }
}

/**
 * **Ein** belegter Helferbefund — die einzige Form, in der ein Helferergebnis
 * zusammengeführt werden darf.
 *
 * Der Konstruktor ist **privat**. Es gibt keinen öffentlichen Weg, diese Klasse
 * zu bauen: der einzige führt über [of] und liefert bei einem Befund ohne
 * Herkunft ein [Verification.Unverifiable] mit Grund. Das ist die tragende
 * Zusage der Aufgabe als fehlender Pfad — [SubagentRoster.merge] nimmt nur
 * dieses hier, und dieses hier kann es nur auf eine Art erwerben.
 *
 * @property subject der Gegenstand, um den es geht — **benannt**.
 * @property verdict was der Helfer dazu sagt.
 * @property evidence der Beleg, **mitgeführt**.
 */
data class VerifiedHelperFinding private constructor(
    val helperId: String,
    val subject: String,
    val verdict: String,
    val evidence: HelperEvidence.SourceCited
) {

    /** Die Fundstelle für die Anzeige und den Nachprüfbericht. */
    fun citationLine(): String = "${evidence.source} · ${evidence.locator}"

    /** Die Zeile für die Oberfläche. */
    fun summaryLine(): String = "$subject: $verdict (${evidence.locator})"

    companion object {

        /**
         * Prüft einen [HelperClaim] und liefert ihn als [VerifiedHelperFinding]
         * zurück — **oder** nennt den Grund, warum das nicht geht.
         *
         * Die Reihenfolge der Prüfung ist die Aussage: erst der Gegenstand, dann
         * die Herkunft. Fehlt beides, ist „kein Gegenstand genannt" die ehrlichere
         * Angabe, weil sie schon am Anfang des Problems liegt.
         */
        fun of(claim: HelperClaim, subject: String): Verification = when {
            subject.isBlank() -> Verification.Unverifiable(UnverifiableReason.NO_SUBJECT_NAMED)
            claim.evidence !is HelperEvidence.SourceCited ->
                Verification.Unverifiable(UnverifiableReason.NO_SOURCE_NAMED)

            else -> Verification.Verifiable(
                VerifiedHelperFinding(
                    helperId = claim.helperId,
                    subject = subject,
                    verdict = claim.statement,
                    evidence = claim.evidence
                )
            )
        }
    }
}

/**
 * Der Zustand, in dem ein Helfer endet.
 *
 * `sealed interface`, und das ist hier die ganze Abbruchregel: [Aborted] und
 * [Failed] haben **kein Ergebnisfeld**. Es gibt keine Konstruktion für „halb
 * fertig, aber als Erfolg gemeldet" — der Zustand *kann* einen Teilergebnis
 * nicht transportieren, also muss der Aufrufer es auch nicht filtern.
 */
sealed interface HelperRunState {

    /** Die Kennung des Helfers, um den es geht. */
    val helperId: String

    /**
     * Was dieser Abschluss als Ergebnis mitbringt.
     *
     * Nur [Finished] liefert eines. Alle anderen Zustände liefern `null` —
     * und zwar aus dem Typ heraus, nicht aus einer Prüfung des Aufrufers.
     */
    val claim: HelperClaim?

    /** Lief der Helfer zu Ende durch? */
    val isFinished: Boolean

    /** Fertig gelaufen. Trägt genau eine Behauptung, belegt oder nicht. */
    data class Finished(
        override val helperId: String,
        override val claim: HelperClaim
    ) : HelperRunState {
        override val isFinished: Boolean get() = true
    }

    /** Vom Nutzer oder wegen eines Limits abgebrochen. */
    data class Aborted(
        override val helperId: String,
        val reason: String
    ) : HelperRunState {
        init {
            require(reason.isNotBlank()) { "Ein Abbruch braucht seinen Grund." }
        }

        override val claim: HelperClaim? get() = null
        override val isFinished: Boolean get() = false
    }

    /** Aus einem Fehler heraus gescheitert. */
    data class Failed(
        override val helperId: String,
        val reason: String
    ) : HelperRunState {
        init {
            require(reason.isNotBlank()) { "Ein Ausfall braucht seinen Grund." }
        }

        override val claim: HelperClaim? get() = null
        override val isFinished: Boolean get() = false
    }
}

/**
 * Vergibt Spezialhelfer, begrenzt ihren Zugriff, führt ihre Ergebnisse
 * zusammen und beziffert ihre Kosten nicht.
 *
 * Alle Regeln sind **reines Kotlin**: Die App liefert die Fakten, dieses Objekt
 * entscheidet, was daraus folgt. Es startet keinen Helfer, öffnet keine Datei
 * und spricht keinen Anbieter an.
 */
object SubagentRoster {

    /**
     * Muss der Nutzer über Helfer unterrichtet werden?
     *
     * Konstant `true`, und genau deshalb hier statt in einer Anzeige: Helfer
     * erzeugen Arbeit, die der Nutzer sonst nicht sieht, und im ungünstigsten
     * Fall Kosten, die er nicht erwartet hat. Ein Aufrufer, der diese Abfrage
     * einmal überspringt, verliert den Nachweis, dass er es nicht überspringen
     * musste.
     */
    fun requiresUserNotice(): Boolean = true

    /**
     * Weist **einen** Helfer zu.
     *
     * Der einzige öffentliche Weg zu einer [HelperSpec] — die Konstruktoren
     * sind `internal`. Der Auftrag, die Eingaben, das erwartete Ergebnis und
     * die Abbruchregeln sind Pflicht: Ein Helfer, dessen Ergebnis niemand
     * benannt hat, liefert am Ende etwas, das niemand einordnen kann.
     *
     * @param grantedTools was der Helfer tatsächlich hält. Hier steht bewusst
     *        **nicht** die Rollen-Obergrenze: Die prüft [grantAccess].
     */
    fun assign(
        helperId: String,
        role: HelperRole,
        objective: String,
        inputs: List<HelperInput>,
        expectedResult: List<String>,
        abortRules: List<String>,
        grantedTools: List<HelperTool>
    ): HelperSpec {
        require(helperId.isNotBlank()) { "Ein Helfer braucht eine Kennung." }
        require(objective.isNotBlank()) { "Ein Helfer braucht einen Auftrag." }
        require(inputs.isNotEmpty()) { "Ein Helfer ohne Eingabe hat nichts zu tun." }
        require(expectedResult.any { it.isNotBlank() }) { "Ein Helfer braucht ein erwartetes Ergebnis." }
        require(abortRules.any { it.isNotBlank() }) { "Ein Helfer braucht eine Abbruchregel." }
        return HelperSpec(
            helperId = helperId,
            role = role,
            objective = objective,
            inputs = inputs,
            expectedResult = expectedResult.filter { it.isNotBlank() },
            abortRules = abortRules.filter { it.isNotBlank() },
            grantedTools = grantedTools.distinct()
        )
    }

    /**
     * Prüft einen Zugriffswunsch gegen **beide** Grenzen.
     *
     * Erst gegen den Auftrag: was nicht im Auftrag steht, wird nicht
     * gebraucht. Dann gegen die Rolle: was die Rolle nicht deckt, wird auch
     * mit Auftrag nicht gedeckt. [AccessOutcome.Granted] trägt ausschließlich
     * die Werkzeuge, die **beide** Prüfungen bestanden haben — es kann nicht
     * mehr herausgeben als beantragt.
     */
    fun grantAccess(
        spec: HelperSpec,
        requested: List<HelperTool>,
        toolsInAssignment: List<HelperTool>
    ): AccessOutcome {
        val verweigert = requested.distinct().mapNotNull { tool ->
            when {
                tool !in toolsInAssignment -> RefusedTool(tool, AccessRefusal.NOT_IN_ASSIGNED_SCOPE)
                tool !in spec.role.allowedTools -> RefusedTool(tool, AccessRefusal.OUTSIDE_ROLE_CEILING)
                else -> null
            }
        }
        if (verweigert.isNotEmpty()) return AccessOutcome.Refused(verweigert)
        return AccessOutcome.Granted(requested.distinct())
    }

    /** Was gerade parallel passiert. */
    fun activity(running: List<RunningHelper>): ParallelActivity = ParallelActivity(running)

    /**
     * Die zusätzlichen Modellkosten durch Helfer.
     *
     * **Die Zahl der Helfer wird nie in einen Betrag umgerechnet.** Ohne
     * [evidence] entsteht [ExtraCost.Open] mit dem Grund
     * [CostOpenReason.NO_PRICE_SOURCE] — nicht eine Schätzung. Mit einer
     * [PriceEvidence] ohne [tokens] bleibt es offen, weil dann die Menge
     * fehlt, die man rechnen müsste.
     */
    fun costOf(
        running: List<RunningHelper>,
        evidence: PriceEvidence? = null,
        tokens: Int? = null,
        usd: String? = null
    ): ExtraCost {
        val modellarbeit = running.count { it.hasStartedModelWork }
        if (evidence == null) {
            return ExtraCost.Open(
                if (modellarbeit == 0) CostOpenReason.NO_PRICE_SOURCE else CostOpenReason.MODEL_ROUTING_UNKNOWN
            )
        }
        if (tokens == null || usd == null) return ExtraCost.Open(CostOpenReason.MODEL_ROUTING_UNKNOWN)
        return ExtraCost.Billed(
            TokenEvidence(
                tokens = tokens,
                usd = usd,
                sourceUrl = evidence.sourceUrl,
                verifiedDate = evidence.verifiedDate
            )
        )
    }

    /** Führt einen Helferabschluss durch die Belegprüfung. */
    fun verify(state: HelperRunState, subject: String): Verification {
        val behauptung = state.claim ?: return Verification.Unverifiable(UnverifiableReason.NO_SOURCE_NAMED)
        return VerifiedHelperFinding.of(behauptung, subject)
    }

    /**
     * Der Grund, warum ein Helfer ohne Ergebnis dasteht.
     *
     * Leer bei [HelperRunState.Finished] — dort gab es nichts zu beanstanden.
     * [HelperRunState.Aborted] und [HelperRunState.Failed] nennen ihren Grund
     * wörtlich, damit die Anzeige „abgebrochen" nicht wie „nichts gefunden"
     * aussieht. Die beiden sind verschiedene Tatsachen und werden hier nicht
     * zusammengefasst.
     */
    fun nonDeliveryReason(state: HelperRunState): String = when (state) {
        is HelperRunState.Finished -> ""
        is HelperRunState.Aborted -> "abgebrochen: ${state.reason}"
        is HelperRunState.Failed -> "ausgefallen: ${state.reason}"
    }

    /**
     * Findet Widersprüche zwischen belegten Befunden.
     *
     * Verglichen wird **nur** der Bezug, nicht die Übereinstimmung im Wortlaut:
     * Zwei Helfer, die über denselben Gegenstand Verschiedenes sagen, stehen
     * nebeneinander, bis ein Mensch entscheidet. Das Wegwählen eines „besseren"
     * Ergebnisses wäre genau die stille Ersetzung, die die Aufgabe ausschließt.
     */
    fun contradictions(findings: List<VerifiedHelperFinding>): List<Contradiction> {
        val widersprueche = mutableListOf<Contradiction>()
        findings.forEachIndexed { index, erster ->
            findings.drop(index + 1).forEach { zweiter ->
                if (erster.subject == zweiter.subject && erster.verdict != zweiter.verdict) {
                    widersprueche += Contradiction(
                        subject = erster.subject,
                        firstHelperId = erster.helperId,
                        secondHelperId = zweiter.helperId,
                        firstVerdict = erster.verdict,
                        secondVerdict = zweiter.verdict
                    )
                }
            }
        }
        return widersprueche
    }

    /**
     * Führt die Ergebnisse mehrerer Helfer zusammen.
     *
     * Nimmt **ausschließlich** [Verification.Verifiable]. Eine
     * [Verification.Unverifiable] wird zu einer namentlich genannten offenen
     * Stelle, nicht zu einem stillschweigenden Ersatz des Hauptergebnisses. Ein
     * [HelperRunState], der kein Ergebnis mitbringt, landet in
     * [MergeReport.droppedHelpers] — mit Grund, damit ein Abbruch nicht wie
     * ein leerer Fund aussieht.
     */
    fun merge(
        states: List<HelperRunState>,
        subjectOf: (HelperRunState) -> String
    ): MergeReport {
        val belegt = mutableListOf<VerifiedHelperFinding>()
        val unbelegt = mutableListOf<String>()
        val ausgefallen = mutableListOf<String>()

        states.forEach { zustand ->
            when (val pruefung = verify(zustand, subjectOf(zustand))) {
                is Verification.Verifiable -> belegt += pruefung.finding
                is Verification.Unverifiable -> {
                    if (zustand.isFinished) {
                        unbelegt += "${zustand.helperId} (${pruefung.reason.germanLabel})"
                    } else {
                        ausgefallen += "${zustand.helperId} (${nonDeliveryReason(zustand)})"
                    }
                }
            }
        }
        return MergeReport(
            mergedFindings = belegt,
            contradictions = contradictions(belegt),
            unverifiedSubjects = unbelegt,
            droppedHelpers = ausgefallen
        )
    }

    /** Der Satz, den die Aufgabe an dieser Stelle verlangt. */
    fun mergeMeaningLine(): String =
        "Ein Helferergebnis wird nur übernommen, wenn der Helfer seinen Beleg " +
            "mitnennt. Ohne Herkunft bleibt es sichtbar offen."
}
