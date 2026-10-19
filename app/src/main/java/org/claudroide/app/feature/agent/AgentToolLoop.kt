package org.claudroide.app.feature.agent

import org.claudroide.app.feature.project.ProjectAccessRegistry
import org.claudroide.app.feature.project.ProjectBoundaryEnforcer

/**
 * Task 072 — „Agentenwerkzeuge verbinden“.
 *
 * Ziel der Aufgabe: Modellantworten mit freigegebenen Werkzeugen verbinden — mit
 * Prüfung, Berechtigung, Ergebnis und Fortsetzung. Vier Zusagen sind strukturell
 * abgesichert, nicht nur per Test:
 *
 *  1. **Werkzeugaufrufe sind auf zugelassene Aktionen beschränkt.** Es gibt nur
 *     [AgentToolCatalog]. Ein Name, der dort nicht steht, wird abgelehnt, bevor
 *     überhaupt Argumente gelesen werden — es gibt keinen Fallback, der ein
 *     unbekanntes Werkzeug doch ausführt. Der Katalog enthält bewusst **kein**
 *     Werkzeug für Netzwerk, Zugangsschlüssel, Git-Upload, Installation oder
 *     fremde Fähigkeiten: diese Aktionen gehören eigenen Aufgaben mit eigenen
 *     Freigaben, und ein Werkzeugkatalog, der sie mit aufnimmt, würde die
 *     Freigabestufe eine Ebene zu tief legen.
 *
 *  2. **Jede Werkzeugaktion wird vor der Ausführung zweimal geprüft.** Erst gegen
 *     den Plan ([ExecutionPlan.grantedApprovals], je Schritt *und* je Stufe),
 *     dann gegen die Projektgrenze ([ProjectAccessRegistry]). Die beiden Prüfungen
 *     sind unabhängig: eine erteilte Freigabe ersetzt die Grenze nicht, und die
 *     Grenze ersetzt die Freigabe nicht. Die zweite Prüfung bekommt keinen Pfad
 *     aus dem Modell durchgereicht: [PreparedTool.command] wird hier aus einem
 *     erlaubten Aufgabenwert **gebaut**, nie aus Modelltext übernommen.
 *
 *  3. **Fehler und Abbruch sind keine Erfolge.** [ToolExecutionResult] trennt
 *     Erfolg und Misserfolg im Typ, [ToolResult] verbietet im `init` einen
 *     Zustand, in dem ein Misserfolg Ausgabe mit sich führen könnte, und
 *     [ToolLoopRun.isSuccess] verlangt zusätzlich, dass nichts übrig blieb.
 *     Ein Schritt, der nach zwei erfolgreichen Werkzeugen am dritten scheitert,
 *     ist ein Fehlschlag — die zwei vorherigen Erfolge machen ihn nicht zum Erfolg.
 *
 *  4. **Nebenwirkungen werden ehrlich verbucht.** Was ausgeführt wurde, steht in
 *     [ToolLoopRun.sideEffects] — auch dann, wenn der Lauf danach abbricht. Ein
 *     gelöschtes oder überschriebenes Datum verschwindet nicht dadurch, dass der
 *     Lauf später fehlschlug.
 *
 * Alles rein und synchron: Strings hinein, Datenklassen hinaus. Kein Android,
 * kein Netzwerk, kein Anbieteraufruf. Die eigentliche Ausführung macht der
 * Aufrufer über [ToolExecutor] — diese Klasse entscheidet nur, **ob** etwas
 * ausgeführt wird.
 */

/** Ein Werkzeugaufruf, wie ihn das Modell anbietet. */
data class ToolCall(
    val name: String,
    val arguments: Map<String, String> = emptyMap()
)

