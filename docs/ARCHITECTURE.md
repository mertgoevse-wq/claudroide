# ARCHITECTUR — Produktarchitektur (aus Spec §12 ausgearbeitet)

**Stand:** 2026-10-09 · **Grundlage:** `claudroide-spec.md` §12, §3–§9 · **Status:** Task 0.1 — aus Spec abgeleitet

---

## 1. Übersicht

Die Architektur folgt dem Prinzip **vertikaler Scheiben**: Jeder Funktionsbereich (Chat, Coding-Agent, Device Agent, etc.) ist als eigenständiger vertikaler Schnitt implementiert, der alle Schichten (UI, Domain, Data) durchdringt. Dies ermöglicht inkrementelle Lieferung und isoliertes Testen.

**Quellbasis:** Codeumzug aus `~/claudroide` liefert `core/` (design, i18n, security, platform, navigation, diagnostics) + `feature/` (chat, project, settings, onboarding, provider, agent, skills, mcp, git, linux, control).

---

## 2. Bausteine (Zielbild)

| # | Baustein | Verantwortung | Quelle / Anmerkung |
|---|---|---|---|
| 1 | **Android UI (Compose, MVVM)** | Screens, Components, Navigation, Theming | alt: `feature/*`, `core/design` |
| 2 | **Chat Engine** | Multi-Modus-Chat (DISCUSS/BUILD), SSE-Streaming, Anhänge | `feature/chat`, `lokicode` Chat-Domain |
| 3 | **Agent Runtime** | Job-Manager, Semaphore (1 Shell, 1 Subagent), Autonomie-Loop, Plan/Execute/Verify | `feature/agent`, `claude-code-android` autonomous.sh |
| 4 | **Context Manager** | Token-Budgeting, Kompression, Rotation, Disk-Persistenz, Wiederaufnahme | §5; Bausteine aus `lokicode` Context Engine |
| 5 | **Model Router** | Rollen-Aliase (`role:fast`, `role:coder`, `role:vision`, `role:summary`), Failover, Health-Checks | §3.3–3.4; `lokicode` ModelSelectionManager |
| 6 | **Provider Abstraction** | Wire-Format-basiert (OpenAI-kompatibel, Anthropic-kompatibel, Gemini), Custom Endpoints als Konfiguration | §3.1; `lokicode` Provider-Register |
| 7 | **BYOK / Secrets** | Android Keystore, SecretMasker, Redaction im Protokoll | §3.2, §8; `feature/provider/SecureKeyStore.kt` |
| 8 | **Files / Projects** | Projekt-Explorer, Datei-Browser, Code-View, Diff, Search | `feature/project` erweitern |
| 9 | **Git** | Diffs, Commits, Log, Konflikte, GitHub-Sync (privat, AUS per Default) | `feature/git` |
| 10 | **Shell / Process Runtime** | Leichte Shell + PRoot-Container (portiert aus `feature/linux`) | §5.3; `claude-code-android` Engine-Technik |
| 11 | **Skills / Plugins / MCP / Prompts** | Kataloge, Progressive Disclosure, Awesome-Prompts-Pipeline | §7; `design-skill-library` Muster |
| 12 | **Session Manager** | Session-/Task-State, Wiederaufnahme (`WEITERARBEITEN`), Checkpoints | `claudroide-resume` Skill, `lokicode` Memory |
| 13 | **Device Agent** | AccessibilityService, Gesten, Screenshot, See→Decide→Act→Verify, Not-Aus, Sperrliste | §6; `clawscreen` Verfahren, `~/claudroide/feature/control` |
| 14 | **Media Runtime** | Bilder, Audio, MIDI, Video, Dokumente verstehen/erzeugen; MCP-Anbindung | §1.1; `claude-media-bridge` (SPÄTER) |
| 15 | **Persistence** | Room/DataStore + Disk-Snapshots (Sicherungen, Rückgängig-Stapel) | §5.2; `lokicode` Offline-Speicher |
| 16 | **Security** | Secret-Scan, Redaction, App-Sandbox, Sperrliste, Netzwerk-Policy | §8; `android-permissions-security` Skill |
| 17 | **Logging / Observability** | Audit-Trail, Redaction, Live-Protokoll im UI | `claude-code-android` Haken |
| 18 | **Permissions** | Android-Runtime-Permissions-Flows (Bedienungshilfe, Bildschirmaufnahme, Mikrofon, Benachrichtigungen) | Interview 3, Runde 4 |
| 19 | **Testing** | Unit + Gerät + Screenshot-Gate (§11) | `testing-setup` Skill, `android-review` |
| 20 | **Update / Migration** | App-Upgrade-Pfade, Datenmigrationen, Bau im Internet (§5.4) | `claude-code-android` Release-Pattern |

