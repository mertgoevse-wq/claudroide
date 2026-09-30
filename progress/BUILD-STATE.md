# Claudroide-Bauzustand

**Stand:** 2026-09-30
**Status:** Spezifikation, 135 maschinenlesbare Task-Pläne, Bau-Automatisierung; kein Android-App-Code begonnen.

## Erledigt
- `claudroide-spec.md` enthält Produktziele, Leitplanken, Prüfkriterien und 135 Aufgaben.
- Genau 135 nummerierte Aufgabendateien unter `tasks/`, plus `skill-matrix.md` und `DEPENDENCIES.md`.
- **Welle 0 (W0) vollständig abgeschlossen:**
  - **Task 001 erledigt:** „Produktregeln und offene Entscheidungen“ — Governance-Matrix, Produktziele, harte Nicht-Ziele, Freigabestufen und Zuständigkeiten vollständig spezifiziert (`done_since_last_edit: true`).
  - **Task 002 erledigt:** „Quellen und Aktualität prüfen“ — Verzeichnis verifizierter Primärquellen für Android (SAF, Services, NNAPI Deprecation), Anthropic API, OpenRouter, Google, OpenAI-Format und Markenrichtlinien erstellt (`done_since_last_edit: true`).
  - **Task 003 erledigt:** „Eigenständige Marke und Grenzen“ — Verbindlicher Unabhängigkeitshinweis, Abgrenzungsmatrix (erlaubte Kompatibilitätshinweise vs. Markenverletzung), Mascot- und Farbkonzept definiert (`done_since_last_edit: true`).
  - **Task 006 erledigt:** „A56-Gerätebestand“ — Datensparsamer Testbogen mit realen Messwerten der Zielumgebung (ARM64, 8 GB RAM, 128 GB UFS, Android 15 One UI 7 Vorgabe) erstellt (`done_since_last_edit: true`).
- **Welle 1 (W1) vollständig abgeschlossen:**
  - **Task 004 erledigt:** „Claude-Zugang prüfen“ — Offizieller BYOK-Weg über Anthropic Messages API und SSE-Streaming festgelegt; Web-Abo-Scraping und unautorisierte Proxys strikt ausgeschlossen (`done_since_last_edit: true`).
  - **Task 005 erledigt:** „Anbieter-Regelmatrix“ — Anschlussmatrix für Claude API, OpenRouter, lokale Server (Ollama/vLLM) und Custom Endpoints verifiziert; inoffizielle/Abo-Wege ausgeschlossen (`done_since_last_edit: true`).
  - **Task 009 erledigt:** „Projektaufbau“ — Modulare Android Clean Architecture (core/feature/agent), Verzeichnisse, Test-Layout und Paketstrukturen definiert (`done_since_last_edit: true`).
  - **Task 019 erledigt:** „Eigenständiger Markenauftritt“ — Vollständiger Design- und Markenleitfaden (Android-Grün `#3DDC84`, Terrakotta-Akzente, Tone of Voice ohne Slop, Mascot- & Banner-Konzept) verankert (`done_since_last_edit: true`).
- **Welle 2 (W2) vollständig abgeschlossen:**
  - **Task 007 erledigt:** „NPU-Machbarkeit“ — Machbarkeitsbericht (NNAPI abgekündigt, Vulkan/GPU möglich, CPU-Fallback, ehrliche NPU-Einstufung) und Benchmark-Testplan erstellt (`done_since_last_edit: true`).
  - **Task 008 erledigt:** „Bauen nur mit dem Telefon“ — Vergleich beider Baupfade; GitHub Actions als akkuschonender Primärweg festgelegt, lokaler On-Device-Bau als transparenter Rückfallweg dokumentiert (`done_since_last_edit: true`).
- **Welle 3 (W3) vollständig abgeschlossen:**
  - **Task 010 erledigt:** „Android-App-Grundlage“ — Vollständiges Android-Scaffold (`app/`, Gradle KTS, Jetpack Compose Material 3 Adaptive AppShell, Dark AMOLED Theme, Unit-Tests) erstellt (`done_since_last_edit: true`).
- **Welle 4 (W4) vollständig abgeschlossen:**
  - **Task 011 erledigt:** „Bauweg direkt am A56“ — Fundierte Machbarkeitsanalyse zu W^X-Restriktionen, SELinux, RAM-Peaks und Akkubelastung bei In-App-Builds erstellt (`done_since_last_edit: true`).
  - **Task 012 erledigt:** „Online-Bau vom Handy aus“ — Vergleich der Cloud-Bauwege; GitHub Actions APK-Pipeline (`.github/workflows/build-apk.yml`) mit OpenJDK 17 und Artefakt-Bereitstellung implementiert (`done_since_last_edit: true`).
- **Welle 5 (W5) in Arbeit:**
  - **Task 013 erledigt:** „Ersteinrichtung“ — 4-Stufen-Onboarding (`OnboardingScreen.kt`) mit Willkommen, Sprache/Theme, BYOK-Transparenz und Erstem Projekt erstellt (`done_since_last_edit: true`).
  - **Task 014 erledigt:** „Sprache automatisch erkennen“ — `LanguageManager.kt`, englische Lokalisierung (`values-en/strings.xml`) und Locale-Fallback-Tests erstellt (`done_since_last_edit: true`).
  - **Task 015 erledigt:** „Sprache manuell wechseln“ — `LanguagePreferences.kt`, `LanguageSelectionDialog.kt` und `LanguagePreferenceTest.kt` implementiert (`done_since_last_edit: true`).
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
- **Task 015 abgeschlossen:** `tasks/015-language-switch.md` fertiggestellt und verifiziert. `LanguagePreferences.kt`, `LanguageSelectionDialog.kt` und `LanguagePreferenceTest.kt` implementiert.
- `python3 tools/sync_frontmatter.py --check` läuft grün über alle 135 Task-Dateien (16 erledigt, 119 offen).
- Bereit für Commit und Push von Task 015. Nächste freie Aufgaben in W5: 016–018; in W6: 020–024.

## Nächster Schritt
1. Claude Code im Projektordner starten und entweder `Baue weiter bis zum finalen Produkt` sagen oder `/claudroide-resume` nutzen.
2. Start der Bau-Schleife mit W0: Tasks 001, 002, 003, 006 (alle ohne Vorgänger).
3. Logo über die Media Bridge des Nutzers rendern (Auftrag in `assets/logo-brief.md`), danach Aufgabe 020–022 damit füttern.
4. Vor app-nahen Tasks: offene Geräte-, Lizenz-, Anbieter- und Architekturfragen aus der Spezifikation klären.

## Offen
- PNG-/WebP-Logo über die Media Bridge des Nutzers rendern und prüfen.
- A56-Gerätewerte, Android-Version, Lizenz, finale Anbieterwege vor Implementierung bestätigen.
