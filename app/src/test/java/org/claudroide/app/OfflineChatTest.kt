package org.claudroide.app

import org.claudroide.app.feature.chat.NetworkReachability
import org.claudroide.app.feature.chat.OfflineChatPolicy
import org.claudroide.app.feature.chat.OfflineChatState
import org.claudroide.app.feature.chat.ReconnectBehaviour
import org.claudroide.app.feature.chat.SendDecision
import org.claudroide.app.feature.provider.network.StreamState
import org.claudroide.app.feature.provider.network.StreamingResponse
import org.claudroide.app.feature.chat.UnsentRequest
import org.claudroide.app.feature.chat.UnsentRequestStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * Task 069 — Offline-Verhalten.
 *
 * Tests the acceptance criteria of tasks/069-offline-chat-state.md:
 *  - "Fertig, wenn": Nichts wird automatisch später gesendet, ohne das in Einstellungen
 *    festzulegen.
 *  - "Fertig, wenn": Nutzer ungesendete Anfrage ansehen, ändern oder löschen kann.
 *  - "Schutz": Keine wiederholte Anfrage oder Kosten nach Verbindungswiederkehr
 *    ohne Zustimmung.
 *  - Schutz: Eine unterbrochene Antwort wird nie als vollständig gespeichert.
 */
class OfflineChatTest {

    private fun readyRequest(id: String = "r-1", text: String = "Wie geht es?") =
        UnsentRequest(id = id, text = text, status = UnsentRequestStatus.READY)

    private fun draft(id: String = "d-1", text: String = "Wie ") =
        UnsentRequest(id = id, text = text, status = UnsentRequestStatus.DRAFT)

    // ---------- Kriterium: nichts wird automatisch später gesendet ----------

    @Test
    fun noSilentSendInvariant_isGuaranteed() {
        assertTrue(OfflineChatPolicy.NO_SILENT_SEND_INVARIANT)
    }

    @Test
    fun reconnectWithManualOnly_keepsEveryRequestLocal() {
        val state = OfflineChatState(
            requests = listOf(readyRequest()),
            reachability = NetworkReachability.ONLINE,
            reconnectBehaviour = ReconnectBehaviour.MANUAL_ONLY
        )
        val outcome = OfflineChatPolicy.onReachabilityChanged(state)

        // Nichts wird gesendet, alles bleibt lokal
        assertTrue(
            "Ohne Einstellung darf bei Netzrückkehr nichts gesendet werden",
            outcome.requestsToSendNow.isEmpty()
        )
        assertEquals(1, outcome.requestsKeptLocal.size)
        assertEquals(OfflineChatPolicy.MSG_MANUAL_ONLY, outcome.userMessage)
    }

    @Test
    fun reconnectWithExplicitOptIn_releasesOnlySendableRequests() {
        val state = OfflineChatState(
            requests = listOf(readyRequest("r-1"), draft("d-1", "Wie ")),
            reachability = NetworkReachability.ONLINE,
            reconnectBehaviour = ReconnectBehaviour.SEND_ELIGIBLE_AFTER_RECONNECT
        )
        val outcome = OfflineChatPolicy.onReachabilityChanged(state)

        // Nur die fertige, sendbare Anfrage wird freigegeben
        assertEquals(1, outcome.requestsToSendNow.size)
        assertEquals("r-1", outcome.requestsToSendNow.first().id)
        // Der unvollständige Entwurf bleibt lokal
        assertEquals(1, outcome.requestsKeptLocal.size)
        assertEquals(UnsentRequestStatus.DRAFT, outcome.requestsKeptLocal.first().status)
        assertEquals(OfflineChatPolicy.MSG_OPTED_IN, outcome.userMessage)
    }

    @Test
    fun sendRequiresExplicitPerRequestConfirmation() {
        val request = readyRequest()
        // Ohne Nutzerbestätigung wird nicht gesendet
        assertEquals(
            SendDecision.BLOCKED_WITHOUT_USER_CONFIRMATION,
            OfflineChatPolicy.decideSend(
                request,
                NetworkReachability.ONLINE,
                userConfirmed = false
            )
        )
        assertFalse(
            "Ohne Bestätigung des Nutzers darf nichts gesendet werden",
            OfflineChatPolicy.canSendNow(request, NetworkReachability.ONLINE, userConfirmed = false)
        )

        // Erst nach Bestätigung ist das Senden erlaubt
        assertEquals(
            SendDecision.ALLOWED,
            OfflineChatPolicy.decideSend(
                request,
                NetworkReachability.ONLINE,
                userConfirmed = true
            )
        )
        val sent = OfflineChatPolicy.confirmSend(request)
        assertEquals(1, sent.confirmedSendAttempts)
        // Jede weitere Bestätigung wird gezählt, nie stillschweigend ausgelöst
        assertEquals(2, OfflineChatPolicy.confirmSend(sent).confirmedSendAttempts)
    }

