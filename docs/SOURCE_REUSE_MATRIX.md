# SOURCE_REUSE_MATRIX — Übernahme-Entscheidungen je Quelle

**Stand:** 2026-10-08 · **Grundlage:** `docs/SOURCE_INDEX.md` (Matrix §2) · **Regel:** Entscheidung **nach** Datei-Sichtung, nicht nach Vermutung. Nicht geprüft = **UNGEPRÜFT** (nicht KEEP/ADAPT/REWRITE/REFERENCE/IGNORE).

---

## Legende

| Kürzel | Bedeutung |
|---|---|
| **KEEP** | Direkt weiterverwenden (Code/Struktur 1:1 übernehmen) |
| **ADAPT** | Übernehmen und anpassen (neue App-Kennung, Architektur-Anpassungen) |
| **REWRITE** | Idee gut, aber Neu-Bau nötig (Clean-Room, andere Abhängigkeiten) |
| **REFERENCE** | Nur als Vorbild/Nachschlagewerk, kein Code übernehmen |
| **IGNORE** | Nicht relevant für Claudroide |
| **UNGEPRÜFT** | Noch nicht gesichtet — Entscheidung steht aus |

---

## 1. Sehr wertvolle Quellen (aus SOURCE_INDEX.md §2.1)

| Quelle | Entscheidung | Begründung | Status |
|---|---|---|---|
| **claudroide** (`~/claudroide`, GitHub `mertgoevse-wq/claudroide`) | **ADAPT** | Kopierbasis für Phase 1.1 — neue App-Kennung `org.claudroide.next`, gleiche Assets byte-identisch, Tests neu messen | **UNGEPRÜFT** — Phase 0.3/1.1 |
| **claude-code-android** (`_sources/github/claude-code-android`) | **ADAPT** (Technik/Werkzeuge), **REFERENCE** (Doku-Aufbau) | Engine-Technik (glibc-Unterbau, Prüfsummen, Rückfall), Absicherungs-Werkzeuge, Haken, Selbstlauf-Skript, 15 Doku-Ebenen | **UNGEPRÜFT** — Phase 0.2/1.4 |
| **lokicode** (`_sources/github/lokicode`) | **ADAPT** | Anbieter-Register, Modellwahl mit Failover, SSE-Chat, Datei-Explorer, Offline-Speicher, Fähigkeiten-Register | **UNGEPRÜFT** — Phase 2/7 |
| **mobile-linux-lab** (`_sources/github/mobile-linux-lab`) | **ADAPT** + **REFERENCE** | Geräteprüfung (CapabilityDetector, BackendRegistry), Container-Unterbau, 50 Plan-Dokumente — Vergleich mit claudroide/feature/linux für Task 0.6 | **UNGEPRÜFT** — Task 0.6 |
| **clawscreen** (`_sources/local/clawscreen`) | **ADAPT** | ADB-Verfahren (ein Server), Zwischenablage-Umlaute, Leiser-Doppeldruck, Messmethodik, anti-slop.py, sync_frontmatter.py | **UNGEPRÜFT** — Phase 6, 10.2 |
| **design-skill-library** (`_sources/github/design-skill-library`) | **ADAPT** | Katalog-Muster (Progressive Disclosure), Anti-Slop-Tor, select-skills.py, slop-scan.sh, ATTRIBUTION.md — für Phase 7.1, 11.7 | **UNGEPRÜFT** — Phase 7, 11.7 |
| **claude-media-bridge** (`_sources/local/claude-media-bridge`) | **ADAPT** | MCP-Anbindung, Ein-Klick-Gratis-Muster, Medien-Erzeugung — für Phase 2.3, 7.2 | **UNGEPRÜFT** — Phase 2, 7 |
| **airbeat-studio** (`_sources/github/airbeat-studio`) | **REFERENCE** (später **ADAPT**) | Steuersprache IMU bis 120 Hz — für Phase 11 (Musik-Steuerung) | **UNGEPRÜFT** — Phase 11 |
| **Genesis_Harness** (`_sources/github/Genesis_Harness`) | **REFERENCE** | Rollen-Register, anbieter-neutraler Modell-Zugriff mit Failover — Muster für Provider-Schicht | **UNGEPRÜFT** — Phase 2 |
| **droidroute** (`_sources/github/droidroute`) | **REFERENCE** (später evtl. **ADAPT**) | Ktor-Server, Provider-Schicht, Handbücher — bleibt eigenständig, Integration erst Phase 11 nach frischer Standserhebung | **UNGEPRÜFT** — Phase 11 |
| **PerpyBridge** (`_sources/local/perpybridge`) | **REFERENCE** | Kostenschutz, Null-Protokollierung — Muster für §3.6, §8 | **UNGEPRÜFT** — Phase 2, 8 |

---

## 2. Nur als Vorbild (aus SOURCE_INDEX.md §2.2)

