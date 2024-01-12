# About
This repository is a project by the [ZBW](www.zbw.eu). It provides a frontend and backend for managing
the access status of bibliographic items.

The main component is the LORI (Library of Rights) service (see `app/lori/server/` for more
detailed information about the application). In general LORIs task is it to manage access information that are stored in
[DSpace](https://dspace.lyrasis.org/) instances (at least for now).

## Why
DSpace is a widespread repository system used in libraries to store metadata and the actual
data, like thesis, books etc.
The access can be managed on a community level, representing an organisation or publisher, or on a
collection level, which may represent a specific package from a publisher containing several
publications. With the OpenAccess transformation the access rights of specific publications in packages
may change over time. For example, some papers might be restricted to read for a few years, until they eventually
will become open access. When it comes to this fine grained right management issues, DSpace has its limitations.
LORI aims to be a service which harvests metadata information and provide their access right
information.

## Setup
See `app/lori/server/` for how to setup the service. In general it is required to have a `java`
version of version 17 in `$PATH`.

To build the project run:
```shell
./gradlew build
```

# Build & push CI/CD images

```shell
cd docker/<IMAGE_DIR>
docker build -t <IMAGE_TAG> .
docker tag <IMAGE_TAG> swr.eu-nl.otc.t-systems.com/zbw-tools-nl/<IMAGE_TAG>:latest
docker push swr.eu-nl.otc.t-systems.com/zbw-tools-nl/<IMAGE_TAG>:latest
```