package org.claudroide.app.feature.chat

import org.json.JSONArray
import org.json.JSONObject
import org.claudroide.app.feature.chat.removePromptFieldsFromSessionsJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SessionDataStorePromptMigrationTest {
    @Test
    fun `removes legacy prompt fields and preserves session data`() {
        val legacy = JSONArray().put(
            JSONObject()
                .put("conversationId", "chat-1")
                .put("messages", JSONArray().put("Question").put("Answer"))
                .put("activePromptTitle", "Legacy role")
                .put("systemPrompt", "Legacy prompt content")
        ).toString()

        val migrated = JSONArray(removePromptFieldsFromSessionsJson(legacy))
        val session = migrated.getJSONObject(0)

        assertEquals("chat-1", session.getString("conversationId"))
        assertEquals("Question", session.getJSONArray("messages").getString(0))
        assertFalse(session.has("activePromptTitle"))
        assertFalse(session.has("systemPrompt"))
    }
}
