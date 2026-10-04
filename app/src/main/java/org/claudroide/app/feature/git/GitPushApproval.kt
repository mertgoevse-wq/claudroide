package org.claudroide.app.feature.git

/**
 * Task 101 — "approve the git upload" (Git-Upload freigeben).
 *
 * Goal: the user sees repository target and change list before an upload.
 * Result: a confirmation view with target address, branch, commit, files and a
 * way out.
 *
 * ## What this file is, and what it is not
 *
 * This is a **scaffold**, and the distinction is load-bearing rather than a
 * hedge. The task's first completion condition — "the upload goes to a private,
 * existing target" — can only be settled by a real push from the device, and the
 * device is not reachable (`adb devices` reports nothing; see
 * `progress/BUILD-STATE.md`). So that condition is recorded here as
 * [PushGate.mayPush] deciding it, and as [DeviceUploadEvidence] recording that
 * nobody has yet observed it. The gate is provable. The upload is not, and this
 * file does not pretend otherwise: it opens no socket, spawns no process and
 * writes no file. A caller that treats `MayPush` as "the upload happened" has
 * misread it, and [DeviceUploadEvidence] is what stops that confusion from being
 * silent.
 *
 * ## The two completion conditions as properties
 *
 *  1. *"The upload goes to a private, existing target."*
 *     [GitPushGate.mayPush] refuses unless the audit says the target was found
 *     on the server **and** its visibility was verified on the device. Intent
 *     never substitutes for measurement — [RemoteRepositoryAudit] carries both
 *     facts and [PushGate] reads them before anything else. What is missing is
 *     not modelled: there is no [PushOutcome.PUSHED], because a run of this
 *     project has never produced one.
 *
 *  2. *"Errors and partial transfer are clearly recognisable."*
 *     [PushState.outcome] is one of four values, and three of them are not
 *     success. A transfer that sent objects and received no acknowledgement is
 *     [PushOutcome.PARTIAL] — not [PushOutcome.COMPLETE], and not an error
 *     either, because nobody knows yet. [PushState.failed] is a separate
 *     constructor so an error can never be confused with an unsettled state.
 *
 * ## The protection: no upload without explicit release
 *
 * [GitPushGate.mayPush] takes `userReleasedUpload` with default `false`. It is
 * derived from nothing: not from a complete file list, not from a verified
 * private target, not from the existence of a commit. Two tests hold that — one
 * asserts a fully prepared request is still refused, the other asserts a call
 * with the argument left out sends nothing. The secret check from task 100 is
 * read here as [GitPushPlan.secretScanReport] and, like in 099, a release does
 * not lift it.
 *
 * ## Language
 *
 * English identifiers, German for the user. [PushDecision.germanLabel] is the
 * label the interface shows; `label` carries the English wording. This follows
 * the project rule rather than the file's neighbours, which are German from
 * task 096 onwards.
 *
 * Pure Kotlin: no network, no filesystem, no process, no Android.
 */

// ── The request ────────────────────────────────────────────────────────────

/**
 * What the user is about to send, and what the check found about the target.
 *
 * [audit] is a **measured** report, not a claim: whoever builds it must have
 * read it from the server on the device. [GitPushGate] treats a `true` here as
 * a measurement, which is why the field cannot be defaulted — see
 * [RemoteRepositoryAudit.visibilityVerifiedOnDevice], which has the same shape
 * for the same reason.
 */
data class GitPushRequest(
    val repository: RemoteRepository,
    val audit: RemoteRepositoryAudit,
    val branch: String,
    /** The commit message, so the user sees what the commit says, not just its id. */
    val message: String,
    /** Exactly the paths that travel. One entry per file — no bundles, no globs. */
    val filePaths: List<String>
) {
    init {
        require(branch.isNotBlank()) { "Ein Upload braucht einen Zweig." }
        require(message.isNotBlank()) { "Ein Upload braucht eine Nachricht." }
        require(filePaths.none { it.isBlank() }) { "Ein leerer Pfad ist kein Pfad." }
        require(filePaths.distinct().size == filePaths.size) {
            "Dieselbe Datei darf im Upload nicht zweimal stehen."
        }
        require(filePaths.none { it.contains("..") }) {
            "Ein Pfad darf nicht aus dem Projekt ausbrechen: ${filePaths.first { it.contains("..") }}"
        }
    }
}

