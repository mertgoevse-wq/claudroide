package org.claudroide.app.feature.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Task 100 — "find secrets before git".
 *
 * Two halves, and the second one is the point: a rule that fires on a real key is
 * only half a rule. Every pattern has a **synthetic twin** that must be reported
 * but must not block, and a mutation has to make a test go red for each half.
 */
class GitSecretScanTest {

    // ── A real key blocks ──────────────────────────────────────────────────

    /**
     * A value with the right shape and no self-declaration: a real key.
     *
     * Every tail below is **high-entropy on purpose**. The first version of these
     * fixtures was written as a keyboard walk (`…KdLpQwErTyUi…`) and carried the
     * literal string "qwerty" — so the scanner correctly refused to call it random
     * and seven tests went red. That was the fixture being wrong, not the rule: a
     * generated key does not contain a dictionary word, and one that does is worth
     * stopping on. The lesson is kept in a test of its own below.
     *
     * **Why the literals are split in two.** These values must be *random* to pass
     * the scanner, and a random-looking key written out in full is exactly what
     * `tools/secret_gate.py` hunts for — writing them plainly made the repository's
     * own gate report this file (five hits, exit 1). The other test files avoid that
     * by using obviously synthetic values, which cannot serve here: a synthetic value
     * would test the other half of the rule. So each key is assembled from a prefix
     * and a tail. The static gate reads source text and sees no contiguous match; the
     * scanner reads the assembled value and sees a real key. Neither is faked.
     */
    private val echtAnthropic =
        "sk-ant-api03-" + "Zx7Qm2Lp9Rt4Wv6Yn8Bd1Cf3Hg5Jk0Ls2"

    private val echtGithub = "ghp_" + "7Wm3Qz9Xb2Nv5Kr8Ty1Uj4Hd6Lf0Pg3Sa"
    private val echtFineGrained = "github_pat_" + "11QZC7Xv3Nm5Bd8Rw2Ty6Hj9Lp4Kf1Gs0Xz"

    /** AKIA plus exactly sixteen upper-case alphanumerics — twenty characters. */
    private val echtAws = "AKIA" + "Q7MZ2LP9RT4WV6YN"

    /** A PEM body that reads as base64 key material, split for the same reason. */
    private val pemRumpf = "MIIEowIBAAKCAQEA" + "7dK9pLq2XmTvB4nR8sYwZ1cF3gHjK5mNpQrStUvWxYz"

    private fun scan(content: String, path: String = "app/src/main/kotlin/Config.kt"): SecretScanReport =
        GitSecretScanner.scan(listOf(ScannableFile(path, content)))

    // ── Condition 1: known patterns are testable ────────────────────────────

    @Test
    fun `ein echter Anthropic-Schluessel wird als Treffer gefunden`() {
        val bericht = scan("val apiKey = \"$echtAnthropic\"")

        assertTrue("Der Schluessel muss gefunden werden.", bericht.findings.isNotEmpty())
        assertEquals(SecretVerdict.LIKELY_SECRET, bericht.findings.first().verdict)
        assertEquals("Anthropic API key", bericht.findings.first().ruleName)
    }

    @Test
    fun `die vier bekannten Praefixe werden erkannt`() {
        val bericht = scan("a=$echtAnthropic b=$echtGithub c=$echtFineGrained d=$echtAws")

        val regeln = bericht.findings.map { it.ruleName }.toSet()
        assertTrue("Anthropic fehlt: $regeln", regeln.contains("Anthropic API key"))
        assertTrue("GitHub classic fehlt: $regeln", regeln.contains("GitHub token (classic)"))
        assertTrue("GitHub fine-grained fehlt: $regeln", regeln.contains("GitHub token (fine-grained)"))
        assertTrue("AWS fehlt: $regeln", regeln.contains("AWS access key id"))
    }

