package org.claudroide.app.feature.project

/**
 * Task 086 — „USB-Verlust abfangen".
 *
 * Ziel: Laufende Projektarbeit sicher anhalten, wenn Speicher verschwindet.
 *
 * ## What this file is, and what it is not
 *
 * This is the **decision layer**, and it moves nothing. It reads what a write
 * or a read reported, and answers whether work may be called saved, and
 * whether the session may continue. It does not write a recovery copy, does
 * not retry, and does not open a second destination — the same separation as
 * tasks 077, 083, 099, 100, 101, 116 and 085, and for the same reason: a type
 * that both decided and rescued would make "the user was warned" and "the
 * work was kept" the same sentence.
 *
 * ## The two completion conditions as properties
 *
 *  1. *"No change is shown as saved when it is not."*
 *     [SaveStatus] has **no** `SAVED` value that a caller can construct
 *     without a confirmation. [SaveStatus.Saved] requires a
 *     [ConfirmedWrite] carrying the bytes written and the system message that
 *     came back; [SaveStatus.Unconfirmed] is what remains when the storage
 *     vanished. There is no optimistic path and no "probably saved", because
 *     "probably" is precisely the word the task forbids.
 *
 *  2. *"Continuing is possible only after a fresh access and file state
 *     check."*
 *     [ResumeGate.mayResume] refuses whenever [StorageSession.accessState]
 *     is not [MediumAccess.VERIFIED] **or** when any file in
 *     [StorageSession.trackedFiles] is not [MediumFileState.CONFIRMED]. Both are
 *     re-checked at call time against the value the caller passes in; nothing
 *     is cached from the moment of the disconnect.
 *
 * ## The protection: no automatic copy to an unknown place
 *
 * The recovery question is answered, never executed. [RecoveryDecision] has
 * exactly three values and **none of them names a destination**:
 * [RecoveryDecision.KeepInPlace] names the folder the user already chose,
 * [RecoveryDecision.HoldInMemory] keeps the bytes in the app and says so, and
 * [RecoveryDecision.Discard] throws the work away explicitly. There is no
 * "save a copy elsewhere" value, because a silent second destination is the
 * failure this task is about.
 *
 * ## The open device condition, stated as a value
 *
 * The interruption of a USB volume has never been observed on this project's
 * device: `adb devices` shows no device. [UsbEvidence.PROBE_OBSERVED_ON_DEVICE]
 * from task 085 is therefore still `false`, and [DisconnectEvidence.
 * INTERRUPTION_OBSERVED_ON_DEVICE] here is `false` for the same reason and
 * with the same intent. The types model the cases; **which case occurs on an
 * SM-A566B is not measured here**.
 *
 * Pure Kotlin: no network, no filesystem, no process, no Android.
 */

/** Was der zuletzt ausgeführte Schreibvorgang zurueckgemeldet hat. */
enum class WriteReport(val label: String, val germanLabel: String) {

    /** Das Dateisystem hat die Bytes bestaetigt. */
    CONFIRMED("confirmed", "bestaetigt"),

    /** Der Vorgang wurde angenommen, aber nicht bestaetigt. */
    UNCONFIRMED("unconfirmed", "unbestaetigt"),

    /** Das Dateisystem meldete einen Fehler. */
    FAILED("failed", "fehlgeschlagen"),

    /** Es wurde gar nichts zurueckgemeldet - das Medium war weg. */
    NO_REPLY("no reply", "keine Antwort");

    /** Darf daraus ein gespeicherter Zustand werden? Nur bei Bestaetigung. */
    val mayCountAsSaved: Boolean get() = this == CONFIRMED
}

/**
 * Ein Schreibvorgang, den das Dateisystem bestaetigt hat.
 *
 * [SaveStatus.Saved] kann nicht ohne eine Instanz dieser Klasse entstehen.
 * Damit ist die Aussage „nichts wird als gespeichert gezeigt, was es nicht
 * ist" nicht eine Zusicherung im Kommentar, sondern eine Bedingung im Typ:
 * es gibt keinen Konstruktorpfad zu `Saved`, der an einem Bestaetigungswert
 * vorbeikommt.
 */
data class ConfirmedWrite(
    val path: String,
    val bytesWritten: Long,
    /** Was das System zurueckgemeldet hat, unveraendert uebernommen. */
    val systemMessage: String?
) {
    init {
        require(path.isNotBlank()) { "Ein bestaetigter Schreibvorgang braucht einen Pfad." }
        require(bytesWritten > 0L) { "Ein bestaetigter Schreibvorgang hat keine Null Bytes." }
    }
}

