# Skill-Zuordnung für die 135 Aufgaben

**Zweck:** Für jeden Task sind mindestens zwei konkrete Fähigkeiten vorgesehen. Ein Name in der Matrix ist kein Beleg, dass eine Fähigkeit in einer Sitzung tatsächlich geladen wurde: vor Start Verfügbarkeit prüfen, beide Fähigkeiten aufrufen/laden und ihr Ergebnis im Checkpoint nennen. Globale Installationen nur nach Nutzerfreigabe.

## Quellen und Status

### Global installiert und geprüft

- `swarm-planner` — `am-will/swarms`; ermittelt Abhängigkeiten und Arbeitswellen.
- `parallel-task` — `am-will/swarms`; führt unabhängige Aufgaben in Wellen aus und erwartet danach Prüfung.
- `adaptive` — offizielles Repository `android/skills`; adaptive Android-Oberflächen.
- `android-profiler` — offizielles Repository `android/skills`; Android-Leistungs-/Speichermessung.
- `android-permissions-security` — offizielles Repository `android/skills`; Android-Berechtigungen und Komponenten absichern.
- `testing-setup` — offizielles Repository `android/skills`; Android-Teststrategie und Prüfaufbau.

Diese sechs wurden mit Skill-CLI für Claude Code global installiert. Die CLI meldete für alle „Safe“ und 0 Socket alerts; `swarm-planner` meldete zusätzlich „Med Risk“ bei Snyk. Alle Skills laufen mit Agentenrechten und müssen vor Nutzung erneut gelesen werden.

### Claude Code integrierte Skills (nicht global installiert)

- `/verify` — integrierter Build-/Laufzeitprüfungs-Skill, nur wenn die installierte Claude-Code-Version ihn bereitstellt.
- `/code-review` — integrierter Review-Skill, nur wenn verfügbar.
- `/claude-api` — integrierter Anthropic-API-Referenzskill, nur wenn verfügbar.

Laut offizieller Skill-Dokumentation sind Skills über `/name` aufrufbar; integrierte Skills und Versionsvoraussetzungen können sich ändern. Verfügbarkeit zur Laufzeit prüfen. Fehlt ein Kandidat, einen bestätigten Ersatz aus dem installierten Set wählen und protokollieren. Keine Lücke mit erfundenem Namen füllen.

## Zuordnung

`Task | Skill 1 | Skill 2 | Grund / vorgesehene Nutzung`

