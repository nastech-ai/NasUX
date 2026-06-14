#!/usr/bin/env bash
# ============================================================================
# NasTech Agent — Service Boot Script
# Powered by NasTech AI / NasUX
# ============================================================================
# Boot sequence:
#   1. Core config loader  (venv + env)
#   2. Logger system
#   3. AI engine (nastech_cli / nastech_launcher.py)
#   4. Command registry
#   5. Gateway services
#   6. UI (Vite dev server, optional)
#
# Usage:
#   ./start.sh              — start all services
#   ./start.sh --debug      — verbose boot logs
#   ./start.sh --no-ui      — skip the Vite frontend
#   ./start.sh --no-runner  — skip GitHub Actions self-hosted runner
# ============================================================================

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Parse flags ───────────────────────────────────────────────────────────────
DEBUG=0
NO_UI=0
NO_RUNNER=0
for arg in "$@"; do
  case "$arg" in
    --debug|--verbose|-v) DEBUG=1 ;;
    --no-ui)              NO_UI=1 ;;
    --no-runner)          NO_RUNNER=1 ;;
  esac
done

# ── Colors ────────────────────────────────────────────────────────────────────
if [ -t 2 ]; then
  GREEN='\033[0;32m'; YELLOW='\033[0;33m'; CYAN='\033[0;36m'
  RED='\033[0;31m'; BOLD='\033[1m'; NC='\033[0m'
else
  GREEN=''; YELLOW=''; CYAN=''; RED=''; BOLD=''; NC=''
fi

# ── Logging helpers ───────────────────────────────────────────────────────────
_BOOT_T0=$(date +%s%3N 2>/dev/null || echo "0")

boot_log() {
  if [ "$DEBUG" -eq 1 ]; then
    local now; now=$(date +%s%3N 2>/dev/null || echo "0")
    local elapsed=$(( now - _BOOT_T0 ))
    echo -e "${CYAN}[BOOT +${elapsed}ms]${NC} $1" >&2
  fi
}

ok_log()   { echo -e "${GREEN}[OK]${NC}   $1" >&2; }
warn_log() { echo -e "${YELLOW}[WARN]${NC} $1" >&2; }
err_log()  { echo -e "${RED}[ERROR]${NC} $1" >&2; }
info_log() { echo -e "${CYAN}[INFO]${NC} $1" >&2; }

# ── PID tracking (for cleanup on exit) ───────────────────────────────────────
declare -a _PIDS=()

_cleanup() {
  if [ "${#_PIDS[@]}" -gt 0 ]; then
    info_log "Shutting down services (PIDs: ${_PIDS[*]})..."
    for pid in "${_PIDS[@]}"; do
      kill "$pid" 2>/dev/null || true
    done
  fi
}
trap _cleanup EXIT INT TERM

# ============================================================================
# STEP 1 — Core config loader: Python venv
# ============================================================================
boot_log "Step 1: Core config loader — setting up Python venv..."

if [ ! -d ".venv" ]; then
  info_log "Creating Python virtual environment..."
  if command -v uv >/dev/null 2>&1; then
    uv venv .venv --python 3.11 || {
      err_log "uv venv creation failed — falling back to python3 -m venv"
      python3 -m venv .venv || { err_log "Cannot create venv. Install Python 3.11+"; exit 1; }
    }
  else
    python3 -m venv .venv || { err_log "Cannot create venv. Install Python 3.11+"; exit 1; }
  fi
  ok_log "Virtual environment created"
fi

# Activate venv
if [ -f ".venv/bin/activate" ]; then
  # shellcheck source=/dev/null
  source .venv/bin/activate
  boot_log "Venv activated: .venv"
else
  warn_log ".venv/bin/activate not found — skipping venv activation"
fi

# ============================================================================
# STEP 2 — Logger system: install deps if needed
# ============================================================================
boot_log "Step 2: Logger system — checking dependencies..."

_check_deps() {
  python3 -c "import fastapi, uvicorn, yaml" 2>/dev/null
}

if ! _check_deps; then
  info_log "Installing Python dependencies..."
  if command -v uv >/dev/null 2>&1; then
    uv pip install -e ".[all,dev]" 2>&1 | tail -5 || {
      warn_log "uv install failed — trying pip"
      pip install -e ".[all,dev]" 2>&1 | tail -5 || {
        warn_log "Full install failed — trying minimal install"
        pip install fastapi uvicorn pyyaml 2>&1 | tail -5 || true
      }
    }
  else
    pip install -e ".[all,dev]" 2>&1 | tail -5 || {
      warn_log "pip install failed — trying minimal packages"
      pip install fastapi uvicorn pyyaml 2>&1 | tail -5 || true
    }
  fi
fi

if _check_deps; then
  ok_log "Core dependencies available"
else
  warn_log "Some dependencies may be missing — services may not start correctly"
fi

# ============================================================================
# STEP 3 — AI engine: load .env
# ============================================================================
boot_log "Step 3: Initializing AI engine — loading environment..."

NASTECH_HOME="${NASTECH_HOME:-$HOME/.nastech}"
ENV_FILE="$NASTECH_HOME/.env"

if [ -f "$ENV_FILE" ]; then
  boot_log "Loading $ENV_FILE"
  # Export vars defined in .env (skip comments and blank lines)
  set -a
  # shellcheck source=/dev/null
  source "$ENV_FILE" 2>/dev/null || true
  set +a
  ok_log "Environment loaded from $ENV_FILE"
else
  warn_log "No .env found at $ENV_FILE — AI features require API keys"
  warn_log "Create one with:  nastech setup"
fi

export NASTECH_HOME
export NASTECH_WEB_DIST="${NASTECH_WEB_DIST:-${SCRIPT_DIR}/nastech_cli/web_dist}"

