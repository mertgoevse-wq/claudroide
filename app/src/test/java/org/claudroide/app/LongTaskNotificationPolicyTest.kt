package org.claudroide.app

import org.claudroide.app.core.platform.TaskLifecycleState
import org.claudroide.app.feature.agent.Certainty
import org.claudroide.app.feature.agent.LongTaskNotificationPolicy
import org.claudroide.app.feature.agent.LongTaskReport
import org.claudroide.app.feature.agent.NotificationGate
import org.claudroide.app.feature.agent.NotificationPlan
import org.claudroide.app.feature.agent.PlatformFacts
import org.claudroide.app.feature.agent.UserMandate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 113 — „Lange Aufgabe melden“.
 *
 * Der Kern der Aufgabe sind zwei Zusicherungen: nichts Privates auf dem
 * Sperrbildschirm und kein dauerhaftes Hintergrundarbeiten ohne Nutzerauftrag.
 * Beide stehen deshalb an erster Stelle.
 */
class LongTaskNotificationPolicyTest {

    private val start = 1_000_000L
    private val session = "sitzung-1"

    private fun facts(
        deviceSdk: Int = 35,
        notificationsAllowed: Boolean = true,
        requiresForegroundService: Boolean = true
    ) = PlatformFacts(
        targetSdk = PlatformFacts.APP_TARGET_SDK,
        deviceSdk = deviceSdk,
        notificationsAllowed = notificationsAllowed,
        requiresForegroundService = requiresForegroundService
    )

    private fun mandate(
        startedAt: Long = start,
        expiresAt: Long? = null,
        sessionId: String = session,
        withdrawn: Boolean = false
    ) = UserMandate(startedAt, expiresAt, sessionId, withdrawn)

    private fun report(
        state: TaskLifecycleState = TaskLifecycleState.RUNNING,
        elapsed: Long = 180_000L,
        certainty: Certainty = Certainty.PROVEN,
        safeLabel: String = "Agentenlauf",
        privateDetail: String? = null
    ) = LongTaskReport(state, elapsed, certainty, safeLabel, privateDetail)

    private fun policy(threshold: Long = 120_000L) = LongTaskNotificationPolicy(threshold)

    // ── Keine privaten Inhalte auf dem Sperrbildschirm ────────────────

    @Test
    fun `der Sperrbildschirm nennt nur die Art der Aufgabe`() {
        val plan = policy().decide(
            report(privateDetail = "Refactor von ChatScreen.kt: Zeile 42 geändert"),
            mandate(),
            facts(),
            start + 200_000L,
            session
        )
        assertNotNull(plan)
        assertEquals("Agentenlauf: läuft", plan!!.lockScreenText)
        assertFalse(
            "Privater Inhalt darf nicht auf dem Sperrbildschirm stehen.",
            plan.lockScreenText.contains("ChatScreen")
        )
    }

    @Test
    fun `privater Inhalt erscheint nur auf dem entsperrten Bildschirm`() {
        val plan = policy().decide(
            report(privateDetail = "Ändert ChatScreen.kt"),
            mandate(),
            facts(),
            start + 200_000L,
            session
        )
        assertNotNull(plan)
        assertTrue("Auf dem entsperrten Bildschirm darf die Zeile stehen.", plan!!.detailText.contains("Ändert ChatScreen.kt"))
        assertFalse(plan.lockScreenText.contains("ChatScreen.kt"))
    }

    @Test
    fun `die Benachrichtigung traegt nie den Schalter fuer privaten Inhalt an`() {
        val plan = policy().decide(report(), mandate(), facts(), start + 200_000L, session)
        assertNotNull(plan)
        assertFalse("Es gibt keinen Weg, privaten Inhalt anzuschalten.", plan!!.mayIncludePrivateContent)
    }

