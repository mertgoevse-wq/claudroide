package org.claudroide.app

import org.claudroide.app.feature.provider.ComparisonValue
import org.claudroide.app.feature.provider.PrivacyCheck
import org.claudroide.app.feature.provider.PrivacyEnforcement
import org.claudroide.app.feature.provider.PrivacyFact
import org.claudroide.app.feature.provider.PrivacyTopic
import org.claudroide.app.feature.provider.ProviderPrivacyRegistry
import org.claudroide.app.feature.provider.UnknownFact
import org.claudroide.app.feature.provider.compareProviders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 125 — „Datenschutz je Anbieter“.
 *
 * Die zentrale Zusage steht an erster Stelle: **Unbekanntes wird als offen
 * markiert.** Eine unbelegte Angabe darf nicht wie eine Tatsache dastehen.
 */
class ProviderPrivacyProfileTest {

    private val quelle = "https://example.invalid/privacy"

    private fun belegt(text: String) = PrivacyFact(text, quelle, "2026-10-02")

    private fun offen(topic: PrivacyTopic, detail: String = "in der Anbieterdokumentation nicht gefunden") =
        UnknownFact(topic, detail)

    private val vollstaendig = ProviderPrivacyRegistry.profile(
        providerId = "anthropic",
        displayName = "Anthropic Claude API",
        known = listOf(
            belegt("Speicherung der Eingabe: gespeichert"),
            belegt("Verarbeitungsregion: Region bekannt"),
            belegt("Verwendung zum Training: nicht zum Training")
        ),
        unknown = emptyList()
    )

    private val mitLuecke = ProviderPrivacyRegistry.profile(
        providerId = "openrouter",
        displayName = "OpenRouter",
        known = listOf(belegt("Speicherung der Eingabe: gespeichert")),
        unknown = listOf(offen(PrivacyTopic.DATA_REGION), offen(PrivacyTopic.MODEL_TRAINING))
    )

    private val alleThemen = ProviderPrivacyRegistry.profile(
        providerId = "vollstaendig",
        displayName = "Dienst mit Angaben zu allen Themen",
        known = listOf(
            belegt("Speicherung der Eingabe: gespeichert"),
            belegt("Aufbewahrung der Protokolle: gespeichert"),
            belegt("Verarbeitungsregion: Region bekannt"),
            belegt("Verwendung zum Training: nicht zum Training"),
            belegt("Weitergabe an Dritte: nicht")
        ),
        unknown = emptyList()
    )

    // ── Unbekanntes wird als offen markiert ───────────────────────────

    @Test
    fun `eine offene Frage macht das Profil offen`() {
        assertTrue(mitLuecke.hasOpenQuestions)
        assertFalse(vollstaendig.hasOpenQuestions)
    }

    @Test
    fun `ein vollstaendig belegtes Profil traegt keine offene Frage`() {
        assertTrue(vollstaendig.isFullyDocumented)
    }

    @Test
    fun `die erklaerung nennt jede offene Frage woertlich`() {
        val zeilen = mitLuecke.explanationLines()
        assertTrue(zeilen.any { it.contains("Offen:") })
        assertTrue(zeilen.any { it.contains("Verarbeitungsregion: offen") })
        assertTrue(zeilen.any { it.contains("Verwendung zum Training: offen") })
    }

    @Test
    fun `eine offene Frage sagt nicht nein sondern offen`() {
        val zeile = offen(PrivacyTopic.DATA_REGION).displayLine()
        assertTrue(zeile.contains("offen"))
        assertFalse(
            "Eine offene Frage ist keine Negation.",
            zeile.contains("nicht gespeichert")
        )
    }

