---
id: "011"
title: "Bauweg direkt am A56"
wave: "W4"
depends_on: [008]
files: [tasks/011-phone-build-path.md]
skills: [`android-profiler`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "968cc4864869fe3f"
---
# Aufgabe 011 — Bauweg direkt am A56

## Ziel
Machbarkeit einer lokalen Android-Bauumgebung ohne separate Termux-App untersuchen.

## Ergebnis
Kleiner Proof-of-concept oder begründeter Machbarkeitsbericht zu benötigten Werkzeugen, Speicher, Wärme, Zeit und Berechtigungen.

## Fertig, wenn
- Ein reproduzierbarer Build oder ein klarer Grund für Unmöglichkeit dokumentiert ist.
- Kein Installationspaket ohne Prüfsumme/Herkunft bezogen wird.

## Schutz
Keine versteckten Systemrechte und keine unbestätigten großen Downloads.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-On-device-build-Skill suchen; Installationswege und Lizenz prüfen, Nutzerfreigabe vor Installation einholen.
