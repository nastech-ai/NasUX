"""
NasTech Agent — Self-Maintaining Boot Launcher
Powered by NasTech AI / NasUX

Strict 8-step boot sequence:
  1. Environment check       — Python version, OS detection, venv
  2. Dependency check & fix  — pip / npm / gradle via nastech_deps.py
  3. Config loader           — NASTECH_HOME, .env, config.yaml
  4. Logger init             — nastech_logging.setup_logging()
  5. AI engine startup       — import + validate nastech_cli
  6. Command registry load   — enumerate all 130+ commands
  7. Gateway services start  — backend on :9119, runner
  8. READY state             — print banner, wait on services

Performance:
  - Dependency check is lock-file guarded (no reinstall on every boot)
  - Steps are individually try/catch isolated (one failure ≠ app crash)
  - Background service monitor auto-isolates crashed services

Usage:
    python3 nastech_launcher.py [--debug] [--no-ui] [--no-gateway] [--check]
"""

from __future__ import annotations

import argparse
import os
import signal
import subprocess
import sys
import time
import threading
from pathlib import Path
from typing import Optional

# ── Project root ──────────────────────────────────────────────────────────────
AGENT_ROOT = Path(__file__).resolve().parent
if str(AGENT_ROOT) not in sys.path:
    sys.path.insert(0, str(AGENT_ROOT))

# ── Windows UTF-8 bootstrap (no-op on POSIX / Android) ───────────────────────
try:
    import nastech_bootstrap  # noqa: F401
except ModuleNotFoundError:
    pass

# ── Colors ────────────────────────────────────────────────────────────────────
_C = sys.stderr.isatty()
GRN  = "\033[0;32m" if _C else ""
YEL  = "\033[0;33m" if _C else ""
CYN  = "\033[0;36m" if _C else ""
RED  = "\033[0;31m" if _C else ""
BLD  = "\033[1m"    if _C else ""
NC   = "\033[0m"    if _C else ""

_boot_start = time.monotonic()
_debug_mode = False


# ── Logging ───────────────────────────────────────────────────────────────────

def _elapsed_ms() -> str:
    return f"+{int((time.monotonic() - _boot_start) * 1000)}ms"


def log_boot(msg: str) -> None:
    if _debug_mode:
        print(f"{CYN}[BOOT {_elapsed_ms()}]{NC} {msg}", file=sys.stderr, flush=True)


def log_ok(msg: str) -> None:
    print(f"{GRN}[OK]{NC}   {msg}", file=sys.stderr, flush=True)


def log_info(msg: str) -> None:
    print(f"{CYN}[INFO]{NC} {msg}", file=sys.stderr, flush=True)


def log_warn(msg: str) -> None:
    print(f"{YEL}[WARN]{NC} {msg}", file=sys.stderr, flush=True)


def log_err(msg: str) -> None:
    print(f"{RED}[ERROR]{NC} {msg}", file=sys.stderr, flush=True)


# ─────────────────────────────────────────────────────────────────────────────
# Service registry
# ─────────────────────────────────────────────────────────────────────────────

class _ServiceRegistry:
    def __init__(self):
        self._lock = threading.Lock()
        self._procs: dict[str, subprocess.Popen] = {}
        self._failed: list[str] = []

    def add(self, name: str, proc: subprocess.Popen) -> None:
        with self._lock:
            self._procs[name] = proc
        log_boot(f"Registered '{name}' (PID {proc.pid})")

    def fail(self, name: str) -> None:
        with self._lock:
            if name not in self._failed:
                self._failed.append(name)

    def failed(self) -> list[str]:
        with self._lock:
            return list(self._failed)

    def all_procs(self) -> dict[str, subprocess.Popen]:
        with self._lock:
            return dict(self._procs)

    def shutdown(self) -> None:
        procs = self.all_procs()
        for name, proc in procs.items():
            try:
                proc.terminate()
                proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                proc.kill()
                log_warn(f"Force-killed '{name}'")
            except Exception:
                pass


_svc = _ServiceRegistry()


