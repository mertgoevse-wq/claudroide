---
id: "046"
title: "Geheimnisse verbergen"
wave: "W11"
depends_on: [044, 045]
files: [app/src/main/java/org/claudroide/app/feature/provider/SecretMasker.kt, app/src/test/java/org/claudroide/app/SecretMaskerTest.kt, tasks/046-secret-redaction.md]
skills: [`android-permissions-security`, `testing-setup`]
status: done
gate: true
done_since_last_edit: true
content-hash: "30018d46c44f00fb"
---
# Aufgabe 046 — Geheimnisse verbergen

## Ziel
API-Schlüssel und sensible Zugänge in Logs, Fehlern, Chat-Ausgaben und Exports verhindern.

## Ergebnis
Zentrale Maskierung, Testfälle mit synthetischen Beispielmustern und sichere Diagnosemeldungen.

## Fertig, wenn
- Testschlüssel in keinem Protokoll oder Export im Klartext erscheinen (`SecretMasker.redact` und `auditAndRedact` filtern Anthropic, OpenAI/OpenRouter, Bearer-Header, Passwörter und PEM-Zertifikate).
- Maskierung nicht fälschlich als vollständige Sicherheitsgarantie deklariert wird (`SecretMasker.isCompleteSecurityGuarantee() = false`, Defense-in-Depth).
- Tests ausschließlich synthetische Muster ohne reale Nutzerschlüssel verwenden.

## Schutz
Keine echten Nutzer-Schlüssel in Tests oder Aufgabenbeispielen. Ausschließlich synthetische Testmuster (`sk-ant-synthetic-...`).

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/provider/SecretMasker.kt`:
  - `object SecretMasker`: Regex-Bereinigung von Anthropic Keys, OpenAI Keys, Bearer Auth Headern, Private Keys und Passwort-Zuweisungen.
  - `data class RedactionAuditReport`: Prüfbericht über gefundene und ersetzte Geheimnismuster.
  - Defense-in-Depth-Invariante (`isCompleteSecurityGuarantee = false`).
- `app/src/test/java/org/claudroide/app/SecretMaskerTest.kt`:
  - Unit-Tests für synthetische Schlüsselbereinigung, Header-Filterung, Zertifikatsblock-Bereinigung und Invariantenprüfung.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `android-permissions-security`: Regex-Muster und Leckschutz für vertrauliche Tokens in Logs und UI-Ausgaben überprüft.
- `testing-setup`: Unit-Tests für synthetische Secret-Maskierung und Audit-Berichte implementiert.

