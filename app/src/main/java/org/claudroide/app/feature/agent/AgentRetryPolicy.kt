package org.claudroide.app.feature.agent

/**
 * Task 079 — „Wiederholungsregeln“.
 *
 * Ziel: Bei vorübergehendem Fehler sicher fortfahren, ohne Aktionen doppelt
 * auszuführen.
 *
 * Der Fehlerfall, vor dem diese Klasse schützt, ist nicht der Absturz, sondern der
 * **zweite Versuch**: Ein Schreibvorgang, der beim ersten Mal vielleicht schon
 * gewirkt hat und trotzdem als fehlgeschlagen gemeldet wurde. Deshalb sind die
 * Zusagen des Auftrags in der Typentscheidung verankert:
 *
 *  1. **Schreib-, Installations- und Git-Aktionen werden nie automatisch
 *     wiederholt.** [RetryAction.neverAutoRetry] ist am Typ befestigt: Für
 *     `INSTALL` und `GIT_PUSH` ist es `true`, und **kein** Parameter von
 *     [AgentRetryPolicy.decide] kann das aufheben — auch eine Idempotenz-Bescheinigung
 *     und eine Nutzerbestätigung nicht. Diese Aktionen enden immer in
 *     [RetryDecision.AskUser]. Das ist der Unterschied zwischen einer Regel, die man
 *     umgehen *könnte*, und einer, die man nicht umgehen *kann*.
 *
 *  2. **Kostenpflichtige Anfragen sind vor der Wiederholung sichtbar.** Für jede
 *     Aktion mit [RetryAction.costsMoney] verlangt [AgentRetryPolicy.decide] das
 *     Flag `costNoticeShown`. Ohne dieses Flag gibt es kein [RetryDecision.RetryNow],
 *     sondern [RetryDecision.AskUser] — eine Wiederholung auf Kosten des Nutzers,
 *     die er nicht gesehen hat, ist damit nicht darstellbar.
 *
 *  3. **Idempotenz oder Bestätigung für Nebenwirkungen.** Eine Aktion mit
 *     [RetryAction.hasSideEffect] wird nur automatisch wiederholt, wenn
 *     [Idempotency.PROVEN] vorliegt oder der Nutzer bestätigt hat. Sonst: nachfragen.
 *
 *  4. **Nur vorübergehende Fehler werden wiederholt.** [FailureKind.UNKNOWN] ist
 *     bewusst *kein* vorübergehender Fehler: Solange nicht feststeht, was passiert
 *     ist, ist Wiederholen die riskantere Annahme. Und was bereits einmal lief,
 *     läuft nicht noch einmal — [AgentRetryPolicy.decideWithStore] fragt
 *     [AgentRunStore.mayExecuteCall] aus Aufgabe 073 und bricht mit
 *     [RetryDenialReason.ALREADY_EXECUTED] ab.
 *
 * Reines Kotlin: keine Datei, kein Netzwerk, kein Anbieter. Die Wartezeit wird
 * berechnet, nicht abgearbeitet — das Aufrufen von `Thread.sleep` liegt beim Anbieter.
 */

/** Eine Aktion, die wiederholt werden *könnte*. */
enum class RetryAction(
    val germanLabel: String,
    /** Verändert sie etwas außerhalb der laufenden Anfrage? */
    val hasSideEffect: Boolean,
    /** Kostet sie beim Ausführen Geld? */
    val costsMoney: Boolean,
    /**
     * Darf sie **nie** automatisch wiederholt werden?
     *
     * Installationen und Git-Upload stehen hier auf `true`, weil beide
     * Dinge verändern, die außerhalb der App liegen und die ein zweiter Versuch
     * doppelt auslösen könnte.
     */
    val neverAutoRetry: Boolean
) {
    /** Eine Datei lesen. */
    READ_FILE("Datei lesen", false, false, false),

    /** Einen Ordner auflisten. */
    LIST_DIRECTORY("Ordner auflisten", false, false, false),

    /** In Dateien suchen. */
    SEARCH_TEXT("Text suchen", false, false, false),

    /** Tests ausführen. Verändert nur den Build-Ordner. */
    RUN_TEST("Tests ausführen", false, false, false),

    /** Eine Anfrage an ein Modell. Kostet bei jedem Versuch. */
    MODEL_REQUEST("Modellanfrage", false, true, false),

    /** Eine Datei schreiben. */
    WRITE_FILE("Datei schreiben", true, false, true),

    /** Eine Datei löschen. */
    DELETE_FILE("Datei löschen", true, false, true),

    /** Etwas installieren. */
    INSTALL("etwas installieren", true, true, true),

    /** Einen Commit anlegen. */
    GIT_COMMIT("Commit anlegen", true, false, true),

    /** Ins Repository hochladen. */
    GIT_PUSH("ins Repository hochladen", true, false, true)
}

