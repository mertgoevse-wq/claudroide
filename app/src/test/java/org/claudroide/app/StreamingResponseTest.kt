package org.claudroide.app

import org.claudroide.app.feature.chat.PartialJsonAssembler
import org.claudroide.app.feature.chat.SseEvent
import org.claudroide.app.feature.chat.SseEventParser
import org.claudroide.app.feature.chat.StopReason
import org.claudroide.app.feature.chat.StreamState
import org.claudroide.app.feature.chat.StreamingResponseEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 036 — "Laufende Antworten".
 *
 * Acceptance criteria under test:
 *  - An aborted answer is never marked complete.
 *  - A network error neither duplicates content nor silently starts new costs.
 */
class StreamingResponseTest {

    // ── Normal stream ──────────────────────────────────────────────────────────

    @Test
    fun completeStream_reportsEndTurnAndFullText() {
        val engine = StreamingResponseEngine()
        val events = SseEventParser().parseAll(
            """
            event: message_start
            data: {"type":"message_start","message":{"id":"msg_1","usage":{"input_tokens":10,"output_tokens":1}}}

            event: content_block_start
            data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hallo"}}

            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":" Welt"}}

            event: content_block_stop
            data: {"type":"content_block_stop","index":0}

            event: message_delta
            data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":12}}

            event: message_stop
            data: {"type":"message_stop"}

            """.trimIndent()
        )

        val result = engine.acceptAll(events)

        assertEquals(StreamState.COMPLETED, result.state)
        assertEquals("Hallo Welt", result.text)
        assertEquals(StopReason.END_TURN, result.stopReason)
        assertEquals("msg_1", result.messageId)
        assertEquals(10, result.inputTokens)
        assertEquals(12, result.outputTokens)
        assertTrue(result.isComplete)
        assertFalse(result.isIncomplete)
    }

    @Test
    fun textGrowsIncrementally_whileReceiving() {
        val engine = StreamingResponseEngine()
        engine.accept(SseEvent.ContentBlockStart(0, "text"))
        assertEquals(StreamState.CONNECTING, engine.current().state)

        engine.accept(SseEvent.ContentBlockDelta(0, "text_delta", text = "Hal"))
        val afterFirst = engine.current()
        assertEquals(StreamState.RECEIVING, afterFirst.state)
        assertEquals("Hal", afterFirst.text)
        assertFalse("partial text is not a finished answer", afterFirst.isComplete)

        engine.accept(SseEvent.ContentBlockDelta(0, "text_delta", text = "lo"))
        assertEquals("Hallo", engine.current().text)
    }

    @Test
    fun interleavedBlocks_areReassembledInIndexOrder() {
        val engine = StreamingResponseEngine()
        engine.acceptAll(
            listOf(
                SseEvent.ContentBlockStart(0, "text"),
                SseEvent.ContentBlockStart(1, "tool_use"),
                // Tool fragments arrive on block 1 but are not display text.
                SseEvent.ContentBlockDelta(1, "input_json_delta", partialJson = "{\"a\":"),
                SseEvent.ContentBlockDelta(0, "text_delta", text = "Antwort"),
                SseEvent.ContentBlockDelta(1, "input_json_delta", partialJson = "1}"),
                SseEvent.ContentBlockDelta(0, "text_delta", text = " hier"),
                SseEvent.MessageStop
            )
        )

        val result = engine.current()
        assertEquals("tool JSON must not leak into the visible text", "Antwort hier", result.text)
        assertTrue(result.isComplete)
    }

    // ── Criterion 1: an aborted answer is never marked complete ────────────────

    @Test
    fun abortedStream_isNotComplete_butKeepsPartialText() {
        val engine = StreamingResponseEngine()
        engine.acceptAll(
            listOf(
                SseEvent.ContentBlockStart(0, "text"),
                SseEvent.ContentBlockDelta(0, "text_delta", text = "Halbe Antwort")
            )
        )

        val result = engine.abort()

        assertEquals(StreamState.ABORTED, result.state)
        assertFalse("aborted must never be reported as complete", result.isComplete)
        assertTrue(result.isIncomplete)
        assertEquals("Halbe Antwort", result.text)
    }

    @Test
    fun interruptedStream_isNotComplete() {
        val engine = StreamingResponseEngine()
        engine.accept(SseEvent.ContentBlockDelta(0, "text_delta", text = "Teiltext"))

        val result = engine.interrupted()

        assertEquals(StreamState.ABORTED, result.state)
        assertFalse(result.isComplete)
        assertTrue(result.isIncomplete)
    }

    @Test
    fun abortAfterCompletion_doesNotUndoTheFinishedAnswer() {
        val engine = StreamingResponseEngine()
        engine.acceptAll(
            listOf(
                SseEvent.ContentBlockDelta(0, "text_delta", text = "Fertig"),
                SseEvent.MessageDelta("end_turn", 5),
                SseEvent.MessageStop
            )
        )

        val result = engine.abort()

        assertEquals(StreamState.COMPLETED, result.state)
        assertTrue(result.isComplete)
    }

    // ── Criterion 2: a network error must not duplicate content ────────────────

    @Test
    fun failedStream_keepsTextOnce_andDoesNotDuplicate() {
        val engine = StreamingResponseEngine()
        engine.acceptAll(
            listOf(
                SseEvent.ContentBlockStart(0, "text"),
                SseEvent.ContentBlockDelta(0, "text_delta", text = "Teil eins "),
                SseEvent.ContentBlockDelta(0, "text_delta", text = "Teil zwei")
            )
        )

        val result = engine.fail("Verbindung unterbrochen.")

        assertEquals(StreamState.FAILED, result.state)
        assertFalse(result.isComplete)
        assertEquals("content must appear exactly once", "Teil eins Teil zwei", result.text)
        assertEquals("Verbindung unterbrochen.", result.errorMessage)
    }

