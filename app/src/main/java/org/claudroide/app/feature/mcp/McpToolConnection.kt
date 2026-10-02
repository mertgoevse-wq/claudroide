package org.claudroide.app.feature.mcp

import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 134 — die Typen des Verbindungskatalogs.
 *
 * Diese Datei **beschreibt** eine Verbindung, sie verbindet nichts: kein Netz,
 * kein Socket, kein MCP-Prozess, kein Installieren. Die Regeln stehen in
 * [McpToolConnectionPolicy], der Katalog in [McpToolConnectionRegistry].
 *
 * Die tragende Eigenschaft des Satzes „Keine fremden MCP-Server starten oder
 * Schlüssel senden ohne Nutzerfreigabe" ist hier die **Form der Konstruktoren**:
 *
 *  * [McpToolConnection] hat einen privaten Konstruktor. Niemand außerhalb dieser
 *    Datei baut einen Katalogeintrag; der einzige Weg führt über
 *    [McpToolConnection.of], und der entscheidet dort, was mit der Anmeldung
 *    passiert.
 *  * [ConnectionApproval] hat einen privaten Konstruktor. Niemand außerhalb
 *    dieser Datei baut eine Freigabe; der einzige Weg führt über
 *    [ConnectionApproval.record] — und der prüft zuerst, ob eine Nutzerentscheidung
 *    **dokumentiert** vorliegt.
 *
 * Genau deshalb sind es `private` und nicht `internal`: Ein `internal`-Konstruktor
 * wäre in demselben Modul für jede Datei aufrufbar, und damit gäbe es wieder
 * einen Weg, eine Freigabe zu bauen, ohne dass eine Nutzerentscheidung
 * vorausging. Weil Kotlin `private` an die Klasse bindet und nicht an die Datei,
 * liegt der Werksweg deshalb in einem `companion object` **derselben** Klasse.
 */

// ── Katalogfelder ─────────────────────────────────────────────────────────

/**
 * Die fünf Felder, die der Verbindungskatalog der Aufgabe verlangt.
 *
 * Sie sind ein eigener Typ und nicht fünf freie Texte, weil die Oberfläche an
 * ihnen entscheidet: Solange eines dieser Felder offen ist, gilt die Verbindung
 * nicht als eingerichtet.
 */
enum class ConnectionField(val label: String, val germanLabel: String) {

    /** Woher die Verbindung stammt — Betreiber, Anbieter oder eigene Herstellung. */
    ORIGIN("Origin", "Herkunft"),

    /** Wohin gesprochen wird. */
    SERVER_ADDRESS("Server address", "Serveradresse"),

    /** Welche Werkzeuge der Server anbietet. */
    TOOLS("Tools", "Werkzeuge"),

    /** Wie sich angemeldet wird. */
    AUTHENTICATION("Authentication", "Anmeldung"),

    /** In welchem Projekt die Verbindung gilt. */
    PROJECT_SCOPE("Project scope", "Projektbereich")
}

/**
 * Eine **belegte** Angabe: die Aussage und ihre Quelle.
 *
 * Beides sind Pflichtfelder, und es gibt keinen anderen Weg hierher als
 * [CatalogValue.stated]. Damit kann niemand eine Herkunft, eine Adresse oder einen
 * Projektbereich ohne Beleg als Tatsache in den Katalog schreiben.
 */
data class ConnectionFact private constructor(
    val statement: String,
    val source: String
) {

    companion object {
        /** Der einzige Weg zu einer belegten Angabe. */
        fun of(statement: String, source: String): ConnectionFact {
            require(statement.isNotBlank()) { "Eine belegte Angabe braucht einen Text." }
            require(source.isNotBlank()) { "Eine belegte Angabe braucht ihre Quelle." }
            return ConnectionFact(statement.trim(), source.trim())
        }
    }
}

/**
 * Ein Feld im Verbindungskatalog: **entweder** belegt **oder** ausdrücklich offen.
 *
 * Der Grund ist derselbe wie in Aufgabe 129: „Ein Name ist kein Nachweis." Ein
 * leerer Text, der wie eine Tatsache aussieht, ist hier genauso unbrauchbar wie
 * eine erfundene Herkunft. Offen ist deshalb ein eigener Zustand mit eigener
 * Frage — kein Leerstring.
 */
sealed interface CatalogValue {

    /** Zu welchem Katalogfeld diese Angabe gehört. */
    val field: ConnectionField

    /** Die belegte Aussage, oder `null`, solange offen. */
    val fact: ConnectionFact?

    /** Die Zeile für die Oberfläche. Offen heißt „offen", nicht „nein". */
    fun displayLine(): String

    /** Ist dieses Feld offen? */
    val isOpen: Boolean get() = fact == null

