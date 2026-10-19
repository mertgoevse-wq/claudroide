---
id: "121"
title: "Links und Sonderdateien"
wave: "W24"
depends_on: [017, 045, 082]
files: [tasks/121-symlink-and-special-file-defense.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "aa58582f5b8a52e6"
---
# Aufgabe 121 — Links und Sonderdateien

## Ziel
Verhindern, dass Links oder spezielle Dateien Zugriff außerhalb des Projekts ermöglichen.

## Ergebnis
Erkennungs- und Behandlungsregeln für Links, Gerätedateien und ungewöhnliche Dateitypen.

## Fertig, wenn
- Unsichere Ziele nicht gelesen oder überschrieben werden.
- Git- und Android-Dateianbietergrenzen getestet sind.

## Schutz
Keine Linkverfolgung ohne erneute Begrenzungsprüfung.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Filesystem-Security-Skill suchen; Quelle/Lizenz und bekannte Risiken prüfen, Nutzer vor Installation fragen.
