# Extract a Function, Class, or Module from Existing Code

## Steps

1. Identify the code block to extract from the argument (file path and line range, or description).
2. Read the target file and analyze the selected code:
   - Determine all variables used within the block that are defined outside it (parameters).
   - Determine all variables modified within the block that are used after it (return values).
   - Identify side effects (I/O, mutations, state updates).
3. Choose the extraction type:
   - **Function/method**: Pure logic with clear inputs and outputs.
   - **Class**: Related state and behavior that form a cohesive unit.
   - **Module/file**: Related functions that belong together.
   - **Extension/mixin**: Logic that naturally belongs on a type.
4. Create the extracted unit:
   - Name it descriptively based on its purpose.
   - Define a clear parameter interface with explicit types.
   - Add a return type annotation.
5. Replace the original code with a call to the extracted unit.
6. Run tests to verify the refactoring preserves behavior.

## Format
```
Extracted: <type> <name> from <source-file>
  Parameters: <param-list>
  Returns: <return-type>
  Lines replaced: <start>-<end>
  Tests: <pass/fail>
```

## Rules
- The extraction must be behavior-preserving; run tests before and after.
- Choose names that describe the purpose, not the implementation.
- Keep the extracted unit's parameter count under 5; use a parameter object if more are needed.
- Maintain the same error handling behavior in the extracted code.
