#!/data/data/com.nastech.nasux/files/usr/bin/bash
# =============================================================================
# NasUX Complete Dependency Setup Script
# Powered by NasTech AI
# =============================================================================
# Installs ALL dependencies for NasTech AI Agent using real Kali Linux APT.
# No Termux packages — everything comes from official kali.org repositories.
#
# Usage: bash ~/nastech-agent/setup-all.sh
# =============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

NASUX_AGENT_DIR="$HOME/nastech-agent"
LOG_FILE="$HOME/nasux-setup.log"

banner() {
    echo -e "${CYAN}"
    echo "╔══════════════════════════════════════════╗"
    echo "║     NasUX · NasTech AI Setup v2.0        ║"
    echo "║  Real Kali Linux — no Termux packages    ║"
    echo "╚══════════════════════════════════════════╝"
    echo -e "${NC}"
}

log()  { echo -e "${GREEN}[✓]${NC} $1" | tee -a "$LOG_FILE"; }
info() { echo -e "${CYAN}[→]${NC} $1" | tee -a "$LOG_FILE"; }
warn() { echo -e "${YELLOW}[!]${NC} $1" | tee -a "$LOG_FILE"; }
err()  { echo -e "${RED}[✗]${NC} $1" | tee -a "$LOG_FILE"; }
step() { echo -e "\n${BOLD}${CYAN}━━━ $1 ━━━${NC}\n" | tee -a "$LOG_FILE"; }

banner

echo "" > "$LOG_FILE"
info "Log: $LOG_FILE"
info "Started: $(date)"

# =============================================================================
# Step 0: Ensure Kali rootfs is installed
# =============================================================================
step "Step 0/6: Kali Linux Environment"

KALI_FS="${KALI_FS:-$HOME/kali-fs}"

if [ ! -d "$KALI_FS/bin" ]; then
    warn "Kali rootfs not found at $KALI_FS"
    info "Installing Kali Linux first..."
    if [ -f "$NASUX_AGENT_DIR/kali-setup.sh" ]; then
        bash "$NASUX_AGENT_DIR/kali-setup.sh"
    else
        err "kali-setup.sh not found. Re-launch NasUX to restore it."
        exit 1
    fi
else
    log "Kali rootfs ready at $KALI_FS"
fi

# Helper: run a command inside Kali proot
kali_run() {
    proot \
        --link2symlink \
        -0 \
        -r "$KALI_FS" \
        -b /dev:/dev \
        -b /proc:/proc \
        -b /sys:/sys \
        -b "$HOME:/root" \
        -b /sdcard:/sdcard \
        -w /root \
        /usr/bin/env -i \
            HOME=/root \
            USER=root \
            TERM="${TERM:-xterm-256color}" \
            LANG=C.UTF-8 \
            PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
            KALI_VERSION=1 \
        /bin/bash -c "$*"
}

# =============================================================================
# Step 1: Update Kali APT and install system packages
# =============================================================================
step "Step 1/6: System Packages (Kali APT)"

info "Updating Kali APT..."
kali_run "apt-get update -y" 2>&1 | tail -5 | tee -a "$LOG_FILE"
log "APT updated"

SYSTEM_PKGS=(
    python3
    python3-pip
    python3-venv
    python3-dev
    python3-setuptools
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
    procps
    net-tools
    iproute2
    dnsutils
    nmap
    socat
    netcat-openbsd
    openssh-client
    rsync
    file
    tree
    diffutils
    patch
    sed
    gawk
    perl
    make
    cmake
    clang
    gcc
    g++
    binutils
    autoconf
    automake
    libtool
    ruby
    golang-go
)

info "Installing ${#SYSTEM_PKGS[@]} system packages via Kali APT..."
kali_run "DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends ${SYSTEM_PKGS[*]}" \
    2>&1 | tail -10 | tee -a "$LOG_FILE" || warn "Some packages may have failed"
log "System packages installed"

# Install Node.js LTS
info "Installing Node.js LTS..."
kali_run "curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && apt-get install -y nodejs" \
    2>&1 | tail -5 | tee -a "$LOG_FILE" || warn "Node.js install failed"
kali_run "node --version && npm --version" 2>/dev/null && log "Node.js installed" || warn "Node.js not available"

# =============================================================================
# Step 2: Python Environment (inside Kali)
# =============================================================================
step "Step 2/6: Python 3 Environment (Kali)"

info "Upgrading pip..."
kali_run "python3 -m pip install --upgrade pip setuptools wheel" 2>&1 | tail -3 | tee -a "$LOG_FILE"

PYTHON_PKGS=(
    "openai"
    "anthropic"
    "mistralai"
    "google-generativeai"
    "python-dotenv"
    "fire"
    "httpx[socks]"
    "rich"
    "tenacity"
    "pyyaml"
    "ruamel.yaml"
    "requests"
    "jinja2"
    "pydantic"
    "pydantic-settings"
    "prompt_toolkit"
    "fastapi"
    "uvicorn[standard]"
    "aiohttp"
    "websockets"
    "click"
    "typer"
    "colorama"
    "tqdm"
    "tabulate"
    "prettytable"
    "cryptography"
    "paramiko"
    "psutil"
    "Pillow"
    "pandas"
    "numpy"
    "matplotlib"
    "scikit-learn"
    "loguru"
    "httpx"
    "aiofiles"
    "python-jose[cryptography]"
    "passlib[bcrypt]"
    "sqlalchemy"
    "pymongo"
    "redis"
    "GitPython"
    "PyGithub"
    "scapy"
    "impacket"
    "ldap3"
    "asyncssh"
    "PyJWT[crypto]"
    "tzdata"
    "arrow"
    "humanize"
    "lxml"
    "beautifulsoup4"
    "scrapy"
    "playwright"
    "boto3"
    "fastapi"
    "uvicorn"
    "grpcio"
    "protobuf"
    "orjson"
    "watchdog"
    "schedule"
    "APScheduler"
    "sentry-sdk"
    "opentelemetry-api"
    "opentelemetry-sdk"
    "pytest"
    "pytest-asyncio"
    "black"
    "ruff"
    "mypy"
    "coverage"
)

