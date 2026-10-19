# ClauDroide Stitch → native Android handoff

## Was in diesem Paket liegt

- `stitch_minimalist_android_chat_interface/`: die Stitch-Screenshots, 26 einzelne `code.html`-Prototypen, Referenz-Screenshots und das ursprüngliche `DESIGN.md`. Nur ein Beispiel für einen teilweise maskierten API-Key wurde im Paket in einen eindeutig redigierten Platzhalter umgewandelt; die ursprünglich hochgeladene ZIP-Datei bleibt separat unverändert.
- `stitch_minimalist_android_chat_interface/claudroide_studio/DESIGN_NATIVE_REFINED.md`: verfeinerte Designregeln, die Inkonsistenzen des ursprünglichen Exports korrigieren.
- `stitch_screen_contact_sheet.png`: Übersicht der 26 erzeugten UI-Screens.
- `01_STITCH_AUDIT.md`: konkrete Bestandsaufnahme des tatsächlichen Exports.
- `02_NATIVE_DESIGN_SPEC.md`: verbindliche visuelle und technische Spezifikation für die Android-Implementierung.
- `03_MOTION_SPEC.md`: dezente Motion-/Animationsregeln für eine echte App.
- `04_CLAUDE_CODE_INTEGRATION_PROMPT.md`: vollständiger Implementierungsauftrag.
- `05_START_HERE_PROMPT.md`: kurzer Einstiegsprompt für Claude Code.
- `screen_inventory.csv`: Screen-Namensliste mit empfohlenem Umsetzungsabschnitt.

## Ganz wichtig

Die `code.html`-Dateien sind eigenständige Web-Prototypen. Sie verwenden Tailwind über CDN und in mehreren Screens externe Google Fonts / Material Symbols. **Sie sind keine native Android-UI und sollen nicht unverändert in ein WebView eingebaut werden.** Sie dienen als visuelle Referenz. Claude Code soll die Vorlagen in der bestehenden Android-Architektur umsetzen, bevorzugt mit den bereits im Repository verwendeten nativen Komponenten.

## Einbau in das Projekt

Empfohlenes Ziel im Repo: `design/stitch-handoff-2026-10-10/` (neuer, eigener Ordner, damit keine frühere Arbeit überschrieben wird). Danach `design/stitch-handoff-2026-10-10/ANDROID_HANDOFF/05_START_HERE_PROMPT.md` in Claude Code lesen lassen und den dort genannten Auftrag ausführen.
