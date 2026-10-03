package org.claudroide.app.feature.git

import org.claudroide.app.feature.project.PathBoundaryGuard

/**
 * Task 096 — „Repository laden".
 *
 * Ziel: Ein Projekt von GitHub oder einem kompatiblen Git-Server **auf das
 * Gerät** holen. Ergebnis: Adresse, Zielordner, Zugang, Speicherbedarf,
 * Download-Fortschritt und Fehlerbehandlung.
 *
 * Diese Datei lädt nichts. Sie entscheidet, **ob** geladen werden darf, zeigt
 * **was** geladen würde, und prüft am Ende, **was tatsächlich** angekommen ist.
 * Der Ladevorgang selbst ist derselbe Aufbau wie in [GitCredentialVault] aus
 * Aufgabe 095: ein Ergebnis kommt als **Wert** zurück, eine Dateisystem- oder
 * Netzoperation findet außerhalb dieser Datei statt. Dadurch bleibt jede Aussage
 * prüfbar — ein Plan kann sich nicht selbst verschreiben, und ein abgebrochener
 * Ladevorgang kann sich nicht als Erfolg ausgeben.
 *
 * ## Die zwei Fertig-Bedingungen als Eigenschaft
 *
 *  1. *„Nutzer vor Download Ziel und Projektquelle sieht."*
 *     [ClonePlan.displayLines] nennt Quelle **und** Ziel **und** Größenangabe
 *     in fester Reihenfolge. [CloneGate.mayStart] gibt den Start aber nur frei,
 *     wenn [ClonePlan.userAcknowledged] gesetzt ist — das ist eine eigene Eingabe
 *     des Nutzers und wird aus nichts abgeleitet. Ein Plan, den niemand gesehen
 *     hat, kann nicht starten.
 *
 *  2. *„Abgebrochener Download keine scheinbar vollständige Arbeitskopie
 *     hinterlässt."*
 *     Das ist die tragende Zusicherung dieser Datei. [CloneWriteResult] führt
 *     **keine** Eigenschaft `isComplete` im Sinn von „die Arbeitskopie
 *     benutzt sich wie eine echte". Es gibt nur [CloneWriteResult.state] aus
 *     [CloneState], und ein abgebrochener Ladevorgang ist
 *     [CloneState.ABORTED] — nicht „fast fertig", nicht „läuft noch". Ein
 *     Test prüft genau das: dass es **keine** boolesche Eigenschaft gibt,
 *     die einen Abbruch unterwegs als nutzbar ausgeben könnte.
 *
 * ## Der Schutz: Repository-Inhalte sind nicht vertrauenswürdig
 *
 * Ein geklontes Repository ist Fremdinhalt. Daraus folgt dreierlei, und alle drei
 * sitzen im Typ statt in einem Kommentar:
 *
 *  * **Nichts aus dem Repository gilt als Anweisung.** [UntrustedContent]
 *    liefert über [UntrustedContent.asProjectInstruction] **immer** `null`:
 *    Es gibt keinen Aufruf, der eine Datei aus einem geklonten Repository in
 *    eine Projektanweisung verwandelt. Das ist die Grenze aus Aufgabe 093
 *    (InstructionTrustPolicy) und Aufgabe 094 (Provenance), hier an der
 *    Ladestelle angewandt.
 *  * **Ausführbarer Inhalt wird benannt, nicht stillschweigend zugelassen.**
 *    [RiskyContentKind] führt die Klassen von Dateien auf, die beim Klonen
 *    gesondert behandelt werden. [ClonePlan.riskyFindings] sammelt sie, und
 *    [CloneGate.mayStart] verweigert den Start **nicht** deshalb — ein Skript im
 *    Repository ist normal —, nennt sie aber vor dem Download, damit niemand
 *    sie erst hinterher entdeckt.
 *  * **Aus dem Repository kommen keine Rechte.** [CloneWriteResult] führt keine
 *    Zugangsdaten und keine Berechtigungen. [SubagentPermissionMatrix] aus
 *    Aufgabe 133 bleibt davon unberührt: Dateien aus einem Fremd-Repository
 *    erhalten keine Werkzeuge, weil sie gelesen wurden.
 *
 * ## Speicherbedarf, ehrlich gerechnet
 *
 * [ClonePlan.requiredBytes] ist aus den **tatsächlich** gemeldeten
 * Einzelgrößen berechnet, nicht aus einer Schätzung. Wenn das Repository
 * seine Größe nicht meldet, ist [CloneSizeReport.sizeKnown] `false` und
 * [CloneGate.mayStart] verweigert den Start mit dem Hinweis, dass die
 * benötigte Speichergröße unbekannt ist. Eine erfundene Zahl wäre schlimmer
 * als keine: Sie füllt den freien Speicher scheinbar auf und der Abbruch
 * kommt überraschend.
 *
 * Reines Kotlin: kein Netz, kein Dateisystem, kein Android.
 */

