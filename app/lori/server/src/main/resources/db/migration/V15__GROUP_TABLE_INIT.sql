CREATE TABLE right_group(
    name text PRIMARY KEY,
    description text,
    ip_addresses json NOT NULL
);