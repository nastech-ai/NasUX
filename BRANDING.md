# NasTech / NasUX Branding Guide

> **Rule #1**: Everything in this repository is **NasUX**, powered by **NasTech AI**.
> Every identifier, comment, label, URL, and string must use NasUX / NasTech naming.
> The automated Branding Enforcer bot catches and fixes violations on every push.

---

## Table of Contents

1. [Quick Reference](#quick-reference)
2. [Names & Identifiers](#names--identifiers)
3. [Package Build Fields](#package-build-fields)
4. [CI / GitHub Actions](#ci--github-actions)
5. [Scripts & Functions](#scripts--functions)
6. [URLs & Links](#urls--links)
7. [Prose & Documentation](#prose--documentation)
8. [Protected Strings](#protected-strings)
9. [Automated Enforcement](#automated-enforcement)

---

## Quick Reference

| Concept | ✅ Correct NasUX Name |
|---|---|
| App | `NasUX` |
| Lowercase identifier | `nasux` |
| Uppercase constant prefix | `NASUX` |
| Packages repo | `nastech-ai/NasUX-Packages` |
| App repo | `nastech-ai/NasUX` |
| Docker image | `ghcr.io/nastech-ai/nasux-package-builder` |
| CI bot name | `NasTech Bot` |
| CI bot email | `nastech-bot@users.noreply.github.com` |
| CI secret | `NASTECH_BOT_TOKEN` |
| GitHub org | `@nastech-ai` |
| Package build variable prefix | `NASUX_PKG_*` |
| Sub-package variable prefix | `NASUX_SUBPKG_*` |
| Architecture variable | `NASUX_ARCH` |
| Script directory variable | `NASUX_SCRIPTDIR` |
| Setup function prefix | `nasux_setup_*` |
| Core tools package | `nasux-tools` |
| Info command | `nasux-info` |
| Storage setup command | `nasux-setup-storage` |
| Platform references | `via NasUX` / `on NasUX` / `Android / NasUX` |
| Python extras key | `.[nasux]` |

---

## Names & Identifiers

### App / Project Names

| Concept | Correct Name |
|---|---|
| The app | **NasUX** |
| The AI agent | **NasTech AI** / **NasTech Agent** |
| The company / org | **NasTech AI** |
| GitHub org | `nastech-ai` |
| App package | `com.nastech.nasux` |
| Main repo | `nastech-ai/NasUX` |
| Packages repo | `nastech-ai/NasUX-Packages` |

### Java / Kotlin Identifiers

```java
// ✅ Correct
package com.nastech.nasux;
class NasUXInstaller { ... }
NasUXConstants.NASUX_PREFIX_DIR_PATH
```

### Python Identifiers

```python
# ✅ Correct
def is_nasux() -> bool: ...
def is_android_terminal() -> bool: ...   # detects any Android terminal
def is_nasux_proot_distro() -> bool: ...
_nasux_cache: bool | None = None
_nasux_terminal_cache: bool | None = None
```

### Shell Variables & Functions

```bash
# ✅ Correct
NASUX_ARCH=aarch64
NASUX_SCRIPTDIR="$(realpath "$(dirname "$0")/../..")"
nasux_setup_golang() { ... }
nasux_setup_rust() { ... }
```

---

## Package Build Fields

All package build scripts (`build.sh`) use **`NASUX_PKG_*`** fields:

```bash
# ✅ Correct — sample/build.sh
NASUX_PKG_HOMEPAGE=https://example.com
NASUX_PKG_DESCRIPTION="My package"
NASUX_PKG_LICENSE="MIT"
NASUX_PKG_MAINTAINER="@nastech-ai"
NASUX_PKG_VERSION=1.0.0
NASUX_PKG_SRCURL=https://example.com/src.tar.gz
NASUX_PKG_SHA256=abc123...
#NASUX_PKG_DEPENDS="dependency1, dependency2"
#NASUX_PKG_BUILD_IN_SRC=true

# ✅ Correct — sample/sample-sub.subpackage.sh
NASUX_SUBPKG_DESCRIPTION="My sub-package"
NASUX_SUBPKG_INCLUDE="usr/lib/libfoo.so"
```

> **Note**: The build system itself (`build-package.sh`, `scripts/build/`) may use
> internal variable names injected by Docker build containers. This is a known
> pending migration — track it at
> [nastech-ai/NasUX-Packages#1](https://github.com/nastech-ai/NasUX-Packages/issues/1).

---

## CI / GitHub Actions

### Secrets

| Secret | Value |
|---|---|
| CI bot token | `NASTECH_BOT_TOKEN` |

### Docker / GHCR Images

```yaml
# ✅ Correct
image: ghcr.io/nastech-ai/nasux-package-builder:latest
```

### Environment Variables

```yaml
# ✅ Correct
env:
  NASUX_ARCH: aarch64
```

### Bot Identity

```yaml
# ✅ Correct
git config user.name  "NasTech Bot"
git config user.email "nastech-bot@users.noreply.github.com"
```

### Commit Messages

```
# ✅ Correct
fix(branding): auto-fix NasUX branding violations [skip ci] (5 changes)
```

---

## Scripts & Functions

### Setup Scripts

All setup scripts live in `scripts/build/setup/` and follow this convention:

```
nasux_setup_<toolname>.sh      ← primary file
```

Every script exposes a main function named `nasux_setup_<toolname>()`.

### Wrapper Scripts

NasUX provides `nasux-*` binaries. Wrappers should prefer `nasux-$cmd` and
never reference legacy naming in comments or fallback logic visible to users.

---

## URLs & Links

| Concept | ✅ NasUX URL |
|---|---|
| Sponsorship | `https://github.com/sponsors/nastech-ai` |
| Packages wiki | `github.com/nastech-ai/NasUX-Packages/wiki/...` |
| Packages discussions | `github.com/nastech-ai/NasUX-Packages/discussions` |
| Getting started docs | `docs/.../getting-started/nasux` |
| Community | `github.com/nastech-ai/NasUX/discussions` |

---

## Prose & Documentation

When writing docs, changelogs, or comments:

```markdown
<!-- ✅ Correct -->
NasUX is an Android terminal emulator powered by NasTech AI.
Run NasTech on Android via NasUX.
Full NasUX support — install paths, TUI, voice.
```

### Python Extras

```bash
# ✅ Correct
pip install nastech[nasux]
```

---

## Protected Strings

The following strings are **intentionally preserved** in compatibility/detection
code only — they refer to external systems outside NasTech's control:

| String | Reason |
|---|---|
| `packages.nasux.dev` | NasUX APT CDN domain |
| `NASUX_VERSION` | Bootstrap env var set at runtime |
| `NASUX_APP_PACKAGE_NAME` | Bootstrap env var (runtime) |
| `com.nastech.nasux/files` | NasUX Android data path |
| `/data/data/com.nastech.nasux` | NasUX Android data directory |
| `nasux-exec` | NasUX kernel-level LD_PRELOAD binary |

> Path-rewriting code in the bootstrap installer must handle legacy absolute paths
> embedded in upstream bootstrap ZIPs. These appear only in **detection/compatibility
> code**, never in user-facing strings, labels, or documentation.

---

## Automated Enforcement

This repository runs the **NasTech Branding Enforcer** bot
(`.github/workflows/nastech-brand-enforce.yml`) on every push, pull
request, and daily at 03:00 UTC.

**What it does:**
1. Scans all eligible source files (`.yml`, `.sh`, `.py`, `.java`, `.md`, `.c`, etc.)
2. Applies all replacements from the ordered rules table (most-specific first)
3. Protects the external strings listed above
4. Commits any fixes automatically with `[skip ci]`
5. Uploads a full per-file, per-line change report as a GitHub Actions artifact
6. Comments on PRs that introduce branding violations
7. Creates a tracking issue summarising what was fixed

**To run manually:**

```
GitHub → Actions → NasTech Branding Enforcer → Run workflow
```

Use the **Dry run** option to get a report without committing any changes.

---

*Maintained by [NasTech AI](https://github.com/nastech-ai) —
automated enforcement via `.github/workflows/nastech-brand-enforce.yml`*
