# ClauDroide

<p align="center">
  <img src="assets/brand/1791295622623.jpg" alt="ClauDroide — Android bot and Claude bot collaborating under starry sky" width="100%" style="border-radius: 14px; box-shadow: 0 8px 24px rgba(0,0,0,0.4);" />
</p>

<p align="center">
  <a href="assets/brand/1791468217945.jpg">
    <img src="assets/brand/1791468217945.jpg" alt="ClauDroide Official Mascot & App Icon" width="108" style="border-radius: 24px; border: 2px solid #E07A5F;" />
  </a>
  <br>
  <b>ClauDroide</b> — Autonomous Mobile AI Coding Assistant for Android
  <br>
  <sub>Native Material 3 Touch Interface · BYOK Privacy Vault · OmniRoute &amp; Segmind Media Bridge · Google Stitch Architecture · Samsung Galaxy A56 Tuned</sub>
</p>

<p align="center">
  <b>🇺🇸 English</b> · <a href="README.de.md">🇩🇪 Deutsch</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%2014%2B%20%2F%20API%2034-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Tasks-135%20%2F%20135%20Done-2EA043?style=flat-square&logo=checkmarx&logoColor=white" alt="Tasks" />
  <img src="https://img.shields.io/badge/Tests-2612%20passing-2EA043?style=flat-square" alt="Tests" />
  <img src="https://img.shields.io/badge/Gateway-OmniRoute%20%3A20128-E07A5F?style=flat-square" alt="OmniRoute" />
  <img src="https://img.shields.io/badge/Design-Google%20Stitch-8AB4F8?style=flat-square" alt="Google Stitch" />
  <img src="https://img.shields.io/badge/Device-Galaxy%20A56%205G-orange?style=flat-square" alt="A56" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square" alt="License" />
</p>

---

## ⚡ Key Highlights & Capabilities

- 🤖 **Developer-Grade Native Mobile Studio:** A full-featured native Android interface inspired by Claude Code — not merely a terminal view. Includes real-time token telemetry, syntax-highlighted code diff cards, interactive tool execution approvals, and multi-file tree browsing.
- 🎨 **Google Stitch High-Fidelity Design System:** Built directly from the verified Stitch design system (`Claudroide Android AI Workspace` on Google AI Pro) with deep slate-charcoal canvas (`#0E1116`), warm terracotta accents (`#E07A5F`), edge-to-edge Android system bars, and 48dp touch envelopes.
- 🌉 **OmniRoute &amp; Multi-Provider Media Bridge:** Native bridge to OmniRoute (`127.0.0.1:20128`), Segmind (`flux-schnell`, `sdxl1.0`, `sd3.5`), Nano Banana 2, and keyless Pollinations fallback for autonomous media asset generation directly on device.
- 🔒 **Hardware-Backed BYOK Security:** Zero web-scraping, zero unauthorized relays. Keys encrypted via Android Keystore (StrongBox / TEE AES-256-GCM) with strict Storage Access Framework (SAF) scoped isolation.

---

## 📱 Interactive Workspace Navigation

```mermaid
graph TD
    AppShell[ClauDroide Native AppShell] --> NavDrawer[Navigation Drawer]
    NavDrawer --> ViewChat["💬 Chat &amp; Composer<br/>Streaming, Tool Approvals"]
    NavDrawer --> ViewMedia["🎨 Media &amp; Bild-Studio<br/>OmniRoute, Segmind, FLUX, Aspect Ratios"]
    NavDrawer --> ViewCodeMcp["⚡ Code &amp; MCP Studio<br/>Tool Registry, Telemetry"]
    NavDrawer --> ViewArtifacts["📦 Artifacts Gallery<br/>Rendered Assets, Diff Viewer, Reports"]
    NavDrawer --> ViewProjects["📁 Project Explorer<br/>SAF Folders, Git Status, Staged Commits"]
    NavDrawer --> ViewSettings["⚙️ Settings &amp; Vault<br/>BYOK Keys, Models, Language, Keystore"]
```

