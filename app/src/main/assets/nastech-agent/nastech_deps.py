"""
nastech_deps.py — NasTech Self-Maintaining Dependency Manager
Powered by NasTech AI / NasUX

Responsibilities:
  - Detect missing / mismatched packages (pip, npm, gradle) without importing them
  - Install only when necessary (lock-file guards every boot)
  - Safe update: patch/minor by default, major requires confirmation
  - Offline safe mode: skip network ops, use cached deps, never crash
  - Retry-once on failure; isolate and log without crashing the app

Lock file:  ~/.nastech/.deps_lock.json
  { "python": {"<pkg>": "<installed_ver>", ...},
    "npm":    {"<pkg>": "<installed_ver>", ...},
    "last_checked": "<iso-timestamp>",
    "first_run_complete": true }

Import-safe: zero third-party imports at module level.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import time
import threading
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

# ── Project root (this file lives next to nastech_constants.py) ───────────────
AGENT_ROOT = Path(__file__).resolve().parent

# ── Colors (stderr only, auto-disabled when not a tty) ───────────────────────
_C = sys.stderr.isatty()
_G  = "\033[0;32m" if _C else ""
_Y  = "\033[0;33m" if _C else ""
_CY = "\033[0;36m" if _C else ""
_R  = "\033[0;31m" if _C else ""
_NC = "\033[0m"    if _C else ""

# ── Update check interval: 24 h ───────────────────────────────────────────────
UPDATE_CHECK_INTERVAL_HOURS = 24

# ── Core Python packages required for NasTech to boot ────────────────────────
PYTHON_CORE_PACKAGES = [
    "fastapi",
    "uvicorn",
    "pyyaml",
    "httpx",
    "rich",
    "click",
    "python-dotenv",
]

# ── npm packages required for the frontend (web/ dir) ────────────────────────
NPM_CORE_PACKAGES: list[str] = []   # populated from package.json if present

# ── Semver helpers ────────────────────────────────────────────────────────────

_VER_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)")


def _parse_ver(v: str) -> tuple[int, int, int] | None:
    m = _VER_RE.match(str(v).strip().lstrip("^~>=<v"))
    if not m:
        return None
    return int(m.group(1)), int(m.group(2)), int(m.group(3))


def _is_safe_upgrade(current: str, candidate: str) -> bool:
    """Return True if upgrading current→candidate is safe (patch or minor bump)."""
    c = _parse_ver(current)
    n = _parse_ver(candidate)
    if c is None or n is None:
        return False
    # Major version bump → NOT safe without confirmation
    if n[0] > c[0]:
        return False
    return n >= c


def _is_major_bump(current: str, candidate: str) -> bool:
    c = _parse_ver(current)
    n = _parse_ver(candidate)
    if c is None or n is None:
        return False
    return n[0] > c[0]


# ── Logging helpers ───────────────────────────────────────────────────────────

_debug_mode: bool = False
_boot_start: float = time.monotonic()


def _log_boot(msg: str) -> None:
    if _debug_mode:
        ms = int((time.monotonic() - _boot_start) * 1000)
        print(f"{_CY}[DEPS +{ms}ms]{_NC} {msg}", file=sys.stderr, flush=True)


def _log_ok(msg: str) -> None:
    print(f"{_G}[OK]{_NC}   {msg}", file=sys.stderr, flush=True)


def _log_info(msg: str) -> None:
    print(f"{_CY}[INFO]{_NC} {msg}", file=sys.stderr, flush=True)


def _log_warn(msg: str) -> None:
    print(f"{_Y}[WARN]{_NC} {msg}", file=sys.stderr, flush=True)


def _log_err(msg: str) -> None:
    print(f"{_R}[ERROR]{_NC} {msg}", file=sys.stderr, flush=True)


# ── Lock file ─────────────────────────────────────────────────────────────────

class _LockFile:
    """Thread-safe JSON lock file that tracks installed packages and timestamps."""

    def __init__(self, path: Path) -> None:
        self.path = path
        self._lock = threading.Lock()

    def _read(self) -> dict:
        try:
            return json.loads(self.path.read_text(encoding="utf-8"))
        except Exception:
            return {}

    def _write(self, data: dict) -> None:
        try:
            self.path.parent.mkdir(parents=True, exist_ok=True)
            self.path.write_text(json.dumps(data, indent=2), encoding="utf-8")
        except Exception as exc:
            _log_warn(f"Could not write deps lock file: {exc}")

    def get(self, *keys, default=None):
        with self._lock:
            d = self._read()
            for k in keys:
                if not isinstance(d, dict):
                    return default
                d = d.get(k, {})
            return d if d != {} else default

    def set(self, section: str, key: str, value) -> None:
        with self._lock:
            d = self._read()
            if section not in d or not isinstance(d[section], dict):
                d[section] = {}
            d[section][key] = value
            self._write(d)

    def set_top(self, key: str, value) -> None:
        with self._lock:
            d = self._read()
            d[key] = value
            self._write(d)

    def get_top(self, key, default=None):
        with self._lock:
            return self._read().get(key, default)

    def first_run_complete(self) -> bool:
        return bool(self.get_top("first_run_complete", False))

    def mark_first_run_complete(self) -> None:
        self.set_top("first_run_complete", True)
        self.set_top("last_bootstrap", datetime.now(timezone.utc).isoformat())

    def should_check_updates(self) -> bool:
        last = self.get_top("last_update_check")
        if not last:
            return True
        try:
            last_dt = datetime.fromisoformat(last)
            age_hours = (datetime.now(timezone.utc) - last_dt).total_seconds() / 3600
            return age_hours >= UPDATE_CHECK_INTERVAL_HOURS
        except Exception:
            return True

    def mark_update_checked(self) -> None:
        self.set_top("last_update_check", datetime.now(timezone.utc).isoformat())

    def get_installed_version(self, ecosystem: str, package: str) -> str | None:
        return self.get(ecosystem, package, default=None)

    def record_installed(self, ecosystem: str, package: str, version: str) -> None:
        self.set(ecosystem, package, version)


# ── Network / offline detection ───────────────────────────────────────────────

def _is_online(timeout: float = 2.0) -> bool:
    """Fast offline check — connects to 8.8.8.8:53 (DNS). No DNS lookup needed."""
    import socket
    try:
        socket.setdefaulttimeout(timeout)
        socket.socket(socket.AF_INET, socket.SOCK_STREAM).connect(("8.8.8.8", 53))
        return True
    except OSError:
        return False


# ── Python dependency helpers ─────────────────────────────────────────────────

def _python_package_version(package: str) -> str | None:
    """Return installed version of *package*, or None if not importable."""
    import importlib.metadata
    # Normalize: click → click, python-dotenv → python_dotenv
    normalized = package.replace("-", "_").replace(".", "_").lower()
    candidates = [package, normalized, package.replace("-", "_")]
    for name in candidates:
        try:
            return importlib.metadata.version(name)
        except Exception:
            pass
    # Fallback: try importing
    try:
        mod = __import__(normalized.split("[")[0])
        return getattr(mod, "__version__", "installed")
    except ImportError:
        return None


def _find_pip() -> str:
    """Return the path to the best pip command available."""
    for candidate in [
        sys.executable + " -m pip",
        "pip3",
        "pip",
    ]:
        parts = candidate.split()
        try:
            result = subprocess.run(
                parts + ["--version"],
                capture_output=True, timeout=5
            )
            if result.returncode == 0:
                return candidate
        except Exception:
            pass
    return sys.executable + " -m pip"


def _find_uv() -> str | None:
    """Return path to uv if available (much faster than pip)."""
    for candidate in [
        "uv",
        str(Path.home() / ".local" / "bin" / "uv"),
        str(Path.home() / ".cargo" / "bin" / "uv"),
    ]:
        try:
            r = subprocess.run(
                [candidate, "--version"],
                capture_output=True, timeout=5
            )
            if r.returncode == 0:
                return candidate
        except Exception:
            pass
    return None


def _get_installed_python_packages() -> dict[str, str]:
    """Return dict of {package_name: version} for all installed packages."""
    import importlib.metadata
    result = {}
    try:
        for dist in importlib.metadata.distributions():
            name = dist.metadata.get("Name", "")
            ver  = dist.metadata.get("Version", "")
            if name and ver:
                result[name.lower()] = ver
    except Exception:
        pass
    return result


def _install_python_package(
    package: str,
    lock: _LockFile,
    uv_cmd: str | None,
    pip_cmd: str,
    extra_index: str | None = None,
    retry: int = 0,
) -> bool:
    """
    Install *package* via uv or pip. Returns True on success.
    Retries once on failure (retry=0 → will retry; retry=1 → final attempt).
    """
    _log_info(f"Installing Python package: {package} ...")

    cmd_parts: list[list[str]]

    if uv_cmd:
        base = [uv_cmd, "pip", "install", package, "--quiet"]
    else:
        base = pip_cmd.split() + ["install", package, "--quiet", "--no-input"]

    if extra_index:
        base += ["--extra-index-url", extra_index]

    try:
        result = subprocess.run(
            base,
            capture_output=True,
            text=True,
            timeout=120,
        )
        if result.returncode == 0:
            version = _python_package_version(package) or "unknown"
            lock.record_installed("python", package.lower(), version)
            _log_ok(f"Installed {package} {version}")
            return True
        else:
            stderr = result.stderr.strip()[-300:] if result.stderr else ""
            _log_warn(f"Install of {package} failed (rc={result.returncode}): {stderr}")
    except subprocess.TimeoutExpired:
        _log_warn(f"Install of {package} timed out after 120s")
    except Exception as exc:
        _log_warn(f"Install of {package} raised: {exc}")

    # Retry once
    if retry == 0:
        _log_info(f"Retrying install of {package}...")
        return _install_python_package(package, lock, uv_cmd, pip_cmd, extra_index, retry=1)

    _log_err(f"Failed to install {package} after 2 attempts — continuing without it")
    return False


# ── npm dependency helpers ─────────────────────────────────────────────────────

def _find_npm() -> str | None:
    for candidate in ["npm", "npx"]:
        try:
            r = subprocess.run([candidate, "--version"], capture_output=True, timeout=5)
            if r.returncode == 0:
                return candidate
        except Exception:
            pass
    return None


def _npm_install(cwd: Path, npm_cmd: str, retry: int = 0) -> bool:
    """Run npm install in *cwd*. Retries once."""
    _log_info(f"Running npm install in {cwd} ...")
    try:
        result = subprocess.run(
            [npm_cmd, "install", "--prefer-offline", "--no-audit", "--loglevel=error"],
            cwd=str(cwd),
            capture_output=True,
            text=True,
            timeout=180,
        )
        if result.returncode == 0:
            _log_ok(f"npm install completed in {cwd.name}/")
            return True
        stderr = result.stderr.strip()[-300:] if result.stderr else ""
        _log_warn(f"npm install failed (rc={result.returncode}): {stderr}")
    except subprocess.TimeoutExpired:
        _log_warn("npm install timed out after 180s")
    except Exception as exc:
        _log_warn(f"npm install error: {exc}")

    if retry == 0:
        _log_info("Retrying npm install...")
        return _npm_install(cwd, npm_cmd, retry=1)

    _log_err("npm install failed after 2 attempts — frontend may not work")
    return False


def _npm_outdated_safe(cwd: Path, npm_cmd: str) -> dict[str, dict]:
    """Return dict of outdated npm packages (name → {current, wanted, latest})."""
    try:
        r = subprocess.run(
            [npm_cmd, "outdated", "--json"],
            cwd=str(cwd), capture_output=True, text=True, timeout=30
        )
        data = json.loads(r.stdout or "{}")
        return data if isinstance(data, dict) else {}
    except Exception:
        return {}


# ── Gradle dependency helpers ─────────────────────────────────────────────────

def _find_gradle() -> str | None:
    # Prefer wrapper
    for candidate in ["./gradlew", "gradlew", "gradle"]:
        try:
            r = subprocess.run(
                [candidate, "--version"],
                capture_output=True, timeout=15,
                cwd=str(AGENT_ROOT.parent.parent.parent.parent),  # project root
            )
            if r.returncode == 0:
                return candidate
        except Exception:
            pass
    return None


def _gradle_resolve(project_root: Path, gradle_cmd: str, retry: int = 0) -> bool:
    """Run gradle dependencies (resolve only, no build). Retries once."""
    _log_info("Running Gradle dependency resolution...")
    try:
        result = subprocess.run(
            [gradle_cmd, "dependencies", "--quiet", "--configuration", "runtimeClasspath"],
            cwd=str(project_root),
            capture_output=True,
            text=True,
            timeout=300,
        )
        if result.returncode == 0:
            _log_ok("Gradle dependencies resolved")
            return True
        stderr = result.stderr.strip()[-300:] if result.stderr else ""
        _log_warn(f"Gradle resolve failed (rc={result.returncode}): {stderr}")
    except subprocess.TimeoutExpired:
        _log_warn("Gradle dependency resolution timed out after 300s")
    except Exception as exc:
        _log_warn(f"Gradle error: {exc}")

    if retry == 0:
        _log_info("Retrying Gradle resolve...")
        return _gradle_resolve(project_root, gradle_cmd, retry=1)

    _log_err("Gradle resolve failed after 2 attempts — build may fail")
    return False


# ── Requirements file detection ───────────────────────────────────────────────

def _read_requirements(path: Path) -> list[str]:
    """Parse a requirements.txt into a list of package specs."""
    if not path.exists():
        return []
    result = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("-r") or line.startswith("--"):
            continue
        result.append(line)
    return result


def _extract_package_name(spec: str) -> str:
    """'httpx>=0.27' → 'httpx'"""
    return re.split(r"[>=<!;\[]", spec)[0].strip()


# ── Update helpers ────────────────────────────────────────────────────────────

def _pip_get_latest_version(package: str, uv_cmd: str | None, pip_cmd: str) -> str | None:
    """Query PyPI for the latest version of *package*."""
    try:
        import urllib.request, urllib.error
        url = f"https://pypi.org/pypi/{package}/json"
        with urllib.request.urlopen(url, timeout=5) as resp:
            data = json.loads(resp.read().decode())
            return data.get("info", {}).get("version")
    except Exception:
        return None


def _npm_get_latest_version(package: str, npm_cmd: str) -> str | None:
    try:
        r = subprocess.run(
            [npm_cmd, "view", package, "version"],
            capture_output=True, text=True, timeout=10
        )
        return r.stdout.strip() if r.returncode == 0 else None
    except Exception:
        return None


# ─────────────────────────────────────────────────────────────────────────────
# Public API
# ─────────────────────────────────────────────────────────────────────────────

class DependencyManager:
    """
    Central dependency manager for NasTech / NasUX.

    Usage:
        dm = DependencyManager(debug=True)
        dm.run_boot_check()   # called from nastech_launcher on every boot
        dm.run_update()       # called by `nastech update`
    """

    def __init__(
        self,
        *,
        debug: bool = False,
        nastech_home: Path | None = None,
        agent_root: Path = AGENT_ROOT,
        assume_yes: bool = False,
    ) -> None:
        global _debug_mode
        _debug_mode = debug
        self.debug = debug
        self.agent_root = agent_root
        self.nastech_home = nastech_home or Path(
            os.environ.get("NASTECH_HOME", Path.home() / ".nastech")
        )
        self.assume_yes = assume_yes
        self.lock = _LockFile(self.nastech_home / ".deps_lock.json")
        self._online: bool | None = None   # cached after first check

    # ── Online detection (cached per DM instance) ────────────────────────────

    def is_online(self) -> bool:
        if self._online is None:
            self._online = _is_online()
            if not self._online:
                _log_warn("Offline mode — network ops skipped, using cached dependencies")
            else:
                _log_boot("Network available")
        return self._online

    # ── Tool discovery ───────────────────────────────────────────────────────

    def _pip(self) -> str:
        return _find_pip()

    def _uv(self) -> str | None:
        return _find_uv()

    def _npm(self) -> str | None:
        return _find_npm()

    # ── Python deps ──────────────────────────────────────────────────────────

    def _get_python_packages_to_check(self) -> list[str]:
        """Merge PYTHON_CORE_PACKAGES + requirements.txt (if present)."""
        packages = list(PYTHON_CORE_PACKAGES)

        req_file = self.agent_root / "requirements.txt"
        for spec in _read_requirements(req_file):
            name = _extract_package_name(spec)
            if name and name.lower() not in {p.lower() for p in packages}:
                packages.append(spec)

        return packages

    def check_python_deps(self) -> tuple[list[str], list[str]]:
        """
        Check Python deps. Returns (missing, outdated_by_lock) lists.
        Does NOT trigger installs — just detection.
        """
        packages = self._get_python_packages_to_check()
        missing = []
        outdated = []

        for spec in packages:
            name = _extract_package_name(spec)
            installed = _python_package_version(name)
            if installed is None:
                _log_boot(f"Python missing: {name}")
                missing.append(spec)
            else:
                _log_boot(f"Python ok: {name}=={installed}")

        return missing, outdated

    def install_python_deps(self, packages: list[str]) -> dict[str, bool]:
        """Install a list of packages. Returns {package: success}."""
        if not packages:
            return {}
        uv = self._uv()
        pip = self._pip()
        results = {}
        for spec in packages:
            name = _extract_package_name(spec)
            ok = _install_python_package(spec, self.lock, uv, pip)
            results[name] = ok
        return results

    def ensure_python_deps(self) -> bool:
        """Check + install missing Python packages. Returns True if all OK."""
        _log_boot("Checking Python dependencies...")

        # Fast path: lock file says first-run complete — only spot-check
        if self.lock.first_run_complete():
            packages = self._get_python_packages_to_check()
            missing = []
            for spec in packages:
                name = _extract_package_name(spec)
                if _python_package_version(name) is None:
                    missing.append(spec)
            if not missing:
                _log_boot("Python deps: all present (lock file validated)")
                return True
            _log_info(f"Found {len(missing)} missing Python package(s) — installing...")
            results = self.install_python_deps(missing)
            return all(results.values())

        # First run: install everything
        _log_info("First run — installing Python dependencies...")
        missing, _ = self.check_python_deps()
        if not missing:
            _log_ok("Python dependencies: all present")
            return True

        _log_info(f"Installing {len(missing)} Python package(s)...")
        results = self.install_python_deps(missing)
        failures = [k for k, v in results.items() if not v]
        if failures:
            _log_warn(f"Some packages could not be installed: {', '.join(failures)}")
        return len(failures) == 0

    # ── npm deps ─────────────────────────────────────────────────────────────

    def ensure_npm_deps(self) -> bool:
        """Check + install npm deps in web/ if present. Returns True if OK or N/A."""
        web_dir = self.agent_root / "web"
        if not web_dir.is_dir():
            _log_boot("No web/ directory — skipping npm deps")
            return True

        pkg_json = web_dir / "package.json"
        if not pkg_json.exists():
            _log_boot("No web/package.json — skipping npm deps")
            return True

        node_modules = web_dir / "node_modules"
        npm = self._npm()

        if npm is None:
            _log_warn("npm not found — frontend deps cannot be installed")
            _log_warn("Install Node.js: https://nodejs.org")
            return False

        # Check if node_modules exists and looks complete
        if node_modules.is_dir() and any(node_modules.iterdir()):
            # Spot check: does package.json match node_modules?
            try:
                pkg = json.loads(pkg_json.read_text(encoding="utf-8"))
                deps = {**pkg.get("dependencies", {}), **pkg.get("devDependencies", {})}
                missing = [
                    d for d in deps
                    if not (node_modules / d).is_dir()
                ]
                if not missing:
                    _log_boot("npm deps: all present")
                    return True
                _log_info(f"Missing {len(missing)} npm package(s) — running npm install...")
            except Exception:
                # Can't validate — run install anyway
                _log_boot("npm deps: cannot validate — running npm install")

        return _npm_install(web_dir, npm)

    # ── Gradle deps ──────────────────────────────────────────────────────────

    def ensure_gradle_deps(self, project_root: Path | None = None) -> bool:
        """Resolve Gradle dependencies if this is a Gradle project. Returns True if OK or N/A."""
        root = project_root or (self.agent_root.parent.parent.parent.parent)
        if not (root / "build.gradle").exists() and not (root / "build.gradle.kts").exists():
            _log_boot("No build.gradle found — skipping Gradle sync")
            return True

        gradle = _find_gradle()
        if gradle is None:
            _log_warn("Gradle not found — Android build deps cannot be synced")
            return False

        return _gradle_resolve(root, gradle)

    # ── Master boot check ────────────────────────────────────────────────────

    def run_boot_check(self) -> bool:
        """
        Called on every boot. Runs the full dependency check sequence.

        Performance: skips if lock file is fresh and no packages are missing.
        Returns True if all critical deps are satisfied.
        """
        _log_boot("Dependency boot check starting...")

        python_ok = self.ensure_python_deps()
        npm_ok    = self.ensure_npm_deps()
        # Gradle sync is expensive — skip on normal boot unless first run
        gradle_ok = True
        if not self.lock.first_run_complete():
            gradle_ok = self.ensure_gradle_deps()

        all_ok = python_ok and npm_ok and gradle_ok

        if not self.lock.first_run_complete():
            self.lock.mark_first_run_complete()
            _log_ok("First-run dependency bootstrap complete")

        if all_ok:
            _log_boot("All dependencies satisfied")
        else:
            _log_warn("Some dependencies could not be installed — app may have limited functionality")

        return all_ok

    # ── Safe update command ───────────────────────────────────────────────────

    def run_update(self, *, force_major: bool = False) -> int:
        """
        `nastech update` implementation.

        - Checks latest pip versions for all core packages
        - Updates patch/minor versions automatically
        - Prompts (or skips with --yes) for major version bumps
        - Prevents infinite update loops via timestamp guard
        - Offline safe: skips and notifies if no network

        Returns exit code (0 = success).
        """
        print(f"\n{_CY}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{_NC}", file=sys.stderr)
        print(f"{_CY}  NasTech Update Check — Powered by NasTech AI{_NC}", file=sys.stderr)
        print(f"{_CY}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{_NC}\n", file=sys.stderr)

        if not self.is_online():
            _log_warn("Offline — cannot check for updates. Using cached packages.")
            return 0

        # Infinite-loop guard: skip if checked recently (unless --force)
        if not force_major and not self.lock.should_check_updates():
            _log_info("Update check skipped — last check was less than 24h ago")
            _log_info("Use  nastech update --force  to override")
            return 0

        uv = self._uv()
        pip = self._pip()
        npm = self._npm()

        packages = self._get_python_packages_to_check()
        installed = _get_installed_python_packages()

        to_update_safe: list[str]      = []
        to_update_major: list[tuple]   = []   # (name, current, latest)
        already_latest:  list[str]     = []
        check_failed:    list[str]     = []

        _log_info(f"Checking {len(packages)} Python package(s)...")

        for spec in packages:
            name = _extract_package_name(spec).lower()
            current = installed.get(name) or _python_package_version(name)
            if current is None:
                to_update_safe.append(spec)
                continue

            latest = _pip_get_latest_version(name, uv, pip)
            if latest is None:
                check_failed.append(name)
                continue

            if _is_major_bump(current, latest):
                to_update_major.append((name, current, latest))
            elif _is_safe_upgrade(current, latest) and _parse_ver(latest) > _parse_ver(current):
                to_update_safe.append(f"{name}=={latest}")
                _log_boot(f"  {name}: {current} → {latest} (safe)")
            else:
                already_latest.append(name)
                _log_boot(f"  {name}: {current} (up to date)")

        # Print summary
        if already_latest:
            _log_ok(f"{len(already_latest)} package(s) already up to date")
        if check_failed:
            _log_warn(f"Could not check: {', '.join(check_failed)}")

        # Apply safe updates
        if to_update_safe:
            _log_info(f"Applying {len(to_update_safe)} safe update(s)...")
            for spec in to_update_safe:
                _install_python_package(spec, self.lock, uv, pip)

        # Handle major bumps
        for name, current, latest in to_update_major:
            _log_warn(f"Major version available: {name} {current} → {latest}")
            if force_major or self.assume_yes:
                _log_info(f"Updating {name} to {latest} (--force or --yes)")
                _install_python_package(f"{name}=={latest}", self.lock, uv, pip)
            else:
                answer = _prompt_yes_no(
                    f"Update {name} from {current} to {latest} (MAJOR)? [y/N] "
                )
                if answer:
                    _install_python_package(f"{name}=={latest}", self.lock, uv, pip)
                else:
                    _log_info(f"Skipped major update for {name}")

        # npm update
        web_dir = self.agent_root / "web"
        if npm and web_dir.is_dir() and (web_dir / "package.json").exists():
            outdated = _npm_outdated_safe(web_dir, npm)
            safe_npm = []
            for pkg_name, info in outdated.items():
                current = info.get("current", "")
                wanted  = info.get("wanted", "")
                latest  = info.get("latest", "")
                if _is_major_bump(current, latest) and not force_major:
                    _log_warn(f"npm major: {pkg_name} {current} → {latest} (skipped)")
                elif wanted and wanted != current:
                    safe_npm.append(pkg_name)

            if safe_npm:
                _log_info(f"Updating {len(safe_npm)} npm package(s)...")
                try:
                    subprocess.run(
                        [npm, "update"] + safe_npm,
                        cwd=str(web_dir), timeout=120, check=False
                    )
                    _log_ok(f"npm packages updated: {', '.join(safe_npm)}")
                except Exception as exc:
                    _log_warn(f"npm update failed: {exc}")

        self.lock.mark_update_checked()

        print(f"\n{_G}[OK]{_NC}   NasTech update complete", file=sys.stderr)
        return 0

    # ── Doctor command ───────────────────────────────────────────────────────

    def run_doctor(self) -> int:
        """
        Prints a structured self-diagnostic report.
        Returns 0 if healthy, 1 if issues found.
        """
        issues = 0
        print(f"\n{_CY}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{_NC}", file=sys.stderr)
        print(f"{_CY}  NasTech Doctor — Dependency Health Report{_NC}", file=sys.stderr)
        print(f"{_CY}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{_NC}\n", file=sys.stderr)

        # Python version
        pv = sys.version_info
        ok = pv >= (3, 11)
        _print_check("Python 3.11+", ok, f"{pv.major}.{pv.minor}.{pv.micro}")
        if not ok:
            issues += 1

        # Core Python packages
        for pkg in PYTHON_CORE_PACKAGES:
            ver = _python_package_version(pkg)
            _print_check(f"  pip: {pkg}", ver is not None, ver or "MISSING")
            if ver is None:
                issues += 1

        # nastech_cli
        ver = _python_package_version("nastech-agent") or _python_package_version("nastech_cli")
        _print_check("nastech_cli (nastech-agent)", ver is not None, ver or "NOT INSTALLED")
        if ver is None:
            issues += 1

        # venv
        venv = os.environ.get("VIRTUAL_ENV")
        _print_check("Virtual environment", bool(venv), venv or "none active")

        # .env file
        env_file = self.nastech_home / ".env"
        _print_check(".env config file", env_file.exists(), str(env_file))

        # Network
        online = self.is_online()
        _print_check("Network connectivity", online, "online" if online else "OFFLINE")

        # npm
        npm = self._npm()
        _print_check("npm", npm is not None, npm or "NOT FOUND")

        # uv
        uv = self._uv()
        _print_check("uv (fast pip)", uv is not None, uv or "not found (optional)")

        # Lock file
        first_run = self.lock.first_run_complete()
        _print_check("First-run bootstrap", first_run, "complete" if first_run else "NOT RUN — run nastech")

        print("", file=sys.stderr)
        if issues == 0:
            print(f"{_G}[OK]{_NC}   All checks passed — NasTech is healthy", file=sys.stderr)
        else:
            print(f"{_Y}[WARN]{_NC} {issues} issue(s) found — run  nastech update  to fix", file=sys.stderr)

        return 0 if issues == 0 else 1


def _print_check(label: str, ok: bool, detail: str) -> None:
    icon = f"{_G}✓{_NC}" if ok else f"{_R}✗{_NC}"
    print(f"  {icon}  {label:<35} {detail}", file=sys.stderr)


def _prompt_yes_no(prompt: str) -> bool:
    """Prompt user for y/n. Returns False if stdin is not a tty."""
    if not sys.stdin.isatty():
        return False
    try:
        answer = input(prompt).strip().lower()
        return answer in {"y", "yes"}
    except (EOFError, KeyboardInterrupt):
        return False


# ─────────────────────────────────────────────────────────────────────────────
# Standalone entrypoint  (python3 nastech_deps.py [check|install|update|doctor])
# ─────────────────────────────────────────────────────────────────────────────

def main() -> int:
    import argparse
    parser = argparse.ArgumentParser(prog="nastech-deps", description="NasTech dependency manager")
    parser.add_argument("command", nargs="?", default="check",
                        choices=["check", "install", "update", "doctor"],
                        help="check | install | update | doctor")
    parser.add_argument("--debug", "-v", action="store_true")
    parser.add_argument("--yes", "-y", action="store_true", dest="assume_yes")
    parser.add_argument("--force", action="store_true", help="Force update even if checked recently")
    args = parser.parse_args()

    dm = DependencyManager(debug=args.debug, assume_yes=args.assume_yes)

    if args.command == "check":
        missing, _ = dm.check_python_deps()
        if missing:
            print(f"{_Y}Missing:{_NC} {', '.join(_extract_package_name(s) for s in missing)}")
            return 1
        _log_ok("All Python deps present")
        return 0

    if args.command == "install":
        ok = dm.run_boot_check()
        return 0 if ok else 1

    if args.command == "update":
        return dm.run_update(force_major=args.force)

    if args.command == "doctor":
        return dm.run_doctor()

    return 0


if __name__ == "__main__":
    sys.exit(main())
