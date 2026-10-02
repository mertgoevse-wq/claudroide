package org.claudroide.app

import org.claudroide.app.core.security.ApprovalEvent
import org.claudroide.app.core.security.ApprovalEventKind
import org.claudroide.app.core.security.ApprovalHistory
import org.claudroide.app.core.security.HistoryExport
import org.claudroide.app.core.security.PermissionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 118 — „Freigabeverlauf“.
 *
 * Das Unterscheidungsproblem steht an erster Stelle: ein Nutzer, der
 * „widerrufen“ sucht, darf keine Umfangsänderung finden — und umgekehrt.
 */
class ApprovalHistoryTest {

    private fun event(
        atMillis: Long = 1_700_000_000_000L,
        projectId: String = "projekt-1",
        projectName: String = "Projekt",
        category: PermissionCategory = PermissionCategory.FILES,
        target: String = "/storage/Projekt",
        kind: ApprovalEventKind = ApprovalEventKind.GRANTED,
        scopeLabel: String = "Lesen & Schreiben",
        reason: String? = null
    ) = ApprovalEvent.create(
        atMillis, projectId, projectName, category, target, kind, scopeLabel, reason
    )

    // ── Widerruf ist klar von einer Änderung unterschieden ────────────

    @Test
    fun `ein Widerruf ist eine Entziehung`() {
        val e = event(kind = ApprovalEventKind.REVOKED, reason = "Nutzer hat es zurückgenommen")
        assertTrue(e.isWithdrawal)
        assertTrue(e.summaryLine().contains("widerrufen"))
        assertTrue(e.summaryLine().contains("Nutzer hat es zurückgenommen"))
    }

    @Test
    fun `eine Umfandsaenderung ist keine Entziehung`() {
        val e = event(kind = ApprovalEventKind.SCOPE_CHANGED, scopeLabel = "Nur Lesen")
        assertFalse(
            "Eine Einschränkung darf nicht als Widerruf erscheinen.",
            e.isWithdrawal
        )
    }

    @Test
    fun `die Erstfreigabe ist keine Entziehung`() {
        assertFalse(event(kind = ApprovalEventKind.GRANTED).isWithdrawal)
    }

    @Test
    fun `ein Ablauf ist eine Entziehung aber keine aktive Handlung`() {
        val e = event(kind = ApprovalEventKind.EXPIRED)
        assertTrue(e.isWithdrawal)
        assertFalse(
            "Ein Ablauf darf nicht als Widerruf missverstanden werden.",
            e.summaryLine().contains("widerrufen")
        )
    }

    @Test
    fun `die Entziehungen eines Verlaufs sind von den Aenderungen getrennt`() {
        val verlauf = ApprovalHistory()
        verlauf.record(event(kind = ApprovalEventKind.GRANTED))
        verlauf.record(event(kind = ApprovalEventKind.SCOPE_CHANGED))
        verlauf.record(event(kind = ApprovalEventKind.REVOKED, reason = "zurückgenommen"))
        verlauf.record(event(kind = ApprovalEventKind.EXPIRED))

        val entzogen = verlauf.withdrawals()
        assertEquals(2, entzogen.size)
        assertTrue(entzogen.all { it.isWithdrawal })
        assertFalse(entzogen.any { it.kind == ApprovalEventKind.SCOPE_CHANGED })
    }

    @Test
    fun `die Entziehungen eines Projekts sind gefiltert`() {
        val verlauf = ApprovalHistory()
        verlauf.record(event(projectId = "projekt-1", kind = ApprovalEventKind.REVOKED, reason = "a"))
        verlauf.record(event(projectId = "projekt-2", kind = ApprovalEventKind.REVOKED, reason = "b"))

        assertEquals(1, verlauf.withdrawalsFor("projekt-1").size)
        assertEquals(1, verlauf.withdrawalsFor("projekt-2").size)
        assertEquals(
            "Ein fremdes Projekt hat keine Entziehungen in diesem Verlauf.",
            0,
            verlauf.withdrawalsFor("projekt-3").size
        )
    }

    @Test
    fun `die erklaerung stellt die Entziehungen voran`() {
        val verlauf = ApprovalHistory()
        verlauf.record(event(kind = ApprovalEventKind.GRANTED))
        verlauf.record(event(kind = ApprovalEventKind.REVOKED, reason = "zurückgenommen"))

        val zeilen = verlauf.explanationLines()
        val entziehungsZeile = zeilen.indexOfFirst { it.contains("widerrufen oder abgelaufen") }
        assertTrue("Die Entziehungen stehen voran.", entziehungsZeile == 0)
    }

    // ── Keine unnötigen Inhalte ───────────────────────────────────────

    @Test
    fun `das Ereignis traegt kein Feld fuer Chat- oder Dateiinhalt`() {
        val felder = ApprovalEvent::class.java.declaredFields.map { it.name }
        val verboten = listOf("content", "chatText", "message", "fileContent", "body", "text", "output")
        assertFalse(
            "Der Verlauf darf keinen Inhalt speichern — dafür darf es kein Feld geben.",
            felder.any { it in verboten }
        )
    }

    @Test
    fun `ein Schluessel im Grund wird geschwaerzt`() {
        val e = event(kind = ApprovalEventKind.REVOKED, reason = "Key sk-ant-api03-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA weggenommen")
        assertFalse(e.reason!!.contains("sk-ant"))
        assertTrue(e.redacted)
    }

