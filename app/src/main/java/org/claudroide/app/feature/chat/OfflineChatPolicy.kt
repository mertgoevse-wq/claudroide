package org.claudroide.app.feature.chat

/**
 * Task 069 — Offline-Verhalten.
 *
 * Acceptance criteria under test (tasks/069-offline-chat-state.md):
 *  - "Fertig, wenn": Nichts wird automatisch später gesendet, ohne das in Einstellungen
 *    festzulegen.
 *  - "Fertig, wenn": Nutzer ungesendete Anfrage ansehen, ändern oder löschen kann.
 *  - "Schutz": Keine wiederholte Anfrage oder Kosten nach Verbindungswiederkehr
 *    ohne Zustimmung.
 *
 * The rule that shapes this whole file: an unsent request stays local. Nothing in here
 * opens a socket, schedules a timer, or knows anything about a provider SDK. Every
 * function is pure and synchronous so the safety rules can be proven on the JVM.
 *
 * Connectivity is *injected*, never guessed. [NetworkReachability.UNKNOWN] is a real
 * state, not a convenience default: when nobody has verified the network, this policy
 * treats it as not-usable, because a wrong "online" would mean sending without consent.
 */

/** Verified network state, supplied by the caller from a real connectivity signal. */
enum class NetworkReachability {
    /** Nobody checked. Never treated as online. */
    UNKNOWN,

    /** A connectivity check reported no usable network. */
    OFFLINE,

    /** A connectivity check reported a usable network. */
    ONLINE
}

/**
 * What the user explicitly allowed to happen when the network comes back.
 *
 * [MANUAL_ONLY] is the default and the only value this build ships as safe. Sending
 * without a fresh, per-request tap would be a silent transmission.
 */
enum class ReconnectBehaviour {
    /** Unsent requests stay local until the user taps send again. */
    MANUAL_ONLY,

    /** The user opted in in settings: eligible requests may be sent after a reconnect. */
    SEND_ELIGIBLE_AFTER_RECONNECT
}

/** Lifecycle of one preserved, not-yet-sent user request. */
enum class UnsentRequestStatus {
    /** User is still typing; text may be incomplete and must not be sent. */
    DRAFT,

    /** Text is finished but not sent, because no usable network was verified. */
    BLOCKED_OFFLINE,

    /** Text is finished and a send is possible right now, but the user has not confirmed. */
    READY
}

/**
 * One user request that has not left the device.
 *
 * A preserved request is local data only. [text] may be a half-typed draft; that is why
 * [isSendable] is false for [UnsentRequestStatus.DRAFT].
 */
data class UnsentRequest(
    val id: String,
    val text: String,
    val status: UnsentRequestStatus = UnsentRequestStatus.DRAFT,
    val createdAtEpochMillis: Long = 0L,
    /** How many send attempts the user has explicitly confirmed so far. */
    val confirmedSendAttempts: Int = 0
) {
    /**
     * True when this request may be handed to the transport.
     *
     * A draft with blank text is never sendable: a half-written message must never
     * reach a provider in a broken state.
     */
    val isSendable: Boolean
        get() = status != UnsentRequestStatus.DRAFT && text.isNotBlank()

    /** The user may always look at a preserved request. */
    val isViewable: Boolean get() = true

    /** The user may always edit a preserved request as long as text remains. */
    val isEditable: Boolean get() = text.isNotEmpty() || status == UnsentRequestStatus.DRAFT

    /** The user may always delete a preserved request. */
    val isDeletable: Boolean get() = true
}

/** The local set of preserved requests plus the user's reconnect setting. */
data class OfflineChatState(
    val requests: List<UnsentRequest> = emptyList(),
    val reachability: NetworkReachability = NetworkReachability.UNKNOWN,
    val reconnectBehaviour: ReconnectBehaviour = ReconnectBehaviour.MANUAL_ONLY
) {
    /** Every preserved request is still local; none of them has been transmitted. */
    val preservedRequests: List<UnsentRequest> get() = requests

    val hasPreservedRequests: Boolean get() = requests.isNotEmpty()
}

