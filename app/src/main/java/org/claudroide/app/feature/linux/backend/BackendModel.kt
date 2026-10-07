package org.claudroide.app.feature.linux.backend

import org.claudroide.app.feature.linux.device.CapabilityStatus

enum class BackendTier(val label: String, val uiBadge: String? = null) {
    B1_AVF("AVF / Platform VM", null),
    B2_QEMU_TCG("QEMU TCG Emulation", "emulated_degraded"),
    B3_PROOT("PRoot Userspace", null),
    NATIVE("Native Linux (Mode C)", null),
    REMOTE("Remote Network Linux", null)
}

data class BackendCapability(
    val tier: BackendTier,
    val isAvailable: Boolean,
    val guestKernel: Boolean,
    val performanceClass: String, // "near_native", "degraded", "syscall_translation", "full"
    val rootRequired: Boolean,
    val hardwarePassthroughSupported: Boolean,
    val status: CapabilityStatus,
    val limitations: List<String> = emptyList()
)

data class ResourceUsage(
    val cpuPercent: Double = 0.0,
    val memoryUsedMb: Long = 0L,
    val memoryTotalMb: Long = 0L,
    val storageUsedBytes: Long = 0L,
    val ioReadBytes: Long = 0L,
    val ioWriteBytes: Long = 0L
)

data class ConnectionHandle(
    val protocol: String, // "ssh", "vnc", "rdp", "pty"
    val host: String,
    val port: Int,
    val isConnected: Boolean,
    val sessionToken: String? = null
)

data class SnapshotResult(
    val success: Boolean,
    val snapshotId: String? = null,
    val sizeBytes: Long = 0L,
    val errorMessage: String? = null
)
