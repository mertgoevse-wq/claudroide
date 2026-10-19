package org.claudroide.app

import org.claudroide.app.feature.chat.ChatProjectLinkEngine
import org.claudroide.app.feature.chat.ChatProjectLinkEngine.LinkOutcome
import org.claudroide.app.feature.chat.ChatProjectLinkEngine.LinkRefusal
import org.claudroide.app.feature.chat.ChatProjectLinkEngine.SendBlock
import org.claudroide.app.feature.chat.ConversationManager
import org.claudroide.app.feature.chat.ManagedConversation
import org.claudroide.app.feature.project.ContextCandidate
import org.claudroide.app.feature.project.ProjectAccessRegistry
import org.claudroide.app.feature.project.ProjectBoundaryEnforcer
import org.claudroide.app.feature.project.RelevanceSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 040 — „Chat und Projekt verbinden“.
 *
 * Die Tests prüfen die Zusagen an den Grenzen, nicht nur im glücklichen Fall:
 * Ablehnung, Widerruf, Wechsel ohne Bestätigung und ein Aufrufer, der eine alte
 * Dateiauswahl noch in der Hand hält.
 */
class ChatProjectLinkTest {

    private val rootA = "/storage/emulated/0/Documents/ProjektA"
    private val rootB = "/storage/emulated/0/Documents/ProjektB"

    private val projectA = ChatProjectLinkEngine.ProjectRef("p-a", "ProjektA", rootA)
    private val projectB = ChatProjectLinkEngine.ProjectRef("p-b", "ProjektB", rootB)

    private val model = "claude-sonnet-5-5"
    private val today = 20_700L

    private fun registry(vararg roots: String): ProjectAccessRegistry = ProjectAccessRegistry(
        ProjectBoundaryEnforcer.ProjectBoundary(projectRoots = roots.toList())
    )

    private fun candidate(path: String, tokens: Int = 100) = ContextCandidate(
        path = path,
        exists = true,
        estimatedTokens = tokens,
        signal = RelevanceSignal.EXPLICIT_MATCH
    )

    private fun engine(vararg roots: String) = ChatProjectLinkEngine(
        conversation = ManagedConversation(title = "Test"),
        registry = registry(*roots)
    )

    /** Engine mit Projekt A als Ordner in der App geöffnet und bereits gebunden. */
    private fun boundToA(
        roots: Array<String> = arrayOf(rootA),
        withContext: Boolean = true
    ): Pair<ChatProjectLinkEngine, List<ChatProjectLinkEngine.PreparedContext?>> {
        val engine = engine(*roots)
        engine.activateProject(projectA)
        val outcome = engine.bind(projectA)
        assertTrue("Binden sollte gelingen: $outcome", outcome is LinkOutcome.Applied)
        val context = if (withContext) {
            engine.prepareContext(listOf(candidate("app/Main.kt")), model, today)
        } else {
            null
        }
        return engine to listOf(context)
    }

    // ── Sichtbarkeit des Projektbezugs ───────────────────────────────────────

    @Test
    fun aNewConversationHasNoProject() {
        val engine = engine(rootA)

        assertNull(engine.binding())
        assertFalse(engine.currentConversation().isProjectBound)
        assertTrue(engine.disclosureLines().any { it.contains("keiner") })
    }

    @Test
    fun bindingIsVisibleInTheConversationAndInTheDisclosure() {
        val engine = engine(rootA)
        engine.activateProject(projectA)
        engine.bind(projectA)

        assertEquals("p-a", engine.currentConversation().projectId)
        assertEquals("ProjektA", engine.currentConversation().projectName)

        val text = engine.disclosureLines().joinToString("\n")
        assertTrue("Projektname muss sichtbar sein: $text", text.contains("ProjektA"))
        assertTrue("Ordner muss sichtbar sein: $text", text.contains(rootA))
    }

    @Test
    fun disclosureNamesEveryFileThatCouldBeSent() {
        val (engine, _) = boundToA()
        engine.prepareContext(
            listOf(candidate("app/Main.kt"), candidate("app/Util.kt")),
            model,
            today
        )

        val text = engine.disclosureLines().joinToString("\n")
        assertTrue("Ausgewählte Datei fehlt: $text", text.contains("app/Main.kt"))
        assertTrue("Ausgewählte Datei fehlt: $text", text.contains("app/Util.kt"))
    }

