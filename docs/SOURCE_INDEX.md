# Quellen-Index und Übernahme-Matrix

**Stand:** 2026-10-08 · **Erhoben von:** Claude Code (Freebuff), per Ordner-Sichtung und `gh`
**Regel:** Quellen sind **read-only**. Nichts löschen, nichts ändern, nichts pushen. Änderungen nur in `/home/mert/claudroide-next`.

**Kürzel:** KEEP = direkt weiterverwenden · ADAPT = übernehmen und anpassen · REWRITE = Idee gut, Neu­bau · REFERENCE = nur als Vorbild/Nachschlagewerk · IGNORE = nicht relevant

---

## 1. Umfang der Untersuchung

| Was | Ergebnis |
|---|---|
| Lokale Ordner unter `/home/mert` | 24 auf erster Ebene, davon 22 mit Git-Verlauf |
| Online-Repos von `mertgoevse-wq` (per `gh`, 2026-10-08) | **43** — 21 öffentlich, 22 privat |
| Fähigkeiten installiert | 40 in `~/.claude/skills` + `~/.agents/skills`, Marktplätze: `ecc` (293 Skills), `brag`, `gradle-skills`, `superpowers-marketplace`, `claude-plugins-official`, `awesome-claude-code-plugins` |
| Leer bzw. inhaltsgleich (IGNORE) | `loki-workbench`, `claude-project-workbench`, `loki-health`, `-veldra-music-intelligence`, `cc-on-usb`, `exy`, `freebuffclaw-bridge`, `clawdroide`, `claude-screen-use`, `claudroide-build`, `media` |
| Werkzeuge der Maschine | Java 17 · Gradle · adb · Node 24 · python3 · uv · git · gh (ausführlich: Spec §2.7) |

---

## 2. Matrix (nach Nutzen geordnet)

### 2.1 Sehr wertvoll — KEEP / ADAPT

