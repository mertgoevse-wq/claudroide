package org.claudroide.app.feature.provider

/**
 * Task 125 — „Datenschutz je Anbieter“.
 *
 * Ziel: Vor der Verbindung und vor der Anfrage erklären, welcher Dienst
 * welche Daten erhält — mit Quelle, möglicher Speicherung, Region, soweit
 * belegt, und einem Prüfdatum.
 *
 * Die Aufgabe verlangt ausdrücklich, **Unbekanntes als offen zu markieren**.
 * Diese Klasse ist deshalb so gebaut, dass eine unbelegte Angabe gar nicht
 * als Tatsache ausgedrückt werden kann:
 *
 *  1. **Jede Angabe trägt ihre Quelle und ihr Prüfdatum.** [PrivacyFact] führt
 *     [sourceUrl] und [verifiedOn] mit. Ein Wert ohne Beleg ist kein
 *     [PrivacyFact], sondern [UnknownFact] — es gibt keinen Weg, ihn als
 *     bekannt zu deklarieren, ohne eine Quelle zu erfinden.
 *
 *  2. **Unbekanntes bleibt sichtbar.** [ProviderPrivacyProfile.unknownFacts]
 *     sammelt alles, was nicht belegt ist, und die Anzeige nennt es. Es gibt
 *     kein Feld, das „unbekannt" als Wert annimmt und dabei wie eine
 *     Tatsache aussieht: eine Aussage über Speicherung oder Region ist immer
 *     entweder ein [PrivacyFact] mit Quelle oder gar keine.
 *
 *  3. **Vergleich ist möglich, aber nicht irreführend.** [ProviderPrivacyProfile.comparison]
 *     stellt Anbieter gegenüber und kennzeichnet jede **fehlende** Angabe als
 *     „offen". Zwei Anbieter mit gleich vielen bekannten Fakten sehen nicht
 *     gleich aus, wenn beim einen die Speicherung unbekannt ist — genau
 *     deshalb steht „offen" dort und nicht ein leeres Feld.
 *
 *  4. **Keine vertraulichen Daten, bevor Ziel und Umfang bekannt sind.**
 *     [PrivacyEnforcement.canSend] verlangt, dass der Nutzer das Profil
 *     bestätigt hat und der Umfang benannt ist. Ohne das ist das Senden
 *     blockiert — auch wenn alle Fakten bekannt wären. Das ist die Kernzusage
 *     aus „Schutz" und sie sitzt am Aufruf, nicht in einer Anweisung.
 *
 * Reines Kotlin: kein Netz, kein Android. Die Fakten kommen aus der
 * Anbieterdokumentation; diese Klasse hält fest, was belegt ist, und ist damit
 * auf der JVM prüfbar.
 */

/** Wie gut eine Datenschutzangabe belegt ist. */
enum class FactBasis(val germanLabel: String) {

    /** Aus der offiziellen Dokumentation des Anbieters, mit Datum. */
    OFFICIAL_DOCUMENTATION("offizielle Dokumentation"),

    /** Aus einer allgemeinen branchenüblichen Angabe ohne anbieterspezifischen Beleg. */
    GENERAL_ASSUMPTION("allgemeine Annahme"),

    /** Nicht belegt — die Angabe ist offen. */
    UNKNOWN("offen")
}

/**
 * Eine belegte Datenschutzangabe.
 *
 * Ohne [sourceUrl] und [verifiedOn] ist keine Angabe belegbar; beides sind
 * Pflichtfelder, damit eine Aussage nicht ohne Herkunft dastehen kann.
 */
data class PrivacyFact(
    val statement: String,
    val sourceUrl: String,
    val verifiedOn: String
) {
    init {
        require(statement.isNotBlank()) { "Eine Angabe braucht einen Text." }
        require(sourceUrl.isNotBlank()) { "Eine belegte Angabe braucht ihre Quelle." }
        require(verifiedOn.isNotBlank()) { "Eine belegte Angabe braucht ein Prüfdatum." }
    }

    val basis: FactBasis = FactBasis.OFFICIAL_DOCUMENTATION
}

/** Die Art der Angabe, um die es geht. */
enum class PrivacyTopic(val germanLabel: String) {

    /** Wird die Eingabe gespeichert? */
    RETENTION("Speicherung der Eingabe"),

    /** In welcher Region wird verarbeitet? */
    DATA_REGION("Verarbeitungsregion"),

    /** Wird die Eingabe zum Trainieren verwendet? */
    MODEL_TRAINING("Verwendung zum Training"),

    /** Wie lange bleibt eine Anfrage zurückverfolgbar? */
    LOG_RETENTION("Aufbewahrung der Protokolle"),

    /** Wird eine Weitergabe an Dritte beschrieben? */
    THIRD_PARTY_SHARING("Weitergabe an Dritte")
}

