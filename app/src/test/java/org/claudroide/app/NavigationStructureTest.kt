package org.claudroide.app

import org.claudroide.app.core.navigation.NavigationSafetyPolicy
import org.claudroide.app.core.navigation.NavigationState
import org.claudroide.app.core.navigation.Screen
import org.junit.Assert.*
import org.junit.Test

class NavigationStructureTest {

    @Test
    fun screenRoutes_areValidAndExplicit() {
        assertEquals("chat_list", Screen.ChatList.route)
        assertEquals("settings", Screen.Settings.route)
        assertEquals("onboarding", Screen.Onboarding.route)
        assertEquals("search", Screen.Search.createRoute())
        assertEquals("search?query=android", Screen.Search.createRoute("android"))
        assertEquals("new_chat", Screen.NewChat.createRoute())
        assertEquals("new_chat?projectId=proj-1", Screen.NewChat.createRoute("proj-1"))
    }

    @Test
    fun topLevelDestinations_areIdentifiedCorrectly() {
        assertTrue(Screen.ChatList.isTopLevel)
        assertTrue(Screen.ProjectExplorer.isTopLevel)
        assertTrue(Screen.Search.isTopLevel)
        assertTrue(Screen.Settings.isTopLevel)
        assertFalse(Screen.ChatDetail.isTopLevel)
        assertFalse(Screen.NewChat.isTopLevel)
        assertFalse(Screen.Onboarding.isTopLevel)
    }

    @Test
    fun chatDetailRoute_withAndWithoutProject_generatesCorrectPath() {
        val routeNoProject = Screen.ChatDetail.createRoute("chat-123")
        assertEquals("chat_detail/chat-123", routeNoProject)

        val routeWithProject = Screen.ChatDetail.createRoute("chat-123", "proj-456")
        assertEquals("chat_detail/chat-123?projectId=proj-456", routeWithProject)
    }

    @Test
    fun navigationState_correctlyReflectsProjectBindingAndBadge() {
        val unboundState = NavigationState(currentScreen = Screen.ChatList)
        assertFalse(unboundState.isProjectBound)
        assertNull(unboundState.projectBadgeText)

        val boundState = NavigationState(
            currentScreen = Screen.ChatDetail,
            activeProjectId = "proj-1",
            activeProjectName = "ClauDroide Core"
        )
        assertTrue(boundState.isProjectBound)
        assertEquals("ClauDroide Core", boundState.projectBadgeText)
    }

    @Test
    fun backNavigation_isPermittedOnlyWhenBackStackExistsAndNotTopLevel() {
        val rootState = NavigationState(
            currentScreen = Screen.ChatList,
            backStack = listOf(Screen.ChatList)
        )
        assertFalse(rootState.canNavigateBack)

        val deepState = NavigationState(
            currentScreen = Screen.ChatDetail,
            backStack = listOf(Screen.ChatList, Screen.ChatDetail)
        )
        assertTrue(deepState.canNavigateBack)
    }

    @Test
    fun navigationSafetyPolicy_strictlyIsolatesDestructiveActions() {
        assertTrue(NavigationSafetyPolicy.REQUIRES_TWO_STEP_CONFIRMATION)
        assertTrue(NavigationSafetyPolicy.MIN_SAFETY_MARGIN_DP >= 16)

        assertFalse(NavigationSafetyPolicy.isDestructiveActionAllowedDirectlyAdjacent("delete_project"))
        assertFalse(NavigationSafetyPolicy.isDestructiveActionAllowedDirectlyAdjacent("clear_history"))
        assertFalse(NavigationSafetyPolicy.isDestructiveActionAllowedDirectlyAdjacent("purge_keys"))
        assertFalse(NavigationSafetyPolicy.isDestructiveActionAllowedDirectlyAdjacent("abort_all"))
        assertTrue(NavigationSafetyPolicy.isDestructiveActionAllowedDirectlyAdjacent("open_settings"))
    }
}
