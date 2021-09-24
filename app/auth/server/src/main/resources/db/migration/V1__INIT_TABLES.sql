create table users
(
    id        serial primary key,
    name      varchar(255) not null unique ,
    password  varchar(255) not null,
    email     varchar(255)
);

create table roles
(
    id         serial primary key,
    name       varchar(255) not null
);

create table user_roles
(
    role_id integer,
    user_id integer,
    primary key(role_id,user_id),
    constraint user_roles_roleId_fkey foreign key (role_id) REFERENCES  roles(id),
    constraint user_roles_userId_fkey foreign key (user_id) REFERENCES  users(id)
);
