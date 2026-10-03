package org.claudroide.app.feature.agent

import org.claudroide.app.core.security.PermissionCategory
import org.claudroide.app.feature.project.PathBoundaryGuard

/**
 * Task 133 — „Helferrechte“.
 *
 * Ziel: Jeder Spezialhelfer bekommt nur die Werkzeuge und Projektteile seiner
 * Aufgabe. Ergebnis: Rechtematrix für Lesen, Schreiben, Befehle, Netzwerk,
 * Anbieter und gemeinsame Daten.
 *
 * Diese Datei stellt **nicht** noch einmal Rechte bereit, sondern sagt, welche
 * Frage beim Weg zu einem Recht **nein** heißen kann. Die Rechte selbst hat
 * [HelperRole] aus Aufgabe 132; hier wird entschieden, ob eine Rolle ein Recht
 * überhaupt erreichen darf und unter welcher Bedingung.
 *
 * Die drei Zusagen der Aufgabe stehen an der Stelle, an der sie sonst nur eine
 * Vereinbarung wären:
 *
 *  1. **Schreibrechte gehen nicht an reine Prüfer.** Die Matrix ist eine
 *     Tabelle aus [HelperRole] × [HelperRightScope] mit einer Zelle für **jede**
 *     Kombination — kein Default, kein „für die übrigen gilt dasselbe". Für
 *     [HelperRole.REVIEW] ist die Zeile `WRITE` fest `DENIED_BY_DESIGN`. Diese
 *     Zelle wird geprüft, **bevor** irgendein anderes Argument eine Rolle
 *     spielen kann, und zwar an derselben Stelle, an der auch eine Nutzerzustimmung
 *     noch nicht gelesen wurde. Damit kann keine Zustimmung, kein Auftrag und
 *     kein Aufrufer ein Schreibrecht in einen Prüfer hineinholen: Ein
 *     ausdrücklich gewilligter Prüfer ist keine Ausnahme, sondern eine andere
 *     Aufgabe. Wer prüfen **und** ändern soll, wird als [HelperRole.SUBTASK]
 *     zugewiesen — als sichtbare Rollenentscheidung, nicht als stille
 *     Rechteerweiterung.
 *  2. **Der Hauptagent erweitert seine Rechte nicht still.** In dieser Datei
 *     gibt es keine Methode, die eine Zusage in eine breitere Zusage überführt,
 *     und keine, die einer Rolle ein Werkzeug hinzufügt. [AgentActor.MainAgent]
 *     ist kein Ungeheuer dieser Matrix: Er darf eine Frage **stellen**, aber sein
 *     eigener Zustand geht in keine Rechnung ein — [HelperRightsView.right]
 *     liest ausschließlich Art, Auftrag, Projektgrenze und das Zustimmungsbuch
 *     des **Helfers**. Seine eigenen Rechte stehen im Freigabezentrum
 *     ([org.claudroide.app.core.security.PermissionCenter]) und werden hier
 *     weder vergeben noch erweitert; [MAIN_AGENT_NOTE] sagt das im Klartext.
 *  3. **Helfer können keine Zustimmung und keine App-Sicherheitsregel umgehen.**
 *     Die wirksamen Rechte werden **bei jedem Aufruf neu berechnet**
 *     ([HelperRightsView.right]) und niemals als `val granted = …` gespeichert —
 *     die Regel „never cache a permission state" aus
 *     `android-permissions-security` auf die Rechte der eigenen App angewandt.
 *     Eine zurückgenommene Zustimmung ([HelperConsent.Withdrawn]) wirkt deshalb
 *     sofort: derselbe Aufrufer, derselbe Auftrag, nur ein späterer Zeitpunkt.
 *     Eine Zustimmung kann außerdem nur **innerhalb** der Rollenobergrenze etwas
 *     bewilligen: [HelperConsent.Approved] prüft das im Konstruktor, sodass eine
 *     unzulässige Bewilligung gar nicht erst baubar ist — auch nicht über `copy`.
 *     Der Projektordner wird über [PathBoundaryGuard] geprüft, nicht über einen
 *     Vergleich von Zeichenketten.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz. Diese Klasse startet
 * keinen Helfer, öffnet keine Datei und spricht niemanden an. Sie beantwortet
 * nur die Frage „darf dieser Helfer das jetzt?".
 */

// ── Rechtekategorien ─────────────────────────────────────────────────────

/**
 * Die sechs Rechtearten, über die die Matrix urteilt.
 *
 * Sie decken das Ergebnis der Aufgabe ab: Lesen, Schreiben, Befehle, Netzwerk,
 * Anbieter und gemeinsame Daten. Jede ist eine **eigene** Zeile — nicht eine
 * Sammelzeile „Restrechte", denn eine Sammelzeile ist der Ort, an dem sich
 * Rechte unbemerkt verstecken.
 */
enum class HelperRightScope(val label: String, val germanLabel: String) {

    /** Dateien des eigenen Projekts ansehen. */
    READ("Read project files", "Projektdateien lesen"),

    /** Dateien des eigenen Projekts ändern. */
    WRITE("Change project files", "Projektdateien ändern"),

    /** Etwas ausführen: Tests, Befehle, Installationen. */
    COMMANDS("Run tests, commands, installs", "Tests, Befehle und Installationen ausführen"),

    /** Daten nach außen geben — Anbieteraufruf oder Upload. */
    NETWORK("Send data out of the app", "Daten aus der App heraus senden"),

    /** Einen Zugangsschlüssel eines Anbieters benutzen. */
    PROVIDERS("Use a provider access key", "Zugangsschlüssel eines Anbieters verwenden"),

