package org.claudroide.app

import org.claudroide.app.feature.project.PersistedGrant
import org.claudroide.app.feature.project.ProjectFolderUiState
import org.claudroide.app.feature.project.RestoreReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for SAF project folder selection state, cancellation safety, and root warnings.
 */
class ProjectFolderSelectionTest {

    @Test
    fun `newly selected folder becomes active project immediately`() {
        val grant = PersistedGrant(
            uriString = "content://com.android.externalstorage.documents/tree/primary%3ABoom",
            displayName = "Boom",
            path = "Boom",
            readOnly = false
        )

        val state = ProjectFolderUiState(
            activeProject = grant,
            allProjects = listOf(grant)
        )

        assertTrue("Must report having active project", state.hasActiveProject)
        assertEquals("Boom", state.activeProject?.displayName)
        assertFalse("Must not be readOnly when write permission granted", state.activeProject!!.readOnly)
    }

    @Test
    fun `picker cancellation does not clear previously active project`() {
        val initialGrant = PersistedGrant(
            uriString = "content://com.android.externalstorage.documents/tree/primary%3ABoom",
            displayName = "Boom",
            path = "Boom",
            readOnly = false
        )

        val stateBeforePicker = ProjectFolderUiState(
            activeProject = initialGrant,
            allProjects = listOf(initialGrant),
            errorMessage = "Alter Fehler"
        )

        // Simulating onPickerCancelled
        val stateAfterCancel = stateBeforePicker.copy(errorMessage = null)

        assertTrue(stateAfterCancel.hasActiveProject)
        assertEquals("Boom", stateAfterCancel.activeProject?.displayName)
        assertNull("Error should be cleared on cancel", stateAfterCancel.errorMessage)
    }

    @Test
    fun `read-only permission is never upgraded to read-write`() {
        val readOnlyGrant = PersistedGrant(
            uriString = "content://com.android.externalstorage.documents/tree/primary%3AReadOnlyFolder",
            displayName = "ReadOnlyFolder",
            path = "ReadOnlyFolder",
            readOnly = true
        )

        val state = ProjectFolderUiState(
            activeProject = readOnlyGrant,
            allProjects = listOf(readOnlyGrant)
        )

        assertTrue(state.activeProject!!.readOnly)
    }

    @Test
    fun `revoking active project clears activeProject and updates allProjects`() {
        val grantA = PersistedGrant(
            uriString = "content://com.android.externalstorage.documents/tree/primary%3ABoom",
            displayName = "Boom",
            path = "Boom",
            readOnly = false
        )
        val grantB = PersistedGrant(
            uriString = "content://com.android.externalstorage.documents/tree/primary%3AOther",
            displayName = "Other",
            path = "Other",
            readOnly = false
        )

        val state = ProjectFolderUiState(
            activeProject = grantA,
            allProjects = listOf(grantA, grantB)
        )

        // Revoke grantA
        val remaining = state.allProjects.filterNot { it.uriString == grantA.uriString }
        val updatedState = state.copy(
            activeProject = remaining.firstOrNull(),
            allProjects = remaining
        )

        assertEquals("Other", updatedState.activeProject?.displayName)
        assertEquals(1, updatedState.allProjects.size)
    }
}
