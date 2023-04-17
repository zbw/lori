plugins {
    kotlin("jvm")
}

dependencies {
    val log4j by System.getProperties()
    val openTelemetry by System.getProperties()
    val slf4j by System.getProperties()
    implementation("org.apache.logging.log4j:log4j-api:$log4j")
    implementation("org.apache.logging.log4j:log4j-core:$log4j")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j")
    implementation("org.slf4j:slf4j-api:$slf4j")
    implementation("io.opentelemetry:opentelemetry-api:$openTelemetry")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:$openTelemetry")
    implementation("io.opentelemetry:opentelemetry-extension-kotlin:$openTelemetry")
    testImplementation("org.slf4j:slf4j-reload4j:$slf4j")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}
