package org.claudroide.app.feature.git

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 100 — "find secrets before git" (Geheimnisse vor Git finden).
 *
 * Goal: detect unintended credentials and private keys **before publication**.
 * Result: a local scan, a finding display that carries no secret value, and safe
 * help for excluding or removing what was found.
 *
 * ## Why this file does not reuse [SecretMasker]
 *
 * [SecretMasker] shortens a value for display: it returns the first and last four
 * characters. For a PEM block those are the block's borders, so the shortened form
 * is no longer recognisable as a key — and a scan that ran over shortened values
 * would report nothing. Masking answers "what may I show"; this answers "what must
 * I refuse". They are opposite questions, so this file keeps its own rules.
 *
 * The two rules are the same ones `tools/secret_gate.py` applies to this
 * repository, and they exist for a measured reason: `app/src/test/` deliberately
 * contains strings like `sk-ant-api03-AAAA...` to prove the masker works. A filter
 * that only looks at the prefix reports those fixtures and blocks its own
 * repository; a filter that is strict enough to skip them lets real keys through.
 * Both failures were observed, not assumed.
 *
 *  1. **The value must be random.** A real key is a random sequence. A test value
 *     is not — it repeats a character, names itself ("synthetic", "beispiel"), or
 *     is a recognisable test run.
 *  2. **PEM material only counts with a body.** The line
 *     `-----BEGIN RSA PRIVATE KEY-----` stands alone in four test files to prove
 *     it gets scrubbed. Suspicion starts at the base64 body on the following lines.
 *
 * ## The two completion conditions as properties
 *
 *  1. *"Known patterns and false positives are testable."*
 *     [SecretVerdict] is part of every [SecretFinding], so a hit that declares
 *     itself synthetic is **visible but not blocking**. It is neither hidden nor
 *     counted as a real key — both halves are properties, not prose.
 *
 *  2. *"Findings block the commit by default."*
 *     [SecretScanReport.blocksCommit] reads only [SecretVerdict.LIKELY_SECRET], and
 *     [GitSecretGate.mayStage] takes no flag that lifts it. "By default" here means
 *     *with no opt-out at all*, which is deliberately stricter than
 *     `ProjectExclusionPolicy.BLOCKED_HEAVY`: a committed secret stays committed.
 *
 * ## The protection: nothing leaves the device
 *
 * No network, no filesystem, no process. [GitSecretScanner] receives text as a
 * value and returns findings; the caller does the reading. There is no code path
 * from a finding to an online scanner — not because one was omitted, but because
 * none exists.
 *
 * ## What a finding may carry
 *
 * A finding holds a path, a line number, a rule name and a verdict. It holds **no
 * excerpt**, not even a masked one: the user is pointed at `path:line` and looks at
 * their own file. Anything else would copy the value into a second place.
 *
 * Pure Kotlin: no network, no filesystem, no process, no Android.
 */

// ── Verdict ────────────────────────────────────────────────────────────────

/**
 * What a match means.
 *
 * Both values are findings: a self-declared synthetic value is **shown**, not
 * silently dropped. Suppressing it would let a real key hide behind a word in a
 * comment; promoting it to a block would make the project's own test suite
 * uncommittable.
 */
enum class SecretVerdict(val label: String, val germanLabel: String) {

    /** A random-looking value. Blocks the commit. */
    LIKELY_SECRET("likely secret", "wahrscheinlich ein echter Schlüssel"),

    /** Names itself as an example. Shown, does not block. */
    SELF_DECLARED_SYNTHETIC("declared synthetic", "gibt sich als Beispielwert zu erkennen")
}

// ── What a scan produces ───────────────────────────────────────────────────

/**
 * One place a rule matched.
 *
 * There is deliberately no field for the matched text. The redacted form would
 * still be a copy of the secret in a second object, and the shortened form leaks
 * the first and last four characters.
 */
