#!/data/data/com.nastech.nasux/files/usr/bin/bash
# =============================================================================
# NasUX — Kali NetHunter Login
# Powered by NasTech AI
# =============================================================================
# Enters the real Kali Linux environment via proot (no root required).
# Your NasUX home directory (~) is accessible inside Kali as /root.
# =============================================================================

KALI_FS="${KALI_FS:-$HOME/kali-fs}"

# Color helpers
RED='\033[0;31m'
CYAN='\033[0;36m'
GREEN='\033[0;32m'
NC='\033[0m'

if [ ! -d "$KALI_FS/bin" ]; then
    echo -e "${RED}[ERROR]${NC} Kali is not installed."
    echo -e "  Run: ${CYAN}bash ~/nastech-agent/kali-setup.sh${NC}"
    exit 1
fi

echo -e "${GREEN}[NasUX]${NC} Entering Kali Linux... (type 'exit' to return)"
echo -e "${CYAN}        NasTech AI is available: nastech chat${NC}"
echo ""

exec proot \
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
        LOGNAME=root \
        TERM="${TERM:-xterm-256color}" \
        LANG=C.UTF-8 \
        LC_ALL=C.UTF-8 \
        PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
        KALI_VERSION=1 \
        NASUX_VERSION="${NASUX_VERSION:-1}" \
        ANDROID_DATA="${ANDROID_DATA:-}" \
        ANDROID_ROOT="${ANDROID_ROOT:-}" \
    /bin/bash --login
