package org.claudroide.app.feature.skills

import org.claudroide.app.core.security.LicenseEvidence
import org.claudroide.app.core.security.ReviewEvidence
import org.claudroide.app.core.security.SkillCandidate
import org.claudroide.app.core.security.SkillSupplyChainReview
import org.claudroide.app.core.security.Verdict

/**
 * Task 129 — „Skills finden".
 *
 * Ziel: Für jede Implementierungsaufgabe mindestens einen global auffindbaren,
 * passenden Skill recherchieren. Ergebnis: Suchbegriffe, Kandidaten, Fundort,
 * Lizenz, Kompatibilität und Begründung dokumentieren.
 *
 * Diese Klasse ist das Suchprotokoll, nicht die Suche selbst: sie führt weder
 * Dateizugriff noch Netzverbindung aus, sondern bewertet, was die Suche
 * geliefert hat. Reines Kotlin, damit die Regeln auf der JVM prüfbar sind.
 *
 * ## Die tragende Trennung: Suchen ist nicht Installieren
 *
 * Der Schutz der Aufgabe lautet „Keine Installation während der Suche". Das ist
 * hier nicht nur eine documentation, sondern eine **strukturelle** Eigenschaft:
 *
 *  * Ein [DiscoveredSkill] ist **per Typdefinition kein Installationskandidat.**
 *    Er trägt weder ein Freigabefeld noch eine Entscheidung über eine Nutzung.
 *  * [SkillDiscoveryInventory.decideInstall] nimmt deshalb **nicht** einen
 *    [DiscoveredSkill], sondern nur eine [DiscoveryOutcome] **und** eine
 *    [SkillConsent] entgegen. Ein Suchtreffer allein genügt der Signatur nicht.
 *  * [SkillConsent] selbst kann der Aufrufer nicht erzeugen: die Konstruktoren
 *    sind `internal`, und der einzige öffentliche Weg ist
 *    [SkillDiscoveryInventory.recordConsent], das zusätzlich den
 *    [InstallRequest] mit der Kennung des Treffers verlangt. Damit lässt sich
 *    eine Freigabe nicht unterstellen, sie ohne dokumentierten Antrag
 *    „laufen zu lassen".
 *
 * ## Die zweite Zusage: Nichts gilt allein wegen seines Namens
 *
 * „Fertig, wenn keine Fähigkeit nur wegen ihres Namens als sicher gilt." Deshalb
 * trägt [DiscoveredSkill] **kein** Feld, das eine Prüfung vortäuschen könnte:
 * Ein **behaupteter** Name ist ein Argument für eine **Behauptung** über den
 * Inhalt, und eine Behauptung ist kein Nachweis. Es gibt keinen Konstruktor,
 * der aus einer Beschreibung ein geprüftes Urteil macht — der Weg von der
 * Behauptung zum belegten Inhalt führt ausschließlich über
 * [ContentEvidence.Inspected], das die tatsächlich gelesene
 * [ContentInspection] **mitführen muss**.
 *
 * ## Die dritte Zusage: Lücken werden ehrlich markiert
 *
 * „Nicht gefundene Skills ehrlich als offen markiert." Deshalb ist
 * [DiscoveryOutcome] ein `sealed interface` und [SearchOutcome] ein echter
 * Zustand: [SearchOutcome.NOT_FOUND] ist ein normaler, **nicht fehlerhafter**
 * Abschluss. Er unterscheidet sich von „gefunden, aber abgelehnt" in beiden
 * Richtungen — er zählt nicht als Treffer und er ist keine Ausnahme.
 */
private const val EMPTY_GAP_REASON = "open"

// ── Suchbegriff ──────────────────────────────────────────────────────────

/**
 * Ein einzelnes Suchbegriff aus einer Implementierungsaufgabe.
 *
 * Die Aufgabe verlangt, die **Suchbegriffe** zu dokumentieren — nicht nur das
 * Ergebnis. Deshalb ist der Begriff Teil des Protokolls und nicht Beiwerk der
 * Oberfläche.
 *
 * @property label Englische Kurzbezeichnung des Begriffs.
 * @property term Der Wortlaut, der tatsächlich gesucht wurde.
 * @property taskId Die Aufgabe, für die gesucht wurde — ohne sie ist nicht
 *   erkennbar, wofür ein Treffer überhaupt gesucht hat.
 */
