package org.claudroide.app.feature.mcp

import org.claudroide.app.feature.project.PathBoundaryGuard
import org.claudroide.app.feature.project.ProjectExclusionPolicy

/**
 * Task 135 — "Werkzeugrechte und Daten": Ansichtstypen und Datenvorschau.
 *
 * Reines Kotlin: kein Android, kein Dateisystem, kein Netz, kein MCP-Prozess.
 */

// ── Rechteansicht ────────────────────────────────────────────────────────────

/** Der Stand eines einzelnen Rechts in der Rechteansicht. */
enum class ToolRightState(val label: String, val germanLabel: String) {

    /** Für genau dieses Werkzeug in genau diesem Projekt bestätigt. */
    FREED("Confirmed", "bestätigt"),

    /** Nicht bestätigt — eine Bestätigung steht aus. */
    OPEN("Not confirmed", "offen"),

    /** Grundsätzlich ausgeschlossen und **nicht** bestätigbar. */
    NEVER_GRANTABLE("Never grantable", "grundsätzlich abgelehnt")
}

/** Eine Zeile der Rechteansicht: ein Recht und sein Stand. */
data class ToolRightRow(val right: ToolRight, val state: ToolRightState, val note: String)

/**
 * Die Rechteansicht: jedes Recht einzeln, mit seinem Stand.
 *
 * Sie listet **alle** [ToolRight] auf — auch die nie freigebaren. Eine Ansicht,
 * die die verbotenen Rechte weglässt, sähe so aus, als wären sie nicht verlangt
 * worden.
 */
data class ToolRightsView(
    val connectionId: String,
    val toolName: String,
    val projectId: String,
    val rows: List<ToolRightRow>
) {

    /** Steht für mindestens ein Recht eine Bestätigung aus? */
    val needsNewConfirmation: Boolean get() = rows.any { it.state == ToolRightState.OPEN }

    /** Die Rechte, die **nie** bestätigt werden können. */
    val forbiddenRights: List<ToolRightRow> get() = rows.filter { it.state == ToolRightState.NEVER_GRANTABLE }

    /** Die Zeilen für die Oberfläche. */
    fun displayLines(): List<String> = buildList {
        add("Rechte für „$toolName“ (Verbindung $connectionId, Projekt $projectId)")
        rows.forEach { add("  ${it.right.germanLabel}: ${it.state.germanLabel} — ${it.note}") }
    }
}

// ── Das Urteil ───────────────────────────────────────────────────────────────

/**
 * Die Erlaubnis, **einen** Werkzeugaufruf zu machen.
 *
 * Privater Konstruktor über das Begleitobjekt: Sie entsteht ausschließlich in
 * [McpPermissionDataView.authorize].
 */
class ToolCallPermit internal constructor(
    val connectionId: String,
    val projectId: String,
    val toolName: String,
    val approvalId: String,
    val rights: ToolRightSet,
    val issuedAt: Long
) {
    /** Kein Weg, aus einer Erlaubnis eine Handlung zu machen. */
    override fun toString(): String =
        "ToolCallPermit(connection=$connectionId, tool=$toolName, rights=${rights.size})"
}

/** Das Urteil über einen Werkzeugaufruf. */
sealed interface ToolCallDecision {

    /** Wurde abgelehnt? */
    val isRefused: Boolean get() = false

    /** Erlaubt — nur weil die angefragten Rechte bestätigt sind. */
    data class MayAct(val permit: ToolCallPermit) : ToolCallDecision

    /** Abgelehnt, mit allen Gründen und der Kennung der geprüften Bestätigung. */
    data class Refused(val reasons: List<String>, val approvalId: String) : ToolCallDecision {

        override val isRefused: Boolean get() = true
    }
}

// ── Abschaltbericht ──────────────────────────────────────────────────────────

/** Was beim Abschalten eines Werkzeugs passiert — und was ausdrücklich nicht. */
data class ToolDeactivationReport(
    val connectionId: String,
    val wasEnabled: Boolean,
    val descriptionLines: List<String>
) {

    /** War überhaupt etwas abzuschalten? */
    val changedSomething: Boolean get() = wasEnabled
}

// ── Datenvorschau ────────────────────────────────────────────────────────────

