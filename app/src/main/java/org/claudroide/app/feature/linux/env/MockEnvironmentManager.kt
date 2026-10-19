package org.claudroide.app.feature.linux.env

import org.claudroide.app.feature.linux.env.migration.ImportResult
import java.io.File
import java.util.UUID

class MockEnvironmentManager(private val persistenceLayer: PersistenceLayer) : EnvironmentManager {
    private val environments = mutableMapOf<UUID, LinuxEnvironment>()

    init {
        persistenceLayer.loadState().forEach {
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
        return try {
            env.transitionTo(EnvironmentState.PREPARING)
            // Mock preparation work (unpacking / checking directories)
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
        return try {
            env.transitionTo(EnvironmentState.STARTING)
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
                    env.transitionTo(EnvironmentState.STOPPED)
                }
                EnvironmentState.STOPPED -> Unit // already stopped
                EnvironmentState.INSTALLED, EnvironmentState.FAILED -> env.transitionTo(EnvironmentState.STOPPED)
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
            val meta = SnapshotMeta(
                name = snapshotName,
                path = "${env.rootfsPath}/snapshots/$snapshotName.tar.gz",
                sizeBytes = 1024L * 1024L * 50L
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
            // Mock restoration logic
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
            environments.remove(envId)
            persistenceLayer.saveState(environments.values.toList())
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun exportEnvironment(envId: UUID, destinationArchive: File): File? {
        val env = environments[envId] ?: return null
        destinationArchive.parentFile?.mkdirs()
        destinationArchive.writeText("mock archive for ${env.name}")
        return destinationArchive
    }

    override fun importEnvironment(
        archiveFile: File,
        hostArchitecture: String,
        customName: String?
    ): ImportResult {
        val name = customName ?: "mock-imported"
        val env = LinuxEnvironment(
            name = name,
            architecture = hostArchitecture,
            mode = EnvironmentMode.MODE_A_PROOT,
            rootfsPath = "/mock/$name",
            state = EnvironmentState.INSTALLED
        )
        environments[env.id] = env
        persistenceLayer.saveState(environments.values.toList())
        return ImportResult.Success(env)
    }

    override fun getEnvironments(): List<LinuxEnvironment> = environments.values.toList()

    override fun getEnvironment(envId: UUID): LinuxEnvironment? = environments[envId]
}
