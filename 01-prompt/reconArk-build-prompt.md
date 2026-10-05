# reconArk — Build Prompt

## 1. Your role

You are the principal architect and lead engineer for **reconArk**, a new, standalone platform that does
two things only: **ETL** (ingest partner and internal transaction data from any channel into one canonical
form) and **Reconciliation** (match and compare that data, side against side, explain every difference,
and report on it). There is no ledger posting, no posting file, no settlement and no payout in reconArk; those
stay in the systems that consume reconArk's results.

You design first, then build. You do not write code until the deliverables in section 21, steps 1–3, are
reviewed. When a requirement here is ambiguous, you state the ambiguity, pick the safest reasonable
default, record it as an ADR, and move on; you never silently guess.

Three things are non-negotiable and are checked by CI on every merge:
1. The data model in section 11.
2. Near-zero duplication (section 15).
3. Zero open security findings: no vulnerabilities, no unreviewed hotspots, nothing a VAPT would flag
   (sections 18–19).
4. Concurrent runs isolated from each other, from a few hundred to 100 million+ records, and every ETL or
   recon run finished within 40 minutes (section 9).
5. Latest stable technology on Java 25 LTS, a Gradle build written from scratch, and virtual threads where
   they help (section 16).
6. ACID transactions with deliberate isolation levels (section 12), writes on the primary and every
   other read on replicas (9.5), and one portable design deployable on AWS, Azure or GCP (section 17).

## 2. Where this comes from — learn from it, inherit nothing

reconArk replaces the ETL and reconciliation parts of an existing loyalty reconciliation and settlement
platform. You inherit its domain knowledge and its lessons — **not its names**. Section 2.4 states the
rule.

### 2.1 Legacy ETL — the behaviour worth keeping
- Sources are registered as data (code, name, file or API, secret reference, active flag). Each has a
  schema (delimiter, header, validation mode) and field mappings: source field → target field, data
  type, required flag, default, format pattern, transformation expression.
- Field rules are reusable expressions. Each has a human-readable failure template with `{field}` and
  `{value}`; sensitive values are masked to their last four characters.
- Bad rows are rejected with a stage, an error code and a per-field explanation naming every failing
  field, its value and why. The job continues; one bad row never fails a job.
- Files can be PGP-encrypted. Decryption supports RSA, ECDH (Curve25519, NIST, brainpool) and ElGamal;
  AES, 3DES, CAST5, Blowfish, Twofish, Camellia and IDEA; ZIP, ZLIB and BZIP2; armored or binary; signed
  and encrypted; hidden recipient; multiple recipients. Plain or truncated files where encryption is
  expected are rejected with a clear reason. Content is sniffed (PDF, ZIP/Office, image, binary,
  non-UTF-8) before parsing, whatever the extension says.
- Job requests pass through a dispatcher. It computes an idempotency key over the normalized request,
  records duplicates, queues requests whose dependencies are still running, and uses conditional state
  transitions so two pods can't both launch the same request.

### 2.2 Legacy recon — the behaviour worth keeping, the design to replace
- Comparison logic is hard-coded in one class per provider family. The fields compared: internal vs
  partner transaction identifier; card amount vs sales amount; total amount; payment date (same calendar
  day in the business timezone); partner status; initiator channel; source and target points.
- Rules: case-insensitive string equality; money rounded to 2 decimal places, half-up; dates compared in
  a timezone; partner status checked against an allowed "success" set.
- Outcome families: match, mismatch, duplicate variants of both, matched-pending-success, failure — each
  with numbered reason codes stored as JSON on the recon row.
- Transactions still pending after a configured number of days become a pending failure.
- Partner lookups are cached per JVM with a cache-warming step; per-partition work lists are held in an
  external cache.

### 2.3 What went wrong, and what reconArk must make impossible
Each item below was a real production-path defect. Design so that each one cannot happen, and prove it
with a test.
1. **Provider logic in code.** A new provider or a new compared field needed a new class and a release.
   → In reconArk, onboarding a provider is configuration only.
2. **Copies drifting apart.** Duplicated per-provider logic diverged (cache keys written and read
   differently; an override silently dropped the pending-failure and offline branches for two
   providers). → One rule engine, no per-provider subclasses.
3. **Wide provider-specific tables.** The partner and recon tables grew a column per provider quirk.
   → Canonical core columns plus typed, indexed extension attributes.
4. **Recon status mixed with downstream lifecycle.** Posting states lived inside the recon status set.
   → reconArk owns recon outcomes only; downstream state belongs downstream.
5. **Partition state that never reached the workers.** Records were loaded into the manager step's
   reader, and every worker got an empty one; the job read 0 rows, threw in a listener and still
   reported success. → Work units carry their own input reference; "completed" requires reconciled
   counts (read = written + rejected + skipped), enforced by a table constraint.
6. **Two concurrent runs claiming the same rows,** and one run's failure revert releasing another run's
   rows. → Every claim is owned by a run id, and every release is scoped to that owner.
7. **A crashed pod blocking everything.** Its execution stayed "running" forever and blocked the daily
   report and the next run of the same scope. → Liveness via heartbeat with a stale threshold; orphaned
   work is detected and re-queued automatically.
8. **Rows finalized as done that were never processed** (claimed, left out of the output, then marked
   complete). → Finalize only what was demonstrably processed; release everything else.
9. **Idempotency key drift.** The key was computed from a timezone-shifted payload, causing duplicate
   launches and relaunch loops. → Canonical, timezone-normalized key material, with a test.
10. **Migrations that would lock a 167 GB table.** → Every schema change is online: metadata-only DDL,
    `CREATE INDEX CONCURRENTLY`, `NOT VALID` constraints validated later, batched backfills with
    `lock_timeout`, dry-run on a copy of production-sized data.
11. **Copy-pasted logic across classes** (one cache lookup duplicated four times; two decryptors in
    parallel versions that behaved differently). → Section 15.
12. **Security gaps that only showed up late:** an unchecked cast on attacker-controlled input crashed
    decryption; expression rules ran unsandboxed; secrets and keys sat beside configuration. → Sections
    18–19.

### 2.4 Inherit nothing by name
reconArk is a clean-room design. Do not reuse any identifier from the legacy platform. That covers:
- package and module names;
- class, interface and enum names, and enum values;
- table and column names;
- configuration property keys, queue and topic names;
- job and step names;
- reason and error codes, and status values.

Sections 2.1–2.3 describe behaviour only. reconArk defines its own names under section 16.4. If a
downstream consumer still needs legacy values during the transition, translate them in an outbound
adapter at the integration boundary. Legacy values never enter reconArk's model, database or APIs.

## 3. Scope

**In scope**
- Provider onboarding: registry, connectors, formats, field mapping, validation and recon rules, all
  versioned configuration with maker-checker.
- ETL: acquire → verify → decrypt → sniff → parse → map → validate → enrich → canonicalize → persist,
  with reject-and-continue.
- Reconciliation: pair records across two (or more) sides, compare configured fields, classify, explain,
  age and re-reconcile as late data arrives.
- Reports: recon summaries, mismatch and exception reports, ageing, ingestion quality; on demand and
  scheduled; API and secure export.
- Ingestion and query APIs over REST, gRPC and SOAP; file drops over SFTP and object storage.
- Operations: messaging, scheduling, distribution, recovery, monitoring, logging, alerting, audit.

**Out of scope:** posting, GL files, settlement, payouts and notifications to end customers. reconArk
publishes recon results as events and APIs; other systems act on them.

## 4. Non-functional requirements

Give each one a measurable target in the design and a test or a dashboard that proves it.

| Area | Requirement |
|---|---|
| Distribution | Stateless services, horizontally scalable on Kubernetes. Any instance can take any work unit; no work assumes a particular pod or shared local disk. |
| Fault tolerance | Losing any single pod, broker node or DB replica loses no data and causes no duplicate result. Processing is at-least-once with idempotent consumers, which gives effectively-once outcomes. |
| Self-healing | Detect stuck, orphaned or poisoned work automatically; reclaim and retry with backoff; quarantine poison messages to a dead-letter queue with full context; never require a manual DB update to unblock a day. |
| Resilience | Timeouts, retries with jitter, circuit breakers and bulkheads on every external call (partner APIs, secret manager, object storage, broker). One slow or failing provider never degrades another. |
| Auto-recovery | After a crash or redeploy, every in-flight run resumes or rolls back to a consistent state on its own. Define the recovery path for each pipeline stage. |
| Isolation | Many ETL and recon runs execute at the same time and never disturb each other: no shared mutable state, no cross-run writes, no starvation, no lock contention (section 9). |
| Scale | From a few hundred to 100 million+ records per run, with one code path. Throughput grows linearly with workers (section 9). |
| Run time | Every ETL run and every recon run finishes end to end within **40 minutes** at the largest supported volume; small runs within 2 minutes (section 9). |
| Latency | Real-time sources (API/MQ push) reconciled within 60 s of both sides being present. |
| Correctness | Money as `NUMERIC(19,4)`/`BigDecimal`, never floating point; explicit rounding mode per rule; every timestamp stored as `TIMESTAMPTZ`, with the business timezone explicit per provider. |
| Auditability | Every record traceable from raw input (file, offset or line, or request id) to canonical row to recon outcome to report, with the configuration version used at each step. |
| Security | Sections 18 and 19. |

## 5. Messaging — one bus, many brokers

- Define one port, `MessageBus`, with publish, consume, acknowledge, negative-acknowledge with delay,
  dead-letter, and partition/ordering key. All application code depends on the port, never on a broker
  client.
- Ship adapters for **Kafka**, **RabbitMQ** and **ActiveMQ (Artemis)**, and keep the port small enough that
  others (Amazon SQS, Azure Service Bus, Pulsar) are a new adapter, not a refactor.
