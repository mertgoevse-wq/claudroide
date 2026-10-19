---
id: "051"
title: "Antigravity-Verbindungsweg"
wave: "W12"
depends_on: [004, 005, 047, 052, 059]
files: [tasks/051-antigravity-official-route-check.md]
skills: [`/swarm-planner`, `/claude-api`]
status: done
gate: false
done_since_last_edit: true
content-hash: "0c8a420c3228c899"
---
# Aufgabe 051 — Antigravity-Verbindungsweg

## Ziel
Antigravity nur dann unterstützen, wenn Google einen dokumentierten Drittanbieterweg ausdrücklich anbietet.

## Ergebnis
Offizielle Quelle, Authentifizierung, Nutzungsregeln, API-Fähigkeiten und Android-Eignung oder begründetes „nicht verfügbar“.

## Fertig, wenn
- Kein Zugang über kopierte CLI-Datei, inoffizielle Anmeldung oder Sitzungsweiterleitung nötig ist.
- Nutzer über Kosten und Datenverarbeitung informiert wird.

## Schutz
Keine Google-Token aus anderen Anwendungen auslesen.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
Global einen offiziellen Google-AI-Integrations-Skill suchen; Quelle, Rechte und Lizenz prüfen und Zustimmung vor Installation einholen.

---

## Prüfungsergebnis (2026-10-01)

**Was ist Antigravity?**
„Antigravity" ist kein offizieller Produktname von Google. Im App-Kontext bezieht es sich auf mögliche inoffizielle Wege zu Google-AI-Diensten (Gemini API, Vertex AI). Eine offizielle, für Drittentwickler dokumentierte „Antigravity"-API existiert nicht.

**Offiziell dokumentierte Google-AI-Wege:**
- **Gemini API** (aistudio.google.com): Dokumentierter BYOK-Weg für Entwickler; Authentifizierung über API-Key aus Google AI Studio; OpenAI-kompatibler Endpunkt verfügbar: `https://generativelanguage.googleapis.com/v1beta/openai/`.
- **Vertex AI**: Authentifizierung über OAuth 2.0 / Service Account — erfordert Cloud-Projekt, nicht direkt Android-BYOK-geeignet.

**Authentifizierung:** Gemini API erlaubt API-Key-Authentifizierung im BYOK-Muster. Kein Abo-Login, keine Sitzungsweiterleitung, keine Token aus anderen Apps.

**Kosten und Datenverarbeitung:** Google verarbeitet Anfragen auf eigenen Servern gemäß Google AI Studio / Gemini API Nutzungsbedingungen (Stand 2026-10-01). Nutzer tragen anfallende Kosten selbst.

**Android-Eignung:** API-Key-Authentifizierung ist Android-kompatibel. Das OpenAI-kompatible Format ermöglicht Nutzung des bestehenden OpenAI-Adapters (Task 054).

**Ergebnis:** Ein offizieller Drittanbieterweg existiert über die **Gemini API (OpenAI-kompatibel)**. Kann als optionaler Katalogeintrag `google_gemini` mit `OPENAI_COMPATIBLE`-Format und BYOK-Schlüssel eingebunden werden. Kein inoffizieller Zugang nötig. Implementierung erfordert explizite Nutzerfreigabe — kein Blockierungsgrund, aber kein aktiver Auftrag.
