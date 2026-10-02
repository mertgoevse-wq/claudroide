package org.claudroide.app.feature.skills

/**
 * Task 124 — „Skill-Installation freigeben".
 *
 * Diese Datei baut die **Schicht, die Aufgaben 129 und 130 offengelassen haben.**
 * 129 lieferte Suche und Zustimmung, 130 lieferte die Kompatibilitätsprüfung —
 * aber was dem Nutzer beim Installieren **gezeigt** wird, war noch nirgends
 * festgehalten. Genau das ist der Auftrag dieser Datei: ein Bestätigungsdialog,
 * der Quelle, Lizenz, Umfang, Installationsort und Entfernen benennt, und der
 * **einzeln** bestätigt werden muss.
 *
 * ## Die tragende Trennung: der Dialog entscheidet, er installiert nicht
 *
 * [SkillConfirmationDialog] hat **keine** Methode, die etwas installiert,
 * ausführt, herunterlädt oder schreibt — geprüft per Reflexion. Was der Dialog
 * liefert, ist ein *Wert* ([SkillConfirmation]) und eine *Zustimmung*
 * ([SkillConsent]). Die eigentliche Dateiverwaltung bleibt beim Betriebssystem.
 *
 * Das ist dieselbe Grenze, die [McpToolConnectionRegistry] in Aufgabe 134
 * zieht: der Wächter prüft, die Ausführung passiert woanders.
 *
 * ## Warum „einzeln" eine Eigenschaft des Typs ist
 *
 * Es gibt keine Methode, die mehrere Skills auf einmal freigibt —
 * `confirmAll` existiert, **gibt aber `null` zurück**. Eine Sammelfreigabe wäre
 * die naheliegende Abkürzung („alle drei gefunden, freigeben?"), und genau sie
 * würde die Zusage „jede globale Installation einzeln bestätigt" aufheben, ohne
 * dass irgendwo eine Zeile Code das verbieten müsste.
 *
 * ## Ablehnen ist ein Ergebnis, kein Abbruch
 *
 * [installationOutcome] beantwortet die zweite Hälfte des Auftrags: Was passiert
 * mit den **anderen** Skills, wenn einer abgelehnt wird? Die Antwort ist
 * „nichts": ein abgelehnter Skill nimmt keinen mit und blockiert keinen anderen.
 * Ein Fehler beim einen darf nicht als Grund dienen, den anderen ungefragt mit
 * zu installieren oder wegzulassen.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz.
 */

// ── Umfang und Installationsort ──────────────────────────────────────────

/**
 * Wie weit reicht eine Fähigkeit?
 *
 * Die Trennung ist die der Aufgabe: [GLOBAL] bedeutet, dass die Fähigkeit **über
 * das Projekt hinaus** gilt — sie bliebe bestehen, wenn das Projekt wechselt.
 * Genau deshalb steht bei einem globalen Recht die Warnung im Dialog, und genau
 * deshalb landet es **nicht** im Projektordner ([defaultSite]).
 */
enum class InstallScope(val label: String, val germanLabel: String) {

    /** Nur dieses Projekt. Beim Projektwechsel fällt die Fähigkeit weg. */
    PROJECT("Project scope", "nur dieses Projekt"),

    /** Über das Projekt hinaus — bleibt bestehen, wenn das Projekt wechselt. */
    GLOBAL("Global scope", "über das Projekt hinaus")
}

/**
 * Wohin eine Fähigkeit geschrieben würde.
 *
 * Der Pfad ist eine **Anzeige**, kein Vollzug: [SkillConfirmationDialog] schreibt
 * nichts. Der Ort wird trotzdem benannt, weil „irgendwo installieren" für den
 * Nutzer dieselbe Wirkung hat wie „nicht nachvollziehbar".
 */
enum class InstallSiteKind(val label: String, val germanLabel: String) {

    /** In den Skillordner **dieses** Projekts. */
    PROJECT_SKILLS("Project skills", "Projektordner"),

