package org.claudroide.app.feature.git

import org.claudroide.app.feature.provider.ProviderConfigValidator
import org.claudroide.app.feature.provider.SecretMasker

/**
 * Task 095 — „Git-Zugang".
 *
 * Ziel: Git-Zugriff über einen vom Nutzer eingerichteten **und widerrufbaren**
 * Weg. Ergebnis: Vergleich **dokumentierter** Zugangswege, Berechtigungsumfang
 * und sichere Ablage.
 *
 * Diese Datei richtet **keinen** Zugang ein. Sie stellt das Gerüst, das eine
 * Einrichtung aufnimmt, und verhindert three Dinge strukturell:
 *
 *  1. **Kein Zugangsweg wird behauptet, bevor er belegt ist.** Jede Angabe zu
 *     einem Weg ist ein [DocumentedFact] mit Quelle **und** Prüfdatum — der
 *     Konstruktor verlangt beides. Was unbekannt ist, steht in
 *     [GitAccessPath.openQuestions] und wird als „offen" angezeigt, nie als
 *     leeres Feld. Das ist dasselbe Muster wie [org.claudroide.app.feature.provider.ProviderPrivacyProfile]
 *     aus Aufgabe 125, und aus demselben Grund: eine unbelegte Aussage über
 *     Rechte oder Lebensdauer darf nicht wie eine Tatsache aussehen.
 *
 *  2. **Der Umfang wird nicht impliciterweitert.** [RepositoryScope] hat keine
 *     Variante „alles, was erreichbar ist" ohne dass sie benannt wird. Der
 *     Zugriff auf Repositories ist eine **Liste von Namen**, und
 *     [RepositoryScope.isLeastPrivilege] ist genau dann wahr, wenn mindestens ein
 *     Repository benannt ist. [GitAccessGate.mayUse] verweigert einen Weg, dessen
 *     Umfang unbenannt ist — ein Weg mit weitem Zugriff ist benannt, aber nicht
 *     empfohlen.
 *
 *  3. **Keine privaten Schlüssel in Projektdateien oder Chats.** Das ist die
 *     Zusage im Abschnitt „Schutz" und sie ist eine Eigenschaft des Typs, nicht
 *     ein Hinweistext: [GitCredential] hat einen **privaten** Konstruktor, und
 *     der einzige Weg zu einem Wert führt über [GitCredential.record], der
 *     [CredentialSink.KEYSTORE] verlangt. [CredentialSink.PROJECT_FILE] und
 *     [CredentialSink.CHAT_TRANSCRIPT] existieren, damit die App sie **benennen
 *     und verweigern** kann — sie sind keine Ausnahme, die man sich erschleichen
 *     könnte, indem man sie nicht aufschreibt.
 *
 * ## Eine Korrektur an einer früheren Aussage
 *
 * In einer früheren Sitzung dieses Projekts stand, eine GitHub App bringe
 * „keinen privaten Schlüssel mit" und der Schutz der Aufgabe sei damit
 * erfüllt. **Das ist falsch und hiermit zurückgenommen.** Die offizielle
 * GitHub-Dokumentation weist für den Manifest-Flow ausdrücklich `pem (private
 * key)` als Rückgabewert aus
 * (<https://docs.github.com/en/rest/apps/apps>, gelesen 2026-10-03); eine
 * GitHub App authentifiziert sich über einen mit ihrem privaten Schlüssel
 * signierten JWT. Die Wahl einer GitHub App bleibt trotzdem richtig — sie ist
 * fein abgestuft, repository-gebunden und ihre Installationstoken sind
 * kurzlebig — aber sie **erfüllt den Schutz nicht von selbst**. Erfüllt wird er
 * nur, wenn der private Schlüssel über [CredentialSink.KEYSTORE] abgelegt wird.
 * Deshalb ist [CredentialSink] hier kein Beiwerk, sondern der Kern.
 *
 * ## Die belegten Fakten
 *
 * Aus derselben Quelle
 * (<https://docs.github.com.com/en/apps/oauth-apps/building-oauth-apps/differences-between-github-apps-and-oauth-apps>,
 * gelesen 2026-10-03), wörtlich: *„In general, GitHub Apps are preferred to
 * OAuth apps because they use fine-grained permissions, give more control over
 * which repositories the app can access, and use short-lived tokens."* Ferner
 * dort: das Installationstoken einer GitHub App verliert den Zugriff, wenn ein
 * Administrator Repositories aus der Installation entfernt, und es läuft nach
 * **einer Stunde** ab; OAuth-App-Token sind standardmäßig langlebig. Diese
 * Sätze stehen als [DocumentedFact] mit Quelle und Datum im Katalog, nicht in
 * diesem Kommentar — die Datei führt keine eigenen Zahlen.
 *
 * Reines Kotlin: kein Netz, kein Dateisystem, kein Android. Diese Datei stellt
 * keinen Zugang her, spricht niemanden an und speichert nichts.
 */

