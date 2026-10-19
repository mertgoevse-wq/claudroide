---
id: "079"
title: "Wiederholungsregeln"
wave: "W16"
depends_on: [070, 071]
files: [tasks/079-agent-retry-policy.md]
skills: [`testing-setup`, `android-permissions-security`]
status: done
gate: false
done_since_last_edit: true
content-hash: "c7089bbb3c43de26"
---
# Aufgabe 079 — Wiederholungsregeln

## Ziel
Bei vorübergehendem Fehler sicher fortfahren, ohne Aktionen doppelt auszuführen.

## Ergebnis
Wiederholungsregeln je Anfrage- und Werkzeugtyp mit Nutzerinformation.

## Fertig, wenn
- Schreib-, Installations- und Git-Aktionen nicht automatisch wiederholt werden.
- Kostenpflichtige Anfragen vor Wiederholung sichtbar sind.

## Schutz
Wiederholungen von Nebenwirkungen benötigen Idempotenz oder Bestätigung.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen API-Retry-/Sicherheits-Skill suchen, Quelle und Lizenz prüfen; Nutzer vor Installation fragen.
