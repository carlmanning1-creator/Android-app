Find and remove dead code, unused imports, and unreachable branches.

## Steps

1. Run Android lint to detect issues: `./gradlew lint` — review `app/build/reports/lint-results-debug.html`.
2. In Android Studio / IDE, check for:
   - Unused imports (greyed out in the editor).
   - Unused variables and parameters (warnings from the Kotlin compiler).
   - Unreachable code after unconditional returns.
3. Scan for unused exports:
   - Find all top-level functions, composables, and data classes.
   - Search the codebase for references to each.
   - Flag any with zero references outside their own file.
4. Detect unreachable code:
   - Code after unconditional `return`/`throw` statements.
   - `when` branches with impossible conditions.
5. Present findings grouped by category with confidence levels.
6. Apply removals only for high-confidence dead code.
7. Run `./gradlew test` after each removal batch to catch false positives.

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
- Preserve anything referenced in `AndroidManifest.xml`.
- Run the full test suite after removing each batch to catch false positives.
- Log removed code in git commit messages for easy reversal.
