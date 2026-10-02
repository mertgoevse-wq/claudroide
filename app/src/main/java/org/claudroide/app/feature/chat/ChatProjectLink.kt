package org.claudroide.app.feature.chat

import org.claudroide.app.feature.project.ContextCandidate
import org.claudroide.app.feature.project.ContextSelection
import org.claudroide.app.feature.project.ContextSelectionPolicy
import org.claudroide.app.feature.project.ProjectAccessRegistry
import org.claudroide.app.feature.project.ProjectBoundaryEnforcer

/**
 * Task 040 — „Chat und Projekt verbinden“.
 *
 * Eine Unterhaltung bekommt einen sichtbaren Projektbezug: zuordnen, wechseln,
 * lösen. Vier Zusagen aus dem Aufgabenbrief sind strukturell abgesichert, nicht
 * nur per Test:
 *
 *  1. **Der Nutzer erkennt, welche Dateien an die KI gehen könnten.**
 *     [ChatProjectLinkEngine.disclosureLines] nennt das verknüpfte Projekt, seinen
 *     Ordner und jede Datei, die derzeit als Kontext bereitstünde — mit Pfad,
 *     Zeilenbereich und Auswahlgrund, weil die Zeilen aus
 *     [ContextSelection.disclosureLines] stammen und nicht neu erfunden sind.
 *
 *  2. **Ein Projektwechsel gibt nicht still Dateien des alten Projekts weiter.**
 *     Der Kontext ist ein *PreparedContext* mit der Bindungs-Generation, für die er
 *     erstellt wurde. [ChatProjectLinkEngine.activateProject], [bind] und [unbind]
 *     erhöhen die Generation und verwerfen die Vorbereitung. Ein vorbereiteter
 *     Kontext aus einem anderen Projekt kann deshalb nicht mehr benutzt werden:
 *     [evaluateSend] meldet `CONTEXT_OUTDATED`, und es gibt keinen Parameter, mit dem
 *     sich eine alte Vorbereitung durchsetzen ließe.
 *
 *  3. **Die Verknüpfung erweitert den Projektbereich nicht.**
 *     Diese Klasse ruft an keiner Stelle `ProjectAccessRegistry.update`. Sie *fragt*
 *     die Grenze über [ProjectAccessRegistry.check] ab und bindet nur an einen
 *     Ordner, den die Grenze bereits erlaubt. Ein gebundenes Gespräch kann den
 *     gespeicherten Projektzugriff deshalb jederzeit überstimmen, ohne dass das
 *     Gespräch selbst etwas behalten könnte: wird der Ordner widerrufen, meldet
 *     [evaluateSend] `PROJECT_ACCESS_REVOKED`.
 *
 *  4. **Ein anderer aktiver Projektordner erzeugt eine Warnung, keine stille
 *     Änderung.** Aktiviert die App einen anderen Ordner, während die Unterhaltung an
 *     einem anderen hängt, bleibt die alte Bindung bestehen und der Versand wird
 *     blockiert (`ACTIVE_PROJECT_DIFFERS`). Erst [bind] mit ausdrücklicher
 *     Bestätigung löst die Warnung auf.
 *
 * Alles ist rein und synchron: Strings und Zahlen hinein, einfache Datenklassen
 * hinaus. Kein Android-Import, kein Netzwerk, kein Anbieteraufruf. Die Grenze wird
 * nicht *gesetzt*, sondern nur gelesen — das ist der Unterschied zwischen einer
 * Verknüpfung im Chat und einer neuen Berechtigung für die App.
 */
