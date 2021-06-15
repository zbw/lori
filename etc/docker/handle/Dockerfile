FROM golang as GOMPLATE

RUN mkdir -p /go/src/github.com/hairyhenderson \
 && git clone https://github.com/hairyhenderson/gomplate.git /go/src/github.com/hairyhenderson/gomplate

WORKDIR /go/src/github.com/hairyhenderson/gomplate
ENV CGO_ENABLED=0
RUN make build

FROM openjdk:9-jdk

# Setup container environment
ARG SERVICE_IP="0.0.0.0"
ARG BIND_ADDR="0.0.0.0"
ARG SITE_VERSION=1
ARG SITE_DESCRIPTION="Example Handle.Net Registry service"
ARG SITE_ORG="example.org"
ARG SITE_CONTACT_NAME="admin"
ARG SITE_CONTACT_PHONE=""
ARG SITE_CONTACT="admin@example.org"

ARG CERTIFI_PASSPHRASE="handl3.net-CHANGEME!!!"
ARG ADM_PASSPHRASE="password"

ARG PSQL_DRIVER_PACKAGE="postgresql-42.2.10.jar"
ARG HANDLE_SOURCE=handle-9.2.0-distribution.tar.gz
ARG SRV_DIR=${SRV_DIR}
ARG HANDLE_USER_ID=1000

ENV DEBIAN_FRONTEND noninteractive
ENV TERM xterm
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV CLIENT_PORT 2641
ENV HTTP_PORT 8000
ENV SRV_DIR /srv/handle

# Site environment
ENV SERVICE_IP=${SERVICE_IP}
ENV BIND_ADDR=${BIND_ADDR}
ENV SITE_VERSION ${SITE_VERSION}
ENV SITE_DESCRIPTION ${SITE_DESCRIPTION}
ENV SITE_ORG ${SITE_ORG}
ENV SITE_CONTACT ${SITE_CONTACT}
ENV SITE_CONTACT_NAME ${SITE_CONTACT_NAME}
ENV SITE_CONTACT_PHONE ${SITE_CONTACT_PHONE}
ENV CERTIFI_PASSPHRASE ${CERTIFI_PASSPHRASE}
ENV ADM_PASSPHRASE ${ADM_PASSPHRASE}

# Site config file environment defaults
ENV SERVER_ADMINS "5678/ADMIN"
ENV REPLICATION_ADMINS "5678/ADMIN"
ENV AUTO_HOMED_PREFIXES "5678/ADMIN"
ENV SERVER_ADMIN_FULL_ACCESS yes
ENV CASE_SENSITIVE no
ENV MAX_SESSION_TIME 86400000
ENV MAX_AUTH_TIME 60000
ENV TRACE_RESOLUTION no
ENV ALLOW_LIST_HDLS no
ENV ALLOW_RECURSION no
ENV TEMPLATE_NS_OVERRIDE yes
ENV STORAGE_TYPE "sql"
ENV SQL_URL "jdbc:postgresql://db:5432/handlesystem"
ENV SQL_DRIVER "org.postgresql.Driver"
ENV SQL_LOGIN "handleserver"
ENV SQL_PASSWD "1qay2wsx"
ENV SQL_READ_ONLY no
ENV ALLOW_NA_ADMINS no

LABEL maintainer="bauer@cesnet.cz" \
  org.label-schema.name=${SITE_DESCRIPTION} \
  org.label-schema.vendor=${SITE_ORG} \
  org.label-schema.schema-version="1.0"

# Expose ports
EXPOSE ${CLIENT_PORT}/tcp
EXPOSE ${CLIENT_PORT}/udp
EXPOSE ${HTTP_PORT}/tcp

# Prepare handle user
RUN useradd handle --create-home -d ${SRV_DIR} --uid ${HANDLE_USER_ID} --gid 0 && \
    chown -R handle:root ${SRV_DIR}


# Download and unpack handle distribution
COPY --from=GOMPLATE /go/src/github.com/hairyhenderson/gomplate/bin/gomplate /usr/bin/gomplate
RUN apt-get update && apt-get install -y --no-install-recommends wget \
	locales locales-all expect\
	&& apt-get clean \
	&& rm -rf /var/lib/apt/lists/* \
	&& mkdir /handle.net-server \
	&& chown handle /handle.net-server

# Setup system locale
RUN localedef --quiet -c -i en_US -f UTF-8 ${LANG}

USER handle

WORKDIR /handle.net-server

RUN wget https://www.handle.net/hnr-source/${HANDLE_SOURCE} \
  && tar -xf ${HANDLE_SOURCE} --strip-components=1
RUN wget https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.44.tar.gz \
  && tar -xf mysql-connector-java-5.1.44.tar.gz -C lib --strip-components=1 \
      mysql-connector-java-5.1.44/mysql-connector-java-5.1.44-bin.jar \
  && rm mysql-connector-java-5.1.44.tar.gz

# Download and install PostgreSQL JDBC driver
RUN wget https://jdbc.postgresql.org/download/${PSQL_DRIVER_PACKAGE} -P lib

# Install and set up the handle distribution
RUN mkdir ./gomplates
COPY --chown=handle:root templates/*.tmpl ./gomplates/
COPY --chown=handle:root scripts/*.sh ./
RUN ./setup.sh

ENTRYPOINT ["./entrypoint.sh"]
CMD [ "./run.sh" ]
