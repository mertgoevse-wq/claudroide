package org.claudroide.app.feature.git

/**
 * Task 104 — „Git-Netzwerkfehler".
 *
 * Ziel: Einen Abbruch beim Laden oder Senden von Git-Daten nachvollziehbar
 * behandeln. Ergebnis: Wiederaufnahme, Statusprüfung, Konflikterkennung und
 * eine klare Meldung.
 *
 * Diese Datei führt **keinen** Netzverkehr aus und ruft **kein** Git auf. Sie
 * hält fest, was die Gegenstelle angenommen hat, was nur gesendet wurde, und was
 * daraus folgt — dieselbe Trennung wie in Aufgabe 096 und 102.
 *
 * ## Die zwei Fertig-Bedingungen als Eigenschaft
 *
 *  1. *„Wiederholen verursacht keine doppelten Commits oder ungewollten
 *     Uploads."*
 *     Jedes Objekt bekommt eine [ObjectId] — eine **stabile Kennung**, nicht
 *     eine Position in einer Liste. [TransferProgress.acceptedObjectIds] ist der
 *     Satz der Kennungen, die die Gegenstelle als **angenommen** gemeldet hat.
 *     [RetryPlan.excludedObjectIds] wird aus genau diesem Satz abgeleitet: ein
 *     erneuter Versuch sendet **nur**, was nicht darin steht. Es gibt keine
 *     Methode, die erneut „alles" sendet.
 *
 *     Wichtig ist die Trennung zwischen *gesendet* und *angenommen*: ein Paket,
 *     das abgeschickt, aber nicht quittiert wurde, ist **nicht** in
 *     [acceptedObjectIds] — und genau deshalb muss ein erneuter Versuch es
 *     erneut senden, statt es zu überspringen.
 *
 *  2. *„Nutzer sieht, welche Daten bereits übertragen sein könnten."*
 *     [TransferProgress.unacknowledgedObjectIds] ist die Menge, die gesendet,
 *     aber nicht bestätigt wurde — also **genau** das, was möglicherweise schon
 *     angekommen ist. Das steht als Eigenschaft da und wird nicht erst aus dem
 *     Zustand erschlossen.
 *
 * ## Der Schutz: unklare Zustände werden nie still fortgesetzt
 *
 * [TransferCertainty] kennt ein eigenes [UNCERTAIN]. [RetryPlan.forProgress]
 * liefert für diesen Fall **nie** einen Plan, der sendet — es gibt keine
 * Methode, die aus einem unbestätigten Zustand einen Fortsetzungsplan macht.
 * [RetryGate.mayRetry] verlangt zusätzlich eine ausdrückliche Bestätigung, und
 * die ist ein eigenes Feld mit Vorgabewert `false`.
 *
 * Reines Kotlin: kein Netz, kein Dateisystem, kein Prozess, kein Android.
 */

// ── Die Gegenstelle ───────────────────────────────────────────────────────

/**
 * Die stabile Kennung eines Übertragungsobjekts.
 *
 * „Stabil" heißt: derselbe Inhalt ergibt dieselbe Kennung, und zwar bevor er
 * gesendet wird. Deshalb wird sie nicht als Nummer in einer Liste vergeben —
 * eine Liste hat Positionen, und eine Position ändert sich beim Wiederholen.
 * Genau darin liegt der Fehler, den diese Aufgabe verhindert.
 */
data class ObjectId(val value: String) {
    init {
        require(value.isNotBlank()) { "Eine Objektkennung darf nicht leer sein." }
        require(value.none { it.isISOControl() }) {
            "Eine Objektkennung kann kein Steuerzeichen enthalten."
        }
    }

    override fun toString(): String = value
}

/** Wie sicher ist der Zustand der Übertragung? */
enum class TransferCertainty(val label: String, val germanLabel: String) {

    /** Jedes gesendete Objekt ist bestätigt. */
    SETTLED("settled", "abgeschlossen"),

    /**
     * Mindestens ein Objekt ist gesendet, aber nicht bestätigt.
     *
     * Der Zustand, in dem ein erneuter Versuch **gefährlich** ist: Der Inhalt
     * könnte beim ersten Mal angekommen sein. Er steht deshalb als eigener Wert
     * und nicht als Fußnote in einem Kommentar.
     */
    UNCERTAIN("uncertain", "unklar")
}

