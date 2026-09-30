# Claudroide-Bauzustand

**Stand:** 2026-09-30
**Status:** Spezifikation, 135 maschinenlesbare Task-Pläne, Bau-Automatisierung; kein Android-App-Code begonnen.

## Erledigt
- `claudroide-spec.md` enthält Produktziele, Leitplanken, Prüfkriterien und 135 Aufgaben.
- Genau 135 nummerierte Aufgabendateien unter `tasks/`, plus `skill-matrix.md` und `DEPENDENCIES.md`.
- **Neu:** Jede Aufgabendatei trägt YAML-Frontmatter (`id`, `title`, `wave`, `depends_on`, `files`, `skills`, `status`, `gate`, `done_since_last_edit`, `content-hash`). Quelle der Wahrheit ist `tools/sync_frontmatter.py`; `--check` prüft, `--status ID=...` setzt Status. Ein `done` gilt nur bei unverändertem Inhalt als verifiziert.
- **Neu:** `tasks/DEPENDENCIES.md` Lücken geschlossen: 091 in W19, 125 in W24, 131 in W27; neue Sperrkanten 088+089→091, 124→131, 070→125, W24→W25 als Extra-Abhängigkeit von 106.
- **Neu:** `CLAUDE.md` mit autonomer Bau-Schleife (5 Schritte, klare Stopp-Punkte), Medienregeln (Media Bridge des Nutzers, PNG/WebP, kein SVG, kein Platzhalter) und Repo-Pflege-Regeln.
- **Neu:** `README.md` interaktiv: Badges, Mermaid-Bauablauf, Schnellstart-Tabelle, Klapp-Elemente, Wellenübersicht, Bild-Pipeline.
- **Neu:** `.github/workflows/repo-health.yml` prüft bei jedem Push: Frontmatter-Konsistenz (135 Tasks), kein SVG in `assets/`, Checkpoint vorhanden.
- `tasks/skill-matrix.md` weist jedem Task zwei Skills zu; sechs globale Skills installiert (`swarm-planner`, `parallel-task`, `adaptive`, `android-profiler`, `android-permissions-security`, `testing-setup`). Integrierte `/code-review`, `/claude-api`, `/verify` vor Nutzung auf Verfügbarkeit prüfen.
- `.claude/skills/claudroide-resume/SKILL.md`: Wiederaufnahme nach Abbruch über `/claudroide-resume`.
- **Medien:** Der Nutzer besitzt eine Claude Media Bridge (Nano Banana Pro/2). Bildauftrag steht in `assets/logo-brief.md`. Noch kein Logo gerendert; `assets/` enthält keine SVG-Datei.

## Global installierte Skills
- `swarm-planner`, `parallel-task` — `am-will/swarms`.
- `adaptive`, `android-profiler`, `android-permissions-security`, `testing-setup` — `android/skills`.

## Aktuelle Arbeit
- `python3 tools/sync_frontmatter.py --check` läuft grün über alle 135 Task-Dateien.
- Commits `fc684fd`, `20e67b0`, `ac43675` sind auf `main` des verifizierten privaten Repositorys `mertgoevse-wq/claudroide`.

## Nächster Schritt
1. Claude Code im Projektordner starten und entweder `Baue weiter bis zum finalen Produkt` sagen oder `/claudroide-resume` nutzen.
2. Start der Bau-Schleife mit W0: Tasks 001, 002, 003, 006 (alle ohne Vorgänger).
3. Logo über die Media Bridge des Nutzers rendern (Auftrag in `assets/logo-brief.md`), danach Aufgabe 020–022 damit füttern.
4. Vor app-nahen Tasks: offene Geräte-, Lizenz-, Anbieter- und Architekturfragen aus der Spezifikation klären.

## Offen
- PNG-/WebP-Logo über die Media Bridge des Nutzers rendern und prüfen.
- A56-Gerätewerte, Android-Version, Lizenz, finale Anbieterwege vor Implementierung bestätigen.
