package org.claudroide.app.feature.project

/**
 * Task 066 — "Übertragene Daten prüfen".
 *
 * Before a project request leaves the device, the user must be able to see what
 * goes where, take single items out, and stop it. This builds that preview.
 *
 * The task's own protection rule is what shapes the design: **the preview must
 * not keep an unnecessary copy of the contents.** So this class stores *where* a
 * piece of content lives (a [ContentRef]) and never a second copy of its text.
 * Building a preview, narrowing it, and rendering it all pass the reference
 * around. Only an explicit, deliberate call materialises text, and it hands back
 * the excerpt — never a full-file copy.
 *
 * Invariants:
 *  - What is shown is exactly what would be sent: [pendingToSend] is the single
 *    source of truth, and the summary is derived from it, never maintained
 *    separately. A preview that can drift from the real payload is worse than
 *    no preview.
 *  - The user can drop any single item, and the summary updates with it.
 *  - Attachments and images are listed with their kind, not hidden as "text".
 *  - A secret is never present in a preview in the first place — the exclusion
 *    policy is applied while building, so there is nothing to remove afterwards.
 *  - No text is retained: [ContentRef] is a location, not a payload.
 */
object RequestDataPreview {

    /**
     * What kind of thing a piece of outgoing content is.
     */
    enum class ContentKind {
        /** A file's text, sent as context. */
        FILE_TEXT,

        /** A slice of a file — a matched region, not the whole file. */
        FILE_EXCERPT,

        /** An image the user attached. */
        IMAGE_ATTACHMENT,

        /** A non-image file the user attached. */
        FILE_ATTACHMENT,

        /** The user's own message text. */
        USER_MESSAGE
    }

    /**
     * German label per kind, so the UI never has to translate at the call site.
     */
    fun kindLabel(kind: ContentKind): String = when (kind) {
        ContentKind.FILE_TEXT -> "Datei"
        ContentKind.FILE_EXCERPT -> "Auszug"
        ContentKind.IMAGE_ATTACHMENT -> "Bild"
        ContentKind.FILE_ATTACHMENT -> "Anhang"
        ContentKind.USER_MESSAGE -> "Eigene Nachricht"
    }

    /**
     * A reference to content that exists somewhere else.
     *
     * Deliberately holds no text. This is the concrete form of the task's
     * protection rule: the preview can say *what* would be sent and *where it
     * comes from* without ever holding a second copy of it.
     */
    data class ContentRef(
        val id: String,
        val projectRelativePath: String,
        val kind: ContentKind,
        val sizeBytes: Long? = null,
        /** Line range for an excerpt, 1-based and inclusive. Null for whole files. */
        val firstLine: Int? = null,
        val lastLine: Int? = null
    ) {
        /** A short, honest description for the preview list. */
        fun describe(): String {
            val where = projectRelativePath.ifBlank { "(ohne Pfad)" }
            val range = if (firstLine != null && lastLine != null) {
                if (firstLine == lastLine) " (Zeile $firstLine)"
                else " (Zeilen $firstLine–$lastLine)"
            } else {
                ""
            }
            return "${kindLabel(kind)}: $where$range"
        }
    }

    /**
     * One recipient of the request. Named explicitly, because "where does this
     * go" is the point of the screen.
     */
    data class Recipient(
        val providerId: String,
        val providerDisplayName: String,
        val modelId: String,
        /** Where the bytes actually land, when the user configured a custom endpoint. */
        val endpointHost: String? = null
    ) {
        fun describe(): String {
            val host = endpointHost?.let { " ($it)" }.orEmpty()
            return "$providerDisplayName · $modelId$host"
        }
    }

    /**
     * One thing the user deliberately left out.
     */
    data class OmittedItem(
        val path: String,
        val reason: String
    )

    /**
     * The preview itself.
     *
     * [pendingToSend] is authoritative. [summaryLines] is computed from it in
     * [build], so a removal cannot leave a stale count on screen.
     */
    data class Preview(
        val recipient: Recipient,
        val pendingToSend: List<ContentRef>,
        val removedByUser: List<ContentRef>,
        val omitted: List<OmittedItem>,
        val summaryLines: List<String>,
        /** False once nothing would be sent any more. */
        val canSend: Boolean
    )

