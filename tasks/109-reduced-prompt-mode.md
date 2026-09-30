---
id: "109"
title: "Weniger-Rückfragen-Modus"
wave: "W26"
depends_on: [017, 018, 105, 107, 108]
files: [tasks/109-reduced-prompt-mode.md]
skills: [`android-permissions-security`, `/code-review`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "6a83d21617663fe7"
---
# Aufgabe 109 — Weniger-Rückfragen-Modus

## Ziel
Einen jederzeit einstellbaren Modus anbieten, der sichere, begrenzte Aktionen weniger oft bestätigt.

## Ergebnis
Risikoerklärung, Projektbindung, Kennzeichnung, Ablauf/Widerruf und erlaubte Aktionstypen.

## Fertig, wenn
- Nutzer ihn selbst einschaltet und später ausschalten kann.
- Löschen, Installieren, Git-Push und externe Dienste nach Sicherheitsentscheidung behandelt werden.

## Schutz
Keine Option darf Android-Rechte oder verbotene Anbieteranmeldungen umgehen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Agent-Sicherheits-/Permission-Skill suchen, Quelle/Lizenz prüfen und Nutzer vor Installation fragen.
