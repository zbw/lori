plugins {
    kotlin("jvm")
}

dependencies {
    val log4j by System.getProperties()
    val openTelemetry by System.getProperties()
    implementation("org.apache.logging.log4j:log4j-api:$log4j")
    implementation("org.apache.logging.log4j:log4j-core:$log4j")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha7")

    implementation("io.opentelemetry:opentelemetry-api:$openTelemetry")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:$openTelemetry")
    implementation("io.opentelemetry:opentelemetry-extension-kotlin:$openTelemetry")
    testImplementation("org.slf4j:slf4j-reload4j:2.0.0-alpha7")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}
