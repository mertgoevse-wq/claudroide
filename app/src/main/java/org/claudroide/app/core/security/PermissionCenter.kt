package org.claudroide.app.core.security

/**
 * Task 117 (Gate) — "Permission Center & Approvals Overview" (Freigabeübersicht).
 *
 * Source: `claudroide-spec.md` section 6 ("Freigaben und Sicherheit") and
 * `tasks/117-permission-center.md`.
 *
 * Core Mandates of Task 117:
 *  1. **All project-related accesses and permissions are findable in one place.**
 *     Provides a unified, structured overview across all five required domains:
 *     - [PermissionCategory.FILES]: Project directories, SAF tree URIs, read/write grants.
 *     - [PermissionCategory.PROVIDERS]: LLM endpoints (Claude, OpenRouter, custom), API key access.
 *     - [PermissionCategory.COMMANDS]: Terminal and execution permissions, approval levels.
 *     - [PermissionCategory.SKILLS]: Installed and project-scoped automation skills.
 *     - [PermissionCategory.EXTERNAL_TOOLS]: External MCP servers and specialized helper tools.
 *
 *  2. **Every grant is strictly bound to a project and an explicit purpose.**
 *     [PermissionGrant.projectId] and [PermissionGrant.purpose] are non-negotiable,
 *     non-blank fields enforced at construction time. There is no such thing as an
 *     anonymous or purposed-less permission grant.
 *
 *  3. **Revocation takes effect immediately and explains its boundary.**
 *     Revoking a grant immediately switches its status to [GrantStatus.REVOKED] and
 *     increments the engine's generation counter so no caller can read cached grants
 *     ("never cache a permission state" from `android-permissions-security`).
 *     Furthermore, [RevocationReport] explicitly explains the boundary of revocation:
 *     what is stopped immediately vs. what cannot be undone (e.g. data already sent
 *     over the network or files already written to disk).
 *
 *  4. **Protection against silent extension or reactivation.**
 *     A revoked or expired grant cannot be resurrected or silently extended through
 *     any other setting (such as toggling ReducedPromptMode, switching theme/language,
 *     or editing global settings). Once revoked, only a completely fresh, explicit
 *     user grant with a new ID and purpose can re-authorize the target.
 *
 *  5. **Adaptive presentation support.**
 *     Follows the `adaptive` skill: supports compact single-column layouts for mobile
 *     (FHD+ A56) and multi-column split summaries for larger viewports, with 48 dp
 *     touch-targets and multi-modal status indicators (color + icon + label).
 */

/**
 * The five mandatory categories of project-related permissions.
 */
enum class PermissionCategory(
    val englishLabel: String,
    val germanLabel: String,
    val description: String
) {
    FILES(
        englishLabel = "Files & Storage",
        germanLabel = "Dateien & Speicher",
        description = "Access to project folders via Android SAF, read/write file operations, and path boundaries."
    ),
    PROVIDERS(
        englishLabel = "AI Providers",
        germanLabel = "KI-Anbieter",
        description = "Authorisations for external LLM endpoints, API keys (BYOK), and token transfers."
    ),
    COMMANDS(
        englishLabel = "Commands & Execution",
        germanLabel = "Befehle & Ausführung",
        description = "Permissions to run shell commands, scripts, and build tools in the project directory."
    ),
    SKILLS(
        englishLabel = "Skills & Workflows",
        germanLabel = "Fähigkeiten & Skills",
        description = "Re-usable automation procedures, project-specific and globally installed skills."
    ),
    EXTERNAL_TOOLS(
        englishLabel = "External Tools & MCP",
        germanLabel = "Externe Werkzeuge & MCP",
        description = "Connections to Model Context Protocol (MCP) servers, specialised helper agents, and external bridges."
    );

    val label: String get() = englishLabel
}

/**
 * The operational scope granted by a permission.
 */
enum class GrantScope(
    val englishLabel: String,
    val germanLabel: String
) {
    READ_ONLY("Read Only", "Nur Lesen"),
    READ_WRITE("Read & Write", "Lesen & Schreiben"),
    EXECUTE("Execute", "Ausführen"),
    FULL_ACCESS("Full Access", "Vollzugriff"),
    SESSION_BOUND("Current Session Only", "Nur aktuelle Sitzung");

    val label: String get() = englishLabel
}

