# Claudroide — Arbeitsplan für Claude Code

**Stand:** 2026-10-08 · **Grundlage:** `claudroide-spec.md` — die **einzige gültige Gesamtspezifikation** (besonders §19 Freigaben, §21–§28 Änderungen, §29 Start/Wiederaufnahme)
**Start:** Terminal im Ordner `/home/mert/claudroide-next`, `claude` starten, Arbeitsauftrag aus **Spec §29.7** einfügen. Nach Absturz: `WEITERARBEITEN` (§29.8). Betriebsanleitung: `CLAUDE.md`.
**Regel:** Jede Aufgabe ist **einzeln abarbeitbar**. Eine Aufgabe gilt erst als fertig, wenn **Verifikation und Akzeptanzkriterium** erfüllt und im `progress/BUILD-STATE.md` ehrlich eingetragen sind. Kein „fertig" bei nur kompiliertem Code.

**Spalten:** ID · Aufgabe & Zweck (warum es sie gibt) · Konkret (Änderungen/Dateien) · Abhängig von · Fertig, wenn (Verifikation + Akzeptanz) · Wiederverwendung aus Quellen · Risiko

**Harte Grenzen für alle Aufgaben:** max. **1 Subagent** und **1 Shell-Befehl** gleichzeitig (A56 stürzt sonst ab) · nur ein Build gleichzeitig (2,2 GB frei) · Quellen read-only · Banner/Abbild unverändert · keine Schlüssel in Git/Protokoll · Not-Aus immer wirksam.

---

## Phase 0 — Bestandsaufnahme (kein Produktcode)

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **0.1** | Projektgerüst und Doku-Ebenen anlegen, damit alles einen festen Ort hat | `docs/`, `progress/`, `tasks/`, `_sources/`; `docs/REQUIREMENTS.md`, `ARCHITECTURE.md`, `ROADMAP.md`, `SECURITY.md`, `DECISIONS.md`, `SOURCE_REUSE_MATRIX.md` aus der Spec ableiten | — | Alle Dateien existieren; jede Aussage aus der Spec ableitbar; keine erfundenen Zahlen | Doku-Aufbau aus `claude-code-android/docs` (15 Ebenen) | Doku-Wildwuchs → Spec §10 begrenzt die Menge **bewusst** |
| **0.2** | Quellen erheben und Index füllen (lokal zuerst, sonst klonen) | `_sources/local/*` verweisen, `_sources/github/*` flach klonen laut Spec §22; `docs/SOURCE_INDEX.md` mit echten Pfaden füllen | 0.1 | Jede Quelle in §22 hat Pfad **oder** begründetes „nicht vorhanden"; keine Quelle verändert | `clawscreen/tools/build-reference-repos.py` | Privat-Repos versehentlich ins öffentliche Repo → Prüfpunkt vor jedem Push |
| **0.3** | Ist-Stand **selbst messen** (nicht glauben) | `./gradlew :app:testDebugUnitTest` im alten Repo; Zahl der Tests, APK-Größe, SDK-Versionen notieren | 0.1 | Gemessene Zahl im Checkpoint; Abweichung zu „2535/2530" dokumentiert | `claudroide/progress/BUILD-STATE.md` | Sehr langsam bei wenig Speicher → nur **ein** Lauf, danach aufräumen |
| **0.4** | Ungeordnete Arbeit im alten Ordner bewerten | `git status`/`diff` in `~/claudroide` lesen; die 13 Dateien in `feature/control` + 5 Testdateien prüfen; Vorschlag „übernehmen/verwerfen" | 0.3 | Schriftliche Einzelbewertung je Datei; **nichts** im alten Ordner geändert | Bedienungshilfe-/Gesten-Arbeit im alten Repo | Tests der neuen Teile laufen evtl. nicht → genau prüfen statt annehmen |
| **0.5** | Umgebungsprüfung für den Bau | Werkzeuge und Grenzen aus Spec §2.7 nachmessen (Java, Gradle, adb, freier Speicher, RAM) | 0.1 | Werte im Checkpoint; fehlende Werkzeuge werden **selbst installiert** (Freigabe §19.5/§27.12) mit Platzprüfung und Begründung | — | Knapper Speicher bricht Builds ab → Prüfung vor jedem langen Lauf |
| **0.6** | Container-/Terminal-Ansatz entscheiden | `claudroide/feature/linux` (83 Dateien) gegen `mobile-linux-lab` vergleichen, Entscheidung begründen | 0.2 | Entscheidung + Begründung in `docs/DECISIONS.md` | beide Quellen | Falsche Wahl kostet viel Zeit → nur die **Grundentscheidung**, kein Umbau in Phase 0 |
| **0.7** | **Skills-Index erstellen (Spec §28.6)** | Volle Liste ALLER gefundenen Skills, Plugins und Marktplätze (global: `~/.claude/skills/`, `~/.claude/plugins/`, `~/.agents/skills/`; projektbezogen; eigene Repos) nach `docs/SKILL-INDEX.md`; je Eintrag: Name, Fundort, kurze Beschreibung „wofür gut“ | 0.1 | Jeder gefundene Eintrag ist gelistet; keine erfundenen Einträge; Datei im Secret-Scan sauber | `claude-plugins-official` (Katalog-Muster) | Liste wird sofort veraltet → als lebendiges Dokument führen (Update-Pflicht nach jedem Bauabschnitt) |
| **0.8** | **Selbstlauf-/Loop-System einrichten (Spec §28.5)** | Loop-Skelett anlegen: Aufgabe wählen → bauen → prüfen → in Gedächtnis-Datei eintragen → weiter; Ziel-Check alle paar Schritte; greift auf `autonomous.sh`-Muster zurück | 0.1, 1.4 | Ein Testlauf: Loop startet, wählt Task, prüft, trägt ein, stoppt sauber am definierten Ende | `claude-code-android/tools/autonomous.sh`, Haken §24.2 | Endlosschleife bei Fehler → Blocker-Regel §4.4 und Versuchszähler gelten auch im Loop |

