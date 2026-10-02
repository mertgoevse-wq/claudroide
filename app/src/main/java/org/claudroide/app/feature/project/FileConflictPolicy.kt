package org.claudroide.app.feature.project

/**
 * Task 091 — "File conflicts" (Datei-Konflikte).
 *
 * Changes must not be written over external or in-between file changes.
 *
 * The result is a version comparison, a conflict message and a controlled way to
 * merge. The protection is: no silent overwrites, no automatic data loss.
 *
 * The whole task turns on one idea: **the app must know what the file looked
 * when it read it.** An edit is only safe to write if the file is still what it
 * was. That is what [FileConflictPolicy.check] compares — not the new content
 * against the old content, but the *current* file against the *base* the agent
 * started from. If the user edited the file in another app while the agent was
 * working, the base no longer matches and the write stops.
 *
 * Two deliberate choices:
 *
 *  - **[MergeChoice.KEEP_BOTH] exists.** A conflict is not forced to a winner.
 *    The user may want both versions, and a tool that only offers "mine" and
 *    "yours" forces a loss.
 *  - **[MergeChoice] has no automatic entry.** There is no `AUTO` value, so
 *    there is no path by which a merge happens without a person choosing. The
 *    task's "no automatic data loss" is a property of the type, not a setting.
 */
enum class MergeChoice(val label: String) {

    /** Write the agent's version over the current one. */
    TAKE_AGENT("keep the version ClauDroide prepared"),

    /** Leave the file exactly as it is now. */
    KEEP_CURRENT("keep the file as it is now"),

    /** Write both, the current one under a second name. Nothing is lost. */
    KEEP_BOTH("keep both — the prepared version is written next to it"),

    /** Stop and let the user edit by hand. */
    ASK_LATER("neither — I will look at it myself")
}

/** What happened when the app checked the file before writing. */
enum class ConflictState(val label: String, val blocksWrite: Boolean) {

    /** The file is still what the agent read. Writing is safe. */
    NO_CONFLICT("the file has not changed since it was read", false),

    /**
     * The file changed while the agent worked.
     *
     * The write stops. Not a warning — a stop, because the alternative is
     * writing over something the user did.
     */
    CONFLICT("the file changed while ClauDroide was working", true),

    /** The file is not there any more. */
    FILE_VANISHED("the file is gone", true),

    /** The file was there and is now something else — e.g. a folder. */
    TYPE_CHANGED("the path is now something other than the file it was", true)
}

/** A change in one file. */
data class ProposedChange(
    val path: String,
    /** The content the file had when it was read. */
    val baseContent: String,
    /** The content the agent wants to write. */
    val proposedContent: String,
    /** The content the file has right now, or null when it is gone. */
    val currentContent: String?,
    /** Does the file exist right now? */
    val currentExists: Boolean = true
) {
    /** Has the agent actually changed anything? */
    val changesAnything: Boolean get() = baseContent != proposedContent
}

/** The result of checking one change. */
data class ConflictCheck(
    val change: ProposedChange,
    val state: ConflictState,
    /** Lines that differ between the base and the current file. */
    val differingLines: List<String>
) {
    val mayWrite: Boolean get() = !state.blocksWrite

    /**
     * The differences, shown before merging.
     *
     * Both sides are quoted: what the file says now, and what the agent
     * prepared. A conflict message that names only one side leaves the user
     * guessing what they would be giving up.
     */
    fun differenceLines(): List<String> = buildList {
        add("File: ${change.path}")
        add(state.label + ".")
        add("The file says now:")
        (change.currentContent ?: "(the file is gone)").lines()
            .take(DIFFERENCE_LINE_LIMIT)
            .forEach { add("  | $it") }
        add("ClauDroide prepared:")
        change.proposedContent.lines()
            .take(DIFFERENCE_LINE_LIMIT)
            .forEach { add("  > $it") }
        if (differingLines.isNotEmpty()) {
            add("These lines differ:")
            differingLines.take(DIFFERENCE_LINE_LIMIT).forEach { add("  * $it") }
        }
        if (state.blocksWrite) {
            add("Nothing has been written. Choose how to continue.")
        }
    }

    companion object {
        /** How many lines of each side are shown. */
        const val DIFFERENCE_LINE_LIMIT = 40
    }
}

