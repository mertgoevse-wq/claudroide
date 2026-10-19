package org.claudroide.app.feature.project

import org.claudroide.app.feature.provider.SecretMasker

/**
 * The one place where a tool action is allowed or refused.
 *
 * Task 119 is a security gate. The rules it enforces, taken from the brief:
 *  - "Jeder Werkzeugtyp denselben Schutz verwendet": reading, writing, deleting and
 *    running a command all go through [evaluate]. There is no second, laxer entry
 *    point, so a caller cannot pick a cheaper check.
 *  - "Widerrufener Zugriff unmittelbar blockiert": a revocation in the current
 *    [ProjectBoundary] takes effect on the very next call. A permit handed out
 *    earlier carries the generation it was issued in and stops being valid as soon
 *    as anything was revoked.
 *  - "Keine KI-Anweisung kann die App-Grenze selbst erweitern": only an action that
 *    originates from [ActionOrigin.USER] may contribute additional roots. A model
 *    action that carries roots of its own is refused, whatever it says about them.
 *
 * Secrets are refused on every path, not only on sending: a secret file may not be
 * read into the AI context *and* may not be written, because a write is how a key
 * gets exfiltrated or overwritten. The secret list itself is reused from
 * [ProjectExclusionPolicy] rather than duplicated here.
 *
 * Everything is pure and synchronous: plain strings in, plain data out, no file
 * system, no Android, no coroutines. That keeps the gate fully testable on the JVM
 * and forces the caller to resolve real paths explicitly via [PathBoundaryGuard],
 * which is where the symlink check lives.
 *
 * Every refusal carries a plain German sentence naming the reason. There is no
 * path through this object that returns a refusal without a message.
 */
object ProjectBoundaryEnforcer {

    /** What a tool wants to do. */
    enum class ToolKind {
        READ,
        WRITE,
        DELETE,
        EXECUTE;

        /**
         * Maps a tool name from a model or a settings screen onto a kind, or null
         * when the name is unknown. An unknown name must never default to ALLOWED —
         * the caller gets [BoundaryDecision.UNKNOWN_TOOL] instead.
         */
        companion object {
            fun parse(raw: String): ToolKind? {
                val normalised = raw.trim().lowercase().replace('-', '_').replace(' ', '_')
                return when (normalised) {
                    "read", "read_file", "readfile", "open", "cat", "view", "lesen" -> READ
                    "write", "write_file", "writefile", "edit", "create", "save", "schreiben" -> WRITE
                    "delete", "delete_file", "deletefile", "remove", "rm", "loeschen", "löschen" -> DELETE
                    "execute", "exec", "run", "command", "shell", "bash", "sh", "ausfuehren", "ausführen" -> EXECUTE
                    else -> null
                }
            }
        }
    }

    /** Who asked. This is what keeps a model from widening the boundary. */
    enum class ActionOrigin {
        /** The user, through the app UI. May extend the set of project areas. */
        USER,

        /** The model, through a tool call. May not extend anything. */
        MODEL,

        /** Not attributable — treated exactly like a model request. */
        UNKNOWN;

        val isUser: Boolean get() = this == USER
    }

    /** Why an action was refused. */
    enum class BoundaryDecision {
        /** Allowed. */
        ALLOWED,

        /** The tool name is not one we know. */
        UNKNOWN_TOOL,

        /** No project area is set at all. */
        NO_PROJECT_ROOT,

        /** The path leaves the project area. */
        OUTSIDE_PROJECT,

        /** The path is unusable (empty, control characters, NUL). */
        MALFORMED_PATH,

        /** The written path is inside, the real target is not. */
        SYMLINK_ESCAPE,

        /** The file may contain a secret. Never sendable, never writable. */
        SECRET_FILE,

        /** The access was withdrawn by the user. */
        ACCESS_REVOKED,

        /** A model tried to hand itself more roots. */
        GRANTS_NOT_FROM_USER,

        /** The command would reach outside the project or run a shell trick. */
        FORBIDDEN_COMMAND
    }

    /**
     * Additional project areas granted to the app.
     *
     * [grantedByUser] is the load-bearing field. It is set only when the user picked
     * the extra area in the app; a model cannot flip it, and a grant that carries
     * `false` contributes nothing.
     */
    data class BoundaryGrants(
        val extraRoots: List<String> = emptyList(),
        val grantedByUser: Boolean = true
    )