data class SearchTerm(
    val label: String,
    val term: String,
    val taskId: String
) {

    /** Nur dieterms `label` und `taskId` — der Suchlauf selbst folgt getrennt. */
    val complete: Boolean
        get() = label.isNotBlank() && term.isNotBlank() && taskId.isNotBlank()
}

// ── Suchlauf ─────────────────────────────────────────────────────────────

/** Wo ein Suchlauf stattgefunden hat. */
enum class DiscoverySourceKind(val label: String, val germanLabel: String) {

    /** Die globalen Skills des Nutzers — der in der Aufgabe geforderte Ort. */
    GLOBAL_SKILLS("Global skills", "globale Skills"),

    /** Ein über Marktplatz oder Plugin bezogener Skill. */
    MARKETPLACE("Marketplace", "Marktplatz"),

    /** Ein aus einem Quellcode-Verzeichnis gelesener Skill. */
    SOURCE_REPOSITORY("Source repository", "Quellcode-Repository"),

    /** Der Umfang wurde nicht bestimmt — **kein** Deckel, der Erfolge verdeckt. */
    SCOPE_UNSPECIFIED("Scope unspecified", "Umfang nicht bestimmt")
}

/**
 * Der Verlauf **einer** Suche: die Begriffe, der Ort, und ob der Lauf eine
 * durchsuchte Stelle nennen kann.
 *
 * @property isGlobal Whether this search actually looked at the global scope.
 *   A run without a stated scope has *not* searched the global skills, and this
 *   flag says so instead of letting the caller assume it did.
 */
data class SearchRun(
    val terms: List<SearchTerm>,
    val source: DiscoverySourceKind,
    val searchedScope: String,
    val isGlobal: Boolean
) {

    /**
     * Ein Lauf zählt nur dann als global durchsucht, wenn **beides** stimmt:
     * Der Ort ist benannt ([DiscoverySourceKind.GLOBAL_SKILLS]) **und** es
     * wurde tatsächlich etwas durchsucht. Der übrige Ort zählt nicht, weil die
     * Aufgabe einen *global auffindbaren* Skill verlangt.
     */
    val isComplete: Boolean
        get() = terms.all { it.complete } &&
            terms.isNotEmpty() &&
            source == DiscoverySourceKind.GLOBAL_SKILLS &&
            searchedScope.isNotBlank()
}

// ── Fundort ──────────────────────────────────────────────────────────────

/** Wie belastbar die Angabe zum Ort eines gefundenen Skills ist. */
enum class ProvenanceEvidence(val label: String, val germanLabel: String) {

    /** Der Ort ist angegeben und als Quellangabe nachprüfbar. */
    RECORDED("Recorded source", "belegter Fundort"),

    /**
     * Der Ort wurde genannt, ist aber nicht nachprüfbar — etwa ein mündliches
     * „irgendwo im Cache". [DiscoveredSkill.discoveredIn] wird dann trotzdem
     * geführt, aber die Zeile ist keine Quelle.
     */
    UNCONFIRMED("Unconfirmed source", "unbestätigter Fundort"),

    /**
     * Es wurde **kein** Ort angegeben.
     *
     * Der eigene Zustand statt eines Leerstrings: ein fehlender Fundort ist ein
     * Befund, kein Formularfeld, das man unausgefüllt lässt.
     */
    MISSING("Source not recorded", "Fundort nicht erfasst")
}

// ── Lizenz ───────────────────────────────────────────────────────────────