---

## Phase 1 — Grundgerüst (vertikaler Schnitt beginnt)

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **1.1** | Projekt kopieren und lauffähig machen | Quellcode aus `~/claudroide` nach `claudroide-next` (ohne `dist/*.apk`, `local.properties`, Build-Reste), **neue App-Kennung** `org.claudroide.next` (+`.debug`), Anzeigename „Claudroide", **gleiches Symbol** | 0.4, 0.5 | `assembleDebug` grün; beide Apps gleichzeitig installierbar; Banner/Abbild **byte-identisch** | Hauptquelle `claudroide` | Kopierfehler → Prüfsummen der Marken-Bilder vor/nach dem Umzug vergleichen |
| **1.2** | Eigene Lizenz und Herkunftsregeln | `LICENSE` (Apache 2.0) + `NOTICE` (nur Fremdteile) anlegen; Regel „eigener Code ohne Angabe, Fremdteile mit Zeile" in `CONTRIBUTING.md` | 0.1 | Lizenz vorhanden; `NOTICE` nennt nur echte Fremdteile | — | Falsche Herkunftsangabe → pro Teil einzeln entscheiden |
| **1.3** | Test- und Gate-Lauf grün | Alle Unit-Tests laufen; Zahl **selbst gemessen** in `BUILD-STATE.md` | 1.1 | Gemessene Zahl + Datum; Abweichungen erklärt | — | Sehr langsam → nur ein Lauf |
| **1.4** | Arbeitsweise-Skripte einrichten | Secret-Scan, Doku-/Quellen-Prüfung, Haken vor/nach Befehlen, Selbstlauf-Skript. **Hinweis: `CLAUDE.md`, `progress/BUILD-STATE.md` und `docs/DECISIONS.md` existieren bereits** (2026-10-08 angelegt) — nur ergänzen, nicht neu bauen | 0.1 | Jedes Skript läuft einmal erfolgreich und ist in `CLAUDE.md` verlinkt | `claude-code-android/tools/*`, `scripts/*`, `claudroide/tools/*` | Werkzeuge erwarten Bun/Node → prüfen, sonst nachbauen |
| **1.5** | Bau-Schleife und Zustandsdatei festschreiben | `CLAUDE.md` mit §16-Ablauf, §19-Freigaben wortgleich; `CLAUDROIDE_CURRENT_STATE.md` mit Kopf (Stand, letzte Tasks, nächster Task, Blocker, Entscheidungen, Quellen, Tests, Grenzen) | 0.1 | Wiederaufnahme funktioniert: App neu starten → Lauf setzt am richtigen Task fort | `clawdroide-resume`-Muster aus altem Repo | Zustandsdatei veraltet → Pflicht-Update nach **jedem** Block |
| **1.6** | Marke und Design-Token sichern | Banner + Symbol byte-identisch übernehmen; Design-Token aus `core/design` behalten; Hell/Dunkel + zwei Farbtöne (grün/beige, warm-rötlich) | 1.1 | Byte-Vergleich identisch; beide Farbtöne in hell/dunkel sichtbar | `core/design/*` | Ungenehmigte Bildänderung → nur mit Nutzer-OK |

