package org.claudroide.app.feature.agent

/**
 * Validates shell commands for safety before suggesting them to the user or
 * executing them in a terminal environment.
 *
 * Classification:
 * - SAFE: Can be copied/suggested freely (ls, pwd, gradlew assembleDebug, npm run dev)
 * - CAUTION: Requires warning dialog before copying (npm install, git reset, gradlew clean)
 * - BLOCKED: Never suggested or executed (rm -rf, sudo, curl|sh, API keys in command)
 *
 * This is a pure Kotlin validator — no Android, no network, no filesystem.
 * The caller (UI, TerminalExecutor, or agent) decides what to do with the result.
 */
object CommandRiskScanner {

    /** The risk level of a command. */
    enum class RiskLevel(val label: String, val germanLabel: String, val icon: String) {
        SAFE("Safe", "Sicher", "✅") {
            override val requiresConfirmation: Boolean = false
        },
        CAUTION("Caution", "Vorsicht", "⚠️") {
            override val requiresConfirmation: Boolean = true
        },
        BLOCKED("Blocked", "Blockiert", "🚫") {
            override val requiresConfirmation: Boolean = false
        };

        abstract val requiresConfirmation: Boolean
    }

    /** Result of scanning a command. */
    data class ScanResult(
        val level: RiskLevel,
        val command: String,
        val reasons: List<String> = emptyList(),
        val suggestedAlternative: String? = null
    ) {
        val isAllowed: Boolean get() = level != RiskLevel.BLOCKED
        val summary: String get() = "${level.icon} ${level.germanLabel}: $command" +
            if (reasons.isNotEmpty()) " — ${reasons.joinToString("; ")}" else ""
    }

    /** Scans a single command line and returns its risk level. */
    fun scan(command: String): ScanResult {
        val trimmed = command.trim()
        if (trimmed.isBlank()) {
            return ScanResult(RiskLevel.SAFE, trimmed, listOf("Empty command"))
        }

        // Check for BLOCKED patterns first (highest priority)
        val blockedReason = checkBlocked(trimmed)
        if (blockedReason != null) {
            return ScanResult(RiskLevel.BLOCKED, trimmed, listOf(blockedReason))
        }

        // Check for CAUTION patterns
        val cautionReasons = checkCaution(trimmed)
        if (cautionReasons.isNotEmpty()) {
            return ScanResult(RiskLevel.CAUTION, trimmed, cautionReasons)
        }

        // Check for SAFE patterns (known safe commands)
        if (isKnownSafe(trimmed)) {
            return ScanResult(RiskLevel.SAFE, trimmed)
        }

        // Unknown command — default to CAUTION
        return ScanResult(RiskLevel.CAUTION, trimmed, listOf("Unknown command — review before executing"))
    }

    /** Scans multiple commands (e.g., from a script or command queue). */
    fun scanAll(commands: List<String>): List<ScanResult> = commands.map { scan(it) }

    /** Overall risk for a batch — highest level wins. */
    fun batchRisk(results: List<ScanResult>): RiskLevel =
        results.maxByOrNull { it.level.ordinal }?.level ?: RiskLevel.SAFE

    // ── Blocked Patterns ────────────────────────────────────────────────────

