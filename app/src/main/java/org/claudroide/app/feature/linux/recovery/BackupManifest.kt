package org.claudroide.app.feature.linux.recovery

import org.claudroide.app.feature.linux.util.MiniJson

data class PartitionBackupEntry(
    val partitionName: String,
    val imageFileName: String,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val sha512Checksum: String? = null,
    val backupTimestamp: Long = System.currentTimeMillis()
)

data class BackupManifest(
    val manifestVersion: Int = 1,
    val deviceId: String = "oneplus-6t",
    val codename: String = "fajita",
    val serialNumber: String? = null,
    val currentRom: String = "LineageOS",
    val androidVersion: String = "unknown",
    val activeSlot: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val partitions: List<PartitionBackupEntry>,
    val storageLocations: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val notes: String = "Stage 1 EFS Critical Modem Calibration Backup"
) {
    val isEfsComplete: Boolean
        get() {
            val names = partitions.map { it.partitionName.lowercase() }
            return names.contains("modemst1") &&
                   names.contains("modemst2") &&
                   (names.contains("fsc") || names.contains("fsg"))
        }

    fun toJson(): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"manifestVersion\": $manifestVersion,\n")
        sb.append("  \"deviceId\": \"$deviceId\",\n")
        sb.append("  \"codename\": \"$codename\",\n")
        sb.append("  \"serialNumber\": ${if (serialNumber != null) "\"$serialNumber\"" else "null"},\n")
        sb.append("  \"currentRom\": \"$currentRom\",\n")
        sb.append("  \"androidVersion\": \"$androidVersion\",\n")
        sb.append("  \"activeSlot\": ${if (activeSlot != null) "\"$activeSlot\"" else "null"},\n")
        sb.append("  \"createdAt\": $createdAt,\n")
        sb.append("  \"isVerified\": $isVerified,\n")
        sb.append("  \"notes\": \"$notes\",\n")
        sb.append("  \"storageLocations\": [${storageLocations.joinToString(",") { "\"$it\"" }}],\n")
        sb.append("  \"partitions\": [\n")
        partitions.forEachIndexed { idx, p ->
            sb.append("    {\n")
            sb.append("      \"partitionName\": \"${p.partitionName}\",\n")
            sb.append("      \"imageFileName\": \"${p.imageFileName}\",\n")
            sb.append("      \"sizeBytes\": ${p.sizeBytes},\n")
            sb.append("      \"sha256Checksum\": \"${p.sha256Checksum}\",\n")
            sb.append("      \"sha512Checksum\": ${p.sha512Checksum?.let { "\"$it\"" } ?: "null"},\n")
            sb.append("      \"backupTimestamp\": ${p.backupTimestamp}\n")
            sb.append("    }${if (idx < partitions.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    companion object {
        /**
         * Parse a manifest written by toJson() or by the on-device backup scripts
         * (scripts/backup_efs.sh). Handles missing/optional values from either producer.
         */
        fun fromJson(json: String): BackupManifest {
            val map = MiniJson.parse(json.trim()) as? Map<*, *>
                ?: throw IllegalArgumentException("Manifest JSON must be an object")
            val partitionsRaw = map["partitions"] as? List<*>
                ?: throw IllegalArgumentException("Manifest JSON missing 'partitions'")
            val partitions = partitionsRaw.mapNotNull { item ->
                val e = item as? Map<*, *> ?: return@mapNotNull null
                PartitionBackupEntry(
                    partitionName = e["partitionName"] as? String ?: error("partition entry missing partitionName"),
                    imageFileName = e["imageFileName"] as? String ?: error("partition entry missing imageFileName"),
                    sizeBytes = (e["sizeBytes"] as? Number)?.toLong() ?: 0L,
                    sha256Checksum = e["sha256Checksum"] as? String ?: "",
                    sha512Checksum = (e["sha512Checksum"] as? String)?.takeIf { it.isNotBlank() },
                    backupTimestamp = (e["backupTimestamp"] as? Number)?.toLong() ?: 0L
                )
            }
            val storage = map["storageLocations"] as? List<*> ?: emptyList<Any>()
            return BackupManifest(
                manifestVersion = (map["manifestVersion"] as? Number)?.toInt() ?: 1,
                deviceId = map["deviceId"] as? String ?: "oneplus-6t",
                codename = map["codename"] as? String ?: "fajita",
                serialNumber = map["serialNumber"] as? String,
                currentRom = map["currentRom"] as? String ?: "LineageOS",
                androidVersion = map["androidVersion"] as? String ?: "unknown",
                activeSlot = map["activeSlot"] as? String,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                partitions = partitions,
                storageLocations = storage.filterIsInstance<String>(),
                isVerified = map["isVerified"] == true,
                notes = map["notes"] as? String ?: ""
            )
        }
    }
}