/**
 * Wie belastbar die Lizenzangabe eines **gefundenen** Skills ist.
 *
 * Inhalt und Begründung sind bewusst nicht wiederholt: sie stehen in
 * [LicenseEvidence] aus Aufgabe 123. Hier kommt nur der Teil hinzu, den die
 * *Suche* sieht — die Frage nach der Lizenzdatei am Fundort.
 *
 * Das ist der reale Unterschied: „Lizenz nicht angegeben" heißt, der Autor
 * schweigt; „Lizenzdatei fehlt" heißt, jemand **behauptet** eine Lizenz und das
 * genannte Dokument liegt am Fundort nicht. Der eigene Lieferketten-Check von
 * ClauDroide fand genau das bei vier installierten Skills.
 */
enum class DiscoveredLicenseEvidence(val label: String, val germanLabel: String) {

    /** Eine Lizenz ist benannt **und** am Fundort liegt ein Lizenzdokument. */
    DOCUMENT_PRESENT("License document present", "Lizenzdokument vorhanden"),

    /**
     * Es wird auf ein Lizenzdokument verwiesen, das am Fundort **fehlt**.
     * Ein Verweis auf ein fehlendes Dokument belegt keine Nutzungsrechte.
     */
    FILE_MISSING("License file missing", "Lizenzdatei fehlt"),

    /**
     * Es wird überhaupt keine Lizenz genannt.
     *
     * **Der Standardfall** eines frisch konstruierten [DiscoveredSkill]: ein
     * stillschweigender Standardwert darf keinen Kandidaten so aussehen lassen,
     * als sei seine Lizenz geprüft.
     */
    NOT_STATED("License not stated", "keine Lizenz angegeben"),

    /**
     * Die Lizenzlage wurde an der Suchstelle **nicht geprüft**.
     *
     * Das ist nicht dasselbe wie „keine Lizenz": hier fehlt die **Untersuchung**,
     * und das Ergebnis steht noch aus. Beide Zustände bleiben unvereinbar mit
     * einer belegten Lizenz.
     */
    NOT_INSPECTED("License not inspected", "Lizenz nicht geprüft")
}

// ── Inhalt ───────────────────────────────────────────────────────────────
//
// Die Inhaltsbelege stehen in `SkillContentEvidence.kt`. Sie sind dort
// ausgelagert, weil sie einen eigenen, trennbaren Nachweis darstellen: der
// Übergang von der Behauptung zum belegten Inhalt.

// ── Kompatibilität ───────────────────────────────────────────────────────

/** Ob ein gefundener Skill zu der Zielumgebung passt. */
enum class CompatibilityVerdict(val label: String, val germanLabel: String) {

    /** Die Kompatibilität wurde gegen die Zielumgebung geprüft und passt. */
    COMPATIBLE("Compatible", "kompatibel"),

    /**
     * Die Umgebung des Skills passt nicht zur Zielumgebung — etwa weil er
     * andere Werkzeuge oder ein anderes Betriebssystem voraussetzt.
     */
    INCOMPATIBLE("Incompatible", "nicht kompatibel"),

    /**
     * Die Kompatibilität wurde **nicht geprüft**.
     *
     * Auch das ist ein eigener Zustand: die Aufgabe verlangt, die Kompatibilität
     * zu dokumentieren, und ein stillschweigend angenommenes „passt" wäre eine
     * erfundene Geräte- oder Umgebungsaussage.
     */
    NOT_ASSESSED("Compatibility not assessed", "Kompatibilität nicht geprüft")
}

// ── Der Suchtreffer ──────────────────────────────────────────────────────

/**
 * Ein **gefundener** Skill: Suchbegriff, Ort, Lizenzlage, Inhaltslage,
 * Kompatibilität — und eine Begründung, warum er gerade zu diesem Begriff passt.
 *
 * Der Typ ist der Kern der Zusage „Suche ist keine Installationsfreigabe":
 * Er hat **kein** Feld für eine Freigabe, keine Installationsentscheidung und
 * keine Möglichkeit, sich selbst zum Installationskandidaten zu erklären. Der
 * einzige Weg von hier zu einer Nutzung führt über
 * [SkillDiscoveryInventory.recordConsent].
 *
 * @property compatibilityOffender Das gefundene Hindernis, falls die
 *   Kompatibilität nicht passt. Ohne Angabe bleibt der Grund beim Pauschalwert
 *   „incompatible"; die Oberfläche soll aber sagen **woran** es lag.
 */
