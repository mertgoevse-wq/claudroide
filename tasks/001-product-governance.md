---
id: "001"
title: "Produktregeln und offene Entscheidungen"
wave: "W0"
depends_on: []
files: [tasks/001-product-governance.md]
skills: [`/swarm-planner`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "ab2f0a193dfad94c"
---
# Aufgabe 001 — Produktregeln und offene Entscheidungen

## Ziel
Die verbindlichen Leitplanken aus `claudroide-spec.md` in eine kurze, für alle Bauaufgaben nutzbare Projektregel übertragen.

## Ergebnis
Eine überprüfbare Liste aus Ziel, Nicht-Zielen, Freigabewegen, offenen Entscheidungen und zuständiger Bestätigung. Widersprüche werden als Frage markiert, nicht geraten.

### 1. Verbindliche Produktziele (Spec 2.2)
- **Installierbare Android-App:** Eigenständige Smartphone-Bedienung (Touch, A56-optimiert), ohne Termux-Zwang für Endnutzer.
- **BYOK („Bring Your Own Key“):** Eigene Anbieter-Schlüssel, konfigurierbare Endpunkte und Modelle. Kein Weiterverkauf von KI-Kontingenten.
- **Projektarbeitsmaschine:** Projekte öffnen, Quellcode durchsuchen, Änderungen vorab als Diff visualisieren, Freigaben einholen, Prüf-/Projektbefehle ausführen.
- **Mobile Entwicklung ohne PC:** Quellcode-Bearbeitung, Testausführung und Git-Synchronisation direkt vom Gerät (oder über transparenten Online-Bau).
- **Datensicherheit:** Lokale, verschlüsselte Ablage sensibler Daten; sicherer Umgang mit lokalen Ordnern, ZIP und Git.

### 2. Nicht-Ziele & Harte Grenzen (Spec 2.3 & 3)
- **Keine Marken- oder UI-Imitation:** Kein Kopieren von Anthropic-/Claude-Marken, geschützten Logos oder Identitäten. Eigenständiges Branding ("Claudroide").
- **Kein Claude-Abo-Login:** Keine unzulässige Vermittlung von Claude Free/Pro/Max Web-Logins oder Scraping. Ausschließlich offizielle Claude API mit Nutzerskripten/Nutzerschlüsseln.
- **Keine Nutzung inoffizieller Binaries:** Keine durchgesickerten oder rückentwickelten Binaries; keine Tarnung fremder CLI-Tools.
- **Keine Root-Rechte:** Kein Aushebeln von Android-Sicherheitsmechanismen, kein unautorisierter Speicherzugriff.
- **Kein stiller Datenabfluss:** Kein unangekündigtes Versenden ganzer Projekte oder API-Keys an Drittserver.
- **Keine erfundenen Hardware-Zusagen:** Keine unbewiesene Behauptung von NPU-Nutzung vor echten Gerätetests.

### 3. Freigabewege (Spec 6.1)
- **Stufe 1 — Vorsichtig (Standard):** Jede Dateiänderung, jeder Systembefehl, jede Netzwerkübertragung und jede Git-Aktion erfordert ausdrückliche Nutzerbestätigung.
- **Stufe 2 — Ausgewogen:** Im Projekt begrenzte, nicht-destruktive Dateiänderungen nach Freigabe des Arbeitsblocks; Befehle und externe Netzzugriffe bedürfen Bestätigung.
- **Stufe 3 — Weniger Rückfragen (Ausnahme-Modus):** Projektbezogen und zeitlich befristet zuschaltbar; wird niemals still oder standardmäßig aktiviert.

### 4. Matrix der offenen Entscheidungen & Zuständigkeiten
| Entscheidung | Status | Zuständigkeit | Vorgehensweise |
|---|---|---|---|
| Marken- & Namensprüfung | Offen vor Store-Release | Nutzer / Rechtsexperte | Recherche vor Veröffentlichung; "Claudroide" als vorläufiger Projektname |
| Lizenzwahl ("offen, aber geschützt") | Offen | Nutzer | Keine Lizenz still annehmen; bis zur Klärung privates Repository |
| A56-Gerätespezifikationen (RAM/NPU) | Offen bis Gerätetest | Nutzer / Task 006 | Echte Werte aus A56 erfassen; keine Annahmen treffen |
| Zusätzliche Monetarisierung | Offen / Später | Nutzer | Vorerst rein BYOK ohne Aufschlag |
| Lokale Modellbeschleunigung (NPU) | Offen (Machbarkeitsprüfung) | Task 007 | Fallback auf CPU/GPU wenn NPU nicht verifizierbar |
| Skill-Installationen | Vorab-Prüfpflicht | Nutzer | Vor jeder fremden Skill-Installation Prüfung von Lizenz/Sicherheit |

### 5. Konformitätsprüfung für Folgeaufgaben
- [x] Jede spätere Aufgabe verweist auf die verbindlichen Leitplanken und ändert Entscheidungen nicht eigenmächtig ab.
- [x] Offene Rechts-, Geräte-, Kosten- und Veröffentlichungsfragen sind explizit ausgewiesen.
- [x] Harte Leitplanken gegen Datenlecks, Markenverletzungen und unbefugte Systemänderungen sind verankert.

## Fertig, wenn
- Jede spätere Aufgabe auf passende Anforderungen verweist und Entscheidungen nicht still ändert.
- Offene Rechts-, Geräte-, Kosten- und Veröffentlichungsfragen sichtbar bleiben.

## Schutz
Keine Online-Aktion, kein Commit/Push und keine fremde Fähigkeit ohne passende Freigabe.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Bei Ausführung global nach einem Produktanforderungen-/Projektplanung-Skill suchen; Quelle, Lizenz und Sicherheit prüfen. Den Skillnamen erst nach Fund und Nutzerbestätigung eintragen; nicht ungefragt installieren.
