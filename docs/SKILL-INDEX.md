# SKILL-INDEX — Vollständiges Verzeichnis aller verfügbaren Skills, Plugins und Marktplätze

**Stand:** 2026-10-08 · **Regel (Spec §28.6):** Lebendiges Dokument — nach jedem Bauabschnitt aktualisieren, wenn neue Skills/Plugins installiert wurden. Nicht gefundene Skills werden **nicht** erfunden, sondern bleiben ungelistet.

---

## 1. Globale Skills (`~/.claude/skills/`)

| Name | Pfad | Zweck (kurz) | Kategorie | Verwendet in Claudroide |
|---|---|---|---|---|
| adaptive | `~/.claude/skills/adaptive/` | Anpassungsfähige Agenten-Strategien | Arbeitsweise | — |
| android-permissions-security | `~/.claude/skills/android-permissions-security/` | Android-Berechtigungen sicher handhaben | Android/Compose | Phase 6 (Device Agent) |
| android-profiler | `~/.claude/skills/android-profiler/` | Android-Performance-Profiling | Android/Compose | Phase 10 / Performance |
| app-studio | `~/.claude/skills/app-studio/` | Autonomes Mobile App Studio (NatPRD + Compose + Build) | Arbeitsweise | — |
| auditing-compose-performance | `~/.claude/skills/auditing-compose-performance/` | Compose-Performance prüfen | Android/Compose | Phase 10 |
| avoiding-subcomposition-pitfalls | `~/.claude/skills/avoiding-subcomposition-pitfalls/` | Subcomposition-Fallen vermeiden | Android/Compose | Phase 1–3 |
| choosing-derivedstateof | `~/.claude/skills/choosing-derivedstateof/` | `derivedStateOf` richtig einsetzen | Android/Compose | Phase 1–3 |
| collecting-flows-safely | `~/.claude/skills/collecting-flows-safely/` | Flows sicher sammeln (Kotlin Coroutines) | Android/Compose | Phase 3–4 |
| compose-kotlin-agent-skills | `~/.claude/skills/compose-kotlin-agent-skills/` | Compose/Kotlin Agenten-Fähigkeiten (Sammlung) | Android/Compose | **Phase 1–8 (Kern)** |
| configuring-lazy-prefetch | `~/.claude/skills/configuring-lazy-prefetch/` | Lazy-Layout Prefetch konfigurieren | Android/Compose | Phase 5 (Listen) |
| configuring-r8-for-compose | `~/.claude/skills/configuring-r8-for-compose/` | R8/Optimierung für Compose | Android/Compose | Phase 10 (Release) |
| debugging-recompositions | `~/.claude/skills/debugging-recompositions/` | Rekompositionen debuggen | Android/Compose | Phase 3–4 |
| deferring-state-reads | `~/.claude/skills/deferring-state-reads/` | State-Lesen aufschieben | Android/Compose | Phase 3–4 |
| diagnosing-compose-stability | `~/.claude/skills/diagnosing-compose-stability/` | Compose-Stabilität diagnostizieren | Android/Compose | Phase 10 |
| enforcing-stability-in-ci | `~/.claude/skills/enforcing-stability-in-ci/` | Stabilität in CI erzwingen | Android/Compose | Phase 10 |
| generating-baseline-profiles | `~/.claude/skills/generating-baseline-profiles/` | Baseline-Profile erzeugen | Android/Compose | Phase 10 |
| graphify | `~/.claude/skills/graphify/` | Eingabe → Wissensgraph | Lernen/Erklären | Spec-Pflege, Meilenstein-Erklärungen |
| iterating-with-ai-and-mcp | `~/.claude/skills/iterating-with-ai-and-mcp/` | Iteration mit AI + MCP (Hot Reload) | Arbeitsweise | — |
| mcp-builder | `~/.claude/skills/mcp-builder/` (Link → `~/.agents/skills/mcp-builder`) | MCP-Server bauen | MCP | Phase 7 |
| migrating-to-modifier-node | `~/.claude/skills/migrating-to-modifier-node/` | Zu Modifier.Node migrieren | Android/Compose | — |
| natprd | `~/.claude/skills/natprd/` | PRD-Generierung via Interview | Lernen/Erklären | Spec-Qualität |
| optimizing-lazy-layouts | `~/.claude/skills/optimizing-lazy-layouts/` | Lazy-Layouts optimieren | Android/Compose | Phase 5 |
| ordering-modifier-chains | `~/.claude/skills/ordering-modifier-chains/` | Modifier-Ketten ordnen | Android/Compose | Phase 1–3 |
| parallel-task | `~/.claude/skills/parallel-task/` | Parallele Aufgaben orchestrieren | Arbeitsweise | Phase 0–10 (Wellen) |
| preserving-state-across-reloads | `~/.claude/skills/preserving-state-across-reloads/` | State über Hot Reload erhalten | Android/Compose | Phase 4 (Kontext) |
| setting-up-compose-hotswan | `~/.claude/skills/setting-up-compose-hotswan/` | Compose Hot Swap einrichten | Android/Compose | — |
| skill-creator | `~/.claude/skills/skill-creator/` (Link → `~/.agents/skills/skill-creator`) | Skills erstellen | Arbeitsweise | — |
| stabilizing-compose-types | `~/.claude/skills/stabilizing-compose-types/` | Compose-Typen stabilisieren | Android/Compose | Phase 1–3 |
| swarm-planner | `~/.claude/skills/swarm-planner/` | Schwarm-Planung (Multi-Agent) | Arbeitsweise | — |
| testing-compose-in-release-mode | `~/.claude/skills/testing-compose-in-release-mode/` | Compose im Release-Modus testen | Android/Compose | Phase 10 |
| testing-setup | `~/.claude/skills/testing-setup/` | Test-Setup (Unit, UI, Integration) | Android/Compose | **Phase 0–3** |
| tracing-recompositions-at-runtime | `~/.claude/skills/tracing-recompositions-at-runtime/` | Rekompositionen zur Laufzeit verfolgen | Android/Compose | Phase 10 |
| understanding-hot-reload-limits | `~/.claude/skills/understanding-hot-reload-limits/` | Hot-Reload-Grenzen verstehen | Android/Compose | — |
| understanding-stability-inference | `~/.claude/skills/understanding-stability-inference/` | Stabilitäts-Inferenz verstehen | Android/Compose | Phase 10 |
| using-efficient-effects | `~/.claude/skills/using-efficient-effects/` | Effiziente Side-Effects (LaunchedEffect, DisposableEffect) | Android/Compose | Phase 3–4 |
| using-stability-analyzer-ide-plugin | `~/.claude/skills/using-stability-analyzer-ide-plugin/` | IDE Stability Analyzer nutzen | Android/Compose | Phase 10 |
| using-strong-skipping-correctly | `~/.claude/skills/using-strong-skipping-correctly/` | Strong Skipping richtig nutzen | Android/Compose | Phase 3–4 |
| visualizing-recomposition-cascades | `~/.claude/skills/visualizing-recomposition-cascades/` | Rekompositions-Kaskaden visualisieren | Android/Compose | Phase 10 |

