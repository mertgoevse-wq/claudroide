package org.claudroide.app.feature.provider

/**
 * A provider-neutral contract for everything the chat surface needs.
 *
 * Task 070 exists so the chat UI, the file tools and the approval flow can talk to
 * *any* provider through one shape, without leaking provider quirks upward and
 * without faking a capability that a provider does not have.
 *
 * Invariants:
 *  - Every capability is reported as supported / unsupported / unknown. Nothing is
 *    silently emulated. A provider that cannot stream says so; the UI then shows a
 *    wait-for-completion state instead of a fake token stream.
 *  - The contract never carries an API key, a token, or a raw secret. Key material
 *    stays inside the provider's own storage layer ([SecureKeyStore]).
 *  - A request is only ever built for the provider the user explicitly selected.
 *  - Cancellation is part of the contract, not an afterthought of the transport.
 */

/** How a provider behaves for one capability. */
enum class CapabilitySupport {
    /** Documented as working. */
    SUPPORTED,

    /** Documented as not available on this provider or model. */
    UNSUPPORTED,

    /** Not verified. Treated as unavailable by callers that must not guess. */
    UNKNOWN
}

/** Streaming behaviour a provider can offer. */
enum class StreamingMode {
    /** Tokens arrive incrementally over SSE. */
    INCREMENTAL,

    /** The provider only returns a complete response; the UI must wait. */
    NONE
}

/** How authentication is performed, per the provider catalog. */
enum class ProviderAuth {
    /** `x-api-key` header (Anthropic style). */
    API_KEY_HEADER,

    /** `Authorization: Bearer` header. */
    BEARER,

    /** No credential, e.g. a local server on the same device. */
    NONE
}

/**
 * What a provider can actually do, resolved for one concrete (provider, model) pair.
 *
 * A field is never optimistic: [UNSUPPORTED] and [UNKNOWN] both mean "do not offer
 * this to the user as if it worked".
 */
data class ProviderCapabilities(
    val streaming: CapabilitySupport,
    val toolCalling: CapabilitySupport,
    val imageInput: CapabilitySupport,
    val systemInstructions: CapabilitySupport
) {
    /**
     * The streaming mode to actually use.
     *
     * Anything other than a confirmed [CapabilitySupport.SUPPORTED] yields
     * [StreamingMode.NONE]: the answer is fetched in one piece rather than faked
     * as a stream.
     */
    val effectiveStreaming: StreamingMode
        get() = if (streaming == CapabilitySupport.SUPPORTED) {
            StreamingMode.INCREMENTAL
        } else {
            StreamingMode.NONE
        }

    /** True when a tool call may be dispatched to this provider. */
    val canUseTools: Boolean get() = toolCalling == CapabilitySupport.SUPPORTED

    /** True when an image may be attached. */
    val canAcceptImages: Boolean get() = imageInput == CapabilitySupport.SUPPORTED
}

/**
 * A provider's identity and transport facts. Contains no secret.
 */
data class ProviderDescriptor(
    val providerId: String,
    val displayName: String,
    val baseUrl: String,
    val auth: ProviderAuth,
    val documentationUrl: String
)

/**
 * One outbound request, expressed provider-neutrally.
 *
 * [maxTokens] is required because every supported provider needs an output cap;
 * leaving it implicit would let an unbounded request through.
 */
data class AgentRequest(
    val providerId: String,
    val modelId: String,
    val messages: List<ChatMessage>,
    val system: String?,
    val maxTokens: Int,
    val streamRequested: Boolean
) {
    /**
     * Validates the request before any network work happens.
     *
     * Returning a typed failure here is what keeps a malformed request from
     * costing the user anything.
     */
    fun validate(): AgentRequestValidation = when {
        providerId.isBlank() ->
            AgentRequestValidation.Invalid("Es ist kein Anbieter ausgewählt.")

        modelId.isBlank() ->
            AgentRequestValidation.Invalid("Es ist kein Modell ausgewählt.")

        messages.isEmpty() ->
            AgentRequestValidation.Invalid("Die Nachricht ist leer.")

        maxTokens < 1 ->
            AgentRequestValidation.Invalid("Die maximale Antwortlänge muss mindestens 1 betragen.")

        // Guard the documented safe ceiling used by the formatters.
        maxTokens > MAX_SAFE_TOKENS ->
            AgentRequestValidation.Invalid(
                "Die maximale Antwortlänge liegt über dem sicheren Bereich von $MAX_SAFE_TOKENS."
            )

        else -> AgentRequestValidation.Valid
    }

    companion object {
        /** Matches the clamp in [AnthropicMessageFormatter]. */
        const val MAX_SAFE_TOKENS = 4096
    }
}

/** Outcome of validating a request before dispatch. */
sealed class AgentRequestValidation {
    object Valid : AgentRequestValidation()
    data class Invalid(val reason: String) : AgentRequestValidation()
}

/**
 * How a request ended.
 *
 * [COMPLETED] is the only state that means "this answer is finished". Everything
 * else keeps the partial text but never claims completion.
 */
