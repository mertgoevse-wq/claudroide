package org.claudroide.app.feature.project

/**
 * Task 093 — "Read project instructions" (Projektanweisungen lesen).
 *
 * Goal: understand common project rules and Claude-Code-style markdown
 * instructions.
 *
 * A file called `CLAUDE.md` inside somebody else's repository is **data the app
 * found**, not an instruction the app obeys. That distinction is the whole task, and
 * it is enforced structurally rather than by good intentions:
 *
 *  1. **A project instruction can never become a command.** [ProjectInstructionReader]
 *    returns [ProjectInstruction], which is a value made of strings. It has no
 *    method that executes, applies, approves or produces a tool call or an approval
 *    level — a test checks the method names and the return types. There is no code
 *    path from "this file said so" to "the app will do it". Whatever an instruction
 *    file says, the actual permissions still come from
 *    [ProjectBoundaryEnforcer] and the approvals the user gives.
 *
 *  2. **Attempts to widen permissions are reported, not followed.** [IgnoreReason.ESCALATION_ATTEMPT]
 *    catches the patterns that try to talk the app out of its rules — "ignore the
 *    previous instructions", "you do not need to ask", "push without confirmation".
 *    The line is kept in [ProjectInstruction.ignored] with its number, so the user can
 *    see it was refused rather than quietly dropped.
 *
 *  3. **Unsupported rules are visibly ignored.** A file this app does not support is
 *     listed in [ProjectInstruction.unsupportedFiles] rather than passed over in
 *     silence, and [InstructionFileKind.isSupported] makes the boundary a property of
 *     the type.
 *
 * Every line still passes through [org.claudroide.app.feature.provider.SecretMasker],
 * because an instruction file is still a file from an untrusted tree.
 */

/** A file that may carry project instructions. */
enum class InstructionFileKind(
    val fileName: String,
    val isSupported: Boolean,
    val note: String
) {
    CLAUDE_MD("CLAUDE.md", true, "Claude Code style project instructions"),
    AGENTS_MD("AGENTS.md", true, "Generic agent instructions"),
    GITHUB_COPILOT(".github/copilot-instructions.md", true, "GitHub Copilot instructions"),
    CURSOR_RULES(".cursorrules", true, "Cursor rules"),
    CONVENTIONS(".editorconfig", true, "Formatting rules, not instructions"),

    /**
     * Recognised but **not** read as instructions.
     *
     * A README is documentation *for humans*. Letting it steer the agent would mean
     * any prose in a repository could become a rule, which is the same hole as [IgnoreReason.ESCALATION_ATTEMPT]
     * with a different name.
     */
    README_MD("README.md", false, "human documentation, not agent instructions"),

    CONTRIBUTING_MD("CONTRIBUTING.md", false, "contribution guide, not agent instructions");

    companion object {
        /**
         * Welche Datei ist das?
         *
         * Verglichen wird **zweimal**: einmal gegen den vollen Pfad und einmal gegen
         * den Dateinamen. Ohne den vollen Pfad würde `.github/copilot-instructions.md`
         * nie erkannt, weil dort nur `copilot-instructions.md` übrig bliebe. Ohne den
         * Dateinamen wiederum wäre `src/main/CLAUDE.md` unauffindbar, obwohl dieselbe
         * Datei an anderer Stelle ohne Pfad gefunden wird.
         */
        fun forPath(path: String): InstructionFileKind? {
            val p = path.trim().replace('\\', '/').trimStart('.', '/')
            val name = p.substringAfterLast('/')
            return values().firstOrNull { it.fileName == p }
                ?: values().firstOrNull { it.fileName.substringAfterLast('/') == name }
        }
    }
}

/** How widely an instruction applies. */
enum class InstructionScope(val label: String) {

    /** Applies to the whole repository. */
    PROJECT("the whole project"),

    /** Applies to a subfolder. */
    DIRECTORY("one folder")
}