data class DiscoveredSkill(
    val name: String,
    val searchedWith: SearchTerm,
    val discoveredIn: String,
    val provenance: ProvenanceEvidence = ProvenanceEvidence.UNCONFIRMED,
    val licenseEvidence: DiscoveredLicenseEvidence = DiscoveredLicenseEvidence.NOT_STATED,
    val content: ContentEvidence = ContentEvidence.UNVERIFIED,
    val compatibility: CompatibilityVerdict = CompatibilityVerdict.NOT_ASSESSED,
    val compatibilityOffender: String = EMPTY_GAP_REASON,
    val rationale: String = EMPTY_GAP_REASON
) {

    /**
     * Passt der Treffer grundsätzlich zu seinem Suchbegriff?
     *
     * Nur [SearchTerm.complete] zählt: ein Treffer zu einem unvollständigen
     * Begriff (kein Wortlaut, keine Aufgabenkennung) belegt nicht, dass die
     * Suche überhaupt die richtige Frage gestellt hat.
     */
    val matchesTerm: Boolean get() = searchedWith.complete
}

// ── Zustand der Suche ───────────────────────────────────────────────────

/** Wie eine Suche ausgegangen ist. */
enum class SearchOutcome(val label: String, val germanLabel: String) {

    /** Mindestens ein Treffer, mindestens eine offene Frage. */
    PARTIAL("Partially covered", "teilweise abgedeckt"),

    /** Alle Suchanfragen mit Treffern beantwortet. */
    COMPLETE("Fully covered", "vollständig abgedeckt"),

    /**
     * Für mindestens einen Suchbegriff wurde **nichts** gefunden.
     *
     * Kein Fehler und keine Ausnahme, sondern ein erstklassiger Zustand: die
     * Aufgabe verlangt, nicht gefundene Skills ehrlich als offen zu markieren.
     * Wer das verschweigt, täuscht eine Abdeckung vor, die es nicht gibt.
     */
    NOT_FOUND("No match found", "nichts gefunden")
}

/**
 * Das Ergebnis eines [SearchRun].
 *
 * `sealed interface`, damit „nichts gefunden" **kein** Sonderfall in einem
 * Attrappen-Objekt ist: Es gibt keinen Weg, ein leeres Trefferergebnis als
 * Erfolg zu verpacken — der Compiler verlangt die Zustandsangabe.
 */
sealed interface DiscoveryOutcome {

    /** Der Suchlauf, zu dem dieses Ergebnis gehört. */
    val run: SearchRun

    /** Ob dieser Abschluss eine offene Frage meldet. */
    val isGap: Boolean

    /**
     * Die Treffer dieses Abschlusses — im Fall einer Lücke **leer**.
     *
     * Der eigene Standardwert statt eines `when`-Springs an jeder Stelle: Ein
     * Suchlauf ohne Treffer hat schlicht keine, und das ist ein gültiger Zustand
     * statt eines Sonderfalls, der beim Aufrufer abgefangen werden müsste.
     */
    val skills: List<DiscoveredSkill>
        get() = emptyList()

    /** Ein Suchlauf mit mindestens einem belegten Treffer. */
    data class Matches(override val run: SearchRun, override val skills: List<DiscoveredSkill>) : DiscoveryOutcome {

        /** Jeder Treffer muss zu seinem eigenen Suchbegriff passen. */
        override val isGap: Boolean get() = skills.isEmpty() || skills.any { !it.matchesTerm }
    }

    /** Die Suche wurde durchgeführt und hat nichts gefunden. */
    data class Gap(override val run: SearchRun, val reason: String) : DiscoveryOutcome {

        override val isGap: Boolean get() = true
    }
}

// ── Zustimmung ───────────────────────────────────────────────────────────

