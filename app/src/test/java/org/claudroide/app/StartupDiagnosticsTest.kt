package org.claudroide.app

import android.app.Application
import org.claudroide.app.core.diagnostics.StartupDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupDiagnosticsTest {

    @Test
    fun testPhaseRecordingSequence() {
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.START, "Init test")
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.APPLICATION_INIT)
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.MAIN_ACTIVITY_CREATE)
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.VIEWMODEL_INIT)
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.FIRST_FRAME)
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.READY)

        val phases = StartupDiagnostics.getRecordedPhases()
        assertTrue("Recorded phases should not be empty", phases.isNotEmpty())
        val phaseNames = phases.map { it.phase }
        assertTrue(phaseNames.contains(StartupDiagnostics.Phase.START))
        assertTrue(phaseNames.contains(StartupDiagnostics.Phase.APPLICATION_INIT))
        assertTrue(phaseNames.contains(StartupDiagnostics.Phase.MAIN_ACTIVITY_CREATE))
        assertTrue(phaseNames.contains(StartupDiagnostics.Phase.VIEWMODEL_INIT))
        assertTrue(phaseNames.contains(StartupDiagnostics.Phase.FIRST_FRAME))
        assertTrue(phaseNames.contains(StartupDiagnostics.Phase.READY))
    }
}
