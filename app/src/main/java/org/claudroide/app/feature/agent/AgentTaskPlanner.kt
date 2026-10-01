package org.claudroide.app.feature.agent

/**
 * Task 071 — „Aufgaben planen“.
 *
 * Ziel der Aufgabe: komplexe Projektaufträge in kleine, nachvollziehbare
 * Schritte übersetzen. Das Ergebnis ist ein Plan mit Reihenfolge, betroffenen
 * Dateien, Risiken und nötigen Freigaben — nicht die Ausführung.
 *
 * Drei Zusagen der Aufgabe sind strukturell abgesichert, nicht nur per Test:
 *
 *  1. **Kein Plan überspringt eine Sicherheitsfreigabe still.** Das ist die
 *     Schutzregel dieser Aufgabe, und sie ist so gebaut, dass ein Fehler nicht
 *     durch Vergessen entstehen kann: [PlanStep.requiredApproval] ist eine
 *     *abgeleitete* Eigenschaft. Ein Schritt gibt [PlanStep.actions] an, und
 *     die benötigte Freigabe folgt daraus über [StepAction.requiredApproval].
 *     Es gibt kein Feld, in dem jemand `approval = NONE` eintragen könnte —
 *     ein Schritt, der eine Datei schreibt, kann seine Freigabe also nicht
 *     selbst für unnötig erklären.
 *
 *  2. **Unklare Anforderungen erscheinen als Frage, nicht als Annahme.**
 *     [AgentTaskPlanner.plan] liefert nie einen ausführbaren Plan, solange
 *     [Clarification] offen ist. [AgentTaskPlanner.detectMissingActions] prüft
 *     zusätzlich das Gegenteil: eine im Auftrag genannte, im Plan aber nicht
 *     abgedeckte Aktion wird als Frage gemeldet, damit sie nicht still
 *     wegfällt.
 *
 *  3. **Der Nutzer kann den Plan vor folgenreichen Aktionen prüfen.**
 *     [ExecutionPlan.reviewLines] nennt für jeden Schritt Reihenfolge, Dateien,
 *     Risiko und die nötige Freigabe im Klartext, und [ExecutionPlan.canStart]
 *     bleibt `false`, solange eine Freigabe nicht ausdrücklich erteilt wurde.
 *
 * Wie bei den anderen Aufgaben dieses Projekts gilt: reines Kotlin, keine
 * Android-Importe, kein Netzwerk, kein Zustand außerhalb der Objekte. Der Plan
 * entscheidet nichts über die Ausführung — er sagt, was der Nutzer erst prüfen
 * muss.
 */

/**
 * Was ein einzelner Plan-Schritt tut.
 *
 * Die Aufzählung ist absichtlich grob und vollständig gemeint: eine Aktion, die
 * hier fehlt, umgeht die Freigabeableitung. Wer eine neue Art von Aktion
 * braucht, ergänzt sie hier **und** [StepAction.requiredApproval] — nicht in
 * einem Plan.
 */
enum class StepAction(val germanLabel: String) {

    /** Eine Datei im Projektordner lesen. Löst keine Freigabe aus. */
    READ_FILE("Datei lesen"),

    /** Eine Datei im Projektordner ändern oder anlegen. */
    WRITE_FILE("Datei ändern"),

    /** Etwas löschen, das nicht einfach zurückholbar ist. */
    DELETE_FILE("Datei löschen"),

    /** Einen Befehl im freigegebenen Projektbereich ausführen. */
    RUN_COMMAND("Befehl ausführen"),

    /** Daten an einen Anbieter außerhalb des Geräts schicken. */
    NETWORK_CALL("Daten an einen Anbieter senden"),

    /** Einen hinterlegten Zugangsschlüssel verwenden. */
    USE_API_KEY("Zugangsschlüssel verwenden"),

    /** Ein Programm, Paket oder eine Abhängigkeit nachinstallieren. */
    INSTALL_DEPENDENCY("Abhängigkeit installieren"),

    /** Einen lokalen Git-Stand benennen. */
    GIT_COMMIT("Git-Stand sichern"),

    /** Einen Git-Stand an einen Server hochladen. */
    GIT_PUSH("Git-Upload"),

