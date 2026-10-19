---
id: "056"
title: "Schlüssel wechseln"
wave: "W11"
depends_on: [044, 045]
files: [tasks/056-provider-key-rotation.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "a9725e3291c3cc89"
---
# Aufgabe 056 — Schlüssel wechseln

## Ziel
Nutzer kann einen Schlüssel ersetzen und den alten aus lokalem Speicher entfernen.

## Ergebnis
Ablauf zum Hinzufügen, Testen, Aktivieren und Löschen eines Schlüssels.

## Fertig, wenn
- Fehler beim neuen Schlüssel den alten nicht still überschreiben.
- Nutzer eine sichere Bestätigung zum Entfernen erhält.

## Schutz
Schlüsselwerte erscheinen nie in Verlauf oder Bestätigungsdialog.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Credential-Management-Skill suchen, Quelle/Lizenz und Risiken prüfen; vor Installation Zustimmung einholen.
