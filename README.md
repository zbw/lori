[#](#) About
TODO

# Docker Login
To be able to upload images to the OTC cloud a user needs to login
to its Docker Registry. This can be done by generating a longterm token
with the Access and Secret Key (see IAM in OTC).

```shell
export ACCESS_KEY=<YOUR-ACCESS-KEYS>
export SECRET_KEY=<YOUR-SECRET-KEYS>

# Get the longterm token
export LONGTERM_TOKEN=$(printf $ACCESS_KEY | openssl dgst -binary -sha256 -hmac $SECRET_KEY | od -An -vtx1 | sed 's/[ \n]//g' | sed 'N;s/\n//')

# Login
docker login -u eu-de_dev@$ACCESS_KEY -p $LONGTERM_TOKEN swr.eu-de.otc.t-systems.com
```

## Build image and push it to registry

```shell
# Build
./gradlew :app:access:server:jib

## Push
docker push swr.eu-de.otc.t-systems.com/zbw-dev/app-access-server:latest
```

