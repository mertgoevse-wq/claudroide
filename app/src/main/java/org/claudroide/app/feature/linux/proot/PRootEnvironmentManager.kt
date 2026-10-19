package org.claudroide.app.feature.linux.proot

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.claudroide.app.feature.linux.distro.DistroCatalog
import org.claudroide.app.feature.linux.distro.provisioning.ProvisioningStatus
import org.claudroide.app.feature.linux.distro.provisioning.RootfsProvisioner
import org.claudroide.app.feature.linux.env.*
import org.claudroide.app.feature.linux.env.migration.EnvironmentMigrationEngine
import org.claudroide.app.feature.linux.env.migration.ImportResult
import org.claudroide.app.feature.linux.proot.bootstrap.RuntimeBootstrapEngine
import org.claudroide.app.feature.linux.proot.bootstrap.RuntimeStatus
import java.io.File
import java.util.UUID

class PRootEnvironmentManager(
    val appFilesDir: File,
    private val persistenceLayer: PersistenceLayer,
    private val processRunner: ProcessRunner = SystemProcessRunner(),
    val runtimeBootstrapEngine: RuntimeBootstrapEngine = RuntimeBootstrapEngine(
        baseDirectory = File(appFilesDir, "runtime/proot"),
        processRunner = processRunner
    ),
    val rootfsProvisioner: RootfsProvisioner = RootfsProvisioner(
        cacheDirectory = File(appFilesDir, "cache/distros")
    )
) : EnvironmentManager {

    constructor(
        context: Context,
        persistenceLayer: PersistenceLayer,
        processRunner: ProcessRunner = SystemProcessRunner()
    ) : this(
        appFilesDir = context.filesDir,
        persistenceLayer = persistenceLayer,
        processRunner = processRunner
    )

    private val environments = mutableMapOf<UUID, LinuxEnvironment>()
    private val activeProcesses = mutableMapOf<UUID, ProcessHandle>()

    init {
        persistenceLayer.loadState().filter { it.mode == EnvironmentMode.MODE_A_PROOT }.forEach {
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
        if (mode != EnvironmentMode.MODE_A_PROOT) {
            throw IllegalArgumentException("PRootEnvironmentManager only supports MODE_A_PROOT")
        }
        val env = LinuxEnvironment(
            name = name,
            distroId = distroId,
            distroVersion = distroVersion,
            rootfsPath = rootfsPath,
            mode = mode,
            state = EnvironmentState.CREATED
        )
        environments[env.id] = env
        persistenceLayer.saveState(environments.values.toList())
        return env
    }

    override fun prepare(envId: UUID): Boolean {
        val env = environments[envId] ?: return false
        val rootfsDir = File(env.rootfsPath)
        return try {
            env.transitionTo(EnvironmentState.PREPARING)

            // 1. Ensure runtime bootstrap is ready
            val runtimeStatus = runBlocking { runtimeBootstrapEngine.getStatus() }
            if (runtimeStatus !is RuntimeStatus.Ready) {
                val bootstrapResult = runBlocking { runtimeBootstrapEngine.bootstrap() }
                if (bootstrapResult !is RuntimeStatus.Ready) {
                    env.state = EnvironmentState.FAILED
                    persistenceLayer.saveState(environments.values.toList())
                    return false
                }
            }

            // 2. Ensure rootfs is provisioned
            if (!rootfsProvisioner.validateRootfs(rootfsDir)) {
                val matchingImage = DistroCatalog.getDistroById(env.distroId)
                if (matchingImage != null) {
                    val provResult = runBlocking {
                        rootfsProvisioner.provision(matchingImage, rootfsDir)
                    }
                    if (provResult !is ProvisioningStatus.Ready) {
                        env.state = EnvironmentState.FAILED
                        persistenceLayer.saveState(environments.values.toList())
                        return false
                    }
                } else {
                    if (!rootfsDir.exists()) rootfsDir.mkdirs()
                }
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
        val rootfsDir = File(env.rootfsPath)

        if (!rootfsDir.exists() || !rootfsDir.isDirectory) {
            env.state = EnvironmentState.FAILED
            persistenceLayer.saveState(environments.values.toList())
            return false
        }

        return try {
            env.transitionTo(EnvironmentState.STARTING)

            val runtimeStatus = runBlocking { runtimeBootstrapEngine.getStatus() }
            val prootBinary = if (runtimeStatus is RuntimeStatus.Ready) {
                runtimeStatus.binaryFile.absolutePath
            } else {
                "proot"
            }

            val config = PRootConfig(
                rootfsPath = rootfsDir.absolutePath,
                prootBinaryPath = prootBinary,
                mounts = PRootConfig.defaultMounts() + listOf(
                    MountBinding(appFilesDir.absolutePath, "/mll-app-files")
                ),
                command = listOf("/bin/sh", "-l")
            )
            val cmd = PRootCommandBuilder.buildCommandLine(config)
            val handle = processRunner.start(cmd, config.envVars, rootfsDir)
            activeProcesses[envId] = handle

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
                    env.transitionTo(EnvironmentState.STOPPING)
                    val handle = activeProcesses.remove(envId)
                    handle?.destroy()
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
        val snapshotsDir = File(appFilesDir, "snapshots/${env.id}")
        val meta = EnvironmentMigrationEngine.createSnapshot(env, snapshotName, snapshotsDir)
        if (meta != null) {
            persistenceLayer.saveState(environments.values.toList())
        }
        return meta
    }

    override fun restoreSnapshot(envId: UUID, snapshotId: String): Boolean {
        val env = environments[envId] ?: return false
        val meta = env.snapshots.firstOrNull { it.id == snapshotId } ?: return false
        if (env.state == EnvironmentState.RUNNING) {
            stop(envId)
        }
        val success = EnvironmentMigrationEngine.restoreSnapshot(env, meta)
        if (success) {
            persistenceLayer.saveState(environments.values.toList())
        }
        return success
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
        val targetEnvsDir = File(appFilesDir, "environments")
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

    override fun destroy(envId: UUID): Boolean {
        val env = environments[envId] ?: return false
        return try {
            if (env.state == EnvironmentState.RUNNING) {
                stop(envId)
            }
            activeProcesses.remove(envId)?.destroy()
            env.transitionTo(EnvironmentState.DESTROYED)
            File(env.rootfsPath).deleteRecursively()
            environments.remove(envId)
            persistenceLayer.saveState(environments.values.toList())
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getRunningProcess(envId: UUID): ProcessHandle? = activeProcesses[envId]

    override fun getEnvironments(): List<LinuxEnvironment> = environments.values.toList()

    override fun getEnvironment(envId: UUID): LinuxEnvironment? = environments[envId]
}
