---
id: "084"
title: "ZIP-Projekt öffnen"
wave: "W18"
depends_on: [010, 017]
files: [tasks/084-zip-import.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "0ba49ecfc52b6fab"
---
# Aufgabe 084 — ZIP-Projekt öffnen

## Ziel
ZIP auswählen, Inhalt prüfen und als Projekt verfügbar machen.

## Ergebnis
Dateiliste, Speicherbedarf, Zielordner, Fortschritt und Abbruchmöglichkeit.

## Fertig, wenn
- Doppelte/unerwartete Pfade und übergroße Archive abgefangen werden.
- Nutzer vor dem Entpacken Ziel und Größe sieht.

## Schutz
Zip-Slip, bösartige Namen und Überschreiben bestehender Dateien verhindern.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen sicheren ZIP-/Android-Datei-Skill suchen, Quelle/Lizenz prüfen und vor Installation Zustimmung einholen.
