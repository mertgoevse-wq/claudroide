package org.claudroide.app.feature.chat

sealed interface MessageContentSegment

data class TextSegment(val text: String) : MessageContentSegment

data class CodeSegment(
    val language: String,
    val code: String,
    val lineCount: Int,
    val isCollapsible: Boolean
) : MessageContentSegment

/**
 * Architectural policy enforcing passive rendering of code blocks
 * and lossless clipboard operations.
 */
object CodeBlockPolicy {
    // Fundamental security invariant: rendered code must NEVER be auto-executed
    const val IS_AUTO_EXECUTION_PERMITTED: Boolean = false

    // Long code blocks (> 25 lines) are initially collapsed with an expand toggle
    const val COLLAPSE_LINE_THRESHOLD: Int = 25

    /**
     * Extracts exact, byte-for-byte code text for clipboard copy.
     * Guarantees zero formatting injection, line number prefixing, or indentation mangling.
     */
    fun extractExactCodeForClipboard(segment: CodeSegment): String {
        return segment.code
    }
}

/**
 * Robust markdown segmenter that separates prose from code fences.
 * Fault-tolerant: unclosed backtick fences do not lose or truncate content.
 */
object MarkdownMessageParser {

    fun parseMessageSegments(rawMarkdown: String): List<MessageContentSegment> {
        if (rawMarkdown.isEmpty()) return emptyList()

        val segments = mutableListOf<MessageContentSegment>()
        val fenceRegex = Regex("""```([a-zA-Z0-9_\-\.]*)\n([\s\S]*?)(?:```|$)""")
        var lastIndex = 0

        for (match in fenceRegex.findAll(rawMarkdown)) {
            val startIndex = match.range.first
            if (startIndex > lastIndex) {
                val prose = rawMarkdown.substring(lastIndex, startIndex)
                if (prose.isNotEmpty()) {
                    segments.add(TextSegment(prose))
                }
            }

            val lang = match.groupValues[1].trim().ifEmpty { "text" }
            val rawCode = match.groupValues[2]
            val lines = rawCode.lines().size
            segments.add(
                CodeSegment(
                    language = lang,
                    code = rawCode,
                    lineCount = lines,
                    isCollapsible = lines > CodeBlockPolicy.COLLAPSE_LINE_THRESHOLD
                )
            )

            lastIndex = match.range.last + 1
        }

        if (lastIndex < rawMarkdown.length) {
            val trailingProse = rawMarkdown.substring(lastIndex)
            if (trailingProse.isNotEmpty()) {
                segments.add(TextSegment(trailingProse))
            }
        }

        return segments
    }
}
