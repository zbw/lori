LORI-Service
====

## About

This service manages rights for bibliographic items. It provides a (g)rpc and REST interface to
read, update and delete these right information.

## Local setup

Prerequisite: Setting up a local postgres.

First create a postgres docker container and start it.

```shell
docker run --name <CONTAINER_NAME> -p <LOCAL_PORT>:5432 -e POSTGRES_PASSWORD=somepassword -d postgres
docker start <CONTAINER_NAME>
```

Then create a role, and a new database as follows with `psql`:

```shell
docker exec -it <CONTAINER_NAME> psql -U postgres
CREATE USER lori WITH PASSWORD '1qay2wsx' CREATEDB;
CREATE DATABASE loridb OWNER lori ENCODING UTF8;
\q
```

Alternatively the tool [pgadmin4](https://www.pgadmin.org/) can be used for setting up the database.
You can connect yourself then via pgadmin or `psql`:
```
docker exec -it postgres_lori_9.6 psql -U lori -d loriinformation
```

Finally, start the service:

```shell
./gradlew :app:lori:server:run
```

## Setup in Cloud environment

Due to OTC restrictions to configure databases via terraform (for more details see
[here](https://github.com/opentelekomcloud/terraform-provider-opentelekomcloud/issues/1513)) the
initial setup of the database is done manually.
There exist two possible ways to accomplish this right now:
1. Run a postgres image in the k8s cluster (recommenend):
    - `kubectl run -n apps -i --tty --rm debug3 --image=library/postgres --restart=Never -- sh`
2. Remote Login via the jumphost:
    - Login into OTC console, select **Elastic Cloud Server**
    - Search for **jumphost** and press the **Remote Login** button

Either way, from their you are able to connect to the database for the first time (password
should be saved in vault):

```
psql --no-readline -U root -h <FLOATING_IP_ADDRESS_POSTGRES> -p 5432 postgres
```
Create user with a save password and the database:

```sql
CREATE USER lori WITH PASSWORD '1qay2wsx' CREATEDB;
GRANT lori TO root;
CREATE DATABASE loriinformation OWNER lori ENCODING UTF8;
```

Be aware that this password and database name needs to be provided to Lori. See the the values file
in the microservice helm chart. Passwords are read from Vault, while the table name is passed with a
config map.

Afterwards you can connect to the DB as following:
```
psql -U lori -h 192.168.225.165 -p 5432 loriinformation
```
And then enter the chosen password (in this example `1qay2wsx`)

## (G)RPC

To send messages via grpc the command line tool [grpcurl](https://github.com/fullstorydev/grpcurl) is recommended.

1. Add an items lori right for _read_ rights with a restriction via grpc.

```shell
grpcurl -plaintext -d '{"items":[{"id":"test_id", "tenant": "www.zbw.eu",
"usage_guide":"www.zbw.eu/licence", "mention":"true",
"actions":[{"type":"ACTION_TYPE_PROTO_PUBLISH", "permission":"true",
"restrictions":[{"type":"RESTRICTION_TYPE_PROTO_DATE",
"attribute":{"type":"ATTRIBUTE_TYPE_PROTO_FROM_DATE",
"values":"2020.01.01"}}]},{"type":"ACTION_TYPE_PROTO_READ", "permission":"true",
"restrictions":[{"type":"RESTRICTION_TYPE_PROTO_AGE",
"attribute":{"type":"ATTRIBUTE_TYPE_PROTO_MIN_AGE", "values":"18"}}]}]}]}' localhost:9092
de.zbw.lori.api.v1.LoriService.AddAccessInformation
```

2. Retrieve lori right via grpc:

```shell
grpcurl -plaintext -d '{"ids":["test_no_rest"]}' localhost:9092 de.zbw.lori.api.v1.LoriService.GetAccessInformation
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
