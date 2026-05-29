Simplify code to improve readability and maintainability.

## Steps

### 1. Reduce Nesting
- Convert nested if/else chains to early returns (guard clauses).
- Replace nested loops with higher-order functions (map, filter, reduce).
- Extract deeply nested callbacks into named functions.
- Target: maximum 3 levels of indentation in any function.

### 2. Extract Functions
- Identify code blocks with a comment explaining what they do — the comment is a sign the block should be a named function.
- Extract repeated logic into shared functions.
- Each function should do one thing. If you need "and" to describe it, split it.

### 3. Improve Naming
- Rename single-letter variables (except loop counters `i`, `j`, `k`).
- Replace abbreviations with full words: `usr` → `user`, `btn` → `button`, `cfg` → `config`.
- Boolean variables should read as questions: `isValid`, `hasPermission`, `canEdit`.
- Functions returning booleans should start with `is`, `has`, `can`, `should`.

### 4. Remove Duplication
- Identify repeated patterns across the codebase using search.
- Extract shared logic into utility functions or base classes.
- Use parameterization instead of copy-paste with minor changes.
- Tolerate duplication across module boundaries if coupling would be worse.

### 5. Simplify Conditionals
- Replace complex boolean expressions with descriptive named variables.
- Use lookup tables or maps instead of long switch/if-else chains.
- Replace nested ternaries with if/else or early returns.
- Use language idioms (optional chaining, nullish coalescing, pattern matching) where they improve clarity.

### 6. Verify
- Run the full test suite after each simplification.
- Confirm behavior is unchanged — simplification should not change observable behavior.
- Check that the code is actually more readable, not just shorter.

## Rules

- Simpler means easier to understand on first read, not fewer lines.
- Do not sacrifice clarity for cleverness. Explicit beats implicit.
- Preserve all existing behavior. This is refactoring, not rewriting.
- Make one kind of simplification per commit for clean diffs.
