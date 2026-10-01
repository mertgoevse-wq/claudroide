package org.claudroide.app.feature.provider

/**
 * Task 062 — "Modell auswählen".
 *
 * Auswahl eines Modells pro Chat oder Projekt, sichtbar gemacht mit:
 * Anbieter, Modell, Fähigkeiten, Kostenhinweis und Bestätigung.
 *
 * Die Datei enthält bewusst nur reine Kotlin-Datenklassen und -Logik:
 * keine Android-Imports, keine Coroutines, kein Netzwerk. Dadurch lässt sich
 * jede Zusage hier auf der JVM ohne Emulator prüfen.
 *
 * Zwei Zusagen aus der Aufgabe werden hier strukturell abgesichert:
 *
 *  1. Ein Modellwechsel löst keine unbemerkte Datenweitergabe aus.
 *     Diese Klasse besitzt keinen Anbieter-Transport. Der Zähler
 *     [ProviderCallAudit] wird nur gelesen, nie erhöht. Ein Modellwechsel
 *     schreibt ausschließlich in die Auswahl-Tabelle dieses Objekts.
 *
 *  2. Nicht verfügbare Modelle werden nicht still ersetzt.
 *     [ModelSelectionPresenter.request] liefert bei einem nicht verfügbaren
 *     Paar aus Anbieter und Modell ein [SelectionOutcome.Rejected] mit
 *     Klartext zurück. Es gibt keinen Ersatzpfad und keinen Standardwert,
 *     der den Fehler überdecken könnte.
 */

/**
 * Zählt Anbieter-Aufrufe (Aufrufe, bei denen Daten das Gerät verlassen).
 *
 * Zweck: eine nachprüfbare Zusage. Alles, was den Anbieter aufruft, muss
 * diesen Zähler erhöhen. Wenn nach einem Modellwechsel [count] weiterhin 0 ist,
 * fand keine Datenweitergabe statt.
 *
 * [ModelSelectionPresenter] hält eine Instanz, erhöht sie aber nie selbst —
 * das ist der Punkt der Prüfung.
 */
class ProviderCallAudit(
    /** Optionaler Beobachter, der jeden Anbieter-Aufruf protokolliert. */
    private val onCall: ((String) -> Unit)? = null
) {
    var count: Int = 0
        private set

    private val log = mutableListOf<String>()

    /** Protokolliert einen Anbieter-Aufruf. Wird ausschließlich vom Netzwerk-Layer aufgerufen. */
    fun record(beschreibung: String) {
        count++
        log.add(beschreibung)
        onCall?.invoke(beschreibung)
    }

    /** Alle protokollierten Aufrufe, in Reihenfolge. */
    fun protokoll(): List<String> = log.toList()

    fun zuruecksetzen() {
        count = 0
        log.clear()
    }
}

/**
 * Wo eine Modellauswahl gilt: für einen einzelnen Chat oder für ein Projekt.
 *
 * Chat und Projekt werden getrennt gespeichert — ein Wechsel in einem Chat
 * verändert die Auswahl eines Projekts nicht.
 */
sealed class SelectionScope {

    data class Chat(val chatId: String) : SelectionScope()

    data class Project(val projectId: String) : SelectionScope()

    /** Interner Schlüssel für die Auswahl-Tabelle. */
    val key: String
        get() = when (this) {
            is Chat -> "chat:$chatId"
            is Project -> "project:$projectId"
        }

    /** Kurze deutsche Bezeichnung für Anzeigezwecke. */
    fun label(): String = when (this) {
        is Chat -> "Chat „$chatId“"
        is Project -> "Projekt „$projectId“"
    }
}

/**
 * Kostenhinweis zu einem Modell.
 *
 * Grundregel: Es wird keine Zahl erfunden. Ohne belegte Quelle bleibt der
 * Hinweis [Unknown]; eine geschätzte oder erfundene Angabe gibt es nicht.
 */
sealed class CostHint {

    /** Keine bestätigten Preise vorhanden. */
    data class Unknown(val reason: String) : CostHint()

    /** Ein Preis, der auf einer dokumentierten Quelle beruht. */
    data class Documented(val text: String, val sourceUrl: String) : CostHint()

