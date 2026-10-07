package org.claudroide.app.feature.linux.distro

object DistroCatalog {

    val UBUNTU_24_04_ARM64 = DistroImage(
        id = "ubuntu-24.04-arm64",
        name = "Ubuntu",
        version = "24.04.5 LTS (Noble Numbat)",
        architecture = "arm64",
        rootfsType = "tar.gz",
        url = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-arm64.tar.gz",
        sha256Checksum = "a91d5a93010193712d346d761372b7c9db6dfcf093893161c64ca107f05914f2",
        packageManager = "apt",
        initSystem = "systemd",
        desktopOptions = listOf("XFCE", "LXQt", "GNOME", "KDE Plasma", "MATE"),
        minimumRamMb = 1024,
        minimumStorageMb = 3072,
        priority = 1,
        supportedBackends = listOf("B1", "B2", "B3"),
        sourceDocumentation = "https://wiki.ubuntu.com/Base",
        releaseDate = "2026-09"
    )

    val ALPINE_3_20_ARM64 = DistroImage(
        id = "alpine-3.20-arm64",
        name = "Alpine Linux",
        version = "3.20.9",
        architecture = "arm64",
        rootfsType = "tar.gz",
        url = "https://dl-cdn.alpinelinux.org/alpine/v3.20/releases/aarch64/alpine-minirootfs-3.20.9-aarch64.tar.gz",
        sha256Checksum = "7e8b4adbf33b1363b90b50e614bbe7bf3d2c8863f9f1718cbc65a883b27c2f43",
        packageManager = "apk",
        initSystem = "openrc",
        desktopOptions = listOf("XFCE", "Sway"),
        minimumRamMb = 256,
        minimumStorageMb = 512,
        priority = 2,
        supportedBackends = listOf("B1", "B2", "B3"),
        sourceDocumentation = "https://alpinelinux.org/downloads/",
        releaseDate = "2026-08"
    )

    val DEBIAN_12_ARM64 = DistroImage(
        id = "debian-12-arm64",
        name = "Debian",
        version = "12 (Bookworm)",
        architecture = "arm64",
        rootfsType = "tar.xz",
        url = "https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-genericcloud-arm64.tar.xz",
        // VERIFIED (RESEARCHED): official https://cloud.debian.org/images/cloud/bookworm/latest/SHA512SUMS
        sha512Checksum = "23628baaf98d6fd519f291e20221ba67663db74f9f01bd7b1a764206f83e37f9144789ab280dd1cf65d7c92c66018851fc8e8c22cfeb589244310a7168f96cf7",
        // No SHA-256 published for this artifact; verification uses sha512Checksum. Placeholder sha256 must never be compared against.
        sha256Checksum = "",
        packageManager = "apt",
        initSystem = "systemd",
        desktopOptions = listOf("XFCE", "LXQt", "GNOME", "MATE"),
        minimumRamMb = 512,
        minimumStorageMb = 2048,
        priority = 2,
        supportedBackends = listOf("B1", "B2", "B3"),
        sourceDocumentation = "https://cloud.debian.org/images/cloud/bookworm/latest/",
        releaseDate = "2026-09"
    )

    val POSTMARKETOS_FAJITA = DistroImage(
        id = "postmarketos-oneplus-fajita",
        name = "postmarketOS",
        version = "v24.06",
        architecture = "arm64",
        rootfsType = "raw.img",
        url = "https://images.postmarketos.org/bpo/v24.06/oneplus-fajita/",
        // Native image: per-release checksum must be read from the release index at
        // flash time. Empty digest = verification intentionally impossible until then.
        sha256Checksum = "",
        packageManager = "apk",
        initSystem = "openrc",
        desktopOptions = listOf("Phosh", "Plasma Mobile", "Sway", "GNOME"),
        minimumRamMb = 2048,
        minimumStorageMb = 8192,
        priority = 3,
        supportedBackends = listOf("NATIVE"),
        supportedDevices = listOf("oneplus-6t"),
        isExperimental = true,
        sourceDocumentation = "https://wiki.postmarketos.org/wiki/OnePlus_6T_(oneplus-fajita)",
        releaseDate = "2026-06"
    )

    fun getAllDistros(): List<DistroImage> {
        return listOf(
            UBUNTU_24_04_ARM64,
            ALPINE_3_20_ARM64,
            DEBIAN_12_ARM64,
            POSTMARKETOS_FAJITA
        )
    }

    fun getDistroById(id: String): DistroImage? {
        return getAllDistros().find { it.id == id || it.id.startsWith(id) }
    }

    fun getAvailableDistros(arch: String): List<DistroImage> {
        val targetArch = if (arch == "aarch64") "arm64" else arch
        return getAllDistros().filter {
            it.architecture == targetArch || it.arch.contains(arch)
        }
    }

    fun getDistrosForDevice(deviceId: String, arch: String): List<DistroImage> {
        val forArch = getAvailableDistros(arch)
        return forArch.filter { image ->
            image.supportedDevices.isEmpty() || image.supportedDevices.contains(deviceId)
        }
    }

    fun getDistrosForBackend(backendTier: String, arch: String): List<DistroImage> {
        val forArch = getAvailableDistros(arch)
        return forArch.filter { image ->
            image.supportedBackends.contains(backendTier)
        }
    }
}
