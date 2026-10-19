package org.claudroide.app.feature.linux.env

enum class EnvironmentState {
    CREATED,
    PREPARING,
    INSTALLED,
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    FAILED,
    SNAPSHOTTING,
    RESTORING,
    DESTROYED;

    fun canTransitionTo(target: EnvironmentState): Boolean {
        if (this == target) return true
        if (this == DESTROYED) return false // Terminal state

        return when (this) {
            CREATED -> target in listOf(PREPARING, FAILED, DESTROYED)
            PREPARING -> target in listOf(INSTALLED, FAILED, DESTROYED)
            INSTALLED -> target in listOf(STARTING, STOPPED, SNAPSHOTTING, DESTROYED)
            STOPPED -> target in listOf(STARTING, SNAPSHOTTING, RESTORING, DESTROYED, PREPARING)
            STARTING -> target in listOf(RUNNING, FAILED, STOPPING)
            RUNNING -> target in listOf(STOPPING, SNAPSHOTTING, FAILED)
            STOPPING -> target in listOf(STOPPED, FAILED)
            SNAPSHOTTING -> target in listOf(RUNNING, STOPPED, INSTALLED, FAILED)
            RESTORING -> target in listOf(STOPPED, FAILED)
            FAILED -> target in listOf(STOPPED, PREPARING, DESTROYED)
            DESTROYED -> false
        }
    }

    fun assertCanTransitionTo(target: EnvironmentState) {
        if (!canTransitionTo(target)) {
            throw IllegalStateException("Invalid environment state transition from $this to $target")
        }
    }
}
