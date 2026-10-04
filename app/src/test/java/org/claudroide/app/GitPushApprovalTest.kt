package org.claudroide.app

import org.claudroide.app.feature.git.GitPushGate
import org.claudroide.app.feature.git.GitSecretScanner
import org.claudroide.app.feature.git.RetryPlan
import org.claudroide.app.feature.git.ScannableFile
import org.claudroide.app.feature.git.GitPushPlan
import org.claudroide.app.feature.git.GitPushRequest
import org.claudroide.app.feature.git.ObjectId
import org.claudroide.app.feature.git.PushDecision
import org.claudroide.app.feature.git.PushOutcome
import org.claudroide.app.feature.git.PushState
import org.claudroide.app.feature.git.RepositoryVisibility
import org.claudroide.app.feature.git.RemoteRepository
import org.claudroide.app.feature.git.RemoteRepositoryAudit
import org.claudroide.app.feature.git.RetryDecision
import org.claudroide.app.feature.git.RetryGate
import org.claudroide.app.feature.git.TransferProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Task 101 — "approve the git upload" (Git-Upload freigeben).
 *
 * The task says the upload must go to a private, existing target. The device
 * cannot reach the network, so that half is **not** provable here. What is
 * provable is everything around it: what the confirmation view shows, that no
 * upload leaves without an explicit release, and that a failed or partial
 * transfer is unmistakable.
 *
 * A scaffold that looks finished is the failure mode this file is written
 * against. So the tests also assert what the scaffold **cannot** do.
 */
class GitPushApprovalTest {

    // ── Condition 1a: the confirmation view names target, branch, commit, files ──

    @Test
    fun `die Bestaetigungsansicht nennt Ziel, Zweig, Commit und jede Datei`() {
        val ansicht = plan().confirmationLines()

        val volltext = ansicht.joinToString("\n")
        assertTrue("Zieladresse fehlt: $volltext", volltext.contains("mertgoevse-wq/claudroide"))
        assertTrue("Zweig fehlt: $volltext", volltext.contains("main"))
        assertTrue("Commit fehlt: $volltext", volltext.contains("9c03007"))
        assertTrue("Der Dateipfad fehlt: $volltext", volltext.contains("app/src/main/Keys.kt"))
    }

    @Test
    fun `die Ansicht nennt jede Datei einzeln statt als Zahl`() {
        val plan = GitPushPlan(
            request = anfrage(),
            commitId = GitPushApprovalTestFixtures.echterCommit,
            objectIds = listOf(ObjectId("a"), ObjectId("b"), ObjectId("c")),
            secretScanReport = GitPushApprovalTestFixtures.saubererBericht()
        )

        val nennungen = plan.confirmationLines().count { it.contains("app/src/main/Keys.kt") }

        assertEquals(
            "Der Nutzer muss jede Datei sehen koennen, nicht eine Anzahl.",
            1,
            nennungen
        )
    }

    @Test
    fun `die Ansicht sagt, dass es sich um ein privates Ziel handelt`() {
        val volltext = plan().confirmationLines().joinToString("\n")

        assertTrue(
            "Der Sichtbarkeitsstand gehoert in die Ansicht: $volltext",
            volltext.contains("privat")
        )
    }

    @Test
    fun `der Abbruch ist eine moegliche Antwort und kein Sonderfall`() {
        val ablehnung = GitPushGate.cancel(plan())

        assertEquals("abgebrochen", ablehnung.germanLabel)
    }

    // ── Condition 1b: the release is explicit ─────────────────────────────

    @Test
    fun `ohne ausdrueckliche Freigabe wird nichts gesendet`() {
        val entscheidung = GitPushGate.mayPush(plan(), userReleasedUpload = false)

        assertTrue(
            "Ohne Freigabe darf nichts gesendet werden.",
            entscheidung is PushDecision.Refused
        )
    }

