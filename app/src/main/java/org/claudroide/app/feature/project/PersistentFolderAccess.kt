package org.claudroide.app.feature.project

/**
 * Task 083 — „Ordnerzugriff merken“.
 *
 * Ziel: Der Nutzer kann gewählte Projektordner nach einem Neustart der App
 * weiterverwenden und den Zugriff jederzeit widerrufen.
 *
 * Der Zugriff ruht auf Androids Storage Access Framework: der Nutzer wählt
 * einen Ordner im Systemdialog, die App sichert die URI-Berechtigung
 * dauerhaft (`takePersistableUriPermission`, siehe [PermissionManager]) und
 * kann sie nach einem Neustart wieder verwenden. Diese Klasse entscheidet,
 * was mit einer solchen gespeicherten Freigabe geschieht — sie öffnet den
 * Dialog nicht und greift nicht auf das Dateisystem zu.
 *
 * Die Zusicherungen der Aufgabe sind im Ablauf selbst verankert, nicht in
 * Anweisungen an den Aufrufer:
 *
 *  1. **Widerruf wirkt sofort.** [PersistentFolderAccess.revoke] entfernt
 *     die Freigabe aus dem Stand und erhöht die [generation]. Ein Urteil,
 *     das ein Aufrufer aus einer älteren Generation hält, ist danach
 *     unbrauchbar ([verdictStillValid]). Schreiben wird dadurch sofort
 *     gestoppt: [canWrite] prüft gegen den **aktuellen** Stand, und es
 *     gibt keinen Schalter, der eine widerrufene Freigabe wieder aktiviert.
 *
 *  2. **Die App ersetzt gelöschte oder verschobene Ordner nicht still.**
 *     [restore] prüft jede gespeicherte Freigabe gegen den
 *     [UriAvailability]-Abfrager. Ein Ordner, der nicht mehr erreichbar
 *     ist, wird **nicht** stillschweigend durch einen ähnlichen ersetzt und
 *     auch nicht stillschweigend behalten: er wandert in
 *     [RestoreReport.invalid] mit dem Grund, und die Oberfläche erfährt,
 *     dass der Nutzer erneut wählen muss. Es gibt keinen Code-Pfad, der
 *     einen ungültigen Eintrag repariert — das wäre eine erfundene
 *     Freigabe.
 *
 *  3. **Der Zugriff bleibt auf den gewünschten URI beschränkt.** Der Stand
 *     kennt ausschließlich URI-Zeichenketten, die der Nutzer selbst gewählt
 *     hat, und kein Feld für eine allgemeine Speicherberechtigung.
 *     [covers] vergleicht am Pfadtrenner, damit ein Nachbarordner mit
 *     ähnlichem Namen nicht durch eine ungenaue Prüfung durchrutscht.
 *
 *  4. **Kein Freigabezustand wird zwischengespeichert.** [isUsable] rechnet
 *     bei jedem Aufruf neu gegen den übergebenen Stand und den Abfrager; es
 *     gibt kein Feld `cached` und keine Methode, die ein Ergebnis merkt.
 *     Das ist die Regel aus dem Sicherheitsskill, angewendet auf die
 *     eigenen Freigaben: Ein widerrufener oder verschwundener Ordner wird
 *     erst beim Aufruf erkannt, nicht aus einem Erinnerungswert.
 *
 * Reines Kotlin: kein Android-Import, kein Context, kein ContentResolver.
 * Die Plattformarbeit (Persistierung, Freigabe) macht [PermissionManager];
 * diese Klasse ist die Entscheidung darüber, was ein gespeicherter Stand
 * bedeutet, und ist deshalb auf der JVM prüfbar.
 */

/** Wie der Nutzer einen Ordner freigibt oder freigibt. */
enum class PersistOutcome(val germanLabel: String) {

    /** Die Freigabe wurde dauerhaft gesichert. */
    PERSISTED("dauerhaft gesichert"),

    /**
     * Das System hat die dauerhafte Sicherung verweigert.
     *
     * Das ist kein Fehler des Nutzers, aber auch kein Zugriff: Ohne die
     * persistierte Berechtigung ist der Ordner nach dem Neustart weg.
     */
    NOT_PERSISTED("nicht dauerhaft gesichert")
}

/**
 * Abfrager, der meldet, ob ein gespeicherter URI noch erreichbar ist.
 *
 * In der Produktion fragt er den ContentResolver; im Test steht hier die
 * Wahrheit darüber, welche Ordner gelöscht oder verschoben wurden. Er
 * ist bewusst eine Funktion und kein Zustand, damit die Prüfung immer
 * gegen die aktuelle Realität läuft.
 */
