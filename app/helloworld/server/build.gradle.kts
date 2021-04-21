import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion by System.getProperties()
    kotlin("jvm") version "$kotlinVersion"
    id("application")
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":app:helloworld:api"))

    implementation("io.grpc:grpc-stub:1.37.0")
    implementation("io.grpc:grpc-protobuf:1.37.0")
    implementation("io.grpc:grpc-netty:1.37.0")
    implementation("io.grpc:grpc-services:1.37.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("org.apache.logging.log4j:log4j-api:2.14.1")
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")
    implementation("io.ktor:ktor-server-core:1.5.3")
    implementation("io.ktor:ktor-server-netty:1.5.3")

    testImplementation(kotlin("test-testng"))
}

tasks.test {
    useTestNG()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("de.zbw.api.helloworld.server.HelloWorldServer")
}
