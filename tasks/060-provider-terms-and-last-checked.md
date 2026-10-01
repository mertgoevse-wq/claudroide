---
id: "060"
title: "Bedingungen und Prüfdatum"
wave: "W10"
depends_on: [005, 016, 017]
files: [app/src/main/java/org/claudroide/app/feature/provider/ProviderTermsVerifier.kt, app/src/test/java/org/claudroide/app/ProviderTermsTest.kt, tasks/060-provider-terms-and-last-checked.md]
skills: [`/swarm-planner`, `/code-review`]
status: done
gate: false
done_since_last_edit: true
content-hash: "024c8a9a6ce7641f"
---
# Aufgabe 060 — Bedingungen und Prüfdatum

## Ziel
Verhindern, dass veraltete Aussagen zu Anmeldung, Kosten oder Nutzungsregeln als aktuell gelten.

## Ergebnis
Je Integration Quelle, Prüfdatum, Verantwortlichkeit und Aktualisierungsbedarf.

## Fertig, wenn
- Ablauf oder Änderung einer Bedingung Integration zur erneuten Prüfung markiert (`ProviderTermsEngine.evaluateVerificationStatus` markiert Einträge > 90 Tage als `NEEDS_REVERIFICATION`).
- Unbestätigte Verbindungsvorlagen nicht als offiziell beworben werden (`canAdvertiseAsOfficial` erfordert `isOfficial` und HTTPS-ToS).
- Scraping- und Abo-Umgehungsversuche sofort als `DEPRECATED` deklassiert werden.

## Schutz
Keine Abo- oder Umgehungswege aus fremden Tutorials übernehmen. Strikte Einhaltung der offiziellen Richtlinien.

## Umgesetzte Architektur & Dateien
- `app/src/main/java/org/claudroide/app/feature/provider/ProviderTermsVerifier.kt`:
  - `enum class VerificationStatus`: VERIFIED_CURRENT, NEEDS_REVERIFICATION, COMMUNITY_UNVERIFIED, DEPRECATED.
  - `data class ProviderTermsRecord`: Nachweisdokumentation mit AGB-URL, Datenschutz-URL, Prüfdatum und Prüfer.
  - `object ProviderTermsEngine`: 90-Tage-Ablauffrist und Schutz vor unzulässigen Abo-Bypässen.
- `app/src/test/java/org/claudroide/app/ProviderTermsTest.kt`:
  - Unit-Tests für Verifikationszeitfenster (< 90 Tage aktuell, > 90 Tage Re-Audit), Community-Vorlagen und strikte Deprecation von Scraping-Mustern.

## Skills (aus skill-matrix.md)
Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md ersetzen statt zu raten.
- `/swarm-planner`: Prüfkalender und Abhängigkeitsregeln für Anbieter-Audits strukturiert.
- `/code-review`: Ausschluss von Abo-Umgehungswegen und Wahrung offizieller Kennzeichnungen überprüft.

