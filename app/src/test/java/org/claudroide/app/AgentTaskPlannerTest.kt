package org.claudroide.app

import org.claudroide.app.feature.agent.AgentTaskPlanner
import org.claudroide.app.feature.agent.Clarification
import org.claudroide.app.feature.agent.ExecutionPlan
import org.claudroide.app.feature.agent.PlanStep
import org.claudroide.app.feature.agent.RequiredApproval
import org.claudroide.app.feature.agent.StepAction
import org.claudroide.app.feature.agent.StepRisk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 071 — „Aufgaben planen“.
 *
 * Geprüfte Zusagen:
 *  - Der Nutzer kann den Plan vor folgenreichen Aktionen prüfen.
 *  - Unklare Anforderungen erscheinen als Frage statt als Annahme.
 *  - Kein Plan überspringt eine Sicherheitsfreigabe still.
 */
class AgentTaskPlannerTest {

    private fun read(id: String = "R1") = PlanStep(
        id = id,
        title = "Projektdateien lesen",
        description = "Alle Kotlin-Dateien im Modul app durchsehen.",
        actions = setOf(StepAction.READ_FILE),
        files = listOf("app/src/main/kotlin")
    )

    private fun write(id: String = "W1", dependsOn: List<String> = listOf("R1")) = PlanStep(
        id = id,
        title = "Fehler beheben",
        description = "Den fehlerhaften Aufruf in Main.kt korrigieren.",
        actions = setOf(StepAction.WRITE_FILE),
        files = listOf("app/src/main/kotlin/Main.kt"),
        dependsOn = dependsOn
    )

    private fun test(id: String = "T1", dependsOn: List<String> = listOf("W1")) = PlanStep(
        id = id,
        title = "Tests ausführen",
        description = "Den Unit-Testlauf anstoßen und das Ergebnis zeigen.",
        actions = setOf(StepAction.RUN_COMMAND),
        files = listOf("app/src/test/kotlin"),
        dependsOn = dependsOn
    )

    // ── Kriterium 3: keine stille Freigabeumgehung ────────────────────────────

    @Test
    fun aFileChangeAlwaysNeedsAnApproval() {
        assertEquals(
            RequiredApproval.FILE_CHANGE,
            write().requiredApproval
        )
        assertTrue(write().requiredApproval.isSecurityRelevant)
    }

    @Test
    fun aPlanStepCannotDeclareThatItNeedsNoApproval() {
        // There is no `approval` field to set, so the only way to get NONE is to
        // perform no approval-relevant action.
        assertEquals(
            "Ein Schreibschritt kann seine Freigabepflicht nicht selbst aufheben",
            RequiredApproval.NONE,
            read().requiredApproval
        )
        assertTrue(
            "und ein Schreibschritt bekommt sie darum auch nicht",
            write().requiredApproval != RequiredApproval.NONE
        )
    }

    @Test
    fun theStepTakesTheStrictestApprovalOfAllItsActions() {
        val step = PlanStep(
            id = "X1",
            title = "Fehler beheben und hochladen",
            description = "Datei ändern und danach an den Server senden.",
            actions = setOf(StepAction.WRITE_FILE, StepAction.GIT_PUSH)
        )

        assertEquals(
            "Ein Schritt muss die strengste seiner Aktionen einhalten",
            RequiredApproval.GIT_PUSH,
            step.requiredApproval
        )
    }

    @Test
    fun aStepWithNoActionsNeedsNoApproval() {
        val step = PlanStep(
            id = "D1",
            title = "Nur erklären",
            description = "Den Auftrag zusammenfassen, ohne etwas zu verändern.",
            actions = emptySet()
        )

        assertEquals(RequiredApproval.NONE, step.requiredApproval)
        assertEquals(StepRisk.LOW, step.risk)
    }

    @Test
    fun readingAndLocalCommittingAreRisklessByDesign() {
        // Beides ist umkehrbar: eine gelesene Datei wird nicht verändert, ein
        // lokaler Commit lässt sich verwerfen.
        assertEquals(RequiredApproval.NONE, StepAction.READ_FILE.requiredApproval)
        assertEquals(RequiredApproval.NONE, StepAction.GIT_COMMIT.requiredApproval)
    }

