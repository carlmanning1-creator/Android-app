Analyze test coverage gaps and generate tests for uncovered code paths.

## Steps

### 1. Run Coverage Analysis
Run coverage for this Android project:
- Unit tests: `./gradlew test jacocoTestReport`
- Review the HTML report in `app/build/reports/jacoco/`

### 2. Identify Gaps
- List classes and functions below 80% line coverage.
- For each low-coverage file, identify:
  - Uncovered branches (if/else, when expressions, error paths).
  - Uncovered functions or methods.
  - Edge cases not exercised (null inputs, empty lists, boundary values).

### 3. Prioritize
- Rank gaps by risk: business logic > data parsing > UI composables > utilities.
- Focus on branches where bugs are most likely: error handling, JSON parsing, API response edge cases.
- In this project, prioritize: `parseMyAvail`, `parseOutlook`, `parsePlanner`, `parseActionItems`, `callClaude`.

### 4. Generate Tests
- Write JUnit4 tests in `app/src/test/` for pure Kotlin functions.
- Write Compose UI tests in `app/src/androidTest/` for composables.
- Each test targets a specific uncovered branch or function.
- Follow the Arrange-Act-Assert structure.

### 5. Verify
- Re-run coverage to confirm the gaps are filled.
- Ensure no existing tests broke.

## Rules

- Do not write tests purely to increase numbers. Every test must assert meaningful behavior.
- Target 80% line coverage and 75% branch coverage on new code as a minimum.
- If a function is genuinely untestable (e.g., thin Compose wrappers), document why rather than writing a meaningless test.