/**
 * Checks changes and describes how to merge them. Writes nothing.
 */
object FileConflictPolicy {

    /**
     * The suffix for the kept copy.
     *
     * Plain ASCII on purpose. An ellipsis reads nicely in a sentence and is a
     * poor filename: it is easy to mistype, awkward to select on a phone
     * keyboard, and shows up inconsistently across file managers.
     */
    const val DEFAULT_OTHER_SUFFIX = "-prepared"

    /**
     * Compares the base against the current file and decides.
     *
     * The comparison is on the **whole content**, not on a timestamp or a size.
     * A timestamp is wrong the moment two edits land in the same second, and a
     * size is wrong the moment an edit keeps the length.
     */
    fun check(change: ProposedChange): ConflictCheck {
        val state = when {
            !change.currentExists -> ConflictState.FILE_VANISHED
            change.currentContent == null -> ConflictState.FILE_VANISHED
            change.baseContent == change.currentContent -> ConflictState.NO_CONFLICT
            else -> ConflictState.CONFLICT
        }

        return ConflictCheck(
            change = change,
            state = state,
            differingLines = differingLines(change.baseContent, change.currentContent ?: "")
        )
    }

    /** Checks several changes at once. */
    fun checkAll(changes: List<ProposedChange>): List<ConflictCheck> = changes.map { check(it) }

    /**
     * The lines that differ between two versions.
     *
     * Position-based, and honest about it: a line reported as changed when it
     * only shifted is a prompt to look, not an accusation. Naming the file and
     * the conflict does the real work.
     */
    fun differingLines(base: String, current: String): List<String> {
        val baseLines = base.lines()
        val currentLines = current.lines()
        val out = mutableListOf<String>()
        val count = maxOf(baseLines.size, currentLines.size)
        for (i in 0 until count) {
            val before = baseLines.getOrNull(i)
            val after = currentLines.getOrNull(i)
            if (before != after) {
                out += "line ${i + 1}: was ${before ?: "(nothing)"} — is now ${after ?: "(nothing)"}"
            }
        }
        return out
    }

    /**
     * What writing this change would do, given a choice.
     *
     * Returns the text that would be written, or `null` when the choice writes
     * nothing. The method is named for what it does: it says what *would*
     * happen. Actually writing belongs to the caller, and this object has no way
     * to write.
     */
    fun contentAfter(
        check: ConflictCheck,
        choice: MergeChoice,
        otherFileName: String = DEFAULT_OTHER_SUFFIX
    ): String? = when (choice) {
        MergeChoice.TAKE_AGENT -> check.change.proposedContent
        MergeChoice.KEEP_CURRENT -> null
        MergeChoice.KEEP_BOTH ->
            (check.change.currentContent ?: "") +
                "\n\n--- ${check.change.path}$otherFileName ---\n" +
                check.change.proposedContent
        MergeChoice.ASK_LATER -> null
    }

    /**
     * The path an extra file would be written to.
     *
     * Named, not guessed: the user is told where their other version went.
     */
    fun pathForOtherCopy(check: ConflictCheck, otherFileName: String = DEFAULT_OTHER_SUFFIX): String {
        val dot = check.change.path.lastIndexOf('.')
        val slash = check.change.path.lastIndexOf('/')
        return if (dot > slash) {
            check.change.path.substring(0, dot) + otherFileName + check.change.path.substring(dot)
        } else {
            check.change.path + otherFileName
        }
    }

    /**
     * May a change be written at all, once the choice is made?
     *
     * Only [MergeChoice.TAKE_AGENT] and [MergeChoice.KEEP_BOTH] write. The
     * other two write nothing, and saying so here keeps the caller from
     * treating "keep current" as an operation that still touches the file.
     */
    fun willWrite(choice: MergeChoice): Boolean =
        choice == MergeChoice.TAKE_AGENT || choice == MergeChoice.KEEP_BOTH

    /**
     * The whole message for a conflicted file.
     */
    fun describe(check: ConflictCheck, choice: MergeChoice? = null): String =
        buildString {
            check.differenceLines().forEach { appendLine(it) }
            if (choice == null) {
                appendLine("Choose: keep ClauDroide's version, keep the file, keep both, or look yourself.")
            } else {
                appendLine("Chosen: ${choice.label}.")
            }
        }
}