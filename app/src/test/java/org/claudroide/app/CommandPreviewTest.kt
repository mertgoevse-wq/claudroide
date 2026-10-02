package org.claudroide.app

import org.claudroide.app.feature.agent.CommandEffect
import org.claudroide.app.feature.agent.CommandPreview
import org.claudroide.app.feature.agent.CommandPreviewPolicy
import org.claudroide.app.feature.agent.CommandRequirements
import org.claudroide.app.feature.agent.CommandRisk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 107 — "Show the command first" (Befehl vorher zeigen).
 *
 * The tests sit on the three promises: the preview cannot run anything, the
 * three serious effects are marked on their own, and an unrecognised command is
 * never described as harmless.
 */
class CommandPreviewTest {

    private fun describe(command: String): CommandPreview =
        CommandPreviewPolicy.describe(command, "/storage/emulated/0/Documents/project")

    // ---- Promise 1: the preview cannot trigger anything --------------------

    /**
     * A preview must not carry a way to execute.
     *
     * This is asserted over the public API by reflection rather than by reading
     * the code, because the claim "the preview only previews" is precisely what
     * decays the first time a `run()` or a lambda parameter is added. The
     * compiler-generated members of the data class are excluded, because Kotlin
     * creates them for every type and they are not something the author wrote.
     */
    @Test
    fun previewTypeExposesNoWayToExecute() {
        val generated = setOf(
            "component1", "component2", "component3", "component4", "component5",
            "copy", "copy\$default", "toString", "hashCode", "equals"
        )

        val previewMethods = CommandPreview::class.java.declaredMethods
            .filterNot { it.name in generated }
            .map { it.name }

        val forbidden = listOf(
            "run", "execute", "exec", "start", "launch", "apply", "commit",
            "confirm", "approve", "invoke", "perform", "doIt", "call"
        )

        val found = previewMethods.filter { forbidden.contains(it) }
        assertEquals(
            "CommandPreview must not expose a way to run anything, found: $found",
            emptyList<String>(),
            found
        )
    }

    /** The same promise for the policy object: it decides, it does not act. */
    @Test
    fun policyTypeExposesNoWayToExecute() {
        val policyMethods = CommandPreviewPolicy::class.java.declaredMethods
            .filterNot { it.name.startsWith("access$") }
            .map { it.name }

        val forbidden = listOf(
            "run", "execute", "exec", "start", "launch", "apply", "commit",
            "confirm", "approve", "invoke", "perform"
        )

        val found = policyMethods.filter { forbidden.contains(it) }
        assertEquals(
            "CommandPreviewPolicy must only decide, found: $found",
            emptyList<String>(),
            found
        )
    }

    /**
     * The preview holds only values.
     *
     * Every field is a String, enum, Boolean or Long — nothing that could be
     * invoked. This catches a "harmless" `onConfirm: () -> Unit` field, which
     * would pass the method-name check above while still being a trigger.
     */
    @Test
    fun previewHoldsNoCallableField() {
        val allowed = setOf(
            String::class.java, CommandRisk::class.java, CommandRequirements::class.java,
            Boolean::class.java, Int::class.java, Long::class.java
        )

        CommandPreview::class.java.declaredFields.forEach { field ->
            val type = field.type
            assertTrue(
                "CommandPreview.${field.name} has type ${type.name}, which is not a plain value",
                allowed.contains(type) || type == List::class.java
            )
        }
    }

    @Test
    fun buildingAPreviewDoesNotRunAnything() {
        // Building a preview for the most destructive command in the set is a
        // pure text operation, on a device, in a plain JVM test with no Android
        // runtime present. If this ever needed one, the type would have grown
        // an Android import and these tests would no longer be runnable here.
        val preview = describe("rm -rf /storage/emulated/0/Documents/project/build")
        assertEquals(CommandRisk.HIGH, preview.risk)
        assertEquals(listOf(CommandEffect.DELETES_DATA), preview.effects)
        assertTrue(
            "the destructive command must still be marked after being built",
            CommandEffect.DELETES_DATA in preview.highlightedEffects()
        )
    }

    // ---- Promise 2: install, delete and external transfer are marked -------

    @Test
    fun installingIsMarkedAsInstalling() {
        val preview = describe("npm install express")
        assertEquals(CommandRisk.HIGH, preview.risk)
        assertTrue(
            "installing must be listed as installing, was ${preview.effects}",
            CommandEffect.INSTALLS_SOFTWARE in preview.effects
        )
        assertTrue(
            "installing must be highlighted, was ${preview.highlightedEffects()}",
            CommandEffect.INSTALLS_SOFTWARE in preview.highlightedEffects()
        )
    }

