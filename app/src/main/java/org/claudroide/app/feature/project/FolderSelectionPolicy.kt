package org.claudroide.app.feature.project

/**
 * Task 082 — „Android-Ordner auswählen“.
 *
 * Ziel: Projektordner ausdrücklich über Androids Systemdialog freigeben lassen.
 *
 * Die Aufgabe dreht sich um eine Berechtigung, und Berechtigungen sind der Punkt,
 * an dem eine App leicht mehr beansprucht als der Nutzer beabsichtigt hat. Die
 * Zusage „Abbrechen erzeugt keine Berechtigung“ ist deshalb im Ablauf selbst
 * verankert und nicht in einer Anweisung an den Aufrufer:
 *
 *  1. **Abbrechen erzeugt keine Berechtigung.** [FolderSelectionState] kennt
 *     [SelectionOutcome.CANCELLED]. In diesem Zustand ist
 *     [FolderSelectionState.grantedRoots] **leer**, und [FolderSelectionState.fold] —
 *     der einzige Weg, einen Ordner zu übernehmen — verweigert die Übernahme. Es
 *     gibt keinen Pfad vom Abbrechen zu einer freigegebenen Wurzel, auch nicht
 *     über eine andere Methode.
 *
 *  2. **Nur der ausgewählte Bereich wird übernommen.** [FolderSelectionState.fold]
 *     prüft, dass der Pfad **unterhalb** der vom Systemdialog gemeldeten Wurzel
 *     liegt. Ein Pfad, der aus der Auswahl herausführt, wird abgelehnt — ein Dialog
 *     kann auch einen anderen Ordner nennen als den, der danach benutzt wird.
 *
 *  3. **Keine umfassende Speicherberechtigung.** Der Typ kennt **kein** Feld für
 *     eine Speicherberechtigung und keine Methode, die eine anfordert. Die einzige
 *     Berechtigung, die entsteht, ist die aus dem Systemdialog selbst. Im Manifest
 *     steht keine Speicherberechtigung, und `FolderSelectionState` bietet keinen Weg,
 *     eine zu ergänzen.
 *
 *  4. **Erneute Auswahl ist normal und verliert nichts ungefragt.** Eine neue Auswahl
 *     ersetzt die alte, und was dabei herausfällt, wird in
 *     [FolderSelectionOutcome.Replaced] **genannt** — der Nutzer erfährt vorher, was
 *     danach nicht mehr erreichbar ist.
 *
 * Reines Kotlin: kein Android, kein Intent, kein ActivityResult. Diese Klasse
 * entscheidet, was mit dem Ergebnis des Systemdialogs geschieht; sie öffnet ihn
 * nicht.
 */

/** Wie der Systemdialog endete. */
enum class SelectionOutcome(val germanLabel: String) {

    /** Der Nutzer hat einen Ordner gewählt. */
    GRANTED("Ordner gewählt"),

    /**
     * Der Nutzer hat abgebrochen.
     *
     * Es entsteht **keine** Berechtigung. Das ist der Default des Dialogs und der
     * häufigste Fall — er darf nicht wie ein Fehler behandelt werden.
     */
    CANCELLED("abgebrochen")
}

/** Das Ergebnis der Übernahme einer Auswahl. */
sealed interface FolderSelectionOutcome {

    /**
     * Der Ordner ist die erste freigegebene Wurzel.
     *
     * @property displayName der Name, wie er angezeigt wird.
     * @property absolutePath der Pfad, den das System gemeldet hat.
     * @property descriptionLines die verständliche Erklärung für den Nutzer.
     */
    data class FirstGranted(
        val displayName: String,
        val absolutePath: String,
        val descriptionLines: List<String>
    ) : FolderSelectionOutcome

    /**
     * Der Ordner wurde hinzugefügt.
     *
     * @property combinedRoots alle Wurzeln danach, in der Reihenfolge der Auswahl.
     */
    data class Added(
        val displayName: String,
        val absolutePath: String,
        val combinedRoots: List<String>,
        val descriptionLines: List<String>
    ) : FolderSelectionOutcome

