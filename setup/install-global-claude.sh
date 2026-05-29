#!/usr/bin/env bash
# Installs generic Claude Code commands and hooks to ~/.claude/
# Run from the repo root: bash setup/install-global-claude.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GLOBAL_DIR="$HOME/.claude"
SOURCE_DIR="$REPO_ROOT/setup/global-claude"
HOOKS_SOURCE="$REPO_ROOT/.claude/hooks/scripts"

print_step() { printf '\n\033[1;34m→ %s\033[0m\n' "$1"; }
print_ok()   { printf '  \033[0;32m✓ %s\033[0m\n' "$1"; }
print_warn() { printf '  \033[0;33m! %s\033[0m\n' "$1"; }

# --- Prerequisites ---
print_step "Checking prerequisites"
for cmd in node python3; do
  if command -v "$cmd" &>/dev/null; then
    print_ok "$cmd found"
  else
    print_warn "$cmd not found — some hooks may not work"
  fi
done

# --- Directories ---
print_step "Creating ~/.claude directory structure"
mkdir -p "$GLOBAL_DIR/hooks/scripts"
mkdir -p "$GLOBAL_DIR/commands/agents"
mkdir -p "$GLOBAL_DIR/commands/architecture"
mkdir -p "$GLOBAL_DIR/commands/documentation"
mkdir -p "$GLOBAL_DIR/commands/git"
mkdir -p "$GLOBAL_DIR/commands/refactoring"
mkdir -p "$GLOBAL_DIR/commands/security"
mkdir -p "$GLOBAL_DIR/commands/testing"
print_ok "Directories created"

# --- settings.json ---
print_step "Installing settings.json"
SETTINGS_DEST="$GLOBAL_DIR/settings.json"
if [[ -f "$SETTINGS_DEST" ]]; then
  BACKUP="$SETTINGS_DEST.bak.$(date +%Y%m%d%H%M%S)"
  cp "$SETTINGS_DEST" "$BACKUP"
  print_warn "Existing settings.json backed up to $BACKUP"
fi
cp "$SOURCE_DIR/settings.json" "$SETTINGS_DEST"
print_ok "settings.json installed"

# --- Hook scripts ---
print_step "Installing hook scripts"
if [[ -d "$HOOKS_SOURCE" ]]; then
  cp "$HOOKS_SOURCE"/*.js "$GLOBAL_DIR/hooks/scripts/" 2>/dev/null || true
  cp "$HOOKS_SOURCE"/*.py "$GLOBAL_DIR/hooks/scripts/" 2>/dev/null || true
  chmod +x "$GLOBAL_DIR/hooks/scripts"/*.js 2>/dev/null || true
  chmod +x "$GLOBAL_DIR/hooks/scripts"/*.py 2>/dev/null || true
  HOOK_COUNT=$(ls "$GLOBAL_DIR/hooks/scripts/" | wc -l | tr -d ' ')
  print_ok "$HOOK_COUNT hook scripts installed"
else
  print_warn "Hook scripts not found at $HOOKS_SOURCE — skipping"
fi

# --- Slash commands ---
print_step "Installing slash commands"
cp -r "$SOURCE_DIR/commands/." "$GLOBAL_DIR/commands/"
CMD_COUNT=$(find "$GLOBAL_DIR/commands" -name "*.md" | wc -l | tr -d ' ')
print_ok "$CMD_COUNT command files installed"

# --- Summary ---
printf '\n\033[1;32mDone.\033[0m Global Claude Code setup complete.\n'
printf '\nInstalled to: %s\n' "$GLOBAL_DIR"
printf '\nAvailable slash commands:\n'
find "$GLOBAL_DIR/commands" -name "*.md" | sort | while read -r f; do
  # Convert path to slash command format: ~/.claude/commands/git/commit.md → /git:commit
  rel="${f#"$GLOBAL_DIR/commands/"}"
  dir="$(dirname "$rel")"
  name="$(basename "$rel" .md)"
  if [[ "$dir" == "." ]]; then
    printf '  /%s\n' "$name"
  else
    printf '  /%s:%s\n' "$dir" "$name"
  fi
done

printf '\nThese commands and hooks are now active in every future Claude Code session.\n'
printf 'To add project-specific context, create a CLAUDE.md in the project root.\n\n'
