package org.claudroide.app

import org.claudroide.app.feature.agent.Admission
import org.claudroide.app.feature.agent.AgentConcurrencyLimiter
import org.claudroide.app.feature.agent.BackgroundModelDecision
import org.claudroide.app.feature.agent.DenialReason
import org.claudroide.app.feature.agent.FileAccess
import org.claudroide.app.feature.agent.ResourceAdvice
import org.claudroide.app.feature.agent.ResourceAdvisor
import org.claudroide.app.feature.agent.ResourceSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 078 — „Parallelität begrenzen“.
 *
 * Die Tests prüfen die beiden Fertig-Kriterien an den Grenzen: der
 * Dateikonflikt zwischen zwei gleichzeitigen Schreibzugriffen und die
 * Reaktion auf knappe Ressourcen. Der vierte Block prüft die Schutzregel gegen
 * das automatische Laden großer Modelle im Hintergrund.
 */
class AgentConcurrencyLimiterTest {

    // ── Testhilfen ───────────────────────────────────────────────────────────

    /** Genug Speicher, voller Akku: alles frei. */
    private fun ample(): ResourceSample = ResourceSample(
        availableMemoryBytes = 6_000_000_000L,
        totalMemoryBytes = 8_000_000_000L,
        batteryPercent = 90,
        isCharging = false,
        isMeasured = true
    )

    /** Wenig freier Speicher, aber noch nicht kritisch. */
    private fun tight(): ResourceSample = ResourceSample(
        availableMemoryBytes = 800_000_000L,
        totalMemoryBytes = 8_000_000_000L,
        batteryPercent = 90,
        isCharging = false,
        isMeasured = true
    )

    /** Kritisch wenig freier Speicher. */
    private fun scarce(): ResourceSample = ResourceSample(
        availableMemoryBytes = 300_000_000L,
        totalMemoryBytes = 8_000_000_000L,
        batteryPercent = 90,
        isCharging = false,
        isMeasured = true
    )

    /** Akku fast leer, nicht am Kabel. */
    private fun emptyBattery(): ResourceSample = ResourceSample(
        availableMemoryBytes = 6_000_000_000L,
        totalMemoryBytes = 8_000_000_000L,
        batteryPercent = 8,
        isCharging = false,
        isMeasured = true
    )

    /** Nichts gemessen. */
    private fun unmeasured(): ResourceSample = ResourceSample(
        availableMemoryBytes = 0L,
        totalMemoryBytes = 0L,
        batteryPercent = 0,
        isCharging = false,
        isMeasured = false
    )

    private fun limiter(
        limit: Int = 3,
        sample: ResourceSample = ample()
    ) = AgentConcurrencyLimiter(limit, sample)

    private fun denied(admission: Admission): Admission.Denied {
        assertTrue("Erwartet: $admission", admission is Admission.Denied)
        return admission as Admission.Denied
    }

    private fun admitted(admission: Admission): Admission.Admitted {
        assertTrue("Erwartet: $admission", admission is Admission.Admitted)
        return admission as Admission.Admitted
    }

    // ── Gleichzeitige Dateiänderungen ────────────────────────────────────────

    @Test
    fun `zwei Schreiber auf denselben Pfad werden abgewiesen`() {
        val l = limiter()
        admitted(l.tryAcquire("a1", mapOf("src/A.kt" to FileAccess.WRITE)))

        val d = denied(l.tryAcquire("a2", mapOf("src/A.kt" to FileAccess.WRITE)))

        assertEquals(DenialReason.FILE_CONFLICT, d.reason)
    }

    @Test
    fun `die Ablehnung nennt den stoerenden Pfad und die andere Einheit`() {
        val l = limiter()
        admitted(l.tryAcquire("a1", mapOf("src/A.kt" to FileAccess.WRITE)))

        val d = denied(l.tryAcquire("a2", mapOf("src/A.kt" to FileAccess.WRITE)))

        assertTrue(d.germanExplanation.contains("src/A.kt"))
        assertTrue(d.germanExplanation.contains("a1"))
        assertTrue(d.germanExplanation.contains("überschreiben"))
    }

    @Test
    fun `ein Leser waehrend eines Schreibvorgangs wird abgewiesen`() {
        val l = limiter()
        admitted(l.tryAcquire("a1", mapOf("src/A.kt" to FileAccess.WRITE)))

        val d = denied(l.tryAcquire("a2", mapOf("src/A.kt" to FileAccess.READ)))

        assertEquals(DenialReason.FILE_CONFLICT, d.reason)
    }

