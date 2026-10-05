package org.claudroide.app.feature.project

import org.claudroide.app.feature.agent.ExtraCost

/**
 * Task 085 — „USB-Projektzugriff".
 *
 * Ziel: Feststellen, welche USB-Dateisysteme und Dateianbieter am A56 lesbar
 * und beschreibbar sind.
 *
 * ## What this file is, and what it is not
 *
 * This is the **decision layer**, and it touches no storage. It describes a
 * volume the user picked, compares it against what was actually probed on the
 * device, and answers whether it may be offered as a project location. It
 * mounts nothing, reads no directory and opens no file — the same separation
 * as tasks 077, 099, 100, 101, 116 and 083, and for the same reason: a type
 * that both decided and accessed would make "the user chose this folder" and
 * "this worked" the same sentence.
 *
 * ## The gate, and why one condition is still open
 *
 * Task 085 is `gate: true`, because its first completion condition — *"only
 * USB paths successfully tested are called available"* — can only be
 * satisfied by a probe on real hardware. `adb devices` shows no device, and
 * `/dev/bus/usb` is not readable from Termux, so **no probe of this project
 * has ever run**.
 *
 * That gap is a **value in the program**, not a sentence in a document:
 * [UsbEvidence.PROBE_OBSERVED_ON_DEVICE] is a `const val = false`. It is
 * `const` and not `var` because a setter would be an invention with a
 * checkbox. A caller cannot flip it; the only way to change it is a new
 * build after a real measurement.
 *
 * [UsbPathAvailability.isUsable] therefore returns `false` for every volume
 * while that constant is `false`. Not as an error, and not as a special
 * "unknown" case that a UI could ignore: **no volume is offered as
 * available**, which is what the completion condition asks for.
 *
 * The same holds for a volume that *was* probed elsewhere: a probe result
 * from a different project, a different device or a different build is not
 * evidence about this one. Only [UsbProbe] values built in a build whose
 * constant is `true` can make a path available.
 *
 * ## The two completion conditions as properties
 *
 *  1. *"Only successfully tested USB paths are called available."*
 *     [UsbPathAvailability.isUsable] reads [UsbProbe.result] and
 *     [UsbEvidence.PROBE_OBSERVED_ON_DEVICE]. It never consults
 *     [UsbVolume.isRemovable], never the volume label, and never the size
 *     the platform reports. A volume can present itself perfectly and still
 *     be refused, because what makes a path usable here is a **measurement**,
 *     not an intention or an enumeration.
 *
 *  2. *"Slow or unreliable storage is explained."*
 *     [UsbProbe.behaviour] is not a speed number the caller supplies. It is a
 *     value built from what the probe observed ([ProbeObservation]), and an
 *     unprobed volume carries [UsbBehaviour.UNTESTED] with a reason — never
 *     "fast". A volume that answered every write correctly but took
 *     [SlowWriteEvidence.FLAGGED_WRITE_MILLIS] or more is [UsbBehaviour.SLOW],
 *     and the explanation names the observed milliseconds rather than
 *     inventing a rate.
 *
 * ## The protection: USB access requires an explicit folder choice
 *
 * [UsbVolume] cannot be constructed without [UsbVolume.pickedByUser] — there
 * is no constructor argument, default or factory that skips it. [UsbProbe]
 * carries no path that was not derived from a picked volume, and
 * [UsbPathAvailability.coveredPath] refuses a path outside the picked tree.
 * The skills assigned to this task say the same thing in general form: scope
 * a grant to what the user actually chose, and check the grant dynamically
 * rather than caching it.
 *
 * ## What this file deliberately does not model
 *
 * **No speed figure is invented.** [SlowWriteEvidence.FLAGGED_WRITE_MILLIS]
 * is a threshold at which a probe result is *flagged*, not a specification of
 * what USB storage achieves on an SM-A566B. Nothing in this project has
 * measured that, and the constant is named so it cannot be read as a
 * performance claim.
 *
 * **No volume is named.** This project has never enumerated a USB volume on
 * the device, so there is no vendor name, no size and no filesystem type
 * anywhere below. Those are user- and device-specific values; they arrive as
 * [UsbProbeObservation] input.
 *
 * Pure Kotlin: no network, no filesystem, no process, no Android.
 */
