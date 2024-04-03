FROM alpine:3.19.1 AS tools

LABEL IMAGE_VERSION_TAG=1.0.3
LABEL IMAGE_NAME=vault-kubectl-helm
LABEL OTC_STAGE=zbw-tools-nl

RUN apk add --no-cache curl \
    bash \
    jq

FROM tools as vault
ARG VAULT_VERSION=1.15.6
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*

FROM vault as gitcrypt
RUN apk add --no-cache \
   git \
   g++ \
   make \
   openssl \
   openssl-dev
ENV VERSION 0.7.0
RUN curl -L https://github.com/AGWA/git-crypt/archive/$VERSION.tar.gz | tar zxv -C /var/tmp
RUN cd /var/tmp/git-crypt-$VERSION && CXXFLAGS='-DOPENSSL_API_COMPAT=0x30000000L' make && make install PREFIX=/usr/local && \
    apk del make

FROM gitcrypt AS kubectl
ARG KUBECTL_VERSION=1.26.15
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/v"$KUBECTL_VERSION"/bin/linux/amd64/kubectl && \
    chmod +x ./kubectl && \
    mv ./kubectl /usr/local/bin/kubectl

FROM kubectl AS helm
ARG HELM_VERSION=3.14.3
RUN curl -L https://get.helm.sh/helm-v"$HELM_VERSION"-linux-amd64.tar.gz -o helm-v"$HELM_VERSION"-linux-amd64.tar.gz && \
    tar -xzf helm-v"$HELM_VERSION"-linux-amd64.tar.gz && \
    chmod +x linux-amd64/helm && \
    mv linux-amd64/helm /usr/local/bin/helm && rm -rf linux-amd64 && rm -rf helm-v"$HELM_VERSION"-linux-amd64.tar.gz

ARG HELMFILE_VERSION=0.163.1
RUN curl -L https://github.com/helmfile/helmfile/releases/download/v"$HELMFILE_VERSION"/helmfile_"$HELMFILE_VERSION"_linux_amd64.tar.gz -o helmfile-v"$HELMFILE_VERSION".tar.gz && \
    mkdir helmfile-v"$HELMFILE_VERSION" && tar -xzf helmfile-v"$HELMFILE_VERSION".tar.gz --directory helmfile-v"$HELMFILE_VERSION" && \
    chmod +x helmfile-v"$HELMFILE_VERSION"/helmfile && \
    mv helmfile-v"$HELMFILE_VERSION"/helmfile /usr/local/bin/helmfile && rm -rf helmfile-v"$HELMFILE_VERSION"

FROM helm AS adduser
ARG IMAGE_USER=vault-kubectl-helm-user
RUN addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER
USER $IMAGE_USER

# install helm-diff in user context
FROM adduser AS helmdiff
ARG HELMDIFF_VERSION=3.9.5
RUN helm plugin install https://github.com/databus23/helm-diff --version $HELMDIFF_VERSION