    /** Eine fremde Fähigkeit ausführen, deren Herkunft ungeprüft ist. */
    RUN_THIRD_PARTY_SKILL("Fremde Fähigkeit ausführen");

    /**
     * Die Freigabe, die diese Aktion zwingend braucht.
     *
     * [NONE] gibt es bewusst nur für [READ_FILE] und [GIT_COMMIT]: beides ist
     * umkehrbar — eine gelesene Datei wird nicht verändert, ein lokaler
     * Commit lässt sich verwerfen. Alles andere, was das Gerät oder fremde
     * Systeme betrifft, braucht eine ausdrückliche Freigabe.
     */
    val requiredApproval: RequiredApproval
        get() = when (this) {
            READ_FILE -> RequiredApproval.NONE
            WRITE_FILE -> RequiredApproval.FILE_CHANGE
            DELETE_FILE -> RequiredApproval.IRREVERSIBLE_DELETE
            RUN_COMMAND -> RequiredApproval.COMMAND_RUN
            NETWORK_CALL -> RequiredApproval.EXTERNAL_TRANSFER
            USE_API_KEY -> RequiredApproval.SECRET_USE
            INSTALL_DEPENDENCY -> RequiredApproval.DEPENDENCY_INSTALL
            GIT_COMMIT -> RequiredApproval.NONE
            GIT_PUSH -> RequiredApproval.GIT_PUSH
            RUN_THIRD_PARTY_SKILL -> RequiredApproval.THIRD_PARTY_SKILL
        }
}

/**
 * Eine Freigabestufe. Jede Stufe wird vor der Ausführung einzeln bestätigt.
 *
 * @property isSecurityRelevant `true` für alles außer [NONE]. Die Oberfläche
 *           nutzt das, um Freigaben hervorzuheben; ein Plan ohne
 *           sicherheitsrelevante Freigabe läuft auf einem anderen Weg als einer
 *           mit, und das ist eine sichtbare Eigenschaft.
 */
enum class RequiredApproval(val germanLabel: String, val isSecurityRelevant: Boolean) {
    NONE("keine Freigabe nötig", false),
    FILE_CHANGE("Änderung an einer Datei freigeben", true),
    COMMAND_RUN("Ausführung eines Befehls freigeben", true),
    EXTERNAL_TRANSFER("Übertragung von Daten nach außen freigeben", true),
    SECRET_USE("Verwendung des Zugangsschlüssels freigeben", true),
    DEPENDENCY_INSTALL("Installation einer Abhängigkeit freigeben", true),
    GIT_PUSH("Git-Upload freigeben", true),
    IRREVERSIBLE_DELETE("Löschen freigeben", true),
    THIRD_PARTY_SKILL("Ausführung einer fremden Fähigkeit freigeben", true)
}

/** Wie gefährlich ein Schritt ist. */
enum class StepRisk(val germanLabel: String) {
    LOW("gering"),
    MEDIUM("mittel"),
    HIGH("hoch")
}

/**
 * Ein einzelner Plan-Schritt.
 *
 * [requiredApproval] ist bewusst *nicht* Teil der Konstruktion: der Schritt
 * beschreibt, **was** er tut, und die Freigabe wird daraus abgeleitet. Damit
 * kann kein Schritt seine eigene Freigabepflicht herunterstufen.
 *
 * @property files betroffene Projektpfade. Wird für die Parallelitätsprüfung
 *           benutzt: zwei Schritte mit einer gemeinsamen Datei laufen nie
 *           gleichzeitig.
 * @property dependsOn Kennungen der Schritte, die vorher fertig sein müssen.
 */