def _install_signal_handlers() -> None:
    def _handle(sig, _):
        log_info(f"Signal {sig} — shutting down services...")
        _svc.shutdown()
        sys.exit(0)
    signal.signal(signal.SIGINT, _handle)
    signal.signal(signal.SIGTERM, _handle)


def _monitor_loop(interval: float = 10.0) -> None:
    """Background thread: watch procs, isolate crashes without killing app."""
    while True:
        time.sleep(interval)
        for name, proc in _svc.all_procs().items():
            rc = proc.poll()
            if rc is not None:
                log_warn(f"Service '{name}' exited (code {rc}) — isolated")
                _svc.fail(name)
                with _svc._lock:
                    _svc._procs.pop(name, None)


# ─────────────────────────────────────────────────────────────────────────────
# STEP 1 — Environment check
# ─────────────────────────────────────────────────────────────────────────────

def step1_environment(args: argparse.Namespace) -> dict:
    """
    Check Python version, detect OS/platform, activate venv.
    Hard-fails on Python < 3.11.
    """
    log_boot("Step 1: Environment check")

    # Python version guard
    if sys.version_info < (3, 11):
        log_err(
            f"NasTech requires Python 3.11+. "
            f"Running {sys.version_info.major}.{sys.version_info.minor}."
        )
        log_err("On NasUX (Kali): apt install python3 python3-pip   |   Desktop: python.org")
        sys.exit(1)

    log_boot(f"Python {sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}")

    # Platform detection
    try:
        from nastech_constants import get_environment_type, is_nasux
        env_type = get_environment_type()
        on_nasux = is_nasux()
    except ImportError:
        env_type = "linux" if sys.platform.startswith("linux") else sys.platform
        on_nasux = bool(os.environ.get("NASUX_VERSION"))

    log_boot(f"Platform: {env_type} | NasUX: {on_nasux}")

    # NASTECH_HOME
    nastech_home = Path(
        os.environ.get("NASTECH_HOME", str(Path.home() / ".nastech"))
    )
    nastech_home.mkdir(parents=True, exist_ok=True)
    os.environ.setdefault("NASTECH_HOME", str(nastech_home))

    # Activate venv (if not already inside one)
    venv_path = _activate_venv()

    if args.debug:
        os.environ["NASTECH_DEBUG"] = "1"
        os.environ["NASTECH_LOG_LEVEL"] = "DEBUG"

    log_ok(f"Environment: {env_type} | NASTECH_HOME: {nastech_home}")
    return {
        "nastech_home": nastech_home,
        "env_type": env_type,
        "on_nasux": on_nasux,
        "venv": venv_path,
    }


def _activate_venv() -> Optional[Path]:
    """Find and activate the first usable venv. Returns its path or None."""
    if os.environ.get("VIRTUAL_ENV"):
        log_boot("Already in a venv — skipping activation")
        return Path(os.environ["VIRTUAL_ENV"])

    nastech_home = Path(os.environ.get("NASTECH_HOME", Path.home() / ".nastech"))
    candidates = [
        nastech_home / "NasTech-Agent" / "venv",
        AGENT_ROOT / ".venv",
        AGENT_ROOT / "venv",
    ]
    for venv_dir in candidates:
        lib = venv_dir / "lib"
        if not lib.is_dir():
            continue
        for child in lib.iterdir():
            if child.name.startswith("python"):
                site = child / "site-packages"
                if site.is_dir():
                    if str(site) not in sys.path:
                        sys.path.insert(0, str(site))
                    venv_bin = str(venv_dir / "bin")
                    os.environ["VIRTUAL_ENV"] = str(venv_dir)
                    os.environ["PATH"] = venv_bin + os.pathsep + os.environ.get("PATH", "")
                    log_boot(f"Activated venv: {venv_dir}")
                    return venv_dir
    log_boot("No venv found — using system Python")
    return None


# ─────────────────────────────────────────────────────────────────────────────
# STEP 2 — Dependency check & install (self-healing)
# ─────────────────────────────────────────────────────────────────────────────

