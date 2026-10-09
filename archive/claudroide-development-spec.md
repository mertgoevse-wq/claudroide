# Claudroide — ergänzende Entwicklungs- und Bau-Spezifikation

**Stand:** 2026-10-08  
**Status:** Entwurf aus drei Interviewrunden; zusätzliche Ausführungsvorgaben, kein Produktcode  
**Geltungsbereich:** Ergänzt `claudroide-spec.md` um Entwicklungsablauf, Sicherungen, Wiederherstellung, Fähigkeiten und den Start eines autonomen Claude-Code-Laufs.  
**Wichtig:** Diese Datei ersetzt die bestehende Produktspezifikation nicht. Bei Widersprüchen gilt die dort festgelegte Rangfolge, bis der Nutzer eine konkrete Änderung bestätigt. Vor dem Baubeginn sind die hier genannten Widersprüche zwischen Planungsdateien zu bereinigen.

---

## 1. Ziel

Claude Code soll Claudroide möglichst eigenständig, zügig und sorgfältig bauen können, ohne dass Mert die Projektidee oder den nächsten Schritt immer wieder erklären muss. Der Agent soll vorhandene Claudroide-Unterlagen und passende Fähigkeiten nutzen, Arbeit in kleinen überprüfbaren Schritten erledigen, Zwischenergebnisse sichern und nach einem Abbruch am letzten dokumentierten Stand weitermachen.

Diese Ergänzung ist **keine Erlaubnis**, Sicherheitsregeln zu umgehen, private Inhalte in ein öffentliches Repository zu laden oder echte Android-Systemabfragen ohne Bestätigung des Nutzers zu überspringen.

## 2. Maßgebliche vorhandene Unterlagen

Vor jedem Baulauf in dieser Reihenfolge lesen:

1. `CLAUDROIDE_CURRENT_STATE.md` — aktueller Übergabestand; auf veraltete Angaben prüfen.
2. `claudroide-spec.md` — maßgebliche Produktentscheidungen und Sicherheitsgrenzen.
3. `claudroide-development-spec.md` — zusätzliche Regeln aus diesem Interview.
4. `docs/TASKS.md` — Aufgaben, Abhängigkeiten und Abnahmekriterien.
5. `docs/SOURCE_INDEX.md` — Quellen, Verwendungszweck und Lizenzbefunde.
6. `progress/BUILD-STATE.md`, sobald diese Datei im Bau angelegt wurde — technischer Fortschrittsnachweis.
7. `docs/SKILL-INDEX.md`, sobald diese Datei in Phase 0 erstellt wurde — Fähigkeiten, Erweiterungen und Fundorte.

Die vorhandenen Notizen enthalten widersprüchliche alte Versions- und Interviewangaben. Vor einer Aussage darüber, was „fertig“ ist, ist der tatsächliche Dateistand zu prüfen. Der Bau startet gemäß aktuellem Plan bei Phase 0 / Aufgabe 0.1, sofern neuere, belegte Fortschrittsdaten nichts anderes zeigen.

## 3. Interview-Ergebnisse

### Runde 1 — vorhandene Daten und Fehlerberichte

- Die alte Claudroide-App soll **nicht automatisch importiert** werden. Mert sagt, sie sei immer direkt abgestürzt und habe nicht zuverlässig funktioniert. Ein Import alter Gespräche oder Einstellungen ist daher kein notwendiger Teil des frühen Umstiegs.
- Beim Wiederherstellen einer Sicherung sollen Daten **zusammengeführt** werden: bereits vorhandene neuere Gespräche bleiben erhalten, fehlende Gespräche werden ergänzt und gleiche Gespräche nicht doppelt angelegt.
- Technische Fehlerberichte bleiben **nur auf dem Handy**. Es gibt keine automatische Übertragung und kein stilles Senden an Entwickler oder Dienste. Der Nutzer kann später selbst entscheiden, ob er etwas teilt; genauer Ablauf zum manuellen Teilen bleibt offen.

### Runde 2 — Starten und laufend sichern

