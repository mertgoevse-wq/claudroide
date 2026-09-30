#!/usr/bin/env python3
"""Claudroide — Task-Frontmatter-Synchronisation.

Aufgabe:
- Liest tasks/DEPENDENCIES.md (Wellen, Vorgänger, Gates) und tasks/skill-matrix.md
  (zwei Skills je Task).
- Schreibt in jede tasks/NNN-*.md einen einheitlichen YAML-Kopf mit
  id, title, wave, depends_on, files, skills, status, gate, done_since_last_edit,
  content-hash. Fremde Frontmatter-Felder bleiben erhalten.
- Benennt die Skill-Sektion einheitlich in "## Skills (aus skill-matrix.md)" um.
- status ohne Angabe ist "pending". "done" gilt nur als verifiziert
  (done_since_last_edit: true), wenn der Inhalt seit dem Setzen von "done"
  unveraendert geblieben ist (SHA-256 des Markdown-Koerpers). Eine spaetere
  Aenderung stuft "done" automatisch auf "in_progress" zurueck.

Aufruf im Projektordner:
    python3 tools/sync_frontmatter.py                     # pruefen und reparieren
    python3 tools/sync_frontmatter.py --check             # nur pruefen (Exit 1 bei Abweichung)
    python3 tools/sync_frontmatter.py --status 042=done   # Status setzen (mehrere erlaubt)

Nur lokale Dateioperationen; kein Netz, keine externen Befehle.
"""

import argparse
import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TASKS = ROOT / "tasks"
DEPS = TASKS / "DEPENDENCIES.md"
MATRIX = TASKS / "skill-matrix.md"

SKILLS_SECTION_TITLE = "Skills (aus skill-matrix.md)"
SKILL_NOTE = (
    "Bei Ausführung **beide** Skills tatsächlich laden und ihre Verwendung im "
    "Checkpoint (`progress/BUILD-STATE.md`) notieren. Fehlt einer, nach CLAUDE.md "
    "ersetzen statt zu raten."
)

STATUS_VALUES = ("pending", "in_progress", "done", "blocked")
SYNC_KEYS = {
    "id", "title", "wave", "depends_on", "files", "skills",
    "status", "gate", "done_since_last_edit", "content-hash",
}

# Regeln, die DEPENDENCIES.md nur in Prosa ausdrückt (nicht als Pfeilkante):
EXTRA_DEPS = {
    "106": ["119", "120"],  # W24 (Sicherheitskern) muss vor W25-Befehlen abgeschlossen sein
}

# Tasks, die als Sicherheitsgate gelten (Welle 24/26/27/28 plus Kerngates aus Regel 6)
GATE_WAVES = {"W24", "W26", "W27", "W28"}
GATE_IDS = {
    "017", "018", "045", "046", "052", "059", "067", "068", "083", "084",
    "085", "090", "095", "100", "101", "108", "117", "119", "120", "121",
    "122", "133", "134", "135",
}


def die(msg: str, code: int = 1) -> None:
    print(f"FEHLER: {msg}", file=sys.stderr)
    sys.exit(code)


def tid_list(text: str) -> list[str]:
    res: list[str] = []
    # Bereiche wie 072–080 oder 048-054 expandieren
    for m in re.finditer(r"(\d{3})\s*[-–—]\s*(\d{3})", text):
        s, e = int(m.group(1)), int(m.group(2))
        for i in range(s, e + 1):
            res.append(f"{i:03d}")
    for tid in re.findall(r"\b\d{3}\b", text):
        if tid not in res:
            res.append(tid)
    return sorted(res)


# ---------------------------------------------------------------- Dependenzen

