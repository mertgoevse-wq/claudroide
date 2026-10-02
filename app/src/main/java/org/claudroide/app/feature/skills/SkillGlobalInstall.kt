package org.claudroide.app.feature.skills

/**
 * Task 131 — „Globalen Skill installieren".
 *
 * Diese Datei schließt die Lücke zwischen **131** und dem, was danach kommt.
 * Aufgabe 124 zeigte den Bestätigungsdialog; diese Datei berechnet, **was**
 * geschrieben würde, **wo** es hingeht, **was dort schon liegt** und **was
 * nachher wirklich da ist**. Sie schreibt selbst nichts — und das ist die
 * tragende Grenze.
 *
 * ## Der Plan schreibt nicht, er sagt
 *
 * [SkillGlobalInstall.plan] liefert einen [GlobalInstallPlan]: eine Liste von
 * Pfaden, ein Zielort, eine Liste von Konflikten und eine Sicherung. Die
 * tatsächliche Dateisystem-Operation passiert außerhalb dieser Datei, und das
 * Ergebnis kommt als **Wert** zurück ([InstallWriteResult]). Damit bleibt die
 * Aussage prüfbar: Ein Plan kann sich nicht verschreiben, und ein Ergebnis kann
 * sich nicht im Nachhinein als Erfolg ausgeben.
 *
 * Reflextionstest: `SkillGlobalInstall` hat **keine** Methode namens `write`,
 * `copyTo`, `delete` oder `mkdir`. Wer hier Dateien anfasst, hat die Grenze
 * gebrochen, und ein Test fällt.
 *
 * ## Die vier Zusagen der Aufgabe als Eigenschaften
 *
 *  1. **Quelle und globaler Geltungsbereich sind sichtbar.** Beides steht in
 *     [InstallationPreview.displayLines] — und `preview` liefert **niemals** eine
 *     Zustimmung mit. Wer schauen will, bekommt Informationen, keine Erlaubnis.
 *  2. **Kein ungefragtes Überschreiben.** Ein Konflikt ist ein *Ergebnis*, kein
 *     Nebenhinweis. `mayWrite()` ist `false`, solange `overwriteConfirmed` fehlt —
 *     und diese Variable wird nirgends aus einer anderen abgeleitet.
 *  3. **Vor dem Ersetzen wird gesichert.** `backupPaths` enthält genau die
 *     Dateien, die auch wirklich ersetzt werden. Nichts zu ersetzen heißt: nichts
 *     zu sichern, und der Plan sagt das auch.
 *  4. **Nachher wird nachgemessen.** [InstallWriteResult.isComplete] prüft drei
 *     Dinge getrennt: fehlende Pfade, unerwartete Pfade und die Frage, ob der Plan
 *     überhaupt schreiben durfte. „Kein Fehler aufgetreten" ist kein Beweis, dass
 *     geschrieben wurde.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz.
 */

// ── Dateien und Konflikte ────────────────────────────────────────────────

/**
 * Eine Datei, die Teil einer Fähigkeit ist.
 *
 * **Der Pfad ist relativ** und wird gegen den Zielordner geprüft. Ein Pfad mit
 * `..` oder führendem `/` wird per `require` abgewiesen — bevor irgendetwas
 * geplant wird. Das ist derselbe Schutz wie `PathBoundaryGuard` aus Aufgabe 120,
 * nur eine Ebene früher: hier wird noch gar nichts gelesen.
 */
data class EnvironmentFile(
    val relativePath: String,
    val sizeBytes: Long = 0L
) {
    init {
        require(relativePath.isNotBlank()) { "Eine Datei braucht ihren Pfad." }
        require(!relativePath.startsWith("/")) {
            "Ein absoluter Pfad würde das Ziel verlassen: $relativePath"
        }
        require(!relativePath.split('/').contains("..")) {
            "Ein Pfad darf nicht aus dem Zielordner ausbrechen: $relativePath"
        }
        require(sizeBytes >= 0L) { "Es gibt keine negative Dateigröße." }
    }
}

/** Wie ein Konflikt zustande kam. */
enum class FileConflict(val label: String, val germanLabel: String) {

    /** Die Datei liegt schon da und würde ersetzt. */
    SAME_NAME_DIFFERENT_CONTENT("Existing file would be replaced", "Datei liegt schon vor und würde ersetzt"),

