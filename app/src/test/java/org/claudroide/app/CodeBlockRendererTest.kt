package org.claudroide.app

import org.claudroide.app.feature.chat.CodeBlockPolicy
import org.claudroide.app.feature.chat.CodeSegment
import org.claudroide.app.feature.chat.MarkdownMessageParser
import org.claudroide.app.feature.chat.TextSegment
import org.junit.Assert.*
import org.junit.Test

class CodeBlockRendererTest {

    @Test
    fun codeBlockPolicy_strictlyForbidsAutoExecution() {
        assertFalse(CodeBlockPolicy.IS_AUTO_EXECUTION_PERMITTED)
    }

    @Test
    fun extractExactCodeForClipboard_isLossless() {
        val snippet = "    fun calculateSum(a: Int, b: Int): Int {\n        return a + b\n    }"
        val segment = CodeSegment(
            language = "kotlin",
            code = snippet,
            lineCount = 3,
            isCollapsible = false
        )
        val extracted = CodeBlockPolicy.extractExactCodeForClipboard(segment)
        assertEquals(snippet, extracted)
    }

    @Test
    fun markdownParser_splitsProseAndCodeBlocks() {
        val markdown = """Here is the implementation:
```kotlin
val message = "Hello"
println(message)
```
Let me know if that helps!"""

        val segments = MarkdownMessageParser.parseMessageSegments(markdown)
        assertEquals(3, segments.size)

        assertTrue(segments[0] is TextSegment)
        assertEquals("Here is the implementation:\n", (segments[0] as TextSegment).text)

        assertTrue(segments[1] is CodeSegment)
        val codeSeg = segments[1] as CodeSegment
        assertEquals("kotlin", codeSeg.language)
        assertTrue(codeSeg.code.contains("val message = \"Hello\""))

        assertTrue(segments[2] is TextSegment)
        assertEquals("\nLet me know if that helps!", (segments[2] as TextSegment).text)
    }

    @Test
    fun markdownParser_isFaultTolerantWithUnclosedFence() {
        val unclosed = """Streaming code snippet:
```python
def process():
    return 42"""

        val segments = MarkdownMessageParser.parseMessageSegments(unclosed)
        assertEquals(2, segments.size)
        assertTrue(segments[1] is CodeSegment)
        val codeSeg = segments[1] as CodeSegment
        assertEquals("python", codeSeg.language)
        assertTrue(codeSeg.code.contains("return 42"))
    }

    @Test
    fun longCodeBlock_isMarkedCollapsible() {
        val longCode = (1..30).joinToString("\n") { "val line$it = $it" }
        val markdown = "```bash\n$longCode\n```"

        val segments = MarkdownMessageParser.parseMessageSegments(markdown)
        val codeSeg = segments.filterIsInstance<CodeSegment>().first()

        assertTrue(codeSeg.lineCount > CodeBlockPolicy.COLLAPSE_LINE_THRESHOLD)
        assertTrue(codeSeg.isCollapsible)
    }
}
