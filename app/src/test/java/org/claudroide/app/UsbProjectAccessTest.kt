package org.claudroide.app

import org.claudroide.app.feature.project.ProbeObservation
import org.claudroide.app.feature.project.SlowWriteEvidence
import org.claudroide.app.feature.project.UsbBehaviour
import org.claudroide.app.feature.project.UsbEvidence
import org.claudroide.app.feature.project.UsbPathAvailability
import org.claudroide.app.feature.project.UsbProbe
import org.claudroide.app.feature.project.UsbVolume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Task 085 — "USB project access" (USB-Projektzugriff).
 *
 * The completion conditions are checked as properties of the values, not as
 * prose about them:
 *
 *  1. *"Only successfully tested USB paths are called available."* — the
 *     decisive test is [kein_USB_Weg_ist_verfuegbar_ohne_Geraetemessung]: the
 *     project's own constant says no probe has ever run, so a volume that
 *     looks perfect and one that looks broken are both refused. That is the
 *     condition under test, not a limitation worked around.
 *
 *  2. *"Slow or unreliable storage is explained."* — [Verhalten_ist_abgeleitet_
 *     nicht_vergeben] pins the behaviour to the numbers, and
 *     [ein_ungenannter_Zeitwert_wird_nicht_erfunden] pins the opposite case.
 *
 * The last test checks the compiled class against forbidden types, so "this
 * file touches no storage" is a fact about the bytecode rather than a promise
 * in a comment.
 */
class UsbProjectAccessTest {

    // ── Fixtures ────────────────────────────────────────────────────────────

    private fun volume(
        label: String = "USB-Laufwerk",
        root: String = "content://com.android.externalstorage.documents/tree/primary%3AUSB",
        removable: Boolean = true,
        filesystem: String? = "exfat"
    ): UsbVolume =
        UsbVolume.pickedByUser(
            label = label,
            rootUri = root,
            isRemovable = removable,
            reportedFilesystem = filesystem
        )

    private fun observation(
        read: Boolean = true,
        writes: Int = 5,
        slowest: Long? = 120L,
        errors: Int = 0
    ): ProbeObservation =
        ProbeObservation(
            readAnswered = read,
            writesCompleted = writes,
            slowestWriteMillis = slowest,
            writeErrors = errors
        )

    // ── Condition 1: only tested paths are available ────────────────────────

    @Test
    fun `kein USB-Weg ist verfuegbar ohne Geraetemessung`() {
        val laufwerk = volume()

        val perfekt = UsbProbe(
            volume = laufwerk,
            observation = observation(read = true, writes = 10, slowest = 5L, errors = 0)
        )

        assertFalse(
            "Ohne Geraetemessung wird kein Weg verfuegbar - auch kein idealer.",
            UsbPathAvailability.isUsable(laufwerk, perfekt)
        )
    }

    @Test
    fun `ohne jede Messung wird der Weg ebenfalls abgewiesen`() {
        val laufwerk = volume()

        assertFalse(
            "Ein gemuesslicher Lauf ohne Messung ist keine Messung.",
            UsbPathAvailability.isUsable(laufwerk, null)
        )
    }

    @Test
    fun `die Geraeteluecke ist ein Wert im Programm und kein Satz im Dokument`() {
        assertFalse(
            "Dieses Projekt hat nie auf einem Geraet gemessen; das steht hier als Wert.",
            UsbEvidence.PROBE_OBSERVED_ON_DEVICE
        )
        assertTrue(
            "Der Wert muss sich auch erklaeren koennen.",
            UsbEvidence.statementLines().any { it.contains("keine USB-Messung") }
        )
    }

    @Test
    fun `eine perfekt aussehende Erkennung allein macht den Weg nicht verfuegbar`() {
        val laufwerk = volume(removable = true, filesystem = "ntfs")

        val probe = UsbProbe(laufwerk, observation())

        assertFalse(
            "Wechseldatentraeger und Dateisystem sind eine Absicht, keine Messung.",
            UsbPathAvailability.isUsable(laufwerk, probe)
        )
    }

    // ── Condition 2: slow or unreliable storage is explained ────────────────

