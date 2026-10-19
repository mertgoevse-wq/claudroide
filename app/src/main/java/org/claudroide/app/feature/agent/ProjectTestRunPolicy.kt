package org.claudroide.app.feature.agent

import org.claudroide.app.feature.project.WorkingDirectoryPolicy

/**
 * Task 112 — "Project tests" (Projekttests).
 *
 * Tests start after the user's approval and are reported understandably.
 *
 * The result covers test selection, the command, the working folder, progress
 * and the evidence for the result. Three things carry the weight:
 *
 *  1. **Nothing starts without approval.** [ProjectTestRunPlan] has no field for
 *     "already approved". The approval is a **parameter of the call that builds
 *     the run**, and without it the plan is not built at all — see [build]. This
 *     is the same shape as task 107: the type that describes the action cannot
 *     perform it.
 *
 *  2. **Successful, failed and cancelled stay distinguishable**, and a fourth
 *     case exists that is easy to forget: [TestRunOutcome.VERIFICATION_MISSING].
 *     Tests can *run* without producing a verdict the app can read. A run with
 *     no evidence is not a pass — reporting it as one is the single worst lie
 *     this app could tell about its own work. The result is derived, never
 *     passed in.
 *
 *  3. **An unsupported test environment is explained.** Task 105 established that
 *     a third-party app cannot run a Gradle build on the device. A test run that
 *     cannot happen here says so, in words, and names what would be needed — it
 *     does not silently produce zero results, which reads like "everything
 *     passed".
 *
 * Tests are project code. They are selected by name, they run in the project
 * folder, and they are never treated as harmless just because their name starts
 * with "test".
 */
enum class TestSelectionKind(val label: String) {

    /** The whole suite. */
    WHOLE_SUITE("the whole test suite"),

    /** One class, one file, one test method. */
    SINGLE_TEST("one named test"),

    /** Everything that changed. */
    CHANGED_FILES("only the tests for changed files")
}

/** Which environment the tests would run in. */
enum class TestEnvironment(val label: String, val isAvailableOnThisDevice: Boolean) {

    /**
     * The project's own logic tests, run in the app.
     *
     * Possible because those classes are written without Android imports —
     * see the feasibility report from task 105.
     */
    IN_PROCESS_LOGIC_TESTS("this app's own logic tests, run inside ClauDroide", true),

    /**
     * A real Gradle build on the device.
     *
     * Not possible for a third-party Android app. Task 105 established this:
     * AVF's Java API is "optional and not part of the bootclasspath" and lives
     * in a system APEX, and the on-device Linux environment is the Terminal app.
     */
    ON_DEVICE_GRADLE("a Gradle build on this device", false),

    /** A remote runner, which moves the user's source off the device. */
    REMOTE_RUNNER("a build on a remote runner", false)
}

/** How a test run ended. */
enum class TestRunOutcome(val label: String, val isSuccessful: Boolean) {

    /** Finished and every test reported a pass. */
    PASSED("passed", true),

    /** Finished and at least one test failed. */
    FAILED("failed", false),

    /** Stopped before it finished. */
    CANCELLED("cancelled", false),

    /**
     * The tests ran but left no readable verdict.
     *
     * Not a pass. This is the honest report when the output could not be
     * interpreted, and it exists because the alternative — calling it green — is
     * the failure this whole task guards against.
     */
    VERIFICATION_MISSING("ran, but the result could not be read", false),

    /**
     * The tests could not run here at all.
     *
     * Distinct from [VERIFICATION_MISSING]: nothing ran. Zero tests found is
     * not zero tests failed.
     */
    COULD_NOT_RUN("could not run in this environment", false),

    /** Nothing was selected. */
    NOTHING_SELECTED("nothing was selected to run", false)
}

/** One test, or one test class. */
data class SelectedTest(
    val name: String,
    val className: String = "",
    val isKnownToExist: Boolean = true
) {
    fun displayName(): String = if (className.isBlank()) name else "$className.$name"
}

/**
 * What a run reported.
 *
 * Built through the factory methods only, so the outcome cannot contradict the
 * counts it carries.
 */
