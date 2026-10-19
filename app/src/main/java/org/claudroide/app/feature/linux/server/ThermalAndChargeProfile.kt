package org.claudroide.app.feature.linux.server

import java.io.File

enum class ThermalStatus {
    NORMAL,
    CAUTION,
    THROTTLED,
    CRITICAL_SHUTDOWN
}

enum class ChargeControlResult {
    SUCCESS,
    UNSUPPORTED,
    PERMISSION_DENIED,
    IO_ERROR
}

data class ThermalAndChargeProfile(
    val chargeLimitPercentage: Int = 80,
    val cautionTempCelsius: Double = 65.0,
    val throttleTempCelsius: Double = 75.0,
    val criticalTempCelsius: Double = 85.0,
    val chargeControlSysfsPath: String = "/sys/class/power_supply/battery/charge_control_limit_max"
) {
    /**
     * Determines thermal status based on current CPU/battery sensor temperature.
     */
    fun evaluateThermalStatus(currentTempCelsius: Double): ThermalStatus {
        return when {
            currentTempCelsius >= criticalTempCelsius -> ThermalStatus.CRITICAL_SHUTDOWN
            currentTempCelsius >= throttleTempCelsius -> ThermalStatus.THROTTLED
            currentTempCelsius >= cautionTempCelsius -> ThermalStatus.CAUTION
            else -> ThermalStatus.NORMAL
        }
    }

    /**
     * Checks if the charge control sysfs node is actually present and writable on this hardware.
     * In unrooted Android or Mode A PRoot, this is typically false (read-only or absent).
     */
    fun isChargeControlSupported(sysfsPath: String = chargeControlSysfsPath): Boolean {
        val file = File(sysfsPath)
        return file.exists() && file.canWrite()
    }

    /**
     * Applies the charge limit percentage to the sysfs node safely, returning an explicit
     * capability status rather than assuming success.
     */
    fun applyChargeLimit(
        targetLimit: Int = chargeLimitPercentage,
        sysfsPath: String = chargeControlSysfsPath
    ): ChargeControlResult {
        val file = File(sysfsPath)
        if (!file.exists()) {
            return ChargeControlResult.UNSUPPORTED
        }
        if (!file.canWrite()) {
            return ChargeControlResult.PERMISSION_DENIED
        }
        return try {
            file.writeText(targetLimit.toString())
            ChargeControlResult.SUCCESS
        } catch (_: Exception) {
            ChargeControlResult.IO_ERROR
        }
    }

    /**
     * Generates shell commands to write charge control threshold into kernel sysfs with guard checks.
     */
    fun generateChargeControlCommand(): String {
        return "if [ -w $chargeControlSysfsPath ]; then echo $chargeLimitPercentage > $chargeControlSysfsPath; fi"
    }
}