/** Result of asking whether one request may be sent. */
enum class SendDecision {
    /** Send it now. */
    ALLOWED,

    /** Refused: nobody verified a usable network. */
    BLOCKED_NO_VERIFIED_NETWORK,

    /** Refused: this is still a draft with no finished text. */
    BLOCKED_INCOMPLETE_TEXT,

    /** Refused: the user has not answered the confirmation prompt for this request. */
    BLOCKED_WITHOUT_USER_CONFIRMATION
}

/** Outcome of handling a network change, in plain German for the UI. */
data class ReconnectOutcome(
    val requestsToSendNow: List<UnsentRequest>,
    val requestsKeptLocal: List<UnsentRequest>,
    val userMessage: String
)

/**
 * Pure decision logic for offline behaviour.
 *
 * There is deliberately no "send later" queue anywhere in this object. The only way a
 * request leaves the device is [confirmSend], which the UI calls from an explicit user
 * action.
 */
object OfflineChatPolicy {

    /**
     * Architectural safety invariant: no unsent request is ever transmitted without an
     * explicit, per-request user confirmation.
     */
    const val NO_SILENT_SEND_INVARIANT: Boolean = true

    /** Plain German texts shown to the user. No marketing language, no invented numbers. */
    const val MSG_OFFLINE_SAVED: String =
        "Kein Internet. Deine Eingabe bleibt auf dem Gerät und wird nicht gesendet."
    const val MSG_OFFLINE_READY: String =
        "Deine Eingabe ist gespeichert. Du kannst sie ansehen, ändern oder löschen."
    const val MSG_DRAFT_NOT_SENDABLE: String =
        "Die Eingabe ist noch unvollständig. Sie wird erst sendbar, wenn du sie selbst freigibst."
    const val MSG_MANUAL_ONLY: String =
        "Das Internet ist wieder da. Deine gespeicherten Eingaben werden nicht von selbst gesendet."
    const val MSG_OPTED_IN: String =
        "Das Internet ist wieder da. Die freigegebenen Eingaben werden jetzt gesendet."
    const val MSG_PARTIAL_ANSWER: String =
        "Die Antwort ist unvollständig. Sie wurde abgebrochen oder ist fehlgeschlagen."

    /**
     * True only when a usable network was actually verified.
     * [NetworkReachability.UNKNOWN] is not online.
     */
    fun isUsableNetwork(reachability: NetworkReachability): Boolean =
        reachability == NetworkReachability.ONLINE

    /** True when the app must show an offline marker. Unknown counts as offline. */
    fun shouldShowOfflineMarker(reachability: NetworkReachability): Boolean =
        !isUsableNetwork(reachability)

    /**
     * Marks a preserved request with the status that matches the verified network state.
     * A draft stays a draft; a finished text is only "ready" once a network is verified.
     */
    fun refreshStatus(request: UnsentRequest, reachability: NetworkReachability): UnsentRequest {
        val newStatus = when {
            request.status == UnsentRequestStatus.DRAFT -> UnsentRequestStatus.DRAFT
            isUsableNetwork(reachability) -> UnsentRequestStatus.READY
            else -> UnsentRequestStatus.BLOCKED_OFFLINE
        }
        return request.copy(status = newStatus)
    }

    /**
     * Decides whether one request may be sent right now.
     *
     * @param userConfirmed true only when the user just tapped send for this exact request.
     */
    fun decideSend(
        request: UnsentRequest,
        reachability: NetworkReachability,
        userConfirmed: Boolean
    ): SendDecision = when {
        !isUsableNetwork(reachability) -> SendDecision.BLOCKED_NO_VERIFIED_NETWORK
        !request.isSendable -> SendDecision.BLOCKED_INCOMPLETE_TEXT
        !userConfirmed -> SendDecision.BLOCKED_WITHOUT_USER_CONFIRMATION
        else -> SendDecision.ALLOWED
    }

    /** True when [decideSend] returns [SendDecision.ALLOWED]. */
    fun canSendNow(
        request: UnsentRequest,
        reachability: NetworkReachability,
        userConfirmed: Boolean
    ): Boolean = decideSend(request, reachability, userConfirmed) == SendDecision.ALLOWED

