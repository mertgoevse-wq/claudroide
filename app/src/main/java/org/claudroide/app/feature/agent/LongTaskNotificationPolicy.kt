package org.claudroide.app.feature.agent

import org.claudroide.app.core.platform.TaskLifecycleState

/**
 * Task 113 — „Lange Aufgabe melden“.
 *
 * Ziel: Der Nutzer erkennt und stoppt eine von ihm gestartete Aufgabe, auch
 * wenn die App gerade nicht im Vordergrund ist.
 *
 * Der Weg dorthin ist eine Benachrichtigung mit Stop-Aktion. Diese Klasse
 * entscheidet, **was** in dieser Benachrichtigung stehen darf und ob sie
 * überhaupt entsteht — sie postet nichts und startet keinen Dienst.
 *
 * Die vier Zusicherungen der Aufgabe sind strukturell verankert:
 *
 *  1. **Zielversion, Berechtigungen und Diensttyp werden geprüft.** [decide]
 *     bekommt die [PlatformFacts] (Zielversion, hat der Nutzer Benachrichtigungen
 *     erlaubt, Service-Typ) und liefert je danach ein Ergebnis. Ein Weg, diese
 *     Prüfung zu überspringen, existiert nicht: [build] verlangt sie als
 *     Parameter, und es gibt keine Variante, die ohne [PlatformFacts] eine
 *     fertige Benachrichtigung erzeugt.
 *
 *  2. **Keine privaten Inhalte auf dem Sperrbildschirm.** Der Inhalt geht
 *     durch [redactForLockScreen], bevor er in [NotificationPlan] landet. Auf
 *     dem gesperrten Bildschirm wird nur die **Art** der Aufgabe und ihr
 *     Zustand gezeigt — nie Chat-Text, Dateinamen, Befehle oder Schlüssel.
 *     Das ist eine Eigenschaft des Typs: [NotificationPlan.lockScreenText]
 *     kann nur aus [safeLabel] und dem Zustand gebaut werden, und es gibt
 *     kein Feld, das rohen Inhalt in die Benachrichtigung trägt.
 *
 *  3. **Kein dauerhaftes Hintergrundarbeiten ohne aktiven Nutzerauftrag.**
 *     Eine Benachrichtigung mit Stop-Aktion ist an einen [UserMandate] gebunden.
 *     Läuft der Auftrag ab oder wird er zurückgenommen ([UserMandate.isActiveAt]),
 *     ist die Benachrichtigung **nicht mehr gültig** — die Klasse liefert dann
 *     [NotificationPlan.none]. Es gibt keinen Pfad, der sie am Leben hält.
 *
 *  4. **Langsame oder unzuverlässige Abläufe werden erklärt, nicht beschönigt.**
 *     [NotificationPlan.uncertaintyNote] ist nicht leer, sobald die Aufgabe
 *     langsam oder unzuverlässig war. Eine ehrliche „unsicher"-Zeile ist der
 *     Normalfall, keine Ausnahme für besondere Fälle.
 *
 * Reines Kotlin: kein Android-Import, kein NotificationManager, kein Context.
 * Die Android-Seite (Kanal, Dienst, PendingIntent) baut die App; diese Klasse
 * ist die Entscheidung und damit auf der JVM prüfbar.
 */

/** Die Plattformumstände, die vor jeder Benachrichtigung geprüft werden. */
data class PlatformFacts(

    /** Zielversion der App (targetSdkVersion). Fakten aus dem Build, nicht geraten. */
    val targetSdk: Int,

    /** Zielversion des Geräts (Build.VERSION.SDK_INT). */
    val deviceSdk: Int,

    /** Hat der Nutzer Benachrichtigungen erlaubt? Wird bei jedem Aufruf neu geprüft. */
    val notificationsAllowed: Boolean,

    /** Läuft die Aufgabe in einem Vordergrunddienst (muss Android so verlangen)? */
    val requiresForegroundService: Boolean
) {
    companion object {
        /** Vertrag: die App zielt auf Android 15 (API 35), mindestens Android 8 (API 26). */
        const val APP_TARGET_SDK = 35
        const val APP_MIN_SDK = 26

        /** Ab Android 13 (API 33) muss der Nutzer Benachrichtigungen erlauben. */
        const val NOTIFICATION_PERMISSION_SDK = 33
    }
}

