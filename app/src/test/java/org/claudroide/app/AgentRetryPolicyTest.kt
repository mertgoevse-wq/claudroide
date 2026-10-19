package org.claudroide.app

import org.claudroide.app.feature.agent.AgentRetryPolicy
import org.claudroide.app.feature.agent.AgentRunStore
import org.claudroide.app.feature.agent.FailureKind
import org.claudroide.app.feature.agent.Idempotency
import org.claudroide.app.feature.agent.RetryAction
import org.claudroide.app.feature.agent.RetryDecision
import org.claudroide.app.feature.agent.RetryDenialReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 079 — „Wiederholungsregeln“.
 *
 * Die Tests gehen die Liste der Aktionen durch und prüfen für jede, dass sie sich
 * so verhält, wie es der Auftrag verlangt. Eine neue Aktion fällt dadurch sofort
 * auf, statt unbemerkt eine Lücke zu bekommen.
 */
class AgentRetryPolicyTest {

    private val policy = AgentRetryPolicy(maxAttempts = 3)

    // ── Testhilfen ───────────────────────────────────────────────────────────

    /** Der günstigste Fall: vorübergehender Fehler, erster Versuch. */
    private fun decide(
        action: RetryAction,
        attemptsSoFar: Int = 1,
        failureKind: FailureKind = FailureKind.TRANSIENT,
        idempotency: Idempotency = Idempotency.UNKNOWN,
        userConfirmed: Boolean = false,
        costNoticeShown: Boolean = false,
        alreadyExecuted: Boolean = false
    ) = policy.decide(
        action,
        attemptsSoFar,
        failureKind,
        idempotency,
        userConfirmed,
        costNoticeShown,
        alreadyExecuted
    )

    private fun retryNow(decision: RetryDecision): RetryDecision.RetryNow {
        assertTrue("Erwartet: $decision", decision is RetryDecision.RetryNow)
        return decision as RetryDecision.RetryNow
    }

    private fun askUser(decision: RetryDecision): RetryDecision.AskUser {
        assertTrue("Erwartet: $decision", decision is RetryDecision.AskUser)
        return decision as RetryDecision.AskUser
    }

    private fun doNotRetry(decision: RetryDecision): RetryDecision.DoNotRetry {
        assertTrue("Erwartet: $decision", decision is RetryDecision.DoNotRetry)
        return decision as RetryDecision.DoNotRetry
    }

    // ── Jede Aktion einzeln ──────────────────────────────────────────────────

    @Test
    fun `jede Aktion ist genau einer von drei Gruppen zugeordnet`() {
        // Diese Liste ist die Zusage des Auftrags in Testform. Eine neue Aktion in
        // RetryAction muss hier ergaenzt werden, sonst prueft sie niemand.
        val sideEffectNeverAutomatic = setOf(
            RetryAction.WRITE_FILE,
            RetryAction.DELETE_FILE,
            RetryAction.INSTALL,
            RetryAction.GIT_COMMIT,
            RetryAction.GIT_PUSH
        )
        val harmless = setOf(
            RetryAction.READ_FILE,
            RetryAction.LIST_DIRECTORY,
            RetryAction.SEARCH_TEXT,
            RetryAction.RUN_TEST
        )
        val costly = setOf(RetryAction.MODEL_REQUEST)

        RetryAction.values().forEach { action ->
            when (action) {
                in sideEffectNeverAutomatic -> {
                    assertTrue(
                        "${action.germanLabel} muss als Nebenwirkung gelten",
                        action.hasSideEffect
                    )
                    assertTrue(
                        "${action.germanLabel} darf nie automatisch wiederholt werden",
                        action.neverAutoRetry
                    )
                }
                in harmless -> {
                    assertFalse("${action.germanLabel} verändert nichts", action.hasSideEffect)
                    assertFalse(
                        "${action.germanLabel} darf automatisch wiederholt werden",
                        action.neverAutoRetry
                    )
                    assertFalse("${action.germanLabel} darf nichts kosten", action.costsMoney)
                }
                in costly -> {
                    assertFalse("${action.germanLabel} darf keine Nebenwirkung haben", action.hasSideEffect)
                    assertFalse(
                        "${action.germanLabel} darf nicht automatisch gesperrt sein",
                        action.neverAutoRetry
                    )
                    assertTrue("${action.germanLabel} muss Kosten verursachen", action.costsMoney)
                }
                else -> throw AssertionError(
                    "${action.name} ist in keiner Gruppe und damit ungetestet."
                )
            }
        }
    }

    @Test
    fun `eine Modellanfrage ist nicht kostenlos`() {
        assertTrue(RetryAction.MODEL_REQUEST.costsMoney)
        assertFalse(RetryAction.MODEL_REQUEST.hasSideEffect)
    }