| Quelle | Pfad / Link | Was vorhanden ist | Was brauchbar ist | Umgang | Warum | Abhängigkeiten | Lizenz-/Herkunftsrisiko |
|---|---|---|---|---|---|---|---|
| **claudroide** | `/home/mert/claudroide` · `github.com/mertgoevse-wq/claudroide` | Kotlin/Compose, 378 Quellen, 127 Testdateien, 137 Aufgabendateien; `core/` + `feature/` (agent 29, provider 26, chat 15, project 24, linux 83, git 7, mcp 9, skills 5); Werkzeuge `secret_gate.py`, `sync_frontmatter.py`, Marken-Bilder | **Fast alles** — dies ist die Kopierbasis. Besonders: Anbieter-Schicht, Agenten-Laufzeit, Kontext-Verwaltung, Projekt-Grenzen, Rechte-Zentrum, Sicherheits-Kern, Linux/Container, Design-Token, Terminal-Farben, Sicherungen | **KEEP** (Umzug), Stellenweise **ADAPT** (neue App-Kennung, zwei Ansichten, Autonomie ohne Rückfragen) | Größter fertiger Bestand, Tests grün laut Repo-Dokumenten (muss **neu gemessen** werden) | Java 17, Gradle, Android SDK 26–35 | **Keine Lizenzdatei** im Repo → neue Apache-2.0-`LICENSE` selbst anlegen. Vor dem Umzug auf private Notizen prüfen |
| **claude-code-android** | `…/claudroide-first-party/claude-code-android` | Kotlin-Mehrfachmodul (androidApp, shared/core, shared/data, shared/domain, build-logic), 15 Doku-Ebenen, Absicherungs-Werkzeuge, Haken, Selbstlauf-Skript | **Engine-Technik** (glibc-Unterbau, Prüfsummen-Kontrolle, Rückfall), **Doku-Aufbau**, **Absicherungen**, Vordergrunddienst für lange Läufe | **ADAPT** (Technik und Werkzeuge), **REFERENCE** (Doku-Aufbau) | Zeigt, wie man die echte Claude-Maschine auf dem Handy betreibt und wie man Läufe absichert | Bun/Node für Werkzeuge; proot-Profil großzügig | **Apache-2.0 + NOTICE + THIRD_PARTY_NOTICES** → bei Übernahme Herkunftszeile behalten |
| **lokicode** | `…/claudroide-first-party/lokicode` | 272 Quellen, 167 Testdateien; `domain/` mit agent, chat, context, memory, provider, router, skills; `CAPABILITIES.md` mit „verifiziert"-Einträgen | Anbieter-Register, **Modellwahl mit Ausfall-Ersatz**, SSE-Chat-Übertragung, Datei-Explorer mit Baum/Suche/Diff, Offline-Speicher, Fähigkeiten-Register | **ADAPT** (gute Teile übernehmen) | Erprobte Teile genau in den Bereichen, die Claudroide braucht; sauber getrennte Domänen | Kotlin/Compose | **Keine Lizenzdatei** (README nennt Apache-2.0 ohne Datei) → eigener Code, keine Fremdangabe; Notiz in `DECISIONS.md` |
| **mobile-linux-lab** | `…/claudroide-first-party/mobile-linux-lab` | Kotlin/Compose, 142 Quellen; `DeviceCapabilityReport`, `CapabilityDetector`, `BackendRegistry`, `PRootBackend` + Tests; **50+ Plan-Dokumente** (PRoot, NPU, GPU, Performance, TASK_GRAPH, SKILLS_MATRIX, THREAT_MODEL) | **Geräteprüfung** (genau das, was §7.4b braucht), Container-Unterbau, Terminal (lokal + SSH), Plan-Dokumente als Vorbild | **ADAPT** + **REFERENCE** | Zweite, saubere Umsetzung des Container-Themas → Vergleich mit `claudroide/feature/linux` (Spec §5.3) | Android SDK, PRoot-Umgebung | Keine Lizenzdatei → eigener Code |
| **clawscreen** | `/home/mert/clawscreen` | Python-Werkzeuge, deutsche Spec v2.1 mit **am A56 gemessenen** Befunden (Bildschirmwege, Modell-Liste mit Laufzeiten, NPU nein / GPU Vulkan ja), `anti-slop.py`, `sync_frontmatter.py`, `BUILD-STATE.md` + Wiederaufnahme-Skill | ADB-Verfahren (ein Server, `ADB_SERVER_SOCKET`, scrcpy), Zwischenablage-Weg für Umlaute, Leiser-Doppeldruck als Nothalt, Messmethodik | **ADAPT** (Verfahren in die App), **REFERENCE** (Messwerte) | Einzige **gemessene** Quelle für Bildschirm und Chip auf genau diesem Gerät | Termux + `android-tools` | Keine Lizenzdatei → eigener Code |
| **design-skill-library** | `…/claudroide-first-party/design-skill-library` | 321 kuratierte Design-Fähigkeiten aus 74 Sammlungen, `CURATION*.tsv`, `ATTRIBUTION.md`, `select-skills.py`, `slop-scan.sh`, Abruf-Nachladen (1 Beschreibung statt 321) | **Katalog-Muster** (Progressive Disclosure), Anti-Slop-Tor, Herkunfts-Verzeichnis | **ADAPT** (Muster + später verkleinerter Katalog in der App) | Löst §7.1 (kein Kontext-Überlauf) und §18 (keine Massenware) mit fertigem Werkzeug | Python | **Fremde Inhalte aus 74 Repos** → Herkunftszeilen nötig (§2.4) |
| **claude-media-bridge** | `/home/mert/claude-media-bridge/claude-media-bridge` | TypeScript, MCP-Server, 7 Anbieter, „Ein-Klick-Anmeldung → kostenloses Modell", Installation via Skript | MCP-Anbindung, **Ein-Klick-Gratis-Muster**, Medien-Erzeugung | **ADAPT** | Deckt §3.2 und §7.2 mit funktionierendem Vorbild | Node/Bun | MIT → Herkunftszeile bei Übernahme |
| **airbeat-studio** | `…/claudroide-first-party/airbeat-studio` | Steuersprache v1.0: binäre + JSON-Pakete, IMU bis 120 Hz, Simulator/Wiederabspieler; Android-Sensordiagnose | **Steuerprotokoll Handy ↔ Musik-Programm** | **ADAPT** (später) / **REFERENCE** | Genau die Brücke, die der Bildschirm-Agent für Musik-Apps braucht | Node, Android | Keine Lizenzdatei → eigener Code |
| **Genesis_Harness** | `…/claudroide-first-party/Genesis_Harness` | Python, 229 Dateien, 56 Fähigkeiten, Rollen-Register, anbieter-neutraler Modell-Zugriff mit Ausfall-Ersatz | Modell-Zugriff ohne Anbieterbindung, Rollen-Register | **REFERENCE** (Muster) | Sauberes Vorbild für Provider-Neutralität | Python | README nennt MIT, **keine Lizenzdatei** → prüfen |
| **droidroute** (+ `droidroute-test`) | `/home/mert/droidroute/droidroute` · privat, MIT | Kotlin, Ktor-Server, Vordergrunddienst, Port 8787, Bind-Modi mit Auth-Untergrenze, Schlüsselverwaltung, `handbooks/` | Lokale Tür, Anbieter-Schicht (spätere Option), Handbuch-Aufbau | **REFERENCE** (heute), **ADAPT** (Phase 11, nach Standserhebung) | Nicht koppeln (Nutzerentscheidung), aber als optionaler Dienst vormerken | Gradle, Ktor | MIT, privat → nichts ins öffentliche Repo |
| **PerpyBridge** | `/home/mert/perpybridge` · privat | JavaScript, Spec, Kostenschutz, Null-Protokollierung | Kostenkontrolle, Protokoll-Armut | **REFERENCE** | Muster für §3.6 und §8 | Node | NOASSERTION → prüfen |

