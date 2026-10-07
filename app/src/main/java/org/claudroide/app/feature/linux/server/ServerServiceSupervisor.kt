package org.claudroide.app.feature.linux.server

import java.io.File

data class ServerServiceStatus(
    val serviceName: String,
    val isRunning: Boolean,
    val port: Int,
    val details: String? = null
)

enum class InitSystemType {
    SYSTEMD,
    OPENRC,
    PROOT_PROCESS_SUPERVISOR,
    UNSUPPORTED
}

class ServerServiceSupervisor(
    val headlessConfig: HeadlessConfig = HeadlessConfig(),
    val sshConfig: HardenedSshConfig = HardenedSshConfig(),
    val thermalProfile: ThermalAndChargeProfile = ThermalAndChargeProfile()
) {

    /**
     * Probes the runtime environment to determine the available init and process management system.
     * Differentiates Mode C (native Linux with systemd/OpenRC) from Mode A (PRoot without PID 1).
     */
    fun detectInitSystem(rootfsPath: String? = null): InitSystemType {
        // 1. Check if running under PRoot
        if (System.getenv("PROOT_TMP_DIR") != null || System.getenv("PROOT_LOADER") != null) {
            return InitSystemType.PROOT_PROCESS_SUPERVISOR
        }

        val prefix = rootfsPath?.let { "$it/" } ?: "/"

        // 2. Check systemd markers
        val systemdRun = File(prefix, "run/systemd/system")
        if (systemdRun.exists() && systemdRun.isDirectory) {
            return InitSystemType.SYSTEMD
        }

        // 3. Check OpenRC markers (e.g. Alpine / postmarketOS)
        val openrcRun = File(prefix, "run/openrc")
        val initd = File(prefix, "etc/init.d")
        if (openrcRun.exists() || (initd.exists() && File(prefix, "sbin/openrc-run").exists())) {
            return InitSystemType.OPENRC
        }

        // 4. In PRoot / Android userland rootfs, default to userland process supervisor
        return InitSystemType.PROOT_PROCESS_SUPERVISOR
    }

    /**
     * Generates a service definition tailored to the detected init system.
     */
    fun generateServiceDefinition(
        initType: InitSystemType,
        serviceName: String,
        execStart: String,
        restartSec: Int = 5
    ): String {
        return when (initType) {
            InitSystemType.SYSTEMD -> generateSystemdServiceUnit(serviceName, execStart, restartSec)
            InitSystemType.OPENRC -> generateOpenRcRunscript(serviceName, execStart)
            InitSystemType.PROOT_PROCESS_SUPERVISOR -> generateProotSupervisorScript(serviceName, execStart, restartSec)
            InitSystemType.UNSUPPORTED -> throw UnsupportedOperationException("Init system is unsupported for service $serviceName")
        }
    }

    /**
     * Generates a standard systemd service unit definition for running headless background services.
     */
    fun generateSystemdServiceUnit(
        serviceName: String,
        execStart: String,
        restartSec: Int = 5
    ): String {
        return buildString {
            appendLine("[Unit]")
            appendLine("Description=MLL Managed Service: $serviceName")
            appendLine("After=network.target")
            appendLine()
            appendLine("[Service]")
            appendLine("Type=simple")
            appendLine("ExecStart=$execStart")
            appendLine("Restart=always")
            appendLine("RestartSec=$restartSec")
            appendLine()
            appendLine("[Install]")
            appendLine("WantedBy=multi-user.target")
        }
    }

    /**
     * Generates an OpenRC runscript for Alpine/postmarketOS native Linux environments.
     */
    fun generateOpenRcRunscript(
        serviceName: String,
        execStart: String
    ): String {
        return buildString {
            appendLine("#!/sbin/openrc-run")
            appendLine("# MLL OpenRC Service Definition for $serviceName")
            appendLine("name=\"$serviceName\"")
            appendLine("description=\"MLL Managed Service: $serviceName\"")
            appendLine("command=\"$execStart\"")
            appendLine("command_background=\"yes\"")
            appendLine("pidfile=\"/run/${serviceName}.pid\"")
            appendLine()
            appendLine("depend() {")
            appendLine("    need net")
            appendLine("}")
        }
    }

    /**
     * Generates a portable POSIX userland supervisor script for Mode A PRoot environments
     * where systemd/OpenRC PID 1 cannot run.
     */
    fun generateProotSupervisorScript(
        serviceName: String,
        execStart: String,
        restartSec: Int = 5
    ): String {
        return buildString {
            appendLine("#!/bin/sh")
            appendLine("# MLL PRoot Process Supervisor for $serviceName")
            appendLine("PIDFILE=\"/tmp/${serviceName}.pid\"")
            appendLine("echo \"$$\" > \"\$PIDFILE\"")
            appendLine("trap 'rm -f \"\$PIDFILE\"; exit 0' TERM INT")
            appendLine("while true; do")
            appendLine("    $execStart")
            appendLine("    STATUS=\$?")
            appendLine("    echo \"[MLL Supervisor] $serviceName exited with \$STATUS, restarting in ${restartSec}s...\"")
            appendLine("    sleep $restartSec")
            appendLine("done")
        }
    }

    /**
     * Evaluates whether the server should throttle or safely throttle down.
     */
    fun evaluateThermalHealth(tempCelsius: Double): ThermalStatus {
        return thermalProfile.evaluateThermalStatus(tempCelsius)
    }
}
