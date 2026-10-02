package org.claudroide.app.core.security

/**
 * Task 123 — „Skill-Quelle prüfen".
 *
 * Ziel: Für jede Bauaufgabe einen passenden global auffindbaren Skill suchen
 * und Risiken untersuchen.
 *
 * Die Aufgabe stellt zwei Bedingungen, und **beide** sind hier die
 * tragenden Teile:
 *
 *  1. **Der Skill wird nicht ausgeführt, bevor der Inhalt geprüft ist.**
 *     [SkillCandidate] trägt deshalb kein Feld, das einen geprüften Inhalt
 *     vortäuschen könnte: `review` ist der einzige Weg zu [Verdict.USABLE].
 *     Es gibt keinen Konstruktor, eine ungeprüfte Herkunft als nutzbar zu
 *     markieren — der Prüfschritt muss durchlaufen werden, sonst entsteht kein
 *     Urteil.
 *
 *  2. **Verdächtige oder nicht lizenzierte Skills werden abgelehnt.**
 *     [Verdict.REJECTED] ist kein Sonderfall, sondern der Weg, den ein Kandidat
 *     nimmt, dessen Lizenz **unbelegt** ist. [LicenseEvidence] unterscheidet
 *     dafür ausdrücklich „nicht angegeben" und „nicht auffindbar" von „belegt":
 *     ein Verweis auf ein fehlendes Dokument ist kein Beleg. Genau dieser
 *     Unterschied entscheidet hier den Unterschied zwischen „geprüft" und
 *     „behauptet".
 *
 * Zusätzlich: **Suche ist keine automatische Installationsfreigabe**
 * ([Verdict.USABLE] sagt nichts über eine Installation aus; dafür gibt es mit
 * [installRequiresUserConsent] einen eigenen, immer positiven Weg).
 *
 * Reines Kotlin: kein Android, kein Dateizugriff, keine Netzverbindung. Die
 * App liefert die Fakten aus der Lieferkette-Prüfung, diese Klasse bewertet
 * sie — und ist damit auf der JVM prüfbar.
 */

/** Wie belastbar die Lizenzangabe eines Kandidaten ist. */
enum class LicenseEvidence(val germanLabel: String) {

    /** Eine Lizenz ist benannt **und** das Dokument ist auffindbar. */
    VERIFIED("belegt"),

    /**
     * Es wird auf ein Dokument verwiesen, das **nicht existiert**.
     *
     * Der eigene Lieferkanten-Check von ClauDroide fand genau das bei vier
     * installierten Skills: `license: Complete terms in LICENSE.txt`, die
     * Datei fehlt. Das ist kein Beleg und wird hier nicht als einer behandelt.
     */
    UNVERIFIABLE_REFERENCE("Verweis ohne Dokument"),

    /** Es wird überhaupt keine Lizenz genannt. */
    NOT_STATED("nicht angegeben")
}

/** Das Ergebnis der Lieferkettenprüfung eines Kandidaten. */
enum class Verdict(val germanLabel: String) {

    /**
     * Inhalt geprüft, Lizenz belegt, keine Risiken.
     *
     * Erreichbar **nur** über [SkillCandidate.review] — es gibt keinen Weg
     * daran vorbei.
     */
    USABLE("einsetzbar"),

    /**
     * Abgelehnt: nicht lizenziert, verdächtiger Inhalt oder nicht prüfbar.
     *
     * Ein Kandidat mit fehlender Lizenzangabe oder mit einem Verweis ohne
     * Dokument landet hier — nicht etwa stillschweigend bei `USABLE`.
     */
    REJECTED("abgelehnt")
}

/** Was bei der Prüfung gefunden wurde. */
data class SkillFinding(val germanLabel: String) {

    /** Beschreibt einen Befund in einer Zeile. */
    fun describe(): String = germanLabel
}

/**
 * Ein geprüfter Skill-Kandidat.
 *
 * Der Typ erzwingt die Reihenfolge der Aufgabe: **erst** die Fakten sammeln,
 * **dann** das Urteil. Es gibt keinen Weg, ein Urteil zu erzeugen, ohne vorher
 * eine [ReviewEvidence] mit dem Ergebnis der Inhaltsprüfung vorzulegen.
 */
data class SkillCandidate(

    /** Der Name des Skills, wie er aufgerufen wird. */
    val name: String,

    /** Woher der Skill stammt (globale Skills, Plugin-Cache, Repository). */
    val origin: String,

    /**
     * Die Belastbarkeit der Lizenzangabe.
     *
     * Standard ist bewusst [LicenseEvidence.NOT_STATED]: Ein Kandidat, dessen
     * Lizenz niemand geprüft hat, soll nicht durch Weglassen des Arguments
     * als geprüft erscheinen.
     */
    val license: LicenseEvidence = LicenseEvidence.NOT_STATED,

    /** Was der Skill tut — der geprüfte Inhalt, nicht die Beschreibung. */
    val content: List<SkillFinding> = emptyList(),

    /** Werkzeuge, die der Skill für seine Arbeit braucht. */
    val requiredTools: List<String> = emptyList()
)

