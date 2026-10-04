---
id: "100"
title: "Geheimnisse vor Git finden"
wave: "W21"
depends_on: [045, 095, 096]
files: [tasks/100-git-secret-scan.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "29a051245a871e11"
---
# Aufgabe 100 — Geheimnisse vor Git finden

## Ziel
Unbeabsichtigte Zugangsdaten und private Schlüssel vor Veröffentlichung erkennen.

## Ergebnis
Lokaler Scan, Trefferanzeige ohne Geheimniswert und sichere Ausschluss-/Entfernungshilfe.

## Fertig, wenn
- Bekannte Muster und Fehlalarme testbar sind.
- Treffer den Commit standardmäßig blockieren.

## Schutz
Treffer werden nicht an einen Online-Scanner gesendet.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Git-Secret-Scanning-Skill suchen; Quelle, Lizenz und lokale Datenverarbeitung prüfen, Nutzer vor Installation fragen.
