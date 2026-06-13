# NasTech Agent — Integration Audit & Roadmap

**Date:** June 3, 2026  
**Scope:** Mobile app (NasTech Expo fork of slopus/happy) ↔ NasTech FastAPI backend wiring

---

## What Was Done This Session

### 1. Firebase Config Files — Replaced & Added
| File | Before | After |
|------|--------|-------|
| `NasTech/google-services.json` | Old Happy project (`happy-coder-9fe36`, package `com.nastech.app`) | NasTech Firebase project (`nastechtestn`, packages `ai.nastech.ba`, `ai.nastech.ba.dev`, `ai.nastech.ba.preview`) |
| `NasTech/GoogleService-Info.plist` | **Did not exist** | Created — iOS Firebase config for `ai.nastech.ba` (GOOGLE_APP_ID `1:797592559366:ios:6184c764da8d0ab9810887`) |

### 2. Bundle IDs — Updated in `app.config.js`
| Variant | Old | New |
|---------|-----|-----|
| production | `com.nastech.app` | `ai.nastech.ba` |
| preview | `com.nastech.app.preview` | `ai.nastech.ba.preview` |
| development | `com.nastech.app.dev` | `ai.nastech.ba.dev` |

Also added `googleServicesFile: "./GoogleService-Info.plist"` to the iOS config block.

### 3. tokenStorage.ts — Stub Credentials Fixed
**Root cause (critical blocker):** `TokenStorage.getCredentials()` and `getCredentialsSync()` always returned `null`. In `sync.ts`, `fetchSessions` has an early-return guard `if (!this.credentials) return;` — meaning **sessions never loaded at all**.

**Fix:** Both methods now return `{ token: 'nastech-local', secret: 'nastech-local' }`.

### 4. sync.ts — All Backend Wiring Mismatches Fixed

#### 4a. `fetchSessions` — Endpoint + Format
- **Before:** `GET /v1/sessions` (Happy API, encrypted session objects)
- **After:** `GET /api/sessions` (NasTech API, plain JSON)
- Removed all encryption decryption logic
- Maps NasTech fields (`session_id`, `title`, `started_at`, `last_active`, `is_active`, `preview`) → app `Session` type
- Initializes sessions with null encryption keys (NasTech is unencrypted)

#### 4b. `fetchMachines` — Stubbed to Empty
- **Before:** `GET /v1/machines` (Happy API — NasTech has no machines)
- **After:** Immediately calls `applyMachines([], true)` and returns — no network call made

#### 4c. `syncSettings` — Local-Only
- **Before:** `POST /v1/account/settings` + `GET /v1/account/settings` (Happy API)
- **After:** Flushes pending settings into local store and returns — no network call

#### 4d. `fetchProfile` — No-Op
- **Before:** `GET /v1/account/profile` (Happy API — NasTech has no profile endpoint)
- **After:** Empty function — returns immediately

#### 4e. `fetchNativeUpdate` — Endpoint Fixed
- **Before:** `POST /v1/version` with platform/version/app_id payload
- **After:** `GET /api/status` (NasTech status endpoint) — always returns `{ available: false }` (NasTech pushes no OTA update URLs)

#### 4f. `fetchInitialLatestPage` — Endpoint + Encryption Removed
- **Before:** `apiSocket.request('/v3/sessions/${id}/messages?before_seq=...&limit=100')` via socket.io + `applyFetchedMessages` (decrypts)
- **After:** `GET /api/sessions/${id}/messages` via plain `fetch()` — maps NasTech messages through `mapNasTechRawMsg()`, sets `hasMoreOlder: false`

#### 4g. `fetchForwardSince` — Endpoint + Encryption Removed
- **Before:** `apiSocket.request('/v3/sessions/${id}/messages?after_seq=...&limit=100')` in a loop
- **After:** `GET /api/sessions/${id}/messages`, slices to messages newer than `fromSeq` by index

