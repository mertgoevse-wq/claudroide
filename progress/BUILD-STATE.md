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
- **Welle 5 (W5) vollständig abgeschlossen:**
  - **Task 013 erledigt:** „Ersteinrichtung“ — 4-Stufen-Onboarding (`OnboardingScreen.kt`) mit Willkommen, Sprache/Theme, BYOK-Transparenz und Erstem Projekt erstellt (`done_since_last_edit: true`).
  - **Task 014 erledigt:** „Sprache automatisch erkennen“ — `LanguageManager.kt`, englische Lokalisierung (`values-en/strings.xml`) und Locale-Fallback-Tests erstellt (`done_since_last_edit: true`).
  - **Task 015 erledigt:** „Sprache manuell wechseln“ — `LanguagePreferences.kt`, `LanguageSelectionDialog.kt` und `LanguagePreferenceTest.kt` implementiert (`done_since_last_edit: true`).
  - **Task 016 erledigt:** „App-Einstellungen“ — `AppSettings.kt` mit Freigabestufen, Maskierung sensibler Tokens (`maskApiKey`) und Warnungsschaltern implementiert (`done_since_last_edit: true`).
  - **Task 017 erledigt (Gate):** „Android-Erlaubnisse“ — Least Privilege verankert, kein `MANAGE_EXTERNAL_STORAGE`, `PermissionManager.kt` mit SAF-Persistierung und Denial-Handling implementiert (`done_since_last_edit: true`).
  - **Task 018 erledigt (Gate):** „Hintergrundaufgaben“ — Lebenszyklusmodell (`TaskLifecycleState`) und `BackgroundTaskManager.kt` mit Start, Pause, Abbruch und Bereinigung implementiert (`done_since_last_edit: true`).
- **Welle 6 (W6) vollständig abgeschlossen:**
  - **Task 020 erledigt:** „Logo und App-Symbol“ — Maskottchen-Logo (`assets/claudroide-mascot-logo.jpg`) und Adaptive-Icon-Spezifikation (108 dp Canvas, 66 dp Safe Zone, A56 FHD+ Dichteskalierung) verifiziert (`done_since_last_edit: true`).
  - **Task 021 erledigt:** „Eigene App-Bilder“ — Leerstufen- und Zustandskatalog (EmptyChat, EmptyProject, Offline, Approval) mit Vektor-Vorrang und 50 KB Deckel spezifiziert (`done_since_last_edit: true`).
  - **Task 022 erledigt:** „Kopf- und Bannerbilder“ — 16:9-Banner (`assets/claudroide-banner.jpg`), Safe-Content-Zonen, Kompression und Alternativtexte implementiert (`done_since_last_edit: true`).
  - **Task 023 erledigt:** „Farben und Kontrast“ — Semantische Farb-Tokens (`ColorTokens.kt`), WCAG 2.2 AAA/AA Kontrastprüfung und Nicht-Allein-Farbe-Statusgarantie implementiert (`done_since_last_edit: true`).
  - **Task 024 erledigt:** „Schrift und Abstände“ — Typografie (`TypeTokens.kt`), Material 3 Mindest-Touch-Targets (48 dp), Spacings und Code-Horizontalskroll-Regeln implementiert (`done_since_last_edit: true`).