data class PlanStep(
    val id: String,
    val title: String,
    val description: String,
    val actions: Set<StepAction>,
    val files: List<String> = emptyList(),
    val dependsOn: List<String> = emptyList(),
    val notes: List<String> = emptyList()
) {
    /**
     * Die benötigte Freigabe — der höchste Wert aller Aktionen dieses Schritts.
     *
     * Abgeleitet, nicht angegeben. Ein Schritt, der eine Datei schreibt und
     * einen Befehl ausführt, braucht also beide Freigaben, nicht die
     * bequemere davon.
     */
    val requiredApproval: RequiredApproval
        get() = actions.maxByOrNull { it.requiredApproval.ordinal }?.requiredApproval
            ?: RequiredApproval.NONE

    /** Ein Schritt, der nur liest, ist nach [StepRisk] gering. */
    val risk: StepRisk
        get() = when {
            requiredApproval == RequiredApproval.NONE -> StepRisk.LOW
            requiredApproval in HIGH_RISK_APPROVALS -> StepRisk.HIGH
            else -> StepRisk.MEDIUM
        }

    companion object {
        /** Freigaben, die als hohes Risiko gelten. */
        val HIGH_RISK_APPROVALS: Set<RequiredApproval> = setOf(
            RequiredApproval.IRREVERSIBLE_DELETE,
            RequiredApproval.GIT_PUSH,
            RequiredApproval.EXTERNAL_TRANSFER,
            RequiredApproval.THIRD_PARTY_SKILL
        )
    }
}

/**
 * Eine offene Frage an den Nutzer.
 *
 * @property why warum ohne Antwort nicht geplant werden kann. Eine Frage ohne
 *           Begründung führt in der Praxis dazu, dass sie übersprungen wird.
 */
data class Clarification(
    val question: String,
    val why: String,
    val affectedSteps: List<String> = emptyList()
)

/** Ein Fehler in der Struktur des Plans. */
data class PlanProblem(
    val message: String,
    val stepIds: List<String> = emptyList()
)

/**
 * Der fertige Plan.
 *
 * Ein Plan ist erst dann ausführbar, wenn [canStart] `true` ist. Das ist erst
 * der Fall, wenn keine offene Frage besteht, keine strukturelle Unregel
 * vorliegt **und** jede sicherheitsrelevante Freigabe einzeln erteilt wurde.
 */
