package org.claudroide.app.feature.agent

import org.claudroide.app.feature.project.WorkingDirectoryPolicy

/**
 * Task 107 — "Show the command first" (Befehl vorher zeigen).
 *
 * The goal is that the user understands the purpose, the project folder and the
 * possible effects **before** a command runs. The result is a preview with a
 * short explanation, the storage and network it needs, and confirm/cancel.
 *
 * Three rules shape the whole type, and each one is structural rather than a
 * convention:
 *
 *  1. **The preview cannot trigger anything.** [CommandPreview] is a data class
 *     and [CommandPreviewPolicy] is a pure decision object: neither opens a file,
 *     starts a process, nor hands back a callback to run. A caller holding a
 *     preview has nothing to call. The test asserts this by reflection over the
 *     public API, because "the preview is only a preview" is exactly the kind of
 *     claim that decays the first time somebody adds a convenience method.
 *
 *  2. **Installing, deleting and sending data off the device are called out on
 *     their own.** They appear in [CommandEffect] and in the headline
 *     [CommandPreview.risk], and [CommandPreviewPolicy.effectsOf] is the single
 *     place that decides. A preview that lists them as one line among many is a
 *     preview nobody reads.
 *
 *  3. **An unrecognised command is not called harmless.** [CommandRisk] has no
 *     "safe" default: [CommandPreviewPolicy.classify] returns
 *     [CommandRisk.UNRECOGNISED] for anything it cannot place, and the English
 *     word used is "unknown", never "safe". Silently treating what we did not
 *     understand as harmless is the failure this rule exists to prevent — the app
 *     does not know what an unfamiliar command will do, and must not imply that
 *     it does.
 */
enum class CommandRisk(val label: String, val needsExplicitConfirmation: Boolean) {

    /** Reads something. Changes nothing. */
    LOW("low risk", false),

    /** Writes into the project folder. */
    MEDIUM("medium risk", true),

    /**
     * Installs software, deletes data, or sends something outside the device.
     */
    HIGH("high risk", true),

    /**
     * The app does not know what this command does.
     *
     * Not low risk. Not medium. There is no risk value a preview can honestly
     * assign to a command it failed to parse, so this risk stands on its own and
     * always needs confirmation.
     */
    UNRECOGNISED("unknown — the app cannot tell what this command does", true)
}

/** One thing the command does. Marked separately so it can be shown separately. */
enum class CommandEffect(val label: String, val isIrreversible: Boolean) {

    /** Reads project files. */
    READS_PROJECT("reads project files", false),

    /** Creates or changes files in the project folder. */
    WRITES_PROJECT("writes files in the project folder", false),

    /** Installs software or applies a system change. */
    INSTALLS_SOFTWARE("installs software", true),

    /** Removes files, and removing them is not undoable by re-running anything. */
    DELETES_DATA("deletes files — this cannot be undone by re-running the command", true),

    /** Sends project data to another machine or the internet. */
    SENDS_DATA_OFF_DEVICE("sends data off this device", true),

    /** Uses the network. */
    USES_NETWORK("uses the network", false),

    /**
     * The command reads something the app does not model.
     *
     * Reported so a gap in the effect list shows up as a gap rather than as
     * silence.
     */
    UNMODELLED_EFFECT("may do something the app does not describe", true)
}

/** What the command needs. Shown before the user decides, not after. */
data class CommandRequirements(
    val needsStorage: Boolean = false,
    val needsNetwork: Boolean = false,
    val estimatedDownloadBytes: Long = 0L
) {
    val hasAnyRequirement: Boolean
        get() = needsStorage || needsNetwork || estimatedDownloadBytes > 0L
}

/**
 * The command as the user sees it.
 *
 * Carries the text, the working directory, the risk, the effects and what it
 * needs. It carries **no way to run anything** — see [CommandPreviewPolicy].
 */
