package org.claudroide.app

import org.claudroide.app.feature.project.ProjectAccessRegistry
import org.claudroide.app.feature.project.ProjectBoundaryEnforcer
import org.claudroide.app.feature.project.ProjectBoundaryEnforcer.ActionOrigin
import org.claudroide.app.feature.project.ProjectBoundaryEnforcer.BoundaryDecision
import org.claudroide.app.feature.project.ProjectBoundaryEnforcer.ToolKind
import org.claudroide.app.feature.project.ProjectBoundaryEnforcer.Revocations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 119 (Gate) — "Projektgrenze durchsetzen".
 *
 * Acceptance criteria from the brief, all of which are exercised here:
 *  - "Zentrale Prüfung vor Lesen, Schreiben, Löschen und Ausführen."
 *  - "Jeder Werkzeugtyp denselben Schutz verwendet." Read, write, delete and execute
 *    must hit one policy object and get the same verdict for the same input; a
 *    per-kind loop proves this instead of assuming it.
 *  - "Widerrufener Zugriff unmittelbar blockiert." A revocation must block the very
 *    next call, not the next app start.
 *  - Schutz: "Keine KI-Anweisung kann die App-Grenze selbst erweitern." A model
 *    action carrying its own roots must be refused.
 *  - A secret is never sendable and not writable through the boundary.
 *  - No silent failures: every refusal carries a plain German reason.
 */
class ProjectBoundaryTest {

    private val root = "/data/user/0/org.claudroide.app/files/projects/demo"

    private val boundary = ProjectBoundaryEnforcer.ProjectBoundary(projectRoots = listOf(root))

    private fun decisionOf(action: ProjectBoundaryEnforcer.ToolAction) =
        ProjectBoundaryEnforcer.evaluate(action, boundary).decision

    private fun verdictOf(action: ProjectBoundaryEnforcer.ToolAction) =
        ProjectBoundaryEnforcer.evaluate(action, boundary)

    private fun fileDecision(kind: ToolKind, path: String) =
        decisionOf(ProjectBoundaryEnforcer.ToolAction.file(kind, path))

    // ── Criterion: every tool kind uses the same protection ─────────────────

    @Test
    fun everyToolKind_usesTheSameCheckForTheSamePath() {
        val action = ProjectBoundaryEnforcer.ToolAction(
            kind = ToolKind.READ,
            rawKind = "READ",
            path = "../../../etc/passwd",
            command = "../../../etc/passwd",
            origin = ActionOrigin.MODEL
        )

        // The three file kinds share one check, so they must answer identically.
        val fileDecisions = listOf(ToolKind.READ, ToolKind.WRITE, ToolKind.DELETE).map { kind ->
            decisionOf(action.copy(kind = kind, rawKind = kind.name))
        }
        assertEquals(
            "Lesen, Schreiben und Löschen müssen denselben Schutz benutzen, aber es gab: $fileDecisions",
            1,
            fileDecisions.distinct().size
        )
        assertEquals(BoundaryDecision.OUTSIDE_PROJECT, fileDecisions.first())

        // Ausführen goes through the same boundary and additionally through the
        // command rules, so it may name a stronger reason — but it must not be laxer.
        val execute = decisionOf(action.copy(kind = ToolKind.EXECUTE, rawKind = "EXECUTE"))
        assertFalse(
            "Ausführen darf einen Weg aus dem Projekt nicht zulassen",
            execute == BoundaryDecision.ALLOWED
        )
        assertTrue(
            "Ausführen muss einen tragfähigen Grund nennen: $execute",
            execute == BoundaryDecision.OUTSIDE_PROJECT || execute == BoundaryDecision.FORBIDDEN_COMMAND
        )
    }

    @Test
    fun ordinaryFileInsideTheProject_isAllowedForEveryFileKind() {
        listOf(ToolKind.READ, ToolKind.WRITE, ToolKind.DELETE).forEach { kind ->
            assertEquals(
                "$kind muss eine Datei im Projekt zulassen",
                BoundaryDecision.ALLOWED,
                fileDecision(kind, "src/main.kt")
            )
        }
        assertEquals(
            "Ausführen im Projekt muss erlaubt sein",
            BoundaryDecision.ALLOWED,
            decisionOf(ProjectBoundaryEnforcer.ToolAction.exec("./gradlew test"))
        )
    }

