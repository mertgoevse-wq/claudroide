package org.claudroide.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.claudroide.app.core.design.ClaudroideTheme
import org.claudroide.app.core.design.ThemeMode
import org.claudroide.app.core.design.ThemeModeResolver
import org.claudroide.app.core.design.theme_compat.CcTheme
import org.claudroide.app.core.diagnostics.StartupDiagnostics
import org.claudroide.app.core.navigation.AppDestination
import org.claudroide.app.core.navigation.NavigationDrawer
import org.claudroide.app.core.navigation.RecentChat
import org.claudroide.app.feature.chat.ChatScreen
import org.claudroide.app.feature.chat.ChatViewModel
import org.claudroide.app.feature.project.ProjectScreen
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.claudroide.app.feature.chat.ConversationManager
import org.claudroide.app.feature.project.ProjectStorage
import org.claudroide.app.feature.project.PersistentFolderAccess
import org.claudroide.app.feature.project.PersistentFolderAccessProvider
import org.claudroide.app.feature.project.ContentResolverUriAvailability
import org.claudroide.app.feature.settings.SettingsScreen
import org.claudroide.app.feature.agent.CodeMcpScreen
import org.claudroide.app.feature.media.MediaStudioScreen
import org.claudroide.app.feature.artifacts.ArtifactsScreen

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
        ProjectStorage.get(this)
        PersistentFolderAccessProvider.initialize(persistentAccess)

        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.COMPOSE_INIT)
        setContent {
            val isDark = isSystemInDarkTheme()
            CcTheme(darkTheme = isDark) {
                AppShell()
            }
        }
    }
}

@Composable
fun AppShell() {
    var currentDestination by remember { mutableStateOf(AppDestination.CHAT) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    StartupDiagnostics.markPhase(StartupDiagnostics.Phase.VIEWMODEL_INIT)
    val context = LocalContext.current.applicationContext as Application
    val chatViewModel: ChatViewModel = viewModel(
        factory = remember(context) { ChatViewModel.provideFactory(context) }
    )
    val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()

    // ── Hierarchical Back Navigation Handler ──
    // 1. If attachment bottom sheet is open, close it first
    BackHandler(enabled = chatUiState.showAttachmentSheet) {
        chatViewModel.onDismissAttachmentSheet()
    }

    // 2. If model selection dialog is open, close it
    BackHandler(enabled = chatUiState.showModelDialog) {
        chatViewModel.onDismissModelDialog()
    }

    // 3. If navigation drawer is open, close it
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // 4. If on a sub-destination (PROJECTS or SETTINGS), return to CHAT
    BackHandler(
        enabled = currentDestination != AppDestination.CHAT &&
                !drawerState.isOpen &&
                !chatUiState.showAttachmentSheet &&
                !chatUiState.showModelDialog
    ) {
        currentDestination = AppDestination.CHAT
    }

    LaunchedEffect(Unit) {
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.FIRST_FRAME)
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.READY)
    }

    // Dynamic recent chats wired to active session
    val recentChats = remember(chatUiState.recentSessions, chatUiState.conversationId, chatUiState.messages.size) {
        val currentSession = chatUiState.messages.takeIf { it.isNotEmpty() }?.let { messages ->
            RecentChat(
                id = chatUiState.conversationId,
                title = messages.firstOrNull()?.text
                    ?.let(ConversationManager::autoGenerateTitle)
                    ?.let(org.claudroide.app.feature.provider.SecretMasker::redact)
                    ?: "Neue Unterhaltung",
                preview = org.claudroide.app.feature.provider.SecretMasker.redact(
                    messages.lastOrNull()?.text.orEmpty()
                ),
                timestamp = chatUiState.recentSessions
                    .firstOrNull { it.conversationId == chatUiState.conversationId }
                    ?.timestamp ?: System.currentTimeMillis(),
            )
        }
        val persistedSessions = chatUiState.recentSessions
            .filterNot { currentSession?.id == it.conversationId }
            .map { session ->
                RecentChat(
                    id = session.conversationId,
                    title = session.title,
                    preview = session.preview,
                    timestamp = session.timestamp,
                )
            }
        listOfNotNull(currentSession) + persistedSessions
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CcTheme.tokens.colors.background,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
            ) {
                NavigationDrawer(
                    onDestinationClick = { destination ->
                        currentDestination = destination
                        scope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        chatViewModel.newConversation()
                        currentDestination = AppDestination.CHAT
                        scope.launch { drawerState.close() }
                    },
                    onClose = {
                        scope.launch { drawerState.close() }
                    },
                    onChatSelect = { chat ->
                        chatViewModel.selectConversation(chat.id, chat.title)
                        currentDestination = AppDestination.CHAT
                        scope.launch { drawerState.close() }
                    },
                    onProfileClick = {
                        chatViewModel.onProfileClick()
                        scope.launch { drawerState.close() }
                    },
                    recentChats = recentChats,
                    currentDestination = currentDestination,
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (currentDestination) {
                AppDestination.CHAT -> ChatScreen(
                    viewModel = chatViewModel,
                    onNewChatClick = { chatViewModel.newConversation() },
                    onNavigationClick = { scope.launch { drawerState.open() } },
                )
                AppDestination.PROJECTS -> ProjectScreen()
                AppDestination.CODE_MCP -> CodeMcpScreen(
                    onBackClick = { currentDestination = AppDestination.CHAT },
                    chatState = chatUiState,
                )
                AppDestination.MEDIA_STUDIO -> MediaStudioScreen(
                    onBackClick = { currentDestination = AppDestination.CHAT }
                )
                AppDestination.ARTIFACTS -> ArtifactsScreen(
                    onBackClick = { currentDestination = AppDestination.CHAT },
                    onNavigateToMediaStudio = { currentDestination = AppDestination.MEDIA_STUDIO }
                )
                AppDestination.SETTINGS -> SettingsScreen()
            }
        }
    }
}