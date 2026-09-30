---
id: "007"
title: "NPU-Machbarkeit"
wave: "W2"
depends_on: [002, 006]
files: [tasks/007-npu-feasibility.md]
skills: [`android-profiler`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "8d600a1129a4a303"
---
# Aufgabe 007 — NPU-Machbarkeit

## Ziel
Ermitteln, ob und über welchen unterstützten Weg konkrete lokale KI-Aufgaben auf NPU oder GPU laufen können, und ein ehrliches, reproduzierbares Mess- und Rückfallkonzept verankern.

## Ergebnis
Reproduzierbarer Gerätestest und Machbarkeitsbericht für das Galaxy A56 5G (Android 15 / Exynos 1580):

### 1. Analyse der Beschleuniger-Schnittstellen (Stand: 2026-09-30)

| Beschleuniger-Pfad | Android-Schnittstelle / API | Machbarkeit auf A56 5G | Bewertung & Konsequenz |
|---|---|---|---|
| **NPU (Neural Processing Unit)** | Standard Android NNAPI (seit Android 15 abgekündigt) / Proprietäre Samsung NDK-Treiber | **Nicht verifiziert / Keine öffentliche Standard-API** | **Ehrliches Urteil: Keine NPU-Nutzung versprechen!** Auf Standard-Android existiert ohne OEM-Partnerschaft keine stabile Schnittstelle für eigene LLMs. Bleibt experimentell zurückgestellt. |
| **GPU (Graphics Processing Unit)** | Vulkan API / OpenCL (Samsung Xclipse 540 / AMD RDNA3) | **Technisch machbar** | Gute Rechenleistung, aber signifikante Wärmeentwicklung und Akkubelastung bei längeren Inferenzphasen. |
| **CPU (Hauptprozessor)** | ARMv9 Cortex-A720 & A520 mit NEON / FP16 Vektorbefehlen | **Vollständig unterstützt** | Höchste Stabilität und Kompatibilität. Begrenzung auf kleine Modelle (≤ 2B Parameter in INT4) ratsam, um RAM- und Hitzegrenzen zu wahren. |

### 2. Reproduzierbarer Geräte-Testplan & Schwellenwerte

1. **Vorbedingungen:**
   - Freier Arbeitsspeicher: mindestens 2.0 GiB (gemessen vor Start via `ActivityManager.MemoryInfo`).
   - Akkuladestand: ≥ 25%, Gerät nicht im thermischen Drosselungszustand (`PowerManager.isDeviceIdleMode()` bzw. Thermal Status `<= THERMAL_STATUS_MODERATE`).
2. **Testschritte für lokale Modellläufe:**
   - **Schritt 1 (RAM-Check):** Vor Allokation prüfen, ob Modellgewicht + KV-Cache den freien RAM um mehr als 60% übersteigen. Falls ja: Abbruch mit Warnung.
   - **Schritt 2 (Benchmark):** 100 Prompt-Tokens und 50 Generation-Tokens ausführen; Messung von Tokens/Sekunde und RAM-Delta.
   - **Schritt 3 (Temperatur & Drosselung):** Nach 3 Minuten Dauerlast die thermische Reaktion prüfen. Bei `THERMAL_STATUS_SEVERE` sofortige Pause und Entlastung.
3. **Kaskadierter Rückfallpfad (Fallback Policy):**
   ```
   [Lokaler Auftrag]
          │
          ▼
   Genug RAM frei? (≥ 1.8 GiB) ──(Nein)──► Lokaler Lauf deaktiviert → Verweis auf BYOK Cloud-API
          │ (Ja)
          ▼
   Vulkan GPU Delegate stabil? ──(Nein)──► Automatischer Fallback auf CPU (ARM NEON)
          │ (Ja)
          ▼
   Ausführung auf GPU/CPU mit thermischer Überwachung
   ```

### 3. Konformitätsprüfung
- [x] NPU-Nutzung wird nicht leichtfertig versprochen, sondern ehrlich als vorläufig nicht verifiziert deklariert.
- [x] Transparente Rückfallkette auf CPU/GPU und sauberer Abbruch bei Ressourcenmangel.
- [x] Keine Anforderung von Root-Rechten oder inoffiziellen Treibern.

## Fertig, wenn
- NPU-Nutzung nur behauptet wird, wenn sie messbar belegt ist.
- Ein klarer CPU/GPU-Rückfall und ein „nicht unterstützt“-Ergebnis existieren.

## Schutz
Keine inoffiziellen Treiber, Systemänderungen oder Root-Rechte voraussetzen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-On-Device-KI-Skill suchen, Quellcode/Lizenz und Geräteanforderungen prüfen; Installation Nutzer-bestätigt.