def parse_deps() -> dict[str, dict]:
    """Task-ID -> {wave, depends_on, gate} aus DEPENDENCIES.md."""
    if not DEPS.exists():
        die(f"{DEPS.relative_to(ROOT)} nicht gefunden")
    text = DEPS.read_text(encoding="utf-8")
    tasks: dict[str, dict] = {}

    wave_row = re.compile(r"^\|\s*(W\d+[a-z]?)\s*—\s*[^|]+\|\s*([^|]+)\|\s*([^|]+)\|")
    for line in text.splitlines():
        m = wave_row.match(line)
        if not m:
            continue
        wave, task_col, pred_col = m.group(1), m.group(2), m.group(3)
        wave_tasks = [t for t in tid_list(task_col)]
        base: list[str] = []
        extras: dict[str, list[str]] = {}
        for stmt in pred_col.split(";"):
            stmt = stmt.strip()
            if not stmt:
                continue
            if "zusätzlich" in stmt:
                left, _, right = stmt.partition("zusätzlich")
                for t in tid_list(left):
                    extras.setdefault(t, []).extend(tid_list(right))
            else:
                base.extend(tid_list(stmt))
        for tid in wave_tasks:
            deps = base + extras.get(tid, [])
            deps = sorted({d for d in deps if d != tid})
            if tid in tasks:
                tasks[tid]["depends_on"] = sorted(set(tasks[tid]["depends_on"]) | set(deps))
                continue
            tasks[tid] = {"wave": wave, "depends_on": deps, "gate": False}

    # Explizite Sperrkanten: "- A → B", "- A + B → C und D (Anmerkung)", verkettete Pfeile A → B → C
    section = text.split("## Explizite Sperrkanten")
    if len(section) > 1:
        edge_text = section[1].split("\n## ")[0]
        for line in edge_text.splitlines():
            for stmt in line.strip().lstrip("-").strip().split(";"):
                if "→" not in stmt:
                    continue
                parts = [s.strip() for s in stmt.split("→") if s.strip()]
                for i in range(len(parts) - 1):
                    left_ids = tid_list(parts[i])
                    right_ids = [t for t in tid_list(parts[i + 1].split("(")[0]) if t not in left_ids]
                    for tgt in right_ids:
                        if tgt not in tasks:
                            continue
                        for dep in left_ids:
                            if dep in tasks and dep != tgt and dep not in tasks[tgt]["depends_on"]:
                                tasks[tgt]["depends_on"].append(dep)

    for tid, extra in EXTRA_DEPS.items():
        if tid in tasks:
            for dep in extra:
                if dep in tasks and dep not in tasks[tid]["depends_on"]:
                    tasks[tid]["depends_on"].append(dep)

    for tid, t in tasks.items():
        if t["wave"] in GATE_WAVES or tid in GATE_IDS:
            t["gate"] = True
    return tasks


# ---------------------------------------------------------------- Skill-Matrix

def parse_matrix() -> dict[str, list[str]]:
    """Task-ID -> [skill1, skill2] (mit Original-Formatierung aus der Matrix)."""
    if not MATRIX.exists():
        die(f"{MATRIX.relative_to(ROOT)} nicht gefunden")
    skills: dict[str, list[str]] = {}
    for line in MATRIX.read_text(encoding="utf-8").splitlines():
        m = re.match(r"^\|\s*(\d{3})\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|", line)
        if m:
            skills[m.group(1)] = [m.group(2).strip(), m.group(3).strip()]
    return skills


# ---------------------------------------------------------------- Frontmatter

def split_frontmatter(raw: str) -> tuple[str | None, str]:
    if not raw.startswith("---\n"):
        return None, raw
    end = raw.find("\n---\n", 4)
    if end == -1:
        return None, raw
    return raw[4:end], raw[end + 5:]


def extract_title(body: str, fallback: str) -> str:
    m = re.search(r"^#\s*Aufgabe\s+\d+\s*[—-]\s*(.+?)\s*$", body, re.M)
    return m.group(1).replace('"', "'") if m else fallback