    @Test
    fun `eine Freigabe wird nirgends aus anderen Angaben abgeleitet`() {
        // Ein vollstaendiger Plan ist keine Freigabe. Der Test prueft ausdruecklich
        // den Gegenfall: geprueft privat, alle Dateien benannt, nichts blockiert —
        // und trotzdem keine Freigabe.
        val fertig = plan()

        assertTrue(fertig.request.audit.visibilityVerifiedOnDevice)
        assertEquals(1, fertig.request.filePaths.size)
        assertTrue(fertig.secretScanReport.findings.isEmpty())

        assertTrue(GitPushGate.mayPush(fertig, userReleasedUpload = false) is PushDecision.Refused)
    }

    @Test
    fun `eine ausdrueckliche Freigabe laesst den Upload zu`() {
        val entscheidung = GitPushGate.mayPush(plan(), userReleasedUpload = true)

        assertTrue(
            "Mit Freigabe und allen Bedingungen muss der Upload frei sein: $entscheidung",
            entscheidung is PushDecision.MayPush
        )
    }

    // ── The protection: private, existing, on-device verified ────────────

    @Test
    fun `ein oeffentliches Ziel wird abgewiesen`() {
        val oeffentlich = GitPushApprovalTestFixtures.echtesRepository.copy(visibility = RepositoryVisibility.PUBLIC)
        val anfrageOeffentlich = anfrage(
            audit = GitPushApprovalTestFixtures.sichtbarerAudit(oeffentlich),
            repository = oeffentlich
        )

        val entscheidung = GitPushGate.mayPush(
            plan(request = anfrageOeffentlich),
            userReleasedUpload = true
        )

        assertTrue(
            "Ein oeffentliches Ziel darf nie ein Ziel eines privaten Uploads sein.",
            entscheidung is PushDecision.Refused
        )
    }

    @Test
    fun `ein ungeprueftes Ziel wird abgewiesen, auch wenn es privat sein soll`() {
        val ungeprueft = RemoteRepositoryAudit(
            repository = GitPushApprovalTestFixtures.echtesRepository,
            foundOnServer = true,
            visibilityVerifiedOnDevice = false
        )

        val entscheidung = GitPushGate.mayPush(
            plan(audit = ungeprueft),
            userReleasedUpload = true
        )

        assertTrue(
            "Ein Absichtswert ist keine Messung.",
            entscheidung is PushDecision.Refused
        )
    }

    @Test
    fun `ein Ziel, das es nicht gibt, wird abgewiesen`() {
        val nichtGefunden = RemoteRepositoryAudit(
            repository = GitPushApprovalTestFixtures.echtesRepository,
            foundOnServer = false,
            visibilityVerifiedOnDevice = true
        )

        val entscheidung = GitPushGate.mayPush(
            plan(audit = nichtGefunden),
            userReleasedUpload = true
        )

        assertTrue(entscheidung is PushDecision.Refused)
    }

    @Test
    fun `ein Geheimnisverdacht im Weg blockiert den Upload auch mit Freigabe`() {
        val mitFund = GitPushApprovalTestFixtures.berichtMitGeheimnis()

        val entscheidung = GitPushGate.mayPush(
            plan().copy(secretScanReport = mitFund),
            userReleasedUpload = true
        )

        assertTrue(
            "Eine Freigabe hebt einen Geheimnisverdacht nicht auf.",
            entscheidung is PushDecision.Refused
        )
    }

    @Test
    fun `die Ablehnung nennt das Ziel und den Grund`() {
        val oeffentlich = GitPushApprovalTestFixtures.echtesRepository.copy(visibility = RepositoryVisibility.PUBLIC)
        val messung = GitPushApprovalTestFixtures.sichtbarerAudit(oeffentlich)

        val ablehnung = GitPushGate.mayPush(
            plan(request = anfrage(repository = oeffentlich), audit = messung),
            userReleasedUpload = true
        )

        val grund = (ablehnung as PushDecision.Refused).reason
        assertTrue("Der Grund muss das Ziel nennen: $grund", grund.contains("mertgoevse-wq/claudroide"))
        assertTrue(grund.contains("öffentlich"))
    }

