package org.claudroide.app

import org.claudroide.app.core.security.LicenseEvidence
import org.claudroide.app.core.security.ReviewEvidence
import org.claudroide.app.core.security.SkillCandidate
import org.claudroide.app.core.security.SkillFinding
import org.claudroide.app.core.security.SkillSupplyChainReview
import org.claudroide.app.core.security.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 123 — „Skill-Quelle prüfen".
 *
 * Die beiden Bedingungen der Aufgabe stehen an erster Stelle: nichts
 * ausführen vor der Prüfung, und nicht lizenzierte Skills ablehnen. Der
 * dritte Schutz — Suche ist keine Installationsfreigabe — hat seinen eigenen
 * Abschnitt am Ende.
 */
class SkillSupplyChainReviewTest {

    private val geprueft = ReviewEvidence(
        contentInspected = true,
        executesCommands = false,
        installsOrDownloads = false
    )

    private fun kandidat(
        name: String = "test-skill",
        origin: String = "global, ~/.claude/skills/",
        license: LicenseEvidence = LicenseEvidence.VERIFIED
    ) = SkillCandidate(name = name, origin = origin, license = license)

    // ── Nichts ausführen, bevor der Inhalt geprüft ist ────────────────

    @Test
    fun `ohne geprueften Inhalt gibt es kein Urteil`() {
        val ungeprueft = ReviewEvidence(contentInspected = false)
        assertFalse("Ohne Lektüre darf der Skill laufen.", ungeprueft.mayRun)
        assertEquals(
            Verdict.REJECTED,
            SkillSupplyChainReview.review(kandidat(), ungeprueft)
        )
    }

    @Test
    fun `ohne Inhaltspruefung hilft auch eine perfekte Lizenz nicht`() {
        // Der sonst sauberste Kandidat: passende Herkunft, belegte Lizenz.
        // Ohne gelesenen Inhalt bleibt er abgelehnt — sonst würde man eine
        // Lizenzprüfung mit einer Inhaltsprüfung verwechseln.
        val bester = kandidat(license = LicenseEvidence.VERIFIED)
        val ungeprueft = ReviewEvidence(contentInspected = false)
        assertEquals(Verdict.REJECTED, SkillSupplyChainReview.review(bester, ungeprueft))
    }

    @Test
    fun `die Ablehnung wegen fehlender Pruefung nennt den Grund`() {
        val text = SkillSupplyChainReview.explanation(
            kandidat(),
            ReviewEvidence(contentInspected = false)
        )
        assertTrue("Der Grund muss die fehlende Prüfung benennen.", text.contains("geprüft"))
    }

    // ── Nicht lizenzierte Skills werden abgelehnt ─────────────────────

    @Test
    fun `eine nicht angegebene Lizenz wird abgelehnt`() {
        val ohneAngabe = kandidat(license = LicenseEvidence.NOT_STATED)
        assertEquals(
            "Ohne Lizenzangabe ist der Skill nicht einsetzbar.",
            Verdict.REJECTED,
            SkillSupplyChainReview.review(ohneAngabe, geprueft)
        )
    }

    @Test
    fun `ein Verweis auf eine fehlende Lizenzdatei ist kein Beleg`() {
        // Der echte Befund aus der Lieferkettenpruefung: vier installierte
        // Skills nennen "Complete terms in LICENSE.txt", die Datei fehlt.
        val ohneDokument = kandidat(license = LicenseEvidence.UNVERIFIABLE_REFERENCE)
        assertEquals(
            "Ein Verweis ohne Dokument belegt keine Nutzungsrechte.",
            Verdict.REJECTED,
            SkillSupplyChainReview.review(ohneDokument, geprueft)
        )
    }

    @Test
    fun `die Lizenzbegruendung nennt den Zustand der Lizenz`() {
        val text = SkillSupplyChainReview.explanation(
            kandidat(license = LicenseEvidence.UNVERIFIABLE_REFERENCE),
            geprueft
        )
        assertTrue(text.contains("abgelehnt"))
    }

    // ── Verdächtiger Inhalt wird abgelehnt ───────────────────────────

    @Test
    fun `ein Skill der Befehle ausfuehrt wird abgelehnt`() {
        val ausfuehrend = ReviewEvidence(
            contentInspected = true,
            executesCommands = true,
            installsOrDownloads = false
        )
        assertEquals(
            Verdict.REJECTED,
            SkillSupplyChainReview.review(kandidat(), ausfuehrend)
        )
    }