/** Wie die Fehlermeldung einzuordnen ist. */
enum class FailureKind(val germanLabel: String) {

    /** Ein Fehler, der von selbst verschwinden kann: Timeout, Verbindung abgerissen. */
    TRANSIENT("vorübergehend"),

    /** Ein Fehler, der bleibt: fehlende Berechtigung, unbekanntes Werkzeug. */
    PERMANENT("dauerhaft"),

    /**
     * Es steht nicht fest, was passiert ist.
     *
     * Wird absichtlich **nicht** wie [TRANSIENT] behandelt: Nach einem Abbruch mit
     * ungeklärter Nebenwirkung ist Wiederholen die Annahme, die mehr schadet.
     */
    UNKNOWN("ungeklärt")
}

/** Ob ein erneuter Versuch denselben Zustand erzeugen würde. */
enum class Idempotency(val germanLabel: String) {

    /** Es ist nicht bekannt. */
    UNKNOWN("nicht bekannt"),

    /** Der Zustand ist nachweislich derselbe. */
    PROVEN("nachgewiesen"),

    /** Der Zustand kann nicht derselbe sein. */
    IMPOSSIBLE("nicht möglich")
}

/** Warum nicht automatisch wiederholt wird. */
enum class RetryDenialReason(val germanLabel: String) {

    /** Die Zahl der Versuche ist erreicht. */
    ATTEMPTS_EXHAUSTED("Zahl der Versuche erreicht"),

    /** Der Fehler ist nicht vorübergehend. */
    NOT_TRANSIENT("Fehler ist nicht vorübergehend"),

    /** Der Fehler ist ungeklärt. */
    UNKNOWN_FAILURE("Fehler ist ungeklärt"),

    /** Diese Aktion wird nie automatisch wiederholt. */
    NEVER_AUTOMATIC("wird nie automatisch wiederholt"),

    /** Ohne Idempotenz oder Bestätigung ist Wiederholen unsicher. */
    NEEDS_CONFIRMATION("Nebenwirkung ohne Nachweis"),

    /** Kostenpflichtige Aktion ohne sichtbaren Kostenhinweis. */
    COST_NOT_VISIBLE("Kosten nicht sichtbar gemacht"),

    /** Die Aktion ist bereits gelaufen. */
    ALREADY_EXECUTED("bereits ausgeführt")
}

/** Das Ergebnis einer Wiederholungsfrage. */
sealed interface RetryDecision {

    /** Der erneute Versuch darf automatisch erfolgen. */
    data class RetryNow(
        val delayMillis: Long,
        val noticeLines: List<String>
    ) : RetryDecision

    /** Der Nutzer muss entscheiden. */
    data class AskUser(val reason: RetryDenialReason, val germanExplanation: String) : RetryDecision

    /** Kein weiterer Versuch. */
    data class DoNotRetry(val reason: RetryDenialReason, val germanExplanation: String) : RetryDecision
}

/**
 * Legt fest, was nach einem Fehler geschieht.
 *
 * @property maxAttempts wie viele Versuche insgesamt gemacht werden dürfen,
 *           einchließlich des ersten. Der Wert `1` bedeutet also: **kein** Retry.
 */
