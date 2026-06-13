#!/data/data/com.nastech.nasux/files/usr/bin/bash
# =============================================================================
# NasUX NasTech AI Auto-Update Script
# Updates NasTech Agent from GitHub and reinstalls all dependencies
# =============================================================================
# Usage: bash ~/nastech-agent/update.sh
#        or: nastech-update  (if alias is set)
# =============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

NASUX_AGENT_DIR="${NASUX_AGENT_DIR:-$HOME/nastech-agent}"

log() { echo -e "${GREEN}[✓]${NC} $1"; }
info() { echo -e "${CYAN}[→]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err() { echo -e "${RED}[✗]${NC} $1"; exit 1; }

echo -e "${CYAN}${BOLD}"
echo "╔══════════════════════════════════════════╗"
echo "║    NasUX · NasTech AI Auto-Updater       ║"
echo "╚══════════════════════════════════════════╝"
echo -e "${NC}"

# Check git is installed
command -v git >/dev/null || err "git is not installed. Run: pkg install git"

# Ensure agent directory exists
if [ ! -d "$NASUX_AGENT_DIR" ]; then
    info "NasTech agent not found — cloning fresh copy…"
    git clone https://github.com/nastech-ai/NasTech-Agent.git "$NASUX_AGENT_DIR"
    log "NasTech Agent cloned to $NASUX_AGENT_DIR"
else
    info "NasTech Agent found at $NASUX_AGENT_DIR"
    cd "$NASUX_AGENT_DIR"

    # Stash any local changes to avoid conflicts
    if git diff --quiet 2>/dev/null; then
        info "Working tree clean — pulling latest…"
    else
        warn "Local changes detected — stashing them…"
        git stash push -m "auto-stash before nasux-update $(date +%Y%m%d-%H%M)" 2>/dev/null || true
    fi

    # Fetch and pull latest
    info "Fetching from GitHub…"
    git fetch origin 2>&1 | tail -3

    CURRENT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    info "Current: $CURRENT"

    git pull origin main 2>&1 | tail -5 || git pull origin master 2>&1 | tail -5 || warn "git pull failed — continuing with existing version"

    NEW=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    if [ "$CURRENT" != "$NEW" ]; then
        log "Updated from $CURRENT → $NEW"
    else
        log "Already up to date ($CURRENT)"
    fi
fi

cd "$NASUX_AGENT_DIR"

# Update Python dependencies
if [ -f "pyproject.toml" ] || [ -f "requirements.txt" ]; then
    info "Updating Python packages…"
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt --upgrade --quiet 2>&1 | tail -5 && log "pip packages updated"
    elif [ -f "pyproject.toml" ]; then
        pip install -e ".[cli]" --upgrade --quiet 2>&1 | tail -5 && log "pip packages updated from pyproject.toml"
    fi
fi

# Update Node.js dependencies
if [ -f "package.json" ]; then
    info "Updating Node.js packages…"
    npm install --quiet 2>&1 | tail -3 && log "npm packages updated"
fi

# Update system packages
info "Updating system packages…"
pkg upgrade -y 2>&1 | tail -3 && log "System packages updated"

# Re-ensure scripts are executable
chmod +x "$NASUX_AGENT_DIR/nastech" 2>/dev/null || true
chmod +x "$NASUX_AGENT_DIR/install.sh" 2>/dev/null || true
chmod +x "$NASUX_AGENT_DIR/start.sh" 2>/dev/null || true
chmod +x "$NASUX_AGENT_DIR/update.sh" 2>/dev/null || true
log "Scripts marked executable"

# Re-link CLI
LINK_DIR="${PREFIX}/bin"
if [ -f "$NASUX_AGENT_DIR/nastech" ] && [ -d "$LINK_DIR" ]; then
    ln -sf "$NASUX_AGENT_DIR/nastech" "$LINK_DIR/nastech"
    log "nastech CLI re-linked"
fi

echo -e "\n${GREEN}${BOLD}╔══════════════════════════════════════════╗"
echo "║      ✓  NasTech AI Updated!              ║"
echo "╚══════════════════════════════════════════╝${NC}"
echo ""
FINAL_VER=$(cd "$NASUX_AGENT_DIR" && git describe --tags --always 2>/dev/null || git rev-parse --short HEAD 2>/dev/null || echo "unknown")
echo -e "  Version: ${CYAN}${FINAL_VER}${NC}"
echo -e "  Run '${BOLD}nastech${NC}' to start the AI agent."
echo ""
