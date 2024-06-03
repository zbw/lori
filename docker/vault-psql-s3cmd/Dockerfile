FROM alpine:3.20.0 AS tools

LABEL IMAGE_VERSION_TAG=1.0.1
LABEL IMAGE_NAME=vault-psql-s3cmd
LABEL OTC_STAGE=zbw-dev-nl

RUN apk add --no-cache curl \
    bash \
    jq

FROM tools as vault
ARG VAULT_VERSION=1.16.3
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*

FROM vault AS s3cmd
ARG S3CMD_VERSION=2.4.0-r1
RUN apk add --no-cache s3cmd="$S3CMD_VERSION"

FROM s3cmd AS psql
ARG POSTGRES_CLIENT_VERSION=16.3-r0
ARG POSTGRES_CLIENT_MAJOR_VERSION=16
RUN apk add --no-cache postgresql"$POSTGRES_CLIENT_MAJOR_VERSION"-client="$POSTGRES_CLIENT_VERSION"

FROM psql AS adduser
ARG IMAGE_USER=vault-s3cmd-user
RUN apk del curl && \
    addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER
USER $IMAGE_USER
