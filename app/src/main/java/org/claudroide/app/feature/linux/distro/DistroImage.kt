package org.claudroide.app.feature.linux.distro

data class DistroImage(
    val id: String,
    val name: String,
    val version: String,
    val architecture: String, // "arm64", "aarch64"
    val rootfsType: String,   // "tar.gz", "tar.xz", "raw.img"
    val url: String,
    val sha256Checksum: String,
    val sha512Checksum: String? = null,
    val signatureUrl: String? = null,
    val packageManager: String, // "apt", "apk", "pacman"
    val initSystem: String,     // "systemd", "openrc", "none"
    val desktopOptions: List<String> = emptyList(), // "XFCE", "GNOME", "Phosh"
    val minimumRamMb: Int = 1024,
    val minimumStorageMb: Int = 2048,
    val priority: Int = 1,
    val supportedBackends: List<String> = listOf("B1", "B2", "B3"),
    val supportedDevices: List<String> = emptyList(), // empty = universal for arch
    val isExperimental: Boolean = false,
    val sourceDocumentation: String = "",
    val releaseDate: String = ""
) {
    val downloadUrl: String get() = url
    // Backward-compatibility properties
    val category: String get() = if (desktopOptions.isNotEmpty()) "desktop" else "minimal"
    val arch: List<String> get() = listOf(architecture, if (architecture == "arm64") "aarch64" else "arm64")
    val sha256CheckSum: String get() = sha256Checksum
}
