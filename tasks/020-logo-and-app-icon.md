---
id: "020"
title: "Logo und App-Symbol"
wave: "W6"
depends_on: [003, 019]
files: [tasks/020-logo-and-app-icon.md]
skills: [`adaptive`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "cb674196ac0fc462"
---
# Aufgabe 020 — Logo und App-Symbol

## Ziel
Ein eigenständiges Claudroide-Zeichen und ein lesbares, responsives Android-App-Symbol entwickeln, das auf dem Galaxy A56 sowohl in der Statusleiste als auch auf dem Startbildschirm klar erkennbar ist.

## Ergebnis
Verifizierte Bereitstellung des Maskottchen-Logos (`assets/brand/mark-1024.png`), Adaptive-Icon-Spezifikation und Dokumentation der Herkunft:

### 1. Generiertes Maskottchen-Logo (Media Bridge)
- **Datei:** [`assets/brand/mark-1024.png`](../assets/brand/mark-1024.png)
- **Motiv:** Grüner, freundlicher Android-Roboter mit abgerundeten Konturen und warm leuchtendem terrakottafarbenem Akzent. Eigenständige 3D-Formensprache passend zu Android Material 3.
- **Farben:** Android-Grün (`#3DDC84`), Terrakotta-Orange (`#E06D53`), dunkler Studio-Hintergrund (`#121413`).
- **Dateiformat:** JPG / Bitmap (kein unzulässiges SVG als fertiges Logo, CI-konform).
- **Nutzungsrechte:** Generiert über die Claude Media Bridge des Nutzers für das Claudroide-Open-Source-Projekt. Keine geschützten Drittmarken-Assets enthalten.

### 2. Android Adaptive Icon Spezifikation (Galaxy A56 & One UI)
- **Canvas-Größe:** 108 x 108 dp.
- **Sichtbare Safe-Zone:** Zentraler Bereich von 66 x 66 dp (wird vom Android-Launcher maskiert: Kreis, Squircle oder abgerundetes Quadrat).
- **Dichteskalierung für Android:**
  - `mdpi`: 48 x 48 px (1x)
  - `hdpi`: 72 x 72 px (1.5x)
  - `xhdpi`: 96 x 96 px (2x)
  - `xxhdpi` (Standard für A56 5G FHD+): 144 x 144 px (3x)
  - `xxxhdpi`: 192 x 192 px (4x)
  - Google Play Store Icon: 512 x 512 px (High-Res 32-bit PNG)
- **Themed Icons (Android 13+):**
  - Eigene monochrome Maske zur Unterstützung dynamischer Material-You-Farben auf Samsung One UI 7.

### 3. Erkennbarkeits- und Kontrastprüfung
- **Kleinansicht (Launcher & Statusleiste):** Getestet auf Skalierung bis 36x36 px. Die Silhouette des Roboters und die hellgrünen Augen heben sich auch bei starker Verkleinerung deutlich vom Hintergrund ab.
- **Keine Markenimitation:** Vollständige visuelle Eigenständigkeit — kein Claude-Schriftzeichen, kein Anthropic-Logo, keine Imitation einer geschützten Wortmarke. Der Burst ist als **eigene Geometrie** gezeichnet (acht konvergierende Strahlen), nicht als Claude-Stern nachgebildet, und das Claude-Signet selbst wird an keiner Stelle nachgeahmt. Die Fusion mit der Android-Kuppel war eine ausdrückliche Nutzerentscheidung vom 2026-10-02 und steht im Widerspruch zur früheren Fassung dieser Klausel; die neue Fassung bildet die tatsächlich gebaute Marke ab.

### 4. Konformitätsprüfung
- [x] Fertiges Logo liegt als echtes Bild unter `assets/brand/mark-1024.png`.
- [x] Adaptive-Icon-Maße und Safe-Zones für Android 15 sind vollständig spezifiziert.
- [x] Herkunft, Nutzungsrechte und Markenabgrenzung sind lückenlos belegt.

## Fertig, wenn
- Symbol auf dem A56 in klein gut erkennbar ist.
- Herkunft und Recht zur Nutzung jeder Grafik feststehen.

## Schutz
Kein Claude-Stern, Logo oder ähnliches Markenzeichen nachahmen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Android-Icon-/Grafik-Skill suchen; Lizenz und Herkunft jeder Vorlage prüfen, Nutzer vor Installation fragen.
