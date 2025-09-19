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
    val kotlinVersion by System.getProperties()
    val gradlePluginBuf by System.getProperties()
    val gradlePluginJib by System.getProperties()
    val gradlePluginKtlint by System.getProperties()
    val gradlePluginNode by System.getProperties()
    val gradlePluginOpenapi by System.getProperties()
    val gradlePluginProtobuf by System.getProperties()
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:$gradlePluginKtlint")
    implementation("build.buf:buf-gradle-plugin:$gradlePluginBuf")
    implementation("com.google.protobuf:protobuf-gradle-plugin:$gradlePluginProtobuf")
    implementation("com.github.node-gradle:gradle-node-plugin:$gradlePluginNode")
    implementation("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:$gradlePluginJib")
    implementation("org.openapitools:openapi-generator-gradle-plugin:$gradlePluginOpenapi")
    implementation(gradleApi())
    implementation(localGroovy())
}
