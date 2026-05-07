DROP INDEX IF EXISTS querystring_idx;

CREATE INDEX querystring_md5_idx ON bookmark (md5(querystring));