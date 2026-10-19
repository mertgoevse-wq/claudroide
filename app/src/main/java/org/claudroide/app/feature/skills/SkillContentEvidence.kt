package org.claudroide.app.feature.skills

/**
 * Task 129 — „Skills finden", Teil 1: die Inhaltsbelege.
 *
 * Die tragende Aussage der Aufgabe lautet: **keine Fähigkeit gilt allein wegen
 * ihres Namens als sicher.** Ein Treffer namens `android-testing` sagt nichts
 * darüber, was in der Datei steht. Diese Datei setzt das als Typ durch:
 *
 * * [ContentEvidence] ist ein `sealed interface` mit drei Zuständen, und es
 *   gibt **keinen** Konstruktor, der aus einer Beschreibung ein geprüftes Urteil
 *   macht. Der einzige Weg von der Behauptung zum belegten Inhalt führt über
 *   [ContentEvidence.Inspected] — und das verlangt die tatsächliche
 *   [ContentInspection], nicht ein Flag.
 *
 * * [ContentEvidence.Inspected.isVerified] ist **kein** freies Feld. Es folgt
 *   aus den zwei Bedingungen, die der Inhalt tragen muss: mindestens **ein**
 *   belegter Befund ([ContentInspection.isUsableEvidence]) und **keine**
 *   nachladende oder ausführende Wirkung.
 *
 * * Deshalb ist `VERIFIED` hier kein Enumwert, den man setzen könnte. Ein
 *   Skill, dessen `SKILL.md` nichts belegt, kann strukturell nicht als geprüft
 *   gelten — es gibt keinen Weg, das zu behaupten, ohne den Inhalt mitzubringen.
 */
// ── Befund ───────────────────────────────────────────────────────────────

/**
 * Ein einzelner belegter Befund aus dem Lesen eines Skill-Inhalts.
 *
 * Ein Befund ist nur dann ein Befund, wenn er **benannt** ist: ein leeres Etikett
 * mit einem leeren Detail trägt nichts und macht aus einem leeren Fund keine
 * Prüfung.
 */
data class ContentFinding(
    val label: String,
    val detail: String
) {

    /** Zählt dieser Befund als echte Feststellung? */
    val isEvidentiary: Boolean
        get() = label.isNotBlank() && detail.isNotBlank()
}

// ── Prüfung ──────────────────────────────────────────────────────────────

/**
 * Das Ergebnis des **Lesens** eines Skill-Inhalts.
 *
 * Anders als in der Herkunftsangabe ist hier ausdrücklich Platz für das, was
 * beim Lesen auffiel — einschließlich der Wirkungen, die einen Skill von einem
 * reinen Anweisungstext unterscheiden.
 *
 * @property executesCommands Führt der Skill Befehle aus? Dann ist er für
 *   ClauDroide kein reiner Anweisungstext.
 * @property installsOrDownloads Lädt oder installiert der Skill nach?
 * @property readsFiles Liest der Skill Dateien? Für reine Anweisungstexte untypisch.
 * @property reachesNetwork Greift der Skill auf das Netz zu?
 */
data class ContentInspection(
    val findings: List<ContentFinding> = emptyList(),
    val executesCommands: Boolean = false,
    val installsOrDownloads: Boolean = false,
    val readsFiles: Boolean = false,
    val reachesNetwork: Boolean = false
) {

    /**
     * Trägt diese Prüfung überhaupt etwas bei?
     *
     * **Nein**, wenn kein einziger Befund benannt wurde. Eine leere Liste ist
     * keine Prüfung, sondern ein leeres Formular — sie darf einen Fund nicht in
     * einen geprüften verwandeln. Leere Befunde werden hier aussortiert, nicht
     * gezählt.
     */
    val usableFindings: List<ContentFinding>
        get() = findings.filter { it.isEvidentiary }

    /** Die Bequemlichkeitsform von [usableFindings], wie die Klassen es nutzen. */
    val isUsableEvidence: Boolean
        get() = usableFindings.isNotEmpty()

    /**
     * Führt der gelesene Inhalt beim Gebrauch etwas nach oder führt er etwas aus?
     *
     * Das ist die Trennlinie zu Aufgabe 123: dort führt genau dieses Paar zur
     * Ablehnung. [readsFiles] und [reachesNetwork] stehen hier bewusst **nicht**
     * dabei — ein Recherche-Skill darf lesen und darf suchen; er darf nur nicht
     * ungefragt etwas nachladen oder etwas starten.
     */
    val hasSideEffects: Boolean
        get() = executesCommands || installsOrDownloads
}

// ── Zustand des Inhalts ──────────────────────────────────────────────────

/**
 * Wie der Inhalt eines **gefundenen** Skills belegt ist.
 *
 * `sealed interface` statt `enum class` aus genau einem Grund: Ein Enumwert
 * `VERIFIED` wäre eine **Behauptung**. Hier gibt es stattdessen nur [Inspected],
 * das die [ContentInspection] **mitführen muss** — der Inhalt ist damit nicht
 * mehr optional, sondern Teil des Zustands.
 */