    /**
     * Withdrawn access. Paths are stored as they were handed in and compared after
     * normalisation. A directory in a set blocks everything below it.
     */
    data class Revocations(
        val read: Set<String> = emptySet(),
        val write: Set<String> = emptySet(),
        val delete: Set<String> = emptySet(),
        val execute: Set<String> = emptySet()
    ) {
        /** The revoked entries for [kind]. */
        fun forKind(kind: ToolKind): Set<String> = when (kind) {
            ToolKind.READ -> read
            ToolKind.WRITE -> write
            ToolKind.DELETE -> delete
            ToolKind.EXECUTE -> execute
        }

        val isEmpty: Boolean
            get() = read.isEmpty() && write.isEmpty() && delete.isEmpty() && execute.isEmpty()
    }

    /**
     * The complete boundary in force at this moment: the project areas, the extra
     * grants, and everything the user has withdrawn.
     */
    data class ProjectBoundary(
        val projectRoots: List<String> = emptyList(),
        val grants: BoundaryGrants = BoundaryGrants(),
        val revoked: Revocations = Revocations()
    ) {
        /**
         * The roots that actually count. A grant without [BoundaryGrants.grantedByUser]
         * is ignored here rather than at the call site, so no caller can forget.
         */
        fun effectiveRoots(): List<String> {
            val all = if (grants.grantedByUser) {
                projectRoots + grants.extraRoots
            } else {
                projectRoots
            }
            return all.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        }
    }

    /**
     * What a tool wants to do. Plain data, no behaviour.
     *
     * @param kind              Parsed tool kind, or null when unknown.
     * @param rawKind           The name as it arrived, for the error message.
     * @param path              Project-relative or absolute path for READ/WRITE/DELETE.
     * @param command           The command line for EXECUTE.
     * @param workingDirectory  Where the command runs; null means the project root.
     * @param origin            Who asked.
     * @param additionalRoots   Roots the action claims for itself. Honoured only for
     *                          [ActionOrigin.USER].
     * @param resolvedRealPath  Real target after following symlinks, passed on to
     *                          [PathBoundaryGuard].
     * @param realRoot          Real target of the project root, ditto.
     */
    data class ToolAction(
        val kind: ToolKind?,
        val rawKind: String = "",
        val path: String? = null,
        val command: String? = null,
        val workingDirectory: String? = null,
        val origin: ActionOrigin = ActionOrigin.MODEL,
        val additionalRoots: List<String> = emptyList(),
        val resolvedRealPath: String? = null,
        val realRoot: String? = null
    ) {
        companion object {
            /** Convenience for the common case: a file action on [path]. */
            fun file(kind: ToolKind, path: String, origin: ActionOrigin = ActionOrigin.MODEL) =
                ToolAction(kind = kind, rawKind = kind.name, path = path, origin = origin)

            /** Convenience for a command action. */
            fun exec(command: String, origin: ActionOrigin = ActionOrigin.MODEL) =
                ToolAction(
                    kind = ToolKind.EXECUTE,
                    rawKind = ToolKind.EXECUTE.name,
                    command = command,
                    origin = origin
                )
        }
    }

    /**
     * The answer. A refusal always carries a message; an allowed action carries none,
     * because there is nothing to explain.
     *
     * @param generation The boundary generation this verdict was made in. A permit a
     *                   caller kept from an earlier turn stops matching as soon as
     *                   the generation moves, which is what makes a revocation bite
     *                   immediately instead of at the next app start.
     */
    data class BoundaryVerdict(
        val decision: BoundaryDecision,
        val message: String,
        val kind: ToolKind?,
        val generation: Long
    ) {
        val isAllowed: Boolean get() = decision == BoundaryDecision.ALLOWED

        /** The message, safe to show and to log. */
        val displayMessage: String get() = message
    }

