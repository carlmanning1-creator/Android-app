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

const sessionData = context[cwd] || {};
const editCount = sessionData.editCount || 0;
const reminders = [];

if (editCount > 5) {
  const isAndroid =
    fs.existsSync(path.join(cwd, "gradlew")) ||
    fs.existsSync(path.join(cwd, "build.gradle.kts")) ||
    fs.existsSync(path.join(cwd, "build.gradle"));

  const hasOtherTestCmd =
    fs.existsSync(path.join(cwd, "package.json")) ||
    fs.existsSync(path.join(cwd, "pyproject.toml")) ||
    fs.existsSync(path.join(cwd, "Makefile"));

  if (isAndroid) {
    reminders.push(
      `${editCount} files were modified this session. Consider running ./gradlew test (unit tests) or ./gradlew connectedAndroidTest (instrumented) before wrapping up.`
    );
  } else if (hasOtherTestCmd) {
    reminders.push(`${editCount} files were modified this session. Consider running the test suite before wrapping up.`);
  }
}

if (editCount > 0) {
  reminders.push("Check for unstaged changes with 'git status' before ending the session.");
}

console.log(JSON.stringify({ reminders, editCount }));
