package org.claudroide.app.feature.agent

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.claudroide.app.core.design.AccessibilityPolicy
import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 075 — „Fortschritt anzeigen“.
 *
 * Ziel: Der Nutzer weiß jederzeit, was der Agent gerade macht und worauf er
 * wartet. Vier Zusagen sind strukturell abgesichert:
 *
 *  1. **Fortschritt gibt nicht vor, eine unbekannte Aufgabe sei erledigt.**
 *     Der Zustand kennt [ProgressPhase.UNKNOWN], und es gibt keinen Weg, aus
 *     [ProgressPhase.UNKNOWN] ein „fertig“ zu machen. [ProgressUiState.isComplete]
 *     ist nur `true`, wenn [ProgressPhase] tatsächlich `FINISHED` ist — eine
 *     unbekannte Phase kann also nie als Erfolg erscheinen, auch nicht in einer
 *     Benachrichtigung. Wird eine Aufgabe ohne Phase gemeldet, ist das Ergebnis
 *     „unbekannt, nicht abgeschlossen“.
 *
 *  2. **Wartezeiten sind mit Stopp verbunden.** [ProgressPhase.isWaiting] ist
 *     eine Eigenschaft der Phase, und [ProgressUiState.showStopAction] leitet
 *     sich genau daraus ab. Es gibt keine Wartephase ohne Stopp-Knopf: Ein
 *     Zustand, in dem gewartet wird, kann nicht gleichzeitig so tun, als gäbe es
 *     nichts zu unterbrechen. Dasselbe gilt für die Benachrichtigung — sie nennt
 *     die Wartezeit *und* den Abbruch.
 *
 *  3. **Keine privaten Werkzeugausgaben in Benachrichtigungen.**
 *     [ProgressUiState.notificationLines] entsteht aus einer **Positivliste**:
 *     Phase, Werkzeugname, Anzahl, Schrittbezeichnung. Es gibt kein Feld, über
 *     das eine Werkzeugausgabe in die Benachrichtigung gelangen könnte — und
 *     jeder Text läuft zusätzlich durch [SecretMasker]. Auf dem Bildschirm darf
 *     die Ausgabe stehen (der Nutzer hat sie angefordert), in der
 *     Benachrichtigung nicht: [ProgressUiState.notificationText] kann sie nicht
 *     enthalten, weil er sie nicht kennt.
 *
 *  4. **Die Anzeige passt sich dem Display an.** [ProgressUiState.lines] liefert
 *     bei kompakter Breite eine Kurzfassung ohne Details, sonst die vollständige
 *     Liste. Der Stopp-Knopf ist in beiden Fällen [AccessibilityPolicy.MinimumTouchTarget]
 *     groß (48 dp) — er wird in dem Moment getippt, in dem etwas klemmt, und ist
 *     genau dann am schwersten zu treffen.
 *
 * Reines Kotlin mit einem einzigen Compose-Import für die Touch-Target-Konstante,
 * damit die 48 dp nicht an zwei Stellen im Code stehen.
 */
object AgentProgressPresenter {

    /** Was der Agent gerade tut. */
    enum class ProgressPhase(val germanLabel: String) {
        /** Der Plan liegt vor, es wurde noch nichts angefasst. */
        PLANNED("geplant"),

        /** Die Systemanweisung und die Werkzeugbeschreibungen werden zusammengestellt. */
        PREPARING("Kontakt wird vorbereitet"),

        /** Der Nutzer wird um eine Freigabe gebeten — hier wartet der Agent. */
        WAITING_FOR_APPROVAL("wartet auf Ihre Zustimmung"),

        /** Ein Anbieter wartet auf eine Antwort. */
        WAITING_FOR_PROVIDER("wartet auf die Antwort des Anbieters"),

        /** Ein Werkzeug läuft. */
        RUNNING_TOOL("führt ein Werkzeug aus"),

        /** Der Nutzer hat abgebrochen. */
        ABORTED("abgebrochen"),

        /** Der Lauf ist vollständig durchgelaufen. */
        FINISHED("abgeschlossen"),

