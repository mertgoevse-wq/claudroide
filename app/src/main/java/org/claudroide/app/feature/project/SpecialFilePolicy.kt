package org.claudroide.app.feature.project

/**
 * Task 121 — „Links und Sonderdateien“.
 *
 * Ziel: Verhindern, dass ein symbolischer Link oder eine Sonderdatei einen
 * Zugriff außerhalb des Projekts ermöglicht.
 *
 * [PathBoundaryGuard] aus Aufgabe 120 entscheidet über **Pfade**. Diese Klasse
 * entscheidet über das, was ein Pfad *ist* — ob er ein Link ist, ob er auf ein
 * Gerät, eine Socket oder eine Pipe zeigt — und, vor allem: **ob das Ziel
 * überhaupt aufgelöst wurde, bevor darauf zugegriffen wird.**
 *
 * Die Kernzusage ist die aus dem Abschnitt „Schutz":
 *
 * > **Keine Linkverfolgung ohne erneute Begrenzungsprüfung.**
 *
 * Sie ist hier so verankert, dass sie nicht umgangen werden kann:
 *
 *  1. **Ein unaufgelöster Link wird nie benutzt.** [FileAccess.isPermitted]
 *     verlangt für jeden Zugriff ein **aufgelöstes** Ziel. Ein Link, dessen
 *     Ziel nicht ermittelt wurde ([SpecialFile.resolvedTarget == null]), gilt
 *     als `UNRESOLVED` und wird abgelehnt — auch dann, wenn der geschriebene
 *     Pfad sauber im Projekt liegt. Das ist der Unterschied zwischen „der Pfad
 *     sieht harmlos aus" und „der Pfad wurde geprüft".
 *
 *  2. **Der Grenzwächter entscheidet danach, nicht davor.** Nach der
 *     Auflösung läuft der echte Zielpfad durch [PathBoundaryGuard.check] mit
 *     dem **aufgelösten** Pfad. Ein Link im Projekt, der auf
 *     `/data/data/com.other.app/…` zeigt, landet damit beim selben Urteil wie
 *     ein direkt geschriebener Pfad — `SYMLINK_ESCAPE`.
 *
 *  3. **Sonderdateien werden nicht wie gewöhnliche Dateien behandelt.** Ein
 *     Gerät (`/dev/…`), eine Socket, eine Pipe oder ein FIFO kann blockieren,
 *     endlos lesen oder in fremde Systeme schreiben. [SpecialFileKind] kennt
 *     sie, und [SpecialFilePolicy.isOrdinaryFile] ist **nur** für gewöhnliche
 *     Dateien und Verzeichnisse wahr. Es gibt keinen Weg, eine Sonderdatei als
 *     „ganz normale Datei" zu deklarieren: [FileAccess] trägt die Art am Typ.
 *
 *  4. **Kein Überschreiben einer unsicheren Ziels.** Es wird weder gelesen
 *     noch geschrieben. [FileAccess.isPermitted] ist `false` für jeden
 *     aufgelösten Ausbruch, für unaufgelöste Links und für Sonderdateien — und
 *     [SpecialFilePolicy.explanation] sagt in **jedem** Fall, warum.
 *
 * Reines Kotlin: kein Android, kein Dateisystem. Die Auflösung eines Links
 * macht das Betriebssystem; diese Klasse entscheidet, was das Ergebnis bedeutet,
 * und ist damit auf der JVM prüfbar.
 */

/** Die Art eines Dateisystemeintrags, soweit sie für Zugriff wichtig ist. */
enum class SpecialFileKind(val germanLabel: String) {

    /** Eine gewöhnliche Datei. */
    REGULAR_FILE("Datei"),

    /** Ein Verzeichnis. */
    DIRECTORY("Ordner"),

    /**
     * Ein symbolischer Link.
     *
     * Ein Link ist kein Ziel. Was hinter ihm liegt, entscheidet — und das erst
     * nach Auflösung.
     */
    SYMLINK("Verweis"),

    /** Ein hart verknüpfter Eintrag (hard link) auf dieselbe Datei. */
    HARD_LINK("harte Verknüpfung"),

    /** Ein Gerät: `/dev/…`. Kann blockieren oder in Systeme schreiben. */
    DEVICE("Gerätedatei"),

    /** Ein Unix-Socket. */
    SOCKET("Socket"),

    /** Eine named Pipe (FIFO). Lesen kann endlos blockieren. */
    FIFO("Pipe"),

    /** Etwas, das die App nicht sicher einordnen konnte. */
    UNKNOWN("unbekannte Dateiart")
}

