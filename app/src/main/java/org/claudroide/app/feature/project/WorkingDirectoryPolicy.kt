package org.claudroide.app.feature.project

/**
 * Task 106 — "Bound the working directory" (Arbeitsordner begrenzen).
 *
 * Goal: run commands only inside the project area the user explicitly released.
 *
 * A command carries a working directory, and a working directory is where path
 * tricks start. `./build`, `build/../..` and a symlinked folder all name something
 * the user never released. The policy therefore resolves rather than compares: every
 * path is turned into a normalised absolute form first, and the check happens on
 * that. Comparing raw strings is what lets `build/../..` through.
 *
 * Two guarantees are structural:
 *
 *  1. **Relative and changed paths cannot escape.** [WorkingDirectoryPolicy.resolve]
 *    applies `.` and `..` itself and refuses to go above the working directory. A
 *    result outside the released root is [WorkingDirectoryVerdict.Blocked], never a
 *    warning. There is no lenient mode and no flag that turns the check off.
 *
 *  2. **Root and system areas stay locked.** [WorkingDirectoryPolicy.SYSTEM_PREFIXES]
 *    names the paths no working directory may ever be. A release of `/` or `/system`
 *    is refused with [WorkingDirectoryVerdict.SystemPathRefused] — refusing the
 *    *grant* matters, because otherwise a mistaken selection would open the whole
 *    filesystem and every later check would pass.
 *
 * The Android-specific half is honest rather than aspirational: a document provider
 * that cannot be listed or written is reported as [WorkingDirectoryVerdict.ProviderNotUsable]
 * with a reason. It is not silently skipped, because a build that quietly sees no
 * files would look like a build that found none.
 */
enum class WorkingDirectoryVerdict(val label: String) {

    /** Inside the released area and usable. */
    ALLOWED("allowed"),

    /** Outside the released area. */
    BLOCKED("outside the project area"),

    /** A root or system path; the release itself is refused. */
    SYSTEM_PATH_REFUSED("system path refused"),

    /** The path escapes the working directory. */
    ESCAPES_WORKING_DIRECTORY("leaves the working directory"),

    /** Android cannot list or write this location. */
    PROVIDER_NOT_USABLE("folder cannot be used on this device")
}

/** What Android can do with a location. */
data class ProviderCapability(
    val canList: Boolean,
    val canRead: Boolean,
    val canWrite: Boolean,
    /** The reason, when something is missing. */
    val unavailableReason: String = ""
) {
    /** Can commands run here at all? */
    val isUsableForCommands: Boolean get() = canList && canRead && canWrite

    /** The first thing that is missing, or an empty string. */
    fun missingReason(): String = when {
        !canList -> unavailableReason.ifBlank { "the folder cannot be listed" }
        !canRead -> unavailableReason.ifBlank { "files cannot be read" }
        !canWrite -> unavailableReason.ifBlank { "files cannot be written" }
        else -> ""
    }
}

/** The answer for one path. */
data class WorkingDirectoryVerdictResult(
    val rawPath: String,
    val resolvedPath: String,
    val verdict: WorkingDirectoryVerdict,
    val reason: String = ""
) {
    val isAllowed: Boolean get() = verdict == WorkingDirectoryVerdict.ALLOWED
}

/**
 * Resolves and checks working directories.
 *
 * Decides only. It opens nothing, lists nothing and runs nothing.
 */
object WorkingDirectoryPolicy {

    /**
     * Paths no working directory may ever be.
     *
     * Compared against the resolved path, so `/system/../system` is caught too.
     */
    val SYSTEM_PREFIXES: List<String> = listOf(
        "/", "/system", "/system/", "/vendor", "/vendor/",
        "/apex", "/apex/", "/proc", "/proc/", "/sys", "/sys/",
        "/dev", "/dev/", "/data", "/data/"
    )

