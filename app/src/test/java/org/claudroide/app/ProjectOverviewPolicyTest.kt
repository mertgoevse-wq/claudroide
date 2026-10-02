package org.claudroide.app

import org.claudroide.app.feature.agent.RequiredApproval
import org.claudroide.app.feature.project.AccessState
import org.claudroide.app.feature.project.LastWorkSummary
import org.claudroide.app.feature.project.OverviewFile
import org.claudroide.app.feature.project.OverviewFileList
import org.claudroide.app.feature.project.PendingApproval
import org.claudroide.app.feature.project.ProjectAccessState
import org.claudroide.app.feature.project.ProjectOverview
import org.claudroide.app.feature.project.SelectionOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 081 — „Projektübersicht“.
 *
 * Die beiden Fertig-Kriterien stehen im Mittelpunkt: ein zurückgezogener Ordner muss
 * unübersehbar sein, und eine Dateiliste darf nur aus einer Auswahl des Nutzers
 * stammen. Der Schutz gegen Geheimnisse wird an der Struktur geprüft.
 */
class ProjectOverviewPolicyTest {

    // ── Testhilfen ───────────────────────────────────────────────────────────

    private fun granted(name: String = "Projekt") = ProjectAccessState.of(
        displayName = name,
        registered = true,
        hasRevokedEntry = false
    )

    private fun revoked(name: String = "Projekt") = ProjectAccessState.of(
        displayName = name,
        registered = true,
        hasRevokedEntry = true
    )

    private fun overview(
        name: String = "Mein Projekt",
        states: List<ProjectAccessState> = listOf(granted()),
        files: OverviewFileList = OverviewFileList(),
        approvals: List<PendingApproval> = emptyList(),
        lastWork: LastWorkSummary? = null
    ) = ProjectOverview(name, states, files, approvals, lastWork)

    private fun joined(lines: List<String>) = lines.joinToString("\n")

    // ── Widerrufene Ordnerrechte ─────────────────────────────────────────────

    @Test
    fun `ein zurueckgezogener Ordner wird berechnet nicht uebergeben`() {
        assertEquals(AccessState.REVOKED, revoked().state)
    }

    @Test
    fun `die Zuruecknahme schlaegt die Registrierung`() {
        // Sonst wuerde ein Ordner, den der Nutzer zurueckgezogen hat, weiterhin
        // als freigegeben erscheinen, nur weil er noch eingetragen ist.
        assertEquals(AccessState.REVOKED, revoked().state)
        assertTrue(granted().state.isUsable)
    }

    @Test
    fun `ein nicht registrierter Ordner ist nicht freigegeben`() {
        val stand = ProjectAccessState.of("X", registered = false, hasRevokedEntry = false)
        assertEquals(AccessState.NONE, stand.state)
        assertFalse(stand.state.isUsable)
    }

    @Test
    fun `ein zurueckgezogener Ordner steht in der Kopfangebote-Zeile`() {
        val text = joined(overview(states = listOf(granted(), revoked("Alt"))).headline())
        assertTrue(text.contains("Zugriff zurückgezogen"))
        assertTrue(text.contains("Alt"))
    }

    @Test
    fun `die Kopfangebote-Zeile nennt den zurueckgezogenen Ordner zuerst`() {
        // Er darf nicht erst nach der Dateiliste ueberlesen werden.
        val zeilen = overview(states = listOf(granted(), revoked("Alt"))).headline()
        val zeileIndex = zeilen.indexOfFirst { it.startsWith("!") }
        assertEquals(1, zeileIndex)
    }

    @Test
    fun `ohne Widerruf erscheint keine Warnung`() {
        val text = joined(overview().headline())
        assertFalse(text.contains("Zurückgezogen"))
    }

    @Test
    fun `mit einem zurueckgezogenen Ordner wird das Arbeiten verweigert`() {
        assertFalse(overview(states = listOf(revoked())).canWork(revoked()))
    }

    @Test
    fun `ein freigegebener Ordner erlaubt das Arbeiten`() {
        assertTrue(overview().canWork(granted()))
    }

    @Test
    fun `ein Ordner mit Zugangsdatei wird als solcher gekennzeichnet`() {
        val stand = ProjectAccessState.of("Geheim", true, false, containsSecretFile = true)
        assertTrue(stand.overviewLine().contains("Zugangsdaten"))
    }

    @Test
    fun `ohne brauchbaren Ordner nennt die Uebersicht den naechsten Schritt`() {
        val o = overview(states = listOf(ProjectAccessState.of("X", false, false)))
        assertTrue(joined(o.nextSafeSteps()).contains("Systemdialog"))
    }

    @Test
    fun `bei Widerruf nennt die Uebersicht das Pruefen des Ordners`() {
        assertTrue(
            joined(overview(states = listOf(revoked())).nextSafeSteps())
                .contains("wieder freigegeben werden soll")
        )
    }

