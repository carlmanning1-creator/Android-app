# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew testDebugUnitTest      # Run a single test class: add --tests "com.carlmanning.sesdashboard.ExampleUnitTest"
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
./gradlew lint                   # Run Android Lint
./gradlew clean                  # Clean build artifacts
```

**SDK targets:** minSdk 30 (Android 11), compileSdk/targetSdk 36. Java 11. Kotlin 2.2.10, AGP 9.2.1.

## Architecture

The entire app lives in a single file: `app/src/main/java/com/carlmanning/sesdashboard/MainActivity.kt` (~1334 lines). There are no ViewModels, repositories, DI containers, or navigation components — state is managed as top-level Compose `mutableStateOf` values directly in the composable tree.

### Data Flow

1. Four `WebView` instances (stored in a `Map<String, WebView>`) load external services.
2. JavaScript is injected via `buildInterceptorJs()` — a ~1334-line JS string that hooks `fetch` and `XMLHttpRequest` to intercept API responses from those services.
3. Intercepted data is passed back to Kotlin via a `ScrapeBridge` (`JavascriptInterface`) which updates global mutable state.
4. The Dashboard tab aggregates all state and calls `callClaude()` to triage action items via the Anthropic API.
5. Triaged `ActionItem` results are rendered as priority-sorted tiles.

### Key Data Models (defined in MainActivity.kt)

- `Source` — WebView source config (name, URL, WebView profile name)
- `MyAvailData` — operational/activity/OOAA counts + titles from myAvailability
- `OutlookData` — unread count, recent senders, conversations from Outlook
- `PlannerData` — total/open task counts + titles from Microsoft Planner
- `ActionItem` — triaged item with source, subject, action, priority (from Claude)

### External Services & WebView Profiles

| Tab | URL | Profile |
|-----|-----|---------|
| myAvail | `myavailability.ses.nsw.gov.au` | default |
| Outlook Personal | `outlook.office.com` | default |
| DBO Ops | `outlook.cloud.microsoft/mail/dbo.ops@ses.nsw.gov.au/` | `"dboops"` (isolated via `ProfileStore`) |
| Planner | `planner.cloud.microsoft` | default |

The isolated WebView profile for DBO Ops allows a second Outlook authentication context to coexist with the personal one.

### Claude API Integration

- Endpoint: `https://api.anthropic.com/v1/messages`
- Model: `claude-haiku-4-5-20251001`
- Called in `callClaude()` via `HttpURLConnection` (30s connect / 60s read timeout)
- API key stored in `SharedPreferences` and configured in the Settings tab

### Navigation

Tab-based UI using Compose filter chips. No Jetpack Navigation component — tab selection is a single `mutableStateOf<String>` that controls which composable is rendered. WebView tabs are kept alive (not recomposed) by always rendering them with `alpha` visibility rather than removing them from composition.
