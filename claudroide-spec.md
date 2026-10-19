# ClauDroide — Produktspezifikation und Baugrundlage

**Stand:** 2026-09-30
**Status:** Entwurf nach Nutzerinterview; Grundlage für die 135 Aufgabenpläne unter `tasks/`
**Projektname:** ClauDroide (vorläufiger Eigenname)
**Erstes Zielgerät:** Samsung Galaxy A56 5G des Nutzers; genaue Gerätevariante, Android-Version, RAM und freier Speicher sind vor dem Gerätetest zu erfassen.
**Zielgruppe zuerst:** Nutzer und ausgewählte Mitwirkende; private Entwicklung, spätere Öffentlichkeit nur nach ausdrücklicher Freigabe.
**Dokumentensprache:** Deutsch, verständlicher Alltagswortschatz. Technische Begriffe werden beim ersten Auftreten erklärt.

> Diese Spezifikation beschreibt ein unabhängiges Produkt. Sie ist keine Zusage, dass Anthropic oder andere Anbieter ein Produkt unterstützen oder genehmigt haben. Sie verlangt weder Kopieren geschützter Bilder und Marken noch Umgehung von Zugangs-, Bezahl- oder Sicherheitsregeln.

## 1. Kurzbeschreibung

ClauDroide soll eine eigenständige Android-App für die Arbeit an Code-Projekten und allgemeine KI-Unterstützung werden. Sie soll sich direkt auf dem Galaxy A56 installieren und bedienen lassen – ohne dass der Nutzer zusätzlich Termux oder einen Computer benötigt. Die Bedienung soll sich an der Klarheit moderner KI-Chat-Apps orientieren, bevorzugt an der einfachen Smartphone-Bedienung der Claude-App, aber mit eigenem Namen, eigenen Zeichen, Farben, Bildern und einem klaren Hinweis, dass die App unabhängig ist.

Der Kern ist eine sichere, projektbezogene Arbeitsmaschine: Sie soll Projekte öffnen, deren Dateien verstehen, passende Stellen finden, Änderungen vorschlagen, nach Freigabe Dateien ändern und erlaubte Prüf- oder Projektbefehle ausführen können. Sie soll Sitzungen und den Projektkontext behalten, Git-Repositories laden und Änderungen zeigen können sowie Claude-Code-ähnliche Projektanweisungen, wiederverwendbare Fähigkeiten, externe Werkzeuge und Spezialhelfer schrittweise unterstützen. „Claude Code-kompatibel“ bedeutet in dieser Spezifikation, nützliche Projektdateien und Arbeitsweisen zu verstehen, nicht Anthropic-Software umzuschreiben oder sich als offizielles Produkt auszugeben.

Die App soll vom Start an eigene Anbieter-Schlüssel (BYOK: „Bring Your Own Key“, also der Nutzer trägt seinen eigenen Schlüssel ein), eigene Modellnamen und eigene Serveradressen unterstützen. Claude soll über einen selbst eingegebenen Claude-API-Schlüssel angeboten werden. Kein Vermitteln eines Claude-Free/Pro/Max-Logins, kein Sammeln von Abo-Sitzungsschlüsseln und keine inoffiziellen Umwege über fremde Kommandozeilen-Anmeldungen. Andere Anbieter werden nur über dokumentierte, erlaubte Wege angebunden. Anbieter können direkt mit dem Nutzer abrechnen; ClauDroide verkauft in der ersten Fassung keine KI-Nutzung weiter.

Die App soll später kleine Modelle ohne Internet ausprobieren können. CPU (Hauptrechner), Grafikchip und NPU (Spezialteil für bestimmte KI-Rechenaufgaben) sollen nur genutzt werden, soweit Android und die tatsächlichen Treiber des A56 dies sicher und messbar zulassen. Die NPU-Nutzung ist ein zu prüfendes Entwicklungsziel, keine vorab versprochene Fähigkeit. Akku, Wärme, Speicher, Antwortzeit und verfügbare Hardware werden gemessen; die App zeigt verständliche Statuswerte und Warnungen, die Nutzer in den Einstellungen abschalten können.

## 2. Nutzer, Problem und Ziele

### 2.1 Hauptnutzer

- Der Besitzer eines Samsung Galaxy A56 5G, der derzeit nur dieses Telefon und Termux mit Debian zur Verfügung hat.
- Er möchte die App auf demselben Gerät bedienen, an Projekten arbeiten und die erste Bau- und Prüfstrecke möglichst vollständig vom Telefon aus erledigen.
- Er ist kein Fachentwickler und möchte Erklärungen, Freigaben, Fehler und Einrichtung in einfacher Sprache sehen.
- Er trägt seine eigenen KI-Anbieter-Zugänge ein und möchte Kosten, Datenweitergabe und lokale Speicherung kontrollieren.

### 2.2 Produktziele

1. Eine echte, installierbare, für Android-Touchscreens entworfene App mit Chatliste, Projektübersicht, verständlicher Einrichtung und dauerhaft gespeicherten Sitzungen.
2. BYOK, frei konfigurierbare Anbieter und Endpunkte sowie ein nachvollziehbarer Modellwechsel.
3. Projektarbeit ohne dauerhafte Terminal-Ansicht: Projekt öffnen, Dateien durchsuchen, Änderungen als prüfbaren Vergleich zeigen, Freigaben geben, Befehle ausführen und Ergebnisse erklären.
4. Unterstützen, dass der Nutzer ohne eigenen Computer den Quellcode bearbeitet, Tests anstößt und – nach eingerichteten Zugangsdaten – private Aufgabenstände in ein privates GitHub-Projekt laden kann.
5. Daten und Zugangsschlüssel sicher speichern, Projekte aus lokal gewählten Ordnern, ZIP-Dateien, Git-Servern und soweit Android es zulässt USB-Speicher nutzen.
6. Gute Reaktion auf einem Galaxy A56; Ressourcen nicht unnötig verbrauchen und lokale KI-/Beschleunigungsmöglichkeiten ehrlich prüfen.
7. Ein vollständiges, überprüfbares Markdown-Projektpaket: diese Hauptdatei plus 135 aufgabenspezifische `tasks/*.md` Dateien, aus denen ein späterer Agent einen geordneten Bauplan abarbeiten kann.

### 2.3 Nicht-Ziele und Grenzen

- Keine vollständige Nachbildung der Claude-App oder Claude-Code-Marken, -Logos, -Bildsprache oder geschützten Oberflächen. Ähnliche Bedienungsprinzipien sind okay; die Gestaltung muss eigenständig sein.
- Keine Anmeldung mit Claude-Abo als Anmeldemethode von ClauDroide und kein Umleiten von Claude Free/Pro/Max-Abfragen über Drittsoftware. Ein späterer Weg über ein unverändertes Anthropic-Programm darf ausschließlich separat und nach schriftlicher Klärung der Bedingungen untersucht werden; er ist nicht Voraussetzung und nicht Teil des sicheren Startumfangs.
- Keine Nutzung durchgesickerter, rückentwickelter oder unerlaubt kopierter Claude-Code-Quellen. Keine Änderung oder Tarnung eines Anthropic-Binaries.
- Keine Garantie, jede Desktop-Funktion auf Android exakt oder sofort zu übernehmen. Jede Funktion braucht Machbarkeits-, Datenschutz-, Android- und Lizenzprüfung. Fehlende oder nicht sichere Funktionen sind sichtbar als nicht verfügbar zu erklären.
- Keine Root-Rechte, kein verstecktes Entsperren des Telefons und kein Zugriff auf Dateien, die der Nutzer nicht ausdrücklich auswählt.
- Keine standardmäßige Übertragung des kompletten Projekts oder stiller Schlüsselversand. Nur notwendige Projektstellen an den gewählten Anbieter senden; vorherige Einsicht bzw. projektbezogene Regeln anbieten.
- Kein verbindliches Versprechen, dass die A56-NPU für frei gewählte Sprachmodelle ansprechbar ist.
- Kein echtes GitHub-Repository, keine GitHub-Zugangsdaten, kein Push und keine Installation von Fremd-Skills im Rahmen dieser Spezifikationserstellung.

