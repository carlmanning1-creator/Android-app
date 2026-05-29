Generate an onboarding guide for a new developer joining this project.

## Steps

1. Review `CLAUDE.md`, `README.md`, and the project structure to understand what exists.
2. Map the setup prerequisites:
   - Android Studio version.
   - Android SDK version (minSdk 30, targetSdk 36).
   - Java 11 (required by `compileOptions`).
3. Document the development workflow:
   - How to clone and open in Android Studio.
   - How to run the app on a device or emulator.
   - How to run tests.
   - How to create a branch and commit.
4. Explain the key architectural concepts a new developer needs to understand:
   - The WebView + JS bridge scraping pattern.
   - The `ScrapeBridge` JavascriptInterface.
   - The top-level `mutableStateOf` globals and why they exist.
   - The Claude API triage pipeline.
5. Document the four data sources and how each is scraped.
6. Write the guide to `docs/onboarding.md`.

## Format

```markdown
# Developer Onboarding Guide

## Prerequisites
- [ ] Install Android Studio <version>
- [ ] Install Android SDK <version>

## Setup
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Add your Anthropic API key in the Settings tab at runtime

## Running the App
<step-by-step instructions>

## Project Structure
<overview of key files and directories>

## Architecture Overview
<explanation of WebView scraping pattern, state model, Claude pipeline>

## Development Workflow
<branching, committing, testing>

## Common Troubleshooting
<known issues and fixes>
```

## Rules

- Write for someone with Android experience but no knowledge of this specific app.
- Include exact commands, not vague instructions.
- Link to `CLAUDE.md` for detailed conventions rather than duplicating them.
- Include common troubleshooting at the end.
