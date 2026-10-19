package org.claudroide.app

import org.claudroide.app.core.security.BiometricCapability
import org.claudroide.app.core.security.ClipboardPolicy
import org.claudroide.app.core.security.DeviceProtectionPolicy
import org.claudroide.app.core.security.DeviceSecurityFacts
import org.claudroide.app.core.security.NotificationPrivacy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 126 — „App- und Geräteschutz“.
 *
 * Die Bedingung „keine biometrische Funktion ohne unterstützte Android-API"
 * steht an erster Stelle: Der Nutzer darf nie auf etwas warten, das es nicht
 * gibt.
 */
class DeviceProtectionPolicyTest {

    private fun facts(
        api: Boolean = false,
        eingetragen: Boolean = false,
        sperre: Boolean = true,
        bioSperre: Boolean = false
    ) = DeviceSecurityFacts(api, eingetragen, sperre, bioSperre)

    // ── Keine Biometrie ohne unterstützte API ──────────────────────────

    @Test
    fun `ohne unterstuetzte API gilt Biometrie als nicht unterstuetzt`() {
        val faehigkeit = DeviceProtectionPolicy.biometricCapability(facts(api = false))
        assertEquals(BiometricCapability.UNSUPPORTED, faehigkeit)
        assertFalse(DeviceProtectionPolicy.mayOfferBiometricLock(facts(api = false)))
    }

    @Test
    fun `unterstuetzt aber nicht eingerichtet ist nicht dasselbe wie nicht unterstuetzt`() {
        val faehigkeit = DeviceProtectionPolicy.biometricCapability(facts(api = true, eingetragen = false))
        assertEquals(BiometricCapability.NOT_ENROLLED, faehigkeit)
        assertFalse("Ohne Einrichtung wird kein biometrischer Schutz angeboten.", faehigkeit.mayOfferBiometric)
    }

    @Test
    fun `unterstuetzt und eingerichtet bietet Biometrie an`() {
        val faehigkeit = DeviceProtectionPolicy.biometricCapability(facts(api = true, eingetragen = true))
        assertEquals(BiometricCapability.AVAILABLE, faehigkeit)
        assertTrue(DeviceProtectionPolicy.mayOfferBiometricLock(facts(api = true, eingetragen = true)))
    }

    @Test
    fun `eine nicht unterstuetzte API kann nicht eingetragen sein`() {
        // Auch wenn fälschlich „eingetragen" gemeldet wird, bleibt das Ergebnis
        // „nicht unterstützt" — die API fehlt, die Einrichtung kann nicht helfen.
        val faehigkeit = DeviceProtectionPolicy.biometricCapability(facts(api = false, eingetragen = true))
        assertEquals(BiometricCapability.UNSUPPORTED, faehigkeit)
    }

    @Test
    fun `bei nicht unterstuetzter Biometrie sagt die App es ehrlich`() {
        val text = DeviceProtectionPolicy.biometricExplanation(facts(api = false))
        assertTrue(text.contains("keine biometrische Entsperrung"))
        assertTrue("Sie muss sagen, dass es keine Option gibt.", text.contains("keinen"))
    }

    @Test
    fun `bei nicht eingetragener Biometrie wird der Einrichtungshinweis genannt`() {
        val text = DeviceProtectionPolicy.biometricExplanation(facts(api = true, eingetragen = false))
        assertTrue(text.contains("nicht eingerichtet"))
    }

    // ── Benachrichtigungen: standardmäßig unbedenklich ────────────────

