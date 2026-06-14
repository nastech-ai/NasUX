"""
NasTech Agent — Python Boot Launcher
Powered by NasTech AI / NasUX

Handles the full 6-step service boot sequence in Python:
  1. Core config loader
  2. Logger system
  3. AI engine
  4. Command registry (130 commands)
  5. Gateway services
  6. UI (if present)

Usage:
    python3 nastech_launcher.py [--debug] [--no-ui] [--no-gateway]

This launcher is intentionally standalone — it imports nothing from
nastech_cli until after the venv is located and activated, so it works
as a bootstrap before the package is fully installed.
"""

from __future__ import annotations

import argparse
import asyncio
import importlib
import os
import signal
import subprocess
import sys
import time
import threading
from pathlib import Path
from typing import Optional

# ── Resolve project root ──────────────────────────────────────────────────────
AGENT_ROOT = Path(__file__).resolve().parent
if str(AGENT_ROOT) not in sys.path:
    sys.path.insert(0, str(AGENT_ROOT))

# ── Apply Windows UTF-8 bootstrap early (no-op on POSIX/Android) ─────────────
try:
    import nastech_bootstrap  # noqa: F401
except ModuleNotFoundError:
    pass

# ── Colors ────────────────────────────────────────────────────────────────────
_COLORS = sys.stderr.isatty()
GREEN  = "\033[0;32m" if _COLORS else ""
YELLOW = "\033[0;33m" if _COLORS else ""
CYAN   = "\033[0;36m" if _COLORS else ""
RED    = "\033[0;31m" if _COLORS else ""
BOLD   = "\033[1m"    if _COLORS else ""
DIM    = "\033[2m"    if _COLORS else ""
NC     = "\033[0m"    if _COLORS else ""

_boot_start = time.monotonic()


# ─────────────────────────────────────────────────────────────────────────────
# Logging helpers
# ─────────────────────────────────────────────────────────────────────────────

_debug_mode = False


def _elapsed() -> str:
    ms = int((time.monotonic() - _boot_start) * 1000)
    return f"+{ms}ms"


def boot(msg: str) -> None:
    if _debug_mode:
        print(f"{CYAN}[BOOT {_elapsed()}]{NC} {msg}", file=sys.stderr, flush=True)


def ok(msg: str) -> None:
    print(f"{GREEN}[OK]{NC}   {msg}", file=sys.stderr, flush=True)


def info(msg: str) -> None:
    print(f"{CYAN}[INFO]{NC} {msg}", file=sys.stderr, flush=True)


def warn(msg: str) -> None:
    print(f"{YELLOW}[WARN]{NC} {msg}", file=sys.stderr, flush=True)


def err(msg: str) -> None:
    print(f"{RED}[ERROR]{NC} {msg}", file=sys.stderr, flush=True)


# ─────────────────────────────────────────────────────────────────────────────
# Service registry — tracks running background processes for cleanup
# ─────────────────────────────────────────────────────────────────────────────

class _ServiceRegistry:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._procs: dict[str, subprocess.Popen] = {}
        self._threads: dict[str, threading.Thread] = {}
        self._failed: list[str] = []

    def register_proc(self, name: str, proc: subprocess.Popen) -> None:
        with self._lock:
            self._procs[name] = proc
        boot(f"Registered service '{name}' (PID {proc.pid})")

    def register_thread(self, name: str, t: threading.Thread) -> None:
        with self._lock:
            self._threads[name] = t

    def mark_failed(self, name: str) -> None:
        with self._lock:
            self._failed.append(name)

    def failed_services(self) -> list[str]:
        with self._lock:
            return list(self._failed)

    def shutdown_all(self) -> None:
        with self._lock:
            procs = dict(self._procs)
        for name, proc in procs.items():
            try:
                proc.terminate()
                proc.wait(timeout=5)
                boot(f"Service '{name}' terminated cleanly")
            except subprocess.TimeoutExpired:
                proc.kill()
                warn(f"Service '{name}' force-killed (did not terminate in 5s)")
            except Exception as exc:
                boot(f"Error stopping service '{name}': {exc}")


