package org.claudroide.app.feature.linux.env.migration

import org.claudroide.app.feature.linux.distro.TarArchiver
import org.claudroide.app.feature.linux.distro.TarExtractor
import org.claudroide.app.feature.linux.env.EnvironmentState
import org.claudroide.app.feature.linux.env.LinuxEnvironment
import org.claudroide.app.feature.linux.env.SnapshotMeta
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

sealed class ImportResult {
    data class Success(val environment: LinuxEnvironment) : ImportResult()
    data class IncompatibleArchitecture(val sourceArch: String, val hostArch: String, val reason: String) : ImportResult()
    data class CorruptedArchive(val reason: String) : ImportResult()
}

/**
 * Core engine for environment snapshot, restore, export, and cross-device import/migration.
 */
object EnvironmentMigrationEngine {

    const val MANIFEST_FILENAME = "mll-manifest.json"

    /**
     * Creates an atomic point-in-time snapshot archive of [env]'s rootfs.
     */
    fun createSnapshot(
        env: LinuxEnvironment,
        snapshotName: String,
        snapshotsDir: File
    ): SnapshotMeta? {
        val rootfsDir = File(env.rootfsPath)
        if (!rootfsDir.exists() || !rootfsDir.isDirectory) return null

        val previousState = env.state
        return try {
            env.transitionTo(EnvironmentState.SNAPSHOTTING)
            snapshotsDir.mkdirs()

            val sanitizedName = snapshotName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val snapFile = File(snapshotsDir, "$sanitizedName.tar.gz")

            val bytesArchived = TarArchiver.archive(
                sourceDir = rootfsDir,
                destinationArchive = snapFile,
                isGzip = true,
                excludes = TarArchiver.DEFAULT_ROOTFS_EXCLUDES
            )

            val meta = SnapshotMeta(
                name = snapshotName,
                createdAt = System.currentTimeMillis(),
                path = snapFile.absolutePath,
                sizeBytes = if (snapFile.exists()) snapFile.length() else bytesArchived
            )

            env.snapshots.add(meta)
            env.transitionTo(if (previousState == EnvironmentState.RUNNING) EnvironmentState.RUNNING else EnvironmentState.STOPPED)
            meta
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            null
        }
    }

    /**
     * Restores an environment rootfs from [snapshotMeta].
     */
    fun restoreSnapshot(env: LinuxEnvironment, snapshotMeta: SnapshotMeta): Boolean {
        val snapFile = File(snapshotMeta.path)
        if (!snapFile.exists()) return false

        val rootfsDir = File(env.rootfsPath)
        return try {
            env.transitionTo(EnvironmentState.RESTORING)

            // Clean current rootfs directory, preserving snapshots directory if nested
            if (rootfsDir.exists()) {
                rootfsDir.listFiles()?.forEach { file ->
                    if (file.name != "snapshots") {
                        file.deleteRecursively()
                    }
                }
            } else {
                rootfsDir.mkdirs()
            }

            // Extract snapshot archive
            FileInputStream(snapFile).use { fis ->
                TarExtractor.extract(
                    inputStream = fis,
                    destinationDir = rootfsDir,
                    isGzip = true
                )
            }

            env.transitionTo(EnvironmentState.STOPPED)
            true
        } catch (e: Exception) {
            env.state = EnvironmentState.FAILED
            false
        }
    }