enum class AgentRunState {
    IDLE,
    CONNECTING,
    RECEIVING,
    COMPLETED,
    ABORTED,
    FAILED
}

/**
 * The result of running one request against one provider.
 *
 * [unsupported] carries the reason a capability could not be used, so the UI can
 * explain a limitation *before* the user hits it rather than failing afterwards.
 */
data class AgentRunResult(
    val state: AgentRunState,
    val text: String = "",
    val stopReason: String? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val errorMessage: String? = null,
    val unsupported: String? = null
) {
    /** True only when the provider confirmed the end of the answer. */
    val isComplete: Boolean get() = state == AgentRunState.COMPLETED

    /**
     * A capability the user asked for that this provider cannot deliver.
     * Never null-checked away: the UI is expected to show it.
     */
    val hasUnsupportedCapability: Boolean get() = unsupported != null
}

/**
 * The contract every provider adapter implements.
 *
 * Implementations must not bypass the provider's own security rules: host pinning,
 * TLS enforcement and redirect policy stay in [NetworkSecurityPolicy] and
 * [CustomEndpointSecurity], and an adapter is not permitted to weaken them.
 */
interface ProviderAgentAdapter {

    /** Identity and transport facts for this provider. */
    fun descriptor(): ProviderDescriptor

    /**
     * Resolves real capabilities for (provider, model).
     *
     * Implementations must consult documented sources and must not infer a
     * capability from the model name.
     */
    fun capabilitiesFor(modelId: String): ProviderCapabilities

    /**
     * Builds the request body for this provider.
     *
     * Must reuse the provider's own formatter so format rules stay in one place.
     */
    fun buildBody(request: AgentRequest): MessageFormatResult

    /**
     * Interprets a complete (non-streaming) response body.
     */
    fun parseResponse(body: Map<String, Any?>): MessageFormatResult

    /**
     * Human-readable, German explanation of a provider-specific quirk, or null when
     * the provider behaves like the documented default.
     */
    fun explainQuirk(quirk: ProviderQuirk): String?
}

/** Provider-specific behaviours worth surfacing to the user. */
enum class ProviderQuirk {
    /** The provider cannot stream; the UI must wait for the full answer. */
    NO_STREAMING,

    /** The provider silently caps output length. */
    SILENT_OUTPUT_CAP,

    /** The provider counts tokens differently, so estimates are rough. */
    IMPRECISE_TOKEN_COUNTING
}

/**
 * Resolves capabilities for a provider by consulting the shared capability registry.
 *
 * This is the seam that keeps provider quirks from leaking upward: an adapter
 * describes *what it is*, and the registry decides *what is confirmed*, so no
 * adapter can grant itself a capability.
 */
object ProviderCapabilityResolver {

    /**
     * Resolves [modelId] for the provider identified by [providerId].
     *
     * An unknown model yields [CapabilitySupport.UNKNOWN] across the board — never a
     * guess, and never silently reported as supported.
     */
    fun resolve(providerId: String, modelId: String): ProviderCapabilities {
        val known = ModelCapabilityRegistry.getCapabilities(modelId).isNotEmpty()
        if (!known) return allUnknown()

        fun support(capability: ModelCapability): CapabilitySupport =
            when (ModelCapabilityRegistry.check(modelId, capability)) {
                is CapabilityCheckResult.Supported -> CapabilitySupport.SUPPORTED
                is CapabilityCheckResult.NotSupported -> CapabilitySupport.UNSUPPORTED
                // Inside a known model an explicit "not in the list" is unsupported;
                // the UNKNOWN branch is reserved for models we have no data on.
                is CapabilityCheckResult.Unknown -> CapabilitySupport.UNKNOWN
            }

        return ProviderCapabilities(
            streaming = support(ModelCapability.STREAMING),
            toolCalling = support(ModelCapability.TOOL_CALLING),
            imageInput = support(ModelCapability.VISION_IMAGE_INPUT),
            systemInstructions = support(ModelCapability.SYSTEM_INSTRUCTIONS)
        )
    }

    /**
     * Explains a missing capability in German, naming the provider and model.
     *
     * Returns null when the capability is available, so a caller cannot accidentally
     * show a warning for something that works.
     */
    fun explainMissing(
        capabilities: ProviderCapabilities,
        providerId: String,
        modelId: String
    ): String? {
        val missing = buildList {
            if (!capabilities.canUseTools) add("Werkzeugaufrufe")
            if (!capabilities.canAcceptImages) add("Bildeingabe")
            if (capabilities.streaming != CapabilitySupport.SUPPORTED) add("Streaming")
        }
        if (missing.isEmpty()) return null

        return "Anbieter „$providerId“ mit Modell „$modelId“ unterstützt laut Dokumentation " +
            "keine ${missing.joinToString(", ")}. Die Funktion bleibt deshalb deaktiviert."
    }

    private fun allUnknown() = ProviderCapabilities(
        streaming = CapabilitySupport.UNKNOWN,
        toolCalling = CapabilitySupport.UNKNOWN,
        imageInput = CapabilitySupport.UNKNOWN,
        systemInstructions = CapabilitySupport.UNKNOWN
    )
}
