package org.claudroide.app.feature.project

/**
 * Task 099 — "secure the task state".
 *
 * Goal: store related, reviewed changes as named local states. Result: commit
 * proposal, file overview, review status and change history.
 *
 * This file writes nothing. It *proposes* a commit — message, file list, review
 * status — and leaves the writing to the caller, as the rest of the project does.
 * The difference to task 098: there the question was which files belong in the
 * list; here it is what the user must **know about that list** before confirming it.
 *
 * ## The two completion conditions as properties
 *
 *  1. *"The commit message describes the purpose understandably."*
 *     [CommitIntent.summary] is not built from file names. A message assembled
 *     from file names ("3 files changed") tells the user nothing about the purpose
 *     and invites guessing. [CommitIntent] therefore requires a whole sentence and
 *     rejects a message that merely repeats file names.
 *
 *  2. *"The user can review the changes before saving."*
 *     [CommitGate.mayCommit] checks in a fixed order: secret suspicion first, then
 *     completeness, then confirmation. And [CommitProposal.displayLines] names every
 *     single file — there is no "3 files" summary hiding what is inside.
 *
 * ## The guard: nothing enters unconfirmed or holding a secret
 *
 *  * [CommitGate.mayCommit] refuses while [GitChangeList.silentlyIncluded] is
 *    non-empty — a file nobody named does not ride along.
 *  * It also refuses while [GitChangeList.blockedBySecretCheck] is non-empty. That
 *    is the same source as in 098; here it becomes a **condition**, not a notice.
 *
 * Every refusal names the paths it is about. A refusal that says "some files" is
 * not actionable, and the user cannot honour a rule he cannot see the subject of.
 *
 * Pure Kotlin: no network, no filesystem, no process, no Android.
 */

/**
 * Whether [this] is in essence nothing but a list of file names.
 *
 * The rule: a message containing **no** file name is plainly a statement. A
 * message containing file names needs at least two further words beside them —
 * otherwise it is only a list.
 *
 * Detection is deliberately based on file names **with** an extension or a path
 * separator, not on capitalisation: German sentences start upper-case, and a gate
 * that rejects "Ersetzt …" because it contains a capital letter is not a usable
 * gate.
 */
private fun String.isOnlyFileNames(): Boolean {
    val words = trim().split(' ', '\t').filter { it.isNotBlank() }
    val fileNames = words.filter { word ->
        word.contains('/') || word.contains(FILE_NAME_SUFFIX)
    }
    if (fileNames.isEmpty()) return false
    val prose = words.filterNot { it in fileNames }
    return prose.size < MIN_PROSE_WORDS
}

/** Trailing `.ext` of a word — what makes a word look like a file name. */
private val FILE_NAME_SUFFIX = Regex("""\.[A-Za-z][A-Za-z0-9]*$""")

/** Words that must stand beside a file name for a message to be a statement. */
private const val MIN_PROSE_WORDS = 2

// ── The intent ─────────────────────────────────────────────────────────────

/**
 * The purpose of a state, **in whole sentences**.
 *
 * The constructor rejects a message that only repeats file names, right here:
 * such a message would otherwise pass later as "valid" and the user could no
 * longer recognise it as empty of meaning.
 */
data class CommitIntent(
    /** The sentence that states the purpose. Short enough for one line. */
    val summary: String,
    /** Why this change was needed. Optional, but recommended. */
    val rationale: String? = null
) {
    init {
        require(summary.isNotBlank()) { "A state needs a purpose." }
        require(summary.length <= MAX_SUMMARY_LENGTH) {
            "A commit message is one line, not an essay; at most $MAX_SUMMARY_LENGTH characters."
        }
        require(!summary.contains('\n')) { "A commit message is one line." }
        require(summary.none { it.isISOControl() && it != '\t' }) {
            "A commit message cannot contain a control character."
        }
        require(!summary.isOnlyFileNames()) {
            "A message that only repeats file names describes no purpose: $summary"
        }
        rationale?.let {
            require(it.isNotBlank()) { "An empty reason is no reason." }
        }
    }

    companion object {
        const val MAX_SUMMARY_LENGTH = 120
    }
}

// ── The proposal ───────────────────────────────────────────────────────────

