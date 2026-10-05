package org.claudroide.app.feature.agent

/**
 * Task 116 — "install project tools" (Projektwerkzeuge installieren).
 *
 * Goal: install dependencies deliberately, with size, source and consequences.
 * Result: package name, origin, license, required storage, network access and
 * removal, each explained before anything is fetched.
 *
 * ## What this file is, and what it is not
 *
 * This is the **decision layer**, and it performs no installation. It reads a
 * described package and answers whether it may be installed, on what evidence,
 * and what the user was told. Nothing here downloads, unpacks or writes — the
 * same separation as tasks 095–104 and 100–101, and for the same reason: a type
 * that both decided and fetched would make "the user approved this" and "this is
 * what happened" the same sentence.
 *
 * The gate on task 116 (`gate: true`) is about **installation**, which needs a
 * device and a user's package choice. Neither is invented here.
 * [DependencyEvidence.INSTALL_OBSERVED_ON_DEVICE] is a `const false` so the gap
 * is a value in the program rather than a sentence in a document.
 *
 * ## The two completion conditions as properties
 *
 *  1. *"The user can confirm download and installation individually."*
 *     [DependencyInstallGate.mayProceed] takes two separate inputs,
 *     `userConfirmedDownload` and `userConfirmedInstall`, each defaulting to
 *     `false`. They are not one flag because a user who allows a download has
 *     not allowed an installation, and the second is the one that changes the
 *     device.
 *
 *  2. *"Errors do not hide a half-finished state."*
 *     [InstallOutcome] has a [PARTIAL] value and no way to express "installed"
 *     without a completion report. [InstallState] is built from what the file
 *     system reported, never from a status word.
 *
 * ## The protection: no unverified package from an arbitrary source
 *
 * [PackageOrigin] is a closed set of four values. [DependencyInstallGate] refuses
 * [UNVERIFIED] outright, and there is no override — the same decision taken in
 * task 100's `GitSecretGate.canOverride`, for the same reason: a protection a
 * caller can switch off is not a protection.
 *
 * Pure Kotlin: no network, no filesystem, no process, no Android.
 */

// ── Where a package comes from ─────────────────────────────────────────────

/**
 * How well the origin of a package is known.
 *
 * The values are **evidence**, not trust levels handed out by the user. There is
 * no `COMMUNITY` or `UNKNOWN_YET` that happens to be installable: [UNVERIFIED] is
 * the absence of evidence and it is refused.
 */
enum class PackageOrigin(val label: String, val germanLabel: String) {

    /** Part of the Android platform. Nothing to fetch, nothing to license. */
    PLATFORM("platform", "Teil von Android"),

    /** Bundled with the app, built from source whose licence this project knows. */
    BUNDLED("bundled", "mit der App geliefert"),

    /** A published package whose licence was read from its own metadata. */
    KNOWN_LICENCE("known licence", "Lizenz gelesen"),

    /** A name and nothing else. Refused by [DependencyInstallGate]. */
    UNVERIFIED("unverified", "Herkunft ungeprüft");

    /** May a package from here be installed at all? */
    val isInstallable: Boolean get() = this != UNVERIFIED
}

/**
 * What the licence permits.
 *
 * [UNKNOWN] is a real value and not `null`: "nobody read the licence" is a fact
 * the user needs, and it is different from "the licence says proprietary".
 */
enum class DependencyLicence(val label: String, val germanLabel: String) {
    APACHE_2("apache-2.0", "Apache 2.0"),
    MIT("mit", "MIT"),
    GPL("gpl", "GPL"),
    PROPRIETARY("proprietary", "proprietär"),
    UNKNOWN("unknown", "unbekannt");

    /**
     * Is copying this into a distributed app allowed?
     *
     * [UNKNOWN] and [PROPRIETARY] both refuse, because "nobody checked" and
     * "checked, and it forbids this" lead to the same action: do not bundle it.
     */
    val permitsBundling: Boolean
        get() = this == APACHE_2 || this == MIT
}

// ── The described package ──────────────────────────────────────────────────

/**
 * A package as it was **described** — before anything was fetched.
 *
 * [downloadBytes] is what the registry reported, not a measurement on this
 * device. [unpackedBytes] matters more than [downloadBytes] on a phone: an archive
 * of 40 MB routinely unpacks to several hundred, and the free space check has to
 * read the second number or it will approve an install that fails halfway.
 */
