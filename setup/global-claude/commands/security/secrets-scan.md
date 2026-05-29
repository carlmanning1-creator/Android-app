Scan the codebase for leaked secrets, API keys, tokens, and credentials.

## Steps

1. Define patterns to search for:
   - Anthropic API keys: `sk-ant-[a-zA-Z0-9]{32,}`
   - AWS keys: `AKIA[0-9A-Z]{16}`, `aws_secret_access_key`
   - API keys: `api[_-]?key\s*[:=]`
   - Tokens: `ghp_`, `gho_`, `github_pat_`, `xoxb-`
   - Private keys: `-----BEGIN (RSA|EC|OPENSSH) PRIVATE KEY-----`
   - Database URLs: `(postgres|mysql|mongodb)://[^:]+:[^@]+@`
   - Generic secrets: `password\s*[:=]\s*["'][^"']+["']`
2. Scan all tracked files: `git ls-files` (skip binary files).
3. Also scan `.env` files and any local config files.
4. Exclude known safe patterns (test fixtures, `.env.example`, documentation examples).
5. Check if `.gitignore` properly excludes sensitive files (`.env`, `*.pem`, `*.key`, local config files).
6. Check git history for secrets in past commits: `git log -p --all -S "sk-ant"` (repeat for other patterns).

## Format

```
Secrets Scan Results
====================

CRITICAL (immediate action required):
  - <file>:<line> - <type>: <masked-value>

WARNING (review needed):
  - <file>:<line> - <type>: <description>

.gitignore check:
  - [ ] .env files excluded
  - [ ] Local config files excluded
  - [ ] Key/certificate files excluded

Remediation:
  1. Rotate <credential type>
  2. Add <pattern> to .gitignore
```

## Rules

- Never print full secret values; mask all but the first 4 characters.
- Scan both tracked and untracked files.
- Recommend using environment variables, secret managers, or encrypted local config for all secrets.
- Secrets should never be present in source code or committed files.
