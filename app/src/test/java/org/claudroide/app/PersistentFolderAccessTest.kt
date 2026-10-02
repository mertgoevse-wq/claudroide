package org.claudroide.app

import org.claudroide.app.feature.project.AccessMode
import org.claudroide.app.feature.project.InvalidGrant
import org.claudroide.app.feature.project.PersistOutcome
import org.claudroide.app.feature.project.PersistedGrant
import org.claudroide.app.feature.project.PersistentFolderAccess
import org.claudroide.app.feature.project.RestoreReport
import org.claudroide.app.feature.project.RevokeOutcome
import org.claudroide.app.feature.project.UriAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 083 — „Ordnerzugriff merken“.
 *
 * Der häufigste und wichtigste Fall steht an erster Stelle: der
 * Widerruf. Er muss sofort wirken, ohne Neustart und ohne
 * Rückweg. Danach der zweite Kern der Aufgabe: ein gelöschter oder
 * verschobener Ordner darf nicht still ersetzt werden.
 */
class PersistentFolderAccessTest {

    private val projektUri = "content://com.android.externalstorage.documents/tree/Documents%3AProjekt"
    private val anderUri = "content://com.android.externalstorage.documents/tree/Documents%3AAnderer"
    private val projektPfad = "/storage/emulated/0/Documents/Projekt"
    private val anderPfad = "/storage/emulated/0/Documents/Anderer"

    private fun grant(
        uri: String = projektUri,
        name: String = "Projekt",
        path: String = projektPfad,
        readOnly: Boolean = false
    ) = PersistedGrant(uri, name, path, readOnly)

    private fun access(grants: List<PersistedGrant> = listOf(grant())) =
        PersistentFolderAccess(grants)

    private fun availability(
        available: Set<String> = setOf(projektUri, anderUri),
        writable: Set<String> = setOf(projektUri, anderUri)
    ): UriAvailability = UriAvailability { uri, mode ->
        val erreichbar = available.contains(uri)
        erreichbar && (mode == AccessMode.READ || writable.contains(uri))
    }

    // ── Widerruf wirkt sofort ──────────────────────────────────────────

    @Test
    fun `widerruf entfernt den Ordner und erhoeht die generation`() {
        val vorher = access()
        val ergebnis = vorher.revoke(projektPfad)

        assertTrue(ergebnis is RevokeOutcome.Revoked)
        val nachher = vorher.stateAfterRevoke(projektPfad)
        assertEquals(0, nachher.grants.size)
        assertEquals(1L, nachher.generation)
    }

    @Test
    fun `widerruf stoppt den Schreibzugriff sofort`() {
        val zustaendig = availability()
        val vorher = access()
        assertTrue(vorher.canWrite("$projektPfad/main.kt", zustaendig))

        val nachher = vorher.stateAfterRevoke(projektPfad)
        assertFalse(
            "Nach dem Widerruf darf nichts mehr geschrieben werden — " +
                "auch nicht mit dem alten Verfügbarkeitsabfrager.",
            nachher.canWrite("$projektPfad/main.kt", zustaendig)
        )
    }

    @Test
    fun `ein Urteil aus der alten Generation ist nach dem Widerruf unbrauchbar`() {
        val vorher = access()
        val altesUrteil = vorher.generation
        val nachher = vorher.stateAfterRevoke(projektPfad)

        assertFalse(nachher.verdictStillValid(altesUrteil))
        assertTrue(nachher.verdictStillValid(nachher.generation))
    }

    @Test
    fun `widerruf eines nie freigegebenen Ordners aendert nichts`() {
        val stand = access()
        val ergebnis = stand.revoke(anderPfad)

        assertTrue(ergebnis is RevokeOutcome.NotGranted)
        assertEquals(stand, stand.stateAfterRevoke(anderPfad))
    }

    // ── Gelöschte oder verschobene Ordner werden nicht still ersetzt ───

    @Test
    fun `ein geloeschter Ordner wird nicht still uebernommen`() {
        val stand = access()
        // Der Ordner existiert nicht mehr: keine URI ist erreichbar.
        val verschwunden = availability(available = emptySet(), writable = emptySet())

        val bericht = stand.restore(verschwunden)

        assertEquals(0, bericht.usable.size)
        assertEquals(1, bericht.invalid.size)
        assertEquals(
            "Der Ordner ist gelöscht oder verschoben.",
            bericht.invalid.first().reason
        )
        assertTrue(bericht.allInvalid)
    }

