---
id: "061"
title: "Modellfähigkeiten"
wave: "W13"
depends_on: [043, 055]
files: [tasks/061-model-capability-labels.md]
skills: [`/claude-api`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "aa43c4c947acc8d0"
---
# Aufgabe 061 — Modellfähigkeiten

## Ziel
Anzeigen, welche Aufgaben ein Modell nachweislich unterstützt.

## Ergebnis
Kennzeichen für Text, Bilder, Werkzeuge, Streaming und weitere bestätigte Funktionen samt Quelle.

## Fertig, wenn
- Nicht unterstützte Funktion vor einer Aktion erklärt wird.
- Fähigkeiten nicht aus dem Modellnamen abgeleitet werden.

## Schutz
Keine Datei an ein Modell senden, das sie nicht verarbeiten kann, ohne Nutzerhinweis.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Provider-Modellfähigkeiten-Skill suchen, Quelle und Lizenz prüfen; Nutzer vor Installation fragen.