/**
 * Der Stand der Zustimmung des Nutzers für **eine** Kennung.
 *
 * Drei ausdrückliche Zustände, kein Standardwert, der eine Freigabe vortäuschen
 * könnte: [REVOKED] und [WITHDRAWN] sind beide **nein**, aber sie unterscheiden
 * sich im Grund — ein entzogener Widerruf sieht in der Oberfläche anders aus als
 * eine nie erteilte Freigabe.
 */
enum class ConsentState(val label: String, val germanLabel: String) {

    /** Der Nutzer hat der Installation dieses Skills ausdrücklich zugestimmt. */
    GRANTED("Consent granted", "Freigabe erteilt"),

    /** Der Nutzer hat die Zustimmung zurückgenommen. */
    REVOKED("Consent revoked", "Freigabe entzogen"),

    /** Es wurde nie zugestimmt — der Normalfall. */
    WITHDRAWN("Consent not given", "keine Freigabe erteilt")
}

/**
 * Die **einzige** Art, wie eine Installationsfreigabe entsteht.
 *
 * Die Konstruktoren sind `internal`: außerhalb dieser Datei kann niemand eine
 * Freigabe bauen, um sie einem Fund hinzuzufügen. Der einzige öffentliche Weg
 * ist [SkillDiscoveryInventory.recordConsent] — und der verlangt zusätzlich den
 * Antrag, damit am Installationsweg selbst nichts vorbeigelangen werden kann.
 *
 * @property scope Auf welche Kennung sich diese Zustimmung bezieht.
 */
data class SkillConsent internal constructor(
    val scope: String,
    val state: ConsentState,
    val grantedAt: Long,
    val revokedAt: Long?
) {

    /**
     * Gilt diese Zustimmung genau für [discoveredName]?
     *
     * Eine Zustimmung für einen anderen Skill gilt nicht mit. Das verhindert den
     * Fehler „einmal freigegeben, alles freigegeben".
     */
    fun covers(discoveredName: String): Boolean = scope == discoveredName
}

/** Der Antrag, den die Oberfläche vor der Freigabe erzeugt. */
data class InstallRequest(
    val discoveredName: String,
    val requestedBy: String,
    val summary: String
)

/** Das Urteil über eine Installation. */
enum class InstallDecision(val label: String, val germanLabel: String) {

    /** Freigegeben — aber nur, weil eine wirksame Zustimmung vorliegt. */
    AUTHORIZED("Install authorized", "Installation freigegeben"),

    /**
     * Abgelehnt. **Kein** Sonderfall, sondern der Weg, den eine fehlende, eine
     * entzogene oder eine auf einen anderen Skill bezogene Zustimmung nimmt.
     */
    REJECTED("Install rejected", "Installation abgelehnt")
}

// ── Suchprotokoll und Freigabe ───────────────────────────────────────────

/**
 * Das Suchprotokoll und die Freigabestelle (Aufgabe 129).
 *
 * Alle Regeln sind **reines Kotlin**: Die App liefert die Fakten aus der
 * Suche, dieses Objekt entscheidet, was daraus folgt. Es wird nichts gesucht,
 * nichts installiert und nichts heruntergeladen.
 */
object SkillDiscoveryInventory {

    /**
     * Muss der Nutzer einer Installation ausdrücklich zustimmen?
     *
     * **Immer.** Die Methode ist hier die Entsprechung zu
     * [SkillSupplyChainReview.installRequiresUserConsent] aus Aufgabe 123: Die
     * Antwort ist konstant `true`, damit keine spätere Änderung sie still auf
     * `false` setzt, ohne dass ein Test auffällt.
     */
    fun installRequiresUserConsent(): Boolean = true

    /**
     * Lohnt sich eine weitere Suche?
     *
     * Offen heißt hier ausdrücklich: **nicht gefunden**, oder gefunden, aber
     * ohne nachgewiesenen Inhalt. Ein Treffer mit ungeprüftem Inhalt ist kein
     * Ersatz für einen fehlenden Skill, und die Oberfläche soll genau das
     * anzeigen.
     */
    fun isOpenGap(skill: DiscoveredSkill): Boolean =
        skill.content == ContentEvidence.UNVERIFIED ||
            skill.licenseEvidence == DiscoveredLicenseEvidence.NOT_INSPECTED ||
            skill.provenance == ProvenanceEvidence.MISSING ||
            skill.compatibility == CompatibilityVerdict.NOT_ASSESSED

