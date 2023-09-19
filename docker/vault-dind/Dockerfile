FROM docker:24.0.6-git AS docker-git

LABEL IMAGE_VERSION_TAG=0.0.2
LABEL IMAGE_NAME=vault-dind
LABEL OTC_STAGE=zbw-tools-nl

RUN apk add --no-cache curl \
    bash \
    jq && \
    rm -rf /var/cache/apk/*

FROM docker-git as vault
ARG VAULT_VERSION=1.14.0

# install vault
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*