---

## Phase 2 — Chat, Dienste, Schlüssel (MUSS-Kern)

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **2.1** | Keystore-Schlüsselspeicher hart machen | Bestehenden Vault prüfen/ergänzen; Maskierung in UI und Protokoll | 1.1 | Schlüssel **nie** im Klartext (Test); Protokoll enthält keine Fragmente | `feature/provider/SecureKeyStore.kt`, `SecretMasker.kt` | Leck über Protokoll → Redaction-Test |
| **2.2** | Drei Mindest-Dienste anbinden | Anthropic, OpenAI, OpenRouter über Wire-Format-Schicht; Testknopf je Dienst | 2.1 | Echter Testaufruf je Dienst liefert Antwort; Fehlerfall wird ehrlich gezeigt | `AnthropicMessageFormat`, `OpenAiMessageFormat`, `ProviderTransport` | Keine Anthropic-/OpenAI-Schlüssel vorhanden → ehrlich als „nicht geprüft" führen |
| **2.3** | Ein-Klick-Gratis-Dienste | OpenRouter-Free und Kilo-Free als Ein-Klick im Setup | 2.1 | Ohne Guthaben Antwort möglich; Anmeldung/Key nur einmal nötig | Muster aus `claude-media-bridge` | Dienst ändert Bedingungen → Datum + Prüfergebnis notieren |
| **2.4** | OmniRoute finden und fragen | Suche auf `127.0.0.1:20128`, Liste zeigen, **fragen**, dann übernehmen | 2.1 | Nutzer wird gefragt; nichts wird still übernommen | `droidroute`-Handbücher | Fremder Anschluss → **20128 nie** selbst belegen |
| **2.5** | Automatische Modellwahl | Rollen `role:fast`, `role:coder`, `role:vision`, `role:summary` auf konkrete Modelle auflösen | 2.2 | Jede Rolle liefert plausible Antwort; Wahl im Protokoll sichtbar | `ModelSelectionManager`, `ModelCapabilityRegistry` | Falsche Rolle → Test je Rolle |
| **2.6** | Ausfall-Ersatz (Failover) | 2–3 Versuche, dann nächster Dienst; Wechsel sichtbar; Text geht nie verloren | 2.5 | Absichtlich kaputter Dienst → Lauf läuft weiter, Wechsel protokolliert | `ModelFallbackPolicy` | Endlosschleife → Versuchszähler begrenzen |
| **2.7** | Kostenanzeige ohne Limit | Tages-/Monatsübersicht je Dienst; keine Warnung, kein Stopp | 2.2 | Kosten sichtbar; nicht Erhebbares steht als „nicht verfügbar" | `CostEstimator`, `CostLabelPresenter` | Erfundene Kosten → nur Gemessenes anzeigen |
| **2.8** | Chat-Oberfläche mit DISCUSS/BUILD | Umschalter, Statuszeile, einklappbares Protokoll, Anhänge (Bild/Datei/Video speichern) | 1.6 | Umschalten sichtbar; DISCUSS zeigt **keine** Datei-/Shell-Werkzeuge | `feature/chat/*`, `core/design/*` | Zu viele Elemente → einfache Ansicht als Standard |
| **2.9** | Beim ersten Start fragen und erklären | Begrüßung mit Banner, Frage nach alten Gesprächen (Import), Rechte-Übersicht mit Sprung in die Einstellung, Geräteprüfung + Modellvorschlag | 2.3 | Alle vier Schritte laufen einmal durch; Ablehnen eines Rechts lässt Rest nutzbar | `OnboardingScreen`, `PermissionCenter` | Import alter Daten scheitert → ehrlich melden, nichts still verwerfen |

---

