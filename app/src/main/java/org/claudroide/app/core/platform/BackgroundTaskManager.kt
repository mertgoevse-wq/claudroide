package org.claudroide.app.core.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TaskLifecycleState {
    IDLE,
    RUNNING,
    PAUSED,
    CANCELLED,
    FAILED,
    COMPLETED
}

data class BackgroundTask(
    val id: String,
    val title: String,
    val state: TaskLifecycleState = TaskLifecycleState.IDLE,
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val cancellable: Boolean = true
)

object BackgroundTaskManager {
    private val _activeTasks = MutableStateFlow<Map<String, BackgroundTask>>(emptyMap())
    val activeTasks: StateFlow<Map<String, BackgroundTask>> = _activeTasks.asStateFlow()

    fun startTask(id: String, title: String): BackgroundTask {
        val task = BackgroundTask(
            id = id,
            title = title,
            state = TaskLifecycleState.RUNNING,
            progress = 0f
        )
        _activeTasks.update { it + (id to task) }
        return task
    }

    fun updateProgress(id: String, progress: Float) {
        _activeTasks.update { current ->
            val task = current[id] ?: return@update current
            current + (id to task.copy(progress = progress.coerceIn(0f, 1f)))
        }
    }

    fun pauseTask(id: String) {
        _activeTasks.update { current ->
            val task = current[id] ?: return@update current
            current + (id to task.copy(state = TaskLifecycleState.PAUSED))
        }
    }

    fun resumeTask(id: String) {
        _activeTasks.update { current ->
            val task = current[id] ?: return@update current
            current + (id to task.copy(state = TaskLifecycleState.RUNNING))
        }
    }

    fun cancelTask(id: String) {
        _activeTasks.update { current ->
            val task = current[id] ?: return@update current
            current + (id to task.copy(state = TaskLifecycleState.CANCELLED))
        }
    }

    fun completeTask(id: String) {
        _activeTasks.update { current ->
            val task = current[id] ?: return@update current
            current + (id to task.copy(state = TaskLifecycleState.COMPLETED, progress = 1f))
        }
    }

    fun failTask(id: String, reason: String) {
        _activeTasks.update { current ->
            val task = current[id] ?: return@update current
            current + (id to task.copy(state = TaskLifecycleState.FAILED, errorMessage = reason))
        }
    }

    fun clearFinishedTask(id: String) {
        _activeTasks.update { it - id }
    }
}