    /** Daten benutzen, die ein anderer Helfer oder ein anderes Projekt erzeugt hat. */
    SHARED_DATA("Use data produced elsewhere", "fremd erzeugte Daten benutzen");

    /** Die Freigabezentrum-Kategorie, in der dieses Recht fällt — für die Übersicht. */
    val category: PermissionCategory
        get() = when (this) {
            READ, WRITE -> PermissionCategory.FILES
            COMMANDS -> PermissionCategory.COMMANDS
            NETWORK, PROVIDERS -> PermissionCategory.PROVIDERS
            SHARED_DATA -> PermissionCategory.EXTERNAL_TOOLS
        }

    /** Wandelt dieses Recht verlangend um — bei einem belastbaren Beleg. */
    fun asPermissionTarget(): String = "helfer:$name"
}

/**
 * Was die Matrix für eine Zelle sagt.
 *
 * Vier Ausgänge, weil „nein" allein nicht reicht: Der Nutzer soll beim
 * Absagen unterscheiden können zwischen „das gibt es hier grundsätzlich nicht"
 * und „das gibt es, aber nur nach deiner Zustimmung".
 */
enum class MatrixAccess(val label: String, val germanLabel: String) {

    /** Die Rolle deckt das Recht ab; es braucht keine zusätzliche Zustimmung. */
    ALLOWED("Allowed by the role", "von der Rolle gedeckt"),

    /** Die Rolle deckt es ab, aber der Nutzer muss diesem Helfer ausdrücklich zustimmen. */
    ONLY_WITH_USER_CONSENT("Only with the user's consent", "nur mit Zustimmung des Nutzers"),

    /** Die App-Sicherheitsregel verbietet es für diese Helferart grundsätzlich. */
    DENIED_BY_DESIGN("Denied by design", "grundsätzlich nicht erlaubt"),

    /**
     * Für diese Helferart gibt es dieses Recht nicht.
     *
     * Der Unterschied zu [DENIED_BY_DESIGN] ist der Ort im Satz: Ein
     * Recherchehelfer „nimmt keine fremden Daten an" — das ist keine verweigerte
     * Anfrage, sondern eine Eigenschaft seiner Art.
     */
    NOT_APPLICABLE("Does not apply to this kind of helper", "für diese Helferart nicht vorgesehen")
}

/**
 * Eine Zelle der Matrix: **eine** Rechtekategorie bei **einer** Helferart.
 *
 * [coveredTools] nennt die Werkzeuge aus [HelperTool], um die es in dieser Zeile
 * geht — es ist **keine** Zusage. Was ein Helfer wirklich hält, steht erst in
 * [RightDecision.Granted], und dort ist es der Schnitt aus Rolle, Auftrag und
 * Zustimmung.
 */
data class MatrixCell(
    val scope: HelperRightScope,
    val access: MatrixAccess,
    val coveredTools: List<HelperTool>,
    val note: String
) {

    /** Braucht diese Zelle eine ausdrückliche Zustimmung des Nutzers? */
    val needsConsent: Boolean get() = access == MatrixAccess.ONLY_WITH_USER_CONSENT

    /** Ist das Recht in dieser Zelle überhaupt erreichbar? */
    val isReachable: Boolean
        get() = access == MatrixAccess.ALLOWED || access == MatrixAccess.ONLY_WITH_USER_CONSENT
}

// ── Wer handelt ──────────────────────────────────────────────────────────

/**
 * Wer die Frage stellt: der Hauptagent oder einer seiner Helfer.
 *
 * [Subagent] ist ein **Helfer mit bekannter Art**. Es gibt hier keinen Weg, einen
 * Helfer ohne Art zu bauen — die Art ist das, woraus die Rechte folgen, und sie
 * muss deshalb am Anfang stehen und nicht als später gesetztes Feld.
 */
sealed interface AgentActor {

    /** Die Kennung, unter der der Aktent geführt wird. */
    val actorId: String

    /** Die Bezeichnung für die Oberfläche. */
    val displayLabel: String

    /** Der Hauptagent. Seine eigenen Rechte stehen im Freigabezentrum. */
    data class MainAgent(override val actorId: String) : AgentActor {

        init {
            require(actorId.isNotBlank()) { "Der Hauptagent braucht eine Kennung." }
        }

        override val displayLabel: String get() = "Hauptagent $actorId"
    }

    /** Ein Spezialhelfer. */
    data class Subagent(override val actorId: String, val role: HelperRole) : AgentActor {

        init {
            require(actorId.isNotBlank()) { "Ein Helfer braucht eine Kennung." }
        }

        override val displayLabel: String get() = "${role.germanLabel} $actorId"
    }
}

// ── Zustimmung des Nutzers ───────────────────────────────────────────────

/**
 * Die Entscheidung des **Nutzers** über **einen** Helfer.
 *
 * `sealed interface` mit genau den drei Lagen, die es in Aufgabe 129 für die
 * Skill-Installation gab: erteilt, abgelehnt, zurückgenommen. „Erteilt" ist
 * ausdrücklich benannt und nicht der Standardwert — sonst erschiene eine
 * fehlende Entscheidung als Erlaubnis.
 *
 * @property helperId an wen genau die Entscheidung gerichtet ist.
 * @property projectId in welchem Projekt.
 * @property decidedAt wann entschieden wurde; ein Zeitstempel 0 ist kein
 *           Zeitpunkt und wird abgelehnt.
 * @property decidedBy wer entschieden hat. Leer bleibt nicht: eine Freigabe ohne
 *           genannten Entscheider ist nicht nachvollziehbar.
 */
sealed interface HelperConsent {

    /** An wen diese Entscheidung gerichtet ist. */
    val helperId: String

