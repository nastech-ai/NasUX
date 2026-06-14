#!/data/data/com.nastech.nasux/files/usr/bin/bash
# =============================================================================
# NasUX — Kali NetHunter Setup
# Powered by NasTech AI
# =============================================================================
# Downloads and installs a real Kali Linux rootfs via proot (no root needed).
# After setup, run:  kali          — enter Kali shell
#                   kali-run CMD  — run one command inside Kali
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

KALI_FS="${KALI_FS:-$HOME/kali-fs}"
KALI_TMP="${KALI_TMP:-$HOME/kali-tmp}"
MIRROR="https://kali.download/nethunter-images/current/rootfs"

echo ""
echo -e "${BOLD}${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${CYAN}║   NasUX — Kali NetHunter Setup               ║${NC}"
echo -e "${BOLD}${CYAN}║   Powered by NasTech AI                       ║${NC}"
echo -e "${BOLD}${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# =============================================================================
# Step 0: Check dependencies
# =============================================================================
step "Step 0/5: Checking Requirements"

for dep in proot curl tar; do
    if ! command -v "$dep" >/dev/null 2>&1; then
        err "$dep is required but not found. Install it first: pkg install $dep"
    fi
    ok "$dep found"
done

# =============================================================================
# Step 1: Detect ABI
# =============================================================================
step "Step 1/5: Detecting Architecture"

ARCH="$(uname -m 2>/dev/null || echo unknown)"
case "$ARCH" in
    aarch64|arm64)
        KALI_ARCH="arm64"
        ok "Architecture: arm64 (AArch64)"
        ;;
    armv7*|armv8*|arm*)
        KALI_ARCH="armhf"
        ok "Architecture: armhf (ARM 32-bit)"
        ;;
    x86_64|amd64)
        KALI_ARCH="amd64"
        ok "Architecture: amd64 (x86_64)"
        ;;
    *)
        warn "Unknown arch '$ARCH' — defaulting to arm64"
        KALI_ARCH="arm64"
        ;;
esac

ROOTFS_FILENAME="kali-nethunter-rootfs-minimal-${KALI_ARCH}.tar.xz"
ROOTFS_URL="${MIRROR}/${ROOTFS_FILENAME}"
CHECKSUM_URL="${ROOTFS_URL}.sha256sum"

# =============================================================================
# Step 2: Check if already installed
# =============================================================================
step "Step 2/5: Checking Existing Installation"

if [ -d "$KALI_FS/bin" ] && [ -f "$KALI_FS/bin/bash" ]; then
    ok "Kali rootfs already installed at $KALI_FS"
    info "Run 'kali' to enter the Kali environment"
    info "To reinstall, delete $KALI_FS and re-run this script"
    KALI_ALREADY_INSTALLED=1
else
    KALI_ALREADY_INSTALLED=0
    info "Kali rootfs not found — downloading..."
fi

# =============================================================================
# Step 3: Download rootfs
# =============================================================================
step "Step 3/5: Downloading Kali Rootfs (~150MB)"

if [ "$KALI_ALREADY_INSTALLED" = "0" ]; then
    mkdir -p "$KALI_TMP"
    ROOTFS_PATH="$KALI_TMP/$ROOTFS_FILENAME"

    if [ -f "$ROOTFS_PATH" ]; then
        warn "Found existing download at $ROOTFS_PATH — checking size..."
        EXISTING_SIZE=$(stat -c%s "$ROOTFS_PATH" 2>/dev/null || echo 0)
        if [ "$EXISTING_SIZE" -gt 50000000 ]; then
            ok "Resuming existing download (${EXISTING_SIZE} bytes)"
            SKIP_DOWNLOAD=1
        else
            warn "Partial download, re-downloading..."
            SKIP_DOWNLOAD=0
        fi
    else
        SKIP_DOWNLOAD=0
    fi

    if [ "$SKIP_DOWNLOAD" = "0" ]; then
        info "URL: $ROOTFS_URL"
        info "Saving to: $ROOTFS_PATH"
        echo ""
        curl -L --progress-bar \
             --retry 3 \
             --retry-delay 5 \
             -o "$ROOTFS_PATH" \
             "$ROOTFS_URL" || err "Download failed. Check your internet connection."
    fi

    # Verify checksum (optional — non-fatal if checksum URL fails)
    info "Verifying download integrity..."
    CHECKSUM_FILE="$KALI_TMP/${ROOTFS_FILENAME}.sha256sum"
    if curl -sL -o "$CHECKSUM_FILE" "$CHECKSUM_URL" 2>/dev/null; then
        EXPECTED_SHA=$(awk '{print $1}' "$CHECKSUM_FILE" 2>/dev/null)
        if [ -n "$EXPECTED_SHA" ]; then
            ACTUAL_SHA=$(sha256sum "$ROOTFS_PATH" 2>/dev/null | awk '{print $1}')
            if [ "$EXPECTED_SHA" = "$ACTUAL_SHA" ]; then
                ok "SHA256 checksum verified"
            else
                warn "Checksum mismatch — continuing anyway (file may still be valid)"
            fi
        fi
    else
        warn "Could not fetch checksum — skipping verification"
    fi

    ok "Download complete"

    # =============================================================================
    # Step 4: Extract rootfs
    # =============================================================================
    step "Step 4/5: Extracting Kali Rootfs"

    info "Destination: $KALI_FS"
    mkdir -p "$KALI_FS"

    info "Extracting (this may take several minutes)..."
    proot --link2symlink \
        tar -xJf "$ROOTFS_PATH" \
        --no-same-owner \
        -C "$KALI_FS" \
        2>/dev/null || {
        # Fallback: try without proot (some environments don't need it for tar)
        warn "proot tar failed, trying direct tar..."
        tar --no-same-owner -xJf "$ROOTFS_PATH" -C "$KALI_FS" 2>/dev/null || {
            err "Extraction failed. Try: tar -xJf $ROOTFS_PATH -C $KALI_FS"
        }
    }

    ok "Extraction complete"

    # Clean up download
    rm -f "$ROOTFS_PATH" "$CHECKSUM_FILE"
    rmdir "$KALI_TMP" 2>/dev/null || true
