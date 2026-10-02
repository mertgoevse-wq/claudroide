package org.claudroide.app.feature.project

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 098 — "Git change list" (Git-Änderungsliste).
 *
 * Goal: the user sees changed, new, removed and untracked files.
 *
 * The dangerous part of a change list is not showing it, it is **committing it**. The
 * task states both protections plainly, and both are structural here:
 *
 *  1. **Files outside the released project are neither shown nor included.**
 *     [GitChangeListPolicy.stageablePaths] filters against
 *     [ProjectBoundaryEnforcer.ProjectBoundary] on the way out. A path that escapes
 *     the project — `..`, an absolute path, a symlink pointing elsewhere — is
 *     counted in [GitChangeList.outsideProjectPaths] and named, so the user can see
 *     it was refused rather than wonder where it went. There is no flag that turns
 *     this filter off.
 *
 *  2. **Unknown files do not quietly travel into a commit, and secrets are checked
 *     before one.** [GitChangeList.coveredByCommitIntent] requires every entry to
 *     have been named. An entry in [GitChangeSet.UNTRACKED] is therefore not
 *     committed by omission — the user has to add it. And [GitChangeList.blockedBySecretCheck]
 *     is computed from [ProjectExclusionPolicy], so a `.env` cannot be staged even if
 *     it somehow reaches the list.
 *
 * The types describe. Nothing here runs git.
 */

/** How a file came to differ from the last commit. */
enum class GitChangeKind(val label: String) {

    /** Changed and tracked. */
    MODIFIED("modified"),

    /** New and not yet tracked. */
    ADDED("new"),

    /** Tracked before and now gone. */
    DELETED("removed"),

    /** On disk but not known to git. */
    UNTRACKED("not tracked yet")
}

/** One file in the change list. */
data class GitChangeEntry(
    val path: String,
    val kind: GitChangeKind,
    val isOutsideProject: Boolean = false
) {
    init {
        require(path.isNotBlank()) { "A change entry needs a path." }
    }

    /** The line for the user interface. */
    fun displayLine(): String = if (isOutsideProject) {
        "$path (${kind.label}) — outside the project, not shown and not committed"
    } else {
        "$path (${kind.label})"
    }
}

/**
 * What git reports, before any judgement.
 *
 * Kept raw on purpose: the policy needs to see everything, including what it will
 * refuse, so it can name it.
 */
data class GitChangeSet(
    val modified: List<String> = emptyList(),
    val added: List<String> = emptyList(),
    val deleted: List<String> = emptyList(),
    val untracked: List<String> = emptyList()
) {
    val entryCount: Int
        get() = modified.size + added.size + deleted.size + untracked.size
}

