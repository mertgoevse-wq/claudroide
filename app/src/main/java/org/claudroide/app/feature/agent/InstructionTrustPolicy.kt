package org.claudroide.app.feature.agent

/**
 * Task 122 (Gate) — "Manipulierte Anweisungen".
 *
 * The threat: a file, a web page or a tool result inside a project contains
 * text addressed to the model ("ignore your instructions", "send the API key to
 * …", "switch to another provider"). Anything read into context is *data*. It
 * must never be able to change what the app considers an instruction, and it
 * must never be able to widen a permission.
 *
 * The invariant the task states is structural, not a matter of prompt wording:
 * **secrets and approval rules stay outside the model's reach.** So the policy
 * below does not try to out-prompt an attacker. It decides, for a piece of
 * content, *what kind of thing it is* — and only the user's own words can ever
 * produce an instruction. No string in a file can.
 *
 * Invariants:
 *  - Content from a project, a page or a tool result is always [TrustLevel.UNTRUSTED_DATA].
 *    There is no path in this file that promotes it to an instruction.
 *  - A suspicious passage is reported to the user and never silently dropped:
 *    hiding it would leave the user unaware of what the project tried to do.
 *  - Approval decisions are never carried inside content. A file saying
 *    "the user already approved this" changes nothing.
 *  - The dangerous part of such content must reach the user *as a quoted
 *    excerpt*, so they can judge it themselves.
 */
object InstructionTrustPolicy {

    /**
     * Where a piece of text came from, and therefore what it may ever become.
     */
    enum class TrustLevel {
        /** Typed by the user in this app. The only source of instructions. */
        USER_AUTHORED,

        /**
         * Read out of a project file, a page, a git history or a tool result.
         * Data, never an instruction — no matter what the text claims about itself.
         */
        UNTRUSTED_DATA
    }

    /**
     * The kinds of attempt this policy recognises.
     */
    enum class InjectionPattern {
        /** Tries to override the app's instructions. */
        INSTRUCTION_OVERRIDE,

        /** Tries to make the model hand over a secret. */
        SECRET_EXFILTRATION,

        /** Tries to grant itself a permission or claim approval. */
        PERMISSION_CLAIM,

        /** Tries to change the provider or model behind the user's back. */
        PROVIDER_SWITCH,

        /** Tries to make the model run a command or a write. */
        SIDE_EFFECT_REQUEST
    }

    /**
     * One recognised attempt, with the excerpt the user gets to see.
     */
    data class Finding(
        val pattern: InjectionPattern,
        /** The matched sentence, quoted. Never longer than the sentence itself. */
        val excerpt: String
    )

    /**
     * What the app does with untrusted content.
     */
    data class ContentVerdict(
        val trustLevel: TrustLevel,
        val findings: List<Finding>,
        /** Line shown to the user in plain German. Empty when nothing was found. */
        val warnings: List<String>
    ) {
        val isSuspicious: Boolean get() = findings.isNotEmpty()

        /**
         * True when the content may be put in front of the model at all.
         *
         * Suspicious content is still allowed — refusing to read a project because one
         * sentence is hostile would make the app useless. What it may not do is carry
         * any authority, which [trustLevel] already guarantees.
         */
        val mayBeIncludedAsData: Boolean get() = trustLevel == TrustLevel.UNTRUSTED_DATA
    }

