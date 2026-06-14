#!/bin/bash
# =============================================================================
# NasUX — NasTech AI Setup Inside Kali NetHunter
# Powered by NasTech AI
# =============================================================================
# Run this INSIDE the Kali environment:
#   kali-run bash ~/nastech-agent/kali-nastech-setup.sh
# OR interactively:
#   kali → bash ~/nastech-agent/kali-nastech-setup.sh
# =============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[✓]${NC} $1"; }
info() { echo -e "${CYAN}[→]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[✗]${NC} $1"; exit 1; }
step() { echo -e "\n${BOLD}${CYAN}━━━ $1 ━━━${NC}\n"; }

AGENT_DIR="${HOME:-/root}/nastech-agent"
LOG_FILE="${HOME:-/root}/kali-nastech-setup.log"

echo ""
echo -e "${BOLD}${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${CYAN}║   NasTech AI Setup — Kali NetHunter          ║${NC}"
echo -e "${BOLD}${CYAN}║   Powered by NasTech AI / NasUX               ║${NC}"
echo -e "${BOLD}${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo "Log: $LOG_FILE"
echo "" > "$LOG_FILE"

# =============================================================================
# Step 1: Update APT
# =============================================================================
step "Step 1/5: Updating Kali APT"

info "Running apt update..."
apt-get update -y 2>&1 | tail -5 | tee -a "$LOG_FILE"
ok "APT updated"

# =============================================================================
# Step 2: Install System Packages
# =============================================================================
step "Step 2/5: Installing System Packages (Kali APT)"

SYSTEM_PKGS=(
    python3
    python3-pip
    python3-venv
    python3-dev
    python3-setuptools
    python3-wheel
    git
    curl
    wget
    nano
    vim
    less
    tmux
    screen
    htop
    build-essential
    pkg-config
    libssl-dev
    libffi-dev
    libsqlite3-dev
    libreadline-dev
    libbz2-dev
    liblzma-dev
    zlib1g-dev
    libncurses5-dev
    zip
    unzip
    tar
    xz-utils
    gzip
    bzip2
    jq
    sqlite3
    openssl
    ca-certificates
    gnupg
    lsb-release
    procps
    net-tools
    iproute2
    curl
    wget
    dnsutils
    nmap
    socat
    netcat-openbsd
    openssh-client
    rsync
    file
    tree
    findutils
    diffutils
    patch
    sed
    gawk
    perl
    make
    cmake
    clang
    binutils
    autoconf
    automake
    libtool
    gcc
    g++
)

info "Installing ${#SYSTEM_PKGS[@]} system packages via Kali APT..."
DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    "${SYSTEM_PKGS[@]}" 2>&1 | tail -10 | tee -a "$LOG_FILE" || {
    warn "Some packages may have failed — continuing..."
}
ok "System packages installed"

# Install Node.js via NodeSource (LTS)
info "Installing Node.js (LTS) from NodeSource..."
if ! command -v node >/dev/null 2>&1; then
    curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - 2>/dev/null || warn "NodeSource setup failed"
    apt-get install -y nodejs 2>/dev/null | tail -3 | tee -a "$LOG_FILE" || warn "Node.js install failed"
fi
if command -v node >/dev/null 2>&1; then
    ok "Node.js $(node --version)"
else
    warn "Node.js not installed — skipping"
fi

# =============================================================================
# Step 3: Python Environment
# =============================================================================
step "Step 3/5: Setting Up Python Environment"

info "Upgrading pip..."
python3 -m pip install --upgrade pip setuptools wheel 2>&1 | tail -3 | tee -a "$LOG_FILE"
ok "pip upgraded"

PYTHON_CORE_PKGS=(
    "openai"
    "anthropic"
    "python-dotenv"
    "fire"
    "httpx[socks]"
    "rich"
    "tenacity"
    "pyyaml"
    "requests"
    "jinja2"
    "pydantic"
    "prompt_toolkit"
    "fastapi"
    "uvicorn[standard]"
    "click"
    "typer"
    "aiohttp"
    "websockets"
    "cryptography"
    "paramiko"
    "psutil"
    "Pillow"
    "pandas"
    "numpy"
    "matplotlib"
    "loguru"
    "httpx"
    "aiofiles"
    "pydantic-settings"
    "python-jose[cryptography]"
    "passlib[bcrypt]"
    "sqlalchemy"
    "GitPython"
    "scapy"
    "impacket"
)

