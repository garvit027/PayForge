create table ledger_account (
    id             uuid primary key,
    code           varchar(64)  not null unique,
    owner_ref      varchar(128) not null,
    currency       varchar(3)   not null,
    account_type   varchar(16)  not null,
    allow_negative boolean      not null default false,
    created_at     timestamptz  not null default now()
);

create table journal_entry (
    id          uuid primary key,
    reference   varchar(128) not null unique,
    description varchar(255),
    status      varchar(16)  not null,
    created_at  timestamptz  not null default now()
);

create table posting (
    id              uuid primary key,
    journal_entry_id uuid        not null references journal_entry (id),
    ledger_account_id uuid       not null references ledger_account (id),
    direction       varchar(8)   not null,
    amount          numeric(19, 4) not null check (amount > 0),
    currency        varchar(3)   not null
);

create index idx_posting_account on posting (ledger_account_id);
create index idx_posting_journal on posting (journal_entry_id);

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

insert into ledger_account (id, code, owner_ref, currency, account_type, allow_negative)
values (gen_random_uuid(), 'PLATFORM-SETTLEMENT', 'platform', 'INR', 'ASSET', true);