/**
 * Der Zustand einer Aenderung.
 *
 * [Saved] traegt immer einen [ConfirmedWrite]. [Unconfirmed] und [Lost] sind
 * getrennte Werte, weil sie fuer den Nutzer Unterschiedliches bedeuten:
 * [Unconfirmed] heisst „wir wissen es nicht", [Lost] heisst „es ist weg".
 * Zusammengefasst waeren beide nur eine unangenehme Unklarheit.
 */
sealed interface SaveStatus {

    /**
     * Das Dateisystem hat es bestaetigt.
     *
     * Ohne [ConfirmedWrite] nicht erzeugbar - siehe Klassenkommentar.
     */
    data class Saved(val confirmation: ConfirmedWrite) : SaveStatus

    /** Angefordert, aber nicht bestaetigt. Der Nutzer weiss es nicht. */
    data class Unconfirmed(val path: String, val reportedBy: WriteReport) : SaveStatus {
        init {
            require(path.isNotBlank()) { "Ein unbestaetigter Pfad braucht einen Pfad." }
        }
    }

    /** Das Medium war nicht mehr erreichbar. Die Aenderung ist nicht gespeichert. */
    data class Lost(val path: String) : SaveStatus {
        init {
            require(path.isNotBlank()) { "Ein verlorener Pfad braucht einen Pfad." }
        }
    }

    /**
     * Darf diese Anzeige „gespeichert" sagen?
     *
     * Eine Eigenschaft, keine Hoffnung: nur [Saved] kann es, und [Saved] gibt
     * es nur mit Bestaetigung.
     */
    val mayBeShownAsSaved: Boolean get() = this is Saved

    /** Die Zeilen, die der Nutzer zu sehen bekommt. */
    fun displayLines(): List<String> = when (this) {
        is Saved ->
            listOf("Gespeichert: ${confirmation.path} (${confirmation.bytesWritten} Bytes)")

        is Unconfirmed ->
            listOf(
                "Nicht gespeichert: $path",
                "Das Medium hat es nicht bestaetigt (${reportedBy.germanLabel}). " +
                    "Der Inhalt ist ungesichert."
            )

        is Lost ->
            listOf(
                "Nicht gespeichert: $path",
                "Das Medium war nicht mehr erreichbar. Die Aenderung liegt nur noch " +
                    "im Arbeitsspeicher."
            )
    }
}

/** Ob auf das Medium aktuell zugegriffen werden kann. */
enum class MediumAccess(val label: String, val germanLabel: String) {

    /** Zuletzt erfolgreich geprueft. */
    VERIFIED("verified", "geprueft"),

    /** Das Medium ist weg. */
    GONE("gone", "nicht erreichbar"),

    /** Es ist unbekannt - es wurde noch nicht nachgesehen. */
    UNKNOWN("unknown", "ungeprueft");

    /** Darf ohne erneute Pruefung weitergearbeitet werden? Nein, solange nicht [VERIFIED]. */
    val allowsContinue: Boolean get() = this == VERIFIED
}

/** Der Zustand einer einzelnen Datei nach dem Verlust. */
enum class MediumFileState(val label: String, val germanLabel: String) {

    /** Auf dem Medium wiedergefunden und gelesen. */
    CONFIRMED("confirmed", "bestaetigt"),

    /** Der Pfad existiert nicht mehr. */
    MISSING("missing", "nicht vorhanden"),

    /** Der Pfad ist da, aber sein Inhalt ungeprueft. */
    UNVERIFIED("unverified", "ungeprueft")
}

/**
 * Die Lage nach einem Speicherverlust.
 *
 * @property trackedFiles jede Datei, an der gearbeitet wurde, mit ihrem
 *           geprueften Zustand. Der Pfad wird **nicht** ersetzt: eine Datei,
 *           die nicht mehr da ist, steht hier mit [MediumFileState.MISSING] und
 *           ihrem Pfad, statt in einer anderen Liste zu verschwinden.
 */
