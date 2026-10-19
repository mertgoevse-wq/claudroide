---
id: "070"
title: "Gemeinsame Agent-Funktionen"
wave: "W15"
depends_on: [048, 049, 052, 053, 054]
files: [tasks/070-provider-independent-agent-contract.md]
skills: [`/claude-api`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "23c1bcafbbaa451a"
---
# Aufgabe 070 — Gemeinsame Agent-Funktionen

## Ziel
Chat, Dateiwerkzeuge und Freigaben über Anbieter hinweg einheitlich steuern.

## Ergebnis
Vertrag für Anfrage, Streaming, Werkzeuge, Fehler, Abbruch und Fähigkeiten.

## Fertig, wenn
- Anbieterbesonderheiten nachvollziehbar abgebildet sind.
- Fehlende Funktionen sichtbar statt still simuliert werden.

## Schutz
Ein gemeinsamer Adapter darf Anbieterschlüssel oder Regeln nicht umgehen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Agent-Architektur-/API-Skill suchen, Quelle/Lizenz prüfen und Nutzer vor Installation fragen.
