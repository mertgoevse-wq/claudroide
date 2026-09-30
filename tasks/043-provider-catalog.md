---
id: "043"
title: "Anbieter-Katalog"
wave: "W10"
depends_on: [005, 016, 017]
files: [tasks/043-provider-catalog.md]
skills: [`/swarm-planner`, `/claude-api`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "042c37b5f163e310"
---
# Aufgabe 043 — Anbieter-Katalog

## Ziel
Nutzern geprüfte Anbieter und frei konfigurierbare Anschlüsse verständlich anbieten.

## Ergebnis
Eintrag je Anbieter mit offizieller Quelle, erlaubter Authentifizierung, Format, Fähigkeiten und letztem Prüfdatum.

## Fertig, wenn
- Veraltete/unbestätigte Einträge deaktivierbar sind.
- Anbieter nicht als offiziell unterstützt erscheinen, wenn der Weg nicht belegt ist.

## Schutz
Keine Zugangsdaten oder nicht freigegebenen Tokenwege katalogisieren.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Provider-Integration-Skill suchen, Herkunft/Lizenz und Authentifizierung prüfen; Nutzer vor Installation fragen.