    /** In welchem Projekt diese Entscheidung gilt. */
    val projectId: String

    /** Wann entschieden wurde. */
    val decidedAt: Long

    /** Wer entschieden hat. */
    val decidedBy: String

    /** Wurde die Zustimmung erteilt? Nur [Approved] beantwortet das mit ja. */
    val isApproval: Boolean get() = this is Approved

    /**
     * Erteilt — aber **nur** innerhalb der Rollenobergrenze.
     *
     * Der Konstruktor ist `internal`, und der öffentliche Weg ist
     * [SubagentPermissionMatrix.approve]. Die eigentliche Absicherung sitzt
     * aber **nicht** in der Sichtbarkeit, sondern in der Prüfung im `init`:
     * [approvedScopes] kann keinen Bereich nennen, den die Rolle nicht deckt.
     * Damit lässt sich für einen Prüfer nicht einmal eine ungültige
     * Schreiberlaubnis herstellen — auch nicht durch einen Aufruf im selben
     * Modul und auch nicht über `copy`.
     *
     * [approvedScopes] kann gegenüber der Rolle also **nur verkleinern**: Der
     * Nutzer darf einem Recherchehelfer das Starten von Tests verbieten, indem
     * er [HelperRightScope.COMMANDS] nicht nennt. Vergrößern kann er nichts.
     */
    data class Approved internal constructor(
        override val helperId: String,
        override val projectId: String,
        val role: HelperRole,
        val approvedScopes: Set<HelperRightScope>,
        override val decidedAt: Long,
        override val decidedBy: String
    ) : HelperConsent {

        init {
            require(helperId.isNotBlank()) { "Eine Zustimmung braucht den Helfer, für den sie gilt." }
            require(projectId.isNotBlank()) { "Eine Zustimmung braucht das Projekt, in dem sie gilt." }
            require(decidedBy.isNotBlank()) { "Eine Zustimmung braucht einen genannten Entscheider." }
            require(decidedAt > 0L) { "Eine Zustimmung braucht einen Zeitpunkt; 0 ist keiner." }
            require(approvedScopes.isNotEmpty()) { "Eine Zustimmung ohne Bereich bewilligt nichts." }
            require(
                approvedScopes.all { SubagentPermissionMatrix.mayBeApproved(role, it) }
            ) {
                "Eine Zustimmung kann keinen Bereich nennen, den die Rolle nicht deckt. " +
                    "Sonst wäre sie eine stille Rechteerweiterung."
            }
        }

        override val isApproval: Boolean get() = true

        /** Gilt diese Erteilung genau für diesen Helfer in genau diesem Projekt? */
        fun covers(candidateHelperId: String, candidateProjectId: String): Boolean =
            helperId == candidateHelperId && projectId == candidateProjectId

        /**
         * War die Erteilung zum Zeitpunkt [nowMs] bereits getroffen?
         *
         * Eine Erteilung aus der Zukunft gilt noch nicht. Ohne diese Prüfung
         * genügte ein einziger Zeitstempel, um ein Recht dauerhaft zu behaupten.
         */
        fun wasDecidedBy(nowMs: Long): Boolean = decidedAt <= nowMs

        /** Die Zeile für die Oberfläche. */
        fun line(): String =
            "$role.germanLabel $helperId in $projectId: ${approvedScopes.joinToString(", ") { it.germanLabel }}" +
                " (zugestimmt von $decidedBy)"
    }

    /** Der Nutzer hat abgelehnt. */
    data class Refused internal constructor(
        override val helperId: String,
        override val projectId: String,
        val reason: String,
        override val decidedAt: Long,
        override val decidedBy: String
    ) : HelperConsent {

        init {
            require(helperId.isNotBlank()) { "Eine Ablehnung braucht den Helfer, für den sie gilt." }
            require(projectId.isNotBlank()) { "Eine Ablehnung braucht das Projekt, in dem sie gilt." }
            require(reason.isNotBlank()) { "Eine Ablehnung ohne Grund wäre eine stille Erlaubnis." }
            require(decidedBy.isNotBlank()) { "Eine Ablehnung braucht einen genannten Entscheider." }
            require(decidedAt > 0L) { "Eine Ablehnung braucht einen Zeitpunkt; 0 ist keiner." }
        }
    }

    /**
     * Der Nutzer hat die Zustimmung zurückgenommen.
     *
     * Ein eigener Fall und nicht nur ein Flag: In der Oberfläche sieht „der
     * Nutzer hat es zurückgenommen" anders aus als „es wurde nie erteilt", und
     * die App darf daraus nichts zurückrechnen, was vorher galt.
     */
    data class Withdrawn internal constructor(
        override val helperId: String,
        override val projectId: String,
        val reason: String,
        override val decidedAt: Long,
        override val decidedBy: String
    ) : HelperConsent {

        init {
            require(helperId.isNotBlank()) { "Eine Entziehung braucht den Helfer, für den sie gilt." }
            require(projectId.isNotBlank()) { "Eine Entziehung braucht das Projekt, in dem sie gilt." }
            require(reason.isNotBlank()) { "Eine Entziehung ohne Grund wäre eine stille Rücknahme." }
            require(decidedBy.isNotBlank()) { "Eine Entziehung braucht einen genannten Entscheider." }
            require(decidedAt > 0L) { "Eine Entziehung braucht einen Zeitpunkt; 0 ist keiner." }
        }
    }
}

/**
 * Das Buch der Nutzerentscheidungen, **veränderbar** — sonst wäre nichts zu
 * widerrufen.
 *
 * Der einzige Leseweg ist [currentFor], und er liest jedes Mal aufs Neue. Es
 * gibt hier keine zwischengespeicherte „gültige Zustimmung" und keinen
 * Schnappschuss pro Sitzung: Wer widerruft, sperrt im nächsten Aufruf.
 */