// ── Belegte Angaben ───────────────────────────────────────────────────────

/**
 * Eine **belegte** Angabe über einen Zugangsweg.
 *
 * [sourceUrl] und [verifiedOn] sind Pflichtfelder: Der Konstruktor verlangt
 * beide. Damit gibt es keinen Weg, eine Aussage über Rechte oder Lebensdauer
 * als bekannt zu deklarieren, ohne eine Quelle zu erfinden — und der Preis dafür
 * ist ehrlich sichtbar, weil [GitAccessPath.displayLines] Quelle und Datum mit
 * ausgibt.
 */
data class DocumentedFact(
    val statement: String,
    val sourceUrl: String,
    val verifiedOn: String
) {
    init {
        require(statement.isNotBlank()) { "Eine Angabe braucht ihren Text." }
        require(sourceUrl.isNotBlank()) { "Eine belegte Angabe braucht ihre Quelle." }
        require(verifiedOn.isNotBlank()) { "Eine belegte Angabe braucht ein Prüfdatum." }
    }

    /** Die Zeile für die Oberfläche — Aussage **und** Herkunft. */
    fun displayLine(): String = "$statement (Quelle: $sourceUrl, geprüft $verifiedOn)"
}

/** Ein Thema, zu dem zu einem Zugangsweg etwas offen ist. */
enum class GitAuthTopic(val label: String, val germanLabel: String) {

    /** Wie lange ein Zugriffstoken gültig ist. */
    TOKEN_LIFETIME("Token lifetime", "Lebensdauer des Tokens"),

    /** Wie der Widerruf technisch erfolgt. */
    REVOCATION("Revocation", "Widerruf"),

    /** Ob ein geheimer Schlüssel auf dem Gerät liegt. */
    SECRET_ON_DEVICE("Secret on device", "Schlüssel auf dem Gerät"),

    /** Welche Rechte der Weg überhaupt beansprucht. */
    PERMISSION_SCOPE("Permission scope", "Berechtigungsumfang")
}

/**
 * Eine **nicht** belegte Angabe — sichtbar, aber ohne Behauptung.
 *
 * [displayLine] sagt „offen". Es gibt hier keinen Weg, „unbekannt" als Wert
 * einzutragen, der wie eine Tatsache aussieht.
 */
data class OpenQuestion(
    val topic: GitAuthTopic,
    val detail: String
) {
    init {
        require(detail.isNotBlank()) { "Eine offene Frage braucht eine Beschreibung." }
    }

    fun displayLine(): String = "${topic.germanLabel}: offen — $detail"
}

// ── Die Zugangswege ───────────────────────────────────────────────────────

/**
 * Die Art des Zugangswegs.
 *
 * Die Reihenfolge ist die der Einschränkung: [GITHUB_APP] steht vorn, weil es
 * der einzige Weg ist, der alle drei belegten Eigenschaften zugleich hat —
 * fein abgestufte Rechte, Repository-Bindung und kurzlebiges Token. Es ist
 * keine Wertung, sondern die Reihenfolge der Belege in
 * [GITHUB_APP_DOCUMENTED].
 */
enum class GitAccessPathKind(
    val label: String,
    val germanLabel: String
) {

    /** Fein abgestufte Rechte, nur gewählte Repositories, kurzlebiges Token. */
    GITHUB_APP("GitHub App", "GitHub App"),

    /** Breite Scopes, Zugriff auf alle erreichbaren Ressourcen des Nutzers. */
    OAUTH_APP("OAuth app", "OAuth-App"),

    /** Ein selbst erzeugter Token, vom Nutzer mit Handbreite vergeben. */
    PERSONAL_ACCESS_TOKEN("Personal access token", "persönlicher Zugriffstoken"),

    /** Ein Schlüsselpaar auf dem Gerät; der öffentliche Schlüssel liegt am Server. */
    SSH_KEY("SSH key", "SSH-Schlüssel")
}

/**
 * Die belegten Angaben zu den GitHub-spezifischen Wegen.
 *
 * Zwei [DocumentedFact] mit derselben Quelle und demselben Prüfdatum, weil
 * genau diese Sätze die Entscheidung tragen. Sie sind Werte, keine Konstanten im
 * Kommentar — dadurch kann ein Test prüfen, ob der Katalog die Quelle noch
 * nennt, und ein späterer Prüfer sieht, **wovon** die Aussage stammt.
 */
