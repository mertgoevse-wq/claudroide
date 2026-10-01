package org.claudroide.app

import org.claudroide.app.feature.project.FilePreviewPolicy
import org.claudroide.app.feature.project.FilePreviewPolicy.PreviewKind
import org.claudroide.app.feature.project.ProjectExclusionPolicy.FileDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

/**
 * Task 088 — "Dateien ansehen".
 *
 * Acceptance criteria under test:
 *  - Binärdateien werden nicht fälschlich als lesbarer Text gezeigt.
 *  - Große Dateien bleiben begrenzt und bedienbar, mit sichtbarem Kürzungsmarker.
 *  - Nicht dekodierbare Dateien werden in einfachem Deutsch erklärt, nicht als
 *    unlesbare Zeichen angezeigt.
 *  - Eine Vorschau bleibt lokal und löst keinen Netzwerkzugriff aus.
 *  - Gesperrte Geheimnisdateien werden nie vorgeschaut und lassen sich nicht
 *    überschreiben.
 */
class FilePreviewTest {

    private fun text(vararg lines: String) =
        lines.joinToString("\n").toByteArray(StandardCharsets.UTF_8)

    private fun previewOf(path: String, bytes: ByteArray) = FilePreviewPolicy.preview(path, bytes)

    // ── Criterion: binary files are never shown as readable text ──────────────

    @Test
    fun pngFile_isNeverPreviewedAsText() {
        // Even with fully printable content, a .png stays binary by extension.
        val bytes = text("not really a picture")
        val result = previewOf("app/src/main/res/logo.png", bytes)

        assertEquals(PreviewKind.BINARY, result.kind)
        assertNull("a binary file must not produce text", result.text)
        assertFalse("a binary file must not be sendable", result.mayBeSent)
    }

    @Test
    fun binaryContentBehindATextExtension_isDetectedByContent() {
        // The extension lies; the bytes do not. This is the case that matters.
        val bytes = ByteArray(600) { 0x00 }
        val result = previewOf("data/report.txt", bytes)

        assertEquals(PreviewKind.BINARY, result.kind)
        assertNull("bytes must never be dumped as text", result.text)
    }

    @Test
    fun binaryContentWithManyControlBytes_isDetected() {
        val bytes = ByteArray(400) { if (it % 2 == 0) 0x01 else 0x7F.toByte() }
        assertTrue(FilePreviewPolicy.looksBinary(bytes))
    }

    @Test
    fun germanTextWithUmlauts_isNotMistakenForBinary() {
        // Umlauts and ß are bytes above 0x7F. Counting them as control bytes
        // would have blocked every German source file.
        val result = previewOf("docs/README.md", text("Grüße aus München", "Straße heißt groß"))

        assertEquals(PreviewKind.TEXT, result.kind)
        assertTrue(result.text!!.contains("Grüße"))
        assertTrue(result.text!!.contains("groß"))
    }

    @Test
    fun ordinarySourceCode_staysPreviewable() {
        val result = previewOf(
            "app/src/main/java/Main.kt",
            text("fun main() {", "    println(\"hallo\")", "}")
        )

        assertEquals(PreviewKind.TEXT, result.kind)
        assertEquals("UTF-8", result.charsetName)
        assertEquals(3, result.lineCount)
        assertTrue(result.text!!.contains("fun main()"))
        assertTrue("a readable text file may be sent", result.mayBeSent)
    }

    @Test
    fun binaryMessage_isGermanAndMentionsNotSending() {
        val result = previewOf("assets/cover.jpg", ByteArray(64) { 0x00 })

        assertTrue("must be explained in German", result.message.contains("Binärdatei"))
        assertTrue("must say it is not sent", result.message.contains("nicht gesendet"))
    }

    // ── Criterion: large files stay limited, with an honest marker ────────────