class HelperConsentLedger {

    private val entscheidungen = linkedMapOf<String, HelperConsent>()

    /** Wie viele Entscheidungen das Buch gerade enthält. */
    val count: Int get() = entscheidungen.size

    /**
     * Trägt eine Entscheidung ein. Die neueste gewinnt — das Buch ist ein
     * Entscheidungsverlauf mit einem aktuellen Stand, kein Stapel.
     */
    @Synchronized
    fun record(consent: HelperConsent) {
        entscheidungen[consent.helperId] = consent
    }

    /**
     * Nimmt eine erteilte Zustimmung zurück.
     *
     * Der Rückweg ist derselbe wie der Hinweg: eine Entscheidung des Nutzers,
     * mit Zeitpunkt und Grund. Es gibt keine Methode, die eine Erteilung still
     * löscht — sie würde in der Oberfläche als „nie erteilt" erscheinen.
     */
    fun withdraw(
        helperId: String,
        reason: String,
        decidedAt: Long,
        decidedBy: String
    ): HelperConsent.Withdrawn {
        val alt = currentFor(helperId)
        val entzogen = HelperConsent.Withdrawn(
            helperId = helperId,
            projectId = alt?.projectId.orEmpty().ifBlank { KEIN_PROJEKT },
            reason = reason,
            decidedAt = decidedAt,
            decidedBy = decidedBy
        )
        record(entzogen)
        return entzogen
    }

    /**
     * Die **aktuelle** Entscheidung zu diesem Helfer, oder `null`.
     *
     * Frisch gelesen, ohne Zwischenspeicher. Der Rückgabewert ist eine
     * Entscheidung, **kein** boolscher Wert — wer sie auswertet, muss den Fall
     * benennen, in dem sie gilt.
     */
    @Synchronized
    fun currentFor(helperId: String): HelperConsent? = entscheidungen[helperId]

    companion object {
        /** Platzhalter, wenn zu einem unbekannten Helfer entzogen wird. */
        const val KEIN_PROJEKT: String = "(kein Projekt)"
    }
}

// ── Ergebnis einer Anfrage ───────────────────────────────────────────────

/** Warum ein Recht nicht erteilt wurde. */
enum class RightRefusal(val label: String, val germanLabel: String) {

    /** Für diese Helferart gibt es das Recht nicht. */
    NOT_APPLICABLE_FOR_THIS_KIND(
        "Does not exist for this kind of helper",
        "für diese Helferart nicht vorgesehen"
    ),

    /** Die Rollenobergrenze deckt dieses Recht nicht ab — auch nicht auf Verlangen. */
    OUTSIDE_ROLE_CEILING(
        "Outside the role's ceiling",
        "außerhalb der Rollen-Obergrenze"
    ),

    /** Der Auftrag des Helfers nennt dieses Recht nicht. */
    NOT_IN_ASSIGNED_SCOPE(
        "Not part of the assignment",
        "nicht Teil des Auftrags"
    ),

    /** Es liegt keine wirksame Zustimmung des Nutzers für genau diesen Helfer vor. */
    NO_LIVE_USER_CONSENT(
        "No current user consent",
        "keine wirksame Zustimmung des Nutzers"
    ),

    /** Der Helfer gehört zu einem anderen Projekt als der laufende. */
    OUTSIDE_PROJECT_BOUNDARY(
        "Helper belongs to another project",
        "Helfer gehört zu einem anderen Projekt"
    ),

    /** Der Zielpfad liegt außerhalb des Projektordners. */
    PATH_OUTSIDE_PROJECT(
        "Path lies outside the project folder",
        "Pfad liegt außerhalb des Projektordners"
    ),

    /** Ein anderer Akteur versucht, die Rechte dieses Helfers anzufragen. */
    CALLER_IS_NOT_THIS_HELPER(
        "Only the helper itself may ask",
        "nur der Helfer selbst darf fragen"
    )
}

/** Worauf eine Erteilung beruht — für die Anzeige und die Nachprüfung. */
enum class GrantBasis(val label: String, val germanLabel: String) {

    /** Die Rolle deckt das Recht ab; eine Zustimmung war nicht nötig. */
    ROLE_CEILING("Covered by the role", "von der Rolle gedeckt"),

    /** Erteilt, weil der Nutzer diesem Helfer ausdrücklich zugestimmt hat. */
    USER_CONSENT("Granted by the user's consent", "durch Zustimmung des Nutzers erteilt")
}

/**
 * Das Ergebnis einer Rechteenfrage — bei einem **frischen** Blick auf den
 * aktuellen Zustand.
 *
 * `sealed interface`, damit „nein" kein Sonderfall in einem Attrappen-Objekt
 * ist: [Refused] hat kein Werkzeugfeld, [Granted] hat kein Rückgabefeld für
 * mehr, als beantragt wurde.
 */
sealed interface RightDecision {

    /** Die Rechtekategorie, um die es geht. */
    val scope: HelperRightScope

    /** Wurde das Recht erteilt? */
    val isGranted: Boolean

    /** Erteilt — mit genau den Werkzeugen, die der Helfer jetzt wirklich hält. */
    data class Granted(
        override val scope: HelperRightScope,
        val helperId: String,
        val tools: List<HelperTool>,
        val basis: GrantBasis
    ) : RightDecision {
        override val isGranted: Boolean get() = true

        /** Die Werkzeuge, die über das reine Lesen hinausgehen. */
        val writesOrSends: Boolean get() = tools.any { it.widensScope }
    }

