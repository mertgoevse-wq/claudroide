package org.claudroide.app

import org.claudroide.app.feature.skills.CompatibilityVerdict
import org.claudroide.app.feature.skills.ConsentState
import org.claudroide.app.feature.skills.ContentEvidence
import org.claudroide.app.feature.skills.ContentFinding
import org.claudroide.app.feature.skills.ContentInspection
import org.claudroide.app.feature.skills.DiscoveredLicenseEvidence
import org.claudroide.app.feature.skills.DiscoveredSkill
import org.claudroide.app.feature.skills.DiscoveryOutcome
import org.claudroide.app.feature.skills.DiscoverySourceKind
import org.claudroide.app.feature.skills.InstallDecision
import org.claudroide.app.feature.skills.ProvenanceEvidence
import org.claudroide.app.feature.skills.SearchOutcome
import org.claudroide.app.feature.skills.SearchRun
import org.claudroide.app.feature.skills.SearchTerm
import org.claudroide.app.feature.skills.SkillConsent
import org.claudroide.app.feature.skills.SkillDiscoveryInventory
import org.claudroide.app.feature.skills.gapReason
import org.claudroide.app.feature.skills.withInspection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 129 — „Skills finden".
 *
 * Die drei Zusage der Aufgabe stehen jeweils an erster Stelle:
 * Suchen ist keine Installationsfreigabe, **kein Name gilt als Nachweis**, und
 * Lücken bleiben offen. Die vierte Gruppe prüft die Trennung zu Aufgabe 123.
 */
class SkillDiscoveryInventoryTest {

    // ── Suchbegriff und Lauf ────────────────────────────────────────────

    @Test
    fun `ein Suchlauf braucht mindestens einen vollstaendigen Begriff`() {
        val unvollstaendig = SearchRun(
            terms = listOf(SearchTerm(label = "", term = "git", taskId = "095")),
            source = DiscoverySourceKind.GLOBAL_SKILLS,
            searchedScope = "~/.claude/skills",
            isGlobal = true
        )
        assertFalse("Ein Begriff ohne Bezeichnung ist kein Begriff.", unvollstaendig.isComplete)

        val vollstaendig = SearchRun(
            terms = listOf(SearchTerm(label = "git-credentials", term = "git credential", taskId = "095")),
            source = DiscoverySourceKind.GLOBAL_SKILLS,
            searchedScope = "~/.claude/skills",
            isGlobal = true
        )
        assertTrue(vollstaendig.isComplete)
    }

    @Test
    fun `ein Lauf ohne globalen Umfang gilt nicht als global durchsucht`() {
        val lokal = SearchRun(
            terms = listOf(SearchTerm("usb", "usb storage", "085")),
            source = DiscoverySourceKind.SOURCE_REPOSITORY,
            searchedScope = "irgendein Repo",
            isGlobal = false
        )
        assertFalse(
            "Die Aufgabe verlangt global auffindbare Skills.",
            lokal.isComplete
        )
    }

    // ── Zusage 1: ein Name ist kein Nachweis ────────────────────────────

    @Test
    fun `ein blosser Name macht keinen Skill geprueft`() {
        val behauptet = ContentEvidence.NamedClaim("android-testing")
        assertFalse(
            "Der Name eines Treffers belegt nichts ueber seinen Inhalt.",
            behauptet.isVerified
        )
        assertEquals(
            "Eine Behauptung traegt keine Pruefung mit sich.",
            ContentEvidence.NotInspected,
            ContentEvidence.UNVERIFIED
        )
    }

    @Test
    fun `der Standardzustand eines frischen Treffers ist ungeprueft`() {
        val treffer = DiscoveredSkill(
            name = "irgendwas",
            searchedWith = SearchTerm("l", "t", "129"),
            discoveredIn = "~/.claude/skills/irgendwas"
        )
        assertFalse(
            "Wer das Argument weggelaesst, darf nicht als geprueft durchgehen.",
            treffer.content.isVerified
        )
    }