/** Der Nutzerauftrag, an dem eine lange Aufgabe hängt. */
data class UserMandate(

    /** Wann der Nutzer die Aufgabe angestoßen hat. */
    val startedAtMillis: Long,

    /** Wann der Auftrag abläuft. `null` = ohne Ablauf. */
    val expiresAtMillis: Long?,

    /** Eindeutige Kennung der Sitzung, aus der der Auftrag stammt. */
    val sessionId: String,

    /** Der Auftrag wurde zurückgenommen (z. B. weil der Nutzer abgebrochen hat). */
    val withdrawn: Boolean = false
) {
    /**
     * Ist der Auftrag zu [nowMillis] in dieser Sitzung noch aktiv?
     *
     * Gerechnet wird bei jedem Aufruf neu — Uhr und Session, nicht ein
     * gespeichertes „läuft noch“. Eine **rückwärts gelaufene** Uhr gilt als
     * abgelaufen: Geräteuhren wandern, und die sichere Lesart von „jetzt liegt
     * vor dem Beginn" ist, dass die Uhr nicht belastbar ist.
     */
    fun isActiveAt(nowMillis: Long, currentSessionId: String): Boolean {
        if (withdrawn) return false
        if (sessionId != currentSessionId) return false
        if (nowMillis < startedAtMillis) return false
        val ende = expiresAtMillis ?: return true
        return nowMillis < ende
    }
}

/** Wie gut sich der Zustand einer Aufgabe belegen lässt. */
enum class Certainty(val germanLabel: String) {
    /** Belegt: der Zustand ist gesichert. */
    PROVEN("belegt"),

    /** Die Aufgabe lief, aber ihr Ergebnis ist nicht gesichert (z. B. ungewisser Abbruch). */
    UNCERTAIN("unsicher"),

    /** Zu langsam oder zu unzuverlässig, um etwas zu behaupten. */
    TOO_SLOW("zu langsam oder unzuverlässig")
}

/** Das Ergebnis der Prüfung von Plattform und Auftrag. */
sealed interface NotificationGate {

    /** Vor der Android-Seite: Alles in Ordnung, die Benachrichtigung darf entstehen. */
    data class Allowed(val facts: PlatformFacts) : NotificationGate

    /** Android verlangt eine Benachrichtigungs-Berechtigung, die fehlt. */
    data class PermissionMissing(val explanationLines: List<String>) : NotificationGate

    /** Kein aktiver Nutzerauftrag — die Benachrichtigung wäre dauerhafte Hintergrundausführung. */
    data class NoActiveMandate(val explanationLines: List<String>) : NotificationGate

    /** Die Aufgabe ist kürzer als gemeldet; eine Benachrichtigung ist unnötig. */
    data class NotLongEnough(val elapsedMillis: Long, val thresholdMillis: Long) : NotificationGate
}

/** Die fertige, bereits entschärfte Benachrichtigung. */
data class NotificationPlan(

    /** Kurzer, unbedenklicher Titel — steht auch auf dem Sperrbildschirm. */
    val title: String,

    /** Zeile für das Sperrbildschirm-Format. Enthält nie privaten Inhalt. */
    val lockScreenText: String,

    /** Zeile für den entsperrten Bildschirm (dort darf mehr stehen, aber nichts Geheimes). */
    val detailText: String,

    /** Der Zustand der Aufgabe in Klartext. */
    val statusLine: String,

    /** Wie belastbar der Zustand ist. */
    val certainty: Certainty,

    /** Gibt es eine Stop-Aktion? Eine lange Aufgabe **muss** stoppbar sein. */
    val showStopAction: Boolean,

    /** Muss die Aufgabe als Vordergrunddienst laufen? */
    val requiresForegroundService: Boolean,

    /** Ehrliche Zeile, wenn die Ausführung langsam oder unzuverlässig war. */
    val uncertaintyNote: String
) {
    /** Darf private Inhalte in die Benachrichtigung? Nie — die App hält das immer ein. */
    val mayIncludePrivateContent: Boolean = false
}

