#!/usr/bin/env bash
# NasUX — Push latest commit to GitHub
# Run after the Replit auto-commit has landed:
#   bash push-to-github.sh

set -e

if [ -z "$GITHUB_PERSONAL_ACCESS_TOKEN" ]; then
  echo "ERROR: GITHUB_PERSONAL_ACCESS_TOKEN secret not set."
  exit 1
fi

REMOTE="https://${GITHUB_PERSONAL_ACCESS_TOKEN}@github.com/nastech-ai/NasUX.git"

echo "Pushing NasUX to GitHub..."
git push "$REMOTE" master 2>&1 | grep -v "https://"

echo ""
echo "✓ NasUX pushed to https://github.com/nastech-ai/NasUX"