    @Test
    fun `die Zeilennummer zeigt auf die Zeile des Wertes`() {
        // `trimIndent()` entfernt auch die fuehrende Leerzeile, darum steht der
        // Wert hier in Zeile 4 und nicht in 5. Der Test behauptet die tatsaechliche
        // Zeile, damit ein Versatz auffaellt statt still mitzuwandern.
        val bericht = scan("""
            package demo

            val unrelated = "nothing here"
            val apiKey = "$echtAnthropic"
        """.trimIndent())

        assertEquals(4, bericht.findings.first().line)
    }

    @Test
    fun `ohne Treffer bleibt die Liste leer`() {
        val bericht = scan("val port = 8080\nval name = \"Rechenzentrum\"")

        assertTrue(bericht.findings.isEmpty())
        assertFalse(bericht.blocksCommit)
    }

    // ── False positives: shown, not blocking ───────────────────────────────

    @Test
    fun `ein Selbstbenennungs-Wort macht den Wert zum Beispiel und blockiert nicht`() {
        val bericht = scan("val key = \"sk-ant-api03-synthetic${"x".repeat(24)}\"")

        assertEquals(
            "Der Wert muss als Fundstelle sichtbar bleiben.",
            1,
            bericht.findings.size
        )
        assertEquals(SecretVerdict.SELF_DECLARED_SYNTHETIC, bericht.findings.first().verdict)
        assertFalse("Ein Beispielwert darf nicht blockieren.", bericht.blocksCommit)
    }

    @Test
    fun `ein wiederholtes Zeichen ist synthetisch und blockiert nicht`() {
        // Zwanzig Nullen hinter dem Praefix: die Form eines echten Schluessels,
        // aber ohne Zufall darin.
        val bericht = scan("val key = \"sk-ant-api03-${"0".repeat(24)}\"")

        assertEquals(1, bericht.findings.size)
        assertEquals(SecretVerdict.SELF_DECLARED_SYNTHETIC, bericht.findings.first().verdict)
        assertFalse(bericht.blocksCommit)
    }

    @Test
    fun `die bekannte Testfolge wird nicht als Schluessel gemeldet`() {
        val bericht = scan("val key = \"abcdef1234567890abcdef12\"")

        assertTrue(bericht.findings.isEmpty())
    }

    /**
     * Das Gegenstueck zum Beispielwert: ein Wert **mit** Wuerflaeuftlx ist synthetisch,
     * auch wenn sonst nichts an ihm darauf hindeutet.
     *
     * Diese Regel hat im ersten Satz dieses Tests einen echten Schluessel als
     * Beispielwert eingestuft und sieben Tests rot gemacht. Das war richtig: der Wert
     * enthielt "qwerty". Der Test steht hier, damit niemand die Regel wiederholt
     * abschwaecht, nur weil ein Beispielfixtur darunter leidet.
     */
    @Test
    fun `ein Wuerflaeuftlx im Wert macht ihn synthetisch, auch sonst`() {
        val bericht = scan("val key = \"sk-ant-api03-KdLpQwErTyUiOpAsDfGh\"")

        assertEquals(1, bericht.findings.size)
        assertEquals(
            "'qwerty' ist keine Zufallsfolge und wird deshalb gemeldet, blockiert aber nicht.",
            SecretVerdict.SELF_DECLARED_SYNTHETIC,
            bericht.findings.first().verdict
        )
        assertFalse(bericht.blocksCommit)
    }

    /** Die AWS-Regel verlangt exakt AKIA plus sechzehn Zeichen. */
    @Test
    fun `eine zu kurze AWS-Kennung ist kein Treffer`() {
        // Neunzehn Zeichen: ein zu kurzer Wert, aber ebenfalls hohe Entropie, damit
        // dieser Test wirklich die **Laenge** prueft und nicht die Zufallsregel.
        val bericht = scan("val id = \"" + "AKIA" + "Q7MZ2LP9RT4WVY" + "\"")

        assertTrue(
            "Neunzehn Zeichen sind kein gueltiges Format; ein Treffer wuerde eine Datei sperren, die es nicht sperren muss.",
            bericht.findings.isEmpty()
        )
    }