// ── Die Adresse ───────────────────────────────────────────────────────────

/**
 * Woher geladen wird.
 *
 * Der [host] wird gegen eine feste Liste **erlaubter** Hosts geprüft, statt
 * gegen eine Liste verbotener: Eine Negativliste ist immer unvollständig, und
 * ein Klon ist die eine Operation, bei der ein falscher Host bedeutet, dass der
 * eigene Schlüssel im Klartext an einen fremden Server gehen. Ein Adress-Schema
 * ohne Host wird abgewiesen — es gibt keine Adresse, die „irgendwo" bedeutet.
 */
data class CloneSource(
    val scheme: String,
    val host: String,
    val owner: String,
    val repositoryName: String,
    val requirePrivate: Boolean = true
) {
    init {
        require(scheme in ALLOWED_SCHEMES) {
            "Nur ${ALLOWED_SCHEMES.joinToString(" und ")} sind erlaubt, nicht $scheme."
        }
        require(host.isNotBlank()) { "Eine Adresse braucht einen Host." }
        require(host.lowercase() in ALLOWED_HOSTS) {
            "Der Host $host ist nicht erlaubt. Erlaubt sind ${ALLOWED_HOSTS.joinToString(", ")}."
        }
        require(owner.isNotBlank()) { "Eine Adresse braucht einen Besitzer oder eine Organisation." }
        require(repositoryName.isNotBlank()) { "Eine Adresse braucht einen Repositorynamen." }
        require(!repositoryName.contains("..")) {
            "Ein Repositoryname darf nicht aus seinem Ordner ausbrechen."
        }
    }

    /** Die kanonische Form, wie sie im Plan angezeigt wird. */
    fun canonical(): String = "$scheme://$host/$owner/$repositoryName"

    /** Die Zeile für die Oberfläche. */
    fun displayLine(): String =
        "${canonical()} (${if (requirePrivate) "nur privat" else "öffentlich erlaubt"})"

    companion object {
        /**
         * Nur HTTPS.
         *
         * Kein `ssh://` und kein `git://`: Beide umgehen die Prüfung, ob der
         * Server der ist, der er behauptet zu sein, und `ssh://` bringt für
         * die Nutzerfreigabe in Aufgabe 095 einen zweiten geheimen Schlüssel
         * auf das Gerät, der dort nicht abgelegt würde.
         */
        val ALLOWED_SCHEMES = listOf("https")

        /** Die erlaubten Hosts. Positivliste — siehe [ALLOWED_SCHEMES]. */
        val ALLOWED_HOSTS = listOf("github.com")
    }
}

// ── Der Speicherbedarf ────────────────────────────────────────────────────

/**
 * Die Speicherangabe des Servers.
 *
 * Getrennt von [ClonePlan], weil „gemeldet" und „bekannt" verschiedene Fragen
 * sind und der Unterschied entscheidend ist: [sizeKnown] `false` heißt, dass
 * die App die Zahl nicht kennt — und dann darf sie auch keine erfinden.
 */
data class CloneSizeReport(
    val reportedBytes: Long?,
    val freeBytes: Long?,
    val filesCount: Long? = null
) {
    init {
        require(reportedBytes == null || reportedBytes >= 0L) { "Es gibt keine negative Größe." }
        require(freeBytes == null || freeBytes >= 0L) { "Es gibt keinen negativen Speicher." }
        require(filesCount == null || filesCount >= 0L) { "Es gibt keine negative Dateizahl." }
    }

    /** Weiß die App, wie groß der Download wird? */
    val sizeKnown: Boolean get() = reportedBytes != null

    /** Passt es auf das Gerät? Nur beantwortbar, wenn beides bekannt ist. */
    val fitsFreeSpace: Boolean?
        get() = if (reportedBytes != null && freeBytes != null) {
            reportedBytes <= freeBytes
        } else {
            null
        }

    /** Die Zeile für die Oberfläche — „unbekannt" ist ein Wert, kein Leerfeld. */
    fun displayLine(): String = when {
        reportedBytes != null && freeBytes != null -> {
            val passt = if (fitsFreeSpace == true) "passt auf" else "passt NICHT auf"
            "Bedarf: ${reportedBytes} Bytes; frei: ${freeBytes} Bytes — $passt den freien Speicher"
        }
        reportedBytes != null -> "Bedarf: ${reportedBytes} Bytes; freier Speicher: unbekannt"
        else -> "Bedarf: unbekannt — der Server hat keine Größe gemeldet"
    }
}

