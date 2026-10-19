package org.claudroide.app.feature.linux.util

/**
 * Minimal dependency-free JSON parser for MLL state files (objects, arrays,
 * strings, booleans, numbers, null). The core library deliberately avoids
 * external runtime dependencies; this replaces the fragile regex parsing that
 * previously corrupted values containing quotes or backslashes.
 */
object MiniJson {

    fun parse(text: String): Any? {
        val p = Parser(text)
        p.skipWs()
        val value = p.parseValue()
        p.skipWs()
        if (!p.atEnd()) throw IllegalArgumentException("Trailing characters at offset ${p.pos}")
        return value
    }

    private class Parser(val s: String) {
        var pos = 0

        fun atEnd() = pos >= s.length

        fun skipWs() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        fun parseValue(): Any? {
            if (atEnd()) throw IllegalArgumentException("Unexpected end of input")
            return when (s[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> { expect("true"); true }
                'f' -> { expect("false"); false }
                'n' -> { expect("null"); null }
                else -> parseNumber()
            }
        }

        fun expect(word: String) {
            if (!s.startsWith(word, pos)) throw IllegalArgumentException("Invalid literal at offset $pos")
            pos += word.length
        }

        fun parseObject(): MutableMap<String, Any?> {
            pos++ // consume '{'
            val map = mutableMapOf<String, Any?>()
            skipWs()
            if (!atEnd() && s[pos] == '}') { pos++; return map }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                if (atEnd() || s[pos] != ':') throw IllegalArgumentException("Expected ':' at offset $pos")
                pos++
                skipWs()
                map[key] = parseValue()
                skipWs()
                if (atEnd()) throw IllegalArgumentException("Unterminated object")
                when (s[pos]) {
                    ',' -> pos++
                    '}' -> { pos++; return map }
                    else -> throw IllegalArgumentException("Expected ',' or '}' at offset $pos")
                }
            }
        }

        fun parseArray(): MutableList<Any?> {
            pos++ // consume '['
            val list = mutableListOf<Any?>()
            skipWs()
            if (!atEnd() && s[pos] == ']') { pos++; return list }
            while (true) {
                skipWs()
                list.add(parseValue())
                skipWs()
                if (atEnd()) throw IllegalArgumentException("Unterminated array")
                when (s[pos]) {
                    ',' -> pos++
                    ']' -> { pos++; return list }
                    else -> throw IllegalArgumentException("Expected ',' or ']' at offset $pos")
                }
            }
        }

        fun parseString(): String {
            if (atEnd() || s[pos] != '"') throw IllegalArgumentException("Expected string at offset $pos")
            pos++
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw IllegalArgumentException("Unterminated string")
                when (val c = s[pos++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (atEnd()) throw IllegalArgumentException("Unterminated escape")
                        when (val e = s[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'u' -> {
                                if (pos + 4 > s.length) throw IllegalArgumentException("Bad unicode escape at offset $pos")
                                sb.append(s.substring(pos, pos + 4).toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("Bad escape '\\$e'")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        fun parseNumber(): Any {
            val start = pos
            if (!atEnd() && (s[pos] == '-' || s[pos] == '+')) pos++
            while (!atEnd() && (s[pos].isDigit() || s[pos] in ".eE+-")) pos++
            val token = s.substring(start, pos)
            return token.toLongOrNull() ?: token.toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid number '$token' at offset $start")
        }
    }
}