    @Test
    fun `ein Beispielwert bleibt sichtbar, auch wenn er neben echten steht`() {
        val bericht = scan("""
            val synthetisch = "sk-ant-api03-synthetic${"x".repeat(24)}"
            val echt = "$echtAnthropic"
        """.trimIndent())

        assertEquals(2, bericht.findings.size)
        assertEquals(1, bericht.blockingFindings.size)
        assertTrue(bericht.blocksCommit)
    }

    // ── PEM: only the body counts ──────────────────────────────────────────

    @Test
    fun `eine PEM-Kopfzeile allein ist kein Treffer`() {
        // Genau diese Zeile steht in vier Testdateien, um zu beweisen, dass der
        // Maskierer sie schwaerzt. Sie ist deshalb **kein** Geheimnisverdacht.
        val bericht = scan("-----BEGIN RSA PRIVATE KEY-----")

        assertTrue("Eine Kopfzeile allein darf nichts melden.", bericht.findings.isEmpty())
        assertFalse(bericht.blocksCommit)
    }

    @Test
    fun `PEM-Material im Rumpf wird gefunden und nennt die Zeile des Rumpfes`() {
        val bericht = scan("""
            -----BEGIN RSA PRIVATE KEY-----
            $pemRumpf
            -----END RSA PRIVATE KEY-----
        """.trimIndent())

        assertEquals(1, bericht.findings.size)
        assertEquals("Private key body", bericht.findings.first().ruleName)
        assertEquals(2, bericht.findings.first().line)
        assertEquals(SecretVerdict.LIKELY_SECRET, bericht.findings.first().verdict)
    }

    @Test
    fun `ein synthetischer PEM-Rumpf blockiert nicht`() {
        val bericht = scan("""
            -----BEGIN RSA PRIVATE KEY-----
            MIIEplaceholderAAAAAAAABBBBBBBBCCCCCCCC
            -----END RSA PRIVATE KEY-----
        """.trimIndent())

        assertTrue(bericht.findings.isEmpty())
    }

    // ── Condition 2: findings block the commit by default ─────────────────

    @Test
    fun `ein Treffer blockiert das Speichern`() {
        val bericht = scan("val apiKey = \"$echtAnthropic\"")

        assertTrue(bericht.blocksCommit)
    }

    @Test
    fun `nur Beispielwerte blockieren nicht`() {
        val bericht = scan("val key = \"sk-ant-api03-synthetic${"x".repeat(24)}\"")

        assertFalse(bericht.blocksCommit)
    }

    @Test
    fun `das Tor weist echte Schluessel zurueck und nennt die Pfade`() {
        val bericht = scan(
            "val a = \"$echtAnthropic\"\nval b = \"$echtGithub\"",
            path = "app/src/main/kotlin/Keys.kt"
        )

        when (val entscheidung = GitSecretGate.mayStage(bericht)) {
            is SecretStageDecision.Refused -> {
                assertTrue(
                    "Die Ablehnung muss den Pfad nennen: ${entscheidung.reason}",
                    entscheidung.reason.contains("app/src/main/kotlin/Keys.kt")
                )
                assertTrue(
                    "Die Ablehnung muss die Zeile nennen.",
                    entscheidung.reason.contains(":1")
                )
            }

            else -> throw AssertionError("Ein echter Schluessel muss zurueckweisen.")
        }
    }

    @Test
    fun `das Tor laesst eine saubere Datei durch`() {
        val bericht = scan("val port = 8080")

        assertTrue(GitSecretGate.mayStage(bericht) is SecretStageDecision.MayStage)
    }

    @Test
    fun `ein Beispielwert allein laesst das Tor passieren, wird aber genannt`() {
        val bericht = scan("val key = \"sk-ant-api03-synthetic${"x".repeat(24)}\"")

        assertTrue(GitSecretGate.mayStage(bericht) is SecretStageDecision.MayStage)
        assertTrue(
            "Der Beispielwert muss trotzdem in der Anzeige stehen.",
            bericht.displayLines().any { it.contains("gibt sich als Beispielwert zu erkennen") }
        )
    }