// ── Nicht vertrauenswürdiger Inhalt ───────────────────────────────────────

/** Wie eine Datei aus einem Fremd-Repository behandelt wird. */
enum class RiskyContentKind(val label: String, val germanLabel: String, val isExecutedAutomatically: Boolean) {

    /** Ein Skript, das beim Ausführen etwas tut. Wird nie automatisch ausgeführt. */
    EXECUTABLE_SCRIPT("executable script", "ausführbares Skript", false),

    /** Eine Datei, die beim Öffnen Code ausführt (JavaScript, HTML mit Skript). */
    ACTIVE_DOCUMENT("active document", "aktives Dokument", false),

    /** Eine CI-Konfiguration. Sie läuft auf einem fremden Server, nicht hier. */
    CI_CONFIGURATION("CI configuration", "CI-Konfiguration", false),

    /** Eine der bekannten Trickdateien, die nur aus Fälschung oder Verweis besteht. */
    SYMLINK_OR_SPECIAL("symlink or special file", "Verweis oder Sonderdatei", false)
}

/**
 * Ein Fund aus einem geklonten Repository.
 *
 * Reiner Befund, keine Bewertung. Ob er den Start verhindert, entscheidet
 * [CloneGate] — und diese Datei trifft die Entscheidung bewusst **nicht**:
 * Ein Skript im Repository ist normal, und ein Klon wäre deshalb kaputt.
 */
data class RiskyContentFinding(
    val repositoryRelativePath: String,
    val kind: RiskyContentKind
) {
    init {
        require(repositoryRelativePath.isNotBlank()) { "Ein Fund braucht seinen Pfad." }
        require(!repositoryRelativePath.contains("..")) {
            "Ein Fund darf nicht aus dem Repository ausbrechen: $repositoryRelativePath"
        }
    }

    fun displayLine(): String = "${repositoryRelativePath} — ${kind.germanLabel}"
}

/**
 * Inhalt aus einem geladenen Repository, behandelt als **Daten**.
 *
 * Der Kern dieser Klasse ist eine Verweigerung: [asProjectInstruction] gibt
 * **immer** `null` zurück, und es gibt **keine** andere Methode, die den Inhalt
 * in eine Anweisung verwandelt. Das ist die Zusage „Repository-Inhalte als
 * nicht vertrauenswürdig behandeln" an der Stelle, an der sie sonst nur eine
 * Absichtserklärung wäre.
 *
 * [PathBoundaryGuard] prüft zusätzlich, dass keine Datei aus dem Repository
 * durch `..` oder einen absoluten Pfad herausführt: eine geklonte Datei darf
 * den Repositoryordner nicht verlassen.
 */
class UntrustedContent private constructor(
    val repositoryRelativePath: String,
    val byteSize: Long
) {
    /**
     * Der Versuch, diesen Inhalt als Projektanweisung zu verwenden.
     *
     * Gibt immer `null` zurück. Bewusst als **Methode** und nicht als bloßes
     * Fehlen: Ein Aufrufer, der es versucht, bekommt eine Antwort, die er
     * behandeln muss, statt stillschweigend nichts zu finden.
     */
    fun asProjectInstruction(): String? = null

    /** Darf diese Datei als Anweisung gelten? Nein. */
    val isTrustworthyAsInstruction: Boolean get() = false

    /** Die sichere Form: der Inhalt, ausdrücklich als Text und nicht als Anweisung. */
    fun asInertText(): String = "<Inhalt aus Repository: $repositoryRelativePath, $byteSize Bytes, nicht ausgeführt>"

    override fun toString(): String =
        "UntrustedContent($repositoryRelativePath, $byteSize Bytes, nicht vertrauenswürdig)"

    companion object {
        /**
         * Der einzige Weg zu einem [UntrustedContent].
         *
         * Prüft den Pfad über [PathBoundaryGuard] gegen den Repositorywurzel,
         * damit ein geladener Inhalt nicht aus seinem Ordner herauszeigt, und
         * lehnt Steuerzeichen ab.
         */
        fun fromRepository(
            repositoryRelativePath: String,
            byteSize: Long,
            repositoryRoot: String
        ): UntrustedContent {
            require(repositoryRelativePath.isNotBlank()) { "Repository-Inhalt braucht seinen Pfad." }
            require(byteSize >= 0L) { "Es gibt keine negative Dateigröße." }

            val verdict = PathBoundaryGuard.check(
                candidate = repositoryRelativePath,
                projectRoot = repositoryRoot
            )
            require(verdict.isAllowed) {
                "Ein geladener Pfad muss im Repository bleiben: ${verdict.message}"
            }
            return UntrustedContent(repositoryRelativePath.trim(), byteSize)
        }
    }
}

