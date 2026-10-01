# Claudroide

<p align="center">
  <img src="assets/claudroide-mascot-logo.jpg" alt="Claudroide Mascot" width="160" style="border-radius: 28px; box-shadow: 0 10px 30px rgba(0,0,0,0.35);" />
  <br>
  <b>Claudroide</b> — Autonomous Mobile AI Coding Assistant for Android
  <br>
  <sub>Native Touch Interface · BYOK (Bring Your Own Key) · Samsung Galaxy A56 5G Tuned · Private GitHub Sync</sub>
</p>

<p align="center">
  <b>🇺🇸 English</b> · <a href="README.de.md">🇩🇪 Deutsch</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Phase-Design%20%26%20Engineering-informational" alt="Phase" />
  <img src="https://img.shields.io/badge/Tasks-135%20Total-blue" alt="Tasks" />
  <img src="https://img.shields.io/badge/App--Code-46%20%2F%20135%20done-yellow" alt="Status" />
  <img src="https://img.shields.io/badge/Waves%200--10-completed-success" alt="W0-W10" />
  <img src="https://img.shields.io/badge/Target-Galaxy%20A56%205G-orange" alt="Device" />
  <img src="https://img.shields.io/badge/Repo-private-success" alt="Repo" />
</p>

<p align="center">
  <img src="assets/claudroide-banner.jpg" alt="Claudroide Header Banner" width="100%" style="border-radius: 16px; margin: 16px 0;" />
</p>

Claudroide is an independent native Android application for AI-assisted software engineering and project management (inspired by the Claude Code CLI paradigm, but with an independent brand, clean-room architecture, and mobile-native Compose UI). Primary hardware benchmark: Samsung Galaxy A56 5G. This repository contains the complete specification, 135 machine-readable task briefs, and the deterministic autonomous engineering loop.

> **Independence Notice:** Claudroide is an independent open-source project and is not commercially or officially affiliated with Anthropic, PBC. Claude is a registered trademark of Anthropic. Claudroide interacts exclusively via official developer APIs with user-provided API keys (BYOK).

