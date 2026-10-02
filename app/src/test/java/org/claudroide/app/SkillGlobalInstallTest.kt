package org.claudroide.app

import org.claudroide.app.feature.skills.CompatibilityVerdict
import org.claudroide.app.feature.skills.ContentEvidence
import org.claudroide.app.feature.skills.ContentFinding
import org.claudroide.app.feature.skills.ContentInspection
import org.claudroide.app.feature.skills.ConsentState
import org.claudroide.app.feature.skills.DiscoveredLicenseEvidence
import org.claudroide.app.feature.skills.DiscoveredSkill
import org.claudroide.app.feature.skills.EnvironmentAudit
import org.claudroide.app.feature.skills.EnvironmentFile
import org.claudroide.app.feature.skills.FileConflict
import org.claudroide.app.feature.skills.GlobalInstallPlan
import org.claudroide.app.feature.skills.InstallScope
import org.claudroide.app.feature.skills.InstallSite
import org.claudroide.app.feature.skills.InstallSiteKind
import org.claudroide.app.feature.skills.InstallWriteResult
import org.claudroide.app.feature.skills.InstallationPreview
import org.claudroide.app.feature.skills.NetworkBehaviour
import org.claudroide.app.feature.skills.PermissionScope
import org.claudroide.app.feature.skills.ProvenanceEvidence
import org.claudroide.app.feature.skills.RequiredPermission
import org.claudroide.app.feature.skills.SearchTerm
import org.claudroide.app.feature.skills.SkillFormat
import org.claudroide.app.feature.skills.SkillGlobalInstall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aufgabe 131 — „Globalen Skill installieren".
 *
 * Diese Aufgabe unterscheidet sich von 124 in einem einzigen, entscheidenden
 * Punkt: **124 zeigte den Dialog, 131 schreibt die Dateien.** Daraus folgt die
 * tragende Grenze dieser Datei — der Arbeitsplan entscheidet und berechnet, und
 * **die Schreiboperation wird als Ergebnis übergeben, nie selbst ausgeführt**.
 *
 * Die zwei Zusagen, die hier je einen Test bekommen:
 *
 *  1. Der Nutzer sieht **die genaue Quelle und den globalen Geltungsbereich**,
 *     bevor irgendetwas geschrieben wird.
 *  2. **Keine vorhandene Fähigkeit wird ungefragt überschrieben.** Ein Konflikt
 *     ist ein *Ergebnis*, kein Nebenhinweis — und ohne ausdrückliche Zustimmung
 *     bleibt die bestehende Datei unangetastet.
 *
 * Aufbau: erst der Plan (was würde geschrieben), dann der Konflikt (was steht
 * dort schon), dann das Ergebnis (was ist wirklich passiert), zuletzt die
 * strukturellen Zusicherungen.
 */
class SkillGlobalInstallTest {

    // ── Hilfen ─────────────────────────────────────────────────────────────

    private val projekt = "projekt-alpha"

    private fun skill(
        name: String = "recherche-helfer",
        license: DiscoveredLicenseEvidence = DiscoveredLicenseEvidence.DOCUMENT_PRESENT
    ): DiscoveredSkill = DiscoveredSkill(
        name = name,
        searchedWith = SearchTerm("Recherche", "recherche", projekt),
        discoveredIn = "lokaler Skillordner",
        provenance = ProvenanceEvidence.RECORDED,
        licenseEvidence = license,
        content = ContentEvidence.Inspected(
            ContentInspection(findings = listOf(ContentFinding("liest nur", "kein Befehl, kein Netz")))
        ),
        compatibility = CompatibilityVerdict.COMPATIBLE
    )