---

## 3. Schichten-Architektur (pro vertikaler Schnitt)

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer (Compose)                                          │
│  ├─ Screens                                                  │
│  ├─ Components (Design-Token aus core/design)                │
│  └─ Navigation (3 Bereiche + ruhiger 4. Punkt Device Agent)  │
├─────────────────────────────────────────────────────────────┤
│  Domain Layer (Use Cases, Business Logic)                    │
│  ├─ Chat Use Cases                                           │
│  ├─ Agent Use Cases (Plan, Execute, Verify)                  │
│  ├─ Provider Use Cases (Routing, Failover, Cost)             │
│  ├─ Device Use Cases (See, Decide, Act, Verify)              │
│  ├─ Context Use Cases (Compress, Rotate, Persist)            │
│  ├─ Project/Git Use Cases                                    │
│  ├─ Skills/MCP Use Cases                                     │
│  └─ Media Use Cases                                          │
├─────────────────────────────────────────────────────────────┤
│  Data Layer (Repositories, Data Sources)                     │
│  ├─ Provider Repositories (Wire-Format Adapters)             │
│  ├─ Keystore Repository (Secrets)                            │
│  ├─ Project/File Repository (Room + FileSystem)              │
│  ├─ Git Repository                                           │
│  ├─ Context/Snapshot Repository (Disk)                       │
│  ├─ Skills/Plugins Registry                                  │
│  ├─ Device Agent Repository (Accessibility, ADB)             │
│  └─ Media Repository                                         │
├─────────────────────────────────────────────────────────────┤
│  Platform Layer (Android Framework, PRoot, ADB, Keystore)    │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Kern-Datenflüsse

### 4.1 Chat-Fluss (DISCUSS-Modus)
```
User Input → Chat Screen → Chat Use Case → Model Router (role:fast)
    → Provider Abstraction → Wire Adapter → Provider API
    → Response → SSE Stream → Chat Screen → Persist (Room)
```

### 4.2 Coding-Agent-Fluss (BUILD-Modus)
```
User Task → Agent Screen → Agent Planner → Task Steps
    → Job Manager (Semaphore: 1 Shell, 1 Subagent)
    → Executor (File Ops, Shell, Diff, Test/Build)
    → Verifier (Test/Build Result, Screenshot)
    → Result → Live Log → Persist State → Checkpoint
```

### 4.3 Device Agent-Fluss
```
User Goal → Device Screen → See (Accessibility + Screenshot)
    → Decide (Model Router role:vision → Provider)
    → Act (Gesten via Accessibility / ADB Notweg)
    → Verify (Screenshot + Element-Liste)
    → Result → Live Log → Persist State
```

### 4.4 Context Management
```
Agent Step → Context Manager (Token Count)
    → Wenn > Warnschwelle: Kompression (Summarize, Drop Tool Outputs)
    → Wenn > Critical: Rotation (Snapshot to Disk → Fresh Context → Reload)
    → State: Task, Plan, Progress, Decisions, Blockers, Last Files
```

---

## 5. Modul-Grenzen & Abhängigkeiten

| Modul | Darf abhängen von | Darf NICHT abhängen von |
|---|---|---|
| UI | Domain, Design-Token | Data, Platform direkt |
| Domain | — | UI, Data (nur Interfaces), Platform |
| Data | Domain (Interfaces), Platform | UI, andere Data-Module |
| Platform | — | Alles oben |

**Regel:** Dependency Inversion — Domain definiert Interfaces, Data implementiert sie.