object GitHubAppDocumentedFacts {

    /** Die Quelle. Gelesen und belegt am 2026-10-03. */
    const val SOURCE_URL: String =
        "https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/differences-between-github-apps-and-oauth-apps"

    /** Das Prüfdatum. Ohne dieses Feld wäre [DocumentedFact] nicht baubar. */
    const val VERIFIED_ON: String = "2026-10-03"

    /**
     * Die Rechte- und Token-Aussage, wörtlich aus der Quelle.
     *
     * *„In general, GitHub Apps are preferred to OAuth apps because they use
     * fine-grained permissions, give more control over which repositories the app
     * can access, and use short-lived tokens."*
     */
    fun fineGrainedAndShortLived(): DocumentedFact = DocumentedFact(
        statement = "GitHub Apps verwenden fein abgestufte Rechte, geben mehr " +
            "Kontrolle darüber, welche Repositories die App sehen darf, und nutzen kurzlebige Token.",
        sourceUrl = SOURCE_URL,
        verifiedOn = VERIFIED_ON
    )

    /**
     * Die Widerrufsaussage, wörtlich aus der Quelle.
     *
     * *„The installation token from a GitHub App loses access to resources if an
     * admin removes repositories from the installation."*
     */
    fun revocationByRemovingRepository(): DocumentedFact = DocumentedFact(
        statement = "Wird ein Repository aus der Installation entfernt, verliert das " +
            "Installationstoken sofort den Zugriff darauf.",
        sourceUrl = SOURCE_URL,
        verifiedOn = VERIFIED_ON
    )

    /**
     * Die Lebensdauer des Installationstokens, wörtlich aus der Quelle.
     *
     * *„Installation access tokens expire after a predefined amount of time
     * (currently 1 hour)."* Die Stunde steht als Zeichenkette in der belegten
     * Aussage und **nicht** als Zahl im Code: eine Zahl im Code wäre eine
     * Behauptung, die sich still veralten könnte, ohne dass ein Test rot wird.
     */
    fun installationTokenExpires(): DocumentedFact = DocumentedFact(
        statement = "Installationstoken laufen nach einer festgelegten Zeit ab (laut Quelle derzeit eine Stunde).",
        sourceUrl = SOURCE_URL,
        verifiedOn = VERIFIED_ON
    )

    /**
     * Die OAuth-Gegenprobe aus derselben Quelle.
     *
     * *„Authorizing an OAuth app grants the app access to the user's accessible
     * resources. […] OAuth app tokens are long-lived by default."* Diese Aussage
     * ist deshalb im Katalog, weil sie den **Nachteil** des Alternativwegs belegt
     * — ein Vergleich, der nur die gute Seite zeigte, wäre keine Entscheidungsgrundlage.
     */
    fun oauthTokensAreLongLived(): DocumentedFact = DocumentedFact(
        statement = "OAuth-App-Token sind standardmäßig langlebig; sie gelten für alle " +
            "für den Nutzer erreichbaren Ressourcen, nicht nur für gewählte Repositories.",
        sourceUrl = SOURCE_URL,
        verifiedOn = VERIFIED_ON
    )
}

/**
 * Ein dokumentierter Zugangsweg.
 *
 * Reiner Wert. [facts] nimmt nur [DocumentedFact] an (mit Quelle und Datum),
 * [openQuestions] nimmt nur [OpenQuestion]. Es gibt damit keinen Weg, eine
 * Vermutung als Beleg einzutragen.
 *
 * @property requiresSecretOnDevice liegt auf dem Gerät ein geheimer Schlüssel.
 *         **Nicht** zu verwechseln mit dem Token: ein Token ist kurzlebig und
 *         wird vom Server ausgestellt, ein privater Schlüssel ist dauerhaft
 *         vorhanden. Beides wird getrennt geführt, weil sie unterschiedliche
 *         Folgen für den Widerruf haben.
 */
