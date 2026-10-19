package org.claudroide.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.claudroide.app.R
import org.claudroide.app.core.design.theme_compat.CcTheme

/**
 * Navigation destination enumeration for the app.
 */
enum class AppDestination(
    val icon: ImageVector,
    val labelRes: Int,
) {
    CHAT(Icons.Default.ChatBubbleOutline, R.string.nav_chat),
    PROJECTS(Icons.Default.Folder, R.string.nav_projects),
    CODE_MCP(Icons.Default.Code, R.string.nav_drawer_code_mcp),
    MEDIA_STUDIO(Icons.Default.AutoFixHigh, R.string.nav_media_studio),
    ARTIFACTS(Icons.Default.Description, R.string.nav_drawer_artifacts),
    SETTINGS(Icons.Default.Settings, R.string.nav_settings),
}

/**
 * Model representing a recent chat entry.
 */
data class RecentChat(
    val id: String,
    val title: String,
    val preview: String,
    val timestamp: Long,
)

/**
 * Navigation drawer content faithful to the official Claude Android app layout.
 * Reference: Screenshot_20261010_133751_Claude.jpg
 *
 * Structure:
 * - Header: Serif title "ClauDroide"
 * - Core navigation: Chats, Projekte, Code, Artefakte, Settings
 * - Section header: Letzte Chats / Recent chats
 * - Scrollable recent chats list
 * - Bottom bar: User avatar ("MZ") + White pill "+ Neuer Chat" / "+ New Chat" button
 */
@Composable
fun NavigationDrawer(
    onDestinationClick: (AppDestination) -> Unit,
    onNewChatClick: () -> Unit,
    onClose: () -> Unit,
    recentChats: List<RecentChat> = emptyList(),
    userEmail: String = "mert.govse.neu@gmail.com",
    isPro: Boolean = true,
    currentDestination: AppDestination = AppDestination.CHAT,
    onChatSelect: (RecentChat) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCodeMcpClick: () -> Unit = {},
    onArtifactsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = CcTheme.tokens.colors

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // 1. Header: Serif Title "ClauDroide" (or "Claude")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ClauDroide",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
        }

        // 2. Primary Navigation Section
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DrawerNavigationRow(
                icon = Icons.Default.ChatBubbleOutline,
                label = stringResource(R.string.nav_drawer_chats),
                selected = currentDestination == AppDestination.CHAT,
                onClick = {
                    onDestinationClick(AppDestination.CHAT)
                    onClose()
                },
            )
            DrawerNavigationRow(
                icon = Icons.Default.Folder,
                label = stringResource(R.string.nav_drawer_projects),
                selected = currentDestination == AppDestination.PROJECTS,
                onClick = {
                    onDestinationClick(AppDestination.PROJECTS)
                    onClose()
                },
            )
            DrawerNavigationRow(
                icon = Icons.Default.Code,
                label = stringResource(R.string.nav_drawer_code_mcp),
                selected = currentDestination == AppDestination.CODE_MCP,
                onClick = {
                    onDestinationClick(AppDestination.CODE_MCP)
                    onClose()
                },
            )
            DrawerNavigationRow(
                icon = Icons.Default.AutoFixHigh,
                label = stringResource(R.string.nav_media_studio),
                selected = currentDestination == AppDestination.MEDIA_STUDIO,
                onClick = {
                    onDestinationClick(AppDestination.MEDIA_STUDIO)
                    onClose()
                },
            )
            DrawerNavigationRow(
                icon = Icons.Default.Description,
                label = stringResource(R.string.nav_drawer_artifacts),
                selected = currentDestination == AppDestination.ARTIFACTS,
                onClick = {
                    onDestinationClick(AppDestination.ARTIFACTS)
                    onClose()
                },
            )
            DrawerNavigationRow(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.nav_settings),
                selected = currentDestination == AppDestination.SETTINGS,
                onClick = {
                    onDestinationClick(AppDestination.SETTINGS)
                    onClose()
                },
            )
        }

        Spacer(Modifier.height(16.dp))

        // 3. Section Header: Recent chats / Letzte Chats
        Text(
            text = stringResource(R.string.nav_drawer_recent_chats),
            style = CcTheme.tokens.typography.labelSmall,
            color = colors.textTertiary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        )

        // 4. Recent Chats List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (recentChats.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.nav_drawer_no_recent),
                        style = CcTheme.tokens.typography.bodySmall,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(recentChats, key = { it.id }) { chat ->
                    RecentChatRow(
                        title = chat.title,
                        preview = chat.preview,
                        onClick = {
                            onChatSelect(chat)
                            onClose()
                        },
                    )
                }
            }
        }

        // 5. Bottom Bar: User Avatar + Pill "+ New Chat" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // User Avatar ("MZ") - Interactive, opens profile info
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable {
                        onProfileClick()
                        onClose()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "MZ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onAccent,
                )
            }

            // High-contrast "+ New Chat" pill button
            Button(
                onClick = {
                    onNewChatClick()
                    onClose()
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Black,
                    )
                    Text(
                        text = stringResource(R.string.nav_drawer_new_chat),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                    )
                }
            }
        }
    }
}

/**
 * Drawer item row with icon and title.
 */
@Composable
private fun DrawerNavigationRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CcTheme.tokens.colors
    val background = if (selected) colors.surfaceSubtle else Color.Transparent
    val textColor = if (selected) colors.textPrimary else colors.textSecondary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = background,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = CcTheme.tokens.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = textColor,
            )
        }
    }
}

/**
 * Recent chat item with leading chat bubble icon.
 */
@Composable
private fun RecentChatRow(
    title: String,
    preview: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CcTheme.tokens.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CcTheme.tokens.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    style = CcTheme.tokens.typography.bodySmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}
