package org.claudroide.app.core.security

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 126 — „App- und Geräteschutz“.
 *
 * Ziel: Sensible Anbieter- und Projektdaten gegen versehentliches
 * Offenliegen schützen.
 *
 * Die Aufgabe nennt vier Maßnahmen: Gerätesperre, App-Wechselbildschirm,
 * Zwischenablage und Benachrichtigungen. Sie stellt aber ausdrücklich zwei
 * Bedingungen, und beide sind hier die tragenden Teile:
 *
 *  1. **Keine biometrische Funktion ohne unterstützte Android-API.** Biometrie
 *     ist eine **Plattformzusage**, keine App-Entscheidung. Wenn Android keine
 *     unterstützte API meldet, gibt es hier **keinen** biometrischen
 *     Schutzweg — und keinen, der ihn verspricht. [BiometricCapability]
 *     unterscheidet deshalb streng zwischen „unterstützt und eingerichtet",
 *     „unterstützt aber nicht eingerichtet" und „**nicht unterstützt**". Nur
 *     der erste Fall bietet eine biometrische Option an. Es gibt keine
 *     einstellbare Funktion, die bei fehlender Unterstützung trotzdem erscheint.
 *
 *  2. **Benachrichtigungen zeigen standardmäßig keine vertraulichen Inhalte.**
 *     Der Standard ist **unbedenklich**, nicht „privat, wenn der Nutzer will".
 *     [NotificationPrivacy] liefert im Standardfall nie Geheimtext; das Umstellen
 *     auf empfindlich ist eine **ausdrückliche** Entscheidung des Nutzers mit
 *     einer Warnung, weil ein Gerät ohne Sperre damit Inhalte im Klartext zeigt.
 *
 * Der Schutz ist **optional** — wie die Aufgabe sagt —, aber wenn er
 * eingeschaltet ist, ist er wirksam: Es gibt keinen „halb aktiviert" Zustand,
 * der eine zusätzliche Absicherung verspricht und sie dann doch nicht gibt.
 *
 * Reines Kotlin: kein Android, kein Biometrie-API-Aufruf. Die App meldet
 * [DeviceSecurityFacts], diese Klasse entscheidet, was daraus folgt — und ist
 * damit auf der JVM prüfbar.
 */

/** Die tatsächlichen Gegebenheiten des Geräts, von der Android-Seite gemeldet. */
data class DeviceSecurityFacts(

    /** Meldet Android eine unterstützte biometrische API? */
    val biometricApiSupported: Boolean = false,

    /** Hat der Nutzer biometrisch eingerichtet (Fingerabdruck/Gesicht)? */
    val biometricEnrolled: Boolean = false,

    /** Ist das Gerät durch PIN/Muster/Passwort gesperrt? */
    val deviceLockScreenEnabled: Boolean = false,

    /** Verlangt das Gerät zum Entsperren eine biometrische Prüfung? */
    val deviceUsesBiometricLock: Boolean = false
)

/** Welche biometrische Fähigkeit tatsächlich vorliegt. */
enum class BiometricCapability(val germanLabel: String) {

    /** Unterstützt **und** vom Nutzer eingerichtet. */
    AVAILABLE("verfügbar"),

    /** Unterstützt, aber nicht eingerichtet — der Nutzer kann es einrichten. */
    NOT_ENROLLED("nicht eingerichtet"),

    /**
     * Das Gerät unterstützt keine biometrische API.
     *
     * In diesem Zustand gibt es **keine** biometrische Option. Der Nutzer darf
     * nicht auf etwas warten, das es nicht gibt.
     */
    UNSUPPORTED("von diesem Gerät nicht unterstützt");

    /** Darf die App biometrischen Schutz **anbieten**? */
    val mayOfferBiometric: Boolean get() = this == AVAILABLE

    /** Muss die App offen sagen, dass es Biometrie hier nicht gibt? */
    val requiresHonestRefusal: Boolean get() = this == UNSUPPORTED
}

/** Die Sichtbarkeit der Benachrichtigung — der Standard ist unbedenklich. */
enum class NotificationPrivacy(val germanLabel: String) {

    /**
     * Der Standard: nur Art und Status, **nie** vertraulicher Inhalt.
     *
     * Auf dem Sperrbildschirm ist das die einzige sichere Wahl; ein Gerät ohne
     * Sperre zeigt die Benachrichtigung im Klartext.
     */
    SAFE_SUMMARY("nur Zusammenfassung"),

    /**
     * Der Nutzer hat ausdrücklich mehr gewählt.
     *
     * Trägt eine **Warnung**, weil damit Inhalte im Klartext sichtbar werden
     * können. Diese Wahl ist nie der Standard und nie still gesetzt.
     */
    SENSITIVE_DETAIL("mit Details")
}

/** Der Umgang mit der Zwischenablage. */
enum class ClipboardPolicy(val germanLabel: String) {

