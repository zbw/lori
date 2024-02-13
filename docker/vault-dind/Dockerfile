FROM docker:25.0.3-git AS tools

LABEL IMAGE_VERSION_TAG=1.0.0
LABEL IMAGE_NAME=vault-dind
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
