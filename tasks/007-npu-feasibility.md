---
id: "007"
title: "NPU-Machbarkeit"
wave: "W2"
depends_on: [002, 006]
files: [tasks/007-npu-feasibility.md]
skills: [`android-profiler`, `testing-setup`]
status: pending
gate: false
done_since_last_edit: false
content-hash: "f343b5ef1b9ba3c6"
---
# Aufgabe 007 — NPU-Machbarkeit

## Ziel
Ermitteln, ob und über welchen unterstützten Weg konkrete lokale KI-Aufgaben auf NPU oder GPU laufen können.

## Ergebnis
Reproduzierbarer Gerätestest für genaues Modell und Android-Version, mit Ergebnis CPU/GPU/NPU, Qualität, Geschwindigkeit, Akku, Wärme und Speicherbedarf.

## Fertig, wenn
- NPU-Nutzung nur behauptet wird, wenn sie messbar belegt ist.
- Ein klarer CPU/GPU-Rückfall und ein „nicht unterstützt“-Ergebnis existieren.

## Schutz
Keine inoffiziellen Treiber, Systemänderungen oder Root-Rechte voraussetzen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-On-Device-KI-Skill suchen, Quellcode/Lizenz und Geräteanforderungen prüfen; Installation Nutzer-bestätigt.
