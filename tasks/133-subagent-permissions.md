---
id: "133"
title: "Helferrechte"
wave: "W28"
depends_on: [070, 117, 119, 122, 132]
files: [tasks/133-subagent-permissions.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "064e014de444c1ee"
---
# Aufgabe 133 — Helferrechte

## Ziel
Jeder Spezialhelfer bekommt nur die Werkzeuge und Projektteile seiner Aufgabe.

## Ergebnis
Rechtematrix für Lesen, Schreiben, Befehle, Netzwerk, Anbieter und gemeinsame Daten.

## Fertig, wenn
- Schreibrechte standardmäßig nicht an reine Prüfer gehen.
- Hauptagent Rechte nicht still erweitert.

## Schutz
Helfer können keine Nutzerbestätigung oder App-Sicherheitsregel umgehen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Least-Privilege-Agent-Skill suchen; Quelle, Lizenz und Grenzen prüfen und Installation durch Nutzer bestätigen lassen.
