package org.claudroide.app.feature.linux.distro.provisioning

import org.claudroide.app.feature.linux.distro.ChecksumVerifier
import org.claudroide.app.feature.linux.distro.DistroDownloader
import org.claudroide.app.feature.linux.distro.DistroImage
import org.claudroide.app.feature.linux.distro.ResilientDistroDownloader
import org.claudroide.app.feature.linux.distro.TarExtractor
import java.io.File
import java.io.FileInputStream

/**
 * Status events emitted during rootfs distribution provisioning.
 */
sealed class ProvisioningStatus {
    object Idle : ProvisioningStatus()
    data class Downloading(val progress: Float) : ProvisioningStatus()
    object Verifying : ProvisioningStatus()
    data class Unpacking(val bytesExtracted: Long) : ProvisioningStatus()
    object Initializing : ProvisioningStatus()
    data class Ready(val rootfsDir: File, val distroId: String, val version: String) : ProvisioningStatus()
    data class Failed(val error: String) : ProvisioningStatus()
}

/**
 * Autonomous engine for downloading, verifying, unpacking, validating, and initializing
 * real Linux root filesystems for PRoot execution.
 */
class RootfsProvisioner(
    val cacheDirectory: File,
    private val downloader: DistroDownloader = ResilientDistroDownloader()
) {

    suspend fun provision(
        image: DistroImage,
        destinationRootfsDir: File,
        onStatus: (ProvisioningStatus) -> Unit = {}
    ): ProvisioningStatus {
        destinationRootfsDir.mkdirs()
        cacheDirectory.mkdirs()

        // 1. Check if already provisioned and validated
        val marker = File(destinationRootfsDir, ".mll_provisioned.json")
        if (marker.exists() && validateRootfs(destinationRootfsDir)) {
            val ready = ProvisioningStatus.Ready(destinationRootfsDir, image.id, image.version)
            onStatus(ready)
            return ready
        }

        // Determine local archive cache path
        val archiveExtension = when {
            image.url.endsWith(".tar.gz") -> ".tar.gz"
            image.url.endsWith(".tgz") -> ".tar.gz"
            image.url.endsWith(".tar.xz") -> ".tar.xz"
            image.url.endsWith(".tar") -> ".tar"
            else -> ".tar.gz"
        }
        val cachedArchive = File(cacheDirectory, "${image.id}-${image.version}-${image.architecture}$archiveExtension")

        try {
            // 2. Download phase
            onStatus(ProvisioningStatus.Downloading(0.0f))
            val downloaded = downloader.download(image, cachedArchive) { progress ->
                onStatus(ProvisioningStatus.Downloading(progress))
            }

            if (!downloaded || !cachedArchive.exists()) {
                val failure = ProvisioningStatus.Failed("Rootfs download failed for ${image.name}")
                onStatus(failure)
                return failure
            }

            // 3. Verification phase
            onStatus(ProvisioningStatus.Verifying)
            val isValidChecksum = ChecksumVerifier.verifyDistroImage(cachedArchive, image)
            if (!isValidChecksum) {
                cachedArchive.delete()
                val failure = ProvisioningStatus.Failed("Integrity verification failed: checksum mismatch for ${image.name}")
                onStatus(failure)
                return failure
            }

            // 4. Extraction phase
            onStatus(ProvisioningStatus.Unpacking(0L))
            val isGzip = archiveExtension == ".tar.gz" || archiveExtension == ".tgz"

            FileInputStream(cachedArchive).use { fis ->
                TarExtractor.extract(fis, destinationRootfsDir, isGzip = isGzip) { bytes ->
                    onStatus(ProvisioningStatus.Unpacking(bytes))
                }
            }

            // 5. Rootfs validation phase
            if (!validateRootfs(destinationRootfsDir)) {
                destinationRootfsDir.deleteRecursively()
                val failure = ProvisioningStatus.Failed(
                    "Rootfs validation failed: missing essential binary /bin/sh in ${destinationRootfsDir.absolutePath}"
                )
                onStatus(failure)
                return failure
            }

            // 6. Minimal Linux initialization
            onStatus(ProvisioningStatus.Initializing)
            initializeLinuxUserland(destinationRootfsDir)

            // 7. Write persistent provisioning marker
            marker.writeText(
                """
                {
                    "distro": "${image.id}",
                    "version": "${image.version}",
                    "architecture": "${image.architecture}",
                    "provisionedAt": ${System.currentTimeMillis()}
                }
                """.trimIndent()
            )

            val ready = ProvisioningStatus.Ready(destinationRootfsDir, image.id, image.version)
            onStatus(ready)
            return ready
        } catch (e: Exception) {
            val failure = ProvisioningStatus.Failed("Provisioning failed: ${e.message}")
            onStatus(failure)
            return failure
        }
    }

    /**
     * Confirms that an extracted directory contains a valid minimal Linux rootfs structure.
     */
    fun validateRootfs(rootfsDir: File): Boolean {
        if (!rootfsDir.exists() || !rootfsDir.isDirectory) return false

        // Check for shell
        val binSh = File(rootfsDir, "bin/sh")
        val usrBinSh = File(rootfsDir, "usr/bin/sh")
        val busybox = File(rootfsDir, "bin/busybox")
        val hasShell = binSh.exists() || usrBinSh.exists() || busybox.exists()

        // Check for /etc directory
        val etcDir = File(rootfsDir, "etc")
        val hasEtc = etcDir.exists() && etcDir.isDirectory

        return hasShell && hasEtc
    }

    private fun initializeLinuxUserland(rootfsDir: File) {
        val etcDir = File(rootfsDir, "etc")
        etcDir.mkdirs()

        // Setup resolv.conf if not present or empty
        val resolvConf = File(etcDir, "resolv.conf")
        if (!resolvConf.exists() || resolvConf.length() == 0L) {
            resolvConf.writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n")
        }

        // Setup hosts if not present
        val hosts = File(etcDir, "hosts")
        if (!hosts.exists() || hosts.length() == 0L) {
            hosts.writeText("127.0.0.1 localhost\n::1 localhost ip6-localhost ip6-loopback\n")
        }

        // Ensure essential pseudo-directories exist for bind mounts
        File(rootfsDir, "dev").mkdirs()
        File(rootfsDir, "proc").mkdirs()
        File(rootfsDir, "sys").mkdirs()
        File(rootfsDir, "tmp").apply {
            mkdirs()
            setWritable(true, false)
            setReadable(true, false)
            setExecutable(true, false)
        }
    }
}
