# ClauDroide-Bauzustand

**Sitzung 45 — Awesome-Prompt-Katalog ausgebaut (2026-10-18):**
- Anforderung dieses Blocks: Den Awesome-Prompt-Katalog (Prompt-Auswahl-Chips/Loader) aus ClauDroide entfernen; README/Dokumentation nicht länger einen ausgelieferten Katalog behaupten; Regressionstests für die entfernten Pfade.
- Umsetzung: `ChatViewModel.kt` (Prompt-Auswahl/Chips-Logik entfernt), `SessionDataStore.kt` (Persistenz-Schlüssel `active_prompt_title`/`system_prompt` samt Migration entfernt), `SessionDataStorePromptMigrationTest.kt` entfernt, `ChatViewModelTest.kt` bereinigt, `AppShellTest.kt`/`MainActivityBackHandlerTest.kt` aktualisiert, `app/src/main/assets/prompts/**` komplett gelöscht, `feature/prompt/` gelöscht, `ProviderTransport.kt`: optionalen `system`-Prompt-Parameter entfernt, damit kein Prompt-Einspeisepfad offen bleibt; reguläre technische Systemanweisungen des Provider-Formatters bleiben unberührt.
- Verifikation: `:app:testDebugUnitTest` → 2.623 Tests, 0 Fehler, 0 übersprungen; `:app:assembleDebug` → BUILD SUCCESSFUL; `secret_gate.py .` → 0 Treffer; `sync_frontmatter.py --check` → OK (135/135); README/DOKU-Claims greifen nicht mehr auf den Katalog.
- Freigabe durch den Nutzer: Commit des Gesamtstands (inkl. App-Icon-Regeneration, neuer Code/MCP-, Media-Studio-, Artefakt-Screens) und Push an `next` (`mertgoevse-wq/claudroide-next.git`). Bekannt, dass das Remote PUBLIC ist — Push geschieht mit dieser ausdrücklichen Einzelbestätigung.

