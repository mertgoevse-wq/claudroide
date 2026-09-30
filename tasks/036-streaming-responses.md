---
id: "036"
title: "Laufende Antworten"
wave: "W9b"
depends_on: [031, 035, 047, 052, 059]
files: [tasks/036-streaming-responses.md]
skills: [`/claude-api`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "053d2332bcddd03f"
---
# Aufgabe 036 — Laufende Antworten

## Ziel
Antworten während des Empfangs fortlaufend darstellen.

## Ergebnis
Status, Zwischenstand, Ende, Abbruch und Wiederverbindung sauber unterscheiden.

## Fertig, wenn
- Abgebrochene Antworten nicht als vollständig markiert werden.
- Netzwerkfehler weder Inhalt duplizieren noch neue Kosten still auslösen.

## Schutz
Nur den ausdrücklich gewählten Anbieter kontaktieren; Übertragungsfehler erklären.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Streaming-API-/Android-Chat-Skill suchen; aktuelle Quelle/Lizenz prüfen und Nutzerfreigabe vor Installation einholen.
