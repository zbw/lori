Access-Service
====

## About

This service manages access rights for all kind of items. It provides a (g)rpc interface to change any information and
REST interface.

The [LIBRML](https://librml.org/index.html) standard (currently in beta version) is used as baseline for the interface.

## Local setup

Prerequisite: Setting up a local postgres.

First create a postgres docker container and start it.

```shell
docker run --name postgres_access -p 5432:5432 -e POSTGRES_PASSWORD=password -d postgres
docker start postgres_access
```

Then create a role, and a new database as follows with `psql`:

```shell
docker exec -it postgres_access psql -U postgres
CREATE USER access WITH PASSWORD '1qay2wsx' CREATEDB;
CREATE DATABASE accessinformation OWNER access ENCODING UTF8;
\q
```

Alternatively the tool [pgadmin4](https://www.pgadmin.org/) can be used for setting up the database.
You can connect yourself then via pgadmin or `psql`:
```
docker exec -it postgres_access_9.6 psql -U access -d accessinformation
```

Finally, start the service:

```shell
./gradlew :app:access:server:run
```

## Setup in Cloud environment

Due to OTC restrictions to configure databases via terraform (for more details see
[here](https://github.com/opentelekomcloud/terraform-provider-opentelekomcloud/issues/1513))
the initial commands need to be applied manually.
There exist two possible ways to accomplish this right now:
1. Run a postgres image in the k8s cluster (recommenend):
    - `kubectl run -n apps -i --tty --rm debug3 --image=library/postgres --restart=Never -- sh`
2. Remote Login via the jumphost:
    - Login into OTC console, select **Elastic Cloud Server**
    - Search for **jumphost** and press the **Remote Login** button

Either way, from their you are able to connect to the database for the first time (password
should be saved in vault):
```
psql --no-readline -U access -h 192.168.179.203 -p 5432 -d root -W
```
Create user with a save password and the database:

```sql
CREATE USER access WITH PASSWORD '1qay2wsx' CREATEDB;
GRANT access TO root;
CREATE DATABASE accessinformation OWNER access ENCODING UTF8;
```

## (G)RPC

In order to send messages grpc it the command line tool [grpcurl](https://github.com/fullstorydev/grpcurl) is recommended.

1. Add an items access right for _read_ rights with a restriction via grpc.

```shell
grpcurl -plaintext -d '{"items":[{"id":"test_id", "tenant": "www.zbw.eu", "usage_guide":"www.zbw.eu/licence", "mention":"true", "actions":[{"type":"ACTION_TYPE_PROTO_PUBLISH", "permission":"true", "restrictions":[{"type":"RESTRICTION_TYPE_PROTO_DATE", "attribute":{"type":"ATTRIBUTE_TYPE_PROTO_FROM_DATE", "values":"2020.01.01"}}]},{"type":"ACTION_TYPE_PROTO_READ", "permission":"true", "restrictions":[{"type":"RESTRICTION_TYPE_PROTO_AGE", "attribute":{"type":"ATTRIBUTE_TYPE_PROTO_MIN_AGE", "values":"18"}}]}]}]}' localhost:9092 de.zbw.access.api.v1.AccessService.AddAccessInformation
```

2. Retrieve access right via grpc:

```shell
grpcurl -plaintext -d '{"ids":["test_no_rest"]}' localhost:9092 de.zbw.access.api.v1.AccessService.GetAccessInformation
```


## REST

We use OpenApi v3.0.1 to represent all REST endpoints. The definition of the endpoints
can be found under `api/src/main/openapi`.

1. Send a POST request to add a new item:

```shell
 curl -H "Content-Type: application/json" \
--request POST \
--data '{"id":"testId", "tenant": "www.zbw.eu", "usage_guide":"www.zbw.eu/licence", "mention":"true", "actions":[{"permission":"true", "actiontype":"read", "restrictions":[{"restrictiontype":"age", "attributetype":"fromdate", "attributevalues":["18"]}]}]}' \
http://localhost:8082/api/v1/accessinformation
```

2. TODO Get request
