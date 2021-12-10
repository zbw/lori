plugins {
    `kotlin-dsl`
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = "17"
        languageVersion = "1.6"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:10.2.0")
    implementation("com.parmet:buf-gradle-plugin:0.3.1")
    implementation("gradle.plugin.com.google.protobuf:protobuf-gradle-plugin:0.8.18")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.5.32")
    implementation("com.github.node-gradle:gradle-node-plugin:3.0.1")
    implementation("gradle.plugin.com.google.cloud.tools:jib-gradle-plugin:3.1.4")
    implementation("org.openapitools:openapi-generator-gradle-plugin:5.3.0")
    implementation(gradleApi())
    implementation(localGroovy())
}