---

<details open>
<summary><b>🚀 Quickstart &amp; Local A56 Workflow</b></summary>

### 1. Launch in Termux / PRoot on Galaxy A56
```bash
cd /home/mert/claudroide-next
cdx status      # Check toolchain, memory, and repo state
cdx check       # Verify fast unit tests and secret scanning
cdx test fast   # Run targeted fast test suites
cdx build debug # Build debug APK with native aapt2 aarch64
```

### 2. Autonomous Session Continuation
```bash
claude          # Launch Claude Code
/claudroide-resume  # Automatically inspects and resumes the verified checkpoint
```
</details>

<details>
<summary><b>🌉 OmniRoute &amp; Segmind Media Generation Bridge</b></summary>

ClauDroide interfaces seamlessly with local and remote AI models through OmniRoute on port `20128` and dedicated media providers:

| Provider | Supported Models | Capabilities | Status |
|---|---|---|---|
| **OmniRoute** (`localhost:20128`) | `segmind/flux-schnell`, `pollinations/flux`, `agy/gemini-3.1-flash-image`, `sdxl1.0` | Text-to-Image, Multi-Model Routing, Tool Use | 🟢 Active |
| **Segmind Direct** | `flux-schnell`, `flux-dev`, `sd3.5-large`, `wan2.1-t2v` | Ultra-fast image &amp; video generation | 🟢 Supported |
| **Pollinations.ai** | `flux`, `turbo` | Zero-config, keyless fallback tier with 100% uptime | 🟢 Configured |
| **Google Antigravity** | `gemini-3.1-flash-image`, `imagen-3.0` | High-fidelity image generation via Google AI Pro | 🟢 Connected |

Generated assets are saved into `app_data/generated_media/` and immediately viewable in the **Artifacts Gallery**.
</details>

<details>
<summary><b>📊 135/135 Task Verification Status</b></summary>

Verified counts synced directly from YAML frontmatter:

| Domain | Tasks | Status |
|---|---|---|
| W0–W2 · Foundations, Feasibility, Hardware Profile | 001–008 | 🟢 8 / 8 Completed (W0–W2 Closed) |
| W3–W6 · Core Architecture, Build Pipelines, Design Tokens | 009–024 | 🟢 16 / 16 Completed (W3–W6 Closed) |
| W7–W9b · Compose UI &amp; Interactive Chat | 025–042 | 🟢 18 / 18 Completed (W7–W9b Closed) |
| W10–W14 · Model Providers, Routing &amp; Privacy | 043–069 | 🟢 27 / 27 Completed (W10–W14 Closed) |
| W15–W19b · Agent Execution &amp; Project Context (SAF) | 070–094 | 🟢 25 / 25 Completed (W15–W19b Closed) |
| W20–W23 · Git Integration &amp; Sandbox Execution | 095–105 | 🟢 11 / 11 Completed (W20–W23 Closed) |
| W24–W26 · Security Engine, Approvals &amp; Token Vault | 106–122 | 🟢 17 / 17 Completed (W24–W26 Closed) |
| W27–W29 · Dynamic Skills, External Tooling &amp; Full E2E | 123–135 | 🟢 13 / 13 Completed (W27–W29 Closed) |

All 2,612 Unit Tests passing (`:app:testDebugUnitTest`). Release APK verified with Scheme v2 signature (`29616268...`).
</details>

---

## 🛡️ Security &amp; Legal Guardrails

- **Distinct Brand Identity:** Clean-room naming and visual identity; zero trademark confusion with Anthropic, Claude, or third-party mascots.
- **Strict BYOK (Bring Your Own Key):** Zero web-scraping, zero session sharing, zero unauthorized proxying. Only official developer endpoints with user-held credentials.
- **Hardware-Backed Encryption:** Tokens stored securely in Android Keystore / EncryptedSharedPreferences; secret scanning enforced prior to commit/push.
- **Data Minimization:** No telemetry leakage, strictly scoped Storage Access Framework (SAF) trees, no unnecessary `MANAGE_EXTERNAL_STORAGE` requests.

