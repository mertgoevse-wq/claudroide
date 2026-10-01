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

    private val SCRUB_PATTERNS = listOf(
        "Anthropic API Key" to Regex("""(?i)\bsk-ant-[a-zA-Z0-9_\-]{8,}\b"""),
        "OpenAI / OpenRouter Key" to Regex("""(?i)\bsk-[a-zA-Z0-9_\-]{16,}\b"""),
        "Bearer Auth Header" to Regex("""(?i)\bbearer\s+[a-zA-Z0-9\-_\.]{16,}\b"""),
        "Private Key Block" to Regex("""-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----"""),
        "Sensitive Assignment" to Regex("""(?i)\b(api[_-]?key|password|client_secret|access_token)\s*[=:]\s*['"]?([^\s'"&,]{4,})['"]?""")
    )

    /**
     * Sanitizes input text, replacing all recognized credentials with [REDACTED].
     */
    fun redact(input: String): String {
        if (input.isBlank()) return input
        var current = input
        for ((_, regex) in SCRUB_PATTERNS) {
            current = regex.replace(current, "[REDACTED]")
        }
        return current
    }

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
                current = regex.replace(current, "[REDACTED]")
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