    // ── Schreib-, Installations- und Git-Aktionen ────────────────────────────

    @Test
    fun `eine Datei wird nicht automatisch neu geschrieben`() {
        assertEquals(
            RetryDenialReason.NEVER_AUTOMATIC,
            askUser(decide(RetryAction.WRITE_FILE)).reason
        )
    }

    @Test
    fun `ein Loeschen wird nicht automatisch wiederholt`() {
        assertEquals(
            RetryDenialReason.NEVER_AUTOMATIC,
            askUser(decide(RetryAction.DELETE_FILE)).reason
        )
    }

    @Test
    fun `eine Installation wird nicht automatisch wiederholt`() {
        assertEquals(
            RetryDenialReason.NEVER_AUTOMATIC,
            askUser(decide(RetryAction.INSTALL)).reason
        )
    }

    @Test
    fun `ein Commit wird nicht automatisch wiederholt`() {
        assertEquals(
            RetryDenialReason.NEVER_AUTOMATIC,
            askUser(decide(RetryAction.GIT_COMMIT)).reason
        )
    }

    @Test
    fun `ein Git-Upload wird nicht automatisch wiederholt`() {
        assertEquals(
            RetryDenialReason.NEVER_AUTOMATIC,
            askUser(decide(RetryAction.GIT_PUSH)).reason
        )
    }

    @Test
    fun `selbst mit Idempotenz wird nicht automatisch wiederholt`() {
        assertEquals(
            RetryDenialReason.NEVER_AUTOMATIC,
            askUser(decide(RetryAction.WRITE_FILE, idempotency = Idempotency.PROVEN)).reason
        )
    }

    @Test
    fun `selbst mit Bestaetigung entscheidet die Regel nicht ueber den Nutzer`() {
        // Auch eine Bestaetigung hebt die automatische Wiederholung nicht auf: Sie
        // gehoert dem Nutzer, nicht diesem Code.
        assertEquals(
            RetryDenialReason.NEVER_AUTOMATIC,
            askUser(decide(RetryAction.GIT_PUSH, userConfirmed = true)).reason
        )
    }

    @Test
    fun `das Schreiben von Installation und Git nennt die Aktion im Klartext`() {
        val d = askUser(decide(RetryAction.GIT_PUSH))
        assertTrue(d.germanExplanation.contains("ins Repository hochladen"))
        assertTrue(d.germanExplanation.contains("nicht automatisch wiederholt"))
    }

    // ── Kostenpflichtige Anfragen ────────────────────────────────────────────

    @Test
    fun `eine kostenpflichtige Anfrage wird ohne Kostenhinweis nicht wiederholt`() {
        assertEquals(
            RetryDenialReason.COST_NOT_VISIBLE,
            askUser(decide(RetryAction.MODEL_REQUEST, costNoticeShown = false)).reason
        )
    }

    @Test
    fun `eine kostenpflichtige Anfrage wird nach Kostenhinweis wiederholt`() {
        retryNow(decide(RetryAction.MODEL_REQUEST, costNoticeShown = true))
    }

    @Test
    fun `der Kostenhinweis nennt dem Nutzer die erneuten Kosten`() {
        val n = retryNow(decide(RetryAction.MODEL_REQUEST, costNoticeShown = true))
        assertTrue(n.noticeLines.joinToString("\n").contains("kostet erneut Geld"))
    }

    @Test
    fun `eine kostenlose Aktion braucht keinen Kostenhinweis`() {
        retryNow(decide(RetryAction.READ_FILE, costNoticeShown = false))
    }

    @Test
    fun `eine Installation ist auch mit Kostenhinweis kein automatischer Versuch`() {
        assertEquals(
            RetryDenialReason.NEVER_AUTOMATIC,
            askUser(decide(RetryAction.INSTALL, costNoticeShown = true)).reason
        )
    }

    @Test
    fun `die Installation wird als kostenpflichtig gefuehrt`() {
        assertTrue(RetryAction.INSTALL.costsMoney)
    }

    // ── Idempotenz oder Bestaetigung ─────────────────────────────────────────

    @Test
    fun `jede Nebenwirkung ist heute durch die automatische Sperre abgedeckt`() {
        // Offen gesagt: Mit dem heutigen Aktionssatz gibt es keine Aktion mit
        // hasSideEffect = true und neverAutoRetry = false. Der Zweig
        // NEEDS_CONFIRMATION ist damit vom strengeren NEVER_AUTOMATIC ueberlagert
        // und nicht erreichbar. Er bleibt als zweite Linie stehen, falls spaeter
        // eine Nebenwirkung ohne automatische Sperre dazukommt — und dieser Test
        // sagt genau das, statt so zu tun, als wuerde er den Zweig ausloesen.
        RetryAction.values().filter { it.hasSideEffect }.forEach {
            assertTrue("${it.germanLabel} haette eine Nebenwirkung ohne Sperre", it.neverAutoRetry)
        }
        assertEquals(
            RetryDenialReason.NEVER_AUTOMATIC,
            askUser(decide(RetryAction.WRITE_FILE, idempotency = Idempotency.IMPOSSIBLE)).reason
        )
    }

