---
id: "124"
title: "Skill-Installation freigeben"
wave: "W27"
depends_on: [001, 002, 017, 123, 130]
files: [tasks/124-skill-install-approval.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "8eabb85966784975"
---
# Aufgabe 124 — Skill-Installation freigeben

## Ziel
Nutzer sieht, was ein gefundener Skill kann und welche Dateien/Rechte er erhält.

## Ergebnis
Bestätigungsdialog mit Quelle, Lizenz, Umfang, Installationsort und Entfernen.

## Fertig, wenn
- Jede globale Installation einzeln bestätigt wird.
- Ablehnen keine andere Fähigkeit automatisch installiert.

## Schutz
Keine ungefragte globale Änderung am Nutzergerät.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen sicheren Skill-Installer-/Package-Review-Skill suchen; Quelle/Lizenz prüfen und Nutzer vor Installation fragen.
