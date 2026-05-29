const fs = require("fs");
const path = require("path");
const os = require("os");

const contextFile = path.join(os.homedir(), ".claude", "session-context.json");
const cwd = process.cwd();

let context = {};
if (fs.existsSync(contextFile)) {
  try {
    context = JSON.parse(fs.readFileSync(contextFile, "utf8"));
  } catch (e) {}
}

let buildSystem = "unknown";
if (fs.existsSync(path.join(cwd, "gradlew"))) buildSystem = "gradle";
else if (fs.existsSync(path.join(cwd, "pnpm-lock.yaml"))) buildSystem = "pnpm";
else if (fs.existsSync(path.join(cwd, "yarn.lock"))) buildSystem = "yarn";
else if (fs.existsSync(path.join(cwd, "package.json"))) buildSystem = "npm";
else if (fs.existsSync(path.join(cwd, "Pipfile.lock"))) buildSystem = "pipenv";
else if (fs.existsSync(path.join(cwd, "poetry.lock"))) buildSystem = "poetry";
else if (fs.existsSync(path.join(cwd, "go.sum"))) buildSystem = "go mod";
else if (fs.existsSync(path.join(cwd, "Cargo.lock"))) buildSystem = "cargo";

const lastSession = context[cwd];
const output = { buildSystem };

if (lastSession) {
  output.previousSession = {
    lastActive: lastSession.lastActive,
    editCount: lastSession.editCount || 0,
    notes: lastSession.notes || "",
  };
}

context[cwd] = {
  ...context[cwd],
  buildSystem,
  lastActive: new Date().toISOString(),
  editCount: 0,
};

fs.mkdirSync(path.dirname(contextFile), { recursive: true });
fs.writeFileSync(contextFile, JSON.stringify(context, null, 2));

console.log(JSON.stringify(output));