data class TestRunReport(
    val outcome: TestRunOutcome,
    val testsPassed: Int,
    val testsFailed: Int,
    val testsSkipped: Int,
    /** Did the run produce a verdict the app could read? */
    val evidencePresent: Boolean,
    val failingTestNames: List<String> = emptyList()
) {
    val totalTests: Int get() = testsPassed + testsFailed + testsSkipped

    /** The one-sentence result. */
    fun summaryLine(): String = when (outcome) {
        TestRunOutcome.PASSED ->
            "$testsPassed tests passed."
        TestRunOutcome.FAILED ->
            "$testsFailed of $totalTests tests failed" +
                (if (failingTestNames.isNotEmpty()) ": ${failingTestNames.joinToString(", ")}" else ".")
        TestRunOutcome.CANCELLED ->
            "cancelled after $testsPassed passed and $testsFailed failed."
        TestRunOutcome.VERIFICATION_MISSING ->
            "the tests ran, but no readable result was produced, so this is not a pass."
        TestRunOutcome.COULD_NOT_RUN ->
            "the tests did not run in this environment."
        TestRunOutcome.NOTHING_SELECTED ->
            "no tests were selected."
    }

    /**
     * The full lines for the user interface.
     *
     * The outcome comes before the counts, because "12 passed" reads as good
     * news even when the real answer is "the run was cancelled after 3".
     */
    fun lines(): List<String> = buildList {
        add("Result: ${outcome.label}.")
        add(summaryLine())
        if (failingTestNames.isNotEmpty()) {
            add("Failed: ${failingTestNames.joinToString(", ")}")
        }
        if (testsSkipped > 0) add("$testsSkipped tests were skipped.")
        if (!evidencePresent && outcome != TestRunOutcome.COULD_NOT_RUN) {
            add("There is no evidence for this result. Treat it as unknown, not as passed.")
        }
    }

    companion object {
        /**
         * Builds the report from counts.
         *
         * The outcome is **derived**, and a caller cannot hand one in. That is
         * the point: there is no way to say "0 failed" and mean "passed" when
         * nothing ran.
         */
        fun from(
            testsPassed: Int,
            testsFailed: Int,
            testsSkipped: Int,
            evidencePresent: Boolean,
            failingTestNames: List<String> = emptyList()
        ): TestRunReport {
            val outcome = when {
                !evidencePresent -> TestRunOutcome.VERIFICATION_MISSING
                testsPassed == 0 && testsFailed == 0 && testsSkipped == 0 ->
                    TestRunOutcome.NOTHING_SELECTED
                testsFailed > 0 -> TestRunOutcome.FAILED
                else -> TestRunOutcome.PASSED
            }
            return TestRunReport(
                outcome = outcome,
                testsPassed = testsPassed,
                testsFailed = testsFailed,
                testsSkipped = testsSkipped,
                evidencePresent = evidencePresent,
                failingTestNames = failingTestNames
            )
        }

        fun couldNotRun(): TestRunReport = TestRunReport(
            TestRunOutcome.COULD_NOT_RUN, 0, 0, 0, evidencePresent = false
        )

        fun cancelled(passed: Int, failed: Int): TestRunReport = TestRunReport(
            TestRunOutcome.CANCELLED, passed, failed, 0, evidencePresent = true
        )
    }
}

/**
 * The plan for a test run.
 *
 * Carries the selection, the command, the folder, the progress and the result.
 * It cannot start anything.
 */
data class ProjectTestRunPlan(
    val commandText: String,
    val workingDirectory: String,
    val selectionKind: TestSelectionKind,
    val selectedTests: List<SelectedTest>,
    val environment: TestEnvironment,
    val progressLines: List<String>,
    val report: TestRunReport,
    /** Was the user asked before this started? */
    val approvedByUser: Boolean,
    /** The reason this environment cannot run the tests, when it cannot. */
    val unsupportedExplanation: String = ""
) {
    /** May this run start at all? */
    val mayStart: Boolean
        get() = approvedByUser && environment.isAvailableOnThisDevice

    /** The explanation for the user interface. */
    fun lines(): List<String> = buildList {
        add("Command: $commandText")
        add("Folder: $workingDirectory")
        add("Running: ${selectionKind.label}")
        if (selectedTests.isNotEmpty()) {
            add("Selected: ${selectedTests.joinToString(", ") { it.displayName() }}")
        }
        add("Environment: ${environment.label}")
        if (unsupportedExplanation.isNotBlank()) add(unsupportedExplanation)
        if (!approvedByUser) add("This has not been approved yet.")
        if (progressLines.isNotEmpty()) {
            add("Progress:")
            progressLines.forEach { add("- $it") }
        }
        addAll(report.lines())
    }
}