    @Test
    fun `eine leere Pruefung ist kein gepruefter Inhalt`() {
        // Kein einziger Befund benannt: das ist ein leeres Formular, kein Nachweis.
        val leer = ContentInspection(findings = emptyList())
        assertFalse(leer.isUsableEvidence)
        assertFalse(ContentEvidence.Inspected(leer).isVerified)
        assertEquals(
            ContentEvidence.NotInspected,
            ContentEvidence.UNVERIFIED.withInspection(leer)
        )
    }

    @Test
    fun `ein Befund ohne Bezeichnung zaehlt nicht als Beleg`() {
        val namenlos = ContentInspection(
            findings = listOf(
                ContentFinding(label = "", detail = "gesehen"),
                ContentFinding(label = "liest Dateien", detail = "")
            )
        )
        assertEquals(
            "Leere Etiketten und leere Details tragen nichts.",
            emptyList<ContentFinding>(),
            namenlos.usableFindings
        )
        assertFalse(namenlos.isUsableEvidence)
    }

    @Test
    fun `ein benannter Befund macht den Inhalt belegt`() {
        val geprueft = ContentInspection(
            findings = listOf(
                ContentFinding("rein anweisend", "SKILL.md enthaelt nur Prosa, keine Befehle")
            )
        )
        assertTrue(geprueft.isUsableEvidence)
        assertTrue(ContentEvidence.Inspected(geprueft).isVerified)
    }

    @Test
    fun `ein ausfuehrender Inhalt gilt trotz Befund nicht als geprueft`() {
        val befehlend = ContentInspection(
            findings = listOf(ContentFinding("fuehrt aus", "ruft curl auf")),
            executesCommands = true
        )
        assertTrue(
            "Der Befund existiert — aber der Inhalt ist nicht rein anweisend.",
            befehlend.isUsableEvidence
        )
        assertFalse(
            "Ausfuehrender Inhalt darf nie als sicher gelten.",
            ContentEvidence.Inspected(befehlend).isVerified
        )
        assertEquals("executes commands", ContentEvidence.Inspected(befehlend).gapReason())
    }

    @Test
    fun `ein ladender Inhalt gilt ebenfalls nicht als geprueft`() {
        val ladend = ContentInspection(
            findings = listOf(ContentFinding("laedt nach", "laedt ein Modell herunter")),
            installsOrDownloads = true
        )
        assertFalse(ContentEvidence.Inspected(ladend).isVerified)
        assertEquals("installs or downloads", ContentEvidence.Inspected(ladend).gapReason())
    }

    @Test
    fun `lesen und suchen sind keine Nachladewirkung`() {
        // Ein Recherche-Skill darf lesen und suchen. Nur das Nachladen und
        // Ausfuehren schliessen die Verwendung aus.
        val recherche = ContentInspection(
            findings = listOf(ContentFinding("liest", "liest SKILL.md am Fundort")),
            readsFiles = true,
            reachesNetwork = true
        )
        assertFalse(recherche.hasSideEffects)
        assertTrue(ContentEvidence.Inspected(recherche).isVerified)
    }

    @Test
    fun `eine Behauptung nennt ihren Grund und einen geprueften Inhalt nicht`() {
        assertEquals(
            "only a name was claimed: android-testing",
            ContentEvidence.NamedClaim("android-testing").gapReason()
        )
        assertEquals(
            "Jeder offene Zustand nennt seinen Grund.",
            "content not inspected",
            ContentEvidence.NotInspected.gapReason()
        )
        val ok = ContentEvidence.Inspected(
            ContentInspection(listOf(ContentFinding("a", "b")))
        )
        assertTrue(ok.isVerified)
        assertEquals("", ok.gapReason())
    }

    // ── Zusage 2: Suche ist keine Installationsfreigabe ──────────────────

