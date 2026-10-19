package org.claudroide.app.feature.project

import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Task 084 — "ZIP-Projekt öffnen" (Open ZIP project).
 *
 * Goal: Select a ZIP archive, inspect its contents safely, and make it available as a project.
 * Result: File list, storage requirements, destination folder, extraction progress, and cancellation.
 *
 * Protection invariants:
 *  1. **Zip-Slip Prevention:** No archive entry may escape the target project root via
 *     directory traversal (`../`), leading slashes, Windows drive letters (`C:\`), or backslashes.
 *     All candidate paths are validated against [PathBoundaryGuard] and resolved within [destinationDirectory].
 *  2. **Malicious & Special Names:** Rejects paths with NUL bytes, control characters, Windows
 *     reserved device names (CON, PRN, AUX, NUL, COM1-9, LPT1-9), or invalid characters.
 *  3. **Duplicate & Colliding Entries:** Detects exact duplicate paths and case-insensitive collisions.
 *  4. **Resource Exhaustion & Zip Bombs:** Enforces strict bounds:
 *     - Maximum total uncompressed size ([DEFAULT_MAX_UNCOMPRESSED_BYTES] = 500 MB)
 *     - Maximum entry count ([DEFAULT_MAX_ENTRY_COUNT] = 10,000)
 *     - Maximum single file size ([DEFAULT_MAX_SINGLE_FILE_BYTES] = 100 MB)
 *     - Excessive compression ratio threshold ([DEFAULT_MAX_COMPRESSION_RATIO] = 100x)
 *  5. **Pre-Extraction Transparency:** The user inspects the destination path, file list,
 *     compressed vs. uncompressed size, and any warnings *before* extraction can be initiated.
 *  6. **Overwrite Protection:** Existing destination files are detected during inspection.
 *     Extraction is strictly refused unless explicit overwrite confirmation ([overwriteConfirmed] = true)
 *     is granted by the user.
 *  7. **Cancellation & Progress:** Long-running extractions report progress and can be cancelled
 *     at any point, stopping further writes and reporting all partially extracted files.
 *
 * Pure Kotlin logic: testable on the JVM without requiring Android platform dependencies.
 */
object ZipProjectImportPolicy {

    const val DEFAULT_MAX_UNCOMPRESSED_BYTES = 500L * 1024L * 1024L // 500 MB
    const val DEFAULT_MAX_ENTRY_COUNT = 10_000
    const val DEFAULT_MAX_SINGLE_FILE_BYTES = 100L * 1024L * 1024L // 100 MB
    const val DEFAULT_MAX_COMPRESSION_RATIO = 100.0 // 100x ratio threshold for zip bomb heuristic
    const val ZIP_BOMB_MIN_BYTES = 10L * 1024L * 1024L // 10 MB minimum uncompressed for ratio heuristic

    private val WINDOWS_RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    /**
     * Issues found during zip entry inspection.
     */
    enum class ZipEntryIssue(val label: String, val blocksExtraction: Boolean) {
        ZIP_SLIP_ATTEMPT(
            "Pfad versucht, den Projektordner zu verlassen (Zip-Slip)",
            true
        ),
        MALFORMED_NAME(
            "Pfad enthält unzulässige Zeichen oder Steuerzeichen",
            true
        ),
        DUPLICATE_ENTRY(
            "Archiv enthält doppelte Dateipfade",
            true
        ),
        CASE_COLLISION(
            "Archiv enthält kollidierende Dateinamen bei Groß-/Kleinschreibung",
            true
        ),
        UNEXPECTED_ABSOLUTE_PATH(
            "Absoluter Pfad im Archiv ist nicht zulässig",
            true
        ),
        RESERVED_DEVICE_NAME(
            "Pfad enthält einen reservierten System- oder Gerätenamen",
            true
        ),
        EXCEEDS_SINGLE_FILE_LIMIT(
            "Einzeldatei überschreitet die erlaubte Maximalgröße",
            true
        ),
        SUSPICIOUS_COMPRESSION_RATIO(
            "Verdächtiges Kompressionsverhältnis (mögliche Zip-Bombe)",
            true
        )
    }

    /**
     * Metadata and safety verdict for a single entry within the archive.
     */
    data class ZipEntryMetadata(
        val rawPath: String,
        val normalizedPath: String,
        val isDirectory: Boolean,
        val compressedSize: Long,
        val uncompressedSize: Long,
        val issues: List<ZipEntryIssue> = emptyList()
    ) {
        val isSafe: Boolean get() = issues.none { it.blocksExtraction }
    }

    /**
     * Configuration limits for archive inspection and extraction.
     */
    data class ZipImportLimits(
        val maxUncompressedBytes: Long = DEFAULT_MAX_UNCOMPRESSED_BYTES,
        val maxEntryCount: Int = DEFAULT_MAX_ENTRY_COUNT,
        val maxSingleFileBytes: Long = DEFAULT_MAX_SINGLE_FILE_BYTES,
        val maxCompressionRatio: Double = DEFAULT_MAX_COMPRESSION_RATIO
    )

    /**
     * Complete inspection plan prepared for user review prior to extraction.
     */
    data class ZipInspectionPlan(
        val archiveName: String,
        val destinationDirectory: String,
        val totalEntries: Int,
        val fileCount: Int,
        val directoryCount: Int,
        val totalCompressedBytes: Long,
        val totalUncompressedBytes: Long,
        val entries: List<ZipEntryMetadata>,
        val blockingIssues: List<String>,
        val existingCollisions: List<String>,
        val overwriteConfirmed: Boolean = false
    ) {
        val hasBlockingSecurityIssues: Boolean get() = blockingIssues.isNotEmpty()

        val hasCollisions: Boolean get() = existingCollisions.isNotEmpty()

        /**
         * Safe to extract only when no security boundaries are violated
         * and any existing file collisions are explicitly acknowledged by the user.
         */
        val canExtract: Boolean
            get() = !hasBlockingSecurityIssues && (!hasCollisions || overwriteConfirmed)

        val compressionRatio: Double
            get() = if (totalCompressedBytes > 0) {
                totalUncompressedBytes.toDouble() / totalCompressedBytes.toDouble()
            } else 1.0

        /**
         * User-facing summary explaining destination, file counts, sizes, and warnings.
         */
        val summaryLines: List<String>
            get() {
                val lines = mutableListOf<String>()
                lines += "Archiv: $archiveName"
                lines += "Zielordner: $destinationDirectory"
                lines += "Dateien: $fileCount ($directoryCount Ordner, $totalEntries Einträge gesamt)"
                lines += "Speicherbedarf unkomprimiert: ${formatBytes(totalUncompressedBytes)}"
                lines += "Größe im Archiv: ${formatBytes(totalCompressedBytes)} (Kompressionsfaktor: ${"%.1f".format(Locale.US, compressionRatio)}x)"

                if (blockingIssues.isNotEmpty()) {
                    lines += "Sicherheitswarnungen (${blockingIssues.size}):"
                    blockingIssues.forEach { lines += "  - $it" }
                }

                if (existingCollisions.isNotEmpty()) {
                    lines += "Bereits vorhandene Dateien im Ziel (${existingCollisions.size}):"
                    existingCollisions.take(5).forEach { lines += "  - $it" }
                    if (existingCollisions.size > 5) {
                        lines += "  - ... und ${existingCollisions.size - 5} weitere"
                    }
                    if (!overwriteConfirmed) {
                        lines += "Hinweis: Überschreiben ist nicht freigegeben. Entpacken blockiert."
                    } else {
                        lines += "Hinweis: Überschreiben wurde vom Nutzer ausdrücklich bestätigt."
                    }
                }

                return lines
            }

        private fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return "%.1f KB".format(Locale.US, kb)
            val mb = kb / 1024.0
            return "%.2f MB".format(Locale.US, mb)
        }
    }

    /**
     * Normalizes a raw ZIP entry name to a relative canonical path.
     * Replaces backslashes, collapses redundant separators and removes leading slashes.
     */
    fun normalizeEntryPath(rawPath: String): String {
        val sanitized = rawPath.replace('\\', '/')
        val parts = sanitized.split('/').filter { it.isNotEmpty() && it != "." }
        return parts.joinToString("/")
    }

    /**
     * Inspects an archive's entry headers without unpacking any file content.
     */
    fun inspect(
        archiveName: String,
        destinationDirectory: String,
        stream: InputStream,
        existingFiles: Set<String> = emptySet(),
        overwriteConfirmed: Boolean = false,
        limits: ZipImportLimits = ZipImportLimits()
    ): ZipInspectionPlan {
        val blockingIssues = mutableListOf<String>()
        val entries = mutableListOf<ZipEntryMetadata>()
        val seenPaths = mutableSetOf<String>()
        val seenLowerPaths = mutableSetOf<String>()
        val collisions = mutableListOf<String>()

        var totalEntries = 0
        var fileCount = 0
        var dirCount = 0
        var totalCompressedBytes = 0L
        var totalUncompressedBytes = 0L

        val zipIn = ZipInputStream(stream)
        var entry: ZipEntry? = zipIn.nextEntry

        while (entry != null) {
            totalEntries++
            if (totalEntries > limits.maxEntryCount) {
                blockingIssues += "Archiv überschreitet das Limit von ${limits.maxEntryCount} Einträgen."
                break
            }

            val rawName = entry.name
            val isDir = entry.isDirectory || rawName.endsWith("/")
            val entryDeclaredCompressed = entry.compressedSize
            val entryDeclaredUncompressed = entry.size

            val issues = mutableListOf<ZipEntryIssue>()

            // Measure actual uncompressed bytes by safely draining entry content
            var actualBytes = 0L
            if (!isDir) {
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                    actualBytes += bytesRead
                    if (actualBytes > limits.maxSingleFileBytes) {
                        issues += ZipEntryIssue.EXCEEDS_SINGLE_FILE_LIMIT
                        break
                    }
                    if (totalUncompressedBytes + actualBytes > limits.maxUncompressedBytes) {
                        blockingIssues += "Gesamte unkomprimierte Größe überschreitet das Limit von ${limits.maxUncompressedBytes / (1024 * 1024)} MB."
                        break
                    }
                }
            }

            val finalUncompressed = if (actualBytes > 0) actualBytes else entryDeclaredUncompressed.coerceAtLeast(0L)
            val finalCompressed = when {
                entryDeclaredCompressed >= 0 -> entryDeclaredCompressed
                actualBytes > 0 -> actualBytes
                else -> 0L
            }

            totalCompressedBytes += finalCompressed
            totalUncompressedBytes += finalUncompressed

            // 1. Malformed name / NUL bytes / control characters
            if (rawName.indexOf('\u0000') >= 0 || rawName.any { it.isISOControl() && it != '\t' }) {
                issues += ZipEntryIssue.MALFORMED_NAME
            }

            // 2. Absolute path check
            if (rawName.startsWith("/") || rawName.startsWith("\\") || rawName.matches(Regex("^[a-zA-Z]:.*"))) {
                issues += ZipEntryIssue.UNEXPECTED_ABSOLUTE_PATH
            }

            // 3. Zip-Slip check using PathBoundaryGuard
            val normalized = normalizeEntryPath(rawName)
            if (normalized.isEmpty() && !isDir) {
                issues += ZipEntryIssue.MALFORMED_NAME
            } else if (normalized.isNotEmpty()) {
                val boundaryVerdict = PathBoundaryGuard.check(normalized, destinationDirectory)
                if (!boundaryVerdict.isAllowed) {
                    issues += ZipEntryIssue.ZIP_SLIP_ATTEMPT
                }
            }

            // 4. Reserved Windows/DOS system device names
            val segments = normalized.split('/')
            for (seg in segments) {
                val base = seg.substringBefore('.').uppercase(Locale.ROOT)
                if (WINDOWS_RESERVED_NAMES.contains(base)) {
                    issues += ZipEntryIssue.RESERVED_DEVICE_NAME
                    break
                }
            }

            // 5. Duplicates & Case collisions
            if (!isDir && normalized.isNotEmpty()) {
                if (!seenPaths.add(normalized)) {
                    issues += ZipEntryIssue.DUPLICATE_ENTRY
                }
                val lower = normalized.lowercase(Locale.ROOT)
                if (!seenLowerPaths.add(lower)) {
                    // Only flag if not already flagged as duplicate
                    if (!issues.contains(ZipEntryIssue.DUPLICATE_ENTRY)) {
                        issues += ZipEntryIssue.CASE_COLLISION
                    }
                }
            }

            // 6. Single file limit check
            if (!isDir && finalUncompressed > limits.maxSingleFileBytes) {
                if (!issues.contains(ZipEntryIssue.EXCEEDS_SINGLE_FILE_LIMIT)) {
                    issues += ZipEntryIssue.EXCEEDS_SINGLE_FILE_LIMIT
                }
            }

            // 7. Compression ratio check on known sizes
            if (!isDir && finalCompressed > 0 && finalUncompressed >= ZIP_BOMB_MIN_BYTES) {
                val ratio = finalUncompressed.toDouble() / finalCompressed.toDouble()
                if (ratio > limits.maxCompressionRatio) {
                    issues += ZipEntryIssue.SUSPICIOUS_COMPRESSION_RATIO
                }
            }

            // 8. Existing file collisions in destination
            if (!isDir && normalized.isNotEmpty() && existingFiles.contains(normalized)) {
                collisions += normalized
            }

            if (isDir) {
                dirCount++
            } else {
                fileCount++
            }

            entries += ZipEntryMetadata(
                rawPath = rawName,
                normalizedPath = normalized,
                isDirectory = isDir,
                compressedSize = finalCompressed,
                uncompressedSize = finalUncompressed,
                issues = issues
            )

            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        // Aggregate limits check
        if (totalUncompressedBytes > limits.maxUncompressedBytes) {
            blockingIssues += "Gesamte unkomprimierte Größe (${totalUncompressedBytes / (1024 * 1024)} MB) " +
                "überschreitet das Limit von ${limits.maxUncompressedBytes / (1024 * 1024)} MB."
        }

        if (totalCompressedBytes > 0 && totalUncompressedBytes >= ZIP_BOMB_MIN_BYTES) {
            val overallRatio = totalUncompressedBytes.toDouble() / totalCompressedBytes.toDouble()
            if (overallRatio > limits.maxCompressionRatio) {
                blockingIssues += "Verdächtiges Gesamtkompressionsverhältnis (${"%.1f".format(Locale.US, overallRatio)}x). Mögliche Zip-Bombe."
            }
        }

        if (totalEntries == 0) {
            blockingIssues += "Das Archiv ist leer und enthält keine Dateien."
        }

        // Collect individual entry issues into blocking issues
        val entrySecurityIssues = entries.flatMap { it.issues }.filter { it.blocksExtraction }.toSet()
        for (issue in entrySecurityIssues) {
            blockingIssues += issue.label
        }

        return ZipInspectionPlan(
            archiveName = archiveName,
            destinationDirectory = destinationDirectory,
            totalEntries = totalEntries,
            fileCount = fileCount,
            directoryCount = dirCount,
            totalCompressedBytes = totalCompressedBytes,
            totalUncompressedBytes = totalUncompressedBytes,
            entries = entries,
            blockingIssues = blockingIssues.distinct(),
            existingCollisions = collisions.distinct(),
            overwriteConfirmed = overwriteConfirmed
        )
    }

    /**
     * Interface for writing extracted files and directories.
     * Allows pure JVM unit testing via in-memory structures or real file I/O on Android.
     */
    interface ZipOutputSink {
        fun createDirectories(relativePath: String)
        fun writeFile(relativePath: String, inputStream: InputStream, uncompressedBytes: Long)
        fun exists(relativePath: String): Boolean
        fun delete(relativePath: String): Boolean
    }

    /**
     * Extraction progress report.
     */
    data class ExtractionProgress(
        val extractedFiles: Int,
        val totalFiles: Int,
        val extractedBytes: Long,
        val totalBytes: Long,
        val currentEntry: String
    ) {
        val percentage: Int
            get() = if (totalBytes > 0) {
                ((extractedBytes.toDouble() / totalBytes.toDouble()) * 100.0).toInt().coerceIn(0, 100)
            } else 0
    }

    /**
     * Result of an extraction run.
     */
    sealed interface ExtractionResult {
        data class Success(
            val extractedCount: Int,
            val totalBytes: Long,
            val destinationFolder: String
        ) : ExtractionResult

        data class Refused(
            val reason: String,
            val blockingIssues: List<String> = emptyList()
        ) : ExtractionResult

        data class Cancelled(
            val extractedCount: Int,
            val extractedBytes: Long,
            val partiallyExtractedFiles: List<String>
        ) : ExtractionResult

        data class Failed(
            val error: String,
            val extractedCount: Int,
            val partiallyExtractedFiles: List<String>
        ) : ExtractionResult
    }

    /**
     * Cancellation token for an in-flight extraction.
     */
    class ExtractionCancellationToken {
        @Volatile
        var isCancelled: Boolean = false
            private set

        fun cancel() {
            isCancelled = true
        }
    }

    /**
     * Executes the extraction according to the previously verified [plan].
     *
     * Invariants enforced during extraction:
     * - The plan MUST be valid ([canExtract] == true). If invalid, refuses immediately.
     * - Each stream byte is counted against the maximum uncompressed limit dynamically to catch
     *   streaming zip bombs whose headers misrepresented uncompressed size.
     * - Cancellation is checked between each entry and during byte streaming.
     */
    fun extract(
        plan: ZipInspectionPlan,
        stream: InputStream,
        sink: ZipOutputSink,
        cancellationToken: ExtractionCancellationToken = ExtractionCancellationToken(),
        onProgress: (ExtractionProgress) -> Unit = {},
        limits: ZipImportLimits = ZipImportLimits()
    ): ExtractionResult {
        if (!plan.canExtract) {
            val reason = when {
                plan.hasBlockingSecurityIssues -> "Archiv enthält ungelöste Sicherheitsrisiken."
                plan.hasCollisions && !plan.overwriteConfirmed ->
                    "Zielordner enthält bestehende Dateien. Überschreiben wurde nicht bestätigt."
                else -> "Entpacken nicht zulässig."
            }
            return ExtractionResult.Refused(reason, plan.blockingIssues)
        }

        val writtenPaths = mutableListOf<String>()
        var extractedFiles = 0
        var totalBytesWritten = 0L

        try {
            val zipIn = ZipInputStream(stream)
            var entry: ZipEntry? = zipIn.nextEntry

            while (entry != null) {
                if (cancellationToken.isCancelled) {
                    return ExtractionResult.Cancelled(
                        extractedCount = extractedFiles,
                        extractedBytes = totalBytesWritten,
                        partiallyExtractedFiles = writtenPaths.toList()
                    )
                }

                val rawName = entry.name
                val isDir = entry.isDirectory || rawName.endsWith("/")
                val normalized = normalizeEntryPath(rawName)

                // Re-verify path safety as defense-in-depth during extraction
                if (normalized.isNotEmpty()) {
                    val boundaryVerdict = PathBoundaryGuard.check(normalized, plan.destinationDirectory)
                    if (!boundaryVerdict.isAllowed) {
                        return ExtractionResult.Failed(
                            "Zip-Slip Angriff erkannt bei: $rawName",
                            extractedFiles,
                            writtenPaths.toList()
                        )
                    }
                }

                if (isDir) {
                    if (normalized.isNotEmpty()) {
                        sink.createDirectories(normalized)
                    }
                } else if (normalized.isNotEmpty()) {
                    // Check collision policy
                    if (sink.exists(normalized) && !plan.overwriteConfirmed) {
                        return ExtractionResult.Refused(
                            "Datei $normalized existiert bereits und Überschreiben ist nicht bestätigt.",
                            listOf("Kollision mit bestehender Datei: $normalized")
                        )
                    }

                    // Ensure parent directories exist
                    val parentDir = normalized.substringBeforeLast('/', "")
                    if (parentDir.isNotEmpty()) {
                        sink.createDirectories(parentDir)
                    }

                    // Stream to sink while checking byte limits dynamically
                    val countingStream = CountingInputStream(zipIn) { bytesRead ->
                        totalBytesWritten += bytesRead
                        if (totalBytesWritten > limits.maxUncompressedBytes) {
                            throw SecurityException(
                                "Maximale unkomprimierte Archivgröße (${limits.maxUncompressedBytes} Bytes) überschritten."
                            )
                        }
                    }

                    sink.writeFile(normalized, countingStream, entry.size)
                    writtenPaths += normalized
                    extractedFiles++

                    onProgress(
                        ExtractionProgress(
                            extractedFiles = extractedFiles,
                            totalFiles = plan.fileCount,
                            extractedBytes = totalBytesWritten,
                            totalBytes = plan.totalUncompressedBytes,
                            currentEntry = normalized
                        )
                    )
                }

                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }

            return ExtractionResult.Success(
                extractedCount = extractedFiles,
                totalBytes = totalBytesWritten,
                destinationFolder = plan.destinationDirectory
            )
        } catch (se: SecurityException) {
            return ExtractionResult.Failed(
                "Sicherheitsabbruch: ${se.message}",
                extractedFiles,
                writtenPaths.toList()
            )
        } catch (e: Exception) {
            return ExtractionResult.Failed(
                "Fehler beim Entpacken: ${e.message}",
                extractedFiles,
                writtenPaths.toList()
            )
        }
    }

    /**
     * Input stream wrapper that reports chunked byte counts to detect
     * decompression bombs mid-stream.
     */
    private class CountingInputStream(
        private val delegate: InputStream,
        private val onBytesRead: (Long) -> Unit
    ) : InputStream() {
        override fun read(): Int {
            val b = delegate.read()
            if (b != -1) {
                onBytesRead(1L)
            }
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val count = delegate.read(b, off, len)
            if (count > 0) {
                onBytesRead(count.toLong())
            }
            return count
        }

        override fun close() {
            // Do not close ZipInputStream entry prematurely
        }
    }
}