    /** Der Zielort existiert gar nicht — nichts zu ersetzen. */
    NONE("No conflict", "kein Konflikt")
}

/** Ein benannter Konflikt mit seinem Pfad. */
data class FileConflictEntry(
    val relativePath: String,
    val kind: FileConflict
) {
    /** Die Zeile für die Oberfläche — Pfad **und** Grund. */
    fun displayLine(): String = "$relativePath — ${kind.germanLabel}"
}

// ── Die Vorschau ─────────────────────────────────────────────────────────

/**
 * Was der Nutzer **vor** der Zustimmung sieht.
 *
 * Reiner Wert. [consent] ist hier **immer `null`** und existiert nur, damit ein
 * Aufrufer ausdrücklich nachsehen kann, dass die Vorschau keine Freigabe
 * erzeugt. Ohne das Feld müsste man auf die Abwesenheit einer Methode vertrauen —
 * ein `null`, das man abfragen kann, ist die belastbarere Zusage.
 */
data class InstallationPreview internal constructor(
    val skillName: String,
    val source: String,
    val scope: InstallScope,
    val site: InstallSite,
    val paths: List<EnvironmentFile>,
    val conflicts: List<FileConflictEntry>,
    val consent: SkillConsent? = null
) {

    /**
     * Die Anzeige in fester Reihenfolge.
     *
     * Erst **was**, dann **woher**, dann **wie weit**, dann **wohin**, dann
     **welche Dateien**, dann **was dort schon liegt**. Die Konflikte stehen
     * bewusst **vor** der Fazit-Zeile: Wer „3 Dateien" liest, muss vorher sehen,
     * dass davon 1 ersetzt wird.
     */
    fun displayLines(): List<String> = buildList {
        add("Fähigkeit: $skillName")
        add("Quelle: $source")
        add("Geltungsbereich: ${scope.germanLabel}")
        add("Ziel: ${site.displayLine()}")
        paths.forEach { add("  Datei: ${it.relativePath}") }
        conflicts.forEach { add("  Achtung: ${it.displayLine()}") }
    }

    companion object {

        /**
         * Der einzige Weg zu einer Vorschau.
         *
         * `require` statt einer stillen Leeranzeige: Eine Vorschau ohne Fähigkeit
         * oder ohne Pfade ist keine Vorschau, sondern ein Bildschirm, der nichts
         * erklärt.
         */
        fun of(
            skillName: String,
            source: String,
            scope: InstallScope,
            site: InstallSite,
            paths: List<EnvironmentFile>,
            conflicts: List<FileConflictEntry>
        ): InstallationPreview {
            require(skillName.isNotBlank()) { "Eine Vorschau braucht eine Fähigkeit." }
            require(source.isNotBlank()) { "Eine Vorschau braucht eine Quelle." }
            require(paths.isNotEmpty()) { "Eine Vorschau ohne Dateien zeigt nichts." }
            return InstallationPreview(skillName, source, scope, site, paths, conflicts)
        }
    }
}

// ── Das Ergebnis des Schreibvorgangs ─────────────────────────────────────

/**
 * Was **tatsächlich** geschrieben wurde — geprüft, nicht angenommen.
 *
 * Die drei Felder sind getrennt, weil sie drei verschiedene Fehler bedeuten:
 * eine **fehlende** Datei (nicht geschrieben), eine **unerwartete** Datei (mehr
 * als geplant) und ein **blockierter** Plan (gar nicht erst erlaubt). Ein Plan,
 * der gar nicht schreiben durfte, kann auch kein vollständiges Ergebnis melden —
 * auch dann nicht, wenn jemand behauptet, es sei etwas geschrieben worden.
 */