data class CommandPreview(
    val commandText: String,
    val workingDirectory: String,
    val purpose: String,
    val risk: CommandRisk,
    val effects: List<CommandEffect>,
    val requirements: CommandRequirements,
    /** Why the app reached this verdict, in one sentence. */
    val explanation: String
) {
    /**
     * The effects worth putting in front of the user, worst first.
     *
     * The irreversible ones lead, because those are the ones a fast reader
     * scrolls past. Order is fixed so two previews of the same command always
     * read the same way.
     */
    fun highlightedEffects(): List<CommandEffect> =
        effects.filter { it.isIrreversible || it == CommandEffect.USES_NETWORK }
            .sortedBy { it.ordinal }

    /** Does this need the user to say yes before anything happens? */
    val needsConfirmation: Boolean get() = risk.needsExplicitConfirmation

    /**
     * The lines for the user interface.
     *
     * The command and its folder come first, then the risk, then the
     * highlighted effects, then the requirements. The order is the point: a
     * preview that lists requirements before saying what the command does gets
     * read as a permission dialog.
     */
    fun lines(): List<String> = buildList {
        add("Command: $commandText")
        add("Folder: $workingDirectory")
        add("Purpose: $purpose")
        add("Risk: ${risk.label}.")
        if (explanation.isNotBlank()) add(explanation)
        val marked = highlightedEffects()
        if (marked.isEmpty()) {
            add("Effects: none known.")
        } else {
            marked.forEach { add("Marked: ${it.label}.") }
        }
        if (requirements.hasAnyRequirement) {
            val parts = mutableListOf<String>()
            if (requirements.needsStorage) parts += "device storage"
            if (requirements.needsNetwork) parts += "network"
            if (requirements.estimatedDownloadBytes > 0L) {
                parts += "about ${requirements.estimatedDownloadBytes} bytes downloaded"
            }
            add("Needs: ${parts.joinToString(", ")}.")
        }
    }
}

/**
 * Builds a preview. Decides only.
 *
 * Nothing here runs, opens, or schedules anything. [describe] is the whole
 * public surface and it takes text and returns text.
 */
object CommandPreviewPolicy {

    /**
     * The tools whose effects are known well enough to describe.
     *
     * Each entry says what it does. A command that starts with none of these is
     * [CommandRisk.UNRECOGNISED] — the list is deliberately not a fallback.
     */
    private data class KnownCommand(
        val tool: String,
        val purpose: String,
        val risk: CommandRisk,
        val effects: List<CommandEffect>
    )

    private val KNOWN = listOf(
        KnownCommand(
            "ls", "lists the files in a folder", CommandRisk.LOW,
            listOf(CommandEffect.READS_PROJECT)
        ),
        KnownCommand(
            "cat", "shows the content of a file", CommandRisk.LOW,
            listOf(CommandEffect.READS_PROJECT)
        ),
        KnownCommand(
            "grep", "searches text inside project files", CommandRisk.LOW,
            listOf(CommandEffect.READS_PROJECT)
        ),
        KnownCommand(
            "git status", "shows which files are changed", CommandRisk.LOW,
            listOf(CommandEffect.READS_PROJECT)
        ),
        KnownCommand(
            "git diff", "shows the changes in the working tree", CommandRisk.LOW,
            listOf(CommandEffect.READS_PROJECT)
        ),
        KnownCommand(
            "mkdir", "creates a folder in the project", CommandRisk.MEDIUM,
            listOf(CommandEffect.WRITES_PROJECT)
        ),
        KnownCommand(
            "cp", "copies a file inside the project", CommandRisk.MEDIUM,
            listOf(CommandEffect.READS_PROJECT, CommandEffect.WRITES_PROJECT)
        ),
        KnownCommand(
            "mv", "moves a file inside the project", CommandRisk.MEDIUM,
            listOf(CommandEffect.READS_PROJECT, CommandEffect.WRITES_PROJECT)
        ),
        KnownCommand(
            "gradle", "builds the project and writes build output", CommandRisk.MEDIUM,
            listOf(
                CommandEffect.READS_PROJECT,
                CommandEffect.WRITES_PROJECT,
                CommandEffect.USES_NETWORK
            )
        ),
        KnownCommand(
            "npm install", "downloads and installs packages", CommandRisk.HIGH,
            listOf(
                CommandEffect.WRITES_PROJECT,
                CommandEffect.INSTALLS_SOFTWARE,
                CommandEffect.USES_NETWORK
            )
        ),
        KnownCommand(
            "apt install", "installs software on the device", CommandRisk.HIGH,
            listOf(
                CommandEffect.INSTALLS_SOFTWARE,
                CommandEffect.USES_NETWORK
            )
        ),
        KnownCommand(
            "pip install", "installs Python packages", CommandRisk.HIGH,
            listOf(
                CommandEffect.WRITES_PROJECT,
                CommandEffect.INSTALLS_SOFTWARE,
                CommandEffect.USES_NETWORK
            )
        ),
        KnownCommand(
            "rm", "removes files from the project", CommandRisk.HIGH,
            listOf(CommandEffect.DELETES_DATA)
        ),
        KnownCommand(
            "git push", "uploads the committed changes to the remote", CommandRisk.HIGH,
            listOf(CommandEffect.READS_PROJECT, CommandEffect.SENDS_DATA_OFF_DEVICE)
        ),
        KnownCommand(
            "curl", "fetches data from the network", CommandRisk.HIGH,
            listOf(CommandEffect.USES_NETWORK, CommandEffect.WRITES_PROJECT)
        ),
        KnownCommand(
            "scp", "copies files to another machine", CommandRisk.HIGH,
            listOf(CommandEffect.READS_PROJECT, CommandEffect.SENDS_DATA_OFF_DEVICE)
        )
    )