object UsbEvidence {

    /**
     * Has this project ever completed a USB probe on a real device?
     *
     * `const val false` and not a `var`: this is a measured fact about the
     * project, and a value that a caller could set would turn "nobody has
     * tested this" into "somebody asserted this". It becomes `true` only in
     * a build made after a real probe on an SM-A566B.
     */
    const val PROBE_OBSERVED_ON_DEVICE: Boolean = false

    /** The lines explaining why no USB path is on offer. */
    fun statementLines(): List<String> = listOf(
        "Es liegt keine USB-Messung dieses Projekts auf einem Gerät vor.",
        "Darum wird kein USB-Weg als verfügbar bezeichnet — auch nicht vorläufig."
    )
}

/**
 * A write speed that is **too slow to keep using without saying so**.
 *
 * This is a flag threshold inside a probe result, not a specification of USB
 * storage on any device. No figure here has been measured on an SM-A566B, and
 * nothing in this project should be read as claiming what USB storage
 * achieves.
 */
object SlowWriteEvidence {

    /** At or above this many milliseconds, a probe write is flagged slow. */
    const val FLAGGED_WRITE_MILLIS: Long = 1_500

    /** How many probe writes a [UsbProbe] needs before behaviour is meaningful. */
    const val MINIMUM_WRITES_FOR_BEHAVIOUR: Int = 3
}

/**
 * How a volume behaved under the probe.
 *
 * The values are **evidence**, not grades a caller assigns. There is no
 * `FAST_BY_ASSUMPTION`: a volume nobody wrote to is [UNTESTED], which is the
 * only honest description of a path with no measurement behind it.
 */
enum class UsbBehaviour(
    val label: String,
    val germanLabel: String
) {

    /** Reads and writes both answered, within the flag threshold. */
    RELIABLE("reliable", "zuverlässig"),

    /** Writes answered, but at or above the flag threshold. */
    SLOW("slow", "langsam"),

    /** A write did not answer, or read and write disagreed. */
    UNRELIABLE("unreliable", "unzuverlässig"),

    /** Nothing was measured. Carries a reason, deliberately no verdict. */
    UNTESTED("untested", "nicht getestet");

    /** May a project be offered here? Only a measured, reliable volume. */
    val isOfferable: Boolean get() = this == RELIABLE
}

/**
 * What one probe of a volume observed.
 *
 * The caller supplies these **after** actually writing to the volume; this
 * type does no measuring. The separation matters for the same reason as
 * everywhere else in this project: "the write answered" and "we decided the
 * write is good enough" are different claims.
 *
 * @property readAnswered whether a read back of a written file returned the
 *           same bytes.
 * @property writesCompleted how many probe writes returned without error.
 * @property slowestWriteMillis the slowest observed write, or `null` when
 *           nothing was written.
 * @property writeErrors how many probe writes failed.
 */