    /** In den gemeinsamen Skillordner — bleibt über Projekte hinweg bestehen. */
    GLOBAL_SKILLS("Global skills", "globaler Ordner")
}

/**
 * Der benannte Installationsort.
 *
 * Gleiche Kennung, anderer Ort = **zwei verschiedene Ziele**. Das ist kein
 * Detail: ein global installierter Skill und derselbe Skill im Projektordner
 * verhalten sich unterschiedlich beim Projektwechsel.
 */
data class InstallSite(
    val kind: InstallSiteKind,
    val path: String
) {
    /** Die Zeile für den Dialog. */
    fun displayLine(): String = "${kind.germanLabel}: $path"
}

// ── Die Wirkung, die ein Inhalt hat ───────────────────────────────────────

/** Was ein geprüfter Inhalt tut. */
enum class ContentEffectKind(val label: String, val germanLabel: String) {

    /** Liest. Eine reine Recherche-Fähigkeit. */
    READ_ONLY("Reads only", "liest nur"),

    /** Greift auf das Netz zu. */
    NETWORK("Reaches the network", "greift auf das Netz zu"),

    /** Führt Befehle aus. */
    EXECUTES("Executes commands", "führt Befehle aus"),

    /** Lädt etwas nach. */
    DOWNLOADS("Installs or downloads", "lädt etwas nach")
}

/**
 * Eine **benannte** Wirkung des Inhalts.
 *
 * Ohne [statement] ist das kein Befund, sondern ein leeres Formular — dieselbe
 * Regel wie bei `ContentFinding` aus Aufgabe 129. Deshalb ist der Wert per
 * `require` erzwungen.
 */
data class ContentEffect(
    val statement: String,
    val kind: ContentEffectKind
) {
    init {
        require(statement.isNotBlank()) { "Eine Wirkung braucht eine Aussage." }
    }

    /**
     * Ist das eine **nachladende** Wirkung?
     *
     * Lesen steht bewusst **nicht** neben den anderen: eine Recherche-Fähigkeit
     * darf lesen und suchen. Sie darf nur nichts starten und nichts nachladen.
     */
    val isExecution: Boolean
        get() = kind != ContentEffectKind.READ_ONLY
}

// ── Das Ergebnis des Ablehnens ────────────────────────────────────────────

/**
 * Was nach einer Ablehnung installiert werden darf.
 *
 * Reines Ergebnis, kein Vollzug: `allowedNames` sagt, was **erlaubt** wäre —
 * geschrieben wird hier nichts.
 */
data class InstallationOutcome(
    val allowedNames: List<String>,
    val reasons: List<String>
) {

    /** Darf überhaupt etwas installiert werden? */
    val isAllowed: Boolean get() = allowedNames.isNotEmpty()

    /** Die Gründe — jede Ablehnung nennt ihren eigenen. */
    fun refusalReasons(): List<String> = reasons
}

// ── Entfernen ────────────────────────────────────────────────────────────

/**
 * Was beim Entfernen einer Fähigkeit passiert — und was ausdrücklich **nicht**.
 *
 * Der letzte Satz ist der wichtige: „Entfernen" darf nicht als „Projekt löschen"
 * gelesen werden. Die Fähigkeit verschwindet, die Dateien des Projekts bleiben.
 */
data class RemovalPlan internal constructor(
    val skillName: String,
    val locationDescription: String
) {
    /** Die Zeile für den Dialog. */
    fun displayLine(): String =
        "Entfernen würde „$skillName“ aus $locationDescription nehmen. Projektdateien bleiben unberührt."

    companion object {

        /**
         * Der einzige Weg zu einem Entfernplan.
         *
         * Ohne Namen ist das keine Zusage, sondern eine leere Anweisung — deshalb
         * `require` statt einer leeren Zeile.
         */
        fun forSkill(skillName: String, projectId: String): RemovalPlan {
            require(skillName.isNotBlank()) { "Ein Entfernplan braucht einen Skillnamen." }
            require(projectId.isNotBlank()) { "Ein Entfernplan braucht das Projekt, aus dem entfernt wird." }
            return RemovalPlan(skillName, "dem Skillverzeichnis von „$projectId“")
        }
    }
}

