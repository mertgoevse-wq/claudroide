---
id: "016"
title: "App-Einstellungen"
wave: "W5"
depends_on: [010]
files: [tasks/016-app-settings.md]
skills: [`adaptive`, `android-permissions-security`]
status: done
gate: false
done_since_last_edit: true
content-hash: "403a8cc1dc8d7df3"
---
# Aufgabe 016 — App-Einstellungen

## Ziel
Sprache, Anbieter, Datenschutz, Sicherung, Freigaben und Warnungen leicht auffindbar machen, sichere Standardwerte setzen und sensible Schlüssel konsequent vor Klartext-Anzeige schützen.

## Ergebnis
Implementierte Einstellungsarchitektur (`AppSettings.kt`, `SettingsScreen.kt`) und Tests (`AppSettingsTest.kt`):

### 1. Strukturierte Einstellungsbereiche
1. **Sprache & Darstellung:**
   - Auswahl zwischen Systemstandard, Deutsch und Englisch (`LanguageSelectionDialog`).
   - AMOLED-Dark-Mode Schalter (Standard: aktiv für A56-Akkuschonung).
2. **Sicherheits- & Freigabestufe (Spec 6.1):**
   - **Vorsichtig (Standard):** Jede Dateiänderung und jeder Befehl erfordert Einzelbestätigung.
   - **Ausgewogen:** Unkritische Projektänderungen gebündelt nach Blockfreigabe.
   - **Weniger Rückfragen:** Zeitlich befristeter Expertenmodus (niemals still oder standardmäßig aktiv).
3. **Anbieter & Schlüsselverwaltung (BYOK):**
   - Übersicht konfigurierter Anbieter (Claude API, OpenRouter, Custom Endpoints).
   - **Strikte Maskierung:** Schlüssel werden niemals im Klartext gerendert (`SettingsState.maskApiKey()`, z.B. `sk-ant-api...3456`).
4. **Systemwarnungen & Ressourcenschutz:**
   - Nicht-kritische Akku- und Speicherwarnungen können vom Nutzer nach eigenem Ermessen deaktiviert werden.
   - Kritische Sicherheitsmeldungen bleiben unmaskiert und unübergehbar.
5. **Datenschutz & Protokolle:**
   - Schalter zur automatischen Bereinigung von Logs und Telemetrie (standardmäßig aus).
   - Unabhängigkeitshinweis und Versionsinformationen (`v0.1.0`).

### 2. Sicherheits- & Schutzregeln
- **Schlüsselschutz:** Schlüssel werden verschlüsselt im Android Keystore verwaltet und auf Einstellungsseiten nur maskiert dargestellt.
- **Wiederherstellbarkeit:** Alle Einstellungen besitzen sichere Werkseinstellungen (`SettingsState()`) und können jederzeit zurückgesetzt werden.

### 3. Konformitätsprüfung
- [x] Einstellungsmodell `AppSettings.kt` mit `ApprovalLevel` und `SettingsState` implementiert.
- [x] Maskierungsfunktion `maskApiKey()` schützt Tokens vor neugierigen Blicken und Screenshots.
- [x] Unit-Test `AppSettingsTest.kt` validiert Maskierung, Freigabestufen und Standardwerte.

## Fertig, wenn
- Nutzer jede wesentliche Auswahl ändern und wiederherstellen kann.
- Nicht-kritische Akku-/Speicherwarnungen abschaltbar sind.

## Schutz
Geheime Schlüssel werden nie auf Einstellungsseiten im Klartext angezeigt.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Einstellungen-/UX-Skill suchen, Quelle, Lizenz und Sicherheit prüfen; vor Installation Nutzerzustimmung einholen.
