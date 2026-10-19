package org.claudroide.app.feature.linux.proot

import java.io.File

/**
 * Host-to-guest directory or file mount binding in PRoot.
 */
data class MountBinding(
    val hostPath: String,
    val guestPath: String = hostPath,
    val readOnly: Boolean = false
) {
    fun toCliArgument(): String {
        return if (hostPath == guestPath) {
            hostPath
        } else {
            "$hostPath:$guestPath"
        }
    }
}

/**
 * Configuration options for launching a rootless Linux container via PRoot on Android.
 */
data class PRootConfig(
    val rootfsPath: String,
    val prootBinaryPath: String = "proot",
    val workingDir: String = "/root",
    val command: List<String> = listOf("/bin/sh", "-l"),
    val mounts: List<MountBinding> = defaultMounts(),
    val envVars: Map<String, String> = defaultEnvVars(),
    val fakeRoot: Boolean = true,
    val link2symlink: Boolean = true,
    val killOnExit: Boolean = true,
    val customArgs: List<String> = emptyList()
) {
    companion object {
        fun defaultMounts(): List<MountBinding> {
            return listOf(
                MountBinding("/dev"),
                MountBinding("/proc"),
                MountBinding("/sys")
            )
        }

        fun defaultEnvVars(): Map<String, String> {
            return mapOf(
                "TERM" to "xterm-256color",
                "HOME" to "/root",
                "LANG" to "en_US.UTF-8",
                "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "SHELL" to "/bin/sh"
            )
        }
    }
}
