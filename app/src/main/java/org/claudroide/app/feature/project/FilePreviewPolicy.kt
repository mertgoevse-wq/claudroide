package org.claudroide.app.feature.project

import org.claudroide.app.feature.provider.SecretMasker
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Builds a local, read-only preview of a project file.
 *
 * Task 088 — "Dateien ansehen".
 *
 * Acceptance criteria this file implements:
 *  - Binary files are never shown as readable text.
 *  - Large files stay limited and usable, with a visible marker for what was cut.
 *  - Content that cannot be decoded is explained in plain German, never shown as
 *    mojibake.
 *
 * Safety rules that hold regardless of the acceptance criteria:
 *  - A preview never executes anything. The only output is plain text plus metadata;
 *    no HTML, no scripts, no embedded content is interpreted.
 *  - A preview is local. It proves nothing about whether a file may be sent, and no
 *    code path in this file performs any network action.
 *  - The exclusion policy of task 067 is consulted first. A file blocked as a secret
 *    is not previewed at all, and there is no override parameter.
 *
 * The functions are pure and synchronous and work on plain [ByteArray] and [String],
 * so they run on the JVM test classpath without Android, coroutines or file handles.
 */
object FilePreviewPolicy {

    /** Shown under every preview so the user knows nothing left the device. */
    const val LOCAL_ONLY_NOTICE =
        "Diese Vorschau bleibt auf dem Gerät. Sie sagt nichts darüber aus, " +
            "ob die Datei an einen Anbieter gesendet werden darf."

    /** Most lines shown before the preview is cut. */
    const val MAX_PREVIEW_LINES = 200

    /** Most characters shown before the preview is cut. */
    const val MAX_PREVIEW_CHARS = 8_000

    /** How many bytes are inspected when guessing whether content is binary. */
    const val BINARY_SNIFF_BYTES = 4_096

    /**
     * Share of control characters above which content counts as binary.
     *
     * Real text is almost free of control bytes; a packed archive, an image or a
     * compiled class file is full of them.
     */
    const val BINARY_CONTROL_RATIO = 0.10

    /** Start of the honest marker that says the file was cut. */
    const val TRUNCATION_MARKER = "… [gekürzt:"

    /** Extensions that are never text, whatever the bytes look like. */
    private val binaryExtensions: Set<String> = setOf(
        // Bilder
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico", "tif", "tiff", "heic",
        // Archive und Verpacktes
        "zip", "gz", "tgz", "bz2", "xz", "7z", "rar", "jar", "aar", "apk", "war",
        // Ausführbare Dateien und Bibliotheken
        "exe", "dll", "so", "bin", "dat", "o", "a", "class", "dex", "wasm", "pyc",
        // Medien
        "mp3", "wav", "ogg", "flac", "m4a", "mp4", "mkv", "mov", "webm", "avi",
        // Datenbanken und Verzeichnisse
        "db", "sqlite", "sqlite3", "realm", "mdb", "pack", "idx",
        // Schriften
        "ttf", "otf", "woff", "woff2", "eot"
    )

    /** What kind of file the preview is dealing with. */
    enum class PreviewKind {
        /** Readable text was produced. */
        TEXT,

        /** Binary content; no text is produced at all. */
        BINARY,

        /** Not text, but not recognisable as a known binary format either. */
        NOT_DECODABLE,

        /** The file has no content. */
        EMPTY,

        /** Blocked as a secret or key material; never previewed. */
        BLOCKED_SECRET,

        /** The path does not exist. */
        NOT_FOUND
    }

    /**
     * The outcome of a preview request.
     *
     * [text] is null whenever nothing may be shown. It is never filled with
     * replacement characters or raw bytes.
     */
    data class FilePreview(
        val path: String,
        val kind: PreviewKind,
        val title: String,
        val charsetName: String,
        val sizeBytes: Long,
        val lineCount: Int,
        val text: String?,
        val message: String,
        val truncated: Boolean,
        val secretRedacted: Boolean,
        val sendDecision: ProjectExclusionPolicy.FileDecision
    ) {
        /** True when readable text is present and may be put on screen. */
        val hasText: Boolean get() = text != null

        /**
         * What the exclusion policy says about sending this file.
         *
         * This is not a promise that sending works; it only repeats the decision of
         * task 067 so the UI does not have to classify the file a second time.
         */
        val mayBeSent: Boolean
            get() = sendDecision == ProjectExclusionPolicy.FileDecision.ALLOWED &&
                kind == PreviewKind.TEXT

        /** Always true: a preview never leaves the device. */
        val isLocalOnly: Boolean get() = true
    }

