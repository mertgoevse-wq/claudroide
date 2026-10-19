package org.claudroide.app.feature.agent

/**
 * Task 080 — „Anbieterfunktionen abgleichen“.
 *
 * Ziel: Agentenfunktionen an nachweisbare Fähigkeiten des aktuellen Modells
 * anpassen.
 *
 * **Belegte Grundlage** (abgerufen am 2026-10-02, siehe [CAPABILITY_DOC_URL]):
 * - Anthropic, „Models overview“: *„All current models support text and image input,
 *   text output, multilingual capabilities, vision, and tool use.“* — **und** dieselbe
 *   Seite listet in ihrer Fähigkeitstabelle eine Zelle *„Not supported“*. Fähigkeiten
 *   sind also **nicht** über alle Modelle gleich, und „alle Modelle können X“ ist keine
 *   Aussage über ein einzelnes Modell.
 * - Dieselbe Seite: *„You can query model capabilities and token limits programmatically
 *   with the Models API. The response includes max_input_tokens, max_tokens, and a
 *   capabilities object for every available model.“*
 *
 * Daraus folgt die Arbeitsregel dieser Klasse, und sie ist der Kern der Aufgabe:
 * **Eine Fähigkeit gilt erst dann als vorhanden, wenn sie belegt ist.** Deshalb
 * [ProviderCapabilityProfile.isVerified] — ohne Nachweis ist [AgentFeature] nicht
 * erfüllt, und [ProviderCapabilityPolicy.check] meldet das als
 * [CapabilityDecision.Unverified], nicht als „geht schon“. Ein stilles
 * „wird schon passen“ wäre hier genau der Fehler, den die Aufgabe verhindern will.
 *
 * **Drei Zusagen strukturell abgesichert:**
 *
 *  1. **Kein stiller Anbieterwechsel.** [ProviderCapabilityPolicy] enthält keine
 *     Methode, die einen Anbieter oder ein Modell wechselt, und keine
 *     [CapabilityDecision]-Variante trägt ein Ziellager. Fehlt eine Fähigkeit, ist
 *     das eine **Meldung**, kein Anlass zum Wechseln. Ein Wechsel bleibt allein der
 *     Weg über `ModelFallbackPolicy` (Aufgabe 061) mit eigener Zustimmung.
 *
 *  2. **Der Ersatzweg berücksichtigt neue Freigaben und Kosten.** Fehlt eine Fähigkeit,
 *     nennt [CapabilityDecision.Workaround] den Ersatzweg **nur** zusammen mit
 *     [Workaround.requiresFreshApproval] und [Workaround.costNoticeLines]. Beide Felder
 *     sind Pflicht im Konstruktor. Damit kann es keinen Ersatzweg geben, der eine
 *     Dateiänderung oder eine kostenpflichtige Anfrage auslöst, ohne dass die
 *     entsprechende Freigabe bzw. der Kostenhinweis vorher sichtbar war.
 *
 *  3. **Fehlende Rechte werden nicht durch eine andere Anmeldung umgangen.** Es gibt
 *     im ganzen Typ **kein Feld für eine andere Anmeldung, ein anderes Konto oder
 *     einen anderen Zugang**. [CapabilityDecision.Unsupported] kann das nicht
 *     ausdrücken, also kann es nicht vorkommen. Die betroffene Funktion bleibt
 *     abgeschaltet — [ProviderCapabilityPolicy.check] liefert dafür
 *     [CapabilityDecision.Unsupported], und `run` wird nicht aufgerufen.
 *
 * Reines Kotlin: keine Datei, kein Netzwerk, kein Anbieter. Die echte Abfrage
 * liefert der Aufrufer; diese Klasse prüft nur, was gemeldet wurde.
 */
object ProviderCapabilityEvidence {

    /**
     * Quelle der Fähigkeitsaussagen, geprüft am 2026-10-02.
     *
     * Bewusst als Konstante im Code statt als Zahl im Kommentar: Eine Aussage über
     * Anbieterfähigkeiten altert, und ein alter Beleg soll als alter Beleg auffallen.
     */
    const val CAPABILITY_DOC_URL: String =
        "https://platform.claude.com/docs/en/models/overview"

