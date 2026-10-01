package org.claudroide.app

import org.claudroide.app.feature.chat.ChatSearchEngine
import org.claudroide.app.feature.chat.ChatSearchFilter
import org.claudroide.app.feature.chat.MatchField
import org.claudroide.app.feature.chat.SearchableConversation
import org.junit.Assert.*
import org.junit.Test

class ChatSearchTest {

    private val sampleData = listOf(
        SearchableConversation(
            id = "c1",
            title = "Refactor Authentication",
            messages = listOf("We need to add biometric prompt", "OAuth token storage in Keystore"),
            timestampMs = 1000L,
            projectId = "p-security",
            projectName = "Security Suite",
            isDeleted = false
        ),
        SearchableConversation(
            id = "c2",
            title = "UI Theme Fixes",
            messages = listOf("AMOLED black background color definition", "Material 3 elevation"),
            timestampMs = 2000L,
            projectId = "p-ui",
            projectName = "Design System",
            isDeleted = false
        ),
        SearchableConversation(
            id = "c3",
            title = "Old Deleted Chat",
            messages = listOf("Biometric prompt discussion from last month"),
            timestampMs = 500L,
            projectId = "p-security",
            isDeleted = true // Deleted!
        )
    )

    @Test
    fun chatSearchEngine_isStrictlyLocal() {
        assertTrue(ChatSearchEngine.IS_STRICTLY_LOCAL)
    }

    @Test
    fun searchByTitle_returnsMatchingConversation() {
        val filter = ChatSearchFilter(query = "Refactor")
        val results = ChatSearchEngine.search(filter, sampleData)

        assertEquals(1, results.size)
        assertEquals("c1", results[0].chatId)
        assertEquals(MatchField.TITLE, results[0].matchedField)
    }

    @Test
    fun searchByMessageContent_extractsContextualSnippet() {
        val filter = ChatSearchFilter(query = "elevation")
        val results = ChatSearchEngine.search(filter, sampleData)

        assertEquals(1, results.size)
        assertEquals("c2", results[0].chatId)
        assertEquals(MatchField.MESSAGE_CONTENT, results[0].matchedField)
        assertTrue(results[0].snippet.contains("elevation"))
    }

    @Test
    fun deletedConversations_neverAppearInSearchResults() {
        // "biometric" occurs in c1 (active) and c3 (deleted)
        val filter = ChatSearchFilter(query = "biometric")
        val results = ChatSearchEngine.search(filter, sampleData)

        assertEquals(1, results.size)
        assertEquals("c1", results[0].chatId)
        assertFalse(results.any { it.chatId == "c3" })
    }

    @Test
    fun projectFiltering_scopesSearchResultsToSpecificProject() {
        val filter = ChatSearchFilter(projectId = "p-ui")
        val results = ChatSearchEngine.search(filter, sampleData)

        assertEquals(1, results.size)
        assertEquals("c2", results[0].chatId)
        assertEquals("Design System", results[0].projectName)
    }

    @Test
    fun dateRangeFiltering_restrictsResults() {
        val filter = ChatSearchFilter(startDateMs = 1500L, endDateMs = 2500L)
        val results = ChatSearchEngine.search(filter, sampleData)

        assertEquals(1, results.size)
        assertEquals("c2", results[0].chatId)
    }
}
