FROM eclipse-temurin:17.0.9_9-jdk AS tools

LABEL IMAGE_VERSION_TAG=0.9.0
LABEL IMAGE_NAME=vault-openjdk
LABEL OTC_STAGE=zbw-tools-nl

RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    bash \
    jq \
    grep \
    unzip \
    && rm -rf /var/lib/apt/lists/*

FROM tools as vault
ARG VAULT_VERSION=1.15.1
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*
