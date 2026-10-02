package org.claudroide.app

import org.claudroide.app.feature.skills.CompatibilityVerdict
import org.claudroide.app.feature.skills.EnvironmentAudit
import org.claudroide.app.feature.skills.NetworkBehaviour
import org.claudroide.app.feature.skills.PermissionScope
import org.claudroide.app.feature.skills.RequiredPermission
import org.claudroide.app.feature.skills.RequiredProgram
import org.claudroide.app.feature.skills.SkillEnvironmentCheck
import org.claudroide.app.feature.skills.SkillFormat
import org.claudroide.app.feature.skills.SkillUsability
import org.claudroide.app.feature.skills.TargetEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 130 — „Skill-Kompatibilität".
 *
 * Die zwei Zusage der Aufgabe stehen vorn: Inkompatibles wird nicht als direkt
 * nutzbar eingeplant, und ausführbare Inhalte werden vor dem Start einzeln
 * geprüft. Die Testfälle verwenden **echte Befunde** aus dem Dateisystem der
 * installierten Skills, nicht erfundene.
 */
class SkillCompatibilityTest {

    // ── Format ──────────────────────────────────────────────────────────

    @Test
    fun `nur Markdown mit Frontmatter ist direkt nutzbar`() {
        assertEquals(
            CompatibilityVerdict.COMPATIBLE,
            SkillEnvironmentCheck.formatVerdict(SkillFormat.MARKDOWN_FRONTMATTER)
        )
    }

    @Test
    fun `reines Markdown ohne Frontmatter ist nicht dasselbe wie mit`() {
        // Kein name und keine description: ein Werkzeug findet den Skill nicht
        // zuverlaessig. Das ist eine offene Frage, keine Kompatibilitaet.
        assertEquals(
            CompatibilityVerdict.NOT_ASSESSED,
            SkillEnvironmentCheck.formatVerdict(SkillFormat.MARKDOWN_PLAIN)
        )
    }

    @Test
    fun `ein ausfuehrbares Programm ist nie direkt nutzbar`() {
        assertEquals(
            CompatibilityVerdict.NOT_ASSESSED,
            SkillEnvironmentCheck.formatVerdict(SkillFormat.EXECUTABLE)
        )
    }

    // ── Zusage 1: nichts wird blind ausgefuehrt ─────────────────────────

    @Test
    fun `der Inhalt muss immer einzeln geprueft werden`() {
        assertTrue(SkillEnvironmentCheck.requiresIndividualInspection())
    }

    @Test
    fun `eine Pruefung ohne Fundort hat nichts geprueft`() {
        val ohneOrt = EnvironmentAudit(
            source = "",
            name = "android-profiler",
            licenseVerified = true,
            format = SkillFormat.MARKDOWN_FRONTMATTER,
            executesNothing = true,
            downloadsNothing = true,
            network = NetworkBehaviour.NONE,
            lastInspectedAt = 1_700_000_000_000L
        )
        assertFalse(ohneOrt.isGrounded)
        assertEquals(
            SkillUsability.NEEDS_INSPECTION,
            SkillEnvironmentCheck.usability(ohneOrt)
        )
    }

    @Test
    fun `eine Pruefung ohne Zeitangabe gilt als nicht durchgefuehrt`() {
        val ohneZeit = vollePruefung().copy(lastInspectedAt = 0L)
        assertFalse(ohneZeit.isGrounded)
        assertEquals(SkillUsability.NEEDS_INSPECTION, SkillEnvironmentCheck.usability(ohneZeit))
    }

    @Test
    fun `ein ungeprueftes Netzverhalten ist keine Behauptung von Sicherheit`() {
        // Echter Befund: bei acht der elf Skills stand kein Netzverhalten fest.
        val ungeprueft = vollePruefung().copy(network = NetworkBehaviour.NOT_INSPECTED)
        assertEquals(
            "Ohne Pruefung des Netzes ist kein Nutzungsurteil moeglich.",
            SkillUsability.NEEDS_INSPECTION,
            SkillEnvironmentCheck.usability(ungeprueft)
        )
        assertTrue(
            SkillEnvironmentCheck.reason(ungeprueft, TargetEnvironment.CLAUDROIDE_ANDROID)
                .contains("Netzverhalten")
        )
    }

    @Test
    fun `eine fehlende Zeitangabe erzeugt keine Attrappe einer Pruefung`() {
        val zeitlos = vollePruefung().copy(lastInspectedAt = 0L)
        assertEquals(
            SkillUsability.NEEDS_INSPECTION,
            SkillEnvironmentCheck.usability(zeitlos)
        )
    }

