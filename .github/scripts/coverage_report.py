#!/usr/bin/env python3
"""Build a Markdown summary with total and PR-added-line coverage."""

from __future__ import annotations

import os
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path


REPORT = Path("target/site/jacoco/jacoco.xml")
SUMMARY = Path("coverage-summary.md")
HUNK = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@")


def percent(covered: int, total: int) -> str:
    return "N/A" if total == 0 else f"{covered / total * 100:.2f}%"


def added_lines(base_sha: str, head_sha: str) -> dict[str, set[int]]:
    diff = subprocess.run(
        [
            "git",
            "diff",
            "--unified=0",
            "--no-ext-diff",
            f"{base_sha}...{head_sha}",
            "--",
            "src/main/java",
        ],
        check=True,
        capture_output=True,
        text=True,
    ).stdout

    result: dict[str, set[int]] = {}
    current_path: str | None = None

    for line in diff.splitlines():
        if line.startswith("+++ "):
            path = line[4:]
            current_path = path[2:] if path.startswith("b/") else None
            continue

        match = HUNK.match(line)
        if current_path and match:
            start = int(match.group(1))
            length = int(match.group(2) or "1")
            result.setdefault(current_path, set()).update(range(start, start + length))

    return result


def main() -> None:
    if not REPORT.exists():
        raise SystemExit(f"JaCoCo report not found: {REPORT}")

    root = ET.parse(REPORT).getroot()
    line_counter = next(
        counter for counter in root.findall("counter") if counter.get("type") == "LINE"
    )
    total_missed = int(line_counter.get("missed", "0"))
    total_covered = int(line_counter.get("covered", "0"))

    changed = added_lines(os.environ["BASE_SHA"], os.environ["HEAD_SHA"])
    new_covered = 0
    new_total = 0

    for package in root.findall("package"):
        package_name = package.get("name", "")
        for source in package.findall("sourcefile"):
            source_path = f"src/main/java/{package_name}/{source.get('name')}"
            changed_in_file = changed.get(source_path, set())
            if not changed_in_file:
                continue

            for line in source.findall("line"):
                number = int(line.get("nr", "0"))
                if number not in changed_in_file:
                    continue
                new_total += 1
                if int(line.get("ci", "0")) > 0:
                    new_covered += 1

    total_lines = total_covered + total_missed
    new_note = "" if new_total else " (nenhuma linha executável adicionada)"
    markdown = f"""## Test coverage

| Escopo | Cobertura de linhas | Linhas cobertas / executáveis |
| --- | ---: | ---: |
| Total | **{percent(total_covered, total_lines)}** | {total_covered} / {total_lines} |
| Código novo | **{percent(new_covered, new_total)}**{new_note} | {new_covered} / {new_total} |

_Código novo considera linhas executáveis adicionadas ou alteradas neste PR. Este relatório é informativo e não aplica quality gate._
"""
    SUMMARY.write_text(markdown, encoding="utf-8")
    print(markdown)


if __name__ == "__main__":
    main()
