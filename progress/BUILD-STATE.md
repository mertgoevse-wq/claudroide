# Claudroide-Bauzustand

**Stand:** 2026-10-01
**Status:** 58 von 135 Aufgaben verifiziert. App-Code, Unit-Tests laufen grün (267 Tests, 0 Fehler).

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
- **Welle 8 (W8) vollständig abgeschlossen:**
- **Welle 9 (W9) vollständig abgeschlossen:**
  - **Task 038 erledigt:** „Stoppen und Wiederholen“ — Abbruch- und Wiederholungssteuerung (`ExecutionControlEngine`), Unterdrückung unaufgeforderter Auto-Retries, Propagierung des Abbruchsignals an Werkzeuge und ehrliche Protokollierung irreversibler Nebenwirkungen mit Unit-Tests (`ExecutionControllerTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 039 erledigt:** „Sitzungen fortsetzen“ — Saubere Sitzungswiederherstellung (`SessionResumptionManager`, `ResumedSessionState`), Bindung von Projekt, Modell und Provider, manuelle Überprüfungspflicht für unterbrochene Werkzeuge und Sendesperre vor Kontextprüfung mit Unit-Tests (`SessionResumptionTest.kt`) verifiziert (`done_since_last_edit: true`).
- **Welle 10 (W10) vollständig abgeschlossen:**
- **Welle 11 (W11) in Arbeit:**
  - **Task 046 erledigt (Gate):** „Geheimnisse verbergen“ — Zentrale Secret-Redaction (`SecretMasker`, `RedactionAuditReport`), Filterung von Anthropic Keys, OpenAI Keys, Bearer Tokens, Passwörtern und PEM-Blöcken, Defense-in-Depth Invariante mit Unit-Tests (`SecretMaskerTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 047 erledigt:** „Verbindung testen“ — Minimale Testanfrage (`ProviderPingTester`, 1 Token Ping), Abwesenheit von Projektdaten, transparente Kostenangabe und Blockade unsicherer Weiterleitungen mit Unit-Tests (`ProviderPingTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 052 erledigt (Gate):** „Eigener Endpunkt“ — Sicherheitsprüfung für benutzerdefinierte Server (`CustomEndpointGuard`), TLS-Zwang für Remote-Server, SSRF-Blockade privater LAN-IP-Bereiche und striktes Host-Pinning für API-Schlüssel mit Unit-Tests (`CustomEndpointTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 055 erledigt:** „Modellnamen verwalten“ — Modellkatalog und manuelle Eingabe (`ModelRegistry`, `ModelDescriptor`), Unterscheidung verifizierter Modelle von manuellen Nutzereingaben (`ModelOrigin`) und anfragefreie Modellauswahl mit Unit-Tests (`ModelNameTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 043 erledigt:** „Anbieter-Katalog“ — Typisierter Katalog für erlaubte Anbieter (`ProviderCatalogRegistry`, `ProviderCatalogEntry`), Dokumentationsbelege, Authentifizierungsarten (x-api-key, Bearer, No-Auth Local), Protokollformate und Fähigkeiten ohne hartcodierte Schlüssel mit Unit-Tests (`ProviderCatalogTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 044 erledigt:** „Anbieter hinzufügen“ — Validierung eigener Anbieteranschlüsse (`ProviderConfigValidator`), HTTPS-Zwang für Remote-Server, Localhost-Freigabe, Modellvalidierung und Vorab-Prüfungsansicht mit maskiertem Schlüssel (`ProviderReviewSummary`) mit Unit-Tests (`ProviderConfigTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 045 erledigt (Gate):** „Schlüssel sicher speichern“ — Hardware-gestützte Keystore-Architektur (`AndroidKeystoreSecurityPolicy`, AES-256-GCM), zwingender Ausschluss aus Cloud-/Auto-Backups, Verbot von Klartextspeicherung und sichere Schlüsseltresor-Schnittstelle (`KeyVaultStorage`, `InMemorySecureKeyVault`) mit Unit-Tests (`SecureKeyStorageTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 060 erledigt:** „Bedingungen und Prüfdatum“ — Re-Audit-Mechanismus (`ProviderTermsEngine`), 90-Tage-Ablauffrist (`VerificationStatus`), Kennzeichnung offizieller ToS und strikte Deprecation von Scraping-/Abo-Umgehungsmustern mit Unit-Tests (`ProviderTermsTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 031 erledigt:** „Chatliste“ — Datenmodell (`ChatSummaryItem`), chronologische Sortierung, Projektfilterung, Leerzustandserkennung (`ChatListUiState`) und Schutz vor Geheimnis-Lecks in Vorschautexten (`ChatPreviewSanitizer`) mit Unit-Tests (`ChatListTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 032 erledigt:** „Chat-Suche und Filter“ — Lokale Volltextsuche nach Titel, Nachricht und Projektname (`ChatSearchEngine`), Snippet-Extraktion (`SearchResultMatch`), Datums- und Projektfilterung und garantierter Ausschluss gelöschter Chats mit Unit-Tests (`ChatSearchTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 033 erledigt:** „Chats erstellen und umbenennen“ — Lokaler Chat-Lebenszyklus (`ConversationManager`, `ManagedConversation`), automatische Titelerzeugung mit Geheimnisbereinigung, Umbenennung und reversible Archivierung mit Unit-Tests (`ConversationManagerTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 034 erledigt:** „Chats löschen und exportieren“ — Export in Markdown und JSON (`ChatExportManager`), automatische Geheimnisbereinigung in Exportdateien, transparente Umfangsübersicht (`ExportScopeSummary`) und bestätigungspflichtiges permanentes Löschen mit Unit-Tests (`ChatExportTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 035 erledigt:** „Nachrichtenfeld“ — Transparente Modell- und Kostenvorschau (`ProviderSendDisclosures`), Entwurfssicherung (`DraftState`) und Schutz vor Doppel-Submissions (`MessageComposerEngine`) mit Unit-Tests (`MessageComposerTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 037 erledigt:** „Code und Antworten anzeigen“ — Parser für Fenced Code-Blocks (`MarkdownMessageParser`), Schwellenwert für Einklappen (`CodeBlockPolicy`), striktes Verbot automatischer Codeausführung und verlustfreies Kopieren mit Unit-Tests (`CodeBlockRendererTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 042 erledigt:** „Gesprächsdatenschutz“ — Garantie null Telemetrie (`ZERO_TELEMETRY_INVARIANT`), Bestätigungszwang für externe Übertragungen (`PrivacyEnforcer.canDispatchExternalPrompt`), Blockade sensibler Dateimuster (.env, id_rsa, .pem) und Offline-Lesbarkeit mit Unit-Tests (`ChatPrivacyTest.kt`) verifiziert (`done_since_last_edit: true`).
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
- **Welle 8 (W8) vollständig abgeschlossen:** Tasks 031, 032, 033, 034, 035, 037, 042 verifiziert.
- **Welle 9 (W9) vollständig abgeschlossen:**
  - **Task 038 erledigt:** „Stoppen und Wiederholen“ — Abbruch- und Wiederholungssteuerung (`ExecutionControlEngine`), Unterdrückung unaufgeforderter Auto-Retries, Propagierung des Abbruchsignals an Werkzeuge und ehrliche Protokollierung irreversibler Nebenwirkungen mit Unit-Tests (`ExecutionControllerTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 039 erledigt:** „Sitzungen fortsetzen“ — Saubere Sitzungswiederherstellung (`SessionResumptionManager`, `ResumedSessionState`), Bindung von Projekt, Modell und Provider, manuelle Überprüfungspflicht für unterbrochene Werkzeuge und Sendesperre vor Kontextprüfung mit Unit-Tests (`SessionResumptionTest.kt`) verifiziert (`done_since_last_edit: true`).
- **Welle 10 (W10) vollständig abgeschlossen:**
- **Welle 11 (W11) in Arbeit:**
  - **Task 046 erledigt (Gate):** „Geheimnisse verbergen“ — Zentrale Secret-Redaction (`SecretMasker`, `RedactionAuditReport`), Filterung von Anthropic Keys, OpenAI Keys, Bearer Tokens, Passwörtern und PEM-Blöcken, Defense-in-Depth Invariante mit Unit-Tests (`SecretMaskerTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 047 erledigt:** „Verbindung testen“ — Minimale Testanfrage (`ProviderPingTester`, 1 Token Ping), Abwesenheit von Projektdaten, transparente Kostenangabe und Blockade unsicherer Weiterleitungen mit Unit-Tests (`ProviderPingTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 052 erledigt (Gate):** „Eigener Endpunkt“ — Sicherheitsprüfung für benutzerdefinierte Server (`CustomEndpointGuard`), TLS-Zwang für Remote-Server, SSRF-Blockade privater LAN-IP-Bereiche und striktes Host-Pinning für API-Schlüssel mit Unit-Tests (`CustomEndpointTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 055 erledigt:** „Modellnamen verwalten“ — Modellkatalog und manuelle Eingabe (`ModelRegistry`, `ModelDescriptor`), Unterscheidung verifizierter Modelle von manuellen Nutzereingaben (`ModelOrigin`) und anfragefreie Modellauswahl mit Unit-Tests (`ModelNameTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 043 erledigt:** „Anbieter-Katalog“ — Typisierter Katalog für erlaubte Anbieter (`ProviderCatalogRegistry`, `ProviderCatalogEntry`), Dokumentationsbelege, Authentifizierungsarten (x-api-key, Bearer, No-Auth Local), Protokollformate und Fähigkeiten ohne hartcodierte Schlüssel mit Unit-Tests (`ProviderCatalogTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 044 erledigt:** „Anbieter hinzufügen“ — Validierung eigener Anbieteranschlüsse (`ProviderConfigValidator`), HTTPS-Zwang für Remote-Server, Localhost-Freigabe, Modellvalidierung und Vorab-Prüfungsansicht mit maskiertem Schlüssel (`ProviderReviewSummary`) mit Unit-Tests (`ProviderConfigTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 045 erledigt (Gate):** „Schlüssel sicher speichern“ — Hardware-gestützte Keystore-Architektur (`AndroidKeystoreSecurityPolicy`, AES-256-GCM), zwingender Ausschluss aus Cloud-/Auto-Backups, Verbot von Klartextspeicherung und sichere Schlüsseltresor-Schnittstelle (`KeyVaultStorage`, `InMemorySecureKeyVault`) mit Unit-Tests (`SecureKeyStorageTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 060 erledigt:** „Bedingungen und Prüfdatum“ — Re-Audit-Mechanismus (`ProviderTermsEngine`), 90-Tage-Ablauffrist (`VerificationStatus`), Kennzeichnung offizieller ToS und strikte Deprecation von Scraping-/Abo-Umgehungsmustern mit Unit-Tests (`ProviderTermsTest.kt`) verifiziert (`done_since_last_edit: true`).
- **Welle 9 (W9) vollständig abgeschlossen:** Tasks 038, 039 verifiziert.
- **Welle 10 (W10) vollständig abgeschlossen:** Tasks 043, 044, 045, 060 verifiziert.
- **Welle 11 (W11) in Arbeit:**
  - **Task 046 erledigt (Gate):** „Geheimnisse verbergen“ — Zentrale Secret-Redaction (`SecretMasker`, `RedactionAuditReport`), Filterung von Anthropic Keys, OpenAI Keys, Bearer Tokens, Passwörtern und PEM-Blöcken, Defense-in-Depth Invariante mit Unit-Tests (`SecretMaskerTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 047 erledigt:** „Verbindung testen“ — Minimale Testanfrage (`ProviderPingTester`, 1 Token Ping), Abwesenheit von Projektdaten, transparente Kostenangabe und Blockade unsicherer Weiterleitungen mit Unit-Tests (`ProviderPingTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 052 erledigt (Gate):** „Eigener Endpunkt“ — Sicherheitsprüfung für benutzerdefinierte Server (`CustomEndpointGuard`), TLS-Zwang für Remote-Server, SSRF-Blockade privater LAN-IP-Bereiche und striktes Host-Pinning für API-Schlüssel mit Unit-Tests (`CustomEndpointTest.kt`) verifiziert (`done_since_last_edit: true`).
  - **Task 055 erledigt:** „Modellnamen verwalten“ — Modellkatalog und manuelle Eingabe (`ModelRegistry`, `ModelDescriptor`), Unterscheidung verifizierter Modelle von manuellen Nutzereingaben (`ModelOrigin`) und anfragefreie Modellauswahl mit Unit-Tests (`ModelNameTest.kt`) verifiziert (`done_since_last_edit: true`).
- **Nächste Aufgabe: Task 061:** „Modellfähigkeiten“ (W13, Abhängigkeiten: keine, Skills `/claude-api` + `testing-setup`).
  - Ziel: Fähigkeiten eines Modells (Streaming, Werkzeugaufrufe, Bildeingabe) nur aus belegter Quelle ableiten, nie aus dem Modellnamen raten. Ohne Beleg: „unbekannt“, Aktion wird blockiert statt geraten.
  - Arbeitsdateien: `app/src/main/java/org/claudroide/app/feature/provider/ModelCapabilityRegistry.kt` (liegt bereits uncommitted vor), zugehörige Testdatei, `tasks/061-model-capability-labels.md`, `progress/BUILD-STATE.md`.
  - Geladene Skills: `/claude-api`, `testing-setup`.
- `python3 tools/sync_frontmatter.py --check` läuft grün über alle 135 Task-Dateien (58 erledigt, 77 offen).

## Wiederaufnahme 2026-10-01 — verifizierter Stand

Der alte Checkpoint war veraltet (nannte Task 056 als „nächste Aufgabe“, obwohl der letzte Commit W12 abgeschlossen hatte). Verifikation gegen Git und Dateien ergab: W11 und W12 sind erledigt, Task 036 war die früheste offene Aufgabe mit erfüllten Abhängigkeiten.

**Vorgeschaltete Reparatur des Builds (kein Task, aber Voraussetzung für jede Prüfung):**
- `app/build.gradle.kts`: `NavigationSuiteScaffold` war nicht auflösbar, weil das Artefakt `androidx.compose.material3:material3-adaptive-navigation-suite` weder deklariert noch im Cache war. Ergänzt (Version 1.0.0) — damit kompiliert die App überhaupt erst.
- `org.json` fehlt auf dem JVM-Test-Classpath (nur Android-Plattform). `testImplementation("org.json:json:20240303")` ergänzt, damit SSE-JSON wirklich getestet und nicht gegen einen Stub geprüft wird.
- **Echter Barrierefreiheitsfehler gefunden und behoben:** `SemanticThemeColors.LightWarningText` (#E65100) erreichte auf `LightWarningBackground` nur 3,46:1 und lag damit unter WCAG AA (4,5:1) — der Test hatte den Istwert nie geprüft, nur die Farbe nachgeschlagen. Ersatz #BF360C (5,11:1), warmer Ton bleibt erhalten. Die Kommentare „5.2:1“ und „6.1:1“ waren ebenfalls falsch und wurden auf gemessene Werte korrigiert.
- `TypeAndSpacingTest.codeFont_usesMonospace` prüfte `toString()` eines Compose-Objekts („Monospace“ statt „FontFamily.Monospace“). Statt die Zeichenkette zu verbiegen, vergleicht der Test jetzt das `FontFamily`-Objekt selbst — die Debug-`toString()` ist kein Vertrag.

**Task 036 erledigt — „Laufende Antworten“:**
- `StreamingResponseEngine.kt` / `SseEventParser.kt` in `feature/chat/`: Zustandsautomat (IDLE → CONNECTING → RECEIVING → COMPLETED/ABORTED/FAILED) über reine, synchrone Datenklassen — kein Socket, kein Coroutine, daher auf der JVM prüfbar.
- Nur `COMPLETED` gilt als fertige Antwort; `isComplete` ist bei Abbruch und Fehler immer `false`. Ereignisse nach einem Endzustand werden verworfen, damit ein spät eintreffendes Delta den Text nicht verdoppelt.
- **Echter Parserfehler gefunden und behoben:** das letzte SSE-Frame ging verloren, wenn die Verbindung direkt nach `data:` schloss. Genau das passiert bei Abbruch — das `message_stop`, das die Antwort als fertig markiert, wäre ausgefallen. `parseAll()` spült jetzt den Puffer am Streamende.
- Werkzeug-Argumente (`partial_json`) werden in Index-Reihenfolge zusammengesetzt und nie als sichtbarer Text gerendert; `thinking_delta` wird geparst, aber nie angezeigt. Unbekannte Ereignistypen und kaputte JSON-Zeilen brechen den Stream nicht ab.
- Unit-Tests: `StreamingResponseTest.kt` (18 Tests) deckt beide „Fertig, wenn“-Kriterien ab.

**Teststand:** `./gradlew :app:testDebugUnitTest` → 267 Tests, 0 Fehler (vorher 249, davon 2 rot seit dem 30.09.). Keine Geheimnisse in den neuen Dateien.

## Nächster Schritt
1. Task 061 („Modellfähigkeiten“) umsetzen: `ModelCapabilityRegistry.kt` liegt bereits uncommitted vor und muss gegen die Belegpflicht geprüft und mit Tests versehen werden.
2. Nach Task 061 committen und stagen; Push nur nach ausdrücklicher Freigabe (siehe Offen).
3. Anschließend Task 062 („Modellwahl“), dann W13c (064 Preise, 065 Limits).

## Offen
- PNG-/WebP-Logo über die Media Bridge des Nutzers rendern und prüfen.
- A56-Gerätewerte, Android-Version, Lizenz, finale Anbieterwege vor Implementierung bestätigen.
- **Nicht nachgefragt — Entscheidung nötig:** Viele Dateien liegen seit dem Abbruch uncommitted vor. Betroffen sind (a) reine Kosmetik — typografische Anführungszeichen in vier Provider-Dateien, (b) `ModelCapabilityRegistry.kt` und `ModelSelectionManager.kt` (vermutlich W13-Vorarbeit zu den Tasks 061/062), (c) Build-Gerüst: `gradlew`, `gradlew.bat`, `gradle/`, `gradle.properties`, `mipmap-*`, `values/colors.xml`, `proguard-rules.pro`. Ohne Freigabe nichts davon gestaged oder gepusht. `local.properties` enthält `sdk.dir` und gehört **nie** ins Repository — es ist in keiner `.gitignore`-Regel abgedeckt und muss vor jedem `git add -A` ausgeschlossen bleiben.