- **Welle 7 (W7) vollständig abgeschlossen:**
  - **Task 025 erledigt:** „Navigation“ — Type-Safe `Screen` Navigation (`NavRoutes.kt`), TopLevel-Hierarchie, Backstack-Management, Projekt-Badge-Bindung (`projectBadgeText`), destruktive Sicherheitsisolation (`NavigationSafetyPolicy`) und Unit-Tests (`NavigationStructureTest.kt`) implementiert (`done_since_last_edit: true`).
  - **Task 026 erledigt:** „Smartphone-Ansichten“ — Viewport-Tokens für Galaxy A56 (`MobileViewportTokens.kt`), Einspalten-Umschaltung (< 600 dp), IME-Höhenberechnung, Einhand-Ergonomie und Knopftrennungsregeln (48 dp, 16 dp Sicherheitsabstand) mit Unit-Tests (`MobileLayoutTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 027 erledigt:** „Eingabe und Tastatur“ — Mehrzeiliges Eingabefeld-Modell (`ChatInputState.kt`), nahtloser Wechsel zwischen Senden und Stoppen während Stream (`InputActionButtonState`), Anhangstransparenz und Provider-Consent-Prüfung (`AttachmentPolicy`) mit Unit-Tests (`ChatInputTest.kt`) implementiert (`done_since_last_edit: true`).
  - **Task 028 erledigt:** „Hell und dunkel“ — Theme-Modi (`ThemeMode.kt`), AMOLED-Dunkelmodus als energiesparender A56-Standard, Hell-Theme und WCAG AA (>= 4.5:1) Kontrastgarantie für Code- und Gefahrenbereiche mit Unit-Tests (`ThemeModeTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 029 erledigt:** „Zugänglichkeit“ — TalkBack-Ansagen für Freigaben und Diffs (`AccessibilityPolicy`), Nicht-Allein-Farbe-Invariante (Farbe + Icon + Textbeschreibung), Skalierung bis 200% Systemschrift und Fokus-Hierarchie mit Unit-Tests (`AccessibilityTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 030 erledigt:** „Lade- und Fehlerzustände“ — Transparente Statusanzeige (`DataTransmissionStatus`), strukturierte UI-Fehler mit separaten Wiederholen/Abbrechen-Aktionen (`AppUiError`) und automatische Geheimnismaskierung (`ErrorSanitizer`) mit Unit-Tests (`ErrorStateTest.kt`) verifiziert (`done_since_last_edit: true`).
- **Welle 8 (W8) in Arbeit:**
  - **Task 031 erledigt:** „Chatliste“ — Datenmodell (`ChatSummaryItem`), chronologische Sortierung, Projektfilterung, Leerzustandserkennung (`ChatListUiState`) und Schutz vor Geheimnis-Lecks in Vorschautexten (`ChatPreviewSanitizer`) mit Unit-Tests (`ChatListTest.kt`) verifiziert (`done_since_last_edit: true`).
- **Assets & Dokumentation:**
  - 16:9 Header-Banner (`assets/claudroide-banner.jpg`) mit Android-Bot und Terrakotta-KI-Funken via Claude Media Bridge generiert.
  - Zweisprachige GitHub-Dokumentation: Englisches Haupt-README (`README.md`) mit interaktivem Sprachwechsler zu deutschem `README.de.md`.
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
- **Task 031 abgeschlossen:** „Chatliste“ (W8) — `ChatListModels.kt`, `ChatListTest.kt` verifiziert.
- **Nächste Aufgabe: Task 032:** „Chat-Suche und Filter“ (W8, Abhängigkeiten: 016, 025 erledigt).
  - Ziel: Lokale Sofortsuche in Unterhaltungen nach Titel, Inhalt und Projektfilter ohne Datenabfluss an externe Server.
  - Arbeitsdateien: `app/src/main/java/org/claudroide/app/feature/chat/ChatSearchManager.kt`, `tasks/032-chat-search-and-filter.md`, `progress/BUILD-STATE.md`.
  - Geladene Skills: `testing-setup`, `/code-review`.
- `python3 tools/sync_frontmatter.py --check` läuft grün über alle 135 Task-Dateien (31 erledigt, 104 offen).

## Nächster Schritt
1. Task 032 („Chat-Suche und Filter“) umsetzen und verifizieren.
2. Nach Task 032 committen, stagen und an `origin main` pushen.
3. Anschließende W8-Aufgaben (033 Aktionen, 034 Export/Löschen, 035 Eingabefluss, 037 Codeanzeige, 042 Datenschutz) autonom abarbeiten.

## Offen
- PNG-/WebP-Logo über die Media Bridge des Nutzers rendern und prüfen.
- A56-Gerätewerte, Android-Version, Lizenz, finale Anbieterwege vor Implementierung bestätigen.
