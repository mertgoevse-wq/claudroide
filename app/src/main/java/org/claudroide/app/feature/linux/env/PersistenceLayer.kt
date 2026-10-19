package org.claudroide.app.feature.linux.env

import android.content.Context
import org.claudroide.app.feature.linux.util.MiniJson
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Handles persistence of Linux environments to disk.
 * Uses atomic writes (write to .tmp then rename) to prevent state corruption.
 *
 * Round-trips ALL LinuxEnvironment fields: network/desktop/service/permission/
 * resource configuration and snapshot metadata are persisted alongside the core
 * fields, so restoring a session loses no configuration.
 */
class PersistenceLayer(private val storageDir: File) {

    constructor(context: Context) : this(context.filesDir)

    private val storeFile: File
        get() = File(storageDir, "environments.json")

    @Synchronized
    fun saveState(envs: List<LinuxEnvironment>) {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val tmpFile = File(storageDir, "environments.json.tmp")
        val json = serializeEnvironments(envs)

        FileOutputStream(tmpFile).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(json)
                writer.flush()
            }
            try {
                fos.fd.sync() // Flush to storage where the FS supports it
            } catch (e: java.io.SyncFailedException) {
                // Overlayfs/FUSE-backed storage (common on Android emulated storage)
                // may reject fsync. The write itself succeeded; the atomic rename
                // below still guarantees a crash-consistent file.
            }
        }

        // Atomic file replace
        if (!tmpFile.renameTo(storeFile)) {
            // Fallback for filesystems where renameTo fails across existing files
            storeFile.delete()
            if (!tmpFile.renameTo(storeFile)) {
                // Copy fallback
                tmpFile.copyTo(storeFile, overwrite = true)
                tmpFile.delete()
            }
        }
    }

    @Synchronized
    fun loadState(): List<LinuxEnvironment> {
        if (!storeFile.exists() || storeFile.length() == 0L) {
            return emptyList()
        }
        return try {
            val content = storeFile.readText(StandardCharsets.UTF_8)
            deserializeEnvironments(content)
        } catch (e: Exception) {
            System.err.println("Warning: failed to parse environments.json: ${e.message}")
            emptyList()
        }
    }

    // ------------------------------------------------------------------
    // Serialization
    // ------------------------------------------------------------------

    private fun serializeEnvironments(envs: List<LinuxEnvironment>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        envs.forEachIndexed { index, env ->
            sb.append("  {\n")
            sb.append("    \"id\": \"${env.id}\",\n")
            sb.append("    \"name\": \"${escapeJson(env.name)}\",\n")
            sb.append("    \"deviceId\": \"${escapeJson(env.deviceId)}\",\n")
            sb.append("    \"mode\": \"${env.mode.name}\",\n")
            sb.append("    \"backendTier\": \"${escapeJson(env.backendTier)}\",\n")
            sb.append("    \"distroId\": \"${escapeJson(env.distroId)}\",\n")
            sb.append("    \"distroVersion\": \"${escapeJson(env.distroVersion)}\",\n")
            sb.append("    \"architecture\": \"${escapeJson(env.architecture)}\",\n")
            sb.append("    \"rootfsPath\": \"${escapeJson(env.rootfsPath)}\",\n")
            sb.append("    \"storageAllocatedBytes\": ${env.storageAllocatedBytes},\n")
            sb.append("    \"state\": \"${env.state.name}\",\n")
            sb.append("    \"createdAt\": ${env.createdAt},\n")
            sb.append("    \"updatedAt\": ${env.updatedAt},\n")
            sb.append("    \"networkConfig\": ${serializeNetwork(env.networkConfig)},\n")
            sb.append("    \"desktopConfig\": ${env.desktopConfig?.let { serializeDesktop(it) } ?: "null"},\n")
            sb.append("    \"services\": ${serializeServices(env.services)},\n")
            sb.append("    \"snapshots\": ${serializeSnapshots(env.snapshots)},\n")
            sb.append("    \"permissions\": ${serializePermissions(env.permissions)},\n")
            sb.append("    \"resources\": ${serializeResources(env.resources)}\n")
            sb.append("  }")
            if (index < envs.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]\n")
        return sb.toString()
    }

    private fun serializeNetwork(n: NetworkConfig): String {
        val ports = n.portForwarding.entries.joinToString(",") { "\"${it.key}:${it.value}\"" }
        val dns = n.dnsServers.joinToString(",") { "\"${escapeJson(it)}\"" }
        return """{"enableNetworking":${n.enableNetworking},"portForwarding":[$ports],"dnsServers":[$dns],"hostname":"${escapeJson(n.hostname)}"}"""
    }

    private fun serializeDesktop(d: DesktopConfig): String {
        return """{"desktopEnvironment":"${escapeJson(d.desktopEnvironment)}","displayWidth":${d.displayWidth},""" +
            """"displayHeight":${d.displayHeight},"displayDpi":${d.displayDpi},"vncPort":${d.vncPort},"rdpPort":${d.rdpPort}}"""
    }

    private fun serializeServices(services: List<ServiceConfig>): String {
        val items = services.joinToString(",") {
            """{"name":"${escapeJson(it.name)}","enabled":${it.enabled},"autoStart":${it.autoStart}}"""
        }
        return "[$items]"
    }

    private fun serializeSnapshots(snapshots: List<SnapshotMeta>): String {
        val items = snapshots.joinToString(",") {
            """{"id":"${it.id}","name":"${escapeJson(it.name)}","createdAt":${it.createdAt},""" +
                """"path":"${escapeJson(it.path)}","sizeBytes":${it.sizeBytes}}"""
        }
        return "[$items]"
    }

    private fun serializePermissions(p: EnvironmentPermissions): String {
        return """{"allowStorageMounts":${p.allowStorageMounts},"allowAudio":${p.allowAudio},""" +
            """"allowCamera":${p.allowCamera},"allowSensors":${p.allowSensors}}"""
    }

    private fun serializeResources(r: ResourceLimits): String {
        val ram = r.maxRamMb?.toString() ?: "null"
        val cores = r.maxCpuCores?.toString() ?: "null"
        val disk = r.maxDiskBytes?.toString() ?: "null"
        return """{"maxRamMb":$ram,"maxCpuCores":$cores,"maxDiskBytes":$disk}"""
    }

    // ------------------------------------------------------------------
    // Deserialization
    // ------------------------------------------------------------------

    private fun deserializeEnvironments(json: String): List<LinuxEnvironment> {
        val list = mutableListOf<LinuxEnvironment>()
        val trimmed = json.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return emptyList()
        }

        val parsed = MiniJson.parse(trimmed) as? List<*> ?: return emptyList()
        for (item in parsed) {
            val map = item as? Map<*, *> ?: continue
            try {
                val idStr = map["id"] as? String ?: continue
                val modeStr = map["mode"] as? String ?: "MODE_A_PROOT"
                val mode = try { EnvironmentMode.valueOf(modeStr) } catch (e: Exception) { EnvironmentMode.MODE_A_PROOT }
                val stateStr = map["state"] as? String ?: "CREATED"
                val state = try { EnvironmentState.valueOf(stateStr) } catch (e: Exception) { EnvironmentState.CREATED }

                val env = LinuxEnvironment(
                    id = UUID.fromString(idStr),
                    name = map["name"] as? String ?: "unnamed",
                    deviceId = map["deviceId"] as? String ?: "local",
                    mode = mode,
                    backendTier = map["backendTier"] as? String ?: "B3",
                    distroId = map["distroId"] as? String ?: "ubuntu",
                    distroVersion = map["distroVersion"] as? String ?: "24.04",
                    architecture = map["architecture"] as? String ?: "arm64",
                    rootfsPath = map["rootfsPath"] as? String ?: "",
                    storageAllocatedBytes = (map["storageAllocatedBytes"] as? Number)?.toLong() ?: 0L,
                    state = state,
                    createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    networkConfig = parseNetwork(map["networkConfig"]),
                    desktopConfig = parseDesktop(map["desktopConfig"]),
                    services = parseServices(map["services"]),
                    snapshots = parseSnapshots(map["snapshots"]),
                    permissions = parsePermissions(map["permissions"]),
                    resources = parseResources(map["resources"])
                )
                list.add(env)
            } catch (e: Exception) {
                // Skip malformed entries
            }
        }
        return list
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

    private fun parseSnapshots(v: Any?): MutableList<SnapshotMeta> {
        val list = v as? List<*> ?: return mutableListOf()
        return list.mapNotNull { s ->
            (s as? Map<*, *>)?.let { m ->
                (m["id"] as? String)?.let {
                    SnapshotMeta(
                        id = it,
                        name = m["name"] as? String ?: "",
                        createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0L,
                        path = m["path"] as? String ?: "",
                        sizeBytes = (m["sizeBytes"] as? Number)?.toLong() ?: 0L
                    )
                }
            }
        }.toMutableList()
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

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