    @Test
    fun `Verhalten ist abgeleitet, nicht vergeben`() {
        assertEquals(
            UsbBehaviour.RELIABLE,
            observation(read = true, writes = 5, slowest = 100L, errors = 0).behaviour
        )
        assertEquals(
            UsbBehaviour.SLOW,
            observation(slowest = SlowWriteEvidence.FLAGGED_WRITE_MILLIS).behaviour
        )
        assertEquals(
            UsbBehaviour.UNRELIABLE,
            observation(errors = 1).behaviour
        )
        assertEquals(
            UsbBehaviour.UNRELIABLE,
            observation(read = false).behaviour
        )
        assertEquals(
            UsbBehaviour.UNTESTED,
            observation(writes = 0).behaviour
        )
    }

    @Test
    fun `zu wenige Schreibvorgaenge gelten als nicht getestet und nicht als zuverlaessig`() {
        val zuWenig = observation(writes = SlowWriteEvidence.MINIMUM_WRITES_FOR_BEHAVIOUR - 1)

        assertEquals(UsbBehaviour.UNTESTED, zuWenig.behaviour)
        assertFalse(
            "Nichts ging schief, heisst nicht: es wurde gar nichts getan.",
            zuWenig.behaviour.isOfferable
        )
    }

    @Test
    fun `langsame Speicherung wird erklaart und nicht stillschweigend angeboten`() {
        val langsam = observation(writes = 4, slowest = 3_200L)

        assertEquals(UsbBehaviour.SLOW, langsam.behaviour)
        assertFalse("Langsam ist nicht verfuegbar.", langsam.behaviour.isOfferable)
        assertTrue(
            "Die Erklaerung nennt die beobachtete Dauer, nicht eine erfundene Rate.",
            langsam.explanationLines().any { it.contains("3200 ms") }
        )
    }

    @Test
    fun `ein ungenannter Zeitwert wird nicht erfunden`() {
        val ohneDauer = ProbeObservation(
            readAnswered = false,
            writesCompleted = 0,
            slowestWriteMillis = null,
            writeErrors = 2
        )

        assertTrue(
            "Ohne Messung wird keine Dauer behauptet.",
            ohneDauer.explanationLines().any { it.contains("keine Dauer") }
        )
    }

    @Test
    fun `ein Schreibfehler wird einzeln genannt und nicht als Erfolg verbucht`() {
        val kaputt = observation(read = false, writes = 3, errors = 2)

        val zeilen = ProbeObservation(
            readAnswered = false,
            writesCompleted = 3,
            slowestWriteMillis = 40L,
            writeErrors = 2
        ).explanationLines()

        assertEquals(UsbBehaviour.UNRELIABLE, kaputt.behaviour)
        assertTrue(zeilen.any { it.contains("2 Schreibfehler") })
        assertTrue(zeilen.any { it.contains("Lesen antwortete nicht") })
    }

    // ── Protection: access requires an explicit folder choice ───────────────

    @Test
    fun `ein Laufwerk ohne Auswahl des Nutzers kann nicht gebaut werden`() {
        val laufwerk = volume()

        assertTrue(
            "Es gibt nur den einen Weg, ein Laufwerk zu bekommen.",
            laufwerk.pickedByUser
        )
        assertEquals(
            "Der Zugriff ist genau der gewaehlte Ordner.",
            "folder chosen by user",
            laufwerk.scope.label
        )
    }

    @Test
    fun `ein Nachbarordner mit aehnlichem Namen rutscht nicht durch`() {
        val laufwerk = volume(root = "content://x/tree/primary%3AUSB")

        assertTrue(
            UsbPathAvailability.coveredPath(
                laufwerk,
                "content://x/tree/primary%3AUSB/unter/datei.kt"
            )
        )
        assertFalse(
            "Aehnlicher Name ist keine aehnliche Freigabe.",
            UsbPathAvailability.coveredPath(laufwerk, "content://x/tree/primary%3AUSBandere")
        )
        assertFalse(
            UsbPathAvailability.coveredPath(laufwerk, "content://x/tree/primary%3AUSB2")
        )
    }

    @Test
    fun `eine Probe von einem anderen Laufwerk gilt nicht als Beleg`() {
        val eigenes = volume(root = "content://x/tree/A")
        val fremdes = volume(root = "content://x/tree/B")

        val fremdeProbe = UsbProbe(fremdes, observation())

        assertFalse(
            "Der Beleg muss zum geprueften Laufwerk gehoeren.",
            UsbPathAvailability.isUsable(eigenes, fremdeProbe)
        )
    }

