package org.claudroide.app.feature.linux.env.migration

import org.claudroide.app.feature.linux.env.*
import org.claudroide.app.feature.linux.util.MiniJson
import java.util.UUID

data class EnvironmentManifest(
    val manifestVersion: Int = 1,
    val environmentId: String,
    val name: String,
    val deviceId: String,
    val mode: String,
    val backendTier: String,
    val distroId: String,
    val distroVersion: String,
    val architecture: String,
    val createdAt: Long,
    val exportedAt: Long = System.currentTimeMillis(),
    val rootfsSha256: String = "",
    val networkConfig: NetworkConfig = NetworkConfig(),
    val desktopConfig: DesktopConfig? = null,
    val services: List<ServiceConfig> = listOf(ServiceConfig("sshd")),
    val permissions: EnvironmentPermissions = EnvironmentPermissions(),
    val resources: ResourceLimits = ResourceLimits()
) {

    companion object {
        fun fromEnvironment(env: LinuxEnvironment, rootfsSha256: String = ""): EnvironmentManifest {
            return EnvironmentManifest(
                environmentId = env.id.toString(),
                name = env.name,
                deviceId = env.deviceId,
                mode = env.mode.name,
                backendTier = env.backendTier,
                distroId = env.distroId,
                distroVersion = env.distroVersion,
                architecture = env.architecture,
                createdAt = env.createdAt,
                exportedAt = System.currentTimeMillis(),
                rootfsSha256 = rootfsSha256,
                networkConfig = env.networkConfig,
                desktopConfig = env.desktopConfig,
                services = env.services,
                permissions = env.permissions,
                resources = env.resources
            )
        }

        fun fromJson(json: String): EnvironmentManifest? {
            return try {
                val parsed = MiniJson.parse(json.trim()) as? Map<*, *> ?: return null
                val envId = parsed["environmentId"] as? String ?: return null
                val name = parsed["name"] as? String ?: "imported-env"
                val deviceId = parsed["deviceId"] as? String ?: "local"
                val mode = parsed["mode"] as? String ?: "MODE_A_PROOT"
                val backendTier = parsed["backendTier"] as? String ?: "B3"
                val distroId = parsed["distroId"] as? String ?: "ubuntu"
                val distroVersion = parsed["distroVersion"] as? String ?: "24.04"
                val architecture = parsed["architecture"] as? String ?: "arm64"
                val createdAt = (parsed["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val exportedAt = (parsed["exportedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val rootfsSha256 = parsed["rootfsSha256"] as? String ?: ""

                EnvironmentManifest(
                    manifestVersion = (parsed["manifestVersion"] as? Number)?.toInt() ?: 1,
                    environmentId = envId,
                    name = name,
                    deviceId = deviceId,
                    mode = mode,
                    backendTier = backendTier,
                    distroId = distroId,
                    distroVersion = distroVersion,
                    architecture = architecture,
                    createdAt = createdAt,
                    exportedAt = exportedAt,
                    rootfsSha256 = rootfsSha256,
                    networkConfig = parseNetwork(parsed["networkConfig"]),
                    desktopConfig = parseDesktop(parsed["desktopConfig"]),
                    services = parseServices(parsed["services"]),
                    permissions = parsePermissions(parsed["permissions"]),
                    resources = parseResources(parsed["resources"])
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun parseNetwork(v: Any?): NetworkConfig {
            val m = v as? Map<*, *> ?: return NetworkConfig()
            @Suppress("UNCHECKED_CAST")
            val ports = (m["portForwarding"] as? List<*>)?.mapNotNull { entry ->
                (entry as? String)?.split(":")?.let { parts ->
                    if (parts.size == 2) parts[0].toIntOrNull()?.let { host -> parts[1].toIntOrNull()?.let { guest -> host to guest } } else null
                }
            }?.toMap() ?: emptyMap()
            return NetworkConfig(
                enableNetworking = m["enableNetworking"] as? Boolean ?: true,
                portForwarding = ports,
                dnsServers = (m["dnsServers"] as? List<*>?)?.filterIsInstance<String>() ?: listOf("1.1.1.1", "8.8.8.8"),
                hostname = m["hostname"] as? String ?: "mll-guest"
            )
        }

        private fun parseDesktop(v: Any?): DesktopConfig? {
            val m = v as? Map<*, *> ?: return null
            return DesktopConfig(
                desktopEnvironment = m["desktopEnvironment"] as? String ?: "XFCE",
                displayWidth = (m["displayWidth"] as? Number)?.toInt() ?: 1920,
                displayHeight = (m["displayHeight"] as? Number)?.toInt() ?: 1080,
                displayDpi = (m["displayDpi"] as? Number)?.toInt() ?: 160,
                vncPort = (m["vncPort"] as? Number)?.toInt() ?: 5900,
                rdpPort = (m["rdpPort"] as? Number)?.toInt() ?: 3389
            )
        }

        private fun parseServices(v: Any?): List<ServiceConfig> {
            val list = v as? List<*> ?: return listOf(ServiceConfig("sshd"))
            return list.mapNotNull { s ->
                (s as? Map<*, *>)?.let { m ->
                    (m["name"] as? String)?.let {
                        ServiceConfig(
                            name = it,
                            enabled = m["enabled"] as? Boolean ?: true,
                            autoStart = m["autoStart"] as? Boolean ?: true
                        )
                    }
                }
            }.ifEmpty { listOf(ServiceConfig("sshd")) }
        }

        private fun parsePermissions(v: Any?): EnvironmentPermissions {
            val m = v as? Map<*, *> ?: return EnvironmentPermissions()
            return EnvironmentPermissions(
                allowStorageMounts = m["allowStorageMounts"] as? Boolean ?: true,
                allowAudio = m["allowAudio"] as? Boolean ?: true,
                allowCamera = m["allowCamera"] as? Boolean ?: false,
                allowSensors = m["allowSensors"] as? Boolean ?: false
            )
        }

        private fun parseResources(v: Any?): ResourceLimits {
            val m = v as? Map<*, *> ?: return ResourceLimits()
            return ResourceLimits(
                maxRamMb = (m["maxRamMb"] as? Number)?.toInt(),
                maxCpuCores = (m["maxCpuCores"] as? Number)?.toInt(),
                maxDiskBytes = (m["maxDiskBytes"] as? Number)?.toLong()
            )
        }
    }

    fun toEnvironment(rootfsPath: String, targetDeviceId: String = "local", customName: String? = null): LinuxEnvironment {
        val envMode = try {
            EnvironmentMode.valueOf(mode)
        } catch (_: Exception) {
            EnvironmentMode.MODE_A_PROOT
        }

        return LinuxEnvironment(
            id = UUID.randomUUID(),
            name = customName ?: name,
            deviceId = targetDeviceId,
            mode = envMode,
            backendTier = backendTier,
            distroId = distroId,
            distroVersion = distroVersion,
            architecture = architecture,
            rootfsPath = rootfsPath,
            storageAllocatedBytes = 0L,
            networkConfig = networkConfig,
            desktopConfig = desktopConfig,
            services = services,
            state = EnvironmentState.INSTALLED,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            permissions = permissions,
            resources = resources
        )
    }

    fun toJson(): String {
        val ports = networkConfig.portForwarding.entries.joinToString(",") { "\"${it.key}:${it.value}\"" }
        val dns = networkConfig.dnsServers.joinToString(",") { "\"${escapeJson(it)}\"" }
        val netJson = """{"enableNetworking":${networkConfig.enableNetworking},"portForwarding":[$ports],"dnsServers":[$dns],"hostname":"${escapeJson(networkConfig.hostname)}"}"""

        val dskJson = desktopConfig?.let { d ->
            """{"desktopEnvironment":"${escapeJson(d.desktopEnvironment)}","displayWidth":${d.displayWidth},"displayHeight":${d.displayHeight},"displayDpi":${d.displayDpi},"vncPort":${d.vncPort},"rdpPort":${d.rdpPort}}"""
        } ?: "null"

        val srvJson = services.joinToString(",") {
            """{"name":"${escapeJson(it.name)}","enabled":${it.enabled},"autoStart":${it.autoStart}}"""
        }.let { "[$it]" }

        val permJson = """{"allowStorageMounts":${permissions.allowStorageMounts},"allowAudio":${permissions.allowAudio},"allowCamera":${permissions.allowCamera},"allowSensors":${permissions.allowSensors}}"""

        val ram = resources.maxRamMb?.toString() ?: "null"
        val cores = resources.maxCpuCores?.toString() ?: "null"
        val disk = resources.maxDiskBytes?.toString() ?: "null"
        val resJson = """{"maxRamMb":$ram,"maxCpuCores":$cores,"maxDiskBytes":$disk}"""

        return """
        {
          "manifestVersion": $manifestVersion,
          "environmentId": "${escapeJson(environmentId)}",
          "name": "${escapeJson(name)}",
          "deviceId": "${escapeJson(deviceId)}",
          "mode": "${escapeJson(mode)}",
          "backendTier": "${escapeJson(backendTier)}",
          "distroId": "${escapeJson(distroId)}",
          "distroVersion": "${escapeJson(distroVersion)}",
          "architecture": "${escapeJson(architecture)}",
          "createdAt": $createdAt,
          "exportedAt": $exportedAt,
          "rootfsSha256": "${escapeJson(rootfsSha256)}",
          "networkConfig": $netJson,
          "desktopConfig": $dskJson,
          "services": $srvJson,
          "permissions": $permJson,
          "resources": $resJson
        }
        """.trimIndent()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
