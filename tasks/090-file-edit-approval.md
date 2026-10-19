---
id: "090"
title: "Änderungen freigeben"
wave: "W19b"
depends_on: [088, 089, 119, 120]
files: [tasks/090-file-edit-approval.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "46338e4949663cb9"
---
# Aufgabe 090 — Änderungen freigeben

## Ziel
Nutzer nimmt vorgeschlagene Dateiänderungen bewusst an oder verwirft sie.

## Ergebnis
Einzeln-/Sammelannahme, klare betroffene Dateien und Ergebnisbestätigung.

## Fertig, wenn
- Ablehnen ursprünglichen Inhalt erhält.
- Teilfreigabe nicht abgelehnte Änderungen speichert.

## Schutz
Aktion vor Speichern nochmals auf aktuelle Datei prüfen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Dateiänderungs-/Freigabe-UX-Skill suchen; Quelle/Lizenz prüfen und Nutzer vor Installation fragen.
