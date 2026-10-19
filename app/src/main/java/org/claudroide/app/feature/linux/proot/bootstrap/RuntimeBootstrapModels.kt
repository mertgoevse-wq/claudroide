package org.claudroide.app.feature.linux.proot.bootstrap

import java.io.File

/**
 * CPU architectures supported by the Mobile Linux Lab runtime.
 */
enum class SupportedAbi(val value: String) {
    ARM64_V8A("arm64-v8a"),
    ARMEABI_V7A("armeabi-v7a"),
    X86_64("x86_64"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(str: String): SupportedAbi {
            val lower = str.lowercase()
            return when {
                lower.contains("aarch64") || lower.contains("arm64") -> ARM64_V8A
                lower.contains("armv7") || lower.contains("armeabi") || lower.contains("arm") -> ARMEABI_V7A
                lower.contains("x86_64") || lower.contains("amd64") -> X86_64
                else -> UNKNOWN
            }
        }

        fun currentDeviceAbi(): SupportedAbi {
            val arch = System.getProperty("os.arch") ?: ""
            return fromString(arch)
        }
    }
}

/**
 * Metadata for a downloadable Linux userland runtime bundle (PRoot and helpers).
 */
data class RuntimeAsset(
    val name: String,
    val version: String,
    val abi: SupportedAbi,
    val url: String,
    val sha256: String,
    val executableRelativePath: String = "bin/proot"
)

/**
 * Lifecycle state of the PRoot runtime engine.
 */
sealed class RuntimeStatus {
    object NotInstalled : RuntimeStatus()
    data class Downloading(val progress: Float) : RuntimeStatus()
    object Verifying : RuntimeStatus()
    object Installing : RuntimeStatus()
    data class Ready(
        val binaryFile: File,
        val version: String,
        val abi: SupportedAbi
    ) : RuntimeStatus()
    data class Failed(val error: String) : RuntimeStatus()
}

/**
 * Interface for downloading runtime artifacts, allowing pluggable mock/network implementations.
 */
interface RuntimeAssetDownloader {
    suspend fun download(
        asset: RuntimeAsset,
        destination: File,
        onProgress: (Float) -> Unit
    ): Boolean
}