    /** Das Datum, an dem [CAPABILITY_DOC_URL] geprüft wurde. */
    const val CAPABILITY_DOC_VERIFIED: String = "2026-10-02"
}

/** Ein Eingabeformat, das ein Modell verstehen kann. */
enum class InputFormat(val germanLabel: String) {

    TEXT("Text"),

    /** Bilder. Laut [ProviderCapabilityEvidence.CAPABILITY_DOC_URL] von allen aktuellen Claude-Modellen unterstützt. */
    IMAGE("Bild"),

    /** PDF-Dokumente. */
    PDF("PDF-Dokument")
}

/** Welche Werkzeuge und Formate eine Agentenfunktion braucht. */
enum class AgentFeature(
    val germanLabel: String,
    val requiredToolNames: Set<String>,
    val requiredInputFormats: Set<InputFormat> = emptySet()
) {
    /** Dateien lesen, Ordner auflisten, suchen. */
    FILE_READING("Dateien lesen und suchen", setOf("read_file", "list_directory", "search_text")),

    /** Dateien schreiben und löschen. */
    FILE_EDITS("Dateien ändern", setOf("write_file", "delete_file")),

    /** Tests ausführen. */
    TEST_RUNNING("Tests ausführen", setOf("run_test")),

    /** Bilder ansehen. */
    IMAGE_REVIEW("Bilder ansehen", emptySet(), setOf(InputFormat.IMAGE)),

    /** PDF-Dokumente ansehen. */
    PDF_REVIEW("PDF-Dokumente ansehen", emptySet(), setOf(InputFormat.PDF))
}

/**
 * Was über ein Modell **belegt** bekannt ist.
 *
 * @property supportedTools die nachweislich vorhandenen Werkzeugnamen. Verglichen
 *           wird **exakt** und ohne Beachtung der Schreibweise, damit ein Modell,
 *           das `Write_File` meldet, nicht fälschlich als abwesend gilt — und
 *           ebenso umgekehrt.
 * @property supportedFormats die nachweislich unterstützten Eingabeformate.
 * @property maxInputTokens die belegte Größe des Eingabefensters, `null` wenn unbekannt.
 * @property isVerified `true`, wenn diese Angaben aus einer belastbaren Quelle
 *           stammen. **Ohne diesen Nachweis zählt keine Fähigkeit als vorhanden.**
 */
data class ProviderCapabilityProfile(
    val providerId: String,
    val modelId: String,
    val supportedTools: Set<String> = emptySet(),
    val supportedFormats: Set<InputFormat> = emptySet(),
    val maxInputTokens: Long? = null,
    val isVerified: Boolean = true
) {
    init {
        require(providerId.isNotBlank()) { "Ein Profil braucht eine Anbieterkennung." }
        require(modelId.isNotBlank()) { "Ein Profil braucht eine Modellkennung." }
        if (maxInputTokens != null) {
            require(maxInputTokens > 0L) { "Ein Eingabefenster ist größer als null." }
        }
    }

    private val normalizedTools: Set<String> = supportedTools.map { it.trim().lowercase() }.toSet()

    /** Ist dieses Werkzeug nachweislich vorhanden? */
    fun supportsTool(toolName: String): Boolean =
        isVerified && toolName.trim().lowercase() in normalizedTools

    /** Ist dieses Format nachweislich vorhanden? */
    fun supportsFormat(format: InputFormat): Boolean = isVerified && format in supportedFormats

    /** Die Werkzeuge, die [feature] braucht und die hier fehlen. */
    fun missingToolsFor(feature: AgentFeature): List<String> {
        if (!isVerified) return feature.requiredToolNames.sorted()
        return feature.requiredToolNames.filter { it.trim().lowercase() !in normalizedTools }.sorted()
    }

    /** Die Formate, die [feature] braucht und die hier fehlen. */
    fun missingFormatsFor(feature: AgentFeature): List<InputFormat> {
        if (!isVerified) return feature.requiredInputFormats.sortedBy { it.name }
        return feature.requiredInputFormats.filter { it !in supportedFormats }.sortedBy { it.name }
    }

    /** Kann [feature] mit diesem Modell ausgeführt werden? */
    fun supports(feature: AgentFeature): Boolean =
        missingToolsFor(feature).isEmpty() && missingFormatsFor(feature).isEmpty()

    /** Die eine Zeile für die Anzeige. */
    fun profileLine(): String {
        val stand = if (isVerified) {
            "belegt"
        } else {
            "nicht belegt"
        }
        val fenster = maxInputTokens?.let { "$it Tokens" } ?: "Fenstergröße unbekannt"
        return "Modell $modelId bei $providerId — Fähigkeiten $stand, $fenster."
    }
}

