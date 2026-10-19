package org.claudroide.app

import org.claudroide.app.feature.agent.AttemptComparison
import org.claudroide.app.feature.agent.AttemptCost
import org.claudroide.app.feature.agent.AttemptFootprint
import org.claudroide.app.feature.agent.AttemptPlan
import org.claudroide.app.feature.agent.AttemptScope
import org.claudroide.app.feature.agent.CostOpenReason
import org.claudroide.app.feature.agent.DataSource
import org.claudroide.app.feature.agent.MergeDecision
import org.claudroide.app.feature.agent.MergeGate
import org.claudroide.app.feature.agent.TokenEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Task 077 — "alternative attempts" (Alternative Arbeitsversuche).
 *
 * The completion conditions ask that the user sees which attempt uses which data
 * and model cost, and that merging happens only after comparison and explicit
 * approval. Both are tested as properties of the types, and the second is tested
 * in both directions: the merge that is refused, and the one that is allowed.
 */
class AttemptForkTest {

    // ── Condition 1: the user sees data and cost per attempt ───────────────

    @Test
    fun `der Vergleich nennt Daten und Kosten fuer jeden Versuch`() {
        val vergleich = AttemptComparison(
            attempts = listOf(
                versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12),
                versuch(name = "B", modell = "opus-5", daten = nutzerDateien(), kosten = 0.40)
            ),
            compared = true
        )

        val text = vergleich.comparisonLines().joinToString("\n")