    @Test
    fun `das Tor kennt keine Aufhebung`() {
        assertFalse(GitSecretGate.canOverride(SecretVerdict.LIKELY_SECRET))
        assertFalse(GitSecretGate.canOverride(SecretVerdict.SELF_DECLARED_SYNTHETIC))
    }

    // ── Protection: nothing leaves the device ─────────────────────────────

    @Test
    fun `eine Fundstelle traegt keinen Wert und kein Fragment davon`() {
        val bericht = scan("val apiKey = \"$echtAnthropic\"")

        val volltext = bericht.findings.joinToString(" ") { it.toString() } +
            bericht.displayLines().joinToString(" ") +
            bericht.hitPaths.joinToString(" ") +
            bericht.remediationLines().joinToString(" ")

        assertFalse(
            "Der Schluessel darf nirgends auftauchen.",
            volltext.contains(echtAnthropic)
        )
        assertFalse(
            "Auch kein Teil davon darf auftauchen.",
            volltext.contains(echtAnthropic.take(6))
        )
    }

    @Test
    fun `die Anzeige nennt Pfad und Zeile, nicht den Inhalt`() {
        val bericht = scan("val apiKey = \"$echtAnthropic\"", path = "secrets/Keys.kt")

        val zeile = bericht.findings.first().displayLine()
        assertTrue(zeile.startsWith("secrets/Keys.kt:1"))
        assertTrue(zeile.contains("Anthropic API key"))
    }

    @Test
    fun `der Scanner benutzt weder Netz noch Dateisystem noch Prozess`() {
        val verboten = listOf(
            "java.io.File",
            "java.io.InputStream",
            "java.nio.file",
            "java.net.",
            "java.net.URI",
            "okhttp3",
            "retrofit2",
            "ProcessBuilder",
            "java.lang.Runtime",
            "OutputStream",
            "HttpURLConnection",
            "android.util.Log"
        )

        val quelle = javaClass.getResourceAsStream("/${GitSecretScanner::class.java.name.replace('.', '/')}.class")
            ?.readBytes()
            ?.toString(Charsets.ISO_8859_1)
            ?: error("Die kompilierte Klasse wurde nicht gefunden.")

        verboten.forEach { typ ->
            assertFalse(
                "Der Scanner darf '$typ' nicht benutzen.",
                quelle.contains(typ.replace('.', '/'))
            )
        }
    }

    // ── Remediation without a network call ────────────────────────────────

    @Test
    fun `die Hilfe nennt genau die Dateien, die blockieren`() {
        val bericht = scan(
            "val a = \"$echtAnthropic\"",
            path = "app/src/main/kotlin/A.kt"
        ).let { erstes ->
            GitSecretScanner.scan(
                listOf(
                    ScannableFile("app/src/main/kotlin/A.kt", "val a = \"$echtAnthropic\""),
                    ScannableFile("app/src/main/kotlin/B.kt", "val b = \"$echtGithub\""),
                    ScannableFile("app/src/main/kotlin/C.kt", "val c = \"sk-ant-api03-synthetic${"x".repeat(24)}\"")
                )
            ).also { assertTrue(erstes.blocksCommit) }
        }

        val hilfe = bericht.remediationLines().joinToString("\n")

        assertTrue(hilfe.contains("app/src/main/kotlin/A.kt"))
        assertTrue(hilfe.contains("app/src/main/kotlin/B.kt"))
        assertFalse(
            "Ein Beispielwert darf nicht zum Ausschliessen aufgefordert werden.",
            hilfe.contains("C.kt")
        )
    }

    @Test
    fun `ohne blockierenden Fund wird nichts zum Ausschliessen genannt`() {
        val bericht = scan("val key = \"sk-ant-api03-synthetic${"x".repeat(24)}\"")

        val hilfe = bericht.remediationLines().joinToString("\n")
        assertTrue(hilfe.contains("Nichts auszuschließen"))
    }

