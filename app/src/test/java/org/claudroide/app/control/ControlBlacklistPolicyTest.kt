package org.claudroide.app.control

import org.claudroide.app.feature.control.safety.BlacklistVerdict
import org.claudroide.app.feature.control.safety.ControlBlacklistPolicy
import org.claudroide.app.feature.control.tools.AndroidToolAction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlBlacklistPolicyTest {

    private val policy = ControlBlacklistPolicy()

    @Test
    fun defaultBlockedPackages_areBlocked() {
        assertTrue(policy.isPackageBlocked("com.paypal.android.p2pmobile"))
        assertTrue(policy.isPackageBlocked("com.revolut.revolut"))
        assertTrue(policy.isPackageBlocked("de.number26.android"))
        assertTrue(policy.isPackageBlocked("com.x8bit.bitwarden"))
        assertTrue(policy.isPackageBlocked("com.onepassword.android"))
    }

    @Test
    fun keywordMatching_blocksBankingAndAuthenticator() {
        assertTrue(policy.isPackageBlocked("de.sparkasse.banking"))
        assertTrue(policy.isPackageBlocked("com.sample.passwords.app"))
        assertTrue(policy.isPackageBlocked("org.example.authenticator.otp"))
    }

    @Test
    fun nonSensitivePackages_areAllowed() {
        assertFalse(policy.isPackageBlocked("com.steinberg.cubasis3"))
        assertFalse(policy.isPackageBlocked("org.claudroide.app"))
        assertFalse(policy.isPackageBlocked("com.android.calculator2"))
    }

    @Test
    fun launchApp_withBlacklistedPackage_isBlocked() {
        val action = AndroidToolAction.LaunchApp("com.paypal.android.p2pmobile")
        val verdict = policy.checkActionAllowed(action, currentForegroundPackage = "org.claudroide.app")
        assertTrue(verdict is BlacklistVerdict.Blocked)
    }

    @Test
    fun interactions_onBlacklistedForegroundApp_areBlocked() {
        val tapAction = AndroidToolAction.Tap(100f, 200f)
        val verdict = policy.checkActionAllowed(tapAction, currentForegroundPackage = "de.number26.android")
        assertTrue(verdict is BlacklistVerdict.Blocked)
    }

    @Test
    fun interactions_onAllowedForegroundApp_areAllowed() {
        val tapAction = AndroidToolAction.Tap(100f, 200f)
        val verdict = policy.checkActionAllowed(tapAction, currentForegroundPackage = "com.steinberg.cubasis3")
        assertTrue(verdict is BlacklistVerdict.Allowed)
    }

    @Test
    fun customBlockedPackage_canBeAddedAndRemoved() {
        assertFalse(policy.isPackageBlocked("com.custom.target"))
        policy.addBlockedPackage("com.custom.target")
        assertTrue(policy.isPackageBlocked("com.custom.target"))
        policy.removeBlockedPackage("com.custom.target")
        assertFalse(policy.isPackageBlocked("com.custom.target"))
    }
}
