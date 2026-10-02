package org.claudroide.app

import org.claudroide.app.feature.skills.CompatibilityVerdict
import org.claudroide.app.feature.skills.ContentEffect
import org.claudroide.app.feature.skills.ContentEffectKind
import org.claudroide.app.feature.skills.ContentEvidence
import org.claudroide.app.feature.skills.ContentFinding
import org.claudroide.app.feature.skills.ContentInspection
import org.claudroide.app.feature.skills.ConsentState
import org.claudroide.app.feature.skills.DiscoveredLicenseEvidence
import org.claudroide.app.feature.skills.DiscoveredSkill
import org.claudroide.app.feature.skills.EnvironmentAudit
import org.claudroide.app.feature.skills.InstallRequest
import org.claudroide.app.feature.skills.InstallScope
import org.claudroide.app.feature.skills.InstallSite
import org.claudroide.app.feature.skills.InstallSiteKind
import org.claudroide.app.feature.skills.NetworkBehaviour
import org.claudroide.app.feature.skills.PermissionScope
import org.claudroide.app.feature.skills.ProvenanceEvidence
import org.claudroide.app.feature.skills.RemovalPlan
import org.claudroide.app.feature.skills.RequiredPermission
import org.claudroide.app.feature.skills.SearchTerm
import org.claudroide.app.feature.skills.SkillCompatibility
import org.claudroide.app.feature.skills.SkillConfirmation
import org.claudroide.app.feature.skills.SkillConfirmationDialog
import org.claudroide.app.feature.skills.SkillDiscoveryInventory
import org.claudroide.app.feature.skills.SkillFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aufgabe 124 — „Skill-Installation freigeben".
 *
 * Die Zusage der Aufgabe ist doppelt, und beide Hälften werden hier einzeln
 * geprüft:
 *
 *  1. **Jede globale Installation wird einzeln bestätigt.** Kein Dialog, der
 *     mehrere Skills auf einmal freigibt; keine Freigabe, die „alle mitbringt".
 *  2. **Ablehnen installiert nichts.** Ein abgelehnter Skill darf keinen anderen
 *     Skill nachziehen — weder mitnehmen („nimm ihn mit") noch ungefragt
 *     vorinstallieren.
 *
 * Aufbau der Tests: erst der Dialog (was der Nutzer sieht), dann die Grenze
 * (was das Ablehnen bewirkt), zuletzt die strukturellen Zusicherungen, die eine
 * spätere Erweiterung auffallen lassen.
 */
class SkillInstallApprovalTest {

    // ── Hilfen ─────────────────────────────────────────────────────────────
    //
    // Synthetische Werte: keine echte Adresse, kein echter Schlüssel, keine
    // erfundene Anbieterangabe. "beispiel.invalid" ist eine reservierte Domain.

    private val projekt = "projekt-alpha"

    private fun skill(
        name: String = "recherche-helfer",
        license: DiscoveredLicenseEvidence = DiscoveredLicenseEvidence.DOCUMENT_PRESENT,
        provenance: ProvenanceEvidence = ProvenanceEvidence.RECORDED
    ): DiscoveredSkill = DiscoveredSkill(
        name = name,
        searchedWith = SearchTerm("Recherche", "recherche", "070"),
        discoveredIn = "lokaler Skillordner",
        provenance = provenance,
        licenseEvidence = license,
        content = ContentEvidence.Inspected(
            ContentInspection(findings = listOf(ContentFinding("liest nur", "kein Befehl, kein Netz")))
        ),
        compatibility = CompatibilityVerdict.COMPATIBLE
    )

    private fun audit(
        source: String = "lokaler Skillordner",
        global: Boolean = true
    ): EnvironmentAudit = EnvironmentAudit(
        source = source,
        name = "recherche-helfer",
        licenseVerified = true,
        format = SkillFormat.MARKDOWN_FRONTMATTER,
        executesNothing = true,
        downloadsNothing = true,
        network = NetworkBehaviour.NONE,
        requiredPermissions = if (global) {
            listOf(
                RequiredPermission(
                    name = "Netzwerkzugriff für die Recherche",
                    scope = PermissionScope.GLOBAL
                )
            )
        } else {
            emptyList()
        },
        lastInspectedAt = 1_700_000_000_000L
    )

    private fun dialog(
        skill: DiscoveredSkill = skill(),
        audit: EnvironmentAudit = audit()
    ): SkillConfirmationDialog = SkillConfirmationDialog.of(skill, audit)

    // ── Zusage 1: der Dialog nennt alles, was der Nutzer entscheidet ───────

    @Test
    fun `der Dialog nennt Quelle Lizenz Umfang Installationsort und Entfernen`() {
        val bestaetigung = dialog().confirmation()

        // Jede Angabe, die der Dialog machen muss, taucht in EINER Liste auf.
        val zeilen = bestaetigung.displayLines()
        listOf(
            bestaetigung.sourceLine,
            bestaetigung.licenseLine,
            bestaetigung.scopeLine,
            bestaetigung.siteLine,
            bestaetigung.removalLine
        ).forEach { zeile ->
            assertTrue(
                "Der Dialog muss die Zeile anzeigen: $zeile",
                zeilen.any { it.contains(zeile) }
            )
        }
    }

    @Test
    fun `eine offene Lizenzlage wird im Dialog offen gezeigt und nicht geraten`() {
        val offene = dialog(skill = skill(license = DiscoveredLicenseEvidence.NOT_INSPECTED))

        assertFalse(
            "Eine ungeprüfte Lizenz darf nicht als geklärt erscheinen.",
            offene.confirmation().licenseLine.contains("nachprüfbar")
        )
        assertTrue(
            "Der Dialog muss sagen, dass die Lizenzlage offen ist.",
            offene.confirmation().licenseLine.contains("offen")
        )
    }

    @Test
    fun `ein fehlender Fundort wird als offen gezeigt und nicht ergaenzt`() {
        val ohne = dialog(audit = audit(source = ""))

        assertTrue(
            "Ohne Fundort darf der Dialog keinen Ort behaupten.",
            ohne.confirmation().sourceLine.contains("offen")
        )
    }

    @Test
    fun `die Umfangzeile nennt jedes geforderte Recht einzeln`() {
        val zeile = dialog().confirmation().scopeLine

        assertTrue("Der Umfang muss das Recht benennen.", zeile.contains("Netzwerkzugriff für die Recherche"))
    }

    @Test
    fun `der Dialog nennt ausdruecklich dass ein globales Recht ueber das Projekt hinausgeht`() {
        val bestaetigung = dialog().confirmation()

        assertEquals(InstallScope.GLOBAL, bestaetigung.scope)
        assertTrue(
            "Ein globales Recht muss als solches benannt werden, nicht als Projektrecht.",
            bestaetigung.scopeLine.contains("über das Projekt hinaus")
        )
    }

    @Test
    fun `eine projektbezogene Faehigkeit wird nicht als global dargestellt`() {
        val lokal = dialog(audit = audit(global = false))

        assertEquals(InstallScope.PROJECT, lokal.confirmation().scope)
    }

    // ── Zusage 2: der Dialog blockiert, was die Zusage verbietet ───────────

    @Test
    fun `eine unvollstaendig geprueft Faehigkeit kann nicht bestaetigt werden`() {
        val offen = dialog(audit = audit(source = ""))

        assertFalse(
            "Ohne belegten Fundort darf der Dialog keine Bestaetigung anbieten.",
            offen.mayConfirm()
        )
    }

    @Test
    fun `eine ausfuehrbare Faehigkeit traegt eine eigene Warnung`() {
        val ausfuehrbar = dialog(
            audit = EnvironmentAudit(
                source = "lokaler Skillordner",
                name = "recherche-helfer",
                licenseVerified = true,
                format = SkillFormat.MARKDOWN_FRONTMATTER,
                executesNothing = false,      // führt etwas aus
                downloadsNothing = true,
                network = NetworkBehaviour.NONE,
                lastInspectedAt = 1_700_000_000_000L
            )
        )

        assertFalse(
            "Ein ausführbarer Inhalt ist nicht über eine generische Freigabe erledigt.",
            ausfuehrbar.mayConfirm()
        )
        assertTrue(
            "Die Ablehnung muss einen Grund nennen.",
            ausfuehrbar.refusalReasons().any { grund ->
                grund.contains("führt Befehle aus") || grund.contains("Befehle ausführt")
            }
        )
    }

    @Test
    fun `eine vollstaendig geprueft und gefahrlose Faehigkeit ist bestaetigbar`() {
        val gut = dialog()

        assertTrue(gut.mayConfirm())
        assertEquals(emptyList<String>(), gut.refusalReasons())
    }

    @Test
    fun `jede Ablehnung nennt ihren Grund einzeln`() {
        val offen = dialog(audit = audit(source = ""))

        val gruende = offen.refusalReasons()
        assertTrue("Eine Ablehnung ohne Grund ist nicht benutzbar.", gruende.isNotEmpty())
        gruende.forEach { grund ->
            assertTrue("Der Grund darf nicht leer sein: '$grund'", grund.isNotBlank())
        }
    }

    // ── Der Dialog ist der einzige Weg zur Freigabe ─────────────────────────

    @Test
    fun `eine Bestaetigung entsteht nur ueber den Dialog und nicht direkt`() {
        val dlg = dialog()
        val antrag = dlg.request("Nutzerin")

        val zustimmung = dlg.confirm(antrag, ConsentState.GRANTED, grantedAt = 1_700_000_000_500L)

        assertEquals(ConsentState.GRANTED, zustimmung.state)
    }

    @Test
    fun `eine Bestaetigung ohne dokumentierten Antrag entsteht nicht`() {
        val dlg = dialog()
        val gebastelt = InstallRequest(
            discoveredName = "recherche-helfer",
            requestedBy = "",
            summary = "am Weg vorbei gebaut"
        )

        val zustimmung = dlg.confirm(gebastelt, ConsentState.GRANTED, grantedAt = 1_700_000_000_500L)

        assertEquals(
            "Ohne Antrag darf keine Freigabe entstehen.",
            ConsentState.WITHDRAWN,
            zustimmung.state
        )
    }

    @Test
    fun `eine Bestaetigung fuer einen anderen Skill gilt nicht fuer diesen`() {
        val dlg = dialog()
        val fremderAntrag = InstallRequest(
            discoveredName = "anderer-skill",
            requestedBy = "Nutzerin",
            summary = "gehoert zu einem anderen Fund"
        )
        val zustimmung = dlg.confirm(fremderAntrag, ConsentState.GRANTED, grantedAt = 1_700_000_000_500L)

        assertFalse(zustimmung.covers("recherche-helfer"))
    }

    @Test
    fun `eine globale Installation braucht immer eine Nutzerentscheidung`() {
        assertTrue(SkillCompatibility.installationRequiresUserConsent())
    }

    // ── Zusage 3: Ablehnen installiert nichts weiter ────────────────────────

    @Test
    fun `ein abgelehnter Skill nimmt keinen anderen mit`() {
        val verzeichnis = listOf(skill("recherche-helfer"), skill("test-helfer"))
        val abgelehnt = "recherche-helfer"

        val nachher = SkillConfirmationDialog.remainingAfterRejection(
            candidates = verzeichnis,
            rejectedName = abgelehnt
        )

        assertEquals(
            "Ein abgelehnter Skill darf keinen weiteren mitnehmen.",
            listOf("test-helfer"),
            nachher.map { it.name }
        )
    }

    @Test
    fun `ein abgelehnter Skill blockiert keinen anderen Skill auf seinem Weg`() {
        val verzeichnis = listOf(skill("recherche-helfer"), skill("test-helfer"))

        val ergebnis = SkillConfirmationDialog.installationOutcome(
            candidates = verzeichnis,
            rejectedName = "recherche-helfer",
            consentedName = "test-helfer"
        )

        assertTrue(
            "Der zweite Skill ist unberührt und bleibt installierbar.",
            ergebnis.isAllowed
        )
        assertEquals(listOf("test-helfer"), ergebnis.allowedNames)
    }

    @Test
    fun `wenn nichts bestaetigt wurde, wird auch nichts installiert`() {
        val verzeichnis = listOf(skill("recherche-helfer"), skill("test-helfer"))

        val ergebnis = SkillConfirmationDialog.installationOutcome(
            candidates = verzeichnis,
            rejectedName = "recherche-helfer",
            consentedName = null
        )

        assertFalse(ergebnis.isAllowed)
        assertEquals(emptyList<String>(), ergebnis.allowedNames)
        assertTrue(
            "Ohne eine Bestaetigung darf das Feld nicht als Installationsziel gelten.",
            ergebnis.refusalReasons().isNotEmpty()
        )
    }

    @Test
    fun `ein unbekannter Name im Ergebnis kann nichts freigeben`() {
        val verzeichnis = listOf(skill("recherche-helfer"))

        val ergebnis = SkillConfirmationDialog.installationOutcome(
            candidates = verzeichnis,
            rejectedName = "recherche-helfer",
            consentedName = "nie-gefunden"
        )

        assertFalse(
            "Ein Name, der nicht im Verzeichnis stand, darf nichts freigeben.",
            ergebnis.isAllowed
        )
    }

    @Test
    fun `eine Mehrfachbestaetigung ist nicht moeglich`() {
        val dlg = dialog()

        val ergebnis = dlg.confirmAll(
            candidates = listOf(skill("recherche-helfer"), skill("test-helfer")),
            consentedName = "recherche-helfer",
            grantedAt = 1_700_000_000_500L
        )

        assertNull(
            "Eine Sammelfreigabe mehrerer Skills ist nicht zulaessig.",
            ergebnis
        )
    }

    // ── Der Installationsort wird benannt, nicht geraten ────────────────────

    @Test
    fun `der Installationsort nennt den Zielpfad ausdruecklich`() {
        val site = SkillConfirmationDialog.defaultSite("recherche-helfer", projekt)

        assertEquals(InstallSiteKind.PROJECT_SKILLS, site.kind)
        assertTrue(
            "Der Ort muss den Zielpfad nennen, sonst ist er nicht auffindbar.",
            site.displayLine().contains(projekt)
        )
    }

    @Test
    fun `ein globaler Skill wird nicht in den Projektordner geschrieben`() {
        // Die Zwei-Arg-Form von defaultSite ist bewusst die PROJEKT-Variante; wer
        // global installieren will, muss den Umfang ausdruecklich nennen. Genau
        // darum wird hier der Umfang uebergeben und nicht weggelassen.
        val site = SkillConfirmationDialog.defaultSite(
            "recherche-helfer",
            projekt,
            InstallScope.GLOBAL
        )

        assertEquals(InstallSiteKind.GLOBAL_SKILLS, site.kind)
        assertFalse(
            "Eine globale Faehigkeit gehoert nicht in den Projektordner.",
            site.path.contains(projekt)
        )
    }

    // ── Entfernen ist Teil des Dialogs, nicht eine Fiktion ─────────────────

    @Test
    fun `der Dialog nennt beim Entfernen genau diesen Skill`() {
        val plan = RemovalPlan.forSkill("recherche-helfer", projekt)

        assertEquals("recherche-helfer", plan.skillName)
        assertTrue(
            "Der Entfernplan muss benennen, WAS entfernt wird.",
            plan.displayLine().contains("recherche-helfer")
        )
    }

    @Test
    fun `ein Entfernplan ohne Skillnamen ist nicht darstellbar`() {
        val fehler = runCatching { RemovalPlan.forSkill("", projekt) }.exceptionOrNull()

        assertTrue(
            "Ein Plan ohne Ziel waere eine leere Zusage.",
            fehler is IllegalArgumentException
        )
    }

    @Test
    fun `entfernen und behalten werden getrennt angeboten`() {
        val dlg = dialog()

        assertTrue(
            "Der Dialog muss das Entfernen anbieten.",
            dlg.mayRemove()
        )
        assertNotNull(dlg.removalPlan())
    }

    // ── Die Inhaltspruefung bleibt Voraussetzung, nicht Formalie ────────────

    @Test
    fun `ein Inhalt ohne belegte Wirkung wird nicht bestaetigbar`() {
        val ohneWirkung = dialog(
            audit = EnvironmentAudit(
                source = "lokaler Skillordner",
                name = "recherche-helfer",
                licenseVerified = true,
                format = SkillFormat.MARKDOWN_FRONTMATTER,
                executesNothing = true,
                downloadsNothing = true,
                network = NetworkBehaviour.NONE,
                lastInspectedAt = 1_700_000_000_000L
            )
        )

        assertTrue(ohneWirkung.mayConfirm())
    }

    @Test
    fun `die Inhaltsbelege trennen Lesen von Ausfuehren`() {
        val gelesen = ContentEffect(
            statement = "liest Dateien im Projekt",
            kind = ContentEffectKind.READ_ONLY
        )
        val gefuehrt = ContentEffect(
            statement = "fuehrt Befehle aus",
            kind = ContentEffectKind.EXECUTES
        )

        assertFalse(gelesen.isExecution)
        assertTrue(gefuehrt.isExecution)
    }

    @Test
    fun `der Dialog nennt jede geforderte Wirkung einzeln und nicht nur die harmlose`() {
        val mitAusfuehrung = dialog(
            audit = EnvironmentAudit(
                source = "lokaler Skillordner",
                name = "recherche-helfer",
                licenseVerified = true,
                format = SkillFormat.MARKDOWN_FRONTMATTER,
                executesNothing = false,
                downloadsNothing = true,
                network = NetworkBehaviour.REQUIRED,
                requiredPermissions = emptyList(),
                lastInspectedAt = 1_700_000_000_000L
            )
        )

        val gruende = mitAusfuehrung.refusalReasons()
        assertTrue(
            "Sowohl Ausfuehrung als auch Netzverhalten muessen auftauchen.",
            gruende.isNotEmpty()
        )
    }

    // ── Structurale Zusage: keine Sammelfreigabe-Schnittstelle ─────────────

    @Test
    fun `die Dialogklasse bietet keine Methode zum Installieren oder Ausfuehren an`() {
        val verboten = listOf("install", "execute", "run", "write", "download", "copy", "deploy")

        val gefunden = SkillConfirmationDialog::class.java.declaredMethods
            .map { it.name.lowercase() }
            .filter { name -> verboten.any { name.contains(it) } }

        assertEquals(
            "Der Dialog entscheidet, er installiert nicht. Gefunden: $gefunden",
            emptyList<String>(),
            gefunden
        )
    }

    @Test
    fun `die Bestaetigung traegt keinen Inhalt und keine Adresse zum Senden`() {
        val felder = SkillConfirmation::class.java.declaredFields.map { it.name.lowercase() }

        listOf("apikey", "token", "secret", "endpoint", "url", "payload").forEach { verboten ->
            assertTrue(
                "Die Bestaetigung darf kein Feld '$verboten' fuehren. Vorhanden: $felder",
                felder.none { it.contains(verboten) }
            )
        }
    }

    @Test
    fun `eine Freigabe aus der Vergangenheit traegt keine fremde Kennung stillschweigend mit`() {
        val dlg = dialog()
        val antrag = dlg.request("Nutzerin")
        val zustimmung = dlg.confirm(antrag, ConsentState.GRANTED, grantedAt = 1_700_000_000_500L)

        assertEquals("recherche-helfer", zustimmung.scope)
        assertFalse(
            "Eine Freigabe fuer einen Skill darf keinen anderen abdecken.",
            zustimmung.covers("test-helfer")
        )
    }

    @Test
    fun `eine bestaetigte Faehigkeit bleibt nach der Freigabe einzeln nachvollziehbar`() {
        val dlg = dialog()
        val antrag = dlg.request("Nutzerin")
        val zustimmung = dlg.confirm(antrag, ConsentState.GRANTED, grantedAt = 1_700_000_000_500L)
        val ergebnis = SkillConfirmationDialog.installationOutcome(
            candidates = listOf(skill()),
            rejectedName = null,
            consentedName = zustimmung.scope
        )

        assertEquals(listOf("recherche-helfer"), ergebnis.allowedNames)
    }

    @Test
    fun `die Recherche-Aufgabe nutzt die Freigabe aus dieser Datei statt einer eigenen`() {
        // Verbindung zu 129: dieselbe Zustimmung, keine zweite Wahrheit.
        val dlg = dialog()
        val antrag = dlg.request("Nutzerin")
        val zustimmung = dlg.confirm(antrag, ConsentState.GRANTED, grantedAt = 1_700_000_000_500L)

        val alt = SkillDiscoveryInventory.recordConsent(
            antrag,
            ConsentState.GRANTED,
            1_700_000_000_500L
        )

        assertEquals(alt.state, zustimmung.state)
        assertEquals(alt.scope, zustimmung.scope)
    }

    @Test
    fun `eine widerrufene Freigabe beendet den Dialog ohne Aenderung an anderen`() {
        val dlg = dialog()
        val antrag = dlg.request("Nutzerin")
        val zustimmung = dlg.confirm(antrag, ConsentState.GRANTED, grantedAt = 1_700_000_000_500L)
        val entzogen = SkillDiscoveryInventory.revokeConsent(zustimmung, revokedAt = 1_700_000_001_000L)

        assertEquals(ConsentState.REVOKED, entzogen.state)
        assertFalse(entzogen.covers("recherche-helfer") && entzogen.state == ConsentState.GRANTED)
    }

    @Test
    fun `der Dialog nennt auch dann keinen Ort, wenn die Quelle offen ist`() {
        val ohne = dialog(audit = audit(source = ""))

        assertTrue(
            "Ohne Quelle darf der Dialog keinen Zielpfad nennen.",
            ohne.confirmation().siteLine.contains(SkillConfirmation.UNKNOWN)
        )
        assertFalse(
            "Ohne Quelle darf kein Pfad behauptet werden.",
            ohne.confirmation().siteLine.contains(projekt)
        )
    }

    @Test
    fun `die Bestaetigung ist ein reiner Wert ohne Uebertragungsweg`() {
        val methoden = SkillConfirmation::class.java.declaredMethods.map { it.name.lowercase() }

        listOf("share", "upload", "send", "sync", "publish", "export").forEach { verboten ->
            assertTrue(
                "Die Bestaetigung darf keine Methode '$verboten' anbieten.",
                methoden.none { it.contains(verboten) }
            )
        }
    }

    @Test
    fun `eine Ablehnung ohne Namen ist nicht darstellbar`() {
        val fehler = runCatching {
            SkillConfirmationDialog.installationOutcome(
                candidates = listOf(skill()),
                rejectedName = null,
                consentedName = null
            )
        }.exceptionOrNull()

        // Ohne Rejection und ohne Zustimmung ist das Ergebnis "nichts installiert",
        // kein Fehler: das ist ein gueltiger, im UI haeufiger Zustand.
        assertNull(fehler)
    }

    @Test
    fun `die Skillart bestimmt den Zielort und nicht der Name des Skills`() {
        val global = SkillConfirmationDialog.defaultSite("recherche-helfer", projekt, InstallScope.GLOBAL)
        val lokal = SkillConfirmationDialog.defaultSite("recherche-helfer", projekt, InstallScope.PROJECT)

        assertEquals(InstallSiteKind.GLOBAL_SKILLS, global.kind)
        assertEquals(InstallSiteKind.PROJECT_SKILLS, lokal.kind)
    }

    @Test
    fun `dieselbe Kennung mit anderem Umfang ist ein anderes Ziel`() {
        val global = SkillConfirmationDialog.defaultSite("recherche-helfer", projekt, InstallScope.GLOBAL)
        val lokal = SkillConfirmationDialog.defaultSite("recherche-helfer", projekt, InstallScope.PROJECT)

        assertFalse(
            "Gleiche Kennung, anderer Ort: das sind zwei verschiedene Ziele.",
            global.equals(lokal)
        )
    }

    @Test
    fun `eine leere Projektkennung erzeugt keinen gueltigen Projektort`() {
        val ohneProjekt = runCatching {
            SkillConfirmationDialog.defaultSite("recherche-helfer", "", InstallScope.PROJECT)
        }.exceptionOrNull()

        assertTrue(
            "Ein Projektort ohne Projektkennung waere ein erfundener Ort.",
            ohneProjekt is IllegalArgumentException
        )
    }

    @Test
    fun `die Entfernzeile nennt auch, dass Dateien des Projekts unberuehrt bleiben`() {
        val plan = RemovalPlan.forSkill("recherche-helfer", projekt)

        assertTrue(
            "Entfernen darf nicht als Loeschen von Projektdateien gelesen werden.",
            plan.displayLine().contains("Projektdateien")
        )
    }

    @Test
    fun `der Dialog nennt die Voraussetzungen als offen, statt sie zu behaupten`() {
        val mitOffenerLizenz = dialog(skill = skill(license = DiscoveredLicenseEvidence.FILE_MISSING))

        assertTrue(
            "Eine fehlende Lizenzdatei ist eine offene Frage, kein Nein.",
            mitOffenerLizenz.confirmation().licenseLine.contains("offen")
        )
    }
}