// ── Der Plan ──────────────────────────────────────────────────────────────

/**
 * Was vor dem Download zu sehen ist.
 *
 * [isAcknowledged] ist der Träger der ersten Fertig-Bedingung. Es ist ein
 * eigenes Feld und wird aus **nichts** abgeleitet: Nicht aus der Größe, nicht
 * aus dem freien Speicher, nicht daraus, dass die Adresse erlaubt ist. Wer
 * starten will, muss bestätigt haben, dass er Quelle und Ziel gesehen hat.
 */
data class ClonePlan(
    val source: CloneSource,
    val targetDirectory: String,
    val sizeReport: CloneSizeReport,
    val riskyFindings: List<RiskyContentFinding> = emptyList(),
    val userAcknowledged: Boolean = false
) {
    init {
        require(targetDirectory.isNotBlank()) { "Ein Plan braucht sein Ziel." }
        require(!targetDirectory.any { it.isISOControl() && it != '\t' }) {
            "Ein Zielordner kann kein Steuerzeichen enthalten."
        }
    }

    /**
     * Der benötigte Speicher, **falls** bekannt.
     *
     * `null` heißt „weiß ich nicht" und wird nicht durch eine Schätzung ersetzt.
     */
    val requiredBytes: Long? get() = sizeReport.reportedBytes

    /** Die Größenangabe: bekannt oder ausdrücklich offen. */
    val isSizeKnown: Boolean get() = sizeReport.sizeKnown

    /**
     * Hat der Nutzer Quelle und Ziel gesehen und dem Laden zugestimmt?
     *
     * Eine eigene Eingabe, aus **nichts** abgeleitet: nicht aus der Größe, nicht
     * aus dem freien Speicher, nicht daraus, dass die Adresse erlaubt ist.
     *
     * [CloneGate.mayStart] prüft dieses Feld **zuerst**, und es ist der einzige
     * Ort, an dem es gelesen wird. Der Vorgabewert von [userAcknowledged] ist
     * `false`, damit ein unbestätigter Plan blockiert.
     */
    val acknowledgedByUser: Boolean get() = userAcknowledged

    /**
     * Die Zeilen für die Oberfläche, in fester Reihenfolge: Quelle, Ziel, Größe, Risiken.
     *
     * Reihenfolge mit Absicht: Die Quelle steht zuerst, damit der erste Satz
     * der Anzeige beantwortet, **woher** das kommt — und nicht, was auf dem
     * Gerät passiert. Eine Anzeige, die mit dem Ziel beginnt, lenkt den Blick
     * auf das eigene Gerät und damit auf das geringere Risiko.
     */
    fun displayLines(): List<String> = buildList {
        add("Quelle: ${source.displayLine()}")
        add("Zielordner auf dem Gerät: $targetDirectory")
        add(sizeReport.displayLine())
        if (riskyFindings.isNotEmpty()) {
            add("Im Repository gefunden (wird nicht automatisch ausgeführt):")
            riskyFindings.forEach { add("  ${it.displayLine()}") }
        }
        if (!isSizeKnown) {
            add(
                "Hinweis: Die benötigte Speichergröße ist unbekannt; " +
                    "ein Abbruch während des Ladens wäre nicht vorhersehbar."
            )
        }
    }
}

// ── Das Ergebnis ──────────────────────────────────────────────────────────

/** Wie ein Ladevorgang ausgegangen ist. */
enum class CloneState(val label: String, val germanLabel: String) {

