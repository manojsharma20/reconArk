#!/bin/bash
# SessionStart: prepare cloud sessions and print the working agreement (stdout becomes session context).
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0

if [ "${CLAUDE_CODE_REMOTE:-}" = "true" ]; then
  # JDK 25 (the environment setup script normally provides it; this is the fallback)
  if ! ls /usr/lib/jvm 2>/dev/null | grep -q "java-25"; then
    (apt-get update -qq && apt-get install -y -qq openjdk-25-jdk-headless) >/dev/null 2>&1 || true
  fi
  J25=$(ls -d /usr/lib/jvm/java-25-openjdk-* 2>/dev/null | head -1)
  if [ -n "$J25" ] && [ -n "${CLAUDE_ENV_FILE:-}" ]; then
    echo "export JAVA_HOME=$J25" >> "$CLAUDE_ENV_FILE"
    echo "export PATH=$J25/bin:\$PATH" >> "$CLAUDE_ENV_FILE"
  fi
  # Frontend dependencies, only when missing (fast on resume)
  if [ -f frontend/package-lock.json ] && [ ! -d frontend/node_modules ]; then
    (cd frontend && npm ci --no-audit --no-fund >/dev/null 2>&1) || echo "WARN: npm ci failed in frontend/ — check network access (registry.npmjs.org)."
  fi
fi

branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
echo "reconArk session — branch: ${branch:-unknown}"
if [ "$branch" = "main" ] || [ "$branch" = "master" ]; then
  echo "You are on $branch. Create a branch before changing anything: git switch -c claude/<short-name>"
fi
echo "Rules: CLAUDE.md §1 (Lego rules). Design: docs/solution, docs/hld, docs/lld. Fast check: ./gradlew verifyQuick; frontend: npm run typecheck && npm test."
exit 0
