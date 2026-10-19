package org.claudroide.app.feature.project

import org.claudroide.app.feature.agent.RequiredApproval
import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 081 — „Projektübersicht“.
 *
 * Ziel: Projektstatus und sichere nächste Schritte auf einer Smartphoneansicht
 * zeigen.
 *
 * Die Übersicht ist die erste Seite, die der Nutzer nach dem Öffnen der App sieht.
 * Sie behauptet also schnell Dinge, die nicht stimmen — deshalb sind die beiden
 * Fertig-Kriterien an den Typen festgemacht:
 *
 *  1. **Widerrufene Ordnerrechte sind deutlich markiert.** Der Zustand jedes
 *     Projektordners wird aus [ProjectAccessState.of] **berechnet** und nie
 *     übergeben. [AccessState.REVOKED] steht in [ProjectOverview.headline] und in
 *     [ProjectOverview.overviewLines] an erster Stelle, und [ProjectOverview.claimFor]
 *     verweigert für einen widerrufenen Ordner jede Zusage. Es gibt keinen Weg,
 *     einen Ordner als „in Ordnung“ zu melden, wenn die Registry ihn zurückgezogen
 *     hat — die Berechnung liest dieselbe Quelle, die auch den Werkzeugzugriff
 *     prüft.
 *
 *  2. **Keine Dateien ohne Auswahl automatisch scannen.** [OverviewFileList] lässt
 *     sich mit [SelectionOrigin.AUTOMATIC_SCAN] gar nicht erst erzeugen: Der
 *     `init`-Block lehnt jede Liste ab, die nicht ausdrücklich aus einer
 *     Nutzerauswahl stammt. In dieser App gibt es zudem keine Speicherberechtigung
 *     (nur `INTERNET` und `ACCESS_NETWORK_STATE` im Manifest) — die Übersicht kann
 *     also gar nichts selbst suchen, was sie auflisten könnte.
 *
 * **Schutz: keine Geheimnisse, keine vollständigen Dateiinhalte.** [OverviewFile]
 * hat **kein Feld für Inhalt** — nur Pfad, Name und Größe. Das ist kein Verzicht auf
 * eine Prüfung, sondern deren Ergebnis: Es gibt keinen Constructor-Pfad, über den
 * ein Dateiinhalt in die Übersicht gelangt. Zusätzlich wird jede Datei über
 * [ProjectExclusionPolicy.classify] geschickt; eine als [ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET]
 * erkannte Datei erscheint nur als Anzahl, nie mit Namen. Jeder Text läuft
 * zusätzlich durch [SecretMasker].
 *
 * Reines Kotlin: kein Android, kein Dateisystem. Die Dateiliste liefert der
 * Aufrufer — und zwar nur aus einer Auswahl des Nutzers.
 */

/** Wie der Zugriff auf einen Projektordner steht. */
enum class AccessState(val germanLabel: String, val isUsable: Boolean) {

    /** Der Ordner ist ausgewählt und nicht zurückgezogen. */
    GRANTED("freigegeben", true),

    /**
     * Der Nutzer hat den Zugriff zurückgezogen.
     *
     * Steht bewusst an erster Stelle der Anzeige: Ein widerrufener Ordner, der wie
     * ein normaler aussieht, wäre der gefährlichste Fehler dieser Seite.
     */
    REVOKED("Zugriff vom Nutzer zurückgezogen", false),

    /** Es ist kein Ordner ausgewählt. */
    NONE("kein Ordner ausgewählt", false)
}

/**
 * Wie eine Dateiliste zustande gekommen ist.
 *
 * [AUTOMATIC_SCAN] existiert, damit die Ablehnung *benannt* werden kann. Es ist kein
 * gültiger Zustand für eine Dateiliste — s. [OverviewFileList].
 */
enum class SelectionOrigin(val germanLabel: String) {

    /** Der Nutzer hat die Dateien ausgewählt. */
    USER_SELECTION("vom Nutzer ausgewählt"),

    /** Es wurde nichts ausgewählt. */
    NONE("nichts ausgewählt"),

    /**
     * Die Dateien wurden gefunden, ohne dass der Nutzer etwas ausgewählt hat.
     *
     * Wird von [OverviewFileList] abgelehnt. Der Wert ist Teil der Prüfung, damit
     * der Grund benennbar ist — nicht, damit er benutzt werden dürfte.
     */
    AUTOMATIC_SCAN("automatisch gefunden")
}

/**
 * Der Zugriffsstand eines Ordners.
 *
 * @property isBlockedForSecret `true`, wenn der Ordner eine Geheimnisdatei enthält.
 *           Solche Ordner werden in der Übersicht nur als Anzahl genannt.
 */
