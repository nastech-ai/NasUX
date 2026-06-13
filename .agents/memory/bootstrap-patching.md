---
name: Bootstrap patching
description: How NasUX bootstrap ZIPs are patched and what limits exist.
---

## Rule
Run `python3 scripts/patch-bootstrap.py --all` from /tmp/bootstrap-check source ZIPs whenever bootstrap needs rebranding. Output goes to app/src/main/cpp/. Update checksums in build.gradle after each run.

**Why:** Bootstrap ZIPs come from upstream with hardcoded `com.termux` everywhere. Patcher handles 4 passes: text content, ELF binary string tables (same-length byte pairs), ZIP entry names, GPG keyring replacement.

## Hard limit
130 ELF binary files per arch still contain compiled-in `termux` symbol names in `.symtab`/`.debug_str`. These are physically impossible to change without rebuilding packages from source. They do NOT affect runtime — LD_LIBRARY_PATH is set to NasUX prefix in NasUXShellEnvironment.java.

## Allowed termux references (brand enforcer exceptions)
- `.github/workflows/nastech-brand-enforce.yml` — scans FOR termux
- `.github/scripts/nastech_branding_enforcer.py` — contains search patterns
- `scripts/patch-bootstrap.py` — the patcher tool itself (in SKIP_FILES)
- `BRANDING.md`, `CHANGELOG.md` — historical reference

## ELF binary patching pairs (same-length, safe)
- `com.termux` (10) → `nastech.ai` (10)
- `termux.org` (10) → `nastech.io` (10)
- `termux.dev` (10) → `nastech.io` (10)
- `Termux\0` (7) → `NasUX\0\0` (7)
- `termux\0` (7) → `nasux\0\0` (7)
