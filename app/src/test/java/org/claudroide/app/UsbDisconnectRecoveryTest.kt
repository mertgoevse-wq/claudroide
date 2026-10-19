package org.claudroide.app

import org.claudroide.app.feature.project.MediumAccess
import org.claudroide.app.feature.project.ConfirmedWrite
import org.claudroide.app.feature.project.DisconnectEvidence
import org.claudroide.app.feature.project.MediumFileState
import org.claudroide.app.feature.project.RecoveryDecision
import org.claudroide.app.feature.project.ResumeGate
import org.claudroide.app.feature.project.SaveStatus
import org.claudroide.app.feature.project.StorageSession
import org.claudroide.app.feature.project.TrackedFile
import org.claudroide.app.feature.project.WriteReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Task 086 — "catch USB loss" (USB-Verlust abfangen).
 *
 *  1. *"No change is shown as saved when it is not."* — the strong test is
 *     [nur_eine_bestaetigte_Aenderung_darf_als_gespeichert_erscheinen]: it
 *     walks every non-confirmed report and asserts none of them can be shown
 *     as saved, and [gespeichert_gibt_es_nur_mit_Bestaetigung] pins the type
 *     shape, so "saved" cannot be produced without a confirmation.
 *
 *  2. *"Continuing is possible only after a fresh access and file state
 *     check."* — [ein_guter_Zugriff_allein_genuegt_nicht] and
 *     [ein_gepruefter_Dateizustand_allein_genuegt_auch_nicht] check both
 *     halves separately, because a gate that checks only one of them would
 *     look exactly like a gate that checks both.
 */
class UsbDisconnectRecoveryTest {

    // ── Fixtures ────────────────────────────────────────────────────────────

    private fun datei(
        pfad: String,
        zustand: MediumFileState = MediumFileState.CONFIRMED,
        groesse: Long? = 100L
    ): TrackedFile = TrackedFile(path = pfad, state = zustand, sizeBytes = groesse)

    private fun sitzung(
        zugriff: MediumAccess = MediumAccess.VERIFIED,
        dateien: List<TrackedFile> = listOf(datei("projekt/Main.kt")),
        system: String? = null
    ): StorageSession =
        StorageSession(
            rootUri = "content://x/tree/primary%3AUSB",
            accessState = zugriff,
            trackedFiles = dateien,
            systemMessage = system
        )

    // ── Condition 1: nothing unsaved is shown as saved ──────────────────────

    @Test
    fun `nur eine bestaetigte Aenderung darf als gespeichert erscheinen`() {
        val bestaetigt = SaveStatus.Saved(
            ConfirmedWrite(
                path = "projekt/Main.kt",
                bytesWritten = 2_048L,
                systemMessage = "written"
            )
        )

        assertTrue(
            "Das Dateisystem hat es bestaetigt.",
            bestaetigt.mayBeShownAsSaved
        )

        listOf(
            WriteReport.UNCONFIRMED,
            WriteReport.FAILED,
            WriteReport.NO_REPLY
        ).forEach { bericht ->
            val unbestaetigt = SaveStatus.Unconfirmed("projekt/Main.kt", bericht)
            val verloren = SaveStatus.Lost("projekt/Main.kt")

            assertFalse(
                "Ein Schreibvorgang mit Bericht '${bericht.label}' ist nicht gespeichert.",
                unbestaetigt.mayBeShownAsSaved
            )
            assertFalse(
                "Ein verlorenes Medium ist nicht gespeichert.",
                verloren.mayBeShownAsSaved
            )
        }
    }

    @Test
    fun `gespeichert gibt es nur mit Bestaetigung`() {
        val typ = SaveStatus.Saved::class.java

        val mitBestaetigung = typ.declaredConstructors.first()
        val parameter = mitBestaetigung.parameterTypes.toList()

        assertEquals(
            "SaveStatus.Saved nimmt genau eine ConfirmedWrite entgegen.",
            listOf(ConfirmedWrite::class.java),
            parameter
        )
    }

    @Test
    fun `eine Bestaetigung braucht echte Bytes und einen Pfad`() {
        val pfadFehlt = abgewiesen {
            ConfirmedWrite(path = "  ", bytesWritten = 10L, systemMessage = null)
        }
        val nullBytes = abgewiesen {
            ConfirmedWrite(path = "a.kt", bytesWritten = 0L, systemMessage = null)
        }

        assertTrue("Eine Bestaetigung ohne Pfad ist keine.", pfadFehlt)
        assertTrue("Null Bytes sind nicht geschrieben worden.", nullBytes)
    }

