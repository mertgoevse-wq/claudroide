package org.claudroide.app.feature.agent

/**
 * Agent operating mode — determines what the agent is allowed to do and how it
 * communicates its intent.
 *
 * DISCUSS: Explore, plan, explain — no file writes, no commands, no provider calls.
 * BUILD: Execute approved plan — file changes, commands, provider calls via ToolExecutor.
 *
 * The mode is **per-agent**, not global: a planner runs in DISCUSS, an engineer in BUILD.
 * The UI shows the current mode and gates pocketAction/pocketArtifact blocks to BUILD only.
 */
enum class AgentMode(val label: String, val germanLabel: String) {

    /** Read-only analysis, planning, and explanation. No side effects. */
    DISCUSS("Discuss", "Besprechen") {
        override val allowsWrites: Boolean = false
        override val allowsCommands: Boolean = false
        override val allowsProviderCalls: Boolean = false
        override val requiresUserConfirmation: Boolean = false
    },

    /** Execute approved changes. Side effects permitted within granted approvals. */
    BUILD("Build", "Bauen") {
        override val allowsWrites: Boolean = true
        override val allowsCommands: Boolean = true
        override val allowsProviderCalls: Boolean = true
        override val requiresUserConfirmation: Boolean = true
    };

    /**
     * Whether this mode permits file writes.
     */
    abstract val allowsWrites: Boolean

    /**
     * Whether this mode permits shell command execution.
     */
    abstract val allowsCommands: Boolean

    /**
     * Whether this mode permits calls to external LLM providers.
     */
    abstract val allowsProviderCalls: Boolean

    /**
     * Whether actions in this mode need explicit user confirmation per step.
     */
    abstract val requiresUserConfirmation: Boolean

    companion object {
        /** Default mode for new sessions — safe, no side effects. */
        const val DEFAULT: AgentMode = DISCUSS

        /** Parse from string (case-insensitive). */
        fun parse(input: String): AgentMode = when (input.trim().lowercase()) {
            "build", "bauen" -> BUILD
            else -> DISCUSS
        }
    }
}