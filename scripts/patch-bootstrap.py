#!/usr/bin/env python3
"""
NasUX Bootstrap Patcher v4
Patches Termux bootstrap ZIPs for NasUX (com.nastech.nasux).

FOUR-PASS STRATEGY:
  Pass 1 — ZIP entry NAMES: rename termux→nasux in every file path inside the ZIP
  Pass 2 — TEXT files:      full string replacement (any length, file rewritten)
  Pass 3 — ELF binaries:    same-length byte-for-byte patching (no offset shift)
  Pass 4 — GPG keyrings:    replace with NasUX-branded OpenPGP keys (pure Python)

Usage:
    python3 scripts/patch-bootstrap.py --all [--verify]
    python3 scripts/patch-bootstrap.py <input.zip> <output.zip> [--verify]
"""

import sys, os, io, re, zipfile, hashlib, struct, time, math, secrets

SRC_DIR  = "/tmp/bootstrap-check"
DEST_DIR = "app/src/main/cpp"
ARCHS    = ["aarch64", "arm", "i686", "x86_64"]

# ─────────────────────────────────────────────────────────────────────────────
# PASS 1: ZIP entry name rules  (applied to file paths inside the archive)
# Most-specific first; simple str.replace() after decoding path.
# ─────────────────────────────────────────────────────────────────────────────
NAME_REPLACEMENTS = [
    ("termux-apps-info-app-version-name", "nasux-apps-info-app-version-name"),
    ("termux-apps-info-env-variable",     "nasux-apps-info-env-variable"),
    ("termux-exec-system-linker-exec",    "nasux-exec-system-linker-exec"),
    ("termux-setup-package-manager",      "nasux-setup-package-manager"),
    ("termux-scoped-env-variable",        "nasux-scoped-env-variable"),
    ("termux-exec-ld-preload-lib",        "nasux-exec-ld-preload-lib"),
    ("termux-core-minimal",               "nasux-core-minimal"),
    ("termux-setup-storage",              "nasux-setup-storage"),
    ("termux-reload-settings",            "nasux-reload-settings"),
    ("termux-change-repo",                "nasux-change-repo"),
    ("termux-wake-unlock",                "nasux-wake-unlock"),
    ("termux-wake-lock",                  "nasux-wake-lock"),
    ("termux-clipboard-set",              "nasux-clipboard-set"),
    ("termux-clipboard-get",              "nasux-clipboard-get"),
    ("termux-notification",               "nasux-notification"),
    ("termux-am-socket",                  "nasux-am-socket"),
    ("termux-open-url",                   "nasux-open-url"),
    ("termux-fix-shebang",                "nasux-fix-shebang"),
    ("termux-keyring",                    "nasux-keyring"),
    ("termux-tools",                      "nasux-tools"),
    ("termux-backup",                     "nasux-backup"),
    ("termux-restore",                    "nasux-restore"),
    ("termux-reset",                      "nasux-reset"),
    ("termux-open",                       "nasux-open"),
    ("termux-toast",                      "nasux-toast"),
    ("termux-vibrate",                    "nasux-vibrate"),
    ("termux-auth",                       "nasux-auth"),
    ("termux-exec",                       "nasux-exec"),
    ("termux-core",                       "nasux-core"),
    ("termux-info",                       "nasux-info"),
    ("termux-api",                        "nasux-api"),
    ("termux-am",                         "nasux-am"),
    # generic: any remaining "termux" in path component
    ("termux",                            "nasux"),
    ("TERMUX",                            "NASUX"),
    ("Termux",                            "NasUX"),
]


def rename_entry(name: str) -> str:
    for old, new in NAME_REPLACEMENTS:
        name = name.replace(old, new)
    return name