/** Der Zustand einer gemeldeten langen Aufgabe. */
data class LongTaskReport(
    val state: TaskLifecycleState,

    /** Wie lange die Aufgabe bereits lief. */
    val elapsedMillis: Long,

    /** Wie belastbar der Zustand ist. */
    val certainty: Certainty,

    /**
     * Die **Art** der Aufgabe in unbedenklicher Form („Agentenlauf", „Projektübersicht").
     * Das ist bewusst eine Kategorie, kein Titel und kein Inhalt — sie steht so
     * auf dem Sperrbildschirm.
     */
    val safeLabel: String,

    /** Optionale private Detailzeile — sie landet **nie** auf dem Sperrbildschirm. */
    val privateDetail: String? = null
)

/**
 * Die Regel, wann und was eine lange Aufgabe meldet.
 *
 * @property thresholdMillis ab wann eine Aufgabe als „lang" gilt. Eine
 *        Projektvorgabe, **keine Messung auf dem A56** — und als solche benannt.
 */
class LongTaskNotificationPolicy(
    private val thresholdMillis: Long = DEFAULT_LONG_TASK_MILLIS
) {

    companion object {
        /** Ab zwei Minuten gilt eine Aufgabe in dieser App als lang. Projektvorgabe. */
        const val DEFAULT_LONG_TASK_MILLIS: Long = 120_000L
    }

    /**
     * Prüft, ob eine Benachrichtigung überhaupt entstehen darf.
     *
     * Die Reihenfolge ist die Aussage: erst Berechtigung, dann Auftrag, dann
     * Schwelle. Fehlt die Berechtigung, ist die Frage nach dem Auftrag
     * irrelevant — es würde nichts anzeigen. Und ohne Auftrag darf es keine
     * Meldung geben, weil genau das die dauerhafte Hintergrundausführung wäre.
     */
    fun gate(report: LongTaskReport, mandate: UserMandate, facts: PlatformFacts, nowMillis: Long, currentSessionId: String): NotificationGate = when {
        facts.deviceSdk >= PlatformFacts.NOTIFICATION_PERMISSION_SDK && !facts.notificationsAllowed ->
            NotificationGate.PermissionMissing(
                listOf(
                    "Benachrichtigungen sind nicht erlaubt. Die App kann melden, " +
                        "dass eine Aufgabe läuft, aber nur, wenn der Nutzer das erlaubt.",
                    "Ohne diese Erlaubnis läuft die Aufgabe trotzdem — sie wird nur " +
                        "nicht angekündigt."
                )
            )

        !mandate.isActiveAt(nowMillis, currentSessionId) ->
            NotificationGate.NoActiveMandate(
                listOf(
                    "Es besteht kein aktiver Nutzerauftrag für diese Aufgabe. Eine " +
                        "Meldung darüber wäre Hintergrundausführung ohne Auftrag.",
                    "Die Aufgabe wird nur gemeldet, solange der Nutzer sie selbst " +
                        "gestartet hat."
                )
            )

        report.elapsedMillis < thresholdMillis ->
            NotificationGate.NotLongEnough(report.elapsedMillis, thresholdMillis)

        else -> NotificationGate.Allowed(facts)
    }

    /**
     * Baut die Benachrichtigung.
     *
     * Verlangt das [gate] kein [NotificationGate.Allowed], liefert diese
     * Funktion [null] — sie kann eine Benachrichtigung also **nicht** bauen,
     * wenn die Prüfung sie verboten hat. Es gibt keinen Umweg, weil die
     * Benachrichtigung ohne die Prüfung nicht gebaut werden kann.
     */
    fun build(gate: NotificationGate, report: LongTaskReport): NotificationPlan? {
        val facts = (gate as? NotificationGate.Allowed)?.facts ?: return null
        return NotificationPlan(
            title = safeTitle(report.safeLabel),
            // Auf dem Sperrbildschirm: nur Art + Zustand. Kein privater Inhalt,
            // kein Dateiname, kein Befehl — das ist der ganze Zweck dieser Zeile.
            lockScreenText = "${report.safeLabel}: ${statusFor(report.state)}",
            detailText = detailFor(report),
            statusLine = statusFor(report.state),
            certainty = report.certainty,
            // Eine lange Aufgabe muss stoppbar sein — außer sie ist schon vorbei.
            // Das ist aus dem Zustand **abgeleitet**, nicht angegeben.
            showStopAction = report.state.isCancellable(),
            requiresForegroundService = facts.requiresForegroundService,
            uncertaintyNote = uncertaintyFor(report)
        )
    }

    /**
     * Prüft und baut in einem Schritt.
     *
     * @return die Benachrichtigung, oder `null`, wenn sie nicht entstehen darf.
     */
    fun decide(report: LongTaskReport, mandate: UserMandate, facts: PlatformFacts, nowMillis: Long, currentSessionId: String): NotificationPlan? =
        build(gate(report, mandate, facts, nowMillis, currentSessionId), report)

    private fun safeTitle(safeLabel: String): String =
        "ClauDroide arbeitet: $safeLabel"

    private fun statusFor(state: TaskLifecycleState): String = when (state) {
        TaskLifecycleState.IDLE -> "wartet auf Start"
        TaskLifecycleState.RUNNING -> "läuft"
        TaskLifecycleState.PAUSED -> "pausiert"
        TaskLifecycleState.CANCELLED -> "abgebrochen"
        TaskLifecycleState.FAILED -> "fehlgeschlagen"
        TaskLifecycleState.COMPLETED -> "beendet"
    }

    private fun detailFor(report: LongTaskReport): String = buildString {
        append(statusFor(report.state))
        append(" · ")
        append(elapsedText(report.elapsedMillis))
        // Auf dem **entsperrten** Bildschirm darf die private Zeile stehen —
        // aber sie ist nie Teil von lockScreenText.
        report.privateDetail?.let { append(" · $it") }
    }

    private fun elapsedText(millis: Long): String {
        val sekunden = millis / 1000
        return when {
            sekunden < 60 -> "${sekunden}s"
            sekunden < 3600 -> "${sekunden / 60} min"
            else -> "${sekunden / 3600} h ${(sekunden % 3600) / 60} min"
        }
    }

    /**
     * Die ehrliche Zeile zu Belastbarkeit. Ein leeres Ergebnis heißt „belegt"
     * — und das ist der **einzige** Fall ohne Zusatzzeile.
     */
    private fun uncertaintyFor(report: LongTaskReport): String = when (report.certainty) {
        Certainty.PROVEN -> ""
        Certainty.UNCERTAIN ->
            "Das Ergebnis ist noch nicht gesichert — die Aufgabe wurde unterbrochen, " +
                "bevor feststand, ob etwas geschrieben wurde."
        Certainty.TOO_SLOW ->
            "Die Aufgabe läuft langsam oder unzuverlässig. Sie meldet ihren Zustand " +
                "vielleicht nicht rechtzeitig — bitte in der App prüfen."
    }

    /** Läuft die Aufgabe noch und lässt sich stoppen? */
    private fun TaskLifecycleState.isCancellable(): Boolean =
        this == TaskLifecycleState.RUNNING || this == TaskLifecycleState.PAUSED
}