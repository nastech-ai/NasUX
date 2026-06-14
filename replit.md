# NasUX — Powered by NasTech AI

NasUX is an Android terminal emulator app enhanced with the NasTech AI Agent permanently pre-installed. Built for users who can't afford a PC — full Linux + AI capabilities on any Android device.

## Project Structure

```
.
├── app/                          # Main Android application (NasUX)
│   ├── src/main/java/com/nastech/nasux/   # Java source (package: com.nastech.nasux)
│   ├── src/main/res/             # Android resources (strings, layouts, XML)
│   ├── src/main/cpp/             # Native C bootstrap (nasux-bootstrap.c)
│   └── src/main/assets/
│       └── nastech-agent/        # NasTech AI Agent (bundled, auto-installed on first run)
├── nasux-shared/                 # Shared library (NasUXConstants, utilities, settings)
│   └── src/main/java/com/nastech/nasux/shared/
├── terminal-emulator/            # Core terminal emulation (JNI/Java)
├── terminal-view/                # Android View for rendering the terminal
└── settings.gradle               # Modules: :app, :nasux-shared, :terminal-emulator, :terminal-view
```

## Build System
- **Gradle** (Android Gradle Plugin)
- **NDK** via `ndk-build` for native C code (`libnasux-bootstrap`, `libnasux`)
- **Package**: `com.nastech.nasux`
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 28

## Key Branding
- App name: **NasUX**
- Powered by: **NasTech AI**
- Package: `com.nastech.nasux`
- Module: `nasux-shared`
- APK output: `nasux-app_<variant>_<abi>.apk`

## NasTech AI Agent Integration
The NasTech Agent is bundled in `app/src/main/assets/nastech-agent/` and is automatically copied to `~/nastech-agent/` on first app launch via `NasUXInstaller.installNasTechAgent()`. Key scripts (`install.sh`, `start.sh`, `nastech`) are made executable automatically.

Users can run `bash ~/nastech-agent/install.sh` to complete the Python environment setup, then use the `nastech` CLI.

## Platform Support — Package Sources
NasUX uses **Kali NetHunter** (proot-distro) as its Android Linux environment. All package operations use Kali APT repos — **not NasUX**.

| Environment | Package Manager | Install Python |
|---|---|---|
| Kali NetHunter (proot) | `apt` | `apt update && apt install -y python3 python3-pip python3-venv` |
| Desktop/Server | `uv` / `pip` | system Python or python.org |

Detection in code (`nastech_constants.py`):
- `is_kali_nethunter()` — checks `KALI_VERSION` env, `/etc/kali_release`, `ID=kali` in `/etc/os-release`, or `/data/data/com.offsec.nethunter`
- `get_environment_type()` returns `"kali-nethunter"` for Kali proot, `"nasux"` for native NasUX shell
- `is_nasux_proot_distro()` returns `True` for Kali (subcase of proot detection)

## Building
Requires Android SDK and NDK. Cannot be run as a web app — this is a native Android APK project.

```bash
./gradlew assembleDebug
```

## User Preferences
- All branding is NasUX / NasTech AI throughout the entire codebase
- NasTech Agent is permanently bundled and auto-installs on first boot
- Package source is Kali Linux APT (not NasUX pkg) — use `apt install` everywhere
