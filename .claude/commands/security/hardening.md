Apply security hardening measures to the codebase.

## Steps

### 1. Android-Specific Hardening
- Gate `WebView.setWebContentsDebuggingEnabled(true)` behind `BuildConfig.DEBUG`.
- Enable `isMinifyEnabled = true` in release build type and verify ProGuard rules.
- Review `AndroidManifest.xml`: remove `android:exported="true"` from any activity/service that does not need it.
- Add `android:usesCleartextTraffic="false"` to the manifest application element (HTTPS only).
- Review permissions in the manifest — remove any not actively used.

### 2. WebView Security
- Review the Content Security Policy for injected JavaScript.
- Ensure `addJavascriptInterface` only exposes the minimum required methods.
- Verify that `@JavascriptInterface` methods validate and sanitize their inputs.
- Consider restricting `webViewClient.shouldOverrideUrlLoading` to expected domains.

### 3. Data Storage
- API key is stored in SharedPreferences — consider migrating to `EncryptedSharedPreferences` (Jetpack Security) for an extra layer of protection.
- Verify no sensitive data is written to external storage or logs.
- Check `android:allowBackup` in the manifest — set to `false` or configure `android:fullBackupContent` rules.

### 4. Network Security
- Add a `network_security_config.xml` that explicitly pins or restricts allowed CAs for `api.anthropic.com` and SES API endpoints.
- Verify all HTTP calls use TLS (no plain HTTP to any endpoint).
- Set reasonable timeouts on all `HttpURLConnection` calls (already done: 30s/60s — verify these are appropriate).

### 5. Input Validation
- Validate the Anthropic API key format before storing it (should start with `sk-ant-`).
- Validate JSON responses from the Claude API before parsing — handle malformed responses gracefully.
- Ensure `parseActionItems()` handles unexpected JSON structures without crashing.

## Rules

- Apply hardening incrementally. Test after each change with `./gradlew assembleRelease`.
- Do not break existing functionality. Security measures should be transparent to legitimate use.
- Document any changes that affect the build configuration or manifest.
