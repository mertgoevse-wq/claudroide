package org.claudroide.app

import org.claudroide.app.feature.project.AccessIntent
import org.claudroide.app.feature.project.AccessRule
import org.claudroide.app.feature.project.SpecialFile
import org.claudroide.app.feature.project.SpecialFileKind
import org.claudroide.app.feature.project.SpecialFilePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 121 — „Links und Sonderdateien“.
 *
 * Der Angriff steht an erster Stelle: ein Verweis **im** Projekt, der auf
 * etwas **außerhalb** zeigt. Der geschriebene Pfad sieht harmlos aus, und
 * genau deshalb ist er gefährlich.
 */
class SpecialFilePolicyTest {

    private val root = "/storage/emulated/0/Documents/Projekt"

    private fun datei(
        path: String = "$root/src/Main.kt",
        kind: SpecialFileKind = SpecialFileKind.REGULAR_FILE,
        resolvedTarget: String? = null
    ) = SpecialFile(path, kind, resolvedTarget)

    private fun verweis(
        path: String = "$root/linked",
        ziel: String? = null
    ) = SpecialFile(path, SpecialFileKind.SYMLINK, ziel)

    // ── Der Linkangriff ───────────────────────────────────────────────

    @Test
    fun `ein Verweis auf ein fremdes Ziel wird abgelehnt`() {
        val v = verweis(ziel = "/data/data/com.other.app/shared_prefs/prefs.xml")
        val entscheidung = SpecialFilePolicy.isPermitted(v, root, AccessIntent.READ)

        assertFalse(
            "Ein Verweis auf ein fremdes Ziel darf nicht gelesen werden.",
            entscheidung.allowed
        )
        assertEquals(AccessRule.TARGET_OUTSIDE_PROJECT, entscheidung.rule)
    }

    @Test
    fun `derselbe fremde Pfad wird bei direktem Zugriff genauso abgelehnt`() {
        // Derselbe Ort, zwei Namen: einmal als Quelle, einmal als Ziel. Beide
        // müssen dasselbe Urteil bekommen.
        val ueberVerweis = SpecialFilePolicy.isPermitted(
            verweis(ziel = "/data/data/com.other.app/shared_prefs/prefs.xml"),
            root,
            AccessIntent.WRITE
        )
        val direkt = SpecialFilePolicy.isPermitted(
            datei(path = "/data/data/com.other.app/shared_prefs/prefs.xml"),
            root,
            AccessIntent.WRITE
        )
        assertFalse(ueberVerweis.allowed)
        assertFalse(direkt.allowed)
    }

    @Test
    fun `ein Verweis ohne ermitteltes Ziel wird abgelehnt`() {
        // Der Pfad liegt sauber im Projekt — aber ohne Ziel gibt es nichts zu
        // prüfen, also wird nicht zugegriffen.
        val v = verweis(ziel = null)
        val entscheidung = SpecialFilePolicy.isPermitted(v, root, AccessIntent.READ)

        assertFalse(entscheidung.allowed)
        assertEquals(AccessRule.UNRESOLVED_LINK, entscheidung.rule)
        assertTrue(entscheidung.reason.contains("nicht ermittelt"))
    }

    @Test
    fun `ein aufgeloester Verweis im Projekt ist erlaubt`() {
        val v = verweis(ziel = "$root/app/src/main.kt")
        assertTrue(SpecialFilePolicy.permits(v, root, AccessIntent.READ))
    }

    @Test
    fun `ein Verweis auf ein uebergeordnetes Verzeichnis wird abgelehnt`() {
        val v = verweis(ziel = "/storage/emulated/0/Documents")
        assertFalse(SpecialFilePolicy.permits(v, root, AccessIntent.READ))
    }

    @Test
    fun `ein traversierender Verweis wird abgelehnt`() {
        val v = verweis(ziel = "$root/../../Documents/Privat/Notiz.txt")
        assertFalse(SpecialFilePolicy.permits(v, root, AccessIntent.READ))
    }