data class InstallWriteResult(
    val plannedPaths: List<String>,
    val writtenPaths: List<String>,
    val unexpectedPaths: List<String>,
    val planWasWritable: Boolean,
    val backupCreated: Boolean,
    val reasons: List<String>
) {

    /** Wurde **alles** geplant und **nichts** darüber hinaus geschrieben? */
    val isComplete: Boolean
        get() = planWasWritable && missingPaths.isEmpty() && unexpectedPaths.isEmpty()

    /** Geplante Dateien, die fehlen. */
    val missingPaths: List<String>
        get() = plannedPaths.filterNot { it in writtenPaths }

    /** Die Zeilen für die Oberfläche. */
    fun displayLines(): List<String> = buildList {
        add("Geplant: ${plannedPaths.size} Datei(en), geschrieben: ${writtenPaths.size}")
        if (backupCreated) add("Die ersetzten Dateien wurden vorher gesichert.")
        missingPaths.forEach { add("Fehlt: $it") }
        unexpectedPaths.forEach { add("Unerwartet geschrieben: $it") }
        reasons.forEach { add("Hinweis: $it") }
        add(if (isComplete) "Ergebnis: vollständig installiert." else "Ergebnis: NICHT vollständig.")
    }

    /** Die Gründe, warum das Ergebnis nicht vollständig ist. */
    fun refusalReasons(): List<String> = buildList {
        if (!planWasWritable) add("Der Plan durfte nicht schreiben — es wurde nichts verändert.")
        missingPaths.forEach { add("Nicht geschrieben: $it") }
        unexpectedPaths.forEach { add("Nicht geplant, aber geschrieben: $it") }
    }
}

// ── Der Plan ─────────────────────────────────────────────────────────────

/**
 * Der Installationsplan: was geschrieben **würde**, wenn der Nutzer zustimmt.
 *
 * **Es wird nichts geschrieben.** Der Plan ist eine Entscheidungshilfe; die
 * Schreiboperation und ihre Rückmeldung kommen von außen als
 * [InstallWriteResult] herein.
 *
 * @property conflicts Die Dateien, die am Zielort **schon** liegen.
 * @property backupPaths Genau die Dateien aus [conflicts], die gesichert werden
 *   müssten — nicht mehr. Eine Sicherung, die auch Neues sichert, wäre irreführend.
 */
data class GlobalInstallPlan internal constructor(
    val skill: DiscoveredSkill,
    val audit: EnvironmentAudit,
    val scope: InstallScope,
    val site: InstallSite,
    val paths: List<EnvironmentFile>,
    val conflicts: List<FileConflictEntry>,
    val overwriteConfirmed: Boolean,
    val consent: SkillConsent?
) {

    /** Die Vorschau, die der Nutzer sieht — ohne Zustimmung. */
    fun preview(): InstallationPreview =
        InstallationPreview.of(
            skillName = skill.name,
            source = audit.source,
            scope = scope,
            site = site,
            paths = paths,
            conflicts = conflicts
        )

    /** Die Zeilen der Vorschau. */
    fun previewLines(): List<String> = preview().displayLines()

    /**
     * Darf der Plan jetzt schreiben?
     *
     * **Vier** Bedingungen, alle nötig:
     *  1. Es gibt überhaupt Dateien — ein leerer Plan tut nichts und sagt nichts.
     *  2. Die Prüfung ist belastbar ([EnvironmentAudit.isGrounded]).
     *  3. Es gibt keine Konflikte **oder** sie sind ausdrücklich bestätigt.
     *  4. Es liegt eine **wirksame** Zustimmung für genau diesen Skill vor.
     *
     * Bedingung 4 ist der Kern: Sie folgt nicht aus einem Feld, sondern aus
     * [SkillConsent] in der **aktuellen** Zeit. Eine zurückgenommene Freigabe
     * wirkt sofort.
     */
    fun mayWrite(): Boolean = refusalReasons().isEmpty()

    /** Muss vor dem Ersetzen gesichert werden? */
    fun requiresBackup(): Boolean = conflicts.isNotEmpty()

    /** Wie viele Dateien würden gesichert — genau die ersetzten. */
    val backupFileCount: Int get() = backupPaths.size

    /** Die zu sichernden Pfade: die Konflikte, **nur** wenn sie bestätigt sind. */
    val backupPaths: List<String>
        get() = if (overwriteConfirmed) conflicts.map { it.relativePath } else emptyList()

    /** Jede Ablehnung mit ihrem Grund. */
    fun refusalReasons(): List<String> = buildList {
        if (paths.isEmpty()) {
            add("Es gibt keine Datei zum Installieren — dieser Plan würde nichts tun.")
        }
        if (!audit.isGrounded) {
            add("Der Fundort ist nicht belegt — es wurde nichts geprüft, was installiert werden könnte.")
        }
        if (!audit.licenseVerified) {
            add("Die Lizenz ist nicht nachprüfbar belegt.")
        }
        if (conflicts.isNotEmpty() && !overwriteConfirmed) {
            conflicts.forEach { add("Die Datei ${it.relativePath} liegt bereits vor.") }
            add("Kein Überschreiben ohne ausdrückliche Bestätigung.")
        }
        if (!SkillGlobalInstall.consentCovers(consent, skill.name)) {
            add("Es liegt keine wirksame Zustimmung des Nutzers für „${skill.name}“ vor.")
        }
    }

    /**
     * Hängt eine Zustimmung an — **ohne** sie zu erzeugen.
     *
     * Der Plan wird neu gerechnet, nicht verändert: So kann keine frühere
     * Sichtbarkeit (`mayWrite()`) durch eine neue Zustimmung rückwirkend
     * verfälscht werden.
     */
    fun withConsent(
        consentForName: String,
        state: ConsentState,
        grantedAt: Long,
        revokedAt: Long? = null
    ): GlobalInstallPlan {
        val antrag = InstallRequest(consentForName, "Nutzerin", "aus dem Installationsdialog")
        val neu = SkillDiscoveryInventory.recordConsent(antrag, state, grantedAt, revokedAt)
        return copy(consent = neu)
    }
}

