# handle-docker
Dockerized Handle.net CESNET registry
with [JDBC](http://www.oracle.com/technetwork/java/javase/jdbc/index.html)
drivers for [MySQL](https://www.mysql.com/) and
[PostgreSQL](https://www.postgresql.org/). For usage instructions refer to the
[Handle.net software user manual](
https://www.handle.net/tech_manual/HN_Tech_Manual_8.pdf). The server software
is installed at ```/handle.net-server```.

# Usage

#### Clone the repository
```
git clone https://github.com/CESNET/handle-docker
cd handle-docker
```

#### (optional) Edit `docker-services.yml` build args to your liking
```yaml
args:
        - SITE_DESCRIPTION=Handle.net Registry Service
        - SITE_ORG=Example Site
        - SITE_CONTACT=contact@example.org
        - SITE_CONTACT_NAME=Example Contact
```

#### Build image with additional build args and run the service with your prefix
```
docker-compose build --build-arg CERTIFI_PASSPHRASE=YourCertPassphrase --build-arg ADM_PASSPHRASE=YourAdminPassphrase
docker-compose up -d
```

# Configuration

The following `--build-args` are possible when building the image:

| Parameter                            | Description                               | Default                             |
| ------------------------------------ | ----------------------------------------- | ----------------------------------- |
| SERVICE_IP                           | Public IP address for clients to connect to this server | 0.0.0.0               |
| BIND_ADDR                            | IP address the server should bind to      | 0.0.0.0                             |
| SITE_VERSION                         | Serial number of Handle site              | 1                                   |                                   
| SITE_DESCRIPTION                     | Handle site description                   | Example Handle.Net Registry service |
| SITE_ORG                             | Handle site organization                  | example.org                         |
| SITE_CONTACT                         | Handle site contact email                 | admin@example.org                   |
| SITE_CONTACT_NAME                    | Handle site contact name                  | admin                               |
| SITE_CONTACT_PHONE                   | Handle site contact phone                 |                                     |
| CERTIFI_PASSPHRASE                   | Site certification key passphrase         | handl3.net-CHANGEME!!!              |
| ADM_PASSPHRASE                       | Administrative key passphrase             | handl3.net-adm-CHANGEME!!!          |
| HANDLE_SOURCE                        | Handle server distribution package        | handle-9.2.0-distribution.tar.gz    |
| SRV_DIR                              | Handle site config directory              | /srv/handle                         |
| HANDLE_USER_ID                       | UID of the handle user                    | 1000                                |
| PSQL_DRIVER_PACKAGE                  | Package file name of PostgreSQL JDBC driver | postgresql-42.2.10.jar            |

The following `ENV` variables are available for runtime configuration:

| Parameter                            | Description                               | Default                             |
| ------------------------------------ | ----------------------------------------- | ----------------------------------- |
| SERVER_ADMINS                        | Comma seperated list of handle admins     |                                     |
| REPLICATION_ADMINS                   | Comma seperated list of handle admins for replication |                         |
| AUTO_HOMED_PREFIXES                  | Comma separated list of auto homed prefixes |                                   |
| SERVER_ADMIN_FULL_ACCESS | Admins in SERVER_ADMINS will have full permissions over all handles on the server | yes     |
| CASE_SENSITIVE                       | Are handles case sensitive                | no                                  |
| MAX_SESSION_TIME                     | Max authenticated client session time in ms. | 86400000                         |
| MAX_AUTH_TIME                        | Max time to wait for for client to respond to auth challenge. | 60000           |
| TRACE_RESOLUTION                     | Set to yes for debugging information to be logged for handle resolution. | no   |
| ALLOW_LIST_HDLS                      | Used to disable list_handles functionality. | no                                |
| ALLOW_RECURSION                | Allow recursive lookup outside of this handle server into global handle network. | no |
| STORAGE_TYPE                         | Empty defaults to built-in storage. Other main option is "sql" |                |
| SQL_URL                              | JDBC URL that is used to connect to the SQL database. |                         |
| SQL_DRIVER                      | Java class that contains the driver for the JDBC connection. | org.postgresql.Driver |
| SQL_LOGIN                | The user name that should be used by the handle server to connect to the database. | handle |
| SQL_PASSWD               | The password that should be used by the handle server to connect to the database.  |        |
| SQL_READ_ONLY                        | Boolean setting for allowing writes to database or not. | no                    |
| ALLOW_NA_ADMINS                      | Allow global handle server admins access to this handle server. | no            |

# Ports

The service exposes the following ports by default (can be overridden by ENV):

 * **ENV CLIENT_PORT** 2641 (TCP,UDP)
 * **ENV HTTP_PORT** 8000 (TCP)


## Licenses

* The [Handle.net software](https://handle.net/download_hnr.html)
is distributed under the
[Handle.net Public License Agreement (Ver.1)](
https://www.handle.net/HNRj/HNR-8-License.pdf).
* The [MySQL JDBC drivers](https://dev.mysql.com/downloads/connector/j/)
are distributed under the
[GNU General Public License, version 2](
https://www.gnu.org/licenses/old-licenses/gpl-2.0.html).
* The [PostgreSQL JDBC drivers](https://jdbc.postgresql.org/)
are distributed under the
[BSD 2-clause "Simplified" License](
https://jdbc.postgresql.org/about/license.html).

## Release History

* 1.0.0
    * Version 9.2.0 of the Handle.net server software

