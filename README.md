# NasUX — Powered by NasTech AI

[![Build NasUX](https://github.com/nastech-ai/NasUX-App/workflows/Build%20NasUX/badge.svg)](https://github.com/nastech-ai/nasux-app/actions)
[![NasTech AI Agent](https://img.shields.io/badge/NasTech%20AI-bundled-00D4AA)](https://github.com/nastech-ai/NasTech-Agent)
[![Min SDK](https://img.shields.io/badge/Android-5.0%2B-blue)](https://github.com/nastech-ai/nasux-app)
[![License](https://img.shields.io/badge/license-Apache%202.0-green)](LICENSE.md)

**NasUX** is an Android terminal emulator app with the **NasTech AI Agent** permanently pre-installed — full Linux + AI capabilities on any Android device, no PC required.

---

## What is NasUX?

NasUX gives you a complete, professional-grade Linux terminal on Android, bundled with an autonomous AI agent that can run commands, browse the web, write code, manage files, and answer questions — all from your phone.

**Built for users who can't afford a PC.** Full Linux. Full AI. Any Android.

---

## Features

| Feature | Details |
|---|---|
| **Full Linux Terminal** | Bash/Zsh, apt/pkg, full POSIX environment |
| **NasTech AI Agent** | Bundled, auto-installed, runs autonomously |
| **Termius-style AMOLED UI** | Dark navy #0D1117, NasTech teal accent |
| **AI Sidebar** | Quick buttons: Start AI, Chat, Update, Install Deps, Setup |
| **200+ Python packages** | Auto-installed on first run via setup script |
| **100+ Node.js packages** | npm/pnpm global tools ready to go |
| **Auto-update** | `bash ~/nastech-agent/update.sh` keeps everything current |
| **Setup Wizard** | First-run guided setup with step-by-step instructions |
| **Splash Screen** | NasTech AI branded launch screen |
| **Termius Key Toolbar** | Extra-keys row for Ctrl, Tab, arrows and more |

---

## Installation

### Download Pre-built APK

Download the latest `nasux-app_*.apk` from [Releases](https://github.com/nastech-ai/nasux-app/releases).

Choose your ABI:
- `arm64-v8a` — most modern Android phones (recommended)
- `armeabi-v7a` — older 32-bit phones
- `x86_64` — Android emulators / ChromeOS
- `universal` — works on all architectures

Enable **Install from Unknown Sources** in your Android settings, then install the APK.

---

## First Run — Setup Wizard

On first launch, NasUX shows a **4-step Setup Wizard**:

1. **Welcome** — overview of NasUX and NasTech AI
2. **NasTech AI** — what the agent can do
3. **Install Dependencies** — copy-paste commands shown in-app
4. **Ready to Launch** — start the AI agent

After the wizard, run these commands in the terminal to complete setup:

```bash
# Install system packages
pkg install python nodejs git curl wget -y

# Install NasTech AI agent
bash ~/nastech-agent/install.sh

# Add your AI API key (OpenAI, Anthropic, or OpenRouter)
nano ~/nastech-agent/.env

# Launch NasTech AI
nastech
```

---

## NasTech AI Quick Commands (Sidebar)

Open the left sidebar (swipe from the left edge) to access:

| Button | Action |
|---|---|
| **▶ Start AI Agent** | Launches `nastech` — the AI CLI |
| **Chat** | Open NasTech AI chat interface |
| **Update** | Run `~/nastech-agent/update.sh` |
| **Install Deps** | Run `~/nastech-agent/setup-all.sh` |
| **Setup** | Run `~/nastech-agent/install.sh` |

---

## Installing All Dependencies

NasUX bundles a comprehensive setup script that installs 200+ Python packages and 100+ Node.js packages:

```bash
bash ~/nastech-agent/setup-all.sh
```

This script installs:
- Python: openai, anthropic, fastapi, pandas, numpy, torch, transformers, and 200+ more
- Node: typescript, express, react, vue, prisma, langchain, and 100+ more
- System: git, curl, wget, openssh, ffmpeg, imagemagick, and more

---

## Auto-Update

Keep NasUX and NasTech AI current:

```bash
# Update NasTech AI and all packages
bash ~/nastech-agent/update.sh

# Or use the alias (after setup)
nastech-update
```

---

## Build From Source

Requires Android SDK + NDK.

```bash
git clone https://github.com/nastech-ai/nasux-app
cd nasux-app
./gradlew assembleDebug
```

APKs are output to `app/build/outputs/apk/debug/` as `nasux-app_*.apk`.

### Project Structure

```
.
├── app/                          # NasUX Android application
│   ├── src/main/java/com/nastech/nasux/   # Java sources
│   ├── src/main/res/             # Layouts, colors, themes, drawables
│   ├── src/main/cpp/             # Native C bootstrap
│   └── src/main/assets/
│       ├── nastech-agent/        # NasTech AI Agent (bundled)
│       ├── nasux-setup-all.sh    # 200+ package installer
│       ├── nasux-update.sh       # Auto-updater
│       └── nasux-colors.properties  # Termius-style terminal colors
├── nasux-shared/                 # Shared library (constants, utilities)
├── terminal-emulator/            # Core terminal emulation (JNI)
└── terminal-view/                # Android View for rendering
```

---

## Branding

| Key | Value |
|---|---|
| App name | **NasUX** |
| Powered by | **NasTech AI** |
| Package | `com.nastech.nasux` |
| APK prefix | `nasux-app_` |
| Primary color | `#00D4AA` (NasTech teal) |
| Background | `#0D1117` (AMOLED dark navy) |

---

## License

NasUX is open source under the [Apache 2.0 License](LICENSE.md).

NasTech AI Agent is a separate project — see [nastech-ai/NasTech-Agent](https://github.com/nastech-ai/NasTech-Agent) for its license and issues.

---

## Contributing

Pull requests and issues are welcome. Please use the issue templates provided.

For NasTech AI Agent bugs, open issues at [nastech-ai/NasTech-Agent](https://github.com/nastech-ai/NasTech-Agent/issues) instead.