## 3. Verbindliche Produktentscheidungen aus dem Interview

| Thema | Bestätigte Richtung |
|---|---|
| Name | ClauDroide als vorläufiger eigener Name; finaler Marken- und Namenscheck vor Veröffentlichung. |
| Design | Smartphonegerecht, ähnlich bequem wie Claude, aber klar eigener Auftritt mit eigenem Logo, Farben, Symbolen und Grafiken. |
| Erste Seite | Chatliste; daneben Projekte und neue Unterhaltung gut erreichbar. |
| Umfang | Vollständiges Zielbild von Anfang an; Umsetzung in geordneten Aufgaben und sichtbaren Ausbaustufen. |
| Android | Eine installierbare App zuerst; keine separate Termux-App als Nutzungsvoraussetzung, keine Root-Rechte. |
| Gerät | Galaxy A56 zuerst; weitere Android-Geräte sind kein Pflichtziel der ersten Fassung. |
| Sprachen | Deutsch und Englisch; anfängliche Wahl folgt der Handysprache, manuelle Auswahl bleibt möglich. |
| Anbieter | Claude API, OpenRouter, OpenCode-bezogene offizielle Wege, Google Antigravity nur falls offiziell dokumentiert und erlaubt, plus frei einstellbare Anbieter/Endpunkte. Verfügbarkeit ist zu prüfen. |
| Schlüssel | Nutzer trägt eigene Schlüssel ein; Kosten werden nicht von ClauDroide weiterverkauft. Zugang nur über offiziell erlaubte Wege. |
| Projekte | Handyordner, ZIP, GitHub/Git-Server und USB-Speicher, soweit Android-Zugriff dies zulässt. |
| Sicherheit | Projektbezogene Freigabestufe; Standard vorsichtig. Ein optionaler, deutlich gekennzeichneter Modus für weniger Rückfragen darf nie still aktiviert sein. |
| Offline | Dateien/gespeicherte Inhalte weiter nutzen; kleines lokales Modell als Möglichkeit, sofern Gerät, Lizenz, Speicher und Beschleuniger es zulassen. |
| Sicherung | Manuelle, verschlüsselte Sicherung an selbst gewähltem Ort; keine automatische unverschlüsselte Cloud-Sicherung. |
| GitHub | Erst privat; privates Repository und Uploads für die Bauarbeit sind gewünscht, Veröffentlichung erst nach Freigabe. Tatsächliche Einrichtung erfordert später URL/Zugang und Nutzerhandlung. |
| Zusätzliche Einnahmen | Denkbar, aber später zu entscheiden und nur getrennt, transparent und nach Anbieterregeln. |
| Bilder | Eigene Logo-, Symbol-, Header- und Bannerentwürfe sind ein geplanter Arbeitsschritt, keine Claude-Marken-Kopie. |
| Bau ohne Computer | Auf dem A56 Quellcode und Aufgaben bearbeiten; sowohl Bau direkt am Handy als auch ein Cloud-Bau vom Handy aus untersuchen. Größen, Kosten, Zugang und Grenzen vorher anzeigen. |
| Aufgabenpaket | 135 getrennte Markdown-Aufgabenpläne werden erstellt; jeder spätere Aufgabenblock braucht mindestens eine passende, global auffindbare Fähigkeit (Skill), die vor Installation geprüft und vom Nutzer bestätigt wird. |

## 4. Bedienung und Hauptabläufe

### 4.1 Erste Einrichtung

1. Start in Deutsch oder Englisch, möglichst entsprechend der Handysprache; Sprache jederzeit änderbar.
2. Kurze Erklärung: unabhängige App, Anbieter-Schlüssel gehören dem Nutzer, mögliche Kosten entstehen direkt beim Anbieter.
3. Optional Anbieter hinzufügen oder zunächst ohne Anbieter durch die App navigieren.
4. Ersten Projektordner über Androids Dateiauswahl ausdrücklich auswählen; Zugriff nach Möglichkeit dauerhaft merken. Alternativ ZIP, Git-Adresse oder später USB.
5. Erklärung zu Speicherung, Sicherungen, Dateiübertragung, Arbeitsfreigaben und Akku-/Speicherwarnungen.
6. Kein Schlüssel wird in Chatverläufe, Protokolle, Fehlermeldungen, Git-Dateien oder Sicherungen im Klartext geschrieben.

### 4.2 Chat und Projektarbeit

- Chatliste mit Suche, Datum, Projektbezug, neuer Unterhaltung, Umbenennen, Archivieren und Löschen.
- Chat mit gut lesbaren Antworten, fortlaufender Ausgabe, Abbrechen/Fortsetzen, Codeblöcken, Kopieren, Anhängen und verständlichem Fehlerzustand.
- Projektansicht mit Repository-/Ordnername, Änderungsstatus, zuletzt bearbeiteten Dateien, Suche und Aufgabenverlauf.
- Modell-/Anbieter-Auswahl pro Unterhaltung oder Projekt; Kosten- und Zugangsstatus vor Beginn soweit der Anbieter dies meldet.
- Geplante Änderungen als verständliche Dateiübersicht und Vergleich: vorher/nachher, einzelne Änderung annehmen oder verwerfen, bei Konflikt nicht überschreiben.
- Befehle und Tests werden als lesbarer Auftrag dargestellt. Ausgabe kann eingeklappt werden; Terminal ist kein Hauptbildschirm.
- Bei lang laufender Arbeit: sichtbarer Fortschritt, Abbrechen, Statusanzeige, Android-konforme fortlaufende Benachrichtigung nur wenn nötig und nach den Android-Regeln.

### 4.3 Git / Repositories

- Repository aus unterstützter Git-Adresse laden oder vorhandenen Ordner anbinden.
- Vor dem Laden Adresse, Anbieter, Zielpfad und mögliche Zugangsanfrage zeigen. Zugangsdaten sicher halten.
- Änderungen, neue Dateien, gelöschte Dateien und Konflikte anzeigen. Vor Upload an GitHub/Git-Server zeigen, was gesendet wird und wohin.
- Für ClauDroide-Bauarbeit: Aufgaben nacheinander umsetzen, nach sinnvollen Aufgabenblöcken committen (lokalen Änderungsstand benennen/speichern), danach in das private Ziel-Repository pushen (hochladen), sobald Credentials und Nutzerfreigabe vorhanden sind. Nicht jede einzelne Zeile muss einen eigenen Commit bekommen; kein Push ohne eingerichtetes Ziel und Freigabe.
- Keine Datei mit Geheimnissen in Commit, Patch oder Aufgabenbericht aufnehmen; vor jedem Commit auf Schlüssel/Token prüfen.