    private fun checkBlocked(cmd: String): String? {
        val lower = cmd.lowercase()

        // Destructive filesystem operations
        if (lower.contains("rm -rf") || lower.contains("rm -r") && lower.contains("/")) {
            return "Recursive force delete — irreversible data loss"
        }
        if (lower.matches("^rm\\s+.*\\*")) {
            return "Wildcard delete — matches multiple files"
        }
        if (lower.contains("mkfs") || lower.contains("format") || lower.contains("dd if=")) {
            return "Filesystem format/disk write — destroys data"
        }
        if (lower.contains("> /dev/") || lower.contains("> /proc/") || lower.contains("> /sys/")) {
            return "Write to kernel/device filesystem — system instability"
        }

        // Privilege escalation
        if (lower.startsWith("sudo ") || lower.contains(" sudo ") || lower.contains("su ")) {
            return "Privilege escalation — not available in Android sandbox"
        }
        if (lower.contains("chmod 777") || lower.contains("chown root")) {
            return "Permission change to world-writable/root — security risk"
        }

        // Shell injection vectors
        if (lower.contains("curl |") && (lower.contains("sh") || lower.contains("bash"))) {
            return "Pipe to shell — remote code execution"
        }
        if (lower.contains("wget |") && (lower.contains("sh") || lower.contains("bash"))) {
            return "Pipe to shell — remote code execution"
        }
        if (lower.contains("eval ") || lower.contains("exec ")) {
            return "Dynamic code execution — injection risk"
        }
        if (lower.contains("`") || lower.contains("\$(")) {
            return "Command substitution — injection risk"
        }

        // Secret leakage
        if (lower.contains("sk-") || lower.contains("ghp_") || lower.contains("gho_") ||
            lower.contains("github_pat_") || lower.contains("AIza") || lower.contains("AKIA") ||
            lower.contains("xoxb-") || lower.contains("xoxp-") || lower.contains("nvapi-") ||
            lower.matches(".*[a-zA-Z0-9]{32,}.*") && (lower.contains("key") || lower.contains("token") || lower.contains("secret"))) {
            return "Possible API key/secret in command — never include credentials"
        }

        // Network exposure
        if (lower.contains("nc -l") || lower.contains("netcat -l") || lower.contains("socat ")) {
            return "Network listener — opens port"
        }
        if (lower.contains("ssh -R") || lower.contains("ssh -L") || lower.contains("ssh -D")) {
            return "SSH tunneling — network exposure"
        }

        // Process manipulation
        if (lower.contains("kill -9") || lower.contains("pkill -9")) {
            return "Force kill — may corrupt state"
        }
        if (lower.contains("reboot") || lower.contains("shutdown") || lower.contains("init ")) {
            return "System power control — not available"
        }

        return null
    }

    // ── Caution Patterns ────────────────────────────────────────────────────

    private fun checkCaution(cmd: String): List<String> {
        val reasons = mutableListOf<String>()
        val lower = cmd.lowercase()

        // Package installation
        if (lower.startsWith("npm install") || lower.startsWith("npm i ") ||
            lower.startsWith("yarn add") || lower.startsWith("pnpm add")) {
            reasons += "Installs packages — may run postinstall scripts, downloads from network"
        }
        if (lower.startsWith("pip install") || lower.startsWith("poetry add")) {
            reasons += "Installs Python packages — may execute setup.py"
        }
        if (lower.startsWith("cargo install") || lower.startsWith("cargo add")) {
            reasons += "Installs Rust packages — compiles from source"
        }
        if (lower.contains("gradle") && (lower.contains("wrapper") || lower.contains("init"))) {
            reasons += "Modifies Gradle configuration — may change build behavior"
        }

        // Git destructive operations
        if (lower.contains("git reset --hard") || lower.contains("git reset -h")) {
            reasons += "Hard reset — discards uncommitted changes"
        }
        if (lower.contains("git clean -f") || lower.contains("git clean -fd")) {
            reasons += "Removes untracked files — irreversible"
        }
        if (lower.contains("git push --force") || lower.contains("git push -f")) {
            reasons += "Force push — rewrites remote history"
        }
        if (lower.contains("git rebase") && !lower.contains("--abort") && !lower.contains("--continue")) {
            reasons += "Rebase — rewrites commit history"
        }
        if (lower.contains("git branch -d") || lower.contains("git branch -D")) {
            reasons += "Deletes branch — may lose commits"
        }

        // Build/clean operations
        if (lower.contains("gradlew clean") || lower.contains("gradle clean")) {
            reasons += "Clean build — removes all build artifacts, next build slower"
        }
        if (lower.contains("make clean") || lower.contains("cargo clean")) {
            reasons += "Clean build — removes all build artifacts"
        }

        // Configuration changes
        if (lower.contains("git config") && (lower.contains("--global") || lower.contains("--system"))) {
            reasons += "Modifies global/system Git config — affects other projects"
        }
        if (lower.contains("ssh-keygen") || lower.contains("gpg --gen-key")) {
            reasons += "Generates cryptographic keys — review key type and passphrase"
        }

        // Network operations
        if (lower.contains("curl ") && !lower.contains("| sh") && !lower.contains("| bash")) {
            reasons += "HTTP request — may send data to external server"
        }
        if (lower.contains("wget ")) {
            reasons += "Downloads file — verify URL and destination"
        }
        if (lower.contains("scp ") || lower.contains("rsync ")) {
            reasons += "File transfer — verify source and destination"
        }

        // Environment modification
        if (lower.contains("export ") && (lower.contains("PATH=") || lower.contains("HOME="))) {
            reasons += "Modifies critical environment variables"
        }
        if (lower.contains("source ") || lower.contains(". ")) {
            reasons += "Sources file — executes its contents in current shell"
        }

        // Long-running processes
        if (lower.contains("npm run dev") || lower.contains("yarn dev") || lower.contains("vite") ||
            lower.contains("webpack serve") || lower.contains("gradlew run") ||
            lower.contains("cargo run") || lower.contains("python -m http.server")) {
            reasons += "Starts long-running server — will occupy terminal"
        }

        return reasons
    }

