Simplify code to improve readability and maintainability.

## Steps

### 1. Reduce Nesting
- Convert nested if/else chains to early returns (guard clauses).
- Replace nested lambdas with named functions.
- Use Kotlin's `when` expression instead of long if/else-if chains.
- Target: maximum 3 levels of indentation in any function.

### 2. Extract Functions and Composables
- Identify code blocks with a comment explaining what they do — the comment is a sign the block should be a named function.
- Extract repeated Compose UI patterns into reusable `@Composable` functions.
- Each composable should render one clearly defined piece of UI.
- Follow the existing `TileCard` pattern for new dashboard tiles.

### 3. Improve Naming
- Rename single-letter variables (except loop counters `i`, `j`).
- Replace abbreviations with full words.
- Boolean variables should read as questions: `isLoading`, `hasError`, `canDismiss`.
- Functions returning booleans should start with `is`, `has`, `can`, `should`.

### 4. Simplify Kotlin Idioms
- Use `?.let`, `?.run`, `?:` (Elvis) where they improve readability — not just to be idiomatic.
- Replace `if (x != null) x.foo() else null` with `x?.foo()`.
- Use `listOf().filter().map()` chains instead of manual loops where intent is clearer.
- Prefer `data class` copy() over manual mutation.

### 5. Verify
- Run `./gradlew test` after each simplification.
- Verify behavior is unchanged, not just that tests pass.
- Check that the code is actually more readable — simpler means easier to understand on first read, not fewer lines.

## Rules

- Simpler means easier to understand on first read, not fewer lines.
- Do not sacrifice clarity for cleverness. Explicit beats implicit.
- Preserve all existing behavior. This is refactoring, not rewriting.
- Make one kind of simplification per commit for clean diffs.