**Sitzung 44 — Gespeicherte Chat-Sitzungen und anwendbare Prompt-Katalog-Prüfung (2026-10-18):**
- Anforderung dieses Blocks: Statt Drawer-Beispieldaten echte zuletzt gespeicherte Chats zeigen und gespeicherte Unterhaltung samt Verlauf wieder öffnen; Provider-/Modell-/Prompt-Kontext mit wiederherstellen; keine Kontextumschaltung während eines Streams; Anmeldedaten in Titel/Vorschau maskieren.
- Awesome-Prompts-Prüfung: alle 14 Einträge und Promptdateien aus `app/src/main/assets/prompts/prompts_index.json` geprüft; keine fehlenden Dateien oder falschen Zeichenzähler, insgesamt 158.066 Zeichen. Das separate `_sources/github/awesome-prompts`-Checkout hat ein `origin`-Remote, aber weder lokale Commits noch Dateien; `git ls-remote --heads origin` lieferte keine Branch-Refs. Daher keine externen Prompts erfunden oder ungeprüft übernommen. Die 14 lokal enthaltenen Promptdateien sind im App-Katalog `andrej_karpathy_coding_guidelines`, `anti_ai_slop_design_architect`, `claude_artifacts_prompt`, `claude_code_subagent_designer`, `code_reviewer_security`, `coding_agent_system_prompt`, `debugging_agent`, `frontend_taste_engineer`, `generative_audio_prompt_engineer`, `generative_image_prompt_engineer`, `jetpack_compose_architect`, `mcp_server_architect`, `superpowers_agentic_development_framework` und `video_gen_prompting`.
- Slash-Command-Inventar: keine aktiven Dateien in `.claude/commands`, `~/.claude/commands` oder `.agents/commands`; als nutzeraufrufbare Skills sind `/claudroide-resume` und `/cdx` verfügbar. Zusätzlich wurden die zwölf vendorten Referenz-Command-Dateien gelesen (`claude-code-android`: `/doc-sync`, `/fix`, `/phase-start`, `/release`, `/ship`, `/status`, `/ui-polish`, `/verify`; `claude-media-bridge`: `/claude-media-bridge`; `design-skill-library`: `/design-interview`, `/design-skills`, `/design`). Diese Vendored-Dateien sind keine in dieser Sitzung registrierten Befehle. Anwendbare Checklisten wurden sinngemäß für Status, Fehlerbehebung, Verifikation, UI-Prüfung, Dokumentation und Commit benutzt; Release, Mediengenerierung, Design-Interview und Phasenstart waren für diesen Chat-Session-Block nicht einschlägig.
- Geordnete Prüf-/Umsetzungsabfolge (aufgabenrelevante Rollen statt blindem Laden fachfremder Skills): 1) Auftrag/Scope und Quellen: `coding_agent_system_prompt`, `classic_software_engineering_canon`, `pragmatic_programmer`, `/status`; 2) Abhängigkeiten und sichere Parallelgrenzen: `swarm-planner`, `parallel-task`, `agent_cooperation_designer`, `parallel_codegen_architect` (nur getrennte Inventur-/Review-Läufe parallel, keine verfügbare Subagent-Schnittstelle); 3) Sessionmodell, Testseams und RED→GREEN: `local_first_memory_engineer`, `agent_state_hygiene_architect`, `agent_reliability_engineer`, `android-testing`, `testing-setup`, `tdd`; 4) Implementierung/Compose-Integration: `agent_context_efficiency_engineer`, `jetpack_compose_architect`, `compose`, `frontend_taste_engineer`, `anti_ai_slop_design_architect`, `accessibility_auditor`; 5) sensible Sitzungsdaten und Prompt-Injection: `android-permissions-security`, `security-and-hardening`, `vibe-security`, `code_reviewer_security`, `owasp_secure_application_architect`, `security_researcher`, `agent_data_injection_attack_auditor`, `instruction_bleed_auditor`; 6) Fehler- und unabhängige Verifikation: `debugging_agent`, `verification_specialist`, `test_strategy_architect`, `trustworthy_agent_reviewer`, `/fix`, `/verify`; 7) Checkpoint und Commit-Schnitt: `agent_context_efficiency_engineer`, `/doc-sync`, `/ship`. Ein Review-Text ist keine echte Subagent-Ausführung; parallel liefen nur unabhängige Tool-Prüfungen. Nicht anwendbare Bild-/Audio-/Video-/MCP-Implementierungsrollen wurden nicht als Begründung für Chat-Code missbraucht.
- Verfügbarkeitsgrenze: lokales Inventar zählt 39 `SKILL.md`-Wurzeldateien; 14 eindeutig passende Skills wurden in dieser Arbeitsphase tatsächlich über den Skill-Aufruf geladen (darunter `testing-setup`, `android-permissions-security`, `android-testing`, `security-and-hardening`, `vibe-security`, `requesting-code-review`, `karpathy-guidelines`, `compose`, `tdd`, `design-library`, `optimizing-lazy-layouts`, `gradle-build-performance`, `claudroide-resume`, `cdx`). Nicht alle installierten Skills sind für Chat-Sessions einschlägig; sie pauschal zu laden hätte keine fachliche Validität. Die lokale Design-Skill-Selector-Datei wurde am erwarteten globalen Pfad nicht gefunden. Kein Skill installiert oder global geändert.
- Remote-/Command-Grenze: ein Push ist eine externe Nebenwirkung und das dokumentierte `next`-Remote ist PUBLIC; kein Push ohne ausdrückliche Einzelbestätigung. Keine echte Subagent-Schnittstelle verfügbar; voneinander unabhängige Quellen- und Ergebnisprüfungen liefen parallel.
- Umgesetzter Session-Block: `SessionDataStore.kt` speichert geordnete, auf 25 begrenzte Session-Snapshots mit Legacy-Einzelverlauf-Fallback; `ChatViewModel.kt` stellt den neuesten Kontext wieder her, aktualisiert sichere Zusammenfassungen, lädt ausgewählte Transkripte und weist Auswahl/neuen Chat/Provider-/Prompt-Wechsel während Streaming ab; `MainActivity.kt` ersetzt erfundene Drawer-Beispiele durch reale gespeicherte Sessions; `NavigationDrawer.kt` zeigt Titel und redigierte Vorschau. Tests in `ChatViewModelTest.kt` decken Start-Wiederherstellung, Auswahl, unbekannte ID, Secret-Redaktion, Persistenz vor Providerfehler, Prompt-Rollen und Streaming-Sperre ab.
- Commit-Schnitt: die Session-Hunks in `MainActivity.kt` und `NavigationDrawer.kt` sind von gleichzeitig geänderten CODE/MCP-, Media-Studio- und Artefakt-Navigations-Hunks trennbar; nur Session-Listen-/Preview-Hunks sollen in diesen Commit. Die neuen Screens, Assets, README-, Icon- und Medienänderungen bleiben unstaged und unberührt.
- Verifikation: gezielte `ChatViewModelTest` zunächst RED mit 25 Tests/2 Fehlern (Tests belegten fehlenden Schutz gegen Provider-/Prompt-Wechsel im Stream und fehlende Speicherung der Anbieterauswahl); nach Ursachenfix GREEN, 25/25. Der serielle vollständige `:app:testDebugUnitTest`-Lauf nach dem letzten Codefix endete `BUILD SUCCESSFUL`, Exit 0; JUnit-XMLs zählen 2.628 Tests in 141 Suiten, 0 Fehler, 0 übersprungen. Zusätzlich `python3 tools/secret_gate.py .` → 0 Treffer, `python3 tools/sync_frontmatter.py --check` → OK (135/135), `git diff --check` → sauber. Die Ergebnisdatei `/tmp/claudroide-full-unit-final.log` enthält den erfolgreichen Abschluss (1m 11s, 22 actionable tasks, 1 executed, 21 up-to-date).
- Einschränkungen: `adb devices -l` lieferte keine Targets; echtes Render-/Drawer-Klickverhalten auf A56/Emulator nicht manuell prüfbar; keine Compose-UI-Tests im Projekt. `SessionDataStore` nutzt serialisierte JSON-Sessions in Preferences-DataStore; kein Android-DataStore-Integrationstest in diesem Block. 39 Skill-Wurzeldateien im lokalen Inventar, aber nur passende geladen. Workspace bleibt mit unabhängigen README-, Icon-, Medien-, Prompt- und Screenänderungen dirty; keine davon zurückgesetzt.
- Letzte sichere Git-Referenz: `26d2b31`; Branch `feat/claudroide-next-architecture`. Session-Commit wird nur aus explizit ausgewählten Session-Dateien/Hunks gebildet; kein Push (bekanntes `next`-Remote ist PUBLIC und noch nicht autorisiert).

