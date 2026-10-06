package org.claudroide.app.feature.mcp

import org.claudroide.app.core.security.PermissionCategory

/**
 * Task 135 — "Werkzeugrechte und Daten": die Regeln der Werkzeugrechte.
 *
 * Reines Kotlin: die App liefert die Fakten, dieses Objekt entscheidet. Es wird
 * nichts aufgerufen, nichts gesendet und nichts gestartet.
 */
object McpToolRightsPolicy {

    /**
     * Muss jedes Recht einzeln bestätigt werden?
     *
     * **Immer.** Konstant `true`, damit keine spätere Änderung sie still
     * abschaltet, ohne dass ein Test auffällt.
     */
    fun rightsRequireUserConsent(): Boolean = true

    /**
     * Die Rechte, die einem externen Werkzeug **nie** eingeräumt werden.
     */
    fun neverGrantable(): Set<ToolRight> =
        ToolRight.entries.filter { it.isNeverGrantable }.toSet()

    /**
     * Die Kategorie, unter der diese Rechte im Permission Center stehen.
     *
     * Dieselbe Kategorie wie die Verbindung selbst aus Aufgabe 134.
     */
    fun permissionCategory(): PermissionCategory = PermissionCategory.EXTERNAL_TOOLS

    /** Der Zieltext, unter dem das Permission Center ein Werkzeug führt. */
    fun permissionGrantTarget(connection: McpToolConnection, toolName: String): String =
        "${connection.permissionGrantTarget()}#${toolName.trim()}"
}
