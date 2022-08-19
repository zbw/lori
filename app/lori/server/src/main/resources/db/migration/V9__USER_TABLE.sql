create type role_enum as enum ('READONLY', 'READWRITE', 'ADMIN');
create table users(
    username text,
    password text,
    role role_enum,
    primary key (username)
);