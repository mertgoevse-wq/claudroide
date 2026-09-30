---
id: "054"
title: "OpenAI-artiges Nachrichtenformat"
wave: "W12"
depends_on: [004, 005, 047, 052, 059]
files: [tasks/054-openai-compatible-format.md]
skills: [`/claude-api`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "07edc0bfe6747d4d"
---
# Aufgabe 054 — OpenAI-artiges Nachrichtenformat

## Ziel
Eigene Endpunkte mit verbreitetem Nachrichtenformat anschließen.

## Ergebnis
Adapter und Prüfungen für Nachrichten, Streaming, Werkzeugantworten und Fehler.

## Fertig, wenn
- Nicht unterstützte Felder klar ausgewiesen sind.
- Keine Anbieterfunktion als universell angenommen wird.

## Schutz
Kopfdaten und Schlüssel nur an bestätigten Endpunkt senden.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen API-Format-/Adapter-Skill suchen; Quelle, Lizenz und Sicherheitswirkung prüfen, Nutzer vor Installation fragen.
