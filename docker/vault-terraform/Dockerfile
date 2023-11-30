FROM alpine:3.18.4 AS tools

LABEL IMAGE_VERSION_TAG=0.9.0
LABEL IMAGE_NAME=vault-terraform
LABEL OTC_STAGE=zbw-tools-nl

RUN apk add --no-cache curl \
    bash \
    jq \
    git

# install vault
FROM tools as vault
ARG VAULT_VERSION=1.15.1
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*

# install terraform
FROM vault AS terraform
ARG TERRAFORM_VERSION=1.5.7
RUN curl -LO https://releases.hashicorp.com/terraform/"$TERRAFORM_VERSION"/terraform_"$TERRAFORM_VERSION"_linux_amd64.zip && \
    unzip terra* && \
    chmod +x ./terraform && \
    mv ./terraform /usr/local/bin/terraform && rm terra*.zip

FROM terraform
ARG IMAGE_USER=vault-terraform
RUN addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER && \
    mkdir -p /var/log/$IMAGE_USER && \
    chown -R $IMAGE_USER:$IMAGE_USER /var/log/$IMAGE_USER
USER $IMAGE_USER
