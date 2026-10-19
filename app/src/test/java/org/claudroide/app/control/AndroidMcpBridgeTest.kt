package org.claudroide.app.control

import kotlinx.coroutines.runBlocking
import org.claudroide.app.feature.control.bridge.FakeAndroidControlBridge
import org.claudroide.app.feature.control.mcp.AndroidMcpBridge
import org.claudroide.app.feature.control.tools.AndroidToolExecutor
import org.claudroide.app.feature.control.mcp.McpToolResult
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AndroidMcpBridgeTest {

    private lateinit var bridge: FakeAndroidControlBridge
    private lateinit var executor: AndroidToolExecutor
    private lateinit var mcpBridge: AndroidMcpBridge

    @Before
    fun setUp() {
        bridge = FakeAndroidControlBridge()
        executor = AndroidToolExecutor(bridge, settleDelayMs = 0L)
        mcpBridge = AndroidMcpBridge(executor)
    }

    @Test
    fun `mcp bridge exposes standard tool definitions`() {
        val names = mcpBridge.toolDefinitions.map { it.name }

        assertTrue(names.contains("android_tap"))
        assertTrue(names.contains("android_observe"))
        assertTrue(names.contains("android_screenshot"))
        assertTrue(names.contains("android_swipe"))
        assertTrue(names.contains("android_scroll"))
        assertTrue(names.contains("android_type"))
        assertTrue(names.contains("android_launch_app"))
        assertTrue(names.contains("android_back"))
    }

    @Test
    fun `executes android_tap through mcp`() = runBlocking {
        val args = JSONObject().apply {
            put("x", 150)
            put("y", 300)
        }

        val result = mcpBridge.executeTool("android_tap", args)

        assertFalse(result.isError)
        assertEquals(1, result.content.size)
        val text = (result.content.first() as McpToolResult.McpContent.Text).text
        assertTrue(text.contains("\"success\": true"))
    }

    @Test
    fun `executes android_observe through mcp and returns elements`() = runBlocking {
        val args = JSONObject()
        val result = mcpBridge.executeTool("android_observe", args)

        assertFalse(result.isError)
        val text = (result.content.first() as McpToolResult.McpContent.Text).text
        assertTrue(text.contains("interactive_elements"))
        assertTrue(text.contains("ClauDroide"))
    }

    @Test
    fun `executing unknown tool returns error`() = runBlocking {
        val result = mcpBridge.executeTool("unknown_tool", JSONObject())

        assertTrue(result.isError)
        val text = (result.content.first() as McpToolResult.McpContent.Text).text
        assertTrue(text.contains("Unknown or unsupported tool"))
    }
}
