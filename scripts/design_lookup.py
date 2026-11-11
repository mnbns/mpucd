#!/usr/bin/env python3
"""Exact-ID retrieval over DESIGN.md / RELEASE_NOTES.md.

Usage:
    python scripts/design_lookup.py <ID> [<ID> ...]
    e.g. python scripts/design_lookup.py US-05 DD-02 L2-FR-07 AC-05.1

Every requirement, user story, decision, and diagram in this repo's design
docs already carries a stable ID (US-xx, AC-xx.y, L2-FR-xx, NFR-xx, DD-xx,
HLD-01, LLD-01, FC-0x, SEQ-01). Exact-ID lookup is more reliable than a
vector/embedding search for this shape of document, so this script just
greps the right section out by structure and prints it verbatim. Agents
should call this before touching code or DESIGN.md, instead of relying on
whatever happens to already be in context.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DESIGN_MD = ROOT / "DESIGN.md"
RELEASE_NOTES_MD = ROOT / "RELEASE_NOTES.md"

ID_RE = re.compile(r"^(US|AC|L2-FR|NFR|DD|FC|HLD|LLD|SEQ)-?(\d+)(?:\.(\d+))?$", re.I)


def load(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


def section_by_heading(text: str, needle: str) -> str | None:
    """Block starting at the first heading (##, ###, or ####) containing
    `needle`, up to the next heading of equal or shallower depth."""
    lines = text.splitlines()
    start = start_level = None
    for i, line in enumerate(lines):
        m = re.match(r"^(#{2,4})\s.*" + re.escape(needle), line)
        if m:
            start, start_level = i, len(m.group(1))
            break
    if start is None:
        return None
    end = len(lines)
    for j in range(start + 1, len(lines)):
        m = re.match(r"^(#{2,4})\s", lines[j])
        if m and len(m.group(1)) <= start_level:
            end = j
            break
    return "\n".join(lines[start:end]).strip()


def dd_bullet(text: str, num: int) -> str | None:
    pat = re.compile(rf"- \*\*DD-{num:02d}\*\*.*?(?=\n- \*\*DD-\d|\n## |\Z)", re.S)
    m = pat.search(text)
    return m.group(0).strip() if m else None


def ac_line(text: str, us_num: int, ac_num: int) -> str | None:
    pat = re.compile(rf"^- \*\*AC-{us_num:02d}\.{ac_num}\*\*.*$", re.M)
    m = pat.search(text)
    return m.group(0).strip() if m else None


def table_rows(text: str, id_: str) -> list[str]:
    pat = re.compile(rf"^\|\s*{re.escape(id_)}\s*\|.*$", re.M)
    return [m.group(0).strip() for m in pat.finditer(text)]


def grep_context(text: str, id_: str, before: int = 1, after: int = 1) -> list[str]:
    lines = text.splitlines()
    hits = []
    for i, line in enumerate(lines):
        if id_ in line:
            lo, hi = max(0, i - before), min(len(lines), i + after + 1)
            hits.append("\n".join(lines[lo:hi]))
    return hits


def lookup(id_raw: str) -> str:
    id_ = id_raw.strip().upper()
    m = ID_RE.match(id_)
    design = load(DESIGN_MD)
    release = load(RELEASE_NOTES_MD)
    out = [f"### {id_}"]

    if not m:
        out.append(f"(unrecognized ID format: {id_raw})")
        return "\n".join(out)

    kind, num, sub = m.group(1).upper(), int(m.group(2)), m.group(3)

    if kind == "US":
        block = section_by_heading(design, f"US-{num:02d} ")
        out.append(block or f"(no US-{num:02d} section found in DESIGN.md)")
    elif kind == "AC" and sub:
        line = ac_line(design, num, int(sub))
        out.append(line or f"(no AC-{num:02d}.{sub} found in DESIGN.md)")
    elif kind == "DD":
        block = dd_bullet(design, num)
        out.append(block or f"(no DD-{num:02d} found in DESIGN.md)")
    elif kind in ("FC", "HLD", "LLD", "SEQ"):
        heading_id = f"{kind}-{num:02d}"
        block = section_by_heading(design, f"({heading_id})") or section_by_heading(design, heading_id)
        out.append(block or f"(no {heading_id} section found in DESIGN.md)")
    elif kind in ("L2-FR", "NFR"):
        canonical = f"{kind}-{num:02d}" if kind == "L2-FR" else f"NFR-{num:02d}"
        rows = table_rows(design, canonical)
        out.extend(rows or [f"(no table rows found for {canonical})"])
        mentions = [c for c in grep_context(design, canonical, before=0, after=0) if "|" not in c]
        if mentions:
            out.append("-- mentioned in --")
            out.extend(mentions)
    else:
        out.append(f"(unhandled ID kind: {kind})")

    defect_hits = grep_context(release, id_, before=0, after=0)
    if defect_hits:
        out.append("-- RELEASE_NOTES.md mentions --")
        out.extend(defect_hits)

    return "\n".join(out)


def main() -> None:
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    print(f"\n{'=' * 70}\n".join(lookup(a) for a in sys.argv[1:]))


if __name__ == "__main__":
    main()
