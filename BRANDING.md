# NasTech / NasUX Branding Guide

> **Rule #1**: There is no "Termux" in NasUX. Every identifier, comment, label,
> URL, and string in this repository must use **NasUX** / **NasTech** naming.
> The automated Branding Enforcer bot will catch and fix violations on every push.

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

| ❌ Don't use | ✅ Use instead |
|---|---|
| `Termux` | `NasUX` |
| `termux` | `nasux` |
| `TERMUX` | `NASUX` |
| `termux/termux-packages` | `nastech-ai/NasUX-Packages` |
| `termux/termux-app` | `nastech-ai/NasUX` |
| `ghcr.io/termux/package-builder` | `ghcr.io/nastech-ai/nasux-package-builder` |
| `Termux Github Actions` | `NasTech Bot` |
| `contact@termux.dev` | `nastech-bot@users.noreply.github.com` |
| `TERMUXBOT2_TOKEN` | `NASTECH_BOT_TOKEN` |
| `@termux` | `@nastech-ai` |
| `TERMUX_PKG_*` | `NASUX_PKG_*` |
| `TERMUX_SUBPKG_*` | `NASUX_SUBPKG_*` |
| `TERMUX_ARCH` | `NASUX_ARCH` |
| `TERMUX_SCRIPTDIR` | `NASUX_SCRIPTDIR` |
| `termux_setup_*` | `nasux_setup_*` |
| `termux-tools` | `nasux-tools` |
| `termux-info` | `nasux-info` |
| `termux-setup-storage` | `nasux-setup-storage` |
| `via Termux` | `via NasUX` |
| `on Termux` | `on NasUX` |
| `Android / Termux` | `Android / NasUX` |
| `.[termux]` extra | `.[nasux]` extra |

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

// ❌ Wrong
package com.termux;
class TermuxInstaller { ... }
TermuxConstants.TERMUX_PREFIX_DIR_PATH
```

### Python Identifiers

```python
# ✅ Correct
def is_nasux() -> bool: ...
def is_android_terminal() -> bool: ...   # detects any Android terminal
def is_nasux_proot_distro() -> bool: ...
_nasux_cache: bool | None = None
_nasux_terminal_cache: bool | None = None

# ❌ Wrong
def is_termux() -> bool: ...
_termux_cache: bool | None = None
```

### Shell Variables & Functions

```bash
# ✅ Correct
NASUX_ARCH=aarch64
NASUX_SCRIPTDIR="$(realpath "$(dirname "$0")/../..")"
nasux_setup_golang() { ... }
nasux_setup_rust() { ... }

# ❌ Wrong
TERMUX_ARCH=aarch64
TERMUX_SCRIPTDIR=...
termux_setup_golang() { ... }
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

> **Note**: The build system itself (`build-package.sh`, `scripts/build/`) still
> uses the legacy `TERMUX_PKG_*` variable names internally because Docker build
> containers inject them. This is a known pending migration — track it at
> [nastech-ai/NasUX-Packages#1](https://github.com/nastech-ai/NasUX-Packages/issues/1).

---

## CI / GitHub Actions

### Secrets

| ❌ Old | ✅ New |
|---|---|
| `TERMUXBOT2_TOKEN` | `NASTECH_BOT_TOKEN` |

### Docker / GHCR Images

```yaml
# ✅ Correct
image: ghcr.io/nastech-ai/nasux-package-builder:latest

# ❌ Wrong
image: ghcr.io/termux/package-builder:latest
```

### Environment Variables

```yaml
# ✅ Correct
env:
  NASUX_ARCH: aarch64

# ❌ Wrong
env:
  TERMUX_ARCH: aarch64
```

### Bot Identity

```yaml
# ✅ Correct
git config user.name  "NasTech Bot"
git config user.email "nastech-bot@users.noreply.github.com"

# ❌ Wrong
git config user.name  "Termux Github Actions"
git config user.email "contact@termux.dev"
```

### Commit Messages

```
# ✅ Correct
fix(branding): auto-fix NasUX branding violations [skip ci] (5 changes)

# ❌ Wrong
fix(branding): auto-fix Termux violations [skip ci]
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
never reference the old naming in comments or fallback logic visible to users.

---

## URLs & Links

| ❌ Old URL | ✅ New URL |
|---|---|
| `https://termux.dev/donate` | `https://github.com/sponsors/nastech-ai` |
| `github.com/termux/termux-packages/wiki/...` | `github.com/nastech-ai/NasUX-Packages/wiki/...` |
| `github.com/termux/termux-packages/discussions` | `github.com/nastech-ai/NasUX-Packages/discussions` |
| `docs/.../getting-started/termux` | `docs/.../getting-started/nasux` |
| `discord.gg/HXpF69X` | `github.com/nastech-ai/NasUX/discussions` |

---

## Prose & Documentation

When writing docs, changelogs, or comments:

```markdown
<!-- ✅ Correct -->
NasUX is an Android terminal emulator powered by NasTech AI.
Run NasTech on Android via NasUX.
Full NasUX support — install paths, TUI, voice.

<!-- ❌ Wrong -->
Termux is an Android terminal emulator.
Run NasTech on Android via Termux.
Full Termux support.
```

### Python Extras

```bash
# ✅ Correct
pip install nastech[nasux]

# ❌ Wrong
pip install nastech[termux]
```

---

## Protected Strings

The following strings are **intentionally preserved** — they refer to external
systems outside NasTech's control and cannot be renamed:

| String | Reason |
|---|---|
| `packages.termux.dev` | External APT CDN — NasTech does not own this domain |
| `TERMUX_VERSION` | Android bootstrap env var set by the underlying system |
| `TERMUX_APP_PACKAGE_NAME` | Android bootstrap env var (external) |
| `com.termux/files` | External Android data path used for compatibility detection |
| `/data/data/com.termux` | External Android data directory (detection only) |
| `termux-exec` | External kernel-level LD_PRELOAD binary |

> These appear only in **detection/compatibility code**, never in user-facing
> strings, labels, or documentation.

---

## Automated Enforcement

This repository runs the **NasTech Branding Enforcer** bot
(`.github/workflows/nastech_branding_enforcer.yml`) on every push, pull
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
automated enforcement via `.github/workflows/nastech_branding_enforcer.yml`*