    private fun audit(global: Boolean = true, source: String = "lokaler Skillordner"): EnvironmentAudit =
        EnvironmentAudit(
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

    private fun files(vararg pfade: String): List<EnvironmentFile> =
        pfade.map { EnvironmentFile(relativePath = it, sizeBytes = 128L) }

    /**
     * Der Plan mit einer **gültigen** Zustimmung für [this] Skill.
     *
     * Fast jeder Schreibpfad braucht das: Die Aufgabe verlangt eine ausdrückliche
     * Zustimmung, und sie bleibt Bedingung — auch dann, wenn kein Konflikt im Weg
     * steht. Der Kürzel macht an jeder Stelle sichtbar, dass getestet wird
     * „Zustimmung **und** kein Konflikt", nicht „kein Konflikt".
     */
    private fun GlobalInstallPlan.zugestimmt(
        name: String = "recherche-helfer",
        grantedAt: Long = 1_700_000_000_500L
    ): GlobalInstallPlan = withConsent(
        consentForName = name,
        state = ConsentState.GRANTED,
        grantedAt = grantedAt
    )

    // ── Zusage 1: Quelle und globaler Geltungsbereich sind sichtbar ────────

    @Test
    fun `der Installationsplan nennt die genaue Quelle`() {
        val plan = SkillGlobalInstall.plan(skill(), audit(), files("SKILL.md"), emptyList())

        val zeilen = plan.previewLines()
        assertTrue(
            "Der Plan muss die Quelle nennen, sonst ist nichts nachprüfbar.",
            zeilen.any { it.contains("lokaler Skillordner") }
        )
    }

    @Test
    fun `der Installationsplan nennt den globalen Geltungsbereich woertlich`() {
        val plan = SkillGlobalInstall.plan(skill(), audit(global = true), files("SKILL.md"), emptyList())

        assertEquals(InstallScope.GLOBAL, plan.scope)
        assertTrue(
            "Der globale Geltungsbereich muss ausgeschrieben stehen.",
            plan.previewLines().any { it.contains("über das Projekt hinaus") }
        )
    }

    @Test
    fun `eine projektbezogene Faehigkeit wird nicht als global geplant`() {
        val plan = SkillGlobalInstall.plan(skill(), audit(global = false), files("SKILL.md"), emptyList())

        assertEquals(InstallScope.PROJECT, plan.scope)
    }

    @Test
    fun `der Plan nennt jeden zu schreibenden Pfad einzeln`() {
        val plan = SkillGlobalInstall.plan(
            skill(),
            audit(),
            files("SKILL.md", "referenz/notizen.md", "beispiele/ablauf.md"),
            emptyList()
        )

        val zeilen = plan.previewLines()
        listOf("SKILL.md", "referenz/notizen.md", "beispiele/ablauf.md").forEach { pfad ->
            assertTrue(
                "Der Plan muss '$pfad' nennen.",
                zeilen.any { it.contains(pfad) }
            )
        }
    }

    @Test
    fun `der Plan nennt den Zielort und nicht nur einen Ordner`() {
        val plan = SkillGlobalInstall.plan(skill(), audit(), files("SKILL.md"), emptyList())

        assertEquals(InstallSiteKind.GLOBAL_SKILLS, plan.site.kind)
        assertTrue(
            "Der Zielpfad muss vollständig stehen.",
            plan.site.path.contains("recherche-helfer")
        )
    }

    @Test
    fun `ein leerer Pfad kann nicht geplant werden`() {
        val fehler = runCatching {
            SkillGlobalInstall.plan(skill(), audit(), files(""), emptyList())
        }.exceptionOrNull()

        assertTrue(
            "Ein Plan mit leerem Pfad waere eine leere Zusage.",
            fehler is IllegalArgumentException
        )
    }

    @Test
    fun `ein Pfad darf nicht aus dem Zielordner ausbrechen`() {
        val fehler = runCatching {
            SkillGlobalInstall.plan(skill(), audit(), files("../../etc/passwd"), emptyList())
        }.exceptionOrNull()

        assertTrue(
            "Ein ausbrechender Pfad waere genau der Angriff, den die Aufgabe verhindert.",
            fehler is IllegalArgumentException
        )
    }

    // ── Zusage 2: keine vorhandene Faehigkeit wird ungefragt ueberschrieben ─

    @Test
    fun `ein Konflikt wird erkannt und benannt`() {
        val plan = SkillGlobalInstall.plan(
            skill(),
            audit(),
            files("SKILL.md"),
            existing = listOf("SKILL.md")
        )

        assertTrue(
            "Eine bereits vorhandene Datei muss als Konflikt auffallen.",
            plan.conflicts.isNotEmpty()
        )
        assertEquals(
            FileConflict.SAME_NAME_DIFFERENT_CONTENT,
            plan.conflicts.first().kind
        )
    }

    @Test
    fun `ohne Konflikt ist der Plan unveraendert und schreibbar`() {
        val plan = SkillGlobalInstall.plan(skill(), audit(), files("SKILL.md"), emptyList())

        assertTrue(plan.conflicts.isEmpty())
        // "Schreibbar" heisst hier: die Zustimmung ist da UND es gibt keinen
        // Konflikt. Ohne Konflikt allein waere der Plan nicht schreibbar — die
        // Zustimmung bleibt Bedingung, auch wenn nichts im Weg steht.
        val mitZustimmung = plan.zugestimmt()
        assertTrue(
            "Ohne Konflikt darf der Plan nicht grundlos blockieren.",
            mitZustimmung.mayWrite()
        )
    }

    @Test
    fun `ein Konflikt blockiert das Schreiben ohne ausdrueckliche Zustimmung`() {
        val plan = SkillGlobalInstall.plan(
            skill(),
            audit(),
            files("SKILL.md"),
            existing = listOf("SKILL.md")
        )

        assertFalse(
            "Ohne ausdrueckliche Zusage darf nichts ueberschrieben werden.",
            plan.mayWrite()
        )
    }

    @Test
    fun `ein Konflikt wird nur mit ausdruecklicher Zusage schreibbar`() {
        val plan = SkillGlobalInstall.plan(
            skill(),
            audit(),
            files("SKILL.md"),
            existing = listOf("SKILL.md"),
            overwriteConfirmed = true
        )

        assertTrue(
            "Eine ausdrueckliche Zusage darf den Konflikt aufloesen.",
            plan.zugestimmt().mayWrite()
        )
    }

    @Test
    fun `eine bestaetigte Ueberschreibung hinterlaesst keinen Ablehnungsgrund`() {
        // Der Nutzer darf ein Ueberschireiben bestaetigen — sonst waere der ganze
        // Sicherungszweig tot. Genau das war der Fehler: der Konfliktgrund stand
        // auch nach der Bestaetigung noch in der Ablehnungsliste, sodass
        // mayWrite() dauerhaft false blieb.
        val bestaetigt = SkillGlobalInstall.plan(
            skill(), audit(), files("SKILL.md"),
            existing = listOf("SKILL.md"), overwriteConfirmed = true
        ).zugestimmt()

        assertTrue(
            "Eine ausdruecklich bestaetigte Ueberschreibung darf nichts blockieren.",
            bestaetigt.mayWrite()
        )
        assertTrue(
            "Nach der Bestaetigung bleibt kein Grund fuer eine Ablehnung.",
            bestaetigt.refusalReasons().isEmpty()
        )
    }

    @Test
    fun `ohne Bestaetigung nennt jeder Konflikt weiterhin seinen Grund`() {
        // Das Gegenstueck: die Bestaetigung darf den Weg nicht zu weit oeffnen.
        // Ohne sie bleibt jeder Konflikt einzeln benannt.
        val plan = SkillGlobalInstall.plan(
            skill(), audit(), files("SKILL.md", "referenz.md"),
            existing = listOf("SKILL.md", "referenz.md")
        )

        val gruende = plan.refusalReasons()
        assertTrue(gruende.any { it.contains("SKILL.md") })
        assertTrue(gruende.any { it.contains("referenz.md") })
        assertTrue(gruende.any { it.contains("Kein Überschreiben") })
    }

    @Test
    fun `jeder Konflikt nennt seinen Pfad und seinen Grund`() {
        val plan = SkillGlobalInstall.plan(
            skill(),
            audit(),
            files("SKILL.md", "referenz.md"),
            existing = listOf("SKILL.md", "referenz.md")
        )

        assertEquals(2, plan.conflicts.size)
        plan.conflicts.forEach { konflikt ->
            assertTrue(konflikt.relativePath.isNotBlank())
            assertTrue(konflikt.displayLine().contains(konflikt.relativePath))
        }
    }

    @Test
    fun `eine leere Dateiliste ist kein Plan und blockiert`() {
        val plan = SkillGlobalInstall.plan(skill(), audit(), emptyList(), emptyList())

        assertFalse(
            "Ein Plan ohne Dateien wuerde nichts tun und nichts sagen.",
            plan.mayWrite()
        )
        assertTrue(plan.refusalReasons().isNotEmpty())
    }

    // ── Die Sicherung vor dem Ueberschreiben ───────────────────────────────

    @Test
    fun `vor dem Ueberschreiben wird eine Sicherung angelegt`() {
        val plan = SkillGlobalInstall.plan(
            skill(),
            audit(),
            files("SKILL.md"),
            existing = listOf("SKILL.md"),
            overwriteConfirmed = true
        )

        assertTrue(
            "Ohne Sicherung waere ein Ueberschreiben endgueltig.",
            plan.requiresBackup()
        )
        assertTrue(
            "Der Plan muss die Sicherung auch beziffern.",
            plan.backupFileCount > 0
        )
    }

    @Test
    fun `ohne Konflikt wird keine Sicherung angelegt`() {
        val plan = SkillGlobalInstall.plan(skill(), audit(), files("SKILL.md"), emptyList())

        assertFalse(
            "Nichts zu ueberschreiben heisst: nichts zu sichern.",
            plan.requiresBackup()
        )
        assertEquals(0, plan.backupFileCount)
    }

    @Test
    fun `die Sicherung bewahrt genau die Dateien, die ueberschrieben werden`() {
        val plan = SkillGlobalInstall.plan(
            skill(),
            audit(),
            files("SKILL.md", "neu.md"),
            existing = listOf("SKILL.md"),
            overwriteConfirmed = true
        )

        assertEquals(
            "Gesichert wird nur, was auch wirklich ersetzt wird.",
            listOf("SKILL.md"),
            plan.backupPaths
        )
    }

    // ── Das Ergebnis der Pruefung nach dem Schreiben ───────────────────────

    @Test
    fun `ein vollstaendig geschriebener Plan meldet Erfolg mit Nachweis`() {
        val ergebnis = SkillGlobalInstall.result(
            plan = SkillGlobalInstall
                .plan(skill(), audit(), files("SKILL.md", "ref.md"), emptyList())
                .zugestimmt(),
            written = listOf("SKILL.md", "ref.md")
        )

        assertTrue(ergebnis.isComplete)
        assertEquals(emptyList<String>(), ergebnis.missingPaths)
        assertEquals(2, ergebnis.writtenPaths.size)
    }

    @Test
    fun `eine fehlende Datei wird gemeldet und nicht als Erfolg verbucht`() {
        val ergebnis = SkillGlobalInstall.result(
            plan = SkillGlobalInstall.plan(skill(), audit(), files("SKILL.md", "ref.md"), emptyList()),
            written = listOf("SKILL.md")
        )

        assertFalse(
            "Eine fehlende Datei ist kein vollstaendiger Erfolg.",
            ergebnis.isComplete
        )
        assertEquals(listOf("ref.md"), ergebnis.missingPaths)
    }

    @Test
    fun `eine zusaetzlich geschriebene Datei wird gemeldet`() {
        val ergebnis = SkillGlobalInstall.result(
            plan = SkillGlobalInstall.plan(skill(), audit(), files("SKILL.md"), emptyList()),
            written = listOf("SKILL.md", "unerwartet.md")
        )

        assertFalse(
            "Eine Datei ausserhalb des Plans darf nicht unauffaellig bleiben.",
            ergebnis.isComplete
        )
        assertTrue(ergebnis.unexpectedPaths.contains("unerwartet.md"))
    }

    @Test
    fun `ein leeres Schreibergebnis ist kein Erfolg`() {
        val ergebnis = SkillGlobalInstall.result(
            plan = SkillGlobalInstall.plan(skill(), audit(), files("SKILL.md"), emptyList()),
            written = emptyList()
        )

        assertFalse(ergebnis.isComplete)
        assertEquals(listOf("SKILL.md"), ergebnis.missingPaths)
    }

    @Test
    fun `das Ergebnis nennt die Sicherung nur, wenn es eine gab`() {
        val mitSicherung = SkillGlobalInstall.result(
            plan = SkillGlobalInstall.plan(
                skill(), audit(), files("SKILL.md"),
                existing = listOf("SKILL.md"), overwriteConfirmed = true
            ).zugestimmt(),
            written = listOf("SKILL.md"),
            backupCreated = true
        )

        assertTrue(
            "Eine beobachtete Sicherung muss die Oberfläche erreichen koennen.",
            mitSicherung.displayLines().any { it.contains("gesichert") || it.contains("Sicherung") }
        )
    }

    @Test
    fun `ein Plan mit beabsichtigter Sicherung meldet keine als ausgefuehrt`() {
        // Der Plan sieht eine Sicherung vor — belegt ist damit gar nichts. Ohne
        // Meldung darf die Anzeige sie nicht als Tatsache behaupten: das Wort
        // "wurde" waere eine Behauptung ueber die Vergangenheit.
        val ohneMeldung = SkillGlobalInstall.result(
            plan = SkillGlobalInstall.plan(
                skill(), audit(), files("SKILL.md"),
                existing = listOf("SKILL.md"), overwriteConfirmed = true
            ).zugestimmt(),
            written = listOf("SKILL.md")
        )

        assertFalse(
            "Der Wunsch nach einer Sicherung ist kein Nachweis, dass sie entstand.",
            ohneMeldung.backupCreated
        )
        assertTrue(
            "Ohne beobachtete Sicherung darf die Anzeige keine behaupten.",
            ohneMeldung.displayLines().none { it.contains("gesichert") }
        )
    }

    @Test
    fun `ein blockierter Plan wird nie als geschrieben gemeldet`() {
        val blockiert = SkillGlobalInstall.plan(
            skill(), audit(), files("SKILL.md"), existing = listOf("SKILL.md")
        )

        val ergebnis = SkillGlobalInstall.result(plan = blockiert, written = listOf("SKILL.md"))

        assertFalse(ergebnis.isComplete)
        assertTrue(
            "Ein blockierter Plan darf kein vollstaendiges Ergebnis melden.",
            ergebnis.refusalReasons().isNotEmpty()
        )
    }

    // ── Entfernen bleibt moeglich und benennt die Sicherung ────────────────

    @Test
    fun `das Entfernen nennt den wiederherstellbaren Stand`() {
        val entfern = SkillGlobalInstall.removal(
            skillName = "recherche-helfer",
            site = InstallSite(InstallSiteKind.GLOBAL_SKILLS, "skills/recherche-helfer"),
            backupExists = true
        )

        val zeile = entfern.displayLines().joinToString(" ")
        assertTrue(
            "Wenn eine Sicherung da ist, muss die Oberfläche sie nennen koennen.",
            zeile.contains("wiederherstellbar") || zeile.contains("Sicherung")
        )
    }

    @Test
    fun `ohne Sicherung wird das Entfernen als endgueltig bezeichnet`() {
        val entfern = SkillGlobalInstall.removal(
            skillName = "recherche-helfer",
            site = InstallSite(InstallSiteKind.GLOBAL_SKILLS, "skills/recherche-helfer"),
            backupExists = false
        )

        val zeile = entfern.displayLines().joinToString(" ")
        assertTrue(
            "Ohne Sicherung ist das Entfernen nicht rueckgaengig zu machen.",
            zeile.contains("endgültig") || zeile.contains("nicht zurück")
        )
    }

    @Test
    fun `das Entfernen nennt genau diesen Skill und nicht den ganzen Ordner`() {
        val entfern = SkillGlobalInstall.removal(
            skillName = "recherche-helfer",
            site = InstallSite(InstallSiteKind.GLOBAL_SKILLS, "skills/recherche-helfer"),
            backupExists = true
        )

        assertTrue(
            "Der Plan muss den Skill nennen.",
            entfern.displayLines().any { it.contains("recherche-helfer") }
        )
    }

    // ── Die strukturellen Zusicherungen ────────────────────────────────────

    @Test
    fun `die Installationsklasse schreibt keine Dateien selbst`() {
        val verboten = listOf("writefile", "writebytes", "copyto", "move", "delete", "mkdir", "createfile")

        val gefunden = SkillGlobalInstall::class.java.declaredMethods
            .map { it.name.lowercase() }
            .filter { name -> verboten.any { name.replace("_", "") == it || name.replace("_", "").contains(it) } }

        assertEquals(
            "Der Arbeitsplan berechnet; das Schreiben passiert woanders. Gefunden: $gefunden",
            emptyList<String>(),
            gefunden
        )
    }

    @Test
    fun `das Ergebnis traegt kein Feld fuer Inhalt oder Schluessel`() {
        val felder = InstallWriteResult::class.java.declaredFields.map { it.name.lowercase() }

        listOf("apikey", "token", "secret", "content", "payload").forEach { verboten ->
            assertTrue(
                "Das Ergebnis darf kein Feld '$verboten' fuehren. Vorhanden: $felder",
                felder.none { it.contains(verboten) }
            )
        }
    }

    @Test
    fun `eine globale Installation braucht immer eine ausdrueckliche Zustimmung`() {
        assertTrue(SkillGlobalInstall.installationRequiresUserConsent())
    }

    @Test
    fun `eine Zustimmung fuer einen anderen Skill loest keinen globalen Install aus`() {
        val plan = SkillGlobalInstall.plan(skill("recherche-helfer"), audit(), files("SKILL.md"), emptyList())

        val ergebnis = plan.withConsent(
            consentForName = "anderer-skill",
            state = ConsentState.GRANTED,
            grantedAt = 1_700_000_000_500L
        )

        assertFalse(
            "Eine Freigabe fuer einen anderen Skill darf diesen nicht freigeben.",
            ergebnis.mayWrite()
        )
    }

    @Test
    fun `eine gueltige Zustimmung macht einen konfliktfreien Plan schreibbar`() {
        val plan = SkillGlobalInstall.plan(skill(), audit(), files("SKILL.md"), emptyList())

        val ergebnis = plan.withConsent(
            consentForName = "recherche-helfer",
            state = ConsentState.GRANTED,
            grantedAt = 1_700_000_000_500L
        )

        assertTrue(ergebnis.mayWrite())
    }

    @Test
    fun `eine entzogene Zustimmung nimmt die Schreibfreigabe wieder weg`() {
        val plan = SkillGlobalInstall.plan(skill(), audit(), files("SKILL.md"), emptyList())
        val erteilt = plan.withConsent(
            consentForName = "recherche-helfer",
            state = ConsentState.GRANTED,
            grantedAt = 1_700_000_000_500L
        )

        val entzogen = erteilt.withConsent(
            consentForName = "recherche-helfer",
            state = ConsentState.REVOKED,
            grantedAt = 1_700_000_000_500L,
            revokedAt = 1_700_000_001_000L
        )

        assertFalse(
            "Eine entzogene Zustimmung darf nicht weiter freigeben.",
            entzogen.mayWrite()
        )
    }

    @Test
    fun `eine Zustimmung ohne Zeitangabe ist keine Zustimmung`() {
        val plan = SkillGlobalInstall.plan(skill(), audit(), files("SKILL.md"), emptyList())

        val ergebnis = plan.withConsent(
            consentForName = "recherche-helfer",
            state = ConsentState.GRANTED,
            grantedAt = 0L
        )

        assertFalse(
            "Eine Zeitangabe 0 ist keine dokumentierte Entscheidung.",
            ergebnis.mayWrite()
        )
    }

    @Test
    fun `eine ungeprueft Faehigkeit wird auch mit Zustimmung nicht geschrieben`() {
        val plan = SkillGlobalInstall.plan(
            skill = skill(),
            audit = audit(source = ""),
            files("SKILL.md"),
            emptyList()
        )

        val ergebnis = plan.withConsent(
            consentForName = "recherche-helfer",
            state = ConsentState.GRANTED,
            grantedAt = 1_700_000_000_500L
        )

        assertFalse(
            "Zustimmung ersetzt keine Inhaltspruefung.",
            ergebnis.mayWrite()
        )
    }

    @Test
    fun `der Plan nennt jede Ablehnung einzeln`() {
        val plan = SkillGlobalInstall.plan(
            skill(),
            audit(),
            files("SKILL.md"),
            existing = listOf("SKILL.md")
        )

        val gruende = plan.refusalReasons()
        assertTrue(
            "Ein Konflikt ohne Grund waere nicht bearbeitbar.",
            gruende.any { it.contains("SKILL.md") }
        )
    }

    @Test
    fun `eine Vorschau ohne bestaetigte Zustimmung ist dennoch nur eine Vorschau`() {
        val vorschau = SkillGlobalInstall.preview(skill(), audit(), files("SKILL.md"), emptyList())

        assertNotNull(vorschau)
        assertNull(
            "Die Vorschau darf selbst keine Freigabe erzeugen.",
            vorschau.consent
        )
    }

    @Test
    fun `die Vorschau nennt dieselben Zeilen wie der Plan`() {
        val vorschau = InstallationPreview.of(
            skillName = "recherche-helfer",
            source = "lokaler Skillordner",
            scope = InstallScope.GLOBAL,
            site = InstallSite(InstallSiteKind.GLOBAL_SKILLS, "skills/recherche-helfer"),
            paths = files("SKILL.md", "ref.md"),
            conflicts = emptyList()
        )

        val zeilen = vorschau.displayLines().joinToString(" ")
        assertTrue(zeilen.contains("lokaler Skillordner"))
        assertTrue(zeilen.contains("über das Projekt hinaus"))
        assertTrue(zeilen.contains("SKILL.md"))
    }
}
