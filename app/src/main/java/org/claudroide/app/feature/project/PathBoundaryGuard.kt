package org.claudroide.app.feature.project

/**
 * Decides whether a path may be read, written or executed inside a project.
 *
 * Task 120 is a security gate. Its central rule comes from the task brief:
 * **the check must be performed on the real target, not only on the string that
 * was written.** A path is therefore resolved against the project root first, and
 * a caller-supplied real path — the result of following symlinks — is compared
 * against the real project root, not the logical one. Any disagreement means the
 * path points somewhere else and is refused.
 *
 * Invariants:
 *  - Nothing outside the project root is reachable, whatever the encoding.
 *  - Traversal (`../`), absolute paths, NUL bytes, and Windows-style separators
 *    cannot escape.
 *  - A symbolic link inside the project that points outside is refused, because
 *    the real target is outside. This is the difference between checking the
 *    written path and checking where it actually lands.
 *  - Rejection is explained in plain German, naming the reason, so the user can
 *    act on it rather than just seeing a failure.
 *
 * The functions are pure: the caller passes the project root and any resolved
 * real path as plain strings. That keeps the whole decision testable on the JVM
 * without touching a real file system, and forces the caller to do the actual
 * `realpath` resolution explicitly rather than letting it be assumed.
 */
object PathBoundaryGuard {

    /** What the guard concluded about a path. */
    enum class PathDecision {
        /** Inside the project, and the real target agrees. Safe to use. */
        ALLOWED,

        /** Escapes the project root via traversal or an absolute path. */
        OUTSIDE_PROJECT,

        /** Contains a NUL byte or a control character. Not a usable path. */
        MALFORMED,

        /** The written path is inside, but the real target is not. */
        SYMLINK_ESCAPE,

        /** The project root itself is empty or unusable. */
        NO_ROOT
    }

    /**
     * Why a path was refused, in language the user can act on.
     */
    data class PathVerdict(
        val decision: PathDecision,
        val message: String
    ) {
        val isAllowed: Boolean get() = decision == PathDecision.ALLOWED
    }

    /**
     * The name of a project-relative path, used in messages and for the
     * single-segment checks.
     */
    private const val SELF = "."

    private const val PARENT = ".."

    /**
     * Checks [candidate] against [projectRoot].
     *
     * @param candidate        Path as written by the caller. May be relative or absolute.
     * @param projectRoot      The project root. Must be non-blank.
     * @param resolvedRealPath The real target after following symlinks, or null when the
     *                         caller could not resolve it. When supplied it is the
     *                         authoritative check: a null value is treated as "unresolved"
     *                         and the logical check alone decides, because a file that
     *                         does not exist yet has no real target.
     * @param realRoot         The real target of [projectRoot] after following symlinks,
     *                         or null when unknown. When both real paths are known they
     *                         must agree on the prefix.
     */
    fun check(
        candidate: String,
        projectRoot: String,
        resolvedRealPath: String? = null,
        realRoot: String? = null
    ): PathVerdict {
        val root = projectRoot.trim()
        if (root.isEmpty()) {
            return PathVerdict(
                PathDecision.NO_ROOT,
                "Es ist kein Projektordner festgelegt. Der Pfad kann nicht geprüft werden."
            )
        }

        if (candidate.indexOf('\u0000') >= 0 || candidate.any { it.isISOControl() && it != '\t' }) {
            return PathVerdict(
                PathDecision.MALFORMED,
                "Der Pfad enthält unzulässige Zeichen und wurde abgelehnt."
            )
        }

        val rootNormal = normalise(root)
        val candidateNormal = normalise(candidate)

        if (candidateNormal.isEmpty() && candidate.trim().replace('\\', '/').trim('/') != SELF) {
            return PathVerdict(
                PathDecision.MALFORMED,
                "Der Pfad ist leer. Es wurde nichts geöffnet."
            )
        }

        val absoluteCandidate = when {
            candidateNormal.startsWith("/") -> candidateNormal
            else -> "$rootNormal/$candidateNormal"
        }

        // The real target is checked FIRST and decides. A path can look perfectly
        // inside and still land outside through a symlink, so when the caller could
        // resolve the target, that resolution is the authoritative answer — including
        // when the written path also happens to traverse. Reporting "outside" for a
        // path whose real target is outside would hide the actual mechanism.
        if (realRoot != null && resolvedRealPath != null) {
            val realRootNormal = normalise(realRoot)
            val realCandidate = resolveWithinRoot(normalise(resolvedRealPath), realRootNormal)
            if (realCandidate == null) {
                return PathVerdict(
                    PathDecision.SYMLINK_ESCAPE,
                    "„$candidate“ zeigt auf ein Ziel außerhalb des Projektordners " +
                        "(Verweis auf einen anderen Ordner). Der Zugriff wurde abgelehnt."
                )
            }
        }

        val resolved = resolveWithinRoot(absoluteCandidate, rootNormal)
            ?: return PathVerdict(
                PathDecision.OUTSIDE_PROJECT,
                "„$candidate“ liegt außerhalb des Projektordners. " +
                    "Dort wird nicht gelesen und nichts geschrieben."
            )

        return PathVerdict(PathDecision.ALLOWED, "")
    }

    /**
     * Resolves [absolute] against [rootNormal] segment by segment, collapsing `.` and
     * `..` on the way. Returns the normalised absolute path when it stays at or below
     * [rootNormal], or null when any `..` step would climb above the root.
     *
     * Returning null rather than a clamped path is deliberate: clamping would silently
     * rewrite the user's path into a different, still-existing file.
     */
    private fun resolveWithinRoot(absolute: String, rootNormal: String): String? {
        val rootSegments = rootNormal.split('/').filter { it.isNotEmpty() }
        val segments = mutableListOf<String>()

        for (segment in absolute.split('/')) {
            when {
                segment.isEmpty() || segment == SELF -> Unit
                segment == PARENT -> {
                    // Climbing above the root is the escape itself.
                    if (segments.size <= rootSegments.size) return null
                    segments.removeAt(segments.size - 1)
                }
                else -> segments.add(segment)
            }
        }

        if (segments.size < rootSegments.size) return null
        for (index in rootSegments.indices) {
            if (segments[index] != rootSegments[index]) return null
        }
        return "/" + segments.joinToString("/")
    }

    /**
     * Normalises a path without touching the file system: backslashes become
     * forward slashes, duplicate separators collapse, `.` segments are dropped.
     * Trailing `/` is removed so `a/b` and `a/b/` are the same path.
     *
     * Deliberately does NOT resolve `..` — that is [resolveWithinRoot]'s job, and
     * doing it here would hide the very thing this gate exists to detect.
     */
    private fun normalise(path: String): String =
        path.trim()
            .replace('\\', '/')
            .split('/')
            .filter { it.isNotEmpty() && it != SELF }
            .joinToString("/")
            .let { if (path.trim().startsWith("/")) "/$it" else it }
}
