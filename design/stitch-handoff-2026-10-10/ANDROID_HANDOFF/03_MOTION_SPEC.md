# ClauDroide — native motion and animation spec

Motion should make state changes understandable, not entertain the user. Avoid web-style hover effects, endless shimmer, bounce, glitter, parallax, or animation on every element.

## Timing and behavior

- Tap/pressed feedback: immediate tonal/ripple feedback; optional 0.98 scale only for prominent controls, returning without bounce.
- Small state/icon transitions: about 120–180 ms.
- Screen content transitions: about 160–220 ms, subtle fade plus 4–8 dp translation where appropriate.
- Bottom sheet/dialog: follow Material 3 motion and native sheet behavior; keep dismissal predictable.
- Expandable sections: use `animateContentSize` or native equivalent, typically around 160–220 ms. Don't animate entire long lists if it causes scroll jumps.
- Alternate content/preview: use a restrained `Crossfade` around 160–200 ms when content actually changes.
- Long tasks: show a spinner or indeterminate status while progress is unknown. Use determinate progress only with a meaningful backend-provided value.

## Chat and streaming

- Text must appear from the real response stream, not from a fake typewriter animation after the response is already known.
- Keep the stop button tied to actual cancellation. Transition it to the normal send action after the streaming state really ends.
- Auto-scroll only while the user is already at or near the bottom. If the user scrolls upward to read, don't pull them back down; offer a restrained "new output" affordance instead.
- Keep list keys stable to prevent message jumps while streaming.
- Streaming Markdown must not reflow the entire conversation unnecessarily. Prefer incremental updates and stable layout where possible.

## Tool and task status

- A tool invocation may appear with a subtle visibility transition when a real tool starts.
- Expand/collapse logs and completed steps with state-driven motion.
- Show a successful check only after the operation reports success. Error and cancellation states should not be delayed by decorative animation.
- Never animate a fake percent from 0 to 100. If no backend progress exists, use an indeterminate indicator with text such as "Verarbeitung läuft".

## Image, audio and video

- Crossfade the placeholder to the actual image only after image data is available.
- Show pixel-by-pixel or progressive preview only when the service returns real partial data.
- Audio play/pause and seek indicators respond to actual player state and position. No decorative waveform should pretend to represent the active track when no waveform exists.
- Video job states reflect actual provider status. Avoid guessed durations, credits, queue positions, or percentages.

## Android implementation

- Prefer native motion APIs already used by the project. In Jetpack Compose, consider `AnimatedVisibility`, `animateContentSize`, `animate*AsState`, `updateTransition`, and `Crossfade` where appropriate; do not add another animation framework just for the mockups.
- Follow the existing Material 3 theme and screen transitions. Avoid CSS animation conversion.
- Respect system animation scale / reduced-motion expectations. Test with Android animator duration scale disabled and with normal settings. Motion must never be required to understand current state.
- Avoid animations that compete with typing, code reading, accessibility focus, text selection, or long-running terminal output.

Reference: https://developer.android.com/develop/ui/compose/quick-guides/content/video/animation-in-compose
