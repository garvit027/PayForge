create table customer (
    id         uuid primary key,
    full_name  varchar(160) not null,
    email      varchar(200) not null unique,
    country    varchar(2)   not null,
    created_at timestamptz  not null default now()
);

create table account (
    id             uuid primary key,
    customer_id    uuid         not null references customer (id),
    account_number varchar(34)  not null unique,
    ledger_code    varchar(64)  not null unique,
    currency       varchar(3)   not null,
    status         varchar(16)  not null,
    created_at     timestamptz  not null default now()
);

create index idx_account_customer on account (customer_id);

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
