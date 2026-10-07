package org.claudroide.app

import android.app.Application
import org.claudroide.app.core.diagnostics.StartupDiagnostics

class ClaudroideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        StartupDiagnostics.install(this)
        StartupDiagnostics.markPhase(StartupDiagnostics.Phase.APPLICATION_INIT)
        // Zentrale Initialisierung von Sicherheitsdiensten und Keystore
    }
}
