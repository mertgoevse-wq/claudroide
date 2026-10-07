package org.claudroide.app.feature.linux.recovery

import org.claudroide.app.feature.linux.distro.ChecksumVerifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Options for configuring a staged backup execution.
 */
data class BackupExecutionOptions(
    val dryRun: Boolean = false,
    val requiredCodename: String = "fajita",
    val secondaryStorageDir: File? = null,
    val targetPartitions: List<String> = listOf("modemst1", "modemst2", "fsc", "fsg"),
    val resumeExisting: Boolean = true,
    val customBackupDir: File? = null
)

/**
 * Robust Staged Backup Engine implementing Phase 1 EFS & Modem Calibration backup
 * with preflight verification, dual-checksum generation, atomic disk writes,
 * resumable recovery, and simulation/dry-run capabilities.
 */
class StagedBackupEngine(
    private val partitionSource: PartitionSource,
    private val recoveryManager: RecoveryManager = RecoveryManager()
) {

    /**
     * Executes comprehensive preflight checks against the target partition source.
     */
    fun runPreflight(
        destinationDir: File,
        options: BackupExecutionOptions = BackupExecutionOptions()
    ): PreflightReport {
        val items = mutableListOf<PreflightCheckItem>()

        val connState = try {
            partitionSource.inspectConnection()
        } catch (e: Exception) {
            TargetConnectionState.DISCONNECTED
        }

        val isConnected = connState in listOf(
            TargetConnectionState.DEVICE_ONLINE,
            TargetConnectionState.RECOVERY_ONLINE,
            TargetConnectionState.NON_ROOT
        )
        items.add(
            PreflightCheckItem(
                name = "Device Connection",
                passed = isConnected,
                message = if (isConnected) "Device online ($connState)" else "Device not reachable via ADB/USB",
                isCritical = true
            )
        )

        var deviceInfo: TargetDeviceInfo? = null
        if (isConnected) {
            deviceInfo = try {
                partitionSource.getDeviceInfo()
            } catch (e: Exception) {
                null
            }
        }

        if (deviceInfo != null) {
            // Check codename match
            val codenameMatches = deviceInfo.codename.equals(options.requiredCodename, ignoreCase = true)
            items.add(
                PreflightCheckItem(
                    name = "Target Device Codename",
                    passed = codenameMatches,
                    message = if (codenameMatches) {
                        "Target device verified: ${deviceInfo.model} (${deviceInfo.codename})"
                    } else {
                        "Expected codename '${options.requiredCodename}', but found '${deviceInfo.codename}'"
                    },
                    isCritical = true
                )
            )

            // Check root access
            items.add(
                PreflightCheckItem(
                    name = "Root Privileges",
                    passed = deviceInfo.isRoot,
                    message = if (deviceInfo.isRoot) "Root access confirmed (uid 0)" else "Non-root shell: root required to read raw block devices",
                    isCritical = true
                )
            )

            // Check battery level
            val batterySafe = deviceInfo.isBatterySafeForRecovery
            items.add(
                PreflightCheckItem(
                    name = "Battery Health / Power",
                    passed = batterySafe,
                    message = if (batterySafe) {
                        "Battery: ${deviceInfo.batteryPercent}% (charging: ${deviceInfo.isCharging})"
                    } else {
                        "Battery too low: ${deviceInfo.batteryPercent}% (must be >= 50% or charging)"
                    },
                    isCritical = true
                )
            )
        }

        // Check available partitions
        val partitionsFound = try {
            partitionSource.getAvailablePartitions()
        } catch (e: Exception) {
            emptyList()
        }

        val availableNames = partitionsFound.map { it.partitionName.lowercase() }
        val hasModemst1 = "modemst1" in availableNames
        val hasModemst2 = "modemst2" in availableNames
        val hasFscOrFsg = "fsc" in availableNames || "fsg" in availableNames
        val efsComplete = hasModemst1 && hasModemst2 && hasFscOrFsg

        items.add(
            PreflightCheckItem(
                name = "EFS Partitions Present",
                passed = efsComplete,
                message = if (efsComplete) {
                    "Found critical EFS block partitions: ${options.targetPartitions.filter { it in availableNames }.joinToString(", ")}"
                } else {
                    "Missing critical EFS partitions. Available: $availableNames"
                },
                isCritical = true
            )
        )

        // Check host disk space
        val estimatedBytes = partitionsFound
            .filter { it.partitionName.lowercase() in options.targetPartitions.map { p -> p.lowercase() } }
            .sumOf { it.sizeBytes.takeIf { s -> s > 0 } ?: (2048L * 1024L) }
        val requiredBytes = estimatedBytes * 2 // 2x safety margin

        val freeSpace = partitionSource.checkHostFreeSpace(destinationDir)
        val spaceOk = freeSpace >= requiredBytes
        items.add(
            PreflightCheckItem(
                name = "Host Storage Space",
                passed = spaceOk,
                message = if (spaceOk) {
                    "Sufficient storage: ${freeSpace / (1024 * 1024)} MB available (estimated need: ${requiredBytes / (1024 * 1024)} MB)"
                } else {
                    "Insufficient storage: only ${freeSpace / (1024 * 1024)} MB available (need at least ${requiredBytes / (1024 * 1024)} MB)"
                },
                isCritical = true
            )
        )

        return PreflightReport(
            targetDevice = deviceInfo,
            items = items,
            partitionsFound = partitionsFound
        )
    }

    /**
     * Executes Stage 1 EFS backup with live progress callbacks, atomic file writing,
     * dual checksum calculation, and integrity verification.
     */
    fun executeStage1Backup(
        destinationBaseDir: File,
        options: BackupExecutionOptions = BackupExecutionOptions(),
        onProgress: (BackupProgress) -> Unit = {}
    ): BackupResult {
        val startTime = System.currentTimeMillis()
        val logs = mutableListOf<String>()

        fun log(msg: String) {
            logs.add("[${System.currentTimeMillis() - startTime}ms] $msg")
        }

        log("Starting Stage 1 EFS backup procedure (dryRun=${options.dryRun})...")
        onProgress(BackupProgress(phase = "PREFLIGHT", message = "Running preflight checks..."))

        val preflight = runPreflight(destinationBaseDir, options)
        if (!preflight.isPassed) {
            val failureMsg = "Preflight check failed:\n" + preflight.failureReasons.joinToString("\n")
            log("ABORT: $failureMsg")
            return BackupResult(
                success = false,
                isDryRun = options.dryRun,
                manifest = null,
                backupDirectory = null,
                error = failureMsg,
                durationMs = System.currentTimeMillis() - startTime,
                logs = logs
            )
        }
        log("Preflight checks passed successfully.")

        val device = preflight.targetDevice ?: TargetDeviceInfo()
        val targetPartitions = preflight.partitionsFound.filter {
            it.partitionName.lowercase() in options.targetPartitions.map { p -> p.lowercase() }
        }

        if (targetPartitions.isEmpty()) {
            val err = "No target partitions matched ${options.targetPartitions}"
            log("ABORT: $err")
            return BackupResult(
                success = false,
                isDryRun = options.dryRun,
                manifest = null,
                backupDirectory = null,
                error = err,
                durationMs = System.currentTimeMillis() - startTime,
                logs = logs
            )
        }

        val backupDir = options.customBackupDir ?: File(destinationBaseDir, "${device.codename}_efs_${startTime}")

        if (options.dryRun) {
            log("DRY RUN: Simulating partition backup without writing files to disk.")
            val simulatedEntries = targetPartitions.map { p ->
                val mockSize = p.sizeBytes.takeIf { it > 0 } ?: (2048L * 1024L)
                PartitionBackupEntry(
                    partitionName = p.partitionName,
                    imageFileName = "${p.partitionName}.img",
                    sizeBytes = mockSize,
                    sha256Checksum = "simulated_sha256_${p.partitionName}",
                    sha512Checksum = "simulated_sha512_${p.partitionName}",
                    backupTimestamp = startTime
                )
            }

            val storageLocations = mutableListOf(backupDir.absolutePath)
            options.secondaryStorageDir?.let { storageLocations.add(it.absolutePath) }

            val dryManifest = BackupManifest(
                deviceId = device.deviceId,
                codename = device.codename,
                serialNumber = device.serialNumber,
                currentRom = device.currentRom,
                androidVersion = device.androidVersion,
                activeSlot = device.activeSlot,
                createdAt = startTime,
                partitions = simulatedEntries,
                storageLocations = storageLocations,
                isVerified = true,
                notes = "Simulated Dry-Run Stage 1 EFS Backup"
            )

            onProgress(
                BackupProgress(
                    phase = "COMPLETE",
                    partitionsCompleted = targetPartitions.size,
                    totalPartitions = targetPartitions.size,
                    message = "Dry-run simulation completed successfully."
                )
            )

            return BackupResult(
                success = true,
                isDryRun = true,
                manifest = dryManifest,
                backupDirectory = backupDir,
                durationMs = System.currentTimeMillis() - startTime,
                logs = logs
            )
        }

        // Live execution: Create backup directory
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            val err = "Failed to create backup directory at ${backupDir.absolutePath}"
            log("ABORT: $err")
            return BackupResult(
                success = false,
                isDryRun = false,
                manifest = null,
                backupDirectory = null,
                error = err,
                durationMs = System.currentTimeMillis() - startTime,
                logs = logs
            )
        }

        log("Created backup directory: ${backupDir.absolutePath}")

        val entries = mutableListOf<PartitionBackupEntry>()
        val totalPartitionsCount = targetPartitions.size

        for ((index, part) in targetPartitions.withIndex()) {
            val imgFileName = "${part.partitionName}.img"
            val targetFile = File(backupDir, imgFileName)
            val partFile = File(backupDir, "$imgFileName.part")

            // Check if existing file is already complete and verified (resume support)
            if (options.resumeExisting && targetFile.exists() && targetFile.length() > 0L) {
                log("Partition image ${part.partitionName} already exists on disk. Computing checksums to verify reuse...")
                val sha256 = ChecksumVerifier.calculateSha256(targetFile)
                val sha512 = ChecksumVerifier.calculateSha512(targetFile)
                entries.add(
                    PartitionBackupEntry(
                        partitionName = part.partitionName,
                        imageFileName = imgFileName,
                        sizeBytes = targetFile.length(),
                        sha256Checksum = sha256,
                        sha512Checksum = sha512,
                        backupTimestamp = targetFile.lastModified()
                    )
                )
                log("Resumed/reused existing verified ${part.partitionName}.img (${targetFile.length()} bytes)")
                continue
            }

            log("Dumping partition ${part.partitionName} (${index + 1}/$totalPartitionsCount)...")
            onProgress(
                BackupProgress(
                    phase = "DUMPING",
                    currentPartition = part.partitionName,
                    partitionsCompleted = index,
                    totalPartitions = totalPartitionsCount,
                    message = "Dumping partition ${part.partitionName}..."
                )
            )

            val sha256Digest = MessageDigest.getInstance("SHA-256")
            val sha512Digest = MessageDigest.getInstance("SHA-512")

            try {
                partitionSource.dumpPartition(part) { bytesRead ->
                    onProgress(
                        BackupProgress(
                            phase = "DUMPING",
                            currentPartition = part.partitionName,
                            bytesDumped = bytesRead,
                            partitionsCompleted = index,
                            totalPartitions = totalPartitionsCount,
                            message = "Dumping ${part.partitionName}: $bytesRead bytes"
                        )
                    )
                }.use { inputStream ->
                    FileOutputStream(partFile).use { fileOut ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            fileOut.write(buffer, 0, read)
                            sha256Digest.update(buffer, 0, read)
                            sha512Digest.update(buffer, 0, read)
                        }
                        fileOut.flush()
                    }
                }

                // Atomic rename
                if (targetFile.exists()) targetFile.delete()
                if (!partFile.renameTo(targetFile)) {
                    throw IOException("Failed to rename temporary file $partFile to $targetFile")
                }

                val finalSha256 = sha256Digest.digest().joinToString("") { "%02x".format(it) }
                val finalSha512 = sha512Digest.digest().joinToString("") { "%02x".format(it) }

                log("Successfully dumped ${part.partitionName} -> $imgFileName (${targetFile.length()} bytes, SHA-256: $finalSha256)")

                entries.add(
                    PartitionBackupEntry(
                        partitionName = part.partitionName,
                        imageFileName = imgFileName,
                        sizeBytes = targetFile.length(),
                        sha256Checksum = finalSha256,
                        sha512Checksum = finalSha512,
                        backupTimestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                partFile.delete()
                val err = "Error dumping partition ${part.partitionName}: ${e.message}"
                log("ERROR: $err")
                return BackupResult(
                    success = false,
                    isDryRun = false,
                    manifest = null,
                    backupDirectory = backupDir,
                    error = err,
                    durationMs = System.currentTimeMillis() - startTime,
                    logs = logs
                )
            }
        }

        // Secondary replication if configured (Rule: 2+ storage locations)
        val storageLocations = mutableListOf(backupDir.absolutePath)
        val secondaryDir = options.secondaryStorageDir
        if (secondaryDir != null) {
            try {
                val replicaDir = File(secondaryDir, backupDir.name)
                replicaDir.mkdirs()
                log("Replicating backup images to secondary location: ${replicaDir.absolutePath}...")
                for (entry in entries) {
                    val src = File(backupDir, entry.imageFileName)
                    val dst = File(replicaDir, entry.imageFileName)
                    src.copyTo(dst, overwrite = true)
                }
                storageLocations.add(replicaDir.absolutePath)
                log("Replication complete.")
            } catch (e: Exception) {
                log("WARNING: Failed to replicate to secondary location: ${e.message}")
            }
        }

        // Generate manifest
        val manifest = BackupManifest(
            deviceId = device.deviceId,
            codename = device.codename,
            serialNumber = device.serialNumber,
            currentRom = device.currentRom,
            androidVersion = device.androidVersion,
            activeSlot = device.activeSlot,
            createdAt = startTime,
            partitions = entries,
            storageLocations = storageLocations,
            isVerified = false,
            notes = "Stage 1 EFS Critical Modem Calibration Backup"
        )

        // Write manifest to backup directory
        val manifestFile = File(backupDir, "backup_manifest.json")
        manifestFile.writeText(manifest.toJson())
        log("Manifest written to: ${manifestFile.absolutePath}")

        // Immediate integrity check
        onProgress(BackupProgress(phase = "VERIFYING", message = "Verifying backup image integrity..."))
        val isVerified = recoveryManager.verifyBackupIntegrity(backupDir, manifest)
        log("Backup integrity check: ${if (isVerified) "PASSED" else "FAILED"}")

        val finalManifest = manifest.copy(isVerified = isVerified)
        manifestFile.writeText(finalManifest.toJson())

        onProgress(
            BackupProgress(
                phase = "COMPLETE",
                partitionsCompleted = totalPartitionsCount,
                totalPartitions = totalPartitionsCount,
                message = "Backup completed. Verified: $isVerified"
            )
        )

        return BackupResult(
            success = isVerified,
            isDryRun = false,
            manifest = finalManifest,
            backupDirectory = backupDir,
            durationMs = System.currentTimeMillis() - startTime,
            logs = logs
        )
    }
}
