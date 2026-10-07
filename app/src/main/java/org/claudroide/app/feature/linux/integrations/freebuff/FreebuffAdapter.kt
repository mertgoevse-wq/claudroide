package org.claudroide.app.feature.linux.integrations.freebuff

import org.claudroide.app.feature.linux.DeviceCapabilityReport
import org.claudroide.app.feature.linux.env.LinuxEnvironment

/**
 * FreebuffAdapter provides an optional, isolated bridge between Mobile Linux Lab (core)
 * and the external Freebuff tooling ecosystem.
 *
 * MLL Core maintains zero hard dependencies on Freebuff. This adapter can be used
 * by optional downstream consumers without coupling core runtime components.
 */
interface FreebuffAdapter {
    val integrationName: String get() = "freebuff"
    val isAvailable: Boolean

    fun exportEnvironmentState(environments: List<LinuxEnvironment>): Map<String, Any>
    fun exportDeviceCapabilities(report: DeviceCapabilityReport): Map<String, Any>
}

/**
 * Default decoupled implementation that exports standard state maps without requiring external libraries.
 */
class DefaultFreebuffAdapter(
    override val isAvailable: Boolean = false
) : FreebuffAdapter {

    override fun exportEnvironmentState(environments: List<LinuxEnvironment>): Map<String, Any> {
        return mapOf(
            "adapter" to integrationName,
            "version" to "1.0.0",
            "environmentCount" to environments.size,
            "environments" to environments.map { env ->
                mapOf(
                    "id" to env.id.toString(),
                    "name" to env.name,
                    "distro" to env.distroId,
                    "state" to env.state.name,
                    "mode" to env.mode.name
                )
            }
        )
    }

    override fun exportDeviceCapabilities(report: DeviceCapabilityReport): Map<String, Any> {
        return mapOf(
            "adapter" to integrationName,
            "device" to report.identity.model,
            "soc" to report.identity.soc,
            "abi" to report.identity.abi,
            "modeA" to report.modes.modeA_PRoot,
            "modeB" to report.modes.modeB_tier,
            "modeC" to report.modes.modeC_readiness,
            "usbHost" to report.hardware.usbHost
        )
    }
}
