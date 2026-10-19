package org.claudroide.app.feature.linux

import android.content.Context
import org.claudroide.app.feature.linux.device.AndroidProbeSource
import org.claudroide.app.feature.linux.device.CapabilityDetector as DeviceDetector
import org.claudroide.app.feature.linux.device.DeviceCapabilityReport as FullDeviceCapabilityReport

object CapabilityDetector {

    fun generateReport(context: Context): DeviceCapabilityReport {
        val fullReport = DeviceDetector.generateReport(AndroidProbeSource(context))
        return DeviceCapabilityReport(
            identity = DeviceIdentity(
                manufacturer = fullReport.manufacturer,
                model = fullReport.model,
                soc = fullReport.cpu.name,
                abi = fullReport.cpu.abi,
                ramGb = fullReport.memory.totalGb.toInt(),
                androidVersion = fullReport.android.versionRelease,
                kernelVersion = fullReport.kernel.release
            ),
            modes = ModeSupport(
                modeA_PRoot = fullReport.modes.modeA_PRoot.name.lowercase(),
                modeB_tier = if (fullReport.modes.modeB1_AVF.name == "VERIFIED") "B1" else "B2",
                modeC_readiness = fullReport.modes.modeC_NativeLinux.name.lowercase()
            ),
            hardware = HardwareFeatures(
                bluetooth = fullReport.connectivity.bluetoothSupported,
                wlan = fullReport.connectivity.wifiSupported,
                cellular = fullReport.connectivity.cellularSupported,
                usbHost = fullReport.connectivity.usbHostSupported
            )
        )
    }

    fun generateFullReport(context: Context): FullDeviceCapabilityReport {
        return DeviceDetector.generateReport(AndroidProbeSource(context))
    }
}