        /**
         * Der Zustand ist nicht bekannt — etwa nach einem Absturz oder bei einem
         * Lauf, der aus einem fremden Grund weitergegeben wurde.
         *
         * Dieser Wert ist kein Notnagel: Er ist die ehrliche Antwort, und er wird
         * nie als „abgeschlossen“ dargestellt.
         */
        UNKNOWN("unbekannt — hier läuft gerade nichts nachweislich");

        /**
         * Wartet der Agent in dieser Phase auf etwas?
         *
         * Aus dieser Eigenschaft leitet sich die Stopp-Möglichkeit ab. Eine Phase,
         * in der gewartet wird, kann den Nutzer nicht hängen lassen.
         */
        val isWaiting: Boolean
            get() = this == WAITING_FOR_APPROVAL || this == WAITING_FOR_PROVIDER
    }

    /** Die Anzeige, wie die Oberfläche sie braucht. */
    data class ProgressUiState(
        val phase: ProgressPhase,
        /** Der Schritt, um den es geht. Leer heißt „noch kein Schritt“. */
        val stepTitle: String = "",
        /** Wie viele Schritte von wie vielen erledigt sind. `-1` heißt unbekannt. */
        val completedSteps: Int = -1,
        val totalSteps: Int = -1,
        /** Der zuletzt ausgeführte Werkzeugname. Leer heißt „noch keines“. */
        val lastToolName: String = "",
        /**
         * Die Ausgabe des letzten Werkzeugs — **nur für den Bildschirm**.
         *
         * Sie ist absichtlich kein Teil von [notificationText]: Eine Benachrichtigung
         * erscheint auf dem Sperrbildschirm und in der Statusleiste. Das ist ein
         * anderer Ort als der Bildschirm, auf dem der Nutzer gerade arbeitet.
         */
        val lastToolOutput: String = "",
        /** Was getan werden muss, bevor es weitergeht. Leer heißt: nichts. */
        val pendingApproval: String = "",
        /** Wartezeit in Millisekunden; `0` heißt „es wird nicht gewartet“. */
        val waitingSinceMillis: Long = 0L,
        /** Wie viele Werkzeugaufrufe in diesem Lauf fehlgeschlagen sind. */
        val failedToolCalls: Int = 0
    ) {

        /** Nur bei wirklich abgeschlossenem Lauf `true`. */
        val isComplete: Boolean
            get() = phase == ProgressPhase.FINISHED

        /** Der Agent wartet auf etwas. */
        val isWaiting: Boolean
            get() = phase.isWaiting

        /**
         * Muss der Stopp-Knopf sichtbar sein?
         *
         * Abgeleitet, nicht angegeben: In jeder Wartephase — und zusätzlich, solange
         * etwas läuft. Es gibt keinen wartenden Zustand ohne diese Möglichkeit,
         * weil die Eigenschaft aus der Phase folgt.
         */
        val showStopAction: Boolean
            get() = phase.isWaiting || phase == ProgressPhase.RUNNING_TOOL

        /** Die Mindestgröße des Stopp-Knopfes. */
        val stopButtonMinSize: Dp = AccessibilityPolicy.MinimumTouchTarget

        /** Wie weit der Lauf ist — oder `null`, wenn das nicht bekannt ist. */
        fun progressFraction(): Float? {
            if (completedSteps < 0 || totalSteps <= 0) return null
            if (completedSteps > totalSteps) return null
            return completedSteps.toFloat() / totalSteps.toFloat()
        }

        /**
         * Die Anzeige für den Bildschirm.
         *
         * @param isCompact `true` bei schmaler Breite: Dann nur Phase, letzter
         *        Schritt und Stopp-Hinweis. Die vollständige Liste mit Werkzeug und
         *        Ausgabe braucht Platz, den ein schmales Display nicht hat.
         */
        fun lines(isCompact: Boolean = false): List<String> = buildList {
            add("Status: ${phase.germanLabel}")
            if (stepTitle.isNotBlank()) add("Schritt: $stepTitle")

            val fraction = progressFraction()
            if (fraction != null) {
                add("Fortschritt: $completedSteps von $totalSteps Schritten")
            } else {
                // Kein erfundener Fortschrittsbalken: Ohne bekannte Gesamtzahl wird
                // keiner angezeigt.
                add("Fortschritt: noch nicht bekannt")
            }

            if (lastToolName.isNotBlank()) {
                add("Zuletzt: ${lastToolName}")
            }
            if (failedToolCalls > 0) {
                add("$failedToolCalls Werkzeugaufruf/-aufrufe fehlgeschlagen.")
            }
            if (pendingApproval.isNotBlank()) {
                add("Erfordert Ihre Zustimmung: $pendingApproval")
            }
            if (isWaiting) {
                add(
                    "Sie warten. „Stoppen“ bricht den Vorgang ab; bitte sagen Sie " +
                        "Bescheid, wenn nichts mehr kommt."
                )
            }
            if (!isCompact) {
                if (lastToolOutput.isNotBlank()) {
                    add("Ausgabe: ${SecretMasker.redact(lastToolOutput)}")
                }
                if (phase == ProgressPhase.UNKNOWN) {
                    add(
                        "Der Zustand ist nicht bekannt. Es wird nichts als erledigt " +
                            "angezeigt, solange das nicht belegt ist."
                    )
                }
            }
        }

        /**
         * Der Text für eine Benachrichtigung.
         *
         * Entsteht ausschließlich aus Positivangaben — Phase, Schritt, Werkzeugname,
         * Anzahl, Freigabe. Die Werkzeugausgabe ist hier **nicht** erreichbar, auch
         * nicht über eine Umleitung: Diese Funktion liest sie nicht.
         */
        fun notificationText(): String {
            val head = "ClauDroide: ${phase.germanLabel}"
            val detail = when {
                stepTitle.isNotBlank() -> " — $stepTitle"
                else -> ""
            }
            val waiting = if (isWaiting) {
                " Tippen Sie zum Stoppen."
            } else {
                ""
            }
            return SecretMasker.redact(head + detail + waiting)
        }

        /** Der Text für eine Benachrichtigung, mit Abbruchhinweis bei Wartezeit. */
        fun notificationTextCompact(): String = notificationText()
    }