    /**
     * The one entry point. Every tool action goes through here — there is no second
     * function that could skip a check.
     *
     * @param action     What the tool wants.
     * @param boundary   The boundary in force.
     * @param generation Generation counter, passed in so the caller keeps ownership
     *                   of it and a verdict can be compared against a later revoke.
     */
    fun evaluate(
        action: ToolAction,
        boundary: ProjectBoundary,
        generation: Long = 0L
    ): BoundaryVerdict {
        val kind = action.kind
            ?: return refuse(
                BoundaryDecision.UNKNOWN_TOOL,
                "Unbekanntes Werkzeug „${clean(action.rawKind)}“. " +
                    "Es wird nicht ausgeführt, weil nicht klar ist, was es tun soll.",
                null,
                generation
            )

        val roots = boundary.effectiveRoots()
        if (roots.isEmpty()) {
            return refuse(
                BoundaryDecision.NO_PROJECT_ROOT,
                "Es ist kein Projektordner ausgewählt. Bitte zuerst einen Ordner wählen.",
                kind,
                generation
            )
        }

        // A model that names its own roots is refused before any path is even looked
        // at. Text in a conversation is not a permission.
        if (action.additionalRoots.isNotEmpty() && !action.origin.isUser) {
            return refuse(
                BoundaryDecision.GRANTS_NOT_FROM_USER,
                "Der Befehl wollte den Projektbereich selbst erweitern. " +
                    "Neue Ordner darf nur der Nutzer in der App auswählen.",
                kind,
                generation
            )
        }

        // A grant the user never confirmed contributes nothing. This is checked
        // inside effectiveRoots(); if a caller insists on it anyway we still refuse.
        val allRoots = roots + if (action.origin.isUser) action.additionalRoots else emptyList()
        val usableRoots = if (boundary.grants.grantedByUser) allRoots else roots

        return when (kind) {
            ToolKind.EXECUTE -> evaluateCommand(action, usableRoots, boundary, generation)
            ToolKind.READ, ToolKind.WRITE, ToolKind.DELETE ->
                evaluateFile(action, kind, usableRoots, boundary, generation)
        }
    }

    // ── File actions: read, write and delete share this one path ─────────────

    private fun evaluateFile(
        action: ToolAction,
        kind: ToolKind,
        roots: List<String>,
        boundary: ProjectBoundary,
        generation: Long
    ): BoundaryVerdict {
        val rawPath = action.path
        if (rawPath.isNullOrBlank()) {
            return refuse(
                BoundaryDecision.MALFORMED_PATH,
                "Für ${kindWord(kind)} wurde kein Dateipfad angegeben.",
                kind,
                generation
            )
        }

        // The path check is delegated, not reimplemented: PathBoundaryGuard owns the
        // traversal, absolute-path and symlink rules. Every file kind calls it the
        // same way, which is what "same protection for every tool kind" means here.
        val verdicts = roots.map { root ->
            PathBoundaryGuard.check(
                candidate = rawPath,
                projectRoot = root,
                resolvedRealPath = action.resolvedRealPath,
                realRoot = action.realRoot ?: root
            )
        }

        val passing = verdicts.firstOrNull { it.isAllowed }
            ?: return refuseFromPath(
                verdicts.firstOrNull { it.decision != PathBoundaryGuard.PathDecision.NO_ROOT }
                    ?: verdicts.first(),
                kind,
                generation
            )

        val matchingRoot = roots[verdicts.indexOf(passing)]
        val relative = relativeTo(matchingRoot, rawPath)

        // Revocation wins over everything else: withdrawn is withdrawn.
        if (isRevoked(boundary.revoked.forKind(kind), absoluteOf(matchingRoot, rawPath))) {
            return refuse(
                BoundaryDecision.ACCESS_REVOKED,
                "Der Zugriff auf „${clean(rawPath)}“ wurde widerrufen. " +
                    "Diese Datei wird für ${kindWord(kind)} nicht mehr benutzt.",
                kind,
                generation
            )
        }

        // A secret is refused on every file kind, not just when sending.
        val secret = ProjectExclusionPolicy.classify(relative, exists = true)
        if (secret.decision == ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET) {
            return refuse(
                BoundaryDecision.SECRET_FILE,
                secretMessage(rawPath, kind),
                kind,
                generation
            )
        }

        return BoundaryVerdict(BoundaryDecision.ALLOWED, "", kind, generation)
    }

    private fun refuseFromPath(
        verdict: PathBoundaryGuard.PathVerdict,
        kind: ToolKind,
        generation: Long
    ): BoundaryVerdict {
        val decision = when (verdict.decision) {
            PathBoundaryGuard.PathDecision.OUTSIDE_PROJECT -> BoundaryDecision.OUTSIDE_PROJECT
            PathBoundaryGuard.PathDecision.SYMLINK_ESCAPE -> BoundaryDecision.SYMLINK_ESCAPE
            PathBoundaryGuard.PathDecision.MALFORMED -> BoundaryDecision.MALFORMED_PATH
            PathBoundaryGuard.PathDecision.NO_ROOT -> BoundaryDecision.NO_PROJECT_ROOT
            PathBoundaryGuard.PathDecision.ALLOWED -> BoundaryDecision.OUTSIDE_PROJECT
        }
        val text = if (verdict.message.isBlank()) {
            "Der Pfad liegt nicht im ausgewählten Projektordner. ${kindWord(kind)} wurde abgelehnt."
        } else {
            verdict.message
        }
        return refuse(decision, text, kind, generation)
    }

