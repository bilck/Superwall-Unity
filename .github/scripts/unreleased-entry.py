#!/usr/bin/env python3
"""Add a bullet to a subheading of the CHANGELOG.md "## [Unreleased]" section.

Usage: unreleased-entry.py <changelog-path> <subheading> <bullet> [replace-prefix]

Bullets use the "* " marker this changelog already uses. A second bump of the
same platform replaces the bullet that starts with replace-prefix instead of
adding another one. package.json is left alone: cutting a version out of
[Unreleased] is a separate release.
"""
import sys

UNRELEASED = "## [Unreleased]"


def main():
    path, subheading, bullet = sys.argv[1:4]
    replace_prefix = sys.argv[4] if len(sys.argv) > 4 else None
    with open(path, encoding="utf-8") as f:
        lines = f.read().split("\n")
    bullet_line = bullet if bullet.startswith("* ") else f"* {bullet}"

    start = next((i for i, line in enumerate(lines) if line.strip() == UNRELEASED), None)
    if start is None:
        insert_at = next((i for i, line in enumerate(lines) if line.startswith("## ")), len(lines))
        lines[insert_at:insert_at] = [UNRELEASED, "", subheading, bullet_line, ""]
        return write(path, lines)

    end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith("## ")), len(lines))

    if replace_prefix:
        stale = next(
            (i for i in range(start + 1, end) if lines[i].startswith(f"* {replace_prefix}")),
            None,
        )
        if stale is not None:
            lines[stale] = bullet_line
            return write(path, lines)

    sub = next((i for i in range(start + 1, end) if lines[i].strip() == subheading), None)
    if sub is None:
        # Sit above the next version heading, after whatever is already unreleased.
        block = [subheading, bullet_line, ""]
        at = end
        if at > start + 1 and lines[at - 1] != "":
            block.insert(0, "")
        lines[at:at] = block
    else:
        j = sub + 1
        while j < end and (lines[j].startswith("* ") or lines[j].startswith("  ")):
            j += 1
        lines.insert(j, bullet_line)

    write(path, lines)


def write(path, lines):
    text = "\n".join(lines)
    if not text.endswith("\n"):
        text += "\n"
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


if __name__ == "__main__":
    main()