# ─────────────────────────────────────────────────────────────────────────────
# PASS 2: TEXT file content replacements
# ─────────────────────────────────────────────────────────────────────────────
TEXT_REPLACEMENTS = [
    # Android package paths
    (b"/data/data/com.termux/files/usr/share/termux-keyring/",
     b"/data/data/com.nastech.nasux/files/usr/share/nasux-keyring/"),
    (b"/data/data/com.termux/files/usr/",
     b"/data/data/com.nastech.nasux/files/usr/"),
    (b"/data/data/com.termux/files/",
     b"/data/data/com.nastech.nasux/files/"),
    (b"/data/data/com.termux/",
     b"/data/data/com.nastech.nasux/"),
    # Android package names
    (b"com.termux.app",     b"com.nastech.nasux.app"),
    (b"com.termux.api",     b"com.nastech.nasux.api"),
    (b"com.termux.boot",    b"com.nastech.nasux.boot"),
    (b"com.termux.styling", b"com.nastech.nasux.styling"),
    (b"com.termux.widget",  b"com.nastech.nasux.widget"),
    (b"com.termux",         b"com.nastech.nasux"),
    # Env var names
    (b"TERMUX_APP_PACKAGE_NAME",  b"NASUX_APP_PACKAGE_NAME"),
    (b"TERMUX_APP_VERSION_NAME",  b"NASUX_APP_VERSION_NAME"),
    (b"TERMUX_APP_VERSION_CODE",  b"NASUX_APP_VERSION_CODE"),
    (b"TERMUX_APP_FILES_DIR",     b"NASUX_APP_FILES_DIR"),
    (b"TERMUX_APP_PID",           b"NASUX_APP_PID"),
    (b"TERMUX_APP_PLUGINS",       b"NASUX_APP_PLUGINS"),
    (b"TERMUX_APP_",              b"NASUX_APP_"),
    (b"TERMUX_PREFIX",            b"NASUX_PREFIX"),
    (b"TERMUX_HOME",              b"NASUX_HOME"),
    (b"TERMUX_VERSION",           b"NASUX_VERSION"),
    (b"TERMUX_HUSHLOGIN",         b"NASUX_HUSHLOGIN"),
    (b"TERMUX_MOTD",              b"NASUX_MOTD"),
    (b"TERMUX_COLORTERM",         b"NASUX_COLORTERM"),
    (b"TERMUX_AM_VERSION",        b"NASUX_AM_VERSION"),
    (b"TERMUX_AM_",               b"NASUX_AM_"),
    (b"TERMUX_EXEC_",             b"NASUX_EXEC_"),
    (b"TERMUX_",                  b"NASUX_"),
    # Shell function names
    (b"termux_core__bash__",   b"nasux_core__bash__"),
    (b"termux_core__sh__",     b"nasux_core__sh__"),
    (b"termux_exec__",         b"nasux_exec__"),
    (b"termux_am__",           b"nasux_am__"),
    (b"termux_api__",          b"nasux_api__"),
    (b"termux_apps_info_",     b"nasux_apps_info_"),
    (b"termux_scoped_",        b"nasux_scoped_"),
    (b"termux_setup_",         b"nasux_setup_"),
    (b"termux_reload_",        b"nasux_reload_"),
    (b"termux_wake_",          b"nasux_wake_"),
    (b"termux_change_",        b"nasux_change_"),
    (b"termux_reset_",         b"nasux_reset_"),
    (b"termux_open_",          b"nasux_open_"),
    (b"termux_backup_",        b"nasux_backup_"),
    (b"termux_restore_",       b"nasux_restore_"),
    # APT package names (specific before generic)
    (b"termux-apps-info-app-version-name", b"nasux-apps-info-app-version-name"),
    (b"termux-apps-info-env-variable",     b"nasux-apps-info-env-variable"),
    (b"termux-exec-system-linker-exec",    b"nasux-exec-system-linker-exec"),
    (b"termux-setup-package-manager",      b"nasux-setup-package-manager"),
    (b"termux-scoped-env-variable",        b"nasux-scoped-env-variable"),
    (b"termux-exec-ld-preload-lib",        b"nasux-exec-ld-preload-lib"),
    (b"termux-core-minimal",               b"nasux-core-minimal"),
    (b"termux-setup-storage",              b"nasux-setup-storage"),
    (b"termux-reload-settings",            b"nasux-reload-settings"),
    (b"termux-change-repo",                b"nasux-change-repo"),
    (b"termux-wake-unlock",                b"nasux-wake-unlock"),
    (b"termux-wake-lock",                  b"nasux-wake-lock"),
    (b"termux-clipboard-set",              b"nasux-clipboard-set"),
    (b"termux-clipboard-get",              b"nasux-clipboard-get"),
    (b"termux-notification",               b"nasux-notification"),
    (b"termux-am-socket",                  b"nasux-am-socket"),
    (b"termux-open-url",                   b"nasux-open-url"),
    (b"termux-fix-shebang",                b"nasux-fix-shebang"),
    (b"termux-keyring",                    b"nasux-keyring"),
    (b"termux-tools",                      b"nasux-tools"),
    (b"termux-backup",                     b"nasux-backup"),
    (b"termux-restore",                    b"nasux-restore"),
    (b"termux-reset",                      b"nasux-reset"),
    (b"termux-open",                       b"nasux-open"),
    (b"termux-toast",                      b"nasux-toast"),
    (b"termux-vibrate",                    b"nasux-vibrate"),
    (b"termux-auth",                       b"nasux-auth"),
    (b"termux-exec",                       b"nasux-exec"),
    (b"termux-core",                       b"nasux-core"),
    (b"termux-info",                       b"nasux-info"),
    (b"termux-api",                        b"nasux-api"),
    (b"termux-am",                         b"nasux-am"),
    # APT URLs
    (b"packages-cf.termux.dev", b"packages-cf.nasux.dev"),
    (b"packages.termux.dev",    b"packages.nasux.dev"),
    (b"termux-main",            b"nasux-main"),
    (b"termux-root",            b"nasux-root"),
    (b"termux-x11",             b"nasux-x11"),
    # GitHub URLs
    (b"github.com/termux/termux-packages", b"github.com/nastech-ai/NasUX-Packages"),
    (b"github.com/termux/termux-app",      b"github.com/nastech-ai/NasUX"),
    (b"github.com/termux/",               b"github.com/nastech-ai/"),
    (b"https://termux.dev",               b"https://nasux.dev"),
    (b"https://termux.org",               b"https://nasux.dev"),
    (b"termux.dev",                       b"nasux.dev"),
    (b"termux.org",                       b"nasux.dev"),
    # Shell vars
    (b"$termux_",  b"$nasux_"),
    (b"${termux_", b"${nasux_"),
    # Brand strings
    (b"Termux Package Manager", b"NasUX Package Manager"),
    (b"Termux installation",    b"NasUX installation"),
    (b"Termux Installation",    b"NasUX Installation"),
    (b"Termux packages",        b"NasUX packages"),
    (b"Termux package",         b"NasUX package"),
    (b"Termux Packages",        b"NasUX Packages"),
    (b"Termux Package",         b"NasUX Package"),
    (b"Termux script",          b"NasUX script"),
    (b"Termux scripts",         b"NasUX scripts"),
    (b"Termux app",             b"NasUX app"),
    (b"Termux App",             b"NasUX App"),
    (b"Termux environment",     b"NasUX environment"),
    (b"Termux terminal",        b"NasUX terminal"),
    (b"your Termux",            b"your NasUX"),
    (b"the Termux",             b"the NasUX"),
    (b"The Termux",             b"The NasUX"),
    (b"from Termux",            b"from NasUX"),
    (b"by Termux",              b"by NasUX"),
    (b"in Termux",              b"in NasUX"),
    (b"on Termux",              b"on NasUX"),
    (b"using Termux",           b"using NasUX"),
    (b"for Termux",             b"for NasUX"),
    (b"with Termux",            b"with NasUX"),
    (b"is Termux",              b"is NasUX"),
    (b"Termux:",                b"NasUX:"),
    (b"'Termux",                b"'NasUX"),
    (b'"Termux',                b'"NasUX'),
    (b"Termux",                 b"NasUX"),
    (b"termux",                 b"nasux"),
    (b"TERMUX",                 b"NASUX"),
]

