package org.claudroide.app.feature.linux.remote.ssh

import java.security.MessageDigest
import java.util.Base64

enum class HostKeyType {
    ED25519,
    RSA,
    ECDSA,
    UNKNOWN
}

data class HostKeyFingerprint(
    val keyType: HostKeyType,
    val rawKey: ByteArray,
    val sha256Fingerprint: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HostKeyFingerprint) return false
        return keyType == other.keyType && sha256Fingerprint == other.sha256Fingerprint
    }

    override fun hashCode(): Int {
        var result = keyType.hashCode()
        result = 31 * result + sha256Fingerprint.hashCode()
        return result
    }

    companion object {
        fun fromRawKey(keyType: HostKeyType, keyBytes: ByteArray): HostKeyFingerprint {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(keyBytes)
            val base64 = Base64.getEncoder().withoutPadding().encodeToString(digest)
            val fingerprint = "SHA256:$base64"
            return HostKeyFingerprint(keyType, keyBytes, fingerprint)
        }

        fun parseAlgorithm(name: String): HostKeyType {
            return when {
                name.contains("ed25519", ignoreCase = true) -> HostKeyType.ED25519
                name.contains("rsa", ignoreCase = true) -> HostKeyType.RSA
                name.contains("ecdsa", ignoreCase = true) -> HostKeyType.ECDSA
                else -> HostKeyType.UNKNOWN
            }
        }
    }
}
