package org.claudroide.app.feature.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.claudroide.app.feature.chat.ChatUiState
import org.claudroide.app.feature.control.bridge.AccessibilityBridgeRegistry
import org.claudroide.app.feature.control.mcp.AndroidMcpBridge
import org.claudroide.app.feature.control.tools.AndroidToolExecutor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.claudroide.app.core.design.theme_compat.CcTheme

/**
 * Interactive Claude Code & MCP Workspace Screen.
 * Provides graphical management of MCP tool connectors and request telemetry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeMcpScreen(
    onBackClick: () -> Unit = {},
    chatState: ChatUiState = ChatUiState(),
    modifier: Modifier = Modifier,
) {
    val colors = CcTheme.tokens.colors
    var accessibilityReady by remember { mutableStateOf(AccessibilityBridgeRegistry.isReady) }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Code & MCP Studio",
                            style = CcTheme.tokens.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Modell: ${chatState.currentModelId}",
                            style = CcTheme.tokens.typography.bodySmall,
                            color = colors.accent,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { accessibilityReady = AccessibilityBridgeRegistry.isReady }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = colors.textSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Tab Selector: 0: MCP Tool Connections, 1: Telemetry
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.surfaceSubtle,
                contentColor = colors.accent,
                divider = { HorizontalDivider(color = colors.border) },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("MCP Werkzeuge", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Telemetrie", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> McpToolsView(accessibilityReady)
                1 -> TelemetryMonitorView(chatState)
            }
        }
    }
}

@Composable
private fun McpToolsView(accessibilityReady: Boolean) {
    val colors = CcTheme.tokens.colors

    val tools = remember(accessibilityReady) {
        val controlBridge = AccessibilityBridgeRegistry.getBridge()
        AndroidMcpBridge(AndroidToolExecutor(controlBridge)).toolDefinitions
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Lokaler Android-Werkzeugkatalog",
                style = CcTheme.tokens.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
            )
        }

        item {
            Text(
                text = if (accessibilityReady) {
                    "Bedienungshilfe aktiv. Lokale Werkzeugdefinitionen sind vorhanden; der Chat-Provider ruft sie noch nicht auf."
                } else {
                    "Bedienungshilfe nicht verbunden — Android-Aktionen sind derzeit nicht ausführbar."
                },
                style = CcTheme.tokens.typography.bodySmall,
                color = if (accessibilityReady) colors.success else colors.textSecondary,
            )
        }

        items(tools, key = { it.name }) { tool ->
            Surface(
                color = colors.surfaceSubtle,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (accessibilityReady) colors.success else colors.textTertiary)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tool.name,
                            style = CcTheme.tokens.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = tool.description,
                            style = CcTheme.tokens.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Local tool definition; not connected to the chat provider",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryMonitorView(chatState: ChatUiState) {
    val colors = CcTheme.tokens.colors
    val usage = chatState.lastUsage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = colors.surfaceSubtle,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Letzte gemeldete Anfrage",
                    style = CcTheme.tokens.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                if (usage == null) {
                    Text(
                        text = "Noch keine Token-Nutzungsdaten vom Anbieter gemeldet.",
                        style = CcTheme.tokens.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                } else {
                    Text(
                        text = "Eingabe: ${usage.inputTokens} · Ausgabe: ${usage.outputTokens} · Gesamt: ${usage.total} Tokens",
                        style = CcTheme.tokens.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                Text(
                    text = "Modell: ${chatState.currentModelId}",
                    style = CcTheme.tokens.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }

        Surface(
            color = colors.surfaceSubtle,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Aktueller Anfragestatus",
                    style = CcTheme.tokens.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text("Provider: ${chatState.currentProviderId}", style = CcTheme.tokens.typography.bodySmall, color = colors.textSecondary)
                Text("Stream: ${chatState.streamState}", style = CcTheme.tokens.typography.bodySmall, color = colors.textSecondary)
                Text("Run aktiv: ${if (chatState.isStreaming) "ja" else "nein"}", style = CcTheme.tokens.typography.bodySmall, color = colors.textSecondary)
                Text(
                    "Externe MCP-Server: nicht verbunden (keine Verbindung wird vorgetäuscht)",
                    style = CcTheme.tokens.typography.bodySmall,
                    color = colors.textSecondary,
                )

            }
        }
    }
}