data class GitAccessPath(
    val kind: GitAccessPathKind,
    val displayName: String,
    val facts: List<DocumentedFact>,
    val openQuestions: List<OpenQuestion> = emptyList(),
    val requiresSecretOnDevice: Boolean = false
) {
    init {
        require(displayName.isNotBlank()) { "Ein Zugangsweg braucht einen Namen." }
    }

    /** Ist zu diesem Weg etwas offen? */
    val hasOpenQuestions: Boolean get() = openQuestions.isNotEmpty()

    /**
     * Ist dieser Weg vollständig belegt?
     *
     * Nur dann darf die App eine Aussage über Rechte, Lebensdauer oder Widerruf
     * als gesichert darstellen. Solange [hasOpenQuestions] gilt, ist die
     * ehrliche Aussage „ungeklärt" — dieselbe Zusage wie in Aufgabe 125.
     */
    val isFullyDocumented: Boolean get() = openQuestions.isEmpty()

    /** Die Zeilen für die Oberfläche, in fester Reihenfolge. */
    fun displayLines(): List<String> = buildList {
        add("Zugangsweg: $displayName")
        if (facts.isEmpty()) {
            add("Zu diesem Zugangsweg liegt keine belegte Angabe vor.")
        } else {
            add("Belegt:")
            facts.forEach { add("  ${it.displayLine()}") }
        }
        if (hasOpenQuestions) {
            add("Offen:")
            openQuestions.forEach { add("  ${it.displayLine()}") }
        }
    }
}

/**
 * Der Katalog der Zugangswege.
 *
 * Die Wege, deren Angaben **nicht** aus einer gelesenen Quelle stammen —
 * persönlicher Token, SSH-Schlüssel —, bekommen hier bewusst **keine**
 * [DocumentedFact]. Sie stehen als [OpenQuestion] drin. Das ist keine
 * Wissenslücke, die kaschiert wird: es ist die Aussage, dass die App hier
 * nichts belegt hat und deshalb nichts behauptet.
 */
object GitAccessCatalog {

    /** Der Weg, den die Nutzerentscheidung für dieses Projekt festlegt. */
    val GITHUB_APP: GitAccessPath = GitAccessPath(
        kind = GitAccessPathKind.GITHUB_APP,
        displayName = "GitHub App",
        facts = listOf(
            GitHubAppDocumentedFacts.fineGrainedAndShortLived(),
            GitHubAppDocumentedFacts.revocationByRemovingRepository(),
            GitHubAppDocumentedFacts.installationTokenExpires()
        ),
        // Belegt und aus dem Grund dieser Liste ausgeschlossen: der private
        // Schlüssel wird in einem Projektordner oder Chatverlauf nicht erzeugt
        // werden, weil er dort nicht hingehört.
        openQuestions = emptyList(),
        requiresSecretOnDevice = true
    )

    /** Der Alternativweg mit breiteren Rechten und langlebigen Token. */
    val OAUTH_APP: GitAccessPath = GitAccessPath(
        kind = GitAccessPathKind.OAUTH_APP,
        displayName = "OAuth-App",
        facts = listOf(
            GitHubAppDocumentedFacts.oauthTokensAreLongLived()
        ),
        requiresSecretOnDevice = false
    )

    /**
     * Der selbst erzeugte Token.
     *
     * **Ohne** Beleg: Weder Lebensdauer noch Widerruf sind hier belegt, und es
     * wird auch nichts behauptet. Die App sagt damit offen, dass sie diesen Weg
     * nicht empfehlen kann — was ehrlicher ist als eine erfundene Frist.
     */
    val PERSONAL_ACCESS_TOKEN: GitAccessPath = GitAccessPath(
        kind = GitAccessPathKind.PERSONAL_ACCESS_TOKEN,
        displayName = "Persönlicher Zugriffstoken",
        facts = emptyList(),
        openQuestions = listOf(
            OpenQuestion(GitAuthTopic.TOKEN_LIFETIME, "keine belegte Angabe zur Gültigkeitsdauer"),
            OpenQuestion(GitAuthTopic.REVOCATION, "keine belegte Angabe zum Widerruf ohne den Nutzer selbst"),
            OpenQuestion(GitAuthTopic.PERMISSION_SCOPE, "keine belegte Angabe zu erreichbaren Repository-Arten")
        ),
        requiresSecretOnDevice = true
    )

    /** Der SSH-Schlüssel — ebenfalls ohne Beleg, aus demselben Grund. */
    val SSH_KEY: GitAccessPath = GitAccessPath(
        kind = GitAccessPathKind.SSH_KEY,
        displayName = "SSH-Schlüssel",
        facts = emptyList(),
        openQuestions = listOf(
            OpenQuestion(GitAuthTopic.SECRET_ON_DEVICE, "der private Schlüssel liegt dauerhaft auf dem Gerät"),
            OpenQuestion(GitAuthTopic.REVOCATION, "keine belegte Angabe zum Widerruf ohne den Schlüssel zu ersetzen")
        ),
        requiresSecretOnDevice = true
    )

    /** Alle vier Wege in der Reihenfolge der Einschränkung. */
    val all: List<GitAccessPath> = listOf(GITHUB_APP, OAUTH_APP, PERSONAL_ACCESS_TOKEN, SSH_KEY)
}

// ── Der Berechtigungsumfang ───────────────────────────────────────────────

