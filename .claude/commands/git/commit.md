# Conventional Commit Message Generator

Analyze staged Git changes and generate a conventional commit message following standard formatting conventions.

## Steps
1. Examine staged file changes via `git diff --cached --stat`.
2. Review actual modifications with `git diff --cached`.
3. Classify the change into one of seven types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, or `style`.

## Commit Message Structure
- Format: `type(scope): subject line`
- Optional body section.
- Subject line: imperative mood, lowercase, no period, max 72 characters.
- Body: wrap at 80 characters, explain the motivation (why), not the what.
- Blank line between subject and body.

## Constraints
- Avoid committing generated files, lock files, or build artifacts unless intentionally desired.
- When multiple distinct changes are staged together, recommend splitting into separate commits.
