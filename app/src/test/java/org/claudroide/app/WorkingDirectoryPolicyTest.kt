package org.claudroide.app

import org.claudroide.app.feature.project.ProviderCapability
import org.claudroide.app.feature.project.WorkingDirectoryPolicy
import org.claudroide.app.feature.project.WorkingDirectoryVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 106 — "Bound the working directory".
 *
 * The interesting cases are the ones that look harmless: `build/../..`,
 * `/system/../system`, and a project folder that is itself a system path.
 */
class WorkingDirectoryPolicyTest {

    private val projekt = "/storage/emulated/0/Documents/Projekt"

    private fun ok() = ProviderCapability(canList = true, canRead = true, canWrite = true)

    private fun resolve(pfad: String, basis: String = projekt, cap: ProviderCapability? = null) =
        WorkingDirectoryPolicy.resolve(basis, pfad, cap)

    // ── Pfadaufloesung ──────────────────────────────────────────────────────

    @Test
    fun `Punkte im Pfad werden aufgeloest`() {
        assertEquals("/a/c", WorkingDirectoryPolicy.normalise("/a/./b/../c"))
    }

    @Test
    fun `doppelte Schraegstriche werden entfernt`() {
        assertEquals("/a/b", WorkingDirectoryPolicy.normalise("/a//b"))
    }

    @Test
    fun `ein Ausstieg ueber die Wurzel hinaus ergibt keinen Pfad`() {
        // Es gibt diesen Pfad nicht. Etwas zurueckzugeben, das benutzbar aussieht,
        // waere schlimmer als null.
        assertNull(WorkingDirectoryPolicy.normalise("/a/../.."))
        assertNull(WorkingDirectoryPolicy.normalise("../a"))
    }

    @Test
    fun `Backslash werden wie Schraegstriche behandelt`() {
        assertEquals("/a/b", WorkingDirectoryPolicy.normalise("\\a\\b"))
    }

    @Test
    fun `ein leerer Pfad ergibt nichts`() {
        assertNull(WorkingDirectoryPolicy.normalise("   "))
    }

    @Test
    fun `ein relativer Pfad bleibt relativ aufgeloest`() {
        assertEquals("a/b", WorkingDirectoryPolicy.normalise("a/./b"))
    }

    // ── Kein Ausstieg aus dem erlaubten Bereich ─────────────────────────────

    @Test
    fun `der Projektordner selbst ist erlaubt`() {
        assertTrue(resolve(".").isAllowed)
        assertTrue(resolve(projekt).isAllowed)
    }

    @Test
    fun `ein Unterordner ist erlaubt`() {
        assertTrue(resolve("app/src/main").isAllowed)
    }

    @Test
    fun `ein relativer Ausstieg wird abgelehnt`() {
        // Zwei Ebenen hoeher landen in /storage/emulated/0 — ein gueltiger Pfad,
        // aber ausserhalb. Dafuer gibt es ein eigenes Urteil.
        val r = resolve("../..")
        assertEquals(WorkingDirectoryVerdict.BLOCKED, r.verdict)
        assertFalse(r.isAllowed)
    }

    @Test
    fun `ein Ausstieg ueber die Dateisystemwurzel hinaus wird als solcher gemeldet`() {
        // Sechs Ebenen fuer einen fuenfteiligen Projektpfad: mehr "..", als der
        // Pfad Segmente hat, ergibt ueberhaupt keinen Pfad mehr.
        val r = resolve("../../../../../..")
        assertEquals(WorkingDirectoryVerdict.ESCAPES_WORKING_DIRECTORY, r.verdict)
        assertTrue(r.reason.contains("leads out"))
    }

    @Test
    fun `ein Ausstieg bis zur Wurzel wird als Systempfad abgelehnt`() {
        // Fuenf Ebenen aus einem fuenfteiligen Pfad ergeben genau "/".
        val r = resolve("../../../../..")
        assertEquals(WorkingDirectoryVerdict.SYSTEM_PATH_REFUSED, r.verdict)
    }

    @Test
    fun `ein Ausstieg mit Punkten im Pfad wird abgelehnt`() {
        // Sieht harmlos aus und ist es nicht.
        val r = resolve("app/../../Fremd")
        assertEquals(WorkingDirectoryVerdict.BLOCKED, r.verdict)
    }

    @Test
    fun `ein absoluter fremder Pfad wird abgelehnt`() {
        assertEquals(WorkingDirectoryVerdict.BLOCKED, resolve("/storage/emulated/0/Fremd").verdict)
    }

    @Test
    fun `ein Ordner mit aehnlichem Namen gilt nicht als drin`() {
        val r = resolve("/storage/emulated/0/Documents/Projekt2")
        assertEquals(WorkingDirectoryVerdict.BLOCKED, r.verdict)
    }

