package org.claudroide.app.feature.chat

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        val AGENT_STATE = stringPreferencesKey("agent_state_json")
        val TOOL_CALLS = stringPreferencesKey("tool_calls_json")
        val PROJECT_URI = stringPreferencesKey("project_uri")
        val RECOVERY_STATE = stringPreferencesKey("recovery_state_json")
    }

    override suspend fun save(record: SessionRecord) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CONVERSATION_ID] = record.conversationId
            prefs[Keys.PROVIDER_ID] = record.providerId
            prefs[Keys.MODEL_ID] = record.modelId
            prefs[Keys.MESSAGES] = messagesToJson(record.messages)
            prefs[Keys.TIMESTAMP] = record.timestamp
            prefs[Keys.STREAM_STATE] = record.streamState.name
        }
    }

    override suspend fun lastSession(): SessionRecord? = context.dataStore.data
        .map { prefs ->
            val conversationId = prefs[Keys.CONVERSATION_ID] ?: return@map null
            val providerId = prefs[Keys.PROVIDER_ID] ?: return@map null
            val modelId = prefs[Keys.MODEL_ID] ?: return@map null
            val messagesJson = prefs[Keys.MESSAGES] ?: return@map null
            val timestamp = prefs[Keys.TIMESTAMP] ?: return@map null
            val streamStateStr = prefs[Keys.STREAM_STATE] ?: org.claudroide.app.feature.provider.network.StreamState.IDLE.name

            val messages = jsonToMessages(messagesJson)
            val streamState = org.claudroide.app.feature.provider.network.StreamState.valueOf(streamStateStr)

            SessionRecord(
                conversationId = conversationId,
                providerId = providerId,
                modelId = modelId,
                messages = messages,
                timestamp = timestamp,
                streamState = streamState
            )
        }
        .first()

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

    // -- JSON serialization --

    private fun messagesToJson(messages: List<String>): String =
        JSONArray(messages).toString()

    private fun jsonToMessages(json: String): List<String> {
        val array = JSONArray(json)
        return (0 until array.length()).map { array.getString(it) }
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

/** Persistence contract for chat sessions. */
interface SessionPersistence {
    suspend fun save(record: SessionRecord)
    suspend fun lastSession(): SessionRecord?
}

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