    /**
     * Packages [env] into a portable archive containing `mll-manifest.json` and `rootfs/`.
     */
    fun exportEnvironment(
        env: LinuxEnvironment,
        destinationArchive: File,
        onProgress: ((Long) -> Unit)? = null
    ): File? {
        val rootfsDir = File(env.rootfsPath)
        if (!rootfsDir.exists() || !rootfsDir.isDirectory) return null

        destinationArchive.parentFile?.mkdirs()

        return try {
            val manifest = EnvironmentManifest.fromEnvironment(env)
            val manifestJson = manifest.toJson()
            val manifestBytes = manifestJson.toByteArray(Charsets.UTF_8)
            var totalBytesWritten = 0L

            FileOutputStream(destinationArchive).use { fos ->
                GZIPOutputStream(fos).use { gzOut ->
                    // 1. Write manifest entry
                    val manifestHeader = ByteArray(512)
                    buildUstarHeader(
                        header = manifestHeader,
                        name = MANIFEST_FILENAME,
                        mode = 0b110100100L, // 0644
                        size = manifestBytes.size.toLong(),
                        mtime = System.currentTimeMillis() / 1000L,
                        typeFlag = '0',
                        linkName = ""
                    )
                    gzOut.write(manifestHeader)
                    gzOut.write(manifestBytes)
                    totalBytesWritten += 512 + manifestBytes.size
                    onProgress?.invoke(totalBytesWritten)

                    val manifestRemainder = (manifestBytes.size % 512)
                    if (manifestRemainder > 0) {
                        val pad = 512 - manifestRemainder
                        gzOut.write(ByteArray(pad))
                        totalBytesWritten += pad
                    }

                    // 2. Stream rootfs files with "rootfs/" prefix
                    val files = mutableListOf<File>()
                    collectFiles(rootfsDir, rootfsDir, TarArchiver.DEFAULT_ROOTFS_EXCLUDES, files)

                    for (file in files) {
                        val relPath = file.relativeTo(rootfsDir).path.replace('\\', '/')
                        if (relPath.isEmpty()) continue

                        val entryName = "rootfs/$relPath"
                        val isDir = file.isDirectory
                        val isSymlink = java.nio.file.Files.isSymbolicLink(file.toPath())
                        val formattedName = if (isDir && !entryName.endsWith("/")) "$entryName/" else entryName

                        val linkTarget = if (isSymlink) {
                            java.nio.file.Files.readSymbolicLink(file.toPath()).toString()
                        } else {
                            ""
                        }

                        val fileSize = if (isDir || isSymlink) 0L else file.length()
                        val mode = when {
                            isDir -> 0b111101101L // 0755
                            isSymlink -> 0b111111111L // 0777
                            file.canExecute() -> 0b111101101L // 0755
                            else -> 0b110100100L // 0644
                        }

                        val header = ByteArray(512)
                        buildUstarHeader(
                            header = header,
                            name = formattedName,
                            mode = mode,
                            size = fileSize,
                            mtime = file.lastModified() / 1000L,
                            typeFlag = when {
                                isSymlink -> '2'
                                isDir -> '5'
                                else -> '0'
                            },
                            linkName = linkTarget
                        )
                        gzOut.write(header)
                        totalBytesWritten += 512

                        if (!isDir && !isSymlink && fileSize > 0) {
                            FileInputStream(file).use { fis ->
                                val buffer = ByteArray(4096)
                                var read: Int
                                while (fis.read(buffer).also { read = it } != -1) {
                                    gzOut.write(buffer, 0, read)
                                    totalBytesWritten += read
                                    onProgress?.invoke(totalBytesWritten)
                                }
                            }
                            val remainder = (fileSize % 512).toInt()
                            if (remainder > 0) {
                                val pad = 512 - remainder
                                gzOut.write(ByteArray(pad))
                                totalBytesWritten += pad
                            }
                        }
                    }

                    // 3. Write EOF blocks
                    gzOut.write(ByteArray(1024))
                    totalBytesWritten += 1024
                    onProgress?.invoke(totalBytesWritten)
                }
            }
            destinationArchive
        } catch (_: Exception) {
            destinationArchive.delete()
            null
        }
    }

    /**
     * Imports a portable `.mll.tar.gz` archive, verifying architecture compatibility before extracting.
     */
    fun importEnvironment(
        archiveFile: File,
        targetEnvsDir: File,
        hostArchitecture: String,
        customName: String? = null
    ): ImportResult {
        if (!archiveFile.exists() || archiveFile.length() == 0L) {
            return ImportResult.CorruptedArchive("Archive file does not exist or is empty")
        }

        // 1. Extract and parse manifest
        val manifest = extractManifest(archiveFile)
            ?: return ImportResult.CorruptedArchive("Could not locate or parse valid $MANIFEST_FILENAME in archive")

        // 2. Verify architecture compatibility
        val compat = ArchCompatibility.check(manifest.architecture, hostArchitecture)
        if (compat is CompatibilityResult.Incompatible) {
            return ImportResult.IncompatibleArchitecture(
                sourceArch = manifest.architecture,
                hostArch = hostArchitecture,
                reason = compat.reason
            )
        }

        // 3. Prepare target directory
        val envName = customName ?: manifest.name
        val sanitizedName = envName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val targetRootfs = File(targetEnvsDir, sanitizedName)
        if (targetRootfs.exists()) {
            targetRootfs.deleteRecursively()
        }
        targetRootfs.mkdirs()

        // 4. Unpack rootfs entries into targetRootfs
        try {
            unpackRootfsFromArchive(archiveFile, targetRootfs)
        } catch (e: Exception) {
            targetRootfs.deleteRecursively()
            return ImportResult.CorruptedArchive("Failed to extract rootfs from archive: ${e.message}")
        }

        val env = manifest.toEnvironment(
            rootfsPath = targetRootfs.absolutePath,
            customName = envName
        )
        return ImportResult.Success(env)
    }