    // ── Echter Befund: android-profiler laedt Google-Binaries ───────────

    @Test
    fun `android-profiler wird wegen nachladendem Inhalt abgelehnt`() {
        // Echter Befund vom Dateisystem:
        //   curl -O "$TOOLS_URL/java_heap_dump" && chmod +x java_heap_dump
        // mit TOOLS_URL="https://raw.githubusercontent.com/google/perfetto/main/tools"
        val profil = vollePruefung().copy(
            name = "android-profiler",
            licenseVerified = false,
            downloadsNothing = false,
            requiredPrograms = listOf(
                RequiredProgram("perfetto", isAvailable = true),
                RequiredProgram("java_heap_dump", isAvailable = false, reason = "nur per curl nachladbar")
            )
        )
        assertTrue("Der Inhalt laedt nach.", profil.isSuspicious)
        assertEquals(SkillUsability.REJECTED, SkillEnvironmentCheck.usability(profil))
        assertTrue(
            SkillEnvironmentCheck.reason(profil, TargetEnvironment.CLAUDROIDE_ANDROID)
                .contains("android-profiler")
        )
    }

    @Test
    fun `eine belegte Lizenz rettet einen ausfuehrenden Inhalt nicht`() {
        // compose-kotlin-agent-skills hat die einzige belegte MIT-Lizenz.
        // Das aendert nichts daran, dass sein Inhalt laedt und ausfuehrt.
        val mitLizenz = vollePruefung().copy(
            name = "compose-kotlin-agent-skills",
            licenseVerified = true,
            executesNothing = false
        )
        assertEquals(
            "Die Lizenz entscheidet ueber Nutzungsrechte, nicht ueber Sicherheit.",
            SkillUsability.REJECTED,
            SkillEnvironmentCheck.usability(mitLizenz)
        )
        assertTrue(
            SkillEnvironmentCheck.reason(mitLizenz, TargetEnvironment.CLAUDROIDE_ANDROID)
                .contains("führt Befehle aus")
        )
    }

    @Test
    fun `eine unbelegte Lizenz schliesst einen sauberen Inhalt aus`() {
        // Echter Befund: vier Skills verweisen auf LICENSE.txt, die nicht existiert.
        val ohneLizenz = vollePruefung().copy(name = "testing-setup", licenseVerified = false)
        assertEquals(SkillUsability.REJECTED, SkillEnvironmentCheck.usability(ohneLizenz))
        assertTrue(
            SkillEnvironmentCheck.reason(ohneLizenz, TargetEnvironment.CLAUDROIDE_ANDROID)
                .contains("Lizenz")
        )
    }

    // ── Blockiert ist nicht abgelehnt ────────────────────────────────────

    @Test
    fun `ein fehlendes Programm blockiert, lehnt aber nicht ab`() {
        val blockiert = vollePruefung().copy(
            requiredPrograms = listOf(RequiredProgram("docker", isAvailable = false, reason = "nicht installiert"))
        )
        assertEquals(
            "Die Faehigkeit ist gut, die Umgebung fehlt.",
            SkillUsability.BLOCKED,
            SkillEnvironmentCheck.usability(blockiert)
        )
        assertTrue(SkillEnvironmentCheck.reason(blockiert, TargetEnvironment.CLAUDROIDE_ANDROID).contains("docker"))
        assertEquals(listOf("docker"), blockiert.missingPrograms)
    }

    @Test
    fun `ein Bedarf ohne Grund wird als offen gemeldet`() {
        // "Wird gebraucht" ist nicht "ist da" — und ein offener Bedarf traegt
        // keinen Grund, solange keiner eingetragen wurde.
        val ohneGrund = RequiredProgram("node", isAvailable = false, reason = "")
        assertFalse(ohneGrund.explainsItself)
        assertFalse(ohneGrund.complete)
        assertFalse(vollePruefung().copy(requiredPrograms = listOf(ohneGrund)).isComplete)
    }

    @Test
    fun `ein erfuellter Bedarf braucht keinen Grund`() {
        val da = RequiredProgram("adb", isAvailable = true)
        assertTrue(da.explainsItself)
        assertTrue(da.complete)
    }

    // ── Der saubere Fall ─────────────────────────────────────────────────

