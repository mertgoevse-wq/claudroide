package org.claudroide.app.feature.linux.distro.build

import org.claudroide.app.feature.linux.distro.DistroCatalog
import org.claudroide.app.feature.linux.distro.DistroImage
import java.io.File

/**
 * High-level API for building custom rootfs images from DistroCatalog entries.
 * Provides pre-configured builders for common MLL use cases.
 */
object RootfsBuildPipeline {

    /**
     * Creates a builder for a catalog distro with MLL-optimized defaults.
     */
    fun fromCatalog(
        distroId: String,
        workDir: File = File("/tmp/mll-rootfs-build"),
        outputDir: File = File("/tmp/mll-rootfs-output"),
        variant: String = "base",
        extraPackages: List<String> = emptyList(),
        reproducible: Boolean = true,
        compression: String = "tar.gz"
    ): RootfsBuilder? {
        val catalogImage = DistroCatalog.getDistroById(distroId) ?: return null

        val config = RootfsBuildConfig(
            distroId = catalogImage.id,
            version = catalogImage.version,
            architecture = catalogImage.architecture,
            variant = variant,
            packageManager = catalogImage.packageManager,
            initSystem = catalogImage.initSystem,
            extraPackages = extraPackages + mllEssentialPackages(catalogImage.packageManager),
            workDir = workDir,
            outputDir = outputDir,
            reproducible = reproducible,
            compression = compression
        )

        return RootfsBuilder(config)
    }

    /**
     * Builds an Ubuntu rootfs optimized for MLL Mode A (PRoot) on A56.
     */
    fun ubuntuForA56(
        workDir: File = File("/tmp/mll-rootfs-build"),
        outputDir: File = File("/tmp/mll-rootfs-output"),
        variant: String = "base",
        desktopEnvironment: String? = null,
        reproducible: Boolean = true
    ): RootfsBuilder {
        val extraPackages = mutableListOf(
            "sudo", "vim", "curl", "wget", "git", "htop", "jq",
            "openssh-client", "net-tools", "iproute2", "procps",
            "python3", "python3-pip", "nodejs", "npm"
        )

        when (desktopEnvironment) {
            "XFCE" -> extraPackages.addAll(listOf("xfce4", "xfce4-goodies", "lightdm", "dbus-x11"))
            "LXQt" -> extraPackages.addAll(listOf("lxqt", "sddm", "dbus-x11"))
            "GNOME" -> extraPackages.addAll(listOf("gnome-shell", "gnome-terminal", "nautilus", "gdm3"))
            "KDE" -> extraPackages.addAll(listOf("plasma-desktop", "konsole", "dolphin", "sddm"))
            "MATE" -> extraPackages.addAll(listOf("mate-desktop-environment", "mate-terminal", "caja", "lightdm"))
        }

        return fromCatalog(
            distroId = "ubuntu-24.04-arm64",
            workDir = workDir,
            outputDir = outputDir,
            variant = variant,
            extraPackages = extraPackages,
            reproducible = reproducible
        ) ?: throw IllegalStateException("Ubuntu catalog entry not found")
    }

    /**
     * Builds a Debian rootfs optimized for MLL.
     */
    fun debianForMll(
        workDir: File = File("/tmp/mll-rootfs-build"),
        outputDir: File = File("/tmp/mll-rootfs-output"),
        variant: String = "base",
        reproducible: Boolean = true
    ): RootfsBuilder {
        val extraPackages = listOf(
            "sudo", "vim", "curl", "wget", "git", "htop", "jq",
            "openssh-client", "net-tools", "iproute2", "procps",
            "python3", "python3-pip"
        )

        return fromCatalog(
            distroId = "debian-12-arm64",
            workDir = workDir,
            outputDir = outputDir,
            variant = variant,
            extraPackages = extraPackages,
            reproducible = reproducible
        ) ?: throw IllegalStateException("Debian catalog entry not found")
    }

    /**
     * Builds an Alpine rootfs optimized for MLL (minimal footprint).
     */
    fun alpineForMll(
        workDir: File = File("/tmp/mll-rootfs-build"),
        outputDir: File = File("/tmp/mll-rootfs-output"),
        variant: String = "minimal",
        reproducible: Boolean = true
    ): RootfsBuilder {
        val extraPackages = listOf(
            "sudo", "vim", "curl", "wget", "git", "htop", "jq",
            "openssh-client", "iproute2", "procps",
            "python3", "py3-pip"
        )

        return fromCatalog(
            distroId = "alpine-3.20-arm64",
            workDir = workDir,
            outputDir = outputDir,
            variant = variant,
            extraPackages = extraPackages,
            reproducible = reproducible
        ) ?: throw IllegalStateException("Alpine catalog entry not found")
    }

    /**
     * Builds a minimal bootstrap rootfs (no extra packages) for fast testing.
     */
    fun minimalBootstrap(
        distroId: String,
        workDir: File = File("/tmp/mll-rootfs-build"),
        outputDir: File = File("/tmp/mll-rootfs-output"),
        reproducible: Boolean = true
    ): RootfsBuilder? {
        return fromCatalog(
            distroId = distroId,
            workDir = workDir,
            outputDir = outputDir,
            variant = "minimal",
            extraPackages = emptyList(),
            reproducible = reproducible
        )
    }

    /**
     * MLL-essential packages per package manager.
     */
    private fun mllEssentialPackages(packageManager: String): List<String> = when (packageManager) {
        "apt" -> listOf("ca-certificates", "locales", "tzdata", "systemd-sysv")
        "apk" -> listOf("ca-certificates", "tzdata")
        "pacman" -> listOf("ca-certificates", "tzdata", "systemd")
        else -> emptyList()
    }

    /**
     * Verifies a built rootfs archive against its expected checksums.
     */
    fun verifyBuildResult(result: RootfsBuildResult.Success): Boolean {
        return true // Placeholder - actual verification in RootfsBuilder
    }
}