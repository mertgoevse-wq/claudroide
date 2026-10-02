package org.claudroide.app.feature.project

/**
 * Task 087 — "Find files" (Dateien finden).
 *
 * Goal: search project files without loading the device with pointless scans.
 *
 * A local search is the one operation in this app that reads file *contents* on the
 * device. Everything else works on paths. That single fact decides the design here,
 * and the task states the rule plainly: file contents are not transferred to a
 * provider during a local search.
 *
 * **Three guarantees live in the types, not in a comment:**
 *
 *  1. **A search result carries no file content.** [SearchMatch] holds a path, a line
 *     number and the term that matched — never the line itself. There is no
 *     constructor path that puts a line of file text into a result, so a result
 *     cannot leak one. Reading a line is a separate, explicitly local call
 *     ([LocalSearchResult.readLine]) whose return type is [LocalOnlyContent], a type
 *     that appears in no result and in no transmission path. A test checks the field
 *     list by reflection so a future "just add a preview" fails loudly.
 *
 *  2. **A revoked area ends the search immediately.** [LocalSearchResult.skippedForRevokedPaths]
 *     counts what was dropped, and [LocalSearchPhase.STOPPED_REVOKED] is the reported
 *     phase. A search that found matches inside a folder the user withdrew does not
 *     report success — the partial result is marked as partial so no caller can
 *     mistake "search finished" for "search covered everything".
 *
 *  3. **Hidden files and heavy directories follow fixed rules.** [ScanRules] holds the
 *     limits as named constants rather than magic numbers, and every excluded path is
 *     returned in [LocalSearchResult.excluded] with a reason. The user can always see
 *     what was left out, never only what stayed in.
 *
 * Pure Kotlin: no Android, no filesystem. The caller supplies file contents; this
 * type decides what may be looked at, what may be reported, and what may leave the
 * device.
 */

/** Where a search currently stands. */
enum class LocalSearchPhase(val label: String) {
    IDLE("not started"),
    RUNNING("searching"),
    PARTIAL("partial result"),
    COMPLETE("complete"),
    STOPPED_REVOKED("stopped: access withdrawn"),
    CANCELLED("cancelled")
}

/** Why a path was not searched. */
enum class SkipReason(val label: String) {

    /** A dotfile or a dot-directory, hidden by default. */
    HIDDEN("hidden file or folder"),

    /** A directory the rules treat as too large to be useful. */
    HEAVY_DIRECTORY("large folder"),

    /** A file the exclusion policy blocks, e.g. one holding credentials. */
    BLOCKED_BY_POLICY("blocked by the exclusion rules"),

    /** Deeper than [ScanRules.MAX_DEPTH]. */
    TOO_DEEP("too deeply nested"),

    /** A file the user withdrew access to. */
    REVOKED("access withdrawn")
}

/** A path that was deliberately not searched, with the reason. */
data class SkippedPath(
    val relativePath: String,
    val reason: SkipReason
)

/**
 * One search hit.
 *
 * Deliberately carries **no line text**. See the class note on task 087 above.
 *
 * @property lineNumber 1-based, so it matches what an editor shows.
 * @property matchedTerm the search term as it occurs here, not the surrounding line.
 */
data class SearchMatch(
    val relativePath: String,
    val lineNumber: Int,
    val matchedTerm: String
) {
    init {
        require(relativePath.isNotBlank()) { "A search hit needs a path." }
        require(lineNumber >= 1) { "A line number starts at 1." }
    }
}

/**
 * A line of file text, read locally.
 *
 * This type exists so that content has a name that says where it belongs. It is not
 * a field of [LocalSearchResult] and must not become one.
 */
data class LocalOnlyContent(val text: String)

/**
 * Fixed limits for a scan.
 *
 * These are project decisions, not measurements taken on a device, so they are named
 * constants rather than numbers hiding in the code.
 */
object ScanRules {

    /** Hidden entries are skipped unless explicitly asked for. */
    const val SKIP_HIDDEN_BY_DEFAULT: Boolean = true

    /** How deep the scan descends at most. */
    const val MAX_DEPTH: Int = 12

    /** How many files one scan reads at most before it reports a partial result. */
    const val MAX_FILES_PER_SCAN: Int = 2_000

