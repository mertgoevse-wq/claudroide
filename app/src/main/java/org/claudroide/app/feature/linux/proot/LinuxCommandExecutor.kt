package org.claudroide.app.feature.linux.proot

import org.claudroide.app.feature.linux.env.LinuxEnvironment
import org.claudroide.app.feature.linux.proot.bootstrap.RuntimeBootstrapEngine
import org.claudroide.app.feature.linux.proot.bootstrap.RuntimeStatus
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Result of a command executed inside a Linux environment.
 */
data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
) {
    val isSuccess: Boolean get() = exitCode == 0
}

/**
 * Executes commands inside rootless Linux environments via PRoot with full environment setup.
 */
class LinuxCommandExecutor(
    private val runtimeBootstrapEngine: RuntimeBootstrapEngine,
    private val processRunner: ProcessRunner = SystemProcessRunner()
) {

    /**
     * Executes a command synchronously inside [env] and collects its output.
     */
    fun execute(
        env: LinuxEnvironment,
        command: List<String>,
        workingDir: String = "/root",
        customEnv: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 60
    ): CommandResult {
        val rootfsDir = File(env.rootfsPath)
        if (!rootfsDir.exists() || !rootfsDir.isDirectory) {
            return CommandResult(
                exitCode = -1,
                stdout = "",
                stderr = "Rootfs directory does not exist: ${env.rootfsPath}",
                durationMs = 0
            )
        }

        val prootBinary = when (val status = kotlinx.coroutines.runBlocking { runtimeBootstrapEngine.getStatus() }) {
            is RuntimeStatus.Ready -> status.binaryFile.absolutePath
            else -> "proot"
        }

        val defaultEnv = PRootConfig.defaultEnvVars() + mapOf(
            "DEBIAN_FRONTEND" to "noninteractive",
            "LC_ALL" to "C.UTF-8"
        ) + customEnv

        val config = PRootConfig(
            rootfsPath = rootfsDir.absolutePath,
            prootBinaryPath = prootBinary,
            workingDir = workingDir,
            command = command,
            mounts = PRootConfig.defaultMounts(),
            envVars = defaultEnv,
            fakeRoot = true,
            link2symlink = true,
            killOnExit = true
        )

        val cliArgs = PRootCommandBuilder.buildCommandLine(config)
        val startTime = System.currentTimeMillis()

        return try {
            val handle = processRunner.start(cliArgs, defaultEnv, rootfsDir)

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val stdoutThread = Thread {
                BufferedReader(InputStreamReader(handle.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stdoutBuilder.append(line).append("\n")
                    }
                }
            }

            val stderrThread = Thread {
                BufferedReader(InputStreamReader(handle.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stderrBuilder.append(line).append("\n")
                    }
                }
            }

            stdoutThread.start()
            stderrThread.start()

            val exitCode = handle.waitFor(timeoutSeconds * 1000L) ?: -1
            stdoutThread.join(2000L)
            stderrThread.join(2000L)

            val duration = System.currentTimeMillis() - startTime
            CommandResult(
                exitCode = exitCode,
                stdout = stdoutBuilder.toString().trimEnd(),
                stderr = stderrBuilder.toString().trimEnd(),
                durationMs = duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            CommandResult(
                exitCode = -1,
                stdout = "",
                stderr = "Process execution failed: ${e.message}",
                durationMs = duration
            )
        }
    }

    /**
     * Starts an asynchronous long-running process inside [env] (e.g. VNC desktop server, daemon).
     */
    fun startProcess(
        env: LinuxEnvironment,
        command: List<String>,
        workingDir: String = "/root",
        customEnv: Map<String, String> = emptyMap()
    ): ProcessHandle {
        val rootfsDir = File(env.rootfsPath)
        val prootBinary = when (val status = kotlinx.coroutines.runBlocking { runtimeBootstrapEngine.getStatus() }) {
            is RuntimeStatus.Ready -> status.binaryFile.absolutePath
            else -> "proot"
        }

        val envVars = PRootConfig.defaultEnvVars() + mapOf(
            "DEBIAN_FRONTEND" to "noninteractive"
        ) + customEnv

        val config = PRootConfig(
            rootfsPath = rootfsDir.absolutePath,
            prootBinaryPath = prootBinary,
            workingDir = workingDir,
            command = command,
            mounts = PRootConfig.defaultMounts(),
            envVars = envVars,
            fakeRoot = true,
            link2symlink = true,
            killOnExit = false
        )

        val cliArgs = PRootCommandBuilder.buildCommandLine(config)
        return processRunner.start(cliArgs, envVars, rootfsDir)
    }
}
