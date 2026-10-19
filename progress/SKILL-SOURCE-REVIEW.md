# Skill-Lieferkette — geprüfte Herkunft (Aufgabe 123)

**Stand:** 2026-10-02, Sitzung 15 (fortgeschrieben)
**Zweck:** Für jede Bauaufgabe einen passenden, **global auffindbaren** Skill suchen und Risiken
untersuchen. Diese Datei ist der Nachweis. Sie ist kein Installationsprotokoll — es wurde
**nichts installiert**.

## Maßstab dieser Prüfung

Die Aufgabe verlangt: Kandidat, Herkunft, Repository, Lizenz, Inhalt, benötigte Werkzeuge und
begründete Eignung. „Begründet" heißt hier: **am Dateisystem abgelesen**, nicht aus dem Gedächtnis
oder aus einer Skill-Beschreibung. Wo eine Angabe nicht belegbar war, steht das hier als
**offen** — nicht als Vermutung.

## Geprüfte Skills

| Skill | Quelle | Autor | Lizenz | Werkzeuge | Status |
|---|---|---|---|---|---|
| `android-permissions-security` | global, `~/.claude/skills/` | Google LLC (`metadata.author`) | **unbelegt** (siehe Befund 1) | keine | freigegeben |
| `testing-setup` | global | Google LLC | **unbelegt** | keine | freigegeben |
| `adaptive` | global | Google LLC | **unbelegt** | keine | freigegeben |
| `android-profiler` | global | Google LLC | **unbelegt** | keine | freigegeben |
| `swarm-planner` | global | **nicht angegeben** | **nicht angegeben** | keine | freigegeben, Herkunft offen |
| `parallel-task` | global | **nicht angegeben** | **nicht angegeben** | keine | freigegeben, Herkunft offen |

### Befund 1 — Lizenzzeile ohne Lizenzdatei (alle vier Google-Skills)

`adaptive`, `android-permissions-security`, `testing-setup` und `android-profiler` führen im
Frontmatter jeweils `license: Complete terms in LICENSE.txt`. In **keinem** der vier Verzeichnisse
existiert eine `LICENSE.txt`:

```
~/.claude/skills/<name>/
├── SKILL.md
└── references/     # adaptive, android-permissions-security, testing-setup, android-profiler
                    # android-profiler zusätzlich: analysis/, recording/
```

Die Lizenz ist damit **nicht überprüfbar**, nicht „fehlend". Das ist ein echter Befund dieses
Projekts und kein Formalismus: eine Zeile, die auf ein nicht vorhandenes Dokument verweist, belegt
keine Nutzungsrechte.

**Bewertung:** Die Freigabe stützt sich nicht auf diese Zeile, sondern darauf, dass die vier Skills
reiner Anweisungstext sind (siehe Befund 2). Ein fehlendes Lizenzdokument bleibt offen und wird
beim Nutzer angesprochen, sobald es um Weitergabe oder Veröffentlichung geht — für die **lokale,
nicht veröffentlichte** Nutzung im eigenen Projekt trägt es die Arbeit, aber es ist nicht
sauber belegt.

### Befund 2 — Inhaltsprüfung: die Skills führen nichts aus

Die Aufgabe fordert „Skill wird nicht ausgeführt, bevor der Inhalt geprüft ist". Geprüft wurde
jeder Skill-Körper auf Netzzugriff, Paketinstallation und destruktive Befehle:

| Muster | Ergebnis |
|---|---|
| `curl`, `wget` | keine Treffer |
| `rm -rf`, `chmod +x` | keine Treffer |
| `npm install`, `pip install`, `eval` | keine Treffer |
| `| sh` | keine Treffer |

Einziger Treffer war die **Wortart** „Documentation retrieval" in `swarm-planner` (Zeile 38) —
eine Anweisung an mich, Doku zu recherchieren, kein ausgeführter Netzaufruf.