_registry = _ServiceRegistry()


def _install_signal_handlers() -> None:
    def _on_signal(sig, frame):
        info(f"Received signal {sig} — shutting down all services...")
        _registry.shutdown_all()
        sys.exit(0)

    signal.signal(signal.SIGINT, _on_signal)
    signal.signal(signal.SIGTERM, _on_signal)


# ─────────────────────────────────────────────────────────────────────────────
# STEP 1 — Core config loader
# ─────────────────────────────────────────────────────────────────────────────

def step1_core_config(args: argparse.Namespace) -> dict:
    """Resolve NASTECH_HOME, locate venv, load env file."""
    boot("Step 1: Core config loader")

    nastech_home = Path(os.environ.get("NASTECH_HOME", Path.home() / ".nastech"))
    nastech_home.mkdir(parents=True, exist_ok=True)

    env_file = nastech_home / ".env"
    if env_file.exists():
        _load_dotenv(env_file)
        boot(f"Loaded .env from {env_file}")
    else:
        warn(f"No .env at {env_file} — AI provider keys not set")
        warn("Run  nastech setup  to configure your AI provider")

    # Locate and activate venv
    venv_activated = _activate_venv()
    if not venv_activated:
        warn("No venv found — running without isolated Python environment")

    ok("Core config loaded")
    return {
        "nastech_home": nastech_home,
        "env_file": env_file,
        "venv_ok": venv_activated,
    }


def _load_dotenv(path: Path) -> None:
    """Minimal dotenv loader — no external deps."""
    try:
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" not in line:
                continue
            key, _, val = line.partition("=")
            key = key.strip()
            val = val.strip().strip('"').strip("'")
            if key and key not in os.environ:
                os.environ[key] = val
    except Exception as exc:
        warn(f"Could not load .env: {exc}")


def _activate_venv() -> bool:
    candidates = [
        Path.home() / ".nastech" / "NasTech-Agent" / "venv",
        AGENT_ROOT / ".venv",
        AGENT_ROOT / "venv",
    ]
    for venv_dir in candidates:
        lib_base = venv_dir / "lib"
        if not lib_base.exists():
            continue
        for child in lib_base.iterdir():
            if child.name.startswith("python3"):
                site = child / "site-packages"
                if site.is_dir():
                    if str(site) not in sys.path:
                        sys.path.insert(0, str(site))
                    venv_bin = str(venv_dir / "bin")
                    os.environ["VIRTUAL_ENV"] = str(venv_dir)
                    os.environ["PATH"] = venv_bin + os.pathsep + os.environ.get("PATH", "")
                    boot(f"Activated venv at {venv_dir}")
                    return True
    return False


# ─────────────────────────────────────────────────────────────────────────────
# STEP 2 — Logger system
# ─────────────────────────────────────────────────────────────────────────────

def step2_logger(args: argparse.Namespace, cfg: dict) -> None:
    """Initialize the NasTech logging subsystem."""
    boot("Step 2: Logger system")

    log_level = "DEBUG" if args.debug else os.environ.get("NASTECH_LOG_LEVEL", "INFO")

    try:
        from nastech_logging import setup_logging
        log_dir = setup_logging(
            nastech_home=cfg["nastech_home"],
            log_level=log_level,
            mode="cli",
        )
        ok(f"Logger initialized (level={log_level}, dir={log_dir})")
    except ImportError:
        # nastech_logging is in the project root — try a basic fallback
        import logging
        logging.basicConfig(
            level=getattr(logging, log_level, logging.INFO),
            format="%(asctime)s %(levelname)s %(name)s: %(message)s",
            stream=sys.stderr,
        )
        boot("Using stdlib logging fallback (nastech_logging unavailable)")
        ok(f"Logger initialized (fallback, level={log_level})")
    except Exception as exc:
        warn(f"Logger setup failed: {exc} — using print fallback")