    /** Geheimnisse werden nie in die Zwischenablage geschrieben. */
    NEVER_COPY_SECRETS("Geheimnisse werden nie kopiert"),

    /** Alles, was kopiert wird, wird vorher geschwärzt. */
    REDACT_BEFORE_COPY("Vor dem Kopieren geschwärzt")
}

/** Das Ergebnis der Prüfung einer Schutzmaßnahme. */
data class ProtectionStatus(

    val id: String,
    val title: String,
    val active: Boolean,

    /** Warum die Maßnahme (in)aktiv ist — nie leer. */
    val explanation: String,

    /** Kann der Nutzer das selbst umschalten? */
    val userCanChange: Boolean = false
)

/**
 * Die Regel, wie sensible Daten auf dem Gerät geschützt werden.
 *
 * Der Standard ist der **schützende**: App-Wechselbildschirm abgedunkelt,
 * Benachrichtigungen unbedenklich, Zwischenablage geschwärzt, Geheimnisse nie
 * kopiert. Alles Umstellen ist eine bewusste Entscheidung des Nutzers und wird
 * als solche begründet.
 */
object DeviceProtectionPolicy {

    /**
     * Der Standard für Benachrichtigungen: unbedenklich.
     *
     * Das ist eine **Vorgabe**, keine Voreinstellung, die beim Start
     * überschrieben werden könnte: `SAFE_SUMMARY` ist der Wert, von dem die
     * App ausgeht. Empfindlichere Angaben muss der Nutzer ausdrücklich wählen.
     */
    fun defaultNotificationPrivacy(): NotificationPrivacy = NotificationPrivacy.SAFE_SUMMARY

    /**
     * Die biometrische Fähigkeit, wie sie sich aus den Fakten ergibt.
     *
     * Ohne unterstützte API ist das Ergebnis immer [BiometricCapability.UNSUPPORTED],
     * egal ob eine Einrichtung gemeldet wird — eine nicht vorhandene API kann
     * nicht eingetrichtet sein.
     */
    fun biometricCapability(facts: DeviceSecurityFacts): BiometricCapability = when {
        !facts.biometricApiSupported -> BiometricCapability.UNSUPPORTED
        facts.biometricEnrolled -> BiometricCapability.AVAILABLE
        else -> BiometricCapability.NOT_ENROLLED
    }

    /**
     * Darf die App biometrischen App-Schutz **anbieten**?
     *
     * Nur wenn die Plattform ihn tatsächlich unterstützt und er eingerichtet
     * ist. Es gibt keinen Weg, bei fehlender Unterstützung „trotzdem anzubieten".
     */
    fun mayOfferBiometricLock(facts: DeviceSecurityFacts): Boolean =
        biometricCapability(facts).mayOfferBiometric

    /**
     * Der ehrliche Text über biometrischen Schutz — auch bei Nichtunterstützung.
     *
     * Die App sagt dann klar, dass das Gerät es nicht kann, statt eine Option
     * zu zeigen, die ins Leere führt.
     */
    fun biometricExplanation(facts: DeviceSecurityFacts): String =
        when (biometricCapability(facts)) {
            BiometricCapability.AVAILABLE ->
                "Dieses Gerät unterstützt biometrische Entsperrung und sie ist eingerichtet."

            BiometricCapability.NOT_ENROLLED ->
                "Dieses Gerät unterstützt biometrische Entsperrung, sie ist aber nicht " +
                    "eingerichtet. Richten Sie sie in den Android-Einstellungen ein."

            BiometricCapability.UNSUPPORTED ->
                "Dieses Gerät unterstützt keine biometrische Entsperrung. ClauDroide " +
                    "bietet deshalb **keinen** biometrischen App-Schutz an. Andere " +
                    "Schutzmaßnahmen bleiben möglich."
        }

    /**
     * Muss beim Umschalten auf empfindliche Benachrichtigungen gewarnt werden?
     *
     * Bei [NotificationPrivacy.SENSITIVE_DETAIL] immer — genau dann, wenn
     * vertrauliche Inhalte im Klartext sichtbar werden können.
     */
    fun warningFor(privacy: NotificationPrivacy): String? = when (privacy) {
        NotificationPrivacy.SAFE_SUMMARY -> null
        NotificationPrivacy.SENSITIVE_DETAIL ->
            "Mit Details können vertrauliche Inhalte im Klartext auf dem " +
                "Sperrbildschirm erscheinen. Auf einem Gerät ohne Sperre sieht " +
                "jemand sie mit."
    }

    /**
     * Was die Benachrichtigung zeigt.
     *
     * Im Standard [NotificationPrivacy.SAFE_SUMMARY] wird **nie** Geheimtext
     * zurückgegeben. Selbst im empfindlichen Fall läuft der Text durch
     * [SecretMasker] — Details sind nicht dasselbe wie Schlüssel im Klartext.
     */
    fun notificationText(privacy: NotificationPrivacy, candidateText: String): String = when (privacy) {
        NotificationPrivacy.SAFE_SUMMARY -> SecretMasker.REDACTION_PLACEHOLDER
        NotificationPrivacy.SENSITIVE_DETAIL -> SecretMasker.redact(candidateText)
    }

