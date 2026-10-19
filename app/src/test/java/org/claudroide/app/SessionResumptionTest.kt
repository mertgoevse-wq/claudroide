package org.claudroide.app

import org.claudroide.app.feature.chat.PersistedSessionRecord
import org.claudroide.app.feature.chat.SessionResumptionManager
import org.junit.Assert.*
import org.junit.Test

class SessionResumptionTest {

    @Test
    fun resumeSession_bindsCorrectProjectModelAndProvider() {
        val record = PersistedSessionRecord(
            conversationId = "sess-42",
            projectId = "proj-android",
            projectName = "Android Engine",
            modelId = "claude-sonnet-5-5",
            providerName = "Anthropic",
            messages = listOf("Hello", "Hi there")
        )

        val resumed = SessionResumptionManager.resumeSession(record)
        assertEquals("sess-42", resumed.conversationId)
        assertEquals("proj-android", resumed.projectId)
        assertEquals("Android Engine", resumed.projectName)
        assertEquals("claude-sonnet-5-5", resumed.modelId)
        assertEquals("Anthropic", resumed.providerName)
        assertEquals(2, resumed.messages.size)
        assertNull(resumed.interruptedAction)
        assertTrue(resumed.isProjectBound)
    }

    @Test
    fun resumeSession_flagsInterruptedActionForManualReview() {
        val record = PersistedSessionRecord(
            conversationId = "sess-interrupted",
            projectId = null,
            projectName = null,
            modelId = "claude-opus-5-5",
            providerName = "Anthropic",
            messages = listOf("Write build.gradle"),
            pendingToolName = "FileEditor",
            pendingTargetPath = "app/build.gradle.kts",
            wasPendingCommitted = false
        )

        val resumed = SessionResumptionManager.resumeSession(record)
        assertNotNull(resumed.interruptedAction)
        assertEquals("FileEditor", resumed.interruptedAction!!.toolName)
        assertEquals("app/build.gradle.kts", resumed.interruptedAction!!.targetPath)
        assertFalse(resumed.interruptedAction!!.wasCommittedBeforeInterruption)
        // Invariant: Never silently re-execute; manual review required
        assertTrue(resumed.interruptedAction!!.requiresManualReview)
    }

    @Test
    fun resumeSession_enforcesContextReviewBeforeDispatch() {
        val record = PersistedSessionRecord(
            conversationId = "sess-verify",
            projectId = null,
            projectName = null,
            modelId = "claude-haiku-4-5-20251001",
            providerName = "Anthropic",
            messages = listOf("Task instruction")
        )

        val resumed = SessionResumptionManager.resumeSession(record)
        // Guard: Outbound dispatch locked until confirmed
        assertTrue(resumed.isContextReviewRequiredBeforeDispatch)
        assertFalse(resumed.canDispatchWithoutReview)

        // After user review confirmation: dispatch unlocked
        val confirmed = SessionResumptionManager.confirmContextReview(resumed)
        assertFalse(confirmed.isContextReviewRequiredBeforeDispatch)
        assertTrue(confirmed.canDispatchWithoutReview)
    }
}