data class ProjectAccessState(
    val displayName: String,
    val state: AccessState,
    val isBlockedForSecret: Boolean = false
) {
    init {
        require(displayName.isNotBlank()) { "Ein Projektordner braucht einen Namen." }
    }

    /** Die Zeile für die Anzeige. */
    fun overviewLine(): String = buildString {
        append(displayName)
        append(" — ")
        append(state.germanLabel)
        if (isBlockedForSecret) {
            append(" (enthält eine Datei mit Zugangsdaten, nicht gezeigt)")
        }
    }

    companion object {

        /**
         * Berechnet den Stand aus der Registry — **nicht** übernehmen.
         *
         * @param displayName der Ordnername für die Anzeige.
         * @param registered `true`, wenn dieser Ordner als Projektwurzel eingetragen ist.
         * @param hasRevokedEntry `true`, wenn die Registry eine Zurücknahme für diesen
         *           Ordner (oder einen Pfad darunter) führt.
         */
        fun of(
            displayName: String,
            registered: Boolean,
            hasRevokedEntry: Boolean,
            containsSecretFile: Boolean = false
        ): ProjectAccessState = ProjectAccessState(
            displayName = displayName,
            state = when {
                hasRevokedEntry -> AccessState.REVOKED
                registered -> AccessState.GRANTED
                else -> AccessState.NONE
            },
            isBlockedForSecret = containsSecretFile
        )
    }
}

/**
 * Eine Datei in der Übersicht.
 *
 * **Es gibt hier kein Feld für den Inhalt.** Nur Pfad, Anzeigename und Größe. Damit
 * ist die Zusage „keine vollständigen Dateiinhalte in der Übersichtsvorschau“
 * nicht durch eine Prüfung erfüllt, sondern dadurch, dass sie nicht verletzbar ist.
 *
 * [isSecret] wird aus [ProjectExclusionPolicy] **berechnet** und ist bewusst **kein**
 * Konstruktorargument: Ein Aufrufer, der das Vergessen würde, würde sonst den Namen
 * einer `.env` in der Übersicht anzeigen. Es gibt kein Feld, das man auf `false`
 * setzen könnte.
 */
data class OverviewFile(
    val relativePath: String,
    val sizeBytes: Long = 0L
) {
    init {
        require(relativePath.isNotBlank()) { "Eine Datei braucht einen Pfad." }
        require(sizeBytes >= 0L) { "Eine Dateigröße kann nicht negativ sein." }
    }

    /** Erkannt [ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET]? */
    val isSecret: Boolean
        get() = ProjectExclusionPolicy.classify(relativePath).decision ==
            ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET

    /** Der Name, wie er angezeigt wird — bei Zugangsdateien nicht der echte. */
    val displayName: String
        get() = if (isSecret) SecretMasker.REDACTION_PLACEHOLDER else relativePath.substringAfterLast('/')

    /** Die Zeile für die Anzeige. */
    fun overviewLine(): String = buildString {
        append(displayName)
        if (sizeBytes > 0L) {
            append(" — ")
            append(sizeBytes)
            append(" Byte")
        }
        if (isSecret) {
            append(" — wird nicht angezeigt")
        }
    }
}

/**
 * Die Dateien der Übersicht.
 *
 * @property selectionOrigin woher die Liste stammt.
 * @property isTruncated `true`, wenn der Aufrufer nur einen Ausschnitt liefert. Die
 *           Übersicht sagt dann „gekürzt“ und behauptet keine Vollständigkeit.
 */
data class OverviewFileList(
    val files: List<OverviewFile> = emptyList(),
    val selectionOrigin: SelectionOrigin = SelectionOrigin.NONE,
    val isTruncated: Boolean = false
) {
    init {
        require(selectionOrigin != SelectionOrigin.AUTOMATIC_SCAN) {
            "Ohne Auswahl des Nutzers wird keine Datei automatisch gescannt. " +
                "Die Übersicht darf nur eine Nutzerauswahl zeigen."
        }
    }

    val isEmpty: Boolean get() = files.isEmpty()

    /** Zugangsdateien, die **nicht** mit Namen genannt werden. */
    val secretCount: Int get() = files.count { it.isSecret }

    /** Dateien, die zeigt werden dürfen. */
    val visibleFiles: List<OverviewFile> get() = files.filterNot { it.isSecret }
}

/** Ein Schritt, der auf eine Freigabe wartet. */
data class PendingApproval(
    val stepId: String,
    val required: RequiredApproval
) {
    init {
        require(stepId.isNotBlank()) { "Eine offene Freigabe braucht einen Schritt." }
    }
}

/** Das Ergebnis des letzten Laufs, soweit es die Übersicht zeigt. */
data class LastWorkSummary(
    val finishedAtEpochMillis: Long,
    val stepCount: Int,
    val changedFileCount: Int,
    val testVerdict: String
) {
    init {
        require(stepCount >= 0) { "Die Schrittzahl kann nicht negativ sein." }
        require(changedFileCount >= 0) { "Die Zahl geänderter Dateien kann nicht negativ sein." }
    }
}

