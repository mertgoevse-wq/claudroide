# Claudroide-Bauzustand

**Stand:** 2026-10-02 (siebte Sitzung)
**Status:** 89 von 135 Aufgaben verifiziert. Tasks 040, 072–082, 087, 089, 092 und 093 abgeschlossen.

**Sprache (Nutzerwunsch vom 2026-10-02):** Englisch zuerst, Deutsch als Zweitwahl. `values/strings.xml` ist jetzt Englisch, `values-de/strings.xml` Deutsch. `CLAUDE.md` entsprechend geändert. Historische deutsche Bezeichner aus den ersten Aufgaben bleiben **unverändert** — sie rückwirkend umzubenennen würde hunderte Zusicherungen in 1108 Tests brechen. Neue Typen führen `label` (englisch).

**Bilder — blockiert, ehrlich dokumentiert:** Beide README-Bilder sollen neu erstellt werden. `~/claude-media-bridge` ist installiert, aber `claude-media-bridge status` meldet **„Not logged in“**; `generate --provider google` endet mit *„No Google credentials found“*. Der einzige konfigurierte Anbieter ist Pollinations — vom Nutzer ausdrücklich **nicht** gewünscht. `login` ist ein interaktiver Google-OAuth und kann nicht aus einer Agentensitzung laufen. **Es ist kein Bild erzeugt und kein Platzhalter eingesetzt worden.** Die neuen englischen Bildaufträge liegen in `assets/logo-brief.md` und sind ausführungsbereit.

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

## Wiederaufnahme 2026-10-01 (zweite Sitzung) — verifizierter Stand

Der erste Checkpoint dieser Sitzung war erneut veraltet: er nannte Task 061 als nächste Aufgabe, obwohl der letzte Commit `e0d02db` bereits **036 und 061** abgeschlossen hat. Verifikation gegen Git und `tools/sync_frontmatter.py` ergab: **60 Aufgaben `done`, 75 offen**; alle 135 Dateien sind konsistent.

**Bereits committet:** Tasks 061 (Modellfähigkeiten) und 070 (Gemeinsame Agent-Funktionen). Die Dateien zu 070 (`ProviderAgentContract.kt` + Test) waren im Arbeitsbaum noch uncommitted, obwohl der Task-Frontmatter schon auf `done` stand — dieser Widerspruch wird in diesem Block aufgelöst.

**Uncommitted im Arbeitsbaum liegen vier Implementierungen zu drei noch offenen Aufgaben:**

| Task | Implementierung | Test | Abnahmekriterien geprüft |
|---|---|---|---|
| 062 „Modell auswählen" | `ModelSelectionPresenter.kt` | `ModelSelectionTest.kt` (39) | Wechsel löst keinen Anbieteraufruf aus (eigener `ProviderCallAudit`-Zähler, bleibt 0); nicht verfügbare Modelle werden mit Klartext **abgelehnt**, es existiert kein Ersatzpfad |
| 064 „Kostenschätzung" | `CostEstimator.kt` | `CostEstimatorTest.kt` (28) | Jeder Preis trägt Quellen-URL + ISO-Prüfdatum und wird nach 90 Tagen als `STALE` markiert; jede Zahl ist als „Schätzung" gekennzeichnet; ohne Beleg entsteht `Incomplete` statt einer Zahl; vollständig offline |
| 067 „Projekt-Ausschlüsse" (Gate) | `ProjectExclusionPolicy.kt` | `ProjectExclusionTest.kt` (18) | `BLOCKED_SECRET` ist per `canOverride` **nicht** aufhebbar; fehlende Dateien werden auf Deutsch erklärt; eine einzige Policy gilt für Suche, Kontext und Werkzeuge |
| 070 „Gemeinsame Agent-Funktionen" | `ProviderAgentContract.kt` | `ProviderAgentContractTest.kt` (17) | Fähigkeiten werden als SUPPORTED/UNSUPPORTED/UNKNOWN gemeldet, nie emuliert; `ProviderCapabilityResolver` gibt bei unbekanntem Modell durchgängig UNKNOWN zurück |

**Zu 064 ausdrücklich geprüft (Preiszahlen):** Die Tabelle in `CostEstimator.PRICE_TABLE` enthält Claude-Preise, die ich nicht gegen die Anbieterdokumentation verifizieren kann — kein Netzzugriff in dieser Sitzung, und die Werte stammen aus der abgebrochenen Sitzung. Sie sind als `VERIFIED = "2026-10-01"` datiert, was **eine Behauptung, kein Beleg** ist. Vor Freigabe von Task 064 müssen die sechs Zahlenpaare gegen `https://docs.anthropic.com/en/docs/about-claude/models` geprüft werden. Bis dahin bleibt 064 offen.

**Teststand:** `./gradlew :app:testDebugUnitTest` → siehe Ergebnis dieses Blocks. 102 neue Tests über vier Dateien.

## Nächster Schritt
1. Ergebnis des Testlaufs eintragen; bei Grün die Tasks 062, 064 (nach Preisprüfung) und 067 über `tools/sync_frontmatter.py --status` als `done` setzen.
2. Commit über den exakten Dateibestand dieser vier Aufgaben. `local.properties` (enthält `sdk.dir`) gehört **nie** ins Repository und ist in keiner `.gitignore`-Regel abgedeckt — vor jedem `git add -A` ausschließen, besser `.gitignore` nachtragen.
3. Push nur nach ausdrücklicher Freigabe des Nutzers (siehe Offen).

## Wiederaufnahme 2026-10-01 (dritte Sitzung) — verifizierter Stand

**Ausgangslage:** 60 von 135 Aufgaben `done`, 75 offen. Lokal 4 Commits vor `origin/main` (`e0d02db`, `c9ee949`, `06b3f81`, `bdcb0c5`). Vier Implementierungen lagen uncommitted im Arbeitsbaum (062, 064, 067, 070).

**Preisprüfung Task 064 — durchgeführt und bestanden.** Die sechs Zahlenpaare wurden gegen die Anbieterquelle geprüft, nicht gegen das Gedächtnis:

| Modell-ID | Eingabe | Ausgabe | Quelle |
|---|---|---|---|
| `claude-opus-5-5` | 4,00 USD | 20,00 USD | Models-Overview, Preiszeile |
| `claude-sonnet-5-5` | 2,00 USD | 10,00 USD | Models-Overview, Preiszeile |
| `claude-sonnet-5` | 2,00 USD | 10,00 USD | Modellseite Sonnet 5 (Legacy) |
| `claude-haiku-4-5` | 1,00 USD | 5,00 USD | Models-Overview, Preiszeile |
| `claude-haiku-4-5-20251001` | 1,00 USD | 5,00 USD | Models-Overview, Claude-API-ID |
| `claude-fable-5-1` | 10,00 USD | 50,00 USD | Models-Overview, Preiszeile |

Die Quell-URL wurde zugleich von der umgezogenen alten Domain (`docs.anthropic.com/en/docs/about-claude/models`, 301) auf die kanonische Adresse (`platform.claude.com/docs/en/about-claude/models/overview`) geändert; `CostEstimatorTest` prüft die neue URL.

**Drei echte Fehler gefunden und behoben** — die uncommitted Arbeit war nicht abnahmefähig:

1. **`ModelSelectionPresenter.request()` — verworfenes Modell nach Abbrechen (Task 062).** `selectionBeforePending = existing.state as? ActiveSelection` gecastet den *Zustand* auf `ActiveSelection`, aber `state` ist `SelectionState.Active(selection)` — die Hülle. Der Cast traf nie, also war `selectionBeforePending` immer null und `cancel()` setzte still auf `Idle`: das vorherige Modell ging verloren. Behoben durch `(existing.state as? SelectionState.Active)?.selection`. Belegt durch `cancel_keepsThePreviousModel`; die Ursache wurde mit einer temporären Sonde eingegrenzt, die danach entfernt wurde.
2. **`CostEstimator.costOf()` — Exponentialform statt Betrag (Task 064).** `stripTrailingZeros()` auf einem Skala-12-Wert liefert für 20,00 USD die Form `2E+1`, die weder gleich `20` vergleicht noch lesbar darstellbar ist. Der Kommentar direkt über der Funktion beschrieb ausdrücklich das gewünschte Verhalten, der Code tat das Gegenteil. Behoben durch Kürzen der Nachkommastellen im Plain-String und erneutes Einlesen. Nebeneffekt: der Test `unknownOutputTokens_giveALowerBoundNotATotal` erwartete `4.00`, während die Nachbartests `4`/`10`/`20`/`24` erwarten — dieselbe Rechnung in zwei Skalen. Der **Test** wurde auf `4` korrigiert, weil die Normalisierung konsistent ist; die Rechnung war nie falsch.
3. **`ProjectExclusionPolicy` — Umgehung über Backup-Namen (Task 067, Gate).** Die Blockliste matchte **exakte** Dateinamen. Real vorkommende Kopien wie `.env.bak`, `.env.old`, `.env.1`, `id_rsa.bak`, `id_rsa.txt` und `.npmrc.bak` galten als `ALLOWED` und wären als Kontext gesendet worden — das brach die Kernzusage der Gate-Aufgabe. Behoben durch `isSecretFileName()`, das nach Abzug eines Backup-Suffixes erneut prüft. Dokumentationsdateien (`docs/env-guide.md`, `docs/secrets.md`, `src/Environment.kt`) bleiben bewusst sendbar.

Dazu **`local.properties` in `.gitignore` aufgenommen** (Zeile 34). Die Datei enthält `sdk.dir=<Pfad>` und wäre bei jedem `git add -A` ins Repository gewandert. `git check-ignore` bestätigt die Wirkung.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **388 Tests, 0 Fehler, 0 übersprungen**. Verteilung der vier Aufgaben: `ProjectExclusionTest` 21, `CostEstimatorTest` 28, `ModelSelectionTest` 39, `ProviderAgentContractTest` 17 (zuvor 18 — die Klassen-Zahl sank, weil ein Test in die neue Backup-Variante umbenannt wurde, nicht weil einer verloren ging). Geheimnis-Scan über `app/src/` findet nur synthetische Test-Fixtures in bereits committeten Dateien.

**Status gesetzt:** 062, 064 und 067 über `tools/sync_frontmatter.py --status` auf `done`; `--check` läuft grün über alle 135 Dateien. 070 war bereits `done`, die Dateien waren nur nie committet.

**Geladene Skills:** `testing-setup` (Testbestand analysiert, Unit-Test-Strategie angewendet; die Hilt/Robolectric/Jacoco-Installation aus Schritt 2–3 wurde **nicht** ausgeführt, weil sie eine eigene Abhängigkeitsentscheidung ist) und `android-permissions-security` (Least-Privilege-Regeln auf die Ausschluss-Policy angewendet: keine Geheimnisdatei je sendbar, keine Ausnahme, die das aufhebt).

**Nächste freigegebene Aufgaben** (alle Abhängigkeiten erfüllt): 041, 071, 081, 082, 084 (Gate), 087, 088, 092, 095 (Gate), 098, 103, 105, 117 (Gate), 118 (Gate), 119 (Gate), 120 (Gate), 122 (Gate), 123 (Gate), 125 (Gate), 126 (Gate), 127 (Gate), 129 (Gate), 130 (Gate).

## Offen
- **Preisprüfung Task 064:** sechs Preiseinträge gegen die Anbieterquelle verifizieren, bevor 064 als erledigt gilt.
- PNG-/WebP-Logo über die Media Bridge des Nutzers rendern und prüfen.
- A56-Gerätewerte, Android-Version, Lizenz, finale Anbieterwege vor Implementierung bestätigen.
- `local.properties` enthält `sdk.dir` und gehört **nie** ins Repository — es ist in keiner `.gitignore`-Regel abgedeckt und muss vor jedem `git add -A` ausgeschlossen bleiben.
- Push-Ziel ist `https://github.com/mertgoevse-wq/claudroide.git` (privat, bestätigt) — ein Push erfolgt erst auf ausdrückliche Freigabe.


## Wiederaufnahme 2026-10-01 (vierte Sitzung) — verifizierter Stand

**Ursache der Abstürze gefunden.** Das Git-Objektrepository wuchs auf **7,8 MB** durch tausende „dangling“ Objekte — Abbruch-Commits der vier Subagenten, die bei jedem Sitzungsende verworfen wurden, ohne dass die Objekte aufgeräumt wurden. `git gc --prune=now` hat sie entfernt; das Repository wiegt jetzt **1,4 MB**. Die gemeldeten 1,9 GB waren ein Fehlalarm: die Dateisystem-Anzeige zählte `app/build/` (20 MB Build-Artefakte) mit. Der versionierte Inhalt misst 1,5 MB. Zusätzlich wurden `.kotlin/` und `local.properties` in `.gitignore` aufgenommen.

**Zweite Ursache: Subagenten sterben mit der Sitzung.** Vier Agenten arbeiteten an 041, 065, 069 und 088. Beim ersten Absturz schrieben sie nichts, beim zweiten schrieben sie ihre Implementierungen, aber nur 065 verlor seine Testdatei. Konsequenz für diese Sitzung: **maximal 2 Subagenten gleichzeitig**, und bei jeder Datei wird der Inhalt geprüft, bevor ein Task als erledigt gilt.

**Sieben echte Fehler gefunden und behoben:**