data class SecretFinding(
    val path: String,
    /** 1-based, as an editor counts. */
    val line: Int,
    val ruleName: String,
    val verdict: SecretVerdict
) {
    init {
        require(path.isNotBlank()) { "A finding needs a path." }
        require(line >= 1) { "Source lines are counted from one, not from zero." }
        require(ruleName.isNotBlank()) { "A finding has to name the rule that matched." }
    }

    /** The line for the user interface: where to look, never what was found. */
    fun displayLine(): String = "$path:$line — ${ruleName} (${verdict.germanLabel})"
}

/** A file as handed to the scanner. The scanner reads nothing itself. */
data class ScannableFile(
    val path: String,
    val content: String
) {
    init {
        require(path.isNotBlank()) { "A scanned file needs a path." }
    }
}

/**
 * The outcome of scanning a set of files.
 *
 * Findings are kept in the order they were found, so the first one in the report is
 * the first thing in the file, not the first rule that happened to match.
 */
data class SecretScanReport(
    val scannedPaths: List<String>,
    val findings: List<SecretFinding>
) {

    /** The paths that carry at least one finding, deduplicated and ordered. */
    val hitPaths: List<String>
        get() = findings.map { it.path }.distinct().sorted()

    /** Findings that block. Everything else is shown and does not block. */
    val blockingFindings: List<SecretFinding>
        get() = findings.filter { it.verdict == SecretVerdict.LIKELY_SECRET }

    /**
     * May these files go into a commit?
     *
     * Reads [blockingFindings] and nothing else. It is not a parameter, not a
     * setting, and not derivable from the user's confirmation — a user who
     * confirms a commit has confirmed the *files*, not the contents.
     */
    val blocksCommit: Boolean get() = blockingFindings.isNotEmpty()

    /** Paths that were scanned and carry no finding at all. */
    val cleanPaths: List<String>
        get() = scannedPaths.filterNot { it in hitPaths }.sorted()

    /**
     * The lines for the user interface.
     *
     * Every finding on its own line, with no count standing in front of them: the
     * user cannot act on "2 findings" but can open `path:line`.
     */
    fun displayLines(): List<String> = buildList {
        add("Geprüfte Dateien: ${scannedPaths.size}")
        if (findings.isEmpty()) {
            add("Kein Schlüsselverdacht. Die Dateien können gespeichert werden.")
            return@buildList
        }
        val sperrend = blockingFindings.size
        add(
            if (sperrend == 0) {
                "${findings.size} Fundstelle(n), keine davon blockiert das Speichern."
            } else {
                "${findings.size} Fundstelle(n), davon $sperrend blockiert das Speichern."
            }
        )
        findings.forEach { add("  ${it.displayLine()}") }
    }

    /**
     * The help for excluding what was found.
     *
     * Excluding the file is the safe route and needs no permission: the commit then
     * carries everything **except** the offending path. The lines say exactly which
     * paths those are, because a remedy that does not name its subject cannot be
     * carried out.
     */
    fun remediationLines(): List<String> = buildList {
        val sperrend = hitPaths.filter { pfad -> findings.any { it.path == pfad && it.verdict == SecretVerdict.LIKELY_SECRET } }
        if (sperrend.isEmpty()) {
            add("Nichts auszuschließen: kein Fund blockiert das Speichern.")
            return@buildList
        }
        add("Diese Datei(en) aus dem Commit nehmen, der Rest kann gespeichert werden:")
        sperrend.forEach { add("  $it") }
        add("Oder den Schlüssel in der Datei selbst entfernen und neu scannen.")
        add("Eine Bestätigung des Nutzers hebt einen Verdacht nicht auf.")
    }

    /** The lines describing what this scan does and does not do. */
    fun policyLines(): List<String> = listOf(
        "Geprüft wird ausschließlich lokal. Es wird nichts gesendet und nichts gespeichert.",
        "Ein Treffer nennt Pfad und Zeile, nie den Wert selbst.",
        "Ein Wert, der sich selbst als Beispiel ausweist, wird gezeigt, blockiert aber nicht.",
        "Ein Wert, der zufällig aussieht, blockiert das Speichern. Es gibt keine Aufhebung.",
        "Text in der Anzeige ist ${SecretMasker.REDACTION_PLACEHOLDER} maskiert."
    )
}