    @Test
    fun `die Ablehnung nennt den erlaubten Bereich im Klartext`() {
        assertTrue(resolve("/andere/x").reason.contains("Projekt"))
    }

    @Test
    fun `ein Unterordner wird als solcher erkannt`() {
        assertTrue(WorkingDirectoryPolicy.subfolderOf(projekt, "app"))
        assertFalse(WorkingDirectoryPolicy.subfolderOf(projekt, "."))
    }

    // ── Systembereiche bleiben gesperrt ─────────────────────────────────────

    @Test
    fun `das Wurzelverzeichnis wird abgelehnt`() {
        assertEquals(WorkingDirectoryVerdict.SYSTEM_PATH_REFUSED, resolve("/").verdict)
    }

    @Test
    fun `Systemordner werden abgelehnt`() {
        listOf("/system", "/vendor", "/apex", "/proc", "/data").forEach {
            assertEquals(it, WorkingDirectoryVerdict.SYSTEM_PATH_REFUSED, resolve(it).verdict)
        }
    }

    @Test
    fun `ein Systemordner mit Unterpfad wird abgelehnt`() {
        assertTrue(resolve("/system/framework").reason.contains("System"))
    }

    @Test
    fun `ein aehnlich benannter Benutzerordner wird nicht abgelehnt`() {
        // /systematic darf nicht als /system gelten.
        assertTrue(resolve("/storage/emulated/0/systematic").isAllowed == false)
        assertFalse(WorkingDirectoryPolicy.isSystemPath("/systematic"))
    }

    @Test
    fun `ein Systempfad der sich zurueckwindet wird abgelehnt`() {
        assertEquals(WorkingDirectoryVerdict.SYSTEM_PATH_REFUSED, resolve("/system/../system").verdict)
    }

    @Test
    fun `die Freigabe eines Systemordners als Projekt wird abgelehnt`() {
        // Sonst wuerde eine Fehlwahl das ganze Dateisystem oeffnen und jede
        // spaetere Pruefung liefe durch.
        val r = resolve(".", basis = "/system")
        assertEquals(WorkingDirectoryVerdict.SYSTEM_PATH_REFUSED, r.verdict)
    }

    // ── Nicht nutzbare Dateianbieter ────────────────────────────────────────

    @Test
    fun `ein nicht auflistbarer Anbieter wird gemeldet`() {
        val r = resolve(".", cap = ProviderCapability(false, true, true, "cloud folder not indexed"))
        assertEquals(WorkingDirectoryVerdict.PROVIDER_NOT_USABLE, r.verdict)
        assertTrue(r.reason.contains("cloud folder not indexed"))
    }

    @Test
    fun `ein nicht beschreibbarer Anbieter wird gemeldet`() {
        val r = resolve(".", cap = ProviderCapability(true, true, false, "read-only source"))
        assertEquals(WorkingDirectoryVerdict.PROVIDER_NOT_USABLE, r.verdict)
        assertTrue(r.reason.contains("read-only source"))
    }

    @Test
    fun `ohne Angabe des Anbieters wird nichts erfunden`() {
        assertTrue(resolve(".", cap = null).isAllowed)
    }

    @Test
    fun `ein vollstaendiger Anbieter wird akzeptiert`() {
        assertTrue(resolve(".", cap = ok()).isAllowed)
    }

    @Test
    fun `der fehlende Grund wird benannt wenn keiner geliefert wurde`() {
        val c = ProviderCapability(true, false, true)
        assertEquals("files cannot be read", c.missingReason())
    }

    @Test
    fun `ein vollstaendiger Anbieter hat keinen fehlenden Grund`() {
        assertEquals("", ok().missingReason())
    }

    // ── Anzeige ─────────────────────────────────────────────────────────────

    @Test
    fun `die Anzeige nennt Rohpfad aufgeloesten Pfad und Urteil`() {
        val text = WorkingDirectoryPolicy.describe(resolve("app")).joinToString("\n")
        assertTrue(text.contains("Folder: app"))
        assertTrue(text.contains("Resolved to: $projekt/app"))
        assertTrue(text.contains("Verdict: allowed"))
    }

    @Test
    fun `die Anzeige nennt einen nicht aufloesbaren Pfad ehrlich`() {
        val text = WorkingDirectoryPolicy.describe(resolve("../../../../../..")).joinToString("\n")
        assertTrue(text.contains("not resolvable"))
    }

    // ── Die Pruefung fuehrt nichts aus ──────────────────────────────────────

    @Test
    fun `die Regel oeffnet und startet nichts`() {
        val verboten = listOf("open", "list", "read", "write", "execute", "run", "create")
        val methoden = WorkingDirectoryPolicy::class.java.methods
            .map { it.name.lowercase() }
            .filterNot { it == "tostring" || it == "hashcode" || it == "equals" || it.startsWith("copy") }
        assertTrue(methoden.none { m -> verboten.any { m.startsWith(it) } })
    }
}
