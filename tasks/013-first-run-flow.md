---
id: "013"
title: "Ersteinrichtung"
wave: "W5"
depends_on: [010]
files: [tasks/013-first-run-flow.md]
skills: [`adaptive`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "9711895fa9b89d55"
---
# Aufgabe 013 — Ersteinrichtung

## Ziel
Neue Nutzer ohne Fachsprache durch Sprache, Datenschutz, Anbieter und erstes Projekt führen und eine jederzeit überspringbare und zurücksetzbare Onboarding-Strecke bereitstellen.

## Ergebnis
Implementierte 4-Stufen-Ersteinrichtung (`OnboardingScreen.kt`) mit klaren Erklärungen und vollständiger Nutzerkontrolle:

### 1. Die 4 Schritte des Onboarding-Wizards
1. **Schritt 1: Willkommen & Unabhängigkeit:**
   - Begrüßung mit Claudroide-Mascot-Grafik.
   - Verbindlicher Unabhängigkeitshinweis (keine geschäftliche Verbindung zu Anthropic, PBC).
   - Erläuterung der Grundfunktionen für Touch-Bedienung auf dem Smartphone.
2. **Schritt 2: Sprache & Darstellung:**
   - Automatische Erkennung der Handysprache (Deutsch / English).
   - Vorschau des akkuschonenden AMOLED-Dark-Themes für das Galaxy A56.
3. **Schritt 3: Eigene Schlüssel (BYOK) & Kosten:**
   - Transparente Aufklärung: Claudroide ist kostenlos und quelloffen; mögliche API-Kosten entstehen ausschließlich direkt beim gewählten Anbieter (Claude API / OpenRouter).
   - Wahlmöglichkeit: Schlüssel direkt eingeben oder „Erst erkunden / Später einrichten“.
4. **Schritt 4: Erstes Projekt:**
   - Option zur Auswahl eines Arbeitsordners via Android Storage Access Framework (SAF) oder Start ohne Projekt.

### 2. Bedienungs- und Schutzgarantien
- **Jederzeit überspringbar:** Über den Button „Überspringen“ in der oberen Leiste kann die Ersteinrichtung sofort verlassen werden.
- **Rücksetzbar:** In den Einstellungen (`SettingsScreen`) kann der Onboarding-Wizard jederzeit erneut aufgerufen oder der Einrichtungsstatus zurückgesetzt werden.
- **Keine stillen Aktionen:** Kein Anbieter-Schlüssel und kein Dateizugriff wird ohne explizite Nutzerhandlung registriert. Keine automatischen Hintergrund-Downloads oder Kontenerstellungen.

### 3. Konformitätsprüfung
- [x] Onboarding-Composable (`OnboardingScreen.kt`) und Step-Logik sind implementiert.
- [x] Unit-Test `OnboardingTest.kt` verifiziert Schrittfolgen und Vollständigkeit.
- [x] Alle Schutzregeln gegen ungewollte Kosten oder Zugriffe sind gewahrt.

## Fertig, wenn
- Kein Anbieter-Schlüssel oder Projektordner ohne Nutzerhandlung hinzugefügt wird.
- Nutzer Einrichtung später fortsetzen oder zurücksetzen kann.

## Schutz
Keine stillen Konten, Downloads, Berechtigungen oder Datenübertragungen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen UX-/Onboarding-Skill suchen; Quelle, Lizenz und Datenschutzbezug prüfen, Installation nur nach Bestätigung.