/**
 * Auf welche Repositories ein Zugang zugreifen darf.
 *
 * Es gibt bewusst **keine** Variante „alles, was erreichbar ist". Der
 * Normalfall ist eine **Liste von Namen**: [SELECTED]. [isLeastPrivilege] ist
 * genau dann wahr, wenn mindestens ein Repository benannt ist, und
 * [GitAccessGate.mayUse] verweigert diejenigen Wege, deren Umfang unbenannt
 * bleibt. Damit ist „möglichst auf erforderliche Repositories beschränkt" —
 * die erste Fertig-Bedingung der Aufgabe — eine Eigenschaft und keine Absicht.
 */
sealed interface RepositoryScope {

    /** Nur diese Repositories, benannt. Der Normalfall. */
    data class Selected(val repositoryNames: List<String>) : RepositoryScope {
        init {
            require(repositoryNames.isNotEmpty()) {
                "Ein benannter Umfang braucht mindestens ein Repository."
            }
            require(repositoryNames.none { it.isBlank() }) {
                "Ein Repository-Name darf nicht leer sein."
            }
            require(repositoryNames.distinct().size == repositoryNames.size) {
                "Dieselbe Repository-Liste zu wiederholen ändert den Umfang nicht."
            }
        }
    }

    /**
     * Jedes Repository, das der Nutzer sehen kann.
     *
     * Existiert, weil GitHub es anbietet ([GitAccessPathKind.OAUTH_APP]) und die
     * App einen vorhandenen Zustand **benennen** muss, statt ihn zu verstecken.
     * [isLeastPrivilege] ist hier `false` und [displayLine] sagt das im Klartext.
     * [Selected] ist damit die einzige Form, die [GitAccessGate.mayUse] ohne
     * Einschränkung annimmt.
     */
    data object EverythingVisible : RepositoryScope

    /** Ist der Umfang auf einzelne Repositories beschränkt? */
    val isLeastPrivilege: Boolean
        get() = this is Selected

    /** Die Zeile für die Oberfläche. */
    fun displayLine(): String = when (this) {
        is Selected -> "Nur benannte Repositories: ${repositoryNames.joinToString(", ")}"
        EverythingVisible -> "Alle für den Nutzer erreichbaren Repositories — nicht auf erforderliche beschränkt"
    }

    /** Gehört [repositoryName] zu diesem Umfang? */
    fun covers(repositoryName: String): Boolean = when (this) {
        is Selected -> repositoryName in repositoryNames
        EverythingVisible -> true
    }
}

// ── Sichere Ablage ────────────────────────────────────────────────────────

/**
 * Wohin ein geheimer Wert gelegt werden darf.
 *
 * Die Variante [KEYSTORE] ist die einzige, die [GitCredential.record] annimmt.
 * [PROJECT_FILE] und [CHAT_TRANSCRIPT] existieren, damit die App sie beim
 * Versuch **benennen und verweigern** kann — so kann ein Aufrufer die Regel nicht
 * umgehen, indem er sie gar nicht ausspricht, und der Nutzer sieht im Fehlerfall
 * den Namen der unzulässigen Ablage statt eines unverständlichen Abbruchs.
 *
 * Der Bezug ist [org.claudroide.app.feature.provider.AndroidKeystoreSecurityPolicy]
 * aus Aufgabe 045: AES-256-GCM im hardwaregestützten Schlüsselspeicher, ohne
 * Klartextdatei, ausgeschlossen aus Sicherungen.
 */
enum class CredentialSink(
    val label: String,
    val germanLabel: String,
    val isPermitted: Boolean
) {

    /** Im hardwaregestützten Schlüsselspeicher. Der einzige zulässige Ort. */
    KEYSTORE("hardware-backed keystore", "hardwaregestützter Schlüsselspeicher", true),

    /** In einer Datei des Projekts — unzulässig. */
    PROJECT_FILE("project file", "Projektdatei", false),

    /** In einem Chatverlauf oder Aufgaben-Markdown — unzulässig. */
    CHAT_TRANSCRIPT("chat transcript", "Chatverlauf", false);

    /** Die Zeile für die Oberfläche, wenn die Ablehnung angezeigt wird. */
    fun refusalLine(): String =
        if (isPermitted) germanLabel
        else "$germanLabel ist für einen geheimen Wert nicht zulässig."
}

