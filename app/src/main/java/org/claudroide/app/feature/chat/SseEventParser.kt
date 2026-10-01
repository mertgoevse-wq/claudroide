package org.claudroide.app.feature.chat

import org.json.JSONObject

/**
 * Parses the Anthropic Messages API server-sent event stream.
 *
 * Wire format (official docs, Messages API streaming):
 * ```
 * event: message_start
 * data: {"type":"message_start","message":{...}}
 *
 * event: content_block_delta
 * data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hallo"}}
 * ```
 *
 * Design rules:
 *  - A malformed frame yields [SseEvent.Malformed] and never throws. One bad line
 *    must not destroy an answer that is otherwise streaming fine.
 *  - An unknown `event:` type yields [SseEvent.Unknown] and never throws, so a new
 *    server-side event cannot crash an older app build.
 *  - JSON is parsed into typed fields only; raw payloads are never logged, because an
 *    echoed request could carry project content.
 */
class SseEventParser {

    private val eventName = StringBuilder()
    private val dataLines = mutableListOf<String>()

    /**
     * Feeds one raw line from the response body.
     *
     * Returns an event when the line completes a frame, otherwise null.
     * A blank line terminates a frame; comment lines (`: keep-alive`) are ignored.
     */
    fun acceptLine(line: String): SseEvent? {
        when {
            // Comment / keep-alive line.
            line.startsWith(":") -> return null

            // Blank line: end of frame.
            line.isEmpty() -> return flush()

            // Field line. "data:" with no space is legal SSE.
            line.startsWith("data:") -> dataLines += line.removePrefix("data:").removePrefix(" ")

            line.startsWith("event:") -> {
                eventName.setLength(0)
                eventName.append(line.removePrefix("event:").removePrefix(" "))
            }

            // "id:" and "retry:" carry no meaning for this API.
            else -> return null
        }
        return null
    }

    /**
     * Parses a complete event block, e.g. for tests and for buffered replay.
     */
    fun parseBlock(block: String): SseEvent {
        block.lineSequence().forEach { acceptLine(it) }
        return flush() ?: SseEvent.Malformed(block.take(120))
    }

    /**
     * Parses a full stream of text, e.g. a captured response for tests.
     *
     * Any frame still buffered when the text ends is flushed. A connection that
     * closes right after `data:` — without the usual trailing blank line — must not
     * lose its last event, which is typically the `message_stop` that marks the
     * answer complete.
     */
    fun parseAll(stream: String): List<SseEvent> {
        val events = stream.lineSequence().mapNotNull { acceptLine(it) }.toMutableList()
        flush()?.let { events.add(it) }
        return events
    }

    private fun flush(): SseEvent? {
        if (dataLines.isEmpty() && eventName.isEmpty()) return null
        val name = eventName.toString().ifEmpty { "message" }
        val payload = dataLines.joinToString("\n")
        dataLines.clear()
        eventName.setLength(0)
        if (payload.isBlank()) return SseEvent.Ping

        return try {
            parseJson(name, JSONObject(payload))
        } catch (e: Exception) {
            // A truncated or non-JSON frame: report it, keep the stream alive.
            SseEvent.Malformed(payload.take(120))
        }
    }

    private fun parseJson(eventName: String, json: JSONObject): SseEvent {
        // The JSON `type` is authoritative; the event: name is only a fallback.
        val type = json.optStringOrNull("type") ?: eventName

        return when (type) {
            "message_start" -> {
                val message = json.optJSONObject("message")
                SseEvent.MessageStart(
                    messageId = message?.optStringOrNull("id"),
                    inputTokens = message?.optJSONObject("usage")?.optIntOrNull("input_tokens"),
                    outputTokens = message?.optJSONObject("usage")?.optIntOrNull("output_tokens")
                )
            }

            "content_block_start" -> SseEvent.ContentBlockStart(
                index = json.optInt("index", 0),
                blockType = json.optJSONObject("content_block")?.optStringOrNull("type")
            )

            "content_block_delta" -> {
                val delta = json.optJSONObject("delta")
                val deltaType = delta?.optStringOrNull("type") ?: "text_delta"
                SseEvent.ContentBlockDelta(
                    index = json.optInt("index", 0),
                    deltaType = deltaType,
                    text = delta?.optStringOrNull("text"),
                    // Tool inputs arrive as partial JSON fragments. They are kept
                    // separate from display text and reassembled by index later.
                    partialJson = delta?.optStringOrNull("partial_json"),
                    // Raw reasoning is recorded but never rendered in the chat.
                    thinkingDelta = delta?.optStringOrNull("thinking")
                )
            }

            "content_block_stop" -> SseEvent.ContentBlockStop(json.optInt("index", 0))

            "message_delta" -> {
                val delta = json.optJSONObject("delta")
                SseEvent.MessageDelta(
                    stopReason = delta?.optStringOrNull("stop_reason"),
                    outputTokens = json.optJSONObject("usage")?.optIntOrNull("output_tokens")
                )
            }

            "message_stop" -> SseEvent.MessageStop

            "ping" -> SseEvent.Ping

            "error" -> {
                val error = json.optJSONObject("error")
                SseEvent.ServerError(
                    errorType = error?.optStringOrNull("type"),
                    message = error?.optStringOrNull("message")
                        ?: "Der Anbieter meldete einen Fehler während der Übertragung."
                )
            }

            else -> SseEvent.Unknown(type)
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).ifEmpty { null }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (isNull(key) || !has(key)) null else optInt(key)
}

/**
 * Reassembles `partial_json` fragments for one content block.
 *
 * The API streams tool-call arguments as JSON fragments that are only valid once
 * concatenated in arrival order. Parsing them early would truncate a tool input, so
 * the engine keeps the raw string and only exposes it when the block stops.
 */
class PartialJsonAssembler {

    private val fragments = StringBuilder()

    /** Appends one fragment. Order is arrival order — never sorted. */
    fun append(fragment: String) {
        fragments.append(fragment)
    }

    /** The reconstructed JSON text, as received. Never logged. */
    fun assembled(): String = fragments.toString()

    /**
     * True when the reassembled text parses as JSON.
     *
     * False means the block stopped mid-object — a truncated tool input, which the
     * caller must treat as invalid rather than execute.
     */
    fun isCompleteJson(): Boolean {
        if (fragments.isEmpty()) return false
        return try {
            org.json.JSONObject(assembled())
            true
        } catch (e: Exception) {
            false
        }
    }
}