/**
 * Lifecycle status of a grant.
 */
enum class GrantStatus(
    val englishLabel: String,
    val germanLabel: String
) {
    ACTIVE("Active", "Aktiv"),
    REVOKED("Revoked", "Widerrufen"),
    EXPIRED("Expired", "Abgelaufen");

    val label: String get() = englishLabel
}

/**
 * A single, auditable permission grant.
 *
 * Every grant must be tied to a specific project ([projectId]) and have an explicit,
 * plain-language [purpose].
 */
data class PermissionGrant(
    val id: String,
    val projectId: String,
    val projectName: String,
    val category: PermissionCategory,
    val target: String,
    val purpose: String,
    val scope: GrantScope,
    val status: GrantStatus = GrantStatus.ACTIVE,
    val grantedAt: Long,
    val grantedByUser: String,
    val expiresAt: Long? = null,
    val sessionId: String? = null,
    val revokedAt: Long? = null,
    val revocationReason: String? = null
) {
    init {
        require(id.isNotBlank()) { "Grant ID must not be blank." }
        require(projectId.isNotBlank()) { "Every grant must be associated with a valid project ID." }
        require(projectName.isNotBlank()) { "Project display name must not be blank." }
        require(target.isNotBlank()) { "Target resource or action must not be blank." }
        require(purpose.isNotBlank()) { "Every grant must have an explicit purpose explaining why access is permitted." }
        require(grantedByUser.isNotBlank()) { "Granting user/actor must be identified for auditing." }
        require(grantedAt > 0L) { "Grant timestamp must be a positive epoch timestamp." }
    }

    /**
     * Dynamically verifies whether this grant is currently valid without caching.
     *
     * In accordance with `android-permissions-security`, permissions are never cached.
     * Evaluates clock bounds, session bounds, and revocation state fresh on every call.
     */
    fun isActiveAt(nowMs: Long, currentSessionId: String? = null): Boolean {
        if (status != GrantStatus.ACTIVE) return false

        // Clock rollback protection: if current time is before the grant time, clock is untrusted.
        if (nowMs < grantedAt) return false

        // Expiration check: if grant has an expiry timestamp and nowMs has passed it.
        if (expiresAt != null && nowMs > expiresAt) return false

        // Session check: if grant is locked to a specific session and session changed.
        if (sessionId != null && currentSessionId != null && sessionId != currentSessionId) {
            return false
        }

        return true
    }

    /**
     * Computes the effective status at the given instant.
     */
    fun effectiveStatus(nowMs: Long, currentSessionId: String? = null): GrantStatus {
        if (status == GrantStatus.REVOKED) return GrantStatus.REVOKED
        if (!isActiveAt(nowMs, currentSessionId)) {
            return GrantStatus.EXPIRED
        }
        return GrantStatus.ACTIVE
    }

    /**
     * Plain-text disclosure line for review screens.
     */
    fun disclosureLine(): String = buildString {
        append("[${category.englishLabel}] ")
        append(target)
        append(" — ")
        append(purpose)
        append(" (")
        append(scope.englishLabel)
        append(", ")
        append(status.englishLabel)
        append(")")
    }
}

/**
 * Report generated upon revoking a grant, detailing immediate effects and irreversible boundaries.
 *
 * Implements the contract: "Widerruf sofort wirkt oder seine Grenze erklärt."
 */
data class RevocationReport(
    val grantId: String,
    val projectId: String,
    val category: PermissionCategory,
    val target: String,
    val revokedAt: Long,
    val reason: String,
    val immediateEffect: String,
    val boundaryExplanation: String,
    val unrevokableNotice: String?
) {
    /**
     * Formats a comprehensive summary for display in dialogs or logs.
     */
    fun fullReportText(): String = buildString {
        appendLine("Revocation Confirmation for $target ($grantId):")
        appendLine("• Immediate effect: $immediateEffect")
        appendLine("• Revocation boundary: $boundaryExplanation")
        if (unrevokableNotice != null) {
            appendLine("• Notice on past actions: $unrevokableNotice")
        }
    }
}

/**
 * Category-level aggregated summary for adaptive presentation.
 */
data class CategorySummary(
    val category: PermissionCategory,
    val totalCount: Int,
    val activeCount: Int,
    val revokedCount: Int,
    val expiredCount: Int,
    val items: List<PermissionGrant>
) {
    val hasActiveGrants: Boolean get() = activeCount > 0
}

