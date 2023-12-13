FROM alpine:3.19.0 AS tools

LABEL IMAGE_VERSION_TAG=0.9.2
LABEL IMAGE_NAME=vault-s3cmd
LABEL OTC_STAGE=zbw-tools-nl

RUN apk add --no-cache curl \
    bash \
    jq

FROM tools as vault
ARG VAULT_VERSION=1.15.4
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*

FROM vault AS s3cmd
ARG S3CMD_VERSION=2.3.0-r3
RUN apk add --no-cache s3cmd="$S3CMD_VERSION"

FROM s3cmd AS adduser
ARG IMAGE_USER=vault-s3cmd-user
RUN apk del curl && \
    addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER
USER $IMAGE_USER