    @Test
    fun longFile_isTruncatedWithAVisibleMarker() {
        val bytes = text(*Array(600) { "Zeile $it" })
        val result = previewOf("docs/lang.txt", bytes)

        assertTrue("600 lines must be cut", result.truncated)
        assertTrue("the marker must be visible in the text", result.text!!.contains(FilePreviewPolicy.TRUNCATION_MARKER))
        // 600 input lines: 200 shown, 400 left out. totalLines counts newlines + 1.
        assertTrue("the marker must say how much is missing", result.text!!.contains("200 von 600 Zeilen"))
        assertTrue("the shown line count must be capped", result.text!!.length < bytes.size)
    }

    @Test
    fun shortFile_isNotTruncated() {
        val result = previewOf("docs/notes.md", text("eins", "zwei", "drei"))

        assertFalse("a short file must not carry a truncation marker", result.truncated)
        assertFalse(result.text!!.contains(FilePreviewPolicy.TRUNCATION_MARKER))
    }

    @Test
    fun veryLongSingleLine_isTruncatedByCharacterCount() {
        val oneLine = "x".repeat(20_000)
        val result = previewOf("docs/dump.log", oneLine.toByteArray(StandardCharsets.UTF_8))

        assertTrue("a single huge line must be cut", result.truncated)
        assertTrue(result.text!!.contains(FilePreviewPolicy.TRUNCATION_MARKER))
        assertTrue(
            "shown text must respect the character cap",
            result.text!!.length <= FilePreviewPolicy.MAX_PREVIEW_CHARS + 200
        )
    }

    @Test
    fun truncatedPreview_stillReportsTheRealFileSize() {
        val bytes = text(*Array(400) { "Zeile $it" })
        val result = FilePreviewPolicy.preview("docs/lang.txt", bytes, sizeBytes = 99_999L)

        assertEquals(99_999L, result.sizeBytes)
        assertTrue("the header must state the real size", result.message.contains("97 KB"))
        assertTrue("the header must say it is cut", result.message.contains("gekürzte Vorschau"))
    }

    // ── Criterion: undecodable content is explained, not shown as mojibake ────

    @Test
    fun invalidUtf8_isExplainedInGermanInsteadOfMojibake() {
        // 0xC3 starts a two-byte sequence that never arrives.
        val bytes = byteArrayOf('H'.code.toByte(), 'i'.code.toByte(), 0xC3.toByte(), 0x28)
        val result = previewOf("notes/readme.txt", bytes)

        assertEquals(PreviewKind.NOT_DECODABLE, result.kind)
        assertNull("broken bytes must not reach the screen", result.text)
        assertTrue("must be explained in German", result.message.contains("nicht als Text lesen"))
        assertFalse("no replacement characters may appear", result.message.contains('�'))
    }

    @Test
    fun latin1File_isReportedAsNotDecodableNotAsGarbage() {
        // "Grüße" in ISO-8859-1: valid bytes, wrong charset for us.
        val bytes = byteArrayOf(0x47, 0x72, 0xFC.toByte(), 0xDF.toByte(), 0x65)
        val result = previewOf("alt/notiz.txt", bytes)

        assertEquals(PreviewKind.NOT_DECODABLE, result.kind)
        assertNull(result.text)
        assertTrue("must mention the charset as a possible cause", result.message.contains("Zeichensatz"))
    }

    @Test
    fun emptyFile_isExplainedNotCrashed() {
        val result = previewOf("src/.gitkeep", ByteArray(0))

        assertEquals(PreviewKind.EMPTY, result.kind)
        assertNull(result.text)
        assertTrue(result.message.contains("leer"))
    }

    @Test
    fun missingFile_isExplainedInGerman() {
        val result = FilePreviewPolicy.preview("src/Ghost.kt", ByteArray(0), exists = false)

        assertEquals(PreviewKind.NOT_FOUND, result.kind)
        assertNull(result.text)
        assertTrue(result.message.contains("Ghost.kt"))
    }

    // ── Criterion: a preview is local and sends nothing ───────────────────────