data class ExecutionPlan(
    val request: String,
    val steps: List<PlanStep>,
    val problems: List<PlanProblem> = emptyList(),
    val clarifications: List<Clarification> = emptyList(),
    /** Vom Nutzer ausdrücklich erteilte Freigaben, als `Schrittkennung + Stufe`. */
    val grantedApprovals: Set<String> = emptySet()
) {
    /**
     * Ob der Plan jetzt ausgeführt werden darf.
     *
     * Drei Bedingungen müssen gemeinsam erfüllt sein: kein Problem, keine offene
     * Frage, jede sicherheitsrelevante Freigabe erteilt. Das ist die Zusage
     * „Nutzer kann den Plan vor folgenreichen Aktionen prüfen“.
     */
    val canStart: Boolean
        get() = problems.isEmpty() &&
            clarifications.isEmpty() &&
            steps.none { needsApproval(it) && !isGranted(it) }

    /**
     * Ob für diesen Schritt eine Freigabe erteilt wurde.
     *
     * Der Schlüssel enthält die Freigabestufe, damit das Erteilen für
     * `FILE_CHANGE` nicht automatisch für `EXTERNAL_TRANSFER` gilt — die
     * Freigaben sind je Stufe einzeln.
     */
    fun isGranted(step: PlanStep): Boolean =
        "${step.id}#${step.requiredApproval.name}" in grantedApprovals

    /** Die Reihenfolge, in der die Schritte ausgeführt werden. */
    fun orderedSteps(): List<PlanStep> = topologicalOrder(steps)

    /** Jede sicherheitsrelevante Freigabe, die noch fehlt. */
    fun missingApprovals(): List<Pair<PlanStep, RequiredApproval>> =
        steps.filter { needsApproval(it) && !isGranted(it) }
            .map { it to it.requiredApproval }

    /**
     * Wellen, in denen Schritte gleichzeitig laufen dürfen.
     *
     * Zwei Bedingungen müssen erfüllt sein: keine gemeinsame Datei und keine
     * Abhängigkeit ineinander. Das ist dieselbe Regel, nach der dieses Projekt
     * seine eigene Bauarbeit aufteilt — ein Agent pro Dateibereich.
     */
    fun parallelGroups(): List<List<PlanStep>> {
        val remaining = steps.toMutableList()
        val done = mutableSetOf<String>()
        val groups = ArrayList<List<PlanStep>>()

        while (remaining.isNotEmpty()) {
            // Greedy statt Filter: ein Schritt ist bereit, wenn seine
            // Abhängigkeiten erledigt sind UND kein bereits für diese Welle
            // gewählter Schritt dieselbe Datei anfasst. Ein reiner Filter
            // („kein anderer Schritt teilt die Datei“) liefert bei zwei Schritten
            // auf derselben Datei gar nichts — beide fallen durch, und die
            // Schleife endet ohne Welle. Genau die Schritte, die nacheinander
            // laufen müssen, wären dann unsichtbar gewesen.
            val wave = ArrayList<PlanStep>()
            val stillWaiting = ArrayList<PlanStep>()

            for (step in remaining.sortedBy { it.id }) {
                val depsReady = step.dependsOn.all { it in done }
                val fileFree = wave.none { sharesFileWith(step, it) }
                if (depsReady && fileFree) wave += step else stillWaiting += step
            }

            if (wave.isEmpty()) break // Zyklus oder Konflikt; in [problems] gemeldet.
            groups += wave
            done += wave.map { it.id }
            remaining.clear()
            remaining.addAll(stillWaiting)
        }
        return groups
    }

    /**
     * Klartext für die Planprüfung durch den Nutzer.
     *
     * Reihenfolge, Dateien, Risiko und Freigabestufe stehen bei jedem Schritt
     * ausdrücklich dabei — ein Schritt, der eine Freigabe braucht, kann in
     * dieser Liste nicht übersehen werden.
     */
    fun reviewLines(): List<String> = buildList {
        add("Plan für: $request")
        if (clarifications.isNotEmpty()) {
            add("Der Plan ist noch nicht ausführbar — es gibt ${clarifications.size} offene Frage(n):")
            clarifications.forEach { add("  ? ${it.question} — ${it.why}") }
            return@buildList
        }
        problems.forEach { add("Planfehler: ${it.message}") }

        orderedSteps().forEachIndexed { index, step ->
            add("")
            add("${index + 1}. ${step.title}")
            add("   ${step.description}")
            if (step.files.isNotEmpty()) add("   Dateien: ${step.files.joinToString(", ")}")
            add("   Risiko: ${step.risk.germanLabel}")
            add(
                "   Freigabe nötig: ${step.requiredApproval.germanLabel}" +
                    if (step.requiredApproval.isSecurityRelevant) " (sicherheitsrelevant)" else ""
            )
            step.notes.forEach { add("   Hinweis: $it") }
            if (step.dependsOn.isNotEmpty()) add("   Nach: ${step.dependsOn.joinToString(", ")}")
        }
        add("")
        add("Erteilte Freigaben: ${grantedApprovals.size}")
        if (canStart) {
            add("Der Plan kann ausgeführt werden.")
        } else {
            add("Der Plan wartet auf Ihre Prüfung.")
        }
    }

    /**
     * Erteilt genau eine Freigabe und liefert den neuen Plan zurück.
     *
     * Bewusst einzeln: [grantAll] existiert für den bewussten Ausnahmemodus der
     * App (Aufgabe 109), aber der normale Weg ist eine Freigabe pro Schritt.
     */
    fun grant(stepId: String, approval: RequiredApproval): ExecutionPlan = copy(
        grantedApprovals = grantedApprovals + "${stepId}#${approval.name}"
    )

    private fun sharesFileWith(a: PlanStep, b: PlanStep): Boolean =
        a.files.any { file -> b.files.contains(file) }

    private fun needsApproval(step: PlanStep): Boolean =
        step.requiredApproval.isSecurityRelevant

    companion object {
        /**
         * Topologische Sortierung.
         *
         * Schritte ohne offene Abhängigkeit zuerst, danach abhängige. Innerhalb
         * einer Stufe alphabetisch, damit derselbe Plan immer dieselbe Reihenfolge
         * ergibt und der Nutzer zwei Pläne vergleichen kann.
         */
        fun topologicalOrder(steps: List<PlanStep>): List<PlanStep> {
            val byId = steps.associateBy { it.id }
            val visited = mutableSetOf<String>()
            val result = ArrayList<PlanStep>()

            fun visit(step: PlanStep) {
                if (step.id in visited) return
                visited += step.id
                step.dependsOn.mapNotNull { byId[it] }
                    .sortedBy { it.id }
                    .forEach { visit(it) }
                result += step
            }

            steps.sortedBy { it.id }.forEach { visit(it) }
            return result
        }
    }
}

