package org.claudroide.app.feature.git

/**
 * Task 097 — „Privates GitHub-Projekt".
 *
 * Ziel: Ein privates Projektziel **einmal geprüft** festhalten, damit die
 * späteren Aufgaben 099 (Commit), 100 (Geheimnis-Scan) und 101 (Push-Freigabe)
 * auf eine belegte Tatsache bauen können statt auf eine Vermutung.
 *
 * Der Aufgabentext sagt „sofern es noch nicht besteht". Das ist eine Bedingung,
 * keine Aufforderung: Diese Datei **erstellt nichts**. Sie stellt den Zustand
 * dar, der von außen geprüft wurde, und lässt jede Behauptung über Sichtbarkeit,
 * Eigentümer oder Lizenz an der Quelle entscheiden, die sie betroffen hat.
 *
 * ## Die zwei Fertig-Bedingungen als Eigenschaft
 *
 *  1. *„Nutzer die tatsächliche Online-Erstellung ausdrücklich bestätigt."*
 *     [RemoteRepositoryPlan.creationConfirmedByUser] ist ein eigenes Feld mit
 *     Vorgabewert `false`, und [RepositoryGate.mayProceed] liest es **zuerst**.
 *     Aus keinem anderen Umstand wird es abgeleitet — nicht aus der Existenz
 *     des Projekts, nicht aus dem Namen, nicht aus dem Vorhandensein eines
 *     Zugangs. Auch ein bereits bestehendes Repository durchläuft diese Tür:
 *     „es gibt es schon" ist keine Bestätigung, sondern der Grund, warum hier
 *     nichts getan werden muss.
 *
 *  2. *„Privatheit vor dem ersten Upload sichtbar geprüft."*
 *     [RepositoryGate.auditBeforeFirstUpload] liest das **von außen gemeldete**
 *     Sichtbarkeitsergebnis [RemoteRepositoryAudit.visibilityVerifiedOnDevice].
 *     Ein lokales `true` aus der Absicht heraus genügt nicht: gefragt ist, was
 *     der Server **tatsächlich** zurückgibt. Ohne diese Messung bleibt der
 *     Audit `PENDING`, und [RepositoryGate.mayProceed] verweigert.
 *
 * ## Der Schutz: diese Datei spricht nicht mit GitHub
 *
 * Kein Netz, kein Dateisystem, kein Android. Das ist keine Vorsicht um der
 * Vorsicht willen, sondern die Grenze aus Aufgabe 096: dort trennt dieselbe
 * Trennung Plan und Ausführung. Ein Typ, der eine URL kennt, könnte ohne diese
 * Datei eine Erstellung auslösen, indem jemand `main(plan)` aufruft und die
 * Rückgabe für den Erfolg hält. Hier gibt es nichts aufzurufen.
 *
 * Reines Kotlin.
 */

// ── Das geprüfte Ziel ─────────────────────────────────────────────────────

/**
 * Das Projektziel: **wo** hin, **wer** besitzt es, **wie** ist es sichtbar.
 *
 * Der [owner] und das [visibility] werden nicht aus dem Namen abgeleitet. Eine
 * Ableitung wäre die bequemste und die gefährlichste Variante: `claudroide`
 * enthält kein `priv`, also wäre ein privates Repository aus dem Namen
 *geschlossen worden — und bei einem späteren Namenswechsel hätte dieser
 * Schluss stillschweigend weitergelaufen. Der Sichtbarkeitszustand kommt
 * deshalb ausschließlich von der Prüfung am Gerät.
 */