## Phase 3 — Coding-Agent vertikal (MUSS)

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **3.1** | Grenzen hart verankern | Sperre: **1 Shell**, **1 Subagent**; Zustand im UI sichtbar; zweite Anfrage wartet/abgelehnt | 2.8 | Ein Test startet zwei Jobs → der zweite wird abgewiesen (nicht nur im Prompt!) | `AgentConcurrencyLimiter` | Nur als Text geregelt → **muss** im Code stehen |
| **3.2** | Agenten-Schleife (Dateien, Shell, Diff) | Werkzeuge lesen/schreiben/löschen im Projektordner; Shell-Ausführung leicht + Container wählbar; Diffs anzeigen | 3.1 | Ein echter kleiner Auftrag läuft durch; Änderung sichtbar | `AgentToolLoop`, `AgentTaskPlanner`, `ProjectScreen` | Projektgrenze verletzt → `ProjectBoundaryEnforcer`-Tests |
| **3.3** | Verifikation vor „fertig" | Test-/Build-Erkennung, Ergebnisprüfung, ehrlicher Bericht | 3.2 | Falsch „fertig" wird erkannt: absichtlich kaputter Test → Lauf meldet Fehler | `ProjectTestRunPolicy`, `AgentRunState` | Behauptete Fertigstellung → Testfall mit Absicht |
| **3.4** | Sicherungen und Rückgängig | Vor jeder Änderung sichern; unbegrenzt rückgängig; Speicher voll → pausieren + fragen | 3.2 | Löschen → Rückgängig stellt Datei wieder her; voller Speicher pausiert | Sicherungs-Logik aus altem Repo | Datenverlust → Sicherung **vor** Schreiben |
| **3.5** | Blocker-Regel und Weiterarbeit | Nach 2 Fehlversuchen: notieren, nächste Aufgabe, am Ende melden; fehlende Dinge überspringen statt anhalten | 3.3 | Lauf endet **nie** still; Blocker stehen im Checkpoint | `AgentRetryPolicy` | Endlose Reparaturversuche → Zähler |
| **3.6** | Beweis-Lauf 1 (kleine Webseite) | Agent baut selbstständig eine kleine Webseite, zeigt sie in der Vorschau, prüft sie | 3.2–3.5 | Screenshot der Vorschau + Prüfprotokoll + Eintrag im Checkpoint | — | Gerät fehlt → Beweis im Vorschau-Bereich der App ist auch ohne Kabel möglich; sonst offen markieren |

---

## Phase 4 — Kontext und Zustand (MUSS)

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **4.1** | Kontextmanager | Verbrauch messen, große Ausgaben kürzen, zusammenfassen, bei Bedarf sauber neu starten mit Gedächtnis-Datei | 3.2 | Lauf über der Grenze → Arbeit geht **nicht** verloren; Fortsetzung korrekt | `AgentContextManager`, `context`-Bereich aus `lokicode` | Verlust bei Rotation → Test mit Absicht |
| **4.2** | Zustand auf Platte | Aufgabe, Plan, Fortschritt, Entscheidungen, Blocker, letzte Dateien nach jedem Schritt schreiben | 4.1 | App-Neustart → Lauf macht am selben Punkt weiter | `SessionResumptionManager` | Veraltete Zustandsdatei → Pflicht-Update |
| **4.3** | Nutzer-Präferenzen | Dauerhafte Wunsch-Datei; **sichtbares** Dazulernen aus Korrekturen; Profi-Ansicht zeigt Einträge zum Löschen | 4.2 | Korrektur → Eintrag erscheint sichtbar; Nutzer kann ihn löschen | `AppSettings` | Heimliches Mitschreiben → ausdrücklich verboten |

---

## Phase 5 — Projekte, Dateien, Git (MUSS)

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **5.1** | Projektordner und Grenzen | App-eigener Ordner als Standard, fremder Ordner per Auswahl; keine Verknüpfung nach außen; Suche im offenen Projekt | 3.2 | Versuch, außerhalb zu schreiben, wird abgewiesen (Test) | `PathBoundaryGuard`, `PersistentFolderAccess` | Pfad-Ausbruch (`../`) → Testfälle |
| **5.2** | Dateiansicht mit Farben und Bearbeiten | Syntaxfarben, Bearbeiten, KI-Änderung daneben | 5.1 | Datei öffnen, ändern, speichern, Diff sehen | `CodeBlockRenderer`, `ChangeReview` | Große Dateien → kürzen, nicht hängen |
| **5.3** | Git im Projekt | Änderungsliste, Commit-Vorschlag, Verlauf, Konflikt-Behandlung, Schlüssel-Scan vor Commit | 5.1 | Commit im Projekt möglich; Schlüssel-Scan blockt Testschlüssel | `feature/git/*` | Versehentlicher Push → Freigabe-Regel §17 |
| **5.4** | GitHub-Sicherung (standardmäßig aus) | Pro Projekt einschaltbar; zeigt vorher **was** hochgeladen würde; Gespräche/Anhänge nur nach Einschalten; Schlüssel nie | 5.3 | Einschalten sichtbar; Vorschau korrekt; ausgeschaltet passiert nichts | `GitRepositoryTarget`, `GitPushApproval` | Datenleck → Vorschau + Secret-Scan als Pflicht |

