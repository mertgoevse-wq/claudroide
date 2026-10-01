package org.claudroide.app.feature.provider

/**
 * Task 063 — „Ersatzmodell einstellen“.
 *
 * Der Nutzer entscheidet, ob die App bei einem Fehler nachfragt oder einen
 * freigegebenen Ersatz verwendet. Standard ist immer „vorher fragen“.
 *
 * Die Datei enthält bewusst nur reine Kotlin-Datenklassen und -Logik: keine
 * Android-Importe, keine Coroutines, kein Netzwerk. Dadurch lässt sich jede
 * Zusage hier auf der JVM ohne Emulator prüfen.
 *
 * Drei Zusagen aus der Aufgabe sind strukturell abgesichert, nicht nur getestet:
 *
 *  1. **Kein automatischer Wechsel ohne ausdrückliche Einrichtung.**
 *     [ModelFallbackPolicy.decide] liefert [FallbackDecision.AskUser], solange
 *     für den Geltungsbereich kein [FallbackSetup] vorliegt. Es gibt keinen
 *     eingebauten Standard-Ersatz und keinen Weg, einen Ersatz zu raten.
 *
 *  2. **Keine stille Übertragung an einen anderen Anbieter.**
 *     Die Freigabe des Hauptanbieters gilt nicht für den Ersatzanbieter.
 *     [FallbackSetup.acknowledgedExternalTransfer] gehört zu genau dem Anbieter,
 *     der im selben Objekt steht — ein Wechsel zu einem dritten Anbieter braucht
 *     darum eine eigene Einrichtung und deren eigene Freigabe.
 *
 *  3. **Kosten und Datenregeln werden bei jeder Entscheidung neu geprüft.**
 *     Nicht einmal bei der Einrichtung, sondern bei jedem Aufruf von [decide].
 *     Ein Preis ohne Quelle blockiert den Wechsel ebenso wie ein Bedingungssatz,
 *     der älter ist als [ProviderTermsEngine.MAX_VERIFICATION_AGE_DAYS].
 */

/**
 * Wie die App auf einen Fehler reagiert.
 *
 * [ASK_FIRST] ist der Standard und der einzige Wert, den ein neuer Bereich
 * haben kann. [USE_APPROVED_FALLBACK] wird erst gültig, wenn ein
 * [FallbackSetup] vorliegt.
 */
enum class FallbackMode(val germanLabel: String) {
    ASK_FIRST("Vorher fragen"),
    USE_APPROVED_FALLBACK("Eingerichteten Ersatz verwenden")
}

/**
 * Art eines Fehlers.
 *
 * [mayFallBack] unterscheidet einen Fehler, bei dem ein Ausweichen auf einen
 * zweiten Anbieter sachlich richtig ist, von einer Ablehnung. Eine Ablehnung
 * des Hauptanbieters darf nicht umgangen werden, indem dieselbe Anfrage
 * woanders gestellt wird — deshalb ist bei diesen Fehlern kein Wechsel
 * vorgesehen, auch nicht nach einer ausdrücklichen Einrichtung.
 */
enum class FailureKind(val mayFallBack: Boolean, val germanText: String) {
    RATE_LIMIT(true, "Kontingent beim Anbieter erschöpft"),
    OVERLOADED(true, "Anbieter gerade überlastet"),
    TEMPORARY_UNAVAILABLE(true, "Anbieter vorübergehend nicht erreichbar"),
    CONNECTION_LOST(true, "Verbindung unterbrochen"),

    SAFETY_REFUSAL(false, "Inhalt vom Anbieter abgelehnt"),
    AUTHENTICATION_FAILED(false, "Zugangsdaten werden abgelehnt"),
    REQUEST_INVALID(false, "Anfrage wird abgelehnt"),
    USER_CANCELLED(false, "Abfrage durch den Nutzer beendet"),
    PROVIDER_DISABLED(false, "Anbieter ist abgeschaltet")
}

