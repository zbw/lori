ALTER TABLE sessions DROP COLUMN role;
DROP TYPE role_enum;
create type permission_enum as enum ('READ', 'WRITE', 'ADMIN');
ALTER TABLE sessions ADD COLUMN permissions permission_enum[];