/**
 * A prepared upload: the request, the commit it sends, and what the secret scan
 * found.
 *
 * The commit id is **text**, not an [ObjectId]: an id is a transport identifier
 * minted by whoever uploads, and this file never mints one. Holding it as a
 * plain string keeps the promise honest — there is no code here that could
 * invent a commit.
 */
data class GitPushPlan(
    val request: GitPushRequest,
    /** The commit the user is about to publish, as git reported it. */
    val commitId: String,
    /** Transport objects, as the uploader listed them. */
    val objectIds: List<ObjectId>,
    /** The finished scan from task 100. Read by [GitPushGate], never derived here. */
    val secretScanReport: SecretScanReport
) {
    init {
        require(commitId.isNotBlank()) { "Ein Upload braucht einen Commit." }
        require(commitId.none { it.isISOControl() }) {
            "Eine Commitkennung kann kein Steuerzeichen enthalten."
        }
    }

    /**
     * The confirmation view: target, branch, commit, and every single file.
     *
     * The order is fixed — destination, then what goes there, then what is in
     * it. A view that led with the file count would answer "how much" before
     * "where", and "where" is the question that decides whether the upload is
     * safe at all.
     */
    fun confirmationLines(): List<String> = buildList {
        add("Ziel: ${request.repository.displayName()} (${request.repository.httpsCloneUrl()})")
        add("Sichtbarkeit: ${request.repository.visibility.germanLabel}")
        add("Zweig: ${request.branch}")
        add("Commit: ${commitId.take(SHORT_COMMIT_LENGTH)} — ${request.message}")
        add("Dateien: ${request.filePaths.size}")
        request.filePaths.forEach { add("  $it") }

        val sperrend = secretScanReport.blockingFindings
        if (sperrend.isNotEmpty()) {
            add("Geheimnisverdacht in ${sperrend.size} Fundstelle(n):")
            sperrend.forEach { add("  ${it.displayLine()}") }
        }
        add("Abbrechen ist jederzeit möglich. Ohne ausdrückliche Freigabe wird nichts gesendet.")
    }

    /** How many objects travel. Shown, because a partial transfer is about count. */
    val objectCount: Int get() = objectIds.size

    companion object {
        /** The length git itself shows for a commit. Longer invites false precision. */
        const val SHORT_COMMIT_LENGTH = 7
    }
}

// ── The gate ───────────────────────────────────────────────────────────────

/** The answer to "may this upload leave the device?". */
sealed interface PushDecision {
    val germanLabel: String

    /** Everything checked. **The sending is outside this file.** */
    data class MayPush(val plan: GitPushPlan) : PushDecision {
        override val germanLabel: String get() = "darf gesendet werden"
    }

    /** Nothing is sent. The reason names the target and what failed. */
    data class Refused(val reason: String) : PushDecision {
        override val germanLabel: String get() = "nicht gesendet"
    }

    /**
     * The user backed out.
     *
     * Its own answer, not the absence of a release. "Abgebrochen" and "noch nicht
     * freigegeben" look identical in a boolean and mean different things to the
     * person who pressed the button, so the decision carries the difference
     * instead of collapsing it into a flag.
     */
    data class Cancelled(val reason: String) : PushDecision {
        override val germanLabel: String get() = "abgebrochen"
    }
}

/**
 * The rule for when an upload may start.
 *
 * Checked in a fixed order, strictest first: the target's existence, then its
 * measured privacy, then the files, then the secret scan, and last the
 * release. The release is last because it is the only input the user can change
 * by saying yes — and saying yes must not carry the earlier checks with it.
 */
object GitPushGate {

