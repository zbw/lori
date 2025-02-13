# About

## Microservice for managing digital object rights
This repository is a project by [ZBW](www.zbw.eu). It provides a frontend and backend for managing digital object rights (e.g. access status) of electronic ressources e.g. publications.
The main component is the LORI (Library of Rights) service (see `app/lori/server/` for more detailed information about the application).
In general LORI’s task is it to store, manage and provide object rights information. The UI enables users to curate object rights information efficiently.
Presentation systems of digital objects are meant to use this rights information via REST-API in order to e.g. grant access to digital objects or display rights information to users.
Currently, this is applied to digital objects stored in [DSpace](https://dspace.lyrasis.org/) instances which can be extended to other systems storing digital objects in the future.

## Why
The project goal is to provide a software solutions to maintain object rights information separately from the systems the corresponding digital objects are stored in. This allows to curate rights information irrespective of the storage solution chosen which has several advantages:

- Staff curating rights information can use the same UI to manage objects from different storages systems
- It is much more efficient to implement sophisticated rights management features in one specialised rights management application rather than numerous storage systems.
- Evolution of well-structed standardised object rights information metadata is eased
- A place is created where rights information from the level of license agreements is inferred to the level of individual digital objects which is what’s needed when practically working with digital objects
- object rights information changing over time can be efficiently curated which is a common use case in the age of OpenAccess transformation

## Setup
See `app/lori/server/` for how to set up the lori service. In general, it is required to have a `java`
of at least version 17 in your `$PATH`.

To build the whole project run:
```shell
./gradlew build
```
For running the microservice do:
```shell
./gradlew :app:lori:server:run
```
