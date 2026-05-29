# Documentation Generation

A systematic approach for creating codebase documentation.

## Core Process
1. Define what to document (function, class, module, or full file).
2. Examine the source code and understand its behavior.
3. Produce KDoc documentation for public functions and classes.
4. Create supplementary materials (README sections, inline comments for non-obvious logic).

## Key Documentation Elements
For public functions, include:
- Function signature with parameter and return types.
- Clear description of functionality.
- `@param` explanations with constraints.
- `@return` value descriptions.
- `@throws` error conditions.
- Practical code examples where helpful.

## Documentation Principles
- Document behavior, not implementation — focus on what code accomplishes, not how.
- Keep docs close to code — inline KDoc for public APIs, block comments only for non-obvious logic.
- Avoid documenting straightforward code (simple getters/setters, obvious composables).
- If explaining a function is too difficult, that is a refactoring signal.

## Android/Kotlin Specifics
- Use KDoc format (`/** */`) for public functions and classes.
- Document `@Composable` functions: what they render, what state they observe, what callbacks they expose.
- Document `@JavascriptInterface` methods carefully — note what JSON shape they expect.
- Comment JS strings (`buildInterceptorJs`, `buildHeightFixJs`) at the section level, not line by line.

## Quality Standards
- Use concrete examples over abstract descriptions.
- Maintain accuracy by referencing actual code.
- Update existing documentation instead of duplicating information.