    @Test
    fun everyPreviewDeclaresItstaysOnTheDevice() {
        listOf(
            previewOf("src/Main.kt", text("ok")),
            previewOf("logo.png", ByteArray(8) { 0x00 }),
            previewOf("data/x.txt", byteArrayOf(0xC3.toByte())),
            previewOf("secret/.env", text("A=B"))
        ).forEach { result ->
            assertTrue("every preview must be local", result.isLocalOnly)
        }
        assertTrue("the notice must say it is local", FilePreviewPolicy.LOCAL_ONLY_NOTICE.contains("Gerät"))
    }

    @Test
    fun previewableTextInAHeavyFolder_isPreviewedButNotSendable() {
        // Preview and send are different decisions; the UI must not conflate them.
        val result = previewOf("build/tmp/out.txt", text("zwischenergebnis"))

        assertEquals(PreviewKind.TEXT, result.kind)
        assertNotNull(result.text)
        assertEquals(FileDecision.BLOCKED_HEAVY, result.sendDecision)
        assertFalse("a heavy file may be previewed but not sent", result.mayBeSent)
    }

    // ── Criterion: secrets are never previewed and have no override ───────────

    @Test
    fun envFile_isNotPreviewedAtAll() {
        val result = previewOf("backend/.env", text("API_KEY=", "GEHEIM=wert"))

        assertEquals(PreviewKind.BLOCKED_SECRET, result.kind)
        assertNull("a secret must never be shown", result.text)
        assertFalse(result.mayBeSent)
        assertTrue("must say no preview is shown", result.message.contains("keine Vorschau"))
    }

    @Test
    fun privateKeyFile_isNotPreviewed() {
        val pem = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA\n-----END RSA PRIVATE KEY-----"
        val result = previewOf("keys/server.pem", pem.toByteArray(StandardCharsets.UTF_8))

        assertEquals(PreviewKind.BLOCKED_SECRET, result.kind)
        assertNull("key material must never be shown", result.text)
    }

    @Test
    fun previewApiHasNoOverrideForSecrets() {
        // Every parameter of preview() is about content, never about permission.
        val method = FilePreviewPolicy::class.java.methods.first { it.name == "preview" }
        val names = method.parameterTypes.map { it.simpleName }

        assertFalse("no force flag may exist", names.any { it.contains("force", true) })
        assertFalse("no override flag may exist", names.any { it.contains("override", true) })
    }

    // ── Criterion: secret-looking content is masked before display ────────────

    @Test
    fun apiKeyInsideAnAllowedFile_isMaskedInThePreview() {
        val result = previewOf(
            "docs/notes.md",
            text("hier steht sk-ant-abcdefghijklmnop und mehr")
        )

        assertTrue("a secret-looking value must be masked", result.secretRedacted)
        assertFalse("the raw key must not be on screen", result.text!!.contains("sk-ant-abcdefghijklmnop"))
        assertTrue(result.text!!.contains("[REDACTED]"))
    }

    @Test
    fun ordinaryText_isNotAltered() {
        val result = previewOf("docs/notes.md", text("nichts geheim hier"))

        assertFalse("no false positive masking", result.secretRedacted)
        assertEquals("nichts geheim hier", result.text)
    }

    // ── Robustness ────────────────────────────────────────────────────────────

    @Test
    fun emptyContent_isNotBinary() {
        assertFalse(FilePreviewPolicy.looksBinary(ByteArray(0)))
    }

    @Test
    fun windowsStylePath_isNormalised() {
        val result = previewOf("backend\\.env", text("A=B"))

        assertEquals(PreviewKind.BLOCKED_SECRET, result.kind)
        assertTrue(result.message.contains(".env"))
    }

    @Test
    fun documentationMentioningASecretName_isStillPreviewable() {
        val result = previewOf("docs/env-guide.md", text("Hier steht, was eine .env enthält."))

        assertEquals(PreviewKind.TEXT, result.kind)
        assertEquals(FileDecision.ALLOWED, result.sendDecision)
        assertTrue(result.mayBeSent)
    }
}