    /** Abgelehnt — und der Grund steht in der Entscheidung, nicht im Aufrufer. */
    data class Refused(
        override val scope: HelperRightScope,
        val helperId: String,
        val refusal: RightRefusal,
        val explanation: String
    ) : RightDecision {
        override val isGranted: Boolean get() = false

        /** Die Zeile für die Oberfläche. */
        fun line(): String = "${scope.germanLabel}: $explanation"
    }
}

// ── Die Rechteansicht eines Helfers ──────────────────────────────────────

/**
 * Die Rechte **eines** Helfers, gerechnet bei jedem Aufruf.
 *
 * Diese Klasse ist ein **Fragekorb**, kein Rechtekorb: Sie hält nur die
 * unveränderlichen Angaben zum Helfer und das Zustimmungsbuch, das sich ändern
 * darf. Sie hält bewusst **kein** Feld, das ein Ergebnis vom Typ `Boolean` oder
 * eine Werkzeugliste cached — eine gespeicherte Erlaubnis überlebt genau das
 * Ereignis, das sie beenden soll.
 *
 * `inner`-freier, `internal` Konstruktor: Außerhalb dieser Datei entsteht eine
 * Ansicht nur über [SubagentPermissionMatrix.viewFor], und dort ist
 * [HelperRole] Pflicht. Man kann sich also keinen Prüfer ohne Art bauen.
 *
 * @property helperId der Helfer, um den es geht.
 * @property projectId das Projekt, in dem dieser Helfer arbeitet.
 * @property role die Art des Helfers — daraus folgt die Zeile der Matrix.
 * @property toolsInAssignment was der Auftrag an Werkzeugen nennt. Mehr als das
 *           gibt es nicht: was nicht im Auftrag steht, wird auch mit Zustimmung
 *           nicht geholt.
 * @property projectRoot der Projektordner, gegen den [PathBoundaryGuard]
 *           prüft. `null` heißt: für diese Anfrage wird kein Pfad geprüft, und
 *           dann darf die Anfrage auch keinen Pfad nennen.
 */