Ergebnis: **kein Skill führt Befehle aus, installiert Pakete oder greift auf das Netz zu.** Alle
sechs sind Anweisungstext für mich. Das ist der Grund, warum sie ohne weitere Prüfung einsetzbar
sind — nicht die (unbelegte) Lizenzzeile.

### Befund 3 — Zwei Skills ohne Herkunftsangabe

`swarm-planner` und `parallel-task` führen **kein** `metadata.author` und **keine** Lizenzangabe.
Ihr Inhalt ist jedoch ebenfalls reiner Anweisungstext (Befund 2), und beide sind im Projekt bereits
in Benutzung: `swarm-planner` bildet die Abhängigkeitsanalyse, mit der gerade die
Aufgabenfreigabe bestimmt wird.

Ihre Herkunft bleibt **offen**. Ich habe sie weder heruntergeladen noch verändert; sie waren vor
dieser Sitzung bereits installiert.

### Befund 4 — Was ein Skill in diesem Projekt auslöst

Ein Skill ist Anweisungstext, kein Programm. Trotzdem löst das Ausführen eines Skills reale
Wirkungen aus, und die gehören geprüft:

- `parallel-task` **startet Agenten**, die Dateien ändern. Das trifft die Projektregel „ein Agent
  pro Dateibereich, kein Agent committet während paralleler Arbeit".
- `swarm-planner` **schreibt Plandateien** (`<topic>-plan.md`) ins Arbeitsverzeichnis.
- `testing-setup` **schlägt Installation** von Hilt, Robolectric, Dropshots und weiterer Frameworks
  vor. Genau das ist im Projekt **nicht** passiert und bleibt verboten: die Tests sind JVM-Logiktests,
  es wurde kein Test-Framework nachinstalliert.

**Es wurde in dieser Prüfung nichts installiert, nichts heruntergeladen und kein Repository
geklont.** Jede künftige Installation eines weiteren Skills braucht nach CLAUDE.md vorher die
ausdrückliche Bestätigung des Nutzers — die Suche ist keine Freigabe.

## Sitzung 15 — Nachtrag: die Prüfung von 123 war unvollständig

Die Prüfung oben ist am 2026-10-02 in Sitzung 14 geschrieben worden und gelten lassen **sechs**
Skills. Installiert sind inzwischen **elf**. Die fünf später hinzugekommenen wurden nie geprüft, und
die Nachprüfung des Bestands hat **einen echten Fehler in Befund 2** aufgedeckt. Das wird hier
korrigiert, statt es im neuen Sitzungsprotokoll zu verstecken.

### Zählung am Dateisystem (`~/.claude/skills/`)

| Skill | `license:` im Frontmatter | `LICENSE`-Datei vorhanden | Autor | Befehle im Text |
|---|---|---|---|---|
| `adaptive` | Complete terms in LICENSE.txt | **nein** | Google LLC | keine |
| `android-permissions-security` | Complete terms in LICENSE.txt | **nein** | Google LLC | keine |
| `testing-setup` | Complete terms in LICENSE.txt | **nein** | Google LLC | keine |
| `android-profiler` | Complete terms in LICENSE.txt | **nein** | Google LLC | keine |
| `compose-kotlin-agent-skills` | MIT | **ja** (MIT, © 2026 haidrrrry) | haidrrrry | keine |
| `natprd` | *(keine Angabe)* | **ja** | *(keine Angabe)* | keine |
| `app-studio` | *(keine Angabe)* | **nein** | *(keine Angabe)* | keine |
| `swarm-planner` | *(keine Angabe)* | **nein** | *(keine Angabe)* | keine |
| `parallel-task` | *(keine Angabe)* | **nein** | *(keine Angabe)* | keine |
| `graphify` | *(keine Angabe)* | **nein** | *(keine Angabe)* | **ja — siehe Befund 5** |
| `learned` | *(kein SKILL.md)* | nein | — | — |

