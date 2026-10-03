---
id: "135"
title: "Werkzeugrechte und Daten"
wave: "W28"
depends_on: [070, 117, 119, 122, 134]
files: [tasks/135-mcp-permission-and-data-view.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "76075be9d97b583d"
---
# Aufgabe 135 — Werkzeugrechte und Daten

## Ziel
Nutzer sieht, welche externen Werkzeuge Zugriff haben und welche Daten sie senden.

## Ergebnis
Rechteansicht, Datenvorschau, Freigabe pro Aktion und sofortige Abschaltung.

## Fertig, wenn
- Werkzeugaufrufe im Verlauf mit Ziel und Ergebnis erscheinen.
- Unbekannte oder geänderte Rechte erneut bestätigt werden.

## Schutz
Externe Werkzeuge dürfen weder Schlüssel auslesen noch Projektgrenzen umgehen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen MCP-Berechtigungs-/Datenschutz-Skill suchen; Quelle, Lizenz und Sicherheitsrisiken prüfen und Nutzer vor Installation fragen.
