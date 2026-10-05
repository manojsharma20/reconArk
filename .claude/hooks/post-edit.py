#!/usr/bin/env python3
"""PostToolUse for Edit/Write: format frontend files, scan the edited file for secrets and legacy names.
Exit code 2 sends stderr back to Claude so it fixes the problem immediately."""
import json, os, re, subprocess, sys

data = json.load(sys.stdin)
path = (data.get("tool_input") or {}).get("file_path") or ""
root = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
if not path or not os.path.isfile(path):
    sys.exit(0)
rel = os.path.relpath(os.path.abspath(path), root)

# 1. Prettier for frontend sources (only when dependencies are installed)
if rel.startswith("frontend/") and re.search(r"\.(ts|tsx|css|json|md|html)$", rel) and "node_modules" not in rel:
    prettier = os.path.join(root, "frontend", "node_modules", ".bin", "prettier")
    if os.path.exists(prettier):
        subprocess.run([prettier, "--write", "--log-level", "warn", path], cwd=os.path.join(root, "frontend"), check=False)

try:
    text = open(path, encoding="utf-8", errors="ignore").read()
except OSError:
    sys.exit(0)

problems = []
secret_patterns = {
    "AWS access key": r"\bAKIA[0-9A-Z]{16}\b",
    "private key": r"-----BEGIN (RSA |EC |OPENSSH |PGP )?PRIVATE KEY",
    "GitHub token": r"\bgh[pousr]_[A-Za-z0-9]{36,}\b",
    "Slack token": r"\bxox[baprs]-[A-Za-z0-9-]{10,}",
    "hard-coded password": r"(?i)(password|passwd|secret)\s*[:=]\s*['\"][^'\"\s${}]{8,}['\"]",
}
if not rel.startswith(".claude/hooks/"):
    for label, pat in secret_patterns.items():
        if re.search(pat, text):
            problems.append(f"possible {label} in {rel}: store values in the secret manager and reference them by name")

denylist = os.path.join(root, "scripts", "naming-denylist.txt")
if os.path.exists(denylist) and not rel.startswith(("docs/reference/", "scripts/")):
    for line in open(denylist, encoding="utf-8"):
        term = line.split("#", 1)[0].strip()
        if term and re.search(r"\b" + re.escape(term) + r"\b", text):
            problems.append(f"legacy identifier '{term}' in {rel} (clean-room naming, ADR-0025)")

if problems:
    print("reconArk post-edit check failed:\n- " + "\n- ".join(problems), file=sys.stderr)
    sys.exit(2)
sys.exit(0)
