---
id: "018"
title: "Hintergrundaufgaben"
wave: "W5"
depends_on: [010]
files: [tasks/018-background-task-lifecycle.md]
skills: [`android-permissions-security`, `android-profiler`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "5a934b4120fbbd5e"
---
# Aufgabe 018 — Hintergrundaufgaben

## Ziel
Vom Nutzer gestartete längere Arbeit nachvollziehbar fortsetzen und jederzeit stoppen.

## Ergebnis
Zustandsmodell für Start, laufend, pausiert, abgebrochen, fehlgeschlagen und beendet.

## Fertig, wenn
- Android-Anforderungen der Zielversion geprüft sind.
- Nutzer über laufende Arbeit informiert ist und sie stoppen kann.

## Schutz
Keine heimliche oder unbegrenzt laufende Hintergrundarbeit.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Hintergrundarbeit-Skill suchen; aktuelle Android-Regeln und Lizenz prüfen, Installation nur nach Zustimmung.
