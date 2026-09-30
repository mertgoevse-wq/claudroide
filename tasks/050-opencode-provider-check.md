---
id: "050"
title: "OpenCode-Weg prüfen"
wave: "W12"
depends_on: [004, 005, 047, 052, 059]
files: [tasks/050-opencode-provider-check.md]
skills: [`/swarm-planner`, `/claude-api`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "6daf56253cd70379"
---
# Aufgabe 050 — OpenCode-Weg prüfen

## Ziel
OpenCode als Anbieter, Client oder Modellzugang sauber unterscheiden.

## Ergebnis
Dokumentierte Wege, unterstützte API-Formate, Authentifizierung und Lizenz festhalten.

## Fertig, wenn
- Nur tatsächlich angebotene Modell-API integriert wird.
- CLI-Anmeldung oder Abo-Zugang nicht als BYOK-API ausgegeben wird.

## Schutz
Keine fremden Tokens aus lokalen Programmen lesen oder wiederverwenden.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen OpenCode-Integrations-Skill suchen; offizielle Dokumentation/Lizenz prüfen und Nutzer vor Installation fragen.
