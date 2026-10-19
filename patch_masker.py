import re

file_path = "app/src/main/java/org/claudroide/app/feature/provider/SecretMasker.kt"
with open(file_path, "r") as f:
    code = f.read()

# Add github token to SCRUB_PATTERNS
github_pattern = r'''"Prefixed Key" to Regex\("""(?i)\\b\[a-zA-Z_\]\[a-zA-Z0-9_.\\-\]\*\\s\*=\\s\*(sk-\[a-zA-Z0-9_\\-\]\{8,\})\ """\)'''
new_pattern = """"Prefixed Key" to Regex(\"\"\"(?i)\\b[a-zA-Z_][a-zA-Z0-9_.\\-]*\\s*=\\s*(sk-[a-zA-Z0-9_\\-]{8,})\"\"\"),
        "GitHub Token" to Regex(\"\"\"\\bgh[pousr]_[A-Za-z0-9]{12,}\\b\"\"\")"""

if '"GitHub Token"' not in code:
    # Just insert it before ") // End of list" if possible, or replace Prefixed Key
    code = code.replace('"Prefixed Key" to Regex("""(?i)\\b[a-zA-Z_][a-zA-Z0-9_.\\-]*\\s*=\\s*(sk-[a-zA-Z0-9_\\-]{8,})""")', new_pattern)

# Add sensitive path detection
if "isSensitiveFileName" not in code:
    methods_insertion = """
    private val SENSITIVE_FILES = setOf(
        ".env",
        "google-services.json",
        "local.properties",
        "keystore",
        "credentials.json",
        "secrets.properties"
    )

    fun isSensitiveFileName(name: String): Boolean {
        return SENSITIVE_FILES.any { name.equals(it, ignoreCase = true) || name.contains(it) }
    }

    /** Returns redacted content or warning if filename is sensitive. */
    fun redactFileEntry(filename: String, content: String): String {
        if (isSensitiveFileName(filename)) {
            return "[Content of \$filename suppressed — sensitive file]"
        }
        return redact(content)
    }
"""
    # Insert methods before the final bracket
    code = code.rsplit("}", 1)[0] + methods_insertion + "}\n"

with open(file_path, "w") as f:
    f.write(code)
