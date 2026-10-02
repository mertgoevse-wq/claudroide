package org.claudroide.app

import org.claudroide.app.feature.agent.AgentFeature
import org.claudroide.app.feature.agent.CapabilityDecision
import org.claudroide.app.feature.agent.InputFormat
import org.claudroide.app.feature.agent.ProviderCapabilityEvidence
import org.claudroide.app.feature.agent.ProviderCapabilityPolicy
import org.claudroide.app.feature.agent.ProviderCapabilityProfile
import org.claudroide.app.feature.agent.Workaround
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 080 — „Anbieterfunktionen abgleichen“.
 *
 * Der Fähigkeitsabgleich entscheidet, was der Agent tun darf. Die Tests gehen
 * deshalb jede [AgentFeature] einzeln durch — eine neue Funktion, die niemand
 * prüft, würde sonst unbemerkt mitlaufen.
 */
class ProviderCapabilityPolicyTest {

    // ── Testhilfen ───────────────────────────────────────────────────────────

    /** Ein Profil, das alle sechs Werkzeuge und beide Formate belegt hat. */
    private fun full(): ProviderCapabilityProfile = ProviderCapabilityProfile(
        providerId = "anthropic",
        modelId = "claude-sonnet-5-5",
        supportedTools = setOf(
            "read_file", "list_directory", "search_text", "write_file", "delete_file", "run_test"
        ),
        supportedFormats = setOf(InputFormat.TEXT, InputFormat.IMAGE, InputFormat.PDF),
        maxInputTokens = 1_000_000L,
        isVerified = true
    )

    /** Ein Profil, das nur lesen kann. */
    private fun readOnly(): ProviderCapabilityProfile = full().copy(
        supportedTools = setOf("read_file", "list_directory", "search_text")
    )

    private fun policy(withWorkarounds: Map<AgentFeature, Workaround> = emptyMap()) =
        ProviderCapabilityPolicy(withWorkarounds)

    private fun lines(decision: CapabilityDecision): String = when (decision) {
        is CapabilityDecision.Ready -> decision.feature.germanLabel
        is CapabilityDecision.Unverified -> decision.germanLines.joinToString("\n")
        is CapabilityDecision.Unsupported -> decision.germanLines.joinToString("\n")
        is CapabilityDecision.WorkaroundAvailable -> decision.germanLines.joinToString("\n")
    }

    // ── Jede Funktion einzeln ────────────────────────────────────────────────

    @Test
    fun `jede Funktion laesst sich mit dem vollen Profil ausfuehren`() {
        AgentFeature.values().forEach { feature ->
            assertTrue(
                "${feature.germanLabel} sollte mit vollem Profil lauffaehig sein",
                policy().check(feature, full()).isExecutable
            )
        }
    }

    @Test
    fun `jede Funktion wird bei einem leeren Profil abgewiesen`() {
        val leer = full().copy(supportedTools = emptySet(), supportedFormats = emptySet())
        AgentFeature.values().forEach { feature ->
            assertFalse(
                "${feature.germanLabel} sollte ohne Faehigkeiten abgewiesen werden",
                policy().check(feature, leer).isExecutable
            )
        }
    }

    @Test
    fun `nur lesen erlaubt Lesen aber keine Aenderung`() {
        val p = policy()
        assertTrue(p.check(AgentFeature.FILE_READING, readOnly()).isExecutable)
        assertFalse(p.check(AgentFeature.FILE_EDITS, readOnly()).isExecutable)
    }

    @Test
    fun `ohne Testwerkzeug laesst sich kein Test starten`() {
        val ohneTests = full().copy(supportedTools = setOf("read_file", "list_directory", "search_text"))
        assertFalse(policy().check(AgentFeature.TEST_RUNNING, ohneTests).isExecutable)
    }

    // ── Nicht belegt heisst nicht vorhanden ──────────────────────────────────

    @Test
    fun `ohne Nachweis gilt keine Faehigkeit als vorhanden`() {
        val unbelegt = full().copy(isVerified = false)
        assertFalse(policy().check(AgentFeature.FILE_READING, unbelegt).isExecutable)
    }

    @Test
    fun `ohne Nachweis heisst die Meldung Unverified und nicht Unsupported`() {
        val d = policy().check(AgentFeature.FILE_READING, full().copy(isVerified = false))
        assertTrue(d is CapabilityDecision.Unverified)
    }

    @Test
    fun `ohne Nachweis wird kein Ersatzweg vorgeschlagen`() {
        // Solange unklar ist, OB die Faehigkeit fehlt, waere jede Ersatzliste eine
        // Behauptung. Deshalb wird hier nicht gefragt.
        val mitErsatzweg = policy(
            mapOf(
                AgentFeature.FILE_EDITS to Workaround(
                    germanDescription = "Änderung als Textvorschlag ausgeben",
                    requiresFreshApproval = true,
                    isFree = true
                )
            )
        )
        val d = mitErsatzweg.check(AgentFeature.FILE_EDITS, full().copy(isVerified = false))
        assertTrue(d is CapabilityDecision.Unverified)
    }