    /** Alles angekommen und nachgemessen. */
    COMPLETE("complete", "vollständig"),

    /** Vom Nutzer abgebrochen. */
    ABORTED("aborted", "abgebrochen"),

    /** Die Verbindung ist abgebrochen. */
    CONNECTION_LOST("connection lost", "Verbindung abgebrochen"),

    /** Der Server hat geantwortet, aber nicht mit Inhalt. */
    REJECTED_BY_SERVER("rejected by server", "vom Server abgelehnt"),

    /** Auf dem Gerät ist nicht genug Platz. */
    OUT_OF_SPACE("out of space", "kein Speicherplatz")
}

/**
 * Was **tatsächlich** auf dem Gerät liegt.
 *
 * Die zweite Fertig-Bedingung: *„Abgebrochener Download keine scheinbar
 * vollständige Arbeitskopie hinterlässt."*
 *
 * [state] ist der ganze Inhalt der Aussage. Es gibt hier bewusst **keine**
 * Methode wie `isUsable()`, `hasFiles()` oder `looksComplete()`, und keinen
 * Getter, der aus einer nicht leeren Ordnerstruktur „vollständig" ableitet.
 * Ein Aufrufer, der eine Arbeitskopie verwenden will, muss [state] prüfen —
 * und [CloneState.ABORTED] ist kein solcher Zustand.
 *
 * [abortedRun] trägt die Teilmenge, die tatsächlich geschrieben wurde. Sie ist
 * für das Aufräumen da, damit der Rest nicht als Arbeitskopie missverstanden
 * wird; sie wird nie als „fast fertig" ausgewiesen.
 */
data class CloneWriteResult(
    val state: CloneState,
    val writtenRelativePaths: List<String>,
    val bytesReceived: Long,
    val failureMessage: String? = null
) {
    init {
        require(bytesReceived >= 0L) { "Es gibt keine negative Bytezahl." }
        require(writtenRelativePaths.none { it.contains("..") }) {
            "Ein geschriebener Pfad darf nicht aus dem Repository ausbrechen."
        }
        // Ein abgebrochener Lauf, der behauptet, nichts geschrieben zu haben,
        // ist möglich (Abbruch vor dem ersten Byte). Ein vollstaendiger Lauf
        // mit geschriebenen Dateien ist genauso moeglich. Beides wird nicht
        // erzwungen - wohl aber die Unmoeglichkeit, state und Pfade
        // unabhaengig voneinander zu behaupten: ein vollstaendiger Lauf mit
        // keiner Datei wird hier abgewiesen, weil dann "vollstaendig" nichts
        // bedeutet und die Arbeitskopie leer waere.
        require(!(state == CloneState.COMPLETE && writtenRelativePaths.isEmpty())) {
            "Ein vollstaendiger Ladevorgang ohne eine einzige Datei ist kein vollstaendiger Ladevorgang."
        }
    }

    /** Wurde etwas geschrieben? */
    val hasPartialContent: Boolean get() = writtenRelativePaths.isNotEmpty()

    /**
     * Darf dieser Ordner als Arbeitskopie benutzt werden?
     *
     * Die einzige Antwort, die diese Klasse auf „vollständig?" gibt, und sie
     * verlangt **den gemeldeten Zustand**, nicht das Vorhandensein von Dateien.
     */
    val isUsableWorkingCopy: Boolean get() = state == CloneState.COMPLETE

    /** Die Zeile für die Oberfläche. */
    fun displayLine(): String {
        val zusatz = if (hasPartialContent) {
            " (${writtenRelativePaths.size} Dateien teilweise geschrieben — kein gültiger Stand)"
        } else {
            ""
        }
        return "Ergebnis: ${state.germanLabel}$zusatz"
    }
}

// ── Das Tor ───────────────────────────────────────────────────────────────

/** Das Ergebnis einer Ladeprüfung. */
sealed interface CloneDecision {

    /** Der Ladevorgang darf beginnen. */
    data class MayStart(val planLines: List<String>) : CloneDecision

    /** Der Nutzer hat Quelle und Ziel nicht bestätigt. */
    data class NotAcknowledged(val reason: String) : CloneDecision

    /** Der benötigte Speicher ist unbekannt. */
    data class SizeUnknown(val reason: String) : CloneDecision

    /** Der Platz reicht nicht. */
    data class OutOfSpace(val reason: String) : CloneDecision

