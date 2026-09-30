# Claudroide — Abhängigkeiten und Parallel-Wellen

Dieses Dokument ist die Arbeitskarte für `/swarm-planner`, `/parallel-task` und `/claudroide-resume`. Task-Titel stehen in `claudroide-spec.md`; zwei konkrete Skills je Task stehen in `tasks/skill-matrix.md`.

## Harte Regeln für parallele Arbeit

1. Aufgaben dürfen gleichzeitig starten, wenn ihre Abhängigkeiten erfüllt sind, keine gemeinsame Entscheidung offen ist und die Arbeitsdateien getrennt sind.
2. Vor jeder Welle den aktuellen Git-/Dateistand prüfen. Die in einer Welle genannten Aufgaben sind ein möglicher Kandidatenpool, nicht die Anweisung, alle gleichzeitig zu starten.
3. Ein Agent erhält genau eine Task-ID, seine Skill-Paarung und eine Liste exklusiver Dateien. Keine zwei Agents bearbeiten dieselbe Datei. Wenn ein gemeinsamer Architekturvertrag geändert werden muss, stoppen und erst eine Leitentscheidung treffen.
4. Parallel-Agents ändern keine gemeinsamen Versions-/Checkpointdateien und committen oder pushen nicht. Die leitende Sitzung fasst Änderungen zusammen, prüft Konflikte, Tests, Sicherheit und aktualisiert `progress/BUILD-STATE.md`.
5. Bei echter Abhängigkeit gilt die unten aufgeführte Kante; abgeschlossene Vorgänger müssen geprüft und als erledigt markiert sein.
6. Aufgaben zu Authentifizierung, Datei-/Prozessgrenzen, Berechtigungen, Verschlüsselung, Git-Push und externen Werkzeugen sind Sicherheitsgates. Daran anschließende Umsetzung darf nicht mit der Prüfung dieses Gates parallel starten.

## Wellen und Vorgänger

`—` heißt: kein fachlicher Task-Vorgänger außer den gemeinsamen Startprüfungen in `CLAUDE.md`. Mehrere Tasks einer Zeile sind ein Kandidatenpool, keine Garantie für gleichzeitigen Start. **Explizite Vorgängerangaben sind maßgeblich, auch wenn dadurch ein Task erst in einer späteren Welle frei wird.** Bei einer Zusatzabhängigkeit innerhalb derselben Zeile die betroffene Task zurückhalten. Nummern sind Gruppierungen, kein Ersatz für den Abhängigkeitscheck.