#### 4h. `loadOlderMessages` — Dead Code for NasTech
- **Before:** Called `apiSocket.request('/v3/sessions/${id}/messages?before_seq=...')` (would 404)
- **After:** Returns `{ hasMore: false }` immediately — NasTech loads all messages at once so this path is never reached anyway

#### 4i. `subscribeToUpdates` — socket.io Replaced with NasTech WebSocket
- **Before:** `apiSocket.onMessage('update', ...)` via socket.io to `/v1/updates` (no socket.io on NasTech)
- **After:** Native `WebSocket` connecting to `/api/ws` (NasTech JSON-RPC WebSocket) — invalidates sessions on any message. 10-second polling interval as fallback. Auto-reconnects on close.

#### 4j. `mapNasTechRawMsg` — New Helper Method
Added private method to the `Sync` class that converts NasTech DB message rows to `NormalizedMessage`:
- `role: 'user'` → `{ role: 'user', content: { type: 'text', text } }`
- `role: 'assistant' | 'tool' | 'system'` → `{ role: 'agent', content: [{ type: 'text', text, uuid, parentUUID: null }] }`
- `content` can be string or JSON object; non-strings are serialized via `JSON.stringify`

---

## What Was Found (Audit)

### Backend Endpoints Comparison

| Mobile App Calls | NasTech Has | Status |
|-----------------|-------------|--------|
| `GET /v1/sessions` | `GET /api/sessions` | ✅ Fixed |
| `GET /v1/machines` | ❌ No equivalent | ✅ Fixed (stub empty) |
| `GET /v1/account/settings` | `GET /api/config` | ✅ Fixed (local-only) |
| `POST /v1/account/settings` | `PUT /api/config` | ✅ Fixed (local-only) |
| `GET /v1/account/profile` | ❌ No equivalent | ✅ Fixed (no-op) |
| `POST /v1/version` | `GET /api/status` | ✅ Fixed |
| `GET /v3/sessions/{id}/messages` | `GET /api/sessions/{id}/messages` | ✅ Fixed |
| `POST /v3/sessions/{id}/messages` | ❌ No equivalent | ⚠️ Not fixed (see below) |
| socket.io `/v1/updates` | WebSocket `/api/ws` | ✅ Fixed |
| Push token registration | ❌ No equivalent | ⚠️ Fails silently |

### Data Format Mismatches Found

**Sessions:**
- Happy API: encrypted `metadata`, `agentState`, per-session `dataEncryptionKey`, `seq`, `tag`
- NasTech API: plain `title`, `started_at`, `last_active`, `is_active`, `preview`, `model`

**Messages:**
- Happy API: encrypted `ApiMessage` objects with `seq`, `localId`, `createdAt` in ms
- NasTech API: plain `{ id (int), role, content (str/obj), created_at (ISO string), session_id }`

**Machines:**
- Happy API: encrypted machine objects with daemon state
- NasTech API: none — NasTech is single-machine by design

---

## Undone Errors / Known Issues

### 🔴 Critical — Not Yet Fixed

1. **`flushOutbox` / Send Message still hits `/v3/sessions/{id}/messages`**  
   The `flushOutbox` function in sync.ts still sends `POST /v3/sessions/${id}/messages` via `apiSocket.request()`. This will return 404 from NasTech. NasTech is an autonomous AI agent — it doesn't accept user-pushed messages. However, if the UI allows typing and sending, it will silently fail.  
   **Fix needed:** Either disable the send UI for NasTech sessions, or add a `POST /api/sessions/{id}/messages` endpoint to NasTech backend that queues a user message for the agent.

2. **`apiSocket.initialize()` still called with NasTech endpoint**  
   Lines ~2693 and ~2741 in sync.ts still call `apiSocket.initialize({ endpoint: API_ENDPOINT }, encryption)`. socket.io will try to handshake with NasTech backend, fail, and keep retrying. This is a silent background noise — it doesn't crash the app — but it wastes connections.  
   **Fix needed:** Stub `apiSocket.initialize()` for NasTech to be a no-op, or remove those calls.

