---
id: "094"
title: "Herkunft von Projektregeln"
wave: "W19"
depends_on: [082, 093]
files: [tasks/094-project-instruction-trust.md]
skills: [`android-permissions-security`, `/code-review`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "5aaa448acdb00d1f"
---
# Aufgabe 094 — Herkunft von Projektregeln

## Ziel
Nutzer erkennt, wer eine Anweisung bereitgestellt hat und welche Wirkung sie hat.

## Ergebnis
Vertrauensstatus für Projekt, Datei, Skill und externe Quelle.

## Fertig, wenn
- Neue Anweisungen keine Freigaben automatisch erhöhen.
- Nutzer Regeln ansehen, ausschließen und widerrufen kann.

## Schutz
Bösartige Projekttexte dürfen nicht unbemerkt Rechte erweitern.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Prompt-Injection-/Projektvertrauens-Skill suchen; Quelle/Lizenz prüfen und vor Installation Zustimmung holen.
