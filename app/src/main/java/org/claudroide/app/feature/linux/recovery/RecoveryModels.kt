package org.claudroide.app.feature.linux.recovery

import java.io.File

/**
 * Connection states for a target device during recovery and inspection workflows.
 */
enum class TargetConnectionState {
    DISCONNECTED,
    UNAUTHORIZED,
    NON_ROOT,
    DEVICE_ONLINE,
    RECOVERY_ONLINE,
    FASTBOOT_ONLINE
}

/**
 * Non-negotiable safety states governing destructive operations.
 * Operations in FLASH-READY require verified EFS backups and explicit human approval.
 */
enum class SafetyState(val label: String, val allowsFlash: Boolean) {
    SAFE("SAFE", false),
    EXPERIMENTAL("EXPERIMENTAL", false),
    LAB("LAB", false),
    FLASH_READY("FLASH-READY", true),
    INSTALLED("INSTALLED", false),
    VERIFIED("VERIFIED", false)
}

/**
 * Detailed device inspection report from a target device.
 */
data class TargetDeviceInfo(
    val deviceId: String = "oneplus-6t",
    val codename: String = "fajita",
    val model: String = "OnePlus 6T (A6013)",
    val serialNumber: String? = null,
    val androidVersion: String = "unknown",
    val currentRom: String = "LineageOS",
    val activeSlot: String? = "a",
    val isRoot: Boolean = true,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = true,
    val connectionState: TargetConnectionState = TargetConnectionState.DEVICE_ONLINE
) {
    val isBatterySafeForRecovery: Boolean
        get() = batteryPercent >= 50 || isCharging
}

/**
 * Discovered partition descriptor on the target block device.
 */
data class TargetPartitionInfo(
    val partitionName: String,
    val blockPath: String,
    val sizeBytes: Long,
    val isEfsCritical: Boolean = false
)

/**
 * Individual preflight check item result.
 */
data class PreflightCheckItem(
    val name: String,
    val passed: Boolean,
    val message: String,
    val isCritical: Boolean = true
)

/**
 * Preflight check report evaluating whether the target environment is ready
 * for staged backup or recovery operations.
 */
data class PreflightReport(
    val timestamp: Long = System.currentTimeMillis(),
    val targetDevice: TargetDeviceInfo?,
    val items: List<PreflightCheckItem>,
    val partitionsFound: List<TargetPartitionInfo>
) {
    val isPassed: Boolean
        get() = items.all { !it.isCritical || it.passed }

    val failureReasons: List<String>
        get() = items.filter { it.isCritical && !it.passed }.map { "${it.name}: ${it.message}" }
}

/**
 * Progress feedback during staged backup operations.
 */
data class BackupProgress(
    val phase: String,
    val currentPartition: String? = null,
    val bytesDumped: Long = 0L,
    val totalBytesEstimated: Long = 0L,
    val partitionsCompleted: Int = 0,
    val totalPartitions: Int = 0,
    val message: String = ""
)

/**
 * Comprehensive result of a backup or dry-run execution.
 */
data class BackupResult(
    val success: Boolean,
    val isDryRun: Boolean,
    val manifest: BackupManifest?,
    val backupDirectory: File?,
    val error: String? = null,
    val durationMs: Long = 0L,
    val logs: List<String> = emptyList()
)

/**
 * Explicit human approval token required for FLASH-READY operations.
 */
data class HumanApprovalToken(
    val approvedBy: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionName: String,
    val targetDeviceId: String = "oneplus-6t",
    val acknowledgedRisk: Boolean = true
)