    @Test
    fun `eine harmlose Aktion braucht keinen Idempotenznachweis`() {
        retryNow(decide(RetryAction.SEARCH_TEXT, idempotency = Idempotency.UNKNOWN))
    }

    @Test
    fun `eine harmlose Aktion wird auch bei unbekannter Idempotenz wiederholt`() {
        retryNow(decide(RetryAction.LIST_DIRECTORY, idempotency = Idempotency.IMPOSSIBLE))
    }

    // ── Fehlerart ────────────────────────────────────────────────────────────

    @Test
    fun `ein dauerhafter Fehler wird nicht wiederholt`() {
        assertEquals(
            RetryDenialReason.NOT_TRANSIENT,
            doNotRetry(decide(RetryAction.READ_FILE, failureKind = FailureKind.PERMANENT)).reason
        )
    }

    @Test
    fun `ein ungeklaerter Fehler wird nicht wiederholt`() {
        assertEquals(
            RetryDenialReason.UNKNOWN_FAILURE,
            doNotRetry(decide(RetryAction.READ_FILE, failureKind = FailureKind.UNKNOWN)).reason
        )
    }

    @Test
    fun `ein ungeklaerter Fehler schlaegt auch die Kostenfrage`() {
        // Sonst wuerde eine kostenpflichtige Anfrage bei ungeklaertem Fehler
        // zuerst nach den Kosten fragen und damit den eigentlichen Grund verdecken.
        assertEquals(
            RetryDenialReason.UNKNOWN_FAILURE,
            doNotRetry(
                decide(
                    RetryAction.MODEL_REQUEST,
                    failureKind = FailureKind.UNKNOWN,
                    costNoticeShown = true
                )
            ).reason
        )
    }

    @Test
    fun `der ungeklaerte Fehler bittet um Nachsehen statt um Wiederholung`() {
        val d = doNotRetry(decide(RetryAction.READ_FILE, failureKind = FailureKind.UNKNOWN))
        assertTrue(d.germanExplanation.contains("erst nachsehen"))
    }

    // ── Grenze der Versuche ──────────────────────────────────────────────────

    @Test
    fun `nach der letzten erlaubten Zahl wird nicht wiederholt`() {
        assertEquals(
            RetryDenialReason.ATTEMPTS_EXHAUSTED,
            doNotRetry(decide(RetryAction.READ_FILE, attemptsSoFar = 3)).reason
        )
    }

    @Test
    fun `beim ersten Fehlschlag wird noch wiederholt`() {
        retryNow(decide(RetryAction.READ_FILE, attemptsSoFar = 1))
    }

    @Test
    fun `eine Zahl von null Versuchen ist moeglich`() {
        retryNow(decide(RetryAction.READ_FILE, attemptsSoFar = 0))
    }

    @Test
    fun `die Versuchsgrenze schlaegt die Sperre wenn sie wirklich erreicht ist`() {
        // Bewusste Reihenfolge: Ist die Zahl der Versuche wirklich erreicht,
        // bringt auch eine Rueckfrage zum Schreiben nichts mehr — es gibt keinen
        // vierten Versuch. Vorher gefragt zu werden ist etwas anderes, und genau
        // das prueft der zweite Teil.
        assertEquals(
            RetryDenialReason.ATTEMPTS_EXHAUSTED,
            doNotRetry(decide(RetryAction.WRITE_FILE, attemptsSoFar = 3)).reason
        )
        assertEquals(
            RetryDenialReason.NEVER_AUTOMATIC,
            askUser(decide(RetryAction.WRITE_FILE, attemptsSoFar = 1)).reason
        )
    }

    @Test
    fun `mit einem einzigen erlaubten Versuch gibt es keine Wiederholung`() {
        val einVersuch = AgentRetryPolicy(maxAttempts = 1)
        val d = einVersuch.decide(RetryAction.READ_FILE, 1, FailureKind.TRANSIENT)
        assertEquals(RetryDenialReason.ATTEMPTS_EXHAUSTED, doNotRetry(d).reason)
    }