    @Test
    fun `ein vollstaendig gepruefter anweisungstext ist einsetzbar`() {
        val sauber = vollePruefung()
        assertTrue(sauber.isComplete)
        assertFalse(sauber.isSuspicious)
        assertEquals(SkillUsability.USABLE, SkillEnvironmentCheck.usability(sauber))
        assertTrue(SkillEnvironmentCheck.reason(sauber, TargetEnvironment.CLAUDROIDE_ANDROID).contains("einsetzbar"))
    }

    @Test
    fun `ein vollstaendig gepruefter anweisungstext darf eingeplant werden`() {
        assertTrue(
            SkillEnvironmentCheck.isPlannable(vollePruefung(), TargetEnvironment.CLAUDROIDE_ANDROID)
        )
    }

    @Test
    fun `ein abgelehnter Skill wird nicht eingeplant`() {
        val abgelehnt = vollePruefung().copy(licenseVerified = false)
        assertFalse(
            "Inkompatibel heisst: nicht als direkt nutzbar einplanen.",
            SkillEnvironmentCheck.isPlannable(abgelehnt, TargetEnvironment.CLAUDROIDE_ANDROID)
        )
    }

    // ── Aktualitaet ─────────────────────────────────────────────────────

    @Test
    fun `eine alte Pruefung ist keine Entwarnung fuer geaenderten Inhalt`() {
        val alt = vollePruefung().copy(lastInspectedAt = 1_000L)
        assertTrue(SkillEnvironmentCheck.isStale(alt, now = 5_000L, maxAgeMillis = 1_000L))
        assertFalse(SkillEnvironmentCheck.isStale(alt, now = 1_500L, maxAgeMillis = 1_000L))
    }

    @Test
    fun `ohne Zeitangabe gilt nichts als veraltet oder frisch`() {
        // now <= 0 bedeutet "keine Zeitangabe", nicht "unendlich alt".
        val zeitlos = vollePruefung().copy(lastInspectedAt = 0L)
        assertFalse(SkillEnvironmentCheck.isStale(zeitlos, now = 0L, maxAgeMillis = 1_000L))
        assertTrue(SkillEnvironmentCheck.isStale(zeitlos, now = 5_000L, maxAgeMillis = 1_000L))
    }

    @Test
    fun `eine nicht positive maximale Dauer behauptet keine Aktualitaet`() {
        assertFalse(
            SkillEnvironmentCheck.isStale(vollePruefung(), now = 9_000L, maxAgeMillis = 0L)
        )
    }

    // ── Jede Begruendung nennt den Skill ────────────────────────────────

    @Test
    fun `jede Begruendung nennt den Skill und faellt nicht leer aus`() {
        val faelle = listOf(
            vollePruefung(),
            vollePruefung().copy(licenseVerified = false),
            vollePruefung().copy(executesNothing = false),
            vollePruefung().copy(downloadsNothing = false),
            vollePruefung().copy(
                requiredPrograms = listOf(RequiredProgram("docker", false, "fehlt"))
            ),
            vollePruefung().copy(network = NetworkBehaviour.NOT_INSPECTED),
            vollePruefung().copy(source = "")
        )
        faelle.forEach { pruefung ->
            val grund = SkillEnvironmentCheck.reason(pruefung, TargetEnvironment.CLAUDROIDE_ANDROID)
            assertTrue("Leerer Grund fuer ${pruefung.name}", grund.isNotBlank())
            assertTrue(
                "Der Grund nennt '${pruefung.name}' nicht: $grund",
                grund.contains(pruefung.name)
            )
        }
    }

    // ── Rechte: Projektgrenze ───────────────────────────────────────────
    //
    // Der Schutz der Aufgabe lautet: "Keine fremde Faehigkeit mit Projekt- oder
    // globalen Rechten blind ausfuehren." Beide Rechte stehen darin — der
    // Unterschied liegt nur darin, ob die vorhandene Projektgrenze sie begrenzt.
    // Deshalb wird hier beides getrennt geprueft: ein projektbezogenes Recht ist
    // kein Grund zur Sorge, ein globales schon.

    @Test
    fun `ein projektbezogenes Recht verlaesst die Projektgrenze nicht`() {
        // Echter Befund aus dem Projekt: der Projektordner ist
        // /storage/emulated/0/Documents/Projekt, ein Zugriff darauf bleibt
        // begrenzt. "Er braucht ein Recht" ist nicht "er verlaesst das Projekt".
        val projektRecht = vollePruefung().copy(
            name = "testing-setup",
            requiredPermissions = listOf(
                RequiredPermission("Schreiben im Projektordner", PermissionScope.PROJECT)
            )
        )
        assertFalse(
            "Durch die Projektgrenze begrenzte Rechte sind kein Grund zur Sorge.",
            SkillEnvironmentCheck.escapesProjectBoundary(projektRecht)
        )
        assertEquals(
            emptyList<String>(),
            projektRecht.globalPermissions
        )
    }

