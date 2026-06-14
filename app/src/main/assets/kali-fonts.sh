#!/bin/bash
# =============================================================================
# NasUX — Font Installer (Kali NetHunter / CLI)
# Powered by NasTech AI
# =============================================================================
# Installs ALL major coding & system fonts inside Kali via apt + direct download.
# Also sets Source Code Pro as the NasUX terminal font (~/.nasux/font.ttf).
#
# Run inside Kali:
#   kali-run bash ~/nastech-agent/kali-fonts.sh
# Or interactively:
#   kali → bash ~/nastech-agent/kali-fonts.sh
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
err()  { echo -e "${RED}[✗]${NC} $1"; }
step() { echo -e "\n${BOLD}${CYAN}━━━ $1 ━━━${NC}\n"; }

FONT_DIR="/usr/share/fonts"
OTF_DIR="$FONT_DIR/opentype"
TTF_DIR="$FONT_DIR/truetype"
TMP_DIR="/tmp/nasux-fonts"

mkdir -p "$OTF_DIR" "$TTF_DIR" "$TMP_DIR"

echo ""
echo -e "${BOLD}${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${CYAN}║   NasUX — Font Installer                     ║${NC}"
echo -e "${BOLD}${CYAN}║   Powered by NasTech AI                       ║${NC}"
echo -e "${BOLD}${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# =============================================================================
# Step 1: APT fonts
# =============================================================================
step "Step 1/4: Installing Fonts via Kali APT"

apt-get update -y 2>&1 | tail -3

APT_FONTS=(
    fonts-dejavu
    fonts-dejavu-core
    fonts-dejavu-extra
    fonts-liberation
    fonts-liberation2
    fonts-noto
    fonts-noto-mono
    fonts-noto-color-emoji
    fonts-noto-cjk
    fonts-inconsolata
    fonts-hack
    fonts-firacode
    fonts-urw-base35
    fonts-open-sans
    fonts-roboto
    fonts-lato
    fontconfig
    wget
    unzip
    curl
    ca-certificates
)

info "Installing ${#APT_FONTS[@]} font packages..."
DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    "${APT_FONTS[@]}" 2>&1 | tail -5 || warn "Some apt font packages failed"
ok "APT fonts installed"

# =============================================================================
# Step 2: Source Code Pro — Adobe (exact command from user)
# =============================================================================
step "Step 2/4: Adobe Source Code Pro"

SCP_DIR="$OTF_DIR/source-code-pro"
mkdir -p "$SCP_DIR"
cd "$TMP_DIR"

info "Downloading Source Code Pro from Adobe GitHub..."
wget -q --show-progress \
    https://github.com/adobe-fonts/source-code-pro/archive/refs/heads/release.zip \
    -O source-code-pro-release.zip || {
    warn "wget failed — trying curl..."
    curl -L --progress-bar \
        https://github.com/adobe-fonts/source-code-pro/archive/refs/heads/release.zip \
        -o source-code-pro-release.zip
}
ok "Downloaded Source Code Pro"

info "Extracting..."
unzip -q -o source-code-pro-release.zip
ok "Extracted"

SCP_EXTRACTED=$(ls -d "$TMP_DIR"/source-code-pro-* 2>/dev/null | head -1)
if [ -z "$SCP_EXTRACTED" ]; then
    warn "Could not find extracted Source Code Pro directory"
