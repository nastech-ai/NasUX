#!/usr/bin/env python3
"""
NasTech Branding Enforcer v2.0
================================
Comprehensive Termux→NasUX replacement bot for nastech-ai repos.

• Scans ALL eligible files (not just .github/ and sample/)
• Applies ordered, case-aware replacements
• Protects external strings that cannot be renamed (CDN URLs, bootstrap env vars)
• Generates a detailed per-file, per-line change report
• Outputs GitHub Actions step outputs for conditional workflow steps
"""

import os, re, sys, json
from pathlib import Path
from datetime import datetime, timezone

# ── Configuration ────────────────────────────────────────────────────────────

REPO_ROOT = Path(os.getcwd())
REPO_NAME = os.environ.get("GITHUB_REPOSITORY", "nastech-ai/unknown")
TRIGGER   = os.environ.get("GITHUB_EVENT_NAME", "manual")
REF       = os.environ.get("GITHUB_REF_NAME", "master")
RUN_URL   = (
    f"https://github.com/{REPO_NAME}/actions/runs/"
    + os.environ.get("GITHUB_RUN_ID", "0")
)
NOW       = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

GITHUB_OUTPUT  = os.environ.get("GITHUB_OUTPUT",       "/dev/null")
GITHUB_SUMMARY = os.environ.get("GITHUB_STEP_SUMMARY", "/dev/null")
REPORT_PATH    = Path("/tmp/branding_report.md")

# Directories to skip entirely
SKIP_DIRS = {
    ".git", ".github/workflows", "packages", "scripts/build", "node_modules",
    "__pycache__", ".gradle", "build", ".idea", ".local",
    "gradle", "app/build", "nasux-shared/build",
    "terminal-emulator/build", "terminal-view/build",
}

# Files to never modify (bot should not self-modify or corrupt its own patterns)
SKIP_FILES = {
    "nastech_branding_enforcer.py",   # this script
    "nastech_branding_enforcer.yml",  # this workflow
    "nastech_branding_bot.py",        # old bot script (regex patterns inside)
    "nastech_branding_bot.yml",       # old bot workflow (patterns inside)
    "BRANDING.md",                    # branding reference map — lists old names intentionally
    "CHANGELOG.md",                   # historical log — may reference old names
    "patch-bootstrap.py",             # patcher tool — contains termux as search patterns
}

# File extensions eligible for scanning
SCAN_EXTS = {
    ".yml", ".yaml", ".sh", ".md", ".py", ".java", ".c", ".h",
    ".mk", ".gradle", ".txt", ".xml", ".properties", ".json",
    ".kt", ".kts", ".rb", ".go", ".ts", ".js",
}

# ── Protected strings (mask before replacing, restore after) ─────────────────
# These are external identifiers we don't own and cannot rename.
PROTECTED = [
    "packages.termux.dev",           # External APT CDN — not owned by NasTech
    "TERMUX_VERSION",                # Android bootstrap env var (external)
    "TERMUX_APP_PACKAGE_NAME",       # Android bootstrap env var (external)
    "com.termux/files",              # External Android data path (detection only)
    "/data/data/com.termux",         # External Android data dir (detection only)
    "termux-exec",                   # External LD_PRELOAD binary (kernel-level)
    "termux-compat",                 # Compat shim binary name
    "nastech_branding_bot.py",       # Old bot — don't rename its references
]