if [ "$DEBUG" -eq 1 ]; then
  export NASTECH_DEBUG=1
  export NASTECH_LOG_LEVEL=DEBUG
  boot_log "Debug mode enabled — verbose logging active"
fi

# ============================================================================
# STEP 4 — Command registry: verify nastech CLI is available
# ============================================================================
boot_log "Step 4: Command registry — verifying nastech CLI..."

_NASTECH_CMD=""
if command -v nastech >/dev/null 2>&1; then
  _NASTECH_CMD="$(command -v nastech)"
  boot_log "nastech command found at $_NASTECH_CMD"
elif [ -f "$SCRIPT_DIR/nastech" ]; then
  _NASTECH_CMD="$SCRIPT_DIR/nastech"
  boot_log "Using local nastech script at $_NASTECH_CMD"
fi

if [ -z "$_NASTECH_CMD" ]; then
  warn_log "nastech command not found in PATH — run install.sh first"
else
  ok_log "Command registry ready  ($_NASTECH_CMD)"
fi

# ============================================================================
# STEP 5 — Gateway services: backend API on port 9119
# ============================================================================
boot_log "Step 5: Gateway services — starting backend on port 9119..."

_start_backend() {
  python3 -m nastech_cli.main dashboard \
    --no-open --insecure \
    --host 127.0.0.1 --port 9119 \
    2>&1 &
  local pid=$!
  echo "$pid"
}

BACKEND_PID=""
if python3 -c "import nastech_cli" 2>/dev/null; then
  BACKEND_PID=$(_start_backend)
  _PIDS+=("$BACKEND_PID")
  boot_log "Backend starting (PID $BACKEND_PID) — waiting for readiness..."

  # Wait up to 10s for backend to be ready
  _backend_ready=0
  for i in $(seq 1 20); do
    sleep 0.5
    if python3 -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:9119/health', timeout=1)" 2>/dev/null; then
      _backend_ready=1
      break
    fi
    # Also accept if the process is still running (may not have /health)
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
      err_log "Backend process exited early (PID $BACKEND_PID)"
      break
    fi
  done

  if [ "$_backend_ready" -eq 1 ]; then
    ok_log "Backend ready on http://127.0.0.1:9119"
  else
    warn_log "Backend did not respond to /health within 10s — it may still be loading"
    ok_log "Backend started (PID $BACKEND_PID) on http://127.0.0.1:9119"
  fi
else
  warn_log "nastech_cli not installed — skipping backend service"
  warn_log "Run:  bash $SCRIPT_DIR/install.sh  to install"
fi

# ============================================================================
# STEP 5b — GitHub Actions self-hosted runner (optional)
# ============================================================================
if [ "$NO_RUNNER" -eq 0 ]; then
  RUNNER_DIR="${SCRIPT_DIR}/actions-runner"
  if [ -d "$RUNNER_DIR" ] && [ -f "$RUNNER_DIR/run.sh" ]; then
    boot_log "Starting GitHub Actions self-hosted runner..."
    export LD_LIBRARY_PATH="/home/runner/.nix-profile/lib:${LD_LIBRARY_PATH:-}"
    export RUNNER_ALLOW_RUNASROOT=1
    (cd "$RUNNER_DIR" && nohup ./run.sh > /tmp/nastech-runner.log 2>&1) &
    RUNNER_PID=$!
    _PIDS+=("$RUNNER_PID")
    ok_log "Actions runner started (PID $RUNNER_PID) — logs: /tmp/nastech-runner.log"
  else
    boot_log "Actions runner not found at $RUNNER_DIR — skipping"
  fi
fi

# ============================================================================
# STEP 6 — UI (Vite frontend, optional)
# ============================================================================

if [ "$NO_UI" -eq 1 ]; then
  info_log "Skipping UI (--no-ui flag)"
elif [ ! -d "$SCRIPT_DIR/web" ]; then
  boot_log "No web/ directory found — skipping Vite frontend"
else
  boot_log "Step 6: UI — starting Vite dev server on port 5000..."

  if [ ! -d "$SCRIPT_DIR/node_modules" ]; then
    warn_log "node_modules not found — running npm install..."
    if command -v npm >/dev/null 2>&1; then
      npm install --prefix "$SCRIPT_DIR" 2>&1 | tail -5 || warn_log "npm install failed"
    else
      warn_log "npm not found — skipping frontend"
      NO_UI=1
    fi
  fi

  if [ "$NO_UI" -eq 0 ] && [ -d "$SCRIPT_DIR/node_modules" ]; then
    cd "$SCRIPT_DIR/web"
    exec node "../node_modules/.bin/vite" --host 0.0.0.0 --port 5000
    # exec replaces this process — cleanup trap handles the backend PID
  fi
fi

# ============================================================================
# READY — all background services are up; wait for them
# ============================================================================

echo ""
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${GREEN}  [OK] NasUX Ready — Powered by NasTech AI${NC}"
echo -e "${BOLD}${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
[ -n "$BACKEND_PID" ] && echo -e "  Dashboard:  ${CYAN}http://127.0.0.1:9119${NC}"
echo -e "  CLI:        ${CYAN}nastech --help${NC}"
[ "$DEBUG" -eq 1 ] && echo -e "  Debug mode: ${YELLOW}ON${NC}"
echo ""

# Wait for all background services
if [ "${#_PIDS[@]}" -gt 0 ]; then
  info_log "Waiting on services (PIDs: ${_PIDS[*]}) — press Ctrl+C to stop"
  wait "${_PIDS[@]}" 2>/dev/null || true
else
  info_log "No background services started. Use  nastech  to interact."
fi
