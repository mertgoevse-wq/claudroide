package org.claudroide.app

import org.claudroide.app.feature.agent.OnDeviceCommandKind
import org.claudroide.app.feature.agent.OnDeviceCommandOutcome
import org.claudroide.app.feature.agent.OnDeviceCommandStatus
import org.claudroide.app.feature.agent.OnDeviceCommandSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 105 — "Check commands on Android".
 *
 * The single rule worth testing hard: an in-process test run must never be
 * presented as a build, however well it went.
 */
class OnDeviceCommandSupportTest {

    private fun inProcessTests(
        status: OnDeviceCommandStatus = OnDeviceCommandStatus.FINISHED,
        completed: Int = 1245,
        failed: Int = 0
    ) = OnDeviceCommandOutcome(
        kind = OnDeviceCommandKind.IN_PROCESS_UNIT_TESTS,
        status = status,
        command = "in-process unit tests",
        completedCount = completed,
        failedCount = failed
    )

    private fun joined(lines: List<String>) = lines.joinToString("\n")

    // ── Ein Testlauf ist kein Build ──────────────────────────────────────────

    @Test
    fun `ein Lauf im Prozess ist kein Build`() {
        val o = inProcessTests()
        assertFalse(o.kind.isRealBuild)
        assertFalse(o.mayBeCalledABuild)
    }

    @Test
    fun `ein perfekter Lauf im Prozess bleibt kein Build`() {
        // Der ganze Sinn dieser Regel: Ein gutes Ergebnis darf die Aussage nicht
        // in einen Build verwandeln.
        val o = inProcessTests(status = OnDeviceCommandStatus.FINISHED, completed = 9999, failed = 0)
        assertFalse(o.mayBeCalledABuild)
    }

    @Test
    fun `jede Zeile nennt beim In-Prozess-Lauf dass nichts gebaut wurde`() {
        val text = joined(OnDeviceCommandSupport.reportLines(inProcessTests()))
        assertTrue(text.contains("did not build anything"))
    }

    @Test
    fun `keine Zeile behauptet einen Build`() {
        OnDeviceCommandSupport.reportLines(inProcessTests()).forEach {
            assertFalse(it, it.contains(OnDeviceCommandSupport.forbiddenClaim))
        }
    }

    @Test
    fun `auch ein abgebrochener Lauf wird nicht als Build bezeichnet`() {
        val o = inProcessTests(status = OnDeviceCommandStatus.ABORTED, completed = 10)
        assertFalse(o.mayBeCalledABuild)
    }

    // ── Ein echter Build braucht einen echten Abschluss ─────────────────────

    @Test
    fun `ein vollstaendiger entfernter Build darf Build genannt werden`() {
        val o = OnDeviceCommandOutcome(
            kind = OnDeviceCommandKind.REMOTE_BUILD,
            status = OnDeviceCommandStatus.FINISHED,
            command = "gradle assembleDebug",
            completedCount = 1
        )
        assertTrue(o.mayBeCalledABuild)
    }

    @Test
    fun `ein abgebrochener entfernter Build darf es nicht`() {
        val o = OnDeviceCommandOutcome(
            kind = OnDeviceCommandKind.REMOTE_BUILD,
            status = OnDeviceCommandStatus.ABORTED,
            command = "gradle assembleDebug"
        )
        assertFalse(o.mayBeCalledABuild)
    }

    @Test
    fun `die Anzeige sagt warum ein entfernter Build kein Build-Ergebnis ist`() {
        val o = OnDeviceCommandOutcome(
            kind = OnDeviceCommandKind.REMOTE_BUILD,
            status = OnDeviceCommandStatus.PARTIAL,
            command = "gradle assembleDebug"
        )
        assertTrue(joined(OnDeviceCommandSupport.reportLines(o)).contains("did not finish"))
    }