    @Test
    fun repeatedConfirmSend_neverTriggersOnItsOwn() {
        val request = readyRequest()
        // confirmSend zählt nur, es sendet nicht. Ohne Netz bleibt alles blockiert.
        val counted = OfflineChatPolicy.confirmSend(request)
        assertEquals(
            SendDecision.BLOCKED_NO_VERIFIED_NETWORK,
            OfflineChatPolicy.decideSend(counted, NetworkReachability.OFFLINE, userConfirmed = true)
        )
    }

    @Test
    fun unknownReachability_isNeverTreatedAsOnline() {
        // Ungeprüft heißt nicht online
        assertFalse(OfflineChatPolicy.isUsableNetwork(NetworkReachability.UNKNOWN))
        assertFalse(OfflineChatPolicy.isUsableNetwork(NetworkReachability.OFFLINE))
        assertTrue(OfflineChatPolicy.isUsableNetwork(NetworkReachability.ONLINE))

        // Ungeprüft bedeutet Offlinekennzeichnung
        assertTrue(OfflineChatPolicy.shouldShowOfflineMarker(NetworkReachability.UNKNOWN))
        assertTrue(OfflineChatPolicy.shouldShowOfflineMarker(NetworkReachability.OFFLINE))
        assertFalse(OfflineChatPolicy.shouldShowOfflineMarker(NetworkReachability.ONLINE))

        val request = readyRequest()
        assertEquals(
            SendDecision.BLOCKED_NO_VERIFIED_NETWORK,
            OfflineChatPolicy.decideSend(request, NetworkReachability.UNKNOWN, userConfirmed = true)
        )
    }

    @Test
    fun offlineRequest_isSavedLocallyAndMarked() {
        val state = OfflineChatState(
            requests = listOf(readyRequest()),
            reachability = NetworkReachability.OFFLINE
        )
        val outcome = OfflineChatPolicy.onReachabilityChanged(state)

        assertTrue(outcome.requestsToSendNow.isEmpty())
        assertEquals(1, outcome.requestsKeptLocal.size)
        assertEquals(
            "Ohne Netz wird die Anfrage als blockiert markiert",
            UnsentRequestStatus.BLOCKED_OFFLINE,
            outcome.requestsKeptLocal.first().status
        )
        assertTrue(OfflineChatPolicy.shouldShowOfflineMarker(state.reachability))
        assertEquals(OfflineChatPolicy.MSG_OFFLINE_READY, outcome.userMessage)
    }

    // ---------- Kriterium: Nutzer kann ansehen, ändern, löschen ----------

    @Test
    fun preservedRequest_canAlwaysBeViewed() {
        assertTrue(readyRequest().isViewable)
        assertTrue(draft().isViewable)
        assertTrue(
            OfflineChatPolicy.addDraft(OfflineChatState(), "d-9", "Text").requests.first().isViewable
        )
    }

    @Test
    fun preservedRequest_canBeEdited() {
        val edited = OfflineChatPolicy.editRequest(readyRequest(), "Neue Frage")
        assertEquals("Neue Frage", edited.text)
        assertTrue(edited.isEditable)

        // Wird der Text leer, ist es wieder ein Entwurf und nicht mehr sendbar
        val emptied = OfflineChatPolicy.editRequest(readyRequest(), "   ")
        assertEquals(UnsentRequestStatus.DRAFT, emptied.status)
        assertFalse(
            "Eine leere Eingabe darf nie gesendet werden",
            emptied.isSendable
        )
    }

    @Test
    fun preservedRequest_canBeDeleted() {
        val state = OfflineChatState(
            requests = listOf(readyRequest("r-1"), readyRequest("r-2")),
            reachability = NetworkReachability.OFFLINE
        )
        assertTrue(state.requests.first().isDeletable)

        val afterDelete = OfflineChatPolicy.deleteRequest(state, "r-1")
        assertEquals(1, afterDelete.requests.size)
        assertEquals("r-2", afterDelete.requests.first().id)
        // Das Löschen sendet nichts und entfernt nur lokal
        assertEquals(NetworkReachability.OFFLINE, afterDelete.reachability)
    }

