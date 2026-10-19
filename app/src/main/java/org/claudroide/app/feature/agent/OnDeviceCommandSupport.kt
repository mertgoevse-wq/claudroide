package org.claudroide.app.feature.agent

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 105 — "Check commands on Android" (Befehle auf Android prüfen).
 *
 * The findings and every source are in `docs/on-device-commands-feasibility.md`. This
 * type carries the one part the app has to get right at runtime: **it must not claim
 * a build happened when what actually happened was an in-process test run.**
 *
 * The research found that a normal third-party Android app cannot run a Gradle
 * build:
 *
 *  - AVF does compile code on Android, but the implementation is the system APEX
 *    `com.android.compos`, and the Java API is *"optional and not part of the
 *    bootclasspath"*. It is not an interface an app may call.
 *  - Android's Linux development environment exists, but is *"available on select
 *    devices"*, needs developer options, and is the Terminal app — not a service an
 *    app drives.
 *  - proot/Termux works but needs the separate terminal app this task rules out.
 *
 * What *is* possible is running **this app's own pure-Kotlin logic tests** in
 * process, with no Gradle, no JDK install and no terminal. That works only because
 * the policy classes were written without Android imports from the start.
 *
 * The consequence is the reason this type exists: an in-process run is **not** a
 * build, and the difference has to survive every round trip. [CommandOutcome] keeps
 * the two apart in the type, and [OnDeviceCommandReport.forbiddenClaim] is the one
 * sentence the app is not allowed to print.
 */

/** What an on-device command run can honestly be. */
enum class OnDeviceCommandKind(
    val label: String,
    /** Did it run a real toolchain, or only this app's own logic? */
    val isRealBuild: Boolean
) {
    /**
     * This app's own pure-Kotlin tests, run in process.
     *
     * Real evidence about real code — but not a build.
     */
    IN_PROCESS_UNIT_TESTS("this app's own logic tests, run in the app", false),

    /** A Gradle build or test task. Not achievable on a third-party app. */
    GRADLE_BUILD("a Gradle build", true),

    /** A plain command such as listing a directory. */
    SIMPLE_COMMAND("a simple command", false),

    /**
     * A remote runner.
     *
     * A real build, and it moves the user's source off the device — which is the
     * user's decision, not a fallback the app picks.
     */
    REMOTE_BUILD("a build on a remote runner", true)
}

/** How a run ended. */
enum class OnDeviceCommandStatus(val label: String) {

    /** Finished and every step reported. */
    FINISHED("finished"),

    /** Partly done; some steps did not report. */
    PARTIAL("partly done"),

    /** Did not finish. */
    ABORTED("stopped before the end"),

    /** Not attempted. */
    NOT_ATTEMPTED("not attempted")
}

/**
 * The result of one on-device command run.
 *
 * @property verifiedEvidence `true` only when [OnDeviceCommandKind.isRealBuild] is
 *           true **and** the run finished. A partial remote build is not evidence
 *           that a build succeeded, so it does not set this.
 */
data class OnDeviceCommandOutcome(
    val kind: OnDeviceCommandKind,
    val status: OnDeviceCommandStatus,
    val command: String,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val verifiedEvidence: Boolean = kind.isRealBuild && status == OnDeviceCommandStatus.FINISHED
) {
    init {
        require(completedCount >= 0) { "A count cannot be negative." }
        require(failedCount >= 0) { "A count cannot be negative." }
        require(status != OnDeviceCommandStatus.FINISHED || failedCount == 0) {
            "A run that had a failure is not finished."
        }
    }

    /** May this be described as a build? */
    val mayBeCalledABuild: Boolean get() = kind.isRealBuild && verifiedEvidence
}

/**
 * What the app may say about running commands on the device.
 *
 * Pure constants and one guard. It runs nothing and starts no process, because the
 * research concluded that a third-party app has nothing safe to start.
 */
object OnDeviceCommandSupport {

    /** The report the research produced. */
    const val FEASIBILITY_DOC: String = "docs/on-device-commands-feasibility.md"

    /** AVF overview, read 2026-10-02. */
    const val AVF_DOC_URL: String = "https://source.android.com/docs/core/virtualization"

    /** AVF use cases, read 2026-10-02. */
    const val AVF_USECASES_DOC_URL: String =
        "https://source.android.com/docs/core/virtualization/usecases"

    /**
     * Can this app run a Gradle build on a user's phone?
     *
     * `false`, and not as a temporary answer. The interface that would make it
     * possible is reserved for the platform.
     */
    const val CAN_RUN_GRADLE_ON_DEVICE: Boolean = false

    /**
     * The sentence the app must never print.
     *
     * Held as a constant so the forbidden claim has exactly one spelling, and a test
     * can check that no report line contains it.
     */
    val forbiddenClaim: String = "the project builds"

    /** The kinds this app can actually perform. */
    val supportedKinds: List<OnDeviceCommandKind> = listOf(
        OnDeviceCommandKind.IN_PROCESS_UNIT_TESTS,
        OnDeviceCommandKind.SIMPLE_COMMAND
    )

    /** Is this kind something this app offers? */
    fun isSupported(kind: OnDeviceCommandKind): Boolean = kind in supportedKinds

    /**
     * The report for the user, which repeats the limit rather than hiding it.
     *
     * A screen that could have said "build" and does not is doing the honest thing,
     * and the user should be able to see that it chose to.
     */
    fun capabilityLines(): List<String> = listOf(
        "Running project commands on this device:",
        "  This app can run its own logic tests in the app itself.",
        "  It cannot run a Gradle build on your phone. Android reserves the " +
            "virtualization interfaces that would make that possible for the platform.",
        "  A simple command such as reading a directory is possible.",
        "  A build on a remote runner is possible but sends your source off the " +
            "device, and that is your decision to make.",
        "",
        "A result from the in-app tests is evidence about this app's rules. It is " +
            "not evidence that your project builds.",
        "",
        "Sources: $AVF_DOC_URL",
        "         $AVF_USECASES_DOC_URL",
        "Full report: $FEASIBILITY_DOC"
    )

    /**
     * The lines for one run.
     *
     * An in-process test run is always accompanied by the sentence that it is not a
     * build, no matter how good the result was.
     */
    fun reportLines(outcome: OnDeviceCommandOutcome): List<String> = buildList {
        add("${outcome.kind.label}: ${outcome.status.label}.")
        add("Command: ${SecretMasker.redact(outcome.command)}")
        add("Completed: ${outcome.completedCount}, failed: ${outcome.failedCount}.")
        if (outcome.kind == OnDeviceCommandKind.IN_PROCESS_UNIT_TESTS) {
            add(
                "This ran the app's own logic tests in the app. It did not build " +
                    "anything and is not a build result."
            )
        }
        if (!outcome.mayBeCalledABuild && outcome.kind.isRealBuild) {
            add(
                "This cannot be called a build result: the run did not finish " +
                    "successfully."
            )
        }
        if (outcome.status == OnDeviceCommandStatus.NOT_ATTEMPTED) {
            add("Nothing was run.")
        }
    }
}