    @Test
    fun `unbestaetigt und verloren werden unterschieden`() {
        val unbestaetigt = SaveStatus.Unconfirmed("a.kt", WriteReport.NO_REPLY)
        val verloren = SaveStatus.Lost("a.kt")

        assertTrue(
            "Der Nutzer muss unterscheiden koennen, was er nicht weiss und was weg ist.",
            unbestaetigt.displayLines() != verloren.displayLines()
        )
        assertTrue(
            "Beide sagen, dass es nicht gespeichert ist.",
            unbestaetigt.displayLines().first().startsWith("Nicht gespeichert") &&
                verloren.displayLines().first().startsWith("Nicht gespeichert")
        )
    }

    @Test
    fun `der Arbeitsspeicherhinweis nennt die Gefahr und droht mit dem Verlust`() {
        val gehalten = RecoveryDecision.HoldInMemory(
            bytesAtRisk = 4_096L,
            reason = "Das Medium ist nicht erreichbar."
        )

        val zeilen = gehalten.displayLines()

        assertTrue(zeilen.any { it.contains("4096") })
        assertTrue(
            "Der Nutzer muss wissen, dass der Prozess das Ende waere.",
            zeilen.any { it.contains("weg") }
        )
        assertFalse(
            "Im Arbeitsspeicher ist nichts gespeichert.",
            gehalten.mayBeShownAsSaved
        )
    }

    // ── Condition 2: continue only after a fresh check ──────────────────────

    @Test
    fun `ein guter Zugriff allein genuegt nicht`() {
        val offen = sitzung(
            zugriff = MediumAccess.VERIFIED,
            dateien = listOf(datei("a.kt", MediumFileState.UNVERIFIED))
        )

        val entscheidung = ResumeGate.mayResume(offen)

        assertTrue(
            "Der Pfad ist da, aber niemand hat den Inhalt gelesen.",
            entscheidung is ResumeGate.ResumeDecision.Refused
        )
        assertEquals(
            listOf("a.kt"),
            (entscheidung as ResumeGate.ResumeDecision.Refused).blockingPaths
        )
    }

    @Test
    fun `ein gepruefter Dateizustand allein genuegt auch nicht`() {
        val weg = sitzung(
            zugriff = MediumAccess.GONE,
            dateien = listOf(datei("a.kt", MediumFileState.CONFIRMED))
        )

        val entscheidung = ResumeGate.mayResume(weg)

        assertTrue(
            "Alle Dateien bestaetigt, aber das Medium ist weg.",
            entscheidung is ResumeGate.ResumeDecision.Refused
        )
        assertTrue(
            (entscheidung as ResumeGate.ResumeDecision.Refused)
                .reason.contains("nicht erreichbar")
        )
    }

    @Test
    fun `ungepruefter Zugriff gibt kein Fortsetzen`() {
        val unbekannt = sitzung(zugriff = MediumAccess.UNKNOWN)

        val entscheidung = ResumeGate.mayResume(unbekannt)

        assertTrue(entscheidung is ResumeGate.ResumeDecision.Refused)
        assertTrue(
            (entscheidung as ResumeGate.ResumeDecision.Refused)
                .reason.contains("noch nicht neu geprueft")
        )
    }

    @Test
    fun `nach beiden Pruefungen darf fortgesetzt werden`() {
        val fertig = sitzung(
            zugriff = MediumAccess.VERIFIED,
            dateien = listOf(datei("a.kt"), datei("b.kt"))
        )

        val entscheidung = ResumeGate.mayResume(fertig)

        assertTrue(entscheidung is ResumeGate.ResumeDecision.MayResume)
        assertEquals(2, (entscheidung as ResumeGate.ResumeDecision.MayResume).checkedFiles)
    }

    @Test
    fun `eine Ablehnung nennt die betroffenen Dateien einzeln`() {
        val teilDefekt = sitzung(
            dateien = listOf(
                datei("a.kt", MediumFileState.CONFIRMED),
                datei("b.kt", MediumFileState.MISSING),
                datei("c.kt", MediumFileState.UNVERIFIED)
            )
        )

        val zeilen = ResumeGate.refusalLines(
            ResumeGate.mayResume(teilDefekt) as ResumeGate.ResumeDecision.Refused
        )

        assertTrue(zeilen.any { it.contains("b.kt") })
        assertTrue(zeilen.any { it.contains("c.kt") })
        assertFalse(
            "a.kt ist bestaetigt und wird nicht als blockiert genannt.",
            zeilen.any { it.contains("a.kt") }
        )
    }