    /**
     * May [plan] be uploaded?
     *
     * @param userReleasedUpload an **own** input, default `false`, derived from
     *        nothing. Not from a complete file list, not from a verified target,
     *        not from the commit existing.
     */
    fun mayPush(plan: GitPushPlan, userReleasedUpload: Boolean = false): PushDecision {
        val request = plan.request

        // Everything about the target is read from the **audit**, never from
        // `request.repository`. The request says where the user *intends* to
        // upload; the audit says what the server *reported*. Reading the intent
        // here would make a private intention pass while the measurement says
        // public — the one substitution this project has refused in 067, 097 and
        // 100 alike. So [geprueftesZiel] is the only repository this gate trusts,
        // and a mismatch between the two is refused rather than resolved.
        val geprueftesZiel = request.audit.repository
        val ziel = geprueftesZiel.displayName()

        // 0. The two must be the same target. A request pointing somewhere else
        //    than what was measured is not a verified upload at all.
        if (geprueftesZiel != request.repository) {
            return PushDecision.Refused(
                "Das beabsichtigte Ziel ${request.repository.displayName()} und das geprüfte " +
                    "Ziel $ziel sind nicht dasselbe. Es wird nichts gesendet."
            )
        }

        // 1. Does the target exist? An upload to a target nobody has found is an
        //    upload into the unknown.
        if (!request.audit.foundOnServer) {
            return PushDecision.Refused(
                "Das Ziel $ziel wurde am Gerät nicht gefunden. Es wird nichts gesendet."
            )
        }

        // 2. Is the privacy measured, and does it say private? The measured
        //    visibility is what counts — [RemoteRepository.visibility] on the
        //    request is an intention and would pass on its own.
        if (!geprueftesZiel.visibility.isPrivate) {
            return PushDecision.Refused(
                "Das Ziel $ziel ist als ${geprueftesZiel.visibility.germanLabel} " +
                    "gemeldet. Ein solches Ziel wird nicht als privat behandelt."
            )
        }
        if (!request.audit.visibilityVerifiedOnDevice) {
            return PushDecision.Refused(
                "Die Privatheit von $ziel wurde am Gerät nicht gegengeprüft. " +
                    "Ein Absichtswert ist keine Messung. Es wird nichts gesendet."
            )
        }

        // 3. A file list. Not a count — an upload with no named file has nothing
        //    the user could have reviewed.
        if (request.filePaths.isEmpty()) {
            return PushDecision.Refused(
                "Der Upload nennt keine Datei. Es wird nichts gesendet."
            )
        }

        // 4. The secret check, from task 100. A release does not lift it: a
        //    committed key stays committed, and the remedy is exclusion or
        //    removal, both of which the report names.
        val sperrend = plan.secretScanReport.blockingFindings
        if (sperrend.isNotEmpty()) {
            return PushDecision.Refused(
                "${sperrend.size} Fundstelle(n) sehen zufällig aus und würden mitgesendet. " +
                    "Betroffen: ${sperrend.map { it.path }.distinct().sorted().joinToString(", ")}. " +
                    "Es wird nichts gesendet."
            )
        }

        // 5. The release. Last, and its own input.
        if (!userReleasedUpload) {
            return PushDecision.Refused(
                "Der Nutzer hat den Upload nicht freigegeben. Es wird nichts gesendet."
            )
        }

        return PushDecision.MayPush(plan)
    }

    /**
 * The user backed out.
 *
 * A first-class answer, not the absence of a release. "Abbrechen" and "not
 * yet confirmed" look the same in a boolean and mean different things to the
 * person who pressed the button, so the decision carries its own state.
 */
    fun cancel(plan: GitPushPlan): PushDecision = PushDecision.Cancelled(
        "Der Nutzer hat abgebrochen. An ${plan.request.repository.displayName()} " +
            "wurde nichts gesendet."
    )
}

// ── Errors and partial transfer ────────────────────────────────────────────

/** How a transfer ended. Three of the four values are not success. */
enum class PushOutcome(val label: String, val germanLabel: String) {

    /** Every object was acknowledged. */
    COMPLETE("complete", "vollständig übertragen"),

    /**
     * Objects went out and no acknowledgement came back.
     *
     * Its own value, not a flavour of [COMPLETE] and not a failure: the content
     * may already be on the server, and retrying blindly is how a second copy is
     * created. Task 104 models the retry; this is the state it starts from.
     */
    PARTIAL("partial", "teilweise übertragen, unbestätigt"),

    /** Nothing was acknowledged and nothing is known to have arrived. */
    UNKNOWN("unknown", "unbestimmt"),

    /** The transfer reported an error. */
    FAILED("failed", "fehlgeschlagen")
}

/**
 * What the receiving side reported, and what may already be there.
 *
 * Built from two sets, not from a status string, because the interesting case is
 * the one no status string covers: sent, unacknowledged. [outcome] is derived and
 * never set, so it cannot disagree with the sets it comes from.
 */