/** Eine **nicht** belegte Angabe — sichtbar, aber ohne Behauptung. */
data class UnknownFact(
    val topic: PrivacyTopic,

    /** Was genau an diesem Anbieter unbekannt ist. */
    val detail: String
) {
    /** Die Zeile für die Oberfläche — sie sagt „offen", nicht „nein". */
    fun displayLine(): String = "${topic.germanLabel}: offen — $detail"
}

/**
 * Das Datenschutzprofil eines Anbieters.
 *
 * @property providerId der Anbieter aus [ProviderCatalogRegistry].
 * @property known die belegten Angaben.
 * @property unknown die offenen Angaben. Was hier steht, ist **nicht**
 *        bekannt — es wird nicht geraten und nicht als „wahrscheinlich"
 *        ausgegeben.
 */
data class ProviderPrivacyProfile(
    val providerId: String,
    val displayName: String,
    val known: List<PrivacyFact>,
    val unknown: List<UnknownFact>
) {
    init {
        require(providerId.isNotBlank()) { "Ein Profil braucht einen Anbieter." }
        require(displayName.isNotBlank()) { "Ein Profil braucht einen Namen." }
    }

    /** Ist zu diesem Anbieter etwas offen? */
    val hasOpenQuestions: Boolean get() = unknown.isNotEmpty()

    /**
     * Ist dieses Profil vollständig belegt?
     *
     * Nur dann darf die App eine unbedenkliche Aussage über den Anbieter
     * treffen. Solange [hasOpenQuestions] gilt, ist die ehrliche Aussage
     * „ungeklärt".
     */
    val isFullyDocumented: Boolean get() = unknown.isEmpty()

    /** Die belegte Angabe zu einem Thema, oder `null`, wenn sie offen ist. */
    fun factFor(topic: PrivacyTopic): PrivacyFact? =
        known.firstOrNull { it.statement.startsWith(topic.germanLabel) }

    /** Die Zeilen für die Oberfläche. */
    fun explanationLines(): List<String> = buildList {
        add("Datenschutzprofil: $displayName")
        if (known.isEmpty()) {
            add("Zu diesem Anbieter liegt keine belegte Angabe vor.")
        } else {
            known.forEach { add("  ${it.statement} (Quelle: ${it.sourceUrl}, geprüft ${it.verifiedOn})") }
        }
        if (hasOpenQuestions) {
            add("Offen:")
            unknown.forEach { add("  ${it.displayLine()}") }
        }
    }
}

/** Das Ergebnis eines Vergleichs zweier Anbieter. */
data class ProviderPrivacyComparison(
    val rows: List<ComparisonRow>
) {
    /** Gibt es einen Anbieter, bei dem etwas offen ist? */
    val hasOpenQuestions: Boolean get() = rows.any { it.valueB == ComparisonValue.OPEN }
}

/** Eine Zeile des Vergleichs: ein Thema, Wert A und Wert B. */
data class ComparisonRow(
    val topic: PrivacyTopic,
    val valueA: ComparisonValue,
    val valueB: ComparisonValue
)

/** Ein Vergleichswert. „OFFEN" ist ein vollwertiger Wert, kein Leerfeld. */
enum class ComparisonValue(val germanLabel: String) {
    STORED("gespeichert"),
    NOT_STORED("nicht gespeichert"),
    REGION_KNOWN("Region bekannt"),
    REGION_UNKNOWN("Region offen"),
    NOT_USED_FOR_TRAINING("nicht zum Training"),
    MAY_BE_USED_FOR_TRAINING("evtl. zum Training"),
    OPEN("offen");

    val isOpen: Boolean get() = this == OPEN || this == REGION_UNKNOWN
}

/**
 * Vergleicht zwei Anbieterprofil[e] Datenschutz-Angelegenheiten.
 *
 * Jede **fehlende** Angabe wird als [ComparisonValue.OPEN] ausgewiesen, nicht
 * als leeres Feld: Zwei Anbieter mit gleich vielen bekannten Angaben sehen
 * sonst gleich aus, obwohl bei einem die Speicherung unbekannt ist. Genau
 * deshalb trägt [ComparisonValue.isOpen] die Aussage „hier weiß die App es
 * nicht".
 */
fun compareProviders(
    profileA: ProviderPrivacyProfile,
    profileB: ProviderPrivacyProfile
): ProviderPrivacyComparison {
    val rows = PrivacyTopic.entries.map { topic ->
        val wertA = wertFuer(topic, profileA)
        val wertB = wertFuer(topic, profileB)
        ComparisonRow(topic, wertA, wertB)
    }
    return ProviderPrivacyComparison(rows)
}

/**
 * Übersetzt eine belegte Angabe in einen Vergleichswert.
 *
 * Eine offene Frage zum Thema schlägt jede vorhandene Angabe: Solange die
 * Frage offen ist, ist der Vergleichswert **offen**, nicht „gespeichert".
 */