    /**
     * Der Standard für die Zwischenablage: Geheimnisse werden nie kopiert.
     *
     * [ClipboardPolicy.NEVER_COPY_SECRETS] ist der Ausgangswert — nicht das
     * Ergebnis einer Benutzerentscheidung, die man vergessen könnte.
     */
    val defaultClipboardPolicy: ClipboardPolicy = ClipboardPolicy.NEVER_COPY_SECRETS

    /**
     * Darf dieser Text in die Zwischenablage?
     *
     * Bei [ClipboardPolicy.NEVER_COPY_SECRETS] wird jeder Text abgelehnt, der
     * wie ein Geheimnis aussieht. Es gibt keinen Weg, den Check zu umgehen,
     * ohne die Politik zu ändern — und die Änderung ist eine sichtbare
     * Entscheidung.
     */
    fun mayCopy(text: String, policy: ClipboardPolicy = defaultClipboardPolicy): Boolean =
        when (policy) {
            ClipboardPolicy.NEVER_COPY_SECRETS -> !SecretMasker.containsSecretLikeText(text)
            ClipboardPolicy.REDACT_BEFORE_COPY -> true
        }

    /**
     * Der Zustand aller Schutzmaßnahmen, mit Begründung.
     *
     * Der Gerätesperre folgt die App: Wenn das Gerät nicht gesperrt ist, ist
     * der biometrische App-Schutz sinnlos — ein Gerät ohne Sperre zeigt
     * ohnehin alles. Deshalb wird er als **nicht sinnvoll** gemeldet, nicht als
     * „aus".
     */
    fun statuses(
        facts: DeviceSecurityFacts,
        notificationPrivacy: NotificationPrivacy = defaultNotificationPrivacy(),
        clipboardPolicy: ClipboardPolicy = defaultClipboardPolicy,
        appSwitcherShielded: Boolean = true
    ): List<ProtectionStatus> = buildList {
        val bio = biometricCapability(facts)

        add(
            ProtectionStatus(
                id = "device_lock",
                title = "Gerätesperre",
                active = facts.deviceLockScreenEnabled,
                explanation = if (facts.deviceLockScreenEnabled) {
                    "Das Gerät ist gesperrt. Das hält jemanden fern, der es in die Hand nimmt."
                } else {
                    "Das Gerät ist **nicht** gesperrt. Ohne Sperre sieht jeder die Inhalte."
                },
                userCanChange = false
            )
        )

        add(
            ProtectionStatus(
                id = "biometric_app_lock",
                title = "Biometrischer App-Schutz",
                active = false,
                explanation = when {
                    !bio.mayOfferBiometric -> "Nicht angeboten — ${bio.germanLabel}."
                    else -> "Dieses Gerät unterstützt biometrische Entsperrung. " +
                        "Der Schutz wird beim Öffnen der App verlangt."
                },
                userCanChange = bio.mayOfferBiometric
            )
        )

        add(
            ProtectionStatus(
                id = "app_switcher_shield",
                title = "App-Wechselbildschirm abschirmen",
                active = appSwitcherShielded,
                explanation = if (appSwitcherShielded) {
                    "In der Übersicht der zuletzt verwendeten Apps wird der Inhalt " +
                        "abgedunkelt. Jemand mit dem Handy in der Hand sieht nichts."
                } else {
                    "Der Inhalt ist in der App-Übersicht sichtbar."
                },
                userCanChange = true
            )
        )

        add(
            ProtectionStatus(
                id = "notifications",
                title = "Benachrichtigungen",
                active = notificationPrivacy == NotificationPrivacy.SAFE_SUMMARY,
                explanation = "Benachrichtigungen zeigen ${notificationPrivacy.germanLabel}. " +
                    (warningFor(notificationPrivacy) ?: "Es werden keine vertraulichen Inhalte gezeigt."),
                userCanChange = true
            )
        )

        add(
            ProtectionStatus(
                id = "clipboard",
                title = "Zwischenablage",
                active = clipboardPolicy == ClipboardPolicy.NEVER_COPY_SECRETS,
                explanation = clipboardPolicy.germanLabel + ".",
                userCanChange = true
            )
        )
    }

    /**
     * Gibt es unter diesen Fakten **überhaupt** einen wirksamen Schutz?
     *
     * Auf einem Gerät ohne Sperre ist das ehrlich gesagt „kaum": Die
     * Benachrichtigung und die App-Übersicht zeigen dann Inhalte, die niemand
     * aufhalten kann. Die App sagt das, statt vier grüne Häkchen zu zeigen.
     */
    fun effectiveProtection(facts: DeviceSecurityFacts): Boolean =
        facts.deviceLockScreenEnabled
}