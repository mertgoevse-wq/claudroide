---
id: "068"
title: "Nur nötigen Projektkontext wählen"
wave: "W14"
depends_on: [039, 042, 062, 067, 045]
files: [tasks/068-minimal-context-selection.md]
skills: [`/claude-api`, `android-permissions-security`]
status: done
gate: true
done_since_last_edit: true
content-hash: "a8406394ddf21774"
---
# Aufgabe 068 — Nur nötigen Projektkontext wählen

## Ziel
Relevante Dateien für die Aufgabe finden, ohne den ganzen Projektordner zu übertragen.

## Ergebnis
Dateisuche, begrenzte Ausschnitte, Quellenanzeige und Erweiterung nur wenn nötig.

## Fertig, wenn
- Jede an den Anbieter gesendete Datei benennbar ist.
- Kontextgrenzen nicht durch stilles Vollprojektladen umgangen werden.

## Schutz
Geheimnisfilter wird vor Kontextversand angewendet.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Codebase-Kontext-/Privatsphäre-Skill suchen, Quelle/Lizenz prüfen und Nutzer vor Installation fragen.