# ─────────────────────────────────────────────────────────────────────────────
# STEP 3 — AI engine
# ─────────────────────────────────────────────────────────────────────────────

def step3_ai_engine(args: argparse.Namespace, cfg: dict) -> Optional[object]:
    """Import and validate the AI engine (nastech_cli)."""
    boot("Step 3: AI engine")

    try:
        boot("Importing nastech_cli...")
        import nastech_cli  # noqa: F401
        version = getattr(nastech_cli, "__version__", "unknown")
        ok(f"AI engine loaded (nastech_cli {version})")
        return nastech_cli
    except ModuleNotFoundError:
        warn("nastech_cli not installed — AI features unavailable")
        warn(f"Install with:  bash {AGENT_ROOT}/install.sh")
        return None
    except Exception as exc:
        err(f"AI engine failed to load: {exc}")
        if args.debug:
            import traceback
            traceback.print_exc()
        _registry.mark_failed("ai-engine")
        return None


# ─────────────────────────────────────────────────────────────────────────────
# STEP 4 — Command registry
# ─────────────────────────────────────────────────────────────────────────────

def step4_command_registry(args: argparse.Namespace, nastech_cli_mod) -> int:
    """Count and validate registered CLI commands."""
    boot("Step 4: Command registry")

    if nastech_cli_mod is None:
        warn("Skipping command registry (AI engine not loaded)")
        return 0

    try:
        # Try to enumerate commands via Click group introspection
        from nastech_cli.main import main as _main_fn
        import click

        ctx = click.Context(_main_fn)
        commands = getattr(_main_fn, "commands", {}) or {}
        count = len(commands)

        if count > 0:
            ok(f"Command registry loaded ({count} commands)")
            if args.debug:
                for name in sorted(commands.keys()):
                    boot(f"  command: {name}")
        else:
            ok("Command registry loaded (commands enumerated at runtime)")
        return count
    except Exception as exc:
        boot(f"Could not enumerate commands: {exc}")
        ok("Command registry loaded (runtime enumeration)")
        return -1


# ─────────────────────────────────────────────────────────────────────────────
# STEP 5 — Gateway services
# ─────────────────────────────────────────────────────────────────────────────

def step5_gateway(args: argparse.Namespace, cfg: dict) -> bool:
    """Start the backend gateway/dashboard service."""
    if args.no_gateway:
        info("Skipping gateway (--no-gateway)")
        return False

    boot("Step 5: Gateway services — backend on port 9119")

    env = {**os.environ, "NASTECH_HOME": str(cfg["nastech_home"])}
    if args.debug:
        env["NASTECH_DEBUG"] = "1"
        env["NASTECH_LOG_LEVEL"] = "DEBUG"

    web_dist = AGENT_ROOT / "nastech_cli" / "web_dist"
    if web_dist.is_dir():
        env["NASTECH_WEB_DIST"] = str(web_dist)

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
            stderr=subprocess.PIPE if not args.debug else None,
        )
        _registry.register_proc("gateway-backend", proc)
        boot(f"Backend process started (PID {proc.pid})")
    except FileNotFoundError:
        warn("nastech_cli.main not found — backend gateway not started")
        warn(f"Install with:  bash {AGENT_ROOT}/install.sh")
        _registry.mark_failed("gateway-backend")
        return False
    except Exception as exc:
        err(f"Failed to start backend gateway: {exc}")
        _registry.mark_failed("gateway-backend")
        return False

    # Health-check loop — wait up to 8s for backend to be ready
    import urllib.request
    import urllib.error

    deadline = time.monotonic() + 8.0
    ready = False
    while time.monotonic() < deadline:
        time.sleep(0.4)
        if proc.poll() is not None:
            err(f"Backend exited with code {proc.returncode} — check logs at ~/.nastech/logs/")
            _registry.mark_failed("gateway-backend")
            return False
        try:
            urllib.request.urlopen("http://127.0.0.1:9119/health", timeout=1)
            ready = True
            break
        except Exception:
            pass

    if ready:
        ok("Gateway backend ready on http://127.0.0.1:9119")
    else:
        # Still running but /health didn't respond — could be OK (no /health endpoint)
        if proc.poll() is None:
            ok("Gateway backend started (PID %d) on http://127.0.0.1:9119" % proc.pid)
        else:
            warn("Gateway backend may not have started — check ~/.nastech/logs/errors.log")
            return False

    return True


