FROM docker:20.10.21-git AS docker-git

RUN apk add --no-cache curl \
    bash \
    jq && \
    rm -rf /var/cache/apk/*

FROM docker-git as gitcrypt

RUN apk --update add \
   curl \
   git \
   g++ \
   make \
   openssl \
   openssl-dev \
   && rm -rf /var/cache/apk/*

ENV VERSION 0.7.0
RUN curl -L https://github.com/AGWA/git-crypt/archive/$VERSION.tar.gz | tar zxv -C /var/tmp
RUN cd /var/tmp/git-crypt-$VERSION && CXXFLAGS='-DOPENSSL_API_COMPAT=0x30000000L' make && make install PREFIX=/usr/local

FROM gitcrypt as vault
ARG VAULT_VERSION=1.12.1

# install vault
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*

# install terraform
FROM vault AS certificate
RUN apk update && apk add ca-certificates && rm -rf /var/cache/apk/*
COPY ./crt/vault-zbw-eu.crt /usr/local/share/ca-certificates/mycert.crt
RUN update-ca-certificates

FROM certificate AS VaultK8s
ARG KUBECTL_VERSION=1.22.17
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/v"$KUBECTL_VERSION"/bin/linux/amd64/kubectl && \
    chmod +x ./kubectl && \
    mv ./kubectl /usr/local/bin/kubectl

FROM VaultK8s AS DownloadHelm
ARG HELM_VERSION=3.10.3
ENV HELM_EXPERIMENTAL_OCI=1
RUN curl -L https://get.helm.sh/helm-v"$HELM_VERSION"-linux-amd64.tar.gz -o helm-v"$HELM_VERSION"-linux-amd64.tar.gz && \
    tar -xzf helm-v"$HELM_VERSION"-linux-amd64.tar.gz && \
    mv linux-amd64/helm /usr/local/bin/helm && rm -rf linux-amd64 && rm -rf helm-v"$HELM_VERSION"-linux-amd64.tar.gz

ARG HELMFILE_VERSION=0.149.0
RUN curl -L https://github.com/helmfile/helmfile/releases/download/v"$HELMFILE_VERSION"/helmfile_"$HELMFILE_VERSION"_linux_amd64.tar.gz -o helmfile-v"$HELMFILE_VERSION".tar.gz && \
    tar -xzf helmfile-v"$HELMFILE_VERSION".tar.gz && \
    chmod +x helmfile && \
    mv helmfile /usr/local/bin/helmfile && rm -rf helmfile && \
    helm plugin install https://github.com/databus23/helm-diff

FROM DownloadHelm AS AddUser
ARG IMAGE_USER=vault-kubectl-helm-user
RUN addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER
USER $IMAGE_USER

FROM AddUser AS AddHelmRepos
RUN helm repo add prometheus-community https://prometheus-community.github.io/helm-charts  && \
    helm repo add hashicorp https://helm.releases.hashicorp.com