else
    # Install OTF (primary)
    if [ -d "$SCP_EXTRACTED/OTF" ]; then
        cp "$SCP_EXTRACTED"/OTF/*.otf "$SCP_DIR/" 2>/dev/null
        ok "Source Code Pro OTF installed ($(ls "$SCP_DIR"/*.otf 2>/dev/null | wc -l) files)"
    fi
    # Install TTF (fallback)
    TTF_SCP_DIR="$TTF_DIR/source-code-pro"
    mkdir -p "$TTF_SCP_DIR"
    if [ -d "$SCP_EXTRACTED/TTF" ]; then
        cp "$SCP_EXTRACTED"/TTF/*.ttf "$TTF_SCP_DIR/" 2>/dev/null
        ok "Source Code Pro TTF installed ($(ls "$TTF_SCP_DIR"/*.ttf 2>/dev/null | wc -l) files)"
    elif [ -d "$SCP_EXTRACTED/WOFF2" ]; then
        warn "No TTF folder — OTF only"
    fi
fi

# =============================================================================
# Step 3: More programming fonts (direct download)
# =============================================================================
step "Step 3/4: Additional Programming Fonts"

install_nerd_font() {
    local name="$1"
    local url="$2"
    local dest_dir="$TTF_DIR/$name"
    mkdir -p "$dest_dir"
    info "Downloading $name..."
    wget -q --show-progress "$url" -O "$TMP_DIR/${name}.zip" 2>/dev/null || \
        curl -sL "$url" -o "$TMP_DIR/${name}.zip" 2>/dev/null || {
        warn "Failed to download $name — skipping"
        return
    }
    unzip -q -o "$TMP_DIR/${name}.zip" -d "$dest_dir" '*.ttf' '*.otf' 2>/dev/null || true
    find "$dest_dir" -mindepth 2 -name "*.ttf" -exec mv {} "$dest_dir/" \; 2>/dev/null || true
    find "$dest_dir" -mindepth 2 -name "*.otf" -exec mv {} "$dest_dir/" \; 2>/dev/null || true
    COUNT=$(find "$dest_dir" -maxdepth 1 \( -name "*.ttf" -o -name "*.otf" \) | wc -l)
    ok "$name installed ($COUNT font files)"
}

# JetBrains Mono — clean, modern coding font
install_nerd_font "jetbrains-mono" \
    "https://github.com/JetBrains/JetBrainsMono/releases/download/v2.304/JetBrainsMono-2.304.zip"

# Cascadia Code — Microsoft's coding font
install_nerd_font "cascadia-code" \
    "https://github.com/microsoft/cascadia-code/releases/download/v2111.01/CascadiaCode-2111.01.zip"

# IBM Plex Mono — IBM's open-source typeface
install_nerd_font "ibm-plex-mono" \
    "https://github.com/IBM/plex/releases/download/%40ibm%2Fplex-mono%401.1.0/ibm-plex-mono.zip"

# Iosevka — extremely customizable coding font
install_nerd_font "iosevka" \
    "https://github.com/be5invis/Iosevka/releases/download/v31.9.1/PkgTTC-Iosevka-31.9.1.zip"

# Fira Code (direct, in case apt version is outdated)
install_nerd_font "fira-code" \
    "https://github.com/tonsky/FiraCode/releases/download/6.2/Fira_Code_v6.2.zip"

# =============================================================================
# Step 4: Rebuild font cache + set NasUX terminal font
# =============================================================================
step "Step 4/4: Rebuilding Font Cache & Setting NasUX Terminal Font"

info "Rebuilding fontconfig cache..."
fc-cache -fv 2>&1 | tail -5
ok "Font cache rebuilt"

# List installed fonts count
TOTAL=$(fc-list 2>/dev/null | wc -l)
ok "Total fonts available: $TOTAL"

# Set Source Code Pro Regular as the NasUX terminal font
# NasUX reads font from ~/.nasux/font.ttf (or .otf is not supported — use TTF)
NASUX_FONT_DIR="${HOME:-/root}/.nasux"
NASUX_FONT_PATH="$NASUX_FONT_DIR/font.ttf"
mkdir -p "$NASUX_FONT_DIR"

SCP_REGULAR_TTF=""
# Prefer TTF for NasUX compatibility
for f in "$TTF_DIR/source-code-pro"/*Regular*.ttf \
          "$TTF_DIR/source-code-pro"/*-Regular*.ttf \
          "$TTF_DIR/source-code-pro"/*.ttf; do
    [ -f "$f" ] && SCP_REGULAR_TTF="$f" && break
done

if [ -n "$SCP_REGULAR_TTF" ]; then
    cp "$SCP_REGULAR_TTF" "$NASUX_FONT_PATH"
    ok "NasUX terminal font → Source Code Pro ($SCP_REGULAR_TTF)"
    echo ""
    warn "Restart NasUX app to apply the new terminal font."
else
    # Try OTF if no TTF
    for f in "$OTF_DIR/source-code-pro"/*Regular*.otf \
              "$OTF_DIR/source-code-pro"/*.otf; do
        [ -f "$f" ] && SCP_REGULAR_TTF="$f" && break
    done
    if [ -n "$SCP_REGULAR_TTF" ]; then
        cp "$SCP_REGULAR_TTF" "$NASUX_FONT_PATH"
        ok "NasUX terminal font → Source Code Pro OTF (rename to .ttf for compatibility)"
    else
        warn "Source Code Pro TTF not found — NasUX font not changed"
    fi
fi

# Clean up temp files
rm -rf "$TMP_DIR"
ok "Temp files cleaned up"

# =============================================================================
# Done
# =============================================================================
echo ""
echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║   ✓ All Fonts Installed — NasUX              ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}Fonts installed:${NC}"
echo -e "  ${BOLD}Source Code Pro${NC}   — Adobe (OTF + TTF)"
echo -e "  ${BOLD}JetBrains Mono${NC}    — clean modern coding"
echo -e "  ${BOLD}Cascadia Code${NC}     — Microsoft (ligatures)"
echo -e "  ${BOLD}Fira Code${NC}         — Nikita Prokopov (ligatures)"
echo -e "  ${BOLD}IBM Plex Mono${NC}     — IBM open source"
echo -e "  ${BOLD}Iosevka${NC}           — customizable coding"
echo -e "  ${BOLD}Hack${NC}              — Kali APT"
echo -e "  ${BOLD}Inconsolata${NC}       — Kali APT"
echo -e "  ${BOLD}Noto Mono${NC}         — Google (full Unicode)"
echo -e "  ${BOLD}DejaVu${NC}            — system standard"
echo -e "  ${BOLD}Liberation${NC}        — open MS-compatible"
echo ""
echo -e "${CYAN}NasUX terminal font:${NC}  Source Code Pro Regular"
echo -e "${YELLOW}→ Restart NasUX to apply${NC}"
echo ""
echo -e "${CYAN}List all fonts:${NC}  fc-list | grep -i 'source\|fira\|jet\|cascadia'"
echo ""