    // ── Commands: the same boundary, plus no shell escape ────────────────────

    private fun evaluateCommand(
        action: ToolAction,
        roots: List<String>,
        boundary: ProjectBoundary,
        generation: Long
    ): BoundaryVerdict {
        val command = action.command
        if (command.isNullOrBlank()) {
            return refuse(
                BoundaryDecision.MALFORMED_PATH,
                "Für das Ausführen wurde kein Befehl angegeben.",
                ToolKind.EXECUTE,
                generation
            )
        }

        // Where the command runs must be a project area too, or a command could sit
        // in /data/data of another app and write there.
        //
        // The direction of the comparison is the whole point: the working directory is
        // the *candidate* and each project root is the root it must sit under. Checking
        // a root against the working directory (the other way round) accepts "/" —
        // every path is trivially below it — and refuses a legitimate subdirectory
        // such as "<root>/app", which is exactly the case a real build needs.
        val workDir = action.workingDirectory?.trim()
        if (!workDir.isNullOrEmpty()) {
            val workDirAllowed = roots.any { root ->
                PathBoundaryGuard.check(candidate = workDir, projectRoot = root).isAllowed
            }
            if (!workDirAllowed) {
                return refuse(
                    BoundaryDecision.OUTSIDE_PROJECT,
                    "Der Arbeitsordner „${clean(workDir)}“ liegt außerhalb des " +
                        "Projektordners. Der Befehl wurde nicht ausgeführt.",
                    ToolKind.EXECUTE,
                    generation
                )
            }
        }

        // A command carries its own paths, and those are read and written just like a
        // tool's `path` field. Without this, "cat /data/data/com.other.app/…/prefs.xml"
        // would pass the boundary that the very same file is refused by as a READ
        // action — the same target, two verdicts, depending on how it was named.
        // Every absolute path in the command must therefore sit under a project root.
        for (token in pathTokensOf(command)) {
            val insideProject = roots.any { root ->
                PathBoundaryGuard.check(candidate = token, projectRoot = root).isAllowed
            }
            if (!insideProject) {
                return refuse(
                    BoundaryDecision.OUTSIDE_PROJECT,
                    "Der Befehl greift auf „${clean(token)}“ zu. Das liegt außerhalb des " +
                        "Projektordners, deshalb wurde der Befehl nicht ausgeführt.",
                    ToolKind.EXECUTE,
                    generation
                )
            }
        }

        FORBIDDEN_IN_COMMAND.forEach { rule ->
            if (rule.pattern.containsMatchIn(command)) {
                return refuse(
                    BoundaryDecision.FORBIDDEN_COMMAND,
                    rule.explanation,
                    ToolKind.EXECUTE,
                    generation
                )
            }
        }

        // A command may not touch a file that may hold a secret, in either direction.
        val mentionedSecret = command.split(' ', '\t', '\n', '|', ';')
            .map { it.substringAfterLast('/').trim() }
            .filter { it.isNotEmpty() }
            .firstOrNull { name ->
                ProjectExclusionPolicy.classify(name, exists = true).decision ==
                    ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET
            }
        if (mentionedSecret != null) {
            return refuse(
                BoundaryDecision.SECRET_FILE,
                "Der Befehl greift auf „${clean(mentionedSecret)}“ zu. " +
                    "Dateien mit möglichen Geheimnissen werden weder gelesen noch geschrieben.",
                ToolKind.EXECUTE,
                generation
            )
        }

        if (isRevoked(boundary.revoked.execute, workDir ?: roots.firstOrNull().orEmpty())) {
            return refuse(
                BoundaryDecision.ACCESS_REVOKED,
                "Das Ausführen von Befehlen in diesem Bereich wurde widerrufen.",
                ToolKind.EXECUTE,
                generation
            )
        }

        return BoundaryVerdict(BoundaryDecision.ALLOWED, "", ToolKind.EXECUTE, generation)
    }

