package org.claudroide.app.feature.provider

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Parses an ISO-8601 date such as "2026-10-01"; null when it is not one.
 * A malformed date is treated as missing, never as "today".
 */
internal fun parseIsoDate(text: String): LocalDate? = try {
    LocalDate.parse(text.trim())
} catch (e: DateTimeParseException) {
    null
} catch (e: Exception) {
    null
}

/**
 * Task 064 — "Kostenschätzung".
 *
 * Purpose: show an approximate expected cost BEFORE the user sends a request.
 *
 * Honesty rules baked into this file (Task 064 acceptance criteria):
 *  - Every price carries a source URL and an ISO verification date. A price without
 *    both is not a price — it is a guess, and guesses never reach the user.
 *  - Every numeric result is labelled "Schätzung". An exact invoice is only ever
 *    claimed when the provider reports one, which this estimator never does.
 *  - A missing or stale price produces an explicit "cannot estimate" answer.
 *    Nothing is ever substituted by a plausible number.
 *  - The estimator is completely OFFLINE. It reads a local table only. It performs
 *    no network request of any kind — measuring cost must never cost money or leak
 *    data without the user knowing (Task 064 "Schutz").
 *  - Arithmetic is [BigDecimal] throughout. `0.1 + 0.2` style float artefacts never
 *    reach the display.
 */

/** One documented price point, with provenance. */
data class ModelPrice(
    val modelId: String,
    /** USD per 1,000,000 input tokens. */
    val inputUsdPerMillionTokens: BigDecimal,
    /** USD per 1,000,000 output tokens. */
    val outputUsdPerMillionTokens: BigDecimal,
    /** Where the number was read. Must be an http(s) URL. */
    val sourceUrl: String,
    /** ISO-8601 date of the last manual check against [sourceUrl], e.g. "2026-10-01". */
    val verifiedDate: String
) {
    /** Whether [verifiedDate] is a parseable ISO-8601 date. */
    val hasUsableVerifiedDate: Boolean
        get() = parseIsoDate(verifiedDate) != null

    /** Whether [sourceUrl] is a real linkable source. */
    val hasUsableSource: Boolean
        get() = sourceUrl.startsWith("https://") || sourceUrl.startsWith("http://")

    /** Whether this price entry is complete enough to be used at all. */
    val isUsable: Boolean
        get() = hasUsableSource && hasUsableVerifiedDate &&
            inputUsdPerMillionTokens >= BigDecimal.ZERO &&
            outputUsdPerMillionTokens >= BigDecimal.ZERO
}

/** How trustworthy the provenance of a price is at the time of use. */
enum class PriceFreshness(val germanLabel: String) {
    /** Source and date present, within the re-check window. */
    CURRENT("Preisquelle aktuell"),

    /** Source and date present, but older than the re-check window. */
    STALE("Preisquelle veraltet"),

    /** No usable source or no parseable date — provenance is missing. */
    UNVERIFIED("Preisquelle nicht belegt")
}

/** Why a cost estimate could not be completed. */
enum class MissingReason(val germanText: String) {
    UNKNOWN_MODEL("Modell steht nicht im Preisverzeichnis"),
    PRICE_WITHOUT_SOURCE("Preis hat keine Quellen-URL"),
    PRICE_WITHOUT_DATE("Preis hat kein Prüfdatum"),
    INPUT_TOKENS_UNKNOWN("Anzahl der Eingabe-Tokens unbekannt"),
    OUTPUT_TOKENS_UNKNOWN("Anzahl der Ausgabe-Tokens unbekannt")
}

/**
 * A token-count forecast. Either an explicit number, or `null` for "not known".
 * `null` is never turned into a default value.
 */
data class TokenForecast(
    val inputTokens: Int?,
    val outputTokens: Int?,
    /** Plain-German statements about how the numbers were obtained. */
    val assumptions: List<String>
) {
    companion object {
        /** Nothing known. */
        fun unknown(): TokenForecast = TokenForecast(null, null, emptyList())

        /** Exact counts, e.g. replaying a measured conversation. */
        fun measured(inputTokens: Int, outputTokens: Int): TokenForecast =
            TokenForecast(inputTokens, outputTokens, listOf("Gemessene Tokenzahl."))
    }
}