    /**
     * Legt den **Antrag** an, über den eine Zustimmung entstehen kann.
     *
     * Der Antrag ist absichtlich ein **eigenes** Wertobjekt: Er entsteht aus dem
     * Suchergebnis, ist aber selbst noch keine Freigabe. Die Freigabe entsteht
     * erst in [recordConsent].
     */
    fun requestInstall(skill: DiscoveredSkill, requestedBy: String): InstallRequest =
        InstallRequest(
            discoveredName = skill.name,
            requestedBy = requestedBy,
            summary = "${skill.name} — ${skill.discoveredIn}"
        )

    /**
     * Nimmt die ausdrückliche Zustimmung des Nutzers entgegen.
     *
     * Ohne passenden Antrag entsteht **keine** Zustimmung: [ConsentState.WITHDRAWN]
     * ist das Ergebnis, und jede nachfolgende Installationsentscheidung fällt
     * damit auf [InstallDecision.REJECTED]. So lässt sich eine Freigabe nicht
     * erfinden, indem man sie ohne Antrag „so eben" mitschickt.
     */
    fun recordConsent(
        request: InstallRequest,
        state: ConsentState,
        recordedAt: Long,
        revokedAt: Long? = null
    ): SkillConsent {
        val dokumentiert = request.discoveredName.isNotBlank() && request.requestedBy.isNotBlank()
        val endgueltig = dokumentiert && state == ConsentState.GRANTED && recordedAt > 0L
        val wirksam = if (endgueltig) ConsentState.GRANTED else ConsentState.WITHDRAWN
        return SkillConsent(
            scope = request.discoveredName,
            state = wirksam,
            grantedAt = recordedAt,
            revokedAt = revokedAt
        )
    }

    /**
     * Hebt eine erteilte Freigabe wieder auf.
     *
     * Auch hier gibt es keinen stillen Rückfall auf „gilt noch": Wer nicht
     * ausdrücklich zustimmt, gilt als nicht zugestimmt.
     */
    fun revokeConsent(consent: SkillConsent, revokedAt: Long): SkillConsent =
        SkillConsent(
            scope = consent.scope,
            state = ConsentState.REVOKED,
            grantedAt = consent.grantedAt,
            revokedAt = revokedAt
        )

    /** Die Herkunftszeile eines Treffers — nie leer, aber nicht immer belastbar. */
    fun provenanceLine(skill: DiscoveredSkill): String =
        "${skill.name} — ${skill.discoveredIn} (${skill.provenance.label})"

    /**
     * Die Treffer, die für diesen Abschluss **zählen**.
     *
     * Ein Treffer, der nicht zu seinem eigenen Suchbegriff passt, ist kein
     * Treffer für diesen Begriff: Die Suche hat dann die falsche Frage gestellt
     * oder den Inhalt falsch zugeordnet. Ihn mitzuzählen würde eine Abdeckung
     * vortäuschen, die es nicht gibt — und es widerspräche [DiscoveryOutcome.isGap],
     * das denselben Fall bereits als offene Frage meldet.
     */
    fun matchingSkills(outcome: DiscoveryOutcome): List<DiscoveredSkill> =
        outcome.skills.filter { it.matchesTerm }

    /**
     * Die Suche **selbst** ist ohne Treffer kein Fehler: sie liefert
     * [SearchOutcome.NOT_FOUND] und damit einen offenen Punkt, der in der
     * Oberfläche stehen bleibt.
     */
    fun searchOutcome(outcome: DiscoveryOutcome): SearchOutcome {
        val treffer = matchingSkills(outcome)
        return when {
            treffer.isEmpty() -> SearchOutcome.NOT_FOUND
            treffer.any { isOpenGap(it) } -> SearchOutcome.PARTIAL
            else -> SearchOutcome.COMPLETE
        }
    }