/**
 * Erzeugt Pläne aus einem Auftrag.
 *
 * Alle Methoden sind rein. Die Klasse führt nichts aus und kennt keinen
 * Anbieter — sie übersetzt einen Auftrag in etwas Prüfbares.
 */
object AgentTaskPlanner {

    /**
     * Stichwörter, die eine Aktion im Auftragstext erkennen lassen.
     *
     * Bewusst eine kleine, sichtbare Liste statt einer Heuristik: sie dient nur
     * dazu, **eine Nachfrage zu stellen**, wenn im Auftrag etwas genannt wird,
     * wofür der Plan keinen Schritt hat. Ein Wortfehler kostet eine Frage; ein
     * stillschweigend übergangener Lösch- oder Installationsschritt wäre genau
     * die stille Freigabeumgehung, die diese Aufgabe verhindern will.
     */
    private val actionKeywords: List<Pair<StepAction, List<String>>> = listOf(
        StepAction.DELETE_FILE to listOf("lösch", "loesch", "delete", "entfern", "remove"),
        StepAction.INSTALL_DEPENDENCY to listOf("installier", "install", "paket", "dependency", "abhängigkeit", "npm", "gradle add"),
        StepAction.GIT_PUSH to listOf("push", "hochladen", "upload", "commit und push"),
        StepAction.NETWORK_CALL to listOf("sende", "schick", "upload datei", "api aufruf", "anfrage an"),
        StepAction.USE_API_KEY to listOf("api-schlüssel", "api key", "schlüssel verwenden", "token verwenden"),
        StepAction.WRITE_FILE to listOf("schreib", "ändere", "aendere", "implementier", "erstelle datei", "fix", "reparier"),
        StepAction.RUN_COMMAND to listOf("führe aus", "fuehre aus", "starte test", "baue", "build", "testlauf", "gradlew", "npm run"),
        StepAction.RUN_THIRD_PARTY_SKILL to listOf("skill", "fähigkeit ausführen", "plugin ausführen")
    )

    /**
     * Baut den Plan.
     *
     * @param request der Auftrag in der Sprache des Nutzers.
     * @param steps die Schritte, die der Aufrufer (oder das Modell) vorschlägt.
     * @param assumptions Annahmen, die getroffen wurden. Jede sichtbare
     *        Annahme ist besser als eine unsichtbare, wird aber in
     *        [Clarification] aufgegriffen, wenn sie eine Aktion betrifft, die
     *        nicht abgedeckt ist.
     */
    fun plan(
        request: String,
        steps: List<PlanStep>,
        assumptions: List<String> = emptyList()
    ): ExecutionPlan {
        val problems = ArrayList<PlanProblem>()
        problems += structuralProblems(steps)
        val clarifications = clarificationsFor(request, steps, assumptions)

        return ExecutionPlan(
            request = request,
            steps = steps,
            problems = problems,
            clarifications = clarifications
        )
    }

    /**
     * Findet Aktionen, die im Auftrag genannt, aber im Plan nicht abgedeckt sind.
     *
     * Das ist die Gegenrichtung zu [PlanStep.requiredApproval]: dort wird
     * verhindert, dass ein *vorhandener* Schritt seine Freigabe unterschlägt;
     * hier wird verhindert, dass eine *fehlende* Aktion unbemerkt untergeht.
     */
    fun detectMissingActions(request: String, steps: List<PlanStep>): List<Clarification> {
        val covered = steps.flatMap { it.actions }.toSet()
        val lower = request.lowercase()

        return actionKeywords
            .filter { (action, keywords) ->
                action !in covered && keywords.any { lower.contains(it) }
            }
            .map { (action, keywords) ->
                val hit = keywords.first { lower.contains(it) }
                Clarification(
                    question = "Soll im Auftrag wirklich „$hit“ passieren " +
                        "(${action.germanLabel})?",
                    why = "Das Wort steht in Ihrem Auftrag, aber der Plan enthält " +
                        "keinen Schritt dafür. Statt es zu übergehen, wird nachgefragt — " +
                        "${action.requiredApproval.germanLabel} wäre nötig."
                )
            }
    }