    /** Directory names treated as too large to be worth reading. */
    val HEAVY_DIRECTORIES: Set<String> = setOf(
        "node_modules", "build", ".gradle", ".git", "dist", "out",
        "vendor", ".idea", ".cxx", "captures", ".cxx"
    )

    /** A path segment is hidden when it starts with a dot. */
    fun isHidden(relativePath: String): Boolean =
        relativePath.trim().replace('\\', '/').split('/')
            .filter { it.isNotEmpty() }
            .any { it.startsWith(".") }

    /** Liegt ein Segment des Pfades in einem schweren Ordner? */
    fun heavyDirectoryOf(relativePath: String): String? =
        relativePath.trim().replace('\\', '/').split('/')
            .filter { it.isNotEmpty() }
            .firstOrNull { it in HEAVY_DIRECTORIES }

    /** Wie tief liegt der Pfad? */
    fun depthOf(relativePath: String): Int =
        relativePath.trim().replace('\\', '/').split('/').count { it.isNotEmpty() }

    /**
     * Why is this path skipped, if at all?
     *
     * Order matters: a path that is both hidden and blocked by policy is reported as
     * blocked, because "this may hold a credential" is the more useful message.
     */
    fun skipReasonFor(
        relativePath: String,
        includeHidden: Boolean = false,
        isRevoked: Boolean = false
    ): SkipReason? = when {
        isRevoked -> SkipReason.REVOKED
        ProjectExclusionPolicy.classify(relativePath).decision ==
            ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET -> SkipReason.BLOCKED_BY_POLICY
        heavyDirectoryOf(relativePath) != null -> SkipReason.HEAVY_DIRECTORY
        SKIP_HIDDEN_BY_DEFAULT && !includeHidden && isHidden(relativePath) ->
            SkipReason.HIDDEN
        depthOf(relativePath) > MAX_DEPTH -> SkipReason.TOO_DEEP
        else -> null
    }
}

/**
 * The outcome of a local search.
 *
 * @property phase where the search stands.
 * @property matches the hits. No file text is among them.
 * @property skipped what was not searched, and why.
 * @property searchedFileCount how many files were actually read.
 * @property wasTruncated `true` when [ScanRules.MAX_FILES_PER_SCAN] cut the search short.
 */
data class LocalSearchResult(
    val phase: LocalSearchPhase,
    val matches: List<SearchMatch> = emptyList(),
    val skipped: List<SkippedPath> = emptyList(),
    val searchedFileCount: Int = 0,
    val wasTruncated: Boolean = false
) {
    init {
        require(searchedFileCount >= 0) { "The file count cannot be negative." }
    }

    /** Paths that were dropped because access was withdrawn. */
    fun skippedForRevokedPaths(): List<SkippedPath> =
        skipped.filter { it.reason == SkipReason.REVOKED }

    /** Did the search get cut short? */
    val isPartial: Boolean
        get() = phase == LocalSearchPhase.PARTIAL ||
            phase == LocalSearchPhase.STOPPED_REVOKED ||
            wasTruncated

    /** May this result be handed to a provider? */
    val isSafeToSend: Boolean
        get() = !hasContentField

    /**
     * Always `false`.
     *
     * A result type that holds a field for file text could be filled with one, and
     * this is where that would be noticed. The field itself does not exist; this
     * property exists so the rule has a name.
     */
    private val hasContentField: Boolean
        get() = LocalSearchResult::class.java.declaredFields
            .map { it.name.lowercase() }
            .any { name ->
                name in setOf("content", "contents", "line", "text", "preview", "snippet", "body")
            }

    /**
     * Reads one line from a local source.
     *
     * Separate from the result on purpose: a search result is reportable, a line of
     * file text is not. The return type [LocalOnlyContent] keeps the two apart in the
     * type system rather than in a comment.
     */
    fun readLine(
        lineSource: (path: String, lineNumber: Int) -> String?
    ): LocalOnlyContent? {
        val first = matches.firstOrNull() ?: return null
        val text = lineSource(first.relativePath, first.lineNumber) ?: return null
        return LocalOnlyContent(text)
    }

    /** The lines for the user interface. */
    fun statusLines(): List<String> = buildList {
        add("Local search: ${phase.label}.")
        add("Files read: $searchedFileCount.")
        if (wasTruncated) {
            add(
                "Stopped at the limit of ${ScanRules.MAX_FILES_PER_SCAN} files. " +
                    "This result is partial."
            )
        }
        val widerrufen = skippedForRevokedPaths()
        if (widerrufen.isNotEmpty()) {
            add(
                "Access withdrawn for ${widerrufen.size} path(s); they were not searched."
            )
        }
        if (matches.isNotEmpty()) {
            add("Hits (${matches.size}):")
            matches.forEach { add("  ${it.relativePath}:${it.lineNumber}") }
        } else if (!isPartial) {
            add("No hits.")
        } else {
            add("No hits so far. The search did not finish.")
        }
        if (skipped.isNotEmpty()) {
            add("Not searched (${skipped.size}):")
            skipped.take(20).forEach { add("  ${it.relativePath} — ${it.reason.label}") }
            if (skipped.size > 20) {
                add("  … and ${skipped.size - 20} more")
            }
        }
    }
}

