#!/usr/bin/env python3
"""PreToolUse guard for file edits: protects accepted ADRs, baseline docs, lockfiles, the Gradle wrapper and secrets."""
import json, os, re, sys

data = json.load(sys.stdin)
path = (data.get("tool_input") or {}).get("file_path") or (data.get("tool_input") or {}).get("notebook_path") or ""
root = os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd())
rel = os.path.relpath(os.path.abspath(path), root) if path else ""

def deny(reason):
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse",
                                             "permissionDecision": "deny",
                                             "permissionDecisionReason": reason}}))
    sys.exit(0)

name = os.path.basename(rel)
if re.match(r"^\.env(\..*)?$", name) and not name.endswith(".example"):
    deny("Secret files (.env) must not be created or edited. Use secret-manager reference names (CLAUDE.md §1.9).")
if re.search(r"\.(pem|key|p12|jks|pfx)$", name) or (name.endswith(".tfvars") and ".example" not in name):
    deny("Key, keystore and tfvars files must never be written to the repository.")
if rel.startswith("docs/reference/") or rel == "docs/README-v0.1.md":
    deny("The v0.1 baseline documents are read-only history. Record changes in HLD/LLD and a new ADR.")
if rel.startswith("gradle/wrapper/"):
    deny("Do not hand-edit the Gradle wrapper; run ./gradlew wrapper --gradle-version <v> after an ADR/approval.")
if name in ("package-lock.json",):
    deny("Lockfiles are generated. Run npm install/ci instead of editing package-lock.json.")
m = re.match(r"^docs/adr/ADR-\d{4}-.*\.md$", rel)
if m and os.path.exists(os.path.join(root, rel)):
    with open(os.path.join(root, rel), encoding="utf-8") as f:
        head = f.read(2000)
    if re.search(r"\|\s*Status\s*\|\s*Accepted", head):
        deny("Accepted ADRs are immutable. Write a new ADR that supersedes it (/new-adr), "
             "then only the superseded record's Status line may change — ask the user to approve that edit.")
sys.exit(0)