    /**
     * Die Begründung eines Suchtreffers — in der Form, in der sie dokumentiert
     * wird: der Begriff, der Ort, die Lizenzlage, die Kompatibilität.
     *
     * Fehlt die Begründung, wird das **gesagt** und nicht durch einen leeren
     * Text ersetzt.
     */
    fun candidateLine(skill: DiscoveredSkill): String =
        "${skill.name} [${skill.searchedWith.term}] @ ${skill.discoveredIn} " +
            "— license: ${skill.licenseEvidence.label}, " +
            "content: ${skill.content.label}, " +
            "compatibility: ${skill.compatibility.label}, " +
            rationaleLine(skill)

    /** Die Begründung, oder ein sichtbarer Platzhalter für eine fehlende. */
    fun rationaleLine(skill: DiscoveredSkill): String =
        if (skill.rationale.isBlank()) {
            "reasoning: not recorded"
        } else {
            "reasoning: ${skill.rationale}"
        }

    /**
     * Das Hindernis der Kompatibilität, oder ein sichtbarer Platzhalter.
     *
     * Ein [CompatibilityVerdict.INCOMPATIBLE] ohne Angabe des Hindernisses
     * bekommt hier den Pauschalwert „incompatible" zurück — nicht eine
     * erfundene Ursache.
     */
    fun compatibilityLine(skill: DiscoveredSkill): String = when (skill.compatibility) {
        CompatibilityVerdict.INCOMPATIBLE ->
            if (skill.compatibilityOffender.isBlank()) "incompatible" else skill.compatibilityOffender

        else -> skill.compatibility.label.lowercase()
    }

    /** Vergleicht einen Fund mit der Aufnahme, die ihn aufgenommen hat. */
    fun explainInsertion(skill: DiscoveredSkill, inserted: Boolean): String = when {
        inserted -> "\"${skill.name}\" wurde als Fund aufgenommen und begründet."
        skill.licenseEvidence == DiscoveredLicenseEvidence.FILE_MISSING ->
            "\"${skill.name}\" wurde nicht aufgenommen: Die genannte Lizenzdatei fehlt."

        skill.provenance == ProvenanceEvidence.MISSING ->
            "\"${skill.name}\" wurde nicht aufgenommen: Es ist kein Fundort erfasst."

        else -> "\"${skill.name}\" wurde nicht aufgenommen: Der Inhalt ist nicht belegt."
    }

    /**
     * Das Urteil über eine Installation — die einzige Stelle, die
     * [InstallDecision.AUTHORIZED] erzeugen kann.
     *
     * Der Weg dorthin ist absichtlich kurz und jede Stufe ist ein Ausschluss:
     * erst eine belegte Lizenz am belegten Ort, dann ein gelesener Inhalt mit
     * einem tragenden Befund, dann Kompatibilität, und **erst dann** eine
     * wirksame, auf genau diesen Skill bezogene Zustimmung. Fehlt eine dieser
     * Stufen, lautet das Ergebnis [InstallDecision.REJECTED] — nicht „gilt
     * vorläufig".
     */
    fun decideInstall(outcome: DiscoveryOutcome, consent: SkillConsent?): InstallDecision = when {
        outcome is DiscoveryOutcome.Gap -> InstallDecision.REJECTED
        outcome.skills.isEmpty() -> InstallDecision.REJECTED
        matchingSkills(outcome).all { !isApproved(it) } -> InstallDecision.REJECTED
        !consentGrantsAny(consent, matchingSkills(outcome)) -> InstallDecision.REJECTED
        else -> InstallDecision.AUTHORIZED
    }

    /**
     * Trägt der Inhaltsnachweis einer anderen Entscheidung als den Task-123-Test?
     *
     * Bei `false` prüft diese Klasse allein weiter; bei `true` entscheidet der
     * etablierte [SkillSupplyChainReview]. Die Weiche verhindert, dass ein
     * Fundstück einen bewusst engen Weg neu erfindet.
     */
    fun useSupplyChainReview(): Boolean = true