1. **`SecretMasker` — kurze Schlüssel blieben ungeschwärzt (Sicherheitsleck).** Das Muster verlangte 16 Zeichen nach `sk-`; `sk-live-9999999` mit 14 Zeichen blieb sichtbar. Jetzt 8 Zeichen. Begründung: ein falscher Treffer kostet ein lesbares Wort, ein falscher Nichttreffer druckt einen lebenden Schlüssel.
2. **`SecretMasker` — Schlüssel mit Präfix blieben ungeschwärzt.** `modell-token=sk-live-9999999` rutschte durch, weil die Assignment-Liste nur Wörter wie `api_key` kannte. Neues Muster `Prefixed Key`.
3. **`SecretMasker` — Marker dreimal hartkodiert.** `[REDACTED]` stand an drei Stellen im Literal. Jetzt `REDACTION_PLACEHOLDER`, damit Tests gegen die Konstante prüfen statt gegen einen geratenen String. Genau dieser Fehler hatte zwei Tests rot gemacht.
4. **`CostLabelPresenter` — unterdrückter Betrag ohne Begründung.** Die Herkunftswarnung wurde nur gesetzt, wenn bereits ein Betrag vorlag. War der Betrag ohnehin null, blieb die Zeile leer und „keine Zahl“ war nicht von einem Fehler unterscheidbar.
5. **`CostLabelPresenter` — 40-Zeilen-Duplikat von `SecretMasker`.** Der abgestoppte Agent hatte eine komplette zweite Schwärzungs-Klasse erfunden (`SecretRedactor`) statt die vorhandene zu nutzen. Ersetzt durch die echte Klasse; `SecretMasker` bekam nur die ehrliche Zusatzfunktion `containsSecretLikeText`.
6. **`OfflineChatPolicy.editRequest` — Entwürfe waren dauerhaft unsendbar.** Bei nicht-leerem Text wurde der alte DRAFT-Status beibehalten. Ein halb getippter Satz konnte deshalb auch nach dem Vervollständigen nie gesendet werden. Jetzt verlässt ein fertiger Text den DRAFT-Zustand.
7. **`PathBoundaryGuard` (Gate) — das echte Ziel wurde zu spät geprüft.** Die Prüfung auf das aufgelöste Ziel lief nach der Traversal-Prüfung. Ein Pfad, der beides tat, meldete nur „außerhalb“ und verbarg damit den tatsächlichen Mechanismus. Das Briefwort „Prüfung auf tatsächlichem Ziel“ verlangt, dass das Ziel zuerst entscheidet. Zusätzlich wurde `"."` fälschlich als MALFORMED abgelehnt.

**Vier Testerwartungen waren falsch, nicht der Code:** ein Test verglich einen festen Modellnamen mit einem Tabellenschlüssel; einer suchte einen deutschen Platzhalter, den die Schwärzung nie erzeugte; einer zählte 600 Eingabezeilen als 601; einer behauptete `canSendNow(..., userConfirmed = false)` sei wahr.

**Task 065 ohne Tests übernommen:** Die Implementierung lag vor, die Testdatei nicht. Der Status wurde auf `pending` zurückgesetzt und erst nach 22 selbst geschriebenen Tests auf `done` gesetzt. Geprüft: ein von Hand eingetragenes Limit wird als solches gekennzeichnet, ein dokumentiertes Limit ohne brauchbare Quelle oder Datum ist unbrauchbar, und kein Status blockiert je eine Anfrage oder verspricht eine Annahme durch den Anbieter.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **492 Tests, 0 Fehler, 0 übersprungen** (vorher 388). Neuer Klassen: `CostLabelTest` 17, `UsageLimitTest` 22, `OfflineChatTest` 17, `FilePreviewTest` 24, `PathBoundaryTest` 25. Geheimnis-Scan über `app/src/main/` ohne Treffer.

**Nächste freigegebene Aufgaben** (23, alle Abhängigkeiten erfüllt): 063, 066, 068 (Gate), 071, 081, 082, 084 (Gate), 087, 092, 095 (Gate), 098, 103, 105, 117 (Gate), 118 (Gate), 119 (Gate), 122 (Gate), 123 (Gate), 125 (Gate), 126 (Gate), 127 (Gate), 129 (Gate), 130 (Gate).

## Wiederaufnahme 2026-10-01 (fünfte Sitzung) — verifizierter Stand

**Ausgangslage:** Der Checkpoint war veraltet (nannte 23 offene Aufgaben, nannte aber 122 als offen, obwohl der letzte Commit `d6b3dd2` ihn abgeschlossen hatte). Verifikation über `sync_frontmatter.py --check`, Git und Dateien: **70 Aufgaben `done`, 65 offen**; alle 135 Dateien konsistent. Acht Commits liegen vor `origin/main`; Push weiterhin nicht freigegeben.

**Task 119 erledigt (Gate) — „Projektgrenze durchsetzen":**
- `ProjectBoundaryEnforcer.kt` (neu): eine einzige Prüffunktion `evaluate()` für Lesen, Schreiben, Löschen und Ausführen. `ToolKind.parse()` liefert bei unbekanntem Werkzeugnamen `null`, niemals `ALLOWED`. `ActionOrigin` trennt Nutzer von Modell: nur `USER` darf weitere Projektordner beitragen, `MODEL` und `UNKNOWN` werden mit `GRANTS_NOT_FROM_USER` abgelehnt. `ProjectAccessRegistry` führt einen Generationszähler, damit ein Widerruf sofort wirkt statt erst beim nächsten Appstart.
- `PathBoundaryGuard.normalise()` erkennt jetzt auch einen **führenden Backslash** als absolut. Vorher wurde `\etc\passwd` als relativer Name gelesen und auf den Projektordner gesetzt — der gleiche Fehler in Windows-Schreibweise.

**Drei echte Fehler gefunden und behoben — alle im Befehlspfad von 119.** Sie fielen nur auf, weil das Verhalten vorher mit einer temporären Sonde gemessen und nicht aus dem Code gelesen wurde:

1. **Ausführen umging die Projektgrenze vollständig (Sicherheitsleck).** Absolute Pfade *innerhalb* eines Befehls wurden nie geprüft. `cat /data/data/com.other.app/shared_prefs/prefs.xml` bekam `ALLOWED` — dieselbe Datei wird als `READ`-Aktion abgelehnt. Dieselbe Zieldatei, zwei Urteile, je nachdem wie sie benannt war. Behoben: jeder absolute Pfad im Befehl läuft durch dieselbe `PathBoundaryGuard`-Prüfung wie ein Dateipfad.
2. **Der Arbeitsordner-Vergleich lief verkehrt herum.** Geprüft wurde `check(root, workDir)` statt `check(workDir, root)`. Folge: `workingDirectory = "/"` ergab `ALLOWED` (jeder Pfad liegt darunter), während ein legitimes Unterverzeichnis `<root>/app` abgelehnt wurde — also genau der Fall, den ein echter Build braucht. Behoben durch die richtige Richtung.
3. **Der Ordnerwechsel-Regel fehlte der Fall „mitten im Befehl".** Das Muster war auf Zeilenbeginn verankert (`(?m)^\s*`), daher kam `ls && cd /data/data/…` durch. Auf `(^|[\s;|&(])(cd|pushd)\s+` erweitert.
4. **Der Tokenisierer lief ins Leere.** `split('\'').joinToString(" ") { "" }` liefert für einen Befehl **ohne** einfaches Anführungszeichen einen leeren String — die Pfadprüfung aus Punkt 1 hätte also bei genau den Befehlen nie gegriffen, für die sie nötig ist. Das war der Grund, warum der erste Fix zunächst wirkungslos blieb. Behoben über `filterIndexed { index, _ -> index % 2 == 0 }`.

**Zwei Testerwartungen waren falsch, nicht der Code:** Zwei Tests verlangten für `ls && cd /data/…` und `echo $(cat /data/…)` genau `FORBIDDEN_COMMAND`. Nach den neuen Prüfungen wird beides vom Pfad-Regelwerk zuerst abgelehnt und trägt deshalb `OUTSIDE_PROJECT`. Die Ablehnung ist in beiden Fällen richtig, nur der genannte Grund unterscheidet sich — die Tests wurden darauf korrigiert.