---

## 2. Globale Skills (`~/.agents/skills/`)

| Name | Pfad | Zweck (kurz) | Kategorie | Verwendet in Claudroide |
|---|---|---|---|---|
| mcp-builder | `~/.agents/skills/mcp-builder/` | MCP-Server bauen | MCP | Phase 7 |
| skill-creator | `~/.agents/skills/skill-creator/` | Skills erstellen | Arbeitsweise | — |

---

## 3. Projektbezogene Skills (in `claudroide-next`, sobald angelegt)

| Name | Pfad | Zweck (kurz) | Kategorie | Verwendet in Claudroide |
|---|---|---|---|---|
| claudroide-resume | `~/claudroide/.claude/skills/claudroide-resume/` | Wiederaufnahme-Muster (altes Repo) | Arbeitsweise | **Phase 0.8, 1.5** |

---

## 4. Eigene Repo-Skills (in `_sources/github/`, read-only)

| Repo | Skills/Plugins darin | Zweck | Übernommen? |
|---|---|---|---|
| claudroide | `.claude/skills/claudroide-resume/` | Wiederaufnahme | **JA** (als Vorlage) |
| claude-code-android | `tools/autonomous.sh`, `scripts/*`, Haken | Selbstlauf, Absicherungen, Doku | **JA** (Code, nicht als Skill) |
| lokicode | `domain/skills/`, `CAPABILITIES.md` | Fähigkeiten-Register, Provider | **JA** (Code adaptieren) |
| mobile-linux-lab | `SKILLS_MATRIX.md`, Plan-Dokumente | Container, Geräteprüfung | **JA** (Referenz) |
| clawscreen | `tools/anti-slop.py`, `sync_frontmatter.py` | Anti-Slop, Task-Konsistenz | **JA** (Code portieren) |
| design-skill-library | 321 Design-Fähigkeiten, `select-skills.py`, `slop-scan.sh` | Katalog-Muster, Anti-Slop | **JA** (Muster adaptieren) |
| claude-media-bridge | MCP-Server, Ein-Klick-Gratis | MCP, Medien, Gratis-Setup | **JA** (Code adaptieren) |
| airbeat-studio | Steuersprache (IMU) | Musik-Apps steuern | Phase 11 |
| Genesis_Harness | 56 Skills, Rollen-Register, Provider-neutral | Muster für Provider/Register | **JA** (Referenz) |
| droidroute | Ktor-Server, Provider-Schicht, Handbücher | Lokale Tür, Provider | Phase 11 (nach Stand) |
| PerpyBridge | Kostenschutz, Null-Logging | Kosten/Logging-Muster | **JA** (Referenz) |

