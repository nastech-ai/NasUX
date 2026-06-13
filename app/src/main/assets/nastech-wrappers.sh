#!/data/data/com.nastech.nasux/files/usr/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# NasTech CLI Wrapper Installer
# Powered by NasTech AI — https://github.com/nastech-ai/NasUX
#
# Creates nastech-* equivalents for all termux-* system commands so that
# NasUX users always interact with NasTech-branded commands.
# ─────────────────────────────────────────────────────────────────────────────

PREFIX="${PREFIX:-/data/data/com.nastech.nasux/files/usr}"
BIN="$PREFIX/bin"

# All system commands that get a nastech-* alias
CMDS=(
    setup-storage
    reload-settings
    open
    share
    clipboard-get
    clipboard-set
    notification
    vibrate
    battery-status
    location
    camera-info
    camera-photo
    camera-video
    microphone-record
    torch
    fingerprint
    wake-lock
    wake-unlock
    info
    fix-shebang
    change-repo
    sensor
    telephony-call
    telephony-cellinfo
    telephony-deviceinfo
    usb
    wifi-connectioninfo
    wifi-scaninfo
    wifi-enable
    download
    tts-speak
    tts-engines
    media-player
    media-scan
    contact-list
    dialog
    job-scheduler
    keystore
    storage-get
    volume
    brightness
)

echo "Installing NasTech CLI wrappers in $BIN..."
installed=0
for cmd in "${CMDS[@]}"; do
    wrapper="$BIN/nastech-$cmd"
    # Prefer nasux- binary; fall back to the underlying system command
    source_bin="$BIN/nasux-$cmd"
    [ -f "$source_bin" ] || source_bin="$BIN/termux-$cmd"

    # Only create wrapper if the underlying command exists
    if [ -f "$source_bin" ]; then
        cat > "$wrapper" << WRAPPER
#!/data/data/com.nastech.nasux/files/usr/bin/bash
# NasTech AI CLI — powered by NasTech AI (NasUX)
# Wrapper for: nasux-$cmd
exec "$source_bin" "\$@"
WRAPPER
        chmod +x "$wrapper"
        installed=$((installed + 1))
    else
        # Create a stub that prints a friendly NasTech error
        cat > "$wrapper" << STUB
#!/data/data/com.nastech.nasux/files/usr/bin/bash
echo "NasTech: '$cmd' requires NasUX:API companion app."
echo "Install it from: https://github.com/nastech-ai/NasUX/releases"
exit 1
STUB
        chmod +x "$wrapper"
    fi
done

# Special: nastech-setup-storage always works via am broadcast
cat > "$BIN/nastech-setup-storage" << 'STORAGE'
#!/data/data/com.nastech.nasux/files/usr/bin/bash
# NasTech Storage Access — powered by NasTech AI
echo "NasTech: Requesting storage access for NasUX..."
if command -v nasux-setup-storage >/dev/null 2>&1; then
    exec nasux-setup-storage "$@"
else
    am broadcast --user 0 -a com.nastech.nasux.action.SETUP_STORAGE \
        -n com.nastech.nasux/.app.NasUXOpenReceiver 2>/dev/null \
    || echo "NasTech: Grant storage manually in Android Settings → Apps → NasUX → Permissions."
fi
STORAGE
chmod +x "$BIN/nastech-setup-storage"

echo "✓ NasTech: $installed wrappers installed. Use 'nastech-*' commands in NasUX."
