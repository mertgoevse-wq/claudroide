package org.claudroide.app

import org.claudroide.app.feature.agent.DependencyDescriptor
import org.claudroide.app.feature.agent.DependencyEvidence
import org.claudroide.app.feature.agent.DependencyInstallGate
import org.claudroide.app.feature.agent.DependencyLicence
import org.claudroide.app.feature.agent.DeviceStorage
import org.claudroide.app.feature.agent.InstallDecision
import org.claudroide.app.feature.agent.InstallOutcome
import org.claudroide.app.feature.agent.InstallState
import org.claudroide.app.feature.agent.PackageOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Task 116 — "install project tools" (Projektwerkzeuge installieren).
 *
 * The completion conditions ask that download and installation be confirmable
 * **individually** and that errors not hide a half-finished state. Both are
 * tested here as properties of the types, and the second one is tested in the
 * direction that matters: the state that *looks* finished from the outside.
 */
class RuntimeDependencyInstallTest {

    // ── Condition 1: download and installation are confirmed separately ────

    @Test
    fun `ohne Bestaetigung wird weder heruntergeladen noch installiert`() {
        val descriptor = erlaubtesPaket()

        val entscheidung = DependencyInstallGate.mayProceed(
            descriptor = descriptor,
            storage = genugSpeicher()
        )

        assertTrue(entscheidung is InstallDecision.Refused)
    }

    @Test
    fun `eine Download-Bestaetigung allein installiert noch nichts`() {
        val descriptor = erlaubtesPaket()

        val entscheidung = DependencyInstallGate.mayProceed(
            descriptor = descriptor,
            storage = genugSpeicher(),
            userConfirmedDownload = true
        )

        assertTrue(
            "Wer den Download erlaubt, hat die Installation nicht erlaubt.",
            entscheidung is InstallDecision.Refused
        )
        assertTrue(
            (entscheidung as InstallDecision.Refused).reason.contains("nicht bestätigt")
        )
    }

    @Test
    fun `eine Installations-Bestaetigung allein laesst den Download offen`() {
        val descriptor = erlaubtesPaket()

        val entscheidung = DependencyInstallGate.mayProceed(
            descriptor = descriptor,
            storage = genugSpeicher(),
            userConfirmedInstall = true
        )

        assertTrue(
            "Wer die Installation erlaubt, hat den Download nicht erlaubt.",
            entscheidung is InstallDecision.Refused
        )
    }

    @Test
    fun `beide Bestaetigungen zusammen lassen die Installation zu`() {
        val entscheidung = DependencyInstallGate.mayProceed(
            descriptor = erlaubtesPaket(),
            storage = genugSpeicher(),
            userConfirmedDownload = true,
            userConfirmedInstall = true
        )

        assertTrue(
            "Zwei Bestaetigungen und ein erlaubtes Paket muessen durchlassen.",
            entscheidung is InstallDecision.MayInstall
        )
    }

    // ── The explanation: name, origin, licence, size, network, removal ────

    @Test
    fun `die Erklaerung nennt Paket, Herkunft, Lizenz, Groesse und Netzwerk`() {
        val text = erlaubtesPaket().explanationLines().joinToString("\n")

        assertTrue(text.contains("gradle-wrapper"))
        assertTrue(text.contains("Herkunft:"))
        assertTrue(text.contains("Lizenz:"))
        assertTrue(text.contains("Download:"))
        assertTrue(text.contains("Entpackt:"))
        assertTrue(text.contains("Netzwerk:"))
    }

    @Test
    fun `die Erklaerung nennt den entpackten Platz als den, der wirklich zaehlt`() {
        val descriptor = erlaubtesPaket().copy(
            downloadBytes = 5L * 1024 * 1024,
            unpackedBytes = 120L * 1024 * 1024
        )

        val text = descriptor.explanationLines().joinToString("\n")

        assertTrue(
            "Der Download ist kleiner als der entpackte Platz; nur letzterer entscheidet.",
            text.contains("120.0 MB")
        )
        assertTrue(text.contains("das ist der Platz, der wirklich gebraucht wird"))
    }

    @Test
    fun `die Faehigkeiten werden einzeln genannt statt als Anzahl`() {
        val descriptor = erlaubtesPaket().copy(
            capabilities = listOf("Netzwerkzugriff", "Dateien ausserhalb des Projekts lesen")
        )

        val text = descriptor.explanationLines().joinToString("\n")

        assertTrue(text.contains("Netzwerkzugriff"))
        assertTrue(text.contains("Dateien ausserhalb des Projekts lesen"))
    }