data class RemoteRepository(
    val owner: String,
    val repositoryName: String,
    val visibility: RepositoryVisibility,
    val license: RepositoryLicense = RepositoryLicense.NONE,
    val defaultBranch: String = "main"
) {
    init {
        require(owner.isNotBlank()) { "Ein Projektziel braucht einen Eigentümer." }
        require(owner.none { it.isISOControl() }) { "Ein Eigentümer kann kein Steuerzeichen enthalten." }
        require(repositoryName.isNotBlank()) { "Ein Projektziel braucht einen Namen." }
        require(!repositoryName.contains("..")) {
            "Ein Repositoryname darf nicht aus seinem Ordner ausbrechen."
        }
        require(defaultBranch.isNotBlank()) { "Ein Projektziel braucht einen Zweig." }
    }

    /** `besitzer/name`, wie es die Oberfläche zeigt. */
    fun displayName(): String = "$owner/$repositoryName"

    /**
     * Die Zieladresse in der Form, in der ein Klon sie erwartet — **nur** für
     * HTTPS. Über SSH wird hier bewusst nichts gebildet: das würde einen zweiten
     * Zugangsweg mit eigener Schlüsselverwaltung eröffnen, den diese Datei nicht
     * prüft. Aufgabe 095 (GitProviderAuth) entscheidet den Zugangsweg.
     */
    fun httpsCloneUrl(): String = "https://github.com/$owner/$repositoryName.git"
}

// ── Sichtbarkeit ──────────────────────────────────────────────────────────

/**
 * Wie sichtbar das Projekt ist — **gemeldet**, nicht gewünscht.
 *
 * [PRIVATE] und [PUBLIC] sind verschiedene Dinge und dürfen nicht ineinander
 * fallen. Es gibt bewusst keinen Vorgabewert: [RemoteRepository] lässt die
 * Sichtbarkeit nicht weg, weil „noch nicht geprüft" und „öffentlich" nicht
 * dasselbe sind und der Unterschied über den ersten Upload entscheidet.
 */
enum class RepositoryVisibility(val label: String, val germanLabel: String) {
    PRIVATE("private", "privat"),
    PUBLIC("public", "öffentlich");

    /**
     * Nur [PRIVATE] gilt als geschützt.
     *
     * Eine Eigenschaft und kein Vergleich in der aufrufenden Datei, damit kein
     * Aufrufer die Frage „ist das privat?" anders beantworten kann als der Typ
     * selbst. Ein Aufruf von `visibility != PUBLIC` hätte denselben Effekt für
     * heute, wäre aber eine Zusage über die Zukunft: sobald eine dritte
     * Sichtbarkeit dazukommt, wäre dieser Vergleich **falsch**, und der Fehler
     * wäre still.
     */
    val isPrivate: Boolean get() = this == PRIVATE
}

/**
 * Der Lizenzstatus — **gemeldet**, nicht behauptet.
 *
 * [NONE] ist ein eigener Wert und nicht `null`. „Keine Lizenz" ist eine
 * Tatsache, die man wissen muss (alles bleibt Eigentum; keine Weiterverwendung
 * ist erlaubt), und sie ist etwas anderes als „die Lizenz ist unbekannt".
 * Genau deshalb steht [NONE] hier und nicht `null`.
 */
enum class RepositoryLicense(val label: String, val germanLabel: String) {
    NONE("none", "keine"),
    PERMISSIVE("permissive", "frei"),
    COPYLEFT("copyleft", "durchschlagend"),
    PROPRIETARY("proprietary", "proprietär"),
    UNKNOWN("unknown", "unbekannt")
}

// ── Der Prüfbericht ───────────────────────────────────────────────────────

/**
 * Das, was die Prüfung am Gerät **tatsächlich** zurückgegeben hat.
 *
 * Erfunden wird hier nichts: die Klasse bekommt nur Werte, die von außen
 * kommen. Sie stellt keine Verbindung her und kennt keine Zugangsdaten.
 */