### 2.2 Nur als Vorbild — REFERENCE / REWRITE

| Quelle | Pfad / Link | Was vorhanden ist | Umgang | Warum |
|---|---|---|---|---|
| VELDRA | `…/claudroide-first-party/VELDRA` | Remix/Vite-Arbeitsumgebung (bolt.diy-Ableitung), 369 TS-Dateien, Umzugsbericht | **REFERENCE** | Bericht belegt: WebView kann keinen Node-Unterbau → **bestätigt den nativen Weg**. Fremde Ableitung → nicht kopieren |
| bolt-diy-android | `…/claudroide-first-party/bolt-diy-android` | gleicher Umzug, 254 TS-Dateien | **REFERENCE** | wie oben |
| aster-code | `…/claudroide-first-party/aster-code` | „Coding-Agent Studio", Claude-artige Chat-Oberfläche, TS/React | **REFERENCE** | UI-Ideen für Chat + Werkzeugkarten |
| aureon-desk | `…/claudroide-first-party/aureon-desk` | große TS/Python-Sammlung, QA-Checklisten, Roadmap | **REFERENCE** | Prüf-Checklisten als Vorbild |
| PocketCodeAgent | `…/claudroide-first-party/PocketCodeAgent` | Android-Kotlin, 95 Quellen, Chat + Datei-Editor + Keystore-Schlüssel, Phasenberichte | **REFERENCE** | Frühphase eines sehr ähnlichen Produkts — Fehler vermeiden |
| flylab | `/home/mert/flylab/flylab` | Android-Kotlin, 101 Quellen, Simulations-Sandkasten | **REFERENCE** | Android-Aufbau, andere Fachrichtung |
| AI-Agent-OS | öffentlich | Architektur, Roadmap, Zustandsdateien, `configs/`, `demo.py` | **REFERENCE** | Zeigt Zustands- und Rollendokumente |
| Linear | `/home/mert/Linear` | nur Dokumente (94 `.md`), gegliedert 01–31 incl. Mess-, Geräte- und Bild-Prüfpläne | **REFERENCE** | Gutes Vorbild für Prüfpläne |
| veldra-video-analysis | öffentlich | Video-Analyse-Dokumentation | **REFERENCE** | später für Medien (§13 Phase 10) |
| OMNI-Agent-Swarm, OMNI-Claude, AI-Workspace-Vault, Prompt_Library, skill-repo, EvoSphere, design-skill-library-dev, CryptoPilot-AI, veldra-music-intelligence, aurelis, grid-proof-messenger, Bolt-android | `github.com/mertgoevse-wq/<name>` (teils privat) | Rollen-, Prompt-, Wissens- und Skill-Sammlungen; teils nur Dokumente | **REFERENCE**, einzeln prüfen | Können Bausteine liefern; Inhalte müssen **stichprobenartig** gelesen werden, nichts blind übernehmen |
| claude-deskdroide („Clawdroide") | `/home/mert/claude-deskdroide` | Android-Entwurf für Nicht-Programmierer, BYOK + eigene Endpunkte, Kennung `de.clawdroide.app` | **REFERENCE** | Sehr früher Entwurf desselben Ziels |

### 2.3 Nicht verwenden — IGNORE

| Quelle | Grund |
|---|---|
| **AI-Agents** (privat) | Sammlung **fremder** Projekte (SWE-agent, crewAI, langchain …) → bleibt privat, kein Code daraus |
| **Retrieval-based-Voice-Conversion-WebUI** (privat) | **Kopie eines fremden** Projekts → bleibt privat |
| **LiteLLM** (privat) | nur eine `config.yaml`, kein eigener Code |
| loki-workbench, claude-project-workbench, loki-health, -veldra-music-intelligence | leer (0 Dateien) |
| cc-on-usb, exy, freebuffclaw-bridge, clawdroide | leer |
| claude-screen-use | Inhaltsgleich mit clawscreen |
| claudroide-build, media | je 1 Datei |
| claudroide-first-party/claudroide | zweite Kopie des Haupt-Repos |

---

## 3. Fremde Quellen (nur mit Lizenzprüfung)

| Zweck | Quelle | Lizenz | Ziel |
|---|---|---|---|
| Modelle auf Grafik/Prozessor | `github.com/ggml-org/llama.cpp` | MIT | Modell-Laufzeit in der App |
| NPU-Weg | `github.com/google-ai-edge/LiteRT` + MediaPipe LLM Inference | Apache-2.0 | Forschungsblock §23 |
| NPU-Weg (Hersteller) | `developer.samsung.com/neural/overview.html` | Samsung-Bedingungen, Partnerfreigabe | Forschungsblock §23 |
| Bildschirm-Übertragung (Notweg) | `github.com/Genymobile/scrcpy` | Apache-2.0 | ADB-Notweg §6.3 |
| Fähigkeiten-Sammlungen | `obra/superpowers`, `gradle/gradle-skills`, `affaan-m/ECC`, `ccplugins/awesome-claude-code-plugins`, `latent-spaces/brag`, `skydoves/compose-performance-skills`, `chrisbanes`-Skills | je prüfen | Spec §24 |
| Design-Fähigkeiten (321 / 74 Repos) | über `design-skill-library` + `ATTRIBUTION.md` | je Ursprung prüfen | erst beim Bauen |

**Pflicht:** Herkunft, Lizenz, Stand, Zweck, Sicherheitsbefund je Quelle; Eintrag in `docs/DECISIONS.md`. Bei unklarer Herkunft **nicht** ins öffentliche Repo.

---

## 4. Nächste Schritte für diesen Index (Phase 0)

1. Je Quelle die **echten** Pfade eintragen (`_sources/local/…`, `_sources/github/…`).
2. Je Baustein **KEEP/ADAPT/REWRITE** endgültig festlegen — nach Sichtung der Dateien, nicht nach Vermutung.
3. Private Repos mit Token lesen; **nichts** davon ins öffentliche Repo.
4. Ergebnisse in `docs/DECISIONS.md` festhalten; Widersprüche zur Spec sofort korrigieren.
