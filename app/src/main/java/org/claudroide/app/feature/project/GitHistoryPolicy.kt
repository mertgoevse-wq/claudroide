package org.claudroide.app.feature.project

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 103 — "Git history" (Git-Verlauf).
 *
 * Goal: the user can understand what was saved in the project and when.
 *
 * A history view is the one place in this app that shows text the app did not just
 * write, and it shows it for the whole past rather than for the current change. The
 * task names two protections, and they are structural:
 *
 *  1. **Commit and upload state are shown separately.** A commit that exists only
 *     on the device is not the same fact as one that reached the remote repository,
 *     and the two must never collapse into a single "saved" label.
 *     [CommitUploadState] has three distinct values and [GitHistoryEntry.savedLine]
 *     spells out which one applies. There is no combined boolean anywhere.
 *
 *  2. **Secrets never appear unmasked in a diff view.** Every line goes through
 *     [SecretMasker] at the point it is rendered — not at the point it is stored, and
 *     not at the point it is sent to a screen. [GitHistoryEntry.renderedDiffLines]
 *     returns already-masked text, so there is no variant of this class that yields
 *     an unmasked line.
 *
 * History stays bound to the project. [GitHistory] carries the project it belongs to
 * and has no method that shares, exports or transmits it; a test checks the class
 * surface for that, because "we would only share it if the user asked" is easier to
 * say than to enforce.
 */

/** Where a commit exists. */
enum class CommitUploadState(val label: String, val isOnDevice: Boolean, val isUploaded: Boolean) {

    /** Only in the working copy; there is no commit yet. */
    UNCOMMITTED("not committed", true, false),

    /** Committed on the device, not uploaded. */
    COMMITTED_LOCALLY("committed on the device, not uploaded", true, false),

    /** Committed on the device and pushed. */
    COMMITTED_AND_PUSHED("committed and uploaded", true, true);

    /** Is the state fully settled? */
    val isFullySaved: Boolean get() = isOnDevice && isUploaded
}

/** One line of one commit's diff. */
data class HistoryDiffLine(
    val path: String,
    val lineNumber: Int,
    val type: DiffLineSign = DiffLineSign.ADDED,
    /** The raw line as git reported it. It is masked on the way out, never on the way in. */
    val rawText: String = ""
) {
    init {
        require(lineNumber >= 1) { "Line numbers start at 1." }
    }
}

/** Whether a history line was added or removed. */
enum class DiffLineSign(val marker: String) {
    ADDED("+"),
    REMOVED("-")
}

/** One commit in the history. */
data class GitHistoryEntry(
    val commitId: String,
    val summary: String,
    val authorLabel: String,
    val committedAtEpochMillis: Long,
    val uploadState: CommitUploadState,
    val changedPaths: List<String> = emptyList(),
    val diffLines: List<HistoryDiffLine> = emptyList()
) {
    init {
        require(commitId.isNotBlank()) { "A commit needs an id." }
        require(summary.isNotBlank()) { "A commit needs a summary." }
    }

    /**
     * The commit line, with the upload state spelled out.
     *
     * A commit that is not uploaded says so here. A single word like "saved" would
     * be true of both states and therefore useless for telling them apart.
     */
    fun savedLine(): String = "$commitId — ${uploadState.label}"

    /** The number of changed paths, counted once. */
    val changedPathCount: Int get() = changedPaths.distinct().size

    /**
     * The diff, with every line masked.
     *
     * Masking happens here, on the way out. Storing it masked would lose the real
     * text; not masking it would show a credential that has been sitting in the
     * repository for a hundred commits.
     */
    fun renderedDiffLines(): List<String> = diffLines.map { line ->
        "${line.type.marker} ${line.path}:${line.lineNumber} ${SecretMasker.redact(line.rawText)}"
    }
}

/**
 * A project's history.
 *
 * @property projectId the project this history belongs to. The type does not
 *           transmit anywhere; the project binding is what the sharing rule hangs on.
 */
data class GitHistory(
    val projectId: String,
    val entries: List<GitHistoryEntry> = emptyList(),
    val isSharedExternally: Boolean = false
) {
    init {
        require(projectId.isNotBlank()) { "A history needs a project." }
    }

    val commitCount: Int get() = entries.size

    /** Commits that exist only on the device. */
    val localOnlyCommits: List<GitHistoryEntry>
        get() = entries.filter { it.uploadState == CommitUploadState.COMMITTED_LOCALLY }

    /** Commits that reached the remote repository. */
    val uploadedCommits: List<GitHistoryEntry>
        get() = entries.filter { it.uploadState == CommitUploadState.COMMITTED_AND_PUSHED }

    /** Is the history in step with the remote repository? */
    val isInSyncWithRemote: Boolean
        get() = entries.isNotEmpty() && localOnlyCommits.isEmpty()

    /** The lines for the user interface. */
    fun displayLines(): List<String> = buildList {
        add("History of $projectId: $commitCount commit(s).")
        when {
            entries.isEmpty() -> add("  Nothing has been committed yet.")
            isInSyncWithRemote -> add("  All commits are uploaded.")
            else -> add(
                "  ${localOnlyCommits.size} commit(s) exist only on this device and are " +
                    "not on the remote repository."
            )
        }
        entries.forEach { entry ->
            add("  ${entry.savedLine()}")
            add("    ${entry.summary} (${entry.authorLabel})")
            if (entry.changedPathCount > 0) {
                add("    Changed: ${entry.changedPathCount} file(s).")
            }
            if (!entry.uploadState.isUploaded) {
                add("    Not on the remote repository.")
            }
        }
        if (isSharedExternally) {
            add("! This history has been shared outside the project at the user's request.")
        }
    }
}
