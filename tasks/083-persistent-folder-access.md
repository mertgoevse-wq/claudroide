---
id: "083"
title: "Ordnerzugriff merken"
wave: "W19"
depends_on: [082]
files: [tasks/083-persistent-folder-access.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "46e06056f7285df9"
---
# Aufgabe 083 — Ordnerzugriff merken

## Ziel
Nutzer kann gewählte Projektordner nach Neustart weiterverwenden und Zugriff widerrufen.

## Ergebnis
Berechtigungsstatus, Wiederherstellung und Fehler bei ungültigem Zugriff.

## Fertig, wenn
- Widerruf sofort angezeigt und Schreiben angehalten wird.
- App keine gelöschten oder verschobenen Ordner still ersetzt.

## Schutz
Zugriff bleibt auf vom Nutzer gewährten URI beschränkt.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Dateiberechtigungs-Skill suchen; Quelle und Lizenz prüfen, vor Installation fragen.
