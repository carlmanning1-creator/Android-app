Generate an onboarding guide for a new developer joining this project.

## Steps

1. Review `CLAUDE.md`, `README.md`, and the project structure to understand what exists.
2. Map the setup prerequisites:
   - Language runtime version (Node, Python, Go, Java, etc.).
   - Package manager and dependency installation steps.
   - Required environment variables or config files.
3. Document the development workflow:
   - How to clone and open the project.
   - How to install dependencies.
   - How to run the application locally.
   - How to run the test suite.
   - How to create a branch and submit changes.
4. Explain the key architectural concepts a new developer needs to understand:
   - High-level architecture and data flow.
   - Key design patterns used in the codebase.
   - Important modules and their responsibilities.
   - External dependencies and integrations.
5. Write the guide to `docs/onboarding.md`.

## Format

```markdown
# Developer Onboarding Guide

## Prerequisites
- [ ] Install <runtime> <version>
- [ ] Install <tool>

## Setup
1. Clone the repository
2. Install dependencies: `<command>`
3. Configure environment: copy `.env.example` to `.env` and fill in values

## Running the Application
<step-by-step instructions>

## Running Tests
<test command and what to expect>

## Project Structure
<overview of key files and directories>

## Architecture Overview
<explanation of key patterns and data flow>

## Development Workflow
<branching, committing, PR process>

## Common Troubleshooting
<known issues and fixes>
```

## Rules

- Write for someone with the relevant language experience but no knowledge of this specific project.
- Include exact commands, not vague instructions.
- Link to `CLAUDE.md` for detailed conventions rather than duplicating them.
- Include common troubleshooting at the end.
