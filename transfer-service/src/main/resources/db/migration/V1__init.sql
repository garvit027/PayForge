create table transfer (
    id                     uuid primary key,
    idempotency_key        varchar(128)   not null unique,
    source_account_number  varchar(34)    not null,
    target_account_number  varchar(34)    not null,
    source_ledger_code     varchar(64),
    target_ledger_code     varchar(64),
    amount                 numeric(19, 4) not null check (amount > 0),
    currency               varchar(3)     not null,
    status                 varchar(16)    not null,
    note                   varchar(255),
    failure_reason         varchar(500),
    created_at             timestamptz    not null default now(),
    updated_at             timestamptz    not null default now(),
    version                bigint         not null default 0
);

create index idx_transfer_source on transfer (source_account_number, created_at desc);
create index idx_transfer_target on transfer (target_account_number, created_at desc);
create index idx_transfer_status on transfer (status);

create table outbox_entry (
    id             bigserial primary key,
    aggregate_type varchar(64)  not null,
    aggregate_id   varchar(128) not null,
    event_type     varchar(128) not null,
    payload        text         not null,
    status         varchar(16)  not null default 'PENDING',
    attempts       int          not null default 0,
    created_at     timestamptz  not null default now(),
    published_at   timestamptz
);

create index idx_outbox_pending on outbox_entry (status, id);
