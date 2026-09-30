---
id: "082"
title: "Android-Ordner auswählen"
wave: "W18"
depends_on: [010, 017]
files: [tasks/082-android-folder-picker.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "5daba620d9887b71"
---
# Aufgabe 082 — Android-Ordner auswählen

## Ziel
Projektordner ausdrücklich über Androids Systemdialog freigeben lassen.

## Ergebnis
Ordnerwahl, leere/ungültige Auswahl, erneute Auswahl und verständliche Erklärung.

## Fertig, wenn
- App nur auf den ausgewählten Bereich zugreift.
- Abbrechen keine Berechtigung erzeugt.

## Schutz
Keine umfassende Speicherberechtigung statt Systemauswahl anfordern.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Storage-Framework-Skill suchen; offizielle Dokumentation und Lizenz prüfen, Installation nur nach Nutzerbestätigung.
