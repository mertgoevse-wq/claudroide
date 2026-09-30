---
id: "045"
title: "Schlüssel sicher speichern"
wave: "W10"
depends_on: [005, 016, 017]
files: [tasks/045-secret-storage.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "93457831ef658b93"
---
# Aufgabe 045 — Schlüssel sicher speichern

## Ziel
Nutzer-Schlüssel lokal so speichern, dass sie nicht als Klartextdatei herumliegen.

## Ergebnis
Konzept für Android-geschützte Schlüsselablage, Entsperrung, Rotation und Wiederherstellung.

## Fertig, wenn
- Bedrohungsmodell und Android-Versionen geprüft sind.
- Schlüssel beim Export/Sichern standardmäßig ausgeschlossen oder geschützt sind.

## Schutz
Keine Übertragung an Claudroide-Server oder Protokolle.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Kryptografie-Skill suchen und Quellcode, Bibliothekslizenz und Risiken prüfen; Installation mit Nutzer abstimmen.
