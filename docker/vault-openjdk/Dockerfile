FROM eclipse-temurin:17.0.8_7-jdk AS tools

LABEL IMAGE_VERSION_TAG=0.2.1
LABEL IMAGE_NAME=vault-openjdk-test
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
