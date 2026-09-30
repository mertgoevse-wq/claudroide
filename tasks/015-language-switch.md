---
id: "015"
title: "Sprache manuell wechseln"
wave: "W5"
depends_on: [010]
files: [tasks/015-language-switch.md]
skills: [`adaptive`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "5e83bcf19b7a8480"
---
# Aufgabe 015 — Sprache manuell wechseln

## Ziel
Nutzern ermöglichen, Deutsch oder Englisch unabhängig von der Handysprache auszuwählen, mit sofortiger visueller Vorschau, dauerhafter lokaler Speicherung und ohne Datenverlust bestehender Konversationen.

## Ergebnis
Implementierte Komponenten für den manuellen Sprachwechsel:
- `LanguagePreferences.kt`: Persistente Speicherung der Nutzerwahl (System, Deutsch, Englisch) in SharedPreferences.
- `LanguageSelectionDialog.kt`: Modal-Dialog mit Radio-Button-Vorschau und Bestätigungslogik.
- `LanguagePreferenceTest.kt`: Unit-Test zur Absicherung der Codes und Beschriftungen.

### 1. Ablauf und sofortige Übernahme
1. **Auswahl im Dialog:** Der Nutzer tippt in den Einstellungen auf die Sprachoption. Ein barrierefreier Auswahldialog zeigt die Optionen (Systemstandard, Deutsch, English).
2. **Sofortige Persistenz:** Die Auswahl wird lokal in `LanguagePreferences` abgelegt.
3. **Konsistente Umschaltung:** Alle Bildschirme (Chats, Projekte, Einstellungen, Fehler- und Bestätigungsdialoge) greifen auf die synchronisierten String-Ressourcen zu (`values/strings.xml` bzw. `values-en/strings.xml`), sodass keine gemischten Texte auftreten.

### 2. Sicherheits- und Datenschutzgarantien
- **Kein Datenverlust:** Das Umschalten der Anzeigesprache verändert weder bestehende Chatverläufe, noch Projektdateien, noch gespeicherte API-Schlüssel.
- **Rein lokal:** Die gewählte Sprache wird ausschließlich auf dem Galaxy A56 gespeichert und nicht an externe Dienste übertragen.

### 3. Konformitätsprüfung
- [x] Manueller Sprachwechsel ist implementiert und dauerhaft speicherbar.
- [x] Vollständige Symmetrie der Übersetzungen verhindert Mischtexte.
- [x] Unit-Test `LanguagePreferenceTest.kt` validiert Sprachkonstanten.

## Fertig, wenn
- Chat, Fehlermeldungen, Berechtigungen und Bestätigungen mitwechseln.
- Texte nicht unvollständig oder aus Versehen gemischt erscheinen.

## Schutz
Sprachwechsel löscht keine Chats und sendet keine Daten.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Mehrsprachen-/Android-Lokalisierungs-Skill suchen; Quelle/Lizenz prüfen und Installation mit Nutzer abstimmen.