    @Test
    fun `die Meldung nennt die Belegquelle`() {
        val d = policy().check(AgentFeature.FILE_READING, full().copy(isVerified = false))
        assertTrue(lines(d).contains(ProviderCapabilityEvidence.CAPABILITY_DOC_URL))
    }

    @Test
    fun `die Belegquelle traegt ihr Pruefdatum im Code`() {
        assertEquals("2026-10-02", ProviderCapabilityEvidence.CAPABILITY_DOC_VERIFIED)
        assertTrue(ProviderCapabilityEvidence.CAPABILITY_DOC_URL.startsWith("https://"))
    }

    // ── Fehlende Faehigkeit ──────────────────────────────────────────────────

    @Test
    fun `die Meldung nennt die fehlenden Werkzeuge beim Namen`() {
        val d = policy().check(AgentFeature.FILE_EDITS, readOnly())
        assertTrue(d is CapabilityDecision.Unsupported)
        assertEquals(listOf("delete_file", "write_file"), (d as CapabilityDecision.Unsupported).missingToolNames)
        assertTrue(lines(d).contains("write_file"))
        assertTrue(lines(d).contains("delete_file"))
    }

    @Test
    fun `die Meldung nennt das fehlende Format im Klartext`() {
        val ohnePdf = full().copy(supportedFormats = setOf(InputFormat.TEXT, InputFormat.IMAGE))
        val d = policy().check(AgentFeature.PDF_REVIEW, ohnePdf)
        assertEquals(
            listOf(InputFormat.PDF),
            (d as CapabilityDecision.Unsupported).missingFormats
        )
        assertTrue(lines(d).contains("PDF-Dokument"))
    }

    @Test
    fun `Bild und PDF sind getrennt pruefbar`() {
        val nurText = full().copy(supportedFormats = setOf(InputFormat.TEXT))
        assertTrue(policy().check(AgentFeature.IMAGE_REVIEW, nurText).isExecutable.not())
        assertTrue(policy().check(AgentFeature.PDF_REVIEW, nurText).isExecutable.not())
    }

    @Test
    fun `ohne Ersatzweg heisst es ausdruecklich dass es keinen gibt`() {
        assertTrue(lines(policy().check(AgentFeature.FILE_EDITS, readOnly())).contains("keinen Ersatzweg"))
    }

    // ── Kein stiller Anbieterwechsel ─────────────────────────────────────────

    @Test
    fun `eine fehlende Faehigkeit nennt keinen anderen Anbieter als Ersatz`() {
        val text = lines(policy().check(AgentFeature.FILE_EDITS, readOnly()))
        assertTrue(text.contains("Es wird kein anderer Anbieter und keine andere Anmeldung verwendet."))
    }

    @Test
    fun `die Meldung verweist fuer einen Wechsel auf die eigene Zustimmung`() {
        assertTrue(
            lines(policy().check(AgentFeature.FILE_EDITS, readOnly()))
                .contains("eigener Zustimmung")
        )
    }

    @Test
    fun `eine fehlende Faehigkeit ist nie ausfuehrbar`() {
        assertFalse(policy().check(AgentFeature.FILE_EDITS, readOnly()).isExecutable)
    }

    @Test
    fun `die Pruefung aendert weder Anbieter noch Modell im Profil`() {
        val vorher = readOnly()
        policy().check(AgentFeature.FILE_EDITS, vorher)
        policy().check(AgentFeature.PDF_REVIEW, vorher)
        assertEquals("anthropic", vorher.providerId)
        assertEquals("claude-sonnet-5-5", vorher.modelId)
    }

    // ── Ersatzweg mit Freigabe und Kosten ────────────────────────────────────

    @Test
    fun `ein Ersatzweg wird als WorkaroundAvailable gemeldet`() {
        val p = policy(
            mapOf(
                AgentFeature.FILE_EDITS to Workaround(
                    germanDescription = "Änderung als Textvorschlag ausgeben",
                    requiresFreshApproval = true,
                    isFree = true
                )
            )
        )
        assertTrue(p.check(AgentFeature.FILE_EDITS, readOnly()) is CapabilityDecision.WorkaroundAvailable)
    }

    @Test
    fun `ein Ersatzweg mit Dateiaenderung nennt die neue Freigabe`() {
        val p = policy(
            mapOf(
                AgentFeature.FILE_EDITS to Workaround(
                    germanDescription = "Änderung als Patch-Datei ablegen",
                    requiresFreshApproval = true,
                    isFree = true
                )
            )
        )
        val text = lines(p.check(AgentFeature.FILE_EDITS, readOnly()))
        assertTrue(text.contains("braucht eine neue Freigabe des Nutzers"))
    }