    /** Eine belegte Angabe. */
    data class Stated(
        override val field: ConnectionField,
        override val fact: ConnectionFact
    ) : CatalogValue {
        override fun displayLine(): String =
            "${field.germanLabel}: ${fact.statement} (Quelle: ${fact.source})"
    }

    /** Eine offene Angabe: sichtbar, aber ohne Behauptung. */
    data class Unstated(
        override val field: ConnectionField,
        val question: String
    ) : CatalogValue {
        override val fact: ConnectionFact? get() = null
        override fun displayLine(): String = "${field.germanLabel}: offen — $question"
    }

    companion object {
        /** Der Weg zu einer belegten Angabe. */
        fun stated(field: ConnectionField, statement: String, source: String): CatalogValue =
            Stated(field, ConnectionFact.of(statement, source))

        /** Der Weg zu einer offenen Angabe. */
        fun open(field: ConnectionField, question: String): CatalogValue {
            require(question.isNotBlank()) { "Eine offene Angabe braucht eine Frage." }
            return Unstated(field, question.trim())
        }
    }
}

// ── Fähigkeiten ───────────────────────────────────────────────────────────

/**
 * **Ein** Werkzeug einer Verbindung: sein Name, was es bewirkt, und woher das
 * belegt ist.
 *
 * Der Wirkungstext ist Pflicht. „Nachvollziehbare Fähigkeiten" heißt, dass der
 * Nutzer vor dem Verbinden weiß, **was** er freischaltet; ein Name allein leistet
 * das nicht — dieselbe Zusage wie in Aufgabe 129, nur für Werkzeuge.
 */
data class ToolCapability private constructor(
    val name: String,
    val effect: String,
    val source: String
) {

    companion object {
        /** Der einzige Weg zu einem belegten Werkzeug. */
        fun of(name: String, effect: String, source: String): ToolCapability {
            require(name.isNotBlank()) { "Ein Werkzeug braucht einen Namen." }
            require(effect.isNotBlank()) { "Ein Werkzeug braucht eine benannte Wirkung." }
            require(source.isNotBlank()) { "Ein Werkzeug braucht eine Quelle." }
            return ToolCapability(name.trim(), effect.trim(), source.trim())
        }
    }

    /** Die Zeile für die Oberfläche. */
    fun displayLine(): String = "$name — $effect (Quelle: $source)"
}

// ── Anmeldung ─────────────────────────────────────────────────────────────

/** Wie sich bei einer Verbindung angemeldet wird. */
enum class AuthMethod(val label: String, val germanLabel: String) {

    /** Die Verbindung braucht keinen Schlüssel, etwa ein lokaler Dienst. */
    NONE("No credential", "ohne Schlüssel"),

    /** Ein Schlüssel wird im Anfragekopf mitgeschickt. */
    API_KEY_HEADER("API key header", "API-Schlüssel im Anfragekopf"),

    /** Ein Verfahren mit Token, das außerhalb von ClauDroide erneuert wird. */
    OAUTH_FLOW("OAuth flow", "OAuth-Verfahren"),

    /** Die Art der Anmeldung ist nicht bestimmt. */
    UNDETERMINED("Not determined", "nicht bestimmt");

    /** Wird für diese Art überhaupt ein Schlüssel gehalten? */
    val holdsCredential: Boolean get() = this == API_KEY_HEADER || this == OAUTH_FLOW
}

/**
 * Die Anmeldung einer Verbindung — in jedem Zustand **ohne** Klartextschlüssel.
 *
 * [ConnectionAuthentication.HeldCredential] hat einen privaten Konstruktor. Das
 * ist die Zusage „kein Geheimnis im Klartext" als Eigenschaft des Typs: Es gibt
 * keinen Weg, eine [ConnectionAuthentication.HeldCredential] zu bauen, ohne vorher
 * durch [held] zu gehen — und [held] maskiert. Das Rohmaterial wird nirgends als
 * Feld gespeichert.
 */
sealed interface ConnectionAuthentication {

    /** Die Verbindung braucht keinen Schlüssel. */
    data object WithoutCredential : ConnectionAuthentication

    /** Die Anmeldung ist nicht geklärt — mit der Frage, was fehlt. */
    data class Unresolved(val question: String) : ConnectionAuthentication

