package org.claudroide.app.feature.provider

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Task 041 — "Anbieter und Kosten anzeigen".
 *
 * Builds the German status text that says, before and after a request, which
 * provider and which model are used and — only where it can be backed by a
 * source — what the request may cost.
 *
 * Rules this file enforces (Task 041 "Fertig, wenn" and "Schutz"):
 *  - A money amount is rendered ONLY when a source URL *and* an ISO-8601
 *    verification date are available. Otherwise no number is shown at all.
 *  - Unknown cost never renders as "kostenlos", "gratis", "0 USD" or an empty
 *    string. It renders as an explicit "unbekannt".
 *  - Every amount carries the word "Schätzung" and the reminder that only the
 *    provider's billing decides the real amount. Never an invoice.
 *  - Prices and capabilities are never derived from the spelling of a model
 *    name. A model that is not in [CostEstimator.PRICE_TABLE] has unknown cost,
 *    no matter how familiar its name looks.
 *  - No provider, model or cost text passes through unredacted, so no secret can
 *    reach a status line or a diagnostics dump (Task 041 "Schutz").
 *
 * Pure and synchronous: no Android types, no coroutines, no I/O.
 */

/** Text used whenever a fact is not documented. Never replaced by a guess. */
const val UNKNOWN_DE: String = "unbekannt"

/**
 * One fully redacted status line set about a request.
 *
 * `amountText` is null exactly when no honest number exists. Callers must render
 * [headline] and [lines] and must not substitute a fallback amount.
 */
data class CostLabel(
    /** One-line summary, always German, always redacted. */
    val headline: String,
    /** Detail lines, redacted, safe to show and to log. */
    val lines: List<String>,
    /** The formatted amount, or null when nothing may be shown. */
    val amountText: String?,
    /** Provenance of the amount, or null when there is none. */
    val sourceUrl: String?,
    /** ISO-8601 verification date, or null when there is none. */
    val verifiedDate: String?,
    /** Freshness of the underlying price, or null when no price was consulted. */
    val freshness: PriceFreshness?,
    /** Reasons why the estimate is not a total. Empty for a complete estimate. */
    val missing: List<MissingReason>
) {
    /** True only when a number is actually present. */
    val hasAmount: Boolean get() = amountText != null

    /** Full text for the status area. */
    fun toDisplayText(): String = (listOf(headline) + lines).joinToString("\n")
}

/** Pure formatter for provider, model and cost text. */
object CostLabelPresenter {

    /** Appended to every shown amount. */
    const val ESTIMATE_WORD: String = "Schätzung"

    /** Shown instead of any amount when nothing can be backed by a source. */
    const val UNKNOWN_COST_TEXT: String = "Kosten unbekannt"

    private val COST_LINE =
        "Kosten: $ESTIMATE_WORD, kein Rechnungsbetrag. " +
            "Den echten Betrag meldet erst die Abrechnung des Anbieters."

    private val UNKNOWN_PROVIDER_LINE =
        "Anbieter: $UNKNOWN_DE — es ist kein Anbieter hinterlegt."

    private val UNKNOWN_MODEL_LINE =
        "Modell: $UNKNOWN_DE — es ist kein Modell ausgewählt."

    /** True when [text] is an ISO-8601 calendar date. */
    fun isIsoDate(text: String): Boolean =
        try {
            LocalDate.parse(text)
            true
        } catch (_: RuntimeException) {
            false
        }

    /** Redacts [text] before it is shown or logged. */
    fun safe(text: String): String = SecretMasker.redact(text)

    /**
     * Builds the provider line.
     *
     * A blank or redacted-away provider name becomes [UNKNOWN_DE]; it is never
     * guessed from the model.
     */
    fun providerLine(providerDisplayName: String?): String {
        val name = providerDisplayName?.let(::safe)?.trim().orEmpty()
        if (name.isEmpty()) return UNKNOWN_PROVIDER_LINE
        return "Anbieter: $name"
    }

