---
id: "052"
title: "Eigener Endpunkt"
wave: "W11"
depends_on: [044, 045]
files: [tasks/052-custom-endpoint.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "864bbfc9bcc9b7da"
---
# Aufgabe 052 — Eigener Endpunkt

## Ziel
Nutzer kann eigene Serveradresse, Format, Modell und erforderliche Kopfdaten einstellen.

## Ergebnis
Prüfbares Konfigurationsformular samt Verbindungstest und verständlichen Fehlermeldungen.

## Fertig, wenn
- Nutzer vor jeder Anfrage die Zieladresse sieht.
- Inkompatible Formate oder Fähigkeiten sichtbar markiert werden.

## Schutz
Unsicheres HTTP nicht still akzeptieren; Geheimnisse niemals an falsche Hostnamen senden.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Custom-API-Endpoint-Skill suchen und dessen Netzwerk-/Schlüsselrisiken prüfen; Nutzerfreigabe für Installation einholen.
