package org.claudroide.app.feature.mcp

/**
 * Task 135 — "Werkzeugrechte und Daten": die Rechte selbst und ihre Menge.
 *
 * [ToolRightSet] ist bewusst keine Datenklasse: es gibt kein `copy()`, das eine
 * bestätigte Menge nachträglich vergrößern könnte. `without` verkleinert,
 * vergrößern gibt es nicht.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz.
 */

// ── Das Recht ───────────────────────────────────────────────────────────────

/**
 * Ein Recht, das ein externes Werkzeug verlangen kann.
 *
 * [label] ist englisch (Sprache zuerst), [germanLabel] die Anzeige in der
 * deutschen Oberfläche.
 */
enum class ToolRight(val label: String, val germanLabel: String) {

    /** Projektdateien lesen. */
    READ_PROJECT_FILES("Read project files", "Projektdateien lesen"),

    /** Im Projekt schreiben. */
    WRITE_PROJECT_FILES("Write project files", "Projektdateien schreiben"),

    /** Im Projekt löschen. */
    DELETE_PROJECT_FILES("Delete project files", "Projektdateien löschen"),

    /** Befehle im Projekt ausführen. */
    RUN_COMMANDS("Run commands", "Befehle ausführen"),

    /** Auf das Netz zugreifen. */
    REACH_NETWORK("Reach the network", "auf das Netz zugreifen"),

    /**
     * Anmeldedaten oder Schlüssel auslesen.
     *
     * **Niemals freigebbar** — siehe [isNeverGrantable].
     */
    READ_CREDENTIALS("Read credentials", "Schlüssel auslesen"),

    /**
     * Den Projektbereich verlassen.
     *
     * **Niemals freigebbar** — siehe [isNeverGrantable].
     */
    LEAVE_PROJECT_BOUNDARY("Leave the project boundary", "Projektgrenze verlassen");

    /**
     * Ist dieses Recht grundsätzlich ausgeschlossen?
     *
     * Die beiden Verbote sitzen hier in der Aufzählung selbst — nicht in einer
     * Prüfung, die man beim Refaktorieren vergessen könnte.
     */
    val isNeverGrantable: Boolean
        get() = this == READ_CREDENTIALS || this == LEAVE_PROJECT_BOUNDARY
}

// ── Die Menge ────────────────────────────────────────────────────────────────

/**
 * Eine **unveränderliche** Menge von [ToolRight].
 */
class ToolRightSet private constructor(private val bits: Set<ToolRight>) {

    /** Wie viele Rechte enthalten sind. */
    val size: Int get() = bits.size

    /** Enthält die Menge kein Recht? */
    val isEmpty: Boolean get() = bits.isEmpty()

    /** Ist [right] enthalten? */
    operator fun contains(right: ToolRight): Boolean = right in bits

    /** Die Rechte als Liste, in der Reihenfolge der Aufzählung. */
    fun asList(): List<ToolRight> = ToolRight.entries.filter { it in bits }

    /**
     * Enthält diese Menge **alle** Rechte aus [other]?
     *
     * Das ist der Vergleich, der Zusage 2 trägt: eine Bestätigung deckt genau
     * die Rechte, die bestätigt wurden — nicht mehr.
     */
    fun includesAllOf(other: ToolRightSet): Boolean = other.bits.all { it in bits }

    /** Die Rechte, die in [other] stehen, aber nicht in dieser Menge. */
    fun missingFrom(other: ToolRightSet): List<ToolRight> = other.bits.filterNot { it in bits }

    /** Verkleinert die Menge. Es gibt bewusst **kein** `grantMore`. */
    fun without(right: ToolRight): ToolRightSet =
        if (right in bits) ToolRightSet(bits - right) else this

    /** Die Rechte als Anzeige. */
    fun displayLines(): List<String> =
        if (bits.isEmpty()) listOf("Rechte: keine") else bits.map { it.germanLabel }

    override fun equals(other: Any?): Boolean = other is ToolRightSet && other.bits == bits

    override fun hashCode(): Int = bits.hashCode()

    override fun toString(): String = "ToolRightSet(${bits.joinToString { it.name }})"

    companion object {

        /** Die leere Menge — „keine Rechte", nicht „alle Rechte". */
        val NONE: ToolRightSet = ToolRightSet(emptySet())

        /** Der einzige Weg zu einer Rechte-Menge. Doppelte fallen weg. */
        fun of(rights: Iterable<ToolRight>): ToolRightSet {
            val gewaehlt = LinkedHashSet<ToolRight>()
            ToolRight.entries.forEach { if (it in rights) gewaehlt.add(it) }
            return if (gewaehlt.isEmpty()) NONE else ToolRightSet(gewaehlt)
        }
    }
}