    @Test
    fun `ein verschobener Ordner bleibt mit dem alten URI als unbrauchbar gemeldet`() {
        // Nur der zweite Ordner existiert noch — der erste wurde verschoben.
        val stand = access(listOf(grant(), grant(uri = anderUri, name = "Anderer", path = anderPfad)))
        val nurNeuer = availability(
            available = setOf(anderUri),
            writable = setOf(anderUri)
        )

        val bericht = stand.restore(nurNeuer)

        assertEquals(1, bericht.usable.size)
        assertEquals(anderUri, bericht.usable.first().uriString)
        assertEquals(
            projektUri,
            (bericht.invalid.first() as InvalidGrant).grant.uriString
        )
        // Kein stiller Ersatz: der verschobene Ordner taucht nicht als
        // „jetzt erreichbar“ auf, sondern nur als unbrauchbar.
        assertFalse(bericht.usable.any { it.uriString == projektUri })
    }

    @Test
    fun `entzogener Schreibzugriff wird beim Wiederherstellen gemeldet`() {
        val stand = access()
        // Der Ordner ist da, aber nur noch lesbar.
        val nurLesen = availability(writable = emptySet())

        val bericht = stand.restore(nurLesen)

        assertEquals(0, bericht.usable.size)
        assertEquals(
            "Der Schreibzugriff wurde entzogen.",
            bericht.invalid.first().reason
        )
    }

    @Test
    fun `restore repariert keinen ungueltigen Eintrag`() {
        val stand = access()
        val bericht = stand.restore(availability(available = emptySet(), writable = emptySet()))

        // Der Bericht ist Information für den Nutzer, kein neuer Stand.
        // Es gibt keine Methode, die aus dem Bericht eine Freigabe macht.
        assertEquals(0, bericht.usable.size)
        assertFalse(bericht.hasUsable)
    }

    // ── Zugriff bleibt auf den gewünschten URI beschränkt ─────────────

    @Test
    fun `ein Nachbarordner mit aehnlichem Namen ist nicht abgedeckt`() {
        val stand = access()
        // „/storage/Projekt-Alt“ liegt nicht unter „/storage/Projekt“.
        assertFalse(stand.covers("/storage/emulated/0/Documents/Projekt-Alt/main.kt"))
    }

    @Test
    fun `unterordner sind abgedeckt`() {
        val stand = access()
        assertTrue(stand.covers("$projektPfad/app/src/main.kt"))
        assertTrue(stand.covers(projektPfad))
    }

    @Test
    fun `ein nur lesender Ordner laesst kein Schreiben zu`() {
        val stand = access(listOf(grant(readOnly = true)))
        val zustaendig = availability()

        assertTrue(stand.canRead("$projektPfad/main.kt", zustaendig))
        assertFalse(
            "Ein nur-lesender Ordner darf nicht beschreibbar sein.",
            stand.canWrite("$projektPfad/main.kt", zustaendig)
        )
    }

    // ── Kein Freigabezustand wird zwischengespeichert ─────────────────

    @Test
    fun `isUsable prueft bei jedem Aufruf neu gegen den Abfrager`() {
        var erreichbar = true
        val wechselnd = UriAvailability { uri, _ -> erreichbar && uri == projektUri }
        val stand = access()

        assertTrue(stand.isUsable("$projektPfad/main.kt", AccessMode.READ, wechselnd))

        erreichbar = false
        assertFalse(
            "Ein vor einer Sekunde erreichbarer Ordner kann jetzt weg sein — " +
                "die Prüfung muss neu rechnen, nicht ein Ergebnis merken.",
            stand.isUsable("$projektPfad/main.kt", AccessMode.READ, wechselnd)
        )
    }

    @Test
    fun `ein weggenommener Schreibzugriff wird sofort beim naechsten Aufruf erkannt`() {
        var schreibbereit = true
        val abfrager = UriAvailability { uri, mode ->
            uri == projektUri && (mode == AccessMode.READ || schreibbereit)
        }
        val stand = access()

        assertTrue(stand.canWrite("$projektPfad/main.kt", abfrager))

        schreibbereit = false
        assertFalse(stand.canWrite("$projektPfad/main.kt", abfrager))
    }

