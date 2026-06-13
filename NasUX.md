# NasUX — Powered by NasTech AI

> **Full Linux terminal + AI agent on any Android device.**
> Built for everyone who needs a PC-grade environment in their pocket.

---

## Table of Contents

1. [What Is NasUX?](#what-is-nasux)
2. [Features](#features)
3. [NasTech AI Agent](#nastech-ai-agent)
4. [Installation](#installation)
5. [First Run](#first-run)
6. [Using the Terminal](#using-the-terminal)
7. [NasTech CLI Commands](#nastech-cli-commands)
8. [Theme & Customization](#theme--customization)
9. [Architecture](#architecture)
10. [Building from Source](#building-from-source)
11. [CI/CD & Automation](#cicd--automation)
12. [Contributing](#contributing)
13. [License](#license)

---

## What Is NasUX?

NasUX is a **free, open-source Android terminal emulator** enhanced with the **NasTech AI Agent** permanently pre-installed. It gives anyone with an Android phone access to a full Linux command-line environment — no PC, no root, no paid subscription required.

**Who is it for?**
- Students and developers in emerging markets who cannot afford a laptop
- Power users who want AI-assisted command-line workflows on the go
- Anyone who wants a pocket-sized Linux + AI workstation

---

## Features

### Terminal Emulator
| Feature | Detail |
|---|---|
| Full VT100/xterm emulation | Compatible with virtually all CLI software |
| Multiple sessions | Run several shells side-by-side |
| Extra-keys toolbar | One-tap access to Ctrl, Tab, arrow keys, pipes |
| Text input overlay | Comfortable long-form text entry |
| URL linking | Tap URLs in terminal output to open them |
| Fullscreen mode | Maximum screen real estate |
| Hardware keyboard | Full key mapping for physical keyboards |
| Customisable font | Size, family, and ligature support |

### AMOLED UI
| Feature | Detail |
|---|---|
| Pure-black AMOLED theme | Saves battery on OLED screens |
| 12 built-in colour presets | Dracula, Solarized, Nord, Monokai, and more |
| Custom wallpaper support | Set any image as terminal background |
| Neon teal accent | NasTech brand accent `#00C8B8` throughout |
| Slide-out session drawer | Quick-switch between terminal sessions |

### Shizuku Integration (Optional)
Run ADB-level commands without root using [Shizuku](https://shizuku.rikki.dev/):
- Execute privileged shell commands
- Access system APIs normally locked to root
- Works over ADB wireless debugging (no PC needed after first setup)

### NasTech AI Agent (see below)
- Built-in AI chat and code assistant
- Persistent across sessions
- Accessible via FAB button or `nastech` CLI

---

## NasTech AI Agent

The NasTech AI Agent is **bundled inside the APK** and automatically installed to `~/nastech-agent/` on first launch — no internet download required for the core files.

### Architecture

```
~/nastech-agent/
├── install.sh          # Full Python environment setup (run once)
├── start.sh            # Launch the agent server
├── nastech             # CLI entry point
├── agent/
│   ├── core.py         # AI conversation engine
│   ├── tools/          # File, shell, search tool plugins
│   └── config.yml      # Model and behaviour settings
└── venv/               # Python virtual environment (created by install.sh)
```

### First-Time Setup

```bash
# Inside NasUX terminal — run once after first boot:
bash ~/nastech-agent/install.sh
```

This installs the Python 3 virtual environment and all dependencies (`openai`, `rich`, `httpx`, etc.).

### Using the Agent

```bash
# Start an AI chat session
nastech chat

# Ask a single question
nastech ask "How do I sort a file by column 2?"

# Get AI to write and run a bash script
nastech run "Create a script that backs up my home directory"

# Check agent status
nastech status

# Update the agent
nastech-update
```

### Accessing the Agent in the UI

Tap the **glowing teal FAB** (floating action button) in the bottom-right corner of the terminal, or open the **side drawer** and tap the NasTech AI section.

---

## Installation

### Download a Pre-Built APK

Go to the [Releases](https://github.com/nastech-ai/NasUX/releases) page and download the APK for your device:

| File | Target Device |
|---|---|
| `nasux-app_*_arm64-v8a.apk` | **Recommended** — most modern phones (Pixel, Samsung, OnePlus, Xiaomi) |
| `nasux-app_*_armeabi-v7a.apk` | Older 32-bit Android phones |
| `nasux-app_*_universal.apk` | Any Android device (larger download) |
| `nasux-app_*_x86_64.apk` | Android emulators, x86 tablets |
| `nasux-app_*_x86.apk` | 32-bit Android emulators |

> **Verify integrity:** Each release includes a `*_sha256sums.txt` file. Check your download with:
> ```bash
> sha256sum -c nasux-app_*_sha256sums.txt
> ```

### APK Variants

NasUX is published in two **bootstrap variants**:

| Variant | Meaning |
|---|---|
| `apt-android-7` | Bootstrap built for Android 7+ (recommended) |
| `apt-android-5` | Bootstrap built for Android 5+ (legacy devices) |

### Enable Unknown Sources

Before installing, allow APKs from unknown sources:
- Android 8+: **Settings → Apps → Special app access → Install unknown apps** → enable for your file manager
- Android 7 and below: **Settings → Security → Unknown sources** → enable

---

## First Run

1. **Install the APK** and open NasUX.
2. The **bootstrap installer** runs automatically — this sets up the Linux filesystem in your app's private storage. Takes 1–2 minutes on first boot.
3. The **NasTech AI Agent** is copied to `~/nastech-agent/` automatically.
4. A terminal session opens. You're in a bash shell with a standard Linux environment.
5. Run the agent setup to enable AI:
   ```bash
   bash ~/nastech-agent/install.sh
   ```

---

## Using the Terminal

### Session Management

| Action | How |
|---|---|
| New session | Tap `+` in the top toolbar or swipe the drawer open |
| Switch session | Swipe open the left drawer and tap a session |
| Rename session | Long-press the session name in the drawer |
| Close session | Type `exit` or long-press → Close |

### Extra-Keys Bar

The toolbar above the keyboard provides quick access to:
`ESC` `TAB` `CTRL` `ALT` `→` `←` `↑` `↓` `|` `&` `~` `-` `_` and more.

Tap the `≡` icon to toggle the extra-keys bar.

### Text Input

Tap the keyboard icon to open the text input overlay — useful for pasting long commands or writing multi-line scripts.

---

## NasTech CLI Commands

After running `install.sh`, these commands are available from the terminal:

| Command | Description |
|---|---|
| `nastech chat` | Interactive AI conversation |
| `nastech ask "<question>"` | Single-shot question/answer |
| `nastech run "<task>"` | AI generates and executes a shell task |
| `nastech file <path>` | AI analyses a file and explains it |
| `nastech status` | Agent health check |
| `nastech-update` | Update the NasTech agent to the latest version |
| `nastech-setup` | Re-run the agent setup |

---

## Theme & Customization

Open **Settings → NasUX → Appearance** or tap the palette icon in the toolbar.

### Colour Presets

| Preset | Description |
|---|---|
| NasTech Dark | Default AMOLED black + teal accent |
| Dracula | Purple-based dark theme |
| Solarized Dark | Warm, low-contrast dark |
| Solarized Light | Light variant |
| Nord | Cool blue-grey tones |
| Monokai | Classic Monokai colours |
| Gruvbox Dark | Warm retro terminal |
| One Dark | Atom-inspired dark |
| Material Dark | Material Design dark |
| Tomorrow Night | Muted dark with bright accents |
| Zenburn | Low-contrast eye-friendly |
| Custom | Set individual ANSI colours |

### Font

Change the terminal font under **Settings → Terminal → Font**. Supports any monospaced TTF/OTF font loaded from your device storage.

### Extra Keys

Customise the extra-keys bar layout in **Settings → Terminal → Extra Keys**. The layout is defined as a JSON array of rows and key names.

---

## Architecture

```
NasUX/
├── app/                              # Main Android application
│   ├── src/main/
│   │   ├── java/com/nastech/nasux/
│   │   │   └── app/                 # Activities, services, managers
│   │   │       ├── NasUXActivity.java            # Main terminal activity
│   │   │       ├── NasUXService.java             # Background terminal service
│   │   │       ├── NasUXInstaller.java           # Bootstrap + agent installer
│   │   │       ├── NasUXThemeManager.java        # Theme engine
│   │   │       ├── NasUXShizukuManager.java      # Shizuku privileged API
│   │   │       ├── NasUXVoiceManager.java        # Voice input
│   │   │       └── NasUXSplashActivity.java      # First-run splash
│   │   ├── res/                     # XML layouts, strings, colours
│   │   ├── cpp/                     # Native bootstrap C code
│   │   └── assets/
│   │       └── nastech-agent/       # Bundled AI agent (auto-installed)
│   └── build.gradle
│
├── nasux-shared/                    # Shared library module
│   └── src/main/java/com/nastech/nasux/shared/
│       ├── nasux/                   # NasUX-specific utilities
│       │   ├── NasUXBootstrap.java  # Bootstrap package management
│       │   ├── NasUXConstants.java  # App-wide constants
│       │   └── extrakeys/           # Extra-keys view system
│       └── shell/                   # Shell execution framework
│
├── terminal-emulator/               # VT100/xterm emulator core (JNI)
├── terminal-view/                   # Android View rendering the terminal
├── nasux-am-library/                # Local am (activity manager) library
└── .github/workflows/               # CI/CD automation
```

### Key Modules

| Module | Role |
|---|---|
| `:app` | Android application shell, UI, activities |
| `:nasux-shared` | Cross-module utilities, settings, shell runner |
| `:terminal-emulator` | VT100/xterm byte-level state machine |
| `:terminal-view` | Android `View` that renders the emulator |
| `:nasux-am-library` | Activity manager (am) socket bridge for ADB commands |

---

## Building from Source

### Requirements

| Tool | Version |
|---|---|
| Android SDK | API 36 (compileSdk) |
| Android NDK | 29.0.14206865 |
| Java | 17 (Temurin recommended) |
| Gradle | Bundled via wrapper |

### Clone & Build

```bash
git clone https://github.com/nastech-ai/NasUX.git
cd NasUX

# Debug build (split APKs per ABI)
./gradlew assembleDebug

# APKs are output to:
# app/build/outputs/apk/debug/nasux-app_*_<abi>.apk
```

### Build Variants

```bash
# Specific package variant (bootstrap flavour)
NASUX_PACKAGE_VARIANT=apt-android-7 ./gradlew assembleDebug

# Release build (unsigned — sign with your own keystore)
./gradlew assembleRelease
```

### Key `gradle.properties` Values

| Property | Value | Notes |
|---|---|---|
| `minSdkVersion` | 24 | Android 7.0 minimum |
| `targetSdkVersion` | 28 | Android 9 target |
| `compileSdkVersion` | 36 | Latest SDK |
| `ndkVersion` | 29.0.14206865 | Required for native bootstrap |
| `markwonVersion` | 4.6.2 | Markdown rendering library |

---

## CI/CD & Automation

All CI/CD runs in GitHub Actions. Every push to `master` triggers the full pipeline:

### Workflows

| Workflow | Trigger | Purpose |
|---|---|---|
| **Build NasUX** | Push to master, PRs | Compiles all modules, builds debug APKs for all ABIs |
| **Unit Tests** | Push to master, PRs | Runs JVM unit tests |
| **NasTech Brand Enforcement** | Push to master, PRs | Blocks any leftover non-NasUX branding |
| **Validate Gradle Wrapper** | Push to master, PRs | Verifies Gradle wrapper checksum |
| **Android Lint & Code Health** | Push to master, PRs | Lint, missing R-import scanner, resource health checks |
| **NasTech Auto-Fix Bot** | Push to master | Auto-commits fixes for R imports, whitespace, CRLF |
| **CodeQL Security Analysis** | Push, weekly | SAST security scan of Java source |
| **Dependency Audit** | Weekly, dep file change | CVE check, AndroidX update check, Gradle version check |
| **Changelog Generator** | Tag push `v*.*.*` | Auto-generates CHANGELOG.md entry from commits |
| **NasUX Auto-Publish** | After Build NasUX passes | Builds release APKs, creates GitHub Release, posts download table |
| **NasTech Dependency Auto-Update** | Weekly (Monday 08:00 UTC) | Bumps Python dependency pins in setup scripts |
| **NasUX Auto-Release** | Tag push `v*.*.*` | Full release build with both bootstrap variants |
| **Attach Debug APKs To Release** | Release published | Attaches debug APKs to the release |

### Release Flow

```
Push to master
    │
    ├─► Build NasUX ──────────────────► (if pass)
    │                                        │
    ├─► Lint & Health checks                 ▼
    │                                  Auto-Publish
    ├─► Unit Tests                           │
    │                                   Builds APKs
    ├─► Brand Enforcement                    │
    │                               Creates GitHub Release
    └─► CodeQL (Java)                        │
                                     Attaches all APKs + checksums
                                             │
                                    (on tag push)
                                             │
                                    Changelog Generator
                                    updates CHANGELOG.md
```

---

## Contributing

1. **Fork** the repository on GitHub
2. **Create a branch**: `git checkout -b feat/my-feature`
3. **Follow the naming convention** — all classes, packages, and resources must use the `NasUX` / `NasTech` / `com.nastech.nasux` namespace. The Brand Enforcement CI check will block any leftover Termux references.
4. **Commit** using [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat: add voice command support`
   - `fix: correct R import in ThemeActivity`
   - `docs: update installation guide`
5. **Push** and open a **Pull Request** against `master`
6. All CI checks must pass before merge

### Code Style

- Java 8 language level (Android compatible)
- No `System.out.println` — use `com.nastech.nasux.shared.logger.Logger`
- No empty `catch` blocks — log or rethrow
- Resource strings go in `app/src/main/res/values/strings.xml`
- Colours go in `app/src/main/res/values/colors.xml`
- Sub-package Activities **must** import `com.nastech.nasux.R` explicitly

---

## License

NasUX is released under the **Apache License 2.0**.

The terminal emulator core (`terminal-emulator/`, `terminal-view/`) is derived from open-source terminal emulator libraries. The NasTech AI Agent and NasUX-specific code are original works by the NasTech AI team.

---

<p align="center">
  <strong>NasUX</strong> — Powered by <strong>NasTech AI</strong><br>
  Full Linux + AI on any Android device.
</p>
