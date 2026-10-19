package org.claudroide.app

import org.claudroide.app.core.security.DataKind
import org.claudroide.app.core.security.DataRetentionPolicy
import org.claudroide.app.core.security.DataScope
import org.claudroide.app.core.security.DeletionTrigger
import org.claudroide.app.core.security.KeyRemoval
import org.claudroide.app.core.security.RetentionPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 127 — „Daten aufbewahren und löschen".
 *
 * Zwei Bedingungen tragen die Aufgabe: die Löschung muss einen Neustart
 * überleben und nachvollziehbar sein, und beim Anbieter gespeicherte Daten
 * dürfen nie als lokal gelöscht gelten. Der dritte Schutz — Schlüssel separat
 * und sofort entfernbar — hat seinen eigenen Abschnitt.
 */
class DataRetentionPolicyTest {

    private val jetzt = 1_700_000_000_000L

    // ── Aufbewahrung: nichts verschwindet ungefragt ──────────────────

    @Test
    fun `der Standard laeuft nicht ab`() {
        assertEquals(
            RetentionPeriod.UNTIL_MANUAL,
            DataRetentionPolicy.defaultRetention()
        )
        assertEquals(
            "Ohne gesetzte Frist gibt es keinen Ablaufzeitpunkt.",
            null,
            DataRetentionPolicy.expiryFor(RetentionPeriod.UNTIL_MANUAL, jetzt, jetzt)
        )
    }

    @Test
    fun `eine unbegrenzte Frist laeuft nie ab`() {
        assertFalse(DataRetentionPolicy.isExpired(RetentionPeriod.UNTIL_MANUAL, null, jetzt))
    }

    @Test
    fun `sieben Tage sind sieben Tage`() {
        val ende = DataRetentionPolicy.expiryFor(RetentionPeriod.SEVEN_DAYS, jetzt, jetzt)!!
        assertEquals(jetzt + 7L * 24 * 60 * 60 * 1000, ende)
    }

    @Test
    fun `die Frist wird bei jedem Aufruf neu gerechnet`() {
        val ende = DataRetentionPolicy.expiryFor(RetentionPeriod.THIRTY_DAYS, jetzt, jetzt)!!
        assertFalse(DataRetentionPolicy.isExpired(RetentionPeriod.THIRTY_DAYS, ende, jetzt))
        assertTrue(DataRetentionPolicy.isExpired(RetentionPeriod.THIRTY_DAYS, ende, ende))
    }

    @Test
    fun `eine rueckwaerts gelaufene Uhr erfindet keine Frist`() {
        // now < createdAt: die Uhr ist nicht belastbar. Eine Frist zu rechnen
        // hieße, eine Zeit zu behaupten, die so nie gemessen wurde.
        assertEquals(
            null,
            DataRetentionPolicy.expiryFor(RetentionPeriod.THIRTY_DAYS, jetzt + 10_000, jetzt)
        )
    }

    // ── Löschen überlebt den Neustart ─────────────────────────────────

    @Test
    fun `eine dauerhafte Loeschung gilt nach dem Neustart`() {
        val ergebnis = DataRetentionPolicy.delete(
            kind = DataKind.CHAT,
            removedItems = 12,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = false
        )
        assertTrue(ergebnis.isCompletelyDeleted)
        assertTrue(ergebnis.covers(DataScope.LOCAL))
    }

    @Test
    fun `eine fluechtige Loeschung wird nicht als vollstaendig gemeldet`() {
        val ergebnis = DataRetentionPolicy.delete(
            kind = DataKind.CHAT,
            removedItems = 3,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = false,
            providerHoldsCopy = false
        )
        assertFalse(
            "Eine Löschung ohne Neustart-Festigung ist nicht vollständig.",
            ergebnis.isCompletelyDeleted
        )
        assertTrue("Der Bericht muss es sagen.", ergebnis.report().contains("nicht dauerhaft"))
    }

    @Test
    fun `die Loeschung nennt Ort Zeitpunkt und Grund`() {
        val ergebnis = DataRetentionPolicy.delete(
            kind = DataKind.AUDIT_LOG,
            removedItems = 4,
            trigger = DeletionTrigger.RETENTION_EXPIRED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = false
        )
        val text = ergebnis.report()
        assertTrue(text.contains(DataKind.AUDIT_LOG.germanLabel))
        assertTrue(text.contains(DataScope.LOCAL.germanLabel))
        // Der Auslöser unterscheidet Wunsch von abgelaufener Frist.
        assertTrue(text.contains("Aufbewahrungsfrist"))
    }

    @Test
    fun `der Nutzerwunsch wird nicht als Fristablauf dargestellt`() {
        val ergebnis = DataRetentionPolicy.delete(
            kind = DataKind.CHAT,
            removedItems = 1,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = false
        )
        assertTrue(ergebnis.report().contains("Wunsch"))
    }

    // ── Der Anbieter ist ein fremder Ort ──────────────────────────────

