---
id: "046"
title: "Geheimnisse verbergen"
wave: "W11"
depends_on: [044, 045]
files: [tasks/046-secret-redaction.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "3fecdfaa6f6fdbad"
---
# Aufgabe 046 — Geheimnisse verbergen

## Ziel
API-Schlüssel und sensible Zugänge in Logs, Fehlern, Chat-Ausgaben und Exports verhindern.

## Ergebnis
Zentrale Maskierung, Testfälle mit Beispielmustern und sichere Diagnosemeldungen.

## Fertig, wenn
- Testschlüssel in keinem Protokoll oder Export im Klartext erscheinen.
- Maskierung nicht fälschlich als vollständige Sicherheitsgarantie gilt.

## Schutz
Keine echten Nutzer-Schlüssel in Tests oder Aufgabenbeispielen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Secret-Scanning-/Sicherheits-Skill suchen; Quelle, Lizenz und Fehlerrisiken prüfen, Nutzer vor Installation fragen.