**Navigation:** [Quickstart](#quickstart-in-claude-code) · [Engineering Loop](#how-the-autonomous-loop-works) · [Progress Status](#progress-status) · [Assets & Mascot](#assets-and-visual-identity) · [Security Guardrails](#security-and-product-guardrails) · [Documentation Index](#complete-documentation-index)

---

## Quickstart in Claude Code

```bash
cd /home/mert/claudroide   # Navigate to project root
claude                      # Launch Claude Code (CLAUDE.md loads automatically)
```

Within Claude Code REPL:

| Command / Prompt | Action |
|---|---|
| `/claudroide-resume` | Inspects latest checkpoint in `progress/BUILD-STATE.md` against filesystem reality and resumes engineering seamlessly. |
| `Baue weiter bis zum finalen Produkt` | Starts the autonomous engineering loop from `CLAUDE.md`: select earliest pending task with fulfilled dependencies → implement → test → checkpoint → commit & push. Pauses only on safety gates or blockers. |
| `Baue Aufgabe 017` | Implements a single specific task in isolation. |

Prerequisites: `git` and verified private remote `mertgoevse-wq/claudroide`. The six globally installed skills (`swarm-planner`, `parallel-task`, `adaptive`, `android-profiler`, `android-permissions-security`, `testing-setup`) are verified and loaded per task.

<details>
<summary><b>Resuming Work After an Interruption or Crash</b></summary>

1. Restart device / Termux environment and enter `/home/mert/claudroide`.
2. Launch `claude`.
3. Type `/claudroide-resume`.

The skill cross-examines the checkpoint, working tree, and Git commit log without speculating. Every finished task is verified, committed, and synced to remote.
</details>

---

## How the Autonomous Loop Works

```mermaid
flowchart LR
    A[Select Task<br/>status: pending] --> B{depends_on<br/>all done?}
    B -- No --> A
    B -- Yes --> C[Load Required Skills<br/>Safety Gate Check]
    C --> D[Implement Code & Architecture<br/>Execute Verification Tests]
    D --> E[Update Task State<br/>sync_frontmatter.py]
    E --> F[Update Checkpoint + Commit + Push]
    F --> A
```

- **Waves Instead of Chaos:** `tasks/DEPENDENCIES.md` segments all 135 tasks into dependency waves W0–W29. Non-overlapping tasks can run concurrently via `/parallel-task`.
- **Machine-Readable Task Definitions:** Every task in `tasks/` carries strict YAML frontmatter (`status`, `depends_on`, `skills`, `wave`, `gate`). The single source of truth is maintained by [`tools/sync_frontmatter.py`](tools/sync_frontmatter.py).
- **Resilient Checkpoints:** `progress/BUILD-STATE.md` records verified IDs, test evidence, modified files, and clean Git commits after every task.

<details>
<summary><b>Repository Maintenance Commands</b></summary>

```bash
python3 tools/sync_frontmatter.py --check            # Verify consistency across 135 tasks (enforced in CI)
python3 tools/sync_frontmatter.py                    # Heal discrepancies and hashes
python3 tools/sync_frontmatter.py --status 025=done  # Mark task 025 as completed
```

Marking `--status ID=done` requires the task specification to remain untouched (`done_since_last_edit: true`); any subsequent modification immediately triggers review.
</details>

---

## Progress Status

Verified counts synced directly from YAML frontmatter:

| Domain | Tasks | Status |
|---|---|---|
| W0–W2 · Foundations, Feasibility, Hardware Profile | 001–008 | 🟢 8 / 8 Completed (W0–W2 Closed) |
| W3–W6 · Core Architecture, Build Pipelines, Design Tokens | 009–024 | 🟢 16 / 16 Completed (W3–W6 Closed) |
| W7–W9b · Compose UI & Interactive Chat | 025–042 | 🟡 In Progress (15 / 18, W7–W9 closed) |
| W10–W14 · Model Providers, Routing & Privacy | 043–069 | 🟡 In Progress (7 / 27, Task 052 done) |
| W15–W19b · Agent Execution & Project Context (SAF) | 070–094 | ⬜ Pending |
| W20–W23 · Git Integration & Sandbox Execution | 095–105 | ⬜ Pending |
| W24–W26 · Security Engine, Approvals & Token Vault | 106–122 | ⬜ Pending |
| W27–W29 · Dynamic Skills, External Tooling & Full E2E | 123–135 | ⬜ Pending |

<details>
<summary><b>Complete Wave Architecture (W0–W29)</b></summary>

| Wave | Tasks | Dependencies |
|---|---|---|
| W0 Bootstrap | 001, 002, 003, 006 | — |
| W1 Feasibility | 004, 005, 009, 019 | 001, 002, 003 |
| W2 Hardware Benchmarks | 007, 008 | 006 (+002) |
| W3 Application Base | 010 | 009, 019 |
| W4 Build Pipelines | 011, 012 | 008 |
| W5 Setup & Lifecycle | 013–018 | 010 |
| W6 Design System | 020–024 | 003, 019 |
| W7 UI Foundation | 025–030 | 010, 023, 024 |
| W8 Chat Engine | 031–035, 037, 042 | 025, 016 |
| W9 Execution Runtime | 038, 039 | 031, 035 |
| W9b SSE Streaming | 036 | 031, 035, 047, 052, 059 |
| W10 Provider Foundations | 043–045, 060 | 005, 016, 017 |
| W11 Provider Hardening | 046, 047, 052, 055–059 | 044, 045 |
| W12 Concrete Providers | 048–051, 053, 054 | 004, 005, 047, 052, 059 |
| W13 Context Management | 061, 062, 065, 067 | 043, 055 |
| W13b Model Fallbacks | 063 | 062, 064, 065 |
| W13c Token Cost Tracking | 041, 064 | 043, 055 (+061/+002) |
| W14 Offline Resilience | 066, 068, 069 | 039, 042, 062, 067 |
| W15 Agent Foundation | 070, 071 | 048–054 (+001) |
| W16 Agent Dispatcher | 040, 072–076, 078–080 | 070, 071 |
| W17 Parallel Orchestration | 077 | 073, 078, 090, 099 |
| W18 Storage Access Framework (SAF) | 081, 082, 084, 087, 088, 092 | 010, 017 |
| W18b Project Instructions | 093 | 003, 122 |
| W19 Access Enforcement | 083, 085, 086, 089, 091, 094 | 082 etc. |
| W19b File Staging & Diffing | 090 | 088, 089, 119, 120 |
| W20 Git Core | 095, 098, 103 | 017, 045, 059 |
| W21 Git Staging & Trees | 096, 097, 099, 100, 102, 104 | 095 etc. |
| W22 Secure Push & Sync | 101 | 095–100 |
| W23 Execution Sandboxing | 105 | 007, 008, 010, 017 |
| W24 Security Kernel | 117–122, 125–127 | 017, 045 |
| W25 Command Validation | 106, 107, 110–112, 114 | 105, 119, 120 |
| W26 Approval Engine & Foreground Service | 108, 109, 113, 115, 116 | 017, 018, 105, 107 |
| W27 Dynamic Skills Engine | 123, 124, 129–131 | 001, 002, 017 |
| W28 MCP & External Integrations | 132–135 | 070, 117, 119, 122 |
| W29 End-to-End Suite | 128 | Comprehensive validation |
</details>

---

## Assets and Visual Identity

Visual assets (logo, adaptive app icon, state illustrations, responsive banners) are crafted via the user's **Claude Media Bridge** following strict brand separation guidelines:

```mermaid
flowchart LR
    A[Asset Specification<br/>assets/logo-brief.md] --> B[Claude Media Bridge<br/>Nano Banana / Flux Engine]
    B --> C{Inspection:<br/>Autonomous, Zero Mark<br/>Confusion?}
    C -- Yes --> D[assets/ · PNG, WebP or JPG<br/>Strictly No SVG Placeholders]
    C -- No --> B
    D --> E[README & Tasks Reference<br/>Verified Binary Asset]
```

- **Mascot Logo:** [`assets/claudroide-mascot-logo.jpg`](assets/claudroide-mascot-logo.jpg) (Friendly green Android robot with glowing terracotta AI spark).
- **Header Banner:** [`assets/claudroide-banner.jpg`](assets/claudroide-banner.jpg) (Green Android bot playfully nibbling a glowing terracotta spark star).
- Binary integrity enforced: all graphics are real PNG, WebP, or JPG files under 50 KB ceiling; no SVG placeholders permitted in `assets/`.

---

## Security and Product Guardrails

- **Distinct Brand Identity:** Clean-room naming and visual identity; zero trademark confusion with Anthropic, Claude, or third-party mascots.
- **Strict BYOK (Bring Your Own Key):** Zero web-scraping, zero session sharing, zero unauthorized proxying. Only official developer endpoints with user-held credentials.
- **Hardware-Backed Encryption:** Tokens stored securely in Android Keystore / EncryptedSharedPreferences; secret scanning enforced prior to commit/push.
- **Data Minimization:** No telemetry leakage, strictly scoped Storage Access Framework (SAF) trees, no unnecessary `MANAGE_EXTERNAL_STORAGE` requests.

---

## Complete Documentation Index

| Document | Purpose |
|---|---|
| [`claudroide-spec.md`](claudroide-spec.md) | Product specification, architecture guidelines, test criteria, and all 135 tasks |
| [`README.de.md`](README.de.md) | Deutsche Version der Dokumentation |
| [`tasks/`](tasks/) | 135 individual task specifications with structured YAML metadata |
| [`tasks/DEPENDENCIES.md`](tasks/DEPENDENCIES.md) | Graph dependencies across waves W0–W29 |
| [`tasks/skill-matrix.md`](tasks/skill-matrix.md) | Two skills assigned per task, provenance, and audit status |
| [`CLAUDE.md`](CLAUDE.md) | Autonomous engineering guidelines, media rules, and Git discipline |
| [`tools/sync_frontmatter.py`](tools/sync_frontmatter.py) | Synchronization engine for task frontmatter and verification |
| [`progress/BUILD-STATE.md`](progress/BUILD-STATE.md) | Resilient verified checkpoint ledger |
| [`.claude/skills/claudroide-resume/SKILL.md`](.claude/skills/claudroide-resume/SKILL.md) | Interruption recovery skill |
| [`assets/logo-brief.md`](assets/logo-brief.md) | Visual asset specifications and generation briefs |
| [`.github/workflows/repo-health.yml`](.github/workflows/repo-health.yml) | GitHub Actions CI verifying task consistency and asset integrity |
| [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) | Android APK compilation workflow on GitHub Actions |
