# Systematic Code Refactoring

A five-phase approach to improving code quality while maintaining correctness.

## Phase 1 — Identify Issues
Catalog common problems including:
- Long functions (>40 lines)
- Deep nesting (>3 levels)
- Duplicated logic
- Poor naming that obscures intent
Recommend extraction and simplification strategies.

## Phase 2 — Test Baseline
Before making changes, establish a green test suite.
If coverage is insufficient, write characterization tests that capture current behavior before changing anything.

## Phase 3 — Plan Atomically
Sequence changes carefully. Each step should be a single, atomic change (one rename, one extract, one move).

## Phase 4 — Apply Incrementally
Run tests after each modification to preserve behavior throughout the refactoring process.

## Phase 5 — Validate Thoroughly
Final verification includes full test execution, type checking, and behavior comparison.

## Key Principle
Refactoring changes structure, not behavior. If behavior changes, that is a feature or a fix.
Prefer multiple small commits over one massive change to facilitate code review and debugging.
