plugins{
    kotlin("jvm")
}

dependencies {
    val grpcVersion by System.getProperties()
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-netty:$grpcVersion")
    implementation("io.grpc:grpc-services:$grpcVersion")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("io.ktor:ktor-server-core:1.5.3")
    implementation("io.ktor:ktor-server-netty:1.5.3")
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    google()
}