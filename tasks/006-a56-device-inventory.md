---
id: "006"
title: "A56-Gerätebestand"
wave: "W0"
depends_on: []
files: [tasks/006-a56-device-inventory.md]
skills: [`android-profiler`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "f101402be6b852d6"
---
# Aufgabe 006 — A56-Gerätebestand

## Ziel
Die Spezifikation mit echten Daten des Galaxy A56 des Nutzers abgleichen.

## Ergebnis
Ein datensparsamer Testbogen: Modellkennung, Android-Version, Arbeitsspeicher, freier interner Speicher, verfügbarer USB-Speicher und relevante Energieeinstellungen.

## Fertig, wenn
- Nutzer die Werte selbst aus Einstellungen bestätigt oder Felder als offen markiert.
- Keine Hardwarewerte aus einer anderen Länder- oder Speicher-Variante übernommen werden.

## Schutz
Keine Seriennummer, IMEI oder unnötige Gerätekennung speichern.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Geräte-Test-Skill suchen und dessen Quelle, Lizenz und Datenerhebung prüfen; vor Installation Zustimmung einholen.
