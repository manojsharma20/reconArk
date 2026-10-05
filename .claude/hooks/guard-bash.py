#!/usr/bin/env python3
"""PreToolUse guard for Bash: blocks history rewrites, pushes to main, TLS bypass, pipe-to-shell, destructive rm."""
import json, re, sys

cmd = ((json.load(sys.stdin).get("tool_input") or {}).get("command") or "")
rules = [
    (r"\bgit\s+push\b[^\n]*\s(-f|--force|--force-with-lease)\b", "Force-push is not allowed. Push a new commit instead."),
    (r"\bgit\s+push\b[^\n]*\s(origin\s+)?(HEAD:)?(main|master)\b", "Never push to main/master. Push a feature branch and open a PR."),
    (r"\bgit\s+commit\b[^\n]*--no-verify", "--no-verify bypasses safety checks."),
    (r"\bgit\s+reset\s+--hard\b", "git reset --hard discards work. Ask the user first."),
    (r"(curl|wget)\b[^\n|]*\|\s*(sudo\s+)?(ba|z)?sh\b", "Piping downloads into a shell is not allowed."),
    (r"(--insecure\b|\bcurl\b[^\n]*\s-k\b|NODE_TLS_REJECT_UNAUTHORIZED=0|GIT_SSL_NO_VERIFY|sslVerify\s*=?\s*false|strict-ssl\s+false)",
     "Disabling TLS verification is not allowed (see /root/.ccr/README.md for proxy CA setup in cloud sessions)."),
    (r"\brm\s+-[a-zA-Z]*r[a-zA-Z]*f?\s+(/|~|\$HOME|\.\.|\*)(\s|$)", "Destructive rm outside the project build output is blocked."),
    (r"\bchmod\s+-R?\s*777\b", "chmod 777 is not allowed."),
]
for pattern, reason in rules:
    if re.search(pattern, cmd):
        print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse",
                                                 "permissionDecision": "deny",
                                                 "permissionDecisionReason": reason}}))
        sys.exit(0)
sys.exit(0)