    // ── Persistierung und Stand ────────────────────────────────────────

    @Test
    fun `eine verweigerte Persistierung erzeugt keine Freigabe`() {
        val stand = PersistentFolderAccess()
        val nachher = stand.persist(grant(), PersistOutcome.NOT_PERSISTED)

        assertEquals(0, nachher.grants.size)
        assertEquals(0L, nachher.generation)
        assertFalse(nachher.hasGrants)
    }

    @Test
    fun `derselbe Ordner wird nicht doppelt gesichert`() {
        val stand = access()
        val nachher = stand.persist(grant(), PersistOutcome.PERSISTED)

        assertEquals(1, nachher.grants.size)
        assertEquals(stand.generation, nachher.generation)
    }

    @Test
    fun `eine neue Freigabe erhoeht die generation`() {
        val stand = access()
        val nachher = stand.persist(grant(uri = anderUri, name = "Anderer", path = anderPfad), PersistOutcome.PERSISTED)

        assertEquals(2, nachher.grants.size)
        assertEquals(stand.generation + 1L, nachher.generation)
    }

    @Test
    fun `replaceAll verfaellt alle aelteren Urteile`() {
        val stand = access()
        val altesUrteil = stand.generation
        val nachher = stand.replaceAll(listOf(grant(uri = anderUri, name = "Anderer", path = anderPfad)))

        assertEquals(1, nachher.grants.size)
        assertEquals(anderPfad, nachher.grantedPaths.first())
        assertFalse(nachher.verdictStillValid(altesUrteil))
    }

    // ── Erläuterung für die Oberfläche ─────────────────────────────────

    @Test
    fun `die erklaerung nennt den Modus jedes Ordners`() {
        val stand = access(listOf(grant(), grant(uri = anderUri, name = "Nur-Lese", path = anderPfad, readOnly = true)))
        val zeilen = stand.explanationLines()

        assertEquals(3, zeilen.size)
        assertTrue(zeilen[1].contains("Lesen und Schreiben"))
        assertTrue(zeilen[2].contains("nur Lesen"))
    }

    @Test
    fun `der wiederherstellungsbericht erklaert was zu tun ist`() {
        val stand = access()
        val bericht = stand.restore(availability(available = emptySet(), writable = emptySet()))
        val zeilen = bericht.explanationLines()

        assertTrue(zeilen.any { it.contains("Bitte wähle die betroffenen Ordner erneut aus.") })
        assertTrue(zeilen.any { it.contains("Nicht mehr erreichbar") })
    }

    @Test
    fun `ein leerer Stand erklaert dass nichts gespeichert ist`() {
        val zeilen = PersistentFolderAccess().explanationLines()

        assertEquals(2, zeilen.size)
        assertTrue(zeilen[0].contains("kein Ordner dauerhaft freigegeben"))
    }

    // ── Der Typ trägt keinen allgemeinen Speicherzugriff ───────────────

    @Test
    fun `eine Freigabe ohne URI ist unzulaessig`() {
        try {
            PersistedGrant(uriString = "", displayName = "Projekt", path = projektPfad)
            org.junit.Assert.fail("Eine leere URI darf keine Freigabe erzeugen.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet: der init-Block verlangt einen URI
        }
    }

    @Test
    fun `eine Freigabe ohne Pfad ist unzulaessig`() {
        try {
            PersistedGrant(uriString = projektUri, displayName = "Projekt", path = "  ")
            org.junit.Assert.fail("Ein leerer Pfad darf keine Freigabe erzeugen.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet: der init-Block verlangt einen Pfad
        }
    }

    @Test
    fun `der Stand kennt keine Methode die eine allgemeine Berechtigung anfordert`() {
        // Die Aufgabe verlangt, dass der Zugriff auf den gewünschten URI
        // beschränkt bleibt. Ein Weg, eine allgemeine Speicherberechtigung
        // hinzuzufügen, darf im Typ nicht existieren.
        val methoden = PersistentFolderAccess::class.java.declaredMethods.map { it.name }
        val unerwuenscht = listOf(
            "requestStoragePermission",
            "grantAllStorage",
            "setFullAccess",
            "enableBroadAccess",
            "requestManageExternalStorage"
        )
        assertFalse(methoden.any { it in unerwuenscht })
    }
}
