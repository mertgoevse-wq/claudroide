package org.claudroide.app.feature.provider.network

import org.json.JSONObject

// --- Stream state ---

enum class StreamState {
    IDLE, CONNECTING, RECEIVING, COMPLETED, ABORTED, FAILED
}

enum class StopReason { END_TURN, MAX_TOKENS, TOOL_USE, UNKNOWN }

// --- SSE wire types ---

sealed class SseEvent {
    data class MessageStart(val messageId: String?, val inputTokens: Int?, val outputTokens: Int?) : SseEvent()
    data class ContentBlockStart(val index: Int, val blockType: String?) : SseEvent()
    data class ContentBlockDelta(
        val index: Int, val deltaType: String,
        val text: String? = null, val partialJson: String? = null, val thinkingDelta: String? = null
    ) : SseEvent()
    data class ContentBlockStop(val index: Int) : SseEvent()
    data class MessageDelta(val stopReason: String?, val outputTokens: Int?) : SseEvent()
    object MessageStop : SseEvent()
    object Ping : SseEvent()
    data class ServerError(val errorType: String?, val message: String) : SseEvent()
    data class Unknown(val type: String) : SseEvent()
    data class Malformed(val raw: String) : SseEvent()
}

// --- Observable streaming response ---

data class StreamingResponse(
    val state: StreamState = StreamState.IDLE,
    val text: String = "",
    val stopReason: StopReason? = null,
    val messageId: String? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val errorMessage: String? = null
) {
    val isComplete: Boolean get() = state == StreamState.COMPLETED
    val isIncomplete: Boolean
        get() = (state == StreamState.ABORTED || state == StreamState.FAILED) && text.isNotEmpty()
    val hasContent: Boolean get() = text.isNotEmpty()
    val usageReported: Boolean get() = inputTokens != null || outputTokens != null
}

// --- SSE parser ---

class SseEventParser {
    private val eventName = StringBuilder()
    private val dataLines = mutableListOf<String>()

    fun acceptLine(line: String): SseEvent? = when {
        line.startsWith(":") -> null
        line.isEmpty() -> flush()
        line.startsWith("data:") -> { dataLines += line.removePrefix("data:").removePrefix(" "); null }
        line.startsWith("event:") -> { eventName.setLength(0); eventName.append(line.removePrefix("event:").removePrefix(" ")); null }
        else -> null
    }

    fun parseBlock(block: String): SseEvent {
        block.lineSequence().forEach { acceptLine(it) }
        return flush() ?: SseEvent.Malformed(block.take(120))
    }

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
        return try { parseJson(name, JSONObject(payload)) }
        catch (e: Exception) { SseEvent.Malformed(payload.take(120)) }
    }

    private fun parseJson(eventName: String, json: JSONObject): SseEvent {
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
                    index = json.optInt("index", 0), deltaType = deltaType,
                    text = delta?.optStringOrNull("text"),
                    partialJson = delta?.optStringOrNull("partial_json"),
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

class PartialJsonAssembler {
    private val fragments = StringBuilder()
    fun append(fragment: String) { fragments.append(fragment) }
    fun assembled(): String = fragments.toString()
    fun isCompleteJson(): Boolean {
        if (fragments.isEmpty()) return false
        return try { org.json.JSONObject(assembled()); true }
        catch (e: Exception) { false }
    }
}

// --- Streaming engine ---

class StreamingResponseEngine {
    private var snapshot = StreamingResponse()
    private val blockText = mutableMapOf<Int, StringBuilder>()
    private var terminal = false

    fun current(): StreamingResponse = snapshot

    fun accept(event: SseEvent): StreamingResponse {
        if (terminal) return snapshot
        when (event) {
            is SseEvent.MessageStart -> snapshot = snapshot.copy(
                state = StreamState.CONNECTING, messageId = event.messageId,
                inputTokens = event.inputTokens, outputTokens = event.outputTokens
            )
            is SseEvent.ContentBlockStart -> {
                if (snapshot.state == StreamState.IDLE) snapshot = snapshot.copy(state = StreamState.CONNECTING)
                blockText.getOrPut(event.index) { StringBuilder() }
            }
            is SseEvent.ContentBlockDelta -> {
                val delta = event.text
                if (!delta.isNullOrEmpty()) {
                    blockText.getOrPut(event.index) { StringBuilder() }.append(delta)
                    snapshot = snapshot.copy(
                        state = StreamState.RECEIVING,
                        text = blockText.entries.sortedBy { it.key }.joinToString("") { it.value.toString() }
                    )
                }
            }
            is SseEvent.MessageDelta -> {
                val stop = mapStopReason(event.stopReason)
                snapshot = snapshot.copy(
                    stopReason = stop ?: snapshot.stopReason,
                    outputTokens = event.outputTokens ?: snapshot.outputTokens
                )
            }
            SseEvent.MessageStop -> {
                terminal = true
                snapshot = snapshot.copy(state = StreamState.COMPLETED, stopReason = snapshot.stopReason ?: StopReason.UNKNOWN)
            }
            SseEvent.Ping -> Unit
            is SseEvent.ServerError -> {
                terminal = true
                snapshot = snapshot.copy(state = StreamState.FAILED, errorMessage = event.message)
            }
            is SseEvent.Unknown, is SseEvent.ContentBlockStop, is SseEvent.Malformed -> Unit
        }
        return snapshot
    }

    fun abort(): StreamingResponse {
        if (terminal) return snapshot
        terminal = true
        return snapshot.copy(state = StreamState.ABORTED).also { snapshot = it }
    }

    fun fail(reason: String): StreamingResponse {
        if (terminal) return snapshot
        terminal = true
        return snapshot.copy(state = StreamState.FAILED, errorMessage = reason).also { snapshot = it }
    }

    fun interrupted(): StreamingResponse {
        if (terminal) return snapshot
        terminal = true
        return snapshot.copy(state = StreamState.ABORTED).also { snapshot = it }
    }

    private fun mapStopReason(raw: String?): StopReason? = when (raw) {
        "end_turn" -> StopReason.END_TURN
        "max_tokens" -> StopReason.MAX_TOKENS
        "tool_use" -> StopReason.TOOL_USE
        null -> null
        else -> StopReason.UNKNOWN
    }
}