/**
 * A **proposal**, not an executed act.
 *
 * [fileList] is the reviewed list from task 098 held by value, so the list the
 * proposal was checked against cannot change afterwards. Without copying it, it
 * would be possible to check one list and save another.
 */
data class CommitProposal(
    val intent: CommitIntent,
    val fileList: GitChangeList,
    val branchName: String,
    /** The commit message headline. */
    val message: String = intent.summary
) {
    init {
        require(branchName.isNotBlank()) { "A state needs a branch." }
        require(message.isNotBlank()) { "A state needs a message." }
        require(!message.isOnlyFileNames()) {
            "A message that only repeats file names describes no purpose: $message"
        }
    }

    /** How many files this state covers. */
    val fileCount: Int get() = fileList.entries.size

    /** The paths that actually go into this state. */
    val committedPaths: List<String> get() = fileList.stageablePaths

    /**
     * The lines for the user interface: **every** file on its own.
     *
     * No count as the first line. The purpose of [CommitGate] is that the user
     * sees what will be saved **before** it is saved — and for that every single
     * line has to be there, not a number behind which he cannot look.
     */
    fun displayLines(): List<String> = buildList {
        add("Zweck: $message")
        intent.rationale?.let { add("Grund: $it") }
        add("Zweig: $branchName")
        add("Dateien: ${fileList.entries.size}")
        fileList.entries.forEach { add("  ${it.displayLine()}") }
        if (!fileList.coveredByCommitIntent) {
            add("Nicht von dir benannt und daher nicht enthalten:")
            fileList.silentlyIncluded.forEach { add("  ${it.path}") }
        }
        if (fileList.blockedBySecretCheck.isNotEmpty()) {
            add("Gesperrt wegen Geheimnisverdacht:")
            fileList.blockedBySecretCheck.forEach { add("  $it") }
        }
    }
}

// ── The gate ───────────────────────────────────────────────────────────────

/** The answer to "may it be saved?". */
sealed interface CommitDecision {
    val germanLabel: String

    /** Checked. The writing is outside this file. */
    data class MayCommit(val proposal: CommitProposal) : CommitDecision {
        override val germanLabel: String get() = "darf gespeichert werden"
    }

    /** Nothing is saved. */
    data class Refused(val reason: String) : CommitDecision {
        override val germanLabel: String get() = "nicht gespeichert"
    }
}

/**
 * The rule for when a state may be saved.
 *
 * Checked in a fixed order: secret suspicion, completeness, confirmation. Secret
 * suspicion comes **first** because it is the only check a user confirmation
 * cannot clear — a committed file stays a committed file.
 */
object CommitGate {

    /**
     * May [proposal] be saved?
     *
     * @param userConfirmed an **own** input, default `false`. It is derived from
     *        nothing else — not from completeness, not from the branch, not from
     *        the existence of a proposal.
     */
    fun mayCommit(
        proposal: CommitProposal,
        userConfirmed: Boolean = false
    ): CommitDecision {
        val list = proposal.fileList

        // 1. Secret suspicion first. A confirmation does not lift it.
        val blocked = list.blockedBySecretCheck
        if (blocked.isNotEmpty()) {
            return CommitDecision.Refused(
                "${blocked.size} Datei(en) stehen wegen Geheimnisverdacht unter Sperre. " +
                    "Sie werden nicht gespeichert: ${blocked.joinToString(", ")}"
            )
        }

        // 2. Nothing nobody named.
        val unnamed = list.silentlyIncluded.map { it.path }
        if (unnamed.isNotEmpty()) {
            return CommitDecision.Refused(
                "${unnamed.size} Datei(en) wurden von niemandem benannt. Sie fahren " +
                    "nicht mit: ${unnamed.joinToString(", ")}. " +
                    "Nenne sie ausdrücklich oder entferne sie."
            )
        }

        // 3. Confirmation.
        if (!userConfirmed) {
            return CommitDecision.Refused(
                "Der Nutzer hat die Änderungen nicht bestätigt. Es wird nichts gespeichert."
            )
        }

        // 4. A state with no file is not a state.
        if (proposal.fileCount == 0) {
            return CommitDecision.Refused(
                "Der Vorschlag enthält keine Datei. Es wird nichts gespeichert."
            )
        }

        return CommitDecision.MayCommit(proposal)
    }
}