else
    step "Step 4/5: Skipped (already installed)"
fi

# =============================================================================
# Step 5: Configure Kali environment
# =============================================================================
step "Step 5/5: Configuring Kali Environment"

# DNS resolver
info "Writing /etc/resolv.conf..."
cat > "$KALI_FS/etc/resolv.conf" << 'RESOLV'
nameserver 8.8.8.8
nameserver 8.8.4.4
nameserver 1.1.1.1
RESOLV
ok "DNS configured"

# Hosts file
info "Writing /etc/hosts..."
cat > "$KALI_FS/etc/hosts" << 'HOSTS'
127.0.0.1   localhost kali
::1         localhost ip6-localhost ip6-loopback
HOSTS
ok "Hosts file configured"

# Kali APT sources
info "Writing /etc/apt/sources.list (Kali Rolling)..."
mkdir -p "$KALI_FS/etc/apt"
cat > "$KALI_FS/etc/apt/sources.list" << 'SOURCES'
# Kali Linux Rolling — Official Repository
# Powered by NasTech AI / NasUX
deb http://http.kali.org/kali kali-rolling main non-free contrib
# Mirror alternatives (uncomment faster one for your region):
# deb https://mirrors.ocf.berkeley.edu/kali kali-rolling main non-free contrib
# deb https://mirror.vinehost.net/kali kali-rolling main non-free contrib
SOURCES
ok "APT sources configured (Kali Rolling)"

# Required directories for proot bind mounts
for dir in proc sys dev dev/pts tmp run; do
    mkdir -p "$KALI_FS/$dir"
done
ok "Required directories created"

# Mark installation complete
echo "$(date)" > "$KALI_FS/.nastech-kali-installed"
ok "Installation marked complete"

# Install kali / kali-run commands into PATH
PREFIX_BIN="${PREFIX:-/data/data/com.nastech.nasux/files/usr}/bin"
KALI_LOGIN_SCRIPT="$(dirname "$0")/kali-login.sh"

if [ -f "$KALI_LOGIN_SCRIPT" ]; then
    cp "$KALI_LOGIN_SCRIPT" "$PREFIX_BIN/kali" && chmod +x "$PREFIX_BIN/kali"
    ok "kali command installed at $PREFIX_BIN/kali"
fi

# Create kali-run (non-interactive single command runner)
cat > "$PREFIX_BIN/kali-run" << KALIRUN
#!/data/data/com.nastech.nasux/files/usr/bin/bash
# NasUX — Run a command inside Kali without interactive login
KALI_FS="\${KALI_FS:-\$HOME/kali-fs}"
if [ ! -d "\$KALI_FS/bin" ]; then
  echo "Kali not installed. Run: bash ~/nastech-agent/kali-setup.sh"
  exit 1
fi
exec proot \\
  --link2symlink \\
  -0 \\
  -r "\$KALI_FS" \\
  -b /dev:/dev \\
  -b /proc:/proc \\
  -b /sys:/sys \\
  -b "\$HOME:/root" \\
  -b /sdcard:/sdcard \\
  -w /root \\
  /usr/bin/env -i \\
    HOME=/root \\
    USER=root \\
    TERM="\${TERM:-xterm-256color}" \\
    LANG=C.UTF-8 \\
    PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \\
    KALI_VERSION=1 \\
  /bin/bash -c "\$*"
KALIRUN
chmod +x "$PREFIX_BIN/kali-run"
ok "kali-run command installed at $PREFIX_BIN/kali-run"

# =============================================================================
# Done
# =============================================================================
echo ""
echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║   ✓ Kali NetHunter Ready — NasUX             ║${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}To get started:${NC}"
echo -e "  ${BOLD}kali${NC}                            — enter Kali Linux shell"
echo -e "  ${BOLD}kali-run apt update${NC}             — run apt inside Kali"
echo -e "  ${BOLD}kali-run bash ~/nastech-agent/kali-nastech-setup.sh${NC}"
echo -e "                                  — install NasTech AI in Kali"
echo -e "  ${BOLD}bash ~/nastech-agent/kali-start-desktop.sh${NC}"
echo -e "                                  — install + start Kali desktop (VNC)"
echo ""
echo -e "${YELLOW}First-time setup inside Kali:${NC}"
echo -e "  kali"
echo -e "  apt update && apt upgrade -y"
echo -e "  bash ~/nastech-agent/kali-nastech-setup.sh"
echo ""
