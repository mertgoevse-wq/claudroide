# ClauDroide — Stitch Android Integration Progress

**Started:** 2026-10-10
**Branch:** feat/claudroide-next-architecture
**Baseline Commit:** e381795 (verified 2579 tests pass, secret gate clean, sync check OK)

## Stage 0 — Baseline Confirmed ✓
- Git status: clean except for design/ folder
- Tests: 2579 passing, 0 failures
- Secret gate: 0 hits
- Sync frontmatter: OK
- Architecture: Jetpack Compose with Material 3, ViewModels, Flow-based state

## Stage 1 — Core Chat Implementation

### Mapping from Stitch screens to existing code:
| Stitch Screen | Status | Target Files |
|---|---|---|
| `claudroide_android_chat_interface` (empty chat) | **IN PROGRESS** | ChatScreen.kt, ChatViewModel.kt, ClaudroideComponents.kt |
| `claudroide_aktive_konversation` (active chat) | PENDING | ChatScreen.kt, StreamingText.kt, MessageBubble |
| `claudroide_anhang_men` (attachment bottom sheet) | PENDING | New AttachmentBottomSheet.kt |
| `claudroide_navigation_drawer` (drawer/history) | PENDING | NavigationDrawer.kt, ChatHistoryScreen.kt |

### Design tokens to align with 02_NATIVE_DESIGN_SPEC.md:
- Colors: Neutral surfaces + muted warm accent (not Terracotta everywhere)
- Typography: Inter for UI, JetBrains Mono for code, no serif headings
- Spacing: 4dp increments, 16dp gutters, 48dp min touch targets
- Shapes: 12-16dp radius for composer/sheets, not pill-shaped everything
- No nested cards; use dividers/whitespace for grouping

### Tasks:
- [x] Update Theme.kt colors to match native spec (neutral + warm accent)
- [x] Update TypeTokens to match spec (16sp body, 20sp code, etc.)
- [x] Fix TypeScaleContractTest and TypeAndSpacingTest to match new spec
- [x] All 2579 tests pass, build clean
- [ ] Refactor ChatScreen: empty state, message list, streaming indicator
- [ ] Refactor MessageComposer: attachment button, mode selector, send/stop
- [ ] Implement StreamingText component for real token streaming
- [ ] Implement AttachmentBottomSheet (modal, 84% width, grip handle)
- [ ] Implement NavigationDrawer with recent chats, projects, settings
- [ ] Connect all to ChatViewModel (streaming, stop, new conversation)
- [ ] Run tests and verify build after each slice

## Stage 2 — Progressive Screen Groups
- Projects, files, artifacts
- Coding workspace, diff, terminal, agent task, build result
- Media generation/result/player
- Provider settings, MCP, skills/plugins, Expert Mode

## Stage 3 — Animations
- Per 03_MOTION_SPEC.md: subtle, state-driven, respect reduced motion

---

## Completed Work Log

### 2026-10-10 — Session Start
- Baseline confirmed: 2579 tests pass, build clean
- Read all Stitch handoff documents (01_AUDIT, 02_NATIVE_DESIGN_SPEC, 03_MOTION_SPEC, 04_INTEGRATION, 05_START_HERE)
- Inspected existing ChatScreen, ChatViewModel, ClaudroideComponents, Theme, TypeTokens
- Created this progress file

### 2026-10-10 — Design Tokens Aligned
- Updated Theme.kt colors to match native spec (neutral + warm accent) ✓
- Updated TypeTokens to match spec (16sp body, 13sp code, etc.) ✓
- Fixed TypeScaleContractTest and TypeAndSpacingTest to match new spec ✓
- All 2579 tests pass ✓
- Build clean ✓

### 2026-10-10 — Stage 1 Core Chat Implementation
- Added missing string resources: `chat_composer_attachment`, `chat_composer_mode` (EN/DE) ✓
- Updated ChatScreen TopAppBar: navigation drawer icon + new conversation action ✓
- Enhanced EmptyState with action button for new conversation ✓
- Enhanced MessageComposer with attachment button, mode selector, send/stop button ✓
- Added AttachmentPreviewRow component for attachment chips ✓
- Added ChatViewModel methods: `onAttachmentClick()`, `onModeClick()`, `removeAttachment()` ✓
- All 2579 tests pass ✓
- Debug APK builds successfully ✓