package org.claudroide.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.claudroide.app.core.design.ClaudroideTheme
import org.claudroide.app.feature.chat.ChatScreen
import org.claudroide.app.feature.project.ProjectScreen
import org.claudroide.app.feature.settings.SettingsScreen

enum class AppDestination(
    val labelRes: Int,
    val icon: ImageVector
) {
    CHAT(R.string.nav_chat, Icons.Default.ChatBubble),
    PROJECTS(R.string.nav_projects, Icons.Default.Folder),
    SETTINGS(R.string.nav_settings, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = stringResource(destination.labelRes)
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        when (currentDestination) {
            AppDestination.CHAT -> ChatScreen()
            AppDestination.PROJECTS -> ProjectScreen()
            AppDestination.SETTINGS -> SettingsScreen()
        }
    }
}
