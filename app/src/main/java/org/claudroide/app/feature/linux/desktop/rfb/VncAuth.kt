package org.claudroide.app.feature.linux.desktop.rfb

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * VNC authentication (RFC 6143 §7.2.2): DES-encrypt the 16-byte server
 * challenge with the (up to) 8-character password as key and return the
 * 16-byte response.
 *
 * Key derivation (canonical vncauth): password truncated/padded with zero
 * bytes to 8 bytes, then the bits within each byte are reversed. DES ignores
 * each key byte's low bit (parity), matching the vncauth convention.
 *
 * Cryptographically weak by protocol design; MLL layers real transport
 * security via SSH tunneling (REMOTE-001) rather than trusting VNC auth.
 */
object VncAuth {

    fun challengeResponse(challenge: ByteArray, password: String): ByteArray {
        require(challenge.size == 16) { "VNC challenge must be 16 bytes" }
        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyFromPassword(password), "DES"))
        return cipher.doFinal(challenge)
    }

    fun keyFromPassword(password: String): ByteArray {
        val raw = password.toByteArray(Charsets.ISO_8859_1)
        return ByteArray(8) { i ->
            val b = if (i < raw.size) raw[i].toInt() and 0xFF else 0
            reverseBits(b).toByte()
        }
    }

    private fun reverseBits(b: Int): Int {
        var v = b
        v = ((v and 0xF0) shr 4) or ((v and 0x0F) shl 4)
        v = ((v and 0xCC) shr 2) or ((v and 0x33) shl 2)
        v = ((v and 0xAA) shr 1) or ((v and 0x55) shl 1)
        return v and 0xFF
    }
}
