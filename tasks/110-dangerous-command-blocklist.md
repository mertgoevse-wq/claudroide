---
id: "110"
title: "Gefährliche Befehle erkennen"
wave: "W25"
depends_on: [105, 119, 120]
files: [tasks/110-dangerous-command-blocklist.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "b153964b32c9417b"
---
# Aufgabe 110 — Gefährliche Befehle erkennen

## Ziel
Bekannte riskante Befehle frühzeitig anhalten und erklären.

## Ergebnis
Prüfregeln für Löschen, Systemzugriff, Downloads, Schlüsselzugriff und Datenversand.

## Fertig, wenn
- Regeln mit harmlosen und gefährlichen Beispielen getestet sind.
- Liste nicht als alleinige Sicherheitsbarriere gilt.

## Schutz
Echte Rechtebegrenzung ist wichtiger als Mustererkennung.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Kommando-Sicherheits-Skill suchen; Quelle, Lizenz und False-negative-Grenzen prüfen, Nutzer vor Installation fragen.
