package org.claudroide.app.feature.project

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.claudroide.app.R
import org.claudroide.app.core.design.theme_compat.CcTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    projectStorage: ProjectStorage = remember(context) { ProjectStorage.get(context) }
) {
    val state by projectStorage.state.collectAsStateWithLifecycle()
    val colors = CcTheme.tokens.colors

    // Safe Compose ActivityResultLauncher for Android Storage Access Framework
    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            projectStorage.onFolderSelected(uri)
        } else {
            projectStorage.onPickerCancelled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_projects)) },
                actions = {
                    IconButton(onClick = { openTreeLauncher.launch(null) }) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.project_open_folder),
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary
                )
            )
        },
        containerColor = colors.background,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error banner if any
            state.errorMessage?.let { errorMsg ->
                item(key = "error_banner") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surfaceSubtle
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(colors.danger)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = colors.danger
                            )
                            Text(
                                text = errorMsg,
                                style = CcTheme.tokens.typography.bodySmall,
                                color = colors.danger,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { projectStorage.clearError() }) {
                                Text("OK", color = colors.textPrimary)
                            }
                        }
                    }
                }
            }

            // System storage root limitation explanation if applicable
            state.systemLimitationWarning?.let { warning ->
                item(key = "system_root_warning") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surfaceSubtle
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(colors.warning)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = colors.warning,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = warning,
                                style = CcTheme.tokens.typography.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Active Project Card (Immediately visible when "Boom" or any folder is selected)
            if (state.activeProject != null) {
                val active = state.activeProject!!
                item(key = "active_project_card") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surface
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(colors.accent)
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.accent
                                    )
                                    Text(
                                        text = "Aktives Projekt",
                                        style = CcTheme.tokens.typography.titleMedium,
                                        color = colors.textPrimary
                                    )
                                }
                                TextButton(onClick = { openTreeLauncher.launch(null) }) {
                                    Text("Wechseln", color = colors.accent)
                                }
                            }

                            Text(
                                text = active.displayName,
                                style = CcTheme.tokens.typography.bodyLarge,
                                color = colors.textPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (active.readOnly) "Modus: Nur Lesen" else "Modus: Lesen und Schreiben",
                                    style = CcTheme.tokens.typography.bodySmall,
                                    color = if (active.readOnly) colors.warning else colors.success
                                )
                                IconButton(onClick = { projectStorage.revokeFolder(active) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Zugriff widerrufen",
                                        tint = colors.textTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty state when no folder has been selected yet
                item(key = "empty_project_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = colors.accent
                            )
                            Text(
                                text = stringResource(R.string.empty_project_title),
                                style = CcTheme.tokens.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.empty_project_desc),
                                style = CcTheme.tokens.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                            Button(
                                onClick = { openTreeLauncher.launch(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent
                                )
                            ) {
                                Text(stringResource(R.string.project_open_folder))
                            }
                        }
                    }
                }
            }

            // Restore report lines if any
            state.restoreReport?.let { report ->
                if (report.invalid.isNotEmpty() || report.usable.isNotEmpty()) {
                    item(key = "restore_report_card") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = colors.surfaceSubtle
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Status der Ordnerfreigaben",
                                    style = CcTheme.tokens.typography.labelSmall,
                                    color = colors.textPrimary
                                )
                                report.explanationLines().forEach { line ->
                                    Text(
                                        text = line,
                                        style = CcTheme.tokens.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Help section
            item(key = "project_help_card") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceSubtle
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Hilfe & Speicherzugriff",
                            style = CcTheme.tokens.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Wähle einen Projektordner (z. B. in Documents oder Download). ClauDroide sichert den Zugriff dauerhaft über Androids Storage Access Framework. Der Zugriff bleibt nach App-Neustarts erhalten und kann jederzeit widerrufen werden.",
                            style = CcTheme.tokens.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Provider for PersistentFolderAccess instance (retained for backward compatibility).
 */
object PersistentFolderAccessProvider {
    @Volatile
    private var instance: PersistentFolderAccess? = null

    fun initialize(access: PersistentFolderAccess) {
        instance = access
    }

    fun get(): PersistentFolderAccess = instance ?: PersistentFolderAccess()
}
