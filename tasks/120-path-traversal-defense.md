---
id: "120"
title: "Pfadschutz"
wave: "W24"
depends_on: [017, 045]
files: [tasks/120-path-traversal-defense.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "c767fb22b03daaf1"
---
# Aufgabe 120 — Pfadschutz

## Ziel
Bösartige oder fehlerhafte Pfade außerhalb des erlaubten Projektordners verhindern.

## Ergebnis
Tests für relative Pfade, Unicode, Sonderzeichen, Umleitungen und Dateianbieter.

## Fertig, wenn
- Kein Werkzeug auf fremde App-/Systemdateien zugreifen kann.
- Unsicherer Pfad abgelehnt und verständlich gemeldet wird.

## Schutz
Prüfung auf tatsächlichem Ziel, nicht nur auf geschriebenem Pfad.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Pfad-/Dateisicherheits-Skill suchen; Quelle/Lizenz und Angriffsgrenzen prüfen, Nutzer vor Installation fragen.