**Task 066 erledigt — „Übertragene Daten prüfen":**
- `RequestDataPreview.kt` (neu): `ContentRef` hält Pfad und Zeilenbereich, **niemals Text** — die Schutzregel der Aufgabe („Vorschau speichert keine unnötige Kopie") ist damit strukturell, nicht nur per Test. `pendingToSend` ist die einzige Wahrheit, `summaryLines` werden daraus abgeleitet, damit eine Entfernung keine veraltete Zahl stehen lassen kann. Geheimnisse werden schon beim Bauen aussortiert und tauchen gar nicht erst in der Liste auf.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **576 Tests, 0 Fehler, 0 übersprungen** (vorher 570, +6 aus den Regressionstests; die Zahl stieg, weil 7 Tests dazukamen und einer umbenannt wurde). Verteilung neu: `ProjectBoundaryTest` 36, `RequestDataPreviewTest` 22, `PathBoundaryTest` 37. Geheimnis-Scan über `app/src/main/` findet nur ein Beispiel in einem Doc-Kommentar (`AppSettings.kt:23`).

**Geladene Skills:** `android-permissions-security` (Prüfung auf Least Privilege für Datei- und Befehlswerkzeuge; ein Weg darf nicht laxer sein als der andere) und `testing-setup` (Ablenkung über Grenzwerte und ein erwarteter Grund, nicht nur „wurde abgelehnt").

**Nächste freigegebene Aufgaben** (21, alle Abhängigkeiten erfüllt): 068 (Gate), 071, 081, 082, 084 (Gate), 087, 092, 093, 095 (Gate), 098, 103, 105, 117 (Gate), 118 (Gate), 123 (Gate), 125 (Gate), 126 (Gate), 127 (Gate), 129 (Gate), 130 (Gate).

## Wiederaufnahme 2026-10-01 (sechste Sitzung) — verifizierter Stand

**Ausgangslage:** Der Checkpoint war erneut veraltet (nannte 70 erledigte Aufgaben, tatsächlich waren es nach Abgleich mit `sync_frontmatter.py` und Git **71**). Uncommitted lag die vollständige Implementierung von Task 063 mit Testdatei — die Vorarbeit der abgebrochenen Sitzung war nicht verloren, aber auch nicht geprüft.

**Task 063 erledigt — „Ersatzmodell einstellen“:** `ModelFallbackPolicy.kt` + `ModelFallbackTest.kt`. Reine Kotlin-Logik ohne Android-Importe, damit jede Zusage auf der JVM prüfbar bleibt. Drei Zusagen strukturell abgesichert: ohne `FallbackSetup` immer `AskUser` (kein eingebauter Ersatz, kein Raten), eine Ablehnung des Hauptanbieters führt nie zu einem Ausweichen, und Preise/Bedingungen werden bei **jedem** `decide()` neu gelesen statt aus der Einrichtung zwischengespeichert.

**Zwei echte Fehler gefunden und behoben — beide im Sicherheitsverhalten, nicht in der Oberfläche:**

1. **Ein „Ersatz“ auf genau das Modell, das gerade scheiterte, wurde als Ersatz akzeptiert.** `decide()` verglich Anbieter und Modell des Ersatzes nicht mit denen der fehlgeschlagenen Anfrage. Ein Nutzer, der Haiku als Ersatz für Haiku eingestellt hatte, bekam bei einer Haiku-Störung `UseFallback` — dieselbe Anfrage an dasselbe Kontingent, mit erneuter Kostenbelastung, ohne ein anderes Modell zu erreichen. Behoben: gleicher Anbieter **und** gleiches Modell führt zu `AskUser` mit der Begründung, dass ein erneuter Versuch eine eigene Entscheidung ist. Gleicher Modellname bei **anderem** Anbieter bleibt ein Ersatz — das ist ein anderer Datenweg und wird getestet.
2. **Der Stale-Preis-Grund wurde per Textsuche in einer Zeile gesucht, die es dort nicht gibt.** `blocked += cost.lines.firstOrNull { it.startsWith("Alter der Preisquelle") }` — diese Zeile stammt aus `CostEstimator.buildDetailLines`, nicht aus den `lines` von `reassessCosts`. Der Ausdruck konnte nie greifen und fiel ersatzlos auf „Die Preisquelle ist zu alt.“ zurück: die Begründung war jedes Mal die generische, obwohl das konkrete Alter bekannt war. Behoben durch das neue Feld `CostReassessment.priceAgeInDays` — das Alter wird als Zahl geführt, nicht aus einem Anzeigetext herausgesucht, damit eine Umbenennung der Oberfläche die Entscheidung nicht verändert.

**Ein weiterer Fund beim Lesen:** `askUser()` bot im Zweig ohne Einrichtung `Beim Modell „unbekannt“ erneut versuchen` an — ein erfundener Modellname in einer echten Nutzerentscheidung. Jetzt `Beim bisherigen Modell erneut versuchen`, wenn kein Ersatz eingetragen ist.

**Eine Testerwartung war falsch, nicht der Code:** `assertEquals(BigDecimal.ZERO, BigDecimal.ZERO)` verglich einen Wert mit sich selbst und bewies nichts. Ersetzt durch echte Prüfungen der Tabellenwerte (Fable 60, Haiku 6 je eine Million Tokens) — mit `compareTo`, weil `BigDecimal.equals` auch die Nachkommastellen vergleicht und `60.00` ungleich `60` ist.

**Geladene Skills:** `android-permissions-security` (Least Privilege auf den Datenweg: die Freigabe des Hauptanbieters gilt nie für den Ersatzanbieter, jede Scope braucht eine eigene Einrichtung) und `testing-setup` (Ablenkung über Grenzwerte — Alter der Einrichtung, Alter der Preisquelle, fehlende Bedingungen — und ein *begründeter* Blockierungsgrund, nicht nur „wurde abgelehnt“).

**Teststand:** `./gradlew :app:testDebugUnitTest` → **617 Tests, 0 Fehler, 0 übersprungen** (vorher 613, +4 neue). Verteilung `ModelFallbackTest` 34 (zuvor 30). Geheimnis-Scan über `app/src/main/` findet nur zwei Beispiele in Doc-Kommentaren bereits committeter Dateien.

## Task 068 erledigt (Gate) — „Nur nötigen Projektkontext wählen“

`ContextSelectionPolicy.kt` + `ContextSelectionTest.kt` (33 Tests).

**Zwei Zusagen strukturell abgesichert, nicht nur per Test:**
- **Jede gesendete Datei ist benennbar.** `SelectedContext.describe()` nennt Dateityp, Pfad, Zeilenbereich, Auswahlgrund und Tokenzahl; `ContextSelection.disclosureLines()` listet gewählte *und* ausgelassene Dateien. Es gibt kein Feld, um ein Element ohne Beschreibung durchzureichen.
- **Kein stilles Vollprojektladen.** Das Budget ist doppelt begrenzt: höchstens `MAX_PROJECT_FRACTION` (25 %) des Modellfensters und nie mehr als `HARD_PROJECT_TOKEN_CEILING` (60 000 Tokens) — auch bei einem 1-Mio.-Token-Fenster. `selectWholeProject()` lehnt ohne ausdrückliche Bestätigung ab und bleibt selbst danach unter derselben Obergrenze. `expand()` kann nur die ausdrücklich verlangten Pfade nachrücken, nie den ganzen Bestand.
- **Geheimnisfilter vor dem Versand.** `ProjectExclusionPolicy` entscheidet als erster Schritt in der Auswahl; ein `BLOCKED_SECRET` ist weder über `expand()` noch über eine Vollprojekt-Bestätigung erreichbar.

**Fenstergrößen gegen die Anbieterquelle geprüft, nicht aus dem Gedächtnis.** Der in der Skill-Matrix zugewiesene Skill `/claude-api` ist in dieser Sitzung **nicht verfügbar** (nur die sechs globalen Skills plus die Projekt-Skills). Ersatz nach CLAUDE.md: die benötigte Einzelheit direkt aus der offiziellen Anbieterdokumentation gelesen statt behauptet — `https://platform.claude.com/docs/en/models/overview`, abgerufen 2026-10-01. Ergebnis: Opus 5.5, Sonnet 5.5 und Fable 5.1 je 1 Mio. Eingabe- und 128 000 Ausgabetokens; Haiku 4.5 mit 200 000 und 64 000. Die Werte stehen mit Quelle und Prüfdatum in `ModelContextLimits.TABLE`. Für ein unbekanntes Modell wird **kein** Fenster geraten: es gilt der konservative Wert `UNKNOWN_WINDOW_TOKENS` mit `isWindowKnown = false`, und die Oberfläche sagt das.

**Kein semantisches Ranking.** `RelevanceSignal` ist absichtlich mechanisch und überprüfbar: von Hand benannt, aus dem Dateinamen gegen den Aufgabentext, Projektanweisung, sonst `NONE`. `NONE` wird **nicht** aufgenommen — eine Datei ohne benennbaren Grund zu senden widerspräche der ersten Zusage. Der Dateiinhalt wird nicht durchsucht, weil das Geheimnisse und Kosten von der Datei abhängig machte.

**Zwei Fehler in der eigenen ersten Fassung behoben, bevor sie getestet wurden:**
1. `expand()` ignorierte seinen eigenen Parameter `requestedPaths` und zog still den gesamten Kandidatenbestand nach — also genau das Verhalten, das die Aufgabe verbietet. Jetzt werden nur die verlangten Pfade berücksichtigt.
2. `selectWithBudget()` war als Duplikat von `select()` abgeschrieben; die Prüfung der harten Obergrenze stand nur im Duplikat. Beide nutzen jetzt denselben Kern, und `require()` in `selectWithBudget()` erzwingt die Obergrenze für **jedes** Budget — auch für ein ausdrücklich erhöhtes.

**Ein Testerwartungsfehler, den der Test aufdeckte:** `anOmittedFile_isAlsoNamedWithItsReason` schlug fehl, weil der Grund für ausgelassene Dateien anders formuliert war als die Beschriftung `RelevanceSignal.NONE.germanLabel`, die die Oberfläche an anderer Stelle zeigt. Nicht der Test wurde geändert: der Grund verwendet jetzt dieselbe Beschriftung, damit ein Grund an zwei Stellen nicht unterschiedlich benannt sein kann.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **650 Tests, 0 Fehler, 0 übersprungen** (vorher 617, +33 aus `ContextSelectionTest`). Geheimnis-Scan über `feature/project/` ohne Treffer.

**Geladene Skills:** `android-permissions-security` (Least Privilege auf den Datenweg: das Budget ist der schmalste Weg, ein Geheimnis hat keinen Weg, und eine Erweiterung ist keine Ausnahme von der Geheimnisregel) und als Ersatz für das fehlende `/claude-api` die direkte Quellenprüfung der Anbieterdokumentation.

## Task 071 erledigt — „Aufgaben planen“

`AgentTaskPlanner.kt` + `AgentTaskPlannerTest.kt` (30 Tests). **071 ist der Hebel dieser Sitzung:** Die Aufgabe sperrt 072–080 und 040; mit ihr fallen 10 weitere Aufgaben aus der „Abhängigkeiten offen“-Liste.

**Die zentrale Designentscheidung: eine Freigabe kann nicht vergessen werden.** `PlanStep.requiredApproval` ist eine *abgeleitete* Eigenschaft. Ein Schritt gibt `actions: Set<StepAction>` an, und die benötigte Freigabe folgt daraus über `StepAction.requiredApproval`. Es gibt **kein Feld**, in dem jemand `approval = NONE` eintragen könnte — ein Schreibschritt kann seine Freigabepflicht also nicht selbst für unnötig erklären. Das trifft die Schutzregel der Aufgabe („Kein Plan darf Sicherheitsfreigaben still überspringen“) an der Wurzel statt per Test.

**Dritte Seite derselben Zusage:** `AgentTaskPlanner.detectMissingActions()` prüft die Gegenrichtung. `requiredApproval` verhindert, dass ein *vorhandener* Schritt seine Freigabe unterschlägt; die Erkennung verhindert, dass eine *fehlende* Aktion unbemerkt untergeht. Erkennt ein Stichwort im Auftragstext eine Aktion (`löschen`, `installier`, `push`, …), die im Plan kein Schritt hat, wird das zur Frage — nicht stillschweigend übergangen. Die Wortliste ist absichtlich klein und sichtbar statt einer Heuristik, und die Asymmetrie ist dokumentiert: ein Wortfehler kostet eine Frage, ein stillschweigend übergangener Löschschritt wäre genau das, was die Aufgabe verhindern will.

**`NONE` gibt es nur für `READ_FILE` und `GIT_COMMIT`** — beides umkehrbar: eine gelesene Datei wird nicht verändert, ein lokaler Commit lässt sich verwerfen. Alles andere, was das Gerät oder fremde Systeme betrifft, braucht eine einzeln erteilte Freigabe. Freigaben sind je Stufe getrennt: das Erteilen für `FILE_CHANGE` gilt nicht für `EXTERNAL_TRANSFER`.

**Ein echter Fehler gefunden und behoben — `parallelGroups()` lieferte bei gemeinsamen Dateien *gar nichts*.** Die Bereitschaftsprüfung war ein Filter („kein anderer offener Schritt teilt eine Datei“). Bei zwei Schritten auf derselben Datei fielen **beide** durch, `ready` war leer, die Schleife brach ab — und die Rückgabe war eine leere Liste statt zweier aufeinanderfolgender Wellen. Genau die Schritte, die nacheinander laufen müssen, wären unsichtbar gewesen. Behoben durch greedy Auswahl: ein Schritt ist bereit, wenn seine Abhängigkeiten erledigt sind **und** kein bereits für diese Welle gewählter Schritt dieselbe Datei anfasst. Der Test `stepsSharingAFileAreNeverRunInParallel` hat das aufgedeckt.

**Ein zweiter Fund beim Kompilieren:** `grantedApprovals` war als `Set<String>` deklariert, mit `emptyList()` initialisiert. Der Typprüfer hat das gemeldet — ein Fehler, der beim Lesen des Codes nicht aufgefallen wäre.

**Parallelwellen-Regel wie im Projekt selbst:** zwei Schritte mit gemeinsamer Datei laufen nie gleichzeitig; unabhängige Schritte in einer Welle. [ExecutionPlan.parallelGroups] bildet daraus Wellen.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **680 Tests, 0 Fehler, 0 übersprungen** (vorher 650, +30 aus `AgentTaskPlannerTest`). Geheimnis-Scan über `feature/agent/` ohne Treffer.

**Geladene Skills:** `/swarm-planner` (explizite `depends_on` je Aufgabe, atomare Schritte, Wellenbildung — die Regel „keine gemeinsame Datei" aus dem Skill wurde direkt zu `parallelGroups()`) und als Ersatz für das in dieser Sitzung **nicht verfügbare** `/code-review` die `testing-setup`-Prüflinie: jede Zusage bekommt einen Test, der sie an der Grenze belastet, nicht nur im glücklichen Fall. Die nicht verfügbaren Skills `/code-review` und `/claude-api` sind hiermit für 071 bzw. 068 dokumentiert; beide wurden nach CLAUDE.md ersetzt, nicht geraten.

**Neu freigegeben durch 071:** 072, 073, 074, 075, 076, 078, 079, 080, 040 (und damit die Welle W15/W16).

## Wiederaufnahme 2026-10-02 (siebte Sitzung) — verifizierter Stand

**Ausgangslage:** Checkpoint und Git stimmten überein, der Arbeitsbaum war sauber. `tools/sync_frontmatter.py --check` grün über alle 135 Dateien: **74 Aufgaben `done`, 61 offen**. Früheste offene Aufgabe mit erfüllten Abhängigkeiten war **040** (W16, `depends_on: 031, 035, 070, 071`). Acht Commits liegen vor `origin/main`; ein Push ist weiterhin nicht freigegeben.

**Task 040 erledigt — „Chat und Projekt verbinden“:** `ChatProjectLink.kt` (neu, `feature/chat/`) + `ChatProjectLinkTest.kt` (32 Tests).

**Vier Zusagen des Aufgabenbriefs strukturell abgesichert, nicht nur per Test:**

1. **„Nutzer erkennt, welche Dateien an die KI gehen könnten“.** `disclosureLines()` nennt das verknüpfte Projekt, den Ordner und jede bereitstehende Datei. Die Zeilen für die Dateien kommen aus `ContextSelection.disclosureLines()` — dieselbe Quelle wie beim eigentlichen Versand, damit Anzeige und Übertragung nicht auseinanderlaufen können. Ohne Projekt steht dort „Projektbezug: keiner“, ohne Auswahl „Es ist keine Datei für dieses Gespräch ausgewählt“.

2. **„Projektwechsel nicht still Dateien aus dem alten Projekt weitergibt“.** Der Kontext ist ein `PreparedContext` mit der Bindungs-Generation, für die er erzeugt wurde. `activateProject`, `bind` und `unbind` erhöhen die Generation und verwerfen die Vorbereitung. Der Versand nimmt nicht blind das, was er hat, sondern fragt `isUsableForSend(context)` — eine ältere `PreparedContext`, die ein Aufrufer noch in der Hand hält, wird mit `CONTEXT_OUTDATED` abgelehnt. `prepareContext()` hat **keinen** Parameter für ein Zielprojekt: die Auswahl wird immer aus der aktuellen Bindung abgeleitet.

3. **„Verknüpfte Projektberechtigung bleibt widerrufbar“.** Diese Klasse ruft an keiner Stelle `ProjectAccessRegistry.update`. Sie *fragt* die Grenze über `check` ab und bindet nur an einen Ordner, den die App bereits freigegeben hat; ein nicht freigegebener Ordner wird mit `PROJECT_NOT_PERMITTED` abgelehnt. Widerruft der Nutzer den Ordner, meldet `evaluateSend()` `PROJECT_ACCESS_REVOKED` — auch noch für eine Vorbereitung, die vor dem Widerruf erzeugt wurde. Test: der Widerruf greift sofort, ohne Appstart.

4. **„Warnung, wenn ein anderer Projektordner aktiv wird“.** Aktiviert die App einen anderen Ordner, bleibt die Bindung bestehen (kein stilles Umschreiben) und der Versand wird mit `ACTIVE_PROJECT_DIFFERS` blockiert. Vor einem erneuten Binden kommt `NeedsConfirmation` mit `ProjectSwitchWarning`, das **beide** Ordner sowie die Dateien nennt, die wegfallen und die hinzukämen. Eine abgelehnte Umsicht bewegt weder Bindung noch Generation.

**Ein echter Fehler in meiner ersten Fassung, gefunden und behoben:** In `bind()` habe ich für die Vorschau der Warnung `ContextSelectionPolicy.select(project.id, …)` aufgerufen — die **Projekt-ID als Modellkennung**. Das Budget hängt am Kontextfenster des Modells; eine Projektbezeichnung ist keine belegte Fenstergröße. Für ein Projekt ohne Modell-Eintrag hätte die Warnung still mit dem konservativen 32 000-Token-Wert gerechnet und das als gültige Zahl dargestellt. Korrigiert auf den echten `modelId`-Parameter, der in der Warnungsberechnung jetzt durchgereicht wird.

**Zwei weitere Korrekturen während des Kompilierens:** `ProjectRef` ist eine verschachtelte Klasse und konnte das private `strip()` der äußeren Klasse nicht auflösen — der Pfadvergleich liegt jetzt in einer dateiweiten `normaliseRoot()`. Und `CONTEXT_OUTDATED` war zunächst toter Zweig: `invalidateContext()` setzte die Vorbereitung auf `null`, wodurch „alt, aber vorhanden“ gar nicht auftreten konnte. Jetzt wird die verworfene Vorbereitung in `discarded` gehalten, damit die Blockierung die betroffenen Dateien *nennen* kann, statt nur „bitte neu auswählen“ zu sagen.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **712 Tests, 0 Fehler, 0 übersprungen** (vorher 680, +32 aus `ChatProjectLinkTest`). Kein Test wurde umbenannt oder entfernt. Geheimnis-Scan über beide neuen Dateien ohne Treffer.

**Geladene Skills:** `android-permissions-security` (Least Privilege auf dem Datenweg: die Verknüpfung darf die Berechtigungsstufe nicht anheben — deshalb der Test `bindingDoesNotMoveTheRegistryGeneration`; und ein gespeichertes Leserecht kann jederzeit überstimmt werden) und `testing-setup` (Ablenkung über die Grenzen: nicht freigegebener Ordner, abgelehnte Umsicht, blockierter Versand und **ein Aufrufer, der eine alte Dateiauswahl noch hält** — der Fall, der beim bloßen Lesen des Codes unsichtbar bleibt).

**Nächste freigegebene Aufgaben** (alle Abhängigkeiten erfüllt): 072, 073, 074, 075, 076, 078, 079, 080 (alle W16, `depends_on: 070, 071`), dazu 081, 082, 084 (Gate), 087, 092, 093, 095 (Gate), 098, 103, 105, 117 (Gate), 118 (Gate), 123 (Gate), 125 (Gate), 126 (Gate), 127 (Gate), 129 (Gate), 130 (Gate).

## Task 072 erledigt — „Agentenwerkzeuge verbinden“

`AgentToolLoop.kt` (neu, `feature/agent/`) + `AgentToolLoopTest.kt` (35 Tests).

**Reihenfolge je Aufruf:** Abbruchprüfung → Katalog → Argumente → Freigabe → Projektgrenze → Ausführung. Eine Stufe kann nur nach links abbrechen; ausgeführt wird erst, wenn alle fünf bestanden sind.

**Vier Zusagen strukturell abgesichert:**
- **Zugelassene Aktionen:** `AgentToolCatalog` mit sechs Werkzeugen (`read_file`, `list_directory`, `search_text`, `write_file`, `delete_file`, `run_test`). Ein unbekannter Name wird abgelehnt, **bevor** Argumente gelesen werden. Der Katalog enthält bewusst kein Netz-, Schlüssel-, Push-, Installations- oder Fremdfähigkeits-Werkzeug — diese Aktionen gehören eigenen Aufgaben. Name-Vergleich ist exakt: `run_tests` ist kein `run_test`.
- **Jede Aktion wird zweimal geprüft:** Freigabe aus dem Plan (`ExecutionPlan.grantedApprovals`, je Schritt *und* Stufe) und dann die Projektgrenze über `ProjectAccessRegistry`. Keine der beiden Prüfungen ersetzt die andere; getestet ist der Fall „Freigabe erteilt, Pfad führt trotzdem aus dem Projekt heraus“.
- **Fehler/Abbruch sind keine Erfolge:** `ToolExecutionResult` trennt Erfolg und Misserfolg im Typ, `ToolResult` verbietet im `init` einen Zustand „Fehler mit Ausgabe“ und „Erfolg ohne Ausgabe“, und `ToolLoopRun.isSuccess` verlangt zusätzlich, dass nichts übrig blieb und kein Stoppgrund vorliegt. Eine Exception aus dem Werkzeug wird abgefangen und zu `FAILED` — nie zu Erfolg.
- **Nebenwirkungen ehrlich verbucht:** Was ausgeführt wurde, steht in `sideEffects`, auch wenn der Lauf danach abbricht. Ein gelöschtes Datum verschwindet nicht, weil danach etwas schiefging.

**Ein Fund an der Schnittstelle zu Task 071, der echte Sicherheitsrelevanz hat.** `ExecutionPlan.isGranted(step)` prüft nur die **höchste** Freigabestufe eines Schritts. Ein Schritt, der `WRITE_FILE` **und** `RUN_COMMAND` enthält, braucht danach nur `COMMAND_RUN` — eine Freigabe für den Befehl hätte also implizit auch die Dateiänderung gedeckt. Für den Werkzeuglauf reicht das nicht, dort wird jedes Werkzeug einzeln geprüft. Ergänzt wurde `ExecutionPlan.isGranted(step, approval)`, das die Stufe ausdrücklich nennt; der Werkzeuglauf prüft damit die Stufe des Werkzeugs, nicht die des Schritts. Test: `theHighestStepApprovalDoesNotCoverTheToolOfACombinedStep`.

**Zwei weitere echte Befunde in meiner ersten Fassung:**
1. **Ein Befehl konnte durch den Aufgabenwert umgangen werden, weil die Groß-/Kleinschreibung nicht zusammenpasste.** Die Map war `unitTest` → Befehl, gesucht wurde mit `lowercase()`. `run_test` mit `task=unitTest` fand also nichts — harmlos in der Richtung, aber es hätte bedeutet, dass die erste Stufe des erlaubten Testlaufs immer abgelehnt wird. Jetzt löst `AgentToolCatalog.commandFor()` die Aufgabe kleinschreibungsgleich auf; der Befehl entsteht trotzdem nur aus der Liste, nie aus Modelltext.
2. **Die Zusammenfassung zählte falsch, wenn die Aufrufgrenze griff.** Bei `index >= maxCallsPerStep` wurde nur der *nächste* Aufruf in `notExecuted` aufgenommen und dann abgebrochen: der Bericht sagte „nicht ausgeführt (1)“, obwohl drei Aufrufe nie liefen. Jetzt werden `calls.drop(index)` aufgeführt — alle, die nicht gelaufen sind.

**Weiterhin bewusst nicht gebaut:** ein allgemeines `run_command`. Im Katalog gibt es nur `run_test` mit einem Aufgabenwert aus einer festen Liste. Die offene Frage „Befehle auf Android ausführen“ ist Aufgabe 105 und hängt an realen Gerätemessungen; ein freier Befehlsweg im Werkzeugkatalog würde diese Entscheidung vorwegnehmen.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **747 Tests, 0 Fehler, 0 übersprungen** (vorher 712, +35 aus `AgentToolLoopTest`). Drei der vier roten Tests waren falsche Erwartungen (ein `List<String>` gegen ein `String?` verglichen, eine falsche Ergebnisanzahl, ein umgestellter Vergleich); der vierte deckte den Zählfehler bei der Aufrufgrenze auf. Geheimnis-Scan über beide Dateien ohne Treffer.

**Geladene Skills:** `android-permissions-security` (jede Werkzeugaktion gegen Grenze **und** Freigabe, kein Pfad und kein Befehl aus Modelltext, `ProjectAccessRegistry` wird nur gelesen — `theRegistryIsNeverWidenedByTheLoop`) und `testing-setup` (Ablenkung über Grenzen: unbekanntes Werkzeug, unerwarteter Parameter, nicht erlaubte Aufgabe, Pfad außerhalb, Geheimnisdatei, Fehler in der Mitte, Abbruch, Aufrufgrenze — und ein Test, der die Ehrlichkeit *im Typ* festnagelt).

**Nächste freigegebene Aufgaben** (alle Abhängigkeiten erfüllt): 074, 075, 076, 078, 079, 080 (alle W16), dazu 081, 082, 084 (Gate), 087, 092, 093, 095 (Gate), 098, 103, 105, 117 (Gate), 118 (Gate), 123 (Gate), 125 (Gate), 126 (Gate), 127 (Gate), 129 (Gate), 130 (Gate).

## Task 073 erledigt — „Agentenlauf speichern“

`AgentRunState.kt` (neu, `feature/agent/`) + `AgentRunStateTest.kt` (29 Tests).

**Zustände wie im Auftrag:** geplant, wartet auf Zustimmung, läuft, beendet, abgebrochen, fehlerhaft — plus `UNCERTAIN_SIDE_EFFECT` für den Fall, dass nach einem Abbruch nicht feststeht, ob geschrieben wurde. Dieser siebte Zustand ist der ehrliche: Ohne ihn müsste ein ungeklärter Abbruch entweder als „beendet“ oder als „abgebrochen und wiederholbar“ erscheinen, und beides wäre falsch.

**„Neustart führt keine Aktion doppelt aus“ — an zwei Stellen abgesichert:**
- `mayExecuteCall(toolName, path)` beantwortet nur die Frage „darf das noch einmal?“ und kann nicht zum Ausführen benutzt werden. Nach `recordCall` ist derselbe Schlüssel gesperrt, auch wenn er mit anderer Groß-/Kleinschreibung geschrieben wird.
- `stepsThatMayRunAgain()` liefert ausschließlich `PLANNED`, `WAITING_FOR_APPROVAL` und `ABORTED` — also Zustände, in denen nachweislich nichts passiert ist. `RUNNING`, `UNCERTAIN_SIDE_EFFECT`, `FAILED` mit Schreibvorgang und `FINISHED` gehören nicht dazu.
- Ein zweiter `recordCall` mit demselben Schlüssel ändert den Zustand **nicht** (`AlreadyRecorded`) und vergrößert auch die Pfadliste nicht. Damit kann auch ein Fehler in der Aufrufschleife keinen zweiten Eintrag erzeugen.

**„Keine Zugangsdaten im Laufstatus“ — zwei Ebenen:**
- Der gespeicherte Zustand kennt keine Argumentwerte, nur Werkzeugnamen und Pfade. `ToolCall.arguments` hat im Protokoll keinen Platz.
- Jeder von außen kommende Text läuft über `SecretMasker.redact`, bevor er gespeichert wird. `addNote` meldet zusätzlich `RecordOutcome.Redacted`, wenn ein Schlüssel erkannt wurde — der Aufrufer kann also anzeigen, dass etwas *nicht* gespeichert wurde. `markFailed` benutzt denselben Weg, weil eine Fehlermeldung eines Werkzeugs genau dort einen Schlüssel enthalten kann.

**„Nutzer sieht, ob eine Änderung schon gespeichert wurde“:** `changedFileLines()` nennt jede berührte Datei mit ihrem `CommitState` (nicht gesichert / lokal gesichert / gesichert und hochgeladen / verworfen). `markFinished` mit geänderten Dateien erzwingt dabei `NOT_SAVED`, wenn kein Sicherungszustand übergeben wurde — ein Schritt, der geschrieben hat, kann nicht als „fertig und gesichert“ dastehen.

**Ein Fehler, den der Test aufgedeckt hat:** `unsavedChangeLines()` hieß „unsaved“, gab aber *alle* berührten Dateien mit ihrem Zustand zurück. Die Funktion hätte damit eine Frage beantwortet und gleichzeitig eine andere verschwiegen — was passiert mit den gesicherten Dateien? Aufgeteilt in `changedFileLines()` (alle, mit Zustand) und `unsavedChangeLines()` (nur die ungesicherten).

**Teststand:** `./gradlew :app:testDebugUnitTest` → **776 Tests, 0 Fehler, 0 übersprungen** (vorher 747, +29 aus `AgentRunStateTest`). Der Geheimnis-Scan findet in den beiden Dateien nur zwei synthetische Test-Fixtures — sie sind genau die Belegstelle dafür, dass die Schwärzung greift.

**Geladene Skills:** `testing-setup` (der entscheidende Test ist der mit dem ausgelösten Exception- und Fehlerpfad: `anUncertainWriteCountsAsNotSaved`, `aFailedStepWithWrittenChangesNeedsReview`) und `android-permissions-security` (Least Privilege auch für *Metadaten*: der Laufstatus ist eine Speicherstelle wie jeder andere und darf nicht zum Ort für Schlüssel werden).

**Nächste freigegebene Aufgaben** (alle Abhängigkeiten erfüllt): 075, 076, 078, 079, 080 (alle W16), dazu 081, 082, 084 (Gate), 087, 092, 093, 095 (Gate), 098, 103, 105, 117 (Gate), 118 (Gate), 123 (Gate), 125 (Gate), 126 (Gate), 127 (Gate), 129 (Gate), 130 (Gate).

## Task 074 erledigt — „Kontext verwalten“

`AgentContextManager.kt` (neu, `feature/agent/`) + `AgentContextManagerTest.kt` (27 Tests).

**Rechenregel aus der Anbieterquelle statt aus dem Gedächtnis.** Der zugewiesene Skill `/claude-api` ist in dieser Sitzung nicht verfügbar; als Ersatz wurde die Provider-Dokumentation direkt gelesen: `https://platform.claude.com/docs/en/build-with-claude/context-windows`, abgerufen am 2026-10-02. Belegt daraus und im Code als `CONTEXT_DOC_URL`/`CONTEXT_DOC_VERIFIED` festgehalten:
- Systemprompt, **alle** Nachrichten, Werkzeugdefinitionen, Bilder, Dokumente **und die erzeugte Antwort zählen in dasselbe Fenster.
- Die Genauigkeit nimmt mit der Tokenzahl ab („mehr Kontext ist nicht automatisch besser“) — deshalb wird hier gekürzt statt gefüllt.
- Für belegte Modelle bleibt Platz für die Antwort reserviert; die Fenstermitte kommt weiter aus `ContextSelectionPolicy.budgetFor` mit ihrer Quelle.

**Vier Zusagen strukturell abgesichert:**
- **Kürzung ist sichtbar.** Jeder Gesprächsteil ist entweder in `keptTurns` oder in `droppedTurns` mit deutschem Grund — es gibt keinen dritten Pfad, und `disclosureLines()` führt die ausgelassenen Teile unter der Überschrift „NICHT mehr berücksichtigt“. Ein Teil mit unbekannter Tokenzahl wird ausgelassen (`TOKEN_COUNT_UNKNOWN`), nicht geschätzt.
- **Keine vertrauliche Datei über die Kürzung hinein.** Die Dateiliste stammt ausschließlich aus `ContextSelectionPolicy.select`, wird danach mit `findExcludedFiles` noch einmal gegen `ProjectExclusionPolicy` geprüft und blockiert den ganzen Plan, falls dort doch eine Geheimnisdatei steht. Der Typ `ContextPlan` führt Pfade, keinen Dateiinhalt.
- **Vorschau vor jeder Anfrage.** Der Plan nennt Fenster, Belegung je Bestandteil, zugesagten Antwortplatz, berücksichtigte und ausgelassene Teile sowie die Dateiliste aus Aufgabe 068. `canSend` ist `false`, solange der Plan über dem Fenster liegt.
- **Festgehaltenes bleibt.** `isPinned` wird nie gekürzt; passt die festgehaltene Menge allein nicht, ist das `OVER_LIMIT` mit Hinweis statt stiller Kürzung.

**Zwei ehrliche Korrekturen nach den ersten roten Tests:**
1. **Ein einzelner Teil, der allein nicht ins Fenster passt, wurde als „beendet“ gemeldet.** Der Plan war technisch passend (kein Teil drin), enthielt aber den ganzen Gesprächsverlust. Die Meldung stimmt zwar — der ausgelassene Teil wird mit Grund genannt —, der Test behauptete aber zu Unrecht `OVER_LIMIT`. Der Test wurde an das reale Verhalten angepasst („gekürzt, mit Grund“), und der echte `OVER_LIMIT`-Fall ist separat getestet: eine Systemanweisung allein über dem Fenster lässt sich durch kein Kürzen mehr retten.
2. **`ContextUsage.fits` prüfte nur den Kontext, nicht den Platz für die Antwort.** Ein Plan, der das Fenster vollständig für den Kontext beanspruchte, wäre „passend“ gewesen und hätte die Antwort abgeschnitten. Jetzt gilt „passt“ als „bleibt auch Raum für die zugesagten Antwort“, mit sichtbarem Überhang.

**Ehrlich benannt:** Der zweite Geheimnisdurchgang (`findExcludedFiles`) ist über `plan()` heute nicht erreichbar, weil Aufgabe 068 dieselbe Regel schon anwendet. Er ist trotzdem implementiert und öffentlich — und mit einer von Hand gebauten Auswahl getestet, die eine `.env` enthält. Sollte sich die Regel in 068 einmal lockern, ist die Stelle benannt, statt dass die Datei unbemerkt durchrutscht.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **803 Tests, 0 Fehler, 0 übersprungen** (vorher 776, +27 aus `AgentContextManagerTest`). Die Kürzungsrechnung ist absichtlich mit einem **nicht belegten** Modell geprüft (32 000 Tokens, keine reservierte Antwort aus `ModelContextLimits.UNKNOWN_WINDOW_TOKENS`) — mit kleinen Zahlen, ohne eine Fenstergröße zu erfinden.

**Geladene Skills:** `android-permissions-security` (die Kürzung ist ein zweiter Weg in den Datenweg — sie bekommt dieselbe Geheimnisregel wie die Auswahl; zusätzlich wird ein Schlüssel im Gespräch vor dem Senden geschwärzt **und** als geschwärzt gemeldet) und als Ersatz für das nicht verfügbare `/claude-api` die direkte Quellenprüfung der Provider-Dokumentation.

## Task 075 erledigt — „Fortschritt anzeigen“

`AgentProgressPresenter.kt` (neu, `feature/agent/`) + `AgentProgressTest.kt` (27 Tests).

**Beide Abnahmekriterien strukturell abgesichert:**
- **„Fortschritt gibt nicht vor, eine unbekannte Aufgabe sei abgeschlossen“.** Der Zustand kennt `ProgressPhase.UNKNOWN`, und `isComplete` ist ausschliesslich bei `FINISHED` `true`. `AgentProgressPresenter.fromRun` erzeugt `UNKNOWN`, wenn zu einem genannten Schritt **kein** Eintrag im Laufstatus existiert — aus „kein Eintrag“ wird nicht „fertig“. Ohne bekannte Gesamtzahl wird kein Fortschrittsbalken gezeigt, sondern der Text „Fortschritt: noch nicht bekannt“.
- **„Wartezeiten mit Stop-/Abbruchmöglichkeit verbunden“.** `showStopAction` ist aus `phase.isWaiting` **abgeleitet**, nicht angegeben: Eine Wartephase ohne Stopp-Knopf ist nicht darstellbar. Das gilt auch für die Benachrichtigung — sie nennt die Wartezeit *und* den Abbruch. Der Knopf ist `AccessibilityPolicy.MinimumTouchTarget` (48 dp); die Konstante kommt aus dem Projekt, nicht aus einer zweiten Zahl im Code.

**Schutz: keine privaten Werkzeugausgaben in Benachrichtigungen.** `notificationText()` entsteht aus einer Positivliste — Phase, Schrittbezeichnung, Abbruchhinweis. Das Feld `lastToolOutput` existiert für den Bildschirm und wird von dieser Funktion nicht gelesen; es gibt damit keinen Weg, die Ausgabe in eine Benachrichtigung zu bringen. Zusätzlich läuft jeder Text durch `SecretMasker`. Getestet mit einer Ausgabe, die einen fremden App-Pfad und einen Schlüssel enthält.

**Anpassung ans Display:** `lines(isCompact = true)` liefert auf schmaler Breite Phase, Schritt, Fortschritt und Stopp-Hinweis, aber keine Werkzeugausgabe — die braucht Platz, den ein schmales Display nicht hat. Der Stopp-Hinweis bleibt in beiden Fällen stehen.

**Ein Fehler, den der Test aufgedeckt hat:** Bei einem Lauf ohne Schritte stand `totalSteps` auf `0` statt auf `-1`. `progressFraction()` lieferte zwar korrekt `null`, die Bedeutung der beiden Kennzahlen war aber uneinheitlich: `-1` heißt „unbekannt“, `0` heißt „null Schritte“. Beide stehen jetzt auf `-1`, damit „unbekannt“ nur eine einzige Darstellung hat.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **830 Tests, 0 Fehler, 0 übersprungen** (vorher 803, +27 aus `AgentProgressTest`).

**Geladene Skills:** `adaptive` (Kurzfassung bei kompakter Breite, Stopp-Knopf in der Mindestgröße des Projekts, Fortschrittsanzeige ohne erfundene Prozentwerte, wenn die Gesamtzahl nicht bekannt ist) und `testing-setup` (der Test `onlyTheFinishedPhaseCountsAsDone` läuft über *alle* Phasen statt über zwei ausgewählte, damit eine neu hinzugefügte Phase nicht ungeprüft durchrutscht).

**Push erfolgt:** Am 2026-10-02 mit Nutzerfreigabe `cb88e3b..2aafa36` nach `origin/main` (privat) gepusht. Danach Aufgaben 080 und 081 committet.

**Nächste freigegebene Aufgaben** (alle Abhängigkeiten erfüllt, ohne Gate): 098, 103, 105.
**Durch 082 neu freigegeben** (es hing an dieser Aufgabe): 083, 085, 086, 089, 090, 091, 094, 121.

## Task 082 erledigt — „Android-Ordner auswählen“

`FolderSelectionPolicy.kt` (neu, `feature/project/`) + `FolderSelectionPolicyTest.kt` (29 Tests).

**Die beiden Fertig-Kriterien strukturell abgesichert:**
- **Abbrechen erzeugt keine Berechtigung.** `SelectionOutcome.CANCELLED` wird in `fold` **ganz zuerst** behandelt und liefert `Cancelled` — und zwar auch dann, wenn der Aufrufer trotzdem einen Namen und einen Pfad mitgibt. Sonst könnte man durch Mitgeben eines Pfades aus einem Abbruch doch noch eine Berechtigung bauen; genau das ist als Test festgehalten. Ein Abbruch wird nicht als Fehler behandelt, weil er der Normalfall des Dialogs ist.
- **Nur der ausgewählte Bereich.** `covers()` prüft mit dem Trenner `"/"`: `/ab/main.kt` gehört **nicht** zu `/a`. Ohne diesen Trenner wäre das ein stiller Zugriff auf Nachbarordner mit ähnlichem Namen — der Test nennt genau diesen Fall.

**Schutz: keine umfassende Speicherberechtigung.** Der Typ hat **kein Feld** und **keine Methode**, die eine Berechtigung anfordert; beide Zusicherungen sind über Reflexion geprüft. Ergänzend steht im Manifest nur `INTERNET` und `ACCESS_NETWORK_STATE`.

**`fold` und `stateAfter` getrennt:** `fold` liefert nur das **Ergebnis**, `stateAfter` den neuen Stand. Wer den Stand braucht, muss ihn ausdrücklich schreiben — sonst könnte ein Aufrufer ein erfolgreiches Ergebnis sehen und einen veralteten Stand behalten. Bei Abbruch bleibt der Stand **unverändert** (statt stillschweigend zurückgesetzt).

**Erneute Auswahl nennt, was wegfällt:** `Replaced.droppedRoots` listet die verlorenen Wurzeln, und `stateAfter` bestätigt, dass der alte Ordner danach nichts mehr abdeckt.

**Zwei Testfehler, die ich behoben habe:** Der Test „keine Methode anfordert“ war zu grob — `getGrantedRoots` enthält „grant“, fragt aber nichts an; jetzt werden nur echte Aktionen geprüft. Und der Test suchte nach „keine allgemeine Zugriffsberechtigung“, während der Code „wird nicht verlangt“ sagt — beide Male war der Code richtig. Dazu vier Stellen, an denen `assertTrue(x is T)` keinen Smart Cast auslöst (JUnit-Methoden haben keinen Vertrag); dort steht jetzt ein expliziter Cast.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1071 Tests, 0 Fehler, 0 übersprungen** (vorher 1042, +29).

**Geladene Skills:** `android-permissions-security` (daraus die beiden Prüfungen über Reflexion: kein Berechtigungsfeld, keine anfordernde Methode — der Task verlangt „keine umfassende Speicherberechtigung“, und das wird am Typ geprüft, nicht in einem Kommentar behauptet) und `testing-setup` (der Abbruch-Fall steht mit Abspruch an erster Stelle, weil er der häufigste ist).

## Task 093 erledigt — „Projektanweisungen lesen“

`ProjectInstructionReader.kt` (neu, `feature/project/`) + `ProjectInstructionReaderTest.kt` (30 Tests).

**Schutz: Anweisungen aus fremdem Projekten sind Daten, keine App-Befehle.** Das ist die Kernaufgabe, und sie ist **strukturell** gelöst: `ProjectInstructionReader` gibt `ProjectInstruction` zurück — einen Wert aus Strings. Er hat **keine Methode**, die ausführt, anwendet, freigibt oder erzeugt, und **keinen Rückgabetyp**, der einen Werkzeugaufruf oder eine Freigabestufe trägt. Drei Tests prüfen Methodennamen, Rückgabetypen und Felder. Es gibt keinen Codepfad von „das steht in der Datei“ zu „die App tut es“.

**Eskalationsversuche werden gemeldet, nicht befolgt.** `IgnoreReason.ESCALATION_ATTEMPT` fängt die Formulierungen, die die App von ihren eigenen Regeln wegreden sollen — „ignore the previous instructions“, „without asking“, „bypass“, „skip the approval“. Die Zeile bleibt in `ignored` **mit Zeilennummer** stehen, damit der Nutzer sieht, dass sie abgelehnt und nicht stillschweigend verworfen wurde. Die Liste ist bewusst **eng** und nur auf Eskalation bezogen: ein Projekt darf „wir nutzen Tabs“ sagen, aber nicht „du musst den Nutzer nicht fragen“.

**Nicht unterstützte Dateien sind sichtbar ignoriert.** `README.md` ist **erkannt, aber nicht als Anweisung gelesen** — sie ist Dokumentation *für Menschen*, und sie steuern zu lassen hieße, jede Prosa im Repository zur Regel zu machen. Das steht als Eigenschaft am Typ (`isSupported`), nicht in einem Kommentar.

**Ein echter Fehler, den der Test aufgedeckt hat:** `InstructionFileKind.forPath` verglich nur den **Dateinamen**. Dadurch wurde `.github/copilot-instructions.md` **nie erkannt** — übrig blieb nur `copilot-instructions.md`. Jetzt wird zweimal verglichen: einmal gegen den vollen Pfad, einmal gegen den Dateinamen. Der erste Vergleich findet verschachtelte Dateien, der zweite findet dieselbe Datei an anderer Ebene. Beide Wege sind getestet.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1196 Tests, 0 Fehler, 0 übersprungen** (vorher 1166, +30).

**Skills:** `android-permissions-security` (daraus die Kernregel: Herkunft aus einem fremden Baum ist kein Vertrauensbeweis — dieselbe Logik wie bei Intent-Extras, die man nicht als Identität akzeptiert) und als Ersatz für `/code-review` das installierte `requesting-code-review`, das die drei Reflexionstests angestoßen hat.

## Task 092 erledigt — „Große Projekte“

`ProjectScalePolicy.kt` (neu, `feature/project/`) + `ProjectScalePolicyTest.kt` (30 Tests).

**Abschätzen zuerst, indizieren danach.** `assess()` liefert nur einen Bericht und startet nichts; die Entscheidung ist ein **eigener Aufruf** mit einem eigenen Typ (`ScaleApproval`). „Wir haben schon angefangen“ und „wir dürfen anfangen“ sind damit zwei verschiedene Aussagen. Zwei Tests prüfen per Reflexion, dass weder `ProjectScalePolicy` eine startende Methode noch `ScaleAssessment` ein startendes Feld hat.

**Speicherbedarf und Scanaufwand stehen vorab da** — inklusive des Satzes *„This is an estimate, not a measurement of your device“*. Ein großes Projekt indiziert **nicht von selbst alles**: `approve()` lässt bei `needsDecision` die Auswahl leer, der Aufrufer muss die Ordner benennen.

**Geheimnis-Ausschlüsse unabhängig von Größenregeln:** `secretPaths` wird **vor und getrennt von** der Größenschätzung gesammelt, und `ScaleApproval.indexablePaths` filtert beim Herausgeben **noch einmal**. Ein Test nimmt eine Zugangsdatei ausdrücklich in die Auswahl auf und belegt, dass sie trotzdem nicht zurückkommt.

**Auswahl später änderbar:** `withExcludedFolders` gibt eine **neue** Freigabe zurück; die alte bleibt unverändert. Ein Test prüft, dass die alte Freigabe nach dem Ändern noch zwei Ordner enthält — sonst würde eine laufende Indizierung unter den Füßen ihre Auswahl verlieren.

**Ein ehrlicher Entwurfsfehler, den der Test aufgedeckt hat:** Ich hatte den **Dateizahl** aus dem Durchlaufen mit einem Hochrechnungsfaktor multipliziert. Das war falsch: Die Zahl der Dateien ist beim Durchlaufen eine **Tatsache**, sie zu vervielfachen macht eine gezählte Zahl zu einer erfundenen, die dann als „Schätzung“ gemeldet wird. Hochgerechnet wird jetzt **nur der Bytebetrag**, ausdrücklich mit Namen; der Dateizahl ist der gezählte Wert. `BYTE_PROJECTION_SAFETY_FACTOR` ersetzt den alten, irreführend benannten `SAMPLE_EXTRAPOLATION_FACTOR`.

**Zwei Testbeispiele waren falsch gewählt** (vom Code korrekt abgewiesen): `.secrets/creds.json` ist keine gesperrte Datei (gesperrt ist u. a. `credentials.json`), und `.git` fällt unter die Regel für große Ordner, nicht unter die für versteckte. Beide Tests prüfen jetzt regelkonfliktfreie Fälle; zusätzlich gibt es je einen Test, der die Vorrangordnung festhält.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1166 Tests, 0 Fehler, 0 übersprungen** (vorher 1136, +30).

**Skills:** `android-profiler` (Speicher- und Zeitbedarf als **Schätzung mit sichtbarem_samplecount**, klar getrennt von einer Messung auf dem A56) und `testing-setup` (zwei Reflexionstests statt zwei Beispielen, damit ein späteres Hinzufügen einer startenden Methode auffällt).

## Task 089 erledigt — „Dateiänderungen vergleichen“

`ChangeReview.kt` (neu, `feature/agent/`) + `ChangeReviewTest.kt` (28 Tests).

**Schutz „Vergleichen ist nicht Speichern“ — durch Abwesenheit erfüllt:** `ChangeReview` hat **keine Methode**, die anwendet, schreibt, committet oder freigibt. Zwei Tests prüfen die Methodenliste und die Feldliste per Reflexion. Die stärkste Freigabe, die der Typ ausdrücken kann, ist eine *Beschreibung* dessen, was die Änderung bräuchte — er kann keine erteilen.

**`requiredApproval` wird berechnet, nicht übergeben.** Eine Datei, die entfernt wird, setzt `IRREVERSIBLE_DELETE` — auch wenn dieselbe Änderung sonst nur eine Gewöhnliche wäre. Eine als normale Bearbeitung verkleidete Löschung ist genau der Fehler, den das verhindert. `isCoveredBy` vergleicht ehrlich: `FILE_CHANGE` deckt keine Löschung.

**Neue und gelöschte Dateien getrennt:** `ChangeKind` unterscheidet `ADDED`/`DELETED`/`MODIFIED`/`RENAMED`, und `kind` ist ein Konstruktorargument — nicht aus Zeilenzahlen abgeleitet. Eine auf null umgeschriebene Datei ist eine Löschung, keine Bearbeitung, und beides darf nicht gleich aussehen.

**Abschnitte entstehen nur an Blockgrenzen**, nie mitten in einem Block. Jede Abschnittsüberschrift nennt Position **und** die Gesamtgröße der Datei, damit jemand, der Teil 3 von 9 liest, weiß, worum es geht. Ein einzelner zu großer Block wird als *abgeschnitten* gemeldet — ihn stillschweigend zu zeigen wäre eine falsche Aussage über den Rest.

**Ein Fehler, den der Test aufgedeckt hat:** Ohne gesetzte Abschnittsgrenze (`maxLinesPerSection = 0`) meldete **jede** Änderung `isTruncated = true`, weil gegen 0 verglichen wurde. Richtig ist: keine Grenze heißt keine angewandte Grenze, also nichts abgeschnitten. Behoben im Code, nicht im Test.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1136 Tests, 0 Fehler, 0 übersprungen** (vorher 1108, +28).

**Skills:** `requesting-code-review` (der Review dieses Diffs ist der Grund, warum die Freigabestufe berechnet statt übergeben wird — sie stand vorher als Parameter im Entwurf) und `adaptive` (Abschnitte mit Positions- und Größenangabe, damit eine lange Änderung auf einem schmalen Display lesbar bleibt).

## Task 087 erledigt — „Dateien finden“

`LocalFileSearch.kt` (neu, `feature/project/`) + `LocalFileSearchTest.kt` (37 Tests). Erster neuer Typ nach der Sprachumstellung, deshalb englisch.

**Die drei Zusagen strukturell abgesichert:**
- **Dateiinhalte werden bei lokaler Suche nicht übertragen.** `SearchMatch` trägt Pfad, Zeilennummer und Suchbegriff — **nicht** die Zeile. Ein Reading einer Zeile ist ein eigener Aufruf mit dem Rückgabetyp `LocalOnlyContent`, der in keinem Ergebnis und in keinem Übertragungsweg vorkommt. Zwei Tests prüfen die Feldliste **über Reflexion**, damit ein späteres „nur mal eine Vorschau dazu“ sofort auffällt. Ein Test mit einem Schlüssel in der gefundenen Zeile belegt, dass er in der Anzeige nicht auftaucht.
- **Die Suche endet sofort auf widerrufenen Bereichen.** Beim ersten widerrufenen Pfad bricht die Schleife **ab** (`break`), nicht nur `continue` — die restlichen Dateien können ebenfalls unerreichbar sein. Die Phase ist dann `STOPPED_REVOKED` und `isPartial` `true`, damit kein Aufrufer „Suche fertig“ mit „Suche vollständig“ verwechselt. `isRevoked` trennt am `/`, damit `privat2` nicht als `privat` gilt.
- **Versteckte Dateien und große Ordner nach festen Regeln.** `ScanRules` hält die Grenzen als benannte Konstanten (Projektvorgaben, keine Gerätemessung). Jeder ausgelassene Pfad erscheint mit **Grund**; die Reihenfolge ist bewusst so gewählt, dass der aussagekräftigere Grund gewinnt.

**Zwei Testfehler, die ich behoben habe:** Ich hatte `.env` als Beispiel für die Hidden-Regel und `.git/config` für den versteckten Ordner gewählt — beide fallen aber unter **strengere** Regeln (`BLOCKED_BY_POLICY` bzw. `HEAVY_DIRECTORY`), weil die Prüfung absichtlich den informativsten Grund zuerst nennt. Der Code war richtig; die Beispiele sind jetzt regelkonfliktfrei, und zwei zusätzliche Tests halten die Vorrangordnung fest.

**Ehrlich benannt:** `ProjectExclusionPolicy` hat **keine** versteckten Dateien in seiner Liste (dort geht es um Geheimnisse und schwere Ordner). Die Hidden-Regel ist deshalb neu in `ScanRules` definiert, statt eine vorhandene Liste zu überschreiben.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1108 Tests, 0 Fehler, 0 übersprungen** (vorher 1071, +37).

**Skills:** `android-profiler` (Scan-Grenzen als benannte Vorgaben, damit klar ist, dass es Projektentscheidungen und keine Messung auf dem A56 sind) und `testing-setup` (Feldlisten per Reflexion statt per Beispiel, damit eine spätere Erweiterung auffällt).

**Zusätzlich installiert (alle MIT/Apache-2.0, nur Markdown, „Safe / 0 alerts“):** `modularization`, `android-source-search`, `android-testing`, `compose`, `gradle-build-performance` aus `rcosteira79/android-skills` (MIT, 149 Sterne, aktiv). **Ehrlich:** `android-source-search` habe ich wegen seines Namens installiert, er ist aber für **AOSP-/AndroidX-Quellcode**, nicht für die projekteigene Suche — für Aufgabe 087 also **nicht** brauchbar. Die anderen vier passen zu den anstehenden Aufgaben.

## Task 081 erledigt — „Projektübersicht“
**Gates mit offener Entscheidung** (Nutzerentscheidung nötig): 084, 095, 117, 118, 123, 125, 126, 127, 129, 130.

## Task 093 erledigt — „Projektanweisungen lesen“

`ProjectInstructionReader.kt` (neu, `feature/project/`) + `ProjectInstructionReaderTest.kt` (30 Tests).

**Schutz: Anweisungen aus fremdem Projekten sind Daten, keine App-Befehle.** Das ist die Kernaufgabe, und sie ist **strukturell** gelöst: `ProjectInstructionReader` gibt `ProjectInstruction` zurück — einen Wert aus Strings. Er hat **keine Methode**, die ausführt, anwendet, freigibt oder erzeugt, und **keinen Rückgabetyp**, der einen Werkzeugaufruf oder eine Freigabestufe trägt. Drei Tests prüfen Methodennamen, Rückgabetypen und Felder. Es gibt keinen Codepfad von „das steht in der Datei“ zu „die App tut es“.

**Eskalationsversuche werden gemeldet, nicht befolgt.** `IgnoreReason.ESCALATION_ATTEMPT` fängt die Formulierungen, die die App von ihren eigenen Regeln wegreden sollen — „ignore the previous instructions“, „without asking“, „bypass“, „skip the approval“. Die Zeile bleibt in `ignored` **mit Zeilennummer** stehen, damit der Nutzer sieht, dass sie abgelehnt und nicht stillschweigend verworfen wurde. Die Liste ist bewusst **eng** und nur auf Eskalation bezogen: ein Projekt darf „wir nutzen Tabs“ sagen, aber nicht „du musst den Nutzer nicht fragen“.

**Nicht unterstützte Dateien sind sichtbar ignoriert.** `README.md` ist **erkannt, aber nicht als Anweisung gelesen** — sie ist Dokumentation *für Menschen*, und sie steuern zu lassen hieße, jede Prosa im Repository zur Regel zu machen. Das steht als Eigenschaft am Typ (`isSupported`), nicht in einem Kommentar.

**Ein echter Fehler, den der Test aufgedeckt hat:** `InstructionFileKind.forPath` verglich nur den **Dateinamen**. Dadurch wurde `.github/copilot-instructions.md` **nie erkannt** — übrig blieb nur `copilot-instructions.md`. Jetzt wird zweimal verglichen: einmal gegen den vollen Pfad, einmal gegen den Dateinamen. Der erste Vergleich findet verschachtelte Dateien, der zweite findet dieselbe Datei an anderer Ebene. Beide Wege sind getestet.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1196 Tests, 0 Fehler, 0 übersprungen** (vorher 1166, +30).

**Skills:** `android-permissions-security` (daraus die Kernregel: Herkunft aus einem fremden Baum ist kein Vertrauensbeweis — dieselbe Logik wie bei Intent-Extras, die man nicht als Identität akzeptiert) und als Ersatz für `/code-review` das installierte `requesting-code-review`, das die drei Reflexionstests angestoßen hat.

## Task 092 erledigt — „Große Projekte“

`ProjectScalePolicy.kt` (neu, `feature/project/`) + `ProjectScalePolicyTest.kt` (30 Tests).

**Abschätzen zuerst, indizieren danach.** `assess()` liefert nur einen Bericht und startet nichts; die Entscheidung ist ein **eigener Aufruf** mit einem eigenen Typ (`ScaleApproval`). „Wir haben schon angefangen“ und „wir dürfen anfangen“ sind damit zwei verschiedene Aussagen. Zwei Tests prüfen per Reflexion, dass weder `ProjectScalePolicy` eine startende Methode noch `ScaleAssessment` ein startendes Feld hat.

**Speicherbedarf und Scanaufwand stehen vorab da** — inklusive des Satzes *„This is an estimate, not a measurement of your device“*. Ein großes Projekt indiziert **nicht von selbst alles**: `approve()` lässt bei `needsDecision` die Auswahl leer, der Aufrufer muss die Ordner benennen.

**Geheimnis-Ausschlüsse unabhängig von Größenregeln:** `secretPaths` wird **vor und getrennt von** der Größenschätzung gesammelt, und `ScaleApproval.indexablePaths` filtert beim Herausgeben **noch einmal**. Ein Test nimmt eine Zugangsdatei ausdrücklich in die Auswahl auf und belegt, dass sie trotzdem nicht zurückkommt.

**Auswahl später änderbar:** `withExcludedFolders` gibt eine **neue** Freigabe zurück; die alte bleibt unverändert. Ein Test prüft, dass die alte Freigabe nach dem Ändern noch zwei Ordner enthält — sonst würde eine laufende Indizierung unter den Füßen ihre Auswahl verlieren.

**Ein ehrlicher Entwurfsfehler, den der Test aufgedeckt hat:** Ich hatte den **Dateizahl** aus dem Durchlaufen mit einem Hochrechnungsfaktor multipliziert. Das war falsch: Die Zahl der Dateien ist beim Durchlaufen eine **Tatsache**, sie zu vervielfachen macht eine gezählte Zahl zu einer erfundenen, die dann als „Schätzung“ gemeldet wird. Hochgerechnet wird jetzt **nur der Bytebetrag**, ausdrücklich mit Namen; der Dateizahl ist der gezählte Wert. `BYTE_PROJECTION_SAFETY_FACTOR` ersetzt den alten, irreführend benannten `SAMPLE_EXTRAPOLATION_FACTOR`.

**Zwei Testbeispiele waren falsch gewählt** (vom Code korrekt abgewiesen): `.secrets/creds.json` ist keine gesperrte Datei (gesperrt ist u. a. `credentials.json`), und `.git` fällt unter die Regel für große Ordner, nicht unter die für versteckte. Beide Tests prüfen jetzt regelkonfliktfreie Fälle; zusätzlich gibt es je einen Test, der die Vorrangordnung festhält.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1166 Tests, 0 Fehler, 0 übersprungen** (vorher 1136, +30).

**Skills:** `android-profiler` (Speicher- und Zeitbedarf als **Schätzung mit sichtbarem_samplecount**, klar getrennt von einer Messung auf dem A56) und `testing-setup` (zwei Reflexionstests statt zwei Beispielen, damit ein späteres Hinzufügen einer startenden Methode auffällt).

## Task 089 erledigt — „Dateiänderungen vergleichen“

`ChangeReview.kt` (neu, `feature/agent/`) + `ChangeReviewTest.kt` (28 Tests).

**Schutz „Vergleichen ist nicht Speichern“ — durch Abwesenheit erfüllt:** `ChangeReview` hat **keine Methode**, die anwendet, schreibt, committet oder freigibt. Zwei Tests prüfen die Methodenliste und die Feldliste per Reflexion. Die stärkste Freigabe, die der Typ ausdrücken kann, ist eine *Beschreibung* dessen, was die Änderung bräuchte — er kann keine erteilen.

**`requiredApproval` wird berechnet, nicht übergeben.** Eine Datei, die entfernt wird, setzt `IRREVERSIBLE_DELETE` — auch wenn dieselbe Änderung sonst nur eine Gewöhnliche wäre. Eine als normale Bearbeitung verkleidete Löschung ist genau der Fehler, den das verhindert. `isCoveredBy` vergleicht ehrlich: `FILE_CHANGE` deckt keine Löschung.

**Neue und gelöschte Dateien getrennt:** `ChangeKind` unterscheidet `ADDED`/`DELETED`/`MODIFIED`/`RENAMED`, und `kind` ist ein Konstruktorargument — nicht aus Zeilenzahlen abgeleitet. Eine auf null umgeschriebene Datei ist eine Löschung, keine Bearbeitung, und beides darf nicht gleich aussehen.

**Abschnitte entstehen nur an Blockgrenzen**, nie mitten in einem Block. Jede Abschnittsüberschrift nennt Position **und** die Gesamtgröße der Datei, damit jemand, der Teil 3 von 9 liest, weiß, worum es geht. Ein einzelner zu großer Block wird als *abgeschnitten* gemeldet — ihn stillschweigend zu zeigen wäre eine falsche Aussage über den Rest.

**Ein Fehler, den der Test aufgedeckt hat:** Ohne gesetzte Abschnittsgrenze (`maxLinesPerSection = 0`) meldete **jede** Änderung `isTruncated = true`, weil gegen 0 verglichen wurde. Richtig ist: keine Grenze heißt keine angewandte Grenze, also nichts abgeschnitten. Behoben im Code, nicht im Test.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1136 Tests, 0 Fehler, 0 übersprungen** (vorher 1108, +28).

**Skills:** `requesting-code-review` (der Review dieses Diffs ist der Grund, warum die Freigabestufe berechnet statt übergeben wird — sie stand vorher als Parameter im Entwurf) und `adaptive` (Abschnitte mit Positions- und Größenangabe, damit eine lange Änderung auf einem schmalen Display lesbar bleibt).

## Task 087 erledigt — „Dateien finden“

`LocalFileSearch.kt` (neu, `feature/project/`) + `LocalFileSearchTest.kt` (37 Tests). Erster neuer Typ nach der Sprachumstellung, deshalb englisch.

**Die drei Zusagen strukturell abgesichert:**
- **Dateiinhalte werden bei lokaler Suche nicht übertragen.** `SearchMatch` trägt Pfad, Zeilennummer und Suchbegriff — **nicht** die Zeile. Ein Reading einer Zeile ist ein eigener Aufruf mit dem Rückgabetyp `LocalOnlyContent`, der in keinem Ergebnis und in keinem Übertragungsweg vorkommt. Zwei Tests prüfen die Feldliste **über Reflexion**, damit ein späteres „nur mal eine Vorschau dazu“ sofort auffällt. Ein Test mit einem Schlüssel in der gefundenen Zeile belegt, dass er in der Anzeige nicht auftaucht.
- **Die Suche endet sofort auf widerrufenen Bereichen.** Beim ersten widerrufenen Pfad bricht die Schleife **ab** (`break`), nicht nur `continue` — die restlichen Dateien können ebenfalls unerreichbar sein. Die Phase ist dann `STOPPED_REVOKED` und `isPartial` `true`, damit kein Aufrufer „Suche fertig“ mit „Suche vollständig“ verwechselt. `isRevoked` trennt am `/`, damit `privat2` nicht als `privat` gilt.
- **Versteckte Dateien und große Ordner nach festen Regeln.** `ScanRules` hält die Grenzen als benannte Konstanten (Projektvorgaben, keine Gerätemessung). Jeder ausgelassene Pfad erscheint mit **Grund**; die Reihenfolge ist bewusst so gewählt, dass der aussagekräftigere Grund gewinnt.

**Zwei Testfehler, die ich behoben habe:** Ich hatte `.env` als Beispiel für die Hidden-Regel und `.git/config` für den versteckten Ordner gewählt — beide fallen aber unter **strengere** Regeln (`BLOCKED_BY_POLICY` bzw. `HEAVY_DIRECTORY`), weil die Prüfung absichtlich den informativsten Grund zuerst nennt. Der Code war richtig; die Beispiele sind jetzt regelkonfliktfrei, und zwei zusätzliche Tests halten die Vorrangordnung fest.

**Ehrlich benannt:** `ProjectExclusionPolicy` hat **keine** versteckten Dateien in seiner Liste (dort geht es um Geheimnisse und schwere Ordner). Die Hidden-Regel ist deshalb neu in `ScanRules` definiert, statt eine vorhandene Liste zu überschreiben.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1108 Tests, 0 Fehler, 0 übersprungen** (vorher 1071, +37).

**Skills:** `android-profiler` (Scan-Grenzen als benannte Vorgaben, damit klar ist, dass es Projektentscheidungen und keine Messung auf dem A56 sind) und `testing-setup` (Feldlisten per Reflexion statt per Beispiel, damit eine spätere Erweiterung auffällt).

**Zusätzlich installiert (alle MIT/Apache-2.0, nur Markdown, „Safe / 0 alerts“):** `modularization`, `android-source-search`, `android-testing`, `compose`, `gradle-build-performance` aus `rcosteira79/android-skills` (MIT, 149 Sterne, aktiv). **Ehrlich:** `android-source-search` habe ich wegen seines Namens installiert, er ist aber für **AOSP-/AndroidX-Quellcode**, nicht für die projekteigene Suche — für Aufgabe 087 also **nicht** brauchbar. Die anderen vier passen zu den anstehenden Aufgaben.

## Task 081 erledigt — „Projektübersicht“

`ProjectOverviewPolicy.kt` (neu, `feature/project/`) + `ProjectOverviewPolicyTest.kt` (40 Tests).

**Die beiden Fertig-Kriterien strukturell abgesichert:**
- **Widerrufene Ordnerrechte sind deutlich markiert.** Der Zustand kommt aus `ProjectAccessState.of(...)` und wird **berechnet, nicht übergeben**. Die Zurücknahme schlägt die Registrierung — sonst bliebe ein zurückgezogener Ordner „freigegeben“, nur weil er noch eingetragen ist. Die Warnung steht in `headline()` an Position 1 (mit `!`), also vor der Dateiliste, wo sie nicht überlesen wird. `canWork` verweigert für `REVOKED` jede Zusage.
- **Keine Dateien ohne Auswahl automatisch scannen.** `OverviewFileList` lässt sich mit `SelectionOrigin.AUTOMATIC_SCAN` **gar nicht** erzeugen — auch nicht leer. Der Wert existiert, damit die Ablehnung *benannt* werden kann. Ergänzend: Das Manifest kennt **nur** `INTERNET` und `ACCESS_NETWORK_STATE`, **keine** Speicherberechtigung — die Übersicht kann also nichts selbst suchen.

**Schutz: keine Geheimnisse, keine Dateiinhalte.** `OverviewFile` hat **kein Inhaltsfeld** (nur Pfad, Name, Größe) und **kein** `isSecret`-Argument.

**Ein echter Fehler, den der Test aufgedeckt hat:** `isSecret` war zunächst ein Konstruktorargument. Der Test „eine Zugangsdatei wird nicht mit Namen gezeigt“ schlug fehl, weil mein Testfall `.env` **ohne** `isSecret = true` übergab — die Übersicht zeigte den Namen. Das war kein Testfehler, sondern eine echte Lücke: ein Aufrufer, der das Flag vergisst, hätte den Namen einer `.env` angezeigt. `isSecret` ist jetzt eine **abgeleitete** Eigenschaft über `ProjectExclusionPolicy.classify` — es gibt kein Feld, das man auf `false` setzen könnte. Zwei Tests halten diesen Zustand fest (kein `isSecret`-Feld auf der Klasse) und belegen, dass auch `.pem`, `id_rsa` und die Kopie `.env.bak` erkannt werden, während `docs/secrets-guide.md` **nicht** aussortiert wird.

**Zwei Testfehler, die ich behoben habe:** Der Widerruf-Test suchte nach „zurück freigegeben“, der Code sagt „wieder freigegeben werden soll“ — der Code war richtig. Und der Test „nichts zu tun“ baute eine Übersicht, in der es eben doch etwas zu tun gab (kein Ordner, kein Lauf) — der Code war richtig; der Test prüft jetzt den Fall, in dem tatsächlich nichts offen ist.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1042 Tests, 0 Fehler, 0 übersprungen** (vorher 1001, +41).

**Geladene Skills:** `adaptive` (die Warnung muss ohne Scrollen sichtbar sein — deshalb steht sie in `headline()` an erster Stelle und nicht versteckt in der Dateiliste) und `testing-setup` (der Test prüft die Abwesenheit des `isSecret`-Feldes über Reflexion statt über ein Beispiel, das die Lücke nicht zeigen würde).

## Task 080 erledigt — „Anbieterfunktionen abgleichen“

## Task 080 erledigt — „Anbieterfunktionen abgleichen“

`ProviderCapabilityPolicy.kt` (neu, `feature/agent/`) + `ProviderCapabilityPolicyTest.kt` (32 Tests).

**Belegte Grundlage** (direkt abgerufen am 2026-10-02, `https://platform.claude.com/docs/en/models/overview`, als Konstante `ProviderCapabilityEvidence.CAPABILITY_DOC_URL` + `CAPABILITY_DOC_VERIFIED` im Code statt als Zahl im Kommentar):
- *„All current models support text and image input, text output, multilingual capabilities, vision, and tool use.“*
- Dieselbe Seite listet in ihrer Fähigkeitstabelle eine Zelle **„Not supported“** — Fähigkeiten sind also nicht über alle Modelle gleich.
- *„You can query model capabilities and token limits programmatically with the Models API. The response includes max_input_tokens, max_tokens, and a capabilities object for every available model.“*

**Daraus folgt die Arbeitsregel der Aufgabe: Eine Fähigkeit gilt erst als vorhanden, wenn sie belegt ist.** `ProviderCapabilityProfile.isVerified` ist Voraussetzung für jede positive Aussage. Ohne Nachweis liefert `check` das eigene Ergebnis `Unverified` — **nicht** „geht schon“. Ohne Nachweis wird auch **kein** Ersatzweg vorgeschlagen: Solange unklar ist, *ob* die Fähigkeit fehlt, wäre jede Ersatzliste eine Behauptung.

**Die drei Zusagen strukturell:**
- **Kein stiller Anbieterwechsel.** Die Klasse enthält **keine** Methode, die Anbieter oder Modell wechselt, und **keine** `CapabilityDecision`-Variante trägt ein Ziellager. Fehlende Fähigkeit → Meldung. Ein Wechsel bleibt allein der Weg über `ModelFallbackPolicy` (Aufgabe 061) mit eigener Zustimmung; die Meldung sagt das dem Nutzer ausdrücklich.
- **Der Ersatzweg berücksichtigt neue Freigaben und Kosten.** `Workaround` hat `requiresFreshApproval` und `costNoticeLines` als Pflichtfelder. Der `init`-Block **lehnt einen kostenpflichtigen Ersatzweg ohne Kostenhinweis ab** — damit kann es keinen Ersatzweg geben, der Geld kostet, ohne dass der Nutzer es vorher gesehen hat.
- **Fehlende Rechte werden nicht durch eine andere Anmeldung umgangen.** Im ganzen Typ gibt es **kein Feld** für eine andere Anmeldung, ein anderes Konto oder einen anderen Zugang. `Unsupported` kann das nicht ausdrücken, also kann es nicht vorkommen. Getestet auch über den Kurzbericht, der „Es wird kein anderer Anbieter und keine andere Anmeldung verwendet“ ausdrücklich sagt.

**Kein Präfixabgleich bei Werkzeugnamen** (bewusst getestet): `read_file_backup` gilt **nicht** als `read_file`. Groß-/Kleinschreibung und umgebende Leerzeichen sind dagegen egal.

**Ein Fehler, den der Test aufgedeckt hat:** Der Test „Schreibweise spielt keine Rolle“ schlug zunächst fehl — mein Testfall hatte `search_text` vergessen, obwohl `FILE_READING` es braucht. Der Code war richtig, der Test falsch; behoben am Test, nicht an der Logik.

**Ein Namenskonflikt, der beim Kompilieren auffiel:** Belegkonstanten und Prüfklasse hießen beide `ProviderCapabilityPolicy`. Die Konstante heisst jetzt `ProviderCapabilityEvidence` — das benennt genauer, was sie ist (Beleg, nicht Prüfung).

**Teststand:** `./gradlew :app:testDebugUnitTest` → **1001 Tests, 0 Fehler, 0 übersprungen** (vorher 969, +32 aus `ProviderCapabilityPolicyTest`). Die 1000er-Marke ist erreicht.

**Geladene Skills:** als Ersatz für das nicht verfügbare `/claude-api` die **direkte Quellenprüfung** der Anbieterdokumentation (nicht geraten: die zitierten Sätze wurden von der Quelle geholt und das Abrufdatum steht im Code), und `testing-setup` (jede der fünf `AgentFeature` wird einzeln mit vollem und leerem Profil geprüft, damit eine neue Funktion nicht ungeprüft mitläuft).

## Task 079 erledigt — „Wiederholungsregeln“

`AgentRetryPolicy.kt` (neu, `feature/agent/`) + `AgentRetryPolicyTest.kt` (42 Tests).

**Die beiden Fertig-Kriterien strukturell abgesichert:**
- **Schreib-, Installations- und Git-Aktionen werden nie automatisch wiederholt.** `RetryAction.neverAutoRetry` hängt am Typ: `WRITE_FILE`, `DELETE_FILE`, `INSTALL`, `GIT_COMMIT` und `GIT_PUSH` sind darauf gesetzt, und **kein Parameter von `decide` hebt es auf** — weder eine Idempotenz-Bescheinigung noch eine Nutzerbestätigung. Diese Aktionen enden immer in `AskUser`. Getestet ausdrücklich mit beiden Gegenbelegen.
- **Kostenpflichtige Anfragen sind vor der Wiederholung sichtbar.** Für `MODEL_REQUEST` (und `INSTALL`) verlangt `decide` das Flag `costNoticeShown`. Ohne dieses Flag gibt es **kein** `RetryNow`, sondern `AskUser` mit dem Grund „Kosten nicht sichtbar gemacht“. Ein Hinweistext im Nachhinein genügt nicht — er muss vorher gezeigt worden sein.

**Schutz gegen Doppelausführung:** `decideWithStore` fragt `AgentRunStore.mayExecuteCall` aus Aufgabe 073 ab. Damit hängt die Wiederholungsregel an **derselben Quelle**, die auch den Neustart schützt: Was dort verbucht ist, wird nicht erneut versucht (`ALREADY_EXECUTED`), und dieser Grund schlägt alle anderen — auch eine Seitenwirkung ohne Sperrvermerk.

**Ein Fehler, den der Test aufgedeckt hat:** Der Test, der jede Aktion einer von drei Gruppen zuordnet, schlug zunächst fehl: `MODEL_REQUEST` gehörte in **keine** Gruppe. Die Liste behauptete eine Vollständigkeit, die nicht stimmte. Die dritte Gruppe (kostenpflichtig, ohne Nebenwirkung, nicht gesperrt) ist jetzt ausdrücklich aufgenommen, sodass jede neue Aktion in [RetryAction] sofort benannt wird, wenn sie nicht in eine der drei passt.

**Offen benannt — toter Zweig:** Der Zweig `NEEDS_CONFIRMATION` („Nebenwirkung ohne Idempotenznachweis“) ist mit dem heutigen Aktionssatz **nicht erreichbar**: Jede Aktion mit `hasSideEffect` trägt auch die strengere automatische Sperre, die vorher greift. Der Zweig bleibt als zweite Linie für künftige Aktionen stehen und ist im Kommentar als solcher benannt; der zugehörige Test sagt ausdrücklich, dass er die Erreichbarkeit **nicht** vortäuselt, sondern die Überlagerung dokumentiert.

**Zwei weitere Entscheidungen, die als Tests festgehalten sind:** (1) Ein **ungeklärter** Fehler wird nicht wie ein vorübergehender behandelt — er fragt nach Nachsehen statt zu wiederholen, weil nach einem Abbruch mit ungeklärter Nebenwirkung Wiederholen die riskantere Annahme ist. (2) Ist die Versuchsgrenze **wirklich** erreicht, schlägt sie auch die automatische Sperre (`ATTEMPTS_EXHAUSTED` statt `NEVER_AUTOMATIC`) — es gibt keinen vierten Versuch, den man anbieten könnte.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **969 Tests, 0 Fehler, 0 übersprungen** (vorher 927, +42 aus `AgentRetryPolicyTest`).

**Geladene Skills:** `testing-setup` (die Gruppenzuordnung prüft jede Aktion einzeln statt zwei Stichproben, damit eine neue Aktion nicht ungeprüft durchrutscht) und `android-permissions-security` (daraus die zweite Linie: Nebenwirkungen werden nie stillschweigend wiederholt, und jeder Eingriff braucht eine sichtbare Zustimmung — dasselbe Prinzip wie bei Berechtigungen).

## Task 078 erledigt — „Parallelität begrenzen“

`AgentConcurrencyLimiter.kt` (neu, `feature/agent/`) + `AgentConcurrencyLimiterTest.kt` (47 Tests).

**Die beiden Fertig-Kriterien strukturell abgesichert:**
- **Gleichzeitige Dateiänderungen überschreiben sich nicht.** Jede angemeldete Einheit hält ihre Pfade belegt, und ein kollidierender Zugriff wird mit `Admission.Denied` **plus Nennung des konfligierenden Pfades und der belegenden Einheit** abgewiesen. Die Sperre entsteht durch [FileAccess] (`coexistsWith`), nicht durch eine Konvention: zwei **Leser** desselben Pfads dürfen parallel laufen, ein Leser gegen einen Schreiber nicht. `release` gibt die Pfade wieder frei.
- **Knappes RAM/Akku verlangsamt oder warnt.** `ResourceAdvisor` kennt vier Lagen: `FULL_SPEED`, `THROTTLED`, `PAUSE_SUGGESTED` und `UNKNOWN`. `effectiveLimit` ist `minOf(configuredLimit, …)` — die Lage kann das Limit **nur senken, nie erhöhen**. Am Kabel bremst ein niedriger Akkustand nicht.

**Schutz gegen die Hintergrundmodellladung:** `requestBackgroundModelLoad` liefert `BLOCKED`, solange `allowBackgroundModelLoad` nicht ausdrücklich gesetzt wurde — auch bei 6 GB freiem Speicher. Es gibt **keinen Parameter, der diese Sperre aufhebt**; selbst mit Zustimmung wird blockiert, wenn der Bedarf über dem freien Speicher liegt.

**Ehrlich bei fehlender Messung:** `ResourceSample.isMeasured` ist im Konstruktor sichtbar. Ohne Messung bleibt der Rat `UNKNOWN`, `freeMemoryFraction` ist `null`, und das Limit wird **weder gesenkt noch erhöht** — ein geratener RAM-Wert wäre hier schlimmer als keiner, weil er als Messung auftrate. Die Schwellen (`MEMORY_THROTTLE_FRACTION = 0.15` usw.) sind als benannte Projektvorgaben im Code, nicht als scheinbare Gerätemesswerte; **es liegen keine A56-Messungen vor und es wird auch keine behauptet**.

**Ein Fund beim Review des eigenen Diffs:** Der Kommentar an `tryAcquire` behauptete, der Dateikonflikt werde „erst nach dem Limit geprüft, damit die Meldung den Pfad nennen kann“ — das war falsch: Bei vollem Limit hätte die Limit-Prüfung abgebrochen und der Pfad wäre **nie** genannt worden, also genau das Gegenteil der Begründung. Die Reihenfolge ist jetzt Pause → Ressourcenlage → **Dateikonflikt** → Limit, mit der Begründung im Kommentar. Zwei Tests pinnen das fest: bei vollem Limit **mit** Konflikt kommt der Pfad, **ohne** Konflikt greift weiterhin das Limit.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **927 Tests, 0 Fehler, 0 übersprungen** (vorher 880, +47 aus `AgentConcurrencyLimiterTest`).

**Geladene Skills:** `android-profiler` (der Kern des Auftrags ist Ressourcenmessung — daraus die Pflicht, *gemessen* und *angenommen* zu trennen, statt eine Zahl zu behaupten) und `parallel-task` (daraus die Regel „ein Agent pro Dateibereich, keine gemeinsamen Dateien“ — als Code umgesetzt: die Pfadsperre in `tryAcquire`).

## Task 076 erledigt — „Ergebnis zusammenfassen“

`AgentResultSummary.kt` (neu, `feature/agent/`) + `AgentResultSummaryTest.kt` (50 Tests).

**Die beiden Fertig-Kriterien strukturell abgesichert:**
- **Vorschlag, gespeicherte Änderung und Git-Upload bleiben unterscheidbar.** `ChangeLevel` kennt sechs Stufen von `PROPOSED` bis `PUSHED`, und `AgentResultSummary.changeLevel` nennt für den Lauf die **schwächste** — nicht die stärkste. Vier hochgeladene und eine nur lokal gespeicherte Datei ergeben damit „lokal gespeichert, nicht hochgeladen“. `hasReached` ist die einzige Stelle, die eine Stufe bejaht, und sie ist für *jede* Änderung wahr, nicht für die beste. `changeSentence()` benennt die Stufe im Klartext, damit die Oberfläche nichts anderes daraus machen kann.
- **Fehler und unbestätigte Punkte bleiben sichtbar.** `RunVerdict` ist **kein** übergebener Wert, sondern wird aus Tests, Fehlern, Abbruchgrund, unbestätigten Punkten und erreichter Stufe berechnet. Ein ungeklärter Nebeneffekt kann damit strukturell nicht als „abgeschlossen“ erscheinen. `reportLines()` stellt Fehler und offene Punkte **vor** die nächsten Möglichkeiten, damit sie beim Scrollen nicht aus dem Blick geraten.

**Teststatus korrekt ausweisen:** `TestOutcome` hat neben `PASSED`/`FAILED` den ehrlichen Sonderfall `RAN_UNVERIFIED` — ein gelaufener Befehl, dessen Ausgabe niemand gelesen hat, ist kein bestandener Test. `ReportedTest` erzwingt im `init`, dass `PASSED` und `FAILED` Zahlen tragen müssen und die beiden Zustände ohne Auswertung **keine** tragen dürfen; es gibt keinen Weg, „bestanden“ ohne Testergebnis zu melden. `fromRun` **rät die Ausgabe nicht**, sondern trägt einen fehlenden Nachweis selbst als `RAN_UNVERIFIED` ein.

**Schutz:** Jeder Text läuft über `SecretMasker`; der `init`-Block weist zusätzlich jeden ungeprüften Text im fertigen Bericht ab (`build` ist der vorgesehene Weg). Getestet mit Schlüssel in Fehlermeldung, Dateiname und Befehl.

**Zwei echte Fehler, die die Tests aufgedeckt haben:**
1. **Eine ungeklärte Nebenwirkung ohne bekannte Datei wurde als `SAVED` gemeldet.** Der Schritt endete in `UNCERTAIN_SIDE_EFFECT`, hatte aber keine `touchedPaths` — also kam nichts in die Änderungsliste, und der Bericht stand auf „lokal gespeichert“, obwohl möglicherweise geschrieben wurde. Behoben mit dem abgeleiteten Feld `unresolvedSideEffect`, das in `fromRun` aus dem Laufstatus **berechnet** und nicht übergeben wird: Der Aufrufer kann eine ungeklärte Nebenwirkung nicht stillschweigend als geklärt deklarieren.
2. **Ein gelaufener Test ohne Auswertung verschwand.** `fromRun` mit `tests = emptyList()` und `expectedTestCount = 0` hätte einen Lauf **mit** Test wie einen Lauf ganz ohne Tests aussehen lassen — und als abgeschlossen. Der fehlende Nachweis wird jetzt ausdrücklich als `RAN_UNVERIFIED` eingetragen und blockiert den Erfolg. Dieser Fehler fiel nicht im ersten Testlauf auf, sondern erst beim Review des eigenen Diffs nach der Skill-Vorgabe; der zugehörige Test prüft ihn mit einem echten `ToolLoopRun`.

**Teststand:** `./gradlew :app:testDebugUnitTest` → **880 Tests, 0 Fehler, 0 übersprungen** (vorher 830, +50 aus `AgentResultSummaryTest`).

**Geladene Skills:** `testing-setup` (Grenztests an den Zusagen, nicht an den Zeilen; jeder ehrliche Sonderfall hat einen eigenen Test) und als Ersatz für das nicht verfügbare `/code-review` neu installiert: **`requesting-code-review`** aus `obra/superpowers` (MIT, 294 106 Sterne, nicht archiviert, zuletzt gepusht 2026-09-27). Prüfung vor der Installation: Das Skill-Verzeichnis enthält nur `SKILL.md` und `code-reviewer.md` — **keine Skripte, keine ausführbaren Dateien**; `npx skills`bewertete es mit „Safe / 0 alerts / Low Risk“. Installation projektlokal unter `.agents/skills/` (nicht global), da die Freigabe des Nutzers allgemein war und nicht für ein fremdes global verändertes Verzeichnis. Genutzt als Review-Checkliste auf den eigenen Diff — daher der zweite Fund oben.

