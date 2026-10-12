#!/usr/bin/env bash
# End-to-end check against a running reconArk (docker compose --profile app, or services started from the IDE).
# Creates the sample definition, uploads the sample files, runs a reconciliation and checks every count.
# Needs: curl, python3.   Usage: deploy/smoke-test.sh [base-url]   (default http://localhost:8080 = the UI's nginx)
set -euo pipefail
BASE=${1:-http://localhost:8080}
DIR="$(cd "$(dirname "$0")/sample-data" && pwd)"
json() { python3 -c "import json,sys; print(json.load(sys.stdin)$1)"; }
fail() { echo "FAIL: $*" >&2; exit 1; }

echo "0. Wait until every service answers"
for path in /api/plugins /api/files /api/runs /api/reports/formats; do
  for i in $(seq 1 60); do
    curl -fsS -o /dev/null "$BASE$path" 2>/dev/null && break
    [ "$i" = 60 ] && fail "$BASE$path does not answer"
    sleep 2
  done
done

echo "1. Create the definition"
definition=$(python3 - "$DIR/definition.json" <<'PY'
import json, sys, time
d = json.load(open(sys.argv[1])); d["name"] += f" (smoke test {int(time.time())})"; print(json.dumps(d))
PY
)
DEF_ID=$(curl -fsS -X POST "$BASE/api/definitions" -H 'Content-Type: application/json' -d "$definition" | json "['id']")
echo "   definition $DEF_ID"

echo "2. Upload the two files"
LEFT=$(curl -fsS -F "file=@$DIR/bank.csv" -F definitionId="$DEF_ID" -F side=LEFT "$BASE/api/files" | json "['id']")
RIGHT=$(curl -fsS -F "file=@$DIR/ledger.txt" -F definitionId="$DEF_ID" -F side=RIGHT "$BASE/api/files" | json "['id']")
echo "   files $LEFT (bank) and $RIGHT (ledger)"

echo "3. Start the run"
RUN=$(curl -fsS -X POST "$BASE/api/runs" -H 'Content-Type: application/json' \
  -d "{\"definitionId\":\"$DEF_ID\",\"leftFileId\":\"$LEFT\",\"rightFileId\":\"$RIGHT\"}" | json "['id']")
echo "   run $RUN"

echo "4. Wait for it to finish"
for i in $(seq 1 60); do
  STATUS=$(curl -fsS "$BASE/api/runs/$RUN" | json "['status']")
  [ "$STATUS" = COMPLETED ] || [ "$STATUS" = FAILED ] && break
  sleep 2
done
echo "   status $STATUS"
[ "$STATUS" = COMPLETED ] || fail "run ended as $STATUS: $(curl -fsS "$BASE/api/runs/$RUN" | json "['error']")"

echo "5. Check the counts"
SUMMARY=$(curl -fsS "$BASE/api/reports/runs/$RUN/summary")
python3 - "$SUMMARY" <<'PY'
import json, sys
s = json.loads(sys.argv[1])
expected = {"matched": 5, "mismatched": 3, "onlyInLeft": 1, "onlyInRight": 1, "rejected": 1, "leftLoaded": 9, "rightLoaded": 9}
wrong = {k: (s[k], v) for k, v in expected.items() if s[k] != v}
if wrong:
    sys.exit(f"FAIL: (actual, expected) {wrong}")
print("   counts OK:", {k: s[k] for k in expected}, "match rate", s["matchRatePercent"], "%")
PY

echo "6. Download the mismatches as CSV"
LINES=$(curl -fsS "$BASE/api/reports/runs/$RUN/export?format=csv&status=MISMATCHED" | wc -l | tr -d ' ')
[ "$LINES" = 4 ] || fail "expected 4 lines (header + 3 mismatches), got $LINES"
echo "   CSV OK ($LINES lines)"

echo "PASS: reconArk works end to end."