---



**Stand:** 2026-10-16 (Sitzung 43 — Einrichtung und Verifikation des reproduzierbaren Galaxy A56 Entwicklungsworkflows)
**Status:** **135 von 135 Aufgaben `done`**, 0 offen. Kanonische Codebasis `claudroide-next` konsolidiert (`docs/INTEGRATION_MAP.md`). Lokale A56-Toolchain (Java 17 Temurin arm64, native aapt2 2.20 aarch64, Android SDK API 34/35) erfolgreich verifiziert. Workflow-CLI `tools/cdx` und Terminal-Workspace `tools/cdx-tmux` implementiert. Skills `/claudroide-resume` und `/cdx` global aktiviert. Fast-Unit-Testlauf erfolgreich durchgeführt (BUILD SUCCESSFUL in 2m 10s, 0 Fehler). Keine offenen Blocker für lokale Weiterentwicklung.

**Teststand (selbst gemessen, 2026-10-16):** Fast-Unit-Tests (`:app:testDebugUnitTest --tests "org.claudroide.app.ThemeScrimTest"`) → BUILD SUCCESSFUL in 2m 10s, 22 actionable tasks, 0 Fehler. Vollständige Baseline (2026-10-15): 2.612 Tests in 139 Suiten, 0 Fehler, 0 übersprungen. `python3 tools/secret_gate.py .` → 0 Treffer, exit 0. `python3 tools/sync_frontmatter.py --check` → OK. `git diff --check` → OK.

**Letzte sichere Git-Referenz:** `69e93f3` auf `feat/claudroide-next-architecture`.

**APK-Delivery (selbst gemessen, 2026-10-14):** Kopiert nach `/sdcard/Download/ClauDroide-debug.apk` (23.331.258 Bytes); `/sdcard/Download/ClauDroide-debug.apk.sha256` erstellt. SHA-256 des kopierten Downloads identisch mit dem Build-Output (`cd33110ea5d6f618462cb248afdd8bcbcb40935b9558c1a0084442e985115d60`).

