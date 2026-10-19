package org.claudroide.app

import org.claudroide.app.feature.project.ProjectExclusionPolicy
import org.claudroide.app.feature.project.RequestDataPreview
import org.claudroide.app.feature.project.RequestDataPreview.ContentKind
import org.claudroide.app.feature.project.RequestDataPreview.ContentRef
import org.claudroide.app.feature.project.RequestDataPreview.Recipient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 066 — "Übertragene Daten prüfen".
 *
 * Acceptance criteria under test:
 *  - The user can remove individual items.
 *  - Attachments and images are listed too.
 *
 * Plus the task's protection rule: the preview must not keep a copy of the
 * contents. That is structural — [ContentRef] holds a path and a line range, never
 * text — so there is nothing for the tests to assert about a stored copy; instead
 * they pin that the preview derives everything from the pending list, and that a
 * removal can never leave a stale summary behind.
 */
class RequestDataPreviewTest {

    private val recipient = Recipient(
        providerId = "anthropic",
        providerDisplayName = "Anthropic Claude API",
        modelId = "claude-opus-5-5"
    )

    private fun previewOf(vararg paths: String) = RequestDataPreview.build(recipient, paths.toList())

    // ── Criterion: the user can remove individual items ───────────────────────

    @Test
    fun aSelectedFile_appearsInThePreview() {
        val preview = previewOf("src/Main.kt")

        assertEquals(1, preview.pendingToSend.size)
        assertEquals("src/Main.kt", preview.pendingToSend.first().projectRelativePath)
        assertTrue(preview.canSend)
    }

    @Test
    fun removingOneItem_leavesTheOthers() {
        val preview = RequestDataPreview.build(
            recipient,
            listOf("src/Main.kt", "README.md", "docs/notes.md")
        )

        val reduced = RequestDataPreview.remove(preview, "file:README.md")

        assertEquals(2, reduced.pendingToSend.size)
        assertFalse(
            "the removed file must be gone",
            reduced.pendingToSend.any { it.projectRelativePath == "README.md" }
        )
        assertTrue(
            "the others must remain",
            reduced.pendingToSend.any { it.projectRelativePath == "src/Main.kt" } &&
                reduced.pendingToSend.any { it.projectRelativePath == "docs/notes.md" }
        )
    }

    @Test
    fun removingAnUnknownItem_changesNothing() {
        val preview = previewOf("src/Main.kt")

        val unchanged = RequestDataPreview.remove(preview, "file:gibt-es-nicht.txt")

        assertEquals(1, unchanged.pendingToSend.size)
    }

    @Test
    fun theSummary_followsARemovalInsteadOfGoingStale() {
        val preview = RequestDataPreview.build(recipient, listOf("a.kt", "b.kt", "c.kt"))
        assertTrue("must start at three: ${preview.summaryLines}", preview.summaryLines.any { it.contains("3 Datei") })

        val reduced = RequestDataPreview.remove(preview, "file:b.kt")

        assertTrue(
            "the count must be recomputed, not left at three: ${reduced.summaryLines}",
            reduced.summaryLines.any { it.contains("2 Datei") }
        )
        assertFalse(
            "a stale count would be worse than no preview",
            reduced.summaryLines.any { it.contains("3 Datei") }
        )
    }

    @Test
    fun removingEverything_disablesSendingRatherThanSendingAnEmptyRequest() {
        var preview = previewOf("a.kt", "b.kt")
        preview = RequestDataPreview.remove(preview, "file:a.kt")
        preview = RequestDataPreview.remove(preview, "file:b.kt")

        assertFalse("nothing left means nothing to send", preview.canSend)
        assertTrue(
            "must say so plainly: ${preview.summaryLines}",
            preview.summaryLines.any { it.contains("keine Dateien") }
        )
    }

