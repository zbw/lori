import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    kotlin("jvm")
    id("com.google.protobuf")
    idea
}

repositories {
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = "17"
        languageVersion = "1.6"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.6.1"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.15.1"
        }
        id("grpckt") {
            val grpcKotlinVersion by System.getProperties()
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk7@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

dependencies {
    val grpcVersion by System.getProperties()
    api("com.google.protobuf:protobuf-java-util:3.15.8")
    implementation("com.google.protobuf:protobuf-java:3.15.8")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    api("io.grpc:grpc-kotlin-stub:1.0.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
}

val bufLint = task<Exec>("bufLint") {
    commandLine("${project.rootDir}/scripts/buf", "lint")
}

tasks.check {
    dependsOn(bufLint)
}
