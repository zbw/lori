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

Test the grpc endpoint:
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