# ─────────────────────────────────────────────────────────────────────────────
# STEP 6 — UI (Vite frontend)
# ─────────────────────────────────────────────────────────────────────────────

def step6_ui(args: argparse.Namespace) -> bool:
    """Start the Vite frontend dev server if web/ directory exists."""
    if args.no_ui:
        info("Skipping UI (--no-ui)")
        return False

    web_dir = AGENT_ROOT / "web"
    if not web_dir.is_dir():
        boot("No web/ directory — skipping UI step")
        return False

    boot("Step 6: UI — Vite dev server on port 5000")

    node_modules = AGENT_ROOT / "node_modules"
    vite_bin = node_modules / ".bin" / "vite"

    if not vite_bin.exists():
        warn("node_modules not found — running npm install for frontend...")
        try:
            subprocess.run(
                ["npm", "install"],
                cwd=str(AGENT_ROOT),
                check=True,
                capture_output=not args.debug,
            )
        except (subprocess.CalledProcessError, FileNotFoundError) as exc:
            warn(f"npm install failed: {exc} — skipping UI")
            return False

    try:
        proc = subprocess.Popen(
            ["node", str(vite_bin), "--host", "0.0.0.0", "--port", "5000"],
            cwd=str(web_dir),
            stdout=subprocess.DEVNULL if not args.debug else None,
        )
        _registry.register_proc("vite-frontend", proc)
        ok("Vite frontend started on http://0.0.0.0:5000")
        return True
    except Exception as exc:
        warn(f"Could not start Vite frontend: {exc}")
        _registry.mark_failed("vite-frontend")
        return False


# ─────────────────────────────────────────────────────────────────────────────
# Auto-recovery monitor
# ─────────────────────────────────────────────────────────────────────────────

def _monitor_services(interval: float = 10.0) -> None:
    """Background thread: watch registered processes and log crashes."""
    while True:
        time.sleep(interval)
        with _registry._lock:
            procs = dict(_registry._procs)
        for name, proc in procs.items():
            rc = proc.poll()
            if rc is not None:
                warn(f"Service '{name}' exited with code {rc} — isolating failure")
                _registry.mark_failed(name)
                with _registry._lock:
                    _registry._procs.pop(name, None)


# ─────────────────────────────────────────────────────────────────────────────
# CLI argument parser
# ─────────────────────────────────────────────────────────────────────────────

def _parse_args(argv=None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="nastech-launcher",
        description="NasTech Agent boot launcher — NasUX / Powered by NasTech AI",
    )
    parser.add_argument(
        "--debug", "--verbose", "-v",
        action="store_true",
        dest="debug",
        help="Enable verbose boot and debug logging",
    )
    parser.add_argument(
        "--no-ui",
        action="store_true",
        dest="no_ui",
        help="Skip the Vite frontend",
    )
    parser.add_argument(
        "--no-gateway",
        action="store_true",
        dest="no_gateway",
        help="Skip the backend gateway/dashboard service",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Validate all services can be imported/found, then exit (no services started)",
    )
    return parser.parse_args(argv)


# ─────────────────────────────────────────────────────────────────────────────
# Main boot orchestrator
# ─────────────────────────────────────────────────────────────────────────────