- Der Einstieg soll einfach sein: im bereits geöffneten Projektordner Claude Code mit dem üblichen Befehl starten und dort einen kurzen, vorbereiteten Arbeitsauftrag eingeben.
- Der Arbeitsstand wird häufig **lokal** gesichert, auch während einer Aufgabe.
- Nach jeder abgeschlossenen und überprüften Aufgabe soll ein geeigneter Online-Zwischenstand gesichert werden.
- Ein unfertiger Code-Zwischenstand darf im Claudroide-Entwicklungsprojekt liegen, wenn er geprüft und klar als Zwischenstand erkennbar ist; das allein macht ihn nicht zu einem fertigen Release.
- Die Auswahl zu Sicherungsinhalten nannte auch private Notizen und Quellkopien. In der Klärungsrunde wurde ausdrücklich festgelegt: **öffentlich wird nur Claudroide-App-Code und dafür bestimmte öffentliche Dokumentation**. Private Notizen, private/sensible Quellenkopien, Schwesterprojekte und Zugangsdaten dürfen nicht in das öffentliche Repository. Eine getrennte private Online-Sicherung ist in dieser Ergänzung nicht freigegeben und bleibt offen.

### Runde 3 — Fähigkeiten, Start und Fertig-Kriterium

- Alle auffindbaren globalen Fähigkeiten und Erweiterungen sollen zuerst inventarisiert und auf Nutzen geprüft werden. Vor einer Aufgabe lädt der Agent **nur die dafür passenden** Anleitungen, statt alles gleichzeitig in seinen Arbeitskontext zu laden.
- Der Agent darf die passenden Claudroide-Dateien und vorhandenen eigenen, schreibgeschützten Quellen gezielt untersuchen und im freigegebenen Claudroide-Arbeitsordner ändern, wenn es dem vereinbarten Bau dient.
- Der Start erfolgt in zwei Schritten: Claude Code im Claudroide-Projektordner öffnen; dort den unten angegebenen Arbeitsauftrag einfügen. Kein eigener Startknopf oder Startskript wird durch dieses Interview beschlossen.
- Eine Aufgabe gilt **erst nach den dazu passenden Prüfungen** als fertig. Wenn eine Prüfung nicht möglich oder nicht bestanden ist, ist das Ergebnis offen bzw. blockiert und muss ehrlich so vermerkt werden.

## 4. Sicherheits- und Zugriffsgrenzen

### 4.1 Öffentliche Sicherung

Das GitHub-Projekt `mertgoevse-wq/claudroide-next` ist öffentlich. Online-Sicherungen und spätere Commits dürfen daher ausschließlich Dateien enthalten, die für dieses öffentliche Claudroide-Projekt bestimmt sind.

Vor jeder Online-Sicherung sind mindestens zu prüfen:

- geheime Schlüssel, Kennwörter, Tokens und private Zugangsdaten;
- persönliche oder private Notizen und lokale Sitzungsdaten, sofern sie nicht ausdrücklich als öffentliche Projektdokumentation gedacht sind;
- private Schwesterprojekte und private Quellkopien;
- heruntergeladene Quellen, die nicht für das öffentliche Repository freigegeben sind;
- temporäre Dateien, APK-Build-Reste und sonstige nicht zum Quellprojekt gehörende Daten.

Ein automatischer Prüflauf ist eine notwendige Schranke, aber kein Beweis, dass eine Datei sicher öffentlich ist. Wenn unklar ist, ob Inhalt privat, fremd lizenziert oder öffentlich geeignet ist, bleibt er lokal und der Befund wird dokumentiert. Diese Ergänzung autorisiert **keinen** Push oder eine andere Online-Aktion in dieser Sitzung.

### 4.2 Android- und Termux-Berechtigungen

Mert hat in einem früheren Interview zugestimmt, Termux bei der Einrichtung alle dort angebotenen Berechtigungen zu geben. Das bedeutet nicht, Android-Sicherheitsabfragen zu überspringen:

- Claude Code darf erklären und den Nutzer zu den passenden Android-Einstellungen führen.
- Berechtigungen werden ausschließlich durch den Nutzer über die regulären Android-Abfragen bzw. Einstellungen erteilt.
- Keine Root-Rechte, kein heimliches Einschalten, keine Umgehung der Sperrliste oder Android-Schutzmechanismen.
- Erweiterungen erhalten nicht automatisch ungeprüft Zugriff auf private Schlüssel oder fremde Projekte. Die bereits festgelegte Sicherheitsprüfung und die Berechtigungsgrenzen der Haupt-Spezifikation gelten weiterhin.

