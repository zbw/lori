LORI-Service
====

## About

The LORI (Library of Rights) service  manages rights for bibliographic items. It provides a (g)rpc and REST interface to
read, update and delete these right information.

## Local setup

**Prerequisites**: Docker

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
and enable the required extension `pg_trgm`:
```sql
CREATE EXTENSION if not exists pg_trgm;
```

Alternatively the tool [pgadmin4](https://www.pgadmin.org/) can be used for setting up the database
and extension.
You can connect yourself then via pgadmin or `psql`:
```
docker exec -it postgres_lori_9.6 psql -U lori -d loriinformation
```

Set the chosen password and database name in `src/main/resources/lori.properties` or as environment
variable.

Finally, start the service from the projects root directory:

```shell
./gradlew :app:lori:server:run
```

Afterwards the service should be accessible under: `localhost:8082/ui`

## Setup in Cloud environment

**Prerequisites**: Having access to a cloud environment with kubernetes.
We use the [Open Telekom Cloud (OTC)](https://open-telekom-cloud.com/en).


### Database Setup
Due to restrictions with the OTC to configure databases via terraform (for more details see
[here](https://github.com/opentelekomcloud/terraform-provider-opentelekomcloud/issues/1513)) the
initial setup of the database is done manually. If your cloud provider allows that, feel free
to skip this section and apply this setup in terraform directly.

There exist two possible ways to accomplish the manual setup:
1. Run a postgres image in the k8s cluster (recommenend):
    - `kubectl run -n <YOUR_NAMESPACE> -i --tty --rm dbSetup --image=library/postgres --restart=Never -- sh`
2. Remote Login via a jumphost (here is described how this can be achieved for the OTC):
    - Login into OTC console, select **Elastic Cloud Server**
    - Search for **jumphost** and press the **Remote Login** button

Either way, from their you are able to connect to the database for the first time (password
should be saved in somewhere secure, for example in [vault](https://www.vaultproject.io/)):

```
psql --no-readline -U root -h <FLOATING_IP_ADDRESS_POSTGRES> -p 5432 postgres
```
Create user with a save password and the database:

```sql
CREATE USER lori WITH PASSWORD '1qay2wsx' CREATEDB;
GRANT lori TO root;
CREATE DATABASE loriinformation OWNER lori ENCODING UTF8;
```

and enable the required extension `pg_trgm`:
```sql
CREATE EXTENSION if not exists pg_trgm;
```

Afterwards you should be able to connect to the DB as following:
```
psql -U lori -h <FLOATING_IP_ADDRESS_POSTGRES> -p 5432 loriinformation
```

Be aware that this password and database name and user needs to be provided to Lori at startup. We
use a HelmChart repository that contains a _values_ file for lori with all information. Sensitive
variables like passwords are read from a Vault store and then provided as secrets. Our tool of
choice for the whole Helm setup is [Helmfile](https://github.com/roboll/helmfile).


### Docker Login
To be able to upload images to the OTC cloud a user needs to login
to its Docker Registry. This can be done by generating a longterm token
with the Access and Secret Key (see IAM in OTC).

```shell
export ACCESS_KEY=<YOUR-ACCESS-KEYS>
export SECRET_KEY=<YOUR-SECRET-KEYS>

# Get the longterm token
export LONGTERM_TOKEN=$(printf $ACCESS_KEY | openssl dgst -binary -sha256 -hmac $SECRET_KEY | od -An -vtx1 | sed 's/[ \n]//g' | sed 'N;s/\n//')

# Login
docker login -u eu-nl_dev-nl@"$ACCESS_KEY" -p "$LONGTERM_TOKEN" swr.eu-nl.otc.t-systems.com
```

There exists a helper script for the above commands in our terraform repository.

### Build & Push microservice image

```shell
./gradlew :app:lori:server:jib
docker push swr.eu-nl.otc.t-systems.com/zbw-dev/app-lori-server:latest
```

### Run in cloud
Since we use a HelmCharts for the whole cloud setup, there doesn't exist any yaml files in the
repository. Basically this instruction showed you how to setup the Database and how to build the
image. How to apply these in a cloud environment is out of scope.

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

We use OpenApi v3 to represent all REST endpoints. The definition of the endpoints
can be found under `api/src/main/openapi`.

Example: Send a POST request to add a new right entry:

```shell
curl -vvv -H "Content-Type: application/json" \
--request POST \
--data '{"rightId":123, "startDate": "2022-01-01", "endDate":"2023-01-01", "licenseConditions":"somelicense", "provenanceLicense":"proven", "accessState":"open"}' \
localhost:8082/api/v1/right
```
