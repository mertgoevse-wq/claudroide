package org.claudroide.app.feature.chat

/**
 * Streaming state of an in-flight answer.
 *
 * Design invariants:
 *  - Only [COMPLETED] counts as a finished answer. An interrupted stream is never
 *    silently promoted to "done" (Task 036 acceptance criterion).
 *  - Text already delivered to the UI is never discarded, so an interrupted answer
 *    still shows what actually arrived.
 *  - A network failure never duplicates content and never implies a new request was
 *    billed. [StreamOutcome.Failed] carries the partial text separately from
 *    [StreamOutcome.Interrupted] so the UI can explain the difference.
 */
enum class StreamState {
    /** Nothing has arrived yet. */
    IDLE,

    /** The request was sent, the response headers arrived, no content yet. */
    CONNECTING,

    /** At least one content delta arrived; [StreamingResponse.snapshot] grows. */
    RECEIVING,

    /** message_stop seen — the answer is final. */
    COMPLETED,

    /** The user aborted. Partial text is kept, the answer is not marked final. */
    ABORTED,

    /** The transport failed. Partial text is kept, no retry is started here. */
    FAILED
}

/** Why the stream stopped, when it stopped for a reason other than success. */
enum class StopReason {
    END_TURN,
    MAX_TOKENS,
    TOOL_USE,
    /** No stop_reason arrived before the transport ended. */
    UNKNOWN
}

/**
 * A single parsed server-sent event from the Anthropic Messages API.
 *
 * ClauDroide talks to the provider over raw HTTP, so the wire format is parsed here
 * rather than by an SDK. Unknown event types are preserved as [Unknown] so a new
 * server event can never crash the stream.
 */
sealed class SseEvent {
    /** `message_start` — carries the message id and initial usage. */
    data class MessageStart(
        val messageId: String?,
        val inputTokens: Int?,
        val outputTokens: Int?
    ) : SseEvent()

    /** `content_block_start` — a new block at [index] begins. */
    data class ContentBlockStart(val index: Int, val blockType: String?) : SseEvent()

    /**
     * `content_block_delta` — an incremental update to block [index].
     *
     * Only [text] and [partialJson] carry user-visible payload. [thinkingDelta] is
     * recorded but not rendered: ClauDroide never shows raw model reasoning.
     */
    data class ContentBlockDelta(
        val index: Int,
        val deltaType: String,
        val text: String? = null,
        val partialJson: String? = null,
        val thinkingDelta: String? = null
    ) : SseEvent()

    /** `content_block_stop` — block [index] is complete. */
    data class ContentBlockStop(val index: Int) : SseEvent()

    /** `message_delta` — top-level stop_reason and cumulative usage. */
    data class MessageDelta(
        val stopReason: String?,
        val outputTokens: Int?
    ) : SseEvent()

    /** `message_stop` — the stream is finished. */
    object MessageStop : SseEvent()

    /** `ping` — keep-alive; carries no content. */
    object Ping : SseEvent()

    /**
     * `error` — the server reported a mid-stream failure.
     * [errorType] and [message] come from the server, never from local guesswork.
     */
    data class ServerError(val errorType: String?, val message: String) : SseEvent()

    /** An event type this build does not know. Ignored, never fatal. */
    data class Unknown(val type: String) : SseEvent()

    /** A line that is not valid SSE framing. Ignored, never fatal. */
    data class Malformed(val raw: String) : SseEvent()
}

/**
 * The observable state of one streaming answer.
 *
 * This is a plain immutable snapshot: the UI renders it, tests assert on it, and no
 * coroutine or Android type is involved, so it can be verified on the JVM.
 */
data class StreamingResponse(
    val state: StreamState = StreamState.IDLE,
    val text: String = "",
    val stopReason: StopReason? = null,
    val messageId: String? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val errorMessage: String? = null
) {
    /**
     * True only when the server confirmed the end of the answer.
     *
     * An aborted or failed stream returns false, so a partial answer is never
     * rendered or stored as if it were complete.
     */
    val isComplete: Boolean get() = state == StreamState.COMPLETED

    /**
     * True when text arrived but the answer is not final.
     * The UI shows this as "incomplete answer", not as a finished message.
     */
    val isIncomplete: Boolean
        get() = (state == StreamState.ABORTED || state == StreamState.FAILED) && text.isNotEmpty()

    /** True when nothing at all was received — no half-empty message is shown. */
    val hasContent: Boolean get() = text.isNotEmpty()

    /** Cost-relevant usage, only when the server actually reported it. */
    val usageReported: Boolean get() = inputTokens != null || outputTokens != null
}