    @Test
    fun everyActionThatLeavesTheDeviceNeedsAnApproval() {
        val leaving = listOf(
            StepAction.NETWORK_CALL,
            StepAction.USE_API_KEY,
            StepAction.INSTALL_DEPENDENCY,
            StepAction.GIT_PUSH,
            StepAction.DELETE_FILE,
            StepAction.RUN_COMMAND,
            StepAction.RUN_THIRD_PARTY_SKILL
        )

        leaving.forEach { action ->
            assertTrue("$action braucht eine Freigabe", action.requiredApproval.isSecurityRelevant)
            assertTrue(
                "$action darf nicht NONE sein",
                action.requiredApproval != RequiredApproval.NONE
            )
        }
    }

    // ── Kriterium 1: der Nutzer prüft vor folgenreichen Aktionen ──────────────

    @Test
    fun aFreshPlanDoesNotStart() {
        val plan = ExecutionPlan(request = "Fehler beheben", steps = listOf(read(), write()))

        assertFalse("Ein ungeprüfter Plan darf nicht starten", plan.canStart)
    }

    @Test
    fun thePlanStartsOnlyAfterEveryApprovalWasGranted() {
        val plan = ExecutionPlan(request = "Fehler beheben", steps = listOf(read(), write()))
            .grant("W1", RequiredApproval.FILE_CHANGE)

        assertTrue("Nach der Freigabe darf der Plan laufen", plan.canStart)
    }

    @Test
    fun grantingOneApprovalDoesNotGrantAnother() {
        val plan = ExecutionPlan(
            request = "Fehler beheben und hochladen",
            steps = listOf(write(), push())
        ).grant("W1", RequiredApproval.FILE_CHANGE)

        assertFalse(
            "Die Git-Freigabe ist eine eigene Entscheidung",
            plan.canStart
        )
        assertEquals(
            listOf("P1"),
            plan.missingApprovals().map { it.first.id }
        )
    }

    @Test
    fun theMissingApprovalsAreNamed() {
        val plan = ExecutionPlan(request = "Alles", steps = listOf(write(), test()))

        val lines = plan.reviewLines().joinToString("\n")
        assertTrue(lines.contains("Änderung an einer Datei freigeben"))
        assertTrue(lines.contains("Ausführung eines Befehls freigeben"))
        assertTrue(
            "Sicherheitsrelevante Freigaben müssen hervorgehoben sein",
            lines.contains("sicherheitsrelevant")
        )
    }

    @Test
    fun everyStepIsShownWithOrderFilesAndRisk() {
        val plan = ExecutionPlan(request = "Fehler beheben", steps = listOf(read(), write(), test()))

        val lines = plan.reviewLines().joinToString("\n")
        assertTrue(lines.contains("1. Projektdateien lesen"))
        assertTrue(lines.contains("2. Fehler beheben"))
        assertTrue(lines.contains("3. Tests ausführen"))
        assertTrue("Die betroffenen Dateien müssen stehen", lines.contains("app/src/main/kotlin/Main.kt"))
        assertTrue("Das Risiko muss stehen", lines.contains("Risiko:"))
    }

    @Test
    fun dependenciesOrderTheSteps() {
        val plan = ExecutionPlan(request = "Alles", steps = listOf(test(), write(), read()))

        assertEquals(
            listOf("R1", "W1", "T1"),
            plan.orderedSteps().map { it.id }
        )
    }

    @Test
    fun independentStepsAreGroupedForParallelWork() {
        val a = PlanStep(
            id = "A", title = "A", description = "Datei A ändern.",
            actions = setOf(StepAction.WRITE_FILE), files = listOf("a.kt")
        )
        val b = PlanStep(
            id = "B", title = "B", description = "Datei B ändern.",
            actions = setOf(StepAction.WRITE_FILE), files = listOf("b.kt")
        )

        val groups = ExecutionPlan(request = "beides", steps = listOf(a, b)).parallelGroups()

        assertEquals("Zwei getrennte Dateien dürfen gleichzeitig laufen", 1, groups.size)
        assertEquals(listOf("A", "B"), groups.first().map { it.id })
    }