---

## 5. Installierte Plugins (`~/.claude/plugins/`, Stand 2026-10-08)

| Plugin | Marktplatz | Version | Zweck (kurz) | Kategorie | Verwendet in Claudroide |
|---|---|---|---|---|---|
| figma | claude-plugins-official | 2.2.127 | Figma Design-to-Code, Code Connect | Design | Phase 7, 11 |
| ecc | ecc | 2.2.2 | 293 Skills (Review, Build, Test, Plan, ...) | Arbeitsweise | **Kern (alle Phasen)** |
| mobile-app-builder | awesome-claude-code-plugins | 1.0.0 | Native iOS/Android App-Entwicklung | Android/Compose | Phase 1–6 |
| ui-designer | awesome-claude-code-plugins | 1.0.0 | UI-Komponenten, Design-Systeme | Design | Phase 1, 7, 11 |
| frontend-developer | awesome-claude-code-plugins | 1.0.0 | React/Vue/Angular, State Management | Design | Phase 2, 5 |
| frontend-design | claude-plugins-official | 315c4e4 | Frontend-Design-Prinzipien, Komponenten | Design | Phase 1, 7 |
| create-worktrees | awesome-claude-code-plugins | 1.0.0 | Git Worktree-Verwaltung | Arbeitsweise | — |
| commit-commands | claude-plugins-official | 315c4e4 | Commit, Push, PR Automatisierung | Arbeitsweise | **Phase 1.4, 10** |
| test-file | awesome-claude-code-plugins | 1.0.0 | Test-Datei-Erstellung | Testing | Phase 0–3 |
| test-writer-fixer | awesome-claude-code-plugins | 1.0.0 | Tests schreiben, laufen lassen, fixen | Testing | **Phase 0–10** |
| documentation-generator | awesome-claude-code-plugins | 1.0.0 | Dokumentation generieren | Dokumentation | Phase 0.1, 10.3 |
| code-review | claude-plugins-official | 315c4e4 | Code-Review (Qualität, Sicherheit) | Review | **Phase 1.4, 3.3, 10.1** |
| security-guidance | claude-plugins-official | 2.0.11 | Sicherheits-Patterns, OWASP | Sicherheit | **Phase 1.4, 8, 10.1** |
| rapid-prototyper | awesome-claude-code-plugins | 1.0.0 | Schnelles MVP/Prototyp bauen | Arbeitsweise | — |
| supabase | claude-plugins-official | 0.1.15 | Supabase (Postgres, Auth, Edge Functions) | Backend | — |
| superpowers | claude-plugins-official | 6.4.1 | Superpowers (Prompt-Eng, Agent-Muster) | Arbeitsweise | **Spec §24.1** |
| agent-manager-skill | awesome-claude-code-plugins | 0.1.0 | Agenten-Verwaltung | Arbeitsweise | — |
| skill-creator | claude-plugins-official | 315c4e4 | Skills erstellen | Arbeitsweise | — |
| ai-engineer | awesome-claude-code-plugins | 1.0.0 | AI/ML Features, LLMs, Recommendations | AI | — |
| chrisbanes-skills | chrisbanes-skills | 2026.10.3 | Compose Animationen, Komponenten, Fokus, Performance, State | Android/Compose | **Kern (Phase 1–8)** |
| android-review | android-review | 1.8.0 | Android/Compose Review (Architektur, UI, Performance, Security) | Android/Compose | **Phase 1–6, 10** |
| gradle-skills | gradle-skills | 1.0.0 | Gradle Best Practices, Wrapper Upgrade | Build | **Phase 1.3, 10** |
| claude-code-setup | claude-plugins-official | 1.0.0 | Claude Code Automation Empfehlungen | Arbeitsweise | — |
| pr-review-toolkit | claude-plugins-official | 315c4e4 | PR Review (Tests, Coverage, Silent Failures) | Review | **Phase 10** |
| code-simplifier | claude-plugins-official | 1.0.0 | Code vereinfachen, Klarheit | Refactoring | **Phase 10.5** |
| context7 | claude-plugins-official | 315c4e4 | Aktuelle Library-Docs abrufen | Docs | **Phase 0.2, 7** |
| plugin-dev | claude-plugins-official | 315c4e4 | Plugin-Entwicklung (Commands, Hooks, MCP) | Arbeitsweise | — |
| hookify | claude-plugins-official | 315c4e4 | Hooks aus Gesprächen ableiten | Arbeitsweise | — |
| brag | brag | 0.4.0 | Vorstellungs-Videos für Releases | Veröffentlichen | **Phase 10.3, §17** |

