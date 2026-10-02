package org.claudroide.app

import org.claudroide.app.feature.project.FolderSelectionOutcome
import org.claudroide.app.feature.project.FolderSelectionState
import org.claudroide.app.feature.project.SelectionOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 082 — „Android-Ordner auswählen“.
 *
 * Der wichtigste Fall ist der häufigste: der Nutzer bricht ab. Genau dort darf
 * keine Berechtigung entstehen, und dieser Test steht deshalb an erster Stelle.
 */
class FolderSelectionPolicyTest {

    private fun granted(
        name: String = "Projekt",
        path: String = "/storage/emulated/0/Documents/Projekt"
    ) = Pair(name, path)

    private fun stateWith(roots: List<String>, last: FolderSelectionOutcome? = null) =
        FolderSelectionState(roots, last)

    private fun linesOf(outcome: FolderSelectionOutcome): List<String> = when (outcome) {
        is FolderSelectionOutcome.FirstGranted -> outcome.descriptionLines
        is FolderSelectionOutcome.Added -> outcome.descriptionLines
        is FolderSelectionOutcome.Replaced -> outcome.descriptionLines
        is FolderSelectionOutcome.Cancelled -> outcome.descriptionLines
        is FolderSelectionOutcome.OutsideSelection -> outcome.descriptionLines
    }

    private fun text(outcome: FolderSelectionOutcome) = linesOf(outcome).joinToString("\n")

    // ── Abbrechen erzeugt keine Berechtigung ─────────────────────────────────

    @Test
    fun `abbrechen gibt keine Berechtigung`() {
        val s = FolderSelectionState()
        val ergebnis = s.fold(SelectionOutcome.CANCELLED)
        assertTrue(ergebnis is FolderSelectionOutcome.Cancelled)
        assertFalse(ergebnis.isGranted)
    }

    @Test
    fun `abbrechen nennt ausdruecklich dass kein Zugriff besteht`() {
        val ergebnis = FolderSelectionState().fold(SelectionOutcome.CANCELLED)
        assertTrue(text(ergebnis).contains("kein Zugriff auf das Dateisystem"))
    }

    @Test
    fun `abbrechen verwechselt wird nicht als Fehler behandelt`() {
        // Ein Abbruch ist der Normalfall und darf keine Fehlermeldung erzeugen.
        val ergebnis = FolderSelectionState().fold(SelectionOutcome.CANCELLED)
        assertTrue(text(ergebnis).contains("abgebrochen"))
        assertFalse(text(ergebnis).contains("Fehler"))
    }

    @Test
    fun `abbrechen loest auch dann nichts aus wenn ein Pfad mitkommt`() {
        // Sonst koennte ein Aufrufer durch Mitgeben eines Pfades aus einem
        // Abbruch doch noch eine Berechtigung bauen.
        val s = FolderSelectionState()
        val ergebnis = s.fold(
            SelectionOutcome.CANCELLED,
            displayName = "Projekt",
            absolutePath = "/storage/emulated/0/Documents/Projekt"
        )
        assertTrue(ergebnis is FolderSelectionOutcome.Cancelled)
        assertEquals(0, s.stateAfter(SelectionOutcome.CANCELLED).grantedRoots.size)
    }

    @Test
    fun `abbrechen laesst einen bestehenden Stand unveraendert`() {
        val vorher = stateWith(listOf("/a"))
        val nachher = vorher.stateAfter(SelectionOutcome.CANCELLED)
        assertEquals(listOf("/a"), nachher.grantedRoots)
    }

    @Test
    fun `eine leere Auswahl gilt nicht als Wahl`() {
        val s = FolderSelectionState()
        val ergebnis = s.fold(SelectionOutcome.GRANTED, displayName = "  ", absolutePath = "  ")
        assertTrue(ergebnis is FolderSelectionOutcome.Cancelled)
    }

    @Test
    fun `eine Auswahl ohne Pfad gilt nicht als Wahl`() {
        val ergebnis = FolderSelectionState().fold(
            SelectionOutcome.GRANTED,
            displayName = "Projekt",
            absolutePath = null
        )
        assertTrue(ergebnis is FolderSelectionOutcome.Cancelled)
    }

    // ─ Nur der ausgewählte Bereich ───────────────────────────────────────────

    @Test
    fun `die erste Auswahl wird uebernommen`() {
        val (name, pfad) = granted()
        val ergebnis = FolderSelectionState().fold(SelectionOutcome.GRANTED, name, pfad)
        assertTrue(ergebnis is FolderSelectionOutcome.FirstGranted)
        assertTrue(ergebnis.isGranted)
    }

    @Test
    fun `die Erklaerung sagt dass nur dieser Ordner gilt`() {
        val (name, pfad) = granted()
        val text = text(FolderSelectionState().fold(SelectionOutcome.GRANTED, name, pfad))
        assertTrue(text.contains("nur auf diesen Ordner"))
    }

    @Test
    fun `die Erklaerung nennt die fehlende allgemeine Berechtigung`() {
        val (name, pfad) = granted()
        val text = text(FolderSelectionState().fold(SelectionOutcome.GRANTED, name, pfad))
        assertTrue(text.contains("wird nicht verlangt"))
    }

    @Test
    fun `ein zweiter Ordner wird hinzugefuegt`() {
        val s = FolderSelectionState(listOf("/a"))
        val ergebnis = s.fold(SelectionOutcome.GRANTED, "Zwei", "/b")
        assertTrue(ergebnis is FolderSelectionOutcome.Added)
        assertEquals(listOf("/a", "/b"), (ergebnis as FolderSelectionOutcome.Added).combinedRoots)
    }