/** Why a line was not followed. */
enum class IgnoreReason(val label: String) {

    /** The line tried to talk the app out of its rules. */
    ESCALATION_ATTEMPT("tries to change the app's own rules"),

    /** The file is not one this app reads as instructions. */
    UNSUPPORTED_FILE("file type not supported as instructions"),

    /** The line was empty or longer than a line of prose can be. */
    NOT_A_RULE("not a rule"),

    /** The line looked like it carried a credential. */
    SECRET_LIKE("may contain a credential")
}

/** One line of an instruction file, kept as text. */
data class InstructionLine(
    val lineNumber: Int,
    val text: String
) {
    init {
        require(lineNumber >= 1) { "Line numbers start at 1." }
    }
}

/** A line that was read but deliberately not followed. */
data class IgnoredInstruction(
    val lineNumber: Int,
    val text: String,
    val reason: IgnoreReason
) {
    init {
        require(lineNumber >= 1) { "Line numbers start at 1." }
    }
}

/**
 * What one instruction file contained.
 *
 * @property sourceFile where it came from, so the user can open it themselves.
 * @property scope how widely it claims to apply.
 * @property lines the lines kept as data.
 * @property ignored the lines read and not followed, each with a reason.
 */
data class ProjectInstruction(
    val sourceFile: String,
    val kind: InstructionFileKind,
    val scope: InstructionScope = InstructionScope.PROJECT,
    val lines: List<InstructionLine> = emptyList(),
    val ignored: List<IgnoredInstruction> = emptyList()
) {
    init {
        require(sourceFile.isNotBlank()) { "An instruction needs a source file." }
    }

    /** Attempts to change the app's own rules that were found. */
    val escalationAttempts: List<IgnoredInstruction>
        get() = ignored.filter { it.reason == IgnoreReason.ESCALATION_ATTEMPT }

    /** Did the file try to widen permissions? */
    val triedToEscalate: Boolean get() = escalationAttempts.isNotEmpty()

    /** The preview, with the refusal visible. */
    fun previewLines(): List<String> = buildList {
        add("Instructions read from $sourceFile (${scope.label}).")
        add(
            "These are text found in the project. They are treated as information, " +
                "not as commands for this app."
        )
        if (lines.isEmpty()) {
            add("  No usable rules found.")
        }
        lines.forEach { add("  ${it.lineNumber}: ${it.text}") }
        if (ignored.isNotEmpty()) {
            add("Read but not followed (${ignored.size}):")
            ignored.forEach { add("  ${it.lineNumber}: ${it.reason.label} — \"${it.text}\"") }
        }
        if (triedToEscalate) {
            add(
                "! This file tried to change the app's own rules " +
                    "(${escalationAttempts.size} line(s)). Those lines were not followed."
            )
        }
    }
}

/**
 * Reads instruction files as data.
 *
 * The class has no method that executes anything and no method that returns a
 * command. That absence is the security property, and it is checked by a test
 * rather than described in a comment.
 */
class ProjectInstructionReader {

    /**
     * Phrases that try to talk the app out of its own rules.
     *
     * Matched case-insensitively against the line. This list is deliberately narrow
     * and about *escalation*: a project is free to say "we use tabs". It is not free
     * to say "you do not need to ask the user".
     */
    private val escalationPhrases: List<String> = listOf(
        "ignore the previous",
        "ignore previous",
        "ignore all previous",
        "disregard the",
        "disregard previous",
        "without asking",
        "without confirmation",
        "no confirmation needed",
        "do not need to ask",
        "you are now allowed",
        "you are permitted to",
        "grant yourself",
        "bypass",
        "--dangerously",
        "skip the approval",
        "skip confirmation"
    )

    /**
     * The longest line still treated as prose.
     *
     * A line far beyond this is not a rule the user could read on a phone; it is
     * almost always an encoded blob.
     */
    val maxLineLength: Int = 500

