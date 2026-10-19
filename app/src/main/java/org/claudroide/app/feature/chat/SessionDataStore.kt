package org.claudroide.app.feature.chat

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

// DataStore delegate must be a top-level property.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "claudroide_sessions")

/**
 * SessionPersistence implementation using Jetpack DataStore Preferences.
 * Provides persistent session storage that survives process restarts.
 */
class SessionDataStore(private val context: Context) : SessionPersistence {

    private object Keys {
        val CONVERSATION_ID = stringPreferencesKey("conversation_id")
        val PROVIDER_ID = stringPreferencesKey("provider_id")
        val MODEL_ID = stringPreferencesKey("model_id")
        val MESSAGES = stringPreferencesKey("messages_json")
        val TIMESTAMP = longPreferencesKey("timestamp")
        val STREAM_STATE = stringPreferencesKey("stream_state")
        val SESSIONS = stringPreferencesKey("sessions_json")
        val AGENT_STATE = stringPreferencesKey("agent_state_json")
        val TOOL_CALLS = stringPreferencesKey("tool_calls_json")
        val PROJECT_URI = stringPreferencesKey("project_uri")
        val RECOVERY_STATE = stringPreferencesKey("recovery_state_json")
    }

    override suspend fun save(record: SessionRecord) {
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.SESSIONS]?.let(::jsonToSessions)
                ?: listOfNotNull(legacySession(prefs))
            val updated = (existing.filterNot { it.conversationId == record.conversationId } + record)
                .sortedByDescending { it.timestamp }
                .take(MAX_STORED_SESSIONS)
            prefs[Keys.SESSIONS] = sessionsToJson(updated)
            prefs[Keys.CONVERSATION_ID] = record.conversationId
            prefs[Keys.PROVIDER_ID] = record.providerId
            prefs[Keys.MODEL_ID] = record.modelId
            prefs[Keys.MESSAGES] = messagesToJson(record.messages)
            prefs[Keys.TIMESTAMP] = record.timestamp
            prefs[Keys.STREAM_STATE] = record.streamState.name
            prefs.remove(stringPreferencesKey("active_prompt_title"))
            prefs.remove(stringPreferencesKey("system_prompt"))
        }
    }

    override suspend fun sessions(limit: Int): List<SessionRecord> = try {
        removePersistedPromptData()
        val preferences = context.dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .first()
        val saved = preferences[Keys.SESSIONS]?.let(::jsonToSessions)
        val records = saved ?: listOfNotNull(legacySession(preferences))
        records.sortedByDescending { it.timestamp }.take(limit.coerceIn(0, MAX_STORED_SESSIONS))
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun loadSession(conversationId: String): SessionRecord? =
        sessions(MAX_STORED_SESSIONS).firstOrNull { it.conversationId == conversationId }

    override suspend fun lastSession(): SessionRecord? = sessions().firstOrNull()

    suspend fun saveAgentState(
        conversationId: String,
        agentState: AgentRunState,
        toolCalls: List<ToolCallRecord>,
        projectUri: String?,
        recoveryState: RecoveryState
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CONVERSATION_ID] = conversationId
            prefs[Keys.AGENT_STATE] = agentStateToJson(agentState)
            prefs[Keys.TOOL_CALLS] = toolCallsToJson(toolCalls)
            prefs[Keys.PROJECT_URI] = projectUri ?: ""
            prefs[Keys.RECOVERY_STATE] = recoveryStateToJson(recoveryState)
        }
    }

    suspend fun loadAgentState(): AgentRecoveryData? = context.dataStore.data
        .map { prefs ->
            val conversationId = prefs[Keys.CONVERSATION_ID] ?: return@map null
            val agentStateJson = prefs[Keys.AGENT_STATE]
            val toolCallsJson = prefs[Keys.TOOL_CALLS]
            val projectUri = prefs[Keys.PROJECT_URI]
            val recoveryStateJson = prefs[Keys.RECOVERY_STATE]

            if ((agentStateJson == null || agentStateJson.isBlank()) &&
                (toolCallsJson == null || toolCallsJson.isBlank())) {
                return@map null
            }

            val agentState = agentStateJson?.let { jsonToAgentState(it) }
            val toolCalls = toolCallsJson?.let { jsonToToolCalls(it) } ?: emptyList()
            val recoveryState = recoveryStateJson?.let { jsonToRecoveryState(it) } ?: RecoveryState()

            AgentRecoveryData(
                conversationId = conversationId,
                agentState = agentState,
                toolCalls = toolCalls,
                projectUri = projectUri?.ifBlank { null },
                recoveryState = recoveryState
            )
        }
        .first()

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private suspend fun removePersistedPromptData() {
        context.dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("active_prompt_title"))
            prefs.remove(stringPreferencesKey("system_prompt"))
            prefs[Keys.SESSIONS]?.let { json ->
                val migrated = removePromptFieldsFromSessionsJson(json)
                if (migrated != json) prefs[Keys.SESSIONS] = migrated
            }
        }
    }

    // -- JSON serialization --

    private fun sessionsToJson(sessions: List<SessionRecord>): String = JSONArray().apply {
        sessions.forEach { record ->
            put(JSONObject().apply {
                put("conversationId", record.conversationId)
                put("providerId", record.providerId)
                put("modelId", record.modelId)
                put("messages", JSONArray(record.messages))
                put("timestamp", record.timestamp)
                put("streamState", record.streamState.name)
            })
        }
    }.toString()

    private fun jsonToSessions(json: String): List<SessionRecord> {
        val array = JSONArray(json)
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            SessionRecord(
                conversationId = obj.getString("conversationId"),
                providerId = obj.getString("providerId"),
                modelId = obj.getString("modelId"),
                messages = jsonToMessages(obj.getJSONArray("messages").toString()),
                timestamp = obj.getLong("timestamp"),
                streamState = runCatching {
                    org.claudroide.app.feature.provider.network.StreamState.valueOf(obj.getString("streamState"))
                }.getOrDefault(org.claudroide.app.feature.provider.network.StreamState.IDLE),
            )
        }
    }

    private fun legacySession(prefs: Preferences): SessionRecord? {
        val conversationId = prefs[Keys.CONVERSATION_ID] ?: return null
        val providerId = prefs[Keys.PROVIDER_ID] ?: return null
        val modelId = prefs[Keys.MODEL_ID] ?: return null
        val messages = prefs[Keys.MESSAGES]?.let { runCatching { jsonToMessages(it) }.getOrNull() } ?: return null
        val timestamp = prefs[Keys.TIMESTAMP] ?: return null
        val streamState = runCatching {
            org.claudroide.app.feature.provider.network.StreamState.valueOf(
                prefs[Keys.STREAM_STATE] ?: org.claudroide.app.feature.provider.network.StreamState.IDLE.name,
            )
        }.getOrDefault(org.claudroide.app.feature.provider.network.StreamState.IDLE)
        return SessionRecord(
            conversationId = conversationId,
            providerId = providerId,
            modelId = modelId,
            messages = messages,
            timestamp = timestamp,
            streamState = streamState,
        )
    }

    private fun messagesToJson(messages: List<String>): String =
        JSONArray(messages).toString()

    private fun jsonToMessages(json: String): List<String> {
        val array = JSONArray(json)
        return (0 until array.length()).map { array.getString(it) }
    }

    private companion object {
        const val MAX_STORED_SESSIONS = 25
    }

    private fun agentStateToJson(state: AgentRunState): String {
        val obj = JSONObject()
        obj.put("status", state.status.name)
        obj.put("currentStep", state.currentStep)
        obj.put("completedSteps", JSONArray(state.completedSteps))
        obj.put("failedStep", state.failedStep?.name ?: "")
        obj.put("errorMessage", state.errorMessage ?: "")
        obj.put("totalTokens", state.totalTokens)
        obj.put("estimatedCostUsd", state.estimatedCostUsd)
        return obj.toString()
    }

    private fun jsonToAgentState(json: String): AgentRunState {
        val obj = JSONObject(json)
        return AgentRunState(
            status = AgentRunState.Status.valueOf(obj.getString("status")),
            currentStep = obj.getInt("currentStep"),
            completedSteps = buildList {
                val arr = obj.getJSONArray("completedSteps")
                for (i in 0 until arr.length()) add(arr.getString(i))
            },
            failedStep = obj.optString("failedStep").takeIf { it.isNotBlank() }
                ?.let { AgentRunState.FailedStep.valueOf(it) },
            errorMessage = obj.optString("errorMessage").takeIf { it.isNotBlank() },
            totalTokens = obj.getLong("totalTokens"),
            estimatedCostUsd = obj.getString("estimatedCostUsd")
        )
    }

    private fun toolCallsToJson(calls: List<ToolCallRecord>): String {
        val array = JSONArray()
        calls.forEach { call ->
            val obj = JSONObject()
            obj.put("toolName", call.toolName)
            obj.put("arguments", JSONObject(call.arguments))
            obj.put("result", call.result)
            obj.put("status", call.status.name)
            obj.put("timestamp", call.timestamp)
            array.put(obj)
        }
        return array.toString()
    }

    private fun jsonToToolCalls(json: String): List<ToolCallRecord> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val argsObj = obj.getJSONObject("arguments")
            val argsMap = mutableMapOf<String, String>()
            val names = argsObj.names()
            if (names != null) {
                for (j in 0 until names.length()) {
                    val key = names.getString(j)
                    argsMap[key] = argsObj.getString(key)
                }
            }
            ToolCallRecord(
                toolName = obj.getString("toolName"),
                arguments = argsMap,
                result = obj.getString("result"),
                status = ToolCallRecord.Status.valueOf(obj.getString("status")),
                timestamp = obj.getLong("timestamp")
            )
        }
    }

    private fun recoveryStateToJson(state: RecoveryState): String {
        val obj = JSONObject()
        obj.put("interruptedAtStep", state.interruptedAtStep)
        obj.put("wasExecutingTool", state.wasExecutingTool)
        obj.put("partiallyCompletedFiles", JSONArray(state.partiallyCompletedFiles))
        obj.put("lastKnownGoodStep", state.lastKnownGoodStep)
        obj.put("shouldResume", state.shouldResume)
        return obj.toString()
    }

    private fun jsonToRecoveryState(json: String): RecoveryState {
        val obj = JSONObject(json)
        return RecoveryState(
            interruptedAtStep = obj.getInt("interruptedAtStep"),
            wasExecutingTool = obj.getBoolean("wasExecutingTool"),
            partiallyCompletedFiles = buildList {
                val arr = obj.getJSONArray("partiallyCompletedFiles")
                for (i in 0 until arr.length()) add(arr.getString(i))
            },
            lastKnownGoodStep = obj.getInt("lastKnownGoodStep"),
            shouldResume = obj.getBoolean("shouldResume")
        )
    }
}