    /**
     * True when [text] carries something the redactor would change.
     *
     * A source URL that looks like a credential is not a source, so the amount behind
     * it is not shown at all rather than shown with a masked link.
     */
    fun containsSecretLike(text: String): Boolean = SecretMasker.containsSecretLikeText(text)

    /**
     * Builds the model line.
     *
     * The origin is shown so the user can see whether the model comes from the
     * provider's own catalogue or was typed in by hand. A hand-entered model has
     * no documented price unless it happens to be in [CostEstimator.PRICE_TABLE].
     */
    fun modelLine(modelId: String?, origin: ModelOrigin?): String {
        val id = modelId?.let(::safe)?.trim().orEmpty()
        if (id.isEmpty()) return UNKNOWN_MODEL_LINE
        val shown = origin?.labelDe ?: UNKNOWN_DE
        return "Modell: $id (Herkunft: $shown)"
    }

    /**
     * Converts an [CostEstimate] into a displayable [CostLabel].
     *
     * The amount survives only if the estimate carries a usable source URL and a
     * parseable ISO date. If either is missing, the label shows the reasons and
     * no number at all.
     */
    fun costLabel(estimate: CostEstimate): CostLabel {
        val lines = ArrayList<String>()
        for (reason in estimate.missing) lines.add(reason.germanText)
        lines.add(estimate.freshness.germanLabel)

        val source = estimate.sourceUrl?.let(::safe)?.trim()?.takeIf { it.isNotEmpty() }
        val date = estimate.verifiedDate?.let(::safe)?.trim()?.takeIf { it.isNotEmpty() }
        val provenanceOk = source != null && source.startsWith("http") &&
            date != null && isIsoDate(date) && !containsSecretLike(source)

        val amount = estimate.amountUsd
        if (amount == null || !provenanceOk) {
            // Always say why: a withheld number without a reason looks like a bug, and
            // "no amount" must be distinguishable from "no price known at all".
            if (!provenanceOk) lines.add(COST_PROVENANCE_WARNING)
            return CostLabel(
                headline = "$UNKNOWN_COST_TEXT — kein belegter Preis.",
                lines = lines,
                amountText = null,
                sourceUrl = null,
                verifiedDate = null,
                freshness = estimate.freshness,
                missing = estimate.missing
            )
        }

        lines.add("Preisquelle: $source")
        lines.add("Preis geprüft am: $date")
        if (estimate is CostEstimate.PartialEstimate) lines.add(PARTIAL_NOTE)
        lines.add(COST_LINE)

        return CostLabel(
            headline = "Geschätzte Kosten: ${CostEstimator.formatUsd(amount)} ($ESTIMATE_WORD)",
            lines = lines,
            amountText = CostEstimator.formatUsd(amount),
            sourceUrl = source,
            verifiedDate = date,
            freshness = estimate.freshness,
            missing = estimate.missing
        )
    }

    /**
     * Complete status text for one request: provider, model, cost.
     *
     * Everything is redacted, so this string may also go into diagnostics.
     */
    fun statusText(
        providerDisplayName: String?,
        modelId: String?,
        origin: ModelOrigin?,
        estimate: CostEstimate?
    ): String {
        val label = if (estimate == null) {
            CostLabel(
                headline = "$UNKNOWN_COST_TEXT — es wurde keine Anfrage gestellt.",
                lines = listOf(UNKNOWN_PROVIDER_LINE, UNKNOWN_MODEL_LINE),
                amountText = null,
                sourceUrl = null,
                verifiedDate = null,
                freshness = null,
                missing = emptyList()
            )
        } else {
            costLabel(estimate)
        }
        return (listOf(
            providerLine(providerDisplayName),
            modelLine(modelId, origin),
            label.headline
        ) + label.lines).joinToString("\n")
    }

    private const val COST_PROVENANCE_WARNING =
        "Der Preis hat keine belegte Quelle und kein Prüfdatum — deshalb wird kein Betrag angezeigt."

    private const val PARTIAL_NOTE =
        "Nur ein Teil der Verbrauchsdaten ist bekannt — der Betrag ist eine Untergrenze."
}