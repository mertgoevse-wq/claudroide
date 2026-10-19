# Claudroide v2 — Produktspezifikation

> **Status:** Interview 4 läuft; Entscheidungen aus Interviews 1–3 bleiben gültig, Änderungen aus Interview 4 sind an den betreffenden Stellen vermerkt. (Stand: 2026-10-07)
> **Projektordner:** `/home/mert/claudroide-next` (leer, wird das neue Arbeitsprojekt)
> **Zweck:** Diese Datei ist die vollständige Grundlage für einen autonomen Claude-Code-Lauf. Der Agent kann das Projekt allein aus dieser Spezifikation aufnehmen, ohne dass der Nutzer die Idee erneut erklärt.
> **Nutzerprofil:** Informatiklaie (Mert Zet, GitHub `mertgoevse-wq`). Erklärungen in einfachem Deutsch bei Meilensteinen. Keine endlosen Rückfragen — autonom arbeiten.

---

## Deutsche Zusammenfassung (für den Nutzer)

Du bekommst eine eigene, native Android-KI-App namens **Claudroide**. Sie ist ein persönliche KI-Arbeitsumgebung: Chat, Coding-Agent, Projekte/Dateien/Git, Skills, MCP, Medien und Bildschirmsteuerung. Sie nutzt deine eigenen API-Schlüssel (BYOK) von sehr vielen Providern, wählt automatisch das passende Modell für jede Aufgabe und weicht bei Ausfällen automatisch auf andere Provider aus. Der interne Zustandsmanager sorgt dafür, dass der Agent auch bei langen Aufgaben den Überblick behält und nach einem Neustart dort weitermacht, wo er aufgehört hat. Die App wird als APK sideloaded auf deinem Samsung Galaxy A56 installiert.

Das bestehende Claudroide-Repo (135 abgeschlossene Aufgaben, gebaute App) dient als **Kopie-Basis**: Der Code wird in das neue Projekt übernommen und dort unabhängig weiterentwickelt. Das Banner-Motiv bleibt dauerhaft erhalten (optische Verbesserung nur mit deiner Freigabe), das App-Symbol bleibt unverändert. **DroidRoute** bleibt ein eigenständiges Projekt und wird nur als mögliche spätere Integration eingeplant.

Die Arbeit wird in ein **neues öffentliches GitHub-Repository `mertgoevse-wq/claudroide-next`** gepusht (Lizenz: Apache 2.0, mit Issues und Releases — Details §17). Die neue App wird so gebaut, dass sie **neben der alten Claudroide-App auf demselben Handy installiert bleiben kann** (eigene interne App-Kennung), ohne die alte zu überschreiben.

Die Entwicklung erfolgt in **zwei Ausbaustufen** (§1.5): Stufe 1 ist das stabil nutzbare Kernprodukt, Stufe 2 folgt direkt danach.

---

## 1. Produktvision

**Claudroide** ist eine persönliche, native Android-KI-Arbeitsumgebung — nicht nur ein Chat. Konzeptionell orientiert an Claude/ChatGPT/Claude Code, aber eigenständige Marke, eigenständige Implementierung (Clean-Room-Prinzip beibehalten).

### 1.1 Funktionsbereiche (Zielbild)

| Bereich | Beschreibung |
|---|---|
| Chat | Mehrere Chat-Modi: normaler KI-Chat, Claude-artig, ChatGPT-artig (Details §9.1) |
| Coding-Agent | Claude-Code-artiger agentischer Modus (siehe §4) |
| Projekte / Dateien / Code | Projektverwaltung, Dateibrowser, Code-Ansicht |
| Git | Diffs, Commits, Verlauf, GitHub-Sync |
| Terminal | Shell-Ausführung in wählbarer Umgebung (§5.3) |
| Vorschau (Preview) | Live-Preview von Web-/App-Projekten |
| Medien | Bilder, Audio, Musik, MIDI, Video, Dokumente verstehen und erzeugen |
| Skills / Plugins / MCP / Prompts | Intelligenter Katalog, progressives Laden (§7) |
| Agenten | Spezialagenten (maximal EIN Subagent gleichzeitig, §4.2) |
| Automatisierung | Hintergrund-/geplante Aufgaben |
| Device Agent | Android-Bildschirmsteuerung (See→Decide→Act→Verify, §6) |
| Sprache / Assistent | In Stufe 1 keine Sprachfunktion; Spracheingabe, Vorlesen und Wakewort in Stufe 2 (§6a) |
| Provider / Modelle | BYOK, viele Provider, automatisches Routing & Failover (§3) |
| Kontextmanager | Kontextüberwachung, Kompression, Rotation, Persistenz (§5) |

### 1.2 Explizit NICHT