    // ── Known Safe Commands ─────────────────────────────────────────────────

    private val SAFE_PREFIXES = setOf(
        "ls", "pwd", "cd ", "cat ", "head ", "tail ", "less ", "more ",
        "grep ", "rg ", "find ", "locate ", "which ", "whereis ",
        "echo ", "printf ", "date ", "whoami ", "id ", "uname ",
        "df ", "du ", "free ", "uptime ", "top ", "htop ", "ps ",
        "git status", "git diff", "git log", "git show", "git branch", "git tag",
        "git stash", "git fetch", "git pull", "git merge --no-ff",
        "gradlew assembleDebug", "gradlew testDebugUnitTest", "gradlew lintDebug",
        "gradlew compileDebugKotlin", "gradlew koverReport",
        "./gradlew ", "gradle ",
        "npm run lint", "npm run typecheck", "npm run test", "npm test",
        "cargo check", "cargo test", "cargo build",
        "python -m pytest", "pytest ", "ruff check", "mypy ",
        "kotlinc ", "javac ", "java -version", "node --version",
        "git config --list", "git config --local", "git config user.name", "git config user.email"
    )

    private val SAFE_EXACT = setOf(
        "ls -la", "ls -l", "ls -a",
        "pwd",
        "git status",
        "git diff",
        "git log --oneline -10",
        "git branch -a",
        "gradle tasks",
        "gradlew tasks",
        "npm list",
        "cargo tree"
    )

    private fun isKnownSafe(cmd: String): Boolean {
        val trimmed = cmd.trim()
        val lower = trimmed.lowercase()

        // Exact matches
        if (SAFE_EXACT.contains(trimmed) || SAFE_EXACT.contains(lower)) {
            return true
        }

        // Prefix matches (allow with args)
        return SAFE_PREFIXES.any { trimmed.startsWith(it) || lower.startsWith(it) }
    }

    companion object {
        /** For testing: exposes checkBlocked for unit tests. */
        @VisibleForTesting
        internal fun checkBlockedForTest(cmd: String): String? = checkBlocked(cmd)

        /** For testing: exposes checkCaution for unit tests. */
        @VisibleForTesting
        internal fun checkCautionForTest(cmd: String): List<String> = checkCaution(cmd)

        /** For testing: exposes isKnownSafe for unit tests. */
        @VisibleForTesting
        internal fun isKnownSafeForTest(cmd: String): Boolean = isKnownSafe(cmd)
    }
}

// For internal test visibility
internal annotation class VisibleForTesting