// ── Der Fortschritt ───────────────────────────────────────────────────────

/**
 * Was beim Übertragen tatsächlich passiert ist.
 *
 * Der Konstruktor weist den widersprüchlichen Fall ab: ein Objekt kann nicht
 * gleichzeitig angenommen und unbestätigt sein, und „bestätigt" heißt nicht
 * gleichzeitig „nicht gesendet".
 */
data class TransferProgress(
    /** Kennungen, die die Gegenstelle als angenommen gemeldet hat. */
    val acceptedObjectIds: Set<ObjectId>,
    /** Kennungen, die abgeschickt wurden, ohne dass eine Quittung eintraf. */
    val unacknowledgedObjectIds: Set<ObjectId>,
    /** Soll übertragen werden. */
    val pendingObjectIds: List<ObjectId>
) {
    init {
        val doppelt = acceptedObjectIds intersect unacknowledgedObjectIds
        require(doppelt.isEmpty()) {
            "Ein Objekt kann nicht angenommen und zugleich unbestätigt sein: $doppelt"
        }
        val doppelt2 = acceptedObjectIds intersect pendingObjectIds.toSet()
        require(doppelt2.isEmpty()) {
            "Ein angenommenes Objekt steht nicht mehr in der Liste der offenen Objekte: $doppelt2"
        }
        // Bewusst NICHT erhoben: ein unbestätigtes Objekt darf zugleich noch
        // offen sein. Genau dieser Fall ist der Grund für diese Datei.
        require(pendingObjectIds.distinct().size == pendingObjectIds.size) {
            "In der Liste der offenen Objekte darf keine Kennung doppelt stehen."
        }
    }

    /**
     * Der Zustand, abgeleitet — nie gesetzt.
     *
     * [UNCERTAIN] entsteht genau dann, wenn ein Objekt ohne Quittung
     * abgeschickt wurde. Er ist kein Randfall, sondern der Normalfall eines
     * Abbruchs mitten in der Übertragung.
     */
    val certainty: TransferCertainty
        get() = if (unacknowledgedObjectIds.isEmpty()) {
            TransferCertainty.SETTLED
        } else {
            TransferCertainty.UNCERTAIN
        }

    /**
     * Das könnte bereits angekommen sein.
     *
     * Die zweite Fertig-Bedingung als Eigenschaft: Der Nutzer bekommt diese
     * Menge **vor** der Entscheidung über einen erneuten Versuch zu sehen, und
     * sie wird nicht aus dem Zustand geraten.
     */
    val mayAlreadyHaveArrived: Set<ObjectId>
        get() = unacknowledgedObjectIds + acceptedObjectIds
}

/**
 * Der erneute Versuch — **berechnet**, nicht behauptet.
 *
 * [excludedObjectIds] ist das, was **nicht** erneut gesendet wird. Es wird aus
 * [TransferProgress.acceptedObjectIds] abgeleitet und kann deshalb nicht
 * von einer Absicht abweichen.
 */
data class RetryPlan(
    val objectIdsToSend: List<ObjectId>,
    val excludedObjectIds: Set<ObjectId>
) {
    init {
        val doppelt = objectIdsToSend.toSet() intersect excludedObjectIds
        require(doppelt.isEmpty()) {
            "Ein Objekt kann nicht gleichzeitig gesendet und ausgeschlossen sein: $doppelt"
        }
    }

    /** Gibt es überhaupt etwas zu senden? */
    val hasWork: Boolean get() = objectIdsToSend.isNotEmpty()

    /** Alle Kennungen, die in diesem Versuch eine Rolle spielen. */
    val allKnownObjectIds: Set<ObjectId> get() = objectIdsToSend.toSet() + excludedObjectIds

    companion object {
        /**
         * Der Plan aus einem Fortschritt.
         *
         * Gesendet wird, was **nicht** angenommen wurde: die noch offenen
         * Objekte und die unbestätigten. Ausgeschlossen sind ausschließlich die
         * [TransferProgress.acceptedObjectIds] — für sie steht die Quittung der
         * Gegenstelle fest, und sie zu senden wäre genau der doppelte Commit,
         * den diese Aufgabe verhindert.
         *
         * Dass hier auch unbestätigte Objekte stehen, ist **kein** Versehen.
         * Dieser Plan beschreibt, was ein erneuter Versuch täte; wer ihn
         * freigibt, entscheidet [RetryGate]. Und [RetryGate] lässt diesen Plan
         * nur zu, wenn [TransferProgress.certainty] [TransferCertainty.SETTLED]
         * ist — bei unbestätigten Objekten kommt man hier gar nicht durch.
         * Die Verantwortung für das Risiko liegt damit an einer Stelle und nicht
         * verteilt in einer Liste.
         */
        fun forProgress(progress: TransferProgress): RetryPlan = RetryPlan(
            objectIdsToSend = progress.pendingObjectIds + progress.unacknowledgedObjectIds,
            excludedObjectIds = progress.acceptedObjectIds
        )
    }
}