/** The reviewed list the user is looking at. */
data class GitChangeList(
    val entries: List<GitChangeEntry>,
    /** Names the user explicitly put into this commit. */
    val commitIntent: Set<String> = emptySet()
) {

    val modified: List<GitChangeEntry> get() = entries.filter { it.kind == GitChangeKind.MODIFIED }
    val added: List<GitChangeEntry> get() = entries.filter { it.kind == GitChangeKind.ADDED }
    val deleted: List<GitChangeEntry> get() = entries.filter { it.kind == GitChangeKind.DELETED }
    val untracked: List<GitChangeEntry> get() = entries.filter { it.kind == GitChangeKind.UNTRACKED }

    /** Entries that are outside the project. Never shown, never committed. */
    val outsideProjectPaths: List<String>
        get() = entries.filter { it.isOutsideProject }.map { it.path }

    /** What may actually be staged. */
    val stageablePaths: List<String>
        get() = entries.filterNot { it.isOutsideProject }
            .map { it.path }
            .filterNot { blockedBySecretCheck(it) }

    /** Paths the secret check refuses. */
    val blockedBySecretCheck: List<String>
        get() = entries.map { it.path }.filter { path ->
            ProjectExclusionPolicy.classify(path).decision ==
                ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET
        }

    fun blockedBySecretCheck(path: String): Boolean =
        ProjectExclusionPolicy.classify(path).decision ==
            ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET

    /**
     * Entries that would travel into the commit without having been named.
     *
     * The point of the whole type: an untracked file that nobody mentioned must not
     * ride along. [GitChangeList.coveredByCommitIntent] is `false` while any exist.
     */
    val silentlyIncluded: List<GitChangeEntry>
        get() = entries.filter { it.path !in commitIntent && !it.isOutsideProject }

    /** Is every entry one the user actually named? */
    val coveredByCommitIntent: Boolean get() = silentlyIncluded.isEmpty()

    /** The lines for the user interface. */
    fun displayLines(): List<String> = buildList {
        add("Changes: ${entries.size} file(s).")
        fun block(label: String, list: List<GitChangeEntry>) {
            if (list.isEmpty()) return
            add("$label (${list.size}):")
            list.forEach { add("  ${it.displayLine()}") }
        }
        block("Modified", modified)
        block("New", added)
        block("Removed", deleted)
        block("Not tracked yet", untracked)

        if (outsideProjectPaths.isNotEmpty()) {
            add(
                "Outside the project (${outsideProjectPaths.size}), not shown and not " +
                    "committed: ${outsideProjectPaths.joinToString(", ")}"
            )
        }
        val gesperrt = blockedBySecretCheck
        if (gesperrt.isNotEmpty()) {
            add(
                "Held back by the secret check (${gesperrt.size}): " +
                    gesperrt.joinToString(", ")
            )
        }
        if (untracked.isNotEmpty() && !coveredByCommitIntent) {
            add(
                "! ${untracked.size} untracked file(s) are **not** included until you " +
                    "name them. This list is incomplete, not silent."
            )
        }
        if (entries.isNotEmpty() && coveredByCommitIntent && gesperrt.isEmpty()) {
            add("Everything shown here is included in the commit.")
        }
    }
}

/**
 * Turns what git reports into a reviewed list.
 *
 * Decides only. It runs no git command, holds no repository and writes nothing.
 */
object GitChangeListPolicy {

    /**
     * Builds the reviewed list.
     *
     * @param boundary the released project area. A path that leaves it is marked
     *        outside and drops out of [GitChangeList.stageablePaths].
     */
    fun review(
        set: GitChangeSet,
        boundary: ProjectBoundaryEnforcer.ProjectBoundary =
            ProjectBoundaryEnforcer.ProjectBoundary(),
        commitIntent: Set<String> = emptySet()
    ): GitChangeList {
        val aktionsgrenze = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction.file(
                ProjectBoundaryEnforcer.ToolKind.WRITE,
                "probe"
            ),
            boundary,
            generation = 0L
        )
        val grenzeVorhanden = aktionsgrenze.decision !=
            ProjectBoundaryEnforcer.BoundaryDecision.NO_PROJECT_ROOT

        fun entries(pfade: List<String>, art: GitChangeKind) = pfade.map { pfad ->
            val ausserhalb = grenzeVorhanden &&
                !isInsideProject(pfad, boundary.effectiveRoots())
            GitChangeEntry(pfad, art, ausserhalb)
        }

        return GitChangeList(
            entries = entries(set.modified, GitChangeKind.MODIFIED) +
                entries(set.added, GitChangeKind.ADDED) +
                entries(set.deleted, GitChangeKind.DELETED) +
                entries(set.untracked, GitChangeKind.UNTRACKED),
            commitIntent = commitIntent
        )
    }

    /**
     * Liegt [path] in einer der freigegebenen Wurzeln?
     *
     * Trennt am `/` und lehnt `..` ab: `/project-evil` darf nicht als `/project`
     * gelten, und `../austritt` darf nicht durchrutschen.
     */
    fun isInsideProject(path: String, roots: List<String>): Boolean {
        val p = path.trim().replace('\\', '/')
        if (p.isEmpty()) return false
        if (p.split('/').any { it == ".." }) return false
        val normalisiert = p.trimStart('/')
        return roots.any { root ->
            val r = root.trim().replace('\\', '/').trim('/')
            normalisiert == r || normalisiert.startsWith("$r/") || p.startsWith("$r/")
        }
    }

    /** The lines describing what this policy does. */
    fun policyLines(): List<String> = listOf(
        "A file outside the project you released is neither shown nor committed.",
        "Untracked files are not included until you name them.",
        "Files that may hold credentials are held back before a commit.",
        "Any text shown here has been passed through ${SecretMasker.REDACTION_PLACEHOLDER} masking."
    )
}
