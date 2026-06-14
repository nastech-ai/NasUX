---
name: Kali NetHunter platform
description: Android environment for NasUX uses Kali Linux proot (apt), not Termux (pkg). Detection and package manager logic.
---

## Rule
All Android package operations in NasUX use Kali Linux APT repos via proot-distro — never Termux `pkg`.

**Why:** User switched from Termux to Kali NetHunter as the Android Linux environment. Kali provides a full Kali Linux CLI + toolset via proot/chroot on Android.

**How to apply:** Whenever guiding users to install a system package on Android, always say `apt install <pkg>` (Kali), never `pkg install <pkg>` (Termux).

## Detection (`nastech_constants.py`)
- `is_kali_nethunter()` — new function; checks `KALI_VERSION` env var, `/etc/kali_release` file, `ID=kali` in `/etc/os-release`, or `/data/data/com.offsec.nethunter` dir
- `get_environment_type()` returns `"kali-nethunter"` for Kali proot (checked before `"android-proot"`)
- `is_nasux_proot_distro()` returns `True` for Kali (calls `is_kali_nethunter()` as a signal)

## install.sh (`INSTALL_MODE`)
- `INSTALL_MODE="kali"` set when `is_kali_nethunter()` or `is_proot_distro()` is true
- Kali mode: Python checked as `python3`, pip used (not uv), no `pkg` references
- Error hint: `apt update && apt install -y python3 python3-pip python3-venv`

## Files changed
- `nastech_constants.py` — added `is_kali_nethunter()`, updated `is_nasux_proot_distro()` and `get_environment_type()`
- `install.sh` — added `is_kali_nethunter()` + `is_proot_distro()` bash helpers, new `kali` INSTALL_MODE, replaced all `pkg install python` hints with `apt install python3 ...`
- `nastech` (CLI entrypoint) — replaced `pkg install python` error hint and fallback help
- `nastech_launcher.py` — replaced `pkg install python` error hint
