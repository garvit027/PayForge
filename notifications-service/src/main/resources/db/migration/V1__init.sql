create table notification (
    id             uuid primary key,
    event_key      varchar(160)   not null unique,
    transfer_id    varchar(64)    not null,
    kind           varchar(32)    not null,
    source_account varchar(34),
    target_account varchar(34),
    amount         numeric(19, 4),
    currency       varchar(3),
    message        varchar(500)   not null,
    created_at     timestamptz    not null default now()
);

create index idx_notification_source on notification (source_account, created_at desc);
create index idx_notification_target on notification (target_account, created_at desc);
