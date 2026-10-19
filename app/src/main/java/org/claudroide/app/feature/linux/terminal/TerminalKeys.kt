package org.claudroide.app.feature.linux.terminal

object TerminalKeys {
    val CTRL_C = byteArrayOf(0x03)
    val CTRL_D = byteArrayOf(0x04)
    val CTRL_Z = byteArrayOf(0x1A)
    val BACKSPACE = byteArrayOf(0x7F)
    val ENTER = byteArrayOf(0x0D)
    val TAB = byteArrayOf(0x09)
    val ESC = byteArrayOf(0x1B)

    // Standard ANSI VT100 / XTerm arrow and navigation sequences
    val ARROW_UP = "\u001b[A".toByteArray(Charsets.UTF_8)
    val ARROW_DOWN = "\u001b[B".toByteArray(Charsets.UTF_8)
    val ARROW_RIGHT = "\u001b[C".toByteArray(Charsets.UTF_8)
    val ARROW_LEFT = "\u001b[D".toByteArray(Charsets.UTF_8)
    val HOME = "\u001b[H".toByteArray(Charsets.UTF_8)
    val END = "\u001b[F".toByteArray(Charsets.UTF_8)
    val PAGE_UP = "\u001b[5~".toByteArray(Charsets.UTF_8)
    val PAGE_DOWN = "\u001b[6~".toByteArray(Charsets.UTF_8)

    /**
     * Translates a control character (e.g. 'c' or 'C') to its ASCII control byte (1-26).
     */
    fun ctrl(char: Char): ByteArray {
        val uppercase = char.uppercaseChar()
        val code = uppercase.code - 64
        return if (code in 1..26) {
            byteArrayOf(code.toByte())
        } else {
            char.toString().toByteArray(Charsets.UTF_8)
        }
    }
}