/**
 * Ein zugelassenes Werkzeug.
 *
 * @property boundaryKind welche Prüfung in [ProjectBoundaryEnforcer] gilt.
 * @property stepAction die Aktion aus [AgentTaskPlanner]; daraus wird die
 *           Freigabe **abgeleitet**, sie wird nicht angegeben. Ein Werkzeug kann
 *           sich seine Freigabepflicht also nicht selbst herabstufen.
 * @property allowedArguments exakt die erlaubten Argumentnamen. Ein zusätzlicher
 *           Schlüssel wird abgelehnt statt ignoriert — sonst wäre ein Argument
 *           möglich, das ein anderer Teil des Programms als Anweisung liest.
 */
data class RegisteredTool(
    val name: String,
    val boundaryKind: ProjectBoundaryEnforcer.ToolKind,
    val stepAction: StepAction,
    val germanLabel: String,
    val allowedArguments: Set<String>,
    val hasIrreversibleEffect: Boolean = false
) {
    /** Die benötigte Freigabestufe, abgeleitet wie beim Plan-Schritt. */
    val requiredApproval: RequiredApproval
        get() = stepAction.requiredApproval

    /**
     * Ob mindestens ein Argument nötig ist. Nur [run_test] hat keines: dort ist der
     * Aufgabenwert selbst das einzige Argument und hat einen festen Wertebereich.
     */
    val requiresArgument: Boolean
        get() = allowedArguments.isNotEmpty()
}

/**
 * Die zugelassenen Werkzeuge.
 *
 * Der Katalog ist die Grenze für „was ein Modell in diesem Aufbau aufrufen
 * darf“. Er ist absichtlich kurz und die Namen sind exakt: keine Präfix- oder
 * Teilstring-Übereinstimmung, damit `run_tests` nicht als `run_test` durchgeht.
 */
object AgentToolCatalog {

    /** Aufgaben, die [run_test] ausführen darf, mit dem Befehl, der daraus wird. */
    val ALLOWED_TEST_TASKS: Map<String, String> = mapOf(
        "unitTest" to "./gradlew :app:testDebugUnitTest",
        "assembleDebug" to "./gradlew :app:assembleDebug",
        "lint" to "./gradlew :app:lintDebug",
        "sync_frontmatter" to "python3 tools/sync_frontmatter.py --check"
    )

    /**
     * Kleinschreibung der Aufgabenliste.
     *
     * `unitTest` und `unittest“ müssen derselbe Auftrag sein — ein Modell schreibt
     * Groß- und Kleinschreibung nicht zuverlässig. Der *Befehl* entsteht trotzdem
     * nicht aus dem Modelltext, sondern wird hier aus der Liste geholt.
     */
    private val testTaskCommands: Map<String, String> =
        ALLOWED_TEST_TASKS.mapKeys { (key, _) -> key.lowercase() }

    /** Der Befehl für eine Aufgabe, oder `null` wenn sie nicht erlaubt ist. */
    fun commandFor(task: String): String? = testTaskCommands[task.trim().lowercase()]

    /** Alle zugelassenen Werkzeuge. */
    val ALL: List<RegisteredTool> = listOf(
        RegisteredTool(
            name = "read_file",
            boundaryKind = ProjectBoundaryEnforcer.ToolKind.READ,
            stepAction = StepAction.READ_FILE,
            germanLabel = "Datei lesen",
            allowedArguments = setOf("path")
        ),
        RegisteredTool(
            name = "list_directory",
            boundaryKind = ProjectBoundaryEnforcer.ToolKind.READ,
            stepAction = StepAction.READ_FILE,
            germanLabel = "Ordner auflisten",
            allowedArguments = setOf("path")
        ),
        RegisteredTool(
            name = "search_text",
            boundaryKind = ProjectBoundaryEnforcer.ToolKind.READ,
            stepAction = StepAction.READ_FILE,
            germanLabel = "Im Projekttext suchen",
            allowedArguments = setOf("path", "query")
        ),
        RegisteredTool(
            name = "write_file",
            boundaryKind = ProjectBoundaryEnforcer.ToolKind.WRITE,
            stepAction = StepAction.WRITE_FILE,
            germanLabel = "Datei schreiben",
            allowedArguments = setOf("path", "content")
        ),
        RegisteredTool(
            name = "delete_file",
            boundaryKind = ProjectBoundaryEnforcer.ToolKind.DELETE,
            stepAction = StepAction.DELETE_FILE,
            germanLabel = "Datei löschen",
            allowedArguments = setOf("path"),
            hasIrreversibleEffect = true
        ),
        RegisteredTool(
            name = "run_test",
            boundaryKind = ProjectBoundaryEnforcer.ToolKind.EXECUTE,
            stepAction = StepAction.RUN_COMMAND,
            germanLabel = "Test oder Prüfung starten",
            allowedArguments = setOf("task")
        )
    )

