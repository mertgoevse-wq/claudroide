package org.claudroide.app.feature.linux.distro

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Single source of truth for hash verification.
 * Algorithm-aware: derives the hash algorithm from the expected digest length so
 * SHA-256 and SHA-512 digests can be validated through the same call sites.
 */
object ChecksumVerifier {

    fun calculateSha256(file: File): String {
        return calculateHash(file, "SHA-256")
    }

    fun calculateSha512(file: File): String {
        return calculateHash(file, "SHA-512")
    }

    fun calculateSha256(inputStream: InputStream): String {
        return calculateHash(inputStream, "SHA-256")
    }

    fun verify(file: File, expectedHash: String, algorithm: String? = null): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val expected = expectedHash.trim()
        if (expected.isEmpty()) return false
        val resolvedAlgorithm = algorithm ?: algorithmFor(expected)
        val actualHash = calculateHash(file, resolvedAlgorithm)
        return actualHash.equals(expected, ignoreCase = true)
    }

    /**
     * Verify a downloaded image using the strongest digest the catalog ships for it.
     * SHA-512 is preferred when present; SHA-256 is the baseline. An image with no
     * usable digest can never verify.
     */
    fun verifyDistroImage(file: File, image: DistroImage): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val sha512 = image.sha512Checksum?.trim().orEmpty()
        if (sha512.isNotEmpty()) return verify(file, sha512)
        val sha256 = image.sha256Checksum.trim()
        if (sha256.isNotEmpty()) return verify(file, sha256)
        return false
    }

    private fun algorithmFor(digest: String): String = when (digest.length) {
        128 -> "SHA-512"
        64 -> "SHA-256"
        else -> throw IllegalArgumentException(
            "Unsupported digest length ${digest.length} (expected 64 hex chars for SHA-256 or 128 for SHA-512)"
        )
    }

    private fun calculateHash(file: File, algorithm: String): String {
        return file.inputStream().use { stream ->
            calculateHash(stream, algorithm)
        }
    }

    private fun calculateHash(stream: InputStream, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