    /**
     * Records a send the user explicitly confirmed.
     * Increments the attempt counter so a repeated send is visible, never implicit.
     */
    fun confirmSend(request: UnsentRequest): UnsentRequest =
        request.copy(confirmedSendAttempts = request.confirmedSendAttempts + 1)

    /**
     * Handles a connectivity change.
     *
     * With [ReconnectBehaviour.MANUAL_ONLY] — the default — nothing is returned for
     * sending. Only requests whose behaviour was set to
     * [ReconnectBehaviour.SEND_ELIGIBLE_AFTER_RECONNECT] in settings, and that are
     * sendable, are released. This function does not send anything itself; the caller
     * acts on the result.
     */
    fun onReachabilityChanged(state: OfflineChatState): ReconnectOutcome {
        val refreshed = state.requests.map { refreshStatus(it, state.reachability) }
        val optedIn = state.reconnectBehaviour == ReconnectBehaviour.SEND_ELIGIBLE_AFTER_RECONNECT
        val (released, kept) = if (optedIn) {
            refreshed.partition { it.isSendable }
        } else {
            emptyList<UnsentRequest>() to refreshed
        }
        val message = when {
            !isUsableNetwork(state.reachability) -> MSG_OFFLINE_READY
            optedIn -> MSG_OPTED_IN
            else -> MSG_MANUAL_ONLY
        }
        return ReconnectOutcome(
            requestsToSendNow = released,
            requestsKeptLocal = kept,
            userMessage = message
        )
    }

    /**
     * How a preserved partial answer must be presented.
     *
     * An aborted or failed answer is never complete, so it can never be stored or shown
     * as a finished message.
     */
    fun partialAnswerIsComplete(streamState: StreamState): Boolean =
        streamState == StreamState.COMPLETED

    /** True when the preserved answer must be shown as "unvollständig". */
    fun showsIncompleteNotice(response: StreamingResponse): Boolean =
        !partialAnswerIsComplete(response.state) &&
            (response.isIncomplete || response.hasContent)

    /** Plain German hint for an interrupted answer. */
    fun incompleteAnswerHint(response: StreamingResponse): String? =
        if (showsIncompleteNotice(response)) MSG_PARTIAL_ANSWER else null

    /** Lets the user change a preserved request. Blank text turns it back into a draft. */
    /**
     * Replaces the text of a preserved request.
     *
     * A finished text must leave the DRAFT state, otherwise a half-typed request could
     * never be sent even after the user completed it. Blanking the text puts it back
     * into DRAFT, which is exactly the "unfinished" meaning of that state.
     */
    fun editRequest(request: UnsentRequest, newText: String): UnsentRequest =
        request.copy(
            text = newText,
            status = if (newText.isBlank()) {
                UnsentRequestStatus.DRAFT
            } else {
                UnsentRequestStatus.READY
            }
        )

    /** Lets the user delete a preserved request. Nothing is sent on deletion. */
    fun deleteRequest(state: OfflineChatState, requestId: String): OfflineChatState =
        state.copy(requests = state.requests.filterNot { it.id == requestId })

    /** Adds a new, unfinished draft. It is not sendable until the user finishes it. */
    fun addDraft(state: OfflineChatState, requestId: String, text: String): OfflineChatState =
        state.copy(
            requests = state.requests +
                UnsentRequest(id = requestId, text = text, status = UnsentRequestStatus.DRAFT)
        )

    /** Plain German text for why a send was refused. */
    fun blockedMessage(decision: SendDecision): String? = when (decision) {
        SendDecision.ALLOWED -> null
        SendDecision.BLOCKED_NO_VERIFIED_NETWORK -> MSG_OFFLINE_SAVED
        SendDecision.BLOCKED_INCOMPLETE_TEXT -> MSG_DRAFT_NOT_SENDABLE
        SendDecision.BLOCKED_WITHOUT_USER_CONFIRMATION -> MSG_MANUAL_ONLY
    }
}
