package org.claudroide.app.feature.media

import org.junit.Assert.*
import org.junit.Test

class MediaBridgeTest {

    @Test
    fun mediaGenerationRequest_hasExpectedDefaults() {
        val req = MediaGenerationRequest(
            prompt = "A cute green bot and orange robot"
        )

        assertEquals("omniroute", req.providerId)
        assertEquals("segmind/flux-schnell", req.modelId)
        assertEquals("1:1", req.aspectRatio)
        assertEquals(25, req.steps)
        assertEquals(7.5f, req.cfgScale)
    }

    @Test
    fun mediaGenerationResult_formatsCorrectly() {
        val res = MediaGenerationResult(
            prompt = "Test prompt",
            modelUsed = "flux",
            providerUsed = "Pollinations.ai",
            localFilePath = "/path/to/img.png",
            imageUrl = "https://example.com/img.png",
            status = "SUCCESS"
        )

        assertEquals("SUCCESS", res.status)
        assertNotNull(res.id)
        assertNull(res.errorMessage)
    }

    @Test
    fun mediaProviderInfo_containsRequiredModels() {
        val info = MediaProviderInfo(
            id = "segmind",
            name = "Segmind",
            description = "Segmind direct pipeline",
            models = listOf("segmind/flux-schnell", "segmind/sdxl1.0-txt2img"),
            defaultModel = "segmind/flux-schnell"
        )

        assertEquals("segmind", info.id)
        assertTrue(info.models.contains("segmind/flux-schnell"))
    }
}
