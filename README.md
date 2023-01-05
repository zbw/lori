# About
This repository contains the microservice source code.

# Docker Login
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

There exists a helper script for the above commands in the terraform repository.

## Build & Push microservice image

```shell
./gradlew :app:access:server:jib
docker push swr.eu-nl.otc.t-systems.com/zbw-dev/app-access-server:latest
```

## Build & push CI/CD image

```shell
cd docker/vault-terraform
docker build -t vault-terraform .
docker tag vault-terraform swr.eu-nl.otc.t-systems.com/zbw-tools-nl/vault-terraform:latest
docker push swr.eu-nl.otc.t-systems.com/zbw-tools-nl/vault-terraform:latest
```

# Gitlab

## Pushing images & personal token
When changes in the last commit are detected for any microservice its image gets
pushed to the cloud. To figure out the changes it is necessary to request the last
commit from the Gitlab API. This request requires a valid token with read rights
on that API. It can be generated in the UI or via Gitlabs API
(https://docs.gitlab.com/ee/api/users.html#create-a-personal-access-token).

If it ever happens that a pipeline prints a 401 error, an outdated token
might be the issue.
