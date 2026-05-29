# SES Unit Dashboard — Claude Code Guide

## What This App Is

A personal Android dashboard for **Carl Manning**, Deputy at **NSW SES Dubbo Unit**. It aggregates four data sources into a single screen and uses Claude (Haiku) to triage unread items into prioritised action items.

**Data sources:** myAvailability (SES), Outlook personal, Outlook DBO Ops shared mailbox, Microsoft Planner
**Core loop:** WebViews load each source → JS injected via `buildInterceptorJs()` scrapes data → `ScrapeBridge.reportData()` sends JSON to Kotlin → "Refresh All" button triggers Claude to classify everything into action items by priority

---

## Architecture

Everything lives in a single file: `app/src/main/java/com/carlmanning/sesdashboard/MainActivity.kt`

```
MainActivity
├── TabRow — horizontal scrolling FilterChip tabs (Dashboard + 4 sources + Settings)
├── ActionButtons — Refresh / Refresh All
├── DashboardScreen — scrollable column of TileCard composables
│   ├── ActionItemsTile — Claude-generated triage output
│   ├── InboxTile — Outlook unread counts
│   ├── ActivitiesTile — myAvailability operational/activity/OOAA
│   ├── PlannerTile — open tasks
│   └── MessagesTile — Stream Chat unread
├── SettingsScreen — Anthropic API key entry + about
└── SourceWebView × 4 — WebViews rendered in a Box, shown/hidden via alpha/zIndex
```

**State is held in top-level `mutableStateOf` globals** — `myAvailState`, `outlookPersonalState`, `outlookDboOpsState`, `plannerState`, `actionItemsState`. This is intentional for simplicity in a single-user personal app; do not refactor to ViewModel unless the app grows significantly.

**Claude API pipeline:**
1. `buildActionsPrompt()` — assembles a text prompt from all current state
2. `callClaude()` — direct `HttpURLConnection` POST to `api.anthropic.com/v1/messages`, runs on `Dispatchers.IO`
3. `parseActionItems()` — parses the JSON array response into `List<ActionItem>`

**JS bridge:** `ScrapeBridge` (`@JavascriptInterface`) receives JSON from injected JS. `buildInterceptorJs()` patches `window.fetch` and `XMLHttpRequest` to capture API responses, then exposes `window.refresh()` which triggers source-specific extractors.

---

## Tech Stack

| Thing | Detail |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Build | Gradle KTS (`:app` module) |
| minSdk | 30 |
| targetSdk / compileSdk | 36 |
| WebView | `androidx.webkit` — multi-profile support for separate Outlook sessions |
| Claude | `claude-haiku-4-5-20251001` via direct HTTP |
| Tests | JUnit4 (unit), Espresso + Compose UI Test (instrumented) |

---

## Kotlin & Compose Conventions

- **Naming:** `camelCase` for functions/variables, `PascalCase` for classes/composables/data classes, `UPPER_SNAKE_CASE` for true constants (`DASHBOARD_TAB`, `PREFS_NAME`, `CLAUDE_MODEL`)
- **Booleans:** prefix with `is`, `has`, `can`, `should` — e.g. `isMinifyEnabled`, `isActive`
- **Composables:** named as nouns or noun phrases describing what they render (`ActionItemsTile`, `TileCard`, `PrioritySection`) — not verbs
- **State:** use `by remember { mutableStateOf(...) }` inside composables; use top-level `mutableStateOf` only for cross-composable shared state
- **Coroutines:** use `Dispatchers.IO` for network, `Dispatchers.Main` for UI updates via `Handler(Looper.getMainLooper()).post {}`
- **Data classes:** immutable with default values; parse at the boundary (`parseMyAvail`, `parseOutlook`, `parsePlanner`) — never pass raw `JSONObject` into composables
- **No magic strings:** extract to `const val`. Existing: `PREFS_NAME`, `PREF_API_KEY`, `CLAUDE_MODEL`, `CLAUDE_API_URL`, `DASHBOARD_TAB`, `SETTINGS_TAB`
- **Function length:** keep composables under 60 lines. Extract `TileCard` wrapper pattern for new tiles. Extract private helper composables with `@Composable private fun`
- **Imports:** Android SDK first, then Compose, then third-party, then project-local

---

## File Structure

```
app/src/main/
├── java/com/carlmanning/sesdashboard/
│   └── MainActivity.kt          ← entire app logic
├── res/
│   ├── values/strings.xml
│   ├── values/colors.xml
│   └── values/themes.xml
└── AndroidManifest.xml

app/src/test/                    ← JUnit4 unit tests
app/src/androidTest/             ← Espresso/Compose instrumented tests
```