/** Wofür auf einen Eintrag zugegriffen werden soll. */
enum class AccessIntent(val germanLabel: String) {
    READ("lesen"),
    WRITE("schreiben"),
    DELETE("löschen"),
    EXECUTE("ausführen")
}

/**
 * Das Ergebnis der Einordnung eines Dateisystemeintrags.
 *
 * @property path der Pfad, **wie er geschrieben wurde**.
 * @property kind die erkannte Art.
 * @property resolvedTarget das echte Ziel nach Aufgelöstwerden von Verweisen,
 *        oder `null`, wenn es nicht ermittelt werden konnte. Genau dieses Feld
 *        trägt die Zusage: Ohne Ziel gibt es keinen Zugriff.
 */
data class SpecialFile(
    val path: String,
    val kind: SpecialFileKind,
    val resolvedTarget: String? = null
) {
    init {
        require(path.isNotBlank()) { "Ein Eintrag braucht einen Pfad." }
    }

    /**
     * Ist es ein Verweis, dessen Ziel noch offen ist?
     *
     * Das gilt für **beide** Verweisarten. Eine harte Verknüpfung ohne
     * ermitteltes Ziel ist genauso wenig geprüft wie ein symbolischer Link
     * ohne Ziel — sie nur für den symbolischen Link zu behandeln hieße, dass
     * `HARD_LINK` ohne Ziel ungeprüft durchkäme.
     */
    val isUnresolvedLink: Boolean
        get() = (kind == SpecialFileKind.SYMLINK || kind == SpecialFileKind.HARD_LINK) &&
            resolvedTarget == null

    /**
     * Wurde das Ziel dieses Eintrags tatsächlich ermittelt?
     *
     * Für einen Verweis ist das die entscheidende Frage. Für eine gewöhnliche
     * Datei genügt der Pfad selbst.
     */
    val targetIsKnown: Boolean
        get() = when (kind) {
            SpecialFileKind.SYMLINK, SpecialFileKind.HARD_LINK -> resolvedTarget != null
            else -> true
        }
}

/** Die Entscheidung über einen Zugriff, mit Begründung. */
data class FileAccessDecision(
    val allowed: Boolean,
    val reason: String,

    /** Welche Regel gegriffen hat — für die Anzeige nachvollziehbar. */
    val rule: AccessRule
)

/** Die Regel, die über einen Zugriff entschieden hat. */
enum class AccessRule(val germanLabel: String) {
    /** Gewöhnliche Datei innerhalb des Projekts. */
    ORDINARY_FILE("gewöhnliche Datei"),

    /** Der Pfad war sauber, das Ziel lag aber außerhalb (Verweis oder traversierung). */
    TARGET_OUTSIDE_PROJECT("Ziel außerhalb des Projekts"),

    /** Ein Verweis, dessen Ziel nicht ermittelt werden konnte. */
    UNRESOLVED_LINK("Verweis ohne bekanntes Ziel"),

    /** Eine Sonderdatei (Gerät, Socket, Pipe). */
    SPECIAL_FILE("Sonderdatei"),

    /** Etwas, das nicht sicher eingeordnet werden konnte. */
    UNKNOWN_FILE_TYPE("unbekannte Dateiart")
}

/**
 * Die Regel, wie Links und Sonderdateien behandelt werden.
 *
 * Bewusst eine reine Entscheidungsschicht: Sie liest keine Datei und öffnet
 * keinen Verweis. Was ein Pfad tatsächlich ist, liefert der Aufrufer als
 * [SpecialFile] — meist aus einem `lstat`, das keinen Verweis auflöst.
 */
object SpecialFilePolicy {