/**
 * Das Ergebnis der **Inhaltsprüfung** — der Schritt, der vor jeder
 * Ausführung liegen muss.
 */
data class ReviewEvidence(

    /** Wurde der Inhalt wirklich gelesen? */
    val contentInspected: Boolean = false,

    /** Führt der Skill Befehle aus, installiert er Pakete oder greift er auf das Netz zu? */
    val executesCommands: Boolean = false,

    /** Installiert der Skill Pakete oder lädt er Inhalte nach? */
    val installsOrDownloads: Boolean = false
) {

    /**
     * Darf dieser Kandidat **überhaupt** geprüft werden?
     *
     * Ohne gelesenen Inhalt gibt es kein Urteil: `mayRun` ist `false`, und
     * [SkillSupplyChainReview.review] liefert [Verdict.REJECTED]. Das ist die
     * Zusage „nicht ausgeführt, bevor der Inhalt geprüft ist" — sie sitzt an
     * der Verzweigung, nicht in einem Kommentar.
     */
    val mayRun: Boolean get() = contentInspected
}

/**
 * Die Bewertung eines Skill-Kandidaten (Aufgabe 123).
 *
 * Alle Regeln sind **reines Kotlin**: Die App liefert die Fakten aus der
 * Lieferkettenprüfung, diese Klasse entscheidet, was daraus folgt. Es wird
 * nichts ausgeführt, nichts installiert und nichts heruntergeladen.
 */
object SkillSupplyChainReview {

    /**
     * Bewertet einen Kandidaten anhand der geprüften Fakten.
     *
     * Die Reihenfolge ist die Aussage:
     *
     *  1. **Inhalt nicht geprüft** → abgelehnt. Ohne Lektüre gibt es kein
     *     Urteil, auch wenn Herkunft und Lizenz sonst stimmten.
     *  2. **Lizenz nicht belegt** → abgelehnt. Das betrifft auch
     *     [LicenseEvidence.UNVERIFIABLE_REFERENCE]: ein Verweis auf ein fehlendes
     *     Dokument belegt nichts.
     *  3. **Verdächtiger Inhalt** (Ausführung, Nachinstallation, Netzzugriff)
     *     → abgelehnt, unabhängig von der Lizenz.
     *  4. Sonst einsetzbar.
     */
    fun review(candidate: SkillCandidate, evidence: ReviewEvidence): Verdict = when {
        !evidence.mayRun -> Verdict.REJECTED
        candidate.license != LicenseEvidence.VERIFIED -> Verdict.REJECTED
        evidence.executesCommands || evidence.installsOrDownloads -> Verdict.REJECTED
        else -> Verdict.USABLE
    }

    /**
     * Muss der Nutzer einer Installation ausdrücklich zustimmen?
     *
     * **Immer.** Diese Methode gibt es, weil die Aufgabe ausdrücklich sagt:
     * Suche ist keine automatische Installationsfreigabe. Es gibt keinen
     * Codepfad, der eine Installation ohne Rückfrage ergänzt — die Antwort ist
     * konstant `true`, damit keine spätere Änderung sie versehentlich still
     * auf `false` setzt, ohne dass ein Test auffällt.
     */
    fun installRequiresUserConsent(): Boolean = true

    /**
     * Die Begründung für ein Urteil — nie leer.
     *
     * Sie nennt den **Grund**, nicht nur das Ergebnis, damit die Oberfläche
     * nicht einen unveränderten Bildschirm zeigt, wenn ein Skill abgelehnt
     * wurde.
     */
    fun explanation(candidate: SkillCandidate, evidence: ReviewEvidence): String = when {
        !evidence.mayRun ->
            "\"${candidate.name}\" wurde nicht ausgewertet: Der Inhalt ist nicht geprüft."

        candidate.license != LicenseEvidence.VERIFIED ->
            "\"${candidate.name}\" ist abgelehnt: Die Lizenz ist ${candidate.license.germanLabel}."

        evidence.executesCommands ->
            "\"${candidate.name}\" führt Befehle aus und wurde deshalb abgelehnt."

        evidence.installsOrDownloads ->
            "\"${candidate.name}\" installiert oder lädt Inhalte nach und wurde deshalb abgelehnt."

        else ->
            "\"${candidate.name}\" ist geprüft, lizenziert und führt nichts aus."
    }

    /**
     * Die Herkunft in der Form, wie sie im Nachweis steht.
     *
     * Eine leere Herkunft ist **keine** Herkunft: `require` weist sie ab, weil
     * „global" ohne Pfad nicht überprüfbar ist.
     */
    fun originLine(candidate: SkillCandidate): String {
        require(candidate.origin.isNotBlank()) {
            "Eine Herkunft ohne Angabe ist nicht überprüfbar."
        }
        return "${candidate.name} — ${candidate.origin}"
    }
}
