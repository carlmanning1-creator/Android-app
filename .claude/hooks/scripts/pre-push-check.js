const fs = require("fs");

// Claude Code sends hook data via stdin as JSON with tool_input wrapper
let input = {};
try {
  const raw = fs.readFileSync(0, "utf8");
  const parsed = JSON.parse(raw);
  input = parsed.tool_input || parsed;
} catch (e) {}

const command = (input.command || "").trim();
if (!command.startsWith("git push")) process.exit(0);

const warnings = [];

if (command.includes("--force") || command.includes("-f")) {
  warnings.push("Force push detected. This rewrites remote history and may affect collaborators.");
}

if (command.includes("main") || command.includes("master")) {
  if (command.includes("--force") || command.includes("-f")) {
    warnings.push("Force pushing to main/master is dangerous. Consider creating a PR instead.");
  }
}

if (!command.includes("origin") && !command.includes("-u")) {
  warnings.push("No remote specified. Pushing to default remote.");
}

if (warnings.length > 0) {
  console.log(JSON.stringify({ warnings, command }));
} else {
  process.exit(0);
}
