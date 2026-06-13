# NasUX Changelog

All notable changes are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [0.119.0] — 2026-06-13

### ✨ New Features
- NasTech AI Agent permanently bundled and auto-installs on first boot (`~/nastech-agent/`)
- Glowing teal NasTech FAB for instant AI summoning from the terminal
- AMOLED pure-black theme with neon teal accent (`#00C8B8`)
- 12 built-in colour presets (Dracula, Solarized, Nord, Monokai, Gruvbox, and more)
- Custom wallpaper support for terminal background
- `nastech` CLI commands: `chat`, `ask`, `run`, `file`, `status`
- `nastech-update` command for in-place agent updates
- Shizuku integration — ADB-level commands without root (requires Shizuku app)
- Voice input support via `NasUXVoiceManager`
- Multiple terminal sessions with slide-out drawer
- Full hardware keyboard support with custom key mapping
- Extra-keys toolbar (Ctrl, Tab, arrow keys, pipes, and more)

### 🔒 Security
- CodeQL security scanning on every push (Java source)
- Dependency vulnerability audit (weekly, CVE database check)
- SHA-256 checksums published with every APK release

### 🏗️ Build & CI
- All CI workflows fully green on GitHub Actions
- Auto-Publish: APKs build and release automatically when Build NasUX passes
- Changelog Generator: CHANGELOG.md auto-updated on every version tag
- NasTech Issue Bot: auto-opens GitHub issues on CI failure, auto-closes on fix
- Android Lint & Code Health: missing R-import scanner, resource health checks
- NasTech Auto-Fix Bot: auto-commits fixes for R imports, whitespace, CRLF
- Dependabot: weekly updates for GitHub Actions, Gradle, and Python dependencies
- Downgraded Shizuku 13.1.5 → 12.2.0 (`newProcess()` public in 12.x)
- `minSdkVersion` set to 24 (Android 7.0)
- Added local `nasux-am-library` module (am/activity-manager socket bridge)
- Added 9 missing string resources (`ok`, `yes`, `no`, `cancel`, `action_yes`, etc.)
- Added missing `import com.nastech.nasux.R` to 4 sub-package Java files

---

## [0.118.0] — 2026-06-01

### ✨ New Features
- Complete rebrand: Termux → NasUX / NasTech AI throughout codebase
- Package renamed to `com.nastech.nasux`
- Module renamed to `nasux-shared`
- All user-facing strings, colours, and resources rebranded
- NasTech brand accent colour `#00C8B8` (teal)

### 🏗️ Build & CI
- NasTech Brand Enforcement CI check added
- Gradle wrapper validation workflow added

---

*This changelog is auto-updated by the Changelog Generator workflow on every version tag push.*