# ── Ordered replacements (most-specific → least-specific) ────────────────────
# Each entry: (regex_pattern, replacement, human_label)
REPLACEMENTS = [
    # ── GitHub org / repo ──────────────────────────────────────────────────
    (r"termux/termux-packages",
     "nastech-ai/NasUX-Packages",                 "GitHub repo path"),
    (r"termux/termux-app",
     "nastech-ai/NasUX",                          "GitHub app repo"),
    (r"ghcr\.io/termux/package-builder",
     "ghcr.io/nastech-ai/nasux-package-builder",  "GHCR image"),
    (r"grimler/package-builder",
     "nastech-ai/nasux-package-builder",           "Docker Hub image"),
    (r"tag termux/package-builder",
     "tag nastech-ai/nasux-package-builder",       "Docker tag"),
    (r"users/termux/packages/container/package-builder",
     "users/nastech-ai/packages/container/nasux-package-builder",
     "GHCR API path"),

    # ── Identity / contact ─────────────────────────────────────────────────
    (r"Termux Github Actions",
     "NasTech Bot",                               "bot git name"),
    (r"contact@termux\.dev",
     "nastech-bot@users.noreply.github.com",       "bot email"),
    (r"TERMUXBOT2_TOKEN",
     "NASTECH_BOT_TOKEN",                         "bot token"),
    (r"@termux(?=[\"'\s,\)]|$)",
     "@nastech-ai",                               "maintainer handle"),

    # ── URLs ───────────────────────────────────────────────────────────────
    (r"https://termux\.dev/donate",
     "https://github.com/sponsors/nastech-ai",    "funding URL"),
    (r"github\.com/termux/termux-packages/wiki",
     "github.com/nastech-ai/NasUX-Packages/wiki", "wiki URL"),
    (r"github\.com/termux/termux-packages/discussions",
     "github.com/nastech-ai/NasUX-Packages/discussions",
     "discussions URL"),
    (r"github\.com/termux/termux-packages/pull",
     "github.com/nastech-ai/NasUX-Packages/pull", "PR URL"),
    (r"github\.com/termux/termux-packages/issues",
     "github.com/nastech-ai/NasUX-Packages/issues", "issues URL"),
    (r"getting-started/termux",
     "getting-started/nasux",                     "docs guide URL"),
    (r"discord\.gg/HXpF69X",
     "github.com/nastech-ai/NasUX/discussions",   "Discord → Discussions"),
    (r"t\.me/termux",
     "github.com/nastech-ai",                     "Telegram link"),
    (r"termux-packages/wiki",
     "NasUX-Packages/wiki",                       "wiki path"),

    # ── Commit messages ────────────────────────────────────────────────────
    (r"auto-fix Termux violations",
     "auto-fix NasUX branding violations",        "bot commit message"),

    # ── CI / build system env vars (OUR own) ──────────────────────────────
    (r"TERMUX_ARCH(?=[^IVES]|$)",
     "NASUX_ARCH",                                "arch env var"),
    (r"TERMUX_SCRIPTDIR",
     "NASUX_SCRIPTDIR",                           "scriptdir env var"),
    (r"TERMUX_PACKAGE_DIRECTORIES",
     "NASUX_PACKAGE_DIRECTORIES",                 "package dirs var"),

    # ── Build-system sample field prefixes ────────────────────────────────
    (r"TERMUX_PKG_MAINTAINER",   "NASUX_PKG_MAINTAINER",  "pkg maintainer"),
    (r"TERMUX_PKG_HOMEPAGE",     "NASUX_PKG_HOMEPAGE",     "pkg homepage"),
    (r"TERMUX_PKG_DESCRIPTION",  "NASUX_PKG_DESCRIPTION",  "pkg description"),
    (r"TERMUX_PKG_LICENSE",      "NASUX_PKG_LICENSE",       "pkg license"),
    (r"TERMUX_PKG_VERSION",      "NASUX_PKG_VERSION",       "pkg version"),
    (r"TERMUX_PKG_SRCURL",       "NASUX_PKG_SRCURL",        "pkg srcurl"),
    (r"TERMUX_PKG_SHA256",       "NASUX_PKG_SHA256",        "pkg sha256"),
    (r"TERMUX_PKG_DEPENDS",      "NASUX_PKG_DEPENDS",       "pkg depends"),
    (r"TERMUX_PKG_BUILD_IN_SRC", "NASUX_PKG_BUILD_IN_SRC",  "pkg build_in_src"),
    (r"TERMUX_PKG_EXCLUDED_ARCHES", "NASUX_PKG_EXCLUDED_ARCHES", "pkg excl arches"),
    (r"TERMUX_SUBPKG_DESCRIPTION", "NASUX_SUBPKG_DESCRIPTION", "subpkg description"),
    (r"TERMUX_SUBPKG_INCLUDE",   "NASUX_SUBPKG_INCLUDE",    "subpkg include"),

    # ── Setup script function names ────────────────────────────────────────
    (r"termux_setup_",           "nasux_setup_",            "setup function"),

    # ── Issue / Docker metadata ────────────────────────────────────────────
    (r"--assignee\s+['\"]?thunder-coding['\"]?",
     "--assignee nastech-ai",                     "issue assignee"),
    (r"(?m)(?<=username:\s)grimler",
     "nastech-ai",                               "Docker Hub username"),
    (r"username:\s*grimler",
     "username: nastech-ai",                      "Docker Hub username (alt)"),

    # ── Documentation / prose ─────────────────────────────────────────────
    (r"Bootstrap archives for Termux application",
     "Bootstrap archives for NasUX application",  "bootstrap title"),
    (r"Termux Developer['\u2019]s Wiki",
     "NasUX Developer's Wiki",                    "wiki label"),
    (r"Termux / Android",  "NasUX / Android",     "platform label"),
    (r"Android / Termux",  "Android / NasUX",     "platform label alt"),
    (r"via Termux",        "via NasUX",            "platform preposition"),
    (r"on Termux",         "on NasUX",             "platform preposition"),
    (r"Termux cold",       "NasUX cold",           "perf label"),
    (r"Termux TUI",        "NasUX TUI",            "TUI label"),
    (r"Termux non-TUI",    "NasUX non-TUI",        "non-TUI label"),
    (r"Termux fast-path",  "NasUX fast-path",      "fast-path label"),
    (r"Termux scrollback", "NasUX scrollback",     "scrollback label"),
    (r"Termux support",    "NasUX support",        "support label"),
    (r"Termux users",      "NasUX users",          "users label"),
    (r"Termux Android",    "NasUX Android",        "android label"),
    (r"Termux gates",      "NasUX gates",          "gates label"),
    (r"termux-info\b",     "nasux-info",           "info command"),
    (r"termux-tools\b",    "nasux-tools",          "tools package"),
    (r"termux-setup-storage\b", "nasux-setup-storage", "storage command"),
    (r"what termux-exec does", "what nasux-exec does", "exec comment"),
    (r"\.\[termux\]",      ".[nasux]",             "Python extra tag"),
    (r"compatible Termux APT mirror",
     "NasUX-compatible APT mirror",              "APT comment"),
    (r"Termux-compatible",  "NasUX-compatible",   "compat adjective"),
    (r"Termux/Android",     "NasUX/Android",      "platform slash"),
    (r"Termux scrollback preservation",
     "NasUX scrollback preservation",            "scrollback feature"),

    # ── Inline package tool comment ────────────────────────────────────────
    (r"internal pkg name: termux-tools",
     "internal pkg name: nasux-tools",           "pkg name comment"),

    # ── Generic fallback — standalone word only (must be LAST) ────────────
    (r"\btermux\b",  "nasux",  "lowercase standalone"),
    (r"\bTermux\b",  "NasUX",  "titlecase standalone"),
    (r"\bTERMUX\b",  "NASUX",  "uppercase standalone"),
]