data class PushState(
    val acceptedObjectIds: Set<ObjectId>,
    val unacknowledgedObjectIds: Set<ObjectId>,
    /** Non-empty exactly when [outcome] is [PushOutcome.FAILED]. */
    val errorMessage: String? = null
) {
    init {
        val doppelt = acceptedObjectIds intersect unacknowledgedObjectIds
        require(doppelt.isEmpty()) {
            "Ein Objekt kann nicht angenommen und zugleich unbestätigt sein: $doppelt"
        }
    }

    /**
     * The state, derived.
     *
     * The order matters: a reported error outranks the sets, because an error
     * with unacknowledged objects is still an error the user has to see.
     */
    val outcome: PushOutcome
        get() = when {
            errorMessage != null -> PushOutcome.FAILED
            unacknowledgedObjectIds.isNotEmpty() -> PushOutcome.PARTIAL
            acceptedObjectIds.isEmpty() -> PushOutcome.UNKNOWN
            else -> PushOutcome.COMPLETE
        }

    /** What might already be on the server. Shown before any retry, not after. */
    val mayAlreadyHaveArrived: Set<ObjectId>
        get() = acceptedObjectIds + unacknowledgedObjectIds

    /**
     * The lines for the user interface.
     *
     * The unacknowledged ids are **named**. "Possibly partly uploaded" is not
     * actionable; `b`, `c` are — that is what a user compares against the
     * server before deciding about a retry.
     */
    fun displayLines(): List<String> = buildList {
        when (outcome) {
            PushOutcome.COMPLETE -> add(
                "Vollständig übertragen: ${acceptedObjectIds.size} Objekt(e) wurden quittiert."
            )

            PushOutcome.PARTIAL -> {
                add(
                    "Teilweise übertragen: ${unacknowledgedObjectIds.size} Objekt(e) wurden " +
                        "abgeschickt, ohne dass eine Quittung eintraf."
                )
                add("Diese könnten bereits angekommen sein:")
                unacknowledgedObjectIds.sortedBy { it.value }.forEach { add("  $it") }
                add("Erst prüfen, dann erneut senden. Ein erneuter Versuch ohne Prüfung " +
                    "kann eine zweite Kopie anlegen.")
            }

            PushOutcome.UNKNOWN -> add(
                "Unbestimmt: es kam weder eine Quittung noch eine Fehlermeldung. " +
                    "Ob etwas angekommen ist, weiß niemand."
            )

            PushOutcome.FAILED -> {
                add("Fehlgeschlagen: ${errorMessage ?: "ohne Angabe einer Ursache"}")
                if (unacknowledgedObjectIds.isNotEmpty()) {
                    add("Abgeschickt, aber unbestätigt — könnten bereits angekommen sein:")
                    unacknowledgedObjectIds.sortedBy { it.value }.forEach { add("  $it") }
                }
            }
        }
    }

    companion object {

        /** From what the receiving side acknowledged. The [UNKNOWN] case is deliberate. */
        fun settledOrUncertain(
            accepted: Set<ObjectId>,
            unacknowledged: Set<ObjectId>
        ): PushState = PushState(accepted, unacknowledged)

        /** From an error report. A separate constructor so it cannot be confused. */
        fun failed(commitId: String, message: String): PushState = PushState(
            acceptedObjectIds = emptySet(),
            unacknowledgedObjectIds = emptySet(),
            errorMessage = "$commitId: $message"
        )
    }
}

// ── The honest gap ─────────────────────────────────────────────────────────

/**
 * What an actual upload would have to prove, and what nobody has observed yet.
 *
 * The task's condition reads "the upload **goes** to a private, existing
 * target". No run of this project has produced such an upload: the device is
 * unreachable, so there is no measurement to report. This type exists so that
 * gap stays a **value in the program** instead of a sentence in a document that
 * can be forgotten.
 *
 * [PushDecision.MayPush] means "every check passed and the sending is somebody
 * else's job". It is not a receipt. Turning it into one requires a run on the
 * device, and [PUSH_OBSERVED_ON_DEVICE] stays `false` until then. There is no
 * setter: an `observed` flag that a caller could flip would be an invention with
 * a checkbox.
 */
object DeviceUploadEvidence {

    /**
     * Has anyone observed an upload reaching a private target on the device?
     *
     * `false`, and it is a `const` rather than a `var` because the honest answer
     * must not be writable from a call site.
     */
    const val PUSH_OBSERVED_ON_DEVICE: Boolean = false

    /** The lines stating what is proven and what is not. */
    fun statementLines(): List<String> = listOf(
        "Geprüft und entschieden: Ziel, Sichtbarkeit, Dateien, Geheimnisprüfung, Freigabe.",
        "Nicht geprüft: ein tatsächlicher Upload. Auf diesem Gerät wurde keiner ausgeführt.",
        "Ein 'darf gesendet werden' ist eine Freigabe, kein Beleg. Der Beleg wäre eine",
        "Quittung des Servers — sie liegt nicht vor und wird nicht behauptet."
    )
}