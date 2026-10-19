package org.claudroide.app.feature.linux.device

/**
 * Status reflecting the evidence and confidence level for any hardware or software capability.
 * Enforces honesty: unverified capabilities are never claimed as supported.
 */
enum class CapabilityStatus {
    VERIFIED,      // Confirmed via direct on-device hardware/software probe
    OBSERVED,      // Seen active in real-world logs or diagnostics
    RESEARCHED,    // Documented in verified upstream source (kernel/distro/wiki)
    INFERRED,      // Deduced logically from architecture or platform guarantees
    UNKNOWN,       // Not yet probed or discovered; requires investigation
    UNSUPPORTED,   // Explicitly not supported by hardware/kernel/platform
    EXPERIMENTAL,  // Available but degraded, unstable, or work-in-progress
    BLOCKED        // Feature blocked by safety policy or missing prerequisite
}

enum class DeviceRole {
    CONTROLLER,
    LOCAL_HOST,
    LAB_DEVICE,
    NATIVE_LINUX_TARGET,
    HEADLESS_SERVER,
    ROADMAP_CLIENT
}

enum class SafetyState {
    SAFE,
    EXPERIMENTAL,
    LAB,
    FLASH_READY,
    INSTALLED,
    VERIFIED
}

data class CpuInfo(
    val name: String,
    val arch: String,
    val abi: String,
    val coreCount: Int,
    val features: List<String> = emptyList(),
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class GpuInfo(
    val name: String,
    val driver: String,
    val openGlVersion: String? = null,
    val vulkanVersion: String? = null,
    val hardwareAccelerationStatus: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class MemoryInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val totalGb: Double,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class StorageInfo(
    val internalTotalBytes: Long,
    val internalAvailableBytes: Long,
    val appPrivateBytesAvailable: Long,
    val externalStoragePresent: Boolean = false,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class KernelInfo(
    val release: String,
    val version: String,
    val architecture: String,
    val isMainline: Boolean = false,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class AndroidInfo(
    val versionRelease: String,
    val apiLevel: Int,
    val securityPatch: String,
    val buildId: String,
    val knoxVaultPresent: Boolean = false,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class BootloaderInfo(
    val isUnlocked: Boolean? = null,
    val oemUnlockAllowed: Boolean? = null,
    val bootloaderVersion: String? = null,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class SlotInfo(
    val isAbDevice: Boolean,
    val currentSlot: String? = null,      // e.g. "_a" or "_b"
    val slotSuccessful: Boolean? = null,
    val slotUnbootable: Boolean? = null,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class RecoveryInfo(
    val recoveryType: String? = null, // "Lineage Recovery", "TWRP", "Stock", etc.
    val efsBackupStatus: CapabilityStatus = CapabilityStatus.UNKNOWN,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class ConnectivityInfo(
    val wifiSupported: Boolean,
    val wifiHotspotSupported: Boolean,
    val wifiStatus: CapabilityStatus,
    val bluetoothSupported: Boolean,
    val bluetoothPanSupported: Boolean,
    val bluetoothStatus: CapabilityStatus,
    val cellularSupported: Boolean,
    val cellularModemDataSupported: Boolean,
    val cellularStatus: CapabilityStatus,
    val usbHostSupported: Boolean,
    val usbOtgSupported: Boolean,
    val usbStatus: CapabilityStatus
)

data class DisplayInfo(
    val widthPixels: Int,
    val heightPixels: Int,
    val densityDpi: Int,
    val externalDisplaySupported: Boolean = false,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class AudioInfo(
    val outputSupported: Boolean = true,
    val inputSupported: Boolean = true,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class CameraInfo(
    val backCameraSupported: Boolean = false,
    val frontCameraSupported: Boolean = false,
    val mainlineLinuxSupported: Boolean = false,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class SensorsInfo(
    val accelerometer: Boolean = false,
    val gyroscope: Boolean = false,
    val magnetometer: Boolean = false,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class PowerThermalInfo(
    val batteryPresent: Boolean = true,
    val batteryCapacityPercent: Int? = null,
    val isCharging: Boolean? = null,
    val chargeThresholdControlSupported: Boolean = false,
    val currentTempCelsius: Double? = null,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class ModemInfo(
    val imeiKnown: Boolean = false,
    val modemManagerCompatible: Boolean = false,
    val callsSupported: Boolean = false,
    val smsSupported: Boolean = false,
    val dataSupported: Boolean = false,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class VirtualizationInfo(
    val avfAvailable: Boolean = false,
    val pKvmSupported: Boolean = false,
    val qemuTcgSupported: Boolean = true, // Universal software fallback
    val terminalAppDetected: Boolean = false,
    val status: CapabilityStatus = CapabilityStatus.UNKNOWN
)

data class ModeSupportMatrix(
    val modeA_PRoot: CapabilityStatus,
    val modeB1_AVF: CapabilityStatus,
    val modeB2_QEMU: CapabilityStatus,
    val modeC_NativeLinux: CapabilityStatus,
    val recommendedMode: String,
    val reasoningTrace: List<String>
)

/**
 * Root device capability report combining all sub-models per mll-spec.md §18.
 */
data class DeviceCapabilityReport(
    val deviceId: String,
    val marketingName: String,
    val manufacturer: String,
    val model: String,
    val codename: String,
    val safetyState: SafetyState,
    val roles: List<DeviceRole>,
    val cpu: CpuInfo,
    val gpu: GpuInfo,
    val memory: MemoryInfo,
    val storage: StorageInfo,
    val kernel: KernelInfo,
    val android: AndroidInfo,
    val bootloader: BootloaderInfo,
    val slots: SlotInfo,
    val recovery: RecoveryInfo,
    val connectivity: ConnectivityInfo,
    val display: DisplayInfo,
    val audio: AudioInfo,
    val camera: CameraInfo,
    val sensors: SensorsInfo,
    val powerThermal: PowerThermalInfo,
    val modem: ModemInfo,
    val virtualization: VirtualizationInfo,
    val modes: ModeSupportMatrix,
    val generatedAtTimestamp: Long = System.currentTimeMillis()
)