    @Test
    fun `zwei Leser desselben Pfads duerfen parallel laufen`() {
        val l = limiter()
        admitted(l.tryAcquire("a1", mapOf("src/A.kt" to FileAccess.READ)))
        admitted(l.tryAcquire("a2", mapOf("src/A.kt" to FileAccess.READ)))
        assertEquals(2, l.activeCount)
    }

    @Test
    fun `verschiedene Pfade storen sich nicht`() {
        val l = limiter()
        admitted(l.tryAcquire("a1", mapOf("src/A.kt" to FileAccess.WRITE)))
        admitted(l.tryAcquire("a2", mapOf("src/B.kt" to FileAccess.WRITE)))
        assertEquals(2, l.activeCount)
    }

    @Test
    fun `nach dem Abmelden ist der Pfad wieder frei`() {
        val l = limiter()
        admitted(l.tryAcquire("a1", mapOf("src/A.kt" to FileAccess.WRITE)))
        assertTrue(l.release("a1"))

        admitted(l.tryAcquire("a2", mapOf("src/A.kt" to FileAccess.WRITE)))
    }

    @Test
    fun `ein Pfad wird nur einmal gehalten`() {
        val l = limiter()
        admitted(l.tryAcquire("a1", mapOf("src/A.kt" to FileAccess.WRITE, "src/B.kt" to FileAccess.WRITE)))
        assertEquals(1, l.statusLines().count { it.contains("a1:") })
    }

    @Test
    fun `der Dateikonflikt wird auch dann genannt wenn das Limit voll ist`() {
        val l = limiter(limit = 1)
        admitted(l.tryAcquire("a1", mapOf("src/A.kt" to FileAccess.WRITE)))

        // Limit ist erschöpft UND der Pfad stört. Der Nutzer soll den Pfad
        // erfahren, nicht nur „Limit erreicht“ — er kann dann entscheiden,
        // auf a1 zu warten, während der Grund sonst im Unklaren bliebe.
        val d = denied(l.tryAcquire("a2", mapOf("src/A.kt" to FileAccess.WRITE)))
        assertEquals(DenialReason.FILE_CONFLICT, d.reason)
        assertTrue(d.germanExplanation.contains("src/A.kt"))
    }

    @Test
    fun `ohne Konflikt greift weiterhin das Limit`() {
        val l = limiter(limit = 1)
        admitted(l.tryAcquire("a1", mapOf("src/A.kt" to FileAccess.WRITE)))
        assertEquals(
            DenialReason.LIMIT_REACHED,
            denied(l.tryAcquire("a2", mapOf("src/B.kt" to FileAccess.WRITE))).reason
        )
    }

    @Test
    fun `ein Konflikt ueber mehrere Pfade nennt den ersten stoerenden`() {
        val l = limiter()
        admitted(l.tryAcquire("a1", mapOf("src/A.kt" to FileAccess.WRITE, "src/B.kt" to FileAccess.WRITE)))

        val d = denied(l.tryAcquire("a2", mapOf("src/Frei.kt" to FileAccess.WRITE, "src/B.kt" to FileAccess.WRITE)))

        assertTrue(d.germanExplanation.contains("src/B.kt"))
        assertFalse(d.germanExplanation.contains("src/Frei.kt"))
    }

    // ── Limit ────────────────────────────────────────────────────────────────

    @Test
    fun `das Limit begrenzt die Zahl gleichzeitiger Einheiten`() {
        val l = limiter(limit = 2)
        admitted(l.tryAcquire("a1"))
        admitted(l.tryAcquire("a2"))

        assertEquals(DenialReason.LIMIT_REACHED, denied(l.tryAcquire("a3")).reason)
    }

    @Test
    fun `die Ablehnung nennt den aktuellen Stand`() {
        val l = limiter(limit = 2)
        admitted(l.tryAcquire("a1"))
        admitted(l.tryAcquire("a2"))

        val d = denied(l.tryAcquire("a3"))
        assertTrue(d.germanExplanation.contains("2 von 2"))
    }

