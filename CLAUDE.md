# Claudroide — Anweisungen für Claude Code

Lies vor jeder Umsetzung `claudroide-spec.md`, `tasks/skill-matrix.md` und `progress/BUILD-STATE.md`. Nutzerwünsche und ausdrückliche Freigaben haben Vorrang; erfinde keine offenen Geräte-, Rechts-, Kosten- oder Anbieterangaben.

## Unterbrochene Arbeit fortsetzen

Gib in Claude Code `/claudroide-resume` ein, um die gespeicherte Arbeit fortzusetzen. Der projektgebundene Skill liegt unter `.claude/skills/claudroide-resume/SKILL.md`.

Vor einer Unterbrechung oder nach jedem abgeschlossenen Aufgabenblock aktualisiere `progress/BUILD-STATE.md`: erledigte IDs, laufende IDs, nächste freigegebene Arbeit, betroffene Dateien, Tests, Hindernisse und letzte sichere Git-Referenz. Nach einem App-/Termux-Absturz starte Claude Code erneut im Projektordner und rufe `/claudroide-resume` auf. Wenn kein aktueller Checkpoint existiert, prüft der Skill zuerst Git-Stand und Dateien; er rät nicht, was erledigt wurde.

## Skills sind Teil der Aufgabe

Vor jeder Aufgabe lade **beide** in `tasks/skill-matrix.md` dafür zugeordneten Skills, sofern diese installiert und mit der aktuellen Claude-Code-Version verfügbar sind. Die sechs geprüften globalen Skills sind `swarm-planner`, `parallel-task`, `adaptive`, `android-profiler`, `android-permissions-security` und `testing-setup`. Verfügbarkeit prüfen, nicht bloß Namen behaupten. Fehlt ein zugewiesener Skill, recherchiere einen Ersatz und dokumentiere Quelle, Lizenz und Sicherheitsbefund. Keine weitere fremde Fähigkeit global installieren, bevor der Nutzer den konkreten Kandidaten bestätigt.

## Parallel arbeiten

- Nutze `/swarm-planner`, um Abhängigkeiten und unabhängige Aufgaben zu bestimmen, und `/parallel-task`, um nur freigegebene Aufgaben in Wellen parallel auszuführen.
- Parallelisiere nur Aufgaben ohne gemeinsame Dateien, ungeklärte Vorentscheidungen oder gemeinsame externe Nebenwirkungen. Jede parallele Arbeit erhält getrennte Dateien oder einen eigenen Git-Worktree.
- Architektur-, Sicherheits-, Provider- und Datenmodell-Entscheidungen sind Sperrpunkte: erst prüfen und festhalten, dann abhängige Implementierungen starten.
- Ein Agent pro Datei/Änderungsbereich. Keine zwei Agenten gleichzeitig dieselbe Datei bearbeiten lassen. Eine leitende Sitzung prüft Zusammenführung, Tests, Sicherheitsregeln und Checkpoint.
- Commits nur für geprüfte, abgeschlossene Aufgabenblöcke. Vor Commit Schlüssel scannen. Push ausschließlich an das verifizierte private Repository dieses Projekts; bei abweichender Sichtbarkeit, unbekanntem Remote oder öffentlichem Ziel anhalten und fragen.

## Qualitätsregeln

- Nutzertexte in einfachem Deutsch; technische Begriffe kurz erklären.
- Keine AI-Slop-Texte: konkret, knapp, belegt; keine leeren Werbeversprechen, künstlichen Superlative, generischen Füllabschnitte oder erfundenen Kennzahlen.
- Keine Claude-/Anthropic-Marken oder -Logos nachahmen. Keine Claude-Abo-Anmeldung vermitteln; Claude API nur gemäß aktuellen offiziellen Regeln und mit eigenem Schlüssel.
- Keine App-Code-Änderung außerhalb des freigegebenen Auftrags. Vor riskanten Datei-, Netzwerk-, Installations- oder Anbieteraktionen prüfen und passende Zustimmung einholen.
- Tests und Gerätegrenzen ehrlich dokumentieren. NPU-Beschleunigung, vollständige Funktionsgleichheit und Build direkt auf dem A56 nicht behaupten, bis am echten Gerät belegt.
