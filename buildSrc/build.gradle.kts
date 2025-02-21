plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

allprojects {
    repositories {
        maven { setUrl("https://jitpack.io") }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.1")
    implementation("build.buf:buf-gradle-plugin:0.10.0")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.9.4")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.1.0")
    implementation("com.github.node-gradle:gradle-node-plugin:7.1.0")
    implementation("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.4.4")
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.11.0")
    implementation(gradleApi())
    implementation(localGroovy())
}
