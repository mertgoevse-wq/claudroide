package org.claudroide.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.claudroide.app.core.design.ClaudroideTheme
import org.claudroide.app.core.diagnostics.StartupDiagnostics
import org.claudroide.app.core.navigation.AppDestination
import org.claudroide.app.core.navigation.NavigationDrawer
import org.claudroide.app.core.navigation.RecentChat
import org.claudroide.app.feature.chat.ChatScreen
import org.claudroide.app.feature.chat.ChatViewModel
import org.claudroide.app.feature.project.ProjectScreen
import org.claudroide.app.feature.project.ProjectSaver
import org.claudroide.app.feature.project.ProjectSaverProvider
import org.claudroide.app.feature.project.PersistentFolderAccess
import org.claudroide.app.feature.project.PersistentFolderAccessProvider
import org.claudroide.app.feature.project.ContentResolverUriAvailability
import org.claudroide.app.feature.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.MAIN_ACTIVITY_CREATE)
        enableEdgeToEdge()

        // Initialize providers
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.DEPENDENCIES_INIT)
        val persistentAccess = PersistentFolderAccess()
        val uriAvailability = ContentResolverUriAvailability(this)
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.PROJECT_STORAGE_INIT)
        ProjectSaverProvider.initialize(this, persistentAccess, uriAvailability)
        PersistentFolderAccessProvider.initialize(persistentAccess)

        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.COMPOSE_INIT)
        setContent {
            ClaudroideTheme {
                AppShell()
            }
        }
    }
}

@Composable
fun AppShell() {
    var currentDestination by remember { mutableStateOf(AppDestination.CHAT) }
    var drawerOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    StartupDiagnostics.markPhase(StartupDiagnostics.Phase.VIEWMODEL_INIT)
    val context = LocalContext.current.applicationContext as Application
    val chatViewModel: ChatViewModel = viewModel(
        factory = remember(context) { ChatViewModel.provideFactory(context) }
    )

    LaunchedEffect(Unit) {
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.FIRST_FRAME)
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.READY)
    }

    // Mock recent chats - in real app, load from ConversationManager
    val recentChats = remember {
        listOf(
            RecentChat("1", "Foreground Service Audio", "Implementing background audio...", System.currentTimeMillis()),
            RecentChat("2", "Inspect Android Architecture", "Jetpack Compose & Coroutines", System.currentTimeMillis() - 86400000),
            RecentChat("3", "Debug Stack Trace Room DB", "Database migration issues", System.currentTimeMillis() - 172800000),
        )
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                item(
                    icon = { Icon(destination.icon, contentDescription = stringResource(destination.labelRes)) },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main content
            when (currentDestination) {
                AppDestination.CHAT -> ChatScreen(
                    viewModel = chatViewModel,
                    onNewChatClick = { chatViewModel.newConversation() },
                )
                AppDestination.PROJECTS -> ProjectScreen()
                AppDestination.SETTINGS -> SettingsScreen()
            }

            // Navigation Drawer overlay
            if (drawerOpen) {
                NavigationDrawer(
                    onDestinationClick = { destination ->
                        currentDestination = destination
                        drawerOpen = false
                    },
                    onNewChatClick = {
                        chatViewModel.newConversation()
                        drawerOpen = false
                    },
                    onClose = { drawerOpen = false },
                    recentChats = recentChats,
                )
            }
        }
    }
}