    @Test
    fun `eine Einheit kann nicht zweimal angemeldet werden`() {
        val l = limiter()
        admitted(l.tryAcquire("a1"))
        assertEquals(DenialReason.LIMIT_REACHED, denied(l.tryAcquire("a1")).reason)
        assertEquals(1, l.activeCount)
    }

    @Test
    fun `das Limit muss mindestens eins sein`() {
        val fehler = runCatching { AgentConcurrencyLimiter(0) }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `nach dem Abmelden ist wieder Platz`() {
        val l = limiter(limit = 1)
        admitted(l.tryAcquire("a1"))
        assertEquals(DenialReason.LIMIT_REACHED, denied(l.tryAcquire("a2")).reason)
        l.release("a1")
        admitted(l.tryAcquire("a2"))
    }

    // ── Ressourcen: RAM und Akku ─────────────────────────────────────────────

    @Test
    fun `bei genug Speicher gilt volles Tempo`() {
        assertEquals(ResourceAdvice.FULL_SPEED, ResourceAdvisor.advise(ample()))
    }

    @Test
    fun `knapper Speicher verlangsamt die Arbeit`() {
        assertEquals(ResourceAdvice.THROTTLED, ResourceAdvisor.advise(tight()))
    }

    @Test
    fun `sehr knapper Speicher rät zum Anhalten`() {
        assertEquals(ResourceAdvice.PAUSE_SUGGESTED, ResourceAdvisor.advise(scarce()))
    }

    @Test
    fun `ein fast leerer Akku verlangsamt die Arbeit`() {
        assertEquals(ResourceAdvice.PAUSE_SUGGESTED, ResourceAdvisor.advise(emptyBattery()))
    }

    @Test
    fun `am Kabel darf ein niedriger Akkustand nicht bremsen`() {
        val amKabel = emptyBattery().copy(batteryPercent = 5, isCharging = true)
        assertEquals(ResourceAdvice.FULL_SPEED, ResourceAdvisor.advise(amKabel))
    }

    @Test
    fun `ohne Messung wird nichts behauptet`() {
        assertEquals(ResourceAdvice.UNKNOWN, ResourceAdvisor.advise(unmeasured()))
    }

    @Test
    fun `ohne Messung wird weder gedrosselt noch beschleunigt`() {
        assertEquals(3, ResourceAdvisor.limitFor(3, unmeasured()))
    }

    @Test
    fun `ohne Messung bleibt das Limit unveraendert`() {
        assertEquals(3, limiter(limit = 3, sample = unmeasured()).effectiveLimit)
    }

    @Test
    fun `ohne Messung wird der freie Speicher nicht geraten`() {
        assertEquals(null, unmeasured().freeMemoryFraction)
    }

    @Test
    fun `knappe Ressourcen senken das Limit nie`() {
        assertTrue(limiter(limit = 4, sample = tight()).effectiveLimit < 4)
    }

    @Test
    fun `das wirksame Limit kann nie ueber dem eingestellten liegen`() {
        assertEquals(2, limiter(limit = 2, sample = ample()).effectiveLimit)
    }

    @Test
    fun `kritische Ressourcen erlauben nur eine Einheit`() {
        assertEquals(1, ResourceAdvisor.limitFor(4, scarce()))
    }

    @Test
    fun `wenn das Limit auf eins sinkt ist die zweite Einheit abgewiesen`() {
        val l = limiter(limit = 3, sample = scarce())
        admitted(l.tryAcquire("a1"))
        assertEquals(DenialReason.RESOURCES_EXHAUSTED, denied(l.tryAcquire("a2")).reason)
    }

    @Test
    fun `der Ressourcenhinweis nennt den Nutzer`() {
        val l = limiter(limit = 3, sample = tight())
        val text = l.statusLines().joinToString("\n")
        assertTrue(text.contains("langsamer"))
        assertTrue(text.contains("3"))
    }

    @Test
    fun `bei kritischer Lage nennt die Anzeige das Anhalten`() {
        val l = limiter(limit = 3, sample = scarce())
        assertTrue(l.statusLines().joinToString("\n").contains("anzuhalten"))
    }

    @Test
    fun `ein Akkustand unter hundert Prozent ist moeglich`() {
        val fehler = runCatching {
            ResourceSample(0L, 100L, 101, false)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    @Test
    fun `ein Gesamt von null Byte liefert keinen Speicheranteil`() {
        assertEquals(null, ResourceSample(0L, 0L, 50, false).freeMemoryFraction)
    }

    // ── Pauseoption ──────────────────────────────────────────────────────────

    @Test
    fun `eine Pause weist jede neue Einheit ab`() {
        val l = limiter()
        l.pause("Der Nutzer hat es angehalten")

        assertEquals(DenialReason.PAUSED, denied(l.tryAcquire("a1")).reason)
    }

    @Test
    fun `die Pause nennt ihren Grund im Klartext`() {
        val l = limiter()
        l.pause("Akku fast leer")
        assertTrue(l.statusLines().joinToString("\n").contains("Akku fast leer"))
    }

    @Test
    fun `nach dem Fortsetzen ist wieder Platz`() {
        val l = limiter()
        l.pause("kurz")
        l.resume()
        admitted(l.tryAcquire("a1"))
    }

    @Test
    fun `eine Pause laesst laufende Arbeit unberuehrt`() {
        val l = limiter()
        admitted(l.tryAcquire("a1"))
        l.pause("Akku fast leer")
        assertEquals(1, l.activeCount)
    }

    @Test
    fun `ohne Pausegrund steht dort kein leerer Text`() {
        val l = limiter()
        l.pause("")
        assertTrue(l.statusLines().joinToString("\n").contains("ohne Angabe"))
    }

    // ── Schutz: keine automatische Modellladung im Hintergrund ──────────────

    @Test
    fun `eine Hintergrundladung ist ohne Zustimmung blockiert`() {
        val l = limiter(sample = ample())
        assertEquals(BackgroundModelDecision.BLOCKED, l.requestBackgroundModelLoad(2_000_000_000L))
    }

    @Test
    fun `auch bei sehr viel Speicher bleibt die Ladung blockiert`() {
        val l = limiter(sample = ample())
        assertFalse(l.requestBackgroundModelLoad(1_000L).isAllowed)
    }

    @Test
    fun `nach ausdruecklicher Zustimmung ist die Ladung moeglich`() {
        val l = limiter(sample = ample())
        l.allowBackgroundModelLoad(true)
        assertEquals(BackgroundModelDecision.ALLOWED, l.requestBackgroundModelLoad(2_000_000_000L))
    }

    @Test
    fun `selbst mit Zustimmung laedt nichts ueber den freien Speicher hinaus`() {
        val l = limiter(sample = ample())
        l.allowBackgroundModelLoad(true)
        assertEquals(BackgroundModelDecision.BLOCKED, l.requestBackgroundModelLoad(7_500_000_000L))
    }

    @Test
    fun `ohne Messung bleibt die Ladung auch mit Zustimmung blockiert`() {
        val l = limiter(sample = unmeasured())
        l.allowBackgroundModelLoad(true)
        assertEquals(BackgroundModelDecision.BLOCKED, l.requestBackgroundModelLoad(1_000L))
    }

    @Test
    fun `widerrufen der Zustimmung blockiert wieder sofort`() {
        val l = limiter(sample = ample())
        l.allowBackgroundModelLoad(true)
        l.allowBackgroundModelLoad(false)
        assertEquals(BackgroundModelDecision.BLOCKED, l.requestBackgroundModelLoad(1_000L))
    }

    @Test
    fun `ein Bedarf von null Byte wird abgelehnt`() {
        val fehler = runCatching {
            limiter().requestBackgroundModelLoad(0L)
        }.exceptionOrNull()
        assertTrue(fehler is IllegalArgumentException)
    }

    // ── Zugriffsarten ────────────────────────────────────────────────────────

    @Test
    fun `zwei Leser stoeren sich nie`() {
        assertTrue(FileAccess.READ.coexistsWith(FileAccess.READ))
    }

    @Test
    fun `ein Schreiber stoert sich mit jedem anderen Zugriff`() {
        assertFalse(FileAccess.WRITE.coexistsWith(FileAccess.READ))
        assertFalse(FileAccess.WRITE.coexistsWith(FileAccess.WRITE))
        assertFalse(FileAccess.READ.coexistsWith(FileAccess.WRITE))
    }

    @Test
    fun `die Zugriffsart erscheint verständlich im Text`() {
        assertEquals("schreibt", FileAccess.WRITE.germanLabel)
        assertEquals("liest", FileAccess.READ.germanLabel)
    }
}
