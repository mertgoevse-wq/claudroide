---
id: "014"
title: "Sprache automatisch erkennen"
wave: "W5"
depends_on: [010]
files: [tasks/014-language-detection.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "03d5fc33a1178420"
---
# Aufgabe 014 — Sprache automatisch erkennen

## Ziel
Deutsch oder Englisch anhand der bevorzugten Handysprache wählen, einen sicheren englischen Standard-Fallback bereitstellen und gewährleisten, dass die automatische Erkennung eine manuelle Nutzereinstellung niemals überschreibt.

## Ergebnis
Implementierte Sprachlogik (`LanguageManager.kt`), englische Ressourcen (`values-en/strings.xml`) und Unit-Tests (`LanguageManagerTest.kt`):

### 1. Sprachauflösungs- und Fallback-Regel
- **Standard (`AppLanguage.SYSTEM`):**
  - Beginnt die primäre Gerätesprache mit `de` (z.B. `de_DE`, `de_AT`, `de_CH`), schaltet die App auf **Deutsch**.
  - Für alle anderen Sprachen (z.B. `en_US`, `fr_FR`, `tr_TR`, `es_ES`) greift automatisch und deterministisch der **englische Fallback** (`AppLanguage.ENGLISH`).
- **Garantie bei manueller Wahl:**
  - Hat der Nutzer in den Einstellungen ausdrücklich Deutsch oder Englisch gewählt (`AppLanguage.GERMAN` / `AppLanguage.ENGLISH`), ignoriert die App die Gerätesprache vollständig. Keine automatische Überschreibung der Nutzerpräferenz.

### 2. Symmetrie der Sprachressourcen
- Alle Schlüssel aus `res/values/strings.xml` sind 1:1 in `res/values-en/strings.xml` übersetzt.
- Identische Menüpunkte, identische Aktionsabläufe und identischer Unabhängigkeitshinweis (Disclaimer) auf Deutsch und Englisch.

### 3. Datenschutz & Geräteschutz
- Die Sprache wird rein lokal im Anwendungskontext ausgewertet (`Locale.getDefault()`).
- Keine Übertragung von Systemsprachen, Tastatur-IDs oder Geräteregionen an externe Server oder Anbieter.

### 4. Konformitätsprüfung
- [x] `LanguageManager.kt` mit Auflösungslogik und Vorrangregel ist implementiert.
- [x] Englische Übersetzungsdatei `values-en/strings.xml` ist vorhanden.
- [x] Unit-Test `LanguageManagerTest.kt` verifiziert deutsches System-Locale, englischen Fallback und unantastbare manuelle Wahl.

## Fertig, wenn
- Die App auf Deutsch und Englisch dieselben Abläufe anbietet.
- Automatische Auswahl eine manuelle Wahl nicht überschreibt.

## Schutz
Spracheinstellung nur lokal verwenden; keine unnötige Geräteinformation übertragen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Lokalisierungs-Skill suchen, Lizenz und Barrierefreiheit prüfen; Nutzer vor Installation fragen.
