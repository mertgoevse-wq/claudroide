# CLAUDE CODE TASK — integrate the Stitch handoff as a native Android UI

Work in `/home/mert/claudroide-next`.

The Stitch handoff documents should be under `design/stitch-handoff-2026-10-10/ANDROID_HANDOFF/` and the original screens under `design/stitch-handoff-2026-10-10/stitch_minimalist_android_chat_interface/` after extraction. If the actual paths differ, locate them with `find design -maxdepth 5 -type f`. Do not assume paths; locate the files first.

Read, in this order:
1. `design/stitch-handoff-2026-10-10/ANDROID_HANDOFF/01_STITCH_AUDIT.md`
2. `design/stitch-handoff-2026-10-10/ANDROID_HANDOFF/02_NATIVE_DESIGN_SPEC.md`
3. `design/stitch-handoff-2026-10-10/ANDROID_HANDOFF/03_MOTION_SPEC.md`
4. `design/stitch-handoff-2026-10-10/ANDROID_HANDOFF/screen_inventory.csv` and the original `design/stitch-handoff-2026-10-10/stitch_minimalist_android_chat_interface/` assets.

The native design spec overrides inconsistent details from the original Stitch `DESIGN.md` and individual HTML prototypes.

## Hard rules

- Preserve all existing user work, uncommitted files, branches and history. Start by recording `git status --short --branch` and do not reset, force-push, `git clean`, or overwrite unrelated files.
- Do not delete, move, or rewrite source repositories during this task.
- Do not ship the HTML prototypes inside a WebView. They use Tailwind/remote font assets and are only visual references. Implement native Android UI with the framework the existing repository actually uses. Inspect before assuming Compose or XML.
- Do not replace functional ViewModels, provider logic, streaming, routing, persistence, permissions, security guards, tool dispatch, or cancellation with mock UI.
- No fake backend results, placeholder API keys, fabricated progress, or credentials in source/screenshot assets.
- Preserve the clean build/test baseline if it is verified. Use one additional subagent maximum and one active shell process maximum.
- Do not stop after writing another plan. Implement the first useful UI slice, run checks, and continue to the next slice while context permits.

## Stage 0 — confirm the real baseline

1. `pwd`, branch, Git status, latest commits, and the current checkpoint.
2. Inspect the actual app UI framework, navigation, theme, screen composables/views, ViewModels, tests, and Gradle tasks.
3. Run the narrow existing tests/build checks appropriate for the baseline, or explain a real blocker.
4. Inspect the Stitch screen images directly. Use HTML as a reference for layout/content only; do not copy CDN scripts into Android.
5. Record the exact mapping from reference screens to existing UI code in `progress/STITCH-ANDROID-INTEGRATION.md`.

## Stage 1 — design tokens and core chat

Implement shared theme/token changes using the current architecture. Normalize UI typography, code typography, spacing, surfaces, icons, button dimensions, light/dark behavior, and reusable components according to `02_NATIVE_DESIGN_SPEC.md`.

Then integrate the highest-priority screens in this order:
1. New/empty chat and message composer.
2. Active conversation and true streamed assistant response.
3. Attachment bottom sheet and attachment-preview state.
4. Navigation drawer / recent conversation history.

Keep the default screen simple. Advanced tools and provider controls are contextual or in Expert Mode. Do not add all integrations to the main composer.

Connect UI actions to the real app state and current business logic. If a capability is missing, show an honest unavailable/setup-required state instead of a fake success.

## Stage 2 — progressive screen groups

After Stage 1 passes checks, continue in this order:
- Projects, files, and artifacts.
- Coding workspace, diff, terminal, agent task, and build result.
- Media generation/result/player and capability-aware provider selection.
- Provider settings, MCP, skills/plugins, and Expert Mode.

Do not attempt to implement all screens as one huge patch. Work in coherent slices, test each slice, and preserve buildability.

## Stage 3 — animations

Implement only motion that improves state clarity, following `03_MOTION_SPEC.md`. Use native APIs and current dependencies. Do not add decorative shimmer, fake progress, or a CSS/WebView animation layer.

Make streaming use actual backend chunks; tie stop/resume/complete states to the ViewModel. Animate expand/collapse, sheet appearance, and actual result arrival subtly. Respect accessibility and Android system animation settings.

## Verification after each slice

- Run focused unit/UI tests available in the repository.
- Run the relevant Gradle build task, and the complete required test suite at sensible checkpoints.
- Inspect failures and fix regressions before continuing.
- Verify no feature behavior or security policy has been lost.
- Build an installable debug APK once core integration is stable. Record the verified path, SHA-256, test count, and build result only if actually measured.

## Source-code reuse and storage

Other local repositories may be considered for reuse, but inspect each candidate and verify source provenance, license, actual implementation, dependencies, tests, and fit before porting anything. Do not delete any source repository or large cache during UI integration. Create a separate disk-usage/cleanup report and mark candidates only; actual removal must happen only after the replacement is integrated and tested, the Git remote/commits/uncommitted changes are secured, and the user can review the proposed deletion list.

## Continuation/checkpoint

Update `progress/STITCH-ANDROID-INTEGRATION.md` with completed code, tests/build results, changed files, open issues, and the next exact action. Keep it concise. If this Claude Code context is exhausted, the next fresh session must read this checkpoint and verify actual Git state instead of repeating finished work.

Start Stage 0 now and then implement Stage 1. Do not wait for more Stitch screens; the 26-screen export is enough to begin with the core chat.
