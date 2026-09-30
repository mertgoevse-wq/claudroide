---
id: "012"
title: "Online-Bau vom Handy aus"
wave: "W4"
depends_on: [008]
files: [tasks/012-cloud-build-path.md]
skills: [`/swarm-planner`, `android-permissions-security`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "463fb2851320bf82"
---
# Aufgabe 012 — Online-Bau vom Handy aus

## Ziel
Einen Cloud-Bau nur dann vorsehen, wenn er die Telefon-only-Anforderung besser erfüllt.

## Ergebnis
Vergleich geprüfter Bauwege mit privaten Repository-Rechten, Kosten, Datenverarbeitung, Build-Logs, APK-Signierung und Löschbarkeit.

## Fertig, wenn
- Anbieter und Datenschutzbedingungen vorgelegt und Nutzerentscheidung eingeholt sind.
- APK-Quelle und Signatur nachprüfbar sind.

## Schutz
Kein Repository oder Quellcode an fremden Dienst übertragen, bevor Nutzer das Ziel bestätigt.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen CI-/Android-Build-Skill suchen; Anbieterzugriffe, Lizenz und Datenschutz prüfen und Nutzer vor Installation fragen.