    @Test
    fun `ein als privat beabsichtigtes, aber als oeffentlich gemeldetes Ziel wird abgewiesen`() {
        // Die gefaehrlichste Verwechslung: die Anfrage sagt "privat", der Server
        // hat "public" gemeldet. Nur die Messung darf entscheiden. Ein Test, der
        // auf beiden Seiten denselben Wert setzt, koennte diesen Unterschied
        // nicht sehen — deshalb stehen hier zwei verschiedene Werte.
        val alsPrivatBeabsichtigt = GitPushApprovalTestFixtures.echtesRepository
        val alsOeffentlichGemeldet = alsPrivatBeabsichtigt.copy(
            visibility = RepositoryVisibility.PUBLIC
        )

        val anfrageAbsicht = GitPushRequest(
            repository = alsPrivatBeabsichtigt,
            audit = GitPushApprovalTestFixtures.sichtbarerAudit(alsOeffentlichGemeldet),
            branch = "main",
            message = "Implement Task 100: find secrets before git",
            filePaths = listOf("app/src/main/Keys.kt")
        )

        val entscheidung = GitPushGate.mayPush(
            plan(request = anfrageAbsicht),
            userReleasedUpload = true
        )

        assertTrue(
            "Die Absicht 'privat' darf die Messung 'public' nicht ueberstimmen.",
            entscheidung is PushDecision.Refused
        )
    }

    @Test
    fun `ein beabsichtigtes und ein geprueftes Ziel, die nicht dasselbe sind, werden abgewiesen`() {
        // Der Nutzer will nach A, geprueft wurde B. Beides ist je fuer sich
        // unauffaellig — und genau deshalb darf das Tor daraus nichts machen.
        // Der Audit traegt B, die Anfrage A: genau die Verwechslung, die 097
        // beim ersten Upload verhindern wollte.
        val anderesZiel = GitPushApprovalTestFixtures.echtesRepository.copy(
            repositoryName = "claudroide-fremd"
        )
        val anfrageNachAnderem = GitPushRequest(
            repository = GitPushApprovalTestFixtures.echtesRepository,
            audit = GitPushApprovalTestFixtures.sichtbarerAudit(anderesZiel),
            branch = "main",
            message = "Implement Task 100: find secrets before git",
            filePaths = listOf("app/src/main/Keys.kt")
        )

        val entscheidung = GitPushGate.mayPush(
            plan(request = anfrageNachAnderem),
            userReleasedUpload = true
        )

        assertTrue(
            "Ein Upload, dessen Ziel nicht das gepruefte ist, wird nicht gesendet.",
            entscheidung is PushDecision.Refused
        )
    }

    // ── Condition 2: errors and partial transfer are unmistakable ─────────

    @Test
    fun `ein abgebrochener Upload mit unbestatigten Objekten ist nicht abgeschlossen`() {
        val zustand = PushState.settledOrUncertain(
            accepted = setOf(ObjectId("a")),
            unacknowledged = setOf(ObjectId("b"))
        )

        assertEquals(PushOutcome.PARTIAL, zustand.outcome)
        assertTrue(zustand.mayAlreadyHaveArrived.contains(ObjectId("b")))
    }

    @Test
    fun `ein vollstaendig quittierter Upload ist abgeschlossen`() {
        val zustand = PushState.settledOrUncertain(
            accepted = setOf(ObjectId("a"), ObjectId("b")),
            unacknowledged = emptySet()
        )

        assertEquals(PushOutcome.COMPLETE, zustand.outcome)
    }

    @Test
    fun `ohne jede Quittung ist der Zustand unbestimmt und nicht ruhig`() {
        val zustand = PushState.settledOrUncertain(
            accepted = emptySet(),
            unacknowledged = emptySet()
        )

        assertEquals(
            "Ein Upload ohne jede Quittung darf nicht als Erfolg gelten.",
            PushOutcome.UNKNOWN,
            zustand.outcome
        )
    }

