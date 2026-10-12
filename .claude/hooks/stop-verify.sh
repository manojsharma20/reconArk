#!/bin/bash
# Stop: run fast verification for what changed; exit 2 (with stderr) makes Claude continue and fix failures.
input=$(cat)
if echo "$input" | python3 -c 'import json,sys; sys.exit(0 if json.load(sys.stdin).get("stop_hook_active") else 1)'; then
  exit 0   # already continuing because of this hook: do not loop
fi
[ "${RECONARK_SKIP_STOP_VERIFY:-}" = "1" ] && exit 0
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0

base=$(git merge-base HEAD origin/main 2>/dev/null || echo HEAD)
changed=$( (git diff --name-only "$base" 2>/dev/null; git ls-files --others --exclude-standard 2>/dev/null) | sort -u)
[ -z "$changed" ] && exit 0
fail=""

# Java: compile everything, run the tests that don't need Docker (common, plugins, engine-core).
if echo "$changed" | grep -qE '^code/(backend|recon-engine)/|^code/.*\.gradle\.kts$|libs\.versions\.toml$'; then
  if ! out=$(cd code && ./gradlew quickCheck -q 2>&1); then
    fail+=$'\n== cd code && ./gradlew quickCheck failed ==\n'"$(echo "$out" | tail -40)"
  fi
fi

if echo "$changed" | grep -q '^code/frontend/' && [ -d code/frontend/node_modules ]; then
  if ! out=$(cd code/frontend && npm run -s typecheck 2>&1 && npx vitest run --reporter=dot 2>&1); then
    fail+=$'\n== frontend typecheck/tests failed ==\n'"$(echo "$out" | tail -40)"
  fi
fi

if echo "$changed" | grep -qE '\.md$' && [ -d docs/_lint/node_modules ]; then
  if ! out=$(cd docs/_lint && npm run -s lint 2>&1); then
    fail+=$'\n== Mermaid diagrams failed to parse ==\n'"$(echo "$out" | tail -20)"
  fi
fi

if [ -n "$fail" ]; then
  echo "reconArk verification failed — fix before finishing (or explain why it cannot be fixed):$fail" >&2
  exit 2
fi
exit 0
