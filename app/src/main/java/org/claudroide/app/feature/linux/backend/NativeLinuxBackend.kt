package org.claudroide.app.feature.linux.backend

import org.claudroide.app.feature.linux.device.CapabilityStatus
import org.claudroide.app.feature.linux.env.EnvironmentState
import org.claudroide.app.feature.linux.env.LinuxEnvironment
import java.io.File

class NativeLinuxBackend(
    val isTargetDeviceAvailable: Boolean = false,
    val isSlotDualBootSupported: Boolean = true
) : LinuxBackend {

    override val tier: BackendTier = BackendTier.NATIVE
    override val name: String = "Native Linux Engine (Mode C)"
    override val isDegraded: Boolean = false

    override suspend fun discover(): BackendCapability {
        return BackendCapability(
            tier = tier,
            isAvailable = isTargetDeviceAvailable,
            guestKernel = true,
            performanceClass = "full",
            rootRequired = true,
            hardwarePassthroughSupported = true,
            status = if (isTargetDeviceAvailable) CapabilityStatus.RESEARCHED else CapabilityStatus.BLOCKED,
            limitations = listOf(
                "Requires unlocked bootloader on target device",
                "Requires Stage 1 EFS backup verified before flashing",
                "Non-destructive A/B slot dual-boot setup required before dedicated conversion",
                "Camera ISP unsupported on mainline SDM845"
            )
        )
    }

    override suspend fun prepare(env: LinuxEnvironment): Boolean {
        if (!isTargetDeviceAvailable) {
            env.state = EnvironmentState.FAILED
            return false
        }
        env.transitionTo(EnvironmentState.PREPARING)
        env.transitionTo(EnvironmentState.INSTALLED)
        return true
    }

    override suspend fun install(env: LinuxEnvironment, rootfsSource: File, onProgress: (Float) -> Unit): Boolean {
        // SAFETY GATE ENFORCEMENT:
        // No flashing or partition writing is permitted without explicit human approval flag
        throw SecurityException(
            "SAFETY GATE: Native Linux flashing/partition operations require explicit human approval and verified offline EFS backup."
        )
    }

    override suspend fun start(env: LinuxEnvironment): Boolean {
        if (!isTargetDeviceAvailable) return false
        env.transitionTo(EnvironmentState.STARTING)
        // Command device to reboot into native Linux slot (e.g. bootctl set-active-boot-slot 1 && reboot)
        env.transitionTo(EnvironmentState.RUNNING)
        return true
    }

    override suspend fun stop(env: LinuxEnvironment): Boolean {
        if (!isTargetDeviceAvailable) return false
        env.transitionTo(EnvironmentState.STOPPING)
        // Reboot back into Android slot
        env.transitionTo(EnvironmentState.STOPPED)
        return true
    }

    override suspend fun restart(env: LinuxEnvironment): Boolean {
        stop(env)
        return start(env)
    }

    override suspend fun suspend(env: LinuxEnvironment): Boolean = false
    override suspend fun resume(env: LinuxEnvironment): Boolean = false

    override suspend fun destroy(env: LinuxEnvironment): Boolean {
        throw SecurityException(
            "SAFETY GATE: Native partition destruction requires explicit human approval."
        )
    }

    override suspend fun snapshot(env: LinuxEnvironment, snapshotName: String): SnapshotResult {
        // Native snapshots rely on Btrfs/ZFS/LVM on the native rootfs
        return SnapshotResult(success = false, errorMessage = "Native filesystem snapshots require active booted Linux session")
    }

    override suspend fun restore(env: LinuxEnvironment, snapshotId: String): Boolean = false

    override suspend fun connect(env: LinuxEnvironment): ConnectionHandle {
        return ConnectionHandle(
            protocol = "ssh",
            host = "192.168.43.10", // Typical hotspot or USB IP for fajita
            port = 22,
            isConnected = env.state == EnvironmentState.RUNNING
        )
    }

    override suspend fun disconnect(env: LinuxEnvironment): Boolean = true
    override suspend fun status(env: LinuxEnvironment): EnvironmentState = env.state

    override suspend fun logs(env: LinuxEnvironment, lines: Int): List<String> {
        return listOf("[NativeLinuxBackend] Target device slot state: inactive/waiting")
    }

    override suspend fun resourceUsage(env: LinuxEnvironment): ResourceUsage {
        return ResourceUsage(
            cpuPercent = 0.0,
            memoryUsedMb = 0,
            memoryTotalMb = 8192
        )
    }
}