/**
 * Folds a sequence of [SseEvent]s into a [StreamingResponse].
 *
 * The engine is deliberately pure and synchronous: it never opens a socket and never
 * retries. Network concerns live in the transport layer, which feeds it events and
 * decides when to stop. That split is what makes the safety rules testable.
 */
class StreamingResponseEngine {

    private var snapshot = StreamingResponse()
    private val blockText = mutableMapOf<Int, StringBuilder>()
    private var terminal = false

    /** The current state. */
    fun current(): StreamingResponse = snapshot

    /**
     * Applies one event and returns the new state.
     *
     * Events arriving after a terminal state are ignored: once the answer is
     * completed or aborted, late deltas must not append text a second time.
     */
    fun accept(event: SseEvent): StreamingResponse {
        if (terminal) return snapshot

        when (event) {
            is SseEvent.MessageStart -> {
                snapshot = snapshot.copy(
                    state = StreamState.CONNECTING,
                    messageId = event.messageId,
                    inputTokens = event.inputTokens,
                    outputTokens = event.outputTokens
                )
            }

            is SseEvent.ContentBlockStart -> {
                if (snapshot.state == StreamState.IDLE) {
                    snapshot = snapshot.copy(state = StreamState.CONNECTING)
                }
                blockText.getOrPut(event.index) { StringBuilder() }
            }

            is SseEvent.ContentBlockDelta -> {
                val delta = event.text
                if (!delta.isNullOrEmpty()) {
                    // Append to this block only, then re-assemble in index order, so
                    // interleaved tool blocks cannot scramble the visible text.
                    blockText.getOrPut(event.index) { StringBuilder() }.append(delta)
                    snapshot = snapshot.copy(
                        state = StreamState.RECEIVING,
                        text = blockText.entries.sortedBy { it.key }
                            .joinToString("") { it.value.toString() }
                    )
                }
            }

            is SseEvent.ContentBlockStop -> Unit // block completeness is implicit

            is SseEvent.MessageDelta -> {
                val stop = mapStopReason(event.stopReason)
                snapshot = snapshot.copy(
                    stopReason = stop ?: snapshot.stopReason,
                    outputTokens = event.outputTokens ?: snapshot.outputTokens
                )
            }

            SseEvent.MessageStop -> {
                terminal = true
                // A message_stop without a preceding stop_reason is still a finished
                // answer; the reason is simply unknown, not the answer itself.
                snapshot = snapshot.copy(
                    state = StreamState.COMPLETED,
                    stopReason = snapshot.stopReason ?: StopReason.UNKNOWN
                )
            }

            SseEvent.Ping -> Unit

            is SseEvent.ServerError -> {
                terminal = true
                snapshot = snapshot.copy(
                    state = StreamState.FAILED,
                    errorMessage = event.message
                )
            }

            is SseEvent.Unknown, is SseEvent.Malformed -> Unit // tolerate, never abort
        }
        return snapshot
    }

    /**
     * Folds a whole batch of events. Convenience for tests and for buffered replays.
     */
    fun acceptAll(events: List<SseEvent>): StreamingResponse {
        events.forEach { accept(it) }
        return snapshot
    }

    /**
     * Ends the stream because the user aborted.
     *
     * Partial text is preserved. The answer is not marked complete, so it can never
     * be mistaken for a finished response.
     */
    fun abort(): StreamingResponse {
        if (terminal) return snapshot
        terminal = true
        return snapshot.copy(state = StreamState.ABORTED).also { snapshot = it }
    }

    /**
     * Ends the stream because the transport failed.
     *
     * [reason] is shown to the user. No retry is started from here — the transport
     * layer decides, so a failure can never silently trigger a second billed request.
     */
    fun fail(reason: String): StreamingResponse {
        if (terminal) return snapshot
        terminal = true
        return snapshot.copy(state = StreamState.FAILED, errorMessage = reason).also { snapshot = it }
    }

    /**
     * Ends the stream because the transport closed without a `message_stop`.
     *
     * This is the reconnect case: the answer is incomplete, not failed and not done.
     */
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
