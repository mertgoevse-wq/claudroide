package org.claudroide.app.feature.linux.remote.discovery

import java.io.File

/**
 * Entry from the kernel ARP table (/proc/net/arp).
 */
data class ArpEntry(
    val ipAddress: String,
    val hwType: String,
    val flags: String,
    val hwAddress: String,
    val mask: String,
    val device: String
) {
    val isComplete: Boolean
        get() = hwAddress.isNotBlank() && hwAddress != "00:00:00:00:00:00"
}

interface ArpTableReader {
    fun readEntries(): List<ArpEntry>
}

class ProcNetArpReader(private val arpPath: String = "/proc/net/arp") : ArpTableReader {
    override fun readEntries(): List<ArpEntry> {
        val file = File(arpPath)
        if (!file.exists() || !file.canRead()) {
            return emptyList()
        }

        return try {
            val lines = file.readLines()
            if (lines.size <= 1) return emptyList()

            // Header line: IP address       HW type     Flags       HW address            Mask     Device
            lines.drop(1).mapNotNull { line ->
                val tokens = line.trim().split("\\s+".toRegex())
                if (tokens.size >= 6) {
                    ArpEntry(
                        ipAddress = tokens[0],
                        hwType = tokens[1],
                        flags = tokens[2],
                        hwAddress = tokens[3],
                        mask = tokens[4],
                        device = tokens[5]
                    )
                } else null
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
