---
id: "086"
title: "USB-Verlust abfangen"
wave: "W19"
depends_on: [082, 085]
files: [tasks/086-usb-disconnect-recovery.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "3752af011d13fcf3"
---
# Aufgabe 086 — USB-Verlust abfangen

## Ziel
Laufende Projektarbeit sicher anhalten, wenn Speicher verschwindet.

## Ergebnis
Schreibstopp, Warnung, ungespeicherte Änderungen und Wiederverbindung prüfen.

## Fertig, wenn
- Keine Änderung als gespeichert angezeigt wird, wenn sie es nicht ist.
- Fortsetzen erst nach erneutem Zugriffs- und Dateistatuscheck möglich ist.

## Schutz
Keine automatische Kopie an unbekannten Ort.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Wechselspeicher-/Datensicherheits-Skill suchen; Quelle/Lizenz prüfen, Installation vom Nutzer bestätigen lassen.