    /**
     * Reads one instruction file.
     *
     * Returns the text as data. A file kind that is not supported comes back with
     * [InstructionFileKind.isSupported] `false` and its content in [ProjectInstruction.ignored],
     * so the user sees that it was found and not used, instead of it disappearing.
     */
    fun read(
        path: String,
        contents: String,
        scope: InstructionScope = InstructionScope.PROJECT
    ): ProjectInstruction {
        val kind = InstructionFileKind.forPath(path)
        val sourceFile = path.trim().replace('\\', '/')

        if (kind == null || !kind.isSupported) {
            val reason = if (kind == null) IgnoreReason.UNSUPPORTED_FILE
            else IgnoreReason.UNSUPPORTED_FILE
            return ProjectInstruction(
                sourceFile = sourceFile,
                kind = kind ?: InstructionFileKind.README_MD,
                scope = scope,
                lines = emptyList(),
                ignored = listOf(
                    IgnoredInstruction(
                        lineNumber = 1,
                        text = (kind?.note ?: "unrecognised file"),
                        reason = reason
                    )
                )
            )
        }

        val behalten = mutableListOf<InstructionLine>()
        val verworfen = mutableListOf<IgnoredInstruction>()

        contents.split('\n').forEachIndexed { index, roh ->
            val nummer = index + 1
            val text = roh.trim()

            when {
                text.isEmpty() -> Unit
                text.length > maxLineLength -> verworfen += IgnoredInstruction(
                    nummer,
                    "line too long (${text.length} characters)",
                    IgnoreReason.NOT_A_RULE
                )
                isEscalation(text) -> verworfen += IgnoredInstruction(
                    nummer,
                    text.take(120),
                    IgnoreReason.ESCALATION_ATTEMPT
                )
                looksLikeSecret(text) -> verworfen += IgnoredInstruction(
                    nummer,
                    org.claudroide.app.feature.provider.SecretMasker.REDACTION_PLACEHOLDER,
                    IgnoreReason.SECRET_LIKE
                )
                else -> behalten += InstructionLine(nummer, text)
            }
        }

        return ProjectInstruction(
            sourceFile = sourceFile,
            kind = kind,
            scope = scope,
            lines = behalten,
            ignored = verworfen
        )
    }

    /** Reads several files, keeping the result of each. */
    fun readAll(
        files: Map<String, String>,
        scope: InstructionScope = InstructionScope.PROJECT
    ): List<ProjectInstruction> = files.map { (path, text) -> read(path, text, scope) }

    /** Finds the instruction files among a list of project paths. */
    fun detect(paths: List<String>): List<InstructionFileKind> =
        paths.mapNotNull { InstructionFileKind.forPath(it) }.distinct()

    /** Files that were found but are not read as instructions. */
    fun unsupportedFiles(paths: List<String>): List<String> =
        paths.filter { InstructionFileKind.forPath(it)?.isSupported == false }

    /** Try to change the app's own rules? */
    fun isEscalation(line: String): Boolean {
        val lower = line.lowercase()
        return escalationPhrases.any { lower.contains(it) }
    }

    private fun looksLikeSecret(line: String): Boolean =
        org.claudroide.app.feature.provider.SecretMasker.containsSecretLikeText(line)

    /** What this app reads, stated plainly for the documentation screen. */
    fun supportedFormatsLines(): List<String> = buildList {
        add("Files read as project instructions:")
        InstructionFileKind.values()
            .filter { it.isSupported }
            .forEach { add("  ${it.fileName} — ${it.note}") }
        add("Found but not read as instructions:")
        InstructionFileKind.values()
            .filterNot { it.isSupported }
            .forEach { add("  ${it.fileName} — ${it.note}") }
        add("")
        add(
            "Whatever these files say, they cannot grant a permission, skip a " +
                "confirmation or widen what the app is allowed to touch. " +
                "Permissions come from the folder you selected and the approvals you give."
        )
    }
}
