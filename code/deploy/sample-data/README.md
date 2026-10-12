# Sample data

| File | What |
|---|---|
| `bank.csv` | Bank statement, CSV, 10 rows (left side) |
| `ledger.txt` | General ledger export, fixed-width, 9 rows (right side) |
| `definition.json` | The recon definition that reconciles them (POST it to `/api/definitions`) |
| `generate.py` | Re-creates the two files, or creates big files for performance tests: `python3 generate.py 1000000` |

## Expected outcome

| Reference | Bank | Ledger | Result | Why |
|---|---|---|---|---|
| TXN-0001 | 1500.00 AED 2026-10-01 | same | MATCHED | |
| TXN-0002 | 250.50 AED 2026-10-01 | date 2026-10-02 | MATCHED | 1 day is within `toleranceDays=1` |
| TXN-0003, -0004, -0005 | | same | MATCHED | |
| TXN-0006 | 3000.00 | 3100.00 | MISMATCHED | amount |
| TXN-0007 | 2026-10-04 | 2026-10-08 | MISMATCHED | valueDate (4 days) |
| TXN-0008 | EUR | AED | MISMATCHED | currency |
| TXN-0009 | 820.00 | — | ONLY_IN_LEFT | missing in the ledger |
| TXN-0010 | — | 330.00 | ONLY_IN_RIGHT | missing in the bank statement |
| (blank) | 10.00 | — | rejected | no reference, so no match key |

Totals: matched 5 · mismatched 3 · only in left 1 · only in right 1 · rejected 1 · loaded 9 + 9.
`../smoke-test.sh` checks exactly these numbers.
