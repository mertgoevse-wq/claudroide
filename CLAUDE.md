# Claudroide — Anweisungen für Claude Code

Lies vor jeder Umsetzung `claudroide-spec.md`, `tasks/skill-matrix.md` und `progress/BUILD-STATE.md`. Nutzerwünsche und ausdrückliche Freigaben haben Vorrang; erfinde keine offenen Geräte-, Rechts-, Kosten- oder Anbieterangaben.

## Unterbrochene Arbeit fortsetzen

Gib in Claude Code `/claudroide-resume` ein, um die gespeicherte Arbeit fortzusetzen. Der projektgebundene Skill liegt unter `.claude/skills/claudroide-resume/SKILL.md`.

Vor einer Unterbrechung oder nach jedem abgeschlossenen Aufgabenblock aktualisiere `progress/BUILD-STATE.md`: erledigte IDs, laufende IDs, nächste freigegebene Arbeit, betroffene Dateien, Tests, Hindernisse und letzte sichere Git-Referenz. Nach einem App-/Termux-Absturz starte Claude Code erneut im Projektordner und rufe `/claudroide-resume` auf. Wenn kein aktueller Checkpoint existiert, prüft der Skill zuerst Git-Stand und Dateien; er rät nicht, was erledigt wurde.

## Autonom bauen (Bau-Schleife)

Arbeite die Aufgaben selbständig nach diesem Loop ab, ohne pro Task nachzufragen:

1. Wähle die früheste Aufgabe mit `status: pending` aus dem Frontmatter, deren gesamte `depends_on`-Liste `status: done` hat. Prüfe den Stand mit `python3 tools/sync_frontmatter.py --check`, nicht mit dem Gedächtnis.
2. Lade beide Skills aus dem Frontmatter und bestätige ihre Verfügbarkeit. Bei `gate: true` erst die unter „Harte Regeln“ genannten Gate-Prüfungen ausführen.
3. Aufgabe abarbeiten, Tests laufen lassen, Ergebnis ehrlich prüfen.
4. Status setzen: `python3 tools/sync_frontmatter.py --status ID=done`, Checkpoint `progress/BUILD-STATE.md` aktualisieren, ein Commit pro abgeschlossenem Block, Push an das verifizierte private Repository.
5. Weiter mit Schritt 1.

Anhalten und nachfragen nur bei: `gate: true` mit offenem Entscheidungsbedarf, fehlendem Schlüssel/Gerätewert, fehlgeschlagenen Tests nach dem zweiten Reparaturversuch, Push-Ziel unklar. Nicht so tun, als sei etwas fertig, das nur geplant ist.

## Skills sind Teil der Aufgabe

Vor jeder Aufgabe lade **beide** in `tasks/skill-matrix.md` dafür zugeordneten Skills, sofern diese installiert und mit der aktuellen Claude-Code-Version verfügbar sind. Die sechs geprüften globalen Skills sind `swarm-planner`, `parallel-task`, `adaptive`, `android-profiler`, `android-permissions-security` und `testing-setup`. Verfügbarkeit prüfen, nicht bloß Namen behaupten. Fehlt ein zugewiesener Skill, recherchiere einen Ersatz und dokumentiere Quelle, Lizenz und Sicherheitsbefund. Keine weitere fremde Fähigkeit global installieren, bevor der Nutzer den konkreten Kandidaten bestätigt.

## Parallel arbeiten

- Nutze `/swarm-planner`, um Abhängigkeiten und unabhängige Aufgaben zu bestimmen, und `/parallel-task`, um nur freigegebene Aufgaben in Wellen parallel auszuführen.
- Parallelisiere nur Aufgaben ohne gemeinsame Dateien, ungeklärte Vorentscheidungen oder gemeinsame externe Nebenwirkungen. Jede parallele Arbeit erhält getrennte Dateien oder einen eigenen Git-Worktree.
- Architektur-, Sicherheits-, Provider- und Datenmodell-Entscheidungen sind Sperrpunkte: erst prüfen und festhalten, dann abhängige Implementierungen starten.
- Ein Agent pro Datei/Änderungsbereich. Keine zwei Agenten gleichzeitig dieselbe Datei bearbeiten lassen. Eine leitende Sitzung prüft Zusammenführung, Tests, Sicherheitsregeln und Checkpoint.
- Commits nur für geprüfte, abgeschlossene Aufgabenblöcke. Vor Commit Schlüssel scannen. Push ausschließlich an das verifizierte private Repository dieses Projekts; bei abweichender Sichtbarkeit, unbekanntem Remote oder öffentlichem Ziel anhalten und fragen.

## Bilder und Medien

- Für Grafikaufgaben (020–022, README-Bilder) die vorhandene Claude Media Bridge des Nutzers nutzen; den Bildauftrag aus `assets/logo-brief.md` übernehmen.
- Fertige Grafiken als PNG, WebP oder JPG ablegen. Kein SVG als fertiges Logo oder fertige Illustration ausgeben. Kein Platzhalter-, Zufalls- oder KI-Füllbild einsetzen, wenn die Bridge nicht liefern kann: Lücke ehrlich dokumentieren.
- Keine weiteren Anbieterkonten (z. B. Bild-APIs mit eigenem Account) anlegen, ohne den Nutzer zu fragen.

## Repo-Pflege

- Vor jedem Commit: `python3 tools/sync_frontmatter.py --check` muss ohne Fehler durchlaufen.
- Nach jedem abgeschlossenen Aufgabenblock: Checkpoint aktualisieren und an `main` des verifizierten privaten Repositorys pushen. README, Aufgabenzählung und Wellenlisten nicht manuell pflegen; das Sync-Skript ist die Quelle.## Qualitätsregeln
- **Sprache: Englisch zuerst, Deutsch als Zweitwahl.** App-Texte und neue Logik
  liefern zuerst englischen Klartext; Deutsch gibt es, wo die App es anbietet
  (`values-de/`). Technische Begriffe werden beim ersten Vorkommen kurz erklärt.
  Historische deutsche Bezeichner aus den ersten Aufgaben werden nicht
  rückwirkend umbenannt — das würde Hunderte von Zusicherungen in Tests brechen.
  Neue Typen führen daher `label` (englisch) und nur bei Bedarf `germanLabel`.
- Keine AI-Slop-Texte: konkret, knapp, belegt; keine leeren Werbeversprechen, künstlichen Superlative, generischen Füllabschnitte oder erfundenen Kennzahlen.
- Keine Claude-/Anthropic-Marken oder -Logos nachahmen. Keine Claude-Abo-Anmeldung vermitteln; Claude API nur gemäß aktuellen offiziellen Regeln und mit eigenem Schlüssel.
- Keine App-Code-Änderung außerhalb des freigegebenen Auftrags. Vor riskanten Datei-, Netzwerk-, Installations- oder Anbieteraktionen prüfen und passende Zustimmung einholen.
- Tests und Gerätegrenzen ehrlich dokumentieren. NPU-Beschleunigung, vollständige Funktionsgleichheit und Build direkt auf dem A56 nicht behaupten, bis am echten Gerät belegt.
