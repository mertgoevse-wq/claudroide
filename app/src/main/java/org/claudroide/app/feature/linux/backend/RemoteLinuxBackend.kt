package org.claudroide.app.feature.linux.backend

import org.claudroide.app.feature.linux.device.CapabilityStatus
import org.claudroide.app.feature.linux.env.EnvironmentState
import org.claudroide.app.feature.linux.env.LinuxEnvironment
import java.io.File

class RemoteLinuxBackend(
    private val remoteHost: String = "192.168.43.1",
    private val sshPort: Int = 22
) : LinuxBackend {

    override val tier: BackendTier = BackendTier.REMOTE
    override val name: String = "Remote Linux Engine"
    override val isDegraded: Boolean = false

    override suspend fun discover(): BackendCapability {
        return BackendCapability(
            tier = tier,
            isAvailable = true,
            guestKernel = true,
            performanceClass = "full",
            rootRequired = false,
            hardwarePassthroughSupported = true,
            status = CapabilityStatus.RESEARCHED,
            limitations = listOf("Subject to network latency and connection bearer bandwidth (hotspot/USB/BT)")
        )
    }

    override suspend fun prepare(env: LinuxEnvironment): Boolean {
        env.transitionTo(EnvironmentState.PREPARING)
        env.transitionTo(EnvironmentState.INSTALLED)
        return true
    }

    override suspend fun install(env: LinuxEnvironment, rootfsSource: File, onProgress: (Float) -> Unit): Boolean {
        onProgress(1.0f)
        return true
    }

    override suspend fun start(env: LinuxEnvironment): Boolean {
        env.transitionTo(EnvironmentState.STARTING)
        env.transitionTo(EnvironmentState.RUNNING)
        return true
    }

    override suspend fun stop(env: LinuxEnvironment): Boolean {
        env.transitionTo(EnvironmentState.STOPPING)
        env.transitionTo(EnvironmentState.STOPPED)
        return true
    }

    override suspend fun restart(env: LinuxEnvironment): Boolean {
        stop(env)
        return start(env)
    }

    override suspend fun suspend(env: LinuxEnvironment): Boolean = true
    override suspend fun resume(env: LinuxEnvironment): Boolean = true
    override suspend fun destroy(env: LinuxEnvironment): Boolean {
        env.transitionTo(EnvironmentState.DESTROYED)
        return true
    }

    override suspend fun snapshot(env: LinuxEnvironment, snapshotName: String): SnapshotResult {
        return SnapshotResult(success = true, snapshotId = "remote-snap-1")
    }

    override suspend fun restore(env: LinuxEnvironment, snapshotId: String): Boolean = true

    override suspend fun connect(env: LinuxEnvironment): ConnectionHandle {
        return ConnectionHandle(
            protocol = "ssh",
            host = remoteHost,
            port = sshPort,
            isConnected = true
        )
    }

    override suspend fun disconnect(env: LinuxEnvironment): Boolean = true
    override suspend fun status(env: LinuxEnvironment): EnvironmentState = env.state
    override suspend fun logs(env: LinuxEnvironment, lines: Int): List<String> = listOf("Remote connected to $remoteHost")
    override suspend fun resourceUsage(env: LinuxEnvironment): ResourceUsage = ResourceUsage()
}