    // ── The protection: nothing from an arbitrary source ───────────────────

    @Test
    fun `ein Paket aus ungepruefter Herkunft wird abgewiesen`() {
        val ungeprueft = erlaubtesPaket().copy(origin = PackageOrigin.UNVERIFIED)

        val entscheidung = DependencyInstallGate.mayProceed(
            descriptor = ungeprueft,
            storage = genugSpeicher(),
            userConfirmedDownload = true,
            userConfirmedInstall = true
        )

        assertTrue(
            "Aus einer beliebigen Quelle wird auch mit zwei Bestaetigungen nichts installiert.",
            entscheidung is InstallDecision.Refused
        )
    }

    @Test
    fun `ein mitgeliefertes Paket ohne passende Lizenz wird abgewiesen`() {
        val falscheLizenz = erlaubtesPaket().copy(
            origin = PackageOrigin.BUNDLED,
            licence = DependencyLicence.PROPRIETARY
        )

        val entscheidung = DependencyInstallGate.mayProceed(
            descriptor = falscheLizenz,
            storage = genugSpeicher(),
            userConfirmedDownload = true,
            userConfirmedInstall = true
        )

        assertTrue(entscheidung is InstallDecision.Refused)
    }

    @Test
    fun `eine ungeprueftes Paket aus beliebiger Quelle laesst sich nicht ueberstimmen`() {
        assertFalse(
            "Eine Herkunftspruefung ohne Aufhebung ist eine Pruefung.",
            DependencyInstallGate.canOverride(PackageOrigin.UNVERIFIED)
        )
        assertFalse(DependencyInstallGate.canOverride(PackageOrigin.PLATFORM))
    }

    @Test
    fun `zu wenig freier Speicher wird abgewiesen, obwohl bestaetigt wurde`() {
        val winzig = DeviceStorage(freeBytes = 1024)

        val entscheidung = DependencyInstallGate.mayProceed(
            descriptor = erlaubtesPaket(),
            storage = winzig,
            userConfirmedDownload = true,
            userConfirmedInstall = true
        )

        assertTrue(
            "Zwei Bestaetigungen schaffen keinen Speicher.",
            entscheidung is InstallDecision.Refused
        )
    }

    @Test
    fun `der Platzbedarf wird vom entpackten Wert berechnet, nicht vom Download`() {
        // 5 MB Download, 300 MB entpackt, 100 MB frei: nach dem Downloadwert
        // waere das genug, nach dem entpackten nicht.
        val descriptor = erlaubtesPaket().copy(
            downloadBytes = 5L * 1024 * 1024,
            unpackedBytes = 300L * 1024 * 1024
        )

        val entscheidung = DependencyInstallGate.mayProceed(
            descriptor = descriptor,
            storage = DeviceStorage(freeBytes = 100L * 1024 * 1024),
            userConfirmedDownload = true,
            userConfirmedInstall = true
        )

        assertTrue(
            "Der Downloadwert darf nicht als Platzbedarf durchgehen.",
            entscheidung is InstallDecision.Refused
        )
    }

    // ── Condition 2: a half-finished install is never called done ─────────

    @Test
    fun `Dateien ohne lauffaehiges Werkzeug ist ein halber Zustand`() {
        val zustand = InstallState(
            packageName = "gradle-wrapper",
            filesPresent = true,
            toolRunnable = false
        )

        assertEquals(InstallOutcome.PARTIAL, zustand.outcome)
        assertFalse("Ein halber Zustand ist kein Erfolg.", zustand.outcome == InstallOutcome.INSTALLED)
    }

    @Test
    fun `der halbe Zustand benennt im ersten Satz, was nicht geht`() {
        val zustand = InstallState("gradle-wrapper", filesPresent = true, toolRunnable = false)

        val zeilen = zustand.displayLines()
        val ersteZeile = zeilen.first()

        assertTrue(
            "Der erste Satz muss mit dem Defizit beginnen, nicht mit dem Ordner: $ersteZeile",
            ersteZeile.contains("nicht lauffähig")
        )
        assertTrue(
            "Der Zustand muss sich selbst als unfertig benennen.",
            zeilen.any { it.contains("kein fertiger Zustand") }
        )
    }