class AgentRetryPolicy(
    private val maxAttempts: Int = 3,
    private val baseDelayMillis: Long = 500L,
    private val maxDelayMillis: Long = 8_000L
) {

    init {
        require(maxAttempts >= 1) { "Es muss mindestens einen Versuch geben." }
        require(baseDelayMillis >= 0L) { "Die Wartezeit kann nicht negativ sein." }
        require(maxDelayMillis >= baseDelayMillis) {
            "Die Obergrenze der Wartezeit darf nicht unter der Anfangswartezeit liegen."
        }
    }

    /**
     * Die Frage: Darf [action] noch einmal versucht werden?
     *
     * @param attemptsSoFar wie viele Versuche **bereits** liefen, einchließlich des
     *           ersten. Der erste Fehlschlag hat also den Wert `1`.
     * @param failureKind wie die Fehlermeldung einzuordnen ist.
     * @param idempotency ob ein zweiter Versuch denselben Zustand ergäbe.
     * @param userConfirmed hat der Nutzer diesen erneuten Versuch bestätigt?
     * @param costNoticeShown wurde der Kostenhinweis dieser Aktion dem Nutzer
     *           tatsächlich angezeigt?
     * @param alreadyExecuted ist diese Aktion nachweislich schon gelaufen?
     */
    fun decide(
        action: RetryAction,
        attemptsSoFar: Int,
        failureKind: FailureKind,
        idempotency: Idempotency = Idempotency.UNKNOWN,
        userConfirmed: Boolean = false,
        costNoticeShown: Boolean = false,
        alreadyExecuted: Boolean = false
    ): RetryDecision {
        require(attemptsSoFar >= 0) { "Die Zahl der Versuche kann nicht negativ sein." }

        // 1. Was schon lief, läuft nicht noch einmal — vor allen anderen Fragen.
        if (alreadyExecuted) {
            return RetryDecision.DoNotRetry(
                RetryDenialReason.ALREADY_EXECUTED,
                "„${action.germanLabel}“ ist bereits ausgeführt. Ein zweiter Versuch " +
                    "würde dieselbe Aktion doppelt auslösen."
            )
        }

        // 2. Nur vorübergehende Fehler werden wiederholt.
        if (failureKind == FailureKind.UNKNOWN) {
            return RetryDecision.DoNotRetry(
                RetryDenialReason.UNKNOWN_FAILURE,
                "Es steht nicht fest, was beim letzten Versuch passiert ist. " +
                    "Bitte erst nachsehen, dann neu starten."
            )
        }
        if (failureKind == FailureKind.PERMANENT) {
            return RetryDecision.DoNotRetry(
                RetryDenialReason.NOT_TRANSIENT,
                "„${action.germanLabel}“ ist mit diesem Fehler dauerhaft gescheitert. " +
                    "Ein weiterer Versuch bringt nichts."
            )
        }

        // 3. Die Zahl der Versuche.
        if (attemptsSoFar >= maxAttempts) {
            return RetryDecision.DoNotRetry(
                RetryDenialReason.ATTEMPTS_EXHAUSTED,
                "„${action.germanLabel}“ ist $attemptsSoFar-mal versucht worden; " +
                    "erlaubt sind $maxAttempts. Bitte den Nutzer informieren."
            )
        }

        // 4. Schreib-, Installations- und Git-Aktionen: nie automatisch. Hier endet
        //    jede Hoffnung auf einen automatischen Versuch — auch mit Idempotenz und
        //    Bestätigung. Nur der Nutzer selbst entscheidet das außerhalb dieser Regel.
        if (action.neverAutoRetry) {
            return RetryDecision.AskUser(
                RetryDenialReason.NEVER_AUTOMATIC,
                "„${action.germanLabel}“ wird nicht automatisch wiederholt. " +
                    "Bitte den Nutzer fragen, ob es erneut versucht werden soll."
            )
        }

        // 5. Nebenwirkung ohne Nachweis: nachfragen.
        //    Mit dem heutigen Aktionssatz ist dieser Zweig vom strengeren
        //    NEVER_AUTOMATIC ueberlagert — jede Aktion mit Nebenwirkung traegt auch
        //    die automatische Sperre. Er bleibt als zweite Linie stehen, falls
        //    kuenftig eine Nebenwirkung ohne Sperre hinzukommt, und ist dann
        //    unmittelbar wirksam.
        if (action.hasSideEffect && idempotency != Idempotency.PROVEN && !userConfirmed) {
            return RetryDecision.AskUser(
                RetryDenialReason.NEEDS_CONFIRMATION,
                "„${action.germanLabel}“ verändert etwas, und es ist nicht " +
                    "nachgewiesen, dass ein zweiter Versuch dasselbe Ergebnis hat. " +
                    "Bitte den Nutzer fragen."
            )
        }

        // 6. Kostenpflichtige Aktion: erst nach sichtbarem Hinweis.
        if (action.costsMoney && !costNoticeShown) {
            return RetryDecision.AskUser(
                RetryDenialReason.COST_NOT_VISIBLE,
                "„${action.germanLabel}“ kostet beim erneuten Versuch erneut. " +
                    "Der Kostenhinweis ist dem Nutzer noch nicht gezeigt worden."
            )
        }

        return RetryDecision.RetryNow(
            delayMillis = delayFor(attemptsSoFar),
            noticeLines = noticeLinesFor(action, attemptsSoFar)
        )
    }

    /**
     * Dieselbe Frage, aber mit dem echten Laufstatus aus Aufgabe 073.
     *
     * Statt `alreadyExecuted` zu raten, wird [AgentRunStore.mayExecuteCall]
     * befragt: Dort steht, welcher Aufruf bereits einmal verbucht wurde. Damit hängt
     * die Wiederholungsregel an derselben Quelle, die auch den Neustart schützt.
     */
    fun decideWithStore(
        store: AgentRunStore,
        action: RetryAction,
        toolName: String,
        path: String?,
        attemptsSoFar: Int,
        failureKind: FailureKind,
        idempotency: Idempotency = Idempotency.UNKNOWN,
        userConfirmed: Boolean = false,
        costNoticeShown: Boolean = false
    ): RetryDecision = decide(
        action = action,
        attemptsSoFar = attemptsSoFar,
        failureKind = failureKind,
        idempotency = idempotency,
        userConfirmed = userConfirmed,
        costNoticeShown = costNoticeShown,
        alreadyExecuted = !store.mayExecuteCall(toolName, path)
    )

    /**
     * Die Wartezeit vor dem nächsten Versuch.
     *
     * Verdoppelt sich je Versuch und wird bei [maxDelayMillis] abgeschnitten.
     * Bewusst **ohne** Zufallsstreuung: Sie würde hier nichts lösen und dafür eine
     * nicht überprüfbare Zahl in den Tests erzeugen.
     */
    fun delayFor(attemptsSoFar: Int): Long {
        if (attemptsSoFar <= 0) return 0L
        val factor = 1L shl (attemptsSoFar - 1).coerceAtMost(20)
        return (baseDelayMillis * factor).coerceAtMost(maxDelayMillis)
    }

    /** Die Zeilen, die dem Nutzer vor einem erneuten Versuch angezeigt werden. */
    private fun noticeLinesFor(action: RetryAction, attemptsSoFar: Int): List<String> = buildList {
        add(
            "„${action.germanLabel}“ wird erneut versucht " +
                "(${attemptsSoFar + 1}. von $maxAttempts Versuchen)."
        )
        if (action.costsMoney) {
            add("Hinweis: Dieser erneute Versuch kostet erneut Geld.")
        }
        add("Wartezeit dazwischen: ${delayFor(attemptsSoFar)} ms.")
    }

    /** Die Zeilen für die Anzeige, unabhängig vom Ergebnis. */
    fun disclosureLines(): List<String> = buildList {
        add("Wiederholungsregeln:")
        add("  Höchstzahl Versuche: $maxAttempts.")
        add("  Schreib-, Installations- und Git-Aktionen werden nie automatisch wiederholt.")
        add("  Kostenpflichtige Anfragen werden erst nach sichtbarem Hinweis wiederholt.")
        add("  Ungeklärte Fehler werden nicht wiederholt.")
        RetryAction.values()
            .filter { it.neverAutoRetry }
            .forEach { add("  ${it.germanLabel}: nur nach Rückfrage.") }
    }
}