/**
 * Turns a character count into a token count.
 *
 * This is a documented rough heuristic, not a measurement, and it is always
 * reported as an assumption rather than as a fact.
 */
object UsageForecast {

    /** Rough average used by English and German text alike. */
    const val CHARACTERS_PER_TOKEN: Int = 4

    private const val HEURISTIC_NOTE =
        "Annahme: rund 4 Zeichen pro Token. Grobe Schätzung, kein Messwert."

    fun fromCharacterCounts(inputCharacters: Int, outputCharacters: Int): TokenForecast =
        TokenForecast(
            inputTokens = charactersToTokens(inputCharacters),
            outputTokens = charactersToTokens(outputCharacters),
            assumptions = listOf(HEURISTIC_NOTE)
        )

    private fun charactersToTokens(characters: Int): Int? =
        if (characters < 0) null
        else (characters + CHARACTERS_PER_TOKEN - 1) / CHARACTERS_PER_TOKEN
}

/** One priced component of an estimate. */
data class CostPart(
    val germanLabel: String,
    val tokens: Int,
    val usd: BigDecimal
)

/**
 * Result of a cost estimation.
 *
 * The three subclasses are deliberately distinct — a complete estimate, an
 * incomplete one ("at least this much"), and "no estimate possible" must never be
 * collapsed into a single number.
 */
sealed class CostEstimate {

    abstract val modelId: String
    abstract val freshness: PriceFreshness
    /** Null whenever no honest number exists. */
    abstract val amountUsd: BigDecimal?
    abstract val sourceUrl: String?
    abstract val verifiedDate: String?
    abstract val headline: String
    abstract val detailLines: List<String>

    /**
     * What could not be determined. Empty for a complete [Estimate].
     *
     * Declared on the base class so a caller can ask "what is missing?" about any
     * outcome without first matching on the subclass.
     */
    abstract val missing: List<MissingReason>

    /** True when a number is shown at all. */
    val hasAmount: Boolean get() = amountUsd != null

    /**
     * The full German text for the pre-send cost hint.
     *
     * Always contains "Schätzung" and always states that this is not an invoice.
     */
    fun toDisplayText(): String {
        val lines = ArrayList<String>()
        lines.add(headline)
        lines.addAll(detailLines)
        lines.add(INVOICE_NOTE)
        return lines.joinToString("\n")
    }

    companion object {
        /**
         * Appended to every result. The provider — not this app — decides the real
         * amount, and only after billing.
         */
        const val INVOICE_NOTE =
            "Schätzung, keine Rechnung: Den tatsächlichen Betrag meldet erst die " +
                "Abrechnung des Anbieters."
    }

    /** Complete estimate: both token counts and both prices are known. */
    data class Estimate(
        override val modelId: String,
        val inputPart: CostPart,
        val outputPart: CostPart,
        override val freshness: PriceFreshness,
        override val sourceUrl: String,
        override val verifiedDate: String,
        override val headline: String = "Geschätzte Kosten: ",
        override val detailLines: List<String>
    ) : CostEstimate() {
        override val amountUsd: BigDecimal = inputPart.usd + outputPart.usd

        /** A complete estimate has nothing missing. */
        override val missing: List<MissingReason> = emptyList()
    }

    /**
     * Partial estimate: only some components are known. The amount is a lower
     * bound ("mindestens"), never a total.
     */
    data class PartialEstimate(
        override val modelId: String,
        val knownParts: List<CostPart>,
        override val missing: List<MissingReason>,
        override val freshness: PriceFreshness,
        override val sourceUrl: String?,
        override val verifiedDate: String?,
        override val headline: String,
        override val detailLines: List<String>
    ) : CostEstimate() {
        override val amountUsd: BigDecimal =
            knownParts.fold(BigDecimal.ZERO) { acc, part -> acc + part.usd }
    }

