package org.claudroide.app.feature.control.music

import org.claudroide.app.feature.control.tools.AndroidToolAction
import java.io.File

/**
 * Metadata and integration capabilities for mobile digital audio workstations (DAWs).
 */
interface DawAdapter {
    val dawName: String
    val packageName: String
    val defaultProjectDirectory: String
    val defaultMidiDirectory: String

    /** Plan an agentic workflow to create a project with specified parameters. */
    fun planProjectCreation(
        projectName: String,
        bpm: Int,
        midiFilePaths: List<String> = emptyList()
    ): List<AndroidToolAction>
}

/**
 * Adapter for Steinberg Cubasis 3 on Android.
 */
class CubasisAdapter : DawAdapter {
    override val dawName: String = "Cubasis 3"
    override val packageName: String = "com.steinberg.cubasis3"
    override val defaultProjectDirectory: String = "/sdcard/Cubasis 3/Projects"
    override val defaultMidiDirectory: String = "/sdcard/Cubasis 3/MIDI"

    override fun planProjectCreation(
        projectName: String,
        bpm: Int,
        midiFilePaths: List<String>
    ): List<AndroidToolAction> {
        return listOf(
            // 1. Launch Cubasis 3
            AndroidToolAction.LaunchApp(packageName),
            // 2. Wait for audio engine and splash screen to settle
            AndroidToolAction.Wait(3000L),
            // 3. Observe initial project screen
            AndroidToolAction.Observe(includeScreenshot = false),
            // 4. Find and open project browser / media bay
            AndroidToolAction.FindText("PROJECT", exact = false),
            // 5. Short wait
            AndroidToolAction.Wait(500L),
            // 6. Find 'New' action or template
            AndroidToolAction.FindText("New", exact = false)
        )
    }
}

/**
 * Adapter for Image-Line FL Studio Mobile on Android.
 */
class FlStudioMobileAdapter : DawAdapter {
    override val dawName: String = "FL Studio Mobile"
    override val packageName: String = "com.imageline.FLM"
    override val defaultProjectDirectory: String = "/sdcard/FLM/User/My Projects"
    override val defaultMidiDirectory: String = "/sdcard/FLM/User/My Tracks"

    override fun planProjectCreation(
        projectName: String,
        bpm: Int,
        midiFilePaths: List<String>
    ): List<AndroidToolAction> {
        return listOf(
            // 1. Launch FL Studio Mobile
            AndroidToolAction.LaunchApp(packageName),
            // 2. Wait for FLM audio engine to initialize
            AndroidToolAction.Wait(2500L),
            // 3. Observe screen
            AndroidToolAction.Observe(includeScreenshot = false),
            // 4. Locate main menu button (top-right logo)
            AndroidToolAction.FindElement("menu_button"),
            // 5. Short wait
            AndroidToolAction.Wait(500L),
            // 6. Select 'New'
            AndroidToolAction.FindText("New", exact = false)
        )
    }
}
