Access-Service
====

## About

This service manages access rights for all kind of items. It provides a (g)rpc interface to change any information. A
REST interface will follow soon.

The [LIBRML](https://librml.org/index.html) standard (currently in beta version) is used as baseline for the interface.

## Local setup

Prerequisite: A postgres database needs to be set up.

First create a docker container and start it.

```shell
docker run --name postgres_access -p 5432:5432 -e POSTGRES_PASSWORD=password -d postgres
docker start postgres_access
```

Then the database and role needs to be created. Via `psql`:

```shell
docker exec -it postgres_access psql -U postgres
CREATE USER access WITH PASSWORD '1qay2wsx' CREATEDB;
CREATE DATABASE accessinformation OWNER access ENCODING UTF8;
\q
```

Alternatively the tool [pgadmin4](https://www.pgadmin.org/) can be used for the setup as well.

Finally, start the service:

```shell
./gradlew :app:access:server:run
```

## Example

This example uses the command line tool [grpcurl](https://github.com/fullstorydev/grpcurl).

1. Add an items access right for _read_ rights with a restriction via grpc.

```shell
grpcurl -plaintext -d '{"items":[{"id":"test_id", "tenant": "www.zbw.eu", "usage_guide":"www.zbw.eu/licence", "mention":"true", "actions":[{"type":"ACTION_TYPE_PROTO_PUBLISH", "permission":"true", "restrictions":[{"type":"RESTRICTION_TYPE_PROTO_DATE", "attribute":{"type":"ATTRIBUTE_TYPE_PROTO_FROM_DATE", "values":"2020.01.01"}}]},{"type":"ACTION_TYPE_PROTO_READ", "permission":"true", "restrictions":[{"type":"RESTRICTION_TYPE_PROTO_AGE", "attribute":{"type":"ATTRIBUTE_TYPE_PROTO_MIN_AGE", "values":"18"}}]}]}]}' localhost:9092 de.zbw.access.api.v1.AccessService.AddAccessInformation
```

2. Retrieve access right via grpc:

```shell
grpcurl -plaintext -d '{"ids":["test_no_rest"]}' localhost:9092 de.zbw.access.api.v1.AccessService.GetAccessInformation
```