- **Exactly one broker is active.** It is chosen by `reconark.messaging.broker=kafka|rabbitmq|activemq`.
  Startup fails fast if none, more than one, or an unknown broker is configured, or if the active
  broker's required settings are missing.
- Define delivery semantics once and make every adapter meet them:
  - at-least-once delivery;
  - ordering per key (provider + business key);
  - bounded redelivery, then dead-letter;
  - consumer idempotency by message id.
- Use a **transactional outbox** for every event that follows a DB write, so state and events never
  disagree.
- Shared behaviour (envelope, headers, trace propagation, retry policy, DLQ routing, idempotency check,
  metrics) lives once in the port's support layer. Adapters contain only broker-specific translation.
- One contract test suite runs unchanged against every adapter (Testcontainers per broker) and covers:
  ordering, redelivery, DLQ, duplicates, broker restart mid-stream, and consumer crash before ack.
- Broker connections use TLS and per-service credentials from the secret manager. Each topic or queue has
  its own ACLs; no service gets wildcard access.
- Topics and queues are configuration, named per section 16.4: `reconark.ingestion.artifact-received.v1`,
  `reconark.ingestion.chunk-ready.v1`, `reconark.ingestion.record-canonicalized.v1`,
  `reconark.recon.bucket-ready.v1`, `reconark.recon.outcome-changed.v1`, `reconark.recon.exception-raised.v1`,
  `reconark.reporting.report-requested.v1`. Each has its own `.dlq` and retry policy.

## 6. Provider onboarding — configuration, not code

A provider is a versioned configuration (section 11.2), plus secret references. Every save is validated in
full; bad configuration fails fast at onboarding time, unlike bad data. Approved versions are immutable.
A version is activated with an effective date, and only after a maker-checker approval by a different
person. Before activation it can be dry-run against sample data.

### 6.1 Secrets
- The properties file holds **keys only**: the secret-manager reference names
  (e.g. `reconark.providers.acquirer-a.secret-ref=prod/reconark/acquirer-a`). The database holds the same
  reference names, never values.
- Values (PGP private key and passphrase, API client secrets, mTLS keys, SFTP keys, basic credentials)
  live only in the secret manager: AWS Secrets Manager first, behind a `SecretProvider` port so Vault and
  Azure Key Vault are adapters.
- Secrets are cached with a TTL and refreshed on rotation without a restart. They are held as `char[]` or
  `byte[]` and zeroed after use where the library allows.
- Missing or invalid secrets fail the run, not the pod. A secret value never appears in logs, exceptions,
  metrics, traces, heap-dump-friendly `toString()` output or API responses.

### 6.2 Connectors (how data arrives)
Each is a plugin behind a `SourceConnector` port. Retry, timeout, pagination, checkpointing,
metrics and SSRF protection live once in a shared connector base; a connector only adds what is specific
to its protocol.
- **File:**
  - pull over SFTP (host-key pinning required);
  - pull from object storage (S3/Blob/GCS);
  - push via object-storage event or upload API.

  Completeness markers (trailer row, control file, record count, checksum) are configurable.
- **REST:** pull, with paging (offset, cursor, link header, time window); push via webhook with HMAC
  signature, timestamp and nonce, or mTLS.
- **SOAP:** WSDL-driven client, WS-Security as configured, hardened XML parsing (section 18).
- **gRPC:** unary and server streaming, deadline per call, TLS/mTLS.
- **Message queue:** consume a partner's topic or queue through the active `MessageBus`.

Pull connectors keep a durable checkpoint (last cursor, last window, last file) so a restart resumes
exactly where it stopped, and a provider can be replayed for a date range on demand.

### 6.3 Formats
- **Delimited:** any delimiter, quoted fields, header by name or by position, BOM-tolerant.
- **Other formats:** fixed-width, Excel, XML, JSON, JSON Lines, Avro, Protobuf.
- **Encoding:** configurable; content sniffing as in 2.1.

There is one parser framework, with a small format-specific reader per format. Mapping, validation,
rejection and metrics are never reimplemented per format.

### 6.4 Field mapping — including fields deep inside nested lists
Mapping must handle payloads where the transactions are a list buried inside an object, and the fields to
reconcile are nested several levels inside each transaction or inside its own sub-lists.

Configuration has two levels:
1. **Record root** — a path expression that selects each record from the payload, e.g.
   `$.data.settlement.batches[*].transactions[*]`. One payload can therefore yield many records.
2. **Field paths** — relative to the record, using a JSONPath-compatible syntax (for XML, the same model
   over XPath):
   - nested objects: `payment.amount.value`;
   - list filters: `parties[?(@.role=='PAYER')].account.iban`;
   - parent context: `$parent.batchId`, `$root.header.fileDate`.

Every field mapping declares:
- target canonical field;
- data type;
- format pattern (date, number, locale);
- required flag;
- default value;
- transformation expression (sandboxed, section 18);
- sensitivity (mask in logs, errors, reports and APIs);
- **cardinality**: the path must yield exactly one value, else the record is rejected with a precise
  reason — or, where the mapping allows it, the first value, all values joined, or the sum.

Paths are compiled and validated when the configuration is saved, never interpreted from raw strings at
run time.

Example to support out of the box:

```json
{
  "header": { "partnerId": "P-17", "fileDate": "2026-10-01" },
  "data": { "batches": [ { "batchId": "B1", "transactions": [ {
      "ref": "TX-9", "status": { "code": "00" },
      "payment": { "amount": { "value": "125.50", "currency": "AED" } },
      "parties": [ { "role": "PAYER", "account": { "iban": "AE07…1234" } } ] } ] } ] }
}
```
Record root `$.data.batches[*].transactions[*]` gives one record per transaction, with these fields:
- `partnerRef = ref`
- `amount = payment.amount.value`
- `currency = payment.amount.currency`
- `statusCode = status.code`
- `payerIban = parties[?(@.role=='PAYER')].account.iban` (sensitive)
- `batchId = $parent.batchId`
- `fileDate = $root.header.fileDate`

### 6.5 Validation
Keep and extend the legacy model (2.1):
- Rules are reusable, named expressions with a human-readable failure template: `{field}`, `{value}`
  (masked if sensitive) and `{rule}`.
- A rule can apply at field level, cross-field within a record, or at file level (row count vs trailer,
  sum vs control total).
- All failures for a record are collected, not just the first. A field that failed is not re-checked by
  a rule that depends on it.

Outcome per record: accepted, rejected (to the reject store, with every failing field and reason), or
quarantined (when the source itself is suspect, e.g. a checksum mismatch: hold the whole file and alert).

### 6.6 Recon rules — configured per provider pair
- **Sides:** which two sources are reconciled (e.g. internal `PAYMENTS_CORE` vs `ACQUIRER_A`). The internal
  side is just another onboarded source, not a special case.
- **Match keys:** one or more canonical fields per side, each with a normalization (trim, case, strip
  leading zeros, remove separators), plus an optional fallback key chain (e.g. auth code + date, then
  RRN + amount).
- **Cardinality:**
  - 1:1, 1:N and N:1, with aggregation (sum of amounts, count) for the N side;
  - duplicate-key policy: flag both, take the latest, or take the first.
- **Compared fields:** an ordered list. Each entry names the field on each side, a comparator and a
  severity:
  - Comparators: exact, case-insensitive, numeric with absolute or percentage tolerance and rounding,
    date/time with a window in a given timezone (or same business day), value mapping (partner `"00"` ≡
    internal `SUCCESS`), regex (linear-time engine), and expression.
  - Each comparator is one strategy class in one registry; nothing compares fields outside it.
  - Severity: blocking mismatch, or warning that still matches.
- **Status semantics:** which partner statuses count as success, pending or failed, and the offline-recon
  flag.
- **Time:**
  - matching window (how long to wait for the other side);
  - ageing buckets;
  - T+N rule that turns a long-pending item into a failure (configurable per rule set).
- **Reason codes:** reconArk's own catalogue (section 16.4), configured with stable codes and message
  templates.

## 7. ETL pipeline

1. **Acquire** — the connector produces an immutable raw artifact, stored once (encrypted at rest) with
   its checksum and provenance. The same artifact received twice is detected (content hash + source +
   business date) and not reprocessed unless explicitly replayed.
2. **Verify** — completeness markers, checksum, signature if configured; size and decompression-ratio
   limits (section 18).
3. **Decrypt** — PGP per 2.1. Failures classify as wrong key, not encrypted, truncated, or unsupported
   (RFC 9580 v6 must either be supported or rejected cleanly, never written as garbage).
4. **Sniff and parse** — streaming, with constant memory regardless of file size.
5. **Map, validate, enrich** — per section 6, with lookup enrichment through a distributed cache that has
   an explicit invalidation story.
6. **Persist** — canonical rows are written idempotently, keyed by source + business key + business date +
   record version. Rejects get a pointer to the record's location, a masked excerpt, and every failure.

Work is split into chunks (byte or line ranges, or record-root slices). Each chunk is a message on the bus
carrying everything a worker needs: artifact reference, range, config version and run id. Any worker can
take any chunk.

A run is complete only when:
- every chunk is acknowledged; and
- `read = accepted + rejected + quarantined` holds per chunk and per run.

Otherwise the run is flagged, not marked done.

## 8. Reconciliation engine

- **One generic, config-driven engine.** No provider subclasses.
- **Two modes, one rule set:**
  - **Streaming (real time):** each canonical record arrival triggers a lookup of its counterpart by match
    key.
  - **Batch (scheduled or on demand):** full reconciliation of a provider pair for a business date.

  Both modes share the same rule evaluation code and must produce identical outcomes for the same data;
  prove it with a property-based test.
