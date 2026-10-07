package org.claudroide.app.feature.linux.distro

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPInputStream

/**
 * Pure Kotlin/JVM POSIX Tar and Tar.gz archive extractor.
 * Handles directory creation, regular files, file permissions, and symbolic links.
 */
object TarExtractor {

    data class TarEntryHeader(
        val name: String,
        val mode: Int,
        val size: Long,
        val typeFlag: Char,
        val linkName: String
    )

    /**
     * Extracts an uncompressed or gzip-compressed tar archive stream into [destinationDir].
     */
    fun extract(
        inputStream: InputStream,
        destinationDir: File,
        isGzip: Boolean = false,
        onProgress: ((bytesExtracted: Long) -> Unit)? = null
    ): Long {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val stream: InputStream = if (isGzip) GZIPInputStream(inputStream) else inputStream
        var totalBytesExtracted = 0L
        val buffer = ByteArray(512)

        while (true) {
            val bytesRead = readFully(stream, buffer, 0, 512)
            if (bytesRead < 512) break

            // End of archive indicator: two consecutive 512-byte blocks of all zeros
            if (isAllZeros(buffer)) {
                break
            }

            val header = parseHeader(buffer) ?: continue

            // Normalize path to prevent Zip Slip / path traversal
            val targetPath = resolveSafely(destinationDir, header.name) ?: continue

            when (header.typeFlag) {
                '5' -> { // Directory
                    targetPath.toFile().mkdirs()
                }
                '2' -> { // Symbolic link
                    targetPath.toFile().parentFile?.mkdirs()
                    try {
                        if (Files.exists(targetPath)) {
                            Files.delete(targetPath)
                        }
                        val linkTarget = Paths.get(header.linkName)
                        Files.createSymbolicLink(targetPath, linkTarget)
                    } catch (_: Exception) {
                        // Fallback: write text file with target or skip if OS forbids
                    }
                }
                '0', '\u0000' -> { // Regular file
                    targetPath.toFile().parentFile?.mkdirs()
                    val written = extractFileContent(stream, targetPath.toFile(), header.size)
                    totalBytesExtracted += written
                    onProgress?.invoke(totalBytesExtracted)

                    // Restore executable permissions if executable bit set in mode (0111)
                    if ((header.mode and 0b001001001) != 0) {
                        targetPath.toFile().setExecutable(true, false)
                    }
                }
                else -> {
                    // Skip unsupported entries (devices, fifos, etc.) by skipping data blocks
                    val blocksToSkip = (header.size + 511) / 512
                    skipBytes(stream, blocksToSkip * 512)
                }
            }
        }

        return totalBytesExtracted
    }

    private fun parseHeader(block: ByteArray): TarEntryHeader? {
        val name = readString(block, 0, 100).trim()
        if (name.isEmpty()) return null

        val mode = readOctal(block, 100, 8).toInt()
        val size = readOctal(block, 124, 12)
        val typeFlag = block[156].toInt().toChar()
        val linkName = readString(block, 157, 100).trim()

        return TarEntryHeader(
            name = name,
            mode = mode,
            size = size,
            typeFlag = typeFlag,
            linkName = linkName
        )
    }

    private fun extractFileContent(stream: InputStream, destFile: File, fileSize: Long): Long {
        var remaining = fileSize
        val buffer = ByteArray(4096)
        destFile.outputStream().use { out ->
            while (remaining > 0) {
                val toRead = minOf(remaining, buffer.size.toLong()).toInt()
                val read = readFully(stream, buffer, 0, toRead)
                if (read <= 0) break
                out.write(buffer, 0, read)
                remaining -= read
            }
        }

        // Tar blocks are padded to 512 bytes
        val remainder = (fileSize % 512).toInt()
        if (remainder > 0) {
            val pad = 512 - remainder
            skipBytes(stream, pad.toLong())
        }

        return fileSize
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

    private fun resolveSafely(baseDir: File, relativePath: String): Path? {
        val normalized = Paths.get(relativePath).normalize().toString()
        if (normalized.startsWith("..") || normalized.startsWith("/")) {
            return null
        }
        val resolved = baseDir.toPath().resolve(normalized).normalize()
        return if (resolved.startsWith(baseDir.toPath())) resolved else null
    }
}
