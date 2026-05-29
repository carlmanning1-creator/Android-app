Find and remove dead code, unused imports, and unreachable branches.

## Steps

1. Run the project's linter or static analysis tool:
   - JavaScript/TypeScript: `npm run lint` or `eslint .`
   - Python: `ruff check .` or `flake8`
   - Go: `staticcheck ./...`
   - Rust: `cargo clippy`
   - JVM/Kotlin: `./gradlew lint`
2. In the IDE, check for:
   - Unused imports (typically greyed out).
   - Unused variables and parameters (compiler warnings).
   - Unreachable code after unconditional returns.
3. Scan for unused exports/declarations:
   - Find all top-level functions, classes, and types.
   - Search the codebase for references to each.
   - Flag any with zero references outside their own file.
4. Detect unreachable code:
   - Code after unconditional `return`/`throw` statements.
   - Branches with impossible conditions.
5. Present findings grouped by category with confidence levels.
6. Apply removals only for high-confidence dead code.
7. Run the test suite after each removal batch to catch false positives.

## Format

```
Dead Code Analysis
==================

Unused imports: <N>
  - <file>:<line> - import <symbol>

Unused declarations: <N>
  - <file>:<line> - <symbol> (0 references)

Unreachable code: <N>
  - <file>:<lines> - <reason>

Safe to remove: <N> items
Needs review: <N> items
```

## Rules

- Never remove code that might be used via reflection or dynamic dispatch.
- Run the full test suite after removing each batch to catch false positives.
- Log removed code in git commit messages for easy reversal.