**AndroidKeystore & StrongBox (Block B verifiziert, 2026-10-14):** `AndroidKeystoreKeyVault.kt` für `minSdk 26` gehärtet. API-sichere Entkopplung von `setIsStrongBoxBacked` über `Api28Helper`, hardwarebasierte Prüfung via `PackageManager.FEATURE_STRONGBOX_KEYSTORE`, robuster Fallback auf TEE-Keystore (AES-256-GCM) bei fehlender StrongBox oder API < 28. `KeyVaultFactory` verbietet stillen Fallback auf Android-Geräten. 8 Unit-Tests in `SecureKeyStorageTest.kt` grün.

**Mutationsprüfungen Tasks 085/086 (Block B verifiziert, 2026-10-14):**
- **Task 085:** Deaktivierung der Geräteprüfungs-Bedingung in `UsbPathAvailability.isUsable` führte zu exakt 2 roten Tests (`kein USB-Weg ist verfuegbar ohne Geraetemessung`, `eine perfekt aussehende Erkennung allein macht den Weg nicht verfuegbar`).
- **Task 086:** Deaktivierung der Bestätigungsprüfung in `ResumeGate.mayResume` führte zu exakt 2 roten Tests (`ein guter Zugriff allein genuegt nicht`, `eine Ablehnung nennt die betroffenen Dateien einzeln`).
- Beide Mutationen ordnungsgemäß zurückgenommen. Klare Unterscheidung: Logische Entscheidungs- und Ablehnungsfunktionen sind nachgewiesen und testgesichert; physische USB-Laufwerksmessung bleibt mangels echtem angeschlossenen USB-Medium am PRoot-Host ungemessen.

**WebP-Assets (Block A verifiziert, 2026-10-14):** `tools/generate_webp.py` erzeugt `claudroide_mark.webp` (31 KB, RGBA) und `claudroide_banner.webp` (27 KB, RGB) reproduzierbar aus `assets/brand/mark-1024.png` und `assets/brand/banner.png`. Beide liegen stabil unter dem 50 KB Cap (`BrandAssetContractTest`).

**CI-Triggers (verifiziert, 2026-10-14):** `build-apk.yml` und `repo-health.yml` triggern auf `[feat/claudroide-next-architecture]`. Default-Branch des kanonischen Repos ist `feat/claudroide-next-architecture`.

**Gerät (selbst gemessen, 2026-10-14):** SM-A566B, Termux/Debian/PRoot. `adb devices` zeigt **kein** Gerät (PRoot Sandbox). APK direkt auf `/sdcard/Download/` bereitgestellt. Kein automatisierter Gerätetest möglich.

**Remote-Verifizierung (2026-10-14):**
- Kanonisches Ziel: `next` → `https://github.com/mertgoevse-wq/claudroide-next.git` — **PUBLIC**, Default-Branch: `feat/claudroide-next-architecture`.
- Ursprüngliches Repo: `origin` → `https://github.com/mertgoevse-wq/claudroide.git` — niemals dorthin pushen.

**Letzte sichere Git-Referenz:** `7b71662` auf `feat/claudroide-next-architecture`.

**Phase 1+2-Checkpoint (2026-10-14, Sitzung 41):**
- Repository verifiziert: Branch `feat/claudroide-next-architecture`, Commit `7b71662`, up-to-date mit `next/feat/claudroide-next-architecture`.
- Unstage Änderung: `AndroidKeystoreKeyVault.kt` — `@RequiresApi`-Annotationen zu `Api28Helper` (korrekt, 8 Tests grün).
- Release-Signing-Konfiguration erstellt und implementiert:
  - Keystore generiert außerhalb des Repos: `/home/mert/claudroide-keystore/claudroide-release.keystore` (RSA 2048-bit, 10000 Tage Gültigkeit, SHA256withRSA, bis 2054-03-01).
  - `keystore.properties` im Projekt-Root angelegt (in `.gitignore`, nie committed).
  - `app/build.gradle.kts` erweitert: `signingConfigs { create("release") { ... } }` + `signingConfig = signingConfigs.getByName("release")` im `release`-Build-Typ.
  - `.gitignore` um `keystore.properties` und `*.release.keystore` erweitert.