### 4.4 Anbieter und Antworten

- Vorinstallierte Anbieter sind nur geprüfte Verbindungsvorlagen. Die App fragt nie still ein Abo eines anderen Anbieters ab.
- Eigener Anbieter: Name, API-Format, Serveradresse, Schlüssel, Modell, optionale zusätzliche Kopfzeilen (HTTP headers), Versionshinweise, Zeitüberschreitung und Fähigkeitshinweise.
- Mindestens bekannte API-Formate, die sich mit Claude-/OpenAI-artigen Nachrichten vertragen, wenn dies technisch möglich ist. Abweichungen sichtbar melden, kein stilles Falschversprechen.
- Testknopf prüft Verbindung ohne Geheimnis an eine unpassende Stelle zu schicken; Ergebnis enthält Anbieter, Modell, Fehlerart und nächste einfache Hilfe.
- Ersatzmodell kann automatisch oder nach Rückfrage verwendet werden – vom Nutzer in Einstellungen wählbar; voreingestellt ist „vorher fragen“.
- Kostenanzeige nur mit den verfügbaren Anbieterdaten; Schätzung als Schätzung kennzeichnen, niemals als exakte Rechnung ausgeben.

## 5. Projektdateien und Claude-Code-ähnliche Arbeitsweisen

Die App soll gängige, vom Nutzer ausgewählte Projektinhalte sicher erkennen, insbesondere `CLAUDE.md`, `.claude/`-Anweisungen, Projektregeln, Skills, Agent-Beschreibungen, Hooks und MCP-Konfigurationen, sofern jeweilige Formate dokumentiert und sicher umsetzbar sind. Sie darf inkompatible Inhalte nicht still ausführen. Vor Übernahme von Regeln wird Quelle/Projekt angezeigt. Geheimnisse, Umgebungsdateien, Schlüsseldateien und große oder irrelevante Ordner müssen durch Ausschlussregeln geschützt werden.

Zielmerkmale des Arbeitskerns: Projektdateien lesen/suchen/schreiben, Änderungen planen, Aufgaben in kleine Schritte zerlegen, passende Tools aufrufen, Tool-Ergebnisse auswerten, Fehler beheben, Sitzungszusammenhang fortsetzen und bei Bedarf spezialisierte Helfer starten. Dieses Verhalten muss für jeden Anbieter über einen klaren Adapter abgebildet werden, ohne bestimmte Anbieterfunktionen zu behaupten, falls diese fehlen. MCP ist eine verbreitete Art, externe Werkzeuge anzubinden; Verbindungen brauchen Herkunft, Berechtigungen, angezeigte Daten und sichere Trennung pro Projekt. Mehrere Helfer erhalten nur die ihnen zugewiesenen Aufgaben und Daten.

## 6. Freigaben und Sicherheit

### 6.1 Projektbezogene Stufen

Vorschlag für die Umsetzung, vor der Implementierung anhand von Android, Agentenarchitektur und Sicherheit prüfen:

- **Vorsichtig (Standard):** Nachfragen, bevor Dateien geändert werden; Befehle, Git-Senden, Installieren, Löschen und externe Werkzeugaktionen separat bestätigen.
- **Ausgewogen:** Nicht-destruktive, im Projekt begrenzte Änderungen können nach Start des Auftrags erfolgen; gefährliche Aktionen und externe Übertragung brauchen weiter Freigabe.
- **Weniger Rückfragen (bewusster Ausnahme-Modus):** Der Nutzer kann ihn pro Projekt einschalten und jederzeit ausschalten. Eine sichtbare Kennzeichnung bleibt erhalten; der Modus erklärt die Folgen und läuft auf Wunsch nach Sitzung oder Zeit ab. Die erste Fassung soll nicht still oder standardmäßig im gefährlichen Modus starten.

Auch bei weniger Rückfragen bleiben Android-Systemfreigaben in Androids Hand. Zugangsschlüssel dürfen nie als Dateiänderung ausgegeben oder an ein anderes Ziel als den ausgewählten Anbieter geschickt werden. Projektbegrenzung, Geheimnisfilter, Protokollierung sicherheitsrelevanter Aktionen, klare Zielanzeige und Abbrechen bleiben verbindlich. Ob bestimmte destruktive Aktionen immer eine Extra-Freigabe brauchen, wird vor Umsetzung als Sicherheitsentscheidung dokumentiert; Standardempfehlung: ja.

### 6.2 Sicherheitsregeln

- Verschlüsselte lokale Schlüsselablage; Geräteschlüssel über Androids geschützte Schlüsselverwaltung, Klartext nur so kurz wie nötig im Speicher.
- Schlüssel nie in Logs, Screenshots/Diagnosedaten, Crashmeldungen, Aufgaben-Markdown, Git-Patches oder KI-Kontext aufnehmen.
- Datenübertragung nur über sichere Verbindung; eigene Endpunkte auf gültiges HTTPS prüfen, unsichere Verbindung mit deutlicher Warnung ablehnen oder nur bewusst erlauben.
- Projektordner-Zugriff nur nach Android-Auswahl. Keine pauschale Dateizugriffsberechtigung anstreben.
- Schutz vor Befehlen außerhalb des Projektordners, unerwartetem Überschreiben, Symlinks, Pfadmanipulation, bösartigen Projektanweisungen, unzuverlässigen Skills und manipulierten Abhängigkeiten.
- Jede externe Fähigkeit / jeder Skill wird vor Nutzung auf Quelle, Inhalte, benötigte Rechte und bekannte Risiken geprüft. Nutzer bestätigt jede Installation. Keine globale Installation im Auftrag ohne klare Bestätigung.
- Export, Chatlöschung, Anbieter-Schlüsselentfernung und Projektentkopplung müssen auffindbar sein.

## 7. Speicher, USB und Alltag

- Chats, Projekteinstellungen und Schlüssel bleiben standardmäßig in privatem App-Speicher.
- Projektordner können vom Nutzer über Androids System-Dateiauswahl freigegeben werden. USB-Speicher nur, wenn Android-Dateiauswahl und Dateianbieter ihn erreichbar machen.
- Wird USB getrennt oder die Berechtigung entzogen: Projekt in schreibgeschützten Zustand setzen, laufende Änderungen anhalten, verständlich warnen und erst nach erneutem Zugriff fortsetzen.
- Sicherung ist verschlüsselt und passwortgeschützt; Nutzer wählt Ziel (z. B. Datei/USB). Wiederherstellung vor Überschreiben prüfen und Verschlüsselung verständlich erklären.
- App darf nur mit ausdrücklicher Zustimmung eine gewählte Sicherung an einen Online-Dienst übertragen. Keine voreingestellte automatische Cloud-Sicherung.
- Android kann einzelne Schreib-/Dateifunktionen für USB- oder Dokumentanbieter einschränken. Vor Freigabe testen; Kopie ins App-Arbeitsverzeichnis als klar gekennzeichnete Alternative anbieten.

## 8. Geräte-Leistung, lokale KI und Hintergrundarbeit