| ID | Skill 1 | Skill 2 | Einsatz |
|---|---|---|---|
| 001 | `/swarm-planner` | `/code-review` | Abhängigkeiten planen; Anforderungskonflikte prüfen. |
| 002 | `/swarm-planner` | `testing-setup` | Quellen-/Prüfplan organisieren; Belegchecks festlegen. |
| 003 | `/swarm-planner` | `/code-review` | Marken-/Rechtsgrenzen in Tasks abbilden; Widersprüche prüfen. |
| 004 | `/claude-api` | `/code-review` | Anthropic-API-Quellen prüfen; Auth-Grenzen gegenlesen. |
| 005 | `/claude-api` | `/code-review` | Anbieter-Authentifizierung belegen; Matrix auf unzulässige Wege prüfen. |
| 006 | `android-profiler` | `testing-setup` | A56-Leistungsdaten erfassen; reproduzierbaren Gerätetest planen. |
| 007 | `android-profiler` | `testing-setup` | Beschleunigung messen; Ergebnisse reproduzierbar verifizieren. |
| 008 | `/swarm-planner` | `android-profiler` | Telefon-Baupfad planen; Gerätebelastung messen. |
| 009 | `/swarm-planner` | `/code-review` | Projektstruktur nach Abhängigkeiten planen; Struktur prüfen. |
| 010 | `adaptive` | `testing-setup` | App-Grundoberfläche und Gerätegrößen; Starttests definieren. |
| 011 | `android-profiler` | `testing-setup` | On-device-Bauressourcen messen; Bauweg testen. |
| 012 | `/swarm-planner` | `android-permissions-security` | Cloud-Bauoptionen vergleichen; Zugriffs-/Datenrisiko prüfen. |
| 013 | `adaptive` | `/code-review` | Einrichtung an kleine Displays anpassen; Ablauf auf Verständlichkeit prüfen. |
| 014 | `adaptive` | `testing-setup` | Sprachauswahl adaptiv darstellen; Locale-Fälle testen. |
| 015 | `adaptive` | `testing-setup` | Manuellen Wechsel gestalten; Wechsel und Persistenz testen. |
| 016 | `adaptive` | `android-permissions-security` | Einstellungen mobil darstellen; sicherheitskritische Optionen prüfen. |
| 017 | `android-permissions-security` | `testing-setup` | Berechtigungen sparsam anfordern; Erlauben/Ablehnen testen. |
| 018 | `android-permissions-security` | `android-profiler` | Hintergrunddienst korrekt begrenzen; Dauerlast messen. |
| 019 | `/swarm-planner` | `/code-review` | Markenaufgaben koordinieren; Nicht-Imitationsgrenzen prüfen. |
| 020 | `adaptive` | `/code-review` | Symbol in Launchergrößen gestalten; Marken-/Lesbarkeit prüfen. |
| 021 | `adaptive` | `/code-review` | Mobilgrafiken und Alternativtexte; Eindeutigkeit prüfen. |
| 022 | `adaptive` | `/code-review` | Banner responsive planen; Rechte-/Darstellungsprüfung. |
| 023 | `adaptive` | `testing-setup` | Kontrast und Statusfarben; Kontrasttests festlegen. |
| 024 | `adaptive` | `testing-setup` | Schrift/Abstände gestalten; Schriftvergrößerung testen. |
| 025 | `adaptive` | `/code-review` | Touch-Navigation entwerfen; Wege und Sicherheitsknöpfe prüfen. |
| 026 | `adaptive` | `testing-setup` | A56-Layouts anpassen; Hochformat/Tastaturfälle testen. |
| 027 | `adaptive` | `testing-setup` | Eingabefeld/IME gestalten; Tastaturzustände testen. |
| 028 | `adaptive` | `testing-setup` | Themen gestalten; Hell-/Dunkelzustände prüfen. |
| 029 | `adaptive` | `testing-setup` | Barrierearme Oberfläche; Screenreader-/Skalierungstests planen. |
| 030 | `adaptive` | `/code-review` | Lade-/Fehleransichten; Meldungen auf Eindeutigkeit prüfen. |
| 031 | `adaptive` | `testing-setup` | Chatliste bauen; Leer-/Vollzustände testen. |
| 032 | `testing-setup` | `/code-review` | Suche/Filter planen; Lösch- und Datenschutzfälle reviewen. |
| 033 | `adaptive` | `testing-setup` | Chataktionen mobil darstellen; Persistenz/Archiv prüfen. |
| 034 | `android-permissions-security` | `testing-setup` | Export-/Löschpfade absichern; Datenlöschung testen. |
| 035 | `adaptive` | `testing-setup` | Eingabe gestalten; Senden/Doppeltippen/Abbrechen prüfen. |
| 036 | `/claude-api` | `testing-setup` | Streaming-Verhalten verstehen; Abbruch/Netzfehler testen. |
| 037 | `adaptive` | `/code-review` | Codeanzeige gestalten; unverändertes Kopieren prüfen. |
| 038 | `testing-setup` | `android-permissions-security` | Retry/Stop testen; Nebenwirkungen und Freigaben prüfen. |
| 039 | `testing-setup` | `android-permissions-security` | Sitzungen fortsetzen; Wiederholung geschützter Aktionen testen. |
| 040 | `android-permissions-security` | `testing-setup` | Projektgrenzen wahren; Projektwechsel-/Datenlecktests. |
| 041 | `adaptive` | `/code-review` | Modell-/Kostenangaben anzeigen; Schätzungen und unbekannte Werte prüfen. |
| 042 | `android-permissions-security` | `testing-setup` | Datenschutzsteuerung; Transfer-/Löschtests. |
| 043 | `/swarm-planner` | `/claude-api` | Anbieterkatalog strukturieren; offizielle Authentifizierung prüfen. |
| 044 | `adaptive` | `android-permissions-security` | Anbieterformular gestalten; Schlüssel-/Zielschutz prüfen. |
| 045 | `android-permissions-security` | `testing-setup` | Schlüsselablage absichern; Verschlüsselungs-/Löschtests. |
| 046 | `android-permissions-security` | `testing-setup` | Geheimnisfilter; Lecktests in Logs/Export. |
| 047 | `/claude-api` | `testing-setup` | Provider-Testanfrage; Fehler-/Abrechnungsfälle testen. |
| 048 | `/claude-api` | `testing-setup` | Claude API integrieren; API-Format/Fehler verifizieren. |
| 049 | `/claude-api` | `testing-setup` | OpenRouter-API nach offizieller Doku; Adapter testen. |
| 050 | `/swarm-planner` | `/claude-api` | OpenCode-Rolle abgrenzen; erlaubte Schnittstelle prüfen. |
| 051 | `/swarm-planner` | `/claude-api` | Antigravity-Zulässigkeit ermitteln; Authentifizierung prüfen. |
| 052 | `android-permissions-security` | `testing-setup` | Custom endpoint absichern; HTTPS/Host/Redirects testen. |
| 053 | `/claude-api` | `testing-setup` | Anthropic-Format abbilden; Protokolltests durchführen. |
| 054 | `/claude-api` | `testing-setup` | kompatibles Nachrichtenformat abbilden; Adaptertests durchführen. |
| 055 | `adaptive` | `testing-setup` | Modellliste darstellen; Verfügbarkeit/Manuelleingabe prüfen. |
| 056 | `android-permissions-security` | `testing-setup` | Schlüsselrotation; Alt-/Neuschlüsselzustände testen. |
| 057 | `android-permissions-security` | `testing-setup` | Deaktivierung/Löschen absichern; Verlaufserhalt testen. |
| 058 | `adaptive` | `/code-review` | Anbieterfehler erklären; Geheimnisfreiheit/Handlungshinweise prüfen. |
| 059 | `android-permissions-security` | `testing-setup` | TLS/Redirects absichern; Fehlerzertifikatstests. |
| 060 | `/swarm-planner` | `/code-review` | Prüfkalender planen; veraltete Aussagen aufdecken. |
| 061 | `/claude-api` | `testing-setup` | Modellfähigkeiten aus Quellen abbilden; Capability-Tests. |
| 062 | `adaptive` | `testing-setup` | Modellwahl gestalten; Anbieterwechsel bestätigen. |
| 063 | `android-permissions-security` | `testing-setup` | Fallback-Consent absichern; Wechsel-/Kostenfälle testen. |
| 064 | `/claude-api` | `testing-setup` | Preisdatenquelle prüfen; Schätzungsdarstellung testen. |
| 065 | `testing-setup` | `android-permissions-security` | Limits/Warnungen prüfen; keine falsche Sperrzusage. |
| 066 | `android-permissions-security` | `testing-setup` | Datenvorschau/Empfänger; Übertragungsumfang testen. |
| 067 | `android-permissions-security` | `testing-setup` | Ausschlussregeln sichern; Geheimnisdateien testen. |
| 068 | `/claude-api` | `android-permissions-security` | Kontext an Anbieter begrenzen; Datenminimierung kontrollieren. |
| 069 | `testing-setup` | `android-permissions-security` | Offlinezustände testen; ungewollte Warteschlange verhindern. |
| 070 | `/claude-api` | `/code-review` | Anbieterneutralen Vertrag prüfen; fehlende Fähigkeiten sichtbar machen. |
| 071 | `/swarm-planner` | `/code-review` | Schritte/Abhängigkeiten planen; Planlücken reviewen. |
| 072 | `android-permissions-security` | `testing-setup` | Toolaufrufe autorisieren; Erlaubnisgrenzen testen. |
| 073 | `testing-setup` | `android-permissions-security` | Laufzustände testen; doppelte Aktionen verhindern. |
| 074 | `/claude-api` | `android-permissions-security` | Kontext reduzieren; Ausschlüsse gegenprüfen. |
| 075 | `adaptive` | `testing-setup` | Fortschritt mobil anzeigen; Status/Abbruch testen. |
| 076 | `/code-review` | `testing-setup` | Ergebnisberichte prüfen; Teststatus korrekt ausweisen. |
| 077 | `/swarm-planner` | `parallel-task` | Alternative Läufe planen; getrennte Ausführung koordinieren. |
| 078 | `android-profiler` | `parallel-task` | Ressourcenlimits messen; unabhängige Teilaufgaben koordinieren. |
| 079 | `testing-setup` | `android-permissions-security` | Retry-Fälle testen; Nebenwirkungen absichern. |
| 080 | `/claude-api` | `testing-setup` | Fähigkeiten/Fallbacks abbilden; keine stillen Wechsel testen. |
| 081 | `adaptive` | `testing-setup` | Projektübersicht gestalten; Berechtigungszustände testen. |
| 082 | `android-permissions-security` | `testing-setup` | Android-Dateiauswahl korrekt nutzen; Ablehnen/Abbrechen testen. |
| 083 | `android-permissions-security` | `testing-setup` | Dauerhafte URI-Zugriffe sichern; Widerruf testen. |
| 084 | `android-permissions-security` | `testing-setup` | ZIP-Importgrenzen prüfen; manipulierte Pfade testen. |
| 085 | `android-permissions-security` | `android-profiler` | USB-Zugriff absichern; Transferleistung messen. |
| 086 | `android-permissions-security` | `testing-setup` | USB-Abbruch schützen; ungespeicherte Änderung testen. |
| 087 | `android-profiler` | `testing-setup` | lokale Suche optimieren; große Ordner/Abbruch testen. |
| 088 | `android-permissions-security` | `testing-setup` | Dateivorschau absichern; Binär-/Großdateien testen. |
| 089 | `/code-review` | `adaptive` | Diff auf Korrektheit prüfen; mobile Vergleichsansicht gestalten. |
| 090 | `android-permissions-security` | `testing-setup` | Dateiannahme absichern; Teilfreigabe testen. |
| 091 | `testing-setup` | `android-permissions-security` | Konfliktfälle testen; Überschreiben blockieren. |
| 092 | `android-profiler` | `testing-setup` | Speicher-/Scanleistung messen; Ausschlüsse testen. |
| 093 | `/code-review` | `android-permissions-security` | Projektanweisungen reviewen; Vertrauensgrenzen durchsetzen. |
| 094 | `android-permissions-security` | `/code-review` | Anweisungsherkunft sichern; Rechteerweiterung prüfen. |
| 095 | `android-permissions-security` | `testing-setup` | Git-Credentials begrenzen; Widerruf/Lecktests. |
| 096 | `testing-setup` | `android-permissions-security` | Clone-Zustände testen; Inhalte als nicht vertrauenswürdig behandeln. |
| 097 | `/swarm-planner` | `android-permissions-security` | Repository-Einrichtung planen; Sichtbarkeit/Scopes prüfen. |
| 098 | `/code-review` | `testing-setup` | Änderungsliste prüfen; neue/gelöschte Dateien testen. |
| 099 | `/parallel-task` | `/code-review` | Tasks in Blöcken koordinieren; Commit-Inhalt reviewen. |
| 100 | `android-permissions-security` | `testing-setup` | Geheimnis-Scan absichern; synthetische Trefferfälle testen. |
| 101 | `android-permissions-security` | `testing-setup` | Push-Freigabe erzwingen; Remote-/Zieldrift testen. |
| 102 | `/code-review` | `testing-setup` | Konfliktlösung prüfen; Originalstände erhalten. |
| 103 | `/code-review` | `testing-setup` | Verlauf/Diffs prüfen; sensible Daten maskieren. |
| 104 | `testing-setup` | `android-permissions-security` | Wiederaufnahme prüfen; Teilübertragungen absichern. |
| 105 | `android-permissions-security` | `android-profiler` | Android-Befehlspfad bewerten; Speicher/Leistung messen. |
| 106 | `android-permissions-security` | `testing-setup` | Arbeitsordnergrenze durchsetzen; Pfadausbruch testen. |
| 107 | `adaptive` | `android-permissions-security` | Befehlsvorschau mobil gestalten; Aktion vor Freigabe blockieren. |
| 108 | `android-permissions-security` | `testing-setup` | Freigabestufen absichern; Stufen-/Widerrufstests. |
| 109 | `android-permissions-security` | `/code-review` | Ausnahme-Modus begrenzen; UI und gefährliche Übergänge prüfen. |
| 110 | `android-permissions-security` | `testing-setup` | Risikomuster prüfen; False-positive/negative-Fälle testen. |
| 111 | `adaptive` | `android-permissions-security` | Ausgabe mobil anzeigen; geheime Parameter maskieren. |
| 112 | `testing-setup` | `android-permissions-security` | Testläufe strukturieren; Projektcode vor Start prüfen. |
| 113 | `android-permissions-security` | `android-profiler` | Benachrichtigung/Dienst absichern; Dauerlast messen. |
| 114 | `android-permissions-security` | `testing-setup` | Abbruch/Prozessende absichern; Cleanup testen. |
| 115 | `android-permissions-security` | `testing-setup` | Aktionsprotokoll minimieren; Maskierungs-/Löschtests. |
| 116 | `android-permissions-security` | `testing-setup` | Abhängigkeiten absichern; Zustimmung/Fehlinstallation testen. |
| 117 | `adaptive` | `android-permissions-security` | Freigaben mobil sichtbar; Widerrufe technisch durchsetzen. |
| 118 | `android-permissions-security` | `testing-setup` | Freigabeverlauf datensparsam; Widerruf nachweisbar testen. |
| 119 | `android-permissions-security` | `testing-setup` | Projektgrenze erzwingen; jede Werkzeugart testen. |
| 120 | `android-permissions-security` | `testing-setup` | Pfadangriffe abwehren; Grenzfälle automatisiert testen. |
| 121 | `android-permissions-security` | `testing-setup` | Links/Sonderdateien absichern; Umgehungstests. |
| 122 | `android-permissions-security` | `/code-review` | Vertrauensgrenzen schützen; Angriffsanweisungen reviewen. |
| 123 | `android-permissions-security` | `/swarm-planner` | Skill-Supply-Chain prüfen; Auswahl/Gates planen. |
| 124 | `android-permissions-security` | `testing-setup` | Skill-Installation bestätigen; Scope/Überschreiben testen. |
| 125 | `/claude-api` | `android-permissions-security` | Anbieterdatenfluss belegen; Transfergrenzen prüfen. |
| 126 | `android-permissions-security` | `testing-setup` | App-/Geräteschutz planen; Benachrichtigungen testen. |
| 127 | `android-permissions-security` | `testing-setup` | Löschung/Aufbewahrung sichern; Restore/Löschtests. |
| 128 | `testing-setup` | `android-permissions-security` | Sicherheits-Testplan erstellen; kritische Freigabefälle abdecken. |
| 129 | `/swarm-planner` | `android-permissions-security` | Skill-Zuordnung planen; Installation von Entdeckung trennen. |
| 130 | `android-permissions-security` | `/code-review` | Skill-Kompatibilität/Supply Chain bewerten; Inhalte prüfen. |
| 131 | `android-permissions-security` | `testing-setup` | Installation/Wiederherstellung prüfen; globale Änderungen testen. |
| 132 | `/swarm-planner` | `parallel-task` | Spezialaufgaben zerlegen; unabhängige Helfer koordinieren. |
| 133 | `android-permissions-security` | `testing-setup` | Helferrechte begrenzen; Rechteisolation testen. |
| 134 | `android-permissions-security` | `/claude-api` | Externe Werkzeuge authentifizieren; Protokoll dokumentieren. |
| 135 | `android-permissions-security` | `testing-setup` | MCP-Daten/Rechte kontrollieren; Änderungs-/Widerruffälle testen. |

## Nutzungsregel

Für jeden Task beide Skills tatsächlich verfügbar machen und laden. Die Aufgabenpläne enthalten zunächst nur Kandidaten und Testabsicht, keine Behauptung, dass jeder Skill individuell installiert wurde oder schon benutzt worden ist. In `progress/BUILD-STATE.md` nach jedem Task die geladenen Skills und ihre konkrete Verwendung notieren. Wenn ein integrierter `/name`-Skill in dieser Claude-Code-Version fehlt, anhand offizieller oder nach Nutzerfreigabe recherchierter Quelle ersetzen.