- **Outcomes:**
  - `MATCHED`;
  - `MATCHED_WITH_WARNINGS`;
  - `MISMATCHED` (per-field diffs: field, internal value, partner value, comparator, tolerance used);
  - `UNMATCHED_INTERNAL` (missing at the partner);
  - `UNMATCHED_PARTNER` (missing internally);
  - `DUPLICATE`;
  - `PENDING` (inside the matching window);
  - `FAILED_PENDING` (T+N exceeded).

  Downstream consumers that need other vocabularies translate them in an outbound adapter (2.4).
- **Re-reconciliation:** a late or corrected record re-opens and re-evaluates the affected groups.
  - The previous outcome is kept as a version, never overwritten.
  - Outcome changes are published as events.
  - Re-recon is idempotent and safe to replay.
- **Concurrency:** a group is evaluated under an ownership claim tagged with run id, worker id and claim
  time, so two workers never write conflicting outcomes. Claims expire by heartbeat, so a dead worker's
  claims are reclaimed automatically.
- **Exceptions:**
  - an exception queue with assignment, comments, manual resolution (maker-checker) and reason;
  - a resolved item never re-opens unless its underlying data changes.
- **Results:**
  - per-record outcome;
  - per-provider, per-day summary (counts and amounts per outcome, matched value, variance);
  - a "reconciled day is final" marker, set once and only by the last run, with the gate counting runs
    still in flight or queued.

## 9. Concurrency, isolation, scale and the 40-minute ceiling

### 9.1 Many runs at once, in isolation
Any number of runs execute simultaneously: ETL for different providers, ETL for the same provider and
different business dates, recon for different rule sets and dates, streaming and batch recon, replays,
and report generation. None may slow down, block, corrupt or fail another.

- **Data isolation:**
  - every row a run writes carries its `run_id`;
  - every claim is owned by run id plus claim token;
  - every release or revert is scoped to its owner;
  - a run reads only its own artifact or its own (rule set, business date) scope;
  - prove it with a test where one run fails mid-way while another of the same provider completes
    untouched.
- **Same-scope rule:** two runs with the same scope — (source, business date) for ETL, or (rule set,
  business date) for recon — never run concurrently. The second waits in the dispatcher queue, unless
  it is an explicit replay that supersedes the first. Streaming and batch recon of the same scope
  coordinate through group claims.
- **Compute isolation:**
  - chunks from all runs share the worker fleet under weighted fair scheduling, so a 100M-record run
    can't starve a 100-record run;
  - per-provider and per-run concurrency caps and bulkheads apply;
  - a run that is failing or slow is throttled, never the fleet.
- **Database isolation:**
  - work is partition-scoped, so runs on different dates or providers touch different partitions;
  - no table locks and no long transactions (commit per chunk);
  - advisory locks are keyed by scope and used only to serialize finalization of the same scope;
  - every query carries the partition key, so partitions are pruned;
  - database IO is shared by every run, so admission control and throttling (9.4) keep one run from
    saturating it for the others.
- **Messaging isolation:** ordering keys per run and scope; a poison message dead-letters only its own
  chunk, with the run marked accordingly; others continue.
- **Memory and caches:** no per-run state held in a JVM beyond the chunk in hand; caches are keyed by
  config version and scope, never shared mutable maps.
- **Noisy-neighbour test (required):** one 100M-record recon, one 100M-record ETL and twenty small runs
  start together. Every small run meets its own target and every large run meets the 40-minute ceiling.

### 9.2 From hundreds to hundreds of millions
- **One pipeline, adaptive plan:**
  - a planner sizes each run from the artifact size or source count before it starts;
  - small runs take one chunk on the fast path with no extra scheduling overhead;
  - large runs split into many chunks sized for 30–60 seconds of work each, so failures and rebalancing
    are cheap.
  - It is the same code either way; only the plan differs.
- **ETL write path:**
  - streaming parse with constant memory;
  - bulk `COPY` into a run-scoped staging partition, then a set-based validate-and-merge into the
    canonical partition;
  - no row-at-a-time inserts, no N+1 lookups;
  - lookups for enrichment are batched.
- **Batch recon:**
  - set-based: candidate pairs come from hash-joins on `canonical_match_key` per (rule set, business
    date, hash bucket);
  - buckets are the unit of work, so workers evaluate rules over bucket slices in parallel;
  - streaming recon uses indexed point lookups instead.
- **Scale-out:**
  - workers autoscale on queue lag (KEDA or equivalent);
  - connection pooling (PgBouncer);
  - read replicas for every pure read (9.5), with more replicas added as read volume grows;
  - more hash buckets and partitions as volume grows.
  - Show throughput rising linearly with workers up to and past 100M records.

### 9.3 The 40-minute ceiling
- **Budget:** every ETL run and every recon run completes end to end — from start to final counts — in
  ≤ 40 minutes at 100 million records on the reference infrastructure. Runs up to 100k records finish
  in ≤ 2 minutes.
- **Capacity math:** the design must show it. 100M records in 40 minutes is about 42k records/s
  sustained. Give a time budget per stage (acquire, decrypt, parse, map/validate, load, merge; or
  key build, candidate join, rule evaluation, persist, summary) and size the reference infrastructure
  to fit with 25 % headroom.
  - The budget includes the database's IO limits (9.4), not only CPU and worker count. At this volume
    the database is usually the binding constraint.
- **Enforcement:**
  - the planner estimates duration from volume and historical throughput and scales out before the run
    starts;
  - stage timings are recorded per run;
  - an alert fires at 75 % of budget (30 minutes);
  - a breach is an incident with the stage breakdown attached, never a silent overrun.
- **Proof:** a nightly performance suite runs 100, 100k, 10M and 100M records for both ETL and recon,
  plus the noisy-neighbour mix from 9.1. A regression beyond the budget fails the pipeline.

### 9.4 Database IO — the shared limit
Workers scale out; the database's IO does not. Every concurrent run writes to and reads from the same
storage, so the design must measure database IO, budget it, minimise it and govern it explicitly. "Add
workers" is never the answer to a database-bound stage.

- **Know the ceiling:**
  - from the chosen database service and storage class, record:
    - provisioned IOPS and throughput (MB/s);
    - the instance's storage-bandwidth cap;
    - the maximum sustainable WAL rate and replication capacity;
    - `max_connections`;
  - write these into the capacity model before any sizing is done.
- **Size the IO per record:**
  - for ETL and recon, estimate and then measure:
    - heap bytes and index bytes written per record;
    - WAL bytes per record;
    - read bytes per record;
  - multiply by 100M records and divide by the 40-minute budget. If the result exceeds 75 % of the
    ceiling while other runs are also active, change the design rather than the threshold.
- **Write less:**
  - ETL loads with binary `COPY` in batches, never row by row;
  - per-run staging is `UNLOGGED` (no WAL). It is re-derivable from the raw artifact, so it is not
    replicated and is rebuilt after a crash. Only the final set-based merge is logged;
  - hot ingest partitions carry only the indexes that matching and pruning need. Covering (`INCLUDE`)
    indexes replace extra lookups;
  - an ADR evaluates loading a partition's data before its indexes exist (load, index, then
    `ATTACH PARTITION`) wherever partition ownership allows it;
  - recon results are append-only versions. Narrow status columns sit on tables with a fillfactor of
    80–90 so updates stay HOT updates;
  - wide JSONB attributes are written once, compressed (`lz4` TOAST), and never rewritten to change a
    status;
  - `synchronous_commit = off` is allowed only for re-derivable staging writes, never for canonical data,
    recon outcomes or audit.
- **Read less:**
  - batch recon joins are sized per hash bucket so they fit in `work_mem`; temporary-file spills are
    measured and treated as a defect;
  - every query is partition-pruned;
  - hot lookups are index-only scans;
  - **only writers touch the primary.** Every pure read goes to a read replica whenever one is
    available (9.5).
- **Housekeeping IO:**
  - each partition gets `ANALYZE` right after its bulk load, so plans are correct;
  - completed partitions are frozen (`VACUUM FREEZE`) off-peak, to avoid anti-wraparound IO storms
    later;
  - autovacuum thresholds are tuned per table for the hot partitions;
  - retention detaches and drops partitions; it never bulk-`DELETE`s;
  - checkpoint and WAL settings are sized for the bulk-load rate, and checkpoint frequency is monitored.
- **Govern it at run time:**
  - a global admission controller gives each run a database-capacity allowance (concurrent writers,
    rows or MB per second) by run class: real-time, small, large;
  - it adapts throttling from live database signals: write latency, IO wait, WAL rate, replication lag,
    checkpoint pressure, lock waits;
  - workers slow down through back-pressure before the database saturates;
  - real-time and small runs keep reserved capacity, so a 100M-record run can never push them past
    their targets;
  - connections go through PgBouncer in transaction mode, with separate fixed pools for the primary and
    each replica per service, and application semaphores keep virtual threads from queueing on those
    pools (16.3).
- **When one primary is not enough:** if the measured budget shows a single primary can't meet the
  40-minute ceiling at peak concurrency, the design must scale writes without breaking isolation, and
  record the choice in an ADR. Options include sharding by provider group across databases, a
  distributed PostgreSQL (e.g. Citus), or separate clusters for ETL and recon.
- **Proof:** the performance suite (9.3) records, per run and per stage:
  - IOPS and MB/s read and written;
  - WAL bytes per record;
  - temporary bytes;
  - replication lag;
  - checkpoint count;
  - lock waits;
  - connection-pool wait time.

  It asserts budgets on them: WAL bytes per record within its budget; no temporary spills in recon
  joins at the reference bucket size; replication lag under 30 s during a 100M-record ingest; database
  IO under 75 % of the ceiling during the noisy-neighbour mix.

### 9.5 Primary for writers, read replicas for everything else
The primary's IO is reserved for writes. Every read that is not part of a write goes to a read replica
whenever one is available.

