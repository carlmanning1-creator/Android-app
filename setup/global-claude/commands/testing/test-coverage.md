Analyze test coverage gaps and generate tests for uncovered code paths.

## Steps

### 1. Run Coverage Analysis
Run coverage using the project's toolchain:
- JavaScript/TypeScript: `npm run test -- --coverage` or `jest --coverage`
- Python: `pytest --cov=<package> --cov-report=html`
- Go: `go test ./... -coverprofile=coverage.out && go tool cover -html=coverage.out`
- JVM/Kotlin: `./gradlew test jacocoTestReport`
- Rust: `cargo tarpaulin`

### 2. Identify Gaps
- List classes and functions below 80% line coverage.
- For each low-coverage file, identify:
  - Uncovered branches (if/else, switch, error paths).
  - Uncovered functions or methods.
  - Edge cases not exercised (null inputs, empty collections, boundary values).

### 3. Prioritize
- Rank gaps by risk: business logic > data parsing > I/O > utilities.
- Focus on branches where bugs are most likely: error handling, external API responses, user input validation.

### 4. Generate Tests
- Write unit tests for pure functions and business logic.
- Write integration tests for I/O boundaries (database, HTTP, filesystem).
- Each test targets a specific uncovered branch or function.
- Follow the Arrange-Act-Assert structure.
- Use descriptive test names: `should <behavior> when <condition>`.

### 5. Verify
- Re-run coverage to confirm the gaps are filled.
- Ensure no existing tests broke.

## Rules

- Do not write tests purely to increase numbers. Every test must assert meaningful behavior.
- Target 80% line coverage and 75% branch coverage on new code as a minimum.
- If a function is genuinely untestable (e.g., thin wrappers), document why rather than writing a meaningless test.
