# NasTech Agent — Publishing & Release Guide

Everything you need to cut a release for every artifact the project ships.

---

## Table of Contents
1. [What ships where](#what-ships-where)
2. [Required secrets & variables](#required-secrets--variables)
3. [Version management](#version-management)
4. [Release checklist (start here)](#release-checklist-start-here)
5. [Python package → PyPI](#python-package--pypi)
6. [Docker images → Docker Hub](#docker-images--docker-hub)
7. [Desktop app → GitHub Release](#desktop-app--github-release)
8. [Windows Tauri installer (signed)](#windows-tauri-installer-signed)
9. [Triggering individual pipelines manually](#triggering-individual-pipelines-manually)
10. [Smoke-testing a release locally](#smoke-testing-a-release-locally)

---

## What ships where

| Artifact | Registry / host | Workflow | Trigger |
|---|---|---|---|
| `nastech-agent` Python wheel + sdist | PyPI (`pip install nastech-agent`) | `upload_to_pypi.yml` | CalVer tag `v20YY.MM.DD[.N]` |
| Docker image (amd64 + arm64) | Docker Hub `nastech-ai/nastech-agent` | `docker-publish.yml` | Push to `main` or GitHub Release |
| Electron desktop (DMG, AppImage, deb, rpm, NSIS, MSI) | GitHub Release (draft) | `desktop-build.yml` | Push to `main` touching `apps/desktop/**` |
| Signed Windows Tauri installer (`NasTech-Setup.exe`) | GitHub Release asset | `build-windows-installer.yml` | Manual dispatch (admins only) |

---

## Required secrets & variables

Set these in **GitHub → Settings → Secrets and variables → Actions** on the `nastech-ai/NasTech-Agent` repository.

### Docker Hub
| Secret | Value |
|---|---|
| `DOCKERHUB_USERNAME` | Your Docker Hub username (e.g. `nastechai`) |
| `DOCKERHUB_TOKEN` | Docker Hub access token with **Read & Write** scope |

Create the token at <https://hub.docker.com/settings/security>.

### PyPI (trusted publishing — no token needed)
PyPI uses OIDC trusted publishing. Configure it once at:
1. PyPI → Your Account → Publishing → Add a new publisher
2. Owner: `nastech-ai`, Repo: `NasTech-Agent`, Workflow: `upload_to_pypi.yml`, Environment: `pypi`

No `PYPI_TOKEN` secret is required after this is set up.

### macOS code-signing & notarization (optional)
If unset, the DMG will build successfully but **won't be notarized** (users see a Gatekeeper warning). Set any of these to enable notarization:

| Secret | Value |
|---|---|
| `APPLE_API_KEY` | Content of your `.p8` key file (or the file path on a self-hosted runner) |
| `APPLE_API_KEY_ID` | 10-character key ID from App Store Connect |
| `APPLE_API_ISSUER` | Issuer UUID from App Store Connect |

Alternatively set `APPLE_NOTARY_PROFILE` if using a macOS keychain profile on a self-hosted runner.

### Windows Tauri installer signing (Azure)
Required only for `build-windows-installer.yml` (the signed `NasTech-Setup.exe`). The Electron desktop NSIS build does **not** require signing.

| Secret / Variable | Kind | Value |
|---|---|---|
| `AZURE_CLIENT_ID` | Secret | Azure app registration client ID |
| `AZURE_TENANT_ID` | Secret | Azure tenant ID |
| `AZURE_SUBSCRIPTION_ID` | Secret | Azure subscription ID |
| `AZURE_SIGNING_ENDPOINT` | Variable | Trusted Signing endpoint URL |
| `AZURE_SIGNING_ACCOUNT_NAME` | Variable | Trusted Signing account name |
| `AZURE_SIGNING_CERTIFICATE_PROFILE` | Variable | Certificate profile name |

Set up **federated credentials** (OIDC) on the Azure app registration for `nastech-ai/NasTech-Agent` so the workflow can authenticate without a client secret.

---

## Version management

The project uses **CalVer** (`YYYY.MM.DD[.N]`) for PyPI releases and independent semver for the desktop app.

| File | Controls |
|---|---|
| `pyproject.toml` → `version` | Python package version (e.g. `0.16.0`) |
| `nastech_cli/__init__.py` → `__version__` | Must stay in lockstep with `pyproject.toml` |
| `acp_registry/agent.json` → `version` | ACP registry — must match `pyproject.toml` |
| `apps/desktop/package.json` → `version` | Electron desktop version (e.g. `0.15.1`) |
| `apps/bootstrap-installer/src-tauri/tauri.conf.json` → `version` | Tauri installer version |

**To bump the Python/Docker version** use `scripts/release.py`:
```bash
# Dry run — preview changelog
python scripts/release.py --bump minor

# Create tag + GitHub Release + trigger PyPI + Docker publish
python scripts/release.py --bump minor --publish
```

**To bump the desktop version** edit `apps/desktop/package.json` and push to `main`. The `desktop-build.yml` workflow will build all platforms and update the draft release.

---

## Release checklist (start here)

Before cutting any release, verify these pass on `main`:

```bash
# Python quality gates
uv run ruff check .
python scripts/check-windows-footguns.py --all
python scripts/branding_lint.py

# Lockfile integrity
uv lock --check
cd web && npm ci   # verifies web/package-lock.json is consistent

# Python tests (abridged local run)
uv run pytest tests/ -x -q --ignore=tests/docker --ignore=tests/e2e
```

All CI workflows (`tests.yml`, `lint.yml`, `branding-lint.yml`, `docker-lint.yml`, `type-check.yml`) must be green on `main` before publishing.

---

## Python package → PyPI

### Automatic (recommended)
```bash
# 1. Ensure you're on main with a clean working tree
git checkout main && git pull

# 2. Run the release script (bumps version, creates tag, opens GitHub Release)
python scripts/release.py --bump minor --publish
#   or --bump patch for a patch release

# 3. The tag push triggers upload_to_pypi.yml automatically:
#    - Builds web dashboard (cd web && npm ci && npm run build)
#    - Builds TUI bundle   (cd ui-tui && npm ci && npm run build)
#    - Builds Python wheel + sdist
#    - Publishes to PyPI via OIDC trusted publishing
#    - Signs artifacts with Sigstore and attaches to the GitHub Release
```

### Manual (escape hatch)
1. Ensure the tag already exists: `git tag v2026.6.8`
2. Go to **Actions → Publish to PyPI → Run workflow**
3. Enter the tag name (e.g. `v2026.6.8`) and click **Run**

### What the workflow builds
- Runs `cd web && npm ci && npm run build` → `nastech_cli/web_dist/`
- Runs `cd ui-tui && npm ci && npm run build` → `nastech_cli/tui_dist/entry.js`
- Bundles `scripts/install.sh` and `scripts/install.ps1` into the wheel
- Publishes to <https://pypi.org/p/nastech-agent>

---

## Docker images → Docker Hub

### Automatic
Every push to `main` that touches `**/*.py`, `pyproject.toml`, `uv.lock`, `Dockerfile`, or `docker/**` triggers `docker-publish.yml`:
- Builds `linux/amd64` on `ubuntu-latest`
- Builds `linux/arm64` on `ubuntu-24.04-arm` (native, no QEMU)
- Runs smoke tests on both arches
- Merges into a multi-arch manifest
- Tags `:main` and `:latest` on Docker Hub

### On GitHub Release
Publishing a GitHub Release tags the image with the release tag name (e.g. `nastech-ai/nastech-agent:v2026.6.8`).

### Local build & test
```bash
# Build and load amd64
docker buildx build --platform linux/amd64 \
  --build-arg NASTECH_GIT_SHA=$(git rev-parse HEAD) \
  -t nastech-ai/nastech-agent:local --load .

# Quick smoke test
docker run --rm nastech-ai/nastech-agent:local nastech --help
docker run --rm nastech-ai/nastech-agent:local nastech dashboard --help
```

### Registry targets
| Tag | Pushed when |
|---|---|
| `:latest` | Every push to `main` |
| `:main` | Every push to `main` |
| `:<release-tag>` | GitHub Release published |

---

## Desktop app → GitHub Release

### Automatic
Push to `main` with changes under `apps/desktop/**` or `apps/shared/**` triggers `desktop-build.yml`. It builds all three platforms in parallel and creates/updates a **draft** GitHub Release named `NasTech Desktop vX.Y.Z`.

### Manual
**Actions → Desktop Build (All Platforms) → Run workflow** — no inputs needed.

### Artifacts produced
| Platform | Files | Runner |
|---|---|---|
| 🍎 macOS | `NasTech-*.dmg`, `NasTech-*.zip` | `macos-latest` |
| 🐧 Linux | `NasTech-*.AppImage`, `NasTech-*.deb`, `NasTech-*.rpm` | `ubuntu-22.04` |
| 🪟 Windows | `NasTech-*.exe` (NSIS), `NasTech-*.msi` | `windows-latest` |

All artifacts use the naming pattern `NasTech-{version}-{os}-{arch}.{ext}`.

### Publishing the draft release
1. Go to **GitHub → Releases → Drafts**
2. Review the auto-generated release notes
3. Click **Edit → Publish release**

### macOS notarization
The `scripts/notarize.cjs` script runs after signing. It **skips silently** if `APPLE_API_KEY`, `APPLE_API_KEY_ID`, and `APPLE_API_ISSUER` are not all set. Set these secrets to produce a notarized DMG.

### Bumping the desktop version
Edit `"version"` in `apps/desktop/package.json` and push to `main`. The release draft will pick up the new version automatically.

---

## Windows Tauri installer (signed)

The `build-windows-installer.yml` workflow builds `NasTech-Setup.exe` — a Tauri-based GUI installer that runs `scripts/install.ps1`. It is **admin-only** (the workflow checks that the triggering actor has admin permission on the repo).

### Prerequisites
- All Azure secrets and variables configured (see [Required secrets](#required-secrets--variables))
- Azure Trusted Signing account with an active certificate profile
- Federated OIDC credentials configured on the Azure app registration

### Running the build
1. Go to **Actions → Build Windows Installer → Run workflow**
2. Only repository admins can trigger it — others receive an authorization error
3. The workflow:
   - Installs `apps/bootstrap-installer` Node dependencies
   - Builds with `npm run tauri:build` (Tauri v2 + Rust)
   - Signs all `.exe` files with Azure Artifact Signing
   - Uploads `NasTech-Setup-installer` (NSIS bundle) and `NasTech-Setup-exe` (raw) as artifacts

### Getting the signed artifacts
Download from **Actions → the completed run → Artifacts**:
- `NasTech-Setup-installer` — NSIS installer bundle
- `NasTech-Setup-exe` — raw signed executable

Attach these manually to the relevant GitHub Release.

---

## Triggering individual pipelines manually

| Goal | How |
|---|---|
| Publish Python to PyPI | `python scripts/release.py --bump patch --publish` OR Actions → Publish to PyPI → Run workflow |
| Build Docker image now | Push any `.py` file to `main` OR manually trigger docker-publish (not exposed as workflow_dispatch — push a no-op commit) |
| Build all desktop platforms | Actions → Desktop Build (All Platforms) → Run workflow |
| Build signed Windows installer | Actions → Build Windows Installer → Run workflow (admin only) |
| Run all Python tests | Actions → Tests → Run workflow |

---

## Smoke-testing a release locally

### Docker
```bash
docker run --rm -it nastech-ai/nastech-agent:latest nastech --version
docker run --rm -it nastech-ai/nastech-agent:latest nastech dump
docker run --rm -p 9119:9119 nastech-ai/nastech-agent:latest
# Then open http://localhost:9119
```

### Python wheel
```bash
pip install nastech-agent==<version>
nastech --version
nastech dashboard --help
```

### Desktop DMG (macOS)
1. Mount the DMG
2. Drag NasTech.app to Applications
3. Launch — confirm the backend starts and the dashboard loads

### Docker Compose
```bash
docker compose up
# Or with Windows-specific overrides:
docker compose -f docker-compose.yml -f docker-compose.windows.yml up
```

---

## CI workflow reference

| Workflow file | Trigger | Blocks merge? |
|---|---|---|
| `tests.yml` | push/PR to main | Yes (required check) |
| `lint.yml` (ruff-blocking) | push/PR to main | Yes |
| `lint.yml` (lint-diff) | push/PR to main | No (advisory) |
| `branding-lint.yml` | push/PR to main | Yes |
| `docker-lint.yml` | Dockerfile/docker/** changes | Yes |
| `type-check.yml` | push/PR to main | Yes |
| `uv-lockfile-check.yml` | push/PR to main | Yes |
| `supply-chain-audit.yml` | push/PR to main | Advisory |
| `docker-publish.yml` | push to main / release | Publish (not blocking) |
| `desktop-build.yml` | push to main / dispatch | Publish (not blocking) |
| `upload_to_pypi.yml` | tag `v20*` / dispatch | Publish (not blocking) |
| `build-windows-installer.yml` | Manual dispatch (admin only) | Publish (not blocking) |