When the app grows beyond one file, split by concern into:
- `data/` — state holders, parsers, API client
- `ui/` — composables by screen
- `bridge/` — `ScrapeBridge` + JS builders

---

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Full check (lint + test + build)
./gradlew check
```

---

## Security Rules

- **The Anthropic API key is the most sensitive data in this app.** It is stored in `SharedPreferences` (`ses-dashboard-prefs`) and passed directly to `api.anthropic.com`. Never log it, never put it in a `BuildConfig` field that ends up in the APK, never commit it anywhere.
- The key is entered by the user in the Settings tab and cleared with the "Clear" button. This is the correct flow — do not add auto-populated defaults.
- `WebView.setWebContentsDebuggingEnabled(true)` is currently always on. This should be gated to debug builds only: `WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)`.
- Cookie storage and third-party cookies are enabled intentionally — required for Outlook/Planner session persistence. Do not remove.
- The SES API bearer token (`localStorage.getItem('accessToken')` in the injected JS) is captured from the live WebView session. It is never stored by the app — it stays in WebView memory only.

---

## Error Handling

- Network errors in `callClaude()` are caught and surfaced via `actionItemsError` state, shown in `ActionItemsTile` with a Retry button. Follow this pattern for any new async operations.
- `ScrapeBridge.reportData()` wraps JSON parsing in a try/catch and leaves `lastExtractedData` as raw JSON on failure — useful for debugging from the Settings tab.
- JS errors in injected scripts are swallowed silently (try/catch in JS) to avoid crashing the WebView client. Log important failures via `postBridge({ error: '...' })` so they surface in `lastExtractedData`.
- Never use empty `catch (e: Exception) {}` blocks. Either handle, log, or rethrow with context.
- Prefer `e.message ?: "Unknown error"` for user-facing error strings.

---

## Git Workflow

Commits follow **conventional commits** format — enforced by the `commit-guard` hook:

```
type(scope): subject in lowercase, max 72 chars, no period
```

Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `style`, `ci`

Examples:
- `feat(dashboard): add OOAA approvals count to activities tile`
- `fix(bridge): handle missing accessToken gracefully in refreshSesData`
- `chore(deps): bump androidx.webkit to 1.11.0`

Branch naming: `feature/short-description`, `fix/short-description`, `chore/short-description`

---

## Claude API Usage

The app currently calls Claude directly from `triggerActionItemsGeneration()`. If extending the Claude integration:

- Model is `claude-haiku-4-5-20251001` — fast and cheap, correct choice for triage tasks
- `max_tokens = 2048` — sufficient for the JSON array response
- The system prompt (`ACTION_SYSTEM_PROMPT`) is the most important tuning lever. Edit it when triage quality needs improvement, not the parsing logic.
- `parseActionItems()` is defensive — it strips markdown fences and finds the JSON array by bracket scanning. Keep this robust.
- Network calls use `HttpURLConnection` with 30s connect / 60s read timeouts. Do not reduce these — Haiku can be slow under load.

---

## Available Slash Commands

Type `/` in Claude Code to access these:

| Command | What it does |
|---|---|
| `/git:commit` | Analyses staged changes and generates a conventional commit message |
| `/git:pr-create` | Drafts a PR description from branch changes |
| `/git:pr-review` | Reviews an open PR for issues |
| `/git:fix-issue` | Works through a GitHub issue to a fix |
| `/testing:tdd` | Guides a TDD red/green/refactor cycle |
| `/testing:test-fix` | Diagnoses and fixes a failing test |
| `/testing:test-coverage` | Identifies coverage gaps and adds missing tests |
| `/security:audit` | Full OWASP-style security audit |
| `/security:secrets-scan` | Scans for hardcoded secrets and credentials |
| `/security:hardening` | Suggests security hardening improvements |
| `/architecture:design-review` | Structured review of a module or feature design |
| `/architecture:refactor` | Five-phase systematic refactor with test safety |
| `/refactoring:extract` | Extracts a function, composable, or module |
| `/refactoring:cleanup` | Cleans up dead code and formatting issues |
| `/refactoring:simplify` | Reduces complexity without changing behaviour |
| `/documentation:doc-gen` | Generates KDoc / inline documentation |
| `/documentation:onboard` | Produces an onboarding guide for the codebase |
| `/agents:mobile-developer` | Mobile specialist mode for Android architecture decisions |
| `/agents:code-reviewer` | Deep code review with structured findings |
| `/agents:security-auditor` | Security specialist mode for threat modelling |
| `/agents:error-detective` | Root-cause analysis for bugs and crashes |
| `/agents:performance-engineer` | Performance analysis and optimisation |