// ── The rules ──────────────────────────────────────────────────────────────

/**
 * A vendor prefix and the **random part** of a value, captured as group 1.
 *
 * Capturing the tail rather than stripping a prefix afterwards keeps the two rules
 * in one place: the pattern proves the shape, and the captured group is what gets
 * judged for randomness. The minimum lengths follow the real key lengths rather
 * than a round number — a threshold longer than a real key would pass every real
 * key while still reporting the fixtures.
 */
private data class PrefixRule(
    val name: String,
    val regex: Regex
)

private val PREFIX_RULES: List<PrefixRule> = listOf(
    PrefixRule("Anthropic API key", Regex("""sk-ant-api\d{2}-([A-Za-z0-9_\-]{20,})""")),
    PrefixRule("GitHub token (classic)", Regex("""ghp_([A-Za-z0-9]{20,})""")),
    PrefixRule("GitHub token (fine-grained)", Regex("""github_pat_([A-Za-z0-9_]{20,})""")),
    PrefixRule("AWS access key id", Regex("""AKIA([0-9A-Z]{16})"""))
)

private val PEM_HEADER: Regex =
    Regex("""-----BEGIN (?:RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----""")

/** The base64 body that follows a PEM header. A header alone is not a secret. */
private val PEM_MATERIAL: Regex = Regex("""MII[A-Za-z0-9+/]{20,}""")

/**
 * Words and runs that give a value away as an example.
 *
 * A test value may say so. That is the difference between an example and a real
 * key, and it is the only reason the project's own fixtures do not block its own
 * repository.
 */
private val SELF_DECLARED: Regex = Regex(
    "Example|dummy|Platzhalter|NichtEcht|synthetic|fake|beispiel" +
        "|abcdef|1234567890|0123456789|qwerty|asdfgh|ABCDEFGHIJ" +
        "|AAAA|BBBB|CCCC|DDDD|EEEE|FFFF|GGGG|HHHH|IIII|JJJJ|KKKK|LLLL" +
        "|MMMM|NNNN|OOOO|PPPP|QQQQ|RRRR|SSSS|TTTT|UUUU|VVVV|WWWW|XXXX|YYYY|ZZZZ",
    RegexOption.IGNORE_CASE
)

/** One character repeated eight times or more: never a random value. */
private val REPEATED: Regex = Regex("""^(.)\1{7,}$""")

/**
 * How many lines after a PEM header the body is still looked for.
 *
 * Three, because the base64 body starts on the **next** line. A window of zero
 * would re-read the header itself, and the header by definition holds no body —
 * so the rule would be documented and unreachable at the same time, which is how
 * this constant stood: the comment above promised "the following lines" and the
 * code looked at none.
 *
 * `tools/secret_gate.py` uses the same four-line window (`range(i, i + 4)`), so
 * the build-time gate of this repository and this scanner do not disagree about
 * what a private key looks like. The window is deliberately small: a base64 run
 * fifty lines below a header is not that key's body, and widening it would only
 * add false positives.
 */
private const val PEM_LOOKAHEAD_LINES = 3

/**
 * Judges one captured value.
 *
 * @return [SecretVerdict.SELF_DECLARED_SYNTHETIC] when the value names itself or
 *         is one long run, otherwise [SecretVerdict.LIKELY_SECRET].
 */
internal fun judgeValue(value: String): SecretVerdict =
    if (REPEATED.matches(value) || SELF_DECLARED.containsMatchIn(value)) {
        SecretVerdict.SELF_DECLARED_SYNTHETIC
    } else {
        SecretVerdict.LIKELY_SECRET
    }

// ── The scanner ────────────────────────────────────────────────────────────

/**
 * Reads files as values and reports where a rule matched.
 *
 * Decides only. It opens nothing, sends nothing, and holds no repository.
 */
object GitSecretScanner {