# ─────────────────────────────────────────────────────────────────────────────
# PASS 3: ELF binary same-length patches
# ─────────────────────────────────────────────────────────────────────────────
ELF_REPLACEMENTS = [
    (b"com.termux",   b"nastech.ai"),    # 10=10
    (b"COM.TERMUX",   b"NASTECH.AI"),    # 10=10
    (b"termux.org",   b"nastech.io"),    # 10=10
    (b"termux.dev",   b"nastech.io"),    # 10=10
    (b"Termux\x00",   b"NasUX\x00\x00"),# 7=7
    (b"termux\x00",   b"nasux\x00\x00"),# 7=7
    (b"TERMUX\x00",   b"NASUX\x00\x00"),# 7=7
    (b" Termux ",     b" NasUX  "),      # 8=8
    (b" termux ",     b" nasux  "),      # 8=8
    (b" TERMUX ",     b" NASUX  "),      # 8=8
    (b"Termux\n",     b"NasUX \n"),      # 7=7
    (b"termux\n",     b"nasux \n"),      # 7=7
    (b'Termux"',      b'NasUX "'),       # 7=7
    (b'termux"',      b'nasux "'),       # 7=7
    (b"Termux'",      b"NasUX '"),       # 7=7
    (b"termux'",      b"nasux '"),       # 7=7
]
for _o, _n in ELF_REPLACEMENTS:
    assert len(_o) == len(_n), f"Pair mismatch: {_o!r}({len(_o)}) != {_n!r}({len(_n)})"