data class RemoteRepositoryAudit(
    val repository: RemoteRepository,
    /** Hat der Server das Ziel zum Zeitpunkt dieser Prüfung gefunden? */
    val foundOnServer: Boolean,
    /**
     * Wurde die Sichtbarkeit **am Gerät** gegengeprüft?
     *
     * Das ist der einzige Ort, an dem aus „privat" ein geprüfter Zustand wird.
     * Solange das `false` ist, gilt das Ziel für [RepositoryGate] als
     * ungeprüft — auch dann, wenn [RemoteRepository.visibility] bereits
     * `PRIVATE` ist. Die Absicht ist nicht die Messung.
     */
    val visibilityVerifiedOnDevice: Boolean,
    /** Wer auf das Ziel zugreifen darf — leer heißt: außer dem Eigentümer niemand. */
    val collaboratorLogins: List<String> = emptyList()
) {
    init {
        require(collaboratorLogins.none { it.isBlank() }) {
            "Ein leerer Zugangsname ist kein Zugang."
        }
    }

    /** Nur der Eigentümer? Der engstmögliche Umfang. */
    val isOwnerOnly: Boolean get() = collaboratorLogins.isEmpty()
}

// ── Der Plan ──────────────────────────────────────────────────────────────

/**
 * Was beim Einrichten des Projektziels **entschieden** wurde.
 *
 * [repositoryExists] und [creationConfirmedByUser] sind zwei getrennte Angaben,
 * und sie werden nicht vermischt. Die wichtigste Folge steht in [RepositoryGate]:
 * ein Ziel, das schon existiert, braucht **keine** Erstellung — und eine
 * Erstellung ist ohne ausdrückliche Bestätigung **nicht** möglich. Damit kann
 * kein Aufruf dieser Datei ein Repository anlegen, auch nicht durch einen
 * Umweg über eine plausible Begründung.
 */
data class RemoteRepositoryPlan(
    val repository: RemoteRepository,
    val repositoryExists: Boolean,
    /** Hat der Nutzer die tatsächliche Online-Erstellung ausdrücklich bestätigt? */
    val creationConfirmedByUser: Boolean = false
) {
    /**
     * Wurde die Online-Erstellung vom Nutzer bestätigt?
     *
     * Eine eigene Eingabe, aus **nichts** abgeleitet: nicht aus der Existenz,
     * nicht aus dem Namen, nicht aus dem Vorhandensein eines Zugangs. Vorgabe
     * `false`, damit ein unbestätigter Plan blockiert. [RepositoryGate.mayProceed]
     * prüft dieses Feld **zuerst**.
     */
    val creationConfirmed: Boolean get() = creationConfirmedByUser
}

// ── Die Regel ─────────────────────────────────────────────────────────────

/**
 * Die Regel, wann ein Projektziel benutzt werden darf.
 *
 * Zwei Fragen, zwei Methoden, und sie sind nicht vermischt:
 *
 *  * [mayProceed] — „darf dieses Ziel angelegt bzw. benutzt werden?"
 *  * [auditBeforeFirstUpload] — „ist die Privatheit geprüft?" Diese Frage wird
 *    **zuerst** gestellt, weil sie die Voraussetzung für alles Weitere ist.
 */
object RepositoryGate {

    /**
     * Die Prüfung **vor** dem ersten Upload.
     *
     * Liefert genau zwei Antworten, und die Absicht spielt in keine hinein:
     * entweder ist am Gerät gegengeprüft worden — dann [VisibilityAudit.Verified]
     * mit dem gemeldeten Zustand — oder es ist nicht geschehen — dann
     * [VisibilityAudit.Pending] mit dem Grund. Aus [RepositoryVisibility.PRIVATE]
     * allein folgt **nie** eine Prüfung.
     */
    fun auditBeforeFirstUpload(audit: RemoteRepositoryAudit): VisibilityAudit = when {
        !audit.foundOnServer -> VisibilityAudit.Pending(
            "Das Projektziel wurde am Gerät nicht gefunden. Ohne diesen Befund ist " +
                "nicht bekannt, ob das Ziel privat ist."
        )

        !audit.repository.visibility.isPrivate -> VisibilityAudit.Pending(
            "Das Ziel ist als ${audit.repository.visibility.germanLabel} gemeldet. " +
                "Ein öffentliches Ziel darf nicht als privat behandelt werden."
        )

        !audit.visibilityVerifiedOnDevice -> VisibilityAudit.Pending(
            "Die Privatheit wurde am Gerät nicht gegengeprüft. Ein Absichtswert ist " +
                "keine Messung."
        )

        else -> VisibilityAudit.Verified(
            audit.repository.visibility,
            "Die Privatheit wurde am Gerät geprüft: ${audit.repository.displayName()} " +
                "ist ${audit.repository.visibility.germanLabel}. " +
                "Der Umfang ist ${if (audit.isOwnerOnly) "nur der Eigentümer" else "mehrere Zugänge"}."
        )
    }

