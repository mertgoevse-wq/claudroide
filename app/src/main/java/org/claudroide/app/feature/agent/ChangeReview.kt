package org.claudroide.app.feature.agent

import org.claudroide.app.feature.project.ProjectExclusionPolicy
import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 089 — "Compare file changes" (Dateiänderungen vergleichen).
 *
 * Goal: the user sees exactly what an agent wants to change.
 *
 * This screen is the last point before a write, and the task states its protection
 * plainly: **reviewing is not saving.** That is enforced by absence rather than by a
 * rule: [ChangeReview] is a value type with formatting methods, and it has **no
 * method that writes, applies, commits or approves anything**. The strongest approval
 * it can express is a description of what the change *would* need — it cannot grant
 * one. [ChangeReview.requiredApproval] is computed from the change, never supplied,
 * so a caller cannot present a deletion as a routine edit.
 *
 * Two more guarantees fall out of the same review:
 *
 *  1. **New and deleted files are marked separately.** [ChangeKind] distinguishes
 *     [ChangeKind.ADDED] and [ChangeKind.DELETED] from [ChangeKind.MODIFIED], and
 *     [FileDiff.kind] is a constructor argument rather than something inferred from
 *     line counts — a file rewritten to nothing is a deletion, not an edit, and the
 *     two must not look alike on screen.
 *
 *  2. **A large change can be read in sections.** [ChangeReview.sections] splits a
 *     file at [DiffHunk] boundaries, never mid-hunk, and each section carries the
 *     total size of the whole file so a reader who sees section 3 of 9 still knows
 *     what they are looking at. Truncation is always reported: a section that was cut
 *     says so.
 *
 * File contents **do** appear here, unlike everywhere else in the app: showing the
 * change is the entire purpose of this screen. What does not happen is the
 * unmasking of a secret — a path the [ProjectExclusionPolicy] blocks never reaches
 * the review at all, and every rendered line passes through [SecretMasker].
 */
enum class ChangeKind(val label: String) {

    /** A new file the agent wants to create. */
    ADDED("new file"),

    /** An existing file the agent wants to change. */
    MODIFIED("changed file"),

    /** A file the agent wants to remove. */
    DELETED("deleted file"),

    /** A file that moved. */
    RENAMED("renamed file");

    /** Can this kind destroy data? */
    val isDestructive: Boolean get() = this == DELETED
}

/** One line inside a diff. */
enum class DiffLineType(val marker: String) {
    CONTEXT(" "),
    ADDED("+"),
    REMOVED("-")
}

/**
 * A single line in a diff.
 *
 * @property oldLineNumber 1-based in the previous version, `null` for an added line.
 * @property newLineNumber 1-based in the new version, `null` for a removed line.
 */
data class DiffLine(
    val type: DiffLineType,
    val text: String,
    val oldLineNumber: Int? = null,
    val newLineNumber: Int? = null
) {
    init {
        require(oldLineNumber == null || oldLineNumber >= 1) { "Line numbers start at 1." }
        require(newLineNumber == null || newLineNumber >= 1) { "Line numbers start at 1." }
    }
}

/**
 * A contiguous block of changed lines.
 *
 * Hunks are the unit sectioning happens on, so a hunk is never split across two
 * sections: a reader never gets half a block and has to guess.
 */
data class DiffHunk(
    val header: String,
    val lines: List<DiffLine>
) {
    init {
        require(header.isNotBlank()) { "A hunk needs a header." }
    }

    val addedCount: Int get() = lines.count { it.type == DiffLineType.ADDED }
    val removedCount: Int get() = lines.count { it.type == DiffLineType.REMOVED }
}

/**
 * The proposed change to one file.
 *
 * @property oldPath the previous path, set only for [ChangeKind.RENAMED].
 */
data class FileDiff(
    val path: String,
    val kind: ChangeKind,
    val hunks: List<DiffHunk> = emptyList(),
    val oldPath: String? = null
) {
    init {
        require(path.isNotBlank()) { "A change needs a path." }
        require(kind != ChangeKind.RENAMED || !oldPath.isNullOrBlank()) {
            "A renamed file needs its previous path."
        }
    }

    val addedCount: Int get() = hunks.sumOf { it.addedCount }
    val removedCount: Int get() = hunks.sumOf { it.removedCount }

    val totalChangedLines: Int get() = addedCount + removedCount

    /** The kind, spelled out so a new kind cannot be added without showing up here. */
    fun headerLine(): String = when (kind) {
        ChangeKind.RENAMED -> "$oldPath -> $path (renamed)"
        else -> "$path (${kind.label})"
    }
}

/** One readable slice of a file's change. */
data class DiffSection(
    val sectionNumber: Int,
    val totalSections: Int,
    val hunk: DiffHunk,
    val totalChangedLines: Int,
    val isTruncated: Boolean = false
) {
    init {
        require(sectionNumber >= 1) { "Sections are numbered from 1." }
        require(totalSections >= 1) { "There is at least one section." }
        require(sectionNumber <= totalSections) { "A section number cannot exceed the total." }
    }

    /** The heading, which always carries the position and the size of the whole file. */
    fun heading(): String {
        val position = "Part $sectionNumber of $totalSections"
        val size = "$totalChangedLines changed line(s) in this file"
        val cut = if (isTruncated) " (cut off)" else ""
        return "$position — $size — ${hunk.header}$cut"
    }
}

/**
 * Everything the agent wants to change, presented for review.
 *
 * The type cannot write. It has no method that applies, commits or approves a change,
 * and that absence is what makes "reviewing is not saving" true rather than
 * aspirational.
 *
 * @property proposedChanges what would change, one entry per file.
 * @property maxLinesPerSection how many diff lines one section may hold. A value of
 *           `0` or less means "no splitting", which is the right default for a small
 *           change and is tested as such.
 */
