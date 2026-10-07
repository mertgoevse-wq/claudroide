package org.claudroide.app.feature.linux.recovery

import org.claudroide.app.feature.linux.distro.ChecksumVerifier
import java.io.File

/**
 * Unified facade for recovery operations, backup verification, and safety gates.
 */
class RecoveryManager {

    val safetyGate: SafetyGateManager = SafetyGateManager(this::verifyBackupIntegrity)

    /**
     * Verifies that all partition images listed in the manifest exist in backupDir,
     * match their expected byte length, and match their SHA-256 (or SHA-512) digests.
     */
    fun verifyBackupIntegrity(backupDir: File, manifest: BackupManifest): Boolean {
        if (!backupDir.exists() || !backupDir.isDirectory) return false
        if (!manifest.isEfsComplete) return false

        for (entry in manifest.partitions) {
            val imageFile = File(backupDir, entry.imageFileName)
            if (!imageFile.exists()) return false
            if (imageFile.length() != entry.sizeBytes && entry.sizeBytes > 0L) return false

            // SHA-512 (when recorded) is authoritative; SHA-256 is the baseline.
            val sha512 = entry.sha512Checksum?.trim().orEmpty()
            val verified = if (sha512.isNotEmpty()) {
                ChecksumVerifier.verify(imageFile, sha512)
            } else {
                ChecksumVerifier.verify(imageFile, entry.sha256Checksum)
            }
            if (!verified) return false
        }
        return true
    }

    /**
     * Asserts that explicit human approval has been recorded before executing an action.
     */
    fun assertSafetyGateApproved(isExplicitlyApproved: Boolean, actionName: String) {
        if (!isExplicitlyApproved) {
            throw SecurityException(
                "SAFETY GATE VIOLATION: Operation '$actionName' requires explicit human approval recorded before execution."
            )
        }
    }

    /**
     * Scans a directory for existing backup directories containing valid backup_manifest.json files.
     */
    fun findBackups(baseDir: File): List<Pair<File, BackupManifest>> {
        if (!baseDir.exists() || !baseDir.isDirectory) return emptyList()
        val results = mutableListOf<Pair<File, BackupManifest>>()

        baseDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val manifestFile = File(dir, "backup_manifest.json")
                if (manifestFile.exists() && manifestFile.length() > 0) {
                    try {
                        val manifest = BackupManifest.fromJson(manifestFile.readText())
                        results.add(Pair(dir, manifest))
                    } catch (_: Exception) {
                        // Ignore corrupt manifest in search
                    }
                }
            }
        }
        return results.sortedByDescending { it.second.createdAt }
    }

    /**
     * Creates a staged backup engine for the given partition source.
     */
    fun createEngine(partitionSource: PartitionSource): StagedBackupEngine {
        return StagedBackupEngine(partitionSource, this)
    }
}