    @Test
    fun `ein Verweis auf ein Android-Systemverzeichnis wird abgelehnt`() {
        listOf("/system/build.prop", "/data/data/org.claudroide.app/files", "/proc/self/environ")
            .forEach { ziel ->
                assertFalse(
                    "Ein Verweis auf $ziel darf nicht gelesen werden.",
                    SpecialFilePolicy.permits(verweis(ziel = ziel), root, AccessIntent.READ)
                )
            }
    }

    // ── Sonderdateien ──────────────────────────────────────────────────

    @Test
    fun `eine Geraetedatei wird nicht geoeffnet`() {
        val d = datei(path = "/dev/block/sda", kind = SpecialFileKind.DEVICE)
        val entscheidung = SpecialFilePolicy.isPermitted(d, root, AccessIntent.READ)

        assertFalse(entscheidung.allowed)
        assertEquals(AccessRule.SPECIAL_FILE, entscheidung.rule)
        assertTrue(entscheidung.reason.contains("Gerätedatei"))
    }

    @Test
    fun `jede Sonderdatei wird abgelehnt`() {
        listOf(
            SpecialFileKind.DEVICE,
            SpecialFileKind.SOCKET,
            SpecialFileKind.FIFO
        ).forEach { art ->
            val d = datei(path = "/dev/dingens", kind = art)
            assertFalse("$art darf nicht geöffnet werden.", SpecialFilePolicy.permits(d, root, AccessIntent.READ))
        }
    }

    @Test
    fun `eine Pipesperre blockiert nicht den ganzen Ablauf`() {
        // Lesen aus einer FIFO kann endlos blockieren — sie wird abgelehnt,
        // statt den Vorgang hängen zu lassen.
        val fifo = datei(path = "/tmp/kanal", kind = SpecialFileKind.FIFO)
        assertFalse(SpecialFilePolicy.permits(fifo, root, AccessIntent.READ))
    }

    @Test
    fun `eine unbekannte Dateiart wird nicht geraten`() {
        val u = datei(path = "$root/fraglich", kind = SpecialFileKind.UNKNOWN)
        val entscheidung = SpecialFilePolicy.isPermitted(u, root, AccessIntent.READ)

        assertFalse("Unbekannt darf nicht als ungefährlich gelten.", entscheidung.allowed)
        assertEquals(AccessRule.UNKNOWN_FILE_TYPE, entscheidung.rule)
    }

    @Test
    fun `eine gewoehnliche Datei ist keine Sonderdatei`() {
        assertTrue(SpecialFilePolicy.isOrdinaryFile(datei()))
        assertTrue(SpecialFilePolicy.isOrdinaryFile(datei(path = "$root/src", kind = SpecialFileKind.DIRECTORY)))
        assertFalse(SpecialFilePolicy.isOrdinaryFile(datei(kind = SpecialFileKind.SOCKET)))
    }

    @Test
    fun `nur Verweise muessen vor dem Zugriff aufgeloest werden`() {
        assertTrue(SpecialFilePolicy.needsResolution(verweis()))
        assertTrue(
            SpecialFilePolicy.needsResolution(
                datei(kind = SpecialFileKind.HARD_LINK, resolvedTarget = "$root/x")
            )
        )
        assertFalse(SpecialFilePolicy.needsResolution(datei()))
    }

    // ── Git- und Anbietergrenzen ──────────────────────────────────────

    @Test
    fun `ein Verweis auf eine Git-Datei einer fremden Quelle wird abgelehnt`() {
        // `git status` folgt Verweisen in .git — ein Link dort darf nicht
        // aus dem Projekt hinausführen.
        val v = verweis(path = "$root/.git/objects/info/alternates", ziel = "/sdcard/anderes-repo/objects")
        assertFalse(SpecialFilePolicy.permits(v, root, AccessIntent.READ))
    }