    /**
     * Scans [files] and returns every finding, in file and line order.
     *
     * @param files path and content pairs. Reading them is the caller's job, which
     *        is what keeps this object free of filesystem and network access.
     */
    fun scan(files: List<ScannableFile>): SecretScanReport {
        val findings = mutableListOf<SecretFinding>()
        files.forEach { file ->
            findings += scanFile(file)
        }
        return SecretScanReport(
            scannedPaths = files.map { it.path },
            findings = findings.distinct()
        )
    }

    private fun scanFile(file: ScannableFile): List<SecretFinding> {
        val lines = file.content.split('\n')
        val findings = mutableListOf<SecretFinding>()

        lines.forEachIndexed { index, line ->
            PREFIX_RULES.forEach { rule ->
                rule.regex.findAll(line).forEach { match ->
                    val wert = match.groupValues.getOrElse(1) { match.value }
                    findings += SecretFinding(
                        path = file.path,
                        line = index + 1,
                        ruleName = rule.name,
                        verdict = judgeValue(wert)
                    )
                }
            }

            if (!PEM_HEADER.containsMatchIn(line)) return@forEachIndexed

            // The body sits in the following lines, not in the header itself. A file
            // that only proves the header is scrubbed has no secret to report.
            for (j in index..minOf(index + PEM_LOOKAHEAD_LINES, lines.lastIndex)) {
                val body = lines[j]
                if (PEM_MATERIAL.containsMatchIn(body) && !SELF_DECLARED.containsMatchIn(body)) {
                    findings += SecretFinding(
                        path = file.path,
                        line = j + 1,
                        ruleName = "Private key body",
                        verdict = SecretVerdict.LIKELY_SECRET
                    )
                    break
                }
            }
        }

        return findings
    }

    /** The lines describing what this scanner does. */
    fun policyLines(): List<String> = SecretScanReport(emptyList(), emptyList()).policyLines()
}

// ── The gate ───────────────────────────────────────────────────────────────

/** The answer to "may these files be staged?". */
sealed interface SecretStageDecision {
    val germanLabel: String

    /** Checked. The staging is outside this file. */
    data class MayStage(val report: SecretScanReport) : SecretStageDecision {
        override val germanLabel: String get() = "darf gespeichert werden"
    }

    /** Nothing is staged. */
    data class Refused(val reason: String) : SecretStageDecision {
        override val germanLabel: String get() = "nicht gespeichert"
    }
}

/**
 * The rule for when scanned files may go into a commit.
 *
 * The refusal names every offending path and nothing else. There is no parameter
 * that lifts it — not a confirmation, not a flag — because a committed secret stays
 * committed, and the remedy is exclusion or removal, both of which
 * [SecretScanReport.remediationLines] names.
 */
object GitSecretGate {

    /**
     * May the files in [report] be staged?
     *
     * @param report the finished scan. It is an **own** input, never derived here.
     */
    fun mayStage(report: SecretScanReport): SecretStageDecision {
        val sperrend = report.blockingFindings
        if (sperrend.isEmpty()) {
            return SecretStageDecision.MayStage(report)
        }

        val pfade = sperrend.map { it.path }.distinct().sorted()
        val zeilen = sperrend.map { it.displayLine() }
        return SecretStageDecision.Refused(
            buildString {
                append("${sperrend.size} Fundstelle(n) sehen zufällig aus und blockieren das Speichern.")
                append(" Sie werden nicht gespeichert.")
                append(" Betroffen: ${pfade.joinToString(", ")}.")
                append(" Zeilen:")
                zeilen.forEach { append(" $it") }
                append(" Nimm die Datei(en) aus dem Commit oder entferne den Schlüssel.")
            }
        )
    }

    /**
     * Can a finding be overridden by hand?
     *
     * Never. This is stricter than `ProjectExclusionPolicy.BLOCKED_HEAVY`, which a
     * user may override: a big folder costs disk, a committed key costs the account.
     */
    fun canOverride(verdict: SecretVerdict): Boolean = false
}