package org.claudroide.app.feature.linux.vm

import android.content.Context
import org.claudroide.app.feature.linux.env.*
import org.claudroide.app.feature.linux.env.migration.EnvironmentMigrationEngine
import org.claudroide.app.feature.linux.env.migration.ImportResult
import java.io.File
import java.util.UUID

class VMEnvironmentManager(
    private val context: Context,
    private val persistenceLayer: PersistenceLayer
) : EnvironmentManager {

    private val environments = mutableMapOf<UUID, LinuxEnvironment>()

    init {
        persistenceLayer.loadState()
            .filter { it.mode == EnvironmentMode.MODE_B_VM_AVF || it.mode == EnvironmentMode.MODE_B_VM_QEMU }
            .forEach {
                environments[it.id] = it
            }
    }

    override fun create(
        name: String,
        distroId: String,
        distroVersion: String,
        rootfsPath: String,
        mode: EnvironmentMode
    ): LinuxEnvironment {
        if (mode != EnvironmentMode.MODE_B_VM_AVF && mode != EnvironmentMode.MODE_B_VM_QEMU) {
            throw IllegalArgumentException("VMEnvironmentManager only supports MODE_B_VM_AVF and MODE_B_VM_QEMU")
        }
        val env = LinuxEnvironment(
            name = name,
            distroId = distroId,
            distroVersion = distroVersion,
            rootfsPath = rootfsPath, // disk image path for VMs
            mode = mode,
            backendTier = if (mode == EnvironmentMode.MODE_B_VM_AVF) "B1" else "B2",
            resources = ResourceLimits(maxRamMb = 2048, maxCpuCores = 2),
            state = EnvironmentState.CREATED
        )
        environments[env.id] = env
        persistenceLayer.saveState(environments.values.toList())
        return env
    }

    override fun prepare(envId: UUID): Boolean {
        val env = environments[envId] ?: return false
        return try {
            env.transitionTo(EnvironmentState.PREPARING)
            val diskImage = File(env.rootfsPath)
            if (!diskImage.exists()) {
                diskImage.parentFile?.mkdirs()
                diskImage.createNewFile()
            }
            env.transitionTo(EnvironmentState.INSTALLED)
            persistenceLayer.saveState(environments.values.toList())
            true
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            persistenceLayer.saveState(environments.values.toList())
            false
        }
    }

    override fun start(envId: UUID): Boolean {
        val env = environments[envId] ?: return false
        val diskImage = File(env.rootfsPath)

        if (!diskImage.exists()) {
            env.state = EnvironmentState.FAILED
            persistenceLayer.saveState(environments.values.toList())
            return false
        }

        return try {
            env.transitionTo(EnvironmentState.STARTING)
            if (env.mode == EnvironmentMode.MODE_B_VM_AVF) {
                // pKVM / AVF VirtualMachineManager launch logic
            } else {
                // Mode B2 (QEMU TCG emulated fallback)
                // e.g. qemu-system-aarch64 -M virt -cpu cortex-a57 -m 2048 -hda diskImage ...
            }
            env.transitionTo(EnvironmentState.RUNNING)
            persistenceLayer.saveState(environments.values.toList())
            true
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            persistenceLayer.saveState(environments.values.toList())
            false
        }
    }

    override fun stop(envId: UUID): Boolean {
        val env = environments[envId] ?: return false
        return try {
            when (env.state) {
                EnvironmentState.RUNNING, EnvironmentState.STARTING -> {
                    // ACPI shutdown signal or socket command to VM guest
                    env.transitionTo(EnvironmentState.STOPPING)
                    env.transitionTo(EnvironmentState.STOPPED)
                }
                EnvironmentState.STOPPED -> Unit // already stopped
                else -> env.transitionTo(EnvironmentState.STOPPED)
            }
            persistenceLayer.saveState(environments.values.toList())
            true
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            persistenceLayer.saveState(environments.values.toList())
            false
        }
    }

    override fun restart(envId: UUID): Boolean {
        val env = environments[envId] ?: return false
        if (env.state == EnvironmentState.RUNNING) {
            if (!stop(envId)) return false
        }
        return start(envId)
    }

    override fun snapshot(envId: UUID, snapshotName: String): SnapshotMeta? {
        val env = environments[envId] ?: return null
        val previousState = env.state
        return try {
            env.transitionTo(EnvironmentState.SNAPSHOTTING)
            val snapFile = File("${env.rootfsPath}.$snapshotName.qcow2")
            val meta = SnapshotMeta(
                name = snapshotName,
                path = snapFile.absolutePath,
                sizeBytes = if (snapFile.exists()) snapFile.length() else 0L
            )
            env.snapshots.add(meta)
            env.transitionTo(previousState)
            persistenceLayer.saveState(environments.values.toList())
            meta
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            persistenceLayer.saveState(environments.values.toList())
            null
        }
    }

    override fun restoreSnapshot(envId: UUID, snapshotId: String): Boolean {
        val env = environments[envId] ?: return false
        if (env.snapshots.none { it.id == snapshotId }) return false
        if (env.state == EnvironmentState.RUNNING) {
            stop(envId)
        }
        return try {
            env.transitionTo(EnvironmentState.RESTORING)
            // Restore disk image state from snapshot
            env.transitionTo(EnvironmentState.STOPPED)
            persistenceLayer.saveState(environments.values.toList())
            true
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            persistenceLayer.saveState(environments.values.toList())
            false
        }
    }

    override fun destroy(envId: UUID): Boolean {
        val env = environments[envId] ?: return false
        return try {
            if (env.state == EnvironmentState.RUNNING) {
                stop(envId)
            }
            env.transitionTo(EnvironmentState.DESTROYED)
            File(env.rootfsPath).delete()
            environments.remove(envId)
            persistenceLayer.saveState(environments.values.toList())
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun exportEnvironment(envId: UUID, destinationArchive: File): File? {
        val env = environments[envId] ?: return null
        val wasRunning = env.state == EnvironmentState.RUNNING
        if (wasRunning) {
            stop(envId)
        }
        val result = EnvironmentMigrationEngine.exportEnvironment(env, destinationArchive)
        if (wasRunning) {
            start(envId)
        }
        return result
    }

    override fun importEnvironment(
        archiveFile: File,
        hostArchitecture: String,
        customName: String?
    ): ImportResult {
        val targetEnvsDir = File(context.filesDir, "environments")
        val result = EnvironmentMigrationEngine.importEnvironment(
            archiveFile = archiveFile,
            targetEnvsDir = targetEnvsDir,
            hostArchitecture = hostArchitecture,
            customName = customName
        )
        if (result is ImportResult.Success) {
            environments[result.environment.id] = result.environment
            persistenceLayer.saveState(environments.values.toList())
        }
        return result
    }

    override fun getEnvironments(): List<LinuxEnvironment> = environments.values.toList()

    override fun getEnvironment(envId: UUID): LinuxEnvironment? = environments[envId]
}
