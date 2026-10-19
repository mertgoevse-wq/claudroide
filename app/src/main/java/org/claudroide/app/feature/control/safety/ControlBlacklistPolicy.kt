package org.claudroide.app.feature.control.safety

import org.claudroide.app.feature.control.tools.AndroidToolAction

/**
 * Result of evaluating an action against the control blacklist policy.
 */
sealed class BlacklistVerdict {
    object Allowed : BlacklistVerdict()
    data class Blocked(val packageName: String, val reason: String) : BlacklistVerdict()
}

/**
 * Task 6.5 / R-5.5 / Spec §6.5 — Control Blacklist Policy.
 *
 * Prevents the Device Agent from inspecting, tapping, capturing, or interacting
 * with sensitive applications:
 *  - Banking and financial services
 *  - Payment providers and wallets
 *  - Password managers and authenticator applications
 *  - System security and credential storage
 */
class ControlBlacklistPolicy(
    customBlockedPackages: Set<String> = emptySet()
) {

    private val blockedPackages = DEFAULT_BLOCKED_PACKAGES.toMutableSet().apply {
        addAll(customBlockedPackages)
    }

    private val blockedKeywords = DEFAULT_BLOCKED_KEYWORDS.toMutableSet()

    fun isPackageBlocked(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val lower = packageName.trim().lowercase()
        if (blockedPackages.contains(lower)) return true
        return blockedKeywords.any { keyword -> lower.contains(keyword) }
    }

    fun checkActionAllowed(
        action: AndroidToolAction,
        currentForegroundPackage: String?
    ): BlacklistVerdict {
        // Check target package if action is LaunchApp
        if (action is AndroidToolAction.LaunchApp) {
            if (isPackageBlocked(action.packageName)) {
                return BlacklistVerdict.Blocked(
                    packageName = action.packageName,
                    reason = "Target application '${action.packageName}' is on the protected blacklist."
                )
            }
        }

        // Check active foreground package for interaction or inspection actions
        if (currentForegroundPackage != null && isPackageBlocked(currentForegroundPackage)) {
            return BlacklistVerdict.Blocked(
                packageName = currentForegroundPackage,
                reason = "Active application '$currentForegroundPackage' is protected by security blacklist. All interactions, gestures, and captures are prohibited."
            )
        }

        return BlacklistVerdict.Allowed
    }

    fun addBlockedPackage(packageName: String): Boolean {
        val clean = packageName.trim().lowercase()
        if (clean.isBlank()) return false
        return blockedPackages.add(clean)
    }

    fun removeBlockedPackage(packageName: String): Boolean {
        return blockedPackages.remove(packageName.trim().lowercase())
    }

    fun getBlockedPackages(): Set<String> = blockedPackages.toSet()

    companion object {
        val DEFAULT_BLOCKED_PACKAGES = setOf(
            // Global & European Banking / Fintech
            "com.paypal.android.p2pmobile",
            "com.revolut.revolut",
            "de.number26.android",
            "com.klarna.mobile",
            "de.sparkasse.android.bankingapp",
            "com.commerzbank.mobile",
            "de.dkb.portalapp",
            "de.ingdiba.bankingapp",
            "com.db.pbc.mmbanking",
            // Password Managers & 2FA
            "com.x8bit.bitwarden",
            "com.onepassword.android",
            "keepass2android.keepass2android",
            "com.lastpass.lpandroid",
            "com.dashlane",
            "com.google.android.apps.authenticator2",
            "com.azure.authenticator",
            // Android System Security Settings
            "com.android.settings.password",
            "com.google.android.gms.auth"
        )

        val DEFAULT_BLOCKED_KEYWORDS = setOf(
            "banking",
            "passwords",
            "authenticator",
            "keystore",
            "credential"
        )
    }
}