| Parallel-Welle | Tasks | Benötigte geprüfte Vorgänger | Hinweise |
|---|---|---|---|
| W0 — Einstieg und Belege | 001, 002, 003, 006 | — | Vier getrennte Dokumentationsbereiche. |
| W1 — Machbarkeit | 004, 005, 009, 019 | 001, 002, 003 | Recht/API/Struktur/Marke getrennte Zuständigkeiten. |
| W2 — Geräte- und Bauprüfung | 007, 008 | 006; 007 zusätzlich 002 | Gerät zuerst vermessen. |
| W3 — App-Grundlage | 010 | 009, 019 | Fundament; blockiert konkrete UI-Integration. |
| W4 — Baupfade | 011, 012 | 008 | Beide Wege separat bewerten, keine Cloud aktivieren. |
| W5 — Einrichtung und Grundlagen | 013, 014, 015, 016, 017, 018 | 010 | Getrennte Abläufe; Berechtigungs-/Hintergrundgates vor Implementierung prüfen. |
| W6 — Gestaltung | 020, 021, 022, 023, 024 | 003, 019 | Eigene Grafikbereiche getrennt; 023/024 können parallel zu Grafiken laufen. |
| W7 — UI-Grundlagen | 025, 026, 027, 028, 029, 030 | 010, 023, 024 | Separate UI-/Testbereiche; gemeinsame Designwerte erst durch Leitung festschreiben. |
| W8 — Chat | 031, 032, 033, 034, 035, 037, 042 | 025, 016 | Abgetrennte Screens/Komponenten möglich. |
| W9 — Chat-Laufzeit | 038, 039 | 031, 035 | Abbruch und Persistenz. |
| W9b — Streaming | 036 | 031, 035, 047, 052, 059 | Streaming braucht getesteten Provider-Vertrag. |
| W10 — Anbietergrundlage | 043, 044, 045, 060 | 005, 016, 017 | Katalog, UI, Speicher und Compliance getrennt; Schlüsselvertrag zuerst abstimmen. |
| W11 — Anbieterhärtung | 046, 047, 052, 055, 056, 057, 058, 059 | 044, 045; 059 zusätzlich 052 | Gemeinsamen Provider-Vertrag in W10 einfrieren; Task 059 nach Custom-Endpoint-Entscheidung. |
| W12 — Konkrete Anbieter | 048, 049, 050, 051, 053, 054 | 004, 005, 047, 052, 059 | Anbieteradapter parallel nur mit separaten Dateien; 050/051 stoppen bei nicht belegtem Zugang. |
| W13 — Modell & Kontext | 061, 062, 065, 067 | 043, 055; 067 zusätzlich 045 | Fähigkeit, Limits und Ausschlüsse. |
| W13b — Fallback | 063 | 062, 064, 065 | Erst nach Modell- und Kostenanzeige festlegen. |
| W13c — Modell-/Kostenanzeige | 041, 064 | 043, 055; 041 zusätzlich 061; 064 zusätzlich 002 | Fähigkeiten und aktuelle Preisdaten zuerst belegen. |
| W14 — Datenübertragung/offline | 066, 068, 069 | 039, 042, 062, 067 | Datenvorschau vor Modellkontext; Offline-Zustand darf unabhängig UI bekommen. |
| W15 — Agent-Grundlage | 070, 071 | 048, 049, 052, 053, 054; 071 zusätzlich 001 | Gemeinsame Verträge und Aufgabenplanung vor Tool-Loop. |
| W16 — Agent-Ausführung | 040, 072, 073, 074, 075, 076, 078, 079, 080 | 070, 071; 040 zusätzlich 031, 035; 074 zusätzlich 067, 068; 080 zusätzlich 061 | Separate Teilsysteme; Ressourcen und Wiederholung unabhängig testen. |
| W17 — Agent-Erweiterung | 077 | 073, 078, 090, 099 | Getrennte Versuche erst nach Datei-/Git-Isolation. |
| W18 — Projektzugriff | 081, 082, 084, 087, 088, 092 | 010, 017 | Datei-/Projektkomponenten aufteilen. |
| W18b — Projektanweisungen | 093 | 003, 122 | Erst Vertrauensgrenzen aus 122 festlegen. |
| W19 — Zugriffsfolgen | 083, 085, 086, 089, 094 | 082; 086 zusätzlich 085; 089 zusätzlich 088; 094 zusätzlich 093 | Rechte, USB, Diff und Vertrauensanzeige. |
| W19b — Dateiannahme | 090 | 088, 089, 119, 120 | Schreiben erst nach Schutz- und Vergleichsprüfung. |
| W20 — Git-Grundlage | 095, 098, 103 | 017, 045, 059 | Zugang, Änderungsliste und Historie getrennt. |
| W21 — Git-Schreibpfad | 096, 097, 099, 100, 102, 104 | 095; 097/099/100/102/104 zusätzlich 096; 099/102 zusätzlich 098; 100 zusätzlich 045 | Clone zuerst; Repository-Erstellung bleibt explizit genehmigungspflichtig. |
| W22 — Git-Upload | 101 | 097, 099, 100, 098, 095 | Letztes Gate; niemals parallel zu Commit-/Geheimnisprüfung. |
| W23 — Befehlsmachbarkeit | 105 | 007, 008, 010, 017 | Kein Befehlsrunner, bevor Android-Sandbox-Weg belegt ist. |
| W24 — Sicherheitskern | 117, 118, 119, 120, 121, 122, 126, 127 | 017, 045; 121 zusätzlich 082 | Sicherheitsgates vor Befehlen und externen Werkzeugen abschließen. |
| W25 — Befehle und Tests | 106, 107, 110, 111, 112, 114 | 105, 119, 120; 112 zusätzlich 105 | 110 ist nur Zusatzschutz, nie Sandbox-Ersatz. |
| W26 — Freigaben und Laufzeit | 108, 109, 113, 115, 116 | 017, 018, 105, 107; 109 zusätzlich 108; 116 zusätzlich 100 | Risikoentscheidungen vor weniger-Rückfragen-Modus. |
| W27 — Skills | 129, 130, 123, 124 | 001, 002, 017; 124 zusätzlich 123/130 | Skill finden, prüfen und installieren strikt getrennt. |
| W28 — Agents/externe Tools | 132, 133, 134, 135 | 070, 117, 119, 122; 133 zusätzlich 132; 135 zusätzlich 134 | Keine echte Verbindung ohne Rechte- und Datenansicht. |
| W29 — Tests & Abschluss | 128 | 045, 046, 059, 067, 090, 100, 101, 106, 108, 119, 120, 122, 127, 134, 135 | Startet erst, wenn die jeweils zu testenden Komponenten vorhanden sind. |