/** Ein Ziel, das **nicht** an das Werkzeug gelangt — mit seinem Grund. */
data class WithheldTarget(val path: String, val reason: String)

/**
 * Was ein Werkzeug in **einem** Aufruf zu sehen bekäme.
 *
 * **Kein Feld trägt Inhalt.** Diese Vorschau nennt Pfade und Gründe, keinen
 * Text, keinen Rückgabewert, keinen Schlüssel.
 */
data class McpToolDataPreview(
    val connectionId: String,
    val projectRoot: String,
    val visiblePaths: List<String>,
    val withheld: List<WithheldTarget>
) {

    /** Darf mit dieser Vorschau überhaupt etwas angefasst werden? */
    val mayProceed: Boolean get() = visiblePaths.isNotEmpty() || withheld.isNotEmpty()

    /**
     * Die Grenze als eigener Satz — nicht als Fußnote.
     *
     * Die Vorschau liest keine Datei und öffnet keinen Ordner.
     */
    val boundaryNotice: String
        get() = "Die Vorschau hat nur Pfade und Gründe geprüft. " +
            "Es wurde keine Datei gelesen und kein Werkzeug ausgeführt."

    /** Die Zeilen für die Oberfläche — auch das, was **nicht** herausgeht. */
    fun displayLines(): List<String> = buildList {
        add("Werkzeug: $connectionId")
        add("Projektordner: ${projectRoot.ifBlank { "offen" }}")
        add("Sichtbar für das Werkzeug: ${visiblePaths.size}")
        visiblePaths.forEach { add("  • $it") }
        if (withheld.isNotEmpty()) {
            add("Withheld: ${withheld.size}")
            withheld.forEach { add("  • ${it.path}: ${it.reason}") }
        }
        add(boundaryNotice)
    }
}

// ── Zielprüfung (intern) ─────────────────────────────────────────────────────

/** Das Urteil über ein einzelnes Ziel. */
internal sealed interface ZielUrteil {
    val pfad: String

    /** Sichtbar für das Werkzeug. */
    data class Sichtbar(override val pfad: String) : ZielUrteil

    /** Withheld, mit Grund. */
    data class Withheld(override val pfad: String, val grund: String) : ZielUrteil
}

/**
 * Prüft **ein** Ziel. Die Reihenfolge ist die Aussage:
 *
 *  1. ein URI ist kein Pfad — abgelehnt, nicht umgerechnet,
 *  2. die Projektgrenze ([PathBoundaryGuard]) entscheidet,
 *  3. die Geheimnisgrenze ([ProjectExclusionPolicy]) entscheidet.
 */
internal fun classify(
    raw: String,
    projectRoot: String,
    existingPaths: Set<String>?
): ZielUrteil {
    val pfad = raw.trim()
    if (pfad.isEmpty()) {
        return ZielUrteil.Withheld(pfad, "Der Pfad ist leer. Es wurde nichts geöffnet.")
    }

    schemeOf(pfad)?.let { schema ->
        return ZielUrteil.Withheld(
            pfad,
            "„$schema“ ist kein Projektpfad. Ein URI wird nicht in einen Pfad umgerechnet, " +
                "sondern abgelehnt."
        )
    }

    val grenze = PathBoundaryGuard.check(pfad, projectRoot)
    if (!grenze.isAllowed) {
        return ZielUrteil.Withheld(pfad, grenze.message)
    }

    val relativ = pfad.replace('\\', '/').trimStart('/')
    val vorhanden = existingPaths == null || relativ in existingPaths
    val ausschluss = ProjectExclusionPolicy.classify(relativ, exists = vorhanden)
    return when (ausschluss.decision) {
        ProjectExclusionPolicy.FileDecision.ALLOWED -> ZielUrteil.Sichtbar(relativ)
        else -> ZielUrteil.Withheld(pfad, ausschluss.message)
    }
}

/** Das Schema eines URI — oder `null`, wenn es keiner ist. */
private fun schemeOf(pfad: String): String? =
    SCHEME.find(pfad)?.groupValues?.get(1)?.lowercase()

private val SCHEME = Regex("^([A-Za-z][A-Za-z0-9+.\\-]*)://")