    /** No honest number exists. [amountUsd] stays null. */
    data class Incomplete(
        override val modelId: String,
        override val missing: List<MissingReason>,
        override val freshness: PriceFreshness,
        override val sourceUrl: String?,
        override val verifiedDate: String?,
        override val headline: String = "Keine Schätzung möglich.",
        override val detailLines: List<String>
    ) : CostEstimate() {
        override val amountUsd: BigDecimal? = null
    }
}

/**
 * Offline cost estimator.
 *
 * Reads a local, dated price table. Never performs I/O of any kind.
 */
object CostEstimator {

    private const val ANTHROPIC_MODELS_DOC =
        "https://platform.claude.com/docs/en/about-claude/models/overview"

    /** Date on which the entries below were last read from [ANTHROPIC_MODELS_DOC]. */
    const val VERIFIED: String = "2026-10-01"

    /** A price older than this many days must be re-checked before it is shown. */
    const val MAX_PRICE_AGE_DAYS: Long = 90L

    private val MILLION = BigDecimal("1000000")

    /** Working scale for the per-million division. */
    private const val DIVISION_SCALE = 12

    private fun usd(value: String): BigDecimal = BigDecimal(value)

    private fun anthropic(
        modelId: String,
        input: String,
        output: String
    ): ModelPrice = ModelPrice(
        modelId = modelId,
        inputUsdPerMillionTokens = usd(input),
        outputUsdPerMillionTokens = usd(output),
        sourceUrl = ANTHROPIC_MODELS_DOC,
        verifiedDate = VERIFIED
    )

    /**
     * The seed price table: Anthropic first-party API rates in USD per million
     * tokens. Unknown models are simply absent — they are never guessed.
     */
    val PRICE_TABLE: Map<String, ModelPrice> = mapOf(
        "claude-opus-5-5" to anthropic("claude-opus-5-5", "4.00", "20.00"),
        "claude-sonnet-5-5" to anthropic("claude-sonnet-5-5", "2.00", "10.00"),
        "claude-sonnet-5" to anthropic("claude-sonnet-5", "2.00", "10.00"),
        "claude-haiku-4-5" to anthropic("claude-haiku-4-5", "1.00", "5.00"),
        "claude-haiku-4-5-20251001" to anthropic("claude-haiku-4-5-20251001", "1.00", "5.00"),
        "claude-fable-5-1" to anthropic("claude-fable-5-1", "10.00", "50.00")
    )

    /** Convenience lookup, null when the model is not in the table. */
    fun priceFor(modelId: String): ModelPrice? = PRICE_TABLE[modelId.trim()]

    /**
     * Problems in the price table itself — a price without source or date, or a
     * broken date. An empty list means the table is clean.
     */
    fun validateTable(prices: Map<String, ModelPrice> = PRICE_TABLE): List<String> {
        val problems = ArrayList<String>()
        prices.forEach { (id, price) ->
            if (!price.hasUsableSource) problems.add("$id: keine http(s)-Quellen-URL")
            if (!price.hasUsableVerifiedDate) problems.add("$id: Prüfdatum nicht ISO-8601")
        }
        return problems
    }