data class ChangeReview(
    val proposedChanges: List<FileDiff> = emptyList(),
    val maxLinesPerSection: Int = 0
) {

    init {
        require(maxLinesPerSection >= 0) { "A section limit cannot be negative." }
    }

    /** Files that would be created. */
    val addedFiles: List<FileDiff> get() = proposedChanges.filter { it.kind == ChangeKind.ADDED }

    /** Files that would be removed. */
    val deletedFiles: List<FileDiff> get() = proposedChanges.filter { it.kind == ChangeKind.DELETED }

    /** Files that would be edited. */
    val modifiedFiles: List<FileDiff> get() = proposedChanges.filter { it.kind == ChangeKind.MODIFIED }

    /** Renamed files. */
    val renamedFiles: List<FileDiff> get() = proposedChanges.filter { it.kind == ChangeKind.RENAMED }

    val totalAddedLines: Int get() = proposedChanges.sumOf { it.addedCount }
    val totalRemovedLines: Int get() = proposedChanges.sumOf { it.removedCount }
    val totalChangedLines: Int get() = totalAddedLines + totalRemovedLines
    val fileCount: Int get() = proposedChanges.size

    /**
     * The approval this change would need.
     *
     * Computed, never supplied, and deliberately strict: a change that removes a file
     * needs [RequiredApproval.IRREVERSIBLE_DELETE] whatever else it contains. A
     * deletion dressed up as an ordinary edit is the failure this prevents.
     */
    val requiredApproval: RequiredApproval
        get() = when {
            deletedFiles.isNotEmpty() -> RequiredApproval.IRREVERSIBLE_DELETE
            proposedChanges.isNotEmpty() -> RequiredApproval.FILE_CHANGE
            else -> RequiredApproval.NONE
        }

    /** Is this set of grants enough for the change? */
    fun isCoveredBy(granted: Set<RequiredApproval>): Boolean =
        proposedChanges.isEmpty() || requiredApproval in granted

    /**
     * Files the review refuses to show.
     *
     * A path the exclusion policy blocks never becomes a [FileDiff] in the first
     * place; this reports any that slipped in, so the caller can name them instead of
     * silently rendering them.
     */
    fun blockedPaths(): List<String> = proposedChanges
        .map { it.path }
        .filter { path ->
            ProjectExclusionPolicy.classify(path).decision ==
                ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET
        }

    /** Only the changes that may be shown. */
    fun showableChanges(): List<FileDiff> = proposedChanges.filter { it.path !in blockedPaths() }

    /**
     * Splits one file into readable sections.
     *
     * Splitting happens between hunks, so a section is always a whole number of
     * blocks. A hunk larger than the limit becomes its own section marked truncated:
     * showing the first part without saying so would misrepresent how much is left.
     */
    fun sectionsFor(change: FileDiff): List<DiffSection> {
        if (change.hunks.isEmpty()) return emptyList()
        if (maxLinesPerSection <= 0) {
            // Keine Grenze gesetzt heisst: keine Grenze angewandt. Nichts ist
            // abgeschnitten, auch wenn ein Block laenger ist als 0 Zeilen — sonst
            // wuerde jede ungteilte Aenderung als abgeschnitten gemeldet.
            return change.hunks.mapIndexed { index, hunk ->
                DiffSection(
                    sectionNumber = index + 1,
                    totalSections = change.hunks.size,
                    hunk = hunk,
                    totalChangedLines = change.totalChangedLines,
                    isTruncated = false
                )
            }
        }

        val groups = mutableListOf<List<DiffHunk>>()
        var current = mutableListOf<DiffHunk>()
        var currentLines = 0
        change.hunks.forEach { hunk ->
            val size = hunk.lines.size
            if (current.isNotEmpty() && currentLines + size > maxLinesPerSection) {
                groups += current
                current = mutableListOf()
                currentLines = 0
            }
            current += hunk
            currentLines += size
        }
        if (current.isNotEmpty()) groups += current

        val total = groups.size
        return groups.mapIndexed { index, group ->
            val lines = group.sumOf { it.lines.size }
            // One oversized hunk can still exceed the limit; say so instead of
            // pretending the section is complete.
            val merged = if (group.size == 1) group.first() else DiffHunk(
                header = group.first().header + " … " + group.last().header,
                lines = group.flatMap { it.lines }
            )
            DiffSection(
                sectionNumber = index + 1,
                totalSections = total,
                hunk = merged,
                totalChangedLines = change.totalChangedLines,
                isTruncated = lines > maxLinesPerSection
            )
        }
    }

    /** The rendered diff, with every line passed through [SecretMasker]. */
    fun renderedLines(): List<String> = buildList {
        add("Proposed change: $fileCount file(s), $totalAddedLines added, $totalRemovedLines removed.")
        add("This is a review. Nothing has been written.")
        val blocked = blockedPaths()
        if (blocked.isNotEmpty()) {
            add("Not shown, because these may hold credentials: ${blocked.joinToString(", ")}")
        }
        showableChanges().forEach { change ->
            add("")
            add(change.headerLine())
            val sections = sectionsFor(change)
            if (sections.isEmpty()) {
                add("  (no line detail)")
            }
            sections.forEach { section ->
                if (sections.size > 1) {
                    add("  ${section.heading()}")
                } else if (section.isTruncated) {
                    add("  ${section.heading()}")
                }
                section.hunk.lines.forEach { line ->
                    add("  ${line.type.marker}${SecretMasker.redact(line.text)}")
                }
            }
        }
    }
}
