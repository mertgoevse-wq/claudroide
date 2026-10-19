package org.claudroide.app.feature.linux.recovery

import java.io.File
import java.io.InputStream

/**
 * Interface abstracting partition discovery and reading from target hardware.
 * Enables 100% hardware-agnostic simulation, testing, and live execution.
 */
interface PartitionSource {

    /**
     * Inspects current connectivity state to the target device.
     */
    fun inspectConnection(): TargetConnectionState

    /**
     * Retrieves detailed metadata about the target device.
     */
    fun getDeviceInfo(): TargetDeviceInfo

    /**
     * Discovers all readable partitions on the target device.
     */
    fun getAvailablePartitions(): List<TargetPartitionInfo>

    /**
     * Opens an input stream to read the raw partition image bytes.
     * Implementations may report read progress via [onProgress].
     */
    fun dumpPartition(
        partition: TargetPartitionInfo,
        onProgress: (bytesRead: Long) -> Unit = {}
    ): InputStream

    /**
     * Checks available free space in bytes at the specified destination directory.
     */
    fun checkHostFreeSpace(destinationDir: File): Long {
        return destinationDir.usableSpace
    }
}
