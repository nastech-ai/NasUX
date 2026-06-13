<p align="center">
  <img src="assets/banner.png" alt="NasTech Agent" width="100%">
</p>
<p align="center">
  <a href="https://docs.nastech-agent.workers.dev/docs/"><img src="https://img.shields.io/badge/Docs-nastech--agent.nastech.com-FFD700?style=for-the-badge" alt="Documentation"></a>
  <a href="https://discord.gg/NasTech"><img src="https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord"></a>
  <a href="https://github.com/nastech-ai/NasTech-Agent/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License: MIT"></a>
  <a href="https://nastech.com"><img src="https://img.shields.io/badge/Built%20by-Nous%20Research-blueviolet?style=for-the-badge" alt="Built by NasTech"></a>
  <a href="README.zh-CN.md"><img src="https://img.shields.io/badge/Lang-中文-red?style=for-the-badge" alt="中文"></a>
</p>

**The self-improving AI agent built by [NasTech](https://nastech.com).** It's the only agent with a built-in learning loop — it creates skills from experience, improves them during use, nudges itself to persist knowledge, searches its own past conversations, and builds a deepening model of who you are across sessions. Run it on a $5 VPS, a GPU cluster, or serverless infrastructure that costs nearly nothing when idle. It's not tied to your laptop — talk to it from Telegram while it works on a cloud VM.

Use any model you want — [NasTech Portal](https://portal.nastech.com), [OpenRouter](https://openrouter.ai) (200+ models), [NovitaAI](https://novita.ai) (AI-native cloud for Model API, Agent Sandbox, and GPU Cloud), [NVIDIA NIM](https://build.nvidia.com) (Nemotron), [Xiaomi MiMo](https://platform.xiaomimimo.com), [z.ai/GLM](https://z.ai), [Kimi/Moonshot](https://platform.moonshot.ai), [MiniMax](https://www.minimax.io), [Hugging Face](https://huggingface.co), OpenAI, or your own endpoint. Switch with `nastech model` — no code changes, no lock-in.

<table>
<tr><td><b>A real terminal interface</b></td><td>Full TUI with multiline editing, slash-command autocomplete, conversation history, interrupt-and-redirect, and streaming tool output.</td></tr>
<tr><td><b>Lives where you do</b></td><td>Telegram, Discord, Slack, WhatsApp, Signal, and CLI — all from a single gateway process. Voice memo transcription, cross-platform conversation continuity.</td></tr>
<tr><td><b>A closed learning loop</b></td><td>Agent-curated memory with periodic nudges. Autonomous skill creation after complex tasks. Skills self-improve during use. FTS5 session search with LLM summarization for cross-session recall. <a href="https://github.com/plastic-labs/honcho">Honcho</a> dialectic user modeling. Compatible with the <a href="https://agentskills.io">agentskills.io</a> open standard.</td></tr>
<tr><td><b>Scheduled automations</b></td><td>Built-in cron scheduler with delivery to any platform. Daily reports, nightly backups, weekly audits — all in natural language, running unattended.</td></tr>
<tr><td><b>Delegates and parallelizes</b></td><td>Spawn isolated subagents for parallel workstreams. Write Python scripts that call tools via RPC, collapsing multi-step pipelines into zero-context-cost turns.</td></tr>
<tr><td><b>Runs anywhere, not just your laptop</b></td><td>Six terminal backends — local, Docker, SSH, Singularity, Modal, and Daytona. Daytona and Modal offer serverless persistence — your agent's environment hibernates when idle and wakes on demand, costing nearly nothing between sessions. Run it on a $5 VPS or a GPU cluster.</td></tr>
<tr><td><b>Research-ready</b></td><td>Batch trajectory generation, trajectory compression for training the next generation of tool-calling models.</td></tr>
</table>

---

## Quick Install

### Linux, macOS, WSL2, NasUX

```bash
curl -fsSL https://raw.githubusercontent.com/nastech-ai/NasTech-Agent/main/scripts/install.sh | bash
```

### Windows (native, PowerShell) — Early Beta

> **Heads up:** Native Windows support is **early beta**. It installs and runs, but hasn't been road-tested as broadly as our Linux/macOS/WSL2 paths. Please [file issues](https://github.com/nastech-ai/NasTech-Agent/issues) when you hit rough edges. For the most battle-tested Windows setup today, run the Linux/macOS one-liner above inside **WSL2**.

Run this in PowerShell:

```powershell
iex (irm https://raw.githubusercontent.com/nastech-ai/NasTech-Agent/main/scripts/install.ps1)
```

The installer handles everything: uv, Python 3.11, Node.js, ripgrep, ffmpeg, **and a portable Git Bash** (MinGit, unpacked to `%LOCALAPPDATA%\nastech\git` — no admin required, completely isolated from any system Git install).  NasTech uses this bundled Git Bash to run shell commands.

If you already have Git installed, the installer detects it and uses that instead.  Otherwise a ~45MB MinGit download is all you need — it won't touch or interfere with any system Git.

> **Android / NasUX:** The tested manual path is documented in the [NasUX guide](https://docs.nastech-agent.workers.dev/docs/getting-started/nasux). On NasUX, NasTech installs a curated `.[nasux]` extra because the full `.[all]` extra currently pulls Android-incompatible voice dependencies.
>
> **Windows:** Native Windows is supported as an **early beta** — the PowerShell one-liner above installs everything, but expect rough edges and please file issues when you hit them. If you'd rather use WSL2 (our most battle-tested Windows path), the Linux command works there too. Native Windows install lives under `%LOCALAPPDATA%\nastech`; WSL2 installs under `~/.nastech` as on Linux.  The only NasTech feature that currently needs WSL2 specifically is the browser-based dashboard chat pane (it uses a POSIX PTY — classic CLI and gateway both run natively).

After installation:

```bash
source ~/.bashrc    # reload shell (or: source ~/.zshrc)
nastech              # start chatting!
```

---

## Getting Started

```bash
nastech              # Interactive CLI — start a conversation
nastech model        # Choose your LLM provider and model
nastech tools        # Configure which tools are enabled
nastech config set   # Set individual config values
nastech gateway      # Start the messaging gateway (Telegram, Discord, etc.)
nastech setup        # Run the full setup wizard (configures everything at once)
nastech claw migrate # Migrate from OpenClaw (if coming from OpenClaw)
nastech update       # Update to the latest version
nastech doctor       # Diagnose any issues
```

📖 **[Full documentation →](https://docs.nastech-agent.workers.dev/docs/)**

---

## Skip the API-key collection — NasTech Portal

NasTech works with whatever provider you want — that's not changing. But if you'd rather not collect five separate API keys for the model, web search, image generation, TTS, and a cloud browser, **[NasTech Portal](https://portal.nastech.com)** covers all of them under one subscription:

- **300+ models** — pick any of them with `/model <name>`
- **Tool Gateway** — web search (Firecrawl), image generation (FAL), text-to-speech (OpenAI), cloud browser (Browser Use), all routed through your sub. No extra accounts.

One command from a fresh install:

```bash
nastech setup --portal
```

That logs you in via OAuth, sets Nous as your provider, and turns on the Tool Gateway. Check what's wired up any time with `nastech portal status`. Full details on the [Tool Gateway docs page](https://docs.nastech-agent.workers.dev/docs/user-guide/features/tool-gateway).

You can still bring your own keys per-tool whenever you want — the gateway is per-backend, not all-or-nothing.

---

## CLI vs Messaging Quick Reference

NasTech has two entry points: start the terminal UI with `nastech`, or run the gateway and talk to it from Telegram, Discord, Slack, WhatsApp, Signal, or Email. Once you're in a conversation, many slash commands are shared across both interfaces.

| Action | CLI | Messaging platforms |
|---------|-----|---------------------|
| Start chatting | `nastech` | Run `nastech gateway setup` + `nastech gateway start`, then send the bot a message |
| Start fresh conversation | `/new` or `/reset` | `/new` or `/reset` |
| Change model | `/model [provider:model]` | `/model [provider:model]` |
| Set a personality | `/personality [name]` | `/personality [name]` |
| Retry or undo the last turn | `/retry`, `/undo` | `/retry`, `/undo` |
| Compress context / check usage | `/compress`, `/usage`, `/insights [--days N]` | `/compress`, `/usage`, `/insights [days]` |
| Browse skills | `/skills` or `/<skill-name>` | `/<skill-name>` |
| Interrupt current work | `Ctrl+C` or send a new message | `/stop` or send a new message |
| Platform-specific status | `/platforms` | `/status`, `/sethome` |

For the full command lists, see the [CLI guide](https://docs.nastech-agent.workers.dev/docs/user-guide/cli) and the [Messaging Gateway guide](https://docs.nastech-agent.workers.dev/docs/user-guide/messaging).

---

## Documentation

All documentation lives at **[nastech-agent.workers.dev/docs](https://docs.nastech-agent.workers.dev/docs/)**:

| Section | What's Covered |
|---------|---------------|
| [Quickstart](https://docs.nastech-agent.workers.dev/docs/getting-started/quickstart) | Install → setup → first conversation in 2 minutes |
| [CLI Usage](https://docs.nastech-agent.workers.dev/docs/user-guide/cli) | Commands, keybindings, personalities, sessions |
| [Configuration](https://docs.nastech-agent.workers.dev/docs/user-guide/configuration) | Config file, providers, models, all options |
| [Messaging Gateway](https://docs.nastech-agent.workers.dev/docs/user-guide/messaging) | Telegram, Discord, Slack, WhatsApp, Signal, Home Assistant |
| [Security](https://docs.nastech-agent.workers.dev/docs/user-guide/security) | Command approval, DM pairing, container isolation |
| [Tools & Toolsets](https://docs.nastech-agent.workers.dev/docs/user-guide/features/tools) | 40+ tools, toolset system, terminal backends |
| [Skills System](https://docs.nastech-agent.workers.dev/docs/user-guide/features/skills) | Procedural memory, Skills Hub, creating skills |
| [Memory](https://docs.nastech-agent.workers.dev/docs/user-guide/features/memory) | Persistent memory, user profiles, best practices |
| [MCP Integration](https://docs.nastech-agent.workers.dev/docs/user-guide/features/mcp) | Connect any MCP server for extended capabilities |
| [Cron Scheduling](https://docs.nastech-agent.workers.dev/docs/user-guide/features/cron) | Scheduled tasks with platform delivery |
| [Context Files](https://docs.nastech-agent.workers.dev/docs/user-guide/features/context-files) | Project context that shapes every conversation |
| [Architecture](https://docs.nastech-agent.workers.dev/docs/developer-guide/architecture) | Project structure, agent loop, key classes |
| [Contributing](https://docs.nastech-agent.workers.dev/docs/developer-guide/contributing) | Development setup, PR process, code style |
| [CLI Reference](https://docs.nastech-agent.workers.dev/docs/reference/cli-commands) | All commands and flags |
| [Environment Variables](https://docs.nastech-agent.workers.dev/docs/reference/environment-variables) | Complete env var reference |

---

## Migrating from OpenClaw

If you're coming from OpenClaw, NasTech can automatically import your settings, memories, skills, and API keys.

**During first-time setup:** The setup wizard (`nastech setup`) automatically detects `~/.openclaw` and offers to migrate before configuration begins.

**Anytime after install:**

```bash
nastech claw migrate              # Interactive migration (full preset)
nastech claw migrate --dry-run    # Preview what would be migrated
nastech claw migrate --preset user-data   # Migrate without secrets
nastech claw migrate --overwrite  # Overwrite existing conflicts
```

What gets imported:
- **SOUL.md** — persona file
- **Memories** — MEMORY.md and USER.md entries
- **Skills** — user-created skills → `~/.nastech/skills/openclaw-imports/`
- **Command allowlist** — approval patterns
- **Messaging settings** — platform configs, allowed users, working directory
- **API keys** — allowlisted secrets (Telegram, OpenRouter, OpenAI, Anthropic, ElevenLabs)
- **TTS assets** — workspace audio files
- **Workspace instructions** — AGENTS.md (with `--workspace-target`)

See `nastech claw migrate --help` for all options, or use the `openclaw-migration` skill for an interactive agent-guided migration with dry-run previews.

---

## Contributing

We welcome contributions! See the [Contributing Guide](https://docs.nastech-agent.workers.dev/docs/developer-guide/contributing) for development setup, code style, and PR process.

Quick start for contributors — clone and go with `setup-nastech.sh`:

```bash
git clone https://github.com/nastech-ai/NasTech-Agent.git
cd nastech-agent
./setup-nastech.sh     # installs uv, creates venv, installs .[all], symlinks ~/.local/bin/nastech
./nastech              # auto-detects the venv, no need to `source` first
```

Manual path (equivalent to the above):

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
uv venv .venv --python 3.11
source .venv/bin/activate
uv pip install -e ".[all,dev]"
scripts/run_tests.sh
```

---

## Community

- 💬 [Discord](https://discord.gg/NasTech)
- 📚 [Skills Hub](https://agentskills.io)
- 🐛 [Issues](https://github.com/nastech-ai/NasTech-Agent/issues)
- 🔌 [computer-use-linux](https://github.com/avifenesh/computer-use-linux) — Linux desktop-control MCP server for NasTech and other MCP hosts, with AT-SPI accessibility trees, Wayland/X11 input, screenshots, and compositor window targeting.
- 🔌 [NasTechClaw](https://github.com/AaronWong1999/nastechclaw) — Community WeChat bridge: Run NasTech Agent and OpenClaw on the same WeChat account.

---

## License

MIT — see [LICENSE](LICENSE).

Built by [NasTech](https://nastech.com).
