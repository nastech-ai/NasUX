#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Python virtual environment ────────────────────────────────────────────────
if [ ! -d ".venv" ]; then
  echo "Creating Python virtual environment..."
  uv venv .venv --python 3.11
fi
source .venv/bin/activate

# Install Python deps if not yet installed
if ! python3 -c "import fastapi, uvicorn" 2>/dev/null; then
  echo "Installing Python dependencies..."
  uv pip install -e ".[all,dev]"
fi

# ── GitHub Actions self-hosted runner ─────────────────────────────────────────
RUNNER_DIR="${SCRIPT_DIR}/actions-runner"
if [ -d "$RUNNER_DIR" ] && [ -f "$RUNNER_DIR/run.sh" ]; then
  echo "Starting GitHub Actions self-hosted runner..."
  export LD_LIBRARY_PATH="/home/runner/.nix-profile/lib:${LD_LIBRARY_PATH:-}"
  export RUNNER_ALLOW_RUNASROOT=1
  (cd "$RUNNER_DIR" && nohup ./run.sh > /tmp/nastech-runner.log 2>&1) &
  RUNNER_PID=$!
  echo "Runner started (PID $RUNNER_PID) — logs: /tmp/nastech-runner.log"
else
  echo "Runner not found at $RUNNER_DIR — skipping"
fi

# ── Backend: NasTech dashboard API on port 9119 ───────────────────────────────
# NASTECH_WEB_DIST is set so the backend skips rebuilding the frontend;
# the Vite dev server handles the UI in development.
export NASTECH_WEB_DIST="${SCRIPT_DIR}/nastech_cli/web_dist"

python3 -m nastech_cli.main dashboard \
  --no-open --insecure \
  --host 127.0.0.1 --port 9119 &
BACKEND_PID=$!

echo "Backend started (PID $BACKEND_PID) on http://127.0.0.1:9119"
sleep 2

# ── Frontend: Vite dev server on port 5000 ────────────────────────────────────
cd web
exec node ../node_modules/.bin/vite --host 0.0.0.0 --port 5000