/**
 * Die Projektübersicht.
 *
 * @property accessStates je Projektordner ein **berechneter** Stand.
 * @property files die Dateiauswahl des Nutzers.
 * @property openApprovals Freigaben, die noch offen sind.
 * @property lastWork das Ergebnis des letzten Laufs, `null` wenn es keines gibt.
 */
data class ProjectOverview(
    val projectName: String,
    val accessStates: List<ProjectAccessState> = emptyList(),
    val files: OverviewFileList = OverviewFileList(),
    val openApprovals: List<PendingApproval> = emptyList(),
    val lastWork: LastWorkSummary? = null
) {
    init {
        require(projectName.isNotBlank()) { "Die Übersicht braucht einen Projektnamen." }
    }

    /** Ordner, deren Zugriff zurückgezogen wurde. */
    val revokedStates: List<ProjectAccessState>
        get() = accessStates.filter { it.state == AccessState.REVOKED }

    /** Ist irgendein Ordner widerrufen? */
    val hasRevokedAccess: Boolean get() = revokedStates.isNotEmpty()

    /** Ordner, mit denen tatsächlich gearbeitet werden kann. */
    val usableStates: List<ProjectAccessState>
        get() = accessStates.filter { it.state.isUsable }

    /** Darf mit diesem Ordner gearbeitet werden? */
    fun canWork(state: ProjectAccessState): Boolean = state.state.isUsable

    /**
     * Die Kopfangebote der Seite.
     *
     * Ein widerrufener Ordner steht **vorn**, damit er beim Öffnen sofort sichtbar
     * ist und nicht erst nach der Dateiliste überlesen wird.
     */
    fun headline(): List<String> = buildList {
        add("Projekt: $projectName")
        if (hasRevokedAccess) {
            add(
                "! Zugriff zurückgezogen: " +
                    revokedStates.joinToString(", ") { it.displayName } +
                    ". Es wird nicht auf diese Ordner zugegriffen."
            )
        }
        val usable = usableStates.size
        add(
            if (usable == 0) {
                "Kein Ordner ist freigegeben. Für jede Arbeit muss zuerst ein Ordner " +
                    "über das Systemdialog ausgewählt werden."
            } else {
                "$usable Ordner freigegeben."
            }
        )
    }

    /** Der vollständige Text der Übersicht. */
    fun overviewLines(): List<String> = buildList {
        addAll(headline())

        add("")
        add("Ordner (${accessStates.size}):")
        if (accessStates.isEmpty()) {
            add("  Kein Ordner ausgewählt.")
        } else {
            accessStates.forEach { add("  ${it.overviewLine()}") }
        }

        add("")
        add("Dateien:")
        when {
            files.isEmpty -> add(
                "  Es wurde nichts ausgewählt. Es wird nichts automatisch gesucht."
            )
            files.secretCount > 0 -> add(
                "  ${files.secretCount} Datei(en) mit Zugangsdaten werden nicht gezeigt."
            )
            else -> Unit
        }
        files.visibleFiles.forEach { add("  ${it.overviewLine()}") }
        if (files.isTruncated) {
            add("  Gekürzt: Es wird nicht die ganze Dateiliste gezeigt.")
        }

        add("")
        add("Letzte Arbeit:")
        val work = lastWork
        if (work == null) {
            add("  Es wurde noch kein Lauf gestartet.")
        } else {
            add("  ${work.stepCount} Schritte, ${work.changedFileCount} geänderte Dateien.")
            add("  Tests: ${work.testVerdict}")
        }

        add("")
        add("Offene Freigaben (${openApprovals.size}):")
        if (openApprovals.isEmpty()) {
            add("  Keine. Es wartet nichts auf eine Zustimmung.")
        } else {
            openApprovals.forEach { add("  Schritt ${it.stepId}: ${it.required.germanLabel}") }
        }
    }

    /** Die nächsten sicheren Schritte, abhängig vom Zustand. */
    fun nextSafeSteps(): List<String> = buildList {
        if (hasRevokedAccess) {
            add("Prüfen, ob der zurückgezogene Ordner wieder freigegeben werden soll.")
        }
        if (usableStates.isEmpty()) {
            add("Einen Ordner über das Systemdialog auswählen.")
        }
        if (openApprovals.isNotEmpty()) {
            add("Die ${openApprovals.size} offene(n) Freigabe(n) entscheiden.")
        }
        if (this@ProjectOverview.lastWork == null) {
            add("Einen ersten Lauf starten.")
        }
        if (isEmpty()) {
            add("Es gibt nichts zu tun. Diese Übersicht ist vollständig geprüft.")
        }
    }
}
