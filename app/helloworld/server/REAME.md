# HelloWorld-Service

An exemplary implementation for a microservice.

It consists of an **api**, **client** (TODO), **server** and **test**(TODO) project.

## Generate API code

```shell
./gradlew :app:helloworld:api:build
```

This will generate client code from the protobuf files. Both, Java and Kotlin code will be generated.

## Run service

```shell
./gradlew :app:helloworld:server:run
```

## Grpc Endpoint

For interacting with grpc endpoints the following tool is recommended:
https://github.com/fullstorydev/grpcurl

```shell
grpcurl -plaintext -d '{"name":"foo"}' localhost:9092 de.zbw.helloworld.api.v1.HelloWorldService.SayHello
```

## Probes

Readyness:
```shell
 curl -vvv 127.0.0.1:8080/ready
```

Healthyness:
```shell
 curl -vvv 127.0.0.1:8080/healthz
```

## Code coverage

Code coverage is done via the jacoco plugin. To run all tests and check for a coverage violation
afterwards, run:
```shell
./gradlew :app:helloworld:server:check
```

To generate a jacoco test report run:

```shell
./gradlew :app:helloworld:server:jacocoTestReport
```
Reports can be found under `./build/reports/jacoco/test/html`

## Ktlint

To check for ktlint errors run
```shell
./gradlew :app:helloworld:server:ktlintCheck
```