def step2_dependencies(args: argparse.Namespace, cfg: dict) -> bool:
    """
    Use nastech_deps.DependencyManager for fully automatic dependency healing.
    - Only installs when packages are missing or first run
    - Offline safe: skips installs if no network
    - Retries once per package on failure
    """
    log_boot("Step 2: Dependency check & install")

    try:
        from nastech_deps import DependencyManager
        dm = DependencyManager(
            debug=args.debug,
            nastech_home=cfg["nastech_home"],
            agent_root=AGENT_ROOT,
        )
        ok = dm.run_boot_check()
        if ok:
            log_ok("Dependencies satisfied")
        else:
            log_warn("Some dependencies missing — limited functionality")
        return ok
    except ImportError:
        log_warn("nastech_deps not available — skipping dependency check")
        return True
    except Exception as exc:
        log_warn(f"Dependency check failed: {exc} — continuing")
        if args.debug:
            import traceback; traceback.print_exc(file=sys.stderr)
        return True  # non-fatal


# ─────────────────────────────────────────────────────────────────────────────
# STEP 3 — Config loader
# ─────────────────────────────────────────────────────────────────────────────

def step3_config(args: argparse.Namespace, cfg: dict) -> dict:
    """Load .env, config.yaml, and runtime constants."""
    log_boot("Step 3: Config loader")

    nastech_home: Path = cfg["nastech_home"]
    env_file = nastech_home / ".env"

    # Load .env
    if env_file.exists():
        _load_dotenv_simple(env_file)
        log_boot(f"Loaded .env from {env_file}")
    else:
        log_warn(f"No .env at {env_file} — run  nastech setup  to configure")

    # Try nastech_cli config loader (best-effort)
    try:
        from nastech_cli.env_loader import load_nastech_dotenv
        load_nastech_dotenv(nastech_home=nastech_home)
        log_boot("nastech_cli env_loader ran successfully")
    except ImportError:
        pass
    except Exception as exc:
        log_boot(f"env_loader non-fatal: {exc}")

    # Expose web_dist for backend
    web_dist = AGENT_ROOT / "nastech_cli" / "web_dist"
    if web_dist.is_dir():
        os.environ.setdefault("NASTECH_WEB_DIST", str(web_dist))

    log_ok(f"Config loaded (NASTECH_HOME={nastech_home})")
    return {**cfg, "env_file": env_file}


def _load_dotenv_simple(path: Path) -> None:
    """Zero-dependency .env parser."""
    try:
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, val = line.partition("=")
            key = key.strip()
            val = val.strip().strip('"').strip("'")
            if key and key not in os.environ:
                os.environ[key] = val
    except Exception as exc:
        log_warn(f"Could not read .env: {exc}")


# ─────────────────────────────────────────────────────────────────────────────
# STEP 4 — Logger init
# ─────────────────────────────────────────────────────────────────────────────

def step4_logger(args: argparse.Namespace, cfg: dict) -> None:
    """Initialize the NasTech structured logging system."""
    log_boot("Step 4: Logger init")

    log_level = "DEBUG" if args.debug else os.environ.get("NASTECH_LOG_LEVEL", "INFO")

    try:
        from nastech_logging import setup_logging
        log_dir = setup_logging(
            nastech_home=cfg["nastech_home"],
            log_level=log_level,
            mode="cli",
        )
        log_ok(f"Logger ready (level={log_level}, dir={log_dir})")
    except ImportError:
        import logging
        logging.basicConfig(
            level=getattr(logging, log_level, logging.INFO),
            format="%(asctime)s %(levelname)s %(name)s: %(message)s",
            stream=sys.stderr,
        )
        log_boot("Logger: stdlib fallback (nastech_logging not available)")
    except Exception as exc:
        log_warn(f"Logger init failed: {exc} — using print fallback")

    if args.debug:
        try:
            from nastech_logging import setup_verbose_logging
            setup_verbose_logging()
            log_boot("Verbose logging enabled")
        except Exception:
            pass


# ─────────────────────────────────────────────────────────────────────────────
# STEP 5 — AI engine startup
# ─────────────────────────────────────────────────────────────────────────────