# ── Helpers ──────────────────────────────────────────────────────────────────

def is_binary(path: Path) -> bool:
    try:
        with open(path, "rb") as f:
            chunk = f.read(8192)
        return b"\x00" in chunk
    except Exception:
        return True

def should_skip(path: Path) -> bool:
    rel = path.relative_to(REPO_ROOT)
    parts = rel.parts
    for skip_dir in SKIP_DIRS:
        if any(p == skip_dir or str(rel).startswith(skip_dir + "/") for p in parts):
            return True
    if path.name in SKIP_FILES:
        return True
    if path.suffix.lower() not in SCAN_EXTS:
        return True
    if is_binary(path):
        return True
    return False

_MASK_PREFIX = "\x00PROTECTED"
_MASK_SUFFIX = "\x00"

def mask_protected(text: str) -> tuple[str, list[str]]:
    saved: list[str] = []
    for s in PROTECTED:
        if s in text:
            idx = len(saved)
            token = f"{_MASK_PREFIX}{idx:04d}{_MASK_SUFFIX}"
            text = text.replace(s, token)
            saved.append(s)
    return text, saved

def unmask_protected(text: str, saved: list[str]) -> str:
    for idx, s in enumerate(saved):
        token = f"{_MASK_PREFIX}{idx:04d}{_MASK_SUFFIX}"
        text = text.replace(token, s)
    return text

# ── Core fix logic ────────────────────────────────────────────────────────────

class FileChange:
    def __init__(self, path: Path):
        self.path = path
        self.rel  = str(path.relative_to(REPO_ROOT))
        self.lines: list[tuple[int, str, str, str]] = []  # (lineno, rule, before, after)

    def add(self, lineno: int, rule: str, before: str, after: str):
        self.lines.append((lineno, rule, before, after))

    @property
    def count(self) -> int:
        return len(self.lines)