    /**
     * Gehaltene Anmeldedaten. [maskedValue] ist **immer** der Platzhalter des
     * Maskierers oder dessen geschwärzte Form; der Klartext existiert im Objekt
     * nicht.
     */
    class HeldCredential private constructor(
        val method: AuthMethod,

        /** Der bereits geschwärzte Wert. */
        val maskedValue: String,

        /** Welche Muster [SecretMasker] in der Eingabe erkannt hat. */
        val detectedPatterns: List<String>
    ) : ConnectionAuthentication {

        /** Niemals der Rohwert: nur Methode, Platzhalter und erkannte Muster. */
        override fun toString(): String =
            "HeldCredential(method=${method.germanLabel}, value=$maskedValue, patterns=$detectedPatterns)"

        /** Wurde der Rohwert hinter der Eingangstür zurückgehalten? Immer `true`. */
        val rawValueDiscarded: Boolean get() = true

        companion object {
            /**
             * Der **einzige** Weg zu gehaltenen Anmeldedaten.
             *
             * Der Rohwert läuft durch [SecretMasker.auditAndRedact], bevor er ein
             * Feld wird. Erkennt der Maskierer nichts, wird der Wert trotzdem
             * **nicht** übernommen: Eine Anmeldeangabe, die keinem bekannten Muster
             * folgt, ist immer noch ein Schlüssel. Übrig bleibt die überprüfbare
             * Angabe, **welche** Muster erkannt wurden.
             */
            fun held(method: AuthMethod, rawValue: String): HeldCredential {
                require(method.holdsCredential) { "Für diese Anmeldeart gibt es keinen Schlüssel." }
                require(rawValue.isNotBlank()) { "Ein Schlüssel braucht einen Wert." }

                val audit = SecretMasker.auditAndRedact(rawValue)
                val gespeichert = if (audit.scrubbedMatchesCount > 0) {
                    audit.sanitizedOutput
                } else {
                    SecretMasker.REDACTION_PLACEHOLDER
                }
                return HeldCredential(method, gespeichert, audit.matchedPatternNames)
            }
        }
    }
}

// ── Die Verbindung ────────────────────────────────────────────────────────

/**
 * **Ein** Eintrag im Verbindungskatalog.
 *
 * Der Konstruktor ist `private`. Der einzige Weg in den Katalog führt über
 * [of], und diese Methode entscheidet dort, was mit der Anmeldung passiert.
 * Damit kann keine Verbindung entstehen, deren Anmeldedaten den Maskierer
 * umgehen.
 *
 * @property tools das Katalogfeld „Werkzeuge": die dokumentierte Liste mit Quelle.
 * @property capabilities die Belegstellen je Werkzeug. Beides wird gebraucht:
 *   [tools] belegt, **welche** Werkzeuge der Server anbietet, [capabilities]
 *   belegt je Werkzeug, **was** es bewirkt. Eine belegte Liste ohne benannte
 *   Wirkungen ist nicht nachvollziehbar — und zählt deshalb nicht als eingerichtet.
 */