// ── Die Zusammenfassung, die der Dialog anzeigt ───────────────────────────

/**
 * Alles, was der Dialog **zeigen** muss — als ein reiner Wert.
 *
 * Jede Zeile ist einzeln benannt, damit die Oberfläche nichts weglassen kann,
 * indem sie eine Zusammenfassung baut. Der Dialog selbst ist die einzige
 * Entscheidungsstelle; dieser Wert trägt nichts bei und sendet nichts.
 *
 * **Kein Feld für Inhalt, Schlüssel oder Adresse.** Geprüft per Reflexion: ein
 * solches Feld hier wäre ein Weg, vertrauliche Angaben in eine Anzeige zu
 * bringen, die der Nutzer nur überfliegt.
 */
data class SkillConfirmation(
    val skillName: String,
    val scope: InstallScope,
    val sourceLine: String,
    val licenseLine: String,
    val scopeLine: String,
    val siteLine: String,
    val removalLine: String,
    val effects: List<ContentEffect>
) {

    /**
     * Der Platzhalter für eine **offene** Angabe.
     *
     * Kein Leerfeld, sondern eine Aussage: „offen" heißt, dass hier nichts
     * belegt ist. Er steht an [SkillConfirmation], weil er eine Eigenschaft der
     * **Anzeige** ist — der Ort, an dem etwas offen bleiben kann, gehört in die
     * Zeile, die es betrifft, nicht in eine Hilfsklasse daneben.
     */
    companion object {
        const val UNKNOWN: String = "offen"
    }

    /**
     * Die Anzeige in fester Reihenfolge.
     *
     * Die Reihenfolge ist die Aussage: erst **was**, dann **woher**, dann **ob
     * rechtsmäßig**, dann **wie weit**, dann **wohin**, dann **wie wieder
     * rückgängig**. Ein Dialog, der mit dem Zielort beginnt, liest sich wie eine
     * Werbung.
     */
    fun displayLines(): List<String> = buildList {
        add("Fähigkeit: $skillName")
        add(sourceLine)
        add(licenseLine)
        add(scopeLine)
        add(siteLine)
        add(removalLine)
        effects.forEach { add("Wirkung: ${it.statement} (${it.kind.germanLabel})") }
    }
}

// ── Der Dialog ───────────────────────────────────────────────────────────

/**
 * Der Bestätigungsdialog (Aufgabe 124).
 *
 * **Er entscheidet und zeigt. Er installiert nichts.** Siehe
 * [SkillConfirmationDialog] — genauer: die Datei-Dokumentation oben.
 */