3. **Encryption layer not bypassed for NasTech sessions**  
   When `fetchMessages` is called, it checks `this.encryption.getSessionEncryption(sessionId)`. For NasTech sessions initialized with null keys, this returns a `SessionEncryption` backed by `legacyEncryption` (SecretBox with master secret). The old `applyFetchedMessages` path would try to decrypt NasTech plaintext with this key and fail. The new `fetchInitialLatestPage` and `fetchForwardSince` bypass this by using `mapNasTechRawMsg` directly — but the check on line ~1824 (`if (!encryption) throw`) still runs and may gate the message fetch.  
   **Fix needed:** In `fetchMessages`, bypass the encryption guard for NasTech sessions.

4. **Push notifications fail**  
   `registerPushToken` calls `syncCurrentPushToken(this.credentials)` which likely hits a Happy API endpoint. NasTech has no push notification relay.  
   **Fix needed:** Stub out `registerPushToken` or implement `/api/push/register` on NasTech backend.

### 🟡 Moderate — Will Degrade Experience

5. **Session encryption guard in `fetchMessages`**  
   `fetchMessages` at line ~1824 checks `encryption = this.encryption.getSessionEncryption(sessionId)`. Since NasTech sessions ARE initialized (with null keys) in `fetchSessions`, `getSessionEncryption()` should return the legacy encryptor — not null. But this needs runtime verification.

6. **Sending messages from the app will silently fail**  
   The UI may allow composing and sending messages. `flushOutbox` will 404. No user-visible error is shown.

7. **`nastechApi.ts` is still orphaned**  
   The file `NasTech/sources/sync/nastechApi.ts` was created as a clean NasTech API layer but is never imported anywhere in the app. It duplicates some of what was fixed in sync.ts.  
   **Fix needed:** Either wire `nastechApi.ts` into the app's main data flow and deprecate the sync.ts overrides, or delete it to avoid confusion.

8. **Session `tag` and `seq` fields are stubs**  
   NasTech sessions are mapped with `tag: ''` and `seq: 0`. The app may use `seq` for ordering or deduplication in edge cases.

9. **No session "active" indicator from WebSocket**  
   The NasTech `/api/ws` WebSocket speaks JSON-RPC (used for the web dashboard). The mobile app's new `subscribeToUpdates` connects to it and calls `sessionsSync.invalidate()` on any message — which works but is coarse-grained (all sessions reload on any activity). A more efficient approach would subscribe to `/api/events` for SSE or parse the JSON-RPC messages to only refresh the affected session.

10. **`prefetchOlderMessagesInBackground` loops unnecessarily**  
    After `fetchInitialLatestPage` sets `hasMoreOlder: false`, the background prefetch loop in `prefetchOlderMessagesInBackground` checks `if (!sessionMessages.hasMoreOlder) return` and exits. This is correct. But on slow devices the loop may run one extra iteration before the state propagates.

### 🟢 Minor — Low Priority

11. **`GoogleService-Info.plist` has single bundle ID (`ai.nastech.ba`)**  
    The iOS plist only covers the production bundle. EAS builds for `dev` and `preview` variants will use the same Firebase config.  
    **Fix needed:** Separate plist files per variant (or use a single one and accept it — Firebase will warn but it works for dev/preview builds).

12. **`friends`, `feed`, `artifacts` sync still called**  
    `friendsSync`, `feedSync`, `artifactsSync` are still called in the reconnect handler (now removed since `subscribeToUpdates` was replaced). But they're still initialized in the constructor and will try to hit Happy-specific endpoints. These will fail silently.

13. **`cryptography` master secret derived from stub `{ token: 'nastech-local', secret: 'nastech-local' }`**  
    The encryption module derives a `masterSecret` from credentials. With stub credentials, all NasTech sessions get the same deterministic encryption key — not a security problem since NasTech doesn't encrypt anything, but worth noting.

---

## Ideas & Upgrades

### Short-Term (Next Sprint)

1. **Add `POST /api/sessions/{id}/messages` to NasTech backend**  
   Allow the mobile app to send a user message that gets injected into the running agent session. This would make the app truly interactive, not just a viewer.

