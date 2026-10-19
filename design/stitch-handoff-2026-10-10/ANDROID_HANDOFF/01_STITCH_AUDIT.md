# Tatsächliche Analyse des Stitch-Exports

## Inventar

Der bereitgestellte ZIP-Export enthält:

- **26 erzeugte Screen-Ordner** mit jeweils `code.html` und `screen.png`.
- **11 als Referenz benannte Screenshots** aus ChatGPT-/Claude-Ansichten.
- Das ursprüngliche `claudroide_studio/DESIGN.md`.
- Einzige Sicherheitsbereinigung im mitgelieferten HTML: eine nur teilweise maskierte Beispiel-Key-Zeichenfolge wurde durch `ANTHROPIC_API_KEY_[REDACTED_SAMPLE]` ersetzt, damit sie nicht mit einem echten Geheimnis verwechselt wird. Die ursprüngliche hochgeladene ZIP-Datei bleibt unverändert.
- Ungefähr 572 KB HTML-Quelltext über die 26 Prototypen; der gesamte entpackte Inhalt beträgt rund 9,16 MB.

Die 26 Screens bilden die Kernpfade ab: Chat, aktive Konversation, Anhänge, Drawer, Verlauf/Suche, Projekte/Dateien, Artefakte, Coding, Code Review, Terminal, Agent-Task, Build, Medien, Video, Audio, Provider-Auswahl, Media Bridge, MCP, Skills/Plugins und Expertenmodus.

## Konkrete Auffälligkeiten im Quelltext und in den Vorschauen

1. **Die Exportdateien sind Web-Mockups, keine Android-Implementierung.** Alle 26 `code.html`-Dateien laden Tailwind über `cdn.tailwindcss.com`. Mehrere laden Schriftarten und Material Symbols von Google Fonts nach. Damit hängen sie bei einer direkten WebView-Nutzung von externen Ressourcen und deren Verfügbarkeit ab.
2. **Typografie ist nicht einheitlich.** Das mitgelieferte `DESIGN.md` verspricht Inter für UI und JetBrains Mono für Code, aber der Hauptbildschirm verwendet im Heading explizit `font-serif`. Andere Screens unterscheiden sich bei den Material-Symbol-Varianten. Es gibt sehr kleine UI-Labels (teilweise rund 10–12 px in CSS), die auf einem Telefon schnell gedrängt wirken.
3. **Die Startansicht enthält hardcodierte Demo-Personalisierung.** Der Text „Guten Tag, Mert“ ist Testinhalt und sollte nicht fest in die App übernommen werden. Standardmäßig lieber neutraler Text, der ohne personenbezogene Annahmen funktioniert.
4. **Zu viele umrahmte Flächen.** In Medien-, Provider-, MCP- und Projekt-Screens entstehen lange vertikale Seiten aus gestapelten Cards und wiederholten Untercontainern. Das ist genau die Card-in-Card-Hierarchie, die dein Audit kritisiert.
4. **Zu viele orange Primäraktionen.** Die Terracotta-Farbe ist auf vielen Seiten gleichzeitig für Buttons, Statusmarker und Akzente präsent. Das schwächt die eigentliche Primäraktion.
5. **Die Vorschauen simulieren ein Gerät im Weblayout.** Statusleiste, Telefonrahmen, Höhen und Abstände sind in HTML aufgebaut. Für das echte Android-UI müssen Systemleisten, Insets, Tastatur und Scrollverhalten vom nativen Android-System übernommen werden.
6. **Die Prototypen sind nicht automatisch funktional.** Sichtbare Buttons und Anzeigen beweisen weder echte Navigation noch echte Provider-, MCP-, Build- oder Medienintegration. Das muss Claude Code gegen die vorhandene Implementierung prüfen und verdrahten.
7. **Branding driftet in Richtung Claude.** Der Hauptscreen enthält im HTML-Kommentar ausdrücklich einen „Anthropic-inspired“ Sunburst; andere Screens nutzen „Anthropic Terracotta“-Kommentare. Das ist kein Grund, die vertraute Chat-Informationsarchitektur aufzugeben, aber ClauDroide sollte ein eigenes schlichtes Symbol verwenden und keine fremde Marke oder deren Erkennungszeichen nachahmen.
8. **Keine echte Generierungsanimation ableitbar.** CSS-Transitions oder kleine UI-Effekte sind nicht dasselbe wie ein realer Jobstatus. Fortschritt muss aus dem tatsächlichen Backend kommen.
8. **Beispiel-Konfigurationen sind nicht produktionsreif.** Einige HTML-Dateien enthalten illustrative Provider-URLs und ein maskiert wirkendes Beispiel für einen API-Key. Solche Werte dürfen nicht als echte Credentials oder produktive Endpoint-Konfiguration übernommen werden.

## Was wir behalten

- Die Informationsarchitektur und Reihenfolge der 26 Screens als Referenz.
- Die klare Trennung von Chat, Projektarbeit, Coding, Medien und Integrationen.
- Die ruhige helle Basis und den sparsam verwendeten warmen Akzent.
- Code-Diffs, Agentenstatus, MCP-/Provider-Konfiguration und Artefaktansichten als eigenständige, kontextuelle Screens.

## Was wir korrigieren

- Einheitliche, native UI-Typografie ohne Serif-Ausreißer und externe Schriftabhängigkeit.
- Weniger Cards; Sektionen mit Weißraum und dünnen Trennlinien gruppieren.
- Weniger gleichzeitige Primärbuttons; sekundäre Aktionen in Menüs/Bottom Sheets.
- Bedienelemente mindestens 48 dp Touchhöhe, angemessene Kontraste und größere Lesbarkeit.
- Native Android-Systemleisten, Insets, Back-Navigation und Tastaturverhalten.
- Subtile, zustandsgetriebene Animationen statt dekorativer Effekte oder erfundener Fortschrittsanzeigen.
- Implementierung in der vorhandenen Architektur, nicht als WebView-Klon.

## Was nicht kopiert werden soll

Keine fremden Logos, proprietären Assets, Markennamen im UI oder Pixel-für-Pixel-Reproduktion. Ziel ist die bekannte, einfache Konversationsbedienung und die Qualität eines professionellen KI-Arbeitswerkzeugs – mit eigener ClauDroide-Marke.