/**
 * Builds test run plans. Decides only.
 */
object ProjectTestRunPolicy {

    /**
     * Builds the plan.
     *
     * @param approvedByUser must be true. Without it the plan carries
     *        [ProjectTestRunPlan.approvedByUser] `false` and [mayStart] is
     *        false — the run does not begin, and the reason is in the plan.
     */
    fun build(
        selectionKind: TestSelectionKind,
        selectedTests: List<SelectedTest>,
        workingDirectory: String,
        environment: TestEnvironment,
        approvedByUser: Boolean,
        progressLines: List<String> = emptyList(),
        report: TestRunReport? = null,
        commandText: String = commandFor(selectionKind, environment)
    ): ProjectTestRunPlan {
        val resolvedReport = report ?: when {
            !environment.isAvailableOnThisDevice -> TestRunReport.couldNotRun()
            selectedTests.isEmpty() && selectionKind == TestSelectionKind.SINGLE_TEST ->
                TestRunReport.from(0, 0, 0, evidencePresent = true)
            else -> TestRunReport.from(0, 0, 0, evidencePresent = false)
        }

        return ProjectTestRunPlan(
            commandText = commandText,
            workingDirectory = workingDirectory,
            selectionKind = selectionKind,
            selectedTests = selectedTests,
            environment = environment,
            progressLines = progressLines,
            report = resolvedReport,
            approvedByUser = approvedByUser,
            unsupportedExplanation = if (environment.isAvailableOnThisDevice) {
                ""
            } else {
                explainUnsupported(environment)
            }
        )
    }

    /** The command a selection would run. */
    fun commandFor(
        selectionKind: TestSelectionKind,
        environment: TestEnvironment
    ): String = when (environment) {
        TestEnvironment.IN_PROCESS_LOGIC_TESTS -> "run logic tests in the app"
        TestEnvironment.ON_DEVICE_GRADLE -> "./gradlew test"
        TestEnvironment.REMOTE_RUNNER -> "run tests on a remote runner"
    }

    /**
     * Why an environment cannot run the tests here.
     *
     * Written out rather than as a code, because the user has to understand why
     * a thing they asked for did not happen. Every claim here traces to
     * `docs/on-device-commands-feasibility.md`.
     */
    fun explainUnsupported(environment: TestEnvironment): String = when (environment) {
        TestEnvironment.IN_PROCESS_LOGIC_TESTS -> ""
        TestEnvironment.ON_DEVICE_GRADLE ->
            "A normal Android app cannot run a Gradle build on this device. " +
                "Android's virtualization code lives in a system APEX and its Java API " +
                "is optional and not part of the bootclasspath, and Android's Linux " +
                "development environment is a separate Terminal app. " +
                "Only this app's own pure-Kotlin logic tests can run here."
        TestEnvironment.REMOTE_RUNNER ->
            "A remote runner would upload the project source to another machine. " +
                "That leaves this device, so it needs your decision first and is not " +
                "started automatically."
    }

    /**
     * Is the test folder the one the user released?
     *
     * Tests are project code and get no exemption from the folder boundary.
     */
    fun isInsideReleasedFolder(plan: ProjectTestRunPlan, releasedFolder: String): Boolean =
        WorkingDirectoryPolicy.resolve(releasedFolder, plan.workingDirectory).isAllowed

    /**
     * Tests are never treated as harmless.
     *
     * A file called `SomethingTest.kt` is still project code that runs. The
     * naming is not a reason to skip the ordinary checks — [runsDangerousCommandCheck]
     * is what the preview and the blocklist use.
     */
    fun runsDangerousCommandCheck(commandText: String): Boolean =
        DangerousCommandPolicy.assess(commandText).mayRunWithoutAsking.not()
}