    /** Die zugelassenen Namen, für die Anzeige und Fehlermeldung. */
    val allowedNames: Set<String> = ALL.map { it.name }.toSet()

    /**
     * Das Werkzeug mit diesem Namen, oder `null`.
     *
     * `null` heißt „nicht zugelassen“ und wird vom Aufrufer **nicht** als
     * Arbeitsauftrag verstanden. Der Vergleich ist exakt nach Kleinschreibung und
     * nach Entfernen äußerer Leerzeichen.
     */
    fun find(name: String): RegisteredTool? =
        ALL.firstOrNull { it.name == name.trim().lowercase() }
}

/**
 * Das Ergebnis der Ausführung durch den Aufrufer.
 *
 * Erfolg und Misserfolg sind zwei verschiedene Typen. Es gibt deshalb keinen
 * Zustand, in dem ein Misserfolg eine Ausgabe mitschleppt, die wie ein Ergebnis
 * aussieht.
 */
sealed interface ToolExecutionResult {

    /** Der Werkzeugaufruf wurde ausgeführt. */
    data class Success(
        val output: String,
        /** Dateien, die dabei verändert oder angelegt wurden. */
        val changedFiles: List<String> = emptyList()
    ) : ToolExecutionResult

    /** Der Werkzeugaufruf ist fehlgeschlagen. */
    data class Failure(val message: String) : ToolExecutionResult
}

/**
 * Was tatsächlich ausgeführt werden darf.
 *
 * Enthält **keine** Modelltexte, die als Befehl durchreichen könnten: [command]
 * wurde aus einem erlaubten Aufgabenwert gebaut, [path] wurde gegen die
 * Projektgrenze geprüft.
 */
data class PreparedTool(
    val callIndex: Int,
    val rawCall: ToolCall,
    val tool: RegisteredTool,
    val path: String?,
    val query: String?,
    val command: String?
)

/**
 * Führt einen geprüften [PreparedTool] aus. Der Aufrufer liefert die
 * Implementierung; im Test ein simpler Auswertungsfunktion-ersatz.
 */
fun interface ToolExecutor {
    fun execute(call: PreparedTool): ToolExecutionResult
}

/** Warum der Werkzeuglauf aufgehört hat. */
enum class StopReason(val germanLabel: String) {
    REFUSED_BY_GATE("vom Sicherheits-Gate abgelehnt"),
    TOOL_FAILED("Werkzeug meldete einen Fehler"),
    ABORTED_BY_USER("vom Nutzer abgebrochen"),
    CALL_LIMIT_EXCEEDED("zu viele Werkzeugaufrufe in einem Schritt"),
    NO_CALLS("der Schritt enthält keinen Werkzeugaufruf")
}

/**
 * Das Ergebnis eines einzelnen Werkzeugaufrufs.
 *
 * Der `init`-Block macht die Zusage „Fehler ist kein Erfolg“ strukturell:
 * `COMPLETED` **muss** Ausgabe haben, jeder andere Zustand **darf** keine
 * Ausgabe haben. Es gibt keinen Constructor-Pfad zu einem Fehlschlag, der eine
 * Ausgabe trägt, die eine Oberfläche als Ergebnis anzeigen könnte.
 */
