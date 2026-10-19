package org.claudroide.app

import org.claudroide.app.feature.provider.CapabilityCheckResult
import org.claudroide.app.feature.provider.ModelCapability
import org.claudroide.app.feature.provider.ModelCapabilityRegistry
import org.claudroide.app.feature.provider.ModelSelectionManager
import org.claudroide.app.feature.provider.ModelSelectionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 061 — "Modellfähigkeiten".
 *
 * Acceptance criteria under test:
 *  - An unsupported capability is explained before an action, not after it fails.
 *  - Capabilities are never derived from the model name.
 */
class ModelCapabilityTest {

    // ── Criterion: no inference from the model name ────────────────────────────

    @Test
    fun unknownModel_isReportedAsUnknown_notAsUnsupported() {
        val result = ModelCapabilityRegistry.check(
            "some-model-i-made-up",
            ModelCapability.VISION_IMAGE_INPUT
        )

        assertTrue(
            "\"not checked\" must not masquerade as \"not supported\"",
            result is CapabilityCheckResult.Unknown
        )
        assertNotNull((result as CapabilityCheckResult.Unknown).explanation)
    }

    @Test
    fun nameThatLooksLikeAVisionModel_doesNotGrantVision() {
        // The name implies vision; the registry must not agree on that basis.
        val result = ModelCapabilityRegistry.check(
            "claude-vision-turbo-9000",
            ModelCapability.VISION_IMAGE_INPUT
        )
        assertTrue(result is CapabilityCheckResult.Unknown)
    }

    @Test
    fun knownModel_hasDocumentedEntriesWithSourceAndDate() {
        val entries = ModelCapabilityRegistry.getCapabilities("claude-opus-5-5")

        assertTrue("a known model must have entries", entries.isNotEmpty())
        entries.forEach {
            assertTrue("every entry needs a source", it.sourceUrl.startsWith("http"))
            assertTrue("every entry needs a verification date", it.verifiedDate.isNotBlank())
        }
    }

    @Test
    fun unknownModel_hasNoEntries() {
        assertTrue(ModelCapabilityRegistry.getCapabilities("nope").isEmpty())
    }

    // ── Criterion: unsupported is explained before the action ───────────────────

    @Test
    fun missingCapability_explainsItselfInGerman() {
        // JSON mode is documented for the OpenAI family, not for the Anthropic entries.
        val result = ModelCapabilityRegistry.check("claude-opus-5-5", ModelCapability.JSON_MODE)

        assertTrue(result is CapabilityCheckResult.NotSupported)
        val explanation = (result as CapabilityCheckResult.NotSupported).explanation
        assertTrue("must name the model", explanation.contains("claude-opus-5-5"))
        // The wording is a "kein/keine/keinen ..." negation, not a literal "nicht".
        assertTrue(
            "must state the missing capability in a German negation",
            explanation.contains("unterstützt keinen JSON-Modus")
        )
        assertTrue("must point at the documentation", explanation.contains("laut Dokumentation"))
    }

    @Test
    fun supportedCapability_isReportedAsSupported() {
        val result = ModelCapabilityRegistry.check(
            "claude-opus-5-5",
            ModelCapability.VISION_IMAGE_INPUT
        )
        assertTrue(result is CapabilityCheckResult.Supported)
    }

    @Test
    fun everyCapability_hasAGermanLabel() {
        ModelCapability.values().forEach {
            assertTrue("label must not be empty", ModelCapabilityRegistry.label(it).isNotBlank())
        }
    }

    // ── Files are never sent on a guess ────────────────────────────────────────

    @Test
    fun fileIsBlocked_whenVisionIsNotSupported() {
        val result = ModelCapabilityRegistry.checkFileInputAllowed("claude-opus-5-5", "screenshot.png")
        assertTrue(result is CapabilityCheckResult.Supported)
    }