    @Test
    fun `die Oberfläche schweigt, wenn kein globales Recht vorliegt`() {
        // Die Warnung darf nicht "Achtung" sagen und dann nichts nennen. Leer
        // heisst hier: es gibt nichts zu nennen.
        val projektRecht = vollePruefung().copy(
            requiredPermissions = listOf(
                RequiredPermission("Schreiben im Projektordner", PermissionScope.PROJECT)
            )
        )
        assertEquals(
            "Keine Warnung ohne globals Recht.",
            "",
            SkillEnvironmentCheck.boundaryWarning(projektRecht)
        )
    }

    @Test
    fun `ein globales Recht verlaesst die Projektgrenze und wird benannt`() {
        // Echter Befund aus dem Projekt: die globalen Skills liegen unter
        // ~/.claude/skills/ — das liegt in keinem Projektordner und bleibt
        // deshalb nicht begrenzt.
        val globalesRecht = vollePruefung().copy(
            name = "graphify",
            requiredPermissions = listOf(
                RequiredPermission("Schreiben unter ~/.claude/skills", PermissionScope.GLOBAL)
            )
        )
        assertTrue(
            "Ein globales Recht hat keine vorhandene Schranke.",
            SkillEnvironmentCheck.escapesProjectBoundary(globalesRecht)
        )
        val warnung = SkillEnvironmentCheck.boundaryWarning(globalesRecht)
        assertTrue("Die Warnung nennt das Recht nicht: $warnung", warnung.isNotBlank())
        assertTrue(
            "Die Warnung nennt das Recht nicht beim Namen: $warnung",
            warnung.contains("Schreiben unter ~/.claude/skills")
        )
    }

    @Test
    fun `die Warnung nennt jedes globale Recht und kein projektbezogenes`() {
        val gemischt = vollePruefung().copy(
            requiredPermissions = listOf(
                RequiredPermission("Lesen im Projektordner", PermissionScope.PROJECT),
                RequiredPermission("Schreiben unter ~/.claude/skills", PermissionScope.GLOBAL),
                RequiredPermission("Lesen unter /storage/emulated/0", PermissionScope.GLOBAL),
                RequiredPermission("Schreiben im Projektordner", PermissionScope.PROJECT)
            )
        )
        assertEquals(
            listOf("Schreiben unter ~/.claude/skills", "Lesen unter /storage/emulated/0"),
            gemischt.globalPermissions
        )
        val warnung = SkillEnvironmentCheck.boundaryWarning(gemischt)
        listOf("Schreiben unter ~/.claude/skills", "Lesen unter /storage/emulated/0").forEach { recht ->
            assertTrue("Die Warnung nennt '$recht' nicht: $warnung", warnung.contains(recht))
        }
        listOf("Lesen im Projektordner", "Schreiben im Projektordner").forEach { projekt ->
            assertFalse(
                "Ein projektbezogenes Recht wird nicht als globals genannt: $warnung",
                warnung.contains(projekt)
            )
        }
    }

    @Test
    fun `ein Recht ohne Namen ist eine offene Angabe, nicht eine Angabe`() {
        val ohneNamen = RequiredPermission("", PermissionScope.GLOBAL)
        assertFalse("Ohne Namen ist das ein leeres Formularfeld.", ohneNamen.complete)
        assertFalse(
            RequiredPermission("   ", PermissionScope.PROJECT).complete
        )
        assertTrue(RequiredPermission("Lesen im Projektordner", PermissionScope.PROJECT).complete)
    }

    @Test
    fun `ein Recht ohne Namen macht die Pruefung unvollstaendig`() {
        // Dasselbe wie bei einem Programm ohne Namen: eine halb ausgefuellte
        // Liste ist keine abgeschlossene Pruefung. Ohne Namen kann die
        // Grenzwarnung nichts benennen — und genau das waere der Fehler, den
        // die Regel "nie leer, wenn escapesProjectBoundary true" verhindert.
        val unvollstaendig = vollePruefung().copy(
            requiredPermissions = listOf(RequiredPermission("", PermissionScope.GLOBAL))
        )
        assertFalse(unvollstaendig.isComplete)
        assertEquals(
            "Eine unvollstaendige Pruefung traegt kein Nutzungsurteil.",
            SkillUsability.NEEDS_INSPECTION,
            SkillEnvironmentCheck.usability(unvollstaendig)
        )
    }