    @Test
    fun `ein zustandsunabhaengiger Sperrbildschirm-Text kann keinen Inhalt hereinbringen`() {
        // Auch wenn jemand eine Detailzeile mit einem Geheimnis übergibt, bleibt
        // der Sperrbildschirm-Text auf Art + Zustand.
        val plan = policy().decide(
            report(privateDetail = "sk-ant-api03-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),
            mandate(),
            facts(),
            start + 200_000L,
            session
        )
        assertNotNull(plan)
        assertEquals("Agentenlauf: läuft", plan!!.lockScreenText)
        assertFalse(plan.lockScreenText.contains("sk-ant"))
    }

    // ── Kein Hintergrundarbeiten ohne aktiven Nutzerauftrag ───────────

    @Test
    fun `ohne Nutzerauftrag entsteht keine Benachrichtigung`() {
        val gate = policy().gate(report(), mandate(), facts(), start + 200_000L, "andere-sitzung")
        assertTrue(gate is NotificationGate.NoActiveMandate)
        assertNull(
            "Ohne Auftrag darf keine Meldung entstehen — das wäre Hintergrundausführung.",
            policy().build(gate, report())
        )
    }

    @Test
    fun `ein zurueckgenommener Auftrag erzeugt keine Benachrichtigung`() {
        val gate = policy().gate(report(), mandate(withdrawn = true), facts(), start + 200_000L, session)
        assertTrue(gate is NotificationGate.NoActiveMandate)
    }

    @Test
    fun `ein abgelaufener Auftrag erzeugt keine Benachrichtigung`() {
        val ablauf = start + 100_000L
        val gate = policy().gate(report(), mandate(expiresAt = ablauf), facts(), start + 150_000L, session)
        assertTrue("Nach dem Ablauf darf nichts mehr gemeldet werden.", gate is NotificationGate.NoActiveMandate)
    }

    @Test
    fun `eine rueckwaerts gelaufene Uhr gilt als abgelaufen`() {
        // „Jetzt liegt vor dem Beginn" — die Uhr ist nicht belastbar.
        val gate = policy().gate(report(), mandate(startedAt = start), facts(), start - 60_000L, session)
        assertTrue(gate is NotificationGate.NoActiveMandate)
    }

    @Test
    fun `ein aktiver Auftrag erzeugt eine Benachrichtigung`() {
        val plan = policy().decide(report(), mandate(), facts(), start + 200_000L, session)
        assertNotNull("Ein aktiver Auftrag über der Schwelle wird gemeldet.", plan)
    }

    // ── Zielversion, Berechtigung, Diensttyp werden geprüft ──────────

    @Test
    fun `ohne Benachrichtigungsberechtigung ab Android 13 wird nichts behauptet`() {
        val gate = policy().gate(
            report(),
            mandate(),
            facts(deviceSdk = 33, notificationsAllowed = false),
            start + 200_000L,
            session
        )
        assertTrue(gate is NotificationGate.PermissionMissing)
        assertNull(policy().build(gate, report()))
    }

    @Test
    fun `vor Android 13 wird keine Berechtigung verlangt`() {
        // Unter API 33 braucht die App keine ausdrückliche Benachrichtigungserlaubnis.
        val plan = policy().decide(
            report(),
            mandate(),
            facts(deviceSdk = 30, notificationsAllowed = false),
            start + 200_000L,
            session
        )
        assertNotNull("Unter Android 13 ist die fehlende Erlaubnis kein Grund.", plan)
    }

    @Test
    fun `die Zielversion der App wird als Faktenwert mitgefuehrt`() {
        val f = facts()
        assertEquals("Die App zielt auf Android 15.", 35, f.targetSdk)
        assertEquals(35, PlatformFacts.APP_TARGET_SDK)
        assertEquals(26, PlatformFacts.APP_MIN_SDK)
    }

    @Test
    fun `ein Vordergrunddienst wird als Anforderung weitergegeben`() {
        val plan = policy().decide(report(), mandate(), facts(requiresForegroundService = true), start + 200_000L, session)
        assertNotNull(plan)
        assertTrue("Wenn Android einen Vordergrunddienst verlangt, steht das in der Meldung.", plan!!.requiresForegroundService)
    }

    @Test
    fun `eine kurze Aufgabe wird nicht gemeldet`() {
        val gate = policy(threshold = 120_000L).gate(report(elapsed = 5_000L), mandate(), facts(), start + 10_000L, session)
        assertTrue(gate is NotificationGate.NotLongEnough)
    }

    @Test
    fun `die Reihenfolge ist erst Berechtigung dann Auftrag`() {
        // Fehlt beides, muss die **Berechtigung** genannt werden — sie ist die
        // erste Frage, weil ohne sie ohnehin nichts angezeigt würde.
        val gate = policy().gate(
            report(),
            mandate(sessionId = "fremd"),
            facts(deviceSdk = 33, notificationsAllowed = false),
            start + 200_000L,
            session
        )
        assertTrue(
            "Ohne Erlaubnis ist die Auftragsfrage zweitrangig.",
            gate is NotificationGate.PermissionMissing
        )
    }

    // ── Stop-Aktion ───────────────────────────────────────────────────

    @Test
    fun `eine laufende lange Aufgabe ist stoppbar`() {
        val plan = policy().decide(report(state = TaskLifecycleState.RUNNING), mandate(), facts(), start + 200_000L, session)
        assertTrue(plan!!.showStopAction)
    }

    @Test
    fun `eine beendete Aufgabe zeigt keine Stop-Aktion`() {
        val plan = policy().decide(report(state = TaskLifecycleState.COMPLETED), mandate(), facts(), start + 200_000L, session)
        assertFalse("Eine beendete Aufgabe lässt sich nicht mehr stoppen.", plan!!.showStopAction)
    }

    @Test
    fun `jede laufende oder pausierte Aufgabe hat eine Stop-Aktion`() {
        listOf(TaskLifecycleState.RUNNING, TaskLifecycleState.PAUSED).forEach { zustand ->
            val plan = policy().decide(report(state = zustand), mandate(), facts(), start + 200_000L, session)
            assertTrue("$zustand muss stoppbar sein.", plan!!.showStopAction)
        }
    }

    // ── Ehrlichkeit bei Unsicherheit ──────────────────────────────────

    @Test
    fun `ein unsicheres Ergebnis wird als unsicher gemeldet`() {
        val plan = policy().decide(report(certainty = Certainty.UNCERTAIN), mandate(), facts(), start + 200_000L, session)
        assertTrue(plan!!.uncertaintyNote.contains("nicht gesichert"))
        assertEquals(Certainty.UNCERTAIN, plan.certainty)
    }

    @Test
    fun `eine langsame Aufgabe wird als langsam erklaert`() {
        val plan = policy().decide(report(certainty = Certainty.TOO_SLOW), mandate(), facts(), start + 200_000L, session)
        assertTrue(plan!!.uncertaintyNote.contains("langsam"))
    }

    @Test
    fun `ein belegter Zustand braucht keine Zusatzzeile`() {
        val plan = policy().decide(report(certainty = Certainty.PROVEN), mandate(), facts(), start + 200_000L, session)
        assertEquals("Nur ein belegter Zustand bleibt ohne Zusatzzeile.", "", plan!!.uncertaintyNote)
    }

    // ── build() lässt sich nicht umgehen ──────────────────────────────

    @Test
    fun `build kann ohne Allowed keine Benachrichtigung erzeugen`() {
        val verboten = NotificationGate.NoActiveMandate(listOf("kein Auftrag"))
        assertNull(policy().build(verboten, report()))
    }

    @Test
    fun `der private Weg existiert nicht auf dem Plan`() {
        // Der Plan darf keinen privaten Inhalt tragen — die Zusicherung hängt
        // am Feld, nicht an der Anweisung.
        val felder = NotificationPlan::class.java.declaredFields.map { it.name }
        assertFalse("Es gibt kein Feld für privaten Inhalt.", felder.any { it.contains("privateContent") })
    }

    // ── Zustandsdarstellung ───────────────────────────────────────────

    @Test
    fun `jeder Zustand hat eine eigene Beschreibung`() {
        val beschreibungen = TaskLifecycleState.entries.map { zustand ->
            policy().decide(report(state = zustand), mandate(), facts(), start + 200_000L, session)!!
                .statusLine
        }
        assertEquals("Jeder Zustand ist unterscheidbar.", TaskLifecycleState.entries.size, beschreibungen.distinct().size)
    }

    @Test
    fun `die Laufzeit wird lesbar gemacht`() {
        val plan = policy().decide(report(elapsed = 125_000L), mandate(), facts(), start + 200_000L, session)
        assertTrue(plan!!.detailText.contains("2 min"))
    }
}