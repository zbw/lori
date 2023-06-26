FROM docker:24.0.2-git AS docker-git
RUN apk add --no-cache curl \
    bash \
    jq && \
    rm -rf /var/cache/apk/*

FROM docker-git as vault
ARG VAULT_VERSION=1.13.1

# install vault
RUN curl -LO https://releases.hashicorp.com/vault/"$VAULT_VERSION"/vault_"$VAULT_VERSION"_linux_amd64.zip && \
    unzip vault* && \
    chmod +x ./vault && \
    mv ./vault /usr/local/bin/vault && \
    rm vault*

# install terraform
FROM vault AS terraform
ARG TERRAFORM_VERSION=1.5.1
RUN curl -LO https://releases.hashicorp.com/terraform/"$TERRAFORM_VERSION"/terraform_"$TERRAFORM_VERSION"_linux_amd64.zip && \
    unzip terra* && \
    chmod +x ./terraform && \
    mv ./terraform /usr/local/bin/terraform

FROM terraform
ARG IMAGE_USER=vault-terraform
RUN addgroup -S $IMAGE_USER && \
    adduser -G $IMAGE_USER --system --shell=/bin/false --disabled-password $IMAGE_USER && \
    mkdir -p /var/log/$IMAGE_USER && \
    chown -R $IMAGE_USER:$IMAGE_USER /var/log/$IMAGE_USER
USER $IMAGE_USER
