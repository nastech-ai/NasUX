#!/data/data/com.nastech.nasux/files/usr/bin/bash
# =============================================================================
# NasTech Environment Setup — Permanent NasUX Shell Configuration
# Powered by NasTech AI
# =============================================================================
# Writes NASUX_VERSION, PATH, NASTECH_HOME, and all NasTech env vars into
# ~/.bashrc and ~/.profile so they persist across every shell session.
#
# Run once on first boot. Safe to re-run (idempotent).
# Usage: bash ~/nastech-agent/nastech-env-setup.sh
# =============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[0;33m'
BOLD='\033[1m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[✓]${NC} $1"; }
info() { echo -e "${CYAN}[→]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }

AGENT_DIR="${HOME}/nastech-agent"
NASTECH_HOME="${HOME}/.nastech"
LOCAL_BIN="${HOME}/.local/bin"
BASHRC="${HOME}/.bashrc"
PROFILE="${HOME}/.profile"

echo ""
echo -e "${BOLD}${CYAN}NasTech AI — Environment Setup${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# =============================================================================
# 1. Write permanent env block to ~/.bashrc
# =============================================================================
info "Writing NasTech env vars to ~/.bashrc..."

MARKER="# >>> NasTech AI Environment (NasUX) <<<"
MARKER_END="# <<< NasTech AI Environment End <<<"

if grep -qF "$MARKER" "$BASHRC" 2>/dev/null; then
    warn "NasTech env block already in ~/.bashrc — skipping (idempotent)"
else
    mkdir -p "$(dirname "$BASHRC")"
    cat >> "$BASHRC" << BASHRC_BLOCK

$MARKER
# NasUX platform detection — identifies this shell as NasUX to NasTech Agent
export NASUX_VERSION=1
export NASTECH_PLATFORM=nasux

# NasTech Agent home directory
export NASTECH_HOME="${NASTECH_HOME}"

# PATH — agent dir, local bin, Kali tools
export PATH="${AGENT_DIR}:${LOCAL_BIN}:\${PATH}"

# Kali Linux alias — enter proot-distro Kali environment
if [ -f "${AGENT_DIR}/kali-login.sh" ]; then
    alias kali="bash ${AGENT_DIR}/kali-login.sh"
fi

# NasTech CLI shortcuts
alias ai='nastech'
alias nastech-update='cd ${AGENT_DIR} && bash update.sh'
alias nastech-setup='bash ${AGENT_DIR}/install.sh'
alias nastech-fonts='bash ${AGENT_DIR}/kali-fonts.sh'
$MARKER_END
BASHRC_BLOCK
    ok "NasTech env block written to ~/.bashrc"
fi

# =============================================================================
# 2. Source ~/.bashrc from ~/.profile (ensures env on login shells)
# =============================================================================
info "Ensuring ~/.profile sources ~/.bashrc..."

if grep -qF "nastech-agent" "$PROFILE" 2>/dev/null; then
    warn "NasTech block already in ~/.profile — skipping"
else
    mkdir -p "$(dirname "$PROFILE")"
    cat >> "$PROFILE" << 'PROFILE_BLOCK'

# NasTech AI — load bashrc for NasUX environment
if [ -f "$HOME/.bashrc" ]; then
    . "$HOME/.bashrc"
fi
PROFILE_BLOCK
    ok "~/.profile updated"
fi

# =============================================================================
# 3. Create ~/.local/bin/nastech symlink/wrapper
# =============================================================================
info "Setting up nastech CLI in PATH..."

mkdir -p "$LOCAL_BIN"

NASTECH_BIN="$LOCAL_BIN/nastech"
if [ ! -f "$NASTECH_BIN" ] || [ ! -s "$NASTECH_BIN" ]; then
    cat > "$NASTECH_BIN" << WRAPPER
#!/data/data/com.nastech.nasux/files/usr/bin/bash
# NasTech CLI wrapper — NasUX / Powered by NasTech AI
export NASUX_VERSION=1
export NASTECH_PLATFORM=nasux
AGENT_DIR="\${HOME}/nastech-agent"
VENV_DIR="\${HOME}/.nastech/NasTech-Agent/venv"
if [ -f "\${VENV_DIR}/bin/activate" ]; then
    source "\${VENV_DIR}/bin/activate" 2>/dev/null || true
fi
if [ -f "\${AGENT_DIR}/nastech" ]; then
    exec python3 "\${AGENT_DIR}/nastech" "\$@"
fi
if [ -f "\${VENV_DIR}/bin/nastech" ]; then
    exec "\${VENV_DIR}/bin/nastech" "\$@"
fi
echo "[NasTech] Not installed. Run: bash \${AGENT_DIR}/install.sh" >&2
exit 1
WRAPPER
    chmod +x "$NASTECH_BIN"
    ok "nastech CLI wrapper installed at $NASTECH_BIN"
else
    ok "nastech CLI already at $NASTECH_BIN"
fi

# =============================================================================
# 4. Create ~/.local/bin/kali shortcut
# =============================================================================
info "Setting up kali shortcut..."

KALI_BIN="$LOCAL_BIN/kali"
if [ ! -f "$KALI_BIN" ]; then
    cat > "$KALI_BIN" << KALI_WRAPPER
#!/data/data/com.nastech.nasux/files/usr/bin/bash
# kali — Enter Kali Linux proot environment (NasUX / Powered by NasTech AI)
KALI_SCRIPT="\${HOME}/nastech-agent/kali-login.sh"
if [ -f "\$KALI_SCRIPT" ]; then
    exec bash "\$KALI_SCRIPT" "\$@"
else
    echo "[NasTech] Kali not set up. Run: bash ~/nastech-agent/kali-setup.sh"
    exit 1
fi
KALI_WRAPPER
    chmod +x "$KALI_BIN"
    ok "kali shortcut installed at $KALI_BIN"
fi

# =============================================================================
# 5. Create ~/.nastech/ directory structure
# =============================================================================
info "Creating NasTech home directories..."

mkdir -p "${NASTECH_HOME}/logs"
mkdir -p "${NASTECH_HOME}/sessions"
mkdir -p "${NASTECH_HOME}/skills"

# Copy .env template if no .env exists yet
if [ ! -f "${NASTECH_HOME}/.env" ]; then
    if [ -f "${AGENT_DIR}/.env.template" ]; then
        cp "${AGENT_DIR}/.env.template" "${NASTECH_HOME}/.env"
        ok "Created ${NASTECH_HOME}/.env from template"
        warn "Edit ~/.nastech/.env to add your API keys (OPENAI_API_KEY, ANTHROPIC_API_KEY, etc.)"
    fi
fi

# =============================================================================
# 6. Make agent scripts executable (ensure all .sh are chmod +x)
# =============================================================================
info "Setting executable permissions on all agent scripts..."

find "$AGENT_DIR" -name "*.sh" -exec chmod +x {} \; 2>/dev/null || true
[ -f "$AGENT_DIR/nastech" ]          && chmod +x "$AGENT_DIR/nastech"
[ -f "$AGENT_DIR/nastech_deps.py" ]  && chmod +x "$AGENT_DIR/nastech_deps.py"
[ -f "$AGENT_DIR/nastech_launcher.py" ] && chmod +x "$AGENT_DIR/nastech_launcher.py"

ok "All scripts are executable"

# =============================================================================
# Done
# =============================================================================
echo ""
echo -e "${BOLD}${GREEN}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║  ✓ NasTech AI Environment Ready — NasUX       ║${NC}"
echo -e "${BOLD}${GREEN}╚════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}Reload your shell:${NC}"
echo -e "  source ~/.bashrc"
echo ""
echo -e "${CYAN}Then use:${NC}"
echo -e "  ${BOLD}nastech --help${NC}           — NasTech AI CLI"
echo -e "  ${BOLD}nastech setup${NC}            — configure AI provider + API key"
echo -e "  ${BOLD}nastech chat${NC}             — start AI chat"
echo -e "  ${BOLD}kali${NC}                     — enter Kali Linux environment"
echo -e "  ${BOLD}bash ~/nastech-agent/install.sh${NC}  — install Python dependencies"
echo ""
echo -e "${YELLOW}To add your API key:${NC}"
echo -e "  nano ~/.nastech/.env"
echo ""