    companion object {
        /**
         * Erzeugt einen dokumentierten Hinweis. Leere Quelle oder leerer Text
         * führen zu [Unknown] — eine Halbangabe wäre eine erfundene.
         */
        fun documented(text: String, sourceUrl: String): CostHint =
            if (text.isBlank() || sourceUrl.isBlank()) {
                Unknown("Für dieses Modell liegen keine bestätigten Preise vor.")
            } else {
                Documented(text.trim(), sourceUrl.trim())
            }
    }
}

/** Deutscher Anzeigetext für einen Kostenhinweis. */
fun costHintText(hint: CostHint): String = when (hint) {
    is CostHint.Unknown -> "Kosten: unbekannt. ${hint.reason}"
    is CostHint.Documented -> "Kosten: ${hint.text} (Quelle: ${hint.sourceUrl})"
}

/**
 * Prüft, ob ein Paar aus Anbieter und Modell benutzt werden darf.
 *
 * Liefert `null`, wenn es benutzt werden darf, sonst einen deutschen
 * Begründungstext. Es gibt bewusst keine Funktion, die ein Ersatzmodell
 * zurückgibt — die Aufgabe verbietet stilles Ersetzen.
 */
fun interface ModelAvailabilitySource {
    fun unavailabilityReason(providerId: String, modelId: String): String?
}

/**
 * Standardprüfung gegen den Anbieter- und Modellkatalog des Projekts.
 *
 * Ein Modell gilt nur als verfügbar, wenn der Anbieter bekannt, aktiv und
 * das Modell für genau diesen Anbieter eingetragen ist.
 */
object CatalogModelAvailability : ModelAvailabilitySource {

    override fun unavailabilityReason(providerId: String, modelId: String): String? {
        if (providerId.isBlank()) {
            return "Die Anbieter-ID darf nicht leer sein. Es wurde kein Modell gesetzt."
        }
        if (modelId.isBlank()) {
            return "Die Modell-ID darf nicht leer sein. Es wurde kein Modell gesetzt."
        }

        val provider = ProviderCatalogRegistry.getProvider(providerId)
            ?: return "Anbieter „$providerId“ ist nicht bekannt. " +
                "Es wurde kein anderes Modell verwendet."

        if (!provider.isEnabled) {
            return "Anbieter „${provider.displayName}“ ist derzeit abgeschaltet. " +
                "Es wurde kein anderes Modell verwendet."
        }

        val offered = ModelRegistry.getModelsForProvider(providerId).any { it.id == modelId }
        if (!offered) {
            return "Modell „$modelId“ wird von „${provider.displayName}“ nicht angeboten. " +
                "Es wurde kein anderes Modell verwendet."
        }
        return null
    }
}

/**
 * Ein vom Nutzer gewähltes Modell, wie es in der Auswahl-Tabelle steht.
 */
data class ModelChoice(
    val providerId: String,
    val modelId: String,
    val providerDisplayName: String,
    val capabilities: List<ModelCapability>,
    val isVerified: Boolean,
    val costHint: CostHint
)

/**
 * Ein noch nicht geltender Wechsel. Solange ein Wechsel hier steht, ist
 * im Chat oder Projekt nichts geändert.
 */
data class PendingModelSwitch(
    val scope: SelectionScope,
    val previousModelId: String?,
    val choice: ModelChoice,
    val capabilityLabels: List<String>,
    val costHintText: String,
    val summaryLines: List<String>,
    val changeWarning: String
) {
    /** Deutsche Kurzfrage, die vor dem Anwenden zu beantworten ist. */
    fun confirmationQuestion(): String =
        "Modell für ${scope.label()} wirklich auf „${choice.modelId}“ " +
            "(${choice.providerDisplayName}) setzen?"
}

/**
 * Ein geltendes Modell für einen Chat oder ein Projekt.
 */
data class ActiveSelection(
    val scope: SelectionScope,
    val choice: ModelChoice,
    val changeNote: String
) {
    val modelId: String get() = choice.modelId
    val providerId: String get() = choice.providerId
    val costHint: CostHint get() = choice.costHint
}