    @Test
    fun `vollstaendig benannte Rechte lassen die Pruefung vollstaendig`() {
        val mitRechten = vollePruefung().copy(
            requiredPermissions = listOf(
                RequiredPermission("Lesen im Projektordner", PermissionScope.PROJECT),
                RequiredPermission("Schreiben unter ~/.claude/skills", PermissionScope.GLOBAL)
            )
        )
        assertTrue(mitRechten.isComplete)
    }

    @Test
    fun `Projektgrenze und Nutzbarkeit sind zwei getrennte Fragen`() {
        // Der Sonderfall aus dem KDoc von escapesProjectBoundary: ein Skill mit
        // globalem Recht darf trotzdem USABLE sein. Verhindert wird nur das
        // Ausfuehren OHNE Einzelfallpruefung — und das ist eine dritte Frage.
        val globalUndGeprueft = vollePruefung().copy(
            name = "graphify",
            requiredPermissions = listOf(
                RequiredPermission("Schreiben unter ~/.claude/skills", PermissionScope.GLOBAL)
            )
        )
        assertTrue(
            SkillEnvironmentCheck.escapesProjectBoundary(globalUndGeprueft)
        )
        assertEquals(
            "Geprueft und lizenziert heisst einsetzbar — trotz globalem Recht.",
            SkillUsability.USABLE,
            SkillEnvironmentCheck.usability(globalUndGeprueft)
        )
        assertTrue(
            SkillEnvironmentCheck.isPlannable(globalUndGeprueft, TargetEnvironment.CLAUDROIDE_ANDROID)
        )
        assertTrue(
            "Das blinde Ausfuehren bleibt trotzdem gesperrt.",
            SkillEnvironmentCheck.requiresIndividualInspection()
        )
    }

    @Test
    fun `die Grenzfrage hebt das Nutzungsurteil nicht auf und umgekehrt`() {
        val projekt = vollePruefung().copy(
            requiredPermissions = listOf(
                RequiredPermission("Lesen im Projektordner", PermissionScope.PROJECT)
            )
        )
        val global = vollePruefung().copy(
            requiredPermissions = listOf(
                RequiredPermission("Schreiben unter ~/.claude/skills", PermissionScope.GLOBAL)
            )
        )
        // Beide sind gleich nutzbar ...
        assertEquals(SkillEnvironmentCheck.usability(projekt), SkillEnvironmentCheck.usability(global))
        // ... aber nur eines verlaesst die Projektgrenze.
        assertFalse(SkillEnvironmentCheck.escapesProjectBoundary(projekt))
        assertTrue(SkillEnvironmentCheck.escapesProjectBoundary(global))
    }

    @Test
    fun `ein abgelehnter Skill wird nicht wegen eines globalen Rechts abgelehnt`() {
        // Die Gegenprobe zur Trennung: die Ablehnung kommt aus der Lizenz,
        // nicht aus dem globalen Recht.
        val ohneLizenz = vollePruefung().copy(
            licenseVerified = false,
            requiredPermissions = listOf(
                RequiredPermission("Schreiben unter ~/.claude/skills", PermissionScope.GLOBAL)
            )
        )
        assertTrue(SkillEnvironmentCheck.escapesProjectBoundary(ohneLizenz))
        assertEquals(
            "Es bleibt eine Ablehnung — aber eine aus der Lizenz.",
            SkillUsability.REJECTED,
            SkillEnvironmentCheck.usability(ohneLizenz)
        )
        assertTrue(
            SkillEnvironmentCheck.reason(ohneLizenz, TargetEnvironment.CLAUDROIDE_ANDROID)
                .contains("Lizenz")
        )
    }

    // ── Hilfe ───────────────────────────────────────────────────────────

    /** Ein Audit, das alle Stufen besteht: belegter Ort, Lizenz, kein Netz. */
    private fun vollePruefung() = EnvironmentAudit(
        source = "~/.claude/skills/android-permissions-security",
        name = "android-permissions-security",
        licenseVerified = true,
        format = SkillFormat.MARKDOWN_FRONTMATTER,
        executesNothing = true,
        downloadsNothing = true,
        network = NetworkBehaviour.NONE,
        lastInspectedAt = 1_700_000_000_000L
    )
}