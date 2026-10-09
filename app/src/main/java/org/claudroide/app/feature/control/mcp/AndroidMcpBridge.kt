package org.claudroide.app.feature.control.mcp

import org.claudroide.app.feature.control.model.ScrollDirection
import org.claudroide.app.feature.control.tools.AndroidToolAction
import org.claudroide.app.feature.control.tools.AndroidToolExecutor
import org.json.JSONArray
import org.json.JSONObject

/**
 * Standard MCP Tool definition for AI model tool calling and external integrations.
 */
data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JSONObject
)

/**
 * Result of executing an MCP tool.
 */
data class McpToolResult(
    val content: List<McpContent>,
    val isError: Boolean = false
) {
    sealed class McpContent {
        data class Text(val text: String) : McpContent()
        data class Image(val data: String, val mimeType: String = "image/jpeg") : McpContent()
    }
}

/**
 * Bridge exposing Android device control, file operations, terminal, and media capabilities
 * through standard Model Context Protocol (MCP) tool interfaces.
 */
class AndroidMcpBridge(
    private val toolExecutor: AndroidToolExecutor
) {

    val toolDefinitions: List<McpToolDefinition> = listOf(
        McpToolDefinition(
            name = "android_observe",
            description = "Inspect visible Android UI hierarchy, active application, bounds, and interactive elements.",
            inputSchema = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("include_screenshot", JSONObject().apply {
                        put("type", "boolean")
                        put("description", "Whether to capture an accompanying screenshot with the accessibility tree.")
                    })
                })
            }
        ),
        McpToolDefinition(
            name = "android_screenshot",
            description = "Capture full-screen device screenshot as base64 JPEG.",
            inputSchema = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject())
            }
        ),
        McpToolDefinition(
            name = "android_tap",
            description = "Tap on screen coordinates (x, y).",
            inputSchema = JSONObject().apply {
                put("type", "object")
                put("required", JSONArray(listOf("x", "y")))
                put("properties", JSONObject().apply {
                    put("x", JSONObject().put("type", "number").put("description", "X coordinate in screen pixels."))
                    put("y", JSONObject().put("type", "number").put("description", "Y coordinate in screen pixels."))
                    put("element_id", JSONObject().put("type", "string").put("description", "Optional target element ID for verification."))
                })
            }
        ),
        McpToolDefinition(
            name = "android_swipe",
            description = "Swipe from (from_x, from_y) to (to_x, to_y).",
            inputSchema = JSONObject().apply {
                put("type", "object")
                put("required", JSONArray(listOf("from_x", "from_y", "to_x", "to_y")))
                put("properties", JSONObject().apply {
                    put("from_x", JSONObject().put("type", "number"))
                    put("from_y", JSONObject().put("type", "number"))
                    put("to_x", JSONObject().put("type", "number"))
                    put("to_y", JSONObject().put("type", "number"))
                    put("duration_ms", JSONObject().put("type", "integer").put("default", 300))
                })
            }
        ),
        McpToolDefinition(
            name = "android_scroll",
            description = "Scroll screen in direction: UP, DOWN, LEFT, RIGHT.",
            inputSchema = JSONObject().apply {
                put("type", "object")
                put("required", JSONArray(listOf("direction")))
                put("properties", JSONObject().apply {
                    put("direction", JSONObject().put("type", "string").put("enum", JSONArray(listOf("UP", "DOWN", "LEFT", "RIGHT"))))
                })
            }
        ),
        McpToolDefinition(
            name = "android_type",
            description = "Type text into currently focused input or specified target element.",
            inputSchema = JSONObject().apply {
                put("type", "object")
                put("required", JSONArray(listOf("text")))
                put("properties", JSONObject().apply {
                    put("text", JSONObject().put("type", "string").put("description", "Text string to input."))
                    put("target_element_id", JSONObject().put("type", "string").put("description", "Optional ID of element."))
                })
            }
        ),
        McpToolDefinition(
            name = "android_back",
            description = "Press system Back button.",
            inputSchema = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject())
            }
        ),
        McpToolDefinition(
            name = "android_home",
            description = "Press system Home button.",
            inputSchema = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject())
            }
        ),
        McpToolDefinition(
            name = "android_launch_app",
            description = "Launch an application by its package name or name (e.g. Cubasis, FL Studio Mobile).",
            inputSchema = JSONObject().apply {
                put("type", "object")
                put("required", JSONArray(listOf("package_name")))
                put("properties", JSONObject().apply {
                    put("package_name", JSONObject().put("type", "string").put("description", "Android application package name or name."))
                })
            }
        ),
        McpToolDefinition(
            name = "android_wait",
            description = "Wait for UI transition or animation.",
            inputSchema = JSONObject().apply {
                put("type", "object")
                put("required", JSONArray(listOf("duration_ms")))
                put("properties", JSONObject().apply {
                    put("duration_ms", JSONObject().put("type", "integer").put("description", "Milliseconds to wait."))
                })
            }
        )
    )

    suspend fun executeTool(name: String, arguments: JSONObject): McpToolResult {
        return try {
            val action = parseAction(name, arguments)
            if (action == null) {
                return McpToolResult(
                    content = listOf(McpToolResult.McpContent.Text("Unknown or unsupported tool '$name'.")),
                    isError = true
                )
            }

            val execution = toolExecutor.execute(action)
            val verification = execution.verification

            val contentList = mutableListOf<McpToolResult.McpContent>()

            val resultJson = JSONObject().apply {
                put("success", verification.success)
                put("observation", verification.observation)
                put("details", verification.verificationDetails)
                if (verification.suggestedRecovery != null) {
                    put("suggested_recovery", verification.suggestedRecovery.name)
                }

                val snapshot = execution.latestSnapshot
                if (snapshot != null) {
                    put("package", snapshot.packageName)
                    put("element_count", snapshot.elements.size)
                    val textArray = JSONArray()
                    snapshot.visibleText.forEach { textArray.put(it) }
                    put("visible_text", textArray)

                    val elementsArray = JSONArray()
                    snapshot.interactiveElements.take(40).forEach { elem ->
                        elementsArray.put(JSONObject().apply {
                            put("id", elem.id)
                            put("label", elem.readableLabel)
                            put("bounds", "[${elem.bounds.left},${elem.bounds.top},${elem.bounds.right},${elem.bounds.bottom}]")
                            put("clickable", elem.isClickable)
                            put("editable", elem.isEditable)
                        })
                    }
                    put("interactive_elements", elementsArray)
                }
            }

            contentList.add(McpToolResult.McpContent.Text(resultJson.toString(2)))

            val screenshotData = execution.screenshotBase64
            if (screenshotData != null) {
                contentList.add(McpToolResult.McpContent.Image(data = screenshotData, mimeType = "image/jpeg"))
            }

            McpToolResult(content = contentList, isError = !verification.success)
        } catch (e: Exception) {
            McpToolResult(
                content = listOf(McpToolResult.McpContent.Text("Tool execution error: ${e.message}")),
                isError = true
            )
        }
    }

    private fun parseAction(name: String, args: JSONObject): AndroidToolAction? {
        return when (name) {
            "android_observe" -> {
                val ss = args.optBoolean("include_screenshot", false)
                AndroidToolAction.Observe(includeScreenshot = ss)
            }
            "android_screenshot" -> AndroidToolAction.Screenshot
            "android_tap" -> {
                val x = args.getDouble("x").toFloat()
                val y = args.getDouble("y").toFloat()
                val elemId = args.optString("element_id").takeIf { it.isNotBlank() }
                AndroidToolAction.Tap(x, y, elemId)
            }
            "android_swipe" -> {
                val fx = args.getDouble("from_x").toFloat()
                val fy = args.getDouble("from_y").toFloat()
                val tx = args.getDouble("to_x").toFloat()
                val ty = args.getDouble("to_y").toFloat()
                val d = args.optLong("duration_ms", 300L)
                AndroidToolAction.Swipe(fx, fy, tx, ty, d)
            }
            "android_scroll" -> {
                val dirStr = args.getString("direction").uppercase()
                val dir = try { ScrollDirection.valueOf(dirStr) } catch (_: Exception) { ScrollDirection.DOWN }
                AndroidToolAction.Scroll(dir)
            }
            "android_type" -> {
                val text = args.getString("text")
                val targetId = args.optString("target_element_id").takeIf { it.isNotBlank() }
                AndroidToolAction.Type(text, targetId)
            }
            "android_back" -> AndroidToolAction.Back
            "android_home" -> AndroidToolAction.Home
            "android_launch_app" -> {
                val pkg = args.getString("package_name")
                AndroidToolAction.LaunchApp(pkg)
            }
            "android_wait" -> {
                val ms = args.getLong("duration_ms")
                AndroidToolAction.Wait(ms)
            }
            else -> null
        }
    }
}