    @Test
    fun `ein Skill der nachinstalliert wird abgelehnt`() {
        val nachinstallierend = ReviewEvidence(
            contentInspected = true,
            executesCommands = false,
            installsOrDownloads = true
        )
        assertEquals(
            Verdict.REJECTED,
            SkillSupplyChainReview.review(kandidat(), nachinstallierend)
        )
    }

    @Test
    fun `die Reihenfolge der Pruefung nennt zuerst den Inhalt`() {
        // Ein Kandidat, der beides nicht tut: ungeprueft UND nicht lizenziert.
        // genannt wird der erste Grund, weil ohne Lektüre kein Urteil möglich ist.
        val text = SkillSupplyChainReview.explanation(
            kandidat(license = LicenseEvidence.NOT_STATED),
            ReviewEvidence(contentInspected = false)
        )
        assertTrue(text.contains("nicht ausgewertet"))
    }

    // ── Der geprüfte, unbedenkliche Fall ─────────────────────────────

    @Test
    fun `ein gepruefter und lizenzierter Skill ist einsetzbar`() {
        assertEquals(Verdict.USABLE, SkillSupplyChainReview.review(kandidat(), geprueft))
    }

    @Test
    fun `der Einsatz traegt die Begruendung mit`() {
        val text = SkillSupplyChainReview.explanation(kandidat(), geprueft)
        assertTrue(text.contains("lizenziert"))
    }

    @Test
    fun `der Standardfall einer ungeprueften Lizenz ist nicht geprueft`() {
        // Wer den Lizenzparameter weglässt, darf nicht versehentlich als
        // belegt durchgehen.
        val ohneAngabe = SkillCandidate(name = "x", origin = "global")
        assertEquals(LicenseEvidence.NOT_STATED, ohneAngabe.license)
        assertEquals(Verdict.REJECTED, SkillSupplyChainReview.review(ohneAngabe, geprueft))
    }

    // ── Suche ist keine Installationsfreigabe ────────────────────────

    @Test
    fun `eine Installation verlangt immer die Zustimmung des Nutzers`() {
        assertTrue(
            "Suche ist keine automatische Installationsfreigabe.",
            SkillSupplyChainReview.installRequiresUserConsent()
        )
    }

    @Test
    fun `auch ein einsetzbarer Skill braucht keine stillschweigende Freigabe`() {
        // Die Beurteilung sagt nichts über eine Installation aus: auch bei
        // USABLE bleibt die Zustimmungspflicht bestehen.
        val urteil = SkillSupplyChainReview.review(kandidat(), geprueft)
        assertEquals(Verdict.USABLE, urteil)
        assertTrue(SkillSupplyChainReview.installRequiresUserConsent())
    }

    // ── Herkunft nachprüfbar ──────────────────────────────────────────

    @Test
    fun `eine leere Herkunft wird abgewiesen`() {
        var geworfen = false
        try {
            SkillSupplyChainReview.originLine(kandidat(origin = "   "))
        } catch (e: IllegalArgumentException) {
            geworfen = true
        }
        assertTrue("Eine Herkunft ohne Angabe ist nicht prüfbar.", geworfen)
    }

    @Test
    fun `die Herkunftszeile nennt Name und Ort`() {
        val zeile = SkillSupplyChainReview.originLine(kandidat(name = "adaptive", origin = "global"))
        assertTrue(zeile.contains("adaptive"))
        assertTrue(zeile.contains("global"))
    }

    // ── Der geprüfte Inhalt wird festgehalten ─────────────────────────

    @Test
    fun `ein Befund wird verstaendlich beschrieben`() {
        val befund = SkillFinding("Führt keine Befehle aus und greift nicht auf das Netz zu.")
        assertTrue(befund.describe().contains("Netz"))
    }

    @Test
    fun `der Kandidat traegt seine benoetigten Werkzeuge und Befunde`() {
        val k = SkillCandidate(
            name = "adaptive",
            origin = "global",
            license = LicenseEvidence.VERIFIED,
            content = listOf(SkillFinding("reiner Anweisungstext")),
            requiredTools = listOf("Kontext7")
        )
        assertEquals(listOf("Kontext7"), k.requiredTools)
        assertEquals(1, k.content.size)
        assertEquals(Verdict.USABLE, SkillSupplyChainReview.review(k, geprueft))
    }
}