class HelperRightsView internal constructor(
    val helperId: String,
    val projectId: String,
    val role: HelperRole,
    val toolsInAssignment: List<HelperTool>,
    private val consents: HelperConsentLedger,
    val projectRoot: String? = null
) {

    init {
        require(helperId.isNotBlank()) { "Eine Rechteansicht braucht den Helfer, für den sie gilt." }
        require(projectId.isNotBlank()) { "Eine Rechteansicht braucht das Projekt, in dem sie gilt." }
    }

    /**
     * Darf dieser Helfer das Recht **jetzt** ausüben?
     *
     * Alles, was die Antwort trägt, wird **in diesem Aufruf** gelesen:
     * Rollenobergrenze, Rollenzelle, Auftrag, Projektgrenze und — falls die
     * Zelle eine Zustimmung verlangt — das **jetzt gültige** Buch. Es gibt hier
     * kein `val granted = …`, das zwischen zwei Aufrufen veralten könnte.
     *
     * Die Reihenfolge der Prüfungen ist die Aussage:
     *
     *  1. Wer fragt. Nur dieser Helfer oder der Hauptagent darf fragen; ein
     *    anderer Helfer darf die Rechte dieses Helfers nicht abfragen.
     *  2. Die Rollenzelle. Eine Zelle, die das Recht nicht kennt
     *     ([MatrixAccess.NOT_APPLICABLE]) oder es verbietet
     *     ([MatrixAccess.DENIED_BY_DESIGN]), beendet die Prüfung — **bevor**
     *     irgendetwas anderes gelesen wurde. Deshalb hilft hier auch keine
     *     Zustimmung.
     *  3. Die Rollenobergrenze aus Aufgabe 132: Ein Bereich, zu dem die Art kein
     *     Werkzeug hält, bleibt unerreichbar.
     *  4. Der Auftrag. Was er nicht nennt, wird nicht beantragt.
     *  5. Die Projektgrenze — für Dateirechte über [PathBoundaryGuard], nicht
     *     über einen Zeichenkettenvergleich.
     *  6. Die Zustimmung, frisch gelesen, mit Zeitprüfung.
     *
     * @param scope die Rechtekategorie.
     * @param caller wer fragt. [AgentActor.MainAgent] ändert an der Antwort
     *        **nichts**: Sein eigener Zustand geht nicht in die Rechnung ein.
     * @param nowMs der Zeitpunkt der Anfrage. Ein Bereich wird nicht vor seiner
     *        Zustimmung gültig.
     * @param sessionProjectId das Projekt, in dem gerade gelaufen wird.
     * @param targetPath bei Dateirechten der Pfad, auf den sich die Anfrage
     *        bezieht. Wird er genannt, ohne dass [projectRoot] gesetzt ist, wird
     *        die Anfrage abgelehnt statt ungeprüft bewilligt.
     */
    fun right(
        scope: HelperRightScope,
        caller: AgentActor,
        nowMs: Long,
        sessionProjectId: String,
        targetPath: String? = null
    ): RightDecision {
        require(nowMs > 0L) { "Eine Rechteenfrage braucht einen Zeitpunkt; 0 ist keiner." }
        require(sessionProjectId.isNotBlank()) { "Eine Rechteenfrage braucht das laufende Projekt." }

        // 1. Wer fragt.
        if (caller is AgentActor.Subagent && caller.actorId != helperId) {
            return verweigert(
                scope,
                RightRefusal.CALLER_IS_NOT_THIS_HELPER,
                "Ein Helfer darf nur die eigenen Rechte erfragen. ${caller.actorId} hat hier nichts zu bestimmen."
            )
        }

        // 2. Die Rollenzelle — vor allem anderen.
        val zelle = SubagentPermissionMatrix.cell(role, scope)
        when (zelle.access) {
            MatrixAccess.NOT_APPLICABLE -> return verweigert(
                scope,
                RightRefusal.NOT_APPLICABLE_FOR_THIS_KIND,
                zelle.note
            )

            MatrixAccess.DENIED_BY_DESIGN -> return verweigert(
                scope,
                RightRefusal.OUTSIDE_ROLE_CEILING,
                zelle.note
            )

            MatrixAccess.ALLOWED, MatrixAccess.ONLY_WITH_USER_CONSENT -> Unit
        }

        // 3. Die Rollenobergrenze aus Aufgabe 132.
        val vonDerRolle = zelle.coveredTools.filter { it in role.allowedTools }
        if (vonDerRolle.isEmpty()) {
            return verweigert(
                scope,
                RightRefusal.OUTSIDE_ROLE_CEILING,
                "${role.germanLabel} $helperId hält für „${scope.germanLabel}“ kein einziges passendes Werkzeug."
            )
        }

        // 4. Der Auftrag.
        val beantragt = vonDerRolle.filter { it in toolsInAssignment }
        if (beantragt.isEmpty()) {
            return verweigert(
                scope,
                RightRefusal.NOT_IN_ASSIGNED_SCOPE,
                "Der Auftrag von $helperId nennt kein Werkzeug für „${scope.germanLabel}“."
            )
        }

        // 5. Das Projekt — und für Dateirechte der echte Pfad.
        if (projectId != sessionProjectId) {
            return verweigert(
                scope,
                RightRefusal.OUTSIDE_PROJECT_BOUNDARY,
                "$helperId gehört zum Projekt $projectId, gerade läuft $sessionProjectId. " +
                    "Dort werden die Rechte eines anderen Projekts nicht benutzt."
            )
        }

        if (scope == HelperRightScope.READ || scope == HelperRightScope.WRITE) {
            val pfadUrteil = pruefePfad(targetPath)
            if (pfadUrteil != null) return verweigert(scope, pfadUrteil.first, pfadUrteil.second)
        }

        // 6. Die Zustimmung — jetzt gelesen, nicht von vorhin.
        if (zelle.needsConsent) {
            val zustimmung = consents.currentFor(helperId)
            val wirksam = zustimmung is HelperConsent.Approved &&
                zustimmung.covers(helperId, projectId) &&
                zustimmung.wasDecidedBy(nowMs) &&
                scope in zustimmung.approvedScopes
            if (!wirksam) {
                return verweigert(
                    scope,
                    RightRefusal.NO_LIVE_USER_CONSENT,
                    "Für ${scope.germanLabel} liegt keine wirksame Zustimmung des Nutzers für $helperId vor."
                )
            }
        }

        return RightDecision.Granted(
            scope = scope,
            helperId = helperId,
            tools = beantragt,
            basis = if (zelle.needsConsent) GrantBasis.USER_CONSENT else GrantBasis.ROLE_CEILING
        )
    }

    /**
     * Prüft [targetPath] gegen den Projektordner.
     *
     * Gibt `null` zurück, wenn nichts zu beanstanden ist. Wird ein Pfad genannt,
     * ohne dass ein Projektordner bekannt ist, ist das **kein** „dann eben
     * ungeprüft": Die Anfrage wird abgelehnt, weil ein Pfad ohne Grenze nicht
     * bewilligt werden kann.
     */
    private fun pruefePfad(targetPath: String?): Pair<RightRefusal, String>? {
        if (targetPath == null) return null
        val wurzel = projectRoot ?: return RightRefusal.PATH_OUTSIDE_PROJECT to
            "Für $helperId ist kein Projektordner festgelegt. Ein Pfad kann deshalb nicht geprüft werden."
        if (targetPath.isBlank()) return null
        val urteil = PathBoundaryGuard.check(targetPath, wurzel)
        return if (urteil.isAllowed) null else RightRefusal.PATH_OUTSIDE_PROJECT to urteil.message
    }

    private fun verweigert(scope: HelperRightScope, grund: RightRefusal, text: String) =
        RightDecision.Refused(scope, helperId, grund, text)

    /**
     * Alle sechs Rechtekategorien in einer Zeile je — **jedes Mal neu berechnet**.
     *
     * Bequemlichkeit für die Oberfläche, keine Abkürzung: Jede Zeile entsteht
     * aus einem eigenen [right]-Aufruf und liest die Zustimmung also erneut.
     */
    fun rightsNow(
        caller: AgentActor,
        nowMs: Long,
        sessionProjectId: String
    ): List<RightDecision> = HelperRightScope.entries.map { scope ->
        right(scope = scope, caller = caller, nowMs = nowMs, sessionProjectId = sessionProjectId)
    }
}

// ── Die Matrix ───────────────────────────────────────────────────────────

/**
 * Die Rechtematrix für Spezialhelfer.
 *
 * Reines Kotlin: keine Datei wird geöffnet, kein Anbieter angeprochen, kein
 * Helfer gestartet. Die Matrix beantwortet nur „darf dieser Helfer das jetzt?"
 * — und sie holt diese Antwort bei jedem Aufruf neu.
 */
object SubagentPermissionMatrix {

