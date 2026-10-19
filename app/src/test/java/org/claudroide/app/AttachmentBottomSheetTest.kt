package org.claudroide.app

import org.claudroide.app.feature.chat.AttachmentOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for AttachmentOption enum — the options presented in the
 * attachment/context bottom sheet. Each option must provide a title,
 * description, icon, and optionally a badge.
 */
class AttachmentBottomSheetTest {

    @Test
    fun allOptions_haveTitleAndDescription() {
        AttachmentOption.entries.forEach { option ->
            assert(option.titleRes != 0) { "${option.name} missing titleRes" }
            assert(option.descriptionRes != 0) { "${option.name} missing descriptionRes" }
        }
    }

    @Test
    fun allOptions_haveIcon() {
        AttachmentOption.entries.forEach { option ->
            assertNotNull("${option.name} missing icon", option.icon)
        }
    }

    @Test
    fun badgeText_isNull_whenNotProvided() {
        val noBadge = AttachmentOption.PHOTOS_AND_DOCS
        assertNull(noBadge.badgeText)
    }

    @Test
    fun badgeText_isSet_whenProvided() {
        val withBadge = AttachmentOption.FILES_AND_CODE
        assertEquals(".kt, .json", withBadge.badgeText)
    }

    @Test
    fun entries_countMatchesExpected() {
        assertEquals(5, AttachmentOption.entries.size)
    }
}
