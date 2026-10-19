package org.claudroide.app

import org.claudroide.app.feature.chat.ChatListUiState
import org.claudroide.app.feature.chat.ChatSummaryItem
import org.junit.Assert.*
import org.junit.Test

class ChatListTest {

    @Test
    fun chatListUiState_correctlyIdentifiesEmptyState() {
        val emptyState = ChatListUiState(chats = emptyList(), isLoading = false)
        assertTrue(emptyState.isEmpty)

        val loadingEmptyState = ChatListUiState(chats = emptyList(), isLoading = true)
        assertFalse(loadingEmptyState.isEmpty)

        val populatedState = ChatListUiState(
            chats = listOf(
                ChatSummaryItem(
                    id = "c1",
                    title = "Test Chat",
                    rawPreviewSnippet = "Hello world",
                    lastModifiedTimestampMs = 1000L
                )
            ),
            isLoading = false
        )
        assertFalse(populatedState.isEmpty)
    }

    @Test
    fun chatListUiState_sortsByLastModifiedDescending() {
        val chatOld = ChatSummaryItem("c1", "Old", "Snippet 1", 1000L)
        val chatNew = ChatSummaryItem("c2", "New", "Snippet 2", 5000L)
        val chatMid = ChatSummaryItem("c3", "Mid", "Snippet 3", 3000L)

        val state = ChatListUiState(chats = listOf(chatOld, chatNew, chatMid))
        val sorted = state.filteredChats

        assertEquals("c2", sorted[0].id)
        assertEquals("c3", sorted[1].id)
        assertEquals("c1", sorted[2].id)
    }

    @Test
    fun chatListUiState_filtersByProjectIdCorrectly() {
        val chatProjA = ChatSummaryItem("c1", "Feature A", "Work", 1000L, projectId = "proj-a", projectName = "Project Alpha")
        val chatProjB = ChatSummaryItem("c2", "Bugfix B", "Fix", 2000L, projectId = "proj-b", projectName = "Project Beta")
        val chatUnbound = ChatSummaryItem("c3", "General Chat", "Ideas", 3000L, projectId = null)

        val stateAll = ChatListUiState(chats = listOf(chatProjA, chatProjB, chatUnbound))
        assertEquals(3, stateAll.filteredChats.size)

        val stateFilterA = ChatListUiState(
            chats = listOf(chatProjA, chatProjB, chatUnbound),
            selectedProjectIdFilter = "proj-a"
        )
        val filtered = stateFilterA.filteredChats
        assertEquals(1, filtered.size)
        assertEquals("c1", filtered[0].id)
        assertTrue(filtered[0].isProjectBound)
        assertEquals("Project Alpha", filtered[0].projectName)
    }

    @Test
    fun chatSummaryItem_sanitizesPreviewSnippetSecrets() {
        val sensitiveChat = ChatSummaryItem(
            id = "c-secret",
            title = "API Testing",
            rawPreviewSnippet = "Use secret key sk-ant-api03-secret1234567890 to test",
            lastModifiedTimestampMs = 2000L
        )

        val preview = sensitiveChat.sanitizedPreview
        assertFalse(preview.contains("sk-ant-api03"))
        assertTrue(preview.contains("[SCHLÜSSEL AUSGEBLENDET]"))
    }
}