---

## 6. Bekannte Marktplätze (`~/.claude/plugins/marketplaces/`)

| Marktplatz | Quelle | Installationsort | Letzte Aktualisierung |
|---|---|---|---|
| claude-plugins-official | `github.com/anthropics/claude-plugins-official` | `~/.claude/plugins/marketplaces/claude-plugins-official` | 2026-10-08 |
| ecc | `github.com/affaan-m/ECC` | `~/.claude/plugins/marketplaces/ecc` | 2026-09-30 |
| awesome-claude-code-plugins | `github.com/ccplugins/awesome-claude-code-plugins` | `~/.claude/plugins/marketplaces/awesome-claude-code-plugins` | 2026-10-08 (autoUpdate) |
| chrisbanes-skills | Directory: `/tmp/skillscan/skills` | `/tmp/skillscan/skills` | 2026-10-03 |
| android-review | Directory: `/tmp/skillscan/android-review` | `/tmp/skillscan/android-review` | 2026-10-03 |
| gradle-skills | `github.com/gradle/gradle-skills` | `~/.claude/plugins/marketplaces/gradle-skills` | 2026-10-03 |
| superpowers-marketplace | `github.com/obra/superpowers-marketplace` | `~/.claude/plugins/marketplaces/superpowers-marketplace` | 2026-10-06 |
| brag | `github.com/latent-spaces/brag` | `~/.claude/plugins/marketplaces/brag` | 2026-10-06 |

---

## 7. Fremde Sammlungen (Referenz, Spec §24.1)

| Sammlung | Quelle | Lizenz | Skills/Plugins | Claudroide-Nutzung |
|---|---|---|---|---|
| superpowers (GitHub) | `obra/superpowers` | MIT | Prompt-Engineering, Agenten-Muster | **JA** (Spec §24.1) |
| gradle-skills (GitHub) | `gradle/gradle-skills` | Apache-2.0 | Gradle-Best-Practices, Wrapper-Upgrade | **JA** (Spec §24.1) |
| ECC (affaan-m/ECC) | `affaan-m/ECC` | MIT | 293 Skills (Review, Build, Test, Plan, …) | **JA** (Spec §24.1, Kern) |
| awesome-claude-code-plugins (GitHub) | `ccplugins/awesome-claude-code-plugins` | Apache-2.0 | Plugin-Katalog, Marktplatz | **JA** (Spec §24.1) |
| brag (GitHub) | `latent-spaces/brag` | MIT | Vorstellungs-Videos für Releases | **JA** (Spec §24.1, §17) |
| compose-performance-skills (GitHub) | `skydoves/compose-performance-skills` | Apache-2.0 | 26 Compose-Leistungs-Skills | **JA** (Spec §24.1, Kern) |
| claude-plugins-official (GitHub) | `anthropics/claude-plugins-official` | Apache-2.0 | Offizielle Plugins, Katalog-Muster | **JA** (Spec §24.1, Katalog) |
| chrisbanes-skills (GitHub) | `chrisbanes/*` | Apache-2.0 | Compose Animationen, Komponenten, Fokus, Performance, State | **JA** (Spec §24.1, Kern) |

---

## 8. Nutzung-Regeln (aus Spec §7.1, §28.7, §29.5)

1. **Progressive Disclosure:** Der Agent sieht zunächst nur Katalog-Metadaten (dieser Index). Bei passender Aufgabe wird der Skill vollständig geladen.
2. **Prüfung vor jedem Task:** Vor jeder Aufgabe prüft der Agent: „Gibt es einen Skill/Plugin, der hier hilft?“ → wenn ja, **automatisch laden und verwenden, ohne Rückfrage**.
3. **Eintragspflicht:** Tatsächliche Verwendung wird im Aufgaben-Checkpoint (`progress/BUILD-STATE.md`) festgehalten.
4. **Keine Erfindung:** Nicht gefundene oder nicht geprüfte Skills dürfen **nicht** als vorhanden ausgegeben werden.
5. **Lebendige Aktualisierung:** Nach jedem Bauabschnitt wird dieser Index ergänzt, wenn neue Skills/Plugins installiert wurden.

---

## 9. Nächste Aktualisierung (Phase 0.7)

- [x] Alle Einträge aus `~/.claude/plugins/` (Marktplätze) sichten und eintragen
- [x] `claude-plugins-official` Katalog vollständig durchgehen
- [x] `awesome-claude-code-plugins` Katalog vollständig durchgehen
- [ ] Eigene Repo-Skills (lokicode, mobile-linux-lab, design-skill-library) detaillierter erfassen
- [ ] Skill-Nutzung je Phase in der Tabelle „Verwendet in Claudroide“ eintragen