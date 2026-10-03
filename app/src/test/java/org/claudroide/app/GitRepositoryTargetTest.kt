package org.claudroide.app

import org.claudroide.app.feature.git.RemoteRepository
import org.claudroide.app.feature.git.RemoteRepositoryAudit
import org.claudroide.app.feature.git.RemoteRepositoryPlan
import org.claudroide.app.feature.git.RepositoryDecision
import org.claudroide.app.feature.git.RepositoryGate
import org.claudroide.app.feature.git.RepositoryLicense
import org.claudroide.app.feature.git.RepositoryVisibility
import org.claudroide.app.feature.git.VisibilityAudit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Task 097 — "Privates GitHub-Projekt".
 *
 * Two failures worth guarding: a target whose privacy is *assumed* rather than
 * measured, and a target that gets created because nobody confirmed anything.
 */
class GitRepositoryTargetTest {

    private val privat = RemoteRepository(
        owner = "mertgoevse-wq",
        repositoryName = "claudroide",
        visibility = RepositoryVisibility.PRIVATE,
        license = RepositoryLicense.NONE
    )

    private fun audit(
        repository: RemoteRepository = privat,
        foundOnServer: Boolean = true,
        visibilityVerifiedOnDevice: Boolean = true,
        collaborators: List<String> = emptyList()
    ) = RemoteRepositoryAudit(repository, foundOnServer, visibilityVerifiedOnDevice, collaborators)

    private fun plan(
        repository: RemoteRepository = privat,
        exists: Boolean = true,
        confirmed: Boolean = true
    ) = RemoteRepositoryPlan(repository = repository, repositoryExists = exists, creationConfirmedByUser = confirmed)

    private fun decision(plan: RemoteRepositoryPlan, audit: RemoteRepositoryAudit?) =
        RepositoryGate.mayProceed(plan, audit)

    // ── Fertig-Bedingung 1: ausdrueckliche Bestaetigung ──────────────────────

    @Test
    fun `ohne Bestaetigung wird nichts angelegt`() {
        val d = decision(plan(confirmed = false), audit())
        assertTrue(d is RepositoryDecision.NotConfirmed)
    }

    @Test
    fun `die Bestaetigung wird aus nichts abgeleitet`() {
        // Every other ingredient of a ready plan is present here.
        val vollstaendig = plan(exists = true, confirmed = false)
        assertTrue(decision(vollstaendig, audit()) is RepositoryDecision.NotConfirmed)
    }

    @Test
    fun `ein bestehendes Ziel braucht keine Erstellungsbestaetigung fuer die Arbeit`() {
        // exists = true is a fact about the world, not a permission.
        val d = decision(plan(exists = true, confirmed = false), audit())
        assertTrue(d is RepositoryDecision.NotConfirmed)
        assertTrue((d as RepositoryDecision.NotConfirmed).reason.contains("besteht bereits"))
    }

    @Test
    fun `der Vorgabewert der Bestaetigung ist ablehnend`() {
        // Constructed without the confirmation argument on purpose.
        val ohneAngabe = RemoteRepositoryPlan(repository = privat, repositoryExists = true)
        assertFalse(ohneAngabe.creationConfirmed)
        assertTrue(decision(ohneAngabe, audit()) is RepositoryDecision.NotConfirmed)
    }

    @Test
    fun `mit Bestaetigung und gepruefter Privatheit geht es weiter`() {
        assertTrue(decision(plan(), audit()) is RepositoryDecision.MayProceed)
    }

    // ── Fertig-Bedingung 2: Privatheit muss am Geraet geprueft sein ──────────

    @Test
    fun `ohne Geraetpruefung bleibt die Privatheit ungeprueft`() {
        val a = audit(visibilityVerifiedOnDevice = false)
        assertTrue(RepositoryGate.auditBeforeFirstUpload(a) is VisibilityAudit.Pending)
    }

    @Test
    fun `der Absichtswert privat ersetzt die Messung nicht`() {
        // visibility = PRIVATE, but never checked on the device.
        val a = audit(visibilityVerifiedOnDevice = false)
        val d = decision(plan(), a)
        assertTrue(d is RepositoryDecision.VisibilityNotChecked)
        assertFalse(decision(plan(), a) is RepositoryDecision.MayProceed)
    }

    @Test
    fun `ohne Pruefbericht wird nicht weitergearbeitet`() {
        assertTrue(decision(plan(), null) is RepositoryDecision.VisibilityNotChecked)
    }

    @Test
    fun `ein oeffentliches Ziel gilt nicht als privat`() {
        val oeffentlich = privat.copy(visibility = RepositoryVisibility.PUBLIC)
        val a = audit(repository = oeffentlich, visibilityVerifiedOnDevice = true)
        assertTrue(RepositoryGate.auditBeforeFirstUpload(a) is VisibilityAudit.Pending)
        assertFalse(decision(plan(repository = oeffentlich), a) is RepositoryDecision.MayProceed)
    }

    @Test
    fun `ein am Geraet nicht gefundenes Ziel wird nicht als privat behandelt`() {
        val a = audit(foundOnServer = false, visibilityVerifiedOnDevice = false)
        val pruefung = RepositoryGate.auditBeforeFirstUpload(a)
        assertTrue(pruefung is VisibilityAudit.Pending)
        assertTrue((pruefung as VisibilityAudit.Pending).reason.contains("nicht gefunden"))
    }

