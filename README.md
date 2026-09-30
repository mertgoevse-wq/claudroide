# Claudroide

Claudroide ist ein frühes Konzept für eine eigenständige Android-App zur KI-gestützten Projektarbeit. Zielgerät ist zunächst das Samsung Galaxy A56. Dieses Repository enthält aktuell die Spezifikation und den Bauplan – noch keine App.

**Logo:** Noch nicht gerendert. Das gewünschte PNG wird erst erstellt, wenn Claude Media Bridge mit Nano Banana Pro/2 tatsächlich verfügbar ist. Das eigenständige Motiv und der Bildauftrag stehen in [`assets/logo-brief.md`](assets/logo-brief.md). Kein SVG-Ersatz.

## Dokumente

- [`claudroide-spec.md`](claudroide-spec.md) — Produktziele, Grenzen, Datenschutz, Bedienabläufe, offene Entscheidungen und 135 Aufgaben.
- [`tasks/`](tasks/) — 135 einzeln nummerierte Aufgabenpläne plus Skill-Zuordnung und Abhängigkeitsgraph.
- [`tasks/DEPENDENCIES.md`](tasks/DEPENDENCIES.md) — Abhängigkeiten und mögliche parallele Arbeitswellen.
- [`tasks/skill-matrix.md`](tasks/skill-matrix.md) — zwei konkrete Skills pro Aufgabe, Quellen und Installationsstatus.
- [`CLAUDE.md`](CLAUDE.md) — Regeln für Claude Code in diesem Projekt.
- [`progress/BUILD-STATE.md`](progress/BUILD-STATE.md) — letzter verifizierter Arbeitsstand.
- [`.claude/skills/claudroide-resume/SKILL.md`](.claude/skills/claudroide-resume/SKILL.md) — `/claudroide-resume` zur Wiederaufnahme nach Unterbrechung.
- [`assets/logo-brief.md`](assets/logo-brief.md) — Bildauftrag für den späteren Nano-Banana-Entwurf.

## Weiterarbeiten nach einem Abbruch

Projektordner in Claude Code öffnen und `/claudroide-resume` eingeben. Der Skill prüft den letzten Eintrag in `progress/BUILD-STATE.md` gegen die Dateien und den Git-Stand. Er arbeitet nur freigegebene, abhängigkeitsfreie Aufgaben weiter ab und hält bei offenen Entscheidungen, Risiken und vor Uploads an. Damit die Wiederaufnahme funktioniert, muss der Checkpoint nach jedem abgeschlossenen Aufgabenblock aktualisiert werden.

## Parallelisierung

Aufgaben dürfen in Wellen parallelisiert werden, wenn alle Vorgänger geprüft sind und die Agenten keine gemeinsamen Dateien bearbeiten. `tasks/DEPENDENCIES.md` enthält die Arbeitswellen; `tasks/skill-matrix.md` gibt für jeden Task zwei Skills vor. Eine leitende Sitzung führt Änderungen zusammen, prüft Tests und aktualisiert den Checkpoint. Parallel-Agenten committen oder pushen nicht.

## Sicherheits- und Produktgrenzen

- Eigenständiger Name und eigenständige Bildsprache; keine Claude-Code- oder Anthropic-Marken nachahmen.
- Claude in Claudroide ist für den Start über einen eigenen Claude-API-Schlüssel vorgesehen. Kein Vermitteln eines Claude-Abo-Logins und kein Token-Relay.
- Anbieter, Endpunkte, Schlüssel und Kosten transparent halten; fremde Skills und zusätzliche Tools vor Nutzung prüfen.
- NPU, lokale KI, Befehlsausführung und Bau direkt am A56 sind Machbarkeitspunkte, keine schon belegten Zusagen.
- Dieses Repository ist für den privaten Anfang vorgesehen. Keine öffentliche Sichtbarkeit oder Lizenz wird vorausgesetzt.

## Status

**Entwurfsphase.** Noch kein Android-App-Code und keine installierbare App. Gerätevariante, Android-Version, Speicher/RAM, finale Lizenz und zulässige Anbieterwege müssen vor der Umsetzung bestätigt werden.
