package org.claudroide.app.feature.project

/**
 * Decides which project files may be sent to an AI provider as context.
 *
 * Task 067 is a security gate. Its rule is deliberately conservative: a file is
 * excluded unless it is positively known to be safe. Anything unknown is excluded,
 * because a false "safe" leaks a secret and a false "excluded" only costs the user
 * one tap to override.
 *
 * Invariants:
 *  - Secrets, key material and environment files are never sendable, and no manual
 *    override can unlock them. There is no `force` that defeats this list.
 *  - Exclusions apply identically to search, context building and agent tools —
 *    a single policy object, so no caller can pick a laxer path.
 *  - A missing file is explained in plain German rather than silently skipped.
 *  - Heavy directories are excluded by default to protect an 8 GB device.
 */
object ProjectExclusionPolicy {

    /**
     * File names that are never sent, whatever the user does.
     *
     * Matched case-insensitively against the file name only, so a project folder
     * called "secrets-guide.md" is not caught by accident.
     */
    private val blockedFileNames: Set<String> = setOf(
        ".env", ".env.local", ".env.production", ".env.development",
        "id_rsa", "id_dsa", "id_ecdsa", "id_ed25519",
        "credentials", "credentials.json",
        ".npmrc", ".pypirc", ".netrc",
        ".htpasswd", ".pgpass", ".git-credentials",
        "secrets.yaml", "secrets.yml", "secrets.json",
        "keystore.properties", "local.properties"
    )

    /** Extensions that indicate key or certificate material. */
    private val blockedExtensions: Set<String> = setOf(
        "pem", "key", "p12", "pfx", "jks", "keystore", "asc", "gpg", "ppk"
    )

    /**
     * Trailing suffixes people use when they copy a secret aside: `.env.bak`,
     * `id_rsa.old`, `server.pem~`. A backup of a secret is still the secret, so the
     * base name is checked after removing these.
     */
    private val backupSuffixes: List<String> = listOf(
        "bak", "backup", "old", "orig", "copy", "saved", "save", "tmp", "temp",
        "1", "2", "0", "dist", "example", "sample", "tpl", "txt", "swp"
    )

    /**
     * True when a file name denotes a known secret, including the backup and copy
     * variants that exact-name matching would miss.
     *
     * Matching stays on the file name, so `docs/env-guide.md` is not caught: only a
     * name whose *stem* is a blocked secret counts.
     */
    private fun isSecretFileName(lowerName: String): Boolean {
        if (lowerName in blockedFileNames) return true

        // Strip a single trailing backup suffix and re-check the remaining name.
        val dot = lowerName.lastIndexOf('.')
        if (dot <= 0) return false
        val stem = lowerName.substring(0, dot)
        val suffix = lowerName.substring(dot + 1)
        if (suffix !in backupSuffixes) return false

        return stem in blockedFileNames || stem.substringAfterLast('.', "") in blockedFileNames
    }

    /**
     * Directories excluded for size, not for secrecy.
     *
     * On an 8 GB device, walking build output or a dependency tree is the fastest way
     * to make the app unusable, and none of it is useful context.
     */
    private val heavyDirectories: Set<String> = setOf(
        "build", ".gradle", ".git", ".idea", ".vscode",
        "node_modules", "vendor", "dist", "out", "target",
        ".next", ".cache", "coverage", "__pycache__", ".venv", "venv"
    )

    /** Files above this size are excluded; the content is truncated or skipped. */
    const val MAX_FILE_SIZE_BYTES = 512L * 1024L

    /** How a single file is treated. */
    enum class FileDecision {
        /** May be sent. */
        ALLOWED,

        /** Blocked as a secret or key; cannot be overridden. */
        BLOCKED_SECRET,

        /** Blocked because of its location or size. */
        BLOCKED_HEAVY,

        /** The path does not exist. */
        NOT_FOUND
    }

    /** Why a file was excluded, in language a user can act on. */
    data class ExclusionReason(
        val decision: FileDecision,
        val message: String
    )

