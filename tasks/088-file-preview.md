---
id: "088"
title: "Dateien ansehen"
wave: "W18"
depends_on: [010, 017]
files: [tasks/088-file-preview.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "e0a6f6c167f824a0"
---
# Aufgabe 088 — Dateien ansehen

## Ziel
Text und ausdrücklich unterstützte Dateien vor Änderungen lesbar anzeigen.

## Ergebnis
Vorschau mit Dateityp, Zeichensatz, Größe und nicht unterstützten Inhalten.

## Fertig, wenn
- Binärdateien nicht fälschlich als lesbarer Text gezeigt werden.
- Große Dateien begrenzt und bedienbar bleiben.

## Schutz
Vorschau führt keine Skripte oder eingebetteten Inhalte aus.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen sicheren Datei-Viewer-/Android-Skill suchen; Herkunft/Lizenz prüfen und Nutzer vor Installation fragen.