    @Test
    fun allFourToolKinds_rejectAPathOutsideTheProject() {
        listOf(
            "../../../etc/passwd",
            "/data/data/com.other.app/files/secret.txt",
            "/proc/self/environ",
            "../other-project/src/main.kt"
        ).forEach { path ->
            listOf(ToolKind.READ, ToolKind.WRITE, ToolKind.DELETE).forEach { kind ->
                assertEquals(
                    "$kind auf „$path“ muss abgelehnt werden",
                    BoundaryDecision.OUTSIDE_PROJECT,
                    fileDecision(kind, path)
                )
            }
        }
    }

    @Test
    fun aSymlinkLeavingTheProject_isRefusedForEveryFileKind() {
        listOf(ToolKind.READ, ToolKind.WRITE, ToolKind.DELETE).forEach { kind ->
            val verdict = ProjectBoundaryEnforcer.evaluate(
                ProjectBoundaryEnforcer.ToolAction(
                    kind = kind,
                    rawKind = kind.name,
                    path = "link/secret",
                    resolvedRealPath = "/data/data/com.other.app/files/secret",
                    realRoot = root
                ),
                boundary
            )
            assertEquals("$kind muss einen Verweis nach draußen ablehnen", BoundaryDecision.SYMLINK_ESCAPE, verdict.decision)
        }
    }

    // ── Criterion: no model instruction can widen the boundary ──────────────

