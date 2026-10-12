# 0005: Split matching into buckets by match key

| Status | Accepted |
|---|---|
| Date | 2026-10-12 |

## Context

Matching must bring the left and right records with the same key together, without one machine holding all records.

## Decision

Every record gets `bucket = floorMod(matchKey.hashCode(), bucketCount)` when loaded. One MATCH job per bucket matches it independently. `bucketCount` is configurable (default 16).

## Consequences

- The same key is always in the same bucket on both sides, on every machine.
- Memory per job ≈ 2 × rows ÷ buckets: choose buckets by volume.
- Uneven key distributions can make some buckets bigger (acceptable for transaction references).

## Alternatives considered

- Sorting both files and merge-joining: one big sequential job, hard to parallelise.
- Matching in SQL (`FULL OUTER JOIN`): fast for exact matches, but tolerances and plugins are harder.
