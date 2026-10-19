---
id: "009"
title: "Projektaufbau"
wave: "W1"
depends_on: [001, 002, 003]
files: [tasks/009-project-layout.md]
skills: [`/swarm-planner`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "cb0d9e746b9b587d"
---
# Aufgabe 009 — Projektaufbau

## Ziel
Eine wartbare, modulare Ordnerstruktur für Android-App, Tests, Dokumente, Bilder und Build-Hilfen planen, die für Touch-Bedienung auf dem Galaxy A56 optimiert ist und Geheimnisse sicher ausschließt.

## Ergebnis
Ein kommentierter, verbindlicher Architektur- und Strukturplan:

### 1. Repository-Root-Struktur

```
claudroide/
├── .github/                      # CI/CD Workflows (Repo-Health, Builds)
│   └── workflows/
├── .claude/                      # Projektgebundene Fähigkeiten & Skills
│   └── skills/claudroide-resume/
├── app/                          # Android-Applikationsmodul (Kotlin + Jetpack Compose)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/org/claudroide/app/
│   │   │   │   ├── core/         # Basissysteme (Design, Network, Security, Platform)
│   │   │   │   ├── feature/      # Funktionale Bildschirme und UI-Flows
│   │   │   │   └── agent/        # Autonome Agenten-Engine, Werkzeuge & Subagenten
│   │   │   ├── res/              # Android XML-Ressourcen (Strings, Launcher-Icons)
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                 # Lokale Unit-Tests (JUnit4, Fakes, MockWebServer)
│   │   └── androidTest/          # Instrumented UI- & Integrationstests
│   └── build.gradle.kts
├── assets/                       # Verifizierte Grafiken (PNG/WebP/JPG: Logo, Maskottchen, Banner)
├── docs/                         # Technische Architektur- & Sicherheitsdokumente
├── progress/                     # Fortschrittskontrolle & Checkpoints (BUILD-STATE.md)
├── tasks/                        # 135 maschinenlesbare Aufgabenpläne mit YAML-Frontmatter
├── tools/                        # Lokale Automatisierungswerkzeuge (sync_frontmatter.py)
├── CLAUDE.md                     # Projekt- und Arbeitsanweisungen
├── README.md                     # Interaktive Dokumentation und Dashboard
└── settings.gradle.kts           # Gradle Multi-Module-Konfiguration
```

### 2. Modul- und Paketaufteilung im Detail (`app/src/main/java/org/claudroide/app/`)

- **`core/` (Wiederverwendbare Basissysteme):**
  - `core/design/`: Jetpack Compose Theme (Dark/Light), Farbpalette (Android-Grün `#3DDC84`, Terrakotta-Akzente), Typografie, Abstände.
  - `core/network/`: Ktor/OkHttp Client, SSE-Parser für Server-Sent Events, TLS-Pinning, automatischer Backoff.
  - `core/security/`: Android Keystore Wrapper, verschlüsselte SharedPreferences, Redaction-Engine zur Geheimnisfilterung.
  - `core/platform/`: Storage Access Framework (SAF) DocumentFile Wrapper, Akku- und Speichermonitoring.
- **`feature/` (Benutzeroberfläche & Workflows):**
  - `feature/chat/`: Chat-Liste, Dialogansicht, Message-Composer, Streaming-Bubble, Code-Viewer mit Syntax-Highlighting.
  - `feature/project/`: Projekt-Explorer, SAF-Ordnerauswahl, Diff-Viewer (Vorher/Nachher), Projektanweisungs-Editor.
  - `feature/provider/`: BYOK-Schlüsselverwaltung, Modell-Auswahl, Verbindungstester, Kostenrechner.
  - `feature/commands/`: Befehlsvorschau, Freigabedialoge (Stufen 1–3), Terminal-Ausgabekonsole.
  - `feature/settings/`: Spracheinstellung (DE/EN), Datenschutz-Schalter, Export- und Löschzentrum.
- **`agent/` (Arbeitsmaschine & Werkzeugschleife):**
  - `agent/engine/`: Planungsschleife, Kontextverwaltung, Sitzungs-Wiederherstellung.
  - `agent/tools/`: Lokale Werkzeuge (Dateilesen/-schreiben, Git-Befehle, Testrunner).
  - `agent/subagent/`: Spezialisierte Unterhelfer mit isoliertem Kontext und Ressourcenbegrenzung.

### 3. Schutz vor Datenlecks & Konventionen
- `.gitignore` schließt alle sensiblen Dateien (`*.env`, `*.key`, `*.apk`, `*.keystore`, `.gradle/`, `build/`) aus.
- Keine zirkulären Abhängigkeiten in den Paketen (`feature` hängt von `core` ab, niemals umgekehrt).

### 4. Konformitätsprüfung
- [x] Strukturplan ist vollständig dokumentiert und auf A56-Bedienung ausgelegt.
- [x] Keine zirkulären Voraussetzungen im Paketentwurf.
- [x] Klare Trennung von Tests (`test/`, `androidTest/`) und Quellcode.

## Fertig, wenn
- Struktur und Benennung in der Hauptdokumentation erklärt sind.
- Aufgabenabhängigkeiten keine kreisförmigen Voraussetzungen haben.

## Schutz
Keine Schlüssel, privaten Sicherungen oder nicht freigegebenen Nutzerdaten ins Repository aufnehmen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Projektstruktur-/Android-Architektur-Skill finden und Qualität, Lizenz und Quelle prüfen; Installation vorher bestätigen lassen.
