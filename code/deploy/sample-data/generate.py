#!/usr/bin/env python3
"""Generates a bank statement (CSV) and a ledger export (fixed-width) for testing reconArk.

Usage:
  python3 generate.py                 # the small, hand-checked sample: bank.csv + ledger.txt (see README.md)
  python3 generate.py 1000000         # 1 million transactions for performance tests: bank-1000000.csv + ledger-1000000.txt

In generated files about 2% of transactions differ in amount, 1% exist only in the bank file, 1% only in the ledger,
and 0.1% of bank rows have no reference (they are rejected). The script prints the expected counts.
"""
import random
import sys

BANK_HEADER = "REF,VALUE_DATE,AMT,CCY,DESCRIPTION\n"
LEDGER_HEADER = "LEDGER EXPORT\n"


def ledger_line(ref, yyyymmdd, amount, currency):
    # reference:1:12  valueDate:13:8  amount:21:12  currency:33:3
    return f"{ref:<12}{yyyymmdd:<8}{amount:012.2f}{currency:<3}\n"


def small():
    bank = [
        ("TXN-0001", "2026-10-01", 1500.00, "AED", "Salary"),
        ("TXN-0002", "2026-10-01", 250.50, "AED", "Card payment"),
        ("TXN-0003", "2026-10-02", 99.99, "AED", "Utility, October"),
        ("TXN-0004", "2026-10-02", 1200.00, "USD", "Transfer"),
        ("TXN-0005", "2026-10-03", 75.00, "AED", "Fee"),
        ("TXN-0006", "2026-10-03", 3000.00, "AED", "Rent"),
        ("TXN-0007", "2026-10-04", 450.00, "AED", "Supplier"),
        ("TXN-0008", "2026-10-04", 60.00, "EUR", "Subscription"),
        ("TXN-0009", "2026-10-05", 820.00, "AED", "Refund"),
        ("", "2026-10-05", 10.00, "AED", "Row without reference"),
    ]
    ledger = [
        ("TXN-0001", "20261001", 1500.00, "AED"),   # matched
        ("TXN-0002", "20261002", 250.50, "AED"),    # matched: date 1 day later is within tolerance
        ("TXN-0003", "20261002", 99.99, "AED"),     # matched
        ("TXN-0004", "20261002", 1200.00, "USD"),   # matched
        ("TXN-0005", "20261003", 75.00, "AED"),     # matched
        ("TXN-0006", "20261003", 3100.00, "AED"),   # MISMATCHED amount
        ("TXN-0007", "20261008", 450.00, "AED"),    # MISMATCHED value date
        ("TXN-0008", "20261004", 60.00, "AED"),     # MISMATCHED currency
        ("TXN-0010", "20261005", 330.00, "AED"),    # ONLY IN RIGHT (ledger)
    ]
    with open("bank.csv", "w") as f:
        f.write(BANK_HEADER)
        for ref, date, amount, ccy, text in bank:
            f.write(f'{ref},{date},{amount:.2f},{ccy},"{text}"\n')
    with open("ledger.txt", "w") as f:
        f.write(LEDGER_HEADER)
        for ref, date, amount, ccy in ledger:
            f.write(ledger_line(ref, date, amount, ccy))
    print("Wrote bank.csv and ledger.txt. Expected: matched 5, mismatched 3, only in left 1, only in right 1, rejected 1")


def large(n):
    rng = random.Random(42)
    counts = {"matched": 0, "mismatched": 0, "only_left": 0, "only_right": 0, "rejected": 0}
    with open(f"bank-{n}.csv", "w") as bank, open(f"ledger-{n}.txt", "w") as ledger:
        bank.write(BANK_HEADER)
        ledger.write(LEDGER_HEADER)
        for i in range(1, n + 1):
            ref = f"T{i:011d}"
            day = 1 + i % 28
            amount = round(rng.uniform(1, 50_000), 2)
            roll = rng.random()
            if roll < 0.001:
                bank.write(f",2026-10-{day:02d},{amount:.2f},AED,no reference\n")
                counts["rejected"] += 1
                continue
            if roll < 0.011:
                bank.write(f"{ref},2026-10-{day:02d},{amount:.2f},AED,only bank\n")
                counts["only_left"] += 1
                continue
            if roll < 0.021:
                ledger.write(ledger_line(ref, f"202610{day:02d}", amount, "AED"))
                counts["only_right"] += 1
                continue
            bank.write(f"{ref},2026-10-{day:02d},{amount:.2f},AED,payment\n")
            if roll < 0.041:
                ledger.write(ledger_line(ref, f"202610{day:02d}", amount + 1, "AED"))
                counts["mismatched"] += 1
            else:
                ledger.write(ledger_line(ref, f"202610{day:02d}", amount, "AED"))
                counts["matched"] += 1
    print(f"Wrote bank-{n}.csv and ledger-{n}.txt. Expected: {counts}")


if __name__ == "__main__":
    if len(sys.argv) > 1:
        large(int(sys.argv[1]))
    else:
        small()
