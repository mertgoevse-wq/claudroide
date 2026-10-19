package org.claudroide.app

import org.claudroide.app.feature.git.ConflictVariants
import org.claudroide.app.feature.git.ConflictingSection
import org.claudroide.app.feature.git.MergeGate
import org.claudroide.app.feature.git.MergeOutcome
import org.claudroide.app.feature.git.MergeResolution
import org.claudroide.app.feature.git.ResolutionState
import org.claudroide.app.feature.git.SectionDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 102 — "Git conflicts".
 *
 * The two failures worth guarding are a merge reported as finished while a
 * section is still undecided, and content that vanishes from the result
 * without anyone having chosen it away.
 */
class GitMergeConflictTest {

    private val datei = "src/main.kt"

    private fun abschnitt(id: String, pfad: String = datei) = ConflictingSection(
        sectionId = id,
        filePath = pfad,
        variants = ConflictVariants(
            ours = "UNS-$id",
            theirs = "IHRE-$id",
            base = "VORFAHR-$id"
        )
    )

    private fun abschnitte(vararg ids: String) = ids.map { abschnitt(it) }

    private fun loesung(
        abschnitte: List<ConflictingSection> = abschnitte("a", "b"),
        entscheidungen: Map<String, SectionDecision> = emptyMap(),
        bestaetigt: Boolean = true
    ) = MergeResolution.of(datei, abschnitte, entscheidungen, bestaetigt)

    private fun vollstaendig(bestaetigt: Boolean = true) = loesung(
        entscheidungen = mapOf(
            "a" to SectionDecision.KeptOurs,
            "b" to SectionDecision.KeptTheirs
        ),
        bestaetigt = bestaetigt
    )

    // ── Fertig-Bedingung 1: kein stilles Loesen ─────────────────────────────

    @Test
    fun `ohne Entscheidung gibt es kein Ergebnis`() {
        assertNull(loesung(entscheidungen = emptyMap()).resolvedFile())
    }

    @Test
    fun `ein einziger offener Abschnitt blockiert das ganze Ergebnis`() {
        val teil = loesung(
            entscheidungen = mapOf("a" to SectionDecision.KeptOurs)
        )
        assertEquals(ResolutionState.INCOMPLETE, teil.state)
        assertNull(teil.resolvedFile())
    }

    @Test
    fun `die offenen Abschnitte werden benannt, nicht nur gezaehlt`() {
        val teil = loesung(entscheidungen = mapOf("a" to SectionDecision.KeptOurs))
        assertEquals(listOf("b"), teil.unresolvedSections.map { it.sectionId })
    }

    @Test
    fun `eine fehlende Entscheidung gilt als offen und nicht als eine Seite`() {
        val teil = loesung(entscheidungen = mapOf("a" to SectionDecision.KeptOurs))
        assertEquals(SectionDecision.UNRESOLVED, teil.decisionFor("b"))
    }

    @Test
    fun `ein offener Abschnitt verhindert das Schreiben`() {
        val teil = loesung(entscheidungen = mapOf("a" to SectionDecision.KeptOurs))
        assertTrue(MergeGate.mayWrite(teil) is MergeOutcome.NoFileWritten)
    }

    @Test
    fun `die Sperre nennt den offenen Abschnitt`() {
        val teil = loesung(entscheidungen = mapOf("a" to SectionDecision.KeptOurs))
        val grund = (MergeGate.mayWrite(teil) as MergeOutcome.NoFileWritten).reason
        assertTrue(grund.contains("b"))
        assertTrue(grund.contains("nichts geschrieben"))
    }

    @Test
    fun `alle Abschnitte entschieden ergibt ein Ergebnis`() {
        assertEquals(ResolutionState.COMPLETE, vollstaendig().state)
        assertNotNull(vollstaendig().resolvedFile())
    }

    @Test
    fun `es gibt keinen Zustand teilweise geloest`() {
        // "TEILWEISE" as an outcome would be exactly the state that overwrites.
        assertEquals(2, ResolutionState.values().size)
        assertFalse(ResolutionState.entries.any { it.name.contains("PARTIAL") })
    }

    @Test
    fun `die Vollstaendigkeit wird nie gesetzt, sondern abgeleitet`() {
        val teil = loesung(entscheidungen = mapOf("a" to SectionDecision.KeptOurs))
        val ganz = vollstaendig()
        assertEquals(ResolutionState.INCOMPLETE, teil.state)
        assertEquals(ResolutionState.COMPLETE, ganz.state)
    }

    // ── Fertig-Bedingung 2: Pruefung vor dem Speichern ───────────────────────

    @Test
    fun `ohne Bestaetigung wird nicht geschrieben`() {
        assertTrue(MergeGate.mayWrite(vollstaendig(bestaetigt = false)) is MergeOutcome.NoFileWritten)
    }

    @Test
    fun `die Bestaetigung wird aus nichts abgeleitet`() {
        val anzahlung = MergeResolution.of(
            datei,
            abschnitte("a"),
            mapOf("a" to SectionDecision.KeptOurs)
        )
        assertFalse(anzahlung.confirmedByUser)
        assertTrue(MergeGate.mayWrite(anzahlung) is MergeOutcome.NoFileWritten)
    }