- Kein „einfacher Chat-Prototyp"/AI-Slop. Hohe Qualität, Ruhe, tägliche Nutzbarkeit.
- Keine Nachahmung von Claude-/Anthropic-Marken, -Logos oder -Designs.
- Keine Umgehung von Android-Sicherheitsmechanismen, kein Root, keine versteckten Aktionen.
- Kein Verlust von Agentenarbeit durch Kontextrotation („neuer Kontext ≠ vergessen").
- Kein monatliches Ausgabenlimit/Warnsystem in v1 (Nutzerentscheidung: nur Anzeige, kein Stopp).

### 1.3 Definition „erste fertig nutzbare Version" (Fertig-Beweis)

Die Version 1 gilt erst dann als fertig, wenn **beide Beweis-Läufe** erfolgreich absolviert wurden (Nutzerentscheidung):

1. **Coding-Agent-Beweis:** Chat funktioniert UND der Coding-Agent hat einmal ein echtes kleines Projekt gebaut oder geändert und das Ergebnis nachgewiesen (Build/Test/Verifikation).
2. **Device-Agent-Beweis:** Der Bildschirm-Agent hat einmal erfolgreich eine App geöffnet und eine Aktion ausgeführt und überprüft.

### 1.5 Zwei Ausbaustufen (Nutzerentscheidung, Interview 3)

Die ursprüngliche „erste Version“ wurde in **zwei Stufen** geteilt, damit Stufe 1 stabil und zügig fertig werden kann:

**Stufe 1 (Kernprodukt):** Chat (DISCUSS/BUILD) + KI-Dienste inkl. Ein-Klick-Gratis-Modelle + Auto-Routing + Auto-Failover + Coding-Agent (Dateien, Shell, Diffs, Verifikation, Sicherungen/Rückgängig) + Device-Agent (Bildschirmsteuerung) + Context-Manager + Live-Protokoll + Bild-Anhänge mit Bildverständnis + weitere Dateianhänge zum Speichern/Weiterreichen, aber noch ohne inhaltliche Auswertung + Onboarding. In Stufe 1 gibt es noch keine Spracheingabe und keine Sprachausgabe.

**Stufe 2 (direkt danach):** Spracheingabe, Vorlesen von Antworten und dauerhaftes Zuhören mit Wakewort; Audio-, Dokumenten- und Videoauswertung (kurze Videos zuerst prüfen); weitere Provider, Skills/MCP-Kataloge und Vorbereitung lokaler KI.

Die Fertig-Definition von §1.3 (beide Beweis-Läufe) gilt für **Stufe 1**.

### 1.4 Design-Entscheidung: Claude-App-Look als Basis

Der Chat orientiert sich erkennbar an der **Claude-Android-App** (Nutzer-Favorit) mit den besten Bedienelementen der ChatGPT-App als Mix. Grenzen: keine 1:1-Kopie geschützter Designs, keine Claude-/ChatGPT-Logos oder -Bilder; eigenständige, ruhige, edle Interpretation. Design-Tokens aus dem alten Claudroide als Basis behalten.

---

## 2. Verhältnis zu bestehenden Projekten

### 2.1 Bestehendes Claudroide-Repo (`~/claudroide`, GitHub `mertgoevse-wq/claudroide`)

- **Status:** 135/135 Tasks erledigt, 2530 Unit-Tests grün, Debug-APK (22 MB) gebaut, Min SDK 26, Target SDK 35, Kotlin + Jetpack Compose, Paket `org.claudroide`.
- **Entscheidung (Interview):** **Kopie als Basis, neues Projekt.** Der bestehende Code wird als Ausgangsbasis in `/home/mert/claudroide-next` übernommen und dort unabhängig weiterentwickelt. Das alte Repo bleibt als Referenz bestehen (READ-ONLY).
- Zu übernehmende, geprüfte Bausteine: Design-Tokens (`core/design/ColorTokens.kt`), i18n-Ansatz (Englisch-first, `values-de/`), Provider-Feature-Struktur, Agent-Modus + `CommandRiskScanner` (DISCUSS/BUILD), Onboarding/Settings-Screens, Build-Infrastruktur, Frontmatter-Task-System (`tools/sync_frontmatter.py`), Secret-Gate (`tools/secret_gate.py`).

### 2.2 BANNER-PRESERVATION (HARTE ANFORDERUNG)

Das Banner-Motiv (grüner Android-Kuppel-Bot mit dem zweiten Bot an den Händen) ist die unverzichtbare Marken-Grundlage: Es wird **nie durch ein anderes/neues KI-Bild ersetzt oder ausgetauscht**. Optische Verbesserungen des Einbaus sind erlaubt (Regel siehe unten). Exakte Lage und Einbindung:

| Asset | Pfad | Verwendung |
|---|---|---|
| **Hauptbanner (README)** | `~/claudroide/assets/brand/1791295622623.jpg` (147.653 Bytes) | Eingebunden in `README.md` (und `README.de.md`) als `<img src="assets/brand/1791295622623.jpg">` mit Alt-Text „ClauDroide — a green Android dome and a terminal-screen bot holding hands beside the product name" |
| Banner (groß) | `~/claudroide/assets/brand/banner.png` (499.899 Bytes) | Brand-Assets |
| Banner (688px) | `~/claudroide/assets/brand/banner-688.png` (122.804 Bytes) | Brand-Assets |
| App-Banner | `~/claudroide/app/src/main/res/drawable-nodpi/claudroide_banner.webp` | In der App (z. B. Onboarding/Settings, referenziert über `ColorTokens.kt`, `ClaudroideComponents.kt`, `OnboardingScreen.kt`, `SettingsScreen.kt`) |
| App-Mark (auch App-Symbol) | `~/claudroide/app/src/main/res/drawable-nodpi/claudroide_mark.webp` | In der App UND als Launcher-Icon. **App-Symbol bleibt unverändert** (Nutzerentscheidung Interview 2) |
| Mark-Icons | `assets/brand/mark-96.png`, `mark-256.png`, `mark-1024.png` | README/Brand |

**Anforderung (Interview 3, Widerspruch aufgelöst):** Beim Kopieren nach `claudroide-next` werden alle Assets unter `assets/brand/` und `drawable-nodpi/` zuerst **byte-identisch** übernommen (das ist der unveränderliche Ausgangspunkt; die Originale im alten Repo bleiben unangetastet). Das **Motiv** (grüner Android-Kuppel-Bot, der den zweiten Bot an den Händen hält) bleibt dauerhaft erhalten und wird nie durch ein anderes/neues KI-Bild ersetzt. **Optische Verbesserungen sind ausdrücklich erlaubt** (Schärfe, Zuschnitt, Darstellung, schönerer Einbau in README und App) — aber **jede einzelne Änderung am Banner braucht vorher die Freigabe des Nutzers**. Das App-Symbol (`claudroide_mark.webp`) bleibt unverändert (Interview 2).

### 2.3 DroidRoute (`~/droidroute/droidroute`, GitHub `mertgoevse-wq/droidroute`, privat, MIT)

- **Status (erheben, nicht raten):** ~16 von 208 Tasks erledigt. Fertig: Ktor-Server-Bootstrap (CIO), Foreground-Service, konfigurierbarer Port (8787), Bind-Modi mit Auth-Floor, named client keys (`dr_`-Präfix). Nächster Task: T-017 (Request-Logging). **Provider-Schicht, Wire-Protokolle, Routing, UI, lokale Modelle: noch nicht begonnen.** Der Nutzer arbeitet parallel daran weiter — der Stand muss vor Integrationsarbeit **frisch erhoben** werden, nicht aus dieser Spec geschlossen.
- **Entscheidung (Interview):** DroidRoute bleibt **ein eigenständiges Projekt/App**. Es wird NICHT als Bibliothek in Claudroide eingebaut und nicht gekoppelt. Claudroide plant die Integration als **spätere, optionale Möglichkeit**: Der Provider-Abstraktionslayer wird so entworfen, dass ein DroidRoute-Endpoint (OpenAI-/Anthropic-/Gemini-kompatibel unter `http://127.0.0.1:8787`) später einfach als einer von vielen Providern hinzugefügt werden kann. Keine harte Kopplung: Claudroide ist ohne DroidRoute voll funktionsfähig.

### 2.4 Schwesterprojekte: READ-ONLY

Alle Projekte unter `$HOME` (u. a. `claudroide-first-party/*`, `clawscreen`, `awesome-prompts`, `awesome-prompts-full`, `claude-media-bridge`, `perpybridge`, `freebuffclaw-bridge`, `cc-on-usb`, `flylab`, `exy`, `clawdroide`, `claude-screen-use`, `claude-deskdroide`, `claudroide-build`, `media`) sind **READ-ONLY-QUELLEN**: Nicht löschen, nicht kaputtmachen, nicht automatisch umbauen. Alle Änderungen erfolgen ausschließlich in `/home/mert/claudroide-next`.

### 2.5 GitHub-Quellen (alle öffentlich + verfügbare private Repos)

Der Agent ist über `gh` CLI authentifiziert (Account `mertgoevse-wq`). **Anforderung (Interview):** Alle GitHub-Repos des Nutzers anschauen (lokal UND online) und bewerten, was jeweils für Claudroide übernommen werden kann. Ablauf:

1. `gh repo list mertgoevse-wq --limit 200` — Bestand ermitteln (~45 Repos bekannt, Stand 2026-10-07).
2. Shallow/filterbare Klone unter `_sources/github/` im Arbeitsprojekt (Speicher sparen; Gerät hat 21 GB frei, 8 Kerne, 7,2 GB RAM — PRoot-Umgebung, Ressourcen beachten).
3. Lokal vorhandene Projekte werden nicht neu geklont, sondern als Pfad in `_sources/` referenziert (`_sources/local/`).
4. Für jede Quelle ein Eintrag in `SOURCE_INDEX.md` (§10): Name, Pfad, GitHub-URL, Sprache/Technologie, Zweck, relevante Funktionen, mögliche Wiederverwendung, Lizenz, Lizenzrisiken, Klassifikation (KEEP / ADAPT / REWRITE / REFERENCE / IGNORE).
5. Fremder Code nur mit geprüfter Lizenz und Herkunft übernehmen (§8).

Bekannte relevante Repos (Auswahl, zu vervollständigen): `claudroide`, `droidroute`, `clawscreen`, `claude-code-android`, `claude-media-bridge`, `claude-project-workbench`, `PocketCodeAgent`, `mobile-linux-lab`, `design-skill-library`, `skill-repo`, `airbeat-studio`, `aureon-desk`, `aster-code`, `bolt-diy-android`, `loki-workbench`, `lokicode`, `Genesis_Harness`, `VELDRA`, `veldra-video-analysis`, `awesome-prompts`, `LiteLLM` (privat, Fork-Referenz).

### 2.6 Awesome Prompts

`~/awesome-prompts` und `~/awesome-prompts-full` (identische Struktur, LICENSE vorhanden) sind Prompt-Quellen. **Nicht wahllos importieren** (§7.3): erkennen → auswählen → kombinieren → bei Bedarf anpassen → dokumentieren.

---

## 3. Provider, BYOK, Routing (Kernbereich)

### 3.1 Provider-Katalog

Gewünschte Provider (Interview-Runde 2). Reihenfolge = Orientierung, nicht strikte Priorität:

- **Priorität 1 (MUSS in v1):** Anthropic (Claude), OpenAI, OpenRouter (Aggregator, viele Modelle inkl. Free-Tier)
- **Priorität 2 (SOLL, rasch nach):** Groq, Mistral, NVIDIA NIM, Bynara, Ollama Cloud, Orcarouter, Fastrouter
- **Priorität 3 (SPÄTER/Erweiterung):** Kilo Code, xKiro, Agent Router, Antigravity (CLI), Bluesminds sowie beliebige **OpenAI-kompatible** und **Anthropic-kompatible** Custom-Endpoints (Nutzer-definierte URL + Key + Modellliste)
- **Lokale Modelle:** SPÄTER (Phase 8+), direkt auf dem A56 (z. B. llama.cpp/MediaPipe-Genai). Architektur vorbereiten, nicht in v1.
- **DroidRoute:** SPÄTER, als optionaler Custom-Provider (§2.3).

**Architekturanforderung:** Der Provider-Layer abstrahiert über Wire-Formate (OpenAI-kompatibel, Anthropic-kompatibel, ggf. Gemini), nicht über individuelle SDKs. Ein neuer „kompatibler" Provider ist damit reine Konfiguration (Name, BaseURL, API-Key-Referenz, Wire-Format, Modellliste), kein Code.

- **Vorhandene Schlüssel des Nutzers (Stand Interview 4):** OpenRouter ist vorhanden; der Nutzer bestätigt auch weitere Schlüssel, hat aber noch nicht genau benannt, welche aus Bynara, Agent Router und NVIDIA NIM tatsächlich einsatzbereit sind. Vor Umsetzung einmal gezielt abgleichen, statt die Liste als sicher vorhandene Schlüssel anzunehmen. Anthropic/OpenAI-Schlüssel sind nach bisheriger Aussage nicht vorhanden.

### 3.2 Ein-Klick-Anbieter mit kostenlosen Modellen (Setup-Hilfe)

- **Entscheidung:** **Beide einbauen.** Im Setup gibt es Ein-Klick-Optionen mit gratis Modellen, damit die App ohne Guthaben sofort benutzbar und testbar ist:
  1. **OpenRouter Free-Modelle** (z. B. kostenloses Llama-Modell über OpenRouter).
  2. **Kilo-Code Free** (Schlüssel mit dem kostenfreien `kc/kilo-auto/free`-Modell).
- Diese Ein-Klick-Provider sind der empfohlene Einstieg im Setup-Bildschirm.

### 3.3 BYOK & Schlüsselspeicher

- **Entscheidung:** **Android Keystore** (empfohlene Option). Keys werden niemals im Klartext gespeichert, nie in Git, nie in normalen Logs (§8).
- Keystore-gebundene Verschlüsselung (z. B. AES-GCM mit Keystore-Key) für die Key-Datenbank.
- UI: Provider-Einrichtung mit Key-Eingabe, Maskierung, Test-Button (Endpoint-Test), Health-Status.

### 3.4 Modell-Routing

- **Entscheidung:** **Automatisch nach Aufgabe.** Einfache Fragen → kleines/günstiges Modell; Coding-/Agentenarbeit → starkes Modell; Vision-Aufgaben (Screenshots) → vision-fähiges Modell.
- Implementierung: Modell-Aliase (z. B. `role:fast`, `role:coder`, `role:vision`, `role:summary`), die der Router auf konkrete Modelle je nach Verfügbarkeit/Health auflöst. Nutzer kann pro Chat/Task manuell überschreiben (SPÄTER, aber Architektur von Anfang an vorsehen).

### 3.5 Failover

- **Entscheidung:** **Ja, automatisch.** Bei Fehler (Timeout, 4xx/5xx, leerem Kontingent, Rate-Limit) wechselt die App selbst zum nächsten funktionsfähigen Provider/Modell. Der Chat/Agentenlauf läuft weiter; der Wechsel wird im Protokoll sichtbar markiert.
- Failover-Kette aus Provider-Prioritäten + Health-Checks (regelmäßiger Endpoint-Test, Zustand im UI sichtbar).
- **Verbindungsabbruch mitten im Gespräch (Nutzerentscheidung):** automatisch 2–3 erneute Versuche, dann automatischer Wechsel auf den nächsten Dienst/das nächste Modell (Failover); erst wenn alles scheitert: ehrliche Fehlermeldung. Bereits geschriebener Text geht dabei **nie** verloren.

### 3.6 Kosten-/Kontingentinformatik

- **Entscheidung:** **Tages-/Monatsübersicht** (Einstellungsseite): Verbrauch und geschätzte Kosten pro Tag, Monat und Provider, aus Antworten extrahierte Token-Zählungen. Live-Anzeige pro Antwort ist SPÄTER optional.
- Kontingent-Erkennung soweit technisch möglich (Fehlercodes, Header); danach Trigger für Failover.

### 3.7 Ausgaben-Regel

- **Entscheidung:** **Kein Limit nötig.** Die App zeigt Verbrauch an (§3.6), warnt aber nicht und stoppt nicht. Kein Budget-Limit-Feature in v1.

---

## 4. Agenten-Runtime (Claude-Code-artig)

### 4.1 Fähigkeiten (Zielbild, phasenweise auszubauen)

Projekt verstehen, Dateien lesen/erstellen/ändern/löschen, Code durchsuchen, Abhängigkeiten verstehen, Git verstehen (Diff, Log, Commit), Diffs anzeigen, Tests ausführen, Builds ausführen, Fehler untersuchen/beheben, Terminal benutzen, Aufgaben planen und in Schritte zerlegen, Fortschritt speichern, Projekte fortsetzen, MCP/Skills/Plugins/Spezialagenten verwenden, Ergebnisse prüfen und Änderungen **tatsächlich verifizieren** — nicht behaupten, dass etwas funktioniert.

### 4.2 HARTE GRENZEN (Runtime-design, nicht nur Prompt)

- **Genau EIN Shell-/Terminal-Job gleichzeitig.** Genau EIN Subagent gleichzeitig. Keine parallelen Shell-Jobs, keine parallelen Subagenten.
- Umsetzung: zentraler **Job-Manager/Semaphor** in der Agent-Runtime; zweite Anfrage auf denselben Job-Typ wartet oder wird abgelehnt. Der Zustand (laufender Job, Besitzer, Startzeit) ist im UI sichtbar.
- Andere Prozesse (Netzwerk-IO, Media-Rendering, Health-Checks) dürfen parallel laufen, sofern sie keine konkurrierenden Agenten-/Shell-Arbeiten sind.

### 4.3 Autonomie-Modus

- **Entscheidung:** **Höchstautonomie** (Nutzer-Wortlaut: „alle Entscheidungen treffen, selbstständig arbeiten, skills nutzen, context leeren intelligent, wenn per OmniRoute/DroidRoute verbunden auch Arbeit an andere Provider auslagern … code erzeugen, schreiben, löschen, ändern … skip permissions").
- Bedeutung: Der Agent trifft technische Entscheidungen selbst, nutzt Skills proaktiv, komprimiert/rotiert Kontext selbstständig, darf Dateien schreiben/löschen/ändern und Shell-Befehle ausführen **ohne Einzelbestätigung** (BUILD-Modus, kein Einzel-Review).
- **Nicht verhandelbare Grenzen auch bei Höchstautonomie** (aus §8/§2.4): Schwesterprojekte READ-ONLY; keine destruktiven Geräteaktionen; keine Secrets in Git/Logs; Banner-Motiv nicht ersetzen; Aktionen transparent protokollieren; Not-Aus jederzeit wirksam.
- **Programmierbereich ohne Bestätigungspflicht (Nutzerentscheidung):** Im BUILD-Modus läuft innerhalb des ausgewählten Projekts alles automatisch — auch Dateien löschen und Befehle ausführen. Die App darf nur echte Dateien im Projektordner ändern. Verknüpfungen, die nach außerhalb zeigen, werden nicht verfolgt und dortige Dateien werden nicht geändert. Sicherheit durch Live-Protokoll, Stopp-Knopf, Sicherungen (§4.11) und die harten Grenzen.
- **Umgang mit Unsicherheit (Nutzerentscheidung, Mischung):** Unwichtige Fragen entscheidet der Agent selbst und dokumentiert Entscheidung + Begründung nachvollziehbar. Bei größeren Folgen außerhalb der freigegebenen Projektgrenze, nicht klar erlaubten Ausgaben oder schwer umkehrbaren Aktionen außerhalb des Projekts fragt er kurz und verständlich nach. Innerhalb des gewählten Projekts darf er auch viele Dateien automatisch ändern oder löschen — aber nur echte Dateien unterhalb dieses Projektordners.

### 4.4 Blocker-Regel

- **Entscheidung:** Nach **2 fehlgeschlagenen Reparaturversuchen**: Blocker in `progress/BUILD-STATE.md` notieren, zur nächsten unabhängigen Aufgabe weiterarbeiten, alle Blocker am Ende des Laufs melden. Ein Lauf endet nie still.

### 4.5 Subagent-Rolle (Nutzerentscheidung)

- Der eine erlaubte Subagent bekommt **je Aufgabe die passendste Rolle** — der Haupt-Agent entscheidet frei: Planer (große Aufgaben in Schritte zerlegen), Prüfer (Änderungen des Haupt-Agents gegenprüfen), Recherche oder anderes.
- Die Rollenwahl wird im Live-Protokoll sichtbar gemacht.

### 4.6 Projekt-Ordner (Nutzerentscheidung)

- **Beides wählbar:** Standard ist der app-eigene geschützte Projektordner (keine Extra-Berechtigungen, sicherste Variante). Zusätzlich kann pro Projekt ein **beliebiger anderer Ordner** gewählt werden (z. B. Documents/Claudroide über die System-Ordnerauswahl — einmalige Berechtigung pro Ordner, kein Vollzugriff).
- Der Coding-Agent arbeitet nur innerhalb des gewählten Projektordners; Schwesterprojekte unter `$HOME` bleiben READ-ONLY (§2.4). Verknüpfungen nach außerhalb werden nicht verfolgt oder verändert. Die App darf den Inhalt des aktiven Projekts durchsuchen, um passende Informationen zu finden (§Interview 4, Runde 4).

### 4.7 Projekt-Sync (Nutzerentscheidung)

- **Ein privates Sammel-Repo** (z. B. `mertgoevse-wq/claudroide-workspaces`) für alle App-Projekte — nicht ein Repo pro Projekt.
- **Standardmäßig AUS (Interview 4):** Es wird nichts zu GitHub hochgeladen, bis der Nutzer die Sicherung für ein Projekt ausdrücklich einschaltet. Die Wahl gilt pro Projekt und kann später geändert werden.
- Abgrenzung: Das Entwicklungs-Repo `claudroide-next` (§2) ist öffentlich und enthält die App-Entwicklung; das Sammel-Repo ist privat und enthält nur ausdrücklich zur Sicherung freigegebene Nutzerprojekte.

### 4.8 Aufgabensystem für die Entwicklung (Nutzerentscheidung)

- **Gleiches bewährtes System wie im alten Claudroide:** eine Datei pro Aufgabe mit Status-Frontmatter und Abhängigkeiten (`tasks/`-Ordner + `tools/sync_frontmatter.py`). Der Agent kennt es bereits, es hat sich bei 135 Tasks bewährt.

### 4.9 Gerätetest (Qualitäts-Gate, Nutzerentscheidung)

- **Beides kombiniert:** Der Agent installiert die APK selbst per adb (aus Termux, wie bei clawscreen) und macht Screenshot-Verifikation; der Nutzer testet zusätzlich täglich die echten Abläufe und meldet Fehler zurück.

### 4.10 App-Updates (Nutzerentscheidung)

- **So automatisch wie möglich:** Der Agent versucht, neue APK-Versionen per adb selbst zu installieren; der Nutzer bestätigt nur noch am Bildschirm. Fallback: bereitgestellte APK-Datei antippen.

### 4.11 Sicherungen & Rückgängig (Nutzerentscheidungen)

- Vor jeder KI-Änderung an Projektdateien macht die App eine **Sicherung**.
- Jede Änderung kann **einzeln rückgängig** gemacht werden — unbegrenzt viele Schritte.
- Wo das Projekt Git nutzt, gilt zusätzlich die vollständige Git-Historie (drei Ebenen: Sicherung → Rückgängig-Stapel → Git).
- **Speicher voll:** Die App löscht alte Sicherungen nicht automatisch. Wenn kein Speicher mehr für eine neue Sicherung da ist, pausiert sie weitere Dateiänderungen und wartet auf den Nutzer.
- **Projekt löschen:** Ein vollständiges Projekt wird nur durch eine bewusste Nutzeraktion gelöscht. Die App verschiebt es nicht still in einen Papierkorb: nach Bestätigung werden Projekt, Verlauf und zugehörige Sicherungen endgültig gelöscht. Falls die lokale Wochen-Sicherung oder GitHub-Sicherung davon eine Kopie enthält, wird diese Kopie nicht stillschweigend mitgelöscht; sie bleibt bis zur eigenen Löschaktion erhalten.

---

## 5. Kontextmanagement & Persistenz

### 5.1 Kontext-Modus

- **Entscheidung:** **Beides kombiniert.** Bei vollem Kontext: (1) automatische Kompression (Zusammenfassung des Verlaufs, Drop großer Tool-Ausgaben, Behalten der letzten N relevanten Nachrichten); (2) wenn das nicht mehr reicht: **sauberer Neustart mit Gedächtnis-Datei** — der Agent schreibt seinen Stand auf Disk, eröffnet einen frischen Kontext und liest den Stand wieder ein.
- Der interne Kontextmanager überwacht: Kontextverbrauch, Warnschwellen, große Tool-Ausgaben (Begrenzung/Trunkierung), Relevanz alter Inhalte.
- Verhalten konzeptionell wie `/compact` und `/clear`, aber eigene Implementierung.

### 5.2 Persistenter Zustand (auf Disk)

Wird nach jedem sinnvollen Schritt geschrieben und nach Rotation/Reset automatisch eingelesen:

- aktuelle Aufgabe, Plan, Fortschritt
- wichtige Entscheidungen (+ Begründung)
- offene Probleme/Blocker
- zuletzt geänderte Dateien
- relevante Architekturinformationen
- Session-/Task-State

**Anforderung:** „Neuer Kontext darf NICHT bedeuten, dass der Agent seine bisherige Arbeit vergisst." Nach einem App-/Geräteneustart wird die Arbeit zuverlässig fortgesetzt.

### 5.3 Shell-Umgebung

- **Entscheidung:** **Beides, Nutzerauswahl.**
  - Standard: leichte In-App-Prozess-Shell (was Android ohne Root erlaubt; ggf. Integration bestehender Termux-Installationen).
  - Optional zuschaltbar: **PRoot/Linux-Container** in der App (Bausteine und Erkenntnisse aus `mobile-linux-lab` nutzen: BackendRegistry, PRootBackend, CapabilityDetector existieren dort und im alten Claudroide unter `feature/linux/`).
- Für große Projekte/Builds ist der Container die empfohlene Umgebung.

### 5.4 Datenhaltung & Sync

- **Entscheidung:** **Lokal + GitHub-Sync.** Chatverläufe, Projekte und Agenten-Stand liegen lokal; Projekte und Build-State werden zusätzlich in private GitHub-Repos synchronisiert (Muster wie bisher bei Claudroide/Clawscreen: `progress/BUILD-STATE.md`, Commit pro Block, Push an verifiziertes privates Remote).
- App-interne Daten (Chats, Kontext-Snapshots) bleiben im App-Speicher; Export als Datei möglich (SPÄTER).
- **Chat-Historie (Nutzerentscheidung): Für immer.** Alle Chats bleiben dauerhaft gespeichert, bis der Nutzer sie selbst löscht. Kein automatisches Aufräumen.
- **Automatik-Sicherung (Nutzerentscheidung):** Zusätzlich zu einer separat einschaltbaren GitHub-Sicherung legt die App regelmäßig (Standard: wöchentlich, einstellbar) eine Sicherungsdatei mit Gesprächen, Anhängen, Einstellungen und Projektdateien in einem Handy-Ordner an. Zugangsschlüssel und geheime Zugangsdaten sind immer ausgeschlossen.
- **Speichergrenze (Interview 4):** Sicherungen bleiben erhalten, damit Änderungen rückgängig gemacht werden können. Wenn der Speicher knapp wird, warnt die App. Sie löscht Sicherungen nie stillschweigend. Reicht der Platz für eine neue Sicherung nicht aus, pausiert sie dateiverändernde Arbeit, bis der Nutzer Platz geschaffen hat.

---

## 6. Device Agent (Android-Bildschirmsteuerung)

### 6.1 Fähigkeiten (Zielbild)

Apps öffnen, Buttons drücken, Menüs bedienen, scrollen, swipen, Text eingeben, Zurück/Home/Recents, Bildschirm lesen, Screenshots analysieren, visuelle Elemente erkennen, Aktionen überprüfen.

### 6.2 Technische Basis (Android-nativ)

- **AccessibilityService** + `AccessibilityNodeInfo` (semantische UI-Elemente — **immer bevorzugt**)
- `dispatchGesture` (Tippen/Swipe/Scroll), `performGlobalAction` (Zurück/Home/Recents)
- `takeScreenshot` (API 30+) bzw. **MediaProjection** als Fallback
- Foreground Service für die Laufzeit; Overlay nur wenn wirklich notwendig
- Visuelle Bildschirmanalyse (Screenshots + vision-fähiges Modell) als **Fallback**, wenn keine brauchbaren Accessibility-Elemente vorhanden sind

### 6.3 Loop & Sicherheit

- **See → Decide → Act → Verify:** Nach jeder Aktion prüft der Agent (per Accessibility-Tree-Diff oder Screenshot), ob der gewünschte Effekt eingetreten ist. Bei unsicheren Aktionen nicht blind weiterklicken.
- **Entscheidung (Interview):** **Autonom mit Not-Aus.** Der Agent arbeitet komplett selbstständig; es gibt immer: großen sichtbaren **Not-Aus**, Pause/Stop, Permission-Controls, sichtbaren Agentenstatus, nachvollziehbare Tool-Aufrufe, **App-Sperrliste** (z. B. Banking-Apps werden nie berührt — Liste vom Nutzer pflegbar, Standard: alle installierten Finanz-Apps aus).
- **Bestätigungen (Nutzerentscheidung, Interview 3):** keine weiteren Pflicht-Bestätigungen — die Kontrolle läuft über Not-Aus und Sperrliste. Passwort-/PIN-Eingabefelder werden niemals automatisch ausgefüllt.
- **Standard-Sperrliste (Interview 4):** Geld-, Banking-, Bezahl- und Passwortspeicher-Apps sind von Anfang an gesperrt. Andere Apps sind erlaubt. Der Nutzer kann die Sperrliste selbst erweitern; eine gesperrte App darf der Device-Agent weder bedienen noch Screenshots oder Inhalte aus ihr an einen KI-Dienst senden.
- **Not-Aus (Interview 4):** Ein Druck stoppt sofort alle laufenden KI-Antworten, Shell-/Terminal-Jobs und Bildschirmaktionen. Bereits gespeicherte Änderungen bleiben bestehen; nach dem Not-Aus wird nichts automatisch fortgesetzt. Ein neuer Lauf muss vom Nutzer gestartet werden.
- **Prioritäts-Apps (Nutzerentscheidung, Interview 3):** besonders wichtig sind Musik-Apps (Bezug zu den Musik-/DAW-Projekten), Browser und Dateimanager. Grundsätzlich sind alle Apps erlaubt außer der Sperrliste.
- Keine Umgehung von Gerätesicherheit (Sperrbildschirm, biometrische Freigaben), keine versteckten/heimlichen Aktionen. Alle Aktionen erscheinen im Live-Protokoll (§9).
- Verteilung: **erst APK/Sideload** (keine Play-Store-Zwangshebel); Play-Store-Kompatibilität nur als eventuelle spätere Option — die Architektur hält die Device-Agent-Komponenten aber austauschbar/abschaltbar.

## 6a. Sprache (Entscheidungen aus Interview 3 und 4)

- **Stufe 1:** keine Spracheingabe und keine Sprachausgabe (neu entschieden in Interview 4; ersetzt die frühere Planung „Vorlesen ab Stufe 1“).
- **Stufe 2:** Spracheingabe per Mikrofonknopf, Vorlesen von Antworten und dauerhaftes Zuhören mit Wakewort. Der Nutzer ist bereit, Claudroide als Android-Assistenten auszuwählen, falls das für die erlaubte Wakewort-Funktion nötig ist.
- Dauerhaftes Zuhören muss jederzeit sichtbar sein und sich jederzeit ausschalten lassen. Androids eigene Hinweise und Freigaben werden nicht umgangen.
- **App-übergreifender Assistent / Hintergrund-Aufgaben:** später (nach Stufe 2).

### 6.4 Bezugsquellen im Eigenbestand

`clawscreen` (Claude steuert Android-Bildschirm via Termux/adb — Konzepte, Grenzen, Lessons Learned), `claude-screen-use`, `clawdroide`. Diese sind Termux-basiert; Claudroide implementiert **nativ**, kann aber Konzepte und Erfahrungswerte übernehmen (REFERENCE/ADAPT).

---

## 7. Skills, Plugins, MCP, Prompts

### 7.1 Intelligenter Katalog (Anti-Kontext-Bloat)

Alles wird über einen Registry-/Katalogmechanismus verwaltet, **nicht** einfach alles gleichzeitig geladen. Für jeden Eintrag: Trigger, Beschreibung, wann verwenden, wann NICHT verwenden, benötigte Tools, benötigte Berechtigungen, Context-Kosten (Größe der Toolbeschreibungen).

- **Skills laden nur bei Bedarf:** Der Agent sieht zunächst nur Katalog-Metadaten; bei passender Aufgabe wird der Skill vollständig geladen (Progressive Disclosure — Bausteine aus `lokicode` (`Skill Registry & Progressive Disclosure`) und `design-skill-library` (On-Demand-Router, Anti-Slop-Gate) übernehmen/anpassen).
- Kataloge: Skill-Katalog, Plugin-Katalog, MCP-Katalog, Prompt-Katalog.

### 7.2 MCP

MCP-Client in der App (stdio lokal, HTTP/SSE remote). Referenz: bestehende MCP-Erfahrung aus `droidroute` (`mcp`-Paket existiert dort als Planung), `claude-media-bridge` (funktionierende Media-MCP-Bridge). MCP-Server werden katalogisiert mit Berechtigungsangaben; Sicherheit §8.

### 7.3 Awesome-Prompts-Pipeline

1. passende Prompts aus der Bibliothek **erkennen** (Relevanz zur Aufgabe)
2. **auswählen** ( maximal wenige, kontextsparend)
3. **kombinieren** (Konfliktvermeidung)
4. bei Bedarf **anpassen**
5. Auswahl/Änderung **dokumentieren** (im Session-/Task-State)

### 7.4a Medien-Details (Entscheidungen aus Interview 3 und 4)

- **Stufe 1:** Bilder können angehängt und von einem dafür geeigneten KI-Dienst ausgewertet werden. Audio, Dokumente und Videos können — sofern der Dienst und die Dateigröße es erlauben — angehängt und gespeichert werden; Claudroide darf ihren Inhalt in Stufe 1 aber noch nicht als verstanden ausgeben. Inhaltliche Auswertung kommt später.
- **Übertragung an KI-Dienste (Interview 4):** Inhalte werden nur dann an einen KI-Dienst im Internet gesendet, wenn der Nutzer sie ausdrücklich an eine Chat-Nachricht anhängt oder ausdrücklich zum Senden auswählt. Die App darf nicht stillschweigend weitere Projektdateien hochladen. Projektdateien dürfen lokal durchsucht werden; nur die für die konkrete Antwort benötigten Ausschnitte werden übermittelt, wenn das für die Antwort erforderlich und durch die Anfrage gedeckt ist. Diese Regel gilt auch für automatisch gefundene Projektdateien: lokal durchsuchen ist erlaubt, Übertragung braucht eine ausdrückliche Auswahl des Nutzers.
- **Kurze Videos:** Ursprünglich wurde „nur kurze Clips“ als mögliche Stufe-1-Auswertung gewählt. Die spätere Antwort „anhängen, später verstehen“ spricht dagegen. Bis zur Klärung gilt die spätere, ausdrücklichere Antwort: Videos in Stufe 1 nur anhängen/speichern, nicht auswerten.
- **Stufe 2:** Audio, Dokumente und Videos werden schrittweise auswertbar; kurze Videos sollen zuerst unterstützt werden. Grenzen für Clip-Länge, Dateigröße und unterstützte Formate müssen vor Umsetzung anhand der Provider-Fähigkeiten festgelegt werden.
- **Später:** Medien **erzeugen/bearbeiten** (Bilder/Audio/Musik/MIDI/Video), Anbindung an claude-media-bridge und Musik-/DAW-Projekte (airbeat-studio u. a.).

### 7.4b Lokale KI (Nutzerentscheidungen, Interview 3)

- **Zeitpunkt: mittlere Phase** (wenn Chat, Agent, Bildschirmsteuerung und Sprache stehen).
- **Gewünschte Szenarien (alle vier):**
  1. **Kleine Modelle auf dem Handy** — ohne Internet nutzbar (kurze Fragen, Zusammenfassungen).
  2. **Größere Modelle im Heimnetz** — von Computer/anderem Gerät im selben Netzwerk, wenn eingeschaltet.
  3. **Modelle von USB-Speicher** — ohne Handy-Speicher zu füllen (Bezug: cc-on-usb-Projekt).
  4. **Gratis-Online als Reserve** — OpenRouter Free / Kilo Free als Lückenbüßer (bereits als Ein-Klick-Provider geplant, §3.2).

### 7.4 Globale Nutzer-Präferenzen

- **Entscheidung:** **Ja.** Eine dauerhafte Präferenz-Datei (App-eigenes „Nutzer-Gedächtnis", analog einer CLAUDE.md für die App selbst): feste Provider-Bevorzugungen, Commit-Stil, Sprache, Verhaltensregeln. Der Agent liest sie bei jedem Lauf. Pro-Projekt-Einstellungen ergänzen/überschreiben sie.

---

## 8. Sicherheit

| Thema | Anforderung |
|---|---|
| API-Keys | Android Keystore; verschlüsselt; Maskierung in UI/Logs; **nie** in Git, nie in Klartext-Logs |
| Berechtigungen | Android-Permissions minimiert, mit Begründung beim Nutzer; Accessibility nur mit klarer Zustimmung |
| Tool Permissions | Riskante Tool-Kategorien definiert; `CommandRiskScanner` (aus altem Claudroide) weiterverwenden; auch bei Höchstautonomie gelten die harten Grenzen (§4.2, §2.4, §6.3) |
| Shell-Sicherheit | Risiko-Scan vor Ausführung; verbotene Befehlsklassen (Root, `pm uninstall`, Massenlöschung außerhalb Projektkontext) hart blockiert |
| MCP-Sicherheit | Server brauchen explizite Freigabe; Tool-Aufrufe werden protokolliert; kein stiller Netzwerkzugriff unbekannter Server |
| Audit Logs | Vollständiges, durchsuchbares Aktions-Protokoll (Tool-Aufrufe, Dateiänderungen, Shell-Befehle, Provider-Wechsel) — Grundlage des Live-Protokolls (§9) |
| Secrets-Scan | Automatischer Secret-Gate vor jedem Commit (Werkzeug aus altem Repo portieren) |
| Netzwerk | Nur HTTPS zu konfigurierten Endpoints; localhost-Endpoints (DroidRoute später) nur gebunden mit Auth |
| Lokale Daten | App-private Speicherung; keine sensiblen Daten auf geteilten Ordnern ohne Nutzerentscheidung |
| Logs | Secret-Freiheit auch in Diagnose-Logs (Redaction-Layer) |
| Stop/Cancel | Jeder Agenten-/Shell-/Device-Lauf ist jederzeit stoppbar; Device-Agent zusätzlich Not-Aus + App-Sperrliste |
| App-Zugang | Keine eigene App-Sperre — die normale Handy-Sperre genügt (Nutzerentscheidung). API-Schlüssel sind trotzdem Keystore-geschützt |
| Berechtigungen im Detail (Nutzerentscheidung Interview 3/4) | Beim ersten Einrichten zeigt die App gesammelt alle Rechte, die sie tatsächlich braucht, erklärt jedes einfach und bietet einen direkten Sprung zur passenden Android-Einstellung. Der Nutzer schaltet die Rechte dort selbst ein. Rechte: Bedienungshilfe + Bildschirmaufnahme (Bildschirmsteuerung), Mikrofon (erst ab Stufe 2), Benachrichtigungen (Hintergrundstatus). Keine Rechte werden heimlich oder automatisch eingeschaltet. Lehnt der Nutzer ein Recht ab, bleibt die übrige App nutzbar; nur die davon abhängige Funktion bleibt aus und erklärt später, wie das Recht eingeschaltet werden kann. |
| Fehler-Verhalten | Ehrliche, nachvollziehbare Fehlermeldungen; nie „funktioniert“ anzeigen, wenn es nicht funktioniert. Bei Verbindung: 2–3 automatische Versuche, dann Failover, dann ehrliche Meldung. Bei knappem Akku/Speicher: vorher prüfen und warnen; mitten im Lauf sauber pausieren und nach dem Laden automatisch weitermachen (Nutzerentscheidung) |
| Offline (Nutzerentscheidung) | Ohne Internet läuft weiter, was geht: Projekte ansehen, Dateien lesen, Verläufe/Einstellungen anzeigen. Chat/Agenten zeigen ehrlich „keine Verbindung“ |
| Not-Aus (Interview 4) | Stoppt sofort alle laufenden KI-Antworten, Shell-Jobs und Bildschirmaktionen; bereits gespeicherte Änderungen bleiben; kein selbstständiger Neustart des Laufs. |
| Projektlöschung (Interview 4) | Nach Bestätigung endgültig löschen: Projekt, Verlauf und zugehörige Rückgängig-Sicherungen. Separate lokale/GitHub-Sicherungen werden nicht stillschweigend gelöscht. |
| Abgelehnte Android-Rechte (Interview 4) | Restliche App bleibt nutzbar. Nur die Funktion, die das abgelehnte Recht benötigt, bleibt aus und erklärt beim nächsten bewussten Aufruf, wie es aktiviert wird. |
| Projektsuche (Interview 4) | Im aktuell geöffneten Projekt darf die KI Dateien lokal durchsuchen und passende Stellen verwenden. Sie ändert nur Dateien innerhalb des Projektordners (§4.6) und lädt nichts stillschweigend hoch. |
| GitHub-Sicherung von Nutzerprojekten (Interview 4) | Pro Projekt einschaltbar, standardmäßig AUS. Erst nach Einschalten werden Projektdateien ins private Sammel-Repo übertragen. |
| Automatische lokale Sicherung (Interview 4) | Wöchentlich: Chats, Anhänge, Einstellungen und Projektdateien; geheime Zugangsdaten ausgeschlossen. Sicherungen werden nicht automatisch entfernt. Bei vollem Speicher pausieren Dateiänderungen, bis der Nutzer Platz schafft. |

---

## 8a. Leistung & Stabilität (Nutzerentscheidung Interview 3)

- **Priorität: Zuverlässigkeit zuerst.** Läufe dürfen länger dauern — sie müssen sauber durchlaufen und dürfen nicht abgebrochen werden. Angeheftete/gesperrte Inhalte dürfen weder durch Aufräumen noch durch Speicherbereinigung verloren gehen.
- Vor langen Läufen: kurze Prüfung (freier Speicher, Akku/Ladekabel) mit Warnung vorher, nicht mitten im Lauf.
- Bei knappem Akku mitten im Lauf: sauber speichern, pausieren, nach dem Laden automatisch weitermachen.

---

## 9. UI / UX

- Sehr schöne, moderne **native** Oberfläche (Jetpack Compose). Hochwertig, ruhig, übersichtlich, für tägliche Nutzung. Keine Entwickler-Demo-Optik, nicht verspielt, **keine** Glas-/Neomorphism-Effekte.

### 9.0 Startbildschirm & Navigation (Nutzerentscheidungen, Interview 3)

- **App-Start:** Erst eine **Ladeseite** (Banner-Motiv + Hinweis „Letzte Arbeit wird geladen“ — der vorherige Zustand wird geladen). Dann:
  - **Neustart der App (frisch geöffnet):** direkt in den **Chat**.
  - **Aus dem Hintergrund zurück:** genau **dort weitermachen, wo der Nutzer zuletzt war** (Chat, Projekt, Gerät).
- **Nicht abgeschickte Texte bleiben erhalten:** Ein eingegebener, aber nicht abgeschickter (oder halb bearbeiteter) Text bleibt nach Schließen/Neuöffnen der App im Eingabefeld stehen.
- **Hauptnavigation:** 3 Hauptbereiche unten (Chat, Projekte, Einstellungen) + **Bildschirmsteuerung als vierter, ruhigerer Punkt** am Ende der Leiste (eigenes kleines Symbol). Alle anderen Bereiche (Modelle, Dateien, Medien, Werkzeuge) über Unterseiten.
- **Tablet:** **nur Handy zuerst** (Galaxy A56); Tablet/Geteilt-Modus später.
- **Transparenz-Entscheidung:** **Live-Protokoll aller Schritte** — jeder Tool-Aufruf, jeder Shell-Befehl, jede Dateiänderung sichtbar (Terminal-Feed-Charakter), einklappbar; Agentenstatus immer sichtbar.
- Bereiche (langfristig): Chat, Projekte, Dateien, Code, Terminal, Preview, Modelle, Provider, Skills, Plugins, MCP, Agents, Media, Device, Settings.
- Navigationsoptik: übersichtliche Kernbereiche; weitere Bereiche werden bei Wachstum gebündelt (z. B. „Erweiterungen"), damit die App nicht unübersichtlich wird.
- Sprache der UI: **Englisch zuerst, Deutsch als Zweit-Spracheinstellung** (`values-de/`), wie im bestehenden Repo.
- Design-Basis: Design-Tokens aus dem alten Claudroide übernehmen; Qualität orientiert an guten Konzepten moderner KI-Apps, ohne geschützte Designs zu kopieren.

### 9.1 Chat-Modi und Design-Mix

- **Design-Mix (Nutzerentscheidung):** Der Chat-Look orientiert sich erkennbar an der **Claude-Android-App** (Nutzer-Favorit), ergänzt um die besten Bedienelemente der ChatGPT-App. Keine 1:1-Kopie geschützter Designs, keine Logos/Bilder der Original-Apps. Eigenständige, ruhige, edle Interpretation.
- **Zwei Modi mit Umschalter (Nutzerentscheidung):**
  - **DISCUSS (normaler Chat):** Keine Datei-/Shell-Werkzeuge. Reines Fragen und Antworten.
  - **BUILD (Agenten-Modus):** Alle Werkzeuge verfügbar (Dateien, Shell, Git, Skills, MCP).
  - Der Umschalter ist im Chat sichtbar; der aktuelle Modus ist jederzeit erkennbar.
- **Chat-Sprache (Nutzerentscheidung):** Die KI antwortet **in der Sprache, in der der Nutzer schreibt** (Standard moderner KI-Apps, keine erzwungene Spracheinstellung).
- Die drei Chat-Arten (normal / Claude-artig / ChatGPT-artig) aus der ursprünglichen Idee werden über die Modus-Umschaltung und System-Anweisungen abgedeckt — kein separater dritter Modus nötig.

### 9.1.1 Chat-Verwaltung & Bedienung (Nutzerentscheidungen, Interview 3)

- **Gesprächsliste:** eigener, einfacher, benutzerfreundlicher Mix: Liste nach Zeit (wie Claude-App) **mit Suchfeld**; Gespräche können Projekten zugeordnet werden und erscheinen zusätzlich dort als Unterabschnitt. Umbenennen/Löschen/Anpinnen über langen Druck.
- **Mehrere Gespräche:** beliebig viele parallel offen, Wechsel über die Liste.
- **Nachrichten bearbeiten (Nutzerentscheidung):** gesendete Nachrichten können bearbeitet und erneut gesendet werden; alte Antworten bleiben als frühere Version sichtbar (wie ChatGPT).
- **Anhänge (Nutzerentscheidung):** Bilder (Galerie/Kamera), beliebige Dateien (Text/PDF/Code), Audio und **auch Videos** — die KI kann alles davon lesen/nutzen.
- **Modell pro Gespräch:** Standard automatisch; oben im Chat ein kleines Auswahlfeld zum Festlegen eines bestimmten Modells.
- **Schriftgröße:** folgt der Android-Systemeinstellung; eine 3-Stufen-Einstellung (klein/mittel/groß) wird vorbereitet, aber zunächst **nicht in der App angezeigt** (Platz sparen).
- **Arbeits-Status (Nutzerentscheidung):** komplettes Schritt-Protokoll (einklappbar) **plus** kurze Statuszeile über dem Eingabefeld („prüfe Datei…“, „Test läuft…“, „Fehler gefunden…“).
- **Datei-Ansicht (Nutzerentscheidung):** Dateien ansehen **mit Farben** für Programmier-Sprachen **und direkt bearbeiten**; KI-Änderungen werden daneben angezeigt.

### 9.2 Aussehen (Nutzerentscheidungen, Interview 3)

- **Standard: Dunkel** als festes Standard-Aussehen (Nutzer nutzt die App meist im Dunkeln), manuell umschaltbar; Folgen der Handy-Einstellung optional.
- **Farbton:** eigener **Claudroide-Look (grün/beige)** passend zum Banner als Standard; **umschaltbar in warm-rötlich** (Claude-Nähe) als zweites Farbschema. Beide Schemata in hell + dunkel.
- **Grundhaltung:** modern, hochwertig, ruhig, schnell, professionell; nicht überladen, verspielt, gläsern, übermäßig animiert.

### 9.3 Onboarding / Erster Start (Nutzerentscheidung)

1. **Kurze Begrüßung** mit dem vorhandenen Banner (§2.2, unverändert).
2. **Setup (möglichst vollständig beim ersten Start):**
   - Provider-Einrichtung mit **Ein-Klick-Optionen für gratis Modelle** (§3.2: OpenRouter Free, Kilo-Code Free) als empfohlener Einstieg.
   - Bestehende Schlüssel (OpenRouter, Bynara, Agent Router, NVIDIA NIM) können sofort eingetragen und getestet werden.
   - Key-Test-Button mit klarem Erfolg/Fehler-Ergebnis.
3. Danach: direkter Einstieg in den Chat. Kein Pflicht-Durchklicken weiterer Menüs.

### 9.4 Hintergrund-Anzeige (Nutzerentscheidung)

- Während langer Agenten-/Device-Läufe zeigt Android einen festen **Status-Hinweis** („Claudroide arbeitet") — schützt den Lauf vor dem Abwürgen durch das System und zeigt dem Nutzer den Zustand.
- Fertige Ergebnisse erscheinen zusätzlich als Benachrichtigung.

---

## 10. Dokumentationsstruktur (wenige, gute Dateien)

Nach dem Interview im Projekt `/home/mert/claudroide-next` anzulegen (keine Markdown-Wüste):

```
/home/mert/claudroide-next/
├── claudroide-spec.md          (diese Datei — Produkt-Spec)
├── docs/
│   ├── REQUIREMENTS.md         (MUSS/SOLL/SPÄTER, Akzeptanzkriterien)
│   ├── ARCHITECTURE.md         (Architektur §12 ausgearbeitet)
│   ├── SOURCE_INDEX.md         (Quellenübersicht §2.5)
│   ├── SOURCE_REUSE_MATRIX.md  (KEEP/ADAPT/REWRITE/REFERENCE/IGNORE je Quelle)
│   ├── SECURITY.md             (§8 ausgearbeitet)
│   ├── ROADMAP.md              (Phasen §13)
│   ├── DECISIONS.md            (Entscheidungslog)
│   └── TASKS.md                (Task-Plan §14, generiert aus Spec)
├── CLAUDE.md                   (Betriebsanleitung für den autonomen Lauf)
├── CLAUDROIDE_CURRENT_STATE.md (lebender Zustands-Checkpoint)
├── _sources/
│   ├── github/                 (shallow Klones öffentlicher Repos)
│   └── local/                  (Pfad-Referenzen auf lokale Schwesterprojekte)
├── progress/
│   └── BUILD-STATE.md          (Checkpoint-Mechanik wie bisher)
└── (App-Code: kopiert aus ~/claudroide, dann weiterentwickelt)
```

Anpassungen an dieser Struktur sind erlaubt, wenn die Analyse eine bessere Lösung ergibt — die Mindestmenge ist: Spec, Requirements, Architecture, Source-Index, Security, Roadmap, Tasks, CLAUDE.md, CURRENT_STATE.

---

## 11. Qualitätsregeln (hart)

- Kein „fertig", bevor die Funktion **tatsächlich getestet** wurde.
- Keine erfundenen Tests, keine erfundenen APIs, keine Behauptung „funktioniert" bei nur kompiliertem Code.
- **Qualitäts-Gate (Entscheidung):** Unit-Tests + Build erfolgreich, **zusätzlich** nach Meilensteinen: APK auf dem A56 installieren und kritische Pfade dort prüfen, plus **Screenshot-Verifikation** der UI als Beleg.
- Tests passend zum Feature; Build prüfen; UI prüfen; Fehlerzustände prüfen; Regressionen prüfen.
- Ehrliche Statusberichte (Muster des alten Repos: „honest state, not a wish list").
- Sprache der Dokumentation/Commits: **Englisch**, mit **deutscher Zusammenfassung** am Anfang der Spec bzw. in Meilensteinberichten (Entscheidung aus Interview-Runde 5).

---

## 12. Produktarchitektur (Zielbild, vom Agenten zu verifizieren/anzupassen)

Der Codeumzug aus `~/claudroide` liefert: `core/` (design, i18n, security, platform, navigation, diagnostics) + `feature/` (chat, project, settings, onboarding, provider, agent, skills, mcp, git, linux, control). Zielarchitektur daraus abgeleitet:

| # | Baustein | Quelle/Anmerkung |
|---|---|---|
| 1 | Android UI (Compose, MVVM) | alt: feature/* |
| 2 | Chat Engine | Multi-Modus-Chat |
| 3 | Agent Runtime | Job-Manager, Semaphore (1 Shell, 1 Subagent), Autonomie-Loop |
| 4 | Context Manager | §5; Token-Budgeting, Kompression, Rotation (Bausteine aus lokicode: Context Engine) |
| 5 | Model Router | §3.3–3.4: Rollen-Aliase, Failover, Health |
| 6 | Provider Abstraction | §3.1: Wire-Format-basiert, Custom Endpoints als Konfiguration |
| 7 | BYOK / Secrets | §3.2, §8: Keystore |
| 8 | Files / Projects | feature/project erweitern |
| 9 | Git | feature/git, GitHub-Sync §5.4 |
| 10 | Shell / Process Runtime | §5.3: leicht + PRoot-Container (feature/linux portieren) |
| 11 | Skills / Plugins / MCP / Prompts | §7: Kataloge, Progressive Disclosure |
| 12 | Session Manager | Session-/Task-State, Wiederaufnahme |
| 13 | Device Agent | §6: AccessibilityService, Gesten, Screenshot |
| 14 | Media Runtime | §1.1; Integration claude-media-bridge (SPÄTER) |
| 15 | Persistence | Room/DataStore + Disk-Snapshots §5.2 |
| 16 | Security | §8 |
| 17 | Logging / Observability | Audit-Trail, Redaction |
| 18 | Permissions | Android-Runtime-Permissions-Flows |
| 19 | Testing | Unit + Gerät + Screenshot-Gate §11 |
| 20 | Update / Migration | App-Upgrade-Pfade, Datenmigrationen |

Diese Aufteilung darf der Agent ändern, wenn die Codeanalyse eine bessere Struktur ergibt; Änderungen werden in `DECISIONS.md` dokumentiert.

---

## 13. Roadmap (vertikale Scheiben, realistisch)

| Phase | Inhalt | Anmerkung |
|---|---|---|
| **0** | Analyse + Quellen | Projekte/Repos sichten, SOURCE_INDEX + REUSE_MATRIX, Codebasis vermessen, DroidRoute-Stand frisch erheben |
| **1** | Projekt-Move + App-Grundgerüst + UI | Kopie nach claudroide-next, Banner 1:1 gesichert, Build/Test-Grün nachweisen |
| **2** | Chat + BYOK/Provider (Anthropic, OpenAI, OpenRouter) + Ein-Klick-Gratis-Provider (OpenRouter Free, Kilo-Code Free) + Auto-Routing + Auto-Failover + Kostenübersicht + Onboarding-Flow (§9.2) + DISCUSS/BUILD-Umschalter | MUSS-Kern |
| **3** | Coding-Agent vertikal: Dateien, Shell, Diffs, Verifikation, Job-Grenzen (1 Shell / 1 Subagent), Höchstautonomie, Blocker-Regel | |
| **4** | Context Manager: Kompression, Rotation, Disk-Persistenz, Wiederaufnahme | |
| **5** | Projekte/Dateien/Git vertikal: Browser, Git-UI, GitHub-Sync | |
| **6** | Device Agent: Accessibility, Gesten, Screenshot, See-Decide-Act-Verify, Not-Aus, Sperrliste, kein Zeitlimit (läuft bis zum manuellen Stopp) | Nutzer-MUSS für v1 |
| **7** | Skills/Plugins/MCP/Prompt-Kataloge, Progressive Disclosure, Awesome-Prompts-Pipeline | |
| **8** | Weitere Provider (Groq, Mistral, NIM, Bynara, Ollama Cloud, …), Custom Endpoints | |
| **9** | Media Runtime + claude-media-bridge-Anbindung, DAW-Projekt-Vorbereitung | SPÄTER-Fokus |
| **10** | DroidRoute-Integration als optionaler Provider (Stand dann erheben) | |
| **11** | Spracheingabe, Vorlesen und Wakewort (Nutzerentscheidungen Interview 3/4) | Stufe 2, direkt nach Stufe 1; Dauerhören nur mit sichtbarem Status und Android-Freigabe |
| **12** | Lokale Modelle auf dem A56 | SPÄTER |
| **13** | Polish: Performance, Security-Härtung, Automatisierung/Hintergrundtasks | durchlaufend |

Reihenfolgeänderungen sind zulässig, wenn die Analyse sie rechtfertigt (in DECISIONS.md begründen). Device Agent kommt nach Nutzerwunsch in die erste nutzbare Version (§14 MUSS).

---

## 14. Anforderungen nach Priorität

### JETZT WIRKLICH NÖTIG = Stufe 1 (MUSS)

*(die folgende MUSS-Liste entspricht Stufe 1 aus §1.5)*

### MUSS (erste richtig nutzbare Version)

1. Projekt aus `~/claudroide` nach `/home/mert/claudroide-next` kopiert; **Banner-Assets byte-identisch erhalten** (§2.2); Build + alle Tests grün nach dem Umzug.
2. Chat mit BYOK: **Anthropic, OpenAI, OpenRouter** (Mindestmenge) über Keystore-Schlüssel.
3. **Automatisches Modell-Routing nach Aufgabe** + **automatisches Failover**.
4. **Coding-Agent (teilweise, wie im Interview genannt)**: Dateien lesen/ändern/erstellen, Shell-Ausführung (leicht + PRoot wählbar), Diffs, Test-/Build-Ausführung, Ergebnis-Verifikation; harte Grenzen 1 Shell / 1 Subagent im Runtime-Design.
5. **Device Agent (Bildschirmsteuerung)**: Accessibility-first, See→Decide→Act→Verify, Not-Aus, App-Sperrliste, Live-Protokoll.
6. **Context Manager**: automatische Kompression + Rotation mit Gedächtnis-Datei; keine Arbeit geht verloren.
7. Live-Protokoll aller Agentenschritte im UI; Tages-/Monats-Kostenübersicht.
8. Autonomer Betrieb: CLAUDE.md-Bau-Loop, Blocker-Regel (2 Versuche), Commit + Push pro Block, Secret-Gate.
9. Dokumentationsstruktur §10 vollständig; SOURCE_INDEX mit **allen** GitHub-Repos (§2.5).
10. **Onboarding (§9.2):** kurze Begrüßung mit Banner, dann Setup mit Ein-Klick-Gratis-Providern (OpenRouter Free, Kilo-Code Free); vorhandene Keys (OpenRouter, Bynara, Agent Router, NVIDIA NIM) sofort eintragbar und testbar.
11. **Neues öffentliches GitHub-Repo `claudroide-next`; neue App parallel zur alten installierbar** (eigene interne App-ID, sichtbarer Name „Claudroide“).
12. **Chat-Modi mit Umschalter (DISCUSS/BUILD)**; Design-Mix Claude-Look als Basis (§1.4, §9.1); Chat-Sprache = Sprache des Nutzers; Chats dauerhaft gespeichert.
13. **Hintergrund-Status-Hinweis** für lange Läufe; Lauf-Ende mit kurzem deutschem Bericht.
14. **Kein Ausgabenlimit, keine App-Sperre, kein Zeitlimit für den Device-Agenten** (bewusste Nutzerentscheidungen).
15. **Fertig-Beweis (§1.3):** v1 gilt erst als fertig, wenn Coding-Agent UND Device-Agent je einen echten Beweis-Lauf absolviert haben.

### SOLL (Stufe 2 — direkt danach)

- Spracheingabe, Vorlesen von Antworten und dauerhaftes Zuhören mit **Wakewort** (§6a); Wakewort erst nach Stufe 1.
- Audio-, Dokumenten- und Videoauswertung (kurze Videos zuerst); Stufe 1 kann diese Dateien nur anhängen/speichern, nicht auswerten.
- Weitere Provider (Groq, Mistral, NIM, Bynara, Ollama Cloud, Orcarouter, Fastrouter, …) und Custom-Endpoint-Konfiguration.
- Skills/Plugins/MCP/Prompt-Kataloge mit Progressive Disclosure; Awesome-Prompts-Pipeline.
- Vollständige Projekte/Dateien/Git-Vertikale (Browser, Git-Verlauf).
- Globale Nutzer-Präferenzen (§7.4).
- APK-Installation + Screenshot-Verifikation als Standard-Meilenstein-Gate.

### SPÄTER (Phasen 9–13)

- Medien **erzeugen/bearbeiten** (Bilder/Audio/Musik/MIDI/Video/Dokumente), claude-media-bridge, DAW-Kollaboration (airbeat-studio u. a.). *(Ansehen/Analysieren inkl. Video ist schon Stufe 1, §7.4a.)*
- DroidRoute als optionaler Provider.
- App-übergreifender Assistent, Hintergrund-Aufgaben.
- **Lokale KI (mittlere Phase, §7.4b):** kleine Modelle auf dem Handy, Heimnetz-Modelle, USB-Modelle.
- Play-Store-Kompatibilität (nur eventuell).

### NUR EXPERIMENT

- 3-Stufen-Schriftgröße (vorbereitet, versteckt, §9.1.1).
- Tablet/Geteilt-Modus.
- Helle Standard-Variante als Voreinstellung.

---

## 15. Offene Unsicherheiten (dokumentiert, nicht geraten)

1. **DroidRoute-Stand:** bewegt sich (Nutzer arbeitet parallel); vor Phase-10-Arbeit frisch erheben. Aktueller Stand laut `status/PROGRESS.md`: T-017 pending, Provider-Schicht offen.
2. **Termux-Interaktion:** unklar, ob die leichte Shell Termux-Pakete direkt nutzen kann oder eine eigene PRoot-Instanz nötig ist — in Phase 0/1 am Gerät klären.
3. **takeScreenshot-API (API 30+) vs. MediaProjection auf dem A56 (Android 15?):** am Gerät verifizieren.
4. **Kosten-Extraktion:** nicht alle Provider liefern verlässliche Kosten-/Kontingentdaten — Übersicht zeigt nur technisch Erhegbares (Tokens immer, Kosten wo verfügbar).
5. **Gerätekonto:** A56 (8 GB RAM) begrenzt PRoot-Workloads; Build großer Projekte ggf. auf Remote auslagern (Option: GitHub Actions, wie im alten Repo).
6. **awesome-prompts vs. awesome-prompts-full:** Unterschied noch nicht analysiert (Ordner sehen identisch aus) — in Phase 0 klären.
7. **Umgebung dieser Entwicklung:** Die Entwicklung läuft selbst in einer PRoot-Distro auf dem A56 (8 Kerne, 7,2 GB RAM, 21 GB frei). Gradle-Builds sind langsam; Build-Artefakte nicht doppelt speichern.

---

## 16. Betriebsanleitung für den späteren autonomen Lauf (Kurzfassung, auszuweiten in CLAUDE.md)

1. Lies diese Spec, dann `docs/REQUIREMENTS.md`, `docs/TASKS.md`, `progress/BUILD-STATE.md`.
2. Wähle die früheste pending-Task mit erfüllten Abhängigkeiten; lade zugeordnete Skills.
3. Implementiere → teste (§11) → verifiziere auf tatsächlichem Verhalten → Task `done` → Checkpoint → Commit (+ Push an verifiziertes Remote) → nächster Task.
4. Nach 2 gescheiterten Versuchen: Blocker notieren, weiter (§4.4).
5. Meilenstein erreicht: kurze einfache Erklärung auf Deutsch (was, warum, Grundprinzip), dann autonom weiter.
6. **Lauf-Ende (Nutzerentscheidung): kurzer Bericht auf einfachem Deutsch** — fertig / offen / Blocker (Muster wie bei clawscreen). Details stehen technisch in `progress/BUILD-STATE.md`.
7. Harte Grenzen jederzeit: §2.2 (Banner + App-Symbol), §2.4 (READ-ONLY), §4.2 (Job-Grenzen), §8 (Sicherheit), §6.3 (Device-Safety).
8. Lies bei jedem Lauf `docs/`-Nutzer-Präferenzen (§7.4) und `CLAUDROIDE_CURRENT_STATE.md`.
9. Die Fertig-Definition für v1 steht in §1.3 (beide Beweis-Läufe).

---

## Anhang A: Antworten aus dem Interview (Protokoll)

| Frage | Antwort |
|---|---|
| Neu vs. weiterbauen | Kopie als Basis, neues Projekt (claudroide-next) |
| MVP-Scope | Chat + BYOK/Provider + **Bildschirmsteuerung** + teilweise Coding-Agent; DroidRoute arbeitet parallel weiter |
| App-Sprache | Englisch zuerst, Deutsch optional |
| Autonomie | Höchstautonom (alle Entscheidungen, Skills nutzen, Kontext intelligent leeren, Auslagerung an andere Provider, skip permissions) |
| DroidRoute | Eigenständig lassen; Integration nur als spätere Option planen; **alle GitHub-Repos anschauen und bewerten** |
| Provider v1 | Anthropic, OpenAI, OpenRouter + langfristig: Groq, Mistral, NIM, Bynara, Ollama Cloud, Orcarouter, Fastrouter, Kilo Code, xKiro, Agent Router, Antigravity, Bluesminds usw. |
| Key-Speicher | Android Keystore |
| Shell | Beides (leicht + PRoot), Nutzerauswahl |
| Device-Safety | Autonom mit Not-Aus + App-Sperrliste |
| Distribution | Erst APK/Sideload, Play Store nur eventuell später |
| Kontext | Kompression + Rotation mit Gedächtnis-Datei kombiniert |
| Priorität nach MVP | Alle: Projekte/Git, Skills/MCP, Media, Sprache (Reihenfolge §13) |
| Transparenz | Live-Protokoll aller Schritte |
| Git | Commit + Push pro Block |
| Name/Ordner | „Claudroide" in `/home/mert/claudroide-next` |
| Kosten | Tages-/Monatsübersicht |
| Daten | Lokal + GitHub-Sync |
| Routing | Automatisch nach Aufgabe |
| Spec-Sprache | Englisch mit deutscher Zusammenfassung |
| Blocker-Regel | Notieren, weiter zur nächsten Aufgabe |
| Nutzer-Gedächtnis | Ja, globale Nutzer-Präferenzen |
| Qualitäts-Gate | Tests + Gerät-Stichproben + Screenshots |
| **Interview 2 (Runde 1)** | Neues öffentliches Repo `claudroide-next`; beide Apps nebeneinander (eigene interne App-ID); vorhandene Keys: OpenRouter + Bynara + Agent Router + NVIDIA NIM (kein Anthropic/OpenAI-Key vorhanden); kein Ausgabenlimit |
| **Interview 2 (Runde 2)** | Chat-Design = Claude-App-Look als Basis, beste Teile von ChatGPT als Mix (keine 1:1-Kopie); Onboarding = kurze Begrüßung, dann Setup mit Ein-Klick-Gratis-Providern (OpenRouter Free, Kilo-Code Free); Hauptnutzung: alles ungefähr gleich; Device-Agent läuft bis zum manuellen Stopp (kein Zeitlimit) |
| **Interview 2 (Runde 3)** | Claude-Look als Basis bestätigt; beide Gratis-Provider einbauen; zwei Modi mit Umschalter (DISCUSS/BUILD); Gerätetest = Agent per adb + Nutzer-Stichproben kombiniert |
| **Interview 2 (Runde 4)** | Projekt-Ordner: app-eigen + frei wählbar pro Projekt; Projekt-Sync in ein privates Sammel-Repo; Subagent-Rolle frei wählbar je Aufgabe; Aufgabensystem = Einzeldateien wie bisher |
| **Interview 2 (Runde 5)** | Hintergrund: fester Status-Hinweis; Chat-Sprache: wie der Nutzer schreibt; Lauf-Ende: kurzer deutscher Bericht; App-Symbol unverändert behalten |
| **Interview 2 (Runde 6)** | Updates: so automatisch wie möglich (adb-Installation, nur noch bestätigen); Chats: für immer behalten; keine eigene App-Sperre (Handy-Sperre reicht); Fertig-Definition v1: Chat + Coding-Agent-Beweis UND Device-Agent-Beweis |
| **Interview 3 (Runde 1)** | Start: Ladeseite (Banner-Motiv + „Letzte Arbeit wird geladen“), dann bei Neustart direkt Chat, aus dem Hintergrund dort weitermachen; nicht abgeschickte Texte bleiben erhalten; Navigation: 3 Bereiche unten + Bildschirmsteuerung als ruhigerer 4. Punkt; **dunkel als Standard**; eigener Claudroide-Look grün/beige, umschaltbar in warm-rötlich (Claude-Nähe); **nur Handy zuerst** |
| **Interview 3 (Runde 2)** | Nav-Vorschlag bestätigt (3 + ruhiger 4. Punkt); Gesprächsliste: eigener Mix aus Claude-App-Liste + Suche + Projektzuordnung, einfach bedienbar; Anhänge: **alles inkl. Video**; Modell pro Chat: automatisch + kleines Auswahlfeld; Schriftgröße: Android-System, 3-Stufen-Einstellung vorbereitet aber versteckt |
| **Interview 3 (Runde 3)** | Projekt = Ordner + Verlauf + Regeln + **Wissenssammlung**; Projektquellen wie in der originalen Claude-App (neu, Ordner vom Handy, von GitHub); Programmierbereich: **alles automatisch** (auch Löschen, keine Bestätigungen); **Sicherungen + unbegrenztes Rückgängig** |
| **Interview 3 (Runde 4)** | Verbindungsabbruch: 2–3 automatische Versuche → Failover → ehrliche Meldung; **Zuverlässigkeit vor Geschwindigkeit**; Berechtigungen: Bedienungshilfe + Bildschirmaufnahme + Mikrofon + Benachrichtigungen, mit direktem Sprung in die Android-Einstellung; Sicherung: Handy + GitHub + wöchentliche Automatik-Sicherung |
| **Interview 3 (Runde 5)** | **Banner-Widerspruch aufgelöst:** Motiv bleibt, optische Verbesserung erlaubt, jede Änderung braucht Nutzer-OK; Lizenz: **Apache 2.0**; README: für Nutzer + Entwickler (deutsche Kurzfassung + englischer Hauptteil); GitHub: **Issues + Releases**; Releases bei jedem Meilenstein mit APK |
| **Interview 3 (Runde 6)** | Steuerungs-Ziele: Musik-Apps + Browser + Dateimanager, sonst alle außer Sperrliste; Bestätigungen: nur Not-Aus + Sperrliste (keine weiteren Pflicht-Bestätigungen); Mikrofon: **Wakewort von Anfang an gewünscht** → in Stufe 2 einsortiert (§1.5); spätere Antwort aus Interview 4 setzt Spracheingabe und Sprachausgabe ebenfalls auf Stufe 2 |
| **Interview 3 (Runde 7)** | Medien Stufe 1: Bilder + Audio + Dokumente + **Videos** (ansehen/analysieren); lokale KI: alle vier Szenarien (Handy, Heimnetz, USB, Gratis-Online), mittlere Phase; Akku/Speicher: vorher prüfen + sauber pausieren + automatisch weiter; KI-Vermeidung: Angst-Formulierungen, Selbstbezüge („Als KI…“), Emoji-Übertreibung, künstliche Schreibweise |
| **Interview 3 (Runde 8)** | **Zwei Ausbaustufen** (§1.5); Wakewort direkt nach Stufe 1; ohne Internet: was geht, läuft weiter; Unsicherheit: Mischung (unwichtig selbst entscheiden + dokumentieren, wichtig nachfragen); lokale KI in mittlerer Phase |
| **Interview 3 (Runde 9)** | Gesendete Nachrichten: bearbeiten + Versionen sichtbar; Arbeits-Status: Live-Protokoll + Statuszeile; Dateien: ansehen mit Farben + bearbeiten; beliebig viele Gespräche parallel; Ladebild: Logo + vorheriger Zustand wird geladen |
| **Interview 4 (Runde 1)** | Medien: nur Bilder in Stufe 1 auswerten; Audio/Dokumente/Video zunächst später verstehen; bei Video war „nur kurze Clips“ gewählt, durch spätere Präzisierung vorläufig überstimmt; Spracheingabe und Vorlesen beide erst nach Stufe 1 |
| **Interview 4 (Runde 2)** | Bestätigt: Audio/Dokumente/Video dürfen in Stufe 1 angehängt werden, werden aber erst später verstanden; Wakewort darf Claudroide als Android-Assistent erfordern, wenn nötig |
| **Interview 4 (Runde 3)** | Im Programmierprojekt alles automatisch einschließlich Löschen, aber nur echte Dateien im gewählten Ordner; alle Android-Rechte gesammelt und erklärt beim ersten Einrichten zeigen; Geld-, Banking-, Bezahl- und Passwort-Apps standardmäßig sperren |
| **Interview 4 (Runde 4)** | Dateiinhalte nur senden, wenn ausdrücklich angehängt/ausgewählt; GitHub-Sicherung je Projekt, standardmäßig aus; lokale wöchentliche Sicherung umfasst alles außer Zugangsschlüsseln, bei knappem Speicher vor Löschen warnen und fragen; KI darf Dateien des offenen Projekts lokal durchsuchen |
| **Interview 4 (Runde 5)** | Not-Aus beendet sofort alle laufenden KI-, Befehls- und Bildschirmarbeiten; Projekt wird nach Bestätigung endgültig gelöscht; wenn Speicher fehlt, keine Sicherung löschen und Dateiänderungen pausieren; abgelehnte Android-Rechte deaktivieren nur die zugehörige Funktion, übrige App bleibt nutzbar |

---

## Anhang B: Abschlussübersicht nach Interview 3

### Endgültig entschieden (zusätzlich zu Interview 1 + 2)

Start-/Ladeverhalten, nicht abgeschickte Texte bleiben erhalten; Navigation (3 + ruhiger 4. Punkt); dunkel als Standard; grün/beige ↔ warm-rötlich umschaltbar; nur Handy zuerst; Gesprächsliste mit Suche + Projektzuordnung; Anhänge inkl. Video; Modellauswahl klein im Chat; Nachricht-Bearbeiten mit Versionen; beliebig viele Gespräche; Datei-Ansicht mit Farben + Bearbeiten; Live-Protokoll + Statuszeile; Projekt = Ordner + Verlauf + Regeln + Wissenssammlung; Projektquellen wie Claude-App; Programmierbereich vollautomatisch (auch Löschen); Sicherungen + unbegrenztes Rückgängig; Verbindungsfehler: 2–3 Versuche → Failover → ehrliche Meldung; Zuverlässigkeit vor Geschwindigkeit; Berechtigungen nur bei Bedarf mit Sprung in die Android-Einstellung; wöchentliche Automatik-Sicherung; **Banner-Widerspruch aufgelöst** (Motiv bleibt, Verbesserung mit Nutzer-OK je Änderung); Lizenz Apache 2.0; README zweistufig (deutsch einfach + englisch technisch), ehrlich ohne Marketing; Issues + Releases bei jedem Meilenstein mit APK; Steuerungs-Ziele Musik/Browser/Dateimanager, sonst Sperrliste; Sprachausgabe Stufe 1, Wakewort Stufe 2; Medien-Analyse (inkl. Video) Stufe 1; lokale KI alle vier Szenarien in mittlerer Phase; Akku/Speicher vorher prüfen + sauber pausieren; offline läuft was geht; Unsicherheit: Mischung aus selbst-entscheiden (dokumentiert) und Nachfragen (bei großen Folgen); KI-Slop-Liste erweitert (§18).

### Gegenüber der vorherigen Spec geändert

1. **Banner-Regel gelockert:** von „nie verändern“ zu „Motiv bleibt, optische Verbesserung mit Nutzer-OK erlaubt“ (§2.2).
2. **Sprache neu sortiert:** Sprachausgabe von „später“ → Stufe 1; Wakewort von „später“ → Stufe 2 (§6a).
3. **Medien-Analyse (inkl. Video) von „später“ → Stufe 1** (nur Ansehen/Analysieren; Erzeugen/Bearbeiten bleibt später) (§7.4a).
4. **Zwei Ausbaustufen eingeführt** (§1.5) — die Fertig-Definition §1.3 gilt für Stufe 1.
5. **Programmierbereich:** von „Löschen braucht Bestätigung“-Option zu „wirklich alles automatisch“ (§4.3).

### Seit Interview 4 zusätzlich entschieden

- Stufe 1: Bilder werden verstanden; Audio, Dokumente und Videos können angehängt/gespeichert werden, aber noch nicht ausgewertet.
- Stufe 2: Audio-/Dokumenten-/Videoauswertung (kurze Videos zuerst), Spracheingabe, Vorlesen und Wakewort.
- Projektdateien dürfen im aktiven Projekt lokal durchsucht werden; Inhalte werden nur nach ausdrücklicher Auswahl an einen KI-Dienst geschickt.
- GitHub-Sicherung für Nutzerdaten standardmäßig aus, pro Projekt einschaltbar; lokale wöchentliche Sicherung enthält Chats, Anhänge, Einstellungen und Projekte, aber keine Zugangsdaten.
- Innerhalb des gewählten Projektordners automatische Änderungen und Löschungen; keine Änderung an Verknüpfungszielen außerhalb dieses Ordners.
- Not-Aus stoppt alles sofort; Projektlöschung nach Bestätigung endgültig; bei Platzmangel nichts automatisch löschen und Änderungen pausieren.
- Android-Rechte gesammelt erklärt beim ersten Einrichten; Ablehnung blockiert nur die betroffene Funktion.
- Standard-Sperrliste: Geld-, Banking-, Bezahl- und Passwortspeicher-Apps.

### Noch offen (bewusst, mit Standard-Fallback)

- DroidRoute-Integrationsstand (vor Phase-10 frisch erheben) — Standard: nicht koppeln.
- Termux vs. eigene PRoot-Instanz für die leichte Shell — Standard: am Gerät in Phase 0/1 klären.
- takeScreenshot vs. MediaProjection auf dem A56 — Standard: am Gerät verifizieren.
- awesome-prompts vs. awesome-prompts-full — Standard: Phase 0 klären.

### Was Claude Code noch braucht, um autonom zu bauen

1. docs/REQUIREMENTS.md, ARCHITECTURE.md, ROADMAP.md, SECURITY.md aus dieser Spec ableiten.
2. Task-Plan (Einzeldateien wie bisher) mit Akzeptanzkriterien je Task erstellen.
3. Öffentliches GitHub-Repo `claudroide-next` anlegen (Apache 2.0, Beschreibung, Themen), Codebasis kopieren, Banner byte-identisch sichern, ersten sauberen Commit + Push.
4. README nach §18 anlegen und fortan nach jedem Meilenstein aktualisieren.

### Fertig-Erkennung je Funktion (Kurzfassung)

Eine Funktion gilt erst als fertig, wenn: Unit-Test grün + Build grün + Verhalten am Gerät nachgewiesen (adb-Installation + Screenshot) + ehrlicher Eintrag in BUILD-STATE.md. Kein „fertig“ bei nur kompiliertem Code (§11).

---

## 17. GitHub-Regeln (NEU, Nutzerentscheidungen Interview 3)

| Thema | Entscheidung |
|---|---|
| Repository | `mertgoevse-wq/claudroide-next`, **öffentlich**, ab dem ersten Tag |
| Lizenz | **Apache 2.0** |
| Beschreibung | Kurze, ehrliche Beschreibung (z. B. „Personal AI workspace for Android: chat, coding agent, device control — bring your own keys.“) |
| Themen/Stichwörter | android, kotlin, jetpack-compose, ai, claude-code, byok, coding-agent, open-source |
| Issues | **Ja**, von Anfang an (Aufgaben/Fehler können dort sichtbar gemacht werden; Haupt-Aufgabenverwaltung bleibt aber das lokale Task-System §4.8) |
| Releases | **Ja**, bei jedem Meilenstein (fertige Phase = Release mit installierbarer APK + kurzer ehrlicher Beschreibung) |
| Discussions/Changelog | Nein/erst später — nur was wirklich benutzt wird |
| Commits | Früh, klein, nachvollziehbar (ein sinnvoller Commit pro Aufgabenblock); **vor jedem Push: Secret-Scan, keine großen/temporären Dateien, keine Build-Reste, keine privaten Daten** |
| Sichtbarkeit | Keine Zugangsdaten, keine privaten Schlüssel, keine geheimen Zugangsdaten im Repo — niemals |

## 18. README-Regeln & Anti-KI-Slop (NEU, Nutzerentscheidungen Interview 3)

### README

- **Zielgruppe:** Nutzer + Entwickler — **deutsche Kurzfassung der Grundidee am Anfang, danach englischer Hauptteil** für Entwickler.
- **Inhalt von Anfang an:** Was ist Claudroide (ehrlich, konkret)? Was kann es heute wirklich (Status mit Vorhanden/Teilweise/Geplant)? Wie installiert man es (APK)? Welche Rechte braucht die App und warum? Wie richtet man KI-Dienste ein (Ein-Klick-Gratis + eigene Schlüssel)? Welche Funktionen sind experimentell? Voraussetzungen (Galaxy A56, Android-Version)? Roadmap (jetzt/später/experimentell)? Hinweise für Mitentwickler?
- **Aktualisierung:** nach jedem Meilenstein — nicht bei jeder Kleinigkeit. Die README spiegelt **immer den tatsächlichen Stand** wider.
- **Verbotsliste:** „revolutionary“, „next generation“, „the future of AI“, „world-changing“, leere Superlative, Fake-Zahlen, erfundene Bewertungen, erfundene Funktionen, unnötige Abzeichen, Emoji-Wolke, Marketing-Fülltexte.
- **Banner:** Motiv bleibt (§2.2); Einbau darf optisch verbessert werden (mit Nutzer-OK je Änderung); Darstellung auf Desktop und Handy prüfen.

### Anti-KI-Slop (gilt für Code, Dokumentation, UI-Texte, Commits)

- Keine unnötigen Dateien, Klassen, sinnlosen Abstraktionen; keine mehrfach vorhandenen Lösungen für dasselbe Problem; kein toter Code; kein halbfertiger Code ohne Grund.
- Keine überlangen Kommentare, die nur den Code beschreiben; keine künstlichen Dokumentationen; keine gefälschten Tests; keine vorgetäuschte Fertigstellung.
- Zusätzlich (Interview 3): keine **Angst-Formulierungen** der KI (rechtliche Panik-Hinweise statt Klartext), keine **Selbstbezüge** („Als KI-Modell kann ich nicht …“ — direkt liefern, was möglich ist), keine **Emoji-Übertreibung** (nur wo wirklich sinnvoll), keine **künstliche Schreibweise** (ruhig, ehrlich, direkt).
- Keine riesige Zahl von Projektdateien nur um „professionell“ auszusehen; Dokumentstruktur §10 bleibt bewusst klein.