- **On the primary:**
  - inserts, `COPY`, merges, updates and deletes;
  - the read-modify-write steps that are themselves writes, done as single statements
    (`UPDATE … WHERE … RETURNING`, `INSERT … ON CONFLICT`): claims, heartbeats, conditional state
    transitions, idempotency inserts, outbox writes and finalization;
  - nothing else. Existence and uniqueness are enforced by constraints and conflict handling, never by a
    "check first" read on the primary.
- **On replicas:**
  - reading recon candidate sets and match keys;
  - ETL enrichment and reference lookups;
  - configuration loads (then cached);
  - every API `GET`: recon queries, exceptions, run status, onboarding previews;
  - reports, exports and dashboards;
  - housekeeping scans that only read.
- **Read-your-writes without the primary:**
  - each run records the commit LSN of its final write (`pg_current_wal_lsn()` after commit);
  - a reader that needs that data — for example recon after the ETL run it depends on, or an API reading
    the result of a write it just made — waits until its replica's `pg_last_wal_replay_lsn()` has passed
    that LSN;
  - the wait is bounded, and on timeout the reader picks another replica or backs off and retries;
  - it never quietly reads stale data.
- **Routing is explicit:**
  - two data sources, `writer` and `reader`;
  - the code depends on separate ports (`…WriteStore` and `…ReadStore`), not on a read-only
    transaction flag;
  - an ArchUnit rule stops query, report and recon-read code from depending on the writer data source;
  - the reader connects with a database role that has `SELECT` only.
- **Several replicas:**
  - reads are load-balanced across healthy replicas, each with its own bounded pool;
  - a replica whose lag exceeds the budget for that kind of read (seconds for API reads, LSN-fenced for
    recon) is taken out of rotation until it catches up;
  - long report queries go to a replica dedicated to them where one exists, so they never delay
    recon reads.
- **When no replica is available:** whether reads fall back to the primary is a configuration switch
  (`reconark.db.read-fallback-to-primary`), on by default only in non-production. With it on, reads
  passing through the primary count against the admission controller's budget (9.4), real-time and
  small runs keep priority, and an alert fires. With it off, readers wait for a replica and back off.
- **Proof:**
  - tests assert that a full ETL plus recon run issues no read-only statements on the primary (captured
    per data source with statement metrics);
  - the replica-lag fencing is tested with a replica artificially delayed;
  - the fallback switch is tested both ways.

## 10. Reports

- **Report types:**
  - daily recon summary per provider pair;
  - mismatch detail with per-field diffs;
  - unmatched items by side, with ageing buckets;
  - exception status and SLA breach;
  - ingestion quality (accepted, rejected and quarantined per source and run, with top reject reasons);
  - trend over a date range.
- **How they run:**
  - Generated asynchronously from a request; large reports stream (constant memory) from read replicas
    or summary tables, never with unbounded queries on the primary.
  - Scheduled reports are definitions with a cron expression and recipients, executed through the same
    path.
- **Formats:** JSON via API; CSV, XLSX and PDF for export, all produced by one export framework (one
  writer per format; data assembly is shared).
- **Content security:**
  - every report applies the caller's data scope (section 19) at query time;
  - sensitive fields are masked unless the caller holds an unmask permission, and every unmask is
    audited;
  - CSV and XLSX cells are escaped against formula injection (`=`, `+`, `-`, `@`, tab, CR at cell start).
- **Delivery:**
  - files are stored encrypted, checksummed, and downloadable only through a short-lived, single-use,
    signed URL bound to the requesting user;
  - files expire per retention policy;
  - every download is audited.

## 11. Data model — PostgreSQL 18

This section is a specification, not a suggestion. Produce the Flyway migrations exactly to it, and
explain any deviation in an ADR.

### 11.1 Conventions (apply to every table)
- **Schema:** `reconark` for application data; `reconark_audit` for the audit log.
- **Naming:** `snake_case`, singular table names. Constraint names:
  - primary key `pk_<table>`, foreign key `fk_<table>__<ref>`, unique `uq_<table>__<cols>`;
  - check `ck_<table>__<rule>`, index `ix_<table>__<cols>`.
- **Keys:**
  - internal surrogate `id BIGINT GENERATED ALWAYS AS IDENTITY`;
  - any row exposed through an API also has `public_id UUID NOT NULL DEFAULT uuidv7()` (time-ordered,
    index-friendly) with a unique constraint;
  - APIs only ever accept and return `public_id`. Sequential ids are never exposed, so they can't be
    enumerated.
