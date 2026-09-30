---
id: "005"
title: "Anbieter-Regelmatrix"
wave: "W1"
depends_on: [001, 002, 003]
files: [tasks/005-provider-policy-matrix.md]
skills: [`/claude-api`, `/code-review`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "def13a7e20a83e8d"
---
# Aufgabe 005 — Anbieter-Regelmatrix

## Ziel
Für Claude API, OpenRouter, OpenCode, Antigravity und eigene Endpunkte jeweils den realen, zulässigen Anschlussweg bestätigen.

## Ergebnis
Tabelle mit offizieller Quelle, Anmeldung, Schlüsseltyp, Kostenabrechnung, Datenhinweisen, App-Kompatibilität und letztem Prüfdatum. Nicht belegte Wege stehen auf „nicht verfügbar“.

## Fertig, wenn
- Jede Integration eine Primärquelle und überprüfte Bedingungen hat.
- Abo-/CLI-Token-Import nicht mit normalem API-Zugang verwechselt wird.

## Schutz
Keine Provider-Token aus fremden Programmen auslesen oder weiterreichen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Skill zu API-Integration/Provider-Prüfung suchen; Quelle, Lizenz und Fähigkeiten prüfen, Nutzer vor Installation fragen.
