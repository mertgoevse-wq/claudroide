package org.claudroide.app.feature.skills

/**
 * Task 130 — „Skill-Kompatibilität".
 *
 * Ziel: Erkennen, ob eine gefundene Fähigkeit für ClauDroide/Android und die
 * Aufgabe geeignet ist. Ergebnis: **Format, benötigte Programme, Rechte,
 * Netzverhalten, Aktualität und Lizenz** festhalten.
 *
 * Diese Datei ist die Prüftafel, nicht die Prüfung selbst: sie führt weder
 * Dateizugriff noch Netzverbindung aus. Reines Kotlin, damit die Regeln auf der
 * JVM prüfbar sind.
 *
 * ## Die tragende Trennung: passend ist nicht nutzbar
 *
 * Der häufigste Fehler bei einer Skill-Recherche ist, einen Treffer wegen seines
 * Namens als **direkt nutzbar** einzuplanen. Deshalb trennt dieses Paket zwei
 * Fragen, die oft verwechselt werden:
 *
 *  * [SkillEnvironmentCheck] beantwortet: *Passt der Inhalt zur Umgebung?*
 *    Das ist eine Aussage über Programme, Rechte und Netzverhalten.
 *  * [SkillUsability] beantwortet: *Darf er ohne weitere Prüfung verwendet
 *    werden?* Das ist eine Aussage über ausführbaren Inhalt.
 *
 * Ein Skill kann [CompatibilityVerdict.COMPATIBLE] sein und trotzdem
 * [SkillUsability.NEEDS_INSPECTION] bleiben — dann ist er richtig geplant, aber
 * nicht einsetzbar. Und er kann [CompatibilityVerdict.INCOMPATIBLE] sein und
 * trotzdem **nicht** als Fehlschlag gelten: eine bewusst zurückgestellte
 * Fähigkeit ist eine gültige Planungsentscheidung.
 *
 * ## Die zweite Zusage: Nichts wird blind ausgeführt
 *
 * „Keine fremde Fähigkeit mit Projekt- oder globalen Rechten blind ausführen."
 * Der Schutz sitzt an der Verzweigung, nicht im Kommentar: [SkillEnvironmentCheck]
 * gibt **niemals** [SkillUsability.USABLE] zurück, solange [EnvironmentAudit]
 * nicht belegt ist, dass der Inhalt nichts ausführt, nichts nachlädt und nicht
 * ins Netz greift. Es gibt keine Standardmethode, die das überspringt.
 *
 * Für den **globalen** Teil des Schutzes ist [EnvironmentAudit.globalPermissions]
 * da: [SkillEnvironmentCheck.escapesProjectBoundary] beantwortet getrennt davon,
 * ob ein blindes Ausführen die Projektgrenze verlassen würde. Bewusst getrennt
 * von [SkillEnvironmentCheck.usability], weil ein vollständig geprüfter Skill
 * mit globalem Recht durchaus [SkillUsability.USABLE] sein kann — verhindert
 * wird nur das Ausführen **ohne** diese Prüfung, und dafür ist die Konstante
 * [SkillEnvironmentCheck.requiresIndividualInspection] zuständig.
 */
private const val UNKNOWN_SOURCE = "not recorded"

// ── Format ───────────────────────────────────────────────────────────────

/** Wie ein Skill verpackt ist — bestimmt, ob er überhaupt lesbar ist. */
enum class SkillFormat(val label: String, val germanLabel: String) {

    /** Eine `SKILL.md` mit YAML-Frontmatter. Das erwartete Format. */
    MARKDOWN_FRONTMATTER("Markdown with frontmatter", "Markdown mit Frontmatter"),

    /**
     * Reines Markdown **ohne** Frontmatter.
     *
     * Kein Fehler — aber ohne `name` und `description` findet ein Werkzeug den
     * Skill nicht zuverlässig. Deshalb ist es nicht dasselbe wie
     * [MARKDOWN_FRONTMATTER] und wird auch nicht als solches behandelt.
     */
    MARKDOWN_PLAIN("Plain markdown", "einfaches Markdown"),

    /** Eine Sammlung mehrerer Dateien. */
    DIRECTORY("Directory", "Verzeichnis"),

    /**
     * Ein **Programm**, kein Anweisungstext.
     *
     * Der wichtigste Unterschied: Solche Skills sind per Definition ausführbar
     * und werden deshalb nie als reiner Anweisungstext eingestuft.
     */
    EXECUTABLE("Executable program", "ausführbares Programm"),