- Galaxy A56 ist Zielgerät, aber genaue Variante und Softwarestand müssen aus den Telefoneinstellungen erhoben werden. Keine RAM- oder Speichergröße raten.
- Vor Optimierung CPU-/GPU-/NPU-Zugriff, Modelle, Android-APIs, Treiber, Temperatur, RAM-Verbrauch und Nutzungsdauer messen. Androids ältere NNAPI-Schnittstelle ist laut Android-Dokumentation seit Android 15 abgekündigt; aktuelle Unterstützungswege sind bei Implementierung zu prüfen.
- Kleine lokale Modelle nur mit verständlicher Auswahl von Downloadgröße, Lizenz, Speicherbedarf, Sprach-/Codequalität, Datenschutz, Akkuverbrauch und Löschmöglichkeit.
- NPU gilt erst als nutzbar, wenn ein konkretes Modell auf genau dieser Gerätevariante reproduzierbar korrekt läuft und die NPU erkennbar nutzt. Sonst fällt die Funktion transparent auf GPU/CPU zurück; keine versteckte Dauerlast.
- Anzeige für laufende Aufgabe, Akku und Speicher; Temperatur nur soweit Android eine zulässige, verlässliche Angabe bereitstellt. Warnungen sind in Einstellungen abschaltbar, kritische System- und Sicherheitsmeldungen nicht verschleiern.
- Android-Hintergrundarbeit nur für sichtbare, vom Nutzer gestartete Aufgaben. Für lang laufende sichtbare Arbeit Android-konforme Benachrichtigung, Start/Stop und passende Berechtigungen berücksichtigen. Keine heimliche Daueraktivität.

## 9. Datenschutz, Kosten, Lizenz und Marke

- Anbieter erhält nur zur Aufgabe notwendige Textstellen/Anhänge. In Einstellungen die gesendeten Projektteile ansehen und Ausschlussregeln pflegen.
- Vor Datei-/Bild-/PDF-/Sprachübertragung den Anbieter und Datenumfang zeigen. Nutzer muss wissen, dass die gewählte KI-Dienstleistung Daten außerhalb des Handys verarbeiten kann.
- Nutzungs- und Kostenhinweise je Anbieter nicht verallgemeinern; Anbieterbedingungen und Limits ändern sich. Zeitstempel/Quelle für integrierte Fakten festhalten.
- Datenschutzprotokoll muss datensparsam sein; optionale Diagnose standardmäßig aus bzw. nur nach Zustimmung.
- App-Quellcode soll später offen teilbar sein, aber der Nutzer hat eine „offen, aber geschützt“-Richtung genannt und Repository bleibt zunächst privat. Lizenz (z. B. Änderungsweitergabe, kommerzielle Nutzung, Marken, Bilder, Abhängigkeiten) ist vor öffentlicher Veröffentlichung als offene Entscheidung zu treffen. Keine Lizenz erfinden oder still auswählen.
- Keine kostenpflichtige Vermittlung, Bündelung oder Weitergabe von KI-Anfragen ohne Anbieterfreigabe, passende Verträge, Transparenz und separate Nutzerentscheidung. Einnahmenmodelle bleiben offen.
- Eigenes Produkt darf nicht Claude Code heißen oder sich als Anthropic-Produkt darstellen. Klarer Zusatz: „Unabhängiges Projekt, nicht von Anthropic entwickelt oder unterstützt.“ Markenregeln unmittelbar vor Veröffentlichung erneut prüfen.

## 10. Akzeptanzkriterien für die erste verifizierbare Version

1. Auf dem A56 lässt sich eine installierbare App aus einer nachvollziehbaren Baukette starten, die keine zusätzliche Termux-App voraussetzt. Falls direkte lokale APK-Erstellung auf dem A56 nicht praktikabel ist, muss ein vom Handy nutzbarer, transparent erklärter Bauweg vorliegen; Größe, nötige Zugänge, Kosten und Datenschutz des Weges sind offenzulegen.
2. Deutsch/Englisch, Chatliste, neuer Chat, gespeicherter Verlauf, Suche und Wiederaufnahme funktionieren nach App-Neustart.
3. Ein Claude-API-Schlüssel und ein frei eingestellter Test-Endpunkt lassen sich sicher hinzufügen, testen, auswählen und entfernen. Schlüssel taucht nach einem Test nicht in Logs, Export oder Dateisuche auf.
4. Ein vom Nutzer gewähltes Projekt wird gelesen; das Modell kann relevante Dateien referenzieren; eine Änderung wird vorher/nachher gezeigt, ausdrücklich angenommen oder abgelehnt und anschließend gespeichert.
5. Ein zulässiger Projekt-Testbefehl kann im klar begrenzten Arbeitsbereich nach der gewählten Freigabe ausgeführt, abgebrochen und mit verständlicher Ausgabe angezeigt werden. Versuch, außerhalb des erlaubten Bereichs zu schreiben, wird blockiert oder sicher bestätigt.
6. Projektdateien und Geheimnisse werden nicht ohne Erklärung vollständig übertragen. Verschlüsselte manuelle Sicherung lässt sich erzeugen und testweise wiederherstellen.
7. Ein getrenntes/entzogenes USB-Projekt verursacht keine stillen Schreibverluste; ClauDroide stoppt und erklärt die nächste Handlung.
8. Git-Änderungen und Uploadziel sind sichtbar; Geheimnisprüfung läuft; Push erfordert eingerichtetes Ziel und bewusste Freigabe.
9. Hintergrundaufgabe ist sichtbar und stoppbar; Warnungen für knappen Speicher, hohen Verbrauch oder nicht verfügbaren lokalen Beschleuniger sind verständlich und die nicht-kritischen Warnungen abschaltbar.
10. Tests decken Schlüssel, Pfadgrenzen, Berechtigungen, Abbruch, Wiederherstellung, Offline-Verhalten und Fehlerfälle ab; Ergebnisse sind im Markdown dokumentiert.

„Vollwertig“ bleibt ein Zielbild, das gegen eine bei Baubeginn festgehaltene Funktionsliste geprüft wird. Nicht erreichbare Funktionen sind als begründet offen markiert; sie werden nicht als fertig bezeichnet.

## 11. Bauprozess und Aufgabenpaket

Die 135 einzelnen Pläne unter `tasks/001-...md` bis `tasks/135-...md` sind Aufgabenbriefings, keine App-Quellcodedateien. Jeder enthält Zweck, konkretes Ergebnis, Prüfkriterien, Risiken und einen Skill-Hinweis. `tasks/skill-matrix.md` ordnet jeder Aufgabe zwei konkrete Skills zu; `tasks/DEPENDENCIES.md` enthält Abhängigkeiten, Sperrkanten und mögliche Parallel-Wellen. Die Regeln dieser Hauptdatei gelten für jede Aufgabe. Vor Umsetzung müssen die Skills tatsächlich verfügbar sein und geladen werden; Namen allein zählen nicht als Verwendung.

Ein späterer Bau-Agent soll die Aufgaben gemäß `tasks/DEPENDENCIES.md` und nur nach erfüllten Abhängigkeiten ausführen. Unabhängige Aufgaben dürfen in parallelen Wellen bearbeitet werden, wenn sie getrennte Dateien und Berechtigungen haben. `CLAUDE.md` und `.claude/skills/claudroide-resume/SKILL.md` definieren den dauerhaft nutzbaren `/claudroide-resume`-Befehl zum Prüfen des letzten Checkpoints und sicheren Fortsetzen nach Abbruch. Vor jeder Aufgabe: aktuelle Dateien lesen, Plattform- und Anbieterfakten prüfen, zugewiesenen Skill global suchen, Skill-Inhalt/Sicherheitswirkung bewerten und den Nutzer vor Installation einer fremden Fähigkeit fragen. Skills nicht blind ausführen. Nach Aufgabenblöcken: Formatierung/Tests/Build soweit verfügbar, Änderungen ansehen, Geheimnisse ausschließen, passenden Commit erstellen. Push in das private GitHub-Projekt nach eingerichteten Zugängen und Nutzerfreigabe. Keine Commits/Pushes an nicht bestätigte Ziele. Kein Deploy.

