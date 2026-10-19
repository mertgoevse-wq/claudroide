package org.claudroide.app.feature.linux.env.migration

sealed class CompatibilityResult {
    data class Compatible(val message: String = "Architectures are fully compatible") : CompatibilityResult()
    data class RequiresEmulation(val reason: String) : CompatibilityResult()
    data class Incompatible(val reason: String) : CompatibilityResult()
}

object ArchCompatibility {

    fun canonicalize(arch: String): String {
        return when (arch.trim().lowercase()) {
            "arm64", "aarch64", "arm64-v8a" -> "arm64"
            "arm", "armv7", "armv7l", "armeabi-v7a", "armeabi" -> "arm"
            "x86_64", "amd64" -> "x86_64"
            "x86", "i386", "i686" -> "x86"
            "riscv64" -> "riscv64"
            else -> arch.trim().lowercase()
        }
    }

    fun check(sourceArch: String, targetArch: String): CompatibilityResult {
        val src = canonicalize(sourceArch)
        val dst = canonicalize(targetArch)

        if (src == dst) {
            return CompatibilityResult.Compatible("Direct native binary execution supported for $src")
        }

        // 64-bit to 32-bit ARM is fundamentally impossible without full CPU emulation
        if (src == "arm64" && dst == "arm") {
            return CompatibilityResult.Incompatible("64-bit ARM ($sourceArch) binaries cannot execute on 32-bit ARM ($targetArch) host")
        }

        // 32-bit ARM container on 64-bit host
        if (src == "arm" && dst == "arm64") {
            return CompatibilityResult.RequiresEmulation("32-bit ARM container on 64-bit ARM host requires 32-bit multilib or QEMU user emulation")
        }

        // Cross-architecture (e.g. x86_64 <-> arm64)
        return CompatibilityResult.RequiresEmulation("Cross-architecture execution ($sourceArch -> $targetArch) requires QEMU user emulation")
    }
}