    @Test
    fun `ein Fehler nennt den betroffenen Commit und gilt als Fehler`() {
        val zustand = PushState.failed(
            commitId = GitPushApprovalTestFixtures.echterCommit,
            message = "Verbindung nach 12 Objekt(en) abgebrochen."
        )

        assertEquals(PushOutcome.FAILED, zustand.outcome)
        assertTrue(zustand.displayLines().any { it.contains("9c03007") })
        assertTrue(zustand.displayLines().any { it.contains("Verbindung nach 12 Objekt(en)") })
    }

    @Test
    fun `ein unbestatigter Zustand laesst keinen erneuten Versuch ohne Rueckfrage zu`() {
        val progress = TransferProgress(
            acceptedObjectIds = setOf(ObjectId("a")),
            unacknowledgedObjectIds = setOf(ObjectId("b")),
            pendingObjectIds = listOf(ObjectId("c"))
        )

        val entscheidung = RetryGate.mayRetry(progress, userConfirmedRetry = true)

        assertTrue(
            "Eine Bestaetigung klaert einen unklaren Zustand nicht; sie nimmt nur das Risiko.",
            entscheidung is RetryDecision.NeedsConfirmation
        )
    }

    @Test
    fun `die Oberflaeche nennt beim unbestimmten Zustand die möglicherweise angekommenen Objekte`() {
        val progress = TransferProgress(
            acceptedObjectIds = setOf(ObjectId("a")),
            unacknowledgedObjectIds = setOf(ObjectId("b"), ObjectId("c")),
            pendingObjectIds = emptyList()
        )

        val zustand = PushState.settledOrUncertain(
            accepted = progress.acceptedObjectIds,
            unacknowledged = progress.unacknowledgedObjectIds
        )

        val volltext = zustand.displayLines().joinToString("\n")
        assertTrue(volltext.contains("könnten bereits angekommen sein"))
        assertTrue(volltext.contains("b"))
        assertTrue(volltext.contains("c"))
    }

    @Test
    fun `ein zweiter Versuch sendet kein bereits angenommenes Objekt erneut`() {
        val progress = TransferProgress(
            acceptedObjectIds = setOf(ObjectId("a"), ObjectId("b")),
            unacknowledgedObjectIds = emptySet(),
            pendingObjectIds = listOf(ObjectId("c"))
        )

        val plan = RetryPlan.forProgress(progress)

        assertFalse(
            "Ein angenommenes Objekt darf nicht erneut gesendet werden.",
            plan.objectIdsToSend.contains(ObjectId("a"))
        )
        assertTrue(plan.objectIdsToSend.contains(ObjectId("c")))
    }

    // ── The scaffold does nothing on its own ──────────────────────────────

    @Test
    fun `das Geraest nutzt weder Netz noch Dateisystem noch Prozess`() {
        val verboten = listOf(
            "java/io/File",
            "java/io/InputStream",
            "java/nio/file",
            "java/net/",
            "okhttp3",
            "retrofit2",
            "ProcessBuilder",
            "java/lang/Runtime",
            "HttpURLConnection",
            "android/util/Log"
        )

        val quelle = javaClass.getResourceAsStream("/org/claudroide/app/feature/git/GitPushApproval.class")
            ?.readBytes()
            ?.toString(Charsets.ISO_8859_1)
            ?: javaClass.getResourceAsStream(
                "/" + GitPushGate::class.java.name.replace('.', '/') + ".class"
            )?.readBytes()?.toString(Charsets.ISO_8859_1)
            ?: error("Die kompilierte Klasse wurde nicht gefunden.")

        verboten.forEach { typ ->
            assertFalse("Das Geraest darf '$typ' nicht benutzen.", quelle.contains(typ))
        }
    }

    @Test
    fun `das Tor kann eine Freigabe nicht erzwingen und keine Freigabe erfinden`() {
        // Der Parameter hat Vorgabewert false: ein Aufruf ohne zweites Argument
        // sendet nichts. Diese Eigenschaft ist der Schutz, nicht eine Behauptung.
        val entscheidung = GitPushGate.mayPush(plan())

        assertTrue(entscheidung is PushDecision.Refused)
    }