/**
 * Ein Ersatzweg für eine fehlende Fähigkeit.
 *
 * @property germanDescription was der Ersatzweg ist.
 * @property requiresFreshApproval `true`, wenn der Ersatzweg eine **neue** Freigabe
 *           braucht, die im bisherigen Auftrag nicht enthalten war. Beim Anlegen ohne
 *           dieses Wissen ist das Feld nicht `false`, sondern der Konstruktor
 *           verlangt eine ausdrückliche Angabe.
 * @property costNoticeLines der Kostenhinweis, **zwingend gefüllt**, sobald der
 *           Ersatzweg Geld kostet.
 */
data class Workaround(
    val germanDescription: String,
    val requiresFreshApproval: Boolean,
    val isFree: Boolean,
    val costNoticeLines: List<String> = emptyList()
) {
    init {
        require(germanDescription.isNotBlank()) { "Ein Ersatzweg braucht eine Beschreibung." }
        if (!isFree) {
            require(costNoticeLines.isNotEmpty()) {
                "Ein Ersatzweg, der etwas kostet, braucht einen sichtbaren Kostenhinweis."
            }
        }
    }
}

/** Das Ergebnis des Fähigkeitsabgleichs. */
sealed interface CapabilityDecision {

    /** Die Funktion kann ausgeführt werden. */
    data class Ready(val feature: AgentFeature) : CapabilityDecision

    /**
     * Die Fähigkeiten sind **nicht belegt**.
     *
     * Getrennt von [Unsupported], weil hier nichts **fehlt**, sondern nichts
     * **wissbar** ist. Beides führt zum selben Ergebnis — die Funktion bleibt
     * abgeschaltet — aber aus zwei verschiedenen Gründen.
     */
    data class Unverified(
        val feature: AgentFeature,
        val profile: ProviderCapabilityProfile,
        val germanLines: List<String>
    ) : CapabilityDecision

    /**
     * Die Fähigkeit fehlt nachweislich.
     *
     * Trägt bewusst **kein** Feld für einen anderen Anbieter, ein anderes Modell oder
     * eine andere Anmeldung. Was fehlt, wird gemeldet; ein Umweg über eine andere
     * Anmeldung ist hier nicht ausdrückbar.
     */
    data class Unsupported(
        val feature: AgentFeature,
        val profile: ProviderCapabilityProfile,
        val missingToolNames: List<String>,
        val missingFormats: List<InputFormat>,
        val germanLines: List<String>
    ) : CapabilityDecision

    /**
     * Die Funktion fehlt, es gibt aber einen Ersatzweg.
     *
     * Der Ersatzweg bringt seine eigenen Freigaben und seinen Kostenhinweis mit. Er
     * wird dem Nutzer **vorgelegt**, nicht ausgeführt.
     */
    data class WorkaroundAvailable(
        val feature: AgentFeature,
        val workaround: Workaround,
        val germanLines: List<String>
    ) : CapabilityDecision

    /** Darf die Funktion in dieser Lage ausgeführt werden? */
    val isExecutable: Boolean get() = this is Ready
}

