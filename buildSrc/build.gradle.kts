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

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = "17"
        languageVersion = "1.8.0"
    }
}

dependencies {
    val kotlinVersion by System.getProperties()
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:11.0.0")
    implementation("com.parmet:buf-gradle-plugin:0.8.2")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.9.1")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.21")
    implementation("com.github.node-gradle:gradle-node-plugin:3.2.1")
    implementation("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.3.1")
    implementation("org.openapitools:openapi-generator-gradle-plugin:6.2.1")
    implementation(gradleApi())
    implementation(localGroovy())
}
