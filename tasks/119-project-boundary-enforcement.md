---
id: "119"
title: "Projektgrenze durchsetzen"
wave: "W24"
depends_on: [017, 045]
files: [tasks/119-project-boundary-enforcement.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "6e81c0856693effe"
---
# Aufgabe 119 — Projektgrenze durchsetzen

## Ziel
Datei- und Befehlswerkzeuge auf vom Nutzer ausgewählte Projektbereiche begrenzen.

## Ergebnis
Zentrale Prüfung vor Lesen, Schreiben, Löschen und Ausführen.

## Fertig, wenn
- Jeder Werkzeugtyp denselben Schutz verwendet.
- Widerrufener Zugriff unmittelbar blockiert wird.

## Schutz
Keine KI-Anweisung kann die App-Grenze selbst erweitern.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Filesystem-Sandbox-Skill suchen; Implementierung, Quelle und Lizenz prüfen, Nutzer vor Installation fragen.