    /**
     * Estimates the cost of one request.
     *
     * @param todayEpochDays reference date for the staleness check, injected so the
     *        logic stays pure and testable.
     * @param prices the local price table; defaults to [PRICE_TABLE].
     */
    fun estimate(
        modelId: String,
        forecast: TokenForecast,
        todayEpochDays: Long,
        prices: Map<String, ModelPrice> = PRICE_TABLE
    ): CostEstimate {
        val id = modelId.trim()
        val price = prices[id]

        if (price == null) {
            return incomplete(id, listOf(MissingReason.UNKNOWN_MODEL), PriceFreshness.UNVERIFIED)
        }

        val missingSources = ArrayList<MissingReason>()
        if (!price.hasUsableSource) missingSources.add(MissingReason.PRICE_WITHOUT_SOURCE)
        if (!price.hasUsableVerifiedDate) missingSources.add(MissingReason.PRICE_WITHOUT_DATE)

        val freshness = when {
            missingSources.isNotEmpty() -> PriceFreshness.UNVERIFIED
            ageInDays(price, todayEpochDays) > MAX_PRICE_AGE_DAYS -> PriceFreshness.STALE
            else -> PriceFreshness.CURRENT
        }

        val missingTokens = ArrayList<MissingReason>()
        if (price.hasUsableSource && price.hasUsableVerifiedDate) {
            if (forecast.inputTokens == null) missingTokens.add(MissingReason.INPUT_TOKENS_UNKNOWN)
            if (forecast.outputTokens == null) missingTokens.add(MissingReason.OUTPUT_TOKENS_UNKNOWN)
        }
        val missing = missingSources + missingTokens

        if (missingSources.isNotEmpty()) {
            // Without provenance the number would be a guess. Refuse outright.
            return incomplete(id, missing, freshness, price)
        }

        if (missingTokens.size == 2) {
            // Not one token count is known: there is no lower bound to show either.
            // "mindestens 0,00 USD" would be a number the user cannot act on.
            return incomplete(id, missing, freshness, price)
        }

        val knownParts = ArrayList<CostPart>()
        if (forecast.inputTokens != null) {
            knownParts.add(
                CostPart(
                    germanLabel = "Eingabe",
                    tokens = forecast.inputTokens,
                    usd = costOf(forecast.inputTokens, price.inputUsdPerMillionTokens)
                )
            )
        }
        if (forecast.outputTokens != null) {
            knownParts.add(
                CostPart(
                    germanLabel = "Ausgabe",
                    tokens = forecast.outputTokens,
                    usd = costOf(forecast.outputTokens, price.outputUsdPerMillionTokens)
                )
            )
        }

        if (missingTokens.isNotEmpty()) {
            // Something is missing: show the known part as a lower bound only.
            return CostEstimate.PartialEstimate(
                modelId = id,
                knownParts = knownParts,
                missing = missingTokens,
                freshness = freshness,
                sourceUrl = price.sourceUrl,
                verifiedDate = price.verifiedDate,
                headline = "Schätzung unvollständig: mindestens " +
                    "${formatUsd(knownParts.fold(BigDecimal.ZERO) { a, p -> a + p.usd })}",
                detailLines = buildDetailLines(
                    id, price, freshness, knownParts, missingTokens, forecast, todayEpochDays
                )
            )
        }

        val inputPart = knownParts[0]
        val outputPart = knownParts[1]
        val total = inputPart.usd + outputPart.usd
        return CostEstimate.Estimate(
            modelId = id,
            inputPart = inputPart,
            outputPart = outputPart,
            freshness = freshness,
            sourceUrl = price.sourceUrl,
            verifiedDate = price.verifiedDate,
            headline = "Geschätzte Kosten: ${formatUsd(total)}",
            detailLines = buildDetailLines(
                id, price, freshness, knownParts, emptyList(), forecast, todayEpochDays
            )
        )
    }

    /** Convenience overload taking a reference date as ISO text. */
    fun estimate(
        modelId: String,
        forecast: TokenForecast,
        todayIsoDate: String,
        prices: Map<String, ModelPrice> = PRICE_TABLE
    ): CostEstimate {
        val parsed = parseIsoDate(todayIsoDate)
            ?: return incomplete(
                modelId.trim(),
                listOf(MissingReason.PRICE_WITHOUT_DATE),
                PriceFreshness.UNVERIFIED
            )
        return estimate(modelId, forecast, parsed.toEpochDay(), prices)
    }