    @Test
    fun `eine belegte Angabe ohne Quelle ist unzulaessig`() {
        try {
            PrivacyFact("Speicherung: gespeichert", "", "2026-10-02")
            org.junit.Assert.fail("Eine Angabe ohne Quelle darf keine Tatsache sein.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `eine belegte Angabe ohne Pruefdatum ist unzulaessig`() {
        try {
            PrivacyFact("Speicherung: gespeichert", quelle, "  ")
            org.junit.Assert.fail("Eine Angabe ohne Prüfdatum darf keine Tatsache sein.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    @Test
    fun `die belegte Angabe zeigt Quelle und Datum`() {
        val zeilen = vollstaendig.explanationLines()
        assertTrue(zeilen.any { it.contains(quelle) })
        assertTrue(zeilen.any { it.contains("2026-10-02") })
    }

    // ── Vergleich ─────────────────────────────────────────────────────

    @Test
    fun `der Vergleich stellt beide Anbieter gegenueber`() {
        val vergleich = compareProviders(vollstaendig, mitLuecke)
        assertEquals(PrivacyTopic.entries.size, vergleich.rows.size)
    }

    @Test
    fun `eine fehlende Angabe erscheint als offen nicht als leerer Wert`() {
        val zeile = compareProviders(vollstaendig, mitLuecke).rows
            .first { it.topic == PrivacyTopic.DATA_REGION }
        assertEquals(ComparisonValue.REGION_KNOWN, zeile.valueA)
        assertEquals(
            "Beim zweiten Anbieter ist die Region offen, nicht leer.",
            ComparisonValue.OPEN,
            zeile.valueB
        )
    }

    @Test
    fun `eine offene Frage schlaegt eine vorhandene Angabe`() {
        // Solange die Frage offen ist, ist der Vergleichswert „offen" —
        // eine vorhandene Teilangabe darf sie nicht verdecken.
        val gemischt = ProviderPrivacyRegistry.profile(
            providerId = "x",
            displayName = "X",
            known = listOf(belegt("Verarbeitungsregion: Region bekannt")),
            unknown = listOf(offen(PrivacyTopic.DATA_REGION))
        )
        val zeile = compareProviders(gemischt, gemischt).rows
            .first { it.topic == PrivacyTopic.DATA_REGION }
        assertEquals(ComparisonValue.OPEN, zeile.valueA)
    }

    @Test
    fun `der Vergleich meldet offene Fragen`() {
        assertTrue(compareProviders(vollstaendig, mitLuecke).hasOpenQuestions)
        // „Vollständig" heißt: **keine ausdrücklich offene Frage**. Ein Profil,
        // das zu einem Thema schlicht nichts sagt, liefert im Vergleich trotzdem
        // „offen" — und das ist richtig: Die App weiß es dann nicht.
        assertTrue(compareProviders(vollstaendig, vollstaendig).hasOpenQuestions)
        assertFalse(
            "Ein Profil mit Angaben zu allen Themen hat keine offene Zelle.",
            compareProviders(alleThemen, alleThemen).hasOpenQuestions
        )
    }

    @Test
    fun `der Vergleich kennt kein stilles Nein`() {
        // Jedes Thema wird bewertet; „unbekannt" heißt OPEN, nicht NOT_STORED.
        val zeile = compareProviders(mitLuecke, mitLuecke).rows
            .first { it.topic == PrivacyTopic.MODEL_TRAINING }
        assertEquals(ComparisonValue.OPEN, zeile.valueA)
        assertFalse(zeile.valueA == ComparisonValue.NOT_USED_FOR_TRAINING)
    }

    // ── Keine vertraulichen Daten ohne bekanntes Ziel und Umfang ───────

    @Test
    fun `ohne Bestaetigung wird nicht gesendet`() {
        val pruefung = PrivacyEnforcement.canSend(vollstaendig, acknowledged = false, scopeLines = listOf("1 Datei"))
        assertTrue(pruefung is PrivacyCheck.NotAcknowledged)
    }

    @Test
    fun `ohne benannten Umfang wird nicht gesendet`() {
        val pruefung = PrivacyEnforcement.canSend(vollstaendig, acknowledged = true, scopeLines = emptyList())
        assertTrue(pruefung is PrivacyCheck.NotAcknowledged)
    }

    @Test
    fun `offene Fragen blockieren den Versand`() {
        val pruefung = PrivacyEnforcement.canSend(mitLuecke, acknowledged = true, scopeLines = listOf("1 Datei"))
        assertTrue(pruefung is PrivacyCheck.OpenQuestions)
    }

    @Test
    fun `erst bei belegtem Profil und Bestaetigung wird gesendet`() {
        val pruefung = PrivacyEnforcement.canSend(
            vollstaendig,
            acknowledged = true,
            scopeLines = listOf("ChatScreen.kt", "README.md")
        )
        assertTrue(pruefung is PrivacyCheck.MaySend)
        assertEquals(2, (pruefung as PrivacyCheck.MaySend).scopeLines.size)
    }

    @Test
    fun `der Umfang wird zurueckgegeben damit der Nutzer ihn sieht`() {
        val pruefung = PrivacyEnforcement.canSend(
            vollstaendig,
            acknowledged = true,
            scopeLines = listOf("ChatScreen.kt", "README.md", "secrets.txt")
        ) as PrivacyCheck.MaySend
        assertTrue(pruefung.scopeLines.any { it.contains("secrets.txt") })
    }

    @Test
    fun `die Bestaetigung gilt nur fuer genau diesen Anbieter`() {
        // Ein Profil wird nicht an ein anderes weitergegeben: jedes Profil
        // braucht seine eigene Pruefung.
        val aGeprueft = PrivacyEnforcement.canSend(vollstaendig, acknowledged = true, scopeLines = listOf("x"))
        val bGeprueft = PrivacyEnforcement.canSend(mitLuecke, acknowledged = false, scopeLines = listOf("x"))
        assertTrue(aGeprueft is PrivacyCheck.MaySend)
        assertTrue(bGeprueft is PrivacyCheck.NotAcknowledged)
    }
}