/**
 * Complete project permission overview returned to callers and presentation layers.
 */
data class ProjectPermissionOverview(
    val projectId: String,
    val projectName: String,
    val generatedAt: Long,
    val totalGrantsCount: Int,
    val activeGrantsCount: Int,
    val revokedGrantsCount: Int,
    val expiredGrantsCount: Int,
    val categories: List<CategorySummary>,
    val headline: String,
    val hasDangerousActiveGrants: Boolean
) {
    /** Returns summary for a specific category. */
    fun forCategory(category: PermissionCategory): CategorySummary {
        return categories.firstOrNull { it.category == category }
            ?: CategorySummary(category, 0, 0, 0, 0, emptyList())
    }
}

/**
 * Central engine and repository for project permissions and approval states.
 *
 * Structural Invariants:
 *  1. Every grant is indexed by [PermissionGrant.id] and scoped to [PermissionGrant.projectId].
 *  2. Revocation is irreversible: a revoked grant can never be switched back to ACTIVE.
 *  3. Generational invalidation: every mutation increments [generation], invalidating
 *     any external cached state.
 *  4. Setting decoupling: no external toggle (such as ReducedPromptMode or AppSettings)
 *     can override or revive an inactive grant.
 */
class PermissionCenter(
    initialGrants: List<PermissionGrant> = emptyList()
) {
    private val grantsMap = mutableMapOf<String, PermissionGrant>()

    /**
     * Monotonically increasing generation counter.
     * Any change (new grant, revocation, purge) increments this value.
     */
    var generation: Long = 0L
        private set

    init {
        for (grant in initialGrants) {
            grantsMap[grant.id] = grant
        }
        if (initialGrants.isNotEmpty()) {
            generation++
        }
    }

    /**
     * Registers a new grant for a project.
     *
     * @throws IllegalArgumentException if an active grant already exists for the same
     *         target within the same project and category, or if required fields are blank.
     */
    @Synchronized
    fun registerGrant(
        id: String,
        projectId: String,
        projectName: String,
        category: PermissionCategory,
        target: String,
        purpose: String,
        scope: GrantScope,
        nowMs: Long,
        grantedByUser: String,
        expiresAt: Long? = null,
        sessionId: String? = null
    ): PermissionGrant {
        val cleanId = id.trim()
        val cleanProjectId = projectId.trim()
        val cleanProjectName = projectName.trim()
        val cleanTarget = target.trim()
        val cleanPurpose = purpose.trim()
        val cleanUser = grantedByUser.trim()

        // Check for duplicate active grant on the same project/category/target
        val existingActive = grantsMap.values.firstOrNull {
            it.projectId == cleanProjectId &&
                it.category == category &&
                it.target.equals(cleanTarget, ignoreCase = true) &&
                it.isActiveAt(nowMs, sessionId)
        }
        if (existingActive != null) {
            throw IllegalArgumentException(
                "An active grant already exists for project '$cleanProjectId' in category '${category.englishLabel}' targeting '$cleanTarget'."
            )
        }

        val grant = PermissionGrant(
            id = cleanId,
            projectId = cleanProjectId,
            projectName = cleanProjectName,
            category = category,
            target = cleanTarget,
            purpose = cleanPurpose,
            scope = scope,
            status = GrantStatus.ACTIVE,
            grantedAt = nowMs,
            grantedByUser = cleanUser,
            expiresAt = expiresAt,
            sessionId = sessionId
        )

        grantsMap[cleanId] = grant
        generation++
        return grant
    }

    /**
     * Immediately revokes a grant and produces an explanation of its boundary.
     *
     * Returns a [RevocationReport] explaining the immediate consequences and
     * irreversible limits of the action.
     *
     * @throws NoSuchElementException if [grantId] is not found.
     * @throws IllegalStateException if the grant was already revoked.
     */
    @Synchronized
    fun revokeGrant(
        grantId: String,
        revokedByUser: String,
        reason: String,
        nowMs: Long
    ): RevocationReport {
        val existing = grantsMap[grantId]
            ?: throw NoSuchElementException("Permission grant '$grantId' was not found.")

        if (existing.status == GrantStatus.REVOKED) {
            throw IllegalStateException("Permission grant '$grantId' is already revoked.")
        }

        val cleanReason = if (reason.isBlank()) "Revoked by user $revokedByUser." else reason.trim()

        val updated = existing.copy(
            status = GrantStatus.REVOKED,
            revokedAt = nowMs,
            revocationReason = cleanReason
        )

        grantsMap[grantId] = updated
        generation++

        return buildRevocationReport(updated, nowMs, cleanReason)
    }

    /**
     * Revokes all active grants for a specific project (e.g. upon project unbinding or USB removal).
     */
    @Synchronized
    fun revokeAllForProject(
        projectId: String,
        revokedByUser: String,
        reason: String,
        nowMs: Long
    ): List<RevocationReport> {
        val targetGrants = grantsMap.values.filter {
            it.projectId == projectId && it.status == GrantStatus.ACTIVE
        }

        return targetGrants.map { grant ->
            revokeGrant(grant.id, revokedByUser, reason, nowMs)
        }
    }

    /**
     * Revokes all active grants within a category for a specific project.
     */
    @Synchronized
    fun revokeCategory(
        projectId: String,
        category: PermissionCategory,
        revokedByUser: String,
        reason: String,
        nowMs: Long
    ): List<RevocationReport> {
        val targetGrants = grantsMap.values.filter {
            it.projectId == projectId && it.category == category && it.status == GrantStatus.ACTIVE
        }

        return targetGrants.map { grant ->
            revokeGrant(grant.id, revokedByUser, reason, nowMs)
        }
    }

    /**
     * Checks if an action on [target] is permitted for [projectId] in [category].
     * Always recomputes status dynamically without caching.
     */
    @Synchronized
    fun isGranted(
        projectId: String,
        category: PermissionCategory,
        target: String,
        nowMs: Long,
        currentSessionId: String? = null
    ): Boolean {
        return grantsMap.values.any { grant ->
            grant.projectId == projectId &&
                grant.category == category &&
                grant.target.equals(target.trim(), ignoreCase = true) &&
                grant.isActiveAt(nowMs, currentSessionId)
        }
    }

    /**
     * Defense-in-depth security check: explicitly prevents any setting change or
     * external actor from silently reactivating or extending a revoked grant.
     *
     * @return [ExtensionResult.Rejected] if grant is revoked or expired.
     */
    @Synchronized
    fun attemptSilentExtension(
        grantId: String,
        attemptedBySetting: String
    ): ExtensionResult {
        val grant = grantsMap[grantId]
            ?: return ExtensionResult.Rejected("Grant not found.")

        if (grant.status == GrantStatus.REVOKED) {
            return ExtensionResult.Rejected(
                "Protection Invariant: Setting '$attemptedBySetting' cannot reactivate revoked grant '${grant.id}' ($grantId). Explicit user re-authorization required."
            )
        }

        return ExtensionResult.Rejected(
            "Protection Invariant: Permissions cannot be extended implicitly through setting '$attemptedBySetting'."
        )
    }

    /**
     * Generates a complete project permission overview for [projectId].
     */
    @Synchronized
    fun getOverview(
        projectId: String,
        nowMs: Long,
        currentSessionId: String? = null
    ): ProjectPermissionOverview {
        val projectGrants = grantsMap.values.filter { it.projectId == projectId }
        val projectName = projectGrants.firstOrNull()?.projectName ?: projectId

        var totalCount = 0
        var activeCount = 0
        var revokedCount = 0
        var expiredCount = 0

        val categoryGroups = mutableMapOf<PermissionCategory, MutableList<PermissionGrant>>()
        for (cat in PermissionCategory.entries) {
            categoryGroups[cat] = mutableListOf()
        }

        for (grant in projectGrants) {
            totalCount++
            when (grant.effectiveStatus(nowMs, currentSessionId)) {
                GrantStatus.ACTIVE -> activeCount++
                GrantStatus.REVOKED -> revokedCount++
                GrantStatus.EXPIRED -> expiredCount++
            }
            categoryGroups[grant.category]?.add(grant)
        }

        val summaries = PermissionCategory.entries.map { cat ->
            val items = categoryGroups[cat] ?: emptyList()
            var catActive = 0
            var catRevoked = 0
            var catExpired = 0

            for (g in items) {
                when (g.effectiveStatus(nowMs, currentSessionId)) {
                    GrantStatus.ACTIVE -> catActive++
                    GrantStatus.REVOKED -> catRevoked++
                    GrantStatus.EXPIRED -> catExpired++
                }
            }

            CategorySummary(
                category = cat,
                totalCount = items.size,
                activeCount = catActive,
                revokedCount = catRevoked,
                expiredCount = catExpired,
                items = items
            )
        }

        val headline = when {
            totalCount == 0 -> "Keine Freigaben für dieses Projekt eingetragen."
            activeCount == 0 -> "Alle Freigaben für dieses Projekt sind widerrufen oder abgelaufen."
            else -> "$activeCount aktive Freigabe(n) für Projekt '$projectName'."
        }

        // Dangerous grants: e.g. FULL_ACCESS or EXECUTE or broad PROVIDERS/COMMANDS
        val hasDangerous = projectGrants.any { grant ->
            grant.isActiveAt(nowMs, currentSessionId) &&
                (grant.scope == GrantScope.FULL_ACCESS ||
                    grant.scope == GrantScope.EXECUTE ||
                    grant.category == PermissionCategory.COMMANDS)
        }

        return ProjectPermissionOverview(
            projectId = projectId,
            projectName = projectName,
            generatedAt = nowMs,
            totalGrantsCount = totalCount,
            activeGrantsCount = activeCount,
            revokedGrantsCount = revokedCount,
            expiredGrantsCount = expiredCount,
            categories = summaries,
            headline = headline,
            hasDangerousActiveGrants = hasDangerous
        )
    }

    /**
     * All grants currently managed in the registry.
     */
    @Synchronized
    fun allGrants(): List<PermissionGrant> = grantsMap.values.toList()

    /**
     * Retrieves a single grant by ID.
     */
    @Synchronized
    fun getGrant(grantId: String): PermissionGrant? = grantsMap[grantId]

    /**
     * Constructs a tailored revocation report explaining the domain boundary.
     */
    private fun buildRevocationReport(
        grant: PermissionGrant,
        revokedAt: Long,
        reason: String
    ): RevocationReport {
        val immediateEffect = when (grant.category) {
            PermissionCategory.FILES ->
                "File access stopped immediately; persistable SAF URI released and path access revoked."
            PermissionCategory.PROVIDERS ->
                "Decoupled from provider session; no further prompts, tokens, or credentials will be transmitted."
            PermissionCategory.COMMANDS ->
                "Command execution authorization revoked; any pending process execution is refused."
            PermissionCategory.SKILLS ->
                "Skill workflow disabled; future automated execution calls are blocked."
            PermissionCategory.EXTERNAL_TOOLS ->
                "External tool/MCP connection severed; tool calls are rejected."
        }

        val boundaryExplanation = when (grant.category) {
            PermissionCategory.FILES ->
                "Files already modified or saved to disk prior to revocation remain on storage and are not deleted."
            PermissionCategory.PROVIDERS ->
                "Tokens and prompt content already transmitted across the network cannot be recalled from external servers."
            PermissionCategory.COMMANDS ->
                "Side effects or changes made by previously completed commands cannot be automatically undone."
            PermissionCategory.SKILLS ->
                "Output artifacts or analysis records generated by earlier skill executions remain intact."
            PermissionCategory.EXTERNAL_TOOLS ->
                "External actions or API calls already executed by the external tool cannot be reversed by ClauDroide."
        }

        val unrevokableNotice = when (grant.category) {
            PermissionCategory.PROVIDERS ->
                "Notice: Provider access token disconnected. Network transmission logs record previously sent byte counts."
            PermissionCategory.COMMANDS ->
                "Notice: Check your git status or disk state to inspect any prior changes made before revocation."
            PermissionCategory.FILES ->
                "Notice: Use Git history or file backups to revert any earlier edits if desired."
            else -> null
        }

        return RevocationReport(
            grantId = grant.id,
            projectId = grant.projectId,
            category = grant.category,
            target = grant.target,
            revokedAt = revokedAt,
            reason = reason,
            immediateEffect = immediateEffect,
            boundaryExplanation = boundaryExplanation,
            unrevokableNotice = unrevokableNotice
        )
    }

    sealed class ExtensionResult {
        data class Rejected(val message: String) : ExtensionResult()
    }
}