    /**
     * Übernimmt ein Urteil aus Aufgabe 123.
     *
     * Wird nur aufgerufen, wenn [useSupplyChainReview] `true` ist: Inhalt und
     * Lizenz bleiben die dort bewiesenen Kriterien, damit der Treffer nicht
     * laxer geprüft wird als ein über die Lieferkette bezogener Kandidat.
     */
    fun delegateToSupplyChainReview(skill: DiscoveredSkill): Verdict =
        SkillSupplyChainReview.review(supplyChainCandidate(skill), reviewEvidence(skill))

    /** Die Begründung einer Installation — nennt immer den Grund. */
    fun explanation(outcome: DiscoveryOutcome, consent: SkillConsent?): String = when {
        outcome is DiscoveryOutcome.Gap ->
            "Installation abgelehnt: Es wurde kein passender Skill gefunden."

        outcome.skills.isEmpty() ->
            "Installation abgelehnt: Die Suche hat keinen Treffer geliefert."

        consent == null || consent.state == ConsentState.WITHDRAWN ->
            "Installation abgelehnt: Es liegt keine Zustimmung des Nutzers vor."

        consent.state == ConsentState.REVOKED ->
            "Installation abgelehnt: Die Zustimmung wurde zurückgenommen."

        matchingSkills(outcome).all { !isApproved(it) } ->
            "Installation abgelehnt: Kein Treffer ist nachweislich geprüft und lizenziert."

        !consent.covers(matchingSkills(outcome).first { isApproved(it) }.name) ->
            "Installation abgelehnt: Die Zustimmung gilt für einen anderen Skill."

        else -> "Installation freigegeben: geprüfter Inhalt, belegte Lizenz, Zustimmung liegt vor."
    }

    // ── Innere Regeln ──────────────────────────────────────────────────

    /**
     * Darf ein Treffer überhaupt einen Installationsantrag tragen?
     *
     * Die drei Ausschlüsse sind je eine eigene Lücke der Aufgabe: kein
     * nachprüfbarer Ort, eine behauptete statt belegte Lizenz, und Inhalt, der
     * nicht belegt ist.
     */
    private fun isApproved(skill: DiscoveredSkill): Boolean {
        val ortBelegt = skill.provenance == ProvenanceEvidence.RECORDED && skill.discoveredIn.isNotBlank()
        val lizenzBelegt = skill.licenseEvidence == DiscoveredLicenseEvidence.DOCUMENT_PRESENT
        val inhaltBelegt = skill.content.isVerified
        val passt = skill.compatibility == CompatibilityVerdict.COMPATIBLE
        return ortBelegt && lizenzBelegt && inhaltBelegt && passt
    }

    /** Reicht die Zustimmung für **irgendeinen** der aufgenommenen Treffer? */
    private fun consentGrantsAny(consent: SkillConsent?, skills: List<DiscoveredSkill>): Boolean {
        if (consent == null || consent.state != ConsentState.GRANTED) return false
        return skills.any { isApproved(it) && consent.covers(it.name) }
    }

    /** Wandelt einen geprüften Fund in den Kandidaten aus Aufgabe 123. */
    private fun supplyChainCandidate(skill: DiscoveredSkill): SkillCandidate = SkillCandidate(
        name = skill.name,
        origin = skill.discoveredIn,
        license = if (skill.licenseEvidence == DiscoveredLicenseEvidence.DOCUMENT_PRESENT) {
            LicenseEvidence.VERIFIED
        } else {
            LicenseEvidence.NOT_STATED
        }
    )

    /** Und dessen Prüfnachweis — dieselbe Reihenfolge, dieselben Kriterien. */
    private fun reviewEvidence(skill: DiscoveredSkill): ReviewEvidence {
        val inspection = skill.content.inspection
        return ReviewEvidence(
            contentInspected = skill.content.isVerified && inspection != null,
            executesCommands = inspection?.executesCommands ?: false,
            installsOrDownloads = inspection?.installsOrDownloads ?: false
        )
    }
}
