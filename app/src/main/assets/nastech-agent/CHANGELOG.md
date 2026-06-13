# NasTech Agent — Changelog

All notable changes to **NasTech Agent** are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions follow [Calendar Versioning](https://calver.org/) (`YYYY.M.D`) aligned with the underlying release cycle, wrapped in semantic version numbers (`0.x.y`) for PyPI / npm compatibility.

NasTech Agent is an independent, fully rebranded fork. The upstream codebase is used as a base; all NasTech-specific changes, branding, and packaging decisions are tracked here separately.

---

## [Unreleased]

### NasTech-Specific
- Branding enforcer (`skills/branding_enforcer.py`) as single source of truth for CI brand rules
- `AGENTS.md` mandatory rules section for AI agents and contributors
- `scripts/branding_lint.py` self-skip extended to cover `branding_enforcer.py` and `AGENTS.md`
- `.github/workflows/changelog-release.yml` — auto-appends a CHANGELOG entry on every version tag push

### Security
- Bumped `aiohttp` 3.13.4 → 3.14.0 across all extras (messaging, slack, homeassistant, sms) — CVE-2026-34513/34518/34519/34520/34525
- Bumped `pytest` 9.0.2 → 9.0.3 in dev extra
- Added `overrides` to `web/package.json`: postcss ≥8.5.10, undici ≥7.24.0, ws ≥8.20.1, shell-quote ≥1.8.4 (CRITICAL), brace-expansion ≥1.1.13, picomatch ≥2.3.2, qs ≥6.15.2, uuid ≥11.1.1, path-to-regexp ≥0.1.13
- Added `overrides` to `website/package.json`: prismjs ≥1.30.0 (multiple HIGH CVEs), postcss ≥8.5.10, ws ≥8.20.1, brace-expansion ≥1.1.13, picomatch ≥2.3.2, shell-quote ≥1.8.4
- Extended root `package.json` overrides: shell-quote, brace-expansion, picomatch, undici, ws, postcss, qs

### Repo Hygiene
- Removed `.agents/` (Replit agent memory — IDE-specific), `.replit` (Replit workspace config), and `dist/` (Python wheel/tarball — published to PyPI) from GitHub via Git Data API commit — 87 files untracked
- Added `dist/`, `build/`, `*.egg-info/` to `.gitignore`

---

## [0.17.0] — 2026-06-13

### NasTech-Specific
- **Skills Hub expansion** — Bulk-installed 120 skills from four public taps: `openai/skills` (44 skills: .curated + .system), `anthropics/skills` (17 skills), `huggingface/skills` (19 skills), and community taps `chujianyun/skills` + `shiwenwen/hope-agent` (42 skills). New categories: `skills/openai/`, `skills/anthropic/`, `skills/huggingface/`, `skills/community/`, `skills/nvidia/`. Total bundled skills: 93 → 212.
- **Env-var rebrand** — `HERMES_DESKTOP` → `NASTECH_DESKTOP`; `HERMES_SESSION_*` → `NASTECH_SESSION_*` across all affected files
- **piper-tts lazy dependency** — `tts_tool` piper dispatch now lazy-imports `piper` on first use instead of at module load; eliminates import error on systems without piper installed
- **8 missing upstream files backfilled** — `cron/scripts/__init__.py`, `cron/scripts/classify_items.py`, `agent/coding_context.py`, `nastech_cli/model_cost_guard.py`, `nastech_cli/blueprint_cmd.py`, `nastech_cli/suggestions_cmd.py`, `nastech_cli/setup_whatsapp_cloud.py`, `tools/read_terminal_tool.py` fetched from Hermes upstream and rebranded
- **Version bump** — `nastech_cli/__init__.py` and `pyproject.toml` set to `0.17.0`

### CI / Workflows
- **4 workflow files rebranded** — replaced stale `hermes`/`Hermes`/`nous-research` strings in `.github/workflows/`; `_SELF_SKIP_FILES` updated in `scripts/branding_lint.py` for `verification-stage-1.yml`
- **PLW1514 encoding fix** — `nastech_constants.py` line 401 `open()` call given explicit `encoding='utf-8'` (ruff PLW1514)
- **Contributor email** — `bantuinvasions@gmail.com` added to `AUTHOR_MAP` in contributor tooling

### Bug Fixes
- Docker build: added `package-lock.json` stub so `npm ci` in `Dockerfile` does not fail on first clone
- `scripts/branding_lint.py` self-skip list extended to cover `verification-stage-1.yml` and `branding_enforcer.py`

---

## [0.16.1] — 2026-06-09

### NasTech-Specific Fixes
- Removed `scripts/nastech_audit.py` from repo (large audit tool — download and run locally instead)
- Deleted `audit_report.json`, `audit_report.md`, `hermes_tree_cache.json` generated artifacts; added all to `.gitignore`
- Removed `package-lock.json` and `web/package-lock.json` from version control — both contained Replit-specific `package-firewall.replit.local` registry URLs that break installs on other machines; added to `.gitignore`
- Fixed `.github/workflows/upload_to_pypi.yml`: changed `npm ci` to `npm install` for `web/` build step (no committed lockfile)
- Fixed `apps/desktop/electron/main.cjs` — 4× stale `Nous Research` copyright / OAuth comment strings replaced with `NasTech`
- Fixed `apps/desktop/scripts/set-exe-identity.cjs` — `CompanyName` and `LegalCopyright` updated to `NasTech`
- Fixed duplicate function definition in `tests/nastech_cli/test_dashboard_browser_safe_imports.py`; split into two properly-named distinct tests
- Created `skills/branding_enforcer.py` — the canonical Hermes→NasTech replacement table and skip-list, loaded by `scripts/branding_lint.py` via AST
- Fixed `.gitignore` stale `@nous-research` comment; added all lock files and generated artifacts to ignore list
- Branding linter: `✅ 0 violations` across entire codebase

### Verified Installation Status
- `uv sync` — ✅ 58 packages resolved and installed
- `pip install -e ".[cli]"` — ✅ editable install succeeds
- `pip install nastech-agent==0.16.0` — ✅ installs from PyPI
- `npm install` (root workspace) — ✅
- `npm install` (`web/`) — ✅ 365 packages, 0 vulnerabilities
- `npm install` (`ui-tui/`) — ✅ 365 packages, 0 vulnerabilities
- `python3 nastech --help` — ✅ CLI entry point works (50+ subcommands)
- `bash setup-nastech.sh` — ✅ uv 0.9.5 + Python 3.11.14 detected, venv created
- `bash install.sh` — ✅ unified installer syntax verified
- `nastech_cli/web_dist/` — ✅ production web build present

---

## [0.16.0] — 2026-06-06 — *The Surface Release*

**Based on upstream v2026.6.5** · 874 commits · 542 PRs · 1,962 files changed · 399 issues closed · 170 contributors

### Highlights
- **Native Desktop App** — NasTech as a real macOS / Linux / Windows application. One-click install, in-app self-update, drag-and-drop files into chat, inline model picker in status bar, concurrent multi-profile sessions, full Simplified Chinese translation, remote gateway connect via OAuth or username/password. Built across 100 PRs and 159 commits.
- **Web Dashboard Admin Panel** — Full browser-based administration: MCP catalog, messaging channels, credentials, webhooks, memory management, pluggable OIDC / username-password login.
- **Quick Setup via NasTech Portal** — Gets you from install to first message in seconds.
- **Skills Hub** — NVIDIA/skills added as a trusted tap. Default skill set trimmed to what you actually use.
- **Model Picker** — Redesigned inline model picker in status bar (desktop + dashboard).
- **OAuth Remote Gateway Auth** — Hosted gateways can gate the dashboard behind an OAuth provider with full session-cookie auth.
- **Multi-language i18n** — Simplified Chinese translation ships by default; locale framework expanded.

### NasTech-Specific Changes for 0.16.0
- Full rebrand: all package names, module paths, CLI entry points, Docker user, env vars, copyright strings changed from upstream to NasTech equivalents
- Published `nastech-agent==0.16.0` to PyPI
- Published `@nastechai/ui@0.19.1`, `@nastechai/agent@0.16.0`, `@nastechai/ink@0.0.2` to npm
- AMOLED dark theme set as default
- `NASTECH_API_KEY` replaces upstream portal key env var
- `Dockerfile` runtime user changed to `nastech`
- `install.sh` and `setup-nastech.sh` — NasTech-branded unified installers
- All GitHub Actions workflows repointed to `nastech-ai/NasTech-Agent`

---

## [0.15.2] — 2026-05-29 — *Patch Release*

**Based on upstream v2026.5.29.2**

### Fixes
- Packaging: ship bundled `plugin.yaml` manifests in wheel and sdist (previously missing from built distributions)

---

## [0.15.1] — 2026-05-29 — *The Patch Release*

**Based on upstream v2026.5.29** · 28 commits · 21 PRs · 9 contributors

### Fixes
- **Dashboard infinite-reload loop fixed** — In loopback mode the dashboard's identity probe (`/api/auth/me`) returns 401 by design; v0.15.0 treated every 401 as a rotated session token and full-page-reloaded. Fixed.
- Kanban worker SIGTERM handling
- `/model` picker unification across CLI and dashboard
- `/yolo` session bypass restored
- Full 19,932-entry skills catalog shipped
- `.md` media delivery restoration
- Gateway probe-stepdown safety
- Web-URL redaction passthrough
- Kanban worker vision on referenced images
- Hindsight observation-default
- Docker users get explicit `--insecure` opt-in env var (no more bind-host inference)
- MCP server bare-command PATH resolution
- arm64 PR-build cache fixes

---

## [0.15.0] — 2026-05-28 — *The Velocity Release*

**Based on upstream v2026.5.28** · 1,302 commits · 747 PRs · 1,746 files changed · 560+ issues closed · 321 contributors

### Highlights
- **Agent core refactor** — `run_agent.py` (16,083 lines) collapses to 3,821 lines (−76%) split across 14 cohesive `agent/*` modules with no behavior change
- **Kanban multi-agent platform** — 104 PRs: orchestrator auto-decomposition, swarm topology, scheduled tasks, worktree-per-task, per-task model overrides, heartbeat / reclaim / zombie detection
- **Cold-start performance** — Additional second shaved off launch; 47% fewer per-conversation function calls; `nastech --version` competitive with Codex CLI head-to-head
- **Session search** — 4,500× faster via FTS5; now free (no API call)
- **Promptware defense** — Defense against Brainworm-class prompt injection attacks
- **Bitwarden Secrets Manager** — Replaces per-provider API keys with one bootstrap token
- **Skill bundles** — One slash command loads an entire workflow bundle
- **Ink TUI multi-session orchestrator** — Manage multiple agent sessions from one TUI
- **Image generation** — Krea 2 Medium + Large added as providers

---

## [0.14.0] — 2026-05-16 — *The Foundation Release*

**Based on upstream v2026.5.16** · 808 commits · 633 PRs · 1,393 files changed · 545 issues closed · 215 contributors

### Highlights
- **xAI Grok + SuperGrok OAuth** — Grok-4.3 with 1M context window; SuperGrok as an OAuth provider
- **OpenAI-compatible local proxy** — Turn any OAuth-authed provider (Claude Pro, ChatGPT Pro, SuperGrok) into an endpoint for Codex / Aider / Cline / Continue
- **X (Twitter) search** — `x_search` tool with OAuth-or-API-key auth
- **Microsoft Teams** — Full end-to-end stack: Graph auth, webhook listener, pipeline runtime, outbound delivery
- **Debloating** — Heavyweight backends now lazy-install on first use; `[all]` extras drop everything covered by lazy-deps; tiered install falls back when a wheel rejects on your platform
- **`pip install nastech-agent` works from PyPI** — First release fully installable from the public registry
- **Cold-start** — ~19 seconds shaved off `nastech` launch time

---

## [0.13.0] — 2026-05-07 — *The Tenacity Release*

**Based on upstream v2026.5.7** · 864 commits · 588 PRs · 829 files changed · 282 issues closed · 295 contributors

### Highlights
- **Durable multi-agent Kanban** — Heartbeat, reclaim, zombie detection, auto-block on incomplete exit, per-task retries, hallucination recovery
- **`/goal` — Ralph loop** — Keeps the agent locked on a target across turns until complete
- **Checkpoints v2** — Rewrites state persistence with real pruning; no more unbounded checkpoint growth
- **Gateway auto-resume** — Interrupted sessions resume automatically after gateway restart
- **Cron `no_agent` watchdog** — Runs cron jobs without spawning a full agent loop
- **Security wave** — 8 P0 fixes: redaction ON by default, Discord role-allowlists are guild-scoped, WhatsApp rejects strangers by default, TOCTOU windows closed across auth.json and MCP OAuth
- **Google Chat** — 20th supported messaging platform
- **Pluggable providers** — Inference backends become an extensible plugin surface
- **7 i18n locales** — Internationalization framework ships with 7 locales

---

## [0.12.0] — 2026-04-30 — *The Curator Release*

**Based on upstream v2026.4.30** · 1,096 commits · 550 PRs · 1,270 files changed · 213 contributors

### Highlights
- **Autonomous Curator** — Background agent grades, prunes, and consolidates your skill library on its own schedule. `nastech curator` runs on the gateway's cron ticker (7-day default). Writes per-run reports to `logs/curator/run.json` + `REPORT.md`.
- **Self-improvement loop** — Substantial upgrade to the review of what to save
- **4 new inference providers**
- **18th messaging platform** (native); 19th via Teams plugin
- **Spotify + Google Meet** native integrations
- **ComfyUI and TouchDesigner-MCP** moved from optional to bundled-by-default
- **TUI cold start** — ~57% cut to visible startup time in the Ink TUI

---

## [0.11.0] — 2026-04-23 — *The Interface Release*

**Based on upstream v2026.4.23** · 1,556 commits · 761 PRs · 1,314 files changed · 290 contributors

### Highlights
- **New Ink-based TUI** — `nastech --tui` is a full React/Ink rewrite of the interactive CLI with a Python JSON-RPC backend (`tui_gateway`). Sticky composer, live streaming with OSC-52 clipboard, stable picker keys, status bar with per-turn stopwatch and git branch, `/clear` confirm, light-theme preset, subagent observability overlay. ~310 commits.
- **Transport ABC + Native AWS Bedrock** — Pluggable transport architecture underneath every provider; native Bedrock support
- **5 new inference paths**
- **QQBot** — 17th messaging platform
- **Dramatically expanded plugin surface**
- **GPT-5.5 via Codex OAuth**

---

## [0.10.0] — 2026-04-16 — *The Tool Gateway Release*

**Based on upstream v2026.4.16**

### Highlights
- **NasTech Tool Gateway** — Portal subscribers get automatic access to web search (Firecrawl), image generation (FAL / FLUX 2 Pro), text-to-speech (OpenAI TTS), and browser automation (Browser Use) — no separate API keys needed. Per-tool opt-in via `use_gateway` config.

---

## [0.9.0] — 2026-04-13 — *The Everywhere Release*

**Based on upstream v2026.4.13** · 487 commits · 269 PRs · 493 files changed · 167 issues resolved · 24 contributors

### Highlights
- **Termux / Android** — Full mobile support; NasTech runs on Android via Termux
- **iMessage + WeChat** — Two new messaging platforms
- **Fast Mode (`/fast`)** — Priority processing for OpenAI and Anthropic models; lower latency on GPT-5.4, Codex, Claude
- **Background process monitoring** — Monitor long-running processes; agent gets notified on completion
- **Local Web Dashboard** — Browser-based dashboard for managing sessions, skills, gateway, and config
- **Security hardening** — 16 platforms hardened across a deep security pass
- **16 supported messaging platforms**

---

## [0.8.0] — 2026-04-08 — *The Intelligence Release*

**Based on upstream v2026.4.8** · 209 PRs · 82 issues resolved

### Highlights
- **Background task auto-notifications (`notify_on_complete`)** — Start a long-running process; agent is notified when it finishes
- **Live model switching** — Switch models mid-conversation across all platforms
- **Smart inactivity timeouts** — Auto-timeout idle sessions; configurable per platform
- **Approval buttons** — Interactive approval/deny flow in messaging platforms
- **MCP OAuth 2.1** — Full OAuth 2.1 support for MCP server connections
- **Native Google AI Studio** — Direct Google AI Studio inference provider

---

## [0.7.0] — 2026-04-03 — *The Resilience Release*

**Based on upstream v2026.4.3** · 168 PRs · 46 issues resolved

### Highlights
- **Pluggable Memory Provider Interface** — Memory is now an extensible plugin system; Honcho, vector stores, custom DBs implement a simple provider ABC
- **Same-Provider Credential Pools** — Multiple API keys per provider with automatic `least_used` rotation; 401 failures trigger automatic failover
- **Camofox anti-detection browser** — Browser Use backend that evades bot detection
- **Inline diff previews** — File diffs shown inline in chat before applying
- **Gateway hardening** — Race conditions and approval routing edge cases fixed across all platforms

---

## [0.6.0] — 2026-03-30 — *The Multi-Instance Release*

**Based on upstream v2026.3.30** · 95 PRs · 16 issues resolved

### Highlights
- **Profiles — multi-instance NasTech** — Run multiple isolated instances from one install. Each profile gets its own config, memory, sessions, skills, and gateway. Create with `nastech profile create`, switch with `nastech -p <name>`.
- **MCP Server Mode** — Expose NasTech conversations to any MCP-compatible client (Claude Desktop, Cursor, VS Code) via `nastech mcp serve`
- **Docker container** — Official Docker image with s6-overlay supervision
- **Fallback provider chains** — Chain multiple providers; automatic failover on error
- **Feishu/Lark + WeCom** — Two new messaging platforms (14th and 15th)
- **Telegram webhook mode** — Webhook transport instead of polling for production deployments
- **Slack multi-workspace OAuth** — Connect NasTech to multiple Slack workspaces from one instance

---

## [0.5.0] — 2026-03-28 — *The Hardening Release*

**Based on upstream v2026.3.28** · 50+ security and reliability fixes

### Highlights
- **400+ models** — NasTech Portal expanded to 400+ models through a single provider endpoint
- **Hugging Face provider** — Full HF Inference API integration with curated model picker
- **Telegram Private Chat Topics** — Project-based conversations with skill binding per topic
- **Native Modal SDK backend** — Modal cloud execution environment, natively integrated
- **Tool-use enforcement for GPT models** — Reliability improvements for GPT function calling
- **Nix flake** — Official Nix packaging
- **Comprehensive supply chain audit** — All GitHub Actions pinned to commit SHAs

---

## [0.4.0] — 2026-03-23 — *The Platform Expansion Release*

**Based on upstream v2026.3.23** · 200+ bug fixes

### Highlights
- **OpenAI-compatible API server** — Expose NasTech as a `/v1/chat/completions` endpoint; `/api/jobs` REST API for cron job management
- **6 new messaging platform adapters** — Signal, DingTalk, SMS (Twilio), Mattermost, Matrix, Webhook join the gateway (9 platforms total)
- **`@` context references** — Reference files, URLs, and sessions inline in chat with `@filename`
- **Gateway prompt caching** — Persistent prompt cache across gateway sessions for faster responses
- **Streaming enabled by default** — Token-by-token delivery on all providers
- **4 new inference providers**
- **MCP server management with OAuth 2.1**

---

## [0.3.0] — 2026-03-17 — *The Streaming & Plugins Release*

**Based on upstream v2026.3.17** · 50+ bug fixes

### Highlights
- **Unified Streaming Infrastructure** — Real-time token-by-token delivery in CLI and all gateway platforms
- **First-Class Plugin Architecture** — Drop Python files into `~/.nastech/plugins/` to extend NasTech with custom tools, commands, and hooks — no forking required
- **Native Anthropic Provider** — Direct Anthropic API with Claude Code credential auto-discovery, OAuth PKCE, and native prompt caching
- **ACP IDE Integration** — VS Code, Zed, JetBrains integration via Agent Communication Protocol
- **Honcho Memory** — Honcho memory provider integration
- **Voice Mode** — Speech-to-text input and text-to-speech output
- **Persistent Shell** — Shell sessions persist across turns; state survives between tool calls
- **Live Chrome CDP browser connect** — Attach to a running Chrome instance via CDP

---

## [0.2.0] — 2026-03-12 — *The First Community Release*

**Based on upstream v2026.3.12** · 216 PRs · 63 contributors · 119 issues resolved

### Highlights
- **Multi-Platform Messaging Gateway** — Telegram, Discord, Slack, WhatsApp, Signal, Email (IMAP/SMTP), and Home Assistant with unified session management, media attachments, and per-platform tool config
- **MCP (Model Context Protocol) Client** — Native MCP support with stdio and HTTP transports, reconnection, resource/prompt discovery, and sampling
- **Skills Ecosystem** — 70+ bundled and optional skills across 15+ categories with Skills Hub for community discovery
- **Computer Use** — Desktop automation via screenshot + mouse/keyboard control
- **Cron Scheduler** — Time-based and interval-based job scheduling with full agent context
- **Session Persistence** — SQLite-backed sessions with FTS5 full-text search across all history
- **Multi-turn Memory** — Persistent memory across conversations with smart summarization
- **15+ inference providers** — OpenAI, Anthropic, OpenRouter, Gemini, Mistral, Groq, Together, and more

---

## [0.1.0] — 2026-02-24 — *Initial Foundation*

Internal pre-public foundation release. Core agent loop, tool registry, basic CLI, and initial provider integrations.

---

## Installation

```bash
# PyPI (recommended)
pip install nastech-agent

# With extras
pip install "nastech-agent[cli]"        # full CLI experience
pip install "nastech-agent[messaging]"  # all messaging platform deps
pip install "nastech-agent[all]"        # everything

# From source
git clone https://github.com/nastech-ai/NasTech-Agent.git
cd NasTech-Agent
./setup-nastech.sh      # sets up venv + installs deps with uv
# or
./install.sh            # unified one-step installer

# Docker
docker pull ghcr.io/nastech-ai/nastech-agent:latest
docker-compose up
```

## npm Packages

| Package | Version | Registry |
|---|---|---|
| `@nastechai/ui` | `0.19.1` | [npm](https://www.npmjs.com/package/@nastechai/ui) |
| `@nastechai/agent` | `0.16.0` | [npm](https://www.npmjs.com/package/@nastechai/agent) |
| `@nastechai/ink` | `0.0.2` | [npm](https://www.npmjs.com/package/@nastechai/ink) |

---

*NasTech Agent is maintained by [NasTech](https://nastechai.com). Repository: [nastech-ai/NasTech-Agent](https://github.com/nastech-ai/NasTech-Agent).*