data class ToolResult(
    val callIndex: Int,
    val toolName: String,
    val status: ToolStatus,
    val message: String,
    val output: String? = null,
    val changedFiles: List<String> = emptyList()
) {
    init {
        require(status == ToolStatus.COMPLETED || output == null) {
            "Ein Aufruf mit Status $status darf keine Ausgabe tragen."
        }
        require(status != ToolStatus.COMPLETED || output != null) {
            "Ein abgeschlossener Aufruf ohne Ausgabe ist kein Ergebnis."
        }
    }

    val isSuccess: Boolean get() = status == ToolStatus.COMPLETED

    /** Die eine Zeile, die der Nutzer zu diesem Aufruf sieht. */
    fun transcriptLine(): String {
        val head = "$callIndex. $toolName"
        return when (status) {
            ToolStatus.COMPLETED ->
                if (changedFiles.isEmpty()) "$head — abgeschlossen: $output"
                else "$head — abgeschlossen, geändert: ${changedFiles.joinToString(", ")}"
            ToolStatus.REFUSED -> "$head — abgelehnt: $message"
            ToolStatus.FAILED -> "$head — fehlgeschlagen: $message"
            ToolStatus.ABORTED -> "$head — abgebrochen: $message"
        }
    }

    companion object {
        /** Ein erfolgreicher Aufruf. */
        fun completed(
            index: Int,
            toolName: String,
            output: String,
            changedFiles: List<String>
        ) = ToolResult(
            callIndex = index,
            toolName = toolName,
            status = ToolStatus.COMPLETED,
            message = "",
            output = output,
            changedFiles = changedFiles
        )

        /** Ein abgelehnter, **nicht** ausgeführter Aufruf. */
        fun refused(index: Int, toolName: String, message: String) = ToolResult(
            callIndex = index,
            toolName = toolName,
            status = ToolStatus.REFUSED,
            message = message
        )

        /** Ein ausgeführter, aber gescheiterter Aufruf. */
        fun failed(index: Int, toolName: String, message: String) = ToolResult(
            callIndex = index,
            toolName = toolName,
            status = ToolStatus.FAILED,
            message = message
        )

        /** Ein nie ausgeführter Aufruf nach einem Abbruch. */
        fun aborted(index: Int, toolName: String, message: String) = ToolResult(
            callIndex = index,
            toolName = toolName,
            status = ToolStatus.ABORTED,
            message = message
        )
    }
}

/** Der Zustand eines einzelnen Werkzeugaufrufs. */
enum class ToolStatus(val germanLabel: String) {
    COMPLETED("abgeschlossen"),
    REFUSED("abgelehnt"),
    FAILED("fehlgeschlagen"),
    ABORTED("abgebrochen")
}

/** Eine Nebenwirkung, die bereits eingetreten ist. */
data class CompletedSideEffect(
    val callIndex: Int,
    val toolName: String,
    val path: String?,
    val isIrreversible: Boolean,
    val germanNote: String
)

/**
 * Das Ergebnis eines ganzen Werkzeuglaufs für einen Plan-Schritt.
 *
 * @property results jedes Ergebnis, in Aufrufreihenfolge.
 * @property notExecuted Aufrufe, die nach dem Stopp **nicht** ausgeführt wurden.
 * @property sideEffects das, was tatsächlich passiert ist — auch wenn der Lauf
 *           danach abgebrochen wurde.
 */