info "Installing ${#PYTHON_CORE_PKGS[@]} core Python packages..."
for pkg in "${PYTHON_CORE_PKGS[@]}"; do
    python3 -m pip install "$pkg" --quiet 2>/dev/null \
        && echo "  ✓ $pkg" | tee -a "$LOG_FILE" \
        || echo "  ! skipped: $pkg" | tee -a "$LOG_FILE"
done
ok "Python packages installed"

# =============================================================================
# Step 4: NasTech AI Agent
# =============================================================================
step "Step 4/5: Installing NasTech AI Agent"

if [ -d "$AGENT_DIR" ]; then
    ok "NasTech Agent found at $AGENT_DIR"
    cd "$AGENT_DIR"
    chmod +x install.sh start.sh nastech 2>/dev/null || true

    info "Running NasTech Agent installer (Kali mode)..."
    export KALI_VERSION=1
    bash install.sh 2>&1 | tail -10 | tee -a "$LOG_FILE" || {
        warn "install.sh returned non-zero — checking manually..."
    }
else
    warn "NasTech Agent not found at $AGENT_DIR — downloading from git..."
    git clone https://github.com/nastech-ai/NasTech-Agent.git "$AGENT_DIR" 2>&1 | tail -5 | tee -a "$LOG_FILE"
    cd "$AGENT_DIR"
    chmod +x install.sh start.sh nastech 2>/dev/null || true
    export KALI_VERSION=1
    bash install.sh 2>&1 | tail -10 | tee -a "$LOG_FILE"
fi

ok "NasTech Agent installed"

# =============================================================================
# Step 5: CLI + PATH Setup
# =============================================================================
step "Step 5/5: Configuring CLI & PATH"

# Symlink nastech to /usr/local/bin for system-wide access
if [ -f "$AGENT_DIR/nastech" ]; then
    ln -sf "$AGENT_DIR/nastech" /usr/local/bin/nastech 2>/dev/null || true
    ok "nastech CLI linked to /usr/local/bin/nastech"
fi

# Also link as `ai` shortcut
ln -sf /usr/local/bin/nastech /usr/local/bin/ai 2>/dev/null || true
ok "ai shortcut linked"

# Update ~/.bashrc inside Kali
BASHRC="${HOME:-/root}/.bashrc"
if ! grep -q "nastech-agent" "$BASHRC" 2>/dev/null; then
    cat >> "$BASHRC" << 'BASHRCEOF'

# NasTech AI — NasUX (Kali NetHunter)
export KALI_VERSION=1
export PATH="$HOME/nastech-agent:$PATH"
alias ai='nastech'
alias nastech-update='cd $HOME/nastech-agent && git pull && bash install.sh'
BASHRCEOF
    ok ".bashrc updated with NasTech aliases"
fi

# =============================================================================
# Done
# =============================================================================
echo ""
echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║   ✓ NasTech AI Ready in Kali — NasUX        ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}Commands available inside Kali:${NC}"
echo -e "  ${BOLD}nastech${NC}              — start NasTech AI"
echo -e "  ${BOLD}ai${NC}                   — shortcut alias"
echo -e "  ${BOLD}nastech chat${NC}         — interactive AI chat"
echo -e "  ${BOLD}nastech --help${NC}       — show all commands"
echo ""
echo -e "${CYAN}Kali tools available:${NC}"
echo -e "  ${BOLD}nmap, scapy, metasploit${NC} — security tools"
echo -e "  ${BOLD}python3, pip3${NC}           — full Python stack"
echo -e "  ${BOLD}apt install PACKAGE${NC}     — install anything from Kali repos"
echo ""
echo -e "${YELLOW}Don't forget to configure your API key:${NC}"
echo -e "  nano ~/nastech-agent/.env"
echo ""
echo "Log: $LOG_FILE"
