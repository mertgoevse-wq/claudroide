---
id: "128"
title: "Sicherheitstests"
wave: "W29"
depends_on: [045, 046, 059, 067, 090, 100, 101, 106, 108, 119, 120, 122, 127, 134, 135]
files: [tasks/128-security-test-suite.md]
skills: [`testing-setup`, `android-permissions-security`]
status: done
gate: false
done_since_last_edit: true
content-hash: "50bfefcd5a59e8d6"
---
# Aufgabe 128 — Sicherheitstests

## Ziel
Kritische Sicherheitsregeln vor jeder nutzbaren Freigabe prüfen.

## Ergebnis
Tests für Schlüssel, Pfade, Freigaben, Netzwerk, Import, Git, Abbruch und bösartige Inhalte.

## Fertig, wenn
- Fehlerfälle dokumentiert und reproduzierbar sind.
- Nicht bestandene Schutztests die Freigabe blockieren.

## Schutz
Tests verwenden ausschließlich erfundene Schlüssel und harmlose Testprojekte.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Mobile-App-Security-Test-Skill suchen; Quelle, Lizenz und Prüfmethoden überprüfen und Nutzer vor Installation fragen.