    /** Das Format wurde nicht bestimmt — kein Deckel, der Erfolge verdeckt. */
    UNKNOWN("Format not determined", "Format nicht bestimmt")
}

// ── Bedarf ───────────────────────────────────────────────────────────────

/**
 * Ein Programm, das ein Skill **braucht**, um zu laufen.
 *
 * Der entscheidende Punkt: „wird gebraucht" ist nicht dasselbe wie „ist da".
 * Ein fehlendes Programm ist kein Fehlschlag, sondern eine **offene Frage** —
 * die Oberfläche soll sagen, was fehlt, statt den Skill stillschweigend
 * überspringen zu lassen.
 */
data class RequiredProgram(
    val name: String,
    val isAvailable: Boolean,
    val reason: String = UNKNOWN_SOURCE
) {

    /** Nennt dieser Bedarf einen Grund, warum er nicht erfüllt ist? */
    val explainsItself: Boolean get() = isAvailable || reason.isNotBlank()

    val complete: Boolean get() = name.isNotBlank() && explainsItself
}

/** Netzverhalten eines Skills — getrennt, weil es eine eigene Freigabe braucht. */
enum class NetworkBehaviour(val label: String, val germanLabel: String) {

    /** Er greift nicht auf das Netz zu. */
    NONE("No network access", "kein Netzzugriff"),

    /** Er liest oder schreibt Daten im Netz — das verlangt eine eigene Prüfung. */
    REQUIRED("Network access required", "Netzzugriff erforderlich"),

    /** Er erreicht das Netz nur, wenn der Nutzer es anstößt. */
    ON_DEMAND("Network access on demand", "Netzzugriff auf Anforderung"),

    /**
     * Das Verhalten wurde **nicht geprüft**.
     *
     * Ein eigener Zustand, weil „nicht geprüft" nicht dasselbe ist wie „kein
     * Netzzugriff": Die erste Behauptung wäre eine unbelegte Geräteaussage.
     */
    NOT_INSPECTED("Network behaviour not inspected", "Netzverhalten nicht geprüft")
}

// ── Rechte ───────────────────────────────────────────────────────────────

/**
 * Wie weit ein Recht reichen würde, wenn man die Fähigkeit blind ausführte.
 *
 * Der Schutz der Aufgabe nennt genau diese beiden Fälle: „Keine fremde
 * Fähigkeit mit **Projekt- oder globalen** Rechten blind ausführen." Deshalb
 * sind es zwei Zustände und nicht einer — ein projektbezogenes Recht lässt sich
 * durch die vorhandene Projektgrenze begrenzen, ein globales nicht.
 */
enum class PermissionScope(val label: String, val germanLabel: String) {

    /** Nur innerhalb des vom Nutzer gewählten Projektordners. */
    PROJECT("Project-scoped permission", "projektbezogenes Recht"),

    /**
     * Über das Projekt hinaus — auf das ganze Gerät oder auf alle Projekte.
     *
     * Das ist der Fall ohne vorhandene Begrenzung: Für ihn gibt es keine
     * technische Schranke, die ClauDroide selbst setzen könnte.
     */
    GLOBAL("Global permission", "globales Recht")
}

/**
 * Ein Recht, das ein Skill bräuchte, um zu laufen — der zweite Teil des
 * Ergebnisses der Aufgabe neben den benötigten Programmen.
 *
 * Wie [RequiredProgram] ist es eine **Angabe**, kein Prüfergebnis: Es sagt, was
 * gebraucht würde, nicht was erlaubt ist. Wichtig ist deshalb [PermissionScope] —
 * ohne ihn wäre „er braucht ein Recht" nicht unterscheidbar von „er braucht ein
 * Recht, das über das Projekt hinausgeht".
 */
data class RequiredPermission(
    val name: String,
    val scope: PermissionScope
) {

    /** Ohne Namen ist das keine Angabe, sondern ein leeres Formularfeld. */
    val complete: Boolean get() = name.isNotBlank()
}

// ── Prüfung ──────────────────────────────────────────────────────────────

/**
 * Die belegten Fakten einer Prüfung am Dateisystem.
 *
 * Jedes Feld ist ein **Nachweis**, keine Behauptung. Deshalb heißen die
 * booleschen Felder `…Absent` (nachgewiesen abwesend) und nicht `…Safe`
 * (behauptet sicher): Ein Standardwert `false` bei `executesAnythingAbsent` würde
 * einen Skill als unauffällig auszeichnen, ohne dass irgendjemand geprüft
 * hätte. **Der Standard ist hier das Sichere, nicht das Bequeme.**
 */