data class ToolLoopRun(
    val stepId: String,
    val results: List<ToolResult>,
    val notExecuted: List<ToolCall>,
    val stopReason: StopReason?,
    val sideEffects: List<CompletedSideEffect> = emptyList()
) {
    /**
     * Ob der Lauf erfolgreich war.
     *
     * Vier Bedingungen zugleich: kein Stoppgrund, mindestens ein Aufruf, alle
     * Aufrufe `COMPLETED`, und nichts blieb übrig. Die letzte Bedingung ist die
     * wichtige: ein Lauf, der nach dem Limit aufhörte, hat nicht alles getan,
     * was der Schritt verlangte, und wäre sonst als Erfolg darstellbar.
     */
    val isSuccess: Boolean
        get() = stopReason == null &&
            results.isNotEmpty() &&
            notExecuted.isEmpty() &&
            results.all { it.isSuccess }

    /** `true`, wenn etwas nicht als Erfolg dargestellt werden darf. */
    val hasFailureOrAbort: Boolean get() = !isSuccess

    /** Die Ergebnisse in aufbereiteter Form. */
    fun successfulResults(): List<ToolResult> = results.filter { it.isSuccess }

    /**
     * Die ehrliche Zusammenfassung für den Nutzer.
     *
     * Genau ein Block, der sagt, wie viel ausgeführt wurde, warum aufgehört wurde
     * und was bereits passiert ist. Ein Lauf mit Abbruch kann hier nicht als
     * abgeschlossen erscheinen.
     */
    fun transcriptLines(): List<String> = buildList {
        add("Werkzeuglauf für Schritt $stepId:")
        if (results.isEmpty()) {
            add("Es wurde kein Werkzeug ausgeführt.")
        }
        results.forEach { add("  ${it.transcriptLine()}") }
        if (notExecuted.isNotEmpty()) {
            add("Nicht ausgeführt (${notExecuted.size}): ${notExecuted.joinToString(", ") { it.name }}")
        }
        stopReason?.let { add("Abbruchgrund: ${it.germanLabel}.") }
        if (sideEffects.isNotEmpty()) {
            add("Bereits eingetretene Änderungen (${sideEffects.size}):")
            sideEffects.forEach { add("  - ${it.germanNote}") }
        }
        add(
            if (isSuccess) {
                "Ergebnis: alle Werkzeugaufrufe abgeschlossen."
            } else {
                "Ergebnis: nicht erfolgreich. Dieser Lauf darf nicht als Erfolg " +
                    "dargestellt werden."
            }
        )
    }
}

/**
 * Verbindet Modellantworten mit Werkzeugen.
 *
 * Reihenfolge je Aufruf: Abbruchprüfung → Katalog → Argumente → Freigabe →
 * Projektgrenze → Ausführung. Jede Stufe kann nur nach links abbrechen; nichts
 * wird ausgeführt, bevor alle fünf bestanden sind.
 */