private fun wertFuer(topic: PrivacyTopic, profile: ProviderPrivacyProfile): ComparisonValue {
    if (profile.unknown.any { it.topic == topic }) return ComparisonValue.OPEN

    val belegt = profile.known.firstOrNull { it.statement.startsWith(topic.germanLabel) }
        ?: return ComparisonValue.OPEN

    val text = belegt.statement.lowercase()
    return when (topic) {
        PrivacyTopic.RETENTION, PrivacyTopic.LOG_RETENTION ->
            if ("nicht gespeichert" in text || "nicht gespeichert" in text) ComparisonValue.NOT_STORED
            else if ("gespeichert" in text) ComparisonValue.STORED
            else ComparisonValue.OPEN

        PrivacyTopic.DATA_REGION ->
            if ("unbekannt" in text) ComparisonValue.REGION_UNKNOWN
            else ComparisonValue.REGION_KNOWN

        PrivacyTopic.MODEL_TRAINING ->
            if ("nicht" in text) ComparisonValue.NOT_USED_FOR_TRAINING
            else ComparisonValue.MAY_BE_USED_FOR_TRAINING

        PrivacyTopic.THIRD_PARTY_SHARING ->
            if ("nicht" in text) ComparisonValue.NOT_STORED
            else ComparisonValue.MAY_BE_USED_FOR_TRAINING
    }
}

/** Das Ergebnis der Prüfung vor dem Senden. */
sealed interface PrivacyCheck {

    /** Senden erlaubt: Ziel und Umfang sind dem Nutzer bekannt. */
    data class MaySend(val scopeLines: List<String>) : PrivacyCheck

    /** Es stehen noch offene Fragen offen — der Nutzer muss sie kennen. */
    data class OpenQuestions(val questions: List<String>) : PrivacyCheck

    /** Der Nutzer hat das Ziel und den Umfang nicht bestätigt. */
    data class NotAcknowledged(val explanationLines: List<String>) : PrivacyCheck
}

/**
 * Die Regel, was vor dem Senden vertraulicher Daten gelten muss.
 *
 * Getrennt vom Profil, weil es eine andere Frage beantwortet: Das Profil sagt,
 * **was** der Anbieter tut; diese Klasse sagt, ob der Nutzer das **weiß**.
 */
object PrivacyEnforcement {

    /**
     * Darf eine vertrauliche Anfrage an [profile] gehen?
     *
     * @param acknowledged hat der Nutzer Ziel **und** Umfang bestätigt?
     * @param scopeLines die benannten Daten, die gesendet würden.
     *
     * Ohne Bestätigung wird **nicht** gesendet, auch wenn das Profil
     * vollständig belegt ist: Die App behauptet nicht, dass der Nutzer
     * informiert ist, wenn er es nicht bestätigt hat. Offene Fragen im Profil
     * blockieren ebenfalls — sie stehen der Aussage entgegen, der Nutzer
     * kenne Ziel und Umfang.
     */
    fun canSend(
        profile: ProviderPrivacyProfile,
        acknowledged: Boolean,
        scopeLines: List<String>
    ): PrivacyCheck = when {
        !acknowledged -> PrivacyCheck.NotAcknowledged(
            listOf(
                "Der Nutzer hat Ziel und Umfang nicht bestätigt. Es wird nichts gesendet.",
                "Vor dem Senden muss bekannt sein, welcher Dienst welche Daten erhält."
            )
        )
        scopeLines.isEmpty() -> PrivacyCheck.NotAcknowledged(
            listOf(
                "Der Umfang ist nicht benannt. Es wird nichts gesendet.",
                "Es muss genau benannt sein, welche Dateien und Nachrichten gehen."
            )
        )
        profile.hasOpenQuestions -> PrivacyCheck.OpenQuestions(
            profile.unknown.map { it.displayLine() }
        )
        else -> PrivacyCheck.MaySend(scopeLines)
    }
}

/** Der Katalog der Datenschutzprofile — bewusst zunächst leer. */
object ProviderPrivacyRegistry {

    /**
     * Baut das Profil eines Anbieters aus belegten Fakten und offenen Fragen.
     *
     * Die Trennung ist der Kern: [known] nimmt nur [PrivacyFact] an (mit
     * Quelle und Datum), [unknown] nimmt nur [UnknownFact]. Es gibt keinen Weg,
     * eine unbelegte Vermutung in [known] einzutragen — der Konstruktor von
     * [PrivacyFact] verlangt Quelle und Datum.
     */
    fun profile(
        providerId: String,
        displayName: String,
        known: List<PrivacyFact>,
        unknown: List<UnknownFact> = emptyList()
    ): ProviderPrivacyProfile =
        ProviderPrivacyProfile(providerId, displayName, known, unknown)
}