    @Test
    fun `eine gepruefte Privatheit nennt den gemeldeten Zustand`() {
        val pruefung = RepositoryGate.auditBeforeFirstUpload(audit())
        assertTrue(pruefung is VisibilityAudit.Verified)
        assertEquals(RepositoryVisibility.PRIVATE, (pruefung as VisibilityAudit.Verified).visibility)
    }

    @Test
    fun `die gepruefte Meldung nennt den Zugangsumfang`() {
        val allein = RepositoryGate.auditBeforeFirstUpload(audit())
        assertTrue((allein as VisibilityAudit.Verified).message.contains("nur der Eigentümer"))
    }

    @Test
    fun `weitere Zugangsnamen werden im Bericht genannt`() {
        val mitTeam = RepositoryGate.auditBeforeFirstUpload(
            audit(collaborators = listOf("mertgoevse-wq", "team"))
        )
        assertTrue((mitTeam as VisibilityAudit.Verified).message.contains("mehrere Zugänge"))
    }

    @Test
    fun `ein leerer Zugangsname wird abgewiesen`() {
        val fehler = runCatching { audit(collaborators = listOf("  ")) }
        assertTrue(fehler.isFailure)
    }

    @Test
    fun `es gibt keinen dritten Zustand der Sichtbarkeitspruefung`() {
        // "vermutlich privat" darf nicht existieren. Gezählt über die Klassen, die
        // das interface selbst als direkt bekennen — kein kotlin-reflect noetig.
        val zustaende = VisibilityAudit::class.java.declaredClasses.filter {
            VisibilityAudit::class.java.isAssignableFrom(it)
        }
        assertEquals(
            listOf("Pending", "Verified"),
            zustaende.map { it.simpleName }.sorted()
        )
    }

    // ── Das Ziel selbst ─────────────────────────────────────────────────────

    @Test
    fun `der Anzeigename ist Eigentuemer und Name`() {
        assertEquals("mertgoevse-wq/claudroide", privat.displayName())
    }

    @Test
    fun `die Klonzieladresse enthaelt den Namen, nicht einen erfundenen Pfad`() {
        assertEquals("https://github.com/mertgoevse-wq/claudroide.git", privat.httpsCloneUrl())
    }

    @Test
    fun `ein Repositoryname darf nicht aus seinem Ordner ausbrechen`() {
        assertTrue(runCatching { privat.copy(repositoryName = "..") }.isFailure)
        assertTrue(runCatching { privat.copy(repositoryName = "a/../../b") }.isFailure)
    }

    @Test
    fun `ein leerer Name wird abgewiesen`() {
        assertTrue(runCatching { privat.copy(repositoryName = " ") }.isFailure)
    }

    @Test
    fun `ein leerer Eigentuemer wird abgewiesen`() {
        assertTrue(runCatching { privat.copy(owner = "") }.isFailure)
    }

    @Test
    fun `die Sichtbarkeit hat keinen Vorgabewert`() {
        // There is no `DEFAULT` entry: unknown visibility must be spelled out.
        assertEquals(2, RepositoryVisibility.values().size)
        assertFalse(RepositoryVisibility.entries.any { it.name == "DEFAULT" })
    }

    @Test
    fun `keine Lizenz ist ein eigener Wert und nicht unbekannt`() {
        assertTrue(runCatching { privat.copy(license = RepositoryLicense.NONE) }.isSuccess)
        assertFalse(RepositoryLicense.NONE == RepositoryLicense.UNKNOWN)
        assertEquals("keine", RepositoryLicense.NONE.germanLabel)
    }

    @Test
    fun `der Standardzweig kann abweichen und wird angezeigt`() {
        assertEquals("main", privat.defaultBranch)
        assertTrue(runCatching { privat.copy(defaultBranch = "") }.isFailure)
    }

    @Test
    fun `nur der Eigentuemer ist der engste Umfang`() {
        assertTrue(audit(collaborators = emptyList()).isOwnerOnly)
        assertFalse(audit(collaborators = listOf("team")).isOwnerOnly)
    }

    // ── Diese Datei kann nichts anlegen ─────────────────────────────────────

    @Test
    fun `das Gate bietet keine Methode, die ein Repository anlegt`() {
        val verboten = listOf("create", "createRepository", "makeRepository", "provision", "init")
        val methoden = RepositoryGate::class.java.methods.map { it.name.lowercase() }
        verboten.forEach { name ->
            assertFalse("$name darf es nicht geben", methoden.any { it.contains(name) })
        }
    }

    @Test
    fun `das Ziel fuehrt keine Netz- oder Dateizugriffe aus`() {
        // Pure data: no member holds a Context, a URL connection, or a File.
        val verboten = listOf("java.io.File", "java.net.URL", "okhttp3", "retrofit2")
        val felder = RemoteRepository::class.java.declaredFields +
            RemoteRepositoryAudit::class.java.declaredFields +
            RemoteRepositoryPlan::class.java.declaredFields
        felder.forEach { feld ->
            assertFalse(
                "Feld ${feld.name} vom Typ ${feld.type.name}",
                verboten.any { feld.type.name.contains(it) }
            )
        }
    }

    @Test
    fun `das Gate ist ein reines Objekt ohne Zustand zwischen Aufrufen`() {
        assertTrue(Modifier.isFinal(RepositoryGate::class.java.modifiers))
        assertEquals(
            0,
            RepositoryGate::class.java.declaredFields.count { !Modifier.isStatic(it.modifiers) }
        )
    }
}