-- Schema concepts. Identity is (schema_type, name) (D11, D12).
create table schema_concept (
    id            uuid primary key,
    schema_type   varchar(16)  not null,
    name          varchar(64)  not null,
    title         text         not null,
    description   text         not null,
    resource_type varchar(64),
    status        varchar(16)  not null,
    slot_key      varchar(80)  not null,
    created       timestamptz  not null,
    modified      timestamptz  not null,
    created_by    varchar(255),
    constraint uq_concept_type_name unique (schema_type, name)
);

-- At most one PUBLISHED concept per slot, enforced by the database (D16).
create unique index uq_one_published_per_slot on schema_concept (slot_key) where status = 'PUBLISHED';

create table schema_version (
    concept_id uuid        not null references schema_concept (id),
    version    integer     not null,
    content    bytea       not null,
    changelog  text        not null,
    created    timestamptz not null,
    created_by varchar(255),
    primary key (concept_id, version)
);

-- One row per slot, locked with SELECT ... FOR UPDATE to serialise slot transitions (D16).
create table slot_lock (
    slot_key varchar(80) primary key
);

create table webhook (
    id         uuid primary key,
    target_url text         not null,
    events     varchar(255) not null,
    events_key varchar(255) not null,
    created    timestamptz  not null,
    constraint uq_webhook_target_events unique (target_url, events_key)
);