- **Audit columns:** every mutable table has `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`,
  `created_by TEXT NOT NULL`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`, `updated_by TEXT NOT NULL`,
  and `row_version INT NOT NULL DEFAULT 0` for optimistic locking.
  - One shared trigger function maintains `updated_at` and keeps `created_at` immutable.
  - Append-only tables carry `created_at`/`created_by` only.
- **Types:**
  - money `NUMERIC(19,4)`;
  - currency `CHAR(3)` checked against ISO-4217 format;
  - times `TIMESTAMPTZ`, business dates `DATE`, durations `INTERVAL`;
  - flags `BOOLEAN NOT NULL`;
  - free text `TEXT` with length checks;
  - semi-structured data `JSONB`, only where the structure varies by provider and with a validated
    schema.
  - No `FLOAT` or `DOUBLE` for any amount.
- **Integrity:**
  - `NOT NULL` unless null has a meaning (document it with a column comment);
  - every foreign key declared and indexed;
  - `ON DELETE RESTRICT` by default, `CASCADE` only for pure child rows of an immutable parent.
- **Enumerations:**
  - small fixed sets: `TEXT` + `CHECK (col IN (…))`;
  - sets that business users extend (reason codes, canonical fields): lookup tables.
- **Deletes and history:**
  - configuration is never hard-deleted; it moves through a status lifecycle;
  - approved configuration rows are immutable (an update trigger rejects changes).
- **Partitioning:** every runtime, reporting and audit table is partitioned, and every table's scheme is
  set in configuration **before installation** (section 11.5).
  - Partitioned primary keys, unique constraints and foreign keys all include the partition key.
  - Idempotency keys include their scope's business date in the key material, so uniqueness within a
    partition is global uniqueness.
  - On partitioned tables `public_id` is unique together with the partition key. APIs address business-date-partitioned
    resources with the business date in the path, so every lookup prunes to one partition.
- **Privileges:**
  - the writer role, used only on the primary, has DML on its own tables, no DDL, and no
    `UPDATE`/`DELETE` on append-only tables;
  - the reader role, used on replicas, has `SELECT` only;
  - migrations run under a separate role;
  - row-level security is enabled on canonical, recon and report tables and enforces provider data scope
    for analyst and partner roles.
- **Migrations:** online-only, as in 2.3 item 10, each with a `lock_timeout`, and each reviewed against
  production-sized data.

### 11.2 Configuration tables
| Table | Purpose and key columns | Keys, constraints and indexes |
|---|---|---|
| `provider` | `public_id`, `code`, `name`, `status` (DRAFT, ACTIVE, SUSPENDED, RETIRED), `business_timezone`, `default_currency` | `uq(code)`; status check; timezone validated against the IANA list on write |
| `provider_config_version` | `provider_id`, `version_no`, `status` (DRAFT, PENDING_APPROVAL, APPROVED, ACTIVE, SUPERSEDED, REJECTED), `effective_from`, `content_hash`, `submitted_by`, `approved_by`, `approved_at`, `change_reason` | `uq(provider_id, version_no)`; partial unique index giving one `ACTIVE` version per provider; `ck(approved_by <> submitted_by)`; rows immutable once `APPROVED` |
| `canonical_field` | dictionary of target fields: `code`, `data_type`, `is_core`, `is_sensitive`, `description` | `pk(code)`; data-type check |
| `source` | `config_version_id`, `code`, `side_role` (INTERNAL, PARTNER), `connector_type` (SFTP, OBJECT_STORE, REST, SOAP, GRPC, MQ, UPLOAD), `format`, `encoding`, `encryption` (NONE, PGP), `compression`, `record_root_path`, `secret_ref` (name only), `connection_settings` (JSONB, non-secret, schema-validated), `schedule_cron`, `expected_by_time`, `completeness_rule` (JSONB) | `uq(config_version_id, code)`; checks on every enumerated column; `ck(secret_ref ~ '^[A-Za-z0-9/_.-]+$')` |
| `field_mapping` | `source_id`, `target_field`, `source_path`, `position`, `data_type`, `format_pattern`, `required`, `default_value`, `transform_expression`, `cardinality` (EXACTLY_ONE, FIRST, JOIN, SUM), `is_sensitive` | `fk → canonical_field`; `uq(source_id, target_field)`; `ck(position IS NULL OR position > 0)` |
| `validation_rule` | reusable library: `code`, `scope` (FIELD, RECORD, FILE), `expression`, `failure_message_template`, `severity`, `status` | `uq(code)`; expression compiled and sandbox-checked before insert |
| `field_mapping_validation` | `field_mapping_id`, `validation_rule_id`, `seq` | `pk(field_mapping_id, validation_rule_id)`; `uq(field_mapping_id, seq)` |
| `reason_code` | `code`, `numeric_code`, `category`, `message_template`, `legacy_v1_code` | `pk(code)`; `uq(numeric_code)` |
| `value_map` / `value_map_entry` | named value translations; entries `left_value`, `right_value` | `uq(value_map.code)`; `pk(value_map_id, left_value)` |
| `recon_rule_set` | `config_version_id`, `code`, `left_source_id`, `right_source_id`, `cardinality` (ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE), `duplicate_policy`, `matching_window`, `pending_failure_after`, `success_statuses`, `pending_statuses` | `uq(config_version_id, code)`; `ck(left_source_id <> right_source_id)`; both FKs to `source` within the same config version (trigger-checked) |
| `recon_match_key` | `rule_set_id`, `priority`, `left_field`, `right_field`, `normalizations` (TEXT[] from a fixed set) | `uq(rule_set_id, priority, left_field)`; `ck(priority > 0)` |
| `recon_compare_rule` | `rule_set_id`, `seq`, `left_field`, `right_field`, `comparator`, `tolerance_abs`, `tolerance_pct`, `rounding_scale`, `rounding_mode`, `time_window`, `value_map_id`, `severity` (BLOCKING, WARNING), `reason_code` | `uq(rule_set_id, seq)`; checks tying each comparator to its required parameters (e.g. a numeric comparator requires a tolerance and rounding; a value-map comparator requires `value_map_id`); `ck(tolerance_abs >= 0 AND tolerance_pct BETWEEN 0 AND 100)` |
| `report_definition` | `public_id`, `code`, `report_type`, `parameters` (JSONB, schema-validated), `schedule_cron`, `output_format`, `owner`, `data_scope`, `status` | `uq(code)`; type and format checks |

### 11.3 Runtime tables
| Table | Purpose and key columns | Keys, constraints and indexes |
|---|---|---|
| `raw_artifact` | `public_id`, `source_id`, `config_version_id`, `storage_uri`, `content_sha256`, `size_bytes`, `business_date`, `received_at`, `status` (RECEIVED, VERIFIED, QUARANTINED, PROCESSED, REJECTED), `quarantine_reason` | `uq(source_id, content_sha256, business_date)`; `ck(size_bytes >= 0)`; `ix(status, received_at)` |
| `ingestion_run` | `public_id`, `source_id`, `business_date`, `config_version_id`, `artifact_id`, `trigger` (SCHEDULE, API, EVENT, REPLAY), `idempotency_key`, `status` (QUEUED, RUNNING, COMPLETED, COMPLETED_WITH_REJECTS, FAILED, CANCELLED), `read_count`, `accepted_count`, `rejected_count`, `quarantined_count`, `started_at`, `finished_at`, `heartbeat_at` | `uq(idempotency_key)`; `ck(status NOT IN ('COMPLETED','COMPLETED_WITH_REJECTS') OR read_count = accepted_count + rejected_count + quarantined_count)`; `ix(status, heartbeat_at)` |
| `work_chunk` | `run_id`, `business_date` (the run's), `seq`, `range_start`, `range_end`, `status`, `claimed_by`, `claim_token`, `claimed_at`, `heartbeat_at`, `attempt`, the same four counts, `last_error_code` | `uq(run_id, seq)`; the same count-balance check; `ck(range_end >= range_start)`; `ix(status, heartbeat_at)` for the reclaimer; every update conditional on `claim_token` |
| `canonical_record` (partitioned by `business_date`) | `source_id`, `config_version_id`, `run_id`, `artifact_id`, `source_locator`, `business_key`, `record_version`, `business_date`, `event_time`, `amount`, `currency`, `status_raw`, `status_normalized`, `attributes` (JSONB), `record_hash` | `pk(id, business_date)`; `uq(source_id, business_key, business_date, record_version)`; `ix(source_id, business_date)`; GIN or expression indexes only on attributes used by match keys |
| `canonical_match_key` (partitioned) | precomputed normalized keys: `record_id`, `business_date`, `rule_set_id`, `priority`, `key_hash` (BYTEA, SHA-256) | `pk(record_id, business_date, rule_set_id, priority)`; `ix(rule_set_id, priority, key_hash, business_date)` — matching never scans JSONB |
| `reject_record` (partitioned) | `run_id`, `chunk_id`, `business_date`, `source_locator`, `stage` (VERIFY, DECRYPT, PARSE, MAP, VALIDATE, PERSIST), `error_code`, `field_errors` (JSONB array of field, masked value, rule, message), `raw_excerpt_masked` | `ix(run_id)`; `ix(error_code, business_date)` |
| `recon_run` | `public_id`, `rule_set_id`, `business_date`, `mode` (STREAM, BATCH, RERECON), `idempotency_key`, `status`, counts per outcome, `started_at`, `finished_at`, `heartbeat_at` | `uq(idempotency_key)`; `ix(rule_set_id, business_date, status)` |
| `recon_group` (partitioned) | one evaluated pairing, versioned: `public_id`, `rule_set_id`, `business_date`, `group_version`, `is_current`, `outcome`, `decided_by_run_id`, `decided_at`, `claim_token`, `claimed_by`, `heartbeat_at` | `pk(id, business_date)`; outcome check; partial `ix(rule_set_id, business_date, outcome) WHERE is_current` |
| `recon_group_member` (partitioned) | `group_id`, `business_date`, `side` (LEFT, RIGHT), `record_id`, `is_current` | `pk(group_id, business_date, side, record_id)`; partial unique `(rule_set_id, record_id) WHERE is_current` — a record belongs to one current group per rule set |
| `recon_field_diff` (partitioned) | `group_id`, `group_version`, `business_date`, `compare_rule_id`, `left_value_masked`, `right_value_masked`, `delta`, `tolerance_applied`, `reason_code`, `severity` | `pk(group_id, group_version, compare_rule_id, business_date)` |
| `recon_exception` | `public_id`, `group_id`, `business_date`, `status` (OPEN, ASSIGNED, PENDING_APPROVAL, RESOLVED, REOPENED), `assignee`, `sla_due_at`, `resolution_code`, `resolution_note`, `resolved_by`, `approved_by`, `approved_at` | `ck(approved_by IS NULL OR approved_by <> resolved_by)`; `ix(status, sla_due_at)`; `ix(assignee, status)` |
| `recon_exception_comment` | `exception_id`, `business_date`, `author`, `body` (length-checked, stored as plain text) | append-only |
| `recon_day_summary` | `rule_set_id`, `business_date`, counts and amounts per outcome, `variance_amount`, `is_final`, `finalized_at`, `finalized_by_run_id` | `pk(rule_set_id, business_date)`; `ck(NOT is_final OR finalized_at IS NOT NULL)` |
| `report_execution` | `public_id`, `report_definition_id` (nullable for ad hoc), `requested_by`, `parameters`, `data_scope_snapshot`, `status`, `row_count`, `file_uri`, `file_sha256`, `expires_at`, `download_count` | `ix(requested_by, created_at)`; `ix(status)`; `ck(expires_at > created_at)` |

### 11.4 Platform tables
| Table | Purpose | Keys, constraints and indexes |
|---|---|---|
| `outbox_event` | transactional outbox: `aggregate_type`, `aggregate_id`, `event_type`, `payload`, `headers`, `published_at` | partial `ix(created_at) WHERE published_at IS NULL` |
| `processed_message` | consumer idempotency: `consumer`, `message_id`, `processed_at` | `pk(consumer, message_id)`; hash-partitioned on `message_id` (11.5) so uniqueness stays global; batched time-based purge |
| `scheduler_lock` | distributed scheduler leadership (ShedLock-compatible) | `pk(name)` |
| `reconark_audit.audit_event` (partitioned monthly) | `occurred_at`, `actor`, `actor_type`, `action`, `entity_type`, `entity_public_id`, `before_masked`, `after_masked`, `client_ip`, `request_id`, `prev_hash`, `hash` | append-only (grants plus a trigger rejecting `UPDATE`/`DELETE`); `hash = SHA-256(prev_hash ‖ canonical event)`, forming a tamper-evident chain that is verified daily |

### 11.5 Partitioning — configured per table, before setup
Partitioning is decided per table, in one versioned file (`reconark-partitioning.yaml`, or the equivalent
`reconark.partitioning.tables.<table>.*` properties), before the first installation. Migrations generate
the DDL from it; nothing about partitioning is hard-coded in the SQL.

- **Per-table settings:**
  - `strategy`: `RANGE`, `LIST`, `HASH`, `RANGE_HASH` (range, then hash sub-partitions) or `NONE`;
  - `key` and, for `RANGE_HASH`, `sub_key`;
  - `interval` for range: `DAILY`, `WEEKLY` or `MONTHLY`;
  - `modulus` for hash: a power of two;
  - `precreate_ahead`: how many future partitions always exist;
  - `retention`: detach after N partitions, then archive (to object storage) or drop;
  - `default_partition`: whether a catch-all exists. Rows landing in it raise an alert.
- **Validation before any DDL (fail fast):**
  - every table appears in the file — nothing is partitioned by accident or by default;
  - keys come from that table's allowed list and are part of its primary key and unique constraints;
  - `NONE` is accepted only for the configuration and lookup tables in 11.2, and only when written
    explicitly;
  - retention is no shorter than the data retention policy, the replay horizon and the report range;
  - the hash modulus is a power of two;
  - foreign keys between partitioned tables carry the partition key.
- **Maintenance:**
  - a leader-elected maintenance job (pg_partman or an in-house equivalent) pre-creates partitions ahead,
    detaches expired ones without blocking, and archives or drops them per policy;
  - an alert fires when future partitions drop below `precreate_ahead`;
  - the job is idempotent and covered by tests.
- **Drift and change:**
  - at startup the service compares the live catalog with the configuration and refuses to start on any
    drift;
  - changing a table's scheme after setup is not a config edit. It is an explicit online migration
    (new partitioned table, batched copy or dual write, verified swap), documented and rehearsed on
    production-sized data.

**Defaults to propose** (confirm against the volumes in section 20):

| Table | Strategy | Key / sub-key | Interval / modulus |
|---|---|---|---|
| `canonical_record` | `RANGE_HASH` | `business_date` / `source_id` | DAILY / 8 |
| `canonical_match_key` | `RANGE_HASH` | `business_date` / `key_hash` | DAILY / 16 |
| `reject_record` | `RANGE` | `business_date` | DAILY |
| `recon_group`, `recon_group_member`, `recon_field_diff` | `RANGE_HASH` | `business_date` / `rule_set_id` | DAILY / 8 |
| `raw_artifact`, `ingestion_run`, `work_chunk`, `recon_run` | `RANGE` | `business_date` | MONTHLY |
| `recon_exception`, `recon_exception_comment`, `recon_day_summary` | `RANGE` | `business_date` | MONTHLY |
| `report_execution` | `RANGE` | `created_at` | MONTHLY |
| `outbox_event` | `RANGE` | `created_at` | DAILY |
| `processed_message` | `HASH` | `message_id` | 16 |
| `reconark_audit.audit_event` | `RANGE` | `occurred_at` | MONTHLY |
| configuration and lookup tables (11.2) | `NONE` (explicit) | — | — |

## 12. Transactions, ACID and isolation

Correctness under concurrency is designed, not assumed. Every write path in the architecture document
states its transaction boundary, its isolation level, the locks it takes, and how it retries.

### 12.1 ACID, applied
- **Atomicity:**
  - one unit of work equals one database transaction. For an ETL chunk, that covers its load, merge,
    counts, claim completion and outbox events; for a recon bucket, its outcome versions, field
    differences, counts and events.
  - It all commits or none of it does. A retry redoes the whole unit, idempotently.
- **Consistency:**
  - invariants live in the database as constraints (section 11): checks, foreign keys, unique keys, the
    count-balance check, maker ≠ checker, and one active configuration version;
  - application validation is the first line of defence, never the only one.
- **Isolation:** the levels in 12.2, chosen per path.
- **Durability:**
  - `synchronous_commit = on` for canonical data, recon outcomes, configuration and audit;
  - synchronous replication to a standby in another zone, so committed outcomes survive losing a zone
    (RPO 0);
  - relaxed durability only for re-derivable staging (9.4).

### 12.2 Isolation levels (PostgreSQL)
| Path | Level | Why |
|---|---|---|
| Claims, heartbeats, state transitions, idempotency and outbox inserts, chunk merges | `READ COMMITTED` | Each is one atomic statement (`UPDATE … WHERE claim_token = ? … RETURNING`, `INSERT … ON CONFLICT`) under row locks, so a stronger level only adds retries. |
| Work queue pickup | `READ COMMITTED` with `SELECT … FOR UPDATE SKIP LOCKED` | Many workers take different chunks without blocking each other. |
| Multi-statement invariants not covered by a single constraint (day finalization, activating a configuration version, exception approval) | `SERIALIZABLE`, or `READ COMMITTED` plus a transaction-scoped advisory lock on the scope | Each choice is recorded in an ADR. Serialization failures (`40001`) and deadlocks (`40P01`) are retried with jitter and a retry limit. |
| Consistent multi-query reads (summaries, report snapshots, recon candidate reads on replicas) | `REPEATABLE READ READ ONLY` | Gives one snapshot across queries. Hot-standby replicas do not support `SERIALIZABLE`, so replica reads never ask for it. |

### 12.3 Locks, timeouts and deadlocks
- **Lock ordering:** locks are always taken in one global order (partition key, then id), so deadlocks
  cannot form by construction; a test proves it under parallel load.
- **Timeouts:** per database role — `lock_timeout`, `statement_timeout`,
  `idle_in_transaction_session_timeout` — plus a transaction timeout in the application. No transaction
  waits forever.
- **Short transactions:** bounded by rows and by seconds. Long transactions block vacuum, bloat tables
  and cancel replica queries, so long reads are chunked and resumable rather than one big snapshot.
  `hot_standby_feedback` and `max_standby_streaming_delay` are set deliberately and recorded in an ADR.
- **Optimistic and pessimistic locking:**
  - entities people edit (configuration, exceptions) use optimistic locking (`row_version`, `If-Match`);
  - automated workers use short pessimistic row locks (claims).

### 12.4 Across the database and the broker
- No distributed transactions: no XA, no two-phase commit.
- State and events stay consistent through the transactional outbox (section 5). Consumers are idempotent
  through `processed_message`, inserted in the same transaction as the consumer's effect.
- Multi-step lifecycles (a run: plan → chunks → merge → finalize) are sagas, with an explicit state
  machine and compensations. Each step is idempotent and resumable after a crash.
- No network call to a partner, broker, secret manager or object store happens inside an open database
  transaction.

### 12.5 Transaction management in code
- Transactions are declared only at application-service boundaries:
  - `@Transactional` with propagation `REQUIRED`, explicit isolation and timeout;
  - or `TransactionTemplate` in workers.
- Never on repositories, controllers, private methods, or methods called from inside the same class
  (proxies don't apply there). ArchUnit checks this.
- `REQUIRES_NEW` only for records that must survive a rollback, such as failure records and audit
  entries, each named in an ADR.
- Read-only use cases run in read-only transactions on the reader data source (9.5); writes run on the
  writer. One transaction never spans both.
- Virtual threads: one transaction per task, and a connection is held only while the transaction is
  open (16.3).
- PgBouncer runs in transaction mode, so code keeps no session state:
  - `SET LOCAL` instead of `SET`;
  - `pg_advisory_xact_lock` instead of session advisory locks;
  - prepared statements confirmed against the PgBouncer and driver versions in use.

### 12.6 Proof
- concurrent claimers never take the same chunk;
- every isolation anomaly the design rules out is attempted in a test and shown not to occur;
- deliberate deadlock and serialization-failure scenarios are retried to success;
- a worker is killed (`kill -9`) between statements and mid-commit, and the run still recovers to a
  consistent state;
- the count-balance and uniqueness invariants hold after every chaos test.

## 13. APIs

- **Contract-first:**
  - OpenAPI 3.1 for REST, `.proto` for gRPC, WSDL for SOAP;
  - one shared error model (RFC 9457 problem details);
  - one pagination model (cursor-based);
  - one idempotency-key header.

  Server stubs and DTOs are generated from the contracts, never hand-written twice.
- **Onboarding API:**
  - create, validate, dry-run, submit, approve or reject, activate and roll back a provider configuration
    version;
  - test a connector;
  - preview a field mapping against an uploaded sample, showing extracted values and validation results
    per field (sensitive values masked).
- **Ingestion:**
  - REST and gRPC endpoints, plus an optional SOAP endpoint, for partners that push;
  - each accepts a batch whose transactions may be nested as in 6.4;
  - each requires an idempotency key and returns per-record acceptance or rejection with reasons;
  - gRPC supports client streaming for large batches.
- **Recon query:** results by provider, date, outcome, key and amount range; per-field diffs; summaries;
  exception queue operations; re-recon trigger (scoped and idempotent).
- **Reports:** request, status, download (signed URL), schedule management.
- **Operations:** trigger, pause, resume and replay a run; view run progress and chunk status; requeue
  the DLQ; health and readiness. These run on a separate, internal-only listener and are never exposed
  publicly.

## 14. Observability

- **Structured JSON logs** carrying correlation id, run id, chunk id, provider, config version, record
  key (masked) and trace id. One business event per line; no PII; no stack traces for expected business
  rejections. CR/LF and control characters in logged values are encoded, to prevent log injection.
- **OpenTelemetry tracing** across API → bus → worker → DB, with trace context propagated through message
  headers on every broker adapter.
- **Metrics (Prometheus):**
  - per provider and stage: records in, accepted, rejected and quarantined;
  - lag and throughput;
  - recon outcomes and matched amount;
  - run duration;
  - claim age, heartbeat staleness, DLQ depth, broker consumer lag;
  - database, per instance (primary and each replica): IOPS and MB/s, IO wait, WAL rate, replication
    lag, LSN-fence wait time, read-only statements reaching the primary (expected zero), checkpoints,
    temporary-file bytes, autovacuum activity, connection-pool wait, admission-controller throttling;
  - secret refresh and connector errors;
  - auth failures and rate-limit rejections.
- **Real-time dashboards** (Grafana) per provider and for the platform; SLOs with burn-rate alerts.
- **Alerts:**
  - file not received by its expected time;
  - reject rate above a threshold;
  - mismatch value above a threshold;
  - run stuck;
  - DLQ non-empty;
  - checksum or decryption failures;
  - spike in auth failures;
  - audit-chain verification failure.

## 15. Code quality — near-zero duplication

- **CI gates:**
  - SonarQube quality gate on new code: duplicated lines ≤ 1 %, 0 bugs, 0 vulnerabilities, 0 unreviewed
    security hotspots, maintainability A, coverage ≥ 85 % (≥ 95 % for the rule engine, comparators,
    mapping and security code);
  - PMD CPD on the whole codebase;
  - the build fails on any breach.
- **Design rules that prevent duplication:**
  - one implementation per concern: retry, idempotency, claim/heartbeat/reclaim, masking, error model,
    pagination, export, audit;
  - concrete strategies and adapters inject or compose these (template method or composition), never
    copy them;
  - one comparator registry, one connector base, one parser framework, one export framework, one
    secret-access component, one claim manager used by both ETL chunks and recon groups;
  - DTOs and clients are generated from contracts;
  - database access is explicit SQL or jOOQ with typed records, with no copy-pasted query fragments
    (shared fragments are named constants or builder functions).
- **ArchUnit tests enforce structure:**
  - the domain core has no framework imports;
  - adapters never call each other;
  - only the `MessageBus` port touches broker clients;
  - only the `SecretProvider` port touches the secret manager;
  - controllers never touch repositories directly.
- **Comments:** none unless something is genuinely non-obvious; then one plain line.

## 16. Technology baseline, build and threading

### 16.1 Versions — latest stable, nothing older
- **Java 25 (LTS)** through a Gradle toolchain. Only final language and platform features in production
  code; `--enable-preview` is forbidden. Structured concurrency, for example, is still a preview in
  Java 25, so it needs an ADR once it is final.
- **Latest GA release of everything else at project start:**
  - Spring Boot 4.x (Spring Framework 7, Spring Security 7);
  - PostgreSQL 18;
  - Flyway;
  - Kafka 4.x client, RabbitMQ and ActiveMQ Artemis clients;
  - BouncyCastle (`jdk18on` artifacts);
  - OpenTelemetry and Micrometer;
  - JUnit, Testcontainers and ArchUnit.

  Record the exact versions in the version catalog and the ADR log.
- **Version policy:**
  - no milestones, release candidates or snapshots;
  - every version lives in one version catalog;
  - automated update pull requests (Renovate or equivalent) run the full test and security pipeline;
  - no dependency with a known High or Critical CVE;
  - any library without a release in 12 months needs an ADR.

### 16.2 Build — Gradle, written from scratch
- **Wrapper and DSL:** Gradle 9.x wrapper with `distributionSha256Sum` pinned; Kotlin DSL only.
- **Written for reconArk from a blank page:**
  - do not copy or adapt build files from any existing project, this one included;
  - no remote script plugins (`apply(from = "https://…")`), no external shared-convention repositories;
  - plugins and dependencies come only from the version catalog, resolved from approved repositories
    (Maven Central or the bank's mirror). Repositories are declared once in `settings.gradle.kts`, with
    `RepositoriesMode.FAIL_ON_PROJECT_REPOS` and content filtering.
- **Layout:**
  - `settings.gradle.kts`, `gradle/libs.versions.toml`;
  - an included `build-logic` build with precompiled convention plugins (`reconark.java-conventions`,
    `reconark.library`, `reconark.spring-service`, `reconark.quality`, `reconark.test-suites`), so each
    module's build file is a few lines and no build logic is duplicated (section 15).
- **Modules** (or an alternative split justified in an ADR):
  - `reconark-domain` (no framework dependencies);
  - `reconark-application` (use cases and ports);
  - `reconark-api-contracts` (OpenAPI, `.proto`, WSDL, plus code generation);
  - one adapter module per port implementation: `adapter-messaging-kafka`, `-rabbitmq`, `-activemq`,
    `adapter-secrets-aws`, `adapter-connector-sftp`, `-objectstore`, `-rest`, `-soap`, `-grpc`,
    `adapter-persistence-postgres`;
  - independently deployable and scalable apps: `reconark-onboarding-api`, `reconark-ingestion-worker`,
    `reconark-recon-worker`, `reconark-report-service`, `reconark-scheduler`.
- **Build settings:**
  - toolchain 25, with `-Xlint:all -Werror`;
  - Error Prone and NullAway, if confirmed compatible with Java 25 (ADR if not);
  - reproducible archives;
  - configuration cache and build cache enabled;
  - dependency locking and dependency verification (`gradle/verification-metadata.xml` with checksums
    and signatures).
- **Test suites:** the JVM Test Suite plugin defines `test`, `integrationTest`, `e2eTest` and
  `performanceTest`, with aggregated JaCoCo coverage.
- **Quality and packaging plugins:**
  - Sonar, Spotless, Checkstyle, PMD CPD;
  - OWASP Dependency-Check and a CycloneDX SBOM;
  - container images built with Jib or Spring Boot buildpacks on a minimal, non-root base.
- **Where things run:** `./gradlew build` runs everything except the e2e and performance suites; CI runs
  all of them.

### 16.3 Threading — virtual threads where they help
- **Virtual threads for blocking, IO-bound work:**
  - API request handling (REST, gRPC, SOAP);
  - connectors (SFTP, object storage, partner REST, SOAP and gRPC calls);
  - secret-manager calls;
  - message consumers whose handlers do IO;
  - report streaming;
  - the outbox relay.
- **Bounded platform-thread pools (sized to cores) for CPU-bound work:** decryption, decompression,
  parsing, hashing and rule evaluation. Virtual threads add nothing there.
- **Virtual threads are not a limit:** guard every scarce resource explicitly:
  - database connections — pool size plus a semaphore, so thousands of virtual threads don't pile onto
    the pool;
  - per-partner call concurrency;
  - broker in-flight messages;
  - memory per chunk.
- **Usage:**
  - one virtual thread per task (`Executors.newVirtualThreadPerTaskExecutor()`), never pooled;
  - named thread factories, for diagnostics.
- **Context:** `ScopedValue` (final in Java 25) carries request and run context instead of `ThreadLocal`.
  Logging MDC and OpenTelemetry context are propagated explicitly onto every task.
- **Pinning:**
  - avoid blocking inside native frames;
  - confirm the JDBC driver and IO libraries are virtual-thread friendly;
  - performance tests record JFR `jdk.VirtualThreadPinned` events and fail on unexpected pinning.
- **Proof:** for every IO-bound path, the performance suite compares throughput and p99 latency with
  virtual threads on and off, and the result is recorded in the ADR log.

### 16.4 Naming — reconArk's own, nothing inherited
Every name below is new. None comes from the legacy platform (2.4).
- **Packages:**
  - one reverse-domain root, chosen once in an ADR (placeholder `io.reconark`);
  - then `<root>.<context>.<layer>`. Bounded contexts: `onboarding`, `ingestion`, `recon`, `reporting`,
    `platform`. Layers: `domain`, `application`, `adapter`, `api`.
  - Example: `io.reconark.recon.domain`, `io.reconark.platform.messaging`.
- **Gradle modules:**
  - `reconark-<context>-<layer>`;
  - adapters `reconark-adapter-<port>-<technology>` (e.g. `reconark-adapter-messaging-kafka`);
  - deployable apps `reconark-app-<name>`.
- **Types:**
  - domain names that say what the thing is: no `Manager`, `Helper`, `Util` or `Common` grab-bags;
  - ports named by capability (`MessageBus`, `SecretProvider`, `CanonicalRecordWriteStore`,
    `CanonicalRecordReadStore`);
  - adapters named `<Technology><Port>` (e.g. `KafkaMessageBus`, `AwsSecretsManagerSecretProvider`).
- **Database:** schema `reconark`; table and constraint conventions per 11.1.
- **Configuration properties:** `reconark.<context>.<setting>`, kebab-case.
- **Topics and queues:** `reconark.<context>.<event>.v<major>`, with the event in past tense and
  kebab-case; dead-letter queues add `.dlq`.
- **Reason and error codes:** `RK-<AREA>-<NNNN>`.
  - Areas: `ING` (acquisition), `DEC` (decryption), `PRS` (parsing), `MAP`, `VAL`, `RCN` (recon), `SEC`,
    `OPS`.
  - Codes are never reused or renumbered. Each has a message template and a problem-type URI.
- **Metrics:** Prometheus style, `reconark_<context>_<measure>_<unit>`
  (e.g. `reconark_ingestion_records_total`, `reconark_recon_run_duration_seconds`).
- **Enforcement:** an ArchUnit rule and a CI check fail on package, module or property names outside
  these patterns. A deny-list of legacy identifiers also fails the build if any reappears.

## 17. Cloud platforms — AWS, Azure and GCP

reconArk runs on AWS, Azure or Google Cloud with the same code, the same container images and the same
Helm charts. Only adapters and infrastructure code differ per cloud. The companion document
`../02-architecture/reconArk-architecture.md` holds the baseline architecture and the decisions per
cloud, with ADRs in `../04-adr/` and diagram sources in `../03-design/`. Treat those decisions as approved
defaults; challenge any of them only through an ADR.

- **Portable core:**
  - Kubernetes;
  - PostgreSQL as the only system of record;
  - the `MessageBus` port with Kafka, RabbitMQ and ActiveMQ adapters;
  - OpenTelemetry for telemetry;
  - OIDC for identity.

  No business code imports a cloud SDK.
- **Cloud-specific adapters, behind ports:**
  - object storage (S3, Blob Storage, Cloud Storage);
  - secret manager (Secrets Manager, Key Vault, Secret Manager);
  - key management and envelope encryption (KMS, Key Vault/Managed HSM, Cloud KMS);
  - SFTP intake;
  - workload identity (IRSA/EKS Pod Identity, Entra Workload ID, GKE Workload Identity).

  No static cloud credentials anywhere.
- **Managed services first,** where they meet the requirements (PostgreSQL 18 with read replicas, the
  chosen broker, private networking, customer-managed keys, the bank's compliance list). Where a managed
  service doesn't — for example, no managed RabbitMQ or ActiveMQ on Azure or GCP — run it on Kubernetes
  with an operator, and record the operational cost in an ADR.
- **Infrastructure as code:**
  - Terraform modules per cloud, one shared module interface, one environment layout;
  - policy-as-code (OPA/Conftest or Checkov) in CI;
  - no manual console changes; drift is detected and alerted.
- **Network:** private by default. Private endpoints for the database, broker, storage and secrets;
  no public IP on any workload. Internet ingress only through the cloud's WAF and the API gateway.
  Egress through a controlled NAT or proxy with the connector allow-list (section 18).
- **Resilience per cloud:**
  - multi-zone for every tier;
  - stated RPO and RTO per tier (RPO 0 for committed recon outcomes);
  - a cross-region disaster recovery option with a tested runbook;
  - backups encrypted with customer-managed keys and restore-tested on a schedule.
- **Proof:**
  - the walking skeleton deploys and passes the end-to-end and isolation suites on at least the primary
    target cloud;
  - the adapter contract tests pass for all three clouds' adapters (using emulators or Testcontainers
    where a real account isn't available).

## 18. Security and VAPT readiness

The bar: a penetration test against OWASP Top 10 (2021), OWASP API Security Top 10 (2023) and ASVS 4.0
Level 2 (Level 3 for the onboarding and reports APIs) finds nothing of medium severity or above, and
SonarQube reports no vulnerabilities and no unreviewed hotspots. Build each control below and pin it with
a test.

| Threat | Required control |
|---|---|
| Injection (SQL) | Parameterized SQL only; no string-built queries; dynamic identifiers come from allow-lists. |
| Expression injection | Expressions (validation, transform, comparator) run in a sandbox: no class, reflection or IO access, no `new`, allow-listed functions only, plus time and memory limits. They are compiled and checked at save time. Prefer CEL; if JEXL, use restricted permissions and prove it with attack tests. |
| XXE / XML bombs | XML and SOAP parsing with DTDs and external entities disabled, secure processing on, and entity-expansion and size limits. |
| Deserialization | No Java native serialization anywhere. Jackson default typing off, unknown properties rejected on inbound APIs, polymorphism only through explicit allow-lists. |
| SSRF | Connector URLs validated against a per-provider egress allow-list. Private, loopback, link-local and cloud metadata addresses (`169.254.169.254`, IPv6 equivalents) are blocked after DNS resolution, re-checked on redirect; redirects off by default. |
| Path traversal | Remote file names and archive entries normalized and confined to a sandbox directory. Names are never used to build local paths directly. |
| Decompression bombs / oversized input | Limits on compressed size, decompressed size, compression ratio, entry count, record size and nesting depth; streaming everywhere. |
| ReDoS | Regex comparators and validators use a linear-time engine (RE2/J) with input length limits. |
| Malicious uploads | Content-type sniffing, size limits, malware scan before processing, and storage outside any web root. |
| CSV/formula injection | Escaping in every CSV/XLSX export (section 10). |
| Sensitive data exposure | Field-level masking driven by `canonical_field.is_sensitive` and mapping sensitivity; masked in logs, errors, rejects, diffs, reports and APIs. Encryption at rest (DB, object store, backups) with KMS-managed keys; column-level encryption for designated PII. |
| Secrets | Section 6.1. Secret scanning (gitleaks or equivalent) in CI and in pre-commit; no secrets in images, env dumps or configuration repositories. |
| Transport | TLS 1.2+ only (1.3 preferred), strong cipher suites, HSTS; mTLS between services through the mesh; certificate rotation automated. |
| Error handling | Uniform problem-details responses; no stack traces, SQL, class names or internal hostnames in any response. |
| Supply chain | Dependency scanning (OWASP Dependency-Check or Snyk) and container scanning (Trivy) fail the build on High/Critical. SBOM (CycloneDX) per build; signed images; minimal, non-root, read-only-filesystem containers. |
| Logging and audit | Security events (login, permission denied, approval, unmask, export, configuration change, secret access failure) go to the tamper-evident audit log (11.4) and to the SIEM. |
| DoS | Rate limits and quotas per client and per endpoint, request size limits, timeouts, bounded thread and connection pools, and back-pressure on streaming endpoints. |

**Verification in CI:**
- SAST (SonarQube plus Semgrep or CodeQL);
- DAST (OWASP ZAP baseline plus an authenticated API scan driven by the OpenAPI spec) on every
  release candidate;
- dependency and container scans;
- secret scan;
- negative security tests for every row of the table.

## 19. API security — onboarding, reports and ingestion

These APIs change what money gets reconciled and expose financial data, so treat them as Tier-1.

- **Gateway and edge:**
  - all public APIs sit behind an API gateway with WAF rules;
  - management and operations APIs are on a separate internal listener, unreachable from the internet;
  - CORS uses an explicit origin allow-list, no wildcards, and credentials only where required.
- **Authentication:**
  - **Human users:** OAuth 2.1 / OIDC authorization code with PKCE through the bank's IdP, with MFA
    required. Step-up authentication (fresh MFA, `acr`/`amr` checked) is required to approve a
    configuration, resolve an exception, unmask data or export a report.
  - **Services:** client credentials with short-lived tokens, or SPIFFE/mTLS identities inside the
    cluster.
  - **Partners pushing data:** mTLS with pinned client certificates plus a signed request (HMAC or JWS
    over body, timestamp and nonce). Replays outside a 5-minute window or with a reused nonce are
    rejected.
- **Token validation:**
  - signature checked against JWKS (cached, rotation-aware);
  - `iss`, `aud`, `exp`, `nbf`, `iat` with small clock skew;
  - algorithm allow-list (no `none`, no HS/RS confusion);
  - token lifetime ≤ 15 min;
  - no tokens in URLs or logs.
- **Authorization:**
  - **Model:** roles (`ONBOARDING_MAKER`, `ONBOARDING_CHECKER`, `RECON_ANALYST`, `RECON_SUPERVISOR`,
    `REPORT_VIEWER`, `REPORT_EXPORTER`, `PII_UNMASK`, `OPERATOR`, `AUDITOR`, `PARTNER_<code>`) plus
    attribute-based data scope (which providers and business dates a caller may see).
  - **Where it is enforced:**
    - at the method level (`@PreAuthorize` or equivalent policy engine such as OPA);
    - in queries, via row-level security keyed on the caller's scope.
  - **Object-level checks (anti-BOLA/IDOR):** every `public_id` lookup verifies the object is in the
    caller's scope, and returns 404 rather than 403 to avoid disclosing existence.
  - **Function-level checks:** checker endpoints reject the maker of the same change.
  - **Property-level checks (mass assignment):** request DTOs are explicit, with no binding to entities.
    Responses are role-shaped, so fields the caller may not see are never serialized.
- **Input validation:**
  - every request validated against the contract (types, formats, ranges, max lengths, max array sizes,
    max nesting depth);
  - unknown fields rejected;
  - file uploads limited by size and type, then sniffed and scanned.
- **Idempotency and integrity:**
  - mutating endpoints require an `Idempotency-Key` and use optimistic locking (`If-Match` with
    `row_version`);
  - approvals bind to the exact `content_hash` that was reviewed.
- **Rate limiting:** per client and per user, stricter on export, unmask, preview and login-adjacent
  endpoints; `429` with `Retry-After`.
- **Response hardening:**
  - security headers (`Strict-Transport-Security`, `Content-Security-Policy` for any UI, `X-Content-Type-Options`,
    `Referrer-Policy`, `Cache-Control: no-store` on sensitive responses);
  - no server or version banners.
- **Report downloads:**
  - signed, short-lived (≤ 5 min), single-use URLs bound to the user;
  - the scope is re-checked at download time;
  - optional watermarking with user and time on PDF and XLSX;
  - every download audited.
- **Versioning:** URI or header versioning, with a deprecation policy (`Deprecation` and `Sunset`
  headers). Unpublished or undocumented endpoints fail the build (contract test against the router).

## 20. Questions to settle first

Before the design is final, list these with your recommended default and get them confirmed:
1. Real volumes per run and per day for each provider (the design assumes up to 100M+ records per run),
   peak concurrency (how many runs at once), the reference infrastructure the 40-minute ceiling is
   measured on, and the recon latency real-time sources need.
   - That includes the database service and storage class, with its IOPS, throughput and instance
     bandwidth limits.
2. The primary target cloud (AWS, Azure or GCP) and any secondary one, regions, multi-zone and
   cross-region DR expectations, and data residency constraints.
3. Which broker the first environment uses, and whether broker switching is a deploy-time choice only
   (assumed) or must also migrate in-flight messages.
4. Which secret manager(s) beyond AWS Secrets Manager, and which IdP (with MFA and step-up support).
5. Who consumes recon results and reports (event topic, API, files), in what formats, and which partners
   get direct API access.
6. Exception workflow ownership, maker-checker roles, and resolution SLAs.
7. Retention periods for raw artifacts, canonical rows, rejects, recon history, reports and audit.
8. Whether legacy data must be migrated into reconArk, or reconArk runs in parallel with outcome
   comparison before cut-over (recommended: parallel run with automated diffing).
9. The applicable regulatory and security frameworks (e.g. central-bank IT controls, PCI DSS if card
   data is in scope, ISO 27001) and who signs off the VAPT.

## 21. What to deliver, in order

1. **Architecture document** (extend `../02-architecture/reconArk-architecture.md`, don't restart it):
   - context and container diagrams (C4);
   - sequence diagrams for file ingestion, API push, streaming recon, batch recon, re-recon, report
     export and crash recovery;
   - a failure-mode table covering every component;
   - a threat model (STRIDE) for every external interface;
   - the transaction design per write path (section 12);
   - the per-cloud reference architectures and Terraform module interface (section 17);
   - ADRs for every significant choice.
2. **Configuration schema:**
   - JSON Schema for a provider document;
   - complete examples for a card acquirer delivering delimited files, a provider delivering PGP-encrypted files over SFTP, a REST
     provider with the nested payload in 6.4, a gRPC streaming provider, and the internal payments
     source;
   - the recon rule sets pairing them.
3. **Data model:**
   - the partitioning configuration (11.5) with its validator;
   - the Flyway migrations implementing section 11 exactly, generated against that configuration;
   - a constraint test suite proving every check, unique index, immutability trigger, row-level security
     policy and partition rule.
4. **Port interfaces and one reference adapter per port,** with the ArchUnit rules in place before
   adapters multiply.
5. **Build skeleton and security baseline wired into CI:**
   - the from-scratch Gradle build (16.2) with every module, convention plugin, test suite and quality
     gate in place;
   - SAST, DAST, dependency, container and secret scans, plus the negative security tests in section 18.

   This must be green before any feature work.
6. **Performance and isolation harness:** the nightly suite from 9.3 (100 to 100M records, plus the
   noisy-neighbour mix), with the capacity model it validates, including the database IO budget and
   assertions from 9.4. It must exist before the first large-volume feature.
7. **Delivery plan:** walking-skeleton first (one provider, end to end, one broker, onboarding and a
   report, all behind the full security model), then increments, each with its acceptance tests.

Do not start step 4 before steps 1–3 are reviewed.