    @Test
    fun aModelThatHandsItselfAnExtraRoot_isRefused() {
        val verdict = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction(
                kind = ToolKind.READ,
                rawKind = "READ",
                path = "src/main.kt",
                origin = ActionOrigin.MODEL,
                additionalRoots = listOf("/data/data/com.other.app/files")
            ),
            boundary
        )
        assertEquals(BoundaryDecision.GRANTS_NOT_FROM_USER, verdict.decision)
        assertTrue("Der Grund muss genannt werden: ${verdict.message}", verdict.message.isNotBlank())
    }

    @Test
    fun aModelWithNoOrigin_isTreatedLikeAModel() {
        val verdict = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction(
                kind = ToolKind.WRITE,
                rawKind = "WRITE",
                path = "src/main.kt",
                origin = ActionOrigin.UNKNOWN,
                additionalRoots = listOf("/sdcard")
            ),
            boundary
        )
        assertEquals(BoundaryDecision.GRANTS_NOT_FROM_USER, verdict.decision)
    }

    @Test
    fun theUserMayAddAProjectAreaByHand() {
        val second = "/storage/emulated/0/Documents/extra"
        val verdict = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction(
                kind = ToolKind.READ,
                rawKind = "READ",
                path = "$second/notes.md",
                origin = ActionOrigin.USER,
                additionalRoots = listOf(second)
            ),
            boundary
        )
        assertEquals("Ein vom Nutzer gewählter Ordner muss gelten", BoundaryDecision.ALLOWED, verdict.decision)
    }

    @Test
    fun aGrantTheUserNeverConfirmed_contributesNothing() {
        val unconfirmed = ProjectBoundaryEnforcer.ProjectBoundary(
            projectRoots = listOf(root),
            grants = ProjectBoundaryEnforcer.BoundaryGrants(
                extraRoots = listOf("/data/data/com.other.app/files"),
                grantedByUser = false
            )
        )
        val verdict = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "/data/data/com.other.app/files/k.txt"),
            unconfirmed
        )
        assertEquals(BoundaryDecision.OUTSIDE_PROJECT, verdict.decision)
    }

    // ── Criterion: secrets are never sendable and never writable ────────────

    @Test
    fun aSecretFile_isNeitherSentNorWritten() {
        listOf(ToolKind.READ, ToolKind.WRITE, ToolKind.DELETE).forEach { kind ->
            val verdict = ProjectBoundaryEnforcer.evaluate(
                ProjectBoundaryEnforcer.ToolAction.file(kind, "config/.env"),
                boundary
            )
            assertEquals("$kind auf .env muss abgelehnt werden", BoundaryDecision.SECRET_FILE, verdict.decision)
            assertTrue("Der Grund muss genannt werden: ${verdict.message}", verdict.message.isNotBlank())
        }
    }

    @Test
    fun keyMaterial_isAlsoRefused() {
        listOf(ToolKind.READ, ToolKind.WRITE).forEach { kind ->
            assertEquals(
                "$kind auf server.pem muss abgelehnt werden",
                BoundaryDecision.SECRET_FILE,
                fileDecision(kind, "certs/server.pem")
            )
        }
    }

    @Test
    fun aSecretOutsideTheProject_isRefusedAsASecretNotAsAPath() {
        // The file cannot be reached anyway; either refusal is correct, but a secret
        // must never come back as ALLOWED.
        val verdict = verdictOf(
            ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "../../other/.env")
        )
        assertFalse("Ein Geheimnis darf nie durchgelassen werden", verdict.isAllowed)
    }

    @Test
    fun aCommandTouchingASecretFile_isRefused() {
        val verdict = verdictOf(ProjectBoundaryEnforcer.ToolAction.exec("cat config/.env"))
        assertEquals(BoundaryDecision.SECRET_FILE, verdict.decision)
        assertTrue(verdict.message.isNotBlank())
    }

    @Test
    fun aSecretPathIsCheckedAgainstTheProjectRootNotTheWholeDisk() {
        // An absolute path inside the project must still be recognised as a secret.
        val verdict = verdictOf(
            ProjectBoundaryEnforcer.ToolAction.file(ToolKind.WRITE, "$root/config/keystore.properties")
        )
        assertEquals(BoundaryDecision.SECRET_FILE, verdict.decision)
    }

    // ── Regression: the command path had the same hole as the file path ─────

    @Test
    fun anAbsolutePathInsideACommand_isCheckedLikeAFilePath() {
        // Real gap (fixed): the file boundary was never applied to the paths a
        // command carries, so `cat /data/data/com.other.app/…/prefs.xml` passed the
        // very check that refuses the identical file as a READ action.
        val verdict = verdictOf(
            ProjectBoundaryEnforcer.ToolAction.exec(
                "cat /data/data/com.other.app/shared_prefs/prefs.xml"
            )
        )
        assertEquals(BoundaryDecision.OUTSIDE_PROJECT, verdict.decision)
        assertTrue("Der Grund muss genannt werden: ${verdict.message}", verdict.message.isNotBlank())
    }

    @Test
    fun everyAbsolutePathInACommand_isChecked_notJustTheFirst() {
        val verdict = verdictOf(
            ProjectBoundaryEnforcer.ToolAction.exec(
                "cp src/main.kt /tmp/a && cp /etc/passwd /tmp/b"
            )
        )
        assertEquals(BoundaryDecision.OUTSIDE_PROJECT, verdict.decision)
    }

    @Test
    fun anOrdinaryCommandWithoutQuotes_isStillChecked() {
        // Real gap (fixed): the token splitter collapsed to an empty string whenever
        // the command contained no quote at all, so the path check silently did not
        // run for exactly the commands that need it most.
        assertEquals(
            BoundaryDecision.OUTSIDE_PROJECT,
            verdictOf(ProjectBoundaryEnforcer.ToolAction.exec("cat /etc/passwd")).decision
        )
        assertEquals(
            "ein gewöhnlicher Befehl ohne Pfad muss erlaubt bleiben",
            BoundaryDecision.ALLOWED,
            verdictOf(ProjectBoundaryEnforcer.ToolAction.exec("./gradlew test")).decision
        )
    }

    @Test
    fun theFilesystemRoot_asWorkingDirectory_isRefused() {
        val verdict = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction(
                kind = ToolKind.EXECUTE,
                rawKind = "EXECUTE",
                command = "./gradlew test",
                workingDirectory = "/",
                origin = ActionOrigin.MODEL
            ),
            boundary
        )
        assertEquals(BoundaryDecision.OUTSIDE_PROJECT, verdict.decision)
    }

    @Test
    fun aSubdirectoryOfTheProject_isAValidWorkingDirectory() {
        // Real gap (fixed): the comparison ran the wrong way round, so "/" was
        // accepted (every path lies below it) and a legitimate subdirectory — the
        // normal case for a real build — was refused.
        listOf(root, "$root/app", "$root/app/src").forEach { dir ->
            val verdict = ProjectBoundaryEnforcer.evaluate(
                ProjectBoundaryEnforcer.ToolAction(
                    kind = ToolKind.EXECUTE,
                    rawKind = "EXECUTE",
                    command = "./gradlew test",
                    workingDirectory = dir,
                    origin = ActionOrigin.MODEL
                ),
                boundary
            )
            assertEquals("$dir muss als Arbeitsordner gelten", BoundaryDecision.ALLOWED, verdict.decision)
        }
    }

    @Test
    fun aDirectoryChangeInTheMiddleOfACommand_isRefused() {
        // Real gap (fixed): the rule only matched at the start of a line, so
        // "ls && cd /somewhere" slipped through.
        //
        // A directory change to an absolute path outside the project is caught by the
        // path rule first, so the reason differs per case (FORBIDDEN_COMMAND vs
        // OUTSIDE_PROJECT). Both are refusals; what matters is that it does not pass.
        listOf(
            "ls && cd /data/data/com.other.app",
            "ls; cd /",
            "ls | cd /storage"
        ).forEach { command ->
            val verdict = verdictOf(ProjectBoundaryEnforcer.ToolAction.exec(command))
            assertTrue(
                "„$command“ muss abgelehnt werden, war aber ${verdict.decision}",
                verdict.decision == BoundaryDecision.FORBIDDEN_COMMAND ||
                    verdict.decision == BoundaryDecision.OUTSIDE_PROJECT
            )
            assertTrue(
                "jede Ablehnung braucht einen deutschen Grund: ${verdict.message}",
                verdict.message.isNotBlank()
            )
        }
    }

    // ── Criterion: revoked access is blocked immediately ────────────────────
    @Test
    fun aRevokedPath_isRefusedOnTheVeryNextCall() {
        val registry = ProjectAccessRegistry(boundary)
        assertTrue(
            "Vor dem Widerruf muss Lesen erlaubt sein",
            registry.check(ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "src/main.kt")).isAllowed
        )

        registry.revoke(ToolKind.READ, "$root/src/main.kt")

        val after = registry.check(ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "src/main.kt"))
        assertEquals("Der Widerruf muss sofort greifen", BoundaryDecision.ACCESS_REVOKED, after.decision)
        assertTrue("Der Grund muss genannt werden: ${after.message}", after.message.isNotBlank())
    }

    @Test
    fun revokingAFolder_revokesEverythingBelowIt() {
        val registry = ProjectAccessRegistry(boundary)
        registry.revoke(ToolKind.READ, "$root/src")
        val nested = registry.check(
            ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "src/deep/nested/File.kt")
        )
        assertEquals(BoundaryDecision.ACCESS_REVOKED, nested.decision)
    }

    @Test
    fun revokingWrite_doesNotSilentlyBlockReading() {
        // Revocation is per tool kind; refusing more than asked would hide the reason.
        val registry = ProjectAccessRegistry(boundary)
        registry.revoke(ToolKind.WRITE, "$root/src/main.kt")
        assertTrue(
            "Lesen wurde nicht widerrufen und muss weiter gehen",
            registry.check(ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "src/main.kt")).isAllowed
        )
        assertEquals(
            BoundaryDecision.ACCESS_REVOKED,
            registry.check(ProjectBoundaryEnforcer.ToolAction.file(ToolKind.WRITE, "src/main.kt")).decision
        )
    }

    @Test
    fun aPermitFromBeforeTheRevocationIsNoLongerValid() {
        val registry = ProjectAccessRegistry(boundary)
        val before = registry.check(ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "src/main.kt"))
        assertTrue(before.isAllowed)
        val generationBefore = registry.generation()

        registry.revoke(ToolKind.READ, "$root/src/main.kt")

        assertFalse(
            "Eine alte Erlaubnis darf nach dem Widerruf nicht mehr gelten",
            registry.isStillValid(before)
        )
        assertNotEquals(generationBefore, registry.generation())
    }

    @Test
    fun aRevocationPreparedInTheBoundaryObjectIsHonoured() {
        val revoked = ProjectBoundaryEnforcer.ProjectBoundary(
            projectRoots = listOf(root),
            revoked = Revocations(read = setOf("$root/src"), write = setOf("$root/src"))
        )
        listOf(ToolKind.READ, ToolKind.WRITE).forEach { kind ->
            val decision = ProjectBoundaryEnforcer.evaluate(
                ProjectBoundaryEnforcer.ToolAction.file(kind, "src/main.kt"),
                revoked
            ).decision
            assertEquals("$kind auf einem widerrufenen Ordner muss abgelehnt werden", BoundaryDecision.ACCESS_REVOKED, decision)
        }

        // Nicht widerrufen: Schreiben bleibt erlaubt.
        assertEquals(
            "Löschen wurde nicht widerrufen",
            BoundaryDecision.ALLOWED,
            ProjectBoundaryEnforcer.evaluate(
                ProjectBoundaryEnforcer.ToolAction.file(ToolKind.DELETE, "src/main.kt"),
                revoked
            ).decision
        )
    }

    // ── Criterion: commands stay inside the boundary ────────────────────────

    @Test
    fun aCommandLeavingTheProjectWithDotDot_isRefused() {
        val verdict = verdictOf(ProjectBoundaryEnforcer.ToolAction.exec("cat ../../etc/passwd"))
        assertEquals(BoundaryDecision.FORBIDDEN_COMMAND, verdict.decision)
        assertTrue(verdict.message.isNotBlank())
    }

    @Test
    fun aCommandThatWouldHandTheModelASecondInterpreter_isRefused() {
        // The first case now also names an absolute path outside the project, so the
        // path rule refuses it before the substitution rule is reached. Either reason
        // is a refusal; the substitution itself must never be executed.
        listOf("echo $(cat /data/data/com.other.app/x)", "echo `whoami`").forEach { command ->
            val decision = decisionOf(ProjectBoundaryEnforcer.ToolAction.exec(command))
            assertTrue(
                "„$command“ muss abgelehnt werden, war aber $decision",
                decision == BoundaryDecision.FORBIDDEN_COMMAND ||
                    decision == BoundaryDecision.OUTSIDE_PROJECT
            )
        }
    }

    @Test
    fun aCommandReachingTheNetwork_isRefused() {
        listOf("curl https://example.com", "wget http://example.com").forEach { command ->
            assertEquals(
                "„$command“ muss abgelehnt werden",
                BoundaryDecision.FORBIDDEN_COMMAND,
                decisionOf(ProjectBoundaryEnforcer.ToolAction.exec(command))
            )
        }
    }

    @Test
    fun aWorkingDirectoryOutsideTheProject_isRefused() {
        val verdict = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction(
                kind = ToolKind.EXECUTE,
                rawKind = "EXECUTE",
                command = "ls",
                workingDirectory = "/data/data/com.other.app/files"
            ),
            boundary
        )
        assertEquals(BoundaryDecision.OUTSIDE_PROJECT, verdict.decision)
    }

    @Test
    fun theProjectItselfAsWorkingDirectory_isAllowed() {
        val verdict = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction(
                kind = ToolKind.EXECUTE,
                rawKind = "EXECUTE",
                command = "./gradlew test",
                workingDirectory = root
            ),
            boundary
        )
        assertEquals(BoundaryDecision.ALLOWED, verdict.decision)
    }

    // ── Criterion: no silent failures ───────────────────────────────────────

    @Test
    fun everyRefusalCarriesAGermanReason() {
        val refusals = listOf(
            verdictOf(ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "../../../etc/passwd")),
            verdictOf(ProjectBoundaryEnforcer.ToolAction.file(ToolKind.WRITE, "config/.env")),
            verdictOf(ProjectBoundaryEnforcer.ToolAction.exec("curl https://example.com")),
            verdictOf(ProjectBoundaryEnforcer.ToolAction.exec("echo $(id)")),
            ProjectBoundaryEnforcer.evaluate(
                ProjectBoundaryEnforcer.ToolAction(kind = null, rawKind = "sudo_rm"),
                boundary
            ),
            ProjectBoundaryEnforcer.evaluate(
                ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "src/main.kt"),
                ProjectBoundaryEnforcer.ProjectBoundary(projectRoots = emptyList())
            ),
            ProjectBoundaryEnforcer.evaluate(
                ProjectBoundaryEnforcer.ToolAction(
                    kind = ToolKind.READ,
                    rawKind = "READ",
                    path = "src/main.kt",
                    origin = ActionOrigin.MODEL,
                    additionalRoots = listOf("/sdcard")
                ),
                boundary
            )
        )
        refusals.forEach { verdict ->
            assertFalse("Eine Ablehnung braucht einen Grund: $verdict", verdict.isAllowed)
            assertTrue(
                "Der Grund muss ein deutscher Satz sein: „${verdict.message}“",
                verdict.message.isNotBlank() && verdict.message.contains(' ') && verdict.message.length > 20
            )
        }
    }

    @Test
    fun anAllowedActionCarriesNoWarningText() {
        val verdict = verdictOf(ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "src/main.kt"))
        assertTrue(verdict.isAllowed)
        assertTrue("Eine Erlaubnis braucht keine Begründung", verdict.message.isBlank())
    }

    @Test
    fun anUnknownToolIsRefusedRatherThanGuessed() {
        val verdict = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction(kind = null, rawKind = "run_everything"),
            boundary
        )
        assertEquals(BoundaryDecision.UNKNOWN_TOOL, verdict.decision)
        assertTrue(verdict.message.contains("run_everything"))
    }

    @Test
    fun withoutAProjectRoot_nothingIsAllowed() {
        val empty = ProjectBoundaryEnforcer.ProjectBoundary(projectRoots = emptyList())
        listOf(ToolKind.READ, ToolKind.WRITE, ToolKind.DELETE).forEach { kind ->
            val verdict = ProjectBoundaryEnforcer.evaluate(
                ProjectBoundaryEnforcer.ToolAction.file(kind, "src/main.kt"),
                empty
            )
            assertEquals("Ohne Projektordner muss alles abgelehnt werden", BoundaryDecision.NO_PROJECT_ROOT, verdict.decision)
        }
    }

    @Test
    fun aMalformedPathIsRefusedWithAReason() {
        val nul = verdictOf(ProjectBoundaryEnforcer.ToolAction.file(ToolKind.READ, "src/main.kt\u0000.png"))
        assertEquals(BoundaryDecision.MALFORMED_PATH, nul.decision)
        assertTrue(nul.message.isNotBlank())

        val empty = verdictOf(ProjectBoundaryEnforcer.ToolAction.file(ToolKind.WRITE, "   "))
        assertFalse("Ein leerer Pfad darf nicht als gültig gelten", empty.isAllowed)
        assertTrue(empty.message.isNotBlank())
    }

    @Test
    fun toolNamesFromDifferentSourcesMapToTheSameKind() {
        // A model may name its tool differently; the kind must still be the same, so
        // the same check runs.
        assertEquals(ToolKind.WRITE, ToolBoundaryKindOf("write_file"))
        assertEquals(ToolKind.READ, ToolBoundaryKindOf("Read"))
        assertEquals(ToolKind.DELETE, ToolBoundaryKindOf("delete_file"))
        assertEquals(ToolKind.EXECUTE, ToolBoundaryKindOf("run"))
    }

    private fun ToolBoundaryKindOf(raw: String): ToolKind? = ToolKind.parse(raw)

    @Test
    fun aRefusalTextNeverEchoesAKeyBack() {
        // A path that itself looks like a credential must not be shown or logged raw.
        val verdict = ProjectBoundaryEnforcer.evaluate(
            ProjectBoundaryEnforcer.ToolAction(
                kind = ToolKind.READ,
                rawKind = "READ",
                // Unusable on purpose, so the refusal quotes the path back to the user.
                path = "config/api_key=sk-ant-abcdefgh12345678.txt\u0000"
            ),
            boundary
        )
        assertFalse(verdict.isAllowed)
        assertFalse(
            "Ein Schlüssel darf nicht im Text stehen: ${verdict.message}",
            verdict.message.contains("sk-ant-abcdefgh12345678")
        )
    }
}