data class EnvironmentAudit(
    val source: String = UNKNOWN_SOURCE,

    /** Der Name des geprüften Skills — damit ein Grund ihn benennen kann. */
    val name: String = UNKNOWN_SOURCE,
    val licenseVerified: Boolean = false,
    val format: SkillFormat = SkillFormat.UNKNOWN,

    /** Nachgewiesen: Der Inhalt führt keine Befehle aus. */
    val executesNothing: Boolean = false,

    /** Nachgewiesen: Der Inhalt installiert oder lädt nichts nach. */
    val downloadsNothing: Boolean = false,

    val network: NetworkBehaviour = NetworkBehaviour.NOT_INSPECTED,
    val requiredPrograms: List<RequiredProgram> = emptyList(),
    val requiredPermissions: List<RequiredPermission> = emptyList(),
    val lastInspectedAt: Long = 0L
) {

    /**
     * Wurde diese Prüfung überhaupt durchgeführt?
     *
     * **Nein**, wenn kein Fundort feststeht. Ein Audit ohne Ort prüft nichts —
     * es wäre eine Prüfung, die nichts geprüft hat.
     */
    val isGrounded: Boolean
        get() = source.isNotBlank() && source != UNKNOWN_SOURCE && lastInspectedAt > 0L

    /**
     * Wurde der Inhalt wirklich gelesen?
     *
     * `lastInspectedAt > 0` allein genügt nicht zusammen mit einem leeren
     * Befund: Die Zeitangabe beweist, dass **etwas** passiert ist, nicht dass
     * der Inhalt geprüft wurde. Deshalb verlangt [isGrounded] beides — und
     * [isComplete] verlangt zusätzlich, dass die Prüfung abgeschlossen ist.
     *
     * Ein Recht ohne Namen ([RequiredPermission.complete] == `false`) macht die
     * Prüfung genauso unvollständig wie ein Programm ohne Namen: [globalPermissions]
     * und [SkillEnvironmentCheck.boundaryWarning] können es nicht benennen, und
     * ein unbenennbares globales Recht ist genau der Fall, in dem die Warnung
     * „Achtung" sagen würde, ohne etwas zu nennen. Ein leeres Formularfeld ist
     * keine Angabe — deshalb gehört es hierher, nicht in [usability].
     */
    val isComplete: Boolean
        get() = isGrounded &&
            network != NetworkBehaviour.NOT_INSPECTED &&
            requiredPrograms.all { it.complete } &&
            requiredPermissions.all { it.complete }

    /** Fehlende Programme — benannt, nicht verschwiegen. */
    val missingPrograms: List<String>
        get() = requiredPrograms.filterNot { it.isAvailable }.map { it.name }

    /**
     * Rechte, die über das Projekt hinausgehen.
     *
     * Getrennt von [missingPrograms], weil sie anders behandelt werden: Ein
     * fehlendes Programm ist reparierbar, ein globales Recht nicht. Es steht
     * deshalb auch **nicht** in [usability] — siehe
     * [SkillEnvironmentCheck.escapesProjectBoundary].
     */
    val globalPermissions: List<String>
        get() = requiredPermissions
            .filter { it.scope == PermissionScope.GLOBAL }
            .map { it.name }

    /** Trägt der Fundort oder das Netzverhalten einen offenen Punkt nach? */
    val isSuspicious: Boolean
        get() = executesNothing.not() || downloadsNothing.not()
}

/**
 * Das Ergebnis der Kompatibilitätsprüfung eines Skills (Aufgabe 130).
 *
 * Alle Regeln sind **reines Kotlin**: Die App liefert die am Dateisystem
 * belegten Fakten, dieses Objekt entscheidet, was daraus folgt.
 */
enum class SkillUsability(val label: String, val germanLabel: String) {

    /**
     * Geprüft, lizenziert, nichts ausführend — einsetzbar.
     *
     * Erreichbar **nur** über [SkillEnvironmentCheck.usability] mit einer
     * vollständigen [EnvironmentAudit]. Es gibt keinen Weg daran vorbei.
     */
    USABLE("Usable", "einsetzbar"),

