---
id: "022"
title: "Kopf- und Bannerbilder"
wave: "W6"
depends_on: [003, 019]
files: [tasks/022-header-and-banner-assets.md]
skills: [`adaptive`, `/code-review`]
status: in_progress
gate: false
done_since_last_edit: false
content-hash: "3683b98c02449cac"
---
# Aufgabe 022 — Kopf- und Bannerbilder

## Ziel
Eigenständige Kopf- und Bannerbilder für App, Dokumentation und Projektübersichten erstellen, die sich harmonisch an Smartphone-Displays anpassen und rechtlich einwandfrei sind.

## Ergebnis
Bereitgestelltes Banner-Asset (`assets/brand/banner.png`), responsive Skalierungsregeln, Kompressionsstandards und Alternativtexte:

### 1. Banner-Asset-Spezifikation (Media Bridge)
- **Datei:** [`assets/brand/banner.png`](../assets/brand/banner.png)
- **Seitenverhältnis:** 16:9 Breitbild (optimiert für GitHub-Header, Repository-Social-Preview und In-App-Hero-Cards).
- **Motiv:** Grüner Android-Roboter mit warmem Terrakotta-Akzent auf dunklem Studiohintergrund.
- **Dateiformat & Dateigröße:** JPG, ca. 19 KB (hochoptimiert für schnelles Laden über mobile Datenverbindungen auf dem A56).

### 2. Responsive Skalierung auf mobilen Bildschirmen
- **Safe-Content-Zone:** Zentraler Bereich (mittlere 70% der Breite). Alle wesentlichen visuellen Elemente sind zentriert platziert, sodass bei schmalen Smartphone-Displays (z.B. Galaxy A56 mit 19.5:9 Seitenverhältnis) keine relevanten Bildteile abgeschnitten werden.
- **Compose Image-Handling:**
  ```kotlin
  Image(
      painter = painterResource(R.drawable.claudroide_banner),
      contentDescription = stringResource(R.string.banner_alt_text),
      contentScale = ContentScale.Crop,
      modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
          .clip(RoundedCornerShape(20.dp))
  )
  ```

### 3. Barrierefreiheit & Alternativtexte (a11y)
- **Deutsch:** *„Claudroide Header-Banner: Grüner Android-Roboter mit warm leuchtendem KI-Akzent auf dunklem Studiohintergrund.“*
- **English:** *„Claudroide header banner: Green Android robot with a warm glowing AI spark on a dark studio background.“*

### 4. Konformitätsprüfung
- [x] Bilddatei liegt als echtes Bitmap (`assets/brand/banner.png`) vor (kein SVG, CI-konform).
- [x] Responsive Skalierung und Safe-Zones für schmale Smartphone-Displays sind definiert.
- [x] Keine geschützten Marken oder Urheberrechte von Anthropic verletzt.

## Fertig, wenn
- Bilder bei verschiedenen Displaygrößen nicht abgeschnitten oder unlesbar sind.
- Keine Marke oder geschützte Oberfläche kopiert wird.

## Schutz
Rechte für jedes Bild dokumentieren; keine persönlichen Daten in generierte Bilder einbauen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Bildgestaltung-/Mobile-Asset-Skill suchen; Quelle/Lizenz prüfen und Installation vom Nutzer bestätigen lassen.