    @Test
    fun `ein Schluessel im Ziel wird geschwaerzt`() {
        val e = event(target = "/data/keys -----BEGIN RSA PRIVATE KEY-----X-----END RSA PRIVATE KEY-----")
        assertFalse(e.target.contains("BEGIN RSA PRIVATE KEY"))
    }

    @Test
    fun `ohne Geheimnis wird nichts als geschwaerzt gemeldet`() {
        assertFalse(event().redacted)
    }

    @Test
    fun `der Widerruf loescht die urspruengliche Erteilung nicht`() {
        // Der Verlauf ist ein Ereignisstrom: der Widerruf kommt **hinzu**.
        val verlauf = ApprovalHistory()
        verlauf.record(event(kind = ApprovalEventKind.GRANTED))
        verlauf.record(event(kind = ApprovalEventKind.REVOKED, reason = "zurückgenommen"))

        assertEquals(2, verlauf.count)
        assertTrue(
            "Die ursprüngliche Erteilung bleibt nachvollziehbar.",
            verlauf.events.any { it.kind == ApprovalEventKind.GRANTED }
        )
    }

    // ── Nicht als öffentliches Protokoll ──────────────────────────────

    @Test
    fun `der Verlauf hat keine Methode die etwas uebertraegt`() {
        val methoden = ApprovalHistory::class.java.declaredMethods.map { it.name }
        val verboten = listOf("share", "upload", "send", "post", "sync", "publish", "export")
        // `export` erzeugt einen Wert, überträgt aber nichts — es ist erlaubt.
        val echteUebertragung = methoden.filter { it in listOf("share", "upload", "send", "post", "sync", "publish") }
        assertTrue("Der Verlauf darf nichts selbst übertragen.", echteUebertragung.isEmpty())
    }

    @Test
    fun `der Export traegt keinen Empfaenger und keinen Endpunkt`() {
        val felder = HistoryExport::class.java.declaredFields.map { it.name }
        assertTrue(felder.none { it in listOf("recipient", "endpoint", "target", "url") })
    }

    // ── Export und Löschen ────────────────────────────────────────────

    @Test
    fun `der Export nennt Projekt Aktion und Zeitpunkt im Text`() {
        val verlauf = ApprovalHistory()
        verlauf.record(event(kind = ApprovalEventKind.REVOKED, reason = "zurückgenommen"))
        val export = verlauf.export()

        assertEquals(1, export.eventCount)
        assertTrue(export.text.contains("widerrufen"))
        assertTrue(export.text.contains("/storage/Projekt"))
        assertTrue(export.text.contains("zurückgenommen"))
    }

    @Test
    fun `ein leerer Verlauf exportiert ehrlich`() {
        assertTrue(ApprovalHistory().export().isEmpty)
    }

    @Test
    fun `loeschen sagt was entfernt wurde`() {
        val verlauf = ApprovalHistory()
        verlauf.record(event())
        verlauf.record(event())

        val bericht = verlauf.clear()
        assertEquals(2, bericht.removedCount)
        assertEquals(0, verlauf.count)
        assertTrue(bericht.descriptionLines.first().contains("2 Ereignisse gelöscht"))
    }

    @Test
    fun `ein leerer Verlauf meldet dass nichts zu loeschen war`() {
        assertTrue(ApprovalHistory().clear().descriptionLines.first().contains("nichts gelöscht"))
    }

    // ── Obergrenze ────────────────────────────────────────────────────

    @Test
    fun `die Obergrenze verwirft die aeltesten Ereignisse`() {
        val verlauf = ApprovalHistory(retentionLimit = 2)
        repeat(4) { i -> verlauf.record(event(atMillis = 1_700_000_000_000L + i)) }
        assertEquals(2, verlauf.count)
        assertEquals(2, verlauf.droppedCount)
        assertTrue(verlauf.isTruncated)
    }

    @Test
    fun `die Kuerzung wird gemeldet`() {
        val verlauf = ApprovalHistory(retentionLimit = 1)
        verlauf.record(event(atMillis = 1L))
        verlauf.record(event(atMillis = 2L))

        assertTrue(verlauf.explanationLines().any { it.contains("verworfen") })
        assertTrue(verlauf.export().text.contains("verworfen"))
    }

    @Test
    fun `eine Obergrenze von null ist unzulaessig`() {
        try {
            ApprovalHistory(retentionLimit = 0)
            org.junit.Assert.fail("Ein Verlauf ohne Obergrenze würde unbegrenzt wachsen.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }

    // ── Verständlichkeit ──────────────────────────────────────────────

    @Test
    fun `jede Ereignisart ist unterscheidbar`() {
        val zeilen = ApprovalEventKind.entries.map { art ->
            event(kind = art, reason = "Grund").summaryLine().substringBefore(" ·")
        }
        assertEquals("Jede Ereignisart ist unterscheidbar.", ApprovalEventKind.entries.size, zeilen.distinct().size)
    }

    @Test
    fun `jede Kategorie wird benannt`() {
        val zeilen = PermissionCategory.entries.map { kategorie ->
            event(category = kategorie).summaryLine()
        }
        assertEquals(PermissionCategory.entries.size, zeilen.size)
        assertTrue(zeilen.any { it.contains("Dateien & Speicher") })
    }
}