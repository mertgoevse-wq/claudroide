package org.claudroide.app

import org.claudroide.app.feature.git.ObjectId
import org.claudroide.app.feature.git.RetryDecision
import org.claudroide.app.feature.git.RetryGate
import org.claudroide.app.feature.git.RetryPlan
import org.claudroide.app.feature.git.TransferCertainty
import org.claudroide.app.feature.git.TransferProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Task 104 — "Git network failure".
 *
 * The two failures worth guarding are a retry that resends what the server
 * already accepted, and a retry that continues past an acknowledgement nobody
 * received.
 */
class GitTransferRecoveryTest {

    private fun id(n: String) = ObjectId(n)

    private val a = id("obj-a")
    private val b = id("obj-b")
    private val c = id("obj-c")

    private fun fortschritt(
        angenommen: Set<ObjectId> = setOf(a),
        unbestaetigt: Set<ObjectId> = emptySet(),
        offen: List<ObjectId> = listOf(b, c)
    ) = TransferProgress(angenommen, unbestaetigt, offen)

    // ── Fertig-Bedingung 1: kein doppelter Versand ──────────────────────────

    @Test
    fun `angenommene Objekte werden nicht erneut gesendet`() {
        val plan = RetryPlan.forProgress(fortschritt(angenommen = setOf(a)))
        assertFalse(plan.objectIdsToSend.contains(a))
        assertTrue(plan.excludedObjectIds.contains(a))
    }

    @Test
    fun `ein zweiter Versuch kann kein Objekt doppelt einbeziehen`() {
        val plan = RetryPlan.forProgress(fortschritt())
        val doppelt = plan.objectIdsToSend.toSet() intersect plan.excludedObjectIds
        assertEquals(emptySet<ObjectId>(), doppelt)
    }

    @Test
    fun `das Tor sendet nichts, was bereits angenommen wurde`() {
        val d = RetryGate.mayRetry(fortschritt(angenommen = setOf(a)), userConfirmedRetry = true)
        val plan = (d as RetryDecision.MayRetry).plan
        assertFalse(plan.objectIdsToSend.contains(a))
    }

    @Test
    fun `ein zweiter Versand derselben Lage aendert nichts am Ausschluss`() {
        val lage = fortschritt(angenommen = setOf(a))
        val erste = RetryPlan.forProgress(lage)
        val zweite = RetryPlan.forProgress(lage)
        assertEquals(erste.objectIdsToSend, zweite.objectIdsToSend)
        assertEquals(erste.excludedObjectIds, zweite.excludedObjectIds)
    }

    @Test
    fun `eine stabile Kennung ist vom Listenplatz unabhaengig`() {
        // Angenommen ist in beiden Laegen a; offen ist einmal kurz, einmal
        // lang. Ein Objekt, das schon angenommen wurde, faellt in keiner der
        // beiden Laegen aus dem Versuch heraus - unabhaengig davon, wie viele
        // andere Objekte daneben stehen.
        val kurz = fortschritt(angenommen = setOf(a), offen = listOf(b))
        val lang = fortschritt(angenommen = setOf(a), offen = listOf(b, c, id("obj-d")))
        val kurzPlan = RetryPlan.forProgress(kurz)
        val langPlan = RetryPlan.forProgress(lang)
        assertEquals(setOf(a), kurzPlan.excludedObjectIds)
        assertEquals(setOf(a), langPlan.excludedObjectIds)
        assertFalse(kurzPlan.objectIdsToSend.contains(a))
        assertFalse(langPlan.objectIdsToSend.contains(a))
    }

    @Test
    fun `ein Objekt kann nicht gesendet und ausgeschlossen zugleich sein`() {
        val plan = RetryPlan.forProgress(fortschritt())
        // Der Plan schliesst a aus und sendet b, c. Setzt man b auf die
        // Ausschlussliste, waere b zugleich in beiden Mengen.
        val widerspruch = runCatching { plan.copy(excludedObjectIds = setOf(b)) }
        assertTrue(widerspruch.isFailure)
    }

    // ── Fertig-Bedingung 2: der Nutzer sieht, was moeglicherweise ankam ─────

