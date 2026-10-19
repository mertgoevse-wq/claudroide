package org.claudroide.app.feature.linux.terminal

import java.util.concurrent.CopyOnWriteArrayList

enum class TerminalColor {
    DEFAULT,
    BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE,
    BRIGHT_BLACK, BRIGHT_RED, BRIGHT_GREEN, BRIGHT_YELLOW, BRIGHT_BLUE, BRIGHT_MAGENTA, BRIGHT_CYAN, BRIGHT_WHITE
}

data class TextStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val fg: TerminalColor = TerminalColor.DEFAULT,
    val bg: TerminalColor = TerminalColor.DEFAULT
)

data class StyledSpan(
    val text: String,
    val style: TextStyle
)

data class TerminalLine(
    val spans: List<StyledSpan>
) {
    val rawText: String by lazy {
        spans.joinToString("") { it.text }
    }
}

/**
 * Thread-safe ANSI terminal screen buffer.
 * Parses VT100 / XTerm ANSI escape sequences (colors, styles, cursor control, screen clearing).
 */
class TerminalBuffer(
    val maxLines: Int = 2000
) {
    private val lines = ArrayList<TerminalLine>()
    private val lock = Any()

    // Current parser state
    private var currentStyle = TextStyle()
    private var currentLineSpans = ArrayList<StyledSpan>()
    private var cursorCol: Int = 0

    var onBell: (() -> Unit)? = null

    init {
        currentLineSpans = ArrayList()
    }

    fun write(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size) {
        val text = String(bytes, offset, length, Charsets.UTF_8)
        write(text)
    }

    fun write(text: String) {
        synchronized(lock) {
            var i = 0
            val len = text.length

            while (i < len) {
                val c = text[i]

                when {
                    c == '\u001b' -> {
                        // Check if it's a CSI sequence: \u001b[
                        if (i + 1 < len && text[i + 1] == '[') {
                            val endIdx = findCsiEnd(text, i + 2)
                            if (endIdx != -1) {
                                val csiContent = text.substring(i + 2, endIdx)
                                val finalChar = text[endIdx]
                                processCsi(csiContent, finalChar)
                                i = endIdx + 1
                                continue
                            }
                        } else if (i + 1 < len && text[i + 1] == ']') {
                            // OSC sequence: \u001b] ... \u0007 or \u001b\
                            val endIdx = findOscEnd(text, i + 2)
                            if (endIdx != -1) {
                                i = endIdx + 1
                                continue
                            }
                        }
                        // Unhandled escape: skip character
                        i++
                    }
                    c == '\r' -> {
                        // Carriage return: reset cursor to beginning of current line
                        cursorCol = 0
                        i++
                    }
                    c == '\n' -> {
                        // New line: commit current line into lines buffer
                        commitLine()
                        i++
                    }
                    c == '\b' || c.code == 0x7F -> {
                        // Backspace
                        handleBackspace()
                        i++
                    }
                    c == '\t' -> {
                        // Tab stop (every 8 spaces)
                        val spacesNeeded = 8 - (cursorCol % 8)
                        appendPlainText(" ".repeat(spacesNeeded))
                        i++
                    }
                    c.code == 0x07 -> {
                        // Bell
                        onBell?.invoke()
                        i++
                    }
                    else -> {
                        // Plain character or run of plain characters
                        val nextSpecial = findNextSpecial(text, i)
                        val chunk = text.substring(i, nextSpecial)
                        appendPlainText(chunk)
                        i = nextSpecial
                    }
                }
            }
        }
    }

    private fun findNextSpecial(text: String, start: Int): Int {
        for (idx in start until text.length) {
            val ch = text[idx]
            if (ch == '\u001b' || ch == '\r' || ch == '\n' || ch == '\b' || ch.code == 0x7F || ch == '\t' || ch.code == 0x07) {
                return idx
            }
        }
        return text.length
    }

    private fun findCsiEnd(text: String, start: Int): Int {
        for (idx in start until text.length) {
            val ch = text[idx]
            if (ch in '@'..'~') {
                return idx
            }
        }
        return -1
    }

    private fun findOscEnd(text: String, start: Int): Int {
        for (idx in start until text.length) {
            val ch = text[idx]
            if (ch.code == 0x07) return idx
            if (ch == '\u001b' && idx + 1 < text.length && text[idx + 1] == '\\') return idx + 1
        }
        return -1
    }

    private fun processCsi(params: String, command: Char) {
        when (command) {
            'm' -> { // SGR (Select Graphic Rendition)
                processSgr(params)
            }
            'J' -> { // Erase in Display
                val mode = params.toIntOrNull() ?: 0
                when (mode) {
                    2, 3 -> {
                        lines.clear()
                        currentLineSpans.clear()
                        cursorCol = 0
                    }
                }
            }
            'K' -> { // Erase in Line
                val mode = params.toIntOrNull() ?: 0
                when (mode) {
                    0 -> { // Clear from cursor to end of line
                        trimLineSpansTo(cursorCol)
                    }
                    2 -> { // Clear entire line
                        currentLineSpans.clear()
                        cursorCol = 0
                    }
                }
            }
            'H', 'f' -> { // Cursor position
                // e.g. 1;1H
            }
        }
    }

    private fun processSgr(params: String) {
        if (params.isBlank()) {
            currentStyle = TextStyle()
            return
        }

        val codes = params.split(';').mapNotNull { it.toIntOrNull() }
        var style = currentStyle

        var idx = 0
        while (idx < codes.size) {
            val code = codes[idx]
            style = when (code) {
                0 -> TextStyle() // Reset
                1 -> style.copy(bold = true)
                2 -> style.copy(bold = false)
                3 -> style.copy(italic = true)
                4 -> style.copy(underline = true)
                22 -> style.copy(bold = false)
                23 -> style.copy(italic = false)
                24 -> style.copy(underline = false)

                // Foreground colors 30..37
                30 -> style.copy(fg = TerminalColor.BLACK)
                31 -> style.copy(fg = TerminalColor.RED)
                32 -> style.copy(fg = TerminalColor.GREEN)
                33 -> style.copy(fg = TerminalColor.YELLOW)
                34 -> style.copy(fg = TerminalColor.BLUE)
                35 -> style.copy(fg = TerminalColor.MAGENTA)
                36 -> style.copy(fg = TerminalColor.CYAN)
                37 -> style.copy(fg = TerminalColor.WHITE)
                39 -> style.copy(fg = TerminalColor.DEFAULT)

                // Background colors 40..47
                40 -> style.copy(bg = TerminalColor.BLACK)
                41 -> style.copy(bg = TerminalColor.RED)
                42 -> style.copy(bg = TerminalColor.GREEN)
                43 -> style.copy(bg = TerminalColor.YELLOW)
                44 -> style.copy(bg = TerminalColor.BLUE)
                45 -> style.copy(bg = TerminalColor.MAGENTA)
                46 -> style.copy(bg = TerminalColor.CYAN)
                47 -> style.copy(bg = TerminalColor.WHITE)
                49 -> style.copy(bg = TerminalColor.DEFAULT)

                // Bright foreground 90..97
                90 -> style.copy(fg = TerminalColor.BRIGHT_BLACK)
                91 -> style.copy(fg = TerminalColor.BRIGHT_RED)
                92 -> style.copy(fg = TerminalColor.BRIGHT_GREEN)
                93 -> style.copy(fg = TerminalColor.BRIGHT_YELLOW)
                94 -> style.copy(fg = TerminalColor.BRIGHT_BLUE)
                95 -> style.copy(fg = TerminalColor.BRIGHT_MAGENTA)
                96 -> style.copy(fg = TerminalColor.BRIGHT_CYAN)
                97 -> style.copy(fg = TerminalColor.BRIGHT_WHITE)

                // Bright background 100..107
                100 -> style.copy(bg = TerminalColor.BRIGHT_BLACK)
                101 -> style.copy(bg = TerminalColor.BRIGHT_RED)
                102 -> style.copy(bg = TerminalColor.BRIGHT_GREEN)
                103 -> style.copy(bg = TerminalColor.BRIGHT_YELLOW)
                104 -> style.copy(bg = TerminalColor.BRIGHT_BLUE)
                105 -> style.copy(bg = TerminalColor.BRIGHT_MAGENTA)
                106 -> style.copy(bg = TerminalColor.BRIGHT_CYAN)
                107 -> style.copy(bg = TerminalColor.BRIGHT_WHITE)

                else -> style
            }
            idx++
        }
        currentStyle = style
    }

    private fun appendPlainText(chunk: String) {
        if (chunk.isEmpty()) return

        // If currentLineSpans already has same style at tail, merge
        if (currentLineSpans.isNotEmpty() && currentLineSpans.last().style == currentStyle) {
            val last = currentLineSpans.removeAt(currentLineSpans.size - 1)
            currentLineSpans.add(StyledSpan(last.text + chunk, currentStyle))
        } else {
            currentLineSpans.add(StyledSpan(chunk, currentStyle))
        }
        cursorCol += chunk.length
    }

    private fun handleBackspace() {
        if (cursorCol <= 0 || currentLineSpans.isEmpty()) return

        // Remove 1 character from currentLineSpans
        val lastSpan = currentLineSpans.last()
        if (lastSpan.text.length > 1) {
            currentLineSpans[currentLineSpans.size - 1] = StyledSpan(
                lastSpan.text.dropLast(1),
                lastSpan.style
            )
        } else {
            currentLineSpans.removeAt(currentLineSpans.size - 1)
        }
        cursorCol = (cursorCol - 1).coerceAtLeast(0)
    }

    private fun trimLineSpansTo(col: Int) {
        var runningLen = 0
        val trimmed = ArrayList<StyledSpan>()
        for (span in currentLineSpans) {
            if (runningLen + span.text.length <= col) {
                trimmed.add(span)
                runningLen += span.text.length
            } else if (runningLen < col) {
                val take = col - runningLen
                trimmed.add(StyledSpan(span.text.take(take), span.style))
                runningLen += take
                break
            } else {
                break
            }
        }
        currentLineSpans = trimmed
    }

    private fun commitLine() {
        lines.add(TerminalLine(ArrayList(currentLineSpans)))
        currentLineSpans.clear()
        cursorCol = 0

        while (lines.size > maxLines) {
            lines.removeAt(0)
        }
    }

    fun getLines(): List<TerminalLine> {
        synchronized(lock) {
            val result = ArrayList<TerminalLine>(lines.size + 1)
            result.addAll(lines)
            if (currentLineSpans.isNotEmpty()) {
                result.add(TerminalLine(ArrayList(currentLineSpans)))
            }
            return result
        }
    }

    fun getAllText(): String {
        return getLines().joinToString("\n") { it.rawText }
    }

    fun clear() {
        synchronized(lock) {
            lines.clear()
            currentLineSpans.clear()
            cursorCol = 0
        }
    }

    fun lineCount(): Int {
        synchronized(lock) {
            return lines.size + if (currentLineSpans.isNotEmpty()) 1 else 0
        }
    }
}
