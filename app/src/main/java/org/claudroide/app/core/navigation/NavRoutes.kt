package org.claudroide.app.core.navigation

/**
 * Type-safe destination routes for ClauDroide navigation shell.
 * Covers ChatList, ProjectExplorer, Search, NewChat, Settings, Onboarding, and ChatDetail.
 */
sealed class Screen(
    val route: String,
    val label: String,
    val isTopLevel: Boolean = false
) {
    data object ChatList : Screen("chat_list", "Chats", isTopLevel = true)

    data object ProjectExplorer : Screen("project_explorer?projectId={projectId}", "Projects", isTopLevel = true) {
        fun createRoute(projectId: String? = null): String {
            return if (projectId != null) {
                "project_explorer?projectId=$projectId"
            } else {
                "project_explorer"
            }
        }
    }

    data object Search : Screen("search?query={query}", "Search", isTopLevel = true) {
        fun createRoute(query: String? = null): String {
            return if (!query.isNullOrBlank()) {
                "search?query=$query"
            } else {
                "search"
            }
        }
    }

    data object NewChat : Screen("new_chat?projectId={projectId}", "New Chat", isTopLevel = false) {
        fun createRoute(projectId: String? = null): String {
            return if (projectId != null) {
                "new_chat?projectId=$projectId"
            } else {
                "new_chat"
            }
        }
    }

    data object Settings : Screen("settings", "Settings", isTopLevel = true)

    data object Onboarding : Screen("onboarding", "Setup", isTopLevel = false)

    data object ChatDetail : Screen("chat_detail/{chatId}?projectId={projectId}", "Conversation", isTopLevel = false) {
        fun createRoute(chatId: String, projectId: String? = null): String {
            return if (projectId != null) {
                "chat_detail/$chatId?projectId=$projectId"
            } else {
                "chat_detail/$chatId"
            }
        }
    }
}

/**
 * Architectural safety policy preventing accidental triggering of destructive operations.
 * Isolates destructive buttons (Delete, Purge, Force Stop) from primary navigation tap targets.
 */
object NavigationSafetyPolicy {
    const val MIN_SAFETY_MARGIN_DP = 16
    const val REQUIRES_TWO_STEP_CONFIRMATION = true

    fun isDestructiveActionAllowedDirectlyAdjacent(actionId: String): Boolean {
        // Destructive actions cannot sit immediately adjacent to navigation/routine taps
        val destructiveActions = setOf("delete_project", "clear_history", "purge_keys", "abort_all")
        return actionId !in destructiveActions
    }
}

/**
 * Reactive state representing current navigation location, back stack trace,
 * and active project binding.
 */
data class NavigationState(
    val currentScreen: Screen = Screen.ChatList,
    val backStack: List<Screen> = listOf(Screen.ChatList),
    val activeProjectId: String? = null,
    val activeProjectName: String? = null
) {
    val isProjectBound: Boolean
        get() = !activeProjectId.isNullOrEmpty()

    val canNavigateBack: Boolean
        get() = backStack.size > 1 && !currentScreen.isTopLevel

    val projectBadgeText: String?
        get() = if (isProjectBound) activeProjectName ?: activeProjectId else null
}
