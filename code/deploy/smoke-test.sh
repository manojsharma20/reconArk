#!/usr/bin/env bash
# End-to-end check against a running reconArk (docker compose --profile app, or services started from the IDE).
# Creates the sample definition, uploads the sample files, runs a reconciliation and checks every count.
# Needs: curl, python3.   Usage: deploy/smoke-test.sh [base-url]   (default http://localhost:8080 = the UI's nginx)
set -euo pipefail
BASE=${1:-http://localhost:8080}
DIR="$(cd "$(dirname "$0")/sample-data" && pwd)"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}
json() { python3 -c "import json,sys; print(json.load(sys.stdin)$1)"; }

# call METHOD PATH [curl options...]: prints the response body; fails with the body when the HTTP status is >= 400.
call() {
  local method=$1 path=$2 out code body
  shift 2
  out=$(curl -sS -X "$method" "$BASE$path" -w $'\n%{http_code}' "$@") || fail "$method $path: no answer"
  code=${out##*$'\n'}
  body=${out%$'\n'*}
  [ "$code" -lt 400 ] || fail "$method $path -> HTTP $code: $body"
  printf '%s' "$body"
}

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
DEF_ID=$(call POST /api/definitions -H 'Content-Type: application/json' -d "$definition" | json "['id']")
echo "   definition $DEF_ID"

echo "2. Upload the two files"
LEFT=$(call POST /api/files -F "file=@$DIR/bank.csv" -F definitionId="$DEF_ID" -F side=LEFT | json "['id']")
RIGHT=$(call POST /api/files -F "file=@$DIR/ledger.txt" -F definitionId="$DEF_ID" -F side=RIGHT | json "['id']")
echo "   files $LEFT (bank) and $RIGHT (ledger)"

echo "3. Start the run"
RUN=$(call POST /api/runs -H 'Content-Type: application/json' \
  -d "{\"definitionId\":\"$DEF_ID\",\"leftFileId\":\"$LEFT\",\"rightFileId\":\"$RIGHT\"}" | json "['id']")
echo "   run $RUN"

echo "4. Wait for it to finish"
STATUS=""
for i in $(seq 1 90); do
  STATUS=$(call GET "/api/runs/$RUN" | json "['status']")
  if [ "$STATUS" = COMPLETED ] || [ "$STATUS" = FAILED ]; then break; fi
  sleep 2
done
echo "   status $STATUS"
[ "$STATUS" = COMPLETED ] || fail "run ended as $STATUS: $(call GET "/api/runs/$RUN"); jobs: $(call GET "/api/runs/$RUN/jobs")"

echo "5. Check the counts"
SUMMARY=$(call GET "/api/reports/runs/$RUN/summary")
CHECK=$(python3 - "$SUMMARY" <<'PY'
import json, sys
s = json.loads(sys.argv[1])
expected = {"matched": 5, "mismatched": 3, "onlyInLeft": 1, "onlyInRight": 1, "rejected": 1, "leftLoaded": 9, "rightLoaded": 9}
wrong = {k: (s[k], v) for k, v in expected.items() if s[k] != v}
print(f"wrong counts (actual, expected): {wrong}" if wrong else
      f"counts OK: { {k: s[k] for k in expected} }, match rate {s['matchRatePercent']} %")
PY
)
case "$CHECK" in counts*) echo "   $CHECK" ;; *) fail "$CHECK" ;; esac

echo "6. Download the mismatches as CSV"
LINES=$(call GET "/api/reports/runs/$RUN/export?format=csv&status=MISMATCHED" | grep -c .)
[ "$LINES" = 4 ] || fail "expected 4 lines (header + 3 mismatches), got $LINES"
echo "   CSV OK ($LINES lines)"

echo "PASS: reconArk works end to end."
