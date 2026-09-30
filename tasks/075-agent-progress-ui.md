---
id: "075"
title: "Fortschritt anzeigen"
wave: "W16"
depends_on: [070, 071]
files: [tasks/075-agent-progress-ui.md]
skills: [`adaptive`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "ee3ac41a0fdbe302"
---
# Aufgabe 075 — Fortschritt anzeigen

## Ziel
Nutzer jederzeit wissen lassen, was der Agent gerade macht und worauf er wartet.

## Ergebnis
Aktuelle Phase, letzte Aktion, Zustimmung nötig, Abbruch und Abschluss anzeigen.

## Fertig, wenn
- Fortschritt nicht vorgibt, eine unbekannte Aufgabe sei abgeschlossen.
- Wartezeiten mit Stop-/Abbruchmöglichkeit verbunden sind.

## Schutz
Keine privaten Tool-Ausgaben ungewollt in Benachrichtigungen zeigen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Mobile-Fortschritts-/UX-Skill suchen; Lizenz und Quelle prüfen, vor Installation Nutzer fragen.