info "Installing ${#PYTHON_PKGS[@]} Python packages inside Kali..."
for pkg in "${PYTHON_PKGS[@]}"; do
    kali_run "python3 -m pip install '$pkg' --quiet 2>/dev/null" \
        && log "pip: $pkg" || warn "pip skipped: $pkg"
done

# =============================================================================
# Step 3: Node.js Global Packages
# =============================================================================
step "Step 3/6: Node.js Global Packages"

NODE_PKGS=(
    typescript
    ts-node
    eslint
    prettier
    nodemon
    pm2
    serve
    http-server
    concurrently
    cross-env
    dotenv-cli
    axios
    express
    fastify
    next
    vite
    webpack
    esbuild
    react
    vue
    svelte
    graphql
    zod
    lodash
    rxjs
    jest
    vitest
    dayjs
    uuid
    bcrypt
    jsonwebtoken
    sharp
    openai
    anthropic
    langchain
    yargs
    commander
    inquirer
    chalk
    ora
    figlet
    node-cron
    ioredis
    mongoose
    prisma
    winston
    pino
    yaml
    csv-parse
    exceljs
    shelljs
    execa
    chokidar
    globby
)

info "Installing ${#NODE_PKGS[@]} Node.js packages globally inside Kali..."
kali_run "npm install -g ${NODE_PKGS[*]} --quiet 2>/dev/null" \
    && log "Node.js packages installed" || warn "Some node packages may have failed"

# =============================================================================
# Step 4: NasTech AI Agent
# =============================================================================
step "Step 4/6: NasTech AI Agent"

if [ -d "$NASUX_AGENT_DIR" ]; then
    info "NasTech Agent found at $NASUX_AGENT_DIR"
    info "Running NasTech installer inside Kali..."
    kali_run "cd /root/nastech-agent && chmod +x install.sh start.sh nastech 2>/dev/null; KALI_VERSION=1 bash install.sh" \
        2>&1 | tail -10 | tee -a "$LOG_FILE" \
        && log "NasTech Agent installed" || warn "NasTech install returned non-zero"
else
    warn "NasTech Agent not found at $NASUX_AGENT_DIR"
fi

# =============================================================================
# Step 5: Fonts (Source Code Pro + all programming fonts)
# =============================================================================
step "Step 5/6: Fonts"

if [ -f "$NASUX_AGENT_DIR/kali-fonts.sh" ]; then
    info "Running full font installer inside Kali..."
    kali_run "bash /root/nastech-agent/kali-fonts.sh" \
        2>&1 | tee -a "$LOG_FILE" \
        && log "All fonts installed" || warn "Font installer had warnings"
else
    warn "kali-fonts.sh not found — installing basic fonts only"
    kali_run "apt-get install -y fonts-hack fonts-firacode fonts-noto-mono fonts-dejavu 2>/dev/null" \
        && log "Basic fonts installed" || warn "Basic font install failed"
fi

# =============================================================================
# Step 6: PATH & CLI Setup
# =============================================================================
step "Step 6/6: PATH & CLI Setup"

# Create kali-run command in PREFIX/bin for easy access
PREFIX_BIN="${PREFIX:-/data/data/com.nastech.nasux/files/usr}/bin"

if [ -f "$NASUX_AGENT_DIR/kali-login.sh" ]; then
    cp "$NASUX_AGENT_DIR/kali-login.sh" "$PREFIX_BIN/kali" 2>/dev/null
    chmod +x "$PREFIX_BIN/kali" 2>/dev/null
    log "kali command installed at $PREFIX_BIN/kali"
fi

# Update ~/.bashrc if needed
BASHRC="$HOME/.bashrc"
if ! grep -q "nastech-agent" "$BASHRC" 2>/dev/null; then
    cat >> "$BASHRC" << 'BASHRCEOF'

# NasTech AI — NasUX
export PATH="$HOME/nastech-agent:$PATH"
alias ai='nastech'
alias kali-update='cd $HOME/nastech-agent && git pull'
alias nastech-update='bash $HOME/nastech-agent/update.sh'
# Quick Kali access
alias kali='bash $HOME/nastech-agent/kali-login.sh'
BASHRCEOF
    log ".bashrc updated"
fi

# =============================================================================
# Done
# =============================================================================
echo ""
echo -e "\n${GREEN}${BOLD}╔══════════════════════════════════════════╗"
echo "║       ✓ NasUX Setup Complete!            ║"
echo "║  Kali Linux · NasTech AI · All Fonts     ║"
echo -e "╚══════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}To use:${NC}"
echo -e "  ${BOLD}kali${NC}                  — enter Kali Linux shell"
echo -e "  ${BOLD}nastech${NC}               — NasTech AI (inside Kali)"
echo -e "  ${BOLD}ai${NC}                    — alias for nastech"
echo -e "  ${BOLD}kali-run apt install X${NC} — install any Kali package"
echo ""
echo -e "${YELLOW}API key required for NasTech AI:${NC}"
echo -e "  nano ~/nastech-agent/.env"
echo ""
echo -e "Log saved to: ${LOG_FILE}"
