plugins {
    kotlin("jvm")
}

dependencies {
    val log4j by System.getProperties()
    implementation("org.apache.logging.log4j:log4j-api:$log4j")
    implementation("org.apache.logging.log4j:log4j-core:$log4j")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("io.opentelemetry:opentelemetry-api:1.11.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.11.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.11.0")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}
