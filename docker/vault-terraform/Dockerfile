FROM alpine:3.19.1 AS tools

LABEL IMAGE_VERSION_TAG=1.0.4
LABEL IMAGE_NAME=vault-terraform
LABEL OTC_STAGE=zbw-tools-nl

RUN apk add --no-cache curl \
    bash \
    jq

FROM tools as vault
ARG VAULT_VERSION=1.16.2
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*

FROM vault AS terraform
ARG TERRAFORM_VERSION=1.8.2
RUN apk add --no-cache git && \
    curl -LO https://releases.hashicorp.com/terraform/"$TERRAFORM_VERSION"/terraform_"$TERRAFORM_VERSION"_linux_amd64.zip && \
    unzip terra* && \
    chmod +x ./terraform && \
    mv ./terraform /usr/local/bin/terraform && rm terra*.zip

FROM terraform AS adduser
ARG IMAGE_USER=vault-terraform
RUN addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER
USER $IMAGE_USER
