package org.claudroide.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.claudroide.app.R
import org.claudroide.app.core.design.ClaudroideTheme
import org.claudroide.app.core.design.components.BrandMark
import org.claudroide.app.core.design.components.ListRow
import org.claudroide.app.core.design.components.SectionHeader
import org.claudroide.app.core.design.components.StatusChip
import org.claudroide.app.core.design.TypeTokens

/**
 * Navigation destination enumeration for the app.
 */
enum class AppDestination(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
) {
    CHAT(Icons.Default.History, R.string.nav_chat),
    PROJECTS(Icons.Default.Folder, R.string.nav_projects),
    SETTINGS(Icons.Default.Settings, R.string.nav_settings),
}

/**
 * Navigation drawer content following the Stitch design specification.
 *
 * Structure per Stitch:
 * - Header: Brand mark + "ClauDroide" + user info
 * - Primary action: "Neuer Chat" pill button
 * - Core navigation: Chats (active), Projekte, Code & MCP, Artefakte
 * - Section: Letzte Chats (recent conversations)
 * - Footer: Settings, Documentation, User profile with PRO badge
 */
@Composable
fun NavigationDrawer(
    onDestinationClick: (AppDestination) -> Unit,
    onNewChatClick: () -> Unit,
    onClose: () -> Unit,
    recentChats: List<RecentChat> = emptyList(),
    userEmail: String = "mert.govse.neu@gmail.com",
    isPro: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .width(304.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Header with brand mark and user info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BrandMark(
                        modifier = Modifier.size(36.dp),
                        contentDescription = stringResource(R.string.brand_mark_alt_text),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "ClauDroide",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.nav_drawer_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.padding(end = -8.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.nav_drawer_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Primary action: Neuer Chat
            Button(
                onClick = onNewChatClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.nav_drawer_new_chat),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // Core navigation
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NavigationDrawerItem(
                icon = Icons.Default.History,
                label = stringResource(R.string.nav_drawer_chats),
                badge = "5",
                selected = true,
                onClick = {
                    onDestinationClick(AppDestination.CHAT)
                    onClose()
                },
            )
            NavigationDrawerItem(
                icon = Icons.Default.Folder,
                label = stringResource(R.string.nav_drawer_projects),
                badge = "4",
                onClick = {
                    onDestinationClick(AppDestination.PROJECTS)
                    onClose()
                },
            )
            NavigationDrawerItem(
                icon = Icons.Default.Terminal,
                label = stringResource(R.string.nav_drawer_code_mcp),
                badgeText = "Host",
                onClick = {
                    // Navigate to code/MCP screen
                    onClose()
                },
            )
            NavigationDrawerItem(
                icon = Icons.Default.Description,
                label = stringResource(R.string.nav_drawer_artifacts),
                onClick = {
                    // Navigate to artifacts screen
                    onClose()
                },
            )
        }

        Divider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // Recent chats section
        SectionHeader(
            title = stringResource(R.string.nav_drawer_recent_chats),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            recentChats.forEach { chat ->
                NavigationDrawerItem(
                    icon = Icons.Default.History,
                    label = chat.title,
                    subtitle = chat.preview,
                    onClick = {
                        // Navigate to specific chat
                        onClose()
                    },
                )
            }
            if (recentChats.isEmpty()) {
                Text(
                    text = stringResource(R.string.nav_drawer_no_recent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Footer
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NavigationDrawerItem(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.nav_settings),
                onClick = {
                    onDestinationClick(AppDestination.SETTINGS)
                    onClose()
                },
            )
            NavigationDrawerItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = stringResource(R.string.nav_drawer_docs),
                onClick = {
                    // Open documentation
                    onClose()
                },
            )
        }

        // User profile card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // User avatar
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "MZ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = userEmail,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        StatusChip(
                            label = stringResource(R.string.nav_drawer_subscribed),
                            tint = MaterialTheme.colorScheme.tertiaryContainer,
                        )
                        Text(
                            text = "v3.7",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isPro) {
                    Text(
                        text = "PRO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                RoundedCornerShape(12.dp),
                            ),
                    )
                }
            }
        }
    }
}

/**
 * Individual navigation drawer item.
 */
@Composable
fun NavigationDrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String? = null,
    badge: String? = null,
    badgeText: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
    else Color.Transparent

    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .then(if (onClick != null) Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = contentColor,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier.size(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            }
            badge?.let {
                Text(
                    text = it,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                )
            }
            badgeText?.let {
                Text(
                    text = it,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.04.em,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(10.dp)),
                )
            }
        }
    }
}

data class RecentChat(
    val id: String,
    val title: String,
    val preview: String,
    val timestamp: Long,
)