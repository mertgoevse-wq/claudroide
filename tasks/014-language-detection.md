---
id: "014"
title: "Sprache automatisch erkennen"
wave: "W5"
depends_on: [010]
files: [tasks/014-language-detection.md]
skills: [`adaptive`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "038bcb04e948e94a"
---
# Aufgabe 014 — Sprache automatisch erkennen

## Ziel
Deutsch oder Englisch anhand der bevorzugten Handysprache wählen.

## Ergebnis
Sprachzuordnung mit klarer Rückfallregel, wenn Android eine nicht unterstützte Sprache meldet.

## Fertig, wenn
- Die App auf Deutsch und Englisch dieselben Abläufe anbietet.
- Automatische Auswahl eine manuelle Wahl nicht überschreibt.

## Schutz
Spracheinstellung nur lokal verwenden; keine unnötige Geräteinformation übertragen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Lokalisierungs-Skill suchen, Lizenz und Barrierefreiheit prüfen; Nutzer vor Installation fragen.
