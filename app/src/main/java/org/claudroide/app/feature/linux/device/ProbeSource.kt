package org.claudroide.app.feature.linux.device

/**
 * Interface abstracting system probes to ensure testability across
 * real Android devices, PRoot Linux environments, and unit test suites.
 */
interface ProbeSource {
    fun getManufacturer(): String
    fun getModel(): String
    fun getHardware(): String
    fun getSupportedAbis(): List<String>
    fun getAndroidRelease(): String
    fun getSdkInt(): Int
    fun getKernelRelease(): String
    fun getTotalRamBytes(): Long
    fun getAvailableRamBytes(): Long
    fun getInternalStorageTotalBytes(): Long
    fun getInternalStorageAvailableBytes(): Long
    fun hasSystemFeature(featureName: String): Boolean
    fun probeAvfSupport(): Boolean
    fun getBootloaderUnlockedState(): Boolean?
    fun getSlotInfo(): Pair<Boolean, String?> // (isAb, currentSlot)
    fun getBatteryPercent(): Int?
    fun isCharging(): Boolean?
}