/** Zustand eines Chat oder Projekts. */
sealed class SelectionState {
    object Idle : SelectionState()
    data class PendingSwitch(val proposal: PendingModelSwitch) : SelectionState()
    data class Active(val selection: ActiveSelection) : SelectionState()
}

/** Ergebnis eines Auswahl- oder Bestätigungsschritts. */
sealed class SelectionOutcome {
    /** Wechsel wartet auf Bestätigung. Es wurde nichts geändert. */
    data class Pending(val proposal: PendingModelSwitch) : SelectionOutcome()

    /** Wechsel wurde ausgeführt. */
    data class Applied(val selection: ActiveSelection) : SelectionOutcome()

    /**
     * Wechsel wurde abgelehnt. [reason] erklärt in deutscher Sprache, warum.
     * Es wurde kein Ersatzmodell gewählt und nichts gesendet.
     */
    data class Rejected(val reason: String) : SelectionOutcome()
}

/** Beobachtungspunkt für die Oberfläche: Zustand plus Anbieter-Aufrufzähler. */
data class SelectionSnapshot(
    val scope: SelectionScope,
    val state: SelectionState,
    val providerCallCount: Int
) {
    val isPending: Boolean get() = state is SelectionState.PendingSwitch
    val isActive: Boolean get() = state is SelectionState.Active
    val isIdle: Boolean get() = state is SelectionState.Idle

    /** Geltendes Modell, oder `null`, wenn noch keines gesetzt ist. */
    val activeModelId: String? get() = (state as? SelectionState.Active)?.selection?.modelId

    /** Wartendes Modell, oder `null`, wenn kein Wechsel offen ist. */
    val pendingModelId: String? get() = (state as? SelectionState.PendingSwitch)?.proposal?.choice?.modelId
}

/** Ein Listen-Eintrag der Modellauswahl: Anbieter, Modell, Fähigkeiten, Kostenhinweis. */
data class ModelOptionRow(
    val providerId: String,
    val providerDisplayName: String,
    val modelId: String,
    val displayName: String,
    val capabilityLabels: List<String>,
    val costHintText: String,
    val isAvailable: Boolean,
    val unavailabilityReason: String?
) {
    /** Deutsche Zeile für die Oberfläche. */
    fun displayLine(): String {
        val faehigkeiten = if (capabilityLabels.isEmpty()) {
            "keine Fähigkeiten bekannt"
        } else {
            capabilityLabels.joinToString(", ")
        }
        val zustand = if (isAvailable) "verfügbar" else "nicht verfügbar"
        return "$displayName — $providerDisplayName — $zustand — Fähigkeiten: $faehigkeiten — $costHintText"
    }
}

/**
 * Vermittelt die Modellauswahl pro Chat oder Projekt.
 *
 * Garantien:
 *  - Ein Wechsel wird erst nach [confirm] geltend; vorher steht er nur als
 *    [PendingModelSwitch] im Zustand.
 *  - [request] prüft die Verfügbarkeit und lehnt bei Bedarf mit Klartext ab.
 *  - Kein Pfad dieser Klasse ruft einen Anbieter auf. [providerCallCount]
 *    bleibt darum 0.
 *  - Kostenangaben stammen ausschließlich aus dem übergebenen Katalog.
 */
