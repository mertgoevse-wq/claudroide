---
id: "044"
title: "Anbieter hinzufügen"
wave: "W10"
depends_on: [005, 016, 017]
files: [tasks/044-provider-add-flow.md]
skills: [`adaptive`, `android-permissions-security`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "f92af27bd8cffcce"
---
# Aufgabe 044 — Anbieter hinzufügen

## Ziel
Eigene Anbieterzugänge mit klaren, sicheren Feldern anlegen.

## Ergebnis
Name, API-Format, Serveradresse, Schlüssel, Modell und Verbindungstest erfassen.

## Fertig, wenn
- Eingabefehler konkret erklärt werden.
- Nutzer vor dem Speichern den Anbieter und das Datenziel prüfen kann.

## Schutz
Schlüssel beim Tippen maskieren und niemals in Diagnoseausgaben übernehmen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen API-Provider-/Formular-Sicherheits-Skill suchen; Quelle/Lizenz prüfen und vor Installation fragen.
