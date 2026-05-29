const fs = require("fs");
const path = require("path");

// Claude Code sends hook data via stdin as JSON with tool_input wrapper
let input = {};
try {
  const raw = fs.readFileSync(0, "utf8");
  const parsed = JSON.parse(raw);
  input = parsed.tool_input || parsed;
} catch (e) {}

const filePath = input.file_path || input.filePath || "";

if (!filePath.endsWith(".md")) {
  process.exit(0);
}

const allowedPatterns = [
  "/docs/",
  "/documentation/",
  "CLAUDE.md",
  "AGENTS.md",
  "CHANGELOG.md",
  "CODEMAP.md",
  "CONTRIBUTING.md",
  "SKILL.md",
  "README.md",
  "/commands/",
  "/skills/",
  "/agents/",
  "/rules/",
  "/templates/",
];

const isAllowed = allowedPatterns.some((pattern) => filePath.includes(pattern));

if (!isAllowed) {
  const fileName = path.basename(filePath);
  console.log(
    JSON.stringify({
      warning: `Creating ${fileName} outside of standard documentation directories. Make sure this file is intentional and not auto-generated boilerplate.`,
      file: filePath,
    })
  );
} else {
  process.exit(0);
}
