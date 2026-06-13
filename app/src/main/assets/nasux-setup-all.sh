#!/data/data/com.nastech.nasux/files/usr/bin/bash
# =============================================================================
# NasUX Complete Dependency Setup Script
# Installs ALL dependencies needed for NasTech AI Agent on Android/NasUX
# =============================================================================
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
    echo "║     NasUX · NasTech AI Setup v1.0        ║"
    echo "║  Full dependency installer for Android   ║"
    echo "╚══════════════════════════════════════════╝"
    echo -e "${NC}"
}

log() { echo -e "${GREEN}[✓]${NC} $1" | tee -a "$LOG_FILE"; }
info() { echo -e "${CYAN}[→]${NC} $1" | tee -a "$LOG_FILE"; }
warn() { echo -e "${YELLOW}[!]${NC} $1" | tee -a "$LOG_FILE"; }
err() { echo -e "${RED}[✗]${NC} $1" | tee -a "$LOG_FILE"; }
step() { echo -e "\n${BOLD}${CYAN}━━━ $1 ━━━${NC}\n" | tee -a "$LOG_FILE"; }

banner

echo "" > "$LOG_FILE"
info "Log: $LOG_FILE"
info "Started: $(date)"

# ─── Step 0: Configure NasUX APT Repository ─────────────────────────────────
step "Step 0/6: NasUX Repository Setup"

NASUX_REPO_URL="https://packages-cf.nasux.dev/apt/nasux-main"
NASUX_SOURCES_FILE="${PREFIX}/etc/apt/sources.list"
NASUX_SOURCES_DIR="${PREFIX}/etc/apt/sources.list.d"

# Ensure apt sources exist and point to NasUX repo
if [ -f "$NASUX_SOURCES_FILE" ]; then
    if ! grep -q "packages-cf.nasux.dev" "$NASUX_SOURCES_FILE" 2>/dev/null; then
        warn "NasUX repo not found in sources.list — adding it"
        echo "deb $NASUX_REPO_URL stable main" >> "$NASUX_SOURCES_FILE"
        log "Added NasUX repo to sources.list"
    else
        log "NasUX repo already configured in sources.list"
    fi
else
    info "Creating ${NASUX_SOURCES_FILE}..."
    mkdir -p "$(dirname "$NASUX_SOURCES_FILE")"
    echo "deb $NASUX_REPO_URL stable main" > "$NASUX_SOURCES_FILE"
    log "Created sources.list with NasUX repo"
fi

# Also verify the NasUX-specific sources directory
if [ -d "$NASUX_SOURCES_DIR" ]; then
    if ! grep -rl "packages-cf.nasux.dev" "$NASUX_SOURCES_DIR" 2>/dev/null | grep -q .; then
        info "Writing nasux-main.list to sources.list.d..."
        echo "deb $NASUX_REPO_URL stable main" > "${NASUX_SOURCES_DIR}/nasux-main.list"
        log "Created ${NASUX_SOURCES_DIR}/nasux-main.list"
    fi
fi

info "Updating package lists..."
pkg update -y 2>&1 | tail -5
log "Package lists updated"

# ─── Step 1: System Packages ────────────────────────────────────────────────
step "Step 1/6: System Packages"

SYSTEM_PKGS=(
    python
    python-pip
    nodejs
    git
    curl
    wget
    openssh
    openssl
    libssl
    build-essential
    pkg-config
    make
    cmake
    clang
    binutils
    libtool
    autoconf
    automake
    zip
    unzip
    tar
    gzip
    bzip2
    xz-utils
    sqlite
    libsqlite
    readline
    libreadline
    ncurses
    libncurses
    libjpeg-turbo
    libpng
    libwebp
    freetype
    libxml2
    libxslt
    libyaml
    gettext
    nano
    vim
    less
    diffutils
    patch
    sed
    gawk
    perl
    ruby
    rust
    cargo
    golang
    jq
    tmux
    screen
    htop
    nmap
    socat
    netcat-openbsd
    iproute2
    dnsutils
    traceroute
    whois
    ffmpeg
    imagemagick
    poppler
    tesseract
    tflite
    proot
    proot-distro
    nasux-utils
)

# NasUX core system tools (nasux-tools provides terminal utilities, manpages, pkg wrappers)
NASUX_CORE_PKGS=(
    nasux-tools
)

info "Installing ${#SYSTEM_PKGS[@]} system packages…"
for pkg in "${SYSTEM_PKGS[@]}"; do
    pkg install -y "$pkg" 2>/dev/null && log "$pkg" || warn "Skipped: $pkg"
done

info "Installing NasUX core system tools…"
for pkg in "${NASUX_CORE_PKGS[@]}"; do
    pkg install -y "$pkg" 2>/dev/null && log "$pkg (NasUX core)" || warn "Skipped: $pkg"