### 4.3 Quellen und Dateien

- Bestehende eigene oder fremde Quellprojekte bleiben nach der Haupt-Spezifikation schreibgeschützt, sofern sie nicht ausdrücklich als aktueller Arbeitsordner freigegeben wurden.
- Änderungen für Claudroide erfolgen im Ordner `/home/mert/claudroide-next`.
- Keine fremden oder privaten Dateien in das öffentliche Claudroide-Projekt übernehmen, nur weil sie lokal auffindbar sind.
- Für fremde Inhalte gelten Herkunfts- und Lizenzprüfung sowie die Regeln in `docs/SOURCE_INDEX.md`.

## 5. Fähigkeiten und Erweiterungen

### 5.1 Inventar

In Phase 0 wird `docs/SKILL-INDEX.md` als lebendes Verzeichnis angelegt. Es soll alle tatsächlich gefundenen globalen, projektbezogenen und in den eigenen Quellen vorhandenen Fähigkeiten, Erweiterungen und Marktplätze aufführen. Je Eintrag werden Name, genauer Fundort, einfacher Zweck, Herkunft und – soweit bekannt – nötige Rechte notiert. Nicht gefundene oder nicht geprüfte Fähigkeiten dürfen nicht als vorhanden ausgegeben werden.

### 5.2 Nutzung je Aufgabe

Vor jeder Aufgabe:

1. Aufgabenbeschreibung und Abnahmekriterien lesen.
2. Im Fähigkeiten-Index nach passenden Anleitungen suchen.
3. Herkunft, Zweck, nötige Rechte und Sicherheit der passenden Anleitung prüfen.
4. Nur die passenden Anleitungen laden und anwenden.
5. Tatsächliche Verwendung und ein wichtiges Ergebnis im Aufgaben-Checkpoint festhalten.

Neue Fähigkeiten und Erweiterungen dürfen nach Sicherheitsprüfung selbstständig eingerichtet werden, soweit sie im freigegebenen Arbeitsbereich und innerhalb der geltenden Sicherheitsregeln bleiben. Unnötig weitreichende, verdächtige oder lizenzunklare Erweiterungen werden nicht blind installiert oder verwendet; sicherere Alternativen suchen und bei echtem Bauhindernis melden.

### 5.3 Vorhandenen Code gezielt nutzen

Vor einer neuen Umsetzung prüft Claude Code die relevanten vorhandenen Claudroide-Dateien und passende Einträge in `docs/SOURCE_INDEX.md`. Es soll bewährte eigene Lösungen wiederverwenden, wenn sie zum Ziel passen, und unnötige doppelte Lösungen vermeiden. Jede Übernahme muss in den Arbeitsnachweisen nachvollziehbar sein. Eine Untersuchung ist keine Erlaubnis, Schwesterprojekte zu ändern.

## 6. Arbeitsablauf und Sicherungen

### 6.1 Je Aufgabe

Für jede Aufgabe aus `docs/TASKS.md` gilt:

1. Vor Beginn den aktuellen Stand lesen und einen kleinen Plan lokal festhalten.
2. Vor Änderungen betroffene Dateien und vorhandene Prüfungen ansehen.
3. In kleinen, zusammengehörenden Änderungen arbeiten.
4. Nach sinnvollen Schritten lokal den Checkpoint aktualisieren: Aufgabe, Dateien, Entscheidungen, was geprüft wurde, Ergebnis, offene Punkte und nächster Schritt.
5. Die passenden Tests und Prüfungen ausführen. Keine Aufgabe als fertig markieren, wenn sie nicht bestanden oder nicht prüfbar ist.
6. Nach erfolgreicher Prüfung den Zwischenstand lokal sichern und gemäß Projektfreigaben die geeignete Online-Sicherung vorbereiten.
7. Vor Veröffentlichung nochmals Geheimnisse, private Daten, Quellenrechte und temporäre Dateien prüfen.
8. Ergebnis in einfacher Sprache zusammenfassen und dann mit der nächsten unabhängigen Aufgabe fortfahren, solange keine harte Stopp-Regel greift.

