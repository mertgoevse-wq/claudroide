package org.claudroide.app

import org.claudroide.app.feature.provider.ModelOrigin
import org.claudroide.app.feature.provider.ModelRegistry
import org.junit.Assert.*
import org.junit.Test

class ModelNameTest {

    @Test
    fun getModelsForProvider_returnsOfficialModelsWithoutFabrication() {
        val anthropicModels = ModelRegistry.getModelsForProvider("anthropic")
        assertTrue(anthropicModels.isNotEmpty())

        val sonnet = anthropicModels.find { it.id == "claude-sonnet-5-5" }
        assertNotNull(sonnet)
        assertEquals(ModelOrigin.OFFICIAL_PROVIDER_CATALOG, sonnet!!.origin)
        assertFalse(sonnet.isManualUserEntry)
        assertTrue(sonnet.supportsTools)
        assertTrue(sonnet.supportsVision)

        val haiku = anthropicModels.find { it.id == "claude-haiku-4-5-20251001" }
        assertNotNull(haiku)
        assertEquals("Claude Haiku 4.5", haiku!!.displayName)
    }

    @Test
    fun addManualModel_clearlyMarksOriginAsCustomUserInput() {
        val custom = ModelRegistry.addManualModel(
            providerId = "local_server",
            modelId = "deepseek-coder-v2:16b",
            displayName = "DeepSeek Coder V2"
        )

        assertEquals("deepseek-coder-v2:16b", custom.id)
        assertEquals("DeepSeek Coder V2", custom.displayName)
        assertEquals(ModelOrigin.MANUAL_USER_INPUT, custom.origin)
        assertTrue(custom.isManualUserEntry)

        // Present in provider's model list
        val localModels = ModelRegistry.getModelsForProvider("local_server")
        assertTrue(localModels.any { it.id == "deepseek-coder-v2:16b" })

        // Cleanup
        ModelRegistry.removeManualModel("local_server", "deepseek-coder-v2:16b")
    }

    @Test(expected = IllegalArgumentException::class)
    fun addManualModel_rejectsBlankId() {
        ModelRegistry.addManualModel("anthropic", "   ")
    }
}