    @Test
    fun `ein Ersatzweg ohne Dateiaenderung nennt keine Freigabe`() {
        val p = policy(
            mapOf(
                AgentFeature.FILE_EDITS to Workaround(
                    germanDescription = "Nur den Vorschlag anzeigen",
                    requiresFreshApproval = false,
                    isFree = true
                )
            )
        )
        assertFalse(
            lines(p.check(AgentFeature.FILE_EDITS, readOnly()))
                .contains("braucht eine neue Freigabe")
        )
    }

    @Test
    fun `ein kostenpflichtiger Ersatzweg nennt die Kosten sichtbar`() {
        val p = policy(
            mapOf(
                AgentFeature.PDF_REVIEW to Workaround(
                    germanDescription = "PDF-Seiten als Bilder nachladen",
                    requiresFreshApproval = true,
                    isFree = false,
                    costNoticeLines = listOf("Das Nachladen kostet erneut Geld.")
                )
            )
        )
        val text = lines(p.check(AgentFeature.PDF_REVIEW, full().copy(supportedFormats = setOf(InputFormat.TEXT))))
        assertTrue(text.contains("Das Nachladen kostet erneut Geld."))
    }

    @Test
    fun `ein Ersatzweg ohne Beschreibung wird abgelehnt`() {
        val fehler = runCatching {
            Workaround(germanDescription = "  ", requiresFreshApproval = true, isFree = true)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `ein kostenpflichtiger Ersatzweg ohne Kostenhinweis wird abgelehnt`() {
        // Ohne diesen Zwang koennte ein Ersatzweg Geld kosten, ohne dass der
        // Nutzer es vorher gesehen hat.
        val fehler = runCatching {
            Workaround(
                germanDescription = "PDF-Seiten nachladen",
                requiresFreshApproval = true,
                isFree = false
            )
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    // ── Exakter Vergleich ────────────────────────────────────────────────────

    @Test
    fun `die Schreibweise des Werkzeugnamens spielt keine Rolle`() {
        val gemischt = full().copy(
            supportedTools = setOf("Read_File", "LIST_directory", " Search_Text ")
        )
        assertTrue(policy().check(AgentFeature.FILE_READING, gemischt).isExecutable)
    }

    @Test
    fun `ein aehnlich benanntes Werkzeug zaehlt nicht als das gesuchte`() {
        // Kein Praefix- oder Teilstringabgleich: "read_file_backup" ist nicht
        // "read_file", und eine solche Regel waere eine stille Faehigkeitsluecke.
        val fremd = full().copy(supportedTools = setOf("read_file_backup", "list_directory", "search_text"))
        assertFalse(policy().check(AgentFeature.FILE_READING, fremd).isExecutable)
    }

    @Test
    fun `ohne Fenstergroesse wird nichts behauptet`() {
        val ohneFenster = full().copy(maxInputTokens = null)
        assertTrue(ohneFenster.profileLine().contains("Fenstergröße unbekannt"))
    }

    @Test
    fun `ein Profil ohne Anbieterkennung wird abgelehnt`() {
        val fehler = runCatching { ProviderCapabilityProfile(providerId = "", modelId = "m") }
            .exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `ein Profil ohne Modellkennung wird abgelehnt`() {
        val fehler = runCatching { ProviderCapabilityProfile(providerId = "p", modelId = " ") }
            .exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `eine Fenstergroesse von null wird abgelehnt`() {
        val fehler = runCatching {
            ProviderCapabilityProfile(providerId = "p", modelId = "m", maxInputTokens = 0L)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    // ── Anzeige ──────────────────────────────────────────────────────────────

    @Test
    fun `der Kurzbericht nennt bei Bereitschaft Modell und Anbieter`() {
        val text = policy().reportLines(AgentFeature.FILE_READING, full()).joinToString("\n")
        assertTrue(text.contains("belegt und ausführbar"))
        assertTrue(text.contains("claude-sonnet-5-5"))
        assertTrue(text.contains("anthropic"))
    }

    @Test
    fun `der Kurzbericht nennt bei Unfähigkeit die fehlenden Werkzeuge`() {
        val text = policy().reportLines(AgentFeature.FILE_EDITS, readOnly()).joinToString("\n")
        assertTrue(text.contains("kann mit Modell"))
        assertTrue(text.contains("Fehlende Werkzeuge"))
    }

    @Test
    fun `das belegte Profil nennt seine Fenstergroesse`() {
        assertTrue(full().profileLine().contains("1000000 Tokens"))
    }
}
