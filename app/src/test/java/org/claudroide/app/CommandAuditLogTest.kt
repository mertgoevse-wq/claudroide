package org.claudroide.app

import org.claudroide.app.feature.agent.AuditActionKind
import org.claudroide.app.feature.agent.AuditApproval
import org.claudroide.app.feature.agent.AuditExport
import org.claudroide.app.feature.agent.AuditOutcome
import org.claudroide.app.feature.agent.AuditRecord
import org.claudroide.app.feature.agent.CommandAuditLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 115 — „Aktionsverlauf“.
 *
 * Zwei Zusicherungen tragen die Aufgabe: nichts Geheimes im Verlauf und
 * nichts Unnötiges darin. Sie stehen deshalb vorn — und der Export ist der
 * Moment, in dem der Verlauf das Gerät verlässt.
 */
class CommandAuditLogTest {

    private val approval = AuditApproval("FILE_CHANGE", "freigabe-1")

    private fun record(
        kind: AuditActionKind = AuditActionKind.READ_FILE,
        subject: String = "/storage/Projekt/src/Main.kt",
        purpose: String = "Datei lesen für die Analyse",
        outcome: AuditOutcome = AuditOutcome.SUCCEEDED,
        atMillis: Long = 1_700_000_000_000L,
        freigabe: AuditApproval = approval
    ) = AuditRecord.create(atMillis, kind, subject, purpose, outcome, freigabe)

    // ── Maskierung vor dem Speichern ──────────────────────────────────

    @Test
    fun `ein Schluessel im Zweck wird geschwaerzt`() {
        val eintrag = record(purpose = "Setze api_key=sk-ant-api03-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        assertFalse(
            "Ein Schlüssel darf nicht im Verlauf stehen.",
            eintrag.purpose.contains("sk-ant")
        )
        assertTrue(eintrag.purpose.contains("[REDACTED]"))
    }

    @Test
    fun `ein Schluessel im Ziel wird geschwaerzt`() {
        // Die App unterstützt Anthropic/OpenAI/OpenRouter/Bearer/PEM — ein
        // nackter AWS-Schlüssel gehört nicht dazu und wird bewusst nicht
        // als Sonderfall behandelt. Geprüft wird deshalb ein Schlüssel, der
        // in das Muster dieser App fällt.
        val eintrag = record(subject = "/data/id_rsa -----BEGIN RSA PRIVATE KEY-----ABC-----END RSA PRIVATE KEY-----")
        assertFalse(
            "Ein Schlüssel im Ziel darf nicht im Verlauf stehen.",
            eintrag.subject.contains("BEGIN RSA PRIVATE KEY")
        )
    }

    @Test
    fun `der Eintrag meldet dass geschwaerzt wurde`() {
        // Ein still verschwundener Wert wäre ein Betrugsverdacht, kein Schutz.
        val eintrag = record(purpose = "Token sk-live-ABCDEFGHIJKLMNOP einsetzen")
        assertTrue(eintrag.redactedFields.contains("Zweck"))
        assertTrue(eintrag.summaryLine().contains("geschwärzt"))
    }

    @Test
    fun `ohne Geheimnis wird nichts als geschwaerzt gemeldet`() {
        val eintrag = record()
        assertTrue(eintrag.redactedFields.isEmpty())
    }

    @Test
    fun `die Maskierung passiert beim Anlegen nicht erst bei der Anzeige`() {
        // Der Verlauf ist eine Speicherstelle: ein ungeschwärzter Wert wäre
        // schon im Speicher, bevor ihn jemand sieht.
        val log = CommandAuditLog()
        log.record(record(purpose = "Key sk-ant-api03-BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"))
        assertFalse(log.records.first().purpose.contains("sk-ant"))
    }

