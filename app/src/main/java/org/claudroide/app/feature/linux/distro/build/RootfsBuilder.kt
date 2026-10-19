package org.claudroide.app.feature.linux.distro.build

import org.claudroide.app.feature.linux.distro.ChecksumVerifier
import org.claudroide.app.feature.linux.distro.DistroImage
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import kotlin.concurrent.thread

/**
 * Main orchestrator for building reproducible rootfs images.
 * Supports debootstrap (Debian/Ubuntu), apk (Alpine), and pacman (Arch) backends.
 */
class RootfsBuilder(
    private val config: RootfsBuildConfig
) {

    private val startTime = System.currentTimeMillis()
    private val workDir = File(config.workDir, "${config.distroId}-${config.version}-${config.architecture}-${config.variant}")
    private val outputDir = config.outputDir

    init {
        workDir.mkdirs()
        outputDir.mkdirs()
    }

    /**
     * Executes the full build pipeline.
     */
    suspend fun build(): RootfsBuildResult {
        val errors = config.validate()
        if (errors.isNotEmpty()) {
            return RootfsBuildResult.Failure(
                message = "Invalid configuration: ${errors.joinToString(", ")}",
                stage = RootfsBuildResult.BuildStage.INITIALIZATION
            )
        }

        return try {
            when (config.packageManager) {
                "apt" -> buildDebianBased()
                "apk" -> buildAlpineBased()
                "pacman" -> buildArchBased()
                else -> RootfsBuildResult.Failure(
                    message = "Unsupported package manager: ${config.packageManager}",
                    stage = RootfsBuildResult.BuildStage.INITIALIZATION
                )
            }
        } catch (e: Exception) {
            RootfsBuildResult.Failure(
                message = "Build failed: ${e.message}",
                cause = e,
                stage = RootfsBuildResult.BuildStage.UNKNOWN,
                partialArtifacts = listOf(workDir)
            )
        }
    }

    /**
     * Builds Debian/Ubuntu-based rootfs using debootstrap.
     */
    private fun buildDebianBased(): RootfsBuildResult {
        val bootstrapDir = File(workDir, "bootstrap")
        val rootfsDir = File(workDir, "rootfs")

        // Stage 1: Download and extract base system
        val debootstrapUrl = config.customMirrors["debootstrap"] ?: "http://deb.debian.org/debian"
        val suite = when (config.distroId.lowercase()) {
            "ubuntu" -> config.version
            "debian" -> config.version
            else -> config.version
        }

        runCommand("debootstrap", listOf(
            "--arch=${debianArch(config.architecture)}",
            "--variant=${debootstrapVariant(config.variant)}",
            "--include=${(config.extraPackages + listOf("systemd-sysv", "ca-certificates", "locales")).joinToString(",")}",
            "--exclude=${config.excludePackages.joinToString(",")}",
            suite,
            bootstrapDir.absolutePath,
            debootstrapUrl
        ), RootfsBuildResult.BuildStage.BOOTSTRAP_DOWNLOAD)

        // Stage 2: Second-stage debootstrap (run inside chroot via qemu if cross-compiling)
        if (config.architecture != hostArchitecture() && config.qemuBinary != null) {
            copyQemuBinary(bootstrapDir)
        }

        runCommand("debootstrap", listOf("--second-stage"), RootfsBuildResult.BuildStage.BOOTSTRAP_EXTRACT, bootstrapDir)

        // Stage 3: Configure the rootfs
        configureDebianRootfs(bootstrapDir, rootfsDir)

        // Stage 4: Install additional packages
        if (config.extraPackages.isNotEmpty()) {
            installPackagesApt(rootfsDir, config.extraPackages)
        }

        // Stage 5: Finalize and compress
        return finalizeBuild(rootfsDir)
    }

    /**
     * Builds Alpine-based rootfs using apk.
     */
    private fun buildAlpineBased(): RootfsBuildResult {
        val rootfsDir = File(workDir, "rootfs")
        rootfsDir.mkdirs()

        val mirror = config.customMirrors["alpine"] ?: "https://dl-cdn.alpinelinux.org/alpine"
        val repo = "$mirror/v${config.version}/main"

        // Initialize apk database
        runCommand("apk", listOf(
            "--root", rootfsDir.absolutePath,
            "--arch", alpineArch(config.architecture),
            "--allow-untrusted",
            "add", "alpine-base", "alpine-keys"
        ), RootfsBuildResult.BuildStage.BOOTSTRAP_DOWNLOAD)

        // Configure repositories
        File(rootfsDir, "etc/apk/repositories").writeText("$repo\n")

        // Install base packages
        val basePackages = listOf(
            "busybox", "openrc", "ca-certificates", "tzdata"
        ) + config.extraPackages - config.excludePackages

        runCommand("apk", listOf(
            "--root", rootfsDir.absolutePath,
            "--arch", alpineArch(config.architecture),
            "add", *basePackages.toTypedArray()
        ), RootfsBuildResult.BuildStage.PACKAGE_INSTALL)

        // Configure Alpine
        configureAlpineRootfs(rootfsDir)

        return finalizeBuild(rootfsDir)
    }

    /**
     * Builds Arch-based rootfs using pacman.
     */
    private fun buildArchBased(): RootfsBuildResult {
        val rootfsDir = File(workDir, "rootfs")
        rootfsDir.mkdirs()

        val mirror = config.customMirrors["arch"] ?: "https://mirror.archlinux.org"

        // Initialize pacman keyring
        runCommand("pacman-key", listOf("--init"), RootfsBuildResult.BuildStage.INITIALIZATION, rootfsDir)
        runCommand("pacman-key", listOf("--populate", "archlinux"), RootfsBuildResult.BuildStage.INITIALIZATION, rootfsDir)

        // Install base system
        val basePackages = listOf(
            "base", "systemd", "ca-certificates", "pacman", "gnupg"
        ) + config.extraPackages - config.excludePackages

        runCommand("pacstrap", listOf(
            "-c", "-d", "-G", "-M",
            rootfsDir.absolutePath,
            *basePackages.toTypedArray()
        ), RootfsBuildResult.BuildStage.BOOTSTRAP_DOWNLOAD)

        // Configure Arch
        configureArchRootfs(rootfsDir, mirror)

        return finalizeBuild(rootfsDir)
    }

    /**
     * Common finalization: cleanup, compress, verify, generate metadata.
     */
    private fun finalizeBuild(rootfsDir: File): RootfsBuildResult {
        // Cleanup phase
        cleanupRootfs(rootfsDir)

        // Compression phase
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        val archiveName = "${config.distroId}-${config.version}-${config.architecture}-${config.variant}-${timestamp}.${config.compression}"
        val archivePath = File(outputDir, archiveName)

        compressRootfs(rootfsDir, archivePath)

        // Verification phase
        val sha256 = ChecksumVerifier.calculateSha256(archivePath)
        val sha512 = ChecksumVerifier.calculateSha512(archivePath)
        val sizeBytes = archivePath.length()

        // Package manifest
        val packageManifest = collectPackageManifest(rootfsDir)

        // Build metadata
        val durationMs = System.currentTimeMillis() - startTime
        val buildMetadata = collectBuildMetadata(durationMs)

        // Cleanup work dir if not keeping
        if (!config.keepWorkDir) {
            deleteRecursive(workDir)
        }

        return RootfsBuildResult.Success(
            archivePath = archivePath,
            sha256 = sha256,
            sha512 = sha512,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            packageManifest = packageManifest,
            buildMetadata = buildMetadata
        )
    }

    /**
     * Configures Debian/Ubuntu rootfs after debootstrap.
     */
    private fun configureDebianRootfs(bootstrapDir: File, rootfsDir: File) {
        // Move to final location
        if (bootstrapDir != rootfsDir) {
            bootstrapDir.renameTo(rootfsDir)
        }

        // Essential configuration files
        File(rootfsDir, "etc/hostname").writeText("mll-${config.distroId}")
        File(rootfsDir, "etc/hosts").writeText("""127.0.0.1\tlocalhost
127.0.1.1\tmll-${config.distroId}
::1\tlocalhost ip6-localhost ip6-loopback
ff02::1\tip6-allnodes
ff02::2\tip6-allrouters
""")

        // DNS
        File(rootfsDir, "etc/resolv.conf").writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n")

        // APT sources
        val sourcesList = when (config.distroId.lowercase()) {
            "ubuntu" -> """deb http://ports.ubuntu.com/ubuntu-ports/ ${config.version} main restricted universe multiverse
deb http://ports.ubuntu.com/ubuntu-ports/ ${config.version}-updates main restricted universe multiverse
deb http://ports.ubuntu.com/ubuntu-ports/ ${config.version}-security main restricted universe multiverse
"""
            "debian" -> """deb http://deb.debian.org/debian ${config.version} main contrib non-free non-free-firmware
deb http://deb.debian.org/debian ${config.version}-updates main contrib non-free non-free-firmware
deb http://security.debian.org/debian-security ${config.version}-security main contrib non-free non-free-firmware
"""
            else -> ""
        }
        File(rootfsDir, "etc/apt/sources.list").writeText(sourcesList)

        // Locale
        File(rootfsDir, "etc/locale.gen").writeText("en_US.UTF-8 UTF-8\n")
        runCommand("locale-gen", emptyList(), RootfsBuildResult.BuildStage.CONFIGURATION, rootfsDir)

        // fstab
        File(rootfsDir, "etc/fstab").writeText("""# <file system> <mount point> <type> <options> <dump> <pass>
proc\t/proc\tproc\tdefaults\t0\t0
sysfs\t/sys\tsysfs\tdefaults\t0\t0
devpts\t/dev/pts\tdevpts\tdefaults\t0\t0
tmpfs\t/tmp\ttmpfs\tdefaults,nosuid,nodev\t0\t0
""")

        // Disable services that don't make sense in PRoot
        val servicesToMask = listOf("systemd-udevd", "systemd-logind", "getty@tty1")
        servicesToMask.forEach { svc ->
            File(rootfsDir, "etc/systemd/system/${svc}.service").writeText("")
        }
    }

    /**
     * Configures Alpine rootfs.
     */
    private fun configureAlpineRootfs(rootfsDir: File) {
        File(rootfsDir, "etc/hostname").writeText("mll-${config.distroId}")
        File(rootfsDir, "etc/hosts").writeText("127.0.0.1\tlocalhost\n")
        File(rootfsDir, "etc/resolv.conf").writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n")

        // OpenRC init scripts
        File(rootfsDir, "etc/inittab").writeText("""::sysinit:/sbin/openrc sysinit
::sysinit:/sbin/openrc boot
::wait:/sbin/openrc default
tty1::respawn:/sbin/getty 38400 tty1
tty2::respawn:/sbin/getty 38400 tty2
tty3::respawn:/sbin/getty 38400 tty3
tty4::respawn:/sbin/getty 38400 tty4
::ctrlaltdel:/sbin/reboot
::shutdown:/sbin/openrc shutdown
""")
    }

    /**
     * Configures Arch rootfs.
     */
    private fun configureArchRootfs(rootfsDir: File, mirror: String) {
        File(rootfsDir, "etc/hostname").writeText("mll-${config.distroId}")
        File(rootfsDir, "etc/hosts").writeText("127.0.0.1\tlocalhost\n")
        File(rootfsDir, "etc/resolv.conf").writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n")
        File(rootfsDir, "etc/locale.gen").writeText("en_US.UTF-8 UTF-8\n")
        runCommand("locale-gen", emptyList(), RootfsBuildResult.BuildStage.CONFIGURATION, rootfsDir)

        // pacman.conf
        File(rootfsDir, "etc/pacman.conf").writeText("""
[options]
HoldPkg     = pacman glibc
Architecture = auto
SigLevel    = Required DatabaseOptional
LocalFileSigLevel = Optional

[core]
Include = /etc/pacman.d/mirrorlist

[extra]
Include = /etc/pacman.d/mirrorlist
""")

        File(rootfsDir, "etc/pacman.d/mirrorlist").writeText("Server = $mirror/\$arch/\$repo\n")
    }

    /**
     * Installs additional packages via apt in the rootfs.
     */
    private fun installPackagesApt(rootfsDir: File, packages: List<String>) {
        runCommand("apt-get", listOf("update"), RootfsBuildResult.BuildStage.PACKAGE_INSTALL, rootfsDir)
        runCommand("apt-get", listOf("install", "-y", "--no-install-recommends", *packages.toTypedArray()), RootfsBuildResult.BuildStage.PACKAGE_INSTALL, rootfsDir)
        runCommand("apt-get", listOf("clean"), RootfsBuildResult.BuildStage.CLEANUP, rootfsDir)
        File(rootfsDir, "var/lib/apt/lists").deleteRecursively()
    }

    /**
     * Compresses the rootfs directory into an archive.
     */
    private fun compressRootfs(rootfsDir: File, outputPath: File) {
        when (config.compression) {
            "tar.gz" -> compressTarGz(rootfsDir, outputPath)
            "tar.xz" -> compressTarXz(rootfsDir, outputPath)
            "tar.zst" -> compressTarZstd(rootfsDir, outputPath)
        }
    }

    private fun compressTarGz(rootfsDir: File, outputPath: File) {
        // Use tar with gzip for reproducible builds
        val args = mutableListOf(
            "--create",
            "--gzip",
            "--file=${outputPath.absolutePath}",
            "--directory=${rootfsDir.absolutePath}",
            "--sort=name",
            "--owner=0",
            "--group=0",
            "--mtime=@0",
            "--numeric-owner",
            "--pax-option=exthdr.name=%d/PaxHeaders/%f,delete=atime,delete=ctime",
            "."
        )
        runCommand("tar", args, RootfsBuildResult.BuildStage.COMPRESSION)
    }

    private fun compressTarXz(rootfsDir: File, outputPath: File) {
        val args = mutableListOf(
            "--create",
            "--xz",
            "--file=${outputPath.absolutePath}",
            "--directory=${rootfsDir.absolutePath}",
            "--sort=name",
            "--owner=0",
            "--group=0",
            "--mtime=@0",
            "--numeric-owner",
            "."
        )
        runCommand("tar", args, RootfsBuildResult.BuildStage.COMPRESSION)
    }

    private fun compressTarZstd(rootfsDir: File, outputPath: File) {
        val args = mutableListOf(
            "--create",
            "--zstd",
            "--file=${outputPath.absolutePath}",
            "--directory=${rootfsDir.absolutePath}",
            "--sort=name",
            "--owner=0",
            "--group=0",
            "--mtime=@0",
            "--numeric-owner",
            "."
        )
        runCommand("tar", args, RootfsBuildResult.BuildStage.COMPRESSION)
    }

    /**
     * Cleans up unnecessary files from rootfs to reduce size.
     */
    private fun cleanupRootfs(rootfsDir: File) {
        val toRemove = listOf(
            "var/cache/apt/archives",
            "var/lib/apt/lists",
            "var/log/*",
            "tmp/*",
            "root/.cache",
            "usr/share/doc",
            "usr/share/man",
            "usr/share/info",
            "usr/share/locale"
        )

        toRemove.forEach { pattern ->
            File(rootfsDir, pattern).deleteRecursively()
        }

        // Remove empty directories
        removeEmptyDirs(rootfsDir)
    }

    private fun removeEmptyDirs(dir: File) {
        dir.listFiles()?.filter { it.isDirectory }?.forEach { removeEmptyDirs(it) }
        if (dir.listFiles()?.isEmpty() == true) {
            dir.delete()
        }
    }

    /**
     * Collects installed package manifest.
     */
    private fun collectPackageManifest(rootfsDir: File): List<RootfsBuildResult.InstalledPackage> {
        return when (config.packageManager) {
            "apt" -> collectAptManifest(rootfsDir)
            "apk" -> collectApkManifest(rootfsDir)
            "pacman" -> collectPacmanManifest(rootfsDir)
            else -> emptyList()
        }
    }

    private fun collectAptManifest(rootfsDir: File): List<RootfsBuildResult.InstalledPackage> {
        val statusFile = File(rootfsDir, "var/lib/dpkg/status")
        if (!statusFile.exists()) return emptyList()

        val packages = mutableListOf<RootfsBuildResult.InstalledPackage>()
        var currentPkg: String? = null
        var currentVer: String? = null
        var currentSize: Long = 0
        var autoInstalled = false

        statusFile.readText().lines().forEach { line ->
            when {
                line.startsWith("Package: ") -> {
                    currentPkg = line.substringAfter("Package: ").trim()
                }
                line.startsWith("Version: ") -> {
                    currentVer = line.substringAfter("Version: ").trim()
                }
                line.startsWith("Installed-Size: ") -> {
                    currentSize = (line.substringAfter("Installed-Size: ").trim().toLongOrNull() ?: 0) * 1024
                }
                line.startsWith("Auto-Installed: ") -> {
                    autoInstalled = line.substringAfter("Auto-Installed: ").trim() == "1"
                }
                line.isBlank() -> {
                    val pkg = currentPkg
                    val ver = currentVer
                    if (pkg != null && ver != null) {
                        packages.add(RootfsBuildResult.InstalledPackage(pkg, ver, currentSize, autoInstalled))
                        currentPkg = null
                        currentVer = null
                        currentSize = 0
                        autoInstalled = false
                    }
                }
            }
        }
        return packages
    }

    private fun collectApkManifest(rootfsDir: File): List<RootfsBuildResult.InstalledPackage> {
        // apk info -v output parsing
        return emptyList() // Simplified for now
    }

    private fun collectPacmanManifest(rootfsDir: File): List<RootfsBuildResult.InstalledPackage> {
        // pacman -Q output parsing
        return emptyList() // Simplified for now
    }

    /**
     * Collects build metadata for reproducibility.
     */
    private fun collectBuildMetadata(durationMs: Long): RootfsBuildResult.BuildMetadata {
        val toolVersions = mutableMapOf<String, String>()
        try { toolVersions["debootstrap"] = runCommandCapture("debootstrap", listOf("--version")).firstOrNull() ?: "unknown" } catch (_: Exception) {}
        try { toolVersions["apk"] = runCommandCapture("apk", listOf("--version")).firstOrNull() ?: "unknown" } catch (_: Exception) {}
        try { toolVersions["pacman"] = runCommandCapture("pacman", listOf("--version")).firstOrNull() ?: "unknown" } catch (_: Exception) {}
        try { toolVersions["tar"] = runCommandCapture("tar", listOf("--version")).firstOrNull() ?: "unknown" } catch (_: Exception) {}

        return RootfsBuildResult.BuildMetadata(
            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()),
            builderCommit = gitCommitHash(),
            configSnapshot = config.toString(),
            toolVersions = toolVersions,
            relevantEnv = mapOf(
                "PATH" to System.getenv("PATH"),
                "LANG" to System.getenv("LANG"),
                "DEBIAN_FRONTEND" to "noninteractive"
            ),
            hostInfo = RootfsBuildResult.HostInfo(
                os = System.getProperty("os.name"),
                arch = System.getProperty("os.arch"),
                kernel = System.getProperty("os.version"),
                cpuCount = Runtime.getRuntime().availableProcessors(),
                totalMemoryMb = Runtime.getRuntime().maxMemory() / 1024 / 1024
            )
        )
    }

    // Helper functions

    private fun runCommand(command: String, args: List<String>, stage: RootfsBuildResult.BuildStage, chrootDir: File? = null) {
        val cmdList = if (chrootDir != null && command !in setOf("debootstrap", "pacstrap")) {
            mutableListOf("chroot", chrootDir.absolutePath, command) + args
        } else {
            mutableListOf(command) + args
        }

        val pb = java.lang.ProcessBuilder(cmdList).apply {
            directory(workDir)
            redirectErrorStream(true)
            environment()["DEBIAN_FRONTEND"] = "noninteractive"
            environment()["LANG"] = "C.UTF-8"
            environment()["LC_ALL"] = "C.UTF-8"
        }

        val process = pb.start()
        val timeout = config.timeoutSeconds

        val finished = process.waitFor(timeout.toLong(), TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            throw RuntimeException("Command timed out after ${timeout}s: $command ${args.joinToString(" ")}")
        }

        val exitCode = process.exitValue()
        if (exitCode != 0) {
            val output = process.inputStream.readBytes().decodeToString()
            throw RuntimeException("Command failed (exit $exitCode): $command ${args.joinToString(" ")}\n$output")
        }
    }

    private fun runCommandCapture(command: String, args: List<String>): List<String> {
        val pb = java.lang.ProcessBuilder(command, *args.toTypedArray()).apply {
            directory(workDir)
            redirectErrorStream(true)
        }
        val process = pb.start()
        process.waitFor(30, TimeUnit.SECONDS)
        return process.inputStream.bufferedReader().readLines()
    }

    private fun copyQemuBinary(rootfsDir: File) {
        config.qemuBinary?.let { qemu ->
            val targetDir = File(rootfsDir, "usr/bin")
            targetDir.mkdirs()
            Files.copy(File(qemu).toPath(), File(targetDir, "qemu-${config.architecture}-static").toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }

    // Architecture mapping helpers

    private fun debianArch(arch: String): String = when (arch) {
        "arm64", "aarch64" -> "arm64"
        "armv7" -> "armhf"
        "x86_64" -> "amd64"
        else -> arch
    }

    private fun alpineArch(arch: String): String = when (arch) {
        "arm64", "aarch64" -> "aarch64"
        "armv7" -> "armv7"
        "x86_64" -> "x86_64"
        else -> arch
    }

    private fun hostArchitecture(): String = System.getProperty("os.arch").lowercase()

    private fun debootstrapVariant(variant: String): String = when (variant) {
        "minimal" -> "minbase"
        "base" -> "buildd"
        "desktop" -> "buildd"
        "server" -> "buildd"
        else -> "minbase"
    }

    private fun gitCommitHash(): String {
        return try {
            val pb = ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(File("."))
                .redirectErrorStream(true)
            pb.start().inputStream.bufferedReader().readText().trim()
        } catch (_: Exception) {
            "unknown"
        }
    }
}