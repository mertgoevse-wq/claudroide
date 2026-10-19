# SECURITY — Sicherheits- und Schutzkonzept

**Stand:** 2026-10-10 · **Grundlage:** `claudroide-spec.md` §8, §17, §19 · **Status:** Task 0.1 — aus Spec abgeleitet

---

## 1. Schutzgrundsätze (Non-negotiables)

1. **BYOK & Schlüsselschutz:**
   - Nutzer hinterlegen ausschließlich eigene API-Schlüssel (Bring Your Own Key).
   - Alle geheimen Schlüssel werden im Android Keystore (`AndroidKeystoreKeyVault`) verschlüsselt gespeichert (AES-256-GCM, hardware-backed Keystore).
   - Schlüssel verlassen das Gerät ausschließlich im HTTPS-Header an den jeweils vom Nutzer konfigurierten Anbieter-Endpunkt.
   - Geheimnisse werden niemals in Logs, Fehlermeldungen, Chatverläufe, Session-Snapshots oder Git-Commits geschrieben (`SecretMasker`).

2. **Automatischer Secret-Scan:**
   - Vor jedem Git-Commit und vor jedem Git-Push prüft `GitSecretScan.kt` (und das Skript `tools/secret_gate.py`) alle Änderungen.
   - Erkannte Schlüsselmuster (Anthropic, OpenAI, GitHub PATs, AWS, PEM-Zertifikate) blockieren den Commit hart.

3. **Pfad- und Dateigrenzen (Path Boundary Enforcement):**
   - Der Dateizugriff der Agenten-Tools ist strikt auf das jeweils geöffnete Projektverzeichnis beschränkt.
   - `PathBoundaryGuard` verhindert Directory-Traversal-Angriffe (`../`, absolute Pfade außerhalb des Projekt-Wurzelverzeichnisses, Symlink-Ausbrüche).
   - Systemdateien, App-interne Datenverzeichnisse und fremde App-Daten bleiben unzugänglich.

4. **Harte Prozess- und Ressourcen-Schranken:**
   - **Exakt 1 aktiver Shell-Prozess** und **exakt 1 Subagent** gleichzeitig (Gerätestabilität auf mobiler Hardware).
   - Kein paralleles Forken unkontrollierter Hintergrundprozesse.
   - Alle ausgeführten Befehle durchlaufen den `CommandRiskScanner`, der riskante Operationen (`rm -rf /`, Fork-Bomben, Re-Flashing, Root-Versuche) blockiert oder an explizite Freigabe koppelt.

5. **Dreifaches Not-Aus für den Geräte-Agenten:**
   - Sichtbarer Not-Aus-Button in der oberen App-Leiste.
   - Schwebender System-Overlay-Not-Aus-Knopf während Bildschirmaktionen.
   - Physischer Not-Halt: Zweifaches Drücken der Leiser-Taste stoppt alle Aktionen sofort.
   - Nach Not-Aus startet keine Aktion selbstständig wieder an.

6. **Sperrliste für sensible Apps (Blacklist):**
   - Vordefinierte Blockierliste für Banking-Apps, Finanzdienstleister, Authenticator-Apps und Passwortmanager.
   - Auf gesperrten Apps führt der Accessibility-Dienst **kein Tippen**, **kein Lesen**, **keinen Screenshot** und **keine Datenübertragung** aus.

7. **Netzwerk- und Transport-Sicherheit:**
   - Für externe Cloud-Anbieter ist ausschließlich TLS/HTTPS zulässig (`NetworkSecurityPolicy`).
   - Klartext-HTTP ist nur für lokale Server auf dem Loopback-Interface (`127.0.0.1` / `localhost`, z.B. Ollama, OmniRoute Port 20128) erlaubt.
   - Unerwartete Cross-Host-Redirects und Downgrades von HTTPS auf HTTP werden strikt abgewiesen.
