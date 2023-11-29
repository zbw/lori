# install vault
FROM alpine:3.18.4 AS vault

LABEL IMAGE_VERSION_TAG=0.9.0
LABEL IMAGE_NAME=vault-s3cmd
LABEL OTC_STAGE=zbw-tools-nl

ARG VAULT_VERSION=1.15.1
RUN apk add --no-cache curl jq && \
    curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault* && \
    apk del curl

# install s3cmd
FROM vault AS s3cmd
ARG S3CMD_VERSION=2.3.0-r3
RUN apk add --no-cache s3cmd="$S3CMD_VERSION"

# use standard user
FROM s3cmd
ARG IMAGE_USER=vault-s3cmd-user
RUN addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER && \
    mkdir -p /var/log/$IMAGE_USER && \
    chown -R $IMAGE_USER:$IMAGE_USER /var/log/$IMAGE_USER
USER $IMAGE_USER