Da der Nutzer nur ein A56 hat, muss der Bauplan parallel einen „nur mit Telefon“-Pfad prüfen: Bearbeitung, Git, Testen und APK-Bau direkt am Gerät oder über einen am Telefon steuerbaren, transparenten Online-Bau. Vor Downloads Größe/Grund/Ziel nennen und Zustimmung abwarten. Keine zusätzlichen globalen Werkzeuge, Dienste, Fremd-Skills oder Accounts einrichten, bevor ihr Nutzen und Folgen erklärt und die nötige Zustimmung vorliegt.

### 11.1 Einheitliche Mindeststruktur jeder Aufgaben-Markdown-Datei

Jede spätere Aufgaben-Datei muss die folgenden Felder ausfüllen, nicht nur auf diese Spezifikation verweisen:

- ID, kurzer Titel, Status, Ziel und konkreter Nutzen.
- Voraussetzungen und Abhängigkeiten; betroffene Produktanforderungen aus dieser Spezifikation.
- Exakte erwartete Dateien/Ergebnisse; keine geheimen Werte oder Platzhalter für unabgeschlossene Implementierung.
- Schrittfolge in einfachen Worten; bestehende Projektkonventionen zuerst prüfen.
- Nutzeroberfläche/Bedienweg und Fehler-/Randfälle, falls betroffen.
- Datenschutz-, Sicherheits-, Kosten-, Lizenz- und Android-Auswirkungen.
- Tests mit konkreten Erfolgskriterien; Geräteprüfung am A56, wo nötig.
- Zur Aufgabe passende Skill-Suche: Suchbegriff, gefundener Kandidat, Quelle/Lizenz und warum geeignet; vor Installation Nutzerzustimmung einholen.
- Fertig-Kriterien, Dokumentationsänderung, Commit-Nachricht-Vorschlag und Upload-Voraussetzung.
- Offene Punkte sichtbar als `[OFFEN]`; niemals als feststehende Tatsache erfinden.

## 12. Technische und rechtliche Recherche (Stand 2026-09-30)

Recherche ist eine Momentaufnahme. Anbieterregeln, APIs, Android-Versionen und Dienste vor Implementierung und Veröffentlichung nochmals prüfen.

