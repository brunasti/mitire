#!/bin/bash
# Called by Claude Code UserPromptSubmit hook.
# Reads the hook JSON payload from stdin, extracts the prompt, and appends it
# with a timestamp to docs/mitire-prompts.md.

PROMPT=$(cat | jq -r '.prompt // empty' 2>/dev/null)

if [ -n "$PROMPT" ]; then
    TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")
    printf '\n---\n\n**%s**\n\n%s\n' "$TIMESTAMP" "$PROMPT" >> "./docs/mitire-prompts.md"
fi