sealed interface ContentEvidence {

    /** Die englische Kurzbezeichnung für Protokolle und Oberfläche. */
    val label: String

    /** Die deutsche Entsprechung. */
    val germanLabel: String

    /**
     * Die tatsächlich gelesene Prüfung — oder `null`, wenn es keine gibt.
     *
     * Nur so lässt sich ein Fehlverhalten prüfen, das die Oberfläche als
     * Tatsache meldet. Ein Zustand, der „geprüft" heißt, aber nichts mitführt,
     * wäre genau die Vortäuschung, die die Aufgabe verbietet.
     */
    val inspection: ContentInspection?
        get() = null

    /**
     * Ist der Inhalt **belegt** geprüft?
     *
     * Nur [Inspected] kann das sein, und auch dort nur mit mindestens einem
     * benannten Befund und ohne nachladende Wirkung.
     */
    val isVerified: Boolean
        get() = false

    /**
     * Nur eine **Behauptung** über den Inhalt — der Name oder die Beschreibung
     * des Skills.
     *
     * Sie ist eine Suchhilfe, kein Nachweis. Der Normalfall direkt nach einer
     * Trefferrückgabe.
     */
    data class NamedClaim(val claimedName: String) : ContentEvidence {
        override val label: String get() = "Named claim only"
        override val germanLabel: String get() = "nur benannt"
    }

    /**
     * Der Inhalt wurde **nicht gelesen**.
     *
     * Ein eigener Zustand und kein boolescher Standardwert, damit „keine
     * Angabe" nicht wie „geprüft" aussieht.
     */
    data object NotInspected : ContentEvidence {
        override val label: String get() = "Content not inspected"
        override val germanLabel: String get() = "Inhalt nicht geprüft"
    }

    /**
     * Der Inhalt **wurde gelesen** — und die Prüfung liegt vollständig bei.
     *
     * Das ist der einzige Zustand, der [isVerified] erreichen kann, und er
     * erreicht es nur, wenn die mitgeführte [ContentInspection] einen Befund
     * trägt und keine nachladende Wirkung hat.
     */
    data class Inspected(override val inspection: ContentInspection) : ContentEvidence {

        override val label: String get() = "Content inspected"
        override val germanLabel: String get() = "Inhalt geprüft"

        override val isVerified: Boolean
            get() = inspection.isUsableEvidence && !inspection.hasSideEffects
    }

    companion object {

        /**
         * Der Standardzustand eines frisch konstruierten [DiscoveredSkill]:
         * **keine** Prüfung.
         *
         * Wer ein Argument weglässt, darf nicht versehentlich als geprüft
         * durchgehen — deshalb ist der Standard ausdrücklich ungünstig.
         */
        val UNVERIFIED: ContentEvidence = NotInspected

        /**
         * Der Zustand für einen Hinweis, warum eine Frage offen bleibt.
         *
         * Kein Sonderwert, sondern derselbe Zustand wie nach einer Suche: Eine
         * Lücke, die benannt wird, ist keine andere als eine, die man verschweigt.
         */
        val OPEN: ContentEvidence = NotInspected
    }
}

/**
 * Liest einen Fundinhalt und liefert den belegten Zustand zurück.
 *
 * Bewusst **nicht** Teil von [ContentEvidence]: Das Lesen ist ein Vorgang mit
 * einem Ergebnis, und ein Vorgang gehört nicht in einen Zustandstyp.
 *
 * Der Aufrufer übergibt die gelesene [ContentInspection] — diese Funktion
 * erfindet sie nicht. Sie kann sie nur in einen Zustand überführen, der sie
 * vollständig mitführt, und sie kann einen Fund **nicht** verbessern, für den
 * keine Prüfung vorliegt.
 */
fun ContentEvidence.withInspection(inspection: ContentInspection): ContentEvidence =
    if (inspection.isUsableEvidence) ContentEvidence.Inspected(inspection) else ContentEvidence.NotInspected

/**
 * Der Fehlgrund, wenn ein Zustand keinen belegten Inhalt trägt.
 *
 * Jeder offene Zustand **nennt** seinen Grund. Der Platzhalter `open` wäre hier
 * falsch: Er unterscheidet nicht zwischen „nicht gelesen" und „gelesen, aber
 * ausführend" — und genau diese Unterscheidung ist der Unterschied zwischen
 * einer Lücke in der Recherche und einem Befund gegen den Skill.
 */
fun ContentEvidence.gapReason(): String = when {
    isVerified -> ""
    this is ContentEvidence.Inspected ->
        when {
            inspection.executesCommands -> "executes commands"
            inspection.installsOrDownloads -> "installs or downloads"
            else -> "inspection carried no finding"
        }

    this is ContentEvidence.NamedClaim -> "only a name was claimed: $claimedName"
    else -> "content not inspected"
}