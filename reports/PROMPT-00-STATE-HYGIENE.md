# PROMPT-00 — Agent State Hygiene Inspection Report

**Repo:** mertgoevse-wq/claudroide
**Branch:** main
**Date:** 2026-10-06
**Mode:** INSPECT-ONLY (no mutations, no deletions, no archives)
**Inspector:** Claude Code — Agent State Hygiene Architect

---

## 1. Active Session Count and Total Size

| Source | Count | Total Size |
|---|---|---|
| `.claude/session-env/` | 135 directories | ~465 KB |
| `.claude/sessions/` | 1 active session file | 7.5 KB |
| `.claude/session-data/` | 1 file | 136 KB |
| `.claude/projects/-home-mert-claudroide/` | 31 dirs + jsonl files | 189 MB |
| `.claude/history.jsonl` | 1 file | 184 KB |
| `.claude/.credentials.json` | 1 file | 1.7 KB |

**Active sessions:** 1 current session (`17696.json`). 30 dated session snapshots under `session-data/` from 2026-09-30 to 2026-10-06.

---

## 2. Archived Session Size

No explicit archive directory exists. Old session data lives inline within `.claude/projects/-home-mert-claudroide/` as UUID-named `*.jsonl` files. Oldest entries are from 2026-09-30.

**Estimated archived size:** ~120 MB across UUID-named `.jsonl` files in `projects/-home-mert-claudroide/`.

---

## 3. Largest Active Sessions

| Path | Size | Date |
|---|---|---|
| `projects/-home-mert-claudroide/7f4036e7-.../` | 8.0 MB | recent |
| `projects/-home-mert-claudroide/959285e6-.../` | 7.8 MB | recent |
| `projects/-home-mert-claudroide/c63f8faf-.../` | 4.9 MB | recent |
| `projects/-home-mert-claudroide/c9209353-.../` | 3.9 MB | recent |
| `projects/-home-mert-claudroide/2ef30f2b-.../` | 3.6 MB | Oct 06 |
| `projects/-home-mert-claudroide/0463ec51-...jsonl` | 5.7 MB | Oct 04 |
| `projects/-home-mert-claudroide/05580b8d-...jsonl` | 1.3 MB | Oct 04 |
| `projects/-home-mert-claudroide/0ab41ff1-...jsonl` | 5.7 MB | Oct 02 |

All paths are pseudonymous UUIDs. The `.jsonl` files are session transcripts; the UUID directories are session-state caches.

---

## 4. Metadata Bloat Summary

### security_warnings_state files
- **Count:** 126 JSON files + lock files in `.claude/security/`
- **Date range:** 2026-09-30 to 2026-10-06
- **Typical size:** 50–500 bytes each (small per-file)
- **Accompanying `.jsonl` transcripts:** 15 large files (100 KB — 5.7 MB each)
- **Bloat verdict:** Low per-file bloat, but 126 UUID-named state files are candidates for archiving. The `.jsonl` transcripts are the real size drivers.

### Plugin cache files
- `plugin-catalog-cache.json`: 536 KB — acceptable
- `plugin-directory-cache-v2.json`: 2.4 MB — acceptable
- `.claude/plugins/cache/`: 339 MB — **largest plugin subdirectory**

---

## 5. Stale Worktree Candidates

- `.claude/worktrees/` — **does not exist** (no git worktrees created)
- No stale worktree candidates found.

---

## 6. Log Size and Rotation Candidates

| File | Size | Date | Verdict |
|---|---|---|---|
| `.claude/security/log.txt` | 787 KB | Oct 06 14:00 | Current; rotate if > 1 MB |
| `.claude/security/log.txt.1` | 1.0 MB | Oct 02 22:30 | Already rotated; candidate for archive |
| `.claude/cost-tracker.log` | 1.5 MB | Oct 06 13:56 | Large; candidate for rotation |
| `.claude/bash-commands.log` | 1.4 MB | Oct 06 13:56 | Large; candidate for rotation |

**Total log size:** ~4.7 MB
**Rotation threshold recommendation:** 2 MB per file.

---

## 7. Path Anomaly Counts

- **Symlinks (expected):** 7 in `.claude/skills/` pointing to `../../.agents/skills/`
  - android-source-search, android-testing, compose, gradle-build-performance, modularization, requesting-code-review, security-and-hardening
- **Absolute path references in config:** `~/.claude/settings.json` contains `/tmp/skillscan/skills` and `/tmp/skillscan/android-review` in `extraKnownMarketplaces`. These reference temporary scan directories — may be stale if `/tmp` was cleared.
- **No Windows extended paths detected.**
- **No dead symlinks detected.**

---

## 8. Dead Config Prune Candidates

| Config Key | File | Verdict |
|---|---|---|
| `GATEGUARD_EXEMPT_GLOBS` | `~/.claude/settings.json` | Active; excludes repo paths |
| `enabledPlugins` | `~/.claude/settings.json` | 27 plugins; verify usage |
| `extraKnownMarketplaces` | `~/.claude/settings.json` | 8 entries; `/tmp/skillscan/` may be stale |
| `GATEGUARD_EXEMPT_GLOBS` | `claudroide/.claude/settings.json` | Minimal; only tasks/**, app/src/**, progress/** |

**`extraKnownMarketplaces` temp paths:** `/tmp/skillscan/skills` and `/tmp/skillscan/android-review` reference temporary scan directories. **Verify before prune.**

---

## 9. Top Heavy Dev Processes

No unusual dev processes detected beyond standard Android/Gradle/JVM activity.

---

## 10. Repository-Level State

| Item | Size | Verdict |
|---|---|---|
| `.git/` | 18 MB | Normal for 30+ session repo |
| `app/build/` | 181 MB | **Large; candidate for `./gradlew clean`** |
| `.gradle/` | 16 MB | Normal Gradle cache |
| `.kotlin/` | 63 KB | Normal |
| `.build/` | 3.5 KB | Normal |
| `build/` (root) | 143 KB | Normal |

---

## 11. Global Claude State Summary

| Directory | Size | Primary Content |
|---|---|---|
| `~/.claude/security/` | **288 MB** | Session transcripts + 126 warning-state files |
| `~/.claude/plugins/` | **583 MB** | Plugin cache (339 MB) + marketplace (242 MB) |
| `~/.claude/projects/` | **305 MB** | Per-project session state (claudroide: 189 MB) |
| `~/.claude/skills/` | 4.5 MB | Global skill symlinks |
| `~/.claude/file-history/` | 7.9 MB | File edit history |
| `~/.claude/session-env/` | 493 KB | 135 session environment dirs |
| **`~/.claude/` (total)** | **~1.3 GB** | All of the above |

**Three dominant bloat candidates:**
1. `.claude/security/` (288 MB) — old session transcripts + 126 UUID state files
2. `.claude/plugins/` (583 MB) — mostly `cache/` (339 MB) and `marketplaces/` (242 MB)
3. `.claude/projects/-home-mert-claudroide/` (189 MB) — 30+ session transcripts as `.jsonl` files

---

## 12. Credential Exposure Risk

| File | Location | Risk | Action |
|---|---|---|---|
| `.credentials.json` | `~/.claude/` | **High** — contains auth credentials | **Never archive without encryption; user must approve** |
| `.claude/settings.json` | `~/.claude/` | Low — config only | Safe to archive |
| `skills-lock.json` | repo root | Low — skill hashes | Safe to archive |

**.credentials.json is treated as a private local artifact. No backup, archive, or copy without explicit user consent.**

---

*Report generated in inspect-only mode. No files were written, moved, or deleted.*
