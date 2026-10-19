package org.claudroide.app.feature.linux.distro.packages

import org.claudroide.app.feature.linux.env.LinuxEnvironment
import org.claudroide.app.feature.linux.proot.CommandResult
import org.claudroide.app.feature.linux.proot.LinuxCommandExecutor
import java.io.File

/**
 * Common contract for package managers running inside a Linux environment.
 */
interface PackageManager {
    val name: String

    fun update(): CommandResult
    fun install(packages: List<String>): CommandResult
    fun remove(packages: List<String>): CommandResult
    fun isInstalled(packageName: String): Boolean
    fun queryVersion(packageName: String): String?
}

/**
 * Debian/Ubuntu APT package manager implementation.
 */
class AptPackageManager(
    private val env: LinuxEnvironment,
    private val executor: LinuxCommandExecutor
) : PackageManager {

    override val name: String get() = "apt"

    override fun update(): CommandResult {
        return executor.execute(
            env = env,
            command = listOf("/usr/bin/apt-get", "update")
        )
    }

    override fun install(packages: List<String>): CommandResult {
        require(packages.isNotEmpty()) { "Package list cannot be empty" }
        val cmd = mutableListOf("/usr/bin/apt-get", "install", "-y")
        cmd.addAll(packages)
        return executor.execute(
            env = env,
            command = cmd
        )
    }

    override fun remove(packages: List<String>): CommandResult {
        require(packages.isNotEmpty()) { "Package list cannot be empty" }
        val cmd = mutableListOf("/usr/bin/apt-get", "remove", "-y")
        cmd.addAll(packages)
        return executor.execute(
            env = env,
            command = cmd
        )
    }

    override fun isInstalled(packageName: String): Boolean {
        // Fast-path: inspect /var/lib/dpkg/status directly without process spawn if file exists
        val statusFile = File(env.rootfsPath, "var/lib/dpkg/status")
        if (statusFile.exists()) {
            val content = statusFile.readText()
            val packageHeader = "Package: $packageName\n"
            val index = content.indexOf(packageHeader)
            if (index != -1) {
                val block = content.substring(index, minOf(content.length, index + 500))
                if (block.contains("Status: install ok installed")) {
                    return true
                }
            }
        }

        // Process fallback: dpkg-query -s
        val result = executor.execute(
            env = env,
            command = listOf("/usr/bin/dpkg-query", "-s", packageName)
        )
        return result.isSuccess && result.stdout.contains("Status: install ok installed")
    }

    override fun queryVersion(packageName: String): String? {
        val statusFile = File(env.rootfsPath, "var/lib/dpkg/status")
        if (statusFile.exists()) {
            val content = statusFile.readText()
            val packageHeader = "Package: $packageName\n"
            val index = content.indexOf(packageHeader)
            if (index != -1) {
                val block = content.substring(index, minOf(content.length, index + 500))
                val versionLine = block.lines().firstOrNull { it.startsWith("Version:") }
                if (versionLine != null) {
                    return versionLine.removePrefix("Version:").trim()
                }
            }
        }

        val result = executor.execute(
            env = env,
            command = listOf("/usr/bin/dpkg-query", "-W", "-f=\${Version}", packageName)
        )
        return if (result.isSuccess && result.stdout.isNotBlank()) result.stdout.trim() else null
    }
}

/**
 * Alpine / postmarketOS APK package manager implementation.
 */
class ApkPackageManager(
    private val env: LinuxEnvironment,
    private val executor: LinuxCommandExecutor
) : PackageManager {

    override val name: String get() = "apk"

    override fun update(): CommandResult {
        return executor.execute(
            env = env,
            command = listOf("/sbin/apk", "update")
        )
    }

    override fun install(packages: List<String>): CommandResult {
        require(packages.isNotEmpty()) { "Package list cannot be empty" }
        val cmd = mutableListOf("/sbin/apk", "add")
        cmd.addAll(packages)
        return executor.execute(
            env = env,
            command = cmd
        )
    }

    override fun remove(packages: List<String>): CommandResult {
        require(packages.isNotEmpty()) { "Package list cannot be empty" }
        val cmd = mutableListOf("/sbin/apk", "del")
        cmd.addAll(packages)
        return executor.execute(
            env = env,
            command = cmd
        )
    }

    override fun isInstalled(packageName: String): Boolean {
        val dbFile = File(env.rootfsPath, "lib/apk/db/installed")
        if (dbFile.exists()) {
            val content = dbFile.readText()
            if (content.contains("P:$packageName\n")) {
                return true
            }
        }

        val result = executor.execute(
            env = env,
            command = listOf("/sbin/apk", "info", "-e", packageName)
        )
        return result.isSuccess && result.stdout.trim() == packageName
    }

    override fun queryVersion(packageName: String): String? {
        val result = executor.execute(
            env = env,
            command = listOf("/sbin/apk", "version", packageName)
        )
        if (result.isSuccess) {
            val line = result.stdout.lines().firstOrNull { it.startsWith(packageName) }
            if (line != null) {
                return line.split("\\s+".toRegex()).getOrNull(2) ?: line
            }
        }
        return null
    }
}

/**
 * Factory for resolving package managers according to distribution type.
 */
object PackageManagerFactory {
    fun create(env: LinuxEnvironment, executor: LinuxCommandExecutor): PackageManager {
        val distro = env.distroId.lowercase()
        return when {
            distro.contains("alpine") || distro.contains("postmarketos") -> ApkPackageManager(env, executor)
            else -> AptPackageManager(env, executor)
        }
    }
}