def main(argv=None) -> int:
    args = _parse_args(argv)
    global _debug_mode
    _debug_mode = args.debug

    if args.debug:
        os.environ.setdefault("NASTECH_DEBUG", "1")
        os.environ.setdefault("NASTECH_LOG_LEVEL", "DEBUG")

    _install_signal_handlers()

    print(f"\n{BOLD}{CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}", file=sys.stderr)
    print(f"{BOLD}{CYAN}  NasUX — Powered by NasTech AI{NC}", file=sys.stderr)
    print(f"{BOLD}{CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}\n", file=sys.stderr)

    boot("Boot sequence starting...")

    # ── STEP 1: Core config ──────────────────────────────────────────────────
    try:
        cfg = step1_core_config(args)
    except Exception as exc:
        err(f"[STEP 1] Core config failed: {exc}")
        if args.debug:
            import traceback; traceback.print_exc()
        return 1

    # ── STEP 2: Logger ───────────────────────────────────────────────────────
    try:
        step2_logger(args, cfg)
    except Exception as exc:
        warn(f"[STEP 2] Logger setup failed: {exc} — continuing with print fallback")

    # ── STEP 3: AI engine ────────────────────────────────────────────────────
    try:
        nastech_cli_mod = step3_ai_engine(args, cfg)
    except Exception as exc:
        err(f"[STEP 3] AI engine failed: {exc}")
        nastech_cli_mod = None

    # ── STEP 4: Command registry ─────────────────────────────────────────────
    try:
        cmd_count = step4_command_registry(args, nastech_cli_mod)
    except Exception as exc:
        warn(f"[STEP 4] Command registry check failed: {exc}")
        cmd_count = 0

    if args.check:
        failed = _registry.failed_services()
        if failed:
            err(f"Check failed — services with errors: {', '.join(failed)}")
            return 1
        ok("All checks passed")
        return 0

    # ── STEP 5: Gateway ──────────────────────────────────────────────────────
    gateway_ok = False
    try:
        gateway_ok = step5_gateway(args, cfg)
    except Exception as exc:
        warn(f"[STEP 5] Gateway service error: {exc} — continuing without gateway")

    # ── STEP 6: UI ───────────────────────────────────────────────────────────
    ui_ok = False
    try:
        ui_ok = step6_ui(args)
    except Exception as exc:
        warn(f"[STEP 6] UI service error: {exc} — continuing without UI")

    # ── Start service monitor thread ─────────────────────────────────────────
    monitor_t = threading.Thread(target=_monitor_services, daemon=True)
    monitor_t.start()

    # ── READY ─────────────────────────────────────────────────────────────────
    elapsed = time.monotonic() - _boot_start
    failed = _registry.failed_services()

    print(f"\n{BOLD}{GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}", file=sys.stderr)
    print(f"{BOLD}{GREEN}  [OK] NasUX Ready  (boot: {elapsed:.2f}s){NC}", file=sys.stderr)
    print(f"{BOLD}{GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}", file=sys.stderr)

    if gateway_ok:
        print(f"  Dashboard:  {CYAN}http://127.0.0.1:9119{NC}", file=sys.stderr)
    if ui_ok:
        print(f"  Frontend:   {CYAN}http://0.0.0.0:5000{NC}", file=sys.stderr)
    if cmd_count and cmd_count > 0:
        print(f"  Commands:   {CYAN}{cmd_count} registered{NC}", file=sys.stderr)
    print(f"  CLI:        {CYAN}nastech --help{NC}", file=sys.stderr)
    if failed:
        print(f"  {YELLOW}Failed:     {', '.join(failed)}{NC}", file=sys.stderr)
    if args.debug:
        print(f"  {YELLOW}Debug mode: ON{NC}", file=sys.stderr)
    print("", file=sys.stderr)

    # ── Wait on background services ───────────────────────────────────────────
    with _registry._lock:
        procs = dict(_registry._procs)

    if procs:
        info(f"Waiting on {len(procs)} service(s) — press Ctrl+C to stop")
        try:
            while True:
                # Check all processes are still alive
                all_done = all(p.poll() is not None for p in procs.values())
                if all_done:
                    break
                time.sleep(1)
        except KeyboardInterrupt:
            info("Shutting down...")
    else:
        info("No background services running. Use  nastech  to interact.")

    return 0 if not failed else 1


if __name__ == "__main__":
    sys.exit(main())