    @Test
    fun `eine harte Verknuepfung muss ebenfalls aufgeloest werden`() {
        val hart = datei(
            path = "$root/app/secrets.txt",
            kind = SpecialFileKind.HARD_LINK,
            resolvedTarget = "/data/data/com.other.app/files/keystore"
        )
        assertFalse(SpecialFilePolicy.permits(hart, root, AccessIntent.WRITE))
    }

    @Test
    fun `eine harte Verknuepfung ohne Ziel wird abgelehnt`() {
        val hart = datei(path = "$root/app/x", kind = SpecialFileKind.HARD_LINK, resolvedTarget = null)
        val entscheidung = SpecialFilePolicy.isPermitted(hart, root, AccessIntent.READ)
        assertFalse(entscheidung.allowed)
        assertEquals(AccessRule.UNRESOLVED_LINK, entscheidung.rule)
    }

    @Test
    fun `ein Anbieterpfad ausserhalb des Projekts wird abgelehnt`() {
        // Android-Dateianbieter liefern Pfade außerhalb des Projektordners;
        // die gelten nicht als „irgendwie erreichbar".
        val anbieter = datei(path = "content://com.android.providers.downloads/document/42")
        assertFalse(SpecialFilePolicy.permits(anbieter, root, AccessIntent.READ))
    }

    // ── Zugriff bleibt gewöhnlich möglich ─────────────────────────────

    @Test
    fun `eine gewoehnliche Datei im Projekt ist erlaubt`() {
        val e = SpecialFilePolicy.isPermitted(datei(), root, AccessIntent.READ)
        assertTrue(e.allowed)
        assertEquals(AccessRule.ORDINARY_FILE, e.rule)
    }

    @Test
    fun `ein Unterordner des Projekts ist erlaubt`() {
        assertTrue(SpecialFilePolicy.permits(datei(path = "$root/src"), root, AccessIntent.WRITE))
    }

    @Test
    fun `ein Nachbarordner mit aehnlichem Namen ist abgelehnt`() {
        assertFalse(SpecialFilePolicy.permits(datei(path = "/storage/emulated/0/Documents/Projekt-2/x"), root, AccessIntent.READ))
    }

    @Test
    fun `jede Absicht wird getrennt geprueft`() {
        AccessIntent.entries.forEach { absicht ->
            val d = SpecialFilePolicy.isPermitted(verweis(ziel = "/data/privat"), root, absicht)
            assertFalse("$absicht darf nicht erlaubt sein.", d.allowed)
        }
    }

    // ── Begründung ────────────────────────────────────────────────────

    @Test
    fun `jede Ablehnung nennt einen Grund`() {
        val faelle = listOf(
            datei(kind = SpecialFileKind.UNKNOWN),
            datei(kind = SpecialFileKind.DEVICE),
            verweis(ziel = null),
            verweis(ziel = "/data/data/other")
        )
        faelle.forEach { datei ->
            val e = SpecialFilePolicy.isPermitted(datei, root, AccessIntent.READ)
            assertFalse(e.reason.isBlank())
        }
    }

    @Test
    fun `die erklaerung nennt Pfad Art und Entscheidung`() {
        val v = verweis(ziel = "/data/data/other")
        val e = SpecialFilePolicy.isPermitted(v, root, AccessIntent.READ)
        val zeilen = SpecialFilePolicy.explanation(v, e)

        assertTrue(zeilen.any { it.contains("linked") })
        assertTrue(zeilen.any { it.contains("Verweis") })
        assertTrue(zeilen.any { it.contains("abgelehnt") })
    }

    @Test
    fun `ein leerer Pfad kann keine Datei beschreiben`() {
        try {
            SpecialFile("  ", SpecialFileKind.REGULAR_FILE)
            org.junit.Assert.fail("Ein leerer Pfad darf keinen Zugriff beschreiben.")
        } catch (erwartet: IllegalArgumentException) {
            // erwartet
        }
    }
}