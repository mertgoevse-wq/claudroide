package org.claudroide.app.feature.agent

import org.claudroide.app.feature.project.WorkingDirectoryPolicy

/**
 * Task 110 — "Recognise dangerous commands" (Gefährliche Befehle erkennen).
 *
 * Known risky commands should be stopped early and explained.
 *
 * **The most important thing this file says is what it is not.** The task states
 * it directly: the list must not be treated as the only security barrier, and
 * real permission limits matter more than pattern matching. A pattern list is a
 * prompt to the user, not a wall — `rm -rf` in a clever wrapper, a shell alias,
 * an obfuscated path or a renamed binary all defeat any list of strings. So:
 *
 *  - [DangerousCommandPolicy.isSoleBarrier] is `false` and says so, and a test
 *    asserts it. The name makes the denial hard to forget.
 *  - A command that matches nothing is [DangerSeverity.NOT_LISTED], never
 *    "safe". The same rule as task 107 applies here: not recognising something
 *    is not evidence that it is harmless.
 *  - The real limits live elsewhere and are named: the working directory from
 *    task 106, the approval levels from task 108, and the file provider itself.
 *
 * The rules cover the five groups the task names: deleting, system access,
 * downloads, key access and sending data away.
 */
enum class DangerousSeverity(val label: String, val stopsBeforeRunning: Boolean) {

    /**
     * A known command, but not one of the risky groups.
     *
     * This is not a clean bill of health — it only means nothing on this list
     * matched.
     */
    ORDINARY("not on the risky list", false),

    /** The command sends project data to another machine. */
    SENDS_DATA("sends project data off the device", true),

    /** The command reads credentials, keys or token files. */
    READS_SECRETS("reads keys, tokens or credentials", true),

    /** The command reaches outside the project folder or into system areas. */
    TOUCHES_SYSTEM("reaches outside the project or into system areas", true),

    /** The command downloads and runs something. */
    DOWNLOADS_AND_RUNS("downloads and runs code", true),

    /** The command deletes data. */
    DELETES("deletes data", true),

    /**
     * The command is not recognised.
     *
     * Not ordinary. Not safe. The app does not know what it does, and the
     * wording in the UI says exactly that.
     */
    NOT_LISTED("not on the risky list — the app does not know what this command does", true);

    /**
     * Which of two severities is the one to show.
     *
     * Written as an explicit comparison rather than as `maxByOrdinal`. The enum
     * order is a readability convenience, not a risk ranking, and treating it as
     * one picked the wrong answer: `curl x | sh` matched both "sends data" and
     * "downloads and runs", and the report named the milder of the two because
     * it happened to be declared later. Reading keys and sending data now both
     * outrank a plain send, and an unknown command outranks everything — it is
     * the case where the app has the least to say.
     */
    fun outranks(other: DangerousSeverity): Boolean = when {
        this === NOT_LISTED -> other !== NOT_LISTED
        other === NOT_LISTED -> false
        else -> this.ordinal >= other.ordinal
    }

    /** The two of these that should be shown. */
    fun mostSevereWith(other: DangerousSeverity): DangerousSeverity =
        if (outranks(other)) this else other

    }

/** Which group a rule belongs to. Named so the coverage can be tested. */
enum class DangerGroup(val label: String) {
    DELETION("deleting"),
    SYSTEM_ACCESS("system access"),
    DOWNLOAD("downloads"),
    SECRETS("keys and credentials"),
    EXFILTRATION("sending data away")
}

/** One rule: a pattern, what it means, and the group it belongs to. */
data class DangerRule(
    val pattern: Regex,
    val severity: DangerousSeverity,
    val group: DangerGroup,
    val explanation: String
)

/** What the policy found in one command. */
data class CommandDangerReport(
    val commandText: String,
    val severity: DangerousSeverity,
    val groups: List<DangerGroup>,
    val matchedExplanations: List<String>,
    /** True when the command also leaves the released folder. */
    val leavesWorkingDirectory: Boolean = false
) {
    /** May this command start without asking? */
    val mayRunWithoutAsking: Boolean
        get() = !severity.stopsBeforeRunning

    /** The lines for the user interface. */
    fun lines(): List<String> = buildList {
        add("Command: $commandText")
        add("Assessment: ${severity.label}.")
        matchedExplanations.forEach { add("- $it") }
        if (leavesWorkingDirectory) {
            add("- The command also leaves the project folder.")
        }
        if (!mayRunWithoutAsking) {
            add("This command has to be confirmed before it runs.")
        }
    }
}