    /**
     * Finds the entry [describe] should use, or `null` when the command is not
     * on the list.
     *
     * The lookup is longest-first: a two-word subcommand wins over the bare
     * tool, because `git push` and `git status` have very different effects and
     * collapsing them would make one of them lie. The single word is the
     * fallback, so `gradle assembleDebug` is still `gradle` and `rm -rf` is
     * still `rm` — a flag or a task name must not turn the most destructive
     * command there is into an unknown one.
     */
    private fun knownFor(commandText: String): KnownCommand? {
        val words = commandText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val first = words[0].lowercase()
        KNOWN.firstOrNull { it.tool == first }?.let { return it }
        if (words.size < 2) return null
        val pair = "$first ${words[1].lowercase()}"
        return KNOWN.firstOrNull { it.tool == pair }
    }

    /**
     * The effects of a command, or an explicit gap.
     *
     * When the tool is not in [KNOWN] the answer is not an empty list — an empty
     * list would read as "this does nothing". It is
     * [CommandEffect.UNMODELLED_EFFECT], which says out loud that the app has
     * not described this command.
     */
    fun effectsOf(commandText: String): List<CommandEffect> {
        val known = knownFor(commandText)
        return known?.effects ?: listOf(CommandEffect.UNMODELLED_EFFECT)
    }

    /**
     * What the app says the command will do.
     *
     * @param workingDirectory the folder the command would run in.
     */
    fun describe(commandText: String, workingDirectory: String): CommandPreview {
        val known = knownFor(commandText)

        val risk = known?.risk ?: CommandRisk.UNRECOGNISED
        val effects = known?.effects ?: listOf(CommandEffect.UNMODELLED_EFFECT)

        val purpose = known?.purpose
            ?: "The app does not know what this command does."

        val explanation = if (known == null) {
            "This command is not on the list of commands the app can describe, " +
                "so it is shown as unknown rather than as safe."
        } else {
            ""
        }

        return CommandPreview(
            commandText = commandText,
            workingDirectory = workingDirectory,
            purpose = purpose,
            risk = risk,
            effects = effects,
            requirements = requirementsFor(effects),
            explanation = explanation
        )
    }

    /**
     * What the command needs, derived from its effects.
     *
     * Derived, not passed in: a caller cannot promise that an installing
     * command needs no storage.
     */
    fun requirementsFor(effects: List<CommandEffect>): CommandRequirements {
        val network = CommandEffect.USES_NETWORK in effects ||
            CommandEffect.SENDS_DATA_OFF_DEVICE in effects
        val storage = CommandEffect.WRITES_PROJECT in effects ||
            CommandEffect.DELETES_DATA in effects ||
            CommandEffect.INSTALLS_SOFTWARE in effects
        return CommandRequirements(needsStorage = storage, needsNetwork = network)
    }

    /**
     * The one sentence a preview has to carry when something irreversible is in
     * it.
     *
     * Empty when there is nothing irreversible, so a quiet preview stays quiet
     * rather than crying wolf on every command.
     */
    fun warningFor(preview: CommandPreview): String {
        val marks = preview.highlightedEffects()
        return when {
            preview.risk == CommandRisk.UNRECOGNISED ->
                "The app does not know what this command does. Check it yourself before confirming."
            marks.isEmpty() -> ""
            else -> "Before you confirm: " +
                marks.joinToString("; ") { it.label } + "."
        }
    }

    /**
     * Whether the folder the command would run in is the one the user released.
     *
     * A preview about `rm -rf` in the wrong folder is a warning about the wrong
     * thing, so the folder check is part of the preview and not a later step.
     */
    fun isPreviewForReleasedFolder(preview: CommandPreview, releasedFolder: String): Boolean =
        WorkingDirectoryPolicy
            .resolve(releasedFolder, preview.workingDirectory)
            .isAllowed
}