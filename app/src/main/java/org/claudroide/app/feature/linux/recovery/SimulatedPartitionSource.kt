package org.claudroide.app.feature.linux.recovery

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeoutException

/**
 * Fault injection options for SimulatedPartitionSource.
 */
sealed class SimulatedFault {
    object None : SimulatedFault()
    data class DisconnectMidStream(val bytesBeforeDisconnect: Long = 1024L) : SimulatedFault()
    object CorruptStreamBytes : SimulatedFault()
    object NonRootAccess : SimulatedFault()
    object LowBattery : SimulatedFault()
    object WrongDeviceModel : SimulatedFault()
    object MissingEfsPartitions : SimulatedFault()
    data class InsufficientStorage(val availableBytes: Long = 1024L) : SimulatedFault()
    object CommandTimeout : SimulatedFault()
}

/**
 * High-fidelity simulated partition source for testing and dry-run execution
 * without requiring a physically connected OnePlus 6T.
 */
class SimulatedPartitionSource(
    private var fault: SimulatedFault = SimulatedFault.None,
    private val simulatedHostFreeSpace: Long? = null
) : PartitionSource {

    var currentDeviceInfo: TargetDeviceInfo = TargetDeviceInfo(
        deviceId = "oneplus-6t",
        codename = "fajita",
        model = "OnePlus 6T (A6013)",
        serialNumber = "FAJITA-SIM-001",
        androidVersion = "14",
        currentRom = "LineageOS 21.0",
        activeSlot = "a",
        isRoot = true,
        batteryPercent = 88,
        isCharging = true,
        connectionState = TargetConnectionState.DEVICE_ONLINE
    )

    private val partitionSizes = mapOf(
        "modemst1" to 2048L * 1024L, // 2 MB
        "modemst2" to 2048L * 1024L, // 2 MB
        "fsc" to 1024L * 1024L,      // 1 MB
        "fsg" to 2048L * 1024L,      // 2 MB
        "boot_a" to 64L * 1024L * 1024L,
        "boot_b" to 64L * 1024L * 1024L
    )

    fun setFault(newFault: SimulatedFault) {
        this.fault = newFault
    }

    override fun inspectConnection(): TargetConnectionState {
        return when (fault) {
            is SimulatedFault.NonRootAccess -> TargetConnectionState.NON_ROOT
            is SimulatedFault.CommandTimeout -> throw TimeoutException("Simulated ADB communication timeout")
            else -> currentDeviceInfo.connectionState
        }
    }

    override fun getDeviceInfo(): TargetDeviceInfo {
        return when (fault) {
            is SimulatedFault.WrongDeviceModel -> currentDeviceInfo.copy(
                deviceId = "pixel-7",
                codename = "panther",
                model = "Google Pixel 7"
            )
            is SimulatedFault.LowBattery -> currentDeviceInfo.copy(
                batteryPercent = 14,
                isCharging = false
            )
            is SimulatedFault.NonRootAccess -> currentDeviceInfo.copy(
                isRoot = false,
                connectionState = TargetConnectionState.NON_ROOT
            )
            else -> currentDeviceInfo
        }
    }

    override fun getAvailablePartitions(): List<TargetPartitionInfo> {
        val partitions = mutableListOf<TargetPartitionInfo>()
        val names = if (fault is SimulatedFault.MissingEfsPartitions) {
            listOf("modemst1", "boot_a")
        } else {
            listOf("modemst1", "modemst2", "fsc", "fsg", "boot_a", "boot_b")
        }

        for (name in names) {
            val size = partitionSizes[name] ?: (1024L * 1024L)
            val isEfs = name in listOf("modemst1", "modemst2", "fsc", "fsg")
            partitions.add(
                TargetPartitionInfo(
                    partitionName = name,
                    blockPath = "/dev/block/bootdevice/by-name/$name",
                    sizeBytes = size,
                    isEfsCritical = isEfs
                )
            )
        }
        return partitions
    }

    override fun dumpPartition(
        partition: TargetPartitionInfo,
        onProgress: (bytesRead: Long) -> Unit
    ): InputStream {
        if (fault is SimulatedFault.NonRootAccess) {
            throw SecurityException("Permission denied: raw block access requires root (uid 0)")
        }
        if (fault is SimulatedFault.CommandTimeout) {
            throw TimeoutException("Timed out while waiting for partition block device response")
        }

        // Generate synthetic repeatable content for the partition
        val targetSize = minOf(partition.sizeBytes, 32L * 1024L) // Keep in-memory buffer manageable for test speed
        val data = ByteArray(targetSize.toInt())
        val seed = partition.partitionName.toByteArray()
        for (i in data.indices) {
            data[i] = ((seed[i % seed.size].toInt() + i) and 0xFF).toByte()
        }

        if (fault is SimulatedFault.CorruptStreamBytes) {
            // Invert first 16 bytes to ensure hash mismatch
            for (i in 0 until minOf(16, data.size)) {
                data[i] = (data[i].toInt() xor 0xFF).toByte()
            }
        }

        val baseStream = ByteArrayInputStream(data)

        val faultCurrent = fault
        return if (faultCurrent is SimulatedFault.DisconnectMidStream) {
            object : InputStream() {
                private var bytesServed = 0L

                override fun read(): Int {
                    if (bytesServed >= faultCurrent.bytesBeforeDisconnect) {
                        throw IOException("Simulated USB disconnection mid-transfer at $bytesServed bytes")
                    }
                    val b = baseStream.read()
                    if (b != -1) {
                        bytesServed++
                        onProgress(bytesServed)
                    }
                    return b
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (bytesServed >= faultCurrent.bytesBeforeDisconnect) {
                        throw IOException("Simulated USB disconnection mid-transfer at $bytesServed bytes")
                    }
                    val allowed = minOf(len.toLong(), faultCurrent.bytesBeforeDisconnect - bytesServed).toInt()
                    val count = baseStream.read(b, off, allowed)
                    if (count > 0) {
                        bytesServed += count
                        onProgress(bytesServed)
                    } else if (allowed == 0) {
                        throw IOException("Simulated USB disconnection mid-transfer at $bytesServed bytes")
                    }
                    return count
                }
            }
        } else {
            object : InputStream() {
                private var totalRead = 0L

                override fun read(): Int {
                    val b = baseStream.read()
                    if (b != -1) {
                        totalRead++
                        onProgress(totalRead)
                    }
                    return b
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val count = baseStream.read(b, off, len)
                    if (count > 0) {
                        totalRead += count
                        onProgress(totalRead)
                    }
                    return count
                }
            }
        }
    }

    override fun checkHostFreeSpace(destinationDir: File): Long {
        if (fault is SimulatedFault.InsufficientStorage) {
            return (fault as SimulatedFault.InsufficientStorage).availableBytes
        }
        return simulatedHostFreeSpace ?: destinationDir.usableSpace.takeIf { it > 0 } ?: (10L * 1024L * 1024L * 1024L)
    }
}