---

## 6. Wiederverwendung aus Quellen (Entscheidungen)

| Baustein | Quelle | Entscheidung | Begründung |
|---|---|---|---|
| Design-Token | `~/claudroide/core/design` | **KEEP** (byte-identisch) | Marken-Konsistenz §2.2 |
| Provider-Schicht | `lokicode/domain/provider` | **ADAPT** | Saubere Wire-Format-Abstraktion, Failover bewährt |
| Model Router | `lokicode/domain/router` | **ADAPT** | Rollen-Aliase, Health-Checks implementiert |
| Agent Runtime | `claude-code-android/tools/autonomous.sh` | **ADAPT** | Job-Manager, Semaphore, Loop-Engineering |
| Context Engine | `lokicode/domain/context` | **ADAPT** | Kompression, Rotation, Persistenz bewährt |
| Device Agent | `clawscreen` + `~/claudroide/feature/control` | **ADAPT** | ADB-Verfahren, Umlaute, Not-Aus gemessen |
| Skills-Katalog | `design-skill-library` | **ADAPT** | Progressive Disclosure, Anti-Slop-Tor |
| Build/Tools | `claude-code-android` | **ADAPT** | Secret-Scan, Haken, Gradle-Best-Practices |
| Container/PRoot | `mobile-linux-lab` + `~/claudroide/feature/linux` | **ENTSCHEIDUNG OFFEN** (Task 0.6) | Vergleich nötig |

---

## 7. Offene Architektur-Entscheidungen (für Task 0.6, Phase 1)

1. **Container-/Terminal-Ansatz:** `claudroide/feature/linux` (83 Dateien, PRoot/AVF/Native/QEMU/Remote) vs. `mobile-linux-lab` (eigene Geräteprüfung, BackendRegistry, PRootBackend) — Entscheidung in `docs/DECISIONS.md`
2. **Shell-Implementierung:** Termux-Pakete direkt vs. eigene PRoot-Instanz — am Gerät klären (Unsicherheit §15.2)
3. **Screenshot-API:** `takeScreenshot` (API 30+) vs. MediaProjection auf A56 — am Gerät verifizieren (Unsicherheit §15.3)
4. **NPU-Integration:** LiteRT-Samsung-Backend vs. Samsung Neural SDK — Forschungsblock Phase 9

---

## 8. Nicht-Funktionale Anforderungen

| Kategorie | Anforderung |
|---|---|
| **Performance** | Builds auf A56 (2,2 GB freier RAM) → nur 1 Build gleichzeitig; große Builds via Bau im Internet (§5.4) |
| **Speicher** | Keine doppelten Build-Artefakte; Sicherungen pausieren bei vollem Speicher |
| **Sicherheit** | Keine Secrets in Git/Logs; Redaction-Test; Sperrliste enforcement; 127.0.0.1 only für eigene Türen |
| **Zuverlässigkeit** | Blocker-Regel (2 Versuche → weiter); unbegrenztes Rückgängig; Arbeit geht bei Rotation nicht verloren |
| **Testbarkeit** | Unit-Tests + Gerät-Stichproben + Screenshot-Gate; absichtlich kaputte Tests als Regressionstests |
| **Wartbarkeit** | Anti-Slop (§18); Code-Hygiene-Lauf nach Meilensteinen (§28.11); Dokumentation §10 |

---

## 9. Querbezüge

- **Requirements:** `docs/REQUIREMENTS.md` (Akzeptanzkriterien je Baustein)
- **Roadmap:** `docs/ROADMAP.md` (Phasen & Wellen)
- **Tasks:** `docs/TASKS.md` (Implementierungsreihenfolge)
- **Security:** `docs/SECURITY.md` (Sicherheitsarchitektur)
- **Decisions:** `docs/DECISIONS.md` (Architektur-Entscheidungen)
- **Source Index:** `docs/SOURCE_INDEX.md` (Herkunft je Baustein)
- **Reuse Matrix:** `docs/SOURCE_REUSE_MATRIX.md` (KEEP/ADAPT/REWRITE/REFERENCE/IGNORE)