# ClauDroide — refined native Android UI specification

This document overrides inconsistent visual details in the original Stitch `DESIGN.md` and generated HTML. Use it as the implementation source of truth. Preserve the existing app architecture and working behavior.

## Product experience

ClauDroide is a conversation-first AI workspace, not a dashboard. Default Mode is an intentionally calm chat. Projects, Coding, and generated outputs are easy to reach; advanced providers, MCP, routing, tool logs, and diagnostics appear only in context or Expert Mode.

Prioritize: (1) new chat and message composer, (2) active conversation and real streaming, (3) navigation drawer/history, (4) projects/files/artifacts, (5) coding/agent workflow, (6) media, (7) integrations and Expert Mode.

## Typography

Use one local, platform-appropriate sans-serif for all UI. In a native Android screen, prefer the app's existing Material 3 typography and Android's default Roboto/sans-serif unless the repository already bundles and consistently uses a licensed Inter font. Never fetch a font from the network at runtime. No serif headings. Do not mix Inter/Roboto/serif per screen.

Use a system monospace font for code and terminal output unless a licensed monospace font is already bundled locally. UI labels are not code and must never use monospace just to look technical.

Recommended baseline on a phone, expressed in `sp` and line height:
- Screen title: 20sp / 28sp, medium (avoid 32sp display headings for ordinary app screens).
- Section title: 16sp / 24sp, medium.
- Body / chat response: 16sp / 24sp, normal.
- Secondary/body small: 14sp / 20sp.
- Metadata: 12sp / 16sp, use sparingly; avoid smaller text for important content.
- Code: 13sp / 19sp or larger where readable; horizontally scroll only when appropriate.

Respect Android font scaling and long localized strings. Avoid truncating user content or model output just to fit a mockup.

## Color and surfaces

Use neutral surfaces and one muted warm accent. Do not use color to decorate each card.

Suggested light tokens:
- `background`: `#FAFAF9`
- `surface`: `#FFFFFF`
- `surfaceSubtle`: `#F4F4F2`
- `borderSubtle`: `#E5E5E2`
- `textPrimary`: `#1C1C1A`
- `textSecondary`: `#666663`
- `accent`: `#A64B2D` (primary actions/selected state only)
- `accentSubtle`: `#F8ECE7`
- `error`: a standard accessible error color

Suggested dark tokens:
- `background`: `#171717`
- `surface`: `#202020`
- `surfaceSubtle`: `#262626`
- `borderSubtle`: `#393939`
- `textPrimary`: `#F2F2EF`
- `textSecondary`: `#B5B5B0`
- `accent`: `#E19A7C`

Check contrast with actual rendering; these are starting tokens, not a substitute for accessibility testing. Support app light/dark mode and follow the existing theme architecture.

## Shape, spacing, and hierarchy

- Use 4 dp spacing increments, with 8 dp as the common base.
- Use 16 dp screen gutters on phones, respecting system insets.
- Use 12–16 dp corner radius for composer, sheets, and occasional content panels; not everything should be pill-shaped.
- Icon-only controls: 44–48 dp interactive bounds; all main touch targets should aim for at least 48 dp.
- Primary/secondary buttons: one primary action per section; 48 dp minimum height where full buttons are used.
- Prefer clear text hierarchy and whitespace over nested backgrounds and borders.
- Do not wrap assistant messages in cards by default. Render conversational content directly on the screen.
- Use bordered/background panels only when they express a distinct object: composer, code block, attachment, artifact preview, confirmation sheet, or active tool run.
- Use dividers or spacing to group settings lists. No card inside another card unless there is a demonstrable interaction need.

## Core chat

- Top app bar: navigation button, current conversation/model context where appropriate, and one contextual action (e.g. new chat). Avoid redundant title pills plus additional title labels.
- Main content: direct readable conversation stream. Responses support Markdown, code, links, and artifacts without nested response cards.
- Composer: fixed above system navigation/IME using native insets; multiline input; single add/attachment button; send/stop action; voice action only when supported. Model/mode selector stays compact and optional.
- When typing, keyboard visibility must not hide the input. Support IME action and content scrolling correctly.
- Stream state comes from the actual ViewModel. Stop cancels the real stream; completion/error/cancel must reset state deterministically.
- Show real connection/retry states. Do not put every tool/feature permanently into the composer.

## Navigation

Keep the top-level hierarchy small: Chat, Projects, Coding. Recent chats, media results, tools and settings are reachable through the drawer or contextual entry points. Do not promote each provider, skill, media format, or MCP server to top-level navigation.

Use native back behavior for hierarchical screens. Use a close button only for temporary dialogs/sheets. Make the distinction consistent.

## Projects, artifacts and coding

- Projects: compact readable rows, recent activity, files, instructions and related chats.
- Files: icon + filename + secondary metadata; show the first few items and expand if long. Avoid repeating the same file info in cards.
- Artifact: content first, with one compact action bar. Use real copy/share/save/export operations only when implemented.
- Code review: keep diff viewport large; collapse optional reviewer explanation and logs. Use line numbers and a stable local monospace style.
- Agent task: show current step first. Completed steps collapse into one compact summary; active step remains visible. Tool arguments/logs expand on demand.
- Terminal: monospace text in one bounded region, not a large nested card stack. Preserve text selection/copy.
- Build: report real Gradle/test status and actual artifact path. No fake success state.

## Media and providers

Media UI is contextual. Show result prominently. Provider choice should filter to providers with the requested capability. A text-only route must not be presented as an image/video generator. Jobs use real backend states; percentages appear only when the backend reports meaningful progress. Partial image preview appears only if actual partial data is available.

MCP, skills, providers and plugins are different concepts; represent their real supported capabilities and authorization status separately. Never embed actual credentials in UI source, screenshot assets, test fixtures, or default config.

## Native implementation requirements

- Inspect whether this repository uses Jetpack Compose, XML Views, or a mixture. Follow the actual architecture; do not migrate frameworks to make Stitch HTML easy to reuse.
- If Compose is already used, map tokens to the current `MaterialTheme`, reusable composables, and existing navigation. If the app uses Views, use existing view/theme patterns instead.
- The HTML prototypes are reference material only. Do not load the screens through WebView and do not add Tailwind or remote font/CDN dependencies to the APK.
- Use Android system status/navigation bars, IME insets, BackHandler/navigation behavior, lifecycle-aware state, and accessibility semantics.
- Keep UI separate from provider/chat/business logic; preserve existing ViewModels, streaming, model routing, persistence, security gates and cancellation behavior.
- Avoid new dependencies unless the repository demonstrates the need and the build/test effect is justified.

## Acceptance criteria

1. The new-chat screen looks calm at 360–430 dp widths and at larger font scale.
2. Text is readable; no serif outliers, random fonts, truncated prompts, or tiny important labels.
3. The composer remains above the keyboard and uses the actual chat state.
4. The default chat shows only essential controls; advanced controls remain contextual.
5. Screens share the same typography, icons, spacing, colors, and button hierarchy.
6. No nested card walls or 3+ layers of decorative containers.
7. Animations reflect actual UI state and respect system motion settings.
8. Unit tests and build checks pass; no existing provider or security behavior regresses.