    @Test
    fun `unbestaetigte Objekte gelten als moeglicherweise angekommen`() {
        val p = fortschritt(angenommen = setOf(a), unbestaetigt = setOf(b))
        assertEquals(setOf(a, b), p.mayAlreadyHaveArrived)
    }

    @Test
    fun `unbestaetigte Objekte sind genau die ohne Quittung`() {
        val p = fortschritt(angenommen = setOf(a), unbestaetigt = setOf(b))
        assertEquals(setOf(b), p.unacknowledgedObjectIds)
    }

    @Test
    fun `ohne Unbestaetigung ist nichts moeglicherweise angekommen`() {
        val p = fortschritt(angenommen = setOf(a))
        assertEquals(setOf(a), p.mayAlreadyHaveArrived)
    }

    @Test
    fun `das Tor nennt, was beim letzten Mal ohne Quittung ging`() {
        val p = fortschritt(angenommen = setOf(a), unbestaetigt = setOf(b))
        val d = RetryGate.mayRetry(p, userConfirmedRetry = true)
        assertTrue(d is RetryDecision.NeedsConfirmation)
        assertEquals(setOf(b), (d as RetryDecision.NeedsConfirmation).uncertainIds)
    }

    @Test
    fun `die Meldung nennt die Anzahl der unbestatigten Objekte`() {
        val p = fortschritt(angenommen = setOf(a), unbestaetigt = setOf(b))
        val grund = (RetryGate.mayRetry(p, true) as RetryDecision.NeedsConfirmation).reason
        assertTrue(grund.contains("1"))
        assertTrue(grund.contains("ohne"))
    }

    // ── Der Schutz: unklare Zustaende gehen nicht still weiter ───────────────

    @Test
    fun `ein unbestatigtes Objekt macht den Zustand unklar`() {
        assertEquals(
            TransferCertainty.UNCERTAIN,
            fortschritt(unbestaetigt = setOf(b)).certainty
        )
    }

    @Test
    fun `ohne Unbestaetigung ist der Zustand abgeschlossen`() {
        assertEquals(TransferCertainty.SETTLED, fortschritt().certainty)
    }

    @Test
    fun `im unklaren Zustand wird nicht gesendet, auch mit Bestaetigung`() {
        val p = fortschritt(angenommen = setOf(a), unbestaetigt = setOf(b))
        val d = RetryGate.mayRetry(p, userConfirmedRetry = true)
        assertTrue(d is RetryDecision.NeedsConfirmation)
        assertFalse(d is RetryDecision.MayRetry)
    }

    @Test
    fun `die Unklarheit wird vor der Bestaetigung geprueft`() {
        // Beides fehlt: unbestaetigt UND nicht bestaetigt. Der Zustand muss
        // der gemeldete Grund sein, weil eine Bestaetigung ihn nicht klaert.
        val p = fortschritt(angenommen = setOf(a), unbestaetigt = setOf(b))
        val d = RetryGate.mayRetry(p, userConfirmedRetry = false)
        assertTrue(d is RetryDecision.NeedsConfirmation)
    }

    @Test
    fun `die Unklarheit enthaelt eine Menge, kein blosses Ja und Nein`() {
        val p = fortschritt(angenommen = setOf(a), unbestaetigt = setOf(b))
        val d = RetryGate.mayRetry(p, true) as RetryDecision.NeedsConfirmation
        assertTrue(d.uncertainIds.contains(b))
        assertFalse(d.uncertainIds.contains(a))
    }

    // ── Bestaetigung ────────────────────────────────────────────────────────

    @Test
    fun `ohne Bestaetigung wird nicht gesendet`() {
        val d = RetryGate.mayRetry(fortschritt(), userConfirmedRetry = false)
        assertTrue(d is RetryDecision.NothingToSend)
    }

    @Test
    fun `die Bestaetigung wird aus nichts abgeleitet`() {
        // Alle Zutaten eines freigegebenen Laufs sind vorhanden - nur die
        // Bestaetigung fehlt.
        val offen = fortschritt(angenommen = setOf(a), offen = listOf(b))
        assertTrue(RetryPlan.forProgress(offen).hasWork)
        assertTrue(RetryGate.mayRetry(offen, userConfirmedRetry = false) is RetryDecision.NothingToSend)
    }