data class ProbeObservation(
    val readAnswered: Boolean,
    val writesCompleted: Int,
    val slowestWriteMillis: Long?,
    val writeErrors: Int
) {
    init {
        require(writesCompleted >= 0) {
            "Anzahl erfolgreicher Schreibvorgänge kann nicht negativ sein."
        }
        require(writeErrors >= 0) { "Anzahl Schreibfehler kann nicht negativ sein." }
        require(slowestWriteMillis == null || slowestWriteMillis >= 0) {
            "Eine gemessene Dauer kann nicht negativ sein."
        }
    }

    /**
     * The behaviour these observations amount to.
     *
     * Derived, because it is a property of the numbers and not a separate
     * opinion: a caller that supplied a speed verdict of its own could
     * disagree with the measurement it just reported.
     *
     * Untested wins over every other value — a volume with no completed write
     * is not "reliable because nothing went wrong", it is untested.
     */
    val behaviour: UsbBehaviour
        get() = when {
            writesCompleted < SlowWriteEvidence.MINIMUM_WRITES_FOR_BEHAVIOUR -> UsbBehaviour.UNTESTED
            writeErrors > 0 || !readAnswered -> UsbBehaviour.UNRELIABLE
            (slowestWriteMillis ?: 0L) >= SlowWriteEvidence.FLAGGED_WRITE_MILLIS -> UsbBehaviour.SLOW
            else -> UsbBehaviour.RELIABLE
        }

    /** The lines explaining this behaviour to the user. */
    fun explanationLines(): List<String> = buildList {
        when (behaviour) {
            UsbBehaviour.UNTESTED ->
                add("Nicht getestet: $writesCompleted erfolgreiche Schreibvorgang(e), " +
                    "nötig sind ${SlowWriteEvidence.MINIMUM_WRITES_FOR_BEHAVIOUR}.")

            UsbBehaviour.SLOW ->
                add("Langsam: $writesCompleted Schreibvorgang(e) erfolgreich, " +
                    "langsamster $slowestWriteMillis ms. Ab " +
                    "${SlowWriteEvidence.FLAGGED_WRITE_MILLIS} ms gilt ein " +
                    "Schreibvorgang als langsam.")

            UsbBehaviour.UNRELIABLE -> {
                add("$writeErrors Schreibfehler.")
                add(if (readAnswered) "Lesen antwortete." else "Lesen antwortete nicht.")
            }

            UsbBehaviour.RELIABLE ->
                add("Zuverlässig im Test: $writesCompleted Schreibvorgang(e), " +
                    "langsamster $slowestWriteMillis ms, Lesen bestätigt.")
        }
        if (slowestWriteMillis == null) {
            add("Es wurde keine Dauer gemessen — es wird auch keine behauptet.")
        }
    }
}

/** A volume the user picked, and what the probe found on it. */
data class UsbProbe(
    val volume: UsbVolume,
    val observation: ProbeObservation?,
    /** Was das System beim Zugriff gemeldet hat, unverändert weitergereicht. */
    val message: String? = null
) {
    init {
        require(volume.pickedByUser) {
            "Ein USB-Weg braucht eine vom Nutzer ausgewählte Ordnerfreigabe."
        }
    }

    /** The behaviour of this probe. Untested when nothing was measured. */
    val behaviour: UsbBehaviour get() = observation?.behaviour ?: UsbBehaviour.UNTESTED

    /** Lines the user sees before deciding whether to use this path. */
    fun displayLines(): List<String> = buildList {
        add("USB-Volume: ${volume.label}")
        add(volume.pathDescription())
        add("Zugriff: ${volume.scope.germanLabel}")
        add("Verhalten: ${behaviour.germanLabel}")
        observation?.explanationLines()?.forEach { add("  $it") }
            ?: add("  Es liegt keine Messung vor.")
        message?.let { add("Meldung des Systems: $it") }
    }
}

/**
 * A volume the user picked with the system folder chooser.
 *
 * @property pickedByUser always `true`. This is the whole point of the
 *           protection "USB access requires an explicit folder choice":
 *           there is no way to build a volume that claims access nobody
 *           granted. A constructor overload with a default would make the
 *           phrase decorative, so it does not exist.
 * @property removable whether the platform reports the volume as removable.
 *           Deliberately **not** used as evidence of usability: a removable
 *           volume that answers every write is still unusable until probed,
 *           and a fixed internal volume that is probed and reliable is not
 *           disqualified for being internal.
 */