data class StorageSession(
    val rootUri: String,
    val accessState: MediumAccess,
    val trackedFiles: List<TrackedFile>,
    /** Meldung des Systems beim Verlust, unveraendert uebernommen. */
    val systemMessage: String? = null
) {
    init {
        require(rootUri.isNotBlank()) { "Eine Sitzung braucht einen gewaehlten Ordner." }
        val namen = trackedFiles.map { it.path }
        require(namen.toSet().size == namen.size) {
            "Zwei Eintraege koennen nicht denselben Pfad tragen."
        }
    }

    /** Die Dateien, die nach dem Verlust fehlen — einzeln benannt. */
    val missingFiles: List<TrackedFile>
        get() = trackedFiles.filter { it.state == MediumFileState.MISSING }

    /** Die Dateien, deren Inhalt nach dem Verlust ungeprueft ist. */
    val unverifiedFiles: List<TrackedFile>
        get() = trackedFiles.filter { it.state == MediumFileState.UNVERIFIED }

    /** Wurde der Zugriff nach dem Verlust ueberhaupt neu geprueft? */
    val accessWasRechecked: Boolean get() = accessState != MediumAccess.UNKNOWN

    /** Die Zeilen fuer die Oberflaeche. */
    fun displayLines(): List<String> = buildList {
        add("Projektordner: $rootUri")
        add("Zugriff: ${accessState.germanLabel}")
        systemMessage?.let { add("Meldung des Systems: $it") }
        if (trackedFiles.isEmpty()) {
            add("Es sind keine Dateien als bearbeitet vermerkt.")
        } else {
            trackedFiles.forEach {
                add("  ${it.path} — ${it.state.germanLabel}")
            }
        }
        if (missingFiles.isNotEmpty()) {
            add("${missingFiles.size} Datei(en) fehlen. Sie werden nicht ersetzt.")
        }
        if (unverifiedFiles.isNotEmpty()) {
            add(
                "${unverifiedFiles.size} Datei(en) sind ungeprueft. " +
                    "Ihr Inhalt wird nicht als gespeichert behauptet."
            )
        }
        if (!accessWasRechecked) {
            add("Der Zugriff wurde nach dem Verlust noch nicht neu geprueft.")
        }
    }
}

/** Eine bearbeitete Datei mit ihrem geprueften Zustand. */
data class TrackedFile(
    val path: String,
    val state: MediumFileState,
    val sizeBytes: Long? = null
) {
    init {
        require(path.isNotBlank()) { "Eine Datei braucht einen Pfad." }
        require(sizeBytes == null || sizeBytes >= 0L) {
            "Eine Dateigroesse kann nicht negativ sein."
        }
    }

    /**
     * Ist der Inhalt dieser Datei nachweislich da?
     *
     * Nur [MediumFileState.CONFIRMED] zaehlt. [UNVERIFIED] ist ausdruecklich kein
     * Beweis: der Pfad ist sichtbar, aber niemand hat den Inhalt gelesen.
     */
    val isContentConfirmed: Boolean get() = state == MediumFileState.CONFIRMED
}

/**
 * Was mit einer nicht gespeicherten Aenderung geschehen soll.
 *
 * **Kein Wert nennt ein fremdes Ziel.** [KeepInPlace] nennt den Ordner, den
 * der Nutzer selbst gewaehlt hat; [HoldInMemory] nennt keinen Ort, weil es
 * keinen gibt - die bytes bleiben im Prozess; [Discard] sagt ausdruecklich,
 * dass wegwerfen eine Entscheidung ist. Ein stilles „irgendwohin kopieren"
 * existiert als Wert nicht.
 */
sealed interface RecoveryDecision {

    /** Im vom Nutzer gewaehlten Ordner belassen. Schreibt nicht selbst. */
    data class KeepInPlace(val rootUri: String) : RecoveryDecision {
        init {
            require(rootUri.isNotBlank()) { "Ein Ziel braucht einen Ordner." }
        }
    }

    /**
     * Nur im Arbeitsspeicher halten.
     *
     * Der ehrliche Fall: der Inhalt existiert, aber nicht auf dem Medium. Wird
     * der Prozess beendet, ist er weg — und [displayLines] sagt das.
     */
    data class HoldInMemory(val bytesAtRisk: Long, val reason: String) : RecoveryDecision {
        init {
            require(bytesAtRisk > 0L) { "Ein Risiko von Null Bytes ist kein Risiko." }
            require(reason.isNotBlank()) { "Ein Verlust braucht einen Grund." }
        }

        /** Darf diese Anzeige „gespeichert" sagen? Nein. */
        val mayBeShownAsSaved: Boolean get() = false
    }

    /** Bewusst verwerfen. Der Nutzer entscheidet das. */
    data class Discard(val discardedPaths: List<String>) : RecoveryDecision {
        init {
            require(discardedPaths.none { it.isBlank() }) {
                "Ein verworfener Pfad darf nicht leer sein."
            }
        }
    }