    /** Der Nutzer hat abgebrochen. */
    data class Aborted(val writtenCount: Int, val reason: String) : CloneDecision
}

/**
 * Die Regel, wann ein Ladevorgang beginnen darf und wann eine Arbeitskopie
 * benutzt werden darf.
 *
 * Zwei Fragen, zwei Methoden, und sie sind nicht vermischt:
 *
 *  * [mayStart] beantwortet „darf geladen werden?" und prüft in fester
 *    Reihenfolge: Bestätigung, dann bekannte Größe, dann freier Speicher.
 *  * [mayUseWorkingCopy] beantwortet „darf der Ordner benutzt werden?" und
 *    liest **ausschließlich** [CloneWriteResult.state]. Es gibt hier keinen Weg,
 *    aus einem Ordner mit Dateien auf „vollständig" zu schließen.
 */
object CloneGate {

    /** Darf der Ladevorgang nach [plan] beginnen? */
    fun mayStart(plan: ClonePlan): CloneDecision = when {
        !plan.acknowledgedByUser -> CloneDecision.NotAcknowledged(
            "Der Nutzer hat Quelle und Ziel nicht bestätigt. Es wird nichts geladen."
        )

        !plan.isSizeKnown -> CloneDecision.SizeUnknown(
            "Die benötigte Speichergröße ist unbekannt; ein Abbruch während des Ladens " +
                "wäre nicht vorhersehbar. Es wird nichts geladen."
        )

        plan.sizeReport.fitsFreeSpace == false -> CloneDecision.OutOfSpace(
            "Der freie Speicher reicht für ${plan.requiredBytes} Bytes nicht. Es wird nichts geladen."
        )

        else -> CloneDecision.MayStart(plan.displayLines())
    }

    /** Wurde abgebrochen, und was ist dabei entstanden? */
    fun onResult(result: CloneWriteResult): CloneDecision = when (result.state) {
        CloneState.COMPLETE -> CloneDecision.MayStart(
            buildList {
                add(result.displayLine())
                add("Der Ordner darf als Arbeitskopie benutzt werden.")
            }
        )

        CloneState.ABORTED -> CloneDecision.Aborted(
            result.writtenRelativePaths.size,
            "Der Ladevorgang wurde abgebrochen. ${result.writtenRelativePaths.size} Dateien " +
                "liegen vor; der Ordner ist KEINE gültige Arbeitskopie."
        )

        CloneState.CONNECTION_LOST -> CloneDecision.Aborted(
            result.writtenRelativePaths.size,
            "Die Verbindung ist abgebrochen. ${result.writtenRelativePaths.size} Dateien " +
                "liegen vor; der Ordner ist KEINE gültige Arbeitskopie."
        )

        CloneState.OUT_OF_SPACE -> CloneDecision.Aborted(
            result.writtenRelativePaths.size,
            "Der Platz reichte nicht. ${result.writtenRelativePaths.size} Dateien liegen vor; " +
                "der Ordner ist KEINE gültige Arbeitskopie."
        )

        CloneState.REJECTED_BY_SERVER -> CloneDecision.Aborted(
            result.writtenRelativePaths.size,
            "Der Server hat den Ladevorgang abgelehnt. ${result.writtenRelativePaths.size} " +
                "Dateien liegen vor; der Ordner ist KEINE gültige Arbeitskopie."
        )
    }

    /**
     * Darf [result] als Arbeitskopie benutzt werden?
     *
     * Die Frage wird allein aus [CloneWriteResult.state] beantwortet. Dateien
     * vorhanden heißt **nicht** benutzbar — das ist der ganze Punkt der zweiten
     * Fertig-Bedingung, und deshalb steht hier kein `!writtenRelativePaths.isEmpty()`,
     * das die Antwort ins Positive kippen würde.
     */
    fun mayUseWorkingCopy(result: CloneWriteResult): Boolean = result.isUsableWorkingCopy

    /**
     * Der Rest, der nach einem Abbruch **weg** muss.
     *
     * [CloneWriteResult.writtenRelativePaths], aber als eine einzige Aussage:
     * „das hier zu löschen". Kein `deleteAll()` in dieser Datei — das Löschen
     * ist eine Nebenwirkung und wird, wie das Schreiben, außerhalb ausgeführt.
     */
    fun cleanupAfterAbort(result: CloneWriteResult): List<String> =
        if (result.isUsableWorkingCopy) {
            emptyList()
        } else {
            result.writtenRelativePaths
        }
}
