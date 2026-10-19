---
id: "115"
title: "Aktionsverlauf"
wave: "W26"
depends_on: [017, 018, 105, 107]
files: [tasks/115-command-audit-log.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "c4a8ee0f8da0ae74"
---
# Aufgabe 115 — Aktionsverlauf

## Ziel
Nutzer kann nachvollziehen, welche Befehle und Werkzeugaktionen ausgeführt wurden.

## Ergebnis
Lokale Protokollpunkte mit Zeitpunkt, Zweck, Status und Freigabe.

## Fertig, wenn
- Verlauf exportierbar/löschbar und lokal datensparsam ist.
- Inhalte nicht unnötig vollständig aufgezeichnet werden.

## Schutz
Schlüssel und sensible Befehlsparameter werden maskiert.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Privacy-Logging-/Android-Sicherheits-Skill suchen; Quelle/Lizenz prüfen und Installation nur nach Zustimmung.