---

## Phase 6 — Bildschirm-Agent (MUSS)

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **6.1** | Zugangshilfe in der App | Eigener `AccessibilityService`, Element-Liste lesen, Gesten (tap/swipe/zurück/home/recents) | 1.1 | Am Gerät: Element-Liste kommt an; Geste wirkt | **ungeordnete Arbeit** aus `~/claudroide/feature/control` (13 Dateien, prüfen!) | Gerät fehlt → nur Prüfung ohne Gerät möglich; Gerätetest offen führen |
| **6.2** | Bild + Liste zusammen | Immer Screenshot **und** Liste; Bild als Augen, wenn die Liste leer ist | 6.1 | Grafik-App (Musik-Programm) wird über das Bild bedient | `ScreenDiff`, `ScreenTreeParser` | Zu langsam/teuer → Bild verkleinern, Wiederholungen erkennen |
| **6.3** | Text mit Umlauten | Zwischenablage-Weg, Rückfall auf einfache Zeichen mit Hinweis | 6.1 | „ä ö ü ß" kommt richtig an | zweiseitiges Verfahren aus `clawscreen` | Falscher Text → Vorher/Nachher prüfen |
| **6.4** | Not-Aus dreifach | Knopf oben, schwebender Knopf, Leiser-Taste zweimal | 6.1 | Alle drei stoppen sofort; nichts startet danach von selbst | Nothalt-Muster aus `clawscreen` | Fehlauslösung → Doppeldruck mit Zeitfenster |
| **6.5** | Sperrliste | Standard: Geld, Banking, Bezahlung, Passwörter; erweiterbar; gesperrte App: **kein** Tippen, **kein** Bild, **kein** Senden | 6.1 | Test-App in Sperrliste wird abgewiesen | Sicherheitskern aus altem Repo | Umgehung → Prüfung **vor** jeder Aktion |
| **6.6** | ADB-Notweg | Ein adb-Server (5037), kabellose Fehlersuche, scrcpy-Bild + Tippen, Rückfall auf `screencap`/`input` | 6.1 | Aus Termux: Bild holen, tippen, App starten | `clawscreen` (gemessen), scrcpy | Zweiter adb-Server → ausdrücklich verboten |
| **6.7** | Beweis-Lauf 2 | Erst Browser (Seite aufrufen + prüfen), dann Musik-App (Titel starten + prüfen) | 6.1–6.6 | Screenshots vor/nach + Protokoll + Checkpoint | — | Ohne Gerät nicht möglich → offen markieren, nicht behaupten |
| **6.8** | App darf Claudroides Tür nutzen | Claude Code in Termux darf über `127.0.0.1:20130` Bildschirm und Dateien nutzen | 6.6 | Aufruf aus Termux wirkt; Protokoll zeigt den Aufruf | neue Tür §12 Punkt 24 | Missbrauch → nur lokal + Prüfschlüssel |

---

## Phase 7 — Fähigkeiten, MCP, Katalog (SOLL)

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **7.1** | Fähigkeiten-Katalog | Metadaten-Katalog, Laden nur bei Bedarf; je Eintrag Trigger, Zweck, benötigte Rechte, Kontextkosten | 2.8 | Katalog sichtbar; Fähigkeit wird erst bei passender Aufgabe ganz geladen | `design-skill-library` (Muster), `feature/skills` | Kontext-Überlauf → Beschreibungen kurz halten |
| **7.2** | MCP-Anbindung | Lokal (stdio) und über Netz; Server brauchen Freigabe; Aufrufe protokolliert | 7.1 | Ein MCP-Server angeschlossen und benutzt | `feature/mcp/*`, `claude-media-bridge` | Fremder Server → Freigabe + Protokoll |
| **7.3** | Prompt-Pipeline | Erkennen → auswählen → kombinieren → anpassen → dokumentieren | 7.1 | Auswahl im Zustand nachvollziehbar | `awesome-prompts` (377 Prompts) | Wahlloses Importieren → verboten |
| **7.4** | Sicherheitsprüfung für Zusatzquellen | Herkunft, Lizenz, Stand, Sicherheitsbefund je Eintrag; bei Gefahr fragen | 7.1 | Kein Eintrag ohne Befund | `SkillSupplyChainReview` | Fremdcode → Herkunftszeile |
| **7.5** | Unbekanntere Quellen suchen (Auftrag) | Gründliche Suche nach wenig bekannten, passenden Projekten; schriftliche Liste mit Umgang je Fund | 0.2 | Liste liegt vor, auch wenn sie Zeit kostet | `design-skill-library`-Methode | Massenware → Anti-Slop-Tor |