class ChatProjectLinkEngine(
    conversation: ManagedConversation =
        ManagedConversation(title = ConversationManager.DEFAULT_CHAT_TITLE),
    private val registry: ProjectAccessRegistry = ProjectAccessRegistry()
) {

    /** Ein Projektordner, wie die App ihn kennt. */
    data class ProjectRef(
        val id: String,
        val displayName: String,
        val rootPath: String
    ) {
        /** Ohne Namen oder ohne Ordner ist das keine nutzbare Verknüpfung. */
        val isUsable: Boolean
            get() = id.isNotBlank() && displayName.isNotBlank() && rootPath.isNotBlank()

        /** Die Zeile, die in Warnungen und Übersichten erscheint. */
        fun label(): String = "$displayName (${rootPath.trim()})"

        /** Zwei Ordner gelten als derselbe, wenn der Pfad derselbe ist. */
        fun sameRootAs(other: ProjectRef): Boolean =
            normaliseRoot(rootPath) == normaliseRoot(other.rootPath)
    }

    /**
     * Der Kontext, der für eine bestimmte Bindung vorbereitet wurde.
     *
     * @property bindingGeneration Generation der Bindung, für die diese Auswahl
     *           erstellt wurde. Sie ist der Grund, warum eine Vorbereitung nach einem
     *           Projektwechsel nicht mehr benutzt werden kann.
     * @property projectId das Projekt, zu dem die Auswahl gehört.
     */
    data class PreparedContext(
        val projectId: String,
        val bindingGeneration: Long,
        val selection: ContextSelection
    ) {
        /** Die Dateien, die bei einem Versand mitgingen. */
        fun filePaths(): List<String> = selection.included.map { it.path }
    }

    /** Warum ein Zuordnen, Wechseln oder Lösen abgelehnt wurde. */
    enum class LinkRefusal(val germanLabel: String) {
        PROJECT_UNUSABLE("Projektordner unbrauchbar"),
        PROJECT_NOT_PERMITTED("Projektordner nicht freigegeben"),
        PROJECT_NOT_ACTIVE("anderer Projektordner aktiv"),
        ALREADY_BOUND_TO_THIS("bereits diesem Projekt zugeordnet")
    }

    /**
     * Ergebnis von [isUsableForSend]: eine alte [PreparedContext] wird nicht mehr
     * angenommen, auch wenn der Aufrufer sie noch in der Hand hält.
     */
    data class ContextUsability(
        val isUsable: Boolean,
        val reason: SendBlock?,
        val message: String
    )

    /**
     * Der Warntext, der vor einem Projektwechsel im Weg steht.
     *
     * Er nennt beide Ordner und benennt die Dateien, die beim Wechsel wegfallen
     * würden. Das ist die konkrete Folge der Änderung, nicht nur „Achtung“.
     */
    data class ProjectSwitchWarning(
        val from: ProjectRef?,
        val to: ProjectRef,
        val filesDropped: List<String>,
        val filesGained: List<String>
    ) {
        /** Die Frage, die der Nutzer beantworten muss. */
        val question: String
            get() = "Unterhaltung von „${from?.label() ?: "ohne Projekt"}“ nach " +
                "„${to.label()}“ umhängen?"

        /** Zeilen für die Anzeige. */
        fun lines(): List<String> = buildList {
            add(from?.let { "Bisher verbunden mit: ${it.label()}" } ?: "Bisher mit keinem Projekt verbunden.")
            add("Danach verbunden mit: ${to.label()}")
            if (filesDropped.isNotEmpty()) {
                add("Diese Dateien werden nicht mehr mitgeschickt (${filesDropped.size}):")
                filesDropped.forEach { add("- $it") }
            }
            if (filesGained.isNotEmpty()) {
                add("Diese Dateien kämen neu hinzu (${filesGained.size}):")
                filesGained.forEach { add("- $it") }
            }
            if (filesDropped.isEmpty() && filesGained.isEmpty()) {
                add("Die Dateiauswahl bleibt leer — es wurden noch keine Dateien ausgewählt.")
            }
        }
    }

    /** Ergebnis einer Verknüpfungsänderung. */
    sealed class LinkOutcome {

        /** Die Änderung wurde ausgeführt. */
        data class Applied(
            val conversation: ManagedConversation,
            val binding: ProjectRef?,
            /** Deutsche Zeilen für die Oberfläche. */
            val notices: List<String>
        ) : LinkOutcome()

        /**
         * Der Wechsel braucht eine ausdrückliche Bestätigung. Nichts wurde
         * geändert — auch kein Kontext.
         */
        data class NeedsConfirmation(
            val warning: ProjectSwitchWarning
        ) : LinkOutcome()

        /** Abgelehnt, mit Grund. */
        data class Refused(
            val reason: LinkRefusal,
            val message: String
        ) : LinkOutcome()
    }

    /** Warum gerade nicht gesendet werden kann. */
    enum class SendBlock(val germanLabel: String) {
        PROJECT_ACCESS_REVOKED("Projektzugriff widerrufen"),
        ACTIVE_PROJECT_DIFFERS("anderer Projektordner aktiv"),
        CONTEXT_OUTDATED("Dateiauswahl gehört zum alten Projekt"),
        NOTHING_BOUND("kein Projekt verbunden")
    }

    /** Das Ergebnis von [evaluateSend]. */
    data class SendReadiness(
        val canSend: Boolean,
        val project: ProjectRef?,
        val block: SendBlock?,
        val message: String,
        val contextFiles: List<String>
    )

    private var conversation: ManagedConversation = conversation

    private var bound: ProjectRef? = null

    private var active: ProjectRef? = null

    private var generation: Long = 0L

    private var prepared: PreparedContext? = null

    /** Die zuletzt verworfene Vorbereitung, damit der Verlust benannt werden kann. */
    private var discarded: PreparedContext? = null

    // ── Zustand lesen ────────────────────────────────────────────────────────

    /** Die Unterhaltung in ihrem aktuellen, speicherbaren Zustand. */
    fun currentConversation(): ManagedConversation = conversation

    /** Das verknüpfte Projekt, oder `null`. */
    fun binding(): ProjectRef? = bound

    /** Der in der App gerade geöffnete Projektordner, oder `null`. */
    fun activeProject(): ProjectRef? = active

    /** Die aktuelle Bindungs-Generation. */
    fun bindingGeneration(): Long = generation

    /** Die aktuelle Vorbereitung, oder `null` wenn keine gültig ist. */
    fun preparedContext(): PreparedContext? =
        prepared?.takeIf { it.bindingGeneration == generation }

    /**
     * Prüft eine [PreparedContext], die der Aufrufer gespeichert hält, gegen den
     * aktuellen Stand.
     *
     * Das ist der Weg, den der Versand nimmt: Er nimmt nicht blind das, was er hat,
     * sondern fragt. Eine Vorbereitung aus einem früheren Projekt gilt hier nie als
     * gültig — weder nach einem Ordnerwechsel noch nach einem erneuten Binden.
     */
    fun isUsableForSend(context: PreparedContext?): ContextUsability {
        // Erst die eigene Vorbereitung prüfen: ein Aufrufer, der eine ältere
        // [PreparedContext] noch hält, erfährt hier den Grund, statt später eine
        // Datei aus dem falschen Projekt zu schicken.
        if (context != null &&
            (context !== prepared || context.bindingGeneration != generation)
        ) {
            return ContextUsability(
                isUsable = false,
                reason = SendBlock.CONTEXT_OUTDATED,
                message = "Diese Dateiauswahl gehört zum vorherigen Projekt und wurde " +
                    "verworfen. Wählen Sie die Dateien neu aus."
            )
        }
        // Danach die eine vollständige Prüfung — kein zweiter Weg, der die
        // Projektgrenze anders bewertet als der Versand.
        val readiness = evaluateSend()
        return ContextUsability(
            isUsable = readiness.canSend,
            reason = if (readiness.canSend) null else readiness.block,
            message = readiness.message
        )
    }

    // ── Verknüpfen, wechseln, lösen ──────────────────────────────────────────

    /**
     * Setzt den in der App geöffneten Projektordner.
     *
     * Das ist der Ordnerwechsel, vor dem gewarnt werden muss: Ist die Unterhaltung an
     * ein anderes Projekt gebunden, bleibt diese Bindung bestehen und
     * [evaluateSend] blockiert, bis der Nutzer sich entscheidet. Der alte Kontext
     * wird verworfen — Dateien des bisher aktiven Projekts können nicht mitlaufen.
     *
     * @return Was der Nutzer jetzt tun muss, wenn die Bindung abweicht.
     */
    fun activateProject(project: ProjectRef?): List<String> {
        active = project
        // Jeder Ordnerwechsel ist eine neue Bindungssituation: die Vorbereitung gilt
        // dann nicht mehr, auch wenn zufällig dasselbe Projekt wieder aktiviert wird.
        invalidateContext()
        val linked = bound
        if (project == null || linked == null || project.sameRootAs(linked)) {
            return emptyList()
        }
        return listOf(
            "Aktiv ist jetzt „${project.label()}“. Diese Unterhaltung ist weiterhin an " +
                "„${linked.label()}“ gebunden. Vor dem Senden muss der Projektbezug " +
                "ausdrücklich gewechselt werden."
        )
    }

    /**
     * Ordnet die Unterhaltung [project] zu.
     *
     * Ist die Unterhaltung bereits an ein anderes Projekt gebunden, wird ohne
     * [acknowledged] nichts geändert: es kommt eine [ProjectSwitchWarning] zurück,
     * die genau benennt, welche Dateien wegfallen und welche hinzukämen.
     *
     * @param candidates Dateien des neuen Projekts, damit die Warnung die Folge
     *        benennen kann. Sie werden hier **nicht** ausgewählt — die Auswahl
     *        entsteht erst über [prepareContext].
     * @param modelId Modell für die Budgetvorschau der Warnung; ohne belegtes Modell
     *        rechnet [ContextSelectionPolicy] bewusst konservativ.
     */
    fun bind(
        project: ProjectRef,
        acknowledged: Boolean = false,
        candidates: List<ContextCandidate> = emptyList(),
        modelId: String = "",
        todayEpochDays: Long = 0L
    ): LinkOutcome {
        if (!project.isUsable) {
            return LinkOutcome.Refused(
                LinkRefusal.PROJECT_UNUSABLE,
                "Der Projektordner hat keinen Namen oder keinen Pfad und kann nicht " +
                    "verknüpft werden."
            )
        }

        // Die Verknüpfung erweitert den Bereich nicht: sie prüft nur, ob die App
        // diesen Ordner bereits freigegeben hat. `update` wird hier bewusst nie
        // gerufen — genau das würde aus einer Chat-Zuordnung eine neue Berechtigung
        // machen.
        val verdict = registry.check(
            ProjectBoundaryEnforcer.ToolAction.file(
                kind = ProjectBoundaryEnforcer.ToolKind.READ,
                path = project.rootPath,
                origin = ProjectBoundaryEnforcer.ActionOrigin.USER
            )
        )
        if (!verdict.isAllowed) {
            return LinkOutcome.Refused(
                LinkRefusal.PROJECT_NOT_PERMITTED,
                "„${project.label()}“ ist für die App nicht freigegeben. " +
                    "Wählen Sie den Ordner zuerst in der App aus. ${verdict.displayMessage}"
            )
        }

        val current = bound
        if (current != null && current.sameRootAs(project)) {
            return LinkOutcome.Applied(
                conversation = conversation,
                binding = current,
                notices = listOf("Diese Unterhaltung ist bereits diesem Projekt zugeordnet.")
            )
        }

        val droppedFiles = preparedContext()?.filePaths().orEmpty()
        val gainedFiles = if (candidates.isEmpty()) {
            emptyList()
        } else {
            // Die Vorschau rechnet mit dem *Modell*, nicht mit der Projekt-ID: das
            // Budget hängt am Kontextfenster, und eine Projektbezeichnung ist keine
            // belegte Fenstergröße.
            ContextSelectionPolicy.select(modelId, candidates, todayEpochDays)
                .included.map { it.path }
        }

        if (current != null && !acknowledged) {
            return LinkOutcome.NeedsConfirmation(
                ProjectSwitchWarning(
                    from = current,
                    to = project,
                    filesDropped = droppedFiles,
                    filesGained = gainedFiles
                )
            )
        }

        if (active != null && !active!!.sameRootAs(project)) {
            return LinkOutcome.Refused(
                LinkRefusal.PROJECT_NOT_ACTIVE,
                "In der App ist gerade „${active!!.label()}“ geöffnet. " +
                    "Öffnen Sie erst diesen Ordner, bevor Sie ihn zuordnen."
            )
        }

        bound = project
        conversation = conversation.copy(
            projectId = project.id,
            projectName = project.displayName
        )
        invalidateContext()

        val notices = buildList {
            add(
                if (current == null) {
                    "Die Unterhaltung ist jetzt mit „${project.label()}“ verbunden."
                } else {
                    "Die Unterhaltung ist jetzt mit „${project.label()}“ verbunden statt " +
                        "mit „${current.label()}“."
                }
            )
            if (droppedFiles.isNotEmpty()) {
                add(
                    "${droppedFiles.size} zuvor ausgewählte Datei(en) wurden verworfen. " +
                        "Sie gehören zum alten Projekt und werden nicht mitgeschickt."
                )
            }
            add("Wählen Sie die Dateien für den neuen Projektordner neu aus.")
        }
        return LinkOutcome.Applied(conversation, project, notices)
    }

    /**
     * Löst die Verknüpfung.
     *
     * Danach ist die Unterhaltung an kein Projekt gebunden und es werden keine
     * Projektdateien mitgeschickt. Der verworfene Kontext bleibt verworfen: das
     * erneute Binden an dasselbe Projekt beginnt mit einer leeren Auswahl.
     */
    fun unbind(): LinkOutcome.Applied {
        val previous = bound
        val dropped = preparedContext()?.filePaths().orEmpty()
        bound = null
        conversation = conversation.copy(projectId = null, projectName = null)
        invalidateContext()
        return LinkOutcome.Applied(
            conversation = conversation,
            binding = null,
            notices = buildList {
                if (previous == null) {
                    add("Diese Unterhaltung war keinem Projekt zugeordnet.")
                } else {
                    add("Die Verbindung zu „${previous.label()}“ wurde gelöst.")
                }
                if (dropped.isNotEmpty()) {
                    add("${dropped.size} ausgewählte Datei(en) wurden verworfen.")
                }
                add("Es werden keine Projektdateien mitgeschickt.")
            }
        )
    }

    // ── Kontext vorbereiten ──────────────────────────────────────────────────

    /**
     * Bereitet den Kontext für das **aktuell verknüpfte** Projekt vor.
     *
     * Es gibt keinen Parameter, mit dem sich ein anderes Projekt angeben ließe: die
     * Auswahl wird immer aus [bound] abgeleitet. Ist kein Projekt verknüpft oder
     * stimmt es nicht mit dem aktiven Ordner überein, wird nichts vorbereitet.
     */
    fun prepareContext(
        candidates: List<ContextCandidate>,
        modelId: String,
        todayEpochDays: Long
    ): PreparedContext? {
        val project = bound ?: return null
        if (active != null && !active!!.sameRootAs(project)) return null
        val selection = ContextSelectionPolicy.select(project.id, candidates, todayEpochDays)
        prepared = PreparedContext(project.id, generation, selection)
        discarded = null
        return prepared
    }

    // ── Senden prüfen ────────────────────────────────────────────────────────

    /**
     * Darf mit dem aktuellen Stand gesendet werden, und was geht dabei mit?
     *
     * Die Reihenfolge ist Absicht: erst der widerrufene Zugriff, dann der aktive
     * Ordner, dann die Gültigkeit des Kontextes. Ein blockierter Grund wird genannt,
     * damit die Oberfläche sagen kann, was zu tun ist.
     */
    fun evaluateSend(): SendReadiness {
        val project = bound
        if (project == null) {
            return SendReadiness(
                canSend = true,
                project = null,
                block = SendBlock.NOTHING_BOUND,
                message = "Diese Unterhaltung ist keinem Projekt zugeordnet. " +
                    "Es werden keine Projektdateien mitgeschickt.",
                contextFiles = emptyList()
            )
        }

        val verdict = registry.check(
            ProjectBoundaryEnforcer.ToolAction.file(
                kind = ProjectBoundaryEnforcer.ToolKind.READ,
                path = project.rootPath,
                origin = ProjectBoundaryEnforcer.ActionOrigin.USER
            )
        )
        if (!verdict.isAllowed) {
            return SendReadiness(
                canSend = false,
                project = project,
                block = SendBlock.PROJECT_ACCESS_REVOKED,
                message = "Der Zugriff auf „${project.label()}“ wurde widerrufen. " +
                    "Aus diesem Projekt wird nichts gesendet.",
                contextFiles = emptyList()
            )
        }

        val current = active
        if (current != null && !current.sameRootAs(project)) {
            return SendReadiness(
                canSend = false,
                project = project,
                block = SendBlock.ACTIVE_PROJECT_DIFFERS,
                message = "Diese Unterhaltung hängt an „${project.label()}“, geöffnet ist " +
                    "aber „${current.label()}“. Wechseln Sie den Projektbezug ausdrücklich, " +
                    "damit keine Datei aus dem falschen Ordner mitgeht.",
                contextFiles = emptyList()
            )
        }

        val context = preparedContext()
        if (prepared == null && discarded != null) {
            return SendReadiness(
                canSend = false,
                project = project,
                block = SendBlock.CONTEXT_OUTDATED,
                message = "Die Dateiauswahl wurde beim Projektbezug verworfen. " +
                    "Betroffen waren: ${discarded!!.filePaths().joinToString(", ")}. " +
                    "Wählen Sie die Dateien im neuen Projekt neu aus.",
                contextFiles = emptyList()
            )
        }

        return SendReadiness(
            canSend = true,
            project = project,
            block = null,
            message = "",
            contextFiles = context?.filePaths().orEmpty()
        )
    }

    // ── Anzeige ──────────────────────────────────────────────────────────────

    /**
     * Alles, was die Oberfläche vor dem Senden zeigen muss: welches Projekt
     * verknüpft ist, welcher Ordner es ist, und welche Dateien bereitstünden.
     */
    fun disclosureLines(): List<String> = buildList {
        val project = bound
        if (project == null) {
            add("Projektbezug: keiner. Es werden keine Projektdateien mitgeschickt.")
        } else {
            add("Projektbezug: ${project.label()}")
            if (active != null && !active!!.sameRootAs(project)) {
                add("Achtung: In der App ist gerade „${active!!.label()}“ geöffnet.")
            }
        }
        val context = preparedContext()
        if (context == null) {
            add("Es ist keine Datei für dieses Gespräch ausgewählt.")
        } else {
            addAll(context.selection.disclosureLines())
        }
    }

    // ── Innere Helfer ────────────────────────────────────────────────────────

    /**
     * Verwirft die Vorbereitung und erhöht die Generation.
     *
     * Beides gehört zusammen: die alte Vorbereitung wird unbenutzbar *und* ihre
     * Gültigkeit lässt sich nicht mehr behaupten, weil die Generation, mit der sie
     * erstellt wurde, nicht mehr stimmt.
     */
    private fun invalidateContext() {
        prepared?.let { discarded = it }
        generation += 1
        prepared = null
    }
}

/**
 * Pfadvergleich ohne Dateisystem: Backslashes werden zu Schrägstrichen, leere und
 * „.“-Teile fallen weg. `…/ProjektA`, `…/ProjektA/` und `…\\ProjektA` sind damit
 * derselbe Ordner — sonst hinge die Bindung an der Schreibweise des Pfads.
 */
private fun normaliseRoot(path: String): String =
    path.trim().replace('\\', '/').split('/').filter { it.isNotEmpty() && it != "." }
        .joinToString("/")