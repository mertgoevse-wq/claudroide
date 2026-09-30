---
id: "113"
title: "Lange Aufgabe melden"
wave: "W26"
depends_on: [017, 018, 105, 107]
files: [tasks/113-long-task-notification.md]
skills: [`android-permissions-security`, `android-profiler`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "5d23864d6c8d889a"
---
# Aufgabe 113 — Lange Aufgabe melden

## Ziel
Nutzer erkennen und stoppen können, wenn eine von ihnen gestartete Aufgabe weiterläuft.

## Ergebnis
Benachrichtigung mit sinnvoller Statusinformation und Stop-Aktion, falls Android dies verlangt.

## Fertig, wenn
- Zielversion, Berechtigungen und Diensttyp geprüft sind.
- Keine privaten Chat- oder Codeinhalte auf Sperrbildschirm erscheinen.

## Schutz
Keine dauerhafte Hintergrundarbeit ohne aktiven Nutzerauftrag.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Foreground-Service-Skill suchen, offizielle Quelle/Lizenz prüfen und Nutzer vor Installation fragen.