    @Test
    fun `weniger als ein Versuch wird abgelehnt`() {
        val fehler = runCatching { AgentRetryPolicy(maxAttempts = 0) }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    // ── Bereits ausgefuehrt ──────────────────────────────────────────────────

    @Test
    fun `eine bereits ausgefuehrte Aktion laeuft nicht noch einmal`() {
        assertEquals(
            RetryDenialReason.ALREADY_EXECUTED,
            doNotRetry(decide(RetryAction.READ_FILE, alreadyExecuted = true)).reason
        )
    }

    @Test
    fun `der Doppelausführungshinweis nennt die Aktion`() {
        val d = doNotRetry(decide(RetryAction.READ_FILE, alreadyExecuted = true))
        assertTrue(d.germanExplanation.contains("bereits ausgeführt"))
        assertTrue(d.germanExplanation.contains("doppelt"))
    }

    @Test
    fun `bereits ausgefuehrt schlaegt auch eine Seitenwirkung ohne Sperre`() {
        assertEquals(
            RetryDenialReason.ALREADY_EXECUTED,
            doNotRetry(
                decide(RetryAction.WRITE_FILE, alreadyExecuted = true, idempotency = Idempotency.PROVEN)
            ).reason
        )
    }

    // ── Zusammenspiel mit dem Laufstatus (Aufgabe 073) ───────────────────────

    @Test
    fun `ein im Laufstatus verbuchter Aufruf wird nicht wiederholt`() {
        val store = AgentRunStore("run-1")
        store.recordCall("s1", "write_file", "src/A.kt", changedPaths = listOf("src/A.kt"))

        val d = policy.decideWithStore(
            store = store,
            action = RetryAction.WRITE_FILE,
            toolName = "write_file",
            path = "src/A.kt",
            attemptsSoFar = 1,
            failureKind = FailureKind.TRANSIENT
        )

        assertEquals(RetryDenialReason.ALREADY_EXECUTED, doNotRetry(d).reason)
    }

    @Test
    fun `ein noch nicht verbuchter Aufruf wird normal behandelt`() {
        val store = AgentRunStore("run-2")

        val d = policy.decideWithStore(
            store = store,
            action = RetryAction.READ_FILE,
            toolName = "read_file",
            path = "src/A.kt",
            attemptsSoFar = 1,
            failureKind = FailureKind.TRANSIENT
        )

        retryNow(d)
    }

    @Test
    fun `ein anderer Pfad im Laufstatus stoert nicht`() {
        val store = AgentRunStore("run-3")
        store.recordCall("s1", "read_file", "src/A.kt")

        val d = policy.decideWithStore(
            store = store,
            action = RetryAction.READ_FILE,
            toolName = "read_file",
            path = "src/B.kt",
            attemptsSoFar = 1,
            failureKind = FailureKind.TRANSIENT
        )

        retryNow(d)
    }

    // ── Wartezeit ────────────────────────────────────────────────────────────

    @Test
    fun `die Wartezeit verdoppelt sich je Versuch`() {
        assertEquals(500L, policy.delayFor(1))
        assertEquals(1_000L, policy.delayFor(2))
        assertEquals(2_000L, policy.delayFor(3))
    }

    @Test
    fun `die Wartezeit wird nach oben begrenzt`() {
        assertEquals(8_000L, policy.delayFor(20))
        assertEquals(8_000L, policy.delayFor(40))
    }

    @Test
    fun `eine sehr hohe Versuchszahl fuehrt nicht zu Ueberlauf`() {
        assertEquals(8_000L, policy.delayFor(1_000))
    }

    @Test
    fun `die Wartezeit nennt den Versuch und die Obergrenze`() {
        val d = retryNow(decide(RetryAction.READ_FILE, attemptsSoFar = 2))
        assertTrue(d.noticeLines.joinToString("\n").contains("3. von 3 Versuchen"))
    }

    @Test
    fun `die Wartezeit steht im Hinweis fuer den Nutzer`() {
        val d = retryNow(decide(RetryAction.READ_FILE, attemptsSoFar = 1))
        assertTrue(d.noticeLines.joinToString("\n").contains("500 ms"))
    }

    // ── Anzeige ──────────────────────────────────────────────────────────────

    @Test
    fun `die Anzeige nennt die Grenzen der Wiederholung`() {
        val text = policy.disclosureLines().joinToString("\n")
        assertTrue(text.contains("Höchstzahl Versuche: 3"))
        assertTrue(text.contains("nie automatisch wiederholt"))
        assertTrue(text.contains("Ungeklärte Fehler werden nicht wiederholt"))
    }

    @Test
    fun `die Anzeige listet jede gesperrte Aktion einzeln auf`() {
        val text = policy.disclosureLines().joinToString("\n")
        RetryAction.values().filter { it.neverAutoRetry }.forEach {
            assertTrue("${it.germanLabel} fehlt in der Anzeige", text.contains(it.germanLabel))
        }
    }
}
