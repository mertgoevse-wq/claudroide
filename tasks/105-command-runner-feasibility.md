---
id: "105"
title: "Befehle auf Android prüfen"
wave: "W23"
depends_on: [007, 008, 010, 017]
files: [tasks/105-command-runner-feasibility.md]
skills: [`android-permissions-security`, `android-profiler`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "e23d8668bfadcbaf"
---
# Aufgabe 105 — Befehle auf Android prüfen

## Ziel
Klären, wie Tests und Projektbefehle ohne separate Termux-App sicher laufen könnten.

## Ergebnis
Machbarkeitsbericht für Android-eigene Prozesse, eingebettete Laufzeit oder andere erlaubte Wege.

## Fertig, wenn
- Unterstützte Sprachen und Befehle, Speicher, Lizenz und Grenzen belegt sind.
- Kein beliebiger Zugriff außerhalb des Projekts angenommen wird.

## Schutz
Keine gefährliche Shell ohne Begrenzung/Bestätigung ausführen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Prozess-/Sandbox-Skill suchen; Quelle, Lizenz und Berechtigungen prüfen, Nutzer vor Installation fragen.
