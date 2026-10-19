package org.claudroide.app.feature.linux.backend

import org.claudroide.app.feature.linux.device.CapabilityStatus
import org.claudroide.app.feature.linux.device.DeviceCapabilityReport
import org.claudroide.app.feature.linux.env.EnvironmentMode

object BackendRegistry {

    fun getAvailableBackends(report: DeviceCapabilityReport): List<LinuxBackend> {
        val list = mutableListOf<LinuxBackend>()

        // PRoot is always available as baseline
        list.add(PRootBackend())

        // AVF if supported
        val avfAvailable = report.modes.modeB1_AVF == CapabilityStatus.VERIFIED
        list.add(AVFBackend(isAvfAvailableOnHost = avfAvailable))

        // QEMU TCG software emulation fallback
        list.add(QEMUBackend())

        // Native Linux (Mode C)
        val nativeAvailable = report.modes.modeC_NativeLinux == CapabilityStatus.VERIFIED
        list.add(NativeLinuxBackend(isTargetDeviceAvailable = nativeAvailable))

        // Remote
        list.add(RemoteLinuxBackend())

        return list
    }

    fun getHighestRecommendedBackend(report: DeviceCapabilityReport): LinuxBackend {
        return when (report.modes.recommendedMode) {
            "MODE_C_NATIVE" -> NativeLinuxBackend(isTargetDeviceAvailable = true)
            "MODE_B_AVF" -> AVFBackend(isAvfAvailableOnHost = true)
            "MODE_B_QEMU" -> QEMUBackend()
            else -> PRootBackend()
        }
    }

    fun getBackendForMode(mode: EnvironmentMode, report: DeviceCapabilityReport): LinuxBackend {
        return when (mode) {
            EnvironmentMode.MODE_A_PROOT -> PRootBackend()
            EnvironmentMode.MODE_B_VM_AVF -> AVFBackend(isAvfAvailableOnHost = report.modes.modeB1_AVF == CapabilityStatus.VERIFIED)
            EnvironmentMode.MODE_B_VM_QEMU -> QEMUBackend()
            EnvironmentMode.MODE_C_NATIVE -> NativeLinuxBackend(isTargetDeviceAvailable = report.modes.modeC_NativeLinux == CapabilityStatus.VERIFIED)
        }
    }
}