    @Test
    fun `ein abgeschlossener Lauf mit Fehlern wird nicht als vollendet angenommen`() {
        val fehler = runCatching {
            OnDeviceCommandOutcome(
                kind = OnDeviceCommandKind.GRADLE_BUILD,
                status = OnDeviceCommandStatus.FINISHED,
                command = "gradle",
                completedCount = 1,
                failedCount = 1
            )
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `negative Zahlen werden abgelehnt`() {
        val fehler = runCatching {
            OnDeviceCommandOutcome(
                kind = OnDeviceCommandKind.SIMPLE_COMMAND,
                status = OnDeviceCommandStatus.FINISHED,
                command = "ls",
                completedCount = -1
            )
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    // ── Was diese App ueberhaupt anbietet ───────────────────────────────────

    @Test
    fun `ein Gradle-Build auf dem Geraet ist als moeglich eingestuft`() {
        assertFalse(OnDeviceCommandSupport.CAN_RUN_GRADLE_ON_DEVICE)
    }

    @Test
    fun `ein Gradle-Build wird nicht als unterstuetzt angeboten`() {
        assertFalse(OnDeviceCommandSupport.isSupported(OnDeviceCommandKind.GRADLE_BUILD))
    }

    @Test
    fun `der In-Prozess-Testlauf wird angeboten`() {
        assertTrue(OnDeviceCommandSupport.isSupported(OnDeviceCommandKind.IN_PROCESS_UNIT_TESTS))
    }

    @Test
    fun `jede angebotene Art laesst sich von einer nicht angebotenen unterscheiden`() {
        OnDeviceCommandKind.values().forEach { art ->
            val erwartet = art == OnDeviceCommandKind.IN_PROCESS_UNIT_TESTS ||
                art == OnDeviceCommandKind.SIMPLE_COMMAND
            assertEquals(
                art.name,
                erwartet,
                OnDeviceCommandSupport.isSupported(art)
            )
        }
    }

    // ── Der Bericht wiederholt die Grenze ───────────────────────────────────

    @Test
    fun `der Faehigkeitsbericht nennt die Grenze ausdruecklich`() {
        val text = joined(OnDeviceCommandSupport.capabilityLines())
        assertTrue(text.contains("cannot run a Gradle build"))
        assertTrue(text.contains("not evidence that your project builds"))
    }

    @Test
    fun `der Faehigkeitsbericht nennt den entfernten Weg als Entscheidung des Nutzers`() {
        assertTrue(
            joined(OnDeviceCommandSupport.capabilityLines())
                .contains("your decision to make")
        )
    }

    @Test
    fun `der Bericht nennt beide Quellen`() {
        val text = joined(OnDeviceCommandSupport.capabilityLines())
        assertTrue(text.contains(OnDeviceCommandSupport.AVF_DOC_URL))
        assertTrue(text.contains(OnDeviceCommandSupport.AVF_USECASES_DOC_URL))
    }

    @Test
    fun `der Bericht verweist auf den ausfuehrlichen Bericht`() {
        assertTrue(
            joined(OnDeviceCommandSupport.capabilityLines())
                .contains(OnDeviceCommandSupport.FEASIBILITY_DOC)
        )
    }

    @Test
    fun `ohne Ausfuehrung sagt die Anzeige das auch`() {
        val o = OnDeviceCommandOutcome(
            kind = OnDeviceCommandKind.GRADLE_BUILD,
            status = OnDeviceCommandStatus.NOT_ATTEMPTED,
            command = "gradle"
        )
        assertTrue(joined(OnDeviceCommandSupport.reportLines(o)).contains("Nothing was run"))
    }

    @Test
    fun `ein Schluessel im Befehl wird geschwaerzt`() {
        val o = OnDeviceCommandOutcome(
            kind = OnDeviceCommandKind.SIMPLE_COMMAND,
            status = OnDeviceCommandStatus.FINISHED,
            command = "curl -H 'auth: sk-ant-abcdefgh12345678' x",
            completedCount = 1
        )
        assertFalse(joined(OnDeviceCommandSupport.reportLines(o)).contains("sk-ant-abcdefgh12345678"))
    }

    @Test
    fun `die Unterstuetzung startet selbst nichts`() {
        val verboten = listOf("execute", "run", "start", "spawn", "process", "shell")
        val methoden = OnDeviceCommandSupport::class.java.methods
            .map { it.name.lowercase() }
            .filterNot { it == "tostring" || it == "hashcode" || it == "equals" }
        assertTrue(methoden.none { m -> verboten.any { m.startsWith(it) } })
    }
}