// ── Das Tor ───────────────────────────────────────────────────────────────

/** Die Antwort auf „darf erneut gesendet werden?". */
sealed interface RetryDecision {
    val germanLabel: String

    /** Der Versuch ist berechnet und darf — nach Bestätigung — laufen. */
    data class MayRetry(
        val plan: RetryPlan,
        /** Was beim letzten Mal ohne Quittung abgeschickt wurde. */
        val mayAlreadyHaveArrived: Set<ObjectId>
    ) : RetryDecision {
        override val germanLabel: String get() = "erneuter Versuch möglich"
    }

    /** Nichts wird erneut gesendet. */
    data class NeedsConfirmation(val reason: String, val uncertainIds: Set<ObjectId>) :
        RetryDecision {
        override val germanLabel: String get() = "Bestätigung nötig"
    }

    /** Nichts zu senden. */
    data class NothingToSend(val reason: String) : RetryDecision {
        override val germanLabel: String get() = "nichts zu senden"
    }
}

/**
 * Die Regel, wann ein erneuter Versuch beginnen darf.
 *
 * Geprüft wird in fester Reihenfolge: **erst** der Zustand, **dann** die
 * Bestätigung. In dieser Reihenfolge, weil sie die strengere ist: Solange etwas
 * unbestätigt ist, kann keine Bestätigung den Zustand klären — sie kann nur
 * sagen, dass der Nutzer es riskiert.
 */
object RetryGate {

    /**
     * Darf nach [progress] erneut gesendet werden?
     *
     * @param userConfirmedRetry eine **eigene** Eingabe des Nutzers, Vorgabe
     *        `false`. Aus keinem anderen Umstand wird sie abgeleitet, und sie
     *        wird nie aus [TransferProgress] gelesen.
     */
    fun mayRetry(progress: TransferProgress, userConfirmedRetry: Boolean = false): RetryDecision {
        val plan = RetryPlan.forProgress(progress)

        // 1. Der unklare Zustand kommt zuerst. Es gibt keinen Pfad durch diese
        //    Tür, der bei [TransferCertainty.UNCERTAIN] sendet.
        if (progress.certainty == TransferCertainty.UNCERTAIN) {
            return RetryDecision.NeedsConfirmation(
                reason = "Es wurde(n) ${progress.unacknowledgedObjectIds.size} Objekt(e) " +
                    "abgeschickt, ohne dass eine Quittung eintraf. Diese Daten könnten " +
                    "bereits angekommen sein. Es wird nichts erneut gesendet.",
                uncertainIds = progress.unacknowledgedObjectIds
            )
        }

        // 2. Bestätigung. Eine eigene Eingabe, nie abgeleitet.
        if (!userConfirmedRetry) {
            return RetryDecision.NothingToSend(
                "Der Nutzer hat den erneuten Versuch nicht bestätigt. Es wird nichts gesendet."
            )
        }

        // 3. Nur, wenn tatsächlich Arbeit offen ist.
        if (!plan.hasWork) {
            return RetryDecision.NothingToSend(
                "Alle Objekte sind bereits angenommen. Es gibt nichts zu senden."
            )
        }

        return RetryDecision.MayRetry(
            plan = plan,
            mayAlreadyHaveArrived = progress.mayAlreadyHaveArrived
        )
    }
}