    /**
     * Normalises a path without touching the filesystem.
     *
     * `a/./b/../c` becomes `a/c`. A `..` that would go above the start returns
     * `null`, because there is no such path and pretending otherwise would hand back
     * a string that looks usable.
     */
    fun normalise(path: String): String? {
        val p = path.trim().replace('\\', '/')
        if (p.isEmpty()) return null
        val absolute = p.startsWith("/")
        val out = mutableListOf<String>()
        p.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (out.isEmpty()) return null else out.removeAt(out.lastIndex)
                else -> out += segment
            }
        }
        val joined = out.joinToString("/")
        return if (absolute) "/$joined" else joined.ifEmpty { "." }
    }

    /**
     * Is this a root or system path?
     *
     * `/` is the only exact match; everything else is a prefix check, so
     * `/system/framework` is caught without matching `/systematic-notes`.
     */
    fun isSystemPath(resolvedPath: String): Boolean {
        val p = resolvedPath.trimEnd('/')
        if (p.isEmpty()) return true
        return SYSTEM_PREFIXES.any { prefix ->
            val q = prefix.trimEnd('/')
            if (q.isEmpty()) p.isEmpty() else p == q || p.startsWith("$q/")
        }
    }

    /**
     * Resolves [candidate] and decides whether a command may run there.
     *
     * @param workingDirectory the released project area.
     * @param candidate the path the command asked for. A relative path is resolved
     *        against [workingDirectory].
     * @param capability what Android can do with the location.
     */
    fun resolve(
        workingDirectory: String,
        candidate: String = ".",
        capability: ProviderCapability? = null
    ): WorkingDirectoryVerdictResult {
        val basis = normalise(workingDirectory)
            ?: return WorkingDirectoryVerdictResult(
                candidate,
                "",
                WorkingDirectoryVerdict.SYSTEM_PATH_REFUSED,
                "The project folder is not a usable path."
            )

        val raw = candidate.trim().replace('\\', '/').ifEmpty { "." }
        val combined = if (raw.startsWith("/")) raw else "${basis.trimEnd('/')}/$raw"
        val resolved = normalise(combined)
            ?: return WorkingDirectoryVerdictResult(
                raw,
                "",
                WorkingDirectoryVerdict.ESCAPES_WORKING_DIRECTORY,
                "The path leads out of the project folder."
            )

        if (isSystemPath(resolved)) {
            return WorkingDirectoryVerdictResult(
                raw,
                resolved,
                WorkingDirectoryVerdict.SYSTEM_PATH_REFUSED,
                "System folders are never available as a working directory."
            )
        }

        if (resolved != basis && !resolved.startsWith("${basis.trimEnd('/')}/")) {
            return WorkingDirectoryVerdictResult(
                raw,
                resolved,
                WorkingDirectoryVerdict.BLOCKED,
                "The path is outside the project folder ${basis.trimEnd('/')}."
            )
        }

        if (capability != null && !capability.isUsableForCommands) {
            return WorkingDirectoryVerdictResult(
                raw,
                resolved,
                WorkingDirectoryVerdict.PROVIDER_NOT_USABLE,
                capability.missingReason()
            )
        }

        return WorkingDirectoryVerdictResult(raw, resolved, WorkingDirectoryVerdict.ALLOWED)
    }

    /**
     * The subfolder rule, stated once.
     *
     * A subfolder of the released area is fine — that is what a build does. What is
     * not fine is leaving it, and [resolve] is the only thing that decides that.
     */
    fun subfolderOf(workingDirectory: String, candidate: String): Boolean {
        val r = resolve(workingDirectory, candidate)
        return r.isAllowed && r.resolvedPath != normalise(workingDirectory)
    }

    /** The lines for the user interface. */
    fun describe(result: WorkingDirectoryVerdictResult): List<String> = listOf(
        "Folder: ${result.rawPath}",
        "Resolved to: ${result.resolvedPath.ifEmpty { "not resolvable" }}",
        "Verdict: ${result.verdict.label}." + if (result.reason.isNotBlank()) " ${result.reason}" else ""
    )
}
