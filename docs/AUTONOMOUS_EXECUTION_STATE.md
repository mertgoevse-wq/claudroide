# ClauDroide — Autonomous Execution State

## Current Project State
- **Project path:** `/home/mert/claudroide-next`
- **Canonical Product:** ClauDroide (Unified Codebase)
- **Branch:** `feat/claudroide-next-architecture`
- **Git remote:** `next` → `https://github.com/mertgoevse-wq/claudroide-next.git`
- **Last safe Git reference:** `a0624af`
- **Current sprint:** Codebase unification, SAF project bug root-cause fix, Back-Navigation hierarchy, Control-Audit, and Dark-Mode Scrim contrast repair.

## Phase 0 — Root Cause Analysis & Diagnostic Verification
1. **Device & Environment:** Samsung Galaxy A56 5G (SM-A566B) running Debian PRoot in Termux, Linux 6.17.0.
2. **Back-Navigation & Launcher Exit at ~18.3s:**
   - **Root Cause:** Absence of Compose `BackHandler` in `AppShell` (`MainActivity.kt`). When the user pressed back while the drawer, bottom sheet, or sub-screens (Projects/Settings) were open, the event fell through unhandled to `ComponentActivity.onBackPressedDispatcher`, causing Android to navigate directly to the launcher or background the app.
   - **Resolution:** Implemented hierarchical `BackHandler` priority chain:
     1. `showAttachmentSheet` → dismisses `AttachmentBottomSheet`.
     2. `showModelDialog` → dismisses model selection dialog.
     3. `drawerState.isOpen` → closes `ModalNavigationDrawer`.
     4. `currentDestination != CHAT` → navigates back to Home (CHAT).
     5. Root screen → standard safe system back.
     SAF folder picker abort/dismissal explicitly handled via `onPickerCancelled()` without closing the Activity.
3. **SAF Project Bug ("Boom" Folder Selection Not Displayed):**
   - **Root Cause:** In `ProjectScreen.kt`, `persistentAccess` was an immutable data class instantiated once in `MainActivity.onCreate()` and remembered in Compose without `State` or `StateFlow` observation. When the user granted folder access, `ProjectSaver` called `persist` on an immutable instance without persisting the grant to disk (SharedPreferences/DataStore) or triggering a recomposition. On startup, `PersistentFolderAccess` re-initialized empty.
   - **Resolution:** Created `ProjectStorage` with `SharedPreferences` persistence (`claudroide_project_storage`), `StateFlow<ProjectFolderUiState>`, `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree())`, resilient cancellation, Scoped Storage root limitation warnings, and persistent grant reconciliation on launch.
4. **Dark Mode Scrim Contrast Bug:**
   - **Root Cause:** In `Theme.kt`, `scrim` was assigned `c.textPrimary`, which evaluates to `#F2F0EA` (light cream/white) in dark mode, causing `ModalBottomSheet` to cover the dark interface with an aggressive light-grey milky haze.
   - **Resolution:** Set `scrim = Color(0xFF000000)` in `Theme.kt` and explicit `scrimColor = Color.Black.copy(alpha = 0.6f)` on `ModalBottomSheet`.
5. **Control Audit & Dead Handlers:**
   - Audited all 34 interactive controls in `docs/CONTROL_AUDIT.md`. Replaced empty `{}` stubs (model selection pill, voice mode button, profile icon, drawer targets) with active dialogs and real intent launchers.

## Phase 1 — Build & Artifact Status
- **Build task:** `./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx1536m" :app:assembleDebug` → **BUILD SUCCESSFUL**
- **Test task:** `./gradlew --no-daemon -Dorg.gradle.jvmargs="-Xmx1536m" :app:testDebugUnitTest --rerun-tasks` → **BUILD SUCCESSFUL**, **2,608 Tests in 139 Suiten, 0 Fehler, 0 übersprungen**
- **Generated APK:** `app/build/outputs/apk/debug/app-debug.apk` (23 MB, 23,094,444 Bytes)
- **Delivered APK:** `/sdcard/Download/ClauDroide-debug.apk` + `.sha256`
- **APK Checksum (SHA-256):** `7549de90dd9ab9f2b848c038352e1aa40d6a17956c9b2752c58854c912586c33`
- **Package ID:** `org.claudroide.next.debug` (VersionCode: 1, VersionName: 0.1.0)
- **Security scan:** `python3 tools/secret_gate.py .` → 0 Treffer (exit 0)
- **Task consistency:** `python3 tools/sync_frontmatter.py --check` → OK (alle 135 Task-Dateien konsistent)
- **Whitespace / Git check:** `git diff --check` → OK (exit 0)

## Phase 2 — Deliverables & Matrices
- `docs/INTEGRATION_MAP.md` — 11-Bereiche-Vergleich zwischen Next und Quellprojekt `claudroide`.
- `docs/CONTROL_AUDIT.md` — 34 auditierte Controls mit Handlern, Aktionen und Tests.
