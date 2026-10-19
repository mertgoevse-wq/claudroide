package org.claudroide.app.feature.linux

/**
 * Data classes for the Device Capability Report per section 18 of mll-spec.md
 */

data class DeviceIdentity(
    val manufacturer: String,
    val model: String,
    val soc: String,
    val abi: String,
    val ramGb: Int,
    val androidVersion: String,
    val kernelVersion: String
)

data class ModeSupport(
    val modeA_PRoot: String, // e.g. "available"
    val modeB_tier: String,  // e.g. "B1", "B2", "unverified"
    val modeC_readiness: String // e.g. "unknown", "unlocked"
)

data class HardwareFeatures(
    val bluetooth: Boolean,
    val wlan: Boolean,
    val cellular: Boolean,
    val usbHost: Boolean
)

data class DeviceCapabilityReport(
    val identity: DeviceIdentity,
    val modes: ModeSupport,
    val hardware: HardwareFeatures
)
