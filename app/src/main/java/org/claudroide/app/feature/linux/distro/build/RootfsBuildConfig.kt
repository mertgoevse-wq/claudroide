package org.claudroide.app.feature.linux.distro.build

import java.io.File

/**
 * Configuration for building a custom rootfs image.
 */
data class RootfsBuildConfig(
    /** Target distribution identifier (e.g., "ubuntu", "debian", "alpine") */
    val distroId: String,

    /** Distribution version (e.g., "24.04", "12", "3.20") */
    val version: String,

    /** Target architecture: "arm64", "aarch64", "armv7", "x86_64" */
    val architecture: String = "arm64",

    /** Build variant: "minimal", "base", "desktop", "server" */
    val variant: String = "base",

    /** Package manager to bootstrap: "apt", "apk", "pacman" */
    val packageManager: String = "apt",

    /** Init system: "systemd", "openrc", "none" */
    val initSystem: String = "systemd",

    /** Additional packages to install during build */
    val extraPackages: List<String> = emptyList(),

    /** Packages to explicitly exclude (e.g., for size optimization) */
    val excludePackages: List<String> = emptyList(),

    /** Working directory for build artifacts */
    val workDir: File = File("/tmp/mll-rootfs-build"),

    /** Output directory for final image */
    val outputDir: File = File("/tmp/mll-rootfs-output"),

    /** Enable reproducible builds (fixed timestamps, sorted files) */
    val reproducible: Boolean = true,

    /** Compression format for output: "tar.gz", "tar.xz", "tar.zst" */
    val compression: String = "tar.gz",

    /** Build timeout in seconds */
    val timeoutSeconds: Int = 1800,

    /** Whether to keep intermediate build artifacts for debugging */
    val keepWorkDir: Boolean = false,

    /** Custom apt/apk repository mirrors (for offline/speed) */
    val customMirrors: Map<String, String> = emptyMap(),

    /** Architecture-specific QEMU user-mode binary path (for cross-build) */
    val qemuBinary: String? = null
) {
    /** Validates the configuration. */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (distroId.isBlank()) errors.add("distroId cannot be blank")
        if (version.isBlank()) errors.add("version cannot be blank")
        val validArchitectures = setOf("arm64", "aarch64", "armv7", "x86_64")
        if (architecture !in validArchitectures) {
            errors.add("Unsupported architecture: $architecture")
        }
        val validVariants = setOf("minimal", "base", "desktop", "server")
        if (variant !in validVariants) {
            errors.add("Unsupported variant: $variant")
        }
        val validPackageManagers = setOf("apt", "apk", "pacman")
        if (packageManager !in validPackageManagers) {
            errors.add("Unsupported package manager: $packageManager")
        }
        val validInitSystems = setOf("systemd", "openrc", "none")
        if (initSystem !in validInitSystems) {
            errors.add("Unsupported init system: $initSystem")
        }
        val validCompressions = setOf("tar.gz", "tar.xz", "tar.zst")
        if (compression !in validCompressions) {
            errors.add("Unsupported compression: $compression")
        }
        if (timeoutSeconds <= 0) errors.add("timeoutSeconds must be positive")
        return errors
    }
}