    @Test
    fun `Dateien und lauffaehiges Werkzeug ist ein fertiger Zustand`() {
        val zustand = InstallState("gradle-wrapper", filesPresent = true, toolRunnable = true)

        assertEquals(InstallOutcome.INSTALLED, zustand.outcome)
    }

    @Test
    fun `ohne Dateien ist nichts installiert`() {
        val zustand = InstallState("gradle-wrapper", filesPresent = false, toolRunnable = false)

        assertEquals(InstallOutcome.NOT_INSTALLED, zustand.outcome)
    }

    @Test
    fun `ein Fehler schlaegt eine teilweise Anwesenheit`() {
        val zustand = InstallState(
            packageName = "gradle-wrapper",
            filesPresent = true,
            toolRunnable = false,
            errorMessage = "Archiv unvollstaendig"
        )

        assertEquals(
            "Ein Fehler muss sichtbar sein, auch wenn Dateien liegen.",
            InstallOutcome.FAILED,
            zustand.outcome
        )
        assertTrue(zustand.displayLines().any { it.contains("Archiv unvollstaendig") })
    }

    @Test
    fun `ein lauffaehiges Werkzeug ohne Dateien ist unmoeglich`() {
        try {
            InstallState("gradle-wrapper", filesPresent = false, toolRunnable = true)
            throw AssertionError("Ein unmoeglicher Zustand wurde zugelassen.")
        } catch (expected: IllegalArgumentException) {
            // Genau so soll es sein.
        }
    }

    // ── The scaffold installs nothing ─────────────────────────────────────

    @Test
    fun `das Geraest nutzt weder Netz noch Dateisystem noch Prozess`() {
        val verboten = listOf(
            "java/io/File",
            "java/io/InputStream",
            "java/nio/file",
            "java/net/",
            "okhttp3",
            "retrofit2",
            "ProcessBuilder",
            "java/lang/Runtime",
            "HttpURLConnection",
            "android/util/Log"
        )

        val quelle = javaClass.getResourceAsStream(
            "/" + DependencyInstallGate::class.java.name.replace('.', '/') + ".class"
        )?.readBytes()?.toString(Charsets.ISO_8859_1)
            ?: error("Die kompilierte Klasse wurde nicht gefunden.")

        verboten.forEach { typ ->
            assertFalse("Das Geraest darf '$typ' nicht benutzen.", quelle.contains(typ))
        }
    }

    @Test
    fun `es wurde auf keinem Geraet etwas installiert, und das steht als Wert da`() {
        assertFalse(
            "Diese Datei darf keinen Geraetelauf behaupten.",
            DependencyEvidence.INSTALL_OBSERVED_ON_DEVICE
        )
        assertTrue(
            DependencyEvidence.statementLines().any { it.contains("Nicht geprüft") }
        )
    }

    @Test
    fun `das Tor haelt keinen Zustand zwischen zwei Aufrufen`() {
        val typ = DependencyInstallGate::class.java

        assertTrue(
            typ.declaredFields.any { it.name == "INSTANCE" && Modifier.isStatic(it.modifiers) }
        )
        assertTrue(
            typ.declaredFields.none { f ->
                !Modifier.isStatic(f.modifiers) || !Modifier.isFinal(f.modifiers)
            }
        )
    }

    @Test
    fun `ein Paket ohne Namen oder mit negativer Groesse ist unmoeglich`() {
        listOf<() -> DependencyDescriptor>(
            { erlaubtesPaket().copy(packageName = " ") },
            { erlaubtesPaket().copy(version = "") },
            { erlaubtesPaket().copy(downloadBytes = -1) },
            { erlaubtesPaket().copy(unpackedBytes = -1) }
        ).forEach { bauen ->
            try {
                bauen()
                throw AssertionError("Ein unmoegliches Paket wurde zugelassen.")
            } catch (expected: IllegalArgumentException) {
                // Genau so soll es sein.
            }
        }
    }

    // ── Fixtures ──────────────────────────────────────────────────────────

    private fun genugSpeicher() = DeviceStorage(freeBytes = 8L * 1024 * 1024 * 1024)

    private fun erlaubtesPaket() = DependencyDescriptor(
        packageName = "gradle-wrapper",
        version = "8.7",
        origin = PackageOrigin.KNOWN_LICENCE,
        licence = DependencyLicence.APACHE_2,
        downloadBytes = 5L * 1024 * 1024,
        unpackedBytes = 40L * 1024 * 1024,
        needsNetwork = true
    )
}