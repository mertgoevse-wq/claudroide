---
id: "059"
title: "Netzwerkschutz"
wave: "W11"
depends_on: [044, 045, 052]
files: [tasks/059-network-security.md]
skills: [`android-permissions-security`, `testing-setup`]
status: pending
gate: true
done_since_last_edit: false
content-hash: "cf5b0b7fd1471c39"
---
# Aufgabe 059 — Netzwerkschutz

## Ziel
Anbieteranfragen verschlüsselt und nur an bestätigte Ziele schicken.

## Ergebnis
HTTPS-Regeln, Zertifikatsfehler, Umleitungen und Endpunktwechsel sicher behandeln.

## Fertig, wenn
- Zertifikatsfehler nicht still ignoriert werden.
- Jede Weiterleitung auf andere Adresse prüfbar und sichtbar ist.

## Schutz
Kein Schlüsselversand über unverschlüsselte Verbindung ohne sehr bewusste, dokumentierte Ausnahme.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Netzwerksicherheits-Skill suchen, Quelle/Lizenz prüfen und Nutzer vor Installation fragen.