    @Test
    fun deletingIsMarkedAsDeleting() {
        val preview = describe("rm -rf build")
        assertEquals(CommandRisk.HIGH, preview.risk)
        assertTrue(
            "deleting must be listed as deleting, was ${preview.effects}",
            CommandEffect.DELETES_DATA in preview.effects
        )
        assertTrue(
            "deleting must be highlighted, was ${preview.highlightedEffects()}",
            CommandEffect.DELETES_DATA in preview.highlightedEffects()
        )
    }

    @Test
    fun sendingDataOffDeviceIsMarkedAsSending() {
        val preview = describe("git push origin main")
        assertEquals(CommandRisk.HIGH, preview.risk)
        assertTrue(
            "pushing must be listed as sending data off the device, was ${preview.effects}",
            CommandEffect.SENDS_DATA_OFF_DEVICE in preview.effects
        )
        assertTrue(
            "sending data must be highlighted, was ${preview.highlightedEffects()}",
            CommandEffect.SENDS_DATA_OFF_DEVICE in preview.highlightedEffects()
        )
    }

    @Test
    fun theDeletionWarningSaysItCannotBeUndone() {
        val warning = CommandPreviewPolicy.warningFor(describe("rm -rf src"))
        assertTrue(
            "the deletion warning must say it cannot be undone, was: $warning",
            warning.contains("cannot be undone")
        )
    }

    /** A read-only command must not cry wolf. */
    @Test
    fun readingIsNotMarkedAsRisky() {
        val preview = describe("ls -la")
        assertEquals(CommandRisk.LOW, preview.risk)
        assertEquals(
            "nothing irreversible should be highlighted for a read, was ${preview.highlightedEffects()}",
            emptyList<CommandEffect>(),
            preview.highlightedEffects()
        )
        assertEquals("", CommandPreviewPolicy.warningFor(preview))
        assertFalse(preview.needsConfirmation)
    }

    /** The folder comes before the permission list. */
    @Test
    fun linesLeadWithCommandAndFolder() {
        val lines = describe("gradle assembleDebug").lines()
        assertTrue("first line must be the command, was: $lines", lines[0].startsWith("Command:"))
        assertTrue("second line must be the folder, was: $lines", lines[1].startsWith("Folder:"))
        assertTrue(
            "the purpose must appear before the requirements, was: $lines",
            lines.any { it.startsWith("Purpose:") }
        )
    }

    // ---- Promise 3: unknown is never called harmless -----------------------

    @Test
    fun unknownCommandIsNotCalledLowRisk() {
        val preview = describe("frobnicate --all")
        assertEquals(CommandRisk.UNRECOGNISED, preview.risk)
    }

    @Test
    fun unknownCommandAlwaysNeedsConfirmation() {
        val preview = describe("frobnicate --all")
        assertTrue(
            "an unknown command must always be confirmed",
            preview.needsConfirmation
        )
        assertTrue(
            CommandRisk.UNRECOGNISED.needsExplicitConfirmation
        )
    }

    @Test
    fun unknownCommandSaysItIsUnknown() {
        val preview = describe("frobnicate --all")
        assertTrue(
            "the risk must be described as unknown, was: ${preview.risk.label}",
            preview.risk.label.contains("unknown")
        )
    }

    /**
     * No risk value may use the word "safe".
     *
     * Asserted over the whole enum rather than per case: a future risk added
     * with a reassuring label is exactly the regression this guards.
     */
    @Test
    fun noRiskLabelClaimsSafety() {
        CommandRisk.values().forEach { risk ->
            assertFalse(
                "risk ${risk.name} is described as '${risk.label}'",
                risk.label.contains("safe", ignoreCase = true) ||
                    risk.label.contains("harmless", ignoreCase = true) ||
                    risk.label.contains("no risk", ignoreCase = true)
            )
        }
    }

    /** An unknown command reports a gap, not "nothing happens". */
    @Test
    fun unknownCommandReportsAnUnmodelledEffect() {
        val effects = CommandPreviewPolicy.effectsOf("frobnicate --all")
        assertEquals(listOf(CommandEffect.UNMODELLED_EFFECT), effects)
    }

    @Test
    fun unknownCommandDoesNotClaimNoEffects() {
        val preview = describe("frobnicate --all")
        assertFalse(
            "an unknown command must not be presented as having no effects",
            preview.lines().any { it == "Effects: none known." }
        )
    }

    @Test
    fun unknownCommandWarningSaysTheAppCannotTell() {
        val warning = CommandPreviewPolicy.warningFor(describe("frobnicate --all"))
        assertTrue(
            "the warning must say the app does not know, was: $warning",
            warning.contains("does not know")
        )
    }

    @Test
    fun emptyCommandIsUnknown() {
        val preview = describe("")
        assertEquals(CommandRisk.UNRECOGNISED, preview.risk)
        assertEquals(listOf(CommandEffect.UNMODELLED_EFFECT), preview.effects)
    }