        assertTrue("Jeder Versuch braucht ein Modell.", text.contains("gpt-5"))
        assertTrue(text.contains("opus-5"))
        assertTrue("Beide Versuche müssen ihre Daten nennen.", text.contains("main.kt"))
        assertTrue(text.contains("notizen.md"))
        assertTrue(text.contains("Kosten:"))
        assertTrue(text.contains("0.1200 USD"))
        assertTrue(text.contains("0.4000 USD"))
    }

    @Test
    fun `ein Versuch ohne Preisbeleg nennt keine Zahl`() {
        val vergleich = AttemptComparison(
            attempts = listOf(
                versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12),
                versuch(
                    name = "B",
                    modell = "opus-5",
                    daten = projektDateien(),
                    kosten = null
                )
            ),
            compared = true
        )

        val text = vergleich.comparisonLines().joinToString("\n")

        assertTrue(
            "Ein Versuch ohne Preisbeleg bleibt offen und nennt keinen Betrag.",
            text.contains("Kosten: offen")
        )
        assertTrue(text.contains(CostOpenReason.NO_PRICE_SOURCE.germanLabel))
        assertFalse(
            "Ein offener Versuch darf nicht als 0,00 USD erscheinen.",
            text.contains("0.0000 USD")
        )
    }

    @Test
    fun `keine Angabe ist nicht dasselbe wie keine Daten`() {
        val ohneAngabe = versuch(
            name = "A",
            modell = "gpt-5",
            daten = DataSource.SourceNotStated,
            kosten = 0.12
        )

        val text = ohneAngabe.footprint.displayLines().joinToString("\n")

        assertTrue(
            "Der Text muss den Unterschied selbst benennen.",
            text.contains("keine Angabe") && text.contains("nicht dasselbe")
        )
    }

    @Test
    fun `der Vergleich nennt keinen Gesamtwert und keinen Gewinner von sich aus`() {
        val vergleich = AttemptComparison(
            attempts = listOf(
                versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12),
                versuch(name = "B", modell = "gpt-5", daten = projektDateien(), kosten = 0.12,
                    freigegeben = true)
            ),
            compared = true
        )

        val text = vergleich.comparisonLines().joinToString("\n")

        assertFalse(
            "Eine erfundene Summe sähe aus wie eine Messung.",
            text.contains("Gesamt")
        )
        assertFalse(
            "Der Vergleich entscheidet nicht, welcher Versuch gewinnt.",
            text.contains("Gewinner") || text.contains("Empfehlung")
        )
    }

    @Test
    fun `der Vergleich weist darauf hin, dass er dem Nutzer noch nicht gezeigt wurde`() {
        val vergleich = AttemptComparison(
            attempts = listOf(
                versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12),
                versuch(name = "B", modell = "gpt-5", daten = projektDateien(), kosten = 0.12)
            ),
            compared = false
        )

        assertTrue(
            vergleich.comparisonLines().any { it.contains("noch nicht gezeigt") }
        )
    }

    // ── Condition 2: merge only after comparison and explicit approval ─────

    @Test
    fun `ohne Vergleich wird nicht zusammengefuehrt`() {
        val vergleich = AttemptComparison(
            attempts = listOf(
                versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12),
                versuch(name = "B", modell = "gpt-5", daten = projektDateien(), kosten = 0.12,
                    freigegeben = true)
            ),
            compared = false
        )

        val entscheidung = MergeGate.mayMerge(vergleich)

        assertTrue(
            "Ein freigegebener Versuch ersetzt den fehlenden Vergleich nicht.",
            entscheidung is MergeDecision.Refused
        )
        assertTrue(
            (entscheidung as MergeDecision.Refused).reason.contains("nicht verglichen")
        )
    }

    @Test
    fun `ein Vergleich ohne Freigabe fuehrt nicht zum Zusammenfuehren`() {
        val vergleich = AttemptComparison(
            attempts = listOf(
                versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12),
                versuch(name = "B", modell = "gpt-5", daten = projektDateien(), kosten = 0.12)
            ),
            compared = true
        )

        val entscheidung = MergeGate.mayMerge(vergleich)

        assertTrue(
            "Ein gezeigter Vergleich ist noch keine Freigabe.",
            entscheidung is MergeDecision.Refused
        )
        assertTrue((entscheidung as MergeDecision.Refused).reason.contains("Kein Versuch ist freigegeben"))
    }

    @Test
    fun `nach Vergleich und Freigabe wird zusammengefuehrt`() {
        val freigegeben = versuch(
            name = "B",
            modell = "opus-5",
            daten = nutzerDateien(),
            kosten = 0.40,
            freigegeben = true
        )
        val vergleich = AttemptComparison(
            attempts = listOf(
                versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12),
                freigegeben
            ),
            compared = true
        )

        val entscheidung = MergeGate.mayMerge(vergleich)

        assertTrue(
            "Vergleich und ausdrückliche Freigabe müssen durchlassen.",
            entscheidung is MergeDecision.MayMerge
        )
        assertEquals("B", (entscheidung as MergeDecision.MayMerge).attempt.name)
    }

    @Test
    fun `eine fehlende Freigabe wird nicht aus dem Vergleich abgeleitet`() {
        // Beide Versuche sind vollständig, gleich teuer und gleich gut beschrieben.
        // Nichts davon ist eine Freigabe.
        val vergleich = AttemptComparison(
            attempts = listOf(
                versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12),
                versuch(name = "B", modell = "gpt-5", daten = projektDateien(), kosten = 0.12)
            ),
            compared = true
        )

        assertFalse(vergleich.attempts.any { it.approvedByUser })
        assertTrue(MergeGate.mayMerge(vergleich) is MergeDecision.Refused)
    }

    @Test
    fun `eine Ablehnung laesst sich nicht ueberstimmen`() {
        assertFalse(
            "Eine Zusammenfuehrung ohne Vergleich und Freigabe ist keine.",
            MergeGate.canOverride()
        )
    }

    // ── The protection: no attempt overwrites the main project ─────────────

    @Test
    fun `kein Versuch darf das Hauptprojekt ueberschreiben`() {
        listOf(AttemptScope.entries).flatten().forEach { bereich ->
            assertFalse(
                "Der Zugriff '$bereich' darf kein Schreibzugriff auf das Hauptprojekt geben.",
                bereich.mayWriteMainProject
            )
        }
    }

    @Test
    fun `ein Versuch braucht ein eigenes Arbeitsverzeichnis`() {
        try {
            versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12)
                .copy(workspaceName = AttemptComparison.MAIN_PROJECT)
            throw AssertionError("Ein Versuch im Hauptprojekt wurde zugelassen.")
        } catch (expected: IllegalArgumentException) {
            // Genau so soll es sein.
        }
    }

    @Test
    fun `ein Versuch mit leerem Namen, leerer Idee oder Leerverzeichnis ist unmoeglich`() {
        listOf<() -> AttemptPlan>(
            { versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12)
                .copy(name = " ") },
            { versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12)
                .copy(idea = "") },
            { versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12)
                .copy(workspaceName = "") },
            { versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12)
                .copy(footprint = versuch(name = "A", modell = "", daten = projektDateien(),
                    kosten = 0.12).footprint) }
        ).forEach { bauen ->
            try {
                bauen()
                throw AssertionError("Ein unmoeglicher Versuch wurde zugelassen.")
            } catch (expected: IllegalArgumentException) {
                // Genau so soll es sein.
            }
        }
    }

    // ── The comparison is a value with a floor ─────────────────────────────

    @Test
    fun `ein Vergleich braucht mindestens zwei Versuche`() {
        try {
            AttemptComparison(
                attempts = listOf(
                    versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12)
                )
            )
            throw AssertionError("Ein Vergleich aus einem Versuch wurde zugelassen.")
        } catch (expected: IllegalArgumentException) {
            // Genau so soll es sein.
        }
    }

    @Test
    fun `zwei Versuche duerfen nicht denselben Namen tragen`() {
        try {
            AttemptComparison(
                attempts = listOf(
                    versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12),
                    versuch(name = "A", modell = "gpt-5", daten = projektDateien(), kosten = 0.12)
                )
            )
            throw AssertionError("Zwei gleichnamige Versuche wurden zugelassen.")
        } catch (expected: IllegalArgumentException) {
            // Genau so soll es sein.
        }
    }

    // ── The scaffold compares nothing and copies nothing ───────────────────

    @Test
    fun `das Geraest kopiert keine Datei und startet keinen Prozess`() {
        val verboten = listOf(
            "java/io/File",
            "java/nio/file",
            "java/net/",
            "okhttp3",
            "retrofit2",
            "ProcessBuilder",
            "java/lang/Runtime",
            "HttpURLConnection",
            "android/util/Log"
        )

        val quelle = javaClass.getResourceAsStream(
            "/" + MergeGate::class.java.name.replace('.', '/') + ".class"
        )?.readBytes()?.toString(Charsets.ISO_8859_1)
            ?: error("Die kompilierte Klasse wurde nicht gefunden.")

        verboten.forEach { typ ->
            assertFalse("Das Geraest darf '$typ' nicht benutzen.", quelle.contains(typ))
        }
    }

    @Test
    fun `das Tor haelt keinen Zustand zwischen zwei Aufrufen`() {
        val typ = MergeGate::class.java

        assertTrue(
            typ.declaredFields.any { it.name == "INSTANCE" && Modifier.isStatic(it.modifiers) }
        )
        assertTrue(
            typ.declaredFields.none { f ->
                !Modifier.isStatic(f.modifiers) || !Modifier.isFinal(f.modifiers)
            }
        )
    }

    // ── Fixtures ───────────────────────────────────────────────────────────

    private fun projektDateien() = DataSource.ProjectFiles(listOf("main.kt", "build.gradle.kts"))

    private fun nutzerDateien() = DataSource.UserPickedFiles(listOf("notizen.md"))

    /** [kosten] `null` means: no price evidence, so the cost stays open. */
    private fun versuch(
        name: String,
        modell: String,
        daten: DataSource,
        kosten: Double?,
        freigegeben: Boolean = false
    ) = AttemptPlan(
        name = name,
        idea = "Ansatz $name",
        workspaceName = "versuch-$name",
        scope = AttemptScope.MAIN_PROJECT_READ_ONLY,
        footprint = AttemptFootprint(
            dataSource = daten,
            model = modell,
            cost = kosten?.let {
                AttemptCost.Billed(
                    tokenEvidence = TokenEvidence(
                        tokens = 1_000,
                        usd = "%.4f USD".format(it),
                        sourceUrl = "https://example.invalid/preise",
                        verifiedDate = "2026-10-04"
                    )
                )
            } ?: AttemptCost.Open(CostOpenReason.NO_PRICE_SOURCE)
        ),
        approvedByUser = freigegeben
    )
}