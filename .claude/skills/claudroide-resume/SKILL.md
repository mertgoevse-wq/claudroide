---
name: claudroide-resume
description: Resume the ClauDroide build from its verified checkpoint after a Claude Code, Termux, or device interruption. Use when the user invokes /claudroide-resume or asks to continue the interrupted ClauDroide task autonomously.
argument-hint: "[optional task ID or instruction]"
user-invocable: true
---

# ClauDroide: sicher am letzten Stand fortsetzen

## Auftrag

Die Nutzerin/der Nutzer möchte nach einem Abbruch mit `/claudroide-resume` ab dem letzten verifizierten Stand weiterarbeiten. Arbeite selbständig innerhalb bereits freigegebener Aufgaben. Unterbrich nur bei einer echten offenen Entscheidung, einem Sicherheitsrisiko, fehlender Berechtigung oder bevor ein Git-Push/andere externe Nebenwirkung erfolgt.

## Verbindliche Startprüfung

1. Lies `CLAUDE.md`, `claudroide-spec.md`, `tasks/skill-matrix.md` und `progress/BUILD-STATE.md` vollständig.
2. Prüfe vorhandene Dateien und Git-Arbeitsstand mit verfügbaren Claude-Code-Werkzeugen. Verwende keine Befehle, die einen unbestätigten externen Effekt haben.
3. Vergleiche den Checkpoint mit tatsächlich vorhandenen Aufgaben, Dateien und Tests. Der Checkpoint ist eine Notiz, kein Beweis: tatsächlichen Stand verifizieren.
4. Wenn kein Git-Repository oder kein Checkpoint besteht, erstelle keinen Commit und starte keine unbekannte Aufgabe. Ermittle Zustand, schreibe einen ehrlichen Checkpoint und fahre mit einer klar freigegebenen, risikoarmen Aufgabe fort.
5. Wenn der Nutzer ein Task-ID-Argument übergeben hat, berücksichtige es. Ein Task, dessen Abhängigkeiten offen sind, darf nicht vorgezogen werden.

## Wiederaufnahme und autonome Arbeit

1. Bestimme die früheste unvollständige Aufgabe, deren Abhängigkeiten erledigt sind, und die beiden in `tasks/skill-matrix.md` genannten Skills.
2. Prüfe, ob beide Skills in der aktiven Claude-Code-Sitzung verfügbar sind. Lies bei unbekannten Skills die Projekt- oder globale `SKILL.md`-Datei, bevor du dich darauf verlässt. Fehlt einer, suche einen Ersatz nur nach den CLAUDE.md-Regeln; fremde Skills nicht ohne ausdrückliche Bestätigung installieren.
3. Prüfe im Task-Briefing und in der Hauptspezifikation, dass der Task zur Parallelisierung freigegeben und sein Dateibereich exklusiv ist.
4. Aktualisiere den Checkpoint auf „läuft“ mit Task-ID, Ziel, Arbeitsdateien, Startzeit und Abhängigkeiten.
5. Arbeite die Aufgabe vollständig ab. Führe passende Prüfungen aus, lies Änderungen erneut, prüfe Geheimnisse und halte Nicht-Erledigtes offen.
6. Markiere den Checkpoint erst nach tatsächlicher Prüfung als erledigt. Trage Dateien, Tests, Ergebnisse und nächste Aufgabe ein.
7. Fahre mit der nächsten freigegebenen Aufgabe fort, solange keine Entscheidung oder Bestätigung nötig ist. Nicht so tun, als könnten Tasks ohne aktuelle Antworten/Schlüssel/Hardwaretests fertiggestellt werden.

## Parallelisierung

- Parallel-Wellen nur nach `tasks/DEPENDENCIES.md` bilden. Vor Start prüfen, dass Vorgänger erledigt sind und keine gemeinsamen Dateien, Zugangsdaten oder externen Dienste verändert werden.
- Aufgaben aus dem Abhängigkeitsgraphen in `tasks/DEPENDENCIES.md` wählen. Eine Aufgabe darf starten, wenn alle `depends_on` erledigt sind.
- Getrennte Agenten erhalten je eine klar begrenzte Aufgaben-ID und exklusive Dateien. Kein Agent commitet oder pusht während paralleler Arbeit; die leitende Sitzung integriert und prüft.
- Nach jeder Welle Konflikte erkennen, Testresultate zusammenführen, Checkpoint aktualisieren. Fehlerhafte/unklare Arbeit nicht als erledigt markieren.

## Immer anhalten und fragen

- Ungeklärte rechtliche Zulässigkeit, insbesondere Claude-Abo-Zugänge, Anbieter-Login oder Weiterleitung.
- Installation eines Skills, Programms, SDKs oder großen Downloads, für die keine aktuelle ausdrückliche Freigabe vorliegt.
- Zugriff auf neue Dateien, Cloud-Dienste, private Daten, Kosten, Zahlung oder Änderung der Anbieterziele.
- Löschen/Überschreiben außerhalb einer schon freigegebenen klar begrenzten Aufgabe.
- Änderung der Sichtbarkeit des GitHub-Repositorys oder ein Push, wenn Remote nicht exakt das bekannte private Projekt-Repository ist.
- Fehlende Nutzerschlüssel, Gerätewerte, Freigaben oder erforderliche Entscheidungen.

## Abschlussmeldung

Nenne kurz erledigte Tasks, Dateien, ausgeführte Tests, offene Hindernisse und die nächste Task-ID. Behaupte nicht, dass ein Skill installiert, ein Test bestanden oder ein Push erfolgt ist, wenn das nicht überprüft wurde.