/**
 * Gleicht eine [AgentFeature] gegen ein belegtes Modellprofil ab.
 *
 * @property workarounds je Funktion ein möglicher Ersatzweg. **Optional**, und
 *           bewusst ohne Vorgabe: Ein Ersatzweg, den sich der Aufrufer spontan
 *           ausdenkt, wäre genau der Weg, der eine Freigabe umgeht.
 */
class ProviderCapabilityPolicy(
    private val workarounds: Map<AgentFeature, Workaround> = emptyMap()
) {

    /**
     * Prüft [feature] gegen [profile].
     *
     * Reihenfolge: erst der Nachweis, dann die Fähigkeit, dann der Ersatzweg. Ohne
     * Nachweis wird **nicht** nach einem Ersatzweg gefragt — solange unklar ist, ob
     * die Fähigkeit fehlt, wäre jede Ersatzvorschlagsliste eine Behauptung.
     */
    fun check(feature: AgentFeature, profile: ProviderCapabilityProfile): CapabilityDecision {
        if (!profile.isVerified) {
            return CapabilityDecision.Unverified(
                feature = feature,
                profile = profile,
                germanLines = buildList {
                    add(
                        "„${feature.germanLabel}“ ist für Modell ${profile.modelId} bei " +
                            "${profile.providerId} nicht belegt."
                    )
                    add(
                        "Belegt ist nur: Fähigkeiten, die der Anbieter ausdrücklich " +
                            "meldet. Solange das fehlt, bleibt die Funktion abgeschaltet."
                    )
                    add("Belegquelle: ${ProviderCapabilityEvidence.CAPABILITY_DOC_URL}")
                }
            )
        }

        val missingTools = profile.missingToolsFor(feature)
        val missingFormats = profile.missingFormatsFor(feature)
        if (missingTools.isNotEmpty() || missingFormats.isNotEmpty()) {
            val lines = buildList {
                add(
                    "„${feature.germanLabel}“ kann mit Modell ${profile.modelId} bei " +
                        "${profile.providerId} nicht ausgeführt werden."
                )
                if (missingTools.isNotEmpty()) {
                    add("  Fehlende Werkzeuge: ${missingTools.joinToString(", ")}")
                }
                missingFormats.forEach { add("  Fehlendes Format: ${it.germanLabel}") }

                val workaround = workarounds[feature]
                if (workaround == null) {
                    add("  Es gibt keinen Ersatzweg.")
                } else {
                    add("  Ersatzweg: ${workaround.germanDescription}")
                    if (workaround.requiresFreshApproval) {
                        add("  Der Ersatzweg braucht eine neue Freigabe des Nutzers.")
                    }
                    if (!workaround.isFree) {
                        workaround.costNoticeLines.forEach { add("  $it") }
                    }
                }
                add(
                    "Es wird kein anderer Anbieter und keine andere Anmeldung " +
                        "verwendet. Ein Wechsel ist nur über den Ersatzweg mit " +
                        "eigener Zustimmung möglich."
                )
            }

            return if (workarounds.containsKey(feature)) {
                CapabilityDecision.WorkaroundAvailable(
                    feature = feature,
                    workaround = workarounds.getValue(feature),
                    germanLines = lines
                )
            } else {
                CapabilityDecision.Unsupported(
                    feature = feature,
                    profile = profile,
                    missingToolNames = missingTools,
                    missingFormats = missingFormats,
                    germanLines = lines
                )
            }
        }

        return CapabilityDecision.Ready(feature)
    }

    /** Bequemer Zugriff auf die fertige Meldung. */
    fun reportLines(feature: AgentFeature, profile: ProviderCapabilityProfile): List<String> =
        when (val decision = check(feature, profile)) {
            is CapabilityDecision.Ready -> listOf(
                "„${feature.germanLabel}“ ist belegt und ausführbar. ${profile.profileLine()}"
            )
            is CapabilityDecision.Unverified -> decision.germanLines
            is CapabilityDecision.Unsupported -> decision.germanLines
            is CapabilityDecision.WorkaroundAvailable -> decision.germanLines
        }
}