**Nur 2 von 11 Skills liefern ein echtes Lizenzdokument mit.** Befund 1 gilt damit nicht mehr für vier,
sondern für die Mehrzahl der installierten Skills.

### Befund 5 (neu, wichtig) — `graphify` führt Befehle aus und installiert ein Paket

Befund 2 sagt: „kein Skill führt Befehle aus, installiert Pakete oder greift auf das Netz zu." Das
war am 2026-10-02 für die sechs geprüften Skills **richtig** und gilt für sie **weiterhin**. Für
`graphify` ist es **falsch**, und der Skill war damals bereits installiert — er wurde nur nicht
geprüft. `graphify/SKILL.md` enthält:

- Zeile 92–93: `"$PYTHON" -m pip install graphifyy -q 2>/dev/null || "$PYTHON" -m pip install
  graphifyy -q --break-system-packages`
- Zeile 166: Verweis auf `pip install 'graphifyy[gemini]'` und auf `GEMINI_API_KEY` /
  `GOOGLE_API_KEY`

Drei Probleme, jedes für sich:

1. **Ungeprüfte Paketinstallation aus PyPI** in die lokale Python-Umgebung, ohne im Skill belegte
   Herkunft, Version, Prüfsumme oder Lizenz des Pakets.
2. **`--break-system-packages` umgeht den PEP-668-Schutz** des Systems. Das ist genau die Absicht:
   eine Umgehung einer Schutzsperre, damit die Installation ohne Nachfrage durchläuft.
3. **API-Schlüssel im Umfeld** (`GEMINI_API_KEY`) — das Skill-Verzeichnis selbst enthält keinen
   Schlüssel, aber der Aufruf würde einen aus der Umgebung laden.

**Bewertung: nicht freigegeben.** `graphify` wird in diesem Projekt **nicht** ausgeführt, bis die
Paketquelle, -version und -lizenz am Dateisystem geprüft sind. Das ist keine Vorsicht um der Vorsicht
willen: Befund 2 hätte diesen Skill als unbedenklich durchgelassen.

### Was die Verspätung bedeutet

Der Fehler ist nicht nur der einzelne Skill, sondern der **Prüfumfang**: sechs geprüfte Skills
wurden mit einer Aussage über „die Skills" verwechselt. Eine Lieferkettenprüfung, die sich nicht
jederzeit neu zählen kann, ist eine Momentaufnahme. Deshalb prüft Aufgabe **130** künftig **nicht**
nur den Inhalt eines Kandidaten, sondern muss den Kandidatenbestand auch **zählen** können —
sonst gilt die Aussage automatisch für alle und damit für keine.

## Nicht funktionsfähig, aber verfügbar

`swarm-planner` verlangt laut eigenem Text Context7 für externe Bibliotheken. In dieser Sitzung ist
Context7 **nicht verfügbar**. Nach CLAUDE.md wurde deshalb nichts behauptet: Die Aufgabe 123 verlangt
keine Bibliotheksrecherche, sondern Herkunfts- und Risikoprüfung vorhandener Skills. Wo die Recherche
fehlt, steht das als **offen** — es wurde kein Ersatz geraten.

## Zuordnung je Bauaufgabe

Die Zuordnung selbst bleibt Quelle der Wahrheit in `tasks/skill-matrix.md`. Abweichungen zwischen
dieser Tabelle und der Matrix sind ein Befund für die Matrixpflege, nicht für diese Datei.

## Offen

- Die Lizenz der vier Google-Skills ist **nicht belegbar** (fehlende `LICENSE.txt`).
- Herkunft und Lizenz von `swarm-planner` und `parallel-task` sind **nicht angegeben**.
- Für 129 („Skills finden") und 130 („Skill-Kompatibilität") ist noch **kein Kandidat gesucht** —
  hier ist nur die Lieferkette der bereits vorhandenen Skills geprüft. Die Suche selbst folgt in
  129 und wird **nicht** als Freigabe zur Installation behandelt.