---

## Phase 8 — Kleine Modelle: Grafik, dann Prozessor (MUSS)

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **8.1** | Geräteprüfung | Freier Speicher + RAM, Kerne, Vulkan, Android-Version, Wärme, Akku erfassen und **im Klartext** anzeigen | 1.1 | Werte stimmen mit der Wirklichkeit überein (nachmessen) | `CapabilityDetector`, `DeviceCapabilityReport` (alt + `mobile-linux-lab`) | Falsche Werte → gegen Systemwerte prüfen |
| **8.2** | Modellkatalog | Kleine Modelle (GGUF) mit Größe, Herkunft, Prüfsumme, Lizenz; Obergrenze 4 GB | 8.1 | Katalog zeigt nur Geprüftes; Herkunft je Eintrag | `DistroCatalog`-Muster (Prüfsummen) | Unsichere Herkunft → nicht aufnehmen |
| **8.3** | Download mit Fortsetzen | Einmal fragen (Größe + freier Platz), WLAN bevorzugt, Mobilfunk mit Hinweis, Abbruch fortsetzbar | 8.2 | Abbruch bei 50 % → Läuft bei 50 % weiter | `ResilientDistroDownloader`, `ChecksumVerifier` | Platzmangel → vorher prüfen und warnen |
| **8.4** | Grafik-Weg (Vulkan) | `llama.cpp` mit Vulkan in der App; Rückfall auf Prozessor bei Fehler | 8.3 | Antwort kommt; Rückfall getestet | `llama.cpp` (MIT) | Treiber instabil → Rückfall **muss** funktionieren |
| **8.5** | Prozessor-Weg als sicherer Rückfall | Reiner Prozessor-Weg, Threads auf große Kerne, Gewichte speicherschonend | 8.3 | Antwort kommt auch ohne Grafik | LiteRT/MediaPipe (Apache-2.0) | Zu langsam → kleinstes Modell wählen |
| **8.6** | Auswahl mit Begründung + Selbsttest | Aus Messwerten + Selbsttest das beste Modell wählen, in einfachen Worten begründen, Ergebnis merken | 8.4, 8.5 | Begründung für Nutzer verständlich; Ergebnis nach Neustart noch da | Speicher-/Wärme-Wächter (neu) | Überhitzung → Drosselung erkennen und berichten |
| **8.7** | Eigene Tür 20130 | Lokaler Endpunkt, nur `127.0.0.1`, Prüfschlüssel, Protokoll; Outfit gleich wie OpenAI-Standard | 8.6 | Aufruf aus Termux liefert Antwort; Protokoll zeigt ihn | `droidroute`-Muster | Offener Anschluss → nur lokal binden |

---

## Phase 9 — NPU-Forschungsblock (Experiment, stört den Hauptbau nicht)

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **9.1** | Wege prüfen | LiteRT-Samsung-Backend (Exynos AI LiteCore) und Samsung Neural SDK auf Erreichbarkeit prüfen; NNAPI **nicht** verfolgen (abgekündigt) | 8.6 | Schriftliches Ergebnis je Weg mit Quelle und Datum | LiteRT, Samsung-Seiten | Partnerfreigabe nötig → ehrlich als offen führen |
| **9.2** | Messläufe nach Messliste | Wörter/s, Zeit bis erstes Wort, Ladezeit, Speicher, Wärme, Abstürze, Wiederaufnahme | 9.1 | Messliste vollständig ausgefüllt — oder ehrliche Lücke | Messmethodik `clawscreen` | Kein Gerät → Block offen lassen, nicht behaupten |
| **9.3** | Brücke bauen (falls 9.1 gelingt) | App bietet die NPU-fähige Laufzeit über 20130 an, damit auch Termux sie nutzt | 8.7, 9.2 | Aufruf aus Termux nutzt die beschleunigte Laufzeit | neue Tür | Falsche Erwartung → nur bei belegtem Erfolg bewerben |
| **9.4** | Feintuning-Experiment | Winziges Modell am Handy feinjustieren: Platzprüfung, Abbruch bei Hitze, ehrlicher Bericht | 9.2 | Entweder belegtes Ergebnis oder dokumentiertes Scheitern | — | Überhitzung/Akkulast → harte Abbruchgrenzen |