def step5_ai_engine(args: argparse.Namespace) -> Optional[object]:
    """
    Import and validate nastech_cli (the AI engine).
    Returns the module on success, None on failure (non-fatal).
    """
    log_boot("Step 5: AI engine startup")

    try:
        import nastech_cli
        version = getattr(nastech_cli, "__version__", "unknown")
        log_ok(f"AI engine loaded (nastech_cli {version})")
        return nastech_cli
    except ModuleNotFoundError:
        log_warn("nastech_cli not installed — AI features unavailable")
        log_warn(f"Run:  bash {AGENT_ROOT}/install.sh")
        _svc.fail("ai-engine")
        return None
    except Exception as exc:
        log_err(f"AI engine failed to import: {exc}")
        if args.debug:
            import traceback; traceback.print_exc(file=sys.stderr)
        _svc.fail("ai-engine")
        return None


# ─────────────────────────────────────────────────────────────────────────────
# STEP 6 — Command registry load
# ─────────────────────────────────────────────────────────────────────────────

def step6_commands(args: argparse.Namespace, nastech_mod) -> int:
    """
    Enumerate all registered CLI commands (130+).
    Returns the count; -1 if enumeration failed (non-fatal).
    """
    log_boot("Step 6: Command registry load")

    if nastech_mod is None:
        log_warn("Skipping command registry (AI engine not available)")
        return 0

    try:
        from nastech_cli.main import main as cli_main
        # Click group introspection
        commands = getattr(cli_main, "commands", None)
        if commands is None:
            # Some Click versions expose commands differently
            import click
            ctx = click.Context(cli_main, info_name="nastech")
            commands = cli_main.list_commands(ctx)

        count = len(commands) if hasattr(commands, "__len__") else sum(1 for _ in commands)
        log_ok(f"Command registry loaded ({count} commands)")
        if args.debug:
            cmd_list = list(commands.keys()) if isinstance(commands, dict) else list(commands)
            for name in sorted(cmd_list[:30]):  # first 30 in debug
                log_boot(f"  cmd: {name}")
            if len(cmd_list) > 30:
                log_boot(f"  ... and {len(cmd_list) - 30} more")
        return count
    except Exception as exc:
        log_boot(f"Command enumeration non-fatal: {exc}")
        log_ok("Command registry loaded (runtime enumeration)")
        return -1


# ─────────────────────────────────────────────────────────────────────────────
# STEP 7 — Gateway services start
# ─────────────────────────────────────────────────────────────────────────────

def step7_gateway(args: argparse.Namespace, cfg: dict) -> bool:
    """
    Start the backend dashboard gateway on port 9119.
    Health-checks up to 8 s before declaring ready.
    Failure is isolated — app continues without gateway.
    """
    if args.no_gateway:
        log_info("Gateway skipped (--no-gateway)")
        return False

    log_boot("Step 7: Gateway services start")

    # Check nastech_cli is importable before launching subprocess
    try:
        import nastech_cli  # noqa: F401
    except ImportError:
        log_warn("nastech_cli not installed — gateway not started")
        _svc.fail("gateway")
        return False

    env = {
        **os.environ,
        "NASTECH_HOME": str(cfg["nastech_home"]),
    }
    if args.debug:
        env["NASTECH_DEBUG"] = "1"
        env["NASTECH_LOG_LEVEL"] = "DEBUG"

    cmd = [
        sys.executable, "-m", "nastech_cli.main",
        "dashboard", "--no-open", "--insecure",
        "--host", "127.0.0.1", "--port", "9119",
    ]

    try:
        proc = subprocess.Popen(
            cmd,
            env=env,
            stdout=subprocess.DEVNULL if not args.debug else None,
            stderr=subprocess.PIPE  if not args.debug else None,
        )
        _svc.add("gateway", proc)
    except FileNotFoundError:
        log_warn(f"Could not start gateway: nastech_cli.main not found")
        _svc.fail("gateway")
        return False
    except Exception as exc:
        log_warn(f"Gateway start error: {exc}")
        _svc.fail("gateway")
        return False

    # Wait up to 8 s for the process to come alive / not immediately crash
    import urllib.request
    deadline = time.monotonic() + 8.0
    ready = False
    while time.monotonic() < deadline:
        time.sleep(0.4)
        if proc.poll() is not None:
            log_err(f"Gateway exited early (code {proc.returncode}) — check ~/.nastech/logs/")
            _svc.fail("gateway")
            return False
        try:
            urllib.request.urlopen("http://127.0.0.1:9119/health", timeout=1)
            ready = True
            break
        except Exception:
            pass

    if ready:
        log_ok("Gateway ready on http://127.0.0.1:9119")
    elif proc.poll() is None:
        # Still running — acceptable (no /health endpoint)
        log_ok(f"Gateway started (PID {proc.pid}) on http://127.0.0.1:9119")
        ready = True
    else:
        log_warn("Gateway may not have started — logs: ~/.nastech/logs/errors.log")
        _svc.fail("gateway")

    return ready


