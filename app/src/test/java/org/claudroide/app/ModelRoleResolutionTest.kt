package org.claudroide.app

import org.claudroide.app.feature.provider.ModelRole
import org.claudroide.app.feature.provider.ModelSelectionManager
import org.claudroide.app.feature.provider.ModelSelectionResult
import org.junit.Assert.*
import org.junit.Test

class ModelRoleResolutionTest {

    private val manager = ModelSelectionManager()

    @Test
    fun modelRole_fromAlias_resolvesKnownRoles() {
        assertEquals(ModelRole.FAST, ModelRole.fromAlias("role:fast"))
        assertEquals(ModelRole.CODER, ModelRole.fromAlias("role:coder"))
        assertEquals(ModelRole.VISION, ModelRole.fromAlias("role:vision"))
        assertEquals(ModelRole.SUMMARY, ModelRole.fromAlias("role:summary"))
        assertNull(ModelRole.fromAlias("role:unknown"))
    }

    @Test
    fun resolveRole_forAnthropic_mapsAppropriately() {
        val fast = manager.resolveRole(ModelRole.FAST, "anthropic")
        assertEquals("anthropic", fast.providerId)
        assertEquals("claude-haiku-4-5-20251001", fast.modelId)
        assertTrue(fast.isVerified)

        val coder = manager.resolveRole(ModelRole.CODER, "anthropic")
        assertEquals("anthropic", coder.providerId)
        assertEquals("claude-sonnet-5-5", coder.modelId)
        assertTrue(coder.isVerified)
    }

    @Test
    fun resolveRole_forOpenAI_mapsAppropriately() {
        val fast = manager.resolveRole(ModelRole.FAST, "openai")
        assertEquals("openai", fast.providerId)
        assertEquals("gpt-4o-mini", fast.modelId)
        assertTrue(fast.isVerified)

        val coder = manager.resolveRole(ModelRole.CODER, "openai")
        assertEquals("openai", coder.providerId)
        assertEquals("gpt-4o", coder.modelId)
        assertTrue(coder.isVerified)
    }

    @Test
    fun resolveRole_forOmniRoute_preservesRoleAlias() {
        val fast = manager.resolveRole(ModelRole.FAST, "omniroute")
        assertEquals("omniroute", fast.providerId)
        assertEquals("role:fast", fast.modelId)
        assertTrue(fast.isVerified)
    }

    @Test
    fun requestModelChange_withRoleAlias_resolvesUnderlyingModel() {
        val result = manager.requestModelChange("anthropic", "role:coder")
        assertTrue(result is ModelSelectionResult.RequiresConfirmation)
        val pending = result as ModelSelectionResult.RequiresConfirmation
        assertEquals("claude-sonnet-5-5", pending.newSelection.modelId)
        assertTrue(pending.newSelection.isVerified)
    }
}
