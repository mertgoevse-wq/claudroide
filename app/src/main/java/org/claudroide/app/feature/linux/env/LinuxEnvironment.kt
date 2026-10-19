package org.claudroide.app.feature.linux.env

import java.util.UUID

data class NetworkConfig(
    val enableNetworking: Boolean = true,
    val portForwarding: Map<Int, Int> = emptyMap(), // hostPort -> guestPort
    val dnsServers: List<String> = listOf("1.1.1.1", "8.8.8.8"),
    val hostname: String = "mll-guest"
)

data class DesktopConfig(
    val desktopEnvironment: String = "XFCE", // e.g. "XFCE", "Phosh", "GNOME"
    val displayWidth: Int = 1920,
    val displayHeight: Int = 1080,
    val displayDpi: Int = 160,
    val vncPort: Int = 5900,
    val rdpPort: Int = 3389
)

data class ServiceConfig(
    val name: String,
    val enabled: Boolean = true,
    val autoStart: Boolean = true
)

data class SnapshotMeta(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val path: String,
    val sizeBytes: Long
)

data class EnvironmentPermissions(
    val allowStorageMounts: Boolean = true,
    val allowAudio: Boolean = true,
    val allowCamera: Boolean = false,
    val allowSensors: Boolean = false
)

data class ResourceLimits(
    val maxRamMb: Int? = null,
    val maxCpuCores: Int? = null,
    val maxDiskBytes: Long? = null
)

data class LinuxEnvironment(
    val id: UUID = UUID.randomUUID(),
    var name: String,
    val deviceId: String = "local",
    val mode: EnvironmentMode,
    val backendTier: String = "B3",
    val distroId: String = "ubuntu",
    val distroVersion: String = "24.04",
    val architecture: String = "arm64",
    val rootfsPath: String,
    var storageAllocatedBytes: Long = 0L,
    var networkConfig: NetworkConfig = NetworkConfig(),
    var desktopConfig: DesktopConfig? = null,
    var services: List<ServiceConfig> = listOf(ServiceConfig("sshd")),
    var state: EnvironmentState = EnvironmentState.CREATED,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val snapshots: MutableList<SnapshotMeta> = mutableListOf(),
    var permissions: EnvironmentPermissions = EnvironmentPermissions(),
    var resources: ResourceLimits = ResourceLimits()
) {
    // Backward-compatible property for existing code
    val distro: String get() = "$distroId $distroVersion"
    val isRunning: Boolean get() = state == EnvironmentState.RUNNING
    val allocatedRamGb: Int? get() = resources.maxRamMb?.let { it / 1024 }

    fun transitionTo(newState: EnvironmentState) {
        state.assertCanTransitionTo(newState)
        state = newState
        updatedAt = System.currentTimeMillis()
    }
}