done

# Install NasTech CLI wrappers (nastech-setup-storage, nastech-open, etc.)
step "NasTech CLI Wrappers"
WRAPPERS_SCRIPT="$(dirname "$0")/nastech-wrappers.sh"
if [ -f "$WRAPPERS_SCRIPT" ]; then
    bash "$WRAPPERS_SCRIPT"
else
    warn "nastech-wrappers.sh not found — skipping CLI wrapper install"
fi

# ─── Step 2: Python Environment ─────────────────────────────────────────────
step "Step 2/6: Python 3.11 Environment"

info "Upgrading pip…"
pip install --upgrade pip setuptools wheel 2>&1 | tail -5

# Core NasTech dependencies (exact pins from pyproject.toml)
PYTHON_PKGS=(
    "openai==2.24.0"
    "python-dotenv==1.2.2"
    "fire==0.7.1"
    "httpx[socks]==0.28.1"
    "rich==14.3.3"
    "tenacity==9.1.4"
    "pyyaml==6.0.3"
    "ruamel.yaml==0.18.17"
    "requests==2.33.0"
    "jinja2==3.1.6"
    "pydantic==2.13.4"
    "prompt_toolkit==3.0.52"
    "croniter==6.0.0"
    "Markdown==3.10.2"
    "PyJWT[crypto]==2.13.0"
    "psutil==7.2.2"
    "pathspec==1.1.1"
    "Pillow==12.2.0"
    "tzdata==2025.3"
    # Extras for enhanced functionality
    "anthropic"
    "mistralai"
    "google-api-python-client"
    "google-auth"
    "google-auth-oauthlib"
    "boto3"
    "fastapi"
    "uvicorn[standard]"
    "aiohttp"
    "websockets"
    "paramiko"
    "cryptography"
    "certifi"
    "charset-normalizer"
    "urllib3"
    "click"
    "typer"
    "colorama"
    "tqdm"
    "tabulate"
    "prettytable"
    "pandas"
    "numpy"
    "scipy"
    "matplotlib"
    "seaborn"
    "scikit-learn"
    "torch"
    "transformers"
    "diffusers"
    "datasets"
    "accelerate"
    "tokenizers"
    "sentencepiece"
    "pygments"
    "black"
    "isort"
    "flake8"
    "mypy"
    "pytest"
    "pytest-asyncio"
    "httpx"
    "aiofiles"
    "pydantic-settings"
    "sqlalchemy"
    "alembic"
    "motor"
    "pymongo"
    "redis"
    "celery"
    "kombu"
    "billiard"
    "amqp"
    "vine"
    "itsdangerous"
    "passlib[bcrypt]"
    "python-jose[cryptography]"
    "email-validator"
    "phonenumbers"
    "pytz"
    "arrow"
    "pendulum"
    "humanize"
    "babel"
    "lxml"
    "beautifulsoup4"
    "html5lib"
    "cssselect"
    "playwright"
    "selenium"
    "mechanize"
    "scrapy"
    "pyautogui"
    "pynput"
    "keyboard"
    "pyperclip"
    "playsound"
    "pydub"
    "speechrecognition"
    "gtts"
    "edge-tts"
    "pyqrcode"
    "qrcode[pil]"
    "barcode"
    "pyotp"
    "itsdangerous"
    "watchdog"
    "schedule"
    "APScheduler"
    "rq"
    "dramatiq"
    "boto3"
    "s3transfer"
    "azure-storage-blob"
    "google-cloud-storage"
    "dropbox"
    "pydrive2"
    "loguru"
    "structlog"
    "sentry-sdk"
    "rollbar"
    "datadog"
    "prometheus-client"
    "opentelemetry-api"
    "opentelemetry-sdk"
    "py-spy"
    "memory-profiler"
    "line-profiler"
    "psutil"
    "gputil"
    "docker"
    "kubernetes"
    "ansible"
    "fabric"
    "paramiko"
    "netmiko"
    "napalm"
    "nornir"
    "pysnmp"
    "pynetbox"
    "ntc-templates"
    "textfsm"
    "ttp"
    "ipaddress"
    "netaddr"
    "netifaces"
    "scapy"
    "pyshark"
    "dpkt"
    "impacket"
    "ldap3"
    "pywinrm"
    "pysmb"
    "ftplib"
    "telnetlib3"
    "asyncssh"
    "scp"
    "rsync"
    "pysftp"
    "GitPython"
    "PyGithub"
    "python-gitlab"
    "jira"
    "atlassian-python-api"
    "slack-sdk"
    "python-telegram-bot"
    "discord.py"
    "tweepy"
    "praw"
    "facebook-sdk"
    "linkedin-api"
    "stripe"
    "paypalrestsdk"
    "sendgrid"
    "mailchimp3"
    "twilio"
    "vonage"
    "boto3"
    "pynamodb"
    "motor"
    "aiomysql"
    "aiopg"
    "aioodbc"
    "cx_Oracle"
    "psycopg2-binary"
    "PyMySQL"
    "pymssql"
    "cassandra-driver"
    "elasticsearch"
    "opensearch-py"
    "clickhouse-driver"
    "influxdb-client"
    "neo4j"
    "py2neo"
    "networkx"
    "graph-tool"
    "pyvis"
    "plotly"
    "dash"
    "bokeh"
    "altair"
    "streamlit"
    "gradio"
    "panel"
    "voila"
    "jupyterlab"
    "notebook"
    "ipywidgets"
    "ipython"
    "nbconvert"
    "papermill"
    "dask"
    "ray"
    "joblib"
    "multiprocess"
    "concurrent-futures"
    "anyio"
    "trio"
    "curio"
    "gevent"
    "eventlet"
    "twisted"
    "tornado"
    "sanic"
    "starlette"
    "litestar"
    "blacksheep"
    "grpcio"
    "protobuf"
    "thrift"
    "avro-python3"
    "fastavro"
    "msgpack"
    "cbor2"
    "orjson"
    "ujson"
    "simplejson"
    "toml"
    "tomli"
    "tomllib"
    "configparser"
    "confuse"
    "dynaconf"
    "python-decouple"
    "environs"
    "pydantic-settings"
    "hydra-core"
    "omegaconf"
    "click"
    "typer"
    "docopt"
    "argparse"
    "plumbum"
    "invoke"
    "nox"
    "tox"
    "pre-commit"
    "commitizen"
    "bump2version"
    "semantic-version"
    "packaging"
    "pip-tools"
    "pipdeptree"
    "licensecheck"
    "safety"
    "bandit"
    "pylint"
    "pycodestyle"
    "pydocstyle"
    "autopep8"
    "yapf"
    "ruff"
    "coverage"
    "codecov"
    "hypothesis"
    "faker"
    "factory-boy"
    "model-bakery"
    "responses"
    "httpretty"
    "vcrpy"
    "moto"
    "freezegun"
    "time-machine"
    "pytest-mock"
    "pytest-cov"
    "pytest-xdist"
    "pytest-timeout"
    "pytest-benchmark"
    "locust"
    "k6"
    "artillery"
)

