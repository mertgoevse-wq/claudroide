package org.claudroide.app.feature.linux.recovery

import java.io.File

/**
 * Enforces safety transitions and prevents unauthorized destructive or flash actions.
 * Rules derived from spec §11 and RECOVERY_PLAN.md:
 * 1. No FLASH-READY transition without verified Stage 1 EFS backup stored in 2+ locations.
 * 2. No destructive operation without an explicit, recorded HumanApprovalToken.
 */
class SafetyGateManager(
    private val integrityVerifier: ((File, BackupManifest) -> Boolean)? = null
) {

    private var currentSafetyState: SafetyState = SafetyState.SAFE
    private val recordedApprovals = mutableListOf<HumanApprovalToken>()

    fun getSafetyState(): SafetyState = currentSafetyState

    fun getRecordedApprovals(): List<HumanApprovalToken> = recordedApprovals.toList()

    /**
     * Records an explicit human approval token for a specific action.
     */
    fun recordApproval(token: HumanApprovalToken) {
        require(token.approvedBy.isNotBlank()) { "Approval must specify human approver name" }
        require(token.actionName.isNotBlank()) { "Approval must specify action name" }
        require(token.acknowledgedRisk) { "Approver must explicitly acknowledge risk" }
        recordedApprovals.add(token)
    }

    /**
     * Evaluates whether a transition to FLASH-READY is permitted.
     * Enforces that:
     * 1. A verified Stage 1 EFS backup exists.
     * 2. The backup is stored in at least two distinct locations.
     * 3. An approval token is already registered.
     */
    fun canTransitionToFlashReady(
        backupDir: File,
        manifest: BackupManifest,
        actionName: String
    ): Pair<Boolean, String?> {
        if (!manifest.isVerified) {
            return Pair(false, "Stage 1 backup manifest is not marked verified")
        }
        if (!manifest.isEfsComplete) {
            return Pair(false, "Stage 1 backup does not contain all critical EFS partitions (modemst1, modemst2, fsc/fsg)")
        }
        if (manifest.storageLocations.size < 2) {
            return Pair(false, "Stage 1 backup must be stored in at least 2 distinct locations (found: ${manifest.storageLocations.size})")
        }
        if (integrityVerifier != null && !integrityVerifier.invoke(backupDir, manifest)) {
            return Pair(false, "Backup integrity verification failed on disk")
        }
        val hasApproval = recordedApprovals.any {
            it.actionName.equals(actionName, ignoreCase = true) &&
                    it.targetDeviceId.equals(manifest.deviceId, ignoreCase = true)
        }
        if (!hasApproval) {
            return Pair(false, "No recorded human approval token found for action '$actionName' on '${manifest.deviceId}'")
        }

        return Pair(true, null)
    }

    /**
     * Attempts to transition to FLASH-READY state, throwing SecurityException if safety invariants fail.
     */
    fun transitionToFlashReady(
        backupDir: File,
        manifest: BackupManifest,
        actionName: String
    ) {
        val (allowed, reason) = canTransitionToFlashReady(backupDir, manifest, actionName)
        if (!allowed) {
            throw SecurityException("SAFETY GATE VIOLATION: Cannot transition to FLASH-READY. $reason")
        }
        currentSafetyState = SafetyState.FLASH_READY
    }

    /**
     * Asserts that an action is currently authorized to execute.
     */
    fun assertActionPermitted(actionName: String, isDestructive: Boolean) {
        if (isDestructive) {
            if (currentSafetyState != SafetyState.FLASH_READY) {
                throw SecurityException(
                    "SAFETY GATE VIOLATION: Destructive action '$actionName' blocked. Current state is $currentSafetyState (must be FLASH-READY)."
                )
            }
            val hasApproval = recordedApprovals.any { it.actionName.equals(actionName, ignoreCase = true) }
            if (!hasApproval) {
                throw SecurityException(
                    "SAFETY GATE VIOLATION: No explicit human approval token recorded for '$actionName'."
                )
            }
        }
    }

    fun resetToSafe() {
        currentSafetyState = SafetyState.SAFE
    }
}
