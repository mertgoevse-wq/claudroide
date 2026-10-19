---
id: "072"
title: "Agentenwerkzeuge verbinden"
wave: "W16"
depends_on: [070, 071]
files: [tasks/072-agent-tool-loop.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "5fb9794d22c15878"
---
# Aufgabe 072 — Agentenwerkzeuge verbinden

## Ziel
Modellantworten sicher mit freigegebenen Datei-, Such- und Testwerkzeugen verknüpfen.

## Ergebnis
Ablauf mit Werkzeugprüfung, Berechtigung, Ergebnis und Fortsetzung.

## Fertig, wenn
- Werkzeugaufrufe auf zugelassene Aktionen beschränkt sind.
- Fehler/Abbruch nicht als Erfolg dargestellt werden.

## Schutz
Jede Werkzeugaktion wird vor Ausführung gegen Projektgrenzen und Freigaben geprüft.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Agent-Tool-Use-Skill suchen, Quelle/Lizenz und Rechte prüfen; Nutzer vor Installation fragen.