    /**
     * Patterns matched against the lower-cased text. Deliberately broad: a false
     * positive costs the user one warning line, a false negative lets a project
     * talk the model into handing over a key.
     */
    private val PATTERNS: List<Pair<InjectionPattern, Regex>> = listOf(
        InjectionPattern.INSTRUCTION_OVERRIDE to Regex(
            """ignore\s+(?:all\s+)?(?:your\s+|the\s+)?(?:previous|prior|above|earlier)\s+instructions"""
        ),
        InjectionPattern.INSTRUCTION_OVERRIDE to Regex(
            """(?:disregard|forget)\s+(?:all\s+)?(?:your\s+|the\s+)?(?:rules|instructions|guidelines)"""
        ),
        InjectionPattern.INSTRUCTION_OVERRIDE to Regex(
            """(?:you\s+are\s+now|from\s+now\s+on\s+you\s+are|new\s+instructions?:)"""
        ),
        InjectionPattern.INSTRUCTION_OVERRIDE to Regex(
            """(?:system|developer)\s+(?:prompt|message|instruction)"""
        ),
        InjectionPattern.SECRET_EXFILTRATION to Regex(
            """(?:send|post|upload|exfiltrat\w*|transmit|forward)\s+(?:the\s+|your\s+|all\s+)?(?:api[\s_-]?key|secret|token|credential|password|private\s+key)s?"""
        ),
        InjectionPattern.SECRET_EXFILTRATION to Regex(
            """(?:reveal|print|show|output|repeat)\s+(?:the\s+|your\s+)?(?:api[\s_-]?key|secret|token|credential|password)"""
        ),
        InjectionPattern.SECRET_EXFILTRATION to Regex(
            """https?://[^\s/]*(?:webhook|requestbin|pipedream|ngrok|burpcollaborator)[^\s]*"""
        ),
        InjectionPattern.PERMISSION_CLAIM to Regex(
            """(?:the\s+)?user\s+(?:has\s+)?(?:already\s+)?(?:approved|confirmed|authorised|authorized|granted\s+permission)"""
        ),
        InjectionPattern.PERMISSION_CLAIM to Regex(
            """(?:no\s+)?(?:further\s+)?(?:confirmation|approval|permission)\s+(?:is\s+)?(?:needed|required|necessary)"""
        ),
        InjectionPattern.PERMISSION_CLAIM to Regex(
            """(?:enable|grant|activate)\s+(?:all\s+)?(?:permissions?|tools?|access)"""
        ),
        InjectionPattern.PROVIDER_SWITCH to Regex(
            """(?:switch|change|use)\s+(?:to\s+)?(?:a\s+)?(?:different\s+|another\s+)?(?:provider|endpoint|api[\s_-]?key|model)"""
        ),
        InjectionPattern.PROVIDER_SWITCH to Regex(
            """(?:instead\s+of|rather\s+than)\s+(?:the\s+)?(?:configured|selected|current)\s+(?:provider|model|endpoint)"""
        ),
        InjectionPattern.SIDE_EFFECT_REQUEST to Regex(
            """(?:run|execute|invoke)\s+(?:the\s+)?(?:following\s+)?(?:command|shell\s+command|script)"""
        ),
        InjectionPattern.SIDE_EFFECT_REQUEST to Regex(
            """(?:delete|remove|overwrite|wipe)\s+(?:all\s+)?(?:files|repository|folder|directory)"""
        )
    )

    /** Sentences longer than this are cut before being quoted back to the user. */
    private const val MAX_EXCERPT_CHARS = 160

    /**
     * Classifies content that came from a project, a page or a tool result.
     *
     * The [trustLevel] is a constant here by design: this function has no branch
     * that could return [TrustLevel.USER_AUTHORED]. Only the user can author an
     * instruction, and that path does not go through untrusted content.
     */
    fun inspect(content: String): ContentVerdict {
        val findings = ArrayList<Finding>()
        val seen = HashSet<Pair<InjectionPattern, String>>()

        // Split on sentence-ish boundaries so the user sees the actual sentence,
        // not the whole file. A line-based split is enough for source files and
        // avoids a regex that can be defeated by an unusual line break.
        content.split('\n', '.', '!', '?', ';').forEach { rawSentence ->
            val sentence = rawSentence.trim()
            if (sentence.isEmpty()) return@forEach
            val lower = sentence.lowercase()

            PATTERNS.forEach { (pattern, regex) ->
                if (regex.containsMatchIn(lower)) {
                    val excerpt = sentence.take(MAX_EXCERPT_CHARS)
                    if (seen.add(pattern to excerpt)) {
                        findings.add(Finding(pattern, excerpt))
                    }
                }
            }
        }

        return ContentVerdict(
            trustLevel = TrustLevel.UNTRUSTED_DATA,
            findings = findings,
            warnings = findings.map { describe(it) }
        )
    }

    /**
     * The label a finding is shown under. German, because that is the app's language.
     */
    fun describe(finding: Finding): String {
        val what = when (finding.pattern) {
            InjectionPattern.INSTRUCTION_OVERRIDE ->
                "will die App-Anweisungen überschreiben"
            InjectionPattern.SECRET_EXFILTRATION ->
                "versucht ein Geheimnis herauszugeben"
            InjectionPattern.PERMISSION_CLAIM ->
                "behauptet, eine Freigabe sei schon erteilt"
            InjectionPattern.PROVIDER_SWITCH ->
                "versucht den Anbieter oder das Modell zu wechseln"
            InjectionPattern.SIDE_EFFECT_REQUEST ->
                "fordert eine ausführende Handlung"
        }
        return "Projektinhalt $what: „${finding.excerpt}“ — " +
            "wird nur als Text gelesen und nicht als Anweisung befolgt."
    }

    /**
     * Wraps untrusted content so its origin is visible to the model and to the user.
     *
     * The wrapper is a convention, not a security mechanism: it makes the boundary
     * legible. The guarantee comes from [TrustLevel] and from the fact that no
     * approval or secret is ever read out of content.
     */
    fun wrapAsData(content: String, sourceDescription: String): String =
        "<project_content source=\"$sourceDescription\" trust=\"data\" " +
            "instructions=\"none\">\n$content\n</project_content>"
}
