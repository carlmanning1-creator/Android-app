# Documentation Generation

A systematic approach for creating codebase documentation.

## Core Process
1. Define what to document (function, class, module, or full file).
2. Examine the source code and understand its behavior.
3. Produce inline documentation for public functions and classes.
4. Create supplementary materials (README sections, inline comments for non-obvious logic).

## Key Documentation Elements
For public functions, include:
- Function signature with parameter and return types.
- Clear description of functionality.
- Parameter explanations with constraints.
- Return value descriptions.
- Error conditions and exceptions thrown.
- Practical code examples where helpful.

## Documentation Principles
- Document behavior, not implementation — focus on what code accomplishes, not how.
- Keep docs close to code — inline doc-comments for public APIs, block comments only for non-obvious logic.
- Avoid documenting straightforward code (simple getters/setters, self-explanatory wrappers).
- If explaining a function is too difficult, that is a refactoring signal.

## Documentation Format by Language
- **JavaScript/TypeScript**: JSDoc (`/** */`) for public functions.
- **Python**: Docstrings (`"""`) following Google or NumPy style.
- **Go**: GoDoc comments on exported symbols.
- **Rust**: `///` doc comments on public items.
- **Java/Kotlin**: Javadoc/KDoc (`/** */`) for public APIs.

## Quality Standards
- Use concrete examples over abstract descriptions.
- Maintain accuracy by referencing actual code.
- Update existing documentation instead of duplicating information.
- Do not document what is already clear from well-named identifiers.
