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
        languageVersion = "1.6"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:10.3.0")
    implementation("com.parmet:buf-gradle-plugin:0.6.0")
    implementation("gradle.plugin.com.google.protobuf:protobuf-gradle-plugin:0.8.18")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.21")
    implementation("com.github.node-gradle:gradle-node-plugin:3.0.1")
    implementation("gradle.plugin.com.google.cloud.tools:jib-gradle-plugin:3.2.1")
    implementation("org.openapitools:openapi-generator-gradle-plugin:5.4.0")
    implementation(gradleApi())
    implementation(localGroovy())
}