---

## 📑 Documentation Index

| Document | Purpose |
|---|---|
| [`claudroide-spec.md`](claudroide-spec.md) | Product specification, architecture guidelines, test criteria, and all 135 tasks |
| [`README.de.md`](README.de.md) | Deutsche Version der Dokumentation |
| [`reports/RELEASE_READINESS_AUDIT.md`](reports/RELEASE_READINESS_AUDIT.md) | Release readiness audit and formal verification report |
| [`reports/FEATURE_TRACEABILITY_MATRIX.csv`](reports/FEATURE_TRACEABILITY_MATRIX.csv) | Full 135-task traceability matrix with hardware and UI evidence |
| [`reports/CRASH_AND_REGRESSION_AUDIT.md`](reports/CRASH_AND_REGRESSION_AUDIT.md) | Root-cause analysis and mutation tests for 5 addressed crash risks |
| [`tasks/skill-matrix.md`](tasks/skill-matrix.md) | Two skills assigned per task, provenance, and audit status |
| [`CLAUDE.md`](CLAUDE.md) | Autonomous engineering guidelines, media rules, and Git discipline |
| [`progress/BUILD-STATE.md`](progress/BUILD-STATE.md) | Resilient verified checkpoint ledger |
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

## Interface & Native Mobile Experience

<p align="center">
  <img src="assets/brand/claudroide-app-showcase.jpg" alt="ClauDroide Mobile UI Showcase on Samsung Galaxy A56" width="100%" style="border-radius: 12px;" />
</p>

ClauDroide delivers a native touch-first developer experience built with Jetpack Compose & Material 3:
- **Interactive Chat & Terminal:** Real-time streaming assistant, syntax-highlighted code diff blocks, local shell tool output, and cancellation controls.
- **Project Workspaces (SAF):** Storage Access Framework integration with scoped directory trees, persistent URI permissions, and zero root requirements.
- **BYOK Privacy Vault:** Hardware-backed API key storage with Android Keystore encryption for Claude API, OpenRouter, and custom endpoints.
- **Autonomous Subagent & Safety Gates:** Built-in verification gates for Git commits, pushes, and destructive operations with clear manual approval.

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

- **Mark:** [`assets/brand/mark-1024.png`](assets/brand/mark-1024.png), with `-256` and `-96` variants. Also generated straight into the app as the adaptive launcher icon (`app/src/main/res/mipmap-*/ic_launcher_foreground.png`, plus a `monochrome` layer for Android 13+ themed icons).
- **Banner:** [`assets/brand/banner.png`](assets/brand/banner.png) — 1376x768, 16:9. The wordmark is real Inter, so the product name is spelled correctly by construction.
- **In the app:** both ship as WebP derived from those PNG sources — `res/drawable-nodpi/claudroide_mark.webp` (31 KB) and `claudroide_banner.webp` (27 KB, down from 114 KB). The PNGs stay in the repo as the lossless source; the 50 KB cap applies to what goes on the device, and `BrandAssetContractTest` enforces it per shipped file.
- **Design:** an Android dome in `#3DDC84` wearing a Claude-grammar burst as its crown. The two rays at ±26° are the droid's antennae **and** two rays of the burst — the same strokes serve both systems, so neither half can be deleted without breaking the other. That is the "no subordinate symbol" requirement expressed as geometry rather than as a caption.
- Drawn as exact vector geometry by [`tools/brand.py`](tools/brand.py), not by an image model: the wordmark has to spell "ClauDroide", and diffusion models misspell wordmarks. The banner and the launcher icon import the same geometry, so they cannot drift apart.
- Binary integrity enforced by CI: no SVG permitted in `assets/` (`.github/workflows/repo-health.yml`). The SVGs under `assets/brand/.build/` are build input, not shipped assets. Source files under `assets/brand/` are uncapped; the per-image 50 KB cap applies to shipped app resources and is test-enforced.

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
