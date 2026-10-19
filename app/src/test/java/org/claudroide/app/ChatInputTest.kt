package org.claudroide.app

import org.claudroide.app.feature.chat.AttachmentItem
import org.claudroide.app.feature.chat.AttachmentPolicy
import org.claudroide.app.feature.chat.ChatInputState
import org.claudroide.app.feature.chat.InputActionButtonState
import org.junit.Assert.*
import org.junit.Test

class ChatInputTest {

    @Test
    fun actionButtonState_transitionsCorrectlyBetweenSendAndDisabled() {
        val emptyState = ChatInputState(text = "")
        assertEquals(InputActionButtonState.DISABLED, emptyState.actionButtonState)

        val whitespaceState = ChatInputState(text = "   \n  ")
        assertEquals(InputActionButtonState.DISABLED, whitespaceState.actionButtonState)

        val textState = ChatInputState(text = "Implement navigation structure")
        assertEquals(InputActionButtonState.SEND, textState.actionButtonState)
    }

    @Test
    fun activeStreaming_transformsActionButtonToStop() {
        // While streaming, action button must always be STOP to prevent double requests
        val streamingWithText = ChatInputState(
            text = "Follow up request",
            isStreaming = true
        )
        assertEquals(InputActionButtonState.STOP, streamingWithText.actionButtonState)

        val streamingEmpty = ChatInputState(
            text = "",
            isStreaming = true
        )
        assertEquals(InputActionButtonState.STOP, streamingEmpty.actionButtonState)
    }

    @Test
    fun attachmentPolicy_blocksTransmissionWithoutExplicitProviderConsent() {
        val attachment = AttachmentItem(
            id = "att-1",
            name = "MainActivity.kt",
            mimeType = "text/x-kotlin",
            sizeBytes = 2048,
            providerConsentGiven = false
        )

        assertFalse(
            AttachmentPolicy.canTransmitAttachment(attachment, "Anthropic Claude API")
        )

        val stateWithUnapprovedAttachment = ChatInputState(
            text = "Inspect this file",
            attachments = listOf(attachment)
        )
        assertEquals(1, stateWithUnapprovedAttachment.pendingAttachmentConsentsCount)
        assertEquals(InputActionButtonState.DISABLED, stateWithUnapprovedAttachment.actionButtonState)
    }

    @Test
    fun attachmentPolicy_enablesSendWhenConsentProvidedAndWithinSizeLimits() {
        val approvedAttachment = AttachmentItem(
            id = "att-2",
            name = "gradle.properties",
            mimeType = "text/plain",
            sizeBytes = 1024,
            providerConsentGiven = true
        )

        assertTrue(
            AttachmentPolicy.canTransmitAttachment(approvedAttachment, "Anthropic Claude API")
        )

        val stateWithApprovedAttachment = ChatInputState(
            text = "Here is the config",
            attachments = listOf(approvedAttachment)
        )
        assertEquals(0, stateWithApprovedAttachment.pendingAttachmentConsentsCount)
        assertEquals(InputActionButtonState.SEND, stateWithApprovedAttachment.actionButtonState)
    }

    @Test
    fun attachmentPolicy_strictlyRejectsOversizedFilesOrEmptyProvider() {
        val hugeAttachment = AttachmentItem(
            id = "att-huge",
            name = "dump.bin",
            mimeType = "application/octet-stream",
            sizeBytes = 15 * 1024 * 1024, // 15 MB > 10 MB limit
            providerConsentGiven = true
        )
        assertFalse(
            AttachmentPolicy.canTransmitAttachment(hugeAttachment, "Anthropic Claude API")
        )

        val validAttachment = AttachmentItem(
            id = "att-3",
            name = "small.txt",
            mimeType = "text/plain",
            sizeBytes = 500,
            providerConsentGiven = true
        )
        assertFalse(
            AttachmentPolicy.canTransmitAttachment(validAttachment, "")
        )
    }
}