1. **Anthropic Agent SDK – offizielle Übersicht:** Die Übersicht beschreibt den SDK als Möglichkeit, Claude-Code-Agenten in Anwendungen einzubauen und nennt eingebaute Werkzeuge, Hooks, Spezialhelfer, MCP, Berechtigungen, Sitzungen und Skills. Sie sagt, Drittanbieter dürfen ohne besondere Zustimmung keinen Claude.ai-Login und keine Limits für Free/Pro/Max in eigenen Produkten anbieten. Sie untersagt Produktauftritte, die Claude Code/Anthropic als eigene Marke bzw. Partnerschaft erscheinen lassen. Für diesen Produktentwurf folgt daraus: Claude-API-Schlüssel als sicherer Startweg; kein Abo-Login/Token-Relay; SDK- bzw. Anthropic-Binary-Integration separat und vor Nutzung auf Erlaubnis, Verträge, unveränderten Betrieb und technische Machbarkeit prüfen. Quelle: [Agent SDK Overview](https://code.claude.com/docs/en/agent-sdk/overview), abgerufen 2026-09-30.
2. **Anthropic Legal and Compliance:** Beschreibt Bedingungen, wenn Kunden Claude Code unverändert in eigenen Produkten laufen lassen; nennt kommerzielle Vertragsbedingungen und Anforderungen an unmodifizierte Ausführung sowie nutzereigene Zugangsdaten. Entwickler eigener Dienste sollen API-Schlüssel nutzen; Claude-Abo-Zugang und Sitzungstoken dürfen nicht durch Drittanbieter vermittelt, gesammelt oder weitergereicht werden. Quelle: [Legal and compliance](https://code.claude.com/docs/en/legal-and-compliance), abgerufen 2026-09-30. Vor einer Integration aktuelle Bedingungen und direkte Freigabe klären; diese Spezifikation erteilt keine Rechte.
3. **Claude Android-App (Google Play):** Die Produktseite nennt allgemeine Chat-, Schreib-, Recherche-, Code-, Bild-/PDF- und Spracheingabe-Funktionen. Diese dienen als Orientierung für komfortable Smartphone-Abläufe, nicht als Anweisung, Oberfläche oder Marke zu kopieren. Quelle: [Google Play Listing](https://play.google.com/store/apps/details?id=com.anthropic.claude&hl=en), abgerufen 2026-09-30.
4. **Samsung Galaxy A56:** Samsung nennt ein 6,7-Zoll-Display, Octa-Core-Prozessor, Kühlung und Akkuangaben; exakte vom Nutzer gekaufte Variante und freier Speicher bleiben offen. Samsung-Produktseite: [Galaxy A56 5G](https://www.samsung.com/us/smartphones/galaxy-a56-5g/), abgerufen 2026-09-30. Das Datenblatt ersetzt keine Messung am tatsächlichen Gerät.
5. **Android-Dateiauswahl:** Androids Storage Access Framework lässt Nutzer Dateien/Ordner über die Systemauswahl freigeben; Zugriff kann Nutzerkontrolle und dauerhafte Berechtigung berücksichtigen. Große Ordner können Leistung beeinträchtigen; externer Speicher hängt vom Dokumentanbieter ab. Quelle: [Access documents and other files](https://developer.android.com/training/data-storage/shared/documents-files), abgerufen 2026-09-30.
6. **Android-Hintergrundarbeit:** Sichtbare lang laufende Arbeit kann einen Foreground Service erfordern, mit dauerhafter Statusbenachrichtigung und Nutzerbewusstsein; Einsatz und Diensttyp müssen zur Android-Version passen. Quelle: [Foreground services overview](https://developer.android.com/develop/background-work/services/fgs), abgerufen 2026-09-30.
7. **Lokale KI-Beschleunigung:** Android dokumentiert, dass NNAPI mit Android 15 abgekündigt wurde; aktuelle Migrations- und Beschleunigerwege sind beim Implementieren zu untersuchen. Quelle: [Android NNAPI migration guide](https://developer.android.com/ndk/guides/neuralnetworks/migration-guide), abgerufen 2026-09-30. Ein allgemeiner verfügbarer NPU-Zugriff auf dem A56 ist durch diese Recherche nicht bestätigt.
8. **AnyClaw als Architektur-Inspiration:** Das öffentlich sichtbare Repository beschreibt ein einzelnes Android-Paket mit eingebettetem Linux-Bereich, App-Oberfläche, lokaler Diensteverwaltung und BYOK-/Agent-Funktionen. Es enthält auch konkrete Sicherheits-/Lizenzrisiken und Behauptungen, die nicht als verifiziert übernommen werden. ClauDroide darf allgemeine Lösungsfragen untersuchen, aber Quellcode, Logos, Marke, Lizenz und Sicherheit separat prüfen und nichts ungeprüft übernehmen. Quelle: [AnyClaw GitHub repository](https://github.com/OpenClawAndroid/openclaw-android-assistant), abgerufen 2026-09-30.
9. **OpenRouter, OpenCode, Antigravity und weitere Dienste:** Die Recherche bestätigt nicht, dass alle angebotenen CLI-Anmeldungen oder Token-Importe in einer Drittanbieter-App erlaubt sind. Deshalb: dokumentierte offizielle API/Anbieterwege zuerst, keine inoffizielle Sitzungs-Weiterleitung. Jede konkrete Integration muss Anbieter-Dokumentation, Nutzungsbedingungen, Authentifizierungsweg und Schlüsselablage vor ihrer Umsetzung bestätigen.

## 13. Offene Entscheidungen – vor Umsetzung nicht erfinden

- Exakte Android-Version, RAM-Ausstattung, Gerätespeicher, freie Kapazität, Modellkennung, thermisches Verhalten und USB-Dateisystem am Galaxy A56.
- Endgültiger Name, Icon-/Bildfreigabe und Markenprüfung.
- Lizenz für Quellcode, erzeugte Bilder und neue Beiträge; welche Arten kommerzieller Nutzung erlaubt sein sollen.
- Ob Anthropic einer etwaigen Einbettung/Verwendung des Agent SDK oder Claude-Code-Binaries zustimmt und ob der konkrete Android-Weg den Bedingungen entspricht. Ohne bestätigte Erlaubnis kein Claude-Abo-Login und kein Binary-Umweg.
- Offiziell unterstützte Authentifizierung, API-Formate und Bedingungen für OpenCode und Antigravity; kein Login per nachgebautem Browser, kopiertem Token oder nicht freigegebenem Proxy.
- Welche Anbieter in erster nutzbarer Fassung tatsächlich verfügbar sind und welche freien Nutzungsstufen derzeit bestehen.
- Welche lokale Modelle und Dateigrößen auf genau dem A56 sinnvoll und rechtlich nutzbar sind; ob überhaupt NPU-Beschleunigung verfügbar ist.
- Ob privates GitHub-Repository schon existiert, Zieladresse, Zugangsweg, Mitwirkende und Upload-Schlüssel. Hier im Dokumentationsschritt wird keines erstellt.
- Wie genau und wann ClauDroide Geld durch getrennte Zusatzdienste einnehmen darf, falls überhaupt.
- Ziel-Mindestversion Android, Verteilung zuerst per direkt installierbarer APK oder Store, Signaturschlüssel und Updateweg.
- Schwellenwerte für Warnung bei Akku, Speicher, Wärme und Downloadgröße; auf A56 messen statt raten.

## 14. Begriffe in einfacher Sprache

- **API-Schlüssel:** Geheimcode, mit dem ein Anbieter seine KI-Nutzung einem Nutzerkonto zuordnet.
- **BYOK:** Der Nutzer bringt den eigenen Anbieter-Schlüssel mit und bezahlt den Anbieter direkt.
- **Endpunkt / Serveradresse:** Internetadresse, an die eine App ihre Anfrage schickt.
- **Repository (Repo):** Projektordner mit Dateien und einer nachvollziehbaren Liste von Änderungen.
- **Git:** Werkzeug, um Änderungen an einem Projekt zu benennen, zu speichern und mit anderen zu teilen.
- **Commit:** Ein gespeicherter, benannter Stand der Änderungen.
- **Push:** Einen gespeicherten Stand an einen Online-Git-Server hochladen.
- **NPU:** Bauteil, das manche KI-Rechenaufgaben besonders sparsam erledigen kann, wenn die Software es tatsächlich erreichen darf.
- **MCP:** Vereinbarte Verbindung, mit der ein KI-Agent zusätzliche Werkzeuge oder Datenquellen verwenden kann.
- **Skill / Fähigkeit:** Wiederverwendbare Anleitung für eine bestimmte Art von Arbeit; kann auch ausführbare Schritte enthalten und muss daher geprüft werden.
- **Hook / Auslöser:** Eine automatisch ausgeführte Aktion bei einem bestimmten Ereignis; braucht besonders genaue Freigaben.
- **Agent:** KI-Arbeitsablauf, der mehrere Schritte und Werkzeuge nacheinander nutzt.
- **Android-Dateiauswahl:** Systemdialog, in dem der Nutzer einen Ordner oder eine Datei ausdrücklich freigibt.
- **Verschlüsselung:** Schutz, bei dem Daten nur mit passendem Schlüssel/Passwort lesbar sind.

---

# Verzeichnis der 135 Markdown-Aufgabenpläne

Alle 135 nummerierten Einzelpläne stehen in `tasks/`; ergänzend existieren `skill-matrix.md` und `DEPENDENCIES.md`. Die Matrix weist jedem nummerierten Task zwei konkrete Skill-Kandidaten zu. Die Nummern allein legen keine Reihenfolge fest; der Abhängigkeitsgraph entscheidet. Die erste Aufgabenfassung ist kein App-Code.

## Grundlagen und Machbarkeit
1. `tasks/001-product-governance.md` — Projektregeln, Nutzerziel, Grenzen und offene Fragen verwalten.
2. `tasks/002-source-verification.md` — Faktenquellen und Aktualisierungsdatum für Anbieter und Android prüfen.
3. `tasks/003-brand-and-legal-boundaries.md` — Eigenständigen Auftritt und Anthropic-Grenzen in den Bauprozess übertragen.
4. `tasks/004-claude-access-feasibility.md` — Claude-API-Zugang und mögliche SDK-Nutzung getrennt bewerten.
5. `tasks/005-provider-policy-matrix.md` — Offizielle Wege für Claude, OpenRouter, OpenCode und Antigravity belegen.
6. `tasks/006-a56-device-inventory.md` — Gerätevariante, Android, RAM, Speicher und Anschlüsse erfassen.
7. `tasks/007-npu-feasibility.md` — Verfügbarkeit und zulässige NPU/GPU/CPU-Wege am echten A56 messen.
8. `tasks/008-phone-only-build-plan.md` — Bau und Test nur vom A56 aus planen und Grenzen benennen.

## App-Grundlage und Einrichtung
9. `tasks/009-project-layout.md` — Projektordner und Aufgabenkonventionen festlegen.
10. `tasks/010-android-app-shell.md` — Startfähige Android-App mit sicherer Basiskonfiguration anlegen.
11. `tasks/011-phone-build-path.md` — Android-Bau direkt am Telefon untersuchen und erproben.
12. `tasks/012-cloud-build-path.md` — Transparenten, vom Handy steuerbaren Ersatz-Bauweg prüfen.
13. `tasks/013-first-run-flow.md` — Erste Einrichtung ohne Fachsprache entwerfen.
14. `tasks/014-language-detection.md` — Handy-Sprache Deutsch/Englisch übernehmen.
15. `tasks/015-language-switch.md` — Sprache manuell wechseln und Auswahl behalten.
16. `tasks/016-app-settings.md` — Auffindbare Einstellungen für Sprache, Anbieter, Daten und Warnungen bauen.
17. `tasks/017-device-permissions.md` — Android-Berechtigungen sparsam und verständlich anfordern.
18. `tasks/018-background-task-lifecycle.md` — Sichtbare, stoppbare Hintergrundarbeit nach Android-Regeln bauen.

## Gestaltung und Navigation
19. `tasks/019-independent-brand.md` — Eigenen Namen, Bildsprache und Abgrenzung definieren.
20. `tasks/020-logo-and-app-icon.md` — Eigenes Logo und Android-App-Symbol entwerfen und einbinden.
21. `tasks/021-chat-illustrations.md` — Eigene Chat-, Projekt- und Leerzustandsbilder erstellen.
22. `tasks/022-header-and-banner-assets.md` — Eigene Kopf- und Bannerbilder für App und Projekt erstellen.
23. `tasks/023-accessible-colors.md` — Eigenständige, kontrastreiche Farbregeln gestalten.
24. `tasks/024-type-and-spacing.md` — Lesbare Schriftgrößen, Abstände und Touchflächen festlegen.
25. `tasks/025-navigation-structure.md` — Chat, Projekte, Suche und Einstellungen verständlich erreichbar machen.
26. `tasks/026-small-screen-layout.md` — Einhand- und Smartphoneansichten am A56 prüfen.
27. `tasks/027-keyboard-and-input.md` — Tastatur, Eingabefeld, Anhänge und Einhandbedienung testen.
28. `tasks/028-theme-and-dark-mode.md` — Hell-/Dunkelgestaltung und Systemeinstellung unterstützen.
29. `tasks/029-accessibility-basics.md` — Bildschirmleser, Schriftvergrößerung und klare Zustände unterstützen.
30. `tasks/030-ui-error-and-loading-states.md` — Lade-, Fehler-, Offline- und Leerezustände verständlich darstellen.

## Chat und Sitzungen
31. `tasks/031-chat-list.md` — Chatliste mit neuen und bestehenden Unterhaltungen bauen.
32. `tasks/032-chat-search-and-filter.md` — Chats suchen und nach Projekt/Datum filtern.
33. `tasks/033-create-and-rename-chat.md` — Unterhaltung erstellen, umbenennen und archivieren.
34. `tasks/034-delete-chat-and-export.md` — Löschen und Export mit verständlicher Bestätigung anbieten.
35. `tasks/035-message-composer.md` — Eingabe, Senden, Mehrzeilen-Text und Abbrechen umsetzen.
36. `tasks/036-streaming-responses.md` — Laufende Antworten schrittweise und stabil anzeigen.
37. `tasks/037-code-message-display.md` — Code, Kopieren und lange Antworten gut lesbar anzeigen.
38. `tasks/038-stop-retry-and-continue.md` — Antwort stoppen, wiederholen und fortsetzen.
39. `tasks/039-session-resume.md` — Chatkontext nach App-Neustart wieder aufnehmen.
40. `tasks/040-chat-project-link.md` — Unterhaltung sicher einem Projekt zuordnen oder lösen.
41. `tasks/041-cost-and-model-labels.md` — Modell und Kostenschätzung transparent beim Chat anzeigen.
42. `tasks/042-conversation-privacy-controls.md` — Datentransfer und Chat-Speicherung pro Gespräch erklären.

## BYOK und Anbieter
43. `tasks/043-provider-catalog.md` — Liste geprüfter, dokumentierter Anbieter führen.
44. `tasks/044-provider-add-flow.md` — Anbieter verständlich und schrittweise hinzufügen.
45. `tasks/045-secret-storage.md` — Zugangsschlüssel verschlüsselt im Gerät speichern.
46. `tasks/046-secret-redaction.md` — Schlüssel in Ausgabe, Protokoll und Export unkenntlich machen.
47. `tasks/047-provider-connection-test.md` — Zugang testen und sichere Fehlermeldung anzeigen.
48. `tasks/048-claude-api-adapter.md` — Claude-API mit eigenem Schlüssel über dokumentierten Weg anbinden.
49. `tasks/049-openrouter-adapter.md` — OpenRouter über dokumentierte API anbinden und Bedingungen prüfen.
50. `tasks/050-opencode-provider-check.md` — OpenCode-Bezug prüfen, ohne Anbieter und Client gleichzusetzen.
51. `tasks/051-antigravity-official-route-check.md` — Antigravity nur bei belegtem, erlaubt dokumentiertem Weg anbinden.
52. `tasks/052-custom-endpoint.md` — Eigene Serveradresse, Format, Kopfzeilen und Modell einstellen.
53. `tasks/053-anthropic-compatible-format.md` — Claude-artige API-Formate korrekt abbilden.
54. `tasks/054-openai-compatible-format.md` — OpenAI-artige API-Formate korrekt abbilden.
55. `tasks/055-model-list-and-manual-model.md` — Modellliste anzeigen und eigene Modellnamen erlauben.
56. `tasks/056-provider-key-rotation.md` — Schlüssel ersetzen und alte Schlüssel sicher entfernen.
57. `tasks/057-provider-disable-and-delete.md` — Anbieter pausieren/löschen, ohne Chatverläufe zu beschädigen.
58. `tasks/058-provider-error-help.md` — Authentifizierungs-, Netzwerk- und Modellfehler in Alltagssprache erklären.
59. `tasks/059-network-security.md` — Sichere Übertragung und Warnung vor unsicheren Endpunkten umsetzen.
60. `tasks/060-provider-terms-and-last-checked.md` — Bedingungen und letzten Prüfzeitpunkt je Anbieter dokumentieren.

## Modellwahl, Kosten und Daten
61. `tasks/061-model-capability-labels.md` — Fähigkeiten wie Bild, Werkzeugnutzung und Streaming je Modell markieren.
62. `tasks/062-model-selection.md` — Modell pro Chat/Projekt festlegen.
63. `tasks/063-model-fallback-settings.md` — Automatischen oder bestätigungspflichtigen Ersatz wählen lassen.
64. `tasks/064-cost-estimate.md` — Vorhandene Preise als datierte Schätzung zeigen.
65. `tasks/065-usage-limits.md` — Nutzerlimits und Warnungen konfigurieren, falls Anbieterwerte verfügbar sind.
66. `tasks/066-request-data-preview.md` — Vor Anfrageübertragung verwendete Projektteile anzeigen.
67. `tasks/067-project-exclusions.md` — Geheimnisse und ausgeschlossene Dateien vom KI-Kontext fernhalten.
68. `tasks/068-minimal-context-selection.md` — Nur notwendige Code-Dateien und Ausschnitte senden.
69. `tasks/069-offline-chat-state.md` — Offline-Status, Warteschlange und Wiederaufnahme verwalten.
70. `tasks/070-provider-independent-agent-contract.md` — Gemeinsame Agent-Funktionen über Anbieter hinweg festlegen.

## Agent-Arbeitskern
71. `tasks/071-agent-task-planning.md` — Große Nutzeraufgaben in prüfbare Schritte zerlegen.
72. `tasks/072-agent-tool-loop.md` — Modellantworten und erlaubte Werkzeuge kontrolliert verknüpfen.
73. `tasks/073-agent-run-state.md` — Laufstatus, Abbruch und Fehlerfortsetzung speichern.
74. `tasks/074-agent-context-management.md` — Kontextgrenzen und benötigte Projektinformationen verwalten.
75. `tasks/075-agent-progress-ui.md` — Fortschritt verständlich anzeigen.
76. `tasks/076-agent-result-summary.md` — Ergebnis, Änderungen und offene Punkte zusammenfassen.
77. `tasks/077-agent-session-fork.md` — Optional getrennte Arbeitsversuche sicher führen.
78. `tasks/078-agent-concurrency-limits.md` — Parallelität und Ressourcenverbrauch begrenzen.
79. `tasks/079-agent-retry-policy.md` — Wiederholungen ohne doppelte Nebenwirkung kontrollieren.
80. `tasks/080-agent-provider-capability-fallback.md` — Nicht unterstützte Anbieterwerkzeuge sichtbar ersetzen oder ablehnen.

## Projekte und Dateien
81. `tasks/081-project-home.md` — Projektübersicht mit Änderungsstatus und letzter Arbeit erstellen.
82. `tasks/082-android-folder-picker.md` — Nutzergewählten Projektordner per Android-Dateiauswahl öffnen.
83. `tasks/083-persistent-folder-access.md` — Dauerhafte Ordnerfreigabe sicher speichern und widerrufen.
84. `tasks/084-zip-import.md` — ZIP auswählen, prüfen und sicher entpacken.
85. `tasks/085-usb-project-access.md` — USB-Projekte über Android-Dateianbieter testen.
86. `tasks/086-usb-disconnect-recovery.md` — Abziehen/Verbindungsverlust ohne stille Datenverluste behandeln.
87. `tasks/087-file-tree-and-search.md` — Projektdateien durchsuchen und passende Inhalte finden.
88. `tasks/088-file-preview.md` — Textdateien und unterstützte Formate sicher anzeigen.
89. `tasks/089-file-diff-viewer.md` — Änderungen verständlich vorher/nachher zeigen.
90. `tasks/090-file-edit-approval.md` — Geänderte Dateien einzeln oder gesammelt annehmen/verwerfen.
91. `tasks/091-file-conflict-protection.md` — Zwischenzeitlich geänderte Dateien vor Überschreiben schützen.
92. `tasks/092-project-size-and-exclusions.md` — Große Ordner/Abhängigkeiten sinnvoll ausschließen.
93. `tasks/093-project-instructions.md` — `CLAUDE.md` und vergleichbare Projektregeln sicher lesen.
94. `tasks/094-project-instruction-trust.md` — Herkunft und mögliche riskante Anweisungen kenntlich machen.

## Repositories und GitHub
95. `tasks/095-git-provider-auth.md` — GitHub/Git-Server über unterstützten Zugang anbinden.
96. `tasks/096-clone-repository.md` — Git-Repository laden, Ziel und Zugriff anzeigen.
97. `tasks/097-create-private-project-repository.md` — Privates ClauDroide-Bau-Repo erst nach Zugang und Bestätigung einrichten.
98. `tasks/098-git-change-list.md` — Änderungen, neue und entfernte Dateien auflisten.
99. `tasks/099-git-commit-flow.md` — Aufgabenblock als benannten lokalen Stand sichern.
100. `tasks/100-git-secret-scan.md` — Vor Commit/Push Geheimnisse erkennen und stoppen.
101. `tasks/101-git-push-approval.md` — Ziel, Dateien und Upload vor jedem Push bestätigen.
102. `tasks/102-git-merge-conflicts.md` — Konflikte verständlich anzeigen und sicher lösen.
103. `tasks/103-git-history-view.md` — Änderungen und frühere Stände nachvollziehbar anzeigen.
104. `tasks/104-git-network-failure.md` — Unterbrochene Uploads sicher wiederholen.

## Befehle, Tests und Freigaben
105. `tasks/105-command-runner-feasibility.md` — Sicheren Android-Befehlspfad ohne separate Termux-App untersuchen.
106. `tasks/106-command-working-directory.md` — Befehle auf freigegebenen Projektbereich begrenzen.
107. `tasks/107-command-preview.md` — Zweck, Zielordner und mögliche Folgen vor Ausführung zeigen.
108. `tasks/108-command-approval-levels.md` — Projektbezogene vorsichtige/ausgewogene Freigabestufen bauen.
109. `tasks/109-reduced-prompt-mode.md` — Bewussten, sichtbaren Modus für weniger Rückfragen sicher gestalten.
110. `tasks/110-dangerous-command-blocklist.md` — Offensichtlich gefährliche Befehle erkennen und behandeln.
111. `tasks/111-command-output-ui.md` — Ausgabe kürzen, durchsuchen und in Alltagssprache erklären.
112. `tasks/112-test-runner.md` — Projektprüfungen nach Nutzerfreigabe starten.
113. `tasks/113-long-task-notification.md` — Sichtbare Android-Benachrichtigung für lange Nutzeraufträge bauen.
114. `tasks/114-task-cancel-and-cleanup.md` — Laufende Befehle sauber stoppen und Zwischendateien behandeln.
115. `tasks/115-command-audit-log.md` — Sicherheitsrelevante Ausführungen datensparsam protokollieren.
116. `tasks/116-runtime-dependency-install.md` — Abhängigkeiten nur nach Anzeige von Quelle, Größe und Zustimmung installieren.

## Berechtigungen, Geheimnisse und Sicherheit
117. `tasks/117-permission-center.md` — Aktuelle Projektfreigaben an einer Stelle anzeigen.
118. `tasks/118-approval-history.md` — Erteilte Freigaben und widerrufbare Regeln nachvollziehbar machen.
119. `tasks/119-project-boundary-enforcement.md` — Zugriff auf gewähltes Projekt technisch begrenzen.
120. `tasks/120-path-traversal-defense.md` — Pfadmanipulation und Ausbrüche aus Projektordnern verhindern.
121. `tasks/121-symlink-and-special-file-defense.md` — Sonderdateien und symbolische Verknüpfungen sicher behandeln.
122. `tasks/122-prompt-injection-defense.md` — Bösartige Anweisungen aus Dateien/Werkzeugen als nicht vertrauenswürdig behandeln.
123. `tasks/123-skill-source-review.md` — Fähigkeiten anhand Quelle, Inhalt und Rechte prüfen.
124. `tasks/124-skill-install-approval.md` — Installationsbestätigung je Fremd-Skill einholen.
125. `tasks/125-provider-data-privacy.md` — Datenweitergabe je Anbieter erklären und kontrollieren.
126. `tasks/126-app-lock-and-device-security.md` — Vertrauliche Ansichten gegen unbeabsichtigten Zugriff schützen.
127. `tasks/127-delete-and-retention-controls.md` — Daten löschen und Aufbewahrung einstellen.
128. `tasks/128-security-test-suite.md` — Sicherheitsfälle automatisiert und am Gerät prüfen.

## Fähigkeiten, Helfer und externe Werkzeuge
129. `tasks/129-skills-list-and-discovery.md` — Skills anzeigen und für jede Aufgabe global passende Fähigkeiten suchen.
130. `tasks/130-skill-compatibility.md` — Skill-Formate und Herkunft untersuchen, ohne unsichere Inhalte auszuführen.
131. `tasks/131-skill-global-install-workflow.md` — Globale Installation nur nach konkreter Nutzerfreigabe unterstützen.
132. `tasks/132-subagent-specialists.md` — Begrenzte Spezialhelfer für getrennte Aufgaben ermöglichen.
133. `tasks/133-subagent-permissions.md` — Daten- und Werkzeugrechte von Helfern einzeln begrenzen.
134. `tasks/134-mcp-tool-connections.md` — MCP-Verbindungen nur nach Anbieter-/Projektprüfung einrichten.
135. `tasks/135-mcp-permission-and-data-view.md` — MCP-Werkzeuge, Rechte und ausgehende Daten sichtbar machen.

---

# Ende der Haupt-Spezifikation