    /**
     * Darf mit diesem Ziel weitergearbeitet werden?
     *
     * Prüft in fester Reihenfolge: Erst die **ausdrückliche** Bestätigung, dann
     * die **geprüfte** Privatheit. In dieser Reihenfolge, weil sie die strengere
     * ist: ein Projektziel, das es noch gar nicht gibt, hat keine Privatheit,
     * über die man etwas prüfen könnte — die Bestätigung ist zuerst da.
     */
    fun mayProceed(plan: RemoteRepositoryPlan, audit: RemoteRepositoryAudit?): RepositoryDecision = when {
        !plan.creationConfirmed -> RepositoryDecision.NotConfirmed(
            if (plan.repositoryExists) {
                "Das Ziel ${plan.repository.displayName()} besteht bereits; es wird nichts " +
                    "angelegt. Für die weitere Arbeit ist keine Erstellung nötig."
            } else {
                "Das Ziel ${plan.repository.displayName()} besteht noch nicht und ist " +
                    "nicht bestätigt. Es wird nichts angelegt."
            }
        )

        audit == null -> RepositoryDecision.VisibilityNotChecked(
            "Die Privatheit wurde nicht geprüft. Es wird nichts angelegt und nichts gesendet."
        )

        auditBeforeFirstUpload(audit) !is VisibilityAudit.Verified -> RepositoryDecision.VisibilityNotChecked(
            (auditBeforeFirstUpload(audit) as VisibilityAudit.Pending).reason
        )

        else -> RepositoryDecision.MayProceed(plan.repository.displayName())
    }
}

// ── Die Antworten ─────────────────────────────────────────────────────────

/**
 * Das Ergebnis der Sichtbarkeitsprüfung: genau zwei Zustände.
 *
 * Ein dritter Zustand („vermutlich privat") existiert bewusst nicht.
 */
sealed interface VisibilityAudit {
    val germanLabel: String

    /** Am Gerät gegengeprüft. */
    data class Verified(
        val visibility: RepositoryVisibility,
        val message: String
    ) : VisibilityAudit {
        override val germanLabel: String get() = "privat geprüft"
    }

    /** Nicht gegengeprüft — mit dem Grund, der daran fehlt. */
    data class Pending(val reason: String) : VisibilityAudit {
        override val germanLabel: String get() = "ungeprüft"
    }
}

/** Die Antwort auf „darf weitergearbeitet werden?". */
sealed interface RepositoryDecision {
    val germanLabel: String

    /** Von Nutzerseite ausdrücklich bestätigt. */
    data class MayProceed(val displayName: String) : RepositoryDecision {
        override val germanLabel: String get() = "bestätigt"
    }

    /** Es gibt keine ausdrückliche Bestätigung. */
    data class NotConfirmed(val reason: String) : RepositoryDecision {
        override val germanLabel: String get() = "nicht bestätigt"
    }

    /** Die Privatheit ist nicht am Gerät geprüft. */
    data class VisibilityNotChecked(val reason: String) : RepositoryDecision {
        override val germanLabel: String get() = "Privatheit ungeprüft"
    }
}