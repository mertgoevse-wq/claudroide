package org.claudroide.app.feature.linux.proot

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * Handle to a running OS process.
 */
interface ProcessHandle {
    val pid: Long
    val isAlive: Boolean
    val inputStream: InputStream
    val errorStream: InputStream
    val outputStream: OutputStream

    fun waitFor(timeoutMs: Long): Int?
    fun destroy()
    fun destroyForcibly()
}

/**
 * Abstraction for spawning and managing native processes.
 */
interface ProcessRunner {
    fun start(
        command: List<String>,
        environment: Map<String, String> = emptyMap(),
        workingDir: File? = null
    ): ProcessHandle
}

/**
 * Real OS process runner using java.lang.ProcessBuilder.
 */
class SystemProcessRunner : ProcessRunner {
    override fun start(
        command: List<String>,
        environment: Map<String, String>,
        workingDir: File?
    ): ProcessHandle {
        val pb = ProcessBuilder(command)
        if (workingDir != null) {
            pb.directory(workingDir)
        }
        val env = pb.environment()
        env.putAll(environment)

        val process = pb.start()
        return JavaProcessHandle(process)
    }

    private class JavaProcessHandle(private val process: Process) : ProcessHandle {
        override val pid: Long
            get() = try {
                -1L
            } catch (_: Exception) {
                -1L
            }

        override val isAlive: Boolean
            get() = process.isAlive

        override val inputStream: InputStream
            get() = process.inputStream

        override val errorStream: InputStream
            get() = process.errorStream

        override val outputStream: OutputStream
            get() = process.outputStream

        override fun waitFor(timeoutMs: Long): Int? {
            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            return if (finished) process.exitValue() else null
        }

        override fun destroy() {
            process.destroy()
        }

        override fun destroyForcibly() {
            process.destroyForcibly()
        }
    }
}

/**
 * Simulated process runner for testing environments where proot is mocked.
 */
class SimulatedProcessRunner(
    private val simulatedExitCode: Int = 0
) : ProcessRunner {

    var lastExecutedCommand: List<String>? = null
        private set
    var lastEnvironment: Map<String, String>? = null
        private set
    var lastWorkingDir: File? = null
        private set

    override fun start(
        command: List<String>,
        environment: Map<String, String>,
        workingDir: File?
    ): ProcessHandle {
        lastExecutedCommand = command
        lastEnvironment = environment
        lastWorkingDir = workingDir

        return object : ProcessHandle {
            private var alive = true

            override val pid: Long = 1337L
            override val isAlive: Boolean
                get() = alive

            override val inputStream: InputStream = "Simulated PRoot output\n".byteInputStream()
            override val errorStream: InputStream = "".byteInputStream()
            override val outputStream: OutputStream = object : OutputStream() {
                override fun write(b: Int) {}
            }

            override fun waitFor(timeoutMs: Long): Int? {
                alive = false
                return simulatedExitCode
            }

            override fun destroy() {
                alive = false
            }

            override fun destroyForcibly() {
                alive = false
            }
        }
    }
}
