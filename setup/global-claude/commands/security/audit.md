# Security Audit

Perform a security audit of the codebase covering common vulnerability categories.

## Steps

### 1. Dependency Vulnerabilities
- Run the package manager's audit tool (`npm audit`, `pip audit`, `cargo audit`, `govulncheck`, `./gradlew dependencyCheckAnalyze`).
- List critical and high severity vulnerabilities.
- For each, determine if the vulnerable code path is actually reachable in this project.
- Recommend specific version upgrades or patches.

### 2. Secrets Scan
- Search for hardcoded secrets, API keys, tokens, and passwords.
- Check `.gitignore` for proper exclusion of sensitive files.
- Verify environment variables or secure storage are used for all secrets.

### 3. OWASP Top 10 Check
- **Injection**: SQL injection, command injection, XSS. Search for string concatenation in queries, `eval()`, `innerHTML`.
- **Broken Auth**: Weak credential handling, missing rate limiting, session fixation.
- **Sensitive Data Exposure**: Unencrypted data at rest/transit, verbose error messages, logs containing PII or tokens.
- **Broken Access Control**: Missing authorization checks, IDOR vulnerabilities.
- **Security Misconfiguration**: Default credentials, unnecessary features enabled, debug endpoints left on in production.

### 4. Input Validation
- Verify all user inputs are validated before processing.
- Check for proper type coercion and boundary checking.
- Verify URL and redirect validation prevents open redirects.

### 5. Logging and Monitoring
- Verify authentication events are logged.
- Check that sensitive data (tokens, passwords, PII) is not included in log entries.
- Verify errors return safe messages to clients without exposing internals.

### 6. Report
Produce a findings report organized by severity (Critical, High, Medium, Low, Info) with:
- Finding description.
- Affected file and line.
- Recommended fix.
- Reference (CWE number or OWASP category).

## Rules
- Prioritize findings by exploitability and impact, not just theoretical risk.
- Do not just list tools to run — actually analyze the code and provide actionable recommendations.
