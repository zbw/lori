FROM alpine:3.18.2 AS docker-git

LABEL IMAGE_VERSION_TAG=0.1.0
LABEL IMAGE_NAME=vault-kubectl-helm
LABEL OTC_STAGE=zbw-tools-nl
LABEL TEST=13

RUN apk add --no-cache curl \
    bash \
    jq \
    git && \
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

# install vault
FROM gitcrypt as vault
ARG VAULT_VERSION=1.14.0
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*

# install kubectl
FROM vault AS vaultk8s
ARG KUBECTL_VERSION=1.26.7
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/v"$KUBECTL_VERSION"/bin/linux/amd64/kubectl && \
    chmod +x ./kubectl && \
    mv ./kubectl /usr/local/bin/kubectl

# install helm
FROM vaultk8s AS downloadhelm
ARG HELM_VERSION=3.12.2
ENV HELM_EXPERIMENTAL_OCI=1
RUN curl -L https://get.helm.sh/helm-v"$HELM_VERSION"-linux-amd64.tar.gz -o helm-v"$HELM_VERSION"-linux-amd64.tar.gz && \
    tar -xzf helm-v"$HELM_VERSION"-linux-amd64.tar.gz && \
    mv linux-amd64/helm /usr/local/bin/helm && rm -rf linux-amd64 && rm -rf helm-v"$HELM_VERSION"-linux-amd64.tar.gz

ARG HELMFILE_VERSION=0.155.1
RUN curl -L https://github.com/helmfile/helmfile/releases/download/v"$HELMFILE_VERSION"/helmfile_"$HELMFILE_VERSION"_linux_amd64.tar.gz -o helmfile-v"$HELMFILE_VERSION".tar.gz && \
    tar -xzf helmfile-v"$HELMFILE_VERSION".tar.gz && \
    chmod +x helmfile && \
    mv helmfile /usr/local/bin/helmfile && rm -rf helmfile

FROM downloadhelm AS adduser
ARG IMAGE_USER=vault-kubectl-helm-user
RUN addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER
USER $IMAGE_USER

# install helm-diff in user context
FROM adduser AS helmdiff
ARG HELMDIFF_VERSION=3.8.1
RUN helm plugin install https://github.com/databus23/helm-diff --version $HELMDIFF_VERSION

FROM helmdiff AS addhelmrepos
RUN helm repo add prometheus-community https://prometheus-community.github.io/helm-charts  && \
    helm repo add hashicorp https://helm.releases.hashicorp.com