    @Test
    fun `der Standard zeigt keine vertraulichen Inhalte`() {
        val text = DeviceProtectionPolicy.notificationText(
            NotificationPrivacy.SAFE_SUMMARY,
            "Antwort über API-Schlüssel sk-ant-api03-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        )
        assertFalse(
            "Im Standard darf kein Geheimtext in der Benachrichtigung stehen.",
            text.contains("sk-ant")
        )
    }

    @Test
    fun `der Standard ist nicht verhandelbar durch eine Option`() {
        // Es gibt keinen Weg, den Standard zu überschreiben, ohne die
        // Entscheidung ausdrücklich zu treffen.
        assertTrue(
            NotificationPrivacy.SAFE_SUMMARY.germanLabel.contains("Zusammenfassung")
        )
        assertEquals(
            NotificationPrivacy.SAFE_SUMMARY,
            org.claudroide.app.core.security.DeviceProtectionPolicy.defaultNotificationPrivacy()
        )
    }

    @Test
    fun `empfindliche Benachrichtigungen werden gewarnt`() {
        val warnung = DeviceProtectionPolicy.warningFor(NotificationPrivacy.SENSITIVE_DETAIL)
        assertTrue(
            "Details auf dem Sperrbildschirm brauchen eine Warnung.",
            warnung!!.contains("Klartext")
        )
    }

    @Test
    fun `der unbedenkliche Standard braucht keine Warnung`() {
        assertNull(DeviceProtectionPolicy.warningFor(NotificationPrivacy.SAFE_SUMMARY))
    }

    @Test
    fun `auch im Detailfall werden Schluessel geschwaerzt`() {
        val text = DeviceProtectionPolicy.notificationText(
            NotificationPrivacy.SENSITIVE_DETAIL,
            "Verwendet sk-ant-api03-BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB im Projekt"
        )
        assertFalse(
            "Auch Details dürfen keinen Schlüssel im Klartext zeigen.",
            text.contains("sk-ant")
        )
    }

    // ── Zwischenablage ────────────────────────────────────────────────

    @Test
    fun `der Standard kopiert nie ein Geheimnis`() {
        assertFalse(
            DeviceProtectionPolicy.mayCopy("Hier steht sk-ant-api03-CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC"
            )
        )
    }

    @Test
    fun `der Standard kopiert gewoehnlichen Text`() {
        assertTrue(DeviceProtectionPolicy.mayCopy("Ein ganz gewöhnlicher Satz."))
    }

    @Test
    fun `der Standard der Zwischenablage ist das Sichere`() {
        assertEquals(
            ClipboardPolicy.NEVER_COPY_SECRETS,
            DeviceProtectionPolicy.defaultClipboardPolicy
        )
    }

    // ── Das Gerät ohne Sperre wird ehrlich dargestellt ────────────────

    @Test
    fun `ein Geraet ohne Sperre hat keinen wirksamen Schutz`() {
        assertFalse(DeviceProtectionPolicy.effectiveProtection(facts(sperre = false)))
    }

    @Test
    fun `ein gesperrtes Geraet hat wirksamen Schutz`() {
        assertTrue(DeviceProtectionPolicy.effectiveProtection(facts(sperre = true)))
    }

    @Test
    fun `der Geraetestatus wird ohne Sperre als inaktiv gemeldet`() {
        val ohneSperre = DeviceProtectionPolicy.statuses(facts(sperre = false))
            .first { it.id == "device_lock" }
        assertFalse(ohneSperre.active)
        assertTrue(ohneSperre.explanation.contains("nicht"))
    }

    // ── Der Wechselbildschirm ─────────────────────────────────────────

    @Test
    fun `der Wechselbildschirm wird standardmaessig abgeschirmt`() {
        val standard = DeviceProtectionPolicy.statuses(facts())
            .first { it.id == "app_switcher_shield" }
        assertTrue("Der Standard schützt den Inhalt.", standard.active)
    }

    @Test
    fun `abgeschirmter Wechselbildschirm wird als wirksam erklaert`() {
        val status = DeviceProtectionPolicy.statuses(facts(), appSwitcherShielded = true)
            .first { it.id == "app_switcher_shield" }
        assertTrue(status.explanation.contains("abgedunkelt"))
    }

    // ── Jede Maßnahme nennt ihren Grund ───────────────────────────────

    @Test
    fun `jede Schutzmassnahme nennt einen Grund`() {
        DeviceProtectionPolicy.statuses(facts()).forEach { status ->
            assertFalse("\"${status.title}\" braucht eine Begründung.", status.explanation.isBlank())
        }
    }

    @Test
    fun `alle vier genannten Massnahmen werden bewertet`() {
        val ids = DeviceProtectionPolicy.statuses(facts()).map { it.id }
        assertTrue("Gerätesperre", ids.contains("device_lock"))
        assertTrue("Biometrie", ids.contains("biometric_app_lock"))
        assertTrue("Wechselbildschirm", ids.contains("app_switcher_shield"))
        assertTrue("Benachrichtigungen", ids.contains("notifications"))
        assertTrue("Zwischenablage", ids.contains("clipboard"))
    }

    @Test
    fun `biometrischer Schutz wird nur dort als umschaltbar angeboten wo er existiert`() {
        val ohneApi = DeviceProtectionPolicy.statuses(facts(api = false, sperre = true))
            .first { it.id == "biometric_app_lock" }
        assertFalse("Ohne API darf es nichts zu Umschalten geben.", ohneApi.userCanChange)

        val mitApi = DeviceProtectionPolicy.statuses(facts(api = true, eingetragen = true))
            .first { it.id == "biometric_app_lock" }
        assertTrue(mitApi.userCanChange)
    }
}