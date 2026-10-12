-- reconArk recon engine tables (schema "engine"). Applied by the manager with Flyway at start-up.
-- Workers write recon_record and recon_result; report-service reads recon_run and recon_result.

-- One reconciliation run: definition snapshot, progress and the final counts.
create table recon_run (
    id               uuid        primary key,
    definition_id    text        not null,
    definition_name  text        not null,
    definition       jsonb       not null,               -- snapshot: later edits to the definition don't change this run
    left_file_id     text        not null,
    right_file_id    text        not null,
    status           text        not null,               -- CREATED, LOADING, MATCHING, COMPLETED, FAILED
    bucket_count     int         not null,
    left_loaded      bigint      not null default 0,
    right_loaded     bigint      not null default 0,
    rejected         bigint      not null default 0,
    matched          bigint      not null default 0,
    mismatched       bigint      not null default 0,
    only_in_left     bigint      not null default 0,
    only_in_right    bigint      not null default 0,
    error            text,
    created_at       timestamptz not null default now(),
    finished_at      timestamptz
);
create index recon_run_created_idx on recon_run (created_at desc);

-- The jobs of a run: 2 LOAD jobs (one per file), then one MATCH job per bucket.
create table recon_job (
    id               uuid        primary key,
    run_id           uuid        not null references recon_run (id) on delete cascade,
    type             text        not null,               -- LOAD, MATCH
    side             text,                               -- LOAD only: LEFT, RIGHT
    storage_key      text,                               -- LOAD only
    bucket           int,                                -- MATCH only
    status           text        not null,               -- QUEUED, DONE, FAILED
    attempts         int         not null default 1,
    records_read     bigint      not null default 0,
    records_rejected bigint      not null default 0,
    results_written  bigint      not null default 0,
    worker_id        text,
    error            text,
    sent_at          timestamptz not null default now(),
    finished_at      timestamptz
);
create index recon_job_run_idx on recon_job (run_id, type, status);
create index recon_job_queued_idx on recon_job (sent_at) where status = 'QUEUED';

-- Records loaded from the files (staging). Deleted when the run completes.
create table recon_record (
    run_id       uuid   not null,
    side         text   not null,
    bucket       int    not null,
    match_key    text   not null,
    line_number  bigint not null,
    fields       jsonb  not null,
    load_job_id  uuid   not null
);
create index recon_record_bucket_idx on recon_record (run_id, bucket);
create index recon_record_job_idx on recon_record (load_job_id);

-- The outcome for every match key.
create table recon_result (
    id            bigserial primary key,
    run_id        uuid  not null,
    bucket        int   not null,
    match_key     text  not null,
    status        text  not null,                        -- MATCHED, MISMATCHED, ONLY_IN_LEFT, ONLY_IN_RIGHT
    left_fields   jsonb,
    right_fields  jsonb,
    differences   jsonb not null default '[]'
);
create index recon_result_run_status_idx on recon_result (run_id, status, id);
create index recon_result_run_bucket_idx on recon_result (run_id, bucket);