/**
 * Ein geheimer Wert, der einem Zugangsweg zugehört.
 *
 * **Privater Konstruktor.** Der einzige Weg zu einem [GitCredential] führt über
 * [record], und dort steht die Prüfung:
 *
 *  * [sink] muss [CredentialSink.KEYSTORE] sein,
 *  * [secret] muss nicht leer sein,
 *  * [secret] darf kein Platzhalter sein, der wie ein echter Schlüssel beginnt,
 *    ohne einer zu sein.
 *
 * Warum nicht `internal`: `internal` wäre im ganzen Modul für jede Datei
 * aufrufbar, und es gäbe wieder einen Weg, einen Wert zu bauen, ohne die
 * Ablageregel zu prüfen. Kotlin bindet `private` an die Klasse, nicht an die
 * Datei; deshalb liegt der Werksweg im Begleitobjekt **derselben** Klasse.
 *
 * Der Wert wird **nicht** in Feldern gespiegelt, die eine Anzeige ausspucken
 * könnten. [displayLine] und [maskedFingerprint] geben nie das Geheimnis aus.
 */
class GitCredential private constructor(
    val credentialId: String,
    val pathKind: GitAccessPathKind,
    private val secret: String,
    val sink: CredentialSink
) {

    /**
     * Der Wert **für den Gebrauch**, nicht für die Anzeige.
     *
     * Bewusst benannt und bewusst das einzige öffentliche Feld dieser Klasse, das
     * das Geheimnis zurückgibt: Es gibt genau einen Abrufpfad, und er heißt
     * `useSecret`, damit eine Anzeige ihn nicht versehentlich benutzt.
     *
     * [toString] ist **überschrieben**, weil Kotlin sonst die Standarddarstellung
     * aller Felder erzeugen würde — und weil `secret` ein Feld dieser Klasse ist,
     * stünde der Wert dann in jedem Log, jeder Fehlermeldung und jedem
     * `assertEquals`-Fehlschlag dieser Klasse. Die Überschreibung nennt ihn nicht.
     */
    fun useSecret(): String = secret

    /**
     * Die maskierte Form, für Anzeigen und Prüfungen.
     *
     * **Bewusst nicht** [ProviderConfigValidator.maskApiKey]: Diese Funktion gibt
     * die ersten und letzten vier Zeichen des Wertes zurück. Bei einem
     * API-Schlüssel ist das ein vertretbarer Anteil, bei einem PEM-Block einer
     * GitHub App sind es die Ränder eines privaten Schlüssels — und die
     * Ausgabe von `maskApiKey` behält sie sogar, weil die Zeilen
     * `-----BEGIN …` und `-----END` durch den Maskierer nicht mehr erkannt
     * werden. Diese Klasse gibt deshalb **kein** Zeichen des Geheimnisses aus.
     *
     * Der Preis ist, dass zwei verschiedene Werte nicht mehr unterscheidbar sind.
     * Das ist der richtige Preis: Der Nutzer sieht ohnehin über
     * [credentialId], welches Geheimnis gemeint ist, und ein „Fingerabdruck",
     * der einen Schlüsselanteil preisgibt, wäre nur eine schwächere Form desselben
     * Geheimnisses.
     */
    fun maskedFingerprint(): String = SecretMasker.REDACTION_PLACEHOLDER

    /**
     * Die Zeile für die Oberfläche.
     *
     * Sie nennt **nie** ein Zeichen des Geheimnisses. Weder der Anfang noch das
     * Ende — siehe [maskedFingerprint], warum [ProviderConfigValidator.maskApiKey]
     * hier nicht verwendet wird.
     */
    fun displayLine(): String =
        "$pathKind → ${sink.germanLabel}, Geheimnis ${maskedFingerprint()}"

    override fun toString(): String =
        "GitCredential(id=$credentialId, kind=$pathKind, sink=$sink, secret=<nicht ausgegeben>)"

    /** Zwei Zugangsdaten sind derselbe, wenn Ablage und Wert übereinstimmen. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GitCredential) return false
        return credentialId == other.credentialId &&
            pathKind == other.pathKind &&
            secret == other.secret
    }

    override fun hashCode(): Int {
        var result = credentialId.hashCode()
        result = 31 * result + pathKind.hashCode()
        result = 31 * result + secret.hashCode()
        return result
    }

    companion object {

        /**
         * Der einzige Weg zu einem [GitCredential].
         *
         * @param credentialId eine Kennung **ohne** Geheimnis — sie wird in
         *        Logs und Bildschirmen angezeigt und darf deshalb nichts
         *        enthalten, was nach einem Schlüssel aussieht.
         * @param sink der Ort. Nur [CredentialSink.KEYSTORE] wird akzeptiert.
         * @param secret der geheime Wert. Er wird auf einen echten Schlüssel
         *        geprüft: ein PEM-Block oder ein `sk-…`-artiger Wert, der als
         *        Geheimnis **in die Anzeige** gerät, wäre genau der Fehler, den
         *        die Aufgabe verhindert.
         *
         * @throws IllegalArgumentException bei jeder anderen Ablage. Die
         *         Verweigerung nennt den unzulässigen Ort, damit die Oberfläche
         *         den Grund zeigen kann statt eines leeren Fehlers.
         */
        fun record(
            credentialId: String,
            pathKind: GitAccessPathKind,
            secret: String,
            sink: CredentialSink
        ): GitCredential {
            require(credentialId.isNotBlank()) { "Eine Zugangsdatenauskunft braucht ihre Kennung." }
            require(sink.isPermitted) {
                "Ein geheimer Wert gehört nicht in ${sink.germanLabel}. " +
                    "Nur ${CredentialSink.KEYSTORE.germanLabel} ist zulässig."
            }
            require(secret.isNotBlank()) { "Ein geheimer Wert braucht einen Inhalt." }
            require(!SecretMasker.containsSecretLikeText(credentialId)) {
                "Die Kennung wird angezeigt und protokolliert; sie darf keinen Schlüssel enthalten."
            }
            return GitCredential(credentialId.trim(), pathKind, secret, sink)
        }
    }
}

