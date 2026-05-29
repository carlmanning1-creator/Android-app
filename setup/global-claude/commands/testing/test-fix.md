Diagnose and fix failing tests in the project.

## Steps

1. Run the test suite and capture output using the project's test command (e.g., `npm test`, `pytest`, `./gradlew test`, `go test ./...`, `cargo test`).
2. Parse the failure output to extract:
   - Test name and file location.
   - Expected vs actual values.
   - Stack trace and error message.
3. For each failing test, determine the root cause category:
   - **Logic change**: Source code changed but test was not updated.
   - **Environment issue**: Missing config, dependency, or timing problem.
   - **Flaky test**: Race condition, non-deterministic ordering.
   - **Real bug**: The test correctly caught a regression in the code.
4. Read the relevant source code and test code side by side.
5. Apply the fix:
   - Update assertions to match new behavior if the change was intentional.
   - Fix the source code if the test caught a real bug.
   - Add synchronization or retry logic for flaky async tests.
6. Re-run only the fixed tests to verify.
7. Run the full suite to check for regressions.

## Format

```
Failing tests: <N>

| Test | File | Cause | Fix |
|------|------|-------|-----|
| test name | path | category | what was done |

Result: <N>/<N> now passing
```

## Rules

- Never delete a failing test without understanding why it fails.
- If a test failure reveals a real bug, fix the source code, not the test.
- Distinguish between intentional behavior changes and regressions.
- Run the full suite after fixes to catch cascading failures.