    private fun extractManifest(archiveFile: File): EnvironmentManifest? {
        return try {
            GZIPInputStream(FileInputStream(archiveFile)).use { stream ->
                val buffer = ByteArray(512)
                while (true) {
                    val bytesRead = readFully(stream, buffer, 0, 512)
                    if (bytesRead < 512 || isAllZeros(buffer)) break

                    val name = readString(buffer, 0, 100).trim()
                    val size = readOctal(buffer, 124, 12)

                    if (name == MANIFEST_FILENAME || name.endsWith("/$MANIFEST_FILENAME")) {
                        val contentBytes = ByteArray(size.toInt())
                        readFully(stream, contentBytes, 0, size.toInt())
                        val json = String(contentBytes, Charsets.UTF_8)
                        return EnvironmentManifest.fromJson(json)
                    } else {
                        val blocksToSkip = (size + 511) / 512
                        skipBytes(stream, blocksToSkip * 512)
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun unpackRootfsFromArchive(archiveFile: File, targetDir: File) {
        GZIPInputStream(FileInputStream(archiveFile)).use { stream ->
            val buffer = ByteArray(512)
            while (true) {
                val bytesRead = readFully(stream, buffer, 0, 512)
                if (bytesRead < 512 || isAllZeros(buffer)) break

                val name = readString(buffer, 0, 100).trim()
                val mode = readOctal(buffer, 100, 8).toInt()
                val size = readOctal(buffer, 124, 12)
                val typeFlag = buffer[156].toInt().toChar()
                val linkName = readString(buffer, 157, 100).trim()

                if (name == MANIFEST_FILENAME || name.endsWith("/$MANIFEST_FILENAME")) {
                    val blocksToSkip = (size + 511) / 512
                    skipBytes(stream, blocksToSkip * 512)
                    continue
                }

                // Strip leading "rootfs/" if present
                val relPath = if (name.startsWith("rootfs/")) name.removePrefix("rootfs/") else name
                if (relPath.isEmpty()) {
                    val blocksToSkip = (size + 511) / 512
                    skipBytes(stream, blocksToSkip * 512)
                    continue
                }

                // Path traversal check
                val normalized = java.nio.file.Paths.get(relPath).normalize().toString()
                if (normalized.startsWith("..") || normalized.startsWith("/")) {
                    val blocksToSkip = (size + 511) / 512
                    skipBytes(stream, blocksToSkip * 512)
                    continue
                }

                val destFile = File(targetDir, normalized)

                when (typeFlag) {
                    '5' -> { // Directory
                        destFile.mkdirs()
                    }
                    '2' -> { // Symbolic link
                        destFile.parentFile?.mkdirs()
                        try {
                            if (destFile.exists()) destFile.delete()
                            java.nio.file.Files.createSymbolicLink(
                                destFile.toPath(),
                                java.nio.file.Paths.get(linkName)
                            )
                        } catch (_: Exception) {
                        }
                    }
                    '0', '\u0000' -> { // Regular file
                        destFile.parentFile?.mkdirs()
                        destFile.outputStream().use { out ->
                            var remaining = size
                            val fileBuf = ByteArray(4096)
                            while (remaining > 0) {
                                val toRead = minOf(remaining, fileBuf.size.toLong()).toInt()
                                val r = readFully(stream, fileBuf, 0, toRead)
                                if (r <= 0) break
                                out.write(fileBuf, 0, r)
                                remaining -= r
                            }
                        }
                        val remainder = (size % 512).toInt()
                        if (remainder > 0) {
                            skipBytes(stream, (512 - remainder).toLong())
                        }
                        if ((mode and 0b001001001) != 0) {
                            destFile.setExecutable(true, false)
                        }
                    }
                    else -> {
                        val blocksToSkip = (size + 511) / 512
                        skipBytes(stream, blocksToSkip * 512)
                    }
                }
            }
        }
    }

    private fun collectFiles(
        current: File,
        baseDir: File,
        excludes: Set<String>,
        result: MutableList<File>
    ) {
        val relPath = current.relativeTo(baseDir).path.replace('\\', '/')
        if (relPath.isNotEmpty()) {
            val normalized = relPath.trim('/')
            for (excl in excludes) {
                val normExcl = excl.trim('/')
                if (normalized == normExcl || normalized.startsWith("$normExcl/")) {
                    return
                }
            }
        }

        if (current != baseDir) {
            result.add(current)
        }

        if (current.isDirectory && !java.nio.file.Files.isSymbolicLink(current.toPath())) {
            val children = current.listFiles() ?: return
            children.sortBy { it.name }
            for (child in children) {
                collectFiles(child, baseDir, excludes, result)
            }
        }
    }

    private fun buildUstarHeader(
        header: ByteArray,
        name: String,
        mode: Long,
        size: Long,
        mtime: Long,
        typeFlag: Char,
        linkName: String
    ) {
        writeString(header, 0, 100, name)
        writeOctal(header, 100, 8, mode)
        writeOctal(header, 108, 8, 0L)
        writeOctal(header, 116, 8, 0L)
        writeOctal(header, 124, 12, size)
        writeOctal(header, 136, 12, mtime)
        header[156] = typeFlag.code.toByte()
        writeString(header, 157, 100, linkName)
        writeString(header, 257, 6, "ustar\u0000")
        header[263] = '0'.code.toByte()
        header[264] = '0'.code.toByte()
        writeString(header, 265, 32, "root")
        writeString(header, 297, 32, "root")

        for (i in 148 until 156) {
            header[i] = ' '.code.toByte()
        }

        var sum = 0L
        for (b in header) {
            sum += (b.toInt() and 0xFF)
        }

        val octalSum = java.lang.Long.toOctalString(sum).padStart(6, '0')
        val chkBytes = "$octalSum\u0000 ".toByteArray(Charsets.US_ASCII)
        System.arraycopy(chkBytes, 0, header, 148, minOf(chkBytes.size, 8))
    }

    private fun writeString(dest: ByteArray, offset: Int, maxLen: Int, str: String) {
        val b = str.toByteArray(Charsets.UTF_8)
        System.arraycopy(b, 0, dest, offset, minOf(b.size, maxLen))
    }

    private fun writeOctal(dest: ByteArray, offset: Int, length: Int, value: Long) {
        val octalStr = java.lang.Long.toOctalString(value)
        val formatted = octalStr.padStart(length - 1, '0') + "\u0000"
        val bytes = formatted.toByteArray(Charsets.US_ASCII)
        System.arraycopy(bytes, 0, dest, offset, minOf(bytes.size, length))
    }

    private fun readString(bytes: ByteArray, offset: Int, length: Int): String {
        var len = 0
        while (len < length && bytes[offset + len] != 0.toByte()) {
            len++
        }
        return String(bytes, offset, len, Charsets.UTF_8)
    }

    private fun readOctal(bytes: ByteArray, offset: Int, length: Int): Long {
        var result = 0L
        for (i in offset until offset + length) {
            val b = bytes[i]
            if (b in '0'.code.toByte()..'7'.code.toByte()) {
                result = (result shl 3) + (b - '0'.code.toByte())
            } else if (b == 0.toByte() || b == ' '.code.toByte()) {
                if (result > 0) break
            }
        }
        return result
    }

    private fun isAllZeros(block: ByteArray): Boolean {
        for (b in block) {
            if (b != 0.toByte()) return false
        }
        return true
    }

    private fun readFully(stream: InputStream, buffer: ByteArray, offset: Int, length: Int): Int {
        var total = 0
        while (total < length) {
            val read = stream.read(buffer, offset + total, length - total)
            if (read == -1) break
            total += read
        }
        return total
    }

    private fun skipBytes(stream: InputStream, count: Long) {
        var remaining = count
        val discard = ByteArray(4096)
        while (remaining > 0) {
            val toRead = minOf(remaining, discard.size.toLong()).toInt()
            val read = stream.read(discard, 0, toRead)
            if (read <= 0) break
            remaining -= read
        }
    }
}
