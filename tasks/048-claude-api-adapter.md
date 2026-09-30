---
id: "048"
title: "Claude API anbinden"
wave: "W12"
depends_on: [004, 005, 047, 052, 059]
files: [tasks/048-claude-api-adapter.md]
skills: [`/claude-api`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "1f1951f6dabaa6d1"
---
# Aufgabe 048 — Claude API anbinden

## Ziel
Claude als Modell über den offiziell dokumentierten API-Weg verfügbar machen.

## Ergebnis
Anfrage-/Antwortadapter mit Streaming, Fehlern und aktuellen Modellinformationen.

## Fertig, wenn
- Anthropic-Dokumentation zur Implementierung erneut geprüft ist.
- Schlüssel des Nutzers direkt nach den Anbieterbedingungen verwendet wird.

## Schutz
Kein Claude-Abo-Login, OAuth-Token-Relay oder Weiterverkauf von Anthropic-Abfragen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Anthropic-API-Skill suchen; offizielle Quelle, Lizenz und Authentifizierung prüfen, Nutzer vor Installation fragen.