fun interface UriAvailability {

    /**
     * @return `true`, wenn [uriString] unter dem angegebenen Zugriffsmodus
     *         noch geöffnet werden kann.
     */
    fun isAvailable(uriString: String, mode: AccessMode): Boolean
}

/** Der Zugriffsmodus, für den eine URI geprüft wird. */
enum class AccessMode(val germanLabel: String) {
    READ("Lesen"),
    WRITE("Schreiben")
}

/** Eine dauerhaft gesicherte Ordnerfreigabe. */
data class PersistedGrant(

    /**
     * Der URI-String, wie ihn der Systemdialog geliefert hat
     * (`content://…/tree/…`).
     *
     * Er dient **nur** der Plattform: Ihm entsprechen
     * [takePersistableUriPermission] und [releaseUriPermission] in
     * [PermissionManager]. Er ist opak — man kann keinen Pfad darin ablesen
     * und nichts darin per Präfix prüfen.
     */
    val uriString: String,

    /** Der vom System gemeldete Anzeigename des Ordners. */
    val displayName: String,

    /**
     * Der Pfad des Ordners im Dateisystem (`/storage/emulated/0/…`).
     *
     * **Warum beide:** URI und Pfad leben in getrennten Namensräumen. Ein
     * SAF-URI beginnt mit `content://` und lässt sich nicht in einen Pfad
     * umrechnen; ein Pfad lässt sich nicht als URI verwenden. Jede Grenz-
     * und Abdeckungsprüfung in dieser App rechnet deshalb mit Pfaden (wie
     * [FolderSelectionPolicy] und [ProjectAccessRegistry]), und der URI
     * bleibt der Griff, den Android für Persistierung und Freigabe braucht.
     * Vor dem Speichern wird der Pfad normalisiert; der Pfad ist die
     * vergleichbare Seite, nicht der URI.
     */
    val path: String,

    /**
     * `true`, wenn die Freigabe nur lesend gesichert wurde.
     *
     * Der Schreibzugriff ist ein eigener Zustand, keine Stufe davon: Ein
     * Ordner kann lesbar sein und trotzdem nicht beschreibbar.
     */
    val readOnly: Boolean = false
) {
    /** Der Pfad, wie er geprüft wird. */
    val normalisedPath: String
        get() = path.trim().replace('\\', '/').trimEnd('/')

    init {
        require(uriString.isNotBlank()) { "Eine Freigabe braucht einen URI." }
        require(displayName.isNotBlank()) { "Eine Freigabe braucht einen Namen." }
        require(path.isNotBlank()) { "Eine Freigabe braucht einen Pfad." }
    }
}

/** Das Ergebnis der Wiederherstellung nach einem Neustart. */
data class RestoreReport(

    /** Die Freigaben, die nachweislich noch erreichbar sind. */
    val usable: List<PersistedGrant>,

    /**
     * Die Freigaben, die nicht mehr erreichbar sind, mit dem Grund.
     *
     * Sie stehen hier, damit die Oberfläche sagt, welche Ordner der Nutzer
     * erneut wählen muss — statt so zu tun, als wäre alles wie vorher.
     */
    val invalid: List<InvalidGrant>
) {
    /** Wurde mindestens ein Ordner wiederhergestellt? */
    val hasUsable: Boolean get() = usable.isNotEmpty()

    /** War der gesamte gespeicherte Stand unbrauchbar? */
    val allInvalid: Boolean get() = invalid.isNotEmpty() && usable.isEmpty()

    /** Die Zeilen für die Oberfläche. */
    fun explanationLines(): List<String> = buildList {
        if (usable.isEmpty() && invalid.isEmpty()) {
            add("Es sind keine Ordnerfreigaben gespeichert.")
            return@buildList
        }
        if (usable.isNotEmpty()) {
            add("${usable.size} Ordner weiterhin erreichbar.")
            usable.forEach { add("  ${it.displayName}") }
        }
        invalid.forEach { add("Nicht mehr erreichbar: ${it.grant.displayName} (${it.reason})") }
        if (invalid.isNotEmpty()) {
            add("Bitte wähle die betroffenen Ordner erneut aus.")
        }
    }
}

/** Eine Freigabe, die nicht mehr nutzbar ist, mit dem Grund dafür. */
data class InvalidGrant(
    val grant: PersistedGrant,
    val reason: String
)

/** Wie eine Widerrufsaktion endete. */
sealed interface RevokeOutcome {

    /** Der Ordner wurde entzogen. */
    data class Revoked(val remainingRoots: List<String>) : RevokeOutcome