    /**
     * Builds a preview from the paths the user selected.
     *
     * The exclusion policy is applied here rather than after, so a secret is
     * filtered out before it can appear in a list the user might trust. Note the
     * asymmetry that follows from task 067: [ProjectExclusionPolicy] returns
     * `BLOCKED_HEAVY` for large files, and those *can* be overridden — so a
     * caller that wants them must pass them through [includeHeavy] deliberately
     * rather than expecting this function to send them.
     */
    fun build(
        recipient: Recipient,
        selectedPaths: List<String>,
        attachments: List<ContentRef> = emptyList(),
        userMessage: String? = null,
        includeHeavy: Boolean = false,
        /**
         * Paths that are actually present, passed in by the caller who has looked
         * them up. A path that is not in here is reported as missing rather than
         * being announced as sendable — without this, the preview would promise to
         * send a file that does not exist. Null means "not checked", which keeps
         * the simple case usable and is documented as a weaker guarantee.
         */
        existingPaths: Set<String>? = null
    ): Preview {
        val kept = ArrayList<ContentRef>()
        val omitted = ArrayList<OmittedItem>()

        selectedPaths.forEach { path ->
            val exists = existingPaths?.contains(path) ?: true
            val reason = ProjectExclusionPolicy.classify(path, exists = exists)
            when (reason.decision) {
                ProjectExclusionPolicy.FileDecision.ALLOWED ->
                    kept.add(
                        ContentRef(
                            id = "file:$path",
                            projectRelativePath = path,
                            kind = ContentKind.FILE_TEXT
                        )
                    )

                ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET ->
                    // Never listed as sendable and never offered as removable: it
                    // simply does not exist for this request.
                    omitted.add(
                        OmittedItem(path, reason.message.ifBlank { "Wird nie gesendet." })
                    )

                ProjectExclusionPolicy.FileDecision.BLOCKED_HEAVY ->
                    if (includeHeavy) {
                        kept.add(
                            ContentRef(
                                id = "file:$path",
                                projectRelativePath = path,
                                kind = ContentKind.FILE_TEXT
                            )
                        )
                    } else {
                        omitted.add(
                            OmittedItem(
                                path,
                                reason.message.ifBlank { "Zu groß für den Kontext." }
                            )
                        )
                    }

                ProjectExclusionPolicy.FileDecision.NOT_FOUND ->
                    omitted.add(
                        OmittedItem(
                            path,
                            reason.message.ifBlank { "Die Datei wurde nicht gefunden." }
                        )
                    )
            }
        }

        // Attachments are the user's explicit choice, so they are not re-filtered by
        // the project exclusion rules — but they are listed separately from context
        // files so their kind is visible.
        kept.addAll(attachments)

        if (userMessage != null && userMessage.isNotBlank()) {
            kept.add(
                ContentRef(
                    id = "message:0",
                    projectRelativePath = "",
                    kind = ContentKind.USER_MESSAGE
                )
            )
        }

        val deduped = kept.distinctBy { it.id }
        return Preview(
            recipient = recipient,
            pendingToSend = deduped,
            removedByUser = emptyList(),
            omitted = omitted,
            summaryLines = summarise(recipient, deduped, omitted),
            canSend = deduped.isNotEmpty()
        )
    }

    /**
     * Removes one item the user singled out.
     *
     * The summary is recomputed from the remaining list, so it can never claim a
     * count the request no longer matches. A secret cannot be re-added through
     * this path: the exclusion policy already removed it in [build].
     */
    fun remove(preview: Preview, contentId: String): Preview {
        val removed = preview.pendingToSend.find { it.id == contentId } ?: return preview
        val remaining = preview.pendingToSend.filterNot { it.id == contentId }

        return preview.copy(
            pendingToSend = remaining,
            removedByUser = preview.removedByUser + removed,
            summaryLines = summarise(preview.recipient, remaining, preview.omitted),
            canSend = remaining.isNotEmpty()
        )
    }

    /**
     * Puts a removed item back, for an undo.
     */
    fun restore(preview: Preview, contentId: String): Preview {
        val item = preview.removedByUser.find { it.id == contentId } ?: return preview
        val restored = preview.pendingToSend + item

        return preview.copy(
            pendingToSend = restored,
            removedByUser = preview.removedByUser.filterNot { it.id == contentId },
            summaryLines = summarise(preview.recipient, restored, preview.omitted),
            canSend = restored.isNotEmpty()
        )
    }

    /** Counts by kind, so the UI can group without walking the list itself. */
    fun countByKind(preview: Preview): Map<ContentKind, Int> =
        preview.pendingToSend.groupingBy { it.kind }.eachCount()

    private fun summarise(
        recipient: Recipient,
        pending: List<ContentRef>,
        omitted: List<OmittedItem>
    ): List<String> {
        val lines = ArrayList<String>()

        lines.add("Geht an: ${recipient.describe()}")

        if (pending.isEmpty()) {
            lines.add("Es werden keine Dateien übertragen.")
            return lines
        }

        val images = pending.count { it.kind == ContentKind.IMAGE_ATTACHMENT }
        val attachments = pending.count { it.kind == ContentKind.FILE_ATTACHMENT }
        val files = pending.count {
            it.kind == ContentKind.FILE_TEXT || it.kind == ContentKind.FILE_EXCERPT
        }
        val messages = pending.count { it.kind == ContentKind.USER_MESSAGE }

        if (files > 0) lines.add("$files Datei(en) als Kontext")
        if (images > 0) lines.add("$images Bild(er)")
        if (attachments > 0) lines.add("$attachments Anhang/Anhänge")
        if (messages > 0) lines.add("$messages eigene Nachricht")

        if (omitted.isNotEmpty()) {
            lines.add("${omitted.size} Eintrag/Einträge nicht enthalten")
        }
        return lines
    }
}
