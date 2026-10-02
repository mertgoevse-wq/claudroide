package org.claudroide.app

import org.claudroide.app.feature.project.ZipProjectImportPolicy
import org.claudroide.app.feature.project.ZipProjectImportPolicy.ExtractionProgress
import org.claudroide.app.feature.project.ZipProjectImportPolicy.ExtractionResult
import org.claudroide.app.feature.project.ZipProjectImportPolicy.ZipEntryIssue
import org.claudroide.app.feature.project.ZipProjectImportPolicy.ZipImportLimits
import org.claudroide.app.feature.project.ZipProjectImportPolicy.ZipInspectionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Task 084 — "ZIP-Projekt öffnen" (Open ZIP project).
 *
 * Verifies that:
 * 1. Valid archives can be inspected and cleanly extracted.
 * 2. Zip-Slip attacks (directory traversal, leading slashes, Windows separators) are caught.
 * 3. Malicious names (NUL bytes, control characters, reserved device names) are rejected.
 * 4. Duplicate entries and case-insensitive collisions are identified and blocked.
 * 5. Size limits, entry counts, and potential zip bombs are strictly enforced.
 * 6. Destination collisions require explicit user overwrite confirmation.
 * 7. Long-running extractions report progress and can be cancelled safely.
 */
class ZipProjectImportTest {

    private val targetFolder = "/data/data/org.claudroide.app/files/projects/demo-project"

    private class InMemoryZipSink : ZipProjectImportPolicy.ZipOutputSink {
        val directories = mutableSetOf<String>()
        val files = mutableMapOf<String, ByteArray>()

        override fun createDirectories(relativePath: String) {
            directories += relativePath
        }

        override fun writeFile(relativePath: String, inputStream: InputStream, uncompressedBytes: Long) {
            files[relativePath] = inputStream.readBytes()
        }

        override fun exists(relativePath: String): Boolean = files.containsKey(relativePath)

        override fun delete(relativePath: String): Boolean = files.remove(relativePath) != null
    }

    private fun createZip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for ((name, data) in entries) {
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)
                if (!name.endsWith("/")) {
                    zos.write(data)
                }
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun text(s: String) = s.toByteArray(StandardCharsets.UTF_8)

    // ── 1. Valid Archive Inspection & Extraction ──────────────────────────

