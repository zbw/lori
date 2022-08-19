plugins {
    id("zbw.kotlin-application")
    id("zbw.kotlin-conventions")
    id("zbw.kotlin-microservice-scaffold")
    id("zbw.kotlin-tests")
    id("zbw.tracing")
    id("zbw.kotlin-json")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    val ktorVersion by System.getProperties()
    val openTelemetry by System.getProperties()
    implementation(project(":app:lori:api"))
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-gson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    runtimeOnly(project(path = ":app:lori:server:ui", configuration = "npmResources"))
    implementation("com.mchange:c3p0:0.9.5.5")
    implementation("io.opentelemetry:opentelemetry-sdk:$openTelemetry")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:$openTelemetry-alpha")
    implementation("io.zonky.test:embedded-postgres:1.3.1")
    implementation("org.postgresql:postgresql:42.3.5")
    implementation("org.flywaydb:flyway-core:7.15.0")
}

application {
    mainClass.set("de.zbw.api.lori.server.LoriServer")
    applicationDefaultJvmArgs = listOf("--add-opens", "java.base/java.time=ALL-UNNAMED")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.740".toBigDecimal()
            }
        }
    }
}

tasks.test {
    // See https://github.com/mockk/mockk/issues/681 for more information
    jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")
}