/**
 * Die ausdrückliche Einrichtung eines Ersatzmodells für genau einen Bereich
 * (Chat oder Projekt).
 *
 * Das Objekt ist die einzige Quelle der Erlaubnis. Fehlt es, gibt es keinen
 * Ersatz. Es wird nie automatisch erzeugt und nie aus einer anderen
 * Einstellung abgeleitet.
 *
 * @property scope für welchen Bereich die Einrichtung gilt.
 * @property targetProviderId Anbieter, der die Ersatzanfrage bedienen würde.
 * @property targetModelId Modell, das der Ersatzanbieter bedienen würde.
 * @property acknowledgedExternalTransfer `true` erst, wenn der Nutzer für
 *           *diesen* Anbieter ausdrücklich bestätigt hat, dass Daten das Gerät
 *           verlassen. Eine Bestätigung für den Hauptanbieter gilt hier nicht.
 * @property acceptedHigherCost `true`, wenn der Nutzer einen höheren Preis als
 *           beim Hauptmodell gesehen und in Kauf genommen hat.
 * @property establishedOnEpochDays Tag der Einrichtung, für die
 *           [ModelFallbackPolicy.SETUP_MAX_AGE_DAYS]-Frist.
 */
data class FallbackSetup(
    val scope: SelectionScope,
    val targetProviderId: String,
    val targetModelId: String,
    val acknowledgedExternalTransfer: Boolean,
    val acceptedHigherCost: Boolean,
    val establishedOnEpochDays: Long
)

/**
 * Ergebnis der erneuten Kostenprüfung für das Ersatzmodell.
 *
 * @property freshness Aktualität der Preisquelle, aus [CostEstimator].
 * @property isPriced `true`, wenn für das Ersatzmodell ein brauchbarer
 *           Preiseintrag mit Quelle und Datum vorliegt.
 * @property isMoreExpensiveThanPrimary `true`, wenn der Ersatz je Token
 *           teurer ist als das Hauptmodell. Verglichen werden die
 *           Preise je eine Million Tokens; es wird keine Umrechnung oder
 *           Gewichtung erfunden.
 * @property priceAgeInDays Alter der Preisquelle in Tagen; `-1`, wenn das
 *           Prüfdatum des Preises nicht lesbar ist. Wird als Zahl geführt und
 *           nicht aus einem Anzeigetext herausgesucht — eine Umbenennung der
 *           Oberfläche darf die Entscheidung nicht verändern.
 * @property lines deutsche Zeilen für die Oberfläche.
 */
data class CostReassessment(
    val targetModelId: String,
    val freshness: PriceFreshness,
    val isPriced: Boolean,
    val isMoreExpensiveThanPrimary: Boolean,
    val priceAgeInDays: Long,
    val lines: List<String>
)

/** Ergebnis eines Fehlers im Bezug auf einen eingerichteten Ersatz. */
sealed class FallbackDecision {

    /**
     * Die App fragt nach. Das ist der Standard und die Antwort auf jeden
     * offenen Entscheidungsbedarf.
     *
     * @property question deutscher Fragetext.
     * @property reasons warum nicht automatisch gewechselt wurde.
     * @property options deutsche Antwortmöglichkeiten.
     */
    data class AskUser(
        val question: String,
        val reasons: List<String>,
        val options: List<String>
    ) : FallbackDecision()

    /**
     * Der eingerichtete Ersatz wird verwendet. Dieser Zweig ist der einzige,
     * der Daten übertragen kann, und er entsteht nur nach vollständiger
     * Prüfung von Verfügbarkeit, Freigabe, Datenregeln und Kosten.
     *
     * @property disclosures was dem Nutzer vor der Übertragung offengelegt
     *           werden muss. Die Liste ist nie leer.
     */
    data class UseFallback(
        val target: FallbackSetup,
        val disclosures: List<String>
    ) : FallbackDecision()

    /**
     * Kein Ersatz möglich. [reason] erklärt in deutscher Sprache, warum.
     * Es wurde nichts gesendet.
     */
    data class Fail(val reason: String) : FallbackDecision()
}

/** Ergebnis eines Einrichtungs- oder Entfernungsversuchs. */
sealed class FallbackOutcome {

    /** Die Einrichtung wurde gespeichert. */
    data class Saved(val setup: FallbackSetup) : FallbackOutcome()

    /**
     * Die Einrichtung wurde abgelehnt. [reason] erklärt warum; es wurde
     * nichts gespeichert und kein Ersatz erzeugt.
     */
    data class Rejected(val reason: String) : FallbackOutcome()

    /** Die Einrichtung wurde entfernt; der Bereich fragt wieder vorher. */
    object Removed : FallbackOutcome()
}

/**
 * Liefert den Bedingungssatz eines Anbieters, oder `null`, wenn keiner
 * hinterlegt ist. Ein fehlender Satz gilt als ungeprüft und blockiert einen
 * automatischen Wechsel — nicht als Erlaubnis.
 */
