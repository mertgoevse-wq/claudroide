package org.claudroide.app.core.diagnostics

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Startup diagnostics logger for debug builds.
 *
 * Tracks every startup phase and logs uncaught exceptions with full stacktraces
 * to an internal log file and external files dir so logs can be inspected
 * even when ADB logcat is unavailable.
 *
 * Never suppresses crashes: delegates to the original UncaughtExceptionHandler.
 */
object StartupDiagnostics {

    private const val TAG = "StartupDiagnostics"
    private const val LOG_FILE_NAME = "startup_diagnostics.log"

    enum class Phase {
        START,
        APPLICATION_INIT,
        MAIN_ACTIVITY_CREATE,
        DEPENDENCIES_INIT,
        DATASTORE_INIT,
        PROJECT_STORAGE_INIT,
        COMPOSE_INIT,
        VIEWMODEL_INIT,
        FIRST_FRAME,
        READY
    }

    data class PhaseRecord(
        val phase: Phase,
        val timestamp: Long,
        val detail: String?
    )

    private val recordedPhases = CopyOnWriteArrayList<PhaseRecord>()
    private var isDebuggable: Boolean = false
    private var logFile: File? = null
    private var externalLogFile: File? = null
    private var originalHandler: Thread.UncaughtExceptionHandler? = null
    private var installed = false

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        installed = true

        val appInfo = context.applicationInfo
        isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // Only active for debug builds
        if (!isDebuggable) return

        logFile = File(context.filesDir, LOG_FILE_NAME)
        externalLogFile = context.getExternalFilesDir(null)?.resolve(LOG_FILE_NAME)

        // Clear previous log on fresh process startup
        try {
            logFile?.writeText("=== ClauDroide Startup Diagnostic Log ===\n")
            externalLogFile?.writeText("=== ClauDroide Startup Diagnostic Log ===\n")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize log file: ${e.message}")
        }

        originalHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
            originalHandler?.uncaughtException(thread, throwable)
        }

        markPhase(Phase.START, "Diagnostic handler installed")
    }

    fun markPhase(phase: Phase, detail: String? = null) {
        if (!isDebuggable && installed) return

        val record = PhaseRecord(phase, System.currentTimeMillis(), detail)
        recordedPhases.add(record)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val timeStr = dateFormat.format(Date(record.timestamp))
        val logLine = "[$timeStr] [${record.phase.name}]${if (detail != null) " $detail" else ""}\n"

        logI(TAG, logLine.trim())

        try {
            logFile?.appendText(logLine)
            externalLogFile?.appendText(logLine)
            // Also attempt to write to public sdcard root if possible
            try {
                File("/sdcard/$LOG_FILE_NAME").appendText(logLine)
            } catch (_: Exception) {
                // Ignore if storage permission not granted for /sdcard
            }
        } catch (e: Exception) {
            logW(TAG, "Failed to append to startup log: ${e.message}")
        }
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val timeStr = dateFormat.format(Date())
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val lastPhase = recordedPhases.lastOrNull()?.phase?.name ?: "UNKNOWN"

        val crashReport = buildString {
            append("\n======================================================\n")
            append("FATAL STARTUP CRASH DETECTED\n")
            append("Time: $timeStr\n")
            append("Thread: ${thread.name} (id: ${thread.id})\n")
            append("Last Recorded Phase: $lastPhase\n")
            append("Exception: ${throwable.javaClass.name}: ${throwable.message}\n")
            append("Phases Traversed:\n")
            recordedPhases.forEach { r ->
                append("  - [${dateFormat.format(Date(r.timestamp))}] ${r.phase.name} (${r.detail ?: ""})\n")
            }
            append("\nFull Stacktrace:\n")
            append(stackTrace)
            append("======================================================\n")
        }

        logE(TAG, crashReport)

        try {
            logFile?.appendText(crashReport)
            externalLogFile?.appendText(crashReport)
            try {
                File("/sdcard/$LOG_FILE_NAME").appendText(crashReport)
            } catch (_: Exception) {
            }
        } catch (e: Exception) {
            logE(TAG, "Failed to write crash report to log file", e)
        }
    }

    private fun logI(tag: String, msg: String) {
        try {
            Log.i(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }

    private fun logW(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {
            System.err.println("[$tag] $msg")
        }
    }

    private fun logE(tag: String, msg: String, tr: Throwable? = null) {
        try {
            if (tr != null) Log.e(tag, msg, tr) else Log.e(tag, msg)
        } catch (_: Throwable) {
            System.err.println("[$tag] $msg")
            tr?.printStackTrace()
        }
    }

    fun getRecordedPhases(): List<PhaseRecord> = recordedPhases.toList()

    fun getLogContent(): String {
        return logFile?.takeIf { it.exists() }?.readText()
            ?: recordedPhases.joinToString("\n") { "[${it.phase.name}] ${it.detail ?: ""}" }
    }

    fun createShareIntent(context: Context): Intent? {
        val file = externalLogFile?.takeIf { it.exists() } ?: logFile?.takeIf { it.exists() } ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ClauDroide Startup Diagnostic Log")
            putExtra(Intent.EXTRA_TEXT, file.readText())
        }
    }
}
