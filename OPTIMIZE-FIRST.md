# OPTIMIZE-FIRST — Erste Aufgabe für Claude Code (Dateien vorbereiten, NICHT bauen)

**Du bekommst diesen Auftrag von Mert. Er lautet: überarbeite zuerst die Planungsdateien dieses Projekts, damit du selbst optimal mit ihnen arbeiten kannst. Beginne danach NICHT mit dem Bau (kein Task 0.1, kein Produktcode).** Melde dich am Ende mit einem kurzen Bericht und warte auf Merts Go.

## 1. Lies zuerst (in dieser Reihenfolge)

1. Diese Datei (`OPTIMIZE-FIRST.md`)
2. `CLAUDROIDE_CURRENT_STATE.md` — wo steht das Projekt?
3. `claudroide-spec.md` — die **einzige gültige Gesamtspezifikation** (besonders §0 Leseanleitung, §19 Startfreigaben, §29 Start/Wiederaufnahme)
4. `CLAUDE.md` — Betriebsanleitung für autonome Läufe
5. `docs/TASKS.md`, `docs/SOURCE_INDEX.md`, `docs/DECISIONS.md`, `progress/BUILD-STATE.md`
6. `archive/README.md` — nur zur Kenntnis: dort liegen veraltete Vorversionen

## 2. Deine einzige Aufgabe in diesem Lauf: Dateien optimieren

Überarbeite die Planungsdateien aus Schritt 1 so, dass **du** aus ihnen ohne Rückfragen autonom arbeiten kannst. Prüfe und verbessere dabei:

1. **Widersprüche zwischen Dateien:** Wo zwei Dateien sich widersprechen, entscheidest du nach der Rangfolge aus Spec §0 (§29 vor §28 vor §27 …) und dokumentierst die Klärung in `docs/DECISIONS.md`. Was du nicht eigenmächtig festlegen darfst (Liste in Spec §29.9), lässt du offen und markierst es klar.
2. **Instruktions-Blutung (Cross-Module-Leakage):** Anweisungen in einer Datei dürfen nicht stillschweigend das Verhalten aus einer anderen Datei verschieben. Prüfe, ob CLAUDE.md, Spec §29 und docs/TASKS.md sich gegenseitig ergänzen statt stören; löse Dopplungen auf, indem genau eine Datei die maßgebliche Fassung hält und die anderen auf sie verweisen.
3. **Ehrlichkeit (Anti-Hallucination, Muster natprd):** Keine Zahl, kein Status, kein „fertig" ohne Beleg im Checkpoint. Unbelegtes bleibt offen markiert (z. B. Testzahl 2535 vs. 2530 → Task 0.3).
4. **Lesbarkeit für dich selbst:** Der Einstieg in einen neuen Lauf muss aus `CLAUDROIDE_CURRENT_STATE.md` + `CLAUDE.md` allein funktionieren. Wenn dir beim Lesen etwas fehlt (z. B. eine Klartext-Definition, ein fehlender Pfad, eine unklare Reihenfolge), ergänze es genau dort.
5. **Anti-Slop (Spec §18):** keine Werbesprache, keine Emojis in Überschriften, keine erfundenen Zahlen, ruhige direkte Sprache. Kriterien aus `~/clawscreen/tools/anti-slop.py` als Maßstab nehmen.
6. **Konsistenz der Verweise:** Alle in den Dateien genannten Pfade müssen existieren oder klar als „wird in Task X angelegt" markiert sein (aktuell z. B. `docs/SKILL-INDEX.md`, `docs/REQUIREMENTS.md` — noch nicht vorhanden, das ist korrekt so).
7. **Skills vorbereiten:** Lege bereits jetzt `docs/SKILL-INDEX.md` als **Vorlage** an (Phase-0-Struktur aus Spec §28 Punkt 6), mit den bereits bekan Einträgen (global installierte Skills sichten: `~/.claude/skills/`, `~/.agents/skills/`) — aber erfinde nichts; nicht gefundene Skills bleiben ungelistet.

**Regeln für die Bearbeitung selbst:**

- Ändere nur innerhalb `/home/mert/claudroide-next`; Schwesterprojekte und `_sources/` bleiben READ-ONLY.
- Pro Datei: erst lesen, dann gezielt ändern; große Dateien (`claudroide-spec.md`, 145 KB) nur an betroffenen Stellen, nicht umschreiben.
- Nach jedem Block: `progress/BUILD-STATE.md` + `CLAUDROIDE_CURRENT_STATE.md` aktualisieren (was gelesen, geändert, entschieden).
- Kein Push in diesem Lauf (es gibt noch kein Remote-Repo; Repo-Anlage ist Task des Baulaufs).
- max. 1 Subagent, 1 Shell-Befehl, 1 Build gleichzeitig (hier: gar kein Build).

## 3. Angewandte Quellen (nach Spec §28.4 automatisch, hier schon erledigt/vorgegeben)

| Quelle | Wofür hier |
|---|---|
| `awesome-prompts`: `instruction_bleed_auditor` | Prüfkriterium 2 (Leakage zwischen Dateien) |
| `awesome-prompts`: `agent_context_efficiency_engineer` | Prüfkriterium 4 (Einstieg schlank halten, Checkpoints statt Kontext-Wust) |
| `awesome-prompts`: `design_system_spec_architect` | **Hinweis für später:** beim App-Bau (Phase UI) eine `DESIGN.md` mit Tokens + Begründung anlegen — nicht Bestandteil dieses Laufs |
| Skill `natprd` | Ehrlichkeits-/Akzeptanzkriterien-Muster (Prüfkriterium 3) |
| Skill `graphify` | optional: Wissensgraph über die Doku ziehen, um Verweis-Lücken zu finden; nur wenn er wirklich hilft |

## 4. Danach — wenn Mert Go sagt

Bau-Start nach `CLAUDE.md` §3 und Spec §29.7 (Arbeitsauftrag), Wiederaufnahme nach Absturz mit `WEITERARBEITEN` (Spec §29.8). Nicht aus eigenem Antrieb in den Bau übergehen: dieser Lauf endet mit dem Bericht.