def build_frontmatter(old_fm: str | None, *, tid: str, title: str, wave: str,
                      depends_on: list[str], files: list[str], skills: list[str],
                      status: str, gate: bool, done_flag: bool,
                      content_hash: str) -> str:
    lines = ["---"]
    if old_fm:
        for line in old_fm.splitlines():
            key = line.split(":", 1)[0].strip()
            if key and key not in SYNC_KEYS:
                lines.append(line)  # fremde Felder unverändert erhalten
    lines += [
        f'id: "{tid}"',
        f'title: "{title}"',
        f'wave: "{wave}"',
        f"depends_on: [{', '.join(depends_on)}]",
        f"files: [{', '.join(files)}]",
        f"skills: [{', '.join(skills)}]",
        f"status: {status}",
        f"gate: {'true' if gate else 'false'}",
        f"done_since_last_edit: {'true' if done_flag else 'false'}",
        f'content-hash: "{content_hash}"',
        "---",
    ]
    return "\n".join(lines)


def normalize_body(body: str) -> str:
    """Skill-Sektion vereinheitlichen."""
    body = re.sub(r"^##\s+Skills?\s*\(aus skill-matrix\.md\)\s*$",
                  f"## {SKILLS_SECTION_TITLE}", body, flags=re.M)
    body = re.sub(r"^##\s+Skills?\s*$", f"## {SKILLS_SECTION_TITLE}", body, flags=re.M)
    if f"## {SKILLS_SECTION_TITLE}" not in body:
        body = body.rstrip("\n") + f"\n\n## {SKILLS_SECTION_TITLE}\n\n{SKILL_NOTE}\n"
    elif SKILL_NOTE not in body:
        body = re.sub(
            rf"(## {re.escape(SKILLS_SECTION_TITLE)}\n)",
            r"\1\n" + SKILL_NOTE + "\n",
            body, count=1,
        )
    return body