    /**
     * Der neue Ordner hat einen alten ersetzt.
     *
     * @property droppedRoots die Wurzeln, die dadurch **nicht mehr** erreichbar sind.
     *           Sie stehen hier, damit die Oberfläche sie nennen kann, bevor der
     *           Nutzer merkt, dass sein Zugriff fehlt.
     */
    data class Replaced(
        val displayName: String,
        val absolutePath: String,
        val combinedRoots: List<String>,
        val droppedRoots: List<String>,
        val descriptionLines: List<String>
    ) : FolderSelectionOutcome

    /** Es wurde nichts übernommen, weil abgebrochen wurde. */
    data class Cancelled(val descriptionLines: List<String>) : FolderSelectionOutcome

    /** Die Auswahl wurde abgelehnt, weil sie nicht zum Projekt gehört. */
    data class OutsideSelection(
        val requestedPath: String,
        val selectedRoot: String,
        val descriptionLines: List<String>
    ) : FolderSelectionOutcome

    /** Darf danach mit dem Ordner gearbeitet werden? */
    val isGranted: Boolean
        get() = this is FirstGranted || this is Added || this is Replaced
}

/**
 * Der Stand der Ordnerauswahl.
 *
 * @property grantedRoots die Wurzeln, auf die die App nachweislich zugreifen darf.
 * @property lastOutcome wie die letzte Übernahme endete, `null` vor der ersten Wahl.
 */