/**
 * Recognises risky commands. Decides only.
 *
 * Opens nothing, starts nothing. [assess] is the whole surface.
 */
object DangerousCommandPolicy {

    /**
     * The rules.
     *
     * Patterns are matched against the lowercased command text. They are
     * deliberately conservative about what they claim: a rule fires on a
     * *group*, and the report says which group, rather than pretending to know
     * the exact effect.
     */
    val RULES: List<DangerRule> = listOf(
        // Deletion.
        DangerRule(
            Regex("""\brm\s+(-[a-z]*\s+)*-?[a-z]*[rf]"""),
            DangerousSeverity.DELETES, DangerGroup.DELETION,
            "rm with -r or -f removes files, and a removed file does not come back."
        ),
        DangerRule(
            Regex("""\brmdir\b|\bdel\b|\bshred\b|\btruncate\b"""),
            DangerousSeverity.DELETES, DangerGroup.DELETION,
            "removes or empties existing files."
        ),
        DangerRule(
            Regex("""\bgit\s+(reset\s+--hard|clean\s+-[a-z]*f|checkout\s+--\s+\.)"""),
            DangerousSeverity.DELETES, DangerGroup.DELETION,
            "discards uncommitted work without a way back."
        ),
        DangerRule(
            Regex("""\bfind\b[^\n]*\s-delete\b"""),
            DangerousSeverity.DELETES, DangerGroup.DELETION,
            "deletes every file it matches."
        ),

        // System access.
        DangerRule(
            Regex("""\b(su|sudo|doas)\b"""),
            DangerousSeverity.TOUCHES_SYSTEM, DangerGroup.SYSTEM_ACCESS,
            "asks for root rights, which reach every app's data on the device."
        ),
        DangerRule(
            Regex("""\b(chmod|chown|mount|umount|mkfs|setenforce)\b"""),
            DangerousSeverity.TOUCHES_SYSTEM, DangerGroup.SYSTEM_ACCESS,
            "changes permissions or the filesystem itself."
        ),
        DangerRule(
            Regex("""\b(pm\s+(install|uninstall)|service\s+call|settings\s+put)\b"""),
            DangerousSeverity.TOUCHES_SYSTEM, DangerGroup.SYSTEM_ACCESS,
            "installs software or changes system settings."
        ),

        // Downloads that run.
        DangerRule(
            Regex("""\b(curl|wget)\b[^\n]*\|\s*(ba)?sh"""),
            DangerousSeverity.DOWNLOADS_AND_RUNS, DangerGroup.DOWNLOAD,
            "pipes downloaded text straight into a shell: the download is executed before anyone reads it."
        ),
        DangerRule(
            Regex("""\beval\b|\bsource\b[^\n]*\bhttp"""),
            DangerousSeverity.DOWNLOADS_AND_RUNS, DangerGroup.DOWNLOAD,
            "runs text as a command."
        ),

        // Keys and credentials.
        DangerRule(
            Regex("""\b(cat|less|more|head|tail|cp|scp|curl|grep|strings|xxd|base64)\b[^\n]*(\.env|\.pem|\.key|\.keystore|\.jks|id_rsa|id_ed25519|\.git-credentials|\.npmrc|\.netrc|credentials\.json)"""),
            DangerousSeverity.READS_SECRETS, DangerGroup.SECRETS,
            "reads key or credential files."
        ),
        DangerRule(
            Regex("""\b(printenv|env)\b\s*(\||>|\bgrep\b)"""),
            DangerousSeverity.READS_SECRETS, DangerGroup.SECRETS,
            "prints environment variables, which is where API keys usually sit."
        ),

        // Sending data away.
        DangerRule(
            Regex("""\b(curl|wget|nc|ncat|netcat|ftp|telnet)\b"""),
            DangerousSeverity.SENDS_DATA, DangerGroup.EXFILTRATION,
            "sends data to another machine."
        ),
        DangerRule(
            Regex("""\b(scp|rsync|sftp|ssh)\b"""),
            DangerousSeverity.SENDS_DATA, DangerGroup.EXFILTRATION,
            "copies files to another machine."
        ),
        DangerRule(
            Regex("""\bgit\s+(push|remote\s+add)"""),
            DangerousSeverity.SENDS_DATA, DangerGroup.EXFILTRATION,
            "uploads project data to a remote server."
        ),
        DangerRule(
            Regex("""\b(mail|sendmail|curl\s+[^\n]*-d\s*@)"""),
            DangerousSeverity.SENDS_DATA, DangerGroup.EXFILTRATION,
            "sends mail from the device."
        )
    )