    @Test
    fun `validZip_inspectsFileAndFolderCountsAndSizesAccurately`() {
        val zipBytes = createZip(
            "src/" to byteArrayOf(),
            "src/Main.kt" to text("fun main() = println(\"hello\")"),
            "README.md" to text("# Demo Project\nWelcome!"),
            "assets/logo.png" to ByteArray(128) { 0x42 }
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "demo.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        assertEquals("demo.zip", plan.archiveName)
        assertEquals(targetFolder, plan.destinationDirectory)
        assertEquals(4, plan.totalEntries)
        assertEquals(3, plan.fileCount)
        assertEquals(1, plan.directoryCount)
        assertTrue(plan.totalUncompressedBytes > 0)
        assertTrue(plan.canExtract)
        assertFalse(plan.hasBlockingSecurityIssues)
        assertFalse(plan.hasCollisions)
    }

    @Test
    fun `validZip_extractsAllFilesSuccessfully`() {
        val zipBytes = createZip(
            "src/" to byteArrayOf(),
            "src/Main.kt" to text("fun main() = 1"),
            "README.md" to text("Hello World")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "demo.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        val sink = InMemoryZipSink()
        val result = ZipProjectImportPolicy.extract(
            plan = plan,
            stream = ByteArrayInputStream(zipBytes),
            sink = sink
        )

        assertTrue(result is ExtractionResult.Success)
        val success = result as ExtractionResult.Success
        assertEquals(2, success.extractedCount)
        assertEquals(targetFolder, success.destinationFolder)
        assertEquals("fun main() = 1", String(sink.files["src/Main.kt"]!!))
        assertEquals("Hello World", String(sink.files["README.md"]!!))
        assertTrue(sink.directories.contains("src"))
    }

    @Test
    fun `validZip_preExtractionSummary_presentsClearInformation`() {
        val zipBytes = createZip(
            "Main.kt" to text("class App"),
            "docs/" to byteArrayOf()
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "starter.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        val summary = plan.summaryLines.joinToString("\n")
        assertTrue(summary.contains("starter.zip"))
        assertTrue(summary.contains(targetFolder))
        assertTrue(summary.contains("Dateien: 1 (1 Ordner, 2 Einträge gesamt)"))
        assertTrue(summary.contains("Speicherbedarf unkomprimiert:"))
    }

    // ── 2. Zip-Slip Attacks (Traversals & Escapes) ──────────────────────────

    @Test
    fun `zipSlip_directoryTraversal_isDetectedAndBlocked`() {
        val zipBytes = createZip(
            "safe.txt" to text("safe"),
            "../../etc/passwd" to text("root:x:0:0")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "malicious.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.hasBlockingSecurityIssues)
        assertTrue(plan.entries.any { it.issues.contains(ZipEntryIssue.ZIP_SLIP_ATTEMPT) })
    }

    @Test
    fun `zipSlip_leadingSlash_isDetectedAndBlocked`() {
        val zipBytes = createZip(
            "/system/bin/bad" to text("exploit")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "bad-root.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.entries.any { it.issues.contains(ZipEntryIssue.UNEXPECTED_ABSOLUTE_PATH) })
    }

    @Test
    fun `zipSlip_windowsBackslashTraversal_isDetectedAndBlocked`() {
        val zipBytes = createZip(
            "..\\..\\Windows\\System32\\bad.dll" to text("bad")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "windows-slip.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.entries.any { it.issues.contains(ZipEntryIssue.ZIP_SLIP_ATTEMPT) })
    }

    @Test
    fun `zipSlip_extractionIsRefusedWhenPlanHasZipSlip`() {
        val zipBytes = createZip(
            "../escape.txt" to text("escape")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "slip.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        val sink = InMemoryZipSink()
        val result = ZipProjectImportPolicy.extract(
            plan = plan,
            stream = ByteArrayInputStream(zipBytes),
            sink = sink
        )

        assertTrue(result is ExtractionResult.Refused)
        assertEquals(0, sink.files.size)
    }

    // ── 3. Malformed and Reserved Names ────────────────────────────────────

    @Test
    fun `malformedName_nulByte_isDetectedAndBlocked`() {
        val zipBytes = createZip(
            "hello\u0000world.txt" to text("danger")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "nul.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.entries.any { it.issues.contains(ZipEntryIssue.MALFORMED_NAME) })
    }

    @Test
    fun `reservedWindowsDeviceName_isDetectedAndBlocked`() {
        val zipBytes = createZip(
            "CON.txt" to text("reserved device")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "con.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.entries.any { it.issues.contains(ZipEntryIssue.RESERVED_DEVICE_NAME) })
    }

    @Test
    fun `reservedDeviceName_inSubfolder_isDetectedAndBlocked`() {
        val zipBytes = createZip(
            "nested/sub/aux" to text("reserved auxiliary")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "aux.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.entries.any { it.issues.contains(ZipEntryIssue.RESERVED_DEVICE_NAME) })
    }

    // ── 4. Duplicate Entries & Case Collisions ─────────────────────────────

    @Test
    fun `duplicateEntry_isDetectedAndBlocked`() {
        val rawZip = createZip(
            "file1.txt" to text("first"),
            "file2.txt" to text("second")
        )
        // Patch the zip bytes replacing "file2.txt" with "file1.txt" to create an archive with duplicate entries
        val target = "file2.txt".toByteArray(StandardCharsets.UTF_8)
        val replacement = "file1.txt".toByteArray(StandardCharsets.UTF_8)
        val patched = rawZip.clone()
        for (i in 0 until patched.size - target.size) {
            var match = true
            for (j in target.indices) {
                if (patched[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                System.arraycopy(replacement, 0, patched, i, replacement.size)
            }
        }

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "dup.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(patched)
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.entries.any { it.issues.contains(ZipEntryIssue.DUPLICATE_ENTRY) })
    }

    @Test
    fun `caseCollision_isDetectedAndBlocked`() {
        val zipBytes = createZip(
            "Readme.md" to text("read 1"),
            "README.MD" to text("read 2")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "case.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.entries.any { it.issues.contains(ZipEntryIssue.CASE_COLLISION) })
    }

    // ── 5. Resource Limits & Empty Archives ────────────────────────────────

    @Test
    fun `emptyArchive_isDetectedAndRefused`() {
        val zipBytes = createZip() // 0 entries

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "empty.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.blockingIssues.any { it.contains("Archiv ist leer") })
    }

    @Test
    fun `exceedsTotalEntryLimit_isBlocked`() {
        val zipBytes = createZip(
            "file1.txt" to text("1"),
            "file2.txt" to text("2"),
            "file3.txt" to text("3")
        )

        val limits = ZipImportLimits(maxEntryCount = 2)
        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "toomany.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes),
            limits = limits
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.blockingIssues.any { it.contains("überschreitet das Limit von 2 Einträgen") })
    }

    @Test
    fun `exceedsTotalUncompressedSizeLimit_isBlocked`() {
        val zipBytes = createZip(
            "big.bin" to ByteArray(2048) { 1 }
        )

        val limits = ZipImportLimits(maxUncompressedBytes = 1000L)
        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "toobig.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes),
            limits = limits
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.blockingIssues.any { it.contains("überschreitet das Limit") })
    }

    @Test
    fun `exceedsSingleFileLimit_isBlocked`() {
        val zipBytes = createZip(
            "large_single.bin" to ByteArray(5000) { 1 }
        )

        val limits = ZipImportLimits(maxSingleFileBytes = 2000L)
        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "singlefile.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes),
            limits = limits
        )

        assertFalse(plan.canExtract)
        assertTrue(plan.entries.any { it.issues.contains(ZipEntryIssue.EXCEEDS_SINGLE_FILE_LIMIT) })
    }

    // ── 6. Existing Collisions & Overwrite Policy ──────────────────────────

    @Test
    fun `existingCollisions_withoutOverwriteConfirmation_blocksExtraction`() {
        val zipBytes = createZip(
            "existing.txt" to text("new version"),
            "fresh.txt" to text("brand new")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "collide.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes),
            existingFiles = setOf("existing.txt"),
            overwriteConfirmed = false
        )

        assertTrue(plan.hasCollisions)
        assertEquals(listOf("existing.txt"), plan.existingCollisions)
        assertFalse(plan.canExtract)

        val sink = InMemoryZipSink()
        sink.writeFile("existing.txt", ByteArrayInputStream(text("old")), 3)

        val result = ZipProjectImportPolicy.extract(
            plan = plan,
            stream = ByteArrayInputStream(zipBytes),
            sink = sink
        )

        assertTrue(result is ExtractionResult.Refused)
        assertEquals("old", String(sink.files["existing.txt"]!!))
        assertFalse(sink.files.containsKey("fresh.txt"))
    }

    @Test
    fun `existingCollisions_withOverwriteConfirmation_allowsExtraction`() {
        val zipBytes = createZip(
            "existing.txt" to text("new version")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "collide.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes),
            existingFiles = setOf("existing.txt"),
            overwriteConfirmed = true
        )

        assertTrue(plan.hasCollisions)
        assertTrue(plan.canExtract)

        val sink = InMemoryZipSink()
        sink.writeFile("existing.txt", ByteArrayInputStream(text("old")), 3)

        val result = ZipProjectImportPolicy.extract(
            plan = plan,
            stream = ByteArrayInputStream(zipBytes),
            sink = sink
        )

        assertTrue(result is ExtractionResult.Success)
        assertEquals("new version", String(sink.files["existing.txt"]!!))
    }

    @Test
    fun `existingCollisions_summaryExplainsCollisionCountAndStatus`() {
        val zipBytes = createZip(
            "conflict.txt" to text("content")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "conflict.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes),
            existingFiles = setOf("conflict.txt"),
            overwriteConfirmed = false
        )

        val summary = plan.summaryLines.joinToString("\n")
        assertTrue(summary.contains("Bereits vorhandene Dateien im Ziel (1):"))
        assertTrue(summary.contains("conflict.txt"))
        assertTrue(summary.contains("Überschreiben ist nicht freigegeben"))
    }

    // ── 7. Progress Tracking & Cancellation ────────────────────────────────

    @Test
    fun `extractionProgress_reportsAccurateCountsAndBytes`() {
        val zipBytes = createZip(
            "a.txt" to text("AAA"),
            "b.txt" to text("BBBBBB")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "progress.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        val progressReports = mutableListOf<ExtractionProgress>()
        val sink = InMemoryZipSink()

        val result = ZipProjectImportPolicy.extract(
            plan = plan,
            stream = ByteArrayInputStream(zipBytes),
            sink = sink,
            onProgress = { progressReports.add(it) }
        )

        assertTrue(result is ExtractionResult.Success)
        assertEquals(2, progressReports.size)
        assertEquals(1, progressReports[0].extractedFiles)
        assertEquals(2, progressReports[1].extractedFiles)
        assertEquals(2, progressReports[1].totalFiles)
        assertEquals("b.txt", progressReports[1].currentEntry)
        assertEquals(100, progressReports[1].percentage)
    }

    @Test
    fun `extractionCancellation_stopsProcessingAndReportsPartialFiles`() {
        val zipBytes = createZip(
            "first.txt" to text("first file"),
            "second.txt" to text("second file"),
            "third.txt" to text("third file")
        )

        val plan = ZipProjectImportPolicy.inspect(
            archiveName = "cancel.zip",
            destinationDirectory = targetFolder,
            stream = ByteArrayInputStream(zipBytes)
        )

        val sink = InMemoryZipSink()
        val token = ZipProjectImportPolicy.ExtractionCancellationToken()

        // Cancel after the first file is extracted
        val result = ZipProjectImportPolicy.extract(
            plan = plan,
            stream = ByteArrayInputStream(zipBytes),
            sink = sink,
            cancellationToken = token,
            onProgress = { progress ->
                if (progress.extractedFiles == 1) {
                    token.cancel()
                }
            }
        )

        assertTrue(result is ExtractionResult.Cancelled)
        val cancelled = result as ExtractionResult.Cancelled
        assertEquals(1, cancelled.extractedCount)
        assertEquals(listOf("first.txt"), cancelled.partiallyExtractedFiles)
        assertTrue(sink.files.containsKey("first.txt"))
        assertFalse(sink.files.containsKey("second.txt"))
        assertFalse(sink.files.containsKey("third.txt"))
    }

    // ── 8. Dynamic Byte Limiter & Path Normalization ────────────────────────

    @Test
    fun `dynamicByteLimiter_catchesStreamingDecompressionBombs`() {
        val zipBytes = createZip(
            "stream_big.bin" to ByteArray(5000) { 0x55 }
        )

        // Simulate plan with deceptive or unmeasured sizes, but runtime limit is low
        val plan = ZipInspectionPlan(
            archiveName = "deceptive.zip",
            destinationDirectory = targetFolder,
            totalEntries = 1,
            fileCount = 1,
            directoryCount = 0,
            totalCompressedBytes = 100L,
            totalUncompressedBytes = 2000L, // Claims 2000 bytes
            entries = listOf(
                ZipProjectImportPolicy.ZipEntryMetadata(
                    rawPath = "stream_big.bin",
                    normalizedPath = "stream_big.bin",
                    isDirectory = false,
                    compressedSize = 100L,
                    uncompressedSize = 2000L
                )
            ),
            blockingIssues = emptyList(),
            existingCollisions = emptyList()
        )

        val sink = InMemoryZipSink()
        val tightLimits = ZipImportLimits(maxUncompressedBytes = 2500L) // Payload is 5000 bytes!

        val result = ZipProjectImportPolicy.extract(
            plan = plan,
            stream = ByteArrayInputStream(zipBytes),
            sink = sink,
            limits = tightLimits
        )

        assertTrue(result is ExtractionResult.Failed)
        val failed = result as ExtractionResult.Failed
        assertTrue(failed.error.contains("Sicherheitsabbruch"))
        assertTrue(failed.error.contains("überschritten"))
    }

    @Test
    fun `normalizeEntryPath_handlesComplexRelativeDotsAndSlashes`() {
        assertEquals("src/Main.kt", ZipProjectImportPolicy.normalizeEntryPath("src/./Main.kt"))
        assertEquals("src/Main.kt", ZipProjectImportPolicy.normalizeEntryPath("src\\Main.kt"))
        assertEquals("src/Main.kt", ZipProjectImportPolicy.normalizeEntryPath("/src/Main.kt"))
        assertEquals("src/Main.kt", ZipProjectImportPolicy.normalizeEntryPath(".//src//Main.kt"))
        assertEquals("", ZipProjectImportPolicy.normalizeEntryPath("./"))
    }
}
