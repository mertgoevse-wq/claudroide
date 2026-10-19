#!/usr/bin/env python3
"""Install the debug APK on a running emulator, walk the main screens and
save one screenshot per step.

CI runs this in .github/workflows/android.yml (job `ui-smoke`). Steps find
their target in the live accessibility tree (`uiautomator dump`), never by
fixed coordinates, so a renamed label shows up as a failed step instead of a
tap into nothing. Each step also keeps its tree dump, which is how selectors
get repaired.

Exit codes: 0 all steps passed, 1 a step failed (only with --strict),
2 the app crashed or never started.
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "org.claudroide.next.debug"
ACTIVITY = "org.claudroide.app.MainActivity"
BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")

# A step either launches the app, taps the first node whose content
# description, text or test tag matches `tap`, types text, or presses back.
# Patterns cover English and German labels.
FLOW: list[dict] = [
    {"name": "start", "launch": True},
    {"name": "drawer", "tap": r"^(Open navigation|Navigation öffnen|Menu|Menü|Chats)$"},
    {"name": "projects", "tap": r"^(Projects|Projekte)$"},
    {"name": "drawer-again", "back": True, "then_tap": r"^(Open navigation|Navigation öffnen|Menu|Menü|Chats)$"},
    {"name": "settings", "tap": r"^(Settings|Einstellungen)$"},
    {"name": "back-to-chat", "back": True},
    {"name": "composer-typed", "type": "Hello from the emulator smoke test", "into": r"(Reply to|Antwort an|Message|Nachricht)"},
    {"name": "model-picker", "tap": r"(Model|Modell|claude|sonnet|opus|haiku)"},
    {"name": "after-model-picker", "back": True},
    {"name": "attachments", "tap": r"^(Add attachment|Anhang hinzufügen)$"},
]


def adb(*args: str, check: bool = True, timeout: int = 60) -> str:
    result = subprocess.run(["adb", *args], capture_output=True, text=True, timeout=timeout)
    if check and result.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)}: {result.stderr.strip()}")
    return result.stdout


def screenshot(path: Path) -> None:
    with path.open("wb") as fh:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=fh, check=True, timeout=30)


def dump_tree(path: Path) -> ET.Element | None:
    # uiautomator refuses to dump while the UI is still animating; retry.
    for _ in range(6):
        out = subprocess.run(
            ["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"],
            capture_output=True, text=True, timeout=30,
        )
        if "dumped" in out.stdout:
            xml = adb("shell", "cat", "/sdcard/ui.xml")
            path.write_text(xml, encoding="utf-8")
            try:
                return ET.fromstring(xml)
            except ET.ParseError:
                pass
        time.sleep(1.5)
    return None


def find(root: ET.Element, pattern: str) -> ET.Element | None:
    rx = re.compile(pattern, re.IGNORECASE)
    for node in root.iter("node"):
        for attr in ("content-desc", "text", "resource-id"):
            value = node.get(attr) or ""
            if value and rx.search(value):
                return node
    return None


def tap(node: ET.Element) -> None:
    x1, y1, x2, y2 = map(int, BOUNDS.match(node.get("bounds", "")).groups())
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def app_alive() -> bool:
    return bool(adb("shell", "pidof", PACKAGE, check=False).strip())


def launch() -> None:
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/{ACTIVITY}", timeout=90)


def settle(seconds: float = 2.0) -> None:
    time.sleep(seconds)


def run_flow(out: Path, label: str) -> tuple[list[dict], bool]:
    shots = out / label
    shots.mkdir(parents=True, exist_ok=True)
    results: list[dict] = []
    crashed = False
    for index, step in enumerate(FLOW, start=1):
        name = f"{index:02d}-{step['name']}"
        record = {"step": name, "ok": True, "note": ""}
        try:
            if step.get("launch"):
                launch()
                settle(4)
            if step.get("back"):
                adb("shell", "input", "keyevent", "KEYCODE_BACK")
                settle()
            pattern = step.get("tap") or step.get("then_tap") or step.get("into")
            if pattern:
                root = dump_tree(shots / f"{name}-before.xml")
                node = find(root, pattern) if root is not None else None
                if node is None:
                    record.update(ok=False, note=f"no node matches /{pattern}/")
                else:
                    tap(node)
                    settle()
                    if step.get("type"):
                        adb("shell", "input", "text", step["type"].replace(" ", "%s"))
                        settle(1)
        except Exception as exc:  # keep walking; the report says what broke
            record.update(ok=False, note=f"{type(exc).__name__}: {exc}")
        if not app_alive():
            record.update(ok=False, note=(record["note"] + " | app process gone").strip(" |"))
            crashed = True
        screenshot(shots / f"{name}.png")
        dump_tree(shots / f"{name}.xml")
        results.append(record)
        print(("PASS " if record["ok"] else "FAIL ") + f"{label}/{name} {record['note']}", flush=True)
        if crashed:
            break
    return results, crashed


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--apk", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--strict", action="store_true", help="fail on any unmatched step")
    args = parser.parse_args()

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    adb("install", "-r", "-g", args.apk, timeout=180)
    adb("logcat", "-c", check=False)

    report: dict[str, list[dict]] = {}
    crashed = False
    for label, night in (("light", "no"), ("dark", "yes")):
        adb("shell", "cmd", "uimode", "night", night, check=False)
        results, crashed = run_flow(out, label)
        report[label] = results
        if crashed:
            break

    (out / "logcat-crash.txt").write_text(adb("logcat", "-d", "-b", "crash", check=False), encoding="utf-8")
    (out / "logcat-app.txt").write_text(
        adb("logcat", "-d", "-v", "time", "*:W", check=False), encoding="utf-8"
    )
    (out / "report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")

    if crashed:
        print("::error::ClauDroide crashed during the smoke flow; see logcat-crash.txt", flush=True)
        return 2
    failed = [r for runs in report.values() for r in runs if not r["ok"]]
    for r in failed:
        print(f"::warning::smoke step {r['step']} failed: {r['note']}", flush=True)
    return 1 if (failed and args.strict) else 0


if __name__ == "__main__":
    sys.exit(main())