# ─────────────────────────────────────────────────────────────────────────────
# PASS 4: GPG keyring replacement (pure-Python minimal OpenPGP v4 RSA key)
# ─────────────────────────────────────────────────────────────────────────────
def _miller_rabin(n, k=10):
    if n < 2: return False
    if n == 2 or n == 3: return True
    if n % 2 == 0: return False
    r, d = 0, n - 1
    while d % 2 == 0: r += 1; d //= 2
    for _ in range(k):
        a = secrets.randbelow(n - 3) + 2
        x = pow(a, d, n)
        if x == 1 or x == n - 1: continue
        for __ in range(r - 1):
            x = pow(x, 2, n)
            if x == n - 1: break
        else:
            return False
    return True


def _gen_prime(bits):
    while True:
        p = secrets.randbits(bits) | (1 << (bits - 1)) | 1
        if _miller_rabin(p):
            return p


def _egcd(a, b):
    if a == 0: return b, 0, 1
    g, x, y = _egcd(b % a, a)
    return g, y - (b // a) * x, x


def _modinv(a, m):
    g, x, _ = _egcd(a % m, m)
    if g != 1: raise ValueError("No inverse")
    return x % m


def gen_openpgp_pubkey(uid: str) -> bytes:
    """Generate a minimal valid OpenPGP v4 RSA-2048 public key packet + UID packet."""
    print("    Generating NasUX RSA-2048 GPG key…", flush=True)
    # Generate RSA key
    e = 65537
    while True:
        p = _gen_prime(1024)
        q = _gen_prime(1024)
        if p == q: continue
        n = p * q
        phi = (p - 1) * (q - 1)
        if math.gcd(e, phi) == 1:
            break

    def mpi(x: int) -> bytes:
        """OpenPGP multi-precision integer encoding."""
        bit_len = x.bit_length()
        byte_len = (bit_len + 7) // 8
        return struct.pack(">H", bit_len) + x.to_bytes(byte_len, "big")

    creation_time = int(time.time())

    # --- Public key packet (tag 6) ---
    key_body = (
        b"\x04"                                     # version 4
        + struct.pack(">I", creation_time)           # creation timestamp
        + b"\x01"                                   # algorithm: RSA encrypt+sign
        + mpi(n)
        + mpi(e)
    )
    pub_pkt = _openpgp_packet(6, key_body)

    # --- UID packet (tag 13) ---
    uid_body = uid.encode("utf-8")
    uid_pkt = _openpgp_packet(13, uid_body)

    # --- Signature packet (tag 2) — self-signature, v4, type 0x13 ---
    # Hashed subpackets: sig creation time + key flags
    hashed_sub = (
        _openpgp_subpacket(2, struct.pack(">I", creation_time))  # sig creation time
        + _openpgp_subpacket(27, b"\x03")                         # key flags: cert+sign
    )
    # Unhashed subpackets: issuer fingerprint (dummy)
    key_fp = hashlib.sha1(b"\x99" + struct.pack(">H", len(key_body)) + key_body).digest()
    unhashed_sub = _openpgp_subpacket(16, key_fp[:8])  # issuer key ID

    sig_prefix = (
        b"\x04"                                     # version 4
        + b"\x13"                                   # sig type: positive cert
        + b"\x01"                                   # public key algo: RSA
        + b"\x08"                                   # hash algo: SHA-256
        + struct.pack(">H", len(hashed_sub)) + hashed_sub
    )
    # Hash the data being signed
    hash_data = (
        b"\x99" + struct.pack(">H", len(key_body)) + key_body   # key packet
        + b"\xb4" + struct.pack(">I", len(uid_body)) + uid_body  # uid packet
        + sig_prefix
        + b"\x04\xff" + struct.pack(">I", len(sig_prefix))
    )
    digest = hashlib.sha256(hash_data).digest()

    # Sign: RSA PKCS1v1.5 SHA-256
    d_key = _modinv(e, (p - 1) * (q - 1))
    # Construct PKCS1v1.5 block: 0x00 0x01 0xff...0xff 0x00 DER_HEADER HASH
    der_sha256 = bytes.fromhex(
        "3031300d060960864801650304020105000420") + digest
    em_len = (n.bit_length() + 7) // 8
    pad_len = em_len - 3 - len(der_sha256)
    em = b"\x00\x01" + b"\xff" * pad_len + b"\x00" + der_sha256
    m = int.from_bytes(em, "big")
    sig_int = pow(m, d_key, n)

    sig_body = (
        sig_prefix
        + struct.pack(">H", len(unhashed_sub)) + unhashed_sub
        + digest[:2]                                 # left 2 bytes of hash
        + mpi(sig_int)
    )
    sig_pkt = _openpgp_packet(2, sig_body)

    return pub_pkt + uid_pkt + sig_pkt


def _openpgp_packet(tag: int, body: bytes) -> bytes:
    hdr = 0xC0 | tag
    length = len(body)
    if length < 192:
        return bytes([hdr, length]) + body
    elif length < 8384:
        length -= 192
        return bytes([hdr, 192 + (length >> 8), length & 0xFF]) + body
    else:
        return bytes([hdr, 0xFF]) + struct.pack(">I", length) + body


def _openpgp_subpacket(subtype: int, body: bytes) -> bytes:
    length = 1 + len(body)
    if length < 192:
        return bytes([length, subtype]) + body
    else:
        length -= 192
        return bytes([192 + (length >> 8), length & 0xFF, subtype]) + body


# Pre-generate ONE key per run (reused across all 4 ZIPs for determinism)
_NASUX_GPG: bytes | None = None

NASUX_GPG_FILES = {
    "nasux-keyring/nastech-ai-releases.gpg": "NasTech AI Releases <releases@nastech.ai>",
    "nasux-keyring/nastech-ai-signing.gpg":  "NasTech AI Signing Key <security@nastech.ai>",
}


def get_nasux_gpg() -> dict:
    global _NASUX_GPG
    if _NASUX_GPG is None:
        _NASUX_GPG = {name: gen_openpgp_pubkey(uid)
                      for name, uid in NASUX_GPG_FILES.items()}
    return _NASUX_GPG


# ─────────────────────────────────────────────────────────────────────────────
# Branding injections for specific text files
# ─────────────────────────────────────────────────────────────────────────────
MOTD = (
    b"########################################\n"
    b"#                                      #\n"
    b"#   N A S U X   T E R M I N A L       #\n"
    b"#   Powered by NasTech AI              #\n"
    b"#                                      #\n"
    b"#   bash ~/nastech-agent/start.sh      #\n"
    b"#   to launch NasTech AI Agent         #\n"
    b"########################################\n\n"
)
PROFILE_FOOTER = (
    b"\n"
    b"# -- NasTech AI --\n"
    b"export TERM=xterm-256color\n"
    b"[ -f ~/.nasuxrc ] && source ~/.nasuxrc\n"
    b"alias nastech='bash ~/nastech-agent/start.sh'\n"
    b"alias nasux-ai='bash ~/nastech-agent/start.sh'\n"
)
SOURCES_LIST = (
    b"# NasUX Package Repository - powered by NasTech AI\n"
    b"# Source: https://github.com/nastech-ai/NasUX-Packages\n"
    b"deb https://packages-cf.nasux.dev/apt/nasux-main stable main\n"
)

# ─────────────────────────────────────────────────────────────────────────────
# File type detection helpers
# ─────────────────────────────────────────────────────────────────────────────
TEXT_EXTS = {
    "", ".sh", ".bash", ".py", ".pl", ".rb", ".lua", ".awk",
    ".conf", ".ini", ".cfg", ".list", ".sources", ".control",
    ".properties", ".info", ".txt", ".md", ".pc", ".la",
    ".m4", ".cmake", ".mk", ".spec", ".env",
    ".1",".2",".3",".4",".5",".6",".7",".8",
    ".profile", ".bashrc", ".zshrc", ".zsh",
    ".json", ".yaml", ".yml", ".toml",
    ".xml", ".html", ".htm", ".pem", ".crt",
}
BIN_EXTS = {
    ".so", ".a", ".o", ".ko",
    ".png", ".jpg", ".gif", ".ico",
    ".gz", ".bz2", ".xz", ".zst", ".lz4",
    ".zip", ".tar", ".deb", ".apk",
    ".ttf", ".otf", ".woff", ".woff2",
    ".pyc", ".pyo", ".class",
}


def is_elf(data: bytes) -> bool:
    return len(data) >= 4 and data[:4] == b"\x7fELF"

def is_ar(data: bytes) -> bool:
    return len(data) >= 7 and data[:7] == b"!<arch>"

def is_gpg(name: str) -> bool:
    return name.endswith(".gpg") or name.endswith(".asc")

def is_text(name: str, data: bytes) -> bool:
    ext = os.path.splitext(name)[1].lower()
    if ext in BIN_EXTS or is_elf(data) or is_ar(data): return False
    if ext in TEXT_EXTS: return True
    if len(data) >= 2 and data[:2] == b"#!": return True
    try:
        data[:512].decode("utf-8"); return True
    except UnicodeDecodeError:
        pass
    sample = data[:512]
    non_print = sum(1 for b in sample if b < 9 or (13 < b < 32) or b == 127)
    return non_print < len(sample) * 0.10


# ─────────────────────────────────────────────────────────────────────────────
# Content patchers
# ─────────────────────────────────────────────────────────────────────────────
def patch_text_content(name: str, data: bytes) -> bytes:
    for old, new in TEXT_REPLACEMENTS:
        data = data.replace(old, new)
    # Branding injections
    base = os.path.basename(name)
    if base in ("motd", "motd.sh"):
        data = MOTD + data
    if base in ("profile", "bash.bashrc") or name.endswith("nasux-bash-completions.sh"):
        data = data + PROFILE_FOOTER
    if name == "etc/apt/sources.list":
        data = SOURCES_LIST
    return data


def patch_elf_content(data: bytes) -> bytes:
    for old, new in ELF_REPLACEMENTS:
        data = data.replace(old, new)
    return data


# ─────────────────────────────────────────────────────────────────────────────
# Main ZIP patcher
# ─────────────────────────────────────────────────────────────────────────────
def patch_zip(src: str, dst: str) -> str:
    print(f"  Patching {os.path.basename(src)} …", flush=True)
    gpg_keys   = get_nasux_gpg()
    buf        = io.BytesIO()
    t = e = s = g = renamed = 0

    with zipfile.ZipFile(src, "r") as zin, \
         zipfile.ZipFile(buf, "w",
                         compression=zipfile.ZIP_DEFLATED,
                         compresslevel=6) as zout:

        for info in zin.infolist():
            raw      = zin.read(info.filename)
            old_name = info.filename
            new_name = rename_entry(old_name)
            if new_name != old_name:
                renamed += 1

            # Skip existing termux-keyring entries (replaced by NasUX keys below)
            if "nasux-keyring" in old_name or "termux-keyring" in old_name:
                if old_name.endswith("/"):
                    continue          # drop old dir entry (re-added via new keys)
                g += 1
                continue              # drop old .gpg files

            # Classify & patch content
            if info.is_dir():
                patched = raw
            elif is_elf(raw) or is_ar(raw):
                patched = patch_elf_content(raw); e += 1
            elif is_text(new_name, raw):
                patched = patch_text_content(new_name, raw); t += 1
            else:
                patched = raw; s += 1

            ni              = zipfile.ZipInfo(new_name)
            ni.compress_type = (zipfile.ZIP_STORED
                                 if new_name == "SYMLINKS.txt"
                                 else zipfile.ZIP_DEFLATED)
            ni.external_attr = info.external_attr
            ni.date_time     = info.date_time
            ni.comment       = info.comment
            zout.writestr(ni, patched)

        # Add NasUX keyring directory + keys
        for rel_path, key_bytes in gpg_keys.items():
            full = f"share/{rel_path}"
            ni   = zipfile.ZipInfo(full)
            ni.compress_type = zipfile.ZIP_DEFLATED
            ni.external_attr = 0o644 << 16
            ni.date_time     = time.localtime()[:6]
            zout.writestr(ni, key_bytes)

    data    = buf.getvalue()
    sha256  = hashlib.sha256(data).hexdigest()
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    with open(dst, "wb") as f: f.write(data)
    print(f"  OK  {os.path.basename(dst):30s}  "
          f"{len(data)/1024/1024:.1f} MB  "
          f"renamed={renamed} text={t} elf={e} gpg→nasux={g} skip={s}  "
          f"sha256={sha256[:16]}…", flush=True)
    return sha256


# ─────────────────────────────────────────────────────────────────────────────
# Verification
# ─────────────────────────────────────────────────────────────────────────────
def verify_zip(path: str):
    z    = zipfile.ZipFile(path)
    elf_hits  = []
    text_hits = []
    name_hits = [n for n in z.namelist() if 'termux' in n.lower()]
    for name in z.namelist():
        if name.endswith("/"): continue
        try:
            data  = z.read(name)
            lower = data.lower()
            if b"termux" in lower:
                if is_elf(data) or is_ar(data):
                    elf_hits.append(name)
                else:
                    idx  = lower.find(b"termux")
                    ctx  = data[max(0,idx-20):idx+40]
                    text_hits.append((name, ctx))
        except Exception:
            pass
    arch = os.path.basename(path).replace("bootstrap-","").replace(".zip","")
    print(f"\n  [{arch}] post-patch audit:")
    print(f"    ZIP entry names with termux : {len(name_hits)}")
    for n in name_hits[:5]: print(f"      {n}")
    print(f"    ELF binary hits (compiled-in): {len(elf_hits)}"
          f"  (symbol data — needs source rebuild to fully remove)")
    print(f"    Text/script hits : {len(text_hits)}")
    for n, ctx in text_hits[:8]:
        print(f"      {n}: {ctx}")


# ─────────────────────────────────────────────────────────────────────────────
# Entry point
# ─────────────────────────────────────────────────────────────────────────────
def main():
    do_verify = "--verify" in sys.argv

    if "--all" in sys.argv or len(sys.argv) == 1:
        print("NasUX Bootstrap Patcher v4 — all 4 architectures\n")
        results = {}
        for arch in ARCHS:
            src = os.path.join(SRC_DIR, f"bootstrap-{arch}.zip")
            dst = os.path.join(DEST_DIR, f"bootstrap-{arch}.zip")
            if not os.path.exists(src):
                print(f"  SKIP  {src} not found"); continue
            results[arch] = patch_zip(src, dst)
            if do_verify: verify_zip(dst)
        print("\n── Updated checksums for build.gradle ──")
        for arch in ARCHS:
            if arch in results:
                print(f'            downloadBootstrap'
                      f'("{arch}", "{results[arch]}", version)')
        return

    if len(sys.argv) >= 3 and not sys.argv[1].startswith("--"):
        sha = patch_zip(sys.argv[1], sys.argv[2])
        if do_verify: verify_zip(sys.argv[2])
        print(f"\nSHA-256: {sha}")
        return

    print(__doc__); sys.exit(1)


if __name__ == "__main__":
    main()
