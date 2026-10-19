package org.claudroide.app.feature.linux.backend

import org.claudroide.app.feature.linux.env.EnvironmentState
import org.claudroide.app.feature.linux.env.LinuxEnvironment
import java.io.File

interface LinuxBackend {
    val tier: BackendTier
    val name: String
    val isDegraded: Boolean

    suspend fun discover(): BackendCapability
    suspend fun prepare(env: LinuxEnvironment): Boolean
    suspend fun install(env: LinuxEnvironment, rootfsSource: File, onProgress: (Float) -> Unit): Boolean
    suspend fun start(env: LinuxEnvironment): Boolean
    suspend fun stop(env: LinuxEnvironment): Boolean
    suspend fun restart(env: LinuxEnvironment): Boolean
    suspend fun suspend(env: LinuxEnvironment): Boolean
    suspend fun resume(env: LinuxEnvironment): Boolean
    suspend fun destroy(env: LinuxEnvironment): Boolean
    suspend fun snapshot(env: LinuxEnvironment, snapshotName: String): SnapshotResult
    suspend fun restore(env: LinuxEnvironment, snapshotId: String): Boolean
    suspend fun connect(env: LinuxEnvironment): ConnectionHandle
    suspend fun disconnect(env: LinuxEnvironment): Boolean
    suspend fun status(env: LinuxEnvironment): EnvironmentState
    suspend fun logs(env: LinuxEnvironment, lines: Int): List<String>
    suspend fun resourceUsage(env: LinuxEnvironment): ResourceUsage
}
