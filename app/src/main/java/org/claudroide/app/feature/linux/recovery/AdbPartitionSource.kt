package org.claudroide.app.feature.linux.recovery

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Live ADB partition source executing read-only inspection and partition extraction
 * over USB, USB tethering, or ADB over TCP.
 *
 * STRICT SAFETY RULE: Only reads from the device; performs zero destructive operations.
 */
class AdbPartitionSource(
    private val adbPath: String = "adb",
    private val deviceSerial: String? = null
) : PartitionSource {

    private fun buildAdbCommand(vararg args: String): List<String> {
        val cmd = mutableListOf(adbPath)
        if (!deviceSerial.isNullOrBlank()) {
            cmd.add("-s")
            cmd.add(deviceSerial)
        }
        cmd.addAll(args)
        return cmd
    }

    private fun runAdb(vararg args: String, timeoutSec: Long = 10L): Pair<Int, String> {
        return try {
            val process = ProcessBuilder(buildAdbCommand(*args))
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                Pair(-1, "Timeout after ${timeoutSec}s executing adb ${args.joinToString(" ")}")
            } else {
                Pair(process.exitValue(), output.trim())
            }
        } catch (e: Exception) {
            Pair(-1, e.message ?: "Execution failed")
        }
    }

    override fun inspectConnection(): TargetConnectionState {
        val (code, output) = runAdb("get-state")
        if (code != 0) {
            // Check if device is visible in recovery or fastboot
            val (_, devicesOut) = runAdb("devices")
            return when {
                devicesOut.contains("unauthorized") -> TargetConnectionState.UNAUTHORIZED
                devicesOut.contains("recovery") -> TargetConnectionState.RECOVERY_ONLINE
                devicesOut.contains("device") -> TargetConnectionState.DEVICE_ONLINE
                else -> TargetConnectionState.DISCONNECTED
            }
        }

        return when (output.lowercase()) {
            "device" -> {
                val (rootCode, rootOut) = runAdb("shell", "id -u")
                if (rootCode == 0 && rootOut.trim() == "0") {
                    TargetConnectionState.DEVICE_ONLINE
                } else {
                    TargetConnectionState.NON_ROOT
                }
            }
            "recovery" -> TargetConnectionState.RECOVERY_ONLINE
            "sideload" -> TargetConnectionState.RECOVERY_ONLINE
            else -> TargetConnectionState.DISCONNECTED
        }
    }

    override fun getDeviceInfo(): TargetDeviceInfo {
        val (_, codename) = runAdb("shell", "getprop", "ro.product.device")
        val (_, model) = runAdb("shell", "getprop", "ro.product.model")
        val (_, androidVer) = runAdb("shell", "getprop", "ro.build.version.release")
        val (_, rom) = runAdb("shell", "getprop", "ro.modversion")
        val (_, slot) = runAdb("shell", "getprop", "ro.boot.slot_suffix")
        val (rootCode, rootOut) = runAdb("shell", "id -u")
        val isRoot = rootCode == 0 && rootOut.trim() == "0"

        // Battery probe via dumpsys battery
        val (_, batteryOut) = runAdb("shell", "dumpsys", "battery")
        val level = batteryOut.lines()
            .firstOrNull { it.trim().startsWith("level:") }
            ?.substringAfter(":")?.trim()?.toIntOrNull() ?: 100
        val powered = batteryOut.lines()
            .any { line ->
                val l = line.trim().lowercase()
                (l.startsWith("ac powered:") || l.startsWith("usb powered:") || l.startsWith("wireless powered:")) &&
                        l.endsWith("true")
            }

        val activeSlot = slot.trim().removePrefix("_").takeIf { it.isNotBlank() } ?: "a"
        val currentRom = if (rom.isNotBlank()) rom.trim() else "LineageOS"

        return TargetDeviceInfo(
            deviceId = if (codename.isNotBlank()) codename.trim() else "oneplus-6t",
            codename = if (codename.isNotBlank()) codename.trim() else "fajita",
            model = if (model.isNotBlank()) model.trim() else "OnePlus 6T (A6013)",
            serialNumber = deviceSerial,
            androidVersion = if (androidVer.isNotBlank()) androidVer.trim() else "unknown",
            currentRom = currentRom,
            activeSlot = activeSlot,
            isRoot = isRoot,
            batteryPercent = level,
            isCharging = powered,
            connectionState = inspectConnection()
        )
    }

    override fun getAvailablePartitions(): List<TargetPartitionInfo> {
        val partitions = mutableListOf<TargetPartitionInfo>()
        val script = """
            if [ -d /dev/block/bootdevice/by-name ]; then
                ls -l /dev/block/bootdevice/by-name/
            elif [ -d /dev/block/by-name ]; then
                ls -l /dev/block/by-name/
            fi
        """.trimIndent()

        val (code, output) = runAdb("shell", script)
        if (code == 0) {
            val criticalEfs = setOf("modemst1", "modemst2", "fsc", "fsg", "efs")
            for (line in output.lines()) {
                val parts = line.split("->").map { it.trim() }
                if (parts.size == 2) {
                    val linkName = parts[0].substringAfterLast(" ").trim()
                    val targetPath = parts[1].trim()
                    if (linkName.isNotBlank()) {
                        val isEfs = linkName.lowercase() in criticalEfs
                        partitions.add(
                            TargetPartitionInfo(
                                partitionName = linkName,
                                blockPath = targetPath,
                                sizeBytes = if (isEfs) 2048L * 1024L else 0L,
                                isEfsCritical = isEfs
                            )
                        )
                    }
                }
            }
        }
        return partitions
    }

    override fun dumpPartition(
        partition: TargetPartitionInfo,
        onProgress: (bytesRead: Long) -> Unit
    ): InputStream {
        val blockPath = partition.blockPath.ifBlank {
            "/dev/block/bootdevice/by-name/${partition.partitionName}"
        }
        val process = ProcessBuilder(
            buildAdbCommand("exec-out", "dd", "if=$blockPath", "bs=4096", "2>/dev/null")
        ).start()

        val rawStream = process.inputStream
        return object : InputStream() {
            private var bytesReadTotal = 0L

            override fun read(): Int {
                val b = rawStream.read()
                if (b != -1) {
                    bytesReadTotal++
                    onProgress(bytesReadTotal)
                }
                return b
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val count = rawStream.read(b, off, len)
                if (count > 0) {
                    bytesReadTotal += count
                    onProgress(bytesReadTotal)
                }
                return count
            }

            override fun close() {
                try {
                    rawStream.close()
                } finally {
                    process.destroy()
                }
            }
        }
    }
}