    /**
     * Der Inhalt muss **vor** der Verwendung einzeln gelesen werden.
     *
     * Kein Fehler, sondern der Normalzustand direkt nach einer Recherche: der
     * Fundort steht, der Inhalt aber noch nicht.
     */
    NEEDS_INSPECTION("Needs individual inspection", "vor Verwendung einzeln zu prüfen"),

    /**
     * Die Umgebung fehlt — das Programm ist nicht da.
     *
     * Bewusst **nicht** [REJECTED]: Der Skill ist nicht schlecht, die Umgebung
     * ist unvollständig. Das ist ein Unterschied, den die Oberfläche zeigt.
     */
    BLOCKED("Blocked by environment", "durch Umgebung blockiert"),

    /** Abgelehnt: nicht lizenziert oder mit ausführendem Inhalt. */
    REJECTED("Rejected", "abgelehnt")
}

enum class TargetEnvironment(val label: String, val germanLabel: String) {

    /** ClauDroide auf dem Galaxy A56 unter Termux/Debian. */
    CLAUDROIDE_ANDROID("ClauDroide on Android", "ClauDroide auf Android"),

    /** Eine beliebige andere Umgebung — dann gelten andere Anforderungen. */
    OTHER("Other environment", "andere Umgebung")
}

object SkillEnvironmentCheck {

    /**
     * Muss der Inhalt vor der Verwendung einzeln gelesen werden?
     *
     * **Immer.** Die Methode ist die Entsprechung zu
     * [SkillSupplyChainReview.installRequiresUserConsent] aus Aufgabe 123: Die
     * Antwort ist konstant `true`, damit keine spätere Änderung sie still auf
     * `false` setzt, ohne dass ein Test auffällt.
     */
    fun requiresIndividualInspection(): Boolean = true

    /**
     * Würde ein blindes Ausführen die Projektgrenze verlassen?
     *
     * Das ist der zweite Teil des Schutzes der Aufgabe: „Keine fremde Fähigkeit
     * mit **Projekt- oder globalen** Rechten blind ausführen." Ein Recht mit
     * [PermissionScope.PROJECT] ist durch die vorhandene Projektgrenze begrenzt
     * und deshalb kein Grund zur Sorge; ein [PermissionScope.GLOBAL] nicht.
     *
     * Die Antwort ist damit **nicht** dieselbe wie [usability]: Ein Skill mit
     * globalem Recht kann trotz allem [SkillUsability.USABLE] sein — geprüft und
     * lizenziert ist er dann. Verhindert wird hier nur das **blinde** Ausführen,
     * und genau dafür gibt es diese getrennte Frage.
     */
    fun escapesProjectBoundary(audit: EnvironmentAudit): Boolean =
        audit.globalPermissions.isNotEmpty()

    /**
     * Was beim blinden Ausführen über die Projektgrenze hinausreichen würde.
     *
     * Nie leer, wenn [escapesProjectBoundary] `true` ist — dieselbe Regel wie bei
     * [reason]: Die Oberfläche soll nicht sagen „Achtung" und nichts nennen.
     */
    fun boundaryWarning(audit: EnvironmentAudit): String =
        if (escapesProjectBoundary(audit)) {
            "Über die Projektgrenze hinaus: ${audit.globalPermissions.joinToString(", ")}"
        } else {
            ""
        }

    /**
     * Passt das Format zur Umgebung?
     *
     * Nur [SkillFormat.MARKDOWN_FRONTMATTER] ist **direkt** nutzbar. Alles
     * andere — ein Executable, ein Verzeichnis, ein unbekanntes Format — ist
     * kompatibel, aber eben nicht direkt nutzbar. Diese Trennung verhindert
     * genau den Fehler „gefunden heißt einsetzbar".
     */
    fun formatVerdict(format: SkillFormat): CompatibilityVerdict = when (format) {
        SkillFormat.MARKDOWN_FRONTMATTER -> CompatibilityVerdict.COMPATIBLE
        else -> CompatibilityVerdict.NOT_ASSESSED
    }