    /**
     * Builds a preview for [path] from the bytes the caller has already read.
     *
     * The bytes are passed in deliberately: this object never touches the file
     * system, so a caller cannot accidentally preview something it should not.
     *
     * @param path         Project-relative path.
     * @param bytes        Content of the file.
     * @param sizeBytes    Real file size when it differs from [bytes] (truncated read).
     * @param exists       Whether the file is there at all.
     */
    fun preview(
        path: String,
        bytes: ByteArray,
        sizeBytes: Long = bytes.size.toLong(),
        exists: Boolean = true
    ): FilePreview {
        val exclusion = ProjectExclusionPolicy.classify(path, exists = exists, sizeBytes = sizeBytes)

        // 1. Security first. A secret is never previewed, and there is no override.
        if (exclusion.decision == ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET) {
            return FilePreview(
                path = path,
                kind = PreviewKind.BLOCKED_SECRET,
                title = "Gesperrt",
                charsetName = "",
                sizeBytes = sizeBytes,
                lineCount = 0,
                text = null,
                message = exclusion.message + " Es wird auch keine Vorschau angezeigt.",
                truncated = false,
                secretRedacted = false,
                sendDecision = exclusion.decision
            )
        }

        if (!exists) {
            return notFound(path, sizeBytes, exclusion.decision)
        }

        // 2. An empty file is not an error, but it must be said plainly.
        if (bytes.isEmpty()) {
            return FilePreview(
                path = path,
                kind = PreviewKind.EMPTY,
                title = "Leere Datei",
                charsetName = "",
                sizeBytes = sizeBytes,
                lineCount = 0,
                text = null,
                message = "Die Datei „${fileNameOf(path)}“ ist leer. Es gibt nichts anzuzeigen.",
                truncated = false,
                secretRedacted = false,
                sendDecision = exclusion.decision
            )
        }

        // 3. Extension check: a .png is binary even if someone named it .txt.
        if (extensionOf(path) in binaryExtensions) {
            return binary(path, sizeBytes, exclusion.decision, "Der Dateityp „.${extensionOf(path)}“ ist ein Binärformat.")
        }

        // 4. Content check: the extension lies often enough that it cannot be trusted.
        if (looksBinary(bytes)) {
            return binary(
                path,
                sizeBytes,
                exclusion.decision,
                "Der Inhalt ist keine lesbare Textdatei."
            )
        }

        // 5. Decode strictly. Undecodable content gets an explanation, not mojibake.
        val decoded = decodeUtf8(bytes)
            ?: return FilePreview(
                path = path,
                kind = PreviewKind.NOT_DECODABLE,
                title = "Kein lesbarer Text",
                charsetName = "",
                sizeBytes = sizeBytes,
                lineCount = 0,
                text = null,
                message = "Der Inhalt von „${fileNameOf(path)}“ ließ sich nicht als Text lesen. " +
                    "Vielleicht ist die Datei in einem anderen Zeichensatz gespeichert " +
                    "oder gar keine Textdatei. Es wird nichts angezeigt, damit keine " +
                    "unlesbaren Zeichen erscheinen.",
                truncated = false,
                secretRedacted = false,
                sendDecision = exclusion.decision
            )

        // 6. Redact before anything can reach the screen.
        val redacted = SecretMasker.redact(decoded)
        val secretRedacted = redacted != decoded

        // 7. Keep it usable: cut long content, but say so.
        val cut = cutToLimits(redacted)
        val message = buildString {
            append(fileNameOf(path))
            append(" — ")
            append(sizeInWords(sizeBytes))
            append(", Zeichensatz UTF-8, ")
            append(if (cut.truncated) "gekürzte Vorschau" else "${cut.totalLines} Zeilen")
            if (secretRedacted) {
                append(". Geheimnis-ähnliche Stellen sind maskiert.")
            }
        }

        return FilePreview(
            path = path,
            kind = PreviewKind.TEXT,
            title = "Textdatei",
            charsetName = "UTF-8",
            sizeBytes = sizeBytes,
            lineCount = cut.totalLines,
            text = cut.text,
            message = message,
            truncated = cut.truncated,
            secretRedacted = secretRedacted,
            sendDecision = exclusion.decision
        )
    }