    @Test
    fun aRemovedItem_canBePutBack() {
        val preview = RequestDataPreview.build(recipient, listOf("a.kt", "b.kt"))
        val reduced = RequestDataPreview.remove(preview, "file:a.kt")

        val restored = RequestDataPreview.restore(reduced, "file:a.kt")

        assertEquals(2, restored.pendingToSend.size)
        assertTrue(restored.removedByUser.isEmpty())
    }

    // ── Criterion: attachments and images are listed too ─────────────────────

    @Test
    fun anImageAttachment_isListedWithItsKind() {
        val image = ContentRef(
            id = "attach:1",
            projectRelativePath = "",
            kind = ContentKind.IMAGE_ATTACHMENT
        )

        val preview = RequestDataPreview.build(recipient, listOf("src/Main.kt"), attachments = listOf(image))

        assertTrue(
            "the image must be listed, not hidden: ${preview.summaryLines}",
            preview.summaryLines.any { it.contains("1 Bild") }
        )
    }

    @Test
    fun aNonImageAttachment_isCountedSeparatelyFromAnImage() {
        val preview = RequestDataPreview.build(
            recipient,
            listOf("src/Main.kt"),
            attachments = listOf(
                ContentRef("attach:1", "", ContentKind.IMAGE_ATTACHMENT),
                ContentRef("attach:2", "dokument.pdf", ContentKind.FILE_ATTACHMENT)
            )
        )

        val counts = RequestDataPreview.countByKind(preview)
        assertEquals(1, counts[ContentKind.IMAGE_ATTACHMENT])
        assertEquals(1, counts[ContentKind.FILE_ATTACHMENT])
        assertEquals(1, counts[ContentKind.FILE_TEXT])
    }

    @Test
    fun anAttachment_canBeRemovedLikeAnythingElse() {
        val preview = RequestDataPreview.build(
            recipient,
            listOf("src/Main.kt"),
            attachments = listOf(ContentRef("attach:1", "", ContentKind.IMAGE_ATTACHMENT))
        )

        val reduced = RequestDataPreview.remove(preview, "attach:1")

        assertEquals(1, reduced.pendingToSend.size)
        assertFalse(reduced.summaryLines.any { it.contains("Bild") })
    }

    @Test
    fun theUsersOwnMessage_isListedSeparatelyFromFiles() {
        val preview = RequestDataPreview.build(recipient, listOf("src/Main.kt"), userMessage = "Was steht hier?")

        assertTrue(
            "the own message must be visible: ${preview.summaryLines}",
            preview.summaryLines.any { it.contains("1 eigene Nachricht") }
        )
    }

    // ── The recipient is named, because "where does this go" is the point ────

    @Test
    fun theRecipient_isAlwaysNamed() {
        val preview = previewOf("a.kt")

        assertTrue(
            "must name provider and model: ${preview.summaryLines}",
            preview.summaryLines.first().contains("Anthropic Claude API") &&
                preview.summaryLines.first().contains("claude-opus-5-5")
        )
    }

    @Test
    fun aCustomEndpointHost_isShownSoTheUserKnowsWhereDataLands() {
        val custom = recipient.copy(endpointHost = "mein-server.example")
        val preview = RequestDataPreview.build(custom, listOf("a.kt"))

        assertTrue(
            "a custom host is exactly the thing worth showing: ${preview.summaryLines}",
            preview.summaryLines.first().contains("mein-server.example")
        )
    }

    // ── Protection: secrets never reach the preview, and nothing is stored ───

    @Test
    fun aSecretFile_isNeverListedAsSendable() {
        val preview = previewOf("src/Main.kt", "backend/.env")

        assertFalse(
            "a secret must never appear in the send list",
            preview.pendingToSend.any { it.projectRelativePath == "backend/.env" }
        )
        assertEquals(1, preview.pendingToSend.size)
    }

    @Test
    fun aSecretFile_isReportedAsOmittedWithAReason() {
        val preview = previewOf("backend/.env")

        assertEquals(1, preview.omitted.size)
        assertTrue(
            "the omission must be explained: ${preview.omitted.first().reason}",
            preview.omitted.first().reason.isNotBlank()
        )
    }