    @Test
    fun `die Vollstaendigkeit wird vor der Bestaetigung geprueft`() {
        // Both wrong: open AND unconfirmed. The open section must be the
        // reported reason, because confirming cannot make it finished.
        val weder = loesung(
            entscheidungen = emptyMap(),
            bestaetigt = false
        )
        val grund = (MergeGate.mayWrite(weder) as MergeOutcome.NoFileWritten).reason
        assertTrue(grund.contains("offen"))
        assertFalse(grund.contains("bestätigt"))
    }

    @Test
    fun `vollstaendig und bestaetigt darf geschrieben werden`() {
        assertTrue(MergeGate.mayWrite(vollstaendig()) is MergeOutcome.MayWrite)
    }

    @Test
    fun `das geschriebene Ergebnis traegt den bestaetigten Zustand`() {
        val ergebnis = (MergeGate.mayWrite(vollstaendig()) as MergeOutcome.MayWrite).file
        assertTrue(ergebnis.isReviewed)
        assertEquals(2, ergebnis.sectionCount)
    }

    // ── Kein Inhalt verschwindet ────────────────────────────────────────────

    @Test
    fun `uns behalten traegt genau uns`() {
        val d = loesung(
            abschnitte = abschnitte("a"),
            entscheidungen = mapOf("a" to SectionDecision.KeptOurs)
        ).resolvedFile()!!
        assertTrue(d.mergedText.contains("UNS-a"))
        assertFalse(d.mergedText.contains("IHRE-a"))
    }

    @Test
    fun `ihre behalten traegt genau ihre`() {
        val d = loesung(
            abschnitte = abschnitte("a"),
            entscheidungen = mapOf("a" to SectionDecision.KeptTheirs)
        ).resolvedFile()!!
        assertTrue(d.mergedText.contains("IHRE-a"))
        assertFalse(d.mergedText.contains("UNS-a"))
    }

    @Test
    fun `beide Seiten behalten traegt beide Texte`() {
        val d = loesung(
            abschnitte = abschnitte("a"),
            entscheidungen = mapOf("a" to SectionDecision.KeptBothSides)
        ).resolvedFile()!!
        assertTrue(d.mergedText.contains("UNS-a"))
        assertTrue(d.mergedText.contains("IHRE-a"))
    }

    @Test
    fun `beide Seiten behalten nennt die Reihenfolge uns vor ihnen`() {
        val d = loesung(
            abschnitte = abschnitte("a"),
            entscheidungen = mapOf("a" to SectionDecision.KeptBothSides)
        ).resolvedFile()!!
        assertTrue(d.mergedText.indexOf("UNS-a") < d.mergedText.indexOf("IHRE-a"))
    }

    @Test
    fun `eine Zusammenfuehrung traegt ihren eigenen Text`() {
        val d = loesung(
            abschnitte = abschnitte("a"),
            entscheidungen = mapOf("a" to SectionDecision.Combined("BEIDES"))
        ).resolvedFile()!!
        assertTrue(d.mergedText.contains("BEIDES"))
    }

    @Test
    fun `eine leere Zusammenfuehrung wird abgewiesen`() {
        assertTrue(runCatching { SectionDecision.Combined("  ") }.isFailure)
    }

    @Test
    fun `keine Seite wird ohne Entscheidung verworfen`() {
        assertTrue(MergeGate.retainedBothSides(vollstaendig()))
    }

    @Test
    fun `bei offenen Abschnitten gilt nichts als erhalten`() {
        val teil = loesung(entscheidungen = mapOf("a" to SectionDecision.KeptOurs))
        assertFalse(MergeGate.retainedBothSides(teil))
    }

    @Test
    fun `die Originale bleiben unabhaengig von der Entscheidung erhalten`() {
        val loesung = loesung(
            entscheidungen = mapOf(
                "a" to SectionDecision.KeptOurs,
                "b" to SectionDecision.KeptTheirs
            )
        )
        val original = loesung.originalVariants()
        assertEquals(2, original.size)
        assertTrue(original.all { it.base == it.base && it.ours.startsWith("UNS-") })
        assertEquals("VORFAHR-a", original[0].base)
    }

    @Test
    fun `eine Entscheidung veraendert die Elternvariante nicht`() {
        val loesung = loesung(
            abschnitte = abschnitte("a"),
            entscheidungen = mapOf("a" to SectionDecision.KeptOurs)
        )
        assertEquals("IHRE-a", loesung.originalVariants().first().theirs)
    }

    @Test
    fun `eine Datei ganz ohne Konflikt liefert kein Ergebnis`() {
        // Nichts zu entscheiden heisst hier nicht "nichts zu tun", sondern
        // "kein Ergebnis": eine leere Datei entsteht nicht aus dem Nichts.
        assertTrue(runCatching { loesung(abschnitte = emptyList()) }.isSuccess)
        assertNull(loesung(abschnitte = emptyList()).resolvedFile())
    }