data class McpToolConnection private constructor(
    val connectionId: String,
    val displayName: String,
    val origin: CatalogValue,
    val serverAddress: CatalogValue,
    val tools: CatalogValue,
    val capabilities: List<ToolCapability>,
    val authentication: ConnectionAuthentication,
    val projectScope: CatalogValue,
    val projectId: String
) {

    /**
     * Die Katalogfelder, die offen sind.
     *
     * Die Reihenfolge ist die des Katalogs der Aufgabe, damit die Oberfläche die
     * Lücken in derselben Reihenfolge nennt, in der sie gefragt wurden.
     */
    val missingFields: List<ConnectionField>
        get() = ConnectionField.entries.filter { offen(it) }

    /** Ist dieses Feld offen? */
    private fun offen(field: ConnectionField): Boolean = when (field) {
        ConnectionField.ORIGIN -> origin.isOpen
        ConnectionField.SERVER_ADDRESS -> serverAddress.isOpen
        ConnectionField.TOOLS -> tools.isOpen || capabilities.isEmpty()
        ConnectionField.AUTHENTICATION -> authentication is ConnectionAuthentication.Unresolved
        ConnectionField.PROJECT_SCOPE -> projectScope.isOpen || projectId.isBlank()
    }

    /** Sind alle fünf Felder belegt und die Anmeldung geklärt? */
    val isFullyDescribed: Boolean get() = missingFields.isEmpty()

    /**
     * Sind die Fähigkeiten nachvollziehbar?
     *
     * Jedes Werkzeug braucht eine benannte Wirkung **und** eine Quelle; beides
     * erzwingt [ToolCapability.of]. Eine leere Liste ist keine Fähigkeitsangabe,
     * sondern das Fehlen einer.
     */
    val isTraceable: Boolean get() = capabilities.isNotEmpty()

    /**
     * Gilt diese Verbindung als eingerichtet?
     *
     * Nein, solange ein Feld offen ist oder keine Fähigkeit belegt wurde. Der
     * Eintrag im Katalog und der Name der Verbindung sind kein Ersatz dafür — das
     * ist „eine Verbindung ohne nachvollziehbare Fähigkeiten gilt nicht als
     * eingerichtet".
     */
    val isConfigured: Boolean get() = isFullyDescribed && isTraceable

    /** Der Zieltext, unter dem die Freigabe im Permission Center geführt wird. */
    fun permissionGrantTarget(): String = "mcp:$connectionId@$projectId"

    /** Der vollständige Katalogeintrag für die Oberfläche. */
    fun displayLines(): List<String> = buildList {
        add("$displayName ($connectionId)")
        add(origin.displayLine())
        add(serverAddress.displayLine())
        add(tools.displayLine())
        add(authenticationLine())
        add(projectScope.displayLine())
        if (capabilities.isEmpty()) {
            add("Werkzeuge: offen — es ist kein einzelnes Werkzeug mit benannter Wirkung belegt.")
        } else {
            capabilities.forEach { add("  ${it.displayLine()}") }
        }
        if (missingFields.isNotEmpty()) {
            add("Diese Verbindung ist deshalb noch nicht eingerichtet. " +
                "Offen: ${missingFields.joinToString(", ") { it.germanLabel }}.")
        }
    }

    /** Die Anmeldezeile — nie mit einem Klartextwert. */
    fun authenticationLine(): String {
        val kopf = ConnectionField.AUTHENTICATION.germanLabel
        return when (val auth = authentication) {
            ConnectionAuthentication.WithoutCredential ->
                "$kopf: ohne Schlüssel (belegt durch die Katalogeintragung)"

            is ConnectionAuthentication.Unresolved ->
                "$kopf: offen — ${auth.question}"

            is ConnectionAuthentication.HeldCredential ->
                "$kopf: ${auth.method.germanLabel}, Wert ${auth.maskedValue} (Maskierung am Eingang)" +
                    if (auth.detectedPatterns.isEmpty()) {
                        " — kein Muster erkannt, der Wert wurde vollständig zurückgehalten"
                    } else {
                        " — erkannte Muster: ${auth.detectedPatterns.joinToString(", ")}"
                    }
        }
    }

    companion object {

        /**
         * Der **einzige** Weg in den Verbindungskatalog.
         *
         * Zwei Dinge passieren hier und nicht später:
         *
         *  1. **Die Anmeldung wird maskiert**, bevor sie ein Feld wird. Der Rohwert
         *     [rawSecret] ist ein Parameter dieser Methode und geht danach in keinem
         *     Feld weiter.
         *  2. **Eine widersprüchliche Anmeldung wird offen gemeldet**, statt ratlos
         *     gespeichert: „ohne Schlüssel" mit angegebenem Schlüssel und „Schlüssel
         *     angekündigt, keiner angegeben" ergeben beide eine offene Frage — und der
         *     verworfene Rohwert wird nirgends abgelegt.
         */
        fun of(
            connectionId: String,
            displayName: String,
            origin: CatalogValue,
            serverAddress: CatalogValue,
            tools: CatalogValue,
            capabilities: List<ToolCapability>,
            authMethod: AuthMethod,
            rawSecret: String?,
            projectScope: CatalogValue,
            projectId: String
        ): McpToolConnection {
            require(connectionId.isNotBlank()) { "Eine Verbindung braucht eine Kennung." }
            require(displayName.isNotBlank()) { "Eine Verbindung braucht einen Namen." }
            return McpToolConnection(
                connectionId = connectionId.trim(),
                displayName = displayName.trim(),
                origin = origin,
                serverAddress = serverAddress,
                tools = tools,
                capabilities = capabilities.toList(),
                authentication = authenticationFor(authMethod, rawSecret),
                projectScope = projectScope,
                projectId = projectId.trim()
            )
        }

        /**
         * Bildet die Anmeldeangabe — und entscheidet dort, wo die Angabe sich
         * selbst widerspricht.
         *
         * Der Rohwert wird in **keinem** Zweig gespeichert: Er geht nur an
         * [ConnectionAuthentication.held], und der gibt nichts als das Maskierte
         * zurück.
         */
        private fun authenticationFor(
            method: AuthMethod,
            rawSecret: String?
        ): ConnectionAuthentication {
            val secret = rawSecret?.takeIf { it.isNotBlank() }
            return when {
                method == AuthMethod.NONE && secret == null -> ConnectionAuthentication.WithoutCredential
                method == AuthMethod.NONE -> ConnectionAuthentication.Unresolved(
                    "als „ohne Schlüssel“ angeboten, aber es wurde ein Schlüssel angegeben — der Wert wurde nicht übernommen"
                )
                method == AuthMethod.UNDETERMINED -> ConnectionAuthentication.Unresolved(
                    "die Art der Anmeldung ist nicht bestimmt"
                )
                secret == null -> ConnectionAuthentication.Unresolved(
                    "ein Schlüssel ist angekündigt, aber keiner angegeben"
                )
                else -> ConnectionAuthentication.HeldCredential.Companion.held(method, secret)
            }
        }
    }
}