    @Test
    fun disclosureSaysWhenNothingIsSelected() {
        val (engine, _) = boundToA(withContext = false)

        assertTrue(
            engine.disclosureLines().any { it.contains("keine Datei") }
        )
    }

    // ── Zuordnen ────────────────────────────────────────────────────────────

    @Test
    fun bindingAProjectTheAppHasNotGrantedIsRefused() {
        val engine = engine(rootA)

        val outcome = engine.bind(projectB)

        assertTrue(outcome is LinkOutcome.Refused)
        assertEquals(LinkRefusal.PROJECT_NOT_PERMITTED, (outcome as LinkOutcome.Refused).reason)
        assertNull("Abgelehnt heißt: keine Bindung", engine.binding())
        assertTrue(outcome.message.isNotBlank())
    }

    @Test
    fun anUnusableProjectIsRefused() {
        val engine = engine(rootA)

        val outcome = engine.bind(projectA.copy(displayName = "  "))

        assertTrue(outcome is LinkOutcome.Refused)
        assertEquals(LinkRefusal.PROJECT_UNUSABLE, (outcome as LinkOutcome.Refused).reason)
    }

    @Test
    fun bindingDoesNotWidenTheProjectBoundary() {
        val engine = engine(rootA)
        val before = engine.currentConversation()

        engine.activateProject(projectA)
        engine.bind(projectA)

        // Die Verknüpfung darf den gespeicherten Bereich nicht erweitern: Projekt B
        // bleibt für die App unzugänglich.
        val verdict = engine.currentConversation()
        assertEquals(before.id, verdict.id)
        val outside = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction.file(
                ProjectBoundaryEnforcer.ToolKind.READ,
                "$rootB/app/Main.kt",
                ProjectBoundaryEnforcer.ActionOrigin.USER
            ),
            ProjectBoundaryEnforcer.ProjectBoundary(projectRoots = listOf(rootA))
        )
        assertFalse("Projekt B darf nicht zugänglich werden", outside.isAllowed)
    }

    @Test
    fun bindingDoesNotMoveTheRegistryGeneration() {
        val boundary = ProjectBoundaryEnforcer.ProjectBoundary(projectRoots = listOf(rootA))
        val registry = ProjectAccessRegistry(boundary)
        val before = registry.generation()
        val engine = ChatProjectLinkEngine(
            conversation = ManagedConversation(title = "Test"),
            registry = registry
        )

        engine.activateProject(projectA)
        engine.bind(projectA)
        engine.unbind()

        assertEquals(
            "Verknüpfen darf die Berechtigungsstufe nicht verändern",
            before,
            registry.generation()
        )
    }

    @Test
    fun rebindingTheSameProjectKeepsTheSelection() {
        val (engine, _) = boundToA()

        val outcome = engine.bind(projectA)

        assertTrue(outcome is LinkOutcome.Applied)
        assertTrue(engine.preparedContext() != null)
    }

    @Test
    fun trailingSlashIsTheSameProject() {
        val (engine, _) = boundToA()

        val outcome = engine.bind(projectA.copy(rootPath = "$rootA/"))

        assertTrue(outcome is LinkOutcome.Applied)
        assertTrue(
            (outcome as LinkOutcome.Applied).notices.any { it.contains("bereits") }
        )
    }

    // ── Wechseln mit Warnung ────────────────────────────────────────────────

    @Test
    fun switchingProjectsWithoutConfirmationChangesNothing() {
        val (engine, contexts) = boundToA(roots = arrayOf(rootA, rootB))
        val contextBefore = contexts.first()
        val generationBefore = engine.bindingGeneration()

        val outcome = engine.bind(projectB)

        assertTrue(outcome is LinkOutcome.NeedsConfirmation)
        assertEquals("p-a", engine.binding()?.id)
        assertEquals("Bindung blieb bestehen", contextBefore, engine.preparedContext())
        assertEquals(
            "Eine abgelehnte Umsicht darf die Generation nicht bewegen",
            generationBefore,
            engine.bindingGeneration()
        )
    }

    @Test
    fun theWarningNamesTheFilesThatWouldBeDroppedAndGained() {
        val (engine, _) = boundToA(roots = arrayOf(rootA, rootB))
        engine.prepareContext(listOf(candidate("app/Main.kt")), model, today)

        val outcome = engine.bind(
            project = projectB,
            candidates = listOf(candidate("lib/Neu.kt"))
        )

        val warning = (outcome as LinkOutcome.NeedsConfirmation).warning
        assertEquals(rootA, warning.from?.rootPath)
        assertEquals(rootB, warning.to.rootPath)
        assertEquals(listOf("app/Main.kt"), warning.filesDropped)
        assertEquals(listOf("lib/Neu.kt"), warning.filesGained)

        val text = warning.lines().joinToString("\n")
        assertTrue(text.contains("app/Main.kt"))
        assertTrue(text.contains("lib/Neu.kt"))
        assertTrue("Warnung muss eine Frage stellen", warning.question.contains("?"))
    }

    @Test
    fun confirmedSwitchRebindsAndDiscardsTheOldFiles() {
        val (engine, contexts) = boundToA(roots = arrayOf(rootA, rootB))
        val oldContext = contexts.first()

        // Ablauf in der App: erst den anderen Ordner öffnen, dann den Bezug
        // ausdrücklich umhängen.
        engine.activateProject(projectB)
        val outcome = engine.bind(projectB, acknowledged = true)

        assertTrue(outcome is LinkOutcome.Applied)
        assertEquals("p-b", engine.binding()?.id)
        assertEquals("p-b", engine.currentConversation().projectId)
        assertNull("Der alte Kontext darf nicht weiterleben", engine.preparedContext())

        val readiness = engine.evaluateSend()
        assertFalse(readiness.canSend)
        assertEquals(SendBlock.CONTEXT_OUTDATED, readiness.block)
        assertTrue(
            "Der verworfene Kontext wird beim Blockieren genannt: ${readiness.message}",
            readiness.message.contains("app/Main.kt")
        )
        assertTrue(engine.isUsableForSend(oldContext).isUsable.not())
        assertEquals(SendBlock.CONTEXT_OUTDATED, engine.isUsableForSend(oldContext).reason)
    }

    @Test
    fun afterConfirmedSwitchOnlyNewProjectFilesAreSent() {
        val (engine, _) = boundToA(roots = arrayOf(rootA, rootB))
        engine.bind(projectB, acknowledged = true)

        val context = engine.prepareContext(listOf(candidate("lib/Neu.kt")), model, today)

        assertNotNull(context)
        assertEquals(listOf("lib/Neu.kt"), engine.evaluateSend().contextFiles)
        assertFalse(
            "Keine Datei des alten Projekts darf mitgehen",
            engine.evaluateSend().contextFiles.contains("app/Main.kt")
        )
    }

    // ── Anderer Projektordner wird aktiv ────────────────────────────────────

    @Test
    fun activatingAnotherProjectWarnsAndBlocksSending() {
        val (engine, _) = boundToA(roots = arrayOf(rootA, rootB))
        engine.prepareContext(listOf(candidate("app/Main.kt")), model, today)

        val notices = engine.activateProject(projectB)

        assertTrue(notices.any { it.contains("ProjektA") })
        assertTrue(notices.any { it.contains("ProjektB") })

        val readiness = engine.evaluateSend()
        assertFalse(readiness.canSend)
        assertEquals(SendBlock.ACTIVE_PROJECT_DIFFERS, readiness.block)
        assertTrue("Keine Datei darf mitgehen", readiness.contextFiles.isEmpty())
    }

    @Test
    fun activatingAnotherProjectDoesNotChangeTheBindingSilently() {
        val (engine, _) = boundToA(roots = arrayOf(rootA, rootB))

        engine.activateProject(projectB)

        assertEquals("p-a", engine.binding()?.id)
        assertEquals("p-a", engine.currentConversation().projectId)
    }

    @Test
    fun aPreparedContextDoesNotSurviveAFolderSwitchBack() {
        val (engine, contexts) = boundToA(roots = arrayOf(rootA, rootB))
        val context = contexts.first()

        engine.activateProject(projectB)
        engine.activateProject(projectA)

        assertFalse(engine.isUsableForSend(context).isUsable)
        assertEquals(SendBlock.CONTEXT_OUTDATED, engine.isUsableForSend(context).reason)
    }

    @Test
    fun disclosureWarnsWhenAnotherProjectIsActive() {
        val (engine, _) = boundToA(roots = arrayOf(rootA, rootB))

        engine.activateProject(projectB)

        assertTrue(engine.disclosureLines().any { it.startsWith("Achtung") })
    }

    // ── Kontextzubereitung ──────────────────────────────────────────────────

    @Test
    fun contextIsPreparedForTheBoundProjectOnly() {
        val (engine, _) = boundToA(roots = arrayOf(rootA, rootB))

        val context = engine.prepareContext(listOf(candidate("app/Main.kt")), model, today)

        assertEquals("p-a", context?.projectId)
    }

    @Test
    fun noContextIsPreparedWhileAnotherProjectIsActive() {
        val (engine, _) = boundToA(roots = arrayOf(rootA, rootB))
        engine.activateProject(projectB)

        assertNull(engine.prepareContext(listOf(candidate("app/Main.kt")), model, today))
    }

    @Test
    fun noContextIsPreparedWithoutABinding() {
        val engine = engine(rootA)

        assertNull(engine.prepareContext(listOf(candidate("app/Main.kt")), model, today))
    }

    @Test
    fun secretFilesNeverReachThePreparedContext() {
        val (engine, _) = boundToA()

        val context = engine.prepareContext(
            listOf(candidate("app/Main.kt"), candidate(".env")),
            model,
            today
        )

        assertEquals(listOf("app/Main.kt"), context?.filePaths())
    }

    // ── Widerruf ────────────────────────────────────────────────────────────

    @Test
    fun revokingTheLinkedProjectBlocksSendingImmediately() {
        val registry = ProjectAccessRegistry(
            ProjectBoundaryEnforcer.ProjectBoundary(projectRoots = listOf(rootA))
        )
        val engine = ChatProjectLinkEngine(
            conversation = ManagedConversation(title = "Test"),
            registry = registry
        )
        engine.activateProject(projectA)
        engine.bind(projectA)
        val context = engine.prepareContext(listOf(candidate("app/Main.kt")), model, today)
        val before = engine.evaluateSend()
        assertTrue(before.canSend)
        assertEquals(listOf("app/Main.kt"), before.contextFiles)

        registry.revoke(ProjectBoundaryEnforcer.ToolKind.READ, rootA)

        val readiness = engine.evaluateSend()
        assertFalse(readiness.canSend)
        assertEquals(SendBlock.PROJECT_ACCESS_REVOKED, readiness.block)
        assertTrue("Aus einem widerrufenen Projekt geht nichts raus", readiness.contextFiles.isEmpty())
        assertTrue(readiness.message.isNotBlank())
        // Auch die alte Vorbereitung, die der Aufrufer noch hält, wird abgelehnt.
        assertFalse(engine.isUsableForSend(context).isUsable)
    }

    @Test
    fun aStaleContextIsAlsoRefusedAfterRevocation() {
        val registry = ProjectAccessRegistry(
            ProjectBoundaryEnforcer.ProjectBoundary(projectRoots = listOf(rootA))
        )
        val engine = ChatProjectLinkEngine(
            conversation = ManagedConversation(title = "Test"),
            registry = registry
        )
        engine.activateProject(projectA)
        engine.bind(projectA)
        val context = engine.prepareContext(listOf(candidate("app/Main.kt")), model, today)

        registry.revoke(ProjectBoundaryEnforcer.ToolKind.READ, rootA)

        val usability = engine.isUsableForSend(context)
        assertFalse(usability.isUsable)
        assertEquals(SendBlock.PROJECT_ACCESS_REVOKED, usability.reason)
    }

    // ── Lösen ───────────────────────────────────────────────────────────────

    @Test
    fun unbindingRemovesTheProjectFromTheConversation() {
        val (engine, _) = boundToA()

        val outcome = engine.unbind()

        assertNull(outcome.binding)
        assertNull(engine.binding())
        assertNull(engine.currentConversation().projectId)
        assertFalse(engine.currentConversation().isProjectBound)
    }

    @Test
    fun afterUnbindingNoProjectFilesAreSent() {
        val (engine, _) = boundToA()
        engine.prepareContext(listOf(candidate("app/Main.kt")), model, today)

        engine.unbind()

        val readiness = engine.evaluateSend()
        assertTrue("Ohne Projekt darf gesendet werden", readiness.canSend)
        assertTrue(readiness.contextFiles.isEmpty())
        assertEquals(SendBlock.NOTHING_BOUND, readiness.block)
        assertTrue(engine.disclosureLines().any { it.contains("keiner") })
    }

    @Test
    fun rebindingTheSameProjectStartsWithAnEmptySelection() {
        val (engine, _) = boundToA()
        engine.prepareContext(listOf(candidate("app/Main.kt")), model, today)

        engine.unbind()
        val outcome = engine.bind(projectA)

        assertTrue(outcome is LinkOutcome.Applied)
        assertNull(engine.preparedContext())
        val readiness = engine.evaluateSend()
        assertFalse(readiness.canSend)
        assertEquals(SendBlock.CONTEXT_OUTDATED, readiness.block)
    }

    @Test
    fun unbindingWithoutAProjectSaysSoInsteadOfClaimingAChange() {
        val engine = engine(rootA)

        val outcome = engine.unbind()

        assertTrue(outcome.notices.any { it.contains("keinem Projekt") })
    }

    @Test
    fun aDefaultConversationKeepsTheDefaultTitle() {
        val engine = ChatProjectLinkEngine(
            conversation = ManagedConversation(title = ConversationManager.DEFAULT_CHAT_TITLE),
            registry = registry(rootA)
        )

        engine.activateProject(projectA)
        val outcome = engine.bind(projectA)

        assertEquals(
            ConversationManager.DEFAULT_CHAT_TITLE,
            (outcome as LinkOutcome.Applied).conversation.title
        )
    }

    // ── Ehrlichkeit der Meldungen ───────────────────────────────────────────

    @Test
    fun everyRefusalCarriesAGermanSentence() {
        val engine = engine(rootA)

        val refusals = listOf(
            engine.bind(projectB),
            engine.bind(projectA.copy(rootPath = "")),
            engine.bind(projectB, acknowledged = true)
        )

        refusals.forEach { outcome ->
            assertTrue("Erwartet: $outcome", outcome is LinkOutcome.Refused)
            assertTrue(
                "Begründung fehlt: ${(outcome as LinkOutcome.Refused).message}",
                outcome.message.isNotBlank()
            )
        }
    }

    @Test
    fun everySendBlockExplainsItself() {
        val registry = ProjectAccessRegistry(
            ProjectBoundaryEnforcer.ProjectBoundary(projectRoots = listOf(rootA, rootB))
        )
        val engine = ChatProjectLinkEngine(
            conversation = ManagedConversation(title = "Test"),
            registry = registry
        )
        val messages = mutableListOf<String>()

        // Fall 1: ein anderer Ordner ist aktiv.
        engine.activateProject(projectA)
        engine.bind(projectA)
        engine.prepareContext(listOf(candidate("app/Main.kt")), model, today)
        engine.activateProject(projectB)
        engine.evaluateSend().also {
            assertEquals(SendBlock.ACTIVE_PROJECT_DIFFERS, it.block)
            messages += it.message
        }

        // Fall 2: zurück im richtigen Ordner, aber die Auswahl ist vom alten Stand.
        engine.activateProject(projectA)
        engine.evaluateSend().also {
            assertEquals(SendBlock.CONTEXT_OUTDATED, it.block)
            messages += it.message
        }

        // Fall 3: der Projektzugriff wurde widerrufen.
        registry.revoke(ProjectBoundaryEnforcer.ToolKind.READ, rootA)
        engine.evaluateSend().also {
            assertEquals(SendBlock.PROJECT_ACCESS_REVOKED, it.block)
            messages += it.message
        }

        messages.forEach {
            assertFalse("Blockierung ohne Begründung: „$it“", it.isBlank())
        }
    }

    @Test
    fun theConversationIdSurvivesEveryChange() {
        val engine = engine(rootA, rootB)
        val id = engine.currentConversation().id

        engine.activateProject(projectA)
        engine.bind(projectA)
        engine.activateProject(projectB)
        engine.bind(projectB, acknowledged = true)
        engine.unbind()

        assertEquals(id, engine.currentConversation().id)
    }
}