Die bestehenden Grenzen gelten unverändert: höchstens ein Subagent, ein Shell-Befehl und ein Build gleichzeitig; Not-Aus muss wirksam sein; Daten werden nie stillschweigend gelöscht.

### 6.2 Lokale und Online-Sicherung

- Lokal: regelmäßig nach sinnvollen Änderungen und Aufgaben-Schritten, nicht nur am Wochenende.
- Online: Ziel ist eine Sicherung nach jeder abgeschlossenen und geprüften Aufgabe, sofern die Änderung für das öffentliche Claudroide-Repository bestimmt ist und alle Prüfungen bestanden sind.
- Klar unfertige Arbeit darf als Code-Zwischenstand gesichert werden, muss aber von fertigen Meilenstein-Releases unterscheidbar bleiben.
- Private Notizen, lokale Sitzungsinhalte, private Quellkopien, Zugangsdaten und nicht freigegebene Quellen werden nicht öffentlich gesichert.
- Scheitert ein Online-Backup oder ist die Verbindung nicht verfügbar, bleibt der geprüfte Stand lokal erhalten. Der Agent dokumentiert die fehlende Online-Sicherung und fährt nur dann fort, wenn dadurch keine weitere harte Regel verletzt wird.
- Keine Online-Sicherung darf als erfolgt gemeldet werden, solange sie nicht tatsächlich bestätigt wurde.

### 6.3 Wiederherstellung nach Absturz

Nach einem Absturz oder einer neuen Sitzung:

1. `CLAUDROIDE_CURRENT_STATE.md` und `progress/BUILD-STATE.md` lesen, sofern vorhanden.
2. Den zuletzt beschriebenen Task, offene Befehle/Jobs und betroffene Dateien prüfen.
3. Mit `git status` bzw. den tatsächlichen Dateien feststellen, was lokal wirklich vorhanden ist; nicht annehmen, dass der letzte Schritt vollständig war.
4. Bereits erledigte Arbeit nicht blind wiederholen. Prüfungen bei unklarem Ergebnis erneut ausführen.
5. Den Checkpoint korrigieren, dann beim ersten nicht belegten Schritt fortsetzen.

Ein Checkpoint kann Arbeit und Entscheidungen sichern, aber nicht garantieren, dass ein KI-System nach einem Absturz jedes vorherige Detail exakt erinnert. Deshalb sind kurze, konkrete Einträge und erneute Prüfung wichtiger als Behauptungen wie „nichts kann verloren gehen“.

### 6.4 Wiederherstellen einer Datensicherung

Beim Zusammenführen einer Sicherung:

- vorhandene neuere Einträge nicht überschreiben;
- nur fehlende Gespräche/Datensätze ergänzen;
- doppelte Einträge vermeiden;
- bei einem nicht auflösbaren Konflikt eine verständliche Auswahl anbieten, statt still Daten zu verwerfen.

Der Import aus der alten Claudroide-App ist nicht vorausgesetzt. Sie war für Mert wegen sofortiger Abstürze nicht zuverlässig nutzbar.

## 7. Fehler und Laufende Arbeit

- Fehlerberichte und Absturzdetails verbleiben auf dem Handy.
- Es gibt kein automatisches Senden von Fehlerdaten, Nutzungsdaten oder privaten Gesprächsinhalten.
- Jede Aufgabe endet entweder mit einem belegten Prüfergebnis oder mit einem sichtbaren offenen Punkt/Blocker.
- Nach zwei fehlgeschlagenen Reparaturversuchen gilt die Blocker-Regel aus der Haupt-Spezifikation; der Agent soll unabhängige Arbeit fortsetzen, statt denselben Fehler endlos zu wiederholen.
- Kritische Hitze, akute Speichergefahr, drohender Datenverlust oder ein Not-Aus überstimmen das Ziel, den Lauf bis zum Ende fortzusetzen: Stand sichern, sicher anhalten und Ursache melden.
- Normale Aufgaben sollen ohne unnötige Rückfragen bis zu einem klaren Abschluss weiterlaufen. Ein fehlender Schlüssel, ein nicht verfügbares Gerät oder ein nicht erreichbarer Dienst wird als Einschränkung protokolliert; andere Aufgaben dürfen weitergehen.

