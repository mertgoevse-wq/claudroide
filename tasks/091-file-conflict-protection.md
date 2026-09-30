---
id: "091"
title: "Datei-Konflikte"
wave: "W19"
depends_on: [082, 088, 089]
files: [tasks/091-file-conflict-protection.md]
skills: [`testing-setup`, `android-permissions-security`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "72a2db437aec48bf"
---
# Aufgabe 091 — Datei-Konflikte

## Ziel
Änderungen nicht über externe oder zwischenzeitliche Dateiänderungen schreiben.

## Ergebnis
Versionsvergleich, Konfliktmeldung und kontrollierter Zusammenführungsweg.

## Fertig, wenn
- Änderung bei abweichendem Ausgangsstand pausiert.
- Nutzer die Unterschiede vor dem Zusammenführen sieht.

## Schutz
Keine stillen Überschreibungen oder automatische Datenverluste.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Git-/Dateikonflikt-Skill suchen; Quelle, Lizenz und Sicherheitswirkung prüfen, Nutzer vor Installation fragen.