def update_readme(done_count: int, total_count: int) -> None:
    """Aktualisiert Status-Badge und W0-Zustand im README.md."""
    readme_path = ROOT / "README.md"
    if not readme_path.exists():
        return
    text = readme_path.read_text(encoding="utf-8")
    color = "red" if done_count == 0 else "yellow" if done_count < (total_count // 2) else "blue" if done_count < total_count else "success"
    badge = f"![Status](https://img.shields.io/badge/App--Code-{done_count}%20%2F%20{total_count}%20erledigt-{color})"
    new_text = re.sub(r"!\[Status\]\(https://img\.shields\.io/badge/App--Code-[^\)]+\)", badge, text)
    if done_count >= 4:
        new_text = re.sub(
            r"\|\s*W0–W2\s*·\s*Grundlagen,\s*Machbarkeit,\s*Geräteprüfung\s*\|\s*001–008\s*\|\s*[^|]+\|",
            f"| W0–W2 · Grundlagen, Machbarkeit, Geräteprüfung | 001–008 | 🟡 {done_count} / 8 erledigt (W0 abgeschlossen) |",
            new_text,
        )
    if new_text != text:
        readme_path.write_text(new_text, encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description="Task-Frontmatter synchronisieren")
    ap.add_argument("--check", action="store_true", help="nur prüfen, nichts schreiben")
    ap.add_argument("--status", action="append", default=[], metavar="ID=STATUS",
                    help=f"Status setzen, STATUS in {STATUS_VALUES}")
    ap.add_argument("--ready", "--next", action="store_true", dest="show_ready",
                    help="früheste ausführbare Aufgaben auflisten")
    args = ap.parse_args()

    overrides: dict[str, str] = {}
    for item in args.status:
        tid, _, status = item.partition("=")
        if not re.fullmatch(r"\d{3}", tid) or status not in STATUS_VALUES:
            die(f"--status braucht ID=STATUS mit STATUS in {STATUS_VALUES}, bekommen: {item!r}")
        overrides[tid] = status

    deps = parse_deps()
    matrix = parse_matrix()
    task_files = sorted(TASKS.glob("[0-9][0-9][0-9]-*.md"))
    if len(task_files) != 135:
        die(f"Erwartet 135 Task-Dateien, gefunden: {len(task_files)}")

    problems: list[str] = []
    changed = 0
    task_records: dict[str, dict] = {}

    for path in task_files:
        tid = path.name[:3]
        if tid not in deps:
            problems.append(f"{path.name}: fehlt in DEPENDENCIES.md")
            continue
        info = deps[tid]
        skills = matrix.get(tid)
        if not skills:
            problems.append(f"{path.name}: keine Zeile in skill-matrix.md")
            continue

        raw = path.read_text(encoding="utf-8")
        old_fm, body = split_frontmatter(raw)
        body = normalize_body(body)

        old_status, old_hash = "", ""
        old_files: list[str] = []
        if old_fm is not None:
            m = re.search(r"^status:\s*(\S+)", old_fm, re.M)
            old_status = m.group(1) if m else ""
            m = re.search(r'^content-hash:\s*"([0-9a-f]+)"', old_fm, re.M)
            old_hash = m.group(1) if m else ""
            m = re.search(r"^files:\s*\[(.*)\]\s*$", old_fm, re.M)
            if m and m.group(1).strip():
                old_files = [x.strip() for x in m.group(1).split(",") if x.strip()]

        current_hash = hashlib.sha256(body.encode()).hexdigest()[:16]

        # "done" verfällt, wenn der Körper nach dem done-Eintrag geändert wurde
        if old_status == "done" and old_hash and old_hash != current_hash and tid not in overrides:
            old_status = "in_progress"

        status = overrides.get(tid) or (old_status if old_status in STATUS_VALUES else "pending")

        # Single-pass done: Wenn per Override auf done gesetzt oder bereits done mit passendem Hash
        done_flag = False
        if status == "done":
            done_flag = (tid in overrides) or (old_status == "done" and old_hash == current_hash)

        content_hash = current_hash
        files = old_files or [f"tasks/{path.name}"]
        title = extract_title(body, path.stem)

        task_records[tid] = {
            "id": tid,
            "title": title,
            "wave": info["wave"],
            "depends_on": info["depends_on"],
            "skills": skills,
            "status": status,
            "gate": info["gate"],
        }

        new_fm = build_frontmatter(
            old_fm, tid=tid, title=title, wave=info["wave"],
            depends_on=info["depends_on"], files=files, skills=skills,
            status=status, gate=info["gate"], done_flag=done_flag,
            content_hash=content_hash,
        )
        new_raw = new_fm + "\n" + body
        if new_raw != raw:
            if args.check:
                problems.append(f"{path.name}: Frontmatter/Skill-Sektion weicht ab")
            else:
                path.write_text(new_raw, encoding="utf-8")
                changed += 1

    # Gegenprobe: Matrix- und Deps-Einträge ohne Datei
    file_ids = {p.name[:3] for p in task_files}
    for tid in sorted(set(deps) - file_ids):
        problems.append(f"Task {tid}: in DEPENDENCIES.md, aber ohne Datei")
    for tid in sorted(set(matrix) - file_ids):
        problems.append(f"Task {tid}: in skill-matrix.md, aber ohne Datei")

    if args.show_ready:
        pending = [t for t in task_records.values() if t["status"] == "pending"]
        ready = [t for t in pending if all(task_records.get(d, {}).get("status") == "done" for d in t["depends_on"])]
        ready.sort(key=lambda x: (x["wave"], x["id"]))
        print(f"Bereit zur Ausführung ({len(ready)} Aufgabe(n), {len(pending)} offen):")
        for r in ready:
            print(f"- ID {r['id']} ({r['wave']}): {r['title']} [Skills: {', '.join(r['skills'])}]")
        return 0

    if not args.check:
        done_total = sum(1 for t in task_records.values() if t["status"] == "done")
        update_readme(done_total, len(task_files))

    if args.check:
        if problems:
            for p in problems:
                print(f"- {p}")
            die(f"{len(problems)} Abweichung(en) — 'python3 tools/sync_frontmatter.py' ausführen")
        print("OK: alle 135 Task-Dateien sind konsistent.")
        return 0

    for p in problems:
        print(f"- {p}")
    print(f"{changed} Datei(en) aktualisiert.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