---

## Phase 10 — Absicherung, Werkzeuge, Veröffentlichung

| ID | Aufgabe & Zweck | Konkret | Abhängig von | Fertig, wenn | Wiederverwendung | Risiko |
|---|---|---|---|---|---|---|
| **10.1** | Vier Absicherungen scharf schalten | Schlüssel-/Datenprüfung, Doku-/Quellenprüfung, Haken vor/nach Befehlen, Selbstlauf | 1.4 | Jede Prüfung blockt einen absichtlichen Fehler | `claude-code-android/tools/*` | Falsch-Positiv blockt Arbeit → Meldung + Umgehungsregel dokumentieren |
| **10.2** | Anti-Massenware-Prüfung im Lauf | Textprüfer gegen Werbesprache, erfundene Zahlen, Emoji-Wolken | 10.1 | Prüfer findet eingebauten Testfall | `clawscreen/tools/anti-slop.py` | Zu streng → Liste pflegen |
| **10.3** | README und Release-Regeln | Zweisprachige README (deutsche Kurzfassung + englischer Teil), ehrlicher Stand, Rechte-Erklärung, Quellenliste | 10.1 | README nennt nur Belegtes; Veröffentlichung nach bestandenen Prüfungen | README-Regeln §18 | Übertreibung → Verbotsliste §18 |
| **10.4** | Bau im Internet einrichten | Arbeitsablauf: nach jedem Meilenstein APK bauen; Release mit Datei | 1.1 | Ein Release enthält eine installierbare Datei | Muster aus altem Repo | Kosten/Zeit → nur bei Meilensteinen |
| **10.5** | **Code-Hygiene-Lauf (Spec §28.11)** | Nach jedem Meilenstein: (a) doppelte Lösungen finden und zusammenführen, (b) toten Code entfernen (unbenutzte Dateien/Funktionen/Klassen), (c) Befund in `progress/BUILD-STATE.md` protokollieren — was weg ist und warum | 1.3 | Nach dem Lauf: Build + Tests grün; jede Entfernung ist protokolliert; nichts wurde blind gelöscht | `sync_frontmatter.py`, Anti-Slop-Prüfer | Zu aggressive Entfernung bricht Funktionalität → nur entfernen, was im Build/Tests nicht referenziert ist |
| **10.6** | **Werkzeug-Räumung (Spec §27.11)** | Selbst installierte Werkzeuge prüfen: nur bei 100%iger Sicherheit, dass nie wieder gebraucht, entfernen — mit Prüfung und Checkpoint-Notiz; Platzgewinn dokumentieren | 10.5 | Entfernte Werkzeuge sind protokolliert; nachfolgender Build läuft grün; systemnahe Bestandteile (Java 17, Android SDK) unberührt | — | Zu früh entfernt → Neuinstallation kostet Zeit; im Zweifel NICHT entfernen |

---

## Phase 11 — Später (nach Stufe 1, erst nach kurzer Rückfrage)

| ID | Aufgabe & Zweck | Abhängig von |
|---|---|---|
| **11.1** | Stufe 2: Spracheingabe, Vorlesen, Wakewort (sichtbarer Zustand, jederzeit abschaltbar) | Stufe 1 fertig (Beweis-Läufe 3.6 + 6.7) |
| **11.2** | Auswertung von Audio, Dokumenten, Videos (kurze Videos zuerst) | 11.1 |
| **11.3** | Weitere Dienste (Groq, Mistral, NVIDIA NIM, Bynara, Ollama Cloud …) und eigene Endpunkte | 2.2 |
| **11.4** | DroidRoute als optionaler Dienst anbinden (Stand vorher frisch erheben) | 11.3 |
| **11.5** | Musik-Steuerung über das AirBeat-Protokoll | 6.7 |
| **11.6** | Modell-Studio (eigene Modelle hereinholen, umwandeln, messen, bewerten) | 9.2 |
| **11.7** | Design-Fähigkeiten **verkleinert** in die App (etwa die besten 40) | 7.1 |
| **11.8** | App arbeitet im abgetrennten Bereich am eigenen Programm | 10.1 |

---

## Wellenplan (was darf gleichzeitig laufen)