    /**
     * Die Rechte des **Hauptagenten** stehen hier nicht.
     *
     * Sie stehen im Freigabezentrum
     * ([org.claudroide.app.core.security.PermissionCenter]) und werden von dort
     * erteilt, erweitert und widerrufen. Diese Matrix kann sie weder vergeben
     * noch erweitern — sie kennt keinen Aufruf, der aus einer bestehenden
     * Zusage eine breitere macht.
     */
    const val MAIN_AGENT_NOTE: String =
        "Die Rechte des Hauptagenten stehen im Freigabezentrum. Diese Matrix " +
            "kann sie weder vergeben noch still erweitern; sie entscheidet nur " +
            "darüber, was ein Helfer darf."

    /**
     * Die Werkzeuge aus [HelperTool], um die es je Rechtekategorie geht.
     *
     * Das ist der **Gegenstand** einer Zeile, keine Zusage. Was ein Helfer hält,
     * ist der Schnitt aus Rolle, Auftrag und Zustimmung — siehe
     * [HelperRightsView.right].
     *
     * [HelperRightScope.SHARED_DATA] nennt [HelperTool.READ_FILE] ganz bewusst:
     * Das Werkzeug ist dasselbe wie beim Lesen, die **Frage** ist eine andere —
     * um wessen Daten es geht. Genau deshalb ist die Zeile für Recherche- und
     * Prüfhelfer „nicht vorgesehen“ und für den Teilaufgabenhelfer zustimmungs-
     * pflichtig, obwohl in allen drei Fällen gelesen werden dürfte.
     */
    private val SCOPE_TOOLS: Map<HelperRightScope, List<HelperTool>> = mapOf(
        HelperRightScope.READ to listOf(HelperTool.READ_FILE, HelperTool.LIST_DIRECTORY, HelperTool.SEARCH_TEXT),
        HelperRightScope.WRITE to listOf(HelperTool.WRITE_FILE, HelperTool.DELETE_FILE),
        HelperRightScope.COMMANDS to listOf(HelperTool.RUN_TEST, HelperTool.RUN_COMMAND, HelperTool.INSTALL_DEPENDENCY),
        HelperRightScope.NETWORK to listOf(HelperTool.NETWORK_CALL, HelperTool.GIT_PUSH),
        HelperRightScope.PROVIDERS to listOf(HelperTool.USE_API_KEY),
        HelperRightScope.SHARED_DATA to listOf(HelperTool.READ_FILE)
    )

    /**
     * Die Matrix selbst: **jede** Helferart gegen **jede** Rechtekategorie.
     *
     * Vollständig ausgeschrieben, mit Absicht. Ein `else ->` oder eine
     * „Standardzeile“ wäre hier die Stelle, an der ein Recht unbemerkt entstünde;
     * deshalb existiert keine. [vollstaendig] prüft das bei jedem Aufruf.
     */
    private val MATRIX: Map<HelperRole, Map<HelperRightScope, MatrixAccess>> = mapOf(
        HelperRole.RESEARCH to mapOf(
            HelperRightScope.READ to MatrixAccess.ALLOWED,
            HelperRightScope.WRITE to MatrixAccess.DENIED_BY_DESIGN,
            HelperRightScope.COMMANDS to MatrixAccess.ONLY_WITH_USER_CONSENT,
            HelperRightScope.NETWORK to MatrixAccess.DENIED_BY_DESIGN,
            HelperRightScope.PROVIDERS to MatrixAccess.DENIED_BY_DESIGN,
            HelperRightScope.SHARED_DATA to MatrixAccess.NOT_APPLICABLE
        ),
        HelperRole.REVIEW to mapOf(
            HelperRightScope.READ to MatrixAccess.ALLOWED,
            // Die tragende Zelle der Aufgabe 1: Ein Prüfer schreibt nicht.
            // Sie steht vor jeder anderen Prüfung und ist nicht über eine
            // Zustimmung erreichbar.
            HelperRightScope.WRITE to MatrixAccess.DENIED_BY_DESIGN,
            HelperRightScope.COMMANDS to MatrixAccess.DENIED_BY_DESIGN,
            HelperRightScope.NETWORK to MatrixAccess.DENIED_BY_DESIGN,
            HelperRightScope.PROVIDERS to MatrixAccess.DENIED_BY_DESIGN,
            HelperRightScope.SHARED_DATA to MatrixAccess.NOT_APPLICABLE
        ),
        HelperRole.SUBTASK to mapOf(
            HelperRightScope.READ to MatrixAccess.ALLOWED,
            HelperRightScope.WRITE to MatrixAccess.ONLY_WITH_USER_CONSENT,
            HelperRightScope.COMMANDS to MatrixAccess.ONLY_WITH_USER_CONSENT,
            HelperRightScope.NETWORK to MatrixAccess.DENIED_BY_DESIGN,
            HelperRightScope.PROVIDERS to MatrixAccess.DENIED_BY_DESIGN,
            HelperRightScope.SHARED_DATA to MatrixAccess.ONLY_WITH_USER_CONSENT
        )
    )

    /**
     * Eine Zelle der Matrix.
     *
     * Der Notiztext nennt die Begründung, damit der Nutzer beim Absagen einen
     * Grund sieht und nicht nur ein „nein".
     */
    fun cell(role: HelperRole, scope: HelperRightScope): MatrixCell {
        val zugang = MATRIX[role]?.get(scope)
            ?: throw IllegalArgumentException(
                "Für ${role.germanLabel} ist die Rechtekategorie „${scope.germanLabel}“ nicht festgelegt. " +
                    "Die Matrix muss vollständig sein."
            )
        return MatrixCell(
            scope = scope,
            access = zugang,
            coveredTools = SCOPE_TOOLS.getValue(scope),
            note = notiz(role, scope, zugang)
        )
    }

    /** Alle sechs Zeilen einer Helferart, in fester Reihenfolge. */
    fun matrixOf(role: HelperRole): List<MatrixCell> =
        HelperRightScope.entries.map { cell(role, it) }

