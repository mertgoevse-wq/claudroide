---
id: "038"
title: "Stoppen und Wiederholen"
wave: "W9"
depends_on: [031, 035]
files: [tasks/038-stop-retry-and-continue.md]
skills: [`testing-setup`, `android-permissions-security`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "d3c481d3a2d27bb5"
---
# Aufgabe 038 — Stoppen und Wiederholen

## Ziel
Nutzer steuern laufende oder unvollständige Antworten sicher.

## Ergebnis
Stop-, Retry- und Continue-Aktionen mit klarer Information über neue Anfrage und mögliche Kosten.

## Fertig, wenn
- Wiederholen eine neue Anfrage nur nach erkennbarem Nutzerbefehl sendet.
- Stoppen auch an Agent-Werkzeuge weitergegeben wird.

## Schutz
Bereits ausgeführte Datei- oder Netzwerkaktionen werden nicht automatisch rückgängig behauptet.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Agent-Abbruch-/Retry-Skill suchen, Quelle/Lizenz prüfen und Nutzer vor Installation fragen.