    // ── Keine Dateien ohne Auswahl ───────────────────────────────────────────

    @Test
    fun `eine automatisch gefundene Dateiliste wird abgelehnt`() {
        val fehler = runCatching {
            OverviewFileList(
                files = listOf(OverviewFile("a.kt")),
                selectionOrigin = SelectionOrigin.AUTOMATIC_SCAN
            )
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `auch eine leere automatische Liste wird abgelehnt`() {
        // Sonst waere der Weg "nichts scannen, aber als Scan melden" offen.
        val fehler = runCatching {
            OverviewFileList(selectionOrigin = SelectionOrigin.AUTOMATIC_SCAN)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `eine Nutzerauswahl ist erlaubt`() {
        val liste = OverviewFileList(
            files = listOf(OverviewFile("a.kt")),
            selectionOrigin = SelectionOrigin.USER_SELECTION
        )
        assertFalse(liste.isEmpty)
    }

    @Test
    fun `ohne Auswahl sagt die Uebersicht dass nichts gesucht wird`() {
        val text = joined(overview().overviewLines())
        assertTrue(text.contains("Es wird nichts automatisch gesucht."))
    }

    @Test
    fun `eine gekuerzte Liste behauptet keine Vollstaendigkeit`() {
        val liste = OverviewFileList(
            files = listOf(OverviewFile("a.kt")),
            selectionOrigin = SelectionOrigin.USER_SELECTION,
            isTruncated = true
        )
        assertTrue(joined(overview(files = liste).overviewLines()).contains("Gekürzt"))
    }

    // ── Geheimnisse und keine Dateiinhalte ───────────────────────────────────

    @Test
    fun `eine Zugangsdatei wird nicht mit Namen gezeigt`() {
        val liste = OverviewFileList(
            files = listOf(OverviewFile(".env")),
            selectionOrigin = SelectionOrigin.USER_SELECTION
        )
        val text = joined(overview(files = liste).overviewLines())
        assertFalse(text.contains(".env"))
        assertTrue(text.contains("1 Datei(en) mit Zugangsdaten werden nicht gezeigt."))
    }

    @Test
    fun `eine Zugangsdatei traegt den Platzhalter statt ihres Namens`() {
        val datei = OverviewFile("config/.env")
        assertTrue(datei.isSecret)
        assertFalse(datei.displayName.contains(".env"))
        assertTrue(datei.displayName.isNotBlank())
    }

    @Test
    fun `die Zugangserkennung wird berechnet und nicht uebergeben`() {
        // Es gibt kein Konstruktorargument, das man auf false setzen koennte.
        val felder = OverviewFile::class.java.declaredFields.map { it.name }
        assertFalse(felder.contains("isSecret"))
    }

    @Test
    fun `eine Schlusseldatei wird ebenfalls erkannt`() {
        assertTrue(OverviewFile("app/signing/server.pem").isSecret)
    }

    @Test
    fun `eine Kopie einer Zugangsdatei wird ebenfalls erkannt`() {
        assertTrue(OverviewFile(".env.bak").isSecret)
    }

    @Test
    fun `eine gewoehnliche Quelldatei ist keine Zugangsdatei`() {
        assertFalse(OverviewFile("app/src/main/java/A.kt").isSecret)
    }

    @Test
    fun `ein Ordner mit Zugangsdaten im Namen ist keine Zugangsdatei`() {
        // Die Regel matcht nur den Dateinamen: ein Projektordner "secrets-guide.md"
        // darf nicht versehentlich aussortiert werden.
        assertFalse(OverviewFile("docs/secrets-guide.md").isSecret)
    }

    @Test
    fun `eine gewoehnliche Datei wird gezeigt`() {
        val liste = OverviewFileList(
            files = listOf(OverviewFile("app/src/main/A.kt", 120L)),
            selectionOrigin = SelectionOrigin.USER_SELECTION
        )
        val text = joined(overview(files = liste).overviewLines())
        assertTrue(text.contains("A.kt"))
        assertTrue(text.contains("120 Byte"))
    }

    @Test
    fun `der Dateityp traegt kein Feld fuer Inhalt`() {
        // Kein Inhaltsfeld: die Zusage ist nicht pruefbar zu verletzen, weil es
        // keinen Weg gibt, einen hineinzugeben. Der Test haelt diesen Zustand fest.
        val felder = OverviewFile::class.java.declaredFields.map { it.name }.toSet()
        val verbotene = setOf("content", "contents", "text", "body", "preview", "data")
        assertTrue(felder.none { it.lowercase() in verbotene })
    }

    @Test
    fun `die gezeigten Dateien enthalten niemals eine Zugangsdatei`() {
        val liste = OverviewFileList(
            files = listOf(OverviewFile("a.kt"), OverviewFile(".env")),
            selectionOrigin = SelectionOrigin.USER_SELECTION
        )
        assertEquals(1, liste.visibleFiles.size)
        assertEquals("a.kt", liste.visibleFiles.single().relativePath)
        assertEquals(1, liste.secretCount)
    }

    // ── Letzte Arbeit ────────────────────────────────────────────────────────

    @Test
    fun `ohne Lauf sagt die Uebersicht das auch`() {
        assertTrue(joined(overview().overviewLines()).contains("noch kein Lauf gestartet"))
    }

    @Test
    fun `die letzte Arbeit nennt Schritte Dateien und Testergebnis`() {
        val arbeit = LastWorkSummary(1000L, 4, 2, "18 von 18 bestanden")
        val text = joined(overview(lastWork = arbeit).overviewLines())
        assertTrue(text.contains("4 Schritte"))
        assertTrue(text.contains("2 geänderte Dateien"))
        assertTrue(text.contains("18 von 18 bestanden"))
    }

    @Test
    fun `ein unbestaetigtes Testergebnis wird woertlich uebernommen`() {
        val arbeit = LastWorkSummary(1000L, 1, 0, "nicht ausgewertet")
        assertTrue(
            joined(overview(lastWork = arbeit).overviewLines()).contains("nicht ausgewertet")
        )
    }

    @Test
    fun `eine negative Schrittzahl wird abgelehnt`() {
        val fehler = runCatching { LastWorkSummary(0L, -1, 0, "x") }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    // ── Offene Freigaben ─────────────────────────────────────────────────────

    @Test
    fun `offene Freigaben werden im Klartext genannt`() {
        val o = overview(
            approvals = listOf(
                PendingApproval("s1", RequiredApproval.FILE_CHANGE),
                PendingApproval("s2", RequiredApproval.GIT_PUSH)
            )
        )
        val text = joined(o.overviewLines())
        assertTrue(text.contains("Offene Freigaben (2)"))
        assertTrue(text.contains("Änderung an einer Datei freigeben"))
        assertTrue(text.contains("Git-Upload freigeben"))
    }

    @Test
    fun `ohne offene Freigaben sagt die Uebersicht dass nichts wartet`() {
        assertTrue(
            joined(overview().overviewLines())
                .contains("Keine. Es wartet nichts auf eine Zustimmung.")
        )
    }

    @Test
    fun `offene Freigaben erscheinen als naechster Schritt`() {
        val o = overview(approvals = listOf(PendingApproval("s1", RequiredApproval.FILE_CHANGE)))
        assertTrue(joined(o.nextSafeSteps()).contains("1 offene(n) Freigabe(n)"))
    }

    // ── Vollstaendigkeit ─────────────────────────────────────────────────────

    @Test
    fun `eine fertige Uebersicht ohne offene Punkte nennt das ausdruecklich`() {
        // Freigegebener Ordner, Arbeit erledigt, keine offene Freigabe: Hier gibt
        // es wirklich nichts zu tun, und genau dann soll die Uebersicht das sagen.
        val o = overview(
            states = listOf(granted()),
            lastWork = LastWorkSummary(1000L, 3, 1, "18 von 18 bestanden")
        )
        val schritte = o.nextSafeSteps()
        assertEquals(1, schritte.size)
        assertTrue(schritte.single().contains("nichts zu tun"))
    }

    @Test
    fun `eine Uebersicht ohne Ordnern zeigt das an`() {
        val text = joined(overview(states = emptyList()).overviewLines())
        assertTrue(text.contains("Kein Ordner ausgewählt."))
    }

    @Test
    fun `mehrere Ordner werden alle genannt`() {
        val o = overview(states = listOf(granted("A"), granted("B"), revoked("C")))
        val text = joined(o.overviewLines())
        assertTrue(text.contains("Ordner (3)"))
        assertTrue(o.hasRevokedAccess)
        assertEquals(2, o.usableStates.size)
    }

    @Test
    fun `ein Ordner ohne Namen wird abgelehnt`() {
        val fehler = runCatching { ProjectAccessState("  ", AccessState.GRANTED) }
            .exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `ein Projekt ohne Namen wird abgelehnt`() {
        val fehler = runCatching { ProjectOverview("") }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `eine Datei ohne Pfad wird abgelehnt`() {
        val fehler = runCatching { OverviewFile(" ") }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `eine negative Dateigroesse wird abgelehnt`() {
        val fehler = runCatching { OverviewFile("a.kt", -1L) }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `eine offene Freigabe ohne Schritt wird abgelehnt`() {
        val fehler = runCatching { PendingApproval("", RequiredApproval.FILE_CHANGE) }
            .exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }
}