    /**
     * This list is **not** the security boundary.
     *
     * Always false, and a test asserts it stays false. If a future change ever
     * needs to claim "we block dangerous commands and that is enough", it has to
     * change this constant and fail loudly here first. The real limits are the
     * working-directory boundary (task 106), the approval levels (task 108) and
     * what the Android file provider will hand out at all.
     */
    const val IS_SOLE_BARRIER: Boolean = false

    /** The same statement, spelled out for a reader. */
    fun isSoleBarrier(): Boolean = IS_SOLE_BARRIER

    /**
     * What the limits actually are.
     *
     * Kept next to the denial so the reason cannot drift away from it.
     */
    fun actualLimits(): List<String> = listOf(
        "The command may only run inside the folder the user released (task 106).",
        "Dangerous actions always need a visible confirmation (task 108).",
        "Android decides what the app may read or write at all; no pattern list can widen that."
    )

    /**
     * Checks a command.
     *
     * @param workingDirectory the folder the command would run in, if known.
     *        When given, the report also says whether the command would leave it.
     */
    fun assess(commandText: String, workingDirectory: String? = null): CommandDangerReport {
        val text = commandText.trim()
        val lower = text.lowercase()

        val matched = RULES.filter { it.pattern.containsMatchIn(lower) }

        val severity = when {
            matched.isNotEmpty() ->
                matched.map { it.severity }
                    .reduce { acc, next -> acc.mostSevereWith(next) }
            text.isEmpty() -> DangerousSeverity.NOT_LISTED
            isKnownOrdinaryCommand(lower) -> DangerousSeverity.ORDINARY
            else -> DangerousSeverity.NOT_LISTED
        }

        val groups = matched.map { it.group }.distinct().sortedBy { it.ordinal }

        val leaves = workingDirectory != null && leavesWorkingDirectory(text, workingDirectory)

        return CommandDangerReport(
            commandText = text,
            severity = severity,
            groups = groups,
            matchedExplanations = matched.map { it.explanation }.distinct(),
            leavesWorkingDirectory = leaves
        )
    }

    /**
     * Commands the app knows and does not consider risky.
     *
     * Only used to separate "known and ordinary" from "not recognised". Both
     * stop the command in the sense that [DangerousSeverity.NOT_LISTED] asks for
     * confirmation too — this list exists so the message can be honest, not to
     * make unknown commands run.
     */
    private val ORDINARY = listOf(
        "ls", "cat", "head", "tail", "grep", "find", "pwd", "echo", "wc",
        "sort", "uniq", "diff", "stat", "file", "which", "date", "du", "df",
        "tree", "mkdir", "touch", "cp", "mv", "sed", "awk", "cut", "tr",
        "gradle", "java", "kotlinc", "adb", "jq", "tar", "zip", "unzip"
    )

    /** Git subcommands that only read. Anything else stays unrecognised. */
    private val ORDINARY_GIT = listOf(
        "status", "diff", "log", "show", "branch", "remote", "config", "blame", "ls-files"
    )

    private fun isKnownOrdinaryCommand(lower: String): Boolean {
        val words = lower.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val first = words.firstOrNull() ?: return false
        if (first in ORDINARY) return true
        if (first == "git") {
            val sub = words.getOrNull(1) ?: return false
            // `git push`, `git reset --hard` and `git clean -f` are matched by
            // rules already; anything left here is a read.
            return sub in ORDINARY_GIT && dangerousRulesFor(lower).isEmpty()
        }
        return false
    }

    /** The rules that match a command, without building a report. */
    private fun dangerousRulesFor(lower: String): List<DangerRule> =
        RULES.filter { it.pattern.containsMatchIn(lower) }

    /**
     * Would this command reach outside the released folder?
     *
     * Only the obvious spellings are detected — `..`, an absolute path, a path
     * that starts at `/`. This is a warning, not a wall: a command can still
     * leave the folder without writing it down, which is why
     * [isSoleBarrier] is false.
     */
    fun leavesWorkingDirectory(commandText: String, workingDirectory: String): Boolean {
        val tokens = commandText.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return false
        val reachesOut = tokens.any { token ->
            token.startsWith("/") ||
                token == "~" || token.startsWith("~/") ||
                token == ".." || token.startsWith("../") ||
                token.contains("/../") ||
                token.contains("/..")
        }
        return reachesOut || !WorkingDirectoryPolicy
            .resolve(workingDirectory, ".")
            .isAllowed
    }

    /** The five groups the task asks for, as lines for a settings or help screen. */
    fun coveredGroups(): List<String> = DangerGroup.values().map { it.label }
}