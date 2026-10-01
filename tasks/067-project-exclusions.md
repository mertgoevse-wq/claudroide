---
id: "067"
title: "Projekt-Ausschlüsse"
wave: "W13"
depends_on: [043, 045, 055]
files: [tasks/067-project-exclusions.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "8de80cab42854c8b"
---
# Aufgabe 067 — Projekt-Ausschlüsse

## Ziel
Geheimnisse, Umgebungsdateien, Schlüssel und schwere Ordner von KI-Anfragen ausschließen.

## Ergebnis
Voreinstellungen, projektbezogene Regeln und Ausschlussvorschau mit manueller Ausnahme.

## Fertig, wenn
- Ausschlüsse bei Suche, Kontextaufbau und Agent-Werkzeugen gelten.
- Fehlende Dateien in verständlicher Form erklärt werden.

## Schutz
Keine geheime Datei automatisch als Kontext senden.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Secret-Scanning-/Projektfilter-Skill suchen; Quelle, Lizenz und Risiken prüfen und Nutzer vor Installation fragen.
