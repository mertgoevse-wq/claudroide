---
id: "017"
title: "Android-Erlaubnisse"
wave: "W5"
depends_on: [010]
files: [tasks/017-device-permissions.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "08a24df1cd4682d0"
---
# Aufgabe 017 — Android-Erlaubnisse

## Ziel
Nur die Android-Zugriffe anfordern, die für eine gerade gewählte Funktion nötig sind.

## Ergebnis
Erlaubnisablauf mit Zweck, Zeitpunkt, Ablehnen-Folge und späterem Widerruf.

## Fertig, wenn
- Projektdateien über die System-Dateiauswahl und nicht über pauschalen Zugriff geöffnet werden.
- Ablehnen die App nicht unnötig unbrauchbar macht.

## Schutz
Keine Root-, versteckten oder unnötig weitreichenden Rechte verlangen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Berechtigungs-Skill suchen, Herkunft/Lizenz und Sicherheitswirkung prüfen; Nutzer vor Installation fragen.
