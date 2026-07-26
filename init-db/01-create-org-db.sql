-- Runs once, on first postgres container boot, via Postgres's
-- /docker-entrypoint-initdb.d/ convention. Creates the organization database
-- and its dedicated user (platform rule: one database per service).

CREATE USER org_user WITH PASSWORD 'org_pass';
CREATE DATABASE org_db OWNER org_user;
GRANT ALL PRIVILEGES ON DATABASE org_db TO org_user;