internal fun removePromptFieldsFromSessionsJson(json: String): String {
    val sessions = JSONArray(json)
    for (index in 0 until sessions.length()) {
        sessions.getJSONObject(index).apply {
            remove("activePromptTitle")
            remove("systemPrompt")
        }
    }
    return sessions.toString()
}

/** Persistence contract for chat sessions. */
interface SessionPersistence {
    suspend fun save(record: SessionRecord)
    suspend fun lastSession(): SessionRecord?
    suspend fun sessions(limit: Int = 25): List<SessionRecord> = listOfNotNull(lastSession()).take(limit.coerceAtLeast(1))
    suspend fun loadSession(conversationId: String): SessionRecord? =
        sessions().firstOrNull { it.conversationId == conversationId }
}

/** Lightweight local summary used in the recent-conversation list. */
data class RecentSession(
    val conversationId: String,
    val title: String,
    val preview: String,
    val timestamp: Long,
)

/** A persisted session snapshot. */
data class SessionRecord(
    val conversationId: String,
    val providerId: String,
    val modelId: String,
    val messages: List<String>,
    val timestamp: Long,
    val streamState: org.claudroide.app.feature.provider.network.StreamState = org.claudroide.app.feature.provider.network.StreamState.IDLE
)

/** Agent runtime state for recovery. */
data class AgentRunState(
    val status: Status = Status.IDLE,
    val currentStep: Int = 0,
    val completedSteps: List<String> = emptyList(),
    val failedStep: FailedStep? = null,
    val errorMessage: String? = null,
    val totalTokens: Long = 0L,
    val estimatedCostUsd: String = ""
) {
    enum class Status { IDLE, PLANNING, EXECUTING, COMPLETED, FAILED, PAUSED }
    enum class FailedStep {
        CONTEXT_ASSEMBLY, MODEL_REQUEST, TOOL_DETECTION, POLICY_EVALUATION,
        APPROVAL_EVALUATION, TOOL_EXECUTION, RESULT_HANDLING, CONTEXT_UPDATE, COMPLETION
    }
}

/** Tool call record for audit and recovery. */
data class ToolCallRecord(
    val toolName: String,
    val arguments: Map<String, String>,
    val result: String,
    val status: Status,
    val timestamp: Long
) {
    enum class Status { PENDING, EXECUTING, COMPLETED, FAILED, REFUSED, ABORTED }
}

/** Recovery state for interrupted agent runs. */
data class RecoveryState(
    val interruptedAtStep: Int = -1,
    val wasExecutingTool: Boolean = false,
    val partiallyCompletedFiles: List<String> = emptyList(),
    val lastKnownGoodStep: Int = -1,
    val shouldResume: Boolean = false
)

/** Combined recovery data. */
data class AgentRecoveryData(
    val conversationId: String,
    val agentState: AgentRunState?,
    val toolCalls: List<ToolCallRecord>,
    val projectUri: String?,
    val recoveryState: RecoveryState
)
