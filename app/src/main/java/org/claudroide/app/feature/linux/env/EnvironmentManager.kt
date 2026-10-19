package org.claudroide.app.feature.linux.env

import org.claudroide.app.feature.linux.env.migration.ImportResult
import java.io.File
import java.util.UUID

interface EnvironmentManager {
    fun create(
        name: String,
        distroId: String = "ubuntu",
        distroVersion: String = "24.04",
        rootfsPath: String,
        mode: EnvironmentMode
    ): LinuxEnvironment

    fun prepare(envId: UUID): Boolean
    fun start(envId: UUID): Boolean
    fun stop(envId: UUID): Boolean
    fun restart(envId: UUID): Boolean
    fun destroy(envId: UUID): Boolean
    fun snapshot(envId: UUID, snapshotName: String): SnapshotMeta?
    fun restoreSnapshot(envId: UUID, snapshotId: String): Boolean
    fun exportEnvironment(envId: UUID, destinationArchive: File): File?
    fun importEnvironment(
        archiveFile: File,
        hostArchitecture: String = "arm64",
        customName: String? = null
    ): ImportResult
    fun getEnvironments(): List<LinuxEnvironment>
    fun getEnvironment(envId: UUID): LinuxEnvironment?
}