    @Test
    fun eventsAfterTerminalState_areIgnored_soTextCannotBeAppendedTwice() {
        val engine = StreamingResponseEngine()
        engine.accept(SseEvent.ContentBlockDelta(0, "text_delta", text = "Antwort"))
        engine.fail("Netzwerkfehler")

        // A late delta from a still-open connection must not revive or extend the answer.
        engine.accept(SseEvent.ContentBlockDelta(0, "text_delta", text = " Antwort"))
        engine.accept(SseEvent.MessageStop)

        val result = engine.current()
        assertEquals(StreamState.FAILED, result.state)
        assertEquals("Antwort", result.text)
        assertFalse(result.isComplete)
    }

    @Test
    fun serverError_isReportedAsFailure_withServerMessage() {
        val engine = StreamingResponseEngine()
        val events = SseEventParser().parseAll(
            """
            event: error
            data: {"type":"error","error":{"type":"overloaded_error","message":"Server überlastet"}}
            """.trimIndent()
        )

        val result = engine.acceptAll(events)

        assertEquals(StreamState.FAILED, result.state)
        assertEquals("Server überlastet", result.errorMessage)
        assertFalse(result.isComplete)
    }

    // ── Usage and stop reasons ─────────────────────────────────────────────────

    @Test
    fun maxTokensStopReason_isDistinguishedFromEndTurn() {
        val engine = StreamingResponseEngine()
        val result = engine.acceptAll(
            listOf(
                SseEvent.ContentBlockDelta(0, "text_delta", text = "abgeschnitten"),
                SseEvent.MessageDelta("max_tokens", 64),
                SseEvent.MessageStop
            )
        )

        assertEquals(StopReason.MAX_TOKENS, result.stopReason)
        assertTrue("server confirmed the end", result.isComplete)
    }

    @Test
    fun messageStop_withoutStopReason_isCompleteWithUnknownReason() {
        val engine = StreamingResponseEngine()
        val result = engine.acceptAll(
            listOf(
                SseEvent.ContentBlockDelta(0, "text_delta", text = "Text"),
                SseEvent.MessageStop
            )
        )

        assertTrue(result.isComplete)
        assertEquals(StopReason.UNKNOWN, result.stopReason)
    }

    @Test
    fun usageIsAbsent_whenServerDidNotReportIt() {
        val engine = StreamingResponseEngine()
        val result = engine.acceptAll(
            listOf(
                SseEvent.ContentBlockDelta(0, "text_delta", text = "Text"),
                SseEvent.MessageStop
            )
        )

        assertNull(result.inputTokens)
        assertNull(result.outputTokens)
        assertFalse("no invented numbers", result.usageReported)
    }

    // ── Parser robustness ──────────────────────────────────────────────────────

    @Test
    fun unknownEventType_isIgnored_andStreamContinues() {
        val parser = SseEventParser()
        val events = parser.parseAll(
            """
            event: some_future_event
            data: {"type":"some_future_event","payload":1}

            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"ok"}}
            """.trimIndent()
        )

        assertTrue(events.first() is SseEvent.Unknown)
        assertTrue(
            "a later known event must still parse",
            events.any { it is SseEvent.ContentBlockDelta }
        )
    }

    @Test
    fun malformedJsonLine_doesNotAbortTheStream() {
        val parser = SseEventParser()
        val events = parser.parseAll(
            """
            event: content_block_delta
            data: {"type":"content_block_delta",,,,broken

            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"trotzdem"}}
            """.trimIndent()
        )

        assertTrue(events.any { it is SseEvent.Malformed })
        val engine = StreamingResponseEngine()
        val result = engine.acceptAll(events)
        assertEquals("trotzdem", result.text)
    }

    @Test
    fun pingAndCommentLines_carryNoContent() {
        val parser = SseEventParser()
        val events = parser.parseAll(
            """
            : keep-alive comment

            event: ping
            data: {"type":"ping"}

            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Text"}}
            """.trimIndent()
        )

        val engine = StreamingResponseEngine()
        val result = engine.acceptAll(events)
        assertEquals("Text", result.text)
    }

    @Test
    fun thinkingDelta_isParsedButNeverRendered() {
        val parser = SseEventParser()
        val events = parser.parseAll(
            """
            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"interne Gedankengänge"}}
            """.trimIndent()
        )

        val delta = events.filterIsInstance<SseEvent.ContentBlockDelta>().single()
        assertEquals("interne Gedankengänge", delta.thinkingDelta)

        val engine = StreamingResponseEngine()
        assertEquals(
            "raw reasoning must not become visible text",
            "",
            engine.acceptAll(events).text
        )
    }

    // ── Partial JSON for tool calls ────────────────────────────────────────────

    @Test
    fun partialJsonFragments_reassembleInArrivalOrder() {
        val assembler = PartialJsonAssembler()
        assembler.append("{\"path\":")
        assembler.append("\"/etc/hosts\"")
        assembler.append("}")

        assertEquals("{\"path\":\"/etc/hosts\"}", assembler.assembled())
        assertTrue(assembler.isCompleteJson())
    }

    @Test
    fun truncatedPartialJson_isReportedAsIncomplete() {
        val assembler = PartialJsonAssembler()
        assembler.append("{\"path\": \"/etc/ho")

        assertFalse(
            "a truncated tool input must never look valid",
            assembler.isCompleteJson()
        )
    }
}
