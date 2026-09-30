---
id: "106"
title: "Arbeitsordner begrenzen"
wave: "W25"
depends_on: [105, 119, 120]
files: [tasks/106-command-working-directory.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "04817033fe63018c"
---
# Aufgabe 106 — Arbeitsordner begrenzen

## Ziel
Befehle nur im ausdrücklich freigegebenen Projektbereich ausführen.

## Ergebnis
Arbeitsordnerprüfung, Pfadauflösung und sichere Behandlung von Unterordnern.

## Fertig, wenn
- Relative und veränderte Pfade nicht aus dem erlaubten Bereich entkommen.
- Nicht les-/schreibbare Android-Dateianbieter klar gemeldet werden.

## Schutz
Root- und Systembereiche bleiben gesperrt.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Sandbox-/Pfadsicherheits-Skill suchen; Quelle/Lizenz prüfen und vor Installation Zustimmung einholen.