2. **Add `GET /api/account/profile` to NasTech backend**  
   Return a minimal profile (hostname, system info, NasTech version) so the app's profile screen shows something meaningful.

3. **Add `GET /api/account/settings` + `PUT /api/account/settings` to NasTech backend**  
   Persist user preferences (theme, model choice, etc.) server-side so they survive app reinstalls.

4. **Implement push notification relay on NasTech backend**  
   When the agent produces a new message, send a push notification to registered mobile devices via FCM/APNs. The Firebase config is now in place — just needs backend implementation.

5. **Replace `apiSocket.initialize()` calls with NasTech-specific init**  
   Add a NasTech-aware init that sets up the WebSocket connection to `/api/ws` instead of trying socket.io.

6. **Wire `nastechApi.ts` properly**  
   Merge `nastechApi.ts` with the sync layer instead of having parallel code paths.

### Medium-Term

7. **Session-level real-time updates**  
   Subscribe to `/api/events` (NasTech SSE) and parse session/message events to do targeted invalidation instead of full session reload on every WebSocket message.

8. **Message send flow**  
   Implement agent message injection — mobile app sends `POST /api/sessions/{id}/run` with user text, NasTech backend spawns/resumes the agent, streams the response back via WebSocket. This turns the app from a viewer into a true mobile control panel.

9. **Session creation from mobile**  
   Add `POST /api/sessions` to NasTech backend. Let the user start a new agent session from the mobile app, choose a profile/model, and have it spin up.

10. **Cron job management from mobile**  
    NasTech has a full cron job API (`/api/cron/jobs`). Surface this in the mobile app — view scheduled jobs, pause/resume, trigger manually.

11. **Agent log viewer**  
    NasTech has `GET /api/logs`. Add a log viewer screen to the mobile app for debugging agent runs in the field.

12. **Multi-server support**  
    NasTech's `serverConfig.ts` allows pointing at different hosts. Add a server management screen in the app where users can save multiple NasTech instances (home server, cloud, etc.) and switch between them.

13. **Proper variant-specific Firebase plists**  
    Create `GoogleService-Info.dev.plist` and `GoogleService-Info.preview.plist` for the respective build variants, and update `app.config.js` to select the correct one per variant.

### Long-Term

14. **End-to-end encryption option**  
    NasTech messages are stored plaintext in SQLite. Add optional encryption at the database layer for sensitive deployments. The mobile app's encryption layer (libsodium) is already integrated.

15. **OAuth provider management from mobile**  
    NasTech has a full OAuth flow at `/api/providers/oauth`. Surface this in the app so users can authorize tools (GitHub, Google, etc.) from their phone.

16. **MCP server management from mobile**  
    NasTech supports MCP servers. Add a mobile screen to add/remove/test MCP servers.

17. **Voice interface via ElevenLabs**  
    The app already has `elevenLabsAgentId` in config and `expo-audio`/LiveKit plugins. Wire the voice UI to NasTech sessions so users can talk to their agent.

---

## Files Changed This Session

| File | Change |
|------|--------|
| `NasTech/google-services.json` | Replaced with NasTech Firebase project credentials (3 variants) |
| `NasTech/GoogleService-Info.plist` | Created — iOS Firebase config |
| `NasTech/app.config.js` | Updated bundle IDs (`ai.nastech.ba.*`), added iOS `googleServicesFile` |
| `NasTech/sources/auth/tokenStorage.ts` | Returns stub credentials instead of null |
| `NasTech/sources/sync/sync.ts` | 9 wiring fixes + new `mapNasTechRawMsg` method |

---

## Previously Done (Prior Sessions)

- 558 real image assets downloaded from `slopus/happy` and committed to `nastech-ai/NasTech-Agent`
- NasTech logos generated (`logotype-light.png`, `logotype-dark.png`, `logo-black.png`, `zen-icon.png`)
- 8 Cloudflare Workers deployed at `nastech-agent.workers.dev`
- Full backend wiring audit completed (this document captures it)
- EAS Build confirmed working on prior runs (26875664837, 26875663640)
