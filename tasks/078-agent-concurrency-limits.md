---
id: "078"
title: "Parallelität begrenzen"
wave: "W16"
depends_on: [070, 071]
files: [tasks/078-agent-concurrency-limits.md]
skills: [`android-profiler`, `parallel-task`]
status: done
gate: false
done_since_last_edit: true
content-hash: "a489448769a7e8b5"
---
# Aufgabe 078 — Parallelität begrenzen

## Ziel
Parallele Arbeit nur nutzen, wenn sie dem A56 und dem Nutzerauftrag angemessen ist.

## Ergebnis
Einstellbares Limit, Ressourcenmessung und verständliche Pauseoption.

## Fertig, wenn
- Gleichzeitige Dateiänderungen sich nicht gegenseitig überschreiben.
- Knappes RAM/Akku den Nutzer warnt oder Arbeit verlangsamt.

## Schutz
Keine automatische große Modellladung im Hintergrund.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Ressourcen-/Agenten-Skill suchen; Quelle/Lizenz prüfen und Zustimmung vor Installation einholen.