fun interface ProviderTermsSource {
    fun recordFor(providerId: String): ProviderTermsRecord?
}

/**
 * Verwaltet den Ersatzmodell-Umschalter pro Chat und Projekt.
 *
 * Garantien:
 *  - Ein Bereich ohne [FallbackSetup] fragt immer nach.
 *  - Eine Ablehnung des Hauptanbieters führt nie zu einem Ausweichen, auch
 *    nicht nach einer Einrichtung.
 *  - Datenregeln und Preise werden bei jeder Entscheidung neu gelesen, nie aus
 *    der Einrichtung zwischengespeichert.
 *  - Diese Klasse besitzt keinen Anbieter-Transport. Sie erhöht
 *    [ProviderCallAudit] nie; der Zähler bleibt darum 0.
 */
class ModelFallbackPolicy(
    private val availability: ModelAvailabilitySource = CatalogModelAvailability,
    private val terms: ProviderTermsSource = ProviderTermsSource { null },
    private val callAudit: ProviderCallAudit = ProviderCallAudit()
) {

    private val setups = mutableMapOf<String, FallbackSetup>()

    /**
     * Wie lange eine Einrichtung gilt, bevor sie erneuert werden muss. Gleiche
     * Frist wie bei den Bedingungssätzen in [ProviderTermsEngine], damit beide
     * Prüfungen zusammenfallen.
     */
    companion object {
        const val SETUP_MAX_AGE_DAYS: Long = 90L
    }

    // ── Einrichtung ───────────────────────────────────────────────────────────

    /**
     * Richtet einen Ersatz ein. Das ist der einzige Weg, durch den ein Ersatz
     * entsteht — und er wird nur ausdrücklich aufgerufen.
     *
     * Der Anbieter und das Modell müssen im Katalog verfügbar sein; sonst wird
     * abgelehnt. Es wird nichts geraten und kein Ersatz erfunden.
     */
    fun configure(
        scope: SelectionScope,
        targetProviderId: String,
        targetModelId: String,
        acknowledgedExternalTransfer: Boolean,
        acceptedHigherCost: Boolean,
        todayEpochDays: Long
    ): FallbackOutcome {
        val reason = availability.unavailabilityReason(targetProviderId, targetModelId)
        if (reason != null) {
            return FallbackOutcome.Rejected(
                "Ersatzmodell nicht eingerichtet. $reason " +
                    "Es wurde kein anderer Ersatz eingesetzt."
            )
        }

        val setup = FallbackSetup(
            scope = scope,
            targetProviderId = targetProviderId,
            targetModelId = targetModelId,
            acknowledgedExternalTransfer = acknowledgedExternalTransfer,
            acceptedHigherCost = acceptedHigherCost,
            establishedOnEpochDays = todayEpochDays
        )
        setups[scope.key] = setup
        return FallbackOutcome.Saved(setup)
    }

    /**
     * Nimmt eine Einrichtung vollständig zurück. Danach fragt der Bereich
     * wieder vorher — es bleibt kein Ersatz verborgen.
     *
     * Ob etwas entfernt wurde, ändert nichts an der Zusage: der Bereich fragt
     * in beiden Fällen vorher. Der Rückgabewert bleibt deshalb [FallbackOutcome.Removed].
     */
    fun clear(scope: SelectionScope): FallbackOutcome {
        setups.remove(scope.key)
        return FallbackOutcome.Removed
    }

    fun setupFor(scope: SelectionScope): FallbackSetup? = setups[scope.key]

    /** Der Modus des Bereichs. Ohne Einrichtung immer [FallbackMode.ASK_FIRST]. */
    fun mode(scope: SelectionScope): FallbackMode =
        if (setups.containsKey(scope.key)) FallbackMode.USE_APPROVED_FALLBACK else FallbackMode.ASK_FIRST

    // ── Entscheidung ──────────────────────────────────────────────────────────

    /**
     * Reagiert auf einen Fehler.
     *
     * @param primaryProviderId Anbieter, der die eigentliche Anfrage stellte.
     * @param primaryModelId Modell, das die eigentliche Anfrage stellte.
     * @param failure Art des Fehlers.
     * @param todayEpochDays Referenztag für Preis- und Bedingungsprüfung.
     */
    fun decide(
        scope: SelectionScope,
        primaryProviderId: String,
        primaryModelId: String,
        failure: FailureKind,
        todayEpochDays: Long
    ): FallbackDecision {
        // Eine Ablehnung wird nicht umgangen. Das gilt auch dann, wenn ein
        // Ersatz eingerichtet ist — sonst wäre die Ablehnung des einen Anbieters
        // nur ein Hindernis auf dem Weg zu einem anderen.
        if (!failure.mayFallBack) {
            return FallbackDecision.Fail(
                "${failure.germanText}. Es wird nicht automatisch ein anderer " +
                    "Anbieter versucht. Sie können ein Modell selbst auswählen."
            )
        }

        val setup = setups[scope.key]
            ?: return askUser(scope, "Es ist kein Ersatzmodell eingerichtet.")

        // Ein Wechsel auf genau das Modell, das gerade gescheitert ist, ist kein
        // Ersatz. Er würde nur dasselbe erneut versuchen und dabei Kosten und
        // Kontingent verbrauchen, ohne ein anderes Modell zu erreichen. Also
        // nachfragen — auch wenn dafür eine Einrichtung existiert.
        if (setup.targetProviderId == primaryProviderId &&
            setup.targetModelId == primaryModelId
        ) {
            return askUser(
                scope,
                "Der eingerichtete Ersatz ist dasselbe Modell „$primaryModelId“, " +
                    "das gerade fehlgeschlagen ist. Ein erneuter Versuch ist eine " +
                    "eigene Entscheidung."
            )
        }

        val blocked = mutableListOf<String>()

        val age = todayEpochDays - setup.establishedOnEpochDays
        if (age > SETUP_MAX_AGE_DAYS) {
            blocked += "Die Einrichtung des Ersatzmodells ist $age Tage alt und " +
                "muss erneuert werden."
        }

        if (!setup.acknowledgedExternalTransfer) {
            blocked += "Sie haben nicht bestätigt, dass Daten an " +
                "„${displayName(setup.targetProviderId)}“ das Gerät verlassen dürfen. " +
                "Es wurde nichts übertragen."
        }

        val unavailable = availability.unavailabilityReason(
            setup.targetProviderId,
            setup.targetModelId
        )
        if (unavailable != null) {
            blocked += "Der eingerichtete Ersatz ist nicht mehr verfügbar. $unavailable"
        }

        val termsStatus = termsStatus(setup.targetProviderId, todayEpochDays)
        if (termsStatus !is ProviderTermsEngineStatus.Usable) {
            blocked += termsStatus.reason
        }

        val cost = reassessCosts(
            primaryModelId = primaryModelId,
            targetModelId = setup.targetModelId,
            todayEpochDays = todayEpochDays
        )
        if (!cost.isPriced || cost.freshness == PriceFreshness.UNVERIFIED) {
            blocked += "Für „${setup.targetModelId}“ liegt kein belegter Preis vor. " +
                "Es wird auf dieser Grundlage nicht automatisch gewechselt."
        } else if (cost.freshness == PriceFreshness.STALE) {
            blocked += "Die Preisquelle für „${setup.targetModelId}“ ist " +
                "${cost.priceAgeInDays} Tage alt und muss geprüft werden."
        } else if (cost.isMoreExpensiveThanPrimary && !setup.acceptedHigherCost) {
            blocked += "„${setup.targetModelId}“ ist je Token teurer als " +
                "„$primaryModelId“. Sie haben einen höheren Preis nicht in Kauf genommen."
        }

        if (blocked.isNotEmpty()) {
            return askUser(scope, blocked.first(), blocked)
        }

        val providerName = displayName(setup.targetProviderId)
        val disclosures = buildList {
            add("Ersatzmodell: ${setup.targetModelId} ($providerName).")
            add("Anlass: ${failure.germanText}.")
            addAll(cost.lines)
            add("Datenregeln von „$providerName“ wurden geprüft: " + termsStatus.reason)
            add("Daten verlassen das Gerät und gehen an „$providerName“. " +
                "Das Hauptmodell „$primaryModelId“ wird für diese Anfrage nicht benutzt.")
        }
        return FallbackDecision.UseFallback(setup, disclosures)
    }

    /**
     * Prüft Anbieterkosten und Datenregeln erneut. Wird bei jeder Entscheidung
     * aufgerufen, nie aus einer früheren Einrichtung übernommen.
     */
    fun reassessCosts(
        primaryModelId: String,
        targetModelId: String,
        todayEpochDays: Long
    ): CostReassessment {
        val target = CostEstimator.priceFor(targetModelId)
        val primary = CostEstimator.priceFor(primaryModelId)
        val estimate = CostEstimator.estimate(
            modelId = targetModelId,
            forecast = TokenForecast.unknown(),
            todayEpochDays = todayEpochDays
        )

        val isPriced = target != null && target.isUsable
        val moreExpensive = if (isPriced && primary != null && primary.isUsable) {
            target.totalRate() > primary.totalRate()
        } else {
            false
        }

        val lines = buildList {
            add("Preis „$targetModelId“: " + estimate.headline)
            estimate.sourceUrl?.let { add("Preisquelle: $it") }
            estimate.verifiedDate?.let { add("Geprüft am $it.") }
            add(estimate.freshness.germanLabel + ".")
        }

        return CostReassessment(
            targetModelId = targetModelId,
            freshness = estimate.freshness,
            isPriced = isPriced,
            isMoreExpensiveThanPrimary = moreExpensive,
            priceAgeInDays = target?.let { CostEstimator.ageInDays(it, todayEpochDays) } ?: -1L,
            lines = lines
        )
    }

    /**
     * Anzahl der Anbieter-Aufrufe. Muss nach jeder Entscheidung 0 bleiben:
     * die Politik entscheidet, sie überträgt nichts.
     */
    fun providerCallCount(): Int = callAudit.count

    fun callAudit(): ProviderCallAudit = callAudit

    // ── Innere Helfer ─────────────────────────────────────────────────────────

    private fun askUser(
        scope: SelectionScope,
        reason: String,
        reasons: List<String> = listOf(reason)
    ): FallbackDecision.AskUser {
        // Nur wenn wirklich ein Ersatz eingetragen ist, darf ein Angebot ihn
        // benennen. Ohne Einrichtung gibt es kein Ersatzmodell — die Auswahl
        // darf keinen Namen erfinden.
        val retryOption = setupFor(scope)?.targetModelId
            ?.let { "Beim Modell „$it“ erneut versuchen" }
            ?: "Beim bisherigen Modell erneut versuchen"
        return FallbackDecision.AskUser(
            question = "Wie möchten Sie mit dem Fehler umgehen?",
            reasons = reasons,
            options = listOf(
                retryOption,
                "Ein anderes Modell selbst auswählen",
                "Abbrechen"
            )
        )
    }

    private fun displayName(providerId: String): String =
        ProviderCatalogRegistry.getProvider(providerId)?.displayName ?: providerId

    private fun termsStatus(providerId: String, todayEpochDays: Long): ProviderTermsEngineStatus {
        val record = terms.recordFor(providerId)
            ?: return ProviderTermsEngineStatus.Unverified(
                "Die Bedingungen von „${displayName(providerId)}“ sind nicht hinterlegt."
            )
        return when (ProviderTermsEngine.evaluateVerificationStatus(record, todayEpochDays)) {
            VerificationStatus.VERIFIED_CURRENT ->
                ProviderTermsEngineStatus.Usable("geprüft am ${record.lastVerifiedDate}")
            VerificationStatus.NEEDS_REVERIFICATION ->
                ProviderTermsEngineStatus.Unverified(
                    "Die Bedingungen von „${displayName(providerId)}“ sind älter als " +
                        "${ProviderTermsEngine.MAX_VERIFICATION_AGE_DAYS} Tage."
                )
            VerificationStatus.COMMUNITY_UNVERIFIED ->
                ProviderTermsEngineStatus.Unverified(
                    "„${displayName(providerId)}“ ist kein geprüfter offizieller Zugang."
                )
            VerificationStatus.DEPRECATED ->
                ProviderTermsEngineStatus.Unverified(
                    "Der Zugang zu „${displayName(providerId)}“ ist im Projekt als " +
                        "veraltet eingestuft."
                )
        }
    }
}

/** Ergebnis der Bedingungsprüfung eines Anbieters. */
sealed class ProviderTermsEngineStatus {

    /** Deutscher Begründungstext in beiden Fällen, damit Aufrufer nicht casten müssen. */
    abstract val reason: String

    /** Die Bedingungen sind belegt und aktuell. */
    data class Usable(override val reason: String) : ProviderTermsEngineStatus()

    /** Die Bedingungen fehlen oder sind zu alt. [reason] erklärt in Deutsch. */
    data class Unverified(override val reason: String) : ProviderTermsEngineStatus()
}

/** Summe aus Eingabe- und Ausgabepreis je eine Million Tokens. */
private fun ModelPrice.totalRate() = inputUsdPerMillionTokens + outputUsdPerMillionTokens
