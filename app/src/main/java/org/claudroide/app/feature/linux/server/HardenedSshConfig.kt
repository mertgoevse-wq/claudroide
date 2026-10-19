package org.claudroide.app.feature.linux.server

data class HardenedSshConfig(
    val port: Int = 22,
    val permitRootLogin: Boolean = false,
    val passwordAuthentication: Boolean = false,
    val pubkeyAuthentication: Boolean = true,
    val x11Forwarding: Boolean = false,
    val maxAuthTries: Int = 3,
    val clientAliveInterval: Int = 30,
    val clientAliveCountMax: Int = 3
) {
    /**
     * Generates a hardened sshd_config content adhering to MLL security policies.
     */
    fun generateSshdConfig(): String {
        return buildString {
            appendLine("# MLL Hardened SSH Daemon Configuration")
            appendLine("Port $port")
            appendLine("PermitRootLogin ${if (permitRootLogin) "yes" else "no"}")
            appendLine("PasswordAuthentication ${if (passwordAuthentication) "yes" else "no"}")
            appendLine("PubkeyAuthentication ${if (pubkeyAuthentication) "yes" else "no"}")
            appendLine("X11Forwarding ${if (x11Forwarding) "yes" else "no"}")
            appendLine("MaxAuthTries $maxAuthTries")
            appendLine("ClientAliveInterval $clientAliveInterval")
            appendLine("ClientAliveCountMax $clientAliveCountMax")
            appendLine("KexAlgorithms curve25519-sha256,curve25519-sha256@libssh.org,diffie-hellman-group16-sha512")
            appendLine("Ciphers chacha20-poly1305@openssh.com,aes256-gcm@openssh.com")
            appendLine("MACs hmac-sha2-512-etm@openssh.com")
        }
    }
}