class AgentToolLoop(
    private val registry: ProjectAccessRegistry,
    private val plan: ExecutionPlan,
    private val executor: ToolExecutor,
    /** Obergrenze je Schritt, damit eine Werkzeugschleife endet. */
    private val maxCallsPerStep: Int = DEFAULT_MAX_CALLS
) {

    /**
     * Führt [calls] für [step] aus.
     *
     * @param isAborted wird vor jedem Aufruf geprüft, nicht nur einmal am Anfang:
     *         ein Abbruch während des dritten Aufrufs darf nicht erst nach dem
     *         vierten sichtbar werden.
     */
    fun run(
        step: PlanStep,
        calls: List<ToolCall>,
        isAborted: () -> Boolean = { false }
    ): ToolLoopRun {
        val results = ArrayList<ToolResult>()
        val notExecuted = ArrayList<ToolCall>()
        val sideEffects = ArrayList<CompletedSideEffect>()

        if (calls.isEmpty()) {
            return ToolLoopRun(
                stepId = step.id,
                results = emptyList(),
                notExecuted = emptyList(),
                stopReason = StopReason.NO_CALLS,
                sideEffects = emptyList()
            )
        }

        var stopReason: StopReason? = null

        for ((index, call) in calls.withIndex()) {
            if (isAborted()) {
                results += ToolResult.aborted(
                    index,
                    call.name,
                    "Der Nutzer hat den Vorgang abgebrochen. Dieser Aufruf wurde nicht ausgeführt."
                )
                notExecuted += calls.drop(index + 1)
                stopReason = StopReason.ABORTED_BY_USER
                break
            }

            if (index >= maxCallsPerStep) {
                // Ab hier läuft nichts mehr. Es werden *alle* übrigen Aufrufe
                // aufgeführt, nicht nur der nächste: sonst würde die Zusammenfassung
                // „nicht ausgeführt: 1“ sagen, während drei Aufrufe nie liefen.
                notExecuted += calls.drop(index)
                stopReason = StopReason.CALL_LIMIT_EXCEEDED
                break
            }

            val gate = gate(call, step, index)
            if (gate is Gate.Refused) {
                results += gate.result
                notExecuted += calls.drop(index + 1)
                stopReason = StopReason.REFUSED_BY_GATE
                break
            }

            val prepared = (gate as Gate.Prepared).call
            val execution = try {
                executor.execute(prepared)
            } catch (failure: Exception) {
                // Eine Exception ist ein Fehlschlag, kein Erfolg — auch dann nicht,
                // wenn im Werkzeug schon die halbe Arbeit geschehen ist.
                ToolExecutionResult.Failure(
                    failure.message?.takeIf { it.isNotBlank() }
                        ?: "Das Werkzeug ist unerwartet abgebrochen."
                )
            }

            when (execution) {
                is ToolExecutionResult.Success -> {
                    results += ToolResult.completed(
                        index = index,
                        toolName = prepared.tool.name,
                        output = execution.output,
                        changedFiles = execution.changedFiles
                    )
                    // Was ausgeführt wurde, wird verbucht — auch wenn der Lauf
                    // später abbricht. Eine überschriebene Datei verschwindet
                    // nicht, weil danach etwas schiefging.
                    sideEffects += noteFor(prepared, execution)
                }

                is ToolExecutionResult.Failure -> {
                    results += ToolResult.failed(index, prepared.tool.name, execution.message)
                    notExecuted += calls.drop(index + 1)
                    stopReason = StopReason.TOOL_FAILED
                    break
                }
            }
        }

        return ToolLoopRun(
            stepId = step.id,
            results = results,
            notExecuted = notExecuted,
            stopReason = stopReason,
            sideEffects = sideEffects
        )
    }

    // ── Die beiden Prüfungen ─────────────────────────────────────────────────

    /**
     * Katalog, Argumente, Freigabe und Projektgrenze — in dieser Reihenfolge.
     *
     * Die Argumente werden vor der Freigabe geprüft, damit die Meldung zuerst das
     * echte Problem nennt. Die Projektgrenze kommt zuletzt, weil sie die
     * aufwendigste und die letzte Instanz ist: Sie entscheidet über den *Ort*,
     * die Freigabe über die *Art*.
     */
    private fun gate(call: ToolCall, step: PlanStep, index: Int): Gate {
        val tool = AgentToolCatalog.find(call.name)
            ?: return Gate.Refused(
                ToolResult.refused(
                    index,
                    call.name,
                    "„${call.name.trim()}“ ist kein zugelassenes Werkzeug. " +
                        "Zugelassen sind: ${AgentToolCatalog.allowedNames.joinToString(", ")}."
                )
            )

        if (tool.requiresArgument) {
            val unexpected = call.arguments.keys - tool.allowedArguments
            if (unexpected.isNotEmpty()) {
                return Gate.Refused(
                    ToolResult.refused(
                        index,
                        tool.name,
                        "Unbekannte(r) Parameter: ${unexpected.joinToString(", ")}. " +
                            "Erlaubt sind: ${tool.allowedArguments.joinToString(", ")}."
                    )
                )
            }
            val missing = tool.allowedArguments - call.arguments.keys
            if (missing.isNotEmpty()) {
                return Gate.Refused(
                    ToolResult.refused(
                        index,
                        tool.name,
                        "Es fehlt: ${missing.joinToString(", ")}."
                    )
                )
            }
        }

        val approval = tool.requiredApproval
        if (approval.isSecurityRelevant && !plan.isGranted(step, approval)) {
            return Gate.Refused(
                ToolResult.refused(
                    index,
                    tool.name,
                    "Für „${tool.germanLabel}“ wurde die Freigabe " +
                        "„${approval.germanLabel}“ für diesen Schritt (${step.id}) " +
                        "nicht erteilt. Es wurde nichts ausgeführt."
                )
            )
        }

        val path = call.arguments["path"]?.trim()?.takeIf { it.isNotEmpty() }
        // Der Befehl wird hier gebaut, nicht aus dem Modelltext übernommen: der
        // Aufgabenwert wird gegen eine feste Liste geprüft. Ein Modell kann so
        // keinen eigenen Befehl einschleusen.
        val command: String? = if (tool.boundaryKind == ProjectBoundaryEnforcer.ToolKind.EXECUTE) {
            AgentToolCatalog.commandFor(call.arguments["task"].orEmpty())
        } else {
            null
        }

        if (tool.boundaryKind == ProjectBoundaryEnforcer.ToolKind.EXECUTE && command == null) {
            return Gate.Refused(
                ToolResult.refused(
                    index,
                    tool.name,
                    "„${call.arguments["task"]?.trim()}“ ist keine erlaubte Aufgabe. " +
                        "Erlaubt sind: ${AgentToolCatalog.ALLOWED_TEST_TASKS.keys.joinToString(", ")}."
                )
            )
        }

        if (tool.boundaryKind != ProjectBoundaryEnforcer.ToolKind.EXECUTE && path == null) {
            return Gate.Refused(
                ToolResult.refused(
                    index,
                    tool.name,
                    "Für „${tool.germanLabel}“ wurde kein Pfad angegeben."
                )
            )
        }

        // Die Grenze prüft das tatsächliche Ziel — dieselbe Prüfung, die auch ein
        // Aufruf aus der Oberfläche bekommen würde. Sie ist hier die letzte
        // Instanz; danach wird nichts mehr geprüft, sondern nur noch ausgeführt.
        val verdict = registry.check(
            ProjectBoundaryEnforcer.ToolAction(
                kind = tool.boundaryKind,
                rawKind = tool.name,
                path = path,
                command = command,
                origin = ProjectBoundaryEnforcer.ActionOrigin.MODEL
            )
        )

        if (!verdict.isAllowed) {
            return Gate.Refused(
                ToolResult.refused(
                    index,
                    tool.name,
                    verdict.displayMessage.ifBlank {
                        "Der Zugriff wurde vom Projekt-Gate abgelehnt."
                    }
                )
            )
        }

        return Gate.Prepared(
            PreparedTool(
                callIndex = index,
                rawCall = call,
                tool = tool,
                path = path,
                query = call.arguments["query"]?.trim()?.takeIf { it.isNotEmpty() },
                command = command
            )
        )
    }

    /** Verbucht eine tatsächlich eingetretene Änderung. */
    private fun noteFor(
        prepared: PreparedTool,
        execution: ToolExecutionResult.Success
    ): List<CompletedSideEffect> = buildList {
        val changed = execution.changedFiles.ifEmpty {
            listOfNotNull(prepared.path).filter { prepared.tool.hasIrreversibleEffect }
        }
        changed.forEach { file ->
            add(
                CompletedSideEffect(
                    callIndex = prepared.callIndex,
                    toolName = prepared.tool.name,
                    path = file,
                    isIrreversible = prepared.tool.hasIrreversibleEffect,
                    germanNote = if (prepared.tool.hasIrreversibleEffect) {
                        "„$file“ wurde gelöscht. Das lässt sich nicht zurücknehmen."
                    } else {
                        "„$file“ wurde verändert."
                    }
                )
            )
        }
    }

    /** Internes Ergebnis der Prüfkette. */
    private sealed interface Gate {
        data class Refused(val result: ToolResult) : Gate
        data class Prepared(val call: PreparedTool) : Gate
    }

    companion object {
        /**
         * Obergrenze je Schritt.
         *
         * Bewusst klein: eine Werkzeugschleife, die endlos läuft, ist auf einem
         * Telefon ein Akku- und Datenproblem, und sie erzeugt Kosten, wenn ein
         * Anbieter beteiligt ist. Was nicht in diese Grenze passt, braucht einen
         * eigenen Schritt im Plan.
         */
        const val DEFAULT_MAX_CALLS: Int = 12
    }
}