    @Test
    fun draft_isNeverSendableUntilTheUserFinishesIt() {
        val d = draft("d-1", "Wie ")
        assertFalse("Ein unvollständiger Entwurf ist nicht sendbar", d.isSendable)
        assertEquals(
            SendDecision.BLOCKED_INCOMPLETE_TEXT,
            OfflineChatPolicy.decideSend(d, NetworkReachability.ONLINE, userConfirmed = true)
        )

        val finished = OfflineChatPolicy.editRequest(d, "Wie geht es dir?")
        val marked = OfflineChatPolicy.refreshStatus(finished, NetworkReachability.ONLINE)
        assertEquals(UnsentRequestStatus.READY, marked.status)
        assertTrue(marked.isSendable)
    }

    @Test
    fun blankTextRequest_isNeverSendableEvenWhenReady() {
        val blank = readyRequest("r-blank", "   ")
        assertFalse(blank.isSendable)
        assertEquals(
            SendDecision.BLOCKED_INCOMPLETE_TEXT,
            OfflineChatPolicy.decideSend(blank, NetworkReachability.ONLINE, userConfirmed = true)
        )
    }

    @Test
    fun blockedMessage_isPlainGermanAndNullWhenAllowed() {
        assertNull(OfflineChatPolicy.blockedMessage(SendDecision.ALLOWED))
        assertEquals(
            OfflineChatPolicy.MSG_OFFLINE_SAVED,
            OfflineChatPolicy.blockedMessage(SendDecision.BLOCKED_NO_VERIFIED_NETWORK)
        )
        assertEquals(
            OfflineChatPolicy.MSG_DRAFT_NOT_SENDABLE,
            OfflineChatPolicy.blockedMessage(SendDecision.BLOCKED_INCOMPLETE_TEXT)
        )
        assertEquals(
            OfflineChatPolicy.MSG_MANUAL_ONLY,
            OfflineChatPolicy.blockedMessage(SendDecision.BLOCKED_WITHOUT_USER_CONFIRMATION)
        )
    }

    // ---------- Schutz: unterbrochene Antwort ist nie vollständig ----------

    @Test
    fun abortedOrFailedAnswer_isNeverComplete() {
        val aborted = StreamingResponse(
            state = StreamState.ABORTED,
            text = "Die Antwort ist halb"
        )
        val failed = StreamingResponse(
            state = StreamState.FAILED,
            text = "Die Antwort ist halb"
        )
        assertFalse(
            "Eine abgebrochene Antwort darf nie als vollständig gelten",
            OfflineChatPolicy.partialAnswerIsComplete(aborted.state)
        )
        assertFalse(
            "Eine fehlgeschlagene Antwort darf nie als vollständig gelten",
            OfflineChatPolicy.partialAnswerIsComplete(failed.state)
        )
        assertFalse(aborted.isComplete)
        assertFalse(failed.isComplete)

        // Der Nutzer sieht den Hinweis auf die unvollständige Antwort
        assertTrue(OfflineChatPolicy.showsIncompleteNotice(aborted))
        assertTrue(OfflineChatPolicy.showsIncompleteNotice(failed))
        assertEquals(OfflineChatPolicy.MSG_PARTIAL_ANSWER, OfflineChatPolicy.incompleteAnswerHint(aborted))
    }

    @Test
    fun completedAnswer_showsNoIncompleteNotice() {
        val completed = StreamingResponse(
            state = StreamState.COMPLETED,
            text = "Fertige Antwort"
        )
        assertTrue(OfflineChatPolicy.partialAnswerIsComplete(completed.state))
        assertFalse(OfflineChatPolicy.showsIncompleteNotice(completed))
        assertNull(OfflineChatPolicy.incompleteAnswerHint(completed))
    }

    @Test
    fun emptyFailedAnswer_showsNoIncompleteNotice() {
        // Ohne Inhalt gibt es nichts Unvollständiges zu zeigen
        val nothing = StreamingResponse(state = StreamState.FAILED, text = "")
        assertFalse(nothing.hasContent)
        assertFalse(OfflineChatPolicy.showsIncompleteNotice(nothing))
        assertNull(OfflineChatPolicy.incompleteAnswerHint(nothing))
    }

    @Test
    fun storedChats_remainReadableOffline() {
        // Gespeicherte Chats bleiben lokal nutzbar, unabhängig vom Netz
        val state = OfflineChatState(
            requests = listOf(readyRequest("r-1")),
            reachability = NetworkReachability.OFFLINE
        )
        assertTrue(state.hasPreservedRequests)
        assertEquals(1, state.preservedRequests.size)
        assertTrue(state.preservedRequests.first().isViewable)
    }
}