info "Installing ${#PYTHON_PKGS[@]} Python packages…"
for pkg in "${PYTHON_PKGS[@]}"; do
    pip install "$pkg" --quiet 2>/dev/null && log "pip: $pkg" || warn "pip skipped: $pkg"
done

# ─── Step 3: Node.js Packages ───────────────────────────────────────────────
step "Step 3/6: Node.js Packages"

NODE_PKGS=(
    npm
    yarn
    pnpm
    typescript
    ts-node
    "@types/node"
    eslint
    prettier
    nodemon
    pm2
    serve
    http-server
    live-server
    concurrently
    cross-env
    dotenv
    axios
    node-fetch
    got
    superagent
    cheerio
    puppeteer-core
    playwright
    express
    fastify
    koa
    hapi
    restify
    nest
    next
    nuxt
    gatsby
    remix
    vite
    webpack
    parcel
    rollup
    esbuild
    babel-cli
    "@babel/core"
    react
    react-dom
    vue
    "@vue/cli"
    svelte
    solid-js
    preact
    lit
    socket.io
    ws
    ioredis
    mongoose
    sequelize
    prisma
    typeorm
    knex
    mikro-orm
    drizzle-orm
    graphql
    apollo-server
    "@apollo/client"
    urql
    trpc
    zod
    yup
    joi
    ajv
    lodash
    ramda
    fp-ts
    rxjs
    mobx
    zustand
    redux
    "@reduxjs/toolkit"
    immer
    recoil
    jotai
    jest
    vitest
    mocha
    chai
    sinon
    supertest
    "@testing-library/react"
    cypress
    playwright
    dayjs
    date-fns
    luxon
    moment
    uuid
    nanoid
    cuid
    ulid
    bcrypt
    argon2
    jsonwebtoken
    passport
    "@auth0/node-auth0"
    stripe
    nodemailer
    "@sendgrid/mail"
    twilio
    sharp
    jimp
    "@tensorflow/tfjs"
    "@tensorflow/tfjs-node"
    onnxruntime-node
    openai
    anthropic
    "@google/generative-ai"
    langchain
    llamaindex
    weaviate-ts-client
    pinecone-client
    qdrant-client
    chromadb
    yargs
    commander
    inquirer
    chalk
    ora
    boxen
    table
    cli-table3
    figlet
    gradient-string
    got
    node-cron
    bull
    bullmq
    bee-queue
    agenda
    node-cache
    lru-cache
    keyv
    flat-cache
    cacache
    pidtree
    pidusage
    systeminformation
    os-utils
    node-os-utils
    shelljs
    execa
    cross-spawn
    which
    find-up
    globby
    fast-glob
    chokidar
    micromatch
    minimatch
    picomatch
    memfs
    mock-fs
    tar
    archiver
    extract-zip
    node-7z
    adm-zip
    jszip
    pdfkit
    pdf-parse
    pdf-lib
    docx
    exceljs
    xlsx
    csv-parse
    csv-stringify
    papaparse
    xml2js
    xmlbuilder2
    fast-xml-parser
    yaml
    toml
    ini
    dotenv-flow
    convict
    nconf
    config
    env-var
    envalid
    winston
    pino
    bunyan
    morgan
    log4js
    debug
    loglevel
    consola
    npmlog
    signale
    "@sentry/node"
    "@datadog/datadog-ci"
    dd-trace
)