    @Test
    fun `zwei Eintraege mit demselben Pfad werden abgewiesen`() {
        val doppelt = abgewiesen {
            sitzung(
                dateien = listOf(datei("a.kt"), datei("a.kt"))
            )
        }

        assertTrue("Ein doppelter Pfad waere eine stillschweigende Ueberschreibung.", doppelt)
    }

    // ── Protection: no automatic copy to an unknown place ───────────────────

    @Test
    fun `keine Wiederherstellungsart nennt ein fremdes Ziel`() {
        val werte = listOf(
            RecoveryDecision.KeepInPlace("content://x/tree/primary%3AUSB"),
            RecoveryDecision.HoldInMemory(10L, "Medium weg."),
            RecoveryDecision.Discard(listOf("a.kt"))
        )

        val verboteneBegriffe = listOf(
            "backup", "Back-up", "kopie", " Kopie", "temporaer", "cache",
            "second", "extern", "andere", "austausch", "wolke", "cloud"
        )

        val zeilen = werte.flatMap { it.displayLines() }
        val treffer = zeilen.filter { zeile ->
            verboteneBegriffe.any { zeile.contains(it, ignoreCase = false) }
        }

        assertEquals(
            "Keine Zeile darf auf ein zweites Ziel hindeuten: " + treffer.toString(),
            emptyList<String>(),
            treffer
        )
    }

    @Test
    fun `das Tor laesst sich nicht uebersteuern`() {
        assertFalse(ResumeGate.canOverride())
    }

    @Test
    fun `die Geraeteluecke bleibt ein Wert im Programm`() {
        assertFalse(
            "Ein Speicherverlust wurde auf dem Geraet nie beobachtet.",
            DisconnectEvidence.INTERRUPTION_OBSERVED_ON_DEVICE
        )
        assertTrue(
            DisconnectEvidence.statementLines()
                .any { it.contains("keine Messung") }
        )

        val feld = DisconnectEvidence::class.java
            .getDeclaredField("INTERRUPTION_OBSERVED_ON_DEVICE")

        assertTrue(
            "const val, kein var.",
            Modifier.isFinal(feld.modifiers) && Modifier.isStatic(feld.modifiers)
        )
    }

    @Test
    fun `die Sitzung nennt fehlende Dateien und behauptet keinen Ersatz`() {
        val mitVerlust = sitzung(
            dateien = listOf(
                datei("a.kt", MediumFileState.CONFIRMED),
                datei("b.kt", MediumFileState.MISSING)
            )
        )

        val zeilen = mitVerlust.displayLines()

        assertEquals(listOf("b.kt"), mitVerlust.missingFiles.map { it.path })
        assertTrue(zeilen.any { it.contains("b.kt") })
        assertTrue(
            "Kein stiller Ersatz durch einen aehnlich benannten Ordner.",
            zeilen.any { it.contains("nicht ersetzt") }
        )
    }

    @Test
    fun `kein Produktionstyp dieser Aufgabe haelt einen Speicher- oder Netztyp`() {
        val verbotene = listOf(
            "java.io.File",
            "java.io.RandomAccessFile",
            "java.nio.file",
            "java.nio.channels.FileChannel",
            "java.net",
            "android.os.Environment",
            "android.content.ContentResolver"
        )

        val verdacht = mutableListOf<String>()

        listOf(
            SaveStatus::class.java,
            StorageSession::class.java,
            ResumeGate::class.java,
            RecoveryDecision::class.java,
            DisconnectEvidence::class.java
        ).forEach { typ ->
            listOf(typ).plus(typ.declaredClasses.toList()).forEach { kandidat ->
                val namen = buildList {
                    addAll(kandidat.declaredMethods.flatMap { m ->
                        m.parameterTypes.map { it.name } + m.returnType.name
                    })
                    addAll(kandidat.declaredFields.map { it.type.name })
                }
                verbotene.forEach { typname ->
                    if (namen.any { it.startsWith(typname) }) {
                        verdacht.add("${kandidat.simpleName} -> $typname")
                    }
                }
            }
        }

        assertEquals(
            "Diese Typen entscheiden, sie greifen nicht zu: " + verdacht.toString(),
            emptyList<String>(),
            verdacht
        )
    }

    /** Fuehrt [block] aus und meldet, ob der Wert abgewiesen wurde. */
    private fun abgewiesen(block: () -> Unit): Boolean = try {
        block()
        false
    } catch (expected: IllegalArgumentException) {
        true
    }
}