    /**
     * Whether the bytes look like binary content.
     *
     * Two independent signals, because both miss cases on their own:
     *  - a NUL byte, which no text file contains,
     *  - a high share of control bytes such as 0x01 or 0x1F.
     *
     * Bytes above 0x7F are not counted: they are normal in UTF-8 text and would
     * make German umlauts look binary.
     */
    fun looksBinary(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val sample = if (bytes.size <= BINARY_SNIFF_BYTES) bytes else bytes.copyOf(BINARY_SNIFF_BYTES)

        var control = 0
        for (raw in sample) {
            val b = raw.toInt() and 0xFF
            if (b == 0x00) return true
            val isControl = b < 0x09 || (b in 0x0E..0x1F) || b == 0x7F
            if (isControl) control++
        }
        return control.toDouble() / sample.size > BINARY_CONTROL_RATIO
    }

    /**
     * Decodes strictly as UTF-8, or returns null when the bytes are not valid UTF-8.
     *
     * Strict decoding is the point: a lenient decoder turns broken bytes into
     * U+FFFD and the user sees a page of � instead of an explanation.
     */
    fun decodeUtf8(bytes: ByteArray): String? = try {
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: CharacterCodingException) {
        null
    }

    // ── internals ────────────────────────────────────────────────────────────

    private fun binary(
        path: String,
        sizeBytes: Long,
        decision: ProjectExclusionPolicy.FileDecision,
        why: String
    ): FilePreview = FilePreview(
        path = path,
        kind = PreviewKind.BINARY,
        title = "Binärdatei",
        charsetName = "",
        sizeBytes = sizeBytes,
        lineCount = 0,
        text = null,
        message = "„${fileNameOf(path)}“ ist eine Binärdatei. $why " +
            "Solche Dateien werden nicht als Text angezeigt und nicht gesendet.",
        truncated = false,
        secretRedacted = false,
        sendDecision = decision
    )

    private fun notFound(
        path: String,
        sizeBytes: Long,
        decision: ProjectExclusionPolicy.FileDecision
    ): FilePreview = FilePreview(
        path = path,
        kind = PreviewKind.NOT_FOUND,
        title = "Nicht gefunden",
        charsetName = "",
        sizeBytes = sizeBytes,
        lineCount = 0,
        text = null,
        message = ProjectExclusionPolicy.classify(path, exists = false).message,
        truncated = false,
        secretRedacted = false,
        sendDecision = decision
    )

    private data class CutText(
        val text: String,
        val truncated: Boolean,
        val totalLines: Int,
        val shownLines: Int
    )

    /**
     * Cuts to [MAX_PREVIEW_LINES] and [MAX_PREVIEW_CHARS], appending a marker that
     * states how much was left out. A cut preview never looks like a whole file.
     */
    private fun cutToLimits(input: String): CutText {
        val totalLines = if (input.isEmpty()) 0 else input.count { it == '\n' } + 1

        var lines = input.lines()
        var shownLines = lines.size
        var truncated = false

        if (lines.size > MAX_PREVIEW_LINES) {
            lines = lines.subList(0, MAX_PREVIEW_LINES)
            shownLines = MAX_PREVIEW_LINES
            truncated = true
        }

        var body = lines.joinToString("\n")
        if (body.length > MAX_PREVIEW_CHARS) {
            body = body.substring(0, MAX_PREVIEW_CHARS)
            truncated = true
        }

        val marker = if (!truncated) {
            ""
        } else {
            "\n\n$TRUNCATION_MARKER gezeigt werden $shownLines von $totalLines Zeilen. " +
                "Der Rest der Datei steht hier nicht.]"
        }
        return CutText(body + marker, truncated, totalLines, shownLines)
    }

    private fun sizeInWords(sizeBytes: Long): String = when {
        sizeBytes < 1024 -> "$sizeBytes Byte"
        sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
        else -> "${sizeBytes / (1024 * 1024)} MB"
    }

    private fun fileNameOf(path: String): String =
        path.trim().replace('\\', '/').substringAfterLast('/')

    private fun extensionOf(path: String): String =
        fileNameOf(path).substringAfterLast('.', "").lowercase()
}