    @Test
    fun `eine beim Anbieter gespeicherte Kopie wird nicht als geloescht gemeldet`() {
        val ergebnis = DataRetentionPolicy.delete(
            kind = DataKind.CHAT,
            removedItems = 20,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = true
        )
        assertFalse(
            "Lokal gelöscht heißt nicht überall gelöscht.",
            ergebnis.isCompletelyDeleted
        )
        assertFalse(ergebnis.covers(DataScope.PROVIDER_HELD))
        assertTrue(ergebnis.covers(DataScope.LOCAL))
    }

    @Test
    fun `der Bericht nennt den Anbieter im ersten Satz der Warnung`() {
        val ergebnis = DataRetentionPolicy.delete(
            kind = DataKind.CHAT,
            removedItems = 20,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = true
        )
        val text = ergebnis.report()
        assertTrue(text.contains("Achtung"))
        assertTrue(text.contains(DataScope.PROVIDER_HELD.germanLabel))
        assertTrue("Der Satz muss eingrenzen, wer löschen kann.", text.contains("nur der Anbieter"))
    }

    @Test
    fun `ohne Anbieterkopie wird keine Warnung erzeugt`() {
        val ergebnis = DataRetentionPolicy.delete(
            kind = DataKind.PROJECT_DATA,
            removedItems = 2,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = false
        )
        assertFalse(ergebnis.report().contains("Achtung"))
        assertTrue(ergebnis.isCompletelyDeleted)
    }

    @Test
    fun `eine unmoegliche Anzahl wird abgewiesen`() {
        var geworfen = false
        try {
            DataRetentionPolicy.delete(
                kind = DataKind.CHAT,
                removedItems = -1,
                trigger = DeletionTrigger.USER_REQUESTED,
                deletedAt = jetzt,
                survivesRestart = true,
                providerHoldsCopy = false
            )
        } catch (e: IllegalArgumentException) {
            geworfen = true
        }
        assertTrue("Eine negative Anzahl ist unbrauchbar.", geworfen)
    }

    // ── Nichts gelöscht wird auch so gesagt ──────────────────────────

    @Test
    fun `eine leere Loeschung meldet nicht etwa Erfolg`() {
        val leer = DataRetentionPolicy.delete(
            kind = DataKind.CHAT,
            removedItems = 0,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = false
        )
        assertFalse(
            "Nichts gelöscht ist nicht vollständig gelöscht.",
            leer.isCompletelyDeleted
        )
    }

    @Test
    fun `eine leere Datenart wird als bereits leer genannt`() {
        // Geprüft, aber nichts vorhanden: der Bericht darf das nicht
        // kommentarlos verschlucken — sonst sieht „nichts gelöscht" nach
        // Überspringen aus statt nach leerem Bestand.
        val leer = DataRetentionPolicy.delete(
            kind = DataKind.PROJECT_DATA,
            removedItems = 0,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = false
        )
        val text = leer.report()
        assertTrue(text.contains("nichts gelöscht"))
        assertTrue(text.contains(DataKind.PROJECT_DATA.germanLabel))
    }

    @Test
    fun `eine leere Datenart mit entfernter Schluesselmeldung bleibt ehrlich`() {
        val leer = DataRetentionPolicy.delete(
            kind = DataKind.CHAT,
            removedItems = 0,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = false,
            keysRemoved = 1
        )
        val text = leer.report()
        assertTrue("Der leere Datenbestand muss genannt werden.", text.contains("nichts gelöscht"))
        assertTrue("Der Schlüssel bleibt eine eigene Handlung.", text.contains("sofort entfernt"))
    }

    // ── Schlüssel separat und sofort entfernbar ──────────────────────

    @Test
    fun `ein vorhandener Schluessel wird entfernt`() {
        assertEquals(1, KeyRemoval.remove(present = true))
        assertTrue(KeyRemoval.report(1).contains("sofort"))
    }

    @Test
    fun `ein fehlender Schluessel wird nicht als entfernt gemeldet`() {
        // Die Falle: "1 Schlüssel entfernt" wäre eine falsche Angabe.
        assertEquals(0, KeyRemoval.remove(present = false))
        assertTrue(
            KeyRemoval.report(0).contains("kein Schlüssel")
        )
    }

    @Test
    fun `Schluessel erscheinen im Loeschbericht`() {
        val ergebnis = DataRetentionPolicy.delete(
            kind = DataKind.CHAT,
            removedItems = 1,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = false,
            keysRemoved = 1
        )
        assertTrue(ergebnis.report().contains("Schlüssel sofort entfernt"))
    }

    @Test
    fun `Schluessel und Chatverlauf sind getrennte Schritte`() {
        // Das Löschen des Verlaufs nimmt den Schlüssel nicht mit: er bleibt,
        // bis er ausdrücklich entfernt wird.
        val nurChat = DataRetentionPolicy.delete(
            kind = DataKind.CHAT,
            removedItems = 5,
            trigger = DeletionTrigger.USER_REQUESTED,
            deletedAt = jetzt,
            survivesRestart = true,
            providerHoldsCopy = false,
            keysRemoved = 0
        )
        assertEquals(0, nurChat.keysRemoved)
    }
}
