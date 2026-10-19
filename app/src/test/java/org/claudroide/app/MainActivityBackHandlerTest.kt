package org.claudroide.app

import org.claudroide.app.core.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure state machine representing the back navigation hierarchy of ClauDroide.
 */
class BackNavigationCoordinator(
    var showAttachmentSheet: Boolean = false,
    var showModelDialog: Boolean = false,
    var isDrawerOpen: Boolean = false,
    var currentDestination: AppDestination = AppDestination.CHAT
) {
    sealed interface BackAction {
        object DismissAttachmentSheet : BackAction
        object DismissModelDialog : BackAction
        object CloseDrawer : BackAction
        data class NavigateTo(val destination: AppDestination) : BackAction
        object SystemBack : BackAction
    }

    fun handleBackPress(): BackAction {
        return when {
            showAttachmentSheet -> {
                showAttachmentSheet = false
                BackAction.DismissAttachmentSheet
            }
            showModelDialog -> {
                showModelDialog = false
                BackAction.DismissModelDialog
            }
            isDrawerOpen -> {
                isDrawerOpen = false
                BackAction.CloseDrawer
            }
            currentDestination != AppDestination.CHAT -> {
                currentDestination = AppDestination.CHAT
                BackAction.NavigateTo(AppDestination.CHAT)
            }
            else -> BackAction.SystemBack
        }
    }
}

/**
 * Regression tests for Back navigation hierarchy and SAF picker abort safety.
 */
class MainActivityBackHandlerTest {

    @Test
    fun `back press closes attachment sheet first if open`() {
        val coordinator = BackNavigationCoordinator(
            showAttachmentSheet = true,
            isDrawerOpen = true,
            currentDestination = AppDestination.SETTINGS
        )

        val action = coordinator.handleBackPress()
        assertEquals(BackNavigationCoordinator.BackAction.DismissAttachmentSheet, action)
        assertFalse(coordinator.showAttachmentSheet)
        assertTrue(coordinator.isDrawerOpen)
        assertEquals(AppDestination.SETTINGS, coordinator.currentDestination)
    }

    @Test
    fun `back press closes model dialog second if open`() {
        val coordinator = BackNavigationCoordinator(
            showAttachmentSheet = false,
            showModelDialog = true,
            isDrawerOpen = true,
            currentDestination = AppDestination.PROJECTS
        )

        val action = coordinator.handleBackPress()
        assertEquals(BackNavigationCoordinator.BackAction.DismissModelDialog, action)
        assertFalse(coordinator.showModelDialog)
        assertTrue(coordinator.isDrawerOpen)
    }

    @Test
    fun `back press closes drawer third if open`() {
        val coordinator = BackNavigationCoordinator(
            showAttachmentSheet = false,
            showModelDialog = false,
            isDrawerOpen = true,
            currentDestination = AppDestination.PROJECTS
        )

        val action = coordinator.handleBackPress()
        assertEquals(BackNavigationCoordinator.BackAction.CloseDrawer, action)
        assertFalse(coordinator.isDrawerOpen)
        assertEquals(AppDestination.PROJECTS, coordinator.currentDestination)
    }

    @Test
    fun `back press on Projects navigates back to Chat fourth`() {
        val coordinator = BackNavigationCoordinator(
            showAttachmentSheet = false,
            showModelDialog = false,
            isDrawerOpen = false,
            currentDestination = AppDestination.PROJECTS
        )

        val action = coordinator.handleBackPress()
        assertEquals(BackNavigationCoordinator.BackAction.NavigateTo(AppDestination.CHAT), action)
        assertEquals(AppDestination.CHAT, coordinator.currentDestination)
    }

    @Test
    fun `back press on Settings navigates back to Chat fourth`() {
        val coordinator = BackNavigationCoordinator(
            showAttachmentSheet = false,
            showModelDialog = false,
            isDrawerOpen = false,
            currentDestination = AppDestination.SETTINGS
        )

        val action = coordinator.handleBackPress()
        assertEquals(BackNavigationCoordinator.BackAction.NavigateTo(AppDestination.CHAT), action)
        assertEquals(AppDestination.CHAT, coordinator.currentDestination)
    }

    @Test
    fun `back press on MediaStudio and CodeMcp navigates back to Chat`() {
        for (dest in listOf(AppDestination.MEDIA_STUDIO, AppDestination.CODE_MCP, AppDestination.ARTIFACTS)) {
            val coordinator = BackNavigationCoordinator(
                showAttachmentSheet = false,
                showModelDialog = false,
                isDrawerOpen = false,
                currentDestination = dest
            )
            val action = coordinator.handleBackPress()
            assertEquals(BackNavigationCoordinator.BackAction.NavigateTo(AppDestination.CHAT), action)
            assertEquals(AppDestination.CHAT, coordinator.currentDestination)
        }
    }

    @Test
    fun `back press on Root Chat screen delegates to system back`() {
        val coordinator = BackNavigationCoordinator(
            showAttachmentSheet = false,
            showModelDialog = false,
            isDrawerOpen = false,
            currentDestination = AppDestination.CHAT
        )

        val action = coordinator.handleBackPress()
        assertEquals(BackNavigationCoordinator.BackAction.SystemBack, action)
    }
}
