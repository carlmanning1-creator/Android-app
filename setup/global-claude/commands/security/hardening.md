Apply security hardening measures to the codebase.

## Steps

### 1. HTTP Security Headers
- Add `Content-Security-Policy` to restrict resource origins.
- Add `X-Frame-Options: DENY` to prevent clickjacking.
- Add `X-Content-Type-Options: nosniff`.
- Add `Strict-Transport-Security` (HSTS) for HTTPS-only deployments.
- Add `Referrer-Policy: no-referrer-when-downgrade`.

### 2. Authentication and Sessions
- Verify password hashing uses bcrypt, scrypt, or argon2id. Never MD5 or SHA-256 alone.
- Ensure session tokens are invalidated on logout and password change.
- Add rate limiting to login and authentication endpoints.
- Enforce account lockout after repeated failed attempts.
- Verify JWT tokens check signature, expiration (`exp`), and issuer (`iss`).

### 3. Input Validation
- Validate all user-supplied input at the API boundary.
- Use parameterized queries or ORM builders — never string-concatenated SQL.
- Sanitize output rendered to HTML to prevent XSS.
- Validate file uploads: check MIME type, extension, and size limits.

### 4. Dependency Hardening
- Pin dependency versions to avoid supply chain attacks.
- Enable automated dependency scanning (Dependabot, Renovate, or equivalent).
- Remove unused dependencies to reduce attack surface.

### 5. Secrets and Configuration
- Verify no secrets are hardcoded in source code.
- Use environment variables or a secrets manager for all credentials.
- Rotate any secrets that may have been previously committed.
- Confirm `.gitignore` excludes `.env`, key files, and local config.

### 6. Logging and Error Handling
- Ensure error responses do not expose stack traces or internal paths.
- Confirm sensitive data (passwords, tokens, PII) is not written to logs.
- Log authentication events, privilege escalations, and security-relevant actions.

## Rules

- Apply hardening incrementally. Test after each change.
- Do not break existing functionality. Security measures should be transparent to legitimate use.
- Document any changes that affect the deployment configuration.
