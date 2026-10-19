package org.claudroide.app.feature.linux.distro.build

import java.io.File

/**
 * Result of a rootfs build operation.
 */
sealed interface RootfsBuildResult {
    data class Success(
        /** Path to the built rootfs archive */
        val archivePath: File,
        /** SHA-256 checksum of the archive */
        val sha256: String,
        /** SHA-512 checksum of the archive (if computed) */
        val sha512: String?,
        /** Size of the archive in bytes */
        val sizeBytes: Long,
        /** Build duration in milliseconds */
        val durationMs: Long,
        /** Manifest of installed packages */
        val packageManifest: List<InstalledPackage>,
        /** Build metadata for reproducibility */
        val buildMetadata: BuildMetadata
    ) : RootfsBuildResult

    data class Failure(
        /** Human-readable error message */
        val message: String,
        /** Optional exception cause */
        val cause: Throwable? = null,
        /** Build stage where failure occurred */
        val stage: BuildStage = BuildStage.UNKNOWN,
        /** Partial artifacts that may be useful for debugging */
        val partialArtifacts: List<File> = emptyList()
    ) : RootfsBuildResult

    /**
     * Installed package information.
     */
    data class InstalledPackage(
        val name: String,
        val version: String,
        val sizeBytes: Long,
        val isAutoInstalled: Boolean = false
    )

    /**
     * Build metadata for reproducibility and audit trail.
     */
    data class BuildMetadata(
        /** Build timestamp (ISO 8601, UTC) */
        val timestamp: String,
        /** Git commit hash of builder code */
        val builderCommit: String,
        /** Exact command line / config used */
        val configSnapshot: String,
        /** Tool versions used (debootstrap, apk, etc.) */
        val toolVersions: Map<String, String>,
        /** Environment variables that affect build */
        val relevantEnv: Map<String, String>,
        /** Host system info */
        val hostInfo: HostInfo
    )

    /**
     * Host system information.
     */
    data class HostInfo(
        val os: String,
        val arch: String,
        val kernel: String,
        val cpuCount: Int,
        val totalMemoryMb: Long
    )

    /**
     * Build stages for failure attribution.
     */
    enum class BuildStage {
        UNKNOWN,
        INITIALIZATION,
        BOOTSTRAP_DOWNLOAD,
        BOOTSTRAP_EXTRACT,
        PACKAGE_INSTALL,
        CONFIGURATION,
        CLEANUP,
        COMPRESSION,
        VERIFICATION,
        FINALIZATION
    }
}