def fix_file(path: Path) -> FileChange | None:
    try:
        original = path.read_text(encoding="utf-8", errors="replace")
    except Exception:
        return None

    lines_in  = original.splitlines(keepends=True)
    lines_out = []
    change    = FileChange(path)

    for lineno, line in enumerate(lines_in, 1):
        masked, saved = mask_protected(line)
        modified = masked

        for pattern, replacement, label in REPLACEMENTS:
            new_line = re.sub(pattern, replacement, modified)
            if new_line != modified:
                modified = new_line

        result = unmask_protected(modified, saved)

        if result != line:
            before = line.rstrip("\n")
            after  = result.rstrip("\n")
            # Find which rule(s) fired — report the most impactful one
            for pattern, replacement, label in REPLACEMENTS:
                if re.search(pattern, mask_protected(line)[0]):
                    change.add(lineno, label, before, after)
                    break

        lines_out.append(result)

    new_content = "".join(lines_out)
    if new_content == original:
        return None

    path.write_text(new_content, encoding="utf-8")
    return change if change.count else None

# ── Walk and fix ─────────────────────────────────────────────────────────────

print(f"NasTech Branding Enforcer — scanning {REPO_ROOT}")
print(f"  Protected strings: {len(PROTECTED)}")
print(f"  Replacement rules: {len(REPLACEMENTS)}")
print()

all_changes: list[FileChange] = []
scanned = 0

for fpath in sorted(REPO_ROOT.rglob("*")):
    if not fpath.is_file():
        continue
    if should_skip(fpath):
        continue
    scanned += 1
    result = fix_file(fpath)
    if result and result.count:
        all_changes.append(result)
        print(f"  fixed ({result.count:3d} change{'s' if result.count != 1 else ''}): {result.rel}")

total_changes = sum(c.count for c in all_changes)
print()
print(f"Scanned {scanned} files. Modified {len(all_changes)} files. {total_changes} total changes.")

# ── Build report ─────────────────────────────────────────────────────────────

def escape_md(s: str) -> str:
    return s.replace("|", "\\|").replace("`", "'")

lines: list[str] = []
lines += [
    f"# NasTech Branding Enforcement Report",
    f"",
    f"| | |",
    f"|---|---|",
    f"| **Generated** | {NOW} |",
    f"| **Repository** | `{REPO_NAME}` |",
    f"| **Branch** | `{REF}` |",
    f"| **Trigger** | `{TRIGGER}` |",
    f"| **Run** | [View run]({RUN_URL}) |",
    f"",
    f"---",
    f"",
    f"## Summary",
    f"",
    f"| Metric | Value |",
    f"|--------|-------|",
    f"| Files scanned | **{scanned}** |",
    f"| Files modified | **{len(all_changes)}** |",
    f"| Total line changes | **{total_changes}** |",
    f"| Protected strings preserved | **{len(PROTECTED)}** |",
    f"| Replacement rules applied | **{len(REPLACEMENTS)}** |",
    f"",
]

if not all_changes:
    lines += [
        "## ✅ All Clean",
        "",
        "No branding violations found. Every file is fully NasTech-branded.",
        "",
    ]
else:
    lines += [
        f"## Changed Files ({len(all_changes)})",
        "",
    ]
    for chg in all_changes:
        lines += [
            f"### `{chg.rel}` — {chg.count} change{'s' if chg.count != 1 else ''}",
            "",
            f"| Line | Rule | Before | After |",
            f"|------|------|--------|-------|",
        ]
        for lineno, rule, before, after in chg.lines[:50]:  # cap at 50 rows per file
            lines.append(
                f"| {lineno} | {escape_md(rule)} "
                f"| `{escape_md(before[:80])}` "
                f"| `{escape_md(after[:80])}` |"
            )
        if len(chg.lines) > 50:
            lines.append(f"| … | *(+{len(chg.lines)-50} more)* | | |")
        lines.append("")

lines += [
    "---",
    "",
    f"## Protected Strings (never replaced)",
    "",
    "These external identifiers were preserved unchanged:",
    "",
]
for s in PROTECTED:
    lines.append(f"- `{s}`")

lines += [
    "",
    "---",
    f"*NasTech Branding Enforcer — [nastech-ai]"
    f"(https://github.com/nastech-ai)*",
]

report = "\n".join(lines)
REPORT_PATH.write_text(report, encoding="utf-8")
print(f"\nReport written to {REPORT_PATH}")

# ── GitHub Actions outputs ────────────────────────────────────────────────────

with open(GITHUB_OUTPUT, "a") as fh:
    fh.write(f"fixes_count={total_changes}\n")
    fh.write(f"files_count={len(all_changes)}\n")
    fh.write(f"scanned_count={scanned}\n")

with open(GITHUB_SUMMARY, "a") as fh:
    fh.write(report)

sys.exit(0)