    // ── Die Abschnitte selbst ───────────────────────────────────────────────

    @Test
    fun `ein Abschnitt ohne Vorfahren wird abgewiesen`() {
        val fehler = runCatching {
            ConflictingSection("a", datei, ConflictVariants("u", "t", ""))
        }
        assertTrue(fehler.isFailure)
    }

    @Test
    fun `gleiche Seiten sind erlaubt, wenn der Vorfahr abweicht`() {
        val gleich = runCatching { ConflictVariants("gleich", "gleich", "anders") }
        assertTrue(gleich.isSuccess)
    }

    @Test
    fun `ein Pfad darf nicht aus dem Projekt ausbrechen`() {
        assertTrue(runCatching { abschnitt("a", "../../etc/passwd") }.isFailure)
    }

    @Test
    fun `doppelt vergebene Abschnittskennungen werden abgewiesen`() {
        val doppelt = runCatching { loesung(abschnitte = abschnitte("a", "a")) }
        assertTrue(doppelt.isFailure)
    }

    @Test
    fun `eine Entscheidung fuer einen unbekannten Abschnitt wird abgewiesen`() {
        val fremd = runCatching {
            loesung(
                abschnitte = abschnitte("a"),
                entscheidungen = mapOf("x" to SectionDecision.KeptOurs)
            )
        }
        assertTrue(fremd.isFailure)
    }

    // ── Die Anzeige ─────────────────────────────────────────────────────────

    @Test
    fun `die Anzeige nennt den offenen Zustand`() {
        val zeilen = loesung(entscheidungen = mapOf("a" to SectionDecision.KeptOurs)).displayLines()
        assertTrue(zeilen.any { it.contains("NICHT geschrieben") })
    }

    @Test
    fun `die Anzeige nennt je Abschnitt seine Entscheidung`() {
        val zeilen = vollstaendig().displayLines()
        assertTrue(zeilen.any { it.contains("uns behalten") })
        assertTrue(zeilen.any { it.contains("ihre behalten") })
    }

    @Test
    fun `die Anzeige zaehlt die Abschnitte`() {
        assertTrue(vollstaendig().displayLines().any { it.contains("Abschnitte: 2") })
    }

    @Test
    fun `jede Entscheidung traegt einen deutschen Namen`() {
        val alle = listOf(
            SectionDecision.UNRESOLVED,
            SectionDecision.KeptOurs,
            SectionDecision.KeptTheirs,
            SectionDecision.Combined("x"),
            SectionDecision.KeptBothSides
        )
        alle.forEach { assertTrue(it.germanLabel.isNotBlank()) }
    }

    // ── Strukturell: diese Datei fuehrt nichts aus ──────────────────────────

    @Test
    fun `das Gate entscheidet nur, es fuehrt nichts aus`() {
        // Geprueft wird nicht ein Verzeichnis von Wörtern, sondern die
        // Tatsache: das Gate gibt eine Entscheidung zurück und kennt keine
        // Datei, keinen Prozess und keinen Rückgabewert, der geschrieben
        // werden könnte. Eine echte Zusammenführung würde eine dieser drei
        // Formen brauchen.
        val gate = MergeGate::class.java
        val verboteneTypen = listOf(
            "java.io.File", "java.lang.ProcessBuilder", "java.io.Writer",
            "java.io.OutputStream", "java.lang.Process"
        )
        gate.declaredFields.forEach { feld ->
            assertFalse(
                "Feld ${feld.name}",
                verboteneTypen.any { feld.type.name.contains(it) }
            )
        }
        gate.methods.forEach { methode ->
            assertFalse(
                "Methode ${methode.name} gibt ${methode.returnType.name} zurück",
                verboteneTypen.any { methode.returnType.name.contains(it) }
            )
        }
    }

    @Test
    fun `die Zusammenfuehrung haelt keine Datei und keinen Prozess`() {
        val verboten = listOf("java.io.File", "java.lang.ProcessBuilder", "java.io.Writer")
        (ConflictVariants::class.java.declaredFields +
            ConflictingSection::class.java.declaredFields +
            MergeResolution::class.java.declaredFields +
            org.claudroide.app.feature.git.ResolvedFile::class.java.declaredFields
            ).forEach { feld ->
            assertFalse(
                "Feld ${feld.name} vom Typ ${feld.type.name}",
                verboten.any { feld.type.name.contains(it) }
            )
        }
    }

    @Test
    fun `das Gate ist ein reines Objekt ohne Zustand zwischen Aufrufen`() {
        val javaClass = MergeGate::class.java
        assertTrue(java.lang.reflect.Modifier.isFinal(javaClass.modifiers))
        assertEquals(
            0,
            javaClass.declaredFields.count { !java.lang.reflect.Modifier.isStatic(it.modifiers) }
        )
    }

    @Test
    fun `die Entscheidung ist nach aussen nur lesbar`() {
        // A setter would allow a half-finished merge: decide one section late.
        val setter = MergeResolution::class.java.methods.filter {
            it.name.startsWith("set") || it.name == "resolve"
        }
        assertEquals(emptyList<String>(), setter.map { it.name })
    }
}