// ── Entfernen ────────────────────────────────────────────────────────────

/**
 * Das Entfernen einer installierten Fähigkeit.
 *
 * Der entscheidende Unterschied zu [RemovalPlan] aus Aufgabe 124: hier steht,
 * ob eine **Sicherung** existiert. Ohne sie ist das Entfernen endgültig, und das
 * steht wörtlich in der Anzeige — „wiederherstellbar" wäre sonst eine
 * Behauptung.
 */
data class GlobalRemovalPlan(
    val skillName: String,
    val site: InstallSite,
    val backupExists: Boolean
) {

    /** Die Anzeige, mit dem Wahrheitswort. */
    fun displayLines(): List<String> = buildList {
        add("Entfernen würde „$skillName“ aus ${site.displayLine()} nehmen.")
        add(
            if (backupExists) {
                "Die ersetzten Dateien liegen gesichert vor und sind wiederherstellbar."
            } else {
                "Es liegt keine Sicherung vor — das Entfernen ist endgültig und nicht zurück."
            }
        )
        add("Projektdateien bleiben unberührt.")
    }
}

// ── Die Regeln ───────────────────────────────────────────────────────────

/**
 * Die Regeln der globalen Installation (Aufgabe 131).
 *
 * Reines Kotlin: die App liefert die Fakten, dieses Objekt entscheidet und
 * **schreibt nichts**.
 */
object SkillGlobalInstall {

    /**
     * Braucht eine globale Installation eine ausdrückliche Zustimmung?
     *
     * **Immer.** Konstant `true`, damit keine spätere Änderung sie still
     * abschaltet. Dieselbe Zusage wie in `SkillCompatibility` (124),
     * `SkillDiscoveryInventory` (129) und `McpToolConnectionPolicy` (134) —
     * an vier unabhängigen Stellen abgesichert, damit das Abschalten an einer
     * Stelle nicht unbemerkt bleibt.
     */
    fun installationRequiresUserConsent(): Boolean = true

    /**
     * Gilt eine Zustimmung für **diesen** Skill, und ist sie wirksam?
     *
     * Der Name wird verglichen und der Zustand geprüft — eine Zustimmung für
     * einen anderen Skill oder eine entzogene wirkt nicht. Eine Zeitangabe `0`
     * ergibt nie eine wirksame Zustimmung.
     */
    fun consentCovers(consent: SkillConsent?, skillName: String): Boolean {
        if (consent == null) return false
        if (consent.state != ConsentState.GRANTED) return false
        if (consent.grantedAt <= 0L) return false
        return consent.covers(skillName)
    }