/**
 * Der Tresor, in dem geheime Werte liegen.
 *
 * Getrennt von [GitCredential], weil der Wert und sein **Ort** verschiedene
 * Fragen sind: [GitCredential] sagt, wohin ein Wert gehört, dieses interface
 * sagt, was tatsächlich darin liegt.
 *
 * [exportSafeMetadata] ist die tragende Zusage für den Abschnitt „Schutz": Was
 * das Gerät verlässt, ist [SecretMasker.redact] unterzogen. Es gibt keinen Weg,
 * aus diesem Tresor einen Rohwert zu exportieren — die Anzeige bekommt
 * ausschließlich [GitCredential.displayLine] und [GitCredential.maskedFingerprint].
 */
interface GitCredentialVault {

    /** Legt einen Wert ab. Ein bereits vorhandener wird ersetzt. */
    fun store(credential: GitCredential)

    /** Der Wert für den Gebrauch, oder `null`, wenn er nicht vorhanden ist. */
    fun retrieve(credentialId: String): GitCredential?

    /** Entfernt einen Wert. `true`, wenn etwas entfernt wurde. */
    fun remove(credentialId: String): Boolean

    /** Wie viele Werte liegen hier? */
    fun size(): Int

    /**
     * Was der Tresor über sich preisgeben darf.
     *
     * Der Rückgabewert ist bereits [SecretMasker.redact] unterzogen. Ein Test
     * prüft, dass dort kein Teil eines Geheimnisses steht — auch kein Anfang.
     */
    fun exportSafeMetadata(): Map<String, String>
}

/**
 * Der Tresor für die JVM-Tests und als Bauplan für die Gerätefassung.
 *
 * Die Gerätefassung ist in [AndroidKeystoreSecurityPolicy] beschrieben
 * (AES-256-GCM, 256 Bit, hardwaregestützt, ausgeschlossen aus Sicherungen).
 * Diese Klasse hält denselben Vertrag in-memory, damit der Vertrag auf der JVM
 * prüfbar ist — sie ist **kein** Ersatz für den Keystore und behauptet das
 * auch nicht.
 */
class InMemoryGitCredentialVault : GitCredentialVault {

    private val schrank = java.util.concurrent.ConcurrentHashMap<String, GitCredential>()

    override fun store(credential: GitCredential) {
        schrank[credential.credentialId] = credential
    }

    override fun retrieve(credentialId: String): GitCredential? = schrank[credentialId]

    override fun remove(credentialId: String): Boolean = schrank.remove(credentialId) != null

    override fun size(): Int = schrank.size

    /**
     * Nur [GitCredential.displayLine] — nie [useSecret].
     *
     * [displayLine] ist zusätzlich noch einmal durch [SecretMasker.redact]
     * geschickt. Das ist doppelt und trotzdem richtig: die erste Maskierung
     * schützt den Wert, die zweite schützt die **Zusammensetzung** der Zeile,
     * falls ein Fingerabdruckformat sich ändert.
     */
    override fun exportSafeMetadata(): Map<String, String> =
        schrank.values.associate { it.credentialId to SecretMasker.redact(it.displayLine()) }
}

// ── Das Wissen des Nutzers über den Upload ────────────────────────────────