    @Test
    fun fileIsBlocked_forUnknownModel_withExplicitReason() {
        val result = ModelCapabilityRegistry.checkFileInputAllowed("mystery-model", "screenshot.png")

        assertTrue(result is CapabilityCheckResult.Unknown)
        val explanation = (result as CapabilityCheckResult.Unknown).explanation
        assertTrue("must say the file was not sent", explanation.contains("nicht gesendet"))
        assertTrue("must name the file", explanation.contains("screenshot.png"))
    }

    // ── Per-model entries, not a shared family list ────────────────────────────

    @Test
    fun capabilitiesAreNotSharedBlindlyAcrossTheFamily() {
        // Guards the specific bug this registry had: one list copied onto every
        // model. JSON mode is OpenAI-only in the current entries, so it must not
        // appear on an Anthropic model.
        val anthropicJson =
            ModelCapabilityRegistry.check("claude-sonnet-5-5", ModelCapability.JSON_MODE)
        val openAiJson = ModelCapabilityRegistry.check("gpt-4o", ModelCapability.JSON_MODE)

        assertTrue(anthropicJson is CapabilityCheckResult.NotSupported)
        assertTrue(openAiJson is CapabilityCheckResult.Supported)
    }

    // ── Model switching surfaces capability state before applying ──────────────

    @Test
    fun modelChange_alwaysRequiresConfirmation() {
        val manager = ModelSelectionManager()
        val result = manager.requestModelChange(
            providerId = "anthropic",
            newModelId = "claude-opus-5-5",
            currentModelId = "gpt-4o"
        )

        assertTrue(result is ModelSelectionResult.RequiresConfirmation)
        val pending = result as ModelSelectionResult.RequiresConfirmation
        assertTrue(
            "the change must be explained before it happens",
            pending.warningText.contains("claude-opus-5-5")
        )
    }

    @Test
    fun unverifiedModel_switchIsFlaggedAsUnverified() {
        val manager = ModelSelectionManager()
        val result = manager.requestModelChange(
            providerId = "anthropic",
            newModelId = "some-local-model"
        ) as ModelSelectionResult.RequiresConfirmation

        assertFalse(result.newSelection.isVerified)
        assertTrue(
            "an unverified model must say so",
            result.warningText.contains("nicht im verifizierten Katalog")
        )
    }

    @Test
    fun blankModelId_isRejected() {
        val manager = ModelSelectionManager()
        assertTrue(manager.requestModelChange("anthropic", "  ") is ModelSelectionResult.Invalid)
        assertTrue(manager.requestModelChange("", "claude-opus-5-5") is ModelSelectionResult.Invalid)
    }

    @Test
    fun confirmedSwitch_keepsTheVerifiedFlagAndCapabilities() {
        val manager = ModelSelectionManager()
        val pending = manager.requestModelChange("anthropic", "claude-opus-5-5")
            as ModelSelectionResult.RequiresConfirmation

        val confirmed = manager.confirmModelChange(pending) as ModelSelectionResult.Confirmed

        assertTrue(confirmed.newSelection.isVerified)
        assertTrue(confirmed.newSelection.capabilities.contains(ModelCapability.STREAMING))
    }

    @Test
    fun fileInputCheck_blocksUnknownModel_withExplanation() {
        val manager = ModelSelectionManager()
        val pending = manager.requestModelChange("anthropic", "mystery-model")
            as ModelSelectionResult.RequiresConfirmation
        val selection = manager.confirmModelChange(pending) as ModelSelectionResult.Confirmed

        val denial = manager.checkFileInputAllowed(selection.newSelection, "diagramm.png")

        assertNotNull("an unknown model must not silently accept a file", denial)
        assertTrue(denial!!.contains("diagramm.png"))
    }

    @Test
    fun fileInputCheck_allowsVerifiedVisionModel() {
        val manager = ModelSelectionManager()
        val pending = manager.requestModelChange("anthropic", "claude-opus-5-5")
            as ModelSelectionResult.RequiresConfirmation
        val selection = manager.confirmModelChange(pending) as ModelSelectionResult.Confirmed

        assertNull(manager.checkFileInputAllowed(selection.newSelection, "diagramm.png"))
    }
}
