package org.claudroide.app.feature.provider

data class RedactionAuditReport(
    val scrubbedMatchesCount: Int,
    val matchedPatternNames: List<String>,
    val sanitizedOutput: String
)

/**
 * Central secret redactor protecting logs, errors, previews, and exports.
 * Scrubs Anthropic keys, OpenAI/OpenRouter keys, Bearer headers, passwords, and private certificates.
 */
object SecretMasker {

    /** The single marker every scrubbed credential is replaced with. */
    const val REDACTION_PLACEHOLDER: String = "[REDACTED]"

    private val SCRUB_PATTERNS = listOf(
        "Anthropic API Key" to Regex("""(?i)\bsk-ant-[a-zA-Z0-9_\-]{8,}\b"""),
        // 8 rather than 16: real keys vary in length, and a key that leaked into a
        // label must be scrubbed even when it is shorter than one vendor's minimum.
        // A false positive only costs a readable word in a diagnostic line.
        "OpenAI / OpenRouter Key" to Regex("""(?i)\bsk-[a-zA-Z0-9_\-]{8,}\b"""),
        "Bearer Auth Header" to Regex("""(?i)\bbearer\s+[a-zA-Z0-9\-_\.]{16,}\b"""),
        "Private Key Block" to Regex("""-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----"""),
        // Also catches a credential glued behind a prefix, e.g. "modell-token=sk-live-…",
        // where the assignment name is not a word the list above knows. The value must
        // look like a key: a plain word after a colon is ordinary prose ("Anbieter: X")
        // and must survive, otherwise every German label would be redacted.
        "Sensitive Assignment" to Regex(
            """(?i)\b(?:api[_-]?key|password|client_secret|access_token)\s*[=:]\s*['"]?([^\s'"&,]{4,})['"]?"""
        ),
        "Prefixed Key" to Regex("""(?i)\b[a-zA-Z_][a-zA-Z0-9_.\-]*\s*=\s*(sk-[a-zA-Z0-9_\-]{8,})""")
    )

    /**
     * Sanitizes input text, replacing all recognized credentials with [REDACTED].
     */
    fun redact(input: String): String {
        if (input.isBlank()) return input
        var current = input
        for ((_, regex) in SCRUB_PATTERNS) {
            current = regex.replace(current, REDACTION_PLACEHOLDER)
        }
        return current
    }

    /**
     * True when [input] contains something [redact] would change.
     *
     * Used to refuse showing a value that carries a credential in it, rather than
     * showing a masked version that is still misleading about what was stored.
     */
    fun containsSecretLikeText(input: String): Boolean =
        input.isNotBlank() && redact(input) != input

    /**
     * Audits an input string, detailing which sensitive patterns were intercepted.
     */
    fun auditAndRedact(input: String): RedactionAuditReport {
        var count = 0
        val matchedNames = mutableListOf<String>()
        var current = input

        for ((name, regex) in SCRUB_PATTERNS) {
            val matches = regex.findAll(current).toList()
            if (matches.isNotEmpty()) {
                count += matches.size
                matchedNames.add(name)
                current = regex.replace(current, REDACTION_PLACEHOLDER)
            }
        }

        return RedactionAuditReport(
            scrubbedMatchesCount = count,
            matchedPatternNames = matchedNames,
            sanitizedOutput = current
        )
    }

    /**
     * Defense-in-depth invariant:
     * Masking reduces exposure risk, but must never be claimed as a 100% replacement
     * for strict network isolation and access controls.
     */
    fun isCompleteSecurityGuarantee(): Boolean = false
}