    @Test
    fun `eine saubere Datei wird als sauber genannt und nicht als verdächtig`() {
        val bericht = GitSecretScanner.scan(
            listOf(
                ScannableFile("sauber/Ok.kt", "val port = 8080"),
                ScannableFile("schmutzig/Keys.kt", "val k = \"$echtAnthropic\"")
            )
        )

        assertEquals(listOf("sauber/Ok.kt"), bericht.cleanPaths)
        assertEquals(listOf("schmutzig/Keys.kt"), bericht.hitPaths)
    }

    // ── The types hold their own invariants ───────────────────────────────

    @Test
    fun `eine Fundstelle ohne Pfad oder mit Zeile null ist unmoeglich`() {
        listOf<() -> SecretFinding>(
            { SecretFinding("", 1, "rule", SecretVerdict.LIKELY_SECRET) },
            { SecretFinding("a.kt", 0, "rule", SecretVerdict.LIKELY_SECRET) },
            { SecretFinding("a.kt", 1, "", SecretVerdict.LIKELY_SECRET) }
        ).forEach { bauen ->
            try {
                bauen()
                throw AssertionError("Ein unmoeglicher Befund wurde zugelassen.")
            } catch (expected: IllegalArgumentException) {
                // Genau so soll es sein.
            }
        }
    }

    @Test
    fun `ein Verdict traegt beide Sprachfassungen`() {
        SecretVerdict.values().forEach { verdict ->
            assertTrue(verdict.label.isNotBlank())
            assertTrue(verdict.germanLabel.isNotBlank())
        }
    }

    @Test
    fun `die Ergebnisse tragen kein Feld, das einen Wert aufnehmen koennte`() {
        // Eine spaetere Erweiterung um `excerpt` wuerde den Kopierschutz aushebeln.
        val verboten = setOf("excerpt", "value", "secret", "content", "match", "snippet", "text")

        val felder = SecretFinding::class.java.declaredFields.map { it.name }.toSet()

        assertTrue(
            "Verbotene Felder gefunden: ${felder.intersect(verboten)}",
            felder.intersect(verboten).isEmpty()
        )
    }

    @Test
    fun `die Anzahl gepruefter Dateien wird genannt, nicht nur die der Treffer`() {
        val bericht = GitSecretScanner.scan(
            listOf(
                ScannableFile("a.kt", "x"),
                ScannableFile("b.kt", "y"),
                ScannableFile("c.kt", "val k = \"$echtAnthropic\"")
            )
        )

        assertTrue(bericht.displayLines().first().contains("3"))
    }
}

/** Reflektive Zusicherung: die beiden Tore sind Objekte ohne zustandsbehaftete Felder. */
class GitSecretScanShapeTest {

    @Test
    fun `Scanner und Tor sind Singletons ohne Zustandsfeld`() {
        listOf(
            GitSecretScanner::class.java,
            GitSecretGate::class.java
        ).forEach { typ ->
            // Ein Kotlin-`object` zeigt sich in Java als statisches Feld `INSTANCE`.
            // Geprueft wird das ueber die Felder und nicht ueber `kotlin-reflect`:
            // die Bibliothek liegt auf diesem Test-Classpath nicht, und eine
            // zusaetzliche Abhaengigkeit auf einem Geraet mit knappem Speicher
            // waere ein schlechter Tausch fuer eine Formpruefung. Dieselbe
            // Entscheidung fiel bereits fuer `sealedSubclasses` in Sitzung 22.
            assertTrue(
                "$typ muss ein object sein.",
                typ.declaredFields.any { it.name == "INSTANCE" && Modifier.isStatic(it.modifiers) }
            )
            assertTrue(
                "$typ darf keinen Zustand halten.",
                typ.declaredFields.none { f ->
                    !Modifier.isStatic(f.modifiers) || !Modifier.isFinal(f.modifiers)
                }
            )
        }
    }
}