class ModelSelectionPresenter(
    private val availability: ModelAvailabilitySource = CatalogModelAvailability,
    private val selectionManager: ModelSelectionManager = ModelSelectionManager(),
    /** Preisangaben je Modell-ID. Leer bedeutet: für alle Modelle unbekannt. */
    private val costCatalog: Map<String, CostHint> = emptyMap(),
    private val callAudit: ProviderCallAudit = ProviderCallAudit()
) {

    private data class ScopeState(
        val state: SelectionState = SelectionState.Idle,
        val history: List<String> = emptyList(),
        /**
         * The selection that was in force before a [SelectionState.PendingSwitch]
         * was opened. Kept so that [cancel] can restore it — the pending proposal
         * replaces [state], so the previous choice would otherwise be lost.
         */
        val selectionBeforePending: ActiveSelection? = null
    )

    private val scopes = mutableMapOf<String, ScopeState>()

    // ── Auswahl anfordern ─────────────────────────────────────────────────────

    /**
     * Nimmt einen Wechselwunsch entgegen.
     *
     * Liefert [SelectionOutcome.Pending], wenn der Wunsch zulässig ist — dann
     * ist noch nichts geändert. Liefert [SelectionOutcome.Rejected], wenn das
     * Modell nicht verfügbar ist; es wird nichts gesetzt und nichts ersetzt.
     */
    fun request(
        scope: SelectionScope,
        providerId: String,
        modelId: String
    ): SelectionOutcome {
        val reason = availability.unavailabilityReason(providerId, modelId)
        if (reason != null) {
            // Ablehnung: der bestehende Zustand bleibt unangetastet.
            return SelectionOutcome.Rejected(reason)
        }

        // Fähigkeiten und Wechsel-Warnung kommen aus dem bestehenden Manager.
        val requested = selectionManager.requestModelChange(
            providerId = providerId,
            newModelId = modelId,
            currentModelId = activeSelection(scope)?.modelId
        )
        if (requested is ModelSelectionResult.Invalid) {
            return SelectionOutcome.Rejected(requested.reason)
        }
        val pending = requested as ModelSelectionResult.RequiresConfirmation

        val providerName = ProviderCatalogRegistry.getProvider(providerId)?.displayName ?: providerId
        val costHint = costHintFor(modelId)
        val labels = pending.newSelection.capabilities.map { ModelCapabilityRegistry.label(it) }

        val proposal = PendingModelSwitch(
            scope = scope,
            previousModelId = activeSelection(scope)?.modelId,
            choice = ModelChoice(
                providerId = providerId,
                modelId = modelId,
                providerDisplayName = providerName,
                capabilities = pending.newSelection.capabilities,
                isVerified = pending.newSelection.isVerified,
                costHint = costHint
            ),
            capabilityLabels = labels,
            costHintText = costHintText(costHint),
            summaryLines = buildSummaryLines(providerName, modelId, labels, costHint, pending.newSelection.isVerified),
            changeWarning = pending.warningText
        )

        val existing = scopes[scope.key] ?: ScopeState()
        scopes[scope.key] = existing.copy(
            state = SelectionState.PendingSwitch(proposal),
            // Remember the model in force, so cancel() can put it back.
            // `state` wraps the selection in SelectionState.Active — casting the
            // state itself to ActiveSelection never matches, which would silently
            // drop the previous model and leave the scope Idle on cancel.
            selectionBeforePending = (existing.state as? SelectionState.Active)?.selection
                ?: existing.selectionBeforePending
        )

        return SelectionOutcome.Pending(proposal)
    }

    // ── Bestätigen und Abbrechen ──────────────────────────────────────────────

    /**
     * Bestätigt den wartenden Wechsel und macht ihn geltend.
     * Ohne offenen Wechsel passiert nichts.
     */
    fun confirm(scope: SelectionScope): SelectionOutcome {
        val current = scopes[scope.key] ?: return SelectionOutcome.Rejected(noPendingReason(scope))
        val proposal = (current.state as? SelectionState.PendingSwitch)?.proposal
            ?: return SelectionOutcome.Rejected(noPendingReason(scope))

        val note = buildChangeNote(proposal)
        val selection = ActiveSelection(scope = scope, choice = proposal.choice, changeNote = note)

        scopes[scope.key] = current.copy(
            state = SelectionState.Active(selection),
            history = current.history + note,
            selectionBeforePending = null
        )
        return SelectionOutcome.Applied(selection)
    }

    /**
     * Verwirft einen wartenden Wechsel. Das zuvor geltende Modell bleibt erhalten.
     */
    fun cancel(scope: SelectionScope): Boolean {
        val current = scopes[scope.key] ?: return false
        if (current.state !is SelectionState.PendingSwitch) return false
        val restored = current.selectionBeforePending
        scopes[scope.key] = current.copy(
            state = restored?.let { SelectionState.Active(it) } ?: SelectionState.Idle,
            selectionBeforePending = null
        )
        return true
    }

    // ── Abfragen ──────────────────────────────────────────────────────────────

    fun state(scope: SelectionScope): SelectionState = scopes[scope.key]?.state ?: SelectionState.Idle

    fun activeSelection(scope: SelectionScope): ActiveSelection? =
        (scopes[scope.key]?.state as? SelectionState.Active)?.selection

    fun pendingSwitch(scope: SelectionScope): PendingModelSwitch? =
        (scopes[scope.key]?.state as? SelectionState.PendingSwitch)?.proposal

    /** Zustand und Anbieter-Aufrufzähler zusammen — die Nachweise für die Oberfläche. */
    fun snapshot(scope: SelectionScope): SelectionSnapshot =
        SelectionSnapshot(scope, state(scope), providerCallCount())

    /** Sichtbare Liste der bisherigen Wechsel für einen Chat oder ein Projekt. */
    fun changeHistory(scope: SelectionScope): List<String> = scopes[scope.key]?.history ?: emptyList()

    /**
     * Anzahl der Anbieter-Aufrufe. Nach jedem Modellwechsel muss hier 0 stehen:
     * ein Wechsel überträgt keine Daten.
     */
    fun providerCallCount(): Int = callAudit.count

    fun callAudit(): ProviderCallAudit = callAudit

    // ── Listen für die Modellauswahl ──────────────────────────────────────────

    /**
     * Zeilen für die Auswahlliste: Anbieter, Modell, Fähigkeiten, Kostenhinweis.
     * Zeigt auch nicht verfügbare Modelle — mit Begründung statt ausgeblendet.
     */
    fun optionRows(providerId: String): List<ModelOptionRow> {
        val provider = ProviderCatalogRegistry.getProvider(providerId)
        val providerName = provider?.displayName ?: providerId
        return ModelRegistry.getModelsForProvider(providerId).map { descriptor ->
            val reason = availability.unavailabilityReason(providerId, descriptor.id)
            val hint = costHintFor(descriptor.id)
            ModelOptionRow(
                providerId = providerId,
                providerDisplayName = providerName,
                modelId = descriptor.id,
                displayName = descriptor.displayName,
                capabilityLabels = knownCapabilityLabels(descriptor.id),
                costHintText = costHintText(hint),
                isAvailable = reason == null,
                unavailabilityReason = reason
            )
        }
    }

    // ── Innere Helfer ─────────────────────────────────────────────────────────

    private fun costHintFor(modelId: String): CostHint = costCatalog[modelId]
        ?: CostHint.Unknown("Für dieses Modell liegen keine bestätigten Preise vor.")

    private fun knownCapabilityLabels(modelId: String): List<String> =
        ModelCapabilityRegistry.getCapabilities(modelId).map { ModelCapabilityRegistry.label(it.capability) }

    private fun noPendingReason(scope: SelectionScope): String =
        "Für ${scope.label()} wartet kein Wechsel auf Bestätigung. Es wurde nichts geändert."

    private fun buildSummaryLines(
        providerName: String,
        modelId: String,
        labels: List<String>,
        costHint: CostHint,
        isVerified: Boolean
    ): List<String> {
        val lines = mutableListOf<String>()
        lines += "Anbieter: $providerName"
        lines += "Modell: $modelId"
        lines += "Fähigkeiten: " + if (labels.isEmpty()) {
            "nicht bekannt"
        } else {
            labels.joinToString(", ")
        }
        lines += costHintText(costHint)
        lines += "Verifiziert: " + if (isVerified) {
            "ja, Eintrag im geprüften Katalog"
        } else {
            "nein, freie Eingabe"
        }
        lines += "Es werden jetzt keine Daten übertragen. " +
            "Erst Ihre nächste gesendete Nachricht geht an den Anbieter."
        return lines
    }

    private fun buildChangeNote(proposal: PendingModelSwitch): String {
        val from = proposal.previousModelId
        val was = if (from != null) "von „$from“" else "erstmals"
        return "${proposal.scope.label()}: Modell $was auf „${proposal.choice.modelId}“ " +
            "(${proposal.choice.providerDisplayName}) gewechselt. " +
            "Beim Wechsel wurden keine Daten übertragen."
    }
}