    // ── Refusals and messages ────────────────────────────────────────────────

    private fun refuse(
        decision: BoundaryDecision,
        message: String,
        kind: ToolKind?,
        generation: Long
    ) = BoundaryVerdict(decision, SecretMasker.redact(message), kind, generation)

    /** The German wording for one refusal reason, per file tool kind. */
    private fun secretMessage(path: String, kind: ToolKind): String = when (kind) {
        ToolKind.READ ->
            "„${clean(path)}“ wird nicht gelesen und nicht gesendet, weil die Datei " +
                "Geheimnisse enthalten kann."
        ToolKind.WRITE ->
            "In „${clean(path)}“ wird nichts geschrieben. Dateien mit möglichen " +
                "Geheimnissen bleiben unangetastet."
        ToolKind.DELETE ->
            "„${clean(path)}“ wird nicht gelöscht. Dateien mit möglichen Geheimnissen " +
                "bleiben erhalten."
        ToolKind.EXECUTE ->
            "„${clean(path)}“ wird nicht verwendet. Dateien mit möglichen Geheimnissen " +
                "bleiben unangetastet."
    }

    private fun kindWord(kind: ToolKind): String = when (kind) {
        ToolKind.READ -> "Lesen"
        ToolKind.WRITE -> "Schreiben"
        ToolKind.DELETE -> "Löschen"
        ToolKind.EXECUTE -> "Ausführen"
    }

    /** Anything echoed back to the user or into a log passes the redactor. */
    private fun clean(value: String): String = SecretMasker.redact(value.trim())

    // ── Small helpers ────────────────────────────────────────────────────────

    /**
     * True when [absolute] equals a revoked entry or lies below one, so revoking a
     * folder revokes its contents.
     */
    private fun isRevoked(revoked: Set<String>, absolute: String): Boolean {
        if (revoked.isEmpty()) return false
        val target = strip(absolute)
        if (target.isEmpty()) return false
        revoked.forEach { entry ->
            val blocked = strip(entry)
            if (blocked.isNotEmpty() && (target == blocked || target.startsWith("$blocked/"))) {
                return true
            }
        }
        return false
    }

    private fun strip(path: String): String =
        path.trim().replace('\\', '/').split('/').filter { it.isNotEmpty() && it != "." }
            .joinToString("/")

    /**
     * The absolute paths a command mentions, so each one can be put through the same
     * boundary check a file tool gets.
     *
     * Shell quoting is not re-implemented here — that would be a second, weaker
     * parser. Anything quoted is skipped instead, so an odd construct is left to the
     * FORBIDDEN_IN_COMMAND rules rather than being half-read.
     */
    private fun pathTokensOf(command: String): List<String> {
        // Segments between single quotes are the quoted ones and are dropped: the
        // even indices are what the shell would actually expose as plain words.
        //
        // Note this used to be `split('\'').joinToString(" ") { "" }`, which returns
        // an *empty* string for a command that contains no quote at all — so every
        // ordinary command tokenised to nothing and the check below silently never
        // ran. Joining only the unquoted segments avoids that failure mode.
        val segments = command.split('\'')
        val unquoted = segments.filterIndexed { index, _ -> index % 2 == 0 }.joinToString(" ")
        return unquoted.split(' ', '\t', '\n', ';', '|', '&', '<', '>', '(', ')', ',')
            .map { it.trim() }
            .filter { it.startsWith("/") && it.length > 1 }
            .distinct()
    }

    /** Absolute form of [path] against [root], for revocation comparison. */
    private fun absoluteOf(root: String, path: String): String {
        val cleanPath = strip(path)
        return if (cleanPath.startsWith("/")) cleanPath else "${strip(root)}/$cleanPath"
    }

    /** The path as seen from inside [root], for [ProjectExclusionPolicy]. */
    private fun relativeTo(root: String, path: String): String {
        val cleanRoot = strip(root)
        val cleanPath = strip(path)
        return when {
            cleanRoot.isEmpty() -> cleanPath
            cleanPath.startsWith("$cleanRoot/") -> cleanPath.removePrefix("$cleanRoot/")
            else -> cleanPath
        }
    }