    /**
     * Der Ordner war gar nicht freigegeben.
     *
     * Das ist keine Fehlermeldung, sondern der ehrliche Fall: Ein zweiter
     * Widerruf desselben Ordners ändert nichts, und die Oberfläche soll
     * nicht behaupten, etwas widerrufen zu haben, was nie galt.
     */
    data class NotGranted(val descriptionLines: List<String>) : RevokeOutcome
}

/**
 * Der Stand der dauerhaft gesicherten Ordnerfreigaben.
 *
 * @property grants die Freigaben, die der Nutzer erteilt und die App
 *        dauerhaft gesichert hat, in der Reihenfolge der Wahl.
 * @property generation zählt jede Änderung hoch. Ein Urteil, das unter einer
 *        älteren Nummer entstand, ist danach unbrauchbar.
 */
data class PersistentFolderAccess(
    val grants: List<PersistedGrant> = emptyList(),
    val generation: Long = 0L
) {

    /** Wurde mindestens ein Ordner freigegeben? */
    val hasGrants: Boolean get() = grants.isNotEmpty()

    /** Alle gespeicherten Ordner-Pfade (Dateisystempfade). */
    val grantedPaths: List<String> get() = grants.map { it.normalisedPath }

    /**
     * Sichert eine neue Freigabe.
     *
     * @param grant die Freigabe aus dem Systemdialog.
     * @param persistResult wie das System die dauerhafte Sicherung bewertet
     *        hat. Wurde die Persistierung verweigert, entsteht **keine**
     *         Freigabe — der Ordner wäre nach dem Neustart ohnehin weg, und
     *         ihn trotzdem zu übernehmen würde einen Zugriff vortäuschen.
     * @return der neue Stand, oder `this`, wenn nichts gesichert wurde.
     */
    fun persist(grant: PersistedGrant, persistResult: PersistOutcome): PersistentFolderAccess {
        if (persistResult != PersistOutcome.PERSISTED) return this
        if (grants.any { it.normalisedPath == grant.normalisedPath }) return this
        return copy(grants = grants + grant, generation = generation + 1)
    }

    /**
     * Entzieht den Ordner [path].
     *
     * Der Eintrag wird entfernt und die Generation erhöht — damit wirkt der
     * Widerruf sofort, nicht beim nächsten Start. Es gibt keine Ausnahme
     * und keinen Schalter, der ihn rückgängig macht.
     */
    fun revoke(path: String): RevokeOutcome {
        val ziel = path.trim().replace('\\', '/').trimEnd('/')
        val trifft = grants.filterNot { it.normalisedPath == ziel }
        if (trifft.size == grants.size) {
            return RevokeOutcome.NotGranted(
                listOf(
                    "Für $path besteht keine Freigabe. Es wurde nichts " +
                        "widerrufen."
                )
            )
        }
        return RevokeOutcome.Revoked(
            remainingRoots = trifft.map { it.normalisedPath }
        )
    }

    /** Der Stand nach [revoke]. Getrennt, weil [revoke] nur das Ergebnis liefert. */
    fun stateAfterRevoke(path: String): PersistentFolderAccess {
        val ziel = path.trim().replace('\\', '/').trimEnd('/')
        val trifft = grants.filterNot { it.normalisedPath == ziel }
        return if (trifft.size == grants.size) this
        else copy(grants = trifft, generation = generation + 1)
    }

    /**
     * Liegt [path] in einem freigegebenen Ordner?
     *
     * Der Vergleich trennt am `/`: `/a/main.kt` gehört nicht zu `/a`. Ohne
     * den Trenner wäre ein Nachbarordner mit ähnlichem Namen durchgerutscht.
     */
    fun covers(path: String): Boolean {
        val normalisiert = path.trim().replace('\\', '/').trimEnd('/')
        if (normalisiert.isEmpty()) return false
        return grantedPaths.any { root ->
            normalisiert == root || normalisiert.startsWith("$root/")
        }
    }

    /**
     * Darf [path] in diesem Modus benutzt werden?
     *
     * Die Zugehörigkeit prüft am **Pfad** (siehe [PersistedGrant.path]), die
     * tatsächliche Erreichbarkeit am **URI** — weil nur Android weiß, ob ein
     * SAF-Ordner noch da ist. Die Prüfung läuft bei jedem Aufruf neu; es gibt
     * kein zwischengespeichertes Ergebnis. Ein Ordner, der vor einer Sekunde
     * noch da war, kann jetzt verschwunden sein, und genau das wird hier
     * erkannt.
     */
    fun isUsable(
        path: String,
        mode: AccessMode,
        availability: UriAvailability
    ): Boolean {
        val normalisiert = path.trim().replace('\\', '/').trimEnd('/')
        if (normalisiert.isEmpty()) return false
        return grants.any { grant ->
            val root = grant.normalisedPath
            val gehoert = normalisiert == root || normalisiert.startsWith("$root/")
            if (!gehoert) return@any false
            val modusPasst = when (mode) {
                AccessMode.READ -> true
                AccessMode.WRITE -> !grant.readOnly
            }
            modusPasst && availability.isAvailable(grant.uriString, mode)
        }
    }

    /**
     * Liegt ein Schreibzugriff auf [path] vor?
     *
     * Schreiben braucht die ausdrückliche Schreibberechtigung; ein
     * nur-lesender Ordner liefert hier `false`, auch wenn er lesbar ist.
     */
    fun canWrite(path: String, availability: UriAvailability): Boolean =
        isUsable(path, AccessMode.WRITE, availability)

    /** Liegt ein Lesezugriff auf [path] vor? */
    fun canRead(path: String, availability: UriAvailability): Boolean =
        isUsable(path, AccessMode.READ, availability)

    /**
     * Stellt den gespeicherten Stand nach einem Neustart her.
     *
     * Jede Freigabe wird gegen [availability] geprüft. Was nicht mehr
     * erreichbar ist, wird **nicht** übernommen und **nicht** ersetzt — es
     * wird mit dem Grund gemeldet, damit der Nutzer entscheidet, ob er den
     * Ordner neu wählt. Ein gelöschter Ordner wird also nicht still durch
     * einen gleichnamigen ersetzt, und ein verschobener Ordner wird nicht
     * still unter dem alten URI weitergeführt.
     *
     * @param availability der Abfrager für die aktuelle Erreichbarkeit.
     * @return der Bericht. Nur [RestoreReport.usable] sollte danach als
     *         neuer Stand übernommen werden; die ungültigen Einträge sind
     *         Information für den Nutzer, keine Freigaben.
     */
    fun restore(availability: UriAvailability): RestoreReport {
        val brauchbar = mutableListOf<PersistedGrant>()
        val unbrauchbar = mutableListOf<InvalidGrant>()
        grants.forEach { grant ->
            val lesbar = availability.isAvailable(grant.uriString, AccessMode.READ)
            val schreibbereit = grant.readOnly ||
                availability.isAvailable(grant.uriString, AccessMode.WRITE)
            when {
                lesbar && schreibbereit -> brauchbar.add(grant)
                else -> unbrauchbar.add(
                    InvalidGrant(
                        grant = grant,
                        reason = when {
                            !lesbar -> "Der Ordner ist gelöscht oder verschoben."
                            grant.readOnly -> "Der Ordner ist nur lesbar freigegeben."
                            else -> "Der Schreibzugriff wurde entzogen."
                        }
                    )
                )
            }
        }
        return RestoreReport(brauchbar, unbrauchbar)
    }

    /**
     * Ist ein Urteil aus [verdictGeneration] noch gültig?
     *
     * Ein Aufrufer, der vor dem Widerruf ein „darf schreiben“ erhalten
     * hat, hält ein Urteil der alten Generation. Nach
     * [revoke]/[persist]/[replaceAll] ist es unbrauchbar, und die Prüfung
     * sagt das — statt so zu tun, als gelte es weiter.
     */
    fun verdictStillValid(verdictGeneration: Long): Boolean =
        verdictGeneration == generation

    /**
     * Ersetzt den gesamten Stand durch [newGrants].
     *
     * Für den Fall, dass der Nutzer seine Auswahl vollständig erneuert:
     * Auch das ist eine Änderung und erhöht die Generation, damit alle
     * älteren Urteile verfallen.
     */
    fun replaceAll(newGrants: List<PersistedGrant>): PersistentFolderAccess =
        copy(grants = newGrants, generation = generation + 1)

    /** Die Zeilen für die Oberfläche. */
    fun explanationLines(): List<String> = buildList {
        if (!hasGrants) {
            add("Es ist kein Ordner dauerhaft freigegeben.")
            add("Ordner bleiben nach einem Neustart erhalten, solange der Nutzer sie nicht widerruft.")
            return@buildList
        }
        add("${grants.size} Ordner dauerhaft freigegeben.")
        grants.forEach { grant ->
            val modus = if (grant.readOnly) "nur Lesen" else "Lesen und Schreiben"
            add("  ${grant.displayName} ($modus)")
        }
    }
}
