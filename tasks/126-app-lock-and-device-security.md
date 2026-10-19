---
id: "126"
title: "App- und Geräteschutz"
wave: "W24"
depends_on: [017, 045]
files: [tasks/126-app-lock-and-device-security.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "1d55ffbf045deb73"
---
# Aufgabe 126 — App- und Geräteschutz

## Ziel
Sensible Anbieter- und Projektdaten gegen versehentliches Offenliegen schützen.

## Ergebnis
Bewertung von Gerätesperre, App-Wechselbildschirm, Zwischenablage und Benachrichtigungen.

## Fertig, wenn
- Optionale Schutzmaßnahmen verständlich und getestet sind.
- Keine biometrische Funktion ohne unterstützte Android-API versprochen wird.

## Schutz
Benachrichtigungen zeigen standardmäßig keine vertraulichen Inhalte.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Datenschutz-/Geräteschutz-Skill suchen; Quelle/Lizenz prüfen, Nutzer vor Installation fragen.