    @Test
    fun `auch der Export schwaerzt noch einmal`() {
        val log = CommandAuditLog()
        log.record(record(purpose = "Nutze ghp_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))
        val export = log.export()
        assertFalse(export.text.contains("ghp_AAAA"))
    }

    // ── Datensparsamkeit ───────────────────────────────────────────────

    @Test
    fun `der Eintrag traegt kein Feld fuer Ausgabe oder Inhalt`() {
        val felder = AuditRecord::class.java.declaredFields.map { it.name }
        val verboten = listOf("output", "stdout", "content", "fileContent", "rawOutput", "response")
        assertFalse(
            "Der Verlauf darf keinen Inhalt speichern — dafür darf es kein Feld geben.",
            felder.any { it in verboten }
        )
    }

    @Test
    fun `ein Nebeneffekt wird als solcher erkannt`() {
        assertTrue(record(kind = AuditActionKind.GIT_PUSH).hasSideEffect)
        assertTrue(record(kind = AuditActionKind.DELETE_FILE).hasSideEffect)
        assertFalse(record(kind = AuditActionKind.READ_FILE).hasSideEffect)
    }

    @Test
    fun `jede Aktionsart ist der Seitenwirkung zugeordnet`() {
        val verwaist = AuditActionKind.entries.filterNot { it in AuditRecord.SIDE_EFFECT_KINDS }
        // READ/LIST/SEARCH/TEST/MODEL_REQUEST sind die Nebenwirkungsfreien.
        assertEquals(5, verwaist.size)
    }

    // ── Lokal, ohne Teilen von allein ─────────────────────────────────

    @Test
    fun `der Verlauf hat keine Methode die etwas teilt`() {
        val methoden = CommandAuditLog::class.java.declaredMethods.map { it.name }
        val verboten = listOf("share", "upload", "send", "post", "sync", "publish")
        assertFalse(
            "Ein Verlauf darf keinen Empfänger benennen können.",
            methoden.any { it in verboten }
        )
    }

    @Test
    fun `der Export traegt keinen Empfaenger und keinen Endpunkt`() {
        val felder = AuditExport::class.java.declaredFields.map { it.name }
        assertTrue(
            "Ein Export darf keinen Empfänger benennen können.",
            felder.none { it in listOf("recipient", "endpoint", "target", "url") }
        )
    }

    // ── Exportieren und Löschen ───────────────────────────────────────

    @Test
    fun `der Export nennt jede protokollierte Aktion`() {
        val log = CommandAuditLog()
        log.record(record(kind = AuditActionKind.WRITE_FILE, purpose = "Datei geändert"))
        log.record(record(kind = AuditActionKind.GIT_PUSH, purpose = "Hochgeladen"))

        val export = log.export()
        assertEquals(2, export.recordCount)
        assertTrue(export.text.contains("Datei geschrieben"))
        assertTrue(export.text.contains("hochgeladen"))
    }

    @Test
    fun `ein leerer Verlauf exportiert ehrlich`() {
        val export = CommandAuditLog().export()
        assertTrue(export.isEmpty)
        assertTrue(export.text.contains("leer"))
    }

    @Test
    fun `loeschen sagt was entfernt wurde`() {
        val log = CommandAuditLog()
        log.record(record())
        val bericht = log.clear()

        assertEquals(1, bericht.removedCount)
        assertEquals(0, log.count)
        assertTrue(bericht.descriptionLines.first().contains("1 Einträge gelöscht"))
    }

    @Test
    fun `ein leerer Verlauf meldet ehrlich dass nichts zu loeschen war`() {
        val bericht = CommandAuditLog().clear()
        assertEquals(0, bericht.removedCount)
        assertTrue(
            "Nicht zu löschen ist nicht dasselbe wie etwas gelöscht zu haben.",
            bericht.descriptionLines.first().contains("nichts gelöscht")
        )
    }

    // ── Obergrenze ────────────────────────────────────────────────────

    @Test
    fun `die Obergrenze verwirft die aeltesten Eintraege`() {
        val log = CommandAuditLog(retentionLimit = 3)
        repeat(5) { i -> log.record(record(purpose = "Aktion $i")) }

        assertEquals(3, log.count)
        assertEquals(2, log.droppedCount)
        assertFalse(log.records.any { it.purpose.contains("Aktion 0") })
        assertTrue(log.records.any { it.purpose.contains("Aktion 4") })
    }

    @Test
    fun `der Verlauf meldet dass er gekuerzt wurde`() {
        val log = CommandAuditLog(retentionLimit = 1)
        log.record(record(purpose = "alt"))
        log.record(record(purpose = "neu"))

        assertTrue(log.isTruncated)
        assertTrue(log.explanationLines().any { it.contains("verworfen") })
        assertTrue(log.export().text.contains("verworfen"))
    }

    @Test
    fun `eine Obergrenze von null ist unzulaessig`() {
        try {
            CommandAuditLog(retentionLimit = 0)
            org.junit.Assert.fail("Ein Verlauf ohne Obergrenze würde unbegrenzt wachsen.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    // ── Nachvollziehbarkeit ───────────────────────────────────────────

    @Test
    fun `jeder Ausgang bleibt unterscheidbar`() {
        val log = CommandAuditLog()
        AuditOutcome.entries.forEach { ausgang ->
            log.record(record(outcome = ausgang))
        }
        assertEquals(AuditOutcome.entries.size, log.count)
        assertEquals(
            AuditOutcome.entries.size,
            log.records.map { it.outcome }.distinct().size
        )
    }

    @Test
    fun `abgebrochen ist nicht dasselbe wie erfolgreich`() {
        val log = CommandAuditLog()
        log.record(record(outcome = AuditOutcome.CANCELLED))
        log.record(record(outcome = AuditOutcome.SUCCEEDED))

        val zeilen = log.explanationLines()
        assertTrue(zeilen.any { it.contains("abgebrochen") })
        assertTrue(zeilen.any { it.contains("erfolgreich") })
    }

    @Test
    fun `ein ungewisser Ausgang wird als ungeklaert protokolliert`() {
        // Abbruch nach dem Schreiben: Der Nutzer muss sehen, dass es offen ist.
        val eintrag = record(kind = AuditActionKind.WRITE_FILE, outcome = AuditOutcome.UNCERTAIN)
        assertTrue(eintrag.summaryLine().contains("ungeklärt"))
    }

    @Test
    fun `die Freigabestufe steht im Verlauf`() {
        val eintrag = record(freigabe = AuditApproval("GIT_PUSH", "freigabe-42"))
        assertTrue(eintrag.summaryLine().contains("GIT_PUSH"))
        assertNotNull(eintrag.approval.approvalId)
    }
}