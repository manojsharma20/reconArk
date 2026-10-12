-- ingestion-service tables (schema "ingestion").
create table uploaded_file (
    id             text        primary key,
    definition_id  text        not null,
    side           text        not null,      -- LEFT, RIGHT
    original_name  text        not null,
    storage_key    text        not null,
    size_bytes     bigint      not null,
    sha256         text        not null,
    uploaded_at    timestamptz not null default now()
);
create index uploaded_file_definition_idx on uploaded_file (definition_id, side, uploaded_at desc);