    @Test
    fun `eine Ablehnung laesst sich nicht von Hand uebersteuern`() {
        assertFalse(UsbPathAvailability.canOverride())
    }

    @Test
    fun `die Sperre der Geraetemessung laesst sich nicht abschalten`() {
        val feld = UsbEvidence::class.java.getDeclaredField("PROBE_OBSERVED_ON_DEVICE")

        assertTrue(
            "Es ist ein const val - kein var, den ein Aufrufer setzen koennte.",
            Modifier.isFinal(feld.modifiers) && Modifier.isStatic(feld.modifiers)
        )
    }

    @Test
    fun `wenn nichts verfuegbar ist, wird das erklaert und nicht verschwiegen`() {
        val zeilen = UsbPathAvailability.unavailableLines()

        assertTrue(
            "Die Luecke wird benannt.",
            zeilen.any { it.contains("keine USB-Messung") }
        )
        assertTrue(
            "Es wird nicht als 'vielleicht spaeter' angeboten.",
            zeilen.any { it.contains("vorläufig") }
        )
        assertTrue(
            "Der Nutzer bekommt gesagt, was fehlt: ein Test auf dem Geraet.",
            zeilen.any { it.contains("Gerät") }
        )
    }

    // ── Structural: this file touches no storage ────────────────────────────

    @Test
    fun `die Datei greift auf keinen Speicher und kein Netz zu`() {
        val verboten = listOf(
            "java.io.File",
            "java.io.FileOutputStream",
            "java.nio.file.Files",
            "java.net.URL",
            "java.net.HttpURLConnection",
            "android.content.ContentResolver",
            "android.content.Context",
            "android.os.Environment",
            "android.hardware.usb.UsbManager",
            "java.lang.ProcessBuilder"
        )

        // Every method signature, every field type and every supertype of every
        // type this task introduces. A reference to a forbidden type shows up
        // here as a name; a decision class that only passes values cannot have
        // one without naming it.
        val eingefuehrteTypen = listOf(
            UsbProbe::class.java,
            UsbVolume::class.java,
            UsbPathAvailability::class.java,
            ProbeObservation::class.java,
            UsbEvidence::class.java
        )

        val verbindungen = mutableListOf<String>()

        eingefuehrteTypen.forEach { typ ->
            val namen = buildList {
                addAll(typ.declaredMethods.flatMap { m ->
                    m.parameterTypes.map { it.name } + m.returnType.name
                })
                addAll(typ.declaredFields.map { it.type.name })
                typ.superclass?.let { add(it.name) }
                addAll(typ.interfaces.map { it.name })
            }

            verboten.forEach { typname ->
                if (namen.any { it.startsWith(typname) }) {
                    verbindungen.add("${typ.simpleName} -> $typname")
                }
            }
        }

        assertEquals(
            "Entscheidungstypen duerfen keinen Speicher- oder Netztyp nennen: " +
                verbindungen.toString(),
            emptyList<String>(),
            verbindungen
        )
    }

    @Test
    fun `kein Produktionstyp dieser Aufgabe haelt einen Speicher- oder Netztyp`() {
        val verboteneMethoden = listOf(
            "java.io.File",
            "java.net.URL",
            "android.os.Environment",
            "android.content.ContentResolver",
            "java.io.RandomAccessFile",
            "java.nio.channels.FileChannel"
        )

        val beanFunktionen = listOf(
            "canWrite",
            "mkdir",
            "delete",
            "exists",
            "listFiles",
            "createNewFile"
        )

        val verdacht = mutableListOf<String>()

        listOf(UsbProbe::class.java, UsbPathAvailability::class.java, UsbVolume::class.java)
            .forEach { typ ->
                typ.declaredMethods.forEach { methode ->
                    verboteneMethoden.forEach { verboten ->
                        if (methode.parameterTypes.any { it.name == verboten }) {
                            verdacht.add("${typ.simpleName}.${methode.name}($verboten)")
                        }
                    }
                    beanFunktionen.forEach { name ->
                        if (methode.name == name && methode.parameterCount <= 1) {
                            verdacht.add("${typ.simpleName}.${methode.name}()")
                        }
                    }
                }
            }

        assertEquals(
            "Entscheidungstypen duerfen keinen Speicherzugriff anbieten.",
            emptyList<String>(),
            verdacht
        )
    }
}