    /**
     * Classifies a single project-relative path.
     *
     * @param path         Project-relative path, e.g. "app/src/Main.kt".
     * @param exists       Whether the file is actually there.
     * @param sizeBytes    File size, or null when unknown.
     */
    fun classify(path: String, exists: Boolean = true, sizeBytes: Long? = null): ExclusionReason {
        val normalised = path.trim().replace('\\', '/').trimStart('/')

        if (normalised.isEmpty()) {
            return ExclusionReason(
                FileDecision.NOT_FOUND,
                "Der Pfad ist leer."
            )
        }

        if (!exists) {
            return ExclusionReason(
                FileDecision.NOT_FOUND,
                "Die Datei „$path“ wurde im Projekt nicht gefunden. " +
                    "Vielleicht wurde sie verschoben oder gelöscht."
            )
        }

        val segments = normalised.split('/').filter { it.isNotEmpty() }
        val fileName = segments.lastOrNull().orEmpty()
        val lowerName = fileName.lowercase()
        val lowerExt = lowerName.substringAfterLast('.', "")

        // Order matters: a secret is reported as a secret even if it also sits in a
        // heavy directory, because "this contains a key" is the more useful message.
        if (isSecretFileName(lowerName)) {
            return ExclusionReason(
                FileDecision.BLOCKED_SECRET,
                "„$fileName“ wird nie an einen Anbieter gesendet, weil die Datei " +
                    "Geheimnisse enthalten kann."
            )
        }

        if (lowerExt in blockedExtensions) {
            return ExclusionReason(
                FileDecision.BLOCKED_SECRET,
                "„$fileName“ wird nie gesendet: Schlüssel- und Zertifikatsdateien " +
                    "gehören nicht in den KI-Kontext."
            )
        }

        val heavySegment = segments.dropLast(1).firstOrNull { it in heavyDirectories }
        if (heavySegment != null) {
            return ExclusionReason(
                FileDecision.BLOCKED_HEAVY,
                "„$path“ liegt im Ordner „$heavySegment“. Der wird ausgelassen, " +
                    "weil er groß ist und keine nützlichen Inhalte hat."
            )
        }

        if (sizeBytes != null && sizeBytes > MAX_FILE_SIZE_BYTES) {
            return ExclusionReason(
                FileDecision.BLOCKED_HEAVY,
                "„$fileName“ ist größer als 512 KB und wird nicht vollständig gesendet."
            )
        }

        return ExclusionReason(FileDecision.ALLOWED, "")
    }

    /**
     * Filters a batch of paths, returning only the sendable ones.
     *
     * The excluded list is returned as well so the UI can show a preview — the user
     * must be able to see what was left out, not just what went out.
     */
    fun partition(paths: List<String>): ExclusionPreview {
        val included = mutableListOf<String>()
        val excluded = mutableListOf<ExclusionReason>()

        paths.forEach { path ->
            val reason = classify(path, exists = true)
            if (reason.decision == FileDecision.ALLOWED) {
                included += path
            } else {
                excluded += reason
            }
        }

        return ExclusionPreview(
            included = included,
            excluded = excluded,
            summary = buildSummary(included.size, excluded)
        )
    }

    /**
     * Whether a manual exception may unlock this exclusion.
     *
     * Heavy folders and oversized files may be included by hand. Secrets never can:
     * there is deliberately no code path that sends them.
     */
    fun canOverride(decision: FileDecision): Boolean = when (decision) {
        FileDecision.BLOCKED_SECRET -> false
        FileDecision.BLOCKED_HEAVY -> true
        FileDecision.NOT_FOUND -> false
        FileDecision.ALLOWED -> false
    }

    private fun buildSummary(includedCount: Int, excluded: List<ExclusionReason>): String {
        if (excluded.isEmpty()) {
            return "Alle $includedCount Dateien dürfen gesendet werden."
        }
        val secrets = excluded.count { it.decision == FileDecision.BLOCKED_SECRET }
        val rest = excluded.size - secrets

        val parts = mutableListOf<String>()
        if (secrets > 0) {
            parts += "$secrets Datei(en) mit möglichen Geheimnissen"
        }
        if (rest > 0) {
            parts += "$rest Datei(en) ausgelassen"
        }
        return "$includedCount Datei(en) sendbar, " +
            parts.joinToString(" und ") + " ausgeschlossen."
    }
}

/**
 * The result of applying the exclusion policy to a set of paths.
 */
data class ExclusionPreview(
    val included: List<String>,
    val excluded: List<ProjectExclusionPolicy.ExclusionReason>,
    val summary: String
) {
    /** True when something was held back, so the UI can show a warning badge. */
    val hasExclusions: Boolean get() = excluded.isNotEmpty()

    /** Count of files blocked for security reasons specifically. */
    val secretCount: Int
        get() = excluded.count { it.decision == ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET }
}
