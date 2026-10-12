-- config-service tables (schema "config").
create table recon_definition (
    id          text        primary key,
    name        text        not null,
    definition  jsonb       not null,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create unique index recon_definition_name_idx on recon_definition (lower(name));