    /**
     * Offene Fragen aus Annahmen und Lücken im Plan.
     */
    private fun clarificationsFor(
        request: String,
        steps: List<PlanStep>,
        assumptions: List<String>
    ): List<Clarification> {
        val questions = ArrayList<Clarification>()
        questions += detectMissingActions(request, steps)

        // Eine Annahme über eine sicherheitsrelevante Aktion ist eine offene
        // Frage, keine Tatsache. Das ist der Punkt „Unklare Anforderungen als
        // Frage statt als Annahme“.
        val riskyWords = actionKeywords
            .filter { (action, _) -> action.requiredApproval.isSecurityRelevant }
            .flatMap { (_, keywords) -> keywords }
        assumptions.forEach { assumption ->
            val lower = assumption.lowercase()
            if (riskyWords.any { lower.contains(it) }) {
                questions += Clarification(
                    question = "Trifft die Annahme zu? „$assumption“",
                    why = "Die Annahme betrifft eine Aktion, die Ihre Freigabe braucht. " +
                        "Eine Annahme ersetzt keine Freigabe."
                )
            }
        }

        // Eine leere Liste ist keine Planung.
        if (steps.isEmpty()) {
            questions += Clarification(
                question = "Was soll zuerst gemacht werden?",
                why = "Der Auftrag enthält noch keine Schritte. Es wird nichts " +
                    "angenommen und nichts ausgeführt."
            )
        }

        // Ein Schritt ohne Ziel und ohne Beschreibung ist für den Nutzer nicht prüfbar.
        steps.filter { it.description.isBlank() }.forEach {
            questions += Clarification(
                question = "Was soll in Schritt „${it.id}“ genau geschehen?",
                why = "Der Schritt hat keine Beschreibung, Sie können ihn also nicht prüfen.",
                affectedSteps = listOf(it.id)
            )
        }

        return questions
    }

    /**
     * Prüft die Struktur: doppelte Kennungen, unbekannte Abhängigkeiten, Zyklen.
     *
     * Ein Plan mit einem Haken in der Struktur ist kein ausführbarer Plan,
     * sondern eine Falle für den Nutzer — deshalb sind das [PlanProblem] und
     * damit ein Grund, [ExecutionPlan.canStart] `false` zu lassen.
     */
    fun structuralProblems(steps: List<PlanStep>): List<PlanProblem> {
        val problems = ArrayList<PlanProblem>()
        val ids = steps.map { it.id }

        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) {
            problems += PlanProblem(
                "Doppelte Schrittkennung: ${duplicates.joinToString(", ")}",
                duplicates.toList()
            )
        }

        val known = ids.toSet()
        val unknownDeps = steps.flatMap { step ->
            step.dependsOn.filter { it !in known }.map { "${step.id} → $it" }
        }
        if (unknownDeps.isNotEmpty()) {
            problems += PlanProblem(
                "Unbekannte Abhängigkeiten: ${unknownDeps.joinToString(", ")}",
                unknownDeps.flatMap { it.split(" → ") }
            )
        }

        val cycle = findCycle(steps)
        if (cycle != null) {
            problems += PlanProblem("Abhängigkeitskreis: ${cycle.joinToString(" → ")}", cycle)
        }

        return problems
    }

    /** Ein Zyklus als Kette von Kennungen, oder `null`. */
    private fun findCycle(steps: List<PlanStep>): List<String>? {
        val byId = steps.associateBy { it.id }
        val state = mutableMapOf<String, Int>() // 0 = unbesucht, 1 = im Stapel, 2 = fertig
        val stack = ArrayList<String>()

        fun visit(id: String): List<String>? {
            when (state[id]) {
                1 -> {
                    val start = stack.indexOf(id)
                    return stack.subList(start, stack.size) + id
                }
                2 -> return null
            }
            state[id] = 1
            stack += id
            byId[id]?.dependsOn.orEmpty().forEach { dep ->
                if (dep in byId) visit(dep)?.let { return it }
            }
            stack.removeAt(stack.size - 1)
            state[id] = 2
            return null
        }

        steps.forEach { step ->
            visit(step.id)?.let { return it }
        }
        return null
    }
}