    @Test
    fun stepsSharingAFileAreNeverRunInParallel() {
        val a = PlanStep(
            id = "A", title = "A", description = "Gemeinsame Datei ändern.",
            actions = setOf(StepAction.WRITE_FILE), files = listOf("shared.kt")
        )
        val b = PlanStep(
            id = "B", title = "B", description = "Dieselbe Datei ändern.",
            actions = setOf(StepAction.WRITE_FILE), files = listOf("shared.kt")
        )

        val groups = ExecutionPlan(request = "beides", steps = listOf(a, b)).parallelGroups()

        assertEquals("Ein Agent pro Dateibereich", 2, groups.size)
        assertEquals(listOf("A"), groups[0].map { it.id })
        assertEquals(listOf("B"), groups[1].map { it.id })
    }

    // ── Kriterium 2: unklare Anforderungen als Frage ──────────────────────────

    @Test
    fun anEmptyPlanAsksWhatToDoFirst() {
        val plan = AgentTaskPlanner.plan("Mach was", emptyList())

        assertFalse(plan.canStart)
        assertTrue(plan.clarifications.isNotEmpty())
        assertTrue(plan.clarifications.any { it.question.contains("zuerst") })
    }

    @Test
    fun aMentionedButUnplannedActionBecomesAQuestion() {
        val plan = AgentTaskPlanner.plan(
            "Lösch bitte die alten Testdateien und schreib danach eine neue.",
            steps = listOf(write())
        )

        val deletion = plan.clarifications.firstOrNull { it.question.contains("lösch") }
        assertTrue(
            "Ein genanntes Löschen darf nicht still fehlen, war ${plan.clarifications}",
            deletion != null
        )
        assertTrue(deletion!!.why.contains("Löschen freigeben"))
    }

    @Test
    fun aMentionedInstallBecomesAQuestion() {
        val plan = AgentTaskPlanner.plan(
            "Bitte das Paket installieren und dann den Test starten.",
            steps = listOf(test())
        )

        assertTrue(
            "Ein genanntes Installieren darf nicht still fehlen",
            plan.clarifications.any { it.question.contains("installier") }
        )
    }

    @Test
    fun aMentionedPushBecomesAQuestion() {
        val plan = AgentTaskPlanner.plan(
            "Nach der Änderung bitte push nach GitHub.",
            steps = listOf(write())
        )

        assertTrue(
            plan.clarifications.any { it.question.contains("push") }
        )
    }

    @Test
    fun anActionAlreadyInThePlanIsNotAskedAboutAgain() {
        val plan = AgentTaskPlanner.plan(
            "Lösch die alte Datei.",
            steps = listOf(
                PlanStep(
                    id = "D1", title = "Löschen", description = "Alte Datei entfernen.",
                    actions = setOf(StepAction.DELETE_FILE)
                )
            )
        )

        assertFalse(
            "Eine abgedeckte Aktion ist keine offene Frage",
            plan.clarifications.any { it.question.contains("Lösch") }
        )
    }

    @Test
    fun aRiskyAssumptionBecomesAQuestionInsteadOfStayingSilent() {
        val plan = AgentTaskPlanner.plan(
            "Alles fertigstellen.",
            steps = listOf(write()),
            assumptions = listOf("Ich lösche dabei alte Build-Dateien")
        )

        assertTrue(
            "Eine Annahme über eine folgenreiche Aktion ist eine Frage",
            plan.clarifications.any { it.question.contains("lösch") }
        )
    }

    @Test
    fun aHarmlessAssumptionDoesNotBlockThePlan() {
        val plan = AgentTaskPlanner.plan(
            "Fehler beheben.",
            steps = listOf(write()),
            assumptions = listOf("Die Datei liegt im Modul app")
        )

        assertFalse(
            "Eine harmlose Annahme ist kein Grund, anzuhalten",
            plan.clarifications.any { it.question.contains("liegt im Modul") }
        )
    }