    @Test
    fun `der Vorgabewert der Bestaetigung ist ablehnend`() {
        val d = RetryGate.mayRetry(fortschritt())
        assertTrue(d is RetryDecision.NothingToSend)
    }

    @Test
    fun `bestaetigt und abgeschlossen darf erneut gesendet werden`() {
        assertTrue(RetryGate.mayRetry(fortschritt(), userConfirmedRetry = true) is RetryDecision.MayRetry)
    }

    @Test
    fun `wenn alles angenommen ist, gibt es nichts zu senden`() {
        val p = fortschritt(angenommen = setOf(a), offen = emptyList())
        val d = RetryGate.mayRetry(p, userConfirmedRetry = true)
        assertTrue(d is RetryDecision.NothingToSend)
    }

    @Test
    fun `der Plan traegt die Kennungen, die schon angekommen sind`() {
        val d = RetryGate.mayRetry(fortschritt(angenommen = setOf(a)), true) as RetryDecision.MayRetry
        assertTrue(d.mayAlreadyHaveArrived.contains(a))
    }

    // ── Widerspruechliche Angaben ────────────────────────────────────────────

    @Test
    fun `ein Objekt kann nicht angenommen und unbestaetigt sein`() {
        val widerspruch = runCatching {
            TransferProgress(setOf(a), setOf(a), emptyList())
        }
        assertTrue(widerspruch.isFailure)
    }

    @Test
    fun `ein angenommenes Objekt steht nicht mehr in der Offen-Liste`() {
        val widerspruch = runCatching {
            TransferProgress(setOf(a), emptySet(), listOf(a))
        }
        assertTrue(widerspruch.isFailure)
    }

    @Test
    fun `die Offen-Liste enthaelt keine doppelten Kennungen`() {
        val doppelt = runCatching {
            TransferProgress(setOf(a), emptySet(), listOf(b, b))
        }
        assertTrue(doppelt.isFailure)
    }

    @Test
    fun `eine leere Objektkennung wird abgewiesen`() {
        assertTrue(runCatching { ObjectId(" ") }.isFailure)
    }

    @Test
    fun `eine Objektkennung mit Steuerzeichen wird abgewiesen`() {
        assertTrue(runCatching { ObjectId("obja") }.isFailure)
    }

    // ── Strukturell ──────────────────────────────────────────────────────────

    @Test
    fun `diese Datei fuehrt keine Uebertragung aus`() {
        val verboteneTypen = listOf(
            "java.net.URL", "java.net.Socket", "java.lang.ProcessBuilder",
            "java.io.OutputStream", "java.net.HttpURLConnection"
        )
        val typen = TransferProgress::class.java.declaredFields +
            RetryPlan::class.java.declaredFields +
            ObjectId::class.java.declaredFields +
            RetryGate::class.java.declaredFields
        typen.forEach { feld ->
            assertFalse(
                "Feld ${feld.name} vom Typ ${feld.type.name}",
                verboteneTypen.any { feld.type.name.contains(it) }
            )
        }
        RetryGate::class.java.methods.forEach { methode ->
            assertFalse(
                "Methode ${methode.name}",
                verboteneTypen.any { methode.returnType.name.contains(it) }
            )
        }
    }

    @Test
    fun `das Tor ist ein reines Objekt ohne Zustand zwischen Aufrufen`() {
        val javaClass = RetryGate::class.java
        assertTrue(Modifier.isFinal(javaClass.modifiers))
        assertEquals(
            0,
            javaClass.declaredFields.count { !Modifier.isStatic(it.modifiers) }
        )
    }

    @Test
    fun `jede Antwort traegt einen deutschen Namen`() {
        val d = RetryGate.mayRetry(fortschritt(), true)
        assertTrue(d.germanLabel.isNotBlank())
    }

    @Test
    fun `jede Uebertragung trägt den Namen ihrer Unsicherheit`() {
        TransferCertainty.values().forEach {
            assertTrue(it.germanLabel.isNotBlank())
        }
    }
}