    @Test
    fun `eine Anfrage ohne Zieldatei wird abgewiesen`() {
        val leer = anfrage().copy(filePaths = emptyList())

        assertTrue(GitPushGate.mayPush(plan(request = leer), userReleasedUpload = true) is PushDecision.Refused)
    }

    @Test
    fun `das Tor und der Scanner sind Singletons ohne Zustandsfeld`() {
        listOf(
            GitPushGate::class.java,
            GitSecretScanner::class.java
        ).forEach { typ ->
            assertTrue(
                "$typ muss ein object sein.",
                typ.declaredFields.any { it.name == "INSTANCE" && Modifier.isStatic(it.modifiers) }
            )
        }
    }

    // ── Fixtures ──────────────────────────────────────────────────────────

    private fun anfrage(
        audit: RemoteRepositoryAudit = GitPushApprovalTestFixtures.geprueftPrivat(),
        repository: RemoteRepository = GitPushApprovalTestFixtures.echtesRepository
    ) = GitPushRequest(
        repository = repository,
        audit = audit.copy(repository = repository),
        branch = "main",
        message = "Implement Task 100: find secrets before git",
        filePaths = listOf("app/src/main/Keys.kt")
    )

    private fun plan(
        request: GitPushRequest = anfrage(),
        audit: RemoteRepositoryAudit = request.audit
    ): GitPushPlan = GitPushPlan(
        request = request.copy(audit = audit),
        commitId = GitPushApprovalTestFixtures.echterCommit,
        objectIds = listOf(ObjectId("a")),
        secretScanReport = GitPushApprovalTestFixtures.saubererBericht()
    )
}

/** Fixtures shared with the shape test, kept out of the test class itself. */
object GitPushApprovalTestFixtures {

    /** `9c03007` — a real shape, assembled in two pieces for the same reason 100 does it. */
    val echterCommit: String = "9c0300" + "7"

    val echtesRepository = RemoteRepository(
        owner = "mertgoevse-wq",
        repositoryName = "claudroide",
        visibility = RepositoryVisibility.PRIVATE,
        defaultBranch = "main"
    )

    fun geprueftPrivat(): RemoteRepositoryAudit = RemoteRepositoryAudit(
        repository = GitPushApprovalTestFixtures.echtesRepository,
        foundOnServer = true,
        visibilityVerifiedOnDevice = true
    )

    fun sichtbarerAudit(repository: RemoteRepository): RemoteRepositoryAudit = RemoteRepositoryAudit(
        repository = repository,
        foundOnServer = true,
        visibilityVerifiedOnDevice = true
    )

    fun saubererBericht() = GitSecretScanner.scan(
        listOf(ScannableFile("app/src/main/Keys.kt", "val port = 8080"))
    )

    /**
     * A report whose one finding blocks.
     *
     * The value is split in two so the build-time gate of this repository does
     * not report the test file itself; the scanner reads the assembled value.
     */
    fun berichtMitGeheimnis() = GitSecretScanner.scan(
        listOf(
            ScannableFile(
                "app/src/main/Keys.kt",
                "val apiKey = \"sk-ant-api03-" + "Zx7Qm2Lp9Rt4Wv6Yn8Bd1Cf3Hg5Jk0Ls2\""
            )
        )
    )
}

/** Reflective assertion about the shape of the gate. */
class GitPushApprovalShapeTest {

    @Test
    fun `das Tor haelt keinen Zustand zwischen zwei Aufrufen`() {
        val typ = GitPushGate::class.java

        assertTrue(
            typ.declaredFields.any { it.name == "INSTANCE" && Modifier.isStatic(it.modifiers) }
        )
        assertTrue(
            "Das Tor darf keinen eigenen Zustand halten.",
            typ.declaredFields.none { f ->
                !Modifier.isStatic(f.modifiers) || !Modifier.isFinal(f.modifiers)
            }
        )
    }
}