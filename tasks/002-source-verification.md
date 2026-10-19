---
id: "002"
title: "Quellen und Aktualität prüfen"
wave: "W0"
depends_on: []
files: [tasks/002-source-verification.md]
skills: [`/swarm-planner`, `testing-setup`]
status: done
gate: false
done_since_last_edit: true
content-hash: "e891a47dc30a335c"
---
# Aufgabe 002 — Quellen und Aktualität prüfen

## Ziel
Ein Quellenverzeichnis für Android-, Anbieter-, Lizenz- und Geräteangaben führen.

## Ergebnis
Jede zeitabhängige Aussage nennt eine Primärquelle, Abrufdatum und betroffene Produktentscheidung. Bei Widerspruch bleibt die Aussage offen, bis der Nutzer entscheidet.

### 1. Primärquellenverzeichnis (Stand: September 2026)

| Bereich | Thema | Primärquelle / Dokumentation | Abruf-/Prüfdatum | Betroffene Produktentscheidung |
|---|---|---|---|---|
| **Android** | SAF (Storage Access Framework) | Android Developers Guide: *Open and modify documents using SAF* (`developer.android.com/training/data-storage/shared/documents-files`) | 2026-09-30 | Ordner- und Dateizugriff über System-Dateiauswahl (`ACTION_OPEN_DOCUMENT_TREE`) ohne pauschale Speicherberechtigung |
| **Android** | Foreground Services & WorkManager | Android Developers Guide: *Foreground service types and permissions* (`developer.android.com/about/versions/14/changes/fgs-types-required`) | 2026-09-30 | Hintergrundaufgaben (Befehle/Builds) zwingend mit sichtbarer Benachrichtigung und klarer Typisierung (`shortService` / `specialUse`) |
| **Android** | NNAPI Abkündigung & ML Acceleration | Android Developers Guide: *Neural Networks API Overview* (`developer.android.com/ndk/guides/neuralnetworks`) — Status: Deprecated in Android 15 | 2026-09-30 | Keine starre NNAPI-Bindung; NPU-Nutzung erst nach gerätespezifischem Benchmark; primärer Fallback CPU/GPU (Vulkan) |
| **Anbieter** | Anthropic Claude Messages API | Offizielle Anthropic API-Dokumentation (`docs.anthropic.com/en/api/messages`) | 2026-09-30 | BYOK via Header `x-api-key` und `anthropic-version: 2023-06-01`; SSE Streaming (`text/event-stream`); Verbot von Web-Session-Scraping |
| **Anbieter** | OpenRouter Unified API | Offizielle OpenRouter API-Dokumentation (`openrouter.ai/docs`) | 2026-09-30 | OpenAI-kompatibler Endpunkt (`/api/v1/chat/completions`) mit BYOK; Modellauflistung über `/api/v1/models` |
| **Anbieter** | Google Antigravity / Gemini | Google AI for Developers (`ai.google.dev/api/rest`) | 2026-09-30 | Ausschließlich dokumentierte REST-Schnittstellen mit API-Key; inoffizielle/undokumentierte Pfade werden explizit abgelehnt |
| **Protokolle** | OpenAI-kompatible Schnittstelle | OpenAI API Reference (`platform.openai.com/docs/api-reference/chat`) | 2026-09-30 | Einstellbare Custom-Endpunkte (z.B. lokale Server, Ollama, vLLM) nach Standardformat |
| **Hardware** | Samsung Galaxy A56 5G | Samsung Semiconductor & Gerätedatenblatt (Exynos 1580 Architektur) | 2026-09-30 | Keine Schätzung von RAM/Speicher; Abfrage zur Laufzeit über `ActivityManager` / `StatFs` |
| **Recht/Marke** | Anthropic Brand Guidelines | Anthropic Trademark & Brand Policy (`anthropic.com/legal/brand`) | 2026-09-30 | Eigenständiger Name „Claudroide“; keine Anthropic-Logos oder Verwechslungsgefahr; Unabhängigkeitshinweis verpflichtend |

### 2. Prüfkriterien & Regeln für Recherche
1. **Keine Sekundärquellen als Beweis:** Zusammenfassungen von Suchmaschinen oder Blogbeiträgen sind keine verbindliche Baugrundlage.
2. **Datenschutz bei Recherche:** Keine Projektpfade, Tokens, Systemlogs oder Nutzercodes an externe Suchmaschinen oder Webseiten übertragen.
3. **Widerspruchsbehandlung:** Steht eine Anbieterdoku im Widerspruch zur Spezifikation (z.B. geänderte API-Formate oder Preise), wird der Widerspruch im Checkpoint dokumentiert und die Entscheidung offen gehalten.

### 3. Konformitätsprüfung
- [x] Jede integrierte Anbieterfunktion beruht auf offizieller Dokumentation.
- [x] Abrufdaten und Primärquellen sind transparent erfasst.
- [x] Keine automatische Übernahme von unbestätigten Hardwarewerten oder inoffiziellen APIs.

## Fertig, wenn
- Jede integrierte Anbieterfunktion auf offizieller Dokumentation beruht.
- Keine Suchmaschinen-Zusammenfassung als verbindlicher Beleg verwendet wird.

## Schutz
Keine Zugangsdaten oder privaten Projektinformationen an Recherche-Webseiten senden.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen Skill für technische Recherche/Quellenprüfung suchen; Inhalt, Quelle und Lizenz prüfen, dann vor Installation Nutzerzustimmung einholen.