    @Test
    fun whitespaceOnlyCommandIsUnknown() {
        val preview = describe("   \t  ")
        assertEquals(CommandRisk.UNRECOGNISED, preview.risk)
    }

    // ---- Git subcommands are separate commands ----------------------------

    @Test
    fun gitPushAndGitStatusDiffer() {
        val push = describe("git push origin main")
        val status = describe("git status")

        assertEquals(CommandRisk.HIGH, push.risk)
        assertEquals(CommandRisk.LOW, status.risk)
        assertFalse(
            "git status must not send data off the device, was ${status.effects}",
            CommandEffect.SENDS_DATA_OFF_DEVICE in status.effects
        )
    }

    // ---- Requirements are derived, not promised ---------------------------

    /**
     * A caller cannot declare an installing command to need nothing.
     *
     * The requirements are computed from the effects, so they cannot disagree
     * with what the command does.
     */
    @Test
    fun requirementsFollowFromTheEffects() {
        val install = describe("pip install requests")
        assertTrue(install.requirements.needsNetwork)
        assertTrue(install.requirements.needsStorage)

        val read = describe("cat build.gradle.kts")
        assertFalse(read.requirements.needsNetwork)
        assertFalse(read.requirements.needsStorage)
    }

    @Test
    fun networkUseIsShownInTheRequirements() {
        val preview = describe("gradle assembleDebug")
        assertTrue(preview.requirements.needsNetwork)
        val lines = preview.lines()
        assertTrue("needs line must be present, was: $lines", lines.any { it.startsWith("Needs:") })
        assertTrue(
            "the needs line must mention the network, was: $lines",
            lines.any { it.startsWith("Needs:") && it.contains("network") }
        )
    }

    @Test
    fun aCommandWithNoRequirementsSaysNothingAboutThem() {
        val preview = describe("ls")
        assertFalse(preview.requirements.hasAnyRequirement)
        assertTrue(
            "no needs line for a plain read, was: ${preview.lines()}",
            preview.lines().none { it.startsWith("Needs:") }
        )
    }

    // ---- The folder is part of the preview --------------------------------

    @Test
    fun previewOutsideTheReleasedFolderIsNotAccepted() {
        // The folder is the command's own working directory, so this asks about
        // a command that would run one level above the released project.
        val preview = CommandPreviewPolicy.describe("rm -rf src", "../other")
        assertFalse(
            "a preview for a folder outside the release must not pass",
            CommandPreviewPolicy.isPreviewForReleasedFolder(
                preview,
                "/storage/emulated/0/Documents/project"
            )
        )
    }

    @Test
    fun previewInsideTheReleasedFolderIsAccepted() {
        val preview = CommandPreviewPolicy.describe("rm -rf src", "build")
        assertTrue(
            CommandPreviewPolicy.isPreviewForReleasedFolder(
                preview,
                "/storage/emulated/0/Documents/project"
            )
        )
    }

    /**
     * The folder itself is not part of the tool lookup.
     *
     * A trailing path must not make a known command unknown, or `rm -rf` with an
     * argument would be downgraded to "unknown" while remaining just as
     * destructive.
     */
    @Test
    fun argumentsDoNotHideAKnownCommand() {
        assertEquals(CommandRisk.HIGH, describe("rm -rf src/main").risk)
        assertEquals(CommandRisk.HIGH, describe("rm").risk)
        assertEquals(CommandRisk.HIGH, describe("rm -rf").risk)
        assertEquals(CommandRisk.LOW, describe("ls -la src").risk)
        assertEquals(CommandRisk.MEDIUM, describe("gradle assembleDebug").risk)
        assertEquals(CommandRisk.HIGH, describe("git push origin main").risk)
        assertEquals(CommandRisk.LOW, describe("git status").risk)
    }

    // ---- Marked effects are ordered worst first ----------------------------

    /**
     * Two previews of the same command read the same way.
     *
     * The order is fixed by the enum, not by the order the effects happened to
     * be discovered, so a user who has seen a command once can find the warning
     * again without reading.
     */
    @Test
    fun highlightedEffectsAreOrderedDeterministically() {
        val a = describe("npm install").highlightedEffects()
        val b = describe("npm install").highlightedEffects()
        assertEquals(a, b)

        val multi = describe("apt install curl").highlightedEffects()
        assertEquals(
            "installing must be listed before the network",
            CommandEffect.INSTALLS_SOFTWARE,
            multi.first()
        )
    }

    @Test
    fun networkIsHighlightedEvenThoughItIsReversible() {
        val preview = describe("curl https://example.com")
        assertTrue(
            "network use must be highlighted even though it is not irreversible",
            CommandEffect.USES_NETWORK in preview.highlightedEffects()
        )
    }
}