    /**
     * Cost of [tokens] at [usdPerMillion] — exact decimal division, no floats.
     *
     * `stripTrailingZeros()` alone would be wrong here: the scale-12 result for 20 USD
     * comes back as the exponential form `2E+1`, which neither compares equal to `20`
     * nor renders as a readable amount. Trimming the fraction in plain-string form and
     * re-parsing keeps the value exact and yields a non-exponential result, so 4.00
     * becomes 4 and 20.00 becomes 20.
     */
    fun costOf(tokens: Int, usdPerMillion: BigDecimal): BigDecimal {
        require(tokens >= 0) { "Tokenzahl darf nicht negativ sein" }
        val exact = BigDecimal(tokens)
            .multiply(usdPerMillion)
            .divide(MILLION, DIVISION_SCALE, RoundingMode.HALF_UP)
        return BigDecimal(trimTrailingFraction(exact.toPlainString()))
    }

    /**
     * Drops trailing zeros from the decimal fraction of a plain decimal string, then
     * a bare trailing point. `20.000000000000` becomes `20`, `0.000004` stays as is.
     * Digits left of the point are never touched, so `20` cannot collapse to `2`.
     */
    private fun trimTrailingFraction(plain: String): String {
        if (!plain.contains('.')) return plain
        val trimmed = plain.trimEnd('0').trimEnd('.')
        return if (trimmed.isEmpty() || trimmed == "-") "0" else trimmed
    }

    /** Formats an amount for a German user: up to six decimals for small amounts, two for normal,
     * comma as decimal separator. Rounds half up — never a float artefact.
     */
    fun formatUsd(amount: BigDecimal): String {
        val digits = when {
            amount.abs() < BigDecimal("0.01") -> 6
            amount.abs() < BigDecimal("0.1") -> 4
            else -> 2
        }
        return amount.setScale(digits, RoundingMode.HALF_UP)
            .toPlainString()
            .replace('.', ',') + " USD"
    }

    /** Whole days since the price was verified; -1 when the date is unusable. */
    fun ageInDays(price: ModelPrice, todayEpochDays: Long): Long {
        val verified = parseIsoDate(price.verifiedDate) ?: return -1L
        return todayEpochDays - verified.toEpochDay()
    }

    private fun incomplete(
        modelId: String,
        missing: List<MissingReason>,
        freshness: PriceFreshness,
        price: ModelPrice? = null
    ): CostEstimate.Incomplete = CostEstimate.Incomplete(
        modelId = modelId,
        missing = missing,
        freshness = freshness,
        sourceUrl = price?.sourceUrl,
        verifiedDate = price?.verifiedDate,
        detailLines = buildList {
            add("Es wird keine Zahl geraten.")
            addAll(missing.map { "Fehlt: ${it.germanText}." })
            price?.let {
                if (it.hasUsableSource) add("Preisquelle: ${it.sourceUrl}")
                else add("Preisquelle: keine belastbare Angabe vorhanden.")
            } ?: add("Preisquelle: keine belastbare Angabe vorhanden.")
        }
    )

    private fun buildDetailLines(
        modelId: String,
        price: ModelPrice,
        freshness: PriceFreshness,
        parts: List<CostPart>,
        missing: List<MissingReason>,
        forecast: TokenForecast,
        todayEpochDays: Long
    ): List<String> = buildList {
        add("Modell: $modelId")
        parts.forEach { add("${it.germanLabel}: ${it.tokens} Tokens = ${formatUsd(it.usd)}") }
        forecast.inputTokens?.let { if (parts.none { p -> p.germanLabel == "Eingabe" }) add("Eingabe: $it Tokens") }
        forecast.outputTokens?.let { if (parts.none { p -> p.germanLabel == "Ausgabe" }) add("Ausgabe: $it Tokens") }
        missing.forEach { add("Fehlt: ${it.germanText}.") }
        forecast.assumptions.forEach { add(it) }
        val age = ageInDays(price, todayEpochDays)
        add("Preisquelle: ${price.sourceUrl} (geprüft am ${price.verifiedDate})")
        if (age >= 0) add("Alter der Preisquelle: $age Tage.")
        add(freshness.germanLabel + ".")
        if (freshness == PriceFreshness.STALE) {
            add("Bitte die Preise an der Quelle prüfen, bevor du dich verlässt.")
        }
    }
}