| Quelle | Entscheidung | Begründung | Status |
|---|---|---|---|
| VELDRA / bolt-diy-android | **REFERENCE** | Belegen: WebView kann keinen Node-Unterbau → bestätigt nativen Weg | **UNGEPRÜFT** |
| aster-code | **REFERENCE** | UI-Ideen für Chat + Werkzeugkarten | **UNGEPRÜFT** |
| aureon-desk | **REFERENCE** | QA-Checklisten als Vorbild | **UNGEPRÜFT** |
| PocketCodeAgent | **REFERENCE** | Frühphase ähnliches Produkt — Fehler vermeiden | **UNGEPRÜFT** |
| flylab | **REFERENCE** | Android-Aufbau, andere Fachrichtung | **UNGEPRÜFT** |
| AI-Agent-OS | **REFERENCE** | Zustands- und Rollendokumente | **UNGEPRÜFT** |
| Linear | **REFERENCE** | Prüfpläne (Mess-, Geräte-, Bild-Prüfpläne) | **UNGEPRÜFT** |
| OMNI-* / Prompt_Library / skill-repo / EvoSphere / CryptoPilot-AI / aurelis / grid-proof-messenger | **REFERENCE**, einzeln prüfen | Können Bausteine liefern; Inhalte stichprobenartig lesen, nichts blind übernehmen | **UNGEPRÜFT** |
| claude-deskdroide | **REFERENCE** | Sehr früher Entwurf desselben Ziels | **UNGEPRÜFT** |

---

## 3. Nicht verwenden (aus SOURCE_INDEX.md §2.3)

| Quelle | Entscheidung | Grund |
|---|---|---|
| AI-Agents (privat) | **IGNORE** | Sammlung fremder Projekte → bleibt privat |
| Retrieval-based-Voice-Conversion-WebUI (privat) | **IGNORE** | Kopie fremden Projekts → bleibt privat |
| LiteLLM (privat) | **IGNORE** | Nur config.yaml, kein eigener Code |
| loki-workbench, claude-project-workbench, loki-health, -veldra-music-intelligence | **IGNORE** | Leer (0 Dateien) |
| cc-on-usb, exy, freebuffclaw-bridge, clawdroide | **IGNORE** | Leer |
| claude-screen-use | **IGNORE** | Inhaltsgleich mit clawscreen |
| claudroide-build, media | **IGNORE** | Je 1 Datei |
| claudroide-first-party/claudroide | **IGNORE** | Zweite Kopie des Haupt-Repos |

---

## 4. Fremde Quellen mit Lizenzprüfung (aus SOURCE_INDEX.md §3)

| Zweck | Quelle | Lizenz | Entscheidung | Status |
|---|---|---|---|---|
| Modelle auf Grafik/Prozessor | `github.com/ggml-org/llama.cpp` | MIT | **ADAPT** (Laufzeit in App) | **UNGEPRÜFT** — Phase 8 |
| NPU-Weg | `github.com/google-ai-edge/LiteRT` + MediaPipe | Apache-2.0 | **ADAPT** (Forschungsblock) | **UNGEPRÜFT** — Phase 9 |
| NPU-Weg (Hersteller) | `developer.samsung.com/neural/overview.html` | Samsung-Bedingungen | **REFERENCE** (Partnerfreigabe nötig) | **UNGEPRÜFT** — Phase 9 |
| Bildschirm-Übertragung (Notweg) | `github.com/Genymobile/scrcpy` | Apache-2.0 | **ADAPT** (ADB-Notweg §6.3) | **UNGEPRÜFT** — Phase 6 |
| Fähigkeiten-Sammlungen | `obra/superpowers`, `gradle/gradle-skills`, `affaan-m/ECC`, `ccplugins/awesome-claude-code-plugins`, `latent-spaces/brag`, `skydoves/compose-performance-skills`, `chrisbanes-*` | je prüfen | **REFERENCE** / **ADAPT** | **UNGEPRÜFT** — Phase 7, 10 |
| Design-Fähigkeiten (321 / 74 Repos) | über `design-skill-library` + `ATTRIBUTION.md` | je Ursprung prüfen | **ADAPT** (verkleinerter Katalog Phase 11.7) | **UNGEPRÜFT** — Phase 11 |

---

## 5. Nächste Schritte

1. **Task 0.2/0.3:** Je Quelle die **echten** Pfade eintragen (`_sources/local/…`, `_sources/github/…`).
2. **Task 0.2/0.6:** Je Baustein **KEEP/ADAPT/REWRITE/REFERENCE/IGNORE** endgültig festlegen — **nach Sichtung der Dateien**, nicht nach Vermutung.
3. Private Repos mit Token lesen; **nichts** davon ins öffentliche Repo.
4. Ergebnisse in `docs/DECISIONS.md` festhalten; Widersprüche zur Spec sofort korrigieren.
5. Spalte "Status" aktualisieren, sobald eine Quelle tatsächlich geprüft wurde.