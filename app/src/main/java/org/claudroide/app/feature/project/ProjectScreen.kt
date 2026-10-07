package org.claudroide.app.feature.project

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.claudroide.app.R
import org.claudroide.app.core.design.ClaudroideTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    projectSaver: ProjectSaver = remember { 
        ProjectSaverProvider.get() 
    },
    persistentAccess: PersistentFolderAccess = remember { 
        PersistentFolderAccessProvider.get() 
    }
) {
    var restoreReport by remember { mutableStateOf<RestoreReport?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    // Restore on first composition
    LaunchedEffect(Unit) {
        if (restoreReport == null && !isRestoring) {
            isRestoring = true
            restoreReport = projectSaver.restorePersistedPermissions()
            isRestoring = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_projects)) },
                actions = {
                    if (persistentAccess.hasGrants) {
                        var showGrants by remember { mutableStateOf(false) }
                        IconButton(onClick = { showGrants = true }) {
                            Icon(Icons.Default.Folder, contentDescription = "Current project")
                        }
                        if (showGrants) {
                            AlertDialog(
                                onDismissRequest = { showGrants = false },
                                title = { Text("Offene Projektordner") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        persistentAccess.grants.forEach { grant ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = grant.displayName, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    text = if (grant.readOnly) "Nur Lesen" else "Lesen/Schreiben",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showGrants = false }) { Text("Schliessen") }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current project status
            if (persistentAccess.hasGrants) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Aktuelles Projekt",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = {
                                // Show folder list dialog
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                        persistentAccess.grants.forEach { grant ->
                            Text(
                                text = "${grant.displayName} (${if (grant.readOnly) "Nur Lesen" else "Lesen/Schreiben"})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Empty state with open folder button
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.empty_project_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.empty_project_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { projectSaver.openFolderPicker() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(stringResource(R.string.project_open_folder))
                        }
                    }
                }
            }

            // Restore report if available
            restoreReport?.let { report ->
                if (report.invalid.isNotEmpty() || report.usable.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                text = "Wiederherstellungsbericht",
                                style = MaterialTheme.typography.titleSmall
                            )
                            report.explanationLines().forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Help text
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Hilfe",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Wähle einen Ordner, um ein Projekt zu öffnen. Der Zugriff bleibt nach einem Neustart der App erhalten. Du kannst den Zugriff jederzeit in den Einstellungen widerrufen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Provider for ProjectSaver instance - will be replaced with proper DI.
 */
object ProjectSaverProvider {
    @Volatile
    private var instance: ProjectSaver? = null

    fun initialize(context: Context, persistentAccess: PersistentFolderAccess, uriAvailability: UriAvailability) {
        instance = ProjectSaver(context, persistentAccess, uriAvailability)
    }

    fun get(): ProjectSaver = instance ?: error("ProjectSaver not initialized. Call initialize() first.")
}

/**
 * Provider for PersistentFolderAccess instance.
 */
object PersistentFolderAccessProvider {
    @Volatile
    private var instance: PersistentFolderAccess? = null

    fun initialize(access: PersistentFolderAccess) {
        instance = access
    }

    fun get(): PersistentFolderAccess = instance ?: PersistentFolderAccess()
}
