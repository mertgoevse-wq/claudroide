---
id: "089"
title: "Dateiänderungen vergleichen"
wave: "W19"
depends_on: [082, 088]
files: [tasks/089-file-diff-viewer.md]
skills: [`/code-review`, `adaptive`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "51d1ad34d795c1d7"
---
# Aufgabe 089 — Dateiänderungen vergleichen

## Ziel
Nutzer sieht genau, was ein Agent ändern möchte.

## Ergebnis
Vergleich mit Datei, hinzugefügten/entfernten Zeilen und Gesamtumfang.

## Fertig, wenn
- Große Änderungen in Abschnitte teilbar sind.
- Neue und gelöschte Dateien separat gekennzeichnet sind.

## Schutz
Vergleich ist kein Speichern; keine Änderung ohne passende Zustimmung.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Diff-/Code-Review-Skill suchen; Quelle, Lizenz und Datensicherheit prüfen und Zustimmung zur Installation einholen.