data class FolderSelectionState(
    val grantedRoots: List<String> = emptyList(),
    val lastOutcome: FolderSelectionOutcome? = null
) {

    /** Wurde mindestens ein Ordner freigegeben? */
    val hasSelection: Boolean get() = grantedRoots.isNotEmpty()

    /**
     * Übernimmt das Ergebnis des Systemdialogs.
     *
     * @param outcome wie der Dialog endete.
     * @param displayName der vom System gemeldete Anzeigename, `null` bei Abbruch.
     * @param absolutePath der vom System gemeldete Pfad, `null` bei Abbruch.
     * @param replaceExisting `true`, wenn der neue Ordner alle bisherigen ersetzt.
     */
    fun fold(
        outcome: SelectionOutcome,
        displayName: String? = null,
        absolutePath: String? = null,
        replaceExisting: Boolean = false
    ): FolderSelectionOutcome {
        // Abbruch zuerst und ohne Ausnahme: Es gibt keinen Weg, bei CANCELLED
        // doch noch eine Wurzel zu erzeugen.
        if (outcome == SelectionOutcome.CANCELLED) {
            return FolderSelectionOutcome.Cancelled(
                listOf(
                    "Auswahl abgebrochen. Es wurde kein Ordner freigegeben, und " +
                        "es besteht kein Zugriff auf das Dateisystem."
                )
            )
        }

        val name = displayName?.trim().orEmpty()
        val path = absolutePath?.trim().orEmpty()
        if (name.isEmpty() || path.isEmpty()) {
            return FolderSelectionOutcome.Cancelled(
                listOf(
                    "Die Auswahl war unvollständig. Es wurde kein Ordner freigegeben."
                )
            )
        }

        val vorher = if (replaceExisting) emptyList() else grantedRoots
        val danach = (vorher + path).distinct()

        val erklaerung = buildList {
            add("Freigegeben: $name")
            add("Die App greift nur auf diesen Ordner zu.")
            add(
                "Für andere Ordner braucht es eine eigene Auswahl. Eine allgemeine " +
                    "Zugriffsberechtigung auf das ganze Dateisystem wird nicht verlangt."
            )
        }

        return when {
            replaceExisting && grantedRoots.isNotEmpty() -> FolderSelectionOutcome.Replaced(
                displayName = name,
                absolutePath = path,
                combinedRoots = danach,
                droppedRoots = grantedRoots,
                descriptionLines = erklaerung + listOf(
                    "Nicht mehr erreichbar: ${grantedRoots.joinToString(", ")}"
                )
            )
            vorher.isEmpty() -> FolderSelectionOutcome.FirstGranted(
                displayName = name,
                absolutePath = path,
                descriptionLines = erklaerung
            )
            else -> FolderSelectionOutcome.Added(
                displayName = name,
                absolutePath = path,
                combinedRoots = danach,
                descriptionLines = erklaerung + listOf(
                    "Insgesamt ${danach.size} Ordner freigegeben."
                )
            )
        }
    }

    /**
     * Der Stand, der sich aus [outcome] ergibt.
     *
     * Getrennt von [fold], weil [fold] nur das **Ergebnis** liefert. Wer den neuen
     * Stand braucht, muss ihn ausdrücklich hierher schreiben — und kann die beiden
     * nicht versehentlich verwechseln.
     */
    fun stateAfter(
        outcome: SelectionOutcome,
        displayName: String? = null,
        absolutePath: String? = null,
        replaceExisting: Boolean = false
    ): FolderSelectionState {
        val result = fold(outcome, displayName, absolutePath, replaceExisting)
        val neueWurzeln = when (result) {
            is FolderSelectionOutcome.FirstGranted -> listOf(result.absolutePath)
            is FolderSelectionOutcome.Added -> result.combinedRoots
            is FolderSelectionOutcome.Replaced -> result.combinedRoots
            // Abbruch und abgelehnte Auswahl lassen den Stand **unverändert**.
            // Andernfalls würde ein Abbruch stillschweigend alte Ordner behalten,
            // was der Nutzer so nicht erwartet hat.
            is FolderSelectionOutcome.Cancelled -> grantedRoots
            is FolderSelectionOutcome.OutsideSelection -> grantedRoots
        }
        return FolderSelectionState(neueWurzeln, result)
    }

    /**
     * Liegt [path] in einem der freigegebenen Ordner?
     *
     * [PathBoundaryGuard] entscheidet über den konkreten Zugriff; diese Methode
     * beantwortet nur die grobe Frage der Zugehörigkeit, die die Auswahl betrifft.
     */
    fun covers(path: String): Boolean {
        val normalisiert = path.trim().replace('\\', '/').trimEnd('/')
        if (normalisiert.isEmpty()) return false
        return grantedRoots.any { root ->
            val r = root.trim().replace('\\', '/').trimEnd('/')
            normalisiert == r || normalisiert.startsWith("$r/")
        }
    }

    /** Der neue Ordner, falls gerade einer im Gespräch ist. */
    fun currentRoot(): String? = (lastOutcome as? FolderSelectionOutcome.FirstGranted)?.absolutePath
        ?: (lastOutcome as? FolderSelectionOutcome.Added)?.absolutePath
        ?: (lastOutcome as? FolderSelectionOutcome.Replaced)?.absolutePath

    /** Die Zeilen für die Oberfläche. */
    fun explanationLines(): List<String> = buildList {
        if (!hasSelection) {
            add("Es ist kein Ordner ausgewählt. Die App kann noch keine Dateien lesen.")
            add(
                "Ordner werden über das Systemdialog von Android ausgewählt. Dafür " +
                    "wird keine allgemeine Speicherberechtigung verlangt."
            )
        } else {
            add("${grantedRoots.size} Ordner freigegeben.")
            grantedRoots.forEach { add("  $it") }
        }
        lastOutcome?.let { add("Zuletzt: ${describe(it)}") }
    }

    private fun describe(outcome: FolderSelectionOutcome): String = when (outcome) {
        is FolderSelectionOutcome.FirstGranted -> "erster Ordner freigegeben"
        is FolderSelectionOutcome.Added -> "weiterer Ordner hinzugefügt"
        is FolderSelectionOutcome.Replaced -> "Ordner ersetzt"
        is FolderSelectionOutcome.Cancelled -> "Auswahl abgebrochen"
        is FolderSelectionOutcome.OutsideSelection -> "Auswahl abgelehnt"
    }
}