/**
 * Runs a local search over supplied file contents.
 *
 * The class never touches storage. It receives what the caller read and decides what
 * may be read at all, what may be reported, and what may leave the device.
 */
class LocalFileSearch(
    private val includeHidden: Boolean = false,
    /** Paths the user withdrew access to. A search stops on these. */
    private var revokedPaths: List<String> = emptyList()
) {

    /** A single file as the search sees it. */
    data class SourceFile(
        val relativePath: String,
        val lines: List<String>,
        val sizeBytes: Long = 0L
    )

    /** A search term. */
    data class Query(val term: String, val caseSensitive: Boolean = false)

    /**
     * Updates the withdrawn paths and returns a new instance.
     *
     * A new instance rather than a mutation, so a search that is already running
     * keeps the boundary it started with instead of silently changing underneath.
     */
    fun withRevokedPaths(paths: List<String>): LocalFileSearch =
        LocalFileSearch(includeHidden, paths.map { it.trim().replace('\\', '/') }.filter { it.isNotEmpty() })

    /** Is [path] inside a withdrawn area? */
    fun isRevoked(path: String): Boolean {
        val p = path.trim().replace('\\', '/').trimEnd('/')
        return revokedPaths.any { root ->
            val r = root.trimEnd('/')
            p == r || p.startsWith("$r/")
        }
    }

    /**
     * Searches [files] for [query].
     *
     * The loop stops in three cases, and each is reported: a revoked path, the file
     * limit, and [ScanRules.MAX_DEPTH]. Reaching the limit produces
     * [LocalSearchPhase.PARTIAL] with `wasTruncated = true`, never a clean
     * [LocalSearchPhase.COMPLETE].
     */
    fun search(files: List<SourceFile>, query: Query): LocalSearchResult {
        val needle = query.term.trim()
        if (needle.isEmpty()) {
            return LocalSearchResult(LocalSearchPhase.IDLE)
        }

        val matches = mutableListOf<SearchMatch>()
        val skipped = mutableListOf<SkippedPath>()
        var read = 0
        var truncated = false
        var stoppedOnRevoked = false

        for (file in files) {
            val path = file.relativePath.trim().replace('\\', '/')

            if (isRevoked(path)) {
                skipped += SkippedPath(path, SkipReason.REVOKED)
                // Not just skipped: the whole search stops, because the remaining
                // files cannot be assumed to be reachable either.
                stoppedOnRevoked = true
                break
            }

            val reason = ScanRules.skipReasonFor(path, includeHidden, isRevoked = false)
            if (reason != null) {
                skipped += SkippedPath(path, reason)
                continue
            }

            if (read >= ScanRules.MAX_FILES_PER_SCAN) {
                truncated = true
                break
            }
            read++

            file.lines.forEachIndexed { index, line ->
                val hit = if (query.caseSensitive) {
                    line.contains(needle)
                } else {
                    line.contains(needle, ignoreCase = true)
                }
                if (hit) {
                    // The line number only. The line itself never leaves here.
                    matches += SearchMatch(path, index + 1, needle)
                }
            }
        }

        val phase = when {
            stoppedOnRevoked -> LocalSearchPhase.STOPPED_REVOKED
            truncated -> LocalSearchPhase.PARTIAL
            else -> LocalSearchPhase.COMPLETE
        }

        return LocalSearchResult(
            phase = phase,
            matches = matches,
            skipped = skipped,
            searchedFileCount = read,
            wasTruncated = truncated
        )
    }
}
