---
id: "122"
title: "Manipulierte Anweisungen"
wave: "W24"
depends_on: [017, 045]
files: [tasks/122-prompt-injection-defense.md]
skills: [`android-permissions-security`, `/code-review`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "5d43c34fb068fa86"
---
# Aufgabe 122 — Manipulierte Anweisungen

## Ziel
Dateiinhalte, Webseiten und Werkzeugausgaben nicht als höhere App-Regeln behandeln.

## Ergebnis
Vertrauensgrenzen, Kennzeichnung und Prüfabläufe für Anweisungen aus Projekten.

## Fertig, wenn
- Inhalt nicht selbst Berechtigungen oder Anbieterwahl ändern kann.
- Gefährliche Aufforderungen an Nutzer zur Bestätigung gehen.

## Schutz
Geheimnisse und Freigaberegeln bleiben außerhalb des Modellkontrollbereichs.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Prompt-Injection-Abwehr-Skill suchen, Quelle/Lizenz und Wirkung prüfen; Nutzer vor Installation fragen.
