---
id: "114"
title: "Abbrechen und aufräumen"
wave: "W25"
depends_on: [105, 119, 120]
files: [tasks/114-task-cancel-and-cleanup.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "66c036df6ee744f9"
---
# Aufgabe 114 — Abbrechen und aufräumen

## Ziel
Laufende Agenten- und Befehlsarbeit zuverlässig beenden.

## Ergebnis
Stop-Signal, Prozessende, temporäre Dateien, unvollständige Änderungen und Wiederaufnahme.

## Fertig, wenn
- Nutzer erfährt, welche Aktionen bereits abgeschlossen wurden.
- Temporäre Projektänderungen nicht still gelöscht werden.

## Schutz
Abbruch beendet keine externe Nebenwirkung, die schon gesendet wurde; das wird erklärt.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Prozess-/Abbruch-Skill suchen; Quelle, Lizenz und Sicherheitswirkung prüfen, Nutzer vor Installation fragen.