info "Installing ${#NODE_PKGS[@]} Node.js packages globally…"
npm install -g "${NODE_PKGS[@]}" --quiet 2>/dev/null || warn "Some node packages may have failed"
log "Node.js packages installed"

# ─── Step 4: NasTech Agent Setup ────────────────────────────────────────────
step "Step 4/6: NasTech AI Agent"

if [ -d "$NASUX_AGENT_DIR" ]; then
    info "NasTech agent found at $NASUX_AGENT_DIR"
    cd "$NASUX_AGENT_DIR"
    chmod +x install.sh start.sh nastech 2>/dev/null || true
    bash install.sh && log "NasTech agent installed" || warn "NasTech install.sh returned non-zero"
else
    warn "NasTech agent not found at $NASUX_AGENT_DIR — installing from git…"
    git clone https://github.com/nastech-ai/NasTech-Agent.git "$NASUX_AGENT_DIR"
    cd "$NASUX_AGENT_DIR"
    chmod +x install.sh start.sh nastech 2>/dev/null || true
    bash install.sh && log "NasTech agent installed from git"
fi

# ─── Step 5: Setup .env ─────────────────────────────────────────────────────
step "Step 5/6: NasTech Configuration"

ENV_FILE="$NASUX_AGENT_DIR/.env"
if [ ! -f "$ENV_FILE" ]; then
    info "Creating .env template…"
    cat > "$ENV_FILE" << 'EOF'
# NasTech AI Configuration
# Set at least ONE of these API keys to use NasTech AI

# OpenAI
# OPENAI_API_KEY=sk-...

# Anthropic (Claude)
# ANTHROPIC_API_KEY=sk-ant-...

# OpenRouter (access ALL models via one key)
# OPENROUTER_API_KEY=sk-or-...

# Google Gemini
# GOOGLE_API_KEY=...

# Model to use (default: claude-opus-4.8 via OpenRouter)
# NASTECH_MODEL=claude-opus-4.8

# Optional: Set your preferred provider
# NASTECH_PROVIDER=openrouter
EOF
    log ".env template created at $ENV_FILE"
    warn "Edit ~/.nastech-agent/.env and add your API key to use NasTech AI"
else
    log ".env already exists"
fi

# ─── Step 6: PATH Setup ─────────────────────────────────────────────────────
step "Step 6/6: PATH & CLI Setup"

LINK_DIR="$PREFIX/bin"
if [ -f "$NASUX_AGENT_DIR/nastech" ]; then
    ln -sf "$NASUX_AGENT_DIR/nastech" "$LINK_DIR/nastech" 2>/dev/null && log "nastech CLI linked to $LINK_DIR"
fi

# Update .bashrc if needed
BASHRC="$HOME/.bashrc"
if ! grep -q "nastech-agent" "$BASHRC" 2>/dev/null; then
    echo "" >> "$BASHRC"
    echo "# NasTech AI" >> "$BASHRC"
    echo "export PATH=\"\$HOME/nastech-agent:\$PATH\"" >> "$BASHRC"
    echo "alias ai='nastech'" >> "$BASHRC"
    echo "alias nastech-update='bash \$HOME/nastech-agent/update.sh'" >> "$BASHRC"
    log ".bashrc updated with NasTech aliases"
fi

# =============================================================================
echo -e "\n${GREEN}${BOLD}╔══════════════════════════════════════════╗"
echo "║         ✓ NasUX Setup Complete!          ║"
echo "╚══════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}To launch NasTech AI:${NC}"
echo -e "  ${BOLD}nastech${NC}          — start AI agent"
echo -e "  ${BOLD}ai${NC}               — alias for nastech"
echo -e "  ${BOLD}nastech-update${NC}   — update to latest version"
echo ""
echo -e "${YELLOW}Don't forget to set your API key:${NC}"
echo -e "  nano ~/nastech-agent/.env"
echo ""
echo -e "Log saved to: ${LOG_FILE}"