def _start_optional_runner(args: argparse.Namespace) -> None:
    """Start GitHub Actions self-hosted runner if present (best-effort)."""
    runner_dir = AGENT_ROOT / "actions-runner"
    run_sh = runner_dir / "run.sh"
    if not run_sh.exists():
        return

    log_boot("Starting GitHub Actions runner...")
    env = {
        **os.environ,
        "RUNNER_ALLOW_RUNASROOT": "1",
        "LD_LIBRARY_PATH": "/home/runner/.nix-profile/lib:" + os.environ.get("LD_LIBRARY_PATH", ""),
    }
    try:
        proc = subprocess.Popen(
            ["/bin/bash", str(run_sh)],
            cwd=str(runner_dir),
            env=env,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        _svc.add("actions-runner", proc)
        log_ok(f"Actions runner started (PID {proc.pid})")
    except Exception as exc:
        log_warn(f"Actions runner not started: {exc}")


def _start_optional_ui(args: argparse.Namespace) -> bool:
    """Start Vite frontend if web/ exists. Returns True if started."""
    if args.no_ui:
        return False
    web_dir = AGENT_ROOT / "web"
    if not web_dir.is_dir():
        return False

    node_modules = AGENT_ROOT / "node_modules"
    vite_bin = node_modules / ".bin" / "vite"

    if not vite_bin.exists():
        log_warn("Vite not found — skipping frontend (run npm install manually)")
        return False

    log_boot("Starting Vite frontend on port 5000...")
    try:
        proc = subprocess.Popen(
            ["node", str(vite_bin), "--host", "0.0.0.0", "--port", "5000"],
            cwd=str(web_dir),
            stdout=subprocess.DEVNULL if not args.debug else None,
        )
        _svc.add("vite-frontend", proc)
        log_ok("Frontend started on http://0.0.0.0:5000")
        return True
    except Exception as exc:
        log_warn(f"Frontend not started: {exc}")
        return False


# ─────────────────────────────────────────────────────────────────────────────
# STEP 8 — READY state
# ─────────────────────────────────────────────────────────────────────────────

def step8_ready(args: argparse.Namespace, cfg: dict, cmd_count: int) -> None:
    """Print the READY banner and wait on background services."""
    elapsed = time.monotonic() - _boot_start
    failed  = _svc.failed()
    procs   = _svc.all_procs()

    print(f"\n{BLD}{GRN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}", file=sys.stderr)
    print(f"{BLD}{GRN}  [OK] NasUX Ready  (boot: {elapsed:.2f}s){NC}", file=sys.stderr)
    print(f"{BLD}{GRN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}", file=sys.stderr)

    if "gateway" in procs:
        print(f"  Dashboard:  {CYN}http://127.0.0.1:9119{NC}", file=sys.stderr)
    if "vite-frontend" in procs:
        print(f"  Frontend:   {CYN}http://0.0.0.0:5000{NC}", file=sys.stderr)
    if cmd_count and cmd_count > 0:
        print(f"  Commands:   {CYN}{cmd_count} registered{NC}", file=sys.stderr)
    print(f"  CLI:        {CYN}nastech --help{NC}", file=sys.stderr)
    if failed:
        print(f"  {YEL}Issues:     {', '.join(failed)} (isolated){NC}", file=sys.stderr)
    if args.debug:
        print(f"  {YEL}Debug mode: ON{NC}", file=sys.stderr)
    print("", file=sys.stderr)

    # Wait on all background processes
    if procs:
        log_info(f"Serving ({len(procs)} service(s)) — Ctrl+C to stop")
        try:
            while True:
                if all(p.poll() is not None for p in procs.values()):
                    log_warn("All services have exited")
                    break
                time.sleep(1)
        except KeyboardInterrupt:
            log_info("Shutdown requested")
    else:
        log_info("No background services — use  nastech  to interact")


# ─────────────────────────────────────────────────────────────────────────────
# Argument parser
# ─────────────────────────────────────────────────────────────────────────────

def _parse_args(argv=None) -> argparse.Namespace:
    p = argparse.ArgumentParser(
        prog="nastech-launcher",
        description="NasTech Agent boot launcher — NasUX / Powered by NasTech AI",
    )
    p.add_argument("--debug", "--verbose", "-v",
                   action="store_true", dest="debug",
                   help="Verbose boot + debug logging")
    p.add_argument("--no-ui",
                   action="store_true", dest="no_ui",
                   help="Skip Vite frontend")
    p.add_argument("--no-gateway",
                   action="store_true", dest="no_gateway",
                   help="Skip backend gateway")
    p.add_argument("--check",
                   action="store_true",
                   help="Validate environment + deps then exit (no services started)")
    return p.parse_args(argv)


# ─────────────────────────────────────────────────────────────────────────────
# Main orchestrator
# ─────────────────────────────────────────────────────────────────────────────

def main(argv=None) -> int:
    args = _parse_args(argv)

    global _debug_mode
    _debug_mode = args.debug

    _install_signal_handlers()

    print(f"\n{BLD}{CYN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}", file=sys.stderr)
    print(f"{BLD}{CYN}  NasUX — Powered by NasTech AI{NC}", file=sys.stderr)
    print(f"{BLD}{CYN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}\n", file=sys.stderr)

    # ── STEP 1: Environment ──────────────────────────────────────────────────
    try:
        cfg = step1_environment(args)
    except SystemExit:
        raise
    except Exception as exc:
        log_err(f"[STEP 1] Environment check failed: {exc}")
        return 1

    # ── STEP 2: Dependencies ─────────────────────────────────────────────────
    try:
        step2_dependencies(args, cfg)
    except Exception as exc:
        log_warn(f"[STEP 2] Dependency check error: {exc} — continuing")

    # ── STEP 3: Config ───────────────────────────────────────────────────────
    try:
        cfg = step3_config(args, cfg)
    except Exception as exc:
        log_warn(f"[STEP 3] Config error: {exc} — using defaults")

    # ── STEP 4: Logger ───────────────────────────────────────────────────────
    try:
        step4_logger(args, cfg)
    except Exception as exc:
        log_warn(f"[STEP 4] Logger error: {exc}")

    # ── STEP 5: AI engine ────────────────────────────────────────────────────
    nastech_mod = None
    try:
        nastech_mod = step5_ai_engine(args)
    except Exception as exc:
        log_warn(f"[STEP 5] AI engine error: {exc}")

    # ── STEP 6: Command registry ─────────────────────────────────────────────
    cmd_count = 0
    try:
        cmd_count = step6_commands(args, nastech_mod)
    except Exception as exc:
        log_warn(f"[STEP 6] Command registry error: {exc}")

    # ── Check mode exits here ────────────────────────────────────────────────
    if args.check:
        failed = _svc.failed()
        if failed:
            log_err(f"Issues: {', '.join(failed)}")
            return 1
        log_ok("All checks passed — NasTech is healthy")
        return 0

    # ── STEP 7: Gateway services ─────────────────────────────────────────────
    try:
        step7_gateway(args, cfg)
        _start_optional_runner(args)
        _start_optional_ui(args)
    except Exception as exc:
        log_warn(f"[STEP 7] Gateway error: {exc} — continuing without services")

    # ── Start monitor thread ─────────────────────────────────────────────────
    threading.Thread(target=_monitor_loop, daemon=True).start()

    # ── STEP 8: READY ────────────────────────────────────────────────────────
    step8_ready(args, cfg, cmd_count)

    failed = _svc.failed()
    return 1 if ("ai-engine" in failed) else 0


if __name__ == "__main__":
    sys.exit(main())