data class DependencyDescriptor(
    val packageName: String,
    val version: String,
    val origin: PackageOrigin,
    val licence: DependencyLicence,
    val downloadBytes: Long,
    val unpackedBytes: Long,
    val needsNetwork: Boolean,
    /** What the package can do that ordinary project files cannot. */
    val capabilities: List<String> = emptyList()
) {
    init {
        require(packageName.isNotBlank()) { "A package needs a name." }
        require(version.isNotBlank()) { "A package needs a version." }
        require(downloadBytes >= 0) { "A download size cannot be negative." }
        require(unpackedBytes >= 0) { "An unpacked size cannot be negative." }
        require(capabilities.none { it.isBlank() }) {
            "An empty capability is not a capability."
        }
    }

    /** The size that decides whether it fits: what it costs once unpacked. */
    val requiredFreeBytes: Long get() = unpackedBytes

    fun displayLine(): String =
        "$packageName:$version — ${origin.germanLabel}, Lizenz ${licence.germanLabel}"

    /** The lines explaining the package before the user decides. */
    fun explanationLines(): List<String> = buildList {
        add(displayLine())
        add("Herkunft: ${origin.germanLabel}")
        add("Lizenz: ${licence.germanLabel}")
        add("Download: ${groesse(downloadBytes)}")
        add("Entpackt: ${groesse(unpackedBytes)} (das ist der Platz, der wirklich gebraucht wird)")
        add("Netzwerk: ${if (needsNetwork) "ja, während der Installation" else "nein"}")
        if (capabilities.isEmpty()) {
            add("Fähigkeiten: keine über die Projektdateien hinausgehenden bekannt")
        } else {
            add("Fähigkeiten:")
            capabilities.forEach { add("  $it") }
        }
        add(if (licence.permitsBundling) {
            "Die Lizenz erlaubt das Mitliefern in einer verbreiteten App."
        } else {
            "Die Lizenz erlaubt das Mitliefern nicht. Das Paket darf nur lokal benutzt werden."
        })
    }

    private fun groesse(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

/** The device state an install depends on, measured rather than assumed. */
data class DeviceStorage(
    val freeBytes: Long
) {
    init {
        require(freeBytes >= 0) { "Freier Speicher kann nicht negativ sein." }
    }
}

// ── The gate ───────────────────────────────────────────────────────────────

/** The answer to "may this package be installed?". */
sealed interface InstallDecision {
    val germanLabel: String

    /** Download and installation may start. The installing is outside this file. */
    data class MayInstall(val descriptor: DependencyDescriptor) : InstallDecision {
        override val germanLabel: String get() = "darf installiert werden"
    }

    /** Nothing is fetched and nothing is installed. */
    data class Refused(val reason: String) : InstallDecision {
        override val germanLabel: String get() = "nicht installiert"
    }
}

/**
 * The rule for when a dependency may be installed.
 *
 * Checked in a fixed order, and the order is the argument: origin first,
 * because an unverified source makes every later answer irrelevant; licence
 * next, because it decides whether the package may even ship; space before
 * confirmation, because "is there room" is a fact, not a preference; and the two
 * confirmations last, each on its own.
 */
object DependencyInstallGate {

    /**
     * May [descriptor] be installed?
     *
     * @param storage what the device reported as free. An **own** input: it is
     *        never derived from the package size, because a package that fits by
     *        its own arithmetic is exactly the claim that needs checking.
     * @param userConfirmedDownload an own input, default `false`.
     * @param userConfirmedInstall an own input, default `false`. Separate from
     *        the download because allowing a fetch is not allowing an install.
     */
    fun mayProceed(
        descriptor: DependencyDescriptor,
        storage: DeviceStorage,
        userConfirmedDownload: Boolean = false,
        userConfirmedInstall: Boolean = false
    ): InstallDecision {
        // 1. Origin. Without evidence of where it comes from, nothing below matters.
        if (!descriptor.origin.isInstallable) {
            return InstallDecision.Refused(
                "${descriptor.packageName}: Die Herkunft ist ungeprüft. Aus einer " +
                    "beliebigen Quelle wird nichts installiert."
            )
        }

        // 2. Licence. A package that may not ship may still be used locally, so
        //    this refuses the *bundling*, and says so, rather than pretending the
        //    package is unusable.
        if (descriptor.origin == PackageOrigin.BUNDLED && !descriptor.licence.permitsBundling) {
            return InstallDecision.Refused(
                "${descriptor.packageName}: Die Lizenz (${descriptor.licence.germanLabel}) " +
                    "erlaubt das Mitliefern nicht. Das Paket wird nicht installiert."
            )
        }

        // 3. Space. Read from [DependencyDescriptor.requiredFreeBytes] — the
        //    unpacked size, not the download — plus room for the app itself.
        val noetig = descriptor.requiredFreeBytes + HEADROOM_BYTES
        if (storage.freeBytes < noetig) {
            return InstallDecision.Refused(
                "${descriptor.packageName}: Es fehlen ${noetig - storage.freeBytes} Byte " +
                    "(frei: ${storage.freeBytes}, benötigt: $noetig). " +
                    "Es wird nichts heruntergeladen."
            )
        }

        // 4. Download, on its own.
        if (!userConfirmedDownload) {
            return InstallDecision.Refused(
                "Der Download von ${descriptor.packageName} ist nicht bestätigt. " +
                    "Es wird nichts heruntergeladen."
            )
        }

        // 5. Installation, on its own.
        if (!userConfirmedInstall) {
            return InstallDecision.Refused(
                "Die Installation von ${descriptor.packageName} ist nicht bestätigt. " +
                    "Es wird nichts installiert."
            )
        }

        return InstallDecision.MayInstall(descriptor)
    }

    /**
     * Can a refusal be overridden by hand?
     *
     * Never. Same reason as in task 100: a protection with an override switch is
     * not a protection. The remedy for a missing licence is to read it, not to
     * insist.
     */
    fun canOverride(origin: PackageOrigin): Boolean = false

    /** Space kept free on top of the package, so an install cannot fill the device. */
    const val HEADROOM_BYTES: Long = 8L * 1024L * 1024L
}

// ── Condition 2: errors do not hide a half-finished state ──────────────────

/** How an install ended. Three of the four values are not success. */
enum class InstallOutcome(val label: String, val germanLabel: String) {

    /** Present on the device and confirmed there. */
    INSTALLED("installed", "installiert"),

    /**
     * Files were written but the tool never ran.
     *
     * Its own value, not a flavour of [INSTALLED]: the folder exists and the
     * tool does not work, which is the state that wastes the most time when it
     * is reported as success.
     */
    PARTIAL("partial", "teilweise, Werkzeug nicht lauffähig"),

    /** Nothing was written. */
    NOT_INSTALLED("not installed", "nicht installiert"),

    /** The attempt failed and the reason is known. */
    FAILED("failed", "fehlgeschlagen")
}

/**
 * What is actually on the device, as the file system reported it.
 *
 * Built from three observed facts rather than a status word, because the case
 * that matters is the one no status word covers: files present, tool not
 * runnable.
 */
data class InstallState(
    val packageName: String,
    val filesPresent: Boolean,
    val toolRunnable: Boolean,
    val errorMessage: String? = null
) {
    init {
        require(packageName.isNotBlank()) { "Ein Zustand braucht einen Paketnamen." }
        // A runnable tool without files is not a state a package can be in.
        require(!toolRunnable || filesPresent) {
            "Ein lauffähiges Werkzeug braucht Dateien."
        }
    }

    /** Derived, never set, so it cannot disagree with the three facts. */
    val outcome: InstallOutcome
        get() = when {
            errorMessage != null -> InstallOutcome.FAILED
            !filesPresent -> InstallOutcome.NOT_INSTALLED
            !toolRunnable -> InstallOutcome.PARTIAL
            else -> InstallOutcome.INSTALLED
        }

    /**
     * The lines for the user interface.
     *
     * [PARTIAL] names itself in the first line. "The folder exists" is exactly
     * what makes this state look successful from the outside, so the wording
     * leads with what does **not** work.
     */
    fun displayLines(): List<String> = buildList {
        when (outcome) {
            InstallOutcome.INSTALLED -> add("$packageName ist installiert und lauffähig.")

            InstallOutcome.PARTIAL -> {
                add("$packageName: Dateien vorhanden, das Werkzeug ist aber nicht lauffähig.")
                add("Das ist kein fertiger Zustand. Der Ordner existiert, der Befehl nicht.")
            }

            InstallOutcome.NOT_INSTALLED -> add("$packageName wurde nicht installiert. Es liegen keine Dateien vor.")

            InstallOutcome.FAILED -> add("$packageName: Fehlgeschlagen — ${errorMessage ?: "ohne Angabe"}")
        }
    }
}

// ── The honest gap ─────────────────────────────────────────────────────────

/**
 * What an actual installation would have to prove, and what nobody has observed.
 *
 * Task 116 is `gate: true` and its result names a package, a source and a
 * licence. Those are **user-specific choices**: this project does not pick a
 * package on the user's behalf, and inventing one would be the exact failure the
 * task's protection warns about. So the decision layer ships complete and the
 * installation itself stays at `false`.
 */
object DependencyEvidence {

    /** Has anyone installed a package on the device through this path? */
    const val INSTALL_OBSERVED_ON_DEVICE: Boolean = false

    fun statementLines(): List<String> = listOf(
        "Geprüft und entschieden: Herkunft, Lizenz, Speicherbedarf, Netzwerk, Bestätigungen.",
        "Nicht geprüft: eine tatsächliche Installation. Auf diesem Gerät wurde keine ausgeführt.",
        "Paketname, Quelle und Lizenz sind Entscheidungen des Nutzers und werden nicht erfunden.",
        "Ein 'darf installiert werden' ist eine Freigabe, kein Beleg."
    )
}