    @Test
    fun aSecretCannotBeReAddedByRemovingSomethingElse() {
        val preview = previewOf("backend/.env", "src/Main.kt")

        val reduced = RequestDataPreview.remove(preview, "file:src/Main.kt")

        assertTrue(
            "removal must never surface a secret",
            reduced.pendingToSend.none { it.projectRelativePath == "backend/.env" }
        )
        assertFalse("nothing sendable is left", reduced.canSend)
    }

    @Test
    fun heavyFilesAreHeldBackUnlessTheCallerDeliberatelyIncludesThem() {
        val held = RequestDataPreview.build(recipient, listOf("build/output.txt"))
        assertTrue("a heavy file must not go out by default", held.pendingToSend.isEmpty())

        val included = RequestDataPreview.build(
            recipient,
            listOf("build/output.txt"),
            includeHeavy = true
        )
        assertEquals(1, included.pendingToSend.size)
    }

    @Test
    fun aMissingFile_isReportedInsteadOfClaimedAsSendable() {
        val preview = RequestDataPreview.build(
            recipient,
            listOf("src/Ghost.kt", "src/Main.kt"),
            existingPaths = setOf("src/Main.kt")
        )

        assertEquals("the ghost file must be reported as missing", 1, preview.omitted.size)
        assertEquals(
            "and must not be announced as sendable",
            1,
            preview.pendingToSend.size
        )
        assertTrue(
            "the rest of the request still works: ${preview.omitted.first().reason}",
            preview.canSend
        )
    }

    @Test
    fun duplicateSelections_areListedOnce() {
        val preview = RequestDataPreview.build(recipient, listOf("a.kt", "a.kt", "a.kt"))

        assertEquals("the same file twice is still one file", 1, preview.pendingToSend.size)
    }

    @Test
    fun aContentRef_describesItselfInGermanWithItsLineRange() {
        val excerpt = ContentRef("e:1", "src/Main.kt", ContentKind.FILE_EXCERPT, firstLine = 10, lastLine = 12)

        val text = excerpt.describe()

        assertTrue(text.contains("Auszug"))
        assertTrue("must show where it comes from: $text", text.contains("src/Main.kt"))
        assertTrue("must show the range: $text", text.contains("Zeilen 10"))
    }

    @Test
    fun thePreviewStoresNoTextItself() {
        // The protection rule, expressed as a type check: a ref knows where content
        // lives, never what it says. If someone adds a text field to ContentRef this
        // stops compiling only if they also update this, which is the point.
        val ref = ContentRef("file:a.kt", "a.kt", ContentKind.FILE_TEXT)
        val properties = ref::class.java.declaredFields.map { it.name }

        assertFalse(
            "a preview ref must not carry the content itself: $properties",
            properties.any { it.contains("text", ignoreCase = true) ||
                it.contains("content", ignoreCase = true) ||
                it.contains("body", ignoreCase = true) }
        )
    }

    @Test
    fun anEmptySelection_producesAnHonestSummaryNotACrash() {
        val preview = RequestDataPreview.build(recipient, emptyList())

        assertFalse(preview.canSend)
        assertTrue(preview.summaryLines.first().contains("Anthropic"))
    }

    @Test
    fun theExclusionPolicy_isTheSingleSourceOfSecrets() {
        // Whatever the exclusion policy blocks as a secret must not be listed here,
        // so the two cannot drift apart.
        val secretPath = "keys/id_rsa"
        assertEquals(
            ProjectExclusionPolicy.FileDecision.BLOCKED_SECRET,
            ProjectExclusionPolicy.classify(secretPath).decision
        )
        assertTrue(
            RequestDataPreview.build(recipient, listOf(secretPath)).pendingToSend.isEmpty()
        )
    }
}
