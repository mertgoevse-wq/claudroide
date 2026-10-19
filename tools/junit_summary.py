#!/usr/bin/env python3
"""Count JUnit XML results and fail when a run proves nothing.

Gradle prints BUILD SUCCESSFUL for an UP-TO-DATE run that executed no test at
all; that happened in sessions 14-16 and was reported as green. This script
reads the XML files themselves: zero files, zero tests, or any failure is red.

Usage: python3 tools/junit_summary.py app/build/test-results/testDebugUnitTest
"""
from __future__ import annotations

import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def main(directory: str) -> int:
    files = sorted(Path(directory).glob("TEST-*.xml"))
    if not files:
        print(f"::error::no JUnit XML under {directory}; the test task produced nothing")
        return 1
    tests = failures = errors = skipped = 0
    broken: list[str] = []
    for path in files:
        root = ET.parse(path).getroot()
        tests += int(root.get("tests", 0))
        failures += int(root.get("failures", 0))
        errors += int(root.get("errors", 0))
        skipped += int(root.get("skipped", 0))
        for case in root.iter("testcase"):
            if case.find("failure") is not None or case.find("error") is not None:
                broken.append(f"{case.get('classname')} > {case.get('name')}")

    lines = [
        "| Suites | Tests | Failures | Errors | Skipped |",
        "|---:|---:|---:|---:|---:|",
        f"| {len(files)} | {tests} | {failures} | {errors} | {skipped} |",
    ]
    if broken:
        lines += ["", "**Failing tests**", ""] + [f"- `{b}`" for b in broken[:50]]
    summary = "\n".join(lines)
    print(summary)
    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as fh:
            fh.write("### Unit tests\n\n" + summary + "\n")

    if tests == 0:
        print("::error::0 tests executed; that proves nothing")
        return 1
    if failures or errors:
        print(f"::error::{failures + errors} failing tests")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "app/build/test-results/testDebugUnitTest"))
