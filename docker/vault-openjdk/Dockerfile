FROM eclipse-temurin:17.0.8.1_1-jdk AS tools

LABEL IMAGE_VERSION_TAG=0.2.4
LABEL IMAGE_NAME=vault-openjdk
LABEL OTC_STAGE=zbw-tools-nl

RUN apt update && \
    apt install -y \
    curl \
    wget \
    git \
    bash \
    jq \
    grep \
    unzip

FROM tools as vault
ARG VAULT_VERSION=1.14.0

# install vault
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*