| Welle | Aufgaben | Startet, wenn |
|---|---|---|
| W0 | 0.1 → 0.2 → 0.3 → 0.4 → 0.5 → 0.6 → 0.7 → 0.8 | sofort (streng der Reihe nach, weil alle auf denselben Dateien lesen) |
| W1 | 1.1 → 1.2 → 1.3 → 1.4 → 1.5 → 1.6 | W0 fertig (0.7–0.8 früh abgeschlossen) |
| W2 | 2.1 → 2.2 → 2.3 → 2.4 → 2.5 → 2.6 → 2.7 → 2.8 → 2.9 | W1 fertig |
| W3 | 3.1 → 3.2 → 3.3 → 3.4 → 3.5 → 3.6 | W2 fertig |
| W4 | 4.1 → 4.2 → 4.3 | W3 fertig |
| W5 | 5.1 → 5.2 → 5.3 → 5.4 | W4 fertig |
| W6 | 6.1 → 6.2 → 6.3 → 6.4 → 6.5 → 6.6 → 6.7 → 6.8 | W5 fertig |
| W7 | 7.1 → 7.2 → 7.3 → 7.4 (7.5 jederzeit möglich) | W6 fertig |
| W8 | 8.1 → 8.2 → 8.3 → 8.4 → 8.5 → 8.6 → 8.7 | W7 fertig |
| W9 | 9.1 → 9.2 → 9.3 → 9.4 (nur wenn Gerät vorhanden) | W8 fertig |
| W10 | 10.1 → 10.2 → 10.3 → 10.4 → 10.5 → 10.6 | parallel ab W3 möglich (10.5/10.6 laufen nach jedem Meilenstein, nie parallel zu einem Build) |
| W11 | 11.x | nach Stufe 1 + Nutzer-Rückfrage |

**Parallel-Regel:** Auf dem A56 stürzt der Agent bei zu viel Gleichzeitigkeit ab. Es gilt **immer**: ein Subagent, ein Shell-Befehl, ein Build. Parallel nur mit **getrennten Dateien** (z. B. 10.1 und 7.5).

---

## Grobe Zeitschätzung (ausdrücklich Schätzung, keine Zusage)

| Abschnitt | Grobe Spanne | Anmerkung |
|---|---|---|
| Phase 0 | 1–2 Tage | Sichtung und Messungen, wenig Gerätelast |
| Phase 1 | 2–4 Tage | Umzug + erster grüner Gesamtlauf (langsam wegen 2,2 GB freiem RAM) |
| Phase 2 | 4–7 Tage | Kern der App |
| Phase 3 | 4–7 Tage | plus Beweis-Lauf Webseite |
| Phase 4 | 2–3 Tage | |
| Phase 5 | 3–5 Tage | |
| Phase 6 | 5–9 Tage | Gerätetest nötig; ohne Kabel nur teilweise prüfbar |
| Phase 7 | 4–6 Tage | |
| Phase 8 | 5–8 Tage | Grafik zuerst, dann Prozessor (Reihenfolge: Grafik → Prozessor → NPU) |
| Phase 9 (NPU) | **2–3 Wochen** | eigener Block; Erfolg nicht garantiert, Rückfall bleibt |
| Phase 10 | laufend | bei jedem Meilenstein |

**Hinweis:** Kalenderzeit kann durch Nachtarbeit am Ladekabel gestrafft werden; begrenzt wird sie durch **Speicher (2,2 GB frei)**, **einen Build gleichzeitig** und **fehlendes Kabel**.

---

## Regeln, die für jede Aufgabe gelten

1. **Vorher prüfen, nachher messen.** Keine Zahl ohne eigene Messung (Spec §2.7/§11).
2. **Erst grün, dann weiter.** Test + Build grün, sonst nicht als fertig eintragen.
3. **Nach jedem Block:** Checkpoint, Commit, Push (nur nach bestandenen Prüfungen), dann nächster Task.
4. **Nach 2 Fehlversuchen:** Blocker notieren, weiter (Spec §4.4). Ein Lauf endet nie still.
5. **Fehlendes Gerät/Schlüssel:** alles Machbare weiterbauen, Lücke offen markieren.
6. **Quellen read-only.** Nichts in fremden Ordnern ändern.
7. **Harte Grenzen jederzeit:** Banner und Symbol, Schwesterprojekte, ein Subagent, Sicherheit, Sperrliste.
8. **Erklären:** an jedem Meilenstein kurz in einfachem Deutsch **mit kleinem Schema** — dann autonom weiter.