## Explizite Sperrkanten

- 006 → 007 und 008.
- 001 → 009 und 071.
- 002 + 003 → 004/005 und Marken-/Quellenentscheidungen.
- 009 → 010 → UI-/Chat-/Projekt-Umsetzung.
- 008 → 011/012; 012 ist nur eine Prüfung, keine Zustimmung zu einem Cloud-Anbieter.
- 004/005/047/052/059 → konkrete Provideradapter 048–054.
- 043 + 055 → 061/062; 062 → 063; 064 → 065.
- 045 + 067 → 068; 066 → Datenübertragungsfreigabe; 069 → automatisches Nachsenden bleibt aus, bis ausdrücklich konfiguriert.
- 048/049/052/053/054 → 070; 070 + 071 → Agenten-Werkzeuge 072–080.
- 070 → 040 (Chat-Projektkontext); 047/052/059 → 036 (Streaming).
- 082 → 083; 085 → 086; 088 → 089 → 090; 119/120 → 090.
- 122 → 093 (Projektanweisungen nur nach festgelegter Vertrauensgrenze).
- 095 → 096; 098 + 099 + 100 + 097 → 101 (Push).
- 105 → jede Befehlsausführung; W24 (119/120) muss vor W25 (106) abgeschlossen sein.
- 108 → 109; 018 + Android-Regeln → 113.
- 123 + 130 → 124; 134 → 135.

## Gemeinsame Dateibereiche vor Start zuweisen

Agenten dürfen nur bei exklusiven Bereichen parallel arbeiten. Wenn zwei Tasks denselben Bereich brauchen, entweder nacheinander ausführen oder Leitung eine gemeinsame Schnittstelle festlegen lassen.

- `claudroide-spec.md`, `CLAUDE.md`, `tasks/skill-matrix.md`, `tasks/DEPENDENCIES.md`, `progress/BUILD-STATE.md`: ausschließlich leitende Sitzung.
- UI-Aufgaben 031–042: pro Task eigene Bildschirm-/Komponentendateien; gemeinsames Navigationsmodell vorher festlegen.
- Anbieter 043–060: jeder Adapter getrennte Datei; gemeinsamer Anbieter-Datentyp zuerst durch Leitung festschreiben.
- Sicherheitsaufgaben 117–128: separate Testfälle/Komponenten; am Ende zentral zusammenführen und reviewen.
- Kein paralleler Push. Jeder Commit nach Abschluss eines integrierten und geprüften Aufgabenblocks.
