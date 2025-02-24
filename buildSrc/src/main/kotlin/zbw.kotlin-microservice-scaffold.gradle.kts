plugins {
    kotlin("jvm")
}

dependencies {
    val grpcVersion by System.getProperties()
    val ktorVersion by System.getProperties()
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-netty:$grpcVersion")
    implementation("io.grpc:grpc-services:$grpcVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.github.gfelbing:konfig-core:0.4.0")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}
