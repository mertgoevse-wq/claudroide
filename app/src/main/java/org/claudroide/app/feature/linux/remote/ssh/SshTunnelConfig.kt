package org.claudroide.app.feature.linux.remote.ssh

sealed class SshAuthMethod {
    /**
     * Preferred cryptographic public key authentication.
     * Keeps private key secured, never exposed in source or plain logs.
     */
    data class PublicKey(
        val keyAlias: String,
        val privateKeyBytes: ByteArray,
        val passphrase: String? = null
    ) : SshAuthMethod() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PublicKey) return false
            return keyAlias == other.keyAlias && privateKeyBytes.contentEquals(other.privateKeyBytes)
        }

        override fun hashCode(): Int {
            var result = keyAlias.hashCode()
            result = 31 * result + privateKeyBytes.contentHashCode()
            return result
        }

        override fun toString(): String = "PublicKey(alias='$keyAlias')"
    }

    /**
     * Fallback interactive password authentication.
     */
    data class Password(
        val password: String
    ) : SshAuthMethod() {
        override fun toString(): String = "Password(***)"
    }
}

data class SshTunnelConfig(
    val connectTimeoutMs: Int = 5000,
    val readTimeoutMs: Int = 10000,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000L,
    val authMethod: SshAuthMethod? = null,
    val strictHostKeyChecking: Boolean = true,
    val knownHostsStore: KnownHostsStore = InMemoryKnownHostsStore()
)
