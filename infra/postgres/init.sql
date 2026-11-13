-- Each service owns its schema. Separate databases keep migrations and blast radius small.
create database account_db;
create database ledger_db;
create database transfer_db;