    /**
     * Darf auf [file] in der Absicht [intent] zugegriffen werden?
     *
     * Die Reihenfolge ist die Aussage:
     *  1. Unbekannte Dateiart → abgelehnt (lieber ratlos als unsicher).
     *  2. Sonderdatei → abgelehnt, mit dem Namen der Art.
     *  3. Verweis ohne ermitteltes Ziel → abgelehnt. **Vor** jeder Grenzprüfung,
     *     weil ohne Ziel gar nichts geprüft werden kann.
     *  4. Verweis mit Ziel → das **Ziel** entscheidet, gemeinsam mit
     *     [PathBoundaryGuard]. Das ist die Zusage „keine Linkverfolgung ohne
     *     erneute Begrenzungsprüfung" als Ablauf.
     *  5. Gewöhnliche Datei → der geschriebene Pfad entscheidet.
     *
     * Es wird **nie** gelesen und **nie** geschrieben; diese Funktion sagt nur
     * etwas darüber, ob es erlaubt wäre.
     */
    fun isPermitted(
        file: SpecialFile,
        projectRoot: String,
        intent: AccessIntent
    ): FileAccessDecision {
        if (file.kind == SpecialFileKind.UNKNOWN) {
            return FileAccessDecision(
                allowed = false,
                reason = "Die Art von „${file.path}“ konnte nicht sicher bestimmt werden. " +
                    "Es wird nicht ${intent.germanLabel}.",
                rule = AccessRule.UNKNOWN_FILE_TYPE
            )
        }

        if (file.kind.isSpecial()) {
            return FileAccessDecision(
                allowed = false,
                reason = "„${file.path}“ ist eine ${file.kind.germanLabel}. " +
                    "Darin wird nicht gelesen und nichts geschrieben.",
                rule = AccessRule.SPECIAL_FILE
            )
        }

        if (file.isUnresolvedLink) {
            return FileAccessDecision(
                allowed = false,
                reason = "„${file.path}“ ist ein Verweis, dessen Ziel nicht ermittelt " +
                    "werden konnte. Ohne bekanntes Ziel wird nicht ${intent.germanLabel}.",
                rule = AccessRule.UNRESOLVED_LINK
            )
        }

        // Ab hier ist das Ziel bekannt. Der **aufgelöste** Pfad entscheidet —
        // bei einem Verweis nicht der geschriebene.
        val entscheidenderPfad = file.resolvedTarget ?: file.path
        val grenzUrteil = PathBoundaryGuard.check(
            candidate = entscheidenderPfad,
            projectRoot = projectRoot,
            resolvedRealPath = entscheidenderPfad,
            realRoot = projectRoot
        )

        return when {
            grenzUrteil.isAllowed -> FileAccessDecision(
                allowed = true,
                reason = "„${file.path}“ liegt im Projektordner und darf ${intent.germanLabel} werden.",
                rule = AccessRule.ORDINARY_FILE
            )
            grenzUrteil.decision == PathBoundaryGuard.PathDecision.SYMLINK_ESCAPE -> FileAccessDecision(
                allowed = false,
                reason = "„${file.path}“ zeigt auf ein Ziel außerhalb des Projektordners " +
                    "„$projectRoot“. Dort wird nicht gelesen und nichts geschrieben.",
                rule = AccessRule.TARGET_OUTSIDE_PROJECT
            )
            else -> FileAccessDecision(
                allowed = false,
                reason = grenzUrteil.message.ifEmpty {
                    "„${file.path}“ liegt außerhalb des Projektordners. " +
                        "Dort wird nicht gelesen und nichts geschrieben."
                },
                rule = AccessRule.TARGET_OUTSIDE_PROJECT
            )
        }
    }

    /** Kurzform: nur die Erlaubnis. */
    fun permits(file: SpecialFile, projectRoot: String, intent: AccessIntent): Boolean =
        isPermitted(file, projectRoot, intent).allowed

    /** Ist es eine gewöhnliche Datei oder ein Verzeichnis? */
    fun isOrdinaryFile(file: SpecialFile): Boolean =
        file.kind == SpecialFileKind.REGULAR_FILE || file.kind == SpecialFileKind.DIRECTORY

    /** Muss der Pfad vor dem Zugriff aufgelöst werden? */
    fun needsResolution(file: SpecialFile): Boolean =
        file.kind == SpecialFileKind.SYMLINK || file.kind == SpecialFileKind.HARD_LINK

    /** Die Zeilen für die Oberfläche. */
    fun explanation(file: SpecialFile, decision: FileAccessDecision): List<String> = buildList {
        add("Datei: ${file.path}")
        add("Art: ${file.kind.germanLabel}")
        if (needsResolution(file)) {
            add(
                if (file.targetIsKnown) "Ziel: ${file.resolvedTarget}"
                else "Ziel: nicht ermittelt"
            )
        }
        add("Entscheidung: ${if (decision.allowed) "erlaubt" else "abgelehnt"} — ${decision.rule.germanLabel}")
        add(decision.reason)
    }

    /** Ist diese Art eine Sonderdatei, die keinen gewöhnlichen Zugriff trägt? */
    private fun SpecialFileKind.isSpecial(): Boolean = when (this) {
        SpecialFileKind.DEVICE,
        SpecialFileKind.SOCKET,
        SpecialFileKind.FIFO -> true
        else -> false
    }
}