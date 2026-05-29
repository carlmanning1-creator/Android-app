# TDD Cycle

## Red Phase
Write a failing test that describes the desired behavior. Ensure it fails for the right reason, not due to syntax errors.

## Green Phase
Implement minimal code to make the test pass without optimization. Verify the test succeeds.

## Refactor Phase
Clean up the code: eliminate duplication, improve naming, remove unnecessary complexity. Maintain test success throughout.

## Repeat
Continue the cycle for each new behavior, one at a time.

## Rules
- Each cycle targets exactly one behavior. Do not batch multiple behaviors.
- Tests must be independent. Use descriptive names: `should <expected behavior> when <condition>`.
- Mock external dependencies (HTTP, DB, filesystem, clock).
- Keep each cycle brief, typically under 5 minutes.
- If implementation takes longer, decompose into smaller increments.
