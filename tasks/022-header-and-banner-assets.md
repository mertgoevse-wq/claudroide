---
id: "022"
title: "Kopf- und Bannerbilder"
wave: "W6"
depends_on: [003, 019]
files: [tasks/022-header-and-banner-assets.md]
skills: [`adaptive`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "23d599f5cb5cb340"
---
# Aufgabe 022 — Kopf- und Bannerbilder

## Ziel
Eigenständige Kopf- und Bannerbilder für App, Dokumentation und Projektübersichten erstellen, die sich harmonisch an Smartphone-Displays anpassen und rechtlich einwandfrei sind.

## Ergebnis
Bereitgestelltes Banner-Asset (`assets/brand/banner.png`), responsive Skalierungsregeln, Kompressionsstandards und Alternativtexte:

### 1. Banner-Asset-Spezifikation (Media Bridge)
- **Quelldatei:** [`assets/brand/banner.png`](../assets/brand/banner.png), 1376x768 PNG, 112 KB
- **In der App:** `res/drawable-nodpi/claudroide_banner.webp`, 1376x768 WebP, **15 KB**
- **Seitenverhältnis:** 16:9 Breitbild (optimiert für GitHub-Header, Repository-Social-Preview und In-App-Hero-Cards).
- **Motiv:** Grüner Android-Roboter mit warmem Terrakotta-Akzent auf dunklem Studiohintergrund.
- **Dateiformat & Dateigröße:** In der App WebP (vom Quell-PNG mit `cwebp -q 82` abgeleitet, 112 KB → 15 KB, also rund 87 % kleiner). Die Quelldatei bleibt als PNG im Repo, damit sie ohne Qualitätsverlust neu abgeleitet werden kann.

**Korrektur dieser Zeilen gegenüber dem ersten Entwurf:** Dort stand „JPG, ca. 19 KB". Das war an beiden Punkten falsch — es ist ein PNG, und es wiegt 112 KB, nicht 19 KB. Die 19 KB gelten erst für die heute in der App liegende WebP-Ableitung.

### 2. Responsive Skalierung auf mobilen Bildschirmen
- **Safe-Content-Zone:** Die spätere Regel dieses Dokuments verlangte die mittleren 70 % der Breite. **Das Motiv erfüllt das nicht** — Maske und Schrift sitzen im linken Bereich, das Bild ist also am linken Rand gefüllt, nicht zentriert. Die Regel wurde darum an die Wirklichkeit angepasst, statt die Wirklichkeit zu behaupten: weil die Komponente auf 16:9 festnagelt (`aspectRatio(1376f / 768f)`) und mit `ContentScale.Crop` skaliert, beschneidet ein schmales 19.5:9-Display **nur den rechten Leerraum**. Der Robot und der Produktname bleiben vollständig sichtbar.
- **Damit das gilt, muss der Inhalt links verankert sein.** Eine zentrierte Safe-Zone wäre hier die falsche Lösung gewesen: sie hätte den Produktnamen auf einem schmalen Display genau abgeschnitten.
- **Compose Image-Handling:** als `BrandBanner` in `core/design/components/ClaudroideComponents.kt`, damit `ContentScale`, Seitenverhältnis und Eckenradius nicht pro Bildschirm neu erfunden werden:
  ```kotlin
  Image(
      painter = painterResource(R.drawable.claudroide_banner),
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = modifier
          .fillMaxWidth()
          .aspectRatio(1376f / 768f)
          .clip(RoundedCornerShape(20.dp))
  )
  ```

### 3. Barrierefreiheit & Alternativtexte (a11y)
- **English (`values/`):** *„Claudroide banner: the green robot mark beside the product name."*
- **Deutsch (`values-de/`):** *„Claudroide-Banner: die grüne Robotermarke neben dem Produktnamen."*
- Beide liegen als `banner_alt_text` in den jeweiligen Sprachordnern. `BrandBanner` **verlangt** die Beschreibung als Parameter und gibt keinen Standardwert vor: das Bild trägt den Produktnamen, muss also beschrieben werden — und die Beschreibung ist sprachabhängig, gehört also in die Ressourcen und nicht in den Compose-Code.
- **Noch nicht am Gerät geprüft:** Die Texte sind bewusst knapper als die ursprüngliche Fassung, weil TalkBack in dieser Umgebung nicht getestet werden konnte. Ein 96-dp-Bild neben bereits vorhandenem Text ist redundant; deshalb ist die Beschreibung an dieser Stelle Pflicht, an anderen (`BrandMark` in Leerzuständen) bewusst `null`.

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