    @Test
    fun `die Suche allein gibt niemals eine Installation frei`() {
        assertTrue(SkillDiscoveryInventory.installRequiresUserConsent())
    }

    @Test
    fun `eine perfekt belegte Suche ohne Zustimmung wird abgelehnt`() {
        val lauf = vollstaendigerLauf()
        val treffer = perfekterTreffer()
        val ergebnis = DiscoveryOutcome.Matches(lauf, listOf(treffer))

        assertEquals(
            InstallDecision.REJECTED,
            SkillDiscoveryInventory.decideInstall(ergebnis, consent = null)
        )
    }

    @Test
    fun `eine Zustimmung fuer einen anderen Skill gilt nicht mit`() {
        val ergebnis = DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(perfekterTreffer()))
        val fremdeZustimmung = SkillDiscoveryInventory.recordConsent(
            SkillDiscoveryInventory.requestInstall(perfekterTreffer(name = "anderer-skill"), "129"),
            ConsentState.GRANTED,
            recordedAt = 1_000L
        )

        assertTrue("Die Zustimmung gilt einem anderen Skill.", fremdeZustimmung.covers("anderer-skill"))
        assertEquals(
            InstallDecision.REJECTED,
            SkillDiscoveryInventory.decideInstall(ergebnis, fremdeZustimmung)
        )
    }

    @Test
    fun `eine entzogene Zustimmung gibt auch einen perfekten Treffer nicht frei`() {
        val treffer = perfekterTreffer()
        val ergebnis = DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(treffer))
        val erteilt = SkillDiscoveryInventory.recordConsent(
            SkillDiscoveryInventory.requestInstall(treffer, "129"),
            ConsentState.GRANTED,
            recordedAt = 1_000L
        )
        assertEquals(InstallDecision.AUTHORIZED, SkillDiscoveryInventory.decideInstall(ergebnis, erteilt))

        val entzogen = SkillDiscoveryInventory.revokeConsent(erteilt, revokedAt = 2_000L)
        assertEquals(ConsentState.REVOKED, entzogen.state)
        assertEquals(
            InstallDecision.REJECTED,
            SkillDiscoveryInventory.decideInstall(ergebnis, entzogen)
        )
    }

    @Test
    fun `eine Zustimmung ohne Antrag entsteht nicht`() {
        // Der Weg ueber recordConsent verlangt den Antrag. Ein Antrag ohne
        // dokumentierten Anfragenden darf keine Freigabe erzeugen.
        val ohneAntrag = SkillDiscoveryInventory.recordConsent(
            SkillDiscoveryInventory.requestInstall(perfekterTreffer(), requestedBy = ""),
            ConsentState.GRANTED,
            recordedAt = 1_000L
        )
        assertEquals(ConsentState.WITHDRAWN, ohneAntrag.state)
        assertEquals(
            InstallDecision.REJECTED,
            SkillDiscoveryInventory.decideInstall(
                DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(perfekterTreffer())),
                ohneAntrag
            )
        )
    }

    @Test
    fun `eine Freigabe braucht eine echte Zeitangabe`() {
        val ohneZeit = SkillDiscoveryInventory.recordConsent(
            SkillDiscoveryInventory.requestInstall(perfekterTreffer(), "129"),
            ConsentState.GRANTED,
            recordedAt = 0L
        )
        assertEquals(ConsentState.WITHDRAWN, ohneZeit.state)
    }

    // ── Was die Freigabe zusätzlich verlangt ────────────────────────────

    @Test
    fun `eine fehlende Lizenzdatei schliesst die Freigabe aus`() {
        // Genau der Befund aus Aufgabe 123 bei vier installierten Skills:
        // `license: Complete terms in LICENSE.txt` — die Datei fehlt.
        val treffer = perfekterTreffer(
            license = DiscoveredLicenseEvidence.FILE_MISSING
        )
        assertEquals(
            InstallDecision.REJECTED,
            SkillDiscoveryInventory.decideInstall(
                DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(treffer)),
                zustimmungFuer(treffer)
            )
        )
    }

    @Test
    fun `ein fehlender Fundort schliesst die Freigabe aus`() {
        val treffer = perfekterTreffer(provenance = ProvenanceEvidence.MISSING)
        assertEquals(
            InstallDecision.REJECTED,
            SkillDiscoveryInventory.decideInstall(
                DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(treffer)),
                zustimmungFuer(treffer)
            )
        )
    }

    @Test
    fun `eine ungepruefte Kompatibilitaet schliesst die Freigabe aus`() {
        val treffer = perfekterTreffer(compatibility = CompatibilityVerdict.NOT_ASSESSED)
        assertEquals(
            "Eine stillschweigend angenommene Kompatibilitaet waere erfunden.",
            InstallDecision.REJECTED,
            SkillDiscoveryInventory.decideInstall(
                DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(treffer)),
                zustimmungFuer(treffer)
            )
        )
    }

    @Test
    fun `eine inkompatible Umgebung schliesst die Freigabe aus und nennt den Grund`() {
        val treffer = perfekterTreffer(
            compatibility = CompatibilityVerdict.INCOMPATIBLE,
            compatibilityOffender = "setzt Docker voraus"
        )
        assertEquals(
            InstallDecision.REJECTED,
            SkillDiscoveryInventory.decideInstall(
                DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(treffer)),
                zustimmungFuer(treffer)
            )
        )
        assertEquals("setzt Docker voraus", SkillDiscoveryInventory.compatibilityLine(treffer))
    }

    @Test
    fun `eine Inkompatibilitaet ohne genannten Grund wird nicht erfunden`() {
        val treffer = perfekterTreffer(compatibility = CompatibilityVerdict.INCOMPATIBLE)
        assertEquals(
            "Keine Ursache erfinden, nur den Pauschalwert nennen.",
            "incompatible",
            SkillDiscoveryInventory.compatibilityLine(treffer)
        )
    }

    @Test
    fun `der perfekte Treffer mit passender Zustimmung wird freigegeben`() {
        val treffer = perfekterTreffer()
        assertEquals(
            InstallDecision.AUTHORIZED,
            SkillDiscoveryInventory.decideInstall(
                DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(treffer)),
                zustimmungFuer(treffer)
            )
        )
    }

    // ── Zusage 3: Luecken bleiben offen ─────────────────────────────────

    @Test
    fun `nichts gefunden ist ein Ergebnis und kein Fehler`() {
        val luecke = DiscoveryOutcome.Gap(vollstaendigerLauf(), "kein Treffer im globalen Umfang")
        assertEquals(SearchOutcome.NOT_FOUND, SkillDiscoveryInventory.searchOutcome(luecke))
        assertTrue(luecke.isGap)
        assertTrue(
            "Eine Luecke hat keine Treffer.",
            SkillDiscoveryInventory.decideInstall(luecke, zustimmungFuer(perfekterTreffer())) ==
                InstallDecision.REJECTED
        )
    }

    @Test
    fun `eine leere Trefferliste gilt nicht als vollstaendig abgedeckt`() {
        val leer = DiscoveryOutcome.Matches(vollstaendigerLauf(), emptyList())
        assertEquals(SearchOutcome.NOT_FOUND, SkillDiscoveryInventory.searchOutcome(leer))
        assertTrue("Ohne Treffer ist die Frage offen.", leer.isGap)
    }

    @Test
    fun `ein Treffer ohne geprueften Inhalt haelt den Lauf offen`() {
        val ungeprueft = perfekterTreffer().copy(content = ContentEvidence.NotInspected)
        val ergebnis = DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(ungeprueft))
        assertEquals(SearchOutcome.PARTIAL, SkillDiscoveryInventory.searchOutcome(ergebnis))
        assertTrue(SkillDiscoveryInventory.isOpenGap(ungeprueft))
    }

    @Test
    fun `eine Lizenz ohne Pruefung haelt den Lauf offen`() {
        val ungeprueft = perfekterTreffer().copy(
            licenseEvidence = DiscoveredLicenseEvidence.NOT_INSPECTED
        )
        assertTrue(SkillDiscoveryInventory.isOpenGap(ungeprueft))
        assertEquals(SearchOutcome.PARTIAL, SkillDiscoveryInventory.searchOutcome(
            DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(ungeprueft))
        ))
    }

    @Test
    fun `ein vollstaendig belegter Lauf gilt als abgedeckt`() {
        val ergebnis = DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(perfekterTreffer()))
        assertEquals(SearchOutcome.COMPLETE, SkillDiscoveryInventory.searchOutcome(ergebnis))
        assertFalse(ergebnis.isGap)
    }

    @Test
    fun `ein Treffer zum falschen Begriff macht den Lauf offen`() {
        val falsch = DiscoveredSkill(
            name = "falsch",
            searchedWith = SearchTerm(label = "", term = "git", taskId = "095"),
            discoveredIn = "~/.claude/skills/falsch"
        )
        assertFalse(falsch.matchesTerm)
        val ergebnis = DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(falsch))
        assertTrue(ergebnis.isGap)
        assertEquals(SearchOutcome.NOT_FOUND, SkillDiscoveryInventory.searchOutcome(ergebnis))
    }

    // ── Protokollzeilen ─────────────────────────────────────────────────

    @Test
    fun `die Begruendung wird gefuehrt oder als fehlend gesagt`() {
        val mitBegruendung = perfekterTreffer(rationale = "rein anweisend, passt zur Aufgabenart")
        assertTrue(
            SkillDiscoveryInventory.rationaleLine(mitBegruendung)
                .contains("rein anweisend, passt zur Aufgabenart")
        )

        val ohneBegruendung = perfekterTreffer(rationale = "")
        assertEquals(
            "reasoning: not recorded",
            SkillDiscoveryInventory.rationaleLine(ohneBegruendung)
        )
    }

    @Test
    fun `die Kandidatenzeile nennt Begriff Ort Lizenz Inhalt Kompatibilitaet`() {
        val zeile = SkillDiscoveryInventory.candidateLine(
            perfekterTreffer(rationale = "rein anweisend")
        )
        listOf("git-credentials", "~/.claude/skills", "license:", "content:", "compatibility:")
            .forEach { teil ->
                assertTrue("Die Zeile nennt '$teil' nicht: $zeile", zeile.contains(teil))
            }
    }

    @Test
    fun `eine fehlende Lizenzdatei wird beim Aufnehmen als Grund genannt`() {
        assertTrue(
            SkillDiscoveryInventory
                .explainInsertion(perfekterTreffer(license = DiscoveredLicenseEvidence.FILE_MISSING), false)
                .contains("Lizenzdatei fehlt")
        )
        assertTrue(
            SkillDiscoveryInventory
                .explainInsertion(perfekterTreffer(provenance = ProvenanceEvidence.MISSING), false)
                .contains("kein Fundort")
        )
        assertTrue(
            SkillDiscoveryInventory
                .explainInsertion(perfekterTreffer(), true)
                .contains("begründet")
        )
    }

    @Test
    fun `die Ablehnung nennt immer einen Grund`() {
        val ergebnis = DiscoveryOutcome.Matches(vollstaendigerLauf(), listOf(perfekterTreffer()))

        assertTrue(
            SkillDiscoveryInventory.explanation(DiscoveryOutcome.Gap(vollstaendigerLauf(), "x"), null)
                .contains("kein passender Skill")
        )
        assertTrue(
            SkillDiscoveryInventory.explanation(ergebnis, null).contains("keine Zustimmung")
        )
        val entzogen = SkillDiscoveryInventory.revokeConsent(
            zustimmungFuer(perfekterTreffer()),
            revokedAt = 5_000L
        )
        assertTrue(
            SkillDiscoveryInventory.explanation(ergebnis, entzogen).contains("zurückgenommen")
        )
        assertTrue(
            SkillDiscoveryInventory.explanation(ergebnis, zustimmungFuer(perfekterTreffer()))
                .contains("Installation freigegeben")
        )
    }

    // ── Trennung zu Aufgabe 123 ─────────────────────────────────────────

    @Test
    fun `ein Treffer wird an die Lieferkettenpruefung delegiert`() {
        assertTrue(SkillDiscoveryInventory.useSupplyChainReview())
        assertEquals(
            org.claudroide.app.core.security.Verdict.USABLE,
            SkillDiscoveryInventory.delegateToSupplyChainReview(perfekterTreffer())
        )
    }

    @Test
    fun `ein ausfuehrender Treffer wird auch von Aufgabe 123 abgelehnt`() {
        // Der Weg darf nicht laxer sein als der bereits bewiesene.
        val befehlend = perfekterTreffer().copy(
            content = ContentEvidence.Inspected(
                ContentInspection(
                    findings = listOf(ContentFinding("fuehrt aus", "ruft curl auf")),
                    executesCommands = true
                )
            )
        )
        assertEquals(
            org.claudroide.app.core.security.Verdict.REJECTED,
            SkillDiscoveryInventory.delegateToSupplyChainReview(befehlend)
        )
    }

    @Test
    fun `ein Treffer mit fehlender Lizenzdatei wird auch von Aufgabe 123 abgelehnt`() {
        assertEquals(
            org.claudroide.app.core.security.Verdict.REJECTED,
            SkillDiscoveryInventory.delegateToSupplyChainReview(
                perfekterTreffer(license = DiscoveredLicenseEvidence.FILE_MISSING)
            )
        )
    }

    @Test
    fun `ein ungepruefter Treffer erreicht auch bei Delegation kein Urteil`() {
        assertEquals(
            "Ohne Lektüre gibt es kein Urteil.",
            org.claudroide.app.core.security.Verdict.REJECTED,
            SkillDiscoveryInventory.delegateToSupplyChainReview(
                perfekterTreffer().copy(content = ContentEvidence.NotInspected)
            )
        )
    }

    // ── Hilfen ──────────────────────────────────────────────────────────

    private fun vollstaendigerLauf() = SearchRun(
        terms = listOf(SearchTerm("git-credentials", "git credential", "095")),
        source = DiscoverySourceKind.GLOBAL_SKILLS,
        searchedScope = "~/.claude/skills",
        isGlobal = true
    )

    /** Ein Treffer, der alle vier Bedingungen der Freigabe erfuellt. */
    private fun perfekterTreffer(
        name: String = "git-credentials",
        provenance: ProvenanceEvidence = ProvenanceEvidence.RECORDED,
        license: DiscoveredLicenseEvidence = DiscoveredLicenseEvidence.DOCUMENT_PRESENT,
        compatibility: CompatibilityVerdict = CompatibilityVerdict.COMPATIBLE,
        compatibilityOffender: String? = null,
        rationale: String = "rein anweisend, belegt am Dateisystem geprueft"
    ) = DiscoveredSkill(
        name = name,
        searchedWith = SearchTerm("git-credentials", "git credential", "095"),
        discoveredIn = "~/.claude/skills/$name",
        provenance = provenance,
        licenseEvidence = license,
        content = ContentEvidence.Inspected(
            ContentInspection(
                findings = listOf(ContentFinding("rein anweisend", "SKILL.md enthaelt nur Prosa"))
            )
        ),
        compatibility = compatibility,
        compatibilityOffender = compatibilityOffender ?: "",
        rationale = rationale
    )

    private fun zustimmungFuer(treffer: DiscoveredSkill): SkillConsent =
        SkillDiscoveryInventory.recordConsent(
            SkillDiscoveryInventory.requestInstall(treffer, "129"),
            ConsentState.GRANTED,
            recordedAt = 1_000L
        )
}