    @Test
    fun aStepWithoutADescriptionIsNotReviewable() {
        val blind = PlanStep(
            id = "X1",
            title = "Schritt",
            description = "",
            actions = setOf(StepAction.READ_FILE)
        )

        val plan = AgentTaskPlanner.plan("Mach was", steps = listOf(blind))

        assertTrue(plan.clarifications.any { it.question.contains("X1") })
        assertTrue(
            "Ein Schritt ohne Beschreibung ist für den Nutzer nicht prüfbar",
            plan.clarifications.any { it.affectedSteps.contains("X1") }
        )
    }

    @Test
    fun openQuestionsKeepThePlanFromStarting() {
        val plan = ExecutionPlan(
            request = "Alles",
            steps = listOf(read()),
            clarifications = listOf(Clarification("Welche Datei?", "Ohne Datei kein Schritt."))
        ).grant("R1", RequiredApproval.NONE)

        assertFalse("Eine offene Frage hält den Plan an", plan.canStart)
    }

    @Test
    fun openQuestionsAreShownInTheReview() {
        val plan = ExecutionPlan(
            request = "Alles",
            steps = listOf(read()),
            clarifications = listOf(Clarification("Welche Datei?", "Ohne Datei kein Schritt."))
        )

        val lines = plan.reviewLines().joinToString("\n")
        assertTrue(lines.contains("nicht ausführbar"))
        assertTrue(lines.contains("Welche Datei?"))
        assertTrue(lines.contains("Ohne Datei kein Schritt."))
    }

    // ── Strukturfehler ───────────────────────────────────────────────────────

    @Test
    fun aDuplicateStepIdIsAProblem() {
        val problems = AgentTaskPlanner.structuralProblems(listOf(read("D1"), read("D1")))

        assertTrue(problems.any { it.message.contains("Doppelte") })
    }

    @Test
    fun anUnknownDependencyIsAProblem() {
        val orphan = write(dependsOn = listOf("GIBT-ES-NICHT"))

        val problems = AgentTaskPlanner.structuralProblems(listOf(read(), orphan))

        assertTrue(problems.any { it.message.contains("Unbekannte") })
    }

    @Test
    fun aDependencyCycleIsAProblem() {
        val a = PlanStep(
            id = "A", title = "A", description = "A wartet auf B.",
            actions = setOf(StepAction.READ_FILE), dependsOn = listOf("B")
        )
        val b = PlanStep(
            id = "B", title = "B", description = "B wartet auf A.",
            actions = setOf(StepAction.READ_FILE), dependsOn = listOf("A")
        )

        val problems = AgentTaskPlanner.structuralProblems(listOf(a, b))

        assertTrue(
            "Ein Kreis muss erkannt werden, war $problems",
            problems.any { it.message.contains("Abhängigkeitskreis") }
        )
    }

    @Test
    fun aStructuralProblemKeepsThePlanFromStarting() {
        val plan = ExecutionPlan(
            request = "Alles",
            steps = listOf(read()),
            problems = listOf(AgentTaskPlanner.structuralProblems(listOf(read("D"), read("D"))).first())
        )

        assertFalse(plan.canStart)
    }

    @Test
    fun aValidPlanHasNoStructuralProblem() {
        assertTrue(AgentTaskPlanner.structuralProblems(listOf(read(), write(), test())).isEmpty())
    }

    // ── Risikoeinstufung ─────────────────────────────────────────────────────

    @Test
    fun riskFollowsTheStrictestApproval() {
        assertEquals(StepRisk.LOW, read().risk)
        assertEquals(StepRisk.MEDIUM, write().risk)
        assertEquals(StepRisk.MEDIUM, test().risk)
        assertEquals(StepRisk.HIGH, push().risk)
        assertEquals(StepRisk.HIGH, delete().risk)
    }

    private fun push(id: String = "P1", dependsOn: List<String> = listOf("W1")) = PlanStep(
        id = id,
        title = "Hochladen",
        description = "Den Stand an das private Repository senden.",
        actions = setOf(StepAction.GIT_PUSH),
        dependsOn = dependsOn
    )

    private fun delete(id: String = "L1") = PlanStep(
        id = id,
        title = "Alte Dateien entfernen",
        description = "Nicht mehr benötigte Dateien im Projekt löschen.",
        actions = setOf(StepAction.DELETE_FILE)
    )
}