    /** Die Zeilen fuer die Oberflaeche. */
    fun displayLines(): List<String> = when (this) {
        is KeepInPlace ->
            listOf(
                "Die Aenderung bleibt im gewaehlten Ordner.",
                "Wiederhergestellt wird nichts von selbst."
            )

        is HoldInMemory ->
            listOf(
                "Nicht gespeichert: $bytesAtRisk Byte(s) liegen nur im Arbeitsspeicher.",
                reason,
                "Wird die App beendet, ist dieser Inhalt weg."
            )

        is Discard ->
            buildList {
                add("Verworfen: ${discardedPaths.size} Datei(en).")
                discardedPaths.forEach { add("  $it") }
            }
    }
}

/**
 * Das Tor, das entscheidet, ob weitergearbeitet werden darf.
 *
 * Beide Halften der zweiten Fertig-Bedingung stehen hier und beide werden bei
 * jedem Aufruf neu gelesen: der Zugriffsstand **und** der Dateizustand. Ein
 * guter Zugriff allein genuegt nicht, und ein gepruefter Dateizustand allein
 * ebenfalls nicht.
 */
object ResumeGate {

    /** Wann darf die Sitzung fortgesetzt werden? */
    sealed interface ResumeDecision {

        /** Beide Pruefungen sind durch. */
        data class MayResume(val checkedFiles: Int) : ResumeDecision

        /**
         * Nein - mit benanntem Grund.
         *
         * [blockingPaths] nennt die Dateien einzeln. Eine Regel, deren
         * Gegenstand man nicht sieht, kann man nicht befolgen.
         */
        data class Refused(
            val reason: String,
            val blockingPaths: List<String>
        ) : ResumeDecision
    }

    /**
     * @param session der **aktuelle** Stand, nicht ein gespeicherter von vor
     *        dem Verlust.
     */
    fun mayResume(session: StorageSession): ResumeDecision {
        if (session.accessState == MediumAccess.GONE) {
            return ResumeDecision.Refused(
                reason = "Das Medium ist nicht erreichbar.",
                blockingPaths = session.trackedFiles.map { it.path }
            )
        }

        if (session.accessState == MediumAccess.UNKNOWN) {
            return ResumeDecision.Refused(
                reason = "Der Zugriff wurde nach dem Verlust noch nicht neu geprueft.",
                blockingPaths = session.trackedFiles.map { it.path }
            )
        }

        val offen = session.trackedFiles.filter { !it.isContentConfirmed }
        if (offen.isNotEmpty()) {
            return ResumeDecision.Refused(
                reason = "Der Inhalt aller bearbeiteten Dateien muss bestaetigt sein.",
                blockingPaths = offen.map { it.path }
            )
        }

        return ResumeDecision.MayResume(session.trackedFiles.size)
    }

    /**
     * Darf eine Ablehnung von Hand uebersteuert werden?
     *
     * Nie. Wie in 077, 100, 101 und 116: Ein Schutz mit Umschalter ist kein
     * Schutz. Der Ausweg aus einem fehlenden Medium ist eine gepruefte
     * Neuverbindung, nicht ein Bestehen auf dem Ergebnis.
     */
    fun canOverride(): Boolean = false

    /** Die Zeilen, wenn das Tor nicht freigibt. */
    fun refusalLines(decision: ResumeDecision.Refused): List<String> = buildList {
        add(decision.reason)
        if (decision.blockingPaths.isNotEmpty()) {
            add("Betroffene Dateien:")
            decision.blockingPaths.forEach { add("  $it") }
        }
        add("Es wird nichts von selbst gerettet und nichts an einen unbekannten Ort geschrieben.")
    }
}

/**
 * Die offene Geraetebedingung dieser Aufgabe.
 *
 * Ein Abbruch eines USB-Volumes wurde auf dem Geraet dieses Projekts noch
 * nie beobachtet: `adb devices` zeigt kein Geraet. Der Wert ist `const` und
 * kein `var`, damit „niemand hat es gesehen" nicht zu „jemand behauptet es"
 * werden kann.
 */
object DisconnectEvidence {

    /** Wurde ein Speicherverlust auf einem echten Geraet beobachtet? */
    const val INTERRUPTION_OBSERVED_ON_DEVICE: Boolean = false

    /** Die Zeilen, die diese Luecke benennen. */
    fun statementLines(): List<String> = listOf(
        "Es liegt keine Messung eines Speicherverlusts auf einem Geraet vor.",
        "Die Faelle in dieser Datei sind modelliert, nicht am Geraet beobachtet."
    )
}