import com.google.protobuf.gradle.id

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

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        val protobufVersion by System.getProperties()
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            val grpcVersion by System.getProperties()
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
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
    val protobufVersion by System.getProperties()
    val grpcKotlinStubVersion by System.getProperties()
    val kotlinxCoroutinesVersion by System.getProperties()
    api("com.google.protobuf:protobuf-java-util:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    api("io.grpc:grpc-kotlin-stub:$grpcKotlinStubVersion")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
}

val bufLint = task<Exec>("bufLint") {
    commandLine("${project.rootDir}/scripts/buf", "lint")
}

tasks.check {
    dependsOn(bufLint)
}