data class UsbVolume private constructor(
    val label: String,
    val rootUri: String,
    val isRemovable: Boolean,
    val reportedFilesystem: String?,
    val pickedByUser: Boolean
) {

    /**
     * Build a volume the user picked.
     *
     * The only way to obtain a [UsbVolume]. There is no secondary factory
     * and no default that could skip the folder choice.
     */
    companion object {

        /**
         * @param rootUri the URI the user chose in the system folder dialog.
         * @param isRemovable what the platform reports, recorded and not acted on.
         * @param reportedFilesystem what the platform reports, recorded and not
         *        acted on. `null` when the platform named nothing.
         */
        fun pickedByUser(
            label: String,
            rootUri: String,
            isRemovable: Boolean,
            reportedFilesystem: String? = null
        ): UsbVolume {
            require(label.isNotBlank()) { "Ein Volume braucht eine Bezeichnung." }
            require(rootUri.isNotBlank()) { "Ein Volume braucht einen gewählten URI." }
            return UsbVolume(
                label = label,
                rootUri = rootUri,
                isRemovable = isRemovable,
                reportedFilesystem = reportedFilesystem,
                pickedByUser = true
            )
        }
    }

    /** The access this volume grants, from [AttemptScope] of task 077's model. */
    val scope: UsbVolumeScope get() = UsbVolumeScope.FOLDER_CHOSEN_BY_USER

    /** One line describing what exactly was chosen. */
    fun pathDescription(): String = "$label — gewählter Ordner: $rootUri"
}

/**
 * What access a picked USB volume grants.
 *
 * Modelled rather than reused from [UsbVolume] itself so the protection has
 * a name that can be referred to in a test and in a review.
 */
enum class UsbVolumeScope(val label: String, val germanLabel: String) {

    /** Exactly the folder the user picked, and nothing beside it. */
    FOLDER_CHOSEN_BY_USER("folder chosen by user", "nur der vom Nutzer gewählte Ordner")
}

/**
 * The decision: may this USB path be offered as a project location?
 *
 * Mirrors the structure of [PersistentFolderAccess] from task 083 rather than
 * inventing a second access model: availability is asked of a caller-supplied
 * prober at call time, never cached, so a volume that vanished is noticed on
 * the next call instead of on the next app start.
 */
object UsbPathAvailability {

    /**
     * May [volume] be offered, given what [probe] found?
     *
     * Both halves of the completion condition are here and neither is
     * derived: the probe must have observed something
     * ([UsbProbe.observation] non-null) **and** that observation must amount
     * to [UsbBehaviour.RELIABLE]. A slow volume is not offered because the
     * task asks for reliability; it is explained instead by
     * [UsbProbe.displayLines].
     */
    fun isUsable(volume: UsbVolume, probe: UsbProbe?): Boolean {
        if (!UsbEvidence.PROBE_OBSERVED_ON_DEVICE) return false
        if (probe == null) return false
        if (probe.volume.rootUri != volume.rootUri) return false
        return probe.behaviour.isOfferable
    }

    /**
     * Is [path] inside the folder the user picked?
     *
     * Compares at the path separator, so a neighbouring directory with a
     * similar name cannot slip through — the same rule task 083 applies in
     * [PersistentFolderAccess.covers].
     */
    fun coveredPath(volume: UsbVolume, path: String): Boolean {
        val root = volume.rootUri.trimEnd('/')
        val target = path.trimStart('/')
        return target.startsWith("$root/")
    }

    /**
     * Can a refusal be overridden by hand?
     *
     * Never. Same reasoning as tasks 100, 101, 116 and 077: a protection with
     * an override switch is not a protection. The remedy for a volume that
     * does not work is to probe it again or to pick another folder, not to
     * insist.
     */
    fun canOverride(): Boolean = false

    /** The lines shown when no USB path is on offer. */
    fun unavailableLines(): List<String> = buildList {
        addAll(UsbEvidence.statementLines())
        add(
            "Ein erfolgreicher Test auf dem Gerät ist nötig. Ein Wechsel des " +
                "Speichersystems ändert daran nichts."
        )
    }
}