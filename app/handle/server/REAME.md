# Handle-Service

The HandleService is responsible for the communication with the Handle Server. Via grpc messages it can create, delete
and modify handles.

## Generate API code

```shell
./gradlew :app:handle:api:build
```

This will generate client code from the protobuf files. Both, Java and Kotlin code will be generated.

## Run service

```shell
./gradlew :app:handle:server:run
```

## Existing grpc endpoints

For interacting with grpc endpoints the following tool is recommended:
https://github.com/fullstorydev/grpcurl

### Create new handle

```shell
grpcurl -plaintext -d '{"custom_handle_suffix":"1007", "handle_values":[{"type":"HANDLE_TYPE_URL", "index": "1", "value":"www.example.com"}]}' localhost:9092 de.zbw.handle.api.v1.HandleService.AddHandle
```

### Delete a handle

```shell
grpcurl -plaintext -d '{"handle_suffix":"1007"}' localhost:9092 de.zbw.handle.api.v1.HandleService.DeleteHandle
```

### Add HandleValues

```shell
grpcurl -plaintext -d '{"handle_suffix":"1007"}' localhost:9092 de.zbw.handle.api.v1.HandleService.DeleteHandle }' localhost:9092 de.zbw.handle.api.v1.HandleService.AddHandleValues
```

TODOs:

- Delete HandleValue
- Modify HandleValue
- implement client interfaces