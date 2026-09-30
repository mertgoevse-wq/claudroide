---
id: "041"
title: "Anbieter und Kosten anzeigen"
wave: "W13c"
depends_on: [043, 055, 061]
files: [tasks/041-cost-and-model-labels.md]
skills: [`adaptive`, `/code-review`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "c2d66512dd99bba5"
---
# Aufgabe 041 — Anbieter und Kosten anzeigen

## Ziel
Vor und nach einer Anfrage sichtbar machen, welches Modell verwendet wird.

## Ergebnis
Anbietername, Modellname und nur verfügbare Verbrauchs-/Preisdaten mit Zeitstempel darstellen.

## Fertig, wenn
- Schätzwerte als solche gekennzeichnet sind.
- Unbekannte Kosten nicht als kostenlos ausgegeben werden.

## Schutz
Kein Geheimnis in Statusanzeige oder Diagnosedaten.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Kosten-/Provider-UX-Skill suchen, Lizenz und Quelle prüfen; Nutzer vor Installation fragen.
