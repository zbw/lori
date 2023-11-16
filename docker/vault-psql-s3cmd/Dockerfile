# install vault
FROM alpine:3.18.4 AS vault

LABEL IMAGE_VERSION_TAG=0.1.0
LABEL IMAGE_NAME=vault-psql-s3cmd
LABEL OTC_STAGE=zbw-dev-nl

ARG VAULT_VERSION=1.15.1
RUN apk add --no-cache curl jq && \
    curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault* && \
    apk del curl

#install psql
FROM vault AS psql
ARG POSTGRES_CLIENT_VERSION=15.5-r0
ARG POSTGRES_CLIENT_MAJOR_VERSION=15
RUN apk add --no-cache postgresql"$POSTGRES_CLIENT_MAJOR_VERSION"-client="$POSTGRES_CLIENT_VERSION"

# install s3cmd
FROM psql AS s3cmd
ARG S3CMD_VERSION=2.3.0
RUN apk add --no-cache python3 py3-pip py3-setuptools libmagic && \
    pip3 install --no-cache-dir python-dateutil python-magic s3cmd=="$S3CMD_VERSION"

# use standard user
FROM s3cmd
ARG IMAGE_USER=vault-s3cmd-user
RUN addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER && \
    mkdir -p /var/log/$IMAGE_USER && \
    chown -R $IMAGE_USER:$IMAGE_USER /var/log/$IMAGE_USER
USER $IMAGE_USER