    /**
     * Shell constructs that would leave the project area or hand the model a second
     * interpreter. Each rule carries its own German explanation so the refusal names
     * what was found.
     */
    private val FORBIDDEN_IN_COMMAND: List<CommandRule> = listOf(
        CommandRule(
            Regex("""(^|[\s"'=])\.\.(/|\s|$)"""),
            "Der Befehl steigt mit „..“ aus dem Projektordner. Das ist nicht erlaubt."
        ),
        CommandRule(
            Regex("""(^|[\s"'=])~(/|\s|$)"""),
            "Der Befehl benutzt „~“ für das Benutzerverzeichnis. Das liegt außerhalb " +
                "des Projektordners."
        ),
        CommandRule(
            Regex("""\$\(|`"""),
            "Der Befehl ersetzt Text durch den Befehl „$(…)“. Das umgeht die " +
                "Projektgrenze und wurde abgelehnt."
        ),
        CommandRule(
            Regex("""(^|[\s;|&(])(cd|pushd)\s+"""),
            "Der Befehl wechselt den Ordner. Er darf nur im Projektordner bleiben und " +
                "darf keinen Ordnerwechsel enthalten."
        ),
        CommandRule(
            Regex("""\bsudo\b|\bsu\s|\bmount\b|\bchmod\s+777\b|\bchown\b"""),
            "Der Befehl darf keine Rechte ändern und keine Ordner einbinden."
        ),
        CommandRule(
            Regex("""https?://"""),
            "Der Befehl darf nicht ins Netz gehen. Das Netz ist vom Werkzeug getrennt."
        ),
        CommandRule(
            Regex("""\b(curl|wget|nc|netcat|ssh|scp|ftp)\b"""),
            "Der Befehl darf keine Daten übertragen oder nach außen verbinden."
        )
    )
}

/**
 * One forbidden command pattern with the sentence shown when it matches.
 */
private data class CommandRule(
    val pattern: Regex,
    val explanation: String
)

/**
 * Holds the boundary and the generation counter, so revoking access takes effect at
 * once instead of at the next app start.
 *
 * Small on purpose: the real state (folder picker, persisted revocations) lives in
 * the feature layer. This class only remembers what is in force *now* and hands out
 * generation numbers, which is the part the gate depends on.
 *
 * Not thread-safe by design for its mutable part; all members are marked accordingly
 * and callers on the main thread are the intended use.
 */
class ProjectAccessRegistry(
    boundary: ProjectBoundaryEnforcer.ProjectBoundary =
        ProjectBoundaryEnforcer.ProjectBoundary()
) {

    private var current: ProjectBoundaryEnforcer.ProjectBoundary = boundary

    private var generationCounter: Long = 0L

    /** The boundary in force, as an immutable snapshot. */
    @Synchronized
    fun current(): ProjectBoundaryEnforcer.ProjectBoundary = current

    /** The current generation. A verdict from an older generation is stale. */
    @Synchronized
    fun generation(): Long = generationCounter

    /**
     * Runs [action] against the boundary in force and returns the verdict.
     * Every tool kind goes through here, which is what keeps the checks equal.
     */
    @Synchronized
    fun check(action: ProjectBoundaryEnforcer.ToolAction): ProjectBoundaryEnforcer.BoundaryVerdict =
        ProjectBoundaryEnforcer.evaluate(action, current, generationCounter)

    /**
     * Replaces the boundary. A changed boundary always moves the generation, so a
     * permit a caller kept from before stops being valid immediately.
     */
    @Synchronized
    fun update(newBoundary: ProjectBoundaryEnforcer.ProjectBoundary) {
        current = newBoundary
        generationCounter += 1
    }

    /**
     * Withdraws [path] for [kind]. The next [check] on that path is refused, and any
     * permit from before becomes stale.
     */
    @Synchronized
    fun revoke(kind: ProjectBoundaryEnforcer.ToolKind, path: String) {
        val old = current.revoked
        val widened = when (kind) {
            ProjectBoundaryEnforcer.ToolKind.READ -> old.copy(read = old.read + path)
            ProjectBoundaryEnforcer.ToolKind.WRITE -> old.copy(write = old.write + path)
            ProjectBoundaryEnforcer.ToolKind.DELETE -> old.copy(delete = old.delete + path)
            ProjectBoundaryEnforcer.ToolKind.EXECUTE -> old.copy(execute = old.execute + path)
        }
        update(current.copy(revoked = widened))
    }

    /** True while [verdict] still reflects the boundary as it stands. */
    @Synchronized
    fun isStillValid(verdict: ProjectBoundaryEnforcer.BoundaryVerdict): Boolean =
        verdict.generation == generationCounter
}