class SkillConfirmationDialog private constructor(
    val skill: DiscoveredSkill,
    val audit: EnvironmentAudit
) {

    /**
     * Der zentrale Wert: was der Dialog anzeigt.
     *
     * Eine offene Angabe wird **offen** gezeigt und nicht ergänzt. Der Platzhalter
     * [UNKNOWN] sagt genau das — er ist kein Leerfeld, sondern eine Aussage.
     */
    fun confirmation(): SkillConfirmation {
        val scope = resolveScope()
        return SkillConfirmation(
            skillName = skill.name,
            scope = scope,
            sourceLine = sourceLine(),
            licenseLine = licenseLine(),
            scopeLine = scopeLine(scope),
            siteLine = siteLine(scope),
            removalLine = RemovalPlan.forSkill(skill.name, skill.searchedWith.taskId).displayLine(),
            effects = effects()
        )
    }

    /** Darf der Nutzer hier überhaupt bestätigen? */
    fun mayConfirm(): Boolean = refusalReasons().isEmpty()

    /**
     * Jede Ablehnung mit ihrem Grund.
     *
     * Die **Reihenfolge ist die Aussage**: erst die Belastbarkeit der Prüfung,
     * dann die Lizenz, dann die nachladende Wirkung. Wer den ersten Grund nicht
     * nennt, verschweigt, dass überhaupt nichts geprüft wurde.
     */
    fun refusalReasons(): List<String> {
        val gruende = mutableListOf<String>()

        if (!audit.isGrounded) {
            gruende += "Der Fundort ist nicht belegt — es wurde nichts geprüft, was bestätigt werden könnte."
        }
        if (!audit.licenseVerified) {
            gruende += "Die Lizenz ist nicht nachprüfbar belegt."
        }
        if (!audit.executesNothing) {
            gruende += "Der Inhalt führt Befehle aus und ist damit kein reiner Anweisungstext."
        }
        if (!audit.downloadsNothing) {
            gruende += "Der Inhalt installiert oder lädt etwas nach."
        }
        if (audit.network != NetworkBehaviour.NONE) {
            gruende += "Der Inhalt greift auf das Netz zu: ${audit.network.germanLabel}."
        }
        return gruende
    }

    // ── Der Antrag und die Zustimmung ──────────────────────────────────────

    /**
     * Legt den **Antrag** an, über den eine Zustimmung entstehen kann.
     *
     * Der Antrag ist ein eigener Wert und ausdrücklich **noch keine Freigabe**.
     */
    fun request(requestedBy: String): InstallRequest =
        InstallRequest(
            discoveredName = skill.name,
            requestedBy = requestedBy,
            summary = "${skill.name} — ${skill.discoveredIn} (${scopeOf().germanLabel})"
        )

    /**
     * Nimmt die ausdrückliche Entscheidung des Nutzers entgegen.
     *
     * Die Wege sind bewusst **begrenzt**: Ein Antrag mit leerem Anfragenden oder
     * eine Zeitangabe `0` ergeben [ConsentState.WITHDRAWN]. Damit kann keine
     * Freigabe entstehen, indem man sie „so eben" mitschickt. Die eigentliche
     * Zustimmung wird an [SkillDiscoveryInventory] delegiert — es gibt keine
     * zweite Wahrheit.
     */
    fun confirm(
        request: InstallRequest,
        state: ConsentState,
        grantedAt: Long,
        revokedAt: Long? = null
    ): SkillConsent {
        val passt = request.discoveredName == skill.name
        val wirksamerAntrag = if (passt) request else request.copy(requestedBy = "")
        return SkillDiscoveryInventory.recordConsent(wirksamerAntrag, state, grantedAt, revokedAt)
    }

    /**
     * **Es gibt keine Sammelfreigabe.**
     *
     * Die Methode existiert, damit die Forderung der Aufgabe **strukturell** ist:
     * Wer nach einer Bestätigung für mehrere Skills sucht, findet genau diese
     * Stelle — und sie gibt `null` zurück. Ein Weg, der hier eine Sammelzustimmung
     * erzeugen könnte, wäre die Aufhebung der Zusage „jede globale Installation
     * einzeln bestätigt".
     */
    fun confirmAll(
        candidates: List<DiscoveredSkill>,
        consentedName: String,
        grantedAt: Long
    ): SkillConsent? = null

    /** Der Ort, an dem die Fähigkeit landen würde — angezeigt, nicht vollzogen. */
    fun removalPlan(): RemovalPlan =
        RemovalPlan.forSkill(skill.name, skill.searchedWith.taskId)

    /** Entfernen wird immer angeboten, nicht nur nach einer Fehlentscheidung. */
    fun mayRemove(): Boolean = skill.name.isNotBlank()

    // ── Die Zeilen ─────────────────────────────────────────────────────────

    private fun sourceLine(): String =
        if (audit.source.isBlank() || audit.source == SkillConfirmation.UNKNOWN) {
            "Quelle: ${SkillConfirmation.UNKNOWN}"
        } else {
            "Quelle: ${audit.source} (${audit.name})"
        }

    /**
     * Der Zielort — oder ein ausdrückliches **Nicht**gewusst.
     *
     * Ohne belegten Fundort wird **kein** Pfad genannt. Das ist keine
     * Vorsichtslosigkeit, sondern die Kette: Wenn schon die Quelle offen ist,
     * wurde nichts geprüft, und ein Zielpfad aus einem ungeprüften Fund wäre
     * die erste erfundene Angabe dieses Dialogs. Die Reihenfolge schützt sich
     * selbst — [SkillConfirmation.sourceLine] steht im Display **vor** dieser
     * Zeile.
     *
     * Der Pfad selbst ist ohnehin nur eine **Anzeige**: es wird nichts geschrieben.
     */
    private fun siteLine(scope: InstallScope): String {
        if (!audit.isGrounded) return "Installationsort: ${SkillConfirmation.UNKNOWN}"
        val projekt = skill.searchedWith.taskId
        return defaultSite(skill.name, projekt, scope).displayLine()
    }

    private fun licenseLine(): String = when {
        skill.licenseEvidence == DiscoveredLicenseEvidence.DOCUMENT_PRESENT && audit.licenseVerified ->
            "Lizenz: nachprüfbar belegt (${skill.licenseEvidence.germanLabel})"
        else ->
            "Lizenz: offen — ${skill.licenseEvidence.germanLabel}"
    }

    private fun scopeLine(scope: InstallScope): String {
        val rechte = audit.requiredPermissions.map { it.name }
        val grenze = if (scope == InstallScope.GLOBAL) {
            " — dies gilt über das Projekt hinaus"
        } else {
            ""
        }
        val aufzaehlung = if (rechte.isEmpty()) {
            "keine zusätzlichen Rechte"
        } else {
            "Rechte: ${rechte.joinToString(", ")}"
        }
        return "Umfang: ${scope.germanLabel}$grenze, $aufzaehlung"
    }

    private fun scopeOf(): InstallScope = resolveScope()

    private fun resolveScope(): InstallScope =
        if (audit.globalPermissions.isNotEmpty()) InstallScope.GLOBAL else InstallScope.PROJECT

    /**
     * Die **benannten** Wirkungen des Inhalts.
     *
     * Aus [ContentEvidence], nicht aus dem Audit: die Wirkungen müssen im Dialog
     * einzeln stehen, weil „liest nur" und „führt Befehle aus" für den Nutzer
     * zwei verschiedene Entscheidungen sind. Eine Zusammenfassung würde genau die
     * harmlose Wirkung zeigen und die unangenehme verschweigen.
     */
    private fun effects(): List<ContentEffect> {
        val liste = mutableListOf<ContentEffect>()
        val inspection = skill.content.inspection
        if (inspection == null) {
            liste += ContentEffect("Inhalt nicht gelesen", ContentEffectKind.EXECUTES)
            return liste
        }
        if (inspection.readsFiles) {
            liste += ContentEffect("liest Dateien", ContentEffectKind.READ_ONLY)
        }
        if (inspection.reachesNetwork) {
            liste += ContentEffect("greift auf das Netz zu", ContentEffectKind.NETWORK)
        }
        if (inspection.executesCommands) {
            liste += ContentEffect("führt Befehle aus", ContentEffectKind.EXECUTES)
        }
        if (inspection.installsOrDownloads) {
            liste += ContentEffect("installiert oder lädt nach", ContentEffectKind.DOWNLOADS)
        }
        if (liste.isEmpty()) {
            liste += ContentEffect("liest nichts und startet nichts", ContentEffectKind.READ_ONLY)
        }
        return liste
    }

    companion object {

        /**
         * Der einzige Weg zu einem Dialog.
         *
         * Ohne Skill gibt es nichts zu bestätigen — deshalb `require` statt einer
         * leeren Anzeige.
         */
        fun of(skill: DiscoveredSkill, audit: EnvironmentAudit): SkillConfirmationDialog {
            require(skill.name.isNotBlank()) { "Ein Dialog braucht eine Fähigkeit." }
            return SkillConfirmationDialog(skill, audit)
        }

        /**
         * Der Standardort für [scope].
         *
         * **Der Umfang bestimmt den Ort, nicht der Name.** Ein globaler Skill
         * gehört nicht in den Projektordner, auch wenn er genauso heißt wie ein
         * lokaler — sonst bliebe er beim Projektwechsel bestehen, ohne dass das
         * jemand bemerkt.
         */
        fun defaultSite(skillName: String, projectId: String, scope: InstallScope): InstallSite {
            require(skillName.isNotBlank()) { "Ein Installationsort braucht einen Namen." }
            return when (scope) {
                InstallScope.PROJECT -> {
                    require(projectId.isNotBlank()) {
                        "Ein Projektort ohne Projektkennung waere ein erfundener Ort."
                    }
                    InstallSite(InstallSiteKind.PROJECT_SKILLS, "$projectId/.claudroide/skills/$skillName")
                }
                InstallScope.GLOBAL ->
                    InstallSite(InstallSiteKind.GLOBAL_SKILLS, "skills/$skillName (global)")
            }
        }

        /**
         * Der Ort aus dem Umfang einer Prüfung.
         *
         * Bequemlichkeit für Aufrufer, die keinen Umfang führen.
         */
        fun defaultSite(skillName: String, projectId: String): InstallSite =
            defaultSite(skillName, projectId, InstallScope.PROJECT)

        /**
         * Was nach einer Ablehnung übrig bleibt.
         *
         * Die Zusage „Ablehnen installiert keine andere Fähigkeit" ist genau das:
         * der abgelehnte Name fällt heraus, **alle anderen bleiben**. Ein Fehler
         * beim einen darf den anderen nicht mitnehmen.
         */
        fun remainingAfterRejection(
            candidates: List<DiscoveredSkill>,
            rejectedName: String
        ): List<DiscoveredSkill> =
            if (rejectedName.isBlank()) candidates
            else candidates.filterNot { it.name == rejectedName }

        /**
         * Was tatsächlich installiert werden dürfte — und warum nicht mehr.
         *
         * Drei Wege, und keiner führt an der Zustimmung vorbei:
         *  * kein bestätigter Name → nichts erlaubt,
         *  * ein Name, der nicht im Verzeichnis stand → nichts erlaubt,
         *  * der Name steht im Verzeichnis und wurde bestätigt → genau dieser eine.
         */
        fun installationOutcome(
            candidates: List<DiscoveredSkill>,
            rejectedName: String?,
            consentedName: String?
        ): InstallationOutcome {
            val verbleib = remainingAfterRejection(candidates, rejectedName.orEmpty())

            if (consentedName.isNullOrBlank()) {
                return InstallationOutcome(
                    allowedNames = emptyList(),
                    reasons = listOf("Es liegt keine Bestätigung des Nutzers vor.")
                )
            }

            val bestaetigt = verbleib.firstOrNull { it.name == consentedName }
                ?: return InstallationOutcome(
                    allowedNames = emptyList(),
                    reasons = listOf(
                        "„$consentedName“ steht nicht zur Auswahl — dafür gibt es keine Bestätigung."
                    )
                )

            return InstallationOutcome(
                allowedNames = listOf(bestaetigt.name),
                reasons = emptyList()
            )
        }
    }
}

/**
 * Die Regeln der Installation (Aufgabe 124).
 *
 * Reines Kotlin: die App liefert die Fakten, dieses Objekt entscheidet. Es wird
 * nichts installiert, nichts geschrieben und nichts heruntergeladen.
 */
object SkillCompatibility {

    /**
     * Muss der Nutzer einer globalen Installation ausdrücklich zustimmen?
     *
     * **Immer.** Konstant `true`, damit keine spätere Änderung sie still
     * abschaltet, ohne dass ein Test auffällt. Das ist die Entsprechung zu
     * [SkillDiscoveryInventory.installRequiresUserConsent] aus Aufgabe 129 und
     * [McpToolConnectionPolicy.connectionRequiresUserConsent] aus Aufgabe 134 —
     * dieselbe Zusage, an drei voneinander unabhängigen Stellen abgesichert.
     */
    fun installationRequiresUserConsent(): Boolean = true
}
