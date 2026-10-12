#!/usr/bin/env python3
"""PreToolUse guard for file edits: protects accepted decision records, lockfiles, the Gradle wrapper and secrets."""
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
    deny("Secret files (.env) must not be created or edited. Use Kubernetes Secrets / the secret manager.")
if re.search(r"\.(pem|key|p12|jks|pfx)$", name) or (name.endswith(".tfvars") and ".example" not in name):
    deny("Key, keystore and tfvars files must never be written to the repository.")
if rel.startswith("code/gradle/wrapper/"):
    deny("Do not hand-edit the Gradle wrapper; run ./gradlew wrapper --gradle-version <v> after approval.")
if name == "package-lock.json":
    deny("Lockfiles are generated. Run npm install/ci instead of editing package-lock.json.")
if re.match(r"^docs/decisions/\d{4}-.*\.md$", rel) and os.path.exists(os.path.join(root, rel)):
    with open(os.path.join(root, rel), encoding="utf-8") as f:
        head = f.read(1000)
    if re.search(r"\|\s*Status\s*\|\s*Accepted", head):
        deny("Accepted decision records don't change. Write a new one that replaces it (/new-adr); "
             "ask the user before changing the old record's Status line.")
sys.exit(0)