## 8. Genau so Claude Code starten

Voraussetzung: Das Terminal steht bereits im Ordner `/home/mert/claudroide-next`.

1. Claude Code öffnen:

   ```bash
   claude
   ```

2. Sobald Claude Code bereit ist, diesen Arbeitsauftrag einfügen:

   > Lies `CLAUDROIDE_CURRENT_STATE.md`, `claudroide-spec.md`, `claudroide-development-spec.md`, `docs/TASKS.md` und `docs/SOURCE_INDEX.md`. Prüfe, welche Aufgabe als Nächstes wirklich offen ist. Arbeite Claudroide autonom nach diesen Dateien: beginne beim nächsten offenen Task, sichere den Stand lokal, führe die passenden Prüfungen aus, dokumentiere ehrliche Ergebnisse und fahre mit der nächsten Aufgabe fort. Nutze vor jeder Aufgabe passende, geprüfte Fähigkeiten aus dem Fähigkeiten-Index, sobald er angelegt ist. Ändere nur den freigegebenen Claudroide-Arbeitsordner. Halte alle Sicherheits-, Datenschutz-, Lizenz-, Ressourcen- und Not-Aus-Grenzen ein. Lade niemals private Daten oder geheime Schlüssel in das öffentliche Repository. Markiere nichts als fertig, solange die passenden Prüfungen nicht bestanden sind. Wenn ein echter Blocker eintritt, sichere den Stand, dokumentiere ihn und arbeite an einer unabhängigen Aufgabe weiter.

Dieser Starttext ist ein **Arbeitsauftrag**, keine Umgehung der Claude-Code-Berechtigungsabfragen. Es wird kein unsicherer Modus eingeschaltet. Falls Claude Code eine Aktion außerhalb seiner erteilten Rechte nicht ausführen kann, muss es die Aktion auslassen oder den Nutzer um die nötige reguläre Freigabe bitten.

## 9. Abnahmekriterien für diese Entwicklungsvereinbarung

- [ ] Der Einstieg ist eindeutig: im Projektordner `claude`, anschließend den Arbeitsauftrag aus §8 einfügen.
- [ ] Alle global auffindbaren Fähigkeiten werden inventarisiert; für eine konkrete Aufgabe werden passende Fähigkeiten geprüft und gezielt geladen.
- [ ] Lokale Checkpoints werden während der Arbeit regelmäßig aktualisiert und erlauben eine nachvollziehbare Wiederaufnahme.
- [ ] Online-Sicherung nach geprüften Aufgaben enthält nur für das öffentliche Claudroide-Projekt geeignete Dateien.
- [ ] Private Notizen, Schwesterprojekte, sensible Quellkopien und Schlüssel gelangen nicht in das öffentliche Repository.
- [ ] Alte Chat-Daten werden nicht automatisch importiert; Sicherungen werden nach Möglichkeit zusammengeführt, ohne neuere Daten zu überschreiben.
- [ ] Fehlerberichte werden nicht automatisch vom Handy übertragen.
- [ ] Eine Aufgabe gilt nur mit bestandener passender Prüfung als fertig; offene Prüfungen und Blocker werden ehrlich markiert.
- [ ] Kein Produktcode wird durch diese Ergänzung angelegt oder verändert.

## 10. Noch offen — nicht eigenmächtig festlegen

1. Ob und wie eine **separate private Online-Sicherung** für persönliche Notizen oder private Arbeitsstände eingerichtet werden soll.
2. Welche konkreten Dateien als dauerhafter lokaler Checkpoint dienen, bis `progress/BUILD-STATE.md` in Phase 0 angelegt ist.
3. Welcher manuelle Ablauf zum Teilen eines lokal gespeicherten Fehlerberichts gewünscht ist.
4. Ob später ein eigener Starthelfer gewünscht ist. Aktuell ist nur `claude` plus Einfügen des Textes aus §8 beschlossen.

---

*Erstellt aus drei Interviewrunden am 2026-10-08. Diese Datei dokumentiert bestätigte Antworten und klar markierte offene Punkte; sie ist keine Bestätigung, dass ein Bau, Test, GitHub-Upload oder eine Sicherung bereits ausgeführt wurde.*