    /**
     * Darf eine Nutzerzustimmung diesen Bereich überhaupt je nennen?
     *
     * Nein für [MatrixAccess.DENIED_BY_DESIGN] und [MatrixAccess.NOT_APPLICABLE].
     * Das ist der zweite Ort, an dem Zusage 2 strukturell steht: Nicht nur die
     * Anwendung der Zustimmung ist begrenzt — eine Zustimmung, die mehr nennt,
     * ist gar nicht erst baubar.
     */
    fun mayBeApproved(role: HelperRole, scope: HelperRightScope): Boolean =
        cell(role, scope).isReachable

    /**
     * Erzeugt eine Nutzerzustimmung — der **einzige** öffentliche Weg zu
     * [HelperConsent.Approved].
     *
     * Es gibt hier keine Variante, die mehr bewilligt als [approvedScopes], und
     * keine, die eine Rolle wechselt. Wer einem Prüfer das Schreiben geben
     * will, bekommt eine Ablehnung beim Bauen der Zustimmung.
     *
     * @param decidedAt der Zeitpunkt der Entscheidung; 0 wird abgelehnt.
     * @param decidedBy wer entschieden hat, im Klartext.
     */
    fun approve(
        helperId: String,
        projectId: String,
        role: HelperRole,
        approvedScopes: Set<HelperRightScope>,
        decidedAt: Long,
        decidedBy: String
    ): HelperConsent.Approved = HelperConsent.Approved(
        helperId = helperId,
        projectId = projectId,
        role = role,
        approvedScopes = approvedScopes,
        decidedAt = decidedAt,
        decidedBy = decidedBy
    )

    /**
     * Die Rechteansicht eines Helfers.
     *
     * Der einzige Weg zu einer [HelperRightsView]. [role] ist Pflicht: Ohne Art
     * gibt es keine Matrixzeile, und damit kein Recht.
     *
     * @param projectRoot der Projektordner, gegen den Dateipfade geprüft werden.
     */
    fun viewFor(
        helperId: String,
        projectId: String,
        role: HelperRole,
        toolsInAssignment: List<HelperTool>,
        consents: HelperConsentLedger,
        projectRoot: String? = null
    ): HelperRightsView = HelperRightsView(
        helperId = helperId,
        projectId = projectId,
        role = role,
        toolsInAssignment = toolsInAssignment.distinct(),
        consents = consents,
        projectRoot = projectRoot
    )

    /** Die Zeilen der Matrix für die Oberfläche: eine je Helferart und Bereich. */
    fun matrixLines(): List<String> = buildList {
        HelperRole.entries.forEach { rolle ->
            add("${rolle.germanLabel} (${rolle.label}):")
            HelperRightScope.entries.forEach { bereich ->
                val zelle = cell(rolle, bereich)
                add("  ${bereich.germanLabel} — ${zelle.access.germanLabel} (${zelle.note})")
            }
        }
    }

    /** Der Satz, den die Aufgabe an dieser Stelle verlangt. */
    fun rightsMeaningLine(): String =
        "Ein Helfer bekommt nur die Rechte seiner Rolle, nur die Werkzeuge seines " +
            "Auftrags und nur das, wofür der Nutzer zugestimmt hat. Diese Rechte " +
            "werden bei jedem Schritt neu geprüft."

    /**
     * Prüft die Vollständigkeit der Matrix.
     *
     * Für jede Helferart und jede Rechtekategorie muss eine Zelle existieren.
     * Diese Funktion wird bei jedem Zugriff aufgerufen: Eine Lücke in der Tabelle
     * fällt damit **beim Lesen** auf und nicht erst in einem Test.
     */
    private fun vollstaendig(): Boolean =
        HelperRole.entries.all { rolle ->
            HelperRightScope.entries.all { bereich -> MATRIX[rolle]?.containsKey(bereich) == true }
        }

    /** Der Begründungstext je Zelle — im Klartext, damit „nein“ erklärbar ist. */
    private fun notiz(rolle: HelperRole, bereich: HelperRightScope, zugang: MatrixAccess): String {
        require(vollstaendig()) { "Die Rechtematrix ist unvollständig." }
        return when (zugang) {
            MatrixAccess.ALLOWED -> "${rolle.germanLabel} darf „${bereich.germanLabel}“ ohne weitere Zustimmung."
            MatrixAccess.ONLY_WITH_USER_CONSENT ->
                "${rolle.germanLabel} darf „${bereich.germanLabel}“ nur, wenn der Nutzer diesem Helfer dafür zustimmt."
            MatrixAccess.DENIED_BY_DESIGN -> when (bereich) {
                HelperRightScope.WRITE ->
                    "Ein ${rolle.germanLabel} ändert nichts. Wer ändern soll, wird als Teilaufgabenhelfer zugewiesen."
                HelperRightScope.COMMANDS ->
                    "Ein ${rolle.germanLabel} führt nichts aus — auch nicht auf Verlangen des Hauptagenten."
                HelperRightScope.NETWORK ->
                    "Ein ${rolle.germanLabel} gibt keine Daten aus der App heraus."
                HelperRightScope.PROVIDERS ->
                    "Ein ${rolle.germanLabel} benutzt keinen Zugangsschlüssel eines Anbieters."
                else -> "Ein ${rolle.germanLabel} darf „${bereich.germanLabel}“ nicht."
            }

            MatrixAccess.NOT_APPLICABLE ->
                "Ein ${rolle.germanLabel} benutzt keine fremd erzeugten Daten. Das ist keine verweigerte Anfrage, sondern eine Eigenschaft seiner Art."
        }
    }
}