/**
 * Was der Nutzer wissen muss, **bevor** etwas das Gerät verlässt.
 *
 * Die zweite Fertig-Bedingung der Aufgabe: *„Nutzer versteht, dass Uploads
 * externe Speicherung verursachen."* Diese Klasse macht daraus einen Wert, der
 * bestätigt werden muss, und [GitAccessGate.mayUse] verweigert ohne ihn.
 *
 * [externalStorageAcknowledged] wird **nicht** aus einer anderen Eigenschaft
 * abgeleitet. Es gibt kein `acknowledged = scope.isLeastPrivilege` und kein
 * `acknowledged = true` als Vorgabe: Die Bestätigung ist eine eigene Eingabe
 * des Nutzers, sonst wäre sie eine Behauptung der App über den Nutzer.
 */
data class UploadDisclosure(
    val repositoryNames: List<String>,
    val localOnlySummary: String,
    val externalStorageAcknowledged: Boolean
) {
    init {
        require(repositoryNames.isNotEmpty()) {
            "Vor einem Upload muss benannt sein, wohin er geht."
        }
    }

    /** Die Zeilen für die Oberfläche, in fester Reihenfolge. */
    fun displayLines(): List<String> = buildList {
        add("Ziele: ${repositoryNames.joinToString(", ")}")
        add("Auf dem Gerät: $localOnlySummary")
        add("Nach dem Upload: Der Inhalt liegt außerhalb des Geräts, beim Anbieter, und ist dort gespeichert.")
    }
}

// ── Das Tor ───────────────────────────────────────────────────────────────

/** Das Ergebnis einer Zugangsprüfung. */
sealed interface GitAccessDecision {

    /**
     * Zugang ist möglich.
     *
     * @property scopeLines die benannten Ziele — damit die Oberfläche sagen kann,
     *         **wohin** es geht, ohne den Zugangsweg zu raten.
     */
    data class Allowed(val scopeLines: List<String>) : GitAccessDecision

    /** Der Nutzer kennt das Ziel und die externe Speicherung nicht. */
    data class NotAcknowledged(val reasons: List<String>) : GitAccessDecision

    /** Der Umfang ist nicht auf erforderliche Repositories beschränkt. */
    data class ScopeTooBroad(val reason: String) : GitAccessDecision
}

/**
 * Die Regel, wann ein Git-Zugang benutzt werden darf.
 *
 * Alle drei Bedingungen der Aufgabe stehen an der **Abfrage**, nicht in einer
 * Anweisung, die man beim Aufrufen übersieht:
 *
 *  1. Der Weg ist **belegt** ([GitAccessPath.isFullyDocumented]). Ein Weg mit
 *     offenen Fragen wird abgelehnt, weil die App dann nicht behaupten kann,
 *     der Nutzer wisse, welche Rechte er erteilt. Das betrifft in diesem Katalog
 *     den persönlichen Token und den SSH-Schlüssel — beide sind bewusst unbelegt.
 *  2. Der Umfang ist **beschränkt** ([RepositoryScope.isLeastPrivilege]).
 *  3. Der Nutzer hat die **externe Speicherung bestätigt**
 *     ([UploadDisclosure.externalStorageAcknowledged]).
 *
 * Getrennt von [GitAccessCatalog]: Der Katalog sagt, was ein Weg **ist**; diese
 * Klasse sagt, ob er **jetzt** benutzt werden darf.
 */
object GitAccessGate {

    /**
     * Darf [path] mit [scope] benutzt werden?
     *
     * Die Reihenfolge der Prüfungen ist fest und wird getestet: Eine offene Frage
     * schlägt einen zu weiten Umfang, und ein zu weiter Umfang schlägt eine
     * fehlende Bestätigung. Der Nutzer bekommt so immer **die erste** offene
     * Sache zu sehen, statt eine beliebige.
     */
    fun mayUse(
        path: GitAccessPath,
        scope: RepositoryScope,
        disclosure: UploadDisclosure
    ): GitAccessDecision = when {
        !path.isFullyDocumented -> GitAccessDecision.NotAcknowledged(
            buildList {
                add("Zu diesem Zugangsweg liegen offene Angaben vor; die Rechte sind damit nicht bekannt:")
                path.openQuestions.forEach { add(it.displayLine()) }
            }
        )

        !scope.isLeastPrivilege -> GitAccessDecision.ScopeTooBroad(
            "Der Umfang ist nicht auf einzelne Repositories beschränkt. " +
                scope.displayLine()
        )

        !disclosure.externalStorageAcknowledged -> GitAccessDecision.NotAcknowledged(
            listOf(
                "Der Nutzer hat die externe Speicherung nicht bestätigt. " +
                    "Nach dem Upload liegt der Inhalt außerhalb des Geräts."
            )
        )

        else -> GitAccessDecision.Allowed(
            buildList {
                addAll(disclosure.displayLines())
                add("Zugangsweg: ${path.displayName}")
            }
        )
    }
}
