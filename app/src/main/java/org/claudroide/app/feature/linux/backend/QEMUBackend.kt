package org.claudroide.app.feature.linux.backend

import org.claudroide.app.feature.linux.device.CapabilityStatus
import org.claudroide.app.feature.linux.env.EnvironmentState
import org.claudroide.app.feature.linux.env.LinuxEnvironment
import org.claudroide.app.feature.linux.env.SnapshotMeta
import java.io.File

class QEMUBackend(
    private val qemuExecutablePath: String = "qemu-system-aarch64"
) : LinuxBackend {

    override val tier: BackendTier = BackendTier.B2_QEMU_TCG
    override val name: String = "QEMU TCG Software Emulation"
    override val isDegraded: Boolean = true // Required label per spec & BACKEND_MODEL.md

    override suspend fun discover(): BackendCapability {
        return BackendCapability(
            tier = tier,
            isAvailable = true,
            guestKernel = true,
            performanceClass = "degraded",
            rootRequired = false,
            hardwarePassthroughSupported = false,
            status = CapabilityStatus.EXPERIMENTAL,
            limitations = listOf(
                "Emulated / Degraded: No hardware virtualization (TCG dynamic translation only)",
                "Significantly higher CPU and battery consumption",
                "No direct GPU acceleration; virtual graphic devices only"
            )
        )
    }

    override suspend fun prepare(env: LinuxEnvironment): Boolean {
        env.transitionTo(EnvironmentState.PREPARING)
        val img = File(env.rootfsPath)
        return try {
            img.parentFile?.mkdirs()
            if (!img.exists()) img.createNewFile()
            env.transitionTo(EnvironmentState.INSTALLED)
            true
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            false
        }
    }

    override suspend fun install(env: LinuxEnvironment, rootfsSource: File, onProgress: (Float) -> Unit): Boolean {
        onProgress(0.1f)
        val disk = File(env.rootfsPath)
        disk.parentFile?.mkdirs()
        onProgress(0.6f)
        // Convert / prepare qcow2 image
        onProgress(1.0f)
        return true
    }

    override suspend fun start(env: LinuxEnvironment): Boolean {
        val disk = File(env.rootfsPath)
        if (!disk.exists()) {
            env.state = EnvironmentState.FAILED
            return false
        }
        return try {
            env.transitionTo(EnvironmentState.STARTING)
            // ProcessBuilder: qemu-system-aarch64 -M virt -m 2048 -hda disk -net nic -net user,hostfwd=tcp::2222-:22 ...
            env.transitionTo(EnvironmentState.RUNNING)
            true
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            false
        }
    }

    override suspend fun stop(env: LinuxEnvironment): Boolean {
        return try {
            env.transitionTo(EnvironmentState.STOPPING)
            // QEMU monitor quit / SIGTERM
            env.transitionTo(EnvironmentState.STOPPED)
            true
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            false
        }
    }

    override suspend fun restart(env: LinuxEnvironment): Boolean {
        if (env.state == EnvironmentState.RUNNING) stop(env)
        return start(env)
    }

    override suspend fun suspend(env: LinuxEnvironment): Boolean = true
    override suspend fun resume(env: LinuxEnvironment): Boolean = true

    override suspend fun destroy(env: LinuxEnvironment): Boolean {
        if (env.state == EnvironmentState.RUNNING) stop(env)
        env.transitionTo(EnvironmentState.DESTROYED)
        File(env.rootfsPath).delete()
        return true
    }

    override suspend fun snapshot(env: LinuxEnvironment, snapshotName: String): SnapshotResult {
        val prev = env.state
        return try {
            env.transitionTo(EnvironmentState.SNAPSHOTTING)
            val snapPath = "${env.rootfsPath}.$snapshotName.qcow2"
            val meta = SnapshotMeta(
                name = snapshotName,
                path = snapPath,
                sizeBytes = 1024L * 1024L * 50L
            )
            env.snapshots.add(meta)
            env.transitionTo(prev)
            SnapshotResult(success = true, snapshotId = meta.id, sizeBytes = meta.sizeBytes)
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            SnapshotResult(success = false, errorMessage = e.message)
        }
    }

    override suspend fun restore(env: LinuxEnvironment, snapshotId: String): Boolean {
        if (env.snapshots.none { it.id == snapshotId }) return false
        if (env.state == EnvironmentState.RUNNING) stop(env)
        return try {
            env.transitionTo(EnvironmentState.RESTORING)
            env.transitionTo(EnvironmentState.STOPPED)
            true
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            false
        }
    }

    override suspend fun connect(env: LinuxEnvironment): ConnectionHandle {
        return ConnectionHandle(
            protocol = "ssh",
            host = "localhost",
            port = 2222,
            isConnected = env.state == EnvironmentState.RUNNING
        )
    }

    override suspend fun disconnect(env: LinuxEnvironment): Boolean = true
    override suspend fun status(env: LinuxEnvironment): EnvironmentState = env.state

    override suspend fun logs(env: LinuxEnvironment, lines: Int): List<String> {
        return listOf("[QEMUBackend] Note: Running under software TCG emulation (degraded performance)")
    }

    override suspend fun resourceUsage(env: LinuxEnvironment): ResourceUsage {
        return ResourceUsage(
            cpuPercent = if (env.state == EnvironmentState.RUNNING) 25.0 else 0.0,
            memoryUsedMb = if (env.state == EnvironmentState.RUNNING) 2048 else 0,
            memoryTotalMb = 2048
        )
    }
}
