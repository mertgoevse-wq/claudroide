---
id: "010"
title: "Android-App-Grundlage"
wave: "W3"
depends_on: [009, 019]
files: [tasks/010-android-app-shell.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "49a523b1ab64b5f4"
---
# Aufgabe 010 — Android-App-Grundlage

## Ziel
Eine minimale, installierbare Claudroide-App mit eigenständigem Namen und sicherer Standardkonfiguration bereitstellen.

## Ergebnis
Startbildschirm, App-Identität, nachvollziehbare Build-Anleitung und Bereiche für Chat, Projekte und Einstellungen im echten Kotlin / Compose App-Modul:

### 1. Implementierte App-Komponenten
- **Gradle Multi-Module Setup:**
  - Root `settings.gradle.kts` mit Google- und MavenCentral-Repositories und `:app`-Einbindung.
  - Root `build.gradle.kts` mit Android Gradle Plugin 8.7.0 und Kotlin Compose 2.0.21.
  - Modul `app/build.gradle.kts` (Namespace `org.claudroide.app`, compileSdk 35, minSdk 26, targetSdk 35, Jetpack Compose Material 3).
- **App-Deklaration & Sicherheit (`AndroidManifest.xml`):**
  - Application-Klasse `ClaudroideApp` registriert.
  - Ausschließlich minimale, notwendige Berechtigungen: `INTERNET` und `ACCESS_NETWORK_STATE`.
  - Keine invasiven oder gefährlichen Berechtigungen angefordert.
- **Adaptive AppShell (`MainActivity.kt` & `NavigationSuiteScaffold`):**
  - Touch-optimierte Navigation mit drei Hauptdestinationen:
    1. **Chats (`ChatScreen`):** Chatliste mit FloatingActionButton für neue Konversationen und sauberem Empty-State.
    2. **Projekte (`ProjectScreen`):** Projekt-Explorer mit Vorbereitung für Storage Access Framework (SAF).
    3. **Einstellungen (`SettingsScreen`):** Sicherheitsstufe, Unabhängigkeitshinweis (Disclaimer) und BYOK-Schlüsselkonfiguration.
- **AMOLED Dark Design (`Theme.kt`):**
  - Eigene Farbpalette (Android-Grün `#3DDC84`, Terrakotta `#E06D53`, AMOLED-Schwarz `#121413`).
- **Lokalisierung (`res/values/strings.xml`):**
  - Deutsch- und Englisch-Grundlagen ohne AI-Slop; verbindlicher Unabhängigkeitshinweis eingebettet.
- **Unit-Tests (`AppShellTest.kt`):**
  - Verifiziert Navigationsziele, Icons und Standardzustände.

### 2. Konformitätsprüfung
- [x] App-Scaffold ist modular in `app/` aufgesetzt und kompiliert nach Android 15 Standards.
- [x] Keine ungenutzten Berechtigungen (kein Storage-Vollzugriff, keine Standortdaten).
- [x] Unabhängigkeitshinweis und eigene Identität sind fest verankert.

## Fertig, wenn
- App am A56 startet und nach Schließen wieder startet.
- Keine ungenutzten Berechtigungen oder geheimen Zugangsdaten verlangt werden.

## Schutz
Keine Marke oder geschützte Gestaltung kopieren; Android-Sicherheitsregeln als Mindeststandard einhalten.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen nativen Android-App-Skill suchen; Quelle, Lizenz und Sicherheitslage prüfen und den Nutzer vor Installation fragen.