    /**
     * Wie einsetzbar ist ein Skill nach dieser Prüfung?
     *
     * Die Reihenfolge ist die Aussage — jede Stufe schließt aus:
     *
     *  1. **Kein belegter Fundort oder keine abgeschlossene Prüfung** →
     *     [SkillUsability.NEEDS_INSPECTION]. Eine Prüfung ohne Ort oder ohne
     *     gelesenen Inhalt ist keine Prüfung.
     *  2. **Unbelegte Lizenz** → [SkillUsability.REJECTED]. Ein Verweis auf ein
     *     fehlendes Dokument belegt keine Nutzungsrechte.
     *  3. **Ausführender oder nachladender Inhalt** → [SkillUsability.REJECTED],
     *     unabhängig von der Lizenz. Das ist der Schutz „nicht blind ausführen".
     *  4. **Fehlende Programme** → [SkillUsability.BLOCKED]. Nicht nutzbar, aber
     *     auch nicht abgelehnt: Die Fähigkeit ist gut, die Umgebung fehlt.
     *  5. **Ungeprüftes Netzverhalten** → [SkillUsability.NEEDS_INSPECTION].
     *     Eine Behauptung „greift nicht ins Netz" wäre eine unbelegte Aussage.
     *  6. Sonst [SkillUsability.USABLE].
     */
    fun usability(audit: EnvironmentAudit): SkillUsability = when {
        !audit.isComplete -> SkillUsability.NEEDS_INSPECTION
        !audit.licenseVerified -> SkillUsability.REJECTED
        audit.isSuspicious -> SkillUsability.REJECTED
        audit.missingPrograms.isNotEmpty() -> SkillUsability.BLOCKED
        else -> SkillUsability.USABLE
    }

    /**
     * Soll dieser Skill für **ClauDroide** eingeplant werden?
     *
     * Der wichtigste Unterschied zu [usability]: Ein zurückgestellter oder
     * abgelehnter Skill ist **kein** Planungsfehler. Er wird bewusst nicht
     * eingeplant, und die Aufgabe verlangt genau das — „inkompatible Skills
     * nicht als direkt nutzbar einplanen". Ein NEIN hier ist in Ordnung, ein
     * unbeabsichtigtes JA nicht.
     */
    fun isPlannable(audit: EnvironmentAudit, target: TargetEnvironment): Boolean = when {
        target != TargetEnvironment.CLAUDROIDE_ANDROID -> true
        // Nur was auch *verwendbar* ist, wird eingeplant. Sonst koennte ein
        // abgelehnter Skill (fehlende Lizenz, ausfuehrender Inhalt) als
        // einplanbar durchgehen — genau das, was die Aufgabe verbietet.
        else -> usability(audit) == SkillUsability.USABLE
    }

    /**
     * Die Aktualität einer Prüfung.
     *
     * Eine alte Prüfung ist **keine** Entwarnung für einen geänderten Inhalt.
     * [EnvironmentAudit.lastInspectedAt] wird deshalb gegen [now] geprüft, und
     * [now <= 0] bedeutet „keine Zeitangabe" — nicht „unendlich alt".
     */
    fun isStale(audit: EnvironmentAudit, now: Long, maxAgeMillis: Long): Boolean {
        if (now <= 0L || maxAgeMillis <= 0L) return false
        if (audit.lastInspectedAt <= 0L) return true
        return now - audit.lastInspectedAt > maxAgeMillis
    }

    /**
     * Der Grund für ein Urteil — nie leer.
     *
     * Die Oberfläche zeigt keinen unveränderten Bildschirm, wenn ein Skill
     * abgelehnt oder zurückgestellt wurde.
     */
    fun reason(audit: EnvironmentAudit, target: TargetEnvironment): String = when (usability(audit)) {
        SkillUsability.NEEDS_INSPECTION ->
            if (audit.source.isBlank() || audit.source == UNKNOWN_SOURCE) {
                "\"${audit.name}\": Es ist kein Fundort festgestellt — ohne Ort wurde nichts geprüft."
            } else if (audit.network == NetworkBehaviour.NOT_INSPECTED) {
                "\"${audit.name}\": Das Netzverhalten wurde nicht geprüft."
            } else {
                "\"${audit.name}\": Die Prüfung ist unvollständig."
            }

        SkillUsability.REJECTED ->
            when {
                !audit.licenseVerified ->
                    "\"${audit.name}\" ist abgelehnt: Die Lizenz ist nicht belegt."

                audit.executesNothing.not() ->
                    "\"${audit.name}\" ist abgelehnt: Der Inhalt führt Befehle aus."

                else ->
                    "\"${audit.name}\" ist abgelehnt: Der Inhalt installiert oder lädt etwas nach."
            }

        SkillUsability.BLOCKED ->
            "\"${audit.name}\" ist blockiert: Es fehlt ${audit.missingPrograms.joinToString(", ")}."

        SkillUsability.USABLE ->
            "\"${audit.name}\" ist geprüft und einsetzbar."
    }
}