- **`:app:assembleRelease` BUILD SUCCESSFUL** in 8m 14s, 46 Tasks (14 executed, 32 up-to-date).
  - Release-APK: `app/build/outputs/apk/release/app-release.apk`, **3.9 MB** (83% kleiner als Debug-APK mit 23 MB).
  - SHA-256: `29616268823ebc66c992784cd4e108bdd8a51a781414de204e24358e0513af6d`.
  - Signatur verifiziert: v2 (APK Signature Scheme v2) **verified**, 1 signer, `apksigner` output: `Verifies`.
  - R8-ProGuard-Audit: Tink-Klassen (`com.google.crypto.tink.Config` uvm.) korrekt in `seeds.txt` behalten. 39 Warnings zu impliziten Default-Constructor-Keep-Regeln in Drittanbieter-Consumer-Proguard-Files — **nicht kritisch**, beeinträchtigen keine Funktionalität.
- Unit-Tests: **2.612 Tests in 139 Suiten, 0 Fehler, 0 übersprungen** (identisch zum Baseline-Stand).
- `python3 tools/secret_gate.py .` → 0 Treffer (exit 0).
- `python3 tools/sync_frontmatter.py --check` → OK (135/135 Tasks konsistent).
- `git diff --check` → sauber.
- APK nach `/sdcard/Download/ClauDroide-release.apk` kopiert; SHA-256 identisch.
- **Letzte sichere Git-Referenz:** `d6efd12` auf `feat/claudroide-next-architecture`.

**Sitzung 42 — Vollständiger Release-Readiness-Audit & Abschlussprüfung (2026-10-15):**
- **135/135 Aufgaben unabhängig auditiert:**
  - `reports/FEATURE_TRACEABILITY_MATRIX.csv`: 135 Datensätze mit Akzeptanzkriterien, Quelldateien, UI-Pfaden, Testresultaten, Hardwareevidenz und Status (`PASS`, `PARTIAL`, `BLOCKED`).
  - 131 Aufgaben mit vollständiger Kotlin-Implementierung und 100% grünen Tests (`PASS`).
  - 4 Aufgaben mit ehrlich ausgewiesenen Hardware-Schranken (`PARTIAL`/`BLOCKED`: 006 ADB im PRoot, 007 NPU-HAL, 085/086 USB-Hardware-Probe).
- **Vier Evidenz- und Auditberichte erstellt und abgelegt:**
  - `reports/RELEASE_READINESS_AUDIT.md`: Umfassender Audit-Bericht mit offiziellem Votum **CONDITIONAL GO** (Sideload/Interner Test: GO; Play Store: GATED bis A56-Rauchtest und Markenfreigabe).
  - `reports/FEATURE_TRACEABILITY_MATRIX.csv`: Detaillierte Traceability-Matrix aller 135 Tasks.
  - `reports/CRASH_AND_REGRESSION_AUDIT.md`: Dokumentation der 5 behobenen Crash-Ursachen (Back-Navigation, SAF-Persistenz, Dark-Mode-Scrim, StrongBox minSdk 26, Termux OOM) und Mutationsprüfungen.
  - `reports/RELEASE_TEST_EVIDENCE.md`: Vollständiger Nachweis aller 2.612 Tests, APK-Größen, Prüfsummen und Signaturprüfungen.
- **Frische Test-Gesamtverifikation (selbst gemessen, 2026-10-15):**
  `./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx1536m" :app:testDebugUnitTest --rerun-tasks` → **BUILD SUCCESSFUL in 13m 13s, 22 executed tasks, 2.612 Tests in 139 Suiten, 0 Fehler, 0 übersprungen**.
- **Build-Artefakte verifiziert:**
  - Release-APK: `app/build/outputs/apk/release/app-release.apk` (3.997.360 Bytes, SHA-256 `29616268823ebc66c992784cd4e108bdd8a51a781414de204e24358e0513af6d`, Scheme v2 verifiziert via `apksigner`).
  - Debug-APK: `app/build/outputs/apk/debug/app-debug.apk` (22.783.042 Bytes, SHA-256 `f5edb453a8fbcbcecf3c851a515f9f63164687e89fdd264c8d9c4f5aaa3440f7`, Schema v1+v2 signiert).
- **Sicherheits- und Qualitätsgates:**
  - `python3 tools/secret_gate.py .` → 0 Treffer (exit 0).
  - `python3 tools/sync_frontmatter.py --check` → OK (135/135 Tasks konsistent).
  - `git diff --check` → sauber.
- **Nächster Schritt:** Push an das verifizierte Repository `next/feat/claudroide-next-architecture` und manueller Smoke Test durch den Nutzer auf dem Samsung Galaxy A56.
