---
id: "030"
title: "Lade- und Fehlerzustände"
wave: "W7"
depends_on: [010, 023, 024]
files: [tasks/030-ui-error-and-loading-states.md]
skills: [`adaptive`, `/code-review`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "fcfcbd3431f2b8cf"
---
# Aufgabe 030 — Lade- und Fehlerzustände

## Ziel
Nutzer bei Wartezeiten und Fehlern nicht mit unverständlichen Technikmeldungen alleinlassen.

## Ergebnis
Meldungen und nächste Schritte für Laden, leere Liste, Offline, Berechtigung verweigert und Anbieterfehler.

## Fertig, wenn
- Jede Meldung erklärt, ob Daten geändert oder gesendet wurden.
- Wiederholen und Abbrechen getrennte, sichere Handlungen sind.

## Schutz
Fehlertext enthält keine Schlüssel, privaten Dateiinhalte oder vollständigen sensiblen Protokolle.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen UX-Fehlermeldungs-Skill suchen; Lizenz/Quelle prüfen und vor Installation Nutzerzustimmung einholen.