    /**
     * Baut den Installationsplan.
     *
     * @param existing Die Dateien, die am Zielort **bereits** liegen. Sie werden
     *   gegen [paths] abgeglichen — jeder Treffer wird ein [FileConflictEntry].
     * @param overwriteConfirmed Nur `true`, wenn der Nutzer das Überschreiben
     *   ausdrücklich bestätigt hat. Der Vorgabewert ist `false`.
     */
    fun plan(
        skill: DiscoveredSkill,
        audit: EnvironmentAudit,
        paths: List<EnvironmentFile>,
        existing: List<String>,
        overwriteConfirmed: Boolean = false
    ): GlobalInstallPlan {
        require(skill.name.isNotBlank()) { "Ein Plan braucht eine Fähigkeit." }

        // Jeder Pfad wird beim Anlegen auf Ausbrüche geprüft (require in
        // EnvironmentFile). Ein leerer Pfad fällt hier als Ausnahme auf — das ist
        // Absicht: ein Plan mit leerem Pfad ist kein Plan.
        paths.forEach { datei ->
            require(datei.relativePath.isNotBlank()) {
                "Der Pfad einer zu installierenden Datei darf nicht leer sein."
            }
        }

        val vorhanden = existing.filter { it.isNotBlank() }.toSet()
        val konflikte = paths
            .filter { vorhanden.contains(it.relativePath) }
            .map { FileConflictEntry(it.relativePath, FileConflict.SAME_NAME_DIFFERENT_CONTENT) }

        val scope = if (audit.globalPermissions.isNotEmpty()) InstallScope.GLOBAL else InstallScope.PROJECT
        val site = SkillConfirmationDialog.defaultSite(skill.name, skill.searchedWith.taskId, scope)

        return GlobalInstallPlan(
            skill = skill,
            audit = audit,
            scope = scope,
            site = site,
            paths = paths,
            conflicts = konflikte,
            overwriteConfirmed = overwriteConfirmed,
            consent = null
        )
    }

    /**
     * Die **Vorschau** ohne Plan und ohne Zustimmung.
     *
     * Wer nur sehen will, bekommt Informationen — niemals eine Erlaubnis. Der
     * Weg ist bewusst getrennt von [plan], damit „ansehen" nicht versehentlich
     * einen schreibbaren Plan erzeugt.
     */
    fun preview(
        skill: DiscoveredSkill,
        audit: EnvironmentAudit,
        paths: List<EnvironmentFile>,
        existing: List<String>
    ): InstallationPreview = plan(skill, audit, paths, existing).preview()

    /**
     * Nimmt das **Ergebnis** des Schreibvorgangs entgegen und prüft es.
     *
     * Das ist die Nachmessung der vierten Zusage: Der Aufrufer meldet, was
     * wirklich geschrieben wurde, und diese Methode vergleicht es gegen den Plan.
     * Sie schreibt nichts und meldet nichts als Erfolg, was nicht belegt ist.
     *
     * @param backupCreated Was der Aufrufer **beobachtet** hat. Der Vorgabewert
     *   ist `false`, und das ist Absicht: Ein Plan, der eine Sicherung
     *   *vorsieht*, hat sie nicht *angelegt*. Ohne Meldung wird nichts behauptet —
     *   die Oberfläche schweigt lieber, als eine Sicherung zu melden, die es
     *   vielleicht nicht gibt. `plan.requiresBackup()` ist der **Plan**, nicht das
     *   Ergebnis, und darf hier nicht als Beleg dienen.
     */
    @Suppress("LongParameterList")
    fun result(
        plan: GlobalInstallPlan,
        written: List<String>,
        backupCreated: Boolean = false
    ): InstallWriteResult {
        val geplant = plan.paths.map { it.relativePath }
        val geschrieben = written.filter { it.isNotBlank() }
        val unerwartet = geschrieben.filterNot { it in geplant }

        val gruende = plan.refusalReasons()
        return InstallWriteResult(
            plannedPaths = geplant,
            writtenPaths = geschrieben,
            unexpectedPaths = unerwartet,
            planWasWritable = plan.mayWrite(),
            backupCreated = backupCreated,
            reasons = gruende
        )
    }

    /**
     * Der Entfernplan.
     *
     * `backupExists` steuert das Wahrheitswort in der Anzeige: Ohne Sicherung ist
     * das Entfernen endgültig, und „wiederherstellbar" wäre eine Behauptung.
     */
    fun removal(skillName: String, site: InstallSite, backupExists: Boolean): GlobalRemovalPlan {
        require(skillName.isNotBlank()) { "Ein Entfernplan braucht einen Skillnamen." }
        return GlobalRemovalPlan(skillName, site, backupExists)
    }
}