    /**
     * Baut den Zustand aus dem Laufstatus.
     *
     * Der Aufrufer liefert [AgentRunState] und den aktuellen [ProgressPhase]; die
     * Klasse rechnet daraus die Anzeige. Fehlt zu einem Schritt der Zustand,
     * entsteht `UNKNOWN` — nicht „fertig“.
     */
    fun fromRun(
        state: AgentRunState,
        phase: ProgressPhase,
        stepId: String? = null,
        lastToolOutput: String = "",
        waitingSinceMillis: Long = 0L
    ): ProgressUiState {
        val step = stepId?.let { state.step(it) }
        val completed = state.steps.count { it.status == AgentRunStatus.FINISHED }
        val phaseForDisplay = if (step == null && stepId != null) {
            // Ein genannter Schritt ohne jeden Eintrag im Status: Das wissen wir
            // nicht, und „abgeschlossen“ wäre geraten.
            ProgressPhase.UNKNOWN
        } else {
            phase
        }
        return ProgressUiState(
            phase = phaseForDisplay,
            // Der Schritt wird über seine Kennung benannt. Einen Titel gibt es im
            // Laufstatus nicht, und der Hinweis eines Schritts ist eine Meldung,
            // kein Name — sie als Überschrift zu missbrauchen wäre irreführend.
            stepTitle = step?.stepId.orEmpty(),
            completedSteps = if (state.steps.isEmpty()) -1 else completed,
            // Ohne Schritte ist die Gesamtzahl unbekannt, nicht „null“. Beide
            // Kennzahlen stehen dann auf -1, damit kein Balken mit „0 von 0“
            // erscheint und beides dasselbe meint.
            totalSteps = if (state.steps.isEmpty()) -1 else state.steps.size,
            lastToolName = step?.toolNames?.lastOrNull().orEmpty(),
            lastToolOutput = lastToolOutput,
            pendingApproval = if (phaseForDisplay.isWaiting) "Freigabe im Plan erteilen" else "",
            waitingSinceMillis = waitingSinceMillis,
            failedToolCalls = state.steps.count { it.status == AgentRunStatus.FAILED }
        )
    }
}