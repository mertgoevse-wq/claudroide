---
id: "037"
title: "Code und Antworten anzeigen"
wave: "W8"
depends_on: [016, 025]
files: [app/src/main/java/org/claudroide/app/feature/chat/CodeBlockRenderer.kt, app/src/test/java/org/claudroide/app/CodeBlockRendererTest.kt, tasks/037-code-message-display.md]
skills: [`adaptive`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "b166a60e2adba44a"
---
# Aufgabe 037 — Code und Antworten anzeigen

## Ziel
Codeblöcke und längere Antworten auf Smartphonebreite lesbar darstellen.

## Ergebnis
Formatierung, horizontales Scrollen wo nötig, Codekopieren und lange Inhalte einklappen.

## Fertig, wenn
- Code unverändert kopiert werden kann (`CodeBlockPolicy.extractExactCodeForClipboard` garantiert exakten Erhalt von Einrückung und Zeichen).
- Formatierungsfehler die Originalantwort nicht zerstören (`MarkdownMessageParser` fängt unvollständige Code-Fences fehlertolerant ab).
- Lange Codeblöcke (> 25 Zeilen) initial eingeklappt dargestellt werden (`isCollapsible`).

## Schutz
Angezeigter Code wird nicht automatisch ausgeführt (`CodeBlockPolicy.IS_AUTO_EXECUTION_PERMITTED = false`).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/chat/CodeBlockRenderer.kt`:
  - `sealed interface MessageContentSegment`: Trennung von `TextSegment` und `CodeSegment`.
  - `object CodeBlockPolicy`: Sicherheitsregel gegen automatische Codeausführung, Schwellenwert für Einklappen (25 Zeilen) und verlustfreies Extrahieren für die Zwischenablage.
  - `object MarkdownMessageParser`: Robuster Parser für Fenced Code-Blocks mit Toleranz gegenüber offenen Fences.
- `app/src/test/java/org/claudroide/app/CodeBlockRendererTest.kt`:
  - Unit-Tests für Sicherheitsinvariante, verlustfreie Zwischenablage, Parsing gemischter Markdown-Inhalte, unvollständige Codeblöcke und Einklapp-Schwellenwert.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `adaptive`: Codeblöcke mit horizontalem Scrollen und mobilen Einklapp-Optionen gestaltet.
- `/code-review`: Sicherheitsregel gegen automatische Ausführung und verlustfreie Clipboard-Extraktion verifiziert.

