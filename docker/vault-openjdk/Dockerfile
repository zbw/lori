FROM debian:buster-slim AS tools

RUN apt update && \
    apt install -y \
    curl \
    wget \
    git \
    bash \
    unzip

FROM tools as openjdk11
RUN apt update && apt install -y openjdk-11-jre

FROM openjdk11 as vault
ARG VAULT_VERSION=1.8.5

# install vault
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*

FROM vault AS certificate
COPY ./crt/vault-zbw-eu.crt /usr/local/share/ca-certificates/mycert.crt
RUN chmod 644 /usr/local/share/ca-certificates/mycert.crt
RUN update-ca-certificates
