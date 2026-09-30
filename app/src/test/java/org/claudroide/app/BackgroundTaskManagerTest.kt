package org.claudroide.app

import org.claudroide.app.core.platform.BackgroundTaskManager
import org.claudroide.app.core.platform.TaskLifecycleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackgroundTaskManagerTest {

    @Test
    fun taskLifecycle_startsInRunningState() {
        val taskId = "test-task-1"
        val task = BackgroundTaskManager.startTask(taskId, "Test Task")
        assertEquals(TaskLifecycleState.RUNNING, task.state)
        assertEquals(0f, task.progress, 0.001f)
    }

    @Test
    fun taskLifecycle_canBeCancelledAndCompleted() {
        val taskId = "test-task-2"
        BackgroundTaskManager.startTask(taskId, "Cancellable Task")
        BackgroundTaskManager.cancelTask(taskId)
        val cancelled = BackgroundTaskManager.activeTasks.value[taskId]
        assertNotNull(cancelled)
        assertEquals(TaskLifecycleState.CANCELLED, cancelled?.state)

        val taskId3 = "test-task-3"
        BackgroundTaskManager.startTask(taskId3, "Completable Task")
        BackgroundTaskManager.completeTask(taskId3)
        val completed = BackgroundTaskManager.activeTasks.value[taskId3]
        assertNotNull(completed)
        assertEquals(TaskLifecycleState.COMPLETED, completed?.state)
        assertEquals(1f, completed?.progress ?: 0f, 0.001f)
    }
}
