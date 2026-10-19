---
id: "131"
title: "Globalen Skill installieren"
wave: "W27"
depends_on: [001, 002, 017, 124]
files: [tasks/131-skill-global-install-workflow.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "41285de246ac5d76"
---
# Aufgabe 131 — Globalen Skill installieren

## Ziel
Nur geprüfte Fähigkeiten nach ausdrücklicher Zustimmung am gewünschten Ort hinzufügen.

## Ergebnis
Installationsvorschau, Sicherung vorhandener Dateien, Ergebnisprüfung und Entfernen.

## Fertig, wenn
- Nutzer die genaue Quelle und den globalen Geltungsbereich sieht.
- Keine vorhandene Fähigkeit ungefragt überschrieben wird.

## Schutz
Installation ändert Dateien und benötigt deshalb ausdrückliche Zustimmung.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen sicheren globalen-Installer-Skill suchen; Herkunft/Lizenz prüfen und vor Installation Nutzerfreigabe einholen.