    @Test
    fun `dieselbe Auswahl zweimal ergibt nur einen Ordner`() {
        val s = FolderSelectionState(listOf("/a"))
        val ergebnis = s.fold(SelectionOutcome.GRANTED, "A", "/a")
        assertEquals(1, (ergebnis as FolderSelectionOutcome.Added).combinedRoots.size)
    }

    @Test
    fun `ein Pfad unterhalb des Ordners gehoert dazu`() {
        val s = FolderSelectionState(listOf("/a"))
        assertTrue(s.covers("/a/src/main.kt"))
    }

    @Test
    fun `ein Pfad ausserhalb des Ordners gehoert nicht dazu`() {
        val s = FolderSelectionState(listOf("/a"))
        assertFalse(s.covers("/b/main.kt"))
    }

    @Test
    fun `ein Ordner mit aehnlichem Anfang gehoert nicht dazu`() {
        // "/ab" beginnt mit "/a", liegt aber ausserhalb. Ohne den Trenner "/"
        // waere das ein stiller Zugriff auf fremde Daten.
        val s = FolderSelectionState(listOf("/a"))
        assertFalse(s.covers("/ab/main.kt"))
    }

    @Test
    fun `der Ordner selbst gehoert dazu`() {
        assertTrue(FolderSelectionState(listOf("/a")).covers("/a"))
    }

    @Test
    fun `ein leerer Pfad gehoert zu nichts`() {
        assertFalse(FolderSelectionState(listOf("/a")).covers("  "))
    }

    // ── Erneute Auswahl ──────────────────────────────────────────────────────

    @Test
    fun `eine Ersetzung nennt die Wurzeln die wegfallen`() {
        val s = FolderSelectionState(listOf("/a", "/b"))
        val ergebnis = s.fold(SelectionOutcome.GRANTED, "Neu", "/c", replaceExisting = true)
        assertTrue(ergebnis is FolderSelectionOutcome.Replaced)
        val ersetzt = ergebnis as FolderSelectionOutcome.Replaced
        assertEquals(listOf("/a", "/b"), ersetzt.droppedRoots)
        assertEquals(listOf("/c"), ersetzt.combinedRoots)
    }

    @Test
    fun `eine Ersetzung nennt im Klartext was nicht mehr erreichbar ist`() {
        val s = FolderSelectionState(listOf("/a"))
        val text = text(s.fold(SelectionOutcome.GRANTED, "Neu", "/c", replaceExisting = true))
        assertTrue(text.contains("Nicht mehr erreichbar: /a"))
    }

    @Test
    fun `nach einer Ersetzung deckt der alte Ordner nichts mehr ab`() {
        val s = FolderSelectionState(listOf("/a"))
        val neu = s.stateAfter(SelectionOutcome.GRANTED, "Neu", "/c", replaceExisting = true)
        assertFalse(neu.covers("/a/main.kt"))
        assertTrue(neu.covers("/c/main.kt"))
    }

    @Test
    fun `eine Ersetzung ohne vorherige Auswahl ist einfach die erste`() {
        val s = FolderSelectionState()
        val ergebnis = s.fold(SelectionOutcome.GRANTED, "Neu", "/c", replaceExisting = true)
        assertTrue(ergebnis is FolderSelectionOutcome.FirstGranted)
    }

    @Test
    fun `ohne Ersetzung bleiben fruehere Ordner bestehen`() {
        val s = FolderSelectionState(listOf("/a"))
        val neu = s.stateAfter(SelectionOutcome.GRANTED, "B", "/b")
        assertEquals(listOf("/a", "/b"), neu.grantedRoots)
    }

    // ── Keine Speicherberechtigung ───────────────────────────────────────────

    @Test
    fun `der Zustand kennt kein Feld fuer eine Speicherberechtigung`() {
        val felder = FolderSelectionState::class.java.declaredFields.map { it.name.lowercase() }
        val verbotene = listOf("permission", "read_external", "write_external", "manage_external")
        assertTrue(felder.none { f -> verbotene.any { f.contains(it) } })
    }

    @Test
    fun `der Zustand bietet keine Methode die eine Berechtigung anfordert`() {
        // Geprüft werden nur echte Aktionen, nicht Getter: "getGrantedRoots"
        // enthält zwar "grant", fragt aber nichts an.
        val methoden = FolderSelectionState::class.java.methods
            .filter { it.parameterCount == 0 }
            .map { it.name.lowercase() }
        val verbotene = listOf("request", "permission", "take", "acquire")
        assertTrue(methoden.none { m -> verbotene.any { m.contains(it) } })
    }

    @Test
    fun `vor der ersten Wahl sagt die Anzeige dass noch nichts geht`() {
        val text = FolderSelectionState().explanationLines().joinToString("\n")
        assertTrue(text.contains("kein Ordner ausgewählt"))
        assertTrue(text.contains("keine allgemeine Speicherberechtigung"))
    }

    @Test
    fun `nach der Wahl nennt die Anzeige die Wurzeln`() {
        val s = FolderSelectionState(listOf("/a", "/b"), null)
        val text = s.explanationLines().joinToString("\n")
        assertTrue(text.contains("2 Ordner freigegeben"))
        assertTrue(text.contains("/a"))
        assertTrue(text.contains("/b"))
    }

    @Test
    fun `der aktuelle Ordner wird genannt`() {
        val s = FolderSelectionState().stateAfter(SelectionOutcome.GRANTED, "P", "/a")
        assertEquals("/a", s.currentRoot())
    }

    @Test
    fun `nach einem Abbruch gibt es keinen aktuellen Ordner`() {
        val s = FolderSelectionState().stateAfter(SelectionOutcome.CANCELLED)
        assertEquals(null, s.currentRoot())
    }

    @Test
    fun `vor der ersten Wahl gibt es keinen aktuellen Ordner`() {
        assertEquals(null, FolderSelectionState().currentRoot())
    }
}
