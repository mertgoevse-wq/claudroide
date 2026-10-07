package org.claudroide.app.feature.linux.distro

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

/**
 * Pure Kotlin/JVM POSIX Tar and Tar.gz archive creator.
 * Handles directories, regular files, symbolic links, file permissions,
 * and exclusion patterns for rootfs snapshots and environment exports.
 */
object TarArchiver {

    val DEFAULT_ROOTFS_EXCLUDES: Set<String> = setOf(
        "dev",
        "proc",
        "sys",
        "snapshots",
        "tmp/.X11-unix",
        "tmp/.X1-lock"
    )

    /**
     * Creates a tar (or tar.gz) archive of [sourceDir] at [destinationArchive].
     *
     * @param sourceDir The root directory to archive
     * @param destinationArchive The destination file to write the archive to
     * @param isGzip Whether to compress with GZIP
     * @param excludes Set of relative paths or prefixes to exclude (e.g. "dev", "proc")
     * @param onProgress Optional callback invoked with cumulative uncompressed bytes archived
     * @return Total uncompressed bytes archived
     */
    fun archive(
        sourceDir: File,
        destinationArchive: File,
        isGzip: Boolean = true,
        excludes: Set<String> = DEFAULT_ROOTFS_EXCLUDES,
        onProgress: ((bytesArchived: Long) -> Unit)? = null
    ): Long {
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            throw IllegalArgumentException("Source directory does not exist or is not a directory: ${sourceDir.absolutePath}")
        }

        destinationArchive.parentFile?.mkdirs()

        val fos = FileOutputStream(destinationArchive)
        val outStream: OutputStream = if (isGzip) GZIPOutputStream(fos) else fos

        var totalBytesArchived = 0L

        outStream.use { out ->
            val filesToArchive = mutableListOf<File>()
            collectFiles(sourceDir, sourceDir, excludes, filesToArchive)

            for (file in filesToArchive) {
                val relPath = file.relativeTo(sourceDir).path.replace('\\', '/')
                if (relPath.isEmpty()) continue

                val isDir = file.isDirectory
                val isSymlink = Files.isSymbolicLink(file.toPath())
                val entryName = if (isDir && !relPath.endsWith("/")) "$relPath/" else relPath

                val header = ByteArray(512)
                val typeFlag = when {
                    isSymlink -> '2'
                    isDir -> '5'
                    else -> '0'
                }

                val linkTarget = if (isSymlink) {
                    Files.readSymbolicLink(file.toPath()).toString()
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

                val mtime = try {
                    file.lastModified() / 1000L
                } catch (_: Exception) {
                    System.currentTimeMillis() / 1000L
                }

                buildHeader(
                    header = header,
                    name = entryName,
                    mode = mode,
                    size = fileSize,
                    mtime = mtime,
                    typeFlag = typeFlag,
                    linkName = linkTarget
                )

                out.write(header)
                totalBytesArchived += 512

                if (!isDir && !isSymlink && fileSize > 0) {
                    FileInputStream(file).use { fis ->
                        val buffer = ByteArray(4096)
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            totalBytesArchived += read
                            onProgress?.invoke(totalBytesArchived)
                        }
                    }

                    // Pad file to 512-byte block boundary
                    val remainder = (fileSize % 512).toInt()
                    if (remainder > 0) {
                        val pad = 512 - remainder
                        out.write(ByteArray(pad))
                        totalBytesArchived += pad
                    }
                }
            }

            // Write two 512-byte zero blocks to signify EOF in POSIX tar
            out.write(ByteArray(1024))
            totalBytesArchived += 1024
        }

        return totalBytesArchived
    }

    private fun collectFiles(
        current: File,
        baseDir: File,
        excludes: Set<String>,
        result: MutableList<File>
    ) {
        val relPath = current.relativeTo(baseDir).path.replace('\\', '/')
        if (relPath.isNotEmpty() && isExcluded(relPath, excludes)) {
            return
        }

        if (current != baseDir) {
            result.add(current)
        }

        if (current.isDirectory && !Files.isSymbolicLink(current.toPath())) {
            val children = current.listFiles() ?: return
            // Sort deterministically for reproducible archives
            children.sortBy { it.name }
            for (child in children) {
                collectFiles(child, baseDir, excludes, result)
            }
        }
    }

    private fun isExcluded(relPath: String, excludes: Set<String>): Boolean {
        val normalized = relPath.trim('/')
        for (exclude in excludes) {
            val normExcl = exclude.trim('/')
            if (normalized == normExcl || normalized.startsWith("$normExcl/")) {
                return true
            }
        }
        return false
    }

    private fun buildHeader(
        header: ByteArray,
        name: String,
        mode: Long,
        size: Long,
        mtime: Long,
        typeFlag: Char,
        linkName: String
    ) {
        // Name (0..99)
        writeString(header, 0, 100, name)
        // Mode (100..107)
        writeOctal(header, 100, 8, mode)
        // UID (108..115)
        writeOctal(header, 108, 8, 0L)
        // GID (116..123)
        writeOctal(header, 116, 8, 0L)
        // Size (124..135)
        writeOctal(header, 124, 12, size)
        // MTime (136..147)
        writeOctal(header, 136, 12, mtime)
        // TypeFlag (156)
        header[156] = typeFlag.code.toByte()
        // LinkName (157..256)
        writeString(header, 157, 100, linkName)
        // Magic (257..262): "ustar\u0000"
        writeString(header, 257, 6, "ustar\u0000")
        // Version (263..264): "00"
        header[263] = '0'.code.toByte()
        header[264] = '0'.code.toByte()
        // UName (265..296)
        writeString(header, 265, 32, "root")
        // GName (297..328)
        writeString(header, 297, 32, "root")

        // Checksum calculation: Treat checksum field (148..155) as 8 spaces ' '
        for (i in 148 until 156) {
            header[i] = ' '.code.toByte()
        }

        var sum = 0L
        for (b in header) {
            sum += (b.toInt() and 0xFF)
        }

        // Write checksum as 6 octal digits + null + space
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
}
