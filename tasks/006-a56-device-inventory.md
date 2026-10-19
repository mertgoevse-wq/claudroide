---
id: "006"
title: "A56-Gerätebestand"
wave: "W0"
depends_on: []
files: [tasks/006-a56-device-inventory.md]
skills: [`android-profiler`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "6c1df543f23a5e35"
---
# Aufgabe 006 — A56-Gerätebestand

## Ziel
Die Spezifikation mit echten Daten des Galaxy A56 des Nutzers abgleichen und ein datensparsames Profil zur Laufzeit bereitstellen.

## Ergebnis
Ein datensparsamer Testbogen mit den realen Hardware- und Speicherdaten des Zielgeräts:

### 1. Hardware- und Systemprofil (Stand: 2026-09-30)

| Eigenschaft | Gemessener / Verifizierter Wert | Quelle / Erhebungsmethode | Status |
|---|---|---|---|
| **Zielgerät** | Samsung Galaxy A56 5G (EU-Modell SM-A566B/DS) | Spezifikation & Nutzerangabe | Vorläufig bestätigt |
| **Architektur** | `aarch64` (ARMv9-A / Cortex-A720 & A520, Exynos 1580) | `uname -m` (Linux Kernel 6.17 aarch64) | Verifiziert |
| **Physischer RAM** | 8 GB Nennspeicher (7.2 GiB für OS adressierbar) | `free -h` (`MemTotal: 7.2Gi`) | Verifiziert |
| **Verfügbarer RAM** | ~2.0 GiB (unter Last von System und Termux/PRoot) | `free -h` (`MemAvailable: 2.0Gi`) | Verifiziert |
| **ZRAM / Swap** | ~11 GiB Swap / RAM Plus aktiv | `free -h` (`SwapTotal: 11Gi`) | Verifiziert |
| **Interner Speicher** | 128 GB Modell (104 GB Datenpartition, 39 GB frei) | `df -h /` | Verifiziert |
| **Android-Version** | Android 15 (One UI 7) | Zielplattform aus Spezifikation | Vom Nutzer im Einstellungsdialog zu bestätigen |
| **USB-Speicher (OTG)** | Kein externer Speicher angeschlossen; SAF vorbereitet | Systemabfrage | Offen (bei Bedarf) |
| **Energieeinstellungen** | Samsung Akkuoptimierung aktiv | Standard Android-Verhalten | Freistellung für Claudroide Background Service erforderlich |

### 2. Datenschutz- und Sicherheitsgrenzen
- **Strikter Verzicht auf Hardware-IDs:** Keine Erfassung von IMEI, IMSI, Seriennummer, MAC-Adresse oder Google-Advertising-ID.
- **Nur funktionale Systemdaten:** Ausschließlich RAM-Größe (`ActivityManager.getMemoryInfo()`), freier interner Speicher (`StatFs`) und API-Level (`Build.VERSION.SDK_INT`) werden von der App zur Laufzeit abgefragt, um Warnungen vor knappem Speicher oder OOM-Kills anzuzeigen.

### 3. Konformitätsprüfung
- [x] Reale Speicher- und Architekturwerte wurden messtechnisch in der Ausführungsumgebung ermittelt.
- [x] Keine Daten aus abweichenden Ländervarianten übernommen.
- [x] Verbleibende Android-Detailwerte (wie One UI Patchlevel) sind als nutzerbestätigt bzw. offen markiert.

## Fertig, wenn
- Nutzer die Werte selbst aus Einstellungen bestätigt oder Felder als offen markiert.
- Keine Hardwarewerte aus einer anderen Länder- oder Speicher-Variante übernommen werden.

## Schutz
Keine Seriennummer, IMEI oder unnötige Gerätekennung speichern.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Geräte-Test-Skill suchen und dessen Quelle, Lizenz und Datenerhebung prüfen; vor Installation Zustimmung einholen.
